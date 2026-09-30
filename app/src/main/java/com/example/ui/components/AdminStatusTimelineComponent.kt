package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.TicketEntity
import com.example.data.TicketTimelineEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun getStatusColor(status: String): Color {
    return when (status) {
        "PENDING" -> Color(0xFFF57C00)
        "TECHNICIAN_ASSIGNED" -> Color(0xFF1976D2)
        "DIAGNOSTICS" -> Color(0xFF7B1FA2)
        "QUOTATION_SENT" -> Color(0xFFE65100)
        "APPROVED" -> Color(0xFF2E7D32)
        "REJECTED" -> Color(0xFFC62828)
        "IN_PROGRESS" -> Color(0xFF0288D1)
        "READY_FOR_DELIVERY" -> Color(0xFF00796B)
        "COMPLETED" -> Color(0xFF2E7D32)
        else -> Color(0xFF616161)
    }
}

private fun getStatusIcon(status: String): ImageVector {
    return when (status) {
        "PENDING" -> Icons.Default.Inbox
        "TECHNICIAN_ASSIGNED" -> Icons.Default.PersonPin
        "DIAGNOSTICS" -> Icons.Default.Search
        "QUOTATION_SENT" -> Icons.Default.RequestQuote
        "APPROVED" -> Icons.Default.CheckCircle
        "REJECTED" -> Icons.Default.Cancel
        "IN_PROGRESS" -> Icons.Default.Handyman
        "READY_FOR_DELIVERY" -> Icons.Default.Inventory
        "COMPLETED" -> Icons.Default.DoneAll
        else -> Icons.Default.Info
    }
}

@Composable
fun AdminStatusTimelineComponent(
    ticket: TicketEntity,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }

    val timelineEntries = remember(ticket.statusTimelineJson, ticket.status, ticket.updatedAt) {
        val parsed = TicketTimelineEntry.parseList(ticket.statusTimelineJson)
        if (parsed.isNotEmpty()) {
            parsed.sortedBy { it.timestamp }
        } else {
            val fallback = mutableListOf<TicketTimelineEntry>()
            fallback.add(
                TicketTimelineEntry(
                    status = "PENDING",
                    updatedByName = ticket.customerName,
                    updatedByRole = "CUSTOMER",
                    remarks = "Ticket registered for ${ticket.deviceName}",
                    timestamp = ticket.createdAt
                )
            )
            if (ticket.assignedTechnicianName.isNotBlank()) {
                fallback.add(
                    TicketTimelineEntry(
                        status = "TECHNICIAN_ASSIGNED",
                        updatedByName = "Admin",
                        updatedByRole = "ADMIN",
                        remarks = "Assigned to ${ticket.assignedTechnicianName}",
                        timestamp = (ticket.createdAt + ticket.updatedAt) / 2
                    )
                )
            }
            if (ticket.status != "PENDING" && ticket.status != "TECHNICIAN_ASSIGNED") {
                fallback.add(
                    TicketTimelineEntry(
                        status = ticket.status,
                        updatedByName = if (ticket.status == "APPROVED" || ticket.status == "REJECTED") ticket.customerName else (ticket.assignedTechnicianName.ifBlank { "Admin" }),
                        updatedByRole = if (ticket.status == "APPROVED" || ticket.status == "REJECTED") "CUSTOMER" else "ADMIN",
                        remarks = ticket.diagnosticNotes.ifBlank { "Status updated to ${ticket.status.replace("_", " ")}" },
                        timestamp = ticket.updatedAt
                    )
                )
            }
            fallback
        }
    }

    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("admin_status_timeline_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timeline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Status History & Audit Trail",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = { isExpanded = !isExpanded }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Admin Only",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            if (isExpanded) {
                Text(
                    text = "Chronological record of status transitions, staff actions, and timestamps.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                timelineEntries.forEachIndexed { index, entry ->
                    val isLast = index == timelineEntries.size - 1
                    val nodeColor = getStatusColor(entry.status)
                    val nodeIcon = getStatusIcon(entry.status)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(36.dp)
                                .fillMaxHeight()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = nodeColor.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(2.dp, nodeColor),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = nodeIcon,
                                        contentDescription = null,
                                        tint = nodeColor,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }

                            if (!isLast) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .weight(1f)
                                        .background(MaterialTheme.colorScheme.outlineVariant)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(bottom = if (isLast) 4.dp else 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = entry.status.replace("_", " "),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = dateFormatter.format(Date(entry.timestamp)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(3.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (entry.updatedByRole == "ADMIN") Icons.Default.Security else if (entry.updatedByRole == "TECHNICIAN") Icons.Default.Engineering else Icons.Default.Person,
                                    contentDescription = null,
                                    tint = if (entry.updatedByRole == "ADMIN") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Updated by: ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = entry.updatedByName,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = entry.updatedByRole,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }

                            if (entry.remarks.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ChatBubbleOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = entry.remarks,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
