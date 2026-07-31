package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "damaged_items")
data class DamagedItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val idBarang: String,
    val namaBarang: String,
    val jumlah: Int,
    val tanggalKerusakan: String,
    val waktuKerusakan: String,
    val keteranganKerusakan: String,
    val namaPetugas: String = "",
    val kondisiBaru: String = "",
    val status: String = "Rusak (Perlu Tindakan)",
    val statusKeterangan: String = "",
    val isDemo: Boolean = false,
    val validationCount: Int = 0,
    val lastValidatedDate: String = "",
    val lastValidatedBy: String = "",
    val validationNotes: String = "",
    val isHibah: Boolean = false,
    val penerimaHibah: String = "",
    val alasanHibah: String = ""
)
