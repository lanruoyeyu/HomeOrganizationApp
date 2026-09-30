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
        MapMarkerEntity::class,
        CategoryEntity::class,
        SubcategoryEntity::class,
        LocationEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun itemDao(): ItemDao
    abstract fun digitalItemDao(): DigitalItemDao
    abstract fun petCategoryDao(): PetCategoryDao
    abstract fun petItemDao(): PetItemDao
    abstract fun mapMarkerDao(): MapMarkerDao
    abstract fun categoryDao(): CategoryDao
    abstract fun subcategoryDao(): SubcategoryDao
    abstract fun locationDao(): LocationDao

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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .addCallback(SEED_CALLBACK)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
