package com.example.data.firestore

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreServiceRepository(
    val db: FirebaseFirestore,
    val auth: FirebaseAuth = Firebase.auth
) {
    // Secondary constructor resolving named database ID from string resources
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun currentUser() = auth.currentUser

    // Observe user profile
    fun observeUserProfile(userId: String = requireUserId()): Flow<FirestoreUser?> {
        val path = "users/$userId"
        return db.collection("users").document(userId).snapshots()
            .map { doc ->
                if (doc.exists()) doc.toObject(FirestoreUser::class.java) else null
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.GET, path)
                emit(null)
            }
    }

    // Save or update user profile
    suspend fun saveUserProfile(user: FirestoreUser) {
        val uid = requireUserId()
        val path = "users/$uid"
        try {
            val userMap = mutableMapOf<String, Any>(
                "userId" to uid,
                "email" to (auth.currentUser?.email ?: user.email),
                "fullName" to user.fullName,
                "phone" to user.phone,
                "role" to user.role,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).set(userMap).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            throw e
        }
    }

    // Observe tickets for current user (filtered by owner/customer or admin)
    fun observeUserTickets(isAdmin: Boolean = false): Flow<List<FirestoreTicket>> {
        val uid = requireUserId()
        val path = "tickets"
        val query = if (isAdmin) {
            db.collection("tickets")
        } else {
            db.collection("tickets").whereEqualTo("userId", uid)
        }
        return query.snapshots()
            .map { snap ->
                snap.toObjects(FirestoreTicket::class.java)
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    // Create a new repair ticket
    suspend fun createTicket(ticket: FirestoreTicket): String {
        val uid = requireUserId()
        val path = "tickets"
        val docRef = db.collection("tickets").document()
        try {
            val data = mutableMapOf<String, Any>(
                "ticketNumber" to ticket.ticketNumber,
                "userId" to uid,
                "customerId" to (if (ticket.customerId.isNotBlank()) ticket.customerId else uid),
                "customerName" to ticket.customerName,
                "customerPhone" to ticket.customerPhone,
                "customerEmail" to ticket.customerEmail,
                "serviceType" to ticket.serviceType,
                "deviceName" to ticket.deviceName,
                "issueDescription" to ticket.issueDescription,
                "locationAddress" to ticket.locationAddress,
                "status" to ticket.status,
                "assignedTechnicianId" to ticket.assignedTechnicianId,
                "assignedTechnicianName" to ticket.assignedTechnicianName,
                "diagnosticNotes" to ticket.diagnosticNotes,
                "partsDescription" to ticket.partsDescription,
                "partsCost" to ticket.partsCost,
                "laborDescription" to ticket.laborDescription,
                "laborCost" to ticket.laborCost,
                "gstRate" to ticket.gstRate,
                "customerApprovalNote" to ticket.customerApprovalNote,
                "quotationItemsJson" to ticket.quotationItemsJson,
                "statusTimelineJson" to ticket.statusTimelineJson,
                "paymentStatus" to ticket.paymentStatus,
                "paymentMode" to ticket.paymentMode,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(data).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "${path}/${docRef.id}")
            throw e
        }
    }

    // Update ticket status or payment
    suspend fun updateTicketStatus(
        ticketId: String,
        newStatus: String,
        notes: String = "",
        paymentStatus: String? = null,
        paymentMode: String? = null
    ) {
        val path = "tickets/$ticketId"
        val docRef = db.collection("tickets").document(ticketId)
        try {
            val updates = mutableMapOf<String, Any>(
                "status" to newStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (notes.isNotBlank()) {
                updates["diagnosticNotes"] = notes
            }
            if (paymentStatus != null) {
                updates["paymentStatus"] = paymentStatus
            }
            if (paymentMode != null) {
                updates["paymentMode"] = paymentMode
            }
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            throw e
        }
    }

    // Observe Company Profile
    fun observeCompanyProfile(): Flow<FirestoreCompanyProfile?> {
        val path = "company_profile/default"
        return db.collection("company_profile").document("default").snapshots()
            .map { doc ->
                if (doc.exists()) doc.toObject(FirestoreCompanyProfile::class.java) else null
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.GET, path)
                emit(null)
            }
    }

    // Save Company Profile (Admin)
    suspend fun saveCompanyProfile(profile: FirestoreCompanyProfile) {
        val path = "company_profile/default"
        try {
            val data = mutableMapOf<String, Any>(
                "companyName" to profile.companyName,
                "tagline" to profile.tagline,
                "ticketSeriesPrefix" to profile.ticketSeriesPrefix,
                "address" to profile.address,
                "gstin" to profile.gstin,
                "phone" to profile.phone,
                "email" to profile.email,
                "termsAndConditions" to profile.termsAndConditions,
                "currencySymbol" to profile.currencySymbol,
                "taxEnabled" to profile.taxEnabled,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("company_profile").document("default").set(data).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            throw e
        }
    }
}
