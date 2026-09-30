package com.fridgekeeper.data.repository

import com.fridgekeeper.data.local.CategoryDao
import com.fridgekeeper.data.local.CategoryEntity
import com.fridgekeeper.data.local.LocationDao
import com.fridgekeeper.data.local.LocationEntity
import com.fridgekeeper.data.local.SubcategoryDao
import com.fridgekeeper.data.local.SubcategoryEntity
import com.fridgekeeper.model.CategoryKind
import com.fridgekeeper.model.QuantityType
import kotlinx.coroutines.flow.Flow

class CatalogRepository(
    private val categoryDao: CategoryDao,
    private val subcategoryDao: SubcategoryDao,
    private val locationDao: LocationDao
) {
    fun observeCategories(): Flow<List<CategoryEntity>> = categoryDao.observeAll()
    fun observeSubcategories(): Flow<List<SubcategoryEntity>> = subcategoryDao.observeAll()
    fun observeLocations(): Flow<List<LocationEntity>> = locationDao.observeAll()

    suspend fun foodCategoryId(): Long =
        categoryDao.getByKind(CategoryKind.FOOD.name)?.id ?: 0L

    suspend fun addCategory(name: String): Long {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || categoryDao.countByName(trimmed) > 0) return -1L
        return categoryDao.insert(
            CategoryEntity(name = trimmed, kind = CategoryKind.CUSTOM.name, isBuiltin = false)
        )
    }

    suspend fun addSubcategory(
        categoryId: Long,
        name: String,
        quantityMode: QuantityType = QuantityType.COUNT,
        trackDailyCostDefault: Boolean = false
    ): Long {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || categoryId <= 0) return -1L
        if (subcategoryDao.countByName(categoryId, trimmed) > 0) return -1L
        return subcategoryDao.insert(
            SubcategoryEntity(
                categoryId = categoryId,
                name = trimmed,
                quantityMode = quantityMode.name,
                trackDailyCostDefault = trackDailyCostDefault,
                isBuiltin = false
            )
        )
    }

    suspend fun addLocation(room: String, spot: String): Long {
        val r = room.trim()
        val s = spot.trim()
        if (r.isEmpty() || s.isEmpty()) return -1L
        return locationDao.insert(LocationEntity(room = r, spot = s))
    }

    suspend fun updateLocation(entity: LocationEntity) = locationDao.update(entity)

    suspend fun deleteLocation(entity: LocationEntity) = locationDao.delete(entity)
}
