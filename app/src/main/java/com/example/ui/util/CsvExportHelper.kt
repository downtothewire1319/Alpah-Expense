package com.example.ui.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.model.Expense
import java.io.File

object CsvExportHelper {

    fun generateCsv(expenses: List<Expense>, title: String): String {
        val sb = StringBuilder()
        sb.append("ID,Date,Title,Category,Type,Amount,Payment Method,Notes,Creator\n")
        expenses.forEach { exp ->
            val cleanTitle = exp.title.replace("\"", "\"\"")
            val cleanNotes = (exp.notes ?: "").replace("\"", "\"\"")
            sb.append("\"${exp.id}\",")
            sb.append("\"${exp.date}\",")
            sb.append("\"$cleanTitle\",")
            sb.append("\"${exp.category}\",")
            sb.append("\"${exp.expenseType}\",")
            sb.append("${exp.amount},")
            sb.append("\"${exp.paymentMethod}\",")
            sb.append("\"$cleanNotes\",")
            sb.append("\"${exp.creatorName}\"\n")
        }
        return sb.toString()
    }

    fun shareCsv(context: Context, csvContent: String, fileName: String = "expenses_export.csv") {
        try {
            val file = File(context.cacheDir, fileName)
            file.writeText(csvContent)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Expense Report")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Expense CSV"))
        } catch (e: Exception) {
            // Fallback to plain text sharing if file provider is not configured
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, csvContent)
                putExtra(Intent.EXTRA_SUBJECT, "Expense Report")
            }
            context.startActivity(Intent.createChooser(intent, "Share Expense Data"))
        }
    }
}
