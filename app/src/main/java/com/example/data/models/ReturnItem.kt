package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "return_items")
data class ReturnItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val productId: String,
    val productName: String,
    val originalSellingPrice: Double,
    val returnQuantity: Int,
    val returnReason: String, // Defective | Wrong Size | Customer Changed Mind | Damaged in Delivery | Other
    val returnDate: String, // YYYY-MM-DD
    val refundType: String, // Full Refund | Partial Refund | Exchange
    val partialRefundAmount: Double = 0.0,
    val notes: String = "",
    val refundAmount: Double, // calculated during entry
    val originalRevenueLost: Double, // sellingPrice * quantity
    val netLoss: Double, // original revenue list + potential refund details
    val color: String = "Multicolor",
    val updatedStock: Boolean = true // Whether stock was returned to system
)
