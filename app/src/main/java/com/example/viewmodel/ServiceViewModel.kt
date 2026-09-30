package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.CompanyProfileEntity
import com.example.data.ServiceRepository
import com.example.data.TicketEntity
import com.example.data.UserEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class ServiceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ServiceRepository
    private val prefs = application.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)

    private val _isOnboardingCompleted = MutableStateFlow(prefs.getBoolean("onboarding_completed", true))
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

    init {
        val dao = AppDatabase.getDatabase(application).serviceDao()
        repository = ServiceRepository(dao)
        viewModelScope.launch {
            if (!prefs.getBoolean("demo_cleared_v2", false)) {
                repository.deleteAllTickets()
                prefs.edit().putBoolean("demo_cleared_v2", true).apply()
            }
        }
    }

    fun completeOnboarding(
        fullName: String,
        phone: String,
        email: String,
        role: String,
        businessName: String
    ) {
        viewModelScope.launch {
            if (businessName.isNotBlank()) {
                val currentProfile = companyProfile.value ?: CompanyProfileEntity()
                repository.saveCompanyProfile(currentProfile.copy(companyName = businessName))
            }
            val username = when (role) {
                "ADMIN" -> "admin"
                "TECHNICIAN" -> "tech_${System.currentTimeMillis() % 1000}"
                else -> "cust_${System.currentTimeMillis() % 1000}"
            }
            val newUser = UserEntity(
                username = username,
                password = if (role == "ADMIN") "admin123" else "pass123",
                role = role,
                fullName = fullName,
                phone = phone,
                email = email
            )
            repository.insertUser(newUser)
            _currentUser.value = newUser
            prefs.edit().putBoolean("onboarding_completed", true).apply()
            _isOnboardingCompleted.value = true
        }
    }

    fun resetOnboarding() {
        prefs.edit().putBoolean("onboarding_completed", false).apply()
        _isOnboardingCompleted.value = false
        _toastMessage.value = "Onboarding flow reset."
    }

    val allTickets: StateFlow<List<TicketEntity>> = repository.allTickets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsers: StateFlow<List<UserEntity>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val technicians: StateFlow<List<UserEntity>> = repository.technicians
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val companyProfile: StateFlow<CompanyProfileEntity?> = repository.companyProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Current User Session
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    // Selected Ticket for Detail / Invoice View
    private val _selectedTicket = MutableStateFlow<TicketEntity?>(null)
    val selectedTicket: StateFlow<TicketEntity?> = _selectedTicket.asStateFlow()

    // Toast message for UI feedback
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // Registered Customer Account State
    private val _registeredCustomerPhone = MutableStateFlow(prefs.getString("registered_customer_phone", "") ?: "")
    val registeredCustomerPhone: StateFlow<String> = _registeredCustomerPhone.asStateFlow()

    private val _registeredCustomerName = MutableStateFlow(prefs.getString("registered_customer_name", "") ?: "")
    val registeredCustomerName: StateFlow<String> = _registeredCustomerName.asStateFlow()

    private val _registeredCustomerEmail = MutableStateFlow(prefs.getString("registered_customer_email", "") ?: "")
    val registeredCustomerEmail: StateFlow<String> = _registeredCustomerEmail.asStateFlow()

    fun registerCustomerAccount(name: String, phone: String, email: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val cleanPhone = phone.trim()
            val cleanName = name.trim()
            val cleanEmail = email.trim()

            repository.registerCustomer(cleanName, cleanPhone, cleanEmail)

            prefs.edit()
                .putString("registered_customer_phone", cleanPhone)
                .putString("registered_customer_name", cleanName)
                .putString("registered_customer_email", cleanEmail)
                .apply()

            _registeredCustomerPhone.value = cleanPhone
            _registeredCustomerName.value = cleanName
            _registeredCustomerEmail.value = cleanEmail
            _customerSearchQuery.value = cleanPhone

            _toastMessage.value = "Registered successfully! Displaying your service jobs."
            onComplete()
        }
    }

    fun clearRegisteredCustomerAccount() {
        prefs.edit()
            .remove("registered_customer_phone")
            .remove("registered_customer_name")
            .remove("registered_customer_email")
            .apply()

        _registeredCustomerPhone.value = ""
        _registeredCustomerName.value = ""
        _registeredCustomerEmail.value = ""
        _customerSearchQuery.value = ""
        _toastMessage.value = "Signed out of customer account."
    }

    // Customer search query state
    private val _customerSearchQuery = MutableStateFlow("")
    val customerSearchQuery: StateFlow<String> = _customerSearchQuery.asStateFlow()

    fun setCustomerSearchQuery(query: String) {
        _customerSearchQuery.value = query
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    // Auto-Save Staff Credentials State
    val isAutoSaveCredentialsEnabled: Boolean
        get() = prefs.getBoolean("auto_save_credentials_enabled", false)

    val savedStaffUsername: String
        get() = prefs.getString("saved_staff_username", "") ?: ""

    val savedStaffPassword: String
        get() = prefs.getString("saved_staff_password", "") ?: ""

    fun saveStaffCredentials(user: String, pass: String, autoSave: Boolean) {
        if (autoSave) {
            prefs.edit()
                .putBoolean("auto_save_credentials_enabled", true)
                .putString("saved_staff_username", user.trim())
                .putString("saved_staff_password", pass)
                .apply()
        } else {
            prefs.edit()
                .putBoolean("auto_save_credentials_enabled", false)
                .remove("saved_staff_username")
                .remove("saved_staff_password")
                .apply()
        }
    }

    fun login(username: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val user = repository.login(username, pass)
            if (user != null) {
                _currentUser.value = user
                onResult(true, user.role)
            } else {
                onResult(false, "Invalid username or password")
            }
        }
    }

    fun logout() {
        _currentUser.value = null
    }

    fun selectTicket(ticket: TicketEntity?) {
        _selectedTicket.value = ticket
    }

    fun createTicket(
        customerName: String,
        customerPhone: String,
        customerEmail: String,
        serviceType: String,
        deviceName: String,
        issueDescription: String,
        locationAddress: String,
        latitude: Double,
        longitude: Double,
        onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            val prefix = companyProfile.value?.ticketSeriesPrefix ?: "NEO-2026-"
            val count = allTickets.value.size + 1
            val ticketNumber = "$prefix${String.format("%03d", count)}"
            val currentUserId = _currentUser.value?.id ?: 4L
            val initialTimeline = listOf(
                com.example.data.TicketTimelineEntry(
                    status = "PENDING",
                    updatedByName = _currentUser.value?.fullName ?: customerName,
                    updatedByRole = _currentUser.value?.role ?: "CUSTOMER",
                    remarks = "Service complaint registered for $deviceName.",
                    timestamp = System.currentTimeMillis()
                )
            )
            val newTicket = TicketEntity(
                ticketNumber = ticketNumber,
                customerId = currentUserId,
                customerName = customerName,
                customerPhone = customerPhone,
                customerEmail = customerEmail,
                serviceType = serviceType,
                deviceName = deviceName,
                issueDescription = issueDescription,
                locationAddress = locationAddress,
                latitude = latitude,
                longitude = longitude,
                status = "PENDING",
                statusTimelineJson = com.example.data.TicketTimelineEntry.toJsonString(initialTimeline),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            repository.insertTicket(newTicket)
            _toastMessage.value = "Ticket $ticketNumber created successfully! (Read-only for customer)"
            onSuccess(ticketNumber)
        }
    }

    private fun appendTimeline(
        ticket: TicketEntity,
        newStatus: String,
        remarks: String = "",
        updaterNameOverride: String? = null,
        updaterRoleOverride: String? = null
    ): String {
        val existing = com.example.data.TicketTimelineEntry.parseList(ticket.statusTimelineJson).toMutableList()
        val updaterName = updaterNameOverride ?: _currentUser.value?.fullName ?: (if (newStatus == "APPROVED" || newStatus == "REJECTED") ticket.customerName else "Staff")
        val updaterRole = updaterRoleOverride ?: _currentUser.value?.role ?: (if (newStatus == "APPROVED" || newStatus == "REJECTED") "CUSTOMER" else "STAFF")
        existing.add(
            com.example.data.TicketTimelineEntry(
                status = newStatus,
                updatedByName = updaterName,
                updatedByRole = updaterRole,
                remarks = remarks,
                timestamp = System.currentTimeMillis()
            )
        )
        return com.example.data.TicketTimelineEntry.toJsonString(existing)
    }

    fun updateTicketStatus(ticketId: Long, newStatus: String, notes: String? = null) {
        viewModelScope.launch {
            val ticket = repository.getTicketById(ticketId)
            if (ticket != null) {
                val updatedTimeline = appendTimeline(ticket, newStatus, notes ?: "Status changed to ${newStatus.replace("_", " ")}")
                val updated = ticket.copy(
                    status = newStatus,
                    diagnosticNotes = if (notes != null) "${ticket.diagnosticNotes}\n[$newStatus]: $notes" else ticket.diagnosticNotes,
                    statusTimelineJson = updatedTimeline,
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateTicket(updated)
                _selectedTicket.value = updated
                _toastMessage.value = "Status updated to $newStatus"
            }
        }
    }

    fun updateTicketPaymentStatus(ticketId: Long, paymentStatus: String, paymentMode: String = "CASH", notes: String = "") {
        viewModelScope.launch {
            val ticket = repository.getTicketById(ticketId)
            if (ticket != null) {
                val modeDisplay = if (paymentStatus == "RECEIVED") " via $paymentMode" else ""
                val remark = "Payment status updated to ${if (paymentStatus == "RECEIVED") "RECEIVED" else "PENDING"}$modeDisplay. $notes".trim()
                val updatedTimeline = appendTimeline(ticket, ticket.status, remark)
                val updated = ticket.copy(
                    paymentStatus = paymentStatus,
                    paymentMode = paymentMode,
                    statusTimelineJson = updatedTimeline,
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateTicket(updated)
                _selectedTicket.value = updated
                _toastMessage.value = if (paymentStatus == "RECEIVED") "Payment marked as RECEIVED ($paymentMode)" else "Payment marked as PENDING"
            }
        }
    }

    fun updateRepairStatusWithColumns(
        ticketId: Long,
        newStatus: String,
        findings: String,
        partsNeeded: String,
        estimatedCompletion: String,
        remarks: String
    ) {
        viewModelScope.launch {
            val ticket = repository.getTicketById(ticketId)
            if (ticket != null) {
                val columnDetails = buildString {
                    append("[$newStatus]")
                    if (findings.isNotBlank()) append(" • Findings: $findings")
                    if (partsNeeded.isNotBlank()) append(" • Parts: $partsNeeded")
                    if (estimatedCompletion.isNotBlank()) append(" • Est: $estimatedCompletion")
                    if (remarks.isNotBlank()) append(" • Note: $remarks")
                }
                val updatedTimeline = appendTimeline(ticket, newStatus, columnDetails)
                val updated = ticket.copy(
                    status = newStatus,
                    diagnosticNotes = if (ticket.diagnosticNotes.isBlank()) columnDetails else "${ticket.diagnosticNotes}\n$columnDetails",
                    statusTimelineJson = updatedTimeline,
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateTicket(updated)
                _selectedTicket.value = updated
                _toastMessage.value = "Repair status & pipeline columns updated to $newStatus"
            }
        }
    }

    fun assignTechnician(ticketId: Long, techId: Long, techName: String) {
        viewModelScope.launch {
            val ticket = repository.getTicketById(ticketId)
            if (ticket != null) {
                val updatedTimeline = appendTimeline(ticket, "TECHNICIAN_ASSIGNED", "Technician assigned: $techName")
                val updated = ticket.copy(
                    assignedTechnicianId = techId,
                    assignedTechnicianName = techName,
                    status = "TECHNICIAN_ASSIGNED",
                    statusTimelineJson = updatedTimeline,
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateTicket(updated)
                _selectedTicket.value = updated
                _toastMessage.value = "Technician $techName assigned!"
            }
        }
    }

    fun deleteTicket(ticketId: Long, onSuccess: () -> Unit = {}) {
        val user = _currentUser.value
        if (user?.role != "ADMIN") {
            _toastMessage.value = "Permission Denied: Only Administrator can remove service jobs"
            return
        }
        viewModelScope.launch {
            repository.deleteTicket(ticketId)
            if (_selectedTicket.value?.id == ticketId) {
                _selectedTicket.value = null
            }
            _toastMessage.value = "Service job permanently removed"
            onSuccess()
        }
    }

    fun cancelTicket(ticketId: Long, reason: String = "Cancelled by user/admin", onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val ticket = repository.getTicketById(ticketId)
            if (ticket != null) {
                val cancelNote = if (reason.isNotBlank()) "Job cancelled. Reason: $reason" else "Job cancelled."
                val updatedTimeline = appendTimeline(ticket, "CANCELLED", cancelNote)
                val updated = ticket.copy(
                    status = "CANCELLED",
                    diagnosticNotes = if (ticket.diagnosticNotes.isBlank()) cancelNote else "${ticket.diagnosticNotes}\n$cancelNote",
                    statusTimelineJson = updatedTimeline,
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateTicket(updated)
                _selectedTicket.value = updated
                _toastMessage.value = "Job #${ticket.ticketNumber} marked as Cancelled"
                onSuccess()
            }
        }
    }

    fun reopenTicket(ticketId: Long, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val ticket = repository.getTicketById(ticketId)
            if (ticket != null) {
                val updatedTimeline = appendTimeline(ticket, "PENDING", "Job ticket reopened and restored to active pipeline")
                val updated = ticket.copy(
                    status = "PENDING",
                    statusTimelineJson = updatedTimeline,
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateTicket(updated)
                _selectedTicket.value = updated
                _toastMessage.value = "Job #${ticket.ticketNumber} reopened"
                onSuccess()
            }
        }
    }

    fun updateQuotationAndPricing(
        ticketId: Long,
        partsDesc: String,
        partsCost: Double,
        laborDesc: String,
        laborCost: Double,
        gstRate: Double
    ) {
        viewModelScope.launch {
            val ticket = repository.getTicketById(ticketId)
            if (ticket != null) {
                val updatedTimeline = appendTimeline(ticket, "QUOTATION_SENT", "Quotation of parts ($partsCost) & labor ($laborCost) sent")
                val updated = ticket.copy(
                    partsDescription = partsDesc,
                    partsCost = partsCost,
                    laborDescription = laborDesc,
                    laborCost = laborCost,
                    gstRate = gstRate,
                    status = "QUOTATION_SENT",
                    statusTimelineJson = updatedTimeline,
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateTicket(updated)
                _selectedTicket.value = updated
                _toastMessage.value = "Quotation items & pricing updated & sent!"
            }
        }
    }

    fun updateQuotationLineItems(
        ticketId: Long,
        items: List<com.example.data.QuotationItem>,
        gstRate: Double
    ) {
        viewModelScope.launch {
            val ticket = repository.getTicketById(ticketId)
            if (ticket != null) {
                val parts = items.filter { it.category == "Part" }.sumOf { it.total }
                val labor = items.filter { it.category != "Part" }.sumOf { it.total }
                val partsSummary = items.filter { it.category == "Part" }.joinToString(", ") { "${it.name} (x${it.quantity})" }.ifEmpty { "Spare parts" }
                val laborSummary = items.filter { it.category != "Part" }.joinToString(", ") { "${it.name} (x${it.quantity})" }.ifEmpty { "Service & repair" }

                val totalDiscount = items.sumOf { it.effectiveDiscount }
                val discRemark = if (totalDiscount > 0) " (discount: %.2f applied)".format(totalDiscount) else ""
                val updatedTimeline = appendTimeline(ticket, "QUOTATION_SENT", "Quotation prepared with ${items.size} line item(s)$discRemark")
                val updated = ticket.copy(
                    partsDescription = partsSummary,
                    partsCost = parts,
                    laborDescription = laborSummary,
                    laborCost = labor,
                    gstRate = gstRate,
                    quotationItemsJson = com.example.data.QuotationItem.toJsonString(items),
                    status = "QUOTATION_SENT",
                    statusTimelineJson = updatedTimeline,
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateTicket(updated)
                _selectedTicket.value = updated
                _toastMessage.value = "Quotation with ${items.size} item(s) sent to customer!"
            }
        }
    }

    fun updateTicketItemsAndDiscounts(
        ticketId: Long,
        items: List<com.example.data.QuotationItem>,
        gstRate: Double? = null,
        statusOverride: String? = null
    ) {
        viewModelScope.launch {
            val ticket = repository.getTicketById(ticketId)
            if (ticket != null) {
                val parts = items.filter { it.category == "Part" }.sumOf { it.total }
                val labor = items.filter { it.category != "Part" }.sumOf { it.total }
                val partsSummary = items.filter { it.category == "Part" }.joinToString(", ") { "${it.name} (x${it.quantity})" }.ifEmpty { "Spare parts" }
                val laborSummary = items.filter { it.category != "Part" }.joinToString(", ") { "${it.name} (x${it.quantity})" }.ifEmpty { "Service & repair" }

                val targetStatus = statusOverride ?: ticket.status
                val totalDiscount = items.sumOf { it.effectiveDiscount }
                val discRemark = if (totalDiscount > 0) " (discount: %.2f applied)".format(totalDiscount) else ""
                val updatedTimeline = appendTimeline(ticket, targetStatus, "Quotation/Invoice items & discounts manually adjusted$discRemark")

                val updated = ticket.copy(
                    partsDescription = partsSummary,
                    partsCost = parts,
                    laborDescription = laborSummary,
                    laborCost = labor,
                    gstRate = gstRate ?: ticket.gstRate,
                    quotationItemsJson = com.example.data.QuotationItem.toJsonString(items),
                    status = targetStatus,
                    statusTimelineJson = updatedTimeline,
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateTicket(updated)
                _selectedTicket.value = updated
                _toastMessage.value = "Discounts and itemized costs updated successfully!"
            }
        }
    }

    fun approveOrRejectQuotation(ticketId: Long, isApproved: Boolean, note: String) {
        viewModelScope.launch {
            val ticket = repository.getTicketById(ticketId)
            if (ticket != null) {
                val newStatus = if (isApproved) "APPROVED" else "REJECTED"
                val remark = if (note.isNotBlank()) note else (if (isApproved) "Quotation approved by customer" else "Quotation rejected by customer")
                val updatedTimeline = appendTimeline(ticket, newStatus, remark)
                val updated = ticket.copy(
                    status = newStatus,
                    customerApprovalNote = note,
                    statusTimelineJson = updatedTimeline,
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateTicket(updated)
                _selectedTicket.value = updated
                _toastMessage.value = if (isApproved) "Quotation Approved by Customer!" else "Quotation Rejected by Customer."
            }
        }
    }

    fun saveCompanyProfile(profile: CompanyProfileEntity) {
        viewModelScope.launch {
            repository.saveCompanyProfile(profile)
            _toastMessage.value = "Company Profile updated successfully!"
        }
    }

    fun toggleThemeMode() {
        val current = companyProfile.value ?: return
        val newMode = if (current.themeMode == "DARK") "LIGHT" else "DARK"
        viewModelScope.launch {
            val updated = current.copy(themeMode = newMode)
            repository.saveCompanyProfile(updated)
            _toastMessage.value = if (newMode == "DARK") "Dark Mode enabled" else "Light Mode enabled"
        }
    }

    fun setTaxEnabled(enabled: Boolean) {
        val current = companyProfile.value ?: return
        viewModelScope.launch {
            val updated = current.copy(taxEnabled = enabled)
            repository.saveCompanyProfile(updated)
        }
    }

    fun generateWhatsAppMessage(ticket: TicketEntity, companyName: String): String {
        return generateWhatsAppStatusMessage(ticket, companyName)
    }

    fun addUser(
        username: String,
        password: String,
        role: String,
        fullName: String,
        phone: String,
        email: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            if (username.isBlank() || password.isBlank() || fullName.isBlank()) {
                onResult(false, "Username, password and name are required")
                return@launch
            }
            val existing = repository.getUserByUsername(username.trim())
            if (existing != null) {
                onResult(false, "Username '${username.trim()}' already exists")
                return@launch
            }
            val newUser = UserEntity(
                username = username.trim(),
                password = password.trim(),
                role = role,
                fullName = fullName.trim(),
                phone = phone.trim(),
                email = email.trim()
            )
            repository.insertUser(newUser)
            _toastMessage.value = "User '$fullName' added successfully"
            onResult(true, "User created successfully")
        }
    }

    fun updateUser(
        user: UserEntity,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            if (user.username.isBlank() || user.fullName.isBlank()) {
                onResult(false, "Username and full name cannot be blank")
                return@launch
            }
            val existing = repository.getUserByUsername(user.username.trim())
            if (existing != null && existing.id != user.id) {
                onResult(false, "Username '${user.username}' is taken by another user")
                return@launch
            }
            repository.updateUser(user)
            _toastMessage.value = "User '${user.fullName}' updated"
            onResult(true, "User updated successfully")
        }
    }

    fun deleteUser(
        userId: Long,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val loggedInId = _currentUser.value?.id
            if (loggedInId == userId) {
                onResult(false, "You cannot delete your own active administrator account")
                return@launch
            }
            repository.deleteUser(userId)
            _toastMessage.value = "User removed"
            onResult(true, "User removed successfully")
        }
    }

    fun generateWhatsAppStatusMessage(ticket: TicketEntity, companyName: String): String {
        val notes = if (ticket.diagnosticNotes.isNotBlank()) "\n📝 *Remarks:* ${ticket.diagnosticNotes}" else ""
        val tech = if (ticket.assignedTechnicianName.isNotBlank()) "\n👨‍🔧 *Technician:* ${ticket.assignedTechnicianName}" else ""
        return """
Hello ${ticket.customerName},

*Service Update from $companyName*
----------------------------------------
🔖 *Ticket:* #${ticket.ticketNumber}
📱 *Device:* ${ticket.deviceName}
⚙️ *Status:* ${ticket.status.replace("_", " ")}$tech$notes

Thank you for choosing *$companyName*!
        """.trimIndent()
    }

    fun generateWhatsAppInvoiceMessage(ticket: TicketEntity, companyName: String, currencySymbol: String = "$"): String {
        val items = com.example.data.QuotationItem.parseList(ticket.quotationItemsJson)
        val grossSubtotal = if (items.isNotEmpty()) items.sumOf { it.grossTotal } else (ticket.partsCost + ticket.laborCost)
        val discount = if (items.isNotEmpty()) items.sumOf { it.effectiveDiscount } else 0.0
        val subtotal = if (items.isNotEmpty()) items.sumOf { it.total } else (ticket.partsCost + ticket.laborCost)
        val gst = if (ticket.gstRate > 0) subtotal * (ticket.gstRate / 100.0) else 0.0
        val total = subtotal + gst
        
        val discStr = String.format(java.util.Locale.getDefault(), "%.2f", discount)
        val gstValStr = String.format(java.util.Locale.getDefault(), "%.2f", gst)
        val subtotalStr = String.format(java.util.Locale.getDefault(), "%.2f", subtotal)
        val totalStr = String.format(java.util.Locale.getDefault(), "%.2f", total)
        
        val discountText = if (discount > 0) "\n🏷️ *Discount Savings:* -$currencySymbol$discStr" else ""
        val gstText = if (gst > 0) "\n🏛️ *GST (${ticket.gstRate}%):* $currencySymbol$gstValStr" else ""

        val itemsSummary = if (items.isNotEmpty()) {
            "\n\n*Itemized Breakdown:*\n" + items.joinToString("\n") {
                val itemTotalStr = String.format(java.util.Locale.getDefault(), "%.2f", it.total)
                "• ${it.name} (x${it.quantity}): $currencySymbol$itemTotalStr"
            }
        } else ""

        return """
Hello ${ticket.customerName},

*Invoice & Estimate Details from $companyName*
----------------------------------------
🔖 *Ticket:* #${ticket.ticketNumber}
📱 *Device:* ${ticket.deviceName}
⚙️ *Status:* ${ticket.status.replace("_", " ")}$itemsSummary
----------------------------------------
💰 *Subtotal:* $currencySymbol$subtotalStr$discountText$gstText
💵 *Total Amount:* $currencySymbol$totalStr
----------------------------------------
Thank you for choosing *$companyName*!
        """.trimIndent()
    }

    // ==================== DATABASE MANAGEMENT FOR ADMIN ====================

    fun updateLiveDataToToday(onComplete: (String) -> Unit = {}) {
        viewModelScope.launch {
            val tickets = allTickets.value
            val now = System.currentTimeMillis()
            tickets.forEach { ticket ->
                val updated = ticket.copy(updatedAt = now)
                repository.updateTicket(updated)
            }
            _toastMessage.value = "Updated live operations timestamps to today"
            onComplete("All active ticket timestamps updated to today")
        }
    }

    fun seedSampleData(onComplete: (String) -> Unit = {}) {
        viewModelScope.launch {
            repository.seedSampleDatabase()
            _toastMessage.value = "Sample database records populated"
            onComplete("Sample database records populated successfully")
        }
    }

    fun addBulkTestTickets(count: Int = 5, onComplete: (String) -> Unit = {}) {
        viewModelScope.launch {
            val baseNumber = allTickets.value.size + 1
            val devices = listOf("MacBook Pro M2 14\"", "Samsung Galaxy S24 Ultra", "iPad Air 5th Gen", "Sony WH-1000XM5", "Canon EOS R6 Camera", "Lenovo ThinkPad X1")
            val customers = listOf("Kavita Rao", "Sameer Verma", "Pooja Hegde", "Aditya Nair", "Deepak Joshi", "Sneha Kulkarni")
            val phones = listOf("+91 98765 44441", "+91 98765 44442", "+91 98765 44443", "+91 98765 44444", "+91 98765 44445", "+91 98765 44446")
            val issues = listOf("Screen replacement required", "Battery draining rapidly", "Water damage diagnostic", "Speaker sound distorted", "Keyboard keys not registering", "Charging port replacement")
            val statuses = listOf("PENDING", "DIAGNOSTICS", "QUOTATION_SENT", "IN_PROGRESS", "COMPLETED")
            val techs = technicians.value

            val newTickets = (0 until count).map { i ->
                val tech = if (techs.isNotEmpty()) techs[i % techs.size] else null
                val status = statuses[i % statuses.size]
                val ticketNum = "NEO-2026-${String.format("%03d", baseNumber + i + 100)}"
                val now = System.currentTimeMillis() - (i * 1000L * 60 * 60 * 4)
                val partsCost = (i + 1) * 850.0
                val laborCost = (i + 1) * 400.0

                val sampleItems = listOf(
                    com.example.data.QuotationItem(name = "Replacement Module / Components", category = "Part", quantity = 1, unitPrice = partsCost),
                    com.example.data.QuotationItem(name = "Service & Technical Diagnostic Fee", category = "Labor", quantity = 1, unitPrice = laborCost)
                )

                TicketEntity(
                    ticketNumber = ticketNum,
                    customerId = 4,
                    customerName = customers[i % customers.size],
                    customerPhone = phones[i % phones.size],
                    customerEmail = "${customers[i % customers.size].lowercase().replace(" ", "")}@gmail.com",
                    serviceType = if (i % 2 == 0) "Doorstep" else "In-Store",
                    deviceName = devices[i % devices.size],
                    issueDescription = issues[i % issues.size],
                    locationAddress = "Tech Hub, Sector ${i + 1}, Bengaluru",
                    latitude = 12.9716 + (i * 0.005),
                    longitude = 77.5946 + (i * 0.005),
                    status = status,
                    assignedTechnicianId = tech?.id ?: 0L,
                    assignedTechnicianName = tech?.fullName ?: "",
                    diagnosticNotes = "Automatic test diagnostic entry for $ticketNum",
                    partsDescription = "Standard replacement components",
                    partsCost = partsCost,
                    laborDescription = "Technical inspection & assembly",
                    laborCost = laborCost,
                    gstRate = 18.0,
                    quotationItemsJson = com.example.data.QuotationItem.toJsonString(sampleItems),
                    statusTimelineJson = com.example.data.TicketTimelineEntry.toJsonString(
                        listOf(
                            com.example.data.TicketTimelineEntry(
                                status = "PENDING",
                                updatedByName = customers[i % customers.size],
                                updatedByRole = "CUSTOMER",
                                remarks = "Test job booked in database.",
                                timestamp = now
                            )
                        )
                    ),
                    createdAt = now,
                    updatedAt = now
                )
            }
            repository.insertTickets(newTickets)
            _toastMessage.value = "Added $count test tickets to database"
            onComplete("Added $count test tickets successfully")
        }
    }

    fun clearAllTickets(onComplete: (String) -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteAllTickets()
            _selectedTicket.value = null
            _toastMessage.value = "All tickets cleared from database"
            onComplete("Database ticket records purged")
        }
    }

    fun clearCompletedAndCancelledTickets(onComplete: (String) -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteCompletedAndCancelledTickets()
            _toastMessage.value = "Purged completed & cancelled tickets"
            onComplete("Purged completed and cancelled records")
        }
    }

    fun clearNonAdminUsers(onComplete: (String) -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteNonAdminUsers()
            _toastMessage.value = "Non-admin users cleared"
            onComplete("Non-admin users removed")
        }
    }

    fun factoryResetDatabase(onComplete: (String) -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteAllTickets()
            repository.deleteNonAdminUsers()
            repository.seedSampleDatabase()
            _selectedTicket.value = null
            _toastMessage.value = "Database factory reset complete"
            onComplete("Database restored to clean default state")
        }
    }

    // ==================== DATABASE BACKUP & RESTORE ====================

    fun generateDatabaseBackupJson(): String {
        val currentProfile = companyProfile.value
        val tickets = allTickets.value
        val users = allUsers.value

        val rootObj = JSONObject()
        rootObj.put("version", "1.0")
        rootObj.put("timestamp", System.currentTimeMillis())

        if (currentProfile != null) {
            val profObj = JSONObject().apply {
                put("companyName", currentProfile.companyName)
                put("tagline", currentProfile.tagline)
                put("gstin", currentProfile.gstin)
                put("phone", currentProfile.phone)
                put("email", currentProfile.email)
                put("address", currentProfile.address)
                put("currencySymbol", currentProfile.currencySymbol)
                put("taxEnabled", currentProfile.taxEnabled)
                put("serviceCenterLat", currentProfile.serviceCenterLat)
                put("serviceCenterLng", currentProfile.serviceCenterLng)
                put("colorTheme", currentProfile.colorTheme)
            }
            rootObj.put("companyProfile", profObj)
        }

        val usersArray = JSONArray()
        for (u in users) {
            val uObj = JSONObject().apply {
                put("username", u.username)
                put("password", u.password)
                put("role", u.role)
                put("fullName", u.fullName)
                put("phone", u.phone)
                put("email", u.email)
            }
            usersArray.put(uObj)
        }
        rootObj.put("users", usersArray)

        val ticketsArray = JSONArray()
        for (t in tickets) {
            val tObj = JSONObject().apply {
                put("ticketNumber", t.ticketNumber)
                put("customerName", t.customerName)
                put("customerPhone", t.customerPhone)
                put("customerEmail", t.customerEmail)
                put("serviceType", t.serviceType)
                put("deviceName", t.deviceName)
                put("issueDescription", t.issueDescription)
                put("status", t.status)
                put("partsCost", t.partsCost)
                put("partsDescription", t.partsDescription)
                put("laborCost", t.laborCost)
                put("laborDescription", t.laborDescription)
                put("quotationItemsJson", t.quotationItemsJson)
                put("assignedTechnicianId", t.assignedTechnicianId)
                put("assignedTechnicianName", t.assignedTechnicianName)
                put("gstRate", t.gstRate)
                put("locationAddress", t.locationAddress)
                put("latitude", t.latitude)
                put("longitude", t.longitude)
                put("diagnosticNotes", t.diagnosticNotes)
                put("statusTimelineJson", t.statusTimelineJson)
                put("paymentStatus", t.paymentStatus)
                put("paymentMode", t.paymentMode)
                put("createdAt", t.createdAt)
                put("updatedAt", t.updatedAt)
            }
            ticketsArray.put(tObj)
        }
        rootObj.put("tickets", ticketsArray)

        return rootObj.toString(2)
    }

    fun restoreDatabaseFromJson(jsonString: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val root = JSONObject(jsonString)

                // 1. Restore Company Profile if present
                if (root.has("companyProfile")) {
                    val pObj = root.getJSONObject("companyProfile")
                    val profile = CompanyProfileEntity(
                        id = 1,
                        companyName = pObj.optString("companyName", "Neo"),
                        tagline = pObj.optString("tagline", ""),
                        gstin = pObj.optString("gstin", ""),
                        phone = pObj.optString("phone", ""),
                        email = pObj.optString("email", ""),
                        address = pObj.optString("address", ""),
                        currencySymbol = pObj.optString("currencySymbol", "$"),
                        taxEnabled = pObj.optBoolean("taxEnabled", true),
                        serviceCenterLat = pObj.optDouble("serviceCenterLat", 12.9716),
                        serviceCenterLng = pObj.optDouble("serviceCenterLng", 77.5946),
                        colorTheme = pObj.optString("colorTheme", "NEO_BLUE")
                    )
                    repository.saveCompanyProfile(profile)
                }

                // 2. Restore Users if present
                var importedUsers = 0
                if (root.has("users")) {
                    val uArr = root.getJSONArray("users")
                    val userList = mutableListOf<UserEntity>()
                    for (i in 0 until uArr.length()) {
                        val uObj = uArr.getJSONObject(i)
                        userList.add(
                            UserEntity(
                                username = uObj.optString("username", "user_$i"),
                                password = uObj.optString("password", "pass123"),
                                role = uObj.optString("role", "TECHNICIAN"),
                                fullName = uObj.optString("fullName", "User $i"),
                                phone = uObj.optString("phone", ""),
                                email = uObj.optString("email", "")
                            )
                        )
                    }
                    if (userList.isNotEmpty()) {
                        repository.insertUsers(userList)
                        importedUsers = userList.size
                    }
                }

                // 3. Restore Tickets if present
                var importedTickets = 0
                if (root.has("tickets")) {
                    val tArr = root.getJSONArray("tickets")
                    val ticketList = mutableListOf<TicketEntity>()
                    for (i in 0 until tArr.length()) {
                        val tObj = tArr.getJSONObject(i)
                        ticketList.add(
                            TicketEntity(
                                ticketNumber = tObj.optString("ticketNumber", "NEO-RESTORE-$i"),
                                customerId = tObj.optLong("customerId", 1L),
                                customerName = tObj.optString("customerName", "Customer"),
                                customerPhone = tObj.optString("customerPhone", ""),
                                customerEmail = tObj.optString("customerEmail", ""),
                                serviceType = tObj.optString("serviceType", "Workshop"),
                                deviceName = tObj.optString("deviceName", "Device"),
                                issueDescription = tObj.optString("issueDescription", ""),
                                status = tObj.optString("status", "PENDING"),
                                partsCost = tObj.optDouble("partsCost", 0.0),
                                partsDescription = tObj.optString("partsDescription", ""),
                                laborCost = tObj.optDouble("laborCost", 0.0),
                                laborDescription = tObj.optString("laborDescription", ""),
                                quotationItemsJson = tObj.optString("quotationItemsJson", "[]"),
                                assignedTechnicianId = tObj.optLong("assignedTechnicianId", 0L),
                                assignedTechnicianName = tObj.optString("assignedTechnicianName", ""),
                                gstRate = tObj.optDouble("gstRate", 18.0),
                                locationAddress = tObj.optString("locationAddress", ""),
                                latitude = tObj.optDouble("latitude", tObj.optDouble("selectedLat", 0.0)),
                                longitude = tObj.optDouble("longitude", tObj.optDouble("selectedLng", 0.0)),
                                diagnosticNotes = tObj.optString("diagnosticNotes", ""),
                                statusTimelineJson = tObj.optString("statusTimelineJson", "[]"),
                                paymentStatus = tObj.optString("paymentStatus", "PENDING"),
                                paymentMode = tObj.optString("paymentMode", "CASH"),
                                createdAt = tObj.optLong("createdAt", System.currentTimeMillis()),
                                updatedAt = tObj.optLong("updatedAt", System.currentTimeMillis())
                            )
                        )
                    }
                    if (ticketList.isNotEmpty()) {
                        repository.insertTickets(ticketList)
                        importedTickets = ticketList.size
                    }
                }

                _toastMessage.value = "Restore Successful! Restored $importedTickets tickets & $importedUsers users."
                onComplete(true, "Successfully restored $importedTickets tickets and $importedUsers users.")
            } catch (e: Exception) {
                e.printStackTrace()
                _toastMessage.value = "Restore Failed: ${e.localizedMessage}"
                onComplete(false, "Invalid Backup Format: ${e.localizedMessage}")
            }
        }
    }
}
