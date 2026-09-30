package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.QuotationItem
import com.example.data.TicketEntity
import com.example.ui.components.AdminStatusTimelineComponent
import com.example.viewmodel.ServiceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketDetailScreen(
    viewModel: ServiceViewModel,
    onNavigateQuotationPreview: () -> Unit,
    onNavigateInvoicePreview: () -> Unit,
    onBack: () -> Unit
) {
    val ticket by viewModel.selectedTicket.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val companyProfile by viewModel.companyProfile.collectAsState()
    val technicians by viewModel.technicians.collectAsState()
    val context = LocalContext.current

    var showStatusDialog by remember { mutableStateOf(false) }
    var showTechDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var showQuotationItemsDialog by remember { mutableStateOf(false) }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var showApprovalDialog by remember { mutableStateOf(false) }
    var approvalActionIsApprove by remember { mutableStateOf(true) }
    var cancelReasonInput by remember { mutableStateOf("") }

    if (ticket == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Ticket Details") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null)
                        }
                    }
                )
            }
        ) { p ->
            Box(modifier = Modifier.fillMaxSize().padding(p), contentAlignment = Alignment.Center) {
                Text("No ticket selected.")
            }
        }
        return
    }

    var showPaymentModeDialog by remember { mutableStateOf(false) }
    var selectedPaymentMode by remember { mutableStateOf("CASH") }

    val currentTicket = ticket!!
    val isCustomer = currentUser?.role == "CUSTOMER" || currentUser == null
    val curr = companyProfile?.currencySymbol ?: "₹"
    val isTaxEnabled = companyProfile?.taxEnabled ?: true
    val isGstActive = isTaxEnabled && currentTicket.gstRate > 0.0

    // Parse itemized quotation line items
    val quotationItems = remember(currentTicket.quotationItemsJson, currentTicket.partsCost, currentTicket.laborCost) {
        val parsed = QuotationItem.parseList(currentTicket.quotationItemsJson)
        if (parsed.isNotEmpty()) {
            parsed
        } else {
            val list = mutableListOf<QuotationItem>()
            if (currentTicket.partsCost > 0) {
                list.add(QuotationItem(name = currentTicket.partsDescription.ifBlank { "Spare parts" }, category = "Part", quantity = 1, unitPrice = currentTicket.partsCost))
            }
            if (currentTicket.laborCost > 0) {
                list.add(QuotationItem(name = currentTicket.laborDescription.ifBlank { "Service & repair labor" }, category = "Labor", quantity = 1, unitPrice = currentTicket.laborCost))
            }
            list
        }
    }

    val grossPartsTotal = quotationItems.filter { it.category == "Part" }.sumOf { it.grossTotal }.let { if (it > 0) it else currentTicket.partsCost }
    val grossLaborTotal = quotationItems.filter { it.category != "Part" }.sumOf { it.grossTotal }.let { if (it > 0) it else currentTicket.laborCost }
    val totalDiscount = quotationItems.sumOf { it.effectiveDiscount }
    val partsTotal = quotationItems.filter { it.category == "Part" }.sumOf { it.total }.let { if (it > 0) it else currentTicket.partsCost }
    val laborTotal = quotationItems.filter { it.category != "Part" }.sumOf { it.total }.let { if (it > 0) it else currentTicket.laborCost }
    val subtotal = partsTotal + laborTotal
    val gstAmount = if (isGstActive) subtotal * (currentTicket.gstRate / 100.0) else 0.0
    val totalEstimate = subtotal + gstAmount

    // Minimal pipeline stages
    val minimalStages = listOf(
        Triple("PENDING", "Booked", Icons.Default.Inbox),
        Triple("DIAGNOSTICS", "Diagnostics", Icons.Default.Search),
        Triple("QUOTATION_SENT", "Quote Sent", Icons.Default.RequestQuote),
        Triple("IN_PROGRESS", "In Repair", Icons.Default.Handyman),
        Triple("COMPLETED", "Done", Icons.Default.DoneAll),
        Triple("CANCELLED", "Cancelled", Icons.Default.Cancel)
    )

    val currentStageIdx = when (currentTicket.status) {
        "PENDING", "TECHNICIAN_ASSIGNED" -> 0
        "DIAGNOSTICS" -> 1
        "QUOTATION_SENT", "APPROVED", "REJECTED" -> 2
        "IN_PROGRESS" -> 3
        "READY_FOR_DELIVERY", "COMPLETED" -> 4
        else -> 0
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ticket ${currentTicket.ticketNumber}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val message = viewModel.generateWhatsAppMessage(currentTicket, companyProfile?.companyName ?: "Neo")
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://api.whatsapp.com/send?phone=${currentTicket.customerPhone.replace(Regex("[^0-9+]"), "")}&text=${Uri.encode(message)}")
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/?text=${Uri.encode(message)}"))
                            context.startActivity(browserIntent)
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "WhatsApp Alert", tint = Color(0xFF25D366))
                    }

                    if (currentUser?.role == "ADMIN") {
                        IconButton(onClick = { showDeleteConfirmationDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = "Remove Job",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // CANCELLED ALERT BANNER
            if (currentTicket.status == "CANCELLED") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "This Job Has Been Cancelled",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "Work is suspended. You can reopen the job anytime.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                            }
                        }

                        if (!isCustomer) {
                            Button(
                                onClick = { viewModel.reopenTicket(currentTicket.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reopen")
                            }
                        }
                    }
                }
            }

            // 1. PROMINENT DECISION CARD (WHEN STATUS IS QUOTATION_SENT)
            if (currentTicket.status == "QUOTATION_SENT") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PriorityHigh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Customer Quotation Decision Required",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = "Please review the repair estimate and Approve or Reject:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Full breakdown of parts and labor
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(text = "Cost Breakdown Summary", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(6.dp))
                                if (totalDiscount > 0) {
                                    DetailRow(label = "Gross Cost (Parts + Labor)", value = "$curr%.2f".format(grossPartsTotal + grossLaborTotal))
                                    DetailRow(label = "Discount Savings", value = "-$curr%.2f".format(totalDiscount))
                                } else {
                                    DetailRow(label = "Parts Estimate", value = "$curr%.2f".format(partsTotal))
                                    DetailRow(label = "Labor & Service", value = "$curr%.2f".format(laborTotal))
                                }
                                DetailRow(label = if (totalDiscount > 0) "Taxable Subtotal" else "Subtotal", value = "$curr%.2f".format(subtotal))
                                if (isTaxEnabled) {
                                    DetailRow(label = "GST (${currentTicket.gstRate}%)", value = "$curr%.2f".format(gstAmount))
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Total Repair Estimate:", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                    Text("$curr%.2f".format(totalEstimate), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Two prominent action buttons: Approve Estimate (Green) & Reject Estimate (Red)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    approvalActionIsApprove = true
                                    showApprovalDialog = true
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Approve Estimate", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    approvalActionIsApprove = false
                                    showApprovalDialog = true
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Cancel, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reject Estimate", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else if (currentTicket.status == "APPROVED" || currentTicket.status == "REJECTED") {
                // Persistent confirmation badges (Quotation Approved / Quotation Rejected)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (currentTicket.status == "APPROVED") Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (currentTicket.status == "APPROVED") Icons.Default.CheckCircle else Icons.Default.HighlightOff,
                            contentDescription = null,
                            tint = if (currentTicket.status == "APPROVED") Color(0xFF2E7D32) else Color(0xFFC62828),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (currentTicket.status == "APPROVED") "Quotation Approved" else "Quotation Rejected",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (currentTicket.status == "APPROVED") Color(0xFF1B5E20) else Color(0xFFB71C1C)
                            )
                            if (currentTicket.customerApprovalNote.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Customer Note: ${currentTicket.customerApprovalNote}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Minimal Stages Pipeline Stepper
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pipeline Progress",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        StatusChip(status = currentTicket.status)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        minimalStages.forEachIndexed { idx, (_, label, icon) ->
                            val isCompleted = idx < currentStageIdx
                            val isCurrent = idx == currentStageIdx

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = when {
                                        isCurrent -> MaterialTheme.colorScheme.primary
                                        isCompleted -> Color(0xFF2E7D32)
                                        else -> MaterialTheme.colorScheme.outlineVariant
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (isCompleted) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        } else {
                                            Icon(icon, contentDescription = null, tint = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal),
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // PAYMENT STATUS & SETTLEMENT CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (currentTicket.paymentStatus == "RECEIVED") Color(0xFFF0FDF4) else Color(0xFFFFFBEB)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (currentTicket.paymentStatus == "RECEIVED") Color(0xFF16A34A).copy(alpha = 0.5f) else Color(0xFFD97706).copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = if (currentTicket.paymentStatus == "RECEIVED") Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (currentTicket.paymentStatus == "RECEIVED") Icons.Default.CheckCircle else Icons.Default.Pending,
                                        contentDescription = null,
                                        tint = if (currentTicket.paymentStatus == "RECEIVED") Color(0xFF16A34A) else Color(0xFFD97706),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Job Payment Settlement",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (currentTicket.paymentStatus == "RECEIVED") "PAYMENT RECEIVED (${currentTicket.paymentMode})" else "PAYMENT PENDING",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (currentTicket.paymentStatus == "RECEIVED") Color(0xFF15803D) else Color(0xFFB45309)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (currentTicket.paymentStatus == "RECEIVED") Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
                        ) {
                            Text(
                                text = if (currentTicket.paymentStatus == "RECEIVED") "PAID" else "PENDING",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (currentTicket.paymentStatus == "RECEIVED") Color(0xFF15803D) else Color(0xFFB45309)
                            )
                        }
                    }

                    if (!isCustomer) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = if (currentTicket.paymentStatus == "RECEIVED") Color(0xFFBBF7D0) else Color(0xFFFDE68A))
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (currentTicket.paymentStatus != "RECEIVED") {
                                Button(
                                    onClick = { showPaymentModeDialog = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                                ) {
                                    Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Mark Payment Received", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.updateTicketPaymentStatus(currentTicket.id, "PENDING")
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Change to Payment Pending", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }
                }
            }

            // Customer & Device Details
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Customer & Site Details", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        
                        IconButton(
                            onClick = {
                                val message = viewModel.generateWhatsAppStatusMessage(currentTicket, companyProfile?.companyName ?: "Neo")
                                val cleanPhone = currentTicket.customerPhone.replace(Regex("[^0-9+]"), "")
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(message)}")
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/?text=${Uri.encode(message)}"))
                                    context.startActivity(browserIntent)
                                }
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Send WhatsApp Status", tint = Color(0xFF25D366))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    DetailRow(label = "Customer Name", value = currentTicket.customerName)
                    DetailRow(label = "Phone", value = currentTicket.customerPhone)
                    DetailRow(label = "Email", value = currentTicket.customerEmail)
                    DetailRow(label = "Device", value = currentTicket.deviceName)
                    DetailRow(label = "Reported Issue", value = currentTicket.issueDescription)
                    DetailRow(label = "Site Location", value = currentTicket.locationAddress)
                    DetailRow(label = "Assigned Tech", value = currentTicket.assignedTechnicianName.ifEmpty { "Unassigned" })
                    if (currentTicket.diagnosticNotes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        DetailRow(label = "Repair Status Remarks", value = currentTicket.diagnosticNotes)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Quick WhatsApp Share:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // WhatsApp Quick Actions: Send Status vs Send Invoice/Quotation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val message = viewModel.generateWhatsAppStatusMessage(currentTicket, companyProfile?.companyName ?: "Neo")
                                val cleanPhone = currentTicket.customerPhone.replace(Regex("[^0-9+]"), "")
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(message)}")
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/?text=${Uri.encode(message)}"))
                                    context.startActivity(browserIntent)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Send Status", color = Color.White, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }

                        Button(
                            onClick = {
                                val message = viewModel.generateWhatsAppInvoiceMessage(currentTicket, companyProfile?.companyName ?: "Neo", curr)
                                val cleanPhone = currentTicket.customerPhone.replace(Regex("[^0-9+]"), "")
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(message)}")
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/?text=${Uri.encode(message)}"))
                                    context.startActivity(browserIntent)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF128C7E)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Send Invoice", color = Color.White, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }

            // Quotation Items Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Quotation & Itemized Bill", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text(text = "${quotationItems.size} item(s) listed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }

                        if (!isCustomer) {
                            OutlinedButton(
                                onClick = { showQuotationItemsDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Manage Items", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (quotationItems.isEmpty()) {
                        Text(
                            text = "No quotation items added yet. Click 'Add / Manage Items' to add spare parts and labor.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else {
                        quotationItems.forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = item.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text(item.category, style = MaterialTheme.typography.labelSmall) },
                                            modifier = Modifier.height(24.dp)
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${item.quantity} x $curr%.2f".format(item.unitPrice),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (item.effectiveDiscount > 0) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFF2E7D32).copy(alpha = 0.12f)
                                            ) {
                                                Text(
                                                    text = "Disc: -$curr%.2f".format(item.effectiveDiscount),
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = Color(0xFF2E7D32),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "$curr%.2f".format(item.total),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    if (item.effectiveDiscount > 0) {
                                        Text(
                                            text = "$curr%.2f".format(item.grossTotal),
                                            style = MaterialTheme.typography.labelSmall.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough),
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                            if (index < quotationItems.size - 1) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    if (totalDiscount > 0) {
                        DetailRow(label = "Gross Amount", value = "$curr%.2f".format(grossPartsTotal + grossLaborTotal))
                        DetailRow(label = "Discount Applied", value = "-$curr%.2f".format(totalDiscount))
                        DetailRow(label = if (isGstActive) "Taxable Subtotal" else "Net Subtotal", value = "$curr%.2f".format(subtotal))
                    } else {
                        DetailRow(label = "Subtotal", value = "$curr%.2f".format(subtotal))
                    }
                    if (isGstActive) {
                        DetailRow(label = "GST (${currentTicket.gstRate}%)", value = "$curr%.2f".format(gstAmount))
                    }
                    DetailRow(label = if (isGstActive) "Grand Total (incl. GST)" else "Total Amount", value = "$curr%.2f".format(totalEstimate))

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onNavigateQuotationPreview,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("View Quotation")
                        }

                        Button(
                            onClick = onNavigateInvoicePreview,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("View Invoice")
                        }
                    }
                }
            }

            // Timeline component for the ticket detail view (Admin Only View)
            if (currentUser?.role == "ADMIN") {
                AdminStatusTimelineComponent(ticket = currentTicket)
            }

            // Action Buttons for Staff/Admin/Tech
            if (!isCustomer) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { showStatusDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Update, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Update Repair Pipeline Status")
                    }

                    if (currentTicket.status != "CANCELLED") {
                        OutlinedButton(
                            onClick = {
                                cancelReasonInput = ""
                                showCancelDialog = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                            border = BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = null, tint = Color(0xFFDC2626))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cancel Job Ticket", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { viewModel.reopenTicket(currentTicket.id) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reopen / Restore Cancelled Job", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (currentUser?.role == "ADMIN") {
                        OutlinedButton(
                            onClick = { showTechDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Engineering, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Assign Technician")
                        }

                        OutlinedButton(
                            onClick = { showDeleteConfirmationDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Remove / Delete Job Ticket (Admin Only)", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // MINIMAL STAGES + ONLY ONE COLUMN REMARKS STATUS DIALOG
    if (showStatusDialog) {
        var selectedStatus by remember { mutableStateOf(currentTicket.status) }
        var singleRemarksNote by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showStatusDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Update Repair Status")
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Select Minimal Stage:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))

                    // Minimal Stages row/grid
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        minimalStages.forEach { (st, label, icon) ->
                            OutlinedCard(
                                onClick = { selectedStatus = st },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = if (selectedStatus == st) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(
                                    width = if (selectedStatus == st) 2.dp else 1.dp,
                                    color = if (selectedStatus == st) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (selectedStatus == st) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (selectedStatus == st) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (selectedStatus == st) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    // ONLY ONE COLUMN: REMARKS
                    Text("Remarks Column:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    OutlinedTextField(
                        value = singleRemarksNote,
                        onValueChange = { singleRemarksNote = it },
                        label = { Text("Repair Status Remarks") },
                        placeholder = { Text("Enter repair notes, findings, or technician remarks...") },
                        leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateTicketStatus(
                        ticketId = currentTicket.id,
                        newStatus = selectedStatus,
                        notes = singleRemarksNote.ifBlank { null }
                    )
                    showStatusDialog = false
                }) {
                    Text("Save Status")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStatusDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Assign Technician Dialog
    if (showTechDialog) {
        AlertDialog(
            onDismissRequest = { showTechDialog = false },
            title = { Text("Assign Technician") },
            text = {
                Column {
                    technicians.forEach { tech ->
                        TextButton(
                            onClick = {
                                viewModel.assignTechnician(currentTicket.id, tech.id, tech.fullName)
                                showTechDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(tech.fullName, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showTechDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Quotation Items Management Dialog
    if (showQuotationItemsDialog) {
        val workingItems = remember { mutableStateListOf<QuotationItem>().apply { addAll(quotationItems) } }
        var isGstOn by remember { mutableStateOf(currentTicket.gstRate > 0.0) }
        var gstRateInput by remember { mutableStateOf(if (currentTicket.gstRate > 0.0) currentTicket.gstRate.toString() else "18.0") }
        var editingItemIndex by remember { mutableStateOf<Int?>(null) }

        AlertDialog(
            onDismissRequest = { showQuotationItemsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.RequestQuote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Quotation Items & Costing")
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Line Items (${workingItems.size})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Button(
                            onClick = { showAddItemDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Item")
                        }
                    }

                    if (workingItems.isEmpty()) {
                        Text("No items. Click '+ Add Item' to insert parts, labor, or services.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    } else {
                        workingItems.forEachIndexed { index, item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = item.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            SuggestionChip(
                                                onClick = {},
                                                label = { Text(item.category, style = MaterialTheme.typography.labelSmall) },
                                                modifier = Modifier.height(22.dp)
                                            )
                                        }
                                        Text(
                                            text = "Qty: ${item.quantity} x $curr%.2f = $curr%.2f".format(item.unitPrice, item.grossTotal),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (item.effectiveDiscount > 0) {
                                            Text(
                                                text = "Discount: -$curr%.2f (%s) • Net: $curr%.2f".format(
                                                    item.effectiveDiscount,
                                                    if (item.discountPercent > 0) "${item.discountPercent.toInt()}%" else "Manual",
                                                    item.total
                                                ),
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = Color(0xFF2E7D32)
                                            )
                                        }
                                    }
                                    Row {
                                        IconButton(onClick = { editingItemIndex = index }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit / Discount", tint = MaterialTheme.colorScheme.primary)
                                        }
                                        IconButton(onClick = { workingItems.removeAt(index) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    if (isTaxEnabled) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Percent, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Include GST in Quotation", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    Text(if (isGstOn) "GST ON (${gstRateInput}%)" else "GST OFF (0%)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                            Switch(
                                checked = isGstOn,
                                onCheckedChange = { isGstOn = it },
                                modifier = Modifier.testTag("quotation_gst_on_off_switch")
                            )
                        }

                        if (isGstOn) {
                            Spacer(modifier = Modifier.height(2.dp))
                            OutlinedTextField(
                                value = gstRateInput,
                                onValueChange = { gstRateInput = it },
                                label = { Text("GST Rate (%)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    val calcGross = workingItems.sumOf { it.grossTotal }
                    val calcDiscount = workingItems.sumOf { it.effectiveDiscount }
                    val calcSubtotal = workingItems.sumOf { it.total }
                    val calcGst = if (isTaxEnabled && isGstOn) calcSubtotal * ((gstRateInput.toDoubleOrNull() ?: 18.0) / 100.0) else 0.0
                    val calcTotal = calcSubtotal + calcGst

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (calcDiscount > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Gross Subtotal:", style = MaterialTheme.typography.bodySmall)
                                Text("$curr%.2f".format(calcGross), style = MaterialTheme.typography.bodySmall)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Discount:", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF2E7D32))
                                Text("-$curr%.2f".format(calcDiscount), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF2E7D32))
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (calcDiscount > 0) "Taxable Net Subtotal:" else "Subtotal:", style = MaterialTheme.typography.bodyMedium)
                            Text("$curr%.2f".format(calcSubtotal), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                        }
                        if (isTaxEnabled && isGstOn) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("GST (${gstRateInput}%):", style = MaterialTheme.typography.bodyMedium)
                                Text("$curr%.2f".format(calcGst), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Grand Total:", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text("$curr%.2f".format(calcTotal), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val rate = if (isTaxEnabled && isGstOn) (gstRateInput.toDoubleOrNull() ?: 18.0) else 0.0
                    viewModel.updateQuotationLineItems(currentTicket.id, workingItems.toList(), rate)
                    showQuotationItemsDialog = false
                }) {
                    Text("Save & Send to Customer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuotationItemsDialog = false }) {
                    Text("Cancel")
                }
            }
        )

        // Nested Dialog: Add Single Line Item with Manual Discount Entry
        if (showAddItemDialog) {
            var itemName by remember { mutableStateOf("") }
            var itemCategory by remember { mutableStateOf("Part") }
            var itemQty by remember { mutableStateOf("1") }
            var itemPrice by remember { mutableStateOf("") }
            var discountModeIsPercent by remember { mutableStateOf(false) }
            var discountInput by remember { mutableStateOf("0") }

            val parsedQty = itemQty.toIntOrNull() ?: 1
            val parsedPrice = itemPrice.toDoubleOrNull() ?: 0.0
            val gross = parsedQty * parsedPrice
            val parsedDiscVal = discountInput.toDoubleOrNull() ?: 0.0
            val calculatedDiscount = if (discountModeIsPercent) gross * (parsedDiscVal / 100.0) else parsedDiscVal
            val netTotal = (gross - calculatedDiscount).coerceAtLeast(0.0)

            AlertDialog(
                onDismissRequest = { showAddItemDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AddCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Quotation Item")
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = itemName,
                            onValueChange = { itemName = it },
                            label = { Text("Item Name / Description") },
                            placeholder = { Text("e.g. Battery Replacement / Motherboard repair") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Item Category:", style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Part", "Labor", "Service").forEach { cat ->
                                FilterChip(
                                    selected = itemCategory == cat,
                                    onClick = { itemCategory = cat },
                                    label = { Text(cat) }
                                )
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = itemQty,
                                onValueChange = { itemQty = it },
                                label = { Text("Quantity") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = itemPrice,
                                onValueChange = { itemPrice = it },
                                label = { Text("Unit Price ($curr)") },
                                modifier = Modifier.weight(1.5f)
                            )
                        }

                        // MANUAL DISCOUNT ENTRY OPTION
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.LocalOffer, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Discount (Manual Entry)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        FilterChip(
                                            selected = !discountModeIsPercent,
                                            onClick = { discountModeIsPercent = false },
                                            label = { Text("Amount ($curr)", style = MaterialTheme.typography.labelSmall) }
                                        )
                                        FilterChip(
                                            selected = discountModeIsPercent,
                                            onClick = { discountModeIsPercent = true },
                                            label = { Text("%", style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = discountInput,
                                    onValueChange = { discountInput = it },
                                    label = { Text(if (discountModeIsPercent) "Discount Percentage (%)" else "Discount Amount ($curr)") },
                                    placeholder = { Text(if (discountModeIsPercent) "e.g. 10" else "e.g. 200") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Preset discount quick buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("0%", "5%", "10%", "15%", "20%").forEach { preset ->
                                        AssistChip(
                                            onClick = {
                                                discountModeIsPercent = true
                                                discountInput = preset.replace("%", "")
                                            },
                                            label = { Text(preset, style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }

                                // Live calculation deduction banner
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Gross: $curr%.2f".format(gross), style = MaterialTheme.typography.bodySmall)
                                        Text("Disc: -$curr%.2f".format(calculatedDiscount), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF2E7D32))
                                        Text("Net: $curr%.2f".format(netTotal), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (itemName.isNotBlank()) {
                                val discountFlat = if (!discountModeIsPercent) parsedDiscVal else 0.0
                                val discountPct = if (discountModeIsPercent) parsedDiscVal else 0.0
                                workingItems.add(
                                    QuotationItem(
                                        name = itemName.trim(),
                                        category = itemCategory,
                                        quantity = parsedQty,
                                        unitPrice = parsedPrice,
                                        discount = discountFlat,
                                        discountPercent = discountPct
                                    )
                                )
                                showAddItemDialog = false
                            }
                        }
                    ) {
                        Text("Add to Quote")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddItemDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Nested Dialog: Edit Item & Manual Discount
        if (editingItemIndex != null && editingItemIndex!! in workingItems.indices) {
            val itemToEdit = workingItems[editingItemIndex!!]
            var editName by remember(editingItemIndex) { mutableStateOf(itemToEdit.name) }
            var editCategory by remember(editingItemIndex) { mutableStateOf(itemToEdit.category) }
            var editQty by remember(editingItemIndex) { mutableStateOf(itemToEdit.quantity.toString()) }
            var editPrice by remember(editingItemIndex) { mutableStateOf(itemToEdit.unitPrice.toString()) }
            var editModePercent by remember(editingItemIndex) { mutableStateOf(itemToEdit.discountPercent > 0) }
            var editDiscountInput by remember(editingItemIndex) {
                mutableStateOf(if (itemToEdit.discountPercent > 0) itemToEdit.discountPercent.toString() else itemToEdit.discount.toString())
            }

            val eQty = editQty.toIntOrNull() ?: 1
            val ePrice = editPrice.toDoubleOrNull() ?: 0.0
            val eGross = eQty * ePrice
            val eDiscVal = editDiscountInput.toDoubleOrNull() ?: 0.0
            val eCalculatedDiscount = if (editModePercent) eGross * (eDiscVal / 100.0) else eDiscVal
            val eNetTotal = (eGross - eCalculatedDiscount).coerceAtLeast(0.0)

            AlertDialog(
                onDismissRequest = { editingItemIndex = null },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit Item & Manual Discount")
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Item Name") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Category:", style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Part", "Labor", "Service").forEach { cat ->
                                FilterChip(
                                    selected = editCategory == cat,
                                    onClick = { editCategory = cat },
                                    label = { Text(cat) }
                                )
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = editQty,
                                onValueChange = { editQty = it },
                                label = { Text("Qty") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = editPrice,
                                onValueChange = { editPrice = it },
                                label = { Text("Price ($curr)") },
                                modifier = Modifier.weight(1.5f)
                            )
                        }

                        // MANUAL DISCOUNT ENTRY OPTION
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.LocalOffer, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Manual Discount Entry", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        FilterChip(
                                            selected = !editModePercent,
                                            onClick = { editModePercent = false },
                                            label = { Text("Amount ($curr)", style = MaterialTheme.typography.labelSmall) }
                                        )
                                        FilterChip(
                                            selected = editModePercent,
                                            onClick = { editModePercent = true },
                                            label = { Text("%", style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = editDiscountInput,
                                    onValueChange = { editDiscountInput = it },
                                    label = { Text(if (editModePercent) "Discount Percentage (%)" else "Discount Amount ($curr)") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("0%", "5%", "10%", "15%", "20%").forEach { preset ->
                                        AssistChip(
                                            onClick = {
                                                editModePercent = true
                                                editDiscountInput = preset.replace("%", "")
                                            },
                                            label = { Text(preset, style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Gross: $curr%.2f".format(eGross), style = MaterialTheme.typography.bodySmall)
                                        Text("Disc: -$curr%.2f".format(eCalculatedDiscount), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF2E7D32))
                                        Text("Net: $curr%.2f".format(eNetTotal), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (editName.isNotBlank()) {
                                val discountFlat = if (!editModePercent) eDiscVal else 0.0
                                val discountPct = if (editModePercent) eDiscVal else 0.0
                                workingItems[editingItemIndex!!] = itemToEdit.copy(
                                    name = editName.trim(),
                                    category = editCategory,
                                    quantity = eQty,
                                    unitPrice = ePrice,
                                    discount = discountFlat,
                                    discountPercent = discountPct
                                )
                                editingItemIndex = null
                            }
                        }
                    ) {
                        Text("Update Item")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingItemIndex = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    // Customer Notes & Comments dialog for Approve / Reject
    if (showApprovalDialog) {
        var customerFeedbackNote by remember {
            mutableStateOf(
                if (approvalActionIsApprove) "Approved. Please proceed with the service." else "Price is high. Please cancel or reconsider."
            )
        }

        AlertDialog(
            onDismissRequest = { showApprovalDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (approvalActionIsApprove) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = null,
                        tint = if (approvalActionIsApprove) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (approvalActionIsApprove) "Approve Quotation" else "Reject Quotation")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (approvalActionIsApprove) "You are approving the repair estimate. Add instructions or notes for the technician/workshop:" else "You are rejecting this quotation. Please provide instructions or reason for rejection:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = customerFeedbackNote,
                        onValueChange = { customerFeedbackNote = it },
                        label = { Text("Customer Notes & Comments") },
                        placeholder = { Text("Enter your feedback, requirements, or instructions...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.approveOrRejectQuotation(currentTicket.id, approvalActionIsApprove, customerFeedbackNote)
                        showApprovalDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (approvalActionIsApprove) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(if (approvalActionIsApprove) "Confirm Approval" else "Confirm Rejection")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApprovalDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Cancel Job Dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Cancel Service Job?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Are you sure you want to cancel ticket #${currentTicket.ticketNumber}? This will halt active repair stages but maintain full audit history.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedTextField(
                        value = cancelReasonInput,
                        onValueChange = { cancelReasonInput = it },
                        label = { Text("Cancellation Reason (Optional)") },
                        placeholder = { Text("e.g. Customer cancelled, Component unavailable...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val reason = cancelReasonInput
                        showCancelDialog = false
                        viewModel.cancelTicket(currentTicket.id, reason)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Confirm Cancel", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Admin-Only Permanent Delete Confirmation Dialog
    if (showDeleteConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmationDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Delete Service Job?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Are you sure you want to permanently delete ticket #${currentTicket.ticketNumber} for ${currentTicket.customerName} (${currentTicket.deviceName})?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "⚠️ This action cannot be undone. All diagnostics, quotes, and timeline entries will be permanently removed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmationDialog = false
                        viewModel.deleteTicket(currentTicket.id, onSuccess = onBack)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete Job", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // DIALOG: SELECT PAYMENT MODE & MARK RECEIVED
    if (showPaymentModeDialog) {
        AlertDialog(
            onDismissRequest = { showPaymentModeDialog = false },
            icon = { Icon(Icons.Default.Payments, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(36.dp)) },
            title = { Text("Mark Payment Received", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select payment mode used by customer:", style = MaterialTheme.typography.bodyMedium)

                    val modes = listOf(
                        "CASH" to "Cash Payment",
                        "UPI" to "UPI / QR Scan (PhonePe, GPay)",
                        "CARD" to "Credit / Debit Card",
                        "BANK_TRANSFER" to "Bank Transfer / NEFT"
                    )

                    modes.forEach { (modeKey, modeLabel) ->
                        Surface(
                            onClick = { selectedPaymentMode = modeKey },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedPaymentMode == modeKey) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (selectedPaymentMode == modeKey) MaterialTheme.colorScheme.primary else Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedPaymentMode == modeKey,
                                    onClick = { selectedPaymentMode = modeKey }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(modeLabel, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (selectedPaymentMode == modeKey) FontWeight.Bold else FontWeight.Normal))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPaymentModeDialog = false
                        viewModel.updateTicketPaymentStatus(currentTicket.id, "RECEIVED", selectedPaymentMode)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                ) {
                    Text("Confirm Payment Received", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentModeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
    }
}
