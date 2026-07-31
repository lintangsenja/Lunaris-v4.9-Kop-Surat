package com.example.data.repository

import android.util.Log
import com.example.data.dao.InventoryDao
import com.example.data.entity.ItemEntity
import com.example.data.entity.LoanItemEntity
import com.example.data.entity.LoanTransactionEntity
import com.example.data.entity.CategoryEntity
import com.example.data.entity.UnitEntity
import com.example.data.entity.PeripheralEntity
import com.example.data.entity.PeripheralRusakEntity
import com.example.data.entity.PemakaianBahanEntity
import com.example.data.entity.BahanAfkirEntity
import com.example.data.entity.MutasiPerangkatEntity
import com.example.data.entity.DamagedItemEntity
import com.example.data.entity.PeripheralStockEntity
import com.example.data.entity.UserEntity
import com.example.data.entity.KopLaporanEntity
import com.example.data.entity.RecentKopEntity
import com.example.data.model.ItemWithStock
import com.example.data.network.GoogleSheetsSyncService
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TransactionDetailResult(
    val transaction: LoanTransactionEntity,
    val items: List<LoanItemEntity>,
    val returnStatusDisplay: String,
    val isReturned: Boolean
)

/**
 * Cloud-First InventoryRepository interacting directly with FirebaseFirestore.
 * Bypasses local Room/SQLite persistence and streams data directly from cloud Firestore collections.
 */
