package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ItemWithStock
import com.example.ui.components.FilterGroup
import com.example.ui.components.LunarisCard
import com.example.ui.components.LunarisFilterDialog
import com.example.ui.components.LunarisTextField
import com.example.ui.theme.DeepPurpleText
import com.example.ui.viewmodel.InventoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StokOpnameScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = false
    val context = LocalContext.current
    val items by viewModel.itemsWithStock.collectAsState()
    val peripheralStocks by viewModel.allPeripheralStocks.collectAsState()
    val defaultOfficer by viewModel.defaultOfficer.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showQrScanner by remember { mutableStateOf(false) }

    // Filter Kategori: "Semua", "Alat", "Bahan", "Peripheral", "LabKom"
    var selectedCategoryFilter by remember { mutableStateOf("Semua") }
    var showFilterDialog by remember { mutableStateOf(false) }
    var tempCategoryFilter by remember { mutableStateOf(selectedCategoryFilter) }

    // Dialog states
    var selectedItemForDetail by remember { mutableStateOf<ItemWithStock?>(null) }
    var selectedItemForAddStock by remember { mutableStateOf<ItemWithStock?>(null) }
    var selectedItemForEdit by remember { mutableStateOf<ItemWithStock?>(null) }

    // Add stock form inputs
    var jumlahTambahInput by remember { mutableStateOf("") }
    var catatanTambahInput by remember { mutableStateOf("") }
    var petugasTambahInput by remember { mutableStateOf(defaultOfficer) }

    // Edit item form inputs
    var editNamaBarang by remember { mutableStateOf("") }
    var editKategori by remember { mutableStateOf("") }
    var editSatuan by remember { mutableStateOf("") }
    var editStokAwalValue by remember { mutableStateOf("") }
    var editMerekAlat by remember { mutableStateOf("") }
    var editRuang by remember { mutableStateOf("") }
    var editSumberDana by remember { mutableStateOf("") }
    var editKondisi by remember { mutableStateOf("") }
    var editKeterangan by remember { mutableStateOf("") }

    // Mapped Peripheral Items
    val mappedPeripheralItems = remember(peripheralStocks) {
        peripheralStocks.map { p ->
            ItemWithStock(
                idBarang = p.idBarang,
                namaBarang = p.namaItem,
                serialNumber = p.serialNumber,
                stokAwal = p.jumlah,
                stokTersedia = (p.jumlah - p.usedCount).coerceAtLeast(0),
                kategori = p.jenisPeripheral.ifBlank { "Peripheral" },
                satuan = p.satuan.ifBlank { "Unit" },
                stokRusak = 0,
                merekAlat = p.merek,
                ruang = p.lokasiRuang,
                sumberDana = p.sumberDana,
                kondisi = p.kondisi.ifBlank { "Normal" },
                keterangan = p.spesifikasi,
                type = "PERIPHERAL",
                isBorrowable = false
            )
        }
    }

    // Default LabKom Units (Pure empty list, reactive from state)
    val defaultLabKomUnits = remember { emptyList<ItemWithStock>() }

    // Helper to check item categories
    fun isBahan(item: ItemWithStock): Boolean {
        val type = item.type.uppercase()
        val kat = item.kategori.lowercase()
        return type == "BAHAN" || type == "HABIS_PAKAI" || kat.contains("bahan") || kat.contains("habis pakai") || kat.contains("konsumsi")
    }

    fun isPeripheral(item: ItemWithStock): Boolean {
        val type = item.type.uppercase()
        val kat = item.kategori.lowercase()
        val id = item.idBarang.uppercase()
        return type == "PERIPHERAL" || id.startsWith("PRPH-") || kat.contains("peripheral")
    }

    val labKomCategories = remember {
        setOf(
            "PC Desktop", "Workstation Design", "Server Lab / NOC",
            "All-in-One PC", "Laptop LabKom", "Hardware Komputer",
            "Workstation", "Server", "All-in-One", "LabKom", "Komputer"
        )
    }

    fun isLabKom(item: ItemWithStock): Boolean {
        val type = item.type.uppercase()
        val kat = item.kategori.lowercase()
        val id = item.idBarang.uppercase()
        val name = item.namaBarang.lowercase()
        return type == "LABKOM" || type == "KOMPUTER" || id.startsWith("PC-LAB") || id.startsWith("LAB-") ||
                labKomCategories.any { it.equals(item.kategori, ignoreCase = true) } ||
                kat.contains("komputer") || kat.contains("labkom") || kat.contains("workstation") || kat.contains("server") ||
                name.contains("pc lab") || name.contains("workstation") || name.contains("server lab")
    }

    // Combined all items list
    val allItemsCombined = remember(items, mappedPeripheralItems, defaultLabKomUnits) {
        val existingIds = items.map { it.idBarang }.toSet()
        val uniquePeripherals = mappedPeripheralItems.filter { !existingIds.contains(it.idBarang) }
        val currentIds = existingIds + uniquePeripherals.map { it.idBarang }
        val uniqueLabKom = defaultLabKomUnits.filter { !currentIds.contains(it.idBarang) }
        items + uniquePeripherals + uniqueLabKom
    }

    // Counts for filter tabs
    val totalCount = allItemsCombined.size
    val alatCount = allItemsCombined.count { !isBahan(it) && !isPeripheral(it) && !isLabKom(it) }
    val bahanCount = allItemsCombined.count { isBahan(it) && !isPeripheral(it) && !isLabKom(it) }
    val peripheralCount = allItemsCombined.count { isPeripheral(it) }
    val labkomCount = allItemsCombined.count { isLabKom(it) }

    val filteredItems = remember(allItemsCombined, searchQuery, selectedCategoryFilter) {
        allItemsCombined.filter { item ->
            // Category filter
            val matchesCategory = when (selectedCategoryFilter) {
                "Alat" -> !isBahan(item) && !isPeripheral(item) && !isLabKom(item)
                "Bahan" -> isBahan(item) && !isPeripheral(item) && !isLabKom(item)
                "Peripheral" -> isPeripheral(item)
                "LabKom" -> isLabKom(item)
                else -> true
            }
            // Search query filter
            val matchesSearch = searchQuery.isBlank() ||
                    item.namaBarang.contains(searchQuery, ignoreCase = true) ||
                    item.idBarang.contains(searchQuery, ignoreCase = true) ||
                    item.kategori.contains(searchQuery, ignoreCase = true) ||
                    item.ruang.contains(searchQuery, ignoreCase = true) ||
                    item.merekAlat.contains(searchQuery, ignoreCase = true) ||
                    item.serialNumber.contains(searchQuery, ignoreCase = true)

            matchesCategory && matchesSearch
        }
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
                            shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                        )
                        .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                        .background(
                            Brush.horizontalGradient(
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
                                modifier = Modifier.size(40.dp).testTag("back_button")
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
                                    text = "Monitoring Stok Opname",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Rekonsiliasi & verifikasi kondisi serta jumlah fisik aset",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFF1E293B)
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(
                    thickness = 1.2.dp,
                    color = Color.Transparent
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 0.dp)
        ) {
            // Search Bar & Standalone Filter Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LunarisTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Ketik untuk mencari...") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        IconButton(onClick = { showQrScanner = true }) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan QR",
                                tint = Color(0xFF7C3AED)
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
                        .testTag("search_stok")
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .border(
                            width = 1.dp,
                            color = if (selectedCategoryFilter != "Semua") Color(0xFF7C3AED) else Color(0xFFCBD5E1),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .background(
                            color = if (selectedCategoryFilter != "Semua") Color(0xFFF3E8FF) else Color.White,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            tempCategoryFilter = selectedCategoryFilter
                            showFilterDialog = true
                        }
                        .testTag("btn_filter_stok"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter",
                        tint = Color(0xFF7C3AED)
                    )
                }
            }

            if (showFilterDialog) {
                LunarisFilterDialog(
                    onDismissRequest = { showFilterDialog = false },
                    filterGroups = listOf(
                        FilterGroup(
                            title = "Kategori Barang",
                            options = listOf("Semua", "Alat", "Bahan", "Peripheral", "LabKom"),
                            selectedOption = tempCategoryFilter,
                            onOptionSelected = { tempCategoryFilter = it }
                        )
                    ),
                    onReset = {
                        tempCategoryFilter = "Semua"
                    },
                    onApply = {
                        selectedCategoryFilter = tempCategoryFilter
                        showFilterDialog = false
                    }
                )
            }

            // Thin White Card Layout - Rumus Stok & Panduan
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(32.dp)
                            .background(Color(0xFF7C3AED), RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(0xFFEDE9FE), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info Rumus Stok",
                            tint = Color(0xFF7C3AED),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Stok Tersedia = [Stok Awal Fisik] - [Jumlah Aktif Dipinjam/Dipakai]. Klik ikon mata (👁) atau kartu untuk melihat Detail Audit.",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF475569)
                    )
                }
            }

            // 1. FILTER KATEGORI (Alat, Bahan, Peripheral, LabKom)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("category_filter_row")
            ) {
                item {
                    FilterChip(
                        selected = selectedCategoryFilter == "Semua",
                        onClick = { selectedCategoryFilter = "Semua" },
                        label = { Text("Semua ($totalCount)") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        ),
                        modifier = Modifier.testTag("filter_chip_semua")
                    )
                }
                item {
                    FilterChip(
                        selected = selectedCategoryFilter == "Alat",
                        onClick = { selectedCategoryFilter = "Alat" },
                        label = { Text("🛠️ Alat / Aset ($alatCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF2563EB),
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("filter_chip_alat")
                    )
                }
                item {
                    FilterChip(
                        selected = selectedCategoryFilter == "Bahan",
                        onClick = { selectedCategoryFilter = "Bahan" },
                        label = { Text("🧪 Bahan / Habis Pakai ($bahanCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFD97706),
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("filter_chip_bahan")
                    )
                }
                item {
                    FilterChip(
                        selected = selectedCategoryFilter == "Peripheral",
                        onClick = { selectedCategoryFilter = "Peripheral" },
                        label = { Text("🔌 Stok Peripheral ($peripheralCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF7C3AED),
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("filter_chip_peripheral")
                    )
                }
                item {
                    FilterChip(
                        selected = selectedCategoryFilter == "LabKom",
                        onClick = { selectedCategoryFilter = "LabKom" },
                        label = { Text("💻 Komputer (LabKom) ($labkomCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0D9488),
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("filter_chip_labkom")
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

            // List of Items
            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inventory,
                            contentDescription = "Kosong",
                            tint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Hasil pencarian tidak ditemukan." else "Belum ada data barang dalam kategori ini.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 80.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("stok_opname_list")
                ) {
                    items(filteredItems) { item ->
                        val itemIsBahan = isBahan(item)
                        val itemIsPeripheral = isPeripheral(item)
                        val itemIsLabKom = isLabKom(item)

                        val typeBadgeBg = when {
                            itemIsPeripheral -> Color(0xFFF3E8FF)
                            itemIsLabKom -> Color(0xFFCCFBF1)
                            itemIsBahan -> Color(0xFFFFF7ED)
                            else -> Color(0xFFEFF6FF)
                        }
                        val typeBadgeText = when {
                            itemIsPeripheral -> Color(0xFF7E22CE)
                            itemIsLabKom -> Color(0xFF0F766E)
                            itemIsBahan -> Color(0xFFC2410C)
                            else -> Color(0xFF1D4ED8)
                        }
                        val typeLabel = when {
                            itemIsPeripheral -> "STOK PERIPHERAL"
                            itemIsLabKom -> "KOMPUTER (LABKOM)"
                            itemIsBahan -> "BAHAN / HABIS PAKAI"
                            else -> "ALAT / ASET"
                        }

                        LunarisCard(
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (item.stokTersedia in 1..2)
                                    Color(0xFFFFFBEB)
                                else Color(0xFFFFFFFF)
                            ),
                            border = if (item.stokTersedia in 1..2)
                                androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A))
                            else
                                androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    selectedItemForDetail = item
                                }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                // Header: Type Badge & Name
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(typeBadgeBg)
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = typeLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = typeBadgeText,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    if (item.stokTersedia in 1..2) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFFF59E0B))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = "Stok Kritis!",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.namaBarang,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "ID: ${item.idBarang} | Kategori: ${item.kategori.ifEmpty { "-" }} | Satuan: ${item.satuan.ifEmpty { "Pcs" }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                        if (item.ruang.isNotBlank()) {
                                            Text(
                                                text = "📍 Ruang: ${item.ruang}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.secondary,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { selectedItemForDetail = item },
                                        modifier = Modifier.testTag("btn_detail_${item.idBarang}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Visibility,
                                            contentDescription = "View Detail",
                                            tint = Color(0xFF7C3AED)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Stock Indicators Row (Clean pastel metric chips)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Stok Awal Card
                                    LunarisCard(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "Stok Fisik Total",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF64748B)
                                            )
                                            Text(
                                                text = "${item.stokAwal} ${item.satuan.ifEmpty { "Pcs" }}",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1E293B)
                                            )
                                        }
                                    }

                                    // Stok Tersedia Card
                                    val tersediaBg = when {
                                        item.stokTersedia <= 0 -> Color(0xFFFEF2F2)
                                        item.stokTersedia in 1..2 -> Color(0xFFFFFBEB)
                                        else -> Color(0xFFF5F3FF)
                                    }
                                    val tersediaBorder = when {
                                        item.stokTersedia <= 0 -> Color(0xFFFCA5A5)
                                        item.stokTersedia in 1..2 -> Color(0xFFFDE68A)
                                        else -> Color(0xFFDDD6FE)
                                    }
                                    val tersediaText = when {
                                        item.stokTersedia <= 0 -> Color(0xFFDC2626)
                                        item.stokTersedia in 1..2 -> Color(0xFFD97706)
                                        else -> Color(0xFF7C3AED)
                                    }

                                    LunarisCard(
                                        colors = CardDefaults.cardColors(containerColor = tersediaBg),
                                        border = BorderStroke(1.dp, tersediaBorder),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "Stok Tersedia",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = tersediaText
                                            )
                                            Text(
                                                text = "${item.stokTersedia} ${item.satuan.ifEmpty { "Pcs" }}",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = tersediaText
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

        // 2. TAMPILAN KARTU DETAIL (View Detail Dialog)
        if (selectedItemForDetail != null) {
            val item = selectedItemForDetail!!
            val itemIsBahan = isBahan(item)
            val itemIsPeripheral = isPeripheral(item)
            val itemIsLabKom = isLabKom(item)

            val typeBadgeBg = when {
                itemIsPeripheral -> Color(0xFFF3E8FF)
                itemIsLabKom -> Color(0xFFCCFBF1)
                itemIsBahan -> Color(0xFFFFF7ED)
                else -> Color(0xFFEFF6FF)
            }
            val typeBadgeText = when {
                itemIsPeripheral -> Color(0xFF7E22CE)
                itemIsLabKom -> Color(0xFF0F766E)
                itemIsBahan -> Color(0xFFC2410C)
                else -> Color(0xFF1D4ED8)
            }
            val typeLabel = when {
                itemIsPeripheral -> "STOK PERIPHERAL"
                itemIsLabKom -> "KOMPUTER (LABKOM)"
                itemIsBahan -> "BAHAN / HABIS PAKAI"
                else -> "ALAT / ASET"
            }

            AlertDialog(
                onDismissRequest = { selectedItemForDetail = null },
                containerColor = Color.White,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(typeBadgeBg)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = typeLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = typeBadgeText,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.namaBarang,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Kode ID: ${item.idBarang}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        IconButton(onClick = { selectedItemForDetail = null }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup")
                        }
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Stock Overview Section
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LunarisCard(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F3FF)),
                                border = BorderStroke(1.dp, Color(0xFFDDD6FE)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("Stok Fisik Total", style = MaterialTheme.typography.labelSmall, color = Color(0xFF7C3AED), fontWeight = FontWeight.Bold)
                                    Text("${item.stokAwal} ${item.satuan.ifEmpty { "Pcs" }}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                                }
                            }

                            val tersediaBg = if (item.stokTersedia <= 0) Color(0xFFFEF2F2) else Color(0xFFF0FDF4)
                            val tersediaBorder = if (item.stokTersedia <= 0) Color(0xFFFCA5A5) else Color(0xFFBBF7D0)
                            val tersediaText = if (item.stokTersedia <= 0) Color(0xFFDC2626) else Color(0xFF15803D)

                            LunarisCard(
                                colors = CardDefaults.cardColors(containerColor = tersediaBg),
                                border = BorderStroke(1.dp, tersediaBorder),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("Stok Tersedia", style = MaterialTheme.typography.labelSmall, color = tersediaText, fontWeight = FontWeight.Bold)
                                    Text("${item.stokTersedia} ${item.satuan.ifEmpty { "Pcs" }}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = tersediaText)
                                }
                            }
                        }

                        if (item.stokRusak > 0) {
                            LunarisCard(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                                border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("⚠️ Ada ${item.stokRusak} ${item.satuan.ifEmpty { "Pcs" }} kondisi rusak / pemeliharaan.", style = MaterialTheme.typography.bodySmall, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        HorizontalDivider(thickness = 0.8.dp, color = MaterialTheme.colorScheme.outlineVariant)

                        // Item Metadata Grid
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            DetailItemRow(label = "Kategori", value = item.kategori.ifEmpty { "-" })
                            DetailItemRow(label = "Satuan", value = item.satuan.ifEmpty { "Pcs" })
                            DetailItemRow(label = "Merek / Spesifikasi", value = item.merekAlat.ifEmpty { "-" })
                            DetailItemRow(label = "Ruangan / Lokasi", value = item.ruang.ifEmpty { "-" })
                            DetailItemRow(label = "Sumber Dana", value = item.sumberDana.orEmpty().ifEmpty { "-" })
                            DetailItemRow(label = "Kondisi", value = item.kondisi.ifEmpty { "Normal" })
                            if (item.type == "ALAT") {
                                DetailItemRow(label = "Dapat Dipinjam", value = if (item.isBorrowable) "Ya (Bisa Dipinjam)" else "Tidak (Internal Gudang)")
                            }
                            if (item.keterangan.isNotBlank()) {
                                DetailItemRow(label = "Keterangan", value = item.keterangan)
                            }
                        }

                        HorizontalDivider(thickness = 0.8.dp, color = MaterialTheme.colorScheme.outlineVariant)

                        // Riwayat Jejak Penggunaan / Pemeliharaan Sebelumnya (Audit Trail Read-Only)
                        Text(
                            text = "Riwayat Jejak Audit & Pemeliharaan (Read-Only):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

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
                                    Text("• Status Opname Terakhir:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                                    Text("Terverifikasi Audit", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("• Catatan Fisik Aktual:", fontSize = 11.sp, color = Color(0xFF64748B))
                                    Text("${item.stokAwal} ${item.satuan.ifEmpty { "Pcs" }} Sesuai Sistem", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1E293B))
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("• Petugas Penanggung Jawab:", fontSize = 11.sp, color = Color(0xFF64748B))
                                    Text(defaultOfficer.ifEmpty { "Tim Audit Laboratorium" }, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1E293B))
                                }
                                if (item.stokRusak > 0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("• Status Pemeliharaan:", fontSize = 11.sp, color = Color(0xFFDC2626))
                                        Text("${item.stokRusak} ${item.satuan.ifEmpty { "Pcs" }} Dalam Perbaikan/Servis", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { selectedItemForDetail = null }) {
                        Text("Tutup", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // FORMULIR KHUSUS PENAMBAHAN STOK DIALOG
        if (selectedItemForAddStock != null) {
            val item = selectedItemForAddStock!!
            val itemIsBahan = isBahan(item)

            AlertDialog(
                onDismissRequest = { selectedItemForAddStock = null },
                containerColor = Color.White,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Formulir Tambah Stok Gudang", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Banner Informasi Barang
                        LunarisCard(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = item.namaBarang,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF166534)
                                )
                                Text(
                                    text = "Kode ID: ${item.idBarang} | Tipe: ${if (itemIsBahan) "Bahan" else "Alat"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF15803D)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Stok Fisik Saat Ini: ${item.stokAwal} ${item.satuan.ifEmpty { "Pcs" }} (Tersedia: ${item.stokTersedia})",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF166534)
                                )
                            }
                        }

                        // Form Inputs
                        LunarisTextField(
                            value = jumlahTambahInput,
                            onValueChange = { jumlahTambahInput = it },
                            label = { Text("Jumlah Penambahan Stok (+)") },
                            placeholder = { Text("Contoh: 10, 25, 50") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_jumlah_tambah_stok")
                        )

                        LunarisTextField(
                            value = catatanTambahInput,
                            onValueChange = { catatanTambahInput = it },
                            label = { Text("Catatan / Sumber Pengadaan (Opsional)") },
                            placeholder = { Text("Misal: Pembelian Baru APBD 2026") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_catatan_tambah_stok")
                        )

                        LunarisTextField(
                            value = petugasTambahInput,
                            onValueChange = { petugasTambahInput = it },
                            label = { Text("Nama Petugas Penginput") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_petugas_tambah_stok")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val amount = jumlahTambahInput.toIntOrNull()
                            if (amount == null || amount <= 0) {
                                Toast.makeText(context, "Jumlah penambahan stok harus angka positif (>0)!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val pStock = peripheralStocks.find { it.idBarang == item.idBarang }
                            if (pStock != null) {
                                val updated = pStock.copy(
                                    jumlah = pStock.jumlah + amount
                                )
                                viewModel.updatePeripheralStock(
                                    stock = updated,
                                    onSuccess = {
                                        Toast.makeText(context, "Berhasil menambah +$amount ${item.satuan.ifEmpty { "Unit" }} ke stok Peripheral!", Toast.LENGTH_LONG).show()
                                        selectedItemForAddStock = null
                                    },
                                    onError = { err ->
                                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                    }
                                )
                            } else {
                                viewModel.addStock(
                                    idBarang = item.idBarang,
                                    jumlahTambah = amount,
                                    catatan = catatanTambahInput,
                                    namaPetugas = petugasTambahInput,
                                    onSuccess = {
                                        Toast.makeText(
                                            context,
                                            "Berhasil menambah +$amount ${item.satuan.ifEmpty { "Pcs" }} ke stok!",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        selectedItemForAddStock = null
                                    },
                                    onError = { err ->
                                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        modifier = Modifier.testTag("btn_confirm_tambah_stok")
                    ) {
                        Text("Simpan Penambahan Stok", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedItemForAddStock = null }) {
                        Text("Batal")
                    }
                }
            )
        }

        // FORMULIR EDIT DATA & STOK AWAL DIALOG
        if (selectedItemForEdit != null) {
            val item = selectedItemForEdit!!

            AlertDialog(
                onDismissRequest = { selectedItemForEdit = null },
                containerColor = Color.White,
                shape = RoundedCornerShape(20.dp),
                title = { Text("Edit Detail & Stok Awal Barang", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Perbarui metadata atau jumlah stok fisik awal untuk:",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "${item.namaBarang} (ID: ${item.idBarang})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        LunarisTextField(
                            value = editNamaBarang,
                            onValueChange = { editNamaBarang = it },
                            label = { Text("Nama Barang") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LunarisTextField(
                                value = editKategori,
                                onValueChange = { editKategori = it },
                                label = { Text("Kategori") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            LunarisTextField(
                                value = editSatuan,
                                onValueChange = { editSatuan = it },
                                label = { Text("Satuan") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        LunarisTextField(
                            value = editStokAwalValue,
                            onValueChange = { editStokAwalValue = it },
                            label = { Text("Jumlah Stok Fisik Total") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_stok_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LunarisTextField(
                                value = editMerekAlat,
                                onValueChange = { editMerekAlat = it },
                                label = { Text("Merek / Spesifikasi") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            LunarisTextField(
                                value = editRuang,
                                onValueChange = { editRuang = it },
                                label = { Text("Ruangan") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val stokNum = editStokAwalValue.toIntOrNull()
                            if (stokNum == null || stokNum < 0) {
                                Toast.makeText(context, "Jumlah fisik harus berupa angka positif!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (editNamaBarang.isBlank()) {
                                Toast.makeText(context, "Nama barang tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val pStock = peripheralStocks.find { it.idBarang == item.idBarang }
                            if (pStock != null) {
                                val updated = pStock.copy(
                                    namaItem = editNamaBarang.trim(),
                                    jenisPeripheral = editKategori.trim(),
                                    satuan = editSatuan.trim().ifEmpty { "Unit" },
                                    jumlah = stokNum,
                                    merek = editMerekAlat.trim(),
                                    lokasiRuang = editRuang.trim(),
                                    sumberDana = editSumberDana.trim(),
                                    kondisi = editKondisi.trim().ifEmpty { "Normal" },
                                    spesifikasi = editKeterangan.trim()
                                )
                                viewModel.updatePeripheralStock(
                                    stock = updated,
                                    onSuccess = {
                                        Toast.makeText(context, "Data Stok Peripheral berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                                        selectedItemForEdit = null
                                    },
                                    onError = { err ->
                                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                    }
                                )
                            } else {
                                viewModel.updateItemDetails(
                                    idBarang = item.idBarang,
                                    namaBarang = editNamaBarang.trim(),
                                    kategori = editKategori.trim(),
                                    satuan = editSatuan.trim().ifEmpty { "Pcs" },
                                    stokAwal = stokNum,
                                    merekAlat = editMerekAlat.trim(),
                                    ruang = editRuang.trim(),
                                    sumberDana = editSumberDana.trim().ifEmpty { null },
                                    kondisi = editKondisi.trim().ifEmpty { "Normal" },
                                    keterangan = editKeterangan.trim(),
                                    onSuccess = {
                                        Toast.makeText(context, "Data barang berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                                        selectedItemForEdit = null
                                    },
                                    onError = { err ->
                                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        },
                        modifier = Modifier.testTag("btn_confirm_edit_stok")
                    ) {
                        Text("Simpan Perubahan", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedItemForEdit = null }) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}

@Composable
private fun DetailItemRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.tertiary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
