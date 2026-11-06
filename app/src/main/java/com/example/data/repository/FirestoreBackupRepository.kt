package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.BankAccount
import com.example.data.model.Budget
import com.example.data.model.Expense
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupMetadata(
    val lastBackupAt: Timestamp? = null,
    val expensesCount: Int = 0,
    val accountsCount: Int = 0,
    val budgetsCount: Int = 0,
    val status: String = "SUCCESS"
) {
    fun getFormattedDate(): String {
        return lastBackupAt?.toDate()?.let {
            SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.US).format(it)
        } ?: "Never"
    }
}

data class RestoredData(
    val expenses: List<Expense>,
    val accounts: List<BankAccount>,
    val budgets: List<Budget>
)

class FirestoreBackupRepository(
    private val context: Context,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(
        context.applicationContext.getString(R.string.firestore_database_id)
    )
) {
    suspend fun backupAllData(
        userId: String,
        expenses: List<Expense>,
        accounts: List<BankAccount>,
        budgets: List<Budget>
    ): Result<BackupMetadata> {
        if (userId.isBlank()) {
            return Result.failure(IllegalStateException("User must be signed in with Google to backup data."))
        }

        return try {
            Log.d("BackupRepo", "Starting manual backup for user $userId with ${expenses.size} expenses, ${accounts.size} accounts")
            val batch = db.batch()

            // 1. Expenses
            expenses.forEach { exp ->
                val docRef = db.collection("users").document(userId).collection("expenses").document(exp.id)
                val expData = mapOf(
                    "id" to exp.id,
                    "userId" to userId,
                    "creatorName" to exp.creatorName,
                    "creatorEmail" to exp.creatorEmail,
                    "expenseType" to exp.expenseType,
                    "category" to exp.category,
                    "title" to exp.title,
                    "amount" to exp.amount,
                    "date" to exp.date,
                    "paymentMethod" to exp.paymentMethod,
                    "notes" to exp.notes,
                    "transactionType" to exp.transactionType,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(docRef, expData)
            }

            // 2. Accounts
            accounts.forEach { acc ->
                val docRef = db.collection("users").document(userId).collection("accounts").document(acc.id)
                val accData = mapOf(
                    "id" to acc.id,
                    "userId" to userId,
                    "name" to acc.name,
                    "accountNumber" to acc.accountNumber,
                    "accountType" to acc.accountType,
                    "balance" to acc.balance,
                    "initialBalance" to acc.initialBalance,
                    "iconName" to acc.iconName,
                    "colorHex" to acc.colorHex,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(docRef, accData)
            }

            // 3. Budgets
            budgets.forEach { b ->
                val docId = b.id.ifBlank { b.month }
                val docRef = db.collection("users").document(userId).collection("budgets").document(docId)
                val bData = mapOf(
                    "id" to docId,
                    "userId" to userId,
                    "month" to b.month,
                    "monthlyBudget" to b.monthlyBudget,
                    "publicBudget" to b.publicBudget,
                    "privateBudget" to b.privateBudget,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(docRef, bData)
            }

            // 4. Backup Metadata
            val metaRef = db.collection("users").document(userId).collection("backups").document("latest")
            val metaData = mapOf(
                "userId" to userId,
                "lastBackupAt" to FieldValue.serverTimestamp(),
                "expensesCount" to expenses.size,
                "accountsCount" to accounts.size,
                "budgetsCount" to budgets.size,
                "status" to "SUCCESS"
            )
            batch.set(metaRef, metaData)

            batch.commit().await()

            val metadata = BackupMetadata(
                lastBackupAt = Timestamp.now(),
                expensesCount = expenses.size,
                accountsCount = accounts.size,
                budgetsCount = budgets.size,
                status = "SUCCESS"
            )
            Result.success(metadata)
        } catch (e: Exception) {
            val userErrorMsg = handleFirestoreError(e, OperationType.WRITE, "users/$userId/backups/latest")
            Log.e("BackupRepo", "Backup failed: $userErrorMsg", e)
            Result.failure(Exception(userErrorMsg, e))
        }
    }

    suspend fun fetchLatestBackupMetadata(userId: String): BackupMetadata? {
        if (userId.isBlank()) return null
        return try {
            val doc = db.collection("users").document(userId).collection("backups").document("latest").get().await()
            if (doc.exists()) {
                BackupMetadata(
                    lastBackupAt = doc.getTimestamp("lastBackupAt"),
                    expensesCount = doc.getLong("expensesCount")?.toInt() ?: 0,
                    accountsCount = doc.getLong("accountsCount")?.toInt() ?: 0,
                    budgetsCount = doc.getLong("budgetsCount")?.toInt() ?: 0,
                    status = doc.getString("status") ?: "SUCCESS"
                )
            } else null
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "users/$userId/backups/latest")
            null
        }
    }

    suspend fun restoreAllData(userId: String): Result<RestoredData> {
        if (userId.isBlank()) {
            return Result.failure(IllegalStateException("User must be signed in with Google to restore data."))
        }

        return try {
            val expDocs = db.collection("users").document(userId).collection("expenses").get().await()
            val accDocs = db.collection("users").document(userId).collection("accounts").get().await()
            val budgetDocs = db.collection("users").document(userId).collection("budgets").get().await()

            val restoredExpenses = expDocs.documents.mapNotNull { doc ->
                doc.toObject(Expense::class.java)
            }
            val restoredAccounts = accDocs.documents.mapNotNull { doc ->
                doc.toObject(BankAccount::class.java)
            }
            val restoredBudgets = budgetDocs.documents.mapNotNull { doc ->
                doc.toObject(Budget::class.java)
            }

            Result.success(
                RestoredData(
                    expenses = restoredExpenses,
                    accounts = restoredAccounts,
                    budgets = restoredBudgets
                )
            )
        } catch (e: Exception) {
            val userErrorMsg = handleFirestoreError(e, OperationType.GET, "users/$userId")
            Log.e("BackupRepo", "Restore failed: $userErrorMsg", e)
            Result.failure(Exception(userErrorMsg, e))
        }
    }
}
