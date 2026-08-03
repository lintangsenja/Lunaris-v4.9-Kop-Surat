@file:kotlin.OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    com.google.accompanist.permissions.ExperimentalPermissionsApi::class
)

package com.example.ui.screens

import com.example.ui.components.LunarisCard
import com.example.ui.components.LunarisTextField
import com.example.ui.components.CameraScannerDialog
import com.example.ui.components.ScanMode

import android.Manifest
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import com.example.ui.theme.DeepPurpleText
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.ItemWithStock
import com.example.ui.viewmodel.InventoryViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanQrScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPeminjaman: (String) -> Unit,
    onNavigateToPengembalian: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userRole by viewModel.userRole.collectAsState()
    val studentPermissions by viewModel.studentPermissions.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Pindai QR", "Buat QR", "Input Manual")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .shadow(
                            elevation = 2.dp,
                            shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                        )
                        .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFF7E0FF),
                                    Color(0xFFBAE7FF)
                                )
                            )
                        )
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onNavigateBack,
                                modifier = Modifier.size(40.dp).testTag("back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Kembali",
                                    tint = Color(0xFF0F172A),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = "Administrasi QR Code",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Pemindaian scanner, pembuatan label QR, dan input manual",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFF1E293B)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Sub-Header Tab Navigation
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 1.dp)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(vertical = 4.dp)
                ) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        tabs.forEachIndexed { index, title ->
                            val tabIcon = when (index) {
                                0 -> Icons.Default.QrCodeScanner
                                1 -> Icons.Default.QrCode
                                else -> Icons.Default.Keyboard
                            }
                            Tab(
                                selected = selectedTab == index,
                                onClick = {
                                    if (index == 1 && userRole == "siswa" && studentPermissions["generate_qr"] == false) {
                                        Toast.makeText(context, "Akses 'Buat QR' dibatasi oleh Admin untuk Siswa", Toast.LENGTH_SHORT).show()
                                    } else if (index == 0 && userRole == "siswa" && studentPermissions["scan_qr"] == false) {
                                        Toast.makeText(context, "Akses 'Pindai QR' dibatasi oleh Admin untuk Siswa", Toast.LENGTH_SHORT).show()
                                    } else {
                                        selectedTab = index
                                    }
                                },
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = tabIcon,
                                            contentDescription = null,
                                            tint = if (selectedTab == index) MaterialTheme.colorScheme.primary else Color.Gray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            when (selectedTab) {
                0 -> ScannerTab(viewModel, onNavigateToPeminjaman, onNavigateToPengembalian)
                1 -> GeneratorTab(viewModel)
                2 -> InputManualTab(viewModel)
            }
        }
    }
}

