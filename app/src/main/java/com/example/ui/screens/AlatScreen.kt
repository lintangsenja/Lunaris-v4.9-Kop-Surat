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
import android.Manifest
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlatScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPemeliharaan: ((String) -> Unit)? = null,
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
    val userRole by viewModel.userRole.collectAsState()
    val studentPermissions by viewModel.studentPermissions.collectAsState()

    val isAlatAllowed = userRole != "siswa" || (
        studentPermissions["alat"] == true ||
        studentPermissions["alat_view"] == true ||
        studentPermissions["alat_detail"] == true ||
        studentPermissions["alat_import"] == true ||
        studentPermissions["alat_export"] == true
    )

    var searchQuery by remember { mutableStateOf("") }
    var showKelolaDataMenu by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var showQrScanner by remember { mutableStateOf(false) }

    var showFilterDialog by remember { mutableStateOf(false) }
    var tempSelectedCategory by remember { mutableStateOf("Semua Kategori") }
    var tempSelectedRoom by remember { mutableStateOf("Semua Ruang") }
    var tempSelectedCondition by remember { mutableStateOf("Semua Kondisi") }
    var tempSelectedMerekAlat by remember { mutableStateOf("Semua Merek Alat") }
    
    var appliedCategory by remember { mutableStateOf("Semua Kategori") }
    var appliedRoom by remember { mutableStateOf("Semua Ruang") }
    var appliedCondition by remember { mutableStateOf("Semua Kondisi") }
    var appliedMerekAlat by remember { mutableStateOf("Semua Merek Alat") }
    
    // Filter items to only show durable goods / Alat (type == "ALAT")
    val filteredItems = remember(allItems, searchQuery, appliedCategory, appliedRoom, appliedCondition, appliedMerekAlat, userRole) {
        allItems.filter { it.type == "ALAT" }
            .filter { it.namaBarang.contains(searchQuery, ignoreCase = true) || it.idBarang.contains(searchQuery, ignoreCase = true) || it.merekAlat.contains(searchQuery, ignoreCase = true) }
            .filter { appliedCategory == "Semua Kategori" || it.kategori == appliedCategory }
            .filter { appliedRoom == "Semua Ruang" || it.ruang == appliedRoom }
            .filter { appliedCondition == "Semua Kondisi" || it.kondisi == appliedCondition }
            .filter { appliedMerekAlat == "Semua Merek Alat" || it.merekAlat == appliedMerekAlat }
            .filter { userRole == "admin" || it.isBorrowable }
    }
    
    val lazyListState = rememberLazyListState()
    
    val allCategories by viewModel.allCategories.collectAsState()
    val merekAlatList by viewModel.merekAlat.collectAsState()
    val ruangList by viewModel.ruang.collectAsState()
    val sumberDanaList by viewModel.sumberDana.collectAsState()
    val kondisiList by viewModel.kondisi.collectAsState()
    val tipeRamList by viewModel.tipeRam.collectAsState()
    val kapasitasRamList by viewModel.kapasitasRam.collectAsState()
    val storageList by viewModel.storage.collectAsState()
    val jenisPcList by viewModel.jenisPc.collectAsState()

    // Filter categories for Alat
    val categories = remember(allCategories) {
        allCategories.filter { it.name != "Logistik" }
    }

    val categoriesOptions = remember(categories) {
        listOf("Semua Kategori") + categories.map { it.name }
    }
    
    val roomOptions = remember(ruangList, allItems) {
        val masterRuangs = if (ruangList.isNotEmpty()) ruangList else listOf("Lab Komputer 1", "Lab Komputer 2", "Lab Server / NOC")
        val itemRuangs = allItems.filter { it.kategori != "Logistik" && it.ruang.isNotEmpty() }.map { it.ruang }
        val uniqueRooms = (masterRuangs + itemRuangs).distinct().sorted()
        listOf("Semua Ruang") + uniqueRooms
    }
    
    val conditionOptions = remember(kondisiList, allItems) {
        val masterKondisi = if (kondisiList.isNotEmpty()) kondisiList else listOf("Normal", "Rusak", "Perbaikan")
        val itemKondisi = allItems.map { it.kondisi }.filter { it.isNotEmpty() }
        val uniqueKondisi = (masterKondisi + itemKondisi).distinct().sorted()
        listOf("Semua Kondisi") + uniqueKondisi
    }
    
    val units by viewModel.allUnits.collectAsState()

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
                            defaultType = "ALAT",
                            onSuccess = { added, updated ->
                                Toast.makeText(context, "Berhasil impor data Alat! Baru: $added, Update: $updated", Toast.LENGTH_LONG).show()
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
    
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showRepairDialog by remember { mutableStateOf(false) }

    var selectedItemForEdit by remember { mutableStateOf<ItemWithStock?>(null) }
    var selectedItemForDelete by remember { mutableStateOf<ItemWithStock?>(null) }
    var selectedItemForRepair by remember { mutableStateOf<ItemWithStock?>(null) }
    var selectedItemForDetail by remember { mutableStateOf<ItemWithStock?>(null) }

    // Add Form State
    var showQuickAddType by remember { mutableStateOf<String?>(null) }
    var quickAddInputValue by remember { mutableStateOf("") }

    var useAutoIdInput by remember { mutableStateOf(true) }
    var customIdInput by remember { mutableStateOf("") }
    var duplicateAlertMessage by remember { mutableStateOf<String?>(null) }

    var nameInput by remember { mutableStateOf("") }
    var serialNumberInput by remember { mutableStateOf("") }
    var showAddSnScanner by remember { mutableStateOf(false) }
    var categoryInput by remember { mutableStateOf("") }
    var unitInput by remember { mutableStateOf("") }
    var initialStockInput by remember { mutableStateOf("1") }
    var merekAlatInput by remember { mutableStateOf("") }
    var ruangInput by remember { mutableStateOf("") }
    var sumberDanaInput by remember { mutableStateOf("Belum Diketahui / Kosongkan") }
    var kondisiInput by remember { mutableStateOf("Normal") }
    var keteranganInput by remember { mutableStateOf("") }
    var isBorrowableInput by remember { mutableStateOf(false) }

    // Edit Form State
    var editNameInput by remember { mutableStateOf("") }
    var editSerialNumberInput by remember { mutableStateOf("") }
    var showEditSnScanner by remember { mutableStateOf(false) }
    var editCategoryInput by remember { mutableStateOf("") }
    var editUnitInput by remember { mutableStateOf("") }
    var editStockInput by remember { mutableStateOf("1") }
    var editMerekAlatInput by remember { mutableStateOf("") }
    var editRuangInput by remember { mutableStateOf("") }
    var editSumberDanaInput by remember { mutableStateOf("Belum Diketahui / Kosongkan") }
    var editKondisiInput by remember { mutableStateOf("Normal") }
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
                editMerekAlatInput = item.merekAlat
                editRuangInput = item.ruang
                editSumberDanaInput = item.sumberDana ?: "Belum Diketahui / Kosongkan"
                editKondisiInput = item.kondisi
                editKeteranganInput = item.keterangan
                editIsBorrowableInput = item.isBorrowable
            }
        }
    }

    var repairQtyInput by remember { mutableStateOf("") }

    // Auto-generate ID estimate preview (use global items for sequence)
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
                                    text = "Kelola Alat & Inventaris",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = androidx.compose.ui.graphics.Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Manajemen data barang, stok, dan spesifikasi alat gudang",
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
                        categoryInput = categories.firstOrNull()?.name ?: ""
                        unitInput = units.firstOrNull()?.name ?: ""
                        merekAlatInput = merekAlatList.firstOrNull() ?: ""
                        ruangInput = ruangList.firstOrNull() ?: ""
                        sumberDanaInput = "Belum Diketahui / Kosongkan"
                        kondisiInput = kondisiList.firstOrNull() ?: "Normal"
                        nameInput = ""
                        initialStockInput = "1"
                        keteranganInput = ""
                        showAddDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = androidx.compose.foundation.shape.CircleShape,
                    modifier = Modifier.testTag("btn_tambah_barang_baru")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Alat")
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (!isAlatAllowed) {
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
                            text = "Akses Alat Dibatasi",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Menu Alat dinonaktifkan oleh Super Admin untuk akun Siswa.",
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
                                    val alatItems = allItems.filter { it.type == "ALAT" }
                                    val filename = "Data_Alat_Lunaris_${System.currentTimeMillis()}.xlsx"
                                    val headers = listOf(
                                        "nama_alat", "kategori", "merek", "ruang", "satuan",
                                        "stok_awal", "stok_tersedia", "stok_rusak", "sumber_dana", "kondisi", "keterangan"
                                    )
                                    val rows = alatItems.map { item ->
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
                                        title = "Data Master Alat Lunaris",
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
                                            Toast.makeText(context, "Data Alat berhasil diekspor ke format Excel (.xlsx)!", Toast.LENGTH_SHORT).show()
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
                                val templateFilename = "Template_Impor_Alat_Lunaris.xlsx"
                                val templateMimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                val headers = listOf(
                                    "nama_alat", "kategori", "merek", "ruang", "satuan",
                                    "stok_awal", "sumber_dana", "kondisi", "keterangan"
                                )
                                val templateRows = listOf(
                                    listOf("Laptop ASUS Core i5", "Elektronik", "ASUS", "Lab Komputer 1", "Unit", "15", "BOS Reguler", "Sangat Baik", "Laptop untuk ujian"),
                                    listOf("Proyektor Epson EB-X400", "Elektronik", "Epson", "Aula Utama", "Unit", "15", "BOS Kinerja", "Baik (Siap Pakai)", "Proyektor presentasi"),
                                    listOf("Kamera DSLR Canon EOS 200D", "Elektronik", "Canon", "Studio Foto", "Unit", "5", "BOS Reguler", "Sangat Baik", "Kamera praktek siswa")
                                )
                                val bytes = generateExcelBytes(
                                    title = "Template Impor Data Alat Lunaris",
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
                            .testTag("alat_search_bar")
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
                            .testTag("alat_filter_button"),
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
                                imageVector = Icons.Default.Inventory,
                                contentDescription = "Kosong",
                                tint = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "Tidak ada alat yang cocok dengan pencarian." else "Belum ada alat. Silakan tambah alat baru.", 
                                color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = lazyListState,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(filteredItems, key = { it.idBarang }) { item ->
                            LunarisCard(
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f) else Color(0xFFCBD5E1)),
                                colors = CardDefaults.cardColors(containerColor = cardBgColor),
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
                                            if (item.merekAlat.isNotEmpty() || item.ruang.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = buildString {
                                                        if (item.merekAlat.isNotEmpty()) append("Merek: ${item.merekAlat}")
                                                        if (item.ruang.isNotEmpty()) {
                                                            if (isNotEmpty()) append(" | ")
                                                            append("Ruang: ${item.ruang}")
                                                        }
                                                    },
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Normal,
                                                    color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
                                                )
                                            }
                                            if (item.kondisi.isNotEmpty() || !item.sumberDana.isNullOrEmpty()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = buildString {
                                                        if (item.kondisi.isNotEmpty()) append("Kondisi: ${item.kondisi}")
                                                        if (!item.sumberDana.isNullOrEmpty()) {
                                                            if (isNotEmpty()) append(" | ")
                                                            append("Dana: ${item.sumberDana}")
                                                        }
                                                    },
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Normal,
                                                    color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
                                                )
                                            }
                                            if (item.keterangan.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Keterangan: ${item.keterangan}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Normal,
                                                    color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
                                                )
                                            }
                                        }

                                        if (userRole == "admin") {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Repair Button
                                                if (item.stokRusak > 0) {
                                                    IconButton(
                                                        onClick = {
                                                            selectedItemForRepair = item
                                                            repairQtyInput = item.stokRusak.toString()
                                                            showRepairDialog = true
                                                        },
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .testTag("repair_barang_${item.idBarang}")
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Build,
                                                            contentDescription = "Perbaiki",
                                                            tint = Color(0xFF0284C7),
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }

                                                // Edit Button
                                                IconButton(
                                                    onClick = {
                                                        selectedItemForEdit = item
                                                        editNameInput = item.namaBarang
                                                        editCategoryInput = item.kategori
                                                        editUnitInput = item.satuan
                                                        editStockInput = item.stokAwal.toString()
                                                        editMerekAlatInput = item.merekAlat
                                                        editRuangInput = item.ruang
                                                        editSumberDanaInput = item.sumberDana ?: "Belum Diketahui / Kosongkan"
                                                        editKondisiInput = item.kondisi
                                                        editKeteranganInput = item.keterangan
                                                        editIsBorrowableInput = item.isBorrowable
                                                        showEditDialog = true
                                                    },
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .testTag("edit_barang_${item.idBarang}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Edit",
                                                        tint = if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }

                                                // Pemeliharaan Button
                                                IconButton(
                                                    onClick = {
                                                        if (onNavigateToPemeliharaan != null) {
                                                            onNavigateToPemeliharaan(item.idBarang)
                                                        } else {
                                                            Toast.makeText(
                                                                context,
                                                                "Alat '${item.namaBarang}' dimasukkan ke jadwal Pemeliharaan.",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                            viewModel.updateItemDetails(
                                                                idBarang = item.idBarang,
                                                                namaBarang = item.namaBarang,
                                                                serialNumber = item.serialNumber ?: "",
                                                                kategori = item.kategori,
                                                                satuan = item.satuan,
                                                                stokAwal = item.stokAwal,
                                                                merekAlat = item.merekAlat,
                                                                ruang = item.ruang,
                                                                sumberDana = item.sumberDana ?: "",
                                                                kondisi = "Perlu Perawatan",
                                                                keterangan = item.keterangan,
                                                                isBorrowable = item.isBorrowable,
                                                                onSuccess = {},
                                                                onError = {}
                                                            )
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .testTag("pemeliharaan_barang_${item.idBarang}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Build,
                                                        contentDescription = "Pemeliharaan",
                                                        tint = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
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
                                        // Info Kategori & Satuan
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            if (item.kategori.isNotEmpty()) {
                                                Text(
                                                    text = "Kategori: ${item.kategori}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF7C3AED)
                                                )
                                            }
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
                                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(text = "Stok Fisik", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "${item.stokAwal} ${item.satuan.ifEmpty { "Pcs" }}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF1E40AF)
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(text = "Tersedia", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                val (stokText, stokColor) = when {
                                                    item.stokTersedia > 2 -> {
                                                        "${item.stokTersedia} ${item.satuan.ifEmpty { "Pcs" }} (Aman)" to Color(0xFF047857) // Hijau
                                                    }
                                                    item.stokTersedia in 1..2 -> {
                                                        "${item.stokTersedia} ${item.satuan.ifEmpty { "Pcs" }} (Menipis)" to Color(0xFFD97706) // Oranye
                                                    }
                                                    else -> {
                                                        "Stok Habis" to Color(0xFFB91C1C) // Merah
                                                    }
                                                }
                                                Text(
                                                    text = stokText,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = stokColor
                                                )
                                            }
                                            if (item.stokRusak > 0) {
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(text = "Rusak", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray)
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = "${item.stokRusak} ${item.satuan.ifEmpty { "Pcs" }}",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFB91C1C)
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

            // Dialog Tambah Barang Baru
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
                            Text("Tambah Alat Baru", fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
                                        label = { Text("Kode Barang / ID") },
                                        placeholder = { Text("Misal: BRG-001") },
                                        readOnly = useAutoIdInput,
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText,
                                            unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else Color(0xFFD8B4FE),
                                            focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                            unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                                        ),
                                        modifier = Modifier.fillMaxWidth().testTag("input_barang_id")
                                    )

                                     LunarisTextField(
                                        value = nameInput,
                                        onValueChange = { nameInput = it },
                                        label = { Text("Nama Alat *") },
                                        placeholder = { Text("Contoh: Laptop Asus") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText,
                                            unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else Color(0xFFD8B4FE),
                                            focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                            unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                                        ),
                                        modifier = Modifier.fillMaxWidth().testTag("input_barang_nama")
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
                                                    contentDescription = "Pindai SN Alat",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().testTag("input_barang_sn")
                                    )

                                    DynamicDropdownField(
                                        label = "Kategori *",
                                        selectedValue = categoryInput,
                                        options = categories.map { it.name },
                                        onValueChange = { categoryInput = it },
                                        testTag = "input_barang_kategori",
                                        onQuickAddClick = { showQuickAddType = "Kategori" }
                                    )
                                }
                            }

                            // Section 2: Merek Alat
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
                                            text = "Merek Alat",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    DynamicDropdownField(
                                        label = "Merek Alat",
                                        selectedValue = merekAlatInput,
                                        options = merekAlatList,
                                        onValueChange = { merekAlatInput = it },
                                        testTag = "input_barang_merek",
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
                                                modifier = Modifier.fillMaxWidth().testTag("input_barang_stok")
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            DynamicDropdownField(
                                                label = "Satuan *",
                                                selectedValue = unitInput,
                                                options = units.map { it.name },
                                                onValueChange = { unitInput = it },
                                                testTag = "input_barang_satuan"
                                            )
                                        }
                                    }

                                    DynamicDropdownField(
                                        label = "Kondisi *",
                                        selectedValue = kondisiInput,
                                        options = kondisiList,
                                        onValueChange = { kondisiInput = it },
                                        testTag = "input_barang_kondisi"
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
                                                testTag = "input_barang_ruang",
                                                onQuickAddClick = { showQuickAddType = "Ruang" }
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            DynamicDropdownField(
                                                label = "Sumber Dana (Opsional)",
                                                selectedValue = sumberDanaInput,
                                                options = listOf("Belum Diketahui / Kosongkan") + sumberDanaList,
                                                onValueChange = { sumberDanaInput = it },
                                                testTag = "input_barang_sumber_dana",
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
                                        modifier = Modifier.fillMaxWidth().testTag("input_barang_keterangan")
                                    )
                                }
                            }

                            // Section 5: Opsi Peminjaman Siswa (Posisi Paling Bawah)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color.White
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isBorrowableInput = !isBorrowableInput }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Barang ini boleh dipinjam oleh siswa",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Nonaktifkan jika barang tidak untuk dipinjamkan",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = isBorrowableInput,
                                        onCheckedChange = { isBorrowableInput = it },
                                        modifier = Modifier.testTag("switch_is_borrowable")
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val stock = initialStockInput.toIntOrNull()
                                if (nameInput.isBlank()) {
                                    Toast.makeText(context, "Nama barang tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (stock == null || stock < 1) {
                                    Toast.makeText(context, "Stok awal minimal harus diisi angka 1!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                viewModel.registerNewItem(
                                    name = nameInput.trim(),
                                    serialNumber = serialNumberInput.trim(),
                                    stokAwal = stock,
                                    kategori = categoryInput,
                                    satuan = unitInput,
                                    merekAlat = merekAlatInput,
                                    ruang = ruangInput,
                                    sumberDana = if (sumberDanaInput == "Belum Diketahui / Kosongkan" || sumberDanaInput.isBlank()) null else sumberDanaInput,
                                    kondisi = kondisiInput,
                                    keterangan = keteranganInput,
                                    isBorrowable = isBorrowableInput,
                                    useAutoId = useAutoIdInput,
                                    customId = customIdInput,
                                    onSuccess = {
                                        Toast.makeText(context, "Alat baru berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                                        showAddDialog = false
                                        serialNumberInput = ""
                                        isBorrowableInput = false // Reset on success
                                        useAutoIdInput = true
                                        customIdInput = ""
                                    },
                                    onError = { err ->
                                        if (err.startsWith("DUPLICATE_ID:")) {
                                            val dupCode = err.substringAfter("DUPLICATE_ID:")
                                            duplicateAlertMessage = "Oops! 😊 Sepertinya Kode Barang '$dupCode' sudah terdaftar di sistem kita. Mohon gunakan kode yang lain atau aktifkan kembali mode ID Otomatis ya! ✨"
                                        } else {
                                            Toast.makeText(context, "Gagal: $err", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                )
                            },
                            enabled = !isAddStockInvalid && nameInput.isNotBlank(),
                            modifier = Modifier.testTag("dialog_btn_simpan_barang")
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
                                    "Merek" -> merekAlatList.any { it.equals(value, ignoreCase = true) }
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
                                        val updated = (viewModel.merekAlat.value + value).distinct()
                                        viewModel.updateMerekAlat(updated)
                                        merekAlatInput = value
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

            // Dialog Edit Barang
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
                            Text("Ubah Data Alat", fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
                                            text = "Kode / ID Alat: ${selectedItemForEdit!!.idBarang}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        )
                                    }

                                    LunarisTextField(
                                        value = editNameInput,
                                        onValueChange = { editNameInput = it },
                                        label = { Text("Nama Alat *") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText,
                                            unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else Color(0xFFD8B4FE),
                                            focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                            unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                                        ),
                                        modifier = Modifier.fillMaxWidth().testTag("edit_barang_nama")
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
                                                    contentDescription = "Pindai SN Alat",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().testTag("edit_barang_sn")
                                    )

                                    DynamicDropdownField(
                                        label = "Kategori *",
                                        selectedValue = editCategoryInput,
                                        options = categories.map { it.name },
                                        onValueChange = { editCategoryInput = it },
                                        testTag = "edit_barang_kategori",
                                        onQuickAddClick = { showQuickAddType = "Kategori" }
                                    )
                                }
                            }

                            // Section 2: Merek Alat
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
                                            text = "Merek Alat",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    DynamicDropdownField(
                                        label = "Merek Alat",
                                        selectedValue = editMerekAlatInput,
                                        options = merekAlatList,
                                        onValueChange = { editMerekAlatInput = it },
                                        testTag = "edit_barang_merek",
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
                                                modifier = Modifier.fillMaxWidth().testTag("edit_barang_stok")
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            DynamicDropdownField(
                                                label = "Satuan *",
                                                selectedValue = editUnitInput,
                                                options = units.map { it.name },
                                                onValueChange = { editUnitInput = it },
                                                testTag = "edit_barang_satuan"
                                            )
                                        }
                                    }

                                    DynamicDropdownField(
                                        label = "Kondisi *",
                                        selectedValue = editKondisiInput,
                                        options = kondisiList,
                                        onValueChange = { editKondisiInput = it },
                                        testTag = "edit_barang_kondisi"
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
                                                testTag = "edit_barang_ruang",
                                                onQuickAddClick = { showQuickAddType = "Ruang" }
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            DynamicDropdownField(
                                                label = "Sumber Dana (Opsional)",
                                                selectedValue = editSumberDanaInput,
                                                options = listOf("Belum Diketahui / Kosongkan") + sumberDanaList,
                                                onValueChange = { editSumberDanaInput = it },
                                                testTag = "edit_barang_sumber_dana",
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
                                        modifier = Modifier.fillMaxWidth().testTag("edit_barang_keterangan")
                                    )
                                }
                            }

                            // Section 5: Opsi Peminjaman Siswa (Posisi Paling Bawah)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color.White
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { editIsBorrowableInput = !editIsBorrowableInput }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Barang ini boleh dipinjam oleh siswa",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Nonaktifkan jika barang tidak untuk dipinjamkan",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = editIsBorrowableInput,
                                        onCheckedChange = { editIsBorrowableInput = it },
                                        modifier = Modifier.testTag("edit_switch_is_borrowable")
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val stock = editStockInput.toIntOrNull()
                                if (editNameInput.isBlank()) {
                                    Toast.makeText(context, "Nama barang tidak boleh kosong!", Toast.LENGTH_SHORT).show()
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
                                    merekAlat = editMerekAlatInput,
                                    ruang = editRuangInput,
                                    sumberDana = if (editSumberDanaInput == "Belum Diketahui / Kosongkan" || editSumberDanaInput.isBlank()) null else editSumberDanaInput,
                                    kondisi = editKondisiInput,
                                    keterangan = editKeteranganInput,
                                    isBorrowable = editIsBorrowableInput,
                                    onSuccess = {
                                        Toast.makeText(context, "Data alat berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                                        showEditDialog = false
                                    },
                                    onError = { err ->
                                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            enabled = !isEditStockInvalid && editNameInput.isNotBlank(),
                            modifier = Modifier.testTag("dialog_btn_update_barang")
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

            // Dialog Konfirmasi Hapus Barang (Berproteksi)
            if (showDeleteConfirmDialog && selectedItemForDelete != null) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmDialog = false },
                    title = { Text("Hapus Alat", fontWeight = FontWeight.Bold) },
                    text = {
                        Text("Apakah Anda yakin ingin menghapus alat '${selectedItemForDelete!!.namaBarang}'? Tindakan ini tidak dapat dibatalkan.")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.deleteItem(
                                    idBarang = selectedItemForDelete!!.idBarang,
                                    onSuccess = {
                                        Toast.makeText(context, "Alat berhasil dihapus!", Toast.LENGTH_SHORT).show()
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

            // Dialog Perbaiki Barang Rusak
            if (showRepairDialog && selectedItemForRepair != null) {
                AlertDialog(
                    onDismissRequest = { showRepairDialog = false },
                    title = { Text("Perbaiki Alat Rusak", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Barang: ${selectedItemForRepair!!.namaBarang}",
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Stok Rusak Saat Ini: ${selectedItemForRepair!!.stokRusak} ${selectedItemForRepair!!.satuan.ifEmpty { "Pcs" }}",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 13.sp
                            )

                            val isRepairQtyInvalid = repairQtyInput.trim().toIntOrNull() == null || 
                                    (repairQtyInput.trim().toIntOrNull() ?: 0) <= 0 ||
                                    (repairQtyInput.trim().toIntOrNull() ?: 0) > selectedItemForRepair!!.stokRusak

                            LunarisTextField(
                                value = repairQtyInput,
                                onValueChange = { repairQtyInput = it },
                                label = { Text("Jumlah Diperbaiki") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                isError = isRepairQtyInvalid,
                                supportingText = {
                                    if (isRepairQtyInvalid) {
                                        Text("Masukkan jumlah valid (1 s/d ${selectedItemForRepair!!.stokRusak})", color = MaterialTheme.colorScheme.error)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("repair_barang_qty")
                            )
                        }
                    },
                    confirmButton = {
                        val isQtyValid = repairQtyInput.trim().toIntOrNull() != null && 
                                (repairQtyInput.trim().toIntOrNull() ?: 0) > 0 &&
                                (repairQtyInput.trim().toIntOrNull() ?: 0) <= selectedItemForRepair!!.stokRusak

                        Button(
                            onClick = {
                                val qtyToRepair = repairQtyInput.trim().toIntOrNull() ?: 0
                                viewModel.repairStokRusak(selectedItemForRepair!!.idBarang, qtyToRepair) {
                                    Toast.makeText(context, "Stok rusak berhasil diperbaiki!", Toast.LENGTH_SHORT).show()
                                    showRepairDialog = false
                                }
                            },
                            enabled = isQtyValid,
                            modifier = Modifier.testTag("dialog_btn_repair_barang")
                        ) {
                            Text("Perbaiki")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRepairDialog = false }) {
                            Text("Batal")
                        }
                    }
                )
            }

            // Detail Barang Pop-up Dialog
            selectedItemForDetail?.let { detailItem ->
                val inMaintenance = detailItem.kondisi.contains("Servis", ignoreCase = true) ||
                        detailItem.kondisi.contains("Pemeliharaan", ignoreCase = true) ||
                        detailItem.kondisi.contains("Perbaikan", ignoreCase = true) ||
                        detailItem.stokRusak > 0

                AlertDialog(
                    onDismissRequest = { selectedItemForDetail = null },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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
                            // Maintenance Status Banner
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (inMaintenance) Color(0xFFFEF3C7) else Color(0xFFDCFCE7),
                                border = BorderStroke(1.dp, if (inMaintenance) Color(0xFFF59E0B) else Color(0xFF10B981))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (inMaintenance) Icons.Default.Build else Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = if (inMaintenance) Color(0xFFB45309) else Color(0xFF15803D),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = if (inMaintenance) "Dalam Pemeliharaan" else "Kondisi Normal",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (inMaintenance) Color(0xFFB45309) else Color(0xFF15803D)
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = if (inMaintenance) {
                                            if (detailItem.stokTersedia == 0) "0 unit normal tersedia, ${detailItem.stokAwal} unit dalam perawatan"
                                            else "${detailItem.stokTersedia} unit normal tersedia, ${detailItem.stokRusak} unit dalam perawatan"
                                        } else {
                                            "${detailItem.stokTersedia} unit normal tersedia"
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (inMaintenance) Color(0xFFB45309) else Color(0xFF15803D)
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            // Detail Info
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Informasi Detail Barang:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                if (detailItem.ruang.isNotEmpty()) Text("• Ruang / Lokasi: ${detailItem.ruang}", fontSize = 12.sp)
                                if (detailItem.merekAlat.isNotEmpty()) Text("• Merek: ${detailItem.merekAlat}", fontSize = 12.sp)
                                Text("• Total Stok Awal: ${detailItem.stokAwal} ${detailItem.satuan}", fontSize = 12.sp)
                                Text("• Stok Normal Tersedia: ${detailItem.stokTersedia} ${detailItem.satuan}", fontSize = 12.sp)
                                if (detailItem.stokRusak > 0) Text("• Stok Servis / Rusak: ${detailItem.stokRusak} ${detailItem.satuan}", fontSize = 12.sp)
                                if (!detailItem.sumberDana.isNullOrEmpty()) Text("• Sumber Dana: ${detailItem.sumberDana}", fontSize = 12.sp)
                                if (detailItem.keterangan.isNotEmpty()) Text("• Keterangan: ${detailItem.keterangan}", fontSize = 12.sp)
                                Text("• Status Pinjam: ${if (detailItem.isBorrowable) "Dapat Dipinjam" else "Tidak Dapat Dipinjam"}", fontSize = 12.sp)
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            // Riwayat Jejak Penggunaan / Pemeliharaan (Audit Trail Read-Only)
                            Text("Riwayat Jejak Audit & Pemeliharaan (Read-Only):", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
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
                                        Text("• Status Inventaris Alat:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                                        Text(if (inMaintenance) "Dalam Perawatan" else "Siap Operasional", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (inMaintenance) Color(0xFFB45309) else Color(0xFF16A34A))
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("• Verifikasi Fisik Sistem:", fontSize = 11.sp, color = Color(0xFF64748B))
                                        Text("${detailItem.stokTersedia} / ${detailItem.stokAwal} ${detailItem.satuan}", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1E293B))
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("• Catatan Log Terakhir:", fontSize = 11.sp, color = Color(0xFF64748B))
                                        Text("Pemeriksaan Rutin Sesuai Prosedur", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1E293B))
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
                            if (onNavigateToPemeliharaan != null) {
                                Button(
                                    onClick = {
                                        val itemToOper = detailItem
                                        selectedItemForDetail = null
                                        onNavigateToPemeliharaan(itemToOper.idBarang)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Oper ke Pemeliharaan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                    title = "Filter Data Alat",
                    onDismissRequest = {
                        tempSelectedCategory = appliedCategory
                        tempSelectedRoom = appliedRoom
                        tempSelectedCondition = appliedCondition
                        tempSelectedMerekAlat = appliedMerekAlat
                        showFilterDialog = false
                    },
                    leftColumnGroups = listOf(
                        FilterGroup(
                            title = "Kondisi Barang",
                            options = conditionOptions,
                            selectedOption = tempSelectedCondition,
                            onOptionSelected = { tempSelectedCondition = it }
                        ),
                        FilterGroup(
                            title = "Merek / Brand",
                            options = listOf("Semua Merek Alat") + merekAlatList,
                            selectedOption = tempSelectedMerekAlat,
                            onOptionSelected = { tempSelectedMerekAlat = it }
                        )
                    ),
                    rightColumnGroups = listOf(
                        FilterGroup(
                            title = "Ruang / Lokasi",
                            options = roomOptions,
                            selectedOption = tempSelectedRoom,
                            onOptionSelected = { tempSelectedRoom = it }
                        ),
                        FilterGroup(
                            title = "Kategori Alat",
                            options = categoriesOptions,
                            selectedOption = tempSelectedCategory,
                            onOptionSelected = { tempSelectedCategory = it }
                        )
                    ),
                    onReset = {
                        tempSelectedCategory = "Semua Kategori"
                        tempSelectedRoom = "Semua Ruang"
                        tempSelectedCondition = "Semua Kondisi"
                        tempSelectedMerekAlat = "Semua Merek Alat"
                    },
                    onApply = {
                        appliedCategory = tempSelectedCategory
                        appliedRoom = tempSelectedRoom
                        appliedCondition = tempSelectedCondition
                        appliedMerekAlat = tempSelectedMerekAlat
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
