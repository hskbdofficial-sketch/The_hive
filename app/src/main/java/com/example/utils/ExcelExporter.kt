package com.example.utils

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.models.LossRecord
import com.example.data.models.Product
import com.example.data.models.ReturnItem
import com.example.data.models.Order
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExcelExporter {
    fun exportFullDatabase(
        context: Context,
        products: List<Product>,
        returns: List<ReturnItem>,
        losses: List<LossRecord>,
        orders: List<Order> = emptyList()
    ) {
        try {
            val sdfStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val filename = "THRIFT-HIVE_Export_$sdfStr.csv"
            val cacheFile = File(context.cacheDir, filename)
            val csvWriter = FileWriter(cacheFile)

            // --- SECTION 1: INVENTORY CATALOG ---
            csvWriter.append("--- SECTION 1: MASTER INVENTORY CATALOG ---\n")
            csvWriter.append("Product ID,Product Name,Category,Size,Purchase Price,Selling Price,Profit Per Unit,Quantity,Total Profit,Date Added,Fulfillment Status,Notes\n")
            products.forEach { p ->
                val notesSanitized = p.notes.replace(",", ";").replace("\n", " ")
                val row = "${p.productId},\"${p.productName}\",${p.category},${p.size},${p.purchasePrice},${p.sellingPrice},${p.profitPerUnit},${p.quantity},${p.totalProfit},${p.dateAdded},${p.deliveryStatus},\"$notesSanitized\"\n"
                csvWriter.append(row)
            }
            csvWriter.append("\n\n")

            // --- SECTION 2: CUSTOMER RETURNS ---
            csvWriter.append("--- SECTION 2: LOGGED CUSTOMER RETURNS ---\n")
            csvWriter.append("Product ID,Product Name,Original Selling Price,Return Quantity,Return Reason,Return Date,Refund Type,Refund Amount,Original Revenue Lost,Net Loss,Restored Stock\n")
            returns.forEach { r ->
                val notesSanitized = r.notes.replace(",", ";").replace("\n", " ")
                val row = "${r.productId},\"${r.productName}\",${r.originalSellingPrice},${r.returnQuantity},${r.returnReason},${r.returnDate},${r.refundType},${r.refundAmount},${r.originalRevenueLost},${r.netLoss},${r.updatedStock}\n"
                csvWriter.append(row)
            }
            csvWriter.append("\n\n")

            // --- SECTION 3: LOGGED BUSINESS LOSSES ---
            csvWriter.append("--- SECTION 3: LOGGED BUSINESS DEFICITS (LOSSES) ---\n")
            csvWriter.append("Loss Type,Product ID,Product Name,Quantity,Purchase Price Per Unit,Deficit Amount Per Unit,Total Loss Amount,Date of Loss,Severity,Description\n")
            losses.forEach { l ->
                val descSanitized = l.description.replace(",", ";").replace("\n", " ")
                val row = "${l.lossType},${l.productId ?: "N/A"},\"${l.productName ?: "Raw Inventory"}\",${l.lossQuantity},${l.purchasePricePerUnit},${l.lossAmountPerUnit},${l.totalLoss},${l.dateOfLoss},${l.severity},\"$descSanitized\"\n"
                csvWriter.append(row)
            }
            csvWriter.append("\n\n")

            // --- SECTION 4: PROFIT OVERVIEW FINANCIAL STATS ---
            csvWriter.append("--- SECTION 4: FINANCIAL EARNING SHEET OVERVIEW ---\n")
            val totalRevenue = products.sumOf { it.sellingPrice * it.quantity }
            val totalCost = products.sumOf { it.purchasePrice * it.quantity }
            val totalLossValue = losses.sumOf { it.totalLoss }
            val totalRefunds = returns.sumOf { it.refundAmount }
            val netProfit = totalRevenue - totalCost - totalLossValue - totalRefunds

            csvWriter.append("Key Parameter,Value\n")
            csvWriter.append("Total Potential Revenue,${totalRevenue}\n")
            csvWriter.append("Total Asset Cost (COGS),${totalCost}\n")
            csvWriter.append("Logged Dispatched Returns (Refunds),${totalRefunds}\n")
            csvWriter.append("Logged Operating Losses (Deficits),${totalLossValue}\n")
            csvWriter.append("Net Retained Store Profit,${netProfit}\n")
            csvWriter.append("\n\n")

            // --- SECTION 5: REGISTERED STORE ORDERS ---
            csvWriter.append("--- SECTION 5: REGISTERED WORK orders ---\n")
            csvWriter.append("Order ID,Customer Phone,Customer Address,Product ID,Product Name,Size,Quantity,Price Per Unit,Total Amount,Delivery Charge Received,Delivery Status,Notes,Order Date\n")
            orders.forEach { o ->
                val addrSanitized = o.customerAddress.replace(",", ";").replace("\n", " ")
                val notesSanitized = o.notes.replace(",", ";").replace("\n", " ")
                val nameSanitized = o.productName.replace(",", ";").replace("\n", " ")
                val row = "${o.id},${o.customerPhone},\"$addrSanitized\",${o.productId},\"$nameSanitized\",${o.size},${o.quantity},${o.pricePerUnit},${o.totalAmount},${o.deliveryChargeReceived},${o.deliveryStatus},\"$notesSanitized\",${o.orderDate}\n"
                csvWriter.append(row)
            }

            csvWriter.flush()
            csvWriter.close()

            // Open Share Intent
            val fileUri = FileProvider.getUriForFile(
                context,
                "com.thrifthive.nasif.fileprovider",
                cacheFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "THRIFT-HIVE Store Export - Statistics Report")
                putExtra(Intent.EXTRA_TEXT, "Bee Smart! Find attached complete store financial spreadsheets from THRIFT-HIVE.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Share THRIFT-HIVE Spreadsheet"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error compiling excel output: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun exportQrCatalog(context: Context, products: List<Product>) {
        try {
            val sdfStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val filename = "THRIFT-HIVE_QR_Catalog_$sdfStr.csv"
            val cacheFile = File(context.cacheDir, filename)
            val csvWriter = FileWriter(cacheFile)

            csvWriter.append("Product ID,Product Name,Category,Size,Quantity,Batch No,QR Payload URL\n")
            products.forEach { p ->
                val nameSanitized = p.productName.replace(",", ";").replace("\n", " ")
                val qrPayload = "thrift_hive://product?id=${p.productId}&size=${p.size}"
                val row = "${p.productId},\"$nameSanitized\",${p.category},${p.size},${p.quantity},${p.batchNo},\"$qrPayload\"\n"
                csvWriter.append(row)
            }
            csvWriter.flush()
            csvWriter.close()

            val fileUri = FileProvider.getUriForFile(
                context,
                "com.thrifthive.nasif.fileprovider",
                cacheFile
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "🐝 Thrift-Hive QR Catalog Export [$sdfStr]")
                putExtra(Intent.EXTRA_TEXT, "Hello! Attached is the CSV catalog containing QR code payloads for all inventory products.")
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share QR Catalog CSV"))
        } catch (e: Exception) {
            Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
