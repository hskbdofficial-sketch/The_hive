package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "products")
data class Product(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val productId: String,
    val batchNo: String,
    val productName: String,
    val category: String, // Tops/Bottoms/Dresses/Outerwear/Accessories/Footwear/Other
    val size: String, // XS/S/M/L/XL/XXL
    val purchasePrice: Double,
    val sellingPrice: Double,
    val profitPerUnit: Double, // sellingPrice - purchasePrice
    val quantity: Int,
    val totalProfit: Double, // profitPerUnit * quantity
    val dateAdded: String, // YYYY-MM-DD
    val sellingDate: String? = null,
    val deliveryStatus: String, // Pending/InTransit/Delivered/Cancelled
    val notes: String = "",
    val createdAt: String = System.currentTimeMillis().toString(),
    val warehouseLocation: String = "Main Warehouse",
    val rackLocation: String = "Rack A1",
    val imageUrl: String = "",
    val color: String = "Multicolor",
    val lowStockThreshold: Int = 2,
    val lifecycleHistory: String = "Product registered; "
)
