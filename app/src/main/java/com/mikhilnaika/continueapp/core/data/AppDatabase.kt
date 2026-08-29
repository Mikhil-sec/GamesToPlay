package com.mikhilnaika.continueapp.core.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mikhilnaika.continueapp.core.data.dao.DrawDao
import com.mikhilnaika.continueapp.core.data.dao.GameDao
import com.mikhilnaika.continueapp.core.data.dao.PileDao
import com.mikhilnaika.continueapp.core.data.dao.RankingDao
import com.mikhilnaika.continueapp.core.data.dao.StackDao
import com.mikhilnaika.continueapp.core.data.entity.DrawEntity
import com.mikhilnaika.continueapp.core.data.entity.GameEntity
import com.mikhilnaika.continueapp.core.data.entity.PileEntryEntity
import com.mikhilnaika.continueapp.core.data.entity.RankingEntity
import com.mikhilnaika.continueapp.core.data.entity.StackEntity
import com.mikhilnaika.continueapp.core.data.entity.StackMemberEntity

/**
 * Room is the source of truth (docs/05-TECH-ARCHITECTURE.md). The UI reads only from here
 * via Flow; the network (Worker) only ever fills `games`.
 */
@Database(
    entities = [
        GameEntity::class,
        PileEntryEntity::class,
        RankingEntity::class,
        StackEntity::class,
        StackMemberEntity::class,
        DrawEntity::class,
    ],
    // Declared in one place — see DatabaseSchema for why the number and the migrations that
    // service it have to live next to each other.
    version = DatabaseSchema.VERSION,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun pileDao(): PileDao
    abstract fun rankingDao(): RankingDao
    abstract fun stackDao(): StackDao
    abstract fun drawDao(): DrawDao

    companion object {
        const val DATABASE_NAME = "continue.db"
    }
}
