package com.example.ui.screens

import android.Manifest
import android.widget.Toast
import androidx.camera.core.CameraControl
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.data.entity.PeripheralRusakEntity
import com.example.data.entity.PeripheralStockEntity
import com.example.ui.components.CameraScannerDialog
import com.example.ui.components.FilterGroup
import com.example.ui.components.LunarisCard
import com.example.ui.components.LunarisFilterDialog
import com.example.ui.components.ScanMode
import com.example.ui.viewmodel.InventoryViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun cleanCategoryText(rawCategory: String): String {
    if (rawCategory.isBlank()) return "Peripheral"
    val cleaned = rawCategory
        .replace(Regex("[\\p{So}\\p{Cn}\\p{Cs}\\p{Extended_Pictographic}]"), "")
        .replace(Regex("^[\\s\\W_]+"), "")
        .trim()
    return if (cleaned.isNotBlank()) cleaned else rawCategory.trim()
}

private fun isSamePeripheralCategory(subKategori: String, stockCategory: String): Boolean {
    val sub = cleanCategoryText(subKategori).trim().lowercase(Locale.getDefault())
    val stock = cleanCategoryText(stockCategory).trim().lowercase(Locale.getDefault())

    if (sub.isBlank() || stock.isBlank()) return false
    if (sub == stock) return true

    val isSubRam = sub.contains("ram")
    val isStockRam = stock.contains("ram")
    if (isSubRam || isStockRam) return isSubRam == isStockRam

    val isSubStorage = sub.contains("storage") || sub.contains("penyimpanan") || sub.contains("ssd") || sub.contains("hdd")
    val isStockStorage = stock.contains("storage") || stock.contains("penyimpanan") || stock.contains("ssd") || stock.contains("hdd")
    if (isSubStorage || isStockStorage) return isSubStorage == isStockStorage

    val isSubInput = sub.contains("mouse") || sub.contains("keyboard")
    val isStockInput = stock.contains("mouse") || stock.contains("keyboard")
    if (isSubInput || isStockInput) return isSubInput == isStockInput

    val isSubPower = sub.contains("ups") || sub.contains("psu") || sub.contains("power")
    val isStockPower = stock.contains("ups") || stock.contains("psu") || stock.contains("power")
    if (isSubPower || isStockPower) return isSubPower == isStockPower

    return sub == stock || sub.contains(stock) || stock.contains(sub)
}

private fun getSatuanForItem(item: PeripheralRusakEntity, stocks: List<PeripheralStockEntity>): String {
    val match = Regex("\\[Satuan:\\s*([^\\]]+)\\]").find(item.keteranganKerusakan)
    if (match != null) {
        val s = match.groupValues[1].trim()
        if (s.isNotBlank()) return s
    }
    val foundStock = stocks.find {
        it.namaItem.equals(item.namaBarang, ignoreCase = true) ||
                (it.idBarang.isNotBlank() && it.idBarang.equals(item.idBarang, ignoreCase = true))
    }
    if (foundStock != null && foundStock.satuan.isNotBlank()) {
        return foundStock.satuan
    }
    return "Unit"
}

