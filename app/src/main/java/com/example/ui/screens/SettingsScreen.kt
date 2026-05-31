package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import com.example.ui.viewmodel.ThriftViewModel
import com.example.utils.ExcelExporter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ThriftViewModel,
    onNavigateToTeamSharing: () -> Unit,
    onLogOutSuccess: () -> Unit
) {
    val context = LocalContext.current

    // Live variables collected from VM
    val storeName by viewModel.storeName.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()

    val products by viewModel.products.collectAsStateWithLifecycle()
    val returnsList by viewModel.returns.collectAsStateWithLifecycle()
    val lossesList by viewModel.losses.collectAsStateWithLifecycle()
    val ordersList by viewModel.orders.collectAsStateWithLifecycle()

    // Screen states
    var storeNameInput by remember { mutableStateOf("") }
    var expandCurrencyMenu by remember { mutableStateOf(false) }

    // Dialog state
    var showResetDialogFirst by remember { mutableStateOf(false) }
    var showResetDialogSecond by remember { mutableStateOf(false) }

    // Prepopulate name input state
    LaunchedEffect(key1 = storeName) {
        storeNameInput = storeName
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Settings ⚙", fontWeight = FontWeight.Bold, color = Color.White) },
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
                Text(
                    text = "Store Configuration",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Input: Store Name edit field
                OutlinedTextField(
                    value = storeNameInput,
                    onValueChange = {
                        storeNameInput = it
                        viewModel.updateStoreName(it)
                    },
                    label = { Text("Store Name Identifier", color = Color(0xFF94A3B8)) },
                    leadingIcon = { Icon(imageVector = Icons.Default.Store, contentDescription = null, tint = HoneyGold) },
                    trailingIcon = {
                        if (storeNameInput != storeName && storeNameInput.isNotBlank()) {
                            IconButton(onClick = {
                                viewModel.updateStoreName(storeNameInput)
                                Toast.makeText(context, "Identifier updated!", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = "Save", tint = SuccessGreen)
                            }
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

                // Menu: Currency Selection List Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = currencySymbol,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Active Currency Token", color = Color(0xFF94A3B8)) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown",
                                tint = HoneyGold,
                                modifier = Modifier
                                    .clickable { expandCurrencyMenu = true }
                                    .rotate(if (expandCurrencyMenu) 180f else 0f)
                            )
                        },
                        leadingIcon = { Icon(imageVector = Icons.Default.Payment, contentDescription = null, tint = HoneyGold) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandCurrencyMenu = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = HoneyGold,
                            unfocusedBorderColor = Color(0xFF334155)
                        )
                    )

                    DropdownMenu(
                        expanded = expandCurrencyMenu,
                        onDismissRequest = { expandCurrencyMenu = false },
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .background(SlateCard)
                    ) {
                        val currencies = listOf("BDT", "USD", "EUR", "GBP", "PKR")
                        currencies.forEach { curr ->
                            DropdownMenuItem(
                                text = { Text(curr, color = Color.White) },
                                onClick = {
                                    viewModel.updateCurrencySymbol(curr)
                                    expandCurrencyMenu = false
                                    Toast.makeText(context, "Currency set to $curr", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }

                // Button: Team Collaboration
                Card(
                    onClick = onNavigateToTeamSharing,
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(imageVector = Icons.Default.Groups, contentDescription = null, tint = HoneyGold)
                            Column {
                                Text("Team Collaboration & Sharing", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Manage store collaborators, assignments, and permissions", color = Color(0xFF64748B), fontSize = 11.sp)
                            }
                        }
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = HoneyGold, modifier = Modifier.size(12.dp))
                    }
                }

                HorizontalDivider(color = Color(0xFF334155))

                Text(
                    text = "Preferences",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Toggle: Dark Theme switch
                Card(
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(imageVector = Icons.Default.DarkMode, contentDescription = null, tint = HoneyGold)
                            Column {
                                Text("Dark Interface Aesthetic", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Enforces deep eye-relaxing navy backings", color = Color(0xFF64748B), fontSize = 11.sp)
                            }
                        }

                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { viewModel.toggleDarkMode() },
                            colors = SwitchDefaults.colors(checkedIconColor = HoneyGold, checkedTrackColor = HoneyGold.copy(0.4f))
                        )
                    }
                }

                // Toggle: Notifications switch
                Card(
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, tint = HoneyGold)
                            Column {
                                Text("Low Stock Notifications", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Remind when item count is under 2 units", color = Color(0xFF64748B), fontSize = 11.sp)
                            }
                        }

                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { viewModel.toggleNotifications() },
                            colors = SwitchDefaults.colors(checkedIconColor = HoneyGold, checkedTrackColor = HoneyGold.copy(0.4f))
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF334155))

                Text(
                    text = "Backup & Data Utilities",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Button: Export ALL Data
                Card(
                    onClick = {
                        if (products.isEmpty() && returnsList.isEmpty() && lossesList.isEmpty() && ordersList.isEmpty()) {
                            Toast.makeText(context, "No stock data available to package", Toast.LENGTH_SHORT).show()
                        } else {
                            ExcelExporter.exportFullDatabase(context, products, returnsList, lossesList, ordersList)
                        }
                    },
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(imageVector = Icons.Default.IosShare, contentDescription = null, tint = HoneyGold)
                            Column {
                                Text("Export Entire Database to Excel", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Renders fully formatted spreadsheets on phone storage", color = Color(0xFF64748B), fontSize = 11.sp)
                            }
                        }
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = HoneyGold, modifier = Modifier.size(12.dp))
                    }
                }

                // Button: Clear All Data
                Card(
                    onClick = { showResetDialogFirst = true },
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, tint = ErrorRed)
                            Column {
                                Text("Wipe Local Storage Database", color = ErrorRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Permanently erase and reset all store figures", color = Color(0xFF64748B), fontSize = 11.sp)
                            }
                        }
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(12.dp))
                    }
                }

                // About card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SlateCard.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("About THRIFT-HIVE 🐝", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        Text(
                            text = "THRIFT-HIVE is an offline-first inventory spreadsheet manager tailored for specialized vintage boutiques and single-owner secondhand thrift sellers to track profit margins and catalog goods.",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                        Text("App Version: v1.1.2 (Stable)", color = Color(0xFF64748B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Owner Name: NASIF HIMADRI", color = HoneyGold, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }

                // Button: Sign Out
                Card(
                    onClick = {
                        viewModel.logout()
                        onLogOutSuccess()
                        Toast.makeText(context, "Logged out successfully!", Toast.LENGTH_SHORT).show()
                    },
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = ErrorRed)
                            Column {
                                Text("Sign Out of Hive", color = ErrorRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Safely sign out of your active seller session on this device", color = Color(0xFF64748B), fontSize = 11.sp)
                            }
                        }
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(12.dp))
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
            }

            // RESET DIALOG 1 (WARNING)
            if (showResetDialogFirst) {
                AlertDialog(
                    onDismissRequest = { showResetDialogFirst = false },
                    title = { Text("Erase Database Tables?", fontWeight = FontWeight.Bold, color = ErrorRed) },
                    text = {
                        Text(
                            "You are about to clear all items, returns, and losses. This operation cannot be undone. Are you sure you want to proceed?",
                            color = Color(0xFFCBD5E1)
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showResetDialogFirst = false
                                showResetDialogSecond = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                        ) {
                            Text("Confirm Proceed")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showResetDialogFirst = false }) {
                            Text("Cancel", color = Color.White)
                        }
                    },
                    containerColor = SlateCard
                )
            }

            // RESET DIALOG 2 (DOUBLE OVERRIDE VERIFICATION)
            if (showResetDialogSecond) {
                AlertDialog(
                    onDismissRequest = { showResetDialogSecond = false },
                    title = { Text("⚠ CRITICAL RESET CONFIRMATION", fontWeight = FontWeight.ExtraBold, color = ErrorRed) },
                    text = {
                        Text(
                            "Are you absolutely positive? Your thrift records will be fully initialized back to blank state datasets.",
                            color = Color(0xFFCBD5E1)
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.clearAllData()
                                showResetDialogSecond = false
                                Toast.makeText(context, "All data wiped. Refreshing catalog.", Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                        ) {
                            Text("Yes, Wipe Database", fontWeight = FontWeight.Black)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showResetDialogSecond = false }) {
                            Text("Hold On, Cancel", color = Color.White)
                        }
                    },
                    containerColor = SlateCard
                )
            }
        }
    }
}
