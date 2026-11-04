package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BankAccount
import com.example.data.model.Expense
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.AccentRed
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.PureWhite
import com.example.ui.util.CategoryIconBadge
import com.example.ui.util.CurrencyHelper
import com.example.ui.util.FinancialInvoiceHelper
import com.example.ui.viewmodel.ExpenseViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class SimpleReportPeriod(val label: String) {
    THIS_MONTH("This Month"),
    LAST_30_DAYS("Last 30 Days"),
    THIS_YEAR("This Year"),
    ALL_TIME("All Time")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ExpenseViewModel
) {
    val context = LocalContext.current
    val user = Firebase.auth.currentUser
    val publicState by viewModel.publicExpensesState.collectAsState()
    val privateList by viewModel.privateExpenses.collectAsState()
    val isVaultActive by viewModel.isPrivateUnlocked.collectAsState()
    val bankAccounts by viewModel.bankAccounts.collectAsState()
    val currency by viewModel.selectedCurrency.collectAsState()
    val currentBudget by viewModel.currentBudget.collectAsState()
    val currencySymbol = currency.symbol

    var reportMode by remember { mutableStateOf(0) } // 0: Spending & Incomes, 1: Bank Statements
    var selectedStatementBank by remember { mutableStateOf("All") }
    var selectedPeriod by remember { mutableStateOf(SimpleReportPeriod.THIS_MONTH) }

    val rawList = if (isVaultActive) {
        privateList
    } else {
        when (val s = publicState) {
            is UiState.Success -> s.data
            else -> emptyList()
        }
    }

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val thisMonthStr = remember { SimpleDateFormat("yyyy-MM", Locale.US).format(Date()) }
    val thisYearStr = remember { SimpleDateFormat("yyyy", Locale.US).format(Date()) }

    val thirtyDaysAgoStr = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -30)
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    // Statement Transactions logic pulled up
    val statementTransactions = rawList.filter {
        if (selectedStatementBank == "All") true
        else it.paymentMethod.contains(selectedStatementBank, ignoreCase = true)
    }.sortedByDescending { it.date }

    // Filter by period
    val periodTransactions = rawList.filter { exp ->
        when (selectedPeriod) {
            SimpleReportPeriod.THIS_MONTH -> exp.date.startsWith(thisMonthStr)
            SimpleReportPeriod.LAST_30_DAYS -> exp.date in thirtyDaysAgoStr..todayStr
            SimpleReportPeriod.THIS_YEAR -> exp.date.startsWith(thisYearStr)
            SimpleReportPeriod.ALL_TIME -> true
        }
    }

    val periodExpensesList = periodTransactions.filter { !it.isIncome }
    val periodIncomeList = periodTransactions.filter { it.isIncome }

    val totalSpent = periodExpensesList.sumOf { it.amount }
    val totalIncome = periodIncomeList.sumOf { it.amount }
    val remainingBalance = totalIncome - totalSpent

    // Category breakdown
    val categoryBreakdown = periodExpensesList.groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
        .entries.sortedByDescending { it.value }

    // Monthly spending breakdown
    val monthlyBreakdown = periodExpensesList.groupBy { it.date.take(7) }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
        .entries.sortedByDescending { it.key }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Financial Reports",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (isVaultActive) "Personal Reports" else "Household Financial Summary",
                            style = MaterialTheme.typography.labelSmall.copy(color = MutedSlate)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (reportMode == 1) {
                                FinancialInvoiceHelper.printInvoice(
                                    context = context,
                                    transactions = statementTransactions,
                                    title = if (selectedStatementBank == "All") "Consolidated Bank Statement" else "$selectedStatementBank Statement",
                                    periodLabel = "Account: $selectedStatementBank",
                                    userName = user?.displayName ?: "User",
                                    userEmail = user?.email ?: "",
                                    currencySymbol = currencySymbol
                                )
                            } else {
                                FinancialInvoiceHelper.printInvoice(
                                    context = context,
                                    transactions = periodTransactions,
                                    title = "${selectedPeriod.label} Financial Report",
                                    periodLabel = selectedPeriod.label,
                                    userName = user?.displayName ?: "User",
                                    userEmail = user?.email ?: "",
                                    currencySymbol = currencySymbol,
                                    isAggregate = selectedPeriod == SimpleReportPeriod.THIS_YEAR || selectedPeriod == SimpleReportPeriod.ALL_TIME
                                )
                            }
                        },
                        modifier = Modifier.testTag("print_report_top_bar_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = "Print Report Invoice",
                            tint = AccentBlue
                        )
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
                .testTag("reports_screen"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mode Toggle: Spending Overview vs Bank Statements (Bank Statements only in Private Vault mode)
            if (isVaultActive) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = reportMode == 0,
                            onClick = { reportMode = 0 },
                            label = { Text("📊 Spending ", fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentBlue,
                                selectedLabelColor = PureWhite
                            ),
                            modifier = Modifier.weight(1f).testTag("report_mode_spending")
                        )
                        FilterChip(
                            selected = reportMode == 1,
                            onClick = { reportMode = 1 },
                            label = { Text("🏦 Bank Statements", fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentGreen,
                                selectedLabelColor = PureWhite
                            ),
                            modifier = Modifier.weight(1f).testTag("report_mode_banks")
                        )
                    }
                }
            }

            if (reportMode == 1) {
                // Bank Account Selector Chips
                item {
                    val allBankNames = remember(bankAccounts) {
                        listOf("All Accounts") + bankAccounts.map { it.name }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allBankNames.forEach { bName ->
                            val isSel = (bName == "All Accounts" && selectedStatementBank == "All") || bName == selectedStatementBank
                            FilterChip(
                                selected = isSel,
                                onClick = {
                                    selectedStatementBank = if (bName == "All Accounts") "All" else bName
                                },
                                label = { Text(bName, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentGreen,
                                    selectedLabelColor = PureWhite
                                )
                            )
                        }
                    }
                }

                val totalCredits = statementTransactions.filter { it.isIncome }.sumOf { it.amount }
                val totalDebits = statementTransactions.filter { !it.isIncome }.sumOf { it.amount }
                val matchingAccount = bankAccounts.find { it.name.equals(selectedStatementBank, ignoreCase = true) }
                val currentAccBalance = matchingAccount?.balance ?: bankAccounts.sumOf { it.balance }

                // Statement Header Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("bank_statement_header_card"),
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalance,
                                        contentDescription = null,
                                        tint = AccentGreen,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (selectedStatementBank == "All") "Consolidated Statement" else "$selectedStatementBank Statement",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            FinancialInvoiceHelper.printInvoice(
                                                context = context,
                                                transactions = statementTransactions,
                                                title = if (selectedStatementBank == "All") "Consolidated Bank Statement" else "$selectedStatementBank Statement",
                                                periodLabel = "Account: $selectedStatementBank",
                                                userName = user?.displayName ?: "User",
                                                userEmail = user?.email ?: "",
                                                currencySymbol = currencySymbol
                                            )
                                        },
                                        modifier = Modifier.testTag("print_statement_icon_button")
                                    ) {
                                        Icon(Icons.Default.Print, contentDescription = "Print Statement", tint = AccentBlue)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = AccentGreen.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "Official Statement",
                                            color = AccentGreen,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Current Available Balance",
                                style = MaterialTheme.typography.labelSmall.copy(color = MutedSlate)
                            )
                            Text(
                                text = CurrencyHelper.format(currentAccBalance, currencySymbol),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    color = AccentGreen.copy(alpha = 0.1f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Total Credits (+)", style = MaterialTheme.typography.labelSmall.copy(color = MutedSlate))
                                        Text(
                                            "+${CurrencyHelper.format(totalCredits, currencySymbol)}",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = AccentGreen)
                                        )
                                    }
                                }

                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    color = AccentRed.copy(alpha = 0.1f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Total Debits (-)", style = MaterialTheme.typography.labelSmall.copy(color = MutedSlate))
                                        Text(
                                            "-${CurrencyHelper.format(totalDebits, currencySymbol)}",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = AccentRed)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Ledger Title
                item {
                    Text(
                        text = "Itemized Statement Ledger (${statementTransactions.size} records)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MutedSlate)
                    )
                }

                if (statementTransactions.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Text(
                                text = "No transactions found for this account.",
                                style = MaterialTheme.typography.bodyMedium.copy(color = MutedSlate),
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                    }
                } else {
                    items(statementTransactions, key = { it.id }) { item ->
                        StatementLedgerItemCard(
                            expense = item,
                            currencySymbol = currencySymbol
                        )
                    }
                }
            } else {
                // 1. Period Selector Chips
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SimpleReportPeriod.values().forEach { period ->
                            val isSelected = period == selectedPeriod
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedPeriod = period },
                                label = { Text(period.label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentBlue,
                                    selectedLabelColor = PureWhite
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                        }
                    }
                }


            // 2. High-Level Summary Card (Income, Spent, Remaining Balance)
            item {
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
                            Text(
                                text = "Summary (${selectedPeriod.label})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            IconButton(
                                onClick = {
                                    FinancialInvoiceHelper.printInvoice(
                                        context = context,
                                        transactions = periodTransactions,
                                        title = "${selectedPeriod.label} Financial Statement Invoice",
                                        periodLabel = selectedPeriod.label,
                                        userName = user?.displayName ?: "User",
                                        userEmail = user?.email ?: "",
                                        currencySymbol = currencySymbol,
                                        isAggregate = selectedPeriod == SimpleReportPeriod.THIS_YEAR || selectedPeriod == SimpleReportPeriod.ALL_TIME
                                    )
                                },
                                modifier = Modifier.testTag("print_summary_icon_button")
                            ) {
                                Icon(Icons.Default.Print, contentDescription = "Print Summary", tint = AccentBlue)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val reportBudget = currentBudget?.publicBudget?.takeIf { it > 0 } ?: currentBudget?.monthlyBudget ?: 0.0
                        val remBudget = reportBudget - totalSpent

                        if (isVaultActive) {
                            // Total Income Row (Vault Mode)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = AccentGreen.copy(alpha = 0.15f),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowDownward,
                                                contentDescription = null,
                                                tint = AccentGreen,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Total Income",
                                        style = MaterialTheme.typography.bodyMedium.copy(color = MutedSlate)
                                    )
                                }
                                Text(
                                    text = "+${CurrencyHelper.format(totalIncome, currencySymbol)}",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = AccentGreen
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        } else if (reportBudget > 0) {
                            // Monthly Budget Target Row (Public Mode)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = AccentCyan.copy(alpha = 0.15f),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Savings,
                                                contentDescription = null,
                                                tint = AccentCyan,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Monthly Budget Target",
                                        style = MaterialTheme.typography.bodyMedium.copy(color = MutedSlate)
                                    )
                                }
                                Text(
                                    text = CurrencyHelper.format(reportBudget, currencySymbol),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // How Much Was Spent Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = AccentRed.copy(alpha = 0.15f),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = null,
                                            tint = AccentRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Total Spent",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = MutedSlate)
                                )
                            }
                            Text(
                                text = "-${CurrencyHelper.format(totalSpent, currencySymbol)}",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Remaining Balance / Budget Remaining Row
                        val isPositive = if (!isVaultActive && reportBudget > 0) remBudget >= 0 else remainingBalance >= 0
                        val displayValue = if (!isVaultActive && reportBudget > 0) remBudget else remainingBalance

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Savings,
                                    contentDescription = null,
                                    tint = if (isPositive) AccentGreen else AccentRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (!isVaultActive && reportBudget > 0) "Budget Remaining" else "Remaining Balance",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Text(
                                text = CurrencyHelper.format(displayValue, currencySymbol),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isPositive) AccentGreen else AccentRed
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Easy-to-understand Plain English Financial Feedback
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = (if (isPositive) AccentGreen else AccentRed).copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isPositive) Icons.Default.CheckCircle else Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = if (isPositive) AccentGreen else AccentRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when {
                                        !isVaultActive && reportBudget > 0 && remBudget >= 0 -> "You have ${CurrencyHelper.format(remBudget, currencySymbol)} left to stay within budget."
                                        !isVaultActive && reportBudget > 0 && remBudget < 0 -> "Over budget limit by ${CurrencyHelper.format(-remBudget, currencySymbol)}."
                                        remainingBalance > 0 -> "You saved ${CurrencyHelper.format(remainingBalance, currencySymbol)} during ${selectedPeriod.label.lowercase()}."
                                        remainingBalance == 0.0 && totalSpent > 0 -> "You broke even during this period."
                                        totalSpent == 0.0 && totalIncome == 0.0 -> "No transactions logged in this period."
                                        else -> "Total spent for this period: ${CurrencyHelper.format(totalSpent, currencySymbol)}."
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (isPositive) AccentGreen else AccentRed,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                }
            }
        }

            // 3. Where Money Was Spent (Category Breakdown with Icons on Left Side)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                                    text = "Where Money Was Spent",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Spending broken down by category",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MutedSlate, fontSize = 11.sp)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.PieChart,
                                contentDescription = null,
                                tint = AccentPurple,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (categoryBreakdown.isEmpty()) {
                            Text(
                                text = "No expenses recorded in this period",
                                style = MaterialTheme.typography.bodyMedium.copy(color = MutedSlate),
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            val safeTotal = totalSpent.coerceAtLeast(1.0)
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                categoryBreakdown.forEach { (cat, amount) ->
                                    val percent = ((amount / safeTotal) * 100).toInt()
                                    val fraction = (amount / safeTotal).toFloat().coerceIn(0.02f, 1f)

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
                                                // Category icon on left side
                                                CategoryIconBadge(
                                                    category = cat,
                                                    isIncome = false,
                                                    size = 32.dp,
                                                    iconSize = 16.dp
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = cat,
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
                                                        color = MutedSlate
                                                    )
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        LinearProgressIndicator(
                                            progress = { fraction },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(7.dp)
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

            // 4. Monthly Spending Overview
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                                    text = "Monthly Spending History",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Total spent per month",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MutedSlate, fontSize = 11.sp)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Summarize,
                                contentDescription = null,
                                tint = AccentBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (monthlyBreakdown.isEmpty()) {
                            Text(
                                text = "No monthly spending recorded",
                                style = MaterialTheme.typography.bodyMedium.copy(color = MutedSlate),
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            val maxMonth = monthlyBreakdown.maxOfOrNull { it.value }?.coerceAtLeast(1.0) ?: 1.0

                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                monthlyBreakdown.take(8).forEach { (monthKey, amount) ->
                                    val fraction = (amount / maxMonth).toFloat().coerceIn(0.04f, 1f)
                                    val formattedMonth = formatMonthTitle(monthKey)

                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = formattedMonth,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.Medium
                                                )
                                            )
                                            Text(
                                                text = CurrencyHelper.format(amount, currencySymbol),
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = { fraction },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = AccentCyan,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatementLedgerItemCard(
    expense: Expense,
    currencySymbol: String
) {
    val isCredit = expense.isIncome

    Card(
        modifier = Modifier.fillMaxWidth(),
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "${expense.date} • ${expense.category} • ${expense.paymentMethod}",
                    style = MaterialTheme.typography.bodySmall.copy(color = MutedSlate, fontSize = 11.sp)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = (if (isCredit) AccentGreen else AccentRed).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (isCredit) "CREDIT" else "DEBIT",
                        color = if (isCredit) AccentGreen else AccentRed,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${if (isCredit) "+" else "-"}${CurrencyHelper.format(expense.amount, currencySymbol)}",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isCredit) AccentGreen else AccentRed
                    )
                )
            }
        }
    }
}


private fun formatMonthTitle(key: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM", Locale.US)
        val date = parser.parse(key) ?: return key
        SimpleDateFormat("MMMM yyyy", Locale.US).format(date)
    } catch (_: Exception) {
        key
    }
}

