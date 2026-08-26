package com.mikhilnaika.continueapp.feature.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikhilnaika.continueapp.core.data.AddSource
import com.mikhilnaika.continueapp.core.data.PileState
import com.mikhilnaika.continueapp.core.data.dao.GameDao
import com.mikhilnaika.continueapp.core.data.dao.PileDao
import com.mikhilnaika.continueapp.core.data.entity.GameEntity
import com.mikhilnaika.continueapp.core.data.entity.PileEntryEntity
import com.mikhilnaika.continueapp.core.network.GameDataSource
import com.mikhilnaika.continueapp.core.network.dto.GameDto
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class DiscoverUiState(
    val query: String = "",
    val searchResults: List<GameDto> = emptyList(),
    val isSearching: Boolean = false,
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
)

/** docs/02-PRODUCT-SPEC.md §2c — 300ms debounce, instant add, rails. */
private const val SEARCH_DEBOUNCE_MS = 300L

private const val RAIL_TRENDING = "trending"
private const val RAIL_NEW = "new"
private const val RAIL_SHORT = "short"
private const val RAIL_GEMS = "gems"
private const val RAIL_GENRE = "genre"

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val gameDataSource: GameDataSource,
    private val gameDao: GameDao,
    private val pileDao: PileDao,
) : ViewModel() {

    private val _state = MutableStateFlow(DiscoverUiState())
    val state: StateFlow<DiscoverUiState> = _state

    private var searchJob: Job? = null
    private val json = Json { ignoreUnknownKeys = true }

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
        fillRail(RAIL_TRENDING) { gameDataSource.trending() }
        fillRail(RAIL_NEW) { gameDataSource.newReleases() }
        fillRail(RAIL_SHORT) { gameDataSource.shortAndSweet() }
        fillRail(RAIL_GEMS) { gameDataSource.hiddenGems() }
        loadGenreRail()
    }

    private fun fillRail(key: String, fetch: suspend () -> List<GameDto>) {
        viewModelScope.launch {
            val games = try {
                fetch()
            } catch (cancellation: CancellationException) {
                throw cancellation // never swallow cancellation — it breaks structured concurrency
            } catch (error: Exception) {
                emptyList()
            }
            _state.update { current ->
                current.copy(rails = current.rails.map { if (it.key == key) it.copy(games = games) else it })
            }
        }
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
            _state.update {
                it.copy(rails = it.rails + DiscoverRail(RAIL_GENRE, "MORE ${shortGenreLabel(genre)}"))
            }
            fillRail(RAIL_GENRE) { gameDataSource.byGenre(genre) }
        }
    }

    private suspend fun dominantPileGenre(): String? =
        pileDao.observeAll().first()
            .flatMap { entry -> runCatching { json.decodeFromString<List<String>>(entry.genresJson) }.getOrDefault(emptyList()) }
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
            _state.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            _state.update { it.copy(isSearching = true) }
            val results = runCatching { gameDataSource.search(query) }.getOrDefault(emptyList())
            _state.update { it.copy(searchResults = results, isSearching = false) }
        }
    }

    /**
     * Instant add with no navigation away from the list — docs/02-PRODUCT-SPEC.md §2c.
     * Caches the [GameDto] into `games` first: `pile_entries` is joined against `games` for
     * every read, so an entry pointing at an uncached id would silently vanish from PILE.
     *
     * Two deliberate details, both from the closed-test report that games added here never
     * reached the pile:
     *
     * 1. The whole write runs [NonCancellable]. A tap on a card followed immediately by a tap
     *    on the nav bar is the *normal* way to use this screen, and the three suspending DAO
     *    calls below are three chances for a cleared ViewModel to cancel the write halfway —
     *    which loses the add silently, with the tick already shown. A handful of milliseconds
     *    of local SQLite work is not worth making interruptible.
     * 2. Every outcome says something. Tapping a game already in the pile used to be a
     *    completely silent no-op, indistinguishable from a broken add.
     */
    fun addToPile(game: GameDto) {
        viewModelScope.launch {
            val alreadyInPile = withContext(NonCancellable) {
                gameDao.upsert(game.toEntity())
                val existing = pileDao.findByGameId(game.id) != null
                if (!existing) {
                    pileDao.insert(
                        PileEntryEntity(
                            gameId = game.id,
                            state = PileState.BACKLOG,
                            addedAt = System.currentTimeMillis(),
                            source = AddSource.SEARCH,
                        )
                    )
                }
                existing
            }
            _state.update {
                it.copy(
                    // Optimistic: `observePile` will confirm this a beat later, but the tick has
                    // to land on the same frame as the tap.
                    addedGameIds = it.addedGameIds + game.id,
                    message = DiscoverMessage(
                        id = System.currentTimeMillis(),
                        text = if (alreadyInPile) {
                            "${game.name.uppercase()} IS ALREADY IN YOUR PILE"
                        } else {
                            "${game.name.uppercase()} ADDED TO YOUR PILE"
                        },
                    ),
                )
            }
        }
    }

    /** Called once the confirmation banner has been on screen long enough. */
    fun consumeMessage(id: Long) {
        _state.update { if (it.message?.id == id) it.copy(message = null) else it }
    }

    private fun GameDto.toEntity(): GameEntity = GameEntity(
        id = id,
        slug = slug,
        name = name,
        coverUrl = coverUrl,
        backgroundUrl = backgroundUrl,
        released = released,
        metacritic = metacritic,
        rating = rating,
        playtimeHoursHastily = playtimeHoursHastily,
        playtimeHoursNormally = playtimeHoursNormally,
        playtimeHoursCompletely = playtimeHoursCompletely,
        genresJson = json.encodeToString(genres),
        tagsJson = json.encodeToString(tags),
        platformsJson = json.encodeToString(platforms),
        cachedAt = System.currentTimeMillis(),
    )
}
