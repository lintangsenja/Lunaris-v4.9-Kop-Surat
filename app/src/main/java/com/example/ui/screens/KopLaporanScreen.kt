package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.entity.KopLaporanEntity
import com.example.data.entity.RecentKopEntity
import com.example.data.entity.DEFAULT_KOP_ROW_ORDER
import com.example.data.entity.DEFAULT_KOP_FONT_FAMILY
import com.example.data.entity.DEFAULT_TTD_FONT_FAMILY
import com.example.data.entity.DEFAULT_TTD_FONT_SIZE
import com.example.data.entity.DEFAULT_JABATAN_OPTIONS
import com.example.data.entity.TtdSignerItem
import com.example.data.entity.getDefaultTtdSigners
import com.example.data.entity.parseKopRowOrder
import com.example.data.entity.parseTtdSigners
import com.example.data.entity.serializeTtdSigners
import com.example.ui.viewmodel.InventoryViewModel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KopLaporanScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val kopState by viewModel.kopLaporan.collectAsState()
    val recentKopList by viewModel.recentKopList.collectAsState()

    // Form text fields
    var pemprovHeader by remember(kopState) { mutableStateOf(kopState.pemprovHeader) }
    var pemprovFontSize by remember(kopState) { mutableIntStateOf(kopState.pemprovFontSize) }

    var dinasHeader by remember(kopState) { mutableStateOf(kopState.dinasHeader) }
    var dinasFontSize by remember(kopState) { mutableIntStateOf(kopState.dinasFontSize) }

    var sekolahBaris1 by remember(kopState) { mutableStateOf(kopState.sekolahBaris1) }
    var sekolahBaris1FontSize by remember(kopState) { mutableIntStateOf(kopState.sekolahBaris1FontSize) }

    var sekolahBaris2 by remember(kopState) { mutableStateOf(kopState.sekolahBaris2) }
    var sekolahBaris2FontSize by remember(kopState) { mutableIntStateOf(kopState.sekolahBaris2FontSize) }

    var alamatBaris1 by remember(kopState) { mutableStateOf(kopState.alamatBaris1) }
    var alamatBaris1FontSize by remember(kopState) { mutableIntStateOf(kopState.alamatBaris1FontSize) }

    var alamatBaris2 by remember(kopState) { mutableStateOf(kopState.alamatBaris2) }
    var alamatBaris2FontSize by remember(kopState) { mutableIntStateOf(kopState.alamatBaris2FontSize) }

    var alamatBaris3 by remember(kopState) { mutableStateOf(kopState.alamatBaris3) }
    var alamatBaris3FontSize by remember(kopState) { mutableIntStateOf(kopState.alamatBaris3FontSize) }

    var lainnyaHeader by remember(kopState) { mutableStateOf(kopState.lainnyaHeader) }
    var lainnyaFontSize by remember(kopState) { mutableIntStateOf(kopState.lainnyaFontSize) }

    // Logo paths
    var logoKiriPath by remember(kopState) { mutableStateOf(kopState.logoKiriPath) }
    var logoKananPath by remember(kopState) { mutableStateOf(kopState.logoKananPath) }

    // Dynamic row order list
    var rowOrderList by remember(kopState) { mutableStateOf<List<String>>(parseKopRowOrder(kopState.rowOrder)) }
    var kopFontFamily by remember(kopState) { mutableStateOf(if (kopState.kopFontFamily.isBlank()) DEFAULT_KOP_FONT_FAMILY else kopState.kopFontFamily) }

    // Active Tab & Permissions
    val canKopLaporan = viewModel.isStudentPermissionGranted("kop_laporan")
    val canKopSurat = viewModel.isStudentPermissionGranted("kop_surat")
    val canFooterTtd = viewModel.isStudentPermissionGranted("footer_ttd")

    var selectedTab by remember { mutableIntStateOf(if (!canKopSurat && canFooterTtd) 1 else 0) }

    LaunchedEffect(canKopSurat, canFooterTtd) {
        if (!canKopSurat && canFooterTtd) {
            selectedTab = 1
        } else if (canKopSurat && !canFooterTtd) {
            selectedTab = 0
        }
    }

    // TTD States
    var tempatTanggal by remember(kopState) { mutableStateOf(kopState.tempatTanggal) }
    var ttdFontFamily by remember(kopState) { mutableStateOf(if (kopState.ttdFontFamily.isBlank()) DEFAULT_TTD_FONT_FAMILY else kopState.ttdFontFamily) }
    var ttdFontSize by remember(kopState) { mutableIntStateOf(if (kopState.ttdFontSize <= 0) DEFAULT_TTD_FONT_SIZE else kopState.ttdFontSize) }
    var signerList by remember(kopState) { mutableStateOf<List<TtdSignerItem>>(parseTtdSigners(kopState.ttdSignersJson)) }

    // Helper functions for dynamic key mapping
    fun getTextForKey(key: String): String = when (key) {
        "pemprov" -> pemprovHeader
        "dinas" -> dinasHeader
        "sekolah1" -> sekolahBaris1
        "sekolah2" -> sekolahBaris2
        "alamat1" -> alamatBaris1
        "alamat2" -> alamatBaris2
        "alamat3" -> alamatBaris3
        "lainnya" -> lainnyaHeader
        else -> ""
    }

    fun setTextForKey(key: String, value: String) {
        when (key) {
            "pemprov" -> pemprovHeader = value
            "dinas" -> dinasHeader = value
            "sekolah1" -> sekolahBaris1 = value
            "sekolah2" -> sekolahBaris2 = value
            "alamat1" -> alamatBaris1 = value
            "alamat2" -> alamatBaris2 = value
            "alamat3" -> alamatBaris3 = value
            "lainnya" -> lainnyaHeader = value
        }
    }

    fun getFontSizeForKey(key: String): Int = when (key) {
        "pemprov" -> pemprovFontSize
        "dinas" -> dinasFontSize
        "sekolah1" -> sekolahBaris1FontSize
        "sekolah2" -> sekolahBaris2FontSize
        "alamat1" -> alamatBaris1FontSize
        "alamat2" -> alamatBaris2FontSize
        "alamat3" -> alamatBaris3FontSize
        "lainnya" -> lainnyaFontSize
        else -> 10
    }

    fun setFontSizeForKey(key: String, size: Int) {
        when (key) {
            "pemprov" -> pemprovFontSize = size
            "dinas" -> dinasFontSize = size
            "sekolah1" -> sekolahBaris1FontSize = size
            "sekolah2" -> sekolahBaris2FontSize = size
            "alamat1" -> alamatBaris1FontSize = size
            "alamat2" -> alamatBaris2FontSize = size
            "alamat3" -> alamatBaris3FontSize = size
            "lainnya" -> lainnyaFontSize = size
        }
    }

    fun getLabelForKey(key: String): String = when (key) {
        "pemprov" -> "a. Pemprov / Instansi Utama"
        "dinas" -> "b. Dinas / Sektor"
        "sekolah1" -> "c. Nama Sekolah (Baris 1)"
        "sekolah2" -> "d. Nama Sekolah (Baris 2)"
        "alamat1" -> "e. Alamat Baris 1"
        "alamat2" -> "f. Alamat Baris 2"
        "alamat3" -> "g. Alamat Baris 3 / Kontak"
        "lainnya" -> "h. Lainnya..."
        else -> key
    }

    fun getPlaceholderForKey(key: String): String = when (key) {
        "pemprov" -> "PEMERINTAH PROVINSI JAWA TENGAH"
        "dinas" -> "DINAS PENDIDIKAN DAN KEBUDAYAAN"
        "sekolah1" -> "SEKOLAH MENENGAH ATAS NEGERI 1 BOBOTSARI"
        "sekolah2" -> "KABUPATEN PURBALINGGA"
        "alamat1" -> "Jalan Raya Bobotsari No. 1, Bobotsari, Purbalingga 53353"
        "alamat2" -> "Telepon (0281) 759021 | Email: sman1bobotsari@yahoo.co.id"
        "alamat3" -> "Website: www.sman1bobotsari.sch.id"
        "lainnya" -> "Nomor Faksimile / Email Tambahan / Catatan Khusus Instansi"
        else -> ""
    }

    fun getFontRangeForKey(key: String): IntRange = when (key) {
        "pemprov", "dinas" -> 10..16
        "sekolah1", "sekolah2" -> 12..20
        "alamat1", "alamat2", "alamat3" -> 8..12
        "lainnya" -> 8..16
        else -> 8..16
    }

    // Dialog state for recent kop
    var showRecentKopDialog by remember { mutableStateOf(false) }

    // Image Picker & Crop States
    var selectedImageUriForCrop by remember { mutableStateOf<Uri?>(null) }
    var cropTargetSlot by remember { mutableStateOf<String?>(null) } // "kiri" or "kanan"

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && cropTargetSlot != null) {
            selectedImageUriForCrop = uri
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Pengaturan Kop Laporan",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF1E1B4B)
                        )
                        Text(
                            text = "Format header & logo cetak kedinasan",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF6B7280)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("kop_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color(0xFF1E1B4B)
                        )
                    }
                },
                actions = {
                    // Recent Kop Button
                    OutlinedButton(
                        onClick = { showRecentKopDialog = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF7C3AED)
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("kop_recent_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Riwayat Kop", style = MaterialTheme.typography.labelMedium)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            // Reset form to active saved state
                            pemprovHeader = kopState.pemprovHeader
                            pemprovFontSize = kopState.pemprovFontSize
                            dinasHeader = kopState.dinasHeader
                            dinasFontSize = kopState.dinasFontSize
                            sekolahBaris1 = kopState.sekolahBaris1
                            sekolahBaris1FontSize = kopState.sekolahBaris1FontSize
                            sekolahBaris2 = kopState.sekolahBaris2
                            sekolahBaris2FontSize = kopState.sekolahBaris2FontSize
                            alamatBaris1 = kopState.alamatBaris1
                            alamatBaris1FontSize = kopState.alamatBaris1FontSize
                            alamatBaris2 = kopState.alamatBaris2
                            alamatBaris2FontSize = kopState.alamatBaris2FontSize
                            alamatBaris3 = kopState.alamatBaris3
                            alamatBaris3FontSize = kopState.alamatBaris3FontSize
                            lainnyaHeader = kopState.lainnyaHeader
                            lainnyaFontSize = kopState.lainnyaFontSize
                            logoKiriPath = kopState.logoKiriPath
                            logoKananPath = kopState.logoKananPath
                            rowOrderList = parseKopRowOrder(kopState.rowOrder)
                            kopFontFamily = if (kopState.kopFontFamily.isBlank()) DEFAULT_KOP_FONT_FAMILY else kopState.kopFontFamily
                            tempatTanggal = kopState.tempatTanggal
                            ttdFontFamily = if (kopState.ttdFontFamily.isBlank()) DEFAULT_TTD_FONT_FAMILY else kopState.ttdFontFamily
                            ttdFontSize = if (kopState.ttdFontSize <= 0) DEFAULT_TTD_FONT_SIZE else kopState.ttdFontSize
                            signerList = parseTtdSigners(kopState.ttdSignersJson)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Reset Form")
                    }

                    Button(
                        onClick = {
                            val updatedKop = KopLaporanEntity(
                                id = 1,
                                pemprovHeader = pemprovHeader.trim(),
                                pemprovFontSize = pemprovFontSize,
                                dinasHeader = dinasHeader.trim(),
                                dinasFontSize = dinasFontSize,
                                sekolahBaris1 = sekolahBaris1.trim(),
                                sekolahBaris1FontSize = sekolahBaris1FontSize,
                                sekolahBaris2 = sekolahBaris2.trim(),
                                sekolahBaris2FontSize = sekolahBaris2FontSize,
                                alamatBaris1 = alamatBaris1.trim(),
                                alamatBaris1FontSize = alamatBaris1FontSize,
                                alamatBaris2 = alamatBaris2.trim(),
                                alamatBaris2FontSize = alamatBaris2FontSize,
                                alamatBaris3 = alamatBaris3.trim(),
                                alamatBaris3FontSize = alamatBaris3FontSize,
                                lainnyaHeader = lainnyaHeader.trim(),
                                lainnyaFontSize = lainnyaFontSize,
                                logoKiriPath = logoKiriPath,
                                logoKananPath = logoKananPath,
                                rowOrder = rowOrderList.joinToString(","),
                                kopFontFamily = kopFontFamily,
                                tempatTanggal = tempatTanggal.trim(),
                                ttdFontFamily = ttdFontFamily,
                                ttdFontSize = ttdFontSize,
                                ttdSignersJson = serializeTtdSigners(signerList)
                            )
                            viewModel.saveKopLaporan(updatedKop, saveToHistory = true) {
                                Toast.makeText(
                                    context,
                                    "Konfigurasi Kop & Footer TTD Berhasil Disimpan!",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp)
                            .testTag("save_kop_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6D28D9),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simpan / Perbarui", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // TAB SELECTOR & PERMISSION GUARD
            if (!canKopLaporan || (!canKopSurat && !canFooterTtd)) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Akses Modul Kop Laporan Dibatasi",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF991B1B)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Modul Kop Laporan atau tab di dalamnya dinonaktifkan untuk peran pengguna Anda.",
                            fontSize = 12.sp,
                            color = Color(0xFF7F1D1D)
                        )
                    }
                }
            } else {
                if (canKopSurat && canFooterTtd) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.White,
                        contentColor = Color(0xFF6D28D9),
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = Color(0xFF6D28D9)
                            )
                        },
                        modifier = Modifier.clip(RoundedCornerShape(12.dp))
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("🏛️ Kop Header", fontWeight = FontWeight.Bold)
                                }
                            }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Draw, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("✍️ Footer & TTD", fontWeight = FontWeight.Bold)
                                }
                            }
                        )
                    }
                }

                if (selectedTab == 0 && canKopSurat) {
                // TAB 0: KOP HEADER CONFIGURATION
            // 1. CARD PREVIEW KOP LAPORAN (LIVE VISUAL PREVIEW)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = Color(0xFF6D28D9),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pratinjau Kop Laporan (Live Preview)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E1B4B)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF3E8FF)
                        ) {
                            Text(
                                text = "Times New Roman (Standard)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF6D28D9),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Live Kop Paper Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Logo Kiri
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .background(Color(0xFFF1F5F9), RoundedCornerShape(6.dp))
                                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(6.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (logoKiriPath.isNotBlank() && File(logoKiriPath).exists()) {
                                        AsyncImage(
                                            model = File(logoKiriPath),
                                            contentDescription = "Logo Kiri",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Fit
                                        )
                                    } else {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Default.Image,
                                                contentDescription = null,
                                                tint = Color(0xFF94A3B8),
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Text(
                                                "Logo Kiri",
                                                fontSize = 8.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }
                                }

                                 // Kop Text Lines (Centered according to rowOrder)
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    for (key in rowOrderList) {
                                        val text = getTextForKey(key)
                                        val fontSize = getFontSizeForKey(key)
                                        val isBold = key in listOf("pemprov", "dinas", "sekolah1", "sekolah2")
                                        val isUppercase = key in listOf("pemprov", "dinas", "sekolah1", "sekolah2")
                                        val scaleFactor = if (isBold) 0.8f else 0.75f

                                        if (text.isNotBlank()) {
                                            Text(
                                                text = if (isUppercase) text.uppercase() else text,
                                                fontSize = (fontSize * scaleFactor).sp,
                                                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                                                fontFamily = when (kopFontFamily.uppercase()) {
                                                    "TIMES NEW ROMAN", "SERIF" -> FontFamily.Serif
                                                    "COURIER", "MONOSPACE" -> FontFamily.Monospace
                                                    else -> FontFamily.SansSerif
                                                },
                                                textAlign = TextAlign.Center,
                                                color = Color.Black,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                // Logo Kanan
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .background(Color(0xFFF1F5F9), RoundedCornerShape(6.dp))
                                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(6.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (logoKananPath.isNotBlank() && File(logoKananPath).exists()) {
                                        AsyncImage(
                                            model = File(logoKananPath),
                                            contentDescription = "Logo Kanan",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Fit
                                        )
                                    } else {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Default.Image,
                                                contentDescription = null,
                                                tint = Color(0xFF94A3B8),
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Text(
                                                "Logo Kanan",
                                                fontSize = 8.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Double line separator kedinasan
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(2.5.dp)
                                        .background(Color.Black)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(Color.Black)
                                )
                            }
                        }
                    }
                }
            }

            // MODUL PILIHAN FONT FAMILY KOP SURAT
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.TextFields,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tipografi & Font Kop Surat",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E1B4B)
                        )
                    }

                    Text(
                        text = "Pilih jenis font resmi kedinasan. Pilihan ini diterapkan secara serasi pada Kop Surat dan Footer TTD:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )

                    val fontOptions = listOf("Times New Roman", "Arial", "Courier")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        fontOptions.forEach { font ->
                            val isSelected = kopFontFamily.equals(font, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    kopFontFamily = font
                                    ttdFontFamily = font
                                },
                                label = {
                                    Text(
                                        font,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFE0F2FE),
                                    selectedLabelColor = Color(0xFF0369A1)
                                )
                            )
                        }
                    }
                }
            }

            // 2. MODUL UPLOAD & CROP LOGO (KIRI & KANAN)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Modul Upload & Crop Logo Header",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E1B4B)
                        )
                    }

                    Text(
                        text = "Unggah logo instansi (PNG transparan / JPG). Disediakan fitur zoom & crop interaktif.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Slot 1: Logo Kiri (Provinsi)
                        LogoSlotCard(
                            title = "Logo Kiri (Provinsi)",
                            subtitle = "Posisi kiri kop surat",
                            logoPath = logoKiriPath,
                            onPickImage = {
                                cropTargetSlot = "kiri"
                                imagePickerLauncher.launch("image/*")
                            },
                            onRemove = { logoKiriPath = "" },
                            modifier = Modifier.weight(1f)
                        )

                        // Slot 2: Logo Kanan (Sekolah)
                        LogoSlotCard(
                            title = "Logo Kanan (Sekolah)",
                            subtitle = "Posisi kanan kop surat",
                            logoPath = logoKananPath,
                            onPickImage = {
                                cropTargetSlot = "kanan"
                                imagePickerLauncher.launch("image/*")
                            },
                            onRemove = { logoKananPath = "" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 3. FORM INPUT TEKS, UKURAN FONT & PENGURUTAN DINAMIS (REORDER)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Konfigurasi Teks & Pengurutan Dinamis",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E1B4B)
                            )
                        }

                        // Reset Order Button
                        TextButton(
                            onClick = {
                                rowOrderList = parseKopRowOrder(com.example.data.entity.DEFAULT_KOP_ROW_ORDER)
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Urutan Default", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Text(
                        text = "Gunakan panah ke atas/bawah pada tiap baris untuk mengatur ulang tata letak cetak kop surat.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )

                    for ((index, key) in rowOrderList.withIndex()) {
                        val positionLetter = ('a' + index).toString()
                        KopReorderableRowInputField(
                            positionLetter = positionLetter,
                            index = index,
                            totalCount = rowOrderList.size,
                            label = getLabelForKey(key),
                            textValue = getTextForKey(key),
                            onTextChange = { setTextForKey(key, it) },
                            selectedFontSize = getFontSizeForKey(key),
                            onFontSizeChange = { setFontSizeForKey(key, it) },
                            fontSizeRange = getFontRangeForKey(key),
                            placeholder = getPlaceholderForKey(key),
                            testTagPrefix = key,
                            onMoveUp = {
                                if (index > 0) {
                                    val mutable = rowOrderList.toMutableList()
                                    val temp = mutable[index]
                                    mutable[index] = mutable[index - 1]
                                    mutable[index - 1] = temp
                                    rowOrderList = mutable
                                }
                            },
                            onMoveDown = {
                                if (index < rowOrderList.size - 1) {
                                    val mutable = rowOrderList.toMutableList()
                                    val temp = mutable[index]
                                    mutable[index] = mutable[index + 1]
                                    mutable[index + 1] = temp
                                    rowOrderList = mutable
                                }
                            }
                        )
                    }
                }
            }
            } else if (selectedTab == 1 && canFooterTtd) {
                // TAB 1: FOOTER & TTD CONFIGURATION

                // 1. CARD: FORM TEMPAT DAN TANGGAL
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Place, contentDescription = null, tint = Color(0xFF6D28D9), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Form Tempat dan Tanggal Dokumen", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFF1E1B4B))
                        }
                        Text(
                            text = "Diletakkan secara dinamis mengikuti posisi penandatangan utama/paling kanan. Kondisi awal bersih tanpa data sampel.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                        OutlinedTextField(
                            value = tempatTanggal,
                            onValueChange = { tempatTanggal = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("tempat_tanggal_input"),
                            label = { Text("Tempat dan Tanggal") },
                            placeholder = { Text("Purbalingga, 19 April 2026") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Event, contentDescription = null, tint = Color(0xFF6D28D9)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6D28D9),
                                focusedLabelColor = Color(0xFF6D28D9)
                            )
                        )
                    }
                }

                // 2. CARD: KONTROL TIPOGRAFI (FONT & UKURAN)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.TextFields, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tipografi Footer TTD (Jenis Font & Ukuran)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFF1E1B4B))
                        }

                        Text("Pilihan Jenis Font (Font Family):", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                        val fontOptions = listOf("Times New Roman", "Arial", "Courier")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            fontOptions.forEach { font ->
                                val isSelected = ttdFontFamily.equals(font, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                    ttdFontFamily = font
                                    kopFontFamily = font
                                },
                                    label = { Text(font, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFE0F2FE),
                                        selectedLabelColor = Color(0xFF0369A1)
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Ukuran Font TTD:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                                Text("Rentang 8pt - 16pt (Default 10pt)", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledIconButton(
                                    onClick = { if (ttdFontSize > 8) ttdFontSize-- },
                                    enabled = ttdFontSize > 8,
                                    modifier = Modifier.size(36.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFF1F5F9))
                                ) {
                                    Text("-", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                                ) {
                                    Text(
                                        text = "$ttdFontSize pt",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }

                                FilledIconButton(
                                    onClick = { if (ttdFontSize < 16) ttdFontSize++ },
                                    enabled = ttdFontSize < 16,
                                    modifier = Modifier.size(36.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFF1F5F9))
                                ) {
                                    Text("+", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }

                // 3. CARD: FORM PENANDATANGAN (MULTI-SIGNER & REORDER)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Draw, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Penandatangan (Multi-Signer)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFF1E1B4B))
                            }
                            TextButton(
                                onClick = { signerList = getDefaultTtdSigners() },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset Role Default", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Text(
                            text = "Gunakan tombol panah ke atas/bawah pada tiap kartu penandatangan untuk mengubah urutan penandatangan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )

                        signerList.forEachIndexed { idx, signer ->
                            TtdSignerCardItem(
                                index = idx,
                                totalCount = signerList.size,
                                signer = signer,
                                onSignerChange = { updated ->
                                    val mutable = signerList.toMutableList()
                                    mutable[idx] = updated
                                    signerList = mutable
                                },
                                onMoveUp = {
                                    if (idx > 0) {
                                        val mutable = signerList.toMutableList()
                                        val temp = mutable[idx]
                                        mutable[idx] = mutable[idx - 1]
                                        mutable[idx - 1] = temp
                                        signerList = mutable
                                    }
                                },
                                onMoveDown = {
                                    if (idx < signerList.size - 1) {
                                        val mutable = signerList.toMutableList()
                                        val temp = mutable[idx]
                                        mutable[idx] = mutable[idx + 1]
                                        mutable[idx + 1] = temp
                                        signerList = mutable
                                    }
                                },
                                onDelete = {
                                    if (signerList.size > 1) {
                                        val mutable = signerList.toMutableList()
                                        mutable.removeAt(idx)
                                        signerList = mutable
                                    }
                                }
                            )
                        }

                        Button(
                            onClick = {
                                val mutable = signerList.toMutableList()
                                mutable.add(TtdSignerItem(jabatan = "", nama = "", nip = "", isEnabled = true))
                                signerList = mutable
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tambah Penandatangan")
                        }
                    }
                }

                // 4. CARD: LIVE PREVIEW TTD
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Visibility, contentDescription = null, tint = Color(0xFF6D28D9), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pratinjau Footer TTD (Live Preview)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFF1E1B4B))
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White, RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                .padding(16.dp)
                        ) {
                            val activeSigners = signerList.filter { it.isEnabled }
                            val fontType = when (ttdFontFamily.uppercase()) {
                                "TIMES NEW ROMAN", "SERIF" -> FontFamily.Serif
                                "COURIER", "MONOSPACE" -> FontFamily.Monospace
                                else -> FontFamily.SansSerif
                            }
                            val displayFontSize = (ttdFontSize * 0.9f).sp

                            Column(modifier = Modifier.fillMaxWidth()) {
                                if (tempatTanggal.isNotBlank()) {
                                    Text(
                                        text = tempatTanggal,
                                        fontFamily = fontType,
                                        fontSize = displayFontSize,
                                        color = Color.Black,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.End
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                if (activeSigners.isEmpty()) {
                                    Text(
                                        text = "(Belum ada penandatangan diaktifkan)",
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                } else {
                                    val maxCols = if (activeSigners.size <= 3) activeSigners.size else 2
                                    val rowsSigners = activeSigners.chunked(maxCols)

                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        rowsSigners.forEach { rowList ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp),
                                                horizontalArrangement = if (rowList.size == 1) Arrangement.End else Arrangement.SpaceBetween
                                            ) {
                                                rowList.forEach { s ->
                                                    Column(
                                                        modifier = Modifier.widthIn(min = 120.dp, max = 150.dp),
                                                        horizontalAlignment = Alignment.Start
                                                    ) {
                                                        Text(
                                                            text = s.jabatan.ifBlank { "Jabatan..." },
                                                            fontFamily = fontType,
                                                            fontSize = displayFontSize,
                                                            fontWeight = FontWeight.Normal,
                                                            textAlign = TextAlign.Start
                                                        )
                                                        Spacer(modifier = Modifier.height(36.dp))
                                                        Text(
                                                            text = s.nama.ifBlank { "( Nama Lengkap )" },
                                                            fontFamily = fontType,
                                                            fontSize = displayFontSize,
                                                            fontWeight = FontWeight.Bold,
                                                            textAlign = TextAlign.Start,
                                                            textDecoration = TextDecoration.Underline
                                                        )
                                                        if (s.nip.isNotBlank()) {
                                                            Text(
                                                                text = if (s.nip.uppercase().startsWith("NIP")) s.nip else "NIP. ${s.nip}",
                                                                fontFamily = fontType,
                                                                fontSize = (ttdFontSize * 0.8f).sp,
                                                                color = Color.DarkGray,
                                                                textAlign = TextAlign.Start
                                                            )
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
                }
            }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // INTERACTIVE LOGO CROP DIALOG
    if (selectedImageUriForCrop != null && cropTargetSlot != null) {
        LogoCropDialog(
            context = context,
            imageUri = selectedImageUriForCrop!!,
            slot = cropTargetSlot!!,
            onDismiss = {
                selectedImageUriForCrop = null
                cropTargetSlot = null
            },
            onCropSuccess = { savedPath ->
                if (cropTargetSlot == "kiri") {
                    logoKiriPath = savedPath
                } else if (cropTargetSlot == "kanan") {
                    logoKananPath = savedPath
                }
                selectedImageUriForCrop = null
                cropTargetSlot = null
                Toast.makeText(context, "Logo berhasil dicrop & disimpan!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // RECENT KOP DIALOG
    if (showRecentKopDialog) {
        RecentKopDialog(
            recentKopList = recentKopList,
            onDismiss = { showRecentKopDialog = false },
            onSelectPreset = { preset ->
                pemprovHeader = preset.pemprovHeader
                pemprovFontSize = preset.pemprovFontSize
                dinasHeader = preset.dinasHeader
                dinasFontSize = preset.dinasFontSize
                sekolahBaris1 = preset.sekolahBaris1
                sekolahBaris1FontSize = preset.sekolahBaris1FontSize
                sekolahBaris2 = preset.sekolahBaris2
                sekolahBaris2FontSize = preset.sekolahBaris2FontSize
                alamatBaris1 = preset.alamatBaris1
                alamatBaris1FontSize = preset.alamatBaris1FontSize
                alamatBaris2 = preset.alamatBaris2
                alamatBaris2FontSize = preset.alamatBaris2FontSize
                alamatBaris3 = preset.alamatBaris3
                alamatBaris3FontSize = preset.alamatBaris3FontSize
                lainnyaHeader = preset.lainnyaHeader
                lainnyaFontSize = preset.lainnyaFontSize
                rowOrderList = parseKopRowOrder(preset.rowOrder)
                kopFontFamily = if (preset.kopFontFamily.isBlank()) DEFAULT_KOP_FONT_FAMILY else preset.kopFontFamily
                tempatTanggal = preset.tempatTanggal
                ttdFontFamily = if (preset.ttdFontFamily.isBlank()) DEFAULT_TTD_FONT_FAMILY else preset.ttdFontFamily
                ttdFontSize = if (preset.ttdFontSize <= 0) DEFAULT_TTD_FONT_SIZE else preset.ttdFontSize
                signerList = parseTtdSigners(preset.ttdSignersJson)

                showRecentKopDialog = false
                Toast.makeText(context, "Konfigurasi teks Kop diterapkan!", Toast.LENGTH_SHORT).show()
            },
            onDeletePreset = { id ->
                viewModel.deleteRecentKop(id)
            }
        )
    }
}

// LOGO SLOT CARD COMPONENT
@Composable
private fun LogoSlotCard(
    title: String,
    subtitle: String,
    logoPath: String,
    onPickImage: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E1B4B),
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Container Box fixed size 90dp x 90dp
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(Color.White, RoundedCornerShape(10.dp))
                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (logoPath.isNotBlank() && File(logoPath).exists()) {
                    AsyncImage(
                        model = File(logoPath),
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = onPickImage,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("Pilih / Crop", fontSize = 11.sp)
                }

                if (logoPath.isNotBlank()) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus Logo",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// REORDERABLE ROW INPUT FIELD WITH FONT SIZE DROPDOWN & ARROW CONTROLS
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KopReorderableRowInputField(
    positionLetter: String,
    index: Int,
    totalCount: Int,
    label: String,
    textValue: String,
    onTextChange: (String) -> Unit,
    selectedFontSize: Int,
    onFontSizeChange: (Int) -> Unit,
    fontSizeRange: IntRange,
    placeholder: String,
    testTagPrefix: String,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    var dropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFAFAFA), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Position Badge + Label
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF6D28D9),
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = positionLetter,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Controls: Font Size Dropdown + Reorder Up/Down Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Font Size Dropdown Pill
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF1F5F9),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .menuAnchor()
                            .clickable { dropdownExpanded = true }
                            .testTag("${testTagPrefix}_fontsize_picker")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedFontSize}pt",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6D28D9)
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = Color(0xFF6D28D9),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        fontSizeRange.forEach { size ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "$size pt ${if (size == selectedFontSize) "✓" else ""}",
                                        fontWeight = if (size == selectedFontSize) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    onFontSizeChange(size)
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Move Up Button
                IconButton(
                    onClick = onMoveUp,
                    enabled = index > 0,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("${testTagPrefix}_move_up")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Pindah Ke Atas",
                        tint = if (index > 0) Color(0xFF6D28D9) else Color(0xFFCBD5E1),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Move Down Button
                IconButton(
                    onClick = onMoveDown,
                    enabled = index < totalCount - 1,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("${testTagPrefix}_move_down")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Pindah Ke Bawah",
                        tint = if (index < totalCount - 1) Color(0xFF6D28D9) else Color(0xFFCBD5E1),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        OutlinedTextField(
            value = textValue,
            onValueChange = onTextChange,
            placeholder = { Text(placeholder, fontSize = 13.sp, color = Color(0xFF94A3B8)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("${testTagPrefix}_input"),
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF6D28D9),
                unfocusedBorderColor = Color(0xFFCBD5E1),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )
    }
}

// INTERACTIVE LOGO CROP DIALOG
@Composable
private fun LogoCropDialog(
    context: Context,
    imageUri: Uri,
    slot: String,
    onDismiss: () -> Unit,
    onCropSuccess: (savedPath: String) -> Unit
) {
    val bitmap = remember(imageUri) {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            Log.e("LogoCropDialog", "Failed to load bitmap", e)
            null
        }
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Crop & Sesuaikan Logo ${if (slot == "kiri") "Kiri (Provinsi)" else "Kanan (Sekolah)"}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E1B4B)
                )

                Text(
                    text = "Geser & perbesar gambar di dalam bingkai kotak berukuran tetap di bawah ini.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )

                // Interactive Fixed Square Container Frame (240dp x 240dp)
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                        .border(2.dp, Color(0xFF6D28D9), RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 3.5f)
                                offsetX += pan.x
                                offsetY += pan.y
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Crop Image",
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offsetX,
                                    translationY = offsetY
                                ),
                            contentScale = ContentScale.Fit
                        )
                    }

                    // Overlay grid line
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val stroke = 1.dp.toPx()
                        val color = Color.White.copy(alpha = 0.35f)

                        // 3x3 grid lines
                        drawLine(color, start = androidx.compose.ui.geometry.Offset(w / 3f, 0f), end = androidx.compose.ui.geometry.Offset(w / 3f, h), strokeWidth = stroke)
                        drawLine(color, start = androidx.compose.ui.geometry.Offset(2 * w / 3f, 0f), end = androidx.compose.ui.geometry.Offset(2 * w / 3f, h), strokeWidth = stroke)
                        drawLine(color, start = androidx.compose.ui.geometry.Offset(0f, h / 3f), end = androidx.compose.ui.geometry.Offset(w, h / 3f), strokeWidth = stroke)
                        drawLine(color, start = androidx.compose.ui.geometry.Offset(0f, 2 * h / 3f), end = androidx.compose.ui.geometry.Offset(w, 2 * h / 3f), strokeWidth = stroke)
                    }
                }

                // Zoom Slider
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Perbesar / Zoom", style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = String.format(Locale.getDefault(), "%.1fx", scale),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6D28D9)
                        )
                    }
                    Slider(
                        value = scale,
                        onValueChange = { scale = it },
                        valueRange = 1f..3.5f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF6D28D9),
                            activeTrackColor = Color(0xFF6D28D9)
                        )
                    )
                }

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batal")
                    }

                    Button(
                        onClick = {
                            if (bitmap != null) {
                                val croppedPath = saveCroppedBitmapToStorage(
                                    context = context,
                                    originalBitmap = bitmap,
                                    scale = scale,
                                    offsetX = offsetX,
                                    offsetY = offsetY,
                                    slot = slot
                                )
                                onCropSuccess(croppedPath)
                            }
                        },
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6D28D9))
                    ) {
                        Text("Crop & Gunakan Logo", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// RECENT KOP DIALOG
@Composable
private fun RecentKopDialog(
    recentKopList: List<RecentKopEntity>,
    onDismiss: () -> Unit,
    onSelectPreset: (RecentKopEntity) -> Unit,
    onDeletePreset: (Int) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = Color(0xFF6D28D9)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Riwayat Kop (Recent Kop)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                // Important Note Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFEF3C7), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Riwayat HANYA menyimpan teks dan ukuran font, TIDAK menyimpan riwayat foto/logo untuk mencegah sampah file. Foto logo tetap menggunakan logo yang diunggah saat ini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF92400E)
                        )
                    }
                }

                if (recentKopList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada riwayat Kop Laporan yang tersimpan.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF94A3B8)
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        recentKopList.forEach { item ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF8FAFC),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.sekolahBaris1.ifBlank { item.title.ifBlank { "Kop Tanpa Nama" } },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1E1B4B)
                                        )
                                        if (item.pemprovHeader.isNotBlank()) {
                                            Text(
                                                text = item.pemprovHeader,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                        Text(
                                            text = "Tersimpan: ${dateFormat.format(Date(item.timestamp))}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Button(
                                            onClick = { onSelectPreset(item) },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6D28D9))
                                        ) {
                                            Text("Gunakan", fontSize = 11.sp)
                                        }

                                        IconButton(
                                            onClick = { onDeletePreset(item.id) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Hapus Preset",
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(18.dp)
                                            )
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
}

