package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserSecurity(
    val id: String = "",
    val userId: String = "",
    val hashedPin: String = "",
    val pinSalt: String = "",
    val autoLockEnabled: Boolean = true,
    val autoLockMinutes: Int = 5,
    @ServerTimestamp val pinUpdatedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Expense(
    val id: String = "",
    val userId: String = "",
    val creatorName: String = "",
    val creatorEmail: String = "",
    val expenseType: String = "public", // "public" or "private"
    val category: String = "Other",
    val title: String = "",
    val amount: Double = 0.0,
    val date: String = "", // "YYYY-MM-DD"
    val paymentMethod: String = "Cash",
    val notes: String? = null,
    val transactionType: String = "expense", // "expense" or "income"
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val isIncome: Boolean
        get() = transactionType.equals("income", ignoreCase = true) ||
                ExpenseCategories.incomeCategories.contains(category) ||
                category.contains("Income", ignoreCase = true) ||
                category.contains("Salary", ignoreCase = true)
}

@IgnoreExtraProperties
data class Budget(
    val id: String = "",
    val userId: String = "",
    val month: String = "", // "YYYY-MM"
    val monthlyBudget: Double = 0.0,
    val publicBudget: Double = 0.0,
    val privateBudget: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BankAccount(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val accountNumber: String = "",
    val accountType: String = "Savings", // "Savings", "Current", "Mobile Wallet", "Cash"
    val balance: Double = 0.0,
    val initialBalance: Double = 0.0,
    val iconName: String = "bank", // "bank", "mobile", "cash"
    val colorHex: Long = 0xFF1E88E5,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

object ExpenseCategories {
    val defaultBankAccounts = listOf(
        BankAccount(id = "janata", name = "Janata Bank", accountType = "Savings", balance = 45000.0, initialBalance = 45000.0, iconName = "bank", colorHex = 0xFF1976D2),
        BankAccount(id = "meghna", name = "Meghna Bank", accountType = "Savings", balance = 35000.0, initialBalance = 35000.0, iconName = "bank", colorHex = 0xFF00897B),
        BankAccount(id = "dbbl", name = "Dutch Bangla Bank", accountType = "Savings", balance = 52000.0, initialBalance = 52000.0, iconName = "bank", colorHex = 0xFFE53935),
        BankAccount(id = "bkash", name = "bKash", accountType = "Mobile Wallet", balance = 12500.0, initialBalance = 12500.0, iconName = "mobile", colorHex = 0xFFD81B60),
        BankAccount(id = "cash", name = "Cash in Hand", accountType = "Cash", balance = 8000.0, initialBalance = 8000.0, iconName = "cash", colorHex = 0xFF43A047)
    )

    val incomeCategories = listOf(
        "Salary / Income",
        "Business Income",
        "Freelance",
        "Investment",
        "Bonus",
        "Monthly Savings Deposit",
        "Other Income"
    )

    val publicCategories = listOf(
        "Food",
        "Groceries",
        "Household",
        "Rent",
        "Electricity",
        "Internet",
        "Transportation",
        "Medical",
        "Entertainment",
        "Shopping",
        "Family",
        "Salary / Income",
        "Business Income",
        "Other"
    )

    val privateCategories = listOf(
        "Personal",
        "Shopping",
        "Personal Food",
        "Gifts",
        "Personal Travel",
        "Savings Transfer",
        "Other"
    )

    val paymentMethods = listOf(
        "Meghna Bank",
        "Janata Bank",
        "Dutch Bangla Bank",
        "bKash",
        "Cash in Hand",
        "Nagad",
        "Card",
        "Other"
    )
}

