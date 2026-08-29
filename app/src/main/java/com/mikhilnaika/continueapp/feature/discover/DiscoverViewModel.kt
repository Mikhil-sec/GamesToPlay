package com.mikhilnaika.continueapp.feature.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikhilnaika.continueapp.core.billing.BillingRepository
import com.mikhilnaika.continueapp.core.data.AddSource
import com.mikhilnaika.continueapp.core.data.GameCacheRepository
import com.mikhilnaika.continueapp.core.data.PileState
import com.mikhilnaika.continueapp.core.data.clearRewardKey
import com.mikhilnaika.continueapp.core.data.dao.PileDao
import com.mikhilnaika.continueapp.core.data.entity.PileEntryEntity
import com.mikhilnaika.continueapp.core.network.GameDataSource
import com.mikhilnaika.continueapp.core.network.dto.GameDto
import com.mikhilnaika.continueapp.core.offline.OfflineGameIndex
import com.mikhilnaika.continueapp.core.util.SearchRanking
import com.mikhilnaika.continueapp.core.util.decodeStringList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class DiscoverUiState(
    val query: String = "",
    val searchResults: List<GameDto> = emptyList(),
    val isSearching: Boolean = false,
    /**
     * Set when the search that produced these results wasn't quite the one the user typed —
     * see [OfflineGameIndex.respell]. Shown above the list, because silently searching for
     * something else is how a search box loses trust.
     */
    val searchNote: String? = null,
    val rails: List<DiscoverRail> = emptyList(),
    /**
     * Every game already in the pile, read from Room rather than remembered in memory.
     *
     * It used to be a set this ViewModel appended to on each tap, which meant the "added" tick
     * only existed until you navigated away: come back to DISCOVER and every game you had
     * already added looked untouched again. A closed-test tester duly re-tapped games and had
     * no way to tell an add from a no-op — which is most of why "I added games and they never
     * showed up" was so hard to pin down. Sourced from the pile itself, the state is true on
     * first composition and stays true.
     */
    val addedGameIds: Set<Long> = emptySet(),
    /** Transient confirmation for the last tap — see [DiscoverMessage]. */
    val message: DiscoverMessage? = null,
)

/**
 * A one-shot line of feedback for a tap on a game.
 *
 * Carries an [id] purely so that adding the same game twice in a row still re-triggers the
 * banner: without it the state would be `==` to the last one and the UI would sit silent on
 * exactly the tap the user most needs an answer to.
 */
data class DiscoverMessage(val id: Long, val text: String)

/**
 * One browsable row. [key] exists so a rail can be filled in when its request lands without the
 * list re-ordering — the rails are declared up front in display order and arrive out of order.
 */
data class DiscoverRail(
    val key: String,
    val title: String,
    val games: List<GameDto> = emptyList(),
    /** Highest page already merged in. SHOW MORE asks for the next one. */
    val page: Int = 0,
    val isLoadingMore: Boolean = false,
    /**
     * False once the rail is exhausted — either the Worker's page cap was reached or a page
     * came back with nothing new. A SHOW MORE button that can only disappoint is worse than no
     * button, so it removes itself.
     */
    val canLoadMore: Boolean = true,
)

/** docs/02-PRODUCT-SPEC.md §2c — 300ms debounce, instant add, rails. */
private const val SEARCH_DEBOUNCE_MS = 300L

/** Matches `MAX_PAGE` in `worker/src/security.ts` — three pages, 60 games, then it stops. */
private const val MAX_RAIL_PAGE = 2