// HELPER FUNCTION: Renders cropped square bitmap and saves to internal storage
private fun saveCroppedBitmapToStorage(
    context: Context,
    originalBitmap: Bitmap,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    slot: String
): String {
    val targetSize = 300 // Standard fixed target output size 300x300 px
    val croppedBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(croppedBitmap)

    // Calculate source rect scaling
    val srcWidth = originalBitmap.width.toFloat()
    val srcHeight = originalBitmap.height.toFloat()

    val paint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
    }

    canvas.save()
    // Center alignment + transform scaling & offsets
    canvas.translate(targetSize / 2f + offsetX, targetSize / 2f + offsetY)
    canvas.scale(scale, scale)

    // Fit original inside target dimensions
    val fitScale = Math.min(targetSize / srcWidth, targetSize / srcHeight)
    val drawW = srcWidth * fitScale
    val drawH = srcHeight * fitScale

    val destRect = android.graphics.RectF(-drawW / 2f, -drawH / 2f, drawW / 2f, drawH / 2f)
    canvas.drawBitmap(originalBitmap, null, destRect, paint)
    canvas.restore()

    // Save to files directory
    val fileName = "logo_${slot}_kop.png"
    val file = File(context.filesDir, fileName)
    FileOutputStream(file).use { out ->
        croppedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }

    return file.absolutePath
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TtdSignerCardItem(
    index: Int,
    totalCount: Int,
    signer: TtdSignerItem,
    onSignerChange: (TtdSignerItem) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    var expandedJabatanDropdown by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFAFAFA), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (signer.isEnabled) Color(0xFF059669) else Color(0xFF94A3B8),
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                Text(
                    text = signer.jabatan.ifBlank { "Penandatangan ${index + 1}" },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            }

            // Controls: Up/Down Arrows, Enable Switch, Delete
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = index > 0,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Naik",
                        tint = if (index > 0) Color(0xFF6D28D9) else Color(0xFFCBD5E1)
                    )
                }

                IconButton(
                    onClick = onMoveDown,
                    enabled = index < totalCount - 1,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Turun",
                        tint = if (index < totalCount - 1) Color(0xFF6D28D9) else Color(0xFFCBD5E1)
                    )
                }

                Switch(
                    checked = signer.isEnabled,
                    onCheckedChange = { onSignerChange(signer.copy(isEnabled = it)) },
                    modifier = Modifier.scale(0.8f)
                )

                if (totalCount > 1) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Jabatan Dropdown / Custom Input
        val suggestedJabatanList = remember {
            (DEFAULT_JABATAN_OPTIONS + listOf("Laboran", "Koordinator Bengkel", "Koordinator Laboratorium", "Guru Pendamping")).distinct()
        }

        ExposedDropdownMenuBox(
            expanded = expandedJabatanDropdown,
            onExpandedChange = { expandedJabatanDropdown = !expandedJabatanDropdown }
        ) {
            OutlinedTextField(
                value = signer.jabatan,
                onValueChange = { onSignerChange(signer.copy(jabatan = it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                label = { Text("Label / Jabatan Penandatangan (Editable)") },
                placeholder = { Text("Ketik jabatan kustom (misal: Laboran, Koordinator Bengkel...)") },
                singleLine = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedJabatanDropdown) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF059669),
                    focusedLabelColor = Color(0xFF059669)
                )
            )

            ExposedDropdownMenu(
                expanded = expandedJabatanDropdown,
                onDismissRequest = { expandedJabatanDropdown = false }
            ) {
                suggestedJabatanList.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSignerChange(signer.copy(jabatan = option))
                            expandedJabatanDropdown = false
                        }
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                DropdownMenuItem(
                    text = {
                        Text(
                            "+ Ketik Jabatan Kustom Bebas",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF059669)
                        )
                    },
                    onClick = {
                        expandedJabatanDropdown = false
                    }
                )
            }
        }

        // Nama Lengkap Input
        OutlinedTextField(
            value = signer.nama,
            onValueChange = { onSignerChange(signer.copy(nama = it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nama Lengkap Penandatangan") },
            placeholder = { Text("Contoh: Drs. H. Ahmad Fauzi, M.Pd.") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF059669),
                focusedLabelColor = Color(0xFF059669)
            )
        )

        // NIP Input
        OutlinedTextField(
            value = signer.nip,
            onValueChange = { onSignerChange(signer.copy(nip = it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("NIP / NUPTK / Identitas") },
            placeholder = { Text("Contoh: 19750812 200003 1 002") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF059669),
                focusedLabelColor = Color(0xFF059669)
            )
        )
    }
}
