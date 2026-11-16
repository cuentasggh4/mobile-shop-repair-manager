package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shop_profile")
data class ShopProfileEntity(
    @PrimaryKey
    val id: Int = 1,
    val shopName: String = "Mobile Repair Shop",
    val tagline: String = "",
    val contactPhone: String = "",
    val whatsappNumber: String = "",
    val email: String = "",
    val address: String = "",
    val currencySymbol: String = "₹",
    val warrantyDays: Int = 90,
    val termsAndConditions: String = "",
    val nextJobNumberSequence: Int = 1,
    val jobNumberPrefix: String = "JOB",
    val lastJobMonth: String = "",
    val preferredWhatsAppPackage: String = "auto",
    val updatedAt: Long = 0L
)
