package com.example.data.database

import androidx.room.TypeConverter
import com.example.data.model.ConditionStatus
import com.example.data.model.IntakeChecklist
import com.example.data.model.RepairStage

class Converters {

    @TypeConverter
    fun fromRepairStage(stage: RepairStage?): String {
        return stage?.name ?: RepairStage.DIAGNOSTIC.name
    }

    @TypeConverter
    fun toRepairStage(value: String?): RepairStage {
        return try {
            if (value != null) RepairStage.valueOf(value) else RepairStage.DIAGNOSTIC
        } catch (e: Exception) {
            RepairStage.DIAGNOSTIC
        }
    }

    companion object {
        fun serializeChecklist(checklist: IntakeChecklist): String {
            return "${checklist.screenTouch.name};${checklist.displayGlass.name};${checklist.frontCamera.name};" +
                    "${checklist.rearCamera.name};${checklist.microphone.name};${checklist.speaker.name};" +
                    "${checklist.chargingPort.name};${checklist.physicalButtons.name};${checklist.wifiBluetooth.name};" +
                    "${checklist.biometrics.name};${checklist.housingCondition.name};${checklist.simNetwork.name}"
        }

        fun deserializeChecklist(raw: String): IntakeChecklist {
            if (raw.isBlank()) return IntakeChecklist()
            val parts = raw.split(";")
            if (parts.size < 12) return IntakeChecklist()
            fun parseStatus(s: String): ConditionStatus {
                return try { ConditionStatus.valueOf(s) } catch (e: Exception) { ConditionStatus.UNTESTED }
            }
            return IntakeChecklist(
                screenTouch = parseStatus(parts[0]),
                displayGlass = parseStatus(parts[1]),
                frontCamera = parseStatus(parts[2]),
                rearCamera = parseStatus(parts[3]),
                microphone = parseStatus(parts[4]),
                speaker = parseStatus(parts[5]),
                chargingPort = parseStatus(parts[6]),
                physicalButtons = parseStatus(parts[7]),
                wifiBluetooth = parseStatus(parts[8]),
                biometrics = parseStatus(parts[9]),
                housingCondition = parseStatus(parts[10]),
                simNetwork = parseStatus(parts[11])
            )
        }

        fun serializeStatusLogs(logs: List<com.example.data.model.StatusLogEntry>): String {
            if (logs.isEmpty()) return ""
            val jsonArray = org.json.JSONArray()
            for (entry in logs) {
                val obj = org.json.JSONObject()
                obj.put("stage", entry.stage.name)
                obj.put("timestamp", entry.timestamp)
                obj.put("note", entry.note)
                jsonArray.put(obj)
            }
            return jsonArray.toString()
        }

        fun deserializeStatusLogs(raw: String): List<com.example.data.model.StatusLogEntry> {
            if (raw.isBlank()) return emptyList()
            return try {
                val jsonArray = org.json.JSONArray(raw)
                val list = ArrayList<com.example.data.model.StatusLogEntry>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val stageStr = obj.optString("stage", com.example.data.model.RepairStage.DIAGNOSTIC.name)
                    val stage = try {
                        com.example.data.model.RepairStage.valueOf(stageStr)
                    } catch (e: Exception) {
                        com.example.data.model.RepairStage.DIAGNOSTIC
                    }
                    val timestamp = obj.optLong("timestamp", 0L)
                    val note = obj.optString("note", "")
                    list.add(com.example.data.model.StatusLogEntry(stage, timestamp, note))
                }
                list
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}
