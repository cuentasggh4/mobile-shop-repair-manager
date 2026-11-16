package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class RepairStage(
    val title: String,
    val colorHex: Long,
    val containerHex: Long
) {
    DIAGNOSTIC("Diagnostics", 0xFF2563EB, 0xFFDBEAFE),
    WAITING_FOR_PARTS("Waiting Parts", 0xFFEA580C, 0xFFFFEDD5),
    IN_PROGRESS("In Progress", 0xFF7C3AED, 0xFFEDE9FE),
    READY_FOR_PICKUP("Ready for Pickup", 0xFF059669, 0xFFD1FAE5),
    DELIVERED("Delivered", 0xFF475569, 0xFFF1F5F9),
    CANCELLED("Cancelled", 0xFFDC2626, 0xFFFEE2E2);

    val badgeColor: Color get() = Color(colorHex)
    val badgeContainerColor: Color get() = Color(containerHex)
}

enum class ConditionStatus(val label: String) {
    WORKING("OK"),
    DEFECTIVE("Issue"),
    UNTESTED("N/A")
}

data class IntakeChecklist(
    val screenTouch: ConditionStatus = ConditionStatus.UNTESTED,
    val displayGlass: ConditionStatus = ConditionStatus.UNTESTED,
    val frontCamera: ConditionStatus = ConditionStatus.UNTESTED,
    val rearCamera: ConditionStatus = ConditionStatus.UNTESTED,
    val microphone: ConditionStatus = ConditionStatus.UNTESTED,
    val speaker: ConditionStatus = ConditionStatus.UNTESTED,
    val chargingPort: ConditionStatus = ConditionStatus.UNTESTED,
    val physicalButtons: ConditionStatus = ConditionStatus.UNTESTED,
    val wifiBluetooth: ConditionStatus = ConditionStatus.UNTESTED,
    val biometrics: ConditionStatus = ConditionStatus.UNTESTED,
    val housingCondition: ConditionStatus = ConditionStatus.UNTESTED,
    val simNetwork: ConditionStatus = ConditionStatus.UNTESTED
) {
    fun toStatusMap(): Map<String, ConditionStatus> {
        return mapOf(
            "Screen Touch" to screenTouch,
            "Display / LCD" to displayGlass,
            "Front Camera" to frontCamera,
            "Rear Camera" to rearCamera,
            "Microphone" to microphone,
            "Speaker / Audio" to speaker,
            "Charging Port" to chargingPort,
            "Power / Vol Buttons" to physicalButtons,
            "Wi-Fi / BT" to wifiBluetooth,
            "Fingerprint / FaceID" to biometrics,
            "Body Housing / Dent" to housingCondition,
            "SIM / Cellular" to simNetwork
        )
    }
}
