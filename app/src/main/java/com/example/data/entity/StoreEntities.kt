package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/** A point-of-sale ticket (accessories, parts, quick services sold over the counter). */
@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerName: String = "",
    val subtotal: Double,
    val discount: Double = 0.0,
    val total: Double,
    val costTotal: Double = 0.0,
    val paymentMode: String = "Cash",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sale_items")
data class SaleItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val saleId: Long,
    val partId: Long = 0, // 0 = free-form item (service, misc.) that is not tracked in stock
    val name: String,
    val quantity: Int,
    val unitPrice: Double,
    val unitCost: Double = 0.0
)

/** Stock received from a supplier. */
@Entity(tableName = "purchases")
data class PurchaseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val supplierId: Long = 0,
    val supplierName: String = "",
    val partId: Long,
    val partName: String,
    val quantity: Int,
    val unitCost: Double,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Cash register movement. type is "IN" (manual deposit), "EXPENSE", or "REPAIR"
 * (money collected on a job card: advance, partial payment or final payment; negative = reversal).
 */
@Entity(tableName = "cash_movements")
data class CashMovementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,
    val amount: Double,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val jobId: Long = 0,
    val paymentMode: String = "Cash"
)
