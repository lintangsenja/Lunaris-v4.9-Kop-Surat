package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.LunarisCard
import com.example.ui.theme.CarbonBlackText
import com.example.ui.theme.DeepPurpleText
import com.example.ui.theme.pastelGradientBackground
import com.example.ui.viewmodel.InventoryViewModel

data class PermissionSubItemData(
    val key: String,
    val title: String,
    val description: String,
    val defaultVal: Boolean
)

data class PermissionParentItemData(
    val parentKey: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val iconBgColor: Color,
    val iconColor: Color,
    val subItems: List<PermissionSubItemData>
)

data class PermissionGroupData(
    val groupTitle: String,
    val groupSubtitle: String,
    val groupIcon: ImageVector,
    val items: List<PermissionParentItemData>
)

data class UserRoleTabInfo(
    val roleKey: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val badgeColor: Color,
    val textColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleManagementScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val studentPermissions by viewModel.studentPermissions.collectAsState()
    var selectedRole by remember { mutableStateOf("super_admin") }
    var activePermissionsMap by remember(selectedRole, studentPermissions) {
        mutableStateOf(
            if (selectedRole == "siswa") studentPermissions
            else viewModel.getRolePermissions(selectedRole)
        )
    }

    var showResetDialog by remember { mutableStateOf(false) }

    // Map to track expanded state of each parent menu card
    var expandedParents by remember { mutableStateOf(mapOf<String, Boolean>()) }

    val roleTabs = remember {
        listOf(
            UserRoleTabInfo(
                roleKey = "super_admin",
                title = "Superadmin",
                subtitle = "Akses Penuh Seluruh Sistem",
                icon = Icons.Default.VerifiedUser,
                badgeColor = Color(0xFFE0E7FF),
                textColor = Color(0xFF3730A3)
            ),
            UserRoleTabInfo(
                roleKey = "admin",
                title = "Admin",
                subtitle = "Akses Operasional & Kelola",
                icon = Icons.Default.AdminPanelSettings,
                badgeColor = Color(0xFFDCFCE7),
                textColor = Color(0xFF15803D)
            ),
            UserRoleTabInfo(
                roleKey = "siswa",
                title = "Siswa / User",
                subtitle = "Peminjaman & Katalog",
                icon = Icons.Default.School,
                badgeColor = Color(0xFFFFEDD5),
                textColor = Color(0xFFC2410C)
            )
        )
    }

    val permissionGroups = remember {
        listOf(
            PermissionGroupData(
                groupTitle = "1. Sirkulasi & Peminjaman",
                groupSubtitle = "Fitur transaksi keluar masuk, QR code, dan log sirkulasi",
                groupIcon = Icons.Default.CloudSync,
                items = listOf(
                    PermissionParentItemData(
                        parentKey = "peminjaman",
                        title = "Menu Peminjaman Alat",
                        description = "Pengajuan & riwayat transaksi peminjaman alat",
                        icon = Icons.Default.Assignment,
                        iconBgColor = Color(0xFFD1FAE5),
                        iconColor = Color(0xFF059669),
                        subItems = listOf(
                            PermissionSubItemData("peminjaman_form", "Form Ajukan Peminjaman", "Mengisi formulir pengajuan peminjaman alat", false),
                            PermissionSubItemData("peminjaman_riwayat", "Riwayat Peminjaman", "Melihat riwayat & status peminjaman aktif/selesai", false)
                        )
                    ),
                    PermissionParentItemData(
                        parentKey = "pengembalian",
                        title = "Menu Pengembalian Alat",
                        description = "Pengembalian barang terpinjam & pelaporan kondisi",
                        icon = Icons.Default.AssignmentReturn,
                        iconBgColor = Color(0xFFE0E7FF),
                        iconColor = Color(0xFF4F46E5),
                        subItems = listOf(
                            PermissionSubItemData("pengembalian_normal", "Pengembalian Normal", "Proses pengembalian alat dalam kondisi baik", false),
                            PermissionSubItemData("pengembalian_parsial", "Pengembalian Parsial / Rusak", "Proses pengembalian barang rusak / bertahap", false)
                        )
                    ),
                    PermissionParentItemData(
                        parentKey = "qr_group",
                        title = "Grup QR Code",
                        description = "Pemindaian scanner & pembuat kode QR barang",
                        icon = Icons.Default.QrCode,
                        iconBgColor = Color(0xFFFCE7F3),
                        iconColor = Color(0xFFDB2777),
                        subItems = listOf(
                            PermissionSubItemData("scan_qr", "Pindai / Scan QR Code", "Memindai QR barang untuk pencarian & transaksi cepat", false),
                            PermissionSubItemData("generate_qr", "Buat / Generate QR Code", "Membuat dan mencetak label QR barang baru", false)
                        )
                    ),
                    PermissionParentItemData(
                        parentKey = "log_transaksi",
                        title = "Menu Log Transaksi",
                        description = "Catatan rekam jejak & audit seluruh aktivitas sirkulasi",
                        icon = Icons.Default.CloudSync,
                        iconBgColor = Color(0xFFCCFBF1),
                        iconColor = Color(0xFF0D9488),
                        subItems = listOf(
                            PermissionSubItemData("log_sirkulasi", "Sirkulasi Alat", "Catatan transaksi peminjaman & pengembalian alat", false),
                            PermissionSubItemData("log_bahan_habis", "Bahan Habis", "Catatan transaksi pemakaian bahan habis pakai", false),
                            PermissionSubItemData("log_peripheral", "Riwayat Peripheral", "Catatan riwayat & mutasi periferal lab", false),
                            PermissionSubItemData("log_mutasi", "Log Mutasi Perangkat", "Catatan histori mutasi & relokasi unit perangkat", false),
                            PermissionSubItemData("log_stok", "Manajemen Stok", "Catatan perubahan & penyesuaian stok", false),
                            PermissionSubItemData("log_pemeliharaan", "Pemeliharaan", "Catatan jadwal & tindakan pemeliharaan", false),
                            PermissionSubItemData("log_aktivitas", "Aktivitas Sistem", "Log audit aktivitas & pengaksesan sistem", false)
                        )
                    )
                )
            ),
            PermissionGroupData(
                groupTitle = "2. Inventaris Aset & Alat",
                groupSubtitle = "Katalog alat, status kondisi, laporan kerusakan, & pemeliharaan",
                groupIcon = Icons.Default.Build,
                items = listOf(
                    PermissionParentItemData(
                        parentKey = "alat",
                        title = "Menu Alat",
                        description = "Katalog inventaris alat, spesifikasi, import/export data",
                        icon = Icons.Default.Build,
                        iconBgColor = Color(0xFFF3E8FF),
                        iconColor = Color(0xFF7C3AED),
                        subItems = listOf(
                            PermissionSubItemData("alat_view", "Katalog Alat (View)", "Melihat katalog & ketersediaan stok alat", false),
                            PermissionSubItemData("alat_detail", "Detail Spesifikasi (View)", "Melihat detail spesifikasi teknis alat", false),
                            PermissionSubItemData("alat_import", "Import Data (Add)", "Mengimpor data alat dari file Excel/CSV", false),
                            PermissionSubItemData("alat_export", "Export Data (View/Export)", "Mengekspor daftar alat ke berkas Excel", false)
                        )
                    ),
                    PermissionParentItemData(
                        parentKey = "kondisi_alat",
                        title = "Menu Kondisi Alat",
                        description = "Inspeksi kondisi fisik & status kelayakan alat",
                        icon = Icons.Default.Info,
                        iconBgColor = Color(0xFFFFE4E6),
                        iconColor = Color(0xFFE11D48),
                        subItems = listOf(
                            PermissionSubItemData("kondisi_alat_catat", "Pencatatan Kondisi (Add)", "Mencatat hasil pemeriksaan kondisi alat", false),
                            PermissionSubItemData("kondisi_alat_view", "Riwayat Kondisi (View)", "Melihat riwayat status kondisi kelayakan alat", false)
                        )
                    ),
                    PermissionParentItemData(
                        parentKey = "alat_rusak",
                        title = "Alat Rusak",
                        description = "Pengaduan & rekap data kerusakan alat",
                        icon = Icons.Default.Warning,
                        iconBgColor = Color(0xFFFFECEF),
                        iconColor = Color(0xFFEF4444),
                        subItems = listOf(
                            PermissionSubItemData("alat_rusak_submit", "Tambah Alat Rusak (Add)", "Melaporkan / mencatat kejadian alat rusak", false),
                            PermissionSubItemData("alat_rusak_view", "Riwayat Alat Rusak (View)", "Melihat daftar & riwayat alat yang rusak", false)
                        )
                    ),
                    PermissionParentItemData(
                        parentKey = "pemeliharaan",
                        title = "Menu Pemeliharaan",
                        description = "Penjadwalan servis berkala & perawatan aset",
                        icon = Icons.Default.Build,
                        iconBgColor = Color(0xFFEFF6FF),
                        iconColor = Color(0xFF2563EB),
                        subItems = listOf(
                            PermissionSubItemData("pemeliharaan_tambah", "Tambah Pemeliharaan (Add)", "Membuat agenda pemeliharaan / perbaikan alat", false),
                            PermissionSubItemData("pemeliharaan_view", "Riwayat Pemeliharaan (View)", "Melihat jadwal & histori perawatan alat", false),
                            PermissionSubItemData("pemeliharaan_servis_luar", "Servis Luar (Edit)", "Pencatatan perbaikan / servis pihak ketiga", false)
                        )
                    ),
                    PermissionParentItemData(
                        parentKey = "hapus_aset",
                        title = "Hapus Aset",
                        description = "Penghapusan data aset permanen dari inventaris",
                        icon = Icons.Default.Delete,
                        iconBgColor = Color(0xFFFFECEF),
                        iconColor = Color(0xFFDC2626),
                        subItems = listOf(
                            PermissionSubItemData("hapus_aset_alat", "Hapus Aset Alat (Delete)", "Penghapusan data inventaris alat", false),
                            PermissionSubItemData("hapus_aset_bahan", "Hapus Aset Bahan (Delete)", "Penghapusan data inventaris bahan", false),
                            PermissionSubItemData("hapus_aset_peripheral", "Hapus Aset Peripheral (Delete)", "Penghapusan data unit peripheral", false)
                        )
                    )
                )
            ),
            PermissionGroupData(
                groupTitle = "3. Bahan Habis Pakai (BHP)",
                groupSubtitle = "Katalog bahan, log pemakaian praktikum, & bahan afkir",
                groupIcon = Icons.Default.Science,
                items = listOf(
                    PermissionParentItemData(
                        parentKey = "bahan",
                        title = "Menu Bahan",
                        description = "Stok bahan habis pakai praktikum & logistik",
                        icon = Icons.Default.Science,
                        iconBgColor = Color(0xFFE0F2FE),
                        iconColor = Color(0xFF0284C7),
                        subItems = listOf(
                            PermissionSubItemData("bahan_view", "Katalog Bahan (View)", "Melihat stok & daftar bahan habis pakai", false),
                            PermissionSubItemData("bahan_detail", "Detail Spesifikasi (View)", "Melihat rincian lokasi simpan & spesifikasi bahan", false),
                            PermissionSubItemData("bahan_import", "Import Data (Add)", "Mengimpor daftar bahan dari file Excel/CSV", false),
                            PermissionSubItemData("bahan_export", "Export Data (View/Export)", "Mengekspor data stok bahan ke format Excel", false)
                        )
                    ),
                    PermissionParentItemData(
                        parentKey = "pemakaian_bahan",
                        title = "Menu Pemakaian Bahan",
                        description = "Pencatatan konsumsi bahan praktikum",
                        icon = Icons.Default.ShoppingCart,
                        iconBgColor = Color(0xFFFCE7F3),
                        iconColor = Color(0xFFDB2777),
                        subItems = listOf(
                            PermissionSubItemData("pemakaian_bahan_form", "Form Pemakaian (Add)", "Mengisi form pengambilan/pemakaian bahan", false),
                            PermissionSubItemData("pemakaian_bahan_log", "Riwayat Pemakaian (View)", "Melihat log konsumsi pemakaian bahan praktikum", false)
                        )
                    ),
                    PermissionParentItemData(
                        parentKey = "bahan_afkir",
                        title = "Menu Bahan Afkir",
                        description = "Pengelolaan bahan rusak / kadaluwarsa",
                        icon = Icons.Default.DeleteSweep,
                        iconBgColor = Color(0xFFFFEDD5),
                        iconColor = Color(0xFFEA580C),
                        subItems = listOf(
                            PermissionSubItemData("bahan_afkir_submit", "Catat Afkir (Delete/Afkir)", "Pencatatan bahan kedaluwarsa / rusak (afkir)", false),
                            PermissionSubItemData("bahan_afkir_view", "Riwayat Afkir (View)", "Melihat riwayat & daftar bahan afkir", false)
                        )
                    )
                )
            ),
            PermissionGroupData(
                groupTitle = "4. Master Data, Stok Opname & Laporan",
                groupSubtitle = "Pengelolaan data induk, audit fisik, & rekapan laporan",
                groupIcon = Icons.Default.Storage,
                items = listOf(
                    PermissionParentItemData(
                        parentKey = "master_data",
                        title = "Master Data",
                        description = "Data induk barang, kategori, ruang, & sumber dana",
                        icon = Icons.Default.Storage,
                        iconBgColor = Color(0xFFD1FAE5),
                        iconColor = Color(0xFF10B981),
                        subItems = listOf(
                            PermissionSubItemData("master_data_view", "Lihat Induk Barang & Lokasi (View)", "Melihat struktur master data barang & daftar ruang", false),
                            PermissionSubItemData("master_data_manage", "Pengelolaan Data & Kategori (Edit/Add)", "Mengelola kategori, ruang, & parameter sarpras", false)
                        )
                    ),
                    PermissionParentItemData(
                        parentKey = "stok_opname",
                        title = "Stok Opname",
                        description = "Audit fisik & penyesuaian ketersediaan stok gudang",
                        icon = Icons.Default.Inventory,
                        iconBgColor = Color(0xFFEFF6FF),
                        iconColor = Color(0xFF3B82F6),
                        subItems = listOf(
                            PermissionSubItemData("stok_opname_audit", "Audit Physical Count (Add)", "Memasukkan data hasil hitung fisik di lapangan", false),
                            PermissionSubItemData("stok_opname_reconcile", "Penyesuaian Fisik Stok (Edit)", "Menyesuaikan angka stok sistem dengan fisik", false)
                        )
                    ),
                    PermissionParentItemData(
                        parentKey = "laporan",
                        title = "Menu Laporan Terpadu",
                        description = "Ringkasan & laporan terpadu seluruh modul sarpras",
                        icon = Icons.Default.Assessment,
                        iconBgColor = Color(0xFFECFEFF),
                        iconColor = Color(0xFF06B6D4),
                        subItems = listOf(
                            PermissionSubItemData("laporan_ringkasan", "Ringkasan Laporan (View)", "Melihat eksekutif summary & grafik laporan", false),
                            PermissionSubItemData("laporan_alat", "Laporan Alat (View)", "Laporan rekapitulasi data alat", false),
                            PermissionSubItemData("laporan_bahan", "Laporan Bahan (View)", "Laporan rekapitulasi data bahan habis pakai", false),
                            PermissionSubItemData("laporan_mutasi", "Laporan Mutasi Perangkat (View)", "Laporan rekapitulasi mutasi & relokasi unit perangkat", false),
                            PermissionSubItemData("laporan_afkir", "Laporan Afkir (View)", "Laporan rekapitulasi bahan afkir", false),
                            PermissionSubItemData("laporan_peminjaman", "Laporan Peminjaman (View)", "Laporan rekapitulasi peminjaman alat", false),
                            PermissionSubItemData("laporan_pengembalian", "Laporan Pengembalian (View)", "Laporan rekapitulasi pengembalian alat", false),
                            PermissionSubItemData("laporan_alat_rusak", "Laporan Alat Rusak (View)", "Laporan rekapitulasi kerusakan alat", false),
                            PermissionSubItemData("laporan_pemeliharaan", "Laporan Pemeliharaan (View)", "Laporan rekapitulasi pemeliharaan alat", false),
                            PermissionSubItemData("laporan_export_excel", "Export Laporan Excel (Export)", "Mengekspor berkas laporan ke format Excel", false),
                            PermissionSubItemData("laporan_print_pdf", "Cetak / PDF Report (Export)", "Mencetak berkas laporan ke format PDF", false)
                        )
                    )
                )
            ),
            PermissionGroupData(
                groupTitle = "5. Periferal & Laboratorium Komputer (LabKom)",
                groupSubtitle = "Pengelolaan laboratorium komputer, stok periferal, & modul periferal rusak",
                groupIcon = Icons.Default.Computer,
                items = listOf(
                    PermissionParentItemData(
                        parentKey = "stok_peripheral",
                        title = "Stok Peripheral",
                        description = "Manajemen stok & distribusi peripheral komputer lab",
                        icon = Icons.Default.Hardware,
                        iconBgColor = Color(0xFFE0E7FF),
                        iconColor = Color(0xFF4338CA),
                        subItems = listOf(
                            PermissionSubItemData("stok_peripheral_view", "Stok Peripheral (View)", "Melihat ketersediaan stok & komponen peripheral", false)
                        )
                    ),
                    PermissionParentItemData(
                        parentKey = "labkom",
                        title = "Laboratorium",
                        description = "Kelola inventaris, fasilitas, & stok peripheral unit komputer lab",
                        icon = Icons.Default.Computer,
                        iconBgColor = Color(0xFFE0F2FE),
                        iconColor = Color(0xFF0284C7),
                        subItems = listOf(
                            PermissionSubItemData("labkom_view", "Katalog Unit LabKom (View)", "Melihat katalog unit PC, hardware, & spesifikasi lab", false),
                            PermissionSubItemData("labkom_form", "Form Unit Lab (Add)", "Input data unit komputer lab baru", false),
                            PermissionSubItemData("labkom_diagnosa", "Diagnosa Unit PC (Edit)", "Melakukan diagnosa & pemeriksaan unit PC", false),
                            PermissionSubItemData("labkom_manage", "Pengelolaan LabKom (Edit/Add)", "Pencatatan, edit, mutasi unit, & penggunaan unit lab", false)
                        )
                    ),
                    PermissionParentItemData(
                        parentKey = "peripheral_rusak",
                        title = "Menu Peripheral Rusak",
                        description = "Pengaduan, diagnosa, & pemulihan hardware peripheral rusak",
                        icon = Icons.Default.Memory,
                        iconBgColor = Color(0xFFFEF3C7),
                        iconColor = Color(0xFFD97706),
                        subItems = listOf(
                            PermissionSubItemData("peripheral_lapor_rusak", "Lapor Rusak (Add)", "Form pelaporan kerusakan peripheral", false),
                            PermissionSubItemData("peripheral_list", "List Peripheral (View/Edit)", "Melihat daftar & melakukan aksi pemulihan ke stok", false)
                        )
                    )
                )
            ),
            PermissionGroupData(
                groupTitle = "6. Modul Mutasi & Relokasi Perangkat",
                groupSubtitle = "Pengelolaan mutasi perangkat, log mutasi, & laporan mutasi",
                groupIcon = Icons.Default.CompareArrows,
                items = listOf(
                    PermissionParentItemData(
                        parentKey = "mutasi_perangkat",
                        title = "Mutasi Perangkat",
                        description = "Pengelolaan mutasi, relokasi, & perpindahan unit perangkat/periferal antar laboratorium",
                        icon = Icons.Default.CompareArrows,
                        iconBgColor = Color(0xFFCCFBF1),
                        iconColor = Color(0xFF0D9488),
                        subItems = listOf(
                            PermissionSubItemData("mutasi_perangkat_view", "View / Lihat Mutasi Perangkat", "Hak akses melihat daftar, detail, & status relokasi unit", false),
                            PermissionSubItemData("mutasi_perangkat_add", "Add / Tambah Mutasi Perangkat", "Hak akses membuat & mencatat pengajuan mutasi perangkat baru", false),
                            PermissionSubItemData("mutasi_perangkat_edit", "Edit / Perbarui Mutasi Perangkat", "Hak akses mengubah data, alasan, & lokasi tujuan mutasi", false),
                            PermissionSubItemData("mutasi_perangkat_delete", "Delete / Hapus Mutasi Perangkat", "Hak akses menghapus rekam jejak & pembatalan mutasi", false)
                        )
                    ),
                    PermissionParentItemData(
                        parentKey = "log_mutasi",
                        title = "Log Mutasi",
                        description = "Catatan rekam jejak, audit histori, & riwayat mutasi perangkat",
                        icon = Icons.Default.History,
                        iconBgColor = Color(0xFFFEF3C7),
                        iconColor = Color(0xFFD97706),
                        subItems = listOf(
                            PermissionSubItemData("log_mutasi_view", "View / Lihat Log Mutasi", "Hak akses melihat audit histori & log riwayat mutasi", false),
                            PermissionSubItemData("log_mutasi_add", "Add / Catat Log Mutasi", "Hak akses mencatat entri log mutasi & audit trail", false),
                            PermissionSubItemData("log_mutasi_edit", "Edit / Perbarui Log Mutasi", "Hak akses memperbarui keterangan audit log mutasi", false),
                            PermissionSubItemData("log_mutasi_delete", "Delete / Hapus Log Mutasi", "Hak akses mengarsipkan / menghapus catatan log mutasi", false)
                        )
                    ),
                    PermissionParentItemData(
                        parentKey = "laporan_mutasi",
                        title = "Laporan Mutasi",
                        description = "Rekapitulasi, statistik, & ekspor laporan mutasi perangkat",
                        icon = Icons.Default.Assessment,
                        iconBgColor = Color(0xFFE0F2FE),
                        iconColor = Color(0xFF0284C7),
                        subItems = listOf(
                            PermissionSubItemData("laporan_mutasi_view", "View / Lihat Laporan Mutasi", "Hak akses melihat laporan & grafik rekapitulasi mutasi", false),
                            PermissionSubItemData("laporan_mutasi_add", "Add / Export / Cetak Laporan", "Hak akses mengekspor laporan mutasi ke Excel & PDF", false),
                            PermissionSubItemData("laporan_mutasi_edit", "Edit / Filter Laporan Mutasi", "Hak akses mengatur filter & kustomisasi periode laporan", false),
                            PermissionSubItemData("laporan_mutasi_delete", "Delete / Clear Laporan Mutasi", "Hak akses mereset / mengarsipkan rekapitulasi laporan mutasi", false)
                        )
                    )
                )
            ),
            PermissionGroupData(
                groupTitle = "7. Pengaturan Dokumen & Kop Laporan",
                groupSubtitle = "Pengaturan header instansi, logo kop surat, tempat tanggal & tanda tangan (TTD)",
                groupIcon = Icons.Default.Description,
                items = listOf(
                    PermissionParentItemData(
                        parentKey = "kop_laporan",
                        title = "Kop Laporan",
                        description = "Pengaturan header instansi, logo kop surat, tempat tanggal & tanda tangan (TTD)",
                        icon = Icons.Default.Description,
                        iconBgColor = Color(0xFFF3E8FF),
                        iconColor = Color(0xFF7C3AED),
                        subItems = listOf(
                            PermissionSubItemData("kop_surat", "Tab Kop Surat / Header", "Akses konfigurasi header instansi, alamat & logo kop surat", false),
                            PermissionSubItemData("footer_ttd", "Tab Footer & TTD", "Akses konfigurasi tempat/tanggal & penandatangan (TTD)", false)
                        )
                    )
                )
            )
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
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
                            modifier = Modifier.size(40.dp).testTag("btn_back_role_management")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali",
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pengaturan Akses Modul",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Manajemen Hak Akses & Perizinan CRUD Per-Role",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFF1E293B)
                            )
                        }
                        IconButton(
                            onClick = { showResetDialog = true },
                            modifier = Modifier.size(40.dp).testTag("btn_reset_permissions")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset Permission",
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pastelGradientBackground(isDark = false)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Info Header Card
                LunarisCard(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, Color(0xFFFED7AA)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFEDD5))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Akses Security",
                                tint = Color(0xFFEA580C),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pengaturan Hak Akses Peran (Role Control)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepPurpleText
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Pilih peran (Role) di bawah, lalu atur perizinan (View, Add, Edit, Delete) untuk modul Mutasi Perangkat, Log Mutasi, Laporan Mutasi, dan modul sarpras lainnya.",
                                fontSize = 11.sp,
                                color = CarbonBlackText.copy(alpha = 0.75f),
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Role Selection Tab Bar
                Text(
                    text = "Pilih Peran Pengguna (Role):",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepPurpleText,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    roleTabs.forEach { tab ->
                        val isSelected = selectedRole == tab.roleKey
                        val bg = if (isSelected) Color(0xFFEA580C) else Color.White
                        val fg = if (isSelected) Color.White else CarbonBlackText
                        val borderCol = if (isSelected) Color(0xFFC2410C) else Color(0xFFE2E8F0)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(bg)
                                .border(1.dp, borderCol, RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedRole = tab.roleKey
                                    activePermissionsMap = if (tab.roleKey == "siswa") studentPermissions
                                    else viewModel.getRolePermissions(tab.roleKey)
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title,
                                    tint = fg,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = tab.title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = fg
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Active Role Summary Badge
                val currentRoleTab = roleTabs.find { it.roleKey == selectedRole } ?: roleTabs.last()
                LunarisCard(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = currentRoleTab.badgeColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = currentRoleTab.icon,
                                contentDescription = null,
                                tint = currentRoleTab.textColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Mengonfigurasi Role: ${currentRoleTab.title}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = currentRoleTab.textColor
                                )
                                Text(
                                    text = currentRoleTab.subtitle,
                                    fontSize = 10.sp,
                                    color = currentRoleTab.textColor.copy(alpha = 0.8f)
                                )
                            }
                        }

                        val activeCount = activePermissionsMap.count { it.value }
                        val totalCount = activePermissionsMap.size
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Akses: $activeCount / $totalCount",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = currentRoleTab.textColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Permission Groups
                permissionGroups.forEach { group ->
                    Text(
                        text = group.groupTitle,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepPurpleText,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Text(
                        text = group.groupSubtitle,
                        fontSize = 11.sp,
                        color = CarbonBlackText.copy(alpha = 0.65f),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        group.items.forEach { parentItem ->
                            val isExpanded = expandedParents[parentItem.parentKey] ?: false

                            // Compute child active counts for selected role
                            val activeSubCount = parentItem.subItems.count { sub ->
                                activePermissionsMap[sub.key] ?: sub.defaultVal
                            }
                            val totalSubCount = parentItem.subItems.size

                            // Status Parent Logic: Full, Partial, Off
                            val isFullActive = activeSubCount == totalSubCount
                            val isOff = activeSubCount == 0
                            val isPartial = !isFullActive && !isOff

                            val parentBadgeText = when {
                                isFullActive -> "Aktif Penuh"
                                isPartial -> "Parsial ($activeSubCount/$totalSubCount)"
                                else -> "Non-Aktif"
                            }

                            val parentBadgeBgColor = when {
                                isFullActive -> Color(0xFFDCFCE7)
                                isPartial -> Color(0xFFFEF3C7)
                                else -> Color(0xFFF3F4F6)
                            }

                            val parentBadgeTextColor = when {
                                isFullActive -> Color(0xFF15803D)
                                isPartial -> Color(0xFFB45309)
                                else -> Color(0xFF6B7280)
                            }

                            val parentSwitchChecked = activeSubCount > 0

                            LunarisCard(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isPartial) Color(0xFFFCD34D) else Color(0xFFE9D5FF)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp)
                                ) {
                                    // Parent Header Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    expandedParents = expandedParents.toMutableMap().apply {
                                                        put(parentItem.parentKey, !isExpanded)
                                                    }
                                                }
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(parentItem.iconBgColor)
                                            ) {
                                                Icon(
                                                    imageVector = parentItem.icon,
                                                    contentDescription = parentItem.title,
                                                    tint = parentItem.iconColor,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = parentItem.title,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = CarbonBlackText
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(parentBadgeBgColor)
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = parentBadgeText,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = parentBadgeTextColor
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = parentItem.description,
                                                    fontSize = 10.sp,
                                                    color = Color.Gray,
                                                    lineHeight = 13.sp
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // Parent Switch Logic
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Switch(
                                                checked = parentSwitchChecked,
                                                onCheckedChange = { _ ->
                                                    val targetVal = !isFullActive
                                                    val updates = mutableMapOf<String, Boolean>()
                                                    updates[parentItem.parentKey] = targetVal
                                                    parentItem.subItems.forEach { sub ->
                                                        updates[sub.key] = targetVal
                                                    }
                                                    viewModel.updateRolePermissionsBatch(selectedRole, updates)
                                                    activePermissionsMap = activePermissionsMap.toMutableMap().apply {
                                                        putAll(updates)
                                                    }

                                                    val statusMsg = if (targetVal) "diaktifkan penuh" else "dinonaktifkan"
                                                    Toast.makeText(
                                                        context,
                                                        "Grup '${parentItem.title}' $statusMsg untuk role ${currentRoleTab.title}",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = Color.White,
                                                    checkedTrackColor = if (isPartial) Color(0xFFD97706) else Color(0xFF10B981),
                                                    uncheckedThumbColor = Color.White,
                                                    uncheckedTrackColor = Color(0xFFD1D5DB)
                                                ),
                                                modifier = Modifier.testTag("parent_switch_${parentItem.parentKey}")
                                            )

                                            IconButton(
                                                onClick = {
                                                    expandedParents = expandedParents.toMutableMap().apply {
                                                        put(parentItem.parentKey, !isExpanded)
                                                    }
                                                },
                                                modifier = Modifier.size(32.dp).testTag("btn_expand_${parentItem.parentKey}")
                                            ) {
                                                Icon(
                                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                    contentDescription = "Expand Submenu",
                                                    tint = Color.Gray
                                                )
                                            }
                                        }
                                    }

                                    // Expandable Sub-Items List
                                    AnimatedVisibility(
                                        visible = isExpanded,
                                        enter = EnterTransition.None,
                                        exit = ExitTransition.None
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 10.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0xFFFFFFFF))
                                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                                .padding(12.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(bottom = 8.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.AccountTree,
                                                        contentDescription = "Submenu",
                                                        tint = Color(0xFF64748B),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "Rincian Hak Akses & Perizinan:",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF475569)
                                                    )
                                                }

                                                // Quick CRUD Batch Toggles
                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    AssistChip(
                                                        onClick = {
                                                            val updates = mutableMapOf<String, Boolean>()
                                                            parentItem.subItems.forEach { updates[it.key] = true }
                                                            updates[parentItem.parentKey] = true
                                                            viewModel.updateRolePermissionsBatch(selectedRole, updates)
                                                            activePermissionsMap = activePermissionsMap.toMutableMap().apply { putAll(updates) }
                                                        },
                                                        label = { Text("Semua Ya", fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                                                        colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFFDCFCE7), labelColor = Color(0xFF15803D)),
                                                        modifier = Modifier.height(24.dp)
                                                    )
                                                    AssistChip(
                                                        onClick = {
                                                            val updates = mutableMapOf<String, Boolean>()
                                                            parentItem.subItems.forEach { updates[it.key] = false }
                                                            updates[parentItem.parentKey] = false
                                                            viewModel.updateRolePermissionsBatch(selectedRole, updates)
                                                            activePermissionsMap = activePermissionsMap.toMutableMap().apply { putAll(updates) }
                                                        },
                                                        label = { Text("Semua Tidak", fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                                                        colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFFFEE2E2), labelColor = Color(0xFFDC2626)),
                                                        modifier = Modifier.height(24.dp)
                                                    )
                                                }
                                            }

                                            HorizontalDivider(thickness = 0.8.dp, color = Color(0xFFE2E8F0))

                                            parentItem.subItems.forEachIndexed { subIdx, subItem ->
                                                val isSubChecked = activePermissionsMap[subItem.key] ?: subItem.defaultVal

                                                // Determine CRUD action type for badge display
                                                val actionTag = when {
                                                    subItem.key.contains("view") || subItem.key.contains("riwayat") || subItem.key.contains("katalog") || subItem.key.contains("detail") || subItem.key.contains("ringkasan") -> "VIEW"
                                                    subItem.key.contains("add") || subItem.key.contains("form") || subItem.key.contains("submit") || subItem.key.contains("tambah") || subItem.key.contains("catat") || subItem.key.contains("generate") || subItem.key.contains("import") -> "ADD"
                                                    subItem.key.contains("edit") || subItem.key.contains("manage") || subItem.key.contains("diagnosa") || subItem.key.contains("reconcile") -> "EDIT"
                                                    subItem.key.contains("delete") || subItem.key.contains("hapus") || subItem.key.contains("afkir") -> "DELETE"
                                                    else -> "ACTION"
                                                }

                                                val actionBadgeBg = when (actionTag) {
                                                    "VIEW" -> Color(0xFFDBEAFE)
                                                    "ADD" -> Color(0xFFDCFCE7)
                                                    "EDIT" -> Color(0xFFFEF3C7)
                                                    "DELETE" -> Color(0xFFFEE2E2)
                                                    else -> Color(0xFFF3F4F6)
                                                }

                                                val actionBadgeFg = when (actionTag) {
                                                    "VIEW" -> Color(0xFF1D4ED8)
                                                    "ADD" -> Color(0xFF15803D)
                                                    "EDIT" -> Color(0xFFB45309)
                                                    "DELETE" -> Color(0xFFDC2626)
                                                    else -> Color(0xFF4B5563)
                                                }

                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(4.dp))
                                                                    .background(actionBadgeBg)
                                                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                                                            ) {
                                                                Text(
                                                                    text = actionTag,
                                                                    fontSize = 8.sp,
                                                                    fontWeight = FontWeight.ExtraBold,
                                                                    color = actionBadgeFg
                                                                )
                                                            }

                                                            Text(
                                                                text = subItem.title,
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = CarbonBlackText
                                                            )

                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(4.dp))
                                                                    .background(
                                                                        if (isSubChecked) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                                                                    )
                                                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                                                            ) {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                                ) {
                                                                    Icon(
                                                                        imageVector = if (isSubChecked) Icons.Default.CheckCircle else Icons.Default.Lock,
                                                                        contentDescription = null,
                                                                        tint = if (isSubChecked) Color(0xFF15803D) else Color(0xFFDC2626),
                                                                        modifier = Modifier.size(10.dp)
                                                                    )
                                                                    Text(
                                                                        text = if (isSubChecked) "Izinkan" else "Tolak",
                                                                        fontSize = 8.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = if (isSubChecked) Color(0xFF15803D) else Color(0xFFDC2626)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = subItem.description,
                                                            fontSize = 10.sp,
                                                            color = Color.Gray,
                                                            lineHeight = 12.sp
                                                        )
                                                    }

                                                    Switch(
                                                        checked = isSubChecked,
                                                        onCheckedChange = { newSubVal ->
                                                            val updates = mutableMapOf<String, Boolean>()
                                                            updates[subItem.key] = newSubVal

                                                            // Recalculate parent state
                                                            val futureActiveCount = parentItem.subItems.count { sub ->
                                                                if (sub.key == subItem.key) newSubVal
                                                                else (activePermissionsMap[sub.key] ?: sub.defaultVal)
                                                            }
                                                            updates[parentItem.parentKey] = (futureActiveCount > 0)

                                                            viewModel.updateRolePermissionsBatch(selectedRole, updates)
                                                            activePermissionsMap = activePermissionsMap.toMutableMap().apply { putAll(updates) }

                                                            val statusTxt = if (newSubVal) "DIIZINKAN" else "DITOLAK"
                                                            Toast.makeText(
                                                                context,
                                                                "Hak akses '${subItem.title}' $statusTxt untuk role ${currentRoleTab.title}",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        },
                                                        colors = SwitchDefaults.colors(
                                                            checkedThumbColor = Color.White,
                                                            checkedTrackColor = Color(0xFF10B981),
                                                            uncheckedThumbColor = Color.White,
                                                            uncheckedTrackColor = Color(0xFFCBD5E1)
                                                        ),
                                                        modifier = Modifier.scale(0.85f).testTag("sub_switch_${subItem.key}")
                                                    )
                                                }

                                                if (subIdx < parentItem.subItems.size - 1) {
                                                    HorizontalDivider(
                                                        thickness = 0.5.dp,
                                                        color = Color(0xFFE2E8F0)
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

                // Reset Button
                OutlinedButton(
                    onClick = { showResetDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFEA580C)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFEA580C)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_reset_defaults_bottom")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Reset Akses Role ${currentRoleTab.title} ke Default",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    if (showResetDialog) {
        val currentRoleTab = roleTabs.find { it.roleKey == selectedRole } ?: roleTabs.last()
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = Color(0xFFEA580C)
                )
            },
            title = {
                Text(
                    text = "Reset Akses Role ${currentRoleTab.title}?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "Pengaturan hak akses perizinan untuk role '${currentRoleTab.title}' (termasuk Mutasi Perangkat, Log Mutasi, Laporan Mutasi, dan modul lainnya) akan dikembalikan ke konfigurasi standar.",
                    fontSize = 13.sp,
                    color = CarbonBlackText.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedRole == "siswa") {
                            viewModel.resetStudentPermissionsToDefault()
                            activePermissionsMap = studentPermissions
                        } else {
                            val resetDefaults = viewModel.getRolePermissions(selectedRole)
                            viewModel.updateRolePermissionsBatch(selectedRole, resetDefaults)
                            activePermissionsMap = resetDefaults
                        }
                        showResetDialog = false
                        Toast.makeText(context, "Hak akses ${currentRoleTab.title} berhasil di-reset ke default!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C))
                ) {
                    Text("Ya, Reset", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
