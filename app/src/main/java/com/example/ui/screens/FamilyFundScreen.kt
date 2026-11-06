package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import com.example.data.model.ExpenseCategories
import com.example.data.model.FamilyFundTransfer
import com.example.data.model.FamilyMember
import com.example.ui.components.AddFamilyMemberDialog
import com.example.ui.components.FamilyFundOverviewSection
import com.example.ui.components.FamilyFundStatementDialog
import com.example.ui.components.RecordFamilyTransferDialog
import com.example.ui.theme.AccentGold
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.MutedSlate
import com.example.ui.util.FamilyFundStatementHelper
import com.example.ui.viewmodel.ExpenseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyFundScreen(
    viewModel: ExpenseViewModel,
    onRequestPinUnlock: () -> Unit
) {
    val context = LocalContext.current
    val isVaultActive by viewModel.isPrivateUnlocked.collectAsState()
    val familyMembers by viewModel.familyMembers.collectAsState()
    val familyTransfers by viewModel.familyTransfers.collectAsState()
    val bankAccounts by viewModel.bankAccounts.collectAsState()
    val currency by viewModel.selectedCurrency.collectAsState()

    var showRecordTransferDialog by remember { mutableStateOf(false) }
    var selectedMemberForTransfer by remember { mutableStateOf<FamilyMember?>(null) }
    var editingTransfer by remember { mutableStateOf<FamilyFundTransfer?>(null) }

    var showAddMemberDialog by remember { mutableStateOf(false) }
    var editingMember by remember { mutableStateOf<FamilyMember?>(null) }

    var showStatementDialog by remember { mutableStateOf(false) }
    var statementMemberId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Fund Management",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Track transfers & member statements",
                            style = MaterialTheme.typography.bodySmall.copy(color = MutedSlate)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        if (!isVaultActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp)
                    .testTag("family_fund_locked_view"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = AccentGold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = "Private Vault Locked",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Unlock your Private Vault to access Family Fund Management.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MutedSlate),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onRequestPinUnlock,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Unlock Private Vault", color = DeepNavy, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("family_fund_screen"),
                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 0.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    FamilyFundOverviewSection(
                        members = familyMembers,
                        transfers = familyTransfers,
                        currencySymbol = currency.symbol,
                        onOpenTransferDialog = { member ->
                            selectedMemberForTransfer = member
                            editingTransfer = null
                            showRecordTransferDialog = true
                        },
                        onOpenAddMemberDialog = {
                            editingMember = null
                            showAddMemberDialog = true
                        },
                        onOpenEditMemberDialog = { member ->
                            editingMember = member
                            showAddMemberDialog = true
                        },
                        onDeleteMember = { memberId ->
                            viewModel.deleteFamilyMember(memberId)
                        },
                        onOpenStatement = { memberId ->
                            statementMemberId = memberId
                            showStatementDialog = true
                        },
                        onPrintMemberStatement = { member ->
                            val memberTransfers = familyTransfers.filter {
                                it.memberId == member.id || it.memberName == member.name
                            }
                            FamilyFundStatementHelper.printStatement(
                                context = context,
                                memberName = member.name,
                                dateRangeLabel = "All Time",
                                transfers = memberTransfers,
                                currencySymbol = currency.symbol
                            )
                        }
                    )
                }
            }
        }
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
