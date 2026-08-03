package com.example.ui.screens
import com.example.ui.components.CameraScannerDialog
import com.example.ui.components.LunarisCard
import com.example.ui.components.LunarisTextField
import com.example.ui.components.FilterGroup
import com.example.ui.components.LunarisFilterDialog
import com.example.ui.components.LunarisTwoColumnFilterDialog

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.CategoryEntity
import com.example.data.entity.ItemEntity
import com.example.data.entity.UnitEntity
import com.example.data.model.ItemWithStock
import com.example.ui.viewmodel.InventoryViewModel
import com.example.ui.theme.GlassWhite
import com.example.ui.theme.GlassWhiteMore
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.DeepPurpleText
import com.example.ui.theme.SoftGoldText
import com.example.ui.theme.CarbonBlackText
import com.example.ui.theme.PastelLavender
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale
import android.Manifest
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BahanScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAfkir: ((String) -> Unit)? = null,
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

    val allItems by viewModel.allBahan.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val studentPermissions by viewModel.studentPermissions.collectAsState()

    val isBahanAllowed = userRole != "siswa" || (
        studentPermissions["bahan"] == true ||
        studentPermissions["bahan_view"] == true ||
        studentPermissions["bahan_detail"] == true ||
        studentPermissions["bahan_import"] == true ||
        studentPermissions["bahan_export"] == true
    )

    LaunchedEffect(Unit) {
        viewModel.forceRefreshState()
        viewModel.getAllBahan()
    }

    var searchQuery by remember { mutableStateOf("") }
    var showKelolaDataMenu by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var showQrScanner by remember { mutableStateOf(false) }

    var showFilterDialog by remember { mutableStateOf(false) }
    var tempSelectedCategory by remember { mutableStateOf("Semua Kategori") }
    var tempSelectedRoom by remember { mutableStateOf("Semua Ruang") }
    var tempSelectedCondition by remember { mutableStateOf("Semua Kondisi") }
    var tempSelectedSource by remember { mutableStateOf("Semua Sumber Dana") }
    
    var appliedCategory by remember { mutableStateOf("Semua Kategori") }
    var appliedRoom by remember { mutableStateOf("Semua Ruang") }
    var appliedCondition by remember { mutableStateOf("Semua Kondisi") }
    var appliedSource by remember { mutableStateOf("Semua Sumber Dana") }
    
    // Filter items to only show consumables / Bahan (type == "BAHAN")
    val filteredItems = remember(allItems, searchQuery, appliedCategory, appliedRoom, appliedCondition, appliedSource, userRole) {
        allItems.filter { it.type == "BAHAN" }
            .filter { it.namaBarang.contains(searchQuery, ignoreCase = true) || it.idBarang.contains(searchQuery, ignoreCase = true) }
            .filter { appliedCategory == "Semua Kategori" || it.kategori == appliedCategory }
            .filter { appliedRoom == "Semua Ruang" || it.ruang == appliedRoom }
            .filter { appliedCondition == "Semua Kondisi" || it.kondisi == appliedCondition }
            .filter { appliedSource == "Semua Sumber Dana" || it.sumberDana == appliedSource }
            .filter { userRole == "admin" || it.isBorrowable }
    }
    
    val lazyListState = rememberLazyListState()

    val categories by viewModel.allCategories.collectAsState()
    val units by viewModel.allUnits.collectAsState()
    val merekBahanList by viewModel.merekBahan.collectAsState()
    val ruangList by viewModel.ruang.collectAsState()
    val sumberDanaList by viewModel.sumberDana.collectAsState()
    val kondisiList by viewModel.kondisi.collectAsState()

    val matchingItemsForFilter = remember(allItems, tempSelectedRoom, tempSelectedCategory) {
        allItems.filter { it.type == "BAHAN" }
            .filter { tempSelectedRoom == "Semua Ruang" || it.ruang == tempSelectedRoom }
            .filter { tempSelectedCategory == "Semua Kategori" || it.kategori == tempSelectedCategory }
    }

    val dynamicRoomOptions = remember(allItems, tempSelectedCategory, ruangList) {
        val matching = allItems.filter { it.type == "BAHAN" }
            .filter { tempSelectedCategory == "Semua Kategori" || it.kategori == tempSelectedCategory }
        val master = if (ruangList.isNotEmpty()) ruangList else emptyList()
        val itemRooms = matching.map { it.ruang }.filter { it.isNotBlank() }
        val unique = (master + itemRooms).distinct().sorted()
        listOf("Semua Ruang") + unique
    }

    val dynamicCategoryOptions = remember(allItems, tempSelectedRoom, categories) {
        val matching = allItems.filter { it.type == "BAHAN" }
            .filter { tempSelectedRoom == "Semua Ruang" || it.ruang == tempSelectedRoom }
        val itemCats = matching.map { it.kategori }.filter { it.isNotBlank() }
        val masterCats = categories.map { it.name }
        val unique = (masterCats + itemCats).distinct().sorted()
        listOf("Semua Kategori") + unique
    }

    val dynamicConditionOptions = remember(matchingItemsForFilter, kondisiList) {
        val defaultConds = listOf("Normal", "Expired / Kedaluwarsa", "Rusak / Habis", "Perbaikan")
        val itemConds = matchingItemsForFilter.map { it.kondisi }.filter { it.isNotBlank() }
        val masterConds = if (kondisiList.isNotEmpty()) kondisiList else emptyList()
        val unique = (defaultConds + masterConds + itemConds).distinct().sorted()
        listOf("Semua Kondisi") + unique
    }

    val dynamicSourceOptions = remember(matchingItemsForFilter, sumberDanaList) {
        val itemSources = matchingItemsForFilter.mapNotNull { it.sumberDana }.filter { it.isNotBlank() }
        val masterSources = if (sumberDanaList.isNotEmpty()) sumberDanaList else emptyList()
        val unique = (masterSources + itemSources).distinct().sorted()
        listOf("Semua Sumber Dana") + unique
    }

    var showAddDialog by remember { mutableStateOf(false) }
    
    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val excelOrCsvLines = readExcelOrCsvInputStream(inputStream)
                    if (excelOrCsvLines.isNotEmpty()) {
                        viewModel.importCsvData(
                            csvLines = excelOrCsvLines,
                            defaultType = "BAHAN",
                            onSuccess = { added, updated ->
                                Toast.makeText(context, "Berhasil impor data Bahan! Baru: $added, Update: $updated", Toast.LENGTH_LONG).show()
                            },
                            onError = { err ->
                                Toast.makeText(context, "Error Impor: $err", Toast.LENGTH_LONG).show()
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

    val tipeRamList by viewModel.tipeRam.collectAsState()
    val kapasitasRamList by viewModel.kapasitasRam.collectAsState()
    val storageList by viewModel.storage.collectAsState()
    val jenisPcList by viewModel.jenisPc.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    var selectedItemForEdit by remember { mutableStateOf<ItemWithStock?>(null) }
    var selectedItemForDelete by remember { mutableStateOf<ItemWithStock?>(null) }
    var selectedItemForDetail by remember { mutableStateOf<ItemWithStock?>(null) }

    var selectedItemIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showBatchDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Add Form State
    var showQuickAddType by remember { mutableStateOf<String?>(null) }
    var quickAddInputValue by remember { mutableStateOf("") }

    var useAutoIdInput by remember { mutableStateOf(true) }
    var customIdInput by remember { mutableStateOf("") }
    var duplicateAlertMessage by remember { mutableStateOf<String?>(null) }

    var nameInput by remember { mutableStateOf("") }
    var serialNumberInput by remember { mutableStateOf("") }
    var showAddSnScanner by remember { mutableStateOf(false) }
    var categoryInput by remember { mutableStateOf("Logistik") }
    var unitInput by remember { mutableStateOf("") }
    var initialStockInput by remember { mutableStateOf("1") }
    var merekBahanInput by remember { mutableStateOf("") }
    var ruangInput by remember { mutableStateOf("") }
    var sumberDanaInput by remember { mutableStateOf("Belum Diketahui / Kosongkan") }
    var kondisiInput by remember { mutableStateOf("Normal / Baik") }
    var keteranganInput by remember { mutableStateOf("") }
    var isBorrowableInput by remember { mutableStateOf(false) }

    // Edit Form State
    var editNameInput by remember { mutableStateOf("") }
    var editSerialNumberInput by remember { mutableStateOf("") }
    var showEditSnScanner by remember { mutableStateOf(false) }
    var editCategoryInput by remember { mutableStateOf("Logistik") }
    var editUnitInput by remember { mutableStateOf("") }
    var editStockInput by remember { mutableStateOf("1") }
    var editMerekBahanInput by remember { mutableStateOf("") }
    var editRuangInput by remember { mutableStateOf("") }
    var editSumberDanaInput by remember { mutableStateOf("Belum Diketahui / Kosongkan") }
    var editKondisiInput by remember { mutableStateOf("Normal / Baik") }
    var editKeteranganInput by remember { mutableStateOf("") }
    var editIsBorrowableInput by remember { mutableStateOf(selectedItemForEdit?.isBorrowable ?: false) }

    LaunchedEffect(selectedItemForEdit, showEditDialog) {
        if (showEditDialog) {
            selectedItemForEdit?.let { item ->
                editNameInput = item.namaBarang
                editSerialNumberInput = item.serialNumber
                editCategoryInput = item.kategori
                editUnitInput = item.satuan
                editStockInput = item.stokAwal.toString()
                editMerekBahanInput = item.merekAlat ?: ""
                editRuangInput = item.ruang ?: ""
                editSumberDanaInput = item.sumberDana ?: "Belum Diketahui / Kosongkan"
                editKondisiInput = item.kondisi ?: "Normal / Baik"
                editKeteranganInput = item.keterangan ?: ""
                editIsBorrowableInput = item.isBorrowable
            }
        }
    }

    // Auto-generate ID estimate preview
    val estimatedNextId = remember(allItems) {
        var maxIdNum = 0
        allItems.forEach { item ->
            if (item.idBarang.startsWith("BRG-")) {
                val numPart = item.idBarang.substringAfter("BRG-").toIntOrNull()
                if (numPart != null && numPart > maxIdNum) {
                    maxIdNum = numPart
                }
            }
        }
        "BRG-${String.format(Locale.US, "%03d", maxIdNum + 1)}"
    }

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
                                    text = "Kelola Bahan Habis Pakai",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = androidx.compose.ui.graphics.Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Manajemen data bahan habis pakai, persediaan, dan pemakaian",
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
        floatingActionButton = {
            if (userRole == "admin") {
                FloatingActionButton(
                    onClick = {
                        categoryInput = categories.firstOrNull { it.name == "Logistik" }?.name ?: categories.firstOrNull()?.name ?: "Logistik"
                        unitInput = units.firstOrNull()?.name ?: ""
                        merekBahanInput = merekBahanList.firstOrNull() ?: ""
                        ruangInput = ruangList.firstOrNull() ?: ""
                        sumberDanaInput = "Belum Diketahui / Kosongkan"
                        kondisiInput = kondisiList.firstOrNull() ?: "Normal / Baik"
                        nameInput = ""
                        initialStockInput = "1"
                        keteranganInput = ""
                        showAddDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = androidx.compose.foundation.shape.CircleShape,
                    modifier = Modifier.testTag("btn_tambah_bahan")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Bahan")
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (!isBahanAllowed) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Akses Bahan Dibatasi",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Menu Bahan / Praktikum dinonaktifkan oleh Super Admin untuk akun Siswa.",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
            val isAddStockInvalid = initialStockInput.trim().toIntOrNull() == null || (initialStockInput.trim().toIntOrNull() ?: 0) < 1
            val isEditStockInvalid = editStockInput.trim().toIntOrNull() == null || (editStockInput.trim().toIntOrNull() ?: 0) < 1

            Column(modifier = Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 0.dp)) {
                if (userRole == "admin") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
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
                                .testTag("btn_import_csv")
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = "Impor", modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Impor", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch(Dispatchers.IO) {
                                    val bahanItems = allItems.filter { it.type == "BAHAN" }
                                    val filename = "Data_Bahan_Lunaris_${System.currentTimeMillis()}.xlsx"
                                    val headers = listOf(
                                        "nama_bahan", "kategori", "merek", "ruang", "satuan",
                                        "stok_awal", "stok_tersedia", "stok_rusak", "sumber_dana", "kondisi", "keterangan"
                                    )
                                    val rows = bahanItems.map { item ->
                                        listOf(
                                            item.namaBarang ?: "",
                                            item.kategori ?: "",
                                            item.merekAlat ?: "",
                                            item.ruang ?: "",
                                            item.satuan ?: "",
                                            item.stokAwal.toString(),
                                            item.stokTersedia.toString(),
                                            item.stokRusak.toString(),
                                            item.sumberDana ?: "",
                                            item.kondisi ?: "",
                                            item.keterangan ?: ""
                                        )
                                    }
                                    val bytes = generateExcelBytes(
                                        title = "Data Master Bahan Lunaris",
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
                                            Toast.makeText(context, "Data Bahan berhasil diekspor ke format Excel (.xlsx)!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_ekspor_csv")
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Ekspor", modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ekspor", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = {
                                val templateFilename = "Template_Impor_Bahan_Lunaris.xlsx"
                                val templateMimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                val headers = listOf(
                                    "nama_bahan", "kategori", "merek", "ruang", "satuan",
                                    "stok_awal", "sumber_dana", "kondisi", "keterangan"
                                )
                                val templateRows = listOf(
                                    listOf("Kertas HVS A4 80g PaperOne", "Logistik", "PaperOne", "Ruang TU", "Rim", "100", "BOS Reguler", "Normal / Baik", "Kertas print laporan"),
                                    listOf("Buku Tulis Sidu 38 Lembar", "Logistik", "Sinar Dunia", "Gudang Sarpras", "Pack", "100", "Bantuan Komite Sekolah", "Expired / Afkir", "Buku stok lama disisihkan")
                                )
                                val bytes = generateExcelBytes(
                                    title = "Template Impor Data Bahan Lunaris",
                                    headers = headers,
                                    rows = templateRows
                                )
                                saveFileToDownloads(
                                    context = context,
                                    filename = templateFilename,
                                    mimeType = templateMimeType,
                                    bytes = bytes
                                ) {
                                    Toast.makeText(context, "Template Excel (.xlsx) berhasil diunduh ke folder Download!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD97706)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_unduh_template")
                        ) {
                            Icon(Icons.Default.Description, contentDescription = "Template", modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Template", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LunarisTextField(
                        value = searchQuery,
                        onValueChange = { 
                            searchQuery = it
                        },
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
                            .testTag("bahan_search_bar")
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .border(
                                width = 1.5.dp,
                                color = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF7C3AED),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .background(
                                color = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { showFilterDialog = true }
                            .testTag("bahan_filter_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF7C3AED)
                        )
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

                if (filteredItems.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = "Kosong",
                                tint = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "Tidak ada bahan yang cocok dengan pencarian." else "Belum ada bahan habis pakai. Silakan tambah baru.", 
                                color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
                            )
                        }
                    }
                } else {
                    // Multi-select header bar
                    if (userRole == "admin" && filteredItems.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isAllSelected = filteredItems.isNotEmpty() && selectedItemIds.size == filteredItems.size
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    selectedItemIds = if (isAllSelected) emptySet() else filteredItems.map { it.idBarang }.toSet()
                                }
                            ) {
                                Checkbox(
                                    checked = isAllSelected,
                                    onCheckedChange = { checked ->
                                        selectedItemIds = if (checked) filteredItems.map { it.idBarang }.toSet() else emptySet()
                                    },
                                    modifier = Modifier.testTag("checkbox_select_all_bahan")
                                )
                                Text(
                                    text = if (isAllSelected) "Batal Pilih Semua" else "Pilih Semua (${filteredItems.size})",
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
                                    modifier = Modifier.testTag("btn_hapus_terpilih_bahan")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Hapus Terpilih (${selectedItemIds.size})", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    LazyColumn(
                        state = lazyListState,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(filteredItems, key = { it.idBarang }) { item ->
                            val isSelected = selectedItemIds.contains(item.idBarang)
                            LunarisCard(
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF7C3AED) else if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f) else Color(0xFFCBD5E1)
                                ),
                                colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFFF3E8FF) else cardBgColor),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { selectedItemForDetail = item }
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (userRole == "admin") {
                                                Checkbox(
                                                    checked = isSelected,
                                                    onCheckedChange = { checked ->
                                                        selectedItemIds = if (checked) {
                                                            selectedItemIds + item.idBarang
                                                        } else {
                                                            selectedItemIds - item.idBarang
                                                        }
                                                    },
                                                    modifier = Modifier.testTag("checkbox_bahan_${item.idBarang}")
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.namaBarang,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "ID: ${item.idBarang}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        if (userRole == "admin") {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Edit Button
                                                IconButton(
                                                    onClick = {
                                                        selectedItemForEdit = item
                                                        editNameInput = item.namaBarang
                                                        editCategoryInput = item.kategori
                                                        editUnitInput = item.satuan
                                                        editStockInput = item.stokAwal.toString()
                                                        editMerekBahanInput = item.merekAlat ?: ""
                                                        editRuangInput = item.ruang ?: ""
                                                        editSumberDanaInput = item.sumberDana ?: "Belum Diketahui / Kosongkan"
                                                        editKondisiInput = item.kondisi ?: "Normal"
                                                        editKeteranganInput = item.keterangan ?: ""
                                                        editIsBorrowableInput = item.isBorrowable
                                                        showEditDialog = true
                                                    },
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .testTag("edit_bahan_${item.idBarang}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Edit",
                                                        tint = if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }

                                                // Diagnosa / Afkir Bahan Button
                                                IconButton(
                                                    onClick = {
                                                        if (onNavigateToAfkir != null) {
                                                            onNavigateToAfkir(item.idBarang)
                                                        } else {
                                                            Toast.makeText(
                                                                context,
                                                                "Bahan '${item.namaBarang}' dialihkan ke menu Bahan Afkir.",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                            viewModel.updateItemDetails(
                                                                idBarang = item.idBarang,
                                                                namaBarang = item.namaBarang,
                                                                serialNumber = item.serialNumber ?: "",
                                                                kategori = item.kategori,
                                                                satuan = item.satuan,
                                                                stokAwal = item.stokAwal,
                                                                merekAlat = item.merekAlat ?: "",
                                                                ruang = item.ruang ?: "",
                                                                sumberDana = item.sumberDana ?: "",
                                                                kondisi = "Afkir",
                                                                keterangan = item.keterangan ?: "",
                                                                isBorrowable = item.isBorrowable,
                                                                onSuccess = {},
                                                                onError = {}
                                                            )
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .testTag("pemeliharaan_bahan_${item.idBarang}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.DeleteSweep,
                                                        contentDescription = "Diagnosa / Bahan Afkir",
                                                        tint = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }

                                                // Single Delete Button
                                                IconButton(
                                                    onClick = {
                                                        selectedItemForDelete = item
                                                        showDeleteConfirmDialog = true
                                                    },
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .testTag("delete_bahan_${item.idBarang}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Hapus Bahan",
                                                        tint = Color(0xFFDC2626),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Category Info
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = "Kategori: Logistik",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF7C3AED)
                                            )
                                            if (item.satuan.isNotEmpty()) {
                                                Text(
                                                    text = "Satuan: ${item.satuan}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
                                                )
                                            }
                                        }

                                        // Stock Info
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(text = "Stok", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            val (stokText, stokColor) = when {
                                                item.stokAwal > 2 -> {
                                                    "${item.stokAwal} ${item.satuan.ifEmpty { "Pcs" }} (Aman)" to Color(0xFF047857) // Hijau
                                                }
                                                item.stokAwal in 1..2 -> {
                                                    "${item.stokAwal} ${item.satuan.ifEmpty { "Pcs" }} (Menipis)" to Color(0xFFD97706) // Oranye
                                                }
                                                else -> {
                                                    "Stok Habis" to Color(0xFFB91C1C) // Merah
                                                }
                                            }
                                            Text(
                                                text = stokText,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = stokColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }



            // Dialog Tambah Bahan Baru
            if (showAddDialog) {
                AlertDialog(
                    onDismissRequest = { showAddDialog = false },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color(0xFFFFFFFF),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text("Tambah Bahan Baru", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Section 1: Identitas & Kategori
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color.White
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
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
                                            imageVector = Icons.Default.Tag,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Identitas & Kategori",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { useAutoIdInput = !useAutoIdInput }
                                            .padding(vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Gunakan ID Otomatis",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Switch(
                                            checked = useAutoIdInput,
                                            onCheckedChange = { useAutoIdInput = it },
                                            modifier = Modifier.testTag("switch_use_auto_id")
                                        )
                                    }

                                    LunarisTextField(
                                        value = if (useAutoIdInput) estimatedNextId else customIdInput,
                                        onValueChange = { if (!useAutoIdInput) customIdInput = it },
                                        label = { Text("Kode Bahan / ID") },
                                        placeholder = { Text("Misal: BHN-001") },
                                        readOnly = useAutoIdInput,
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText,
                                            unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else Color(0xFFD8B4FE),
                                            focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                            unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                                        ),
                                        modifier = Modifier.fillMaxWidth().testTag("input_bahan_id")
                                    )

                                    LunarisTextField(
                                        value = nameInput,
                                        onValueChange = { nameInput = it },
                                        label = { Text("Nama Bahan *") },
                                        placeholder = { Text("Contoh: Kertas HVS A4") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText,
                                            unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else Color(0xFFD8B4FE),
                                            focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                            unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                                        ),
                                        modifier = Modifier.fillMaxWidth().testTag("input_bahan_nama")
                                    )

                                    LunarisTextField(
                                        value = serialNumberInput,
                                        onValueChange = { serialNumberInput = it },
                                        label = { Text("Serial Number (SN) / Barcode (Opsional)") },
                                        placeholder = { Text("Ketik manual / pindai dengan kamera...") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText,
                                            unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else Color(0xFFD8B4FE),
                                            focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                            unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                                        ),
                                        trailingIcon = {
                                            IconButton(onClick = { showAddSnScanner = true }) {
                                                Icon(
                                                    imageVector = Icons.Default.QrCodeScanner,
                                                    contentDescription = "Pindai SN Bahan",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().testTag("input_bahan_sn")
                                    )

                                    DynamicDropdownField(
                                        label = "Kategori *",
                                        selectedValue = categoryInput,
                                        options = categories.map { it.name },
                                        onValueChange = { categoryInput = it },
                                        testTag = "input_bahan_kategori",
                                        onQuickAddClick = { showQuickAddType = "Kategori" }
                                    )
                                }
                            }

                            // Section 2: Merek Bahan
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color.White
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
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
                                            imageVector = Icons.Default.Build,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Merek Bahan",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    DynamicDropdownField(
                                        label = "Merek Bahan",
                                        selectedValue = merekBahanInput,
                                        options = merekBahanList,
                                        onValueChange = { merekBahanInput = it },
                                        testTag = "input_bahan_merek",
                                        onQuickAddClick = { showQuickAddType = "Merek" }
                                    )
                                }
                            }

                            // Section 3: Stok, Satuan & Status
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color.White
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
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
                                            imageVector = Icons.Default.Inventory,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Stok, Satuan & Status",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            LunarisTextField(
                                                value = initialStockInput,
                                                onValueChange = { initialStockInput = it },
                                                label = { Text("Stok Awal Fisik *") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                isError = isAddStockInvalid,
                                                supportingText = {
                                                    if (isAddStockInvalid) {
                                                        Text("Stok min 1!", color = MaterialTheme.colorScheme.error)
                                                    }
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText,
                                                    unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else Color(0xFFD8B4FE),
                                                    focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                                    unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                                                ),
                                                modifier = Modifier.fillMaxWidth().testTag("input_bahan_stok")
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            DynamicDropdownField(
                                                label = "Satuan *",
                                                selectedValue = unitInput,
                                                options = units.map { it.name },
                                                onValueChange = { unitInput = it },
                                                testTag = "input_bahan_satuan"
                                            )
                                        }
                                    }

                                    DynamicDropdownField(
                                        label = "Kondisi *",
                                        selectedValue = kondisiInput,
                                        options = kondisiList,
                                        onValueChange = { kondisiInput = it },
                                        testTag = "input_bahan_kondisi"
                                    )
                                }
                            }

                            // Section 4: Lokasi, Sumber Dana & Keterangan
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color.White
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
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
                                            imageVector = Icons.Default.Place,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Lokasi, Sumber Dana & Keterangan",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            DynamicDropdownField(
                                                label = "Ruang / Lokasi *",
                                                selectedValue = ruangInput,
                                                options = ruangList,
                                                onValueChange = { ruangInput = it },
                                                testTag = "input_bahan_ruang",
                                                onQuickAddClick = { showQuickAddType = "Ruang" }
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            DynamicDropdownField(
                                                label = "Sumber Dana (Opsional)",
                                                selectedValue = sumberDanaInput,
                                                options = listOf("Belum Diketahui / Kosongkan") + sumberDanaList,
                                                onValueChange = { sumberDanaInput = it },
                                                testTag = "input_bahan_sumber_dana",
                                                onQuickAddClick = { showQuickAddType = "Sumber Dana" }
                                            )
                                        }
                                    }

                                    LunarisTextField(
                                        value = keteranganInput,
                                        onValueChange = { keteranganInput = it },
                                        label = { Text("Keterangan") },
                                        placeholder = { Text("Keterangan tambahan...") },
                                        minLines = 2,
                                        maxLines = 4,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText,
                                            unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else Color(0xFFD8B4FE),
                                            focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                            unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                                        ),
                                        modifier = Modifier.fillMaxWidth().testTag("input_bahan_keterangan")
                                    )
                                }
                            }

                            // Section 5: Opsi Peminjaman Siswa (Posisi Paling Bawah)

                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val stock = initialStockInput.toIntOrNull()
                                if (nameInput.isBlank()) {
                                    Toast.makeText(context, "Nama bahan tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (stock == null || stock < 1) {
                                    Toast.makeText(context, "Stok awal minimal harus diisi angka 1!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                viewModel.insertBahan(
                                    name = nameInput.trim(),
                                    serialNumber = serialNumberInput.trim(),
                                    stokAwal = stock,
                                    kategori = "Logistik",
                                    satuan = unitInput,
                                    merekAlat = merekBahanInput,
                                    ruang = ruangInput,
                                    sumberDana = if (sumberDanaInput == "Belum Diketahui / Kosongkan" || sumberDanaInput.isBlank()) null else sumberDanaInput,
                                    kondisi = kondisiInput,
                                    keterangan = keteranganInput,
                                    isBorrowable = isBorrowableInput,
                                    useAutoId = useAutoIdInput,
                                    customId = customIdInput,
                                    onSuccess = {
                                        Toast.makeText(context, "Bahan baru berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                                        // Reset filters so the new item is guaranteed to show up
                                        searchQuery = ""
                                        appliedRoom = "Semua Ruang"
                                        appliedCondition = "Semua Kondisi"
                                        appliedSource = "Semua Sumber Dana"
                                        showAddDialog = false
                                        serialNumberInput = ""
                                        isBorrowableInput = false // Reset on success
                                        useAutoIdInput = true
                                        customIdInput = ""
                                    },
                                    onError = { err ->
                                        if (err.startsWith("DUPLICATE_ID:")) {
                                            val dupCode = err.substringAfter("DUPLICATE_ID:")
                                            duplicateAlertMessage = "Oops! 😊 Sepertinya Kode Bahan/Barang '$dupCode' sudah terdaftar di sistem kita. Mohon gunakan kode yang lain atau aktifkan kembali mode ID Otomatis ya! ✨"
                                        } else {
                                            Toast.makeText(context, "Gagal: $err", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                )
                            },
                            enabled = !isAddStockInvalid && nameInput.isNotBlank(),
                            modifier = Modifier.testTag("dialog_btn_simpan_bahan")
                        ) {
                            Text("Simpan")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddDialog = false }) {
                            Text("Batal")
                        }
                    }
                )
            }

            if (showAddSnScanner) {
                CameraScannerDialog(
                    onDismissRequest = { showAddSnScanner = false },
                    onBarcodeScanned = { scannedCode ->
                        serialNumberInput = scannedCode
                        showAddSnScanner = false
                    }
                )
            }

            // Dialog Warning Kode Barang Duplikat
            if (duplicateAlertMessage != null) {
                AlertDialog(
                    onDismissRequest = { duplicateAlertMessage = null },
                    shape = RoundedCornerShape(16.dp),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Kode Barang Terdaftar ✨", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    },
                    text = {
                        Text(
                            text = duplicateAlertMessage ?: "",
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = { duplicateAlertMessage = null },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Mengerti 😊", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            // Dialog Quick Add
            if (showQuickAddType != null) {
                AlertDialog(
                    onDismissRequest = { 
                        showQuickAddType = null 
                        quickAddInputValue = ""
                    },
                    shape = RoundedCornerShape(16.dp),
                    title = { Text("Tambah ${showQuickAddType} Baru", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Masukkan nama ${showQuickAddType?.lowercase()} baru yang ingin ditambahkan.",
                                fontSize = 14.sp
                            )
                            LunarisTextField(
                                value = quickAddInputValue,
                                onValueChange = { quickAddInputValue = it },
                                label = { Text("Nama ${showQuickAddType}") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth().testTag("quick_add_input")
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val value = quickAddInputValue.trim()
                                if (value.isEmpty()) {
                                    Toast.makeText(context, "Nama tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val isDuplicate = when (showQuickAddType) {
                                    "Kategori" -> categories.any { it.name.equals(value, ignoreCase = true) }
                                    "Merek" -> merekBahanList.any { it.equals(value, ignoreCase = true) }
                                    "Ruang" -> ruangList.any { it.equals(value, ignoreCase = true) }
                                    "Sumber Dana" -> sumberDanaList.any { it.equals(value, ignoreCase = true) }
                                    else -> false
                                }
                                if (isDuplicate) {
                                    Toast.makeText(context, "Nama ${showQuickAddType?.lowercase()} sudah ada/terdaftar!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                when (showQuickAddType) {
                                    "Kategori" -> {
                                        viewModel.addCategory(
                                            name = value,
                                            onSuccess = {
                                                categoryInput = value
                                                showQuickAddType = null
                                                quickAddInputValue = ""
                                                Toast.makeText(context, "Kategori berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                                            },
                                            onError = { err ->
                                                Toast.makeText(context, "Gagal: $err", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                    "Merek" -> {
                                        val updated = (viewModel.merekBahan.value + value).distinct()
                                        viewModel.updateMerekBahan(updated)
                                        merekBahanInput = value
                                        showQuickAddType = null
                                        quickAddInputValue = ""
                                        Toast.makeText(context, "Merek berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                                    }
                                    "Ruang" -> {
                                        val updated = (viewModel.ruang.value + value).distinct()
                                        viewModel.updateRuang(updated)
                                        ruangInput = value
                                        showQuickAddType = null
                                        quickAddInputValue = ""
                                        Toast.makeText(context, "Ruang berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                                    }
                                    "Sumber Dana" -> {
                                        val updated = (viewModel.sumberDana.value + value).distinct()
                                        viewModel.updateSumberDana(updated)
                                        sumberDanaInput = value
                                        showQuickAddType = null
                                        quickAddInputValue = ""
                                        Toast.makeText(context, "Sumber Dana berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.testTag("quick_add_btn_simpan")
                        ) {
                            Text("Simpan")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { 
                            showQuickAddType = null 
                            quickAddInputValue = ""
                        }) {
                            Text("Batal")
                        }
                    }
                )
            }

            // Dialog Edit Bahan
            if (showEditDialog && selectedItemForEdit != null) {
                AlertDialog(
                    onDismissRequest = { showEditDialog = false },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color(0xFFFFFFFF),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text("Ubah Data Bahan", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Section 1: Identitas & Kategori
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color.White
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
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
                                            imageVector = Icons.Default.Tag,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Identitas & Kategori",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Kode / ID Bahan: ${selectedItemForEdit!!.idBarang}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        )
                                    }

                                    LunarisTextField(
                                        value = editNameInput,
                                        onValueChange = { editNameInput = it },
                                        label = { Text("Nama Bahan *") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText,
                                            unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else Color(0xFFD8B4FE),
                                            focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                            unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                                        ),
                                        modifier = Modifier.fillMaxWidth().testTag("edit_bahan_nama")
                                    )

                                    LunarisTextField(
                                        value = editSerialNumberInput,
                                        onValueChange = { editSerialNumberInput = it },
                                        label = { Text("Serial Number (SN) / Barcode (Opsional)") },
                                        placeholder = { Text("Ketik manual / pindai dengan kamera...") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText,
                                            unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else Color(0xFFD8B4FE),
                                            focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                            unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                                        ),
                                        trailingIcon = {
                                            IconButton(onClick = { showEditSnScanner = true }) {
                                                Icon(
                                                    imageVector = Icons.Default.QrCodeScanner,
                                                    contentDescription = "Pindai SN Bahan",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().testTag("edit_bahan_sn")
                                    )

                                    DynamicDropdownField(
                                        label = "Kategori *",
                                        selectedValue = editCategoryInput,
                                        options = categories.map { it.name },
                                        onValueChange = { editCategoryInput = it },
                                        testTag = "edit_bahan_kategori",
                                        onQuickAddClick = { showQuickAddType = "Kategori" }
                                    )
                                }
                            }

                            // Section 2: Merek Bahan
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color.White
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
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
                                            imageVector = Icons.Default.Build,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Merek Bahan",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    DynamicDropdownField(
                                        label = "Merek Bahan",
                                        selectedValue = editMerekBahanInput,
                                        options = merekBahanList,
                                        onValueChange = { editMerekBahanInput = it },
                                        testTag = "edit_bahan_merek",
                                        onQuickAddClick = { showQuickAddType = "Merek" }
                                    )
                                }
                            }

                            // Section 3: Stok, Satuan & Status
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color.White
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
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
                                            imageVector = Icons.Default.Inventory,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Stok, Satuan & Status",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            LunarisTextField(
                                                value = editStockInput,
                                                onValueChange = { editStockInput = it },
                                                label = { Text("Stok Awal Fisik *") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                isError = isEditStockInvalid,
                                                supportingText = {
                                                    if (isEditStockInvalid) {
                                                        Text("Stok min 1!", color = MaterialTheme.colorScheme.error)
                                                    }
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText,
                                                    unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else Color(0xFFD8B4FE),
                                                    focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                                    unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                                                ),
                                                modifier = Modifier.fillMaxWidth().testTag("edit_bahan_stok")
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            DynamicDropdownField(
                                                label = "Satuan *",
                                                selectedValue = editUnitInput,
                                                options = units.map { it.name },
                                                onValueChange = { editUnitInput = it },
                                                testTag = "edit_bahan_satuan"
                                            )
                                        }
                                    }

                                    DynamicDropdownField(
                                        label = "Kondisi *",
                                        selectedValue = editKondisiInput,
                                        options = kondisiList,
                                        onValueChange = { editKondisiInput = it },
                                        testTag = "edit_bahan_kondisi"
                                    )
                                }
                            }

                            // Section 4: Lokasi, Sumber Dana & Keterangan
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color.White
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
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
                                            imageVector = Icons.Default.Place,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Lokasi, Sumber Dana & Keterangan",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            DynamicDropdownField(
                                                label = "Ruang / Lokasi *",
                                                selectedValue = editRuangInput,
                                                options = ruangList,
                                                onValueChange = { editRuangInput = it },
                                                testTag = "edit_bahan_ruang",
                                                onQuickAddClick = { showQuickAddType = "Ruang" }
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            DynamicDropdownField(
                                                label = "Sumber Dana (Opsional)",
                                                selectedValue = editSumberDanaInput,
                                                options = listOf("Belum Diketahui / Kosongkan") + sumberDanaList,
                                                onValueChange = { editSumberDanaInput = it },
                                                testTag = "edit_bahan_sumber_dana",
                                                onQuickAddClick = { showQuickAddType = "Sumber Dana" }
                                            )
                                        }
                                    }

                                    LunarisTextField(
                                        value = editKeteranganInput,
                                        onValueChange = { editKeteranganInput = it },
                                        label = { Text("Keterangan") },
                                        placeholder = { Text("Keterangan tambahan...") },
                                        minLines = 2,
                                        maxLines = 4,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText,
                                            unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else Color(0xFFD8B4FE),
                                            focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                            unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                                        ),
                                        modifier = Modifier.fillMaxWidth().testTag("edit_bahan_keterangan")
                                    )
                                }
                            }

                            // Section 5: Opsi Peminjaman Siswa (Posisi Paling Bawah)

                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val stock = editStockInput.toIntOrNull()
                                if (editNameInput.isBlank()) {
                                    Toast.makeText(context, "Nama bahan tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (stock == null || stock < 1) {
                                    Toast.makeText(context, "Stok awal minimal harus diisi angka 1!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                viewModel.updateItemDetails(
                                    idBarang = selectedItemForEdit!!.idBarang,
                                    namaBarang = editNameInput.trim(),
                                    serialNumber = editSerialNumberInput.trim(),
                                    kategori = editCategoryInput,
                                    satuan = editUnitInput,
                                    stokAwal = stock,
                                    merekAlat = editMerekBahanInput,
                                    ruang = editRuangInput,
                                    sumberDana = if (editSumberDanaInput == "Belum Diketahui / Kosongkan" || editSumberDanaInput.isBlank()) null else editSumberDanaInput,
                                    kondisi = editKondisiInput,
                                    keterangan = editKeteranganInput,
                                    isBorrowable = editIsBorrowableInput,
                                    onSuccess = {
                                        Toast.makeText(context, "Data bahan berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                                        showEditDialog = false
                                    },
                                    onError = { err ->
                                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            enabled = !isEditStockInvalid && editNameInput.isNotBlank(),
                            modifier = Modifier.testTag("dialog_btn_update_bahan")
                        ) {
                            Text("Simpan Perubahan")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEditDialog = false }) {
                            Text("Batal")
                        }
                    }
                )
            }

            if (showEditSnScanner) {
                CameraScannerDialog(
                    onDismissRequest = { showEditSnScanner = false },
                    onBarcodeScanned = { scannedCode ->
                        editSerialNumberInput = scannedCode
                        showEditSnScanner = false
                    }
                )
            }

            // Dialog Konfirmasi Hapus Bahan
            if (showDeleteConfirmDialog && selectedItemForDelete != null) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmDialog = false },
                    title = { Text("Hapus Bahan", fontWeight = FontWeight.Bold) },
                    text = {
                        Text("Apakah Anda yakin ingin menghapus bahan '${selectedItemForDelete!!.namaBarang}' dari database lokal dan Firestore? Fitur ini diperuntukkan khusus bagi koreksi data input yang keliru.")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.deleteItem(
                                    idBarang = selectedItemForDelete!!.idBarang,
                                    onSuccess = {
                                        Toast.makeText(context, "Bahan berhasil dihapus!", Toast.LENGTH_SHORT).show()
                                        showDeleteConfirmDialog = false
                                    },
                                    onError = { err ->
                                        Toast.makeText(context, "Gagal menghapus: $err", Toast.LENGTH_LONG).show()
                                        showDeleteConfirmDialog = false
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.testTag("dialog_btn_konfirmasi_hapus")
                        ) {
                            Text("Hapus")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirmDialog = false }) {
                            Text("Batal")
                        }
                    }
                )
            }

            // Dialog Konfirmasi Hapus Massal (Batch Delete)
            if (showBatchDeleteConfirmDialog && selectedItemIds.isNotEmpty()) {
                AlertDialog(
                    onDismissRequest = { showBatchDeleteConfirmDialog = false },
                    title = { Text("Konfirmasi Hapus ${selectedItemIds.size} Data Bahan", fontWeight = FontWeight.Bold) },
                    text = {
                        Text("Apakah Anda yakin ingin menghapus ${selectedItemIds.size} data bahan terpilih secara permanen dari database lokal dan Firestore? Fitur ini diperuntukkan khusus bagi koreksi data input yang keliru (bulk import) dan tindakan ini tidak dapat dibatalkan.")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val idsToDelete = selectedItemIds.toList()
                                var deletedCount = 0
                                idsToDelete.forEach { id ->
                                    viewModel.deleteItem(
                                        idBarang = id,
                                        onSuccess = { deletedCount++ },
                                        onError = {}
                                    )
                                }
                                Toast.makeText(context, "Berhasil menghapus $deletedCount data bahan!", Toast.LENGTH_SHORT).show()
                                selectedItemIds = emptySet()
                                showBatchDeleteConfirmDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            modifier = Modifier.testTag("dialog_btn_confirm_batch_delete_bahan")
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
            
            // Detail Bahan Pop-up Dialog (Read-Only Audit Card)
            selectedItemForDetail?.let { detailItem ->
                val isCritical = detailItem.stokTersedia <= 0 || detailItem.stokTersedia < 5
                AlertDialog(
                    onDismissRequest = { selectedItemForDetail = null },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text(detailItem.namaBarang, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("ID: ${detailItem.idBarang} | ${detailItem.kategori}", fontSize = 12.sp, color = Color.Gray)
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
                                color = if (isCritical) Color(0xFFFEF2F2) else Color(0xFFDCFCE7),
                                border = BorderStroke(1.dp, if (isCritical) Color(0xFFFCA5A5) else Color(0xFF10B981))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isCritical) Icons.Default.Warning else Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = if (isCritical) Color(0xFFDC2626) else Color(0xFF15803D),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = if (isCritical) "Stok Kritis / Perlu Restock" else "Stok Tersedia / Ready",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isCritical) Color(0xFFDC2626) else Color(0xFF15803D)
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Sisa Stok: ${detailItem.stokTersedia} ${detailItem.satuan} (Total Fisik: ${detailItem.stokAwal} ${detailItem.satuan})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isCritical) Color(0xFFDC2626) else Color(0xFF15803D)
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            // Detail Specs
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Informasi Detail Bahan Habis Pakai:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                if (!detailItem.ruang.isNullOrEmpty()) Text("• Ruang / Lokasi Storage: ${detailItem.ruang}", fontSize = 12.sp)
                                if (!detailItem.merekAlat.isNullOrEmpty()) Text("• Merek / Brand: ${detailItem.merekAlat}", fontSize = 12.sp)
                                Text("• Stok Fisik Awal: ${detailItem.stokAwal} ${detailItem.satuan}", fontSize = 12.sp)
                                Text("• Sisa Stok Tersedia: ${detailItem.stokTersedia} ${detailItem.satuan}", fontSize = 12.sp)
                                if (!detailItem.sumberDana.isNullOrEmpty()) Text("• Sumber Dana: ${detailItem.sumberDana}", fontSize = 12.sp)
                                if (!detailItem.keterangan.isNullOrEmpty()) Text("• Keterangan: ${detailItem.keterangan}", fontSize = 12.sp)
                                Text("• Kondisi: ${detailItem.kondisi ?: "Normal"}", fontSize = 12.sp)
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            // Audit Trail Read-Only
                            Text("Riwayat Jejak Audit & Pemakaian (Read-Only):", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
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
                                        Text("• Status Opname Bahan:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                                        Text("Terverifikasi Audit", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("• Akurasi Fisik vs Sistem:", fontSize = 11.sp, color = Color(0xFF64748B))
                                        Text("${detailItem.stokTersedia} ${detailItem.satuan} Valid", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1E293B))
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("• Jejak Distribusi Terakhir:", fontSize = 11.sp, color = Color(0xFF64748B))
                                        Text("Pencatatan Rutin Praktikum", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1E293B))
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (onNavigateToAfkir != null) {
                                Button(
                                    onClick = {
                                        val itemToOper = detailItem
                                        selectedItemForDetail = null
                                        onNavigateToAfkir(itemToOper.idBarang)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Masukkan ke Bahan Afkir", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            TextButton(onClick = { selectedItemForDetail = null }) {
                                Text("Tutup", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                )
            }

            if (showFilterDialog) {
                LunarisTwoColumnFilterDialog(
                    title = "Filter Data Bahan",
                    onDismissRequest = {
                        tempSelectedCategory = appliedCategory
                        tempSelectedRoom = appliedRoom
                        tempSelectedCondition = appliedCondition
                        tempSelectedSource = appliedSource
                        showFilterDialog = false
                    },
                    leftColumnGroups = listOf(
                        FilterGroup(
                            title = "Ruang / Lokasi",
                            options = dynamicRoomOptions,
                            selectedOption = tempSelectedRoom,
                            onOptionSelected = { tempSelectedRoom = it }
                        ),
                        FilterGroup(
                            title = "Kategori Bahan",
                            options = dynamicCategoryOptions,
                            selectedOption = tempSelectedCategory,
                            onOptionSelected = { tempSelectedCategory = it }
                        )
                    ),
                    rightColumnGroups = listOf(
                        FilterGroup(
                            title = "Status Kondisi",
                            options = dynamicConditionOptions,
                            selectedOption = tempSelectedCondition,
                            onOptionSelected = { tempSelectedCondition = it }
                        ),
                        FilterGroup(
                            title = "Sumber Dana",
                            options = dynamicSourceOptions,
                            selectedOption = tempSelectedSource,
                            onOptionSelected = { tempSelectedSource = it }
                        )
                    ),
                    onReset = {
                        tempSelectedCategory = "Semua Kategori"
                        tempSelectedRoom = "Semua Ruang"
                        tempSelectedCondition = "Semua Kondisi"
                        tempSelectedSource = "Semua Sumber Dana"
                    },
                    onApply = {
                        appliedCategory = tempSelectedCategory
                        appliedRoom = tempSelectedRoom
                        appliedCondition = tempSelectedCondition
                        appliedSource = tempSelectedSource
                        showFilterDialog = false
                    }
                )
            }
            }
        }
    }
}

private fun parseCsvLine(line: String, delimiter: Char): List<String> {
    val result = mutableListOf<String>()
    var current = java.lang.StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < line.length) {
        val c = line[i]
        if (c == '\"') {
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
