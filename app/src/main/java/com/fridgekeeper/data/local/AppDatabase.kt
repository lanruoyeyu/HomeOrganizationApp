package com.fridgekeeper.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fridgekeeper.model.CategoryKind
import com.fridgekeeper.model.ItemCategory

@Database(
    entities = [
        ItemEntity::class,
        DigitalItemEntity::class,
        PetCategoryEntity::class,
        PetItemEntity::class,
        MedicineEntity::class,
        MapMarkerEntity::class,
        MapPlaceEntity::class,
        CategoryEntity::class,
        SubcategoryEntity::class,
        LocationEntity::class,
        UsageRecordEntity::class,
        LedgerEntryEntity::class,
        CouponEntity::class,
        CheckinTaskEntity::class,
        CheckinRecordEntity::class
    ],
    version = 11,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun itemDao(): ItemDao
    abstract fun digitalItemDao(): DigitalItemDao
    abstract fun petCategoryDao(): PetCategoryDao
    abstract fun petItemDao(): PetItemDao
    abstract fun medicineDao(): MedicineDao
    abstract fun mapMarkerDao(): MapMarkerDao
    abstract fun mapPlaceDao(): MapPlaceDao
    abstract fun categoryDao(): CategoryDao
    abstract fun subcategoryDao(): SubcategoryDao
    abstract fun locationDao(): LocationDao
    abstract fun usageRecordDao(): UsageRecordDao
    abstract fun ledgerDao(): LedgerDao
    abstract fun couponDao(): CouponDao
    abstract fun checkinDao(): CheckinDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `digital_items` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`purchasePrice` REAL NOT NULL, " +
                        "`purchaseDate` INTEGER NOT NULL, " +
                        "`sellPrice` REAL, " +
                        "`sellDate` INTEGER, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL)"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                createPetTables(db)
                seedDefaultPetCategories(db)
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `map_markers` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`itemType` TEXT NOT NULL, " +
                        "`itemId` INTEGER NOT NULL, " +
                        "`itemDisplayName` TEXT NOT NULL, " +
                        "`posX` REAL NOT NULL, " +
                        "`posY` REAL NOT NULL, " +
                        "`roomLabel` TEXT, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                createOrganizationTables(db)
                addItemV5Columns(db)
                val foodId = seedBuiltinCategories(db)
                migrateLegacyFoodCategories(db, foodId)
                db.execSQL("ALTER TABLE `digital_items` ADD COLUMN `locationId` INTEGER")
                db.execSQL("ALTER TABLE `digital_items` ADD COLUMN `trackDailyCost` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `pet_items` ADD COLUMN `locationId` INTEGER")
                db.execSQL("ALTER TABLE `pet_items` ADD COLUMN `trackDailyCost` INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `medicines` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`expiryDate` INTEGER NOT NULL, " +
                        "`price` REAL, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL)"
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `map_places` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`posX` REAL NOT NULL, " +
                        "`posY` REAL NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `usage_records` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`itemType` TEXT NOT NULL, " +
                        "`itemId` INTEGER NOT NULL, " +
                        "`itemName` TEXT NOT NULL, " +
                        "`usageText` TEXT NOT NULL, " +
                        "`usedAt` INTEGER NOT NULL, " +
                        "`scene` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `ledger_entries` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`type` TEXT NOT NULL, " +
                        "`amount` REAL NOT NULL, " +
                        "`category` TEXT NOT NULL, " +
                        "`paymentMethod` TEXT NOT NULL, " +
                        "`note` TEXT NOT NULL, " +
                        "`occurredAt` INTEGER NOT NULL, " +
                        "`rawInput` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `coupons` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`source` TEXT NOT NULL, " +
                        "`type` TEXT NOT NULL, " +
                        "`threshold` REAL NOT NULL, " +
                        "`amount` REAL NOT NULL, " +
                        "`validFrom` INTEGER, " +
                        "`validUntil` INTEGER NOT NULL, " +
                        "`scene` TEXT NOT NULL, " +
                        "`isUsed` INTEGER NOT NULL, " +
                        "`usedAt` INTEGER, " +
                        "`note` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `checkin_tasks` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`platform` TEXT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`cycle` TEXT NOT NULL, " +
                        "`lastCheckinAt` INTEGER, " +
                        "`remindHour` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `checkin_records` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`taskId` INTEGER NOT NULL, " +
                        "`checkedAt` INTEGER NOT NULL, " +
                        "`note` TEXT NOT NULL)"
                )
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `ledger_entries` ADD COLUMN `subcategory` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `ledger_entries` ADD COLUMN `account2` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `ledger_entries` ADD COLUMN `tags` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `ledger_entries` ADD COLUMN `book` TEXT NOT NULL DEFAULT '日常账本'")
                db.execSQL("ALTER TABLE `ledger_entries` ADD COLUMN `currency` TEXT NOT NULL DEFAULT 'CNY'")
                db.execSQL("ALTER TABLE `ledger_entries` ADD COLUMN `typeLabel` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `ledger_entries` ADD COLUMN `refund` REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `ledger_entries` ADD COLUMN `discount` REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `ledger_entries` ADD COLUMN `fee` REAL NOT NULL DEFAULT 0")
            }
        }

        private val SEED_CALLBACK = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                seedDefaultPetCategories(db)
                seedBuiltinCategories(db)
            }
        }

        private fun createPetTables(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `pet_categories` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`name` TEXT NOT NULL, " +
                    "`isDefault` INTEGER NOT NULL, " +
                    "`createdAt` INTEGER NOT NULL)"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `pet_items` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`name` TEXT NOT NULL, " +
                    "`categoryId` INTEGER NOT NULL, " +
                    "`purchasePrice` REAL NOT NULL, " +
                    "`purchaseDate` INTEGER NOT NULL, " +
                    "`quantityType` TEXT NOT NULL, " +
                    "`weightUnit` TEXT, " +
                    "`quantity` REAL NOT NULL, " +
                    "`tags` TEXT NOT NULL, " +
                    "`createdAt` INTEGER NOT NULL, " +
                    "`updatedAt` INTEGER NOT NULL)"
            )
        }

        private fun createOrganizationTables(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `categories` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`name` TEXT NOT NULL, " +
                    "`kind` TEXT NOT NULL, " +
                    "`isBuiltin` INTEGER NOT NULL, " +
                    "`createdAt` INTEGER NOT NULL)"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `subcategories` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`categoryId` INTEGER NOT NULL, " +
                    "`name` TEXT NOT NULL, " +
                    "`quantityMode` TEXT NOT NULL, " +
                    "`trackDailyCostDefault` INTEGER NOT NULL, " +
                    "`isBuiltin` INTEGER NOT NULL, " +
                    "`createdAt` INTEGER NOT NULL)"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `locations` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`room` TEXT NOT NULL, " +
                    "`spot` TEXT NOT NULL, " +
                    "`createdAt` INTEGER NOT NULL)"
            )
        }

        private fun addItemV5Columns(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `items` ADD COLUMN `subcategoryId` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `items` ADD COLUMN `locationId` INTEGER")
            db.execSQL("ALTER TABLE `items` ADD COLUMN `trackDailyCost` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `items` ADD COLUMN `buyPrice` REAL")
            db.execSQL("ALTER TABLE `items` ADD COLUMN `buyAt` INTEGER")
        }

        private fun seedBuiltinCategories(db: SupportSQLiteDatabase): Long {
            val now = System.currentTimeMillis()
            db.execSQL(
                "INSERT INTO `categories` (`name`, `kind`, `isBuiltin`, `createdAt`) VALUES " +
                    "('食品', '${CategoryKind.FOOD.name}', 1, $now), " +
                    "('数码产品', '${CategoryKind.DIGITAL.name}', 1, $now), " +
                    "('宠物用品', '${CategoryKind.PET.name}', 1, $now)"
            )
            val cursor = db.query("SELECT `id` FROM `categories` WHERE `kind` = '${CategoryKind.FOOD.name}' LIMIT 1")
            cursor.use {
                return if (it.moveToFirst()) it.getLong(0) else 0L
            }
        }

        private fun migrateLegacyFoodCategories(db: SupportSQLiteDatabase, foodCategoryId: Long) {
            if (foodCategoryId <= 0) return
            val now = System.currentTimeMillis()
            ItemCategory.entries.forEach { cat ->
                db.execSQL(
                    "INSERT INTO `subcategories` (`categoryId`, `name`, `quantityMode`, `trackDailyCostDefault`, `isBuiltin`, `createdAt`) " +
                        "VALUES ($foodCategoryId, '${cat.displayName}', 'COUNT', 0, 1, $now)"
                )
                val cursor = db.query(
                    "SELECT `id` FROM `subcategories` WHERE `categoryId` = $foodCategoryId AND `name` = '${cat.displayName}' LIMIT 1"
                )
                val subId = cursor.use { if (it.moveToFirst()) it.getLong(0) else 0L }
                if (subId > 0) {
                    db.execSQL(
                        "UPDATE `items` SET `subcategoryId` = $subId WHERE `category` = '${cat.name}'"
                    )
                }
            }
        }

        private fun seedDefaultPetCategories(db: SupportSQLiteDatabase) {
            val now = System.currentTimeMillis()
            listOf("猫粮", "猫砂", "猫玩具").forEach { name ->
                db.execSQL(
                    "INSERT INTO `pet_categories` (`name`, `isDefault`, `createdAt`) " +
                        "VALUES ('$name', 1, $now)"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fridge_keeper.db"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                        MIGRATION_10_11
                    )
                    .addCallback(SEED_CALLBACK)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
