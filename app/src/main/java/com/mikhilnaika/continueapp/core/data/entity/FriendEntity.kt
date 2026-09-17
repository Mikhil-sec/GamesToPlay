package com.mikhilnaika.continueapp.core.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mikhilnaika.continueapp.core.data.PileState

/**
 * A friend whose pile this device follows — docs/02-PRODUCT-SPEC.md §6 "FRIENDS".
 *
 * Everything here arrived in a link the friend chose to send, except [name], which the user typed
 * on this phone and which never leaves it. There is no server copy: see
 * [com.mikhilnaika.continueapp.core.friends.PileSnapshot].
 */
@Entity(
    tableName = "friends",
    indices = [Index(value = ["publicKey"], unique = true)],
)
data class FriendEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** The friend's pile key; only a link signed by it may update this row. */
    val publicKey: String,
    /** Last applied snapshot's sequence — anything not greater is not an update. */
    val sequence: Long,
    /** When the friend shared it, clamped to [receivedAt] so a fast clock can't claim the future. */
    val sharedAt: Long,
    val receivedAt: Long,
    val addedAt: Long,
    val backlogTotal: Int,
    val playingTotal: Int,
    val clearedTotal: Int,
    val retiredTotal: Int,
    val wantedTotal: Int,
)

/** One game in a friend's pile, in the state they had it. */
@Entity(tableName = "friend_games", primaryKeys = ["friendId", "gameId"])
data class FriendGameEntity(
    val friendId: Long,
    val gameId: Long,
    val state: PileState,
)

/** One line of a friend's HIGH SCORES. */
@Entity(tableName = "friend_ranks", primaryKeys = ["friendId", "position"])
data class FriendRankEntity(
    val friendId: Long,
    val position: Int,
    val gameId: Long,
)
