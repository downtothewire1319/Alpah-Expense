package com.example

import com.example.data.model.BankAccount
import com.example.data.model.Expense
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testBudgetExcludesIncome() {
        // Simulating the exact scenario from user's report
        val transactions = listOf(
            Expense(id = "1", title = "Salary", amount = 32000.0, transactionType = "income", category = "Salary / Income"),
            Expense(id = "2", title = "Sohan", amount = 7560.0, transactionType = "expense", category = "Loan Paid"),
            Expense(id = "3", title = "Phone Emi", amount = 5586.0, transactionType = "expense", category = "Other"),
            Expense(id = "4", title = "Anowar", amount = 5000.0, transactionType = "expense", category = "Personal")
        )

        val totalExpenses = transactions.filter { !it.isIncome }.sumOf { it.amount }
        val totalIncome = transactions.filter { it.isIncome }.sumOf { it.amount }
        val netSavings = totalIncome - totalExpenses

        // Total expenses MUST be 18,146 and NOT 50,146
        assertEquals(18146.0, totalExpenses, 0.001)
        assertEquals(32000.0, totalIncome, 0.001)
        assertEquals(13854.0, netSavings, 0.001)
    }

    @Test
    fun testBankBalanceAdjustment() {
        val meghnaBank = BankAccount(
            id = "meghna",
            name = "Meghna Bank",
            balance = 25000.0
        )

        // Expense of 5586 paid via Meghna Bank
        val newBalance = meghnaBank.balance - 5586.0
        assertEquals(19414.0, newBalance, 0.001)
    }
}
