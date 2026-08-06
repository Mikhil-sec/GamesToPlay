package com.mikhilnaika.continueapp.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mikhilnaika.continueapp.core.data.RankBucket

/** docs/02-PRODUCT-SPEC.md §5 — one row per ranked game; position is the global ordinal. */
@Entity(tableName = "rankings")
data class RankingEntity(
    @PrimaryKey val gameId: Long,
    val bucket: RankBucket,
    val position: Int,
    val verdict: String? = null,
    val wouldReplay: Boolean = false,
    val rankedAt: Long,
)
