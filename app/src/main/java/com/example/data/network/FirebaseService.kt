package com.example.data.network

import android.util.Log
import com.example.data.database.AppDatabase
import com.example.data.entity.ItemEntity
import com.example.data.entity.LoanItemEntity
import com.example.data.entity.LoanTransactionEntity
import com.google.firebase.firestore.ListenerRegistration
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FirebaseService(private val db: AppDatabase) {

    private val firestore get() = FirebaseManager.getFirestore()
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _isCloudConnected = MutableStateFlow(true)
    val isCloudConnected: StateFlow<Boolean> = _isCloudConnected.asStateFlow()

    private var itemsListener: ListenerRegistration? = null
    private var transactionsListener: ListenerRegistration? = null
    private var loanItemsListener: ListenerRegistration? = null
    private var unitsListener: ListenerRegistration? = null
    private var categoriesListener: ListenerRegistration? = null
    private var usersListener: ListenerRegistration? = null

    fun startRealtimeSync() {
        Log.d("FirebaseService", "Starting real-time Firestore sync...")

        val fs = firestore
        if (fs == null) {
            _isCloudConnected.value = false
            Log.w("FirebaseService", "Firestore instance is null, real-time sync aborted")
            return
        }

        try {
            // 1. Sync Items
            itemsListener = fs.collection("items")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        _isCloudConnected.value = false
                        Log.e("FirebaseService", "Error listening to items", error)
                        return@addSnapshotListener
                    }
                    _isCloudConnected.value = true
                    if (snapshot != null) {
                        scope.launch {
                            try {
                                for (doc in snapshot.documents) {
                                    val idBarang = doc.id.ifBlank { doc.getString("idBarang") ?: doc.getString("id") ?: doc.getString("id_barang") ?: "" }
                                    val namaBarang = doc.getString("namaBarang") ?: doc.getString("name") ?: doc.getString("nama") ?: doc.getString("nama_barang") ?: doc.getString("title") ?: ""
                                    if (namaBarang.isBlank()) continue

                                    val stokAwal = doc.getLong("stokAwal")?.toInt()
                                        ?: doc.getLong("stock")?.toInt()
                                        ?: doc.getLong("stok")?.toInt()
                                        ?: doc.getLong("stok_awal")?.toInt()
                                        ?: doc.getLong("quantity")?.toInt()
                                        ?: doc.getLong("qty")?.toInt()
                                        ?: 0
                                    val kategori = doc.getString("kategori") ?: doc.getString("category") ?: ""
                                    val satuan = doc.getString("satuan") ?: doc.getString("unit") ?: ""
                                    val stokRusak = doc.getLong("stokRusak")?.toInt()
                                        ?: doc.getLong("stok_rusak")?.toInt()
                                        ?: doc.getLong("brokenStock")?.toInt()
                                        ?: 0
                                    val merekAlat = doc.getString("merekAlat") ?: doc.getString("merek") ?: doc.getString("brand") ?: ""
                                    val ruang = doc.getString("ruang") ?: doc.getString("room") ?: doc.getString("location") ?: doc.getString("ruangan") ?: ""
                                    val sumberDana = doc.getString("sumberDana") ?: doc.getString("sumber_dana") ?: doc.getString("source")
                                    val kondisi = doc.getString("kondisi") ?: doc.getString("condition") ?: ""
                                    val keterangan = doc.getString("keterangan") ?: doc.getString("description") ?: doc.getString("notes") ?: ""
                                    val isDemo = doc.getBoolean("isDemo") ?: doc.getBoolean("is_demo") ?: false
                                    val type = doc.getString("type") ?: doc.getString("tipe") ?: "ALAT"
                                    val isBorrowable = doc.getBoolean("isBorrowable")
                                        ?: doc.getBoolean("isAvailableForStudent")
                                        ?: doc.getBoolean("is_available_for_student")
                                        ?: doc.getBoolean("is_borrowable")
                                        ?: doc.getBoolean("borrowable")
                                        ?: true
                                    val serialNumber = doc.getString("serialNumber") ?: doc.getString("serial_number") ?: doc.getString("sn") ?: ""

                                    val item = ItemEntity(
                                        idBarang = idBarang,
                                        namaBarang = namaBarang,
                                        serialNumber = serialNumber,
                                        stokAwal = stokAwal,
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
                                    db.inventoryDao().insertItem(item)
                                }
                                Log.d("FirebaseService", "Synced ${snapshot.size()} items from Firestore to Room.")
                            } catch (e: Exception) {
                                Log.e("FirebaseService", "Failed to sync items to Room", e)
                            }
                        }
                    }
                }

            // 2. Sync Loan Transactions
            transactionsListener = fs.collection("transactions")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("FirebaseService", "Error listening to transactions", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        scope.launch {
                            try {
                                for (doc in snapshot.documents) {
                                    val idTransaksi = doc.id
                                    val tanggal = doc.getString("tanggal") ?: continue
                                    val namaPeminjam = doc.getString("namaPeminjam") ?: continue
                                    val kelas = doc.getString("kelas") ?: ""
                                    val waktu = doc.getString("waktu") ?: ""
                                    val kondisi = doc.getString("kondisi") ?: ""
                                    val namaPetugas = doc.getString("namaPetugas") ?: ""
                                    val status = doc.getString("status") ?: "Dipinjam"
                                    val tanggalKembali = doc.getString("tanggalKembali")
                                    val waktuKembali = doc.getString("waktuKembali")
                                    val kondisiKembali = doc.getString("kondisiKembali")
                                    val petugasKembali = doc.getString("petugasKembali")
                                    val keteranganKerusakan = doc.getString("keteranganKerusakan")
                                    val whatsappNumber = doc.getString("whatsappNumber")
                                    val durasiHari = doc.getLong("durasiHari")?.toInt() ?: 1
                                    val isDemo = doc.getBoolean("isDemo") ?: false
                                    val tujuanPeminjaman = doc.getString("tujuanPeminjaman")
                                    val detailTujuan = doc.getString("detailTujuan")

                                    val transaction = LoanTransactionEntity(
                                        idTransaksi = idTransaksi,
                                        tanggal = tanggal,
                                        namaPeminjam = namaPeminjam,
                                        kelas = kelas,
                                        waktu = waktu,
                                        kondisi = kondisi,
                                        namaPetugas = namaPetugas,
                                        status = status,
                                        tanggalKembali = tanggalKembali,
                                        waktuKembali = waktuKembali,
                                        kondisiKembali = kondisiKembali,
                                        petugasKembali = petugasKembali,
                                        keteranganKerusakan = keteranganKerusakan,
                                        whatsappNumber = whatsappNumber,
                                        durasiHari = durasiHari,
                                        isDemo = isDemo,
                                        tujuanPeminjaman = tujuanPeminjaman,
                                        detailTujuan = detailTujuan
                                    )
                                    db.inventoryDao().insertTransaction(transaction)
                                }
                                Log.d("FirebaseService", "Synced ${snapshot.size()} transactions from Firestore to Room.")
                            } catch (e: Exception) {
                                Log.e("FirebaseService", "Failed to sync transactions to Room", e)
                            }
                        }
                    }
                }

            // 3. Sync Loan Items
            loanItemsListener = fs.collection("loan_items")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("FirebaseService", "Error listening to loan_items", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        scope.launch {
                            try {
                                val itemsList = mutableListOf<LoanItemEntity>()
                                for (doc in snapshot.documents) {
                                    val id = doc.getLong("id")?.toInt() ?: continue
                                    val idTransaksi = doc.getString("idTransaksi") ?: continue
                                    val idBarang = doc.getString("idBarang") ?: continue
                                    val namaBarang = doc.getString("namaBarang") ?: continue
                                    val jumlah = doc.getLong("jumlah")?.toInt() ?: 1
                                    val isDemo = doc.getBoolean("isDemo") ?: false

                                    val loanItem = LoanItemEntity(
                                        id = id,
                                        idTransaksi = idTransaksi,
                                        idBarang = idBarang,
                                        namaBarang = namaBarang,
                                        jumlah = jumlah,
                                        isDemo = isDemo
                                    )
                                    itemsList.add(loanItem)
                                }
                                if (itemsList.isNotEmpty()) {
                                    val groupedByTx = itemsList.groupBy { it.idTransaksi }
                                    for ((txId, txItems) in groupedByTx) {
                                        db.inventoryDao().deleteLoanItemsForTransaction(txId)
                                        db.inventoryDao().insertLoanItems(txItems)
                                    }
                                    db.inventoryDao().cleanupDuplicateLoanItems()
                                }
                                Log.d("FirebaseService", "Synced ${snapshot.size()} loan items from Firestore to Room.")
                            } catch (e: Exception) {
                                Log.e("FirebaseService", "Failed to sync loan items to Room", e)
                            }
                        }
                    }
                }

            // 4. Sync Units
            unitsListener = fs.collection("units")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("FirebaseService", "Error listening to units", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        scope.launch {
                            try {
                                val existingUnits = db.inventoryDao().getAllUnitsList()
                                val existingNames = existingUnits.map { it.name.trim().lowercase() }.toSet()
                                for (doc in snapshot.documents) {
                                    val name = (doc.getString("name") ?: doc.id).trim()
                                    if (name.isNotBlank() && !existingNames.contains(name.lowercase())) {
                                        db.inventoryDao().insertUnit(com.example.data.entity.UnitEntity(name = name))
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("FirebaseService", "Failed to sync units to Room", e)
                            }
                        }
                    }
                }

            // 5. Sync Categories
            categoriesListener = fs.collection("categories")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("FirebaseService", "Error listening to categories", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        scope.launch {
                            try {
                                val existingCats = db.inventoryDao().getAllCategoriesList()
                                val existingNames = existingCats.map { it.name.trim().lowercase() }.toSet()
                                for (doc in snapshot.documents) {
                                    val name = (doc.getString("name") ?: doc.id).trim()
                                    if (name.isNotBlank() && !existingNames.contains(name.lowercase())) {
                                        db.inventoryDao().insertCategory(com.example.data.entity.CategoryEntity(name = name))
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("FirebaseService", "Failed to sync categories to Room", e)
                            }
                        }
                    }
                }

            // 6. Sync Users / Profiles
            usersListener = fs.collection("users")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("FirebaseService", "Error listening to users", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        scope.launch {
                            try {
                                for (doc in snapshot.documents) {
                                    val username = doc.id
                                    val fullName = doc.getString("fullName") ?: doc.getString("full_name") ?: ""
                                    val role = doc.getString("role") ?: "siswa"
                                    val photoUrl = doc.getString("photoUrl") ?: doc.getString("photo_url") ?: ""
                                    val existingUser = db.inventoryDao().getUserByUsername(username)
                                    if (existingUser != null) {
                                        val updated = existingUser.copy(
                                            fullName = if (fullName.isNotBlank()) fullName else existingUser.fullName,
                                            role = if (role.isNotBlank()) role else existingUser.role,
                                            photoUrl = if (photoUrl.isNotBlank()) photoUrl else existingUser.photoUrl
                                        )
                                        db.inventoryDao().insertUser(updated)
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("FirebaseService", "Failed to sync users to Room", e)
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e("FirebaseService", "Failed to start realtime sync: ${e.message}", e)
        }
    }

    fun stopRealtimeSync() {
        itemsListener?.remove()
        transactionsListener?.remove()
        loanItemsListener?.remove()
        unitsListener?.remove()
        categoriesListener?.remove()
        usersListener?.remove()
        Log.d("FirebaseService", "Stopped real-time Firestore sync.")
    }

    fun cleanupDuplicateUnitsInFirestore(onComplete: (() -> Unit)? = null) {
        scope.launch {
            try {
                val fs = firestore ?: run {
                    onComplete?.invoke()
                    return@launch
                }
                fs.collection("units").get()
                    .addOnSuccessListener { snapshot ->
                        if (snapshot != null && !snapshot.isEmpty) {
                            val seenNames = mutableSetOf<String>()
                            val batch = fs.batch()
                            var hasDeletions = false

                            for (doc in snapshot.documents) {
                                val name = (doc.getString("name") ?: doc.id).trim()
                                val normKey = name.lowercase()
                                if (normKey.isBlank() || seenNames.contains(normKey)) {
                                    batch.delete(doc.reference)
                                    hasDeletions = true
                                } else {
                                    seenNames.add(normKey)
                                }
                            }
                            if (hasDeletions) {
                                batch.commit().addOnCompleteListener {
                                    Log.d("FirebaseService", "Cleaned up duplicate units in Firestore.")
                                    onComplete?.invoke()
                                }
                            } else {
                                onComplete?.invoke()
                            }
                        } else {
                            onComplete?.invoke()
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("FirebaseService", "Error scanning Firestore units collection", e)
                        onComplete?.invoke()
                    }
            } catch (e: Exception) {
                Log.e("FirebaseService", "Exception cleaning Firestore units", e)
                onComplete?.invoke()
            }
        }
    }

    fun saveItemToFirestore(item: ItemEntity) {
        scope.launch {
            val itemMap = mapOf(
                "idBarang" to item.idBarang,
                "id" to item.idBarang,
                "namaBarang" to item.namaBarang,
                "name" to item.namaBarang,
                "nama" to item.namaBarang,
                "serialNumber" to item.serialNumber,
                "stokAwal" to item.stokAwal,
                "stock" to item.stokAwal,
                "stok" to item.stokAwal,
                "kategori" to item.kategori,
                "category" to item.kategori,
                "satuan" to item.satuan,
                "unit" to item.satuan,
                "stokRusak" to item.stokRusak,
                "merekAlat" to item.merekAlat,
                "merek" to item.merekAlat,
                "brand" to item.merekAlat,
                "ruang" to item.ruang,
                "room" to item.ruang,
                "sumberDana" to item.sumberDana,
                "kondisi" to item.kondisi,
                "condition" to item.kondisi,
                "keterangan" to item.keterangan,
                "isDemo" to item.isDemo,
                "type" to item.type,
                "isBorrowable" to item.isBorrowable,
                "isAvailableForStudent" to item.isBorrowable
            )
            FirebaseManager.setDocument("items", item.idBarang, itemMap, merge = true)
        }
    }

    fun deleteItemFromFirestore(idBarang: String) {
        scope.launch {
            FirebaseManager.deleteDocument("items", idBarang)
        }
    }

    fun saveTransactionToFirestore(transaction: LoanTransactionEntity) {
        scope.launch {
            FirebaseManager.setDocument("transactions", transaction.idTransaksi, transaction)
        }
    }

    fun saveLoanItemsToFirestore(items: List<LoanItemEntity>) {
        scope.launch {
            for (item in items) {
                val docId = "${item.idTransaksi}_${item.idBarang}"
                val data = mapOf(
                    "id" to item.id,
                    "idTransaksi" to item.idTransaksi,
                    "idBarang" to item.idBarang,
                    "namaBarang" to item.namaBarang,
                    "jumlah" to item.jumlah,
                    "isDemo" to item.isDemo
                )
                FirebaseManager.setDocument("loan_items", docId, data)
            }
        }
    }

    fun clearAllTransactionsFromFirestore(onComplete: (() -> Unit)? = null) {
        scope.launch {
            try {
                val fs = firestore
                if (fs == null) {
                    onComplete?.invoke()
                    return@launch
                }
                val collections = listOf("transactions", "loan_items", "pemakaian_bahan", "bahan_afkir", "damaged_items")
                for (coll in collections) {
                    fs.collection(coll).get()
                        .addOnSuccessListener { snapshot ->
                            if (snapshot != null && !snapshot.isEmpty) {
                                val batch = fs.batch()
                                for (doc in snapshot.documents) {
                                    batch.delete(doc.reference)
                                }
                                batch.commit().addOnCompleteListener {
                                    Log.d("FirebaseService", "Cleared collection $coll from Firestore.")
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("FirebaseService", "Error querying collection $coll for deletion", e)
                        }
                }
                onComplete?.invoke()
            } catch (e: Exception) {
                Log.e("FirebaseService", "Exception clearing Firestore transaction collections", e)
                onComplete?.invoke()
            }
        }
    }

    fun uploadLocalDataToCloud(
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            try {
                val fs = firestore
                if (fs == null) {
                    withContext(Dispatchers.Main) {
                        onError("Koneksi Firestore tidak tersedia. Periksa jaringan internet Anda.")
                    }
                    return@launch
                }

                var uploadedCount = 0

                // 1. Items
                val items = db.inventoryDao().getAllItemsList()
                for (item in items) {
                    val itemMap = mapOf(
                        "idBarang" to item.idBarang,
                        "id" to item.idBarang,
                        "namaBarang" to item.namaBarang,
                        "name" to item.namaBarang,
                        "nama" to item.namaBarang,
                        "serialNumber" to item.serialNumber,
                        "stokAwal" to item.stokAwal,
                        "stock" to item.stokAwal,
                        "stok" to item.stokAwal,
                        "kategori" to item.kategori,
                        "category" to item.kategori,
                        "satuan" to item.satuan,
                        "unit" to item.satuan,
                        "stokRusak" to item.stokRusak,
                        "merekAlat" to item.merekAlat,
                        "merek" to item.merekAlat,
                        "brand" to item.merekAlat,
                        "ruang" to item.ruang,
                        "room" to item.ruang,
                        "sumberDana" to item.sumberDana,
                        "kondisi" to item.kondisi,
                        "condition" to item.kondisi,
                        "keterangan" to item.keterangan,
                        "isDemo" to item.isDemo,
                        "type" to item.type,
                        "isBorrowable" to item.isBorrowable,
                        "isAvailableForStudent" to item.isBorrowable
                    )
                    fs.collection("items").document(item.idBarang).set(itemMap, com.google.firebase.firestore.SetOptions.merge())
                    uploadedCount++
                }

                // 2. Transactions
                val transactions = db.inventoryDao().getAllLoanTransactionsList()
                for (tx in transactions) {
                    fs.collection("transactions").document(tx.idTransaksi).set(tx, com.google.firebase.firestore.SetOptions.merge())
                    uploadedCount++
                }

                // 3. Loan Items
                val loanItems = db.inventoryDao().getAllLoanItemsList()
                for (item in loanItems) {
                    val docId = "${item.idTransaksi}_${item.idBarang}"
                    val data = mapOf(
                        "id" to item.id,
                        "idTransaksi" to item.idTransaksi,
                        "idBarang" to item.idBarang,
                        "namaBarang" to item.namaBarang,
                        "jumlah" to item.jumlah,
                        "isDemo" to item.isDemo
                    )
                    fs.collection("loan_items").document(docId).set(data, com.google.firebase.firestore.SetOptions.merge())
                    uploadedCount++
                }

                // 4. Users
                val users = db.inventoryDao().getAllUsersList()
                for (user in users) {
                    val userData = mutableMapOf<String, Any>(
                        "username" to user.username,
                        "role" to user.role,
                        "fullName" to user.fullName,
                        "createdAt" to user.createdAt
                    )
                    if (user.password.isNotBlank()) userData["password"] = user.password
                    if (user.photoUrl.isNotBlank()) userData["photoUrl"] = user.photoUrl
                    fs.collection("users").document(user.username).set(userData, com.google.firebase.firestore.SetOptions.merge())
                    uploadedCount++

                    // Sync Super Admin photoUrl to central document pengaturan_global/profil_admin
                    if (user.photoUrl.isNotBlank() && (user.role == "super_admin" || user.username.equals("admin", true) || user.username.equals("lintang", true))) {
                        val adminData = mapOf(
                            "photoUrl" to user.photoUrl,
                            "admin_photo_url" to user.photoUrl,
                            "admin_username" to user.username,
                            "admin_full_name" to user.fullName,
                            "updatedAt" to System.currentTimeMillis()
                        )
                        fs.collection("pengaturan_global").document("profil_admin").set(adminData, com.google.firebase.firestore.SetOptions.merge())
                    }
                }

                // 5. Units
                val units = db.inventoryDao().getAllUnitsList()
                for (unit in units) {
                    val docId = "UNIT_${unit.id}"
                    val unitData = mapOf(
                        "id" to unit.id,
                        "name" to unit.name,
                        "isDemo" to unit.isDemo
                    )
                    fs.collection("units").document(docId).set(unitData, com.google.firebase.firestore.SetOptions.merge())
                    uploadedCount++
                }

                // 6. Pemakaian Bahan
                val pemakaian = db.inventoryDao().getAllPemakaianBahanList()
                for (p in pemakaian) {
                    fs.collection("pemakaian_bahan").document(p.idPemakaian).set(p, com.google.firebase.firestore.SetOptions.merge())
                    uploadedCount++
                }

                _isCloudConnected.value = true
                withContext(Dispatchers.Main) {
                    onSuccess("Berhasil mengunggah $uploadedCount dokumen ke Firestore Cloud!")
                }
            } catch (e: Exception) {
                Log.e("FirebaseService", "Error uploading local data to Cloud", e)
                _isCloudConnected.value = false
                withContext(Dispatchers.Main) {
                    onError("Gagal mengunggah data ke Cloud: ${e.message}")
                }
            }
        }
    }

    fun downloadCloudDataToLocal(
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            try {
                val fs = firestore
                if (fs == null) {
                    withContext(Dispatchers.Main) {
                        onError("Koneksi Firestore tidak tersedia. Periksa jaringan internet Anda.")
                    }
                    return@launch
                }

                var downloadedCount = 0

                // 1. Items
                val itemsTask = fs.collection("items").get()
                val itemsSnapshot = Tasks.await(itemsTask)
                for (doc in itemsSnapshot.documents) {
                    val idBarang = doc.id.ifBlank { doc.getString("idBarang") ?: doc.getString("id") ?: doc.getString("id_barang") ?: "" }
                    val namaBarang = doc.getString("namaBarang") ?: doc.getString("name") ?: doc.getString("nama") ?: doc.getString("nama_barang") ?: doc.getString("title") ?: ""
                    if (namaBarang.isBlank()) continue

                    val stokAwal = doc.getLong("stokAwal")?.toInt()
                        ?: doc.getLong("stock")?.toInt()
                        ?: doc.getLong("stok")?.toInt()
                        ?: doc.getLong("stok_awal")?.toInt()
                        ?: doc.getLong("quantity")?.toInt()
                        ?: doc.getLong("qty")?.toInt()
                        ?: 0
                    val kategori = doc.getString("kategori") ?: doc.getString("category") ?: ""
                    val satuan = doc.getString("satuan") ?: doc.getString("unit") ?: ""
                    val stokRusak = doc.getLong("stokRusak")?.toInt()
                        ?: doc.getLong("stok_rusak")?.toInt()
                        ?: doc.getLong("brokenStock")?.toInt()
                        ?: 0
                    val merekAlat = doc.getString("merekAlat") ?: doc.getString("merek") ?: doc.getString("brand") ?: ""
                    val ruang = doc.getString("ruang") ?: doc.getString("room") ?: doc.getString("location") ?: doc.getString("ruangan") ?: ""
                    val sumberDana = doc.getString("sumberDana") ?: doc.getString("sumber_dana") ?: doc.getString("source")
                    val kondisi = doc.getString("kondisi") ?: doc.getString("condition") ?: ""
                    val keterangan = doc.getString("keterangan") ?: doc.getString("description") ?: doc.getString("notes") ?: ""
                    val isDemo = doc.getBoolean("isDemo") ?: doc.getBoolean("is_demo") ?: false
                    val type = doc.getString("type") ?: doc.getString("tipe") ?: "ALAT"
                    val isBorrowable = doc.getBoolean("isBorrowable")
                        ?: doc.getBoolean("isAvailableForStudent")
                        ?: doc.getBoolean("is_available_for_student")
                        ?: doc.getBoolean("is_borrowable")
                        ?: doc.getBoolean("borrowable")
                        ?: true
                    val serialNumber = doc.getString("serialNumber") ?: doc.getString("serial_number") ?: doc.getString("sn") ?: ""

                    val item = ItemEntity(
                        idBarang = idBarang,
                        namaBarang = namaBarang,
                        serialNumber = serialNumber,
                        stokAwal = stokAwal,
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
                    db.inventoryDao().insertItem(item)
                    downloadedCount++
                }

                // 2. Transactions
                val txTask = fs.collection("transactions").get()
                val txSnapshot = Tasks.await(txTask)
                for (doc in txSnapshot.documents) {
                    val idTransaksi = doc.id
                    val tanggal = doc.getString("tanggal") ?: continue
                    val namaPeminjam = doc.getString("namaPeminjam") ?: continue
                    val kelas = doc.getString("kelas") ?: ""
                    val waktu = doc.getString("waktu") ?: ""
                    val kondisi = doc.getString("kondisi") ?: ""
                    val namaPetugas = doc.getString("namaPetugas") ?: ""
                    val status = doc.getString("status") ?: "Dipinjam"
                    val tanggalKembali = doc.getString("tanggalKembali")
                    val waktuKembali = doc.getString("waktuKembali")
                    val kondisiKembali = doc.getString("kondisiKembali")
                    val petugasKembali = doc.getString("petugasKembali")
                    val keteranganKerusakan = doc.getString("keteranganKerusakan")
                    val whatsappNumber = doc.getString("whatsappNumber")
                    val durasiHari = doc.getLong("durasiHari")?.toInt() ?: 1
                    val isDemo = doc.getBoolean("isDemo") ?: false
                    val tujuanPeminjaman = doc.getString("tujuanPeminjaman")
                    val detailTujuan = doc.getString("detailTujuan")

                    val tx = LoanTransactionEntity(
                        idTransaksi = idTransaksi,
                        tanggal = tanggal,
                        namaPeminjam = namaPeminjam,
                        kelas = kelas,
                        waktu = waktu,
                        kondisi = kondisi,
                        namaPetugas = namaPetugas,
                        status = status,
                        tanggalKembali = tanggalKembali,
                        waktuKembali = waktuKembali,
                        kondisiKembali = kondisiKembali,
                        petugasKembali = petugasKembali,
                        keteranganKerusakan = keteranganKerusakan,
                        whatsappNumber = whatsappNumber,
                        durasiHari = durasiHari,
                        isDemo = isDemo,
                        tujuanPeminjaman = tujuanPeminjaman,
                        detailTujuan = detailTujuan
                    )
                    db.inventoryDao().insertTransaction(tx)
                    downloadedCount++
                }

                // 3. Users
                val userTask = fs.collection("users").get()
                val userSnapshot = Tasks.await(userTask)
                for (doc in userSnapshot.documents) {
                    val username = doc.id
                    val fullName = doc.getString("fullName") ?: doc.getString("full_name") ?: ""
                    val role = doc.getString("role") ?: "siswa"
                    val photoUrl = doc.getString("photoUrl") ?: doc.getString("photo_url") ?: ""
                    val password = doc.getString("password") ?: "123456"
                    val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    val user = com.example.data.entity.UserEntity(
                        username = username,
                        password = password,
                        role = role,
                        fullName = fullName,
                        createdAt = createdAt,
                        photoUrl = photoUrl
                    )
                    db.inventoryDao().insertUser(user)
                    downloadedCount++
                }

                // 4. Units
                val unitsTask = fs.collection("units").get()
                val unitsSnapshot = Tasks.await(unitsTask)
                for (doc in unitsSnapshot.documents) {
                    val name = doc.getString("name") ?: doc.id
                    val isDemo = doc.getBoolean("isDemo") ?: false
                    if (name.isNotBlank()) {
                        db.inventoryDao().insertUnit(com.example.data.entity.UnitEntity(name = name.trim(), isDemo = isDemo))
                        downloadedCount++
                    }
                }

                // 5. Central Admin Profile Photo (pengaturan_global/profil_admin)
                try {
                    val adminDocTask = fs.collection("pengaturan_global").document("profil_admin").get()
                    val adminSnapshot = Tasks.await(adminDocTask)
                    if (adminSnapshot.exists()) {
                        val adminPhoto = adminSnapshot.getString("photoUrl") ?: adminSnapshot.getString("admin_photo_url")
                        val adminUsername = adminSnapshot.getString("admin_username") ?: "lintang"
                        if (!adminPhoto.isNullOrBlank()) {
                            val adminUser = db.inventoryDao().getUserByUsername(adminUsername)
                            if (adminUser != null) {
                                db.inventoryDao().insertUser(adminUser.copy(photoUrl = adminPhoto))
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("FirebaseService", "Error syncing central admin photo from profil_admin", e)
                }

                _isCloudConnected.value = true
                withContext(Dispatchers.Main) {
                    onSuccess("Berhasil mengunduh $downloadedCount item/data dari Firestore Cloud ke database lokal!")
                }
            } catch (e: Exception) {
                Log.e("FirebaseService", "Error downloading Cloud data to local", e)
                _isCloudConnected.value = false
                withContext(Dispatchers.Main) {
                    onError("Gagal mengunduh data dari Cloud: ${e.message}")
                }
            }
        }
    }
}
