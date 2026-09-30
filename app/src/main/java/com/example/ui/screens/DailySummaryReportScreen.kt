package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TicketEntity
import com.example.viewmodel.ServiceViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailySummaryReportScreen(
    viewModel: ServiceViewModel,
    onTicketClick: (TicketEntity) -> Unit,
    onBack: () -> Unit
) {
    val allTickets by viewModel.allTickets.collectAsState()
    val technicians by viewModel.technicians.collectAsState()
    val companyProfile by viewModel.companyProfile.collectAsState()
    val context = LocalContext.current
    val curr = companyProfile?.currencySymbol ?: "$"

    var selectedTab by remember { mutableStateOf(0) } // 0: Daily, 1: Monthly
    val tabs = listOf("Daily Report", "Monthly Report")

    // Daily Calendar State
    var selectedDailyCalendar by remember { mutableStateOf(Calendar.getInstance()) }

    // Monthly Calendar State
    var selectedMonthlyCalendar by remember { mutableStateOf(Calendar.getInstance()) }

    // DAILY TIME BOUNDS
    val startOfDayMillis = remember(selectedDailyCalendar) {
        val c = selectedDailyCalendar.clone() as Calendar
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        c.timeInMillis
    }

    val endOfDayMillis = remember(startOfDayMillis) {
        startOfDayMillis + 24 * 60 * 60 * 1000 - 1
    }

    val isToday = remember(startOfDayMillis) {
        val nowCal = Calendar.getInstance()
        nowCal.set(Calendar.HOUR_OF_DAY, 0)
        nowCal.set(Calendar.MINUTE, 0)
        nowCal.set(Calendar.SECOND, 0)
        nowCal.set(Calendar.MILLISECOND, 0)
        nowCal.timeInMillis == startOfDayMillis
    }

    val isYesterday = remember(startOfDayMillis) {
        val nowCal = Calendar.getInstance()
        nowCal.add(Calendar.DAY_OF_YEAR, -1)
        nowCal.set(Calendar.HOUR_OF_DAY, 0)
        nowCal.set(Calendar.MINUTE, 0)
        nowCal.set(Calendar.SECOND, 0)
        nowCal.set(Calendar.MILLISECOND, 0)
        nowCal.timeInMillis == startOfDayMillis
    }

    val dailyDateDisplayStr = remember(selectedDailyCalendar) {
        val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
        sdf.format(selectedDailyCalendar.time)
    }

    val dailyShortDateStr = remember(selectedDailyCalendar) {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        sdf.format(selectedDailyCalendar.time)
    }

    // MONTHLY TIME BOUNDS
    val startOfMonthMillis = remember(selectedMonthlyCalendar) {
        val c = selectedMonthlyCalendar.clone() as Calendar
        c.set(Calendar.DAY_OF_MONTH, 1)
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        c.timeInMillis
    }

    val endOfMonthMillis = remember(selectedMonthlyCalendar) {
        val c = selectedMonthlyCalendar.clone() as Calendar
        c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH))
        c.set(Calendar.HOUR_OF_DAY, 23)
        c.set(Calendar.MINUTE, 59)
        c.set(Calendar.SECOND, 59)
        c.set(Calendar.MILLISECOND, 999)
        c.timeInMillis
    }

    val isCurrentMonth = remember(startOfMonthMillis) {
        val nowCal = Calendar.getInstance()
        nowCal.set(Calendar.DAY_OF_MONTH, 1)
        nowCal.set(Calendar.HOUR_OF_DAY, 0)
        nowCal.set(Calendar.MINUTE, 0)
        nowCal.set(Calendar.SECOND, 0)
        nowCal.set(Calendar.MILLISECOND, 0)
        nowCal.timeInMillis == startOfMonthMillis
    }

    val monthDisplayStr = remember(selectedMonthlyCalendar) {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        sdf.format(selectedMonthlyCalendar.time)
    }

    // ----------------- DAILY AGGREGATIONS -----------------
    val createdTodayTickets = remember(allTickets, startOfDayMillis, endOfDayMillis) {
        allTickets.filter { it.createdAt in startOfDayMillis..endOfDayMillis }
    }

    val completedTodayTickets = remember(allTickets, startOfDayMillis, endOfDayMillis) {
        allTickets.filter { it.status == "COMPLETED" && (it.updatedAt in startOfDayMillis..endOfDayMillis || it.createdAt in startOfDayMillis..endOfDayMillis) }
    }

    val inProgressTodayTickets = remember(allTickets, endOfDayMillis) {
        allTickets.filter { (it.status == "IN_PROGRESS" || it.status == "DIAGNOSTICS") && it.createdAt <= endOfDayMillis }
    }

    val pendingInvoicesTodayTickets = remember(allTickets, endOfDayMillis) {
        allTickets.filter {
            (it.status == "QUOTATION_SENT" || it.status == "APPROVED" || it.status == "IN_PROGRESS" || it.status == "READY_FOR_DELIVERY") &&
            it.createdAt <= endOfDayMillis
        }
    }

    val cancelledTodayTickets = remember(allTickets, startOfDayMillis, endOfDayMillis) {
        allTickets.filter { it.status == "CANCELLED" && it.updatedAt in startOfDayMillis..endOfDayMillis }
    }

    val totalDailyRevenue = remember(completedTodayTickets) {
        completedTodayTickets.sumOf { t ->
            val items = com.example.data.QuotationItem.parseList(t.quotationItemsJson)
            val subtotal = if (items.isNotEmpty()) items.sumOf { it.total } else (t.partsCost + t.laborCost)
            val gst = if (t.gstRate > 0) subtotal * (t.gstRate / 100.0) else 0.0
            subtotal + gst
        }
    }

    val totalDailyPendingReceivables = remember(pendingInvoicesTodayTickets) {
        pendingInvoicesTodayTickets.sumOf { t ->
            val items = com.example.data.QuotationItem.parseList(t.quotationItemsJson)
            val subtotal = if (items.isNotEmpty()) items.sumOf { it.total } else (t.partsCost + t.laborCost)
            val gst = if (t.gstRate > 0) subtotal * (t.gstRate / 100.0) else 0.0
            subtotal + gst
        }
    }

    val dailyDoorstep = remember(createdTodayTickets) { createdTodayTickets.count { it.serviceType.contains("Doorstep", ignoreCase = true) } }
    val dailyInstore = remember(createdTodayTickets) { createdTodayTickets.count { it.serviceType.contains("In-Store", ignoreCase = true) || it.serviceType.contains("Walk-in", ignoreCase = true) } }
    val dailyPickDrop = remember(createdTodayTickets) { createdTodayTickets.count { it.serviceType.contains("Pick", ignoreCase = true) } }

    // ----------------- MONTHLY AGGREGATIONS -----------------
    val createdMonthTickets = remember(allTickets, startOfMonthMillis, endOfMonthMillis) {
        allTickets.filter { it.createdAt in startOfMonthMillis..endOfMonthMillis }
    }

    val completedMonthTickets = remember(allTickets, startOfMonthMillis, endOfMonthMillis) {
        allTickets.filter { it.status == "COMPLETED" && (it.updatedAt in startOfMonthMillis..endOfMonthMillis || it.createdAt in startOfMonthMillis..endOfMonthMillis) }
    }

    val cancelledMonthTickets = remember(allTickets, startOfMonthMillis, endOfMonthMillis) {
        allTickets.filter { it.status == "CANCELLED" && (it.updatedAt in startOfMonthMillis..endOfMonthMillis || it.createdAt in startOfMonthMillis..endOfMonthMillis) }
    }

    val pendingMonthInvoicesTickets = remember(allTickets, endOfMonthMillis) {
        allTickets.filter {
            (it.status == "QUOTATION_SENT" || it.status == "APPROVED" || it.status == "IN_PROGRESS" || it.status == "READY_FOR_DELIVERY") &&
            it.createdAt <= endOfMonthMillis
        }
    }

    val totalMonthlyRevenue = remember(completedMonthTickets) {
        completedMonthTickets.sumOf { t ->
            val items = com.example.data.QuotationItem.parseList(t.quotationItemsJson)
            val subtotal = if (items.isNotEmpty()) items.sumOf { it.total } else (t.partsCost + t.laborCost)
            val gst = if (t.gstRate > 0) subtotal * (t.gstRate / 100.0) else 0.0
            subtotal + gst
        }
    }

    val totalMonthlyPendingReceivables = remember(pendingMonthInvoicesTickets) {
        pendingMonthInvoicesTickets.sumOf { t ->
            val items = com.example.data.QuotationItem.parseList(t.quotationItemsJson)
            val subtotal = if (items.isNotEmpty()) items.sumOf { it.total } else (t.partsCost + t.laborCost)
            val gst = if (t.gstRate > 0) subtotal * (t.gstRate / 100.0) else 0.0
            subtotal + gst
        }
    }

    val monthlyCompletionRate = remember(createdMonthTickets, completedMonthTickets) {
        if (createdMonthTickets.isEmpty()) 100 else ((completedMonthTickets.size.toDouble() / createdMonthTickets.size) * 100).toInt().coerceAtMost(100)
    }

    val avgTicketValue = remember(completedMonthTickets, totalMonthlyRevenue) {
        if (completedMonthTickets.isEmpty()) 0.0 else totalMonthlyRevenue / completedMonthTickets.size
    }

    val monthlyDoorstep = remember(createdMonthTickets) { createdMonthTickets.count { it.serviceType.contains("Doorstep", ignoreCase = true) } }
    val monthlyInstore = remember(createdMonthTickets) { createdMonthTickets.count { it.serviceType.contains("In-Store", ignoreCase = true) || it.serviceType.contains("Walk-in", ignoreCase = true) } }
    val monthlyPickDrop = remember(createdMonthTickets) { createdMonthTickets.count { it.serviceType.contains("Pick", ignoreCase = true) } }

    val dailyPartsRevenue = remember(completedTodayTickets) {
        completedTodayTickets.sumOf { t ->
            val items = com.example.data.QuotationItem.parseList(t.quotationItemsJson)
            if (items.isNotEmpty()) items.filter { it.category.contains("Part", ignoreCase = true) }.sumOf { it.total } else t.partsCost
        }
    }

    val dailyLaborRevenue = remember(completedTodayTickets) {
        completedTodayTickets.sumOf { t ->
            val items = com.example.data.QuotationItem.parseList(t.quotationItemsJson)
            if (items.isNotEmpty()) items.filter { it.category.contains("Labor", ignoreCase = true) || it.category.contains("Service", ignoreCase = true) }.sumOf { it.total } else t.laborCost
        }
    }

    val dailyGstTax = remember(completedTodayTickets) {
        completedTodayTickets.sumOf { t ->
            val items = com.example.data.QuotationItem.parseList(t.quotationItemsJson)
            val subtotal = if (items.isNotEmpty()) items.sumOf { it.total } else (t.partsCost + t.laborCost)
            if (t.gstRate > 0) subtotal * (t.gstRate / 100.0) else 0.0
        }
    }

    val monthlyPartsRevenue = remember(completedMonthTickets) {
        completedMonthTickets.sumOf { t ->
            val items = com.example.data.QuotationItem.parseList(t.quotationItemsJson)
            if (items.isNotEmpty()) items.filter { it.category.contains("Part", ignoreCase = true) }.sumOf { it.total } else t.partsCost
        }
    }

    val monthlyLaborRevenue = remember(completedMonthTickets) {
        completedMonthTickets.sumOf { t ->
            val items = com.example.data.QuotationItem.parseList(t.quotationItemsJson)
            if (items.isNotEmpty()) items.filter { it.category.contains("Labor", ignoreCase = true) || it.category.contains("Service", ignoreCase = true) }.sumOf { it.total } else t.laborCost
        }
    }

    val monthlyGstTax = remember(completedMonthTickets) {
        completedMonthTickets.sumOf { t ->
            val items = com.example.data.QuotationItem.parseList(t.quotationItemsJson)
            val subtotal = if (items.isNotEmpty()) items.sumOf { it.total } else (t.partsCost + t.laborCost)
            if (t.gstRate > 0) subtotal * (t.gstRate / 100.0) else 0.0
        }
    }

    fun exportDailyPdf() {
        val profile = companyProfile ?: return
        com.example.util.PdfExporter.generateAndOpenDailyReportPdf(
            context = context,
            companyProfile = profile,
            dateDisplayStr = dailyDateDisplayStr,
            createdTickets = createdTodayTickets,
            completedTickets = completedTodayTickets,
            inProgressTickets = inProgressTodayTickets,
            totalRevenue = totalDailyRevenue,
            partsRevenue = dailyPartsRevenue,
            laborRevenue = dailyLaborRevenue,
            gstTax = dailyGstTax
        )
    }

    fun exportMonthlyPdf() {
        val profile = companyProfile ?: return
        com.example.util.PdfExporter.generateAndOpenMonthlyReportPdf(
            context = context,
            companyProfile = profile,
            monthDisplayStr = monthDisplayStr,
            monthlyTickets = if (completedMonthTickets.isNotEmpty()) completedMonthTickets else createdMonthTickets,
            totalRevenue = totalMonthlyRevenue,
            partsRevenue = monthlyPartsRevenue,
            laborRevenue = monthlyLaborRevenue,
            gstTax = monthlyGstTax,
            completedCount = completedMonthTickets.size,
            inProgressCount = createdMonthTickets.size - completedMonthTickets.size
        )
    }

    // Share Functions
    fun shareDailySummaryWhatsApp() {
        val companyName = companyProfile?.companyName ?: "Service Center"
        val revStr = String.format(Locale.getDefault(), "%.2f", totalDailyRevenue)
        val pendStr = String.format(Locale.getDefault(), "%.2f", totalDailyPendingReceivables)
        val message = """
📊 *DAILY BUSINESS SUMMARY REPORT*
🏢 *$companyName*
📅 *Date:* $dailyDateDisplayStr
----------------------------------------
📈 *KEY PERFORMANCE METRICS:*
• 🎫 *New Tickets Created:* ${createdTodayTickets.size}
• ✅ *Jobs Completed Today:* ${completedTodayTickets.size}
• ⏳ *In-Progress Pipeline:* ${inProgressTodayTickets.size}
• 📑 *Pending Invoices:* ${pendingInvoicesTodayTickets.size}
• 🚫 *Cancelled Jobs:* ${cancelledTodayTickets.size}

💰 *FINANCIAL OVERVIEW:*
• 💵 *Completed Revenue:* $curr$revStr
• 💳 *Pending Receivables:* $curr$pendStr

🔧 *SERVICE BREAKDOWN:*
• 🏠 Doorstep: $dailyDoorstep
• 🏬 In-Store: $dailyInstore
• 🚚 Pick & Drop: $dailyPickDrop

----------------------------------------
_Generated via Neo Service Management_
        """.trimIndent()

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, message)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share Daily Summary Report"))
    }

    fun shareMonthlySummaryWhatsApp() {
        val companyName = companyProfile?.companyName ?: "Service Center"
        val revStr = String.format(Locale.getDefault(), "%.2f", totalMonthlyRevenue)
        val pendStr = String.format(Locale.getDefault(), "%.2f", totalMonthlyPendingReceivables)
        val avgStr = String.format(Locale.getDefault(), "%.2f", avgTicketValue)
        val message = """
📈 *MONTHLY BUSINESS SUMMARY REPORT*
🏢 *$companyName*
📅 *Period:* $monthDisplayStr
----------------------------------------
🏆 *EXECUTIVE HIGHLIGHTS:*
• 🎫 *Total Tickets Registered:* ${createdMonthTickets.size}
• ✅ *Total Jobs Completed:* ${completedMonthTickets.size}
• 🎯 *Completion Rate:* $monthlyCompletionRate%
• 💵 *Total Monthly Revenue:* $curr$revStr
• 💳 *Pending Receivables:* $curr$pendStr
• 🏷️ *Average Ticket Value:* $curr$avgStr
• 🚫 *Cancelled Tickets:* ${cancelledMonthTickets.size}

🔧 *SERVICE CHANNELS:*
• 🏠 Doorstep: $monthlyDoorstep (${if (createdMonthTickets.isNotEmpty()) (monthlyDoorstep * 100 / createdMonthTickets.size) else 0}%)
• 🏬 In-Store: $monthlyInstore (${if (createdMonthTickets.isNotEmpty()) (monthlyInstore * 100 / createdMonthTickets.size) else 0}%)
• 🚚 Pick & Drop: $monthlyPickDrop (${if (createdMonthTickets.isNotEmpty()) (monthlyPickDrop * 100 / createdMonthTickets.size) else 0}%)

----------------------------------------
_Generated via Neo Service Management_
        """.trimIndent()

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, message)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share Monthly Summary Report"))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (selectedTab == 0) "Daily Summary Report" else "Monthly Summary Report", fontWeight = FontWeight.Bold)
                        Text(
                            text = if (selectedTab == 0) (if (isToday) "Live Pulse • Today" else dailyShortDateStr) else monthDisplayStr,
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
                    IconButton(onClick = {
                        if (selectedTab == 0) exportDailyPdf() else exportMonthlyPdf()
                    }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Export Report PDF", tint = MaterialTheme.colorScheme.primary)
                    }

                    IconButton(onClick = {
                        if (selectedTab == 0) shareDailySummaryWhatsApp() else shareMonthlySummaryWhatsApp()
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share Report", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Tab Switcher: Daily vs Monthly
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (index == 0) Icons.Default.Today else Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (selectedTab == 0) {
                // ==================== DAILY SUMMARY TAB ====================
                // Daily Date Navigation Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val c = selectedDailyCalendar.clone() as Calendar
                                c.add(Calendar.DAY_OF_YEAR, -1)
                                selectedDailyCalendar = c
                            }
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day")
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = dailyDateDisplayStr,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (isToday) {
                                    Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                        Text("TODAY", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                } else {
                                    TextButton(
                                        onClick = { selectedDailyCalendar = Calendar.getInstance() },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                    ) {
                                        Text("Jump to Today", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }

                        IconButton(
                            onClick = {
                                val c = selectedDailyCalendar.clone() as Calendar
                                c.add(Calendar.DAY_OF_YEAR, 1)
                                selectedDailyCalendar = c
                            }
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next Day")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { exportDailyPdf() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Daily Report PDF", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // KPI Cards Grid
                    item {
                        Text(
                            text = "Daily Aggregations",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ReportMetricCard(
                                title = "New Tickets",
                                value = createdTodayTickets.size.toString(),
                                subtitle = "Created on this day",
                                icon = Icons.Default.AddCircleOutline,
                                color = Color(0xFF0284C7),
                                modifier = Modifier.weight(1f)
                            )
                            ReportMetricCard(
                                title = "Completed",
                                value = completedTodayTickets.size.toString(),
                                subtitle = "Jobs closed today",
                                icon = Icons.Default.CheckCircle,
                                color = Color(0xFF16A34A),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ReportMetricCard(
                                title = "Pending Invoices",
                                value = pendingInvoicesTodayTickets.size.toString(),
                                subtitle = "$curr%.2f awaiting".format(totalDailyPendingReceivables),
                                icon = Icons.Default.ReceiptLong,
                                color = Color(0xFFD97706),
                                modifier = Modifier.weight(1f)
                            )
                            ReportMetricCard(
                                title = "Daily Revenue",
                                value = "$curr%.0f".format(totalDailyRevenue),
                                subtitle = "From closed jobs",
                                icon = Icons.Default.AccountBalanceWallet,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Service Breakdown Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Daily Service Types",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    ServiceTypeCount(label = "Doorstep", count = dailyDoorstep, icon = Icons.Default.Home)
                                    ServiceTypeCount(label = "In-Store", count = dailyInstore, icon = Icons.Default.Storefront)
                                    ServiceTypeCount(label = "Pick & Drop", count = dailyPickDrop, icon = Icons.Default.LocalShipping)
                                }
                            }
                        }
                    }

                    // Technician Daily Allocation
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Technician Field Load Today",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${technicians.size} Techs",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                if (technicians.isEmpty()) {
                                    Text("No technicians registered yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                } else {
                                    technicians.forEach { tech ->
                                        val assignedCount = allTickets.count { it.assignedTechnicianId == tech.id && it.createdAt in startOfDayMillis..endOfDayMillis }
                                        val completedCount = allTickets.count { it.assignedTechnicianId == tech.id && it.status == "COMPLETED" && (it.updatedAt in startOfDayMillis..endOfDayMillis || it.createdAt in startOfDayMillis..endOfDayMillis) }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Engineering, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(tech.fullName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)) {
                                                    Text("Assigned: $assignedCount", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                }
                                                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFDCFCE7)) {
                                                    Text("Done: $completedCount", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF16A34A), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Ticket Items for Day
                    item {
                        Text(
                            text = "Tickets Activity on this Date (${createdTodayTickets.size})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (createdTodayTickets.isEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ) {
                                Text(
                                    text = "No new tickets were created on $dailyShortDateStr",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else {
                        items(createdTodayTickets, key = { it.id }) { ticket ->
                            AdminTicketCard(
                                ticket = ticket,
                                currencySymbol = curr,
                                onClick = { onTicketClick(ticket) }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            } else {
                // ==================== MONTHLY SUMMARY TAB ====================
                // Month Navigation Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val c = selectedMonthlyCalendar.clone() as Calendar
                                c.add(Calendar.MONTH, -1)
                                selectedMonthlyCalendar = c
                            }
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = monthDisplayStr,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (isCurrentMonth) {
                                Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                    Text("CURRENT MONTH", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            } else {
                                TextButton(
                                    onClick = { selectedMonthlyCalendar = Calendar.getInstance() },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                ) {
                                    Text("Current Month", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        IconButton(
                            onClick = {
                                val c = selectedMonthlyCalendar.clone() as Calendar
                                c.add(Calendar.MONTH, 1)
                                selectedMonthlyCalendar = c
                            }
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { exportMonthlyPdf() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Monthly Report PDF", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Monthly Executive KPI Cards
                    item {
                        Text(
                            text = "Monthly Performance Matrix",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ReportMetricCard(
                                title = "Monthly Revenue",
                                value = "$curr%.0f".format(totalMonthlyRevenue),
                                subtitle = "${completedMonthTickets.size} closed orders",
                                icon = Icons.Default.MonetizationOn,
                                color = Color(0xFF16A34A),
                                modifier = Modifier.weight(1f)
                            )
                            ReportMetricCard(
                                title = "Total Bookings",
                                value = createdMonthTickets.size.toString(),
                                subtitle = "Registered in $monthDisplayStr",
                                icon = Icons.Default.ConfirmationNumber,
                                color = Color(0xFF0284C7),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ReportMetricCard(
                                title = "Pending Receivables",
                                value = "$curr%.0f".format(totalMonthlyPendingReceivables),
                                subtitle = "${pendingMonthInvoicesTickets.size} awaiting completion",
                                icon = Icons.Default.PendingActions,
                                color = Color(0xFFD97706),
                                modifier = Modifier.weight(1f)
                            )
                            ReportMetricCard(
                                title = "Avg Ticket Value",
                                value = "$curr%.0f".format(avgTicketValue),
                                subtitle = "$monthlyCompletionRate% job completion",
                                icon = Icons.Default.TrendingUp,
                                color = Color(0xFF7C3AED),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Monthly Service Distribution
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Monthly Channel Distribution",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    val total = createdMonthTickets.size.coerceAtLeast(1)
                                    ServiceTypePercent(label = "Doorstep", count = monthlyDoorstep, percent = (monthlyDoorstep * 100 / total), icon = Icons.Default.Home)
                                    ServiceTypePercent(label = "In-Store", count = monthlyInstore, percent = (monthlyInstore * 100 / total), icon = Icons.Default.Storefront)
                                    ServiceTypePercent(label = "Pick & Drop", count = monthlyPickDrop, percent = (monthlyPickDrop * 100 / total), icon = Icons.Default.LocalShipping)
                                }
                            }
                        }
                    }

                    // Technician Monthly Leaderboard
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Technician Monthly Standings",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "$monthDisplayStr",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                if (technicians.isEmpty()) {
                                    Text("No technicians active in system.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                } else {
                                    technicians.forEach { tech ->
                                        val assignedInMonth = allTickets.filter { it.assignedTechnicianId == tech.id && it.createdAt in startOfMonthMillis..endOfMonthMillis }
                                        val completedInMonth = allTickets.filter { it.assignedTechnicianId == tech.id && it.status == "COMPLETED" && (it.updatedAt in startOfMonthMillis..endOfMonthMillis || it.createdAt in startOfMonthMillis..endOfMonthMillis) }
                                        val revenueByTech = completedInMonth.sumOf { t ->
                                            val items = com.example.data.QuotationItem.parseList(t.quotationItemsJson)
                                            val subtotal = if (items.isNotEmpty()) items.sumOf { it.total } else (t.partsCost + t.laborCost)
                                            val gst = if (t.gstRate > 0) subtotal * (t.gstRate / 100.0) else 0.0
                                            subtotal + gst
                                        }

                                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Engineering, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(tech.fullName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                                }

                                                Text(
                                                    text = "$curr%.0f".format(revenueByTech),
                                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                    color = Color(0xFF16A34A)
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Assigned: ${assignedInMonth.size}  •  Completed: ${completedInMonth.size}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.outline
                                                )

                                                val rate = if (assignedInMonth.isEmpty()) 100 else (completedInMonth.size * 100 / assignedInMonth.size).coerceAtMost(100)
                                                Text(
                                                    text = "Success: $rate%",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            LinearProgressIndicator(
                                                progress = { if (assignedInMonth.isEmpty()) 1f else (completedInMonth.size.toFloat() / assignedInMonth.size.toFloat()).coerceIn(0f, 1f) },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(6.dp)
                                                    .clip(RoundedCornerShape(3.dp)),
                                                color = Color(0xFF16A34A),
                                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Monthly Ticket Highlights List
                    item {
                        Text(
                            text = "Monthly Ticket Highlights (${createdMonthTickets.size} Registered)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (createdMonthTickets.isEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ) {
                                Text(
                                    text = "No tickets registered in $monthDisplayStr",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else {
                        items(createdMonthTickets.take(15), key = { it.id }) { ticket ->
                            AdminTicketCard(
                                ticket = ticket,
                                currencySymbol = curr,
                                onClick = { onTicketClick(ticket) }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = CircleShape,
                    color = color.copy(alpha = 0.15f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = color
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ServiceTypeCount(label: String, count: Int, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = count.toString(), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun ServiceTypePercent(label: String, count: Int, percent: Int, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = "$count ($percent%)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
    }
}