class InventoryRepository(
    private val inventoryDao: InventoryDao? = null,
    private val syncService: GoogleSheetsSyncService = GoogleSheetsSyncService()
) {

    private val firestore: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    companion object {
        const val PATH_ITEMS = "items"
        const val PATH_SETTINGS = "settings"
        const val PATH_PERMISSIONS = "permissions"
        const val PATH_USERS = "users"
        const val PATH_TRANSACTIONS = "transactions"
        const val PATH_LOAN_ITEMS = "loan_items"
        const val PATH_CATEGORIES = "categories"
        const val PATH_UNITS = "units"
        const val PATH_PERIPHERALS = "peripherals"
        const val PATH_DAMAGED_ITEMS = "damaged_items"
        const val PATH_PEMAKAIAN_BAHAN = "pemakaian_bahan"
        const val PATH_BAHAN_AFKIR = "bahan_afkir"
        const val PATH_PERIPHERAL_STOCKS = "peripheral_stocks"
        const val PATH_PERIPHERAL_RUSAK = "peripheral_rusak"
        const val PATH_MUTASI_PERANGKAT = "mutasi_perangkat"
    }

    // Users Stream directly from Cloud Firestore ("users")
    val allUsers: Flow<List<UserEntity>> = callbackFlow {
        val listener = firestore.collection(PATH_USERS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("InventoryRepo", "Error listening to users from Firestore", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        val username = doc.id.ifBlank { doc.getString("username") ?: "" }
                        if (username.isBlank()) null else {
                            val role = doc.getString("role") ?: "siswa"
                            val fullName = doc.getString("fullName") ?: doc.getString("full_name") ?: username
                            val password = doc.getString("password") ?: ""
                            val photoUrl = doc.getString("photoUrl") ?: doc.getString("photo_url") ?: ""
                            UserEntity(
                                username = username,
                                password = password,
                                role = role,
                                fullName = fullName,
                                photoUrl = photoUrl
                            )
                        }
                    }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    // 1. Items Stream directly from Cloud Firestore ("items")
    val itemsWithStock: Flow<List<ItemWithStock>> = callbackFlow {
        val listener = firestore.collection(PATH_ITEMS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("InventoryRepo", "Error listening to items from Firestore", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        val idBarang = doc.id.ifBlank { doc.getString("idBarang") ?: doc.getString("id") ?: "" }
                        val namaBarang = doc.getString("namaBarang") ?: doc.getString("name") ?: doc.getString("nama") ?: ""
                        if (namaBarang.isBlank()) null else {
                            val stokAwal = doc.getLong("stokAwal")?.toInt() ?: doc.getLong("stock")?.toInt() ?: doc.getLong("stok")?.toInt() ?: 0
                            val kategori = doc.getString("kategori") ?: doc.getString("category") ?: ""
                            val satuan = doc.getString("satuan") ?: doc.getString("unit") ?: ""
                            val stokRusak = doc.getLong("stokRusak")?.toInt() ?: 0
                            val pinjamCount = doc.getLong("stokDipinjam")?.toInt() ?: doc.getLong("pinjamCount")?.toInt() ?: 0
                            val stokTersedia = (stokAwal - stokRusak - pinjamCount).coerceAtLeast(0)
                            val merekAlat = doc.getString("merekAlat") ?: doc.getString("merek") ?: ""
                            val ruang = doc.getString("ruang") ?: doc.getString("room") ?: ""
                            val sumberDana = doc.getString("sumberDana") ?: doc.getString("sumber_dana")
                            val kondisi = doc.getString("kondisi") ?: doc.getString("condition") ?: ""
                            val keterangan = doc.getString("keterangan") ?: doc.getString("description") ?: ""
                            val isDemo = doc.getBoolean("isDemo") ?: false
                            val type = doc.getString("type") ?: doc.getString("tipe") ?: "ALAT"
                            val isBorrowable = doc.getBoolean("isBorrowable") ?: doc.getBoolean("isAvailableForStudent") ?: true
                            val serialNumber = doc.getString("serialNumber") ?: ""

                            ItemWithStock(
                                idBarang = idBarang,
                                namaBarang = namaBarang,
                                serialNumber = serialNumber,
                                stokAwal = stokAwal,
                                stokTersedia = stokTersedia,
                                kategori = kategori,
                                satuan = satuan,
                                stokRusak = stokRusak,
                                merekAlat = merekAlat,
                                ruang = ruang,
                                sumberDana = sumberDana,
                                kondisi = kondisi,
                                keterangan = keterangan,
                                isDemo = isDemo,
                                type = type,
                                isBorrowable = isBorrowable
                            )
                        }
                    }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    // 2. Transactions Stream directly from Cloud Firestore ("transactions")
    val allTransactions: Flow<List<LoanTransactionEntity>> = callbackFlow {
        val listener = firestore.collection(PATH_TRANSACTIONS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("InventoryRepo", "Error listening to transactions from Firestore", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        val idTx = doc.id
                        val tanggal = doc.getString("tanggal") ?: return@mapNotNull null
                        val namaPeminjam = doc.getString("namaPeminjam") ?: return@mapNotNull null
                        LoanTransactionEntity(
                            idTransaksi = idTx,
                            tanggal = tanggal,
                            namaPeminjam = namaPeminjam,
                            kelas = doc.getString("kelas") ?: "",
                            waktu = doc.getString("waktu") ?: "",
                            kondisi = doc.getString("kondisi") ?: "",
                            namaPetugas = doc.getString("namaPetugas") ?: "",
                            status = doc.getString("status") ?: "Dipinjam",
                            tanggalKembali = doc.getString("tanggalKembali"),
                            waktuKembali = doc.getString("waktuKembali"),
                            kondisiKembali = doc.getString("kondisiKembali"),
                            petugasKembali = doc.getString("petugasKembali"),
                            keteranganKerusakan = doc.getString("keteranganKerusakan"),
                            whatsappNumber = doc.getString("whatsappNumber"),
                            durasiHari = doc.getLong("durasiHari")?.toInt() ?: 1,
                            isDemo = doc.getBoolean("isDemo") ?: false,
                            tujuanPeminjaman = doc.getString("tujuanPeminjaman"),
                            detailTujuan = doc.getString("detailTujuan")
                        )
                    }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    val activeTransactions: Flow<List<LoanTransactionEntity>> = allTransactions.map { list ->
        list.filter { it.status != "Kembali" }
    }

    // 3. Damaged Items Stream directly from Cloud Firestore ("damaged_items")
    val allDamagedItems: Flow<List<DamagedItemEntity>> = callbackFlow {
        val listener = firestore.collection(PATH_DAMAGED_ITEMS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("InventoryRepo", "Error listening to damaged items from Firestore", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        val id = doc.getLong("id")?.toInt() ?: doc.id.hashCode()
                        val idBarang = doc.getString("idBarang") ?: ""
                        val namaBarang = doc.getString("namaBarang") ?: ""
                        DamagedItemEntity(
                            id = id,
                            idBarang = idBarang,
                            namaBarang = namaBarang,
                            jumlah = doc.getLong("jumlah")?.toInt() ?: 1,
                            tanggalKerusakan = doc.getString("tanggalKerusakan") ?: "",
                            waktuKerusakan = doc.getString("waktuKerusakan") ?: "",
                            keteranganKerusakan = doc.getString("keteranganKerusakan") ?: "",
                            namaPetugas = doc.getString("namaPetugas") ?: "",
                            kondisiBaru = doc.getString("kondisiBaru") ?: "Rusak",
                            status = doc.getString("status") ?: "Rusak (Perlu Tindakan)",
                            statusKeterangan = doc.getString("statusKeterangan") ?: "",
                            isDemo = doc.getBoolean("isDemo") ?: false,
                            validationCount = doc.getLong("validationCount")?.toInt() ?: 0,
                            lastValidatedDate = doc.getString("lastValidatedDate") ?: "",
                            lastValidatedBy = doc.getString("lastValidatedBy") ?: "",
                            validationNotes = doc.getString("validationNotes") ?: "",
                            isHibah = doc.getBoolean("isHibah") ?: false,
                            penerimaHibah = doc.getString("penerimaHibah") ?: "",
                            alasanHibah = doc.getString("alasanHibah") ?: ""
                        )
                    }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    // 4. Pemakaian Bahan Stream
    val allPemakaianBahan: Flow<List<PemakaianBahanEntity>> = callbackFlow {
        val listener = firestore.collection(PATH_PEMAKAIAN_BAHAN)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        val id = doc.id
                        val idBarang = doc.getString("idBarang") ?: ""
                        val namaBarang = doc.getString("namaBarang") ?: doc.getString("namaBahan") ?: ""
                        PemakaianBahanEntity(
                            idPemakaian = id,
                            idBarang = idBarang,
                            namaBarang = namaBarang,
                            jumlahDiambil = doc.getLong("jumlahDiambil")?.toInt() ?: doc.getLong("jumlah")?.toInt() ?: 1,
                            satuan = doc.getString("satuan") ?: "",
                            namaPeminta = doc.getString("namaPeminta") ?: doc.getString("namaPengguna") ?: "",
                            jabatan = doc.getString("jabatan") ?: "",
                            kelas = doc.getString("kelas"),
                            namaPetugas = doc.getString("namaPetugas") ?: "",
                            tanggalPemakaian = doc.getString("tanggalPemakaian") ?: doc.getString("tanggal") ?: "",
                            keterangan = doc.getString("keterangan") ?: "",
                            isDemo = doc.getBoolean("isDemo") ?: false
                        )
                    }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    // 5. Bahan Afkir Stream
    val allBahanAfkir: Flow<List<BahanAfkirEntity>> = callbackFlow {
        val listener = firestore.collection(PATH_BAHAN_AFKIR)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        val id = doc.id
                        val idBarang = doc.getString("idBarang") ?: ""
                        val namaBarang = doc.getString("namaBarang") ?: doc.getString("namaBahan") ?: ""
                        BahanAfkirEntity(
                            idAfkir = id,
                            idBarang = idBarang,
                            namaBarang = namaBarang,
                            jumlahAfkir = doc.getLong("jumlahAfkir")?.toInt() ?: doc.getLong("jumlah")?.toInt() ?: 1,
                            satuan = doc.getString("satuan") ?: "",
                            alasan = doc.getString("alasan") ?: "",
                            tanggalAfkir = doc.getString("tanggalAfkir") ?: doc.getString("tanggal") ?: "",
                            status = doc.getString("status") ?: "Aktif",
                            isDemo = doc.getBoolean("isDemo") ?: false
                        )
                    }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    // 6. Categories Stream
    val allCategories: Flow<List<CategoryEntity>> = callbackFlow {
        val listener = firestore.collection(PATH_CATEGORIES)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        val name = doc.getString("name") ?: doc.id
                        if (name.isBlank()) null else CategoryEntity(id = doc.id.hashCode(), name = name)
                    }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    // 7. Units Stream
    val allUnits: Flow<List<UnitEntity>> = callbackFlow {
        val listener = firestore.collection(PATH_UNITS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        val name = doc.getString("name") ?: doc.id
                        if (name.isBlank()) null else UnitEntity(id = doc.id.hashCode(), name = name)
                    }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    // 8. Peripherals Stream
    val allPeripherals: Flow<List<PeripheralEntity>> = callbackFlow {
        val listener = firestore.collection(PATH_PERIPHERALS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        val name = doc.getString("name") ?: doc.id
                        if (name.isBlank()) null else PeripheralEntity(id = doc.id.hashCode(), name = name)
                    }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    // 9. Peripheral Rusak Stream
    val allPeripheralRusak: Flow<List<PeripheralRusakEntity>> = callbackFlow {
        val listener = firestore.collection(PATH_PERIPHERAL_RUSAK)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        val id = doc.getLong("id")?.toInt() ?: doc.id.hashCode()
                        PeripheralRusakEntity(
                            id = id,
                            idBarang = doc.getString("idBarang") ?: "",
                            namaBarang = doc.getString("namaBarang") ?: "",
                            subKategori = doc.getString("subKategori") ?: "",
                            jumlah = doc.getLong("jumlah")?.toInt() ?: 1,
                            tanggalKerusakan = doc.getString("tanggalKerusakan") ?: "",
                            waktuKerusakan = doc.getString("waktuKerusakan") ?: "",
                            keteranganKerusakan = doc.getString("keteranganKerusakan") ?: "",
                            statusDiagnosa = doc.getString("statusDiagnosa") ?: "Rusak Ringan",
                            status = doc.getString("status") ?: "Proses Diagnosa",
                            namaPetugas = doc.getString("namaPetugas") ?: ""
                        )
                    }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    // 10. Peripheral Stocks Stream
    val allPeripheralStocks: Flow<List<PeripheralStockEntity>> = callbackFlow {
        val listener = firestore.collection(PATH_PERIPHERAL_STOCKS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        val idBarang = doc.getString("idBarang") ?: doc.id
                        PeripheralStockEntity(
                            idBarang = idBarang,
                            namaItem = doc.getString("namaItem") ?: "",
                            jenisPeripheral = doc.getString("jenisPeripheral") ?: "",
                            merek = doc.getString("merek") ?: "",
                            jumlah = doc.getLong("jumlah")?.toInt() ?: 0,
                            kondisi = doc.getString("kondisi") ?: "Baik",
                            lokasiRuang = doc.getString("lokasiRuang") ?: "",
                            sumberDana = doc.getString("sumberDana") ?: "",
                            satuan = doc.getString("satuan") ?: "Unit",
                            tanggalMasuk = doc.getString("tanggalMasuk") ?: ""
                        )
                    }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    // 11. Mutasi Perangkat Stream
    val allMutasiPerangkat: Flow<List<MutasiPerangkatEntity>> = callbackFlow {
        val listener = firestore.collection(PATH_MUTASI_PERANGKAT)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        val idMutasi = doc.id
                        MutasiPerangkatEntity(
                            id = doc.getLong("id")?.toInt() ?: doc.id.hashCode(),
                            idMutasi = idMutasi,
                            idBarang = doc.getString("idBarang") ?: "",
                            namaBarang = doc.getString("namaBarang") ?: "",
                            serialNumber = doc.getString("serialNumber") ?: "",
                            jenisPerangkat = doc.getString("jenisPerangkat") ?: "PERIPHERAL",
                            ruangAsal = doc.getString("ruangAsal") ?: doc.getString("asalRuang") ?: "",
                            ruangTujuan = doc.getString("ruangTujuan") ?: doc.getString("tujuanRuang") ?: "",
                            tanggalMutasi = doc.getString("tanggalMutasi") ?: "",
                            namaPetugas = doc.getString("namaPetugas") ?: "",
                            alasanMutasi = doc.getString("alasanMutasi") ?: "",
                            keterangan = doc.getString("keterangan") ?: "",
                            isDemo = doc.getBoolean("isDemo") ?: false
                        )
                    }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    // CRUD Operations calling Firestore Directly

    suspend fun insertItem(
        id: String,
        name: String,
        serialNumber: String = "",
        stokAwal: Int,
        kategori: String = "",
        satuan: String = "",
        merekAlat: String = "",
        ruang: String = "",
        sumberDana: String? = null,
        kondisi: String = "",
        keterangan: String = "",
        type: String = "ALAT",
        isBorrowable: Boolean = true
    ) {
        val itemMap = hashMapOf<String, Any?>(
            "idBarang" to id,
            "id" to id,
            "namaBarang" to name,
            "name" to name,
            "serialNumber" to serialNumber,
            "stokAwal" to stokAwal,
            "stock" to stokAwal,
            "kategori" to kategori,
            "category" to kategori,
            "satuan" to satuan,
            "unit" to satuan,
            "merekAlat" to merekAlat,
            "merek" to merekAlat,
            "ruang" to ruang,
            "sumberDana" to sumberDana,
            "kondisi" to kondisi,
            "keterangan" to keterangan,
            "type" to type,
            "isBorrowable" to isBorrowable,
            "stokRusak" to 0
        )
        firestore.collection(PATH_ITEMS).document(id).set(itemMap, SetOptions.merge()).await()
    }

    suspend fun updateItem(item: ItemEntity) {
        val itemMap = hashMapOf<String, Any?>(
            "idBarang" to item.idBarang,
            "id" to item.idBarang,
            "namaBarang" to item.namaBarang,
            "name" to item.namaBarang,
            "serialNumber" to item.serialNumber,
            "stokAwal" to item.stokAwal,
            "stock" to item.stokAwal,
            "kategori" to item.kategori,
            "category" to item.kategori,
            "satuan" to item.satuan,
            "unit" to item.satuan,
            "stokRusak" to item.stokRusak,
            "merekAlat" to item.merekAlat,
            "merek" to item.merekAlat,
            "ruang" to item.ruang,
            "sumberDana" to item.sumberDana,
            "kondisi" to item.kondisi,
            "keterangan" to item.keterangan,
            "type" to item.type,
            "isBorrowable" to item.isBorrowable
        )
        firestore.collection(PATH_ITEMS).document(item.idBarang).set(itemMap, SetOptions.merge()).await()
    }

    suspend fun deleteItemById(idBarang: String) {
        firestore.collection(PATH_ITEMS).document(idBarang).delete().await()
    }

    suspend fun getActiveLoanCountForItem(idBarang: String): Int {
        return try {
            val snapshot = firestore.collection(PATH_LOAN_ITEMS)
                .whereEqualTo("idBarang", idBarang)
                .get()
                .await()
            snapshot.documents.sumOf { doc -> doc.getLong("jumlah")?.toInt() ?: 1 }
        } catch (e: Exception) {
            0
        }
    }

    suspend fun repairStokRusak(idBarang: String, jumlahDiperbaiki: Int) {
        try {
            val doc = firestore.collection(PATH_ITEMS).document(idBarang).get().await()
            if (doc.exists()) {
                val currentRusak = doc.getLong("stokRusak")?.toInt() ?: 0
                val updatedRusak = (currentRusak - jumlahDiperbaiki).coerceAtLeast(0)
                firestore.collection(PATH_ITEMS).document(idBarang)
                    .set(mapOf("stokRusak" to updatedRusak), SetOptions.merge()).await()
            }
        } catch (e: Exception) {
            Log.e("InventoryRepo", "Error repairing stok rusak in Firestore", e)
        }
    }

    // Categories
    suspend fun insertCategory(name: String) {
        val docId = name.trim().lowercase().replace(" ", "_")
        firestore.collection(PATH_CATEGORIES).document(docId)
            .set(mapOf("name" to name.trim()), SetOptions.merge()).await()
    }

    suspend fun updateCategory(category: CategoryEntity) {
        val docId = category.name.trim().lowercase().replace(" ", "_")
        firestore.collection(PATH_CATEGORIES).document(docId)
            .set(mapOf("id" to category.id, "name" to category.name), SetOptions.merge()).await()
    }

    suspend fun deleteCategoryById(id: Int) {
        firestore.collection(PATH_CATEGORIES).document(id.toString()).delete().await()
    }

    // Units
    suspend fun insertUnit(name: String) {
        val docId = name.trim().lowercase().replace(" ", "_")
        firestore.collection(PATH_UNITS).document(docId)
            .set(mapOf("name" to name.trim()), SetOptions.merge()).await()
    }

    suspend fun updateUnit(unit: UnitEntity) {
        val docId = unit.name.trim().lowercase().replace(" ", "_")
        firestore.collection(PATH_UNITS).document(docId)
            .set(mapOf("id" to unit.id, "name" to unit.name), SetOptions.merge()).await()
    }

    suspend fun deleteUnitById(id: Int) {
        firestore.collection(PATH_UNITS).document(id.toString()).delete().await()
    }

    suspend fun cleanupDuplicateUnits(): Int {
        return try {
            val snapshot = firestore.collection(PATH_UNITS).get().await()
            val seen = mutableSetOf<String>()
            var removedCount = 0
            for (doc in snapshot.documents) {
                val name = doc.getString("name") ?: doc.id
                val normalized = name.trim().lowercase()
                if (normalized.isBlank() || seen.contains(normalized)) {
                    doc.reference.delete()
                    removedCount++
                } else {
                    seen.add(normalized)
                }
            }
            removedCount
        } catch (e: Exception) {
            0
        }
    }

    // Peripherals
    suspend fun insertPeripheral(name: String) {
        val docId = name.trim().lowercase().replace(" ", "_")
        firestore.collection(PATH_PERIPHERALS).document(docId)
            .set(mapOf("name" to name.trim()), SetOptions.merge()).await()
    }

    suspend fun updatePeripheral(peripheral: PeripheralEntity) {
        val docId = peripheral.name.trim().lowercase().replace(" ", "_")
        firestore.collection(PATH_PERIPHERALS).document(docId)
            .set(mapOf("id" to peripheral.id, "name" to peripheral.name), SetOptions.merge()).await()
    }

    suspend fun deletePeripheralById(id: Int) {
        firestore.collection(PATH_PERIPHERALS).document(id.toString()).delete().await()
    }

    suspend fun getItemsForTransaction(idTransaksi: String): List<LoanItemEntity> {
        return try {
            val snapshot = firestore.collection(PATH_LOAN_ITEMS)
                .whereEqualTo("idTransaksi", idTransaksi)
                .get().await()
            snapshot.documents.mapNotNull { doc ->
                LoanItemEntity(
                    id = doc.getLong("id")?.toInt() ?: doc.id.hashCode(),
                    idTransaksi = doc.getString("idTransaksi") ?: idTransaksi,
                    idBarang = doc.getString("idBarang") ?: "",
                    namaBarang = doc.getString("namaBarang") ?: "",
                    jumlah = doc.getLong("jumlah")?.toInt() ?: 1,
                    isDemo = doc.getBoolean("isDemo") ?: false
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getTransactionDetail(idTransaksi: String): TransactionDetailResult? {
        return try {
            val doc = firestore.collection(PATH_TRANSACTIONS).document(idTransaksi).get().await()
            if (!doc.exists()) return null
            val tanggal = doc.getString("tanggal") ?: ""
            val namaPeminjam = doc.getString("namaPeminjam") ?: ""
            val tx = LoanTransactionEntity(
                idTransaksi = idTransaksi,
                tanggal = tanggal,
                namaPeminjam = namaPeminjam,
                kelas = doc.getString("kelas") ?: "",
                waktu = doc.getString("waktu") ?: "",
                kondisi = doc.getString("kondisi") ?: "",
                namaPetugas = doc.getString("namaPetugas") ?: "",
                status = doc.getString("status") ?: "Dipinjam",
                tanggalKembali = doc.getString("tanggalKembali"),
                waktuKembali = doc.getString("waktuKembali"),
                kondisiKembali = doc.getString("kondisiKembali"),
                petugasKembali = doc.getString("petugasKembali"),
                keteranganKerusakan = doc.getString("keteranganKerusakan")
            )
            val items = getItemsForTransaction(idTransaksi)
            val isReturned = tx.status == "Kembali"
            val returnStatusDisplay = if (isReturned) {
                val tgl = tx.tanggalKembali ?: tx.tanggal
                val wkt = tx.waktuKembali ?: tx.waktu
                val ptg = tx.petugasKembali ?: tx.namaPetugas
                "Dikembalikan pada $tgl $wkt WIB (Petugas: $ptg)"
            } else {
                "Belum Dikembalikan"
            }
            TransactionDetailResult(
                transaction = tx,
                items = items,
                returnStatusDisplay = returnStatusDisplay,
                isReturned = isReturned
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getAllLoanItems(): List<LoanItemEntity> {
        return try {
            val snapshot = firestore.collection(PATH_LOAN_ITEMS).get().await()
            snapshot.documents.mapNotNull { doc ->
                LoanItemEntity(
                    id = doc.getLong("id")?.toInt() ?: doc.id.hashCode(),
                    idTransaksi = doc.getString("idTransaksi") ?: "",
                    idBarang = doc.getString("idBarang") ?: "",
                    namaBarang = doc.getString("namaBarang") ?: "",
                    jumlah = doc.getLong("jumlah")?.toInt() ?: 1
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun createLoan(
        transaction: LoanTransactionEntity,
        items: List<LoanItemEntity>,
        settingsRepo: SettingsRepository
    ): Boolean {
        return try {
            firestore.collection(PATH_TRANSACTIONS).document(transaction.idTransaksi)
                .set(transaction, SetOptions.merge()).await()

            for (item in items) {
                val docId = "${item.idTransaksi}_${item.idBarang}"
                firestore.collection(PATH_LOAN_ITEMS).document(docId)
                    .set(item, SetOptions.merge()).await()
            }

            val webAppUrl = settingsRepo.getSheetsUrl()
            if (webAppUrl.isNotEmpty() && settingsRepo.isAutoSyncEnabled()) {
                try {
                    val success = syncService.pushLoan(webAppUrl, transaction, items)
                    if (success) {
                        val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm:ss", Locale("id", "ID"))
                        settingsRepo.setLastSyncTime(sdf.format(Date()))
                    }
                } catch (e: Exception) {
                    Log.e("InventoryRepo", "Failed to sync loan to sheets", e)
                }
            }
            true
        } catch (e: Exception) {
            Log.e("InventoryRepo", "Error creating loan in Firestore", e)
            false
        }
    }

    suspend fun returnLoan(
        idTransaksi: String,
        tanggalKembali: String,
        waktuKembali: String,
        kondisiKembali: String,
        petugasKembali: String,
        keteranganKerusakan: String?,
        itemConditions: Map<String, String> = emptyMap(),
        itemDamagedCounts: Map<String, Int> = emptyMap(),
        itemNotes: Map<String, String> = emptyMap(),
        settingsRepo: SettingsRepository
    ): Boolean {
        return try {
            val updateData = hashMapOf<String, Any?>(
                "status" to "Kembali",
                "tanggalKembali" to tanggalKembali,
                "waktuKembali" to waktuKembali,
                "kondisiKembali" to kondisiKembali,
                "petugasKembali" to petugasKembali,
                "keteranganKerusakan" to keteranganKerusakan
            )
            firestore.collection(PATH_TRANSACTIONS).document(idTransaksi)
                .set(updateData, SetOptions.merge()).await()

            val loanItems = getItemsForTransaction(idTransaksi)
            for (item in loanItems) {
                val cond = itemConditions[item.idBarang] ?: "Baik / Normal"
                val note = itemNotes[item.idBarang]?.trim() ?: ""
                val totalQty = item.jumlah
                val damagedQty = if (itemDamagedCounts.containsKey(item.idBarang)) {
                    (itemDamagedCounts[item.idBarang] ?: 0).coerceIn(0, totalQty)
                } else {
                    if (cond == "Baik / Normal") 0 else totalQty
                }

                if (damagedQty > 0) {
                    val docId = "${item.idBarang}_${System.currentTimeMillis()}"
                    val damagedMap = hashMapOf<String, Any?>(
                        "idBarang" to item.idBarang,
                        "namaBarang" to item.namaBarang,
                        "jumlah" to damagedQty,
                        "tanggalKerusakan" to tanggalKembali,
                        "waktuKerusakan" to waktuKembali,
                        "keteranganKerusakan" to note.ifBlank { "Rusak saat pengembalian ($damagedQty unit)" },
                        "namaPetugas" to petugasKembali,
                        "kondisiBaru" to cond,
                        "status" to "Rusak (Perlu Tindakan)"
                    )
                    firestore.collection(PATH_DAMAGED_ITEMS).document(docId).set(damagedMap, SetOptions.merge()).await()
                }
            }

            val webAppUrl = settingsRepo.getSheetsUrl()
            if (webAppUrl.isNotEmpty() && settingsRepo.isAutoSyncEnabled()) {
                try {
                    val success = syncService.pushReturn(
                        webAppUrl = webAppUrl,
                        idTransaksi = idTransaksi,
                        tanggalKembali = tanggalKembali,
                        waktuKembali = waktuKembali,
                        kondisiKembali = kondisiKembali,
                        petugasKembali = petugasKembali
                    )
                    if (success) {
                        val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm:ss", Locale("id", "ID"))
                        settingsRepo.setLastSyncTime(sdf.format(Date()))
                    }
                } catch (e: Exception) {
                    Log.e("InventoryRepo", "Failed to sync return to sheets", e)
                }
            }
            true
        } catch (e: Exception) {
            Log.e("InventoryRepo", "Error returning loan in Firestore", e)
            false
        }
    }

    suspend fun syncWithSheets(
        settingsRepo: SettingsRepository,
        onProgress: (String) -> Unit
    ): Result<Unit> {
        val webAppUrl = settingsRepo.getSheetsUrl()
        if (webAppUrl.isEmpty()) {
            return Result.failure(Exception("URL Google Sheets belum diatur! Silakan atur di menu Pengaturan."))
        }
        return try {
            onProgress("Menghubungkan ke Google Sheets...")
            val sheetsItems = syncService.pullItems(webAppUrl)
            if (sheetsItems.isNotEmpty()) {
                onProgress("Menyinkronkan data barang ke Firestore Cloud...")
                sheetsItems.forEach { item ->
                    updateItem(item)
                }
            }
            val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm:ss", Locale("id", "ID"))
            settingsRepo.setLastSyncTime(sdf.format(Date()))
            onProgress("Sinkronisasi Cloud selesai dengan sukses!")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("InventoryRepo", "Synchronization failed", e)
            Result.failure(Exception("Gagal menyinkronkan data: ${e.localizedMessage ?: "Koneksi bermasalah"}"))
        }
    }

    suspend fun clearAllData() {
        val collections = listOf(PATH_ITEMS, PATH_TRANSACTIONS, PATH_LOAN_ITEMS, PATH_DAMAGED_ITEMS, PATH_PEMAKAIAN_BAHAN, PATH_BAHAN_AFKIR)
        for (coll in collections) {
            try {
                val snapshot = firestore.collection(coll).get().await()
                snapshot.documents.forEach { doc -> doc.reference.delete() }
            } catch (e: Exception) {
                Log.e("InventoryRepo", "Error clearing Firestore collection $coll", e)
            }
        }
    }

    suspend fun clearAllTransactions() {
        val collections = listOf(PATH_TRANSACTIONS, PATH_LOAN_ITEMS, PATH_DAMAGED_ITEMS, PATH_PEMAKAIAN_BAHAN, PATH_BAHAN_AFKIR)
        for (coll in collections) {
            try {
                val snapshot = firestore.collection(coll).get().await()
                snapshot.documents.forEach { doc -> doc.reference.delete() }
            } catch (e: Exception) {
                Log.e("InventoryRepo", "Error clearing Firestore transaction collection $coll", e)
            }
        }
    }

    suspend fun recordPemakaian(pemakaian: PemakaianBahanEntity) {
        firestore.collection(PATH_PEMAKAIAN_BAHAN).document(pemakaian.idPemakaian)
            .set(pemakaian, SetOptions.merge()).await()
    }

    suspend fun recordBahanAfkir(afkir: BahanAfkirEntity) {
        firestore.collection(PATH_BAHAN_AFKIR).document(afkir.idAfkir)
            .set(afkir, SetOptions.merge()).await()
    }

    suspend fun undoBahanAfkir(idAfkir: String) {
        firestore.collection(PATH_BAHAN_AFKIR).document(idAfkir).delete().await()
    }

    suspend fun updateBahanAfkirStatusCustom(idAfkir: String, status: String) {
        firestore.collection(PATH_BAHAN_AFKIR).document(idAfkir)
            .set(mapOf("status" to status), SetOptions.merge()).await()
    }

    suspend fun deleteBahanAfkirPermanently(
        idAfkir: String,
        currentDate: String,
        currentTime: String,
        namaPetugas: String
    ) {
        firestore.collection(PATH_BAHAN_AFKIR).document(idAfkir).delete().await()
    }

    suspend fun insertMutasiPerangkat(entity: MutasiPerangkatEntity) {
        firestore.collection(PATH_MUTASI_PERANGKAT).document(entity.idMutasi)
            .set(entity, SetOptions.merge()).await()
    }

    suspend fun deleteMutasiPerangkat(entity: MutasiPerangkatEntity) {
        firestore.collection(PATH_MUTASI_PERANGKAT).document(entity.idMutasi).delete().await()
    }

    suspend fun recordDamagedReport(damaged: DamagedItemEntity) {
        val docId = damaged.id.toString().ifBlank { "${damaged.idBarang}_${System.currentTimeMillis()}" }
        firestore.collection(PATH_DAMAGED_ITEMS).document(docId).set(damaged, SetOptions.merge()).await()
    }

    suspend fun recordHibahDamagedItem(
        id: Int,
        penerima: String = "",
        alasan: String = "",
        currentDate: String = "",
        currentTime: String = "",
        namaPetugas: String = "",
        recipient: String = penerima,
        reason: String = alasan,
        officer: String = namaPetugas,
        date: String = currentDate
    ) {
        val finalPenerima = penerima.ifBlank { recipient }
        val finalAlasan = alasan.ifBlank { reason }
        val finalOfficer = namaPetugas.ifBlank { officer }
        val finalDate = currentDate.ifBlank { date }

        val updateMap = mapOf(
            "isHibah" to true,
            "penerimaHibah" to finalPenerima,
            "alasanHibah" to finalAlasan,
            "status" to "Dihibahkan",
            "petugasTindakan" to finalOfficer,
            "tanggalTindakan" to finalDate,
            "waktuTindakan" to currentTime
        )
        firestore.collection(PATH_DAMAGED_ITEMS).document(id.toString()).set(updateMap, SetOptions.merge()).await()
    }

    suspend fun cancelDamagedReport(id: Int) {
        firestore.collection(PATH_DAMAGED_ITEMS).document(id.toString()).delete().await()
    }

    suspend fun validateDamagedItem(id: Int, date: String, officer: String, notes: String) {
        val updateMap = mapOf(
            "status" to "Tervalidasi",
            "tanggalTindakan" to date,
            "petugasTindakan" to officer,
            "catatanTindakan" to notes
        )
        firestore.collection(PATH_DAMAGED_ITEMS).document(id.toString()).set(updateMap, SetOptions.merge()).await()
    }

    suspend fun updateDamagedStatus(
        damagedId: Int,
        newStatus: String,
        alasan: String,
        namaPetugas: String,
        currentDate: String,
        currentTime: String
    ) {
        val updateMap = mapOf(
            "status" to newStatus,
            "alasanHibah" to alasan,
            "petugasTindakan" to namaPetugas,
            "tanggalTindakan" to currentDate
        )
        firestore.collection(PATH_DAMAGED_ITEMS).document(damagedId.toString()).set(updateMap, SetOptions.merge()).await()
    }

    suspend fun deleteDamagedItemPermanently(
        id: Int,
        currentDate: String,
        currentTime: String,
        namaPetugas: String
    ) {
        firestore.collection(PATH_DAMAGED_ITEMS).document(id.toString()).delete().await()
    }

    // Peripheral Rusak
    suspend fun recordPeripheralRusakReport(report: PeripheralRusakEntity) {
        val docId = report.id.toString().ifBlank { "PRPH_${System.currentTimeMillis()}" }
        firestore.collection(PATH_PERIPHERAL_RUSAK).document(docId).set(report, SetOptions.merge()).await()
    }

    suspend fun updatePeripheralRusak(item: PeripheralRusakEntity) {
        firestore.collection(PATH_PERIPHERAL_RUSAK).document(item.id.toString()).set(item, SetOptions.merge()).await()
    }

    suspend fun restorePeripheralToStock(
        id: Int,
        recoveryDate: String = "",
        newCondition: String = "Baik",
        reason: String = "",
        namaPetugas: String = "",
        currentTime: String = ""
    ) {
        firestore.collection(PATH_PERIPHERAL_RUSAK).document(id.toString()).delete().await()
    }

    suspend fun updatePeripheralRusakStatus(
        id: Int,
        newStatus: String = "",
        newDiagnosa: String = "",
        catatan: String = "",
        namaPetugas: String = "",
        currentDate: String = "",
        currentTime: String = ""
    ) {
        val updateMap = mapOf(
            "status" to newStatus,
            "statusDiagnosa" to newDiagnosa,
            "keteranganKerusakan" to catatan,
            "namaPetugas" to namaPetugas,
            "tanggalKerusakan" to currentDate,
            "waktuKerusakan" to currentTime
        )
        firestore.collection(PATH_PERIPHERAL_RUSAK).document(id.toString()).set(updateMap, SetOptions.merge()).await()
    }

    suspend fun recordHibahPeripheralRusak(
        id: Int,
        penerima: String = "",
        alasan: String = "",
        currentDate: String = "",
        currentTime: String = "",
        namaPetugas: String = "",
        recipient: String = penerima,
        reason: String = alasan,
        officer: String = namaPetugas,
        date: String = currentDate
    ) {
        val finalPenerima = penerima.ifBlank { recipient }
        val finalAlasan = alasan.ifBlank { reason }
        val finalOfficer = namaPetugas.ifBlank { officer }
        val finalDate = currentDate.ifBlank { date }

        val updateMap = mapOf(
            "status" to "Dihibahkan",
            "penerimaHibah" to finalPenerima,
            "alasanHibah" to finalAlasan,
            "namaPetugas" to finalOfficer,
            "tanggalKerusakan" to finalDate,
            "waktuKerusakan" to currentTime
        )
        firestore.collection(PATH_PERIPHERAL_RUSAK).document(id.toString()).set(updateMap, SetOptions.merge()).await()
    }

    suspend fun validatePeripheralRusak(id: Int, date: String, officer: String, notes: String) {
        val updateMap = mapOf(
            "status" to "Tervalidasi",
            "tanggalKerusakan" to date,
            "namaPetugas" to officer,
            "keteranganKerusakan" to notes
        )
        firestore.collection(PATH_PERIPHERAL_RUSAK).document(id.toString()).set(updateMap, SetOptions.merge()).await()
    }

    suspend fun deletePeripheralRusakPermanently(
        id: Int,
        currentDate: String,
        currentTime: String,
        namaPetugas: String
    ) {
        firestore.collection(PATH_PERIPHERAL_RUSAK).document(id.toString()).delete().await()
    }

    // Peripheral Stock Operations
    suspend fun insertPeripheralStock(stock: PeripheralStockEntity) {
        val docId = stock.idBarang.ifBlank { "PRPH_STK_${System.currentTimeMillis()}" }
        firestore.collection(PATH_PERIPHERAL_STOCKS).document(docId).set(stock, SetOptions.merge()).await()
    }

    suspend fun updatePeripheralStock(stock: PeripheralStockEntity) {
        firestore.collection(PATH_PERIPHERAL_STOCKS).document(stock.idBarang).set(stock, SetOptions.merge()).await()
    }

    suspend fun deletePeripheralStockById(id: Int) {
        firestore.collection(PATH_PERIPHERAL_STOCKS).document(id.toString()).delete().await()
    }

    suspend fun usePeripheralStock(
        id: Int,
        useQty: Int,
        targetPc: String,
        officerName: String,
        currentDate: String,
        currentTime: String
    ) {
        val docId = id.toString()
        try {
            val doc = firestore.collection(PATH_PERIPHERAL_STOCKS).document(docId).get().await()
            if (doc.exists()) {
                val currentQty = doc.getLong("jumlah")?.toInt() ?: 0
                val updatedQty = (currentQty - useQty).coerceAtLeast(0)
                firestore.collection(PATH_PERIPHERAL_STOCKS).document(docId)
                    .set(mapOf("jumlah" to updatedQty), SetOptions.merge()).await()
            }
        } catch (e: Exception) {
            Log.e("InventoryRepo", "Error updating peripheral stock usage in Firestore", e)
        }
    }

    // Kop Laporan Methods
    fun getKopLaporanFlow(): Flow<KopLaporanEntity> {
        return inventoryDao?.getKopLaporanFlow()?.map { it ?: KopLaporanEntity() }
            ?: kotlinx.coroutines.flow.flowOf(KopLaporanEntity())
    }

    suspend fun getKopLaporanSync(): KopLaporanEntity {
        return inventoryDao?.getKopLaporanSync() ?: KopLaporanEntity()
    }

    suspend fun saveKopLaporan(kop: KopLaporanEntity, saveToHistory: Boolean = true) {
        inventoryDao?.saveKopLaporan(kop)
        if (saveToHistory) {
            val title = kop.sekolahBaris1.ifBlank { "Kop Laporan" }
            val recent = RecentKopEntity(
                title = title,
                pemprovHeader = kop.pemprovHeader,
                pemprovFontSize = kop.pemprovFontSize,
                dinasHeader = kop.dinasHeader,
                dinasFontSize = kop.dinasFontSize,
                sekolahBaris1 = kop.sekolahBaris1,
                sekolahBaris1FontSize = kop.sekolahBaris1FontSize,
                sekolahBaris2 = kop.sekolahBaris2,
                sekolahBaris2FontSize = kop.sekolahBaris2FontSize,
                alamatBaris1 = kop.alamatBaris1,
                alamatBaris1FontSize = kop.alamatBaris1FontSize,
                alamatBaris2 = kop.alamatBaris2,
                alamatBaris2FontSize = kop.alamatBaris2FontSize,
                alamatBaris3 = kop.alamatBaris3,
                alamatBaris3FontSize = kop.alamatBaris3FontSize,
                lainnyaHeader = kop.lainnyaHeader,
                lainnyaFontSize = kop.lainnyaFontSize,
                rowOrder = kop.rowOrder,
                kopFontFamily = kop.kopFontFamily,
                tempatTanggal = kop.tempatTanggal,
                ttdFontFamily = kop.ttdFontFamily,
                ttdFontSize = kop.ttdFontSize,
                ttdSignersJson = kop.ttdSignersJson,
                timestamp = System.currentTimeMillis()
            )
            inventoryDao?.insertRecentKop(recent)
        }
    }

    fun getRecentKopListFlow(): Flow<List<RecentKopEntity>> {
        return inventoryDao?.getRecentKopListFlow() ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }

    suspend fun deleteRecentKop(id: Int) {
        inventoryDao?.deleteRecentKop(id)
    }
}
