package com.example.ui.screens

import android.app.DatePickerDialog
import android.widget.DatePicker
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.models.LossRecord
import com.example.data.models.Product
import com.example.ui.theme.*
import com.example.ui.viewmodel.ThriftViewModel
import com.example.utils.ExcelExporter
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LossCalculatorScreen(
    viewModel: ThriftViewModel
) {
    val context = LocalContext.current
    val products by viewModel.products.collectAsStateWithLifecycle()
    val lossesList by viewModel.losses.collectAsStateWithLifecycle()
    val returnsList by viewModel.returns.collectAsStateWithLifecycle()
    val currency by viewModel.currencySymbol.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0 = Log a Loss, 1 = Loss Summary & NET PROFIT

    // Form states
    var lossType by remember { mutableStateOf("Damaged Stock") } // Damaged Stock | Expired/Unsellable | Delivery Loss | Price Drop Loss | Stolen/Missing | Markdown/Discount Loss | Other
    var showTypeMenu by remember { mutableStateOf(false) }

    var showProductSelector by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }

    var lossQtyInput by remember { mutableStateOf("1") }
    var purchasePricePerUnitInput by remember { mutableStateOf("0") }
    var lossAmountPerUnitInput by remember { mutableStateOf("0") }
    var dateOfLoss by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var severity by remember { mutableStateOf("Medium") } // Low | Medium | High

    // Setup Date defaults on start
    LaunchedEffect(key1 = true) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        dateOfLoss = sdf.format(Date())
    }

    // Autofill purchase price on product selection
    LaunchedEffect(key1 = selectedProduct) {
        if (selectedProduct != null) {
            purchasePricePerUnitInput = selectedProduct!!.purchasePrice.toInt().toString()
            lossAmountPerUnitInput = selectedProduct!!.purchasePrice.toInt().toString() // Default loss to full purchase value
        }
    }

    // Dynamic Calculations
    val lossQty = lossQtyInput.toIntOrNull() ?: 1
    val lossAmtPerUnit = lossAmountPerUnitInput.toDoubleOrNull() ?: 0.0
    val totalLoss = lossQty * lossAmtPerUnit

    // Dynamic Severity Classification
    LaunchedEffect(key1 = totalLoss) {
        severity = if (totalLoss < 1000.0) "Low"
        else if (totalLoss < 5000.0) "Medium"
        else "High"
    }

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, y: Int, m: Int, d: Int ->
            val mCal = Calendar.getInstance()
            mCal.set(y, m, d)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            dateOfLoss = sdf.format(mCal.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Loss calculator 💸", fontWeight = FontWeight.Bold, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyBg)
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
                        text = { Text("Log a Loss", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("Loss Summary", fontWeight = FontWeight.Bold) }
                    )
                }

                if (activeTab == 0) {
                    // TAB 1: Log a Loss Form
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text("Record a Business Deficit", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)

                        // 1. Loss Type selector
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = lossType,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Loss Type Category", color = Color(0xFF94A3B8)) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Dropdown",
                                        tint = HoneyGold,
                                        modifier = Modifier
                                            .clickable { showTypeMenu = true }
                                            .rotate(if (showTypeMenu) 180f else 0f)
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showTypeMenu = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = HoneyGold,
                                    unfocusedBorderColor = Color(0xFF334155)
                                )
                            )

                            DropdownMenu(
                                expanded = showTypeMenu,
                                onDismissRequest = { showTypeMenu = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .background(SlateCard)
                            ) {
                                val types = listOf(
                                    "Damaged Stock", "Expired/Unsellable", "Delivery Loss",
                                    "Price Drop Loss", "Stolen/Missing", "Markdown/Discount Loss", "Other"
                                )
                                types.forEach { item ->
                                    DropdownMenuItem(
                                        text = { Text(item, color = Color.White) },
                                        onClick = {
                                            lossType = item
                                            showTypeMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        // 2. Select Product Selector
                        Card(
                            onClick = { showProductSelector = true },
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
                                if (selectedProduct == null) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = HoneyGold)
                                        Text("Select product (Optional if raw loss)...", color = Color(0xFF94A3B8), fontSize = 13.sp)
                                    }
                                } else {
                                    Column {
                                        Text(selectedProduct!!.productName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Batch: ${selectedProduct!!.batchNo}  •  Rem: ${selectedProduct!!.quantity}", color = Color(0xFF64748B), fontSize = 11.sp)
                                    }
                                }
                                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = HoneyGold, modifier = Modifier.size(12.dp))
                            }
                        }

                        // 3. Row: Quantity + Purchase unit price tracker
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = lossQtyInput,
                                onValueChange = { lossQtyInput = it },
                                label = { Text("Loss Quantity", color = Color(0xFF94A3B8), fontSize = 12.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = HoneyGold,
                                    unfocusedBorderColor = Color(0xFF334155)
                                )
                            )

                            OutlinedTextField(
                                value = lossAmountPerUnitInput,
                                onValueChange = { lossAmountPerUnitInput = it },
                                label = { Text("Deficit Amt/Unit", color = Color(0xFF94A3B8), fontSize = 12.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1.2f),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = HoneyGold,
                                    unfocusedBorderColor = Color(0xFF334155)
                                )
                            )
                        }

                        // Read Only: Total Loss calculations display
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SlateCard.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Computed Net Deficit", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                    Text("Formula: Qty * Deficit Amount", color = Color(0xFF64748B), fontSize = 10.sp)
                                }
                                Text(
                                    text = "$currency ${String.format(Locale.getDefault(), "%,.0f", totalLoss)}",
                                    color = ErrorRed,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // 4. Date of Loss Picker
                        OutlinedTextField(
                            value = dateOfLoss,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Date of Loss", color = Color(0xFF94A3B8)) },
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

                        // 5. Severity Rating selector (Low | Medium | High)
                        Column {
                            Text("Severity Level Indicator", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val severities = listOf("Low", "Medium", "High")
                                severities.forEach { sev ->
                                    val active = severity == sev
                                    val sevColor = when (sev) {
                                        "Low" -> SuccessGreen
                                        "Medium" -> HoneyGold
                                        else -> ErrorRed
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .background(
                                                if (active) sevColor else SlateCard,
                                                RoundedCornerShape(6.dp)
                                            )
                                            .clickable { severity = sev },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = sev,
                                            color = if (active) Color.Black else Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // 6. Description / Notes
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Deficit Cause Description", color = Color(0xFF94A3B8)) },
                            placeholder = { Text("e.g. Broken hanger snagged sleeves causing slight fabric pull...", color = Color(0xFF475569)) },
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

                        // LOG LOSS BUTTON
                        Button(
                            onClick = {
                                val qtyNum = lossQtyInput.toIntOrNull() ?: 0
                                if (qtyNum <= 0) {
                                    Toast.makeText(context, "Please specify a loss quantity", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (lossAmtPerUnit <= 0.0) {
                                    Toast.makeText(context, "Please specify loss amount", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                viewModel.logLoss(
                                    lossType = lossType,
                                    productId = selectedProduct?.productId,
                                    productName = selectedProduct?.productName ?: "Raw Inventory Adjustment",
                                    lossQuantity = lossQty,
                                    purchasePricePerUnit = purchasePricePerUnitInput.toDoubleOrNull() ?: 0.0,
                                    lossAmountPerUnit = lossAmtPerUnit,
                                    dateOfLoss = dateOfLoss,
                                    description = description,
                                    severity = severity
                                )

                                Toast.makeText(context, "Deficit Logged Successfully", Toast.LENGTH_SHORT).show()

                                // Reset form inputs
                                selectedProduct = null
                                lossQtyInput = "1"
                                description = ""
                                lossAmountPerUnitInput = "0"
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HoneyGold, contentColor = Color.Black)
                        ) {
                            Text("Post Deficit Record", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Spacer(modifier = Modifier.height(100.dp))
                    }
                } else if (activeTab == 1) {
                    // TAB 2: Loss Summary & NET PROFIT Audit Ledger
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Total Loss display panel (Red)
                        val totalLossAmount = lossesList.sumOf { it.totalLoss }
                        val totalRefunds = returnsList.sumOf { it.refundAmount }
                        val totalCostAmount = products.sumOf { it.purchasePrice * it.quantity }
                        val totalRevenueAmount = products.sumOf { it.sellingPrice * it.quantity }
                        val netProfitCalculated = totalRevenueAmount - totalCostAmount - totalLossAmount - totalRefunds

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Card(
                                modifier = Modifier.weight(1f).height(100.dp),
                                colors = CardDefaults.cardColors(containerColor = SlateCard),
                                border = BorderStroke(1.dp, ErrorRed.copy(0.4f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                                    Text("Accumulated Deficit", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("$currency ${String.format(Locale.getDefault(), "%,.0f", totalLossAmount)}", color = ErrorRed, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                                    Text("${lossesList.size} raw records", color = Color(0xFF64748B), fontSize = 10.sp)
                                }
                            }

                            Card(
                                modifier = Modifier.weight(1.1f).height(100.dp),
                                colors = CardDefaults.cardColors(containerColor = SlateCard),
                                border = BorderStroke(1.dp, if (netProfitCalculated >= 0) SuccessGreen.copy(0.4f) else ErrorRed.copy(0.4f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                                    Text("Net Store Profit", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "$currency ${String.format(Locale.getDefault(), "%,.0f", netProfitCalculated)}",
                                        color = if (netProfitCalculated >= 0) SuccessGreen else ErrorRed,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text("After returns & losses", color = Color(0xFF64748B), fontSize = 9.sp)
                                }
                            }
                        }

                        // Custom drawn line and pie widgets
                        LossAnalyticsWidget(lossesList, currency)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Logged Deficit records", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                            IconButton(onClick = {
                                if (lossesList.isEmpty()) {
                                    Toast.makeText(context, "No loss records to export", Toast.LENGTH_SHORT).show()
                                } else {
                                    ExcelExporter.exportFullDatabase(context, emptyList(), emptyList(), lossesList)
                                }
                            }) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = "Export Losses", tint = HoneyGold)
                            }
                        }

                        if (lossesList.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = SlateCard)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(imageVector = Icons.Default.Verified, contentDescription = "Cool", tint = SuccessGreen, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No Deficits Encountered! 🐝", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text("Your thrift clothes and shipping lines are running smoothly.", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                lossesList.forEach { loss ->
                                    LossRecordRow(loss, currency) {
                                        viewModel.deleteLoss(loss.id)
                                        Toast.makeText(context, "Deficit entry deleted", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }

            // Selector Full List picker (Dialog setup)
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
                            Text("Select Target Product", fontWeight = FontWeight.Bold)
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
                        Box(modifier = Modifier.size(width = 300.dp, height = 260.dp)) {
                            if (dialogList.isEmpty()) {
                                Text("No items match", modifier = Modifier.align(Alignment.Center))
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
                                                Text("ID: ${prod.productId}  •  Rem: ${prod.quantity}", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                            }
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
        }
    }
}

@Composable
fun LossRecordRow(
    loss: LossRecord,
    currency: String,
    onDelete: () -> Unit
) {
    val sevColor = when (loss.severity) {
        "High" -> ErrorRed
        "Medium" -> HoneyGold
        else -> SuccessGreen
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SlateCard)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .background(ErrorRed.copy(0.12f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(loss.lossType, fontSize = 10.sp, color = ErrorRed, fontWeight = FontWeight.Bold)
                    }

                    Box(
                        modifier = Modifier
                            .background(sevColor.copy(0.15f), CircleShape)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(loss.severity + " Risk", fontSize = 9.sp, color = sevColor, fontWeight = FontWeight.Bold)
                    }
                }

                Text(loss.dateOfLoss, color = Color(0xFF64748B), fontSize = 11.sp)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(loss.productName ?: "Raw Inventory Adjustments", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)

            Spacer(modifier = Modifier.height(4.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    Text("Qty: ${loss.lossQuantity}  •  Deficit/Unit: $currency ${loss.lossAmountPerUnit.toInt()}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    if (loss.description.isNotEmpty()) {
                        Text("Cause: ${loss.description}", color = Color(0xFF64748B), fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("- $currency ${String.format(Locale.getDefault(), "%,.0f", loss.totalLoss)}", color = ErrorRed, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete entry", tint = ErrorRed.copy(0.6f), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun LossAnalyticsWidget(losses: List<LossRecord>, currency: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Text("Deficit Category analysis & Trend line", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(16.dp))

            if (losses.isEmpty()) {
                Text("No data to graph", color = Color(0xFF64748B), fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp))
            } else {
                // Pie arc calculations for breakdown
                val types = listOf("Damaged Stock", "Expired/Unsellable", "Delivery Loss", "Price Drop Loss", "Stolen/Missing", "Markdown/Discount Loss", "Other")
                val totalSum = losses.sumOf { it.totalLoss }.coerceAtLeast(1.0)

                val breakdown = types.map { t ->
                    losses.filter { it.lossType == t }.sumOf { it.totalLoss }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Canvas(modifier = Modifier.size(90.dp)) {
                        val strokeWidth = 14f
                        val colorsList = listOf(ErrorRed, HoneyGold, InfoBlue, SuccessGreen, Color(0xFFA855F7), Color(0xFFEC4899), Color(0xFF64748B))

                        var startAngle = -90f
                        breakdown.forEachIndexed { idx, value ->
                            if (value > 0) {
                                val sweep = (value / totalSum).toFloat() * 360f
                                drawArc(colorsList[idx % colorsList.size], startAngle, sweep, false, style = Stroke(width = strokeWidth))
                                startAngle += sweep
                            }
                        }
                    }

                    // Legend Column
                    Column {
                        val colorsList = listOf(ErrorRed, HoneyGold, InfoBlue, SuccessGreen, Color(0xFFA855F7), Color(0xFFEC4899), Color(0xFF64748B))
                        types.forEachIndexed { i, typeName ->
                            val value = breakdown[i]
                            if (value > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                                    Box(modifier = Modifier.size(8.dp).background(colorsList[i % colorsList.size], CircleShape))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("$typeName: ${(value/1000).toInt()}k", fontSize = 10.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = Color(0xFF334155))
                Spacer(modifier = Modifier.height(16.dp))

                Text("Weekly/Monthly trend graph ($currency)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                // Custom Canvas drawing a trend line
                Canvas(modifier = Modifier.fillMaxWidth().height(60.dp)) {
                    val path = Path()
                    val width = size.width
                    val height = size.height

                    // Dummy interpolation for graphic design representation
                    val points = if (losses.size >= 3) {
                        losses.map { it.totalLoss.toFloat() }.take(6)
                    } else {
                        listOf(400f, 1200f, 800f, 2500f, 1500f, totalSum.toFloat() / 2.5f)
                    }
                    val maxPointVal = points.maxOrNull()?.coerceAtLeast(100f) ?: 100f

                    val stepX = width / (points.size - 1)

                    points.forEachIndexed { idx, point ->
                        val ratioY = (point / maxPointVal) * 0.8f
                        val x = idx * stepX
                        val y = height - (ratioY * height)

                        if (idx == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                        drawCircle(color = ErrorRed, radius = 6f, center = Offset(x, y))
                    }

                    drawPath(path = path, color = ErrorRed, style = Stroke(width = 4f))
                }
            }
        }
    }
}
