package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "orders")
data class Order(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val customerPhone: String,
    val customerAddress: String,
    val productId: String,
    val productName: String,
    val size: String,
    val quantity: Int,
    val pricePerUnit: Double,
    val totalAmount: Double,
    val deliveryChargeReceived: Boolean,
    val deliveryStatus: String, // None/Pending/Confirmed/Packed/Dispatched/InTransit/Delivered/Returned/Refunded/Cancelled
    val notes: String = "",
    val orderDate: String, // YYYY-MM-DD
    val createdAt: Long = System.currentTimeMillis(),
    val courierName: String = "",
    val trackingId: String = "",
    val deliveryTimeline: String = "Order Placed;",
    val productImageUrl: String = "",
    val invoiceLocalPath: String = "",
    val isSynced: Boolean = true
)
