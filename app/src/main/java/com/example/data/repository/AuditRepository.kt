package com.example.data.repository

import com.example.data.dao.InventoryDao
import com.example.data.entity.LoanTransactionEntity
import com.example.data.entity.isFakeTransaction
import com.example.data.network.FirebaseService
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AuditRepository(
    private val dao: InventoryDao,
    private val firebaseService: FirebaseService
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val _systemLogs = MutableStateFlow<List<LoanTransactionEntity>>(emptyList())
    val systemLogs: StateFlow<List<LoanTransactionEntity>> = _systemLogs.asStateFlow()

    init {
        listenToLocalAuditLogs()
        listenToCloudAuditLogs()
    }

    private fun listenToLocalAuditLogs() {
        coroutineScope.launch {
            try {
                dao.getAllTransactions().collect { list ->
                    val localSystemLogs = list.filter { it.isFakeTransaction() }
                    updateSystemLogs(localLogs = localSystemLogs)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private var cachedCloudLogs = listOf<LoanTransactionEntity>()
    private var cachedLocalLogs = listOf<LoanTransactionEntity>()

    private fun updateSystemLogs(
        localLogs: List<LoanTransactionEntity>? = null,
        cloudLogs: List<LoanTransactionEntity>? = null
    ) {
        if (localLogs != null) cachedLocalLogs = localLogs
        if (cloudLogs != null) cachedCloudLogs = cloudLogs

        val mergedMap = LinkedHashMap<String, LoanTransactionEntity>()
        (_systemLogs.value + cachedLocalLogs + cachedCloudLogs).forEach { item ->
            mergedMap[item.idTransaksi] = item
        }
        _systemLogs.value = mergedMap.values.sortedByDescending { it.idTransaksi }
    }

    private fun listenToCloudAuditLogs() {
        try {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("audit_logs")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    val cloudList = snapshot.documents.mapNotNull { doc ->
                        val idTx = doc.id
                        val tanggal = doc.getString("tanggal") ?: ""
                        val namaPeminjam = doc.getString("namaPeminjam") ?: ""
                        LoanTransactionEntity(
                            idTransaksi = idTx,
                            tanggal = tanggal,
                            namaPeminjam = namaPeminjam,
                            kelas = doc.getString("kelas") ?: "Aktivitas Sistem",
                            waktu = doc.getString("waktu") ?: "",
                            kondisi = doc.getString("kondisi") ?: "Normal",
                            namaPetugas = doc.getString("namaPetugas") ?: "Administrator",
                            status = doc.getString("status") ?: "Audit",
                            tanggalKembali = doc.getString("tanggalKembali"),
                            waktuKembali = doc.getString("waktuKembali"),
                            kondisiKembali = doc.getString("kondisiKembali"),
                            petugasKembali = doc.getString("petugasKembali"),
                            keteranganKerusakan = doc.getString("keteranganKerusakan")
                        )
                    }
                    updateSystemLogs(cloudLogs = cloudList)
                    coroutineScope.launch {
                        cloudList.forEach { tx ->
                            try {
                                dao.insertTransaction(tx)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun logActivity(
        activityType: String,
        subjectName: String,
        details: String,
        officerName: String = "Administrator"
    ) {
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val sdfTime = SimpleDateFormat("HH:mm", Locale.US)
        val currentDate = sdfDate.format(Date())
        val currentTime = sdfTime.format(Date())

        val idPrefix = when {
            activityType.contains("Impor", ignoreCase = true) -> "TX-IMP"
            activityType.contains("Manual", ignoreCase = true) || activityType.contains("Tambah", ignoreCase = true) -> "TX-INP"
            else -> "TX-AUD"
        }

        val auditTx = LoanTransactionEntity(
            idTransaksi = "$idPrefix-${System.currentTimeMillis()}",
            tanggal = currentDate,
            waktu = currentTime,
            namaPeminjam = "$activityType: $subjectName",
            kelas = "Aktivitas Sistem",
            kondisi = "Normal",
            namaPetugas = officerName.ifBlank { "Administrator" },
            status = activityType,
            tanggalKembali = currentDate,
            waktuKembali = currentTime,
            kondisiKembali = "Normal",
            petugasKembali = officerName.ifBlank { "Administrator" },
            keteranganKerusakan = details
        )

        try {
            dao.insertTransaction(auditTx)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val current = _systemLogs.value.toMutableList()
        current.add(0, auditTx)
        _systemLogs.value = current.distinctBy { it.idTransaksi }.sortedByDescending { it.idTransaksi }

        try {
            val firestore = FirebaseFirestore.getInstance()
            val docData = mapOf(
                "idTransaksi" to auditTx.idTransaksi,
                "tanggal" to auditTx.tanggal,
                "waktu" to auditTx.waktu,
                "namaPeminjam" to auditTx.namaPeminjam,
                "kelas" to auditTx.kelas,
                "kondisi" to auditTx.kondisi,
                "namaPetugas" to auditTx.namaPetugas,
                "status" to auditTx.status,
                "tanggalKembali" to auditTx.tanggalKembali,
                "waktuKembali" to auditTx.waktuKembali,
                "kondisiKembali" to auditTx.kondisiKembali,
                "petugasKembali" to auditTx.petugasKembali,
                "keteranganKerusakan" to auditTx.keteranganKerusakan
            )
            firestore.collection("audit_logs").document(auditTx.idTransaksi).set(docData)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
