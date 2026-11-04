package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.BankAccount
import com.example.data.model.Budget
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategories
import com.example.data.model.UserProfile
import com.example.data.model.UserSecurity
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ExpenseRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- User Profile ---

    suspend fun syncUserProfile(user: FirebaseUser): Result<Unit> = runCatching {
        val uid = user.uid
        val docRef = db.collection("users").document(uid)
        val snapshot = docRef.get().await()
        if (!snapshot.exists()) {
            val profileData = mapOf(
                "userId" to uid,
                "name" to (user.displayName ?: "User"),
                "email" to (user.email ?: ""),
                "photoUrl" to user.photoUrl?.toString(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(profileData).await()
        } else {
            val existingName = snapshot.getString("name")
            val existingPhoto = snapshot.getString("photoUrl")
            val updateData = mutableMapOf<String, Any>(
                "email" to (user.email ?: ""),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (existingName.isNullOrBlank() && !user.displayName.isNullOrBlank()) {
                updateData["name"] = user.displayName!!
            }
            if (existingPhoto.isNullOrBlank() && user.photoUrl != null) {
                updateData["photoUrl"] = user.photoUrl.toString()
            }
            docRef.update(updateData).await()
        }
        Unit
    }.onFailure { e ->
        if (e is Exception) handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}")
    }

    fun observeUserProfile(): Flow<UserProfile?> = flow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            emit(null)
            return@flow
        }
        val docRef = db.collection("users").document(uid)
        emitAll(
            docRef.snapshots().map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(UserProfile::class.java)?.copy(id = snapshot.id)
                } else {
                    null
                }
            }
        )
    }.catch { e ->
        if (e is Exception) handleFirestoreError(e, OperationType.GET, "users/${auth.currentUser?.uid}")
        emit(null)
    }

    suspend fun updateUserProfile(name: String, photoUrl: String?): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val updateMap = mutableMapOf<String, Any>(
            "name" to name.trim(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (photoUrl != null) {
            updateMap["photoUrl"] = photoUrl
        }
        docRef.update(updateMap).await()
        Unit
    }.onFailure { e ->
        if (e is Exception) handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}")
    }

    // --- Security Settings (PIN) ---

    suspend fun getUserSecurity(): Result<UserSecurity?> = runCatching {
        val uid = requireUserId()
        val path = "users/$uid/security/pin"
        val snapshot = db.document(path).get().await()
        if (snapshot.exists()) {
            snapshot.toObject(UserSecurity::class.java)
        } else {
            null
        }
    }.onFailure { e ->
        if (e is Exception) handleFirestoreError(e, OperationType.GET, "users/${auth.currentUser?.uid}/security/pin")
    }

    suspend fun saveUserSecurity(
        hashedPin: String,
        salt: String,
        autoLockEnabled: Boolean,
        autoLockMinutes: Int
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("security").document("pin")
        val exists = docRef.get().await().exists()

        if (!exists) {
            val data = mapOf(
                "userId" to uid,
                "hashedPin" to hashedPin,
                "pinSalt" to salt,
                "autoLockEnabled" to autoLockEnabled,
                "autoLockMinutes" to autoLockMinutes,
                "pinUpdatedAt" to FieldValue.serverTimestamp(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(data).await()
        } else {
            val data = mapOf(
                "hashedPin" to hashedPin,
                "pinSalt" to salt,
                "autoLockEnabled" to autoLockEnabled,
                "autoLockMinutes" to autoLockMinutes,
                "pinUpdatedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(data).await()
        }
        Unit
    }.onFailure { e ->
        if (e is Exception) handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}/security/pin")
    }

    // --- Public Expenses ---

    fun observePublicExpenses(): Flow<List<Expense>> {
        val path = "public_expenses"
        return db.collection(path)
            .orderBy("date", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Expense::class.java)?.let { exp ->
                        if (exp.id.isBlank()) exp.copy(id = doc.id) else exp
                    }
                }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    suspend fun addPublicExpense(expense: Expense): Result<String> = runCatching {
        val uid = requireUserId()
        val user = auth.currentUser
        val expenseId = if (expense.id.isNotBlank()) expense.id else "pub_${UUID.randomUUID().toString().replace("-", "")}"
        val docRef = db.collection("public_expenses").document(expenseId)

        val payload = mapOf(
            "userId" to uid,
            "creatorName" to (user?.displayName ?: "Family Member"),
            "creatorEmail" to (user?.email ?: ""),
            "expenseType" to "public",
            "category" to expense.category,
            "title" to expense.title,
            "amount" to expense.amount,
            "date" to expense.date,
            "paymentMethod" to expense.paymentMethod,
            "notes" to expense.notes,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        docRef.set(payload).await()
        expenseId
    }.onFailure { e ->
        if (e is Exception) handleFirestoreError(e, OperationType.CREATE, "public_expenses")
    }

    suspend fun updatePublicExpense(expense: Expense): Result<Unit> = runCatching {
        require(expense.id.isNotBlank()) { "Expense ID must not be blank" }
        val docRef = db.collection("public_expenses").document(expense.id)

        val updateData = mapOf(
            "category" to expense.category,
            "title" to expense.title,
            "amount" to expense.amount,
            "date" to expense.date,
            "paymentMethod" to expense.paymentMethod,
            "notes" to expense.notes,
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        docRef.update(updateData).await()
        Unit
    }.onFailure { e ->
        if (e is Exception) handleFirestoreError(e, OperationType.UPDATE, "public_expenses/${expense.id}")
    }

    suspend fun deletePublicExpense(expenseId: String): Result<Unit> = runCatching {
        val docRef = db.collection("public_expenses").document(expenseId)
        docRef.delete().await()
        Unit
    }.onFailure { e ->
        if (e is Exception) handleFirestoreError(e, OperationType.DELETE, "public_expenses/$expenseId")
    }

    // --- Private Expenses (Owner Locked) ---

    fun observePrivateExpenses(): Flow<List<Expense>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/private_expenses"
        val expenseFlow = db.collection("users").document(uid).collection("private_expenses")
            .orderBy("date", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Expense::class.java)?.let { exp ->
                        if (exp.id.isBlank()) exp.copy(id = doc.id) else exp
                    }
                }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
        emitAll(expenseFlow)
    }

    suspend fun addPrivateExpense(expense: Expense): Result<String> = runCatching {
        val uid = requireUserId()
        val expenseId = if (expense.id.isNotBlank()) expense.id else "priv_${UUID.randomUUID().toString().replace("-", "")}"
        val docRef = db.collection("users").document(uid).collection("private_expenses").document(expenseId)

        val payload = mapOf(
            "userId" to uid,
            "expenseType" to "private",
            "category" to expense.category,
            "title" to expense.title,
            "amount" to expense.amount,
            "date" to expense.date,
            "paymentMethod" to expense.paymentMethod,
            "notes" to expense.notes,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        docRef.set(payload).await()
        expenseId
    }.onFailure { e ->
        if (e is Exception) handleFirestoreError(e, OperationType.CREATE, "users/${auth.currentUser?.uid}/private_expenses")
    }

    suspend fun updatePrivateExpense(expense: Expense): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(expense.id.isNotBlank()) { "Expense ID must not be blank" }
        val docRef = db.collection("users").document(uid).collection("private_expenses").document(expense.id)

        val updateData = mapOf(
            "category" to expense.category,
            "title" to expense.title,
            "amount" to expense.amount,
            "date" to expense.date,
            "paymentMethod" to expense.paymentMethod,
            "notes" to expense.notes,
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        docRef.update(updateData).await()
        Unit
    }.onFailure { e ->
        if (e is Exception) handleFirestoreError(e, OperationType.UPDATE, "users/${auth.currentUser?.uid}/private_expenses/${expense.id}")
    }

    suspend fun deletePrivateExpense(expenseId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("private_expenses").document(expenseId)
        docRef.delete().await()
        Unit
    }.onFailure { e ->
        if (e is Exception) handleFirestoreError(e, OperationType.DELETE, "users/${auth.currentUser?.uid}/private_expenses/$expenseId")
    }

    // --- Budgets ---

    fun observeBudget(month: String): Flow<Budget?> = flow {
        val uid = requireUserId()
        val path = "users/$uid/budgets/$month"
        val budgetFlow = db.collection("users").document(uid).collection("budgets").document(month)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(Budget::class.java)?.let { b ->
                        if (b.id.isBlank()) b.copy(id = snapshot.id) else b
                    }
                } else null
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.GET, path)
                throw e
            }
        emitAll(budgetFlow)
    }

    suspend fun saveBudget(budget: Budget): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(budget.month.isNotBlank()) { "Month must not be blank" }
        val docRef = db.collection("users").document(uid).collection("budgets").document(budget.month)
        val exists = docRef.get().await().exists()

        if (!exists) {
            val payload = mapOf(
                "userId" to uid,
                "month" to budget.month,
                "monthlyBudget" to budget.monthlyBudget,
                "publicBudget" to budget.publicBudget,
                "privateBudget" to budget.privateBudget,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
        } else {
            val updatePayload = mapOf(
                "month" to budget.month,
                "monthlyBudget" to budget.monthlyBudget,
                "publicBudget" to budget.publicBudget,
                "privateBudget" to budget.privateBudget,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updatePayload).await()
        }
        Unit
    }.onFailure { e ->
        if (e is Exception) handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}/budgets/${budget.month}")
    }

    // --- Bank Accounts / Monthly Savings ---

    private suspend fun seedDefaultBankAccountsIfEmpty(uid: String) {
        val col = db.collection("users").document(uid).collection("bank_accounts")
        val snapshot = col.get().await()
        if (snapshot.isEmpty) {
            val batch = db.batch()
            for (acc in ExpenseCategories.defaultBankAccounts) {
                val docRef = col.document(acc.id)
                val payload = mapOf(
                    "userId" to uid,
                    "name" to acc.name,
                    "accountNumber" to acc.accountNumber,
                    "accountType" to acc.accountType,
                    "balance" to acc.balance,
                    "initialBalance" to acc.initialBalance,
                    "iconName" to acc.iconName,
                    "colorHex" to acc.colorHex,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(docRef, payload)
            }
            batch.commit().await()
        }
    }

    fun observeBankAccounts(): Flow<List<BankAccount>> = flow {
        val uid = requireUserId()
        runCatching { seedDefaultBankAccountsIfEmpty(uid) }
        val path = "users/$uid/bank_accounts"
        val accountsFlow = db.collection("users").document(uid).collection("bank_accounts")
            .snapshots()
            .map { snapshot ->
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(BankAccount::class.java)?.let { acc ->
                        if (acc.id.isBlank()) acc.copy(id = doc.id) else acc
                    }
                }
                if (list.isEmpty()) {
                    ExpenseCategories.defaultBankAccounts
                } else {
                    list
                }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.GET, path)
                emit(ExpenseCategories.defaultBankAccounts)
            }
        emitAll(accountsFlow)
    }

    suspend fun saveBankAccount(account: BankAccount): Result<Unit> = runCatching {
        val uid = requireUserId()
        runCatching { seedDefaultBankAccountsIfEmpty(uid) }
        val id = if (account.id.isNotBlank()) account.id else UUID.randomUUID().toString().replace("-", "")
        val docRef = db.collection("users").document(uid).collection("bank_accounts").document(id)
        val exists = docRef.get().await().exists()

        if (!exists) {
            val payload = mapOf(
                "id" to id,
                "userId" to uid,
                "name" to account.name,
                "accountNumber" to account.accountNumber,
                "accountType" to account.accountType,
                "balance" to account.balance,
                "initialBalance" to (if (account.initialBalance > 0) account.initialBalance else account.balance),
                "iconName" to account.iconName,
                "colorHex" to account.colorHex,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
        } else {
            val updatePayload = mapOf(
                "name" to account.name,
                "accountNumber" to account.accountNumber,
                "accountType" to account.accountType,
                "balance" to account.balance,
                "iconName" to account.iconName,
                "colorHex" to account.colorHex,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updatePayload).await()
        }
        Unit
    }.onFailure { e ->
        if (e is Exception) handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}/bank_accounts")
    }

    suspend fun updateBankBalance(accountId: String, newBalance: Double): Result<Unit> = runCatching {
        val uid = requireUserId()
        runCatching { seedDefaultBankAccountsIfEmpty(uid) }
        val docRef = db.collection("users").document(uid).collection("bank_accounts").document(accountId)
        val snapshot = docRef.get().await()
        if (snapshot.exists()) {
            docRef.update(
                mapOf(
                    "balance" to newBalance,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            val matchingDefault = ExpenseCategories.defaultBankAccounts.find { it.id == accountId || it.name.equals(accountId, ignoreCase = true) }
            val name = matchingDefault?.name ?: accountId
            val payload = mapOf(
                "id" to accountId,
                "userId" to uid,
                "name" to name,
                "accountNumber" to (matchingDefault?.accountNumber ?: ""),
                "accountType" to (matchingDefault?.accountType ?: "Savings"),
                "balance" to newBalance,
                "initialBalance" to (matchingDefault?.initialBalance ?: newBalance),
                "iconName" to (matchingDefault?.iconName ?: "bank"),
                "colorHex" to (matchingDefault?.colorHex ?: 0xFF1E88E5),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
        }
        Unit
    }.onFailure { e ->
        if (e is Exception) handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}/bank_accounts/$accountId")
    }

    suspend fun adjustBankBalanceByName(bankName: String, delta: Double): Result<Unit> = runCatching {
        val uid = requireUserId()
        runCatching { seedDefaultBankAccountsIfEmpty(uid) }
        val col = db.collection("users").document(uid).collection("bank_accounts")
        val allDocs = col.get().await().documents
        val matchingDoc = allDocs.find { doc ->
            val name = doc.getString("name") ?: ""
            val id = doc.id
            name.equals(bankName, ignoreCase = true) ||
            id.equals(bankName, ignoreCase = true) ||
            bankName.contains(name, ignoreCase = true) ||
            name.contains(bankName, ignoreCase = true)
        }

        if (matchingDoc != null) {
            val currentBal = matchingDoc.getDouble("balance") ?: 0.0
            val newBal = currentBal + delta
            matchingDoc.reference.update(
                mapOf(
                    "balance" to newBal,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            val newId = UUID.randomUUID().toString().replace("-", "").take(16)
            val icon = if (bankName.contains("bKash", true) || bankName.contains("Nagad", true)) "mobile" else "bank"
            val payload = mapOf(
                "id" to newId,
                "userId" to uid,
                "name" to bankName,
                "accountNumber" to "",
                "accountType" to "Savings",
                "balance" to delta,
                "initialBalance" to 0.0,
                "iconName" to icon,
                "colorHex" to 0xFF1E88E5,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            col.document(newId).set(payload).await()
        }
        Unit
    }

    suspend fun deleteBankAccount(accountId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        db.collection("users").document(uid).collection("bank_accounts").document(accountId).delete().await()
        Unit
    }.onFailure { e ->
        if (e is Exception) handleFirestoreError(e, OperationType.DELETE, "users/${auth.currentUser?.uid}/bank_accounts/$accountId")
    }
}

