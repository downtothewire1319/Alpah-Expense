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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FamilyRestroom
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
import androidx.compose.runtime.LaunchedEffect
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
import com.example.data.model.ExpenseCategories
import com.example.data.model.FamilyFundTransfer
import com.example.data.model.FamilyMember
import com.example.data.model.MonthlySavingsBreakdown
import com.example.data.util.FinancialCalculator
import com.example.ui.components.AddFamilyMemberDialog
import com.example.ui.components.FamilyFundOverviewSection
import com.example.ui.components.FamilyFundStatementDialog
import com.example.ui.components.InvoiceDialog
import com.example.ui.components.RecordFamilyTransferDialog
import com.example.ui.components.VaultFamilyFundOverviewCard
import com.example.ui.util.FamilyFundStatementHelper
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentOrange
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
import com.example.ui.viewmodel.SyncStatus
import com.example.ui.viewmodel.UiState
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.draw.rotate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: ExpenseViewModel,
    onNavigateToExpenses: () -> Unit,
    onNavigateToFamily: () -> Unit,
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
    val familyMembers by viewModel.familyMembers.collectAsState()
    val familyTransfers by viewModel.familyTransfers.collectAsState()
    val userProfileName by viewModel.userProfileName.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

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

    // Family Fund Dialog States
    var showRecordTransferDialog by remember { mutableStateOf(false) }
    var selectedMemberForTransfer by remember { mutableStateOf<FamilyMember?>(null) }
    var editingTransfer by remember { mutableStateOf<FamilyFundTransfer?>(null) }

    var showAddMemberDialog by remember { mutableStateOf(false) }
    var editingMember by remember { mutableStateOf<FamilyMember?>(null) }

    var showStatementDialog by remember { mutableStateOf(false) }
    var statementMemberId by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(bottom = 60.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header with subtle lock/unlock icon button
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Dashboard",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    Text(
                        text = if (isVaultActive) "Personal Overview" else "Family & Household Finances",
                        style = MaterialTheme.typography.bodySmall.copy(color = MutedSlate, fontSize = 14.sp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val syncStatus by viewModel.syncStatus.collectAsState()
                    SyncStatusIcon(status = syncStatus)
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Subtle icon button: Lock when in normal mode, LockOpen when unlocked
                    Surface(
                        shape = CircleShape,
                        color = if (isVaultActive) AccentGold.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(38.dp)
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
                                modifier = Modifier.size(20.dp)
                            )
                        }
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

            // Total Fund Transfer Card (Vault Mode)
            item {
                VaultFamilyFundOverviewCard(
                    members = familyMembers,
                    transfers = familyTransfers,
                    currencySymbol = currency.symbol,
                    onNavigateToFamily = onNavigateToFamily
                )
            }
        }

        // 2. DASHBOARD SUMMARY STACKED BAR CHART
        item {
            DashboardSummaryStackedBarChartCard(
                summary = summary,
                currencySymbol = currency.symbol,
                isVaultActive = isVaultActive
            )
        }


        // 3. TOTAL EXPENSES SO FAR CARD
        item {
            TotalExpensesSoFarCard(
                summary = summary,
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

    if (showRecordTransferDialog) {
        val availableSources = remember(bankAccounts) {
            (ExpenseCategories.defaultPaymentSources + bankAccounts.map { it.name }).distinct()
        }
        RecordFamilyTransferDialog(
            members = familyMembers,
            paymentSources = availableSources,
            initialMember = selectedMemberForTransfer,
            initialTransfer = editingTransfer,
            currencySymbol = currency.symbol,
            onDismiss = {
                showRecordTransferDialog = false
                selectedMemberForTransfer = null
                editingTransfer = null
            },
            onSave = { memberId, memberName, amount, date, source, trxId, notes ->
                if (editingTransfer != null) {
                    val updated = editingTransfer!!.copy(
                        memberId = memberId,
                        memberName = memberName,
                        amount = amount,
                        date = date,
                        paymentSource = source,
                        transactionId = trxId,
                        notes = notes
                    )
                    viewModel.updateFamilyTransfer(editingTransfer!!, updated)
                } else {
                    viewModel.recordFamilyTransfer(
                        memberId = memberId,
                        memberName = memberName,
                        amount = amount,
                        date = date,
                        paymentSource = source,
                        transactionId = trxId,
                        notes = notes
                    )
                }
                showRecordTransferDialog = false
                selectedMemberForTransfer = null
                editingTransfer = null
            },
            onAddNewMember = {
                editingMember = null
                showAddMemberDialog = true
            }
        )
    }

    if (showAddMemberDialog) {
        AddFamilyMemberDialog(
            initialMember = editingMember,
            onDismiss = {
                showAddMemberDialog = false
                editingMember = null
            },
            onSave = { name, relationship, colorHex, phone, notes, id ->
                viewModel.saveFamilyMember(
                    name = name,
                    relationship = relationship,
                    avatarColorHex = colorHex,
                    phone = phone,
                    notes = notes,
                    id = id
                )
                showAddMemberDialog = false
                editingMember = null
            }
        )
    }

    if (showStatementDialog) {
        FamilyFundStatementDialog(
            initialMemberId = statementMemberId,
            members = familyMembers,
            allTransfers = familyTransfers,
            currencySymbol = currency.symbol,
            userProfileName = userProfileName,
            onDismiss = {
                showStatementDialog = false
                statementMemberId = null
            },
            onEditTransfer = { transfer ->
                editingTransfer = transfer
                showRecordTransferDialog = true
            },
            onDeleteTransfer = { transfer ->
                viewModel.deleteFamilyTransfer(transfer)
            },
            onNewTransfer = {
                editingTransfer = null
                selectedMemberForTransfer = null
                showRecordTransferDialog = true
            }
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Budget Reminder Hero Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
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
                        .padding(12.dp)
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
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Monthly Budget Target",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = SoftWhite.copy(alpha = 0.85f),
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }

                            if (budget > 0) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (remainingBudget >= 0) AccentGreen.copy(alpha = 0.2f) else AccentRed.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = if (remainingBudget >= 0) "Within Limit" else "Over Limit",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (remainingBudget >= 0) AccentGreen else AccentRed,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (budget > 0) {
                            Text(
                                text = if (remainingBudget >= 0) "Remaining to Reach Budget Limit" else "Over Budget Limit By",
                                style = MaterialTheme.typography.labelSmall.copy(color = SoftWhite.copy(alpha = 0.7f), fontSize = 12.sp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = CurrencyHelper.format(if (remainingBudget >= 0) remainingBudget else -remainingBudget, currencySymbol),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (remainingBudget >= 0) PureWhite else Color(0xFFFF8A80),
                                    fontSize = 20.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            val fraction = (spentThisMonth / budget).toFloat().coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (remainingBudget >= 0) AccentCyan else AccentRed,
                                trackColor = SoftWhite.copy(alpha = 0.2f)
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Spent: ${CurrencyHelper.format(spentThisMonth, currencySymbol)}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = SoftWhite.copy(alpha = 0.8f), fontSize = 12.sp)
                                )
                                Text(
                                    text = "Target: ${CurrencyHelper.format(budget, currencySymbol)}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = SoftWhite.copy(alpha = 0.8f), fontSize = 12.sp)
                                )
                            }
                        } else {
                            Text(
                                text = "No Budget Limit Set",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PureWhite
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Spent this month: ${CurrencyHelper.format(spentThisMonth, currencySymbol)}",
                                style = MaterialTheme.typography.bodySmall.copy(color = SoftWhite.copy(alpha = 0.8f), fontSize = 14.sp)
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Combined Total Expenses Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Total Combined Expenses",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MutedSlate,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = CurrencyHelper.format(summary.totalCost, currencySymbol),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 20.sp
                                )
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = AccentRed.copy(alpha = 0.12f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = AccentRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Public Expense breakdown
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = AccentBlue.copy(alpha = 0.08f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "Public Expense",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MutedSlate, fontSize = 12.sp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = CurrencyHelper.format(summary.totalPublicCost, currencySymbol),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = AccentBlue
                                    )
                                )
                            }
                        }

                        // Private Expense breakdown
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = AccentGold.copy(alpha = 0.08f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "Private Expense",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MutedSlate, fontSize = 12.sp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = CurrencyHelper.format(summary.totalPrivateCost, currencySymbol),
                                    style = MaterialTheme.typography.bodyMedium.copy(
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
private fun formatCompactAmount(amount: Double, symbol: String): String {
    return CurrencyHelper.format(amount, symbol)
}

private fun formatCompactExpenseAmount(amount: Double, symbol: String): String {
    return CurrencyHelper.format(amount, symbol)
}

private fun formatMonthShortLabel(key: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM", Locale.US)
        val date = parser.parse(key) ?: return key
        SimpleDateFormat("MMM", Locale.US).format(date)
    } catch (_: Exception) {
        key
    }
}

private fun formatMonthLabel(key: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM", Locale.US)
        val date = parser.parse(key) ?: return key
        SimpleDateFormat("MMM yy", Locale.US).format(date)
    } catch (_: Exception) {
        key
    }
}


/**
 * Dashboard Summary Stacked Bar Chart:
 * - Clean, modern, responsive Vertical Stacked Bar Chart for the expense dashboard.
 * - Compact top header with "Total Cost" and prominent combined amount + compact legend.
 * - Sliding monthly stacked bars (Nov 26, Dec 26, Jan 27).
 * - Current month bar fully visible with auto-scroll.
 * - Proportional heights, blue (Public) and orange (Private).
 * - Exact Bangladesh currency formatted amounts inside segments without names.
 * - Highly compact footprint (~150dp total card height vs ~500dp previously).
 */
@Composable
fun DashboardSummaryStackedBarChartCard(
    summary: FinancialSummary,
    currencySymbol: String,
    isVaultActive: Boolean
) {
    val totalCost = if (isVaultActive) summary.combinedCost else summary.totalPublicCost

    val currentMonthKey = remember {
        SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
    }

    // Collect all available past months ending at current month (6 months minimum, NO future months)
    val months = remember(summary.publicMonthlyTrend, summary.privateMonthlyTrend, currentMonthKey) {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        val allKeys = mutableSetOf<String>()

        // Always include at least 6 months ending at currentMonthKey (5 past months + current month)
        for (offset in 5 downTo 0) {
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.MONTH, -offset)
            allKeys.add(sdf.format(cal.time))
        }

        allKeys.addAll(summary.publicMonthlyTrend.keys)
        if (isVaultActive) {
            allKeys.addAll(summary.privateMonthlyTrend.keys)
        }

        // STRICT RULE: Exclude any future months (> currentMonthKey)
        allKeys.filter { it <= currentMonthKey }.sorted()
    }

    val lazyListState = rememberLazyListState()

    // Max total for scaling bars
    val maxTotal = remember(months, summary, isVaultActive) {
        months.maxOfOrNull { month ->
            val pub = summary.publicMonthlyTrend[month] ?: 0.0
            val priv = if (isVaultActive) summary.privateMonthlyTrend[month] ?: 0.0 else 0.0
            pub + priv
        }?.coerceAtLeast(1.0) ?: 1.0
    }

    // Auto-scroll so current month (last item in list) is fully visible
    LaunchedEffect(months, currentMonthKey) {
        if (months.isNotEmpty()) {
            lazyListState.scrollToItem((months.size - 1).coerceAtLeast(0))
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .testTag("dashboard_stacked_bar_chart_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth()
        ) {
            // Header Row: Total Cost on left, Legend on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Monthly Spend",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "Last 6 Months",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MutedSlate,
                            fontSize = 11.sp
                        )
                    )
                }

                // Compact Legend
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem(color = AccentBlue, label = "Public")
                    if (isVaultActive) {
                        LegendItem(color = AccentOrange, label = "Private")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Horizontally scrollable row of monthly stacked bars
            LazyRow(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(months, key = { it }) { monthKey ->
                    val pubCost = summary.publicMonthlyTrend[monthKey] ?: 0.0
                    val privCost = if (isVaultActive) summary.privateMonthlyTrend[monthKey] ?: 0.0 else 0.0
                    val monthTotal = pubCost + privCost
                    val isCurrentMonth = (monthKey == currentMonthKey)

                    MonthlyStackedBar(
                        monthLabel = formatMonthShortLabel(monthKey),
                        publicAmount = pubCost,
                        privateAmount = privCost,
                        totalAmount = monthTotal,
                        maxAmount = maxTotal,
                        currencySymbol = currencySymbol,
                        isVaultActive = isVaultActive,
                        isCurrentMonth = isCurrentMonth
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthlyStackedBar(
    monthLabel: String,
    publicAmount: Double,
    privateAmount: Double,
    totalAmount: Double,
    maxAmount: Double,
    currencySymbol: String,
    isVaultActive: Boolean,
    isCurrentMonth: Boolean
) {
    val maxBarHeight = 80.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier.width(44.dp)
    ) {
        // Total amount text above the bar
        if (totalAmount > 0) {
            Text(
                text = CurrencyHelper.format(totalAmount, currencySymbol),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    color = if (isCurrentMonth) AccentBlue else MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 1,
                overflow = TextOverflow.Clip,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))

            val rawFraction = (totalAmount / maxAmount).toFloat()
            val scaledHeight = (maxBarHeight * rawFraction).coerceIn(16.dp, maxBarHeight)

            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(scaledHeight)
                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Private segment (Orange) on top if active and present
                    if (isVaultActive && privateAmount > 0) {
                        val privWeight = (privateAmount / totalAmount).toFloat().coerceIn(0.12f, 0.88f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(if (publicAmount > 0) privWeight else 1f)
                                .background(AccentOrange),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = CurrencyHelper.format(privateAmount, currencySymbol),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.sp
                                ),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Visible,
                                modifier = Modifier
                                    .rotate(-45f)
                                    .padding(horizontal = 2.dp)
                                    .align(Alignment.Center)
                            )
                        }
                    }

                    // Public segment (Blue) on bottom (or full bar if private == 0)
                    if (publicAmount > 0) {
                        val pubWeight = (publicAmount / totalAmount).toFloat().coerceIn(0.12f, 0.88f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(if (isVaultActive && privateAmount > 0) pubWeight else 1f)
                                .background(AccentBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = CurrencyHelper.format(publicAmount, currencySymbol),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.sp
                                ),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Visible,
                                modifier = Modifier
                                    .rotate(-45f)
                                    .padding(horizontal = 2.dp)
                                    .align(Alignment.Center)
                            )
                        }
                    }
                }
            }
        } else {
            // Empty placeholder line for months with zero cost
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Month label below bar
        Text(
            text = monthLabel,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isCurrentMonth) FontWeight.ExtraBold else FontWeight.Medium,
                color = if (isCurrentMonth) AccentBlue else MutedSlate,
                fontSize = 11.sp
            ),
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(color = MutedSlate, fontSize = 12.sp)
        )
    }
}

