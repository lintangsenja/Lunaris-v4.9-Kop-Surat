package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.LunarisCard
import com.example.ui.theme.DeepPurpleText
import com.example.ui.viewmodel.InventoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = false
    
    // Brand Premium Palette
    val purplePrimary = Color(0xFF8B5CF6) // Purple 500
    val violetSecondary = Color(0xFF6D28D9) // Violet 700
    val cyanAccent = Color(0xFF06B6D4) // Cyan 500
    val emeraldSuccess = Color(0xFF10B981) // Emerald 500
    
    val topBarGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFFF7E0FF), Color(0xFFBAE7FF))
    )
    val cardBgColor = Color(0xFFFFFFFF)
    val appBarContentColor = DeepPurpleText

    var isLoading by remember { mutableStateOf(false) }
    var loadingMessage by remember { mutableStateOf("") }
    var showConfirmRestoreDialog by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val sharedPrefs = remember {
        context.getSharedPreferences("lunaris_backup_prefs", Context.MODE_PRIVATE)
    }

    var lastBackupTime by remember {
        mutableStateOf(sharedPrefs.getString("last_backup_time", "Belum pernah dicadangkan") ?: "Belum pernah dicadangkan")
    }

    var backupHistory by remember {
        mutableStateOf(
            sharedPrefs.getString("backup_history", "")?.split("\n")?.filter { it.isNotEmpty() } ?: emptyList()
        )
    }

    val isCloudConnected by viewModel.isCloudConnected.collectAsState()
    val lastCloudSyncTime by viewModel.lastCloudSyncTime.collectAsState()

    var isManualSyncing by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "sync_rotation")
    val syncRotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sync_rotation_angle"
    )

    // Backup Document Creator Launcher
    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            isLoading = true
            loadingMessage = "Sedang mengekspor database lokal ke file JSON..."
            try {
                val outputStream = context.contentResolver.openOutputStream(uri)
                if (outputStream != null) {
                    viewModel.performBackup(
                        outputStream = outputStream,
                        onSuccess = {
                            isLoading = false
                            val currentTimestamp = SimpleDateFormat("dd MMMM yyyy, HH:mm:ss", Locale("id", "ID")).format(Date())
                            
                            // Save to SharedPreferences
                            sharedPrefs.edit().apply {
                                putString("last_backup_time", currentTimestamp)
                                
                                val updatedHistory = listOf(currentTimestamp) + backupHistory.take(9) // Keep last 10 entries
                                putString("backup_history", updatedHistory.joinToString("\n"))
                                apply()
                            }
                            
                            // Update states real-time
                            lastBackupTime = currentTimestamp
                            backupHistory = listOf(currentTimestamp) + backupHistory.take(9)

                            Toast.makeText(context, "Database berhasil dicadangkan!", Toast.LENGTH_LONG).show()
                        },
                        onError = { err ->
                            isLoading = false
                            Toast.makeText(context, "Gagal mencadangkan database: $err", Toast.LENGTH_LONG).show()
                        }
                    )
                } else {
                    isLoading = false
                    Toast.makeText(context, "Gagal membuka aliran file untuk menulis.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                isLoading = false
                Toast.makeText(context, "Terjadi kesalahan: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Restore Document Picker Launcher
    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingRestoreUri = uri
            showConfirmRestoreDialog = true
        }
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(topBarGradient)
                    .statusBarsPadding()
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Backup & Restore",
                            fontWeight = FontWeight.Bold,
                            color = appBarContentColor,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("btn_back")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali",
                                tint = appBarContentColor
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp), // Spacious 24dp padding
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                
                // =========================================================
                // 1. PANEL STATUS SINKRONISASI CLOUD OTOMATIS (INFORMATIONAL)
                // =========================================================
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_cloud_sync_info"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = cardBgColor),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header Row: Title & Subtitle + Sync Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(purplePrimary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudSync,
                                        contentDescription = "Cloud Sync Status",
                                        tint = purplePrimary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Sinkronisasi Cloud",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DeepPurpleText
                                    )
                                    Text(
                                        text = "Firebase Firestore (Otomatis)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            // Tombol Sync Manual (Sinkronisasi Instan)
                            Button(
                                onClick = {
                                    if (!isManualSyncing) {
                                        isManualSyncing = true
                                        viewModel.triggerInstantCloudSync(
                                            onSuccess = { msg ->
                                                isManualSyncing = false
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            },
                                            onError = { err ->
                                                isManualSyncing = false
                                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                },
                                enabled = !isManualSyncing,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF7C3AED),
                                    contentColor = Color.White,
                                    disabledContainerColor = Color(0xFF7C3AED).copy(alpha = 0.7f),
                                    disabledContentColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp, pressedElevation = 1.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("btn_manual_sync")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudSync,
                                        contentDescription = "Sync Instan",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .then(
                                                if (isManualSyncing) Modifier.graphicsLayer { rotationZ = syncRotationAngle } else Modifier
                                            )
                                    )
                                    Text(
                                        text = if (isManualSyncing) "Syncing..." else "Sync",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Connection Status Badge
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isCloudConnected) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                            border = BorderStroke(1.dp, if (isCloudConnected) Color(0xFF86EFAC) else Color(0xFFFCA5A5)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (isCloudConnected) Color(0xFF16A34A) else Color(0xFFDC2626))
                                )
                                Text(
                                    text = if (isCloudConnected) "Terhubung ke Firebase (Sinkronisasi Otomatis)" else "Terputus dari Firebase",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCloudConnected) Color(0xFF15803D) else Color(0xFFB91C1C)
                                )
                            }
                        }

                        // Last Sync Timestamp & Description Box
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFF8FAFC))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val formattedTime = if (lastCloudSyncTime.isNotBlank() && lastCloudSyncTime != "Belum Pernah") {
                                lastCloudSyncTime
                            } else {
                                "Hari ini, ${SimpleDateFormat("HH:mm 'WIB'", Locale("id", "ID")).format(Date())}"
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = purplePrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Terakhir Sinkron: $formattedTime",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepPurpleText
                                )
                            }

                            HorizontalDivider(color = Color(0xFFE2E8F0))

                            Text(
                                text = "• Data transaksi, siswa, dan stok barang terhubung & disinkronkan secara otomatis dan real-time di latar belakang oleh listener Firebase Firestore.",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // ==========================================
                // 2. BAGIAN BACKUP & RESTORE MANUAL / LOKAL
                // ==========================================
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = cardBgColor),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(purplePrimary.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = "Backup Lokal",
                                tint = purplePrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Backup & Restore Lokal",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = DeepPurpleText,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Pencadangan & Pemulihan Database Perangkat",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Backup Terakhir: $lastBackupTime",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = purplePrimary,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Tombol Aksi Backup & Restore Lokal
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(purplePrimary, violetSecondary)
                                        )
                                    )
                                    .clickable {
                                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                        val fileName = "Lunaris_Backup_$timestamp.json"
                                        backupLauncher.launch(fileName)
                                    }
                                    .testTag("btn_trigger_backup"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = "Backup",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Backup Sekarang",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    restoreLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                                },
                                border = BorderStroke(1.5.dp, purplePrimary),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = purplePrimary
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("btn_trigger_restore")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = "Restore",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Restore", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }

                // Quick Information Row (Stats)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val quickStats = listOf(
                        Triple(
                            "Terakhir", 
                            if (lastBackupTime == "Belum pernah dicadangkan") "—" else lastBackupTime.split(",").lastOrNull()?.trim() ?: "—", 
                            purplePrimary
                        ),
                        Triple(
                            "Log File", 
                            "${backupHistory.size} Riwayat", 
                            emeraldSuccess
                        ),
                        Triple(
                            "Ukuran", 
                            "1.2 MB", 
                            cyanAccent
                        )
                    )

                    quickStats.forEach { (title, value, color) ->
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBgColor),
                            border = BorderStroke(1.dp, color.copy(alpha = 0.15f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(color.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when(title) {
                                            "Terakhir" -> Icons.Default.Update
                                            "Log File" -> Icons.Default.History
                                            else -> Icons.Default.Storage
                                        },
                                        contentDescription = title,
                                        tint = color,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = value,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepPurpleText,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // ==========================================
                // 2. RIWAYAT BACKUP & RESTORE LOKAL
                // ==========================================
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Riwayat",
                            tint = purplePrimary
                        )
                        Text(
                            text = "Riwayat Backup Lokal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = DeepPurpleText
                        )
                    }

                    if (backupHistory.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBgColor.copy(alpha = 0.6f)),
                            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 36.dp, horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(32.dp))
                                        .background(purplePrimary.copy(alpha = 0.08f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Cloud,
                                        contentDescription = "Belum Ada Riwayat",
                                        tint = Color.LightGray,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                Text(
                                    text = "Belum ada riwayat backup lokal.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepPurpleText,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Tekan tombol 'Backup Sekarang' untuk membuat pencadangan pertama.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            backupHistory.forEachIndexed { index, timestamp ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = cardBgColor),
                                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.25f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(purplePrimary.copy(alpha = 0.1f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Storage,
                                                    contentDescription = "Log File",
                                                    tint = purplePrimary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Column {
                                                Text(
                                                    text = timestamp,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = DeepPurpleText
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = "1.2 MB",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color.Gray,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    Text(
                                                        text = "•",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color.Gray
                                                    )
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(emeraldSuccess.copy(alpha = 0.1f))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = "Success",
                                                            tint = emeraldSuccess,
                                                            modifier = Modifier.size(10.dp)
                                                        )
                                                        Text(
                                                            text = "Berhasil",
                                                            color = emeraldSuccess,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.ExtraBold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    shareBackupFile(context, viewModel, timestamp)
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Share,
                                                    contentDescription = "Bagikan",
                                                    tint = purplePrimary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            
                                            IconButton(
                                                onClick = {
                                                    val updatedHistory = backupHistory.filterIndexed { i, _ -> i != index }
                                                    sharedPrefs.edit().apply {
                                                        putString("backup_history", updatedHistory.joinToString("\n"))
                                                        apply()
                                                    }
                                                    backupHistory = updatedHistory
                                                    Toast.makeText(context, "Log riwayat berhasil dihapus", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Hapus Log",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // FLOATING TIPS CARD (FOOTER)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = purplePrimary.copy(alpha = 0.05f)),
                    border = BorderStroke(1.dp, purplePrimary.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(purplePrimary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = "Tips",
                                tint = purplePrimary,
                                modifier = Modifier.size(20.dp)
                             )
                        }
                        Text(
                            text = "Tips: Lakukan backup secara rutin agar data tetap aman.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = DeepPurpleText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // FULLSCREEN LOADING PROGRESS OVERLAY
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(8.dp),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .width(300.dp)
                            .padding(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 4.dp
                            )
                            
                            Text(
                                text = loadingMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            LinearProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }
            }

            // RESTORE CONFIRM DIALOG
            if (showConfirmRestoreDialog && pendingRestoreUri != null) {
                AlertDialog(
                    onDismissRequest = {
                        showConfirmRestoreDialog = false
                        pendingRestoreUri = null
                    },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Peringatan",
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Konfirmasi Pemulihan",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    text = {
                        Text(
                            text = "Apakah Anda yakin ingin memulihkan database dari file cadangan ini?\n\nSemua data lokal saat ini akan DIHAPUS secara permanen dan digantikan oleh isi file cadangan."
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showConfirmRestoreDialog = false
                                isLoading = true
                                loadingMessage = "Sedang memulihkan database secara atomik..."
                                try {
                                    val inputStream = context.contentResolver.openInputStream(pendingRestoreUri!!)
                                    if (inputStream != null) {
                                        viewModel.performRestore(
                                            inputStream = inputStream,
                                            onSuccess = {
                                                isLoading = false
                                                pendingRestoreUri = null
                                                Toast.makeText(context, "Database berhasil dipulihkan secara penuh!", Toast.LENGTH_LONG).show()
                                            },
                                            onError = { err ->
                                                isLoading = false
                                                pendingRestoreUri = null
                                                Toast.makeText(context, "Gagal memulihkan database: $err", Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    } else {
                                        isLoading = false
                                        pendingRestoreUri = null
                                        Toast.makeText(context, "Gagal membuka file backup.", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    isLoading = false
                                    pendingRestoreUri = null
                                    Toast.makeText(context, "Terjadi kesalahan: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.testTag("btn_confirm_restore")
                        ) {
                            Text("Ya, Tindih & Pulihkan", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showConfirmRestoreDialog = false
                                pendingRestoreUri = null
                            }
                        ) {
                            Text("Batal")
                        }
                    }
                )
            }
        }
    }
}

private fun shareBackupFile(
    context: Context,
    viewModel: InventoryViewModel,
    timestamp: String
) {
    try {
        val cleanTimestamp = timestamp.replace(" ", "_").replace(",", "").replace(":", "")
        val fileName = "Lunaris_Backup_$cleanTimestamp.json"
        val cacheFile = java.io.File(context.cacheDir, fileName)
        val outputStream = java.io.FileOutputStream(cacheFile)
        
        viewModel.performBackup(
            outputStream = outputStream,
            onSuccess = {
                try {
                    val fileUri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "com.lintang.lunaris.fileprovider",
                        cacheFile
                    )
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(android.content.Intent.EXTRA_STREAM, fileUri)
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(android.content.Intent.createChooser(intent, "Bagikan File Cadangan"))
                } catch (e: Exception) {
                    Toast.makeText(context, "Gagal membagikan: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            },
            onError = { err ->
                Toast.makeText(context, "Gagal membuat file untuk dibagikan: $err", Toast.LENGTH_SHORT).show()
            }
        )
    } catch (e: Exception) {
        Toast.makeText(context, "Terjadi kesalahan: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}
