package com.mikhilnaika.continueapp.feature.stacks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikhilnaika.continueapp.core.billing.BillingRepository
import com.mikhilnaika.continueapp.core.data.dao.GameDao
import com.mikhilnaika.continueapp.core.data.dao.StackDao
import com.mikhilnaika.continueapp.core.data.entity.StackEntity
import com.mikhilnaika.continueapp.core.data.entity.StackMemberEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** docs/02-PRODUCT-SPEC.md §1 "Stacks (collections)" — Free: 2 stacks. Pro: unlimited. */
private const val FREE_STACK_LIMIT = 2

data class StackMemberDisplay(val gameId: Long, val name: String, val coverUrl: String?)

data class StacksUiState(
    val stacks: List<StackEntity> = emptyList(),
    val isPro: Boolean = false,
    val selectedStackId: Long? = null,
    val selectedStackMembers: List<StackMemberDisplay> = emptyList(),
    val limitReachedMessage: String? = null,
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class StacksViewModel @Inject constructor(
    private val stackDao: StackDao,
    private val gameDao: GameDao,
    billingRepository: BillingRepository,
) : ViewModel() {

    private val selectedStackId = MutableStateFlow<Long?>(null)
    private val _state = MutableStateFlow(StacksUiState())
    val state: StateFlow<StacksUiState> = _state

    init {
        combine(stackDao.observeAll(), billingRepository.isPro) { stacks, pro -> stacks to pro }
            .onEach { (stacks, pro) -> _state.update { it.copy(stacks = stacks, isPro = pro) } }
            .launchIn(viewModelScope)

        selectedStackId
            .onEach { id -> _state.update { it.copy(selectedStackId = id) } }
            .launchIn(viewModelScope)

        selectedStackId.flatMapLatest { id ->
            if (id == null) kotlinx.coroutines.flow.flowOf(emptyList()) else stackDao.observeMembers(id)
        }.onEach { members ->
            val display = members.map { member ->
                val game = gameDao.get(member.gameId)
                StackMemberDisplay(member.gameId, game?.name ?: "", game?.coverUrl)
            }
            _state.update { it.copy(selectedStackMembers = display) }
        }.launchIn(viewModelScope)
    }

    fun selectStack(stackId: Long?) {
        selectedStackId.value = stackId
    }

    fun createStack(name: String, emoji: String?) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val count = stackDao.count()
            if (!_state.value.isPro && count >= FREE_STACK_LIMIT) {
                _state.update { it.copy(limitReachedMessage = "Free plan is capped at $FREE_STACK_LIMIT stacks — GO PRO for unlimited.") }
                return@launch
            }
            stackDao.insert(
                StackEntity(name = name.trim(), emoji = emoji, sortOrder = count, createdAt = System.currentTimeMillis())
            )
        }
    }

    fun renameStack(stackId: Long, name: String, emoji: String?) {
        if (name.isBlank()) return
        viewModelScope.launch { stackDao.rename(stackId, name.trim(), emoji) }
    }

    fun deleteStack(stack: StackEntity) {
        viewModelScope.launch {
            stackDao.delete(stack)
            if (selectedStackId.value == stack.stackId) selectedStackId.value = null
        }
    }

    fun dismissLimitMessage() = _state.update { it.copy(limitReachedMessage = null) }

    fun addGameToStack(stackId: Long, gameId: Long) {
        viewModelScope.launch {
            stackDao.addMember(StackMemberEntity(stackId, gameId, sortOrder = System.currentTimeMillis().toInt()))
        }
    }

    fun removeGameFromStack(stackId: Long, gameId: Long) {
        viewModelScope.launch { stackDao.removeMember(stackId, gameId) }
    }
}
