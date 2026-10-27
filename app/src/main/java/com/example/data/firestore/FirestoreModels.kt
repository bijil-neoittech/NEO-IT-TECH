package com.example.data.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class FirestoreUser(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val fullName: String = "",
    val phone: String = "",
    val role: String = "CUSTOMER", // "ADMIN", "TECHNICIAN", "CUSTOMER"
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreTicket(
    @DocumentId val id: String = "",
    val ticketNumber: String = "",
    val userId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val customerEmail: String = "",
    val serviceType: String = "In-Store",
    val deviceName: String = "",
    val issueDescription: String = "",
    val locationAddress: String = "",
    val status: String = "PENDING",
    val assignedTechnicianId: String = "",
    val assignedTechnicianName: String = "",
    val diagnosticNotes: String = "",
    val partsDescription: String = "",
    val partsCost: Double = 0.0,
    val laborDescription: String = "",
    val laborCost: Double = 0.0,
    val gstRate: Double = 18.0,
    val customerApprovalNote: String = "",
    val quotationItemsJson: String = "",
    val statusTimelineJson: String = "[]",
    val paymentStatus: String = "PENDING",
    val paymentMode: String = "CASH",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreCompanyProfile(
    @DocumentId val id: String = "default",
    val companyName: String = "NEO IT TECH",
    val tagline: String = "Next Gen Computer Solutions & Repairs",
    val ticketSeriesPrefix: String = "NEO-2026-",
    val address: String = "Tech Park, MG Road, Bengaluru, Karnataka - 560001",
    val gstin: String = "29AABCN1234F1Z5",
    val phone: String = "+91 98765 43210",
    val email: String = "info.neoittech@gmail.com",
    val termsAndConditions: String = "",
    val currencySymbol: String = "₹",
    val taxEnabled: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
