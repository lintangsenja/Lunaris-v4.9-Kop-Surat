package com.example.ui.screens
import com.example.data.entity.KopLaporanEntity
import com.example.data.entity.parseKopRowOrder
import com.example.data.entity.parseTtdSigners
import com.example.data.entity.TtdSignerItem
import com.example.ui.components.LunarisCard
import com.example.ui.components.CameraScannerDialog
import com.example.ui.components.LunarisDatePickerDialog

import android.app.DatePickerDialog
import android.content.Context
import android.os.Environment
import android.os.Build
import java.io.File
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.LoanItemEntity
import com.example.data.entity.MutasiPerangkatEntity
import com.example.data.model.ItemWithStock
import com.example.data.model.ReportStats
import com.example.data.model.ReportDetailItem
import kotlinx.coroutines.flow.flowOf
import com.example.ui.theme.DeepPurpleText
import com.example.ui.theme.PastelLavender
import com.example.ui.viewmodel.InventoryViewModel
import com.example.ui.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class BorrowedLineItem(
    val namaPeminjam: String,
    val kelas: String,
    val namaBarang: String,
    val jumlah: Int,
    val tanggal: String,
    val petugas: String,
    val status: String,
    val whatsappNumber: String?,
    val idTransaksi: String,
    val tujuanPeminjaman: String? = null,
    val detailTujuan: String? = null
)

data class ReturnedLineItem(
    val namaPeminjam: String,
    val kelas: String,
    val namaBarang: String,
    val jumlah: Int,
    val tanggalKembali: String,
    val petugasKembali: String,
    val tanggalPinjam: String
)

