package com.example.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class QuotationItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String = "Part", // "Part", "Labor", "Service"
    val quantity: Int = 1,
    val unitPrice: Double = 0.0,
    val discount: Double = 0.0, // Manual discount flat amount
    val discountPercent: Double = 0.0 // Optional discount percentage (e.g. 10.0 for 10%)
) {
    val grossTotal: Double get() = quantity * unitPrice

    val effectiveDiscount: Double
        get() = if (discountPercent > 0.0) {
            (grossTotal * (discountPercent / 100.0))
        } else {
            discount.coerceAtLeast(0.0)
        }

    val total: Double get() = (grossTotal - effectiveDiscount).coerceAtLeast(0.0)

    fun toJsonObject(): JSONObject {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("name", name)
        obj.put("category", category)
        obj.put("quantity", quantity)
        obj.put("unitPrice", unitPrice)
        obj.put("discount", discount)
        obj.put("discountPercent", discountPercent)
        return obj
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): QuotationItem {
            return QuotationItem(
                id = obj.optString("id", UUID.randomUUID().toString()),
                name = obj.optString("name", "Item"),
                category = obj.optString("category", "Part"),
                quantity = obj.optInt("quantity", 1),
                unitPrice = obj.optDouble("unitPrice", 0.0),
                discount = obj.optDouble("discount", 0.0),
                discountPercent = obj.optDouble("discountPercent", 0.0)
            )
        }

        fun parseList(jsonString: String?): List<QuotationItem> {
            if (jsonString.isNullOrBlank()) return emptyList()
            val list = mutableListOf<QuotationItem>()
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

        fun toJsonString(items: List<QuotationItem>): String {
            val array = JSONArray()
            items.forEach { array.put(it.toJsonObject()) }
            return array.toString()
        }
    }
}
