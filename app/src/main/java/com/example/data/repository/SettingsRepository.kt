package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private val Context.dataStore by preferencesDataStore(name = "developer_branding_prefs")
private val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
private val INSTANSI_NAME_KEY = stringPreferencesKey("instansi_name")
private val OFFICER_NAME_KEY = stringPreferencesKey("officer_name")
private val OFFICER_NIP_KEY = stringPreferencesKey("officer_nip")

class SettingsRepository(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("gudang_settings", Context.MODE_PRIVATE)

    init {
        // Clear old dummy data or empty data so default Merek Alat list is populated
        val currentMerekAlat = prefs.getString("merek_alat", null)
        if (currentMerekAlat == null || currentMerekAlat.isBlank() || currentMerekAlat == "Sony|#|Logitech|#|Canon|#|Epson|#|HP") {
            prefs.edit().remove("merek_alat").apply()
        }
        val currentMerekBahan = prefs.getString("merek_bahan", null)
        if (currentMerekBahan == "Sinar Dunia|#|PaperOne|#|Joyko|#|Kenko|#|Faber-Castell") {
            prefs.edit().remove("merek_bahan").apply()
        }
        val currentRuang = prefs.getString("ruang", null)
        if (currentRuang == "Lab Komputer|#|Ruang Guru|#|Gudang Utama|#|Kelas X-A|#|Kelas XI-B") {
            prefs.edit().remove("ruang").apply()
        }
        val currentSumberDana = prefs.getString("sumber_dana", null)
        if (currentSumberDana == "BOS|#|Dana Komite|#|Bantuan Pemerintah|#|BOP") {
            prefs.edit().remove("sumber_dana").apply()
        }
        val currentKondisi = prefs.getString("kondisi", null)
        if (currentKondisi == "Normal|#|Perbaikan|#|Rusak|#|Expired") {
            prefs.edit().remove("kondisi").apply()
        }
    }

    companion object {
        private const val KEY_SHEETS_URL = "sheets_url"
        private const val KEY_AUTO_SYNC = "auto_sync"
        private const val KEY_LAST_SYNC = "last_sync"
        private const val KEY_DEFAULT_OFFICER = "default_officer"
        private const val KEY_OFFICER_NIP = "officer_nip"
        private const val KEY_APP_THEME = "app_theme"
        private const val KEY_INSTANSI_NAME = "instansi_name"
        private const val KEY_INSTANSI_LOGO_PATH = "instansi_logo_path"
        private const val KEY_USER_PROFILE_PHOTO = "user_profile_photo"
    }

    fun isDemoFinished(): Boolean {
        return prefs.getBoolean("is_demo_finished", false)
    }

    fun setDemoFinished(finished: Boolean) {
        prefs.edit().putBoolean("is_demo_finished", finished).apply()
    }

    fun checkAndInitializeBranding() {
        runBlocking {
            val isFirst = context.dataStore.data.first()[IS_FIRST_LAUNCH] ?: true
            if (isFirst) {
                context.dataStore.edit { preferences ->
                    preferences[IS_FIRST_LAUNCH] = false
                    preferences[INSTANSI_NAME_KEY] = "Pradipta Graha Digital"
                    preferences[OFFICER_NAME_KEY] = "Kevin Ricky Utama, S.Kom."
                    preferences[OFFICER_NIP_KEY] = "199804192025211035"
                }
                setInstansiName("Pradipta Graha Digital")
                setDefaultOfficer("Kevin Ricky Utama, S.Kom.")
                setOfficerNip("199804192025211035")
            }
        }
    }

    fun getInstansiName(): String {
        return prefs.getString(KEY_INSTANSI_NAME, "SMAN 1 Bobotsari") ?: "SMAN 1 Bobotsari"
    }

    fun setInstansiName(name: String) {
        prefs.edit().putString(KEY_INSTANSI_NAME, name).apply()
        runBlocking {
            context.dataStore.edit { preferences ->
                preferences[INSTANSI_NAME_KEY] = name
            }
        }
    }

    fun getInstansiLogoPath(): String {
        return prefs.getString(KEY_INSTANSI_LOGO_PATH, "") ?: ""
    }

    fun setInstansiLogoPath(path: String) {
        prefs.edit().putString(KEY_INSTANSI_LOGO_PATH, path).apply()
    }

    fun getUserProfilePhoto(): String {
        return prefs.getString(KEY_USER_PROFILE_PHOTO, "") ?: ""
    }

    fun setUserProfilePhoto(path: String) {
        prefs.edit().putString(KEY_USER_PROFILE_PHOTO, path).apply()
    }

    fun getUserProfilePhotoForUser(username: String): String {
        if (username.isBlank()) return getUserProfilePhoto()
        val userSpecific = prefs.getString("user_profile_photo_${username.lowercase()}", "") ?: ""
        return userSpecific.ifBlank { getUserProfilePhoto() }
    }

    fun setUserProfilePhotoForUser(username: String, path: String) {
        if (username.isNotBlank()) {
            prefs.edit().putString("user_profile_photo_${username.lowercase()}", path).apply()
        }
        setUserProfilePhoto(path)
    }

    private fun sanitizeItem(text: String): String {
        // Strip leading emojis and symbols
        return text.replace(Regex("^[\\p{So}\\p{Cn}\\u2000-\\u3FFF\\uD800-\\uDBFF\\uDC00-\\uDFFF\\s]+"), "").trim()
    }

    // Helper functions to save/get lists from SharedPreferences
    private fun getList(key: String, defaults: List<String>): List<String> {
        val raw = prefs.getString(key, null)
        if (raw == null) {
            // Save defaults first time
            saveList(key, defaults)
            return defaults
        }
        if (raw.isEmpty()) return emptyList()
        return raw.split("|#|").map { sanitizeItem(it) }.filter { it.isNotBlank() }
    }

    private fun saveList(key: String, list: List<String>) {
        val sanitizedList = list.map { sanitizeItem(it) }.filter { it.isNotBlank() }
        val serialized = sanitizedList.joinToString("|#|")
        prefs.edit().putString(key, serialized).apply()
    }

    fun getMerekAlat(): List<String> {
        val defaults = listOf(
            "BenQ", "Hitachi", "Epson", "Asus", "Acer", "Lenovo", "HP", "Samsung", "SanDisk", "Toshiba", "Seagate", "WD", "MSI"
        )
        return getList("merek_alat", defaults).sorted()
    }

    fun saveMerekAlat(list: List<String>) {
        saveList("merek_alat", list.sorted())
    }

    fun getMerekBahan(): List<String> {
        return getList("merek_bahan", emptyList()).sorted()
    }

    fun saveMerekBahan(list: List<String>) {
        saveList("merek_bahan", list.sorted())
    }

    fun getRuang(): List<String> {
        return getList("ruang", emptyList()).sorted()
    }

    fun saveRuang(list: List<String>) {
        saveList("ruang", list.sorted())
    }

    fun getSumberDana(): List<String> {
        return getList("sumber_dana", emptyList()).sorted()
    }

    fun saveSumberDana(list: List<String>) {
        saveList("sumber_dana", list.sorted())
    }

    fun getKondisi(): List<String> {
        return getList("kondisi", emptyList()).sorted()
    }

    fun saveKondisi(list: List<String>) {
        saveList("kondisi", list.sorted())
    }

    fun getTipeRam(): List<String> {
        val defaults = listOf(
            "LongDIMM DDR3", "LongDIMM DDR4", "LongDIMM DDR5",
            "SoDIMM DDR3", "SoDIMM DDR4", "SoDIMM DDR5"
        )
        return getList("tipe_ram", defaults)
    }

    fun saveTipeRam(list: List<String>) {
        saveList("tipe_ram", list)
    }

    fun getKapasitasRam(): List<String> {
        val defaults = listOf(
            "2 GB", "4 GB", "6 GB", "8 GB", "16 GB"
        )
        return getList("kapasitas_ram", defaults)
    }

    fun saveKapasitasRam(list: List<String>) {
        saveList("kapasitas_ram", list)
    }

    fun getStorage(): List<String> {
        val defaults = listOf(
            "Extr Flashdisk", "Extr HDD", "Extr MicroSD", "Extr SSD",
            "HDD 2.5", "HDD 3.5", "SSD NVMe M.2", "SSD SATA 2.5"
        )
        return getList("storage", defaults)
    }

    fun saveStorage(list: List<String>) {
        saveList("storage", list)
    }

    fun getJenisPc(): List<String> {
        val defaults = listOf(
            "AIO (All-in-One)", "PC Desktop", "Server", "Mini-PC"
        )
        return getList("jenis_pc", defaults)
    }

    fun saveJenisPc(list: List<String>) {
        saveList("jenis_pc", list)
    }

    fun getPeripheral(): List<String> {
        val defaults = listOf(
            "RAM",
            "Internal Storage",
            "External Storage",
            "Mouse & Keyboard",
            "UPS & PSU",
            "Peripheral Lainnya"
        )
        return getList("peripheral", defaults)
    }

    fun savePeripheral(list: List<String>) {
        saveList("peripheral", list)
    }

    fun getGuruMapel(): List<String> {
        return getList("guru_mapel", emptyList()).sorted()
    }

    fun saveGuruMapel(list: List<String>) {
        saveList("guru_mapel", list.sorted())
    }

    fun getStaf(): List<String> {
        return getList("staf", emptyList()).sorted()
    }

    fun saveStaf(list: List<String>) {
        saveList("staf", list.sorted())
    }

    fun getJabatan(): List<String> {
        val defaults = listOf("Siswa", "Guru", "Staf")
        return getList("jabatan", defaults)
    }

    fun saveJabatan(list: List<String>) {
        saveList("jabatan", list)
    }

    fun getSheetsUrl(): String {
        return prefs.getString(KEY_SHEETS_URL, "") ?: ""
    }

    fun setSheetsUrl(url: String) {
        prefs.edit().putString(KEY_SHEETS_URL, url).apply()
    }

    fun isAutoSyncEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_SYNC, true)
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SYNC, enabled).apply()
    }

    fun getLastSyncTime(): String {
        return prefs.getString(KEY_LAST_SYNC, "-") ?: "-"
    }

    fun setLastSyncTime(time: String) {
        prefs.edit().putString(KEY_LAST_SYNC, time).apply()
    }

    fun getLastCloudSyncTime(): String {
        return prefs.getString("last_cloud_sync_time", "Belum Pernah") ?: "Belum Pernah"
    }

    fun setLastCloudSyncTime(time: String) {
        prefs.edit().putString("last_cloud_sync_time", time).apply()
    }

    fun getDefaultOfficer(): String {
        val name = prefs.getString(KEY_DEFAULT_OFFICER, "") ?: ""
        return if (name.isBlank() || name == "Administrator" || name == "Lintang Senja") "Kevin Ricky Utama, S.Kom." else name
    }

    fun setDefaultOfficer(officer: String) {
        prefs.edit().putString(KEY_DEFAULT_OFFICER, officer).apply()
        runBlocking {
            context.dataStore.edit { preferences ->
                preferences[OFFICER_NAME_KEY] = officer
            }
        }
    }

    fun getOfficerNip(): String {
        val nip = prefs.getString(KEY_OFFICER_NIP, "") ?: ""
        return if (nip.isBlank() || nip == "19980419202511035") "199804192025211035" else nip
    }

    fun setOfficerNip(nip: String) {
        prefs.edit().putString(KEY_OFFICER_NIP, nip).apply()
        runBlocking {
            context.dataStore.edit { preferences ->
                preferences[OFFICER_NIP_KEY] = nip
            }
        }
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean("is_logged_in", false)
    }

    fun setLoggedIn(loggedIn: Boolean) {
        prefs.edit().putBoolean("is_logged_in", loggedIn).apply()
    }

    fun getLoggedInUser(): String {
        return prefs.getString("logged_in_user", "") ?: ""
    }

    fun setLoggedInUser(username: String) {
        prefs.edit().putString("logged_in_user", username).apply()
    }

    fun getUserRole(): String {
        return prefs.getString("user_role", "siswa") ?: "siswa"
    }

    fun setUserRole(role: String) {
        prefs.edit().putString("user_role", role).apply()
    }

    fun logout() {
        prefs.edit()
            .putBoolean("is_logged_in", false)
            .putString("logged_in_user", "")
            .putString("user_role", "siswa")
            .apply()
    }

    fun getAppTheme(): String {
        return prefs.getString(KEY_APP_THEME, "auto") ?: "auto"
    }

    fun setAppTheme(theme: String) {
        prefs.edit().putString(KEY_APP_THEME, theme).apply()
    }

    fun getRecentMenus(): List<String> {
        return getList("recent_menus", listOf("Peminjaman", "Alat", "Laporan"))
    }

    fun saveRecentMenus(list: List<String>) {
        saveList("recent_menus", list)
    }

    fun getStudentPermissions(): Map<String, Boolean> {
        val defaults = mapOf(
            "peminjaman" to false,
            "peminjaman_form" to false,
            "peminjaman_riwayat" to false,

            "pengembalian" to false,
            "pengembalian_normal" to false,
            "pengembalian_parsial" to false,

            "qr_group" to false,
            "scan_qr" to false,
            "generate_qr" to false,

            "log_transaksi" to false,
            "log_transaksi_view" to false,
            "log_transaksi_export" to false,
            "log_sirkulasi" to false,
            "log_bahan_habis" to false,
            "log_stok" to false,
            "log_pemeliharaan" to false,
            "log_aktivitas" to false,

            "alat" to false,
            "alat_view" to false,
            "alat_detail" to false,
            "alat_import" to false,
            "alat_export" to false,

            "kondisi_alat" to false,
            "kondisi_alat_catat" to false,
            "kondisi_alat_view" to false,
            "kondisi_alat_report" to false,

            "alat_rusak" to false,
            "alat_rusak_submit" to false,
            "alat_rusak_view" to false,

            "pemeliharaan" to false,
            "pemeliharaan_tambah" to false,
            "pemeliharaan_view" to false,
            "pemeliharaan_servis_luar" to false,
            "pemeliharaan_history" to false,

            "hapus_aset" to false,
            "hapus_aset_alat" to false,
            "hapus_aset_bahan" to false,
            "hapus_aset_peripheral" to false,

            "stok_peripheral" to false,
            "stok_peripheral_view" to false,

            "bahan" to false,
            "bahan_view" to false,
            "bahan_detail" to false,
            "bahan_import" to false,
            "bahan_export" to false,

            "pemakaian_bahan" to false,
            "pemakaian_bahan_form" to false,
            "pemakaian_bahan_log" to false,

            "bahan_afkir" to false,
            "bahan_afkir_submit" to false,
            "bahan_afkir_view" to false,
            "bahan_afkir_report" to false,

            "master_data" to false,
            "master_data_view" to false,
            "master_data_manage" to false,

            "stok_opname" to false,
            "stok_opname_audit" to false,
            "stok_opname_reconcile" to false,

            "laporan" to false,
            "laporan_view" to false,
            "laporan_export" to false,
            "laporan_ringkasan" to false,
            "laporan_alat" to false,
            "laporan_bahan" to false,
            "laporan_afkir" to false,
            "laporan_peminjaman" to false,
            "laporan_pengembalian" to false,
            "laporan_alat_rusak" to false,
            "laporan_pemeliharaan" to false,
            "laporan_export_excel" to false,
            "laporan_print_pdf" to false,

            "peripheral_rusak" to false,
            "peripheral_lapor_rusak" to false,
            "peripheral_list" to false,

            "log_peripheral" to false,

            "labkom" to false,
            "labkom_view" to false,
            "labkom_form" to false,
            "labkom_diagnosa" to false,
            "labkom_manage" to false,

            "mutasi_perangkat" to false,
            "mutasi_perangkat_view" to false,
            "mutasi_perangkat_add" to false,
            "mutasi_perangkat_edit" to false,
            "mutasi_perangkat_delete" to false,

            "log_mutasi" to false,
            "log_mutasi_view" to false,
            "log_mutasi_add" to false,
            "log_mutasi_edit" to false,
            "log_mutasi_delete" to false,

            "laporan_mutasi" to false,
            "laporan_mutasi_view" to false,
            "laporan_mutasi_add" to false,
            "laporan_mutasi_edit" to false,
            "laporan_mutasi_delete" to false,

            "kop_laporan" to false,
            "kop_surat" to false,
            "footer_ttd" to false
        )
        val map = mutableMapOf<String, Boolean>()
        defaults.forEach { (key, defaultVal) ->
            map[key] = prefs.getBoolean("perm_student_$key", defaultVal)
        }
        return map
    }

    fun getRolePermissions(role: String): Map<String, Boolean> {
        val normalizedRole = role.lowercase().replace(" ", "_")
        if (normalizedRole == "siswa") return getStudentPermissions()

        val studentDefaults = getStudentPermissions()
        val defaultValForRole = when (normalizedRole) {
            "admin", "super_admin" -> true
            else -> false
        }

        val map = mutableMapOf<String, Boolean>()
        studentDefaults.keys.forEach { key ->
            map[key] = prefs.getBoolean("perm_${normalizedRole}_$key", defaultValForRole)
        }
        return map
    }

    fun saveRolePermissions(role: String, map: Map<String, Boolean>) {
        val normalizedRole = role.lowercase().replace(" ", "_")
        if (normalizedRole == "siswa") {
            saveStudentPermissions(map)
            return
        }
        val editor = prefs.edit()
        map.forEach { (key, value) ->
            editor.putBoolean("perm_${normalizedRole}_$key", value)
        }
        editor.apply()
    }

    fun saveStudentPermissions(map: Map<String, Boolean>) {
        val editor = prefs.edit()
        map.forEach { (key, value) ->
            editor.putBoolean("perm_student_$key", value)
        }
        editor.apply()
    }

    fun getGeneratedQrCodes(): Set<String> {
        return prefs.getStringSet("generated_qr_codes", emptySet()) ?: emptySet()
    }

    fun markQrCodeGenerated(idBarang: String) {
        val currentSet = prefs.getStringSet("generated_qr_codes", emptySet())?.toMutableSet() ?: mutableSetOf()
        currentSet.add(idBarang)
        prefs.edit().putStringSet("generated_qr_codes", currentSet).apply()
    }

    fun clearAllSettings() {
        prefs.edit().clear().apply()
        runBlocking {
            context.dataStore.edit { preferences ->
                preferences.clear()
            }
        }
    }
}
