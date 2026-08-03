package com.example.ui.screens

import com.example.ui.components.CameraScannerDialog
import com.example.ui.components.ScanMode

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraControl
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.ui.components.LunarisCard
import com.example.ui.components.LunarisFilterDialog
import com.example.ui.components.LunarisDatePickerDialog
import com.example.ui.components.FilterGroup
import com.example.data.model.ItemWithStock
import com.example.ui.theme.DeepPurpleText
import com.example.ui.viewmodel.InventoryViewModel
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PcUnitData(
    val id: String = "",
    val name: String = "",
    val jenisPerangkat: String = "PC",
    val serialNumber: String = "",
    val merek: String = "",
    val processor: String = "",
    val tipeRam: String = "DDR4",
    val kapasitasRam: String = "8 GB",
    val storage: String = "SSD NVMe",
    val kapasitasStorage: String = "",
    val layarInch: String = "",
    val labRoom: String = "",
    val status: String = "Baik / Normal",
    val sumberDana: String = "",
    val qty: Int = 1,
    val satuan: String = "Unit",
    val keterangan: String = ""
)

fun encodePcSpecs(
    processor: String,
    tipeRam: String,
    kapasitasRam: String,
    storage: String,
    kapasitasStorage: String,
    layarInch: String,
    keterangan: String
): String {
    return "PROC:$processor|RAM_T:$tipeRam|RAM_C:$kapasitasRam|STG_T:$storage|STG_C:$kapasitasStorage|LAYAR:$layarInch|NOTE:$keterangan"
}

