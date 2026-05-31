package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.models.Product
import com.example.ui.theme.*
import com.example.ui.viewmodel.ThriftViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: ThriftViewModel,
    onNavigateToAddProduct: () -> Unit,
    onNavigateToReturnCalc: () -> Unit,
    onNavigateToLossCalc: () -> Unit,
    onNavigateToOrders: () -> Unit,
    onNavigateToProductDetails: (String) -> Unit
) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val returns by viewModel.returns.collectAsStateWithLifecycle()
    val losses by viewModel.losses.collectAsStateWithLifecycle()
    val currency by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    // Calculated fields
    val totalProducts = products.size
    val totalStockUnits = products.sumOf { it.quantity }
    val totalRevenue = products.sumOf { it.sellingPrice * it.quantity }
    val totalProfit = products.sumOf { it.totalProfit }
    val totalReturnsCount = returns.sumOf { it.returnQuantity }
    val totalLossAmt = losses.sumOf { it.totalLoss }

    // Scrollable Column
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "THRIFT-HIVE",
                            fontWeight = FontWeight.Bold,
                            color = HoneyGold,
                            fontSize = 20.sp
                        )
                        Icon(
                            imageVector = Icons.Default.Hive,
                            contentDescription = "Hive",
                            tint = HoneyGold,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                actions = {
                    Text(
                        text = "🐝 NASIF HIMADRI",
                        fontWeight = FontWeight.Bold,
                        color = HoneyGold,
                        modifier = Modifier.padding(end = 16.dp),
                        fontSize = 12.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NavyBg
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NavyBg)
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Welcome header
                item {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = "Welcome to THRIFT-HIVE 🐝",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Smart Inventory & Profit Tracker",
                            fontSize = 14.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                // Realtime Sync Status & Collaboration Hub
                item {
                    val syncState by viewModel.syncStatus.collectAsStateWithLifecycle()
                    val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()
                    val currentUserEmail by viewModel.currentUserEmail.collectAsStateWithLifecycle()
                    val activeRole = "Owner" // Owner is default

                    var expandLogs by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier.fillMaxWidth().animateContentSize(),
                        colors = CardDefaults.cardColors(containerColor = SlateCard),
                        border = BorderStroke(1.dp, if (syncState == "Synced") SuccessGreen.copy(alpha = 0.5f) else HoneyGold.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (syncState == "Synced") Icons.Default.CloudQueue else Icons.Default.CloudSync,
                                        contentDescription = null,
                                        tint = if (syncState == "Synced") SuccessGreen else HoneyGold
                                    )
                                    Column {
                                        Text(
                                            text = "Sync & Collab Central ☁",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "Active: ${currentUserEmail ?: "nasifhimadri@gmail.com"} • $activeRole",
                                            fontSize = 11.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (syncState == "Synced") SuccessGreen.copy(alpha = 0.2f) else HoneyGold.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = syncState.uppercase(),
                                        color = if (syncState == "Synced") SuccessGreen else HoneyGold,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            HorizontalDivider(color = Color(0xFF334155), thickness = 0.5.dp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { expandLogs = !expandLogs },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (expandLogs) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = HoneyGold,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (expandLogs) "Hide Audit Timeline" else "Inspect Realtime Activity Logs",
                                            fontSize = 11.sp,
                                            color = HoneyGold,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                ElevatedButton(
                                    onClick = { viewModel.performCloudSync() },
                                    colors = ButtonDefaults.elevatedButtonColors(
                                        containerColor = HoneyGold,
                                        contentColor = Color.Black
                                    ),
                                    modifier = Modifier.height(30.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Sync Cloud", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            AnimatedVisibility(visible = expandLogs) {
                                Card(
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 120.dp),
                                    colors = CardDefaults.cardColors(containerColor = NavyBg),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp).verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        auditLogs.forEach { log ->
                                            Text(
                                                text = "• $log",
                                                color = Color(0xFF94A3B8),
                                                fontSize = 10.sp,
                                                lineHeight = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick Action Center
                item {
                    Text(
                        text = "Quick Actions",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickActionCard(
                            title = "Add Product",
                            icon = Icons.Default.Add,
                            color = HoneyGold,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToAddProduct
                        )
                        QuickActionCard(
                            title = "Orders & Dispatch",
                            icon = Icons.Default.LocalShipping,
                            color = HoneyGoldLight,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToOrders
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickActionCard(
                            title = "Returns",
                            icon = Icons.AutoMirrored.Filled.CompareArrows,
                            color = InfoBlue,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToReturnCalc
                        )
                        QuickActionCard(
                            title = "Log Loss",
                            icon = Icons.AutoMirrored.Filled.TrendingDown,
                            color = ErrorRed,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToLossCalc
                        )
                    }
                }

                // 2-Column Animated Stat cards
                item {
                    Text(
                        text = "Business Metrics",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatCard(
                                title = "Total Products",
                                value = "$totalProducts",
                                label = "Unique items",
                                icon = Icons.Default.Inventory,
                                iconColor = HoneyGold,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "Total Stock",
                                value = "$totalStockUnits",
                                label = "Units in store",
                                icon = Icons.Default.Dns,
                                iconColor = InfoBlue,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatCard(
                                title = "Total Revenue",
                                value = "$currency ${String.format(Locale.getDefault(), "%,.0f", totalRevenue)}",
                                label = "Potential sales",
                                icon = Icons.Default.AttachMoney,
                                iconColor = SuccessGreen,
                                comicAccent = true,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "Projected Profit",
                                value = "$currency ${String.format(Locale.getDefault(), "%,.0f", totalProfit)}",
                                label = "Estimated markup",
                                icon = Icons.AutoMirrored.Filled.TrendingUp,
                                iconColor = SuccessGreen,
                                highlight = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatCard(
                                title = "Customer Returns",
                                value = "$totalReturnsCount",
                                label = "Returned items",
                                icon = Icons.Default.Autorenew,
                                iconColor = InfoBlue,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "Total Losses",
                                value = "$currency ${String.format(Locale.getDefault(), "%,.0f", totalLossAmt)}",
                                label = "Damaged & markdown",
                                icon = Icons.AutoMirrored.Filled.TrendingDown,
                                iconColor = ErrorRed,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Custom charts display
                item {
                    Text(
                        text = "Sales & Operations Analysis",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    CustomBusinessCharts(products, currency)
                }

                // Recent Additions List (last 5 items)
                item {
                    Text(
                        text = "Recent Handpicked Listings",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                val recentProducts = products.take(5)
                if (recentProducts.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SlateCard)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Inbox,
                                    contentDescription = "Empty",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No Thrift Products Listed Yet",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Tap '+ Add Product' above to begin mapping your store inventory",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                } else {
                    items(recentProducts) { product ->
                        RecentProductRow(product, currency, onNavigateToProductDetails)
                    }
                }

                // Large space at bottom for scrolling comfort
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .height(76.dp),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
    comicAccent: Boolean = false
) {
    val borderBrush = if (highlight) {
        Brush.linearGradient(listOf(HoneyGold, SuccessGreen))
    } else null

    Card(
        modifier = modifier
            .height(108.dp)
            .then(
                if (borderBrush != null) Modifier.border(
                    1.dp,
                    borderBrush,
                    RoundedCornerShape(12.dp)
                ) else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) SlateCard.copy(alpha = 0.9f) else SlateCard
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF94A3B8)
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Column {
                Text(
                    text = value,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (comicAccent) HoneyGold else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun RecentProductRow(
    product: Product,
    currency: String,
    onNavigateToProductDetails: (String) -> Unit
) {
    val statusColor = when (product.deliveryStatus) {
        "Delivered" -> SuccessGreen
        "InTransit" -> InfoBlue
        "Pending" -> HoneyGold
        else -> ErrorRed
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToProductDetails(product.id) },
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon code representation
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(statusColor.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalMall,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = product.productName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = product.productId,
                        color = HoneyGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Qty: ${product.quantity}  •  Size: ${product.size}",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                    Text(
                        text = "$currency ${String.format(Locale.getDefault(), "%,.0f", product.sellingPrice)}",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CustomBusinessCharts(products: List<Product>, currency: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Profit Breakdown by Category ($currency)",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Build category profit dictionary
            val categories = listOf("Tops", "Bottoms", "Dresses", "Outerwear", "Accessories", "Footwear", "Other")
            val profitMap = categories.associateWith { name ->
                products.filter { it.category == name }.sumOf { it.totalProfit }
            }
            val maxProfit = profitMap.values.maxOrNull()?.coerceAtLeast(100.0) ?: 100.0

            // Bar chart row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                categories.forEach { category ->
                    val profitVal = profitMap[category] ?: 0.0
                    val ratio = (profitVal / maxProfit).toFloat().coerceIn(0.01f, 1.0f)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (profitVal > 0) "${(profitVal/1000).toInt()}k" else "0",
                            fontSize = 9.sp,
                            color = HoneyGold,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        Box(
                            modifier = Modifier
                                .width(14.dp)
                                .fillMaxHeight(ratio)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(HoneyGold, HoneyGold.copy(alpha = 0.4f))
                                    ), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = category.take(3),
                            fontSize = 9.sp,
                            color = Color(0xFF94A3B8),
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = Color(0xFF334155), thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Fulfillment Status Distribution",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Count distribution of status
            val delivered = products.filter { it.deliveryStatus == "Delivered" }.sumOf { it.quantity }
            val transit = products.filter { it.deliveryStatus == "InTransit" }.sumOf { it.quantity }
            val pending = products.filter { it.deliveryStatus == "Pending" }.sumOf { it.quantity }
            val cancelled = products.filter { it.deliveryStatus == "Cancelled" }.sumOf { it.quantity }
            val totalStatus = (delivered + transit + pending + cancelled).coerceAtLeast(1)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Circular canvas drawing
                Canvas(modifier = Modifier.size(100.dp)) {
                    val stroke = Stroke(width = 16f)
                    val sweepDelivered = (delivered.toFloat() / totalStatus) * 360f
                    val sweepTransit = (transit.toFloat() / totalStatus) * 360f
                    val sweepPending = (pending.toFloat() / totalStatus) * 360f
                    val sweepCancelled = (cancelled.toFloat() / totalStatus) * 360f

                    var startAngle = -90f
                    // Delivered (Green)
                    drawArc(SuccessGreen, startAngle, sweepDelivered, false, style = stroke)
                    startAngle += sweepDelivered

                    // In Transit (Blue)
                    drawArc(InfoBlue, startAngle, sweepTransit, false, style = stroke)
                    startAngle += sweepTransit

                    // Pending (Gold)
                    drawArc(HoneyGold, startAngle, sweepPending, false, style = stroke)
                    startAngle += sweepPending

                    // Cancelled (Red)
                    drawArc(ErrorRed, startAngle, sweepCancelled, false, style = stroke)
                }

                // Legend
                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(start = 12.dp)
                ) {
                    LegendItem("Delivered ($delivered)", SuccessGreen)
                    LegendItem("In Transit ($transit)", InfoBlue)
                    LegendItem("Pending ($pending)", HoneyGold)
                    LegendItem("Cancelled ($cancelled)", ErrorRed)
                }
            }
        }
    }
}

@Composable
fun LegendItem(text: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = 11.sp,
            color = Color(0xFFF1F5F9),
            fontWeight = FontWeight.Medium
        )
    }
}
