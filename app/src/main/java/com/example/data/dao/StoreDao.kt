package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.CashMovementEntity
import com.example.data.entity.InventoryPartEntity
import com.example.data.entity.PurchaseEntity
import com.example.data.entity.SaleEntity
import com.example.data.entity.SaleItemEntity
import com.example.data.entity.SupplierEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StoreDao {

    // --- Suppliers ---
    @Query("SELECT * FROM suppliers ORDER BY name COLLATE NOCASE ASC")
    fun getAllSuppliers(): Flow<List<SupplierEntity>>

    @Insert
    suspend fun insertSupplier(supplier: SupplierEntity): Long

    @Update
    suspend fun updateSupplier(supplier: SupplierEntity)

    @Delete
    suspend fun deleteSupplier(supplier: SupplierEntity)

    // --- Sales ---
    @Insert
    suspend fun insertSale(sale: SaleEntity): Long

    @Insert
    suspend fun insertSaleItems(items: List<SaleItemEntity>)

    @Query("SELECT * FROM sales WHERE createdAt >= :from AND createdAt < :to ORDER BY createdAt DESC")
    fun getSalesBetween(from: Long, to: Long): Flow<List<SaleEntity>>

    @Query(
        "SELECT si.* FROM sale_items si INNER JOIN sales s ON s.id = si.saleId " +
            "WHERE s.createdAt >= :from AND s.createdAt < :to"
    )
    fun getSaleItemsBetween(from: Long, to: Long): Flow<List<SaleItemEntity>>

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    suspend fun getSaleItems(saleId: Long): List<SaleItemEntity>

    @Query("DELETE FROM sale_items WHERE saleId = :saleId")
    suspend fun deleteSaleItems(saleId: Long)

    @Query("DELETE FROM sales WHERE id = :saleId")
    suspend fun deleteSale(saleId: Long)

    // --- Purchases ---
    @Insert
    suspend fun insertPurchase(purchase: PurchaseEntity): Long

    @Query("SELECT * FROM purchases WHERE createdAt >= :from AND createdAt < :to ORDER BY createdAt DESC")
    fun getPurchasesBetween(from: Long, to: Long): Flow<List<PurchaseEntity>>

    // --- Cash movements ---
    @Insert
    suspend fun insertMovement(movement: CashMovementEntity): Long

    @Delete
    suspend fun deleteMovement(movement: CashMovementEntity)

    @Query("SELECT * FROM cash_movements WHERE createdAt >= :from AND createdAt < :to ORDER BY createdAt DESC")
    fun getMovementsBetween(from: Long, to: Long): Flow<List<CashMovementEntity>>

    // --- Stock helpers (used inside transactions) ---
    @Query("SELECT * FROM inventory_parts WHERE id = :id")
    suspend fun getPart(id: Long): InventoryPartEntity?

    @Query(
        "UPDATE inventory_parts SET quantityInStock = quantityInStock - :amount, updatedAt = :now " +
            "WHERE id = :id AND quantityInStock >= :amount"
    )
    suspend fun deductStockSafe(id: Long, amount: Int, now: Long): Int

    @Query("UPDATE inventory_parts SET quantityInStock = quantityInStock + :amount, updatedAt = :now WHERE id = :id")
    suspend fun addStock(id: Long, amount: Int, now: Long): Int

    @Query(
        "UPDATE inventory_parts SET purchaseCost = :cost, supplierId = :supplierId, updatedAt = :now WHERE id = :id"
    )
    suspend fun setPurchaseInfo(id: Long, cost: Double, supplierId: Long, now: Long): Int
}
