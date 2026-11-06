package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import com.example.data.model.ExpenseCategories
import com.example.data.model.FamilyFundTransfer
import com.example.data.model.FamilyMember
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.PureWhite
import com.example.ui.util.CurrencyHelper
import com.example.ui.util.ResponsiveModal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecordFamilyTransferDialog(
    members: List<FamilyMember>,
    paymentSources: List<String>,
    initialMember: FamilyMember? = null,
    initialTransfer: FamilyFundTransfer? = null,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (
        memberId: String,
        memberName: String,
        amount: Double,
        date: String,
        paymentSource: String,
        transactionId: String,
        notes: String
    ) -> Unit,
    onAddNewMember: () -> Unit
) {
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

    var selectedMember by remember {
        mutableStateOf(
            if (initialTransfer != null) {
                members.find { it.id == initialTransfer.memberId || it.name == initialTransfer.memberName }
                    ?: members.firstOrNull()
            } else if (initialMember != null) {
                members.find { it.id == initialMember.id || it.name == initialMember.name }
                    ?: initialMember
            } else {
                members.firstOrNull()
            }
        )
    }

    var amountText by remember {
        mutableStateOf(
            if (initialTransfer != null && initialTransfer.amount > 0) {
                if (initialTransfer.amount % 1.0 == 0.0) initialTransfer.amount.toLong().toString()
                else initialTransfer.amount.toString()
            } else ""
        )
    }

    var dateText by remember { mutableStateOf(initialTransfer?.date?.ifBlank { today } ?: today) }
    var selectedPaymentSource by remember {
        mutableStateOf(initialTransfer?.paymentSource?.ifBlank { "Janata Bank" } ?: "Janata Bank")
    }
    var customSourceText by remember { mutableStateOf("") }
    var isCustomSource by remember { mutableStateOf(false) }

    var trxIdText by remember { mutableStateOf(initialTransfer?.transactionId ?: "") }
    var notesText by remember { mutableStateOf(initialTransfer?.notes ?: "") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    val allSources = remember(paymentSources) {
        (ExpenseCategories.defaultPaymentSources + paymentSources).distinct()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = ResponsiveModal.dialogProperties
    ) {
        Surface(
            modifier = ResponsiveModal.modalModifier(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
                    .testTag("record_family_transfer_dialog")
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (initialTransfer != null) "Edit Family Transfer" else "Record Family Transfer",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "Auto-recorded as Expense & deducted from account",
                            style = MaterialTheme.typography.bodySmall.copy(color = MutedSlate, fontSize = 11.sp)
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = AccentGold.copy(alpha = 0.15f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = null,
                                tint = AccentGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 1. Select Family Member
                Text(
                    text = "Select Family Member *",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    members.forEach { m ->
                        val isSelected = selectedMember?.id == m.id || (selectedMember == null && m.name == selectedMember?.name)
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedMember = m },
                            label = { Text(m.name, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(Color(m.avatarColorHex)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = m.name.take(1).uppercase(),
                                        color = PureWhite,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentBlue.copy(alpha = 0.2f),
                                selectedLabelColor = AccentBlue
                            ),
                            modifier = Modifier.testTag("select_member_${m.name.lowercase().replace(" ", "_")}")
                        )
                    }

                    // Add custom member button
                    OutlinedButton(
                        onClick = onAddNewMember,
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Amount Field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it.filter { char -> char.isDigit() || char == '.' }
                        errorMessage = null
                    },
                    label = { Text("Transfer Amount ($currencySymbol) *") },
                    placeholder = { Text("e.g. 10000") },
                    leadingIcon = {
                        Text(
                            text = currencySymbol,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = AccentGold
                            ),
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("family_transfer_amount_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                val parsedAmt = amountText.toDoubleOrNull() ?: 0.0
                if (parsedAmt > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Formatted: ${CurrencyHelper.format(parsedAmt, currencySymbol)}",
                        style = MaterialTheme.typography.bodySmall.copy(color = AccentGreen, fontSize = 11.sp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Transfer Date
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("Transfer Date (YYYY-MM-DD) *") },
                    leadingIcon = {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("family_transfer_date_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 4. Payment Source / From Account
                Text(
                    text = "Payment Source / From Account *",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    allSources.forEach { source ->
                        val isSelected = !isCustomSource && selectedPaymentSource.equals(source, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                isCustomSource = false
                                selectedPaymentSource = source
                            },
                            label = { Text(source, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentGold.copy(alpha = 0.2f),
                                selectedLabelColor = AccentGold
                            )
                        )
                    }

                    FilterChip(
                        selected = isCustomSource,
                        onClick = { isCustomSource = true },
                        label = { Text("Other", fontSize = 12.sp) }
                    )
                }

                if (isCustomSource) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customSourceText,
                        onValueChange = { customSourceText = it },
                        label = { Text("Specify Custom Account / Source") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 5. Transaction ID (Optional)
                OutlinedTextField(
                    value = trxIdText,
                    onValueChange = { trxIdText = it },
                    label = { Text("Transaction ID / Reference (Optional)") },
                    placeholder = { Text("e.g. TRX-948204 or Bank Ref") },
                    leadingIcon = {
                        Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("family_transfer_trx_id_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 6. Note / Description (Optional)
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Note / Description (Optional)") },
                    placeholder = { Text("e.g. Monthly family maintenance, Medical bill support") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("family_transfer_notes_input"),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2,
                    maxLines = 3
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val member = selectedMember
                            if (member == null) {
                                errorMessage = "Please select a family member"
                                return@Button
                            }
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (amt <= 0.0) {
                                errorMessage = "Please enter a valid amount greater than 0"
                                return@Button
                            }
                            val source = if (isCustomSource && customSourceText.isNotBlank()) customSourceText.trim() else selectedPaymentSource
                            if (source.isBlank()) {
                                errorMessage = "Please select or enter a payment source"
                                return@Button
                            }

                            onSave(
                                member.id,
                                member.name,
                                amt,
                                dateText.trim(),
                                source,
                                trxIdText.trim(),
                                notesText.trim()
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("submit_family_transfer_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = DeepNavy, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (initialTransfer != null) "Update" else "Transfer",
                            color = DeepNavy,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