private fun cleanKeterangan(rawKeterangan: String): String {
    return rawKeterangan.replace(Regex("\\[Satuan:\\s*([^\\]]+)\\]"), "").trim()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeripheralRusakScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(1) } // 0: Lapor Rusak, 1: List Peripheral

    val peripheralRusakList by viewModel.allPeripheralRusak.collectAsState()
    val defaultOfficer by viewModel.defaultOfficer.collectAsState()
    val allPeripheralStocks by viewModel.allPeripheralStocks.collectAsState()
    val allPeripherals by viewModel.allPeripherals.collectAsState()
    val kondisiList by viewModel.kondisi.collectAsState()
    val allUnits by viewModel.allUnits.collectAsState()

    val userRole by viewModel.userRole.collectAsState()
    val canLapor = userRole.contains("admin", ignoreCase = true) || viewModel.isStudentPermissionGranted("peripheral_lapor_rusak")
    val canList = userRole.contains("admin", ignoreCase = true) || viewModel.isStudentPermissionGranted("peripheral_list")

    LaunchedEffect(canLapor, canList) {
        if (!canLapor && canList && selectedTab == 0) {
            selectedTab = 1
        } else if (canLapor && !canList && selectedTab == 1) {
            selectedTab = 0
        }
    }

    // Master Satuan List
    val masterSatuan = remember(allUnits) {
        val unitNames = allUnits.map { it.name }
        if (unitNames.isNotEmpty()) unitNames else listOf("Unit", "Buah", "Set", "Pcs", "Box")
    }

    // Category list sourced directly from Peripheral master data
    val masterCategories = remember(allPeripherals) {
        val fromSettings = viewModel.settingsRepository.getPeripheral()
        val defaultList = listOf(
            "RAM",
            "Storage / Media Penyimpanan",
            "Mouse & Keyboard",
            "UPS & PSU",
            "Peripheral Lainnya"
        )
        val combined = (defaultList + fromSettings + allPeripherals.map { it.name })
        combined.map { cleanCategoryText(it) }.distinct().filter { it.isNotBlank() }
    }

    // Dynamic condition/status list from master data
    val statusDiagnosaOptions = remember {
        listOf("Perlu Diagnosa")
    }

    // Form States
    var subKategori by remember(masterCategories) { mutableStateOf(masterCategories.firstOrNull() ?: "RAM") }
    var subKategoriExpanded by remember { mutableStateOf(false) }

    var namaBarang by remember { mutableStateOf("") }
    var namaBarangExpanded by remember { mutableStateOf(false) }

    var isAutoId by remember { mutableStateOf(true) }
    var idBarang by remember { mutableStateOf("") }
    var serialNumber by remember { mutableStateOf("") }
    var jumlahText by remember { mutableStateOf("1") }
    var satuan by remember(masterSatuan) { mutableStateOf(masterSatuan.firstOrNull() ?: "Unit") }

    var tanggalLaporan by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    }
    var keterangan by remember { mutableStateOf("") }
    var namaPetugas by remember(defaultOfficer) { mutableStateOf(defaultOfficer.ifBlank { "Laboran Komputer" }) }
    val statusDiagnosa = "Perlu Diagnosa"

    fun generateAutoId(): String {
        return "PRPH-" + SimpleDateFormat("yyMMdd", Locale.getDefault()).format(Date()) + "-" + (100..999).random()
    }

    LaunchedEffect(isAutoId) {
        if (isAutoId && idBarang.isBlank()) {
            idBarang = generateAutoId()
        }
    }

    // Dynamic interactive peripheral suggestions (strictly filtered by current category)
    val peripheralSuggestions = remember(subKategori, namaBarang, allPeripheralStocks) {
        allPeripheralStocks
            .filter { stock ->
                isSamePeripheralCategory(subKategori, stock.jenisPeripheral) &&
                        (namaBarang.isBlank() || stock.namaItem.contains(namaBarang, ignoreCase = true))
            }
            .map { it.namaItem }
            .distinct()
            .filter { it.isNotBlank() }
    }

    // Auto-fill logic when ID Barang or Serial Number (SN) is entered/scanned
    LaunchedEffect(idBarang, serialNumber) {
        val cleanId = idBarang.trim()
        val cleanSn = serialNumber.trim()

        if (cleanId.isNotBlank() || cleanSn.isNotBlank()) {
            val matchedStock = allPeripheralStocks.firstOrNull { stock ->
                (cleanId.isNotBlank() && stock.idBarang.isNotBlank() && stock.idBarang.equals(cleanId, ignoreCase = true)) ||
                (cleanSn.isNotBlank() && stock.serialNumber.isNotBlank() && stock.serialNumber.equals(cleanSn, ignoreCase = true))
            }
            if (matchedStock != null) {
                if (matchedStock.namaItem.isNotBlank()) {
                    namaBarang = matchedStock.namaItem
                }
                val catClean = cleanCategoryText(matchedStock.jenisPeripheral)
                if (catClean.isNotBlank()) {
                    subKategori = catClean
                }
                if (matchedStock.satuan.isNotBlank()) {
                    satuan = matchedStock.satuan
                }
                if (matchedStock.idBarang.isNotBlank() && (idBarang.isBlank() || isAutoId)) {
                    idBarang = matchedStock.idBarang
                    isAutoId = false
                }
                if (matchedStock.serialNumber.isNotBlank() && serialNumber.isBlank()) {
                    serialNumber = matchedStock.serialNumber
                }
            }
        }
    }

    // Auto-fill logic when Nama Peripheral is selected/typed
    LaunchedEffect(namaBarang, subKategori) {
        val cleanName = namaBarang.trim()
        if (cleanName.isNotBlank()) {
            val matchedStock = allPeripheralStocks.firstOrNull { stock ->
                stock.namaItem.equals(cleanName, ignoreCase = true)
            }
            if (matchedStock != null) {
                if (matchedStock.satuan.isNotBlank()) {
                    satuan = matchedStock.satuan
                }
                val catClean = cleanCategoryText(matchedStock.jenisPeripheral)
                if (catClean.isNotBlank() && subKategori.isBlank()) {
                    subKategori = catClean
                }
                if (matchedStock.idBarang.isNotBlank() && (idBarang.isBlank() || isAutoId)) {
                    idBarang = matchedStock.idBarang
                    isAutoId = false
                }
                if (matchedStock.serialNumber.isNotBlank() && serialNumber.isBlank()) {
                    serialNumber = matchedStock.serialNumber
                }
            }
        }
    }

    // Dialog States
    var showBarcodeScannerDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var selectedDetailItem by remember { mutableStateOf<PeripheralRusakEntity?>(null) }
    var itemToMutasiRusak by remember { mutableStateOf<PeripheralRusakEntity?>(null) }
    var itemToMutasiNormal by remember { mutableStateOf<PeripheralRusakEntity?>(null) }
    var itemToDelete by remember { mutableStateOf<PeripheralRusakEntity?>(null) }

    // Search and Filter
    var searchQuery by remember { mutableStateOf("") }
    var showQrScanner by remember { mutableStateOf(false) }
    var appliedFilterSubKategori by remember { mutableStateOf("Semua") }
    var tempFilterSubKategori by remember { mutableStateOf("Semua") }
    var activeCardId by remember { mutableStateOf<Int?>(null) }

    // Dual-Priority Scanner States
    var showCameraScannerDialog by remember { mutableStateOf(false) }
    var initialScanMode by remember { mutableStateOf(ScanMode.PRIMARY_QR) }
    var showNotFoundDialog by remember { mutableStateOf(false) }
    var scannedCodeForNotFound by remember { mutableStateOf("") }
    var scannedModeForNotFound by remember { mutableStateOf(ScanMode.PRIMARY_QR) }
    var scannerTargetField by remember { mutableStateOf("AUTO_POPULATE") }

    val handleScannedCode = { rawCode: String, mode: ScanMode ->
        val cleanCode = if (rawCode.contains("\"id\":")) {
            try {
                rawCode.substringAfter("\"id\":\"").substringBefore("\"")
            } catch (e: Exception) { rawCode }
        } else rawCode

        if (scannerTargetField == "SEARCH") {
            searchQuery = cleanCode
            Toast.makeText(context, "Mencari data: $cleanCode", Toast.LENGTH_SHORT).show()
        } else {
            val matchedStock = allPeripheralStocks.firstOrNull { stock ->
                (stock.idBarang.isNotBlank() && stock.idBarang.equals(cleanCode, ignoreCase = true)) ||
                (stock.serialNumber.isNotBlank() && stock.serialNumber.equals(cleanCode, ignoreCase = true)) ||
                stock.namaItem.equals(cleanCode, ignoreCase = true)
            }
            if (matchedStock != null) {
                if (matchedStock.namaItem.isNotBlank()) {
                    namaBarang = matchedStock.namaItem
                }
                val catClean = cleanCategoryText(matchedStock.jenisPeripheral)
                if (catClean.isNotBlank()) {
                    subKategori = catClean
                }
                if (matchedStock.satuan.isNotBlank()) {
                    satuan = matchedStock.satuan
                }
                if (matchedStock.idBarang.isNotBlank()) {
                    idBarang = matchedStock.idBarang
                    isAutoId = false
                }
                if (matchedStock.serialNumber.isNotBlank()) {
                    serialNumber = matchedStock.serialNumber
                }
                Toast.makeText(context, "✨ Auto-Populate Berhasil! Data '${matchedStock.namaItem}' ditarik dari sistem.", Toast.LENGTH_LONG).show()
            } else {
                scannedCodeForNotFound = cleanCode
                scannedModeForNotFound = mode
                showNotFoundDialog = true
            }
        }
    }

    Scaffold(
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
                                text = "Peripheral Rusak",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Kelola diagnosa & penanganan peripheral bermasalah",
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
            if (canLapor && selectedTab == 1) {
                FloatingActionButton(
                    onClick = { selectedTab = 0 },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.testTag("btn_lapor_rusak_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Lapor Rusak")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!canLapor && !canList) {
                Box(
                    modifier = Modifier.fillMaxSize(),
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
                                text = "Anda tidak memiliki izin untuk mengakses sub-menu Lapor Rusak maupun List Peripheral. Silakan hubungi Super Admin.",
                                textAlign = TextAlign.Center,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            } else {
                // Tab Navigation
                val activeTabIdx = if (selectedTab == 0 && canLapor) 0 else if (canList) 1 else 0
                TabRow(
                    selectedTabIndex = activeTabIdx,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        if (activeTabIdx < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[activeTabIdx]),
                                color = MaterialTheme.colorScheme.primary,
                                height = 3.dp
                            )
                        }
                    }
                ) {
                    if (canLapor) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.AddAlert,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = if (selectedTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Lapor Rusak",
                                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        )
                    }
                    if (canList) {
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Computer,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = if (selectedTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "List Peripheral (${peripheralRusakList.size})",
                                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        )
                    }
                }

            if (selectedTab == 0) {
                // Form Tab (Lapor Rusak) - Info banner removed as requested
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Scan Auto-Populate Action Banner Button
                    item {
                        Button(
                            onClick = {
                                scannerTargetField = "AUTO_POPULATE"
                                initialScanMode = ScanMode.PRIMARY_QR
                                showCameraScannerDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0F766E),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_scan_auto_populate_peripheral")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pindai QR Lunaris / Barcode Peripheral (Auto-Populate)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Card Section 1: Identitas & Pilihan Peripheral
                    item {
                        com.example.ui.components.LunarisCard(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else Color.White
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (androidx.compose.foundation.isSystemInDarkTheme()) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f) else Color(0xFFE9D5FF)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Memory,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Identitas & Pilihan Peripheral",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 16.sp
                                    )
                                }

                                ExposedDropdownMenuBox(
                                    expanded = subKategoriExpanded,
                                    onExpandedChange = { subKategoriExpanded = !subKategoriExpanded },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = subKategori,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Kategori Peripheral *") },
                                        leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subKategoriExpanded) },
                                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = subKategoriExpanded,
                                        onDismissRequest = { subKategoriExpanded = false }
                                    ) {
                                        masterCategories.forEach { cat ->
                                            DropdownMenuItem(
                                                text = { Text(cat) },
                                                onClick = {
                                                    if (subKategori != cat) {
                                                        subKategori = cat
                                                        namaBarang = ""
                                                    }
                                                    subKategoriExpanded = false
                                                    namaBarangExpanded = true
                                                }
                                            )
                                        }
                                    }
                                }

                                ExposedDropdownMenuBox(
                                    expanded = namaBarangExpanded && peripheralSuggestions.isNotEmpty(),
                                    onExpandedChange = { namaBarangExpanded = !namaBarangExpanded },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = namaBarang,
                                        onValueChange = {
                                            namaBarang = it
                                            namaBarangExpanded = true
                                        },
                                        label = { Text("Nama Peripheral / Modul *") },
                                        placeholder = { Text("Ketik atau pilih nama barang...") },
                                        leadingIcon = { Icon(Icons.Default.Memory, contentDescription = null) },
                                        trailingIcon = if (peripheralSuggestions.isNotEmpty()) {
                                            { ExposedDropdownMenuDefaults.TrailingIcon(expanded = namaBarangExpanded) }
                                        } else null,
                                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    if (peripheralSuggestions.isNotEmpty()) {
                                        ExposedDropdownMenu(
                                            expanded = namaBarangExpanded,
                                            onDismissRequest = { namaBarangExpanded = false }
                                        ) {
                                            peripheralSuggestions.forEach { option ->
                                                DropdownMenuItem(
                                                    text = { Text(option) },
                                                    onClick = {
                                                        namaBarang = option
                                                        namaBarangExpanded = false
                                                        val foundStock = allPeripheralStocks.find { it.namaItem.equals(option, ignoreCase = true) }
                                                        if (foundStock != null) {
                                                            if (foundStock.idBarang.isNotBlank()) {
                                                                idBarang = foundStock.idBarang
                                                            }
                                                            if (foundStock.satuan.isNotBlank()) {
                                                                satuan = foundStock.satuan
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = jumlahText,
                                        onValueChange = { jumlahText = it.filter { c -> c.isDigit() } },
                                        label = { Text("Jumlah / Qty *") },
                                        leadingIcon = { Icon(Icons.Default.Numbers, contentDescription = null) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    OutlinedTextField(
                                        value = satuan.ifBlank { "Unit" },
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Satuan *") },
                                        placeholder = { Text("Otomatis") },
                                        leadingIcon = { Icon(Icons.Default.Straighten, contentDescription = null) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Card Section 2: Pemindai & Serial Number / Barcode
                    item {
                        com.example.ui.components.LunarisCard(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else Color.White
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (androidx.compose.foundation.isSystemInDarkTheme()) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f) else Color(0xFFE9D5FF)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Pemindai & Serial Number / Barcode",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 16.sp
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "ID Barang / Kode Laporan *",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                if (isAutoId) "ID Otomatis" else "ID Manual",
                                                fontSize = 11.sp,
                                                color = if (isAutoId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Switch(
                                                checked = isAutoId,
                                                onCheckedChange = { checked ->
                                                    isAutoId = checked
                                                    if (checked) {
                                                        idBarang = generateAutoId()
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
                                        label = { Text("ID Barang *") },
                                        placeholder = { Text(if (isAutoId) "Otomatis dibuat sistem" else "Ketik ID Manual...") },
                                        leadingIcon = { Icon(Icons.Default.Tag, contentDescription = null) },
                                        trailingIcon = if (isAutoId) {
                                            {
                                                IconButton(onClick = { idBarang = generateAutoId() }) {
                                                    Icon(Icons.Default.Refresh, contentDescription = "Acak Ulang ID", tint = MaterialTheme.colorScheme.primary)
                                                }
                                            }
                                        } else null,
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }

                                OutlinedTextField(
                                    value = serialNumber,
                                    onValueChange = { serialNumber = it },
                                    label = { Text("Serial Number (SN) / Barcode (Opsional)") },
                                    placeholder = { Text("Pindai atau ketik SN / Barcode...") },
                                    leadingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            scannerTargetField = "AUTO_POPULATE"
                                            initialScanMode = ScanMode.FALLBACK_BARCODE
                                            showCameraScannerDialog = true
                                        }) {
                                            Icon(
                                                Icons.Default.QrCodeScanner,
                                                contentDescription = "Pindai Barcode / QR SN",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }

                    // Card Section 3: Informasi Petugas & Tanggal
                    item {
                        com.example.ui.components.LunarisCard(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else Color.White
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (androidx.compose.foundation.isSystemInDarkTheme()) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f) else Color(0xFFE9D5FF)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Badge,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Informasi Petugas & Tanggal",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 16.sp
                                    )
                                }

                                DateDropdownSelector(
                                    selectedDateString = tanggalLaporan,
                                    onDateChanged = { tanggalLaporan = it },
                                    label = "Tanggal Laporan *"
                                )

                                OutlinedTextField(
                                    value = namaPetugas,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Nama Petugas / Laboran (Otomatis)") },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                    )
                                )
                            }
                        }
                    }

                    // Card Section 4: Diagnosa & Catatan Kerusakan
                    item {
                        com.example.ui.components.LunarisCard(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else Color.White
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (androidx.compose.foundation.isSystemInDarkTheme()) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f) else Color(0xFFE9D5FF)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ReportProblem,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Diagnosa & Catatan Kerusakan",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 16.sp
                                    )
                                }

                                OutlinedTextField(
                                    value = keterangan,
                                    onValueChange = { keterangan = it },
                                    label = { Text("Keterangan / Diagnosa Kerusakan") },
                                    placeholder = { Text("Masukkan rincian keluhan, gejala, atau lokasi fisik kerusakan...") },
                                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 2,
                                    maxLines = 4,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }

                    // 9. Tombol Simpan Laporan
                    item {
                        Button(
                            onClick = {
                                if (namaBarang.isBlank()) {
                                    Toast.makeText(context, "Nama peripheral wajib diisi!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val jumlah = jumlahText.toIntOrNull() ?: 1
                                val metaParts = mutableListOf<String>()
                                if (satuan.isNotBlank()) metaParts.add("Satuan: $satuan")
                                if (serialNumber.isNotBlank()) metaParts.add("SN: $serialNumber")

                                val metaStr = if (metaParts.isNotEmpty()) "[${metaParts.joinToString(" • ")}]" else ""
                                val fullKeterangan = if (metaStr.isNotBlank()) {
                                    if (keterangan.isBlank()) metaStr else "$keterangan $metaStr"
                                } else keterangan

                                viewModel.reportPeripheralRusak(
                                    idBarang = idBarang,
                                    namaBarang = namaBarang,
                                    subKategori = subKategori,
                                    jumlah = jumlah,
                                    keteranganKerusakan = fullKeterangan,
                                    namaPetugas = namaPetugas,
                                    statusDiagnosa = statusDiagnosa,
                                    tanggalKerusakan = tanggalLaporan,
                                    onSuccess = {
                                        Toast.makeText(context, "Laporan Peripheral Rusak Berhasil Disimpan! ✨", Toast.LENGTH_SHORT).show()
                                        namaBarang = ""
                                        serialNumber = ""
                                        keterangan = ""
                                        jumlahText = "1"
                                        if (isAutoId) {
                                            idBarang = generateAutoId()
                                        } else {
                                            idBarang = ""
                                        }
                                        selectedTab = 1
                                    },
                                    onError = { err ->
                                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Simpan Laporan Peripheral Rusak", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Tab List Peripheral
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(Modifier.height(12.dp))

                    // Search and Filter Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Ketik untuk mencari...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = null)
                                        }
                                    }
                                    IconButton(onClick = {
                                        scannerTargetField = "SEARCH"
                                        initialScanMode = ScanMode.PRIMARY_QR
                                        showCameraScannerDialog = true
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.QrCodeScanner,
                                            contentDescription = "Scan QR",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        FilledIconButton(
                            onClick = {
                                tempFilterSubKategori = appliedFilterSubKategori
                                showFilterDialog = true
                            },
                            modifier = Modifier.size(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (appliedFilterSubKategori != "Semua") Color(0xFF7C3AED).copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer,
                                contentColor = if (appliedFilterSubKategori != "Semua") Color(0xFF7C3AED) else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter")
                        }
                    }

                    if (appliedFilterSubKategori != "Semua") {
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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
                                        text = "Kategori: $appliedFilterSubKategori",
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
                                            .clickable { appliedFilterSubKategori = "Semua" }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    val filteredItems = remember(peripheralRusakList, searchQuery, appliedFilterSubKategori) {
                        peripheralRusakList.filter { item ->
                            val isNotMovedToHapus = item.status != "Hapus Aset" && !item.status.equals("Siap Afkir", ignoreCase = true) && !item.isHibah
                            val matchSearch = searchQuery.isBlank() ||
                                    item.namaBarang.contains(searchQuery, ignoreCase = true) ||
                                    item.subKategori.contains(searchQuery, ignoreCase = true) ||
                                    item.keteranganKerusakan.contains(searchQuery, ignoreCase = true)
                            val matchCategory = appliedFilterSubKategori == "Semua" || cleanCategoryText(item.subKategori).equals(appliedFilterSubKategori, ignoreCase = true)
                            isNotMovedToHapus && matchSearch && matchCategory
                        }
                    }

                    if (filteredItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            com.example.ui.components.LunarisCard(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else Color.White
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (androidx.compose.foundation.isSystemInDarkTheme()) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f) else Color(0xFFE9D5FF)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text("Tidak Ada Data Peripheral Rusak", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Semua peripheral hardware dalam kondisi baik.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(filteredItems, key = { it.id }) { item ->
                                val isSelected = (activeCardId == item.id) || (selectedDetailItem?.id == item.id)
                                PeripheralRusakCard(
                                    item = item,
                                    allPeripheralStocks = allPeripheralStocks,
                                    isSelected = isSelected,
                                    onClick = {
                                        activeCardId = item.id
                                        selectedDetailItem = item
                                    },
                                    onQuickNormal = {
                                        activeCardId = item.id
                                        itemToMutasiNormal = item
                                    },
                                    onDeleteAsset = {
                                        activeCardId = item.id
                                        itemToDelete = item
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dual-Priority Camera Scanner Dialog
    if (showCameraScannerDialog) {
        CameraScannerDialog(
            title = if (scannerTargetField == "SEARCH") "Pindai QR / Barcode Pencarian" else "Pindai Kode QR / Barcode Peripheral",
            initialMode = initialScanMode,
            onDismissRequest = { showCameraScannerDialog = false },
            onCodeScannedWithMode = { scannedCode, mode ->
                showCameraScannerDialog = false
                handleScannedCode(scannedCode, mode)
            }
        )
    }

    // Dialog Data Tidak Ditemukan
    if (showNotFoundDialog) {
        AlertDialog(
            onDismissRequest = { showNotFoundDialog = false },
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFEAB308),
                        modifier = Modifier.size(24.dp)
                    )
                    Text("Data Tidak Ditemukan", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                val modeLabel = if (scannedModeForNotFound == ScanMode.PRIMARY_QR) "QR Label Lunaris" else "Barcode Pabrik / SN"
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Kode '$scannedCodeForNotFound' ($modeLabel) tidak ditemukan di master peripheral stok.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Apakah Anda ingin mendaftarkan laporan barang baru dengan ID/SN ini atau beralih metode pemindaian?",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        serialNumber = scannedCodeForNotFound
                        if (idBarang.isBlank() || isAutoId) {
                            idBarang = scannedCodeForNotFound
                            isAutoId = false
                        }
                        showNotFoundDialog = false
                        Toast.makeText(context, "ID/SN '$scannedCodeForNotFound' diisi ke formulir.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Isi Form Manual", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            showNotFoundDialog = false
                            initialScanMode = if (scannedModeForNotFound == ScanMode.PRIMARY_QR) ScanMode.FALLBACK_BARCODE else ScanMode.PRIMARY_QR
                            showCameraScannerDialog = true
                        }
                    ) {
                        Text(
                            text = if (scannedModeForNotFound == ScanMode.PRIMARY_QR) "Pindai Barcode Pabrik" else "Pindai QR Lunaris",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    TextButton(onClick = { showNotFoundDialog = false }) {
                        Text("Batal", fontSize = 12.sp)
                    }
                }
            }
        )
    }

    // Filter Popup Dialog
    if (showFilterDialog) {
        LunarisFilterDialog(
            onDismissRequest = { showFilterDialog = false },
            filterGroups = listOf(
                FilterGroup(
                    title = "Kategori Peripheral",
                    options = listOf("Semua") + masterCategories,
                    selectedOption = tempFilterSubKategori,
                    onOptionSelected = { tempFilterSubKategori = it }
                )
            ),
            onReset = {
                tempFilterSubKategori = "Semua"
            },
            onApply = {
                appliedFilterSubKategori = tempFilterSubKategori
                showFilterDialog = false
            }
        )
    }

    // Comprehensive Detail Popup Dialog
    if (selectedDetailItem != null) {
        val item = selectedDetailItem!!
        val itemSatuan = getSatuanForItem(item, allPeripheralStocks)
        val cleanCat = cleanCategoryText(item.subKategori)
        val cleanKet = cleanKeterangan(item.keteranganKerusakan)

        AlertDialog(
            onDismissRequest = { selectedDetailItem = null },
            title = {
                Text("Detail Laporan Peripheral", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Nama: ${item.namaBarang}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Kategori: $cleanCat")
                    Text("ID Barcode: ${if (item.idBarang.isNotBlank()) item.idBarang else "-"}")
                    Text("Jumlah: ${item.jumlah} $itemSatuan")
                    Text("Entry Date (Tanggal Input): ${item.tanggalKerusakan}")
                    Text("Petugas / Laboran: ${item.namaPetugas}")
                    Text("Status Diagnosa: ${item.statusDiagnosa}", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    if (cleanKet.isNotBlank()) {
                        Text("Keterangan: $cleanKet")
                    }
                    if (item.validationCount > 0) {
                        Text("Validasi: ${item.validationCount}x oleh ${item.lastValidatedBy ?: "-"}")
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            val target = item
                            selectedDetailItem = null
                            itemToMutasiNormal = target
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Normal (Ke Stok)", fontSize = 11.sp)
                    }
                    Button(
                        onClick = {
                            val target = item
                            selectedDetailItem = null
                            itemToDelete = target
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Hapus Aset", fontSize = 11.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedDetailItem = null }) {
                    Text("Tutup")
                }
            }
        )
    }

    // Confirmation Dialog for "Hapus Aset" (Mutasi ke Antrian Afkir / Hapus Aset)
    if (itemToDelete != null) {
        val item = itemToDelete!!
        var tanggalMutasi by remember(item) {
            mutableStateOf(java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()))
        }
        var catatanMutasi by remember(item) { mutableStateOf("") }
        var officerName by remember(item) { mutableStateOf(defaultOfficer.ifBlank { "Laboran Komputer" }) }

        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Konfirmasi Mutasi ke Hapus Aset", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Summary info card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = item.namaBarang,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Kategori: ${cleanCategoryText(item.subKategori)} | ID: ${if (item.idBarang.isNotBlank()) item.idBarang else "-"} | Qty: ${item.jumlah} ${getSatuanForItem(item, allPeripheralStocks)}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    Text(
                        "Pindahkan item peripheral rusak ini ke Menu Utama 'Hapus Aset' (Tab Peripheral) untuk masuk ke antrian peninjauan/afkir.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 1. Tanggal Mutasi
                    DateDropdownSelector(
                        selectedDateString = tanggalMutasi,
                        onDateChanged = { tanggalMutasi = it },
                        label = "Tanggal Mutasi / Tindakan *"
                    )

                    // 2. Catatan / Alasan Mutasi
                    OutlinedTextField(
                        value = catatanMutasi,
                        onValueChange = { catatanMutasi = it },
                        label = { Text("Catatan / Alasan Mutasi *", fontSize = 11.sp) },
                        placeholder = { Text("Contoh: Rusak total tidak dapat diperbaiki...") },
                        leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3,
                        shape = RoundedCornerShape(10.dp)
                    )

                    // 3. Nama Petugas
                    OutlinedTextField(
                        value = officerName,
                        onValueChange = { officerName = it },
                        label = { Text("Nama Petugas / Eksekutor *", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (catatanMutasi.isBlank()) {
                            Toast.makeText(context, "Catatan / Alasan Mutasi wajib diisi!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.updatePeripheralRusakStatus(
                            id = item.id,
                            newStatus = "Hapus Aset",
                            newDiagnosa = item.statusDiagnosa.ifBlank { "Siap Afkir" },
                            catatan = catatanMutasi,
                            officerName = officerName,
                            onSuccess = {
                                Toast.makeText(context, "Aset '${item.namaBarang}' berhasil dipindahkan ke Menu Hapus Aset!", Toast.LENGTH_SHORT).show()
                                itemToDelete = null
                            },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pindahkan ke Hapus Aset")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Confirmation Dialog for Quick Action "Normal"
    if (itemToMutasiNormal != null) {
        val item = itemToMutasiNormal!!
        var tanggalPemulihan by remember(item) {
            mutableStateOf(java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()))
        }
        var selectedKondisi by remember(item) {
            mutableStateOf(if (kondisiList.isNotEmpty()) kondisiList.first() else "Baik (Siap Pakai)")
        }
        var kondisiDropdownExpanded by remember { mutableStateOf(false) }
        var alasanKembali by remember(item) { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { itemToMutasiNormal = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Konfirmasi Pemulihan ke Stok", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Summary info card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = item.namaBarang,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF166534)
                            )
                            Text(
                                text = "Kategori: ${cleanCategoryText(item.subKategori)} | Qty: ${item.jumlah} ${getSatuanForItem(item, allPeripheralStocks)}",
                                fontSize = 12.sp,
                                color = Color(0xFF15803D)
                            )
                        }
                    }

                    // Field 1: Tanggal Pemulihan/Pengembalian (Date Dropdown Selector)
                    DateDropdownSelector(
                        selectedDateString = tanggalPemulihan,
                        onDateChanged = { tanggalPemulihan = it },
                        label = "Tanggal Pemulihan / Pengembalian *"
                    )

                    // Field 2: Kondisi Terbaru (Ketik Manual)
                    OutlinedTextField(
                        value = selectedKondisi,
                        onValueChange = { selectedKondisi = it },
                        label = { Text("Catatan Kondisi Terbaru *", fontSize = 12.sp) },
                        placeholder = { Text("Contoh: Baik (Siap Pakai), Normal, Normal Setelah Servis...", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null, tint = Color(0xFF10B981)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Field 3: Keterangan / Alasan Kembali (Mandatory)
                    OutlinedTextField(
                        value = alasanKembali,
                        onValueChange = { alasanKembali = it },
                        label = { Text("Keterangan / Alasan Kembali *", fontSize = 12.sp) },
                        placeholder = { Text("Jelaskan perbaikan/alasan barang dinyatakan normal", fontSize = 12.sp) },
                        minLines = 2,
                        maxLines = 4,
                        isError = alasanKembali.isBlank(),
                        supportingText = {
                            if (alasanKembali.isBlank()) {
                                Text("Wajib diisi!", color = MaterialTheme.colorScheme.error, fontSize = 10.sp)
                            } else {
                                Text("Akan dicatat ke log riwayat inventaris", fontSize = 10.sp, color = Color.Gray)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (alasanKembali.isBlank()) {
                            Toast.makeText(context, "Keterangan / Alasan Kembali wajib diisi!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.restorePeripheralToStock(
                            id = item.id,
                            recoveryDate = tanggalPemulihan,
                            newCondition = selectedKondisi,
                            reason = alasanKembali,
                            officerName = defaultOfficer,
                            onSuccess = {
                                Toast.makeText(context, "Peripheral '${item.namaBarang}' berhasil dipulihkan & dikembalikan ke stok!", Toast.LENGTH_SHORT).show()
                                itemToMutasiNormal = null
                            },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Kembalikan ke Stok")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToMutasiNormal = null }) {
                    Text("Batal")
                }
            }
        )
    }
}
}

// Physical Camera Barcode Scan Dialog (1D Barcode & QR Code)
@Composable
private fun PeripheralCameraScanDialog(
    onDismiss: () -> Unit,
    onBarcodeScanned: (String) -> Unit
) {
    CameraScannerDialog(
        title = "Pindai Barcode / QR Code Peripheral",
        initialMode = ScanMode.PRIMARY_QR,
        onDismissRequest = onDismiss,
        onCodeScannedWithMode = { scannedCode, _ ->
            onBarcodeScanned(scannedCode)
        }
    )
}

// Clean Minimalist Peripheral Rusak Card
@Composable
private fun PeripheralRusakCard(
    item: PeripheralRusakEntity,
    allPeripheralStocks: List<PeripheralStockEntity>,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onQuickNormal: () -> Unit,
    onDeleteAsset: () -> Unit
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val statusBg = when {
        item.status.contains("Hapus", ignoreCase = true) || item.statusDiagnosa.contains("Afkir", ignoreCase = true) -> Color(0xFFF3E8FF)
        item.status.contains("Servis", ignoreCase = true) -> Color(0xFFFFFBEB)
        else -> Color(0xFFFEF2F2)
    }
    val statusText = when {
        item.status.contains("Hapus", ignoreCase = true) || item.statusDiagnosa.contains("Afkir", ignoreCase = true) -> Color(0xFF7E22CE)
        item.status.contains("Servis", ignoreCase = true) -> Color(0xFFD97706)
        else -> Color(0xFFDC2626)
    }

    val itemSatuan = getSatuanForItem(item, allPeripheralStocks)
    val cleanKet = cleanKeterangan(item.keteranganKerusakan)
    val cleanCat = cleanCategoryText(item.subKategori)

    val cardBorder = if (isSelected) {
        BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f) else Color(0xFFE9D5FF))
    }

    com.example.ui.components.LunarisCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFFFFFF)
        ),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Badge (Clean minimal without decorative icons)
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        cleanCat,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        item.statusDiagnosa,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = statusText)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                item.namaBarang,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Text(
                "ID: ${if (item.idBarang.isNotBlank()) item.idBarang else "-"} | Jumlah: ${item.jumlah} $itemSatuan",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )

            if (cleanKet.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Keterangan: $cleanKet",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Entry Date: ${item.tanggalKerusakan} • Oleh: ${item.namaPetugas}",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.outline),
                    fontSize = 11.sp
                )

                if (item.validationCount > 0) {
                    Surface(
                        color = Color(0xFFD1FAE5),
                        shape = CircleShape
                    ) {
                        Text(
                            "✓ Validasi ${item.validationCount}x",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF047857), fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tindakan Aset:",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.outline),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Normal / Kembalikan ke Stok - Pure Icon Button
                    IconButton(
                        onClick = onQuickNormal,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("btn_normal_peripheral_${item.idBarang}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Kembalikan Normal Ke Stok",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Hapus Aset / Afkir - Pure Icon Button
                    IconButton(
                        onClick = onDeleteAsset,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("btn_hapus_peripheral_${item.idBarang}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = "Hapus Aset",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
