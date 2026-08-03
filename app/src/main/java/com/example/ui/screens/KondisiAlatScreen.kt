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
fun KondisiAlatScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val itemsWithStock by viewModel.itemsWithStock.collectAsState()
    val allDamagedItems by viewModel.allDamagedItems.collectAsState()
    val isDark = false

    // Filter tools (ALAT)
    val alatItems = remember(itemsWithStock) {
        itemsWithStock.filter { it.type == "ALAT" }
    }

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
    val dividerColor = MaterialTheme.colorScheme.outlineVariant

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
                                    text = "Kondisi Alat (Asset Condition)",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = androidx.compose.ui.graphics.Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Monitoring kondisi kelayakan dan status alat laboratorium",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = androidx.compose.ui.graphics.Color(0xFF1E293B)
                                )
                            }
                        }
                    }
                }
                androidx.compose.material3.HorizontalDivider(
                    thickness = 1.2.dp,
                    color = androidx.compose.ui.graphics.Color.Transparent
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            PantauAsetTab(viewModel = viewModel, alatItems = alatItems)
        }
    }
}

@Composable
fun PantauAsetTab(
    viewModel: InventoryViewModel,
    alatItems: List<ItemWithStock>
) {
    val masterRuangList by viewModel.ruang.collectAsState()
    val masterKondisiList by viewModel.kondisi.collectAsState()

    // Data filter Ruang dari master tab Ruang + data item
    val roomOptions = remember(masterRuangList, alatItems) {
        listOf("Semua Ruang") + (masterRuangList + alatItems.map { it.ruang }).filter { it.isNotBlank() }.distinct().sorted()
    }

    // Data filter Kondisi dari master tab Kondisi + data item
    val conditionOptions = remember(masterKondisiList, alatItems) {
        listOf("Semua Kondisi") + (masterKondisiList + alatItems.map { it.kondisi }).filter { it.isNotBlank() }.distinct().sorted()
    }

    val uniqueCategories = remember(alatItems) {
        listOf("Semua Kategori") + alatItems.map { it.kategori }.filter { it.isNotBlank() }.distinct().sorted()
    }

    var selectedRoomFilter by remember { mutableStateOf("Semua Ruang") }
    var selectedConditionFilter by remember { mutableStateOf("Semua Kondisi") }
    var selectedCategoryFilter by remember { mutableStateOf("Semua Kategori") }
    var searchQuery by remember { mutableStateOf("") }
    var showQrScanner by remember { mutableStateOf(false) }

    var showFilterDialog by remember { mutableStateOf(false) }
    var tempRoomFilter by remember { mutableStateOf("Semua Ruang") }
    var tempConditionFilter by remember { mutableStateOf("Semua Kondisi") }
    var tempCategoryFilter by remember { mutableStateOf("Semua Kategori") }

    // Filter tools currently matching condition, room, category, and search
    val filteredDamagedAlat = remember(alatItems, selectedRoomFilter, selectedConditionFilter, selectedCategoryFilter, searchQuery) {
        alatItems.filter { item ->
            val matchesCondition = if (selectedConditionFilter == "Semua Kondisi") {
                !item.kondisi.equals("Normal", ignoreCase = true) &&
                !item.kondisi.equals("Baik", ignoreCase = true) &&
                !item.kondisi.equals("Normal / Baik", ignoreCase = true) &&
                item.kondisi.isNotBlank()
            } else {
                item.kondisi.equals(selectedConditionFilter, ignoreCase = true)
            }
            val matchesRoom = selectedRoomFilter == "Semua Ruang" || item.ruang.equals(selectedRoomFilter, ignoreCase = true)
            val matchesCategory = selectedCategoryFilter == "Semua Kategori" || item.kategori.equals(selectedCategoryFilter, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                    item.namaBarang.contains(searchQuery, ignoreCase = true) ||
                    item.idBarang.contains(searchQuery, ignoreCase = true) ||
                    item.kategori.contains(searchQuery, ignoreCase = true) ||
                    item.ruang.contains(searchQuery, ignoreCase = true)
            matchesCondition && matchesRoom && matchesCategory && matchesSearch
        }
    }

    val isDark = false
    val cardBg = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFFFFFF)
    val textColor = if (isDark) MaterialTheme.colorScheme.onSurface else DeepPurpleText

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 0.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search Bar & Filter Button Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LunarisTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Ketik untuk mencari...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cari") },
                trailingIcon = {
                    IconButton(onClick = { showQrScanner = true }) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan QR",
                            tint = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText
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
                    .testTag("kondisi_alat_search_bar")
            )

            Surface(
                onClick = {
                    tempRoomFilter = selectedRoomFilter
                    tempConditionFilter = selectedConditionFilter
                    tempCategoryFilter = selectedCategoryFilter
                    showFilterDialog = true
                },
                shape = RoundedCornerShape(16.dp),
                color = if (selectedRoomFilter != "Semua Ruang" || selectedConditionFilter != "Semua Kondisi" || selectedCategoryFilter != "Semua Kategori") Color(0xFFF3E8FF) else Color.White,
                border = BorderStroke(1.dp, if (selectedRoomFilter != "Semua Ruang" || selectedConditionFilter != "Semua Kondisi" || selectedCategoryFilter != "Semua Kategori") Color(0xFF7C3AED) else Color(0xFFCBD5E1)),
                modifier = Modifier
                    .size(48.dp)
                    .testTag("btn_filter_kondisi_alat")
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
                        title = "Ruang / Lokasi",
                        options = roomOptions,
                        selectedOption = tempRoomFilter,
                        onOptionSelected = { tempRoomFilter = it }
                    ),
                    FilterGroup(
                        title = "Kondisi Barang",
                        options = conditionOptions,
                        selectedOption = tempConditionFilter,
                        onOptionSelected = { tempConditionFilter = it }
                    ),
                    FilterGroup(
                        title = "Kategori Alat",
                        options = uniqueCategories,
                        selectedOption = tempCategoryFilter,
                        onOptionSelected = { tempCategoryFilter = it }
                    )
                ),
                onReset = {
                    tempRoomFilter = "Semua Ruang"
                    tempConditionFilter = "Semua Kondisi"
                    tempCategoryFilter = "Semua Kategori"
                },
                onApply = {
                    selectedRoomFilter = tempRoomFilter
                    selectedConditionFilter = tempConditionFilter
                    selectedCategoryFilter = tempCategoryFilter
                    showFilterDialog = false
                }
            )
        }

        // List of Damaged/Repairing Assets
        if (filteredDamagedAlat.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                LunarisCard(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Aman",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Semua aset dalam kondisi Normal!",
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tidak ada alat yang rusak atau dalam perbaikan saat ini.",
                            color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredDamagedAlat) { item ->
                    LunarisCard(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("asset_item_${item.idBarang}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.idBarang,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText,
                                        fontSize = 12.sp
                                    )

                                    // Dynamic tag: Perbaikan -> Amber, Rusak -> Red/Pink Pastel
                                    val isPerbaikan = item.kondisi.equals("Perbaikan", ignoreCase = true)
                                    val tagBg = if (isPerbaikan) {
                                        if (isDark) MaterialTheme.colorScheme.primaryContainer else Color(0xFFFEF3C7)
                                    } else {
                                        if (isDark) MaterialTheme.colorScheme.errorContainer else Color(0xFFFFECEF)
                                    }
                                    val tagText = if (isPerbaikan) {
                                        if (isDark) MaterialTheme.colorScheme.onPrimaryContainer else Color(0xFFD97706)
                                    } else {
                                        if (isDark) MaterialTheme.colorScheme.onErrorContainer else Color(0xFFE11D48)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(tagBg)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = item.kondisi,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 11.sp,
                                            color = tagText
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = item.namaBarang,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = if (isDark) MaterialTheme.colorScheme.onSurface else CarbonBlackText
                                )

                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Room, 
                                            contentDescription = "Ruang", 
                                            tint = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray, 
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = item.ruang.ifBlank { "-" }, 
                                            fontSize = 12.sp, 
                                            color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Folder, 
                                            contentDescription = "Kategori", 
                                            tint = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray, 
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = item.kategori.ifBlank { "-" }, 
                                            fontSize = 12.sp, 
                                            color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text(
                                        text = "Rusak/Perbaikan: ${item.stokRusak} ${item.satuan}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.DarkGray
                                    )
                                    Text(
                                        text = "Total Aset: ${item.stokAwal} ${item.satuan}",
                                        fontSize = 12.sp,
                                        color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f) else Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showQrScanner) {
            SearchQrScanDialog(
                onDismiss = { showQrScanner = false },
                onQrScanned = { scannedCode ->
                    showQrScanner = false
                    searchQuery = scannedCode
                }
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LaporKerusakanTab(
    viewModel: InventoryViewModel,
    alatItems: List<ItemWithStock>,
    historyList: List<DamagedItemEntity>
) {
    val context = LocalContext.current
    val isDark = false
    val cardBg = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFFFFFF)
    val textColor = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText
    val titleColor = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText
    val subColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray

    var selectedAlat by remember { mutableStateOf<ItemWithStock?>(null) }
    var searchAlatQuery by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    var jumlahRusakInput by remember { mutableStateOf("") }
    var selectedKondisiBaru by remember { mutableStateOf("Rusak") }
    var namaPetugasInput by remember { mutableStateOf("") }
    var keteranganInput by remember { mutableStateOf("") }

    var showQrScanner by remember { mutableStateOf(false) }

    // Date setup
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

    // Validation
    val availableStok = selectedAlat?.stokTersedia ?: 0
    val jumlahRusak = jumlahRusakInput.toIntOrNull() ?: 0
    val isJumlahInvalid = remember(jumlahRusakInput, selectedAlat) {
        if (jumlahRusakInput.isEmpty()) false
        else {
            jumlahRusak <= 0 || (selectedAlat != null && jumlahRusak > availableStok)
        }
    }

    val canSubmit = selectedAlat != null &&
            jumlahRusak > 0 &&
            jumlahRusak <= availableStok &&
            !isJumlahInvalid &&
            namaPetugasInput.isNotBlank() &&
            keteranganInput.isNotBlank()

    // Filtered assets suggestions
    val filteredAlatSuggestions = remember(searchAlatQuery, alatItems) {
        if (searchAlatQuery.isBlank()) {
            alatItems
        } else {
            alatItems.filter { it.namaBarang.contains(searchAlatQuery, ignoreCase = true) }
        }
    }

    // Delete confirm state
    var itemToDelete by remember { mutableStateOf<DamagedItemEntity?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // FORM SECTION
        item {
            LunarisCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Form Lapor Kerusakan Alat",
                        fontWeight = FontWeight.ExtraBold,
                        color = textColor,
                        fontSize = 18.sp
                    )

                    // Input A: Nama Alat (Searchable Dropdown + QR Button)
                    Text(
                        text = "Pilih Alat",
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        fontSize = 14.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            LunarisTextField(
                                value = if (selectedAlat != null && !dropdownExpanded) selectedAlat!!.namaBarang else searchAlatQuery,
                                onValueChange = {
                                    searchAlatQuery = it
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
                                    focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText,
                                    unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else PastelLavender,
                                    focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                    unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("lapor_search_alat_input")
                            )

                            DropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false },
                                properties = PopupProperties(focusable = false),
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                if (filteredAlatSuggestions.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("Alat tidak ditemukan") },
                                        onClick = { dropdownExpanded = false }
                                    )
                                } else {
                                    filteredAlatSuggestions.forEach { item ->
                                        DropdownMenuItem(
                                            text = { Text("${item.namaBarang} (Stok: ${item.stokTersedia} ${item.satuan})") },
                                            onClick = {
                                                selectedAlat = item
                                                searchAlatQuery = item.namaBarang
                                                dropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // QR Scan
                        IconButton(
                            onClick = { showQrScanner = true },
                            modifier = Modifier
                                    .size(54.dp)
                                    .background(
                                        if (isDark) MaterialTheme.colorScheme.primaryContainer else PastelLavender, 
                                        RoundedCornerShape(16.dp)
                                    )
                                    .testTag("btn_lapor_qr")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Pindai QR Alat",
                                tint = if (isDark) MaterialTheme.colorScheme.onPrimaryContainer else DeepPurpleText
                            )
                        }
                    }

                    // Real-time info: Kondisi Saat Ini & Stok
                    if (selectedAlat != null) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Kondisi Saat Ini: ${selectedAlat!!.kondisi.ifBlank { "Normal / Baik" }}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText
                            )
                            Text(
                                text = "Stok Tersedia: ${selectedAlat!!.stokTersedia} ${selectedAlat!!.satuan}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedAlat!!.stokTersedia > 0) Color(0xFF059669) else Color.Red
                            )
                        }
                    }

                    // Input B: Jumlah Rusak
                    Text(
                        text = "Jumlah Rusak",
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        fontSize = 14.sp
                    )
                    LunarisTextField(
                        value = jumlahRusakInput,
                        onValueChange = { jumlahRusakInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = { Text("Masukkan jumlah yang rusak/diperbaiki") },
                        singleLine = true,
                        isError = isJumlahInvalid,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText,
                            unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else PastelLavender,
                            focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                            unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                        ),
                        supportingText = {
                            if (isJumlahInvalid) {
                                Text(
                                    "Jumlah harus > 0 dan tidak boleh melebihi stok tersedia ($availableStok)!",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lapor_jumlah_input")
                    )

                    // Input C: Kondisi Baru (Dropdown)
                    Text(
                        text = "Kondisi Baru",
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        fontSize = 14.sp
                    )
                    val reportOptions = remember { listOf("Expired / Afkir", "Rusak", "Pemeliharaan", "Rusak Fisik") }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        reportOptions.chunked(2).forEach { rowOptions ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowOptions.forEach { kondisiOption ->
                                    val isSelected = selectedKondisiBaru == kondisiOption
                                    val borderCol = if (isSelected) {
                                        if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText
                                    } else {
                                        if (isDark) MaterialTheme.colorScheme.outline else Color.Gray.copy(alpha = 0.4f)
                                    }
                                    val bgCol = if (isSelected) {
                                        if (isDark) MaterialTheme.colorScheme.primaryContainer else PastelLavender
                                    } else {
                                        Color.Transparent
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(bgCol)
                                            .border(1.5.dp, borderCol, RoundedCornerShape(16.dp))
                                            .clickable { selectedKondisiBaru = kondisiOption }
                                            .testTag("btn_kondisibaru_$kondisiOption"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = kondisiOption,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isSelected) {
                                                if (isDark) MaterialTheme.colorScheme.onPrimaryContainer else DeepPurpleText
                                            } else {
                                                if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.DarkGray
                                            }
                                        )
                                    }
                                }
                                if (rowOptions.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    // Input D: Nama Petugas
                    Text(
                        text = "Nama Petugas Lapor",
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        fontSize = 14.sp
                    )
                    LunarisTextField(
                        value = namaPetugasInput,
                        onValueChange = { namaPetugasInput = it },
                        placeholder = { Text("Nama lengkap petugas...") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText,
                            unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else PastelLavender,
                            focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                            unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lapor_petugas_input")
                    )

                    // Input E: Tanggal Pencatatan
                    Text(
                        text = "Tanggal Laporan",
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        fontSize = 14.sp
                    )
                    LunarisTextField(
                        value = selectedDate,
                        onValueChange = {},
                        readOnly = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText,
                            unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else PastelLavender,
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
                            .testTag("lapor_tanggal_input")
                    )

                    // Input F: Keterangan / Kronologi
                    Text(
                        text = "Keterangan / Kronologi Kerusakan",
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        fontSize = 14.sp
                    )
                    LunarisTextField(
                        value = keteranganInput,
                        onValueChange = { keteranganInput = it },
                        placeholder = { Text("Ceritakan bagaimana kejadiannya...") },
                        maxLines = 3,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText,
                            unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else PastelLavender,
                            focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                            unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lapor_keterangan_input")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Submit Button
                    Button(
                        onClick = {
                            if (!canSubmit) return@Button
                            val currentTimeStr = SimpleDateFormat("HH:mm", Locale.US).format(Date())
                            viewModel.recordDamagedReport(
                                idBarang = selectedAlat!!.idBarang,
                                namaBarang = selectedAlat!!.namaBarang,
                                jumlah = jumlahRusak,
                                tanggalKerusakan = selectedDate,
                                waktuKerusakan = currentTimeStr,
                                keteranganKerusakan = keteranganInput,
                                namaPetugas = namaPetugasInput,
                                kondisiBaru = selectedKondisiBaru,
                                onSuccess = {
                                    Toast.makeText(context, "Laporan kerusakan berhasil dicatat & status diperbarui!", Toast.LENGTH_SHORT).show()
                                    // Reset inputs
                                    jumlahRusakInput = ""
                                    keteranganInput = ""
                                    selectedAlat = null
                                    searchAlatQuery = ""
                                },
                                onError = { err ->
                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        enabled = canSubmit,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) MaterialTheme.colorScheme.error else Color(0xFFF43F5E), // Red Coral or themed error
                            disabledContainerColor = if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_simpan_laporan")
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = "Simpan")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Kirim Laporan Kerusakan", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // HEADER: RIWAYAT LAPORAN
        item {
            val isDark = false
            val textColor = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Riwayat Laporan Kerusakan",
                    fontWeight = FontWeight.ExtraBold,
                    color = textColor,
                    fontSize = 18.sp
                )
                Badge(
                    containerColor = if (isDark) MaterialTheme.colorScheme.primaryContainer else PastelLavender,
                    contentColor = if (isDark) MaterialTheme.colorScheme.onPrimaryContainer else DeepPurpleText
                ) {
                    Text(
                        text = "${historyList.size} Laporan",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // HISTORY LIST (REVERSE LOGIC SUPPORTED)
        if (historyList.isEmpty()) {
            item {
                LunarisCard(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Kosong",
                            tint = subColor.copy(alpha = 0.5f),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Belum ada riwayat laporan kerusakan.",
                            color = subColor,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(historyList) { record ->
                LunarisCard(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("lapor_item_${record.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Report #${record.id}",
                                    fontWeight = FontWeight.Bold,
                                    color = titleColor,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = record.tanggalKerusakan,
                                    fontSize = 11.sp,
                                    color = subColor
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = record.namaBarang,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = if (isDark) MaterialTheme.colorScheme.onSurface else CarbonBlackText
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Jumlah: ${record.jumlah} unit",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.DarkGray
                                )

                                // Kondisi Baru tag
                                val isPerbaikan = record.kondisiBaru.equals("Perbaikan", ignoreCase = true)
                                val tagText = if (isPerbaikan) {
                                    if (isDark) MaterialTheme.colorScheme.primary else Color(0xFFD97706)
                                } else {
                                    if (isDark) MaterialTheme.colorScheme.error else Color(0xFFE11D48)
                                }

                                Text(
                                    text = record.kondisiBaru.ifBlank { "Rusak" },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = tagText
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Laporan oleh: ${record.namaPetugas.ifBlank { "Anonim" }}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = titleColor
                            )

                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Kronologi: ${record.keteranganKerusakan}",
                                fontSize = 12.sp,
                                color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.DarkGray
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Delete button (trash bin) - Reverse logic trigger
                        IconButton(
                            onClick = { itemToDelete = record },
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    color = if (isDark) MaterialTheme.colorScheme.errorContainer else Color(0xFFFFECEF), 
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .testTag("btn_delete_laporan_${record.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Hapus Laporan",
                                tint = if (isDark) MaterialTheme.colorScheme.onErrorContainer else Color(0xFFE11D48)
                            )
                        }
                    }
                }
            }
        }
    }

    // QR SCAN DIALOG
    if (showQrScanner) {
        CameraScannerDialog(
            title = "Pindai QR / Barcode Alat",
            initialMode = ScanMode.PRIMARY_QR,
            onDismissRequest = { showQrScanner = false },
            onBarcodeScanned = { scannedCode ->
                showQrScanner = false
                val matched = alatItems.find { 
                    it.idBarang.equals(scannedCode.trim(), ignoreCase = true) ||
                    it.serialNumber.equals(scannedCode.trim(), ignoreCase = true)
                }
                if (matched != null) {
                    selectedAlat = matched
                    searchAlatQuery = matched.namaBarang
                    Toast.makeText(context, "Alat '${matched.namaBarang}' terdeteksi!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Alat dengan ID '$scannedCode' tidak terdaftar atau merupakan bahan habis pakai!", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    // CONFIRM DELETE / REVERSE LOGIC DIALOG
    if (itemToDelete != null) {
        AlertDialog(
            shape = RoundedCornerShape(16.dp),
            onDismissRequest = { itemToDelete = null },
            title = { Text("Batalkan Laporan & Pulihkan?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Apakah Anda yakin ingin menghapus laporan ini?\n\nSistem secara otomatis akan mengembalikan status alat '${itemToDelete!!.namaBarang}' menjadi 'Normal' dan mengurangi ${itemToDelete!!.jumlah} unit dari stok rusak (Reverse Logic)."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val record = itemToDelete!!
                        itemToDelete = null
                        viewModel.cancelDamagedReport(
                            id = record.id,
                            onSuccess = {
                                Toast.makeText(context, "Laporan dibatalkan & status alat dipulihkan!", Toast.LENGTH_LONG).show()
                            },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) MaterialTheme.colorScheme.error else Color(0xFFE11D48)
                    )
                ) {
                    Text("Pulihkan & Hapus", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun SearchQrScanDialog(
    onDismiss: () -> Unit,
    onQrScanned: (String) -> Unit
) {
    CameraScannerDialog(
        title = "Pindai QR / Barcode / SN",
        initialMode = ScanMode.PRIMARY_QR,
        onDismissRequest = onDismiss,
        onBarcodeScanned = onQrScanned
    )
}

