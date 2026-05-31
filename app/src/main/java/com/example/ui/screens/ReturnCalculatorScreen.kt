package com.example.ui.screens

import android.app.DatePickerDialog
import android.widget.DatePicker
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.models.Product
import com.example.data.models.ReturnItem
import com.example.ui.theme.*
import com.example.ui.viewmodel.ThriftViewModel
import java.text.SimpleDateFormat
import java.util.*
import com.example.ui.components.CameraScannerDialog
import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReturnCalculatorScreen(
    viewModel: ThriftViewModel
) {
    val context = LocalContext.current
    val products by viewModel.products.collectAsStateWithLifecycle()
    val returnsList by viewModel.returns.collectAsStateWithLifecycle()
    val currency by viewModel.currencySymbol.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0 = Log Return, 1 = QR Generate, 2 = Return History

    // Multi-Product Returns structure & states
    data class ReturnItemDraft(
        val productId: String,
        val productName: String,
        val size: String,
        val quantity: Int,
        val color: String,
        val sellingPrice: Double,
        val purchasePrice: Double
    )

    val scannedReturnParts = remember { mutableStateListOf<ReturnItemDraft>() }
    var showMultiReturnMatchDialog by remember { mutableStateOf(false) }

    // Multi-Product QR Return Generation Draft
    val qrReturnBuildList = remember { mutableStateListOf<ReturnItemDraft>() }
    var qrBuildProductSelected by remember { mutableStateOf<Product?>(null) }
    var qrBuildSizeSelected by remember { mutableStateOf("M") }
    var qrBuildColorSelected by remember { mutableStateOf("Multicolor") }
    var qrBuildQtySelected by remember { mutableStateOf("1") }
    var showQrGeneratorOutputDialog by remember { mutableStateOf(false) }
    var showQrProductSelector by remember { mutableStateOf(false) }

    // Selection Dialog State
    var showProductSelector by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var returnQtyInput by remember { mutableStateOf("1") }

    // Scanned Parcel Dialog State
    var scannedProductForReturn by remember { mutableStateOf<Product?>(null) }
    var showScanMatchDialog by remember { mutableStateOf(false) }
    var showCameraScannerDialog by remember { mutableStateOf(false) }

    fun handleScannedReturnedParcel(scannedText: String) {
        // Intercept Multi-product Return Package payload
        if (scannedText.startsWith("thrift_hive://returns?")) {
            try {
                scannedReturnParts.clear()
                val uri = android.net.Uri.parse(scannedText)
                val rawData = uri.getQueryParameter("data") ?: "[]"
                // Decode JSON
                val array = JSONArray(rawData)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    scannedReturnParts.add(
                        ReturnItemDraft(
                            productId = obj.optString("id", ""),
                            productName = obj.optString("name", ""),
                            size = obj.optString("size", "M"),
                            quantity = obj.optInt("qty", 1),
                            color = obj.optString("color", "Multicolor"),
                            sellingPrice = obj.optDouble("selling", 0.0),
                            purchasePrice = obj.optDouble("purchase", 0.0)
                        )
                    )
                }
                if (scannedReturnParts.isNotEmpty()) {
                    showMultiReturnMatchDialog = true
                    Toast.makeText(context, "Multi-Product Return Package Identified! 🔒", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Scanned package is empty.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to parse return package: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
            return
        }

        // QR Scan -> Read Product ID & JSON Metadata -> Query Database -> Auto Fill Form
        var targetId = ""
        var targetSize = ""
        var targetQty = 1
        var targetColor = "Multicolor"

        try {
            val trimmed = scannedText.trim()
            if (trimmed.startsWith("{")) {
                val json = org.json.JSONObject(trimmed)
                targetId = json.optString("id", json.optString("productId", ""))
                targetSize = json.optString("size", "")
                targetQty = json.optInt("qty", json.optInt("quantity", 1))
                targetColor = json.optString("color", "Multicolor")
            } else if (trimmed.startsWith("thrift_hive://product?")) {
                val uri = android.net.Uri.parse(trimmed)
                targetId = uri.getQueryParameter("id") ?: ""
                targetSize = uri.getQueryParameter("size") ?: ""
                targetColor = uri.getQueryParameter("color") ?: "Multicolor"
            } else {
                targetId = trimmed
            }
        } catch (e: Exception) {
            targetId = scannedText.trim()
        }

        if (targetId.isBlank()) {
            Toast.makeText(context, "Scanned payload did not yield a valid Product identifier", Toast.LENGTH_SHORT).show()
            return
        }

        // Direct DB Query - retrieves product from Database
        viewModel.getProductByUuid(targetId) { dbProduct ->
            if (dbProduct != null) {
                // Auto Fill Form
                selectedProduct = dbProduct
                returnQtyInput = targetQty.toString()
                scannedProductForReturn = dbProduct
                showScanMatchDialog = true
                Toast.makeText(context, "Cloud-Fetched Product details Auto Filled! 🐝", Toast.LENGTH_LONG).show()
            } else {
                val match = products.find { p -> p.productId == targetId }
                if (match != null) {
                    selectedProduct = match
                    returnQtyInput = targetQty.toString()
                    scannedProductForReturn = match
                    showScanMatchDialog = true
                    Toast.makeText(context, "Product pre-filled from local listing!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "ID: $targetId not matched in the storage database.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun onScanParcelClick() {
        showCameraScannerDialog = true
    }

    // Input States
    var returnReason by remember { mutableStateOf("Wrong Size") } // Defective | Wrong Size | Customer Changed Mind | Damaged in Delivery | Other
    var refundType by remember { mutableStateOf("Full Refund") } // Full Refund | Partial Refund | Exchange
    var partialRefundInput by remember { mutableStateOf("") }
    var returnDate by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var updateStockChecked by remember { mutableStateOf(true) }

    // History filter queries
    var historySearchQuery by remember { mutableStateOf("") }

    // Status dropdowns
    var showReasonMenu by remember { mutableStateOf(false) }
    var showRefundMenu by remember { mutableStateOf(false) }

    // Setup Date defaults on start
    LaunchedEffect(key1 = true) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        returnDate = sdf.format(Date())
    }

    // Mathematical Auto Calculations
    val returnQty = returnQtyInput.toIntOrNull() ?: 1
    val originalSellingPrice = selectedProduct?.sellingPrice ?: 0.0
    val originalPurchasePrice = selectedProduct?.purchasePrice ?: 0.0

    val originalRevenueLost = originalSellingPrice * returnQty
    val calculatedRefundAmount = when (refundType) {
        "Full Refund" -> originalSellingPrice * returnQty
        "Partial Refund" -> (partialRefundInput.toDoubleOrNull() ?: 0.0)
        else -> 0.0 // Exchange = 0 refund
    }
    // Net Loss from Return = amount refunded - value of returned goods if stocked back
    // If not stocking back, we lost the refund AND the purchase price of those damaged models.
    val netValueReturned = if (updateStockChecked) (originalPurchasePrice * returnQty) else 0.0
    val netLossFromReturn = calculatedRefundAmount - netValueReturned

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, y: Int, m: Int, d: Int ->
            val mCal = Calendar.getInstance()
            mCal.set(y, m, d)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            returnDate = sdf.format(mCal.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Product returns 🔄", fontWeight = FontWeight.Bold, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyBg),
                actions = {
                    IconButton(onClick = { onScanParcelClick() }) {
                        Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = "Scan Returned Parcel QR", tint = HoneyGold)
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NavyBg)
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Tab Selection Tabs
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = SlateCard,
                    contentColor = HoneyGold,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                            color = HoneyGold
                        )
                    }
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("Log a Return", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("Generate Return QR", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                    )
                    Tab(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        text = { Text("Return History", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                    )
                }

                if (activeTab == 0) {
                    // TAB 1: Return Calculator Form
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            "Return Product Calculator",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Selector field
                        Card(
                            onClick = { showProductSelector = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SlateCard)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (selectedProduct == null) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = HoneyGold)
                                        Text("Select product from inventory...", color = Color(0xFF94A3B8), fontSize = 14.sp)
                                    }
                                } else {
                                    Column {
                                        Text(selectedProduct!!.productName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text("${selectedProduct!!.productId}  •  Stock remaining: ${selectedProduct!!.quantity}", color = Color(0xFF64748B), fontSize = 12.sp)
                                    }
                                }
                                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = HoneyGold, modifier = Modifier.size(14.dp))
                            }
                        }

                        // Original Price Display Card
                        if (selectedProduct != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = SlateCard.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Retail Selling Price", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                    Text("$currency ${String.format(Locale.getDefault(), "%,.0f", originalSellingPrice)}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }

                        // Field: Return Quantity
                        OutlinedTextField(
                            value = returnQtyInput,
                            onValueChange = { returnQtyInput = it },
                            label = { Text("Quantity to Return", color = Color(0xFF94A3B8)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = HoneyGold,
                                unfocusedBorderColor = Color(0xFF334155)
                            )
                        )

                        // Reason for Return Dropdown
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = returnReason,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Return Reason", color = Color(0xFF94A3B8)) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Dropdown",
                                        tint = HoneyGold,
                                        modifier = Modifier
                                            .clickable { showReasonMenu = true }
                                            .rotate(if (showReasonMenu) 180f else 0f)
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showReasonMenu = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = HoneyGold,
                                    unfocusedBorderColor = Color(0xFF334155)
                                )
                            )

                            DropdownMenu(
                                expanded = showReasonMenu,
                                onDismissRequest = { showReasonMenu = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .background(SlateCard)
                            ) {
                                val reasons = listOf("Defective", "Wrong Size", "Customer Changed Mind", "Damaged in Delivery", "Other")
                                reasons.forEach { r ->
                                    DropdownMenuItem(
                                        text = { Text(r, color = Color.White) },
                                        onClick = {
                                            returnReason = r
                                            showReasonMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        // Date Picker: Return Date
                        OutlinedTextField(
                            value = returnDate,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Return Date Logging", color = Color(0xFF94A3B8)) },
                            leadingIcon = { Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, tint = HoneyGold) },
                            trailingIcon = {
                                IconButton(onClick = { datePickerDialog.show() }) {
                                    Icon(imageVector = Icons.Default.DateRange, contentDescription = "Choose Date", tint = HoneyGold)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = HoneyGold,
                                unfocusedBorderColor = Color(0xFF334155)
                            )
                        )

                        // Refund Type Dropdown
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = refundType,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Refund Action/Type", color = Color(0xFF94A3B8)) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Dropdown",
                                        tint = HoneyGold,
                                        modifier = Modifier
                                            .clickable { showRefundMenu = true }
                                            .rotate(if (showRefundMenu) 180f else 0f)
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showRefundMenu = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = HoneyGold,
                                    unfocusedBorderColor = Color(0xFF334155)
                                )
                            )

                            DropdownMenu(
                                expanded = showRefundMenu,
                                onDismissRequest = { showRefundMenu = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .background(SlateCard)
                            ) {
                                val refunds = listOf("Full Refund", "Partial Refund", "Exchange")
                                refunds.forEach { item ->
                                    DropdownMenuItem(
                                        text = { Text(item, color = Color.White) },
                                        onClick = {
                                            refundType = item
                                            showRefundMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        // Field: Partial Refund Amount (only visible if Partial selected)
                        AnimatedVisibility(visible = refundType == "Partial Refund") {
                            OutlinedTextField(
                                value = partialRefundInput,
                                onValueChange = { partialRefundInput = it },
                                label = { Text("Partial Refund Amount", color = Color(0xFF94A3B8)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                leadingIcon = { Icon(imageVector = Icons.Default.Money, contentDescription = null, tint = HoneyGold) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = HoneyGold,
                                    unfocusedBorderColor = Color(0xFF334155)
                                )
                            )
                        }

                        // Checkbox: Place returned stock back in catalog?
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { updateStockChecked = !updateStockChecked }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = updateStockChecked,
                                onCheckedChange = { updateStockChecked = it },
                                colors = CheckboxDefaults.colors(checkedColor = HoneyGold)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("Restore Stock Quantity", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Tick to place returned items back into live active stock", color = Color(0xFF64748B), fontSize = 11.sp)
                            }
                        }

                        // Field: Return Notes
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Log Return Notes", color = Color(0xFF94A3B8)) },
                            placeholder = { Text("e.g. Returned with tag attached, customer prefers a bigger fit...", color = Color(0xFF475569)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(88.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = HoneyGold,
                                unfocusedBorderColor = Color(0xFF334155)
                            )
                        )

                        // DYNAMIC MATHEMATICAL OUTPUT PREVIEW CARD
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SlateCard),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("Return Estimates summary", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                HorizontalDivider(color = Color(0xFF334155))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Retail Cash Outlay Lost", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                    Text("$currency ${String.format(Locale.getDefault(), "%,.0f", originalRevenueLost)}", color = Color.White, fontSize = 13.sp)
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Refund Payout Cost", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                    Text("$currency ${String.format(Locale.getDefault(), "%,.0f", calculatedRefundAmount)}", color = HoneyGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Restock asset value", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                    Text("$currency ${String.format(Locale.getDefault(), "%,.0f", netValueReturned)}", color = SuccessGreen, fontSize = 13.sp)
                                }

                                HorizontalDivider(color = Color(0xFF334155))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Net Return Deficit (Loss)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    val lossColor = if (netLossFromReturn > 0.0) ErrorRed else SuccessGreen
                                    Text(
                                        text = "$currency ${String.format(Locale.getDefault(), "%,.0f", netLossFromReturn)}",
                                        color = lossColor,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }

                        // CONFIRM RETURN BUTTON
                        Button(
                            onClick = {
                                if (selectedProduct == null) {
                                    Toast.makeText(context, "Please select an inventory product", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val qtyNum = returnQtyInput.toIntOrNull() ?: 0
                                if (qtyNum <= 0) {
                                    Toast.makeText(context, "Please specify a quantity", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                viewModel.confirmReturn(
                                    productId = selectedProduct!!.productId,
                                    productName = selectedProduct!!.productName,
                                    originalSellingPrice = originalSellingPrice,
                                    returnQuantity = returnQty,
                                    returnReason = returnReason,
                                    returnDate = returnDate,
                                    refundType = refundType,
                                    partialRefundAmount = partialRefundInput.toDoubleOrNull() ?: 0.0,
                                    notes = notes,
                                    refundAmount = calculatedRefundAmount,
                                    originalRevenueLost = originalRevenueLost,
                                    netLoss = netLossFromReturn,
                                    updatedStock = updateStockChecked
                                )

                                Toast.makeText(context, "Return logged & Stock updated successfully!", Toast.LENGTH_LONG).show()

                                // Reset form inputs
                                selectedProduct = null
                                returnQtyInput = "1"
                                notes = ""
                                partialRefundInput = ""
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HoneyGold, contentColor = Color.Black)
                        ) {
                            Text("Log product replacement", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Spacer(modifier = Modifier.height(100.dp))
                    }
                } else if (activeTab == 1) {
                    // TAB 1.5: MULTI-PRODUCT RETURN QR GENERATOR (Requirement 3)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            "Multi-Product Return QR Package Builder",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "Select multiple products with specific sizes and quantities. Our algorithm will compress them into a single return-dispatch QR code.",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )

                        // Selector Field for QR Builder
                        Card(
                            onClick = { showQrProductSelector = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SlateCard)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (qrBuildProductSelected == null) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Icon(imageVector = Icons.Default.AddShoppingCart, contentDescription = null, tint = HoneyGold)
                                        Text("Pick product for return bundle...", color = Color(0xFF94A3B8), fontSize = 13.sp)
                                    }
                                } else {
                                    Column {
                                        Text(qrBuildProductSelected!!.productName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("SKU: ${qrBuildProductSelected!!.productId}  •  Color: ${qrBuildProductSelected!!.color}", color = Color(0xFF64748B), fontSize = 11.sp)
                                    }
                                }
                                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = HoneyGold, modifier = Modifier.size(12.dp))
                            }
                        }

                        if (qrBuildProductSelected != null) {
                            val prod = qrBuildProductSelected!!
                            
                            // Size Selection (XS - XXL)
                            Column {
                                Text("Select Size", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val sizeOpts = listOf("XS", "S", "M", "L", "XL", "XXL")
                                    sizeOpts.forEach { item ->
                                        val selected = qrBuildSizeSelected == item
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(38.dp)
                                                .background(
                                                    if (selected) HoneyGold else SlateCard,
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .clickable { qrBuildSizeSelected = item },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = item,
                                                color = if (selected) Color.Black else Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            // Color Selection
                            Column {
                                Text("Select Color Code", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val colorsList = listOf("Multicolor", "Red", "Green", "Blue", "Black", "White", "Yellow", "Pink")
                                    colorsList.forEach { colName ->
                                        val selected = qrBuildColorSelected.equals(colName, ignoreCase = true)
                                        Box(
                                            modifier = Modifier
                                                .background(if (selected) HoneyGold else SlateCard, RoundedCornerShape(6.dp))
                                                .border(1.dp, if (selected) HoneyGold else Color(0xFF334155), RoundedCornerShape(6.dp))
                                                .clickable { qrBuildColorSelected = colName }
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(colName, color = if (selected) Color.Black else Color.White, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                                        }
                                    }
                                }
                            }

                            // Quantity Row & "Add" Action
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Quantity to Return", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                val v = qrBuildQtySelected.toIntOrNull() ?: 1
                                                if (v > 1) qrBuildQtySelected = (v - 1).toString()
                                            },
                                            modifier = Modifier.background(SlateCard, CircleShape).size(36.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Remove, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                        Text(
                                            qrBuildQtySelected,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )
                                        IconButton(
                                            onClick = {
                                                val v = qrBuildQtySelected.toIntOrNull() ?: 1
                                                qrBuildQtySelected = (v + 1).toString()
                                            },
                                            modifier = Modifier.background(SlateCard, CircleShape).size(36.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        val qty = qrBuildQtySelected.toIntOrNull() ?: 1
                                        qrReturnBuildList.add(
                                            ReturnItemDraft(
                                                productId = prod.productId,
                                                productName = prod.productName,
                                                size = qrBuildSizeSelected,
                                                quantity = qty,
                                                color = qrBuildColorSelected,
                                                sellingPrice = prod.sellingPrice,
                                                purchasePrice = prod.purchasePrice
                                            )
                                        )
                                        Toast.makeText(context, "${prod.productName} added to return package list!", Toast.LENGTH_SHORT).show()
                                        // Reset selections
                                        qrBuildProductSelected = null
                                        qrBuildQtySelected = "1"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = HoneyGold),
                                    border = BorderStroke(1.dp, HoneyGold.copy(0.4f)),
                                    modifier = Modifier.padding(top = 16.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add to Package", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Listed parcel list
                        if (qrReturnBuildList.isNotEmpty()) {
                            Text("Consolidated Returned Items (${qrReturnBuildList.size})", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                qrReturnBuildList.forEach { draft ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = SlateCard),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(draft.productName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Text("SKU: ${draft.productId}", color = HoneyGold, fontSize = 11.sp)
                                                    Text("Size: ${draft.size}", color = Color.Gray, fontSize = 11.sp)
                                                    Text("Color: ${draft.color}", color = Color(0xFF38BDF8), fontSize = 11.sp)
                                                    Text("Qty: ${draft.quantity}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            IconButton(onClick = { qrReturnBuildList.remove(draft) }) {
                                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = { showQrGeneratorOutputDialog = true },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = HoneyGold, contentColor = Color.Black)
                            ) {
                                Icon(imageVector = Icons.Default.QrCode, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generate Packages Return QR Code", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        } else {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SlateCard.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(36.dp))
                                    Text("Bulk Package is Empty", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("Pick products above to bundle a unified return delivery dispatch QR code.", color = Color.Gray, fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(100.dp))
                    }
                } else if (activeTab == 2) {
                    // TAB 2: Return History Log
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Dispatched Returns", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text("Grand Refund: $currency ${String.format(Locale.getDefault(), "%,.0f", returnsList.sumOf { it.refundAmount })}", color = ErrorRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Box(
                                modifier = Modifier
                                    .background(HoneyGold.copy(0.12f), CircleShape)
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("${returnsList.size} logs", color = HoneyGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Search Bar filter inside history
                        TextField(
                            value = historySearchQuery,
                            onValueChange = { historySearchQuery = it },
                            placeholder = { Text("Filter returns by product description...", color = Color(0xFF64748B), fontSize = 13.sp) },
                            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF64748B)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .padding(bottom = 10.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = SlateCard,
                                unfocusedContainerColor = SlateCard,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )

                        val filteredReturnsList = returnsList.filter {
                            it.productName.contains(historySearchQuery, ignoreCase = true) ||
                                    it.productId.contains(historySearchQuery, ignoreCase = true) ||
                                    it.returnReason.contains(historySearchQuery, ignoreCase = true)
                        }

                        if (filteredReturnsList.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(imageVector = Icons.Default.FolderZip, contentDescription = "Empty", tint = Color(0xFF475569), modifier = Modifier.size(54.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Zero Returns Logged", color = Color.White, fontSize = 14.sp)
                                    Text("No customer returns recorded in database", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(filteredReturnsList, key = { it.id }) { item ->
                                    ReturnHistoryRow(item, currency) {
                                        viewModel.deleteReturn(item.id)
                                        Toast.makeText(context, "Return log deleted", Toast.LENGTH_SHORT).show()
                                    }
                                }

                                item {
                                    Spacer(modifier = Modifier.height(100.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Expanded Product Selector Dialog (Popup full screen list with live filter)
            if (showProductSelector) {
                var dialogQuery by remember { mutableStateOf("") }
                val dialogList = products.filter {
                    it.productName.contains(dialogQuery, ignoreCase = true) ||
                            it.productId.contains(dialogQuery, ignoreCase = true)
                }

                AlertDialog(
                    onDismissRequest = { showProductSelector = false },
                    title = {
                        Column {
                            Text("Select Stock Product", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            TextField(
                                value = dialogQuery,
                                onValueChange = { dialogQuery = it },
                                placeholder = { Text("Filter products...", fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(6.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )
                        }
                    },
                    text = {
                        Box(modifier = Modifier.size(width = 300.dp, height = 300.dp)) {
                            if (dialogList.isEmpty()) {
                                Text("No items match query", modifier = Modifier.align(Alignment.Center))
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(dialogList) { prod ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedProduct = prod
                                                    showProductSelector = false
                                                }
                                                .padding(vertical = 12.dp, horizontal = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(prod.productName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                                Text("${prod.productId}  •  Rem: ${prod.quantity}", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                            }
                                            Text("$currency ${prod.sellingPrice.toInt()}", color = HoneyGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                        HorizontalDivider(color = Color(0xFF334155))
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showProductSelector = false }) {
                            Text("Dismiss", color = HoneyGold)
                        }
                    },
                    containerColor = SlateCard
                )
            }

            // Custom local Camera/Barcode scanner dialog
            CameraScannerDialog(
                show = showCameraScannerDialog,
                onDismiss = { showCameraScannerDialog = false },
                onCodeScanned = { payload ->
                    handleScannedReturnedParcel(payload)
                }
            )

            // Scanner Autoconfirm Confirmation Overlay Dialog
            if (showScanMatchDialog && scannedProductForReturn != null) {
                val match = scannedProductForReturn!!
                AlertDialog(
                    onDismissRequest = { showScanMatchDialog = false },
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, tint = HoneyGold, modifier = Modifier.size(44.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Returned Parcel Identified! 🔍", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                        }
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("The system detected a returned package for the following SKU variant:", color = Color.LightGray, fontSize = 13.sp)

                            Card(
                                colors = CardDefaults.cardColors(containerColor = NavyBg),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(match.productName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("SKU: ${match.productId}  |  Size: ${match.size}", color = HoneyGold, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    Text("Retail Price: $currency ${match.sellingPrice.toInt()}  |  Cost: $currency ${match.purchasePrice.toInt()}", color = Color.Gray, fontSize = 11.sp)
                                }
                            }

                            Text("Would you like to immediately mark this parcel as returned into inventory (restoring physical stock by +1) and process a standard Full Refund?", color = Color.LightGray, fontSize = 13.sp)
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val originalRevenueLost = match.sellingPrice * 1
                                val refundAmt = match.sellingPrice * 1
                                val netValueReturned = match.purchasePrice * 1
                                val netLoss = refundAmt - netValueReturned

                                viewModel.confirmReturn(
                                    productId = match.productId,
                                    productName = match.productName,
                                    originalSellingPrice = match.sellingPrice,
                                    returnQuantity = 1,
                                    returnReason = "Wrong Size",
                                    returnDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                                    refundType = "Full Refund",
                                    partialRefundAmount = 0.0,
                                    notes = "Processed automatically via Returned Parcel QR Scan.",
                                    refundAmount = refundAmt,
                                    originalRevenueLost = originalRevenueLost,
                                    netLoss = netLoss,
                                    updatedStock = true
                                )
                                Toast.makeText(context, "Return parsed & Stock restored successfully!", Toast.LENGTH_LONG).show()
                                showScanMatchDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.Black)
                        ) {
                            Text("Yes, Log Return", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                selectedProduct = match
                                returnQtyInput = "1"
                                notes = "Loaded via Parcel QR scan."
                                activeTab = 0
                                showScanMatchDialog = false
                            }
                        ) {
                            Text("No, Edit Details", color = HoneyGold)
                        }
                    },
                    containerColor = SlateCard
                )
            }

            // 1. Bulk return item selector (Requirement 3)
            if (showQrProductSelector) {
                var dialogQuery by remember { mutableStateOf("") }
                val dialogList = products.filter {
                    it.productName.contains(dialogQuery, ignoreCase = true) ||
                            it.productId.contains(dialogQuery, ignoreCase = true)
                }

                AlertDialog(
                    onDismissRequest = { showQrProductSelector = false },
                    title = {
                        Column {
                            Text("Select Item for Return Package", fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.height(6.dp))
                            TextField(
                                value = dialogQuery,
                                onValueChange = { dialogQuery = it },
                                placeholder = { Text("Filter products...", fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(6.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )
                        }
                    },
                    text = {
                        Box(modifier = Modifier.size(width = 300.dp, height = 300.dp)) {
                            if (dialogList.isEmpty()) {
                                Text("No items match query", modifier = Modifier.align(Alignment.Center), color = Color.Gray)
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(dialogList) { prod ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    qrBuildProductSelected = prod
                                                    showQrProductSelector = false
                                                }
                                                .padding(vertical = 12.dp, horizontal = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(prod.productName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                                Text("SKU: ${prod.productId}  |  Rem: ${prod.quantity}", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                            }
                                            Text("$currency ${prod.sellingPrice.toInt()}", color = HoneyGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                        HorizontalDivider(color = Color(0xFF334155))
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showQrProductSelector = false }) {
                            Text("Cancel", color = HoneyGold)
                        }
                    },
                    containerColor = NavyBg
                )
            }

            // 2. Scan Confirm Multi-Item Return Match Dialog (Requirement 2 & 5)
            if (showMultiReturnMatchDialog && scannedReturnParts.isNotEmpty()) {
                AlertDialog(
                    onDismissRequest = { showMultiReturnMatchDialog = false },
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Icon(imageVector = Icons.Default.DoneAll, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(44.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Bulk Return Cargo Decoded 📋", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Single Scan parsed multiple products successfully. Here is the returned inventory payload details:", color = Color.LightGray, fontSize = 12.sp)

                            Box(modifier = Modifier.heightIn(max = 220.dp)) {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(scannedReturnParts) { item ->
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = SlateCard),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(item.productName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        Text("SKU: ${item.productId}", color = HoneyGold, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                        Text("Size: ${item.size}", color = Color.LightGray, fontSize = 11.sp)
                                                        Text("Col: ${item.color}", color = Color(0xFF38BDF8), fontSize = 11.sp)
                                                    }
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Text("Qty:", color = Color.Gray, fontSize = 12.sp)
                                                    Text("${item.quantity}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Text("Pressing 'Accept Freight' will automatically restore physical stock in the database and issue customer refunds.", color = Color.LightGray, fontSize = 11.sp)
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                scannedReturnParts.forEach { item ->
                                    val originalRevenueLost = item.sellingPrice * item.quantity
                                    val refundAmt = item.sellingPrice * item.quantity
                                    val netValueReturned = item.purchasePrice * item.quantity
                                    val netLoss = refundAmt - netValueReturned

                                    viewModel.confirmReturn(
                                        productId = item.productId,
                                        productName = item.productName,
                                        originalSellingPrice = item.sellingPrice,
                                        returnQuantity = item.quantity,
                                        returnReason = "Bulk Scanned Return",
                                        returnDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                                        refundType = "Full Refund",
                                        partialRefundAmount = 0.0,
                                        notes = "Processed automatically via single scan QR return package.",
                                        refundAmount = refundAmt,
                                        originalRevenueLost = originalRevenueLost,
                                        netLoss = netLoss,
                                        updatedStock = true,
                                        color = item.color
                                    )
                                }
                                showMultiReturnMatchDialog = false
                                scannedReturnParts.clear()
                                Toast.makeText(context, "All returned quantities logged and stock updated!", Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HoneyGold, contentColor = Color.Black)
                        ) {
                            Text("Accept Freight", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showMultiReturnMatchDialog = false }) {
                            Text("Cancel", color = Color.Gray)
                        }
                    },
                    containerColor = NavyBg
                )
            }

            // 3. Render Combined Returns QR Code Dialog (Requirement 3)
            if (showQrGeneratorOutputDialog && qrReturnBuildList.isNotEmpty()) {
                val listAsJson = org.json.JSONArray().apply {
                    qrReturnBuildList.forEach { draft ->
                        put(org.json.JSONObject().apply {
                            put("id", draft.productId)
                            put("name", draft.productName)
                            put("qty", draft.quantity)
                            put("size", draft.size)
                            put("color", draft.color)
                            put("selling", draft.sellingPrice)
                            put("purchase", draft.purchasePrice)
                        })
                    }
                }.toString()
                val qrPayload = "thrift_hive://returns?data=" + android.net.Uri.encode(listAsJson)

                val consolidatedBitmap = remember(qrPayload) {
                    try {
                        val writer = QRCodeWriter()
                        val bitMatrix = writer.encode(qrPayload, BarcodeFormat.QR_CODE, 512, 512)
                        val w = bitMatrix.width
                        val h = bitMatrix.height
                        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        for (x in 0 until w) {
                            for (y in 0 until h) {
                                bmp.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                            }
                        }
                        bmp
                    } catch (e: Exception) {
                        null
                    }
                }

                AlertDialog(
                    onDismissRequest = { showQrGeneratorOutputDialog = false },
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text("Return Package QR Code", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                            Text("Consolidated return info compressed", fontSize = 11.sp, color = Color.Gray)
                        }
                    },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            if (consolidatedBitmap != null) {
                                Image(
                                    bitmap = consolidatedBitmap.asImageBitmap(),
                                    contentDescription = "Return Package QR",
                                    modifier = Modifier.size(200.dp).background(Color.White).padding(8.dp)
                                )
                            } else {
                                Text("Generating QR failed...", color = ErrorRed)
                            }

                            Text("Scan this code on the 'Log a Return' scanner to instantly parse and check in all returned stock.", color = Color.LightGray, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showQrGeneratorOutputDialog = false
                                qrReturnBuildList.clear()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HoneyGold, contentColor = Color.Black)
                        ) {
                            Text("Finish & Clear", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showQrGeneratorOutputDialog = false }) {
                            Text("Stay", color = Color.Gray)
                        }
                    },
                    containerColor = NavyBg
                )
            }
        }
    }
}

@Composable
fun ReturnHistoryRow(
    item: ReturnItem,
    currency: String,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SlateCard)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .background(HoneyGold.copy(0.12f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(item.productId, fontSize = 11.sp, color = HoneyGold, fontWeight = FontWeight.Bold)
                    }

                    Box(
                        modifier = Modifier
                            .background(Color(0xFF334155), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(item.returnReason, fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Text(item.returnDate, color = Color(0xFF64748B), fontSize = 11.sp)
            }

            Text(item.productName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Qty returned: ${item.returnQuantity}  •  Action: ${item.refundType}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                Text("- $currency ${String.format(Locale.getDefault(), "%,.0f", item.refundAmount)}", color = ErrorRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            if (item.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Note: ${item.notes}", color = Color(0xFF64748B), fontSize = 11.sp)
            }

            HorizontalDivider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (item.updatedStock) "✓ Stock restored to catalog" else "⚠ Damaged (stock lost)",
                    color = if (item.updatedStock) SuccessGreen else ErrorRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete record", tint = ErrorRed.copy(0.7f), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