data class ScanHistoryEntry(
    val item: ItemWithStock,
    val timestamp: String
)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScannerTab(
    viewModel: InventoryViewModel,
    onNavigateToPeminjaman: (String) -> Unit,
    onNavigateToPengembalian: (String) -> Unit
) {
    val context = LocalContext.current
    val items by viewModel.itemsWithStock.collectAsState()
    val userRole by viewModel.userRole.collectAsState()

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    var scannedItem by remember { mutableStateOf<ItemWithStock?>(null) }
    var showResultDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDamagedDialog by remember { mutableStateOf(false) }
    var showMaintenanceDialog by remember { mutableStateOf(false) }
    var showUnknownQrDialog by remember { mutableStateOf(false) }
    var unknownQrText by remember { mutableStateOf("") }

    var isFlashOn by remember { mutableStateOf(false) }
    var cameraControlState by remember { mutableStateOf<CameraControl?>(null) }

    // Scan history log
    var scanHistory by remember { mutableStateOf<List<ScanHistoryEntry>>(emptyList()) }

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    // Callback when QR code is scanned
    val handleQrScanned = { qrText: String ->
        val clean = qrText.trim()
        val matchedItem = items.find { item ->
            item.idBarang.equals(clean, ignoreCase = true) ||
            clean.contains(item.idBarang, ignoreCase = true) ||
            (item.serialNumber.isNotBlank() && (item.serialNumber.equals(clean, ignoreCase = true) || clean.contains(item.serialNumber, ignoreCase = true)))
        }
        if (matchedItem != null) {
            if (userRole != "admin" && !matchedItem.isBorrowable) {
                Toast.makeText(context, "Barang '${matchedItem.namaBarang}' tidak diperbolehkan untuk dipinjam oleh Siswa!", Toast.LENGTH_LONG).show()
            } else {
                scannedItem = matchedItem
                showResultDialog = true

                // Add to scan history (keep last 10, most recent first)
                val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                scanHistory = (listOf(ScanHistoryEntry(matchedItem, timeStr)) + scanHistory.filter { it.item.idBarang != matchedItem.idBarang }).take(10)
            }
        } else {
            unknownQrText = clean
            showUnknownQrDialog = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Camera Viewfinder Card (Proportional ratio, sleek frame, compact flashlight button)
        LunarisCard(
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(24.dp))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (cameraPermissionState.status.isGranted) {
                    CameraPreviewView(
                        onQrScanned = { handleQrScanned(it) },
                        isFlashOn = isFlashOn,
                        onCameraControlReady = { cameraControlState = it }
                    )

                    // Laser Overlay & Framing Box
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f))
                    ) {
                        // Scanner box frame
                        Box(
                            modifier = Modifier
                                .size(190.dp)
                                .align(Alignment.Center)
                                .border(2.5.dp, Color(0xFF6366F1), RoundedCornerShape(20.dp))
                        ) {
                            val infiniteTransition = rememberInfiniteTransition(label = "laser")
                            val laserY by infiniteTransition.animateFloat(
                                initialValue = 10f,
                                targetValue = 180f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(2000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "laser_y"
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .offset(y = laserY.dp)
                                    .background(Color(0xFFEF4444))
                            )
                        }

                        // Top Header inside Camera: Live status badge + Compact Flashlight Button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color(0xFF10B981), CircleShape)
                                    )
                                    Text(
                                        text = "Arahkan Kamera ke Kode QR",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Compact, non-jumbo Flashlight Toggle Button
                            IconButton(
                                onClick = {
                                    isFlashOn = !isFlashOn
                                    cameraControlState?.enableTorch(isFlashOn)
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Color.Black.copy(alpha = 0.65f), CircleShape)
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
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0F172A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "Kamera",
                                tint = Color.Gray,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Akses Kamera Diperlukan",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Diperlukan untuk memindai kode QR barang.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = Color.LightGray
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { cameraPermissionState.launchPermissionRequest() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                            ) {
                                Text("Aktifkan Kamera", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // 2. Section: Riwayat Scan Terakhir
        LunarisCard(
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFF4F46E5).copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Riwayat",
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Riwayat Scan Terakhir",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Daftar pemindaian barang pada sesi ini",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    if (scanHistory.isNotEmpty()) {
                        TextButton(onClick = { scanHistory = emptyList() }) {
                            Text("Bersihkan", fontSize = 11.sp, color = Color(0xFFEF4444))
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                if (scanHistory.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Kosong",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Belum ada riwayat pemindaian pada sesi ini.",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                } else {
                    scanHistory.forEach { entry ->
                        val labKomCats = setOf(
                            "PC Desktop", "Workstation Design", "Server Lab / NOC",
                            "All-in-One PC", "Laptop LabKom", "Hardware Komputer",
                            "Workstation", "Server", "All-in-One", "LabKom"
                        )
                        val isLab = entry.item.type.equals("LABKOM", ignoreCase = true) ||
                                entry.item.idBarang.startsWith("PC-LAB", ignoreCase = true) ||
                                entry.item.idBarang.startsWith("LAB-", ignoreCase = true) ||
                                labKomCats.contains(entry.item.kategori) ||
                                entry.item.namaBarang.contains("PC Lab", ignoreCase = true) ||
                                entry.item.namaBarang.contains("Workstation", ignoreCase = true) ||
                                entry.item.namaBarang.contains("Server", ignoreCase = true)

                        val isBhn = !isLab && (
                            entry.item.type.equals("BAHAN", ignoreCase = true) ||
                            entry.item.idBarang.startsWith("BHN-", ignoreCase = true) ||
                            entry.item.kategori.equals("Logistik", ignoreCase = true) ||
                            entry.item.kategori.contains("Bahan", ignoreCase = true)
                        )

                        val isPrp = !isLab && !isBhn && (
                            entry.item.type.equals("PERIPHERAL", ignoreCase = true) ||
                            entry.item.idBarang.startsWith("PRP-", ignoreCase = true) ||
                            entry.item.kategori.contains("Peripheral", ignoreCase = true)
                        )

                        val (badgeLabel, badgeColor, iconVector) = when {
                            isLab -> Triple("QR LabKom", Color(0xFF7C3AED), Icons.Default.Computer)
                            isPrp -> Triple("QR Peripheral", Color(0xFFD97706), Icons.Default.Extension)
                            isBhn -> Triple("QR Bahan", Color(0xFF059669), Icons.Default.Inventory2)
                            else -> Triple("QR Alat", Color(0xFF2563EB), Icons.Default.Build)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                .clickable {
                                    scannedItem = entry.item
                                    showResultDialog = true
                                }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(badgeColor.copy(alpha = 0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = iconVector,
                                        contentDescription = null,
                                        tint = badgeColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = entry.item.namaBarang,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF0F172A),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "ID: ${entry.item.idBarang} | Stok: ${entry.item.stokTersedia} ${entry.item.satuan}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Surface(
                                    color = badgeColor,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = badgeLabel,
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = entry.timestamp,
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

        // Dialog Peringatan QR Tidak Dikenal
        if (showUnknownQrDialog) {
            AlertDialog(
                onDismissRequest = { showUnknownQrDialog = false },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFFEF4444).copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Peringatan",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = "Kode QR Tidak Dikenali",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Kode QR yang dipindai tidak terdaftar di dalam sistem inventaris resmi Lunaris.",
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            color = Color(0xFF475569)
                        )
                        Surface(
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Isi Kode: $unknownQrText",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showUnknownQrDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Tutup", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Pop-up card dialog on successful scan
        if (showResultDialog && scannedItem != null) {
            val item = scannedItem!!
            val labKomCats = setOf(
                "PC Desktop", "Workstation Design", "Server Lab / NOC",
                "All-in-One PC", "Laptop LabKom", "Hardware Komputer",
                "Workstation", "Server", "All-in-One", "LabKom"
            )
            val isLab = item.type.equals("LABKOM", ignoreCase = true) ||
                    item.idBarang.startsWith("PC-LAB", ignoreCase = true) ||
                    item.idBarang.startsWith("LAB-", ignoreCase = true) ||
                    labKomCats.contains(item.kategori) ||
                    item.namaBarang.contains("PC Lab", ignoreCase = true) ||
                    item.namaBarang.contains("Workstation", ignoreCase = true) ||
                    item.namaBarang.contains("Server", ignoreCase = true)

            val isBhn = !isLab && (
                item.type.equals("BAHAN", ignoreCase = true) ||
                item.idBarang.startsWith("BHN-", ignoreCase = true) ||
                item.kategori.equals("Logistik", ignoreCase = true) ||
                item.kategori.contains("Bahan", ignoreCase = true)
            )

            val isPrp = !isLab && !isBhn && (
                item.type.equals("PERIPHERAL", ignoreCase = true) ||
                item.idBarang.startsWith("PRP-", ignoreCase = true) ||
                item.kategori.contains("Peripheral", ignoreCase = true)
            )

            val (categoryLabel, badgeBg) = when {
                isLab -> Pair("QR LABKOM", Color(0xFF7C3AED))
                isPrp -> Pair("QR PERIPHERAL", Color(0xFFD97706))
                isBhn -> Pair("QR BAHAN", Color(0xFF059669))
                else -> Pair("QR ALAT", Color(0xFF2563EB))
            }

            AlertDialog(
                onDismissRequest = { showResultDialog = false },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.QrCode, contentDescription = "QR", tint = Color(0xFF4F46E5))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Barang Terdeteksi", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        // Badge ALAT / BAHAN
                        Box(
                            modifier = Modifier
                                .background(badgeBg, RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = categoryLabel,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = item.namaBarang,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )
                        HorizontalDivider(color = Color(0xFFE2E8F0))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("ID Barang:", fontSize = 12.sp, color = Color.Gray)
                            Text(item.idBarang, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Kategori:", fontSize = 12.sp, color = Color.Gray)
                            Text(item.kategori.ifBlank { "-" }, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        if (!isBhn && item.merekAlat.isNotBlank()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Merek:", fontSize = 12.sp, color = Color.Gray)
                                Text(item.merekAlat, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Ruangan:", fontSize = 12.sp, color = Color.Gray)
                            Text(item.ruang.ifBlank { "-" }, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Kondisi:", fontSize = 12.sp, color = Color.Gray)
                            Text(item.kondisi.ifBlank { "Baik" }, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        if (item.keterangan.isNotBlank()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Keterangan:", fontSize = 12.sp, color = Color.Gray)
                                Text(item.keterangan, fontSize = 12.sp, fontWeight = FontWeight.Normal)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Stock summary card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (item.stokTersedia > 0) Color(0xFFECFDF5) else Color(0xFFFEF2F2),
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    1.dp,
                                    if (item.stokTersedia > 0) Color(0xFFA7F3D0) else Color(0xFFFECACA),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (item.stokTersedia > 0) "Stok Tersedia: ${item.stokTersedia} ${item.satuan}" else "Stok Habis (0 ${item.satuan})",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (item.stokTersedia > 0) Color(0xFF047857) else Color(0xFFB91C1C)
                                )
                                Text(
                                    text = "Total Stok Awal: ${item.stokAwal} ${item.satuan} | Stok Rusak: ${item.stokRusak} ${item.satuan}",
                                    fontSize = 11.sp,
                                    color = Color.DarkGray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Quick Action Buttons Row (Edit, Rusak, Perawatan) - NOTE: Strictly NO Hapus button!
                        Text("Aksi Cepat Inventaris:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Edit Button
                            Button(
                                onClick = { showEditDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                modifier = Modifier.weight(1f).height(40.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            // Rusak Button
                            Button(
                                onClick = { showDamagedDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                modifier = Modifier.weight(1f).height(40.dp)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = "Rusak", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Rusak", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            // Perawatan Button
                            Button(
                                onClick = { showMaintenanceDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                modifier = Modifier.weight(1f).height(40.dp)
                            ) {
                                Icon(Icons.Default.Build, contentDescription = "Perawatan", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Perawatan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 4.dp))

                        // Secondary Actions: Circulation
                        val isOutOfStock = item.stokTersedia == 0
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (item.type == "ALAT") {
                                OutlinedButton(
                                    onClick = {
                                        showResultDialog = false
                                        onNavigateToPeminjaman(item.idBarang)
                                    },
                                    enabled = !isOutOfStock,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.FileUpload, contentDescription = "Pinjam", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pinjam", fontSize = 11.sp)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    showResultDialog = false
                                    onNavigateToPengembalian(item.idBarang)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = "Kembali", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Kembali", fontSize = 11.sp)
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    // Close / Back button to read details without making changes
                    Button(
                        onClick = { showResultDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64748B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tutup", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // 1. Edit Item Dialog
        if (showEditDialog && scannedItem != null) {
            val item = scannedItem!!
            var nameInput by remember { mutableStateOf(item.namaBarang) }
            var catInput by remember { mutableStateOf(item.kategori) }
            var merekInput by remember { mutableStateOf(item.merekAlat) }
            var ruangInput by remember { mutableStateOf(item.ruang) }
            var kondisiInput by remember { mutableStateOf(item.kondisi.ifBlank { "Baik" }) }
            var stokAwalInput by remember { mutableStateOf(item.stokAwal.toString()) }
            var satuanInput by remember { mutableStateOf(item.satuan) }
            var ketInput by remember { mutableStateOf(item.keterangan) }

            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                title = { Text("Edit Detail Barang", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LunarisTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Nama Barang") }
                        )
                        LunarisTextField(
                            value = catInput,
                            onValueChange = { catInput = it },
                            label = { Text("Kategori") }
                        )
                        LunarisTextField(
                            value = merekInput,
                            onValueChange = { merekInput = it },
                            label = { Text("Merek") }
                        )
                        LunarisTextField(
                            value = ruangInput,
                            onValueChange = { ruangInput = it },
                            label = { Text("Ruang") }
                        )
                        LunarisTextField(
                            value = kondisiInput,
                            onValueChange = { kondisiInput = it },
                            label = { Text("Kondisi") }
                        )
                        LunarisTextField(
                            value = stokAwalInput,
                            onValueChange = { stokAwalInput = it },
                            label = { Text("Stok Awal") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        LunarisTextField(
                            value = satuanInput,
                            onValueChange = { satuanInput = it },
                            label = { Text("Satuan") }
                        )
                        LunarisTextField(
                            value = ketInput,
                            onValueChange = { ketInput = it },
                            label = { Text("Keterangan") }
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val stokInt = stokAwalInput.toIntOrNull() ?: item.stokAwal
                            viewModel.updateItemDetails(
                                idBarang = item.idBarang,
                                namaBarang = nameInput.trim(),
                                kategori = catInput.trim(),
                                satuan = satuanInput.trim(),
                                stokAwal = stokInt,
                                merekAlat = merekInput.trim(),
                                ruang = ruangInput.trim(),
                                sumberDana = item.sumberDana,
                                kondisi = kondisiInput.trim(),
                                keterangan = ketInput.trim(),
                                onSuccess = {
                                    Toast.makeText(context, "Data barang berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                                    showEditDialog = false
                                    showResultDialog = false
                                },
                                onError = { err ->
                                    Toast.makeText(context, "Gagal update: $err", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    ) {
                        Text("Simpan")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }

        // 2. Report Damaged Item Dialog
        if (showDamagedDialog && scannedItem != null) {
            val item = scannedItem!!
            val isBahan = item.type.equals("BAHAN", ignoreCase = true) || item.kategori.equals("Logistik", ignoreCase = true)
            var jumlahInput by remember { mutableStateOf("1") }
            var ketInput by remember { mutableStateOf("") }
            var kondisiBaru by remember { mutableStateOf("Rusak Ringan") }

            val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val sdfTime = SimpleDateFormat("HH:mm", Locale.US)
            val currentDate = sdfDate.format(Date())
            val currentTime = sdfTime.format(Date())

            AlertDialog(
                onDismissRequest = { showDamagedDialog = false },
                title = { Text(if (isBahan) "Pencatatan Bahan Afkir" else "Catat Kerusakan Alat", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Barang: ${item.namaBarang} (${item.idBarang})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        LunarisTextField(
                            value = jumlahInput,
                            onValueChange = { jumlahInput = it },
                            label = { Text("Jumlah Rusak / Afkir") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        if (!isBahan) {
                            Text("Kondisi Kerusakan:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = kondisiBaru == "Rusak Ringan",
                                    onClick = { kondisiBaru = "Rusak Ringan" },
                                    label = { Text("Rusak Ringan", fontSize = 11.sp) }
                                )
                                FilterChip(
                                    selected = kondisiBaru == "Rusak Berat",
                                    onClick = { kondisiBaru = "Rusak Berat" },
                                    label = { Text("Rusak Berat", fontSize = 11.sp) }
                                )
                            }
                        }
                        LunarisTextField(
                            value = ketInput,
                            onValueChange = { ketInput = it },
                            label = { Text(if (isBahan) "Alasan Afkir" else "Kronologi / Keterangan Kerusakan") },
                            minLines = 2
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val qty = jumlahInput.toIntOrNull() ?: 1
                            if (isBahan) {
                                viewModel.recordBahanAfkir(
                                    idBarang = item.idBarang,
                                    namaBarang = item.namaBarang,
                                    jumlahAfkir = qty,
                                    satuan = item.satuan,
                                    alasan = ketInput.ifBlank { "Afkir dari scan QR" },
                                    tanggalAfkir = currentDate,
                                    onSuccess = {
                                        Toast.makeText(context, "Bahan afkir berhasil dicatat!", Toast.LENGTH_SHORT).show()
                                        showDamagedDialog = false
                                        showResultDialog = false
                                    },
                                    onError = { err ->
                                        Toast.makeText(context, "Gagal: $err", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            } else {
                                viewModel.recordDamagedReport(
                                    idBarang = item.idBarang,
                                    namaBarang = item.namaBarang,
                                    jumlah = qty,
                                    tanggalKerusakan = currentDate,
                                    waktuKerusakan = currentTime,
                                    keteranganKerusakan = ketInput.ifBlank { "Laporan kerusakan via scan QR" },
                                    namaPetugas = "Petugas Logistik",
                                    kondisiBaru = kondisiBaru,
                                    onSuccess = {
                                        Toast.makeText(context, "Laporan kerusakan berhasil disimpan!", Toast.LENGTH_SHORT).show()
                                        showDamagedDialog = false
                                        showResultDialog = false
                                    },
                                    onError = { err ->
                                        Toast.makeText(context, "Gagal: $err", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                    ) {
                        Text("Simpan Laporan")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDamagedDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }

        // 3. Maintenance / Perawatan Dialog
        if (showMaintenanceDialog && scannedItem != null) {
            val item = scannedItem!!
            var catatanMaint by remember { mutableStateOf("") }
            val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val sdfTime = SimpleDateFormat("HH:mm", Locale.US)
            val currentDate = sdfDate.format(Date())
            val currentTime = sdfTime.format(Date())

            AlertDialog(
                onDismissRequest = { showMaintenanceDialog = false },
                title = { Text("Catat Perawatan / Servis", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Barang: ${item.namaBarang} (${item.idBarang})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        LunarisTextField(
                            value = catatanMaint,
                            onValueChange = { catatanMaint = it },
                            label = { Text("Detail Tindakan Perawatan / Servis") },
                            placeholder = { Text("Contoh: Pembersihan filter, penggantian oli, perawatan berkala...") },
                            minLines = 3
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.recordDamagedReport(
                                idBarang = item.idBarang,
                                namaBarang = item.namaBarang,
                                jumlah = 1,
                                tanggalKerusakan = currentDate,
                                waktuKerusakan = currentTime,
                                keteranganKerusakan = catatanMaint.ifBlank { "Pencatatan pemeliharaan berkala" },
                                namaPetugas = "Petugas Pemeliharaan",
                                kondisiBaru = "Dalam Perawatan",
                                status = "Servis Luar/Pemeliharaan",
                                onSuccess = {
                                    Toast.makeText(context, "Catatan pemeliharaan berhasil disimpan!", Toast.LENGTH_SHORT).show()
                                    showMaintenanceDialog = false
                                    showResultDialog = false
                                },
                                onError = { err ->
                                    Toast.makeText(context, "Gagal: $err", Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                    ) {
                        Text("Simpan Perawatan")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showMaintenanceDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }
}

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraPreviewView(
    onQrScanned: (String) -> Unit,
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
                                        if (rawValue != null) {
                                            onQrScanned(rawValue)
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
                    } else if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        null
                    }

                    if (cameraSelector != null) {
                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                        onCameraControlReady(camera.cameraControl)
                        try {
                            if (camera.cameraInfo.hasFlashUnit()) {
                                camera.cameraControl.enableTorch(isFlashOn)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                } catch (exc: Exception) {
                    exc.printStackTrace()
                }

            }, androidx.core.content.ContextCompat.getMainExecutor(context))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun GeneratorTab(viewModel: InventoryViewModel) {
    val context = LocalContext.current
    val items by viewModel.itemsWithStock.collectAsState()
    val allPeripheralStocks by viewModel.allPeripheralStocks.collectAsState()

    val peripheralAsItems = remember(allPeripheralStocks) {
        allPeripheralStocks.map { stock ->
            ItemWithStock(
                idBarang = stock.idBarang,
                namaBarang = stock.namaItem,
                stokAwal = stock.jumlah,
                stokTersedia = stock.jumlah,
                kategori = stock.jenisPeripheral,
                satuan = stock.satuan,
                merekAlat = stock.merek.ifBlank { stock.serialNumber },
                ruang = stock.lokasiRuang,
                sumberDana = stock.sumberDana,
                kondisi = stock.kondisi,
                keterangan = stock.spesifikasi,
                type = "PERIPHERAL",
                isBorrowable = false
            )
        }
    }

    val defaultLabKomUnits = remember { emptyList<ItemWithStock>() }

    val checkLabKom = { item: ItemWithStock ->
        val labKomCategories = setOf(
            "PC Desktop", "Workstation Design", "Server Lab / NOC",
            "All-in-One PC", "Laptop LabKom", "Hardware Komputer",
            "Workstation", "Server", "All-in-One", "LabKom"
        )
        item.type.equals("LABKOM", ignoreCase = true) ||
                item.idBarang.startsWith("PC-LAB", ignoreCase = true) ||
                item.idBarang.startsWith("LAB-", ignoreCase = true) ||
                labKomCategories.contains(item.kategori) ||
                item.namaBarang.contains("PC Lab", ignoreCase = true) ||
                item.namaBarang.contains("Workstation", ignoreCase = true) ||
                item.namaBarang.contains("Server Lab", ignoreCase = true)
    }

    val checkBahan = { item: ItemWithStock ->
        !checkLabKom(item) && (
            item.type.equals("BAHAN", ignoreCase = true) ||
                    item.idBarang.startsWith("BHN-", ignoreCase = true) ||
                    item.kategori.equals("Logistik", ignoreCase = true) ||
                    item.kategori.contains("Bahan", ignoreCase = true)
        )
    }

    val checkPeripheral = { item: ItemWithStock ->
        !checkLabKom(item) && !checkBahan(item) && (
            item.type.equals("PERIPHERAL", ignoreCase = true) ||
                    item.idBarang.startsWith("PRP-", ignoreCase = true) ||
                    item.kategori.contains("Peripheral", ignoreCase = true)
        )
    }

    val checkAlat = { item: ItemWithStock ->
        !checkLabKom(item) && !checkBahan(item) && !checkPeripheral(item)
    }

    val alatItems = remember(items) {
        items.filter { checkAlat(it) }
    }

    val bahanItems = remember(items) {
        items.filter { checkBahan(it) }
    }

    val peripheralItems = remember(items, peripheralAsItems) {
        val dbPeripherals = items.filter { checkPeripheral(it) }
        (peripheralAsItems + dbPeripherals).distinctBy { it.idBarang }
    }

    val labKomItems = remember(items, defaultLabKomUnits) {
        val dbLabKom = items.filter { checkLabKom(it) }
        (defaultLabKomUnits + dbLabKom).distinctBy { it.idBarang }
    }

    val allCombinedItems = remember(alatItems, bahanItems, peripheralItems, labKomItems) {
        (alatItems + bahanItems + peripheralItems + labKomItems).distinctBy { it.idBarang }
    }

    // Filter type state: "SEMUA", "ALAT", "BAHAN", "PERIPHERAL", "LABKOM"
    var typeFilter by remember { mutableStateOf("SEMUA") }

    // Dropdown and search states
    var searchQuery by remember { mutableStateOf("") }
    var selectedItem by remember { mutableStateOf<ItemWithStock?>(null) }
    var expandedDropdown by remember { mutableStateOf(false) }

    // Generated QR Output State
    var generatedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var generatedItemName by remember { mutableStateOf("") }
    var generatedItemId by remember { mutableStateOf("") }

    // Settings for persistent QR marker
    val settingsRepo = remember { viewModel.settingsRepository }
    var generatedSet by remember { mutableStateOf<Set<String>>(settingsRepo.getGeneratedQrCodes()) }

    // Dialog warnings
    var showRegenerateWarning by remember { mutableStateOf(false) }

    // History tab state: "SEMUA", "ALAT", "BAHAN", "PERIPHERAL", "LABKOM"
    var historySubTab by remember { mutableStateOf("SEMUA") }

    // Separate generated history lists for All, Alat, Bahan, Peripheral, and LabKom
    val generatedAllItems = remember(allCombinedItems, generatedSet) {
        allCombinedItems.filter { item -> generatedSet.contains(item.idBarang) }
    }

    val generatedAlatItems = remember(alatItems, generatedSet) {
        alatItems.filter { item -> generatedSet.contains(item.idBarang) }
    }

    val generatedBahanItems = remember(bahanItems, generatedSet) {
        bahanItems.filter { item -> generatedSet.contains(item.idBarang) }
    }

    val generatedPeripheralItems = remember(peripheralItems, generatedSet) {
        peripheralItems.filter { item -> generatedSet.contains(item.idBarang) }
    }

    val generatedLabKomItems = remember(labKomItems, generatedSet) {
        labKomItems.filter { item -> generatedSet.contains(item.idBarang) }
    }

    // Filter items strictly based on selected category filter (ALAT, BAHAN, PERIPHERAL, LABKOM, SEMUA)
    val filteredByTypeItems = remember(typeFilter, alatItems, bahanItems, peripheralItems, labKomItems, allCombinedItems) {
        when (typeFilter) {
            "ALAT" -> alatItems
            "BAHAN" -> bahanItems
            "PERIPHERAL" -> peripheralItems
            "LABKOM" -> labKomItems
            else -> allCombinedItems
        }
    }

    // Helper to generate a QR
    val triggerQrCodeGeneration = { content: String, name: String ->
        try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (bitMatrix.get(x, y)) AndroidColor.BLACK else AndroidColor.WHITE)
                }
            }
            generatedBitmap = bmp
            generatedItemName = name
            generatedItemId = content
            settingsRepo.markQrCodeGenerated(content)
            generatedSet = settingsRepo.getGeneratedQrCodes()
            Toast.makeText(context, "Kode QR untuk '$name' berhasil dibuat!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membuat kode QR: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Function to save image with custom white area and item name/specs text label
    val saveQrToGallery = { bitmap: Bitmap, fileNameId: String, itemName: String, itemDetail: ItemWithStock? ->
        try {
            val cardWidth = 640
            val cardHeight = 880
            val cleanName = if (itemName.startsWith("[") && itemName.contains("]")) {
                itemName.substringAfter("]").trim()
            } else {
                itemName
            }.trim()

            val combinedBitmap = Bitmap.createBitmap(cardWidth, cardHeight, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(combinedBitmap)
            
            val bgPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                style = android.graphics.Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, cardWidth.toFloat(), cardHeight.toFloat(), bgPaint)
            
            val gradient = android.graphics.LinearGradient(
                0f, 0f, cardWidth.toFloat(), cardHeight.toFloat(),
                android.graphics.Color.parseColor("#7C3AED"),
                android.graphics.Color.parseColor("#4F46E5"),
                android.graphics.Shader.TileMode.CLAMP
            )
            val borderPaint = android.graphics.Paint().apply {
                shader = gradient
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 14f
                isAntiAlias = true
            }
            val margin = 7f
            canvas.drawRoundRect(
                margin, margin, cardWidth.toFloat() - margin, cardHeight.toFloat() - margin,
                24f, 24f,
                borderPaint
            )

            val qrLeft = (cardWidth - 480) / 2f
            val qrTop = 60f
            val qrCardBgPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                style = android.graphics.Paint.Style.FILL
                isAntiAlias = true
            }
            val qrCardBorderPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#F1F5F9")
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 2f
                isAntiAlias = true
            }
            val qrCardRect = android.graphics.RectF(qrLeft - 12f, qrTop - 12f, qrLeft + 480f + 12f, qrTop + 480f + 12f)
            canvas.drawRoundRect(qrCardRect, 16f, 16f, qrCardBgPaint)
            canvas.drawRoundRect(qrCardRect, 16f, 16f, qrCardBorderPaint)

            canvas.drawBitmap(bitmap, null, android.graphics.RectF(qrLeft, qrTop, qrLeft + 480f, qrTop + 480f), null)
            
            val containerPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#F8FAFC")
                style = android.graphics.Paint.Style.FILL
                isAntiAlias = true
            }
            val containerBorderPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#E2E8F0")
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 2f
                isAntiAlias = true
            }
            val containerRect = android.graphics.RectF(48f, 580f, cardWidth - 48f, cardHeight - 40f)
            canvas.drawRoundRect(containerRect, 16f, 16f, containerPaint)
            canvas.drawRoundRect(containerRect, 16f, 16f, containerBorderPaint)

            val namePaint = android.text.TextPaint().apply {
                color = android.graphics.Color.parseColor("#0F172A")
                textSize = 26f
                isAntiAlias = true
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
            }
            val codePaint = android.text.TextPaint().apply {
                color = android.graphics.Color.parseColor("#4F46E5")
                textSize = 20f
                isAntiAlias = true
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
            }
            val detailPaint = android.text.TextPaint().apply {
                color = android.graphics.Color.parseColor("#475569")
                textSize = 17f
                isAntiAlias = true
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.NORMAL)
            }

            val maxContainerTextWidth = cardWidth - 120
            
            val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.text.StaticLayout.Builder.obtain(cleanName, 0, cleanName.length, namePaint, maxContainerTextWidth)
                    .setAlignment(android.text.Layout.Alignment.ALIGN_CENTER)
                    .setLineSpacing(0f, 1.1f)
                    .setIncludePad(false)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                android.text.StaticLayout(
                    cleanName,
                    namePaint,
                    maxContainerTextWidth,
                    android.text.Layout.Alignment.ALIGN_CENTER,
                    1.1f,
                    0f,
                    false
                )
            }
            
            var currentY = 600f
            canvas.save()
            canvas.translate(60f, currentY)
            staticLayout.draw(canvas)
            canvas.restore()

            currentY += staticLayout.height + 14f

            val codeText = "ID: $fileNameId" + if (!itemDetail?.serialNumber.isNullOrBlank()) " | SN: ${itemDetail?.serialNumber}" else ""
            val codeWidth = codePaint.measureText(codeText)
            val codeX = (cardWidth - codeWidth) / 2f
            canvas.drawText(codeText, codeX, currentY + 16f, codePaint)
            currentY += 34f

            val roomText = if (!itemDetail?.ruang.isNullOrBlank()) "Lokasi: ${itemDetail?.ruang}" else ""
            val specText = if (!itemDetail?.keterangan.isNullOrBlank()) itemDetail?.keterangan ?: "" else ""
            val combinedDetail = listOf(roomText, specText).filter { it.isNotBlank() }.joinToString(" • ")

            if (combinedDetail.isNotBlank()) {
                val detailLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    android.text.StaticLayout.Builder.obtain(combinedDetail, 0, combinedDetail.length, detailPaint, maxContainerTextWidth)
                        .setAlignment(android.text.Layout.Alignment.ALIGN_CENTER)
                        .setLineSpacing(0f, 1.1f)
                        .setIncludePad(false)
                        .build()
                } else {
                    @Suppress("DEPRECATION")
                    android.text.StaticLayout(
                        combinedDetail,
                        detailPaint,
                        maxContainerTextWidth,
                        android.text.Layout.Alignment.ALIGN_CENTER,
                        1.1f,
                        0f,
                        false
                    )
                }
                canvas.save()
                canvas.translate(60f, currentY + 8f)
                detailLayout.draw(canvas)
                canvas.restore()
            }

            val filename = "QR_${fileNameId.replace(" ", "_")}_${System.currentTimeMillis()}.png"
            val fos: OutputStream?
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/GudangSMANSA")
                }
                val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                fos = imageUri?.let { resolver.openOutputStream(it) }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()
                val image = java.io.File(imagesDir, filename)
                fos = java.io.FileOutputStream(image)
            }

            if (fos != null) {
                combinedBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                fos.close()
                Toast.makeText(context, "Gambar QR Code berhasil disimpan ke Galeri HP!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Gagal membuka jalur penyimpanan", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal menyimpan gambar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Standalone Card: Form Pilih Barang & Generate QR
        LunarisCard(
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, Color(0xFF4F46E5).copy(alpha = 0.3f), RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFF4F46E5).copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Search",
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Pilih Barang & Generate QR",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Pilih item dari daftar inventaris untuk membuat Kode QR",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                // Selection row containing dropdown field + universal filter icon button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Selection dropdown wrapper
                    Box(modifier = Modifier.weight(1f)) {
                        LunarisTextField(
                            value = selectedItem?.let { "[${it.idBarang}] ${it.namaBarang}" } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            placeholder = { Text("Klik untuk memilih barang...") },
                            trailingIcon = {
                                Icon(
                                    imageVector = if (expandedDropdown) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = "Pilih",
                                    modifier = Modifier.clickable { expandedDropdown = !expandedDropdown }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedDropdown = !expandedDropdown }
                                .testTag("dropdown_barang")
                        )

                        // Actual dropdown menu with search bar
                        DropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .height(320.dp)
                        ) {
                            LunarisTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                label = { Text("Cari Nama / ID Barang") },
                                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Cari") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            )

                            val searchedItems = filteredByTypeItems.filter {
                                it.namaBarang.contains(searchQuery, ignoreCase = true) ||
                                        it.idBarang.contains(searchQuery, ignoreCase = true)
                            }

                            if (searchedItems.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Tidak ada barang cocok") },
                                    onClick = {}
                                )
                            } else {
                                searchedItems.forEach { item ->
                                    val isGenerated = generatedSet.contains(item.idBarang)
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("[${item.idBarang}] ${item.namaBarang}", fontSize = 13.sp)
                                                if (isGenerated) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = Icons.Default.CheckCircle,
                                                            contentDescription = "Dibuat",
                                                            tint = Color(0xFF15803D),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Sudah Ada", fontSize = 10.sp, color = Color(0xFF15803D), fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        },
                                        onClick = {
                                            selectedItem = item
                                            expandedDropdown = false
                                            generatedBitmap = null
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Universal Filter Icon Button (Purple matching Generate button, aligned vertically)
                    Box {
                        var filterMenuExpanded by remember { mutableStateOf(false) }

                        IconButton(
                            onClick = { filterMenuExpanded = !filterMenuExpanded },
                            modifier = Modifier
                                .size(52.dp)
                                .background(Color(0xFF4F46E5), RoundedCornerShape(12.dp))
                                .testTag("btn_filter_jenis_barang")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Filter Jenis Barang",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = filterMenuExpanded,
                            onDismissRequest = { filterMenuExpanded = false },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.width(200.dp)
                        ) {
                            Text(
                                text = "Filter Jenis Barang",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4F46E5),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                            HorizontalDivider(color = Color(0xFFE2E8F0))

                            val filterOptions = listOf(
                                "SEMUA" to "Semua Barang",
                                "ALAT" to "QR Alat",
                                "BAHAN" to "QR Bahan",
                                "PERIPHERAL" to "QR Peripheral",
                                "LABKOM" to "QR LabKom"
                            )

                            filterOptions.forEach { (code, label) ->
                                val isSelected = typeFilter == code
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) Color(0xFF4F46E5) else Color(0xFF0F172A)
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Aktif",
                                                    tint = Color(0xFF4F46E5),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        typeFilter = code
                                        selectedItem = null
                                        filterMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Detail info card & Status indicator banner when item selected
                if (selectedItem != null) {
                    val item = selectedItem!!
                    val isLabKomItem = checkLabKom(item)
                    val alreadyHasQr = generatedSet.contains(item.idBarang)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Detailed item specification card
                        LunarisCard(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.namaBarang,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    Surface(
                                        color = if (isLabKomItem) Color(0xFFF3E8FF) else Color(0xFFE0E7FF),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (isLabKomItem) "PC LabKom" else item.type,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isLabKomItem) Color(0xFF7C3AED) else Color(0xFF4338CA),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "ID: ${item.idBarang}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF4F46E5),
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (item.serialNumber.isNotBlank()) {
                                        Text(
                                            text = "SN: ${item.serialNumber}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    if (item.ruang.isNotBlank()) {
                                        Text(
                                            text = "Lokasi: ${item.ruang}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }

                                if (item.keterangan.isNotBlank()) {
                                    Text(
                                        text = "Spesifikasi: ${item.keterangan}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF475569)
                                    )
                                }
                            }
                        }

                        // Status Banner
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (alreadyHasQr) Color(0xFFDCFCE7) else Color(0xFFEFF6FF),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = if (alreadyHasQr) Icons.Default.CheckCircle else Icons.Default.Info,
                                contentDescription = "Status",
                                tint = if (alreadyHasQr) Color(0xFF15803D) else Color(0xFF2563EB),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (alreadyHasQr) "Kode QR sudah pernah dibuat sebelumnya" else "Kode QR belum pernah dibuat",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (alreadyHasQr) Color(0xFF15803D) else Color(0xFF2563EB)
                            )
                        }
                    }
                }

                // Generate Button
                Button(
                    onClick = {
                        if (selectedItem == null) {
                            Toast.makeText(context, "Silakan pilih barang terlebih dahulu!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val alreadyGenerated = generatedSet.contains(selectedItem!!.idBarang)
                        if (alreadyGenerated) {
                            showRegenerateWarning = true
                        } else {
                            triggerQrCodeGeneration(selectedItem!!.idBarang, selectedItem!!.namaBarang)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_generate_qr")
                ) {
                    Icon(imageVector = Icons.Default.QrCode, contentDescription = "QR")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate QR Code", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Output Display Box of generated QR Code
        if (generatedBitmap != null && selectedItem != null) {
            val item = selectedItem!!
            LunarisCard(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "KODE QR BERHASIL DI-GENERATE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4F46E5),
                        letterSpacing = 1.sp
                    )

                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .background(Color.White, RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        Image(
                            bitmap = generatedBitmap!!.asImageBitmap(),
                            contentDescription = "Hasil QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = generatedItemName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Kode ID: $generatedItemId" + if (item.serialNumber.isNotBlank()) " | SN: ${item.serialNumber}" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4F46E5),
                            fontWeight = FontWeight.Bold
                        )
                        if (item.ruang.isNotBlank()) {
                            Text(
                                text = "Lokasi: ${item.ruang}",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        if (item.keterangan.isNotBlank()) {
                            Text(
                                text = "Spesifikasi: ${item.keterangan}",
                                fontSize = 11.sp,
                                color = Color(0xFF475569),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    Button(
                        onClick = { saveQrToGallery(generatedBitmap!!, generatedItemId, generatedItemName, item) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_simpan_qr_galeri")
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = "Simpan")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simpan ke Galeri", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3. Section Riwayat Item yang Baru Dibuat QR-nya
        LunarisCard(
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Section Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFF059669).copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Riwayat",
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Riwayat Item Dibuat QR",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Daftar inventaris yang telah memiliki Kode QR",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                // Filter chips for history: Semua, Alat, Bahan, Peripheral, LabKom
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = historySubTab == "SEMUA",
                        onClick = { historySubTab = "SEMUA" },
                        label = { Text("Semua (${generatedAllItems.size})", fontSize = 11.sp, fontWeight = FontWeight.Medium) }
                    )
                    FilterChip(
                        selected = historySubTab == "ALAT",
                        onClick = { historySubTab = "ALAT" },
                        label = { Text("Alat (${generatedAlatItems.size})", fontSize = 11.sp, fontWeight = FontWeight.Medium) }
                    )
                    FilterChip(
                        selected = historySubTab == "BAHAN",
                        onClick = { historySubTab = "BAHAN" },
                        label = { Text("Bahan (${generatedBahanItems.size})", fontSize = 11.sp, fontWeight = FontWeight.Medium) }
                    )
                    FilterChip(
                        selected = historySubTab == "PERIPHERAL",
                        onClick = { historySubTab = "PERIPHERAL" },
                        label = { Text("Peripheral (${generatedPeripheralItems.size})", fontSize = 11.sp, fontWeight = FontWeight.Medium) }
                    )
                    FilterChip(
                        selected = historySubTab == "LABKOM",
                        onClick = { historySubTab = "LABKOM" },
                        label = { Text("LabKom (${generatedLabKomItems.size})", fontSize = 11.sp, fontWeight = FontWeight.Medium) }
                    )
                }

                val currentHistoryList = when (historySubTab) {
                    "ALAT" -> generatedAlatItems
                    "BAHAN" -> generatedBahanItems
                    "PERIPHERAL" -> generatedPeripheralItems
                    "LABKOM" -> generatedLabKomItems
                    else -> generatedAllItems
                }

                if (currentHistoryList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = "Kosong",
                                tint = Color.LightGray,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = when (historySubTab) {
                                    "ALAT" -> "Belum ada QR Alat yang dibuat"
                                    "BAHAN" -> "Belum ada QR Bahan yang dibuat"
                                    "PERIPHERAL" -> "Belum ada QR Peripheral yang dibuat"
                                    "LABKOM" -> "Belum ada QR LabKom yang dibuat"
                                    else -> "Belum ada QR item yang dibuat"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Gray
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        currentHistoryList.forEach { item ->
                            val isBahan = checkBahan(item)
                            val isPeripheral = checkPeripheral(item)
                            val isLabKom = checkLabKom(item)

                            val accentColor = when {
                                isLabKom -> Color(0xFF7C3AED)
                                isPeripheral -> Color(0xFFD97706)
                                isBahan -> Color(0xFF059669)
                                else -> Color(0xFF2563EB)
                            }
                            val itemIcon = when {
                                isLabKom -> Icons.Default.Computer
                                isPeripheral -> Icons.Default.Extension
                                isBahan -> Icons.Default.Inventory2
                                else -> Icons.Default.Build
                            }
                            val tagText = when {
                                isLabKom -> "QR LabKom"
                                isPeripheral -> "Peripheral"
                                isBahan -> "Bahan"
                                else -> "Alat"
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF8FAFC))
                                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                    .clickable {
                                        selectedItem = item
                                        triggerQrCodeGeneration(item.idBarang, item.namaBarang)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(accentColor.copy(alpha = 0.1f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = itemIcon,
                                            contentDescription = "Icon",
                                            tint = accentColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = item.namaBarang,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = item.idBarang,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = accentColor
                                            )
                                            Text(
                                                text = "•",
                                                fontSize = 10.sp,
                                                color = Color.Gray
                                            )
                                            Text(
                                                text = item.kategori,
                                                fontSize = 10.sp,
                                                color = Color.Gray
                                            )
                                            Text(
                                                text = "•",
                                                fontSize = 10.sp,
                                                color = Color.Gray
                                            )
                                            Surface(
                                                color = accentColor.copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = tagText,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = accentColor,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        if (item.serialNumber.isNotBlank() || item.ruang.isNotBlank()) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                if (item.serialNumber.isNotBlank()) {
                                                    Text(
                                                        text = "SN: ${item.serialNumber}",
                                                        fontSize = 10.sp,
                                                        color = Color(0xFF475569),
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                                if (item.serialNumber.isNotBlank() && item.ruang.isNotBlank()) {
                                                    Text(
                                                        text = "•",
                                                        fontSize = 10.sp,
                                                        color = Color.Gray
                                                    )
                                                }
                                                if (item.ruang.isNotBlank()) {
                                                    Text(
                                                        text = item.ruang,
                                                        fontSize = 10.sp,
                                                        color = Color(0xFF64748B)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .background(Color(0xFFDCFCE7), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "QR Ready",
                                        tint = Color(0xFF15803D),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Lihat QR",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF15803D)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog Warnings on Regenerating QR
    if (showRegenerateWarning && selectedItem != null) {
        AlertDialog(
            onDismissRequest = { showRegenerateWarning = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = "Peringatan", tint = Color(0xFFD69E2E))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Peringatan Regenerasi QR", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "Kode QR untuk barang '${selectedItem!!.namaBarang}' (${selectedItem!!.idBarang}) sudah ada. Apakah Anda yakin ingin membuat ulang?",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRegenerateWarning = false
                        triggerQrCodeGeneration(selectedItem!!.idBarang, selectedItem!!.namaBarang)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD69E2E))
                ) {
                    Text("Ya, Buat Ulang")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRegenerateWarning = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun InputManualTab(viewModel: InventoryViewModel) {
    val context = LocalContext.current

    // Toggle Form: 0 = Form Alat, 1 = Form Bahan, 2 = Form Peripheral, 3 = Form Lab
    var selectedFormType by remember { mutableStateOf(0) }
    val isBahanSelected = selectedFormType == 1
    val isPeripheralSelected = selectedFormType == 2
    val isLabSelected = selectedFormType == 3

    // Master data from ViewModel
    val categoriesEntities by viewModel.allCategories.collectAsState()
    val unitsEntities by viewModel.allUnits.collectAsState()
    val ruangList by viewModel.ruang.collectAsState()
    val sumberDanaList by viewModel.sumberDana.collectAsState()
    val kondisiList by viewModel.kondisi.collectAsState()
    val merekAlatList by viewModel.merekAlat.collectAsState()

    val categories = remember(categoriesEntities) {
        categoriesEntities.map { it.name }.ifEmpty {
            listOf("Elektronik", "Alat Tulis", "Sarana Prasarana", "Olahraga", "Logistik")
        }
    }
    val units = remember(unitsEntities) {
        unitsEntities.map { it.name }.ifEmpty {
            listOf("Pcs", "Unit", "Set", "Pack", "Buku", "Keping")
        }
    }
    val masterRuang = remember(ruangList) {
        if (ruangList.isNotEmpty()) ruangList else listOf("Lab Komputer 1", "Lab Komputer 2", "Lab Server / NOC", "Ruang TU", "Gudang Utama")
    }
    val masterSumberDana = remember(sumberDanaList) {
        if (sumberDanaList.isNotEmpty()) sumberDanaList else listOf("BOS Reguler", "BOS Kinerja", "Komite Sekolah", "Bantuan Hibah / CSR", "Yayasan / Mandiri")
    }
    val masterKondisi = remember(kondisiList) {
        if (kondisiList.isNotEmpty()) kondisiList else listOf("Normal / Baik", "Expired / Afkir", "Rusak", "Pemeliharaan", "Rusak Fisik")
    }
    val masterMerek = remember(merekAlatList) {
        if (merekAlatList.isNotEmpty()) merekAlatList else listOf("Kingston", "Corsair", "Samsung", "Asus", "Gigabyte", "Logitech", "Epson", "Mikrotik")
    }

    val peripheralMasterCategories = remember {
        listOf(
            "RAM",
            "Internal Storage",
            "External Storage",
            "UPS",
            "GPU / Graphic Card",
            "Power Supply",
            "Networking",
            "Display / Monitor",
            "Kabel & Adaptor",
            "Aksesori",
            "Peripheral Lainnya"
        )
    }

    val labKomMasterCategories = remember {
        listOf(
            "PC Desktop",
            "Workstation Design",
            "Server Lab / NOC",
            "All-in-One PC",
            "Laptop LabKom",
            "Hardware Komputer"
        )
    }

    // Form state variables
    var manualNama by remember { mutableStateOf("") }
    var manualKategori by remember(categories) { mutableStateOf(categories.firstOrNull() ?: "Elektronik") }
    var manualPeripheralCat by remember { mutableStateOf(peripheralMasterCategories.first()) }
    var manualLabKomCat by remember { mutableStateOf(labKomMasterCategories.first()) }
    var manualSatuan by remember(units) { mutableStateOf(units.firstOrNull() ?: "Pcs") }
    var manualMerek by remember { mutableStateOf("") }
    var manualRuang by remember(masterRuang) { mutableStateOf(masterRuang.firstOrNull() ?: "Lab Komputer 1") }
    var manualSumberDana by remember(masterSumberDana) { mutableStateOf(masterSumberDana.firstOrNull() ?: "BOS Reguler") }
    var manualKondisi by remember(masterKondisi) { mutableStateOf(masterKondisi.firstOrNull() ?: "Normal / Baik") }
    var manualStok by remember { mutableStateOf("1") }
    var manualKeterangan by remember { mutableStateOf("") }
    var manualBorrowable by remember { mutableStateOf(false) }

    // Alat ID & SN states
    var manualAlatIsAutoId by remember { mutableStateOf(true) }
    var manualAlatCustomId by remember { mutableStateOf("") }
    var manualAlatSn by remember { mutableStateOf("") }
    var showAlatSnCameraScannerDialog by remember { mutableStateOf(false) }

    // Bahan ID & SN states
    var manualBahanIsAutoId by remember { mutableStateOf(true) }
    var manualBahanCustomId by remember { mutableStateOf("") }
    var manualBahanSn by remember { mutableStateOf("") }
    var showBahanSnCameraScannerDialog by remember { mutableStateOf(false) }

    // Peripheral ID & SN states
    var manualPeripheralIsAutoId by remember { mutableStateOf(true) }
    var manualPeripheralCustomId by remember { mutableStateOf("") }
    var manualPeripheralSn by remember { mutableStateOf("") }

    // LabKom ID, SN, and Spec states
    var manualLabKomIsAutoId by remember { mutableStateOf(true) }
    var manualLabKomCustomId by remember { mutableStateOf("") }
    var manualLabKomSn by remember { mutableStateOf("") }
    var manualLabKomSpec by remember { mutableStateOf("") }

    var expandedKategoriDropdown by remember { mutableStateOf(false) }
    var expandedSatuanDropdown by remember { mutableStateOf(false) }
    var expandedRuangDropdown by remember { mutableStateOf(false) }
    var expandedKondisiDropdown by remember { mutableStateOf(false) }
    var expandedSumberDanaDropdown by remember { mutableStateOf(false) }
    var expandedMerekDropdown by remember { mutableStateOf(false) }

    var showSnCameraScannerDialog by remember { mutableStateOf(false) }
    var showLabSnCameraScannerDialog by remember { mutableStateOf(false) }

    // Generated QR state for immediate download after manual save
    var generatedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var generatedItemName by remember { mutableStateOf("") }
    var generatedItemId by remember { mutableStateOf("") }

    val settingsRepo = remember { viewModel.settingsRepository }

    // Helper to generate a QR
    val triggerQrCodeGeneration = { content: String, name: String ->
        try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (bitMatrix.get(x, y)) AndroidColor.BLACK else AndroidColor.WHITE)
                }
            }
            generatedBitmap = bmp
            generatedItemName = name
            generatedItemId = content
            settingsRepo.markQrCodeGenerated(content)
            Toast.makeText(context, "Barang '$name' berhasil didaftarkan & Kode QR di-generate!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membuat kode QR: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    val saveQrToGallery = { bitmap: Bitmap, fileNameId: String, itemName: String ->
        try {
            val cardWidth = 640
            val cardHeight = 840
            val combinedBitmap = Bitmap.createBitmap(cardWidth, cardHeight, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(combinedBitmap)
            
            val bgPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                style = android.graphics.Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, cardWidth.toFloat(), cardHeight.toFloat(), bgPaint)
            
            val gradient = android.graphics.LinearGradient(
                0f, 0f, cardWidth.toFloat(), cardHeight.toFloat(),
                android.graphics.Color.parseColor("#3B82F6"),
                android.graphics.Color.parseColor("#2DD4BF"),
                android.graphics.Shader.TileMode.CLAMP
            )
            val borderPaint = android.graphics.Paint().apply {
                shader = gradient
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 14f
                isAntiAlias = true
            }
            val margin = 7f
            canvas.drawRoundRect(margin, margin, cardWidth.toFloat() - margin, cardHeight.toFloat() - margin, 24f, 24f, borderPaint)

            val qrLeft = (cardWidth - 512) / 2f
            val qrTop = 80f
            canvas.drawBitmap(bitmap, qrLeft, qrTop, null)

            val namePaint = android.text.TextPaint().apply {
                color = android.graphics.Color.parseColor("#0F172A")
                textSize = 28f
                isAntiAlias = true
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
            }
            val codePaint = android.text.TextPaint().apply {
                color = android.graphics.Color.parseColor("#64748B")
                textSize = 20f
                isAntiAlias = true
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.NORMAL)
            }

            val codeText = "Kode ID: $fileNameId"
            canvas.drawText(itemName, 80f, 650f, namePaint)
            canvas.drawText(codeText, 80f, 700f, codePaint)

            val filename = "QR_${fileNameId.replace(" ", "_")}_${System.currentTimeMillis()}.png"
            val fos: OutputStream?
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/GudangSMANSA")
                }
                val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                fos = imageUri?.let { resolver.openOutputStream(it) }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()
                val image = java.io.File(imagesDir, filename)
                fos = java.io.FileOutputStream(image)
            }

            if (fos != null) {
                combinedBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                fos.close()
                Toast.makeText(context, "Gambar QR Code berhasil disimpan ke Galeri HP!", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal menyimpan gambar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Upper Card: Selector for Form Alat vs Form Bahan vs Form Peripheral vs Form Lab
        val activeAccentColor = when (selectedFormType) {
            1 -> Color(0xFF059669)
            2 -> Color(0xFFD97706)
            3 -> Color(0xFF7C3AED)
            else -> Color(0xFF2563EB)
        }
        val activeCardBorderColor = when (selectedFormType) {
            1 -> Color(0xFF10B981)
            2 -> Color(0xFFF59E0B)
            3 -> Color(0xFF8B5CF6)
            else -> Color(0xFF3B82F6)
        }

        LunarisCard(
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, activeCardBorderColor.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Pilih Jenis Formulir Manual",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                // Grid 2x2 Layout for Form Selector
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Row 1: Form Alat & Form Bahan
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // --- Form Alat Selection Card ---
                        val isAlatActive = selectedFormType == 0
                        val alatBg = if (isAlatActive) {
                            Brush.horizontalGradient(listOf(Color(0xFF1E40AF), Color(0xFF2563EB)))
                        } else {
                            Brush.horizontalGradient(listOf(Color(0xFFF8FAFC), Color(0xFFF1F5F9)))
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(alatBg)
                                .border(
                                    width = if (isAlatActive) 1.5.dp else 1.dp,
                                    color = if (isAlatActive) Color(0xFF2563EB) else Color(0xFFE2E8F0),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedFormType = 0 }
                                .padding(horizontal = 10.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            if (isAlatActive) Color.White.copy(alpha = 0.2f) else Color(0xFF2563EB).copy(alpha = 0.1f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Build,
                                        contentDescription = "Alat",
                                        tint = if (isAlatActive) Color.White else Color(0xFF2563EB),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                Text(
                                    text = "Form Alat",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAlatActive) Color.White else Color(0xFF1E293B)
                                )
                            }
                        }

                        // --- Form Bahan Selection Card ---
                        val isBahanActive = selectedFormType == 1
                        val bahanBg = if (isBahanActive) {
                            Brush.horizontalGradient(listOf(Color(0xFF065F46), Color(0xFF059669)))
                        } else {
                            Brush.horizontalGradient(listOf(Color(0xFFF8FAFC), Color(0xFFF1F5F9)))
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(bahanBg)
                                .border(
                                    width = if (isBahanActive) 1.5.dp else 1.dp,
                                    color = if (isBahanActive) Color(0xFF059669) else Color(0xFFE2E8F0),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedFormType = 1 }
                                .padding(horizontal = 10.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            if (isBahanActive) Color.White.copy(alpha = 0.2f) else Color(0xFF059669).copy(alpha = 0.1f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Inventory2,
                                        contentDescription = "Bahan",
                                        tint = if (isBahanActive) Color.White else Color(0xFF059669),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                Text(
                                    text = "Form Bahan",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isBahanActive) Color.White else Color(0xFF1E293B)
                                )
                            }
                        }
                    }

                    // Row 2: Form Peripheral & Form Lab
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // --- Form Peripheral Selection Card ---
                        val isPeripheralActive = selectedFormType == 2
                        val peripheralBg = if (isPeripheralActive) {
                            Brush.horizontalGradient(listOf(Color(0xFFB45309), Color(0xFFD97706)))
                        } else {
                            Brush.horizontalGradient(listOf(Color(0xFFF8FAFC), Color(0xFFF1F5F9)))
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(peripheralBg)
                                .border(
                                    width = if (isPeripheralActive) 1.5.dp else 1.dp,
                                    color = if (isPeripheralActive) Color(0xFFD97706) else Color(0xFFE2E8F0),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedFormType = 2 }
                                .padding(horizontal = 10.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            if (isPeripheralActive) Color.White.copy(alpha = 0.2f) else Color(0xFFD97706).copy(alpha = 0.1f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Extension,
                                        contentDescription = "Peripheral",
                                        tint = if (isPeripheralActive) Color.White else Color(0xFFD97706),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                Text(
                                    text = "Form Peripheral",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPeripheralActive) Color.White else Color(0xFF1E293B)
                                )
                            }
                        }

                        // --- Form Lab Selection Card ---
                        val isLabActive = selectedFormType == 3
                        val labBg = if (isLabActive) {
                            Brush.horizontalGradient(listOf(Color(0xFF5B21B6), Color(0xFF7C3AED)))
                        } else {
                            Brush.horizontalGradient(listOf(Color(0xFFF8FAFC), Color(0xFFF1F5F9)))
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(labBg)
                                .border(
                                    width = if (isLabActive) 1.5.dp else 1.dp,
                                    color = if (isLabActive) Color(0xFF7C3AED) else Color(0xFFE2E8F0),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedFormType = 3 }
                                .padding(horizontal = 10.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            if (isLabActive) Color.White.copy(alpha = 0.2f) else Color(0xFF7C3AED).copy(alpha = 0.1f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Computer,
                                        contentDescription = "Form Lab",
                                        tint = if (isLabActive) Color.White else Color(0xFF7C3AED),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                Text(
                                    text = "Form Lab",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLabActive) Color.White else Color(0xFF1E293B)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Separate Card for Form Input (Dynamic Title & Colorful Border)
        LunarisCard(
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, activeAccentColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Dynamic Card Title Banner
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(activeAccentColor.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(activeAccentColor.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (selectedFormType) {
                                1 -> Icons.Default.Inventory2
                                2 -> Icons.Default.Extension
                                3 -> Icons.Default.Computer
                                else -> Icons.Default.Build
                            },
                            contentDescription = "Icon Form",
                            tint = activeAccentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = when (selectedFormType) {
                            1 -> "Form Input Bahan Manual"
                            2 -> "Form Input Peripheral Manual"
                            3 -> "Form Input PC Lab Komputer Manual"
                            else -> "Form Input Alat Manual"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                if (isLabSelected) {
                    // --- FORM LAB (LABKOM PC) FIELDS ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Mode ID Perangkat:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF475569)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilterChip(
                                selected = manualLabKomIsAutoId,
                                onClick = { manualLabKomIsAutoId = true },
                                label = { Text("Otomatis (PC-LAB-)", fontSize = 11.sp) }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            FilterChip(
                                selected = !manualLabKomIsAutoId,
                                onClick = { manualLabKomIsAutoId = false },
                                label = { Text("Manual", fontSize = 11.sp) }
                            )
                        }
                    }

                    if (!manualLabKomIsAutoId) {
                        LunarisTextField(
                            value = manualLabKomCustomId,
                            onValueChange = { manualLabKomCustomId = it },
                            label = { Text("Ketik ID PC / Perangkat Lab") },
                            placeholder = { Text("Contoh: PC-LAB1-01") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Camera Scanner Dialog for Serial Number / QR
                    if (showLabSnCameraScannerDialog) {
                        CameraScannerDialog(
                            title = "Pindai Serial Number (SN) Komputer",
                            initialMode = ScanMode.FALLBACK_BARCODE,
                            onDismissRequest = { showLabSnCameraScannerDialog = false },
                            onCodeScannedWithMode = { scanned, mode ->
                                manualLabKomSn = scanned
                                val modeText = if (mode == ScanMode.PRIMARY_QR) "QR Lunaris" else "Barcode Pabrik"
                                Toast.makeText(context, "[$modeText] Serial Number berhasil dipindai: $scanned", Toast.LENGTH_SHORT).show()
                                showLabSnCameraScannerDialog = false
                            }
                        )
                    }

                    // Input Serial Number (SN) with Camera Scanner Button
                    LunarisTextField(
                        value = manualLabKomSn,
                        onValueChange = { manualLabKomSn = it },
                        label = { Text("Nomor Seri / Serial Number (SN)") },
                        placeholder = { Text("Contoh: SN-PC-20240901") },
                        trailingIcon = {
                            IconButton(onClick = { showLabSnCameraScannerDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Pindai Barcode / QR SN",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Dropdown Kategori LabKom
                    Box(modifier = Modifier.fillMaxWidth()) {
                        LunarisTextField(
                            value = manualLabKomCat,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Kategori / Jenis Komputer *") },
                            trailingIcon = {
                                Icon(
                                    imageVector = if (expandedKategoriDropdown) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = "Pilih",
                                    modifier = Modifier.clickable { expandedKategoriDropdown = !expandedKategoriDropdown }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedKategoriDropdown = !expandedKategoriDropdown }
                        )
                        DropdownMenu(
                            expanded = expandedKategoriDropdown,
                            onDismissRequest = { expandedKategoriDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            labKomMasterCategories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        manualLabKomCat = cat
                                        expandedKategoriDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Input Nama Perangkat / Nomor PC
                    LunarisTextField(
                        value = manualNama,
                        onValueChange = { manualNama = it },
                        label = { Text("Nomor PC / Nama Perangkat *") },
                        placeholder = { Text("Contoh: PC Client 01 - Lab 1") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_nama_barang")
                    )

                    // Input Spesifikasi Singkat
                    LunarisTextField(
                        value = manualLabKomSpec,
                        onValueChange = { manualLabKomSpec = it },
                        label = { Text("Spesifikasi Singkat PC") },
                        placeholder = { Text("Contoh: Core i5-10400 | RAM 16GB | SSD 512GB | GTX 1650") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Input Merek / Brand Dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        LunarisTextField(
                            value = manualMerek,
                            onValueChange = { manualMerek = it },
                            label = { Text("Merek / Rakitan") },
                            placeholder = { Text("Contoh: ASUS / Dell / HP / Rakitan Lab") },
                            trailingIcon = {
                                Icon(
                                    imageVector = if (expandedMerekDropdown) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = "Pilih Merek",
                                    modifier = Modifier.clickable { expandedMerekDropdown = !expandedMerekDropdown }
                                )
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = expandedMerekDropdown,
                            onDismissRequest = { expandedMerekDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            masterMerek.forEach { brand ->
                                DropdownMenuItem(
                                    text = { Text(brand) },
                                    onClick = {
                                        manualMerek = brand
                                        expandedMerekDropdown = false
                                    }
                                )
                            }
                        }
                    }
                } else if (isPeripheralSelected) {
                    // --- PERIPHERAL FORM SPECIFIC FIELDS ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Mode ID Barang:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF475569)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilterChip(
                                selected = manualPeripheralIsAutoId,
                                onClick = { manualPeripheralIsAutoId = true },
                                label = { Text("Otomatis (PER-)", fontSize = 11.sp) }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            FilterChip(
                                selected = !manualPeripheralIsAutoId,
                                onClick = { manualPeripheralIsAutoId = false },
                                label = { Text("Manual", fontSize = 11.sp) }
                            )
                        }
                    }

                    if (!manualPeripheralIsAutoId) {
                        LunarisTextField(
                            value = manualPeripheralCustomId,
                            onValueChange = { manualPeripheralCustomId = it },
                            label = { Text("Ketik ID Peripheral") },
                            placeholder = { Text("Contoh: PER-RAM-001") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Camera Scanner Dialog for Serial Number / QR
                    if (showSnCameraScannerDialog) {
                        CameraScannerDialog(
                            title = "Pindai Serial Number (SN) / Barcode",
                            initialMode = ScanMode.FALLBACK_BARCODE,
                            onDismissRequest = { showSnCameraScannerDialog = false },
                            onCodeScannedWithMode = { scanned, mode ->
                                manualPeripheralSn = scanned
                                val modeText = if (mode == ScanMode.PRIMARY_QR) "QR Lunaris" else "Barcode Pabrik"
                                Toast.makeText(context, "[$modeText] Serial Number berhasil dipindai: $scanned", Toast.LENGTH_SHORT).show()
                                showSnCameraScannerDialog = false
                            }
                        )
                    }

                    // Input Serial Number (SN) with Camera Scanner Button
                    LunarisTextField(
                        value = manualPeripheralSn,
                        onValueChange = { manualPeripheralSn = it },
                        label = { Text("Serial Number (SN)") },
                        placeholder = { Text("Contoh: SN-8493821093") },
                        trailingIcon = {
                            IconButton(onClick = { showSnCameraScannerDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Pindai Barcode / QR SN",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Dropdown Kategori Peripheral
                    Box(modifier = Modifier.fillMaxWidth()) {
                        LunarisTextField(
                            value = manualPeripheralCat,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Kategori Peripheral *") },
                            trailingIcon = {
                                Icon(
                                    imageVector = if (expandedKategoriDropdown) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = "Pilih",
                                    modifier = Modifier.clickable { expandedKategoriDropdown = !expandedKategoriDropdown }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedKategoriDropdown = !expandedKategoriDropdown }
                        )
                        DropdownMenu(
                            expanded = expandedKategoriDropdown,
                            onDismissRequest = { expandedKategoriDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            peripheralMasterCategories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        manualPeripheralCat = cat
                                        expandedKategoriDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Input Nama Peripheral / Modul
                    LunarisTextField(
                        value = manualNama,
                        onValueChange = { manualNama = it },
                        label = { Text("Nama Perangkat / Modul *") },
                        placeholder = { Text("Contoh: RAM DDR4 8GB PC-21300") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_nama_barang")
                    )

                    // Input Merek / Brand Dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        LunarisTextField(
                            value = manualMerek,
                            onValueChange = { manualMerek = it },
                            label = { Text("Merek / Brand") },
                            placeholder = { Text("Contoh: Kingston / Corsair / Samsung") },
                            trailingIcon = {
                                Icon(
                                    imageVector = if (expandedMerekDropdown) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = "Pilih Merek",
                                    modifier = Modifier.clickable { expandedMerekDropdown = !expandedMerekDropdown }
                                )
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = expandedMerekDropdown,
                            onDismissRequest = { expandedMerekDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            masterMerek.forEach { brand ->
                                DropdownMenuItem(
                                    text = { Text(brand) },
                                    onClick = {
                                        manualMerek = brand
                                        expandedMerekDropdown = false
                                    }
                                )
                            }
                        }
                    }
                } else if (isBahanSelected) {
                    // --- FORM BAHAN FIELDS ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Mode ID Bahan:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF475569)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilterChip(
                                selected = manualBahanIsAutoId,
                                onClick = { manualBahanIsAutoId = true },
                                label = { Text("Otomatis (BHN-)", fontSize = 11.sp) }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            FilterChip(
                                selected = !manualBahanIsAutoId,
                                onClick = { manualBahanIsAutoId = false },
                                label = { Text("Input Manual", fontSize = 11.sp) }
                            )
                        }
                    }

                    if (!manualBahanIsAutoId) {
                        LunarisTextField(
                            value = manualBahanCustomId,
                            onValueChange = { manualBahanCustomId = it },
                            label = { Text("Kode Barang / ID Bahan *") },
                            placeholder = { Text("Contoh: BHN-001 / BHN-KRT-01") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("manual_id_bahan")
                        )
                    }

                    // Camera Scanner Dialog for Serial Number / Barcode Bahan
                    if (showBahanSnCameraScannerDialog) {
                        CameraScannerDialog(
                            title = "Pindai Serial Number / Barcode Bahan",
                            initialMode = ScanMode.FALLBACK_BARCODE,
                            onDismissRequest = { showBahanSnCameraScannerDialog = false },
                            onCodeScannedWithMode = { scanned, mode ->
                                manualBahanSn = scanned
                                val modeText = if (mode == ScanMode.PRIMARY_QR) "QR Lunaris" else "Barcode Pabrik"
                                Toast.makeText(context, "[$modeText] Serial Number / Barcode berhasil dipindai: $scanned", Toast.LENGTH_SHORT).show()
                                showBahanSnCameraScannerDialog = false
                            }
                        )
                    }

                    // Input Serial Number (SN / Barcode Opsional) with Camera Scanner Button
                    LunarisTextField(
                        value = manualBahanSn,
                        onValueChange = { manualBahanSn = it },
                        label = { Text("Nomor Seri / Serial Number (SN / Barcode Opsional)") },
                        placeholder = { Text("Contoh: 899100100201") },
                        trailingIcon = {
                            IconButton(onClick = { showBahanSnCameraScannerDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Pindai Barcode / QR Bahan",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Input Nama Bahan
                    LunarisTextField(
                        value = manualNama,
                        onValueChange = { manualNama = it },
                        label = { Text("Nama Bahan *") },
                        placeholder = { Text("Contoh: Kertas A4 80gr / Filament 3D Printer") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_nama_barang")
                    )

                    // Input Kategori Bahan Dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        LunarisTextField(
                            value = manualKategori,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Kategori Bahan *") },
                            trailingIcon = {
                                Icon(
                                    imageVector = if (expandedKategoriDropdown) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = "Pilih",
                                    modifier = Modifier.clickable { expandedKategoriDropdown = !expandedKategoriDropdown }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedKategoriDropdown = !expandedKategoriDropdown }
                        )
                        DropdownMenu(
                            expanded = expandedKategoriDropdown,
                            onDismissRequest = { expandedKategoriDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        manualKategori = cat
                                        expandedKategoriDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Input Spesifikasi / Merek Bahan
                    LunarisTextField(
                        value = manualMerek,
                        onValueChange = { manualMerek = it },
                        label = { Text("Spesifikasi / Merek Bahan (Opsional)") },
                        placeholder = { Text("Contoh: PaperOne / Joyko / Sinar Dunia") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // --- FORM ALAT FIELDS ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Mode ID Alat:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF475569)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilterChip(
                                selected = manualAlatIsAutoId,
                                onClick = { manualAlatIsAutoId = true },
                                label = { Text("Otomatis (ALT-)", fontSize = 11.sp) }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            FilterChip(
                                selected = !manualAlatIsAutoId,
                                onClick = { manualAlatIsAutoId = false },
                                label = { Text("Input Manual", fontSize = 11.sp) }
                            )
                        }
                    }

                    if (!manualAlatIsAutoId) {
                        LunarisTextField(
                            value = manualAlatCustomId,
                            onValueChange = { manualAlatCustomId = it },
                            label = { Text("Kode Barang / ID Alat *") },
                            placeholder = { Text("Contoh: ALT-001 / BRG-001") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("manual_id_alat")
                        )
                    }

                    // Camera Scanner Dialog for Serial Number / QR Alat
                    if (showAlatSnCameraScannerDialog) {
                        CameraScannerDialog(
                            title = "Pindai Serial Number (SN) Alat",
                            initialMode = ScanMode.FALLBACK_BARCODE,
                            onDismissRequest = { showAlatSnCameraScannerDialog = false },
                            onCodeScannedWithMode = { scanned, mode ->
                                manualAlatSn = scanned
                                val modeText = if (mode == ScanMode.PRIMARY_QR) "QR Lunaris" else "Barcode Pabrik"
                                Toast.makeText(context, "[$modeText] Serial Number berhasil dipindai: $scanned", Toast.LENGTH_SHORT).show()
                                showAlatSnCameraScannerDialog = false
                            }
                        )
                    }

                    // Input Serial Number (SN) with Camera Scanner Button
                    LunarisTextField(
                        value = manualAlatSn,
                        onValueChange = { manualAlatSn = it },
                        label = { Text("Nomor Seri / Serial Number (SN)") },
                        placeholder = { Text("Contoh: SN-8492048201") },
                        trailingIcon = {
                            IconButton(onClick = { showAlatSnCameraScannerDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Pindai Barcode / QR Alat",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Input Nama Alat
                    LunarisTextField(
                        value = manualNama,
                        onValueChange = { manualNama = it },
                        label = { Text("Nama Alat *") },
                        placeholder = { Text("Contoh: Laptop Asus Vivobook / Projector Epson") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_nama_barang")
                    )

                    // Input Kategori Alat Dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        LunarisTextField(
                            value = manualKategori,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Kategori *") },
                            trailingIcon = {
                                Icon(
                                    imageVector = if (expandedKategoriDropdown) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = "Pilih",
                                    modifier = Modifier.clickable { expandedKategoriDropdown = !expandedKategoriDropdown }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedKategoriDropdown = !expandedKategoriDropdown }
                        )
                        DropdownMenu(
                            expanded = expandedKategoriDropdown,
                            onDismissRequest = { expandedKategoriDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        manualKategori = cat
                                        expandedKategoriDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Input Merek Alat
                    LunarisTextField(
                        value = manualMerek,
                        onValueChange = { manualMerek = it },
                        label = { Text("Merek Alat") },
                        placeholder = { Text("Contoh: Asus / Epson / Mikrotik") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Input Ruang Dropdown
                    Box(modifier = Modifier.weight(1f)) {
                        LunarisTextField(
                            value = manualRuang,
                            onValueChange = {},
                            readOnly = true,
                            label = {
                                Text(
                                    when {
                                        isBahanSelected -> "Lokasi Penyimpanan *"
                                        isPeripheralSelected -> "Lokasi Penempatan *"
                                        isLabSelected -> "Ruang / Lokasi Lab *"
                                        else -> "Ruang / Lokasi *"
                                    }
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = if (expandedRuangDropdown) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = "Pilih Ruang",
                                    modifier = Modifier.clickable { expandedRuangDropdown = !expandedRuangDropdown }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedRuangDropdown = !expandedRuangDropdown }
                        )
                        DropdownMenu(
                            expanded = expandedRuangDropdown,
                            onDismissRequest = { expandedRuangDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            masterRuang.forEach { room ->
                                DropdownMenuItem(
                                    text = { Text(room) },
                                    onClick = {
                                        manualRuang = room
                                        expandedRuangDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Input Kondisi Dropdown
                    Box(modifier = Modifier.weight(1f)) {
                        LunarisTextField(
                            value = manualKondisi,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Kondisi Awal *") },
                            trailingIcon = {
                                Icon(
                                    imageVector = if (expandedKondisiDropdown) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = "Pilih Kondisi",
                                    modifier = Modifier.clickable { expandedKondisiDropdown = !expandedKondisiDropdown }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedKondisiDropdown = !expandedKondisiDropdown }
                        )
                        DropdownMenu(
                            expanded = expandedKondisiDropdown,
                            onDismissRequest = { expandedKondisiDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            masterKondisi.forEach { cond ->
                                DropdownMenuItem(
                                    text = { Text(cond) },
                                    onClick = {
                                        manualKondisi = cond
                                        expandedKondisiDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Sumber Dana Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    LunarisTextField(
                        value = manualSumberDana,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Sumber Dana *") },
                        trailingIcon = {
                            Icon(
                                imageVector = if (expandedSumberDanaDropdown) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = "Pilih Sumber Dana",
                                modifier = Modifier.clickable { expandedSumberDanaDropdown = !expandedSumberDanaDropdown }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedSumberDanaDropdown = !expandedSumberDanaDropdown }
                    )
                    DropdownMenu(
                        expanded = expandedSumberDanaDropdown,
                        onDismissRequest = { expandedSumberDanaDropdown = false },
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        masterSumberDana.forEach { src ->
                            DropdownMenuItem(
                                text = { Text(src) },
                                onClick = {
                                    manualSumberDana = src
                                    expandedSumberDanaDropdown = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Input Stok Awal
                    Box(modifier = Modifier.weight(1f)) {
                        LunarisTextField(
                            value = manualStok,
                            onValueChange = { manualStok = it },
                            label = { Text(if (isBahanSelected) "Jumlah Stok Minimal *" else "Stok Awal / Unit *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("manual_stok_awal")
                        )
                    }

                    // Input Satuan Dropdown
                    Box(modifier = Modifier.weight(1f)) {
                        LunarisTextField(
                            value = manualSatuan,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Satuan *") },
                            trailingIcon = {
                                Icon(
                                    imageVector = if (expandedSatuanDropdown) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = "Pilih",
                                    modifier = Modifier.clickable { expandedSatuanDropdown = !expandedSatuanDropdown }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedSatuanDropdown = !expandedSatuanDropdown }
                        )
                        DropdownMenu(
                            expanded = expandedSatuanDropdown,
                            onDismissRequest = { expandedSatuanDropdown = false }
                        ) {
                            units.forEach { sat ->
                                DropdownMenuItem(
                                    text = { Text(sat) },
                                    onClick = {
                                        manualSatuan = sat
                                        expandedSatuanDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Input Keterangan
                LunarisTextField(
                    value = manualKeterangan,
                    onValueChange = { manualKeterangan = it },
                    label = { Text("Keterangan Tambahan / Spesifikasi") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (!isBahanSelected && !isLabSelected) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { manualBorrowable = !manualBorrowable }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Switch(
                            checked = manualBorrowable,
                            onCheckedChange = { manualBorrowable = it },
                            modifier = Modifier.testTag("switch_manual_borrowable")
                        )
                        Column {
                            Text(
                                text = "Tampilkan untuk Siswa",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (manualBorrowable) "Barang ini dapat dilihat & dipinjam oleh siswa" else "Sembunyikan dari siswa (default off - akses khusus admin)",
                                fontSize = 11.sp,
                                color = if (manualBorrowable) Color(0xFF059669) else Color(0xFFDC2626)
                            )
                        }
                    }
                }

                // Submit Button which registers on DB + generates QR Code
                Button(
                    onClick = {
                        val stockInt = manualStok.toIntOrNull()
                        if (manualNama.isBlank()) {
                            Toast.makeText(context, "Nama / Nomor PC tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (stockInt == null || stockInt < 1) {
                            Toast.makeText(context, "Stok awal minimal 1!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        if (isLabSelected) {
                            val finalId = if (manualLabKomIsAutoId || manualLabKomCustomId.isBlank()) {
                                val prefix = when {
                                    manualRuang.contains("1") -> "PC-LAB1"
                                    manualRuang.contains("2") -> "PC-LAB2"
                                    manualRuang.contains("Server") || manualRuang.contains("NOC") -> "PC-LAB3"
                                    else -> "PC-LAB"
                                }
                                "$prefix-${java.text.SimpleDateFormat("HHmmss", java.util.Locale.US).format(java.util.Date())}"
                            } else {
                                manualLabKomCustomId.trim()
                            }

                            val fullSpec = listOf(manualLabKomSpec.trim(), manualKeterangan.trim()).filter { it.isNotBlank() }.joinToString(" | ")

                            viewModel.registerNewItem(
                                name = manualNama.trim(),
                                serialNumber = manualLabKomSn.trim(),
                                stokAwal = stockInt,
                                kategori = manualLabKomCat.trim(),
                                satuan = manualSatuan.trim().ifEmpty { "Unit" },
                                merekAlat = manualMerek.trim(),
                                ruang = manualRuang.trim().ifEmpty { "Lab Komputer 1" },
                                sumberDana = manualSumberDana.trim().ifEmpty { "BOS Reguler" },
                                kondisi = manualKondisi.trim().ifEmpty { "Normal / Baik" },
                                keterangan = fullSpec,
                                isBorrowable = false,
                                useAutoId = manualLabKomIsAutoId,
                                customId = if (!manualLabKomIsAutoId) finalId else null,
                                onSuccess = {
                                    val newItem = viewModel.itemsWithStock.value.find { 
                                        it.namaBarang.equals(manualNama.trim(), ignoreCase = true)
                                    }
                                    val newId = newItem?.idBarang ?: finalId
                                    triggerQrCodeGeneration(newId, manualNama.trim())

                                    // Clear fields
                                    manualNama = ""
                                    manualStok = "1"
                                    manualMerek = ""
                                    manualLabKomSn = ""
                                    manualLabKomSpec = ""
                                    manualKeterangan = ""
                                    manualLabKomCustomId = ""
                                },
                                onError = { error ->
                                    Toast.makeText(context, "Pendaftaran Gagal: $error", Toast.LENGTH_LONG).show()
                                }
                            )
                        } else if (isPeripheralSelected) {
                            val finalId = if (manualPeripheralIsAutoId || manualPeripheralCustomId.isBlank()) {
                                "PER-${java.text.SimpleDateFormat("yyyyMMddHHmmss", java.util.Locale.US).format(java.util.Date())}"
                            } else {
                                manualPeripheralCustomId.trim()
                            }

                            viewModel.insertPeripheralStock(
                                idBarang = finalId,
                                jenisPeripheral = manualPeripheralCat,
                                namaItem = manualNama.trim(),
                                merek = manualMerek.trim(),
                                spesifikasi = manualKeterangan.trim(),
                                satuan = manualSatuan.trim().ifEmpty { "Unit" },
                                jumlah = stockInt,
                                sumberDana = manualSumberDana.trim().ifEmpty { "BOS / Sekolah" },
                                lokasiRuang = manualRuang.trim().ifEmpty { "Lab Komputer 1" },
                                kondisi = manualKondisi.trim().ifEmpty { "Baik" },
                                serialNumber = manualPeripheralSn.trim(),
                                onSuccess = {
                                    triggerQrCodeGeneration(finalId, manualNama.trim())

                                    // Clear fields
                                    manualNama = ""
                                    manualStok = "1"
                                    manualMerek = ""
                                    manualPeripheralSn = ""
                                    manualKeterangan = ""
                                    manualBorrowable = false
                                    manualPeripheralCustomId = ""
                                },
                                onError = { error ->
                                    Toast.makeText(context, "Pendaftaran Gagal: $error", Toast.LENGTH_LONG).show()
                                }
                            )
                        } else if (isBahanSelected) {
                            val finalBahanId = if (!manualBahanIsAutoId && manualBahanCustomId.isNotBlank()) manualBahanCustomId.trim() else null
                            viewModel.insertBahan(
                                name = manualNama.trim(),
                                serialNumber = manualBahanSn.trim(),
                                stokAwal = stockInt,
                                kategori = manualKategori.trim().ifEmpty { "Logistik" },
                                satuan = manualSatuan.trim(),
                                merekAlat = manualMerek.trim(),
                                ruang = manualRuang.trim(),
                                sumberDana = manualSumberDana.trim(),
                                kondisi = manualKondisi.trim().ifEmpty { "Normal / Baik" },
                                keterangan = manualKeterangan.trim(),
                                isBorrowable = false,
                                useAutoId = manualBahanIsAutoId,
                                customId = finalBahanId,
                                onSuccess = {
                                    val newItem = viewModel.itemsWithStock.value.find { 
                                        it.namaBarang.equals(manualNama.trim(), ignoreCase = true)
                                    }
                                    val newId = newItem?.idBarang ?: finalBahanId ?: "BHN-NEW"
                                    triggerQrCodeGeneration(newId, manualNama.trim())

                                    // Clear fields
                                    manualNama = ""
                                    manualStok = "1"
                                    manualMerek = ""
                                    manualBahanSn = ""
                                    manualKeterangan = ""
                                    manualBorrowable = false
                                    manualBahanCustomId = ""
                                },
                                onError = { error ->
                                    Toast.makeText(context, "Pendaftaran Gagal: $error", Toast.LENGTH_LONG).show()
                                }
                            )
                        } else {
                            val finalAlatId = if (!manualAlatIsAutoId && manualAlatCustomId.isNotBlank()) manualAlatCustomId.trim() else null
                            viewModel.registerNewItem(
                                name = manualNama.trim(),
                                serialNumber = manualAlatSn.trim(),
                                stokAwal = stockInt,
                                kategori = manualKategori.trim(),
                                satuan = manualSatuan.trim(),
                                merekAlat = manualMerek.trim(),
                                ruang = manualRuang.trim(),
                                sumberDana = manualSumberDana.trim(),
                                kondisi = manualKondisi.trim().ifEmpty { "Normal / Baik" },
                                keterangan = manualKeterangan.trim(),
                                isBorrowable = manualBorrowable,
                                useAutoId = manualAlatIsAutoId,
                                customId = finalAlatId,
                                onSuccess = {
                                    val newItem = viewModel.itemsWithStock.value.find { 
                                        it.namaBarang.equals(manualNama.trim(), ignoreCase = true)
                                    }
                                    val newId = newItem?.idBarang ?: finalAlatId ?: "BRG-NEW"
                                    triggerQrCodeGeneration(newId, manualNama.trim())

                                    // Clear fields
                                    manualNama = ""
                                    manualStok = "1"
                                    manualMerek = ""
                                    manualAlatSn = ""
                                    manualKeterangan = ""
                                    manualBorrowable = false
                                    manualAlatCustomId = ""
                                },
                                onError = { error ->
                                    Toast.makeText(context, "Pendaftaran Gagal: $error", Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (selectedFormType) {
                            1 -> Color(0xFF059669)
                            2 -> Color(0xFFD97706)
                            3 -> Color(0xFF7C3AED)
                            else -> Color(0xFF2563EB)
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_save_and_generate_qr")
                ) {
                    Icon(imageVector = Icons.Default.AppRegistration, contentDescription = "Daftar")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (selectedFormType) {
                            1 -> "Simpan Bahan & Buat QR"
                            2 -> "Simpan Peripheral & Buat QR"
                            3 -> "Simpan PC Lab & Buat QR"
                            else -> "Simpan Alat & Buat QR"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Display generated QR code after saving manual item
        if (generatedBitmap != null) {
            LunarisCard(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "KODE QR BARANG BARU DIBUAT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF15803D),
                        letterSpacing = 1.sp
                    )

                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .background(Color.White, RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        Image(
                            bitmap = generatedBitmap!!.asImageBitmap(),
                            contentDescription = "Hasil QR Code Manual",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = generatedItemName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Kode ID: $generatedItemId",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = { saveQrToGallery(generatedBitmap!!, generatedItemId, generatedItemName) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_simpan_qr_galeri")
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = "Simpan")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simpan ke Galeri", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
