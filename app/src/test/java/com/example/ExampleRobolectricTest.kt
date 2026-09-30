package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("NEO Service", appName)
  }

  @Test
  fun `test database and dao`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.example.data.AppDatabase.getDatabase(context)
    val dao = db.serviceDao()
    org.junit.Assert.assertNotNull(dao)
  }

  @Test
  fun `test drawable exists`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val drawable = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.img_neo_logo_1790575625683)
    org.junit.Assert.assertNotNull(drawable)
  }

  @Test
  fun `test ticket timeline entry history serialization and parsing`() {
    val entries = listOf(
      com.example.data.TicketTimelineEntry(
        status = "PENDING",
        updatedByName = "Aarav Sharma (Admin)",
        updatedByRole = "ADMIN",
        remarks = "Ticket registered",
        timestamp = 1000L
      ),
      com.example.data.TicketTimelineEntry(
        status = "DIAGNOSTICS",
        updatedByName = "Rajesh Kumar (Senior Tech)",
        updatedByRole = "TECHNICIAN",
        remarks = "Diagnostics completed",
        timestamp = 2000L
      )
    )
    val json = com.example.data.TicketTimelineEntry.toJsonString(entries)
    val parsed = com.example.data.TicketTimelineEntry.parseList(json)
    assertEquals(2, parsed.size)
    assertEquals("PENDING", parsed[0].status)
    assertEquals("Aarav Sharma (Admin)", parsed[0].updatedByName)
    assertEquals("ADMIN", parsed[0].updatedByRole)
    assertEquals("DIAGNOSTICS", parsed[1].status)
    assertEquals("Rajesh Kumar (Senior Tech)", parsed[1].updatedByName)
    assertEquals("TECHNICIAN", parsed[1].updatedByRole)
  }

  @Test
  fun `test quotation item manual discount calculation and serialization`() {
    // 1. Flat discount manual entry test
    val itemFlat = com.example.data.QuotationItem(
      name = "Replacement Display Panel",
      category = "Part",
      quantity = 2,
      unitPrice = 2500.0,
      discount = 500.0 // Flat discount of 500
    )
    assertEquals(5000.0, itemFlat.grossTotal, 0.001)
    assertEquals(500.0, itemFlat.effectiveDiscount, 0.001)
    assertEquals(4500.0, itemFlat.total, 0.001)

    // 2. Percentage discount manual entry test (10%)
    val itemPct = com.example.data.QuotationItem(
      name = "Service & Labor",
      category = "Labor",
      quantity = 1,
      unitPrice = 1200.0,
      discountPercent = 10.0 // 10% discount = 120
    )
    assertEquals(1200.0, itemPct.grossTotal, 0.001)
    assertEquals(120.0, itemPct.effectiveDiscount, 0.001)
    assertEquals(1080.0, itemPct.total, 0.001)

    // 3. Serialization and deserialization test
    val list = listOf(itemFlat, itemPct)
    val json = com.example.data.QuotationItem.toJsonString(list)
    val parsed = com.example.data.QuotationItem.parseList(json)
    assertEquals(2, parsed.size)
    assertEquals("Replacement Display Panel", parsed[0].name)
    assertEquals(500.0, parsed[0].discount, 0.001)
    assertEquals(4500.0, parsed[0].total, 0.001)

    assertEquals("Service & Labor", parsed[1].name)
    assertEquals(10.0, parsed[1].discountPercent, 0.001)
    assertEquals(120.0, parsed[1].effectiveDiscount, 0.001)
    assertEquals(1080.0, parsed[1].total, 0.001)
  }
}
