package com.example.ui.screens
import com.example.ui.components.LunarisCard
import com.example.ui.components.LunarisTextField

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import java.io.BufferedReader
import java.io.InputStreamReader
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterDataScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.forceRefreshState()
    }
    val tabTitles = listOf("Merek Alat", "Merek Bahan", "Kategori", "Satuan", "Ruang", "Sumber Dana", "Kondisi", "Jabatan", "Tipe RAM", "Kapasitas RAM", "Storage", "Jenis PC", "Peripheral", "Guru Mapel", "Staf")
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

    val appBarContentColor = if (isDark) {
        MaterialTheme.colorScheme.onSurface
    } else {
        DeepPurpleText
    }
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
                                    text = "Master Data",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = androidx.compose.ui.graphics.Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Kelola data dan referensi",
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
                        .padding(vertical = 6.dp)
                ) {
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        edgePadding = 8.dp,
                        indicator = {},
                        divider = {},
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        tabTitles.forEachIndexed { index, title ->
                            val isSelected = selectedTab == index
                            Tab(
                                selected = isSelected,
                                onClick = { selectedTab = index },
                                text = {
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 2.dp, vertical = 1.dp)
                                            .background(
                                                color = if (isSelected) {
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                } else {
                                                    Color.Transparent
                                                },
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .padding(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = title,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 13.sp,
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                            }
                                        )
                                    }
                                },
                                modifier = Modifier.testTag("tab_$index")
                            )
                        }
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
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    0 -> {
                        val items by viewModel.merekAlat.collectAsState()
                        KelolaSimpleListTab(
                            menuName = "Merek Alat",
                            items = items,
                            onSave = { viewModel.updateMerekAlat(it) },
                            testTagPrefix = "merek_alat"
                        )
                    }
                    1 -> {
                        val items by viewModel.merekBahan.collectAsState()
                        KelolaSimpleListTab(
                            menuName = "Merek Bahan",
                            items = items,
                            onSave = { viewModel.updateMerekBahan(it) },
                            testTagPrefix = "merek_bahan"
                        )
                    }
                    2 -> KelolaKategoriTab(viewModel)
                    3 -> KelolaSatuanTab(viewModel)
                    4 -> {
                        val items by viewModel.ruang.collectAsState()
                        KelolaSimpleListTab(
                            menuName = "Ruang",
                            items = items,
                            onSave = { viewModel.updateRuang(it) },
                            testTagPrefix = "ruang"
                        )
                    }
                    5 -> {
                        val items by viewModel.sumberDana.collectAsState()
                        KelolaSimpleListTab(
                            menuName = "Sumber Dana",
                            items = items,
                            onSave = { viewModel.updateSumberDana(it) },
                            testTagPrefix = "sumber_dana"
                        )
                    }
                    6 -> {
                        val items by viewModel.kondisi.collectAsState()
                        KelolaSimpleListTab(
                            menuName = "Kondisi",
                            items = items,
                            onSave = { viewModel.updateKondisi(it) },
                            testTagPrefix = "kondisi"
                        )
                    }
                    7 -> {
                        val items by viewModel.jabatanList.collectAsState()
                        KelolaSimpleListTab(
                            menuName = "Jabatan",
                            items = items,
                            onSave = { viewModel.updateJabatan(it) },
                            testTagPrefix = "jabatan"
                        )
                    }
                    8 -> {
                        val items by viewModel.tipeRam.collectAsState()
                        KelolaSimpleListTab(
                            menuName = "Tipe RAM",
                            items = items,
                            onSave = { viewModel.updateTipeRam(it) },
                            testTagPrefix = "tipe_ram"
                        )
                    }
                    9 -> {
                        val items by viewModel.kapasitasRam.collectAsState()
                        KelolaSimpleListTab(
                            menuName = "Kapasitas RAM",
                            items = items,
                            onSave = { viewModel.updateKapasitasRam(it) },
                            testTagPrefix = "kapasitas_ram"
                        )
                    }
                    10 -> {
                        val items by viewModel.storage.collectAsState()
                        KelolaSimpleListTab(
                            menuName = "Storage",
                            items = items,
                            onSave = { viewModel.updateStorage(it) },
                            testTagPrefix = "storage"
                        )
                    }
                    11 -> {
                        val items by viewModel.jenisPc.collectAsState()
                        KelolaSimpleListTab(
                            menuName = "Jenis PC",
                            items = items,
                            onSave = { viewModel.updateJenisPc(it) },
                            testTagPrefix = "jenis_pc"
                        )
                    }
                    12 -> {
                        val items by viewModel.peripheral.collectAsState()
                        KelolaSimpleListTab(
                            menuName = "Peripheral",
                            items = items,
                            onSave = { viewModel.updatePeripheral(it) },
                            testTagPrefix = "peripheral"
                        )
                    }
                    13 -> KelolaGuruMapelTab(viewModel)
                    14 -> KelolaStafTab(viewModel)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KelolaBarangTab(viewModel: InventoryViewModel) {
    val isDark = false
    val context = LocalContext.current
    val items by viewModel.itemsWithStock.collectAsState()
    val categories by viewModel.allCategories.collectAsState()
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
                            onSuccess = { added, updated ->
                                Toast.makeText(context, "Berhasil impor data! Baru: $added, Update: $updated", Toast.LENGTH_LONG).show()
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

    // Add Form State
    var nameInput by remember { mutableStateOf("") }
    var categoryInput by remember { mutableStateOf("") }
    var unitInput by remember { mutableStateOf("") }
    var initialStockInput by remember { mutableStateOf("0") }

    // Edit Form State
    var editNameInput by remember { mutableStateOf("") }
    var editCategoryInput by remember { mutableStateOf("") }
    var editUnitInput by remember { mutableStateOf("") }
    var editStockInput by remember { mutableStateOf("0") }

    var repairQtyInput by remember { mutableStateOf("") }

    // Auto-generate ID estimate preview
    val estimatedNextId = remember(items) {
        var maxIdNum = 0
        items.forEach { item ->
            if (item.idBarang.startsWith("BRG-")) {
                val numPart = item.idBarang.substringAfter("BRG-").toIntOrNull()
                if (numPart != null && numPart > maxIdNum) {
                    maxIdNum = numPart
                }
            }
        }
        "BRG-${String.format(Locale.US, "%03d", maxIdNum + 1)}"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val isAddStockInvalid = initialStockInput.trim().toIntOrNull() == null || (initialStockInput.trim().toIntOrNull() ?: 0) <= 0
        val isEditStockInvalid = editStockInput.trim().toIntOrNull() == null || (editStockInput.trim().toIntOrNull() ?: 0) <= 0

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Button(
                onClick = {
                    categoryInput = categories.firstOrNull()?.name ?: ""
                    unitInput = units.firstOrNull()?.name ?: ""
                    nameInput = ""
                    initialStockInput = ""
                    showAddDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(bottom = 0.dp)
                    .testTag("btn_tambah_barang_baru")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Tambah Baru", fontWeight = FontWeight.Bold, maxLines = 1)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        csvLauncher.launch("*/*")
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GlassWhite,
                        contentColor = DeepPurpleText
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_impor_csv")
                ) {
                    Icon(
                        imageVector = Icons.Default.Upload,
                        contentDescription = "Impor CSV",
                        tint = DeepPurpleText
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Impor",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        fontSize = 11.sp,
                        color = DeepPurpleText
                    )
                }

                Button(
                    onClick = {
                        if (items.isEmpty()) {
                            Toast.makeText(context, "Tidak ada data untuk diekspor!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val filename = "Master_Data_General_Lunaris_${System.currentTimeMillis()}.xlsx"
                        val headers = listOf(
                            "nama_barang", "kategori", "merek", "ruang", "satuan",
                            "stok_awal", "stok_tersedia", "stok_rusak", "sumber_dana", "kondisi", "keterangan"
                        )
                        val rows = items.map { item ->
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
                            title = "Master Data General Lunaris",
                            headers = headers,
                            rows = rows
                        )
                        saveFileToDownloads(
                            context = context,
                            filename = filename,
                            mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            bytes = bytes
                        ) {
                            Toast.makeText(context, "Data General berhasil diekspor ke format Excel (.xlsx)!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GlassWhite,
                        contentColor = DeepPurpleText
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_ekspor_csv_general")
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Ekspor Excel",
                        tint = DeepPurpleText
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Ekspor",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        fontSize = 11.sp,
                        color = DeepPurpleText
                    )
                }

                Button(
                    onClick = {
                        val templateFilename = "Template_Impor_General_Lunaris.xlsx"
                        val templateMimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        val headers = listOf(
                            "nama_barang", "kategori", "merek", "ruang", "satuan",
                            "stok_awal", "sumber_dana", "kondisi", "keterangan"
                        )
                        val templateRows = listOf(
                            listOf("Laptop ASUS Core i5", "Elektronik", "ASUS", "Lab Komputer 1", "Unit", "15", "BOS Reguler", "Sangat Baik", "Laptop untuk ujian"),
                            listOf("Kertas HVS A4 80g PaperOne", "Logistik", "PaperOne", "Ruang TU", "Rim", "100", "BOS Reguler", "Normal (Terawat)", "Kertas print laporan")
                        )
                        val bytes = generateExcelBytes(
                            title = "Template Impor Data General Lunaris",
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GlassWhite,
                        contentColor = DeepPurpleText
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_unduh_template_csv")
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "Unduh Template",
                        tint = DeepPurpleText
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Template",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        fontSize = 11.sp,
                        color = DeepPurpleText
                    )
                }
            }

            if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inventory,
                            contentDescription = "Kosong",
                            tint = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Belum ada barang. Silakan tambah barang baru.", color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items) { item ->
                        LunarisCard(
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = GlassWhiteMore),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.namaBarang,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "ID: ${item.idBarang}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        // Repair Button
                                        if (item.stokRusak > 0) {
                                            IconButton(
                                                onClick = {
                                                    selectedItemForRepair = item
                                                    repairQtyInput = item.stokRusak.toString()
                                                    showRepairDialog = true
                                                },
                                                modifier = Modifier.testTag("repair_barang_${item.idBarang}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Build,
                                                    contentDescription = "Perbaiki",
                                                    tint = Color(0xFFD97706)
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
                                                showEditDialog = true
                                            },
                                            modifier = Modifier.testTag("edit_barang_${item.idBarang}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit",
                                                tint = MaterialTheme.colorScheme.secondary
                                            )
                                        }

                                        // Delete Button
                                        IconButton(
                                            onClick = {
                                                selectedItemForDelete = item
                                                showDeleteConfirmDialog = true
                                            },
                                            modifier = Modifier.testTag("delete_barang_${item.idBarang}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Hapus",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Divider(color = Color(0xFFF1F5F9))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Info Kategori & Satuan
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (item.kategori.isNotEmpty()) {
                                            SuggestionChip(
                                                onClick = {},
                                                label = { Text(item.kategori, fontSize = 11.sp) }
                                            )
                                        }
                                        if (item.satuan.isNotEmpty()) {
                                            SuggestionChip(
                                                onClick = {},
                                                label = { Text(item.satuan, fontSize = 11.sp) }
                                            )
                                        }
                                    }

                                    // Stock Badges
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        BadgeWithLabel(
                                            label = "Stok Fisik",
                                            value = "${item.stokAwal} ${item.satuan.ifEmpty { "Pcs" }}",
                                            bgColor = Color(0xFFEFF6FF),
                                            textColor = Color(0xFF1E40AF)
                                        )
                                        BadgeWithLabel(
                                            label = "Tersedia",
                                            value = "${item.stokTersedia} ${item.satuan.ifEmpty { "Pcs" }}",
                                            bgColor = if (item.stokTersedia > 0) Color(0xFFECFDF5) else Color(0xFFFEF2F2),
                                            textColor = if (item.stokTersedia > 0) Color(0xFF065F46) else Color(0xFF991B1B)
                                        )
                                        if (item.stokRusak > 0) {
                                            BadgeWithLabel(
                                                label = "Rusak",
                                                value = "${item.stokRusak} ${item.satuan.ifEmpty { "Pcs" }}",
                                                bgColor = Color(0xFFFEF2F2),
                                                textColor = Color(0xFFB91C1C)
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

        // Dialog Tambah Barang Baru
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Tambah Barang Baru", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Estimasi ID Otomatis: $estimatedNextId",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        LunarisTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Nama Barang") },
                            placeholder = { Text("Contoh: Laptop Asus") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_barang_nama")
                        )

                        // Category Dropdown
                        DynamicDropdownField(
                            label = "Kategori",
                            selectedValue = categoryInput,
                            options = categories.map { it.name },
                            onValueChange = { categoryInput = it },
                            testTag = "input_barang_kategori"
                        )

                        // Unit Dropdown
                        DynamicDropdownField(
                            label = "Satuan",
                            selectedValue = unitInput,
                            options = units.map { it.name },
                            onValueChange = { unitInput = it },
                            testTag = "input_barang_satuan"
                        )

                        LunarisTextField(
                            value = initialStockInput,
                            onValueChange = { initialStockInput = it },
                            label = { Text("Stok Awal Fisik") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            isError = isAddStockInvalid,
                            supportingText = {
                                if (isAddStockInvalid) {
                                    Text("Stok awal harus berupa angka lebih besar dari 0!", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("input_barang_stok")
                        )
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
                            if (stock == null || stock <= 0) {
                                Toast.makeText(context, "Stok awal harus lebih besar dari 0!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            viewModel.registerNewItem(
                                name = nameInput.trim(),
                                stokAwal = stock,
                                kategori = categoryInput,
                                satuan = unitInput,
                                onSuccess = {
                                    Toast.makeText(context, "Barang baru berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                                    showAddDialog = false
                                },
                                onError = { err ->
                                    Toast.makeText(context, "Gagal: $err", Toast.LENGTH_LONG).show()
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

        // Dialog Edit Barang
        if (showEditDialog && selectedItemForEdit != null) {
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                title = { Text("Ubah Data Barang", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "ID Barang: ${selectedItemForEdit!!.idBarang}",
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )

                        LunarisTextField(
                            value = editNameInput,
                            onValueChange = { editNameInput = it },
                            label = { Text("Nama Barang") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("edit_barang_nama")
                        )

                        // Category Dropdown
                        DynamicDropdownField(
                            label = "Kategori",
                            selectedValue = editCategoryInput,
                            options = categories.map { it.name },
                            onValueChange = { editCategoryInput = it },
                            testTag = "edit_barang_kategori"
                        )

                        // Unit Dropdown
                        DynamicDropdownField(
                            label = "Satuan",
                            selectedValue = editUnitInput,
                            options = units.map { it.name },
                            onValueChange = { editUnitInput = it },
                            testTag = "edit_barang_satuan"
                        )

                        LunarisTextField(
                            value = editStockInput,
                            onValueChange = { editStockInput = it },
                            label = { Text("Stok Awal Fisik") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            isError = isEditStockInvalid,
                            supportingText = {
                                if (isEditStockInvalid) {
                                    Text("Stok awal harus berupa angka lebih besar dari 0!", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("edit_barang_stok")
                        )
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
                            if (stock == null || stock <= 0) {
                                Toast.makeText(context, "Stok awal harus lebih besar dari 0!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            viewModel.updateItemDetails(
                                idBarang = selectedItemForEdit!!.idBarang,
                                namaBarang = editNameInput.trim(),
                                kategori = editCategoryInput,
                                satuan = editUnitInput,
                                stokAwal = stock,
                                isBorrowable = selectedItemForEdit!!.isBorrowable,
                                onSuccess = {
                                    Toast.makeText(context, "Data barang berhasil diperbarui!", Toast.LENGTH_SHORT).show()
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

        // Dialog Konfirmasi Hapus Barang (Berproteksi)
        if (showDeleteConfirmDialog && selectedItemForDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text("Hapus Barang", fontWeight = FontWeight.Bold) },
                text = {
                    Text("Apakah Anda yakin ingin menghapus barang '${selectedItemForDelete!!.namaBarang}'? Tindakan ini tidak dapat dibatalkan.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteItem(
                                idBarang = selectedItemForDelete!!.idBarang,
                                onSuccess = {
                                    Toast.makeText(context, "Barang berhasil dihapus!", Toast.LENGTH_SHORT).show()
                                    showDeleteConfirmDialog = false
                                },
                                onError = { err ->
                                    // Protected block prevents deletion if active loans exist
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
                title = { Text("Perbaiki Barang Rusak", fontWeight = FontWeight.Bold) },
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
    }
}

@Composable
fun KelolaKategoriTab(viewModel: InventoryViewModel) {
    val context = LocalContext.current
    val categories by viewModel.allCategories.collectAsState()
    val isDark = false

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }

    var selectedCategoryForEdit by remember { mutableStateOf<CategoryEntity?>(null) }
    var selectedCategoryForDelete by remember { mutableStateOf<CategoryEntity?>(null) }
    var selectedCategoryIds by remember { mutableStateOf<Set<Int>>(emptySet()) }

    var addInput by remember { mutableStateOf("") }
    var editInput by remember { mutableStateOf("") }

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val rows = readExcelOrCsvInputStream(inputStream)

                    if (rows.isNotEmpty()) {
                        var addedCount = 0
                        val dataRows = if (rows.size >= 3) {
                            val r1 = rows.getOrNull(0)?.firstOrNull()?.trim() ?: ""
                            val r3 = rows.getOrNull(2)?.firstOrNull()?.trim() ?: ""
                            if (r1.contains("Template", ignoreCase = true) ||
                                r1.contains("Impor", ignoreCase = true) ||
                                r3.contains("Kategori", ignoreCase = true) ||
                                r3.contains("Nama", ignoreCase = true)) {
                                rows.drop(3)
                            } else if (r1.contains("Kategori", ignoreCase = true) || r1.contains("Nama", ignoreCase = true)) {
                                rows.drop(1)
                            } else {
                                rows
                            }
                        } else if (rows.firstOrNull()?.firstOrNull()?.trim()?.let {
                            it.contains("Kategori", ignoreCase = true) || it.contains("Nama", ignoreCase = true) || it.contains("Template", ignoreCase = true)
                        } == true) {
                            rows.drop(1)
                        } else {
                            rows
                        }

                        dataRows.forEach { cols ->
                            val valStr = cols.getOrNull(0)?.trim() ?: ""
                            if (valStr.isNotBlank()) {
                                if (!valStr.contains("Template Impor", ignoreCase = true) &&
                                    !valStr.contains("Template Data", ignoreCase = true) &&
                                    !valStr.equals("kategori", ignoreCase = true) &&
                                    !valStr.equals("nama", ignoreCase = true) &&
                                    !valStr.startsWith("nama_", ignoreCase = true) &&
                                    categories.none { it.name.trim().equals(valStr, ignoreCase = true) }) {
                                    viewModel.addCategory(valStr, {}, {})
                                    addedCount++
                                }
                            }
                        }
                        if (addedCount > 0) {
                            Toast.makeText(context, "Berhasil mengimpor $addedCount kategori baru!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Semua kategori dari file Excel/CSV sudah ada!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "File Excel/CSV kosong!", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal membaca file Excel/CSV: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 0.dp)) {
            // CSV/Excel Import/Export and Template Row
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
                        containerColor = GlassWhite,
                        contentColor = DeepPurpleText
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("btn_impor_csv_kategori")
                ) {
                    Icon(imageVector = Icons.Default.Upload, contentDescription = "Impor Excel", tint = DeepPurpleText, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Impor", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DeepPurpleText, maxLines = 1)
                }

                Button(
                    onClick = {
                        if (categories.isEmpty()) {
                            Toast.makeText(context, "Tidak ada data kategori untuk diekspor!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val headers = listOf("nama_kategori")
                        val rows = categories.map { listOf(it.name) }
                        val bytes = generateExcelBytes(
                            title = "Master Data Kategori Barang Lunaris",
                            headers = headers,
                            rows = rows
                        )
                        saveFileToDownloads(
                            context = context,
                            filename = "Master_Kategori_Lunaris_${System.currentTimeMillis()}.xlsx",
                            mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            bytes = bytes
                        ) {
                            Toast.makeText(context, "Data Kategori berhasil diekspor ke Excel (.xlsx)!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GlassWhite,
                        contentColor = DeepPurpleText
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("btn_ekspor_csv_kategori")
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = "Ekspor Excel", tint = DeepPurpleText, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ekspor", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DeepPurpleText, maxLines = 1)
                }

                Button(
                    onClick = {
                        val headers = listOf("nama_kategori")
                        val templateRows = listOf(
                            listOf("Perangkat Utama"),
                            listOf("Komponen Hardware"),
                            listOf("Kabel & Adaptor")
                        )
                        val bytes = generateExcelBytes(
                            title = "Template Impor Master Kategori Lunaris",
                            headers = headers,
                            rows = templateRows
                        )
                        saveFileToDownloads(
                            context = context,
                            filename = "Template_Master_Kategori_Lunaris.xlsx",
                            mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            bytes = bytes
                        ) {
                            Toast.makeText(context, "Template Excel (.xlsx) Kategori berhasil diunduh!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GlassWhite,
                        contentColor = DeepPurpleText
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("btn_unduh_template_kategori")
                ) {
                    Icon(imageVector = Icons.Default.Description, contentDescription = "Unduh Template", tint = DeepPurpleText, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Template", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DeepPurpleText, maxLines = 1)
                }
            }

            if (categories.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Data belum tersedia.", color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray)
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isAllSelected = categories.isNotEmpty() && selectedCategoryIds.size == categories.size
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            selectedCategoryIds = if (isAllSelected) emptySet<Int>() else categories.map { it.id }.toSet()
                        }
                    ) {
                        Checkbox(
                            checked = isAllSelected,
                            onCheckedChange = { checked ->
                                selectedCategoryIds = if (checked) categories.map { it.id }.toSet() else emptySet<Int>()
                            },
                            modifier = Modifier.testTag("checkbox_select_all_kategori")
                        )
                        Text(
                            text = if (isAllSelected) "Batal Pilih Semua" else "Pilih Semua (${categories.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (selectedCategoryIds.isNotEmpty()) {
                        Button(
                            onClick = { showBatchDeleteDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("btn_hapus_terpilih_kategori")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Hapus Terpilih (${selectedCategoryIds.size})", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 160.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(categories, key = { it.id }) { category ->
                        val isSelected = selectedCategoryIds.contains(category.id)
                        LunarisCard(
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Color(0xFF7C3AED) else if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f) else Color(0xFFCBD5E1)
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFFF3E8FF) else if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFFFFFF)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            selectedCategoryIds = if (checked) selectedCategoryIds + category.id else selectedCategoryIds - category.id
                                        },
                                        modifier = Modifier.testTag("checkbox_kategori_${category.id}")
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = category.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            selectedCategoryForEdit = category
                                            editInput = category.name
                                            showEditDialog = true
                                        },
                                        modifier = Modifier.testTag("edit_kategori_${category.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Ubah",
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            selectedCategoryForDelete = category
                                            showDeleteDialog = true
                                        },
                                        modifier = Modifier.testTag("delete_kategori_${category.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Hapus",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                addInput = ""
                showAddDialog = true
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 100.dp, end = 16.dp)
                .zIndex(5f)
                .testTag("btn_tambah_kategori")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Tambah Kategori Baru",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }

    // Add Category Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Tambah Kategori Baru", fontWeight = FontWeight.Bold) },
            text = {
                LunarisTextField(
                    value = addInput,
                    onValueChange = { addInput = it },
                    label = { Text("Nama Kategori") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("input_kategori_name")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addCategory(
                            name = addInput,
                            onSuccess = {
                                Toast.makeText(context, "Kategori berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                                showAddDialog = false
                            },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("dialog_btn_simpan_kategori")
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddDialog = false },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Batal")
                }
            }
        )
    }

    // Edit Category Dialog
    if (showEditDialog && selectedCategoryForEdit != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Ubah Nama Kategori", fontWeight = FontWeight.Bold) },
            text = {
                LunarisTextField(
                    value = editInput,
                    onValueChange = { editInput = it },
                    label = { Text("Nama Kategori") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("edit_kategori_name")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateCategory(
                            id = selectedCategoryForEdit!!.id,
                            name = editInput,
                            onSuccess = {
                                Toast.makeText(context, "Kategori berhasil diubah!", Toast.LENGTH_SHORT).show()
                                showEditDialog = false
                            },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("dialog_btn_update_kategori")
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditDialog = false },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Batal")
                }
            }
        )
    }

    // Delete Category Dialog
    if (showDeleteDialog && selectedCategoryForDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus Kategori", fontWeight = FontWeight.Bold) },
            text = {
                Text("Apakah Anda yakin ingin menghapus kategori '${selectedCategoryForDelete!!.name}' secara permanen? Fitur ini diperuntukkan khusus bagi koreksi data input yang keliru.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCategory(
                            id = selectedCategoryForDelete!!.id,
                            onSuccess = {
                                Toast.makeText(context, "Kategori berhasil dihapus!", Toast.LENGTH_SHORT).show()
                                showDeleteDialog = false
                            },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("dialog_btn_hapus_kategori")
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Batal")
                }
            }
        )
    }

    // Batch Delete Category Dialog
    if (showBatchDeleteDialog && selectedCategoryIds.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteDialog = false },
            title = { Text("Konfirmasi Hapus ${selectedCategoryIds.size} Kategori", fontWeight = FontWeight.Bold) },
            text = {
                Text("Apakah Anda yakin ingin menghapus ${selectedCategoryIds.size} kategori terpilih secara permanen? Fitur ini diperuntukkan khusus bagi koreksi data input yang keliru (bulk import).")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val idsToDelete = selectedCategoryIds.toList()
                        var deletedCount = 0
                        idsToDelete.forEach { id ->
                            viewModel.deleteCategory(
                                id = id,
                                onSuccess = { deletedCount++ },
                                onError = {}
                            )
                        }
                        Toast.makeText(context, "Berhasil menghapus $deletedCount kategori!", Toast.LENGTH_SHORT).show()
                        selectedCategoryIds = emptySet()
                        showBatchDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("dialog_btn_confirm_batch_delete_kategori")
                ) {
                    Text("Hapus Permanen")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBatchDeleteDialog = false },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun KelolaSatuanTab(viewModel: InventoryViewModel) {
    val context = LocalContext.current
    val units by viewModel.allUnits.collectAsState()
    val isDark = false

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }

    var selectedUnitForEdit by remember { mutableStateOf<UnitEntity?>(null) }
    var selectedUnitForDelete by remember { mutableStateOf<UnitEntity?>(null) }
    var selectedUnitIds by remember { mutableStateOf<Set<Int>>(emptySet()) }

    var addInput by remember { mutableStateOf("") }
    var editInput by remember { mutableStateOf("") }

    // Auto cleanup duplicate units on tab launch
    LaunchedEffect(Unit) {
        viewModel.cleanupDuplicateUnits()
    }

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val rows = readExcelOrCsvInputStream(inputStream)

                    if (rows.isNotEmpty()) {
                        var addedCount = 0
                        val processedInCsv = mutableSetOf<String>()
                        val dataRows = if (rows.size >= 3) {
                            val r1 = rows.getOrNull(0)?.firstOrNull()?.trim() ?: ""
                            val r3 = rows.getOrNull(2)?.firstOrNull()?.trim() ?: ""
                            if (r1.contains("Template", ignoreCase = true) ||
                                r1.contains("Impor", ignoreCase = true) ||
                                r3.contains("Satuan", ignoreCase = true) ||
                                r3.contains("Nama", ignoreCase = true)) {
                                rows.drop(3)
                            } else if (r1.contains("Satuan", ignoreCase = true) || r1.contains("Nama", ignoreCase = true)) {
                                rows.drop(1)
                            } else {
                                rows
                            }
                        } else if (rows.firstOrNull()?.firstOrNull()?.trim()?.let {
                            it.contains("Satuan", ignoreCase = true) || it.contains("Nama", ignoreCase = true) || it.contains("Template", ignoreCase = true)
                        } == true) {
                            rows.drop(1)
                        } else {
                            rows
                        }

                        dataRows.forEach { cols ->
                            val valStr = cols.getOrNull(0)?.trim() ?: ""
                            if (valStr.isNotBlank()) {
                                if (!valStr.contains("Template Impor", ignoreCase = true) &&
                                    !valStr.contains("Template Data", ignoreCase = true) &&
                                    !valStr.equals("satuan", ignoreCase = true) &&
                                    !valStr.equals("nama", ignoreCase = true) &&
                                    !valStr.startsWith("nama_", ignoreCase = true)) {
                                    val norm = valStr.lowercase()
                                    if (!processedInCsv.contains(norm) && units.none { it.name.trim().equals(valStr, ignoreCase = true) }) {
                                        processedInCsv.add(norm)
                                        viewModel.addUnit(valStr, {}, {})
                                        addedCount++
                                    }
                                }
                            }
                        }
                        if (addedCount > 0) {
                            Toast.makeText(context, "Berhasil mengimpor $addedCount satuan baru!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Semua satuan dari file Excel/CSV sudah ada!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "File Excel/CSV kosong!", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal membaca file Excel/CSV: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 0.dp)) {
            // Excel/CSV Import/Export and Template Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { csvLauncher.launch("*/*") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GlassWhite,
                        contentColor = DeepPurpleText
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("btn_impor_csv_satuan")
                ) {
                    Icon(imageVector = Icons.Default.Upload, contentDescription = "Impor Excel", tint = DeepPurpleText, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Impor", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = DeepPurpleText, maxLines = 1)
                }

                Button(
                    onClick = {
                        if (units.isEmpty()) {
                            Toast.makeText(context, "Tidak ada data satuan untuk diekspor!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val headers = listOf("nama_satuan")
                        val rows = units.map { listOf(it.name) }
                        val bytes = generateExcelBytes(
                            title = "Master Data Satuan Barang Lunaris",
                            headers = headers,
                            rows = rows
                        )
                        saveFileToDownloads(
                            context = context,
                            filename = "Master_Satuan_Lunaris_${System.currentTimeMillis()}.xlsx",
                            mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            bytes = bytes
                        ) {
                            Toast.makeText(context, "Data Satuan berhasil diekspor ke Excel (.xlsx)!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GlassWhite,
                        contentColor = DeepPurpleText
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("btn_ekspor_csv_satuan")
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = "Ekspor Excel", tint = DeepPurpleText, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Ekspor", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = DeepPurpleText, maxLines = 1)
                }

                Button(
                    onClick = {
                        val headers = listOf("nama_satuan")
                        val templateRows = listOf(
                            listOf("Unit"),
                            listOf("Pcs"),
                            listOf("Box"),
                            listOf("Roll"),
                            listOf("Set")
                        )
                        val bytes = generateExcelBytes(
                            title = "Template Impor Master Satuan Lunaris",
                            headers = headers,
                            rows = templateRows
                        )
                        saveFileToDownloads(
                            context = context,
                            filename = "Template_Master_Satuan_Lunaris.xlsx",
                            mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            bytes = bytes
                        ) {
                            Toast.makeText(context, "Template Excel (.xlsx) Satuan berhasil diunduh!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GlassWhite,
                        contentColor = DeepPurpleText
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("btn_unduh_template_satuan")
                ) {
                    Icon(imageVector = Icons.Default.Description, contentDescription = "Unduh Template", tint = DeepPurpleText, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Template", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = DeepPurpleText, maxLines = 1)
                }

                Button(
                    onClick = {
                        viewModel.cleanupDuplicateUnits(
                            onSuccess = { count ->
                                if (count > 0) {
                                    Toast.makeText(context, "Berhasil membersihkan $count data satuan duplikat!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Data satuan sudah bersih & tidak ada duplikat.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GlassWhite,
                        contentColor = DeepPurpleText
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    modifier = Modifier
                        .weight(1.1f)
                        .height(44.dp)
                        .testTag("btn_bersihkan_duplikat_satuan")
                ) {
                    Icon(imageVector = Icons.Default.CleaningServices, contentDescription = "Bersihkan Duplikat", tint = DeepPurpleText, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Bersihkan", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = DeepPurpleText, maxLines = 1)
                }
            }

            if (units.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Data belum tersedia.", color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray)
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isAllSelected = units.isNotEmpty() && selectedUnitIds.size == units.size
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            selectedUnitIds = if (isAllSelected) emptySet<Int>() else units.map { it.id }.toSet()
                        }
                    ) {
                        Checkbox(
                            checked = isAllSelected,
                            onCheckedChange = { checked ->
                                selectedUnitIds = if (checked) units.map { it.id }.toSet() else emptySet<Int>()
                            },
                            modifier = Modifier.testTag("checkbox_select_all_satuan")
                        )
                        Text(
                            text = if (isAllSelected) "Batal Pilih Semua" else "Pilih Semua (${units.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (selectedUnitIds.isNotEmpty()) {
                        Button(
                            onClick = { showBatchDeleteDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("btn_hapus_terpilih_satuan")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Hapus Terpilih (${selectedUnitIds.size})", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 160.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(units, key = { it.id }) { unit ->
                        val isSelected = selectedUnitIds.contains(unit.id)
                        LunarisCard(
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Color(0xFF7C3AED) else if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f) else Color(0xFFCBD5E1)
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFFF3E8FF) else if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFFFFFF)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            selectedUnitIds = if (checked) selectedUnitIds + unit.id else selectedUnitIds - unit.id
                                        },
                                        modifier = Modifier.testTag("checkbox_satuan_${unit.id}")
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = unit.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            selectedUnitForEdit = unit
                                            editInput = unit.name
                                            showEditDialog = true
                                        },
                                        modifier = Modifier.testTag("edit_satuan_${unit.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Ubah",
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            selectedUnitForDelete = unit
                                            showDeleteDialog = true
                                        },
                                        modifier = Modifier.testTag("delete_satuan_${unit.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Hapus",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                addInput = ""
                showAddDialog = true
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 100.dp, end = 16.dp)
                .zIndex(5f)
                .testTag("btn_tambah_satuan")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Tambah Satuan Baru",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }

    // Add Unit Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Tambah Satuan Baru", fontWeight = FontWeight.Bold) },
            text = {
                LunarisTextField(
                    value = addInput,
                    onValueChange = { addInput = it },
                    label = { Text("Nama Satuan (Contoh: Unit, Pcs)") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("input_satuan_name")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addUnit(
                            name = addInput,
                            onSuccess = {
                                Toast.makeText(context, "Satuan berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                                showAddDialog = false
                            },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("dialog_btn_simpan_satuan")
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddDialog = false },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Batal")
                }
            }
        )
    }

    // Edit Unit Dialog
    if (showEditDialog && selectedUnitForEdit != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Ubah Nama Satuan", fontWeight = FontWeight.Bold) },
            text = {
                LunarisTextField(
                    value = editInput,
                    onValueChange = { editInput = it },
                    label = { Text("Nama Satuan") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("edit_satuan_name")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateUnit(
                            id = selectedUnitForEdit!!.id,
                            name = editInput,
                            onSuccess = {
                                Toast.makeText(context, "Satuan berhasil diubah!", Toast.LENGTH_SHORT).show()
                                showEditDialog = false
                            },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("dialog_btn_update_satuan")
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditDialog = false },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Batal")
                }
            }
        )
    }

    // Delete Unit Dialog
    if (showDeleteDialog && selectedUnitForDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus Satuan", fontWeight = FontWeight.Bold) },
            text = {
                Text("Apakah Anda yakin ingin menghapus satuan '${selectedUnitForDelete!!.name}' secara permanen? Fitur ini diperuntukkan khusus bagi koreksi data input yang keliru.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteUnit(
                            id = selectedUnitForDelete!!.id,
                            onSuccess = {
                                Toast.makeText(context, "Satuan berhasil dihapus!", Toast.LENGTH_SHORT).show()
                                showDeleteDialog = false
                            },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("dialog_btn_hapus_satuan")
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Batal")
                }
            }
        )
    }

    // Batch Delete Unit Dialog
    if (showBatchDeleteDialog && selectedUnitIds.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteDialog = false },
            title = { Text("Konfirmasi Hapus ${selectedUnitIds.size} Satuan", fontWeight = FontWeight.Bold) },
            text = {
                Text("Apakah Anda yakin ingin menghapus ${selectedUnitIds.size} satuan terpilih secara permanen? Fitur ini diperuntukkan khusus bagi koreksi data input yang keliru (bulk import).")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val idsToDelete = selectedUnitIds.toList()
                        var deletedCount = 0
                        idsToDelete.forEach { id ->
                            viewModel.deleteUnit(
                                id = id,
                                onSuccess = { deletedCount++ },
                                onError = {}
                            )
                        }
                        Toast.makeText(context, "Berhasil menghapus $deletedCount satuan!", Toast.LENGTH_SHORT).show()
                        selectedUnitIds = emptySet()
                        showBatchDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("dialog_btn_confirm_batch_delete_satuan")
                ) {
                    Text("Hapus Permanen")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBatchDeleteDialog = false },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Batal")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicDropdownField(
    label: String,
    selectedValue: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    testTag: String = "",
    onQuickAddClick: (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val isDark = false

    Box(modifier = Modifier.fillMaxWidth()) {
        LunarisTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = "Pilih"
                    )
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF7C3AED),
                unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else Color(0xFFD8B4FE),
                focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .testTag(testTag)
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            if (options.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("Tidak ada pilihan") },
                    onClick = { expanded = false }
                )
            } else {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
            if (onQuickAddClick != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                DropdownMenuItem(
                    text = { Text("+ Tambah Baru", color = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF7C3AED), fontWeight = FontWeight.Bold) },
                    onClick = {
                        expanded = false
                        onQuickAddClick()
                    },
                    modifier = Modifier.testTag("dropdown_quick_add_${label.lowercase().replace(" ", "_")}")
                )
            }
        }
    }
}

@Composable
fun BadgeWithLabel(
    label: String,
    value: String,
    bgColor: Color,
    textColor: Color
) {
    val isDark = false
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray)
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .background(bgColor, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

@Composable
fun KelolaSimpleListTab(
    menuName: String,
    items: List<String>,
    onSave: (List<String>) -> Unit,
    placeholder: String = "Data belum tersedia.",
    testTagPrefix: String
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }

    var selectedIndexForEdit by remember { mutableStateOf(-1) }
    var selectedIndexForDelete by remember { mutableStateOf(-1) }
    var selectedItemIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }

    var addInput by remember { mutableStateOf("") }
    var editInput by remember { mutableStateOf("") }

    val isDark = false

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val rows = readExcelOrCsvInputStream(inputStream)

                    if (rows.isNotEmpty()) {
                        val parsedItems = mutableListOf<String>()
                        val dataRows = if (rows.size >= 3) {
                            val r1 = rows.getOrNull(0)?.firstOrNull()?.trim() ?: ""
                            val r3 = rows.getOrNull(2)?.firstOrNull()?.trim() ?: ""
                            if (r1.contains("Template", ignoreCase = true) ||
                                r1.contains("Impor", ignoreCase = true) ||
                                r3.contains("Nama", ignoreCase = true) ||
                                r3.contains(menuName, ignoreCase = true)) {
                                rows.drop(3)
                            } else if (r1.contains("Nama", ignoreCase = true) || r1.contains(menuName, ignoreCase = true)) {
                                rows.drop(1)
                            } else {
                                rows
                            }
                        } else if (rows.firstOrNull()?.firstOrNull()?.trim()?.let {
                            it.contains("Nama", ignoreCase = true) || it.contains(menuName, ignoreCase = true) || it.contains("Template", ignoreCase = true)
                        } == true) {
                            rows.drop(1)
                        } else {
                            rows
                        }

                        dataRows.forEach { cols ->
                            val valStr = cols.getOrNull(0)?.trim() ?: ""
                            if (valStr.isNotBlank()) {
                                if (!valStr.contains("Template Impor", ignoreCase = true) &&
                                    !valStr.contains("Template Data", ignoreCase = true) &&
                                    !valStr.equals("nama", ignoreCase = true) &&
                                    !valStr.equals("name", ignoreCase = true) &&
                                    !valStr.equals(menuName, ignoreCase = true) &&
                                    !valStr.startsWith("nama_", ignoreCase = true)) {
                                    parsedItems.add(valStr)
                                }
                            }
                        }

                        if (parsedItems.isNotEmpty()) {
                            val mutableCurrent = items.toMutableList()
                            var addedCount = 0
                            parsedItems.forEach { newItem ->
                                if (!mutableCurrent.contains(newItem)) {
                                    mutableCurrent.add(newItem)
                                    addedCount++
                                }
                            }
                            if (addedCount > 0) {
                                onSave(mutableCurrent)
                                Toast.makeText(context, "Berhasil mengimpor $addedCount data $menuName!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Semua data $menuName dari file Excel/CSV sudah ada!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Tidak ada data $menuName yang valid dalam file Excel/CSV!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "File Excel/CSV kosong!", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal membaca file Excel/CSV: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 0.dp)) {
            // Excel/CSV Import/Export and Template Row
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
                        containerColor = GlassWhite,
                        contentColor = DeepPurpleText
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("btn_impor_csv_${testTagPrefix}")
                ) {
                    Icon(imageVector = Icons.Default.Upload, contentDescription = "Impor Excel", tint = DeepPurpleText, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Impor", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DeepPurpleText, maxLines = 1)
                }

                Button(
                    onClick = {
                        if (items.isEmpty()) {
                            Toast.makeText(context, "Tidak ada data $menuName untuk diekspor!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val headerName = "nama_${menuName.lowercase().replace(" ", "_")}"
                        val headers = listOf(headerName)
                        val rows = items.map { listOf(it) }
                        val bytes = generateExcelBytes(
                            title = "Master Data $menuName Lunaris",
                            headers = headers,
                            rows = rows
                        )
                        saveFileToDownloads(
                            context = context,
                            filename = "Master_${menuName.replace(" ", "_")}_Lunaris_${System.currentTimeMillis()}.xlsx",
                            mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            bytes = bytes
                        ) {
                            Toast.makeText(context, "Data $menuName berhasil diekspor ke Excel (.xlsx)!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GlassWhite,
                        contentColor = DeepPurpleText
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("btn_ekspor_csv_${testTagPrefix}")
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = "Ekspor Excel", tint = DeepPurpleText, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ekspor", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DeepPurpleText, maxLines = 1)
                }

                Button(
                    onClick = {
                        val headerName = "nama_${menuName.lowercase().replace(" ", "_")}"
                        val headers = listOf(headerName)
                        val templateRows = listOf(
                            listOf("Contoh $menuName 1"),
                            listOf("Contoh $menuName 2"),
                            listOf("Contoh $menuName 3")
                        )
                        val bytes = generateExcelBytes(
                            title = "Template Impor Master $menuName Lunaris",
                            headers = headers,
                            rows = templateRows
                        )
                        saveFileToDownloads(
                            context = context,
                            filename = "Template_Master_${menuName.replace(" ", "_")}_Lunaris.xlsx",
                            mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            bytes = bytes
                        ) {
                            Toast.makeText(context, "Template Excel (.xlsx) $menuName berhasil diunduh!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GlassWhite,
                        contentColor = DeepPurpleText
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("btn_unduh_template_${testTagPrefix}")
                ) {
                    Icon(imageVector = Icons.Default.Description, contentDescription = "Unduh Template", tint = DeepPurpleText, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Template", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DeepPurpleText, maxLines = 1)
                }
            }

            if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(placeholder, color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray)
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isAllSelected = items.isNotEmpty() && selectedItemIndices.size == items.size
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            selectedItemIndices = if (isAllSelected) emptySet() else items.indices.toSet()
                        }
                    ) {
                        Checkbox(
                            checked = isAllSelected,
                            onCheckedChange = { checked ->
                                selectedItemIndices = if (checked) items.indices.toSet() else emptySet()
                            },
                            modifier = Modifier.testTag("checkbox_select_all_${testTagPrefix}")
                        )
                        Text(
                            text = if (isAllSelected) "Batal Pilih Semua" else "Pilih Semua (${items.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (selectedItemIndices.isNotEmpty()) {
                        Button(
                            onClick = { showBatchDeleteDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("btn_hapus_terpilih_${testTagPrefix}")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Hapus Terpilih (${selectedItemIndices.size})", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 160.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(items) { index, item ->
                        val isSelected = selectedItemIndices.contains(index)
                        LunarisCard(
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Color(0xFF7C3AED) else if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f) else Color(0xFFCBD5E1)
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFFF3E8FF) else if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFFFFFF)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            selectedItemIndices = if (checked) selectedItemIndices + index else selectedItemIndices - index
                                        },
                                        modifier = Modifier.testTag("checkbox_${testTagPrefix}_$index")
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = item,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            selectedIndexForEdit = index
                                            editInput = item
                                            showEditDialog = true
                                        },
                                        modifier = Modifier.testTag("edit_${testTagPrefix}_$index")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Ubah",
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            selectedIndexForDelete = index
                                            showDeleteDialog = true
                                        },
                                        modifier = Modifier.testTag("delete_${testTagPrefix}_$index")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Hapus",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                addInput = ""
                showAddDialog = true
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 100.dp, end = 16.dp)
                .zIndex(5f)
                .testTag("btn_tambah_${testTagPrefix}")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Tambah $menuName Baru",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }

    // Add Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Tambah $menuName Baru", fontWeight = FontWeight.Bold) },
            text = {
                LunarisTextField(
                    value = addInput,
                    onValueChange = { addInput = it },
                    label = { Text("Nama $menuName") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("input_${testTagPrefix}_name")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = addInput.trim()
                        if (trimmed.isEmpty()) {
                            Toast.makeText(context, "Nama tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (items.contains(trimmed)) {
                            Toast.makeText(context, "$menuName sudah ada!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val newList = items.toMutableList().apply { add(trimmed) }
                        onSave(newList)
                        Toast.makeText(context, "$menuName berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                        showAddDialog = false
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("dialog_btn_simpan_${testTagPrefix}")
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddDialog = false },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Batal")
                }
            }
        )
    }

    // Edit Dialog
    if (showEditDialog && selectedIndexForEdit != -1) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Ubah $menuName", fontWeight = FontWeight.Bold) },
            text = {
                LunarisTextField(
                    value = editInput,
                    onValueChange = { editInput = it },
                    label = { Text("Nama $menuName") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("edit_${testTagPrefix}_name")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = editInput.trim()
                        if (trimmed.isEmpty()) {
                            Toast.makeText(context, "Nama tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val oldVal = items[selectedIndexForEdit]
                        if (trimmed != oldVal && items.contains(trimmed)) {
                            Toast.makeText(context, "$menuName sudah ada!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val newList = items.toMutableList().apply { set(selectedIndexForEdit, trimmed) }
                        onSave(newList)
                        Toast.makeText(context, "$menuName berhasil diubah!", Toast.LENGTH_SHORT).show()
                        showEditDialog = false
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("dialog_btn_update_${testTagPrefix}")
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditDialog = false },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Batal")
                }
            }
        )
    }

    // Delete Dialog
    if (showDeleteDialog && selectedIndexForDelete != -1) {
        val itemToDelete = items[selectedIndexForDelete]
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus $menuName", fontWeight = FontWeight.Bold) },
            text = {
                Text("Apakah Anda yakin ingin menghapus $menuName '$itemToDelete' secara permanen? Fitur ini diperuntukkan khusus bagi koreksi data input yang keliru.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newList = items.toMutableList().apply { removeAt(selectedIndexForDelete) }
                        onSave(newList)
                        Toast.makeText(context, "$menuName berhasil dihapus!", Toast.LENGTH_SHORT).show()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("dialog_btn_hapus_${testTagPrefix}")
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Batal")
                }
            }
        )
    }

    // Batch Delete Dialog
    if (showBatchDeleteDialog && selectedItemIndices.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteDialog = false },
            title = { Text("Konfirmasi Hapus ${selectedItemIndices.size} Data $menuName", fontWeight = FontWeight.Bold) },
            text = {
                Text("Apakah Anda yakin ingin menghapus ${selectedItemIndices.size} data $menuName terpilih secara permanen? Fitur ini diperuntukkan khusus bagi koreksi data input yang keliru (bulk import).")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newList = items.filterIndexed { idx, _ -> idx !in selectedItemIndices }
                        onSave(newList)
                        Toast.makeText(context, "Berhasil menghapus ${selectedItemIndices.size} data $menuName!", Toast.LENGTH_SHORT).show()
                        selectedItemIndices = emptySet()
                        showBatchDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("dialog_btn_confirm_batch_delete_${testTagPrefix}")
                ) {
                    Text("Hapus Permanen")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBatchDeleteDialog = false },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun KelolaGuruMapelTab(
    viewModel: InventoryViewModel
) {
    val context = LocalContext.current
    val items by viewModel.guruMapel.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }

    var selectedIndexForEdit by remember { mutableStateOf(-1) }
    var selectedIndexForDelete by remember { mutableStateOf(-1) }
    var selectedGuruIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }

    var addNama by remember { mutableStateOf("") }
    var addNip by remember { mutableStateOf("") }
    var addMapel by remember { mutableStateOf("") }
    
    var editNama by remember { mutableStateOf("") }
    var editNip by remember { mutableStateOf("") }
    var editMapel by remember { mutableStateOf("") }

    var csvPreviewGuru by remember { mutableStateOf<List<Triple<String, String, String>>?>(null) }

    val isDark = isSystemInDarkTheme()

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val rows = readExcelOrCsvInputStream(inputStream)
                    
                    if (rows.isNotEmpty()) {
                        val rowsToPreview = if (rows.size >= 3) {
                            val r1 = rows.getOrNull(0)?.firstOrNull()?.trim() ?: ""
                            val r3 = rows.getOrNull(2)?.firstOrNull()?.trim() ?: ""
                            if (r1.contains("Template", ignoreCase = true) ||
                                r1.contains("Impor", ignoreCase = true) ||
                                r3.contains("Nama", ignoreCase = true) ||
                                r3.contains("NIP", ignoreCase = true) ||
                                r3.contains("Guru", ignoreCase = true) ||
                                r3.contains("Mapel", ignoreCase = true)) {
                                rows.drop(3)
                            } else if (r1.contains("Nama", ignoreCase = true) || r1.contains("NIP", ignoreCase = true)) {
                                rows.drop(1)
                            } else {
                                rows
                            }
                        } else if (rows.firstOrNull()?.firstOrNull()?.trim()?.let {
                            it.contains("Nama", ignoreCase = true) || it.contains("NIP", ignoreCase = true) || it.contains("Template", ignoreCase = true)
                        } == true) {
                            rows.drop(1)
                        } else {
                            rows
                        }
                        
                        val parsed = rowsToPreview.map { cols ->
                            val nama = cols.getOrNull(0)?.trim() ?: ""
                            val nip = cols.getOrNull(1)?.trim() ?: ""
                            val mapel = cols.getOrNull(2)?.trim() ?: ""
                            Triple(nama, nip, mapel)
                        }.filter { 
                            it.first.isNotBlank() &&
                            !it.first.contains("Template Impor", ignoreCase = true) &&
                            !it.first.contains("Template Data", ignoreCase = true) &&
                            !it.first.equals("Nama Guru", ignoreCase = true) &&
                            !it.first.equals("Nama", ignoreCase = true)
                        }
                        
                        if (parsed.isNotEmpty()) {
                            csvPreviewGuru = parsed
                        } else {
                            Toast.makeText(context, "Tidak ada data guru yang valid dalam file Excel/CSV!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "File Excel/CSV kosong!", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal membaca file Excel/CSV: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 0.dp)) {
            // CSV Import/Export and Template Row
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
                    containerColor = GlassWhite,
                    contentColor = DeepPurpleText
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("btn_impor_csv_guru")
            ) {
                Icon(imageVector = Icons.Default.Upload, contentDescription = "Impor CSV", tint = DeepPurpleText, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Impor", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DeepPurpleText, maxLines = 1)
            }

            Button(
                onClick = {
                    if (items.isEmpty()) {
                        Toast.makeText(context, "Tidak ada data guru untuk diekspor!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val headers = listOf("nama_guru", "nip", "mata_pelajaran")
                    val rows = items.map { rawItem ->
                        val parts = rawItem.split("|:|")
                        val nama = parts.getOrNull(0) ?: rawItem
                        val nip = if (parts.size >= 3) parts.getOrNull(1) ?: "" else ""
                        val mapel = if (parts.size >= 3) parts.getOrNull(2) ?: "" else parts.getOrNull(1) ?: ""
                        listOf(nama, nip, mapel)
                    }
                    val bytes = generateExcelBytes(
                        title = "Daftar Guru & Mata Pelajaran Lunaris",
                        headers = headers,
                        rows = rows
                    )
                    saveFileToDownloads(
                        context = context,
                        filename = "Daftar_Guru_Mapel_Lunaris_${System.currentTimeMillis()}.xlsx",
                        mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        bytes = bytes
                    ) {
                        Toast.makeText(context, "Daftar Guru berhasil diekspor ke Excel (.xlsx)!", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = GlassWhite,
                    contentColor = DeepPurpleText
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("btn_ekspor_csv_guru")
            ) {
                Icon(imageVector = Icons.Default.Download, contentDescription = "Ekspor Excel", tint = DeepPurpleText, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Ekspor", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DeepPurpleText, maxLines = 1)
            }

            Button(
                onClick = {
                    val headers = listOf("nama_guru", "nip", "mata_pelajaran")
                    val templateRows = listOf(
                        listOf("Dr. Budi Santoso, M.Pd", "198503152010011002", "Fisika"),
                        listOf("Siti Aminah, S.Pd", "", "Matematika"),
                        listOf("Ahmad Rifai, S.Kom", "199208102019031005", "Pemrograman Web")
                    )
                    val bytes = generateExcelBytes(
                        title = "Template Impor Data Guru Lunaris",
                        headers = headers,
                        rows = templateRows
                    )
                    saveFileToDownloads(
                        context = context,
                        filename = "Template_Impor_Guru_Lunaris.xlsx",
                        mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        bytes = bytes
                    ) {
                        Toast.makeText(context, "Template Excel (.xlsx) Guru berhasil diunduh!", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = GlassWhite,
                    contentColor = DeepPurpleText
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("btn_unduh_template_csv_guru")
            ) {
                Icon(imageVector = Icons.Default.Description, contentDescription = "Unduh Template", tint = DeepPurpleText, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Template", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DeepPurpleText, maxLines = 1)
            }
        }

        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Data guru mapel belum tersedia.", color = Color.Gray)
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isAllSelected = items.isNotEmpty() && selectedGuruIndices.size == items.size
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        selectedGuruIndices = if (isAllSelected) emptySet() else items.indices.toSet()
                    }
                ) {
                    Checkbox(
                        checked = isAllSelected,
                        onCheckedChange = { checked ->
                            selectedGuruIndices = if (checked) items.indices.toSet() else emptySet()
                        },
                        modifier = Modifier.testTag("checkbox_select_all_guru_mapel")
                    )
                    Text(
                        text = if (isAllSelected) "Batal Pilih Semua" else "Pilih Semua (${items.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (selectedGuruIndices.isNotEmpty()) {
                    Button(
                        onClick = { showBatchDeleteDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_hapus_terpilih_guru_mapel")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Hapus Terpilih (${selectedGuruIndices.size})", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }

            // Elegant responsive table view
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    // Table structure
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        // Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF1F5F9))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Nama Guru", modifier = Modifier.weight(0.38f), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (isDark) MaterialTheme.colorScheme.onSurface else DeepPurpleText)
                            Text("NIP", modifier = Modifier.weight(0.26f), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (isDark) MaterialTheme.colorScheme.onSurface else DeepPurpleText)
                            Text("Mapel", modifier = Modifier.weight(0.18f), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (isDark) MaterialTheme.colorScheme.onSurface else DeepPurpleText)
                            Text("Aksi", modifier = Modifier.weight(0.18f), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (isDark) MaterialTheme.colorScheme.onSurface else DeepPurpleText, textAlign = TextAlign.Center)
                        }

                        // Data rows
                        items.forEachIndexed { index, rawItem ->
                            val parts = rawItem.split("|:|")
                            val nama = parts.getOrNull(0) ?: rawItem
                            val nip = if (parts.size >= 3) parts.getOrNull(1) ?: "" else ""
                            val mapel = if (parts.size >= 3) parts.getOrNull(2) ?: "" else parts.getOrNull(1) ?: ""
                            val isSelected = selectedGuruIndices.contains(index)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isSelected) Color(0xFFF3E8FF)
                                        else if (isDark) {
                                            if (index % 2 == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                        } else {
                                            if (index % 2 == 0) Color.White else Color(0xFFF8FAFC)
                                        }
                                    )
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        selectedGuruIndices = if (checked) selectedGuruIndices + index else selectedGuruIndices - index
                                    },
                                    modifier = Modifier.size(32.dp).testTag("checkbox_guru_mapel_$index")
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(nama, modifier = Modifier.weight(0.38f), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF1E293B))
                                Text(if (nip.isEmpty()) "-" else nip, modifier = Modifier.weight(0.26f), fontSize = 13.sp, color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF64748B))
                                Text(if (mapel.isEmpty()) "-" else mapel, modifier = Modifier.weight(0.18f), fontSize = 13.sp, color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF64748B))

                                Row(
                                    modifier = Modifier.weight(0.18f),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            selectedIndexForEdit = index
                                            editNama = nama
                                            editNip = nip
                                            editMapel = mapel
                                            showEditDialog = true
                                        },
                                        modifier = Modifier.size(28.dp).testTag("edit_guru_mapel_$index")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Ubah",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(2.dp))
                                    IconButton(
                                        onClick = {
                                            selectedIndexForDelete = index
                                            showDeleteDialog = true
                                        },
                                        modifier = Modifier.size(28.dp).testTag("delete_guru_mapel_$index")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Hapus",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
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

        FloatingActionButton(
            onClick = {
                addNama = ""
                addNip = ""
                addMapel = ""
                showAddDialog = true
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 100.dp, end = 16.dp)
                .zIndex(5f)
                .testTag("btn_tambah_guru_mapel")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Tambah Guru Mapel",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Tambah Guru Mapel Baru", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LunarisTextField(
                        value = addNama,
                        onValueChange = { addNama = it },
                        label = { Text("Nama Guru (Wajib)") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().testTag("input_guru_name")
                    )
                    LunarisTextField(
                        value = addNip,
                        onValueChange = { addNip = it },
                        label = { Text("NIP (Opsional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().testTag("input_guru_nip")
                    )
                    LunarisTextField(
                        value = addMapel,
                        onValueChange = { addMapel = it },
                        label = { Text("Mata Pelajaran (Opsional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().testTag("input_guru_subject")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedNama = addNama.trim()
                        val trimmedNip = addNip.trim()
                        val trimmedMapel = addMapel.trim()
                        if (trimmedNama.isEmpty()) {
                            Toast.makeText(context, "Nama Guru tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val combined = "$trimmedNama|:|$trimmedNip|:|$trimmedMapel"
                        if (items.contains(combined)) {
                            Toast.makeText(context, "Data Guru Mapel tersebut sudah ada!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val newList = items.toMutableList().apply { add(combined) }
                        viewModel.updateGuruMapel(newList)
                        Toast.makeText(context, "Guru Mapel berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                        showAddDialog = false
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("dialog_btn_simpan_guru_mapel")
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddDialog = false },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Batal")
                }
            }
        )
    }

    if (showEditDialog && selectedIndexForEdit != -1) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Ubah Guru Mapel", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LunarisTextField(
                        value = editNama,
                        onValueChange = { editNama = it },
                        label = { Text("Nama Guru (Wajib)") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().testTag("edit_guru_name")
                    )
                    LunarisTextField(
                        value = editNip,
                        onValueChange = { editNip = it },
                        label = { Text("NIP (Opsional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().testTag("edit_guru_nip")
                    )
                    LunarisTextField(
                        value = editMapel,
                        onValueChange = { editMapel = it },
                        label = { Text("Mata Pelajaran (Opsional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().testTag("edit_guru_subject")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedNama = editNama.trim()
                        val trimmedNip = editNip.trim()
                        val trimmedMapel = editMapel.trim()
                        if (trimmedNama.isEmpty()) {
                            Toast.makeText(context, "Nama Guru tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val combined = "$trimmedNama|:|$trimmedNip|:|$trimmedMapel"
                        val newList = items.toMutableList().apply { set(selectedIndexForEdit, combined) }
                        viewModel.updateGuruMapel(newList)
                        Toast.makeText(context, "Guru Mapel berhasil diubah!", Toast.LENGTH_SHORT).show()
                        showEditDialog = false
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("dialog_btn_update_guru_mapel")
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditDialog = false },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Batal")
                }
            }
        )
    }

    if (showDeleteDialog && selectedIndexForDelete != -1) {
        val rawToDelete = items[selectedIndexForDelete]
        val parts = rawToDelete.split("|:|")
        val namaToDelete = parts.getOrNull(0) ?: rawToDelete
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus Guru Mapel", fontWeight = FontWeight.Bold) },
            text = {
                Text("Apakah Anda yakin ingin menghapus guru '$namaToDelete' secara permanen? Fitur ini diperuntukkan khusus bagi koreksi data input yang keliru.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newList = items.toMutableList().apply { removeAt(selectedIndexForDelete) }
                        viewModel.updateGuruMapel(newList)
                        Toast.makeText(context, "Guru Mapel berhasil dihapus!", Toast.LENGTH_SHORT).show()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("dialog_btn_hapus_guru_mapel")
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Batal")
                }
            }
        )
    }

    // Batch Delete Guru Mapel Dialog
    if (showBatchDeleteDialog && selectedGuruIndices.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteDialog = false },
            title = { Text("Konfirmasi Hapus ${selectedGuruIndices.size} Guru Mapel", fontWeight = FontWeight.Bold) },
            text = {
                Text("Apakah Anda yakin ingin menghapus ${selectedGuruIndices.size} data guru mapel terpilih secara permanen? Fitur ini diperuntukkan khusus bagi koreksi data input yang keliru (bulk import).")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newList = items.filterIndexed { idx, _ -> idx !in selectedGuruIndices }
                        viewModel.updateGuruMapel(newList)
                        Toast.makeText(context, "Berhasil menghapus ${selectedGuruIndices.size} guru mapel!", Toast.LENGTH_SHORT).show()
                        selectedGuruIndices = emptySet()
                        showBatchDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("dialog_btn_confirm_batch_delete_guru_mapel")
                ) {
                    Text("Hapus Permanen")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBatchDeleteDialog = false },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Batal")
                }
            }
        )
    }

    // CSV Import Preview Modal
    csvPreviewGuru?.let { previewList ->
        AlertDialog(
            onDismissRequest = { csvPreviewGuru = null },
            title = { Text("Pratinjau Impor Guru Mapel", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Berikut adalah ${previewList.size} data guru yang terdeteksi dari file CSV:",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .heightIn(max = 280.dp)
                            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(8.dp)
                        ) {
                            itemsIndexed(previewList) { index, item ->
                                val (nama, nip, mapel) = item
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Text(text = "${index + 1}. $nama", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text(text = "   NIP: ${if (nip.isBlank()) "-" else nip} | Mapel: ${if (mapel.isBlank()) "-" else mapel}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    if (index < previewList.size - 1) {
                                        Divider(modifier = Modifier.padding(top = 4.dp), color = Color.LightGray.copy(alpha = 0.5f))
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
                        val newList = items.toMutableList()
                        var added = 0
                        previewList.forEach { pItem ->
                            val (nama, nip, mapel) = pItem
                            val combined = "${nama.trim()}|:${nip.trim()}|:${mapel.trim()}"
                            if (!newList.contains(combined)) {
                                newList.add(combined)
                                added++
                            }
                        }
                        viewModel.updateGuruMapel(newList)
                        Toast.makeText(context, "Berhasil mengimpor $added data Guru baru!", Toast.LENGTH_SHORT).show()
                        csvPreviewGuru = null
                    },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Impor Semua")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { csvPreviewGuru = null },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun KelolaStafTab(
    viewModel: InventoryViewModel
) {
    val context = LocalContext.current
    val items by viewModel.staf.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }

    var selectedIndexForEdit by remember { mutableStateOf(-1) }
    var selectedIndexForDelete by remember { mutableStateOf(-1) }
    var selectedStafIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }

    var addNama by remember { mutableStateOf("") }
    var addNip by remember { mutableStateOf("") }
    var addJabatan by remember { mutableStateOf("") }
    
    var editNama by remember { mutableStateOf("") }
    var editNip by remember { mutableStateOf("") }
    var editJabatan by remember { mutableStateOf("") }

    var csvPreviewStaf by remember { mutableStateOf<List<Triple<String, String, String>>?>(null) }

    val isDark = isSystemInDarkTheme()

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val rows = readExcelOrCsvInputStream(inputStream)
                    
                    if (rows.isNotEmpty()) {
                        val rowsToPreview = if (rows.size >= 3) {
                            val r1 = rows.getOrNull(0)?.firstOrNull()?.trim() ?: ""
                            val r3 = rows.getOrNull(2)?.firstOrNull()?.trim() ?: ""
                            if (r1.contains("Template", ignoreCase = true) ||
                                r1.contains("Impor", ignoreCase = true) ||
                                r3.contains("Nama", ignoreCase = true) ||
                                r3.contains("NIP", ignoreCase = true) ||
                                r3.contains("Staf", ignoreCase = true) ||
                                r3.contains("Jabatan", ignoreCase = true)) {
                                rows.drop(3)
                            } else if (r1.contains("Nama", ignoreCase = true) || r1.contains("NIP", ignoreCase = true)) {
                                rows.drop(1)
                            } else {
                                rows
                            }
                        } else if (rows.firstOrNull()?.firstOrNull()?.trim()?.let {
                            it.contains("Nama", ignoreCase = true) || it.contains("NIP", ignoreCase = true) || it.contains("Template", ignoreCase = true)
                        } == true) {
                            rows.drop(1)
                        } else {
                            rows
                        }
                        
                        val parsed = rowsToPreview.map { cols ->
                            val nama = cols.getOrNull(0)?.trim() ?: ""
                            val nip = cols.getOrNull(1)?.trim() ?: ""
                            val jabatan = cols.getOrNull(2)?.trim() ?: ""
                            Triple(nama, nip, jabatan)
                        }.filter { 
                            it.first.isNotBlank() &&
                            !it.first.contains("Template Impor", ignoreCase = true) &&
                            !it.first.contains("Template Data", ignoreCase = true) &&
                            !it.first.equals("Nama Staf", ignoreCase = true) &&
                            !it.first.equals("Nama", ignoreCase = true)
                        }
                        
                        if (parsed.isNotEmpty()) {
                            csvPreviewStaf = parsed
                        } else {
                            Toast.makeText(context, "Tidak ada data staf yang valid dalam file Excel/CSV!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "File Excel/CSV kosong!", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal membaca file Excel/CSV: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 0.dp)) {
            // CSV Import/Export and Template Row
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
                    containerColor = GlassWhite,
                    contentColor = DeepPurpleText
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("btn_impor_csv_staf")
            ) {
                Icon(imageVector = Icons.Default.Upload, contentDescription = "Impor CSV", tint = DeepPurpleText, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Impor", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DeepPurpleText, maxLines = 1)
            }

            Button(
                onClick = {
                    if (items.isEmpty()) {
                        Toast.makeText(context, "Tidak ada data staf untuk diekspor!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val headers = listOf("nama_staf", "nip", "jabatan")
                    val rows = items.map { rawItem ->
                        val parts = rawItem.split("|:|")
                        val nama = parts.getOrNull(0) ?: rawItem
                        val nip = if (parts.size >= 3) parts.getOrNull(1) ?: "" else ""
                        val jabatan = if (parts.size >= 3) parts.getOrNull(2) ?: "" else parts.getOrNull(1) ?: ""
                        listOf(nama, nip, jabatan)
                    }
                    val bytes = generateExcelBytes(
                        title = "Daftar Staf & Tenaga Kependidikan Lunaris",
                        headers = headers,
                        rows = rows
                    )
                    saveFileToDownloads(
                        context = context,
                        filename = "Daftar_Staf_Lunaris_${System.currentTimeMillis()}.xlsx",
                        mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        bytes = bytes
                    ) {
                        Toast.makeText(context, "Daftar Staf berhasil diekspor ke Excel (.xlsx)!", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = GlassWhite,
                    contentColor = DeepPurpleText
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("btn_ekspor_csv_staf")
            ) {
                Icon(imageVector = Icons.Default.Download, contentDescription = "Ekspor Excel", tint = DeepPurpleText, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Ekspor", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DeepPurpleText, maxLines = 1)
            }

            Button(
                onClick = {
                    val headers = listOf("nama_staf", "nip", "jabatan")
                    val templateRows = listOf(
                        listOf("Ahmad Subagyo, S.E", "197812042005011001", "Kepala Tata Usaha"),
                        listOf("Dewi Lestari", "", "Staf Administrasi"),
                        listOf("Bambang Hermawan", "198305112009021003", "Teknisi Laboratorium")
                    )
                    val bytes = generateExcelBytes(
                        title = "Template Impor Data Staf Lunaris",
                        headers = headers,
                        rows = templateRows
                    )
                    saveFileToDownloads(
                        context = context,
                        filename = "Template_Impor_Staf_Lunaris.xlsx",
                        mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        bytes = bytes
                    ) {
                        Toast.makeText(context, "Template Excel (.xlsx) Staf berhasil diunduh!", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = GlassWhite,
                    contentColor = DeepPurpleText
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("btn_unduh_template_csv_staf")
            ) {
                Icon(imageVector = Icons.Default.Description, contentDescription = "Unduh Template", tint = DeepPurpleText, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Template", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DeepPurpleText, maxLines = 1)
            }
        }

        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Data staf belum tersedia.", color = Color.Gray)
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isAllSelected = items.isNotEmpty() && selectedStafIndices.size == items.size
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        selectedStafIndices = if (isAllSelected) emptySet() else items.indices.toSet()
                    }
                ) {
                    Checkbox(
                        checked = isAllSelected,
                        onCheckedChange = { checked ->
                            selectedStafIndices = if (checked) items.indices.toSet() else emptySet()
                        },
                        modifier = Modifier.testTag("checkbox_select_all_staf")
                    )
                    Text(
                        text = if (isAllSelected) "Batal Pilih Semua" else "Pilih Semua (${items.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (selectedStafIndices.isNotEmpty()) {
                    Button(
                        onClick = { showBatchDeleteDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_hapus_terpilih_staf")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Hapus Terpilih (${selectedStafIndices.size})", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }

            // Elegant responsive table view
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    // Table structure
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        // Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF1F5F9))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Nama Staf", modifier = Modifier.weight(0.38f), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (isDark) MaterialTheme.colorScheme.onSurface else DeepPurpleText)
                            Text("NIP", modifier = Modifier.weight(0.26f), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (isDark) MaterialTheme.colorScheme.onSurface else DeepPurpleText)
                            Text("Jabatan", modifier = Modifier.weight(0.18f), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (isDark) MaterialTheme.colorScheme.onSurface else DeepPurpleText)
                            Text("Aksi", modifier = Modifier.weight(0.18f), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (isDark) MaterialTheme.colorScheme.onSurface else DeepPurpleText, textAlign = TextAlign.Center)
                        }

                        // Data rows
                        items.forEachIndexed { index, rawItem ->
                            val parts = rawItem.split("|:|")
                            val nama = parts.getOrNull(0) ?: rawItem
                            val nip = if (parts.size >= 3) parts.getOrNull(1) ?: "" else ""
                            val jabatan = if (parts.size >= 3) parts.getOrNull(2) ?: "" else parts.getOrNull(1) ?: ""
                            val isSelected = selectedStafIndices.contains(index)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isSelected) Color(0xFFF3E8FF)
                                        else if (isDark) {
                                            if (index % 2 == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                        } else {
                                            if (index % 2 == 0) Color.White else Color(0xFFF8FAFC)
                                        }
                                    )
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        selectedStafIndices = if (checked) selectedStafIndices + index else selectedStafIndices - index
                                    },
                                    modifier = Modifier.size(32.dp).testTag("checkbox_staf_$index")
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(nama, modifier = Modifier.weight(0.38f), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF1E293B))
                                Text(if (nip.isEmpty()) "-" else nip, modifier = Modifier.weight(0.26f), fontSize = 13.sp, color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF64748B))
                                Text(if (jabatan.isEmpty()) "-" else jabatan, modifier = Modifier.weight(0.18f), fontSize = 13.sp, color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF64748B))

                                Row(
                                    modifier = Modifier.weight(0.18f),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            selectedIndexForEdit = index
                                            editNama = nama
                                            editNip = nip
                                            editJabatan = jabatan
                                            showEditDialog = true
                                        },
                                        modifier = Modifier.size(28.dp).testTag("edit_staf_$index")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Ubah",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(2.dp))
                                    IconButton(
                                        onClick = {
                                            selectedIndexForDelete = index
                                            showDeleteDialog = true
                                        },
                                        modifier = Modifier.size(28.dp).testTag("delete_staf_$index")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Hapus",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
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

        FloatingActionButton(
            onClick = {
                addNama = ""
                addNip = ""
                addJabatan = ""
                showAddDialog = true
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 100.dp, end = 16.dp)
                .zIndex(5f)
                .testTag("btn_tambah_staf")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Tambah Staf",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Tambah Staf Baru", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LunarisTextField(
                        value = addNama,
                        onValueChange = { addNama = it },
                        label = { Text("Nama Staf (Wajib)") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().testTag("input_staf_name")
                    )
                    LunarisTextField(
                        value = addNip,
                        onValueChange = { addNip = it },
                        label = { Text("NIP (Opsional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().testTag("input_staf_nip")
                    )
                    LunarisTextField(
                        value = addJabatan,
                        onValueChange = { addJabatan = it },
                        label = { Text("Jabatan (Opsional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().testTag("input_staf_role")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedNama = addNama.trim()
                        val trimmedNip = addNip.trim()
                        val trimmedJabatan = addJabatan.trim()
                        if (trimmedNama.isEmpty()) {
                            Toast.makeText(context, "Nama Staf tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val combined = "$trimmedNama|:|$trimmedNip|:|$trimmedJabatan"
                        if (items.contains(combined)) {
                            Toast.makeText(context, "Data Staf tersebut sudah ada!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val newList = items.toMutableList().apply { add(combined) }
                        viewModel.updateStaf(newList)
                        Toast.makeText(context, "Staf berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                        showAddDialog = false
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("dialog_btn_simpan_staf")
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddDialog = false },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Batal")
                }
            }
        )
    }

    if (showEditDialog && selectedIndexForEdit != -1) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Ubah Staf", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LunarisTextField(
                        value = editNama,
                        onValueChange = { editNama = it },
                        label = { Text("Nama Staf (Wajib)") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().testTag("edit_staf_name")
                    )
                    LunarisTextField(
                        value = editNip,
                        onValueChange = { editNip = it },
                        label = { Text("NIP (Opsional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().testTag("edit_staf_nip")
                    )
                    LunarisTextField(
                        value = editJabatan,
                        onValueChange = { editJabatan = it },
                        label = { Text("Jabatan (Opsional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().testTag("edit_staf_role")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedNama = editNama.trim()
                        val trimmedNip = editNip.trim()
                        val trimmedJabatan = editJabatan.trim()
                        if (trimmedNama.isEmpty()) {
                            Toast.makeText(context, "Nama Staf tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val combined = "$trimmedNama|:|$trimmedNip|:|$trimmedJabatan"
                        val newList = items.toMutableList().apply { set(selectedIndexForEdit, combined) }
                        viewModel.updateStaf(newList)
                        Toast.makeText(context, "Staf berhasil diubah!", Toast.LENGTH_SHORT).show()
                        showEditDialog = false
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("dialog_btn_update_staf")
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditDialog = false },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Batal")
                }
            }
        )
    }

    if (showDeleteDialog && selectedIndexForDelete != -1) {
        val rawToDelete = items[selectedIndexForDelete]
        val parts = rawToDelete.split("|:|")
        val namaToDelete = parts.getOrNull(0) ?: rawToDelete
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus Staf", fontWeight = FontWeight.Bold) },
            text = {
                Text("Apakah Anda yakin ingin menghapus staf '$namaToDelete' secara permanen? Fitur ini diperuntukkan khusus bagi koreksi data input yang keliru.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newList = items.toMutableList().apply { removeAt(selectedIndexForDelete) }
                        viewModel.updateStaf(newList)
                        Toast.makeText(context, "Staf berhasil dihapus!", Toast.LENGTH_SHORT).show()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("dialog_btn_hapus_staf")
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Batal")
                }
            }
        )
    }

    // Batch Delete Staf Dialog
    if (showBatchDeleteDialog && selectedStafIndices.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteDialog = false },
            title = { Text("Konfirmasi Hapus ${selectedStafIndices.size} Staf", fontWeight = FontWeight.Bold) },
            text = {
                Text("Apakah Anda yakin ingin menghapus ${selectedStafIndices.size} data staf terpilih secara permanen? Fitur ini diperuntukkan khusus bagi koreksi data input yang keliru (bulk import).")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newList = items.filterIndexed { idx, _ -> idx !in selectedStafIndices }
                        viewModel.updateStaf(newList)
                        Toast.makeText(context, "Berhasil menghapus ${selectedStafIndices.size} staf!", Toast.LENGTH_SHORT).show()
                        selectedStafIndices = emptySet()
                        showBatchDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("dialog_btn_confirm_batch_delete_staf")
                ) {
                    Text("Hapus Permanen")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBatchDeleteDialog = false },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Batal")
                }
            }
        )
    }

    // CSV Import Preview Modal
    csvPreviewStaf?.let { previewList ->
        AlertDialog(
            onDismissRequest = { csvPreviewStaf = null },
            title = { Text("Pratinjau Impor Staf", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Berikut adalah ${previewList.size} data staf yang terdeteksi dari file CSV:",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .heightIn(max = 280.dp)
                            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(8.dp)
                        ) {
                            itemsIndexed(previewList) { index, item ->
                                val (nama, nip, jabatan) = item
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Text(text = "${index + 1}. $nama", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text(text = "   NIP: ${if (nip.isBlank()) "-" else nip} | Jabatan: ${if (jabatan.isBlank()) "-" else jabatan}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    if (index < previewList.size - 1) {
                                        Divider(modifier = Modifier.padding(top = 4.dp), color = Color.LightGray.copy(alpha = 0.5f))
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
                        val newList = items.toMutableList()
                        var added = 0
                        previewList.forEach { pItem ->
                            val (nama, nip, jabatan) = pItem
                            val combined = "${nama.trim()}|:${nip.trim()}|:${jabatan.trim()}"
                            if (!newList.contains(combined)) {
                                newList.add(combined)
                                added++
                            }
                        }
                        viewModel.updateStaf(newList)
                        Toast.makeText(context, "Berhasil mengimpor $added data Staf baru!", Toast.LENGTH_SHORT).show()
                        csvPreviewStaf = null
                    },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Impor Semua")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { csvPreviewStaf = null },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Batal")
                }
            }
        )
    }
}

private fun escapeCsv(value: String): String {
    val clean = value.replace("\"", "\"\"")
    return if (clean.contains(",") || clean.contains("\"") || clean.contains("\n") || clean.contains("\r")) {
        "\"$clean\""
    } else {
        clean
    }
}
