package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TicketEntity
import com.example.viewmodel.ServiceViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerPortalScreen(
    viewModel: ServiceViewModel,
    onNavigateCreateTicket: () -> Unit,
    onTicketClick: (TicketEntity) -> Unit,
    onBack: () -> Unit
) {
    val allTickets by viewModel.allTickets.collectAsState()
    val companyProfile by viewModel.companyProfile.collectAsState()
    val curr = companyProfile?.currencySymbol ?: "$"

    val regPhone by viewModel.registeredCustomerPhone.collectAsState()
    val regName by viewModel.registeredCustomerName.collectAsState()
    val regEmail by viewModel.registeredCustomerEmail.collectAsState()

    val defaultSearchQuery by viewModel.customerSearchQuery.collectAsState()
    var searchPhoneOrEmail by remember(defaultSearchQuery, regPhone) { mutableStateOf(if (regPhone.isNotBlank()) regPhone else defaultSearchQuery) }
    var activeSearchQuery by remember(defaultSearchQuery, regPhone) { mutableStateOf(if (regPhone.isNotBlank()) regPhone else defaultSearchQuery) }

    var regInputName by remember { mutableStateOf("") }
    var regInputPhone by remember { mutableStateOf("") }
    var regInputEmail by remember { mutableStateOf("") }

    val filteredTickets = remember(allTickets, activeSearchQuery) {
        if (activeSearchQuery.isBlank()) emptyList()
        else {
            val q = activeSearchQuery.trim().lowercase()
            allTickets.filter {
                it.customerPhone.lowercase().contains(q) ||
                it.customerEmail.lowercase().contains(q) ||
                it.ticketNumber.lowercase().contains(q)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Live Service Tracking", fontWeight = FontWeight.Bold)
                        Text(
                            text = companyProfile?.companyName ?: "Service Center",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateCreateTicket) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = "New Job", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateCreateTicket,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Book Repair Job", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Customer Registration or Account Card
            if (regPhone.isNotBlank()) {
                // Registered Customer Profile Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = regName.ifEmpty { "Registered Customer" },
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "📱 $regPhone" + if (regEmail.isNotBlank()) " | ✉️ $regEmail" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(onClick = { viewModel.clearRegisteredCustomerAccount() }) {
                            Icon(Icons.Default.Logout, contentDescription = "Switch Account", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            } else {
                // Unregistered Customer: Simple 1-Step Registration Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Customer One-Time Registration",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Register once to instantly view all your service job details",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = regInputName,
                            onValueChange = { regInputName = it },
                            label = { Text("Full Name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = regInputPhone,
                            onValueChange = { regInputPhone = it },
                            label = { Text("Mobile Phone Number") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            placeholder = { Text("e.g. +91 98765 43210") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = regInputEmail,
                            onValueChange = { regInputEmail = it },
                            label = { Text("Email Address (Optional)") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                if (regInputName.isNotBlank() && regInputPhone.isNotBlank()) {
                                    viewModel.registerCustomerAccount(regInputName, regInputPhone, regInputEmail)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = regInputName.isNotBlank() && regInputPhone.isNotBlank()
                        ) {
                            Icon(Icons.Default.HowToReg, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Register & View My Job Details", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Input Card for Quick Ticket Lookup
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchPhoneOrEmail,
                            onValueChange = { searchPhoneOrEmail = it },
                            label = { Text("Track by Ticket # or Phone") },
                            placeholder = { Text("e.g. NEO-2026-001", fontSize = 13.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { activeSearchQuery = searchPhoneOrEmail },
                            modifier = Modifier.height(52.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Find", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (activeSearchQuery.isNotBlank()) "Tracked Service Status (${filteredTickets.size})" else "Track Your Service Job",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (activeSearchQuery.isNotBlank()) {
                    TextButton(onClick = {
                        searchPhoneOrEmail = ""
                        activeSearchQuery = ""
                    }) {
                        Text("Clear", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (activeSearchQuery.isBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Enter tracking # or register above to view job status",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (filteredTickets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No job found matching tracking # \"$activeSearchQuery\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredTickets, key = { it.id }) { ticket ->
                        ServiceStatusProgressCard(
                            ticket = ticket,
                            curr = curr,
                            isTaxEnabled = companyProfile?.taxEnabled ?: true,
                            onClick = { onTicketClick(ticket) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ServiceStatusProgressCard(
    ticket: TicketEntity,
    curr: String,
    isTaxEnabled: Boolean,
    onClick: () -> Unit
) {
    val currentStep = when (ticket.status) {
        "PENDING", "TECHNICIAN_ASSIGNED" -> 1
        "DIAGNOSTICS" -> 2
        "QUOTATION_SENT", "APPROVED" -> 3
        "IN_PROGRESS" -> 4
        "READY_FOR_DELIVERY", "COMPLETED" -> 5
        else -> 1
    }

    val isCancelled = ticket.status == "CANCELLED" || ticket.status == "REJECTED"
    val dateFormatter = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    val statusExplanation = when (ticket.status) {
        "PENDING" -> "Job registered successfully. Awaiting technician assignment."
        "TECHNICIAN_ASSIGNED" -> "Technician assigned: ${ticket.assignedTechnicianName.ifEmpty { "Field Tech" }}. Initial setup underway."
        "DIAGNOSTICS" -> "Device is undergoing technical hardware & software diagnostics."
        "QUOTATION_SENT" -> "Cost estimate prepared. Tap to review and approve quotation."
        "APPROVED" -> "Quotation approved! Parts allocation and repair scheduled."
        "IN_PROGRESS" -> "Service & repair currently in progress at workshop."
        "READY_FOR_DELIVERY" -> "Repair completed & quality tested! Ready for pickup/delivery."
        "COMPLETED" -> "Service completed & delivered to customer."
        "CANCELLED", "REJECTED" -> "Service request has been cancelled/closed."
        else -> "Status: ${ticket.status.replace("_", " ")}"
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "#${ticket.ticketNumber}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = ticket.deviceName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
                StatusChip(status = ticket.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Status Description Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isCancelled) Color(0xFFFEE2E2) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isCancelled) Icons.Default.Cancel else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (isCancelled) Color(0xFFDC2626) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = statusExplanation,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = if (isCancelled) Color(0xFF991B1B) else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (!isCancelled) {
                Spacer(modifier = Modifier.height(14.dp))

                // 5-Step Progress Tracker (1 -> 5)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val steps = listOf("Received", "Diagnostic", "Quotation", "Repairing", "Ready")
                    steps.forEachIndexed { index, stepName ->
                        val stepNum = index + 1
                        val isDone = stepNum <= currentStep
                        val isCurrent = stepNum == currentStep

                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = CircleShape,
                                color = when {
                                    isCurrent -> MaterialTheme.colorScheme.primary
                                    isDone -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isDone && !isCurrent) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    } else {
                                        Text(
                                            text = "$stepNum",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                            color = if (isDone || isCurrent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stepName,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Updated: ${dateFormatter.format(Date(ticket.updatedAt))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Text(
                    text = "Tap for details ➔",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun CustomerTicketItem(
    ticket: TicketEntity,
    curr: String,
    isTaxEnabled: Boolean,
    onClick: () -> Unit
) {
    val isQuotePending = ticket.status == "QUOTATION_SENT"
    val dateFormatter = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isQuotePending) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isQuotePending) 3.dp else 1.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isQuotePending) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "#${ticket.ticketNumber}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = dateFormatter.format(Date(ticket.createdAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                StatusChip(status = ticket.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = ticket.deviceName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Issue: ${ticket.issueDescription}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )

            // Quotation Action Banner for Customer
            if (isQuotePending) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Quotation Ready • Tap to Review",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }

                        val items = com.example.data.QuotationItem.parseList(ticket.quotationItemsJson)
                        val subtotal = if (items.isNotEmpty()) items.sumOf { it.total } else (ticket.partsCost + ticket.laborCost)
                        val isGstActive = isTaxEnabled && ticket.gstRate > 0.0
                        val total = subtotal + (if (isGstActive) subtotal * (ticket.gstRate / 100.0) else 0.0)

                        Text(
                            text = "$curr%.2f".format(total),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.HomeRepairService,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = ticket.serviceType,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Text(
                    text = "Tech: ${ticket.assignedTechnicianName.ifEmpty { "Assigned Soon" }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun StatusChip(status: String) {
    val colorsPair = when (status) {
        "PENDING" -> Color(0xFFFEF3C7) to Color(0xFFD97706)
        "TECHNICIAN_ASSIGNED", "DIAGNOSTICS" -> Color(0xFFEDE9FE) to Color(0xFF7C3AED)
        "QUOTATION_SENT" -> Color(0xFFFFEDD5) to Color(0xFFEA580C)
        "APPROVED" -> Color(0xFFD1FAE5) to Color(0xFF059669)
        "IN_PROGRESS" -> Color(0xFFE0F2FE) to Color(0xFF0284C7)
        "REJECTED", "CANCELLED" -> Color(0xFFFEE2E2) to Color(0xFFDC2626)
        "READY_FOR_DELIVERY", "COMPLETED" -> Color(0xFFDCFCE7) to Color(0xFF16A34A)
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    val containerColor = colorsPair.first
    val contentColor = colorsPair.second

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor
    ) {
        Text(
            text = status.replace("_", " "),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
            color = contentColor
        )
    }
}
