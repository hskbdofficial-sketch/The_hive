package com.example.data.repository

import com.example.data.database.ProductDao
import com.example.data.database.ReturnDao
import com.example.data.database.LossDao
import com.example.data.database.OrderDao
import com.example.data.models.Product
import com.example.data.models.ReturnItem
import com.example.data.models.LossRecord
import com.example.data.models.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ThriftRepository(
    private val productDao: ProductDao,
    private val returnDao: ReturnDao,
    private val lossDao: LossDao,
    private val orderDao: OrderDao
) {
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    val allReturns: Flow<List<ReturnItem>> = returnDao.getAllReturns()
    val allLosses: Flow<List<LossRecord>> = lossDao.getAllLosses()
    val allOrders: Flow<List<Order>> = orderDao.getAllOrders()

    suspend fun getProductById(id: String): Product? = productDao.getProductById(id)
    suspend fun getProductByProductId(productId: String): Product? = productDao.getProductByProductId(productId)

    suspend fun insertProduct(product: Product) = productDao.insertProduct(product)
    suspend fun insertProducts(products: List<Product>) = productDao.insertProducts(products)
    suspend fun updateProduct(product: Product) = productDao.updateProduct(product)
    suspend fun deleteProduct(product: Product) = productDao.deleteProduct(product)
    suspend fun deleteProductById(id: String) = productDao.deleteProductById(id)
    suspend fun deleteProducts(products: List<Product>) = productDao.deleteProducts(products)

    suspend fun insertReturn(returnItem: ReturnItem) = returnDao.insertReturn(returnItem)
    suspend fun deleteReturnById(id: String) = returnDao.deleteReturnById(id)

    suspend fun insertLoss(lossRecord: LossRecord) = lossDao.insertLoss(lossRecord)
    suspend fun deleteLossById(id: String) = lossDao.deleteLossById(id)

    suspend fun insertOrder(order: Order) = orderDao.insertOrder(order)
    suspend fun updateOrder(order: Order) = orderDao.updateOrder(order)
    suspend fun deleteOrderById(id: String) = orderDao.deleteOrderById(id)

    suspend fun prepopulateIfNeeded() {
        val currentList = allProducts.firstOrNull()
        if (currentList.isNullOrEmpty()) {
            val sdf = SimpleDateFormat("yyyy-MM-DD", Locale.getDefault())
            val dateStr = sdf.format(Date())
            val sampleProducts = listOf(
                Product(
                    productId = "TH-001",
                    batchNo = "BATCH-A1",
                    productName = "Vintage Leather Bomber Jacket",
                    category = "Outerwear",
                    size = "L",
                    purchasePrice = 4500.0,
                    sellingPrice = 8500.0,
                    profitPerUnit = 4000.0,
                    quantity = 3,
                    totalProfit = 12000.0,
                    dateAdded = dateStr,
                    deliveryStatus = "Delivered",
                    notes = "Genuine brown distressed leather, very high demand."
                ),
                Product(
                    productId = "TH-002",
                    batchNo = "BATCH-A1",
                    productName = "Levi's 501 Classic Denim Jeans",
                    category = "Bottoms",
                    size = "M",
                    purchasePrice = 1200.0,
                    sellingPrice = 3200.0,
                    profitPerUnit = 2000.0,
                    quantity = 8,
                    totalProfit = 16000.0,
                    dateAdded = dateStr,
                    deliveryStatus = "Delivered",
                    notes = "Classic vintage blue stonewash fit."
                ),
                Product(
                    productId = "TH-003",
                    batchNo = "BATCH-B2",
                    productName = "Carhartt Canvas Work Jacket",
                    category = "Outerwear",
                    size = "XL",
                    purchasePrice = 3500.0,
                    sellingPrice = 7000.0,
                    profitPerUnit = 3500.0,
                    quantity = 2,
                    totalProfit = 7000.0,
                    dateAdded = dateStr,
                    deliveryStatus = "InTransit",
                    notes = "Heavy duty duck canvas lining. Small vintage fading."
                ),
                Product(
                    productId = "TH-004",
                    batchNo = "BATCH-C1",
                    productName = "Japan Knit Cozy Sweater",
                    category = "Tops",
                    size = "M",
                    purchasePrice = 800.0,
                    sellingPrice = 2000.0,
                    profitPerUnit = 1200.0,
                    quantity = 12,
                    totalProfit = 14400.0,
                    dateAdded = dateStr,
                    deliveryStatus = "Pending",
                    notes = "Japanese wool knit, highly insulated."
                ),
                Product(
                    productId = "TH-005",
                    batchNo = "BATCH-D4",
                    productName = "Vintage Velvet Party Dress",
                    category = "Dresses",
                    size = "S",
                    purchasePrice = 1500.0,
                    sellingPrice = 3800.0,
                    profitPerUnit = 2300.0,
                    quantity = 4,
                    totalProfit = 9200.0,
                    dateAdded = dateStr,
                    deliveryStatus = "Cancelled",
                    notes = "Velvet evening wear. Returned once due to tight sleeves."
                )
            )
            productDao.insertProducts(sampleProducts)
        }
    }
}
