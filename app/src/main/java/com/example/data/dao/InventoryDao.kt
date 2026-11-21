package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.InventoryPartEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {

    @Query("SELECT * FROM inventory_parts ORDER BY partName ASC")
    fun getAllParts(): Flow<List<InventoryPartEntity>>

    @Query("SELECT * FROM inventory_parts WHERE quantityInStock <= minStockThreshold ORDER BY quantityInStock ASC")
    fun getLowStockParts(): Flow<List<InventoryPartEntity>>

    @Query("SELECT * FROM inventory_parts WHERE id = :id")
    suspend fun getPartById(id: Long): InventoryPartEntity?

    @Query("""
        SELECT * FROM inventory_parts 
        WHERE partName LIKE '%' || :query || '%' 
           OR category LIKE '%' || :query || '%' 
           OR compatibleModels LIKE '%' || :query || '%' 
        ORDER BY partName ASC
    """)
    fun searchParts(query: String): Flow<List<InventoryPartEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPart(part: InventoryPartEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(parts: List<InventoryPartEntity>)

    @Update
    suspend fun updatePart(part: InventoryPartEntity)

    @Delete
    suspend fun deletePart(part: InventoryPartEntity)

    @Query("UPDATE inventory_parts SET quantityInStock = quantityInStock - :amount WHERE id = :id AND quantityInStock >= :amount")
    suspend fun deductStock(id: Long, amount: Int): Int

    @Query("SELECT COUNT(*) FROM inventory_parts")
    suspend fun getPartCount(): Int
}
