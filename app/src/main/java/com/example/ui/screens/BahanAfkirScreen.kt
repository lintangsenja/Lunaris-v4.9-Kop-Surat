package com.example.ui.screens
import com.example.ui.components.LunarisCard
import com.example.ui.components.LunarisTextField
import com.example.ui.components.FilterGroup
import com.example.ui.components.LunarisFilterDialog
import com.example.ui.components.LunarisDatePickerDialog

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.data.entity.BahanAfkirEntity
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
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BahanAfkirScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit,
    initialSelectedId: String? = null,
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

    val allItems by viewModel.itemsWithStock.collectAsState()
    val logistikItems = remember(allItems) {
        allItems.filter { it.kategori.equals("Logistik", ignoreCase = true) }
    }
    val historyList by viewModel.allBahanAfkir.collectAsState()
    val defaultOfficerState by viewModel.defaultOfficer.collectAsState()
    val defaultOfficer = defaultOfficerState.ifBlank { "Administrator" }
    val kondisiList by viewModel.kondisi.collectAsState()
    val alasanOptions = remember(kondisiList) { kondisiList }

    var showQrScanner by remember { mutableStateOf(false) }

    val userRole by viewModel.userRole.collectAsState()
    val studentPermissions by viewModel.studentPermissions.collectAsState()
    val canSubmitPermission = viewModel.isStudentPermissionGranted("bahan_afkir_submit", studentPermissions)
    val canView = viewModel.isStudentPermissionGranted("bahan_afkir_view", studentPermissions)
    val canAccessAfkir = viewModel.isStudentPermissionGranted("bahan_afkir", studentPermissions) && (canSubmitPermission || canView)

    // Tab state
    var selectedTabState by remember { mutableStateOf(0) }

    LaunchedEffect(userRole, canSubmitPermission, canView) {
        if (userRole.contains("siswa", ignoreCase = true)) {
            if (!canSubmitPermission && canView) {
                selectedTabState = 1
            } else if (canSubmitPermission && !canView) {
                selectedTabState = 0
            }
        }
    }

    // Form inputs
    var bahanSearchQuery by remember { mutableStateOf("") }
    var selectedBahan by remember { mutableStateOf<ItemWithStock?>(null) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    var jumlahAfkirInput by remember { mutableStateOf("") }
    var serialNumberInput by remember { mutableStateOf("") }
    var showSnScannerDialog by remember { mutableStateOf(false) }
    var isReasonDropdownExpanded by remember { mutableStateOf(false) }
    var selectedReason by remember { mutableStateOf("") }
    var selectedSatuan by remember { mutableStateOf("-") }

    LaunchedEffect(alasanOptions) {
        if (selectedReason.isBlank() || !alasanOptions.contains(selectedReason)) {
            selectedReason = alasanOptions.firstOrNull() ?: ""
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

    // Auto pre-fill material data when navigated with an initial itemId/selectedId
    LaunchedEffect(initialSelectedId, logistikItems, allItems) {
        if (!initialSelectedId.isNullOrBlank()) {
            val foundItem = logistikItems.find {
                it.idBarang.equals(initialSelectedId, ignoreCase = true) ||
                it.namaBarang.equals(initialSelectedId, ignoreCase = true)
            } ?: allItems.find {
                it.idBarang.equals(initialSelectedId, ignoreCase = true) ||
                it.namaBarang.equals(initialSelectedId, ignoreCase = true)
            }
            if (foundItem != null) {
                selectedBahan = foundItem
                bahanSearchQuery = foundItem.namaBarang
                serialNumberInput = foundItem.serialNumber ?: ""
                if (jumlahAfkirInput.isBlank() || jumlahAfkirInput == "0") {
                    jumlahAfkirInput = "1"
                }
                selectedTabState = 0
            }
        }
    }

    // Update Satuan auto-lock
    LaunchedEffect(selectedBahan) {
        if (selectedBahan != null) {
            selectedSatuan = selectedBahan!!.satuan
        } else {
            selectedSatuan = "-"
        }
    }

    // Form validation
    val stokTersedia = selectedBahan?.stokTersedia ?: 0
    val maxAllowedStock = remember(selectedBahan) {
        selectedBahan?.let { maxOf(it.stokTersedia, it.stokAwal, 1) } ?: 1
    }
    val jumlahAfkir = jumlahAfkirInput.toIntOrNull() ?: 0
    val isJumlahInvalid = remember(jumlahAfkirInput, selectedBahan, maxAllowedStock) {
        if (jumlahAfkirInput.isEmpty()) false
        else {
            jumlahAfkir <= 0 || (selectedBahan != null && jumlahAfkir > maxAllowedStock)
        }
    }

    val canSubmit = selectedBahan != null &&
            jumlahAfkir > 0 &&
            jumlahAfkir <= maxAllowedStock &&
            !isJumlahInvalid

    // Filtered materials suggestions for searchable dropdown
    val filteredBahan = remember(bahanSearchQuery, logistikItems) {
        if (bahanSearchQuery.isBlank()) {
            logistikItems
        } else {
            logistikItems.filter { it.namaBarang.contains(bahanSearchQuery, ignoreCase = true) }
        }
    }

    // Advanced Data Table States
    var historySearchQuery by remember { mutableStateOf("") }
    var showHistoryQrScanner by remember { mutableStateOf(false) }

    // Confirm dialog states
    var itemToUndo by remember { mutableStateOf<BahanAfkirEntity?>(null) }
    var itemToTransferToHapusAset by remember { mutableStateOf<BahanAfkirEntity?>(null) }
    var itemToDeletePermanently by remember { mutableStateOf<BahanAfkirEntity?>(null) }

    var showFilterDialog by remember { mutableStateOf(false) }
    var appliedReasonFilter by remember { mutableStateOf("Semua") }
    var appliedSortFilter by remember { mutableStateOf("Terbaru") }
    var tempReasonFilter by remember { mutableStateOf("Semua") }
    var tempSortFilter by remember { mutableStateOf("Terbaru") }

    // Filter active items for List Bahan Afkir (excluding transferred/hibah items)
    val filteredHistory = remember(historyList, historySearchQuery, appliedReasonFilter, appliedSortFilter) {
        val baseList = historyList.filter { it.status != "Hapus Aset" && it.status != "Hibah" }
        val list = baseList.filter { item ->
            val matchesSearch = historySearchQuery.isBlank() || (
                item.namaBarang.contains(historySearchQuery, ignoreCase = true) ||
                item.idAfkir.contains(historySearchQuery, ignoreCase = true) ||
                item.alasan.contains(historySearchQuery, ignoreCase = true) ||
                item.status.contains(historySearchQuery, ignoreCase = true)
            )
            val matchesReason = appliedReasonFilter == "Semua" || item.alasan.contains(appliedReasonFilter, ignoreCase = true)
            matchesSearch && matchesReason
        }
        if (appliedSortFilter == "Terlama") list.reversed() else list
    }

    val selectedTabColor = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText
    val unselectedTabColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else Color.Gray.copy(alpha = 0.8f)

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
                                    text = "Manajemen Bahan Afkir",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = androidx.compose.ui.graphics.Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Pencatatan afkir/kadaluarsa dan daftar bahan afkir",
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.EditNote,
                                        contentDescription = null,
                                        tint = if (selectedTabState == 0) selectedTabColor else unselectedTabColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Catat Afkir",
                                        fontSize = 15.sp,
                                        fontWeight = if (selectedTabState == 0) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedTabState == 0) selectedTabColor else unselectedTabColor
                                    )
                                }
                            },
                            modifier = Modifier.testTag("tab_catat_afkir")
                        )
                        Tab(
                            selected = selectedTabState == 1,
                            onClick = { selectedTabState = 1 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.List,
                                        contentDescription = null,
                                        tint = if (selectedTabState == 1) selectedTabColor else unselectedTabColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "List Bahan Afkir",
                                        fontSize = 15.sp,
                                        fontWeight = if (selectedTabState == 1) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedTabState == 1) selectedTabColor else unselectedTabColor
                                    )
                                }
                            },
                            modifier = Modifier.testTag("tab_list_bahan_afkir")
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

            AnimatedVisibility(
                visible = selectedTabState == 0,
                enter = EnterTransition.None,
                exit = ExitTransition.None
            ) {
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
                                containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFFFFFF)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f) else Color(0xFFCBD5E1)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "Catat Bahan Afkir Baru",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) MaterialTheme.colorScheme.onSurface else DeepPurpleText,
                                    fontSize = 18.sp
                                )

                                // Input A: Nama Bahan (Searchable Dropdown + QR Scanner button)
                                Text(
                                    text = "Nama Bahan",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else DeepPurpleText,
                                    fontSize = 14.sp
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        LunarisTextField(
                                            value = if (selectedBahan != null) selectedBahan!!.namaBarang else bahanSearchQuery,
                                            onValueChange = {
                                                bahanSearchQuery = it
                                                selectedBahan = null // Reset selected item if user types manually
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
                                            shape = RoundedCornerShape(16.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF7C3AED),
                                                unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else Color.Gray.copy(alpha = 0.4f),
                                                focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                                unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("afkir_search_input")
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

                                    // QR Scanner Shortcut
                                    IconButton(
                                        onClick = { showQrScanner = true },
                                        modifier = Modifier
                                            .size(54.dp)
                                            .background(
                                                if (isDark) MaterialTheme.colorScheme.primaryContainer else Color(0xFFF3E8FF),
                                                RoundedCornerShape(16.dp)
                                            )
                                            .testTag("btn_afkir_qr")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.QrCodeScanner,
                                            contentDescription = "Pindai QR",
                                            tint = if (isDark) MaterialTheme.colorScheme.onPrimaryContainer else DeepPurpleText
                                        )
                                    }
                                }

                                // Real-time stock label
                                if (selectedBahan != null) {
                                    Text(
                                        text = "Stok Tersedia: ${selectedBahan!!.stokTersedia} ${selectedBahan!!.satuan}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedBahan!!.stokTersedia > 0) Color(0xFF059669) else Color.Red,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }

                                // Row 1: Jumlah Afkir & Satuan
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    // Input B: Jumlah Afkir
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Jumlah Afkir",
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else DeepPurpleText,
                                            fontSize = 14.sp,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                        LunarisTextField(
                                            value = jumlahAfkirInput,
                                            onValueChange = { jumlahAfkirInput = it },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            placeholder = { Text("Masukkan jumlah") },
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
                                                        color = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("afkir_jumlah_input")
                                        )
                                    }

                                    // Input C: Satuan (Locked display)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Satuan",
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else DeepPurpleText,
                                            fontSize = 14.sp,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                        LunarisTextField(
                                            value = selectedSatuan,
                                            onValueChange = {},
                                            readOnly = true,
                                            enabled = false,
                                            shape = RoundedCornerShape(16.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                disabledTextColor = if (isDark) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else Color.DarkGray,
                                                disabledBorderColor = if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f),
                                                disabledContainerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color.LightGray.copy(alpha = 0.15f)
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("afkir_satuan_input")
                                        )
                                    }
                                }

                                HorizontalDivider(
                                    color = if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFE2E8F0)
                                )

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
                                        fontSize = 16.sp
                                    )
                                }

                                Text(
                                    text = "Serial Number (SN) / Barcode (Opsional)",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else DeepPurpleText,
                                    fontSize = 14.sp
                                )
                                LunarisTextField(
                                    value = serialNumberInput,
                                    onValueChange = { serialNumberInput = it },
                                    placeholder = { Text("Ketik atau pindai SN / Barcode...") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF7C3AED),
                                        unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else Color.Gray.copy(alpha = 0.4f),
                                        focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                        unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                                    ),
                                    trailingIcon = {
                                        IconButton(onClick = { showSnScannerDialog = true }) {
                                            Icon(
                                                imageVector = Icons.Default.PhotoCamera,
                                                contentDescription = "Pindai Kamera SN",
                                                tint = DeepPurpleText
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("afkir_sn_input")
                                )

                                HorizontalDivider(
                                    color = if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFE2E8F0)
                                )

                                // Section 3: Detail Alasan & Tanggal
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Event,
                                        contentDescription = null,
                                        tint = DeepPurpleText,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Detail Alasan & Tanggal Pencatatan",
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) MaterialTheme.colorScheme.onSurface else DeepPurpleText,
                                        fontSize = 16.sp
                                    )
                                }

                                // Row 2: Alasan Afkir & Tanggal Pencatatan
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    // Input D: Alasan (Dropdown)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Alasan Afkir",
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else DeepPurpleText,
                                            fontSize = 14.sp,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                        ExposedDropdownMenuBox(
                                            expanded = isReasonDropdownExpanded,
                                            onExpandedChange = { isReasonDropdownExpanded = it },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            LunarisTextField(
                                                value = selectedReason,
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("Pilih Alasan") },
                                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isReasonDropdownExpanded) },
                                                shape = RoundedCornerShape(16.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF7C3AED),
                                                    unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else Color(0xFFD8B4FE),
                                                    focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                                    unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .menuAnchor()
                                                    .testTag("afkir_alasan_dropdown")
                                            )
                                            ExposedDropdownMenu(
                                                expanded = isReasonDropdownExpanded,
                                                onDismissRequest = { isReasonDropdownExpanded = false },
                                                modifier = Modifier.background(if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White)
                                            ) {
                                                if (alasanOptions.isEmpty()) {
                                                    DropdownMenuItem(
                                                        text = { Text("Tidak ada pilihan di Master Data") },
                                                        onClick = { isReasonDropdownExpanded = false }
                                                    )
                                                } else {
                                                    alasanOptions.forEach { option ->
                                                        val isOptionSelected = option == selectedReason
                                                        DropdownMenuItem(
                                                            text = {
                                                                Text(
                                                                    text = option,
                                                                    fontWeight = if (isOptionSelected) FontWeight.Bold else FontWeight.Normal,
                                                                    color = if (isOptionSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                                )
                                                            },
                                                            onClick = {
                                                                selectedReason = option
                                                                isReasonDropdownExpanded = false
                                                            },
                                                            modifier = Modifier.background(
                                                                if (isOptionSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                                                else Color.Transparent
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Input E: Tanggal Afkir (DatePicker)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Tanggal Pencatatan",
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else DeepPurpleText,
                                            fontSize = 14.sp,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                        LunarisTextField(
                                            value = selectedDate,
                                            onValueChange = {},
                                            readOnly = true,
                                            shape = RoundedCornerShape(16.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF7C3AED),
                                                unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else Color.Gray.copy(alpha = 0.4f),
                                                focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                                unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                                            ),
                                            trailingIcon = {
                                                IconButton(onClick = { showDatePickerDialog = true }) {
                                                    Icon(imageVector = Icons.Default.CalendarToday, contentDescription = "Pilih Tanggal")
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { showDatePickerDialog = true }
                                                .testTag("afkir_tanggal_input")
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Submit Button
                                Button(
                                    onClick = {
                                        if (!canSubmit) return@Button
                                        val finalReason = if (serialNumberInput.isNotBlank()) {
                                            "$selectedReason [SN: $serialNumberInput]"
                                        } else selectedReason

                                        viewModel.recordBahanAfkir(
                                            idBarang = selectedBahan!!.idBarang,
                                            namaBarang = selectedBahan!!.namaBarang,
                                            jumlahAfkir = jumlahAfkir,
                                            satuan = selectedSatuan,
                                            alasan = finalReason,
                                            tanggalAfkir = selectedDate,
                                            onSuccess = {
                                                Toast.makeText(context, "Bahan afkir berhasil dicatat!", Toast.LENGTH_SHORT).show()
                                                // Reset Form inputs
                                                jumlahAfkirInput = ""
                                                serialNumberInput = ""
                                                selectedBahan = null
                                                bahanSearchQuery = ""
                                                // Automatically navigate to history tab to view history
                                                selectedTabState = 1
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
                                        disabledContainerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.Gray.copy(alpha = 0.4f),
                                        disabledContentColor = if (isDark) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.6f)
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("btn_simpan_afkir")
                                ) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = "Simpan")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Simpan Bahan Afkir", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = selectedTabState == 1,
                enter = EnterTransition.None,
                exit = ExitTransition.None
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 0.dp)
                ) {
                    // Search Bar & Row Controller
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LunarisTextField(
                            value = historySearchQuery,
                            onValueChange = { 
                                historySearchQuery = it
                            },
                            placeholder = { Text("Ketik untuk mencari...") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
                                )
                            },
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
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("riwayat_search_bar")
                        )

                        Surface(
                            onClick = {
                                tempReasonFilter = appliedReasonFilter
                                tempSortFilter = appliedSortFilter
                                showFilterDialog = true
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = if (appliedReasonFilter != "Semua" || appliedSortFilter != "Terbaru") Color(0xFFF3E8FF) else Color.White,
                            border = BorderStroke(1.dp, if (appliedReasonFilter != "Semua" || appliedSortFilter != "Terbaru") Color(0xFF7C3AED) else Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("btn_filter_bahan_afkir")
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

                    if (showFilterDialog) {
                        LunarisFilterDialog(
                            onDismissRequest = { showFilterDialog = false },
                            filterGroups = listOf(
                                FilterGroup(
                                    title = "Alasan Afkir",
                                    options = listOf("Semua") + alasanOptions.distinct(),
                                    selectedOption = tempReasonFilter,
                                    onOptionSelected = { tempReasonFilter = it }
                                ),
                                FilterGroup(
                                    title = "Urutkan Berdasarkan",
                                    options = listOf("Terbaru", "Terlama"),
                                    selectedOption = tempSortFilter,
                                    onOptionSelected = { tempSortFilter = it }
                                )
                            ),
                            onReset = {
                                tempReasonFilter = "Semua"
                                tempSortFilter = "Terbaru"
                            },
                            onApply = {
                                appliedReasonFilter = tempReasonFilter
                                appliedSortFilter = tempSortFilter
                                showFilterDialog = false
                            }
                        )
                    }

                    if (showHistoryQrScanner) {
                        SearchQrScanDialog(
                            onDismiss = { showHistoryQrScanner = false },
                            onQrScanned = { scannedCode ->
                                showHistoryQrScanner = false
                                historySearchQuery = scannedCode
                            }
                        )
                    }

                    // Riwayat Data Table
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp)
                    ) {
                        if (filteredHistory.isEmpty()) {
                            item {
                                LunarisCard(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFFFFFF)
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.3f) else Color(0xFFCBD5E1)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteSweep,
                                            contentDescription = "Kosong",
                                            tint = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f) else Color.Gray.copy(alpha = 0.4f),
                                            modifier = Modifier.size(54.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = if (historySearchQuery.isBlank()) "Belum ada riwayat bahan afkir." else "Pencarian tidak ditemukan.",
                                            color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray,
                                            fontWeight = FontWeight.Medium,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            items(filteredHistory) { record ->
                                val isCanceled = record.status == "Dibatalkan"
                                val cardBg = if (isDark) {
                                    if (isCanceled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant
                                } else {
                                    Color(0xFFFFFFFF)
                                }
                                LunarisCard(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = cardBg),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f) else Color(0xFFCBD5E1)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .testTag("afkir_item_${record.idAfkir}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = record.idAfkir,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isCanceled) (if (isDark) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else Color.Gray) else (if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText),
                                                    fontSize = 12.sp
                                                )
                                                Text(
                                                    text = record.tanggalAfkir,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = record.namaBarang,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = if (isCanceled) (if (isDark) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else Color.Gray) else (if (isDark) MaterialTheme.colorScheme.onSurface else CarbonBlackText)
                                            )

                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = "${record.jumlahAfkir} ${record.satuan}",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (isCanceled) (if (isDark) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else Color.Gray) else (if (isDark) MaterialTheme.colorScheme.onSurface else Color.DarkGray)
                                                )

                                                // Status text-only
                                                val statusColor = if (isCanceled) {
                                                    if (isDark) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else Color(0xFF64748B)
                                                } else {
                                                    when (record.alasan) {
                                                        "Kedaluwarsa" -> if (isDark) Color(0xFFFB7185) else Color(0xFFE11D48)
                                                        "Rusak Fisik" -> if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
                                                        else -> if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                                                    }
                                                }
                                                Text(
                                                    text = if (isCanceled) "Dibatalkan" else record.alasan,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = statusColor
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // 1. Kembali Normal Button - Pure Icon Button
                                            IconButton(
                                                onClick = { itemToUndo = record },
                                                enabled = !isCanceled,
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .testTag("btn_undo_afkir_${record.idAfkir}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Restore,
                                                    contentDescription = "Kembali Normal",
                                                    tint = if (!isCanceled) Color(0xFF0284C7) else Color.Gray.copy(alpha = 0.4f),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            // 2. Hapus Button (Transfers to Menu Hapus Aset) - Pure Icon Button
                                            IconButton(
                                                onClick = { itemToTransferToHapusAset = record },
                                                enabled = !isCanceled,
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .testTag("btn_delete_afkir_${record.idAfkir}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteSweep,
                                                    contentDescription = "Pindahkan ke Hapus Aset",
                                                    tint = if (!isCanceled) Color(0xFFDC2626) else Color.Gray.copy(alpha = 0.4f),
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

        // Qr Scan Dialog
        if (showQrScanner) {
            SearchQrScanDialog(
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

        // 1. Confirm Undo / Kembalikan Dialog
        if (itemToUndo != null) {
            AlertDialog(
                onDismissRequest = { itemToUndo = null },
                title = { Text("Konfirmasi Kembali Normal", fontWeight = FontWeight.Bold) },
                text = { 
                    Text("Apakah Anda yakin ingin mengembalikan bahan '${itemToUndo!!.namaBarang}' ke kondisi normal (bisa dipakai)?\n\nStok sebanyak ${itemToUndo!!.jumlahAfkir} ${itemToUndo!!.satuan} akan dikembalikan ke master stok fisik utama.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val record = itemToUndo!!
                            itemToUndo = null
                            viewModel.undoBahanAfkir(
                                idAfkir = record.idAfkir,
                                onSuccess = {
                                    Toast.makeText(context, "Bahan '${record.namaBarang}' berhasil dikembalikan ke status Normal!", Toast.LENGTH_LONG).show()
                                },
                                onError = { err ->
                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                    ) {
                        Text("Kembalikan Normal", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { itemToUndo = null }) {
                        Text("Batal")
                    }
                }
            )
        }

        // 2. Confirm Transfer to Hapus Aset Dialog
        if (itemToTransferToHapusAset != null) {
            AlertDialog(
                onDismissRequest = { itemToTransferToHapusAset = null },
                title = { Text("Pindahkan ke Menu Hapus Aset", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626)) },
                text = { 
                    Text("Apakah Anda yakin ingin memindahkan bahan afkir '${itemToTransferToHapusAset!!.namaBarang}' (${itemToTransferToHapusAset!!.jumlahAfkir} ${itemToTransferToHapusAset!!.satuan}) ke Menu Hapus Aset?\n\nData akan disatukan di Menu Hapus Aset (Tab Bahan) untuk tindakan lanjut Hibah atau Hapus Permanen.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val record = itemToTransferToHapusAset!!
                            itemToTransferToHapusAset = null
                            viewModel.transferBahanAfkirToHapusAset(
                                idAfkir = record.idAfkir,
                                officerName = defaultOfficer,
                                onSuccess = {
                                    Toast.makeText(context, "Bahan afkir '${record.namaBarang}' berhasil dipindahkan ke Menu Hapus Aset!", Toast.LENGTH_LONG).show()
                                },
                                onError = { err ->
                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                    ) {
                        Text("Pindahkan ke Hapus Aset", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { itemToTransferToHapusAset = null }) {
                        Text("Batal")
                    }
                }
            )
        }

        // 2. Confirm Permanent Physical Delete Dialog
        if (itemToDeletePermanently != null) {
            AlertDialog(
                onDismissRequest = { itemToDeletePermanently = null },
                title = { Text("Hapus Permanen", fontWeight = FontWeight.Bold, color = Color(0xFFE11D48)) },
                text = { 
                    Text("Apakah Anda yakin ingin menghapus data afkir '${itemToDeletePermanently!!.namaBarang}' secara permanen?\n\nAksi ini tidak dapat dibatalkan, dan bukti penghapusan fisik akan dicatat di Log Transaksi untuk audit.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val record = itemToDeletePermanently!!
                            itemToDeletePermanently = null
                            viewModel.deleteBahanAfkirPermanently(
                                idAfkir = record.idAfkir,
                                namaPetugas = defaultOfficer,
                                onSuccess = {
                                    Toast.makeText(context, "Catatan afkir dihapus permanen & audit dicatat!", Toast.LENGTH_LONG).show()
                                },
                                onError = { err ->
                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
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
}
