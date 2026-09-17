package com.mikhilnaika.continueapp.feature.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikhilnaika.continueapp.core.data.entity.FriendEntity
import com.mikhilnaika.continueapp.core.friends.FriendRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FriendRowUi(val friend: FriendEntity, val covers: List<String>)

data class FriendsUiState(
    val isLoaded: Boolean = false,
    val friends: List<FriendRowUi> = emptyList(),
)

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val friendRepository: FriendRepository,
) : ViewModel() {

    val state: StateFlow<FriendsUiState> = combine(friendRepository.friends, friendRepository.covers) { friends, covers ->
        val byFriend = covers.groupBy({ it.friendId }, { it.coverUrl })
        FriendsUiState(
            isLoaded = true,
            friends = friends.map { FriendRowUi(it, byFriend[it.id].orEmpty().distinct().take(ROW_COVERS)) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FriendsUiState())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun rename(friendId: Long, name: String) {
        viewModelScope.launch {
            if (!friendRepository.rename(friendId, name)) _message.value = "That name is empty."
        }
    }

    fun remove(friend: FriendEntity) {
        viewModelScope.launch {
            friendRepository.remove(friend.id)
            _message.value = "${friend.name} removed."
        }
    }

    fun resetMyLink() {
        viewModelScope.launch {
            friendRepository.resetMyKey()
            _message.value = "Done. Your next share starts fresh."
        }
    }

    fun messageShown() {
        _message.value = null
    }

    private companion object {
        const val ROW_COVERS = 3
    }
}
