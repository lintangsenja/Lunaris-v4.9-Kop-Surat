package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "peripheral_rusak")
data class PeripheralRusakEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val idBarang: String,
    val namaBarang: String,
    val subKategori: String = "🔌 Peripheral Lainnya",
    val jumlah: Int = 1,
    val tanggalKerusakan: String = "",
    val waktuKerusakan: String = "",
    val keteranganKerusakan: String = "",
    val namaPetugas: String = "",
    val statusDiagnosa: String = "Perlu Diagnosa",
    val status: String = "Rusak (Perlu Tindakan)",
    val validationCount: Int = 0,
    val lastValidatedDate: String = "",
    val lastValidatedBy: String = "",
    val validationNotes: String = "",
    val isHibah: Boolean = false,
    val penerimaHibah: String = "",
    val alasanHibah: String = "",
    val isDemo: Boolean = false
)
