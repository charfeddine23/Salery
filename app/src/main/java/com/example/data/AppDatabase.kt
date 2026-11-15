package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Converters {
    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = runCatching {
        TransactionType.valueOf(value)
    }.getOrDefault(TransactionType.EXPENSE)

    @TypeConverter
    fun fromDebtType(value: DebtType): String = value.name

    @TypeConverter
    fun toDebtType(value: String): DebtType = runCatching {
        DebtType.valueOf(value)
    }.getOrDefault(DebtType.OWED_BY_ME)
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        var hasColumn = false
        db.query("PRAGMA table_info(transactions)").use { cursor ->
            val nameIdx = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                if (nameIdx >= 0 && cursor.getString(nameIdx) == "isPaidFromSavings") {
                    hasColumn = true
                    break
                }
            }
        }
        if (!hasColumn) {
            db.execSQL("ALTER TABLE transactions ADD COLUMN isPaidFromSavings INTEGER NOT NULL DEFAULT 0")
        }
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `archived_alg_post_sms` (
                `id` TEXT NOT NULL,
                `sender` TEXT NOT NULL,
                `rawBody` TEXT NOT NULL,
                `receivedAtMillis` INTEGER NOT NULL,
                `capturedAtMillis` INTEGER NOT NULL,
                `captureSource` TEXT NOT NULL,
                `extractedBalance` REAL,
                `extractedTxAmount` REAL,
                `extractedIsExpense` INTEGER,
                `hasExplicitFeeKeyword` INTEGER NOT NULL,
                `matchedRule` TEXT NOT NULL,
                `studyStatus` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
    }
}

@Database(
    entities = [TransactionEntity::class, DebtEntity::class, ArchivedAlgPosteSmsEntity::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "salerypelse_database"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
