package com.example.ui.screens
import com.example.ui.components.LunarisCard
import com.example.ui.components.LunarisTextField
import com.example.ui.components.LunarisDatePickerDialog

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.example.data.entity.DamagedItemEntity
import com.example.data.model.ItemWithStock
import com.example.ui.viewmodel.InventoryViewModel
import com.example.ui.theme.GlassWhiteMore
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.DeepPurpleText
import com.example.ui.theme.PastelLavender
import com.example.ui.theme.BackgroundLight
import com.example.ui.theme.CarbonBlackText
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PemeliharaanScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit,
    initialSelectedId: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allItems by viewModel.itemsWithStock.collectAsState()
    val alatItems = remember(allItems) {
        allItems.filter { !it.kategori.equals("Logistik", ignoreCase = true) }
    }
    val rawHistoryList by viewModel.allDamagedItems.collectAsState()
    val servisLuarList by viewModel.servisLuarItems.collectAsState()
    // Filter history specifically for internal maintenance items
    val historyList = remember(rawHistoryList) {
        rawHistoryList.filter {
            (it.status.equals("Pemeliharaan", ignoreCase = true) || it.status.equals("Servis Luar/Pemeliharaan", ignoreCase = true)) &&
            !it.status.contains("Servis Luar", ignoreCase = true)
        }
    }
    val defaultOfficerState by viewModel.defaultOfficer.collectAsState()
    val defaultOfficer = defaultOfficerState.ifBlank { "Administrator" }

    var showQrScanner by remember { mutableStateOf(false) }

    val userRole by viewModel.userRole.collectAsState()
    val canTambah = viewModel.isStudentPermissionGranted("pemeliharaan_tambah")
    val canView = viewModel.isStudentPermissionGranted("pemeliharaan_view")
    val canAccessPemeliharaan = viewModel.isStudentPermissionGranted("pemeliharaan") && (canTambah || canView)

    // Tab state
    var selectedTabState by remember { mutableStateOf(0) }

    LaunchedEffect(userRole, canTambah, canView) {
        if (userRole.contains("siswa", ignoreCase = true)) {
            if (!canTambah && canView) {
                selectedTabState = 1
            } else if (canTambah && !canView) {
                selectedTabState = 0
            }
        }
    }

    // Form inputs
    var selectedObjekCategory by remember { mutableStateOf("Alat") } // "Alat" or "Komputer"
    var selectedPc by remember { mutableStateOf<PcUnitData?>(null) }
    var tipePemeliharaanInput by remember { mutableStateOf("Pemeliharaan Internal") }
    var alatSearchQuery by remember { mutableStateOf("") }
    var selectedAlat by remember { mutableStateOf<ItemWithStock?>(null) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val defaultPcUnits = remember(allItems) {
        allItems.filter { item ->
            item.type.equals("LABKOM", ignoreCase = true) ||
            item.idBarang.startsWith("PC-LAB", ignoreCase = true) ||
            item.idBarang.startsWith("LAB-", ignoreCase = true) ||
            item.kategori.contains("LabKom", ignoreCase = true) ||
            item.namaBarang.contains("PC Lab", ignoreCase = true) ||
            item.namaBarang.contains("Workstation", ignoreCase = true)
        }.map { item ->
            PcUnitData(
                id = item.idBarang,
                name = item.namaBarang,
                jenisPerangkat = item.kategori.ifBlank { "PC" },
                serialNumber = item.serialNumber,
                labRoom = item.ruang.ifBlank { "Lab Komputer 1" },
                status = item.kondisi.ifBlank { "Baik / Normal" },
                qty = if (item.stokAwal > 0) item.stokAwal else 1,
                satuan = item.satuan.ifBlank { "Unit" },
                keterangan = item.keterangan
            )
        }
    }

    val filteredPcUnits = remember(alatSearchQuery, defaultPcUnits) {
        if (alatSearchQuery.isBlank()) {
            defaultPcUnits
        } else {
            defaultPcUnits.filter { 
                it.name.contains(alatSearchQuery, ignoreCase = true) || 
                it.id.contains(alatSearchQuery, ignoreCase = true) ||
                it.serialNumber.contains(alatSearchQuery, ignoreCase = true)
            }
        }
    }

    var jumlahPemeliharaanInput by remember { mutableStateOf("") }
    var serialNumberInput by remember { mutableStateOf("") }
    var showSnScannerDialog by remember { mutableStateOf(false) }
    var catatanInput by remember { mutableStateOf("") }
    var petugasInput by remember { mutableStateOf(defaultOfficer) }

    LaunchedEffect(defaultOfficer) {
        if (petugasInput.isBlank()) {
            petugasInput = defaultOfficer
        }
    }

    // Auto pre-fill item data when navigated with an initial itemId/selectedId
    LaunchedEffect(initialSelectedId, allItems) {
        if (!initialSelectedId.isNullOrBlank() && allItems.isNotEmpty()) {
            val foundItem = allItems.find {
                it.idBarang.equals(initialSelectedId, ignoreCase = true) ||
                it.namaBarang.equals(initialSelectedId, ignoreCase = true)
            }
            if (foundItem != null) {
                selectedAlat = foundItem
                selectedObjekCategory = if (foundItem.kategori.contains("Komputer", ignoreCase = true) || foundItem.kategori.contains("LabKom", ignoreCase = true)) "Komputer" else "Alat"
                alatSearchQuery = foundItem.namaBarang
                serialNumberInput = foundItem.serialNumber ?: ""
                if (jumlahPemeliharaanInput.isBlank() || jumlahPemeliharaanInput == "0") {
                    jumlahPemeliharaanInput = "1"
                }
                if (catatanInput.isBlank()) {
                    catatanInput = "Pemeliharaan & perawatan berkala"
                }
                selectedTabState = 0
            }
        }
    }

    // DatePicker setup
    val calendar = Calendar.getInstance()
    var selectedDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)) }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    if (showDatePickerDialog) {
        LunarisDatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            selectedDateString = selectedDate,
            onDateSelected = { newDate ->
                selectedDate = newDate
            }
        )
    }

    // Form validation
    val stokTersedia = selectedAlat?.stokTersedia ?: 0
    val maxAllowedStock = remember(selectedAlat) {
        selectedAlat?.let { maxOf(it.stokTersedia, it.stokAwal, 1) } ?: 1
    }
    val jumlahPemeliharaan = jumlahPemeliharaanInput.toIntOrNull() ?: 0
    val isJumlahInvalid = remember(jumlahPemeliharaanInput, selectedAlat, maxAllowedStock) {
        if (jumlahPemeliharaanInput.isEmpty()) false
        else {
            jumlahPemeliharaan <= 0 || (selectedAlat != null && jumlahPemeliharaan > maxAllowedStock)
        }
    }

    val canSubmit = selectedAlat != null &&
            jumlahPemeliharaan > 0 &&
            jumlahPemeliharaan <= maxAllowedStock &&
            !isJumlahInvalid &&
            catatanInput.isNotBlank() &&
            petugasInput.isNotBlank()

    // Filtered assets suggestions for searchable dropdown
    val filteredAlat = remember(alatSearchQuery, alatItems) {
        if (alatSearchQuery.isBlank()) {
            alatItems
        } else {
            alatItems.filter { it.namaBarang.contains(alatSearchQuery, ignoreCase = true) }
        }
    }

    // Advanced Data Table States
    var historySearchQuery by remember { mutableStateOf("") }
    var showHistoryQrScanner by remember { mutableStateOf(false) }

    // Confirm dialog states
    var itemToSelesai by remember { mutableStateOf<DamagedItemEntity?>(null) }
    var itemToRusakBack by remember { mutableStateOf<DamagedItemEntity?>(null) }
    var itemToDeletePermanently by remember { mutableStateOf<DamagedItemEntity?>(null) }

    // Broken action notes state
    var brokenNote by remember { mutableStateOf("") }
    var brokenOfficer by remember { mutableStateOf(defaultOfficer) }

    // Filter history based on search query
    val filteredHistory = remember(historyList, historySearchQuery) {
        if (historySearchQuery.isBlank()) {
            historyList
        } else {
            historyList.filter {
                it.namaBarang.contains(historySearchQuery, ignoreCase = true) ||
                it.idBarang.contains(historySearchQuery, ignoreCase = true) ||
                it.keteranganKerusakan.contains(historySearchQuery, ignoreCase = true) ||
                it.statusKeterangan.contains(historySearchQuery, ignoreCase = true) ||
                it.status.contains(historySearchQuery, ignoreCase = true)
            }
        }
    }

    val isDark = false
    val topBarGradient = if (isDark) {
        Brush.horizontalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.surface
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(Color(0xFFE9D5FF), Color(0xFFBFDBFE))
        )
    }
    val appBarContentColor = if (isDark) MaterialTheme.colorScheme.onSurface else DeepPurpleText
    val selectedTabColor = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText
    val unselectedTabColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else Color.Gray.copy(alpha = 0.8f)
    val dividerColor = MaterialTheme.colorScheme.outlineVariant

    // Main layout
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
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                        )
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                        .background(
                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                colors = listOf(
                                    androidx.compose.ui.graphics.Color(0xFFF7E0FF),
                                    androidx.compose.ui.graphics.Color(0xFFBAE7FF)
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
                                    tint = androidx.compose.ui.graphics.Color(0xFF0F172A),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = "Kelola Pemeliharaan",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = androidx.compose.ui.graphics.Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Tambah pemeliharaan, list pemeliharaan, dan servis luar",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = androidx.compose.ui.graphics.Color(0xFF1E293B)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Sub-Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 1.dp)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(vertical = 4.dp)
                ) {
                    TabRow(
                        selectedTabIndex = selectedTabState,
                        containerColor = Color.Transparent,
                        divider = {},
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTabState]),
                                height = 3.dp,
                                color = selectedTabColor
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Tab(
                            selected = selectedTabState == 0,
                            onClick = { selectedTabState = 0 },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Build,
                                        contentDescription = null,
                                        tint = if (selectedTabState == 0) selectedTabColor else unselectedTabColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Tambah Pemeliharaan",
                                        fontWeight = if (selectedTabState == 0) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedTabState == 0) selectedTabColor else unselectedTabColor,
                                        fontSize = 13.sp
                                    )
                                }
                            },
                            modifier = Modifier.testTag("tab_tambah_pemeliharaan")
                        )
                        Tab(
                            selected = selectedTabState == 1,
                            onClick = { selectedTabState = 1 },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ListAlt,
                                        contentDescription = null,
                                        tint = if (selectedTabState == 1) selectedTabColor else unselectedTabColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "List Pemeliharaan",
                                        fontWeight = if (selectedTabState == 1) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedTabState == 1) selectedTabColor else unselectedTabColor,
                                        fontSize = 13.sp
                                    )
                                }
                            },
                            modifier = Modifier.testTag("tab_riwayat_pemeliharaan")
                        )
                        Tab(
                            selected = selectedTabState == 2,
                            onClick = { selectedTabState = 2 },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Engineering,
                                        contentDescription = null,
                                        tint = if (selectedTabState == 2) selectedTabColor else unselectedTabColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Servis Luar",
                                        fontWeight = if (selectedTabState == 2) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedTabState == 2) selectedTabColor else unselectedTabColor,
                                        fontSize = 13.sp
                                    )
                                }
                            },
                            modifier = Modifier.testTag("tab_servis_luar")
                        )
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
            when (selectedTabState) {
                0 -> {
                    // Form Tab Content
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            LunarisCard(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f) else Color(0xFFCBD5E1)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text(
                                        text = "Form Tambah Pemeliharaan",
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) MaterialTheme.colorScheme.onSurface else DeepPurpleText,
                                        fontSize = 18.sp
                                    )

                                    // Pemilih Kategori Objek Perangkat (Segmented Switch)
                                    Text(
                                        text = "Pilih Kategori Objek Perangkat",
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else DeepPurpleText,
                                        fontSize = 13.sp
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF1F5F9))
                                            .padding(4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        val isAlatSelected = selectedObjekCategory == "Alat"
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(42.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isAlatSelected) Color(0xFF7C3AED) else Color.Transparent)
                                                .clickable {
                                                    selectedObjekCategory = "Alat"
                                                    selectedAlat = null
                                                    selectedPc = null
                                                    alatSearchQuery = ""
                                                    serialNumberInput = ""
                                                }
                                                .testTag("tab_kat_alat"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Icon(
                                                    imageVector = Icons.Default.Build,
                                                    contentDescription = null,
                                                    tint = if (isAlatSelected) Color.White else Color.Gray,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = "Alat / Aset",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = if (isAlatSelected) Color.White else Color.Gray
                                                )
                                            }
                                        }

                                        val isKomputerSelected = selectedObjekCategory == "Komputer"
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(42.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isKomputerSelected) Color(0xFF7C3AED) else Color.Transparent)
                                                .clickable {
                                                    selectedObjekCategory = "Komputer"
                                                    selectedAlat = null
                                                    selectedPc = null
                                                    alatSearchQuery = ""
                                                    serialNumberInput = ""
                                                }
                                                .testTag("tab_kat_komputer"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Icon(
                                                    imageVector = Icons.Default.Computer,
                                                    contentDescription = null,
                                                    tint = if (isKomputerSelected) Color.White else Color.Gray,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = "Komputer / LabKom",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = if (isKomputerSelected) Color.White else Color.Gray
                                                )
                                            }
                                        }
                                    }

                                    // Section 1: Identitas & Pilihan Alat
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Build,
                                            contentDescription = null,
                                            tint = DeepPurpleText,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Identitas & Pilihan Alat",
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) MaterialTheme.colorScheme.onSurface else DeepPurpleText,
                                            fontSize = 15.sp
                                        )
                                    }

                                    // Jenis Pemeliharaan
                                    Text(
                                        text = "Kategori / Jenis Pemeliharaan",
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else DeepPurpleText,
                                        fontSize = 13.sp
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilterChip(
                                            selected = tipePemeliharaanInput == "Pemeliharaan Internal",
                                            onClick = { tipePemeliharaanInput = "Pemeliharaan Internal" },
                                            label = { Text("Pemeliharaan Internal", fontSize = 13.sp) },
                                            leadingIcon = if (tipePemeliharaanInput == "Pemeliharaan Internal") {
                                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                            } else null,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        )
                                        FilterChip(
                                            selected = tipePemeliharaanInput == "Servis Luar",
                                            onClick = { tipePemeliharaanInput = "Servis Luar" },
                                            label = { Text("Servis Luar", fontSize = 13.sp) },
                                            leadingIcon = if (tipePemeliharaanInput == "Servis Luar") {
                                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                            } else null,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    // Input A: Pilih Alat
                                    Text(
                                        text = "Pilih Alat",
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else DeepPurpleText,
                                        fontSize = 13.sp
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            LunarisTextField(
                                                value = if (selectedAlat != null) selectedAlat!!.namaBarang else alatSearchQuery,
                                                onValueChange = {
                                                    alatSearchQuery = it
                                                    selectedAlat = null
                                                    dropdownExpanded = true
                                                },
                                                placeholder = { Text("Ketik nama alat...") },
                                                trailingIcon = {
                                                    IconButton(onClick = { dropdownExpanded = !dropdownExpanded }) {
                                                        Icon(
                                                            imageVector = if (dropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                                            contentDescription = "Pilih"
                                                        )
                                                    }
                                                },
                                                singleLine = true,
                                                shape = RoundedCornerShape(16.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF7C3AED),
                                                    unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else Color.Gray.copy(alpha = 0.4f),
                                                    focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                                    unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("maint_select_input")
                                            )

                                            DropdownMenu(
                                                expanded = dropdownExpanded,
                                                onDismissRequest = { dropdownExpanded = false },
                                                properties = PopupProperties(focusable = false),
                                                modifier = Modifier.fillMaxWidth(0.85f)
                                            ) {
                                                if (selectedObjekCategory == "Alat") {
                                                    if (filteredAlat.isEmpty()) {
                                                        DropdownMenuItem(
                                                            text = { Text("Alat tidak ditemukan") },
                                                            onClick = { dropdownExpanded = false }
                                                        )
                                                    } else {
                                                        filteredAlat.forEach { item ->
                                                            DropdownMenuItem(
                                                                text = { Text("${item.namaBarang} (Stok Ready: ${item.stokTersedia} ${item.satuan})") },
                                                                onClick = {
                                                                    selectedAlat = item
                                                                    selectedPc = null
                                                                    alatSearchQuery = item.namaBarang
                                                                    serialNumberInput = item.serialNumber ?: ""
                                                                    if (jumlahPemeliharaanInput.isBlank() || jumlahPemeliharaanInput == "0") {
                                                                        jumlahPemeliharaanInput = "1"
                                                                    }
                                                                    if (catatanInput.isBlank()) {
                                                                        catatanInput = "Pemeliharaan & perawatan berkala"
                                                                    }
                                                                    dropdownExpanded = false
                                                                }
                                                            )
                                                        }
                                                    }
                                                } else {
                                                    if (filteredPcUnits.isEmpty()) {
                                                        DropdownMenuItem(
                                                            text = { Text("Perangkat PC tidak ditemukan") },
                                                            onClick = { dropdownExpanded = false }
                                                        )
                                                    } else {
                                                        filteredPcUnits.forEach { pc ->
                                                            DropdownMenuItem(
                                                                text = { Text("${pc.name} - ${pc.labRoom} [${pc.serialNumber}]") },
                                                                onClick = {
                                                                    selectedPc = pc
                                                                    alatSearchQuery = pc.name
                                                                    serialNumberInput = pc.serialNumber
                                                                    jumlahPemeliharaanInput = "1"
                                                                    selectedAlat = ItemWithStock(
                                                                        idBarang = pc.id,
                                                                        namaBarang = pc.name,
                                                                        kategori = pc.jenisPerangkat,
                                                                        stokTersedia = pc.qty,
                                                                        stokAwal = pc.qty,
                                                                        satuan = pc.satuan,
                                                                        kondisi = pc.status,
                                                                        ruang = pc.labRoom
                                                                    )
                                                                    dropdownExpanded = false
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // QR Code Scanner button
                                        IconButton(
                                            onClick = { showQrScanner = true },
                                            modifier = Modifier
                                                .size(54.dp)
                                                .background(
                                                    if (isDark) MaterialTheme.colorScheme.primaryContainer else Color(0xFFF3E8FF),
                                                    RoundedCornerShape(16.dp)
                                                )
                                                .testTag("btn_maint_qr")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.QrCodeScanner,
                                                contentDescription = "Pindai QR",
                                                tint = if (isDark) MaterialTheme.colorScheme.onPrimaryContainer else DeepPurpleText
                                            )
                                        }
                                    }

                                    if (selectedAlat != null) {
                                        Text(
                                            text = "Stok Tersedia: ${selectedAlat!!.stokTersedia} ${selectedAlat!!.satuan}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedAlat!!.stokTersedia > 0) Color(0xFF059669) else Color.Red,
                                            modifier = Modifier.padding(start = 4.dp)
                                        )
                                    }

                                    // Row 1: Jumlah Alat & Tanggal Pemeliharaan
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        // Input B: Jumlah Alat
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Jumlah Alat",
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else DeepPurpleText,
                                                fontSize = 13.sp,
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            )
                                            LunarisTextField(
                                                value = jumlahPemeliharaanInput,
                                                onValueChange = { jumlahPemeliharaanInput = it },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                placeholder = { Text("Jumlah alat") },
                                                singleLine = true,
                                                isError = isJumlahInvalid,
                                                shape = RoundedCornerShape(16.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF7C3AED),
                                                    unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else Color.Gray.copy(alpha = 0.4f),
                                                    focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                                    unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                                                ),
                                                supportingText = {
                                                    if (isJumlahInvalid) {
                                                        Text(
                                                            "Jumlah harus > 0 dan tidak boleh melebihi stok tersedia ($stokTersedia)!",
                                                            color = MaterialTheme.colorScheme.error,
                                                            fontSize = 11.sp
                                                        )
                                                    }
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("maint_jumlah_input")
                                            )
                                        }

                                        // Input D: Tanggal Pemeliharaan
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Tanggal Pemeliharaan",
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else DeepPurpleText,
                                                fontSize = 13.sp,
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            )
                                            LunarisTextField(
                                                value = selectedDate,
                                                onValueChange = {},
                                                readOnly = true,
                                                shape = RoundedCornerShape(16.dp),
                                                trailingIcon = {
                                                    IconButton(onClick = { showDatePickerDialog = true }) {
                                                        Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = "Pilih Tanggal")
                                                    }
                                                },
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF7C3AED),
                                                    unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else Color.Gray.copy(alpha = 0.4f),
                                                    focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                                    unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { showDatePickerDialog = true }
                                                    .testTag("maint_tanggal_input")
                                            )
                                        }
                                    }

                                    HorizontalDivider(color = Color(0xFFE2E8F0))

                                    // Section 2: Pemindai & Serial Number / Barcode
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.QrCodeScanner,
                                            contentDescription = null,
                                            tint = DeepPurpleText,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Pemindai & Serial Number / Barcode",
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) MaterialTheme.colorScheme.onSurface else DeepPurpleText,
                                            fontSize = 15.sp
                                        )
                                    }

                                    Text(
                                        text = "Serial Number (SN) / Barcode (Opsional)",
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else DeepPurpleText,
                                        fontSize = 13.sp
                                    )
                                    LunarisTextField(
                                        value = serialNumberInput,
                                        onValueChange = { serialNumberInput = it },
                                        placeholder = { Text("Ketik atau pindai SN / Barcode...") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp),
                                        trailingIcon = {
                                            IconButton(onClick = { showSnScannerDialog = true }) {
                                                Icon(
                                                    imageVector = Icons.Default.PhotoCamera,
                                                    contentDescription = "Pindai Kamera SN",
                                                    tint = DeepPurpleText
                                                )
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().testTag("maint_sn_input")
                                    )

                                    HorizontalDivider(color = Color(0xFFE2E8F0))

                                    // Section 3: Informasi Petugas Penanggung Jawab
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Badge,
                                            contentDescription = null,
                                            tint = DeepPurpleText,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Informasi Petugas Penanggung Jawab",
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) MaterialTheme.colorScheme.onSurface else DeepPurpleText,
                                            fontSize = 15.sp
                                        )
                                    }

                                    Text(
                                        text = "Nama Petugas",
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else DeepPurpleText,
                                        fontSize = 13.sp
                                    )
                                    LunarisTextField(
                                        value = petugasInput,
                                        onValueChange = { petugasInput = it },
                                        placeholder = { Text("Nama petugas penanggung jawab") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("maint_petugas_input")
                                    )

                                    HorizontalDivider(color = Color(0xFFE2E8F0))

                                    // Section 4: Detail Catatan Pemeliharaan
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Description,
                                            contentDescription = null,
                                            tint = DeepPurpleText,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Detail Catatan Pemeliharaan",
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) MaterialTheme.colorScheme.onSurface else DeepPurpleText,
                                            fontSize = 15.sp
                                        )
                                    }

                                    Text(
                                        text = "Catatan Pemeliharaan",
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else DeepPurpleText,
                                        fontSize = 13.sp
                                    )
                                    LunarisTextField(
                                        value = catatanInput,
                                        onValueChange = { catatanInput = it },
                                        placeholder = { Text("Contoh: Kalibrasi ulang multimeter, perbaikan sensor suhu") },
                                        minLines = 2,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("maint_catatan_input")
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Submit Button
                                    Button(
                                        onClick = {
                                            val tool = selectedAlat!!
                                            val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
                                            val currentTime = timeFormat.format(Date())

                                            viewModel.recordDamagedReport(
                                                idBarang = tool.idBarang,
                                                namaBarang = tool.namaBarang,
                                                jumlah = jumlahPemeliharaan,
                                                tanggalKerusakan = selectedDate,
                                                waktuKerusakan = currentTime,
                                                keteranganKerusakan = if (serialNumberInput.isNotBlank()) "SN: $serialNumberInput | $catatanInput" else catatanInput,
                                                namaPetugas = petugasInput,
                                                kondisiBaru = if (tipePemeliharaanInput == "Servis Luar") "Servis Luar" else "Perbaikan",
                                                status = if (tipePemeliharaanInput == "Servis Luar") "Servis Luar" else "Pemeliharaan",
                                                onSuccess = {
                                                    Toast.makeText(context, "Aset berhasil dikirim ke ${if (tipePemeliharaanInput == "Servis Luar") "Servis Luar" else "Pemeliharaan"}!", Toast.LENGTH_LONG).show()
                                                    // Reset Form
                                                    selectedAlat = null
                                                    alatSearchQuery = ""
                                                    jumlahPemeliharaanInput = ""
                                                    serialNumberInput = ""
                                                    catatanInput = ""
                                                    petugasInput = defaultOfficer
                                                    // Move to History Tab
                                                    selectedTabState = if (tipePemeliharaanInput == "Servis Luar") 2 else 1
                                                },
                                                onError = { err ->
                                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                                }
                                            )
                                        },
                                        enabled = canSubmit,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary,
                                            disabledContainerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.Gray.copy(alpha = 0.3f),
                                            disabledContentColor = if (isDark) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.6f)
                                        ),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .testTag("btn_submit_maint")
                                    ) {
                                        Text(
                                            text = "Kirim ke Pemeliharaan",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // History Tab Content (Data Table with Search, Pagination, Row Controller)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 0.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Search Bar & Row Controller Row
                        LunarisTextField(
                            value = historySearchQuery,
                            onValueChange = { 
                                historySearchQuery = it
                            },
                            placeholder = { Text("Ketik untuk mencari...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cari") },
                            trailingIcon = {
                                IconButton(onClick = { showHistoryQrScanner = true }) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = "Scan QR",
                                        tint = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF7C3AED)
                                    )
                                }
                            },
                            singleLine = true,
                            isStaticOutline = false,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF7C3AED),
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("maint_search_bar")
                        )

                        if (showHistoryQrScanner) {
                            SearchQrScanDialog(
                                onDismiss = { showHistoryQrScanner = false },
                                onQrScanned = { scannedCode ->
                                    showHistoryQrScanner = false
                                    historySearchQuery = scannedCode
                                }
                            )
                        }

                        // Data Table Content
                        if (filteredHistory.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Tidak ada riwayat pemeliharaan yang cocok.",
                                    color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else Color.Gray,
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 300.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(filteredHistory, key = { it.id }) { item ->
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.TopStart
                                    ) {
                                        LunarisCard(
                                            shape = RoundedCornerShape(14.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White
                                            ),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                            border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f) else Color(0xFFCBD5E1)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .widthIn(max = 400.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = item.namaBarang,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 14.sp,
                                                            color = if (isDark) MaterialTheme.colorScheme.onSurface else CarbonBlackText
                                                        )
                                                        Text(
                                                            text = "ID: ${item.idBarang} | Jumlah: ${item.jumlah} Pcs",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f) else Color.Gray
                                                        )
                                                    }

                                                    // Status Indicator Text-only
                                                    Text(
                                                        text = "Pemeliharaan",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF2563EB)
                                                    )
                                                }

                                                Text(
                                                    text = "Keterangan: ${item.keteranganKerusakan}",
                                                    fontSize = 12.sp,
                                                    color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.DarkGray
                                                )
                                                if (item.statusKeterangan.isNotBlank()) {
                                                    Text(
                                                        text = "Catatan Status: ${item.statusKeterangan}",
                                                        fontSize = 12.sp,
                                                        color = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF2563EB),
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Tgl: ${item.tanggalKerusakan} (${item.namaPetugas})",
                                                        fontSize = 11.sp,
                                                        color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) else Color.Gray
                                                    )

                                                    // Actions Buttons Row
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        // Selesai (Kembali ke Stok Utama)
                                                        IconButton(
                                                            onClick = { itemToSelesai = item },
                                                            modifier = Modifier
                                                                .size(36.dp)
                                                                .testTag("btn_maint_done_${item.id}")
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = "Selesai (Kembalikan ke Stok)",
                                                                tint = if (isDark) Color(0xFF34D399) else Color(0xFF10B981),
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }

                                                        // Rusak (Pindahkan ke Alat Rusak)
                                                        IconButton(
                                                            onClick = { 
                                                                itemToRusakBack = item
                                                                brokenNote = ""
                                                                brokenOfficer = defaultOfficer
                                                            },
                                                            modifier = Modifier
                                                                .size(36.dp)
                                                                .testTag("btn_maint_broken_${item.id}")
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Warning,
                                                                contentDescription = "Pindahkan ke Alat Rusak",
                                                                tint = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }

                                                        // Hapus Permanen
                                                        IconButton(
                                                            onClick = { itemToDeletePermanently = item },
                                                            modifier = Modifier
                                                                .size(36.dp)
                                                                .testTag("btn_maint_hapus_${item.id}")
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Delete,
                                                                contentDescription = "Hapus Permanen",
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
                            }
                        }
                    }
                }
                2 -> {
                    // Servis Luar Tab Content
                    val filteredServisLuar = remember(servisLuarList, historySearchQuery) {
                        if (historySearchQuery.isBlank()) {
                            servisLuarList
                        } else {
                            servisLuarList.filter {
                                it.namaBarang.contains(historySearchQuery, ignoreCase = true) ||
                                it.idBarang.contains(historySearchQuery, ignoreCase = true) ||
                                it.keteranganKerusakan.contains(historySearchQuery, ignoreCase = true) ||
                                it.namaPetugas.contains(historySearchQuery, ignoreCase = true)
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 0.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LunarisTextField(
                            value = historySearchQuery,
                            onValueChange = { historySearchQuery = it },
                            placeholder = { Text("Ketik untuk mencari...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cari") },
                            trailingIcon = {
                                IconButton(onClick = { showHistoryQrScanner = true }) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = "Scan QR",
                                        tint = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF7C3AED)
                                    )
                                }
                            },
                            singleLine = true,
                            isStaticOutline = false,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF7C3AED),
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC)
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("servis_luar_search_bar")
                        )

                        if (filteredServisLuar.isEmpty()) {
                            Box(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Tidak ada alat dalam daftar Servis Luar.",
                                    color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else Color.Gray,
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 300.dp),
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentPadding = PaddingValues(bottom = 80.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filteredServisLuar, key = { it.id }) { item ->
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.TopStart
                                    ) {
                                        LunarisCard(
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White
                                            ),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                            border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f) else Color(0xFFCBD5E1)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .widthIn(max = 400.dp)
                                                .padding(vertical = 4.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = item.namaBarang,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 14.sp,
                                                            color = if (isDark) MaterialTheme.colorScheme.onSurface else CarbonBlackText
                                                        )
                                                        Text(
                                                            text = "ID: ${item.idBarang} | Jumlah: ${item.jumlah} Pcs",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f) else Color.Gray
                                                        )
                                                    }

                                                    Text(
                                                        text = "Servis Luar",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = Color(0xFFD97706)
                                                    )
                                                }

                                                Text(
                                                    text = "Vendor/Catatan: ${item.keteranganKerusakan.ifBlank { "-" }}",
                                                    fontSize = 12.sp,
                                                    color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.DarkGray
                                                )

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Tgl Kirim: ${item.tanggalKerusakan} (${item.namaPetugas})",
                                                        fontSize = 11.sp,
                                                        color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) else Color.Gray
                                                    )

                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        // Selesai (Kembali Normal)
                                                        IconButton(
                                                            onClick = { itemToSelesai = item },
                                                            modifier = Modifier
                                                                .size(36.dp)
                                                                .testTag("btn_servis_done_${item.id}")
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = "Selesai (Kembali Normal)",
                                                                tint = if (isDark) Color(0xFF34D399) else Color(0xFF10B981),
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }

                                                        // Ubah ke Rusak
                                                        IconButton(
                                                            onClick = {
                                                                itemToRusakBack = item
                                                                brokenNote = ""
                                                                brokenOfficer = defaultOfficer
                                                            },
                                                            modifier = Modifier
                                                                .size(36.dp)
                                                                .testTag("btn_servis_broken_${item.id}")
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Warning,
                                                                contentDescription = "Pindahkan ke Alat Rusak",
                                                                tint = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
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
                        }
                    }
                }
            }
        }

        // QR Scanner Dialog
        if (showQrScanner) {
            SearchQrScanDialog(
                onDismiss = { showQrScanner = false },
                onQrScanned = { scannedCode ->
                    showQrScanner = false
                    if (selectedObjekCategory == "Alat") {
                        val matched = alatItems.find { 
                            it.idBarang.equals(scannedCode, ignoreCase = true) || 
                            (it.serialNumber != null && it.serialNumber.equals(scannedCode, ignoreCase = true)) || 
                            it.namaBarang.contains(scannedCode, ignoreCase = true) 
                        }
                        if (matched != null) {
                            selectedAlat = matched
                            selectedPc = null
                            alatSearchQuery = matched.namaBarang
                            serialNumberInput = matched.serialNumber ?: scannedCode
                            jumlahPemeliharaanInput = "1"
                            Toast.makeText(context, "Alat '${matched.namaBarang}' terdeteksi secara otomatis!", Toast.LENGTH_SHORT).show()
                        } else {
                            serialNumberInput = scannedCode
                            alatSearchQuery = scannedCode
                            jumlahPemeliharaanInput = "1"
                            Toast.makeText(context, "Data SN/QR terdeteksi: $scannedCode", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val matchedPc = defaultPcUnits.find { 
                            it.id.equals(scannedCode, ignoreCase = true) || 
                            it.serialNumber.equals(scannedCode, ignoreCase = true) || 
                            it.name.contains(scannedCode, ignoreCase = true) 
                        }
                        if (matchedPc != null) {
                            selectedPc = matchedPc
                            selectedAlat = ItemWithStock(
                                idBarang = matchedPc.id,
                                namaBarang = matchedPc.name,
                                kategori = matchedPc.jenisPerangkat,
                                stokTersedia = matchedPc.qty,
                                stokAwal = matchedPc.qty,
                                satuan = matchedPc.satuan,
                                kondisi = matchedPc.status,
                                ruang = matchedPc.labRoom
                            )
                            alatSearchQuery = matchedPc.name
                            serialNumberInput = matchedPc.serialNumber
                            jumlahPemeliharaanInput = "1"
                            Toast.makeText(context, "Komputer '${matchedPc.name}' terdeteksi secara otomatis!", Toast.LENGTH_SHORT).show()
                        } else {
                            serialNumberInput = scannedCode
                            alatSearchQuery = scannedCode
                            jumlahPemeliharaanInput = "1"
                            Toast.makeText(context, "Data SN/QR Komputer terdeteksi: $scannedCode", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }

        // SN / Barcode Camera Scanner Dialog
        if (showSnScannerDialog) {
            SearchQrScanDialog(
                onDismiss = { showSnScannerDialog = false },
                onQrScanned = { scannedCode ->
                    serialNumberInput = scannedCode
                    showSnScannerDialog = false
                    Toast.makeText(context, "SN/Barcode terdeteksi: $scannedCode", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Dialog 1: Confirmation to complete maintenance (Selesai -> Normal)
        if (itemToSelesai != null) {
            AlertDialog(
                onDismissRequest = { itemToSelesai = null },
                shape = RoundedCornerShape(16.dp),
                title = { Text("Konfirmasi Pemeliharaan Selesai", fontWeight = FontWeight.Bold) },
                text = { 
                    Text("Apakah Anda yakin pemeliharaan untuk alat '${itemToSelesai!!.namaBarang}' sudah selesai?\n\nAlat sebanyak ${itemToSelesai!!.jumlah} unit akan ditarik dari daftar pemeliharaan dan dimasukkan kembali ke stok aktif.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val record = itemToSelesai!!
                            itemToSelesai = null
                            viewModel.updateDamagedStatus(
                                damagedId = record.id,
                                newStatus = "Normal (Tersedia)",
                                alasan = "Pemeliharaan/Perbaikan Selesai - Alat dikembalikan ke stok aktif",
                                namaPetugas = defaultOfficer,
                                onSuccess = {
                                    Toast.makeText(context, "Pemeliharaan selesai! Alat kembali ke stok aktif dan dicatat di log transaksi.", Toast.LENGTH_LONG).show()
                                },
                                onError = { err ->
                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("Selesai & Kembalikan", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { itemToSelesai = null }) {
                        Text("Batal")
                    }
                }
            )
        }

        // Dialog 2: Confirmation & Note Input to move back to Alat Rusak (Rusak)
        if (itemToRusakBack != null) {
            AlertDialog(
                onDismissRequest = { itemToRusakBack = null },
                shape = RoundedCornerShape(16.dp),
                title = { Text("Kirim Kembali ke Alat Rusak", fontWeight = FontWeight.Bold) },
                text = { 
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Silakan tuliskan kronologi/alasan pemindahan alat '${itemToRusakBack!!.namaBarang}' kembali ke status Rusak (perlu tindakan):")
                        
                        LunarisTextField(
                            value = brokenNote,
                            onValueChange = { brokenNote = it },
                            placeholder = { Text("Contoh: perbaikan gagal, kerusakan bertambah parah") },
                            label = { Text("Alasan Kembali Rusak") },
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF7C3AED),
                                unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else Color.Gray.copy(alpha = 0.4f),
                                focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        LunarisTextField(
                            value = brokenOfficer,
                            onValueChange = { brokenOfficer = it },
                            placeholder = { Text("Nama petugas pelapor") },
                            label = { Text("Petugas") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF7C3AED),
                                unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else Color.Gray.copy(alpha = 0.4f),
                                focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val record = itemToRusakBack!!
                            itemToRusakBack = null
                            val note = brokenNote.ifBlank { "Pemeliharaan gagal / dibatalkan" }
                            viewModel.updateDamagedStatus(
                                damagedId = record.id,
                                newStatus = "Rusak (Perlu Tindakan)",
                                alasan = note,
                                namaPetugas = brokenOfficer,
                                onSuccess = {
                                    Toast.makeText(context, "Alat berhasil dikembalikan ke modul Alat Rusak!", Toast.LENGTH_LONG).show()
                                },
                                onError = { err ->
                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                    ) {
                        Text("Kirim ke Alat Rusak", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { itemToRusakBack = null }) {
                        Text("Batal")
                    }
                }
            )
        }

        // Dialog 3: Permanent physical delete confirmation (Hapus)
        if (itemToDeletePermanently != null) {
            AlertDialog(
                onDismissRequest = { itemToDeletePermanently = null },
                shape = RoundedCornerShape(16.dp),
                title = { Text("Hapus Permanen", fontWeight = FontWeight.Bold, color = Color(0xFFEF4444)) },
                text = { 
                    Text("Apakah Anda yakin ingin menghapus fisik alat '${itemToDeletePermanently!!.namaBarang}' sebanyak ${itemToDeletePermanently!!.jumlah} unit secara permanen?\n\nAksi ini akan mengurangi jumlah total aset fisik Anda secara permanen. Tindakan ini akan dicatat di Log Transaksi untuk audit.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val record = itemToDeletePermanently!!
                            itemToDeletePermanently = null
                            viewModel.deleteDamagedItemPermanently(
                                id = record.id,
                                namaPetugas = defaultOfficer,
                                onSuccess = {
                                    Toast.makeText(context, "Aset pemeliharaan dihapus secara permanen & audit dicatat!", Toast.LENGTH_LONG).show()
                                },
                                onError = { err ->
                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                    ) {
                        Text("Hapus Permanen", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { itemToDeletePermanently = null }) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}
