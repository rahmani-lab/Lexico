package com.rahmanilab.lingodo.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rahmanilab.lingodo.data.local.dao.CardDao
import com.rahmanilab.lingodo.data.local.dao.CardScheduleDao
import com.rahmanilab.lingodo.data.local.dao.DeckDao
import com.rahmanilab.lingodo.data.local.dao.LanguagePairDao
import com.rahmanilab.lingodo.data.local.dao.ReviewLogDao
import com.rahmanilab.lingodo.data.local.dao.TagDao
import com.rahmanilab.lingodo.data.local.entity.CardEntity
import com.rahmanilab.lingodo.data.local.entity.CardScheduleEntity
import com.rahmanilab.lingodo.data.local.entity.CardTagCrossRef
import com.rahmanilab.lingodo.data.local.entity.DeckEntity
import com.rahmanilab.lingodo.data.local.entity.LanguagePairEntity
import com.rahmanilab.lingodo.data.local.entity.ReviewLogEntity
import com.rahmanilab.lingodo.data.local.entity.TagEntity

@Database(
    entities = [
        DeckEntity::class,
        CardEntity::class,
        CardScheduleEntity::class,
        ReviewLogEntity::class,
        TagEntity::class,
        CardTagCrossRef::class,
        LanguagePairEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class LingoDoDatabase : RoomDatabase() {

    abstract fun deckDao(): DeckDao
    abstract fun cardDao(): CardDao
    abstract fun cardScheduleDao(): CardScheduleDao
    abstract fun reviewLogDao(): ReviewLogDao
    abstract fun tagDao(): TagDao
    abstract fun languagePairDao(): LanguagePairDao

    companion object {
        const val DATABASE_NAME = "lingodo.db"

        /**
         * v1 → v2: introduce language pairs (LingoDo upgrade). Adds the `language_pairs` table with
         * a default Persian→English pair, a `languagePairId` column on decks pointing at it, and a
         * `wordForms` column on cards. Non-destructive: existing decks/cards are preserved.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `language_pairs` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`sourceCode` TEXT NOT NULL, " +
                        "`targetCode` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS " +
                        "`index_language_pairs_sourceCode_targetCode` " +
                        "ON `language_pairs` (`sourceCode`, `targetCode`)"
                )
                db.execSQL(
                    "INSERT OR IGNORE INTO `language_pairs` " +
                        "(`id`, `sourceCode`, `targetCode`, `createdAt`) " +
                        "VALUES (1, 'fa', 'en', ${System.currentTimeMillis()})"
                )
                db.execSQL("ALTER TABLE `decks` ADD COLUMN `languagePairId` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_decks_languagePairId` ON `decks` (`languagePairId`)")
                db.execSQL("ALTER TABLE `cards` ADD COLUMN `wordForms` TEXT NOT NULL DEFAULT ''")
            }
        }

        /**
         * v2 → v3: nested decks. Adds a nullable `parentId` on decks (null = top-level "book",
         * set = a "lesson" under that book) plus its index. Non-destructive: every existing deck
         * stays top-level.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `decks` ADD COLUMN `parentId` INTEGER")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_decks_parentId` ON `decks` (`parentId`)")
            }
        }

        fun build(context: Context): LingoDoDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                LingoDoDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
    }
}
