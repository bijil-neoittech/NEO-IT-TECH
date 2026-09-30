package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.QuotationItem
import com.example.ui.components.CompanyLogoImage
import com.example.viewmodel.ServiceViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoicePreviewScreen(
    viewModel: ServiceViewModel,
    onBack: () -> Unit
) {
    val ticket by viewModel.selectedTicket.collectAsState()
    val companyProfile by viewModel.companyProfile.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isCustomer = currentUser?.role == "CUSTOMER" || currentUser == null
    val context = LocalContext.current

    if (ticket == null || companyProfile == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Invoice Preview") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { p ->
            Box(modifier = Modifier.fillMaxSize().padding(p), contentAlignment = Alignment.Center) {
                Text("No ticket data available.")
            }
        }
        return
    }

    val curTicket = ticket!!
    val profile = companyProfile!!
    val curr = profile.currencySymbol
    val isTaxEnabled = profile.taxEnabled
    val isGstActive = isTaxEnabled && curTicket.gstRate > 0.0
    val currentDateStr = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()) }
    var showDiscountDialog by remember { mutableStateOf(false) }

    val items = remember(curTicket.quotationItemsJson, curTicket.partsCost, curTicket.laborCost) {
        val parsed = QuotationItem.parseList(curTicket.quotationItemsJson)
        if (parsed.isNotEmpty()) parsed
        else {
            val fallback = mutableListOf<QuotationItem>()
            if (curTicket.partsCost > 0) {
                fallback.add(QuotationItem(name = curTicket.partsDescription.ifBlank { "Spare parts" }, category = "Part", quantity = 1, unitPrice = curTicket.partsCost))
            }
            if (curTicket.laborCost > 0) {
                fallback.add(QuotationItem(name = curTicket.laborDescription.ifBlank { "Service labor" }, category = "Labor", quantity = 1, unitPrice = curTicket.laborCost))
            }
            fallback
        }
    }

    val grossSubtotal = items.sumOf { it.grossTotal }.let { if (it > 0) it else (curTicket.partsCost + curTicket.laborCost) }
    val totalDiscount = items.sumOf { it.effectiveDiscount }
    val subtotal = items.sumOf { it.total }.let { if (it > 0) it else (curTicket.partsCost + curTicket.laborCost) }
    val totalGst = if (isGstActive) subtotal * (curTicket.gstRate / 100.0) else 0.0
    val cgst = totalGst / 2.0
    val sgst = totalGst / 2.0
    val grandTotal = subtotal + totalGst

    // WhatsApp share invoice format (Zero tax mention when isGstActive is false)
    val shareInvoiceText = buildString {
        append(if (isGstActive) "🧾 *OFFICIAL GST TAX INVOICE*\n" else "🧾 *OFFICIAL SERVICE INVOICE & RECEIPT*\n")
        append("🏢 ${profile.companyName}\n")
        if (isGstActive && profile.gstin.isNotBlank()) {
            append("📑 GSTIN: ${profile.gstin}\n")
        }
        append("🎫 Invoice Ref: INV-${curTicket.ticketNumber}\n")
        append("👤 Billed To: ${curTicket.customerName}\n")
        append("💻 Device: ${curTicket.deviceName}\n")
        if (totalDiscount > 0) {
            append("💰 Gross Amount: $curr" + "%.2f\n".format(grossSubtotal))
            append("🏷️ Discount Savings: -$curr" + "%.2f\n".format(totalDiscount))
            append("💰 Net Taxable: $curr" + "%.2f\n".format(subtotal))
        } else {
            append("💰 Subtotal: $curr" + "%.2f\n".format(subtotal))
        }
        if (isGstActive) {
            append("⚖️ CGST (${curTicket.gstRate / 2}%): $curr" + "%.2f\n".format(cgst))
            append("⚖️ SGST (${curTicket.gstRate / 2}%): $curr" + "%.2f\n".format(sgst))
        }
        append("💵 *Total Paid/Payable: $curr" + "%.2f*\n".format(grandTotal))
        append("🛡️ 30-Day Service Guarantee Included\n")
        append("📞 Helpline: ${profile.phone}")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isGstActive) "Tax Invoice" else "Invoice / Receipt")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isCustomer) {
                        // Manual Discount Data Entry Icon
                        IconButton(onClick = { showDiscountDialog = true }) {
                            Icon(Icons.Default.LocalOffer, contentDescription = "Enter Discounts", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // WhatsApp Share
                    IconButton(onClick = {
                        val sendIntent = Intent(Intent.ACTION_VIEW).apply {
                            val phone = curTicket.customerPhone.replace(Regex("[^0-9+]"), "")
                            data = Uri.parse("https://api.whatsapp.com/send?phone=$phone&text=${Uri.encode(shareInvoiceText)}")
                        }
                        try {
                            context.startActivity(sendIntent)
                        } catch (e: Exception) {
                            val generalShare = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareInvoiceText)
                            }
                            context.startActivity(Intent.createChooser(generalShare, "Share Invoice"))
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share Invoice", tint = Color(0xFF25D366))
                    }

                    // Print / PDF Icon
                    IconButton(onClick = {
                        com.example.util.PdfExporter.generateAndOpenPdf(context, curTicket, profile, isInvoice = true)
                    }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Export Invoice PDF", tint = MaterialTheme.colorScheme.primary)
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // MAIN PROFESSIONAL INVOICE CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    // SECTION 1: COMPANY HEADER WITH MAXIMUM ICONS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Business, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = profile.companyName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = profile.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = profile.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                Spacer(modifier = Modifier.width(10.dp))

                                Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = profile.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            // GSTIN only when tax is active
                            if (isGstActive && profile.gstin.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = "GSTIN: ${profile.gstin}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))
                        CompanyLogoImage(profile = profile, modifier = Modifier.size(64.dp))
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))

                    // SECTION 2: 2-COLUMN SIMPLIFIED INFO GRID (BILLED TO & INVOICE META)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Left Column: Billed To Customer Box
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Billed To", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = curTicket.customerName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PhoneIphone, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = curTicket.customerPhone, style = MaterialTheme.typography.bodySmall)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = curTicket.locationAddress, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.DevicesOther, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = curTicket.deviceName, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        }

                        // Right Column: Invoice Reference Box
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isTaxEnabled) "Tax Invoice" else "Invoice Ref", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = "INV-${curTicket.ticketNumber}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Event, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Date: $currentDateStr", style = MaterialTheme.typography.bodySmall)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(if (curTicket.serviceType == "Onsite") Icons.Default.DirectionsCar else Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Mode: ${curTicket.serviceType}", style = MaterialTheme.typography.bodySmall)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Payment: Completed", style = MaterialTheme.typography.bodySmall, color = Color(0xFF2E7D32))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // SECTION 3: ITEMIZED BILLING TABLE WITH MAXIMUM ICONS & DISCOUNT COLUMN
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(modifier = Modifier.weight(1.8f), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Item / Description", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Text(text = "Qty", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center)
                            Text(text = "Rate", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.weight(0.9f), textAlign = TextAlign.End)
                            Row(modifier = Modifier.weight(1.0f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocalOffer, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(text = "Discount", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Row(modifier = Modifier.weight(1.1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(text = "Amount ($curr)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimaryContainer, textAlign = TextAlign.End)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Item Rows with Discount Column
                    items.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1.8f)) {
                                Text(text = item.name, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                                Text(text = item.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }

                            Text(text = "${item.quantity}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center)
                            Text(text = "$curr" + "%.0f".format(item.unitPrice), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(0.9f), textAlign = TextAlign.End)

                            // Discount Column Data
                            Column(modifier = Modifier.weight(1.0f), horizontalAlignment = Alignment.End) {
                                if (item.effectiveDiscount > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF2E7D32).copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "-$curr" + "%.2f".format(item.effectiveDiscount),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFF2E7D32),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                    if (item.discountPercent > 0) {
                                        Text("(${item.discountPercent.toInt()}%)", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32))
                                    }
                                } else {
                                    Text("$curr" + "0.00", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                }
                            }

                            Text(
                                text = "$curr" + "%.2f".format(item.total),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.weight(1.1f),
                                textAlign = TextAlign.End
                            )
                        }

                        if (index < items.size - 1) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action Button: Enter / Adjust Discounts
                    if (!isCustomer) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = { showDiscountDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.LocalOffer, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Manual Discount Entry", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))

                    // SECTION 4: FINANCIAL BREAKDOWN WITH MAXIMUM ICONS & DISCOUNT
                    if (totalDiscount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Gross Subtotal Amount", style = MaterialTheme.typography.bodyMedium)
                            }
                            Text("$curr" + "%.2f".format(grossSubtotal), style = MaterialTheme.typography.bodyMedium)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocalOffer, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Discount Deduction", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = Color(0xFF2E7D32))
                            }
                            Text("-$curr" + "%.2f".format(totalDiscount), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF2E7D32))
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isGstActive) "Subtotal (Taxable Value)" else "Subtotal Amount", style = MaterialTheme.typography.bodyMedium)
                        }
                        Text("$curr" + "%.2f".format(subtotal), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                    }

                    // ONLY SHOW TAX ROWS IF TAX IS ACTIVE
                    if (isGstActive) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Percent, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("CGST (${curTicket.gstRate / 2}%)", style = MaterialTheme.typography.bodyMedium)
                            }
                            Text("$curr" + "%.2f".format(cgst), style = MaterialTheme.typography.bodyMedium)
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Percent, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("SGST (${curTicket.gstRate / 2}%)", style = MaterialTheme.typography.bodyMedium)
                            }
                            Text("$curr" + "%.2f".format(sgst), style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Grand Total Banner
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PriceCheck, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isGstActive) "Grand Total Invoice Amount" else "Total Amount Payable",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Text(
                                text = "$curr" + "%.2f".format(grandTotal),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // SECTION 5: TERMS & AUTHORIZED STAMP
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(modifier = Modifier.weight(1.3f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Terms & Warranty:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.outline)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = profile.termsAndConditions.ifBlank { "1. 30 days warranty on replaced hardware\n2. Free storage for 15 days upon completion" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Seal & Signature Column
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(0.7f)) {
                            Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(if (isTaxEnabled) "Official Tax Invoice" else "Official Bill / Receipt", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                            Text(text = profile.companyName.take(18), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                        }
                    }
                }
            }

            // Export & Share Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        com.example.util.PdfExporter.generateAndOpenPdf(context, curTicket, profile, isInvoice = true)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export Invoice PDF", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }

                OutlinedButton(
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_VIEW).apply {
                            val phone = curTicket.customerPhone.replace(Regex("[^0-9+]"), "")
                            data = Uri.parse("https://api.whatsapp.com/send?phone=$phone&text=${Uri.encode(shareInvoiceText)}")
                        }
                        try {
                            context.startActivity(sendIntent)
                        } catch (e: Exception) {
                            val generalShare = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareInvoiceText)
                            }
                            context.startActivity(Intent.createChooser(generalShare, "Share Invoice"))
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF25D366), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share WhatsApp", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }

    // Manual Discount Data Entry Dialog
    if (showDiscountDialog) {
        val editList = remember(items) {
            mutableStateListOf<QuotationItem>().apply { addAll(items) }
        }
        var isGstOn by remember { mutableStateOf(curTicket.gstRate > 0.0) }
        var gstRateInput by remember { mutableStateOf(if (curTicket.gstRate > 0.0) curTicket.gstRate.toString() else "18.0") }

        AlertDialog(
            onDismissRequest = { showDiscountDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocalOffer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Manual Discount Entry")
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Enter or adjust discount data for each invoice line item below:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    editList.forEachIndexed { idx, item ->
                        var isPct by remember(item.id) { mutableStateOf(item.discountPercent > 0) }
                        var discStr by remember(item.id) {
                            mutableStateOf(
                                if (item.discountPercent > 0) item.discountPercent.toString()
                                else if (item.discount > 0) item.discount.toString()
                                else "0"
                            )
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                        Text(
                                            "Qty: ${item.quantity} x $curr%.2f = $curr%.2f".format(item.unitPrice, item.grossTotal),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        FilterChip(
                                            selected = !isPct,
                                            onClick = {
                                                isPct = false
                                                val v = discStr.toDoubleOrNull() ?: 0.0
                                                editList[idx] = item.copy(discount = v, discountPercent = 0.0)
                                            },
                                            label = { Text("$curr", style = MaterialTheme.typography.labelSmall) }
                                        )
                                        FilterChip(
                                            selected = isPct,
                                            onClick = {
                                                isPct = true
                                                val v = discStr.toDoubleOrNull() ?: 0.0
                                                editList[idx] = item.copy(discount = 0.0, discountPercent = v)
                                            },
                                            label = { Text("%", style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = discStr,
                                    onValueChange = { newVal ->
                                        discStr = newVal
                                        val v = newVal.toDoubleOrNull() ?: 0.0
                                        editList[idx] = if (isPct) {
                                            item.copy(discount = 0.0, discountPercent = v)
                                        } else {
                                            item.copy(discount = v, discountPercent = 0.0)
                                        }
                                    },
                                    label = { Text(if (isPct) "Discount %" else "Discount Amount ($curr)") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    listOf("0%", "5%", "10%", "15%", "20%").forEach { p ->
                                        AssistChip(
                                            onClick = {
                                                isPct = true
                                                val num = p.replace("%", "")
                                                discStr = num
                                                val v = num.toDoubleOrNull() ?: 0.0
                                                editList[idx] = item.copy(discount = 0.0, discountPercent = v)
                                            },
                                            label = { Text(p, style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }

                                val currentEffectiveDisc = editList[idx].effectiveDiscount
                                val currentNet = editList[idx].total
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Disc: -$curr%.2f".format(currentEffectiveDisc), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF2E7D32))
                                    Text("Line Amount: $curr%.2f".format(currentNet), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }

                    if (isTaxEnabled) {
                        Spacer(modifier = Modifier.height(4.dp))
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
                                    Text("Include GST in Invoice", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    Text(if (isGstOn) "GST ON (${gstRateInput}%)" else "GST OFF (0%)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                            Switch(
                                checked = isGstOn,
                                onCheckedChange = { isGstOn = it },
                                modifier = Modifier.testTag("invoice_preview_gst_switch")
                            )
                        }

                        if (isGstOn) {
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = gstRateInput,
                                onValueChange = { gstRateInput = it },
                                label = { Text("GST Rate (%)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    val dialogGross = editList.sumOf { it.grossTotal }
                    val dialogDiscount = editList.sumOf { it.effectiveDiscount }
                    val dialogSubtotal = editList.sumOf { it.total }
                    val activeRate = gstRateInput.toDoubleOrNull() ?: 18.0
                    val dialogGst = if (isTaxEnabled && isGstOn) dialogSubtotal * (activeRate / 100.0) else 0.0
                    val dialogCgst = dialogGst / 2.0
                    val dialogSgst = dialogGst / 2.0
                    val dialogGrand = dialogSubtotal + dialogGst

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Gross Subtotal:", style = MaterialTheme.typography.bodySmall)
                                Text("$curr%.2f".format(dialogGross), style = MaterialTheme.typography.bodySmall)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Discount:", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF2E7D32))
                                Text("-$curr%.2f".format(dialogDiscount), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF2E7D32))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Taxable Subtotal:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                Text("$curr%.2f".format(dialogSubtotal), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            }
                            if (isTaxEnabled && isGstOn) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("CGST (${activeRate / 2}%):", style = MaterialTheme.typography.bodySmall)
                                    Text("$curr%.2f".format(dialogCgst), style = MaterialTheme.typography.bodySmall)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("SGST (${activeRate / 2}%):", style = MaterialTheme.typography.bodySmall)
                                    Text("$curr%.2f".format(dialogSgst), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("New Total Payable:", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                Text("$curr%.2f".format(dialogGrand), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalRate = if (isTaxEnabled && isGstOn) (gstRateInput.toDoubleOrNull() ?: 18.0) else 0.0
                        viewModel.updateTicketItemsAndDiscounts(curTicket.id, editList.toList(), finalRate)
                        showDiscountDialog = false
                    }
                ) {
                    Text("Apply Discounts")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscountDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
