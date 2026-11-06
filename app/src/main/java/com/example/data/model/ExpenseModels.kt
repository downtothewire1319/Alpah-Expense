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
    val transactionType: String = "expense", // "expense", "income", or "transfer"
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val isIncome: Boolean
        get() = transactionType.equals("income", ignoreCase = true) || 
                category.contains("Income", ignoreCase = true) ||
                category.contains("Salary", ignoreCase = true) ||
                ExpenseCategories.incomeCategories.any { it.equals(category, ignoreCase = true) } ||
                (transactionType.equals("transfer", ignoreCase = true) && (title.contains("from", ignoreCase = true) || notes?.contains("Received", ignoreCase = true) == true))

    val isExpense: Boolean
        get() = transactionType.equals("expense", ignoreCase = true) || 
                (!isIncome && !isTransfer)

    val isTransfer: Boolean
        get() = transactionType.equals("transfer", ignoreCase = true) || category.equals("Savings Transfer", ignoreCase = true)

    // Helper to identify if it's a REAL income (not a transfer)
    val isActualIncome: Boolean
        get() = (transactionType.equals("income", ignoreCase = true) || 
                 category.contains("Income", ignoreCase = true) || 
                 category.contains("Salary", ignoreCase = true) ||
                 ExpenseCategories.incomeCategories.any { it.equals(category, ignoreCase = true) }) && !isTransfer

    // Helper to identify if it's a REAL expense (not a transfer)
    val isActualExpense: Boolean
        get() = !isActualIncome && !isTransfer
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
        "Family Support",
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
        "Family Support",
        "Other"
    )

    val paymentMethods = listOf(
        "Janata Bank",
        "Dutch-Bangla Bank (DBBL)",
        "Meghna Bank",
        "bKash",
        "Nagad",
        "Cash in Hand",
        "Card",
        "Other"
    )

    val defaultPaymentSources = listOf(
        "Janata Bank",
        "Dutch-Bangla Bank (DBBL)",
        "Meghna Bank",
        "bKash",
        "Nagad",
        "Cash in Hand",
        "Other"
    )

    val defaultFamilyMembers = listOf(
        FamilyMember(id = "elder_brother", name = "Elder Brother", relationship = "Elder Brother", avatarColorHex = 0xFF1976D2),
        FamilyMember(id = "father", name = "Father", relationship = "Father", avatarColorHex = 0xFF388E3C),
        FamilyMember(id = "wife", name = "Wife", relationship = "Wife", avatarColorHex = 0xFFE91E63)
    )
}

@IgnoreExtraProperties
data class FamilyMember(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val relationship: String = "", // "Elder Brother", "Father", "Wife", "Mother", etc.
    val avatarColorHex: Long = 0xFF1E88E5,
    val phone: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FamilyFundTransfer(
    val id: String = "",
    val userId: String = "",
    val memberId: String = "",
    val memberName: String = "",
    val amount: Double = 0.0,
    val date: String = "", // "YYYY-MM-DD"
    val paymentSource: String = "Janata Bank",
    val transactionId: String = "",
    val notes: String = "",
    val status: String = "Completed",
    val linkedExpenseId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class MonthlySavingsBreakdown(
    val month: String = "",
    val income: Double = 0.0,
    val publicExpense: Double = 0.0,
    val privateExpense: Double = 0.0,
    val savings: Double = 0.0
)