/**
 * Total Expenses So Far Card
 */
@Composable
fun TotalExpensesSoFarCard(
    summary: FinancialSummary,
    currencySymbol: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total So Far",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MutedSlate,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    )
                    Text(
                        text = CurrencyHelper.format(summary.totalCost, currencySymbol),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 22.sp
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AccentBlue.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "Expense Summary",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = AccentBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Second Line: This Year and This Month in 2-column layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // This Year Cost
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "This Year",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MutedSlate,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = CurrencyHelper.format(summary.currentYearCost, currencySymbol),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = AccentBlue,
                            fontSize = 16.sp
                        )
                    )
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(32.dp)
                        .align(Alignment.CenterVertically)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                )

                // This Month Cost
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "This Month",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MutedSlate,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = CurrencyHelper.format(summary.currentMonthCost, currencySymbol),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = AccentRed,
                            fontSize = 16.sp
                        )
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
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Top Expense Categories",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Top 5 spending areas",
                        style = MaterialTheme.typography.bodySmall.copy(color = MutedSlate, fontSize = 12.sp)
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = AccentPurple.copy(alpha = 0.12f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = null,
                            tint = AccentPurple,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val top5 = categoryTotals.entries.sortedByDescending { it.value }.take(5)

            if (top5.isEmpty()) {
                Text(
                    text = "No category expense data available yet",
                    style = MaterialTheme.typography.bodySmall.copy(color = MutedSlate),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                val safeTotal = if (totalExpenses > 0) totalExpenses else top5.sumOf { it.value }.coerceAtLeast(1.0)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                    CategoryIconBadge(
                                        category = categoryName,
                                        isIncome = false,
                                        size = 24.dp,
                                        iconSize = 13.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = categoryName,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = CurrencyHelper.format(amount, currencySymbol),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "($percent%)",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MutedSlate,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
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
                        style = MaterialTheme.typography.bodySmall.copy(color = MutedSlate, fontSize = 13.sp)
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
                        fontSize = 12.sp
                    )
                )
            }
        }
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
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = AccentGold.copy(alpha = 0.15f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Savings,
                                contentDescription = null,
                                tint = AccentGold,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Total Saved from Banking",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Combined savings across bank accounts",
                            style = MaterialTheme.typography.bodySmall.copy(color = MutedSlate, fontSize = 12.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Text(
                    text = "Manage ↗",
                    style = MaterialTheme.typography.labelSmall.copy(color = AccentGold, fontWeight = FontWeight.Bold),
                    modifier = Modifier.clickable { onNavigateToSavings() }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text("Total Saved Balance", style = MaterialTheme.typography.labelSmall.copy(color = MutedSlate, fontSize = 12.sp))
                    Text(
                        CurrencyHelper.format(totalSavings, currencySymbol),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = AccentGreen
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AccentGreen.copy(alpha = 0.15f)
                ) {
                    Text(
                        "${bankAccounts.size} Accounts",
                        color = AccentGreen,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (bankAccounts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))

                // Horizontal pill list of accounts
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    bankAccounts.forEach { acc ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${acc.name}: ",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                )
                                Text(
                                    text = CurrencyHelper.format(acc.balance, currencySymbol),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
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
}


@Composable
fun SyncStatusIcon(status: SyncStatus) {
    val infiniteTransition = rememberInfiniteTransition(label = "sync_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000)
        ),
        label = "rotation"
    )

    val (icon, tint, desc) = when (status) {
        SyncStatus.SYNCED -> Triple(Icons.Default.CloudDone, AccentGreen, "Synced")
        SyncStatus.SYNCING -> Triple(Icons.Default.CloudSync, AccentBlue, "Syncing")
        SyncStatus.OFFLINE -> Triple(Icons.Default.CloudOff, AccentRed, "Offline")
    }

    Icon(
        imageVector = icon,
        contentDescription = desc,
        tint = tint,
        modifier = Modifier
            .size(20.dp)
            .then(if (status == SyncStatus.SYNCING) Modifier.rotate(rotation) else Modifier)
    )
}