@OptIn(ExperimentalMaterial3Api::class, com.google.accompanist.permissions.ExperimentalPermissionsApi::class)
@Composable
fun LaporanScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToKopLaporan: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Real-time Data from Room
    val kopState by viewModel.kopLaporan.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val itemsWithStock by viewModel.itemsWithStock.collectAsState()
    val totalStok by viewModel.totalStok.collectAsState()
    val damagedItems by viewModel.allDamagedItems.collectAsState()
    val maintenanceItems by viewModel.maintenanceItems.collectAsState()
    val pemakaianBahanList by viewModel.allPemakaianBahan.collectAsState()
    val bahanAfkirList by viewModel.allBahanAfkir.collectAsState()
    val hapusAsetPeripheralItems by viewModel.hapusAsetPeripheralItems.collectAsState()
    val servisLuarItems by viewModel.servisLuarItems.collectAsState()
    val mutasiList by viewModel.mutasiPerangkatList.collectAsState()
    val namaPetugasState by viewModel.defaultOfficer.collectAsState()
    val namaPetugas = namaPetugasState.ifBlank { "Administrator" }

    val masterRuangList by viewModel.ruang.collectAsState()
    val masterRooms = remember(itemsWithStock, masterRuangList) {
        (masterRuangList + itemsWithStock.map { it.ruang.ifBlank { "Lainnya" } }).distinct().filter { it.isNotBlank() }.sorted()
    }

    var selectedTabState by remember { mutableStateOf(0) }
    var isExporting by remember { mutableStateOf(false) }

    var showExportSuccessDialog by remember { mutableStateOf(false) }
    var showExportFormatDialog by remember { mutableStateOf(false) }
    var successFilename by remember { mutableStateOf("") }
    var successFileMimeType by remember { mutableStateOf("") }
    var successFileUri by remember { mutableStateOf<android.net.Uri?>(null) }

    // Date Picker States
    val calendarWib = remember {
        Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"), Locale("id", "ID"))
    }
    
    val vmStartDate by viewModel.startDateText.collectAsState()
    val vmEndDate by viewModel.endDateText.collectAsState()

    var startDateText by remember(vmStartDate) { mutableStateOf(vmStartDate) }
    var endDateText by remember(vmEndDate) { mutableStateOf(vmEndDate) }

    var refreshTrigger by remember { mutableStateOf(0) }
    var selectedStatus by remember { mutableStateOf<String?>(null) }

    val userRole by viewModel.userRole.collectAsState()
    val studentPermissions by viewModel.studentPermissions.collectAsState()

    fun isTabAllowed(index: Int): Boolean {
        if (!userRole.contains("siswa", ignoreCase = true)) return true
        if (!viewModel.isStudentPermissionGranted("laporan")) return false
        val key = when(index) {
            0 -> "laporan_ringkasan"
            1 -> "laporan_alat"
            2 -> "laporan_pemakaian_afkir"
            3 -> "laporan_penghapusan"
            4 -> "laporan_peripheral"
            5 -> "laporan_labkom"
            6 -> "laporan_mutasi"
            7 -> "laporan_pemeliharaan"
            8 -> "laporan_log_aktivitas"
            9 -> "laporan_sirkulasi"
            else -> "laporan_ringkasan"
        }
        return viewModel.isStudentPermissionGranted(key)
    }

    val canExportExcel = !userRole.contains("siswa", ignoreCase = true) || viewModel.isStudentPermissionGranted("laporan_export_excel")
    val canPrintPdf = !userRole.contains("siswa", ignoreCase = true) || viewModel.isStudentPermissionGranted("laporan_print_pdf")
    val canKopLaporan = viewModel.isStudentPermissionGranted("kop_laporan")

    LaunchedEffect(userRole, studentPermissions) {
        if (userRole.contains("siswa", ignoreCase = true) && !isTabAllowed(selectedTabState)) {
            val nextAllowed = (0..9).firstOrNull { isTabAllowed(it) }
            if (nextAllowed != null) {
                selectedTabState = nextAllowed
            }
        }
    }

    val reportStats by remember(startDateText, endDateText) {
        viewModel.fetchReportStats(startDateText, endDateText)
    }.collectAsState(initial = null)

    LaunchedEffect(startDateText, endDateText) {
        android.util.Log.d("LaporanScreen", "Date range updated to $startDateText ... $endDateText. Force-refreshing all database observation flows simultaneously.")
        selectedStatus = null
        refreshTrigger++
    }

    // Keep cache of loan items details per transaction
    val itemsCache = remember { mutableStateMapOf<String, List<LoanItemEntity>>() }

    // Preload item details on display
    LaunchedEffect(transactions) {
        transactions.forEach { tx ->
            if (!itemsCache.containsKey(tx.idTransaksi)) {
                val list = viewModel.getItemsForTransaction(tx.idTransaksi)
                itemsCache[tx.idTransaksi] = list
            }
        }
    }

    // Dynamic filtering based on date range
    val filteredBorrowed = remember(transactions, itemsCache, startDateText, endDateText, refreshTrigger) {
        val list = mutableListOf<BorrowedLineItem>()
        transactions.forEach { tx ->
            if (tx.tanggal >= startDateText && tx.tanggal <= endDateText) {
                val lines = itemsCache[tx.idTransaksi] ?: emptyList()
                lines.forEach { item ->
                    list.add(
                        BorrowedLineItem(
                            namaPeminjam = tx.namaPeminjam,
                            kelas = tx.kelas,
                            namaBarang = item.namaBarang,
                            jumlah = item.jumlah,
                            tanggal = tx.tanggal,
                            petugas = tx.namaPetugas,
                            status = tx.status,
                            whatsappNumber = tx.whatsappNumber,
                            idTransaksi = tx.idTransaksi,
                            tujuanPeminjaman = tx.tujuanPeminjaman,
                            detailTujuan = tx.detailTujuan
                        )
                    )
                }
            }
        }
        list.sortedByDescending { it.tanggal }
    }

    // Acuan mingguan statis dihapus agar grafik dinamis menggunakan range tanggal yang dipilih

    val filteredReturned = remember(transactions, itemsCache, startDateText, endDateText, refreshTrigger) {
        val list = mutableListOf<ReturnedLineItem>()
        transactions.filter { it.status == "Kembali" }.forEach { tx ->
            val tglKembali = tx.tanggalKembali ?: ""
            if (tglKembali >= startDateText && tglKembali <= endDateText) {
                val lines = itemsCache[tx.idTransaksi] ?: emptyList()
                lines.forEach { item ->
                    list.add(
                        ReturnedLineItem(
                            namaPeminjam = tx.namaPeminjam,
                            kelas = tx.kelas,
                            namaBarang = item.namaBarang,
                            jumlah = item.jumlah,
                            tanggalKembali = tglKembali,
                            petugasKembali = tx.petugasKembali ?: "-",
                            tanggalPinjam = tx.tanggal
                        )
                    )
                }
            }
        }
        list.sortedByDescending { it.tanggalKembali }
    }

    val filteredDamaged = remember(damagedItems, startDateText, endDateText, refreshTrigger) {
        damagedItems.filter { 
            it.tanggalKerusakan >= startDateText && 
            it.tanggalKerusakan <= endDateText && 
            (it.status == "Rusak (Perlu Tindakan)" || it.status.isBlank() || it.status == "Rusak") 
        }.sortedByDescending { it.tanggalKerusakan }
    }

    val filteredMaintenance = remember(maintenanceItems, startDateText, endDateText, refreshTrigger) {
        val filtered = maintenanceItems.filter { 
            it.tanggalKerusakan >= startDateText && 
            it.tanggalKerusakan <= endDateText
        }.sortedByDescending { it.tanggalKerusakan }
        
        if (filtered.isEmpty()) {
            android.util.Log.w("LaporanScreen", "No maintenance records found in range $startDateText to $endDateText. Unfiltered maintenance count: ${maintenanceItems.size}")
        } else {
            android.util.Log.d("LaporanScreen", "Found ${filtered.size} maintenance records in range $startDateText to $endDateText")
        }
        filtered
    }

    val filteredPemakaian = remember(pemakaianBahanList, startDateText, endDateText, refreshTrigger) {
        pemakaianBahanList.filter { it.tanggalPemakaian >= startDateText && it.tanggalPemakaian <= endDateText }
            .sortedByDescending { it.tanggalPemakaian }
    }

    val filteredAfkir = remember(bahanAfkirList, startDateText, endDateText, refreshTrigger) {
        bahanAfkirList.filter { it.tanggalAfkir >= startDateText && it.tanggalAfkir <= endDateText }
            .sortedByDescending { it.tanggalAfkir }
    }

    val storagePermissionState = com.google.accompanist.permissions.rememberPermissionState(
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    val doActualExport: (String) -> Unit = { format ->
        scope.launch {
            isExporting = true

            val dateStr = SimpleDateFormat("dd_MM_yyyy", Locale("id", "ID")).format(Date())
            val folderName = when (selectedTabState) {
                0 -> "Ringkasan Full"
                1 -> "Laporan Alat"
                2 -> "Riwayat Pemakaian dan Afkir Bahan"
                3 -> "Laporan Penghapusan Aset"
                4 -> "Peripheral dan Stok"
                5 -> "LabKom dan PC"
                6 -> "Mutasi Perangkat"
                7 -> "Pemeliharaan dan Servis Luar"
                8 -> "Log Aktivitas dan Manajemen Stok"
                else -> "Sirkulasi Peminjaman"
            }

            val filename = "Laporan_${folderName.replace(" ", "_")}_$dateStr.${if (format == "Excel") "xlsx" else if (format == "Word" || format == "DOCX") "docx" else "pdf"}"

            val title = when (selectedTabState) {
                0 -> "LAPORAN RINGKASAN FULL INVENTARIS - LUNARIS"
                1 -> "LAPORAN ALAT & ASET - LUNARIS"
                2 -> "LAPORAN RIWAYAT PEMAKAIAN & AFKIR BAHAN - LUNARIS"
                3 -> "LAPORAN PENGHAPUSAN ASET - LUNARIS"
                4 -> "LAPORAN STOK PERIPHERAL - LUNARIS"
                5 -> "LAPORAN LABKOM & PC - LUNARIS"
                6 -> "LAPORAN MUTASI PERANGKAT - LUNARIS"
                7 -> "LAPORAN PEMELIHARAAN & SERVIS - LUNARIS"
                8 -> "LOG AKTIVITAS & MANAJEMEN STOK - LUNARIS"
                else -> "LAPORAN SIRKULASI PEMINJAMAN - LUNARIS"
            }

            val (headers, rows) = when (selectedTabState) {
                0 -> {
                    val h = listOf("No", "ID Barang", "Nama Barang", "Kategori", "Stok Tersedia", "Satuan", "Kondisi", "Lokasi/Ruang")
                    val r = itemsWithStock.mapIndexed { idx, it ->
                        listOf(
                            (idx + 1).toString(),
                            it.idBarang,
                            it.namaBarang,
                            it.kategori,
                            it.stokTersedia.toString(),
                            it.satuan.ifBlank { "Pcs" },
                            it.kondisi,
                            it.ruang
                        )
                    }
                    Pair(h, r)
                }
                1 -> {
                    val h = listOf("No", "ID Barang", "Nama Alat", "Kategori", "Stok Tersedia", "Satuan", "Kondisi", "Ruang/Lokasi")
                    val toolsOnly = itemsWithStock.filter { !it.kategori.equals("Logistik", ignoreCase = true) && !it.kategori.contains("Bahan", ignoreCase = true) }
                    val r = toolsOnly.mapIndexed { idx, it ->
                        listOf(
                            (idx + 1).toString(),
                            it.idBarang,
                            it.namaBarang,
                            it.kategori,
                            it.stokTersedia.toString(),
                            it.satuan.ifBlank { "Unit" },
                            it.kondisi,
                            it.ruang
                        )
                    }
                    Pair(h, r)
                }
                2 -> {
                    val h = listOf("No", "Tipe Aktivitas", "Nama Bahan", "Jumlah", "Satuan", "Peminta / Alasan", "Petugas", "Tanggal", "Keterangan")
                    val r = mutableListOf<List<String>>()
                    var counter = 1
                    filteredPemakaian.forEach {
                        r.add(listOf((counter++).toString(), "Pemakaian Bahan", it.namaBarang, it.jumlahDiambil.toString(), it.satuan, it.namaPeminta, it.namaPetugas, it.tanggalPemakaian, it.keterangan.ifBlank { "-" }))
                    }
                    filteredAfkir.forEach {
                        r.add(listOf((counter++).toString(), "Afkir Bahan", it.namaBarang, it.jumlahAfkir.toString(), it.satuan, it.alasan, "-", it.tanggalAfkir, "Bahan Afkir"))
                    }
                    Pair(h, r)
                }
                3 -> {
                    val h = listOf("No", "Tipe Aset", "Nama Barang", "Jumlah", "Satuan", "Status / Action", "Petugas", "Tanggal", "Keterangan / Catatan")
                    val r = mutableListOf<List<String>>()
                    var counter = 1
                    damagedItems.filter { it.isHibah || it.status.contains("Hapus", ignoreCase = true) || it.status.contains("Hibah", ignoreCase = true) }.forEach {
                        r.add(listOf((counter++).toString(), "Alat / Aset", it.namaBarang, it.jumlah.toString(), "Unit", if (it.isHibah) "Hibah Alat" else "Penghapusan Alat", it.namaPetugas, it.tanggalKerusakan, it.keteranganKerusakan))
                    }
                    bahanAfkirList.forEach {
                        r.add(listOf((counter++).toString(), "Bahan Practicum", it.namaBarang, it.jumlahAfkir.toString(), it.satuan, "Afkir / Penghapusan Bahan", "-", it.tanggalAfkir, it.alasan))
                    }
                    hapusAsetPeripheralItems.forEach {
                        r.add(listOf((counter++).toString(), "Peripheral", it.namaBarang, it.jumlah.toString(), "Unit", if (it.isHibah) "Hibah Peripheral" else "Penghapusan Peripheral", "-", "-", it.keteranganKerusakan))
                    }
                    Pair(h, r)
                }
                4 -> {
                    val h = listOf("No", "ID Barang", "Nama Peripheral", "Stok Ready", "Satuan", "Kondisi", "Ruang/Lokasi")
                    val peripherals = itemsWithStock.filter { 
                        it.kategori.contains("Peripheral", ignoreCase = true) || 
                        it.type == "PERIPHERAL" || 
                        listOf("Mouse", "Keyboard", "Monitor", "Kabel", "Headset", "Switch", "Hub", "RAM", "SSD").any { p -> it.namaBarang.contains(p, ignoreCase = true) } 
                    }
                    val r = peripherals.mapIndexed { idx, it ->
                        listOf(
                            (idx + 1).toString(),
                            it.idBarang,
                            it.namaBarang,
                            it.stokTersedia.toString(),
                            it.satuan.ifBlank { "Pcs" },
                            it.kondisi,
                            it.ruang
                        )
                    }
                    Pair(h, r)
                }
                5 -> {
                    val h = listOf("No", "ID Unit", "Nama PC / Komputer", "Kategori", "Stok Ready", "Kondisi", "Ruang Lab")
                    val labkom = itemsWithStock.filter { 
                        it.kategori.contains("LabKom", ignoreCase = true) || 
                        it.ruang.contains("LabKom", ignoreCase = true) || 
                        it.namaBarang.contains("PC", ignoreCase = true) || 
                        it.namaBarang.contains("Komputer", ignoreCase = true) ||
                        it.type == "LABKOM"
                    }
                    val r = labkom.mapIndexed { idx, it ->
                        listOf(
                            (idx + 1).toString(),
                            it.idBarang,
                            it.namaBarang,
                            it.kategori,
                            it.stokTersedia.toString(),
                            it.kondisi,
                            it.ruang
                        )
                    }
                    Pair(h, r)
                }
                6 -> {
                    val h = listOf("No", "Nama Perangkat", "Jenis Perangkat", "Ruang Asal", "Ruang Tujuan", "Petugas", "Tanggal Mutasi", "Alasan & Keterangan")
                    val r = mutasiList.mapIndexed { idx, it ->
                        listOf(
                            (idx + 1).toString(),
                            it.namaBarang,
                            it.jenisPerangkat,
                            it.ruangAsal,
                            it.ruangTujuan,
                            it.namaPetugas,
                            it.tanggalMutasi,
                            "Alasan: ${it.alasanMutasi}. Ket: ${it.keterangan}"
                        )
                    }
                    Pair(h, r)
                }
                7 -> {
                    val h = listOf("No", "Kategori Servis", "Nama Perangkat", "Jumlah", "Satuan", "Petugas / Vendor", "Tanggal", "Tindakan & Catatan")
                    val r = mutableListOf<List<String>>()
                    var counter = 1
                    filteredMaintenance.forEach {
                        r.add(listOf((counter++).toString(), "Pemeliharaan Berkala", it.namaBarang, it.jumlah.toString(), "Unit", it.namaPetugas, it.tanggalKerusakan, "Ket: ${it.keteranganKerusakan}. Catatan: ${it.statusKeterangan}"))
                    }
                    servisLuarItems.forEach {
                        r.add(listOf((counter++).toString(), "Servis Luar (Vendor)", it.namaBarang, it.jumlah.toString(), "Unit", it.namaPetugas, it.tanggalKerusakan, "Keterangan: ${it.keteranganKerusakan}"))
                    }
                    Pair(h, r)
                }
                8 -> {
                    val h = listOf("No", "Tipe Aktivitas", "Subjek / Barang", "Jumlah", "Satuan", "Peminjam / Peminta", "Petugas", "Tanggal", "Keterangan / Status")
                    val r = mutableListOf<List<String>>()
                    var counter = 1
                    transactions.forEach {
                        r.add(listOf((counter++).toString(), "Sirkulasi Peminjaman", "Tx #${it.idTransaksi}", "1", "Transaksi", it.namaPeminjam, it.namaPetugas, it.tanggal, "Status: ${it.status}"))
                    }
                    filteredPemakaian.forEach {
                        r.add(listOf((counter++).toString(), "Pemakaian Bahan", it.namaBarang, it.jumlahDiambil.toString(), it.satuan, it.namaPeminta, it.namaPetugas, it.tanggalPemakaian, it.keterangan))
                    }
                    filteredAfkir.forEach {
                        r.add(listOf((counter++).toString(), "Afkir Bahan", it.namaBarang, it.jumlahAfkir.toString(), it.satuan, "-", "-", it.tanggalAfkir, it.alasan))
                    }
                    Pair(h, r)
                }
                else -> {
                    val h = listOf("No", "Status Transaksi", "Nama Barang", "Jumlah", "Satuan", "Peminjam (Kelas)", "Petugas", "Tanggal", "Keterangan")
                    val r = mutableListOf<List<String>>()
                    var counter = 1
                    filteredBorrowed.forEach {
                        r.add(listOf((counter++).toString(), "Dipinjam (${it.status})", it.namaBarang, it.jumlah.toString(), "Pcs", "${it.namaPeminjam} (${it.kelas})", it.petugas, it.tanggal, "Peminjaman Aktif"))
                    }
                    filteredReturned.forEach {
                        r.add(listOf((counter++).toString(), "Dikembalikan", it.namaBarang, it.jumlah.toString(), "Pcs", "${it.namaPeminjam} (${it.kelas})", it.petugasKembali, it.tanggalKembali, "Pinjam: ${it.tanggalPinjam}"))
                    }
                    Pair(h, r)
                }
            }

            val bytes = when (format) {
                "Word", "DOCX" -> generateWordBytes(title, "$startDateText s/d $endDateText", headers, rows, kopState)
                "Excel" -> generateExcelBytes(title, headers, rows, kopState)
                else -> generatePdfBytes(context, title, "$startDateText s/d $endDateText", headers, rows, kopState)
            }

            val mimeType = when (format) {
                "Word", "DOCX" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                "Excel" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                else -> "application/pdf"
            }

            val exportResult = com.example.utils.LunarisStorageHelper.saveExportFile(
                context = context,
                subfolderName = folderName,
                filename = filename,
                bytes = bytes,
                mimeType = mimeType
            )

            if (exportResult != null) {
                successFilename = exportResult.filename
                successFileMimeType = exportResult.mimeType
                successFileUri = exportResult.uri
                
                Toast.makeText(context, "Laporan berhasil disimpan di:\n${exportResult.displayPath}${exportResult.filename}", Toast.LENGTH_LONG).show()
                showExportSuccessDialog = true
                
                // Automatically attempt opening exported document
                com.example.utils.LunarisStorageHelper.openFile(context, exportResult.uri, exportResult.mimeType)
            } else {
                Toast.makeText(context, "Gagal mengekspor laporan!", Toast.LENGTH_SHORT).show()
            }
            isExporting = false
        }
    }

    val handleExport: (String) -> Unit = { format ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            doActualExport(format)
        } else {
            if (storagePermissionState.status.isGranted) {
                doActualExport(format)
            } else {
                Toast.makeText(context, "Memerlukan izin penyimpanan untuk menyimpan laporan", Toast.LENGTH_SHORT).show()
                storagePermissionState.launchPermissionRequest()
            }
        }
    }

    // Date Picker Dialog Helpers
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    if (showStartPicker) {
        LunarisDatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            selectedDateString = startDateText,
            onDateSelected = { dateStr ->
                startDateText = dateStr
                viewModel.updateDateFilter(dateStr, endDateText)
            }
        )
    }

    if (showEndPicker) {
        LunarisDatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            selectedDateString = endDateText,
            onDateSelected = { dateStr ->
                endDateText = dateStr
                viewModel.updateDateFilter(startDateText, dateStr)
            }
        )
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
            colors = listOf(Color(0xFFF3E8FF), Color(0xFFDDD6FE))
        )
    }
    val appBarContentColor = if (isDark) MaterialTheme.colorScheme.onSurface else DeepPurpleText
    val selectedTabColor = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText
    val unselectedTabColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else Color.Gray.copy(alpha = 0.8f)
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
                                    androidx.compose.ui.graphics.Color(0xFFF3E8FF),
                                    androidx.compose.ui.graphics.Color(0xFFDDD6FE)
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Lunaris Reporting Analytics",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = androidx.compose.ui.graphics.Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Analisis terpadu dan rekapitulasi laporan inventaris sarpras",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = androidx.compose.ui.graphics.Color(0xFF1E293B)
                                )
                            }
                            if (canKopLaporan) {
                                IconButton(
                                    onClick = onNavigateToKopLaporan,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .testTag("kop_laporan_action_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = "Pengaturan Kop Laporan",
                                        tint = androidx.compose.ui.graphics.Color(0xFF6D28D9),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
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
                    ScrollableTabRow(
                        selectedTabIndex = selectedTabState,
                        containerColor = Color.Transparent,
                        contentColor = selectedTabColor,
                        edgePadding = 16.dp,
                        divider = {},
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabState]),
                                height = 3.dp,
                                color = selectedTabColor
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val tabs = listOf(
                            "📊 Ringkasan Full" to "laporan_tab_summary",
                            "🔧 Laporan Alat" to "laporan_tab_alat",
                            "🧪 Pemakaian & Afkir Bahan" to "laporan_tab_pemakaian_afkir",
                            "🗑️ Penghapusan Aset" to "laporan_tab_penghapusan",
                            "🔌 Peripheral & Stok" to "laporan_tab_peripheral",
                            "💻 LabKom & PC" to "laporan_tab_labkom",
                            "📦 Mutasi Perangkat" to "laporan_tab_mutasi",
                            "🛠️ Pemeliharaan & Servis Luar" to "laporan_tab_pemeliharaan",
                            "📜 Log Aktivitas & Stok" to "laporan_tab_log_aktivitas",
                            "🔄 Sirkulasi Peminjaman" to "laporan_tab_sirkulasi"
                        )
                        tabs.forEachIndexed { index, (title, tag) ->
                            val allowed = isTabAllowed(index)
                            Tab(
                                selected = selectedTabState == index,
                                onClick = {
                                    if (allowed) {
                                        selectedTabState = index
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Sub-menu '$title' terkunci untuk akun Siswa",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                selectedContentColor = selectedTabColor,
                                unselectedContentColor = if (allowed) unselectedTabColor else Color.LightGray,
                                text = { 
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (!allowed) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = "Terkunci",
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                        Text(
                                            text = title, 
                                            fontWeight = if (selectedTabState == index) FontWeight.Bold else FontWeight.Medium, 
                                            fontSize = 15.sp,
                                            color = if (!allowed) Color.Gray.copy(alpha = 0.6f) else Color.Unspecified
                                        )
                                    }
                                },
                                modifier = Modifier.testTag(tag)
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showExportFormatDialog = true },
                containerColor = Color(0xFF7C3AED),
                contentColor = Color.White,
                shape = androidx.compose.foundation.shape.CircleShape,
                modifier = Modifier
                    .padding(bottom = 76.dp, end = 4.dp)
                    .testTag("btn_export_laporan_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Print,
                    contentDescription = "Cetak / Ekspor Laporan",
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // SUB-MENU REPORT ACTION HEADER BAR WITH CATEGORY TITLE
                val currentSubMenuTitle = when (selectedTabState) {
                    0 -> "Ringkasan Full Inventaris"
                    1 -> "Laporan Alat & Aset"
                    2 -> "Laporan Bahan & Pemakaian"
                    3 -> "Laporan Afkir & Alat Rusak"
                    4 -> "Laporan Stok Peripheral"
                    5 -> "Laporan LabKom & PC"
                    6 -> "Laporan Mutasi Perangkat"
                    7 -> "Laporan Pemeliharaan & Servis"
                    8 -> "Log Aktivitas & Stok"
                    else -> "Sirkulasi Peminjaman"
                }

                val currentSubMenuIcon = when (selectedTabState) {
                    0 -> Icons.Default.Assessment
                    1 -> Icons.Default.Build
                    2 -> Icons.Default.Science
                    3 -> Icons.Default.ReportProblem
                    4 -> Icons.Default.Memory
                    5 -> Icons.Default.Computer
                    6 -> Icons.Default.CloudSync
                    7 -> Icons.Default.Handyman
                    8 -> Icons.Default.History
                    else -> Icons.Default.Sync
                }

                LunarisCard(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFAF5FF)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outline else Color(0xFFDDD6FE)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF7C3AED), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = currentSubMenuIcon,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = currentSubMenuTitle,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = DeepPurpleText
                                )
                                Text(
                                    text = "Periode: $startDateText s/d $endDateText",
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Saring Tanggal (Glassmorphism Card Style)
                LunarisCard(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFFFFFF)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f) else Color(0xFFCBD5E1)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Filter Rentang Tanggal",
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) MaterialTheme.colorScheme.onSurface else DeepPurpleText,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        1.5.dp,
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { showStartPicker = true }
                                    .padding(12.dp)
                            ) {
                                Text(
                                    "Dari",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF7C3AED),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        startDateText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) MaterialTheme.colorScheme.onSurface else Color.Black
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        1.5.dp,
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { showEndPicker = true }
                                    .padding(12.dp)
                            ) {
                                Text(
                                    "Sampai",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF7C3AED),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        endDateText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) MaterialTheme.colorScheme.onSurface else Color.Black
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Main Dynamic Content Area with Elegant Animation Transitions
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    when (selectedTabState) {
                        0 -> SummaryTabContent(
                            stats = reportStats,
                            transactionsCount = transactions.size,
                            selectedStatus = selectedStatus,
                            onSelectedStatusChange = { selectedStatus = it },
                            startDateText = startDateText,
                            endDateText = endDateText,
                            viewModel = viewModel,
                            onNavigateToTab = { selectedTabState = it }
                        )
                        1 -> AlatListTabContent(
                            items = itemsWithStock,
                            masterRooms = masterRooms,
                            onNavigateToTab = { selectedTabState = it }
                        )
                        2 -> PemakaianAfkirTabContent(
                            pemakaian = filteredPemakaian,
                            afkir = filteredAfkir,
                            masterRooms = masterRooms,
                            onNavigateToTab = { selectedTabState = it }
                        )
                        3 -> PenghapusanAsetTabContent(
                            damagedItems = damagedItems,
                            afkirItems = bahanAfkirList,
                            peripheralItems = hapusAsetPeripheralItems,
                            masterRooms = masterRooms,
                            onNavigateToTab = { selectedTabState = it }
                        )
                        4 -> PeripheralListTabContent(
                            items = itemsWithStock,
                            masterRooms = masterRooms,
                            onNavigateToTab = { selectedTabState = it }
                        )
                        5 -> LabKomListTabContent(
                            items = itemsWithStock,
                            maintenance = filteredMaintenance,
                            masterRooms = masterRooms,
                            onNavigateToTab = { selectedTabState = it }
                        )
                        6 -> MutasiTabContent(
                            mutasiList = mutasiList,
                            masterRooms = masterRooms,
                            onNavigateToTab = { selectedTabState = it }
                        )
                        7 -> PemeliharaanServisTabContent(
                            maintenance = filteredMaintenance,
                            servisLuar = servisLuarItems,
                            items = itemsWithStock,
                            masterRooms = masterRooms,
                            onNavigateToTab = { selectedTabState = it }
                        )
                        8 -> LogAktivitasStokTabContent(
                            transactions = transactions,
                            items = itemsWithStock,
                            pemakaian = filteredPemakaian,
                            afkir = filteredAfkir,
                            damaged = damagedItems,
                            masterRooms = masterRooms,
                            onNavigateToTab = { selectedTabState = it }
                        )
                        9 -> SirkulasiTabContent(
                            borrowed = filteredBorrowed,
                            returned = filteredReturned,
                            masterRooms = masterRooms,
                            onNavigateToTab = { selectedTabState = it }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(80.dp))
            }

            // High Contrast Loading Spinner Overlay
            if (isExporting) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    LunarisCard(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = Color(0xFF7C3AED))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Memproses file laporan...", fontWeight = FontWeight.Bold, color = DeepPurpleText)
                        }
                    }
                }
            }
        }

        // Beautiful Actionable Success Dialog
        if (showExportSuccessDialog && successFileUri != null) {
            val dialogBgColor = Color.White
            val textColor = Color(0xFF1F2937)
            val accentColor = Color(0xFF7C3AED)
            val secondaryBtnBg = Color(0xFFEDE9FE)
            val secondaryBtnText = Color(0xFF4C1D95)

            AlertDialog(
                onDismissRequest = { showExportSuccessDialog = false },
                containerColor = dialogBgColor,
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "Laporan Berhasil Disimpan",
                            fontWeight = FontWeight.ExtraBold,
                            color = textColor,
                            fontSize = 18.sp
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "File laporan Anda telah berhasil diekspor langsung ke folder penyimpanan internal:",
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF4B5563),
                            fontSize = 14.sp
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Color(0xFFF3F4F6),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = successFilename,
                                    color = accentColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Lokasi: /Lunaris/Unduh Laporan/",
                                    color = Color(0xFF6B7280),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Text(
                            text = "Gunakan tombol di bawah untuk membuka dokumen secara langsung atau membagikannya.",
                            color = Color(0xFF4B5563),
                            fontSize = 13.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            openFile(context, successFileUri!!, successFileMimeType)
                            showExportSuccessDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("dialog_btn_buka_file")
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Buka File",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            shareFile(context, successFileUri!!, successFileMimeType)
                            showExportSuccessDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = secondaryBtnBg),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("dialog_btn_bagikan_file")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = secondaryBtnText,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Bagikan",
                            color = secondaryBtnText,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        }

        if (showExportFormatDialog) {
            val currentTabTitle = when (selectedTabState) {
                0 -> "Ringkasan Full"
                1 -> "Laporan Alat & Aset"
                2 -> "Bahan & Pemakaian"
                3 -> "Afkir & Alat Rusak"
                4 -> "Peripheral & Stok"
                5 -> "LabKom & PC"
                6 -> "Pemeliharaan & Servis"
                else -> "Sirkulasi Peminjaman"
            }

            Dialog(
                onDismissRequest = { showExportFormatDialog = false }
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                    tonalElevation = 6.dp,
                    shadowElevation = 12.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color(0xFFEDE9FE), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Print,
                                    contentDescription = null,
                                    tint = Color(0xFF7C3AED),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Pilih Format Ekspor Laporan",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DeepPurpleText,
                                    fontSize = 17.sp
                                )
                                Text(
                                    text = "Sub-menu: $currentTabTitle",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Surface(
                                onClick = { showExportFormatDialog = false },
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = Color(0xFFF1F5F9),
                                modifier = Modifier.size(32.dp).testTag("dialog_btn_tutup_export")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Tutup",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        // Option 1: Print / PDF
                        Surface(
                            onClick = {
                                showExportFormatDialog = false
                                if (canPrintPdf) {
                                    handleExport("PDF")
                                } else {
                                    Toast.makeText(context, "Fitur Cetak PDF dinonaktifkan untuk akun Siswa", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFFAF5FF),
                            border = BorderStroke(1.dp, Color(0xFFDDD6FE)),
                            modifier = Modifier.fillMaxWidth().testTag("btn_export_format_pdf")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color(0xFF7C3AED), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Print / PDF Document", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DeepPurpleText)
                                    Text("Cetak langsung ke printer fisik atau simpan dokumen PDF resmi.", fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                        }

                        // Option 2: Excel
                        Surface(
                            onClick = {
                                showExportFormatDialog = false
                                if (canExportExcel) {
                                    handleExport("Excel")
                                } else {
                                    Toast.makeText(context, "Fitur Ekspor Excel dinonaktifkan untuk akun Siswa", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFECFDF5),
                            border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                            modifier = Modifier.fillMaxWidth().testTag("btn_export_format_excel")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color(0xFF10B981), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Description, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Excel / Spreadsheet (.xlsx)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF065F46))
                                    Text("Rekapitulasi data terstruktur untuk lembar kerja Microsoft Excel.", fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                        }

                        // Option 3: Word Document (.docx)
                        Surface(
                            onClick = {
                                showExportFormatDialog = false
                                if (canExportExcel) {
                                    handleExport("Word")
                                } else {
                                    Toast.makeText(context, "Fitur Ekspor Word dinonaktifkan untuk akun Siswa", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                            modifier = Modifier.fillMaxWidth().testTag("btn_export_format_word")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color(0xFF2563EB), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Article, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Word Document (.docx)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1E40AF))
                                    Text("Dokumen Microsoft Word resmi lengkap dengan Kop Laporan dan TTD.", fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// REUSABLE SEARCH & FILTER HEADER
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportSearchAndFilterBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedKondisiFilter: String?,
    onKondisiFilterChange: (String?) -> Unit,
    selectedRoomFilter: String?,
    onRoomFilterChange: (String?) -> Unit,
    masterRooms: List<String>,
    masterKondisiList: List<String> = listOf("Baik", "Perlu Perawatan", "Rusak", "Afkir", "Rusak (Perlu Tindakan)"),
    placeholderText: String = "Ketik untuk mencari...",
    modifier: Modifier = Modifier
) {
    var showFilterPopup by remember { mutableStateOf(false) }
    var showReportQrScanner by remember { mutableStateOf(false) }
    val isFilterActive = selectedKondisiFilter != null || selectedRoomFilter != null

    if (showReportQrScanner) {
        CameraScannerDialog(
            title = "Pindai QR / Barcode Barang",
            onDismissRequest = { showReportQrScanner = false },
            onBarcodeScanned = { code ->
                onSearchQueryChange(code)
                showReportQrScanner = false
            }
        )
    }

    if (showFilterPopup) {
        val roomList = remember(masterRooms) {
            if (masterRooms.isNotEmpty()) masterRooms else listOf("Lab Biologi", "Lab Kimia", "Lab Fisika", "LabKom Utama", "Ruang Alat")
        }
        var tempKondisi by remember { mutableStateOf(selectedKondisiFilter ?: "Semua Kondisi") }
        var tempRoom by remember { mutableStateOf(selectedRoomFilter ?: "Semua Ruang") }

        LunarisFilterDialog(
            onDismissRequest = { showFilterPopup = false },
            title = "Filter Laporan",
            filterGroups = listOf(
                FilterGroup(
                    title = "Kondisi Barang",
                    options = listOf("Semua Kondisi") + masterKondisiList,
                    selectedOption = tempKondisi,
                    onOptionSelected = { tempKondisi = it }
                ),
                FilterGroup(
                    title = "Ruang / Lokasi",
                    options = listOf("Semua Ruang") + roomList,
                    selectedOption = tempRoom,
                    onOptionSelected = { tempRoom = it }
                )
            ),
            onReset = {
                tempKondisi = "Semua Kondisi"
                tempRoom = "Semua Ruang"
                onKondisiFilterChange(null)
                onRoomFilterChange(null)
                showFilterPopup = false
            },
            onApply = {
                onKondisiFilterChange(if (tempKondisi.startsWith("Semua")) null else tempKondisi)
                onRoomFilterChange(if (tempRoom.startsWith("Semua")) null else tempRoom)
                showFilterPopup = false
            }
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Kotak Pencarian (Hanya Teks Placeholder "Ketik untuk mencari..." & QrCodeScanner di kanan)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        text = "Ketik untuk mencari...",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Cari",
                        tint = Color(0xFF7C3AED),
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchQueryChange("") },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Hapus",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = { showReportQrScanner = true },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("btn_scan_qr_laporan")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Pindai QR Kode",
                                tint = Color(0xFF7C3AED),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedBorderColor = Color(0xFF7C3AED),
                    unfocusedTextColor = DeepPurpleText,
                    focusedTextColor = DeepPurpleText
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("input_search_laporan")
            )

            // Ikon Pemicu Filter Standalone (Di luar kotak pencarian, sebelah kanan, tanpa label teks)
            Surface(
                onClick = { showFilterPopup = true },
                shape = RoundedCornerShape(14.dp),
                color = if (isFilterActive) Color(0xFF7C3AED) else Color.White,
                border = BorderStroke(
                    1.dp,
                    if (isFilterActive) Color(0xFF6D28D9) else Color(0xFFCBD5E1)
                ),
                modifier = Modifier
                    .size(50.dp)
                    .testTag("btn_filter_laporan_icon")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter",
                        tint = if (isFilterActive) Color.White else Color(0xFF475569),
                        modifier = Modifier.size(22.dp)
                    )
                    if (isFilterActive) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(5.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }
            }
        }

        // Active Filter Chips
        if (isFilterActive) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedKondisiFilter != null) {
                    SuggestionChip(
                        onClick = { onKondisiFilterChange(null) },
                        label = { Text("Kondisi: $selectedKondisiFilter ✕", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = Color(0xFFEDE9FE),
                            labelColor = Color(0xFF6D28D9)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFDDD6FE))
                    )
                }
                if (selectedRoomFilter != null) {
                    SuggestionChip(
                        onClick = { onRoomFilterChange(null) },
                        label = { Text("Ruang: $selectedRoomFilter ✕", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = Color(0xFFEDE9FE),
                            labelColor = Color(0xFF6D28D9)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFDDD6FE))
                    )
                }
            }
        }
    }
}

// ==========================================
// TAB 0: RINGKASAN CONTENT
// ==========================================
@Composable
fun SummaryTabContent(
    stats: ReportStats?,
    transactionsCount: Int,
    selectedStatus: String?,
    onSelectedStatusChange: (String?) -> Unit,
    startDateText: String,
    endDateText: String,
    viewModel: InventoryViewModel,
    onNavigateToTab: (Int) -> Unit
) {
    if (stats == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF7C3AED))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Memuat ringkasan data...",
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }
        return
    }

    val detailItems by remember(selectedStatus, startDateText, endDateText) {
        if (selectedStatus != null) {
            viewModel.fetchReportDetailItems(selectedStatus, startDateText, endDateText)
        } else {
            flowOf(emptyList())
        }
    }.collectAsState(initial = null)

    val isDark = false

    val itemsList by viewModel.itemsWithStock.collectAsState()

    val stokAmanCount = remember(itemsList) { itemsList.count { it.stokTersedia > 2 } }
    val perluPengadaanCount = remember(itemsList) { itemsList.count { it.stokTersedia == 0 } }
    val stokKritisCount = remember(itemsList) { itemsList.count { it.stokTersedia in 1..2 } }

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // FIXED BAR CHART CARD (Now presenting a modern Stock Status horizontal Bar Chart)
        LunarisCard(
            colors = CardDefaults.cardColors(containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFFFFFF)),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f) else Color(0xFFCBD5E1)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Distribusi Status Stok",
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                )
                Text(
                    text = "Klik pada batang status untuk menyaring rincian data di bawah",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
                )
                StockStatusBarChart(
                    stokAmanCount = stokAmanCount,
                    perluPengadaanCount = perluPengadaanCount,
                    stokKritisCount = stokKritisCount,
                    selectedStatus = selectedStatus,
                    onBarClick = { category ->
                        onSelectedStatusChange(if (selectedStatus == category) null else category)
                    }
                )
            }
        }

        // SCROLLABLE DRILL-DOWN TABLE (now part of the parent scrollable layout)
        LunarisCard(
            colors = CardDefaults.cardColors(containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFFFFFF)),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f) else Color(0xFFCBD5E1)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedStatus != null) "Detail Data: $selectedStatus" else "Rincian Data",
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                    )
                    if (selectedStatus != null) {
                        TextButton(
                            onClick = { onSelectedStatusChange(null) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Reset Filter ✕", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (selectedStatus == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = null,
                                tint = Color(0xFF7C3AED).copy(alpha = 0.6f),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Klik bagian grafik untuk melihat rincian",
                                fontWeight = FontWeight.Medium,
                                color = Color.Gray,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    val itemsList = detailItems
                    if (itemsList == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF7C3AED), modifier = Modifier.size(24.dp))
                        }
                    } else if (itemsList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFEF2F2), RoundedCornerShape(12.dp))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Tidak ada data untuk status [$selectedStatus] dalam rentang tanggal yang dipilih",
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFEF4444),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // Table Headers (Fixed)
                        val label1 = "Nama Alat"
                        val label2 = when (selectedStatus) {
                            "Tersedia" -> "Ruang"
                            "Dipinjam" -> "Peminjam"
                            "Perbaikan" -> "Kerusakan"
                            "Afkir" -> "Alasan Afkir"
                            "Stok Aman", "Perlu Pengadaan", "Stok Kritis" -> "Ruang"
                            else -> "Kategori"
                        }
                        val label3 = "Jumlah"
                        val label4 = when (selectedStatus) {
                            "Tersedia" -> "Kondisi"
                            "Dipinjam" -> "Tgl Pinjam"
                            "Perbaikan" -> "Tgl Masuk"
                            "Afkir" -> "Tgl Afkir"
                            "Stok Aman", "Perlu Pengadaan", "Stok Kritis" -> "Status"
                            else -> "Tanggal"
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFEDE9FE), RoundedCornerShape(8.dp))
                                .padding(vertical = 8.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = label1, modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DeepPurpleText)
                            Text(text = label2, modifier = Modifier.weight(1.8f), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DeepPurpleText)
                            Text(text = label3, modifier = Modifier.weight(0.9f), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DeepPurpleText, textAlign = TextAlign.End)
                            Text(text = label4, modifier = Modifier.weight(1.3f), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DeepPurpleText, textAlign = TextAlign.End)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            itemsList.forEachIndexed { idx, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = if (idx % 2 == 0) Color.White else Color(0xFFF8FAFC),
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .padding(vertical = 10.dp, horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = item.name, modifier = Modifier.weight(2f), fontWeight = FontWeight.Medium, fontSize = 11.sp, color = Color(0xFF1E293B))
                                    Text(text = item.categoryOrRoom, modifier = Modifier.weight(1.8f), fontSize = 11.sp, color = Color(0xFF475569))
                                    Text(
                                        text = "${item.quantity} ${item.extra}", 
                                        modifier = Modifier.weight(0.9f), 
                                        fontSize = 11.sp, 
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A), 
                                        textAlign = TextAlign.End
                                    )
                                    Text(text = item.dateOrStatus, modifier = Modifier.weight(1.3f), fontSize = 11.sp, color = Color(0xFF64748B), textAlign = TextAlign.End)
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

