package com.mikhilnaika.continueapp.core.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromPileState(value: PileState): String = value.name

    @TypeConverter
    fun toPileState(value: String): PileState = PileState.valueOf(value)

    @TypeConverter
    fun fromAddSource(value: AddSource): String = value.name

    @TypeConverter
    fun toAddSource(value: String): AddSource = AddSource.valueOf(value)

    @TypeConverter
    fun fromRankBucket(value: RankBucket): String = value.name

    @TypeConverter
    fun toRankBucket(value: String): RankBucket = RankBucket.valueOf(value)

    @TypeConverter
    fun fromTimeBudget(value: TimeBudget): String = value.name

    @TypeConverter
    fun toTimeBudget(value: String): TimeBudget = TimeBudget.valueOf(value)

    @TypeConverter
    fun fromMood(value: Mood): String = value.name

    @TypeConverter
    fun toMood(value: String): Mood = Mood.valueOf(value)
}
