package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.models.Product
import com.example.data.models.Order
import com.example.ui.theme.*
import com.example.ui.viewmodel.ThriftViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.core.content.FileProvider
import android.content.Intent
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    viewModel: ThriftViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()

    val canEdit = viewModel.canCurrentUserEditOrders()

    var activeTab by remember { mutableStateOf(0) } // 0 = Place Order, 1 = Order Registry

    // Place Order State Variables
    var customerPhone by remember { mutableStateOf("") }
    var customerAddress by remember { mutableStateOf("") }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var selectedQuantity by remember { mutableIntStateOf(1) }
    var deliveryChargeReceived by remember { mutableStateOf(false) }
    var deliveryStatus by remember { mutableStateOf("Pending") } // Pending, None, In Transit, Delivered, Cancelled
    var notes by remember { mutableStateOf("") }
    var courierName by remember { mutableStateOf("") }
    var trackingId by remember { mutableStateOf("") }

    var productSearchText by remember { mutableStateOf("") }
    var showProductDropdown by remember { mutableStateOf(false) }
    var showStatusDropdown by remember { mutableStateOf(false) }

    // Registry Filter parameters
    var registrySearchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("All") }

    // Edit Order State Variables
    var showEditDialog by remember { mutableStateOf(false) }
    var orderToEdit by remember { mutableStateOf<Order?>(null) }
    var editCustomerPhone by remember { mutableStateOf("") }
    var editCustomerAddress by remember { mutableStateOf("") }
    var editDeliveryStatus by remember { mutableStateOf("Pending") }
    var editDeliveryChargeReceived by remember { mutableStateOf(false) }
    var editQuantity by remember { mutableIntStateOf(1) }
    var editNotes by remember { mutableStateOf("") }
    var showEditStatusDropdown by remember { mutableStateOf(false) }
    var editCourierName by remember { mutableStateOf("") }
    var editTrackingId by remember { mutableStateOf("") }

    // Visual Invoice share states
    var showInvoicePreviewDialog by remember { mutableStateOf(false) }
    var selectedOrderForInvoice by remember { mutableStateOf<Order?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Orders & Shipments 📦", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyBg)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(NavyBg)
                .padding(innerPadding)
        ) {
            // Read-Only Security warning if user is viewer/does not have order privileges
            if (!canEdit) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ErrorRed.copy(0.15f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Security, contentDescription = "Access Controls", tint = ErrorRed)
                        Text(
                            text = "Security Restriction: You have Read-Only view access for store orders.",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Tab Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { activeTab = 0 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeTab == 0) HoneyGold else SlateCard,
                        contentColor = if (activeTab == 0) Color.Black else Color.White
                    ),
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Place Order", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Button(
                    onClick = { activeTab = 1 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeTab == 1) HoneyGold else SlateCard,
                        contentColor = if (activeTab == 1) Color.Black else Color.White
                    ),
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ListAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Registry (${orders.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            if (activeTab == 0) {
                // Place Order Screen
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SlateCard)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "Store Customer details",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )

                                OutlinedTextField(
                                    value = customerPhone,
                                    onValueChange = { customerPhone = it },
                                    label = { Text("Customer Phone Target", color = Color(0xFF94A3B8)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = HoneyGold) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    enabled = canEdit,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = HoneyGold,
                                        unfocusedBorderColor = Color(0xFF334155),
                                        disabledTextColor = Color.Gray
                                    )
                                )

                                OutlinedTextField(
                                    value = customerAddress,
                                    onValueChange = { customerAddress = it },
                                    label = { Text("Delivery Address", color = Color(0xFF94A3B8)) },
                                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = HoneyGold) },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 2,
                                    shape = RoundedCornerShape(8.dp),
                                    enabled = canEdit,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = HoneyGold,
                                        unfocusedBorderColor = Color(0xFF334155),
                                        disabledTextColor = Color.Gray
                                    )
                                )
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SlateCard)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "Product Allocation",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )

                                // Select Product dropdown field
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = if (selectedProduct != null) "${selectedProduct!!.productId} — ${selectedProduct!!.productName}" else productSearchText,
                                        onValueChange = {
                                            selectedProduct = null
                                            productSearchText = it
                                            showProductDropdown = true
                                        },
                                        placeholder = { Text("Search by ID or Product Name...", color = Color.Gray) },
                                        label = { Text("Select Catalog Product", color = Color(0xFF94A3B8)) },
                                        leadingIcon = { Icon(Icons.Default.Inventory2, contentDescription = null, tint = HoneyGold) },
                                        trailingIcon = {
                                            IconButton(onClick = { showProductDropdown = !showProductDropdown }) {
                                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = HoneyGold)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        enabled = canEdit,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = HoneyGold,
                                            unfocusedBorderColor = Color(0xFF334155),
                                            disabledTextColor = Color.Gray
                                        )
                                    )

                                    DropdownMenu(
                                        expanded = showProductDropdown && canEdit,
                                        onDismissRequest = { showProductDropdown = false },
                                        modifier = Modifier
                                            .fillMaxWidth(0.9f)
                                            .background(SlateCard)
                                    ) {
                                        val filteredInStock = products.filter {
                                            (it.productId.contains(productSearchText, ignoreCase = true) ||
                                                    it.productName.contains(productSearchText, ignoreCase = true)) &&
                                                    it.quantity > 0
                                        }

                                        if (filteredInStock.isEmpty()) {
                                            DropdownMenuItem(
                                                text = { Text("No matched products in stock.", color = Color.Gray) },
                                                onClick = { showProductDropdown = false }
                                            )
                                        } else {
                                            filteredInStock.take(15).forEach { prod ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Column {
                                                            Text("${prod.productId} — ${prod.productName}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                            Text("Size: ${prod.size} — Stock: ${prod.quantity} Units — Price: ${viewModel.currencySymbol.value} ${prod.sellingPrice}", color = Color.Gray, fontSize = 11.sp)
                                                        }
                                                    },
                                                    onClick = {
                                                        selectedProduct = prod
                                                        selectedQuantity = 1
                                                        showProductDropdown = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                // Show details if product is selected
                                selectedProduct?.let { prod ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(NavyBg.copy(0.4f))
                                            .padding(12.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = "Matched Category: ${prod.category} | Size: ${prod.size}",
                                                color = HoneyGold,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = "Current Stock Quantity: ${prod.quantity} pcs remaining",
                                                color = if (prod.quantity < 3) ErrorRed else SuccessGreen,
                                                fontSize = 11.sp
                                            )
                                            Text(
                                                text = "Unit Value: ${viewModel.currencySymbol.value} ${prod.sellingPrice}",
                                                color = Color.White,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    // Quantity Picker
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Order Quantity:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            IconButton(
                                                onClick = { if (selectedQuantity > 1) selectedQuantity-- },
                                                enabled = canEdit && selectedQuantity > 1,
                                                modifier = Modifier.size(36.dp).background(SlateCard, RoundedCornerShape(18.dp))
                                            ) {
                                                Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease", tint = Color.LightGray)
                                            }

                                            Text(
                                                text = selectedQuantity.toString(),
                                                color = Color.White,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 18.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp)
                                            )

                                            IconButton(
                                                onClick = {
                                                    if (selectedQuantity < prod.quantity) {
                                                        selectedQuantity++
                                                    } else {
                                                        Toast.makeText(context, "Cannot order more than available stock (${prod.quantity} units)", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                enabled = canEdit && selectedQuantity < prod.quantity,
                                                modifier = Modifier.size(36.dp).background(SlateCard, RoundedCornerShape(18.dp))
                                            ) {
                                                Icon(imageVector = Icons.Default.Add, contentDescription = "Increase", tint = HoneyGold)
                                            }
                                        }
                                    }

                                    Divider(color = Color(0xFF334155), thickness = 1.dp)

                                    // Display calculated Total Profit/Value
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Grand Order Cost:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(
                                            text = "${viewModel.currencySymbol.value} ${prod.sellingPrice * selectedQuantity}",
                                            color = HoneyGold,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 20.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SlateCard)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "Delivery & Shipments Options",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )

                                // Delivery Status dropdown with None option
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = deliveryStatus,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Delivery Status", color = Color(0xFF94A3B8)) },
                                        leadingIcon = { Icon(Icons.Default.Moped, contentDescription = null, tint = HoneyGold) },
                                        trailingIcon = {
                                            IconButton(onClick = { showStatusDropdown = !showStatusDropdown }) {
                                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = HoneyGold)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        enabled = canEdit,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            unfocusedBorderColor = Color(0xFF334155),
                                            disabledTextColor = Color.Gray
                                        )
                                    )

                                    DropdownMenu(
                                        expanded = showStatusDropdown && canEdit,
                                        onDismissRequest = { showStatusDropdown = false },
                                        modifier = Modifier
                                            .fillMaxWidth(0.9f)
                                            .background(SlateCard)
                                    ) {
                                        val statuses = listOf("None", "Pending", "In Transit", "Delivered", "Cancelled")
                                        statuses.forEach { st ->
                                            DropdownMenuItem(
                                                text = { Text(st, color = Color.White) },
                                                onClick = {
                                                    deliveryStatus = st
                                                    showStatusDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Delivery Charge Paid/Not toggle
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(NavyBg.copy(0.4f))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Delivery Charge Received?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Toggle if transit charges were collected upfront.", color = Color.Gray, fontSize = 11.sp)
                                    }

                                    Switch(
                                        checked = deliveryChargeReceived,
                                        onCheckedChange = { deliveryChargeReceived = it },
                                        enabled = canEdit,
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = HoneyGold,
                                            checkedTrackColor = HoneyGold.copy(0.3f),
                                            uncheckedThumbColor = Color.Gray,
                                            uncheckedTrackColor = SlateCard
                                        )
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = courierName,
                                        onValueChange = { courierName = it },
                                        label = { Text("Courier", color = Color(0xFF94A3B8)) },
                                        placeholder = { Text("e.g. FedEx, DHL", color = Color(0xFF475569), fontSize = 12.sp) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        enabled = canEdit,
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = HoneyGold,
                                            unfocusedBorderColor = Color(0xFF334155),
                                            disabledTextColor = Color.Gray
                                        )
                                    )

                                    OutlinedTextField(
                                        value = trackingId,
                                        onValueChange = { trackingId = it },
                                        label = { Text("Tracking ID", color = Color(0xFF94A3B8)) },
                                        placeholder = { Text("e.g. TRK1284", color = Color(0xFF475569), fontSize = 12.sp) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        enabled = canEdit,
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = HoneyGold,
                                            unfocusedBorderColor = Color(0xFF334155),
                                            disabledTextColor = Color.Gray
                                        )
                                    )
                                }

                                OutlinedTextField(
                                    value = notes,
                                    onValueChange = { notes = it },
                                    label = { Text("Special Dispatch Instructions", color = Color(0xFF94A3B8)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    enabled = canEdit,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = HoneyGold,
                                        unfocusedBorderColor = Color(0xFF334155),
                                        disabledTextColor = Color.Gray
                                    )
                                )
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                if (customerPhone.isBlank() || customerAddress.isBlank()) {
                                    Toast.makeText(context, "Please populate phone & address targets.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val prod = selectedProduct
                                if (prod == null) {
                                    Toast.makeText(context, "Please assign a valid catalog product.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val todayStr = sdfDate.format(Date())

                                viewModel.placeOrder(
                                    customerPhone = customerPhone.trim(),
                                    customerAddress = customerAddress.trim(),
                                    productId = prod.productId,
                                    quantity = selectedQuantity,
                                    deliveryChargeReceived = deliveryChargeReceived,
                                    deliveryStatus = deliveryStatus,
                                    notes = notes.trim(),
                                    orderDate = todayStr,
                                    courierName = courierName.trim(),
                                    trackingId = trackingId.trim(),
                                    deliveryTimeline = "Placed in system on $todayStr Tracker status: $deliveryStatus;",
                                    onSuccess = {
                                        customerPhone = ""
                                        customerAddress = ""
                                        selectedProduct = null
                                        selectedQuantity = 1
                                        deliveryChargeReceived = false
                                        deliveryStatus = "Pending"
                                        notes = ""
                                        courierName = ""
                                        trackingId = ""
                                        Toast.makeText(context, "Order successfully registered! Inventory stock synchronized.", Toast.LENGTH_LONG).show()
                                        // Navigate to Registry
                                        activeTab = 1
                                    },
                                    onFailure = { err ->
                                        Toast.makeText(context, "Error: $err", Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            enabled = canEdit && selectedProduct != null,
                            colors = ButtonDefaults.buttonColors(containerColor = HoneyGold, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text("Confirm & Disburse Stock", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            } else {
                // Order Registry view
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Filters & Search fields
                    OutlinedTextField(
                        value = registrySearchQuery,
                        onValueChange = { registrySearchQuery = it },
                        placeholder = { Text("Filter by phone, product, address...", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = HoneyGold) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = HoneyGold,
                            unfocusedBorderColor = Color(0xFF334155)
                        )
                    )

                    // Compact filter Row for status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val filters = listOf("All", "Pending", "Delivered", "None")
                        filters.forEach { status ->
                            val isSelected = statusFilter == status
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) HoneyGold else SlateCard)
                                    .clickable { statusFilter = status }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = status,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // Order List Registry items
                    val filteredOrders = orders.filter { ord ->
                        val matchesQuery = ord.customerPhone.contains(registrySearchQuery, ignoreCase = true) ||
                                ord.productName.contains(registrySearchQuery, ignoreCase = true) ||
                                ord.customerAddress.contains(registrySearchQuery, ignoreCase = true) ||
                                ord.productId.contains(registrySearchQuery, ignoreCase = true)

                        val matchesStatus = if (statusFilter == "All") true else ord.deliveryStatus.equals(statusFilter, ignoreCase = true)

                        matchesQuery && matchesStatus
                    }

                    if (filteredOrders.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.BookmarkBorder, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(44.dp))
                                Text("No registered orders match current query.", color = Color.Gray, fontSize = 13.sp)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredOrders) { ord ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = SlateCard)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Row containing order details & Date
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = ord.productName,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = "ID: ${ord.productId} — Size: ${ord.size} — Qty: ${ord.quantity}",
                                                    color = Color.Gray,
                                                    fontSize = 11.sp
                                                )
                                            }

                                            // Top Badge
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(
                                                        when (ord.deliveryStatus) {
                                                            "Delivered" -> SuccessGreen.copy(0.12f)
                                                            "Cancelled" -> ErrorRed.copy(0.12f)
                                                            "Pending" -> HoneyGold.copy(0.12f)
                                                            else -> InfoBlue.copy(0.12f)
                                                        }
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = ord.deliveryStatus,
                                                    color = when (ord.deliveryStatus) {
                                                        "Delivered" -> SuccessGreen
                                                        "Cancelled" -> ErrorRed
                                                        "Pending" -> HoneyGold
                                                        else -> InfoBlue
                                                    },
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }

                                        Divider(color = Color(0xFF334155), thickness = 0.5.dp)

                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text("☎️ Phone: ${ord.customerPhone}", color = Color.LightGray, fontSize = 12.sp)
                                            Text("📍 Address: ${ord.customerAddress}", color = Color.Gray, fontSize = 12.sp)
                                            if (ord.courierName.isNotBlank() || ord.trackingId.isNotBlank()) {
                                                Text("🚚 Logistics: ${if (ord.courierName.isNotBlank()) ord.courierName else "N/A"} • ID: ${if (ord.trackingId.isNotBlank()) ord.trackingId else "Not Tracked"}", color = HoneyGoldLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                            if (ord.notes.isNotBlank()) {
                                                Text("📝 Instructions: ${ord.notes}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Paid indicator
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (ord.deliveryChargeReceived) Icons.Default.CheckCircle else Icons.Default.Pending,
                                                    contentDescription = null,
                                                    tint = if (ord.deliveryChargeReceived) SuccessGreen else HoneyGold,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = if (ord.deliveryChargeReceived) "Charge Received" else "Charge Pending",
                                                    color = if (ord.deliveryChargeReceived) SuccessGreen else HoneyGold,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                            }

                                            // Total Price
                                            Text(
                                                text = "${viewModel.currencySymbol.value} ${ord.totalAmount}",
                                                color = Color.White,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 15.sp
                                            )
                                        }

                                        // Actions: Print Invoice, Edit, & Delete
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    selectedOrderForInvoice = ord
                                                    showInvoicePreviewDialog = true
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ReceiptLong,
                                                    contentDescription = "Visual Shareable Invoice",
                                                    tint = HoneyGold
                                                )
                                            }

                                            if (canEdit) {
                                                IconButton(
                                                    onClick = {
                                                        orderToEdit = ord
                                                        editCustomerPhone = ord.customerPhone
                                                        editCustomerAddress = ord.customerAddress
                                                        editDeliveryStatus = ord.deliveryStatus
                                                        editDeliveryChargeReceived = ord.deliveryChargeReceived
                                                        editQuantity = ord.quantity
                                                        editNotes = ord.notes
                                                        editCourierName = ord.courierName
                                                        editTrackingId = ord.trackingId
                                                        showEditDialog = true
                                                    }
                                                ) {
                                                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit order details", tint = InfoBlue)
                                                }

                                                IconButton(
                                                    onClick = {
                                                        viewModel.deleteOrder(ord.id)
                                                        Toast.makeText(context, "Order records deleted", Toast.LENGTH_SHORT).show()
                                                    }
                                                ) {
                                                    Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Delete", tint = ErrorRed)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showEditDialog && orderToEdit != null) {
            val ord = orderToEdit!!
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                title = { Text("Edit Order Details", fontWeight = FontWeight.Bold, color = Color.White) },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        OutlinedTextField(
                            value = editCustomerPhone,
                            onValueChange = { editCustomerPhone = it },
                            label = { Text("Customer Phone", color = Color.LightGray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = HoneyGold,
                                unfocusedBorderColor = Color(0xFF334155)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = editCustomerAddress,
                            onValueChange = { editCustomerAddress = it },
                            label = { Text("Delivery Address", color = Color.LightGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = HoneyGold,
                                unfocusedBorderColor = Color(0xFF334155)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = editCourierName,
                                onValueChange = { editCourierName = it },
                                label = { Text("Courier", color = Color.LightGray) },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = HoneyGold,
                                    unfocusedBorderColor = Color(0xFF334155)
                                )
                            )

                            OutlinedTextField(
                                value = editTrackingId,
                                onValueChange = { editTrackingId = it },
                                label = { Text("Tracking ID", color = Color.LightGray) },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = HoneyGold,
                                    unfocusedBorderColor = Color(0xFF334155)
                                )
                            )
                        }

                        // Delivery Status Selector
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = editDeliveryStatus,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Delivery Status", color = Color.LightGray) },
                                trailingIcon = {
                                    IconButton(onClick = { showEditStatusDropdown = !showEditStatusDropdown }) {
                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = HoneyGold)
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = HoneyGold,
                                    unfocusedBorderColor = Color(0xFF334155)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            DropdownMenu(
                                expanded = showEditStatusDropdown,
                                onDismissRequest = { showEditStatusDropdown = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .background(SlateCard)
                            ) {
                                val statuses = listOf("Pending", "Confirmed", "Packed", "Dispatched", "In Transit", "Delivered", "Returned", "Refunded", "Cancelled")
                                statuses.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text(s, color = Color.White) },
                                        onClick = {
                                            editDeliveryStatus = s
                                            showEditStatusDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        // Quantity Counter Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Order Quantity:", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = { if (editQuantity > 1) editQuantity-- },
                                    enabled = editQuantity > 1,
                                    modifier = Modifier.background(Color(0xFF334155), CircleShape).size(36.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Desc Quantity", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                                Text("$editQuantity", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                IconButton(
                                    onClick = { editQuantity++ },
                                    modifier = Modifier.background(HoneyGold, CircleShape).size(36.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = "Inc Quantity", tint = Color.Black, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        // Delivery Charge Checked
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { editDeliveryChargeReceived = !editDeliveryChargeReceived }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = editDeliveryChargeReceived,
                                onCheckedChange = { editDeliveryChargeReceived = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = HoneyGold,
                                    checkmarkColor = Color.Black
                                )
                            )
                            Text("Delivery Charge Received?", color = Color.White)
                        }

                        OutlinedTextField(
                            value = editNotes,
                            onValueChange = { editNotes = it },
                            label = { Text("Special Instructions / Notes", color = Color.LightGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                  focusedTextColor = Color.White,
                                  unfocusedTextColor = Color.White,
                                  focusedBorderColor = HoneyGold,
                                  unfocusedBorderColor = Color(0xFF334155)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (editCustomerPhone.isBlank() || editCustomerAddress.isBlank()) {
                                Toast.makeText(context, "Contact coordinates are required", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val newTotal = ord.pricePerUnit * editQuantity

                            val updatedOrder = ord.copy(
                                customerPhone = editCustomerPhone.trim(),
                                customerAddress = editCustomerAddress.trim(),
                                deliveryStatus = editDeliveryStatus,
                                deliveryChargeReceived = editDeliveryChargeReceived,
                                quantity = editQuantity,
                                notes = editNotes.trim(),
                                totalAmount = newTotal,
                                courierName = editCourierName.trim(),
                                trackingId = editTrackingId.trim()
                            )

                            viewModel.updateOrder(
                                order = updatedOrder,
                                onSuccess = {
                                    showEditDialog = false
                                    Toast.makeText(context, "Order successfully updated in database!", Toast.LENGTH_SHORT).show()
                                },
                                onFailure = { errMsg ->
                                    Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HoneyGold, contentColor = Color.Black)
                    ) {
                        Text("Save Changes", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditDialog = false }) {
                        Text("Cancel", color = Color.White)
                    }
                },
                containerColor = SlateCard
            )
        }
        if (showInvoicePreviewDialog && selectedOrderForInvoice != null) {
            val ord = selectedOrderForInvoice!!
            val invoiceFile = remember(ord) {
                com.example.utils.InvoiceGenerator.generateAndSaveInvoice(context, ord)
            }

            AlertDialog(
                onDismissRequest = { showInvoicePreviewDialog = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null, tint = HoneyGold)
                        Text("Invoice Preview 📋", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "A high-fidelity shop receipt with a genuine security certificate has been generated for INV-${ord.id.take(8).uppercase()}.",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )

                        if (invoiceFile != null && invoiceFile.exists()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(340.dp)
                                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp)),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.Black)
                            ) {
                                AsyncImage(
                                    model = invoiceFile,
                                    contentDescription = "Visual Store Invoice",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .background(Color.Black.copy(0.4f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Failed to render invoice image preview.", color = ErrorRed, fontSize = 13.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (invoiceFile != null && invoiceFile.exists()) {
                                try {
                                    val apkUri = FileProvider.getUriForFile(
                                        context,
                                        "com.thrifthive.nasif.fileprovider",
                                        invoiceFile
                                    )
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/png"
                                        putExtra(Intent.EXTRA_STREAM, apkUri)
                                        putExtra(Intent.EXTRA_SUBJECT, "🐝 Thrift-Hive Receipt Invoice: INV-${ord.id.take(8).uppercase()}")
                                        putExtra(Intent.EXTRA_TEXT, "Hello! Attached is the official transaction invoice sheet for your secondhand purchase. Managed by proprietor NASIF HIMADRI.")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share Invoice Image"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not share file: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(context, "Invoice image is missing", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.Black)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Share Image", fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showInvoicePreviewDialog = false }) {
                        Text("Close", color = Color.White)
                    }
                },
                containerColor = SlateCard
            )
        }
    }
}
