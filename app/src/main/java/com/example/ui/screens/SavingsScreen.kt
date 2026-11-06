package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BankAccount
import com.example.data.model.Expense
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.LightNavy
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.PureWhite
import com.example.ui.util.CurrencyHelper
import com.example.ui.util.FinancialInvoiceHelper
import com.example.ui.util.ResponsiveModal
import com.example.ui.viewmodel.ExpenseViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun SavingsScreen(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier,
    showInlineHistory: Boolean = false
) {
    val context = LocalContext.current
    val user = Firebase.auth.currentUser
    val bankAccounts by viewModel.bankAccounts.collectAsState()
    val currency by viewModel.selectedCurrency.collectAsState()
    val userProfileName by viewModel.userProfileName.collectAsState()
    val currencySymbol = currency.symbol

    val privateList by viewModel.privateExpenses.collectAsState()
    val publicState by viewModel.publicExpensesState.collectAsState()
    val isVaultActive by viewModel.isPrivateUnlocked.collectAsState()

    var showDepositDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<BankAccount?>(null) }
    var accountToDelete by remember { mutableStateOf<BankAccount?>(null) }
    var targetDepositBank by remember { mutableStateOf<String?>(null) }
    var selectedHistoryBank by remember { mutableStateOf("All Accounts") }
    var selectedHistoryType by remember { mutableStateOf("All") }

    val totalSavings = bankAccounts.sumOf { it.balance }

    val publicList = when (val s = publicState) {
        is UiState.Success -> s.data
        else -> emptyList()
    }

    val allTransactions = if (isVaultActive) {
        (privateList + publicList).distinctBy { it.id }
    } else {
        publicList
    }

    val bankHistoryList = remember(allTransactions, selectedHistoryBank, selectedHistoryType) {
        allTransactions.filter { exp ->
            val bankMatches = if (selectedHistoryBank == "All Accounts") {
                true
            } else {
                exp.paymentMethod.contains(selectedHistoryBank, ignoreCase = true) ||
                        selectedHistoryBank.contains(exp.paymentMethod, ignoreCase = true)
            }
            
            val typeMatches = when (selectedHistoryType) {
                "Deposits" -> exp.isActualIncome
                "Withdrawals" -> exp.isActualExpense
                "Transfers" -> exp.isTransfer
                else -> true
            }
            bankMatches && typeMatches
        }.sortedByDescending { it.date }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("savings_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Savings Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("total_savings_hero_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                DeepNavy,
                                Color(0xFF0D274D),
                                Color(0xFF143B66)
                            )
                        )
                    )
                    .padding(22.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = AccentGold.copy(alpha = 0.2f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Savings,
                                        contentDescription = null,
                                        tint = AccentGold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Total Monthly Savings",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = PureWhite.copy(alpha = 0.85f),
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AccentGreen.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "${bankAccounts.size} Accounts",
                                color = AccentGreen,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = CurrencyHelper.format(totalSavings, currencySymbol),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = PureWhite,
                            fontSize = 32.sp
                        )
                    )

                    Text(
                        text = "Secured across Janata, Meghna, DBBL, bKash & Cash",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = PureWhite.copy(alpha = 0.65f)
                        )
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Action Buttons: Quick Deposit, Transfer, Add Account
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                targetDepositBank = null
                                showDepositDialog = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_deposit_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Deposit", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, softWrap = false)
                        }

                        Button(
                            onClick = { showTransferDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("transfer_banks_button")
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Transfer", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, softWrap = false)
                        }

                        OutlinedButton(
                            onClick = { showAddAccountDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+ New", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        }

        // Section Title: My Bank Balances
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Bank & Wallet Balances",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Button(
                onClick = {
                    FinancialInvoiceHelper.printInvoice(
                        context = context,
                        transactions = allTransactions,
                        title = "Bank Savings Statement",
                        periodLabel = "All Accounts Statement",
                        userName = userProfileName,
                        userEmail = user?.email ?: "",
                        currencySymbol = currencySymbol
                    )
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.testTag("print_bank_statement_button")
            ) {
                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Statement", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Bank Account Cards
        bankAccounts.forEach { account ->
            BankAccountCard(
                account = account,
                currencySymbol = currencySymbol,
                onEdit = { editingAccount = account },
                onDelete = { accountToDelete = account },
                onDeposit = {
                    targetDepositBank = account.name
                    showDepositDialog = true
                }
            )
        }

        if (showInlineHistory) {
            // Section Title: History of Deposits & Withdrawals
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = AccentGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Deposits & Withdrawals History",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Button(
                    onClick = {
                        FinancialInvoiceHelper.printInvoice(
                            context = context,
                            transactions = bankHistoryList,
                            title = "Bank Savings Statement",
                            periodLabel = "Account: $selectedHistoryBank",
                            userName = userProfileName,
                            userEmail = user?.email ?: "",
                            currencySymbol = currencySymbol
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("print_savings_history_button")
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Print Statement", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Account Filter Chips for History
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val historyFilterBanks = remember(bankAccounts) {
                    listOf("All Accounts") + bankAccounts.map { it.name }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    historyFilterBanks.forEach { bName ->
                        val isSel = bName == selectedHistoryBank
                        FilterChip(
                            selected = isSel,
                            onClick = { selectedHistoryBank = bName },
                            label = { Text(bName, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentGold,
                                selectedLabelColor = DeepNavy
                            )
                        )
                    }
                }

                // Transaction Type Filter Chips
                val types = listOf("All", "Deposits", "Withdrawals", "Transfers")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    types.forEach { type ->
                        val isSel = type == selectedHistoryType
                        FilterChip(
                            selected = isSel,
                            onClick = { selectedHistoryType = type },
                            label = { Text(type, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentBlue,
                                selectedLabelColor = PureWhite
                            )
                        )
                    }
                }
            }

            // History List Items
            if (bankHistoryList.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text(
                        text = "No deposits or withdrawals recorded yet for $selectedHistoryBank.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MutedSlate),
                        modifier = Modifier.padding(20.dp)
                    )
                }
            } else {
                bankHistoryList.forEach { expense ->
                    SavingsHistoryItemCard(
                        expense = expense,
                        currencySymbol = currencySymbol
                    )
                }
            }
        }
    }

    // Deposit Dialog
    if (showDepositDialog) {
        DepositFundsDialog(
            bankAccounts = bankAccounts,
            preselectedBank = targetDepositBank,
            currencySymbol = currencySymbol,
            onDismiss = { showDepositDialog = false },
            onDeposit = { bankName, amount, note ->
                viewModel.depositToBank(bankName, amount, note)
                showDepositDialog = false
            }
        )
    }

    // Transfer Dialog
    if (showTransferDialog) {
        TransferFundsDialog(
            bankAccounts = bankAccounts,
            currencySymbol = currencySymbol,
            onDismiss = { showTransferDialog = false },
            onTransfer = { from, to, amount ->
                viewModel.transferBetweenBanks(from, to, amount)
                showTransferDialog = false
            }
        )
    }

    // Edit Account Dialog
    editingAccount?.let { acc ->
        EditAccountDialog(
            account = acc,
            onDismiss = { editingAccount = null },
            onSave = { updated ->
                viewModel.saveBankAccount(updated)
                editingAccount = null
            }
        )
    }

    // Add New Account Dialog
    if (showAddAccountDialog) {
        AddAccountDialog(
            onDismiss = { showAddAccountDialog = false },
            onAdd = { newAcc ->
                viewModel.saveBankAccount(newAcc)
                showAddAccountDialog = false
            }
        )
    }

    // Delete Account Confirmation Dialog
    accountToDelete?.let { acc ->
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            modifier = ResponsiveModal.modalModifier(isSmall = true),
            properties = ResponsiveModal.dialogProperties,
            title = { Text("Delete Account", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete ${acc.name}? All transaction history for this account will remain, but this account will no longer be available for new transactions.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBankAccount(acc.id)
                        accountToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { accountToDelete = null }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun BankAccountCard(
    account: BankAccount,
    currencySymbol: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDeposit: () -> Unit
) {
    val (icon, tint) = when {
        account.name.contains("bKash", ignoreCase = true) -> Icons.Default.PhoneAndroid to Color(0xFFE91E63)
        account.name.contains("Nagad", ignoreCase = true) -> Icons.Default.PhoneAndroid to Color(0xFFFF9800)
        account.name.contains("Cash", ignoreCase = true) -> Icons.Default.Money to AccentGreen
        account.name.contains("Meghna", ignoreCase = true) -> Icons.Default.AccountBalance to Color(0xFF009688)
        account.name.contains("Janata", ignoreCase = true) -> Icons.Default.AccountBalance to AccentBlue
        account.name.contains("Dutch", ignoreCase = true) || account.name.contains("DBBL", ignoreCase = true) -> Icons.Default.AccountBalance to Color(0xFFE53935)
        else -> Icons.Default.AccountBalance to AccentCyan
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bank_card_${account.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = tint.copy(alpha = 0.15f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = if (account.accountNumber.isNotBlank()) "${account.accountType} • ${account.accountNumber}" else account.accountType,
                    style = MaterialTheme.typography.labelSmall.copy(color = MutedSlate),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = CurrencyHelper.format(account.balance, currencySymbol),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = if (account.balance >= 0) AccentGreen else AccentRed
                    )
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp).testTag("edit_bank_${account.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit balance",
                            tint = MutedSlate,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp).testTag("delete_bank_${account.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete account",
                            tint = AccentRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AccentGreen.copy(alpha = 0.12f),
                        modifier = Modifier.clickable { onDeposit() }
                    ) {
                        Text(
                            text = "+ Save",
                            color = AccentGreen,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SavingsHistoryItemCard(
    expense: Expense,
    currencySymbol: String
) {
    val isIncome = expense.isIncome
    val isTransfer = expense.isTransfer
    
    val (label, tint, icon) = when {
        isTransfer -> Triple("TRANSFER", AccentBlue, Icons.Default.SwapHoriz)
        isIncome -> Triple("DEPOSIT", AccentGreen, Icons.Default.ArrowDownward)
        else -> Triple("WITHDRAWAL", AccentRed, Icons.Default.ArrowUpward)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("savings_history_item_${expense.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = tint.copy(alpha = 0.15f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = expense.title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${expense.date} • ${expense.category}",
                        style = MaterialTheme.typography.labelSmall.copy(color = MutedSlate)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = expense.paymentMethod,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = AccentBlue,
                                fontSize = 12.sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isTransfer) "" else if (isIncome) "+" else "-"}${CurrencyHelper.format(expense.amount, currencySymbol)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isTransfer) AccentBlue else if (isIncome) AccentGreen else AccentRed
                    )
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = tint.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = tint,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepositFundsDialog(
    bankAccounts: List<BankAccount>,
    preselectedBank: String?,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onDeposit: (bankName: String, amount: Double, note: String?) -> Unit
) {
    var selectedBank by remember {
        mutableStateOf(preselectedBank ?: bankAccounts.firstOrNull()?.name ?: "Janata Bank")
    }
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = ResponsiveModal.modalModifier(),
        properties = ResponsiveModal.dialogProperties,
        title = { Text("Deposit to Savings Account", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Bank Selector
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedBank,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Target Bank / Account") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        bankAccounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text("${acc.name} (Balance: ${CurrencyHelper.format(acc.balance, currencySymbol)})") },
                                onClick = {
                                    selectedBank = acc.name
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Deposit Amount (${CurrencyHelper.currentCurrency.symbol})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("deposit_amount_input")
                )

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Note / Description (Optional)") },
                    placeholder = { Text("e.g. Monthly salary savings deposit") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0 && selectedBank.isNotBlank()) {
                        onDeposit(selectedBank, amt, noteText.ifBlank { null })
                    }
                },
                modifier = Modifier.testTag("confirm_deposit_button")
            ) {
                Text("Confirm Deposit")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferFundsDialog(
    bankAccounts: List<BankAccount>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onTransfer: (fromBank: String, toBank: String, amount: Double) -> Unit
) {
    var fromBank by remember { mutableStateOf(bankAccounts.firstOrNull()?.name ?: "Janata Bank") }
    var toBank by remember { mutableStateOf(bankAccounts.getOrNull(1)?.name ?: "bKash") }
    var amountText by remember { mutableStateOf("") }
    var fromExpanded by remember { mutableStateOf(false) }
    var toExpanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = ResponsiveModal.modalModifier(),
        properties = ResponsiveModal.dialogProperties,
        title = { Text("Transfer Between Accounts", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // From
                ExposedDropdownMenuBox(
                    expanded = fromExpanded,
                    onExpandedChange = { fromExpanded = !fromExpanded }
                ) {
                    OutlinedTextField(
                        value = "From: $fromBank",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = fromExpanded,
                        onDismissRequest = { fromExpanded = false }
                    ) {
                        bankAccounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text("${acc.name} (${CurrencyHelper.format(acc.balance, currencySymbol)})") },
                                onClick = {
                                    fromBank = acc.name
                                    fromExpanded = false
                                }
                            )
                        }
                    }
                }

                // To
                ExposedDropdownMenuBox(
                    expanded = toExpanded,
                    onExpandedChange = { toExpanded = !toExpanded }
                ) {
                    OutlinedTextField(
                        value = "To: $toBank",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = toExpanded,
                        onDismissRequest = { toExpanded = false }
                    ) {
                        bankAccounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text(acc.name) },
                                onClick = {
                                    toBank = acc.name
                                    toExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        error = null
                    },
                    label = { Text("Transfer Amount (${CurrencyHelper.currentCurrency.symbol})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("transfer_amount_input")
                )

                error?.let {
                    Text(it, color = AccentRed, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt <= 0) {
                        error = "Enter a valid amount"
                        return@Button
                    }
                    if (fromBank == toBank) {
                        error = "Sender and receiver bank cannot be the same"
                        return@Button
                    }
                    onTransfer(fromBank, toBank, amt)
                },
                modifier = Modifier.testTag("confirm_transfer_button")
            ) {
                Text("Execute Transfer")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun EditAccountDialog(
    account: BankAccount,
    onDismiss: () -> Unit,
    onSave: (BankAccount) -> Unit
) {
    var name by remember { mutableStateOf(account.name) }
    var accountNumber by remember { mutableStateOf(account.accountNumber) }
    var balanceText by remember { mutableStateOf(account.balance.toString()) }
    var accountType by remember { mutableStateOf(account.accountType) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = ResponsiveModal.modalModifier(),
        properties = ResponsiveModal.dialogProperties,
        title = { Text("Edit Bank / Wallet Balance", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it },
                    label = { Text("Current Balance (${CurrencyHelper.currentCurrency.symbol})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("edit_balance_input")
                )

                OutlinedTextField(
                    value = accountNumber,
                    onValueChange = { accountNumber = it },
                    label = { Text("Account Number / Card (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = accountType,
                    onValueChange = { accountType = it },
                    label = { Text("Type (e.g. Savings, Mobile Wallet, Cash)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bal = balanceText.toDoubleOrNull() ?: account.balance
                    onSave(
                        account.copy(
                            name = name.trim(),
                            balance = bal,
                            accountNumber = accountNumber.trim(),
                            accountType = accountType.trim()
                        )
                    )
                },
                modifier = Modifier.testTag("save_bank_edit_button")
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun AddAccountDialog(
    onDismiss: () -> Unit,
    onAdd: (BankAccount) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var balanceText by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var accountType by remember { mutableStateOf("Savings") }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = ResponsiveModal.modalModifier(),
        properties = ResponsiveModal.dialogProperties,
        title = { Text("Add Bank or Mobile Wallet", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Bank / Wallet Name") },
                    placeholder = { Text("e.g. City Bank, Nagad, Pubali Bank") },
                    modifier = Modifier.fillMaxWidth().testTag("add_bank_name_input")
                )

                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it },
                    label = { Text("Starting Balance (${CurrencyHelper.currentCurrency.symbol})") },
                    placeholder = { Text("e.g. 25000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("add_bank_balance_input")
                )

                OutlinedTextField(
                    value = accountNumber,
                    onValueChange = { accountNumber = it },
                    label = { Text("Account Number (Optional)") },
                    placeholder = { Text("e.g. •••• 9876") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = accountType,
                    onValueChange = { accountType = it },
                    label = { Text("Account Type") },
                    placeholder = { Text("Savings, Current, Mobile Wallet") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bal = balanceText.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank()) {
                        val icon = if (name.contains("bKash", true) || name.contains("Nagad", true)) "mobile" else "bank"
                        onAdd(
                            BankAccount(
                                name = name.trim(),
                                balance = bal,
                                initialBalance = bal,
                                accountNumber = accountNumber.trim(),
                                accountType = accountType.trim(),
                                iconName = icon
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("confirm_add_bank_button")
            ) {
                Text("Add Account")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
