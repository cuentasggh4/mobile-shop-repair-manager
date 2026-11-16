package com.example.data.model

data class StatusLogEntry(
    val stage: RepairStage,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = ""
)
