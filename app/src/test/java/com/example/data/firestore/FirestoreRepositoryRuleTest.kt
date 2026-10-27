package com.example.data.firestore

import com.example.base.FirestoreEmulatorTestBase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirestoreRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createTicket_authenticatedUser_success() = runBlocking {
        val uid = signInTestUser("alice.rule@example.com")
        val repo = FirestoreServiceRepository(firestore, auth)

        val ticket = FirestoreTicket(
            ticketNumber = "NEO-2026-TEST1",
            userId = uid,
            customerId = uid,
            customerName = "Alice Tester",
            customerPhone = "+91 98765 00000",
            deviceName = "MacBook Pro M2",
            issueDescription = "Battery replacement",
            status = "PENDING",
            paymentStatus = "PENDING"
        )

        val docId = withTimeout(10000L) {
            repo.createTicket(ticket)
        }
        assertTrue(docId.isNotBlank())

        val tickets = withTimeout(10000L) {
            repo.observeUserTickets(isAdmin = false).first { it.isNotEmpty() }
        }
        val created = tickets.find { it.ticketNumber == "NEO-2026-TEST1" }
        assertNotNull(created)
        assertEquals("Alice Tester", created?.customerName)
    }

    @Test
    fun saveUserProfile_authenticatedUser_success() = runBlocking {
        val uid = signInTestUser("bob.profile@example.com")
        val repo = FirestoreServiceRepository(firestore, auth)

        val user = FirestoreUser(
            userId = uid,
            email = "bob.profile@example.com",
            fullName = "Bob Profile",
            phone = "+91 99999 11111",
            role = "CUSTOMER"
        )

        withTimeout(10000L) {
            repo.saveUserProfile(user)
        }

        val observed = withTimeout(10000L) {
            repo.observeUserProfile(uid).first { it != null }
        }
        assertNotNull(observed)
        assertEquals("Bob Profile", observed?.fullName)
        assertEquals("CUSTOMER", observed?.role)
    }

    @Test(expected = Exception::class)
    fun unauthenticatedAccess_throwsException() {
        runBlocking {
            auth.signOut()
            val repo = FirestoreServiceRepository(firestore, auth)
            repo.requireUserId()
        }
    }
}
