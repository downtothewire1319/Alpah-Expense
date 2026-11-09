package com.example.ui.util

import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.model.Expense
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FinancialInvoiceHelper {

    /**
     * Triggers Android's native PrintManager to print or "Save as PDF" directly from Android
     */
    fun printInvoice(
        context: Context,
        transactions: List<Expense>,
        title: String,
        periodLabel: String,
        userName: String,
        userEmail: String,
        currencySymbol: String,
        isAggregate: Boolean = false
    ) {
        val loggedInUser = Firebase.auth.currentUser
        val effectiveUserName = userName.ifBlank { loggedInUser?.displayName ?: "Account Holder" }
        val effectiveUserEmail = (loggedInUser?.email ?: "").ifBlank { userEmail }

        val htmlContent = buildHtmlInvoice(
            transactions = transactions,
            title = title,
            periodLabel = periodLabel,
            userName = effectiveUserName,
            userEmail = effectiveUserEmail,
            currencySymbol = currencySymbol,
            isAggregate = isAggregate
        )

        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager != null) {
            val webView = WebView(context)
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    val printAdapter = webView.createPrintDocumentAdapter("Financial_Report_${System.currentTimeMillis()}")
                    printManager.print("Expense Report Invoice", printAdapter, PrintAttributes.Builder().build())
                }
            }
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        } else {
            // Fallback to text sharing if PrintManager unavailable
            shareInvoiceText(context, transactions, title, periodLabel, userName, userEmail, currencySymbol, isAggregate)
        }
    }

    /**
     * Shares a clean, formatted text invoice via Android Intent (WhatsApp, Email, etc.)
     */
    fun shareInvoiceText(
        context: Context,
        transactions: List<Expense>,
        title: String,
        periodLabel: String,
        userName: String,
        userEmail: String,
        currencySymbol: String,
        isAggregate: Boolean = false
    ) {
        val text = buildTextInvoice(transactions, title, periodLabel, userName, userEmail, currencySymbol, isAggregate)
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, "$title - $periodLabel")
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Financial Invoice")
        context.startActivity(shareIntent)
    }

    fun buildTextInvoice(
        transactions: List<Expense>,
        title: String,
        periodLabel: String,
        userName: String,
        userEmail: String,
        currencySymbol: String,
        isAggregate: Boolean = false
    ): String {
        val displayTransactions = if (isAggregate) aggregateTransactionsByMonth(transactions) else transactions
        val creditsList = displayTransactions.filter { it.isIncome }
        val debitsList = displayTransactions.filter { !it.isIncome }
        val totalSpent = debitsList.sumOf { it.amount }
        val totalIncome = creditsList.sumOf { it.amount }
        val netBalance = totalIncome - totalSpent
        val dateGen = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())

        val sb = StringBuilder()
        sb.appendLine("==========================================")
        sb.appendLine("      FINANCIAL STATEMENT & INVOICE       ")
        sb.appendLine("==========================================")
        sb.appendLine("Report: $title")
        sb.appendLine("Scope: $periodLabel")
        sb.appendLine("Account Holder: $userName")
        sb.appendLine("Email: $userEmail")
        sb.appendLine("Generated: $dateGen")
        sb.appendLine("------------------------------------------")
        sb.appendLine("FINANCIAL SUMMARY:")
        sb.appendLine("  Total Income:  +$currencySymbol ${String.format(Locale.US, "%,.2f", totalIncome)}")
        sb.appendLine("  Total Expense: -$currencySymbol ${String.format(Locale.US, "%,.2f", totalSpent)}")
        sb.appendLine("  Net Balance:    $currencySymbol ${String.format(Locale.US, "%,.2f", netBalance)}")
        sb.appendLine("------------------------------------------")
        sb.appendLine("TRANSACTION LEDGER (${displayTransactions.size} records):")

        displayTransactions.sortedByDescending { it.date }.forEach { item ->
            val sign = if (item.isIncome) "+" else "-"
            val tag = if (item.isIncome) "[CREDIT]" else "[DEBIT] "
            sb.appendLine("${item.date} | $tag ${item.title}")
            sb.appendLine("   Category: ${item.category} | Method: ${item.paymentMethod}")
            sb.appendLine("   Amount: $sign$currencySymbol ${String.format(Locale.US, "%,.2f", item.amount)}")
            sb.appendLine("")
        }

        sb.appendLine("==========================================")
        sb.appendLine("Generated by Personal & Family Expense Tracker")
        return sb.toString()
    }

    private fun buildHtmlInvoice(
        transactions: List<Expense>,
        title: String,
        periodLabel: String,
        userName: String,
        userEmail: String,
        currencySymbol: String,
        isAggregate: Boolean = false
    ): String {
        val displayTransactions = if (isAggregate) aggregateTransactionsByMonth(transactions) else transactions
        val creditsList = displayTransactions.filter { it.isIncome }
        val debitsList = displayTransactions.filter { !it.isIncome }
        val totalSpent = debitsList.sumOf { it.amount }
        val totalIncome = creditsList.sumOf { it.amount }
        val netBalance = totalIncome - totalSpent
        val dateGen = SimpleDateFormat("MMMM dd, yyyy", Locale.US).format(Date())

        val categoryRows = debitsList.groupBy { it.category }
            .mapValues { it.value.sumOf { e -> e.amount } }
            .entries.sortedByDescending { it.value }
            .take(6)
            .joinToString("\n") { (cat, amt) ->
                val pct = if (totalSpent > 0) ((amt / totalSpent) * 100).toInt() else 0
                """
                <tr>
                    <td>$cat</td>
                    <td style="text-align:right;">$currencySymbol ${String.format(Locale.US, "%,.2f", amt)}</td>
                    <td style="text-align:right;">$pct%</td>
                </tr>
                """.trimIndent()
            }

        val transactionRows = displayTransactions.sortedByDescending { it.date }
            .joinToString("\n") { item ->
                val isInc = item.isIncome
                val color = if (isInc) "#2e7d32" else "#c62828"
                val badge = if (isInc) "CREDIT" else "DEBIT"
                val sign = if (isInc) "+" else "-"
                """
                <tr>
                    <td>${item.date}</td>
                    <td><strong>${item.title}</strong><br/><small style="color:#666;">${item.category} • ${item.paymentMethod}</small></td>
                    <td><span style="background:${if (isInc) "#e8f5e9" else "#ffebee"}; color:$color; padding:2px 8px; border-radius:4px; font-size:11px; font-weight:bold;">$badge</span></td>
                    <td style="text-align:right; font-weight:bold; color:$color;">$sign$currencySymbol ${String.format(Locale.US, "%,.2f", item.amount)}</td>
                </tr>
                """.trimIndent()
            }

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8"/>
            <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
            <title>$title - Invoice</title>
            <style>
                body {
                    font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
                    color: #1a202c;
                    margin: 20px;
                    background: #fff;
                    font-size: 13px;
                    line-height: 1.5;
                }
                .invoice-box {
                    max-width: 800px;
                    margin: auto;
                    border: 1px solid #e2e8f0;
                    padding: 30px;
                    border-radius: 8px;
                }
                .header-table {
                    width: 100%;
                    margin-bottom: 25px;
                }
                .header-table td {
                    vertical-align: top;
                }
                .brand {
                    font-size: 22px;
                    font-weight: 800;
                    color: #0A192F;
                    letter-spacing: -0.5px;
                }
                .invoice-title {
                    text-align: right;
                    font-size: 20px;
                    font-weight: 700;
                    color: #1E88E5;
                }
                .summary-cards {
                    display: table;
                    width: 100%;
                    margin: 20px 0;
                    background: #f8fafc;
                    border: 1px solid #e2e8f0;
                    border-radius: 6px;
                }
                .summary-card {
                    display: table-cell;
                    padding: 15px;
                    text-align: center;
                    border-right: 1px solid #e2e8f0;
                }
                .summary-card:last-child {
                    border-right: none;
                }
                .summary-label {
                    font-size: 11px;
                    text-transform: uppercase;
                    color: #64748b;
                    font-weight: 600;
                }
                .summary-value {
                    font-size: 18px;
                    font-weight: 800;
                    margin-top: 4px;
                }
                table.data-table {
                    width: 100%;
                    border-collapse: collapse;
                    margin-top: 15px;
                    margin-bottom: 25px;
                }
                table.data-table th {
                    background: #0A192F;
                    color: #fff;
                    text-align: left;
                    padding: 8px 10px;
                    font-size: 12px;
                    font-weight: 600;
                }
                table.data-table td {
                    padding: 8px 10px;
                    border-bottom: 1px solid #edf2f7;
                }
                .footer {
                    margin-top: 30px;
                    padding-top: 15px;
                    border-top: 1px solid #e2e8f0;
                    text-align: center;
                    font-size: 11px;
                    color: #718096;
                }
            </style>
        </head>
        <body>
            <div class="invoice-box">
                <table class="header-table">
                    <tr>
                        <td>
                            <div class="brand">EXPENSE TRACKER</div>
                            <div style="color:#64748b; font-size:12px; margin-top:3px;">Personal & Family Financial Statement</div>
                            <div style="margin-top:8px;">
                                <strong>Account Holder:</strong> $userName<br/>
                                <strong>Email:</strong> $userEmail
                            </div>
                        </td>
                        <td class="invoice-title">
                            <div>FINANCIAL INVOICE</div>
                            <div style="font-size:12px; color:#64748b; font-weight:normal; margin-top:4px;">
                                <strong>Report Scope:</strong> $periodLabel<br/>
                                <strong>Generated:</strong> $dateGen
                            </div>
                        </td>
                    </tr>
                </table>

                <div class="summary-cards">
                    <div class="summary-card">
                        <div class="summary-label">Total Income</div>
                        <div class="summary-value" style="color:#2e7d32;">$currencySymbol ${String.format(Locale.US, "%,.2f", totalIncome)}</div>
                    </div>
                    <div class="summary-card">
                        <div class="summary-label">Total Expenses</div>
                        <div class="summary-value" style="color:#c62828;">$currencySymbol ${String.format(Locale.US, "%,.2f", totalSpent)}</div>
                    </div>
                    <div class="summary-card">
                        <div class="summary-label">Net Balance</div>
                        <div class="summary-value" style="color:#1a202c;">$currencySymbol ${String.format(Locale.US, "%,.2f", netBalance)}</div>
                    </div>
                </div>

                ${if (categoryRows.isNotBlank()) """
                <h4 style="margin:20px 0 6px 0; color:#0A192F;">Top Expense Categories</h4>
                <table class="data-table" style="margin-bottom:15px;">
                    <thead>
                        <tr>
                            <th>Category</th>
                            <th style="text-align:right;">Amount Spent</th>
                            <th style="text-align:right;">Share (%)</th>
                        </tr>
                    </thead>
                    <tbody>
                        $categoryRows
                    </tbody>
                </table>
                """ else ""}

                <h4 style="margin:20px 0 6px 0; color:#0A192F;">Itemized Transactions Ledger (${displayTransactions.size} records)</h4>
                <table class="data-table">
                    <thead>
                        <tr>
                            <th style="width:90px;">Date</th>
                            <th>Description</th>
                            <th style="width:70px;">Type</th>
                            <th style="width:110px; text-align:right;">Amount</th>
                        </tr>
                    </thead>
                    <tbody>
                        $transactionRows
                    </tbody>
                </table>

                <div class="footer">
                    This official financial statement invoice was generated from Personal & Family Expense Tracker.<br/>
                    Verified electronic document • Confidential
                </div>
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    private fun aggregateTransactionsByMonth(transactions: List<Expense>): List<Expense> {
        return transactions.filter { !it.isTransfer }.groupBy { it.date.take(7) }.map { (month, list) ->
            val total = list.sumOf { if (it.isActualIncome) it.amount else -it.amount }
            Expense(
                title = "Total for $month",
                amount = Math.abs(total),
                category = "Monthly Aggregate",
                date = "$month-01",
                paymentMethod = "N/A",
                transactionType = if (total >= 0) "income" else "expense",
                expenseType = "public"
            )
        }
    }
}
