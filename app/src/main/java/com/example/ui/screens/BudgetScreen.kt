package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Budget
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.PureWhite
import com.example.ui.util.CurrencyHelper
import com.example.ui.util.ResponsiveModal
import com.example.ui.viewmodel.ExpenseViewModel
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    viewModel: ExpenseViewModel
) {
    val currentBudget by viewModel.currentBudget.collectAsState()
    val publicState by viewModel.publicExpensesState.collectAsState()
    val privateList by viewModel.privateExpenses.collectAsState()
    val isPrivateUnlocked by viewModel.isPrivateUnlocked.collectAsState()
    val currency by viewModel.selectedCurrency.collectAsState()

    var showEditBudgetDialog by remember { mutableStateOf(false) }

    val currentMonth = remember { SimpleDateFormat("yyyy-MM", Locale.US).format(Date()) }
    val displayMonth = remember { SimpleDateFormat("MMMM yyyy", Locale.US).format(Date()) }

    val publicList = when (val s = publicState) {
        is UiState.Success -> s.data
        else -> emptyList()
    }

    val monthPublicSpent = publicList.filter { it.date.startsWith(currentMonth) && !it.isIncome }.sumOf { it.amount }
    val monthPrivateSpent = if (isPrivateUnlocked) {
        privateList.filter { it.date.startsWith(currentMonth) && !it.isIncome }.sumOf { it.amount }
    } else 0.0

    val monthPublicIncome = publicList.filter { it.date.startsWith(currentMonth) && it.isIncome }.sumOf { it.amount }
    val monthPrivateIncome = if (isPrivateUnlocked) {
        privateList.filter { it.date.startsWith(currentMonth) && it.isIncome }.sumOf { it.amount }
    } else 0.0

    val monthTotalSpent = monthPublicSpent + monthPrivateSpent
    val monthTotalIncome = monthPublicIncome + monthPrivateIncome

    val totalBudget = currentBudget?.monthlyBudget ?: 0.0
    val publicBudget = currentBudget?.publicBudget ?: 0.0
    val privateBudget = currentBudget?.privateBudget ?: 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Monthly Budget ($displayMonth)",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                actions = {
                    Button(
                        onClick = { showEditBudgetDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 8.dp).testTag("edit_budget_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (totalBudget > 0) "Adjust" else "Set Budget")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("budget_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Overall Monthly Budget Hero Card
            item {
                BudgetStatusCard(
                    title = "Overall Monthly Budget",
                    budgetAmount = totalBudget,
                    spentAmount = monthTotalSpent,
                    currencySymbol = currency.symbol,
                    accentColor = AccentBlue
                )
            }

            // Public Expense Budget Card
            item {
                BudgetStatusCard(
                    title = "Shared Public Budget",
                    budgetAmount = publicBudget,
                    spentAmount = monthPublicSpent,
                    currencySymbol = currency.symbol,
                    accentColor = AccentBlue
                )
            }

            // Secondary Budget Card (only displayed when vault is active)
            if (isPrivateUnlocked) {
                item {
                    BudgetStatusCard(
                        title = "Vault Budget",
                        budgetAmount = privateBudget,
                        spentAmount = monthPrivateSpent,
                        currencySymbol = currency.symbol,
                        accentColor = AccentGold
                    )
                }
            }

            // Savings Tracker
            item {
                SavingsTrackerCard(
                    totalIncome = monthTotalIncome,
                    totalSpent = monthTotalSpent,
                    totalBudget = totalBudget,
                    currencySymbol = currency.symbol
                )
            }
        }
    }

    if (showEditBudgetDialog) {
        EditBudgetDialog(
            currentBudget = currentBudget,
            onDismiss = { showEditBudgetDialog = false },
            onSave = { monthly, pub, priv ->
                viewModel.saveBudget(monthly, pub, priv)
                showEditBudgetDialog = false
            }
        )
    }
}

