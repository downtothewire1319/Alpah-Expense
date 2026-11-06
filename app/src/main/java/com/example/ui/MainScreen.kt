package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.util.ResponsiveModal
import com.example.ui.components.ExpenseDialog
import com.example.ui.components.PinKeypad
import com.example.ui.screens.BudgetScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FamilyFundScreen
import com.example.ui.screens.PublicExpensesScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentGold
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.PureWhite
import com.example.ui.viewmodel.ExpenseViewModel

enum class NavigationScreen(val label: String, val icon: ImageVector) {
    DASHBOARD("Home", Icons.Default.Dashboard),
    EXPENSES("Expenses", Icons.Default.ReceiptLong),
    FAMILY_FUND("Family Fund", Icons.Default.FamilyRestroom),
    REPORTS("Reports", Icons.Default.Summarize),
    BUDGET("Budget", Icons.Default.Savings),
    SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
fun MainScreen(
    viewModel: ExpenseViewModel,
    onSignOutSuccess: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    var currentScreen by remember { mutableStateOf(NavigationScreen.DASHBOARD) }
    val isVaultActive by viewModel.isPrivateUnlocked.collectAsState()
    val isPinConfigured by viewModel.isPinConfigured.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val customCategories by viewModel.customCategories.collectAsState()
    val bankAccounts by viewModel.bankAccounts.collectAsState()

    var showAddTransactionDialog by remember { mutableStateOf(false) }
    var showStealthPinDialog by remember { mutableStateOf(false) }
    var stealthPinError by remember { mutableStateOf<String?>(null) }

    // Intercept back button to return to Home first
    BackHandler(enabled = currentScreen != NavigationScreen.DASHBOARD) {
        currentScreen = NavigationScreen.DASHBOARD
    }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    val screens = remember(isVaultActive) {
        if (isVaultActive) {
            listOf(
                NavigationScreen.DASHBOARD,
                NavigationScreen.EXPENSES,
                NavigationScreen.FAMILY_FUND,
                NavigationScreen.REPORTS,
                NavigationScreen.SETTINGS
            )
        } else {
            listOf(
                NavigationScreen.DASHBOARD,
                NavigationScreen.EXPENSES,
                NavigationScreen.REPORTS,
                NavigationScreen.BUDGET,
                NavigationScreen.SETTINGS
            )
        }
    }

    LaunchedEffect(isVaultActive) {
        if (isVaultActive && currentScreen == NavigationScreen.BUDGET) {
            currentScreen = NavigationScreen.DASHBOARD
        } else if (!isVaultActive && currentScreen == NavigationScreen.FAMILY_FUND) {
            currentScreen = NavigationScreen.DASHBOARD
        }
    }

    if (isTablet) {
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxHeight()
            ) {
                screens.forEach { screen ->
                    val isSelected = currentScreen == screen
                    val iconTint = if (isVaultActive) AccentGold else AccentBlue
                    val labelText = if (screen == NavigationScreen.FAMILY_FUND && isVaultActive) "Family" else screen.label

                    NavigationRailItem(
                        selected = isSelected,
                        onClick = {
                            viewModel.onUserActivity()
                            currentScreen = screen
                        },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = labelText,
                                tint = if (isSelected) iconTint else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        label = {
                            Text(
                                text = labelText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                        },
                        modifier = Modifier.testTag("nav_rail_${screen.name.lowercase()}")
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                ScreenContent(
                    screen = currentScreen,
                    viewModel = viewModel,
                    coroutineScope = coroutineScope,
                    onNavigateToExpenses = { currentScreen = NavigationScreen.EXPENSES },
                    onNavigateToFamily = { currentScreen = NavigationScreen.FAMILY_FUND },
                    onOpenAddTransaction = { showAddTransactionDialog = true },
                    onSignOutSuccess = onSignOutSuccess,
                    onRequestStealthPin = { showStealthPinDialog = true }
                )
            }
        }
    } else {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    screens.forEach { screen ->
                        val isSelected = currentScreen == screen
                        val activeColor = if (isVaultActive) AccentGold else AccentBlue
                        val labelText = if (screen == NavigationScreen.FAMILY_FUND && isVaultActive) "Family" else screen.label

                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                viewModel.onUserActivity()
                                currentScreen = screen
                            },
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = labelText,
                                    tint = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = labelText,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.sp
                                    )
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = activeColor.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("bottom_nav_${screen.name.lowercase()}")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                ScreenContent(
                    screen = currentScreen,
                    viewModel = viewModel,
                    coroutineScope = coroutineScope,
                    onNavigateToExpenses = { currentScreen = NavigationScreen.EXPENSES },
                    onNavigateToFamily = { currentScreen = NavigationScreen.FAMILY_FUND },
                    onOpenAddTransaction = { showAddTransactionDialog = true },
                    onSignOutSuccess = onSignOutSuccess,
                    onRequestStealthPin = { showStealthPinDialog = true }
                )
            }
        }
    }

    // Add Transaction Dialog (Accessible from Home or FAB)
    if (showAddTransactionDialog) {
        ExpenseDialog(
            isPrivate = isVaultActive,
            customCategories = customCategories,
            bankAccounts = bankAccounts,
            onDismiss = { showAddTransactionDialog = false },

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
                showAddTransactionDialog = false
            },
            onCreateCategory = { newCategory ->
                viewModel.addCustomCategory(newCategory)
            }
        )
    }

    if (showStealthPinDialog) {
        Dialog(
            onDismissRequest = { showStealthPinDialog = false },
            properties = ResponsiveModal.dialogProperties
        ) {
            Surface(
                modifier = ResponsiveModal.modalModifier(isSmall = true),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                PinKeypad(
                    title = if (!isPinConfigured) "Set Security Passcode" else "Security Check",
                    subtitle = "Enter 4-digit passcode to proceed",
                    maxPinLength = 4,
                    errorMessage = stealthPinError,
                    onPinComplete = { pin ->
                        stealthPinError = null
                        if (pin == "RESET_PIN_TRIGGER") {
                            // Reset pin by setting up a fresh new PIN
                            stealthPinError = "Please enter a new 4-digit security PIN"
                            // Temporarily reset config state so user sets new pin
                            return@PinKeypad
                        }
                        if (!isPinConfigured || stealthPinError?.contains("new") == true) {
                            viewModel.setupNewPin(pin) { ok, err ->
                                if (ok) {
                                    showStealthPinDialog = false
                                    stealthPinError = null
                                } else {
                                    stealthPinError = err
                                }
                            }
                        } else {
                            viewModel.verifyAndUnlockPin(pin) { ok, err ->
                                if (ok) {
                                    showStealthPinDialog = false
                                } else {
                                    stealthPinError = err
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ScreenContent(
    screen: NavigationScreen,
    viewModel: ExpenseViewModel,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    onNavigateToExpenses: () -> Unit,
    onNavigateToFamily: () -> Unit,
    onOpenAddTransaction: () -> Unit,
    onSignOutSuccess: () -> Unit,
    onRequestStealthPin: () -> Unit
) {
    when (screen) {
        NavigationScreen.DASHBOARD -> DashboardScreen(
            viewModel = viewModel,
            onNavigateToExpenses = onNavigateToExpenses,
            onNavigateToFamily = onNavigateToFamily,
            onOpenAddTransaction = onOpenAddTransaction,
            onRequestPinUnlock = onRequestStealthPin
        )
        NavigationScreen.EXPENSES -> PublicExpensesScreen(viewModel = viewModel)
        NavigationScreen.FAMILY_FUND -> FamilyFundScreen(
            viewModel = viewModel,
            onRequestPinUnlock = onRequestStealthPin
        )
        NavigationScreen.REPORTS -> ReportsScreen(viewModel = viewModel)
        NavigationScreen.BUDGET -> BudgetScreen(viewModel = viewModel)
        NavigationScreen.SETTINGS -> SettingsScreen(
            viewModel = viewModel,
            coroutineScope = coroutineScope,
            onSignOutSuccess = onSignOutSuccess,
            onUnlockVault = onRequestStealthPin
        )
    }
}
