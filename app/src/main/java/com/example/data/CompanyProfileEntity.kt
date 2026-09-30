package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "company_profile")
data class CompanyProfileEntity(
    @PrimaryKey val id: Long = 1,
    val companyName: String = "NEO IT TECH",
    val tagline: String = "Next Gen Computer Solutions & Repairs",
    val ticketSeriesPrefix: String = "NEO-2026-",
    val address: String = "Tech Park, MG Road, Bengaluru, Karnataka - 560001",
    val gstin: String = "29AABCN1234F1Z5",
    val phone: String = "+91 98765 43210",
    val email: String = "info.neoittech@gmail.com",
    val termsAndConditions: String = "1. Service warranty is 30 days on repaired parts.\n2. Estimates are valid for 7 days.\n3. Goods once repaired will be stored for 15 days free of charge.",
    val currencySymbol: String = "₹", // ₹, $, €, £, etc.
    val logoResourceName: String = "ic_neo_launcher_1790656282466",
    val logoUri: String = "", // Optional custom image URI from Android Photo Picker
    val themeMode: String = "SYSTEM", // "LIGHT", "DARK", "SYSTEM"
    val colorTheme: String = "NEO_BLUE", // "NEO_BLUE", "EMERALD", "PURPLE"
    val taxEnabled: Boolean = true, // Enable / Disable GST & Tax ID
    val serviceCenterLat: Double = 12.9716, // Service Center Google Maps Lat
    val serviceCenterLng: Double = 77.5946  // Service Center Google Maps Lng
)
