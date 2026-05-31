package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.ThriftViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: ThriftViewModel,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    var isLoginTab by remember { mutableStateOf(true) }

    // Security & OTP state variables
    var isOtpVerificationMode by remember { mutableStateOf(false) }
    var systemGeneratedOtp by remember { mutableStateOf("") }
    var userEnteredOtp by remember { mutableStateOf("") }
    
    // Auth Cache variables
    var cachedEmail by remember { mutableStateOf("") }
    var cachedPassword by remember { mutableStateOf("") }
    var cachedName by remember { mutableStateOf("") }
    var isCachedSignup by remember { mutableStateOf(false) }

    // Inputs States
    var nameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBg)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Elegant Hive Branding Logo
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(SlateCard, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Hive,
                    contentDescription = "Thrift Hive Logo",
                    tint = HoneyGold,
                    modifier = Modifier.size(54.dp)
                )
            }

            Text(
                text = "THRIFT-HIVE",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp
            )

            Text(
                text = "Dedicated secondhand and boutique store manager owned by NASIF HIMADRI",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (!isOtpVerificationMode) {
                // Premium styled Toggle Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .background(SlateCard, RoundedCornerShape(10.dp))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isLoginTab) HoneyGold else Color.Transparent)
                            .clickable { isLoginTab = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Sign In",
                            color = if (isLoginTab) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (!isLoginTab) HoneyGold else Color.Transparent)
                            .clickable { isLoginTab = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Create Account",
                            color = if (!isLoginTab) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                // Input Fields Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Title for active state
                        Text(
                            text = if (isLoginTab) "Welcome Back" else "Register a New Hive Account",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        // Optional Full Name field
                        AnimatedVisibility(
                            visible = !isLoginTab,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = { nameInput = it },
                                label = { Text("Your Preferred Name", color = Color(0xFF94A3B8)) },
                                placeholder = { Text("e.g. John Doe", color = Color(0xFF475569)) },
                                leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = HoneyGold) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = HoneyGold,
                                    unfocusedBorderColor = Color(0xFF334155)
                                )
                            )
                        }

                        // Email field
                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text("Email Address", color = Color(0xFF94A3B8)) },
                            placeholder = { Text("e.g. user@domain.com", color = Color(0xFF475569)) },
                            leadingIcon = { Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = HoneyGold) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = HoneyGold,
                                unfocusedBorderColor = Color(0xFF334155)
                            )
                        )

                        // Password field
                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("Password", color = Color(0xFF94A3B8)) },
                            leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = HoneyGold) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle password visibility",
                                        tint = Color(0xFF64748B)
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = HoneyGold,
                                unfocusedBorderColor = Color(0xFF334155)
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Trigger Credentials confirmation and proceed to OTP phase.
                        Button(
                            onClick = {
                                if (emailInput.isBlank() || passwordInput.isBlank()) {
                                    Toast.makeText(context, "All credentials fields are strictly required", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (!isLoginTab && nameInput.isBlank()) {
                                    Toast.makeText(context, "Please enter your preferred name", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                // Secure dispatch generator
                                cachedEmail = emailInput
                                cachedPassword = passwordInput
                                cachedName = nameInput
                                isCachedSignup = !isLoginTab

                                val otp = (100000..999999).random().toString()
                                systemGeneratedOtp = otp
                                userEnteredOtp = ""
                                isOtpVerificationMode = true

                                Toast.makeText(context, "🔒 OTP verification dispatched successfully!", Toast.LENGTH_LONG).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HoneyGold, contentColor = Color.Black)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text(
                                    text = if (isLoginTab) "Authenticate Account" else "Initialize Registration",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            } else {
                // High-Fidelity Interactive Security OTP Verification Interface Mode
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.LockClock, contentDescription = null, tint = HoneyGold, modifier = Modifier.size(24.dp))
                            Text(
                                text = "OTP Authenticator Terminal",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Text(
                            text = "A dynamic 6-digit secure verification token has been routed to device linked to $cachedEmail. Please type the digits to verify your request.",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        // Beautiful HUD display showing the generated OTP code
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(0.4f), RoundedCornerShape(8.dp))
                                .border(1.dp, HoneyGold.copy(0.3f), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "SECURE SIMULATED INBOUND TRANSMISSION",
                                    color = HoneyGold,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Your verification PIN is: $systemGeneratedOtp",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        OutlinedTextField(
                            value = userEnteredOtp,
                            onValueChange = { if (it.length <= 6) userEnteredOtp = it },
                            label = { Text("6-Digit OTP Token", color = Color(0xFF94A3B8)) },
                            placeholder = { Text("000000", color = Color(0xFF475569)) },
                            leadingIcon = { Icon(imageVector = Icons.Default.VpnKey, contentDescription = null, tint = HoneyGold) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = HoneyGold,
                                unfocusedBorderColor = Color(0xFF334155),
                                cursorColor = HoneyGold
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Back Button
                            Button(
                                onClick = { isOtpVerificationMode = false },
                                modifier = Modifier
                                    .weight(0.4f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155), contentColor = Color.White)
                            ) {
                                Text("Edit Info", fontSize = 13.sp)
                            }

                            // Verify Button
                            Button(
                                onClick = {
                                    if (userEnteredOtp.trim() != systemGeneratedOtp) {
                                        Toast.makeText(context, "❌ Invalid OTP token. Please try again.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }

                                    // Complete credential synchronization
                                    if (isCachedSignup) {
                                        viewModel.signup(cachedName, cachedEmail, cachedPassword) { success, msg ->
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            if (success) {
                                                onAuthSuccess()
                                            } else {
                                                isOtpVerificationMode = false
                                            }
                                        }
                                    } else {
                                        viewModel.login(cachedEmail, cachedPassword) { success, msg ->
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            if (success) {
                                                onAuthSuccess()
                                            } else {
                                                isOtpVerificationMode = false
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(0.6f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.Black)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("Verify & Match", fontWeight = FontWeight.Black, fontSize = 13.sp)
                                }
                            }
                        }

                        // Resend link
                        Text(
                            text = "Didn't receive passcode? Resend Security Token",
                            color = HoneyGoldLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .clickable {
                                    val newOtp = (100000..999999).random().toString()
                                    systemGeneratedOtp = newOtp
                                    userEnteredOtp = ""
                                    Toast.makeText(context, "New security authentication token dispatch simulated!", Toast.LENGTH_SHORT).show()
                                }
                        )
                    }
                }
            }
        }
    }
}
