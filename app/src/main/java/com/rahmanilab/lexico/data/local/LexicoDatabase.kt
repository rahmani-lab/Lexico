package com.rahmanilab.lexico.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.rahmanilab.lexico.data.local.dao.CardDao
import com.rahmanilab.lexico.data.local.dao.CardScheduleDao
import com.rahmanilab.lexico.data.local.dao.DeckDao
import com.rahmanilab.lexico.data.local.dao.ReviewLogDao
import com.rahmanilab.lexico.data.local.dao.TagDao
import com.rahmanilab.lexico.data.local.entity.CardEntity
import com.rahmanilab.lexico.data.local.entity.CardScheduleEntity
import com.rahmanilab.lexico.data.local.entity.CardTagCrossRef
import com.rahmanilab.lexico.data.local.entity.DeckEntity
import com.rahmanilab.lexico.data.local.entity.ReviewLogEntity
import com.rahmanilab.lexico.data.local.entity.TagEntity

@Database(
    entities = [
        DeckEntity::class,
        CardEntity::class,
        CardScheduleEntity::class,
        ReviewLogEntity::class,
        TagEntity::class,
        CardTagCrossRef::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class LexicoDatabase : RoomDatabase() {

    abstract fun deckDao(): DeckDao
    abstract fun cardDao(): CardDao
    abstract fun cardScheduleDao(): CardScheduleDao
    abstract fun reviewLogDao(): ReviewLogDao
    abstract fun tagDao(): TagDao

    companion object {
        const val DATABASE_NAME = "lexico.db"

        fun build(context: Context): LexicoDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                LexicoDatabase::class.java,
                DATABASE_NAME
            )
                // Foreign-key enforcement is on by default in Room; nothing else required for v1.
                .build()
    }
}
