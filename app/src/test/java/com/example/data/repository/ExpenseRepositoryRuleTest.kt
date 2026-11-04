package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Expense
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

import org.junit.Ignore

@Ignore("Requires local Firestore emulator running")
class ExpenseRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun addPublicExpense_valid_createsDocument() = runBlocking<Unit> {
        signInTestUser(ALICE_EMAIL)
        val repository = ExpenseRepository(firestore)

        val expense = Expense(
            id = "pub_${UUID.randomUUID().toString().replace("-", "")}",
            expenseType = "public",
            category = "Groceries",
            title = "Supermarket Grocery",
            amount = 1500.0,
            date = "2026-11-02",
            paymentMethod = "Cash",
            notes = "Weekly essentials"
        )

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addPublicExpense(expense)
        }
        assertTrue(result.isSuccess)
    }

    @Test
    fun observePublicExpenses_returnsEmittedList() = runBlocking<Unit> {
        signInTestUser(ALICE_EMAIL)
        val repository = ExpenseRepository(firestore)
        val id = "pub_${UUID.randomUUID().toString().replace("-", "")}"
        val expense = Expense(
            id = id,
            expenseType = "public",
            category = "Household",
            title = "Electricity Bill",
            amount = 800.0,
            date = "2026-11-02",
            paymentMethod = "bKash / Mobile Banking"
        )

        repository.addPublicExpense(expense)

        val list = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observePublicExpenses().first { expenses ->
                expenses.any { it.id == id }
            }
        }
        assertTrue(list.any { it.id == id })
    }

    @Test
    fun addPrivateExpense_valid_createsDocument() = runBlocking<Unit> {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = ExpenseRepository(firestore)

        val id = "priv_${UUID.randomUUID().toString().replace("-", "")}"
        val expense = Expense(
            id = id,
            userId = aliceUid,
            expenseType = "private",
            category = "Personal",
            title = "Earphones",
            amount = 2500.0,
            date = "2026-11-02",
            paymentMethod = "Card"
        )

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addPrivateExpense(expense)
        }
        assertTrue(result.isSuccess)
    }

    @Test
    fun privateExpense_crossUserAccess_isForbidden() = runBlocking<Unit> {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = ExpenseRepository(firestore)
        val privId = "priv_${UUID.randomUUID().toString().replace("-", "")}"
        val expense = Expense(
            id = privId,
            userId = aliceUid,
            expenseType = "private",
            category = "Personal",
            title = "Secret Diary",
            amount = 300.0,
            date = "2026-11-02",
            paymentMethod = "Cash"
        )
        aliceRepo.addPrivateExpense(expense)

        // Now Bob signs in and attempts to access Alice's private expenses directly
        signInTestUser(BOB_EMAIL)
        val bobDb = firestore

        val caughtException: FirebaseFirestoreException? = try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                bobDb.collection("users").document(aliceUid).collection("private_expenses").document(privId).get().await()
            }
            null
        } catch (e: Exception) {
            e.unwrapFirestoreException()
        }

        assertNotNull(caughtException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, caughtException?.code)
    }

    @Test
    fun securityPin_saveAndVerifyByOwner_succeeds() = runBlocking<Unit> {
        signInTestUser(ALICE_EMAIL)
        val repository = ExpenseRepository(firestore)

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveUserSecurity(
                hashedPin = "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
                salt = "randomsalt123",
                autoLockEnabled = true,
                autoLockMinutes = 5
            )
        }
        assertTrue(saveResult.isSuccess)

        val getResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getUserSecurity()
        }
        assertTrue(getResult.isSuccess)
        val security = getResult.getOrNull()
        assertNotNull(security)
        assertEquals("1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef", security?.hashedPin)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_rule@test.com"
        const val BOB_EMAIL = "bob_rule@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
