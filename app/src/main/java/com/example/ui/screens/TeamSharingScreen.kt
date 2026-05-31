package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import com.example.ui.viewmodel.ThriftViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamSharingScreen(
    viewModel: ThriftViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val teamMembers by viewModel.teamMembers.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()

    var showInviteDialog by remember { mutableStateOf(false) }

    // Dialog state variables
    var inviteName by remember { mutableStateOf("") }
    var inviteEmail by remember { mutableStateOf("") }
    var inviteRole by remember { mutableStateOf("Editor") }
    var expandRoleDropdown by remember { mutableStateOf(false) }

    // Dialog granular permission variables
    var canEditInventory by remember { mutableStateOf(true) }
    var canEditReturns by remember { mutableStateOf(true) }
    var canEditLosses by remember { mutableStateOf(true) }
    var canEditOrders by remember { mutableStateOf(true) }
    var canViewReports by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Team & Sharing 👥", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyBg)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    inviteName = ""
                    inviteEmail = ""
                    inviteRole = "Editor"
                    showInviteDialog = true
                },
                containerColor = HoneyGold,
                contentColor = Color.Black
            ) {
                Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Add Teammate")
            }
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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Team Overview header card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SlateCard)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Store Collaboration Network",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                "Easily invite collaborators to view or edit the Thrift Store. Share backup inventory sheets directly with active teammates to sync off-grid configurations.",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Share Copy of database trigger utilizing System Share Sheet (Intent.ACTION_SEND)
                            Button(
                                onClick = {
                                    if (products.isEmpty()) {
                                        Toast.makeText(context, "No stock items in catalog to compile.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    val summaryBuilder = StringBuilder()
                                    summaryBuilder.append("🐝 Thrift Hive Store Share Copy! 🐝\n")
                                    summaryBuilder.append("Store: ${viewModel.storeName.value}\n")
                                    summaryBuilder.append("Total Catalog Items: ${products.size}\n\n")
                                    summaryBuilder.append("--- INVENTORY --- \n")
                                    products.take(20).forEach { item ->
                                        summaryBuilder.append("ID: ${item.productId} — ${item.productName} — Qty: ${item.quantity} — Size: ${item.size} — Margin Profit: ${viewModel.currencySymbol.value} ${item.profitPerUnit.toInt()}\n")
                                    }
                                    if (products.size > 20) {
                                        summaryBuilder.append("... and ${products.size - 20} more items.\n")
                                    }

                                    // Trigger standard system Intent for real integration
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, summaryBuilder.toString())
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Share Inventory With Teammates")
                                    context.startActivity(shareIntent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = HoneyGold, contentColor = Color.Black),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("Broadcast Stock copy to Team", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        "Registered Members (${teamMembers.size})",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Listing registered team members
                items(teamMembers) { member ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SlateCard)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Fancy Letter Circle Avatar with varying backgrounds
                                val avatarBg = when(member.role) {
                                    "Administrator" -> HoneyGold
                                    "Editor" -> SuccessGreen
                                    else -> Color(0xFF3B82F6)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(avatarBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = member.name.take(1).uppercase(),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp,
                                        color = Color.Black
                                    )
                                }

                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = member.name,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )

                                        // Role Mini Badge
                                        val roleBadgeBg = avatarBg.copy(0.12f)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(roleBadgeBg)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = member.role,
                                                color = avatarBg,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }

                                    Text(
                                        text = member.email,
                                        color = Color(0xFF64748B),
                                        fontSize = 11.sp
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        if (member.canEditInventory) {
                                            BadgeWithLabel("Stock 📦", SuccessGreen)
                                        }
                                        if (member.canEditReturns) {
                                            BadgeWithLabel("Returns ↩️", InfoBlue)
                                        }
                                        if (member.canEditLosses) {
                                            BadgeWithLabel("Losses 📉", ErrorRed)
                                        }
                                        if (member.canEditOrders) {
                                            BadgeWithLabel("Orders 🏷️", HoneyGold)
                                        }
                                        if (member.canViewReports) {
                                            BadgeWithLabel("Reports 📊", Color.LightGray)
                                        }
                                    }
                                }
                            }

                            // Disallow removing default Nasif Himadri/admin settings for safety
                            if (!member.email.contains("nasifhimadri")) {
                                IconButton(
                                    onClick = {
                                        viewModel.removeTeamMember(member.email)
                                        Toast.makeText(context, "${member.name} removed from your scope", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Remove member", tint = ErrorRed)
                                }
                            }
                        }
                    }
                }
            }

            // Dialogue: Add Teammate Input interface overlays
            if (showInviteDialog) {
                AlertDialog(
                    onDismissRequest = { showInviteDialog = false },
                    title = { Text("Invite New Teammate", fontWeight = FontWeight.Bold, color = Color.White) },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            OutlinedTextField(
                                value = inviteName,
                                onValueChange = { inviteName = it },
                                label = { Text("Collaborator Name", color = Color(0xFF94A3B8)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = HoneyGold,
                                    unfocusedBorderColor = Color(0xFF334155)
                                )
                            )

                            OutlinedTextField(
                                value = inviteEmail,
                                onValueChange = { inviteEmail = it },
                                label = { Text("Collaborator Email", color = Color(0xFF94A3B8)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = HoneyGold,
                                    unfocusedBorderColor = Color(0xFF334155)
                                )
                            )

                            // Dropdown choosing active roles
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = inviteRole,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Assign Privileges", color = Color(0xFF94A3B8)) },
                                    trailingIcon = {
                                        IconButton(onClick = { expandRoleDropdown = !expandRoleDropdown }) {
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = HoneyGold)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        unfocusedBorderColor = Color(0xFF334155)
                                    )
                                )

                                DropdownMenu(
                                    expanded = expandRoleDropdown,
                                    onDismissRequest = { expandRoleDropdown = false },
                                    modifier = Modifier
                                        .fillMaxWidth(0.9f)
                                        .background(SlateCard)
                                ) {
                                    val roles = listOf("Administrator", "Editor", "Viewer")
                                    roles.forEach { r ->
                                        DropdownMenuItem(
                                            text = { Text(r, color = Color.White) },
                                            onClick = {
                                                inviteRole = r
                                                expandRoleDropdown = false
                                                // Automatically adjust checkboxes based on selected profile defaults
                                                when (r) {
                                                    "Administrator" -> {
                                                        canEditInventory = true
                                                        canEditReturns = true
                                                        canEditLosses = true
                                                        canEditOrders = true
                                                        canViewReports = true
                                                    }
                                                    "Editor" -> {
                                                        canEditInventory = true
                                                        canEditReturns = true
                                                        canEditLosses = true
                                                        canEditOrders = true
                                                        canViewReports = false
                                                    }
                                                    "Viewer" -> {
                                                        canEditInventory = false
                                                        canEditReturns = false
                                                        canEditLosses = false
                                                        canEditOrders = false
                                                        canViewReports = true
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            // Horizontal divider prior to specific privilege switches
                            Divider(color = Color(0xFF334155), thickness = 1.dp)

                            Text("Granular Module Authorization:", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                PermissionCheckboxItem("Access Inventory Edit Controls", canEditInventory) { canEditInventory = it }
                                PermissionCheckboxItem("Access Returns Registry", canEditReturns) { canEditReturns = it }
                                PermissionCheckboxItem("Access Profit/Loss Controls", canEditLosses) { canEditLosses = it }
                                PermissionCheckboxItem("Access Store Order Placement", canEditOrders) { canEditOrders = it }
                                PermissionCheckboxItem("Access Metrics & Report Widgets", canViewReports) { canViewReports = it }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (inviteName.isBlank() || inviteEmail.isBlank()) {
                                    Toast.makeText(context, "Full name coordinates are required", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val emailToShare = inviteEmail
                                viewModel.addTeamMember(
                                    inviteName,
                                    inviteEmail,
                                    inviteRole,
                                    canEditInventory,
                                    canEditReturns,
                                    canEditLosses,
                                    canEditOrders,
                                    canViewReports
                                )
                                showInviteDialog = false
                                Toast.makeText(context, "$inviteName successfully registered! Launching APK email dispatch...", Toast.LENGTH_SHORT).show()
                                shareApkToEmail(context, emailToShare)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HoneyGold, contentColor = Color.Black)
                        ) {
                            Text("Register Teammate", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showInviteDialog = false }) {
                            Text("Cancel", color = Color.White)
                        }
                    },
                    containerColor = SlateCard
                )
            }
        }
    }
}

@Composable
fun BadgeWithLabel(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(0.12f))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(text = label, color = color, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun PermissionCheckboxItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = HoneyGold,
                uncheckedColor = Color.Gray,
                checkmarkColor = Color.Black
            ),
            modifier = Modifier.size(24.dp)
        )
        Text(text = label, color = Color.White, fontSize = 12.sp)
    }
}

private fun shareApkToEmail(context: android.content.Context, emailAddress: String) {
    try {
        val srcFile = File(context.packageCodePath)
        val destFile = File(context.cacheDir, "thrift_hive.apk")
        
        srcFile.inputStream().use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        
        val apkUri = FileProvider.getUriForFile(
            context,
            "com.thrifthive.nasif.fileprovider",
            destFile
        )
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.android.package-archive"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(emailAddress))
            putExtra(Intent.EXTRA_SUBJECT, "🐝 Welcome to Thrift-Hive App Team [APK Share]")
            putExtra(Intent.EXTRA_TEXT, "Hello!\n\nYou have been added as an official team member of Thrift-Hive.\nAttached is the latest active APK file to install this application directly on your device.\n\nWarm regards,\nNASIF HIMADRI @ Thrift-Hive Team")
            putExtra(Intent.EXTRA_STREAM, apkUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(intent, "Email APK to Teammate"))
    } catch (e: Exception) {
        Toast.makeText(context, "Could not copy or share APK: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
