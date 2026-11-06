package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.TableChart
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ExpenseCategories
import com.example.data.model.FamilyFundTransfer
import com.example.data.model.FamilyMember
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.AccentRed
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.LightNavy
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftWhite
import com.example.ui.util.CurrencyHelper
import com.example.ui.util.FamilyFundStatementHelper
import com.example.ui.util.ResponsiveModal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun FamilyFundStatementDialog(
    initialMemberId: String? = null,
    members: List<FamilyMember>,
    allTransfers: List<FamilyFundTransfer>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onEditTransfer: (FamilyFundTransfer) -> Unit,
    onDeleteTransfer: (FamilyFundTransfer) -> Unit,
    onNewTransfer: () -> Unit
) {
    val context = LocalContext.current

    var selectedMemberId by remember {
        mutableStateOf(initialMemberId ?: "All")
    }
    var selectedPaymentSource by remember { mutableStateOf("All") }
    var selectedPeriod by remember { mutableStateOf("All Time") }

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    var customStartDate by remember { mutableStateOf(todayStr) }
    var customEndDate by remember { mutableStateOf(todayStr) }

    var transferToDelete by remember { mutableStateOf<FamilyFundTransfer?>(null) }

    val periodOptions = listOf(
        "All Time",
        "Custom Date Range"
    )

    val paymentSourcesList = remember(allTransfers) {
        val fromTransfers = allTransfers.map { it.paymentSource }.distinct()
        listOf("All") + (ExpenseCategories.defaultPaymentSources + fromTransfers).distinct()
    }

    // Filter calculation
    val filteredTransfers = remember(
        allTransfers,
        selectedMemberId,
        selectedPaymentSource,
        selectedPeriod,
        customStartDate,
        customEndDate
    ) {
        allTransfers.filter { t ->
            // Member filter
            val memberMatches = selectedMemberId == "All" ||
                    t.memberId == selectedMemberId ||
                    t.memberName.equals(selectedMemberId, ignoreCase = true)
            if (!memberMatches) return@filter false

            // Payment source filter
            val sourceMatches = selectedPaymentSource == "All" ||
                    t.paymentSource.equals(selectedPaymentSource, ignoreCase = true)
            if (!sourceMatches) return@filter false

            // Period filter
            val periodMatches = when (selectedPeriod) {
                "Custom Date Range" -> {
                    val sOk = customStartDate.isBlank() || t.date >= customStartDate
                    val eOk = customEndDate.isBlank() || t.date <= customEndDate
                    sOk && eOk
                }
                else -> true // "All Time"
            }
            periodMatches
        }
    }

    val totalTransferred = filteredTransfers.sumOf { it.amount }
    val transactionCount = filteredTransfers.size
    val distinctMonths = filteredTransfers.map { it.date.take(7) }.distinct().size.coerceAtLeast(1)
    val monthlyAverage = if (filteredTransfers.isNotEmpty()) totalTransferred / distinctMonths else 0.0

    // Selected member name for statement header
    val activeMemberName = remember(selectedMemberId, members) {
        if (selectedMemberId == "All") "All Family Members"
        else members.find { it.id == selectedMemberId || it.name == selectedMemberId }?.name ?: selectedMemberId
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 12.dp)
                .testTag("family_fund_statement_dialog"),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "FAMILY FUND STATEMENT",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            )
                        }
                        Text(
                            text = "$activeMemberName • $selectedPeriod",
                            style = MaterialTheme.typography.bodySmall.copy(color = MutedSlate, fontSize = 11.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Action buttons: Print, Close
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                FamilyFundStatementHelper.printStatement(
                                    context = context,
                                    memberName = activeMemberName,
                                    dateRangeLabel = selectedPeriod,
                                    transfers = filteredTransfers,
                                    currencySymbol = currencySymbol
                                )
                            },
                            modifier = Modifier.size(36.dp).testTag("print_family_statement_button")
                        ) {
                            Icon(Icons.Default.Print, contentDescription = "Print Statement", tint = AccentBlue, modifier = Modifier.size(20.dp))
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp).testTag("close_family_statement_button")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Summary Hero Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepNavy),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "TOTAL TRANSFERRED AMOUNT",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = AccentCyan,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = CurrencyHelper.format(totalTransferred, currencySymbol),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = PureWhite,
                                        fontSize = 22.sp
                                    )
                                )
                            }

                            Button(
                                onClick = onNewTransfer,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp).testTag("dialog_new_transfer_button")
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, tint = DeepNavy, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Transfer", color = DeepNavy, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(PureWhite.copy(alpha = 0.15f))
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Transactions", style = MaterialTheme.typography.labelSmall.copy(color = SoftWhite.copy(alpha = 0.7f), fontSize = 10.sp))
                                Text(
                                    "$transactionCount records",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = PureWhite, fontWeight = FontWeight.Bold)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Monthly Avg", style = MaterialTheme.typography.labelSmall.copy(color = SoftWhite.copy(alpha = 0.7f), fontSize = 10.sp))
                                Text(
                                    CurrencyHelper.format(monthlyAverage, currencySymbol),
                                    style = MaterialTheme.typography.bodyMedium.copy(color = AccentCyan, fontWeight = FontWeight.Bold)
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("Active Member", style = MaterialTheme.typography.labelSmall.copy(color = SoftWhite.copy(alpha = 0.7f), fontSize = 10.sp))
                                Text(
                                    activeMemberName,
                                    style = MaterialTheme.typography.bodyMedium.copy(color = AccentGold, fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Filters Section
                // 1. Period Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    periodOptions.forEach { period ->
                        val isSelected = selectedPeriod == period
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedPeriod = period },
                            label = { Text(period, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentBlue.copy(alpha = 0.2f),
                                selectedLabelColor = AccentBlue
                            ),
                            modifier = Modifier.testTag("filter_period_${period.lowercase().replace(" ", "_")}")
                        )
                    }
                }

                if (selectedPeriod == "Custom Date Range") {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customStartDate,
                            onValueChange = { customStartDate = it },
                            label = { Text("From (YYYY-MM-DD)", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = customEndDate,
                            onValueChange = { customEndDate = it },
                            label = { Text("To (YYYY-MM-DD)", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 2. Family Member Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedMemberId == "All",
                        onClick = { selectedMemberId = "All" },
                        label = { Text("All Members", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentGold.copy(alpha = 0.2f),
                            selectedLabelColor = AccentGold
                        )
                    )

                    members.forEach { m ->
                        val isSelected = selectedMemberId == m.id || selectedMemberId == m.name
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedMemberId = m.id },
                            label = { Text(m.name, fontSize = 11.sp) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Color(m.avatarColorHex)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(m.name.take(1).uppercase(), color = PureWhite, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentGold.copy(alpha = 0.2f),
                                selectedLabelColor = AccentGold
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 3. Payment Source Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    paymentSourcesList.forEach { src ->
                        val isSelected = selectedPaymentSource.equals(src, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedPaymentSource = src },
                            label = { Text(src, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentGreen.copy(alpha = 0.2f),
                                selectedLabelColor = AccentGreen
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Transaction Ledger Table
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transaction History (${filteredTransfers.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (filteredTransfers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No transfers found for selected filters",
                                style = MaterialTheme.typography.bodyMedium.copy(color = MutedSlate)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onNewTransfer,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGold)
                            ) {
                                Text("Record First Transfer", color = DeepNavy, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("family_transfers_list"),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredTransfers, key = { it.id }) { transfer ->
                            FamilyTransferItemCard(
                                transfer = transfer,
                                currencySymbol = currencySymbol,
                                onEdit = { onEditTransfer(transfer) },
                                onDelete = { transferToDelete = transfer }
                            )
                        }
                    }
                }
            }
        }
    }

    if (transferToDelete != null) {
        AlertDialog(
            onDismissRequest = { transferToDelete = null },
            title = { Text("Delete Family Transfer?") },
            text = {
                Text("This will permanently remove this transfer record of ${CurrencyHelper.format(transferToDelete!!.amount, currencySymbol)} to ${transferToDelete!!.memberName}. The linked expense will be removed and the amount will be refunded to ${transferToDelete!!.paymentSource}.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val t = transferToDelete!!
                        transferToDelete = null
                        onDeleteTransfer(t)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Text("Delete & Refund", color = PureWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { transferToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun FamilyTransferItemCard(
    transfer: FamilyFundTransfer,
    currencySymbol: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("family_transfer_item_${transfer.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(AccentBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = transfer.memberName.take(1).uppercase(),
                            color = AccentBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = transfer.memberName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${transfer.date} • ${transfer.paymentSource}",
                            style = MaterialTheme.typography.bodySmall.copy(color = MutedSlate, fontSize = 11.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = CurrencyHelper.format(transfer.amount, currencySymbol),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = AccentBlue
                        )
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = AccentGreen.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = transfer.status,
                            color = AccentGreen,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (transfer.transactionId.isNotBlank() || transfer.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (transfer.transactionId.isNotBlank()) {
                            Text(
                                text = "Ref ID: ${transfer.transactionId}",
                                style = MaterialTheme.typography.bodySmall.copy(color = MutedSlate, fontSize = 10.sp)
                            )
                        }
                        if (transfer.notes.isNotBlank()) {
                            Text(
                                text = transfer.notes,
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row {
                        IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MutedSlate, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AccentRed.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MutedSlate, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AccentRed.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
