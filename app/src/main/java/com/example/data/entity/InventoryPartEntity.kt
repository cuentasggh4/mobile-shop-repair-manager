package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventory_parts")
data class InventoryPartEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val partName: String, // e.g. "iPhone 13 OLED Display Assembly"
    val category: String, // Screen, Battery, Charging Port, Camera, Housing, Flex Cable, Other
    val compatibleModels: String, // "iPhone 13, 13 Pro"
    val quantityInStock: Int,
    val minStockThreshold: Int = 3,
    val purchaseCost: Double,
    val sellingPrice: Double,
    val locationRack: String = "Bin A1",
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isLowStock: Boolean
        get() = quantityInStock <= minStockThreshold
}
