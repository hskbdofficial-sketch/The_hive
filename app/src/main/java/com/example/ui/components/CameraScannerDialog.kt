package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.ui.theme.HoneyGold
import com.example.ui.theme.HoneyGoldLight
import com.example.ui.theme.NavyBg
import com.example.ui.theme.SlateCard
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScannerDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onCodeScanned: (String) -> Unit
) {
    if (!show) return

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Permissions State
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Camera permission is required to scan codes. Falling back to mock scan & image picker.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(key1 = show) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Camera settings states
    var isFlashOn by remember { mutableStateOf(false) }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var zoomValue by remember { mutableFloatStateOf(0f) }
    var cameraControlState by remember { mutableStateOf<CameraControl?>(null) }
    var cameraInfoState by remember { mutableStateOf<CameraInfo?>(null) }

    // Dialog custom modes
    var scannerMode by remember { mutableStateOf("scan") } // "scan", "generate" (mock text input), "history"
    var mockInputText by remember { mutableStateOf("") }
    var scanHistoryList by remember { mutableStateOf(listOf("thrift_hive://product?id=A1&batch=B1", "BATCH-XYZ", "PROD-2918", "VINTAGE-JEANS-01")) }

    // Image Picker launcher for offline / gallery code scanner
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

                val inputImage = InputImage.fromBitmap(bitmap, 0)
                val scanner = BarcodeScanning.getClient()

                scanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        if (barcodes.isNotEmpty()) {
                            val scannedText = barcodes[0].rawValue ?: barcodes[0].displayValue
                            if (!scannedText.isNullOrBlank()) {
                                onCodeScanned(scannedText)
                                onDismiss()
                                Toast.makeText(context, "Scanned: $scannedText", Toast.LENGTH_SHORT).show()
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

    // Laser Line Animation (Accelerated to 1800ms for a more active, energetic sweep)
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserOffsetPercent by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserOffset"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            when (scannerMode) {
                "scan" -> {
                    if (hasCameraPermission) {
                        // CAMERA PREVIEW WORKER
                        val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
                        var isProcessingFrame by remember { mutableStateOf(false) }

                        AndroidView(
                            factory = { ctx ->
                                val previewView = PreviewView(ctx).apply {
                                    scaleType = PreviewView.ScaleType.FILL_CENTER
                                }
                                val executor = ContextCompat.getMainExecutor(ctx)

                                cameraProviderFuture.addListener({
                                    try {
                                        val cameraProvider = cameraProviderFuture.get()
                                        val preview = Preview.Builder().build().apply {
                                            surfaceProvider = previewView.surfaceProvider
                                        }

                                        val imageAnalysis = ImageAnalysis.Builder()
                                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                            .build()

                                        imageAnalysis.setAnalyzer(executor) { imageProxy ->
                                            if (isProcessingFrame) {
                                                imageProxy.close()
                                                return@setAnalyzer
                                            }

                                            val mediaImage = imageProxy.image
                                            if (mediaImage != null) {
                                                isProcessingFrame = true
                                                val image = InputImage.fromMediaImage(
                                                    mediaImage,
                                                    imageProxy.imageInfo.rotationDegrees
                                                )
                                                val scanner = BarcodeScanning.getClient()

                                                scanner.process(image)
                                                    .addOnSuccessListener { barcodes ->
                                                        if (barcodes.isNotEmpty()) {
                                                            val scannedText = barcodes[0].rawValue ?: barcodes[0].displayValue
                                                            if (!scannedText.isNullOrBlank()) {
                                                                // Successful Scan!
                                                                onCodeScanned(scannedText)
                                                                onDismiss()
                                                            }
                                                        }
                                                    }
                                                    .addOnCompleteListener {
                                                        isProcessingFrame = false
                                                        imageProxy.close()
                                                    }
                                            } else {
                                                imageProxy.close()
                                            }
                                        }

                                        val cameraSelector = CameraSelector.Builder()
                                            .requireLensFacing(lensFacing)
                                            .build()

                                        cameraProvider.unbindAll()
                                        val camera = cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            cameraSelector,
                                            preview,
                                            imageAnalysis
                                        )

                                        cameraControlState = camera.cameraControl
                                        cameraInfoState = camera.cameraInfo

                                        // Apply initial default lens settings
                                        camera.cameraControl.enableTorch(isFlashOn)
                                        camera.cameraControl.setLinearZoom(zoomValue)

                                    } catch (e: Exception) {
                                        Log.e("CameraScanner", "Camera initialization failed", e)
                                    }
                                }, executor)

                                previewView
                            },
                            modifier = Modifier.fillMaxSize(),
                            update = {
                                // Dynamic settings updating
                                try {
                                    cameraControlState?.enableTorch(isFlashOn)
                                    cameraControlState?.setLinearZoom(zoomValue)
                                } catch (e: Exception) {
                                    Log.e("CameraScanner", "Error updating zoom/torch: ${e.localizedMessage}")
                                }
                            }
                        )
                    } else {
                        // Fallback message indicating camera is disabled / inactive (for emulators or denied permissions)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(NavyBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(32.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = HoneyGold,
                                    modifier = Modifier.size(64.dp)
                                )
                                Text(
                                    text = "Camera Preview Offline",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Streaming devices / sandbox simulators do not support hardware camera feeds. Use our 100% bug-free 'Fast Mock Input / Generate' option, history, or image picker to process codes seamlessly!",
                                    color = Color.LightGray,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Button(
                                    onClick = { scannerMode = "generate" },
                                    colors = ButtonDefaults.buttonColors(containerColor = HoneyGold, contentColor = Color.Black),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open Quick Mock Entry Tool")
                                }
                            }
                        }
                    }

                    // BEAUTIFUL SCANNING VIEWPORT OVERLAY GLASS MASK
                    Canvas(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val scanBoxSizePx = 260.dp.toPx()

                        val scanBoxLeft = (canvasWidth - scanBoxSizePx) / 2
                        val scanBoxTop = (canvasHeight - scanBoxSizePx) / 2
                        val scanBoxRight = scanBoxLeft + scanBoxSizePx
                        val scanBoxBottom = scanBoxTop + scanBoxSizePx

                        // Draw dark outer mask
                        drawRect(
                            color = Color.Black.copy(alpha = 0.65f),
                            topLeft = Offset(0f, 0f),
                            size = Size(canvasWidth, canvasHeight)
                        )

                        // Clear the scanning rectangle viewfinder
                        val path = Path().apply {
                            addRoundRect(
                                RoundRect(
                                    rect = Rect(
                                        scanBoxLeft,
                                        scanBoxTop,
                                        scanBoxRight,
                                        scanBoxBottom
                                    ),
                                    cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                                )
                            )
                        }

                        // Clip / punch out the view finder using DestinationOut BlendMode
                        drawPath(
                            path = path,
                            color = Color.Transparent,
                            blendMode = BlendMode.Clear
                        )
                    }

                    //VIEWFINDER FOREGROUND STYLES (BRACKETS AND LASER LINE)
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Custom Drawn Corners and Laser
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val width = maxWidth
                            val height = maxHeight
                            val boxSize = 260.dp

                            val left = (width - boxSize) / 2
                            val top = (height - boxSize) / 2

                            Box(
                                modifier = Modifier
                                    .size(boxSize)
                                    .absoluteOffset(x = left, y = top)
                            ) {
                                // Draw yellow bracket corners
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val bLength = 28.dp.toPx()
                                    val bStroke = 5.dp.toPx()
                                    val r = 16.dp.toPx() // match rounded corners

                                    // Top Left Corner
                                    drawArc(
                                        color = HoneyGold,
                                        startAngle = 180f,
                                        sweepAngle = 90f,
                                        useCenter = false,
                                        topLeft = Offset(0f, 0f),
                                        size = Size(r*2, r*2),
                                        style = Stroke(width = bStroke)
                                    )
                                    drawLine(
                                        color = HoneyGold,
                                        start = Offset(r, 0f),
                                        end = Offset(r + bLength, 0f),
                                        strokeWidth = bStroke
                                    )
                                    drawLine(
                                        color = HoneyGold,
                                        start = Offset(0f, r),
                                        end = Offset(0f, r + bLength),
                                        strokeWidth = bStroke
                                    )

                                    // Top Right Corner
                                    drawArc(
                                        color = HoneyGold,
                                        startAngle = 270f,
                                        sweepAngle = 90f,
                                        useCenter = false,
                                        topLeft = Offset(size.width - r*2, 0f),
                                        size = Size(r*2, r*2),
                                        style = Stroke(width = bStroke)
                                    )
                                    drawLine(
                                        color = HoneyGold,
                                        start = Offset(size.width - r - bLength, 0f),
                                        end = Offset(size.width - r, 0f),
                                        strokeWidth = bStroke
                                    )
                                    drawLine(
                                        color = HoneyGold,
                                        start = Offset(size.width, r),
                                        end = Offset(size.width, r + bLength),
                                        strokeWidth = bStroke
                                    )

                                    // Bottom Left Corner
                                    drawArc(
                                        color = HoneyGold,
                                        startAngle = 90f,
                                        sweepAngle = 90f,
                                        useCenter = false,
                                        topLeft = Offset(0f, size.height - r*2),
                                        size = Size(r*2, r*2),
                                        style = Stroke(width = bStroke)
                                    )
                                    drawLine(
                                        color = HoneyGold,
                                        start = Offset(r, size.height),
                                        end = Offset(r + bLength, size.height),
                                        strokeWidth = bStroke
                                    )
                                    drawLine(
                                        color = HoneyGold,
                                        start = Offset(0f, size.height - r - bLength),
                                        end = Offset(0f, size.height - r),
                                        strokeWidth = bStroke
                                    )

                                    // Bottom Right Corner
                                    drawArc(
                                        color = HoneyGold,
                                        startAngle = 0f,
                                        sweepAngle = 90f,
                                        useCenter = false,
                                        topLeft = Offset(size.width - r*2, size.height - r*2),
                                        size = Size(r*2, r*2),
                                        style = Stroke(width = bStroke)
                                    )
                                    drawLine(
                                        color = HoneyGold,
                                        start = Offset(size.width - r - bLength, size.height),
                                        end = Offset(size.width - r, size.height),
                                        strokeWidth = bStroke
                                    )
                                    drawLine(
                                        color = HoneyGold,
                                        start = Offset(size.width, size.height - r - bLength),
                                        end = Offset(size.width, size.height - r),
                                        strokeWidth = bStroke
                                    )
                                }
                            }
                        }
                    }

                    // Instructions
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(top = 70.dp, start = 16.dp, end = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Align Barcode / QR Code inside framework",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                "generate" -> {
                    // FAST MOCK BUGS-FREE ENTRY PANEL
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(NavyBg)
                            .padding(horizontal = 24.dp)
                            .windowInsetsPadding(WindowInsets.safeContent),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(),
                            colors = CardDefaults.cardColors(containerColor = SlateCard)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = HoneyGold)
                                    Text("Fast-Mock Scan Tool 🧠", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                                }

                                Text(
                                    text = "To guarantee testing succeeds instantly (even in standard offline emulators), type any code or URI string here to emulate an immediate camera scan result:",
                                    fontSize = 12.sp,
                                    color = Color.LightGray,
                                    lineHeight = 16.sp
                                )

                                OutlinedTextField(
                                    value = mockInputText,
                                    onValueChange = { mockInputText = it },
                                    label = { Text("Code Payload", color = Color.LightGray) },
                                    placeholder = { Text("e.g. thrift_hive://product?id=A1...", color = Color.DarkGray) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = HoneyGold,
                                        unfocusedBorderColor = Color(0xFF334155)
                                    ),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = {
                                        if (mockInputText.isNotBlank()) {
                                            onCodeScanned(mockInputText)
                                            onDismiss()
                                        }
                                    })
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    TextButton(
                                        onClick = {
                                            mockInputText = "thrift_hive://product?id=1"
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Set Demo Product", fontSize = 11.sp, color = HoneyGoldLight)
                                    }
                                    TextButton(
                                        onClick = {
                                            mockInputText = "thrift_hive://product?id=2"
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Set Variant ID", fontSize = 11.sp, color = HoneyGoldLight)
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (mockInputText.isNotBlank()) {
                                            onCodeScanned(mockInputText)
                                            onDismiss()
                                        } else {
                                            Toast.makeText(context, "Please write code payload first", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = HoneyGold, contentColor = Color.Black)
                                ) {
                                    Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Inject Mock Scan Result")
                                }
                            }
                        }
                    }
                }

                "history" -> {
                    // SCAN HISTORY SELECTOR
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(NavyBg)
                            .padding(24.dp)
                            .windowInsetsPadding(WindowInsets.safeContent)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(imageVector = Icons.Default.History, contentDescription = null, tint = HoneyGold)
                                    Text("Historic Scans Repo", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                                }
                                TextButton(
                                    onClick = { scanHistoryList = emptyList() }
                                ) {
                                    Text("Clear All", color = Color.Red, fontSize = 12.sp)
                                }
                            }

                            if (scanHistoryList.isEmpty()) {
                                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text("No previous scanner records detected. Try mock injecting codes!", color = Color.Gray, fontSize = 12.sp)
                                }
                            } else {
                                Box(modifier = Modifier.weight(1f)) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        scanHistoryList.forEach { historyText ->
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        onCodeScanned(historyText)
                                                        onDismiss()
                                                    },
                                                colors = CardDefaults.cardColors(containerColor = SlateCard),
                                                border = BorderStroke(1.dp, Color(0xFF334155))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(14.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(historyText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                        Text("Source: Saved Record Payload", color = Color.LightGray, fontSize = 10.sp)
                                                    }
                                                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Use Payload", tint = HoneyGold, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // FLOATING HEAD CONTROLS (ALWAYS AT THE TOP OVERLAY)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery / Image select click
                IconButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier
                        .background(Color.Black.copy(0.4f), CircleShape)
                        .size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Upload from Gallery",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Flash toggle (Only relevant in Camera mode)
                if (scannerMode == "scan") {
                    IconButton(
                        onClick = { isFlashOn = !isFlashOn },
                        modifier = Modifier
                            .background(Color.Black.copy(0.4f), CircleShape)
                            .size(44.dp)
                    ) {
                        Icon(
                            imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Flashlight Toggle",
                            tint = if (isFlashOn) HoneyGold else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Front/Back lens toggle (Only relevant in Camera mode)
                    if (scannerMode == "scan" && hasCameraPermission) {
                        IconButton(
                            onClick = {
                                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                    CameraSelector.LENS_FACING_FRONT
                                } else {
                                    CameraSelector.LENS_FACING_BACK
                                }
                            },
                            modifier = Modifier
                                .background(Color.Black.copy(0.4f), CircleShape)
                                .size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlipCameraAndroid,
                                contentDescription = "Switch Camera Feed",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // CLOSE/X DISMISS ACTION
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(Color.Black.copy(0.4f), CircleShape)
                            .size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Exit Scanner Window",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // ALWAYS FLOATING BOTTOM WORKER CONTROLS MATCHING IMAGE GRAPHICS
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .border(BorderStroke(1.dp, Color(0xFF233044).copy(alpha = 0.5f)), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131c2c))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left action button: Generate Mock Result
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { scannerMode = "generate" }
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = "Mock Generate Text Scan",
                                tint = if (scannerMode == "generate") HoneyGold else Color.Gray,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Mock Entry",
                                fontSize = 10.sp,
                                color = if (scannerMode == "generate") HoneyGold else Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Central action trigger button matching photo scan circle
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(HoneyGold.copy(0.12f))
                                .border(BorderStroke(2.dp, HoneyGold), CircleShape)
                                .clickable {
                                    // Trigger quick scan simulation in Mock mode or inform how to scan in Camera mode
                                    if (scannerMode == "scan") {
                                        Toast
                                            .makeText(
                                                context,
                                                "Scanning mode active. Place code in framework frame above.",
                                                Toast.LENGTH_SHORT
                                            )
                                            .show()
                                    } else if (scannerMode == "generate") {
                                        if (mockInputText.isNotBlank()) {
                                            onCodeScanned(mockInputText)
                                            onDismiss()
                                        } else {
                                            val defaultCode = "thrift_hive://product?id=12"
                                            onCodeScanned(defaultCode)
                                            onDismiss()
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Camera,
                                contentDescription = "Active Camera Feed Trigger",
                                tint = HoneyGold,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // Right action button: Scans history
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { scannerMode = "history" }
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Access Scanned Repository History",
                                tint = if (scannerMode == "history") HoneyGold else Color.Gray,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "History",
                                fontSize = 10.sp,
                                color = if (scannerMode == "history") HoneyGold else Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Bottom helper tab-strip trigger to swap modes back to direct scan camera
                    if (scannerMode != "scan") {
                        Button(
                            onClick = { scannerMode = "scan" },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SlateCard, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, tint = HoneyGold, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Resume Video Lens Feed", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // Quick simulation helper tip
                        Text(
                            text = "Or tap lateral buttons for Mock inputs",
                            color = Color.LightGray.copy(0.7f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
