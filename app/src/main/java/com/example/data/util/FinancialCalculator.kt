package com.example.data.util

import com.example.data.model.BankAccount
import com.example.data.model.Expense
import com.example.data.model.MonthlySavingsBreakdown
import java.util.Locale

/**
 * Unified financial calculation engine for the application.
 * Calculates net monthly savings, income, public & private expenses, and bank balances.
 * Strictly excludes transfers between bank accounts to prevent double-counting.
 */
object FinancialCalculator {

    /**
     * Checks whether an expense item represents an internal bank/savings transfer.
     */
    fun isTransferTransaction(expense: Expense): Boolean {
        return expense.isTransfer ||
                expense.category.equals("Savings Transfer", ignoreCase = true) ||
                expense.category.equals("Transfer", ignoreCase = true) ||
                expense.transactionType.equals("transfer", ignoreCase = true)
    }

    /**
     * Checks if an expense item is a genuine income transaction (and not an internal transfer).
     */
    fun isActualIncome(expense: Expense): Boolean {
        return expense.isActualIncome
    }

    /**
     * Checks if an expense item is a genuine expense transaction (and not an internal transfer or income).
     */
    fun isActualExpense(expense: Expense): Boolean {
        return expense.isActualExpense
    }

    /**
     * Calculates net monthly savings for a specific year and month:
     * Monthly Savings = Monthly Income - Public Expenses - Private Expenses
     *
     * Returns the net savings amount (positive or negative).
     */
    fun calculateMonthlySavings(
        year: Int,
        month: Int,
        publicExpenses: List<Expense> = emptyList(),
        privateExpenses: List<Expense> = emptyList()
    ): Double {
        val monthKey = String.format(Locale.US, "%04d-%02d", year, month)

        val monthPublic = publicExpenses.filter { it.date.startsWith(monthKey) }
        val monthPrivate = privateExpenses.filter { it.date.startsWith(monthKey) }

        val income = (monthPublic + monthPrivate)
            .filter { isActualIncome(it) }
            .sumOf { it.amount }

        val pubExpense = monthPublic
            .filter { isActualExpense(it) }
            .sumOf { it.amount }

        val privExpense = monthPrivate
            .filter { isActualExpense(it) }
            .sumOf { it.amount }

        return income - pubExpense - privExpense
    }

    /**
     * Overloaded helper calculating monthly savings from a single combined expense list.
     */
    fun calculateMonthlySavings(
        year: Int,
        month: Int,
        allExpenses: List<Expense>
    ): Double {
        val (publics, privates) = allExpenses.partition { it.expenseType.equals("public", ignoreCase = true) }
        return calculateMonthlySavings(year, month, publics, privates)
    }

    /**
     * Calculates full monthly savings breakdown (Income, Public Expense, Private Expense, Net Savings)
     * for a given month.
     */
    fun calculateMonthlyBreakdown(
        year: Int,
        month: Int,
        publicExpenses: List<Expense> = emptyList(),
        privateExpenses: List<Expense> = emptyList()
    ): MonthlySavingsBreakdown {
        val monthKey = String.format(Locale.US, "%04d-%02d", year, month)

        val monthPublic = publicExpenses.filter { it.date.startsWith(monthKey) }
        val monthPrivate = privateExpenses.filter { it.date.startsWith(monthKey) }

        val income = (monthPublic + monthPrivate)
            .filter { isActualIncome(it) }
            .sumOf { it.amount }

        val pubExpense = monthPublic
            .filter { isActualExpense(it) }
            .sumOf { it.amount }

        val privExpense = monthPrivate
            .filter { isActualExpense(it) }
            .sumOf { it.amount }

        val savings = income - pubExpense - privExpense

        return MonthlySavingsBreakdown(
            month = monthKey,
            income = income,
            publicExpense = pubExpense,
            privateExpense = privExpense,
            savings = savings
        )
    }

    /**
     * Calculates monthly savings breakdowns for all 12 months (Jan..Dec) of a given year.
     */
    fun calculateMonthlyBreakdownsForYear(
        year: Int,
        publicExpenses: List<Expense> = emptyList(),
        privateExpenses: List<Expense> = emptyList()
    ): Map<String, MonthlySavingsBreakdown> {
        val result = linkedMapOf<String, MonthlySavingsBreakdown>()
        for (m in 1..12) {
            val monthKey = String.format(Locale.US, "%04d-%02d", year, m)
            result[monthKey] = calculateMonthlyBreakdown(year, m, publicExpenses, privateExpenses)
        }
        return result
    }

    /**
     * Calculates current accumulated total running bank balance from connected bank accounts.
     */
    fun calculateTotalBankBalance(bankAccounts: List<BankAccount>): Double {
        return bankAccounts.sumOf { it.balance }
    }
}