@Composable
fun BudgetStatusCard(
    title: String,
    budgetAmount: Double,
    spentAmount: Double,
    currencySymbol: String,
    accentColor: Color
) {
    val hasBudget = budgetAmount > 0.0
    val fraction = if (hasBudget) (spentAmount / budgetAmount).toFloat().coerceIn(0f, 1.5f) else 0f
    val isOverBudget = hasBudget && spentAmount > budgetAmount
    val isNearBudget = hasBudget && spentAmount >= budgetAmount * 0.85 && !isOverBudget
    val remainingAmount = if (hasBudget) budgetAmount - spentAmount else 0.0

    val progressColor = when {
        !hasBudget -> MutedSlate.copy(alpha = 0.5f)
        isOverBudget -> AccentRed
        isNearBudget -> AccentGold
        else -> AccentGreen
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                if (isOverBudget) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AccentRed.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = AccentRed, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Over Budget", color = AccentRed, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                } else if (!hasBudget) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "No Limit Set",
                            color = MutedSlate,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Spent: ${CurrencyHelper.format(spentAmount, currencySymbol)}",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isOverBudget) AccentRed else if (hasBudget) AccentGreen else MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = if (hasBudget) "of ${CurrencyHelper.format(budgetAmount, currencySymbol)} budget" else "of no budget limit",
                        style = MaterialTheme.typography.bodySmall.copy(color = MutedSlate)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    if (hasBudget) {
                        Text(
                            text = if (isOverBudget) "Exceeded by" else "Remaining",
                            style = MaterialTheme.typography.labelSmall.copy(color = MutedSlate)
                        )
                        Text(
                            text = CurrencyHelper.format(kotlin.math.abs(remainingAmount), currencySymbol),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isOverBudget) AccentRed else AccentGreen
                            )
                        )
                    } else {
                        Text(
                            text = "Remaining",
                            style = MaterialTheme.typography.labelSmall.copy(color = MutedSlate)
                        )
                        Text(
                            text = "--",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MutedSlate
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { fraction.coerceAtMost(1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            if (!hasBudget) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No budget limit set for this month. Tap Adjust to set one.",
                    style = MaterialTheme.typography.labelSmall.copy(color = MutedSlate)
                )
            }
        }
    }
}

@Composable
fun SavingsTrackerCard(
    totalIncome: Double,
    totalSpent: Double,
    totalBudget: Double,
    currencySymbol: String
) {
    val netMonthlySavings = totalIncome - totalSpent
    val isPositiveSavings = netMonthlySavings >= 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = (if (isPositiveSavings) AccentGreen else AccentRed).copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = if (isPositiveSavings) AccentGreen else AccentRed,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Net Monthly Cash Savings",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = if (isPositiveSavings) "Surplus saved this month" else "Deficit / Overspent this month",
                        style = MaterialTheme.typography.bodySmall.copy(color = MutedSlate)
                    )
                }

                Text(
                    text = "${if (isPositiveSavings) "+" else "-"}${CurrencyHelper.format(kotlin.math.abs(netMonthlySavings), currencySymbol)}",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isPositiveSavings) AccentGreen else AccentRed
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Month Inflow", style = MaterialTheme.typography.labelSmall.copy(color = MutedSlate))
                        Text(
                            "+${CurrencyHelper.format(totalIncome, currencySymbol)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = AccentGreen)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Month Outflow", style = MaterialTheme.typography.labelSmall.copy(color = MutedSlate))
                        Text(
                            "-${CurrencyHelper.format(totalSpent, currencySymbol)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = AccentRed)
                        )
                    }
                    if (totalBudget > 0) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Budget Left", style = MaterialTheme.typography.labelSmall.copy(color = MutedSlate))
                            val bRem = totalBudget - totalSpent
                            Text(
                                CurrencyHelper.format(bRem, currencySymbol),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (bRem >= 0) AccentBlue else AccentRed
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun EditBudgetDialog(
    currentBudget: Budget?,
    onDismiss: () -> Unit,
    onSave: (monthlyBudget: Double, publicBudget: Double, privateBudget: Double) -> Unit
) {
    var monthlyText by remember { mutableStateOf(if (currentBudget != null && currentBudget.monthlyBudget > 0) currentBudget.monthlyBudget.toString() else "") }
    var publicText by remember { mutableStateOf(if (currentBudget != null && currentBudget.publicBudget > 0) currentBudget.publicBudget.toString() else "") }
    var privateText by remember { mutableStateOf(if (currentBudget != null && currentBudget.privateBudget > 0) currentBudget.privateBudget.toString() else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = ResponsiveModal.modalModifier(),
        properties = ResponsiveModal.dialogProperties,
        title = { Text("Set Monthly Budgets", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = monthlyText,
                    onValueChange = { monthlyText = it },
                    label = { Text("Total Monthly Budget") },
                    placeholder = { Text("e.g. 50000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("monthly_budget_input")
                )

                OutlinedTextField(
                    value = publicText,
                    onValueChange = { publicText = it },
                    label = { Text("Public Household Budget") },
                    placeholder = { Text("e.g. 35000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("public_budget_input")
                )

                OutlinedTextField(
                    value = privateText,
                    onValueChange = { privateText = it },
                    label = { Text("Private Personal Budget") },
                    placeholder = { Text("e.g. 15000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("private_budget_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val m = monthlyText.toDoubleOrNull() ?: 0.0
                    val pub = publicText.toDoubleOrNull() ?: 0.0
                    val priv = privateText.toDoubleOrNull() ?: 0.0
                    onSave(m, pub, priv)
                },
                modifier = Modifier.testTag("save_budget_button")
            ) {
                Text("Save Budget")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
