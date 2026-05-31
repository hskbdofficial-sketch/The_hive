package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "loss_records")
data class LossRecord(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val lossType: String, // Damaged Stock | Expired/Unsellable | Delivery Loss | Price Drop Loss | Stolen/Missing | Markdown/Discount Loss | Other
    val productId: String? = null,
    val productName: String? = null,
    val lossQuantity: Int,
    val purchasePricePerUnit: Double = 0.0,
    val lossAmountPerUnit: Double,
    val totalLoss: Double, // lossQuantity * lossAmountPerUnit
    val dateOfLoss: String, // YYYY-MM-DD
    val description: String = "",
    val severity: String // Low | Medium | High
)
