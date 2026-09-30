package com.fridgekeeper.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class SubcategoryCount(
    val subcategoryId: Long,
    val name: String,
    val count: Int
)

@Dao
interface ItemDao {

    @Query("SELECT * FROM items WHERE isCleared = 0 ORDER BY expirationDate ASC")
    fun observeActiveItems(): Flow<List<ItemEntity>>

    @Query(
        "SELECT * FROM items WHERE isCleared = 0 AND subcategoryId IN " +
            "(SELECT id FROM subcategories WHERE categoryId = :categoryId) " +
            "ORDER BY expirationDate ASC"
    )
    fun observeActiveByCategory(categoryId: Long): Flow<List<ItemEntity>>

    @Query(
        "SELECT s.id AS subcategoryId, s.name AS name, IFNULL(SUM(i.quantity), 0) AS count " +
            "FROM subcategories s LEFT JOIN items i ON i.subcategoryId = s.id AND i.isCleared = 0 " +
            "WHERE s.categoryId = :categoryId GROUP BY s.id"
    )
    fun observeSubcategoryCounts(categoryId: Long): Flow<List<SubcategoryCount>>

    @Query("SELECT category, SUM(quantity) as count FROM items WHERE isCleared = 0 GROUP BY category")
    fun observeCategoryCounts(): Flow<List<CategoryCount>>

    @Query("SELECT SUM(quantity) FROM items WHERE isCleared = 0")
    fun observeTotalCount(): Flow<Int?>

    @Insert
    suspend fun insert(item: ItemEntity): Long

    @Update
    suspend fun update(item: ItemEntity)

    @Query("UPDATE items SET quantity = :quantity, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateQuantity(id: Long, quantity: Int, timestamp: Long)

    @Query("UPDATE items SET isCleared = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun markCleared(id: Long, timestamp: Long)
}
