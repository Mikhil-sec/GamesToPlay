package com.mikhilnaika.continueapp.feature.rank

import com.mikhilnaika.continueapp.core.data.RankBucket

enum class RankPhase { LOADING, BUCKET_SELECT, COMPARING, VERDICT, DONE }

data class RankOpponent(
    val gameId: Long,
    val name: String,
    val coverUrl: String?,
)

data class RankUiState(
    val phase: RankPhase = RankPhase.LOADING,
    val gameId: Long = 0,
    val gameName: String = "",
    val coverUrl: String? = null,
    val selectedBucket: RankBucket? = null,
    val currentOpponent: RankOpponent? = null,
    val comparisonsUsed: Int = 0,
    val verdictText: String = "",
    val wouldReplay: Boolean = false,
    val finalPosition: Int? = null,
)