fun ItemWithStock.toPcUnitData(): PcUnitData {
    var proc = ""
    var ramT = "DDR4"
    var ramC = "8 GB"
    var stgT = "SSD NVMe"
    var stgC = ""
    var layar = ""
    var note = keterangan

    if (keterangan.contains("PROC:") || keterangan.contains("RAM_T:")) {
        val parts = keterangan.split("|")
        val noteParts = mutableListOf<String>()
        for (part in parts) {
            when {
                part.startsWith("PROC:") -> proc = part.removePrefix("PROC:")
                part.startsWith("RAM_T:") -> ramT = part.removePrefix("RAM_T:")
                part.startsWith("RAM_C:") -> ramC = part.removePrefix("RAM_C:")
                part.startsWith("STG_T:") -> stgT = part.removePrefix("STG_T:")
                part.startsWith("STG_C:") -> stgC = part.removePrefix("STG_C:")
                part.startsWith("LAYAR:") -> layar = part.removePrefix("LAYAR:")
                part.startsWith("NOTE:") -> noteParts.add(part.removePrefix("NOTE:"))
                else -> if (part.isNotBlank()) noteParts.add(part)
            }
        }
        note = noteParts.joinToString(" | ")
    }

    return PcUnitData(
        id = idBarang,
        name = namaBarang,
        jenisPerangkat = when (kategori) {
            "PC Desktop" -> "PC"
            "AIO (All-in-One)", "AIO All in One", "PC All-in-One", "All-in-One" -> "AIO"
            else -> kategori.ifBlank { "PC" }
        },
        serialNumber = serialNumber,
        merek = merekAlat,
        processor = proc,
        tipeRam = ramT.ifBlank { "DDR4" },
        kapasitasRam = ramC.ifBlank { "8 GB" },
        storage = when (stgT) {
            "SSD NVMe M.2" -> "SSD NVMe"
            "SSD SATA 2.5" -> "SSD SATA"
            else -> stgT.ifBlank { "SSD NVMe" }
        },
        kapasitasStorage = stgC,
        layarInch = layar,
        labRoom = ruang,
        status = kondisi.ifBlank { "Normal / Baik" },
        sumberDana = sumberDana ?: "",
        qty = if (stokAwal > 0) stokAwal else 1,
        satuan = satuan.ifBlank { "Unit" },
        keterangan = note
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabKomScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userRole by viewModel.userRole.collectAsState()
    val canLabKomView = userRole.contains("admin", ignoreCase = true) || viewModel.isStudentPermissionGranted("labkom_view")
    val canLabKomManage = userRole.contains("admin", ignoreCase = true) || viewModel.isStudentPermissionGranted("labkom_manage")

    LaunchedEffect(Unit) {
        viewModel.forceRefreshState()
    }

    // Collect Master Data for filtering and options
    val ruangList by viewModel.ruang.collectAsState()
    val ruangOptions = ruangList

    var showFilterMenu by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("Semua Filter") }
    var searchQuery by remember { mutableStateOf("") }
    var showQrScanner by remember { mutableStateOf(false) }

    // Collect items reactively from ViewModel StateFlow
    val itemsWithStock by viewModel.itemsWithStock.collectAsState()

    val labKomCats = remember {
        setOf(
            "PC", "AIO", "PC Desktop", "Workstation Design", "Server Lab / NOC",
            "All-in-One PC", "Laptop LabKom", "Hardware Komputer",
            "Workstation", "Server", "All-in-One", "LabKom", "Komputer"
        )
    }

    val pcUnitsList = remember(itemsWithStock) {
        itemsWithStock.filter { item ->
            item.type.equals("LABKOM", ignoreCase = true) ||
            item.idBarang.startsWith("PC-LAB", ignoreCase = true) ||
            item.idBarang.startsWith("LAB-", ignoreCase = true) ||
            labKomCats.contains(item.kategori) ||
            item.namaBarang.contains("PC Lab", ignoreCase = true) ||
            item.namaBarang.contains("Workstation", ignoreCase = true) ||
            item.namaBarang.contains("LabKom", ignoreCase = true)
        }.map { item ->
            item.toPcUnitData()
        }
    }

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val rows = readExcelOrCsvInputStream(inputStream)
                    val importedUnits = mutableListOf<PcUnitData>()
                    
                    if (rows.isNotEmpty()) {
                        val dataRows = if (rows.size >= 3) {
                            val firstCellRow1 = rows.getOrNull(0)?.firstOrNull()?.trim() ?: ""
                            val firstCellRow3 = rows.getOrNull(2)?.firstOrNull()?.trim() ?: ""
                            if (firstCellRow1.contains("Template", ignoreCase = true) ||
                                firstCellRow1.contains("Impor", ignoreCase = true) ||
                                firstCellRow1.contains("Laboratorium", ignoreCase = true) ||
                                firstCellRow3.contains("Jenis", ignoreCase = true) ||
                                firstCellRow3.contains("Perangkat", ignoreCase = true)) {
                                // Row 1: Title, Row 2: Blank, Row 3: Header -> Start reading data from Row 4 (index 3)
                                rows.drop(3)
                            } else if (firstCellRow1.contains("Jenis", ignoreCase = true) ||
                                       firstCellRow1.contains("Perangkat", ignoreCase = true)) {
                                rows.drop(1)
                            } else {
                                rows
                            }
                        } else if (rows.firstOrNull()?.getOrNull(0)?.contains("Jenis", ignoreCase = true) == true ||
                                   rows.firstOrNull()?.getOrNull(0)?.contains("Perangkat", ignoreCase = true) == true) {
                            rows.drop(1)
                        } else {
                            rows
                        }
                        
                        for (cols in dataRows) {
                            if (cols.size >= 3 && cols.any { it.isNotBlank() }) {
                                val firstCell = cols.getOrElse(0) { "" }.trim()
                                if (firstCell.contains("Template Impor", ignoreCase = true) ||
                                    firstCell.contains("Template Data", ignoreCase = true) ||
                                    firstCell.equals("Jenis Perangkat", ignoreCase = true) ||
                                    firstCell.equals("Jenis_Perangkat", ignoreCase = true) ||
                                    firstCell.equals("Jenis", ignoreCase = true) ||
                                    firstCell.equals("SN", ignoreCase = true)) {
                                    continue
                                }

                                val newUnit = PcUnitData(
                                    jenisPerangkat = cols.getOrElse(0) { "PC" }.let { raw ->
                                        when (raw) {
                                            "PC Desktop" -> "PC"
                                            "AIO (All-in-One)", "AIO All in One", "PC All-in-One", "All-in-One" -> "AIO"
                                            else -> raw.ifBlank { "PC" }
                                        }
                                    },
                                    serialNumber = cols.getOrElse(1) { "" },
                                    name = cols.getOrElse(2) { "PC Lab" }.ifBlank { "PC Lab" },
                                    id = cols.getOrElse(3) { "" }.ifBlank { "PC-LAB-${System.currentTimeMillis() % 10000}" },
                                    merek = cols.getOrElse(4) { "" },
                                    tipeRam = cols.getOrElse(5) { "DDR4" }.ifBlank { "DDR4" },
                                    kapasitasRam = cols.getOrElse(6) { "8 GB" }.ifBlank { "8 GB" },
                                    storage = cols.getOrElse(7) { "SSD NVMe" }.let { raw ->
                                        when (raw) {
                                            "SSD NVMe M.2" -> "SSD NVMe"
                                            "SSD SATA 2.5" -> "SSD SATA"
                                            else -> raw.ifBlank { "SSD NVMe" }
                                        }
                                    },
                                    kapasitasStorage = cols.getOrElse(8) { "256 GB" }.ifBlank { "256 GB" },
                                    processor = cols.getOrElse(9) { "" },
                                    layarInch = cols.getOrElse(10) { "24 Inch" }.ifBlank { "24 Inch" },
                                    labRoom = cols.getOrElse(11) { "" },
                                    status = cols.getOrElse(12) { "Normal / Baik" }.ifBlank { "Normal / Baik" },
                                    sumberDana = cols.getOrElse(13) { "BOS" },
                                    qty = cols.getOrElse(14) { "1" }.toIntOrNull() ?: 1,
                                    satuan = cols.getOrElse(15) { "Unit" }.ifBlank { "Unit" },
                                    keterangan = cols.getOrElse(16) { "" }
                                )
                                importedUnits.add(newUnit)
                            }
                        }
                    }
                    if (importedUnits.isNotEmpty()) {
                        coroutineScope.launch {
                            for (unit in importedUnits) {
                                val fullKeterangan = encodePcSpecs(
                                    processor = unit.processor,
                                    tipeRam = unit.tipeRam,
                                    kapasitasRam = unit.kapasitasRam,
                                    storage = unit.storage,
                                    kapasitasStorage = unit.kapasitasStorage,
                                    layarInch = unit.layarInch,
                                    keterangan = unit.keterangan
                                )
                                viewModel.registerNewItem(
                                    name = unit.name,
                                    serialNumber = unit.serialNumber,
                                    stokAwal = unit.qty,
                                    kategori = unit.jenisPerangkat,
                                    satuan = unit.satuan,
                                    merekAlat = unit.merek,
                                    ruang = unit.labRoom,
                                    sumberDana = unit.sumberDana.ifBlank { null },
                                    kondisi = unit.status,
                                    keterangan = fullKeterangan,
                                    isBorrowable = false,
                                    useAutoId = false,
                                    customId = unit.id,
                                    type = "LABKOM",
                                    onSuccess = {},
                                    onError = {}
                                )
                            }
                            viewModel.logSystemActivity(
                                activityType = "Impor Massal",
                                subjectName = "Impor Massal PC Unit LabKom (${importedUnits.size} Unit)",
                                details = "Berhasil mengimpor ${importedUnits.size} unit PC LabKom secara massal via file Excel/CSV.",
                                officerName = null
                            )
                            Toast.makeText(context, "Berhasil mengimpor ${importedUnits.size} unit PC!", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(context, "File CSV tidak berisi data valid!", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal mengimpor file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    var showAddPcDialog by remember { mutableStateOf(false) }
    var selectedUnitForEdit by remember { mutableStateOf<PcUnitData?>(null) }
    var unitToDelete by remember { mutableStateOf<PcUnitData?>(null) }
    var selectedUnitForDetail by remember { mutableStateOf<PcUnitData?>(null) }

    var selectedUnitIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showBatchDeleteConfirmDialog by remember { mutableStateOf(false) }

    var showDiagnosisDialog by remember { mutableStateOf(false) }
    var selectedUnitForDiagnosis by remember { mutableStateOf<PcUnitData?>(null) }
    val defaultOfficerState by viewModel.defaultOfficer.collectAsState()
    val defaultOfficer = defaultOfficerState.ifBlank { "Administrator" }

    val filteredList = pcUnitsList.filter { unit ->
        val matchFilter = when {
            selectedFilter == "Semua Filter" -> true
            selectedFilter.startsWith("Ruang: ") -> unit.labRoom.equals(selectedFilter.removePrefix("Ruang: "), ignoreCase = true)
            selectedFilter.startsWith("Jenis: ") -> unit.jenisPerangkat.equals(selectedFilter.removePrefix("Jenis: "), ignoreCase = true)
            selectedFilter.startsWith("Status: ") -> unit.status.contains(selectedFilter.removePrefix("Status: "), ignoreCase = true)
            else -> true
        }
        val matchSearch = searchQuery.isBlank() ||
                unit.name.contains(searchQuery, ignoreCase = true) ||
                unit.id.contains(searchQuery, ignoreCase = true) ||
                unit.serialNumber.contains(searchQuery, ignoreCase = true) ||
                unit.processor.contains(searchQuery, ignoreCase = true) ||
                unit.merek.contains(searchQuery, ignoreCase = true) ||
                unit.jenisPerangkat.contains(searchQuery, ignoreCase = true)
        matchFilter && matchSearch
    }

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
                            modifier = Modifier.size(40.dp).testTag("btn_back_labkom")
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
                                text = "Laboratorium Komputer",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Manajemen laboratorium komputer",
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
                    onClick = {
                        selectedUnitForEdit = null
                        showAddPcDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.testTag("btn_tambah_unit_pc")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Tambah Unit PC",
                        modifier = Modifier.size(24.dp)
                    )
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
                            text = "Anda tidak memiliki izin untuk mengakses modul Laboratorium Komputer. Silakan hubungi Super Admin.",
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
                // Summary Cards
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LunarisCard(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E8FF)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text("RUANGAN LAB", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B21A8))
                            Text("${ruangOptions.size} Ruang", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF7C3AED))
                            Text("Fasilitas Terdaftar", fontSize = 9.sp, color = Color(0xFF6B21A8).copy(alpha = 0.8f))
                        }
                    }

                    LunarisCard(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2FE)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text("TOTAL UNIT PC", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0369A1))
                            Text("${pcUnitsList.size} Unit", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0284C7))
                            Text("Perangkat Terdaftar", fontSize = 9.sp, color = Color(0xFF0369A1).copy(alpha = 0.8f))
                        }
                    }

                    LunarisCard(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text("STATUS AKTIF", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                            Text("${pcUnitsList.count { it.status.contains("Baik", ignoreCase = true) || it.status.contains("Normal", ignoreCase = true) }} Baik", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF16A34A))
                            Text("Siap Digunakan Lab", fontSize = 9.sp, color = Color(0xFF15803D).copy(alpha = 0.8f))
                        }
                    }
                }

                // Action Buttons Bar (Export, Import, Unduh Template)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
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
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f).testTag("btn_import_labkom")
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = "Impor", modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Impor", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                val filename = "Data_PC_LabKom_Lunaris_${System.currentTimeMillis()}.xlsx"
                                val headers = listOf(
                                    "Jenis Perangkat", "SN", "Nama Perangkat", "ID Perangkat", "Merek",
                                    "Tipe RAM", "Kapasitas RAM", "Storage", "Kapasitas", "Processor",
                                    "Layar", "Ruang", "Kondisi", "Sumber Dana", "Qty", "Satuan", "Keterangan"
                                )
                                val rows = pcUnitsList.map { unit ->
                                    listOf(
                                        unit.jenisPerangkat,
                                        unit.serialNumber,
                                        unit.name,
                                        unit.id,
                                        unit.merek,
                                        unit.tipeRam,
                                        unit.kapasitasRam,
                                        unit.storage,
                                        unit.kapasitasStorage,
                                        unit.processor,
                                        unit.layarInch,
                                        unit.labRoom,
                                        unit.status,
                                        unit.sumberDana,
                                        unit.qty.toString(),
                                        unit.satuan,
                                        unit.keterangan
                                    )
                                }
                                val bytes = generateExcelBytes(
                                    title = "Data Perangkat Laboratorium Komputer Lunaris",
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
                                        Toast.makeText(context, "Seluruh data PC LabKom berhasil diekspor ke Excel (.xlsx)!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f).testTag("btn_export_labkom")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Ekspor", modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ekspor", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = {
                            val templateFilename = "Template_Impor_LabKom_Lunaris.xlsx"
                            val templateMimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                            val headers = listOf(
                                "Jenis Perangkat", "SN", "Nama Perangkat", "ID Perangkat", "Merek",
                                "Tipe RAM", "Kapasitas RAM", "Storage", "Kapasitas", "Processor",
                                "Layar", "Ruang", "Kondisi", "Sumber Dana", "Qty", "Satuan", "Keterangan"
                            )
                            val templateRows = listOf(
                                listOf("PC", "SN-ASUS-2026-001", "PC LabKom 01", "PC-LAB1-001", "Asus", "DDR4", "16 GB", "SSD NVMe", "512 GB", "Intel Core i5-10400F @ 2.90GHz", "24 Inch", "Lab Komputer 1", "Normal / Baik", "BOS", "1", "Unit", "Unit PC Siap Pakai Praktikum"),
                                listOf("Workstation", "SN-DELL-2026-089", "Workstation Design 02", "PC-LAB2-002", "Dell", "DDR4", "32 GB", "SSD NVMe", "1 TB", "Intel Core i7-12700K @ 3.60GHz", "27 Inch", "Lab Komputer 2", "Pemeliharaan", "BOS Reguler", "1", "Unit", "Unit PC High End Grafis")
                            )
                            val bytes = generateExcelBytes(
                                title = "Template Impor Data PC Laboratorium Lunaris",
                                headers = headers,
                                rows = templateRows
                            )
                            saveFileToDownloads(
                                context = context,
                                filename = templateFilename,
                                mimeType = templateMimeType,
                                bytes = bytes
                            ) {
                                Toast.makeText(context, "Template Excel (.xlsx) LabKom berhasil diunduh!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFD97706)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD97706)),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f).testTag("btn_unduh_template_labkom")
                    ) {
                        Icon(Icons.Default.Description, contentDescription = "Template", modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Template", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Search Bar + Unified Filter Dropdown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Ketik untuk mencari...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                                IconButton(onClick = { showQrScanner = true }) {
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
                            .testTag("input_search_labkom"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    if (showQrScanner) {
                        SearchQrScanDialog(
                            onDismiss = { showQrScanner = false },
                            onQrScanned = { scannedCode ->
                                showQrScanner = false
                                searchQuery = scannedCode
                            }
                        )
                    }

                    IconButton(
                        onClick = { showFilterMenu = true },
                        modifier = Modifier
                            .size(50.dp)
                            .background(
                                if (selectedFilter != "Semua Filter") Color(0xFF7C3AED).copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                RoundedCornerShape(12.dp)
                            )
                            .testTag("btn_filter_labkom")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = if (selectedFilter != "Semua Filter") Color(0xFF7C3AED) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (selectedFilter != "Semua Filter") {
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
                                    text = "Filter: $selectedFilter",
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
                                        .clickable { selectedFilter = "Semua Filter" }
                                )
                            }
                        }
                    }
                }

                if (showFilterMenu) {
                    var tempRuangFilter by remember {
                        mutableStateOf(
                            if (selectedFilter.startsWith("Ruang: ")) selectedFilter.removePrefix("Ruang: ") else "Semua Ruang"
                        )
                    }
                    var tempJenisFilter by remember {
                        mutableStateOf(
                            if (selectedFilter.startsWith("Jenis: ")) selectedFilter.removePrefix("Jenis: ") else "Semua Jenis"
                        )
                    }
                    var tempStatusFilter by remember {
                        mutableStateOf(
                            if (selectedFilter.startsWith("Status: ")) selectedFilter.removePrefix("Status: ") else "Semua Status"
                        )
                    }

                    LunarisFilterDialog(
                        onDismissRequest = { showFilterMenu = false },
                        filterGroups = listOf(
                            FilterGroup(
                                title = "Ruang Laboratorium",
                                options = listOf("Semua Ruang") + ruangOptions,
                                selectedOption = tempRuangFilter,
                                onOptionSelected = { tempRuangFilter = it }
                            ),
                            FilterGroup(
                                title = "Jenis Perangkat",
                                options = listOf("Semua Jenis", "PC", "AIO", "Workstation", "Server", "Laptop"),
                                selectedOption = tempJenisFilter,
                                onOptionSelected = { tempJenisFilter = it }
                            ),
                            FilterGroup(
                                title = "Status / Kondisi",
                                options = listOf("Semua Status", "Baik / Normal", "Perlu Servis", "Dalam Perawatan"),
                                selectedOption = tempStatusFilter,
                                onOptionSelected = { tempStatusFilter = it }
                            )
                        ),
                        onReset = {
                            tempRuangFilter = "Semua Ruang"
                            tempJenisFilter = "Semua Jenis"
                            tempStatusFilter = "Semua Status"
                            selectedFilter = "Semua Filter"
                            showFilterMenu = false
                        },
                        onApply = {
                            selectedFilter = when {
                                tempRuangFilter != "Semua Ruang" -> "Ruang: $tempRuangFilter"
                                tempJenisFilter != "Semua Jenis" -> "Jenis: $tempJenisFilter"
                                tempStatusFilter != "Semua Status" -> "Status: $tempStatusFilter"
                                else -> "Semua Filter"
                            }
                            showFilterMenu = false
                        }
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Content Area
                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    color = Color(0xFFF3E8FF),
                                    shape = CircleShape,
                                    modifier = Modifier.size(64.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Computer,
                                            contentDescription = null,
                                            tint = Color(0xFF7C3AED),
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = "Belum Ada Unit PC",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Gunakan tombol di bawah untuk menambahkan unit PC baru dengan formulir 17 spesifikasi lengkap.",
                                    textAlign = TextAlign.Center,
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                Spacer(Modifier.height(16.dp))
                                if (canLabKomManage) {
                                    Button(
                                        onClick = {
                                            selectedUnitForEdit = null
                                            showAddPcDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Tambah Unit PC LabKom")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Multi-select header bar
                    if (canLabKomManage && filteredList.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isAllSelected = filteredList.isNotEmpty() && selectedUnitIds.size == filteredList.size
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    selectedUnitIds = if (isAllSelected) emptySet() else filteredList.map { it.id }.toSet()
                                }
                            ) {
                                Checkbox(
                                    checked = isAllSelected,
                                    onCheckedChange = { checked ->
                                        selectedUnitIds = if (checked) filteredList.map { it.id }.toSet() else emptySet()
                                    },
                                    modifier = Modifier.testTag("checkbox_select_all_labkom")
                                )
                                Text(
                                    text = if (isAllSelected) "Batal Pilih Semua" else "Pilih Semua (${filteredList.size})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (selectedUnitIds.isNotEmpty()) {
                                Button(
                                    onClick = { showBatchDeleteConfirmDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("btn_hapus_terpilih_labkom")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Hapus Terpilih (${selectedUnitIds.size})", fontWeight = FontWeight.Bold, fontSize = 11.sp)
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
                        items(filteredList, key = { it.id }) { pc ->
                            val isSelected = selectedUnitIds.contains(pc.id)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { selectedUnitForDetail = pc },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFFF3E8FF) else if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFFFFFF)),
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF7C3AED) else if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f) else Color(0xFFE9D5FF)
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
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (canLabKomManage) {
                                                Checkbox(
                                                    checked = isSelected,
                                                    onCheckedChange = { checked ->
                                                        selectedUnitIds = if (checked) selectedUnitIds + pc.id else selectedUnitIds - pc.id
                                                    },
                                                    modifier = Modifier.testTag("checkbox_pc_${pc.id}")
                                                )
                                            }
                                            Surface(
                                                color = Color(0xFFF3E8FF),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = pc.jenisPerangkat,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF7C3AED),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                            Text(
                                                text = pc.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Surface(
                                            color = if (pc.status.contains("Baik", ignoreCase = true) || pc.status.contains("Normal", ignoreCase = true)) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = pc.status,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (pc.status.contains("Baik", ignoreCase = true) || pc.status.contains("Normal", ignoreCase = true)) Color(0xFF15803D) else Color(0xFFB45309)
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "ID: ${pc.id} | Ruang: ${pc.labRoom}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF0284C7)
                                        )

                                        if (pc.serialNumber.isNotBlank()) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(Icons.Default.QrCode, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                                Text("SN: ${pc.serialNumber}", fontSize = 11.sp, color = Color.Gray)
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(6.dp))

                                    val hardwareDetail = listOfNotNull(
                                        pc.merek.ifBlank { null },
                                        pc.processor.ifBlank { null },
                                        "${pc.tipeRam} ${pc.kapasitasRam}".trim().ifBlank { null },
                                        "${pc.storage} ${pc.kapasitasStorage}".trim().ifBlank { null },
                                        pc.layarInch.ifBlank { null }
                                    ).joinToString(" • ")

                                    if (hardwareDetail.isNotBlank()) {
                                        Text(
                                            text = hardwareDetail,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (pc.sumberDana.isNotBlank() || pc.keterangan.isNotBlank()) {
                                        Spacer(Modifier.height(4.dp))
                                        val extraText = listOfNotNull(
                                            if (pc.sumberDana.isNotBlank()) "Sumber Dana: ${pc.sumberDana}" else null,
                                            if (pc.keterangan.isNotBlank()) "Ket: ${pc.keterangan}" else null
                                        ).joinToString(" | ")
                                        Text(
                                            text = extraText,
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                    }

                                    if (canLabKomManage) {
                                        Spacer(Modifier.height(2.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Edit Button
                                                IconButton(
                                                    onClick = {
                                                        selectedUnitForEdit = pc
                                                        showAddPcDialog = true
                                                    },
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .testTag("btn_edit_pc_${pc.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Edit Unit PC",
                                                        tint = Color(0xFF2563EB),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }

                                                // Pemeliharaan / Diagnosa Button
                                                IconButton(
                                                    onClick = {
                                                        selectedUnitForDiagnosis = pc
                                                        showDiagnosisDialog = true
                                                    },
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .testTag("btn_pemeliharaan_pc_${pc.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Build,
                                                        contentDescription = "Pemeliharaan & Diagnosa Unit PC",
                                                        tint = Color(0xFFD97706),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }

                                                // Single Delete Button
                                                IconButton(
                                                    onClick = {
                                                        unitToDelete = pc
                                                    },
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .testTag("btn_hapus_pc_${pc.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Hapus Unit PC",
                                                        tint = Color(0xFFDC2626),
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
    }

    // Add / Edit PC Unit Dialog
    if (showAddPcDialog) {
        AddEditPcUnitDialog(
            initialUnit = selectedUnitForEdit,
            viewModel = viewModel,
            existingCount = pcUnitsList.size,
            onDismissRequest = {
                showAddPcDialog = false
                selectedUnitForEdit = null
            },
            onSaveUnit = { savedUnit ->
                val fullKeterangan = encodePcSpecs(
                    processor = savedUnit.processor,
                    tipeRam = savedUnit.tipeRam,
                    kapasitasRam = savedUnit.kapasitasRam,
                    storage = savedUnit.storage,
                    kapasitasStorage = savedUnit.kapasitasStorage,
                    layarInch = savedUnit.layarInch,
                    keterangan = savedUnit.keterangan
                )
                if (selectedUnitForEdit != null) {
                    viewModel.updateItemDetails(
                        idBarang = savedUnit.id,
                        namaBarang = savedUnit.name,
                        kategori = savedUnit.jenisPerangkat,
                        satuan = savedUnit.satuan,
                        stokAwal = savedUnit.qty,
                        merekAlat = savedUnit.merek,
                        ruang = savedUnit.labRoom,
                        sumberDana = savedUnit.sumberDana.ifBlank { null },
                        kondisi = savedUnit.status,
                        keterangan = fullKeterangan,
                        serialNumber = savedUnit.serialNumber,
                        onSuccess = {
                            Toast.makeText(context, "Unit PC '${savedUnit.name}' berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                        },
                        onError = { err ->
                            Toast.makeText(context, "Gagal memperbarui: $err", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    viewModel.registerNewItem(
                        name = savedUnit.name,
                        serialNumber = savedUnit.serialNumber,
                        stokAwal = savedUnit.qty,
                        kategori = savedUnit.jenisPerangkat,
                        satuan = savedUnit.satuan,
                        merekAlat = savedUnit.merek,
                        ruang = savedUnit.labRoom,
                        sumberDana = savedUnit.sumberDana.ifBlank { null },
                        kondisi = savedUnit.status,
                        keterangan = fullKeterangan,
                        isBorrowable = false,
                        useAutoId = false,
                        customId = savedUnit.id,
                        type = "LABKOM",
                        onSuccess = {
                            Toast.makeText(context, "Unit PC '${savedUnit.name}' berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                        },
                        onError = { err ->
                            Toast.makeText(context, "Gagal menambahkan: $err", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                showAddPcDialog = false
                selectedUnitForEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
    unitToDelete?.let { unit ->
        AlertDialog(
            onDismissRequest = { unitToDelete = null },
            title = { Text("Konfirmasi Hapus Unit PC", fontWeight = FontWeight.Bold) },
            text = { Text("Apakah Anda yakin ingin menghapus unit '${unit.name}' (${unit.id}) secara permanen dari database lokal dan Firestore? Fitur ini diperuntukkan khusus bagi koreksi data input yang keliru.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteItem(
                            idBarang = unit.id,
                            onSuccess = {
                                Toast.makeText(context, "Unit PC '${unit.name}' telah dihapus", Toast.LENGTH_SHORT).show()
                                unitToDelete = null
                            },
                            onError = { err ->
                                Toast.makeText(context, "Gagal menghapus unit: $err", Toast.LENGTH_SHORT).show()
                                unitToDelete = null
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    modifier = Modifier.testTag("dialog_btn_confirm_delete_pc")
                ) {
                    Text("Hapus Unit")
                }
            },
            dismissButton = {
                TextButton(onClick = { unitToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Dialog Konfirmasi Hapus Massal (Batch Delete PC LabKom)
    if (showBatchDeleteConfirmDialog && selectedUnitIds.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteConfirmDialog = false },
            title = { Text("Konfirmasi Hapus ${selectedUnitIds.size} Unit PC", fontWeight = FontWeight.Bold) },
            text = {
                Text("Apakah Anda yakin ingin menghapus ${selectedUnitIds.size} unit PC terpilih secara permanen dari database lokal dan Firestore? Fitur ini diperuntukkan khusus bagi koreksi data input yang keliru (bulk import) dan tindakan ini tidak dapat dibatalkan.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val idsToDelete = selectedUnitIds.toList()
                        var deletedCount = 0
                        idsToDelete.forEach { id ->
                            viewModel.deleteItem(
                                idBarang = id,
                                onSuccess = { deletedCount++ },
                                onError = {}
                            )
                        }
                        Toast.makeText(context, "Berhasil menghapus $deletedCount unit PC!", Toast.LENGTH_SHORT).show()
                        selectedUnitIds = emptySet()
                        showBatchDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    modifier = Modifier.testTag("dialog_btn_confirm_batch_delete_labkom")
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

    // Detail PC Pop-up Dialog
    selectedUnitForDetail?.let { pc ->
        val inMaintenance = pc.status.contains("Servis", ignoreCase = true) ||
                pc.status.contains("Perawatan", ignoreCase = true) ||
                pc.status.contains("Pemeliharaan", ignoreCase = true) ||
                pc.status.contains("Perlu Servis", ignoreCase = true)

        AlertDialog(
            onDismissRequest = { selectedUnitForDetail = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Computer, contentDescription = null, tint = Color(0xFF7C3AED))
                    Column {
                        Text(pc.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("ID: ${pc.id} | ${pc.jenisPerangkat}", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Status Banner Notification
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
                                    text = if (inMaintenance) "Dalam Pemeliharaan" else "Kondisi Normal / Baik",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (inMaintenance) Color(0xFFB45309) else Color(0xFF15803D)
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (inMaintenance) "0 unit normal tersedia, 1 unit dalam perawatan" else "1 unit normal tersedia, 0 unit dalam perawatan",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (inMaintenance) Color(0xFFB45309) else Color(0xFF15803D)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Detail Specs
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Detail Spesifikasi & Lokasi:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        Text("• Lokasi Ruang: ${pc.labRoom}", fontSize = 12.sp)
                        if (pc.merek.isNotBlank()) Text("• Merek: ${pc.merek}", fontSize = 12.sp)
                        if (pc.processor.isNotBlank()) Text("• Processor: ${pc.processor}", fontSize = 12.sp)
                        if (pc.tipeRam.isNotBlank() || pc.kapasitasRam.isNotBlank()) Text("• RAM: ${pc.tipeRam} ${pc.kapasitasRam}".trim(), fontSize = 12.sp)
                        if (pc.storage.isNotBlank() || pc.kapasitasStorage.isNotBlank()) Text("• Storage: ${pc.storage} ${pc.kapasitasStorage}".trim(), fontSize = 12.sp)
                        if (pc.layarInch.isNotBlank()) Text("• Layar: ${pc.layarInch}", fontSize = 12.sp)
                        if (pc.serialNumber.isNotBlank()) Text("• Serial Number: ${pc.serialNumber}", fontSize = 12.sp)
                        if (pc.sumberDana.isNotBlank()) Text("• Sumber Dana: ${pc.sumberDana}", fontSize = 12.sp)
                        if (pc.keterangan.isNotBlank()) Text("• Keterangan: ${pc.keterangan}", fontSize = 12.sp)
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Riwayat Jejak Pemeliharaan & Diagnosa (Audit Trail Read-Only)
                    Text("Riwayat Jejak Audit & Diagnosa (Read-Only):", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
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
                                Text("• Status Perangkat PC:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                                Text(if (inMaintenance) "Perlu Servis / Pemeliharaan" else "Kondisi Baik & Siap Pakai", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (inMaintenance) Color(0xFFB45309) else Color(0xFF16A34A))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("• Catatan Inspeksi:", fontSize = 11.sp, color = Color(0xFF64748B))
                                Text(if (pc.keterangan.isNotBlank()) pc.keterangan else "Sistem & Hardware Normal", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1E293B))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("• Status Verifikasi:", fontSize = 11.sp, color = Color(0xFF64748B))
                                Text("Terverifikasi Tim Audit LabKom", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1E293B))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedUnitForDetail = null }) {
                    Text("Tutup", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Formulir Pop-up Diagnosa Dialog
    if (showDiagnosisDialog && selectedUnitForDiagnosis != null) {
        DiagnosisPcDialog(
            pc = selectedUnitForDiagnosis!!,
            defaultOfficerName = defaultOfficer,
            onDismissRequest = {
                showDiagnosisDialog = false
                selectedUnitForDiagnosis = null
            },
            onConfirmSave = { tanggal, jumlah, jenisDiagnosa, keterangan, namaPetugas ->
                val sdfTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US)
                val currentTime = sdfTime.format(java.util.Date())
                val isServisLuar = jenisDiagnosa.contains("Servis Luar", ignoreCase = true)
                val statusTarget = if (isServisLuar) "Servis Luar/Pemeliharaan" else "Pemeliharaan"

                viewModel.recordDamagedReport(
                    idBarang = selectedUnitForDiagnosis!!.id,
                    namaBarang = selectedUnitForDiagnosis!!.name,
                    jumlah = jumlah,
                    tanggalKerusakan = tanggal,
                    waktuKerusakan = currentTime,
                    keteranganKerusakan = keterangan,
                    namaPetugas = namaPetugas,
                    kondisiBaru = "Dalam Perawatan",
                    status = statusTarget,
                    onSuccess = {
                        Toast.makeText(context, "Data diagnosa berhasil disimpan ke menu Pemeliharaan!", Toast.LENGTH_LONG).show()
                        showDiagnosisDialog = false
                        selectedUnitForDiagnosis = null
                    },
                    onError = { err ->
                        Toast.makeText(context, "Gagal menyimpan diagnosa: $err", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPcUnitDialog(
    initialUnit: PcUnitData? = null,
    viewModel: InventoryViewModel,
    existingCount: Int,
    onDismissRequest: () -> Unit,
    onSaveUnit: (PcUnitData) -> Unit
) {
    val context = LocalContext.current

    // Collect Master Data from ViewModel
    val jenisPcList by viewModel.jenisPc.collectAsState()
    val merekList by viewModel.merekAlat.collectAsState()
    val tipeRamList by viewModel.tipeRam.collectAsState()
    val kapasitasRamList by viewModel.kapasitasRam.collectAsState()
    val storageList by viewModel.storage.collectAsState()
    val ruangList by viewModel.ruang.collectAsState()
    val kondisiList by viewModel.kondisi.collectAsState()
    val sumberDanaList by viewModel.sumberDana.collectAsState()
    val unitsList by viewModel.allUnits.collectAsState()

    // Fallbacks if master data is empty
    val jenisPcOptions = if (jenisPcList.isNotEmpty()) jenisPcList else listOf("PC", "AIO", "Mini PC", "Workstation", "Laptop Lab")
    val merekOptions = if (merekList.isNotEmpty()) merekList else listOf("Asus", "Lenovo", "Dell", "HP", "Acer", "MSI", "Custom / Rakitan")
    val tipeRamOptions = if (tipeRamList.isNotEmpty()) tipeRamList else listOf("DDR4", "DDR5", "DDR3", "LPDDR4", "LPDDR5")
    val kapasitasRamOptions = if (kapasitasRamList.isNotEmpty()) kapasitasRamList else listOf("4 GB", "8 GB", "16 GB", "32 GB", "64 GB")
    val storageOptions = if (storageList.isNotEmpty()) storageList else listOf("SSD NVMe", "SSD SATA", "HDD 3.5\"", "SSHD", "Dual Storage (SSD+HDD)")
    val ruangOptions = ruangList
    val kondisiOptions = if (kondisiList.isNotEmpty()) kondisiList else listOf("Normal / Baik", "Expired / Afkir", "Rusak", "Pemeliharaan", "Rusak Fisik")
    val sumberDanaOptions = if (sumberDanaList.isNotEmpty()) sumberDanaList else listOf("BOS", "BOPD", "Komite", "Hibah", "APBD")
    val satuanOptions = if (unitsList.isNotEmpty()) unitsList.map { it.name } else listOf("Unit", "Set", "Pcs", "Buah")

    // Form States (17 Fields)
    var jenisPerangkat by remember(initialUnit) { mutableStateOf(initialUnit?.jenisPerangkat ?: (jenisPcOptions.firstOrNull() ?: "PC")) }
    var serialNumber by remember(initialUnit) { mutableStateOf(initialUnit?.serialNumber ?: "") }
    var name by remember(initialUnit) { mutableStateOf(initialUnit?.name ?: "PC-LAB-${String.format(java.util.Locale.US, "%02d", existingCount + 1)}") }

    var isAutoId by remember(initialUnit) { mutableStateOf(initialUnit == null) }
    val autoIdGenerated = remember(existingCount) { "PC-LAB1-${String.format(java.util.Locale.US, "%03d", existingCount + 1)}" }
    var idText by remember(initialUnit) { mutableStateOf(initialUnit?.id ?: "") }

    var merek by remember(initialUnit) { mutableStateOf(initialUnit?.merek ?: (merekOptions.firstOrNull() ?: "Asus")) }
    var tipeRam by remember(initialUnit) { mutableStateOf(initialUnit?.tipeRam ?: (tipeRamOptions.firstOrNull() ?: "DDR4")) }
    var kapasitasRam by remember(initialUnit) { mutableStateOf(initialUnit?.kapasitasRam ?: (kapasitasRamOptions.firstOrNull() ?: "8 GB")) }
    var storage by remember(initialUnit) { mutableStateOf(initialUnit?.storage ?: (storageOptions.firstOrNull() ?: "SSD NVMe")) }
    var kapasitasStorage by remember(initialUnit) { mutableStateOf(initialUnit?.kapasitasStorage ?: (if (initialUnit == null) "256 GB" else "")) }
    var processor by remember(initialUnit) { mutableStateOf(initialUnit?.processor ?: "") }
    var layarInch by remember(initialUnit) { mutableStateOf(initialUnit?.layarInch ?: "") }

    var labRoom by remember(initialUnit) { mutableStateOf(initialUnit?.labRoom ?: (ruangOptions.firstOrNull() ?: "")) }
    var kondisi by remember(initialUnit) { mutableStateOf(initialUnit?.status ?: (kondisiOptions.firstOrNull() ?: "Normal / Baik")) }
    var sumberDana by remember(initialUnit) { mutableStateOf(initialUnit?.sumberDana ?: "") } // Default empty
    var qty by remember(initialUnit) { mutableStateOf(initialUnit?.qty?.toString() ?: "1") }
    var satuan by remember(initialUnit) { mutableStateOf(initialUnit?.satuan ?: (satuanOptions.firstOrNull() ?: "Unit")) }
    var keterangan by remember(initialUnit) { mutableStateOf(initialUnit?.keterangan ?: "") }

    var showCameraScanner by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Dialog Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF3E8FF))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = Color(0xFF7C3AED),
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (initialUnit == null) Icons.Default.Computer else Icons.Default.EditNote,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = if (initialUnit == null) "Tambah Unit PC LabKom" else "Edit Unit PC LabKom",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF5B21B6)
                            )
                            Text(
                                text = "Formulir inventarisasi 17 atribut spesifikasi unit PC",
                                fontSize = 11.sp,
                                color = Color(0xFF6B21A8)
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.testTag("btn_close_dialog_pc")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color(0xFF5B21B6))
                    }
                }

                // Scrollable Content Form
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // CARD 1: Identitas Perangkat
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    color = Color(0xFF7C3AED).copy(alpha = 0.1f),
                                    shape = CircleShape,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Computer, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(18.dp))
                                    }
                                }
                                Column {
                                    Text("1. Identitas Perangkat", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                    Text("Jenis, ID, Nama Perangkat, & Serial Number (SN)", fontSize = 10.5.sp, color = Color(0xFF64748B))
                                }
                            }

                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                            // 1. Jenis Perangkat & 4. ID Perangkat
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // 1. Jenis Perangkat
                                MasterDataDropdown(
                                    label = "Jenis Perangkat",
                                    selectedValue = jenisPerangkat,
                                    options = jenisPcOptions,
                                    onValueChange = { jenisPerangkat = it },
                                    modifier = Modifier.weight(1f),
                                    testTagStr = "input_jenis_perangkat"
                                )

                                // 4. ID Perangkat
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("ID Perangkat", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF334155))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Auto", fontSize = 10.sp, color = if (isAutoId) Color(0xFF7C3AED) else Color.Gray)
                                            Switch(
                                                checked = isAutoId,
                                                onCheckedChange = { isAutoId = it },
                                                modifier = Modifier
                                                    .scale(0.65f)
                                                    .height(20.dp)
                                            )
                                        }
                                    }
                                    OutlinedTextField(
                                        value = if (isAutoId) autoIdGenerated else idText,
                                        onValueChange = { if (!isAutoId) idText = it },
                                        readOnly = isAutoId,
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("input_id_perangkat"),
                                        placeholder = { Text("Contoh: PC-001", fontSize = 11.sp) }
                                    )
                                }
                            }

                            // 3. Nama Perangkat
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Nama Perangkat", fontSize = 11.sp) },
                                placeholder = { Text("Contoh: PC-LAB1-01 / Workstation Multimedia", fontSize = 11.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("input_nama_perangkat")
                            )

                            // 2. SN (Serial Number)
                            OutlinedTextField(
                                value = serialNumber,
                                onValueChange = { serialNumber = it },
                                label = { Text("SN (Serial Number)", fontSize = 11.sp) },
                                placeholder = { Text("Ketik manual / Scan QR-Barcode (Opsional)", fontSize = 11.sp) },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { showCameraScanner = true },
                                        modifier = Modifier
                                            .padding(end = 4.dp)
                                            .size(36.dp)
                                            .testTag("btn_scan_sn_pc")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.QrCodeScanner,
                                            contentDescription = "Pindai Kamera SN",
                                            tint = Color(0xFF7C3AED),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("input_sn_perangkat")
                            )
                        }
                    }

                    // CARD 2: Spesifikasi Hardware
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    color = Color(0xFF0284C7).copy(alpha = 0.1f),
                                    shape = CircleShape,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Memory, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(18.dp))
                                    }
                                }
                                Column {
                                    Text("2. Spesifikasi Hardware", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                    Text("Merek, Processor, RAM, Storage, & Layar", fontSize = 10.5.sp, color = Color(0xFF64748B))
                                }
                            }

                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                            // 5. Merek & 10. Processor
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // 5. Merek
                                MasterDataDropdown(
                                    label = "Merek Perangkat",
                                    selectedValue = merek,
                                    options = merekOptions,
                                    onValueChange = { merek = it },
                                    modifier = Modifier.weight(1f),
                                    testTagStr = "input_merek_pc"
                                )

                                // 10. Processor
                                OutlinedTextField(
                                    value = processor,
                                    onValueChange = { processor = it },
                                    label = { Text("Processor", fontSize = 11.sp) },
                                    placeholder = { Text("Core i5 / Ryzen 5 (Opsional)", fontSize = 11.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).testTag("input_processor_pc")
                                )
                            }

                            // 6. Tipe RAM & 7. Kapasitas RAM
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // 6. Tipe RAM
                                MasterDataDropdown(
                                    label = "Tipe RAM",
                                    selectedValue = tipeRam,
                                    options = tipeRamOptions,
                                    onValueChange = { tipeRam = it },
                                    modifier = Modifier.weight(1f),
                                    testTagStr = "input_tipe_ram_pc"
                                )

                                // 7. Kapasitas RAM
                                MasterDataDropdown(
                                    label = "Kapasitas RAM",
                                    selectedValue = kapasitasRam,
                                    options = kapasitasRamOptions,
                                    onValueChange = { kapasitasRam = it },
                                    modifier = Modifier.weight(1f),
                                    testTagStr = "input_kapasitas_ram_pc"
                                )
                            }

                            // 8. Storage & 9. Kapasitas Storage
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // 8. Storage
                                MasterDataDropdown(
                                    label = "Media Storage",
                                    selectedValue = storage,
                                    options = storageOptions,
                                    onValueChange = { storage = it },
                                    modifier = Modifier.weight(1f),
                                    testTagStr = "input_storage_pc"
                                )

                                // 9. Kapasitas Storage
                                OutlinedTextField(
                                    value = kapasitasStorage,
                                    onValueChange = { kapasitasStorage = it },
                                    label = { Text("Kapasitas Storage", fontSize = 11.sp) },
                                    placeholder = { Text("256 GB / 512 GB (Opsional)", fontSize = 11.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).testTag("input_kapasitas_storage_pc")
                                )
                            }

                            // 11. Layar dalam Inch
                            OutlinedTextField(
                                value = layarInch,
                                onValueChange = { layarInch = it },
                                label = { Text("Layar dalam Inch", fontSize = 11.sp) },
                                placeholder = { Text("Contoh: 24 Inch / 21.5 Inch (Opsional)", fontSize = 11.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("input_layar_inch_pc")
                            )
                        }
                    }

                    // CARD 3: Lokasi & Penempatan Inventory
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    color = Color(0xFF16A34A).copy(alpha = 0.1f),
                                    shape = CircleShape,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                                    }
                                }
                                Column {
                                    Text("3. Lokasi & Penempatan", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                    Text("Ruang, Kondisi, Kuantitas, Satuan, & Sumber Dana", fontSize = 10.5.sp, color = Color(0xFF64748B))
                                }
                            }

                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                            // 12. Ruang & 13. Kondisi
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // 12. Ruang
                                MasterDataDropdown(
                                    label = "Ruangan Lab",
                                    selectedValue = labRoom,
                                    options = ruangOptions,
                                    onValueChange = { labRoom = it },
                                    modifier = Modifier.weight(1f),
                                    testTagStr = "input_ruang_pc"
                                )

                                // 13. Kondisi
                                MasterDataDropdown(
                                    label = "Kondisi Unit",
                                    selectedValue = kondisi,
                                    options = kondisiOptions,
                                    onValueChange = { kondisi = it },
                                    modifier = Modifier.weight(1f),
                                    testTagStr = "input_kondisi_pc"
                                )
                            }

                            // 15. Qty & 16. Satuan
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // 15. Qty
                                OutlinedTextField(
                                    value = qty,
                                    onValueChange = { qty = it },
                                    label = { Text("Qty (Kuantitas)", fontSize = 11.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).testTag("input_qty_pc")
                                )

                                // 16. Satuan
                                MasterDataDropdown(
                                    label = "Satuan",
                                    selectedValue = satuan,
                                    options = satuanOptions,
                                    onValueChange = { satuan = it },
                                    modifier = Modifier.weight(1f),
                                    testTagStr = "input_satuan_pc"
                                )
                            }

                            // 14. Sumber Dana
                            MasterDataDropdown(
                                label = "Sumber Dana",
                                selectedValue = sumberDana,
                                options = sumberDanaOptions,
                                allowEmptyOption = true,
                                onValueChange = { sumberDana = it },
                                modifier = Modifier.fillMaxWidth(),
                                testTagStr = "input_sumber_dana_pc"
                            )
                        }
                    }

                    // CARD 4: Catatan & Keterangan
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    color = Color(0xFFD97706).copy(alpha = 0.1f),
                                    shape = CircleShape,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                                    }
                                }
                                Column {
                                    Text("4. Catatan & Keterangan", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                    Text("Catatan tambahan kondisi, kelengkapan, garansi, dll.", fontSize = 10.5.sp, color = Color(0xFF64748B))
                                }
                            }

                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                            // 17. Keterangan
                            OutlinedTextField(
                                value = keterangan,
                                onValueChange = { keterangan = it },
                                label = { Text("Keterangan Tambahan", fontSize = 11.sp) },
                                placeholder = { Text("Contoh: Kelengkapan kabel power & VGA lengkap, garansi s/d 2027 (Opsional)", fontSize = 11.sp) },
                                minLines = 2,
                                maxLines = 4,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("input_keterangan_pc")
                            )
                        }
                    }
                }

                // Bottom Sticky Action Bar
                Surface(
                    shadowElevation = 8.dp,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismissRequest,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("btn_cancel_pc"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, Color(0xFFCBD5E1))
                        ) {
                            Text(
                                text = "Batal",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF475569),
                                fontSize = 13.sp
                            )
                        }

                        Button(
                            onClick = {
                                val finalId = if (isAutoId) autoIdGenerated else idText.trim()
                                if (name.isBlank()) {
                                    Toast.makeText(context, "Nama Perangkat wajib diisi!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val unitResult = PcUnitData(
                                    id = if (finalId.isBlank()) "PC-${System.currentTimeMillis()}" else finalId,
                                    name = name.trim(),
                                    jenisPerangkat = jenisPerangkat.ifBlank { "PC" },
                                    serialNumber = serialNumber.trim(),
                                    merek = merek,
                                    processor = processor.trim(),
                                    tipeRam = tipeRam,
                                    kapasitasRam = kapasitasRam,
                                    storage = storage,
                                    kapasitasStorage = kapasitasStorage.trim(),
                                    layarInch = layarInch.trim(),
                                    labRoom = labRoom,
                                    status = kondisi,
                                    sumberDana = sumberDana,
                                    qty = qty.toIntOrNull() ?: 1,
                                    satuan = satuan.ifBlank { "Unit" },
                                    keterangan = keterangan.trim()
                                )
                                onSaveUnit(unitResult)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(46.dp)
                                .testTag("btn_simpan_unit_pc"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = if (initialUnit == null) Icons.Default.Save else Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (initialUnit == null) "Simpan Unit PC" else "Perbarui Unit PC",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCameraScanner) {
        PcSnCameraScannerDialog(
            onDismissRequest = { showCameraScanner = false },
            onSnScanned = { scannedCode ->
                serialNumber = scannedCode
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterDataDropdown(
    label: String,
    selectedValue: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Pilih $label",
    allowEmptyOption: Boolean = false,
    emptyOptionText: String = "— Tidak Dipilih / Kosong —",
    testTagStr: String = ""
) {
    var expanded by remember { mutableStateOf(false) }

    val effectiveOptions = remember(options, selectedValue) {
        if (selectedValue.isNotBlank() && !options.contains(selectedValue)) {
            listOf(selectedValue) + options
        } else {
            options
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = if (selectedValue.isBlank() && allowEmptyOption) emptyOptionText else selectedValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, fontSize = 11.sp) },
            placeholder = { Text(placeholder, fontSize = 11.sp) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
                .then(if (testTagStr.isNotEmpty()) Modifier.testTag(testTagStr) else Modifier)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (allowEmptyOption) {
                DropdownMenuItem(
                    text = { Text(emptyOptionText, fontSize = 12.sp, color = Color.Gray) },
                    onClick = {
                        onValueChange("")
                        expanded = false
                    }
                )
            }
            effectiveOptions.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            fontSize = 12.sp,
                            fontWeight = if (option == selectedValue) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PcSnCameraScannerDialog(
    onDismissRequest: () -> Unit,
    onSnScanned: (String) -> Unit
) {
    CameraScannerDialog(
        title = "Pindai Serial Number (SN) / Barcode PC",
        initialMode = ScanMode.PRIMARY_QR,
        onDismissRequest = onDismissRequest,
        onCodeScannedWithMode = { scannedCode, mode ->
            onSnScanned(scannedCode)
        }
    )
}

private fun parseCsvLine(line: String, delimiter: Char): List<String> {
    val tokens = mutableListOf<String>()
    val sb = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < line.length) {
        val c = line[i]
        if (c == '"') {
            if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                sb.append('"')
                i++
            } else {
                inQuotes = !inQuotes
            }
        } else if (c == delimiter && !inQuotes) {
            tokens.add(sb.toString().trim())
            sb.clear()
        } else {
            sb.append(c)
        }
        i++
    }
    tokens.add(sb.toString().trim())
    return tokens
}

private fun escapeCsv(value: String): String {
    if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains(";")) {
        return "\"${value.replace("\"", "\"\"")}\""
    }
    return value
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosisPcDialog(
    pc: PcUnitData,
    defaultOfficerName: String,
    onDismissRequest: () -> Unit,
    onConfirmSave: (tanggal: String, jumlah: Int, jenisDiagnosa: String, keterangan: String, namaPetugas: String) -> Unit
) {
    val context = LocalContext.current

    // 1. Tanggal Diagnosa (Default today yyyy-MM-dd)
    val sdfDate = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale("id", "ID")) }
    var tanggalInput by remember { mutableStateOf(sdfDate.format(java.util.Date())) }
    var showDatePicker by remember { mutableStateOf(false) }

    // 2. Jumlah Perawatan & Sisa Stok
    val totalStok = if (pc.qty > 0) pc.qty else 1
    var jumlahInput by remember { mutableStateOf("1") }
    val enteredJumlah = jumlahInput.toIntOrNull() ?: 0
    val sisaStok = (totalStok - enteredJumlah).coerceAtLeast(0)

    // 3. Satuan Otomatis
    val satuanAuto = pc.satuan.ifBlank { "Unit" }

    // 4. Opsi Jenis Diagnosa ("Pemeliharaan (Servis Internal)" vs "Servis Luar")
    var jenisDiagnosa by remember { mutableStateOf("Pemeliharaan (Servis Internal)") }

    // 5. Keterangan Diagnosa (Wajib)
    var keteranganInput by remember { mutableStateOf("") }

    // 6. Nama Petugas (Dinamis dari akun/profil aktif)
    var namaPetugasInput by remember(defaultOfficerName) {
        mutableStateOf(if (defaultOfficerName.isNotBlank()) defaultOfficerName else "Administrator")
    }

    if (showDatePicker) {
        LunarisDatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            selectedDateString = tanggalInput,
            onDateSelected = { newDate ->
                tanggalInput = newDate
            }
        )
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Banner
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = Color(0xFFD97706).copy(alpha = 0.12f),
                        shape = CircleShape,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Formulir Diagnosa & Pemeliharaan",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "${pc.name} (${pc.id.ifBlank { "PC Unit" }})",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                // Pure White Card 1: Tanggal & Jumlah
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "1. Informasi Tanggal & Jumlah Perawatan",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF334155)
                        )

                        // Tanggal Diagnosa Input Box with Picker
                        OutlinedTextField(
                            value = tanggalInput,
                            onValueChange = { tanggalInput = it },
                            label = { Text("Tanggal Diagnosa *", fontSize = 11.sp) },
                            placeholder = { Text("YYYY-MM-DD", fontSize = 11.sp) },
                            trailingIcon = {
                                IconButton(onClick = { showDatePicker = true }) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = "Pilih Tanggal",
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_tanggal_diagnosa")
                        )

                        // Jumlah Perawatan & Satuan Otomatis
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Input Jumlah
                            Column(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = jumlahInput,
                                    onValueChange = { input ->
                                        if (input.isEmpty() || input.all { it.isDigit() }) {
                                            jumlahInput = input
                                        }
                                    },
                                    label = { Text("Jumlah Unit *", fontSize = 11.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_jumlah_diagnosa")
                                )

                                // Real-time remaining stock status text
                                Text(
                                    text = "Sisa stok: $sisaStok $satuanAuto (dari $totalStok)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (enteredJumlah > totalStok) Color.Red else Color(0xFF16A34A),
                                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                                )
                            }

                            // Satuan Otomatis (Read-only)
                            OutlinedTextField(
                                value = satuanAuto,
                                onValueChange = {},
                                readOnly = true,
                                enabled = false,
                                label = { Text("Satuan", fontSize = 11.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(0.8f)
                                    .testTag("input_satuan_diagnosa")
                            )
                        }
                    }
                }

                // Pure White Card 2: Opsi Jenis Diagnosa
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "2. Jenis Diagnosa / Tindakan",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF334155)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "Pemeliharaan (Servis Internal)" to "Tab Pemeliharaan",
                                "Servis Luar" to "Tab Servis Luar"
                            ).forEach { (optionKey, tabDesc) ->
                                val isSelected = jenisDiagnosa == optionKey
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) Color(0xFFD97706) else Color(0xFFE2E8F0),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { jenisDiagnosa = optionKey },
                                    color = if (isSelected) Color(0xFFFFFBEB) else Color.White
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { jenisDiagnosa = optionKey },
                                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFD97706)),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text(
                                                text = if (optionKey.contains("Internal")) "Pemeliharaan" else "Servis Luar",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 11.5.sp,
                                                color = if (isSelected) Color(0xFF92400E) else Color(0xFF1E293B),
                                                maxLines = 1
                                            )
                                            Text(
                                                text = tabDesc,
                                                fontSize = 9.5.sp,
                                                color = Color(0xFF64748B),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Pure White Card 3: Keterangan & Petugas
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "3. Keterangan & Penanggung Jawab",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF334155)
                        )

                        // Keterangan Diagnosa (Wajib / Mandatory)
                        OutlinedTextField(
                            value = keteranganInput,
                            onValueChange = { keteranganInput = it },
                            label = { Text("Keterangan Diagnosa / Kendala *", fontSize = 11.sp) },
                            placeholder = { Text("Catat detail kendala, kerusakan, atau tindakan servis...", fontSize = 11.sp) },
                            minLines = 2,
                            maxLines = 4,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_keterangan_diagnosa")
                        )

                        // Nama Petugas (Otomatis dari Profil)
                        OutlinedTextField(
                            value = namaPetugasInput,
                            onValueChange = { namaPetugasInput = it },
                            label = { Text("Nama Petugas / Penanggung Jawab *", fontSize = 11.sp) },
                            placeholder = { Text("Nama Petugas LabKom", fontSize = 11.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_petugas_diagnosa")
                        )
                    }
                }

                // Balanced Action Buttons (Batal & Simpan)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("btn_batal_diagnosa"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, Color(0xFFCBD5E1))
                    ) {
                        Text(
                            text = "Batal",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569),
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = {
                            if (keteranganInput.isBlank()) {
                                Toast.makeText(context, "Keterangan diagnosa tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (enteredJumlah <= 0) {
                                Toast.makeText(context, "Jumlah unit harus lebih dari 0!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            onConfirmSave(
                                tanggalInput,
                                enteredJumlah,
                                jenisDiagnosa,
                                keteranganInput.trim(),
                                namaPetugasInput.ifBlank { "Laboran Komputer" }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(46.dp)
                            .testTag("btn_simpan_diagnosa"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Simpan Diagnosa",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
