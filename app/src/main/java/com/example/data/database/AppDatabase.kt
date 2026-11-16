package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.dao.InventoryDao
import com.example.data.dao.JobCardDao
import com.example.data.dao.ShopProfileDao
import com.example.data.entity.InventoryPartEntity
import com.example.data.entity.JobCardEntity
import com.example.data.entity.ShopProfileEntity

@Database(
    entities = [
        JobCardEntity::class,
        InventoryPartEntity::class,
        ShopProfileEntity::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun jobCardDao(): JobCardDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun shopProfileDao(): ShopProfileDao

    companion object {
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE shop_profile ADD COLUMN preferredWhatsAppPackage TEXT NOT NULL DEFAULT 'auto'")
            }
        }

        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE job_cards ADD COLUMN statusLogsJson TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE job_cards ADD COLUMN pickedUpByOriginalCustomer INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE job_cards ADD COLUMN recipientName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE job_cards ADD COLUMN recipientPhone TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE job_cards ADD COLUMN recordUuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE shop_profile ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE shop_profile ADD COLUMN jobNumberPrefix TEXT NOT NULL DEFAULT 'JOB'")
                db.execSQL("ALTER TABLE shop_profile ADD COLUMN lastJobMonth TEXT NOT NULL DEFAULT ''")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mobile_repair_shop.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                 .fallbackToDestructiveMigration()
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
