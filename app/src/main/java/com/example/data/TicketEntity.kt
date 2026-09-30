package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tickets")
data class TicketEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ticketNumber: String, // e.g. NEO-2026-001
    val customerId: Long,
    val customerName: String,
    val customerPhone: String,
    val customerEmail: String,
    val serviceType: String, // "Onsite", "Carry-in", "Workshop"
    val deviceName: String,
    val issueDescription: String,
    val locationAddress: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val status: String, // "PENDING", "TECHNICIAN_ASSIGNED", "DIAGNOSTICS", "QUOTATION_SENT", "APPROVED", "REJECTED", "IN_PROGRESS", "READY_FOR_DELIVERY", "COMPLETED"
    val assignedTechnicianId: Long = 0,
    val assignedTechnicianName: String = "",
    val diagnosticNotes: String = "",
    val partsDescription: String = "Spare parts / components",
    val partsCost: Double = 0.0,
    val laborDescription: String = "Service & repair labor",
    val laborCost: Double = 0.0,
    val gstRate: Double = 18.0, // 18% GST standard in India
    val customerApprovalNote: String = "",
    val quotationItemsJson: String = "", // Serialized List<QuotationItem>
    val statusTimelineJson: String = "[]", // Serialized List<TicketTimelineEntry>
    val paymentStatus: String = "PENDING", // "PENDING", "RECEIVED"
    val paymentMode: String = "CASH", // "CASH", "UPI", "CARD", "BANK_TRANSFER"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
