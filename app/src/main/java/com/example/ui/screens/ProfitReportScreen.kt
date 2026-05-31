package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.models.Product
import com.example.ui.theme.*
import com.example.ui.viewmodel.ThriftViewModel
import com.example.utils.ExcelExporter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfitReportScreen(
    viewModel: ThriftViewModel
) {
    val context = LocalContext.current
    val products by viewModel.products.collectAsStateWithLifecycle()
    val returnsList by viewModel.returns.collectAsStateWithLifecycle()
    val lossesList by viewModel.losses.collectAsStateWithLifecycle()
    val currency by viewModel.currencySymbol.collectAsStateWithLifecycle()

    var selectedTimeRange by remember { mutableStateOf("Month") } // Week / Month / Year

    // Mathematical Financial calculations
    val totalRevenue = products.sumOf { it.sellingPrice * it.quantity }
    val totalCost = products.sumOf { it.purchasePrice * it.quantity }
    val grossProfit = totalRevenue - totalCost
    val totalLossAmt = lossesList.sumOf { it.totalLoss }
    val totalRefunds = returnsList.sumOf { it.refundAmount }
    val netProfit = totalRevenue - totalCost - totalLossAmt - totalRefunds

    // Margin percentages
    val marginPercentage = if (totalRevenue > 0) ((netProfit / totalRevenue) * 100).toInt() else 0

    // Highlight items
    val bestSellingCategory = remember(products) {
        if (products.isEmpty()) "None"
        else products.groupBy { it.category }
            .maxByOrNull { entry -> entry.value.sumOf { it.quantity } }?.key ?: "None"
    }

    val mostProfitableProduct = remember(products) {
        if (products.isEmpty()) null
        else products.maxByOrNull { it.totalProfit }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Financial Reports 📊", fontWeight = FontWeight.Bold, color = Color.White) },
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
                // Header Range selector chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Week", "Month", "Year").forEach { range ->
                        val active = selectedTimeRange == range
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .background(if (active) HoneyGold else SlateCard, RoundedCornerShape(8.dp))
                                .clickable { selectedTimeRange = range },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "This $range",
                                color = if (active) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // Financial Overview Widget
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SlateCard)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Executive Financial Summary",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                        HorizontalDivider(color = Color(0xFF334155))

                        // Profit numbers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Net business Profit", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("After all refunds & raw losses", color = Color(0xFF64748B), fontSize = 9.sp)
                            }
                            Text(
                                text = "$currency ${String.format(Locale.getDefault(), "%,.0f", netProfit)}",
                                color = if (netProfit >= 0) SuccessGreen else ErrorRed,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        // Progress representation of margin proportion
                        Column(modifier = Modifier.padding(top = 4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Net profit Margin Rate", color = Color(0xFF64748B), fontSize = 10.sp)
                                Text("$marginPercentage%", color = HoneyGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { (marginPercentage.toFloat() / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = HoneyGold,
                                trackColor = Color(0xFF334155)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        HorizontalDivider(color = Color(0xFF334155))

                        // Grid statistics
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Total Revenue", color = Color(0xFF64748B), fontSize = 10.sp)
                                Text("$currency ${String.format(Locale.getDefault(), "%,.0f", totalRevenue)}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Total Cost (COGS)", color = Color(0xFF64748B), fontSize = 10.sp)
                                Text("$currency ${String.format(Locale.getDefault(), "%,.0f", totalCost)}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Settled Refunds", color = Color(0xFF64748B), fontSize = 10.sp)
                                Text("$currency ${String.format(Locale.getDefault(), "%,.0f", totalRefunds)}", color = ErrorRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Deficit & Markdowns", color = Color(0xFF64748B), fontSize = 10.sp)
                                Text("$currency ${String.format(Locale.getDefault(), "%,.0f", totalLossAmt)}", color = ErrorRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Best selling highlights Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f).height(106.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateCard)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
                            Text("Top Category Segment", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Box(
                                modifier = Modifier
                                    .background(HoneyGold.copy(0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(bestSellingCategory, color = HoneyGold, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text("Based on units stocked", color = Color(0xFF64748B), fontSize = 9.sp)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1.1f).height(106.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateCard)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
                            Text("Star Performer", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = mostProfitableProduct?.productName ?: "No products",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Earned $currency ${String.format(Locale.getDefault(), "%,.0f", mostProfitableProduct?.totalProfit ?: 0.0)}",
                                color = SuccessGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Report Table
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Table: Category Performance Grid", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            IconButton(onClick = {
                                if (products.isEmpty()) {
                                    Toast.makeText(context, "No report data", Toast.LENGTH_SHORT).show()
                                } else {
                                    ExcelExporter.exportFullDatabase(context, products, returnsList, lossesList)
                                }
                            }) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = "Export Report", tint = HoneyGold)
                            }
                        }

                        // Table Headers
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F172A), RoundedCornerShape(4.dp))
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Segment", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
                            Text("Stock", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text("Cost", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                            Text("Profit", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f), textAlign = TextAlign.End)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        val categories = listOf("Tops", "Bottoms", "Dresses", "Outerwear", "Accessories", "Footwear", "Other")
                        categories.forEach { cat ->
                            val catProducts = products.filter { it.category == cat }
                            val unitsStocked = catProducts.sumOf { it.quantity }
                            val catCost = catProducts.sumOf { it.purchasePrice * it.quantity }
                            val catProfit = catProducts.sumOf { it.totalProfit }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(cat, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
                                Text("$unitsStocked", color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                Text("${catCost.toInt()}", color = Color(0xFFCBD5E1), fontSize = 12.sp, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                                Text(
                                    text = "${catProfit.toInt()}",
                                    color = if (catProfit >= 0) SuccessGreen else ErrorRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1.5f),
                                    textAlign = TextAlign.End
                                )
                            }
                            HorizontalDivider(color = Color(0xFF0F172A), thickness = 0.5.dp)
                        }
                    }
                }

                // Graphic charts drawing Category profitability proportions
                ProfitCategoryAnalyticsCard(products)

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
fun ProfitCategoryAnalyticsCard(products: List<Product>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SlateCard)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Gross margins category breakdown", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(16.dp))

            if (products.isEmpty()) {
                Text("Insert clothing items for dynamic mapping", color = Color(0xFF64748B), fontSize = 12.sp)
            } else {
                val categories = listOf("Tops", "Bottoms", "Dresses", "Outerwear", "Accessories", "Footwear", "Other")
                val totalMarkupProfit = products.sumOf { it.totalProfit }.coerceAtLeast(1.0)

                categories.forEachIndexed { idx, category ->
                    val profitVal = products.filter { it.category == category }.sumOf { it.totalProfit }.coerceAtLeast(0.0)
                    val percent = (profitVal / totalMarkupProfit).toFloat()

                    if (profitVal > 0) {
                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(category, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                Text("${(percent * 100).toInt()}% contribution", fontSize = 11.sp, color = HoneyGold, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { percent },
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                                color = HoneyGold,
                                trackColor = Color(0xFF0F172A)
                            )
                        }
                    }
                }
            }
        }
    }
}
