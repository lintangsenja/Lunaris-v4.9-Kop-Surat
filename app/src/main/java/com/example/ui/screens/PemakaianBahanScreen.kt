package com.example.ui.screens
import com.example.ui.components.LunarisCard
import com.example.ui.components.LunarisTextField
import com.example.ui.components.CameraScannerDialog
import com.example.ui.components.ScanMode
import com.example.ui.components.FilterGroup
import com.example.ui.components.LunarisFilterDialog
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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.example.data.entity.CategoryEntity
import com.example.data.entity.PemakaianBahanEntity
import com.example.data.entity.UnitEntity
import com.example.data.model.ItemWithStock
import com.example.ui.viewmodel.InventoryViewModel
import com.example.ui.theme.GlassWhite
import com.example.ui.theme.GlassWhiteMore
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.DeepPurpleText
import com.example.ui.theme.PastelLavender
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PemakaianBahanScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
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
    val cardBgColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFFFFFF)
    val appBarContentColor = if (isDark) MaterialTheme.colorScheme.onSurface else DeepPurpleText
    val dividerColor = MaterialTheme.colorScheme.outlineVariant

    val userRole by viewModel.userRole.collectAsState()
    val canForm = viewModel.isStudentPermissionGranted("pemakaian_bahan_form")
    val canLog = viewModel.isStudentPermissionGranted("pemakaian_bahan_log")
    val canAccessPemakaian = viewModel.isStudentPermissionGranted("pemakaian_bahan") && (canForm || canLog)

    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(userRole, canForm, canLog) {
        if (userRole.contains("siswa", ignoreCase = true)) {
            if (!canForm && canLog) {
                selectedTab = 1
            } else if (canForm && !canLog) {
                selectedTab = 0
            }
        }
    }

    val allItems by viewModel.itemsWithStock.collectAsState()
    val logistikItems = remember(allItems) {
        allItems.filter { it.kategori.equals("Logistik", ignoreCase = true) }
    }
    val historyList by viewModel.allPemakaianBahan.collectAsState()
    
    var historySearchQuery by remember { mutableStateOf("") }
    var showHistoryQrScanner by remember { mutableStateOf(false) }
    var showHistoryFilterDialog by remember { mutableStateOf(false) }
    var appliedRoomFilter by remember { mutableStateOf("Semua") }
    var appliedSortFilter by remember { mutableStateOf("Terbaru") }
    var tempRoomFilter by remember { mutableStateOf("Semua") }
    var tempSortFilter by remember { mutableStateOf("Terbaru") }

    val filteredHistory = remember(historyList, historySearchQuery, appliedRoomFilter, appliedSortFilter) {
        val list = historyList.filter { record ->
            val matchesSearch = record.namaBarang.contains(historySearchQuery, ignoreCase = true) ||
                    record.idPemakaian.contains(historySearchQuery, ignoreCase = true) ||
                    record.namaPeminta.contains(historySearchQuery, ignoreCase = true)
            val matchesRoom = appliedRoomFilter == "Semua" || (record.kelas ?: "").contains(appliedRoomFilter, ignoreCase = true)
            matchesSearch && matchesRoom
        }
        if (appliedSortFilter == "Terlama") list.reversed() else list
    }

    val units by viewModel.allUnits.collectAsState()

    var showQrScanner by remember { mutableStateOf(false) }

    // Form inputs
    var bahanSearchQuery by remember { mutableStateOf("") }
    var selectedBahan by remember { mutableStateOf<ItemWithStock?>(null) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    var serialNumberInput by remember { mutableStateOf("") }
    var showSnScannerDialog by remember { mutableStateOf(false) }
    var jumlahDiambilInput by remember { mutableStateOf("") }
    var selectedSatuan by remember { mutableStateOf("") }
    var namaPeminta by remember { mutableStateOf("") }
    var jabatan by remember { mutableStateOf("") }
    var isJabatanExpanded by remember { mutableStateOf(false) }
    val jabatanList by viewModel.jabatanList.collectAsState()
    var kelas by remember { mutableStateOf("") }
    var namaPetugasInput by remember { mutableStateOf("") }
    var keterangan by remember { mutableStateOf("") }

    val defaultOfficerState by viewModel.defaultOfficer.collectAsState()
    LaunchedEffect(defaultOfficerState) {
        if (namaPetugasInput.isEmpty()) {
            namaPetugasInput = defaultOfficerState
        }
    }

    // Set default satuan if a material gets selected
    LaunchedEffect(selectedBahan) {
        if (selectedBahan != null) {
            selectedSatuan = selectedBahan!!.satuan
        }
    }

    // DatePicker State setup
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

    // Form Validations
    val stokTersedia = selectedBahan?.stokTersedia ?: 0
    val jumlahDiambil = jumlahDiambilInput.toIntOrNull() ?: 0
    val isJumlahInvalid = remember(jumlahDiambilInput, selectedBahan) {
        if (jumlahDiambilInput.isEmpty()) false
        else {
            jumlahDiambil <= 0 || (selectedBahan != null && jumlahDiambil > stokTersedia)
        }
    }

    val canSubmit = selectedBahan != null &&
            jumlahDiambil > 0 &&
            jumlahDiambil <= stokTersedia &&
            namaPeminta.isNotBlank() &&
            namaPetugasInput.isNotBlank() &&
            !isJumlahInvalid

    // Filtered materials suggestions for searchable dropdown
    val filteredBahan = remember(bahanSearchQuery, logistikItems) {
        if (bahanSearchQuery.isBlank()) {
            logistikItems
        } else {
            logistikItems.filter { it.namaBarang.contains(bahanSearchQuery, ignoreCase = true) }
        }
    }

    val selectedTabColor = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText
    val unselectedTabColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else Color.Gray.copy(alpha = 0.8f)

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
                                    text = "Pemakaian Bahan Habis Pakai",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = androidx.compose.ui.graphics.Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Pencatatan pemakaian bahan dan riwayat pemakaian",
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
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        divider = {},
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                height = 3.dp,
                                color = selectedTabColor
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Assignment,
                                        contentDescription = null,
                                        tint = if (selectedTab == 0) selectedTabColor else unselectedTabColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Form Pemakaian",
                                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedTab == 0) selectedTabColor else unselectedTabColor,
                                        fontSize = 14.sp
                                    )
                                }
                            },
                            modifier = Modifier.testTag("tab_form_pemakaian")
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = if (selectedTab == 1) selectedTabColor else unselectedTabColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Riwayat Pemakaian",
                                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedTab == 1) selectedTabColor else unselectedTabColor,
                                        fontSize = 14.sp
                                    )
                                }
                            },
                            modifier = Modifier.testTag("tab_riwayat_pemakaian")
                        )
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 0.dp)
        ) {
            if (selectedTab == 0) {
                // Form Pemakaian
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                        // Single White Container Card for the Entire Form
                        LunarisCard(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Section 1: Identitas & Pilihan Bahan
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Inventory2,
                                        contentDescription = null,
                                        tint = DeepPurpleText,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Identitas & Pilihan Bahan",
                                        fontWeight = FontWeight.Bold,
                                        color = DeepPurpleText,
                                        fontSize = 16.sp
                                    )
                                }
                                // 1. Nama Bahan (Searchable Dropdown + QR Scan)
                                Text(
                                    text = "Nama Bahan",
                                    fontWeight = FontWeight.Bold,
                                    color = DeepPurpleText,
                                    fontSize = 14.sp
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        LunarisTextField(
                                            value = if (selectedBahan != null && !dropdownExpanded) selectedBahan!!.namaBarang else bahanSearchQuery,
                                            onValueChange = {
                                                bahanSearchQuery = it
                                                selectedBahan = null // Reset selection if they edit/type
                                                dropdownExpanded = true
                                            },
                                            placeholder = { Text("Ketik nama bahan...") },
                                            trailingIcon = {
                                                IconButton(onClick = { dropdownExpanded = !dropdownExpanded }) {
                                                    Icon(
                                                        imageVector = if (dropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                                        contentDescription = "Pilih"
                                                    )
                                                }
                                            },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth().testTag("pemakaian_search_input")
                                        )

                                        DropdownMenu(
                                            expanded = dropdownExpanded,
                                            onDismissRequest = { dropdownExpanded = false },
                                            properties = PopupProperties(focusable = false),
                                            modifier = Modifier.fillMaxWidth(0.85f)
                                        ) {
                                            if (filteredBahan.isEmpty()) {
                                                DropdownMenuItem(
                                                    text = { Text("Bahan tidak ditemukan") },
                                                    onClick = { dropdownExpanded = false }
                                                )
                                            } else {
                                                filteredBahan.forEach { item ->
                                                    DropdownMenuItem(
                                                        text = { Text("${item.namaBarang} (Stok: ${item.stokTersedia} ${item.satuan})") },
                                                        onClick = {
                                                            selectedBahan = item
                                                            bahanSearchQuery = item.namaBarang
                                                            dropdownExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // QR Scanner Button
                                    IconButton(
                                        onClick = { showQrScanner = true },
                                        modifier = Modifier
                                            .size(54.dp)
                                            .background(PastelLavender, RoundedCornerShape(12.dp))
                                            .testTag("btn_pemakaian_qr")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.QrCodeScanner,
                                            contentDescription = "Pindai QR",
                                            tint = DeepPurpleText
                                        )
                                    }
                                }

                                // Real-time available stock label
                                if (selectedBahan != null) {
                                    Text(
                                        text = "Stok Tersedia: ${selectedBahan!!.stokTersedia} ${selectedBahan!!.satuan}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedBahan!!.stokTersedia > 0) Color(0xFF059669) else Color.Red,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }

                                // 2-Kolom: Jumlah Diambil & Satuan (Otomatis)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    // 2. Jumlah Diambil
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Jumlah Diambil",
                                            fontWeight = FontWeight.Bold,
                                            color = DeepPurpleText,
                                            fontSize = 14.sp,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                        LunarisTextField(
                                            value = jumlahDiambilInput,
                                            onValueChange = { jumlahDiambilInput = it },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            placeholder = { Text("Contoh: 5") },
                                            singleLine = true,
                                            isError = isJumlahInvalid,
                                            supportingText = {
                                                if (isJumlahInvalid) {
                                                    Text(
                                                        "Jumlah harus > 0 dan tidak boleh melebihi stok tersedia ($stokTersedia)!",
                                                        color = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().testTag("pemakaian_jumlah_input")
                                        )
                                    }

                                    // 3. Satuan
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Satuan (Otomatis)",
                                            fontWeight = FontWeight.Bold,
                                            color = DeepPurpleText,
                                            fontSize = 14.sp,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                        LunarisTextField(
                                            value = selectedSatuan.ifBlank { "Otomatis terisi..." },
                                            onValueChange = {},
                                            readOnly = true,
                                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = DeepPurpleText
                                            ),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = Color(0xFFF3F4F6),
                                                unfocusedContainerColor = Color(0xFFF3F4F6),
                                                disabledContainerColor = Color(0xFFF3F4F6),
                                                focusedBorderColor = PastelLavender,
                                                unfocusedBorderColor = PastelLavender.copy(alpha = 0.5f)
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("pemakaian_satuan")
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
                                        color = DeepPurpleText,
                                        fontSize = 16.sp
                                    )
                                }

                                Text(
                                    text = "Serial Number (SN) / Barcode (Opsional)",
                                    fontWeight = FontWeight.Bold,
                                    color = DeepPurpleText,
                                    fontSize = 14.sp
                                )
                                LunarisTextField(
                                    value = serialNumberInput,
                                    onValueChange = { serialNumberInput = it },
                                    placeholder = { Text("Ketik atau pindai SN / Barcode...") },
                                    singleLine = true,
                                    trailingIcon = {
                                        IconButton(onClick = { showSnScannerDialog = true }) {
                                            Icon(
                                                imageVector = Icons.Default.PhotoCamera,
                                                contentDescription = "Pindai Kamera SN",
                                                tint = DeepPurpleText
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("pemakaian_sn_input")
                                )

                                HorizontalDivider(color = Color(0xFFE2E8F0))

                                // Section 3: Informasi Peminta & Petugas
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = DeepPurpleText,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Informasi Peminta & Petugas",
                                        fontWeight = FontWeight.Bold,
                                        color = DeepPurpleText,
                                        fontSize = 16.sp
                                    )
                                }

                                Text(
                                    text = "Nama Peminta",
                                    fontWeight = FontWeight.Bold,
                                    color = DeepPurpleText,
                                    fontSize = 14.sp
                                )
                                LunarisTextField(
                                    value = namaPeminta,
                                    onValueChange = { namaPeminta = it },
                                    placeholder = { Text("Contoh: Ahmad Subarjo") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("pemakaian_peminta_input")
                                )

                                // 5. Jabatan Dropdown
                                Text(
                                    text = "Jabatan",
                                    fontWeight = FontWeight.Bold,
                                    color = DeepPurpleText,
                                    fontSize = 14.sp
                                )
                                ExposedDropdownMenuBox(
                                    expanded = isJabatanExpanded,
                                    onExpandedChange = { isJabatanExpanded = it },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    LunarisTextField(
                                        value = jabatan,
                                        onValueChange = { jabatan = it },
                                        placeholder = { Text("Pilih / Ketik Jabatan...") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isJabatanExpanded) },
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(MenuAnchorType.PrimaryEditable)
                                            .testTag("pemakaian_jabatan_input")
                                    )
                                    ExposedDropdownMenu(
                                        expanded = isJabatanExpanded,
                                        onDismissRequest = { isJabatanExpanded = false }
                                    ) {
                                        val opts = if (jabatanList.isEmpty()) listOf("Siswa", "Guru", "Staf") else jabatanList
                                        opts.forEach { opt ->
                                            DropdownMenuItem(
                                                text = { Text(opt) },
                                                onClick = {
                                                    jabatan = opt
                                                    isJabatanExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // 2-Kolom: Tanggal Pemakaian & Kelas (Opsional)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    // 8. Tanggal Pemakaian
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Tanggal Pemakaian",
                                            fontWeight = FontWeight.Bold,
                                            color = DeepPurpleText,
                                            fontSize = 14.sp,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                        LunarisTextField(
                                            value = selectedDate,
                                            onValueChange = {},
                                            readOnly = true,
                                            trailingIcon = {
                                                IconButton(onClick = { showDatePickerDialog = true }) {
                                                    Icon(imageVector = Icons.Default.CalendarToday, contentDescription = "Pilih Tanggal")
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { showDatePickerDialog = true }
                                                .testTag("pemakaian_tanggal")
                                        )
                                    }

                                    // 6. Kelas
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Kelas (Opsional)",
                                            fontWeight = FontWeight.Bold,
                                            color = DeepPurpleText,
                                            fontSize = 14.sp,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                        LunarisTextField(
                                            value = kelas,
                                            onValueChange = { kelas = it },
                                            placeholder = { Text("Diisi jika siswa") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth().testTag("pemakaian_kelas_input")
                                        )
                                    }
                                }

                                // 7. Nama Petugas
                                Text(
                                    text = "Nama Petugas",
                                    fontWeight = FontWeight.Bold,
                                    color = DeepPurpleText,
                                    fontSize = 14.sp
                                )
                                LunarisTextField(
                                    value = namaPetugasInput,
                                    onValueChange = { namaPetugasInput = it },
                                    placeholder = { Text("Nama petugas inventaris...") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("pemakaian_petugas_input")
                                )

                                HorizontalDivider(color = Color(0xFFE2E8F0))

                                // Section 4: Keperluan & Catatan Pemakaian
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notes,
                                        contentDescription = null,
                                        tint = DeepPurpleText,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Keperluan & Catatan Pemakaian",
                                        fontWeight = FontWeight.Bold,
                                        color = DeepPurpleText,
                                        fontSize = 16.sp
                                    )
                                }

                                Text(
                                    text = "Keterangan",
                                    fontWeight = FontWeight.Bold,
                                    color = DeepPurpleText,
                                    fontSize = 14.sp
                                )
                                LunarisTextField(
                                    value = keterangan,
                                    onValueChange = { keterangan = it },
                                    placeholder = { Text("Rincian keperluan pemakaian bahan...") },
                                    minLines = 2,
                                    maxLines = 4,
                                    modifier = Modifier.fillMaxWidth().testTag("pemakaian_keterangan_input")
                                )
                            }
                        }

                        // Submit Button
                        Button(
                            onClick = {
                                if (!canSubmit) return@Button
                                val finalKeterangan = if (serialNumberInput.isNotBlank()) {
                                    if (keterangan.isBlank()) "[SN: $serialNumberInput]" else "$keterangan [SN: $serialNumberInput]"
                                } else keterangan

                                viewModel.recordPemakaian(
                                    idBarang = selectedBahan!!.idBarang,
                                    namaBarang = selectedBahan!!.namaBarang,
                                    jumlahDiambil = jumlahDiambil,
                                    satuan = selectedSatuan,
                                    namaPeminta = namaPeminta,
                                    jabatan = jabatan,
                                    kelas = kelas,
                                    namaPetugas = namaPetugasInput,
                                    tanggalPemakaian = selectedDate,
                                    keterangan = finalKeterangan,
                                    onSuccess = {
                                        Toast.makeText(context, "Pemakaian bahan berhasil dicatat!", Toast.LENGTH_SHORT).show()
                                        // Reset Form
                                        jumlahDiambilInput = ""
                                        serialNumberInput = ""
                                        namaPeminta = ""
                                        jabatan = ""
                                        kelas = ""
                                        keterangan = ""
                                        selectedBahan = null
                                        bahanSearchQuery = ""
                                    },
                                    onError = { err ->
                                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            enabled = canSubmit,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = Color.Gray.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_simpan_pemakaian")
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = "Simpan")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Simpan Pemakaian", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                } else {
                    // Riwayat Pemakaian
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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
                                            tint = Color(0xFF7C3AED)
                                        )
                                    }
                                },
                                singleLine = true,
                                isStaticOutline = false,
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF7C3AED),
                                    unfocusedBorderColor = Color(0xFFCBD5E1),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("pemakaian_history_search_bar")
                            )

                            Surface(
                                onClick = {
                                    tempRoomFilter = appliedRoomFilter
                                    tempSortFilter = appliedSortFilter
                                    showHistoryFilterDialog = true
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = if (appliedRoomFilter != "Semua" || appliedSortFilter != "Terbaru") Color(0xFFF3E8FF) else Color.White,
                                border = BorderStroke(1.dp, if (appliedRoomFilter != "Semua" || appliedSortFilter != "Terbaru") Color(0xFF7C3AED) else Color(0xFFCBD5E1)),
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("btn_filter_pemakaian")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.FilterList,
                                        contentDescription = "Filter",
                                        tint = Color(0xFF7C3AED)
                                    )
                                }
                            }
                        }

                        if (showHistoryFilterDialog) {
                            LunarisFilterDialog(
                                onDismissRequest = { showHistoryFilterDialog = false },
                                filterGroups = listOf(
                                    FilterGroup(
                                        title = "Lokasi / Ruangan",
                                        options = listOf("Semua", "Lab Biologi", "Lab Kimia", "Lab Fisika", "Ruang Praktikum"),
                                        selectedOption = tempRoomFilter,
                                        onOptionSelected = { tempRoomFilter = it }
                                    ),
                                    FilterGroup(
                                        title = "Urutkan Berdasarkan",
                                        options = listOf("Terbaru", "Terlama"),
                                        selectedOption = tempSortFilter,
                                        onOptionSelected = { tempSortFilter = it }
                                    )
                                ),
                                onReset = {
                                    tempRoomFilter = "Semua"
                                    tempSortFilter = "Terbaru"
                                },
                                onApply = {
                                    appliedRoomFilter = tempRoomFilter
                                    appliedSortFilter = tempSortFilter
                                    showHistoryFilterDialog = false
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (showHistoryQrScanner) {
                            SearchQrScanDialog(
                                onDismiss = { showHistoryQrScanner = false },
                                onQrScanned = { scannedCode ->
                                    showHistoryQrScanner = false
                                    historySearchQuery = scannedCode
                                }
                            )
                        }

                        if (filteredHistory.isEmpty()) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = "Kosong",
                                        tint = Color.Gray.copy(alpha = 0.5f),
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (historySearchQuery.isNotEmpty()) "Tidak ada riwayat yang cocok dengan pencarian." else "Belum ada riwayat pemakaian bahan.", 
                                        color = Color.Gray
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(bottom = 80.dp),
                                modifier = Modifier.weight(1f).fillMaxWidth()
                            ) {
                                items(filteredHistory) { record ->
                                    LunarisCard(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = cardBgColor),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                        border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f) else Color(0xFFCBD5E1)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("pemakaian_item_${record.idPemakaian}")
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = record.idPemakaian,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = DeepPurpleText,
                                                    fontSize = 12.sp
                                                )
                                                Text(
                                                    text = record.tanggalPemakaian,
                                                    fontSize = 11.sp,
                                                    color = Color.Gray
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = record.namaBarang,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp
                                            )

                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Jumlah: ${record.jumlahDiambil} ${record.satuan}",
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = "Petugas: ${record.namaPetugas}",
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Peminta: ${record.namaPeminta} (${record.jabatan}${if (!record.kelas.isNullOrEmpty()) ", Kelas " + record.kelas else ""})",
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            if (record.keterangan.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = "Keterangan: ${record.keterangan}",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
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

        // Qr Scan Dialog
        if (showQrScanner) {
            PemakaianQrScanDialog(
                onDismiss = { showQrScanner = false },
                onQrScanned = { scannedCode ->
                    showQrScanner = false
                    val matched = logistikItems.find { it.idBarang == scannedCode }
                    if (matched != null) {
                        selectedBahan = matched
                        bahanSearchQuery = matched.namaBarang
                        Toast.makeText(context, "Bahan '${matched.namaBarang}' terdeteksi!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Bahan dengan ID '$scannedCode' tidak terdaftar atau bukan bahan habis pakai!", Toast.LENGTH_LONG).show()
                    }
                }
            )
        }

        // SN / Barcode Scanner Dialog
        if (showSnScannerDialog) {
            SearchQrScanDialog(
                onDismiss = { showSnScannerDialog = false },
                onQrScanned = { scannedCode ->
                    showSnScannerDialog = false
                    serialNumberInput = scannedCode
                    Toast.makeText(context, "SN/Barcode berhasil dipindai: $scannedCode", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
fun PemakaianQrScanDialog(
    onDismiss: () -> Unit,
    onQrScanned: (String) -> Unit
) {
    CameraScannerDialog(
        title = "Pindai QR / Barcode Bahan",
        initialMode = ScanMode.PRIMARY_QR,
        onDismissRequest = onDismiss,
        onBarcodeScanned = onQrScanned
    )
}
