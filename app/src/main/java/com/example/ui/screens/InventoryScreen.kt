package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.models.Product
import com.example.ui.theme.*
import com.example.ui.viewmodel.ThriftViewModel
import com.example.utils.ExcelExporter
import java.util.Locale
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: ThriftViewModel,
    onNavigateToAddProduct: () -> Unit,
    onNavigateToEditProduct: (String) -> Unit
) {
    val context = LocalContext.current
    val products by viewModel.products.collectAsStateWithLifecycle()
    val currency by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val orders by viewModel.orders.collectAsStateWithLifecycle()

    // State Variables
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("All") } // All / Pending / InTransit / Delivered / Cancelled
    var sortBy by remember { mutableStateOf("Date") } // Date / Price / Profit / Name
    var sortAscending by remember { mutableStateOf(false) }

    // Bulk action states
    var isBulkMode by remember { mutableStateOf(false) }
    val selectedProductIds = remember { mutableStateListOf<String>() }
    var productToDelete by remember { mutableStateOf<Product?>(null) }

    // Expanded Product Details state
    val expandedProductIds = remember { mutableStateListOf<String>() }

    // Show Sort Menu State
    var showSortMenu by remember { mutableStateOf(false) }

    // Filter, Sort and Search matching logic
    val filteredProducts = remember(products, searchQuery, selectedStatusFilter, sortBy, sortAscending) {
        var list = products.filter {
            val matchesQuery = it.productName.contains(searchQuery, ignoreCase = true) ||
                    it.productId.contains(searchQuery, ignoreCase = true) ||
                    it.batchNo.contains(searchQuery, ignoreCase = true)

            val matchesStatus = selectedStatusFilter == "All" ||
                    it.deliveryStatus.equals(selectedStatusFilter, ignoreCase = true)

            matchesQuery && matchesStatus
        }

        list = when (sortBy) {
            "Price" -> if (sortAscending) list.sortedBy { it.sellingPrice } else list.sortedByDescending { it.sellingPrice }
            "Profit" -> if (sortAscending) list.sortedBy { it.totalProfit } else list.sortedByDescending { it.totalProfit }
            "Name" -> if (sortAscending) list.sortedBy { it.productName } else list.sortedByDescending { it.productName }
            else -> if (sortAscending) list.sortedBy { it.createdAt } else list.sortedByDescending { it.createdAt }
        }
        list
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventory Catalog", fontWeight = FontWeight.Bold, color = Color.White) },
                actions = {
                    IconButton(onClick = {
                        if (products.isEmpty() && orders.isEmpty()) {
                            Toast.makeText(context, "No stock or order data to export", Toast.LENGTH_SHORT).show()
                        } else {
                            ExcelExporter.exportFullDatabase(context, products, emptyList(), emptyList(), orders)
                        }
                    }) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Export Excel", tint = HoneyGold)
                    }
                    if (isBulkMode) {
                        IconButton(onClick = {
                            isBulkMode = false
                            selectedProductIds.clear()
                        }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel bulk select", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyBg)
            )
        },
        floatingActionButton = {
            if (!isBulkMode) {
                FloatingActionButton(
                    onClick = onNavigateToAddProduct,
                    containerColor = HoneyGold,
                    contentColor = Color.Black
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add New Product")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NavyBg)
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Search Bar + Live Sort trigger
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search product name, ID, or batch...", color = Color(0xFF64748B), fontSize = 14.sp) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF64748B)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF64748B))
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = SlateCard,
                            unfocusedContainerColor = SlateCard,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )

                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier
                            .size(52.dp)
                            .background(SlateCard, RoundedCornerShape(10.dp))
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort", tint = HoneyGold)
                    }
                }

                // Status horizontal filter chips
                val filters = listOf("All", "Pending", "InTransit", "Delivered", "Cancelled")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filters.forEach { filterName ->
                        val isSel = selectedStatusFilter == filterName
                        val label = if (filterName == "InTransit") "In Transit" else filterName
                        FilterChip(
                            selected = isSel,
                            onClick = { selectedStatusFilter = filterName },
                            label = { Text(label, color = if (isSel) Color.Black else Color.White) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HoneyGold,
                                containerColor = SlateCard
                            ),
                            border = null
                        )
                    }
                }

                if (isBulkMode) {
                    // Bulk Action Panel at top
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateCard.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Selected: ${selectedProductIds.size} unique items",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                TextButton(
                                    onClick = {
                                        // Mark all selected as Delivered
                                        selectedProductIds.forEach { id ->
                                            viewModel.updateProductDeliveryStatus(id, "Delivered")
                                        }
                                        Toast.makeText(context, "Marked successfully", Toast.LENGTH_SHORT).show()
                                        selectedProductIds.clear()
                                        isBulkMode = false
                                    },
                                    enabled = selectedProductIds.isNotEmpty()
                                ) {
                                    Text("Mark Delivered", color = SuccessGreen, fontWeight = FontWeight.Bold)
                                }

                                TextButton(
                                    onClick = {
                                        // Export only selected products
                                        val selProds = products.filter { selectedProductIds.contains(it.id) }
                                        ExcelExporter.exportFullDatabase(context, selProds, emptyList(), emptyList())
                                        selectedProductIds.clear()
                                        isBulkMode = false
                                    },
                                    enabled = selectedProductIds.isNotEmpty()
                                ) {
                                    Text("Export", color = HoneyGold, fontWeight = FontWeight.Bold)
                                }

                                IconButton(
                                    onClick = {
                                        selectedProductIds.forEach { id ->
                                            viewModel.deleteProductById(id)
                                        }
                                        Toast.makeText(context, "Deleted items", Toast.LENGTH_SHORT).show()
                                        selectedProductIds.clear()
                                        isBulkMode = false
                                    },
                                    enabled = selectedProductIds.isNotEmpty()
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed)
                                }
                            }
                        }
                    }
                }

                // Inventory Listing
                // Compute consolidated grouping of products by productId, preserving selected sort order
                val groupedProducts = remember(filteredProducts) {
                    filteredProducts.groupBy { it.productId }
                }
                val sortedGroupKeys = remember(filteredProducts, groupedProducts) {
                    val seen = mutableSetOf<String>()
                    val keys = mutableListOf<String>()
                    filteredProducts.forEach {
                        if (!seen.contains(it.productId)) {
                            seen.add(it.productId)
                            keys.add(it.productId)
                        }
                    }
                    keys
                }

                if (filteredProducts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "Empty",
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No Products Match Your Filter", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Try spelling name, batch code or adding a product", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(sortedGroupKeys, key = { it }) { productId ->
                            val variants = groupedProducts[productId] ?: emptyList()
                            GroupedInventoryCard(
                                productId = productId,
                                variants = variants,
                                currency = currency,
                                isBulkMode = isBulkMode,
                                selectedProductIds = selectedProductIds,
                                onToggleSelect = { product ->
                                    if (selectedProductIds.contains(product.id)) {
                                        selectedProductIds.remove(product.id)
                                        if (selectedProductIds.isEmpty()) isBulkMode = false
                                    } else {
                                        selectedProductIds.add(product.id)
                                    }
                                },
                                onLongClick = { product ->
                                    if (!isBulkMode) {
                                        isBulkMode = true
                                        selectedProductIds.add(product.id)
                                    }
                                },
                                onEdit = { product -> onNavigateToEditProduct(product.id) },
                                onDelete = { product -> productToDelete = product }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }

        // Dropdown Sort Menu Overlay
        if (showSortMenu) {
            AlertDialog(
                onDismissRequest = { showSortMenu = false },
                title = { Text("Sort Inventory", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Date", "Price", "Profit", "Name").forEach { option ->
                            val active = sortBy == option
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { sortBy = option }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Sort by $option",
                                    color = if (active) HoneyGold else Color.Unspecified,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                                )
                                if (active) {
                                    Icon(
                                        imageVector = if (sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                        contentDescription = "Direction",
                                        tint = HoneyGold
                                    )
                                }
                            }
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { sortAscending = !sortAscending }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Direction")
                            Text(
                                text = if (sortAscending) "Ascending ↑" else "Descending ↓",
                                color = HoneyGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showSortMenu = false },
                        colors = ButtonDefaults.buttonColors(containerColor = HoneyGold, contentColor = Color.Black)
                    ) {
                        Text("Apply")
                    }
                }
            )
        }

        // Product Deletion Confirmation Dialog Overlay
        val toDelete = productToDelete
        if (toDelete != null) {
            AlertDialog(
                onDismissRequest = { productToDelete = null },
                title = { Text("Confirm Deletion", fontWeight = FontWeight.Bold, color = Color.White) },
                text = { Text("Are you sure you want to delete ${toDelete.productName}? This will permanently remove it from your inventory records.", color = Color(0xFFCBD5E1)) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteProduct(toDelete)
                            Toast.makeText(context, "${toDelete.productName} removed", Toast.LENGTH_SHORT).show()
                            productToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, contentColor = Color.White)
                    ) {
                        Text("Remove", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { productToDelete = null }) {
                        Text("Cancel", color = Color.White)
                    }
                },
                containerColor = SlateCard
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupedInventoryCard(
    productId: String,
    variants: List<Product>,
    currency: String,
    isBulkMode: Boolean,
    selectedProductIds: List<String>,
    onToggleSelect: (Product) -> Unit,
    onLongClick: (Product) -> Unit,
    onEdit: (Product) -> Unit,
    onDelete: (Product) -> Unit
) {
    // Current selected variant in this card. Default to first variant
    var selectedIndex by remember(variants) { mutableIntStateOf(0) }
    val activeIndex = if (selectedIndex in variants.indices) selectedIndex else 0
    val activeProduct = if (variants.isNotEmpty()) variants[activeIndex] else null

    if (activeProduct == null) return

    val totalStock = variants.sumOf { it.quantity }
    val isOutOfStock = totalStock == 0

    val statusColor = when (activeProduct.deliveryStatus) {
        "Delivered" -> SuccessGreen
        "InTransit" -> InfoBlue
        "Pending" -> HoneyGold
        else -> ErrorRed
    }

    val profitColor = if (activeProduct.totalProfit >= 0) SuccessGreen else ErrorRed

    val isSelected = selectedProductIds.contains(activeProduct.id)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (isBulkMode) {
                        onToggleSelect(activeProduct)
                    }
                },
                onLongClick = { onLongClick(activeProduct) }
            ),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row (ID, Category, Stock status, Status badge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isBulkMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleSelect(activeProduct) },
                            colors = CheckboxDefaults.colors(checkedColor = HoneyGold),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // SKU/Product SKU Golden Badge
                    Box(
                        modifier = Modifier
                            .background(HoneyGold.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = activeProduct.productId,
                            fontSize = 11.sp,
                            color = HoneyGold,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Category
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF334155), CircleShape)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = activeProduct.category,
                            fontSize = 9.sp,
                            color = Color(0xFFCBD5E1),
                            fontWeight = FontWeight.Bold
                        )
                    }

                }

                // Consolidated Stock Status Badge (Requirement 4)
                Box(
                    modifier = Modifier
                        .background(
                            if (!isOutOfStock) SuccessGreen.copy(alpha = 0.12f) else ErrorRed.copy(alpha = 0.12f),
                            CircleShape
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (!isOutOfStock) "IN STOCK" else "OUT OF STOCK",
                        fontSize = 10.sp,
                        color = if (!isOutOfStock) SuccessGreen else ErrorRed,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title and Total stock display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (activeProduct.imageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = activeProduct.imageUrl,
                            contentDescription = "Variant Image Reference",
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E293B)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E293B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Checkroom,
                                contentDescription = null,
                                tint = HoneyGold.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = activeProduct.productName,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Batch: ${activeProduct.batchNo}",
                            color = Color(0xFF64748B),
                            fontSize = 11.sp
                        )
                    }
                }

                // Total stock count pill at a glance
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .background(HoneyGold.copy(0.12f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = HoneyGold,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "$totalStock Pcs",
                        color = HoneyGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sizes breakdown row with chips
            Text(
                text = "Size Stock Breakdown (Tap to inspect details):",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                variants.forEachIndexed { index, productVariant ->
                    val isChipSelected = index == activeIndex
                    val chipStrokeColor = if (isChipSelected) HoneyGold else Color(0xFF475569)
                    val chipBgColor = if (isChipSelected) HoneyGold.copy(0.15f) else SlateCard

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(chipBgColor)
                            .border(1.dp, chipStrokeColor, RoundedCornerShape(8.dp))
                            .clickable { selectedIndex = index }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = productVariant.size,
                            color = if (isChipSelected) HoneyGold else Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                        Box(
                            modifier = Modifier
                                .background(
                                    if (productVariant.quantity > 0) SuccessGreen.copy(0.2f) else ErrorRed.copy(0.2f),
                                    CircleShape
                                )
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "${productVariant.quantity}",
                                color = if (productVariant.quantity > 0) SuccessGreen else ErrorRed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFF334155), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Selected size metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Purchase (Size ${activeProduct.size})", color = Color(0xFF64748B), fontSize = 10.sp)
                    Text(
                        text = "$currency ${String.format(Locale.getDefault(), "%,.0f", activeProduct.purchasePrice)}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    Text(text = "Selling (Size ${activeProduct.size})", color = Color(0xFF64748B), fontSize = 10.sp)
                    Text(
                        text = "$currency ${String.format(Locale.getDefault(), "%,.0f", activeProduct.sellingPrice)}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    Text(text = "Unit Profit", color = Color(0xFF64748B), fontSize = 10.sp)
                    Text(
                        text = "$currency ${String.format(Locale.getDefault(), "%,.0f", activeProduct.profitPerUnit)}",
                        color = SuccessGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    Text(text = "Total Profit", color = Color(0xFF64748B), fontSize = 10.sp)
                    Text(
                        text = "$currency ${String.format(Locale.getDefault(), "%,.0f", activeProduct.totalProfit)}",
                        color = profitColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (activeProduct.warehouseLocation.isNotBlank() || activeProduct.rackLocation.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(NavyBg)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = HoneyGold, modifier = Modifier.size(12.dp))
                        Text(text = "Logistics Location:", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "Wh: ${if (activeProduct.warehouseLocation.isNotBlank()) activeProduct.warehouseLocation else "Main"} • Rack: ${if (activeProduct.rackLocation.isNotBlank()) activeProduct.rackLocation else "N/A"}",
                        color = HoneyGoldLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action triggers for active variant
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Edit Size ${activeProduct.size}:",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(end = 4.dp)
                )

                IconButton(onClick = { onEdit(activeProduct) }) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit product size", tint = InfoBlue, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { onDelete(activeProduct) }) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete product size", tint = ErrorRed, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun DetailItem(title: String, value: String) {
    Column {
        Text(title, color = Color(0xFF64748B), fontSize = 10.sp)
        Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
