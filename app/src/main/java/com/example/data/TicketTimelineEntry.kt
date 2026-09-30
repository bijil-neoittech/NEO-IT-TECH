package com.example.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class TicketTimelineEntry(
    val id: String = UUID.randomUUID().toString(),
    val status: String,
    val updatedByName: String,
    val updatedByRole: String, // "ADMIN", "TECHNICIAN", "STAFF", "CUSTOMER"
    val remarks: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJsonObject(): JSONObject {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("status", status)
        obj.put("updatedByName", updatedByName)
        obj.put("updatedByRole", updatedByRole)
        obj.put("remarks", remarks)
        obj.put("timestamp", timestamp)
        return obj
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): TicketTimelineEntry {
            return TicketTimelineEntry(
                id = obj.optString("id", UUID.randomUUID().toString()),
                status = obj.optString("status", "PENDING"),
                updatedByName = obj.optString("updatedByName", "System"),
                updatedByRole = obj.optString("updatedByRole", "STAFF"),
                remarks = obj.optString("remarks", ""),
                timestamp = obj.optLong("timestamp", System.currentTimeMillis())
            )
        }

        fun parseList(jsonString: String?): List<TicketTimelineEntry> {
            if (jsonString.isNullOrBlank()) return emptyList()
            val list = mutableListOf<TicketTimelineEntry>()
            try {
                val array = JSONArray(jsonString)
                for (i in 0 until array.length()) {
                    list.add(fromJsonObject(array.getJSONObject(i)))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return list
        }

        fun toJsonString(entries: List<TicketTimelineEntry>): String {
            val array = JSONArray()
            entries.forEach { array.put(it.toJsonObject()) }
            return array.toString()
        }
    }
}
