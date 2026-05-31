package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hive
import androidx.compose.material.icons.filled.HomeRepairService
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.HoneyGold
import com.example.ui.theme.HoneyGoldLight
import com.example.ui.theme.NavyBg
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SplashScreen(onNavigateToDashboard: () -> Unit) {
    var startAnim by remember { mutableStateOf(false) }

    val scaleValue by animateFloatAsState(
        targetValue = if (startAnim) 1.1f else 0.4f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logo_scale"
    )

    val alphaValue by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(durationMillis = 1200, easing = LinearOutSlowInEasing),
        label = "logo_alpha"
    )

    // Gold Shimmer Effect for title using Infinite Transition
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -300f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )

    LaunchedEffect(key1 = true) {
        startAnim = true
        delay(2500)
        onNavigateToDashboard()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Honeycomb / Hexagonal Ring of Gold Dots or Icons enclosing Clothes Vector
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .scale(scaleValue)
                    .alpha(alphaValue),
                contentAlignment = Alignment.Center
            ) {
                // Outer rotating Honeycomb frame
                Icon(
                    imageVector = Icons.Default.Hive,
                    contentDescription = "Honeycomb Hive",
                    tint = HoneyGold,
                    modifier = Modifier.fillMaxSize()
                )

                // Enclosed Clothes Hanger analog (using ShoppingBag in gold)
                Icon(
                    imageVector = Icons.Default.ShoppingBag,
                    contentDescription = "Clothes Hanger analogue",
                    tint = Color.Black,
                    modifier = Modifier.size(54.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Shimmering Text "THRIFT-HIVE"
            val textBrush = Brush.linearGradient(
                colors = listOf(
                    HoneyGold,
                    HoneyGoldLight,
                    Color(0xFFFF85A2), // Soft bright neon pink highlighting line
                    HoneyGold
                ),
                start = androidx.compose.ui.geometry.Offset(shimmerOffset, 0f),
                end = androidx.compose.ui.geometry.Offset(shimmerOffset + 150f, 150f)
            )

            Text(
                text = "THRIFT-HIVE",
                style = TextStyle(
                    brush = textBrush,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    letterSpacing = 2.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Pulse animation for underlay tagline
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0.5f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse_alpha"
            )

            Text(
                text = "Smart Inventory for Smart Thrifters",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF94A3B8), // slate-400
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(pulseAlpha)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "OWNER: NASIF HIMADRI",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = HoneyGold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.alpha(0.85f)
            )
        }
    }
}
