package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BankAccount
import com.example.data.model.Budget
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategories
import com.example.data.model.FamilyFundTransfer
import com.example.data.model.FamilyMember
import com.example.data.model.MonthlySavingsBreakdown
import com.example.data.model.UserProfile
import com.example.data.model.UserSecurity
import com.example.data.repository.BackupMetadata
import com.example.data.repository.ExpenseRepository
import com.example.data.security.PinSecurityManager
import com.example.data.util.FinancialCalculator
import com.example.ui.util.CurrencyHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface BackupUiState {
    data object Loading : BackupUiState
    data class Success(val metadata: BackupMetadata, val message: String = "Backup successful") : BackupUiState
    data class Error(val message: String) : BackupUiState
}

enum class SyncStatus {
    SYNCED, SYNCING, OFFLINE
}

data class FinancialSummary(
    val totalIncome: Double = 0.0,
    val totalCost: Double = 0.0,
    val netBalance: Double = 0.0,
    val currentMonthIncome: Double = 0.0,
    val currentMonthCost: Double = 0.0,
    val currentMonthBalance: Double = 0.0,
    val totalPublicCost: Double = 0.0,
    val totalPublicIncome: Double = 0.0,
    val currentMonthPublicCost: Double = 0.0,
    val totalPrivateCost: Double = 0.0,
    val totalPrivateIncome: Double = 0.0,
    val currentMonthPrivateCost: Double = 0.0,
    val combinedCost: Double = 0.0,
    val combinedIncome: Double = 0.0,
    val combinedCurrentMonthCost: Double = 0.0,
    val publicCategoryTotals: Map<String, Double> = emptyMap(),
    val privateCategoryTotals: Map<String, Double> = emptyMap(),
    val combinedCategoryTotals: Map<String, Double> = emptyMap(),
    val publicMonthlyTrend: Map<String, Double> = emptyMap(),
    val privateMonthlyTrend: Map<String, Double> = emptyMap(),
    val combinedMonthlyTrend: Map<String, Double> = emptyMap(),
    val combinedMonthlySavingsTrend: Map<String, Double> = emptyMap(),
    val monthlySavingsBreakdowns: Map<String, MonthlySavingsBreakdown> = emptyMap(),
    val currentYearCost: Double = 0.0,
    val publicIncomeMonthlyTrend: Map<String, Double> = emptyMap(),
    val privateIncomeMonthlyTrend: Map<String, Double> = emptyMap(),
    val combinedIncomeMonthlyTrend: Map<String, Double> = emptyMap()
) {
    val totalPublic: Double get() = totalPublicCost
    val totalPrivate: Double get() = totalPrivateCost
    val combinedTotal: Double get() = combinedCost
    val currentMonthPublic: Double get() = currentMonthPublicCost
    val currentMonthPrivate: Double get() = currentMonthPrivateCost
    val combinedCurrentMonth: Double get() = combinedCurrentMonthCost
}

