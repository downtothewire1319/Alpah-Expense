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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Diversity1
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FamilyFundTransfer
import com.example.data.model.FamilyMember
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
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftWhite
import com.example.ui.util.CurrencyHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VaultFamilyFundOverviewCard(
    members: List<FamilyMember>,
    transfers: List<FamilyFundTransfer>,
    currencySymbol: String,
    onNavigateToFamily: () -> Unit
) {
    val totalTransferred = remember(transfers) { transfers.sumOf { it.amount } }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .clickable { onNavigateToFamily() }
            .testTag("vault_family_fund_overview_card"),
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
                                imageVector = Icons.Default.FamilyRestroom,
                                contentDescription = null,
                                tint = AccentGold,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Family Transfer",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Cumulative transfers to family members",
                            style = MaterialTheme.typography.bodySmall.copy(color = MutedSlate, fontSize = 10.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Text(
                    text = "Manage ↗",
                    style = MaterialTheme.typography.labelSmall.copy(color = AccentGold, fontWeight = FontWeight.Bold),
                    modifier = Modifier.clickable { onNavigateToFamily() }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text("Total Transferred", style = MaterialTheme.typography.labelSmall.copy(color = MutedSlate, fontSize = 10.sp))
                    Text(
                        CurrencyHelper.format(totalTransferred, currencySymbol),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = AccentGold
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AccentGold.copy(alpha = 0.15f)
                ) {
                    Text(
                        "${members.size} Members",
                        color = AccentGold,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (members.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    members.forEach { member ->
                        val memberTotal = transfers.filter { it.memberId == member.id || it.memberName == member.name }.sumOf { it.amount }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.clickable { onNavigateToFamily() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(member.avatarColorHex)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = member.name.take(1).uppercase(),
                                        color = PureWhite,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = member.name,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                        maxLines = 1
                                    )
                                    Text(
                                        text = CurrencyHelper.format(memberTotal, currencySymbol),
                                        style = MaterialTheme.typography.labelSmall.copy(color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 10.sp)
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

@Composable
fun FamilyFundOverviewSection(
    members: List<FamilyMember>,
    transfers: List<FamilyFundTransfer>,
    currencySymbol: String,
    onOpenTransferDialog: (FamilyMember?) -> Unit,
    onOpenAddMemberDialog: () -> Unit,
    onOpenEditMemberDialog: (FamilyMember) -> Unit,
    onDeleteMember: (String) -> Unit,
    onOpenStatement: (memberId: String?) -> Unit,
    onPrintMemberStatement: (FamilyMember) -> Unit
) {
    val thisMonthKey = remember { SimpleDateFormat("yyyy-MM", Locale.US).format(Date()) }
    val thisYearKey = remember { SimpleDateFormat("yyyy", Locale.US).format(Date()) }

    val totalFamilyExpenses = remember(transfers) { transfers.sumOf { it.amount } }
    val thisMonthTransfers = remember(transfers, thisMonthKey) {
        transfers.filter { it.date.startsWith(thisMonthKey) }.sumOf { it.amount }
    }
    val thisYearTransfers = remember(transfers, thisYearKey) {
        transfers.filter { it.date.startsWith(thisYearKey) }.sumOf { it.amount }
    }
    val totalTransactions = transfers.size
    val totalMembersCount = members.size

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .testTag("family_fund_overview_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = AccentGold.copy(alpha = 0.15f),
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.FamilyRestroom,
                                contentDescription = null,
                                tint = AccentGold,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Fund Management",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Transfer to family members",
                            style = MaterialTheme.typography.bodySmall.copy(color = MutedSlate, fontSize = 10.sp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { onOpenTransferDialog(null) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp).testTag("quick_send_money_button")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, tint = DeepNavy, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Send", color = DeepNavy, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    Text(
                        text = "Statement ↗",
                        style = MaterialTheme.typography.labelSmall.copy(color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 11.sp),
                        modifier = Modifier
                            .clickable { onOpenStatement("All") }
                            .padding(vertical = 6.dp)
                            .testTag("open_family_statements_button")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Summary Grid Card
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = DeepNavy
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Total Family Expenses",
                                style = MaterialTheme.typography.labelSmall.copy(color = AccentCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = CurrencyHelper.format(totalFamilyExpenses, currencySymbol),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PureWhite,
                                    fontSize = 18.sp
                                )
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = AccentGold.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "$totalTransactions Transfers",
                                color = AccentGold,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(PureWhite.copy(alpha = 0.12f))
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("This Month", style = MaterialTheme.typography.labelSmall.copy(color = SoftWhite.copy(alpha = 0.7f), fontSize = 9.sp))
                            Text(
                                CurrencyHelper.format(thisMonthTransfers, currencySymbol),
                                style = MaterialTheme.typography.bodySmall.copy(color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("This Year", style = MaterialTheme.typography.labelSmall.copy(color = SoftWhite.copy(alpha = 0.7f), fontSize = 9.sp))
                            Text(
                                CurrencyHelper.format(thisYearTransfers, currencySymbol),
                                style = MaterialTheme.typography.bodySmall.copy(color = AccentCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Family Members", style = MaterialTheme.typography.labelSmall.copy(color = SoftWhite.copy(alpha = 0.7f), fontSize = 9.sp))
                            Text(
                                "$totalMembersCount Members",
                                style = MaterialTheme.typography.bodySmall.copy(color = AccentGold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            )
                        }
                    }
                }
            }

            // Member Wise Breakdown Progress
            if (members.isNotEmpty() && totalFamilyExpenses > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Spending Breakdown by Member",
                    style = MaterialTheme.typography.labelSmall.copy(color = MutedSlate, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                ) {
                    members.forEach { m ->
                        val memberTotal = transfers.filter { it.memberId == m.id || it.memberName == m.name }.sumOf { it.amount }
                        if (memberTotal > 0) {
                            val weight = (memberTotal / totalFamilyExpenses).toFloat().coerceAtLeast(0.02f)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(weight)
                                    .background(Color(m.avatarColorHex))
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Family Member Cards Title & Horizontal List
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Family Members (${members.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )

                Text(
                    text = "+ Add Member",
                    style = MaterialTheme.typography.labelSmall.copy(color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 11.sp),
                    modifier = Modifier
                        .clickable { onOpenAddMemberDialog() }
                        .testTag("add_family_member_link")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Horizontal Scroll of Individual Member Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                members.forEach { member ->
                    val memberTransfers = transfers.filter { it.memberId == member.id || it.memberName == member.name }
                    val memberTotal = memberTransfers.sumOf { it.amount }
                    val lastTransfer = memberTransfers.maxByOrNull { it.date }

                    FamilyMemberCard(
                        member = member,
                        totalTransferred = memberTotal,
                        transactionCount = memberTransfers.size,
                        lastTransferDate = lastTransfer?.date,
                        currencySymbol = currencySymbol,
                        onViewStatement = { onOpenStatement(member.id) },
                        onPrintStatement = { onPrintMemberStatement(member) },
                        onEdit = { onOpenEditMemberDialog(member) },
                        onDelete = { onDeleteMember(member.id) }
                    )
                }

                // Add Member Placeholder Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier
                        .width(130.dp)
                        .height(175.dp)
                        .clickable { onOpenAddMemberDialog() }
                        .testTag("add_member_card_button")
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = AccentBlue.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = AccentBlue)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Add Member",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Brother, Sister, etc.",
                            style = MaterialTheme.typography.labelSmall.copy(color = MutedSlate, fontSize = 9.sp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FamilyMemberCard(
    member: FamilyMember,
    totalTransferred: Double,
    transactionCount: Int,
    lastTransferDate: String?,
    currencySymbol: String,
    onViewStatement: () -> Unit,
    onPrintStatement: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(180.dp)
            .testTag("family_member_card_${member.name.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Member Avatar & Name + Edit/Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(member.avatarColorHex)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = member.name.take(1).uppercase(),
                            color = PureWhite,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = member.name,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = member.relationship,
                            style = MaterialTheme.typography.labelSmall.copy(color = MutedSlate, fontSize = 9.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(24.dp).testTag("edit_member_${member.name}")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AccentBlue, modifier = Modifier.size(14.dp))
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp).testTag("delete_member_${member.name}")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AccentRed, modifier = Modifier.size(14.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Total Transferred",
                style = MaterialTheme.typography.labelSmall.copy(color = MutedSlate, fontSize = 9.sp)
            )
            Text(
                text = CurrencyHelper.format(totalTransferred, currencySymbol),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = AccentBlue,
                    fontSize = 14.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$transactionCount transfers",
                    style = MaterialTheme.typography.bodySmall.copy(color = MutedSlate, fontSize = 9.sp)
                )
                Text(
                    text = lastTransferDate?.takeLast(5) ?: "None",
                    style = MaterialTheme.typography.bodySmall.copy(color = MutedSlate, fontSize = 9.sp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onViewStatement,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.weight(1f).height(28.dp).testTag("member_view_statement_${member.name.lowercase().replace(" ", "_")}")
                ) {
                    Text("Statement", color = PureWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(28.dp).clickable { onPrintStatement() }.testTag("member_print_statement_${member.name.lowercase().replace(" ", "_")}")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = "Print Statement",
                            tint = AccentBlue,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
