package com.example.ui.components

import android.Manifest
import android.widget.Toast
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

enum class ScanMode {
    PRIMARY_QR,      // QR Label Sistem Lunaris (Utama)
    FALLBACK_BARCODE // Barcode Pabrik / Serial Number (Cadangan)
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScannerDialog(
    title: String = "Pindai QR / Barcode Barang",
    initialMode: ScanMode = ScanMode.PRIMARY_QR,
    onDismissRequest: () -> Unit,
    onBarcodeScanned: (String) -> Unit = {},
    onCodeScannedWithMode: ((String, ScanMode) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    
    var isFlashOn by remember { mutableStateOf(false) }
    var cameraControlState by remember { mutableStateOf<CameraControl?>(null) }
    var hasScanned by remember { mutableStateOf(false) }
    var scanMode by remember { mutableStateOf(initialMode) }
    var showScanSuccessFeedback by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header Bar
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
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = Color(0xFF818CF8),
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.White.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Mode Selector Tabs (Prioritas Utama vs Cadangan/Fallback)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Tab 1: QR Label Sistem Lunaris (Utama)
                    val isPrimary = scanMode == ScanMode.PRIMARY_QR
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isPrimary) Color(0xFF0F766E) else Color.Transparent
                            )
                            .clickable { scanMode = ScanMode.PRIMARY_QR }
                            .padding(vertical = 8.dp, horizontal = 6.dp)
                            .testTag("tab_primary_qr"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = if (isPrimary) Color.White else Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "QR Lunaris",
                                fontSize = 12.sp,
                                fontWeight = if (isPrimary) FontWeight.Bold else FontWeight.Medium,
                                color = if (isPrimary) Color.White else Color(0xFF94A3B8)
                            )
                            Spacer(Modifier.width(2.dp))
                            Surface(
                                color = if (isPrimary) Color(0xFF22C55E) else Color(0xFF334155),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Utama",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    // Tab 2: Barcode Pabrik / SN (Cadangan)
                    val isFallback = scanMode == ScanMode.FALLBACK_BARCODE
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isFallback) Color(0xFFB45309) else Color.Transparent
                            )
                            .clickable { scanMode = ScanMode.FALLBACK_BARCODE }
                            .padding(vertical = 8.dp, horizontal = 6.dp)
                            .testTag("tab_fallback_barcode"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewWeek,
                                contentDescription = null,
                                tint = if (isFallback) Color.White else Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Barcode / SN",
                                fontSize = 12.sp,
                                fontWeight = if (isFallback) FontWeight.Bold else FontWeight.Medium,
                                color = if (isFallback) Color.White else Color(0xFF94A3B8)
                            )
                            Spacer(Modifier.width(2.dp))
                            Surface(
                                color = if (isFallback) Color(0xFFF59E0B) else Color(0xFF334155),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Cadangan",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                // Camera Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
                ) {
                    if (cameraPermissionState.status.isGranted) {
                        ScannerCameraPreview(
                            onCodeScanned = { rawCode ->
                                if (!hasScanned && rawCode.isNotBlank()) {
                                    hasScanned = true
                                    showScanSuccessFeedback = true
                                    val modeName = if (scanMode == ScanMode.PRIMARY_QR) "QR Lunaris" else "Barcode Pabrik"
                                    Toast.makeText(context, "[$modeName] Terdeteksi: $rawCode", Toast.LENGTH_SHORT).show()
                                    onCodeScannedWithMode?.invoke(rawCode, scanMode)
                                    onBarcodeScanned(rawCode)
                                    onDismissRequest()
                                }
                            },
                            isFlashOn = isFlashOn,
                            onCameraControlReady = { cameraControlState = it }
                        )

                        // Framing box overlay depends on scanMode
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.35f))
                        ) {
                            // Mode Banner inside camera view
                            Surface(
                                color = if (scanMode == ScanMode.PRIMARY_QR) Color(0xCC0F766E) else Color(0xCCB45309),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 10.dp)
                            ) {
                                Text(
                                    text = if (scanMode == ScanMode.PRIMARY_QR)
                                        "🎯 Mode Utama: QR Label Sistem Lunaris"
                                    else
                                        "⚡ Mode Cadangan: Barcode Pabrik / Serial Number",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            // Viewfinder framing box
                            val frameWidth = if (scanMode == ScanMode.PRIMARY_QR) 180.dp else 230.dp
                            val frameHeight = if (scanMode == ScanMode.PRIMARY_QR) 180.dp else 110.dp
                            val frameBorderColor = if (scanMode == ScanMode.PRIMARY_QR) Color(0xFF22C55E) else Color(0xFFF59E0B)

                            Box(
                                modifier = Modifier
                                    .size(width = frameWidth, height = frameHeight)
                                    .align(Alignment.Center)
                                    .border(2.5.dp, frameBorderColor, RoundedCornerShape(16.dp))
                            ) {
                                val infiniteTransition = rememberInfiniteTransition(label = "laser_anim")
                                val targetLaserVal = (frameHeight.value - 10f).coerceAtLeast(10f)
                                val laserY by infiniteTransition.animateFloat(
                                    initialValue = 10f,
                                    targetValue = targetLaserVal,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1600, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "laser_y"
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(2.dp)
                                        .offset(y = laserY.dp)
                                        .background(if (scanMode == ScanMode.PRIMARY_QR) Color(0xFF22C55E) else Color(0xFFEF4444))
                                )
                            }

                            // Flashlight Toggle Button inside scanner
                            IconButton(
                                onClick = {
                                    isFlashOn = !isFlashOn
                                    cameraControlState?.enableTorch(isFlashOn)
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(10.dp)
                                    .size(36.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                    .testTag("flashlight_toggle")
                            ) {
                                Icon(
                                    imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                    contentDescription = "Senter",
                                    tint = if (isFlashOn) Color(0xFFFBBF24) else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Successful Scan Feedback Overlay
                            if (showScanSuccessFeedback) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xAA10B981)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(56.dp)
                                        )
                                        Text(
                                            text = "Pemindaian Berhasil!",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 15.sp
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Permission Request UI
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Akses Kamera Diperlukan",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Izinkan aplikasi mengakses kamera untuk memindai QR Label Lunaris / Barcode Pabrik.",
                                color = Color.LightGray,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { cameraPermissionState.launchPermissionRequest() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Buka Kamera", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Helper Text below scanner view
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFEDE9FE), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF7C3AED),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Gunakan Scan QR Label Lunaris (Utama) atau beralih ke Barcode Pabrik jika label rusak.",
                        fontSize = 10.5.sp,
                        color = Color(0xFF4C1D95),
                        lineHeight = 14.sp
                    )
                }

                // Footer Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            scanMode = if (scanMode == ScanMode.PRIMARY_QR) ScanMode.FALLBACK_BARCODE else ScanMode.PRIMARY_QR
                        }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF818CF8))
                            Text(
                                text = if (scanMode == ScanMode.PRIMARY_QR) "Beralih ke Barcode Pabrik" else "Beralih ke QR Lunaris",
                                color = Color(0xFF818CF8),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    TextButton(onClick = onDismissRequest) {
                        Text("Batal / Isi Manual", color = Color.LightGray, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScannerCameraPreview(
    onCodeScanned: (String) -> Unit,
    isFlashOn: Boolean,
    onCameraControlReady: (CameraControl) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val barcodeOptions = remember {
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_ALL_FORMATS,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_CODE_93,
                Barcode.FORMAT_CODABAR,
                Barcode.FORMAT_DATA_MATRIX,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_ITF,
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_PDF417,
                Barcode.FORMAT_AZTEC
            )
            .build()
    }
    val scanner = remember { BarcodeScanning.getClient(barcodeOptions) }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                if (cameraProviderFuture.isDone) {
                    cameraProviderFuture.get().unbindAll()
                }
            } catch (exc: Exception) {
                exc.printStackTrace()
            }
            try {
                scanner.close()
            } catch (exc: Exception) {
                exc.printStackTrace()
            }
            cameraExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            cameraProviderFuture.addListener({
                try {
                    if (androidx.core.content.ContextCompat.checkSelfPermission(
                            ctx,
                            Manifest.permission.CAMERA
                        ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                    ) {
                        return@addListener
                    }

                    val cameraProvider = cameraProviderFuture.get()

                    val preview = androidx.camera.core.Preview.Builder().build().apply {
                        surfaceProvider = previewView.surfaceProvider
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                            scanner.process(inputImage)
                                .addOnSuccessListener { barcodes ->
                                    for (barcode in barcodes) {
                                        val rawValue = barcode.rawValue
                                        if (!rawValue.isNullOrBlank()) {
                                            onCodeScanned(rawValue)
                                            break
                                        }
                                    }
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }

                    val cameraSelector = if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    } else {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    }

                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                    onCameraControlReady(camera.cameraControl)
                    camera.cameraControl.enableTorch(isFlashOn)
                } catch (exc: Exception) {
                    exc.printStackTrace()
                }
            }, androidx.core.content.ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}
