package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BankAccount
import com.example.data.model.Budget
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategories
import com.example.data.model.UserProfile
import com.example.data.model.UserSecurity
import com.example.data.repository.ExpenseRepository
import com.example.data.security.PinSecurityManager
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
    val combinedMonthlyTrend: Map<String, Double> = emptyMap()
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

    // Public Expenses state
    val publicExpensesState: StateFlow<UiState<List<Expense>>> = repository.observePublicExpenses()
        .map<List<Expense>, UiState<List<Expense>>> { UiState.Success(it) }
        .catch { emit(UiState.Error(it.message ?: "Failed to load public expenses")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
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
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = null
        )

    // Selected currency
    private val _selectedCurrency = MutableStateFlow(CurrencyHelper.currentCurrency)
    val selectedCurrency: StateFlow<CurrencyHelper.Currency> = _selectedCurrency.asStateFlow()

    // Bank Accounts & Monthly Savings (Meghna, Janata, Dutch Bangla, bKash, etc.)
    val bankAccounts: StateFlow<List<BankAccount>> = repository.observeBankAccounts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = ExpenseCategories.defaultBankAccounts
        )

    // Status messages for Snackbars/Alerts
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // User Profile state
    val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = null
        )

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
            val res = repository.saveBankAccount(account)
            if (res.isSuccess) {
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
                            transactionType = "expense",
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
                            transactionType = "income",
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
                            transactionType = "expense",
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
                            transactionType = "income",
                            expenseType = "public"
                        )
                    )
                }
                _statusMessage.value = "Transferred ${CurrencyHelper.format(amount, selectedCurrency.value.symbol)} successfully"
                onDone(true)
            } else {
                _statusMessage.value = "Transfer failed"
                onDone(false)
            }
        }
    }


    // --- Budget Operations ---

    fun saveBudget(monthlyBudget: Double, publicBudget: Double, privateBudget: Double, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val budget = Budget(
                month = currentMonthKey,
                monthlyBudget = monthlyBudget,
                publicBudget = publicBudget,
                privateBudget = privateBudget
            )
            val result = repository.saveBudget(budget)
            if (result.isSuccess) {
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
        for (i in 5 downTo 0) {
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.MONTH, -i)
            result[sdf.format(cal.time)] = 0.0
        }
        for (item in costList) {
            val m = item.date.take(7)
            if (m.length == 7) {
                result[m] = (result[m] ?: 0.0) + item.amount
            }
        }
        return result.toSortedMap()
    }

    // Financial Summary (Reactive StateFlow)
    val financialSummary: StateFlow<FinancialSummary> = combine(
        publicExpensesState,
        privateExpenses,
        isPrivateUnlocked
    ) { publicState, privateList, isUnlocked ->
        val publicList = (publicState as? UiState.Success)?.data ?: emptyList()
        computeSummaryInternal(publicList, privateList, isUnlocked)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = FinancialSummary()
    )

    private fun computeSummaryInternal(
        publicList: List<Expense>,
        privateList: List<Expense>,
        isUnlocked: Boolean
    ): FinancialSummary {
        val currentMonth = currentMonthKey

        val publicIncomeList = publicList.filter { it.isIncome }
        val publicCostList = publicList.filter { !it.isIncome }

        val publicTotalIncome = publicIncomeList.sumOf { it.amount }
        val publicTotalCost = publicCostList.sumOf { it.amount }
        val publicNetBalance = publicTotalIncome - publicTotalCost

        val currentMonthPubIncome = publicIncomeList.filter { it.date.startsWith(currentMonth) }.sumOf { it.amount }
        val currentMonthPubCost = publicCostList.filter { it.date.startsWith(currentMonth) }.sumOf { it.amount }
        val currentMonthPubBalance = currentMonthPubIncome - currentMonthPubCost

        val publicCatTotals = publicCostList.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val publicTrend = buildMonthlyTrend(publicCostList)

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
                publicMonthlyTrend = publicTrend
            )
        }

        val privateIncomeList = privateList.filter { it.isIncome }
        val privateCostList = privateList.filter { !it.isIncome }

        val privateTotalIncome = privateIncomeList.sumOf { it.amount }
        val privateTotalCost = privateCostList.sumOf { it.amount }
        val privateNetBalance = privateTotalIncome - privateTotalCost

        val currentMonthPrivIncome = privateIncomeList.filter { it.date.startsWith(currentMonth) }.sumOf { it.amount }
        val currentMonthPrivCost = privateCostList.filter { it.date.startsWith(currentMonth) }.sumOf { it.amount }
        val currentMonthPrivBalance = currentMonthPrivIncome - currentMonthPrivCost

        val privateCatTotals = privateCostList.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val privateTrend = buildMonthlyTrend(privateCostList)

        val combinedIncome = publicTotalIncome + privateTotalIncome
        val combinedCost = publicTotalCost + privateTotalCost
        val combinedNetBalance = combinedIncome - combinedCost

        val combinedMonthIncome = currentMonthPubIncome + currentMonthPrivIncome
        val combinedMonthCost = currentMonthPubCost + currentMonthPrivCost
        val combinedMonthBalance = combinedMonthIncome - combinedMonthCost

        val allCostExpenses = publicCostList + privateCostList
        val combinedCatTotals = allCostExpenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val combinedTrend = buildMonthlyTrend(allCostExpenses)

        return FinancialSummary(
            totalIncome = privateTotalIncome + publicTotalIncome,
            totalCost = privateTotalCost + publicTotalCost,
            netBalance = privateNetBalance + publicNetBalance,
            currentMonthIncome = currentMonthPrivIncome + currentMonthPubIncome,
            currentMonthCost = currentMonthPrivCost + currentMonthPubCost,
            currentMonthBalance = currentMonthPrivBalance + currentMonthPubBalance,
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
            combinedMonthlyTrend = combinedTrend
        )
    }

    fun getCategoryTotalsForMonth(month: String, isPrivate: Boolean): Map<String, Double> {
        val list = if (isPrivate) _privateExpenses.value else (publicExpensesState.value as? UiState.Success)?.data ?: emptyList()
        return list.filter { it.date.startsWith(month) && !it.isIncome }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }
}
