package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.dao.InventoryDao
import com.example.data.dao.JobCardDao
import com.example.data.dao.ShopProfileDao
import com.example.data.dao.StoreDao
import com.example.data.entity.CashMovementEntity
import com.example.data.entity.InventoryPartEntity
import com.example.data.entity.JobCardEntity
import com.example.data.entity.PriceItemEntity
import com.example.data.entity.PurchaseEntity
import com.example.data.entity.SaleEntity
import com.example.data.entity.SaleItemEntity
import com.example.data.entity.ShopProfileEntity
import com.example.data.entity.SupplierEntity

@Database(
    entities = [
        JobCardEntity::class,
        InventoryPartEntity::class,
        ShopProfileEntity::class,
        SupplierEntity::class,
        SaleEntity::class,
        SaleItemEntity::class,
        PurchaseEntity::class,
        CashMovementEntity::class,
        PriceItemEntity::class
    ],
    version = 9,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun jobCardDao(): JobCardDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun shopProfileDao(): ShopProfileDao
    abstract fun storeDao(): StoreDao

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

        // v7: store module (suppliers, POS sales, purchases, cash register) + barcode/supplier on parts.
        // Existing job cards, parts and shop profile are preserved.
        val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE inventory_parts ADD COLUMN barcode TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE inventory_parts ADD COLUMN supplierId INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `suppliers` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, `phone` TEXT NOT NULL, `notes` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `sales` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`customerName` TEXT NOT NULL, `subtotal` REAL NOT NULL, `discount` REAL NOT NULL, " +
                        "`total` REAL NOT NULL, `costTotal` REAL NOT NULL, `paymentMode` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `sale_items` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`saleId` INTEGER NOT NULL, `partId` INTEGER NOT NULL, `name` TEXT NOT NULL, " +
                        "`quantity` INTEGER NOT NULL, `unitPrice` REAL NOT NULL, `unitCost` REAL NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `purchases` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`supplierId` INTEGER NOT NULL, `supplierName` TEXT NOT NULL, " +
                        "`partId` INTEGER NOT NULL, `partName` TEXT NOT NULL, `quantity` INTEGER NOT NULL, " +
                        "`unitCost` REAL NOT NULL, `createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `cash_movements` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`type` TEXT NOT NULL, `amount` REAL NOT NULL, `note` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
            }
        }

        // v8: money collected on job cards (advances, payments, delivery) is recorded in the cash register.
        val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cash_movements ADD COLUMN jobId INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE cash_movements ADD COLUMN paymentMode TEXT NOT NULL DEFAULT 'Cash'")
                // Record what existing jobs have already collected so past totals stay correct.
                db.execSQL(
                    "INSERT INTO cash_movements (type, amount, note, createdAt, jobId, paymentMode) " +
                        "SELECT 'REPAIR', amt, '#' || jobNumber, COALESCE(deliveredAt, updatedAt), id, " +
                        "CASE WHEN paymentMode = '' THEN 'Cash' ELSE paymentMode END " +
                        "FROM (SELECT *, CASE WHEN isPaid = 1 THEN MAX((CASE WHEN finalCost > 0 THEN finalCost ELSE estimatedCost END) - discount, 0) " +
                        "ELSE advancePaid END AS amt FROM job_cards WHERE stage != 'CANCELLED') WHERE amt > 0"
                )
            }
        }

        // v9: supplier price lists imported from PDF for the quoter.
        val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `price_items` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, `category` TEXT NOT NULL, `cost` REAL NOT NULL, " +
                        "`source` TEXT NOT NULL, `importedAt` INTEGER NOT NULL)"
                )
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
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                 .fallbackToDestructiveMigration()
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
