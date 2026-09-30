package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.CompanyProfileEntity
import com.example.data.QuotationItem
import com.example.data.TicketEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    private const val PAGE_WIDTH = 595 // Standard A4 width in points (72 dpi)
    private const val PAGE_HEIGHT = 842 // Standard A4 height in points (72 dpi)

    fun generateAndOpenPdf(
        context: Context,
        ticket: TicketEntity,
        companyProfile: CompanyProfileEntity,
        isInvoice: Boolean
    ) {
        try {
            val pdfFile = createDocumentPdf(context, ticket, companyProfile, isInvoice)
            openPdfFile(context, pdfFile, if (isInvoice) "Invoice #${ticket.ticketNumber}" else "Quotation #${ticket.ticketNumber}")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error exporting PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun createDocumentPdf(
        context: Context,
        ticket: TicketEntity,
        companyProfile: CompanyProfileEntity,
        isInvoice: Boolean
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        // Paints & Styles
        val primaryPaint = Paint().apply {
            color = Color.parseColor("#0F172A") // Dark Navy
            style = Paint.Style.FILL
        }

        val accentPaint = Paint().apply {
            color = Color.parseColor("#0284C7") // Primary Blue
            style = Paint.Style.FILL
        }

        val textPaint = Paint().apply {
            color = Color.parseColor("#1E293B")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val boldPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val headerTitlePaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subTextPaint = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 9f
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }

        val curr = companyProfile.currencySymbol.ifEmpty { "$" }
        val isTaxEnabled = companyProfile.taxEnabled
        val isGstActive = isTaxEnabled && ticket.gstRate > 0.0
        val dateFormatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val dateStr = dateFormatter.format(Date(if (ticket.updatedAt > 0) ticket.updatedAt else ticket.createdAt))

        var y = 30f

        // 1. TOP BRANDING BAR
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 12f, accentPaint)

        y += 20f

        // Company Name & Subtitle
        canvas.drawText(companyProfile.companyName.ifEmpty { "NEO SERVICE CENTER" }, 30f, y, headerTitlePaint)
        y += 14f
        if (companyProfile.tagline.isNotBlank()) {
            canvas.drawText(companyProfile.tagline, 30f, y, subTextPaint)
            y += 14f
        }

        val companyDetailsY = y
        canvas.drawText(companyProfile.address.ifEmpty { "Service Operations Hub" }, 30f, y, subTextPaint)
        y += 12f
        canvas.drawText("Phone: ${companyProfile.phone} | Email: ${companyProfile.email}", 30f, y, subTextPaint)
        y += 12f
        if (isGstActive && companyProfile.gstin.isNotBlank()) {
            canvas.drawText("GSTIN: ${companyProfile.gstin}", 30f, y, boldPaint)
            y += 12f
        }

        // 2. DOCUMENT TYPE BADGE (Top Right)
        val docTitle = if (isInvoice) "TAX INVOICE" else "REPAIR QUOTATION"
        val badgePaint = Paint().apply {
            color = if (isInvoice) Color.parseColor("#DCFCE7") else Color.parseColor("#E0F2FE")
            style = Paint.Style.FILL
        }
        val badgeTextPaint = Paint().apply {
            color = if (isInvoice) Color.parseColor("#15803D") else Color.parseColor("#0369A1")
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        canvas.drawRoundRect(PAGE_WIDTH - 180f, 25f, PAGE_WIDTH - 30f, 60f, 8f, 8f, badgePaint)
        canvas.drawText(docTitle, PAGE_WIDTH - 165f, 48f, badgeTextPaint)

        canvas.drawText("Doc #: ${ticket.ticketNumber}", PAGE_WIDTH - 170f, 75f, boldPaint)
        canvas.drawText("Date: $dateStr", PAGE_WIDTH - 170f, 88f, subTextPaint)

        y = Math.max(y, 110f)

        // Divider
        canvas.drawLine(30f, y, PAGE_WIDTH - 30f, y, linePaint)
        y += 16f

        // 3. CUSTOMER & DEVICE INFO CARDS
        val col2X = PAGE_WIDTH / 2f + 10f

        // Column 1: Customer Details
        canvas.drawText("CUSTOMER DETAILS", 30f, y, boldPaint)
        canvas.drawText("JOB / DEVICE METRICS", col2X, y, boldPaint)
        y += 14f

        canvas.drawText("Name: ${ticket.customerName}", 30f, y, textPaint)
        canvas.drawText("Device: ${ticket.deviceName}", col2X, y, textPaint)
        y += 12f

        canvas.drawText("Phone: ${ticket.customerPhone}", 30f, y, textPaint)
        canvas.drawText("Service Type: ${ticket.serviceType}", col2X, y, textPaint)
        y += 12f

        canvas.drawText("Email: ${ticket.customerEmail.ifEmpty { "N/A" }}", 30f, y, textPaint)
        canvas.drawText("Status: ${ticket.status.replace("_", " ")}", col2X, y, boldPaint)
        y += 12f

        canvas.drawText("Address: ${ticket.locationAddress.take(45)}", 30f, y, subTextPaint)
        canvas.drawText("Assigned Tech: ${ticket.assignedTechnicianName.ifEmpty { "Workshop Tech" }}", col2X, y, textPaint)
        y += 18f

        // Issue Description Banner
        if (ticket.issueDescription.isNotBlank()) {
            val bgPaint = Paint().apply {
                color = Color.parseColor("#F8FAFC")
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(30f, y, PAGE_WIDTH - 30f, y + 24f, 6f, 6f, bgPaint)
            canvas.drawText("Reported Issue: ${ticket.issueDescription.take(75)}", 40f, y + 16f, subTextPaint)
            y += 32f
        }

        // 4. ITEMIZED ITEMS TABLE
        val tableHeaderBg = Paint().apply {
            color = Color.parseColor("#F1F5F9")
            style = Paint.Style.FILL
        }

        canvas.drawRoundRect(30f, y, PAGE_WIDTH - 30f, y + 22f, 4f, 4f, tableHeaderBg)

        val tableY = y + 15f
        canvas.drawText("#", 40f, tableY, boldPaint)
        canvas.drawText("Item Description", 65f, tableY, boldPaint)
        canvas.drawText("Cat", 290f, tableY, boldPaint)
        canvas.drawText("Qty", 350f, tableY, boldPaint)
        canvas.drawText("Unit Price", 400f, tableY, boldPaint)
        canvas.drawText("Total ($curr)", 490f, tableY, boldPaint)

        y += 28f

        val parsedItems = QuotationItem.parseList(ticket.quotationItemsJson)
        val itemsList = if (parsedItems.isNotEmpty()) parsedItems else {
            val list = mutableListOf<QuotationItem>()
            if (ticket.partsCost > 0) list.add(QuotationItem(name = ticket.partsDescription.ifEmpty { "Replacement Spare Parts" }, category = "Part", quantity = 1, unitPrice = ticket.partsCost))
            if (ticket.laborCost > 0) list.add(QuotationItem(name = ticket.laborDescription.ifEmpty { "Diagnostic & Repair Labor" }, category = "Labor", quantity = 1, unitPrice = ticket.laborCost))
            if (list.isEmpty()) list.add(QuotationItem(name = "Standard Technical Service", category = "Labor", quantity = 1, unitPrice = 0.0))
            list
        }

        var index = 1
        for (item in itemsList) {
            canvas.drawText("$index", 40f, y, subTextPaint)
            canvas.drawText(item.name.take(35), 65f, y, textPaint)
            canvas.drawText(item.category, 290f, y, subTextPaint)
            canvas.drawText("${item.quantity}", 355f, y, textPaint)
            canvas.drawText(String.format(Locale.getDefault(), "%.2f", item.unitPrice), 400f, y, textPaint)
            canvas.drawText(String.format(Locale.getDefault(), "%.2f", item.total), 490f, y, boldPaint)

            y += 18f
            canvas.drawLine(30f, y - 6f, PAGE_WIDTH - 30f, y - 6f, linePaint)
            index++
        }

        y += 10f

        // 5. CALCULATIONS SUMMARY (Right Aligned)
        val subtotal = itemsList.sumOf { it.total }
        val discount = itemsList.sumOf { it.effectiveDiscount }
        val gstAmount = if (isGstActive) subtotal * (ticket.gstRate / 100.0) else 0.0
        val grandTotal = subtotal + gstAmount

        val summaryX = 350f

        canvas.drawText("Subtotal:", summaryX, y, textPaint)
        canvas.drawText("$curr${String.format(Locale.getDefault(), "%.2f", subtotal)}", 490f, y, textPaint)
        y += 14f

        if (discount > 0) {
            val greenPaint = Paint(textPaint).apply { color = Color.parseColor("#16A34A") }
            canvas.drawText("Discount Savings:", summaryX, y, greenPaint)
            canvas.drawText("-$curr${String.format(Locale.getDefault(), "%.2f", discount)}", 490f, y, greenPaint)
            y += 14f
        }

        if (isGstActive) {
            canvas.drawText("GST (${ticket.gstRate}%):", summaryX, y, textPaint)
            canvas.drawText("$curr${String.format(Locale.getDefault(), "%.2f", gstAmount)}", 490f, y, textPaint)
            y += 14f
        }

        // Total Box
        val grandTotalBg = Paint().apply {
            color = Color.parseColor("#0F172A")
            style = Paint.Style.FILL
        }
        val grandTotalText = Paint().apply {
            color = Color.WHITE
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        canvas.drawRoundRect(summaryX - 10f, y, PAGE_WIDTH - 30f, y + 26f, 6f, 6f, grandTotalBg)
        canvas.drawText("GRAND TOTAL:", summaryX, y + 17f, grandTotalText)
        canvas.drawText("$curr${String.format(Locale.getDefault(), "%.2f", grandTotal)}", 480f, y + 17f, grandTotalText)

        y += 40f

        // Payment Settlement Status Stamp
        if (isInvoice) {
            val paymentStampBg = Paint().apply {
                color = if (ticket.paymentStatus == "RECEIVED") Color.parseColor("#DCFCE7") else Color.parseColor("#FEF3C7")
                style = Paint.Style.FILL
            }
            val paymentStampText = Paint().apply {
                color = if (ticket.paymentStatus == "RECEIVED") Color.parseColor("#15803D") else Color.parseColor("#B45309")
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val payText = if (ticket.paymentStatus == "RECEIVED") "PAYMENT STATUS: PAID (${ticket.paymentMode})" else "PAYMENT STATUS: PENDING"
            canvas.drawRoundRect(30f, y, 260f, y + 22f, 6f, 6f, paymentStampBg)
            canvas.drawText(payText, 38f, y + 15f, paymentStampText)
            y += 32f
        }

        // 6. WARRANTY & TERMS FOOTER
        canvas.drawLine(30f, y, PAGE_WIDTH - 30f, y, linePaint)
        y += 16f

        canvas.drawText("TERMS & CONDITIONS / WARRANTY NOTICE:", 30f, y, boldPaint)
        y += 14f
        canvas.drawText("• 30-Day warranty applicable on replaced hardware parts and technical service.", 30f, y, subTextPaint)
        y += 12f
        canvas.drawText("• Physical, liquid or customer unauthorized tampering invalidates repair warranty.", 30f, y, subTextPaint)
        y += 12f
        canvas.drawText("• Computer generated electronic document. Valid without physical stamp.", 30f, y, subTextPaint)

        // Authorized Signatory Box (Bottom Right)
        val stampY = y - 10f
        canvas.drawText("For ${companyProfile.companyName.ifEmpty { "Service Center" }}", PAGE_WIDTH - 180f, stampY, boldPaint)
        canvas.drawLine(PAGE_WIDTH - 180f, stampY + 30f, PAGE_WIDTH - 30f, stampY + 30f, linePaint)
        canvas.drawText("Authorized Signatory", PAGE_WIDTH - 160f, stampY + 42f, subTextPaint)

        document.finishPage(page)

        // Save PDF file to cache directory
        val pdfDir = File(context.cacheDir, "exported_pdfs")
        if (!pdfDir.exists()) pdfDir.mkdirs()

        val fileName = if (isInvoice) "Invoice_${ticket.ticketNumber}.pdf" else "Quotation_${ticket.ticketNumber}.pdf"
        val pdfFile = File(pdfDir, fileName)

        val outputStream = FileOutputStream(pdfFile)
        document.writeTo(outputStream)
        outputStream.close()
        document.close()

        return pdfFile
    }

    private fun openPdfFile(context: Context, pdfFile: File, title: String) {
        val authority = "${context.packageName}.fileprovider"
        val contentUri: Uri = FileProvider.getUriForFile(context, authority, pdfFile)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "Export & Share $title").apply {
            putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(intent))
        }

        context.startActivity(chooser)
    }

    // ==================== DAILY REPORT PDF EXPORT ====================
    fun generateAndOpenDailyReportPdf(
        context: Context,
        companyProfile: CompanyProfileEntity,
        dateDisplayStr: String,
        createdTickets: List<TicketEntity>,
        completedTickets: List<TicketEntity>,
        inProgressTickets: List<TicketEntity>,
        totalRevenue: Double,
        partsRevenue: Double,
        laborRevenue: Double,
        gstTax: Double
    ) {
        try {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = document.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val curr = companyProfile.currencySymbol.ifEmpty { "$" }

            val accentPaint = Paint().apply {
                color = Color.parseColor("#0284C7")
                style = Paint.Style.FILL
            }
            val textPaint = Paint().apply {
                color = Color.parseColor("#1E293B")
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                isAntiAlias = true
            }
            val boldPaint = Paint().apply {
                color = Color.parseColor("#0F172A")
                textSize = 10.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val headerTitlePaint = Paint().apply {
                color = Color.parseColor("#0F172A")
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val subTextPaint = Paint().apply {
                color = Color.parseColor("#64748B")
                textSize = 8.5f
                isAntiAlias = true
            }
            val linePaint = Paint().apply {
                color = Color.parseColor("#E2E8F0")
                strokeWidth = 1f
                style = Paint.Style.STROKE
            }

            var y = 25f

            // Top bar
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 10f, accentPaint)
            y += 15f

            // Company Header
            canvas.drawText(companyProfile.companyName.ifEmpty { "NEO SERVICE CENTER" }, 30f, y, headerTitlePaint)
            y += 14f
            if (companyProfile.tagline.isNotBlank()) {
                canvas.drawText(companyProfile.tagline, 30f, y, subTextPaint)
                y += 12f
            }
            canvas.drawText("Phone: ${companyProfile.phone} | Email: ${companyProfile.email}", 30f, y, subTextPaint)
            y += 12f
            if (companyProfile.taxEnabled && companyProfile.gstin.isNotBlank()) {
                canvas.drawText("GSTIN: ${companyProfile.gstin}", 30f, y, subTextPaint)
                y += 12f
            }

            // Document Badge (Top Right)
            val badgePaint = Paint().apply {
                color = Color.parseColor("#E0F2FE")
                style = Paint.Style.FILL
            }
            val badgeTextPaint = Paint().apply {
                color = Color.parseColor("#0369A1")
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            canvas.drawRoundRect(PAGE_WIDTH - 210f, 20f, PAGE_WIDTH - 30f, 52f, 6f, 6f, badgePaint)
            canvas.drawText("DAILY SUMMARY REPORT", PAGE_WIDTH - 200f, 41f, badgeTextPaint)
            canvas.drawText("Date: $dateDisplayStr", PAGE_WIDTH - 205f, 66f, boldPaint)

            y = Math.max(y, 85f)
            canvas.drawLine(30f, y, PAGE_WIDTH - 30f, y, linePaint)
            y += 14f

            // 1. KEY METRICS SUMMARY ROW
            val cardBgPaint = Paint().apply {
                color = Color.parseColor("#F8FAFC")
                style = Paint.Style.FILL
            }

            val cardW = (PAGE_WIDTH - 80f) / 3f

            // Card 1: Total Revenue
            canvas.drawRoundRect(30f, y, 30f + cardW, y + 42f, 6f, 6f, cardBgPaint)
            canvas.drawText("Total Revenue Today", 38f, y + 14f, subTextPaint)
            val greenBold = Paint(boldPaint).apply { color = Color.parseColor("#16A34A"); textSize = 13f }
            canvas.drawText("$curr${String.format(Locale.getDefault(), "%.2f", totalRevenue)}", 38f, y + 32f, greenBold)

            // Card 2: Jobs Completed
            val c2X = 30f + cardW + 10f
            canvas.drawRoundRect(c2X, y, c2X + cardW, y + 42f, 6f, 6f, cardBgPaint)
            canvas.drawText("Jobs Completed", c2X + 8f, y + 14f, subTextPaint)
            canvas.drawText("${completedTickets.size} Jobs", c2X + 8f, y + 32f, boldPaint)

            // Card 3: New Jobs Received
            val c3X = c2X + cardW + 10f
            canvas.drawRoundRect(c3X, y, c3X + cardW, y + 42f, 6f, 6f, cardBgPaint)
            canvas.drawText("New Jobs Received", c3X + 8f, y + 14f, subTextPaint)
            canvas.drawText("${createdTickets.size} Jobs", c3X + 8f, y + 32f, boldPaint)

            y += 54f

            // 2. FINANCIAL REVENUE BREAKDOWN
            canvas.drawText("DAILY REVENUE BREAKDOWN", 30f, y, boldPaint)
            y += 12f

            canvas.drawText("Labor Service Revenue:", 30f, y, textPaint)
            canvas.drawText("$curr${String.format(Locale.getDefault(), "%.2f", laborRevenue)}", 220f, y, textPaint)

            canvas.drawText("Hardware Parts Revenue:", 300f, y, textPaint)
            canvas.drawText("$curr${String.format(Locale.getDefault(), "%.2f", partsRevenue)}", 480f, y, textPaint)
            y += 14f

            canvas.drawText("GST / Tax Collected:", 30f, y, textPaint)
            canvas.drawText("$curr${String.format(Locale.getDefault(), "%.2f", gstTax)}", 220f, y, textPaint)

            canvas.drawText("Net Total Collected:", 300f, y, boldPaint)
            canvas.drawText("$curr${String.format(Locale.getDefault(), "%.2f", totalRevenue)}", 480f, y, greenBold)

            y += 20f
            canvas.drawLine(30f, y, PAGE_WIDTH - 30f, y, linePaint)
            y += 14f

            // 3. TODAY'S JOBS AUDIT TABLE
            canvas.drawText("DAILY COMPLETED & ACTIVE JOBS LOG", 30f, y, boldPaint)
            y += 12f

            val tableHeaderBg = Paint().apply {
                color = Color.parseColor("#F1F5F9")
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(30f, y, PAGE_WIDTH - 30f, y + 20f, 4f, 4f, tableHeaderBg)

            val tY = y + 14f
            canvas.drawText("#", 38f, tY, boldPaint)
            canvas.drawText("Ticket #", 60f, tY, boldPaint)
            canvas.drawText("Customer", 150f, tY, boldPaint)
            canvas.drawText("Device", 280f, tY, boldPaint)
            canvas.drawText("Status", 410f, tY, boldPaint)
            canvas.drawText("Total ($curr)", 490f, tY, boldPaint)

            y += 24f

            val allLoggedToday = (completedTickets + createdTickets + inProgressTickets).distinctBy { it.id }

            if (allLoggedToday.isEmpty()) {
                canvas.drawText("No tickets recorded for $dateDisplayStr", 38f, y, subTextPaint)
                y += 20f
            } else {
                var idx = 1
                for (t in allLoggedToday.take(22)) { // max rows for 1 page clean fit
                    val rowSub = if (t.partsCost + t.laborCost > 0) t.partsCost + t.laborCost else 0.0
                    val rowGst = if (companyProfile.taxEnabled && t.gstRate > 0) rowSub * (t.gstRate / 100.0) else 0.0
                    val rowTotal = rowSub + rowGst

                    canvas.drawText("$idx", 38f, y, subTextPaint)
                    canvas.drawText(t.ticketNumber, 60f, y, boldPaint)
                    canvas.drawText(t.customerName.take(18), 150f, y, textPaint)
                    canvas.drawText(t.deviceName.take(18), 280f, y, textPaint)
                    canvas.drawText(t.status.replace("_", " "), 410f, y, subTextPaint)
                    canvas.drawText(String.format(Locale.getDefault(), "%.2f", rowTotal), 490f, y, boldPaint)

                    y += 16f
                    canvas.drawLine(30f, y - 4f, PAGE_WIDTH - 30f, y - 4f, linePaint)
                    idx++
                }
            }

            y += 20f

            // Footer
            val genDateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
            canvas.drawText("Report Generated: $genDateStr | Confidential Operations Record", 30f, PAGE_HEIGHT - 30f, subTextPaint)
            canvas.drawText("For ${companyProfile.companyName.ifEmpty { "Service Operations" }}", PAGE_WIDTH - 180f, PAGE_HEIGHT - 30f, boldPaint)

            document.finishPage(page)

            val pdfDir = File(context.cacheDir, "exported_pdfs")
            if (!pdfDir.exists()) pdfDir.mkdirs()

            val pdfFile = File(pdfDir, "Daily_Report_${dateDisplayStr.replace(" ", "_").replace(",", "")}.pdf")
            val outputStream = FileOutputStream(pdfFile)
            document.writeTo(outputStream)
            outputStream.close()
            document.close()

            openPdfFile(context, pdfFile, "Daily Report - $dateDisplayStr")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error exporting Daily PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    // ==================== MONTHLY REPORT PDF EXPORT ====================
    fun generateAndOpenMonthlyReportPdf(
        context: Context,
        companyProfile: CompanyProfileEntity,
        monthDisplayStr: String,
        monthlyTickets: List<TicketEntity>,
        totalRevenue: Double,
        partsRevenue: Double,
        laborRevenue: Double,
        gstTax: Double,
        completedCount: Int,
        inProgressCount: Int
    ) {
        try {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = document.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val curr = companyProfile.currencySymbol.ifEmpty { "$" }

            val accentPaint = Paint().apply {
                color = Color.parseColor("#0F766E") // Teal Accent
                style = Paint.Style.FILL
            }
            val textPaint = Paint().apply {
                color = Color.parseColor("#1E293B")
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                isAntiAlias = true
            }
            val boldPaint = Paint().apply {
                color = Color.parseColor("#0F172A")
                textSize = 10.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val headerTitlePaint = Paint().apply {
                color = Color.parseColor("#0F172A")
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val subTextPaint = Paint().apply {
                color = Color.parseColor("#64748B")
                textSize = 8.5f
                isAntiAlias = true
            }
            val linePaint = Paint().apply {
                color = Color.parseColor("#E2E8F0")
                strokeWidth = 1f
                style = Paint.Style.STROKE
            }

            var y = 25f

            // Top bar
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 10f, accentPaint)
            y += 15f

            // Company Header
            canvas.drawText(companyProfile.companyName.ifEmpty { "NEO SERVICE CENTER" }, 30f, y, headerTitlePaint)
            y += 14f
            if (companyProfile.tagline.isNotBlank()) {
                canvas.drawText(companyProfile.tagline, 30f, y, subTextPaint)
                y += 12f
            }
            canvas.drawText("Phone: ${companyProfile.phone} | Email: ${companyProfile.email}", 30f, y, subTextPaint)
            y += 12f
            if (companyProfile.taxEnabled && companyProfile.gstin.isNotBlank()) {
                canvas.drawText("GSTIN: ${companyProfile.gstin}", 30f, y, subTextPaint)
                y += 12f
            }

            // Document Badge (Top Right)
            val badgePaint = Paint().apply {
                color = Color.parseColor("#CCFBF1")
                style = Paint.Style.FILL
            }
            val badgeTextPaint = Paint().apply {
                color = Color.parseColor("#0F766E")
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            canvas.drawRoundRect(PAGE_WIDTH - 220f, 20f, PAGE_WIDTH - 30f, 52f, 6f, 6f, badgePaint)
            canvas.drawText("MONTHLY SUMMARY REPORT", PAGE_WIDTH - 212f, 41f, badgeTextPaint)
            canvas.drawText("Period: $monthDisplayStr", PAGE_WIDTH - 205f, 66f, boldPaint)

            y = Math.max(y, 85f)
            canvas.drawLine(30f, y, PAGE_WIDTH - 30f, y, linePaint)
            y += 14f

            // 1. KEY METRICS SUMMARY ROW
            val cardBgPaint = Paint().apply {
                color = Color.parseColor("#F8FAFC")
                style = Paint.Style.FILL
            }

            val cardW = (PAGE_WIDTH - 80f) / 3f

            // Card 1: Total Monthly Revenue
            canvas.drawRoundRect(30f, y, 30f + cardW, y + 42f, 6f, 6f, cardBgPaint)
            canvas.drawText("Total Revenue ($monthDisplayStr)", 38f, y + 14f, subTextPaint)
            val greenBold = Paint(boldPaint).apply { color = Color.parseColor("#0F766E"); textSize = 13f }
            canvas.drawText("$curr${String.format(Locale.getDefault(), "%.2f", totalRevenue)}", 38f, y + 32f, greenBold)

            // Card 2: Total Jobs Processed
            val c2X = 30f + cardW + 10f
            canvas.drawRoundRect(c2X, y, c2X + cardW, y + 42f, 6f, 6f, cardBgPaint)
            canvas.drawText("Total Jobs Processed", c2X + 8f, y + 14f, subTextPaint)
            canvas.drawText("${monthlyTickets.size} Jobs", c2X + 8f, y + 32f, boldPaint)

            // Card 3: Completed Repairs
            val c3X = c2X + cardW + 10f
            canvas.drawRoundRect(c3X, y, c3X + cardW, y + 42f, 6f, 6f, cardBgPaint)
            canvas.drawText("Completed Repairs", c3X + 8f, y + 14f, subTextPaint)
            canvas.drawText("$completedCount Completed", c3X + 8f, y + 32f, boldPaint)

            y += 54f

            // 2. FINANCIAL REVENUE BREAKDOWN
            canvas.drawText("MONTHLY FINANCIAL BREAKDOWN", 30f, y, boldPaint)
            y += 12f

            canvas.drawText("Labor Service Revenue:", 30f, y, textPaint)
            canvas.drawText("$curr${String.format(Locale.getDefault(), "%.2f", laborRevenue)}", 220f, y, textPaint)

            canvas.drawText("Hardware Parts Revenue:", 300f, y, textPaint)
            canvas.drawText("$curr${String.format(Locale.getDefault(), "%.2f", partsRevenue)}", 480f, y, textPaint)
            y += 14f

            canvas.drawText("GST / Tax Portion:", 30f, y, textPaint)
            canvas.drawText("$curr${String.format(Locale.getDefault(), "%.2f", gstTax)}", 220f, y, textPaint)

            canvas.drawText("Grand Monthly Total:", 300f, y, boldPaint)
            canvas.drawText("$curr${String.format(Locale.getDefault(), "%.2f", totalRevenue)}", 480f, y, greenBold)

            y += 20f
            canvas.drawLine(30f, y, PAGE_WIDTH - 30f, y, linePaint)
            y += 14f

            // 3. MONTHLY JOBS LOG TABLE
            canvas.drawText("MONTHLY REPAIR JOBS LOG ($monthDisplayStr)", 30f, y, boldPaint)
            y += 12f

            val tableHeaderBg = Paint().apply {
                color = Color.parseColor("#F1F5F9")
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(30f, y, PAGE_WIDTH - 30f, y + 20f, 4f, 4f, tableHeaderBg)

            val tY = y + 14f
            canvas.drawText("#", 38f, tY, boldPaint)
            canvas.drawText("Ticket #", 60f, tY, boldPaint)
            canvas.drawText("Customer", 150f, tY, boldPaint)
            canvas.drawText("Device", 280f, tY, boldPaint)
            canvas.drawText("Tech", 400f, tY, boldPaint)
            canvas.drawText("Total ($curr)", 490f, tY, boldPaint)

            y += 24f

            if (monthlyTickets.isEmpty()) {
                canvas.drawText("No tickets recorded for $monthDisplayStr", 38f, y, subTextPaint)
                y += 20f
            } else {
                var idx = 1
                for (t in monthlyTickets.take(22)) {
                    val rowSub = if (t.partsCost + t.laborCost > 0) t.partsCost + t.laborCost else 0.0
                    val rowGst = if (companyProfile.taxEnabled && t.gstRate > 0) rowSub * (t.gstRate / 100.0) else 0.0
                    val rowTotal = rowSub + rowGst

                    canvas.drawText("$idx", 38f, y, subTextPaint)
                    canvas.drawText(t.ticketNumber, 60f, y, boldPaint)
                    canvas.drawText(t.customerName.take(18), 150f, y, textPaint)
                    canvas.drawText(t.deviceName.take(18), 280f, y, textPaint)
                    canvas.drawText(t.assignedTechnicianName.ifEmpty { "Tech" }.take(14), 400f, y, subTextPaint)
                    canvas.drawText(String.format(Locale.getDefault(), "%.2f", rowTotal), 490f, y, boldPaint)

                    y += 16f
                    canvas.drawLine(30f, y - 4f, PAGE_WIDTH - 30f, y - 4f, linePaint)
                    idx++
                }
            }

            y += 20f

            // Footer
            val genDateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
            canvas.drawText("Report Generated: $genDateStr | Confidential Operations Record", 30f, PAGE_HEIGHT - 30f, subTextPaint)
            canvas.drawText("For ${companyProfile.companyName.ifEmpty { "Service Operations" }}", PAGE_WIDTH - 180f, PAGE_HEIGHT - 30f, boldPaint)

            document.finishPage(page)

            val pdfDir = File(context.cacheDir, "exported_pdfs")
            if (!pdfDir.exists()) pdfDir.mkdirs()

            val pdfFile = File(pdfDir, "Monthly_Report_${monthDisplayStr.replace(" ", "_")}.pdf")
            val outputStream = FileOutputStream(pdfFile)
            document.writeTo(outputStream)
            outputStream.close()
            document.close()

            openPdfFile(context, pdfFile, "Monthly Report - $monthDisplayStr")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error exporting Monthly PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