private const val RAIL_TRENDING = "trending"
private const val RAIL_NEW = "new"
private const val RAIL_SHORT = "short"
private const val RAIL_GEMS = "gems"
private const val RAIL_GENRE = "genre"

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val gameDataSource: GameDataSource,
    private val gameCacheRepository: GameCacheRepository,
    private val offlineGameIndex: OfflineGameIndex,
    private val billingRepository: BillingRepository,
    private val pileDao: PileDao,
) : ViewModel() {

    private val _state = MutableStateFlow(DiscoverUiState())
    val state: StateFlow<DiscoverUiState> = _state

    private var searchJob: Job? = null

    /** The genre the personalised rail was built for, needed to page it. */
    private var genreRailName: String? = null

    init {
        observePile()
        loadRails()
    }

    /** Keeps [DiscoverUiState.addedGameIds] equal to what is actually in the pile. */
    private fun observePile() {
        viewModelScope.launch {
            pileDao.observeAllGameIds()
                .map { it.toSet() }
                .collect { ids -> _state.update { it.copy(addedGameIds = ids) } }
        }
    }

    /**
     * Five rails, per docs/02-PRODUCT-SPEC.md §2c — two of which existed, which is why a tester
     * reported DISCOVER as feeling empty.
     *
     * Each rail is its own coroutine and fills in as it lands, rather than the screen waiting on
     * the slowest of five sequential round trips. The rails are declared up front in display
     * order so nothing re-shuffles under the user's thumb as results arrive, and a rail that
     * comes back empty simply doesn't render.
     */
    private fun loadRails() {
        _state.update {
            it.copy(
                rails = listOf(
                    DiscoverRail(RAIL_TRENDING, "TRENDING NOW"),
                    DiscoverRail(RAIL_NEW, "NEW RELEASES"),
                    DiscoverRail(RAIL_SHORT, "SHORT & SWEET"),
                    DiscoverRail(RAIL_GEMS, "HIDDEN GEMS"),
                )
            )
        }
        fillRail(RAIL_TRENDING, page = 0)
        fillRail(RAIL_NEW, page = 0)
        fillRail(RAIL_SHORT, page = 0)
        fillRail(RAIL_GEMS, page = 0)
        loadGenreRail()
    }

    private suspend fun fetchRail(key: String, page: Int): List<GameDto> = when (key) {
        RAIL_TRENDING -> gameDataSource.trending(page)
        RAIL_NEW -> gameDataSource.newReleases(page)
        RAIL_SHORT -> gameDataSource.shortAndSweet(page)
        RAIL_GEMS -> gameDataSource.hiddenGems(page)
        RAIL_GENRE -> genreRailName?.let { gameDataSource.byGenre(it, page) }.orEmpty()
        else -> emptyList()
    }

    private fun fillRail(key: String, page: Int) {
        viewModelScope.launch {
            val games = try {
                fetchRail(key, page)
            } catch (cancellation: CancellationException) {
                throw cancellation // never swallow cancellation — it breaks structured concurrency
            } catch (error: Exception) {
                emptyList()
            }
            _state.update { current ->
                current.copy(
                    rails = current.rails.map { rail ->
                        if (rail.key != key) return@map rail
                        // Merge rather than replace: IGDB's windows overlap at the edges (a
                        // game's rating count can move it between pages between requests), and a
                        // repeated id in a keyed LazyRow throws.
                        val seen = rail.games.mapTo(mutableSetOf()) { it.id }
                        val fresh = games.filter { seen.add(it.id) }
                        rail.copy(
                            games = rail.games + fresh,
                            page = page,
                            isLoadingMore = false,
                            canLoadMore = page < MAX_RAIL_PAGE && fresh.isNotEmpty(),
                        )
                    }
                )
            }
        }
    }

    /**
     * SHOW MORE — asked for directly in closed-test feedback ("*Discover could have Show More,
     * if it would strain the api, drop the idea*").
     *
     * It doesn't strain it, and that is a property of the design rather than luck: a rail's
     * contents don't depend on who is asking, so page 2 of TRENDING is **one** cache entry
     * shared by every user of the app, not one per user. Three pages is the whole budget, after
     * which the button removes itself. See the KV-write reasoning in `worker/src/index.ts`.
     */
    fun loadMoreRail(key: String) {
        val rail = _state.value.rails.firstOrNull { it.key == key } ?: return
        if (!rail.canLoadMore || rail.isLoadingMore || rail.page >= MAX_RAIL_PAGE) return
        _state.update { current ->
            current.copy(rails = current.rails.map { if (it.key == key) it.copy(isLoadingMore = true) else it })
        }
        fillRail(key, rail.page + 1)
    }

    /**
     * A rail built from whatever the user's pile is actually full of.
     *
     * Appended only once a genre is known, and skipped entirely for an empty pile — a
     * "personalised" rail on a fresh install is just a random genre wearing a personal label.
     */
    private fun loadGenreRail() {
        viewModelScope.launch {
            val genre = dominantPileGenre() ?: return@launch
            genreRailName = genre
            _state.update {
                it.copy(rails = it.rails + DiscoverRail(RAIL_GENRE, "MORE ${shortGenreLabel(genre)}"))
            }
            fillRail(RAIL_GENRE, page = 0)
        }
    }

    private suspend fun dominantPileGenre(): String? =
        pileDao.observeAll().first()
            .flatMap { entry -> decodeStringList(entry.genresJson) }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key

    /**
     * IGDB spells its genres out in full — "Role-playing (RPG)", "Real Time Strategy (RTS)" —
     * which makes an unreadable rail heading. Where the name carries a short parenthetical
     * acronym, that acronym *is* the everyday name, so prefer it; otherwise drop the
     * parenthetical and keep the words. The full name is still what gets sent to the Worker,
     * because that has to match IGDB's own table exactly.
     */
    private fun shortGenreLabel(genre: String): String {
        val parenthetical = Regex("""\(([^)]+)\)""").find(genre)?.groupValues?.get(1)
        if (parenthetical != null && parenthetical.length <= 5 && parenthetical == parenthetical.uppercase()) {
            return parenthetical
        }
        return genre.substringBefore(" (").trim().uppercase()
    }

    /** The X in the search field — docs/02-PRODUCT-SPEC.md §2c. */
    fun clearQuery() {
        onQueryChanged("")
    }

    fun onQueryChanged(query: String) {
        _state.update { it.copy(query = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _state.update { it.copy(searchResults = emptyList(), isSearching = false, searchNote = null) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            _state.update { it.copy(isSearching = true) }

            val direct = searchOrEmpty(query)

            // The respell pass — the fix for "spiderman vs spider-man". IGDB tokenises on
            // punctuation, so a query with the separators left out is one unknown token and
            // matches almost nothing; the on-device IGDB name index knows where the spaces go
            // (see OfflineGameIndex.respell). Both spellings are searched and merged, because
            // the original is still the better query whenever it was already right.
            val respelled = respellOrNull(query)
            val extra = if (respelled != null) searchOrEmpty(respelled) else emptyList()

            val merged = SearchRanking.clean(query, direct + extra)
            val note = if (respelled != null && extra.isNotEmpty()) {
                "ALSO SHOWING \"${respelled.uppercase()}\""
            } else {
                null
            }
            _state.update { it.copy(searchResults = merged, isSearching = false, searchNote = note) }
        }
    }

    private suspend fun searchOrEmpty(query: String): List<GameDto> = try {
        gameDataSource.search(query)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        emptyList()
    }

    private suspend fun respellOrNull(query: String): String? = try {
        offlineGameIndex.respell(query)?.takeIf { !it.equals(query, ignoreCase = true) }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        null
    }

    /**
     * Instant add with no navigation away from the list — docs/02-PRODUCT-SPEC.md §2c.
     * Caches the [GameDto] into `games` first: `pile_entries` is joined against `games` for
     * every read, so an entry pointing at an uncached id would silently vanish from PILE.
     *
     * [state], [startedAt] and [finishedAt] are what make this the backdating path too: a game
     * you finished in 2019 goes straight into CLEARED with the dates you actually played it,
     * rather than having to be added, moved, and then edited.
     *
     * Two deliberate details, both from the closed-test report that games added here never
     * reached the pile:
     *
     * 1. The whole write runs [NonCancellable]. A tap on a card followed immediately by a tap
     *    on the nav bar is the *normal* way to use this screen, and the suspending DAO calls
     *    below are each a chance for a cleared ViewModel to cancel the write halfway — which
     *    loses the add silently, with the tick already shown. A handful of milliseconds of
     *    local SQLite work is not worth making interruptible.
     * 2. Every outcome says something. Tapping a game already in the pile used to be a
     *    completely silent no-op, indistinguishable from a broken add.
     */
    fun addToPile(
        game: GameDto,
        state: PileState = PileState.BACKLOG,
        startedAt: Long? = null,
        finishedAt: Long? = null,
    ) {
        viewModelScope.launch {
            // Logging a finished game is record-keeping, not an achievement — burn the reward
            // key so a backdated clear can never be turned into coins. See CoinLedger.
            if (finishedAt != null) billingRepository.markRewardClaimed(clearRewardKey(game.id))

            val alreadyInPile = withContext(NonCancellable) {
                gameCacheRepository.cache(game)
                val existing = pileDao.findByGameId(game.id)
                if (existing == null) {
                    pileDao.insert(
                        PileEntryEntity(
                            gameId = game.id,
                            state = state,
                            // A backdated game joined the pile the day it was played, not
                            // today: otherwise "time in the pile" reads 0 days for a game
                            // finished in 2019 and RECENT buries this year's games under it.
                            addedAt = startedAt ?: finishedAt ?: System.currentTimeMillis(),
                            startedAt = startedAt,
                            finishedAt = finishedAt,
                            source = AddSource.SEARCH,
                        )
                    )
                } else if (startedAt != null || finishedAt != null) {
                    // Already there, but the user has just told us when they played it — that's
                    // an edit, not a duplicate, so honour it instead of refusing.
                    pileDao.update(
                        existing.copy(
                            state = state,
                            startedAt = startedAt ?: existing.startedAt,
                            finishedAt = finishedAt ?: existing.finishedAt,
                        )
                    )
                }
                existing != null
            }
            _state.update {
                it.copy(
                    // Optimistic: `observePile` will confirm this a beat later, but the tick has
                    // to land on the same frame as the tap.
                    addedGameIds = it.addedGameIds + game.id,
                    message = DiscoverMessage(
                        id = System.currentTimeMillis(),
                        text = messageFor(game, state, alreadyInPile, backdated = finishedAt != null),
                    ),
                )
            }
        }
    }

    private fun messageFor(
        game: GameDto,
        state: PileState,
        alreadyInPile: Boolean,
        backdated: Boolean,
    ): String {
        val name = game.name.uppercase()
        return when {
            backdated && alreadyInPile -> "$name — DATES UPDATED"
            backdated -> "$name LOGGED AS CLEARED"
            alreadyInPile -> "$name IS ALREADY IN YOUR PILE"
            state == PileState.WISHLIST -> "$name ADDED TO WANTED"
            else -> "$name ADDED TO YOUR PILE"
        }
    }

    /** Called once the confirmation banner has been on screen long enough. */
    fun consumeMessage(id: Long) {
        _state.update { if (it.message?.id == id) it.copy(message = null) else it }
    }
}