class ExpenseViewModel(
    private val repository: ExpenseRepository,
    val currentUserId: String
) : ViewModel() {

    private val currentMonthKey: String
        get() = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

    val isPrivateUnlocked = PinSecurityManager.isPrivateUnlocked

    // Custom categories created by user
    private val _customCategories = MutableStateFlow<List<String>>(emptyList())
    val customCategories: StateFlow<List<String>> = _customCategories.asStateFlow()

    fun addCustomCategory(newCategory: String) {
        val trimmed = newCategory.trim()
        if (trimmed.isNotBlank() && !_customCategories.value.contains(trimmed)) {
            _customCategories.value = _customCategories.value + trimmed
        }
    }

    fun getAllCategories(isIncomeMode: Boolean, isPrivate: Boolean): List<String> {
        val baseList = when {
            isIncomeMode -> ExpenseCategories.incomeCategories
            isPrivate -> ExpenseCategories.privateCategories
            else -> ExpenseCategories.publicCategories.filter { it !in ExpenseCategories.incomeCategories }
        }
        return (baseList + _customCategories.value).distinct()
    }

    // Cache preservation to prevent transient data loss during sync/loading
    private var lastPublicExpensesCache: List<Expense> = emptyList()
    private var lastPrivateExpensesCache: List<Expense> = emptyList()

    private val _isDataInitialized = MutableStateFlow(false)
    val isDataInitialized: StateFlow<Boolean> = _isDataInitialized.asStateFlow()

    // Public Expenses state
    val publicExpensesState: StateFlow<UiState<List<Expense>>> = repository.observePublicExpenses()
        .map<List<Expense>, UiState<List<Expense>>> { UiState.Success(it) }
        .catch { emit(UiState.Error(it.message ?: "Failed to load public expenses")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = UiState.Loading
        )

    // Private Expenses state (stream only emits when authenticated and user unlocked PIN)
    private val _privateExpenses = MutableStateFlow<List<Expense>>(emptyList())
    val privateExpenses: StateFlow<List<Expense>> = _privateExpenses.asStateFlow()

    private val _privateExpensesLoading = MutableStateFlow(false)
    val privateExpensesLoading: StateFlow<Boolean> = _privateExpensesLoading.asStateFlow()

    // Security Settings state
    private val _userSecurity = MutableStateFlow<UserSecurity?>(null)
    val userSecurity: StateFlow<UserSecurity?> = _userSecurity.asStateFlow()

    private val _isPinConfigured = MutableStateFlow(false)
    val isPinConfigured: StateFlow<Boolean> = _isPinConfigured.asStateFlow()

    // Current Month Budget
    val currentBudget: StateFlow<Budget?> = repository.observeBudget(currentMonthKey)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    // Selected currency
    private val _selectedCurrency = MutableStateFlow(CurrencyHelper.currentCurrency)
    val selectedCurrency: StateFlow<CurrencyHelper.Currency> = _selectedCurrency.asStateFlow()

    // Bank Accounts & Monthly Savings (Meghna, Janata, Dutch Bangla, bKash, etc.)
    val bankAccounts: StateFlow<List<BankAccount>> = repository.observeBankAccounts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ExpenseCategories.defaultBankAccounts
        )

    // Family Fund Management (Elder Brother, Father, Wife, etc.)
    val familyMembers: StateFlow<List<FamilyMember>> = repository.observeFamilyMembers()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ExpenseCategories.defaultFamilyMembers
        )

    val familyTransfers: StateFlow<List<FamilyFundTransfer>> = repository.observeFamilyTransfers()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    // Status messages for Snackbars/Alerts
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // User Profile state
    val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    val userProfileName: StateFlow<String> = combine(userProfile, PinSecurityManager.isPrivateUnlocked) { profile, _ ->
        profile?.name?.ifBlank { null } ?: repository.getCurrentUser()?.displayName ?: "User"
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = "User"
    )

    private val _syncStatus = MutableStateFlow(SyncStatus.SYNCED)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    fun updateUserProfile(name: String, photoUri: String?, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.updateUserProfile(name, photoUri)
            if (result.isSuccess) {
                _statusMessage.value = "Profile updated successfully"
                onComplete(true)
            } else {
                _statusMessage.value = "Failed to update profile: ${result.exceptionOrNull()?.message}"
                onComplete(false)
            }
        }
    }

    init {
        loadSecuritySettings()
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun setCurrency(currency: CurrencyHelper.Currency) {
        CurrencyHelper.currentCurrency = currency
        _selectedCurrency.value = currency
    }

    fun loadSecuritySettings() {
        viewModelScope.launch {
            val result = repository.getUserSecurity()
            if (result.isSuccess) {
                val sec = result.getOrNull()
                _userSecurity.value = sec
                _isPinConfigured.value = sec != null && sec.hashedPin.isNotBlank()
                if (sec != null) {
                    PinSecurityManager.autoLockMinutes = sec.autoLockMinutes
                    PinSecurityManager.autoLockEnabled = sec.autoLockEnabled
                }
            }
        }
    }

    // --- PIN Management ---

    fun verifyAndUnlockPin(enteredPin: String, onResult: (Boolean, String?) -> Unit) {
        PinSecurityManager.onUserActivity()
        val security = _userSecurity.value
        if (security == null || security.hashedPin.isBlank()) {
            onResult(false, "No PIN set yet. Please set a PIN.")
            return
        }

        val computedHash = PinSecurityManager.hashPin(enteredPin, security.pinSalt)
        if (computedHash == security.hashedPin) {
            PinSecurityManager.unlock()
            startObservingPrivateExpenses()
            onResult(true, null)
        } else {
            onResult(false, "Incorrect PIN. Please try again.")
        }
    }

    fun setupNewPin(newPin: String, onResult: (Boolean, String?) -> Unit) {
        if (newPin.length < 4 || newPin.length > 6) {
            onResult(false, "PIN must be 4 to 6 digits.")
            return
        }

        val salt = PinSecurityManager.generateSalt()
        val hashed = PinSecurityManager.hashPin(newPin, salt)

        viewModelScope.launch {
            val result = repository.saveUserSecurity(
                hashedPin = hashed,
                salt = salt,
                autoLockEnabled = true,
                autoLockMinutes = _userSecurity.value?.autoLockMinutes ?: 5
            )
            if (result.isSuccess) {
                loadSecuritySettings()
                PinSecurityManager.unlock()
                startObservingPrivateExpenses()
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.localizedMessage ?: "Failed to set PIN.")
            }
        }
    }

    fun changePin(currentPin: String, newPin: String, onResult: (Boolean, String?) -> Unit) {
        val security = _userSecurity.value
        if (security == null) {
            setupNewPin(newPin, onResult)
            return
        }

        val currentHash = PinSecurityManager.hashPin(currentPin, security.pinSalt)
        if (currentHash != security.hashedPin) {
            onResult(false, "Current PIN is incorrect.")
            return
        }

        if (newPin.length < 4 || newPin.length > 6) {
            onResult(false, "New PIN must be 4 to 6 digits.")
            return
        }

        val newSalt = PinSecurityManager.generateSalt()
        val newHashed = PinSecurityManager.hashPin(newPin, newSalt)

        viewModelScope.launch {
            val result = repository.saveUserSecurity(
                hashedPin = newHashed,
                salt = newSalt,
                autoLockEnabled = security.autoLockEnabled,
                autoLockMinutes = security.autoLockMinutes
            )
            if (result.isSuccess) {
                loadSecuritySettings()
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.localizedMessage ?: "Failed to update PIN.")
            }
        }
    }

    fun updateAutoLockSettings(enabled: Boolean, minutes: Int) {
        val sec = _userSecurity.value ?: return
        PinSecurityManager.autoLockEnabled = enabled
        PinSecurityManager.autoLockMinutes = minutes

        viewModelScope.launch {
            repository.saveUserSecurity(
                hashedPin = sec.hashedPin,
                salt = sec.pinSalt,
                autoLockEnabled = enabled,
                autoLockMinutes = minutes
            )
            loadSecuritySettings()
        }
    }

    fun lockPrivateSection() {
        PinSecurityManager.lock()
        _privateExpenses.value = emptyList()
    }

    private fun startObservingPrivateExpenses() {
        viewModelScope.launch {
            _privateExpensesLoading.value = true
            repository.observePrivateExpenses().collect { list ->
                _privateExpenses.value = list
                _privateExpensesLoading.value = false
            }
        }
    }

    fun onUserActivity() {
        val didLock = PinSecurityManager.checkAutoLock()
        if (didLock) {
            _privateExpenses.value = emptyList()
        } else {
            PinSecurityManager.onUserActivity()
        }
    }

    // --- Expense Operations ---

    fun addPublicExpense(
        title: String,
        amount: Double,
        category: String,
        date: String,
        paymentMethod: String,
        notes: String?,
        transactionType: String = "expense",
        onDone: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val expense = Expense(
                title = title.trim(),
                amount = amount,
                category = category,
                date = date,
                paymentMethod = paymentMethod,
                notes = notes?.trim()?.ifBlank { null },
                transactionType = transactionType,
                expenseType = "public"
            )
            val result = repository.addPublicExpense(expense)
            if (result.isSuccess) {
                val delta = if (expense.isIncome) expense.amount else -expense.amount
                repository.adjustBankBalanceByName(expense.paymentMethod, delta)
                autoBackup()
                _statusMessage.value = if (expense.isIncome) "Income recorded" else "Expense added"
                onDone(true)
            } else {
                _statusMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to add"
                onDone(false)
            }
        }
    }

    fun updatePublicExpense(
        id: String,
        title: String,
        amount: Double,
        category: String,
        date: String,
        paymentMethod: String,
        notes: String?,
        transactionType: String = "expense",
        onDone: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val oldExp = (publicExpensesState.value as? UiState.Success)?.data?.find { it.id == id }
            val expense = Expense(
                id = id,
                title = title.trim(),
                amount = amount,
                category = category,
                date = date,
                paymentMethod = paymentMethod,
                notes = notes?.trim()?.ifBlank { null },
                transactionType = transactionType,
                expenseType = "public"
            )
            val result = repository.updatePublicExpense(expense)
            if (result.isSuccess) {
                if (oldExp != null) {
                    val reverseOld = if (oldExp.isIncome) -oldExp.amount else oldExp.amount
                    repository.adjustBankBalanceByName(oldExp.paymentMethod, reverseOld)
                }
                val newDelta = if (expense.isIncome) expense.amount else -expense.amount
                repository.adjustBankBalanceByName(expense.paymentMethod, newDelta)
                autoBackup()
                _statusMessage.value = "Updated successfully"
                onDone(true)
            } else {
                _statusMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to update"
                onDone(false)
            }
        }
    }

    fun deletePublicExpense(id: String, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val exp = (publicExpensesState.value as? UiState.Success)?.data?.find { it.id == id }
            val result = repository.deletePublicExpense(id)
            if (result.isSuccess) {
                if (exp != null) {
                    val reverseDelta = if (exp.isIncome) -exp.amount else exp.amount
                    repository.adjustBankBalanceByName(exp.paymentMethod, reverseDelta)
                }
                autoBackup()
                _statusMessage.value = "Entry deleted"
                onDone(true)
            } else {
                _statusMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to delete"
                onDone(false)
            }
        }
    }

    fun addPrivateExpense(
        title: String,
        amount: Double,
        category: String,
        date: String,
        paymentMethod: String,
        notes: String?,
        transactionType: String = "expense",
        onDone: (Boolean) -> Unit = {}
    ) {
        if (!PinSecurityManager.isPrivateUnlocked.value) {
            _statusMessage.value = "Vault is locked"
            onDone(false)
            return
        }

        viewModelScope.launch {
            val expense = Expense(
                title = title.trim(),
                amount = amount,
                category = category,
                date = date,
                paymentMethod = paymentMethod,
                notes = notes?.trim()?.ifBlank { null },
                transactionType = transactionType,
                expenseType = "private"
            )
            val result = repository.addPrivateExpense(expense)
            if (result.isSuccess) {
                val delta = if (expense.isIncome) expense.amount else -expense.amount
                repository.adjustBankBalanceByName(expense.paymentMethod, delta)
                autoBackup()
                _statusMessage.value = if (expense.isIncome) "Private income saved" else "Private expense saved"
                onDone(true)
            } else {
                _statusMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to add"
                onDone(false)
            }
        }
    }

    fun updatePrivateExpense(
        id: String,
        title: String,
        amount: Double,
        category: String,
        date: String,
        paymentMethod: String,
        notes: String?,
        transactionType: String = "expense",
        onDone: (Boolean) -> Unit = {}
    ) {
        if (!PinSecurityManager.isPrivateUnlocked.value) {
            _statusMessage.value = "Vault is locked"
            onDone(false)
            return
        }

        viewModelScope.launch {
            val oldExp = _privateExpenses.value.find { it.id == id }
            val expense = Expense(
                id = id,
                title = title.trim(),
                amount = amount,
                category = category,
                date = date,
                paymentMethod = paymentMethod,
                notes = notes?.trim()?.ifBlank { null },
                transactionType = transactionType,
                expenseType = "private"
            )
            val result = repository.updatePrivateExpense(expense)
            if (result.isSuccess) {
                if (oldExp != null) {
                    val reverseOld = if (oldExp.isIncome) -oldExp.amount else oldExp.amount
                    repository.adjustBankBalanceByName(oldExp.paymentMethod, reverseOld)
                }
                val newDelta = if (expense.isIncome) expense.amount else -expense.amount
                repository.adjustBankBalanceByName(expense.paymentMethod, newDelta)
                autoBackup()
                _statusMessage.value = "Private entry updated"
                onDone(true)
            } else {
                _statusMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to update"
                onDone(false)
            }
        }
    }

    fun deletePrivateExpense(id: String, onDone: (Boolean) -> Unit = {}) {
        if (!PinSecurityManager.isPrivateUnlocked.value) {
            _statusMessage.value = "Vault is locked"
            onDone(false)
            return
        }

        viewModelScope.launch {
            val exp = _privateExpenses.value.find { it.id == id }
            val result = repository.deletePrivateExpense(id)
            if (result.isSuccess) {
                if (exp != null) {
                    val reverseDelta = if (exp.isIncome) -exp.amount else exp.amount
                    repository.adjustBankBalanceByName(exp.paymentMethod, reverseDelta)
                }
                autoBackup()
                _statusMessage.value = "Private entry deleted"
                onDone(true)
            } else {
                _statusMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to delete"
                onDone(false)
            }
        }
    }

    // --- Bank Account & Monthly Savings Operations ---

    fun saveBankAccount(account: BankAccount, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val oldAccount = bankAccounts.value.find { it.id == account.id }
            val res = if (oldAccount != null && oldAccount.name != account.name) {
                repository.renameBankAccount(account.id, oldAccount.name, account.name)
            } else {
                repository.saveBankAccount(account)
            }
            if (res.isSuccess) {
                autoBackup()
                _statusMessage.value = "Bank account updated"
                onDone(true)
            } else {
                _statusMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to save bank"
                onDone(false)
            }
        }
    }

    fun deleteBankAccount(accountId: String, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.deleteBankAccount(accountId)
            if (res.isSuccess) {
                autoBackup()
                _statusMessage.value = "Bank account deleted"
                onDone(true)
            } else {
                _statusMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to delete account"
                onDone(false)
            }
        }
    }

    fun updateBankBalance(accountId: String, newBalance: Double, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.updateBankBalance(accountId, newBalance)
            if (res.isSuccess) {
                autoBackup()
                _statusMessage.value = "Balance updated"
                onDone(true)
            } else {
                _statusMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to update balance"
                onDone(false)
            }
        }
    }

    fun depositToBank(bankName: String, amount: Double, note: String?, onDone: (Boolean) -> Unit = {}) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        if (PinSecurityManager.isPrivateUnlocked.value) {
            addPrivateExpense(
                title = "Deposit to $bankName",
                amount = amount,
                category = "Monthly Savings Deposit",
                date = today,
                paymentMethod = bankName,
                notes = note,
                transactionType = "income",
                onDone = onDone
            )
        } else {
            addPublicExpense(
                title = "Deposit to $bankName",
                amount = amount,
                category = "Monthly Savings Deposit",
                date = today,
                paymentMethod = bankName,
                notes = note,
                transactionType = "income",
                onDone = onDone
            )
        }
    }

    fun transferBetweenBanks(fromBank: String, toBank: String, amount: Double, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val deductRes = repository.adjustBankBalanceByName(fromBank, -amount)
            val addRes = repository.adjustBankBalanceByName(toBank, amount)
            if (deductRes.isSuccess && addRes.isSuccess) {
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                if (PinSecurityManager.isPrivateUnlocked.value) {
                    repository.addPrivateExpense(
                        Expense(
                            title = "Transfer to $toBank",
                            amount = amount,
                            category = "Savings Transfer",
                            date = today,
                            paymentMethod = fromBank,
                            notes = "Transferred $amount from $fromBank to $toBank",
                            transactionType = "transfer",
                            expenseType = "private"
                        )
                    )
                    repository.addPrivateExpense(
                        Expense(
                            title = "Transfer from $fromBank",
                            amount = amount,
                            category = "Savings Transfer",
                            date = today,
                            paymentMethod = toBank,
                            notes = "Received transfer of $amount from $fromBank",
                            transactionType = "transfer",
                            expenseType = "private"
                        )
                    )
                } else {
                    repository.addPublicExpense(
                        Expense(
                            title = "Transfer to $toBank",
                            amount = amount,
                            category = "Savings Transfer",
                            date = today,
                            paymentMethod = fromBank,
                            notes = "Transferred $amount from $fromBank to $toBank",
                            transactionType = "transfer",
                            expenseType = "public"
                        )
                    )
                    repository.addPublicExpense(
                        Expense(
                            title = "Transfer from $fromBank",
                            amount = amount,
                            category = "Savings Transfer",
                            date = today,
                            paymentMethod = toBank,
                            notes = "Received transfer of $amount from $fromBank",
                            transactionType = "transfer",
                            expenseType = "public"
                        )
                    )
                }
                autoBackup()
                _statusMessage.value = "Transferred ${CurrencyHelper.format(amount, selectedCurrency.value.symbol)} successfully"
                onDone(true)
            } else {
                _statusMessage.value = "Transfer failed"
                onDone(false)
            }
        }
    }


    // --- Family Fund Operations ---

    fun recordFamilyTransfer(
        memberId: String,
        memberName: String,
        amount: Double,
        date: String,
        paymentSource: String,
        transactionId: String,
        notes: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (amount <= 0.0) {
            _statusMessage.value = "Amount must be greater than zero"
            onDone(false)
            return
        }

        viewModelScope.launch {
            val transfer = FamilyFundTransfer(
                memberId = memberId,
                memberName = memberName,
                amount = amount,
                date = date.ifBlank { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) },
                paymentSource = paymentSource,
                transactionId = transactionId.trim(),
                notes = notes.trim(),
                status = "Completed"
            )

            val result = repository.addFamilyTransfer(transfer)
            if (result.isSuccess) {
                autoBackup()
                _statusMessage.value = "Recorded transfer of ${CurrencyHelper.format(amount, selectedCurrency.value.symbol)} to $memberName"
                onDone(true)
            } else {
                _statusMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to record family transfer"
                onDone(false)
            }
        }
    }

    fun updateFamilyTransfer(
        oldTransfer: FamilyFundTransfer,
        newTransfer: FamilyFundTransfer,
        onDone: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = repository.updateFamilyTransfer(oldTransfer, newTransfer)
            if (result.isSuccess) {
                autoBackup()
                _statusMessage.value = "Transfer updated successfully"
                onDone(true)
            } else {
                _statusMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to update transfer"
                onDone(false)
            }
        }
    }

    fun deleteFamilyTransfer(transfer: FamilyFundTransfer, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.deleteFamilyTransfer(transfer)
            if (result.isSuccess) {
                autoBackup()
                _statusMessage.value = "Transfer deleted & account balance refunded"
                onDone(true)
            } else {
                _statusMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to delete transfer"
                onDone(false)
            }
        }
    }

    fun saveFamilyMember(
        name: String,
        relationship: String,
        avatarColorHex: Long = 0xFF1E88E5,
        phone: String = "",
        notes: String = "",
        id: String = "",
        onDone: (Boolean) -> Unit = {}
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            _statusMessage.value = "Member name cannot be blank"
            onDone(false)
            return
        }

        viewModelScope.launch {
            val member = FamilyMember(
                id = id,
                name = trimmedName,
                relationship = relationship.ifBlank { "Family Member" },
                avatarColorHex = avatarColorHex,
                phone = phone.trim(),
                notes = notes.trim()
            )
            val result = repository.saveFamilyMember(member)
            if (result.isSuccess) {
                autoBackup()
                _statusMessage.value = "Family member '$trimmedName' saved"
                onDone(true)
            } else {
                _statusMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to save member"
                onDone(false)
            }
        }
    }

    fun deleteFamilyMember(memberId: String, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.deleteFamilyMember(memberId)
            if (result.isSuccess) {
                autoBackup()
                _statusMessage.value = "Family member removed"
                onDone(true)
            } else {
                _statusMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to delete member"
                onDone(false)
            }
        }
    }

    fun filterFamilyTransfers(
        transfers: List<FamilyFundTransfer>,
        memberId: String? = null,
        paymentSource: String? = null,
        period: String = "All Time",
        customStartDate: String? = null,
        customEndDate: String? = null
    ): List<FamilyFundTransfer> {
        val now = java.util.Calendar.getInstance()
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now.time)
        val thisMonthPrefix = SimpleDateFormat("yyyy-MM", Locale.US).format(now.time)
        val thisYearPrefix = SimpleDateFormat("yyyy", Locale.US).format(now.time)

        val calLastMonth = java.util.Calendar.getInstance().apply { add(java.util.Calendar.MONTH, -1) }
        val lastMonthPrefix = SimpleDateFormat("yyyy-MM", Locale.US).format(calLastMonth.time)

        val cal3MonthsAgo = java.util.Calendar.getInstance().apply { add(java.util.Calendar.MONTH, -3) }
        val threeMonthsAgoStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal3MonthsAgo.time)

        val cal4MonthsAgo = java.util.Calendar.getInstance().apply { add(java.util.Calendar.MONTH, -4) }
        val fourMonthsAgoStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal4MonthsAgo.time)

        val calWeekAgo = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -7) }
        val weekAgoStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calWeekAgo.time)

        return transfers.filter { item ->
            // 1. Member filter
            val memberMatches = memberId.isNullOrBlank() || memberId == "All" ||
                    item.memberId == memberId || item.memberName.equals(memberId, ignoreCase = true)
            if (!memberMatches) return@filter false

            // 2. Payment source filter
            val sourceMatches = paymentSource.isNullOrBlank() || paymentSource == "All" ||
                    item.paymentSource.equals(paymentSource, ignoreCase = true)
            if (!sourceMatches) return@filter false

            // 3. Period filter
            val periodMatches = when (period) {
                "Today" -> item.date == todayStr
                "This Week" -> item.date in weekAgoStr..todayStr
                "This Month" -> item.date.startsWith(thisMonthPrefix)
                "Last Month" -> item.date.startsWith(lastMonthPrefix)
                "Last 3 Months" -> item.date >= threeMonthsAgoStr && item.date <= todayStr
                "Last 4 Months" -> item.date >= fourMonthsAgoStr && item.date <= todayStr
                "This Year" -> item.date.startsWith(thisYearPrefix)
                "Custom Date Range" -> {
                    val startOk = customStartDate.isNullOrBlank() || item.date >= customStartDate
                    val endOk = customEndDate.isNullOrBlank() || item.date <= customEndDate
                    startOk && endOk
                }
                else -> true
            }
            periodMatches
        }
    }

    // --- Budget Operations ---

    fun saveBudget(monthlyBudget: Double, publicBudget: Double, privateBudget: Double = 0.0, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val budget = Budget(
                month = currentMonthKey,
                monthlyBudget = monthlyBudget,
                publicBudget = publicBudget,
                privateBudget = privateBudget
            )
            val result = repository.saveBudget(budget)
            if (result.isSuccess) {
                autoBackup()
                _statusMessage.value = "Budget updated for $currentMonthKey"
                onDone(true)
            } else {
                _statusMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to save budget"
                onDone(false)
            }
        }
    }

    // --- Summary Calculations ---

    private fun buildMonthlyTrend(costList: List<Expense>): Map<String, Double> {
        val result = linkedMapOf<String, Double>()
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        // Rolling window: 6 months past to 3 months future (e.g. Nov 26, Dec 26, Jan 27, Feb 27)
        for (offset in -6..3) {
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.MONTH, offset)
            result[sdf.format(cal.time)] = 0.0
        }
        for (item in costList) {
            val m = item.date.take(7)
            if (m.length == 7 && !result.containsKey(m)) {
                result[m] = 0.0
            }
        }
        for (item in costList) {
            val m = item.date.take(7)
            if (m.length == 7) {
                result[m] = (result[m] ?: 0.0) + item.amount
            }
        }
        return result.toSortedMap()
    }

    private fun buildSavingsTrend(incomeList: List<Expense>, expenseList: List<Expense>): Map<String, Double> {
        val result = linkedMapOf<String, Double>()
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        for (i in 5 downTo 0) {
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.MONTH, -i)
            result[sdf.format(cal.time)] = 0.0
        }
        result[currentMonthKey] = result[currentMonthKey] ?: 0.0
        for (item in (incomeList + expenseList)) {
            val m = item.date.take(7)
            if (m.length == 7 && !result.containsKey(m)) {
                result[m] = 0.0
            }
        }
        for (item in incomeList) {
            val m = item.date.take(7)
            if (m.length == 7) {
                result[m] = (result[m] ?: 0.0) + item.amount
            }
        }
        for (item in expenseList) {
            val m = item.date.take(7)
            if (m.length == 7) {
                result[m] = (result[m] ?: 0.0) - item.amount
            }
        }
        return result.toSortedMap()
    }

    // Financial Summary (Reactive StateFlow with Cache Preservation)
    val financialSummary: StateFlow<FinancialSummary> = combine(
        publicExpensesState,
        privateExpenses,
        isPrivateUnlocked
    ) { publicState, privateList, isUnlocked ->
        val publicList = when (publicState) {
            is UiState.Success -> {
                lastPublicExpensesCache = publicState.data
                _isDataInitialized.value = true
                publicState.data
            }
            is UiState.Loading -> {
                lastPublicExpensesCache
            }
            is UiState.Error -> {
                _isDataInitialized.value = true
                lastPublicExpensesCache
            }
        }
        val effectivePrivateList = if (isUnlocked) {
            if (privateList.isNotEmpty()) {
                lastPrivateExpensesCache = privateList
                privateList
            } else {
                lastPrivateExpensesCache
            }
        } else {
            emptyList()
        }
        computeSummaryInternal(publicList, effectivePrivateList, isUnlocked)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = FinancialSummary()
    )

    private fun computeSummaryInternal(
        publicList: List<Expense>,
        privateList: List<Expense>,
        isUnlocked: Boolean
    ): FinancialSummary {
        val currentMonth = currentMonthKey
        val currentYear = currentMonth.take(4) // e.g. "2024"

        // Filter out transfers for income/expense calculations
        val actualPublicIncomeList = publicList.filter { it.isActualIncome }
        val actualPublicCostList = publicList.filter { it.isActualExpense }

        val actualPrivateIncomeList = if (isUnlocked) privateList.filter { it.isActualIncome } else emptyList()
        val actualPrivateCostList = if (isUnlocked) privateList.filter { it.isActualExpense } else emptyList()

        val publicTotalIncome = actualPublicIncomeList.sumOf { it.amount }
        val publicTotalCost = actualPublicCostList.sumOf { it.amount }
        val publicNetBalance = publicTotalIncome - publicTotalCost

        val currentMonthPubIncome = actualPublicIncomeList.filter { it.date.startsWith(currentMonth) }.sumOf { it.amount }
        val currentMonthPubCost = actualPublicCostList.filter { it.date.startsWith(currentMonth) }.sumOf { it.amount }
        val currentMonthPubBalance = currentMonthPubIncome - currentMonthPubCost

        val publicCatTotals = actualPublicCostList.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val publicTrend = buildMonthlyTrend(actualPublicCostList)
        val publicIncomeTrend = buildMonthlyTrend(actualPublicIncomeList)

        val savingsBreakdowns = buildSavingsBreakdowns(
            actualPublicIncomeList,
            actualPrivateIncomeList,
            actualPublicCostList,
            actualPrivateCostList
        )
        val combinedSavingsTrend = savingsBreakdowns.mapValues { it.value.savings }

        if (!isUnlocked) {
            return FinancialSummary(
                totalIncome = publicTotalIncome,
                totalCost = publicTotalCost,
                netBalance = publicNetBalance,
                currentMonthIncome = currentMonthPubIncome,
                currentMonthCost = currentMonthPubCost,
                currentMonthBalance = currentMonthPubBalance,
                totalPublicCost = publicTotalCost,
                totalPublicIncome = publicTotalIncome,
                currentMonthPublicCost = currentMonthPubCost,
                totalPrivateCost = 0.0,
                totalPrivateIncome = 0.0,
                currentMonthPrivateCost = 0.0,
                combinedCost = publicTotalCost,
                combinedIncome = publicTotalIncome,
                combinedCurrentMonthCost = currentMonthPubCost,
                publicCategoryTotals = publicCatTotals,
                publicMonthlyTrend = publicTrend,
                publicIncomeMonthlyTrend = publicIncomeTrend,
                combinedIncomeMonthlyTrend = publicIncomeTrend,
                combinedMonthlySavingsTrend = combinedSavingsTrend,
                monthlySavingsBreakdowns = savingsBreakdowns,
                currentYearCost = actualPublicCostList.filter { it.date.startsWith(currentYear) }.sumOf { it.amount }
            )
        }

        val privateTotalIncome = actualPrivateIncomeList.sumOf { it.amount }
        val privateTotalCost = actualPrivateCostList.sumOf { it.amount }
        val privateNetBalance = privateTotalIncome - privateTotalCost

        val currentMonthPrivIncome = actualPrivateIncomeList.filter { it.date.startsWith(currentMonth) }.sumOf { it.amount }
        val currentMonthPrivCost = actualPrivateCostList.filter { it.date.startsWith(currentMonth) }.sumOf { it.amount }
        val currentMonthPrivBalance = currentMonthPrivIncome - currentMonthPrivCost

        val privateCatTotals = actualPrivateCostList.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val privateTrend = buildMonthlyTrend(actualPrivateCostList)
        val privateIncomeTrend = buildMonthlyTrend(actualPrivateIncomeList)

        val combinedIncomeList = actualPublicIncomeList + actualPrivateIncomeList
        val combinedCostList = actualPublicCostList + actualPrivateCostList

        val combinedIncome = publicTotalIncome + privateTotalIncome
        val combinedCost = publicTotalCost + privateTotalCost
        val combinedNetBalance = combinedIncome - combinedCost

        val combinedMonthIncome = currentMonthPubIncome + currentMonthPrivIncome
        val combinedMonthCost = currentMonthPubCost + currentMonthPrivCost
        val combinedMonthBalance = combinedMonthIncome - combinedMonthCost

        val combinedCatTotals = combinedCostList.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val combinedTrend = buildMonthlyTrend(combinedCostList)
        val combinedIncomeTrend = buildMonthlyTrend(combinedIncomeList)

        val currentYearCost = combinedCostList.filter { it.date.startsWith(currentYear) }.sumOf { it.amount }

        return FinancialSummary(
            totalIncome = combinedIncome,
            totalCost = combinedCost,
            netBalance = combinedNetBalance,
            currentMonthIncome = combinedMonthIncome,
            currentMonthCost = combinedMonthCost,
            currentMonthBalance = combinedMonthBalance,
            totalPublicCost = publicTotalCost,
            totalPublicIncome = publicTotalIncome,
            currentMonthPublicCost = currentMonthPubCost,
            totalPrivateCost = privateTotalCost,
            totalPrivateIncome = privateTotalIncome,
            currentMonthPrivateCost = currentMonthPrivCost,
            combinedCost = combinedCost,
            combinedIncome = combinedIncome,
            combinedCurrentMonthCost = combinedMonthCost,
            publicCategoryTotals = publicCatTotals,
            privateCategoryTotals = privateCatTotals,
            combinedCategoryTotals = combinedCatTotals,
            publicMonthlyTrend = publicTrend,
            privateMonthlyTrend = privateTrend,
            combinedMonthlyTrend = combinedTrend,
            publicIncomeMonthlyTrend = publicIncomeTrend,
            privateIncomeMonthlyTrend = privateIncomeTrend,
            combinedIncomeMonthlyTrend = combinedIncomeTrend,
            combinedMonthlySavingsTrend = combinedSavingsTrend,
            monthlySavingsBreakdowns = savingsBreakdowns,
            currentYearCost = currentYearCost
        )
    }

    private fun buildSavingsBreakdowns(
        publicIncomeList: List<Expense>,
        privateIncomeList: List<Expense>,
        publicCostList: List<Expense>,
        privateCostList: List<Expense>
    ): Map<String, MonthlySavingsBreakdown> {
        val currentYear = currentMonthKey.take(4).toIntOrNull() ?: 2026
        val publicExpenses = publicIncomeList + publicCostList
        val privateExpenses = privateIncomeList + privateCostList

        val baseBreakdowns = FinancialCalculator.calculateMonthlyBreakdownsForYear(
            year = currentYear,
            publicExpenses = publicExpenses,
            privateExpenses = privateExpenses
        ).toMutableMap()

        val allItems = publicExpenses + privateExpenses
        for (item in allItems) {
            val m = item.date.take(7)
            if (m.length == 7 && !baseBreakdowns.containsKey(m)) {
                val parts = m.split("-")
                if (parts.size == 2) {
                    val yr = parts[0].toIntOrNull() ?: currentYear
                    val mo = parts[1].toIntOrNull() ?: 1
                    baseBreakdowns[m] = FinancialCalculator.calculateMonthlyBreakdown(yr, mo, publicExpenses, privateExpenses)
                }
            }
        }

        return baseBreakdowns.toSortedMap()
    }

    fun getCategoryTotalsForMonth(month: String, isPrivate: Boolean): Map<String, Double> {
        val list = if (isPrivate) _privateExpenses.value else (publicExpensesState.value as? UiState.Success)?.data ?: emptyList()
        return list.filter { it.date.startsWith(month) && !it.isIncome }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    private fun autoBackup() {
        viewModelScope.launch {
            _syncStatus.value = SyncStatus.SYNCING
            val result = repository.backupAllData(currentUserId)
            if (result.isSuccess) {
                _syncStatus.value = SyncStatus.SYNCED
            } else {
                _syncStatus.value = SyncStatus.OFFLINE
            }
        }
    }

    fun initBackupRepository(context: android.content.Context) {
        // No-op or silent init
    }
}
