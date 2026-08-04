package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.BahanAfkirEntity
import com.example.data.entity.DamagedItemEntity
import com.example.data.entity.PeripheralRusakEntity
import com.example.ui.components.LunarisCard
import com.example.ui.components.LunarisTextField
import com.example.ui.theme.DeepPurpleText
import com.example.ui.viewmodel.InventoryViewModel

data class HibahItemDisplay(
    val id: String,
    val idBarang: String,
    val namaBarang: String,
    val kategori: String,
    val jumlah: Int,
    val satuan: String,
    val penerimaHibah: String,
    val alasanHibah: String,
    val tanggalHibah: String,
    val petugas: String
)

private fun cleanCategoryText(rawCategory: String): String {
    if (rawCategory.isBlank()) return "Peripheral"
    val cleaned = rawCategory
        .replace(Regex("[\\p{So}\\p{Cn}\\p{Cs}\\p{Extended_Pictographic}]"), "")
        .replace(Regex("^[\\s\\W_]+"), "")
        .trim()
    return if (cleaned.isNotBlank()) cleaned else rawCategory.trim()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HapusAsetScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allDamagedItems by viewModel.allDamagedItems.collectAsState()
    val allBahanAfkir by viewModel.allBahanAfkir.collectAsState()
    val hapusAsetPeripheralItems by viewModel.hapusAsetPeripheralItems.collectAsState()
    val defaultOfficer by viewModel.defaultOfficer.collectAsState()
    val allItemsWithStock by viewModel.itemsWithStock.collectAsState()

    var selectedTabState by remember { mutableIntStateOf(0) } // 0 = Alat, 1 = Bahan, 2 = Peripheral, 3 = List Hibah
    var selectedHibahDetail by remember { mutableStateOf<HibahItemDisplay?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showQrScanner by remember { mutableStateOf(false) }

    // Dialog States
    var itemToHibahAlat by remember { mutableStateOf<DamagedItemEntity?>(null) }
    var itemToHibahBahan by remember { mutableStateOf<BahanAfkirEntity?>(null) }
    var itemToHibahPeripheral by remember { mutableStateOf<PeripheralRusakEntity?>(null) }

    // Destructive 2x Confirmation States
    var itemToHapusStep1Alat by remember { mutableStateOf<DamagedItemEntity?>(null) }
    var itemToHapusStep2Alat by remember { mutableStateOf<DamagedItemEntity?>(null) }

    var itemToHapusStep1Bahan by remember { mutableStateOf<BahanAfkirEntity?>(null) }
    var itemToHapusStep2Bahan by remember { mutableStateOf<BahanAfkirEntity?>(null) }

    var itemToHapusStep1Peripheral by remember { mutableStateOf<PeripheralRusakEntity?>(null) }
    var itemToHapusStep2Peripheral by remember { mutableStateOf<PeripheralRusakEntity?>(null) }

    // Hibah Form Inputs
    var penerimaHibahInput by remember { mutableStateOf("") }
    var alasanHibahInput by remember { mutableStateOf("") }
    var officerNameInput by remember { mutableStateOf("") }

    // Validation Confirmation Input
    var confirmValidationText by remember { mutableStateOf("") }

    // Filter Alat for Hapus Aset
    val filteredAlat = remember(allDamagedItems, searchQuery) {
        val baseList = allDamagedItems.filter {
            !it.isHibah && (
                it.status == "Hapus Aset" ||
                it.status == "Siap Afkir" ||
                it.validationCount >= 2 ||
                it.status == "Rusak (Permanen)" ||
                it.status.contains("Rusak", ignoreCase = true)
            )
        }
        if (searchQuery.isBlank()) {
            baseList
        } else {
            baseList.filter {
                it.namaBarang.contains(searchQuery, ignoreCase = true) ||
                it.idBarang.contains(searchQuery, ignoreCase = true) ||
                it.keteranganKerusakan.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Filter Bahan for Hapus Aset
    val filteredBahan = remember(allBahanAfkir, searchQuery) {
        val baseList = allBahanAfkir.filter {
            it.status == "Hapus Aset" || it.status == "Siap Hapus" || it.status == "Aktif"
        }
        if (searchQuery.isBlank()) {
            baseList
        } else {
            baseList.filter {
                it.namaBarang.contains(searchQuery, ignoreCase = true) ||
                it.idAfkir.contains(searchQuery, ignoreCase = true) ||
                it.alasan.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Filter Peripheral for Hapus Aset
    val filteredPeripheral = remember(hapusAsetPeripheralItems, searchQuery) {
        val baseList = hapusAsetPeripheralItems.filter { !it.isHibah }
        if (searchQuery.isBlank()) {
            baseList
        } else {
            baseList.filter {
                it.namaBarang.contains(searchQuery, ignoreCase = true) ||
                it.idBarang.contains(searchQuery, ignoreCase = true) ||
                it.subKategori.contains(searchQuery, ignoreCase = true) ||
                it.keteranganKerusakan.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val filteredHibah = remember(allDamagedItems, hapusAsetPeripheralItems, allBahanAfkir, allItemsWithStock, searchQuery, defaultOfficer) {
        val list = mutableListOf<HibahItemDisplay>()

        allDamagedItems.filter {
            it.isHibah || it.status.equals("Dihibahkan", ignoreCase = true) || it.status.contains("Hibah", ignoreCase = true)
        }.forEach { damaged ->
            val mainItem = allItemsWithStock.find { it.idBarang == damaged.idBarang }
            val cat = when {
                mainItem?.type == "PC" || mainItem?.kategori.equals("Lab Komputer", ignoreCase = true) || mainItem?.kategori.equals("Komputer", ignoreCase = true) -> "PC / LabKom"
                mainItem?.type == "PERIPHERAL" || mainItem?.kategori.equals("Peripheral", ignoreCase = true) -> "Peripheral"
                else -> "Alat"
            }
            list.add(
                HibahItemDisplay(
                    id = "damaged_${damaged.id}",
                    idBarang = damaged.idBarang,
                    namaBarang = damaged.namaBarang,
                    kategori = cat,
                    jumlah = damaged.jumlah,
                    satuan = mainItem?.satuan ?: "Unit",
                    penerimaHibah = damaged.penerimaHibah.ifBlank { "Instansi Pihak Luar" },
                    alasanHibah = damaged.alasanHibah.ifBlank { damaged.keteranganKerusakan.ifBlank { "Hibah Aset" } },
                    tanggalHibah = damaged.lastValidatedDate.ifBlank { damaged.tanggalKerusakan },
                    petugas = damaged.lastValidatedBy.ifBlank { damaged.namaPetugas.ifBlank { defaultOfficer } }
                )
            )
        }

        hapusAsetPeripheralItems.filter {
            it.isHibah || it.status.equals("Dihibahkan", ignoreCase = true) || it.status.contains("Hibah", ignoreCase = true)
        }.forEach { periph ->
            list.add(
                HibahItemDisplay(
                    id = "periph_${periph.id}",
                    idBarang = periph.idBarang,
                    namaBarang = periph.namaBarang,
                    kategori = "Peripheral (${cleanCategoryText(periph.subKategori)})",
                    jumlah = periph.jumlah,
                    satuan = "Unit",
                    penerimaHibah = periph.penerimaHibah.ifBlank { "Instansi Pihak Luar" },
                    alasanHibah = periph.alasanHibah.ifBlank { periph.keteranganKerusakan.ifBlank { "Hibah Periferal" } },
                    tanggalHibah = periph.tanggalKerusakan,
                    petugas = periph.namaPetugas.ifBlank { defaultOfficer }
                )
            )
        }

        allBahanAfkir.filter {
            it.status.equals("Hibah", ignoreCase = true) || it.status.equals("Dihibahkan", ignoreCase = true)
        }.forEach { bahan ->
            list.add(
                HibahItemDisplay(
                    id = "bahan_${bahan.idAfkir}",
                    idBarang = bahan.idAfkir,
                    namaBarang = bahan.namaBarang,
                    kategori = "Bahan",
                    jumlah = bahan.jumlahAfkir,
                    satuan = bahan.satuan.ifBlank { "Satuan" },
                    penerimaHibah = "Instansi / Pihak Luar",
                    alasanHibah = bahan.alasan.ifBlank { "Hibah Bahan Afkir" },
                    tanggalHibah = bahan.tanggalAfkir,
                    petugas = defaultOfficer.ifBlank { "Administrator" }
                )
            )
        }

        if (searchQuery.isBlank()) {
            list
        } else {
            list.filter {
                it.namaBarang.contains(searchQuery, ignoreCase = true) ||
                it.idBarang.contains(searchQuery, ignoreCase = true) ||
                it.penerimaHibah.contains(searchQuery, ignoreCase = true) ||
                it.alasanHibah.contains(searchQuery, ignoreCase = true) ||
                it.kategori.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val unselectedTabColor = Color.Gray.copy(alpha = 0.8f)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .shadow(elevation = 3.dp, shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                        .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                        .background(Color(0xFFDC2626))
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
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
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "Hapus Aset",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Zona Bahaya: Penghapusan data aset tercatat",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 3 Tabs: Alat, Bahan, and Peripheral
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 1.dp)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(vertical = 2.dp)
                ) {
                    TabRow(
                        selectedTabIndex = selectedTabState,
                        containerColor = Color.Transparent,
                        divider = {},
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTabState]),
                                height = 3.dp,
                                color = Color(0xFFDC2626)
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
                                        imageVector = Icons.Default.Build,
                                        contentDescription = null,
                                        tint = if (selectedTabState == 0) Color(0xFFDC2626) else unselectedTabColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Alat (${filteredAlat.size})",
                                        fontWeight = if (selectedTabState == 0) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedTabState == 0) Color(0xFFDC2626) else unselectedTabColor,
                                        fontSize = 13.sp
                                    )
                                }
                            },
                            modifier = Modifier.testTag("tab_hapus_alat")
                        )

                        Tab(
                            selected = selectedTabState == 1,
                            onClick = { selectedTabState = 1 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Science,
                                        contentDescription = null,
                                        tint = if (selectedTabState == 1) Color(0xFFDC2626) else unselectedTabColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Bahan (${filteredBahan.size})",
                                        fontWeight = if (selectedTabState == 1) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedTabState == 1) Color(0xFFDC2626) else unselectedTabColor,
                                        fontSize = 13.sp
                                    )
                                }
                            },
                            modifier = Modifier.testTag("tab_hapus_bahan")
                        )

                        Tab(
                            selected = selectedTabState == 2,
                            onClick = { selectedTabState = 2 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Computer,
                                        contentDescription = null,
                                        tint = if (selectedTabState == 2) Color(0xFFDC2626) else unselectedTabColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Peripheral (${filteredPeripheral.size})",
                                        fontWeight = if (selectedTabState == 2) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedTabState == 2) Color(0xFFDC2626) else unselectedTabColor,
                                        fontSize = 13.sp
                                    )
                                }
                            },
                            modifier = Modifier.testTag("tab_hapus_peripheral")
                        )

                        Tab(
                            selected = selectedTabState == 3,
                            onClick = { selectedTabState = 3 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CardGiftcard,
                                        contentDescription = null,
                                        tint = if (selectedTabState == 3) Color(0xFF8B5CF6) else unselectedTabColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "List Hibah (${filteredHibah.size})",
                                        fontWeight = if (selectedTabState == 3) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedTabState == 3) Color(0xFF8B5CF6) else unselectedTabColor,
                                        fontSize = 13.sp
                                    )
                                }
                            },
                            modifier = Modifier.testTag("tab_hapus_hibah")
                        )
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            LunarisTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Cari aset yang akan dihapus / dihibahkan...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
                            }
                        }
                        IconButton(onClick = { showQrScanner = true }) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan QR",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("search_hapus_aset")
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

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedTabState == 0) {
                // TAB ALAT
                if (filteredAlat.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Tidak ada data alat afkir / siap hapus.", color = Color.Gray)
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(filteredAlat, key = { it.id }) { item ->
                            LunarisCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = item.namaBarang,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Surface(
                                            color = Color(0xFFFEE2E2),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "Alat Rusak / Afkir",
                                                color = Color(0xFFDC2626),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("ID Barang: ${item.idBarang} | Jumlah: ${item.jumlah} unit", fontSize = 13.sp, color = Color.Gray)
                                    if (item.keteranganKerusakan.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Keterangan: ${item.keteranganKerusakan}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Tindakan Aset:",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.Gray
                                        )

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Hibah - Pure Icon Button
                                            IconButton(
                                                onClick = {
                                                    itemToHibahAlat = item
                                                    penerimaHibahInput = ""
                                                    alasanHibahInput = item.keteranganKerusakan
                                                    officerNameInput = defaultOfficer
                                                },
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .testTag("btn_hibah_alat_${item.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CardGiftcard,
                                                    contentDescription = "Hibah Aset",
                                                    tint = Color(0xFF0284C7),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            // Hapus - Pure Icon Button
                                            IconButton(
                                                onClick = { itemToHapusStep1Alat = item },
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .testTag("btn_hapus_alat_${item.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Hapus Aset",
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
            } else if (selectedTabState == 1) {
                // TAB BAHAN
                if (filteredBahan.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Tidak ada data bahan afkir / siap hapus.", color = Color.Gray)
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(filteredBahan, key = { it.idAfkir }) { item ->
                            LunarisCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = item.namaBarang,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Surface(
                                            color = Color(0xFFFFEDD5),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "Bahan Afkir",
                                                color = Color(0xFFEA580C),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("ID Afkir: ${item.idAfkir} | Jumlah: ${item.jumlahAfkir} ${item.satuan}", fontSize = 13.sp, color = Color.Gray)
                                    if (item.alasan.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Alasan: ${item.alasan}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Tindakan Aset:",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.Gray
                                        )

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Hibah - Pure Icon Button
                                            IconButton(
                                                onClick = {
                                                    itemToHibahBahan = item
                                                    penerimaHibahInput = ""
                                                    alasanHibahInput = item.alasan
                                                    officerNameInput = defaultOfficer
                                                },
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .testTag("btn_hibah_bahan_${item.idAfkir}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CardGiftcard,
                                                    contentDescription = "Hibah Aset",
                                                    tint = Color(0xFF0284C7),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            // Hapus - Pure Icon Button
                                            IconButton(
                                                onClick = { itemToHapusStep1Bahan = item },
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .testTag("btn_hapus_bahan_${item.idAfkir}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Hapus Aset",
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
            } else if (selectedTabState == 2) {
                // TAB PERIPHERAL
                if (filteredPeripheral.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Tidak ada data peripheral afkir / siap hapus.", color = Color.Gray)
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(filteredPeripheral, key = { it.id }) { item ->
                            LunarisCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = item.namaBarang,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Surface(
                                            color = Color(0xFFF3E8FF),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = cleanCategoryText(item.subKategori),
                                                color = Color(0xFF7E22CE),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("ID: ${item.idBarang} | Jumlah: ${item.jumlah} unit", fontSize = 13.sp, color = Color.Gray)
                                    if (item.keteranganKerusakan.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Kronologi / Kerusakan: ${item.keteranganKerusakan}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Tindakan Aset:",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.Gray
                                        )

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Hibah - Pure Icon Button
                                            IconButton(
                                                onClick = {
                                                    itemToHibahPeripheral = item
                                                    penerimaHibahInput = ""
                                                    alasanHibahInput = item.keteranganKerusakan
                                                    officerNameInput = defaultOfficer
                                                },
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .testTag("btn_hibah_peripheral_${item.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CardGiftcard,
                                                    contentDescription = "Hibah Aset",
                                                    tint = Color(0xFF0284C7),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            // Hapus - Pure Icon Button
                                            IconButton(
                                                onClick = { itemToHapusStep1Peripheral = item },
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .testTag("btn_hapus_peripheral_${item.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Hapus Aset",
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
            } else {
                // TAB LIST HIBAH
                if (filteredHibah.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Belum ada data aset yang dihibahkan.", color = Color.Gray, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Aset yang dihibahkan melalui tombol Hibah akan tercatat di sini.", color = Color.Gray.copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(filteredHibah, key = { it.id }) { item ->
                            LunarisCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedHibahDetail = item }
                                    .testTag("card_hibah_${item.id}")
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = item.namaBarang,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Surface(
                                            color = Color(0xFFF3E8FF),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = item.kategori,
                                                color = Color(0xFF7E22CE),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("ID Aset: ${item.idBarang} | Jumlah: ${item.jumlah} ${item.satuan}", fontSize = 13.sp, color = Color.Gray)
                                    
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Business, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Penerima / Instansi: ", fontSize = 13.sp, color = Color.Gray)
                                        Text(item.penerimaHibah, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7E22CE))
                                    }

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Tanggal: ${item.tanggalHibah}", fontSize = 12.sp, color = Color.Gray)
                                        TextButton(
                                            onClick = { selectedHibahDetail = item },
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("Lihat Detail Hibah", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8B5CF6))
                                            Spacer(Modifier.width(4.dp))
                                            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF8B5CF6))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ================= DIALOGS =================

        // 1. HIBAH ALAT DIALOG
        if (itemToHibahAlat != null) {
            AlertDialog(
                onDismissRequest = { itemToHibahAlat = null },
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                icon = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFFE0F2FE), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CardGiftcard,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                },
                title = { Text("Proses Hibah Alat", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Merekam hibah untuk alat '${itemToHibahAlat!!.namaBarang}'.", color = Color(0xFF334155))
                        LunarisTextField(
                            value = penerimaHibahInput,
                            onValueChange = { penerimaHibahInput = it },
                            label = { Text("Nama Penerima / Instansi *") },
                            modifier = Modifier.fillMaxWidth().testTag("input_penerima_hibah")
                        )
                        LunarisTextField(
                            value = alasanHibahInput,
                            onValueChange = { alasanHibahInput = it },
                            label = { Text("Alasan / Catatan Hibah") },
                            modifier = Modifier.fillMaxWidth().testTag("input_alasan_hibah")
                        )
                        LunarisTextField(
                            value = officerNameInput,
                            onValueChange = { officerNameInput = it },
                            label = { Text("Nama Petugas") },
                            modifier = Modifier.fillMaxWidth().testTag("input_petugas_hibah")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val record = itemToHibahAlat!!
                            itemToHibahAlat = null
                            viewModel.recordHibahAset(
                                id = record.id,
                                penerima = penerimaHibahInput,
                                alasan = alasanHibahInput,
                                officerName = officerNameInput,
                                onSuccess = {
                                    Toast.makeText(context, "Hibah alat '${record.namaBarang}' berhasil dicatat!", Toast.LENGTH_LONG).show()
                                },
                                onError = { err ->
                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Simpan Hibah", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { itemToHibahAlat = null },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Text("Batal", color = Color(0xFF475569))
                    }
                }
            )
        }

        // 2. HIBAH BAHAN DIALOG
        if (itemToHibahBahan != null) {
            AlertDialog(
                onDismissRequest = { itemToHibahBahan = null },
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                icon = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFFE0F2FE), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CardGiftcard,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                },
                title = { Text("Proses Hibah Bahan Afkir", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Merekam hibah untuk bahan afkir '${itemToHibahBahan!!.namaBarang}'.", color = Color(0xFF334155))
                        LunarisTextField(
                            value = penerimaHibahInput,
                            onValueChange = { penerimaHibahInput = it },
                            label = { Text("Nama Penerima / Instansi *") },
                            modifier = Modifier.fillMaxWidth().testTag("input_penerima_hibah_bahan")
                        )
                        LunarisTextField(
                            value = alasanHibahInput,
                            onValueChange = { alasanHibahInput = it },
                            label = { Text("Alasan / Catatan Hibah") },
                            modifier = Modifier.fillMaxWidth().testTag("input_alasan_hibah_bahan")
                        )
                        LunarisTextField(
                            value = officerNameInput,
                            onValueChange = { officerNameInput = it },
                            label = { Text("Nama Petugas") },
                            modifier = Modifier.fillMaxWidth().testTag("input_petugas_hibah_bahan")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val record = itemToHibahBahan!!
                            itemToHibahBahan = null
                            viewModel.recordHibahBahanAfkir(
                                idAfkir = record.idAfkir,
                                penerima = penerimaHibahInput,
                                alasan = alasanHibahInput,
                                officerName = officerNameInput,
                                onSuccess = {
                                    Toast.makeText(context, "Hibah bahan afkir '${record.namaBarang}' berhasil dicatat!", Toast.LENGTH_LONG).show()
                                },
                                onError = { err ->
                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Simpan Hibah", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { itemToHibahBahan = null },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Text("Batal", color = Color(0xFF475569))
                    }
                }
            )
        }

        // 3. HIBAH PERIPHERAL DIALOG
        if (itemToHibahPeripheral != null) {
            AlertDialog(
                onDismissRequest = { itemToHibahPeripheral = null },
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                icon = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFFE0F2FE), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CardGiftcard,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                },
                title = { Text("Proses Hibah Peripheral", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Merekam hibah untuk peripheral '${itemToHibahPeripheral!!.namaBarang}'.", color = Color(0xFF334155))
                        LunarisTextField(
                            value = penerimaHibahInput,
                            onValueChange = { penerimaHibahInput = it },
                            label = { Text("Nama Penerima / Instansi *") },
                            modifier = Modifier.fillMaxWidth().testTag("input_penerima_hibah_peripheral")
                        )
                        LunarisTextField(
                            value = alasanHibahInput,
                            onValueChange = { alasanHibahInput = it },
                            label = { Text("Alasan / Catatan Hibah") },
                            modifier = Modifier.fillMaxWidth().testTag("input_alasan_hibah_peripheral")
                        )
                        LunarisTextField(
                            value = officerNameInput,
                            onValueChange = { officerNameInput = it },
                            label = { Text("Nama Petugas") },
                            modifier = Modifier.fillMaxWidth().testTag("input_petugas_hibah_peripheral")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val record = itemToHibahPeripheral!!
                            itemToHibahPeripheral = null
                            viewModel.recordHibahPeripheralRusak(
                                id = record.id,
                                penerima = penerimaHibahInput,
                                alasan = alasanHibahInput,
                                officerName = officerNameInput,
                                onSuccess = {
                                    Toast.makeText(context, "Hibah peripheral '${record.namaBarang}' berhasil dicatat!", Toast.LENGTH_LONG).show()
                                },
                                onError = { err ->
                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Simpan Hibah", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { itemToHibahPeripheral = null },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Text("Batal", color = Color(0xFF475569))
                    }
                }
            )
        }

        // 4. ALAT: DESTRUCTIVE STEP 1 (PERINGATAN KEAMANAN BERLAPIS)
        if (itemToHapusStep1Alat != null) {
            AlertDialog(
                onDismissRequest = { itemToHapusStep1Alat = null },
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                icon = {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color(0xFFFEE2E2), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Peringatan Hapus",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                },
                title = {
                    Text("Peringatan Keamanan (1/2)", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626), fontSize = 18.sp)
                },
                text = {
                    Text(
                        "PERHATIAN: Anda akan menghapus alat '${itemToHapusStep1Alat!!.namaBarang}' secara PERMANEN dari seluruh sistem dan database inventaris.\n\nTindakan ini destruktif dan tidak dapat dikembalikan lagi. Apakah Anda yakin ingin melanjutkannya?",
                        fontSize = 14.sp,
                        color = Color(0xFF334155)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val selected = itemToHapusStep1Alat
                            itemToHapusStep1Alat = null
                            confirmValidationText = ""
                            itemToHapusStep2Alat = selected
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Lanjutkan Validasi Akhir (2/2)", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { itemToHapusStep1Alat = null },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Text("Batal", color = Color(0xFF475569))
                    }
                }
            )
        }

        // 5. ALAT: DESTRUCTIVE STEP 2 (VALIDASI GANDA KONFIRMASI TEKS)
        if (itemToHapusStep2Alat != null) {
            AlertDialog(
                onDismissRequest = { itemToHapusStep2Alat = null },
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                icon = {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color(0xFFFEE2E2), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Konfirmasi Ganda",
                            tint = Color(0xFFB91C1C),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                },
                title = {
                    Text("Validasi Konfirmasi Ganda (2/2)", fontWeight = FontWeight.Bold, color = Color(0xFFB91C1C), fontSize = 18.sp)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Ketik kata 'HAPUS' di bawah ini untuk mengonfirmasi penghapusan permanen alat '${itemToHapusStep2Alat!!.namaBarang}':",
                            fontSize = 14.sp,
                            color = Color(0xFF334155)
                        )
                        LunarisTextField(
                            value = confirmValidationText,
                            onValueChange = { confirmValidationText = it },
                            label = { Text("Ketik 'HAPUS'") },
                            modifier = Modifier.fillMaxWidth().testTag("input_confirm_hapus_alat")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val record = itemToHapusStep2Alat!!
                            itemToHapusStep2Alat = null
                            viewModel.deleteDamagedItemPermanently(
                                id = record.id,
                                namaPetugas = defaultOfficer,
                                onSuccess = {
                                    Toast.makeText(context, "Aset alat '${record.namaBarang}' telah berhasil dihapus permanen dari sistem!", Toast.LENGTH_LONG).show()
                                },
                                onError = { err ->
                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        enabled = confirmValidationText.trim().equals("HAPUS", ignoreCase = true),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("HAPUS PERMANEN SEKARANG", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { itemToHapusStep2Alat = null },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Text("Batal", color = Color(0xFF475569))
                    }
                }
            )
        }

        // 6. BAHAN: DESTRUCTIVE STEP 1 (PERINGATAN KEAMANAN BERLAPIS)
        if (itemToHapusStep1Bahan != null) {
            AlertDialog(
                onDismissRequest = { itemToHapusStep1Bahan = null },
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                icon = {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color(0xFFFEE2E2), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Peringatan Hapus",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                },
                title = {
                    Text("Peringatan Keamanan (1/2)", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626), fontSize = 18.sp)
                },
                text = {
                    Text(
                        "PERHATIAN: Anda akan menghapus bahan afkir '${itemToHapusStep1Bahan!!.namaBarang}' secara PERMANEN dari sistem inventaris.\n\nTindakan ini destruktif dan tidak dapat dikembalikan lagi. Apakah Anda yakin ingin melanjutkannya?",
                        fontSize = 14.sp,
                        color = Color(0xFF334155)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val selected = itemToHapusStep1Bahan
                            itemToHapusStep1Bahan = null
                            confirmValidationText = ""
                            itemToHapusStep2Bahan = selected
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Lanjutkan Validasi Akhir (2/2)", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { itemToHapusStep1Bahan = null },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Text("Batal", color = Color(0xFF475569))
                    }
                }
            )
        }

        // 7. BAHAN: DESTRUCTIVE STEP 2 (VALIDASI GANDA KONFIRMASI TEKS)
        if (itemToHapusStep2Bahan != null) {
            AlertDialog(
                onDismissRequest = { itemToHapusStep2Bahan = null },
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                icon = {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color(0xFFFEE2E2), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Konfirmasi Ganda",
                            tint = Color(0xFFB91C1C),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                },
                title = {
                    Text("Validasi Konfirmasi Ganda (2/2)", fontWeight = FontWeight.Bold, color = Color(0xFFB91C1C), fontSize = 18.sp)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Ketik kata 'HAPUS' di bawah ini untuk mengonfirmasi penghapusan permanen bahan afkir '${itemToHapusStep2Bahan!!.namaBarang}':",
                            fontSize = 14.sp,
                            color = Color(0xFF334155)
                        )
                        LunarisTextField(
                            value = confirmValidationText,
                            onValueChange = { confirmValidationText = it },
                            label = { Text("Ketik 'HAPUS'") },
                            modifier = Modifier.fillMaxWidth().testTag("input_confirm_hapus_bahan")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val record = itemToHapusStep2Bahan!!
                            itemToHapusStep2Bahan = null
                            viewModel.deleteBahanAfkirPermanently(
                                idAfkir = record.idAfkir,
                                namaPetugas = defaultOfficer,
                                onSuccess = {
                                    Toast.makeText(context, "Bahan afkir '${record.namaBarang}' telah berhasil dihapus permanen dari sistem!", Toast.LENGTH_LONG).show()
                                },
                                onError = { err ->
                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        enabled = confirmValidationText.trim().equals("HAPUS", ignoreCase = true),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("HAPUS PERMANEN SEKARANG", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { itemToHapusStep2Bahan = null },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Text("Batal", color = Color(0xFF475569))
                    }
                }
            )
        }

        // 8. PERIPHERAL: DESTRUCTIVE STEP 1 (PERINGATAN KEAMANAN BERLAPIS)
        if (itemToHapusStep1Peripheral != null) {
            AlertDialog(
                onDismissRequest = { itemToHapusStep1Peripheral = null },
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                icon = {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color(0xFFFEE2E2), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Peringatan Hapus",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                },
                title = {
                    Text("Peringatan Keamanan (1/2)", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626), fontSize = 18.sp)
                },
                text = {
                    Text(
                        "PERHATIAN: Anda akan menghapus peripheral '${itemToHapusStep1Peripheral!!.namaBarang}' secara PERMANEN dari sistem inventaris.\n\nTindakan ini destruktif dan tidak dapat dikembalikan lagi. Apakah Anda yakin ingin melanjutkannya?",
                        fontSize = 14.sp,
                        color = Color(0xFF334155)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val selected = itemToHapusStep1Peripheral
                            itemToHapusStep1Peripheral = null
                            confirmValidationText = ""
                            itemToHapusStep2Peripheral = selected
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Lanjutkan Validasi Akhir (2/2)", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { itemToHapusStep1Peripheral = null },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Text("Batal", color = Color(0xFF475569))
                    }
                }
            )
        }

        // 9. PERIPHERAL: DESTRUCTIVE STEP 2 (VALIDASI GANDA KONFIRMASI TEKS)
        if (itemToHapusStep2Peripheral != null) {
            AlertDialog(
                onDismissRequest = { itemToHapusStep2Peripheral = null },
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                icon = {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color(0xFFFEE2E2), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Konfirmasi Ganda",
                            tint = Color(0xFFB91C1C),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                },
                title = {
                    Text("Validasi Konfirmasi Ganda (2/2)", fontWeight = FontWeight.Bold, color = Color(0xFFB91C1C), fontSize = 18.sp)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Ketik kata 'HAPUS' di bawah ini untuk mengonfirmasi penghapusan permanen peripheral '${itemToHapusStep2Peripheral!!.namaBarang}':",
                            fontSize = 14.sp,
                            color = Color(0xFF334155)
                        )
                        LunarisTextField(
                            value = confirmValidationText,
                            onValueChange = { confirmValidationText = it },
                            label = { Text("Ketik 'HAPUS'") },
                            modifier = Modifier.fillMaxWidth().testTag("input_confirm_hapus_peripheral")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val record = itemToHapusStep2Peripheral!!
                            itemToHapusStep2Peripheral = null
                            viewModel.deletePeripheralRusakPermanently(
                                id = record.id,
                                namaPetugas = defaultOfficer,
                                onSuccess = {
                                    Toast.makeText(context, "Peripheral '${record.namaBarang}' telah berhasil dihapus permanen dari sistem!", Toast.LENGTH_LONG).show()
                                },
                                onError = { err ->
                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        enabled = confirmValidationText.trim().equals("HAPUS", ignoreCase = true),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("HAPUS PERMANEN SEKARANG", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { itemToHapusStep2Peripheral = null },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Text("Batal", color = Color(0xFF475569))
                    }
                }
            )
        }
        // 5. DETAIL HIBAH POPUP DIALOG
        if (selectedHibahDetail != null) {
            val detail = selectedHibahDetail!!
            AlertDialog(
                onDismissRequest = { selectedHibahDetail = null },
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                icon = {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color(0xFFF3E8FF), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CardGiftcard,
                            contentDescription = null,
                            tint = Color(0xFF7E22CE),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Detail Informasi Hibah Aset", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0F172A))
                        Text(detail.namaBarang, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Color(0xFF7E22CE))
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("ID Aset / Barang:", fontSize = 12.sp, color = Color.Gray)
                                    Text(detail.idBarang, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Kategori Perangkat:", fontSize = 12.sp, color = Color.Gray)
                                    Text(detail.kategori, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7E22CE))
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Jumlah Dihibahkan:", fontSize = 12.sp, color = Color.Gray)
                                    Text("${detail.jumlah} ${detail.satuan}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Surface(
                            color = Color(0xFFF3E8FF).copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFDDD6FE)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Penerima / Instansi Tujuan:", fontSize = 12.sp, color = Color(0xFF6B21A8), fontWeight = FontWeight.Bold)
                                Text(
                                    text = detail.penerimaHibah,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF581C87)
                                )
                            }
                        }

                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Alasan / Keterangan Hibah:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text(
                                    text = detail.alasanHibah,
                                    fontSize = 13.sp,
                                    color = Color(0xFF334155)
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                            Text("Tanggal: ${detail.tanggalHibah}", fontSize = 11.sp, color = Color.Gray)
                            Text("Petugas: ${detail.petugas}", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { selectedHibahDetail = null },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Tutup", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}