// ==========================================
// NEW TABS DEFINITIONS (ALAT, BAHAN, PEMINJAMAN, PENGEMBALIAN, ALAT RUSAK)
// ==========================================
@Composable
fun AlatListTabContent(
    items: List<ItemWithStock>,
    masterRooms: List<String> = emptyList(),
    onNavigateToTab: (Int) -> Unit
) {
    val isDark = false
    val toolsOnly = remember(items) {
        items.filter { !it.kategori.equals("Logistik", ignoreCase = true) }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedRoomFilter by remember { mutableStateOf<String?>(null) }
    var selectedKondisiFilter by remember { mutableStateOf<String?>(null) }

    // Group items by room and map to Triple(Baik, PerluPerawatan, Rusak)
    val roomConditionData = remember(toolsOnly) {
        val groups = toolsOnly.groupBy { it.ruang.ifBlank { "Lainnya" } }
        groups.mapValues { (_, roomItems) ->
            var baik = 0f
            var perawatan = 0f
            var rusak = 0f
            roomItems.forEach { item ->
                val r = item.stokRusak.toFloat()
                rusak += r
                
                val rem = (item.stokAwal - item.stokRusak).coerceAtLeast(0).toFloat()
                if (item.kondisi.equals("Baik", ignoreCase = true) || item.kondisi.isBlank()) {
                    baik += rem
                } else if (item.kondisi.equals("Perlu Perawatan", ignoreCase = true) || item.kondisi.equals("Pemeliharaan", ignoreCase = true)) {
                    perawatan += rem
                } else {
                    rusak += rem
                }
            }
            Triple(baik, perawatan, rusak)
        }
    }

    val filteredTools = remember(toolsOnly, searchQuery, selectedRoomFilter, selectedKondisiFilter) {
        toolsOnly.filter { item ->
            val matchesSearch = searchQuery.isBlank() ||
                item.namaBarang.contains(searchQuery, ignoreCase = true) ||
                item.idBarang.contains(searchQuery, ignoreCase = true) ||
                item.ruang.contains(searchQuery, ignoreCase = true)
            val matchesRoom = selectedRoomFilter == null || (item.ruang.ifBlank { "Lainnya" }) == selectedRoomFilter
            val matchesKondisi = selectedKondisiFilter == null || item.kondisi.equals(selectedKondisiFilter, ignoreCase = true)
            matchesSearch && matchesRoom && matchesKondisi
        }
    }

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. MAIN HEADER / SUMMARY CARD (FIRST)
        LunarisCard(
            colors = CardDefaults.cardColors(containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFFFFFF)),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f) else Color(0xFFCBD5E1)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Kondisi Alat per Ruangan",
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepPurpleText,
                    fontSize = 16.sp
                )
                Text(
                    text = "Klik pada bar ruangan untuk menyaring daftar alat",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))
                StackedBarChart(
                    data = roomConditionData,
                    onBarClick = { room ->
                        selectedRoomFilter = if (selectedRoomFilter == room) null else room
                    }
                )
            }
        }

        // 2. SEARCH & FILTER BAR (SECOND)
        ReportSearchAndFilterBar(
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            selectedKondisiFilter = selectedKondisiFilter,
            onKondisiFilterChange = { selectedKondisiFilter = it },
            selectedRoomFilter = selectedRoomFilter,
            onRoomFilterChange = { selectedRoomFilter = it },
            masterRooms = masterRooms,
            placeholderText = "Ketik untuk mencari..."
        )

        // Header Row for the list
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Daftar Inventaris Alat", fontWeight = FontWeight.ExtraBold, color = DeepPurpleText, fontSize = 16.sp)
            if (selectedRoomFilter != null) {
                SuggestionChip(
                    onClick = { selectedRoomFilter = null },
                    label = { Text("Ruang: $selectedRoomFilter ✕", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        if (filteredTools.isEmpty()) {
            EmptyStateView("Tidak ada data inventaris alat untuk penyaringan ini.")
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                filteredTools.forEach { item ->
                    LunarisCard(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFFF3E8FF), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Build, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(20.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.namaBarang,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepPurpleText
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Ruang: ${item.ruang.ifBlank { "Lainnya" }} | Kondisi: ${item.kondisi.ifBlank { "Baik" }}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${item.stokTersedia} / ${item.stokAwal}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = DeepPurpleText
                                )
                                Text("Tersedia", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun BahanListTabContent(
    items: List<ItemWithStock>,
    pemakaian: List<com.example.data.entity.PemakaianBahanEntity>,
    masterRooms: List<String> = emptyList(),
    onNavigateToTab: (Int) -> Unit
) {
    val isDark = false
    val bahanOnly = remember(items) {
        items.filter { it.kategori.equals("Logistik", ignoreCase = true) }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedRoomFilter by remember { mutableStateOf<String?>(null) }
    var selectedKondisiFilter by remember { mutableStateOf<String?>(null) }
    var selectedMonthFilter by remember { mutableStateOf<String?>(null) }

    // Group usage by month chronologically
    val monthlyUsageData = remember(pemakaian) {
        val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agt", "Sep", "Okt", "Nov", "Des")
        val usageMap = java.util.TreeMap<String, Float>()
        
        // Ensure there's a baseline of current year months
        val cal = java.util.Calendar.getInstance()
        val currentMonth = cal.get(java.util.Calendar.MONTH)
        for (i in (currentMonth - 5).coerceAtLeast(0)..currentMonth) {
            val key = String.format(Locale.US, "%02d %s", i + 1, monthNames[i])
            usageMap[key] = 0f
        }

        pemakaian.forEach { p ->
            try {
                val parts = p.tanggalPemakaian.split("-")
                if (parts.size >= 2) {
                    val monthIndex = parts[1].toInt() - 1
                    if (monthIndex in 0..11) {
                        val mName = monthNames[monthIndex]
                        val key = String.format(Locale.US, "%02d %s", monthIndex + 1, mName)
                        usageMap[key] = (usageMap[key] ?: 0f) + p.jumlahDiambil.toFloat()
                    }
                }
            } catch (e: Exception) { /* ignore */ }
        }
        usageMap.mapKeys { it.key.substring(3) }
    }

    val filteredBahan = remember(bahanOnly, pemakaian, searchQuery, selectedRoomFilter, selectedKondisiFilter, selectedMonthFilter) {
        var baseList = bahanOnly

        if (selectedMonthFilter != null) {
            val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agt", "Sep", "Okt", "Nov", "Des")
            val targetItemIds = pemakaian.filter { p ->
                try {
                    val parts = p.tanggalPemakaian.split("-")
                    if (parts.size >= 2) {
                        val mIdx = parts[1].toInt() - 1
                        mIdx in 0..11 && monthNames[mIdx] == selectedMonthFilter
                    } else false
                } catch (e: Exception) { false }
            }.map { it.idBarang }.toSet()
            baseList = baseList.filter { it.idBarang in targetItemIds }
        }

        baseList.filter { item ->
            val matchesSearch = searchQuery.isBlank() ||
                item.namaBarang.contains(searchQuery, ignoreCase = true) ||
                item.idBarang.contains(searchQuery, ignoreCase = true) ||
                item.ruang.contains(searchQuery, ignoreCase = true)
            val matchesRoom = selectedRoomFilter == null || (item.ruang.ifBlank { "Lainnya" }) == selectedRoomFilter
            val matchesKondisi = selectedKondisiFilter == null || item.kondisi.equals(selectedKondisiFilter, ignoreCase = true)
            matchesSearch && matchesRoom && matchesKondisi
        }
    }

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Search & Filter Bar
        ReportSearchAndFilterBar(
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            selectedKondisiFilter = selectedKondisiFilter,
            onKondisiFilterChange = { selectedKondisiFilter = it },
            selectedRoomFilter = selectedRoomFilter,
            onRoomFilterChange = { selectedRoomFilter = it },
            masterRooms = masterRooms,
            placeholderText = "Ketik untuk mencari..."
        )
        // FIXED LINE CHART CARD
        LunarisCard(
            colors = CardDefaults.cardColors(containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFFFFFF)),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f) else Color(0xFFCBD5E1)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Tren Konsumsi Bahan Bulanan",
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepPurpleText,
                    fontSize = 16.sp
                )
                Text(
                    text = "Klik pada label bulan untuk menyaring daftar bahan",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))
                LineChart(
                    data = monthlyUsageData,
                    lineColor = Color(0xFF7C3AED),
                    onPointClick = { month ->
                        selectedMonthFilter = if (selectedMonthFilter == month) null else month
                    }
                )
            }
        }

        // Header Row for the list
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Daftar Inventaris Bahan", fontWeight = FontWeight.ExtraBold, color = DeepPurpleText, fontSize = 16.sp)
            if (selectedMonthFilter != null) {
                SuggestionChip(
                    onClick = { selectedMonthFilter = null },
                    label = { Text("Bulan: $selectedMonthFilter ✕", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        if (filteredBahan.isEmpty()) {
            EmptyStateView("Tidak ada data inventaris bahan yang dikonsumsi pada bulan ini.")
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                filteredBahan.forEach { item ->
                    LunarisCard(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFFE0F2FE), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Science, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(20.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.namaBarang,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepPurpleText
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Ruang: ${item.ruang} | Satuan: ${item.satuan}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${item.stokTersedia}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = DeepPurpleText
                                    )
                                    Text("Sisa Stok", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

fun isReturnLate(tanggalPinjam: String, tanggalKembali: String?): Boolean {
    if (tanggalKembali == null) return false
    return tanggalPinjam != tanggalKembali
}

fun isLaporanOverdue(tanggalTransaksi: String): Boolean {
    try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val transDate = sdf.parse(tanggalTransaksi) ?: return false
        val calTrans = Calendar.getInstance().apply { time = transDate }
        calTrans.set(Calendar.HOUR_OF_DAY, 0)
        calTrans.set(Calendar.MINUTE, 0)
        calTrans.set(Calendar.SECOND, 0)
        calTrans.set(Calendar.MILLISECOND, 0)
        val calToday = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.JULY)
            set(Calendar.DAY_OF_MONTH, 16)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diffMs = calToday.timeInMillis - calTrans.timeInMillis
        val diffDays = (diffMs / (1000 * 60 * 60 * 24)).toInt()
        return diffDays > 0
    } catch (e: Exception) {
        return false
    }
}

@Composable
fun PeminjamanListTabContent(
    borrowed: List<BorrowedLineItem>,
    onNavigateToTab: (Int) -> Unit
) {
    val isDark = false
    val context = LocalContext.current
    var selectedItemFilter by remember { mutableStateOf<String?>(null) }

    // Calculate Top 5 most borrowed tools in the selected date range
    val topBorrowedData = remember(borrowed) {
        borrowed.groupBy { it.namaBarang }
            .mapValues { entry -> entry.value.sumOf { it.jumlah }.toFloat() }
            .toList()
            .sortedByDescending { it.second }
            .take(5)
            .toMap()
    }

    val filteredBorrowed = remember(borrowed, selectedItemFilter) {
        if (selectedItemFilter == null) {
            borrowed
        } else {
            borrowed.filter { it.namaBarang == selectedItemFilter }
        }
    }

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // FIXED HORIZONTAL BAR CHART CARD
        LunarisCard(
            colors = CardDefaults.cardColors(containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFFFFFF)),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f) else Color(0xFFCBD5E1)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Statistik Peminjaman Alat",
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepPurpleText,
                    fontSize = 16.sp
                )
                Text("Klik pada bar alat untuk menyaring daftar transaksi", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))
                
                if (topBorrowedData.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color.LightGray,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tidak ada aktivitas peminjaman pada rentang tanggal terpilih",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    InteractiveHorizontalBarChart(
                        data = topBorrowedData,
                        barColor = Color(0xFF7C3AED),
                        onBarClick = { itemName ->
                            selectedItemFilter = if (selectedItemFilter == itemName) null else itemName
                        }
                    )
                }
            }
        }

        // Header Row for the list
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Riwayat Peminjaman Alat", fontWeight = FontWeight.ExtraBold, color = DeepPurpleText, fontSize = 16.sp)
            if (selectedItemFilter != null) {
                SuggestionChip(
                    onClick = { selectedItemFilter = null },
                    label = { Text("Alat: $selectedItemFilter ✕", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        if (filteredBorrowed.isEmpty()) {
            EmptyStateView("Tidak ada riwayat peminjaman untuk penyaringan ini.")
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                filteredBorrowed.forEach { item ->
                    val isLate = item.status == "Dipinjam" && isLaporanOverdue(item.tanggal)
                    LunarisCard(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, if (isLate) Color(0xFFFCA5A5) else Color(0xFFF1F5F9)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(if (isLate) Color(0xFFFEE2E2) else Color(0xFFEFF6FF), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isLate) Icons.Default.Warning else Icons.Default.Assignment,
                                        contentDescription = null,
                                        tint = if (isLate) Color(0xFFEF4444) else Color(0xFF3B82F6),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "${item.namaBarang} (${item.jumlah} Pcs)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = DeepPurpleText,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        
                                        if (isLate) {
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFFFEE2E2), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "Terlambat",
                                                    color = Color(0xFF991B1B),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.sp
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Peminjam: ${item.namaPeminjam} (${item.kelas})", style = MaterialTheme.typography.bodySmall)
                                    if (!item.tujuanPeminjaman.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("Tujuan: ${item.tujuanPeminjaman}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                    }
                                    if (!item.detailTujuan.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        val detailLabel = if (item.tujuanPeminjaman == "Kegiatan Belajar Mengajar (KBM)") {
                                            "Guru/Mapel"
                                        } else {
                                            "Detail"
                                        }
                                        Text("$detailLabel: ${item.detailTujuan}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Petugas: ${item.petugas}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        Text(item.tanggal, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                }
                            }
                            
                            if (isLate) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = {
                                            val phoneNum = item.whatsappNumber ?: "6285600005719"
                                            val message = "Halo ${item.namaPeminjam}, kami dari sarpras ingin mengingatkan bahwa ${item.namaBarang} (${item.jumlah} Pcs) yang Anda pinjam telah melewati batas pengembalian. Harap segera mengembalikannya ke gudang. Terima kasih!"
                                            val encodedMsg = java.net.URLEncoder.encode(message, "UTF-8")
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                                data = android.net.Uri.parse("https://wa.me/$phoneNum?text=$encodedMsg")
                                            }
                                            context.startActivity(intent)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(32.dp).testTag("wa_remind_button_laporan")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "WA",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Kirim Pengingat WA", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun PengembalianListTabContent(
    returned: List<ReturnedLineItem>,
    transactions: List<com.example.data.entity.LoanTransactionEntity>,
    onNavigateToTab: (Int) -> Unit
) {
    val isDark = false
    var selectedTimelinessFilter by remember { mutableStateOf<String?>(null) }

    // Calculate Tepat Waktu vs Terlambat counts based on transactions
    val (tepatWaktuCount, terlambatCount) = remember(transactions) {
        var tepat = 0f
        var lambat = 0f
        transactions.forEach { tx ->
            if (tx.status == "Kembali") {
                val isLate = isReturnLate(tx.tanggal, tx.tanggalKembali)
                if (isLate) lambat += 1f else tepat += 1f
            } else if (tx.status == "Dipinjam") {
                if (isLaporanOverdue(tx.tanggal)) {
                    lambat += 1f
                }
            }
        }
        Pair(tepat, lambat)
    }

    val filteredReturned = remember(returned, selectedTimelinessFilter) {
        if (selectedTimelinessFilter == null) {
            returned
        } else {
            val filterForLate = selectedTimelinessFilter == "Terlambat"
            returned.filter { isReturnLate(it.tanggalPinjam, it.tanggalKembali) == filterForLate }
        }
    }

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // FIXED DISCIPLINE BAR CHART CARD
        LunarisCard(
            colors = CardDefaults.cardColors(containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFFFFFF)),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f) else Color(0xFFCBD5E1)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Statistik Pengembalian Alat",
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepPurpleText,
                    fontSize = 16.sp
                )
                Text("Klik pada bar untuk menyaring daftar pengembalian", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))
                DisciplineBarChart(
                    tepatWaktu = tepatWaktuCount,
                    terlambat = terlambatCount,
                    onBarClick = { filter ->
                        selectedTimelinessFilter = if (selectedTimelinessFilter == filter) null else filter
                    }
                )
            }
        }

        // Header Row for the list
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Riwayat Pengembalian Alat", fontWeight = FontWeight.ExtraBold, color = DeepPurpleText, fontSize = 16.sp)
            if (selectedTimelinessFilter != null) {
                SuggestionChip(
                    onClick = { selectedTimelinessFilter = null },
                    label = { Text("Filter: $selectedTimelinessFilter ✕", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        if (filteredReturned.isEmpty()) {
            EmptyStateView("Tidak ada riwayat pengembalian untuk penyaringan ini.")
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                filteredReturned.forEach { item ->
                    LunarisCard(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFFECFDF5), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${item.namaBarang} (${item.jumlah} Pcs)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepPurpleText
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Peminjam: ${item.namaPeminjam} (${item.kelas})", style = MaterialTheme.typography.bodySmall)
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Petugas: ${item.petugasKembali}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    Text(item.tanggalKembali, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun AlatRusakListTabContent(
    damaged: List<com.example.data.entity.DamagedItemEntity>,
    onNavigateToTab: (Int) -> Unit
) {
    val isDark = false
    var selectedMonthFilter by remember { mutableStateOf<String?>(null) }

    // Group damage count by month chronologically
    val monthlyDamageData = remember(damaged) {
        val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agt", "Sep", "Okt", "Nov", "Des")
        val damageMap = java.util.TreeMap<String, Float>()
        
        val cal = java.util.Calendar.getInstance()
        val currentMonth = cal.get(java.util.Calendar.MONTH)
        for (i in (currentMonth - 5).coerceAtLeast(0)..currentMonth) {
            val key = String.format(Locale.US, "%02d %s", i + 1, monthNames[i])
            damageMap[key] = 0f
        }

        damaged.forEach { d ->
            try {
                val parts = d.tanggalKerusakan.split("-")
                if (parts.size >= 2) {
                    val monthIndex = parts[1].toInt() - 1
                    if (monthIndex in 0..11) {
                        val mName = monthNames[monthIndex]
                        val key = String.format(Locale.US, "%02d %s", monthIndex + 1, mName)
                        damageMap[key] = (damageMap[key] ?: 0f) + d.jumlah.toFloat()
                    }
                }
            } catch (e: Exception) { /* ignore */ }
        }
        damageMap.mapKeys { it.key.substring(3) }
    }

    val filteredDamaged = remember(damaged, selectedMonthFilter) {
        if (selectedMonthFilter == null) {
            damaged
        } else {
            val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agt", "Sep", "Okt", "Nov", "Des")
            damaged.filter { d ->
                try {
                    val parts = d.tanggalKerusakan.split("-")
                    if (parts.size >= 2) {
                        val mIdx = parts[1].toInt() - 1
                        mIdx in 0..11 && monthNames[mIdx] == selectedMonthFilter
                    } else false
                } catch (e: Exception) { false }
            }
        }
    }

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // FIXED DAMAGE AREA CHART CARD
        LunarisCard(
            colors = CardDefaults.cardColors(containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFFFFFF)),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f) else Color(0xFFCBD5E1)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Daftar Alat Rusak",
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepPurpleText,
                    fontSize = 16.sp
                )
                Text("Klik pada label bulan untuk menyaring log kerusakan", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))
                LineChart(
                    data = monthlyDamageData,
                    lineColor = Color(0xFFEF4444), // Crimson/Red for damage
                    onPointClick = { month ->
                        selectedMonthFilter = if (selectedMonthFilter == month) null else month
                    }
                )
            }
        }

        // Header Row for the list
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Daftar Kondisi Alat Rusak", fontWeight = FontWeight.ExtraBold, color = DeepPurpleText, fontSize = 16.sp)
            if (selectedMonthFilter != null) {
                SuggestionChip(
                    onClick = { selectedMonthFilter = null },
                    label = { Text("Bulan: $selectedMonthFilter ✕", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        if (filteredDamaged.isEmpty()) {
            EmptyStateView("Tidak ada laporan alat rusak untuk penyaringan ini.")
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                filteredDamaged.forEach { item ->
                    LunarisCard(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color(0xFFFEF2F2), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${item.namaBarang} (${item.jumlah} Pcs Rusak)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepPurpleText
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Keterangan: ${item.keteranganKerusakan}", style = MaterialTheme.typography.bodySmall)
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Tanggal Lapor", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        Text(item.tanggalKerusakan, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun PemeliharaanTabContent(
    maintenance: List<com.example.data.entity.DamagedItemEntity>,
    items: List<ItemWithStock>,
    masterRooms: List<String> = emptyList(),
    onNavigateToTab: (Int) -> Unit
) {
    val isDark = false
    var searchQuery by remember { mutableStateOf("") }
    var selectedRoomFilter by remember { mutableStateOf<String?>(null) }
    var selectedKondisiFilter by remember { mutableStateOf<String?>(null) }

    // Map each item ID to its room
    val itemRoomMap = remember(items) {
        items.associate { it.idBarang to it.ruang }
    }

    // Group maintenance items by room
    val roomMaintenanceData = remember(maintenance, itemRoomMap) {
        val grouped = maintenance.groupBy { itemRoomMap[it.idBarang]?.ifBlank { "Lainnya" } ?: "Lainnya" }
            .mapValues { entry -> entry.value.sumOf { it.jumlah }.toFloat() }
        
        if (grouped.isEmpty()) {
            android.util.Log.e("PemeliharaanTabContent", "Maintenance data map for Bar Chart is EMPTY! Unfiltered maintenance list size is: ${maintenance.size}")
        } else {
            android.util.Log.d("PemeliharaanTabContent", "Successfully mapped maintenance data for Bar Chart: $grouped")
        }
        grouped
    }

    val filteredMaintenance = remember(maintenance, searchQuery, selectedRoomFilter, selectedKondisiFilter, itemRoomMap) {
        val kFilter = selectedKondisiFilter
        maintenance.filter { item ->
            val room = itemRoomMap[item.idBarang]?.ifBlank { "Lainnya" } ?: "Lainnya"
            val matchesSearch = searchQuery.isBlank() ||
                item.namaBarang.contains(searchQuery, ignoreCase = true) ||
                item.idBarang.contains(searchQuery, ignoreCase = true) ||
                item.keteranganKerusakan.contains(searchQuery, ignoreCase = true) ||
                room.contains(searchQuery, ignoreCase = true)
            val matchesRoom = selectedRoomFilter == null || room == selectedRoomFilter
            val matchesKondisi = kFilter == null || item.status.contains(kFilter, ignoreCase = true)
            matchesSearch && matchesRoom && matchesKondisi
        }
    }

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. MAIN HEADER / SUMMARY CARD (FIRST)
        LunarisCard(
            colors = CardDefaults.cardColors(containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFFFFFF)),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f) else Color(0xFFCBD5E1)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Jadwal Pemeliharaan Alat",
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepPurpleText,
                    fontSize = 16.sp
                )
                Text("Klik pada bar ruangan untuk menyaring daftar pemeliharaan", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))
                
                if (roomMaintenanceData.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color.LightGray,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tidak ada alat dalam status pemeliharaan",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    InteractiveHorizontalBarChart(
                        data = roomMaintenanceData,
                        barColor = Color(0xFFF59E0B), // Amber for maintenance
                        onBarClick = { room ->
                            selectedRoomFilter = if (selectedRoomFilter == room) null else room
                        }
                    )
                }
            }
        }

        // 2. SEARCH & FILTER BAR (SECOND)
        ReportSearchAndFilterBar(
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            selectedKondisiFilter = selectedKondisiFilter,
            onKondisiFilterChange = { selectedKondisiFilter = it },
            selectedRoomFilter = selectedRoomFilter,
            onRoomFilterChange = { selectedRoomFilter = it },
            masterRooms = masterRooms,
            placeholderText = "Ketik untuk mencari..."
        )

        // Header Row for the list
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Daftar Pemeliharaan Alat (Servis Luar)", fontWeight = FontWeight.ExtraBold, color = DeepPurpleText, fontSize = 16.sp)
            if (selectedRoomFilter != null) {
                SuggestionChip(
                    onClick = { selectedRoomFilter = null },
                    label = { Text("Ruang: $selectedRoomFilter ✕", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        if (filteredMaintenance.isEmpty()) {
            EmptyStateView("Tidak ada alat dalam status pemeliharaan untuk penyaringan ini.")
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                filteredMaintenance.forEach { item ->
                    LunarisCard(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color(0xFFEDE9FE), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Build, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(20.dp))
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${item.namaBarang} (${item.jumlah} Pcs di-Servis)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepPurpleText
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Keterangan Awal: ${item.keteranganKerusakan}", style = MaterialTheme.typography.bodySmall)
                                    if (item.statusKeterangan.isNotBlank()) {
                                        Text("Catatan Pemeliharaan: ${item.statusKeterangan}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF7C3AED), fontWeight = FontWeight.SemiBold)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Petugas: ${item.namaPetugas}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        Text(item.tanggalKerusakan, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

// ==========================================
// TAB 3: AFKIR CONTENT
// ==========================================
@Composable
fun AfkirTabContent(
    afkir: List<com.example.data.entity.BahanAfkirEntity>,
    onNavigateToTab: (Int) -> Unit
) {
    val isDark = false
    var selectedReasonFilter by remember { mutableStateOf<String?>(null) }

    // Reason Pie Chart Mapping
    val reasonCounts = remember(afkir) {
        afkir.groupBy { it.alasan.ifBlank { "Lainnya" } }
            .mapValues { entry -> entry.value.sumOf { it.jumlahAfkir }.toFloat() }
    }

    val chartColors = listOf(
        Color(0xFFEF4444), // Red for Rusak/Damage
        Color(0xFFF59E0B), // Amber for Expired
        Color(0xFF6B7280)  // Gray for Lost
    )

    val filteredAfkir = remember(afkir, selectedReasonFilter) {
        if (selectedReasonFilter == null || selectedReasonFilter == "Semua") {
            afkir
        } else {
            afkir.filter { it.alasan.ifBlank { "Lainnya" } == selectedReasonFilter }
        }
    }

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // FIXED PIE CHART CARD
        LunarisCard(
            colors = CardDefaults.cardColors(containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFFFFFF)),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f) else Color(0xFFCBD5E1)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Data Inventaris Afkir",
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepPurpleText,
                    fontSize = 16.sp
                )
                Text("Klik pada label alasan untuk menyaring log", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))
                InteractivePieChart(
                    data = reasonCounts,
                    colors = chartColors,
                    onSliceClick = { reason ->
                        selectedReasonFilter = if (reason == "Semua" || selectedReasonFilter == reason) null else reason
                    }
                )
            }
        }

        // Header Row for the list
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Log Riwayat Bahan Afkir", fontWeight = FontWeight.ExtraBold, color = DeepPurpleText, fontSize = 16.sp)
            if (selectedReasonFilter != null) {
                SuggestionChip(
                    onClick = { selectedReasonFilter = null },
                    label = { Text("Alasan: $selectedReasonFilter ✕", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        if (filteredAfkir.isEmpty()) {
            EmptyStateView("Tidak ada data log bahan afkir untuk penyaringan ini.")
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                filteredAfkir.forEach { log ->
                    LunarisCard(
                        colors = CardDefaults.cardColors(containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFFFFFF)),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f) else Color(0xFFCBD5E1)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(log.namaBarang, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepPurpleText)
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFFEF2F2), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("${log.jumlahAfkir} ${log.satuan}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Alasan Afkir: ${log.alasan}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF1F5F9))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("ID Barang: ${log.idBarang}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                Text(log.tanggalAfkir, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

// ==========================================
// TAB 3: AFKIR & ALAT RUSAK CONTENT
// ==========================================
@Composable
fun AfkirAlatRusakTabContent(
    afkir: List<com.example.data.entity.BahanAfkirEntity>,
    damaged: List<com.example.data.entity.DamagedItemEntity>,
    masterRooms: List<String> = emptyList(),
    onNavigateToTab: (Int) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedRoomFilter by remember { mutableStateOf<String?>(null) }
    var selectedKondisiFilter by remember { mutableStateOf<String?>(null) }

    val filteredDamaged = remember(damaged, searchQuery, selectedKondisiFilter) {
        val kFilter = selectedKondisiFilter
        damaged.filter { item ->
            val matchesSearch = searchQuery.isBlank() ||
                item.namaBarang.contains(searchQuery, ignoreCase = true) ||
                item.idBarang.contains(searchQuery, ignoreCase = true) ||
                item.keteranganKerusakan.contains(searchQuery, ignoreCase = true)
            val matchesKondisi = kFilter == null || item.status.contains(kFilter, ignoreCase = true)
            matchesSearch && matchesKondisi
        }
    }

    val filteredAfkir = remember(afkir, searchQuery, selectedKondisiFilter) {
        val kFilter = selectedKondisiFilter
        afkir.filter { item ->
            val matchesSearch = searchQuery.isBlank() ||
                item.namaBarang.contains(searchQuery, ignoreCase = true) ||
                item.idBarang.contains(searchQuery, ignoreCase = true) ||
                item.alasan.contains(searchQuery, ignoreCase = true)
            val matchesKondisi = kFilter == null || item.status.contains(kFilter, ignoreCase = true)
            matchesSearch && matchesKondisi
        }
    }

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. MAIN HEADER / SUMMARY CARD (FIRST)
        LunarisCard(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Laporan Rekapitulasi Bahan Afkir & List Alat Rusak",
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepPurpleText,
                    fontSize = 16.sp
                )
                Text(
                    text = "Rekapitulasi pencatatan bahan afkir serta rincian alat rusak",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFFEF2F2), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text("Total Bahan Afkir", fontSize = 11.sp, color = Color(0xFF991B1B), fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${afkir.sumOf { it.jumlahAfkir }} Item", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFFFFBEB), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text("Total Alat Rusak", fontSize = 11.sp, color = Color(0xFF92400E), fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${damaged.sumOf { it.jumlah }} Pcs", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                        }
                    }
                }
            }
        }

        // 2. SEARCH & FILTER BAR (SECOND)
        ReportSearchAndFilterBar(
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            selectedKondisiFilter = selectedKondisiFilter,
            onKondisiFilterChange = { selectedKondisiFilter = it },
            selectedRoomFilter = selectedRoomFilter,
            onRoomFilterChange = { selectedRoomFilter = it },
            masterRooms = masterRooms,
            placeholderText = "Ketik untuk mencari..."
        )

        Text("Daftar Bahan Afkir", fontWeight = FontWeight.ExtraBold, color = DeepPurpleText, fontSize = 16.sp)
        if (filteredAfkir.isEmpty()) {
            EmptyStateView("Tidak ada catatan bahan afkir.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                filteredAfkir.forEach { item ->
                    LunarisCard(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.namaBarang, fontWeight = FontWeight.Bold, color = DeepPurpleText, fontSize = 14.sp)
                                Text("Alasan: ${item.alasan} | Tgl: ${item.tanggalAfkir}", fontSize = 12.sp, color = Color.Gray)
                            }
                            Text("${item.jumlahAfkir} ${item.satuan}", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626), fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Daftar Kondisi Alat Rusak", fontWeight = FontWeight.ExtraBold, color = DeepPurpleText, fontSize = 16.sp)
        if (filteredDamaged.isEmpty()) {
            EmptyStateView("Tidak ada laporan alat rusak.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                filteredDamaged.forEach { d ->
                    LunarisCard(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(d.namaBarang, fontWeight = FontWeight.Bold, color = DeepPurpleText, fontSize = 14.sp)
                                Text("Keterangan: ${d.keteranganKerusakan} | Tgl: ${d.tanggalKerusakan}", fontSize = 12.sp, color = Color.Gray)
                            }
                            Text("${d.jumlah} Pcs", fontWeight = FontWeight.Bold, color = Color(0xFFD97706), fontSize = 14.sp)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

// ==========================================
// TAB 4: PERIPHERAL & RUSAK CONTENT
// ==========================================
@Composable
fun PeripheralListTabContent(
    items: List<ItemWithStock>,
    masterRooms: List<String> = emptyList(),
    onNavigateToTab: (Int) -> Unit
) {
    val peripheralItems = remember(items) {
        items.filter { 
            it.kategori.contains("Peripheral", ignoreCase = true) || 
            it.type == "PERIPHERAL" ||
            listOf("Mouse", "Keyboard", "Monitor", "Kabel", "Headset", "Switch", "Hub", "RAM", "SSD", "GPU", "Printer", "Scanner").any { keyword ->
                it.namaBarang.contains(keyword, ignoreCase = true)
            }
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedRoomFilter by remember { mutableStateOf<String?>(null) }
    var selectedKondisiFilter by remember { mutableStateOf<String?>(null) }

    val filteredPeripheral = remember(peripheralItems, searchQuery, selectedRoomFilter, selectedKondisiFilter) {
        peripheralItems.filter { item ->
            val matchesSearch = searchQuery.isBlank() ||
                item.namaBarang.contains(searchQuery, ignoreCase = true) ||
                item.idBarang.contains(searchQuery, ignoreCase = true) ||
                item.ruang.contains(searchQuery, ignoreCase = true)
            val matchesRoom = selectedRoomFilter == null || (item.ruang.ifBlank { "Lainnya" }) == selectedRoomFilter
            val matchesKondisi = selectedKondisiFilter == null || item.kondisi.equals(selectedKondisiFilter, ignoreCase = true)
            matchesSearch && matchesRoom && matchesKondisi
        }
    }

    val readyCount = remember(filteredPeripheral) { filteredPeripheral.sumOf { it.stokTersedia } }
    val damagedCount = remember(filteredPeripheral) { filteredPeripheral.sumOf { it.stokRusak } }

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. MAIN HEADER / SUMMARY CARD (FIRST)
        LunarisCard(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Laporan Stok Peripheral & Peripheral Rusak",
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepPurpleText,
                    fontSize = 16.sp
                )
                Text(
                    text = "Rincian ketersediaan dan status kondisi unit peripheral",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFF0FDF4), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text("Ready / Normal", fontSize = 11.sp, color = Color(0xFF166534), fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("$readyCount Pcs", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFFEF2F2), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text("Peripheral Rusak", fontSize = 11.sp, color = Color(0xFF991B1B), fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("$damagedCount Pcs", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                        }
                    }
                }
            }
        }

        // 2. SEARCH & FILTER BAR (SECOND)
        ReportSearchAndFilterBar(
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            selectedKondisiFilter = selectedKondisiFilter,
            onKondisiFilterChange = { selectedKondisiFilter = it },
            selectedRoomFilter = selectedRoomFilter,
            onRoomFilterChange = { selectedRoomFilter = it },
            masterRooms = masterRooms,
            placeholderText = "Ketik untuk mencari..."
        )

        Text(
            text = "Daftar Inventaris Peripheral ${selectedRoomFilter?.let { "($it)" } ?: ""}",
            fontWeight = FontWeight.ExtraBold,
            color = DeepPurpleText,
            fontSize = 16.sp
        )

        if (filteredPeripheral.isEmpty()) {
            EmptyStateView("Tidak ada data peripheral untuk penyaringan ini.")
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                filteredPeripheral.forEach { item ->
                    LunarisCard(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFFEDE9FE), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Devices, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(20.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.namaBarang, fontWeight = FontWeight.Bold, color = DeepPurpleText, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Ruang: ${item.ruang.ifBlank { "Lainnya" }} | Kondisi: ${item.kondisi.ifBlank { "Baik" }}", fontSize = 12.sp, color = Color.Gray)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${item.stokTersedia} / ${item.stokAwal}", fontWeight = FontWeight.Black, color = DeepPurpleText, fontSize = 16.sp)
                                Text("Tersedia", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

// ==========================================
// TAB 5: LABKOM & PERAWATAN PC CONTENT
// ==========================================
@Composable
fun LabKomListTabContent(
    items: List<ItemWithStock>,
    maintenance: List<com.example.data.entity.DamagedItemEntity>,
    masterRooms: List<String> = emptyList(),
    onNavigateToTab: (Int) -> Unit
) {
    val pcItems = remember(items) {
        items.filter {
            it.kategori.contains("LabKom", ignoreCase = true) ||
            it.ruang.contains("LabKom", ignoreCase = true) ||
            it.namaBarang.contains("PC", ignoreCase = true) ||
            it.namaBarang.contains("Komputer", ignoreCase = true) ||
            it.namaBarang.contains("Server", ignoreCase = true)
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedRoomFilter by remember { mutableStateOf<String?>(null) }
    var selectedKondisiFilter by remember { mutableStateOf<String?>(null) }

    val filteredPcItems = remember(pcItems, searchQuery, selectedRoomFilter, selectedKondisiFilter) {
        pcItems.filter { item ->
            val matchesSearch = searchQuery.isBlank() ||
                item.namaBarang.contains(searchQuery, ignoreCase = true) ||
                item.idBarang.contains(searchQuery, ignoreCase = true) ||
                item.ruang.contains(searchQuery, ignoreCase = true)
            val matchesRoom = selectedRoomFilter == null || (item.ruang.ifBlank { "LabKom Utama" }) == selectedRoomFilter
            val matchesKondisi = selectedKondisiFilter == null || item.kondisi.equals(selectedKondisiFilter, ignoreCase = true)
            matchesSearch && matchesRoom && matchesKondisi
        }
    }

    val pcReady = remember(filteredPcItems) { filteredPcItems.sumOf { it.stokTersedia } }
    val pcDamaged = remember(filteredPcItems) { filteredPcItems.sumOf { it.stokRusak } }

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. MAIN HEADER / SUMMARY CARD (FIRST)
        LunarisCard(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Laporan LabKom & Perawatan PC",
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepPurpleText,
                    fontSize = 16.sp
                )
                Text(
                    text = "Inventaris PC, unit pemeliharaan, serta rekam jejak perawatan per ruang",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFF3E8FF), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text("PC Ready / Normal", fontSize = 11.sp, color = Color(0xFF6B21A8), fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("$pcReady Unit", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED))
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFFEF2F2), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text("PC Perawatan / Rusak", fontSize = 11.sp, color = Color(0xFF991B1B), fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("$pcDamaged Unit", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                        }
                    }
                }
            }
        }

        // 2. SEARCH & FILTER BAR (SECOND)
        ReportSearchAndFilterBar(
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            selectedKondisiFilter = selectedKondisiFilter,
            onKondisiFilterChange = { selectedKondisiFilter = it },
            selectedRoomFilter = selectedRoomFilter,
            onRoomFilterChange = { selectedRoomFilter = it },
            masterRooms = masterRooms,
            placeholderText = "Ketik untuk mencari..."
        )

        Text(
            text = "Daftar Inventaris PC LabKom ${selectedRoomFilter?.let { "($it)" } ?: ""}",
            fontWeight = FontWeight.ExtraBold,
            color = DeepPurpleText,
            fontSize = 16.sp
        )

        if (filteredPcItems.isEmpty()) {
            EmptyStateView("Tidak ada data inventaris PC LabKom untuk penyaringan ini.")
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                filteredPcItems.forEach { pc ->
                    LunarisCard(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFFEDE9FE), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Computer, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(20.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(pc.namaBarang, fontWeight = FontWeight.Bold, color = DeepPurpleText, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Ruang: ${pc.ruang.ifBlank { "LabKom" }} | Kondisi: ${pc.kondisi.ifBlank { "Baik" }}", fontSize = 12.sp, color = Color.Gray)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${pc.stokTersedia} / ${pc.stokAwal}", fontWeight = FontWeight.Black, color = DeepPurpleText, fontSize = 16.sp)
                                Text("Unit Ready", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

// ==========================================
// TAB 6: LAPORAN MUTASI PERANGKAT CONTENT
// ==========================================
@Composable
fun MutasiTabContent(
    mutasiList: List<MutasiPerangkatEntity>,
    masterRooms: List<String> = emptyList(),
    onNavigateToTab: (Int) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedRuangAsalFilter by remember { mutableStateOf<String?>(null) }
    var selectedRuangTujuanFilter by remember { mutableStateOf<String?>(null) }
    var selectedJenisPerangkatFilter by remember { mutableStateOf<String?>(null) }
    var selectedDatePreset by remember { mutableStateOf("Semua") }
    var filterStartDateText by remember { mutableStateOf("") }
    var filterEndDateText by remember { mutableStateOf("") }

    var showFilterDialog by remember { mutableStateOf(false) }
    var showMutasiQrScanner by remember { mutableStateOf(false) }

    if (showMutasiQrScanner) {
        CameraScannerDialog(
            title = "Pindai QR / Barcode Mutasi",
            onDismissRequest = { showMutasiQrScanner = false },
            onBarcodeScanned = { code ->
                searchQuery = code
                showMutasiQrScanner = false
            }
        )
    }

    val sdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID")) }

    val filteredList = remember(
        mutasiList,
        searchQuery,
        selectedRuangAsalFilter,
        selectedRuangTujuanFilter,
        selectedJenisPerangkatFilter,
        selectedDatePreset,
        filterStartDateText,
        filterEndDateText
    ) {
        mutasiList.filter { item ->
            // Search Query Filter
            val matchesQuery = searchQuery.isBlank() ||
                    item.namaBarang.contains(searchQuery, ignoreCase = true) ||
                    item.idBarang.contains(searchQuery, ignoreCase = true) ||
                    item.idMutasi.contains(searchQuery, ignoreCase = true) ||
                    item.serialNumber.contains(searchQuery, ignoreCase = true) ||
                    item.ruangAsal.contains(searchQuery, ignoreCase = true) ||
                    item.ruangTujuan.contains(searchQuery, ignoreCase = true) ||
                    item.namaPetugas.contains(searchQuery, ignoreCase = true) ||
                    item.alasanMutasi.contains(searchQuery, ignoreCase = true)

            // Room Asal Filter
            val matchesRuangAsal = selectedRuangAsalFilter == null || item.ruangAsal.equals(selectedRuangAsalFilter, ignoreCase = true)

            // Room Tujuan Filter
            val matchesRuangTujuan = selectedRuangTujuanFilter == null || item.ruangTujuan.equals(selectedRuangTujuanFilter, ignoreCase = true)

            // Device Type Filter
            val matchesJenis = selectedJenisPerangkatFilter == null || item.jenisPerangkat.equals(selectedJenisPerangkatFilter, ignoreCase = true)

            // Date Filter
            var matchesDate = true
            if (filterStartDateText.isNotBlank() || filterEndDateText.isNotBlank()) {
                try {
                    val itemDate = sdf.parse(item.tanggalMutasi)
                    if (itemDate != null) {
                        if (filterStartDateText.isNotBlank()) {
                            val startDate = sdf.parse(filterStartDateText)
                            if (startDate != null && itemDate.before(startDate)) matchesDate = false
                        }
                        if (filterEndDateText.isNotBlank()) {
                            val endDate = sdf.parse(filterEndDateText)
                            if (endDate != null && itemDate.after(endDate)) matchesDate = false
                        }
                    }
                } catch (e: Exception) {
                    // Fallback if parsing fails
                }
            }

            matchesQuery && matchesRuangAsal && matchesRuangTujuan && matchesJenis && matchesDate
        }
    }

    val totalMutasiCount = filteredList.size
    val peripheralCount = filteredList.count { it.jenisPerangkat.contains("Peripheral", ignoreCase = true) }
    val labkomCount = filteredList.count {
        it.jenisPerangkat.contains("Komputer", ignoreCase = true) ||
        it.jenisPerangkat.contains("PC", ignoreCase = true) ||
        it.jenisPerangkat.contains("LabKom", ignoreCase = true)
    }

    val isFilterActive = selectedRuangAsalFilter != null ||
            selectedRuangTujuanFilter != null ||
            selectedJenisPerangkatFilter != null ||
            filterStartDateText.isNotBlank() ||
            filterEndDateText.isNotBlank() ||
            selectedDatePreset != "Semua"

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // TOP SUMMARY STAT CARDS (Clean White Card)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SummaryStatMiniCard(
                title = "Total Mutasi",
                value = "$totalMutasiCount",
                sub = "Pencatatan",
                icon = Icons.Default.CloudSync,
                iconBg = Color(0xFFEDE9FE),
                iconTint = Color(0xFF7C3AED),
                modifier = Modifier.weight(1f)
            )
            SummaryStatMiniCard(
                title = "Relokasi Peripheral",
                value = "$peripheralCount",
                sub = "Unit Peripheral",
                icon = Icons.Default.Memory,
                iconBg = Color(0xFFF3E8FF),
                iconTint = Color(0xFF7C3AED),
                modifier = Modifier.weight(1f)
            )
            SummaryStatMiniCard(
                title = "Relokasi PC / Lab",
                value = "$labkomCount",
                sub = "Unit PC/Komputer",
                icon = Icons.Default.Computer,
                iconBg = Color(0xFFEDE9FE),
                iconTint = Color(0xFF6D28D9),
                modifier = Modifier.weight(1f)
            )
        }

        // SEARCH & FILTER BAR
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Ketik untuk mencari...", fontSize = 13.sp, color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cari", tint = Color(0xFF7C3AED), modifier = Modifier.size(20.dp)) },
                trailingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Hapus", tint = Color.Gray, modifier = Modifier.size(18.dp))
                            }
                        }
                        IconButton(
                            onClick = { showMutasiQrScanner = true },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("btn_scan_qr_mutasi")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Pindai QR Kode",
                                tint = Color(0xFF7C3AED),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Color(0xFF7C3AED),
                    unfocusedBorderColor = Color(0xFFCBD5E1)
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("laporan_mutasi_search_input")
            )

            // FILTER DIALOG BUTTON (Standalone Icon-Only Button)
            Surface(
                onClick = { showFilterDialog = true },
                shape = RoundedCornerShape(14.dp),
                color = if (isFilterActive) Color(0xFF7C3AED) else Color.White,
                border = BorderStroke(1.dp, if (isFilterActive) Color(0xFF6D28D9) else Color(0xFFCBD5E1)),
                modifier = Modifier
                    .size(50.dp)
                    .testTag("laporan_mutasi_filter_btn")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter",
                        tint = if (isFilterActive) Color.White else Color(0xFF475569),
                        modifier = Modifier.size(22.dp)
                    )
                    if (isFilterActive) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(5.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }
            }
        }

        // MUTASI LOG CARDS LIST
        if (filteredList.isEmpty()) {
            LunarisCard(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Tidak Ada Record Mutasi",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = DeepPurpleText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Belum ada riwayat mutasi perangkat yang sesuai dengan kriteria filter.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                filteredList.forEachIndexed { index, item ->
                    MutasiCardItem(item = item, index = index)
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }

    // COMPACT SIDE-BY-SIDE GRID FILTER POP-UP DIALOG
    if (showFilterDialog) {
        var showFilterStartPicker by remember { mutableStateOf(false) }
        var showFilterEndPicker by remember { mutableStateOf(false) }

        if (showFilterStartPicker) {
            val isoDate = try {
                if (filterStartDateText.contains("/")) {
                    val p = filterStartDateText.split("/")
                    if (p.size == 3) "${p[2]}-${p[1]}-${p[0]}" else filterStartDateText
                } else filterStartDateText
            } catch (_: Exception) { filterStartDateText }

            LunarisDatePickerDialog(
                onDismissRequest = { showFilterStartPicker = false },
                selectedDateString = isoDate,
                onDateSelected = { newIso ->
                    val formatted = try {
                        val p = newIso.split("-")
                        if (p.size == 3) "${p[2]}/${p[1]}/${p[0]}" else newIso
                    } catch (_: Exception) { newIso }
                    filterStartDateText = formatted
                    selectedDatePreset = "Kustom"
                }
            )
        }

        if (showFilterEndPicker) {
            val isoDate = try {
                if (filterEndDateText.contains("/")) {
                    val p = filterEndDateText.split("/")
                    if (p.size == 3) "${p[2]}-${p[1]}-${p[0]}" else filterEndDateText
                } else filterEndDateText
            } catch (_: Exception) { filterEndDateText }

            LunarisDatePickerDialog(
                onDismissRequest = { showFilterEndPicker = false },
                selectedDateString = isoDate,
                onDateSelected = { newIso ->
                    val formatted = try {
                        val p = newIso.split("-")
                        if (p.size == 3) "${p[2]}/${p[1]}/${p[0]}" else newIso
                    } catch (_: Exception) { newIso }
                    filterEndDateText = formatted
                    selectedDatePreset = "Kustom"
                }
            )
        }

        Dialog(onDismissRequest = { showFilterDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Dialog Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFFEDE9FE), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = null,
                                    tint = Color(0xFF7C3AED),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "Filter Log Mutasi",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = DeepPurpleText
                            )
                        }
                        IconButton(onClick = { showFilterDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.Gray)
                        }
                    }

                    HorizontalDivider(color = Color(0xFFE2E8F0))

                    // SIDE-BY-SIDE GRID LAYOUT
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // KOLOM KIRI (Ruang Asal, Ruang Tujuan, Jenis Perangkat)
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "📌 Lokasi & Jenis",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = Color(0xFF7C3AED)
                            )

                            // Ruang Asal Dropdown
                            DropdownFilterSelector(
                                label = "Ruang Asal",
                                selectedValue = selectedRuangAsalFilter ?: "Semua Ruang Asal",
                                options = listOf("Semua Ruang Asal") + masterRooms,
                                onOptionSelected = { selected ->
                                    selectedRuangAsalFilter = if (selected == "Semua Ruang Asal") null else selected
                                }
                            )

                            // Ruang Tujuan Dropdown
                            DropdownFilterSelector(
                                label = "Ruang Tujuan",
                                selectedValue = selectedRuangTujuanFilter ?: "Semua Ruang Tujuan",
                                options = listOf("Semua Ruang Tujuan") + masterRooms,
                                onOptionSelected = { selected ->
                                    selectedRuangTujuanFilter = if (selected == "Semua Ruang Tujuan") null else selected
                                }
                            )

                            // Jenis Perangkat Dropdown
                            DropdownFilterSelector(
                                label = "Jenis Perangkat",
                                selectedValue = selectedJenisPerangkatFilter ?: "Semua Jenis",
                                options = listOf("Semua Jenis", "Peripheral", "Komputer / PC / LabKom"),
                                onOptionSelected = { selected ->
                                    selectedJenisPerangkatFilter = if (selected == "Semua Jenis") null else selected
                                }
                            )
                        }

                        // Vertical Divider
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(220.dp)
                                .background(Color(0xFFE2E8F0))
                        )

                        // KOLOM KANAN (Periode Tanggal Mutasi)
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "📅 Periode Tanggal",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = Color(0xFF7C3AED)
                            )

                            // Date Preset Selector
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Preset Waktu", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    listOf("Semua", "Hari Ini", "Bulan Ini").forEach { preset ->
                                        val isSelected = selectedDatePreset == preset
                                        Surface(
                                            onClick = {
                                                selectedDatePreset = preset
                                                val todayStr = sdf.format(Date())
                                                when (preset) {
                                                    "Hari Ini" -> {
                                                        filterStartDateText = todayStr
                                                        filterEndDateText = todayStr
                                                    }
                                                    "Bulan Ini" -> {
                                                        val cal = Calendar.getInstance()
                                                        cal.set(Calendar.DAY_OF_MONTH, 1)
                                                        filterStartDateText = sdf.format(cal.time)
                                                        filterEndDateText = todayStr
                                                    }
                                                    else -> {
                                                        filterStartDateText = ""
                                                        filterEndDateText = ""
                                                    }
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSelected) Color(0xFF7C3AED) else Color(0xFFF1F5F9),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = preset,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else Color.DarkGray,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Start Date Input Button
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Dari Tanggal", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                                OutlinedButton(
                                    onClick = { showFilterStartPicker = true },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFF8FAFC)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = filterStartDateText.ifBlank { "dd/mm/yyyy" },
                                            fontSize = 11.sp,
                                            color = if (filterStartDateText.isNotBlank()) DeepPurpleText else Color.Gray
                                        )
                                        Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }

                            // End Date Input Button
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Sampai Tanggal", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                                OutlinedButton(
                                    onClick = { showFilterEndPicker = true },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFF8FAFC)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = filterEndDateText.ifBlank { "dd/mm/yyyy" },
                                            fontSize = 11.sp,
                                            color = if (filterEndDateText.isNotBlank()) DeepPurpleText else Color.Gray
                                        )
                                        Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFE2E8F0))

                    // Dialog Actions (Reset & Terapkan Filter)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                selectedRuangAsalFilter = null
                                selectedRuangTujuanFilter = null
                                selectedJenisPerangkatFilter = null
                                selectedDatePreset = "Semua"
                                filterStartDateText = ""
                                filterEndDateText = ""
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("laporan_mutasi_reset_filter_btn")
                        ) {
                            Text("Reset", color = Color.DarkGray, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Button(
                            onClick = { showFilterDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("laporan_mutasi_apply_filter_btn")
                        ) {
                            Text("Terapkan Filter", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// ITEM CARD FOR MUTASI LIST (Clean White Card)
// ==========================================
@Composable
fun MutasiCardItem(
    item: MutasiPerangkatEntity,
    index: Int
) {
    val isPeripheral = item.jenisPerangkat.contains("Peripheral", ignoreCase = true)
    val cardIcon = if (isPeripheral) Icons.Default.Memory else Icons.Default.Computer
    val badgeBg = if (isPeripheral) Color(0xFFE0F2FE) else Color(0xFFDCFCE7)
    val badgeTint = if (isPeripheral) Color(0xFF0284C7) else Color(0xFF16A34A)

    LunarisCard(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mutasi_card_item_$index")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Device Name, Badge, Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(badgeBg, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = cardIcon,
                            contentDescription = null,
                            tint = badgeTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = item.namaBarang,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = DeepPurpleText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.idBarang.ifBlank { item.idMutasi.ifBlank { item.serialNumber } },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B)
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    text = item.jenisPerangkat,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.DarkGray,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFAF5FF)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = null,
                            tint = Color(0xFF7C3AED),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = item.tanggalMutasi,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7C3AED)
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            // Relocation Route Box (Ruang Asal -> Ruang Tujuan)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Ruang Asal", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                        Text(item.ruangAsal, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                    }

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(0xFFEDE9FE), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Pindah Ke",
                            tint = Color(0xFF7C3AED),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text("Ruang Tujuan", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                        Text(item.ruangTujuan, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                    }
                }
            }

            // Footer: Petugas & Alasan
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Text(
                        text = "Petugas: ${item.namaPetugas}",
                        fontSize = 11.sp,
                        color = Color.DarkGray,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (item.alasanMutasi.isNotBlank()) {
                    Text(
                        text = "Alasan: ${item.alasanMutasi}",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// Dropdown Filter Helper Component
@Composable
fun DropdownFilterSelector(
    label: String,
    selectedValue: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFF8FAFC)),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedValue,
                        fontSize = 11.sp,
                        color = DeepPurpleText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option, fontSize = 12.sp) },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

// Summary Stat Mini Card Component
@Composable
fun SummaryStatMiniCard(
    title: String,
    value: String,
    sub: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    LunarisCard(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconBg, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    fontSize = 10.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepPurpleText
                )
                Text(
                    text = sub,
                    fontSize = 9.sp,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

// ==========================================
// TAB 7: SIRKULASI PEMINJAMAN & PENGEMBALIAN CONTENT
// ==========================================
@Composable
fun SirkulasiTabContent(
    borrowed: List<BorrowedLineItem>,
    returned: List<ReturnedLineItem>,
    masterRooms: List<String> = emptyList(),
    onNavigateToTab: (Int) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedRoomFilter by remember { mutableStateOf<String?>(null) }
    var selectedKondisiFilter by remember { mutableStateOf<String?>(null) }
    var selectedSubTab by remember { mutableIntStateOf(0) } // 0: Semua, 1: Dipinjam, 2: Dikembalikan

    val filteredBorrowed = remember(borrowed, searchQuery, selectedKondisiFilter) {
        val kFilter = selectedKondisiFilter
        borrowed.filter { item ->
            val matchesSearch = searchQuery.isBlank() ||
                item.namaBarang.contains(searchQuery, ignoreCase = true) ||
                item.namaPeminjam.contains(searchQuery, ignoreCase = true) ||
                item.kelas.contains(searchQuery, ignoreCase = true) ||
                (item.tujuanPeminjaman?.contains(searchQuery, ignoreCase = true) == true)
            val matchesKondisi = kFilter == null || item.status.contains(kFilter, ignoreCase = true)
            matchesSearch && matchesKondisi
        }
    }

    val filteredReturned = remember(returned, searchQuery) {
        returned.filter { item ->
            searchQuery.isBlank() ||
                item.namaBarang.contains(searchQuery, ignoreCase = true) ||
                item.namaPeminjam.contains(searchQuery, ignoreCase = true) ||
                item.kelas.contains(searchQuery, ignoreCase = true)
        }
    }

    val activeCount = remember(filteredBorrowed) { filteredBorrowed.count { it.status == "Dipinjam" } }
    val returnedCount = remember(filteredReturned) { filteredReturned.size }
    val totalCount = activeCount + returnedCount

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. MAIN HEADER / SUMMARY CARD (FIRST)
        LunarisCard(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Laporan Terpadu Sirkulasi Peminjaman & Pengembalian",
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepPurpleText,
                    fontSize = 16.sp
                )
                Text(
                    text = "Rekapitulasi sirkulasi peminjaman dan pengembalian alat dalam 1 laporan ringkas",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFEFF6FF), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFDBEAFE), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Sync, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sedang Dipinjam", fontSize = 11.sp, color = Color(0xFF6B21A8), fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("$activeCount Transaksi", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF7C3AED))
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFECFDF5), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFA7F3D0), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AssignmentReturn, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sudah Dikembalikan", fontSize = 11.sp, color = Color(0xFF065F46), fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("$returnedCount Transaksi", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF059669))
                        }
                    }
                }
            }
        }

        // 2. SEARCH & FILTER BAR (SECOND)
        ReportSearchAndFilterBar(
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            selectedKondisiFilter = selectedKondisiFilter,
            onKondisiFilterChange = { selectedKondisiFilter = it },
            selectedRoomFilter = selectedRoomFilter,
            onRoomFilterChange = { selectedRoomFilter = it },
            masterRooms = masterRooms,
            placeholderText = "Ketik untuk mencari..."
        )

        // Sub-Tab Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                label = { Text("Semua ($totalCount)", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = DeepPurpleText,
                    selectedLabelColor = Color.White
                )
            )
            FilterChip(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                label = { Text("Sedang Dipinjam ($activeCount)", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF7C3AED),
                    selectedLabelColor = Color.White
                )
            )
            FilterChip(
                selected = selectedSubTab == 2,
                onClick = { selectedSubTab = 2 },
                label = { Text("Dikembalikan ($returnedCount)", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF059669),
                    selectedLabelColor = Color.White
                )
            )
        }

        Text("Daftar Sirkulasi Alat Terpadu", fontWeight = FontWeight.ExtraBold, color = DeepPurpleText, fontSize = 16.sp)

        val showBorrowedList = selectedSubTab == 0 || selectedSubTab == 1
        val showReturnedList = selectedSubTab == 0 || selectedSubTab == 2

        if ((!showBorrowedList || filteredBorrowed.isEmpty()) && (!showReturnedList || filteredReturned.isEmpty())) {
            EmptyStateView("Tidak ada transaksi peminjaman/pengembalian pada rentang tanggal terpilih.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (showBorrowedList) {
                    filteredBorrowed.forEach { b ->
                        LunarisCard(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .background(Color(0xFFEFF6FF), RoundedCornerShape(10.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Sync, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
                                        }
                                        Column {
                                            Text(b.namaBarang, fontWeight = FontWeight.Bold, color = DeepPurpleText, fontSize = 14.sp)
                                            Text("Peminjam: ${b.namaPeminjam} (${b.kelas})", fontSize = 12.sp, color = Color.Gray)
                                        }
                                    }
                                    Surface(
                                        color = if (b.status == "Kembali") Color(0xFFECFDF5) else Color(0xFFEFF6FF),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = b.status,
                                            color = if (b.status == "Kembali") Color(0xFF059669) else Color(0xFF2563EB),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                if (!b.tujuanPeminjaman.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Tujuan: ${b.tujuanPeminjaman}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF1F5F9))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Petugas: ${b.petugas} | Jumlah: ${b.jumlah} Pcs", fontSize = 11.sp, color = Color.Gray)
                                    Text(b.tanggal, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }

                if (showReturnedList) {
                    filteredReturned.forEach { r ->
                        LunarisCard(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .background(Color(0xFFECFDF5), RoundedCornerShape(10.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.AssignmentReturn, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(20.dp))
                                        }
                                        Column {
                                            Text(r.namaBarang, fontWeight = FontWeight.Bold, color = DeepPurpleText, fontSize = 14.sp)
                                            Text("Pengembali: ${r.namaPeminjam} (${r.kelas})", fontSize = 12.sp, color = Color.Gray)
                                        }
                                    }
                                    Surface(
                                        color = Color(0xFFECFDF5),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "Dikembalikan",
                                            color = Color(0xFF059669),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF1F5F9))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Petugas Kembali: ${r.petugasKembali} | Jumlah: ${r.jumlah} Pcs", fontSize = 11.sp, color = Color.Gray)
                                    Text(r.tanggalKembali, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

// ==========================================
// CUSTOM INTERACTIVE GRAPHIC DRAWINGS (CANVAS)
// ==========================================
@Composable
fun GlassmorphicPieChart(
    data: Map<String, Float>,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    val total = data.values.sum()
    if (total == 0f) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(130.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Tidak ada data untuk grafik", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
        return
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Canvas(modifier = Modifier.size(110.dp)) {
            var startAngle = 0f
            data.entries.forEachIndexed { index, entry ->
                val sweepAngle = (entry.value / total) * 360f
                val color = colors[index % colors.size]
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    size = size
                )
                startAngle += sweepAngle
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            data.entries.forEachIndexed { index, entry ->
                val color = colors[index % colors.size]
                val percentage = if (total > 0) (entry.value / total) * 100 else 0f
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(color, RoundedCornerShape(3.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${entry.key}: ${entry.value.toInt()} (${String.format(Locale.US, "%.1f", percentage)}%)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = DeepPurpleText
                    )
                }
            }
        }
    }
}

@Composable
fun GlassmorphicBarChart(
    data: Map<String, Float>,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    val maxVal = data.values.maxOrNull() ?: 0f
    if (maxVal == 0f) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(130.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Tidak ada data untuk grafik", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        data.entries.forEach { entry ->
            val fraction = entry.value / maxVal
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(entry.key, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = DeepPurpleText)
                    Text("${entry.value.toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(4.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .fillMaxHeight()
                            .background(barColor, RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = Color.LightGray,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ==========================================
// FILE OPERATIONS & SCOPED STORAGE HELPER
// ==========================================
fun saveReportToAutoPath(
    context: Context,
    folderName: String,
    filename: String,
    bytes: ByteArray
): File? {
    try {
        // 1. Target directory: /storage/emulated/0/Lunaris/Unduh Laporan/[folderName]
        // In Indonesian storage naming, external storage root is the "Penyimpanan Internal"
        val storageRoot = Environment.getExternalStorageDirectory()
        val targetDir = File(storageRoot, "Lunaris/Unduh Laporan/$folderName")
        
        var finalDir = targetDir
        var canWrite = false
        
        try {
            if (!finalDir.exists()) {
                finalDir.mkdirs()
            }
            val testFile = File(finalDir, ".test")
            if (testFile.createNewFile()) {
                testFile.delete()
                canWrite = true
            }
        } catch (e: Exception) {
            canWrite = false
        }
        
        // 2. Fallback to /storage/emulated/0/Download/Lunaris/Unduh Laporan/[folderName]
        if (!canWrite) {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            finalDir = File(downloadsDir, "Lunaris/Unduh Laporan/$folderName")
            try {
                if (!finalDir.exists()) {
                    finalDir.mkdirs()
                }
                val testFile = File(finalDir, ".test")
                if (testFile.createNewFile()) {
                    testFile.delete()
                    canWrite = true
                }
            } catch (e: Exception) {
                canWrite = false
            }
        }
        
        // 3. Fallback to external files dir: /storage/emulated/0/Android/data/[package]/files/Lunaris/...
        if (!canWrite) {
            finalDir = File(context.getExternalFilesDir(null), "Lunaris/Unduh Laporan/$folderName")
            if (!finalDir.exists()) {
                finalDir.mkdirs()
            }
        }
        
        val destFile = File(finalDir, filename)
        destFile.outputStream().use { os ->
            os.write(bytes)
        }
        return destFile
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

fun writeBytesToUri(context: Context, uri: android.net.Uri, bytes: ByteArray): Boolean {
    return try {
        context.contentResolver.openOutputStream(uri)?.use { os ->
            os.write(bytes)
            true
        } ?: false
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

fun shareFile(context: Context, uri: android.net.Uri, mimeType: String) {
    try {
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Bagikan Laporan"))
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal membagikan file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

fun openFile(context: Context, uri: android.net.Uri, mimeType: String) {
    try {
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Tidak ada aplikasi untuk membuka file ini. Silakan pasang aplikasi penampil PDF/Excel/CSV terlebih dahulu.", Toast.LENGTH_LONG).show()
    }
}

// ==============================================
// NATIVE FORMAT GENERATORS (WORD, EXCEL, PDF)
// ==============================================

private fun createZipPackage(entries: Map<String, String>): ByteArray {
    val bos = java.io.ByteArrayOutputStream()
    java.util.zip.ZipOutputStream(bos).use { zos ->
        for ((path, content) in entries) {
            val entry = java.util.zip.ZipEntry(path)
            zos.putNextEntry(entry)
            zos.write(content.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }
    }
    return bos.toByteArray()
}

private fun getExcelColumnName(index: Int): String {
    var num = index
    val sb = java.lang.StringBuilder()
    while (num >= 0) {
        sb.insert(0, ('A' + (num % 26)))
        num = (num / 26) - 1
    }
    return sb.toString()
}

private fun escapeXml(text: String): String {
    return text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}

fun generateWordBytes(title: String, period: String, headers: List<String>, rows: List<List<String>>, kopLaporan: KopLaporanEntity? = null): ByteArray {
    val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>"""

    val rootRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""

    val docRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
</Relationships>"""

    val docXml = java.lang.StringBuilder()
    docXml.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <w:body>
""")

    // Kop Laporan Header
    kopLaporan?.let { kop ->
        val order = parseKopRowOrder(kop.rowOrder)
        for (key in order) {
            val text = when (key) {
                "pemprov" -> kop.pemprovHeader
                "dinas" -> kop.dinasHeader
                "sekolah1" -> kop.sekolahBaris1
                "sekolah2" -> kop.sekolahBaris2
                "alamat1" -> kop.alamatBaris1
                "alamat2" -> kop.alamatBaris2
                "alamat3" -> kop.alamatBaris3
                "lainnya" -> kop.lainnyaHeader
                else -> ""
            }
            if (text.isNotBlank()) {
                val isBold = key in listOf("pemprov", "dinas", "sekolah1", "sekolah2")
                val szVal = if (isBold) "26" else "20"
                docXml.append("    <w:p><w:pPr><w:jc w:val=\"center\"/><w:spacing w:after=\"40\"/></w:pPr>")
                docXml.append("<w:r><w:rPr>")
                if (isBold) docXml.append("<w:b/>")
                docXml.append("<w:rFonts w:ascii=\"Times New Roman\" w:hAnsi=\"Times New Roman\"/><w:sz w:val=\"$szVal\"/></w:rPr>")
                docXml.append("<w:t xml:space=\"preserve\">").append(escapeXml(text)).append("</w:t></w:r></w:p>\n")
            }
        }
        docXml.append("    <w:p><w:pPr><w:pBdr><w:bottom w:val=\"double\" w:sz=\"12\" w:space=\"4\" w:color=\"000000\"/></w:pBdr><w:spacing w:after=\"200\"/></w:pPr></w:p>\n")
    }

    // Title & Period
    docXml.append("    <w:p><w:pPr><w:spacing w:before=\"100\" w:after=\"60\"/></w:pPr><w:r><w:rPr><w:b/><w:sz w:val=\"28\"/><w:color w:val=\"3B0764\"/></w:rPr><w:t xml:space=\"preserve\">")
    docXml.append(escapeXml(title)).append("</w:t></w:r></w:p>\n")
    if (period.isNotBlank()) {
        docXml.append("    <w:p><w:pPr><w:spacing w:after=\"160\"/></w:pPr><w:r><w:rPr><w:i/><w:sz w:val=\"20\"/><w:color w:val=\"475569\"/></w:rPr><w:t xml:space=\"preserve\">")
        docXml.append(escapeXml(period)).append("</w:t></w:r></w:p>\n")
    }

    // Data Table
    docXml.append("""    <w:tbl>
      <w:tblPr>
        <w:tblW w:w="5000" w:type="pct"/>
        <w:tblBorders>
          <w:top w:val="single" w:sz="4" w:space="0" w:color="CBD5E1"/>
          <w:left w:val="single" w:sz="4" w:space="0" w:color="CBD5E1"/>
          <w:bottom w:val="single" w:sz="4" w:space="0" w:color="CBD5E1"/>
          <w:right w:val="single" w:sz="4" w:space="0" w:color="CBD5E1"/>
          <w:insideH w:val="single" w:sz="4" w:space="0" w:color="CBD5E1"/>
          <w:insideV w:val="single" w:sz="4" w:space="0" w:color="CBD5E1"/>
        </w:tblBorders>
      </w:tblPr>
""")

    // Header Row
    docXml.append("      <w:tr>\n")
    headers.forEach { h ->
        docXml.append("        <w:tc><w:tcPr><w:shd w:val=\"clear\" w:color=\"auto\" w:fill=\"6D28D9\"/></w:tcPr><w:p><w:pPr><w:jc w:val=\"center\"/><w:spacing w:before=\"80\" w:after=\"80\"/></w:pPr><w:r><w:rPr><w:b/><w:color w:val=\"FFFFFF\"/><w:sz w:val=\"20\"/></w:rPr><w:t xml:space=\"preserve\">")
        docXml.append(escapeXml(h)).append("</w:t></w:r></w:p></w:tc>\n")
    }
    docXml.append("      </w:tr>\n")

    // Data Rows
    rows.forEach { row ->
        docXml.append("      <w:tr>\n")
        headers.indices.forEach { colIdx ->
            val valStr = row.getOrNull(colIdx) ?: ""
            docXml.append("        <w:tc><w:p><w:pPr><w:spacing w:before=\"60\" w:after=\"60\"/></w:pPr><w:r><w:rPr><w:sz w:val=\"18\"/><w:color w:val=\"0F172A\"/></w:rPr><w:t xml:space=\"preserve\">")
            docXml.append(escapeXml(valStr)).append("</w:t></w:r></w:p></w:tc>\n")
        }
        docXml.append("      </w:tr>\n")
    }
    docXml.append("    </w:tbl>\n")

    // Signature Footer
    kopLaporan?.let { kop ->
        val activeSigners = parseTtdSigners(kop.ttdSignersJson).filter { it.isEnabled }
        val tempatTanggalText = kop.tempatTanggal.trim()

        if (activeSigners.isNotEmpty() || tempatTanggalText.isNotBlank()) {
            docXml.append("    <w:p><w:pPr><w:spacing w:before=\"300\"/></w:pPr></w:p>\n")
            val signersToRender = if (activeSigners.isEmpty()) {
                listOf(TtdSignerItem(jabatan = "Kepala Sekolah", nama = "", nip = ""))
            } else activeSigners

            val count = signersToRender.size
            docXml.append("""    <w:tbl>
      <w:tblPr>
        <w:tblW w:w="5000" w:type="pct"/>
        <w:tblBorders>
          <w:top w:val="none"/><w:left w:val="none"/><w:bottom w:val="none"/><w:right w:val="none"/>
          <w:insideH w:val="none"/><w:insideV w:val="none"/>
        </w:tblBorders>
      </w:tblPr>
      <w:tr>
""")
            signersToRender.forEachIndexed { idx, signer ->
                docXml.append("        <w:tc><w:p><w:pPr><w:jc w:val=\"center\"/><w:spacing w:after=\"40\"/></w:pPr>")
                if (idx == count - 1 && tempatTanggalText.isNotBlank()) {
                    docXml.append("<w:r><w:rPr><w:sz w:val=\"19\"/></w:rPr><w:t xml:space=\"preserve\">").append(escapeXml(tempatTanggalText)).append("</w:t></w:r>")
                } else {
                    docXml.append("<w:r><w:rPr><w:sz w:val=\"19\"/></w:rPr><w:t xml:space=\"preserve\"> </w:t></w:r>")
                }
                docXml.append("</w:p>")

                val jbt = if (signer.jabatan.isNotBlank()) signer.jabatan else "Jabatan..."
                docXml.append("<w:p><w:pPr><w:jc w:val=\"center\"/><w:spacing w:after=\"600\"/></w:pPr><w:r><w:rPr><w:sz w:val=\"19\"/></w:rPr><w:t xml:space=\"preserve\">").append(escapeXml(jbt)).append("</w:t></w:r></w:p>")

                val namaStr = if (signer.nama.isNotBlank()) signer.nama else " "
                docXml.append("<w:p><w:pPr><w:jc w:val=\"center\"/><w:spacing w:after=\"40\"/></w:pPr><w:r><w:rPr><w:b/><w:u w:val=\"single\"/><w:sz w:val=\"20\"/></w:rPr><w:t xml:space=\"preserve\">").append(escapeXml(namaStr)).append("</w:t></w:r></w:p>")

                if (signer.nip.isNotBlank()) {
                    val nipLabel = if (signer.nip.uppercase().startsWith("NIP")) signer.nip else "NIP. ${signer.nip}"
                    docXml.append("<w:p><w:pPr><w:jc w:val=\"center\"/></w:pPr><w:r><w:rPr><w:sz w:val=\"18\"/><w:color w:val=\"334155\"/></w:rPr><w:t xml:space=\"preserve\">").append(escapeXml(nipLabel)).append("</w:t></w:r></w:p>")
                } else {
                    docXml.append("<w:p><w:pPr><w:jc w:val=\"center\"/></w:pPr></w:p>")
                }
                docXml.append("</w:tc>\n")
            }
            docXml.append("      </w:tr>\n    </w:tbl>\n")
        }
    }

    docXml.append("""    <w:sectPr>
      <w:pgSz w:w="11906" w:h="16838"/>
      <w:pgMar w:top="1134" w:right="1134" w:bottom="1134" w:left="1134"/>
    </w:sectPr>
  </w:body>
</w:document>""")

    val entries = mapOf(
        "[Content_Types].xml" to contentTypes,
        "_rels/.rels" to rootRels,
        "word/_rels/document.xml.rels" to docRels,
        "word/document.xml" to docXml.toString()
    )

    return createZipPackage(entries)
}

fun generateExcelBytes(title: String, headers: List<String>, rows: List<List<String>>, kopLaporan: KopLaporanEntity? = null): ByteArray {
    val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""

    val rootRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    val workbookRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

    val workbookXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Laporan" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>"""

    val stylesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="5">
    <font><sz val="11"/><color theme="1"/><name val="Calibri"/></font>
    <font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
    <font><b/><sz val="13"/><color rgb="FF3B0764"/><name val="Calibri"/></font>
    <font><b/><sz val="12"/><color rgb="FF1E1B4B"/><name val="Times New Roman"/></font>
    <font><sz val="10"/><color rgb="FF333333"/><name val="Times New Roman"/></font>
  </fonts>
  <fills count="3">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FF6D28D9"/><bgColor indexed="64"/></patternFill></fill>
  </fills>
  <borders count="2">
    <border><left/><right/><top/><bottom/><diagonal/></border>
    <border>
      <left style="thin"><color rgb="FFCBD5E1"/></left>
      <right style="thin"><color rgb="FFCBD5E1"/></right>
      <top style="thin"><color rgb="FFCBD5E1"/></top>
      <bottom style="thin"><color rgb="FFCBD5E1"/></bottom>
    </border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="6">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="2" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center" wrapText="1"/>
    </xf>
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1"/>
    <xf numFmtId="0" fontId="3" fillId="0" borderId="0" xfId="0" applyFont="1" applyAlignment="1">
      <alignment horizontal="center"/>
    </xf>
    <xf numFmtId="0" fontId="4" fillId="0" borderId="0" xfId="0" applyFont="1" applyAlignment="1">
      <alignment horizontal="center"/>
    </xf>
  </cellXfs>
</styleSheet>"""

    val sheetXml = java.lang.StringBuilder()
    sheetXml.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <cols>
    <col min="1" max="25" width="22" customWidth="1"/>
  </cols>
  <sheetData>
""")

    var rowIndex = 1
    val numCols = maxOf(headers.size, 1)

    // Kop Laporan
    kopLaporan?.let { kop ->
        val order = parseKopRowOrder(kop.rowOrder)
        for (key in order) {
            val text = when (key) {
                "pemprov" -> kop.pemprovHeader
                "dinas" -> kop.dinasHeader
                "sekolah1" -> kop.sekolahBaris1
                "sekolah2" -> kop.sekolahBaris2
                "alamat1" -> kop.alamatBaris1
                "alamat2" -> kop.alamatBaris2
                "alamat3" -> kop.alamatBaris3
                "lainnya" -> kop.lainnyaHeader
                else -> ""
            }
            if (text.isNotBlank()) {
                val styleId = if (key in listOf("pemprov", "dinas", "sekolah1", "sekolah2")) "4" else "5"
                sheetXml.append("    <row r=\"$rowIndex\" ht=\"20\" customHeight=\"1\">\n")
                sheetXml.append("      <c r=\"A$rowIndex\" t=\"inlineStr\" s=\"$styleId\"><is><t>")
                sheetXml.append(escapeXml(text))
                sheetXml.append("</t></is></c>\n    </row>\n")
                rowIndex++
            }
        }
        rowIndex++
    }

    // Title
    sheetXml.append("    <row r=\"$rowIndex\" ht=\"24\" customHeight=\"1\">\n")
    sheetXml.append("      <c r=\"A$rowIndex\" t=\"inlineStr\" s=\"1\"><is><t>")
    sheetXml.append(escapeXml(title))
    sheetXml.append("</t></is></c>\n    </row>\n")
    rowIndex++
    rowIndex++

    // Header Row
    sheetXml.append("    <row r=\"$rowIndex\" ht=\"26\" customHeight=\"1\">\n")
    headers.forEachIndexed { colIdx, h ->
        val colRef = "${getExcelColumnName(colIdx)}$rowIndex"
        sheetXml.append("      <c r=\"$colRef\" t=\"inlineStr\" s=\"2\"><is><t>")
        sheetXml.append(escapeXml(h))
        sheetXml.append("</t></is></c>\n")
    }
    sheetXml.append("    </row>\n")
    rowIndex++

    // Data Rows
    rows.forEach { row ->
        sheetXml.append("    <row r=\"$rowIndex\" ht=\"20\" customHeight=\"1\">\n")
        headers.indices.forEach { colIdx ->
            val colRef = "${getExcelColumnName(colIdx)}$rowIndex"
            val valStr = row.getOrNull(colIdx) ?: ""
            sheetXml.append("      <c r=\"$colRef\" t=\"inlineStr\" s=\"3\"><is><t>")
            sheetXml.append(escapeXml(valStr))
            sheetXml.append("</t></is></c>\n")
        }
        sheetXml.append("    </row>\n")
        rowIndex++
    }

    // Signature Footer
    kopLaporan?.let { kop ->
        val activeSigners = parseTtdSigners(kop.ttdSignersJson).filter { it.isEnabled }
        val tempatTanggalText = kop.tempatTanggal.trim()

        if (activeSigners.isNotEmpty() || tempatTanggalText.isNotBlank()) {
            rowIndex += 2

            val signersToRender = if (activeSigners.isEmpty()) {
                listOf(TtdSignerItem(jabatan = "Kepala Sekolah", nama = "", nip = ""))
            } else activeSigners

            val count = signersToRender.size

            if (tempatTanggalText.isNotBlank()) {
                val lastColRef = "${getExcelColumnName(numCols - 1)}$rowIndex"
                sheetXml.append("    <row r=\"$rowIndex\" ht=\"18\" customHeight=\"1\">\n")
                sheetXml.append("      <c r=\"$lastColRef\" t=\"inlineStr\" s=\"5\"><is><t>")
                sheetXml.append(escapeXml(tempatTanggalText))
                sheetXml.append("</t></is></c>\n    </row>\n")
                rowIndex++
            }

            sheetXml.append("    <row r=\"$rowIndex\" ht=\"18\" customHeight=\"1\">\n")
            signersToRender.forEachIndexed { idx, signer ->
                val colIndex = if (count == 1) numCols - 1 else (idx * (numCols - 1) / maxOf(count - 1, 1))
                val colRef = "${getExcelColumnName(colIndex)}$rowIndex"
                val jbt = if (signer.jabatan.isNotBlank()) signer.jabatan else "Jabatan..."
                sheetXml.append("      <c r=\"$colRef\" t=\"inlineStr\" s=\"5\"><is><t>")
                sheetXml.append(escapeXml(jbt))
                sheetXml.append("</t></is></c>\n")
            }
            sheetXml.append("    </row>\n")

            rowIndex += 3

            sheetXml.append("    <row r=\"$rowIndex\" ht=\"18\" customHeight=\"1\">\n")
            signersToRender.forEachIndexed { idx, signer ->
                val colIndex = if (count == 1) numCols - 1 else (idx * (numCols - 1) / maxOf(count - 1, 1))
                val colRef = "${getExcelColumnName(colIndex)}$rowIndex"
                val namaStr = if (signer.nama.isNotBlank()) signer.nama else ""
                sheetXml.append("      <c r=\"$colRef\" t=\"inlineStr\" s=\"4\"><is><t>")
                sheetXml.append(escapeXml(namaStr))
                sheetXml.append("</t></is></c>\n")
            }
            sheetXml.append("    </row>\n")
            rowIndex++

            sheetXml.append("    <row r=\"$rowIndex\" ht=\"18\" customHeight=\"1\">\n")
            signersToRender.forEachIndexed { idx, signer ->
                val colIndex = if (count == 1) numCols - 1 else (idx * (numCols - 1) / maxOf(count - 1, 1))
                val colRef = "${getExcelColumnName(colIndex)}$rowIndex"
                if (signer.nip.isNotBlank()) {
                    val nipLabel = if (signer.nip.uppercase().startsWith("NIP")) signer.nip else "NIP. ${signer.nip}"
                    sheetXml.append("      <c r=\"$colRef\" t=\"inlineStr\" s=\"5\"><is><t>")
                    sheetXml.append(escapeXml(nipLabel))
                    sheetXml.append("</t></is></c>\n")
                }
            }
            sheetXml.append("    </row>\n")
        }
    }

    sheetXml.append("  </sheetData>\n</worksheet>")

    val entries = mapOf(
        "[Content_Types].xml" to contentTypes,
        "_rels/.rels" to rootRels,
        "xl/_rels/workbook.xml.rels" to workbookRels,
        "xl/workbook.xml" to workbookXml,
        "xl/styles.xml" to stylesXml,
        "xl/worksheets/sheet1.xml" to sheetXml.toString()
    )

    return createZipPackage(entries)
}

fun readXlsxBytes(bytes: ByteArray): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    var sharedStrings = listOf<String>()
    var sheetBytes: ByteArray? = null

    try {
        java.util.zip.ZipInputStream(java.io.ByteArrayInputStream(bytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val entryName = entry.name.lowercase(java.util.Locale.ROOT)
                if (entryName == "xl/sharedstrings.xml") {
                    sharedStrings = parseSharedStringsXml(zis.readBytes())
                } else if (sheetBytes == null && (entryName == "xl/worksheets/sheet1.xml" || (entryName.startsWith("xl/worksheets/sheet") && entryName.endsWith(".xml")))) {
                    sheetBytes = zis.readBytes()
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        if (sheetBytes != null) {
            rows.addAll(parseSheetXml(sheetBytes!!, sharedStrings))
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return rows
}

private fun parseSharedStringsXml(bytes: ByteArray): List<String> {
    val result = mutableListOf<String>()
    try {
        val factory = javax.xml.parsers.DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = false
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(java.io.ByteArrayInputStream(bytes))
        val siList = doc.getElementsByTagName("si")
        for (i in 0 until siList.length) {
            val node = siList.item(i)
            val sb = java.lang.StringBuilder()
            if (node is org.w3c.dom.Element) {
                val tList = node.getElementsByTagName("t")
                if (tList.length > 0) {
                    for (j in 0 until tList.length) {
                        sb.append(tList.item(j).textContent ?: "")
                    }
                } else {
                    sb.append(node.textContent ?: "")
                }
            } else {
                sb.append(node?.textContent ?: "")
            }
            result.add(sb.toString())
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return result
}

private fun parseSheetXml(bytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
    val result = mutableListOf<List<String>>()
    try {
        val factory = javax.xml.parsers.DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = false
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(java.io.ByteArrayInputStream(bytes))
        val rowList = doc.getElementsByTagName("row")
        
        for (i in 0 until rowList.length) {
            val rowNode = rowList.item(i) as? org.w3c.dom.Element ?: continue
            val cList = rowNode.getElementsByTagName("c")
            val cellMap = mutableMapOf<Int, String>()
            var maxColIndex = -1

            for (j in 0 until cList.length) {
                val cElem = cList.item(j) as? org.w3c.dom.Element ?: continue
                val ref = cElem.getAttribute("r")
                val type = cElem.getAttribute("t")
                
                val colIndex = if (ref.isNotBlank()) parseExcelColIndex(ref) else j
                if (colIndex > maxColIndex) maxColIndex = colIndex

                val cellValue: String = when (type) {
                    "s" -> {
                        val vNode = cElem.getElementsByTagName("v").item(0)
                        val idx = vNode?.textContent?.trim()?.toIntOrNull()
                        if (idx != null && idx in sharedStrings.indices) sharedStrings[idx] else ""
                    }
                    "inlineStr" -> {
                        val isNode = cElem.getElementsByTagName("is").item(0) as? org.w3c.dom.Element
                        val tNode = isNode?.getElementsByTagName("t")?.item(0) ?: cElem.getElementsByTagName("t").item(0)
                        tNode?.textContent ?: ""
                    }
                    else -> {
                        val vNode = cElem.getElementsByTagName("v").item(0)
                        val tNode = cElem.getElementsByTagName("t").item(0)
                        vNode?.textContent ?: tNode?.textContent ?: ""
                    }
                }
                cellMap[colIndex] = cellValue
            }

            if (maxColIndex >= 0) {
                val rowData = mutableListOf<String>()
                for (col in 0..maxColIndex) {
                    rowData.add(cellMap[col] ?: "")
                }
                result.add(rowData)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return result
}

private fun parseExcelColIndex(ref: String): Int {
    val letters = ref.takeWhile { it.isLetter() }.uppercase(java.util.Locale.ROOT)
    if (letters.isEmpty()) return -1
    var col = 0
    for (char in letters) {
        col = col * 26 + (char - 'A' + 1)
    }
    return col - 1
}

fun readExcelOrCsvInputStream(inputStream: java.io.InputStream): List<List<String>> {
    val bytes = inputStream.readBytes()
    if (bytes.isEmpty()) return emptyList()

    try {
        val rows = readXlsxBytes(bytes)
        if (rows.isNotEmpty()) {
            return rows
        }
    } catch (_: Exception) {}

    try {
        val reader = java.io.BufferedReader(java.io.InputStreamReader(java.io.ByteArrayInputStream(bytes)))
        val csvLines = mutableListOf<List<String>>()
        var line = reader.readLine()
        
        var delimiter = ','
        if (line != null) {
            if (line.contains(";") && !line.contains(",")) {
                delimiter = ';'
            } else if (line.count { it == ';' } > line.count { it == ',' }) {
                delimiter = ';'
            }
        }
        
        while (line != null) {
            if (line.isNotBlank()) {
                val cols = mutableListOf<String>()
                var cur = StringBuilder()
                var inQuotes = false
                for (ch in line) {
                    if (ch == '"') {
                        inQuotes = !inQuotes
                    } else if (ch == delimiter && !inQuotes) {
                        cols.add(cur.toString().trim())
                        cur = StringBuilder()
                    } else {
                        cur.append(ch)
                    }
                }
                cols.add(cur.toString().trim())
                csvLines.add(cols)
            }
            line = reader.readLine()
        }
        return csvLines
    } catch (_: Exception) {
        return emptyList()
    }
}

fun generatePdfBytes(
    context: Context,
    title: String,
    period: String,
    headers: List<String>,
    rows: List<List<String>>,
    kopLaporan: KopLaporanEntity? = null
): ByteArray {
    val pdfDocument = android.graphics.pdf.PdfDocument()
    
    val pageWidth = 595 // A4 standard width
    val pageHeight = 842 // A4 standard height
    
    var pageNumber = 1
    var pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
    var currentPage = pdfDocument.startPage(pageInfo)
    var canvas = currentPage.canvas
    
    val paintText = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 8.5f
        isAntiAlias = true
    }
    
    val paintHeader = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#3B0764") // DeepPurpleText
        textSize = 13f
        isFakeBoldText = true
        isAntiAlias = true
    }
    
    val paintSub = android.graphics.Paint().apply {
        color = android.graphics.Color.DKGRAY
        textSize = 9f
        isAntiAlias = true
    }
    
    val paintTableHead = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 8.5f
        isFakeBoldText = true
        isAntiAlias = true
    }
    
    val paintTableHeadBg = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#6D28D9") // Deep Purple
    }
    
    val paintBorder = android.graphics.Paint().apply {
        color = android.graphics.Color.LTGRAY
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 0.5f
    }
    
    val paintBgAlternate = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#FAF5FF")
    }

    var y = 35f

    // Draw Kop Laporan Header if provided
    val kop = kopLaporan ?: KopLaporanEntity()
    
    // Draw Logo Kiri
    if (kop.logoKiriPath.isNotBlank() && java.io.File(kop.logoKiriPath).exists()) {
        try {
            val bmp = android.graphics.BitmapFactory.decodeFile(kop.logoKiriPath)
            if (bmp != null) {
                val destRect = android.graphics.RectF(40f, y, 92f, y + 52f)
                canvas.drawBitmap(bmp, null, destRect, null)
            }
        } catch (e: Exception) {
            android.util.Log.e("PDF", "Error drawing logo kiri", e)
        }
    }

    // Draw Logo Kanan
    if (kop.logoKananPath.isNotBlank() && java.io.File(kop.logoKananPath).exists()) {
        try {
            val bmp = android.graphics.BitmapFactory.decodeFile(kop.logoKananPath)
            if (bmp != null) {
                val destRect = android.graphics.RectF(503f, y, 555f, y + 52f)
                canvas.drawBitmap(bmp, null, destRect, null)
            }
        } catch (e: Exception) {
            android.util.Log.e("PDF", "Error drawing logo kanan", e)
        }
    }

    // Draw Kop Text Lines (Centered) according to rowOrder
    val textCenterX = pageWidth / 2f

    val kopFontTypeface = when (kop.kopFontFamily.uppercase()) {
        "TIMES NEW ROMAN", "SERIF" -> android.graphics.Typeface.SERIF
        "COURIER", "MONOSPACE" -> android.graphics.Typeface.MONOSPACE
        else -> android.graphics.Typeface.SANS_SERIF
    }

    fun drawKopTextLine(text: String, fontSizePt: Int, isBold: Boolean) {
        if (text.isBlank()) return
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = fontSizePt * 0.72f
            isFakeBoldText = isBold
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = kopFontTypeface
        }
        canvas.drawText(text, textCenterX, y + (fontSizePt * 0.72f), paint)
        y += (fontSizePt * 0.72f) + 2.5f
    }

    val orderedKeys = parseKopRowOrder(kop.rowOrder)
    for (key in orderedKeys) {
        when (key) {
            "pemprov" -> drawKopTextLine(kop.pemprovHeader.uppercase(), kop.pemprovFontSize, true)
            "dinas" -> drawKopTextLine(kop.dinasHeader.uppercase(), kop.dinasFontSize, true)
            "sekolah1" -> drawKopTextLine(kop.sekolahBaris1.uppercase(), kop.sekolahBaris1FontSize, true)
            "sekolah2" -> drawKopTextLine(kop.sekolahBaris2.uppercase(), kop.sekolahBaris2FontSize, true)
            "alamat1" -> drawKopTextLine(kop.alamatBaris1, kop.alamatBaris1FontSize, false)
            "alamat2" -> drawKopTextLine(kop.alamatBaris2, kop.alamatBaris2FontSize, false)
            "alamat3" -> drawKopTextLine(kop.alamatBaris3, kop.alamatBaris3FontSize, false)
            "lainnya" -> drawKopTextLine(kop.lainnyaHeader, kop.lainnyaFontSize, false)
        }
    }

    y += 3f

    // Draw Kedinasan Double Line Separator
    val paintThickLine = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        strokeWidth = 2.0f
        style = android.graphics.Paint.Style.STROKE
    }
    val paintThinLine = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        strokeWidth = 0.8f
        style = android.graphics.Paint.Style.STROKE
    }

    canvas.drawLine(40f, y, 555f, y, paintThickLine)
    y += 3f
    canvas.drawLine(40f, y, 555f, y, paintThinLine)
    y += 18f

    // Header Title
    canvas.drawText(title, 40f, y, paintHeader)
    y += 16f
    canvas.drawText("Periode: $period", 40f, y, paintSub)
    y += 24f
    
    // Dynamic Columns distribution widths
    val numCols = maxOf(headers.size, 1)
    val totalTableWidth = 515f // 555f - 40f
    val colWidths = FloatArray(numCols)
    val colPositions = FloatArray(numCols)

    val isDefault7 = numCols == 7
    val defaultWidths = floatArrayOf(100f, 45f, 45f, 105f, 70f, 65f, 85f)
    var currentX = 40f
    for (i in 0 until numCols) {
        colPositions[i] = currentX
        val w = if (isDefault7) defaultWidths[i] else (totalTableWidth / numCols)
        colWidths[i] = w
        currentX += w
    }
    
    // Draw Header Table
    canvas.drawRect(40f, y, 555f, y + 20f, paintTableHeadBg)
    for (i in 0 until numCols) {
        val hText = headers.getOrElse(i) { "" }
        canvas.drawText(hText, colPositions[i] + 4f, y + 13f, paintTableHead)
    }
    y += 20f
    
    // Draw Rows
    rows.forEachIndexed { rowIndex, row ->
        if (y + 24f > pageHeight - 50f) {
            pdfDocument.finishPage(currentPage)
            pageNumber++
            pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            currentPage = pdfDocument.startPage(pageInfo)
            canvas = currentPage.canvas
            y = 50f
            
            // Repeat Header
            canvas.drawRect(40f, y, 555f, y + 20f, paintTableHeadBg)
            for (i in 0 until numCols) {
                val hText = headers.getOrElse(i) { "" }
                canvas.drawText(hText, colPositions[i] + 4f, y + 13f, paintTableHead)
            }
            y += 20f
        }
        
        // Alternate Background Draw
        if (rowIndex % 2 == 1) {
            canvas.drawRect(40f, y, 555f, y + 20f, paintBgAlternate)
        }
        
        // Border Rectangle
        canvas.drawRect(40f, y, 555f, y + 20f, paintBorder)
        for (i in 1 until numCols) {
            canvas.drawLine(colPositions[i], y, colPositions[i], y + 20f, paintBorder)
        }
        
        // Write cells values
        for (i in 0 until numCols) {
            val rawValue = row.getOrNull(i) ?: ""
            val paint = paintText
            val availableWidth = colWidths[i] - 8f
            var textToDraw = rawValue
            if (paint.measureText(textToDraw) > availableWidth) {
                while (textToDraw.isNotEmpty() && paint.measureText("$textToDraw...") > availableWidth) {
                    textToDraw = textToDraw.substring(0, textToDraw.length - 1)
                }
                textToDraw = "$textToDraw..."
            }
            canvas.drawText(textToDraw, colPositions[i] + 4f, y + 13f, paintText)
        }
        y += 20f
    }

    // DRAW FOOTER TTD (SIGNATURE SECTION)
    val activeSigners = parseTtdSigners(kop.ttdSignersJson).filter { it.isEnabled }
    val tempatTanggalText = kop.tempatTanggal.trim()

    if (activeSigners.isNotEmpty() || tempatTanggalText.isNotBlank()) {
        val ttdFontTypeface = when (kop.ttdFontFamily.uppercase()) {
            "TIMES NEW ROMAN", "SERIF" -> android.graphics.Typeface.SERIF
            "COURIER", "MONOSPACE" -> android.graphics.Typeface.MONOSPACE
            else -> android.graphics.Typeface.SANS_SERIF
        }
        val ttdFontSizePx = (if (kop.ttdFontSize <= 0) 10 else kop.ttdFontSize) * 0.9f

        val paintTtdText = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = ttdFontSizePx
            isAntiAlias = true
            typeface = ttdFontTypeface
            textAlign = android.graphics.Paint.Align.LEFT
        }
        val paintTtdTextBold = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = ttdFontSizePx
            isFakeBoldText = true
            isAntiAlias = true
            typeface = ttdFontTypeface
            textAlign = android.graphics.Paint.Align.LEFT
        }
        val paintTtdTextRight = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = ttdFontSizePx
            isAntiAlias = true
            typeface = ttdFontTypeface
            textAlign = android.graphics.Paint.Align.RIGHT
        }

        val neededSpace = if (activeSigners.size > 3) 220f else 120f
        if (y + neededSpace > pageHeight - 40f) {
            pdfDocument.finishPage(currentPage)
            pageNumber++
            pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            currentPage = pdfDocument.startPage(pageInfo)
            canvas = currentPage.canvas
            y = 50f
        } else {
            y += 24f
        }

        val signersToRender = if (activeSigners.isEmpty()) {
            listOf(TtdSignerItem(jabatan = "Kepala Sekolah", nama = "", nip = ""))
        } else activeSigners

        val maxCols = if (signersToRender.size <= 3) signersToRender.size else 2
        val firstRowSigners = signersToRender.take(maxCols)
        val secondRowSigners = signersToRender.drop(maxCols)

        fun getXForIndex(idx: Int, totalInRow: Int): Float {
            return when (totalInRow) {
                1 -> 370f
                2 -> if (idx == 0) 50f else 370f
                3 -> when (idx) {
                    0 -> 50f
                    1 -> 210f
                    else -> 370f
                }
                else -> 50f + idx * 150f
            }
        }

        if (tempatTanggalText.isNotBlank()) {
            val rightColX = getXForIndex(firstRowSigners.size - 1, firstRowSigners.size)
            canvas.drawText(tempatTanggalText, rightColX, y, paintTtdText)
            y += ttdFontSizePx + 6f
        }

        val row1StartY = y
        var maxRow1Y = row1StartY

        firstRowSigners.forEachIndexed { idx, signer ->
            val colX = getXForIndex(idx, firstRowSigners.size)
            var currentY = row1StartY

            if (signer.jabatan.isNotBlank()) {
                canvas.drawText(signer.jabatan, colX, currentY, paintTtdText)
                currentY += ttdFontSizePx + 4f
            }

            currentY += 42f

            if (signer.nama.isNotBlank()) {
                canvas.drawText(signer.nama, colX, currentY, paintTtdTextBold)
                val nameWidth = paintTtdTextBold.measureText(signer.nama)
                canvas.drawLine(colX, currentY + 1.5f, colX + nameWidth, currentY + 1.5f, paintTtdTextBold)
                currentY += ttdFontSizePx + 4f
            }

            if (signer.nip.isNotBlank()) {
                val nipLabel = if (signer.nip.uppercase().startsWith("NIP")) signer.nip else "NIP. ${signer.nip}"
                canvas.drawText(nipLabel, colX, currentY, paintTtdText)
                currentY += ttdFontSizePx + 4f
            }

            if (currentY > maxRow1Y) maxRow1Y = currentY
        }

        if (secondRowSigners.isNotEmpty()) {
            val row2StartY = maxRow1Y + 16f
            secondRowSigners.forEachIndexed { idx, signer ->
                val colX = getXForIndex(idx, secondRowSigners.size)
                var currentY = row2StartY

                if (signer.jabatan.isNotBlank()) {
                    canvas.drawText(signer.jabatan, colX, currentY, paintTtdText)
                    currentY += ttdFontSizePx + 4f
                }

                currentY += 42f

                if (signer.nama.isNotBlank()) {
                    canvas.drawText(signer.nama, colX, currentY, paintTtdTextBold)
                    val nameWidth = paintTtdTextBold.measureText(signer.nama)
                    canvas.drawLine(colX, currentY + 1.5f, colX + nameWidth, currentY + 1.5f, paintTtdTextBold)
                    currentY += ttdFontSizePx + 4f
                }

                if (signer.nip.isNotBlank()) {
                    val nipLabel = if (signer.nip.uppercase().startsWith("NIP")) signer.nip else "NIP. ${signer.nip}"
                    canvas.drawText(nipLabel, colX, currentY, paintTtdText)
                    currentY += ttdFontSizePx + 4f
                }
            }
        }
    }
    
    pdfDocument.finishPage(currentPage)
    
    val bos = java.io.ByteArrayOutputStream()
    pdfDocument.writeTo(bos)
    pdfDocument.close()
    
    return bos.toByteArray()
}

fun saveFileToDownloads(
    context: Context, 
    filename: String, 
    mimeType: String, 
    bytes: ByteArray,
    onSuccess: (android.net.Uri) -> Unit = {}
) {
    try {
        val resolver = context.contentResolver
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { os ->
                    os.write(bytes)
                }
                Toast.makeText(context, "Berhasil diekspor ke folder Downloads: $filename", Toast.LENGTH_LONG).show()
                onSuccess(uri)
            } else {
                Toast.makeText(context, "Gagal membuat file!", Toast.LENGTH_SHORT).show()
            }
        } else {
            val downloadDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val file = java.io.File(downloadDir, filename)
            java.io.FileOutputStream(file).use { os ->
                os.write(bytes)
            }
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "com.lintang.lunaris.fileprovider",
                file
            )
            Toast.makeText(context, "Berhasil diekspor ke folder Downloads: $filename", Toast.LENGTH_LONG).show()
            onSuccess(uri)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}

// ==========================================
// SUB-MENU 1: RIWAYAT PEMAKAIAN & AFKIR BAHAN
// ==========================================
@Composable
fun PemakaianAfkirTabContent(
    pemakaian: List<com.example.data.entity.PemakaianBahanEntity>,
    afkir: List<com.example.data.entity.BahanAfkirEntity>,
    masterRooms: List<String> = emptyList(),
    onNavigateToTab: (Int) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedRoomFilter by remember { mutableStateOf<String?>(null) }
    var selectedSubTab by remember { mutableIntStateOf(0) }

    val filteredPemakaian = remember(pemakaian, searchQuery, selectedRoomFilter) {
        pemakaian.filter { p ->
            val matchesSearch = searchQuery.isBlank() ||
                p.namaBarang.contains(searchQuery, ignoreCase = true) ||
                p.idBarang.contains(searchQuery, ignoreCase = true) ||
                p.namaPeminta.contains(searchQuery, ignoreCase = true) ||
                p.keterangan.contains(searchQuery, ignoreCase = true)
            val matchesRoom = selectedRoomFilter == null || (p.kelas != null && p.kelas.contains(selectedRoomFilter!!, ignoreCase = true))
            matchesSearch && matchesRoom
        }
    }

    val filteredAfkir = remember(afkir, searchQuery) {
        afkir.filter { a ->
            searchQuery.isBlank() ||
                a.namaBarang.contains(searchQuery, ignoreCase = true) ||
                a.idBarang.contains(searchQuery, ignoreCase = true) ||
                a.alasan.contains(searchQuery, ignoreCase = true)
        }
    }

    val totalPemakaianQty = remember(filteredPemakaian) { filteredPemakaian.sumOf { it.jumlahDiambil } }
    val totalAfkirQty = remember(filteredAfkir) { filteredAfkir.sumOf { it.jumlahAfkir } }

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. MAIN HEADER / SUMMARY CARD (FIRST)
        LunarisCard(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Laporan Riwayat Pemakaian & Afkir Bahan",
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepPurpleText,
                    fontSize = 16.sp
                )
                Text(
                    text = "Rekam jejak penggunaan bahan laboratorium & log bahan afkir",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFF3E8FF), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Science, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Total Pemakaian", fontSize = 11.sp, color = Color(0xFF6B21A8), fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("$totalPemakaianQty Unit", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF7C3AED))
                            Text("${filteredPemakaian.size} Transaksi", fontSize = 11.sp, color = Color.Gray)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFFEF2F2), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Total Bahan Afkir", fontSize = 11.sp, color = Color(0xFF991B1B), fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("$totalAfkirQty Item", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFDC2626))
                            Text("${filteredAfkir.size} Records Afkir", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }

        // 2. SEARCH & FILTER BAR (SECOND)
        ReportSearchAndFilterBar(
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            selectedKondisiFilter = null,
            onKondisiFilterChange = {},
            selectedRoomFilter = selectedRoomFilter,
            onRoomFilterChange = { selectedRoomFilter = it },
            masterRooms = masterRooms,
            placeholderText = "Ketik untuk mencari..."
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                label = { Text("Semua (${filteredPemakaian.size + filteredAfkir.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = DeepPurpleText,
                    selectedLabelColor = Color.White
                )
            )
            FilterChip(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                label = { Text("Riwayat Pemakaian (${filteredPemakaian.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF7C3AED),
                    selectedLabelColor = Color.White
                )
            )
            FilterChip(
                selected = selectedSubTab == 2,
                onClick = { selectedSubTab = 2 },
                label = { Text("Log Afkir (${filteredAfkir.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFDC2626),
                    selectedLabelColor = Color.White
                )
            )
        }

        if (selectedSubTab == 0 || selectedSubTab == 1) {
            Text("Jejak Penggunaan Bahan", fontWeight = FontWeight.ExtraBold, color = DeepPurpleText, fontSize = 16.sp)
            if (filteredPemakaian.isEmpty()) {
                EmptyStateView("Tidak ada riwayat pemakaian bahan pada periode ini.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    filteredPemakaian.forEach { item ->
                        LunarisCard(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .background(Color(0xFFEFF6FF), RoundedCornerShape(10.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Science, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
                                        }
                                        Column {
                                            Text(item.namaBarang, fontWeight = FontWeight.Bold, color = DeepPurpleText, fontSize = 14.sp)
                                            Text("Peminta: ${item.namaPeminta} (${item.kelas?.ifBlank { "Lab" } ?: "Lab"})", fontSize = 12.sp, color = Color.Gray)
                                        }
                                    }
                                    Surface(
                                        color = Color(0xFFDBEAFE),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "${item.jumlahDiambil} ${item.satuan}",
                                            color = Color(0xFF1D4ED8),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                if (item.keterangan.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Keperluan: ${item.keterangan}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF1F5F9))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Petugas: ${item.namaPetugas}", fontSize = 11.sp, color = Color.Gray)
                                    Text(item.tanggalPemakaian, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (selectedSubTab == 0 || selectedSubTab == 2) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Daftar Bahan Afkir (Expired / Rusak)", fontWeight = FontWeight.ExtraBold, color = DeepPurpleText, fontSize = 16.sp)
            if (filteredAfkir.isEmpty()) {
                EmptyStateView("Tidak ada daftar bahan afkir pada periode ini.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    filteredAfkir.forEach { log ->
                        LunarisCard(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .background(Color(0xFFFEF2F2), RoundedCornerShape(10.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                                        }
                                        Column {
                                            Text(log.namaBarang, fontWeight = FontWeight.Bold, color = DeepPurpleText, fontSize = 14.sp)
                                            Text("ID Barang: ${log.idBarang}", fontSize = 12.sp, color = Color.Gray)
                                        }
                                    }
                                    Surface(
                                        color = Color(0xFFFEE2E2),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "${log.jumlahAfkir} ${log.satuan}",
                                            color = Color(0xFFDC2626),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Alasan Afkir: ${log.alasan}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFB45309))
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF1F5F9))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Status: ${log.status.ifBlank { "Afkir" }}", fontSize = 11.sp, color = Color.Gray)
                                    Text(log.tanggalAfkir, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

// ==========================================
// SUB-MENU 2: LAPORAN PENGHAPUSAN ASET
// ==========================================
@Composable
fun PenghapusanAsetTabContent(
    damagedItems: List<com.example.data.entity.DamagedItemEntity>,
    afkirItems: List<com.example.data.entity.BahanAfkirEntity>,
    peripheralItems: List<com.example.data.entity.PeripheralRusakEntity>,
    masterRooms: List<String> = emptyList(),
    onNavigateToTab: (Int) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("Semua") }
    var showFilterPopup by remember { mutableStateOf(false) }
    var showPenghapusanQrScanner by remember { mutableStateOf(false) }

    val isFilterActive = selectedCategoryFilter != "Semua"

    val filteredAlat = remember(damagedItems, searchQuery) {
        damagedItems.filter {
            (it.isHibah || it.status.contains("Hapus", ignoreCase = true) || it.status.contains("Hibah", ignoreCase = true) || it.validationCount >= 2) &&
            (searchQuery.isBlank() || it.namaBarang.contains(searchQuery, ignoreCase = true) || it.idBarang.contains(searchQuery, ignoreCase = true))
        }
    }

    val filteredBahan = remember(afkirItems, searchQuery) {
        afkirItems.filter {
            (searchQuery.isBlank() || it.namaBarang.contains(searchQuery, ignoreCase = true) || it.idBarang.contains(searchQuery, ignoreCase = true))
        }
    }

    val filteredPeripheral = remember(peripheralItems, searchQuery) {
        peripheralItems.filter {
            (searchQuery.isBlank() || it.namaBarang.contains(searchQuery, ignoreCase = true) || it.idBarang.contains(searchQuery, ignoreCase = true))
        }
    }

    val totalAlatHapus = filteredAlat.sumOf { it.jumlah }
    val totalBahanHapus = filteredBahan.sumOf { it.jumlahAfkir }
    val totalPeripheralHapus = filteredPeripheral.sumOf { it.jumlah }

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. MAIN HEADER / SUMMARY CARD (FIRST)
        LunarisCard(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Laporan Rekapitulasi Penghapusan Aset",
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepPurpleText,
                    fontSize = 16.sp
                )
                Text(
                    text = "Penghapusan resmi & hibah untuk Alat, Bahan, Peripheral, dan Komputer/PC",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFFEF2F2), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text("Aset Alat", fontSize = 11.sp, color = Color(0xFF991B1B), fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("$totalAlatHapus Unit", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFDC2626))
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFFFFBEB), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text("Aset Bahan", fontSize = 11.sp, color = Color(0xFF92400E), fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("$totalBahanHapus Item", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFD97706))
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFF3E8FF), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text("Peripheral/PC", fontSize = 11.sp, color = Color(0xFF6B21A8), fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("$totalPeripheralHapus Unit", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF7E22CE))
                        }
                    }
                }
            }
        }

        // 2. SEARCH BAR & STANDALONE FILTER TRIGGER
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Kotak Pencarian (Hanya Teks Placeholder "Ketik untuk mencari..." & QrCodeScanner di kanan)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Ketik untuk mencari...",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Cari",
                        tint = Color(0xFF7C3AED),
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Hapus",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = { showPenghapusanQrScanner = true },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("btn_scan_qr_penghapusan")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Pindai QR Kode",
                                tint = Color(0xFF7C3AED),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedBorderColor = Color(0xFF7C3AED),
                    unfocusedTextColor = DeepPurpleText,
                    focusedTextColor = DeepPurpleText
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("input_search_penghapusan")
            )

            // Ikon Pemicu Filter Standalone (Di luar kotak pencarian, sebelah kanan, tanpa label teks)
            Surface(
                onClick = { showFilterPopup = true },
                shape = RoundedCornerShape(14.dp),
                color = if (isFilterActive) Color(0xFF7C3AED) else Color.White,
                border = BorderStroke(
                    1.dp,
                    if (isFilterActive) Color(0xFF6D28D9) else Color(0xFFCBD5E1)
                ),
                modifier = Modifier
                    .size(50.dp)
                    .testTag("btn_filter_penghapusan_icon")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter",
                        tint = if (isFilterActive) Color.White else Color(0xFF475569),
                        modifier = Modifier.size(22.dp)
                    )
                    if (isFilterActive) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(5.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }
            }
        }

        if (isFilterActive) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AssistChip(
                    onClick = { selectedCategoryFilter = "Semua" },
                    label = { Text("Kategori: $selectedCategoryFilter", fontSize = 11.sp, color = Color(0xFF7C3AED)) },
                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFF7C3AED)) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFFF3E8FF)),
                    border = BorderStroke(1.dp, Color(0xFFDDD6FE))
                )
            }
        }

        if (selectedCategoryFilter == "Semua" || selectedCategoryFilter == "Alat") {
            Text("Penghapusan / Hibah Aset Alat", fontWeight = FontWeight.ExtraBold, color = DeepPurpleText, fontSize = 15.sp)
            if (filteredAlat.isEmpty()) {
                EmptyStateView("Tidak ada rekapitulasi penghapusan alat.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    filteredAlat.forEach { item ->
                        LunarisCard(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(item.namaBarang, fontWeight = FontWeight.Bold, color = DeepPurpleText, fontSize = 14.sp)
                                    Surface(
                                        color = if (item.isHibah) Color(0xFFE0F2FE) else Color(0xFFFEE2E2),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (item.isHibah) "Hibah Aset" else "Hapus Aset",
                                            color = if (item.isHibah) Color(0xFF0369A1) else Color(0xFFDC2626),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("ID: ${item.idBarang} | Jumlah: ${item.jumlah} Unit", fontSize = 12.sp, color = Color.Gray)
                                if (item.keteranganKerusakan.isNotBlank()) {
                                    Text("Ket: ${item.keteranganKerusakan}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Petugas: ${item.namaPetugas}", fontSize = 11.sp, color = Color.Gray)
                                    Text(item.tanggalKerusakan, fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (selectedCategoryFilter == "Semua" || selectedCategoryFilter == "Bahan") {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Penghapusan Aset Bahan Afkir", fontWeight = FontWeight.ExtraBold, color = DeepPurpleText, fontSize = 15.sp)
            if (filteredBahan.isEmpty()) {
                EmptyStateView("Tidak ada rekapitulasi penghapusan bahan.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    filteredBahan.forEach { item ->
                        LunarisCard(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(item.namaBarang, fontWeight = FontWeight.Bold, color = DeepPurpleText, fontSize = 14.sp)
                                    Surface(
                                        color = Color(0xFFFFEDD5),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "Bahan Afkir/Hapus",
                                            color = Color(0xFFC2410C),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("ID: ${item.idAfkir} | Jumlah: ${item.jumlahAfkir} ${item.satuan}", fontSize = 12.sp, color = Color.Gray)
                                Text("Alasan: ${item.alasan}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Status: ${item.status.ifBlank { "Hapus Aset" }}", fontSize = 11.sp, color = Color.Gray)
                                    Text(item.tanggalAfkir, fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (selectedCategoryFilter == "Semua" || selectedCategoryFilter == "Peripheral") {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Penghapusan / Hibah Peripheral", fontWeight = FontWeight.ExtraBold, color = DeepPurpleText, fontSize = 15.sp)
            if (filteredPeripheral.isEmpty()) {
                EmptyStateView("Tidak ada rekapitulasi penghapusan peripheral.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    filteredPeripheral.forEach { item ->
                        LunarisCard(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(item.namaBarang, fontWeight = FontWeight.Bold, color = DeepPurpleText, fontSize = 14.sp)
                                    Surface(
                                        color = Color(0xFFF3E8FF),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (item.isHibah) "Hibah Peripheral" else "Hapus Peripheral",
                                            color = Color(0xFF7E22CE),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("ID: ${item.idBarang} | Jumlah: ${item.jumlah} Unit", fontSize = 12.sp, color = Color.Gray)
                                if (item.keteranganKerusakan.isNotBlank()) {
                                    Text("Ket: ${item.keteranganKerusakan}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Sub: ${item.subKategori}", fontSize = 11.sp, color = Color.Gray)
                                    Text("Hapus/Hibah", fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showFilterPopup) {
            var tempCategory by remember { mutableStateOf(selectedCategoryFilter) }

            LunarisFilterDialog(
                onDismissRequest = { showFilterPopup = false },
                title = "Filter Penghapusan Aset",
                filterGroups = listOf(
                    FilterGroup(
                        title = "Kategori Aset",
                        options = listOf("Semua", "Alat", "Bahan", "Peripheral"),
                        selectedOption = tempCategory,
                        onOptionSelected = { tempCategory = it }
                    )
                ),
                onReset = {
                    tempCategory = "Semua"
                    selectedCategoryFilter = "Semua"
                    showFilterPopup = false
                },
                onApply = {
                    selectedCategoryFilter = tempCategory
                    showFilterPopup = false
                }
            )
        }

        if (showPenghapusanQrScanner) {
            CameraScannerDialog(
                title = "Pindai QR Kode Penghapusan",
                initialMode = ScanMode.PRIMARY_QR,
                onDismissRequest = { showPenghapusanQrScanner = false },
                onBarcodeScanned = { scannedCode ->
                    showPenghapusanQrScanner = false
                    searchQuery = scannedCode.trim()
                }
            )
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

// ==========================================
// SUB-MENU 3: LAPORAN PEMELIHARAAN & SERVIS LUAR
// ==========================================
@Composable
fun PemeliharaanServisTabContent(
    maintenance: List<com.example.data.entity.DamagedItemEntity>,
    servisLuar: List<com.example.data.entity.DamagedItemEntity>,
    items: List<ItemWithStock>,
    masterRooms: List<String> = emptyList(),
    onNavigateToTab: (Int) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedRoomFilter by remember { mutableStateOf<String?>(null) }
    var selectedFilterType by remember { mutableStateOf("Semua") }

    val itemRoomMap = remember(items) {
        items.associate { it.idBarang to it.ruang }
    }

    val filteredMaintenance = remember(maintenance, searchQuery, selectedRoomFilter) {
        maintenance.filter { m ->
            val room = itemRoomMap[m.idBarang]?.ifBlank { "Lainnya" } ?: "Lainnya"
            val matchesSearch = searchQuery.isBlank() ||
                m.namaBarang.contains(searchQuery, ignoreCase = true) ||
                m.idBarang.contains(searchQuery, ignoreCase = true) ||
                m.keteranganKerusakan.contains(searchQuery, ignoreCase = true)
            val matchesRoom = selectedRoomFilter == null || room == selectedRoomFilter
            matchesSearch && matchesRoom
        }
    }

    val filteredServisLuar = remember(servisLuar, searchQuery, selectedRoomFilter) {
        servisLuar.filter { s ->
            val room = itemRoomMap[s.idBarang]?.ifBlank { "Lainnya" } ?: "Lainnya"
            val matchesSearch = searchQuery.isBlank() ||
                s.namaBarang.contains(searchQuery, ignoreCase = true) ||
                s.idBarang.contains(searchQuery, ignoreCase = true) ||
                s.keteranganKerusakan.contains(searchQuery, ignoreCase = true)
            val matchesRoom = selectedRoomFilter == null || room == selectedRoomFilter
            matchesSearch && matchesRoom
        }
    }

    val roomMaintenanceData = remember(maintenance, itemRoomMap) {
        maintenance.groupBy { itemRoomMap[it.idBarang]?.ifBlank { "Lainnya" } ?: "Lainnya" }
            .mapValues { entry -> entry.value.sumOf { it.jumlah }.toFloat() }
    }

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. MAIN HEADER / SUMMARY CARD (FIRST)
        LunarisCard(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Laporan Pemeliharaan & Servis Luar",
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepPurpleText,
                    fontSize = 16.sp
                )
                Text(
                    text = "Riwayat pemeliharaan berkala laboratorium serta servis luar untuk Alat & Komputer",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFFEF3C7), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text("Pemeliharaan Berkala", fontSize = 11.sp, color = Color(0xFF92400E), fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${filteredMaintenance.sumOf { it.jumlah }} Unit", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFD97706))
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFF3E8FF), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text("Servis Luar (Vendor)", fontSize = 11.sp, color = Color(0xFF6B21A8), fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${filteredServisLuar.sumOf { it.jumlah }} Unit", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF7C3AED))
                        }
                    }
                }
            }
        }

        // 2. SEARCH & FILTER BAR (SECOND)
        ReportSearchAndFilterBar(
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            selectedKondisiFilter = null,
            onKondisiFilterChange = {},
            selectedRoomFilter = selectedRoomFilter,
            onRoomFilterChange = { selectedRoomFilter = it },
            masterRooms = masterRooms,
            placeholderText = "Ketik untuk mencari..."
        )

        if (roomMaintenanceData.isNotEmpty()) {
            LunarisCard(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Distribusi Pemeliharaan per Ruangan",
                        fontWeight = FontWeight.Bold,
                        color = DeepPurpleText,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    InteractiveHorizontalBarChart(
                        data = roomMaintenanceData,
                        barColor = Color(0xFFF59E0B),
                        onBarClick = { room ->
                            selectedRoomFilter = if (selectedRoomFilter == room) null else room
                        }
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Semua", "Pemeliharaan Berkala", "Servis Luar").forEach { filter ->
                FilterChip(
                    selected = selectedFilterType == filter,
                    onClick = { selectedFilterType = filter },
                    label = { Text(filter, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DeepPurpleText,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        if (selectedFilterType == "Semua" || selectedFilterType == "Pemeliharaan Berkala") {
            Text("Pemeliharaan Berkala Intern", fontWeight = FontWeight.ExtraBold, color = DeepPurpleText, fontSize = 16.sp)
            if (filteredMaintenance.isEmpty()) {
                EmptyStateView("Tidak ada alat/komputer dalam jadwal pemeliharaan berkala.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    filteredMaintenance.forEach { item ->
                        LunarisCard(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .background(Color(0xFFFEF3C7), RoundedCornerShape(10.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Build, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                                        }
                                        Column {
                                            Text(item.namaBarang, fontWeight = FontWeight.Bold, color = DeepPurpleText, fontSize = 14.sp)
                                            Text("Ruang: ${itemRoomMap[item.idBarang]?.ifBlank { "Laboratorium" } ?: "Laboratorium"}", fontSize = 12.sp, color = Color.Gray)
                                        }
                                    }
                                    Surface(
                                        color = Color(0xFFFEF3C7),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "${item.jumlah} Unit",
                                            color = Color(0xFFB45309),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Catatan / Kerusakan: ${item.keteranganKerusakan}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (item.statusKeterangan.isNotBlank()) {
                                    Text("Tindakan: ${item.statusKeterangan}", fontSize = 12.sp, color = Color(0xFF7C3AED), fontWeight = FontWeight.SemiBold)
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF1F5F9))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Petugas: ${item.namaPetugas}", fontSize = 11.sp, color = Color.Gray)
                                    Text(item.tanggalKerusakan, fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (selectedFilterType == "Semua" || selectedFilterType == "Servis Luar") {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Riwayat Servis Luar (Teknisi / Vendor)", fontWeight = FontWeight.ExtraBold, color = DeepPurpleText, fontSize = 16.sp)
            if (filteredServisLuar.isEmpty()) {
                EmptyStateView("Tidak ada riwayat servis luar untuk periode ini.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    filteredServisLuar.forEach { item ->
                        LunarisCard(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .background(Color(0xFFEDE9FE), RoundedCornerShape(10.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Engineering, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(20.dp))
                                        }
                                        Column {
                                            Text(item.namaBarang, fontWeight = FontWeight.Bold, color = DeepPurpleText, fontSize = 14.sp)
                                            Text("Status: Servis Luar Vendor", fontSize = 12.sp, color = Color(0xFF7C3AED), fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                    Surface(
                                        color = Color(0xFFDBEAFE),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "${item.jumlah} Unit",
                                            color = Color(0xFF1D4ED8),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Diagnosis / Kerusakan: ${item.keteranganKerusakan}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (item.statusKeterangan.isNotBlank()) {
                                    Text("Laporan Servis: ${item.statusKeterangan}", fontSize = 12.sp, color = Color(0xFF059669), fontWeight = FontWeight.Medium)
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF1F5F9))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Penanggung Jawab: ${item.namaPetugas}", fontSize = 11.sp, color = Color.Gray)
                                    Text(item.tanggalKerusakan, fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

// ==========================================
// SUB-MENU 4: LOG AKTIVITAS SISTEM & STOK
// ==========================================
@Composable
fun LogAktivitasStokTabContent(
    transactions: List<com.example.data.entity.LoanTransactionEntity>,
    items: List<ItemWithStock>,
    pemakaian: List<com.example.data.entity.PemakaianBahanEntity>,
    afkir: List<com.example.data.entity.BahanAfkirEntity>,
    damaged: List<com.example.data.entity.DamagedItemEntity>,
    masterRooms: List<String> = emptyList(),
    onNavigateToTab: (Int) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("Semua") }
    var showFilterPopup by remember { mutableStateOf(false) }
    var showLogQrScanner by remember { mutableStateOf(false) }

    val isFilterActive = selectedCategoryFilter != "Semua"

    data class AuditLogEntry(
        val type: String,
        val title: String,
        val subtitle: String,
        val officer: String,
        val date: String,
        val badgeColor: Color,
        val badgeBg: Color
    )

    val auditLog = remember(transactions, pemakaian, afkir, damaged) {
        val list = mutableListOf<AuditLogEntry>()

        transactions.forEach { tx ->
            if (tx.status == "Kembali") {
                list.add(
                    AuditLogEntry(
                        type = "KEMBALI",
                        title = "Pengembalian Alat: ${tx.namaPeminjam} (${tx.kelas})",
                        subtitle = "Kode Tx: ${tx.idTransaksi} | Tgl Pinjam: ${tx.tanggal}",
                        officer = tx.petugasKembali ?: tx.namaPetugas,
                        date = tx.tanggalKembali ?: tx.tanggal,
                        badgeColor = Color(0xFF059669),
                        badgeBg = Color(0xFFD1FAE5)
                    )
                )
            } else {
                list.add(
                    AuditLogEntry(
                        type = "PINJAM",
                        title = "Peminjaman Alat: ${tx.namaPeminjam} (${tx.kelas})",
                        subtitle = "Kode Tx: ${tx.idTransaksi} | Status: ${tx.status}",
                        officer = tx.namaPetugas,
                        date = tx.tanggal,
                        badgeColor = Color(0xFF7C3AED),
                        badgeBg = Color(0xFFEDE9FE)
                    )
                )
            }
        }

        pemakaian.forEach { p ->
            list.add(
                AuditLogEntry(
                    type = "PEMAKAIAN",
                    title = "Pemakaian Bahan: ${p.namaBarang} (${p.jumlahDiambil} ${p.satuan})",
                    subtitle = "Peminta: ${p.namaPeminta} | Keperluan: ${p.keterangan}",
                    officer = p.namaPetugas,
                    date = p.tanggalPemakaian,
                    badgeColor = Color(0xFF7C3AED),
                    badgeBg = Color(0xFFEDE9FE)
                )
            )
        }

        afkir.forEach { a ->
            list.add(
                AuditLogEntry(
                    type = "AFKIR",
                    title = "Bahan Afkir: ${a.namaBarang} (${a.jumlahAfkir} ${a.satuan})",
                    subtitle = "Alasan: ${a.alasan}",
                    officer = "Administrator",
                    date = a.tanggalAfkir,
                    badgeColor = Color(0xFFDC2626),
                    badgeBg = Color(0xFFFEE2E2)
                )
            )
        }

        damaged.forEach { d ->
            list.add(
                AuditLogEntry(
                    type = "RUSAK",
                    title = "Laporan Alat Rusak: ${d.namaBarang} (${d.jumlah} Pcs)",
                    subtitle = "Keterangan: ${d.keteranganKerusakan} | Status: ${d.status}",
                    officer = d.namaPetugas,
                    date = d.tanggalKerusakan,
                    badgeColor = Color(0xFFD97706),
                    badgeBg = Color(0xFFFEF3C7)
                )
            )
        }

        list.sortedByDescending { it.date }
    }

    val filteredAuditLog = remember(auditLog, searchQuery, selectedCategoryFilter) {
        auditLog.filter { entry ->
            val matchesSearch = searchQuery.isBlank() ||
                entry.title.contains(searchQuery, ignoreCase = true) ||
                entry.subtitle.contains(searchQuery, ignoreCase = true) ||
                entry.officer.contains(searchQuery, ignoreCase = true)
            val matchesCategory = when (selectedCategoryFilter) {
                "Pergerakan Stok" -> entry.type == "PEMAKAIAN" || entry.type == "AFKIR"
                "Sirkulasi" -> entry.type == "PINJAM" || entry.type == "KEMBALI"
                "Afkir & Damage" -> entry.type == "AFKIR" || entry.type == "RUSAK"
                else -> true
            }
            matchesSearch && matchesCategory
        }
    }

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. MAIN HEADER / SUMMARY CARD (FIRST)
        LunarisCard(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Log Aktivitas Sistem & Manajemen Stok",
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepPurpleText,
                    fontSize = 16.sp
                )
                Text(
                    text = "Audit trail lengkap pergerakan stok, sirkulasi, serta aktivitas sistem",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text("Total Log Audit", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${auditLog.size}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = DeepPurpleText)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFEDE9FE), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text("Sirkulasi", fontSize = 11.sp, color = Color(0xFF6B21A8), fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${transactions.size}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF7C3AED))
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFF3E8FF), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text("Pemakaian Bahan", fontSize = 11.sp, color = Color(0xFF6B21A8), fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${pemakaian.size}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF7E22CE))
                        }
                    }
                }
            }
        }

        // 2. SEARCH BAR & STANDALONE FILTER TRIGGER
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Kotak Pencarian (Hanya Teks Placeholder "Ketik untuk mencari..." & QrCodeScanner di kanan)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Ketik untuk mencari...",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Cari",
                        tint = Color(0xFF7C3AED),
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Hapus",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = { showLogQrScanner = true },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("btn_scan_qr_log")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Pindai QR Kode",
                                tint = Color(0xFF7C3AED),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedBorderColor = Color(0xFF7C3AED),
                    unfocusedTextColor = DeepPurpleText,
                    focusedTextColor = DeepPurpleText
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("input_search_log")
            )

            // Ikon Pemicu Filter Standalone (Di luar kotak pencarian, sebelah kanan, tanpa label teks)
            Surface(
                onClick = { showFilterPopup = true },
                shape = RoundedCornerShape(14.dp),
                color = if (isFilterActive) Color(0xFF7C3AED) else Color.White,
                border = BorderStroke(
                    1.dp,
                    if (isFilterActive) Color(0xFF6D28D9) else Color(0xFFCBD5E1)
                ),
                modifier = Modifier
                    .size(50.dp)
                    .testTag("btn_filter_log_icon")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter",
                        tint = if (isFilterActive) Color.White else Color(0xFF475569),
                        modifier = Modifier.size(22.dp)
                    )
                    if (isFilterActive) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(5.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }
            }
        }

        if (isFilterActive) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AssistChip(
                    onClick = { selectedCategoryFilter = "Semua" },
                    label = { Text("Kategori: $selectedCategoryFilter", fontSize = 11.sp, color = Color(0xFF7C3AED)) },
                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFF7C3AED)) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFFF3E8FF)),
                    border = BorderStroke(1.dp, Color(0xFFDDD6FE))
                )
            }
        }

        Text("Rincian Audit Trail Aktivitas System (${filteredAuditLog.size})", fontWeight = FontWeight.ExtraBold, color = DeepPurpleText, fontSize = 15.sp)

        if (filteredAuditLog.isEmpty()) {
            EmptyStateView("Tidak ada data log aktivitas sistem untuk penyaringan ini.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                filteredAuditLog.forEach { log ->
                    LunarisCard(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(log.title, fontWeight = FontWeight.Bold, color = DeepPurpleText, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = log.badgeBg,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = log.type,
                                        color = log.badgeColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(log.subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF1F5F9))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Petugas: ${log.officer}", fontSize = 11.sp, color = Color.Gray)
                                Text(log.date, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }

        if (showFilterPopup) {
            var tempCategory by remember { mutableStateOf(selectedCategoryFilter) }

            LunarisFilterDialog(
                onDismissRequest = { showFilterPopup = false },
                title = "Filter Log Aktivitas",
                filterGroups = listOf(
                    FilterGroup(
                        title = "Kategori Aktivitas",
                        options = listOf("Semua", "Sirkulasi", "Pergerakan Stok", "Afkir & Damage"),
                        selectedOption = tempCategory,
                        onOptionSelected = { tempCategory = it }
                    )
                ),
                onReset = {
                    tempCategory = "Semua"
                    selectedCategoryFilter = "Semua"
                    showFilterPopup = false
                },
                onApply = {
                    selectedCategoryFilter = tempCategory
                    showFilterPopup = false
                }
            )
        }

        if (showLogQrScanner) {
            CameraScannerDialog(
                title = "Pindai QR Kode Log",
                initialMode = ScanMode.PRIMARY_QR,
                onDismissRequest = { showLogQrScanner = false },
                onBarcodeScanned = { scannedCode ->
                    showLogQrScanner = false
                    searchQuery = scannedCode.trim()
                }
            )
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}
