package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.RepairStage

@Entity(tableName = "job_cards")
data class JobCardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recordUuid: String = java.util.UUID.randomUUID().toString(),
    val jobNumber: String, // e.g. JOB-1001
    val customerName: String,
    val customerPhone: String,
    val deviceBrand: String, // Apple, Samsung, Xiaomi, etc.
    val deviceModel: String, // e.g. iPhone 13 Pro
    val deviceColor: String = "",
    val imeiOrSerial: String = "",
    val securityLockCode: String = "", // PIN or pattern note
    val issueDescription: String,
    val issueTagsCsv: String = "", // Comma-separated: Screen,Battery,Charging
    val checklistJson: String = "", // Serialized 12-point condition status
    val stage: RepairStage = RepairStage.DIAGNOSTIC,
    val estimatedCost: Double = 0.0,
    val advancePaid: Double = 0.0,
    val discount: Double = 0.0,
    val finalCost: Double = 0.0,
    val isPaid: Boolean = false,
    val paymentMode: String = "Cash", // Cash, UPI, Card, Transfer
    val technicianNotes: String = "",
    val linkedPartIdsCsv: String = "", // IDs of spare parts consumed
    val statusLogsJson: String = "", // JSON array of StatusLogEntry history
    val pickedUpByOriginalCustomer: Boolean = true, // Whether original customer picked up or representative
    val recipientName: String = "", // Name of person who picked up if not original customer
    val recipientPhone: String = "", // Phone of person who picked up if not original customer
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deliveredAt: Long? = null
) {
    val statusLogs: List<com.example.data.model.StatusLogEntry>
        get() = com.example.data.database.Converters.deserializeStatusLogs(statusLogsJson)

    val balanceDue: Double
        get() {
            val total = if (finalCost > 0.0) finalCost else estimatedCost
            val net = (total - discount - advancePaid).coerceAtLeast(0.0)
            return if (isPaid) 0.0 else net
        }
}
