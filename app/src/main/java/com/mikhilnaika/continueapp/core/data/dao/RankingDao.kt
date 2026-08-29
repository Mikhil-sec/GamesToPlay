package com.mikhilnaika.continueapp.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.mikhilnaika.continueapp.core.data.RankBucket
import com.mikhilnaika.continueapp.core.data.entity.RankingEntity
import kotlinx.coroutines.flow.Flow

/**
 * An abstract class rather than an interface purely so [reorder] can be a concrete
 * `@Transaction` method — the form Room can reliably wrap. In a Kotlin interface the body would
 * compile into `DefaultImpls`, out of reach of Room's generated override.
 */
@Dao
abstract class RankingDao {
    @Query("SELECT * FROM rankings ORDER BY position ASC")
    abstract fun observeAll(): Flow<List<RankingEntity>>

    @Query("SELECT * FROM rankings ORDER BY position ASC")
    abstract suspend fun getAll(): List<RankingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsert(ranking: RankingEntity)

    @Query("UPDATE rankings SET position = position + 1 WHERE position >= :fromPosition")
    abstract suspend fun shiftDown(fromPosition: Int)

    /**
     * Same reason as `StackDao.removeFromAllStacks` — no foreign key ties a ranking to its pile
     * entry, so a game removed from the pile would otherwise keep its slot in RANK forever with
     * nothing behind it. Leaves the surrounding positions alone: they're a total order, and the
     * gap it opens is invisible because RANK reads them by `ORDER BY position`, not by value.
     */
    @Query("DELETE FROM rankings WHERE gameId = :gameId")
    abstract suspend fun deleteByGameId(gameId: Long)

    @Query("UPDATE rankings SET position = :position, bucket = :bucket WHERE gameId = :gameId")
    abstract suspend fun setPlacement(gameId: Long, position: Int, bucket: RankBucket)

    /**
     * Rewrites the whole leaderboard from an explicit order — what a manual move up or down does.
     *
     * Renumbers densely from 0 rather than swapping two rows. Positions arrive with gaps in
     * them by design ([deleteByGameId] leaves one behind, and [shiftDown] only ever pushes
     * downward), so "swap the two numbers" would be correct only by luck. One transaction, so a
     * leaderboard can never be observed half-renumbered.
     *
     * Bucket comes along for the ride, because a manual move can leave a game somewhere its old
     * bucket contradicts. Letting the two drift would corrupt the next *automatic* placement:
     * [com.mikhilnaika.continueapp.feature.rank.PairwiseRanker] binary-searches within a bucket
     * and assumes each bucket occupies one contiguous run of positions.
     */
    @Transaction
    open suspend fun reorder(placements: List<RankPlacement>) {
        placements.forEachIndexed { index, placement ->
            setPlacement(placement.gameId, index, placement.bucket)
        }
    }
}

/** One row's target slot for [RankingDao.reorder]. */
data class RankPlacement(val gameId: Long, val bucket: RankBucket)
