package com.example.ui.screens

import com.example.ui.components.CameraScannerDialog
import com.example.ui.components.ScanMode

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraControl
import androidx.compose.foundation.BorderStroke
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.PeripheralStockEntity
import com.example.ui.components.LunarisCard
import com.example.ui.components.LunarisFilterDialog
import com.example.ui.components.LunarisDatePickerDialog
import com.example.ui.components.FilterGroup
import com.example.ui.theme.CarbonBlackText
import com.example.ui.theme.DeepPurpleText
import com.example.ui.viewmodel.InventoryViewModel
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun cleanCategoryText(rawCategory: String): String {
    if (rawCategory.isBlank()) return "Peripheral"
    val cleaned = rawCategory
        .replace(Regex("[\\p{So}\\p{Cn}\\p{Cs}\\p{Extended_Pictographic}]"), "")
        .replace(Regex("^[\\s\\W_]+"), "")
        .trim()
    return if (cleaned.isNotBlank()) cleaned else rawCategory.trim()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StokPeripheralScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPeripheralRusak: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val excelOrCsvLines = readExcelOrCsvInputStream(inputStream)
                    if (excelOrCsvLines.isNotEmpty()) {
                        viewModel.importPeripheralCsvData(
                            csvLines = excelOrCsvLines,
                            onSuccess = { added, updated ->
                                Toast.makeText(context, "Berhasil Impor Data Peripheral! Baru: $added, Update: $updated", Toast.LENGTH_LONG).show()
                            },
                            onError = { err ->
                                Toast.makeText(context, "Error Impor Peripheral: $err", Toast.LENGTH_LONG).show()
                            }
                        )
                    } else {
                        Toast.makeText(context, "File Excel/CSV kosong!", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal membaca file Excel/CSV: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Real-time states from ViewModel
    val peripheralStocks by viewModel.allPeripheralStocks.collectAsState()
    val peripheralRusakList by viewModel.allPeripheralRusak.collectAsState()
    val defaultOfficer by viewModel.defaultOfficer.collectAsState()
    val kondisiList by viewModel.kondisi.collectAsState()
    val ruangList by viewModel.ruang.collectAsState()
    val sumberDanaList by viewModel.sumberDana.collectAsState()
    val merekAlatList by viewModel.merekAlat.collectAsState()
    val allUnits by viewModel.allUnits.collectAsState()

    val userRole by viewModel.userRole.collectAsState()
    val studentPermissions by viewModel.studentPermissions.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.forceRefreshState()
    }
    val canLabKomView = userRole.contains("admin", ignoreCase = true) || viewModel.isStudentPermissionGranted("labkom_view", studentPermissions)
    val canLabKomManage = userRole.contains("admin", ignoreCase = true) || viewModel.isStudentPermissionGranted("labkom_manage", studentPermissions)

    // Master data from settings
    val masterCategories = remember {
        val fromSettings = viewModel.settingsRepository.getPeripheral()
        val defaultList = listOf(
            "RAM",
            "Storage / Media Penyimpanan",
            "Mouse & Keyboard",
            "UPS & PSU",
            "Peripheral Lainnya"
        )
        (defaultList + fromSettings).map { cleanCategoryText(it) }.distinct().filter { it.isNotBlank() }
    }

    val masterMerek = remember(merekAlatList) {
        if (merekAlatList.isNotEmpty()) merekAlatList else {
            val list = viewModel.settingsRepository.getMerekAlat()
            if (list.isNotEmpty()) list else listOf("Corsair", "Kingston", "Logitech", "Samsung", "Adata", "APC", "V-Gen", "Simbadda", "Epson", "ASUS", "HP", "Lenovo")
        }
    }

    val masterRuang = remember(ruangList) {
        ruangList
    }

    val masterSumberDana = remember(sumberDanaList) {
        if (sumberDanaList.isNotEmpty()) sumberDanaList else {
            val list = viewModel.settingsRepository.getSumberDana()
            if (list.isNotEmpty()) list else listOf("BOS Reguler", "BOP Provinsi", "Bantuan Komite Sekolah", "Bantuan Pemda", "Dana Kas Sekolah")
        }
    }

    val masterKondisi = remember(kondisiList) {
        if (kondisiList.isNotEmpty()) kondisiList else listOf("Normal / Baik", "Expired / Afkir", "Rusak", "Pemeliharaan", "Rusak Fisik")
    }

    val masterSatuan = remember(allUnits) {
        val unitNames = allUnits.map { it.name }
        if (unitNames.isNotEmpty()) unitNames else listOf("Unit", "Buah", "Set", "Pcs", "Box")
    }

    // Main Category Tabs (Clean without decorative icons/emojis)
    val tabs = remember {
        listOf(
            "Semua",
            "RAM",
            "Storage / Media Penyimpanan",
            "Mouse & Keyboard",
            "UPS & PSU",
            "Peripheral Lainnya",
            "Riwayat Pemakaian"
        )
    }
    var selectedTabCategory by remember { mutableStateOf("Semua") }

    // Search and Sort Control
    var searchQuery by remember { mutableStateOf("") }
    var showSearchQrScanner by remember { mutableStateOf(false) }
    var sortOption by remember { mutableStateOf("FIFO (Masuk Terlama)") } // "FIFO (Masuk Terlama)", "LIFO (Masuk Terbaru)", "Nama (A-Z)", "Nama (Z-A)", "Jumlah (Banyak-Sedikit)"
    var showSortDialog by remember { mutableStateOf(false) }

    // Dialog Control States
    var showAddDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<PeripheralStockEntity?>(null) }
    var itemToUse by remember { mutableStateOf<PeripheralStockEntity?>(null) }
    var itemToLaporRusak by remember { mutableStateOf<PeripheralStockEntity?>(null) }
    var itemForDetail by remember { mutableStateOf<PeripheralStockEntity?>(null) }
    var showCameraScannerDialog by remember { mutableStateOf(false) }
    var scannerCallback by remember { mutableStateOf<((String) -> Unit)?>(null) }

    var selectedItemIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var showBatchDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showSingleDeleteConfirmDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<PeripheralStockEntity?>(null) }

    // FIFO Sorting & Filtering Logic
    val filteredStocks = remember(
        peripheralStocks,
        selectedTabCategory,
        searchQuery,
        sortOption
    ) {
        var list = peripheralStocks.filter { item ->
            val cleanCat = cleanCategoryText(item.jenisPeripheral)
            val matchCategory = when (selectedTabCategory) {
                "Semua", "Riwayat Pemakaian" -> true
                "RAM" -> cleanCat.contains("RAM", ignoreCase = true)
                "Storage / Media Penyimpanan" -> cleanCat.contains("Storage", ignoreCase = true) || cleanCat.contains("Penyimpanan", ignoreCase = true) || cleanCat.contains("SSD", ignoreCase = true) || cleanCat.contains("HDD", ignoreCase = true) || cleanCat.contains("Flashdisk", ignoreCase = true)
                "Mouse & Keyboard" -> cleanCat.contains("Mouse", ignoreCase = true) || cleanCat.contains("Keyboard", ignoreCase = true)
                "UPS & PSU" -> cleanCat.contains("UPS", ignoreCase = true) || cleanCat.contains("PSU", ignoreCase = true) || cleanCat.contains("Power", ignoreCase = true)
                "Peripheral Lainnya" -> !cleanCat.contains("RAM", ignoreCase = true) && !cleanCat.contains("Storage", ignoreCase = true) && !cleanCat.contains("Penyimpanan", ignoreCase = true) && !cleanCat.contains("SSD", ignoreCase = true) && !cleanCat.contains("HDD", ignoreCase = true) && !cleanCat.contains("Mouse", ignoreCase = true) && !cleanCat.contains("Keyboard", ignoreCase = true) && !cleanCat.contains("UPS", ignoreCase = true) && !cleanCat.contains("PSU", ignoreCase = true)
                else -> cleanCat.equals(selectedTabCategory, ignoreCase = true)
            }

            val matchSearch = searchQuery.isBlank() ||
                    item.namaItem.contains(searchQuery, ignoreCase = true) ||
                    item.idBarang.contains(searchQuery, ignoreCase = true) ||
                    item.merek.contains(searchQuery, ignoreCase = true) ||
                    item.serialNumber.contains(searchQuery, ignoreCase = true) ||
                    item.spesifikasi.contains(searchQuery, ignoreCase = true)

            matchCategory && matchSearch
        }

        // Apply Sorting Rule
        list = when (sortOption) {
            "LIFO (Masuk Terbaru)" -> list.sortedWith(compareByDescending<PeripheralStockEntity> { it.tanggalMasuk }.thenByDescending { it.id })
            "Nama (A-Z)" -> list.sortedBy { it.namaItem.lowercase(Locale.ROOT) }
            "Nama (Z-A)" -> list.sortedByDescending { it.namaItem.lowercase(Locale.ROOT) }
            "Jumlah (Banyak-Sedikit)" -> list.sortedByDescending { it.jumlah }
            else -> list.sortedWith(compareBy<PeripheralStockEntity> { if (it.tanggalMasuk.isNotBlank()) it.tanggalMasuk else "9999-12-31" }.thenBy { it.id })
        }

        list
    }

    // Dashboard Metric Calculations
    val totalStockQty = remember(peripheralStocks) { peripheralStocks.sumOf { it.jumlah } }
    val totalUsedQty = remember(peripheralStocks) { peripheralStocks.sumOf { it.usedCount } }
    val totalAvailableQty = remember(totalStockQty, totalUsedQty) { (totalStockQty - totalUsedQty).coerceAtLeast(0) }
    val totalRusakCount = remember(peripheralRusakList) { peripheralRusakList.size }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Box(
                modifier = Modifier
                    .shadow(
                        elevation = 2.dp,
                        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                    )
                    .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                    .background(
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
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
                            modifier = Modifier.size(40.dp).testTag("btn_back_stok_peripheral")
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
                                text = "Stok Peripheral",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Manajemen alokasi, pemakaian & rotasi hardware periferal",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFF1E293B)
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (canLabKomManage) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.testTag("btn_tambah_stok_peripheral")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Stok")
                }
            }
        }
    ) { paddingValues ->
        if (!canLabKomView && !canLabKomManage) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Akses Terkunci",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Anda tidak memiliki izin untuk mengakses Stok Peripheral. Silakan hubungi Super Admin.",
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                // 1. Kelola Data Massal (Import, Ekspor, Unduh Template Khusus Peripheral)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { csvLauncher.launch("*/*") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_import_csv_peripheral")
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = "Impor", modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Impor", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                val filename = "Data_Peripheral_Lunaris_${System.currentTimeMillis()}.xlsx"
                                val headers = listOf(
                                    "id_barang", "jenis_peripheral", "nama_item", "merek", "spesifikasi",
                                    "satuan", "jumlah", "tanggal_masuk", "sumber_dana", "lokasi_ruang", "kondisi", "serial_number"
                                )
                                val rows = peripheralStocks.map { item ->
                                    listOf(
                                        item.idBarang,
                                        item.jenisPeripheral,
                                        item.namaItem,
                                        item.merek,
                                        item.spesifikasi,
                                        item.satuan,
                                        item.jumlah.toString(),
                                        item.tanggalMasuk,
                                        item.sumberDana,
                                        item.lokasiRuang,
                                        item.kondisi,
                                        item.serialNumber
                                    )
                                }
                                val bytes = generateExcelBytes(
                                    title = "Data Stok Peripheral Lunaris",
                                    headers = headers,
                                    rows = rows
                                )
                                withContext(Dispatchers.Main) {
                                    saveFileToDownloads(
                                        context = context,
                                        filename = filename,
                                        mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                        bytes = bytes
                                    ) {
                                        Toast.makeText(context, "Data Peripheral berhasil diekspor ke format Excel (.xlsx)!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_export_csv_peripheral")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Ekspor", modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ekspor", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = {
                            val templateFilename = "Template_Impor_Peripheral_Lunaris.xlsx"
                            val templateMimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                            val headers = listOf(
                                "id_barang", "jenis_peripheral", "nama_item", "merek", "spesifikasi",
                                "satuan", "jumlah", "tanggal_masuk", "sumber_dana", "lokasi_ruang", "kondisi", "serial_number"
                            )
                            val templateRows = listOf(
                                listOf("PRPH-001", "RAM", "RAM DDR4 16GB V-Gen Tsunami 3200MHz", "V-Gen", "DDR4 16GB PC25600", "Pcs", "10", "2026-01-15", "BOS Reguler", "Lab Komputer 1", "Normal / Baik", "SN-RAM16G-9021"),
                                listOf("PRPH-002", "Storage / Media Penyimpanan", "SSD NVMe 512GB Samsung 980", "Samsung", "M.2 NVMe PCIe 3.0", "Pcs", "8", "2026-02-10", "BOS Kinerja", "Lab Server / NOC", "Normal / Baik", "SN-NVME512-8812"),
                                listOf("PRPH-003", "Mouse & Keyboard", "Keyboard Mech RGB Outemu Blue", "Logitech", "Mechanical Wired USB", "Set", "15", "2026-03-01", "Bantuan Komite Sekolah", "Lab Komputer 2", "Pemeliharaan", "SN-KBMECH-3321"),
                                listOf("PRPH-004", "UPS & PSU", "UPSICA 1200VA 600W LCD", "APC", "1200VA AVR Battery Backup", "Unit", "5", "2026-03-20", "Bantuan Pemda", "Lab Server / NOC", "Rusak Fisik", "SN-UPS1200-5541")
                            )
                            val bytes = generateExcelBytes(
                                title = "Template Impor Data Peripheral Lunaris",
                                headers = headers,
                                rows = templateRows
                            )
                            saveFileToDownloads(
                                context = context,
                                filename = templateFilename,
                                mimeType = templateMimeType,
                                bytes = bytes
                            ) {
                                Toast.makeText(context, "Template Excel (.xlsx) Peripheral berhasil diunduh!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD97706)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_unduh_template_csv_peripheral")
                    ) {
                        Icon(Icons.Default.Description, contentDescription = "Template", modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Template", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                    }
                }

                Spacer(Modifier.height(6.dp))

                // 4. Search and Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Ketik untuk mencari...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                    }
                                }
                                IconButton(onClick = { showSearchQrScanner = true }) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = "Scan QR",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("input_search_stok_peripheral"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    if (showSearchQrScanner) {
                        SearchQrScanDialog(
                            onDismiss = { showSearchQrScanner = false },
                            onQrScanned = { scannedCode ->
                                showSearchQrScanner = false
                                searchQuery = scannedCode
                            }
                        )
                    }

                    val isFilterActive = selectedTabCategory != "Semua" || sortOption != "FIFO (Masuk Terlama)"
                    FilledTonalIconButton(
                        onClick = { showSortDialog = true },
                        modifier = Modifier
                            .size(50.dp)
                            .testTag("btn_filter_sort_stok_peripheral"),
                        shape = RoundedCornerShape(12.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (isFilterActive) 
                                MaterialTheme.colorScheme.primaryContainer 
                            else 
                                MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = if (isFilterActive) 
                                MaterialTheme.colorScheme.onPrimaryContainer 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter Kategori & Urutkan Stok",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                val isFilterActive = selectedTabCategory != "Semua" || sortOption != "FIFO (Masuk Terlama)"
                if (isFilterActive) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedTabCategory != "Semua") {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF3E8FF),
                                border = BorderStroke(1.dp, Color(0xFF7C3AED))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Kategori: $selectedTabCategory",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF7C3AED)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Hapus Filter",
                                        tint = Color(0xFF7C3AED),
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { selectedTabCategory = "Semua" }
                                    )
                                }
                            }
                        }
                        if (sortOption != "FIFO (Masuk Terlama)") {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF3E8FF),
                                border = BorderStroke(1.dp, Color(0xFF7C3AED))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Urut: $sortOption",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF7C3AED)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Hapus Urutan",
                                        tint = Color(0xFF7C3AED),
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { sortOption = "FIFO (Masuk Terlama)" }
                                    )
                                }
                            }
                        }
                        TextButton(
                            onClick = {
                                selectedTabCategory = "Semua"
                                sortOption = "FIFO (Masuk Terlama)"
                            },
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text("Reset", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // 5. Main List View or Usage History Tab View
                if (selectedTabCategory == "Riwayat Pemakaian") {
                    val usedItems = remember(peripheralStocks) { peripheralStocks.filter { it.usedCount > 0 } }
                    if (usedItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Belum Ada Riwayat Pemakaian Peripheral",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.outline)
                                )
                                Text(
                                    text = "Semua stok peripheral masih utuh berada di ruang penyimpanan.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.outline)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(usedItems, key = { "used_${it.id}" }) { item ->
                                LunarisCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFFFFFF)),
                                    border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f) else Color(0xFFCBD5E1)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                cleanCategoryText(item.jenisPeripheral),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            )
                                            Text(
                                                "Terpasang: ${item.usedCount} ${item.satuan}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFD97706)
                                                )
                                            )
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            item.namaItem,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            "ID: ${item.idBarang} | Sisa Stok: ${item.jumlah - item.usedCount} ${item.satuan}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (filteredStocks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Inbox,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Tidak ada stok peripheral ditemukan",
                                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.outline)
                            )
                        }
                    }
                } else {
                    // Multi-select header bar
                    if (canLabKomManage && filteredStocks.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isAllSelected = filteredStocks.isNotEmpty() && selectedItemIds.size == filteredStocks.size
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    selectedItemIds = if (isAllSelected) emptySet() else filteredStocks.map { it.id }.toSet()
                                }
                            ) {
                                Checkbox(
                                    checked = isAllSelected,
                                    onCheckedChange = { checked ->
                                        selectedItemIds = if (checked) filteredStocks.map { it.id }.toSet() else emptySet()
                                    },
                                    modifier = Modifier.testTag("checkbox_select_all_peripheral")
                                )
                                Text(
                                    text = if (isAllSelected) "Batal Pilih Semua" else "Pilih Semua (${filteredStocks.size})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (selectedItemIds.isNotEmpty()) {
                                Button(
                                    onClick = { showBatchDeleteConfirmDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("btn_hapus_terpilih_peripheral")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Hapus Terpilih (${selectedItemIds.size})", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(filteredStocks, key = { it.id }) { stock ->
                            val isSelected = selectedItemIds.contains(stock.id)
                            PeripheralStockCardItem(
                                item = stock,
                                isSelected = isSelected,
                                onSelectChange = if (canLabKomManage) { { checked ->
                                    selectedItemIds = if (checked) selectedItemIds + stock.id else selectedItemIds - stock.id
                                } } else null,
                                onUse = { itemToUse = stock },
                                onEdit = { itemToEdit = stock },
                                onLaporRusak = { itemToLaporRusak = stock },
                                onDelete = if (canLabKomManage) { {
                                    itemToDelete = stock
                                    showSingleDeleteConfirmDialog = true
                                } } else null,
                                onDetail = { itemForDetail = stock }
                            )
                        }
                    }
                }
            }
        }
    }

    // DIALOGS
    if (showSortDialog) {
        val categoriesList = listOf(
            "Semua",
            "RAM",
            "Storage / Media Penyimpanan",
            "Mouse & Keyboard",
            "UPS & PSU",
            "Peripheral Lainnya",
            "Riwayat Pemakaian"
        )
        val sortOptionsList = listOf(
            Triple("FIFO (Masuk Terlama)", "Terlama / FIFO", "Masuk Terlama -> Terbaru (Prioritas rotasi)"),
            Triple("LIFO (Masuk Terbaru)", "Terbaru / LIFO", "Masuk Terbaru -> Terlama"),
            Triple("Nama (A-Z)", "Nama A-Z", "Abjad nama item dari A ke Z"),
            Triple("Nama (Z-A)", "Nama Z-A", "Abjad nama item dari Z ke A"),
            Triple("Jumlah (Banyak-Sedikit)", "Kuantitas Stok", "Jumlah unit terbanyak ke tersedikit")
        )
        var tempCategory by remember { mutableStateOf(selectedTabCategory) }
        var tempSort by remember { mutableStateOf(sortOption) }

        val sortMap = mapOf(
            "FIFO (Terlama)" to "FIFO (Masuk Terlama)",
            "LIFO (Terbaru)" to "LIFO (Masuk Terbaru)",
            "Nama (A-Z)" to "Nama (A-Z)",
            "Nama (Z-A)" to "Nama (Z-A)",
            "Stok Terbanyak" to "Jumlah (Banyak-Sedikit)"
        )
        val reverseSortMap = sortMap.entries.associate { (k, v) -> v to k }

        LunarisFilterDialog(
            onDismissRequest = { showSortDialog = false },
            filterGroups = listOf(
                FilterGroup(
                    title = "Kategori Peripheral",
                    options = categoriesList,
                    selectedOption = tempCategory,
                    onOptionSelected = { tempCategory = it }
                ),
                FilterGroup(
                    title = "Urutan Data (Sorting)",
                    options = sortMap.keys.toList(),
                    selectedOption = reverseSortMap[tempSort] ?: "FIFO (Terlama)",
                    onOptionSelected = { chosen ->
                        tempSort = sortMap[chosen] ?: "FIFO (Masuk Terlama)"
                    }
                )
            ),
            onReset = {
                tempCategory = "Semua"
                tempSort = "FIFO (Masuk Terlama)"
                selectedTabCategory = "Semua"
                sortOption = "FIFO (Masuk Terlama)"
                showSortDialog = false
            },
            onApply = {
                selectedTabCategory = tempCategory
                sortOption = tempSort
                showSortDialog = false
            }
        )
    }

    if (showAddDialog) {
        PeripheralStockFormDialog(
            initialItem = null,
            masterCategories = masterCategories,
            masterMerek = masterMerek,
            masterRuang = masterRuang,
            masterSumberDana = masterSumberDana,
            masterKondisi = masterKondisi,
            masterSatuan = masterSatuan,
            defaultOfficer = defaultOfficer,
            onDismiss = { showAddDialog = false },
            onScanBarcodeClick = { callback ->
                scannerCallback = callback
                showCameraScannerDialog = true
            },
            onSave = { stockData ->
                viewModel.insertPeripheralStock(
                    idBarang = stockData.idBarang,
                    jenisPeripheral = stockData.jenisPeripheral,
                    namaItem = stockData.namaItem,
                    merek = stockData.merek,
                    spesifikasi = stockData.spesifikasi,
                    satuan = stockData.satuan,
                    jumlah = stockData.jumlah,
                    tanggalMasuk = stockData.tanggalMasuk,
                    sumberDana = stockData.sumberDana,
                    lokasiRuang = stockData.lokasiRuang,
                    kondisi = stockData.kondisi,
                    serialNumber = stockData.serialNumber,
                    onSuccess = {
                        Toast.makeText(context, "Stok peripheral berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                        showAddDialog = false
                    },
                    onError = { err ->
                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
    }

    if (itemToEdit != null) {
        PeripheralStockFormDialog(
            initialItem = itemToEdit,
            masterCategories = masterCategories,
            masterMerek = masterMerek,
            masterRuang = masterRuang,
            masterSumberDana = masterSumberDana,
            masterKondisi = masterKondisi,
            masterSatuan = masterSatuan,
            defaultOfficer = defaultOfficer,
            onDismiss = { itemToEdit = null },
            onScanBarcodeClick = { callback ->
                scannerCallback = callback
                showCameraScannerDialog = true
            },
            onSave = { stockData ->
                viewModel.updatePeripheralStock(
                    stock = stockData,
                    onSuccess = {
                        Toast.makeText(context, "Data peripheral berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                        itemToEdit = null
                    },
                    onError = { err ->
                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
    }

    if (itemToUse != null) {
        CatatPemakaianPeripheralDialog(
            item = itemToUse!!,
            allUnits = allUnits,
            masterRuang = masterRuang,
            defaultOfficer = defaultOfficer,
            onDismiss = { itemToUse = null },
            onConfirmUse = { targetPc, countUsed, notes ->
                viewModel.usePeripheralStock(
                    id = itemToUse!!.id,
                    useQty = countUsed,
                    targetPc = targetPc,
                    officerName = defaultOfficer,
                    onSuccess = {
                        Toast.makeText(context, "Pemakaian peripheral pada $targetPc berhasil dicatat!", Toast.LENGTH_SHORT).show()
                        itemToUse = null
                    },
                    onError = { err ->
                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
    }

    if (itemToLaporRusak != null) {
        LaporRusakPeripheralDialog(
            item = itemToLaporRusak!!,
            defaultOfficer = defaultOfficer,
            masterSatuan = masterSatuan,
            onDismiss = { itemToLaporRusak = null },
            onScanBarcodeClick = { callback ->
                scannerCallback = callback
                showCameraScannerDialog = true
            },
            onConfirmLaporRusak = { customId, moveQty, satuan, officer, date, serialNumber, reason ->
                viewModel.movePeripheralStockToRusak(
                    stock = itemToLaporRusak!!,
                    moveQty = moveQty,
                    reason = reason,
                    officerName = officer,
                    customIdBarang = customId,
                    customDate = date,
                    serialNumber = serialNumber,
                    satuan = satuan,
                    onSuccess = {
                        Toast.makeText(context, "Item berhasil dipindahkan ke List Peripheral Rusak!", Toast.LENGTH_SHORT).show()
                        itemToLaporRusak = null
                    },
                    onError = { err ->
                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
    }

    if (showCameraScannerDialog) {
        StokPeripheralCameraScanDialog(
            onDismiss = { showCameraScannerDialog = false },
            onCodeScanned = { scannedCode ->
                scannerCallback?.invoke(scannedCode)
                Toast.makeText(context, "Berhasil memindai Serial Number: $scannedCode", Toast.LENGTH_SHORT).show()
                showCameraScannerDialog = false
            }
        )
    }

    // Detail Peripheral Pop-up Dialog (Read-Only Audit Card)
    itemForDetail?.let { detail ->
        val isDark = androidx.compose.foundation.isSystemInDarkTheme()
        val availableQty = (detail.jumlah - detail.usedCount).coerceAtLeast(0)
        
        AlertDialog(
            onDismissRequest = { itemForDetail = null },
            shape = RoundedCornerShape(16.dp),
            containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text(detail.namaItem, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("ID: ${detail.idBarang} | ${cleanCategoryText(detail.jenisPeripheral)}", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Availability Status Banner
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (availableQty > 0) Color(0xFFDCFCE7) else Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, if (availableQty > 0) Color(0xFF10B981) else Color(0xFFFCA5A5))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (availableQty > 0) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (availableQty > 0) Color(0xFF15803D) else Color(0xFFDC2626),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (availableQty > 0) "Normal / Tersedia Stok" else "Stok Habis / Terpasang Semua",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (availableQty > 0) Color(0xFF15803D) else Color(0xFFDC2626)
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Sisa Stok: $availableQty ${detail.satuan} | Terpasang: ${detail.usedCount} ${detail.satuan} | Total: ${detail.jumlah} ${detail.satuan}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (availableQty > 0) Color(0xFF15803D) else Color(0xFFDC2626)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Detail Specs
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Informasi Detail Peripheral:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        if (detail.merek.isNotBlank()) Text("• Merek / Brand: ${detail.merek}", fontSize = 12.sp)
                        if (detail.serialNumber.isNotBlank()) Text("• Serial Number: ${detail.serialNumber}", fontSize = 12.sp)
                        if (detail.spesifikasi.isNotBlank()) Text("• Spesifikasi: ${detail.spesifikasi}", fontSize = 12.sp)
                        if (detail.lokasiRuang.isNotBlank()) Text("• Lokasi Ruang: ${detail.lokasiRuang}", fontSize = 12.sp)
                        if (detail.tanggalMasuk.isNotBlank()) Text("• Tanggal Masuk Stok: ${detail.tanggalMasuk}", fontSize = 12.sp)
                        Text("• Kondisi: ${detail.kondisi}", fontSize = 12.sp)
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Audit Trail Read-Only
                    Text("Riwayat Jejak Audit & Alokasi (Read-Only):", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("• Audit Opname Kartu:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                                Text("Terverifikasi Valid", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("• Terpasang di Unit Lab:", fontSize = 11.sp, color = Color(0xFF64748B))
                                Text("${detail.usedCount} ${detail.satuan} Unit", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1E293B))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("• Status Transaksi Stok:", fontSize = 11.sp, color = Color(0xFF64748B))
                                Text("Pencatatan Otomatis FIFO", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1E293B))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { itemForDetail = null }) {
                    Text("Tutup", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Dialog Konfirmasi Hapus Single Peripheral
    if (showSingleDeleteConfirmDialog && itemToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showSingleDeleteConfirmDialog = false
                itemToDelete = null
            },
            title = { Text("Hapus Stok Peripheral", fontWeight = FontWeight.Bold) },
            text = {
                Text("Apakah Anda yakin ingin menghapus peripheral '${itemToDelete!!.namaItem}' (ID: ${itemToDelete!!.idBarang}) secara permanen dari database lokal dan Firestore? Fitur ini diperuntukkan khusus bagi koreksi data input yang keliru.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val currentItem = itemToDelete!!
                        viewModel.deletePeripheralStock(
                            id = currentItem.id,
                            onSuccess = {
                                Toast.makeText(context, "Stok peripheral berhasil dihapus!", Toast.LENGTH_SHORT).show()
                                showSingleDeleteConfirmDialog = false
                                itemToDelete = null
                            },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                showSingleDeleteConfirmDialog = false
                                itemToDelete = null
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("dialog_btn_confirm_delete_peripheral")
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSingleDeleteConfirmDialog = false
                    itemToDelete = null
                }) {
                    Text("Batal")
                }
            }
        )
    }

    // Dialog Konfirmasi Hapus Massal (Batch Delete Peripheral)
    if (showBatchDeleteConfirmDialog && selectedItemIds.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteConfirmDialog = false },
            title = { Text("Konfirmasi Hapus ${selectedItemIds.size} Data Peripheral", fontWeight = FontWeight.Bold) },
            text = {
                Text("Apakah Anda yakin ingin menghapus ${selectedItemIds.size} data peripheral terpilih secara permanen dari database lokal dan Firestore? Fitur ini diperuntukkan khusus bagi koreksi data input yang keliru (bulk import) dan tindakan ini tidak dapat dibatalkan.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val idsToDelete = selectedItemIds.toList()
                        var deletedCount = 0
                        idsToDelete.forEach { id ->
                            viewModel.deletePeripheralStock(
                                id = id,
                                onSuccess = { deletedCount++ },
                                onError = {}
                            )
                        }
                        Toast.makeText(context, "Berhasil menghapus $deletedCount data peripheral!", Toast.LENGTH_SHORT).show()
                        selectedItemIds = emptySet()
                        showBatchDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    modifier = Modifier.testTag("dialog_btn_confirm_batch_delete_peripheral")
                ) {
                    Text("Hapus Permanen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteConfirmDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

// PERIPHERAL CARD ITEM COMPOSABLE
@Composable
private fun PeripheralStockCardItem(
    item: PeripheralStockEntity,
    onUse: () -> Unit,
    onEdit: () -> Unit,
    onLaporRusak: () -> Unit,
    onDelete: (() -> Unit)? = null,
    isSelected: Boolean = false,
    onSelectChange: ((Boolean) -> Unit)? = null,
    onDetail: () -> Unit = {}
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val availableQty = (item.jumlah - item.usedCount).coerceAtLeast(0)
    val cleanCat = cleanCategoryText(item.jenisPeripheral)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onDetail() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFFF3E8FF) else if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFFFFFF)),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) Color(0xFF7C3AED) else if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f) else Color(0xFFCBD5E1)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (onSelectChange != null) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = onSelectChange,
                            modifier = Modifier.testTag("checkbox_peripheral_${item.id}")
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = cleanCat,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Surface(
                        color = Color(0xFFDCFCE7),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = item.kondisi,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D)
                            )
                        )
                    }
                }

                Text(
                    text = "Masuk: ${if (item.tanggalMasuk.isNotBlank()) item.tanggalMasuk else "-"}",
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline),
                    fontSize = 11.sp
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = item.namaItem,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp)
            )

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "ID: ${item.idBarang}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                )
                if (item.merek.isNotBlank()) {
                    Text(
                        text = "Merek: ${item.merek}",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
                if (item.serialNumber.isNotBlank()) {
                    Text(
                        text = "SN: ${item.serialNumber}",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }

            if (item.spesifikasi.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Spek: ${item.spesifikasi}",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray, fontSize = 11.sp)
                )
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Sisa Stok", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
                        Text(
                            "$availableQty ${item.satuan}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = if (availableQty > 0) Color(0xFF16A34A) else Color.Red
                            )
                        )
                    }

                    Column {
                        Text("Terpasang", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
                        Text(
                            "${item.usedCount} ${item.satuan}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD97706)
                            )
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // a. Gunakan / Pasang (Alokasi ke PC Lab) - Pure Icon Button
                    IconButton(
                        onClick = onUse,
                        enabled = availableQty > 0,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("btn_gunakan_peripheral_${item.idBarang}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = "Gunakan / Pasang Ke PC Lab",
                            tint = if (availableQty > 0) Color(0xFF0284C7) else Color.Gray.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // b. Edit (Perbarui data unit) - Pure Icon Button
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("btn_edit_peripheral_${item.idBarang}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Data Stok Peripheral",
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // c. Lapor Rusak (Mutasi ke daftar peripheral rusak) - Pure Icon Button
                    IconButton(
                        onClick = onLaporRusak,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("btn_lapor_rusak_peripheral_${item.idBarang}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Lapor Kerusakan Peripheral",
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // d. Hapus Data Peripheral - Pure Icon Button
                    if (onDelete != null) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("btn_hapus_peripheral_${item.idBarang}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Hapus Stok Peripheral",
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun StokPeripheralCameraScanDialog(
    onDismiss: () -> Unit,
    onCodeScanned: (String) -> Unit
) {
    CameraScannerDialog(
        title = "Pindai QR / Barcode Peripheral",
        initialMode = ScanMode.PRIMARY_QR,
        onDismissRequest = onDismiss,
        onCodeScannedWithMode = { scannedCode, mode ->
            onCodeScanned(scannedCode)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeripheralStockFormDialog(
    initialItem: PeripheralStockEntity?,
    masterCategories: List<String>,
    masterMerek: List<String>,
    masterRuang: List<String>,
    masterSumberDana: List<String>,
    masterKondisi: List<String>,
    masterSatuan: List<String>,
    defaultOfficer: String,
    onDismiss: () -> Unit,
    onScanBarcodeClick: ((String) -> Unit) -> Unit,
    onSave: (PeripheralStockEntity) -> Unit
) {
    val isEdit = initialItem != null
    var isAutoId by remember { mutableStateOf(!isEdit) }
    var idBarang by remember {
        mutableStateOf(initialItem?.idBarang ?: "PER-${SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(Date())}")
    }
    var jenisPeripheral by remember {
        mutableStateOf(initialItem?.jenisPeripheral ?: (masterCategories.firstOrNull() ?: "RAM"))
    }
    var namaItem by remember { mutableStateOf(initialItem?.namaItem ?: "") }
    var merek by remember { mutableStateOf(initialItem?.merek ?: "") }
    var merekExpanded by remember { mutableStateOf(false) }
    var spesifikasi by remember { mutableStateOf(initialItem?.spesifikasi ?: "") }
    var satuan by remember { mutableStateOf(initialItem?.satuan ?: "Unit") }
    var jumlahText by remember { mutableStateOf(initialItem?.jumlah?.toString() ?: "1") }
    var tanggalMasuk by remember {
        mutableStateOf(initialItem?.tanggalMasuk ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    }
    var sumberDana by remember { mutableStateOf(initialItem?.sumberDana ?: (masterSumberDana.firstOrNull() ?: "BOS Reguler")) }
    var lokasiRuang by remember { mutableStateOf(initialItem?.lokasiRuang ?: (masterRuang.firstOrNull() ?: "")) }
    var kondisi by remember { mutableStateOf(initialItem?.kondisi ?: (masterKondisi.firstOrNull() ?: "Baru")) }
    var serialNumber by remember { mutableStateOf(initialItem?.serialNumber ?: "") }

    var catDropdownExpanded by remember { mutableStateOf(false) }

    var satuanExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isEdit) "Edit Data Stok Peripheral" else "Tambah Stok Peripheral Baru",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Section 1: Identitas & Kategori
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Identitas & Kategori",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Auto / Manual ID Toggle Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Saklar Mode ID Barang:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(if (isAutoId) "ID Otomatis" else "ID Manual", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(6.dp))
                                Switch(
                                    checked = isAutoId,
                                    onCheckedChange = { checked ->
                                        isAutoId = checked
                                        if (checked && !isEdit) {
                                            idBarang = "PER-${SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(Date())}"
                                        }
                                    },
                                    modifier = Modifier.scale(0.85f)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = idBarang,
                                onValueChange = { if (!isAutoId) idBarang = it },
                                readOnly = isAutoId,
                                label = { Text("ID Barang *", fontSize = 11.sp) },
                                trailingIcon = if (isAutoId) {
                                    { Text("Otomatis", fontSize = 9.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp)) }
                                } else null,
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )

                            ExposedDropdownMenuBox(
                                expanded = catDropdownExpanded,
                                onExpandedChange = { catDropdownExpanded = !catDropdownExpanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = cleanCategoryText(jenisPeripheral),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Kategori *", fontSize = 11.sp) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catDropdownExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = catDropdownExpanded,
                                    onDismissRequest = { catDropdownExpanded = false }
                                ) {
                                    masterCategories.forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text(cat, fontSize = 12.sp) },
                                            onClick = {
                                                jenisPeripheral = cat
                                                catDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = namaItem,
                            onValueChange = { namaItem = it },
                            label = { Text("Nama Peripheral / Item *", fontSize = 11.sp) },
                            placeholder = { Text("Contoh: RAM Kingston Fury 8GB DDR4") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                // Section 2: Spesifikasi & Merek
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Memory,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Merek & Spesifikasi",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val filteredMerek = masterMerek.filter { it.contains(merek, ignoreCase = true) }
                            ExposedDropdownMenuBox(
                                expanded = merekExpanded,
                                onExpandedChange = { merekExpanded = !merekExpanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = merek,
                                    onValueChange = {
                                        merek = it
                                        merekExpanded = true
                                    },
                                    label = { Text("Merek / Brand *", fontSize = 11.sp) },
                                    placeholder = { Text("Pilih / Ketik Merek") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = merekExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = merekExpanded,
                                    onDismissRequest = { merekExpanded = false }
                                ) {
                                    filteredMerek.forEach { brand ->
                                        DropdownMenuItem(
                                            text = { Text(brand, fontSize = 12.sp) },
                                            onClick = {
                                                merek = brand
                                                merekExpanded = false
                                            }
                                        )
                                    }
                                    if (merek.isNotBlank() && filteredMerek.none { it.equals(merek, ignoreCase = true) }) {
                                        DropdownMenuItem(
                                            text = { Text("+ Tambah Merek Baru: \"$merek\"", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                                            onClick = {
                                                merekExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = serialNumber,
                                onValueChange = { serialNumber = it },
                                label = { Text("Serial Number (SN)", fontSize = 11.sp) },
                                trailingIcon = {
                                    IconButton(onClick = {
                                        onScanBarcodeClick { scanned ->
                                            serialNumber = scanned
                                        }
                                    }) {
                                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Kamera", tint = MaterialTheme.colorScheme.primary)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        OutlinedTextField(
                            value = spesifikasi,
                            onValueChange = { spesifikasi = it },
                            label = { Text("Detail Spesifikasi / Kapasitas", fontSize = 11.sp) },
                            placeholder = { Text("Contoh: 8GB DDR4 PC25600 / 500GB NVMe") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                // Section 3: Kuantitas & Kondisi Fisik
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Dns,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Stok & Kondisi Fisik",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = jumlahText,
                                onValueChange = { jumlahText = it.filter { c -> c.isDigit() } },
                                label = { Text("Jumlah Stok *", fontSize = 11.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )

                            val filteredSatuan = masterSatuan.filter { it.contains(satuan, ignoreCase = true) }
                            ExposedDropdownMenuBox(
                                expanded = satuanExpanded,
                                onExpandedChange = { satuanExpanded = !satuanExpanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = satuan,
                                    onValueChange = {
                                        satuan = it
                                        satuanExpanded = true
                                    },
                                    label = { Text("Satuan *", fontSize = 11.sp) },
                                    placeholder = { Text("Pilih Satuan") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = satuanExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = satuanExpanded,
                                    onDismissRequest = { satuanExpanded = false }
                                ) {
                                    filteredSatuan.forEach { unitItem ->
                                        DropdownMenuItem(
                                            text = { Text(unitItem, fontSize = 12.sp) },
                                            onClick = {
                                                satuan = unitItem
                                                satuanExpanded = false
                                            }
                                        )
                                    }
                                    if (satuan.isNotBlank() && filteredSatuan.none { it.equals(satuan, ignoreCase = true) }) {
                                        DropdownMenuItem(
                                            text = { Text("+ Gunakan Satuan Baru: \"$satuan\"", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                                            onClick = {
                                                satuanExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        var kondisiExpanded by remember { mutableStateOf(false) }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ExposedDropdownMenuBox(
                                expanded = kondisiExpanded,
                                onExpandedChange = { kondisiExpanded = !kondisiExpanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = kondisi,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Kondisi *", fontSize = 11.sp) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = kondisiExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = kondisiExpanded,
                                    onDismissRequest = { kondisiExpanded = false }
                                ) {
                                    masterKondisi.forEach { cond ->
                                        DropdownMenuItem(
                                            text = { Text(cond, fontSize = 12.sp) },
                                            onClick = {
                                                kondisi = cond
                                                kondisiExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                DateDropdownSelector(
                                    selectedDateString = tanggalMasuk,
                                    onDateChanged = { tanggalMasuk = it },
                                    label = "Tanggal Masuk *"
                                )
                            }
                        }
                    }
                }

                // Section 4: Lokasi & Sumber Dana
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Place,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Lokasi & Sumber Dana",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        var ruangExpanded by remember { mutableStateOf(false) }
                        var sumberDanaExpanded by remember { mutableStateOf(false) }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ExposedDropdownMenuBox(
                                expanded = ruangExpanded,
                                onExpandedChange = { ruangExpanded = !ruangExpanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = lokasiRuang,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Ruang / Lokasi *", fontSize = 11.sp) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ruangExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = ruangExpanded,
                                    onDismissRequest = { ruangExpanded = false }
                                ) {
                                    masterRuang.forEach { room ->
                                        DropdownMenuItem(
                                            text = { Text(room, fontSize = 12.sp) },
                                            onClick = {
                                                lokasiRuang = room
                                                ruangExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            ExposedDropdownMenuBox(
                                expanded = sumberDanaExpanded,
                                onExpandedChange = { sumberDanaExpanded = !sumberDanaExpanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = sumberDana,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Sumber Dana *", fontSize = 11.sp) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sumberDanaExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = sumberDanaExpanded,
                                    onDismissRequest = { sumberDanaExpanded = false }
                                ) {
                                    masterSumberDana.forEach { src ->
                                        DropdownMenuItem(
                                            text = { Text(src, fontSize = 12.sp) },
                                            onClick = {
                                                sumberDana = src
                                                sumberDanaExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val jm = jumlahText.toIntOrNull() ?: 1
                    if (namaItem.isBlank() || idBarang.isBlank()) {
                        return@Button
                    }
                    val stockResult = (initialItem ?: PeripheralStockEntity(idBarang = idBarang, jenisPeripheral = jenisPeripheral, namaItem = namaItem)).copy(
                        idBarang = idBarang.trim(),
                        jenisPeripheral = cleanCategoryText(jenisPeripheral),
                        namaItem = namaItem.trim(),
                        merek = merek.trim(),
                        spesifikasi = spesifikasi.trim(),
                        satuan = satuan.ifBlank { "Unit" },
                        jumlah = jm,
                        tanggalMasuk = tanggalMasuk.ifBlank { SimpleDateFormat("yyyy-MM-DD", Locale.getDefault()).format(Date()) },
                        sumberDana = sumberDana,
                        lokasiRuang = lokasiRuang,
                        kondisi = kondisi,
                        serialNumber = serialNumber.trim()
                    )
                    onSave(stockResult)
                }
            ) {
                Text(if (isEdit) "Simpan Perubahan" else "Tambah Stok")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
private fun CatatPemakaianPeripheralDialog(
    item: PeripheralStockEntity,
    allUnits: List<com.example.data.entity.UnitEntity>,
    masterRuang: List<String>,
    defaultOfficer: String,
    onDismiss: () -> Unit,
    onConfirmUse: (targetPc: String, countUsed: Int, notes: String) -> Unit
) {
    val availableQty = (item.jumlah - item.usedCount).coerceAtLeast(0)

    val ruangOptions = remember(masterRuang) {
        masterRuang.map { it.trim() }.distinct().filter { it.isNotBlank() }
    }

    var selectedRuang by remember(ruangOptions) {
        mutableStateOf(ruangOptions.firstOrNull() ?: "")
    }
    var ruangDropdownExpanded by remember { mutableStateOf(false) }

    val pcOptions = remember(selectedRuang) {
        when {
            selectedRuang.contains("Lab Komputer 1", ignoreCase = true) || selectedRuang.contains("Lab 1", ignoreCase = true) || selectedRuang.contains("LabKom 1", ignoreCase = true) ->
                (1..20).map { "PC-LAB1-${String.format(Locale.US, "%02d", it)}" }
            selectedRuang.contains("Lab Komputer 2", ignoreCase = true) || selectedRuang.contains("Lab 2", ignoreCase = true) || selectedRuang.contains("LabKom 2", ignoreCase = true) ->
                (1..20).map { "PC-LAB2-${String.format(Locale.US, "%02d", it)}" }
            selectedRuang.contains("Server", ignoreCase = true) || selectedRuang.contains("NOC", ignoreCase = true) ->
                listOf("PC Server Main", "PC Server Storage", "PC NOC Master", "RACK-SERVER-01")
            selectedRuang.contains("TU", ignoreCase = true) || selectedRuang.contains("Tata Usaha", ignoreCase = true) ->
                listOf("PC-TU-01", "PC-TU-02", "PC-TU-03", "PC-TU-BENDAHARA")
            selectedRuang.equals("Luar LabKom", ignoreCase = true) ->
                listOf("Luar LabKom / Perangkat Khusus")
            else -> {
                val code = selectedRuang.uppercase().replace(Regex("[^A-Z0-9]"), "").take(6).ifBlank { "ROOM" }
                (1..10).map { "PC-$code-${String.format(Locale.US, "%02d", it)}" } + listOf("Perangkat Khusus $selectedRuang")
            }
        }
    }

    var selectedPcUnit by remember(pcOptions) { mutableStateOf(pcOptions.firstOrNull() ?: "Luar LabKom") }
    var pcDropdownExpanded by remember { mutableStateOf(false) }

    var countUsedText by remember { mutableStateOf("1") }
    var tanggalPakai by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var notes by remember { mutableStateOf("") }
    var customPcText by remember { mutableStateOf("") }

    LaunchedEffect(selectedRuang) {
        selectedPcUnit = pcOptions.firstOrNull() ?: "Luar LabKom"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Catat Pemakaian Peripheral", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card 1: Information
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = item.namaItem,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Kategori: ${cleanCategoryText(item.jenisPeripheral)} | Sisa Stok: $availableQty ${item.satuan}",
                            fontSize = 12.sp,
                            color = Color(0xFF15803D),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Card 2: Target Location
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Text("Target Penempatan", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        }

                        // 1. Filter Ruang/Lokasi
                        Box {
                            OutlinedTextField(
                                value = selectedRuang,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Ruang / Lokasi Target *", fontSize = 11.sp) },
                                trailingIcon = { IconButton(onClick = { ruangDropdownExpanded = !ruangDropdownExpanded }) { Icon(Icons.Default.ArrowDropDown, null) } },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                            DropdownMenu(
                                expanded = ruangDropdownExpanded,
                                onDismissRequest = { ruangDropdownExpanded = false }
                            ) {
                                ruangOptions.forEach { r ->
                                    DropdownMenuItem(
                                        text = { Text(r, fontSize = 12.sp) },
                                        onClick = {
                                            selectedRuang = r
                                            ruangDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // 2. Unit PC Target
                        Box {
                            OutlinedTextField(
                                value = selectedPcUnit,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Unit PC Target ($selectedRuang) *", fontSize = 11.sp) },
                                trailingIcon = { IconButton(onClick = { pcDropdownExpanded = !pcDropdownExpanded }) { Icon(Icons.Default.ArrowDropDown, null) } },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                            DropdownMenu(
                                expanded = pcDropdownExpanded,
                                onDismissRequest = { pcDropdownExpanded = false }
                            ) {
                                pcOptions.forEach { pc ->
                                    DropdownMenuItem(
                                        text = { Text(pc, fontSize = 12.sp) },
                                        onClick = {
                                            selectedPcUnit = pc
                                            pcDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        if (selectedRuang == "Luar LabKom" || selectedPcUnit == "Luar LabKom") {
                            OutlinedTextField(
                                value = customPcText,
                                onValueChange = { customPcText = it },
                                label = { Text("Nama Ruangan / Perangkat Khusus", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }

                // Card 3: Usage details
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Text("Detail Pemakaian", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        }

                        OutlinedTextField(
                            value = countUsedText,
                            onValueChange = { countUsedText = it.filter { c -> c.isDigit() } },
                            label = { Text("Jumlah Dipakai * (${item.satuan})", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        DateDropdownSelector(
                            selectedDateString = tanggalPakai,
                            onDateChanged = { tanggalPakai = it },
                            label = "Tanggal Pakai *"
                        )

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Catatan / Keterangan", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val count = countUsedText.toIntOrNull() ?: 1
                    if (count <= 0 || count > availableQty) return@Button
                    val targetLocation = if (selectedRuang == "Luar LabKom") customPcText.ifBlank { "Luar LabKom" } else "$selectedRuang ($selectedPcUnit)"
                    onConfirmUse(targetLocation, count, "$notes [Tgl: $tanggalPakai]")
                }
            ) {
                Text("Konfirmasi Pemakaian")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LaporRusakPeripheralDialog(
    item: PeripheralStockEntity,
    defaultOfficer: String,
    masterSatuan: List<String>,
    onDismiss: () -> Unit,
    onScanBarcodeClick: ((String) -> Unit) -> Unit,
    onConfirmLaporRusak: (
        customId: String,
        moveQty: Int,
        satuan: String,
        officer: String,
        date: String,
        serialNumber: String,
        reason: String
    ) -> Unit
) {
    var isAutoId by remember { mutableStateOf(true) }
    var idBarang by remember {
        mutableStateOf(item.idBarang.ifBlank { "PRPH-${SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(Date())}" })
    }
    var tanggalLaporan by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    }
    var jumlahText by remember { mutableStateOf(item.jumlah.coerceAtLeast(1).toString()) }
    var satuan by remember { mutableStateOf(item.satuan.ifBlank { masterSatuan.firstOrNull() ?: "Unit" }) }
    var satuanExpanded by remember { mutableStateOf(false) }
    var officerName by remember { mutableStateOf(defaultOfficer.ifBlank { "Laboran Komputer" }) }
    var serialNumber by remember { mutableStateOf(item.serialNumber) }
    var reasonText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Lapor Kerusakan Peripheral",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Info Banner Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Item: ${item.namaItem} (${cleanCategoryText(item.jenisPeripheral)})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF92400E)
                        )
                        Text(
                            text = "Stok Tersedia: ${item.jumlah} ${item.satuan} | Lokasi: ${item.lokasiRuang}",
                            fontSize = 11.sp,
                            color = Color(0xFFB45309)
                        )
                    }
                }

                // Section 1: Identitas Laporan
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Tag, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Text("Identitas & Tanggal Laporan", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Kode Laporan:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(if (isAutoId) "ID Otomatis" else "ID Manual", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(4.dp))
                                Switch(
                                    checked = isAutoId,
                                    onCheckedChange = { checked ->
                                        isAutoId = checked
                                        if (checked) {
                                            idBarang = "PRPH-${SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(Date())}"
                                        }
                                    },
                                    modifier = Modifier.scale(0.85f)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = idBarang,
                            onValueChange = { if (!isAutoId) idBarang = it },
                            readOnly = isAutoId,
                            label = { Text("ID Barang / Kode Laporan *", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        DateDropdownSelector(
                            selectedDateString = tanggalLaporan,
                            onDateChanged = { tanggalLaporan = it },
                            label = "Tanggal Laporan *"
                        )
                    }
                }

                // Section 2: Kuantitas & Petugas
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Dns, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Text("Kuantitas & Petugas", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = jumlahText,
                                onValueChange = { jumlahText = it.filter { c -> c.isDigit() } },
                                label = { Text("Jumlah Rusak *", fontSize = 11.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )

                            ExposedDropdownMenuBox(
                                expanded = satuanExpanded,
                                onExpandedChange = { satuanExpanded = !satuanExpanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = satuan,
                                    onValueChange = { satuan = it },
                                    label = { Text("Satuan *", fontSize = 11.sp) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = satuanExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = satuanExpanded,
                                    onDismissRequest = { satuanExpanded = false }
                                ) {
                                    masterSatuan.forEach { s ->
                                        DropdownMenuItem(
                                            text = { Text(s, fontSize = 12.sp) },
                                            onClick = {
                                                satuan = s
                                                satuanExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = officerName,
                            onValueChange = { officerName = it },
                            label = { Text("Petugas Pelapor *", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                // Section 3: Serial Number & Alasan
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                            Text("Serial Number & Alasan Kerusakan", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFDC2626))
                        }

                        OutlinedTextField(
                            value = serialNumber,
                            onValueChange = { serialNumber = it },
                            label = { Text("Serial Number (SN) / Barcode", fontSize = 11.sp) },
                            placeholder = { Text("Pindai / ketik SN...") },
                            trailingIcon = {
                                IconButton(onClick = {
                                    onScanBarcodeClick { scanned ->
                                        serialNumber = scanned
                                    }
                                }) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Pindai Kamera", tint = MaterialTheme.colorScheme.primary)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = reasonText,
                            onValueChange = { reasonText = it },
                            label = { Text("Alasan Kerusakan / Gejala Defek *", fontSize = 11.sp) },
                            placeholder = { Text("Contoh: Pin terbakar / Tidak terdeteksi / Rusak fisik") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = jumlahText.toIntOrNull() ?: 1
                    if (qty <= 0) return@Button
                    if (reasonText.isBlank()) return@Button
                    onConfirmLaporRusak(idBarang, qty, satuan, officerName, tanggalLaporan, serialNumber, reasonText)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Konfirmasi Lapor Rusak")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateDropdownSelector(
    selectedDateString: String,
    onDateChanged: (String) -> Unit,
    label: String = "Tanggal *"
) {
    var showModalDatePicker by remember { mutableStateOf(false) }

    fun formatDateForDisplay(dateStr: String): String {
        return try {
            val parts = dateStr.split("-")
            if (parts.size == 3) {
                val y = parts[0].toInt()
                val m = parts[1].toInt()
                val d = parts[2].toInt()
                val monthsList = listOf(
                    "Januari", "Februari", "Maret", "April", "Mei", "Juni",
                    "Juli", "Agustus", "September", "Oktober", "November", "Desember"
                )
                val monthName = monthsList.getOrElse(m - 1) { "" }
                String.format(Locale("id", "ID"), "%02d %s %04d", d, monthName, y)
            } else dateStr
        } catch (e: Exception) {
            dateStr
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = formatDateForDisplay(selectedDateString),
            onValueChange = {},
            readOnly = true,
            label = { Text(label, fontSize = 11.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Event,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                IconButton(onClick = { showModalDatePicker = true }) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Buka Pemilih Tanggal",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(10.dp)
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showModalDatePicker = true }
        )
    }

    if (showModalDatePicker) {
        LunarisDatePickerDialog(
            onDismissRequest = { showModalDatePicker = false },
            selectedDateString = selectedDateString,
            onDateSelected = { newDate ->
                onDateChanged(newDate)
            }
        )
    }
}

private fun parseCsvLine(line: String, delimiter: Char): List<String> {
    val result = mutableListOf<String>()
    var current = java.lang.StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < line.length) {
        val c = line[i]
        if (c == '"') {
            inQuotes = !inQuotes
        } else if (c == delimiter && !inQuotes) {
            result.add(current.toString().trim().removeSurrounding("\""))
            current = java.lang.StringBuilder()
        } else {
            current.append(c)
        }
        i++
    }
    result.add(current.toString().trim().removeSurrounding("\""))
    return result
}

private fun escapeCsv(value: String): String {
    val clean = value.replace("\"", "\"\"")
    return if (clean.contains(",") || clean.contains("\"") || clean.contains("\n") || clean.contains("\r")) {
        "\"$clean\""
    } else {
        clean
    }
}
