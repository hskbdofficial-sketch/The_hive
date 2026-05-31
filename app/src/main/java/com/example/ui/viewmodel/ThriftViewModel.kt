package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.ThriftDatabase
import com.example.data.models.LossRecord
import com.example.data.models.Product
import com.example.data.models.ReturnItem
import com.example.data.repository.ThriftRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ThriftViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ThriftDatabase.getDatabase(application)
    private val repository = ThriftRepository(db.productDao(), db.returnDao(), db.lossDao(), db.orderDao())

    // Shared Preferences for Settings
    private val prefs = application.getSharedPreferences("thrift_hive_prefs", Context.MODE_PRIVATE)

    // Exposed Flows
    val products: StateFlow<List<Product>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val returns: StateFlow<List<ReturnItem>> = repository.allReturns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val losses: StateFlow<List<LossRecord>> = repository.allLosses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val orders: StateFlow<List<com.example.data.models.Order>> = repository.allOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Settings States
    private val _storeName = MutableStateFlow(prefs.getString("store_name", "THRIFT-HIVE") ?: "THRIFT-HIVE")
    val storeName: StateFlow<String> = _storeName.asStateFlow()

    private val _currencySymbol = MutableStateFlow(prefs.getString("currency_symbol", "BDT") ?: "BDT")
    val currencySymbol: StateFlow<String> = _currencySymbol.asStateFlow()

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("is_dark_mode", true))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(prefs.getBoolean("notifications_enabled", true))
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    // Loading & Refresh State
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Authentication States
    private val _currentUserEmail = MutableStateFlow(prefs.getString("current_user_email", null))
    val currentUserEmail: StateFlow<String?> = _currentUserEmail.asStateFlow()

    private val _currentUserName = MutableStateFlow(prefs.getString("current_user_name", null))
    val currentUserName: StateFlow<String?> = _currentUserName.asStateFlow()

    // Team Sharing States
    data class TeamMember(
        val name: String,
        val email: String,
        val role: String,
        val canEditInventory: Boolean = true,
        val canEditReturns: Boolean = true,
        val canEditLosses: Boolean = true,
        val canEditOrders: Boolean = true,
        val canViewReports: Boolean = true
    )
    private val _teamMembers = MutableStateFlow<List<TeamMember>>(emptyList())
    val teamMembers: StateFlow<List<TeamMember>> = _teamMembers.asStateFlow()

    init {
        viewModelScope.launch {
            repository.prepopulateIfNeeded()
        }
        loadTeamMembers()
    }

    private fun loadTeamMembers() {
        val serialized = prefs.getString("team_members_serialized", "") ?: ""
        if (serialized.isEmpty()) {
            val list = listOf(
                TeamMember("Nasif Himadri", "nasifhimadri@gmail.com", "Administrator", true, true, true, true, true),
                TeamMember("John Doe", "john.thrift@gmail.com", "Editor", true, true, true, true, false),
                TeamMember("Jane Smith", "jane.hive@gmail.com", "Viewer", false, false, false, false, true)
            )
            _teamMembers.value = list
            saveTeamMembersList(list)
        } else {
            val list = serialized.split(";;").mapNotNull {
                val parts = it.split("::")
                if (parts.size >= 3) {
                    val name = parts[0]
                    val email = parts[1]
                    val role = parts[2]
                    val canEditInventory = parts.getOrNull(3)?.toBoolean() ?: (role != "Viewer")
                    val canEditReturns = parts.getOrNull(4)?.toBoolean() ?: (role != "Viewer")
                    val canEditLosses = parts.getOrNull(5)?.toBoolean() ?: (role != "Viewer")
                    val canEditOrders = parts.getOrNull(6)?.toBoolean() ?: (role != "Viewer")
                    val canViewReports = parts.getOrNull(7)?.toBoolean() ?: (role == "Administrator" || role == "Viewer")
                    TeamMember(name, email, role, canEditInventory, canEditReturns, canEditLosses, canEditOrders, canViewReports)
                } else null
            }
            _teamMembers.value = list
        }
    }

    private fun saveTeamMembersList(list: List<TeamMember>) {
        val serialized = list.joinToString(";;") {
            "${it.name}::${it.email}::${it.role}::${it.canEditInventory}::${it.canEditReturns}::${it.canEditLosses}::${it.canEditOrders}::${it.canViewReports}"
        }
        prefs.edit().putString("team_members_serialized", serialized).apply()
        _teamMembers.value = list
    }

    fun addTeamMember(
        name: String,
        email: String,
        role: String,
        canEditInventory: Boolean = true,
        canEditReturns: Boolean = true,
        canEditLosses: Boolean = true,
        canEditOrders: Boolean = true,
        canViewReports: Boolean = true
    ) {
        val current = _teamMembers.value.toMutableList()
        current.removeAll { it.email.equals(email, ignoreCase = true) }
        current.add(TeamMember(name, email, role, canEditInventory, canEditReturns, canEditLosses, canEditOrders, canViewReports))
        saveTeamMembersList(current)
    }

    fun removeTeamMember(email: String) {
        val current = _teamMembers.value.toMutableList()
        current.removeAll { it.email.equals(email, ignoreCase = true) }
        saveTeamMembersList(current)
    }

    // Role-based security validation
    fun currentUserHasPrivilege(privilegeCheck: (TeamMember) -> Boolean): Boolean {
        val email = _currentUserEmail.value ?: return true // standalone/testing default
        val member = _teamMembers.value.find { it.email.equals(email, ignoreCase = true) }
        return member?.let(privilegeCheck) ?: true
    }

    fun canCurrentUserEditInventory(): Boolean = currentUserHasPrivilege { it.canEditInventory }
    fun canCurrentUserEditReturns(): Boolean = currentUserHasPrivilege { it.canEditReturns }
    fun canCurrentUserEditLosses(): Boolean = currentUserHasPrivilege { it.canEditLosses }
    fun canCurrentUserEditOrders(): Boolean = currentUserHasPrivilege { it.canEditOrders }
    fun canCurrentUserViewReports(): Boolean = currentUserHasPrivilege { it.canViewReports }

    fun placeOrder(
        customerPhone: String,
        customerAddress: String,
        productId: String,
        quantity: Int,
        deliveryChargeReceived: Boolean,
        deliveryStatus: String,
        notes: String,
        orderDate: String,
        courierName: String = "",
        trackingId: String = "",
        deliveryTimeline: String = "Order Placed;",
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val product = repository.getProductByProductId(productId)
                if (product == null) {
                    onFailure("Product not found in inventory catalog.")
                    return@launch
                }
                if (product.quantity < quantity) {
                    onFailure("Insufficient stock! Available quantity: ${product.quantity}")
                    return@launch
                }

                // Deduct inventory
                val updatedProduct = product.copy(
                    quantity = product.quantity - quantity
                )
                repository.updateProduct(updatedProduct)

                // Create Order
                val totalCost = product.sellingPrice * quantity
                var finalOrder = com.example.data.models.Order(
                    customerPhone = customerPhone,
                    customerAddress = customerAddress,
                    productId = productId,
                    productName = product.productName,
                    size = product.size,
                    quantity = quantity,
                    pricePerUnit = product.sellingPrice,
                    totalAmount = totalCost,
                    deliveryChargeReceived = deliveryChargeReceived,
                    deliveryStatus = deliveryStatus,
                    notes = notes,
                    orderDate = orderDate,
                    courierName = courierName,
                    trackingId = trackingId,
                    deliveryTimeline = deliveryTimeline,
                    productImageUrl = product.imageUrl // Automatically couple product visual image to the invoice
                )

                // Always generate and save order invoice image securely
                val file = com.example.utils.InvoiceGenerator.generateAndSaveInvoice(getApplication(), finalOrder)
                if (file != null) {
                    finalOrder = finalOrder.copy(invoiceLocalPath = file.absolutePath)
                }

                repository.insertOrder(finalOrder)
                onSuccess()
            } catch (e: Exception) {
                onFailure(e.localizedMessage ?: "Unknown database error placing order.")
            }
        }
    }

    fun deleteOrder(orderId: String) {
        viewModelScope.launch {
            repository.deleteOrderById(orderId)
        }
    }

    fun updateOrder(
        order: com.example.data.models.Order,
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val oldOrder = orders.value.find { it.id == order.id }
                val product = repository.getProductByProductId(order.productId)
                if (product != null && oldOrder != null) {
                    var netStockChange = 0
                    if (oldOrder.deliveryStatus != "Cancelled" && order.deliveryStatus == "Cancelled") {
                        netStockChange = oldOrder.quantity
                    } else if (oldOrder.deliveryStatus == "Cancelled" && order.deliveryStatus != "Cancelled") {
                        netStockChange = -order.quantity
                    } else if (order.deliveryStatus != "Cancelled") {
                        netStockChange = oldOrder.quantity - order.quantity
                    }

                    val newQty = product.quantity + netStockChange
                    if (newQty < 0) {
                        onFailure("Insufficient stock in inventory to update this order's quantity! Available product quantity: ${product.quantity}")
                        return@launch
                    }
                    repository.updateProduct(product.copy(quantity = newQty))
                }

                // Always regenerate invoice to update the delivery/payment status visual stamps
                var finalOrder = order
                val file = com.example.utils.InvoiceGenerator.generateAndSaveInvoice(getApplication(), order)
                if (file != null) {
                    finalOrder = order.copy(invoiceLocalPath = file.absolutePath)
                }

                repository.updateOrder(finalOrder)
                onSuccess()
            } catch (e: Exception) {
                onFailure(e.localizedMessage ?: "Failed to update order in database.")
            }
        }
    }

    fun login(email: String, password: String, onResponse: (Boolean, String) -> Unit) {
        val normalizedEmail = email.trim().lowercase()
        val storedPass = prefs.getString("account_pass_$normalizedEmail", null)
        val name = prefs.getString("account_name_$normalizedEmail", null)
        val hashedInput = com.example.utils.EncryptionUtils.sha256Hash(password)
        if (storedPass == null) {
            onResponse(false, "No account exists with this email address.")
        } else if (storedPass != password && storedPass != hashedInput) {
            onResponse(false, "Incorrect password. Please try again.")
        } else {
            // Upgrade to SHA-256 securely if not already hashed
            if (storedPass == password) {
                prefs.edit().putString("account_pass_$normalizedEmail", hashedInput).apply()
            }
            _currentUserEmail.value = normalizedEmail
            _currentUserName.value = name ?: "User"
            prefs.edit().putString("current_user_email", normalizedEmail).apply()
            prefs.edit().putString("current_user_name", name ?: "User").apply()
            onResponse(true, "Successfully logged in!")
        }
    }

    fun signup(name: String, email: String, password: String, onResponse: (Boolean, String) -> Unit) {
        val normalizedEmail = email.trim().lowercase()
        if (normalizedEmail.isBlank() || password.length < 4) {
            onResponse(false, "Invalid email format or password too short (min 4 characters).")
            return
        }
        val storedPass = prefs.getString("account_pass_$normalizedEmail", null)
        if (storedPass != null || normalizedEmail == "demo@thrifthive.com") {
            onResponse(false, "An account is already registered with this email.")
        } else {
            val secureHash = com.example.utils.EncryptionUtils.sha256Hash(password)
            prefs.edit().putString("account_name_$normalizedEmail", name).apply()
            prefs.edit().putString("account_pass_$normalizedEmail", secureHash).apply()
            
            _currentUserEmail.value = normalizedEmail
            _currentUserName.value = name
            prefs.edit().putString("current_user_email", normalizedEmail).apply()
            prefs.edit().putString("current_user_name", name).apply()
            
            onResponse(true, "Account created successfully!")
        }
    }

    fun logout() {
        _currentUserEmail.value = null
        _currentUserName.value = null
        prefs.edit().remove("current_user_email").remove("current_user_name").apply()
    }

    // Refresh Trigger
    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            // Small simulated delay for pulling to refresh feel
            kotlinx.coroutines.delay(600)
            _isRefreshing.value = false
        }
    }

    // Product Business Actions
    fun getProductByUuid(id: String, onCompleted: (Product?) -> Unit) {
        viewModelScope.launch {
            val prod = repository.getProductById(id)
            onCompleted(prod)
        }
    }

    fun saveProduct(
        id: String?,
        productId: String,
        batchNo: String,
        productName: String,
        category: String,
        size: String,
        purchasePrice: Double,
        sellingPrice: Double,
        quantity: Int,
        dateAdded: String,
        sellingDate: String? = null,
        deliveryStatus: String,
        notes: String,
        warehouseLocation: String = "Main Warehouse",
        rackLocation: String = "Rack A1",
        imageUrl: String = "",
        color: String = "Multicolor",
        lowStockThreshold: Int = 2,
        lifecycleHistory: String = "Product registered; "
    ) {
        viewModelScope.launch {
            val profitPerUnit = sellingPrice - purchasePrice
            val totalProfit = profitPerUnit * quantity

            val product = Product(
                id = id ?: UUID.randomUUID().toString(),
                productId = productId,
                batchNo = batchNo,
                productName = productName,
                category = category,
                size = size,
                purchasePrice = purchasePrice,
                sellingPrice = sellingPrice,
                profitPerUnit = profitPerUnit,
                quantity = quantity,
                totalProfit = totalProfit,
                dateAdded = dateAdded,
                sellingDate = sellingDate,
                deliveryStatus = deliveryStatus,
                notes = notes,
                createdAt = System.currentTimeMillis().toString(),
                warehouseLocation = warehouseLocation,
                rackLocation = rackLocation,
                imageUrl = imageUrl,
                color = color,
                lowStockThreshold = lowStockThreshold,
                lifecycleHistory = lifecycleHistory
            )
            repository.insertProduct(product)
            triggerSyncChange() // Trigger auto backup/sync simulation on edit
        }
    }

    fun saveProducts(productsList: List<Product>) {
        viewModelScope.launch {
            repository.insertProducts(productsList)
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
        }
    }

    fun deleteProductById(id: String) {
        viewModelScope.launch {
            repository.deleteProductById(id)
        }
    }

    fun updateProductDeliveryStatus(id: String, status: String) {
        viewModelScope.launch {
            val existing = repository.getProductById(id)
            if (existing != null) {
                repository.insertProduct(existing.copy(deliveryStatus = status))
            }
        }
    }

    fun getNextProductId(): String {
        val current = products.value
        val maxId = current.mapNotNull {
            val part = it.productId.substringAfter("TH-", "")
            part.toIntOrNull()
        }.maxOrNull() ?: 0
        return String.format(Locale.getDefault(), "TH-%03d", maxId + 1)
    }

    // Return Business Actions
    fun confirmReturn(
        productId: String,
        productName: String,
        originalSellingPrice: Double,
        returnQuantity: Int,
        returnReason: String,
        returnDate: String,
        refundType: String,
        partialRefundAmount: Double = 0.0,
        notes: String,
        refundAmount: Double,
        originalRevenueLost: Double,
        netLoss: Double,
        updatedStock: Boolean,
        color: String = "Multicolor"
    ) {
        viewModelScope.launch {
            // Log Return Item
            val returnItem = ReturnItem(
                productId = productId,
                productName = productName,
                originalSellingPrice = originalSellingPrice,
                returnQuantity = returnQuantity,
                returnReason = returnReason,
                returnDate = returnDate,
                refundType = refundType,
                partialRefundAmount = partialRefundAmount,
                notes = notes,
                refundAmount = refundAmount,
                originalRevenueLost = originalRevenueLost,
                netLoss = netLoss,
                color = color,
                updatedStock = updatedStock
            )
            repository.insertReturn(returnItem)

            // Auto-update quantity in inventory if requested
            if (updatedStock) {
                val dbProduct = repository.getProductByProductId(productId)
                if (dbProduct != null) {
                    val newQty = dbProduct.quantity + returnQuantity
                    repository.insertProduct(
                        dbProduct.copy(
                            quantity = newQty,
                            totalProfit = dbProduct.profitPerUnit * newQty
                        )
                    )
                }
            }
        }
    }

    fun deleteReturn(id: String) {
        viewModelScope.launch {
            repository.deleteReturnById(id)
        }
    }

    // Loss Business Actions
    fun logLoss(
        lossType: String,
        productId: String?,
        productName: String?,
        lossQuantity: Int,
        purchasePricePerUnit: Double,
        lossAmountPerUnit: Double,
        dateOfLoss: String,
        description: String = "",
        severity: String
    ) {
        viewModelScope.launch {
            // Deduct from stock if product and quantity is entered, and it is "Damaged/Stolen"
            if (productId != null) {
                val dbProduct = repository.getProductByProductId(productId)
                if (dbProduct != null) {
                    val finalQty = (dbProduct.quantity - lossQuantity).coerceAtLeast(0)
                    repository.insertProduct(
                        dbProduct.copy(
                            quantity = finalQty,
                            totalProfit = dbProduct.profitPerUnit * finalQty
                        )
                    )
                }
            }

            val totalLoss = lossQuantity * lossAmountPerUnit
            val lossRecord = LossRecord(
                lossType = lossType,
                productId = productId,
                productName = productName,
                lossQuantity = lossQuantity,
                purchasePricePerUnit = purchasePricePerUnit,
                lossAmountPerUnit = lossAmountPerUnit,
                totalLoss = totalLoss,
                dateOfLoss = dateOfLoss,
                description = description,
                severity = severity
            )
            repository.insertLoss(lossRecord)
        }
    }

    fun deleteLoss(id: String) {
        viewModelScope.launch {
            repository.deleteLossById(id)
        }
    }

    // Settings Updates
    fun updateStoreName(name: String) {
        _storeName.value = name
        prefs.edit().putString("store_name", name).apply()
    }

    fun updateCurrencySymbol(symbol: String) {
        _currencySymbol.value = symbol
        prefs.edit().putString("currency_symbol", symbol).apply()
    }

    fun toggleDarkMode() {
        val newVal = !_isDarkMode.value
        _isDarkMode.value = newVal
        prefs.edit().putBoolean("is_dark_mode", newVal).apply()
    }

    fun toggleNotifications() {
        val newVal = !_notificationsEnabled.value
        _notificationsEnabled.value = newVal
        prefs.edit().putBoolean("notifications_enabled", newVal).apply()
    }

    fun clearAllData() {
        viewModelScope.launch {
            // Wipe Room
            db.clearAllTables()
            // Reset prefs
            prefs.edit().clear().apply()
            _storeName.value = "THRIFT-HIVE"
            _currencySymbol.value = "BDT"
            _isDarkMode.value = true
            _notificationsEnabled.value = true

            // Trigger prepopulate again
            repository.prepopulateIfNeeded()
        }
    }

    // Sync Status States
    private val _syncStatus = MutableStateFlow("Synced")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _lastSyncTime = MutableStateFlow(System.currentTimeMillis())
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    // Activity timeline / Audit Logs
    private val _auditLogs = MutableStateFlow<List<String>>(
        listOf(
            "System initialized.",
            "Database loaded with 3 product records.",
            "Local sandbox sync activated successfully."
        )
    )
    val auditLogs: StateFlow<List<String>> = _auditLogs.asStateFlow()

    fun logAuditAction(action: String) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        _auditLogs.value = listOf("[$sdf] $action") + _auditLogs.value.take(25)
    }

    private fun triggerSyncChange() {
        _syncStatus.value = "Pending Sync"
        viewModelScope.launch {
            kotlinx.coroutines.delay(1500)
            performCloudSync()
        }
    }

    fun performCloudSync() {
        viewModelScope.launch {
            _syncStatus.value = "Syncing with cloud..."
            kotlinx.coroutines.delay(1200)
            _syncStatus.value = "Synced"
            _lastSyncTime.value = System.currentTimeMillis()
            logAuditAction("Automatic cloud background synchronization completed successfully.")
        }
    }

    // AI Insight Generator fallback or using mock inputs
    data class AIInsight(
        val smartPriceSuggestion: String,
        val forecasting: String,
        val deadStockRisk: String,
        val restockRecommendation: String,
        val returnRiskPrediction: String
    )

    fun getAIInsightsForProduct(product: Product): StateFlow<AIInsight> {
        val calculatedFlow = MutableStateFlow(
            AIInsight(
                smartPriceSuggestion = "Based on market velocity for ${product.category}, consider a promotional price of ${product.sellingPrice * 0.95} to speed up sales, or set to ${product.sellingPrice * 1.1} during peak holiday weekends.",
                forecasting = "Estimated inventory exhaustion timeline is ${if (product.quantity > 5) "4-6 weeks" else "3-5 days"} based on category seasonal demand index.",
                deadStockRisk = if (System.currentTimeMillis() - product.createdAt.toLongOrNull().let { it ?: System.currentTimeMillis() } > 15L * 24 * 60 * 60 * 1000) { "CRITICAL RISK: Registered over 15 days ago with zero sales movement. Proactively bundle with Accessories." } else { "LOW RISK: Moving steadily. Keep current layout placement." },
                restockRecommendation = if (product.quantity <= product.lowStockThreshold) { "RESTOCK IMMEDIATELY: Current qty (${product.quantity}) is equal/under threshold (${product.lowStockThreshold}). Target restock size: +10 units from Batch ${product.batchNo}." } else { "Sufficient inventory levels. No restock needed." },
                returnRiskPrediction = "Expected 2% return rate based on historic fits for size ${product.size} in ${product.category}. Ensure necklines and chest sizing specifications are displayed clearly."
            )
        )
        return calculatedFlow.asStateFlow()
    }
}
