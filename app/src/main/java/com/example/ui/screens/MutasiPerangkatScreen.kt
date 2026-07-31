package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.app.DatePickerDialog
import java.util.Calendar
import com.example.ui.components.CameraScannerDialog
import com.example.ui.components.ScanMode
import com.example.ui.components.LunarisFilterDialog
import com.example.ui.components.FilterGroup
import com.example.data.entity.MutasiPerangkatEntity
import com.example.ui.components.LunarisCard
import com.example.ui.components.LunarisDatePickerDialog
import com.example.ui.viewmodel.InventoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MutasiPerangkatScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val mutasiList by viewModel.mutasiPerangkatList.collectAsState()
    val peripheralStocks by viewModel.allPeripheralStocks.collectAsState()
    val itemsWithStock by viewModel.itemsWithStock.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val kondisiList by viewModel.kondisi.collectAsState()
    val ruangList by viewModel.ruang.collectAsState()

    val masterRooms = remember(ruangList) {
        ruangList.map { it.trim() }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val masterConditions = remember(kondisiList) {
        kondisiList.distinct().filter { it.isNotBlank() }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterCategory by remember { mutableStateOf("Semua") } // Semua, Peripheral, LabKom / PC, Alat Sarpras
    var selectedFilterRuang by remember { mutableStateOf<String?>(null) }
    var showFilterPopup by remember { mutableStateOf(false) }
    var showSearchQrScanner by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedDetailMutasi by remember { mutableStateOf<MutasiPerangkatEntity?>(null) }
    var deleteCandidate by remember { mutableStateOf<MutasiPerangkatEntity?>(null) }

    val isFilterActive = (selectedFilterCategory != "Semua" && selectedFilterCategory != "Semua Kategori") ||
            (selectedFilterRuang != null && selectedFilterRuang != "Semua Ruang")

    val filteredList = remember(mutasiList, searchQuery, selectedFilterCategory, selectedFilterRuang) {
        mutasiList.filter { item ->
            val matchesCategory = when {
                selectedFilterCategory.equals("PERIPHERAL", ignoreCase = true) || selectedFilterCategory.contains("Peripheral", ignoreCase = true) -> item.jenisPerangkat.equals("PERIPHERAL", ignoreCase = true)
                selectedFilterCategory.equals("LABKOM", ignoreCase = true) || selectedFilterCategory.contains("LabKom", ignoreCase = true) || selectedFilterCategory.contains("PC", ignoreCase = true) -> item.jenisPerangkat.equals("LABKOM", ignoreCase = true) || item.jenisPerangkat.contains("KOMPUTER", ignoreCase = true) || item.jenisPerangkat.contains("PC", ignoreCase = true)
                selectedFilterCategory.equals("ALAT", ignoreCase = true) || selectedFilterCategory.contains("Alat", ignoreCase = true) -> item.jenisPerangkat.equals("ALAT", ignoreCase = true)
                else -> true
            }
            val matchesRuang = if (selectedFilterRuang == null || selectedFilterRuang == "Semua Ruang") true else (
                item.ruangAsal.equals(selectedFilterRuang, ignoreCase = true) ||
                item.ruangTujuan.equals(selectedFilterRuang, ignoreCase = true)
            )
            val matchesQuery = searchQuery.isBlank() ||
                    item.namaBarang.contains(searchQuery, ignoreCase = true) ||
                    item.idBarang.contains(searchQuery, ignoreCase = true) ||
                    item.idMutasi.contains(searchQuery, ignoreCase = true) ||
                    item.ruangAsal.contains(searchQuery, ignoreCase = true) ||
                    item.ruangTujuan.contains(searchQuery, ignoreCase = true) ||
                    item.namaPetugas.contains(searchQuery, ignoreCase = true) ||
                    item.alasanMutasi.contains(searchQuery, ignoreCase = true)

            matchesCategory && matchesRuang && matchesQuery
        }
    }

    // Statistics
    val totalCount = mutasiList.size
    val peripheralCount = mutasiList.count { it.jenisPerangkat.equals("PERIPHERAL", ignoreCase = true) }
    val labkomCount = mutasiList.count { it.jenisPerangkat.equals("LABKOM", ignoreCase = true) || it.jenisPerangkat.contains("PC", ignoreCase = true) }
    val alatCount = mutasiList.count { it.jenisPerangkat.equals("ALAT", ignoreCase = true) }

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
                            modifier = Modifier.size(40.dp).testTag("btn_back_mutasi")
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
                                text = "Mutasi Perangkat",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Log & Formulir Perpindahan Unit Aset Tetap Antar-Ruang",
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
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF7C3AED),
                contentColor = Color.White,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                modifier = Modifier.testTag("fab_add_mutasi")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Catat Mutasi Baru",
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Summary Stats Section (Clean White Cards)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatMiniCard(
                    title = "Total Mutasi",
                    value = "$totalCount",
                    icon = Icons.Default.CloudSync,
                    iconBg = Color(0xFFF3E8FF),
                    iconTint = Color(0xFF7C3AED),
                    modifier = Modifier.weight(1f)
                )
                StatMiniCard(
                    title = "Peripheral",
                    value = "$peripheralCount",
                    icon = Icons.Default.Memory,
                    iconBg = Color(0xFFCCFBF1),
                    iconTint = Color(0xFF0D9488),
                    modifier = Modifier.weight(1f)
                )
                StatMiniCard(
                    title = "LabKom / PC",
                    value = "$labkomCount",
                    icon = Icons.Default.Computer,
                    iconBg = Color(0xFFE0F2FE),
                    iconTint = Color(0xFF0284C7),
                    modifier = Modifier.weight(1f)
                )
                StatMiniCard(
                    title = "Alat Sarpras",
                    value = "$alatCount",
                    icon = Icons.Default.Build,
                    iconBg = Color(0xFFFEF3C7),
                    iconTint = Color(0xFFD97706),
                    modifier = Modifier.weight(1f)
                )
            }

            // Search & Filter Controls (Standalone Search Box + External Filter Trigger Button)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Kotak Pencarian (Hanya Teks Placeholder & QrCodeScanner di kanan)
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
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Hapus",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = { showSearchQrScanner = true },
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
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White,
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedBorderColor = Color(0xFF7C3AED),
                        unfocusedTextColor = Color(0xFF0F172A),
                        focusedTextColor = Color(0xFF0F172A)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("input_search_mutasi")
                )

                // Ikon Pemicu Filter Standalone (Di luar kotak pencarian, sebelah kanan)
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
                        .testTag("btn_filter_mutasi_icon")
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

            // Active Filter Chips Bar
            if (isFilterActive) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (selectedFilterCategory != "Semua" && selectedFilterCategory != "Semua Kategori") {
                        AssistChip(
                            onClick = { selectedFilterCategory = "Semua" },
                            label = { Text("Kategori: $selectedFilterCategory", fontSize = 11.sp, color = Color(0xFF7C3AED)) },
                            trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFF7C3AED)) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFFF3E8FF)),
                            border = BorderStroke(1.dp, Color(0xFFDDD6FE))
                        )
                    }
                    if (selectedFilterRuang != null && selectedFilterRuang != "Semua Ruang") {
                        AssistChip(
                            onClick = { selectedFilterRuang = null },
                            label = { Text("Ruang: $selectedFilterRuang", fontSize = 11.sp, color = Color(0xFF7C3AED)) },
                            trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFF7C3AED)) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFFF3E8FF)),
                            border = BorderStroke(1.dp, Color(0xFFDDD6FE))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main List Section
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFCCFBF1),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = null,
                                    tint = Color(0xFF0D9488),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "Tidak ada log mutasi yang cocok" else "Belum Ada Catatan Mutasi",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF334155)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "Coba ubah kata kunci pencarian atau filter kategori" else "Klik tombol 'Catat Mutasi Baru' di kanan bawah untuk mencatat perpindahan aset",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("list_mutasi_perangkat")
                ) {
                    items(filteredList, key = { it.idMutasi.ifBlank { it.id.toString() } }) { item ->
                        MutasiCardItem(
                            item = item,
                            onClickDetail = { selectedDetailMutasi = item },
                            onClickDelete = { deleteCandidate = item }
                        )
                    }
                }
            }
        }
    }

    // Filter Popup Dialog
    if (showFilterPopup) {
        var tempCategory by remember { mutableStateOf(if (selectedFilterCategory == "Semua") "Semua Kategori" else selectedFilterCategory) }
        var tempRuang by remember { mutableStateOf(selectedFilterRuang ?: "Semua Ruang") }

        val roomOptions = remember(masterRooms) {
            listOf("Semua Ruang") + masterRooms
        }

        LunarisFilterDialog(
            onDismissRequest = { showFilterPopup = false },
            title = "Filter Mutasi",
            filterGroups = listOf(
                FilterGroup(
                    title = "Kategori Perangkat",
                    options = listOf("Semua Kategori", "Peripheral", "LabKom / PC", "Alat Sarpras"),
                    selectedOption = tempCategory,
                    onOptionSelected = { tempCategory = it }
                ),
                FilterGroup(
                    title = "Ruang Asal / Tujuan",
                    options = roomOptions,
                    selectedOption = tempRuang,
                    onOptionSelected = { tempRuang = it }
                )
            ),
            onReset = {
                tempCategory = "Semua Kategori"
                tempRuang = "Semua Ruang"
                selectedFilterCategory = "Semua"
                selectedFilterRuang = null
                showFilterPopup = false
            },
            onApply = {
                selectedFilterCategory = if (tempCategory.startsWith("Semua")) "Semua" else tempCategory
                selectedFilterRuang = if (tempRuang.startsWith("Semua")) null else tempRuang
                showFilterPopup = false
            }
        )
    }

    // Search QR Scanner Camera Dialog
    if (showSearchQrScanner) {
        CameraScannerDialog(
            title = "Pindai QR Kode Mutasi",
            initialMode = ScanMode.PRIMARY_QR,
            onDismissRequest = { showSearchQrScanner = false },
            onBarcodeScanned = { scannedCode ->
                showSearchQrScanner = false
                searchQuery = scannedCode.trim()
            }
        )
    }

    // Add Mutasi Dialog
    if (showAddDialog) {
        AddMutasiDialog(
            peripheralStocks = peripheralStocks,
            itemsWithStock = itemsWithStock,
            masterRooms = masterRooms,
            masterConditions = masterConditions,
            currentOfficer = profile.namaPetugas.ifBlank { "Petugas Lab" },
            onDismiss = { showAddDialog = false },
            onSubmit = { idBarang, namaBarang, serialNumber, jenisPerangkat, ruangAsal, ruangTujuan, tanggalMutasi, namaPetugas, alasanMutasi, keterangan ->
                viewModel.addMutasiPerangkat(
                    idBarang = idBarang,
                    namaBarang = namaBarang,
                    serialNumber = serialNumber,
                    jenisPerangkat = jenisPerangkat,
                    ruangAsal = ruangAsal,
                    ruangTujuan = ruangTujuan,
                    tanggalMutasi = tanggalMutasi,
                    namaPetugas = namaPetugas,
                    alasanMutasi = alasanMutasi,
                    keterangan = keterangan,
                    onSuccess = {
                        Toast.makeText(context, "Mutasi perangkat $namaBarang berhasil dicatat!", Toast.LENGTH_SHORT).show()
                        showAddDialog = false
                    },
                    onError = { err ->
                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
    }

    // Detail Mutasi Dialog
    selectedDetailMutasi?.let { detail ->
        DetailMutasiDialog(
            item = detail,
            onDismiss = { selectedDetailMutasi = null }
        )
    }

    // Delete Confirmation Dialog
    deleteCandidate?.let { candidate ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Hapus Log Mutasi", fontWeight = FontWeight.Bold) },
            text = {
                Text("Apakah Anda yakin ingin menghapus catatan mutasi perangkat '${candidate.namaBarang}' (${candidate.idMutasi})?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMutasiPerangkat(
                            entity = candidate,
                            onSuccess = {
                                Toast.makeText(context, "Log mutasi berhasil dihapus", Toast.LENGTH_SHORT).show()
                                deleteCandidate = null
                            },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                deleteCandidate = null
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Hapus", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { deleteCandidate = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun StatMiniCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = iconBg,
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
            Text(text = title, fontSize = 9.sp, color = Color(0xFF64748B), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun FilterCategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        selected = isSelected,
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) Color(0xFF7C3AED) else Color(0xFFF1F5F9),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else Color(0xFF475569)
            )
        }
    }
}

@Composable
private fun MutasiCardItem(
    item: MutasiPerangkatEntity,
    onClickDetail: () -> Unit,
    onClickDelete: () -> Unit
) {
    val (typeBg, typeColor) = when {
        item.jenisPerangkat.equals("PERIPHERAL", ignoreCase = true) -> Color(0xFFCCFBF1) to Color(0xFF0D9488)
        item.jenisPerangkat.equals("LABKOM", ignoreCase = true) || item.jenisPerangkat.contains("PC", ignoreCase = true) -> Color(0xFFF3E8FF) to Color(0xFF7C3AED)
        else -> Color(0xFFFEF3C7) to Color(0xFFD97706)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClickDetail() }
            .testTag("item_mutasi_${item.idMutasi}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = typeBg
                    ) {
                        Text(
                            text = item.jenisPerangkat.uppercase(Locale.getDefault()),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = typeColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = item.idMutasi,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B)
                    )
                }

                Text(
                    text = item.tanggalMutasi,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Item Name
            Text(
                text = item.namaBarang,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color(0xFF0F172A)
            )

            if (item.serialNumber.isNotBlank()) {
                Text(
                    text = "SN: ${item.serialNumber}",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Relocation Route Indicator Box
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Origin Room
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "RUANG ASAL", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                        Text(
                            text = item.ruangAsal.ifBlank { "Gudang / Tidak Tercatat" },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFDC2626),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Arrow
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFCCFBF1),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Box(modifier = Modifier.padding(4.dp), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "Pindah Ke",
                                tint = Color(0xFF0D9488),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Destination Room
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text(text = "RUANG TUJUAN", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                        Text(
                            text = item.ruangTujuan.ifBlank { "Tujuan Belum Ditentukan" },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF059669),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Officer & Reason Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                    Text(
                        text = item.namaPetugas,
                        fontSize = 11.sp,
                        color = Color(0xFF475569)
                    )
                }

                IconButton(
                    onClick = onClickDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Hapus Log",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (item.alasanMutasi.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Alasan: ${item.alasanMutasi}",
                        fontSize = 11.sp,
                        color = Color(0xFF334155),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMutasiDialog(
    peripheralStocks: List<com.example.data.entity.PeripheralStockEntity>,
    itemsWithStock: List<com.example.data.model.ItemWithStock>,
    masterRooms: List<String>,
    masterConditions: List<String> = emptyList(),
    currentOfficer: String,
    onDismiss: () -> Unit,
    onSubmit: (
        idBarang: String,
        namaBarang: String,
        serialNumber: String,
        jenisPerangkat: String,
        ruangAsal: String,
        ruangTujuan: String,
        tanggalMutasi: String,
        namaPetugas: String,
        alasanMutasi: String,
        keterangan: String
    ) -> Unit
) {
    val context = LocalContext.current
    var selectedDialogCategory by remember { mutableStateOf("Semua") } // Semua, Alat, Peripheral, LabKom
    var deviceSearchQuery by remember { mutableStateOf("") }
    var selectedDeviceName by remember { mutableStateOf("") }
    var selectedDeviceId by remember { mutableStateOf("") }
    var selectedDeviceSn by remember { mutableStateOf("") }
    var selectedDeviceType by remember { mutableStateOf("PERIPHERAL") } // PERIPHERAL, LABKOM, ALAT
    var kondisiSebelumPindah by remember { mutableStateOf(masterConditions.firstOrNull() ?: "") }
    var ruangAsal by remember { mutableStateOf("") }
    var isCustomRuangAsal by remember { mutableStateOf(false) }
    var customRuangAsalText by remember { mutableStateOf("") }
    var ruangTujuan by remember { mutableStateOf("") }
    var isCustomRuangTujuan by remember { mutableStateOf(false) }
    var customRuangTujuanText by remember { mutableStateOf("") }
    var tanggalMutasi by remember { mutableStateOf(SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID")).format(Date())) }
    var namaPetugas by remember { mutableStateOf(currentOfficer) }
    var alasanMutasi by remember { mutableStateOf("") }
    var keterangan by remember { mutableStateOf("") }

    var isDeviceDropdownExpanded by remember { mutableStateOf(false) }
    var isKondisiDropdownExpanded by remember { mutableStateOf(false) }
    var isRuangAsalDropdownExpanded by remember { mutableStateOf(false) }
    var isRoomDropdownExpanded by remember { mutableStateOf(false) }

    var showQrScanner by remember { mutableStateOf(false) }
    var showBarcodeScanner by remember { mutableStateOf(false) }

    // Combined device list for selection
    val candidateDevices = remember(peripheralStocks, itemsWithStock) {
        val listPeripherals = peripheralStocks.map {
            DeviceOption(
                idBarang = it.idBarang.ifBlank { "PRPH-${it.id}" },
                namaBarang = it.namaItem,
                serialNumber = it.serialNumber,
                currentRoom = it.lokasiRuang,
                jenisPerangkat = "PERIPHERAL"
            )
        }
        val listItems = itemsWithStock.map {
            DeviceOption(
                idBarang = it.idBarang,
                namaBarang = it.namaBarang,
                serialNumber = it.serialNumber,
                currentRoom = it.ruang,
                jenisPerangkat = if (it.type.equals("ALAT", ignoreCase = true)) "ALAT" else "LABKOM"
            )
        }
        (listPeripherals + listItems).distinctBy { it.idBarang }
    }

    val filteredCandidates = remember(candidateDevices, selectedDialogCategory, deviceSearchQuery) {
        candidateDevices.filter { dev ->
            val matchesCategory = when (selectedDialogCategory) {
                "Alat" -> dev.jenisPerangkat.equals("ALAT", ignoreCase = true)
                "Peripheral" -> dev.jenisPerangkat.equals("PERIPHERAL", ignoreCase = true)
                "LabKom" -> dev.jenisPerangkat.equals("LABKOM", ignoreCase = true) || dev.jenisPerangkat.contains("KOMPUTER", ignoreCase = true) || dev.jenisPerangkat.contains("PC", ignoreCase = true)
                else -> true
            }
            val matchesQuery = deviceSearchQuery.isBlank() ||
                    dev.namaBarang.contains(deviceSearchQuery, ignoreCase = true) ||
                    dev.idBarang.contains(deviceSearchQuery, ignoreCase = true) ||
                    dev.serialNumber.contains(deviceSearchQuery, ignoreCase = true) ||
                    dev.currentRoom.contains(deviceSearchQuery, ignoreCase = true)

            matchesCategory && matchesQuery
        }
    }

    var showDatePickerDialog by remember { mutableStateOf(false) }

    if (showDatePickerDialog) {
        val isoDate = try {
            if (tanggalMutasi.contains("/")) {
                val p = tanggalMutasi.split("/")
                if (p.size == 3) "${p[2]}-${p[1]}-${p[0]}" else tanggalMutasi
            } else tanggalMutasi
        } catch (_: Exception) { tanggalMutasi }

        LunarisDatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            selectedDateString = isoDate,
            onDateSelected = { newIso ->
                val formatted = try {
                    val p = newIso.split("-")
                    if (p.size == 3) "${p[2]}/${p[1]}/${p[0]}" else newIso
                } catch (_: Exception) { newIso }
                tanggalMutasi = formatted
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp)
                .testTag("dialog_add_mutasi")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Dialog Title Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEDE9FE),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color(0xFF7C3AED))
                            }
                        }
                        Column {
                            Text("Formulir Mutasi Perangkat", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                            Text("Pilih unit, audit kondisi & lokasi tujuan perpindahan", fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color(0xFF64748B))
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFE2E8F0))

                val customTextFieldColors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF7C3AED),
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedLabelColor = Color(0xFF7C3AED),
                    cursorColor = Color(0xFF7C3AED)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                ) {
                    item {
                        // 1. FILTER KATEGORI PERANGKAT (4 OPTION PILLS)
                        Text("Filter Kategori Perangkat", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF334155))
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Semua", "Alat", "Peripheral", "LabKom").forEach { cat ->
                                val isSelected = selectedDialogCategory == cat
                                Surface(
                                    onClick = { selectedDialogCategory = cat },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) Color(0xFF7C3AED) else Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF7C3AED) else Color(0xFFCBD5E1)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = cat,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else Color(0xFF475569),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        // 2. KOTAK PENCARIAN UTAMA (DROPDOWN & QR SCANNER)
                        Text("Pilih / Cari Perangkat *", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF334155))
                        Spacer(modifier = Modifier.height(4.dp))

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = if (selectedDeviceName.isNotBlank() && deviceSearchQuery == selectedDeviceName)
                                    "$selectedDeviceName ($selectedDeviceId)"
                                else deviceSearchQuery,
                                onValueChange = {
                                    deviceSearchQuery = it
                                    isDeviceDropdownExpanded = true
                                },
                                placeholder = { Text("Ketik nama / ID / scan QR...", fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Default.Computer, contentDescription = null, tint = Color(0xFF7C3AED)) },
                                trailingIcon = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { showQrScanner = true }) {
                                            Icon(
                                                imageVector = Icons.Default.QrCodeScanner,
                                                contentDescription = "Scan QR Perangkat",
                                                tint = Color(0xFF7C3AED)
                                            )
                                        }
                                        IconButton(onClick = { isDeviceDropdownExpanded = !isDeviceDropdownExpanded }) {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Pilih Perangkat")
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = customTextFieldColors,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_select_device")
                            )

                            DropdownMenu(
                                expanded = isDeviceDropdownExpanded,
                                onDismissRequest = { isDeviceDropdownExpanded = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .heightIn(max = 240.dp)
                            ) {
                                if (filteredCandidates.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("Tidak ada perangkat cocok", fontSize = 12.sp, color = Color.Gray) },
                                        onClick = { isDeviceDropdownExpanded = false }
                                    )
                                } else {
                                    filteredCandidates.forEach { dev ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text("${dev.namaBarang} [${dev.jenisPerangkat}]", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    Text("ID: ${dev.idBarang} | SN: ${dev.serialNumber.ifBlank { "-" }} | Posisi: ${dev.currentRoom.ifBlank { "Ruang Belum Diatur" }}", fontSize = 10.sp, color = Color(0xFF64748B))
                                                }
                                            },
                                            onClick = {
                                                selectedDeviceName = dev.namaBarang
                                                selectedDeviceId = dev.idBarang
                                                selectedDeviceSn = dev.serialNumber
                                                selectedDeviceType = dev.jenisPerangkat
                                                ruangAsal = dev.currentRoom
                                                deviceSearchQuery = dev.namaBarang
                                                isDeviceDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        // 3. FIELD SERIAL NUMBER (SN) & BARCODE SCANNER
                        OutlinedTextField(
                            value = selectedDeviceSn,
                            onValueChange = { newSn ->
                                selectedDeviceSn = newSn
                                if (newSn.isNotBlank()) {
                                    val matched = candidateDevices.find {
                                        it.serialNumber.equals(newSn.trim(), ignoreCase = true)
                                    }
                                    if (matched != null) {
                                        selectedDeviceName = matched.namaBarang
                                        selectedDeviceId = matched.idBarang
                                        selectedDeviceType = matched.jenisPerangkat
                                        ruangAsal = matched.currentRoom
                                        deviceSearchQuery = matched.namaBarang
                                    }
                                }
                            },
                            label = { Text("Serial Number (SN) / Barcode", fontSize = 11.sp) },
                            placeholder = { Text("Ketik SN atau pangkas lewat kamera...", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null, tint = Color(0xFF7C3AED)) },
                            trailingIcon = {
                                IconButton(onClick = { showBarcodeScanner = true }) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Scan Barcode SN",
                                        tint = Color(0xFF7C3AED)
                                    )
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = customTextFieldColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_serial_number_sn")
                        )
                    }

                    item {
                        // 4. KONDISI SEBELUM PINDAH (PULLS STRICTLY FROM MASTER DATA KONDISI)
                        Text("Kondisi Sebelum Pindah *", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF334155))
                        Spacer(modifier = Modifier.height(4.dp))

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = kondisiSebelumPindah.ifBlank { "Pilih Kondisi" },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Audit Kondisi Fisik Aset", fontSize = 11.sp) },
                                leadingIcon = { Icon(Icons.Default.Build, contentDescription = null, tint = Color(0xFF7C3AED)) },
                                trailingIcon = {
                                    IconButton(onClick = { isKondisiDropdownExpanded = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Pilih Kondisi")
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = customTextFieldColors,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isKondisiDropdownExpanded = true }
                                    .testTag("input_kondisi_sebelum_pindah")
                            )

                            DropdownMenu(
                                expanded = isKondisiDropdownExpanded,
                                onDismissRequest = { isKondisiDropdownExpanded = false }
                            ) {
                                masterConditions.forEach { cond ->
                                    DropdownMenuItem(
                                        text = { Text(cond, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                        onClick = {
                                            kondisiSebelumPindah = cond
                                            isKondisiDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        // 5. RUANG ASAL (Penempatan Saat Ini) - Interactive Dropdown from Master Data Ruang
                        Text("Ruang Asal (Penempatan Saat Ini) *", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF334155))
                        Spacer(modifier = Modifier.height(4.dp))

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = if (isCustomRuangAsal) "Lainnya (Input Manual)" else ruangAsal.ifBlank { "Pilih Ruang Asal" },
                                onValueChange = {},
                                readOnly = true,
                                enabled = false,
                                label = { Text("Ruang Asal (Penempatan Saat Ini)", fontSize = 11.sp) },
                                leadingIcon = { Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF7C3AED)) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = if (isRuangAsalDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = "Pilih Ruang Asal",
                                        tint = Color(0xFF7C3AED)
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = Color(0xFF1E293B),
                                    disabledBorderColor = Color(0xFFCBD5E1),
                                    disabledLabelColor = Color(0xFF64748B),
                                    disabledLeadingIconColor = Color(0xFF7C3AED),
                                    disabledTrailingIconColor = Color(0xFF7C3AED),
                                    disabledContainerColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_ruang_asal")
                            )

                            // Clickable transparent overlay covering entire field
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { isRuangAsalDropdownExpanded = !isRuangAsalDropdownExpanded }
                            )

                            DropdownMenu(
                                expanded = isRuangAsalDropdownExpanded,
                                onDismissRequest = { isRuangAsalDropdownExpanded = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .heightIn(max = 260.dp)
                            ) {
                                if (masterRooms.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("Tidak ada master ruang. Silakan isi manual.", fontSize = 12.sp, color = Color.Gray) },
                                        onClick = { isRuangAsalDropdownExpanded = false }
                                    )
                                } else {
                                    masterRooms.forEach { room ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.Place,
                                                        contentDescription = null,
                                                        tint = if (!isCustomRuangAsal && room == ruangAsal) Color(0xFF7C3AED) else Color(0xFF64748B),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = room,
                                                        fontSize = 12.sp,
                                                        fontWeight = if (!isCustomRuangAsal && room == ruangAsal) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (!isCustomRuangAsal && room == ruangAsal) Color(0xFF7C3AED) else Color(0xFF1E293B)
                                                    )
                                                }
                                            },
                                            onClick = {
                                                ruangAsal = room
                                                isCustomRuangAsal = false
                                                isRuangAsalDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("+ Lokasi / Ruang Asal Lainnya...", fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED), fontSize = 12.sp)
                                        }
                                    },
                                    onClick = {
                                        isCustomRuangAsal = true
                                        isRuangAsalDropdownExpanded = false
                                    }
                                )
                            }
                        }

                        if (isCustomRuangAsal) {
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = customRuangAsalText,
                                onValueChange = { customRuangAsalText = it },
                                label = { Text("Nama Ruang Asal Kustom", fontSize = 11.sp) },
                                placeholder = { Text("Contoh: Ruang Server / Gudang Barat", fontSize = 11.sp) },
                                shape = RoundedCornerShape(12.dp),
                                colors = customTextFieldColors,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_custom_ruang_asal")
                            )
                        }
                    }

                    item {
                        // 6. RUANG TUJUAN - Interactive Dropdown from Master Data Ruang
                        Text("Ruang / Lokasi Tujuan *", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF334155))
                        Spacer(modifier = Modifier.height(4.dp))

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = if (isCustomRuangTujuan) "Lainnya (Input Manual)" else ruangTujuan.ifBlank { "Pilih Ruang Tujuan" },
                                onValueChange = {},
                                readOnly = true,
                                enabled = false,
                                label = { Text("Ruang / Lokasi Tujuan", fontSize = 11.sp) },
                                leadingIcon = { Icon(Icons.Default.MeetingRoom, contentDescription = null, tint = Color(0xFF7C3AED)) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = if (isRoomDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = "Pilih Ruangan",
                                        tint = Color(0xFF7C3AED)
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = Color(0xFF1E293B),
                                    disabledBorderColor = Color(0xFFCBD5E1),
                                    disabledLabelColor = Color(0xFF64748B),
                                    disabledLeadingIconColor = Color(0xFF7C3AED),
                                    disabledTrailingIconColor = Color(0xFF7C3AED),
                                    disabledContainerColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_ruang_tujuan")
                            )

                            // Clickable transparent overlay covering entire field
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { isRoomDropdownExpanded = !isRoomDropdownExpanded }
                            )

                            DropdownMenu(
                                expanded = isRoomDropdownExpanded,
                                onDismissRequest = { isRoomDropdownExpanded = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .heightIn(max = 260.dp)
                            ) {
                                if (masterRooms.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("Tidak ada master ruang. Silakan isi manual.", fontSize = 12.sp, color = Color.Gray) },
                                        onClick = { isRoomDropdownExpanded = false }
                                    )
                                } else {
                                    masterRooms.forEach { room ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.MeetingRoom,
                                                        contentDescription = null,
                                                        tint = if (!isCustomRuangTujuan && room == ruangTujuan) Color(0xFF7C3AED) else Color(0xFF64748B),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = room,
                                                        fontSize = 12.sp,
                                                        fontWeight = if (!isCustomRuangTujuan && room == ruangTujuan) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (!isCustomRuangTujuan && room == ruangTujuan) Color(0xFF7C3AED) else Color(0xFF1E293B)
                                                    )
                                                }
                                            },
                                            onClick = {
                                                ruangTujuan = room
                                                isCustomRuangTujuan = false
                                                isRoomDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("+ Lokasi / Ruang Lainnya...", fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED), fontSize = 12.sp)
                                        }
                                    },
                                    onClick = {
                                        isCustomRuangTujuan = true
                                        isRoomDropdownExpanded = false
                                    }
                                )
                            }
                        }

                        if (isCustomRuangTujuan) {
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = customRuangTujuanText,
                                onValueChange = { customRuangTujuanText = it },
                                label = { Text("Nama Ruang Tujuan Baru", fontSize = 11.sp) },
                                placeholder = { Text("Contoh: Ruang Kepala Sekolah / Lab Biologi", fontSize = 11.sp) },
                                shape = RoundedCornerShape(12.dp),
                                colors = customTextFieldColors,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_custom_ruang_tujuan")
                            )
                        }
                    }

                    item {
                        // 7. TANGGAL MUTASI (KALENDER INTERAKSI) & PETUGAS
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showDatePickerDialog = true }
                            ) {
                                OutlinedTextField(
                                    value = tanggalMutasi,
                                    onValueChange = { tanggalMutasi = it },
                                    readOnly = true,
                                    label = { Text("Tanggal Mutasi *", fontSize = 11.sp) },
                                    leadingIcon = {
                                        IconButton(onClick = { showDatePickerDialog = true }) {
                                            Icon(Icons.Default.DateRange, contentDescription = "Pilih Kalender", tint = Color(0xFF7C3AED))
                                        }
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { showDatePickerDialog = true }) {
                                            Icon(Icons.Default.CalendarMonth, contentDescription = "Kalender", tint = Color(0xFF7C3AED))
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = customTextFieldColors,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_tanggal_mutasi")
                                )
                            }

                            OutlinedTextField(
                                value = namaPetugas,
                                onValueChange = { namaPetugas = it },
                                label = { Text("Petugas / Teknisi *", fontSize = 11.sp) },
                                shape = RoundedCornerShape(12.dp),
                                colors = customTextFieldColors,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        // 8. ALASAN MUTASI
                        OutlinedTextField(
                            value = alasanMutasi,
                            onValueChange = { alasanMutasi = it },
                            label = { Text("Alasan Mutasi / Perpindahan *", fontSize = 11.sp) },
                            placeholder = { Text("Contoh: Reorganisasi Lab 1, Penggantian Unit, Kebutuhan CBT", fontSize = 11.sp) },
                            shape = RoundedCornerShape(12.dp),
                            colors = customTextFieldColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_alasan_mutasi")
                        )
                    }

                    item {
                        // 9. KETERANGAN TAMBAHAN
                        OutlinedTextField(
                            value = keterangan,
                            onValueChange = { keterangan = it },
                            label = { Text("Keterangan Tambahan (Opsional)", fontSize = 11.sp) },
                            shape = RoundedCornerShape(12.dp),
                            colors = customTextFieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batal")
                    }

                    Button(
                        onClick = {
                            val finalSourceRoom = if (isCustomRuangAsal) customRuangAsalText.trim() else ruangAsal.trim()
                            val finalTargetRoom = if (isCustomRuangTujuan) customRuangTujuanText.trim() else ruangTujuan.trim()

                            if (selectedDeviceName.isBlank()) {
                                Toast.makeText(context, "Harap pilih perangkat yang akan dimutasi!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (finalSourceRoom.isBlank()) {
                                Toast.makeText(context, "Harap pilih atau isi ruang asal tempat perangkat berada saat ini!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (finalTargetRoom.isBlank()) {
                                Toast.makeText(context, "Harap pilih atau isi ruang tujuan!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (finalSourceRoom.equals(finalTargetRoom, ignoreCase = true)) {
                                Toast.makeText(context, "Ruang tujuan tidak boleh sama dengan ruang asal!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (alasanMutasi.isBlank()) {
                                Toast.makeText(context, "Harap isi alasan mutasi!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val fullKeterangan = "Kondisi Sebelum Pindah: $kondisiSebelumPindah. ${keterangan.trim()}".trim()

                            onSubmit(
                                selectedDeviceId,
                                selectedDeviceName,
                                selectedDeviceSn,
                                selectedDeviceType,
                                finalSourceRoom,
                                finalTargetRoom,
                                tanggalMutasi,
                                namaPetugas,
                                alasanMutasi,
                                fullKeterangan
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_submit_mutasi")
                    ) {
                        Text("Simpan Mutasi", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // QR SCANNER DIALOG
    if (showQrScanner) {
        CameraScannerDialog(
            title = "Pindai QR Perangkat Mutasi",
            initialMode = ScanMode.PRIMARY_QR,
            onDismissRequest = { showQrScanner = false },
            onBarcodeScanned = { scannedCode ->
                showQrScanner = false
                val matched = candidateDevices.find {
                    it.idBarang.equals(scannedCode.trim(), ignoreCase = true) ||
                    it.serialNumber.equals(scannedCode.trim(), ignoreCase = true) ||
                    it.namaBarang.contains(scannedCode.trim(), ignoreCase = true)
                }
                if (matched != null) {
                    selectedDeviceName = matched.namaBarang
                    selectedDeviceId = matched.idBarang
                    selectedDeviceSn = matched.serialNumber
                    selectedDeviceType = matched.jenisPerangkat
                    ruangAsal = matched.currentRoom
                    deviceSearchQuery = matched.namaBarang
                    Toast.makeText(context, "Perangkat Ditemukan: ${matched.namaBarang}", Toast.LENGTH_SHORT).show()
                } else {
                    deviceSearchQuery = scannedCode.trim()
                    isDeviceDropdownExpanded = true
                    Toast.makeText(context, "Hasil Pindai QR: $scannedCode. Silakan pilih dari daftar.", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // BARCODE / SERIAL NUMBER SCANNER DIALOG
    if (showBarcodeScanner) {
        CameraScannerDialog(
            title = "Pindai Barcode / Serial Number",
            initialMode = ScanMode.FALLBACK_BARCODE,
            onDismissRequest = { showBarcodeScanner = false },
            onBarcodeScanned = { scannedSn ->
                showBarcodeScanner = false
                selectedDeviceSn = scannedSn.trim()
                val matched = candidateDevices.find {
                    it.serialNumber.equals(scannedSn.trim(), ignoreCase = true) ||
                    it.idBarang.equals(scannedSn.trim(), ignoreCase = true)
                }
                if (matched != null) {
                    selectedDeviceName = matched.namaBarang
                    selectedDeviceId = matched.idBarang
                    selectedDeviceType = matched.jenisPerangkat
                    ruangAsal = matched.currentRoom
                    deviceSearchQuery = matched.namaBarang
                    Toast.makeText(context, "Perangkat Ditemukan via SN: ${matched.namaBarang}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "SN Terpindai: $scannedSn", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
private fun DetailMutasiDialog(
    item: MutasiPerangkatEntity,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Detail Mutasi Perangkat", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFE2E8F0))

                DetailRow("ID Mutasi", item.idMutasi)
                DetailRow("Nama Perangkat", item.namaBarang)
                DetailRow("ID / Kode Barang", item.idBarang)
                if (item.serialNumber.isNotBlank()) DetailRow("Serial Number", item.serialNumber)
                DetailRow("Jenis Perangkat", item.jenisPerangkat)
                DetailRow("Ruang Asal", item.ruangAsal)
                DetailRow("Ruang Tujuan", item.ruangTujuan)
                DetailRow("Tanggal Mutasi", item.tanggalMutasi)
                DetailRow("Petugas / Teknisi", item.namaPetugas)
                DetailRow("Alasan Mutasi", item.alasanMutasi)
                if (item.keterangan.isNotBlank()) DetailRow("Keterangan", item.keterangan)

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tutup", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = Color(0xFF64748B))
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
    }
}

private data class DeviceOption(
    val idBarang: String,
    val namaBarang: String,
    val serialNumber: String,
    val currentRoom: String,
    val jenisPerangkat: String
)
