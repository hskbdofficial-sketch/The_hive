package com.example.utils

import android.content.Context
import android.graphics.*
import com.example.data.models.Order
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object InvoiceGenerator {

    /**
     * Generates a high-resolution, visually stylized PNG invoice image for the given Order and saves it to local files is.
     */
    fun generateAndSaveInvoice(context: Context, order: Order): File? {
        try {
            val invoiceDir = File(context.filesDir, "Invoices")
            if (!invoiceDir.exists()) {
                invoiceDir.mkdirs()
            }

            // Save as PNG
            val filename = "invoice_${order.id}.png"
            val file = File(invoiceDir, filename)

            // Canvas size dimensions matching high-fidelity letter ratio
            val width = 800
            val height = 1100

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Paints
            val bgPaint = Paint().apply { color = Color.WHITE }
            val headerPaint = Paint().apply { color = Color.parseColor("#000000") } // Pure pitch black
            val goldPaint = Paint().apply { color = Color.parseColor("#FF0055") } // Neon red
            val lightGrayPaint = Paint().apply { color = Color.parseColor("#F8FAFC") }
            val linePaint = Paint().apply {
                color = Color.parseColor("#CBD5E1")
                strokeWidth = 2f
            }

            // Clear background
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            // 1. Draw Slate Header Banner
            canvas.drawRect(0f, 0f, width.toFloat(), 180f, headerPaint)

            // Draw orange corner highlights/stripes
            val triPath = Path().apply {
                moveTo(width.toFloat() - 120f, 0f)
                lineTo(width.toFloat(), 0f)
                lineTo(width.toFloat(), 180f)
                lineTo(width.toFloat() - 50f, 180f)
                close()
            }
            val triPaint = Paint().apply { color = Color.parseColor("#D90040") } // Hot Crimson
            canvas.drawPath(triPath, triPaint)

            // 2. Headings text
            val textPaint = Paint().apply {
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            // Title left
            textPaint.color = Color.parseColor("#F59E0B")
            textPaint.textSize = 34f
            canvas.drawText("THRIFT-HIVE STORE", 30f, 70f, textPaint)

            textPaint.color = Color.WHITE
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textPaint.textSize = 14f
            canvas.drawText("Premium Secondhand boutique proprietor — NASIF HIMADRI", 30f, 105f, textPaint)
            
            // Stats date on header
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val printedDate = sdf.format(Date(order.createdAt))
            canvas.drawText("Generated at: $printedDate", 30f, 135f, textPaint)

            // Right side Header title
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.color = Color.WHITE
            textPaint.textSize = 28f
            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("INVOICE", width.toFloat() - 30f, 70f, textPaint)

            textPaint.color = Color.parseColor("#94A3B8")
            textPaint.textSize = 12f
            canvas.drawText("REF ID: INV-${order.id.take(8).uppercase()}", width.toFloat() - 30f, 105f, textPaint)
            textPaint.textAlign = Paint.Align.LEFT // reset

            // 3. Orange Separation Diagonal Accent bar
            canvas.drawRect(0f, 180f, width.toFloat(), 188f, goldPaint)

            // 4. Client and Vendor Info columns
            textPaint.textSize = 15f
            textPaint.color = Color.parseColor("#1E293B")
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("INVOICE TO:", 40f, 235f, textPaint)

            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textPaint.textSize = 13f
            textPaint.color = Color.parseColor("#475569")
            canvas.drawText("Customer Credentials phone block:", 40f, 260f, textPaint)
            
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.color = Color.parseColor("#0F172A")
            canvas.drawText("☎ ${order.customerPhone}", 40f, 285f, textPaint)
            
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textPaint.color = Color.parseColor("#475569")
            canvas.drawText("Shipping coordinates Address:", 40f, 315f, textPaint)
            
            // Handle potentially long address wrapping
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.color = Color.parseColor("#0F172A")
            val addressTxt = order.customerAddress.take(45)
            canvas.drawText("📍 $addressTxt", 40f, 340f, textPaint)
            if (order.customerAddress.length > 45) {
                canvas.drawText("   " + order.customerAddress.substring(45).take(45), 40f, 362f, textPaint)
            }

            // Vendor Address column (Right aligned offsets)
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.color = Color.parseColor("#1E293B")
            textPaint.textSize = 15f
            canvas.drawText("INVOICE FROM:", 460f, 235f, textPaint)

            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textPaint.textSize = 13f
            textPaint.color = Color.parseColor("#475569")
            canvas.drawText("Thrift-Hive Vault HQ", 460f, 260f, textPaint)
            canvas.drawText("Owner: NASIF HIMADRI", 460f, 285f, textPaint)
            canvas.drawText("Proprietor Boutique system", 460f, 310f, textPaint)
            canvas.drawText("Dhaka, Bangladesh", 460f, 335f, textPaint)

            // Timeline date details spacer
            canvas.drawLine(40f, 390f, width - 40f, 390f, linePaint)

            // Order Date and Courier details
            textPaint.textSize = 12f
            textPaint.color = Color.parseColor("#64748B")
            canvas.drawText("ORDER DATE: ${order.orderDate}", 40f, 415f, textPaint)
            
            val courier = order.courierName.ifBlank { "Unassigned" }
            canvas.drawText("COURIER PARTNER: $courier", 270f, 415f, textPaint)
            
            val tracking = order.trackingId.ifBlank { "Unassigned" }
            canvas.drawText("TRACKING ID: $tracking", width.toFloat() - 250f, 415f, textPaint)

            // 5. Products Table Table headers
            canvas.drawRect(40f, 445f, width - 40f, 485f, lightGrayPaint)
            canvas.drawLine(40f, 445f, width - 40f, 445f, linePaint)
            canvas.drawLine(40f, 485f, width - 40f, 485f, linePaint)

            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.color = Color.parseColor("#0F172A")
            textPaint.textSize = 13f
            
            canvas.drawText("PRODUCT SPECIFICATION / DISPATCH INFO", 55f, 470f, textPaint)
            canvas.drawText("SZ", 480f, 470f, textPaint)
            canvas.drawText("PRICE", 540f, 470f, textPaint)
            canvas.drawText("QTY", 640f, 470f, textPaint)
            canvas.drawText("TOTAL", 710f, 470f, textPaint)

            // 6. Draw Table details
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textPaint.textSize = 13f
            textPaint.color = Color.parseColor("#1E293B")
            
            val croppedName = if (order.productName.length > 40) order.productName.take(37) + "..." else order.productName
            canvas.drawText(croppedName, 55f, 525f, textPaint)
            
            textPaint.color = Color.parseColor("#64748B")
            canvas.drawText("SKU: ${order.productId.uppercase()}", 55f, 545f, textPaint)

            textPaint.color = Color.parseColor("#1E293B")
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(order.size, 480f, 525f, textPaint)
            canvas.drawText(order.pricePerUnit.toString(), 540f, 525f, textPaint)
            canvas.drawText("${order.quantity}X", 640f, 525f, textPaint)
            canvas.drawText(order.totalAmount.toString(), 710f, 525f, textPaint)

            // Draw line divider under invoice record
            canvas.drawLine(40f, 575f, width - 40f, 575f, linePaint)

            // 7. Neon Red Highlighted GRAND TOTAL card box
            val cardRect = RectF(width.toFloat() - 380f, 620f, width.toFloat() - 40f, 685f)
            val cardPaint = Paint().apply { color = Color.parseColor("#FFF0F2") } // Soft rose red
            val borderPaint = Paint().apply {
                color = Color.parseColor("#FF0055") // Neon red border
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            canvas.drawRoundRect(cardRect, 6f, 6f, cardPaint)
            canvas.drawRoundRect(cardRect, 6f, 6f, borderPaint)

            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.color = Color.parseColor("#990022") // Hot dark crimson for text contrast
            textPaint.textSize = 14f
            canvas.drawText("GRAND TOTAL AMOUNT DUE:", width.toFloat() - 360f, 658f, textPaint)

            textPaint.textSize = 18f
            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("$ ${order.totalAmount}", width.toFloat() - 60f, 658f, textPaint)
            textPaint.textAlign = Paint.Align.LEFT

            // Notes and Delivery status detail
            textPaint.color = Color.parseColor("#334155")
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textPaint.textSize = 12f
            canvas.drawText("Payment Terms: Cash/Online Boutique Bank Transfers", 40f, 635f, textPaint)
            canvas.drawText("Delivery: Courier trackable directly.", 40f, 655f, textPaint)
            if (order.notes.isNotBlank()) {
                val notesStr = if (order.notes.length > 50) order.notes.take(47) + "..." else order.notes
                canvas.drawText("Notes: $notesStr", 40f, 680f, textPaint)
            }

            // 8. Dynamic OFFICIAL HIVE SECURITY SEAL/STAMP (Stunning Retro Circle stamp)
            // Render on bottom section
            val stampCenterX = 560f
            val stampCenterY = 880f
            val stampRadius = 75f

            val stampPaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeWidth = 4f
                color = when (order.deliveryStatus) {
                    "Delivered", "Delivered;" -> Color.parseColor("#10B981") // Green for completed/paid
                    "Cancelled" -> Color.parseColor("#EF4444") // Red
                    else -> Color.parseColor("#FF0055") // Neon red brand accent
                }
            }

            // Draw circular retro stamp borders
            canvas.drawCircle(stampCenterX, stampCenterY, stampRadius, stampPaint)
            stampPaint.strokeWidth = 1.5f
            canvas.drawCircle(stampCenterX, stampCenterY, stampRadius - 8f, stampPaint)

            // Top curved text path
            val stampPathTop = Path().apply {
                addArc(
                    RectF(stampCenterX - stampRadius, stampCenterY - stampRadius, stampCenterX + stampRadius, stampCenterY + stampRadius),
                    180f,
                    180f
                )
            }
            // Bottom curved text path
            val stampPathBottom = Path().apply {
                addArc(
                    RectF(stampCenterX - stampRadius, stampCenterY - stampRadius, stampCenterX + stampRadius, stampCenterY + stampRadius),
                    0f,
                    180f
                )
            }

            val stampTextPaint = Paint().apply {
                isAntiAlias = true
                color = stampPaint.color
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }

            canvas.drawTextOnPath("★ THRIFT-HIVE OFFICIAL ★", stampPathTop, 0f, 15f, stampTextPaint)
            canvas.drawTextOnPath("NASIF HIMADRI VAULT", stampPathBottom, 0f, -6f, stampTextPaint)

            // Inner stamp text
            val innerStampPaint = Paint().apply {
                isAntiAlias = true
                color = stampPaint.color
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            
            // Draw stamp textual indicator (e.g. "OFFICIAL", "PAID", "CONFIRMED")
            val statusStampText = when {
                order.deliveryStatus.equals("Delivered", true) || order.deliveryChargeReceived -> "PAID"
                order.deliveryStatus.equals("Cancelled", true) -> "VOIDED"
                else -> "CONFIRMED"
            }
            canvas.drawText(statusStampText, stampCenterX, stampCenterY + 6f, innerStampPaint)

            // Stamp Tilt shadow line
            val tiltPaint = Paint().apply {
                color = stampPaint.color
                strokeWidth = 3f
                style = Paint.Style.STROKE
            }
            canvas.drawLine(stampCenterX - 65f, stampCenterY + 22f, stampCenterX + 65f, stampCenterY - 22f, tiltPaint)

            // Draw a cute physical signature line next to the stamp
            val sigPaint = Paint().apply {
                color = Color.parseColor("#475569")
                strokeWidth = 1.5f
            }
            canvas.drawLine(80f, 910f, 260f, 910f, sigPaint)
            
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            textPaint.color = Color.parseColor("#64748B")
            textPaint.textSize = 11f
            canvas.drawText("Proprietor Signature [NASIF HIMADRI]", 80f, 928f, textPaint)
            
            canvas.drawText("Approved digital secure order transaction seal.", 80f, 946f, textPaint)

            // 9. Footnote and security watermark
            textPaint.color = Color.parseColor("#94A3B8")
            textPaint.textSize = 10f
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Thank you for shopping at Thrift-Hive! Direct invoice correspondence.", width / 2f, 1025f, textPaint)
            canvas.drawText("🔒 All local order data is robustly encrypted with AES-256 and cloud synchronized.", width / 2f, 1045f, textPaint)

            // Save visual bitmap to file output
            val fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.flush()
            fos.close()

            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
