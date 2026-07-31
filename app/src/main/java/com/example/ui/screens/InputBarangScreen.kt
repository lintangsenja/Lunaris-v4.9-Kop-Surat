package com.example.ui.screens
import com.example.ui.components.LunarisCard
import com.example.ui.components.LunarisTextField
import com.example.ui.components.CameraScannerDialog
import com.example.ui.components.ScanMode
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DeepPurpleText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.InventoryViewModel
import java.util.Locale

import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputBarangScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit,
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

    val items by viewModel.itemsWithStock.collectAsState()
    val tipeRamList by viewModel.tipeRam.collectAsState()
    val kapasitasRamList by viewModel.kapasitasRam.collectAsState()
    val storageList by viewModel.storage.collectAsState()
    val jenisPcList by viewModel.jenisPc.collectAsState()

    var useAutoId by remember { mutableStateOf(true) }
    var customIdInput by remember { mutableStateOf("") }
    var duplicateAlertMessage by remember { mutableStateOf<String?>(null) }

    var namaBarang by remember { mutableStateOf("") }
    var stokAwalString by remember { mutableStateOf("1") }

    var jenisPcInput by remember { mutableStateOf("") }
    var tipeRamInput by remember { mutableStateOf("") }
    var kapasitasRamInput by remember { mutableStateOf("") }
    var storageInput by remember { mutableStateOf("") }

    // Dual-Priority Scanner States
    var showCameraScannerDialog by remember { mutableStateOf(false) }
    var initialScanMode by remember { mutableStateOf(ScanMode.PRIMARY_QR) }
    var showNotFoundDialog by remember { mutableStateOf(false) }
    var scannedCodeForNotFound by remember { mutableStateOf("") }
    var scannedModeForNotFound by remember { mutableStateOf(ScanMode.PRIMARY_QR) }

    val handleScannedCode = { rawCode: String, mode: ScanMode ->
        val cleanCode = if (rawCode.contains("\"id\":")) {
            try {
                rawCode.substringAfter("\"id\":\"").substringBefore("\"")
            } catch (e: Exception) { rawCode }
        } else rawCode

        val match = items.find { 
            it.idBarang.equals(cleanCode, ignoreCase = true) ||
            (it.serialNumber.isNotBlank() && it.serialNumber.equals(cleanCode, ignoreCase = true))
        }
        if (match != null) {
            namaBarang = match.namaBarang
            customIdInput = match.idBarang
            useAutoId = false
            Toast.makeText(context, "✨ Auto-Populate Berhasil! Data '${match.namaBarang}' ditarik dari sistem.", Toast.LENGTH_LONG).show()
        } else {
            scannedCodeForNotFound = cleanCode
            scannedModeForNotFound = mode
            showNotFoundDialog = true
        }
    }

    // Estimate next ID for user preview
    val nextId = remember(items) {
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
                                    text = "Registrasi Barang Baru",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = androidx.compose.ui.graphics.Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Pencatatan & pendaftaran unit inventaris laboratorium",
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
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 0.dp)
        ) {
            // Form Card
            LunarisCard(
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFFFFFF)),
                border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f) else Color(0xFFCBD5E1)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Auto ID Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { useAutoId = !useAutoId }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Gunakan ID Otomatis",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Switch(
                            checked = useAutoId,
                            onCheckedChange = { useAutoId = it },
                            modifier = Modifier.testTag("switch_use_auto_id")
                        )
                    }

                    // Scan Auto-Populate Action Banner Button
                    Button(
                        onClick = {
                            initialScanMode = ScanMode.PRIMARY_QR
                            showCameraScannerDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0F766E),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_scan_auto_populate")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pindai QR Lunaris / Barcode (Auto-Populate)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // Kode Barang / ID TextField
                    LunarisTextField(
                        value = if (useAutoId) nextId else customIdInput,
                        onValueChange = { if (!useAutoId) customIdInput = it },
                        label = { Text("Kode Barang / ID") },
                        placeholder = { Text("Misal: BRG-001") },
                        readOnly = useAutoId,
                        trailingIcon = {
                            IconButton(onClick = {
                                initialScanMode = ScanMode.PRIMARY_QR
                                showCameraScannerDialog = true
                            }) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Pindai QR/Barcode",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_barang_id")
                    )

                    // Input Nama Barang
                    LunarisTextField(
                        value = namaBarang,
                        onValueChange = { namaBarang = it },
                        label = { Text("Nama Barang") },
                        placeholder = { Text("Contoh: Proyektor Epson, Rol Kabel 10m") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_nama_barang")
                    )

                    // Hardware Specifications (Optional Dropdowns)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            DynamicDropdownField(
                                label = "Jenis PC (Opsional)",
                                selectedValue = jenisPcInput,
                                options = listOf("") + jenisPcList,
                                onValueChange = { jenisPcInput = it },
                                testTag = "input_barang_jenis_pc"
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            DynamicDropdownField(
                                label = "Storage (Opsional)",
                                selectedValue = storageInput,
                                options = listOf("") + storageList,
                                onValueChange = { storageInput = it },
                                testTag = "input_barang_storage"
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            DynamicDropdownField(
                                label = "Tipe RAM (Opsional)",
                                selectedValue = tipeRamInput,
                                options = listOf("") + tipeRamList,
                                onValueChange = { tipeRamInput = it },
                                testTag = "input_barang_tipe_ram"
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            DynamicDropdownField(
                                label = "Kapasitas RAM (Opsional)",
                                selectedValue = kapasitasRamInput,
                                options = listOf("") + kapasitasRamList,
                                onValueChange = { kapasitasRamInput = it },
                                testTag = "input_barang_kapasitas_ram"
                            )
                        }
                    }

                    // Input Stok Awal
                    LunarisTextField(
                        value = stokAwalString,
                        onValueChange = { stokAwalString = it },
                        label = { Text("Stok Awal Fisik") },
                        placeholder = { Text("Contoh: 10") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_stok_awal")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Submit Button
                    Button(
                        onClick = {
                            val stok = stokAwalString.toIntOrNull()
                            if (namaBarang.isBlank()) {
                                Toast.makeText(context, "Nama barang tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (stok == null || stok < 0) {
                                Toast.makeText(context, "Stok awal harus angka positif!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val specs = listOfNotNull(
                                if (jenisPcInput.isNotBlank()) "PC: $jenisPcInput" else null,
                                if (tipeRamInput.isNotBlank()) "RAM: $tipeRamInput" else null,
                                if (kapasitasRamInput.isNotBlank()) "Kapasitas: $kapasitasRamInput" else null,
                                if (storageInput.isNotBlank()) "Storage: $storageInput" else null
                            ).joinToString(", ")

                            viewModel.registerNewItem(
                                name = namaBarang.trim(),
                                stokAwal = stok,
                                keterangan = if (specs.isNotBlank()) "Spec: $specs" else "",
                                useAutoId = useAutoId,
                                customId = customIdInput,
                                onSuccess = {
                                    Toast.makeText(context, "Barang berhasil terdaftar!", Toast.LENGTH_SHORT).show()
                                    namaBarang = ""
                                    stokAwalString = "1"
                                    jenisPcInput = ""
                                    tipeRamInput = ""
                                    kapasitasRamInput = ""
                                    storageInput = ""
                                    useAutoId = true
                                    customIdInput = ""
                                },
                                onError = { error ->
                                    if (error.startsWith("DUPLICATE_ID:")) {
                                        val dupCode = error.substringAfter("DUPLICATE_ID:")
                                        duplicateAlertMessage = "Oops! 😊 Sepertinya Kode Barang '$dupCode' sudah terdaftar di sistem kita. Mohon gunakan kode yang lain atau aktifkan kembali mode ID Otomatis ya! ✨"
                                    } else {
                                        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                    }
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_simpan_barang")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Daftar")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Daftarkan Barang", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Subtitle List
            Text(
                text = "Daftar Aset Terdaftar (${items.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (items.isEmpty()) {
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
                            tint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Belum ada barang terdaftar.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 80.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("registered_items_list")
                ) {
                    items(items) { item ->
                        LunarisCard(
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFFFFFF)),
                            border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f) else Color(0xFFCBD5E1))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
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
                                        text = "ID: ${item.idBarang}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .background(
                                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                            MaterialTheme.shapes.small
                                        )
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Stok: ${item.stokAwal}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

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

    if (showCameraScannerDialog) {
        CameraScannerDialog(
            title = "Pindai Kode QR / Barcode Barang",
            initialMode = initialScanMode,
            onDismissRequest = { showCameraScannerDialog = false },
            onCodeScannedWithMode = { scannedCode, mode ->
                showCameraScannerDialog = false
                handleScannedCode(scannedCode, mode)
            }
        )
    }

    if (showNotFoundDialog) {
        AlertDialog(
            onDismissRequest = { showNotFoundDialog = false },
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFEAB308),
                        modifier = Modifier.size(24.dp)
                    )
                    Text("Data Tidak Ditemukan", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                val modeLabel = if (scannedModeForNotFound == ScanMode.PRIMARY_QR) "QR Label Lunaris" else "Barcode Pabrik / SN"
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Kode '$scannedCodeForNotFound' ($modeLabel) tidak ditemukan di master inventaris.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Apakah Anda ingin mendaftarkan barang baru dengan ID/Kode ini atau beralih metode pemindaian?",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        customIdInput = scannedCodeForNotFound
                        useAutoId = false
                        showNotFoundDialog = false
                        Toast.makeText(context, "ID '$scannedCodeForNotFound' diisi ke formulir.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Daftarkan Baru / Isi Manual", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            showNotFoundDialog = false
                            initialScanMode = if (scannedModeForNotFound == ScanMode.PRIMARY_QR) ScanMode.FALLBACK_BARCODE else ScanMode.PRIMARY_QR
                            showCameraScannerDialog = true
                        }
                    ) {
                        Text(
                            text = if (scannedModeForNotFound == ScanMode.PRIMARY_QR) "Pindai Barcode Pabrik" else "Pindai QR Lunaris",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    TextButton(onClick = { showNotFoundDialog = false }) {
                        Text("Batal", fontSize = 12.sp)
                    }
                }
            }
        )
    }
}
