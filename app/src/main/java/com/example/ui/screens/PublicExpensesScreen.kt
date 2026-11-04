package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Expense
import com.example.ui.components.ExpenseDialog
import com.example.ui.components.InvoiceDialog
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.PureWhite
import com.example.ui.util.CategoryIconBadge
import com.example.ui.util.CsvExportHelper
import com.example.ui.util.CurrencyHelper
import com.example.ui.util.ResponsiveModal
import com.example.ui.viewmodel.ExpenseViewModel
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicExpensesScreen(
    viewModel: ExpenseViewModel
) {
    val context = LocalContext.current
    val publicState by viewModel.publicExpensesState.collectAsState()
    val privateList by viewModel.privateExpenses.collectAsState()
    val isVaultActive by viewModel.isPrivateUnlocked.collectAsState()
    val customCategories by viewModel.customCategories.collectAsState()
    val bankAccounts by viewModel.bankAccounts.collectAsState()
    val currency by viewModel.selectedCurrency.collectAsState()
    val currentBudget by viewModel.currentBudget.collectAsState()
    val currencySymbol = currency.symbol

    var vaultSubTab by remember { mutableStateOf(0) } // 0: Transactions, 1: Monthly Savings & Banks
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }
    var invoiceExpense by remember { mutableStateOf<Expense?>(null) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }

    val rawList = if (isVaultActive) {
        privateList
    } else {
        when (val s = publicState) {
            is UiState.Success -> s.data
            else -> emptyList()
        }
    }

    // Extract dynamic categories present in data + custom
    val allFilterCategories = remember(rawList, customCategories) {
        val cats = rawList.map { it.category }.distinct()
        listOf("All") + cats
    }

    val filteredList = rawList.filter { expense ->
        val matchesCategory = selectedCategory == "All" || expense.category.equals(selectedCategory, ignoreCase = true)
        val matchesQuery = searchQuery.isBlank() ||
                expense.title.contains(searchQuery, ignoreCase = true) ||
                expense.category.contains(searchQuery, ignoreCase = true) ||
                (expense.notes ?: "").contains(searchQuery, ignoreCase = true) ||
                expense.creatorName.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesQuery
    }.sortedByDescending { it.date }

    val totalExpensesSum = filteredList.filter { !it.isIncome }.sumOf { it.amount }
    val totalIncomeSum = filteredList.filter { it.isIncome }.sumOf { it.amount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isVaultActive) "Transactions" else "Public Expenses",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (isVaultActive) "Personal Vault Mode" else "Household Log",
                            style = MaterialTheme.typography.labelSmall.copy(color = MutedSlate)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = if (isVaultActive) AccentGold else AccentBlue,
                contentColor = if (isVaultActive) DeepNavy else PureWhite,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_expense_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Transaction",
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("public_expenses_screen")
        ) {
            // In Vault mode, show switcher between Transactions and Monthly Savings & Banks
            if (isVaultActive) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = vaultSubTab == 0,
                        onClick = { vaultSubTab = 0 },
                        label = { Text("Transactions", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentGold,
                            selectedLabelColor = DeepNavy
                        ),
                        modifier = Modifier.weight(1f).testTag("vault_tab_transactions")
                    )
                    FilterChip(
                        selected = vaultSubTab == 1,
                        onClick = { vaultSubTab = 1 },
                        label = { Text("Monthly Savings", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentGold,
                            selectedLabelColor = DeepNavy
                        ),
                        modifier = Modifier.weight(1f).testTag("vault_tab_savings")
                    )
                }
            }

            if (isVaultActive && vaultSubTab == 1) {
                SavingsScreen(viewModel = viewModel)
            } else {
                // Search Input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("expense_search_field"),
                    placeholder = { Text("Search by title, category, or notes...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = MutedSlate)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )


            // Category Filter Chips with Category Icons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                allFilterCategories.forEach { cat ->
                    val isSelected = cat == selectedCategory
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (cat != "All") {
                                    CategoryIconBadge(
                                        category = cat,
                                        isIncome = cat.contains("Income", ignoreCase = true),
                                        size = 20.dp,
                                        iconSize = 12.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = cat,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (isVaultActive) AccentGold else AccentBlue,
                            selectedLabelColor = if (isVaultActive) DeepNavy else PureWhite
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }

            // Summary Strip for filtered results
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${filteredList.size} items",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, color = MutedSlate)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (isVaultActive && totalIncomeSum > 0) {
                            Text(
                                text = "In: +${CurrencyHelper.format(totalIncomeSum, currencySymbol)}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = AccentGreen
                                )
                            )
                        } else if (!isVaultActive) {
                            val pubBudget = currentBudget?.publicBudget?.takeIf { it > 0 } ?: currentBudget?.monthlyBudget ?: 0.0
                            if (pubBudget > 0) {
                                val rem = pubBudget - totalExpensesSum
                                Text(
                                    text = "Budget Left: ${CurrencyHelper.format(rem, currencySymbol)}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (rem >= 0) AccentGreen else AccentRed
                                    )
                                )
                            }
                        }
                        Text(
                            text = "Cost: ${CurrencyHelper.format(totalExpensesSum, currencySymbol)}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }

            // Transaction List
            if (rawList.isEmpty() && publicState is UiState.Loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentBlue)
                }
            } else if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = MutedSlate,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank() || selectedCategory != "All") "No matching transactions found" else "No transactions logged yet",
                            style = MaterialTheme.typography.titleMedium.copy(color = MutedSlate, fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap the '+' button below to add your first entry",
                            style = MaterialTheme.typography.bodySmall.copy(color = MutedSlate)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList, key = { it.id }) { expense ->
                        TransactionItemCard(
                            expense = expense,
                            currentUserId = viewModel.currentUserId,
                            currencySymbol = currency.symbol,
                            onClick = { invoiceExpense = expense },
                            onEdit = { editingExpense = expense },
                            onDeleteClick = { expenseToDelete = expense }
                        )
                    }
                }
            }
            }
        }
    }

    // Add Transaction Dialog
    if (showAddDialog) {
        ExpenseDialog(
            isPrivate = isVaultActive,
            customCategories = customCategories,
            bankAccounts = bankAccounts,
            onDismiss = { showAddDialog = false },
            onSave = { title, amount, category, date, paymentMethod, notes, transactionType ->
                if (isVaultActive) {
                    viewModel.addPrivateExpense(
                        title = title,
                        amount = amount,
                        category = category,
                        date = date,
                        paymentMethod = paymentMethod,
                        notes = notes,
                        transactionType = transactionType
                    )
                } else {
                    viewModel.addPublicExpense(
                        title = title,
                        amount = amount,
                        category = category,
                        date = date,
                        paymentMethod = paymentMethod,
                        notes = notes,
                        transactionType = transactionType
                    )
                }
                showAddDialog = false
            },
            onCreateCategory = { newCategory ->
                viewModel.addCustomCategory(newCategory)
            }
        )
    }

    // Edit Transaction Dialog
    if (editingExpense != null) {
        ExpenseDialog(
            initialExpense = editingExpense,
            isPrivate = isVaultActive,
            customCategories = customCategories,
            bankAccounts = bankAccounts,
            onDismiss = { editingExpense = null },
            onSave = { title, amount, category, date, paymentMethod, notes, transactionType ->
                if (isVaultActive) {
                    viewModel.updatePrivateExpense(
                        id = editingExpense!!.id,
                        title = title,
                        amount = amount,
                        category = category,
                        date = date,
                        paymentMethod = paymentMethod,
                        notes = notes,
                        transactionType = transactionType
                    )
                } else {
                    viewModel.updatePublicExpense(
                        id = editingExpense!!.id,
                        title = title,
                        amount = amount,
                        category = category,
                        date = date,
                        paymentMethod = paymentMethod,
                        notes = notes,
                        transactionType = transactionType
                    )
                }
                editingExpense = null
            },
            onCreateCategory = { newCategory ->
                viewModel.addCustomCategory(newCategory)
            }
        )
    }


    // Invoice Voucher Dialog
    if (invoiceExpense != null) {
        InvoiceDialog(
            expense = invoiceExpense!!,
            currencySymbol = currency.symbol,
            onDismiss = { invoiceExpense = null }
        )
    }

    // Safe Delete Confirmation Dialog (Requirement 2)
    if (expenseToDelete != null) {
        val target = expenseToDelete!!
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            modifier = ResponsiveModal.modalModifier(isSmall = true),
            properties = ResponsiveModal.dialogProperties,
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = AccentRed,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Delete Transaction",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    Text(
                        text = "Are you sure you want to delete this expense?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "\"${target.title}\" (${CurrencyHelper.format(target.amount, currencySymbol)})",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = target.id
                        expenseToDelete = null
                        if (isVaultActive) {
                            viewModel.deletePrivateExpense(id)
                        } else {
                            viewModel.deletePublicExpense(id)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("Delete", color = PureWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { expenseToDelete = null },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

/**
 * Transaction item card with related icon on left side, amount, and safe edit/delete actions
 */
@Composable
fun TransactionItemCard(
    expense: Expense,
    currentUserId: String,
    currencySymbol: String,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isIncome = expense.isIncome
    val isOwner = expense.userId.isBlank() || expense.userId == currentUserId

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("transaction_item_${expense.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category icon on the left side!
                    CategoryIconBadge(
                        category = expense.category,
                        isIncome = isIncome,
                        size = 42.dp,
                        iconSize = 22.dp
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = expense.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${expense.category} • ${expense.date} • ${expense.paymentMethod}",
                            style = MaterialTheme.typography.bodySmall.copy(color = MutedSlate, fontSize = 12.sp)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${if (isIncome) "+" else "-"}${CurrencyHelper.format(expense.amount, currencySymbol)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isIncome) AccentGreen else MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "Receipt ↗",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = AccentBlue,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            if (!expense.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ) {
                    Text(
                        text = expense.notes,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action row: edit, delete (safe dialog trigger), and author label
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isOwner) "You" else "By: ${expense.creatorName}",
                    style = MaterialTheme.typography.labelSmall.copy(color = MutedSlate)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = AccentBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = AccentRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
