package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.util.ResponsiveModal
import com.example.data.model.Expense
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.LightBorder
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftWhite
import com.example.ui.util.CurrencyHelper

@Composable
fun InvoiceDialog(
    expense: Expense,
    currencySymbol: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val invoiceNumber = "INV-" + expense.id.takeLast(6).uppercase()

    Dialog(
        onDismissRequest = onDismiss,
        properties = ResponsiveModal.dialogProperties
    ) {
        Card(
            modifier = ResponsiveModal.modalModifier()
                .testTag("invoice_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "EXPENSE RECEIPT / INVOICE",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = DeepNavy,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = invoiceNumber,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = AccentBlue,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MutedSlate)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = LightBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))

                // Invoice Meta Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Date", style = MaterialTheme.typography.labelSmall.copy(color = MutedSlate))
                        Text(expense.date, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = DeepNavy))
                    }
                    Column {
                        Text("Category", style = MaterialTheme.typography.labelSmall.copy(color = MutedSlate))
                        Text(expense.category, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = DeepNavy))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Payment Method", style = MaterialTheme.typography.labelSmall.copy(color = MutedSlate))
                        Text(expense.paymentMethod, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = DeepNavy))
                    }
                }

                if (expense.creatorName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Logged By: ${expense.creatorName}", style = MaterialTheme.typography.bodySmall.copy(color = MutedSlate))
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Invoice Table
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SoftWhite,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("DESCRIPTION", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MutedSlate))
                            Text("AMOUNT", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MutedSlate))
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = LightBorder, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = expense.title,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = DeepNavy)
                                )
                                if (!expense.notes.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = expense.notes,
                                        style = MaterialTheme.typography.bodySmall.copy(color = MutedSlate)
                                    )
                                }
                            }
                            Text(
                                text = CurrencyHelper.format(expense.amount, currencySymbol),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, color = DeepNavy)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = LightBorder, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("TOTAL PAID", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = DeepNavy))
                            Text(
                                text = CurrencyHelper.format(expense.amount, currencySymbol),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = AccentBlue
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // PAID Stamp Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AccentGreen.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, AccentGreen)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "VERIFIED & PAID",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = AccentGreen,
                                    letterSpacing = 1.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons: Print & Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            shareInvoiceAsText(context, expense, currencySymbol)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("share_invoice_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share")
                    }

                    Button(
                        onClick = {
                            printInvoice(context, expense, currencySymbol)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("print_invoice_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepNavy)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp), tint = PureWhite)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Print Invoice", color = PureWhite)
                    }
                }
            }
        }
    }
}

fun shareInvoiceAsText(context: Context, expense: Expense, currencySymbol: String) {
    val invoiceText = buildString {
        appendLine("========================================")
        appendLine("       EXPENSE RECEIPT / INVOICE        ")
        appendLine("========================================")
        appendLine("Invoice No: INV-${expense.id.takeLast(6).uppercase()}")
        appendLine("Date: ${expense.date}")
        appendLine("Category: ${expense.category}")
        appendLine("Payment Method: ${expense.paymentMethod}")
        if (expense.creatorName.isNotBlank()) {
            appendLine("Logged By: ${expense.creatorName}")
        }
        appendLine("----------------------------------------")
        appendLine("Description: ${expense.title}")
        if (!expense.notes.isNullOrBlank()) {
            appendLine("Notes: ${expense.notes}")
        }
        appendLine("----------------------------------------")
        appendLine("TOTAL PAID: ${CurrencyHelper.format(expense.amount, currencySymbol)}")
        appendLine("STATUS: PAID & CLEARED")
        appendLine("========================================")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Invoice INV-${expense.id.takeLast(6).uppercase()}")
        putExtra(Intent.EXTRA_TEXT, invoiceText)
    }
    context.startActivity(Intent.createChooser(intent, "Share Invoice Receipt"))
}

fun printInvoice(context: Context, expense: Expense, currencySymbol: String) {
    try {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? android.print.PrintManager
        if (printManager != null) {
            val htmlDocument = """
                <html>
                <head>
                    <style>
                        body { font-family: 'Helvetica Neue', Arial, sans-serif; padding: 24px; color: #0f172a; }
                        .header { border-bottom: 2px solid #2563eb; padding-bottom: 12px; margin-bottom: 20px; }
                        .title { font-size: 20px; font-weight: bold; color: #0a192f; }
                        .inv-no { font-size: 16px; color: #2563eb; font-weight: bold; font-family: monospace; }
                        .table { width: 100%; border-collapse: collapse; margin-top: 16px; }
                        .table th { background-color: #f8fafc; padding: 10px; text-align: left; border-bottom: 1px solid #e2e8f0; font-size: 12px; color: #64748b; }
                        .table td { padding: 12px 10px; border-bottom: 1px solid #f1f5f9; font-size: 14px; }
                        .total-row { font-size: 18px; font-weight: bold; color: #2563eb; }
                        .badge { display: inline-block; padding: 4px 12px; border: 1px solid #10b981; color: #10b981; border-radius: 4px; font-size: 12px; font-weight: bold; margin-top: 20px; }
                    </style>
                </head>
                <body>
                    <div class="header">
                        <div class="title">EXPENSE INVOICE / RECEIPT</div>
                        <div class="inv-no">INV-${expense.id.takeLast(6).uppercase()}</div>
                        <p style="margin: 4px 0; color: #64748b; font-size: 13px;">Date: ${expense.date} | Category: ${expense.category} | Method: ${expense.paymentMethod}</p>
                    </div>
                    <table class="table">
                        <thead>
                            <tr><th>DESCRIPTION</th><th style="text-align: right;">AMOUNT</th></tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td><strong>${expense.title}</strong><br><small style="color: #64748b;">${expense.notes ?: ""}</small></td>
                                <td style="text-align: right; font-weight: bold;">${CurrencyHelper.format(expense.amount, currencySymbol)}</td>
                            </tr>
                            <tr class="total-row">
                                <td>TOTAL AMOUNT</td>
                                <td style="text-align: right;">${CurrencyHelper.format(expense.amount, currencySymbol)}</td>
                            </tr>
                        </tbody>
                    </table>
                    <div class="badge">STATUS: PAID &amp; CLEARED</div>
                </body>
                </html>
            """.trimIndent()

            val webView = android.webkit.WebView(context)
            webView.loadDataWithBaseURL(null, htmlDocument, "text/html", "UTF-8", null)
            val printAdapter = webView.createPrintDocumentAdapter("Invoice_${expense.id}")
            printManager.print("Expense_Invoice_${expense.id}", printAdapter, android.print.PrintAttributes.Builder().build())
            return
        }
    } catch (_: Exception) {}

    // Fallback if PrintManager not available
    shareInvoiceAsText(context, expense, currencySymbol)
}
