package com.example.util

import androidx.annotation.StringRes
import com.example.R
import com.example.data.model.ConditionStatus
import com.example.data.model.RepairStage

/** Maps stored identifiers (enum values and checklist keys) to translatable text. */
object Labels {

    @StringRes
    fun stageRes(stage: RepairStage): Int = when (stage) {
        RepairStage.DIAGNOSTIC -> R.string.status_diagnostics
        RepairStage.WAITING_FOR_PARTS -> R.string.status_waiting_parts
        RepairStage.IN_PROGRESS -> R.string.status_in_progress
        RepairStage.READY_FOR_PICKUP -> R.string.stage_ready_for_pickup
        RepairStage.DELIVERED -> R.string.status_delivered
        RepairStage.CANCELLED -> R.string.status_cancelled
    }

    @StringRes
    fun conditionRes(status: ConditionStatus): Int = when (status) {
        ConditionStatus.WORKING -> R.string.check_status_ok
        ConditionStatus.DEFECTIVE -> R.string.check_status_issue
        ConditionStatus.UNTESTED -> R.string.check_status_na
    }

    /** Keys are the identifiers produced by IntakeChecklist.toStatusMap(); they are stored data and never translated. */
    @StringRes
    fun checklistRes(key: String): Int? = when (key) {
        "Screen Touch" -> R.string.check_touch
        "Display / LCD" -> R.string.check_display
        "Front Camera" -> R.string.check_camera_front
        "Rear Camera" -> R.string.check_camera_rear
        "Microphone" -> R.string.check_mic
        "Speaker / Audio" -> R.string.check_speaker
        "Charging Port" -> R.string.check_port
        "Power / Vol Buttons" -> R.string.check_buttons
        "Wi-Fi / BT" -> R.string.check_wireless
        "Fingerprint / FaceID" -> R.string.check_biometrics
        "Body Housing / Dent" -> R.string.check_housing
        "SIM / Cellular" -> R.string.check_sim
        else -> null
    }
}
