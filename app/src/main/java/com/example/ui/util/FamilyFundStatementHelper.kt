package com.example.ui.util

import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.FileProvider
import com.example.data.model.FamilyFundTransfer
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FamilyFundStatementHelper {

    fun printStatement(
        context: Context,
        memberName: String,
        dateRangeLabel: String,
        transfers: List<FamilyFundTransfer>,
        currencySymbol: String,
        userName: String = ""
    ) {
        val htmlContent = buildHtmlStatement(
            memberName = memberName,
            dateRangeLabel = dateRangeLabel,
            transfers = transfers,
            currencySymbol = currencySymbol,
            userName = userName
        )

        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager != null) {
            val webView = WebView(context)
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    val printAdapter = webView.createPrintDocumentAdapter("Family_Fund_Statement_${System.currentTimeMillis()}")
                    printManager.print("Family Fund Transfer Statement", printAdapter, PrintAttributes.Builder().build())
                }
            }
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        } else {
            shareTextStatement(context, memberName, dateRangeLabel, transfers, currencySymbol)
        }
    }

    fun exportExcelCsv(
        context: Context,
        memberName: String,
        dateRangeLabel: String,
        transfers: List<FamilyFundTransfer>,
        currencySymbol: String
    ): Result<Unit> = runCatching {
        val totalAmount = transfers.sumOf { it.amount }
        val dateGen = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())

        val sb = StringBuilder()
        // UTF-8 BOM so Microsoft Excel and Google Sheets render Bengali text accurately
        sb.append('\uFEFF')
        sb.appendLine("FAMILY FUND TRANSFER STATEMENT")
        sb.appendLine("Recipient / Member:,\"$memberName\"")
        sb.appendLine("Date Range:,\"$dateRangeLabel\"")
        sb.appendLine("Statement Generated:,\"$dateGen\"")
        sb.appendLine("Total Transferred:,\"${CurrencyHelper.format(totalAmount, currencySymbol)}\"")
        sb.appendLine("Total Transactions:,\"${transfers.size}\"")
        sb.appendLine("")

        // Payment Source Breakdown
        sb.appendLine("PAYMENT SOURCE BREAKDOWN")
        sb.appendLine("Source,Total Amount (BDT),Percentage")
        val sourceBreakdown = transfers.groupBy { it.paymentSource }
        sourceBreakdown.forEach { (source, list) ->
            val srcTotal = list.sumOf { it.amount }
            val pct = if (totalAmount > 0) String.format(Locale.US, "%.1f%%", (srcTotal / totalAmount) * 100) else "0%"
            sb.appendLine("\"$source\",${srcTotal},\"$pct\"")
        }
        sb.appendLine("")

        // Detailed Transactions
        sb.appendLine("DETAILED TRANSACTION TABLE")
        sb.appendLine("Date,Recipient Member,Amount (BDT),Payment Source,Transaction ID,Status,Note / Description")
        transfers.sortedByDescending { it.date }.forEach { t ->
            val noteClean = t.notes.replace("\"", "\"\"")
            val trxIdClean = t.transactionId.replace("\"", "\"\"")
            sb.appendLine("\"${t.date}\",\"${t.memberName}\",${t.amount},\"${t.paymentSource}\",\"$trxIdClean\",\"${t.status}\",\"$noteClean\"")
        }

        val fileName = "Family_Fund_Statement_${System.currentTimeMillis()}.csv"
        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use { out ->
            out.write(sb.toString().toByteArray(Charsets.UTF_8))
        }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Family Fund Transfer Statement - $memberName ($dateRangeLabel)")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Export Statement as Excel (CSV)"))
    }

    private fun shareTextStatement(
        context: Context,
        memberName: String,
        dateRangeLabel: String,
        transfers: List<FamilyFundTransfer>,
        currencySymbol: String
    ) {
        val totalAmount = transfers.sumOf { it.amount }
        val dateGen = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())

        val sb = StringBuilder()
        sb.appendLine("==========================================")
        sb.appendLine("     FAMILY FUND TRANSFER STATEMENT       ")
        sb.appendLine("==========================================")
        sb.appendLine("Recipient: $memberName")
        sb.appendLine("Scope: $dateRangeLabel")
        sb.appendLine("Generated: $dateGen")
        sb.appendLine("Total Transferred: ${CurrencyHelper.format(totalAmount, currencySymbol)}")
        sb.appendLine("Total Transactions: ${transfers.size}")
        sb.appendLine("------------------------------------------")
        sb.appendLine("PAYMENT SOURCE BREAKDOWN:")
        transfers.groupBy { it.paymentSource }.forEach { (source, list) ->
            sb.appendLine("  • $source: ${CurrencyHelper.format(list.sumOf { it.amount }, currencySymbol)}")
        }
        sb.appendLine("------------------------------------------")
        sb.appendLine("TRANSACTIONS:")
        transfers.sortedByDescending { it.date }.forEach { t ->
            sb.appendLine("${t.date} | ${t.memberName} | ${CurrencyHelper.format(t.amount, currencySymbol)}")
            sb.appendLine("   Source: ${t.paymentSource} | Status: ${t.status}")
            if (t.transactionId.isNotBlank()) sb.appendLine("   TrxID: ${t.transactionId}")
            if (t.notes.isNotBlank()) sb.appendLine("   Note: ${t.notes}")
            sb.appendLine("")
        }

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, sb.toString())
            putExtra(Intent.EXTRA_SUBJECT, "Family Fund Statement - $memberName")
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share Statement"))
    }

    private fun buildHtmlStatement(
        memberName: String,
        dateRangeLabel: String,
        transfers: List<FamilyFundTransfer>,
        currencySymbol: String,
        userName: String = ""
    ): String {
        val effectiveUserName = userName.ifBlank { com.google.firebase.Firebase.auth.currentUser?.displayName ?: "Account Holder" }
        val totalAmount = transfers.sumOf { it.amount }
        val count = transfers.size
        val dateGen = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.US).format(Date())
        val monthlyAvg = if (transfers.isNotEmpty()) {
            val distinctMonths = transfers.map { it.date.take(7) }.distinct().size.coerceAtLeast(1)
            totalAmount / distinctMonths
        } else 0.0

        val sourceBreakdown = transfers.groupBy { it.paymentSource }
        val sourceRows = sourceBreakdown.map { (source, list) ->
            val sum = list.sumOf { it.amount }
            val pct = if (totalAmount > 0) (sum / totalAmount * 100).toInt() else 0
            """
            <tr>
                <td style="padding: 8px 12px; font-weight: bold; border-bottom: 1px solid #e0e0e0;">$source</td>
                <td style="padding: 8px 12px; text-align: right; border-bottom: 1px solid #e0e0e0;">${list.size}</td>
                <td style="padding: 8px 12px; text-align: right; font-weight: bold; color: #1976D2; border-bottom: 1px solid #e0e0e0;">${CurrencyHelper.format(sum, currencySymbol)}</td>
                <td style="padding: 8px 12px; text-align: right; border-bottom: 1px solid #e0e0e0;">$pct%</td>
            </tr>
            """.trimIndent()
        }.joinToString("\n")

        val tableRows = transfers.sortedByDescending { it.date }.mapIndexed { idx, t ->
            val rowBg = if (idx % 2 == 0) "#ffffff" else "#f8f9fa"
            """
            <tr style="background-color: $rowBg;">
                <td style="padding: 10px 12px; border-bottom: 1px solid #e0e0e0; font-size: 13px;">${t.date}</td>
                <td style="padding: 10px 12px; border-bottom: 1px solid #e0e0e0; font-weight: 600; font-size: 13px;">${t.memberName}</td>
                <td style="padding: 10px 12px; border-bottom: 1px solid #e0e0e0; text-align: right; font-weight: bold; color: #0d47a1; font-size: 14px;">${CurrencyHelper.format(t.amount, currencySymbol)}</td>
                <td style="padding: 10px 12px; border-bottom: 1px solid #e0e0e0; font-size: 13px;">${t.paymentSource}</td>
                <td style="padding: 10px 12px; border-bottom: 1px solid #e0e0e0; font-family: monospace; font-size: 12px; color: #555;">${if (t.transactionId.isNotBlank()) t.transactionId else "-"}</td>
                <td style="padding: 10px 12px; border-bottom: 1px solid #e0e0e0; text-align: center;"><span style="background-color: #e8f5e9; color: #2e7d32; padding: 3px 8px; border-radius: 12px; font-size: 11px; font-weight: bold;">${t.status}</span></td>
                <td style="padding: 10px 12px; border-bottom: 1px solid #e0e0e0; font-size: 12px; color: #666;">${if (t.notes.isNotBlank()) t.notes else "-"}</td>
            </tr>
            """.trimIndent()
        }.joinToString("\n")

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <title>Family Fund Transfer Statement</title>
            <style>
                body {
                    font-family: 'Segoe UI', Helvetica, Arial, sans-serif;
                    margin: 20px;
                    color: #212121;
                    line-height: 1.4;
                }
                .header {
                    border-bottom: 3px solid #1976D2;
                    padding-bottom: 16px;
                    margin-bottom: 24px;
                }
                .badge {
                    background-color: #E3F2FD;
                    color: #1565C0;
                    padding: 4px 10px;
                    border-radius: 4px;
                    font-size: 12px;
                    font-weight: bold;
                    display: inline-block;
                    margin-bottom: 8px;
                }
                .title {
                    font-size: 26px;
                    font-weight: 800;
                    color: #0D47A1;
                    margin: 0;
                }
                .meta {
                    font-size: 13px;
                    color: #616161;
                    margin-top: 6px;
                }
                .cards {
                    display: flex;
                    gap: 16px;
                    margin-bottom: 24px;
                }
                .card {
                    flex: 1;
                    background: #F5F7FA;
                    border: 1px solid #CFD8DC;
                    border-radius: 8px;
                    padding: 14px;
                }
                .card-title {
                    font-size: 11px;
                    text-transform: uppercase;
                    color: #546E7A;
                    font-weight: bold;
                }
                .card-val {
                    font-size: 22px;
                    font-weight: bold;
                    color: #0D47A1;
                    margin-top: 4px;
                }
                table {
                    width: 100%;
                    border-collapse: collapse;
                    margin-top: 12px;
                    margin-bottom: 24px;
                }
                th {
                    background-color: #ECEFF1;
                    color: #37474F;
                    font-size: 12px;
                    text-transform: uppercase;
                    padding: 10px 12px;
                    text-align: left;
                    border-bottom: 2px solid #CFD8DC;
                }
                .section-heading {
                    font-size: 16px;
                    font-weight: bold;
                    color: #263238;
                    margin-top: 20px;
                    margin-bottom: 8px;
                    border-left: 4px solid #1976D2;
                    padding-left: 8px;
                }
                .footer {
                    margin-top: 40px;
                    padding-top: 16px;
                    border-top: 1px solid #CFD8DC;
                    font-size: 11px;
                    color: #78909C;
                    text-align: center;
                }
            </style>
        </head>
        <body>
            <div class="header">
                <span class="badge">OFFICIAL PERSONAL RECORD</span>
                <h1 class="title">FAMILY FUND TRANSFER STATEMENT</h1>
                <div class="meta">
                    <strong>Account Holder:</strong> $effectiveUserName &nbsp;|&nbsp; 
                    <strong>Recipient / Family Member:</strong> $memberName &nbsp;|&nbsp; 
                    <strong>Date Scope:</strong> $dateRangeLabel &nbsp;|&nbsp; 
                    <strong>Generated:</strong> $dateGen
                </div>
            </div>

            <div class="cards">
                <div class="card">
                    <div class="card-title">Total Transferred</div>
                    <div class="card-val">${CurrencyHelper.format(totalAmount, currencySymbol)}</div>
                </div>
                <div class="card">
                    <div class="card-title">Total Transfers</div>
                    <div class="card-val">$count Transactions</div>
                </div>
                <div class="card">
                    <div class="card-title">Monthly Average</div>
                    <div class="card-val">${CurrencyHelper.format(monthlyAvg, currencySymbol)}</div>
                </div>
            </div>

            <div class="section-heading">Payment Source Breakdown</div>
            <table>
                <thead>
                    <tr>
                        <th>Payment Source</th>
                        <th style="text-align: right;">Transactions</th>
                        <th style="text-align: right;">Total Transferred</th>
                        <th style="text-align: right;">Share</th>
                    </tr>
                </thead>
                <tbody>
                    $sourceRows
                </tbody>
            </table>

            <div class="section-heading">Detailed Transfer Ledger</div>
            <table>
                <thead>
                    <tr>
                        <th>Date</th>
                        <th>Recipient</th>
                        <th style="text-align: right;">Amount</th>
                        <th>Payment Source</th>
                        <th>Trx ID</th>
                        <th style="text-align: center;">Status</th>
                        <th>Description / Note</th>
                    </tr>
                </thead>
                <tbody>
                    $tableRows
                </tbody>
            </table>

            <div class="footer">
                This document is a certified statement of personal fund transfers to family members.<br>
                Generated by Family Fund Management System &bull; All monetary figures in Bangladeshi Taka (৳).
            </div>
        </body>
        </html>
        """.trimIndent()
    }
}
