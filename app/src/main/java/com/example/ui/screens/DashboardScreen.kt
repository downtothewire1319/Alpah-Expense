package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BankAccount
import com.example.data.model.Expense
import com.example.ui.components.InvoiceDialog
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.AccentRed
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.LightNavy
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.NavyCard
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftWhite
import com.example.ui.util.CategoryIconBadge
import com.example.ui.util.CurrencyHelper
import com.example.ui.viewmodel.ExpenseViewModel
import com.example.ui.viewmodel.FinancialSummary
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: ExpenseViewModel,
    onNavigateToExpenses: () -> Unit,
    onOpenAddTransaction: () -> Unit,
    onRequestPinUnlock: () -> Unit
) {
    val publicState by viewModel.publicExpensesState.collectAsState()
    val privateList by viewModel.privateExpenses.collectAsState()
    val isVaultActive by viewModel.isPrivateUnlocked.collectAsState()
    val bankAccounts by viewModel.bankAccounts.collectAsState()
    val currency by viewModel.selectedCurrency.collectAsState()
    val summary by viewModel.financialSummary.collectAsState()
    val currentBudget by viewModel.currentBudget.collectAsState()

    val publicList = when (val s = publicState) {
        is UiState.Success -> s.data
        else -> emptyList()
    }

    val activeTransactions = if (isVaultActive) {
        (publicList + privateList).sortedByDescending { it.date }
    } else {
        publicList.sortedByDescending { it.date }
    }

    var selectedInvoiceExpense by remember { mutableStateOf<Expense?>(null) }
    var selectedMonthHighlight by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with subtle lock/unlock icon button (No suspicious text!)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Dashboard",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    Text(
                        text = if (isVaultActive) "Personal Overview" else "Family & Household Finances",
                        style = MaterialTheme.typography.bodySmall.copy(color = MutedSlate)
                    )
                }

                // Subtle icon button: Lock when in normal mode, LockOpen when unlocked
                Surface(
                    shape = CircleShape,
                    color = if (isVaultActive) AccentGold.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(44.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (isVaultActive) {
                                viewModel.lockPrivateSection()
                            } else {
                                onRequestPinUnlock()
                            }
                        },
                        modifier = Modifier.testTag("dashboard_stealth_lock_button")
                    ) {
                        Icon(
                            imageVector = if (isVaultActive) Icons.Default.LockOpen else Icons.Outlined.Lock,
                            contentDescription = if (isVaultActive) "Lock" else "Unlock",
                            tint = if (isVaultActive) AccentGold else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // 1. TOP 3 SUMMARY CARDS (Total Income, Total Expenses, Remaining Balance)
        item {
            TopThreeSummaryCards(
                summary = summary,
                currentBudget = currentBudget,
                currencySymbol = currency.symbol,
                isVaultActive = isVaultActive
            )
        }

        // Vault Monthly Savings & Bank Balances Overview (Only visible in vault mode)
        if (isVaultActive) {
            item {
                VaultSavingsOverviewCard(
                    bankAccounts = bankAccounts,
                    currencySymbol = currency.symbol,
                    onNavigateToSavings = onNavigateToExpenses
                )
            }
        }


        // Quick Action: Add Transaction
        item {
            Button(
                onClick = onOpenAddTransaction,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .height(52.dp)
                    .testTag("quick_add_transaction_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isVaultActive) AccentGold else AccentBlue
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = if (isVaultActive) DeepNavy else PureWhite,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Add Transaction",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = if (isVaultActive) DeepNavy else PureWhite,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        // 2. MONTHLY EXPENSES BAR CHART (Horizontal scrollable, clear vertical bars)
        item {
            MonthlyExpensesBarChartCard(
                publicMonthlyTrend = summary.publicMonthlyTrend,
                privateMonthlyTrend = summary.privateMonthlyTrend,
                currencySymbol = currency.symbol,
                isVaultActive = isVaultActive,
                selectedMonth = selectedMonthHighlight,
                onSelectMonth = { selectedMonthHighlight = if (selectedMonthHighlight == it) null else it }
            )
        }

        // 3. TOTAL EXPENSES SO FAR CARD
        item {
            TotalExpensesSoFarCard(
                totalExpenses = summary.totalCost,
                transactionCount = activeTransactions.count { !it.isIncome },
                currencySymbol = currency.symbol
            )
        }

        // 4. TOP EXPENSE CATEGORIES BAR CHART (Top 5 categories with icon on left side)
        item {
            val displayedCategoryTotals = if (selectedMonthHighlight != null) {
                viewModel.getCategoryTotalsForMonth(selectedMonthHighlight!!, isVaultActive)
            } else {
                if (isVaultActive) summary.privateCategoryTotals else summary.publicCategoryTotals
            }

            TopExpenseCategoriesCard(
                categoryTotals = displayedCategoryTotals,
                totalExpenses = summary.totalCost,
                currencySymbol = currency.symbol
            )
        }
    }

    if (selectedInvoiceExpense != null) {
        InvoiceDialog(
            expense = selectedInvoiceExpense!!,
            currencySymbol = currency.symbol,
            onDismiss = { selectedInvoiceExpense = null }
        )
    }
}

/**
 * Top Summary Cards:
 * In Public Mode (!isVaultActive): Displays Monthly Budget Target & Remaining Budget to Reach, plus Total Expenses.
 * In Vault Mode (isVaultActive): Displays Personal Vault Balance & Income/Expenses.
 */
@Composable
fun TopThreeSummaryCards(
    summary: FinancialSummary,
    currentBudget: com.example.data.model.Budget?,
    currencySymbol: String,
    isVaultActive: Boolean = false
) {
    if (!isVaultActive) {
        val budget = currentBudget?.publicBudget?.takeIf { it > 0 } ?: currentBudget?.monthlyBudget ?: 0.0
        val spentThisMonth = summary.currentMonthPublicCost
        val remainingBudget = budget - spentThisMonth

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Budget Reminder Hero Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = DeepNavy),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(DeepNavy, LightNavy)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Savings,
                                    contentDescription = null,
                                    tint = AccentCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Monthly Budget Target",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = SoftWhite.copy(alpha = 0.8f),
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }

                            if (budget > 0) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (remainingBudget >= 0) AccentGreen.copy(alpha = 0.2f) else AccentRed.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = if (remainingBudget >= 0) "Within Limit" else "Over Limit",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (remainingBudget >= 0) AccentGreen else AccentRed,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (budget > 0) {
                            Text(
                                text = if (remainingBudget >= 0) "Remaining to Reach Budget Limit" else "Over Budget Limit By",
                                style = MaterialTheme.typography.labelSmall.copy(color = SoftWhite.copy(alpha = 0.7f))
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = CurrencyHelper.format(if (remainingBudget >= 0) remainingBudget else -remainingBudget, currencySymbol),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (remainingBudget >= 0) PureWhite else Color(0xFFFF8A80)
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            val fraction = (spentThisMonth / budget).toFloat().coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (remainingBudget >= 0) AccentCyan else AccentRed,
                                trackColor = SoftWhite.copy(alpha = 0.2f)
                            )

                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Spent: ${CurrencyHelper.format(spentThisMonth, currencySymbol)}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = SoftWhite.copy(alpha = 0.8f), fontSize = 11.sp)
                                )
                                Text(
                                    text = "Target: ${CurrencyHelper.format(budget, currencySymbol)}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = SoftWhite.copy(alpha = 0.8f), fontSize = 11.sp)
                                )
                            }
                        } else {
                            Text(
                                text = "No Budget Limit Set",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PureWhite
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Spent this month: ${CurrencyHelper.format(spentThisMonth, currencySymbol)}",
                                style = MaterialTheme.typography.bodyMedium.copy(color = SoftWhite.copy(alpha = 0.8f))
                            )
                        }
                    }
                }
            }

            // Total Public Expenses Card (Full width)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total Expenses",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MutedSlate,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CurrencyHelper.format(summary.totalCost, currencySymbol),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = AccentRed.copy(alpha = 0.15f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = AccentRed,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Combined Total Expenses Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Total Combined Expenses",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = MutedSlate,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = CurrencyHelper.format(summary.totalCost, currencySymbol),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = AccentRed.copy(alpha = 0.12f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = AccentRed,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Public Expense breakdown
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = AccentBlue.copy(alpha = 0.08f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Public Expense",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MutedSlate)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = CurrencyHelper.format(summary.totalPublicCost, currencySymbol),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = AccentBlue
                                    )
                                )
                            }
                        }

                        // Private Expense breakdown
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = AccentGold.copy(alpha = 0.08f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Private Expense",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MutedSlate)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = CurrencyHelper.format(summary.totalPrivateCost, currencySymbol),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = AccentGold
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Monthly Expenses Bar Chart:
 * - Months horizontally
 * - Expense amount vertically
 * - Stacked bars representing public (Blue) and private (Gold) expenses
 * - Horizontally scrollable on mobile
 */
@Composable
fun MonthlyExpensesBarChartCard(
    publicMonthlyTrend: Map<String, Double>,
    privateMonthlyTrend: Map<String, Double>,
    currencySymbol: String,
    isVaultActive: Boolean,
    selectedMonth: String?,
    onSelectMonth: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Monthly Expenses",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = AccentBlue.copy(alpha = 0.12f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val allMonths = (publicMonthlyTrend.keys + (if (isVaultActive) privateMonthlyTrend.keys else emptySet())).distinct().sorted()
            if (allMonths.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No monthly spending recorded yet",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MutedSlate)
                    )
                }
            } else {
                val maxExpense = allMonths.maxOf { 
                    val pub = publicMonthlyTrend[it] ?: 0.0
                    val priv = if (isVaultActive) (privateMonthlyTrend[it] ?: 0.0) else 0.0
                    (pub + priv)
                }.coerceAtLeast(1.0)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    allMonths.forEach { monthKey ->
                        val publicAmt = publicMonthlyTrend[monthKey] ?: 0.0
                        val privateAmt = if (isVaultActive) (privateMonthlyTrend[monthKey] ?: 0.0) else 0.0
                        val totalAmt = publicAmt + privateAmt
                        val isHighlighted = selectedMonth == monthKey

                        val totalRatio = (totalAmt / maxExpense).toFloat().coerceIn(0.1f, 1f)
                        val barHeight = (140 * totalRatio).dp

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { onSelectMonth(monthKey) }
                                .padding(horizontal = 4.dp)
                        ) {
                            // 1. Total Label on Top (as per drawing)
                            Text(
                                text = CurrencyHelper.format(totalAmt, currencySymbol),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // 2. Stacked Bar (as per drawing)
                            Box(
                                modifier = Modifier
                                    .width(42.dp)
                                    .height(barHeight)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    // Private part (Top in stack)
                                    if (privateAmt > 0) {
                                        val privWeight = (privateAmt / totalAmt).toFloat().coerceAtLeast(0.01f)
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(privWeight)
                                                .background(AccentGold),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (totalRatio > 0.3f && privateAmt > 0) {
                                                Text(
                                                    text = CurrencyHelper.format(privateAmt, currencySymbol),
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 8.sp,
                                                        color = DeepNavy,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Clip
                                                )
                                            }
                                        }
                                    }

                                    // Public part (Bottom in stack)
                                    if (publicAmt > 0) {
                                        val pubWeight = (publicAmt / totalAmt).toFloat().coerceAtLeast(0.01f)
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(pubWeight)
                                                .background(AccentBlue),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (totalRatio > 0.3f && publicAmt > 0) {
                                                Text(
                                                    text = CurrencyHelper.format(publicAmt, currencySymbol),
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 8.sp,
                                                        color = PureWhite,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Clip
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = formatMonthLabel(monthKey),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isHighlighted) MaterialTheme.colorScheme.onSurface else MutedSlate,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Total Expenses So Far Card
 */
@Composable
fun TotalExpensesSoFarCard(
    totalExpenses: Double,
    transactionCount: Int,
    currencySymbol: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Total Expenses So Far",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MutedSlate,
                        fontWeight = FontWeight.Medium
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = CurrencyHelper.format(totalExpenses, currencySymbol),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$transactionCount transactions recorded",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MutedSlate,
                        fontSize = 11.sp
                    )
                )
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = AccentRed.copy(alpha = 0.12f),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = AccentRed,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

    }
}

/**
 * Top Expense Categories Bar Chart (Top 5 categories with icon on left side)
 */
@Composable
fun TopExpenseCategoriesCard(
    categoryTotals: Map<String, Double>,
    totalExpenses: Double,
    currencySymbol: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Top Expense Categories",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Top 5 spending areas",
                        style = MaterialTheme.typography.bodySmall.copy(color = MutedSlate, fontSize = 11.sp)
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = AccentPurple.copy(alpha = 0.12f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = null,
                            tint = AccentPurple,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val top5 = categoryTotals.entries.sortedByDescending { it.value }.take(5)

            if (top5.isEmpty()) {
                Text(
                    text = "No category expense data available yet",
                    style = MaterialTheme.typography.bodyMedium.copy(color = MutedSlate),
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                val safeTotal = if (totalExpenses > 0) totalExpenses else top5.sumOf { it.value }.coerceAtLeast(1.0)

                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    top5.forEach { (categoryName, amount) ->
                        val fraction = (amount / safeTotal).toFloat().coerceIn(0.02f, 1f)
                        val percent = (fraction * 100).toInt()

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Related icon on the left side of category!
                                    CategoryIconBadge(
                                        category = categoryName,
                                        isIncome = false,
                                        size = 32.dp,
                                        iconSize = 16.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = categoryName,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = CurrencyHelper.format(amount, currencySymbol),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "($percent%)",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MutedSlate,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Horizontal progress bar
                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = AccentBlue,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dashboard Transaction Row: related icon on left side, amount, and tap-for-invoice
 */
@Composable
fun DashboardTransactionRow(
    expense: Expense,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isIncome = expense.isIncome

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("dashboard_tx_${expense.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Category icon on the left side
                CategoryIconBadge(
                    category = expense.category,
                    isIncome = isIncome,
                    size = 40.dp,
                    iconSize = 20.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = expense.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${expense.category} • ${expense.date}",
                        style = MaterialTheme.typography.bodySmall.copy(color = MutedSlate, fontSize = 11.sp)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isIncome) "+" else "-"}${CurrencyHelper.format(expense.amount, currencySymbol)}",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isIncome) AccentGreen else MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = "Receipt ↗",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = AccentBlue,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}

private fun formatMonthLabel(key: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM", Locale.US)
        val date = parser.parse(key) ?: return key
        SimpleDateFormat("MMM ''yy", Locale.US).format(date)
    } catch (_: Exception) {
        key
    }
}

@Composable
fun VaultSavingsOverviewCard(
    bankAccounts: List<BankAccount>,
    currencySymbol: String,
    onNavigateToSavings: () -> Unit
) {
    val totalSavings = bankAccounts.sumOf { it.balance }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .testTag("vault_savings_overview_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = AccentGold.copy(alpha = 0.15f),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Savings,
                                contentDescription = null,
                                tint = AccentGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Monthly Savings & Banks",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Janata, Meghna, DBBL, bKash",
                            style = MaterialTheme.typography.bodySmall.copy(color = MutedSlate, fontSize = 11.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Text(
                    text = "Manage ↗",
                    style = MaterialTheme.typography.labelMedium.copy(color = AccentGold, fontWeight = FontWeight.Bold),
                    modifier = Modifier.clickable { onNavigateToSavings() }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text("Total Saved Balance", style = MaterialTheme.typography.labelSmall.copy(color = MutedSlate))
                    Text(
                        CurrencyHelper.format(totalSavings, currencySymbol),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = AccentGreen
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AccentGreen.copy(alpha = 0.15f)
                ) {
                    Text(
                        "${bankAccounts.size} Accounts",
                        color = AccentGreen,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Horizontal pill list of accounts
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                bankAccounts.forEach { acc ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${acc.name}: ",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                            )
                            Text(
                                text = "৳${acc.balance.toInt()}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (acc.balance >= 0) AccentGreen else AccentRed
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

