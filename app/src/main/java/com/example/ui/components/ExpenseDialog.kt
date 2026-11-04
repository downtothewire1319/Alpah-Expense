package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.window.Dialog
import com.example.ui.util.ResponsiveModal
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategories
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.PureWhite
import com.example.ui.util.CategoryIconBadge
import com.example.ui.util.CategoryIconHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.example.data.model.BankAccount

@Composable
fun ExpenseDialog(
    initialExpense: Expense? = null,
    isPrivate: Boolean,
    customCategories: List<String> = emptyList(),
    bankAccounts: List<BankAccount> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (title: String, amount: Double, category: String, date: String, paymentMethod: String, notes: String?, transactionType: String) -> Unit,
    onCreateCategory: (String) -> Unit = {}
) {
    val todayDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    var isIncomeMode by remember { mutableStateOf(if (isPrivate) (initialExpense?.isIncome == true) else false) }

    // Dynamic category list
    var localCustomCategories by remember { mutableStateOf(customCategories) }

    fun getCategoryList(): List<String> {
        val base = when {
            isIncomeMode && isPrivate -> ExpenseCategories.incomeCategories
            isPrivate -> ExpenseCategories.privateCategories
            else -> ExpenseCategories.publicCategories.filter { it !in ExpenseCategories.incomeCategories }
        }
        return (base + localCustomCategories).distinct()
    }

    val currentCategories = getCategoryList()

    var title by remember { mutableStateOf(initialExpense?.title ?: "") }
    var amountText by remember { mutableStateOf(if (initialExpense != null && initialExpense.amount > 0) initialExpense.amount.toString() else "") }
    var category by remember { mutableStateOf(initialExpense?.category ?: currentCategories.firstOrNull() ?: "Other") }
    var date by remember { mutableStateOf(initialExpense?.date ?: todayDate) }
    var paymentMethod by remember {
        mutableStateOf(
            initialExpense?.paymentMethod
                ?: bankAccounts.firstOrNull()?.name
                ?: "Janata Bank"
        )
    }
    var notes by remember { mutableStateOf(initialExpense?.notes ?: "") }

    var showCategoryPicker by remember { mutableStateOf(false) }
    var showCreateCategoryDialog by remember { mutableStateOf(false) }
    var showPaymentPicker by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }


    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = ResponsiveModal.modalModifier(),
        properties = ResponsiveModal.dialogProperties,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialExpense == null) {
                        if (isIncomeMode) "Record Income" else "Add Expense"
                    } else {
                        if (isIncomeMode) "Edit Income" else "Edit Expense"
                    },
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (isPrivate) {
                    // Toggle between Expense and Income in Vault mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !isIncomeMode,
                            onClick = {
                                isIncomeMode = false
                                val newCats = getCategoryList()
                                if (category !in newCats) {
                                    category = newCats.firstOrNull() ?: "Food"
                                }
                            },
                            label = { Text("Expense", fontWeight = FontWeight.SemiBold) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentBlue,
                                selectedLabelColor = PureWhite
                            )
                        )
                        FilterChip(
                            selected = isIncomeMode,
                            onClick = {
                                isIncomeMode = true
                                val newCats = getCategoryList()
                                if (category !in newCats) {
                                    category = newCats.firstOrNull() ?: "Salary / Income"
                                }
                            },
                            label = { Text("Income", fontWeight = FontWeight.SemiBold) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentGreen,
                                selectedLabelColor = PureWhite
                            )
                        )
                    }
                }

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; validationError = null },
                    label = { Text(if (isIncomeMode) "Income Source / Title *" else "Expense Title *") },
                    placeholder = { Text(if (isIncomeMode) "e.g., Monthly Salary, Bonus, Freelance" else "e.g., Grocery Shopping, Electricity Bill") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_title_input")
                )

                // Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}\$"))) {
                            amountText = it
                            validationError = null
                        }
                    },
                    label = { Text(if (isIncomeMode) "Income Amount (৳) *" else "Expense Amount (৳) *") },
                    placeholder = { Text("0.00") },
                    leadingIcon = {
                        Text(
                            text = "৳",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isIncomeMode) AccentGreen else AccentBlue
                            ),
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_amount_input")
                )

                // Quick Increment Pills for Mobile Convenience
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(500.0, 1000.0, 2000.0, 5000.0).forEach { quickAmt ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val cur = amountText.toDoubleOrNull() ?: 0.0
                                    amountText = (cur + quickAmt).toInt().toString()
                                }
                        ) {
                            Text(
                                text = "+${quickAmt.toInt()}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(vertical = 6.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }


                // Category Selector Card (with related icon on the left side)
                Column {
                    Text(
                        text = "Category *",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { showCategoryPicker = true }
                            .testTag("expense_category_selector"),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CategoryIconBadge(
                                    category = category,
                                    isIncome = isIncomeMode,
                                    size = 36.dp,
                                    iconSize = 20.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Change Category",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Date
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (YYYY-MM-DD) *") },
                    trailingIcon = {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Date")
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_date_input")
                )

                // Payment Method Card
                Column {
                    Text(
                        text = if (isIncomeMode) "Received Via" else "Payment Method",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { showPaymentPicker = true }
                            .testTag("expense_payment_method_selector"),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Payment,
                                    contentDescription = null,
                                    tint = AccentBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = paymentMethod,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Change Payment Method",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("Additional information or remarks...") },
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_notes_input")
                )

                if (validationError != null) {
                    Text(
                        text = validationError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        validationError = "Please enter a title"
                        return@Button
                    }
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0.0) {
                        validationError = "Please enter a valid amount"
                        return@Button
                    }
                    onSave(
                        title.trim(),
                        amount,
                        category,
                        date,
                        paymentMethod,
                        notes.trim().ifBlank { null },
                        if (isIncomeMode) "income" else "expense"
                    )
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isIncomeMode) AccentGreen else AccentBlue)
            ) {
                Text(
                    text = if (initialExpense == null) {
                        if (isIncomeMode) "Save Income" else "Save Expense"
                    } else "Update",
                    color = PureWhite,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )

    // Category Picker Dialog with icon on left side of every category & Create Category option
    if (showCategoryPicker) {
        Dialog(
            onDismissRequest = { showCategoryPicker = false },
            properties = ResponsiveModal.dialogProperties
        ) {
            Card(
                modifier = ResponsiveModal.modalModifier(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Category",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        IconButton(onClick = { showCategoryPicker = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // "Create New Category" button
                    Button(
                        onClick = {
                            showCreateCategoryDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_new_category_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "+ Create New Category",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(currentCategories) { cat ->
                            val isSelected = cat == category
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        category = cat
                                        showCategoryPicker = false
                                    },
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CategoryIconBadge(
                                            category = cat,
                                            isIncome = isIncomeMode,
                                            size = 36.dp,
                                            iconSize = 18.dp
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = cat,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
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

    // Create New Category Dialog
    if (showCreateCategoryDialog) {
        var newCategoryName by remember { mutableStateOf("") }
        var createError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showCreateCategoryDialog = false },
            modifier = ResponsiveModal.modalModifier(isSmall = true),
            properties = ResponsiveModal.dialogProperties,
            title = {
                Text(
                    text = "Create New Category",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter a name for the new category:",
                        style = MaterialTheme.typography.bodySmall.copy(color = MutedSlate)
                    )
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = {
                            newCategoryName = it
                            createError = null
                        },
                        label = { Text("Category Name") },
                        placeholder = { Text("e.g., School Fees, Gym, Pet Food") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_category_input")
                    )
                    if (createError != null) {
                        Text(
                            text = createError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newCategoryName.trim()
                        if (trimmed.isBlank()) {
                            createError = "Please enter category name"
                            return@Button
                        }
                        if (trimmed.length > 30) {
                            createError = "Name cannot exceed 30 characters"
                            return@Button
                        }
                        // Add to local list and trigger callback
                        if (!localCustomCategories.contains(trimmed)) {
                            localCustomCategories = localCustomCategories + trimmed
                        }
                        onCreateCategory(trimmed)
                        category = trimmed
                        showCreateCategoryDialog = false
                        showCategoryPicker = false
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save Category")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showCreateCategoryDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Payment Method Picker Dialog
    if (showPaymentPicker) {
        Dialog(
            onDismissRequest = { showPaymentPicker = false },
            properties = ResponsiveModal.dialogProperties
        ) {
            Card(
                modifier = ResponsiveModal.modalModifier(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = if (isIncomeMode) "Deposit To Account" else "Pay From Bank / Wallet",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val effectiveMethods = if (bankAccounts.isNotEmpty()) {
                        (bankAccounts.map { it.name } + ExpenseCategories.paymentMethods).distinct()
                    } else {
                        ExpenseCategories.paymentMethods
                    }

                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        effectiveMethods.forEach { method ->
                            val isSelected = method == paymentMethod
                            val matchingAccount = bankAccounts.find { it.name.equals(method, ignoreCase = true) }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        paymentMethod = method
                                        showPaymentPicker = false
                                    },
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = method,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                            )
                                        )
                                        if (matchingAccount != null) {
                                            Text(
                                                text = "Current Balance: ৳${matchingAccount.balance.toInt()}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = AccentGreen,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            )
                                        }
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary
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
