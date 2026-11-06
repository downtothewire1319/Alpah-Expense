package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategories
import com.example.ui.components.ExpenseDialog
import com.example.ui.components.PinKeypad
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentRed
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.LightNavy
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.NavyCard
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftWhite
import com.example.ui.util.CsvExportHelper
import com.example.ui.util.CurrencyHelper
import com.example.ui.viewmodel.ExpenseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivateExpensesScreen(
    viewModel: ExpenseViewModel
) {
    val context = LocalContext.current
    val isUnlocked by viewModel.isPrivateUnlocked.collectAsState()
    val isPinConfigured by viewModel.isPinConfigured.collectAsState()
    val privateList by viewModel.privateExpenses.collectAsState()
    val isLoading by viewModel.privateExpensesLoading.collectAsState()
    val bankAccounts by viewModel.bankAccounts.collectAsState()
    val currency by viewModel.selectedCurrency.collectAsState()

    var pinError by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }

    if (!isUnlocked) {
        // Locked Vault State - PIN keypad
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .testTag("private_vault_locked_view"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (!isPinConfigured) {
                    // Initial PIN setup prompt
                    PinKeypad(
                        title = "Create Private Vault PIN",
                        subtitle = "Choose a 4-digit PIN to protect your private expenses",
                        maxPinLength = 4,
                        errorMessage = pinError,
                        onPinComplete = { enteredPin ->
                            pinError = null
                            viewModel.setupNewPin(enteredPin) { success, err ->
                                if (!success) {
                                    pinError = err
                                }
                            }
                        }
                    )
                } else {
                    // Unlock with existing PIN
                    PinKeypad(
                        title = "Enter Vault PIN",
                        subtitle = "Enter your PIN to view & manage private expenses",
                        maxPinLength = 4,
                        errorMessage = pinError,
                        onPinComplete = { enteredPin ->
                            pinError = null
                            viewModel.verifyAndUnlockPin(enteredPin) { success, err ->
                                if (!success) {
                                    pinError = err
                                }
                            }
                        }
                    )
                }
            }
        }
        return
    }

    // Unlocked State - Full Private Management (Exclude transfers)
    val filteredList = privateList.filter { expense ->
        if (expense.isTransfer) return@filter false
        val matchesCategory = selectedCategory == "All" || expense.category.equals(selectedCategory, ignoreCase = true)
        val matchesQuery = searchQuery.isBlank() ||
                expense.title.contains(searchQuery, ignoreCase = true) ||
                expense.category.contains(searchQuery, ignoreCase = true) ||
                (expense.notes ?: "").contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesQuery
    }

    val totalPrivateAmount = filteredList.filter { it.isActualExpense }.sumOf { it.amount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = AccentGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Private Vault",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                actions = {

                    // Lock button
                    Button(
                        onClick = { viewModel.lockPrivateSection() },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("lock_vault_button")
                    ) {
                        Text(
                            text = "Lock",
                            color = AccentGold,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = AccentGold,
                contentColor = DeepNavy,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_private_expense_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Private Expense")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("private_expenses_screen")
        ) {
            // Security badge
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = AccentGold.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = AccentGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Encrypted & isolated at backend level • Not visible to other users",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = AccentGold,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("private_expense_search_input"),
                placeholder = { Text("Search private transactions...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = MutedSlate)
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            // Category Chips (Private categories)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val allCats = listOf("All") + ExpenseCategories.privateCategories
                allCats.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentGold,
                            selectedLabelColor = DeepNavy
                        )
                    )
                }
            }

            // Summary Stats Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${filteredList.size} private entries",
                        style = MaterialTheme.typography.bodySmall.copy(color = MutedSlate)
                    )
                    Text(
                        text = "Total: ${CurrencyHelper.format(totalPrivateAmount, currency.symbol)}",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = AccentGold
                        )
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentGold)
                }
            } else if (filteredList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MutedSlate,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isNotBlank() || selectedCategory != "All") "No matching private expenses" else "No private expenses logged yet. Tap + to add!",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MutedSlate),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredList, key = { it.id }) { expense ->
                        PrivateExpenseCard(
                            expense = expense,
                            currencySymbol = currency.symbol,
                            onEdit = { editingExpense = expense },
                            onDelete = { viewModel.deletePrivateExpense(expense.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        ExpenseDialog(
            isPrivate = true,
            bankAccounts = bankAccounts,
            onDismiss = { showAddDialog = false },
            onSave = { title, amount, category, date, paymentMethod, notes, transactionType ->
                viewModel.addPrivateExpense(
                    title = title,
                    amount = amount,
                    category = category,
                    date = date,
                    paymentMethod = paymentMethod,
                    notes = notes,
                    transactionType = transactionType
                )
                showAddDialog = false
            }
        )
    }

    if (editingExpense != null) {
        ExpenseDialog(
            initialExpense = editingExpense,
            isPrivate = true,
            bankAccounts = bankAccounts,
            onDismiss = { editingExpense = null },
            onSave = { title, amount, category, date, paymentMethod, notes, transactionType ->
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
                editingExpense = null
            }
        )
    }
}

@Composable
fun PrivateExpenseCard(
    expense: Expense,
    currencySymbol: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = AccentGold.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Private",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = AccentGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = expense.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${expense.category} • ${expense.date} • ${expense.paymentMethod}",
                        style = MaterialTheme.typography.bodySmall.copy(color = MutedSlate, fontSize = 12.sp)
                    )
                }

                Text(
                    text = CurrencyHelper.format(expense.amount, currencySymbol),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = AccentGold
                    )
                )
            }

            if (!expense.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Text(
                        text = expense.notes,
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = AccentGold,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
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
