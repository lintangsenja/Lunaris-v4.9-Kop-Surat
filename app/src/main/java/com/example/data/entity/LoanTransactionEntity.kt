package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "loan_transactions")
data class LoanTransactionEntity(
    @PrimaryKey val idTransaksi: String,
    val tanggal: String,
    val namaPeminjam: String,
    val kelas: String,
    val waktu: String,
    val kondisi: String, // "Baik" or "Rusak"
    val namaPetugas: String,
    val status: String, // "Dipinjam" or "Kembali"
    val tanggalKembali: String? = null,
    val waktuKembali: String? = null,
    val kondisiKembali: String? = null, // "Baik" or "Rusak"
    val petugasKembali: String? = null,
    val keteranganKerusakan: String? = null,
    val whatsappNumber: String? = null,
    val durasiHari: Int = 1,
    val isDemo: Boolean = false,
    val tujuanPeminjaman: String? = null,
    val detailTujuan: String? = null
)

fun isFakeLoanTransaction(
    idTransaksi: String,
    status: String = "",
    kelas: String = "",
    namaPeminjam: String = ""
): Boolean {
    val id = idTransaksi.trim().uppercase()
    if (id.startsWith("TX-INP") || id.startsWith("TX-ADD") || id.startsWith("TX-OPN") ||
        id.startsWith("TX-RUM") || id.startsWith("TX-DEL") || id.startsWith("TX-AFK") ||
        id.startsWith("TX-DMG") || id.startsWith("TX-AUD") || id.startsWith("TX-VAL") ||
        id.startsWith("TX-PMK") || id.startsWith("TX-SYN")) {
        return true
    }

    val st = status.trim().lowercase()
    val fakeStatuses = listOf(
        "aset baru", "input baru", "penambahan stok", "tambah stok", "stock opname",
        "pindah ruangan", "hapus aset", "sistem / aset", "audit", "afkir", "rusak",
        "servis", "validasi petugas", "pemakaian bahan", "hibah", "aset / sistem"
    )
    if (fakeStatuses.any { st.contains(it) }) {
        return true
    }

    val kl = kelas.trim().lowercase()
    if (kl.contains("sistem / aset") || kl.contains("audit") || kl.contains("pemakaian bahan") ||
        kl.contains("tambah stok") || kl.contains("hapus aset")) {
        return true
    }

    val pm = namaPeminjam.trim().lowercase()
    if (pm.startsWith("input baru:") || pm.startsWith("penambahan stok:") ||
        pm.startsWith("tambah stok") || pm.startsWith("pindah ruangan:") ||
        pm.startsWith("hapus master aset:") || pm.startsWith("stock opname:") ||
        pm.startsWith("bahan afkir:") || pm.startsWith("barang rusak:") ||
        pm.startsWith("sedang servis:") || pm.startsWith("audit:") ||
        pm.startsWith("validasi alat rusak") || pm.startsWith("hapus permanen:")) {
        return true
    }

    return false
}

fun LoanTransactionEntity.isFakeTransaction(): Boolean {
    return isFakeLoanTransaction(
        idTransaksi = this.idTransaksi,
        status = this.status,
        kelas = this.kelas,
        namaPeminjam = this.namaPeminjam
    )
}

