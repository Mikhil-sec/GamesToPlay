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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class DiscoverUiState(
    val query: String = "",
    val searchResults: List<GameDto> = emptyList(),
    val isSearching: Boolean = false,
    val trending: List<GameDto> = emptyList(),
    val shortAndSweet: List<GameDto> = emptyList(),
    val addedGameIds: Set<Long> = emptySet(),
)

/** docs/02-PRODUCT-SPEC.md §2c — 300ms debounce, instant add, rails. */
private const val SEARCH_DEBOUNCE_MS = 300L

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
        loadRails()
    }

    private fun loadRails() {
        viewModelScope.launch {
            val trending = runCatching { gameDataSource.trending() }.getOrDefault(emptyList())
            val short = runCatching { gameDataSource.shortAndSweet() }.getOrDefault(emptyList())
            _state.update { it.copy(trending = trending, shortAndSweet = short) }
        }
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
     */
    fun addToPile(game: GameDto) {
        viewModelScope.launch {
            gameDao.upsert(game.toEntity())
            if (pileDao.findByGameId(game.id) == null) {
                pileDao.insert(
                    PileEntryEntity(
                        gameId = game.id,
                        state = PileState.BACKLOG,
                        addedAt = System.currentTimeMillis(),
                        source = AddSource.SEARCH,
                    )
                )
            }
            _state.update { it.copy(addedGameIds = it.addedGameIds + game.id) }
        }
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
