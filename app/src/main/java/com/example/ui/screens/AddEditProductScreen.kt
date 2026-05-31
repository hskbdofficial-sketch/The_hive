package com.example.ui.screens

import android.Manifest
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.widget.DatePicker
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.*
import com.example.ui.viewmodel.ThriftViewModel
import com.example.ui.components.CameraScannerDialog
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.barcode.BarcodeScanning
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import android.content.Intent
import java.io.File
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductScreen(
    viewModel: ThriftViewModel,
    productIdToEdit: String?, // Null if adding
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val currency by viewModel.currencySymbol.collectAsState()

    // Form inputs state
    var productId by remember { mutableStateOf("") }
    var batchNo by remember { mutableStateOf("BATCH-A1") }
    var productName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Tops") } // Tops/Bottoms/Dresses/Outerwear/Accessories/Footwear/Other
    var size by remember { mutableStateOf("M") } // XS/S/M/L/XL/XXL
    var purchasePriceInput by remember { mutableStateOf("") }
    var sellingPriceInput by remember { mutableStateOf("") }
    var quantityInput by remember { mutableStateOf("1") }
    var stockStatus by remember { mutableStateOf("In Stock") } // In Stock / Out of Stock
    var notes by remember { mutableStateOf("") }
    var dateAdded by remember { mutableStateOf("") }
    var sellingDate by remember { mutableStateOf("") }
    var deliveryStatus by remember { mutableStateOf("Pending") } // Pending/InTransit/Delivered/Cancelled
    var warehouseLocation by remember { mutableStateOf("Main Warehouse") }
    var rackLocation by remember { mutableStateOf("Rack A1") }
    var imageUrl by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("Multicolor") }
    var lowStockThresholdInput by remember { mutableStateOf("2") }
    var lifecycleHistory by remember { mutableStateOf("Product initial registration.") }

    // Temporary multiple sizes variation list draft
    val variationList = remember { mutableStateListOf<com.example.data.models.Product>() }

    // Loading & Celebrating State
    var isSaving by remember { mutableStateOf(false) }
    var showCelebrate by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }

    // Dropdowns display states
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showDeliveryMenu by remember { mutableStateOf(false) }

    var pendingScanTarget by remember { mutableStateOf<String?>(null) }
    var showCameraScannerDialog by remember { mutableStateOf(false) }
    var showPhotoSourceDialog by remember { mutableStateOf(false) }

    val productImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            imageUrl = uri.toString()
            Toast.makeText(context, "Product image attached successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    val onScanClick: (targetField: String) -> Unit = { target ->
        pendingScanTarget = target
        showCameraScannerDialog = true
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                @Suppress("DEPRECATION")
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                } else {
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }

                val image = InputImage.fromBitmap(bitmap, 0)
                val scanner = BarcodeScanning.getClient()

                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        if (barcodes.isNotEmpty()) {
                            val scannedText = barcodes[0].rawValue ?: barcodes[0].displayValue
                            if (!scannedText.isNullOrBlank()) {
                                if (pendingScanTarget == "batch") {
                                    batchNo = scannedText
                                    Toast.makeText(context, "Image Scanned Batch: $scannedText", Toast.LENGTH_SHORT).show()
                                } else if (pendingScanTarget == "name") {
                                    productName = scannedText
                                    Toast.makeText(context, "Image Scanned Name: $scannedText", Toast.LENGTH_SHORT).show()
                                } else if (pendingScanTarget == "productId") {
                                    productId = scannedText
                                    Toast.makeText(context, "Image Scanned Product ID: $scannedText", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Detected barcode had blank raw values.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "No QR or Barcode detected in this image.", Toast.LENGTH_LONG).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Failed processing image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
            } catch (e: Exception) {
                Toast.makeText(context, "Error loading image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Set Default/Initial values
    LaunchedEffect(key1 = productIdToEdit) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        if (productIdToEdit != null) {
            viewModel.getProductByUuid(productIdToEdit) { existingProduct ->
                if (existingProduct != null) {
                    productId = existingProduct.productId
                    batchNo = existingProduct.batchNo
                    productName = existingProduct.productName
                    category = existingProduct.category
                    size = existingProduct.size
                    purchasePriceInput = existingProduct.purchasePrice.toInt().toString()
                    sellingPriceInput = existingProduct.sellingPrice.toInt().toString()
                    quantityInput = existingProduct.quantity.toString()
                    stockStatus = if (existingProduct.quantity > 0) "In Stock" else "Out of Stock"
                    notes = existingProduct.notes
                    dateAdded = existingProduct.dateAdded
                    sellingDate = existingProduct.sellingDate ?: ""
                    deliveryStatus = existingProduct.deliveryStatus
                    warehouseLocation = existingProduct.warehouseLocation
                    rackLocation = existingProduct.rackLocation
                    imageUrl = existingProduct.imageUrl
                    color = existingProduct.color
                    lowStockThresholdInput = existingProduct.lowStockThreshold.toString()
                    lifecycleHistory = existingProduct.lifecycleHistory
                }
            }
        } else {
            // Generate next available Product ID sequence automatically
            productId = viewModel.getNextProductId()
            dateAdded = sdf.format(Date())
        }
    }

    // Mathematical Profit estimators
    val purchasePrice = purchasePriceInput.toDoubleOrNull() ?: 0.0
    val sellingPrice = sellingPriceInput.toDoubleOrNull() ?: 0.0
    val quantity = quantityInput.toIntOrNull() ?: 0

    val profitPerUnit = sellingPrice - purchasePrice
    val totalProfit = profitPerUnit * quantity

    // Material 3 Date Picker Dialog handlers
    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, y: Int, m: Int, d: Int ->
            val mCal = Calendar.getInstance()
            mCal.set(y, m, d)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            dateAdded = sdf.format(mCal.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val sellingDatePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, y: Int, m: Int, d: Int ->
            val mCal = Calendar.getInstance()
            mCal.set(y, m, d)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            sellingDate = sdf.format(mCal.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (productIdToEdit != null) "Edit Thrift Product" else "Add Thrift Product", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Field: Product ID (Allows manual typing, defaulted to auto-generated sequence)
                OutlinedTextField(
                    value = productId,
                    onValueChange = { productId = it },
                    label = { Text("Product ID / Custom Identifier", color = Color(0xFF94A3B8)) },
                    placeholder = { Text("e.g. TH-001", color = Color(0xFF475569)) },
                    leadingIcon = { Icon(imageVector = Icons.Default.QrCode, contentDescription = null, tint = HoneyGold) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = HoneyGold,
                        unfocusedBorderColor = Color(0xFF334155),
                        cursorColor = HoneyGold
                    )
                )

                // Field: Product Name
                OutlinedTextField(
                    value = productName,
                    onValueChange = { productName = it },
                    label = { Text("Product Name", color = Color(0xFF94A3B8)) },
                    placeholder = { Text("e.g. vintage denim skirt", color = Color(0xFF475569)) },
                    leadingIcon = { Icon(imageVector = Icons.Default.LocalMall, contentDescription = null, tint = HoneyGold) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = HoneyGold,
                        unfocusedBorderColor = Color(0xFF334155),
                        cursorColor = HoneyGold
                    )
                )

                // Field: Batch Number
                OutlinedTextField(
                    value = batchNo,
                    onValueChange = { batchNo = it },
                    label = { Text("Batch Number", color = Color(0xFF94A3B8)) },
                    placeholder = { Text("e.g. BATCH-A1", color = Color(0xFF475569)) },
                    leadingIcon = { Icon(imageVector = Icons.Default.QrCode, contentDescription = null, tint = HoneyGold) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = HoneyGold,
                        unfocusedBorderColor = Color(0xFF334155)
                    )
                )

                // Row: Category (Dropdown selector)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category", color = Color(0xFF94A3B8)) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown",
                                tint = HoneyGold,
                                modifier = Modifier
                                    .clickable { showCategoryMenu = true }
                                    .rotate(if (showCategoryMenu) 180f else 0f)
                            )
                        },
                        leadingIcon = { Icon(imageVector = Icons.Default.Category, contentDescription = null, tint = HoneyGold) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCategoryMenu = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = HoneyGold,
                            unfocusedBorderColor = Color(0xFF334155)
                        )
                    )

                    DropdownMenu(
                        expanded = showCategoryMenu,
                        onDismissRequest = { showCategoryMenu = false },
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .background(SlateCard)
                    ) {
                        val categoriesList = listOf("Tops", "Bottoms", "Dresses", "Outerwear", "Accessories", "Footwear", "Other")
                        categoriesList.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat, color = Color.White) },
                                onClick = {
                                    category = cat
                                    showCategoryMenu = false
                                }
                            )
                        }
                    }
                }

                // Row: Size (Segment Selector)
                Column {
                    Text("Selected Clothing Size", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val sizesList = listOf("XS", "S", "M", "L", "XL", "XXL")
                        sizesList.forEach { s ->
                            val active = size == s
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .background(
                                        if (active) HoneyGold else SlateCard,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { size = s },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = s,
                                    color = if (active) Color.Black else Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Row: Clothing Color Theme Selection (Requirement 5)
                Column {
                    Text("Product Color Code Strategy", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val colorsList = listOf(
                            "Multicolor" to Color.Transparent,
                            "Red" to Color(0xFFEF4444),
                            "Green" to Color(0xFF22C55E),
                            "Blue" to Color(0xFF3B82F6),
                            "Black" to Color(0xFF0F172A),
                            "White" to Color.White,
                            "Yellow" to Color(0xFFEAB308),
                            "Pink" to Color(0xFFEC4899),
                            "Orange" to Color(0xFFF97316),
                            "Grey" to Color(0xFF64748B)
                        )
                        colorsList.forEach { (colName, colValue) ->
                            val active = color.equals(colName, ignoreCase = true)
                            val borderCol = if (active) HoneyGold else Color(0xFF334155)
                            val borderThickness = if (active) 2.dp else 1.dp
                            
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SlateCard)
                                    .border(borderThickness, borderCol, RoundedCornerShape(6.dp))
                                    .clickable { color = colName }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .background(
                                            if (colName == "Multicolor") Color.Transparent else colValue,
                                            CircleShape
                                        )
                                        .border(
                                            1.dp,
                                            if (colName == "Multicolor") Color.White else Color.White.copy(alpha = 0.4f),
                                            CircleShape
                                        )
                                ) {
                                    if (colName == "Multicolor") {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            drawLine(
                                                color = Color.Red,
                                                start = Offset(0f, this.size.height),
                                                end = Offset(this.size.width, 0f),
                                                strokeWidth = 1.5.dp.toPx()
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = colName,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // --- MULTIPLE SIZES VARIATION CONTROLLERS ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SlateCard.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Multiple Size & Qty Manager 👕",
                            color = HoneyGold,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Quickly log differing quantities or prices across multiple sizes for this product.",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (productName.isBlank()) {
                                Toast.makeText(context, "Set product name first!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (purchasePrice <= 0.0 || sellingPrice <= 0.0) {
                                Toast.makeText(context, "Fill Prices below first!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (quantity <= 0) {
                                Toast.makeText(context, "Choose a Quantity greater than zero first!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val suffix = if (variationList.isEmpty()) "" else "-$size"
                            val variationItem = com.example.data.models.Product(
                                id = UUID.randomUUID().toString(),
                                productId = "$productId$suffix",
                                batchNo = batchNo,
                                productName = "$productName ($size)",
                                category = category,
                                size = size,
                                purchasePrice = purchasePrice,
                                sellingPrice = sellingPrice,
                                profitPerUnit = profitPerUnit,
                                quantity = quantity,
                                totalProfit = totalProfit,
                                dateAdded = dateAdded,
                                sellingDate = if (sellingDate.isBlank()) null else sellingDate,
                                deliveryStatus = deliveryStatus,
                                notes = notes,
                                imageUrl = imageUrl,
                                color = color,
                                createdAt = System.currentTimeMillis().toString()
                            )

                            // Replace if size already exists in temporary draft
                            variationList.removeAll { it.size == size }
                            variationList.add(variationItem)
                            Toast.makeText(context, "Added variation: Size $size ($quantity units)", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E293B),
                            contentColor = HoneyGold
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, HoneyGold.copy(0.5f))
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Size variation ($size - $quantity Pcs - $sellingPrice $currency)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    if (variationList.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Draft Variations List:",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            variationList.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(SlateCard, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(text = "Size ${item.size} (${item.quantity} units)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(text = "Price: $currency ${item.sellingPrice} | Unit ID: ${item.productId}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                    }
                                    IconButton(
                                        onClick = { variationList.remove(item) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Draft", tint = ErrorRed, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Row: Prices Inputs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = purchasePriceInput,
                        onValueChange = { purchasePriceInput = it },
                        label = { Text("Purchase Price ($currency)", color = Color(0xFF94A3B8), fontSize = 12.sp) },
                        placeholder = { Text(currency, color = Color(0xFF475569)) },
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
                        value = sellingPriceInput,
                        onValueChange = { sellingPriceInput = it },
                        label = { Text("Selling Price ($currency)", color = Color(0xFF94A3B8), fontSize = 12.sp) },
                        placeholder = { Text(currency, color = Color(0xFF475569)) },
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
                }

                // Read Only: Auto-Profit Estimate Preview
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SlateCard.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Profit / Unit", color = Color(0xFF64748B), fontSize = 11.sp)
                            val color = if (profitPerUnit >= 0) SuccessGreen else ErrorRed
                            Text(
                                text = "$currency ${String.format(Locale.getDefault(), "%,.0f", profitPerUnit)}",
                                color = color,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Estimated Total Gain", color = Color(0xFF64748B), fontSize = 11.sp)
                            val color = if (totalProfit >= 0) SuccessGreen else ErrorRed
                            Text(
                                text = "$currency ${String.format(Locale.getDefault(), "%,.0f", totalProfit)}",
                                color = color,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Field: Quantity
                OutlinedTextField(
                    value = quantityInput,
                    onValueChange = {
                        quantityInput = it
                        val q = it.toIntOrNull() ?: 0
                        stockStatus = if (q > 0) "In Stock" else "Out of Stock"
                    },
                    label = { Text("Total Quantity Listed", color = Color(0xFF94A3B8)) },
                    placeholder = { Text("1", color = Color(0xFF475569)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = { Icon(imageVector = Icons.Default.Inbox, contentDescription = null, tint = HoneyGold) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = HoneyGold,
                        unfocusedBorderColor = Color(0xFF334155)
                    )
                )

                // Stock Status (In Stock / Out of Stock Segment Selector)
                Column {
                    Text("Stock Status / Availability", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        listOf("In Stock", "Out of Stock").forEach { option ->
                            val active = stockStatus == option
                            val color = if (option == "In Stock") SuccessGreen else ErrorRed
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .background(
                                        if (active) color.copy(alpha = 0.2f) else SlateCard,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (active) color else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        stockStatus = option
                                        if (option == "Out of Stock") {
                                            quantityInput = "0"
                                        } else if (quantityInput == "0" || quantityInput.isBlank()) {
                                            quantityInput = "1"
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (option == "In Stock") Icons.Default.CheckCircle else Icons.Default.Cancel,
                                        contentDescription = null,
                                        tint = if (active) color else Color(0xFF64748B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = option,
                                        color = if (active) Color.White else Color(0xFF94A3B8),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Date Picker: Date Added
                OutlinedTextField(
                    value = dateAdded,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date Added", color = Color(0xFF94A3B8)) },
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

                // Delivery Status (Dropdown menu)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = deliveryStatus,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Delivery Status", color = Color(0xFF94A3B8)) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown",
                                tint = HoneyGold,
                                modifier = Modifier
                                    .clickable { showDeliveryMenu = true }
                                    .rotate(if (showDeliveryMenu) 180f else 0f)
                            )
                        },
                        leadingIcon = { Icon(imageVector = Icons.Default.LocalShipping, contentDescription = null, tint = HoneyGold) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDeliveryMenu = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = HoneyGold,
                            unfocusedBorderColor = Color(0xFF334155)
                        )
                    )

                    DropdownMenu(
                        expanded = showDeliveryMenu,
                        onDismissRequest = { showDeliveryMenu = false },
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .background(SlateCard)
                    ) {
                        val statuses = listOf("Pending", "InTransit", "Delivered", "Cancelled")
                        statuses.forEach { stat ->
                            DropdownMenuItem(
                                text = { Text(stat, color = Color.White) },
                                onClick = {
                                    deliveryStatus = stat
                                    showDeliveryMenu = false
                                }
                            )
                        }
                    }
                }

                // Optional: Selling Date Picker (only editable if Delivered/Cancelled)
                OutlinedTextField(
                    value = sellingDate,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Selling Date (Optional)", color = Color(0xFF94A3B8)) },
                    leadingIcon = { Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, tint = HoneyGold) },
                    trailingIcon = {
                        IconButton(onClick = { sellingDatePickerDialog.show() }) {
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

                // Field: Notes Description
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Add Notes", color = Color(0xFF94A3B8)) },
                    placeholder = { Text("e.g. vintage tags attached, slight tear inside pocket...", color = Color(0xFF475569)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = HoneyGold,
                        unfocusedBorderColor = Color(0xFF334155)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // --- NEW PRODUCTION-GRADE INVENTORY CONTROLS ---
                Text(
                    text = "Warehouse & Logistics",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = warehouseLocation,
                        onValueChange = { warehouseLocation = it },
                        label = { Text("Warehouse Location", color = Color(0xFF94A3B8)) },
                        placeholder = { Text("e.g. Warehouse A", color = Color(0xFF475569)) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Store, contentDescription = null, tint = HoneyGold) },
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
                        value = rackLocation,
                        onValueChange = { rackLocation = it },
                        label = { Text("Rack & Shelf ID", color = Color(0xFF94A3B8)) },
                        placeholder = { Text("e.g. Section F-3", color = Color(0xFF475569)) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Grid3x3, contentDescription = null, tint = HoneyGold) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = HoneyGold,
                            unfocusedBorderColor = Color(0xFF334155)
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = lowStockThresholdInput,
                        onValueChange = { lowStockThresholdInput = it },
                        label = { Text("Low Stock Threshold", color = Color(0xFF94A3B8)) },
                        placeholder = { Text("e.g. 2", color = Color(0xFF475569)) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = HoneyGold) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = HoneyGold,
                            unfocusedBorderColor = Color(0xFF334155)
                        )
                    )

                    // Mock Product Photo Selection Card / Gallery Display (Requirement 4 & 5)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SlateCard)
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                            .clickable {
                                showPhotoSourceDialog = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (imageUrl.isNotEmpty()) Icons.Default.CheckCircle else Icons.Default.AddAPhoto,
                                contentDescription = null,
                                tint = if (imageUrl.isNotEmpty()) SuccessGreen else HoneyGold
                            )
                            Text(
                                text = if (imageUrl.isNotEmpty()) "Photo Attached" else "Add Photo",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (showPhotoSourceDialog) {
                        AlertDialog(
                            onDismissRequest = { showPhotoSourceDialog = false },
                            title = { Text("Select Logo / Photo Source", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Button(
                                        onClick = {
                                            showPhotoSourceDialog = false
                                            productImagePickerLauncher.launch("image/*")
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = HoneyGold, contentColor = Color.Black),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Choose from Local Gallery", fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            showPhotoSourceDialog = false
                                            // Pre-loaded stunning apparel apparel background preset URLs
                                            val presetsList = listOf(
                                                "https://images.unsplash.com/photo-1542291026-7eec264c27ff", // Red apparel
                                                "https://images.unsplash.com/photo-1523381210434-271e8be1f52b", // Jackets
                                                "https://images.unsplash.com/photo-1583743814966-8936f5b7be1a", // T-shirt black
                                                "https://images.unsplash.com/photo-1618354691373-d851c5c3a990"  // Casual shoes
                                            )
                                            imageUrl = presetsList.random()
                                            Toast.makeText(context, "Thrift apparel apparel photography linked!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                        border = BorderStroke(1.dp, Color(0xFF334155)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = HoneyGold)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Link High-Res Preset Photo")
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showPhotoSourceDialog = false }) {
                                    Text("Dismiss", color = HoneyGold)
                                }
                            },
                            containerColor = SlateCard
                        )
                    }
                }

                // AI suggestions card dynamically generated based on current values
                val sampleProd = com.example.data.models.Product(
                    productId = productId.ifBlank { "PROP" },
                    batchNo = batchNo.ifBlank { "B-1" },
                    productName = productName.ifBlank { "Vintage Item" },
                    category = category,
                    size = size,
                    purchasePrice = purchasePrice,
                    sellingPrice = sellingPrice,
                    profitPerUnit = profitPerUnit,
                    quantity = quantity,
                    totalProfit = totalProfit,
                    dateAdded = dateAdded,
                    deliveryStatus = deliveryStatus,
                    lowStockThreshold = lowStockThresholdInput.toIntOrNull() ?: 2
                )
                val aiInsightFlow = viewModel.getAIInsightsForProduct(sampleProd)
                val aiInsight by aiInsightFlow.collectAsState()

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SlateCard.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, HoneyGold.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = HoneyGold, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("BeeHive AI Price & Stock Suggestions 🧠", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("• Suggestions: ${aiInsight.smartPriceSuggestion}", color = Color.LightGray, fontSize = 11.sp, lineHeight = 15.sp)
                            Text("• Turnover: ${aiInsight.forecasting}", color = Color.LightGray, fontSize = 11.sp, lineHeight = 15.sp)
                            Text("• Stock Risk: ${aiInsight.deadStockRisk}", color = Color.LightGray, fontSize = 11.sp, lineHeight = 15.sp)
                            Text("• Returns predictor: ${aiInsight.returnRiskPrediction}", color = Color.LightGray, fontSize = 11.sp, lineHeight = 15.sp)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(
                                onClick = {
                                    val suggestedPrice = (purchasePrice * 1.5).coerceAtLeast(sellingPrice)
                                    sellingPriceInput = suggestedPrice.toInt().toString()
                                    Toast.makeText(context, "Applied suggested price: $currency ${suggestedPrice.toInt()}", Toast.LENGTH_SHORT).show()
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Text("Apply suggesting markup", color = HoneyGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // --- PRODUCT LIFECYCLE TIMELINE AUDIT HISTORY ---
                if (productIdToEdit != null && lifecycleHistory.isNotEmpty()) {
                    Text(
                        text = "Product Life History Timeline 📋",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SlateCard)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            lifecycleHistory.split(";").filter { it.isNotBlank() }.reversed().forEach { logEntry ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = HoneyGoldLight,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = logEntry.trim(),
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // --- QR GENERATION ACTIONS ---
                OutlinedButton(
                    onClick = {
                        if (productId.isBlank()) {
                            Toast.makeText(context, "Define a Product ID SKU first", Toast.LENGTH_SHORT).show()
                        } else {
                            showQrDialog = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HoneyGold),
                    border = BorderStroke(1.dp, HoneyGold)
                ) {
                    Icon(imageVector = Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate & Share Product QR Code", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                if (showQrDialog) {
                    val qrPayload = "thrift_hive://product?id=$productId&size=$size"
                    val qrBitmap = remember(qrPayload) {
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
                        onDismissRequest = { showQrDialog = false },
                        title = {
                            Text(
                                text = "Product QR Code ID Generator",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 18.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        },
                        text = {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Scan target: $productName (Size $size)\nSKU Payload: $qrPayload",
                                    color = Color.LightGray,
                                    fontSize = 12.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )

                                if (qrBitmap != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(240.dp)
                                            .background(Color.White, RoundedCornerShape(12.dp))
                                            .padding(12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            bitmap = qrBitmap.asImageBitmap(),
                                            contentDescription = "Generated QR Code",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Button(
                                        onClick = {
                                            try {
                                                val cachePath = File(context.cacheDir, "shared_qr")
                                                cachePath.mkdirs()
                                                val file = File(cachePath, "${productId}_${size}_qr.png")
                                                file.outputStream().use { out ->
                                                    qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                                                }
                                                val fileUri = FileProvider.getUriForFile(
                                                    context,
                                                    "com.thrifthive.nasif.fileprovider",
                                                    file
                                                )
                                                val intent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "image/png"
                                                    putExtra(Intent.EXTRA_STREAM, fileUri)
                                                    putExtra(Intent.EXTRA_SUBJECT, "THRIFT-HIVE QR Code: $productId ($size)")
                                                    putExtra(Intent.EXTRA_TEXT, "Scan this QR code in THRIFT-HIVE to locate or process returns for $productName.")
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(Intent.createChooser(intent, "Share QR Code Image"))
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Could not share QR image: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = HoneyGold, contentColor = Color.Black),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Share QR Code Image", fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Text("Failed to generate QR bitmap", color = ErrorRed)
                                }

                                TextButton(
                                    onClick = {
                                        // Export ALL calculated system products with QR values inside CSV
                                        viewModel.products.value.let { allProducts ->
                                            if (allProducts.isEmpty()) {
                                                val singleProdList = listOf(
                                                    com.example.data.models.Product(
                                                        productId = productId,
                                                        batchNo = batchNo,
                                                        productName = productName,
                                                        category = category,
                                                        size = size,
                                                        purchasePrice = purchasePrice,
                                                        sellingPrice = sellingPrice,
                                                        quantity = quantity,
                                                        deliveryStatus = deliveryStatus,
                                                        notes = notes,
                                                        totalProfit = totalProfit,
                                                        profitPerUnit = profitPerUnit,
                                                        dateAdded = dateAdded
                                                    )
                                                )
                                                com.example.utils.ExcelExporter.exportQrCatalog(context, singleProdList)
                                            } else {
                                                com.example.utils.ExcelExporter.exportQrCatalog(context, allProducts)
                                            }
                                        }
                                        showQrDialog = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, tint = HoneyGold, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Export QRs Catalog CSV", color = HoneyGold, fontWeight = FontWeight.Bold)
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = { showQrDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray, contentColor = Color.White)
                            ) {
                                Text("Close")
                            }
                        },
                        containerColor = SlateCard
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // SAVE BUTTON
                Button(
                    onClick = {
                        // Form validations
                        if (productId.isBlank()) {
                            Toast.makeText(context, "Please enter a Product ID", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (productName.isBlank()) {
                            Toast.makeText(context, "Please enter a Product Name", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        // Only do price validation on single product mode
                        if (variationList.isEmpty()) {
                            if (purchasePrice <= 0.0 || sellingPrice <= 0.0) {
                                Toast.makeText(context, "Please enter valid financial prices", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (quantity < 0) {
                                Toast.makeText(context, "Please enter a valid listing quantity", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                        }

                        isSaving = true
                        if (variationList.isNotEmpty()) {
                            // Bulk save all generated size variations
                            viewModel.saveProducts(variationList.toList())
                        } else {
                            val updatedHistory = if (productIdToEdit != null) {
                                "Amended on $dateAdded: qty=$quantity, loc=$warehouseLocation; " + lifecycleHistory
                            } else {
                                "Product registered. Initial stock: $quantity in $warehouseLocation ($rackLocation) on $dateAdded; "
                            }

                            // Save standard/single product flow
                            viewModel.saveProduct(
                                id = productIdToEdit,
                                productId = productId,
                                batchNo = batchNo,
                                productName = productName,
                                category = category,
                                size = size,
                                purchasePrice = purchasePrice,
                                sellingPrice = sellingPrice,
                                quantity = quantity,
                                dateAdded = dateAdded,
                                sellingDate = sellingDate.ifBlank { null },
                                deliveryStatus = deliveryStatus,
                                notes = notes,
                                warehouseLocation = warehouseLocation,
                                rackLocation = rackLocation,
                                imageUrl = imageUrl,
                                color = color,
                                lowStockThreshold = lowStockThresholdInput.toIntOrNull() ?: 2,
                                lifecycleHistory = updatedHistory
                            )
                        }

                        // Trigger animation sequence
                        showCelebrate = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HoneyGold, contentColor = Color.Black)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = if (productIdToEdit != null) "Update Product Record" else "Stock Inventory Item",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }

            // Confetti/Aesthetic Success Celebration Popup Drawer
            if (showCelebrate) {
                AlertDialog(
                    onDismissRequest = {
                        showCelebrate = false
                        onNavigateBack()
                    },
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Icon(imageVector = Icons.Default.TaskAlt, contentDescription = "Success", tint = SuccessGreen, modifier = Modifier.size(54.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Inventory Stocked! 🐝", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    },
                    text = {
                        Text(
                            text = "Product '$productName' of batch $batchNo is logged successfully under ID $productId.",
                            color = Color(0xFFCBD5E1),
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showCelebrate = false
                                onNavigateBack()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HoneyGold, contentColor = Color.Black)
                        ) {
                            Text("Acknowledge")
                        }
                    },
                    containerColor = SlateCard
                )
            }
        }
    }
}
