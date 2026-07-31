package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mutasi_perangkat")
data class MutasiPerangkatEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val idMutasi: String = "",
    val idBarang: String = "",
    val namaBarang: String = "",
    val serialNumber: String = "",
    val jenisPerangkat: String = "PERIPHERAL", // PERIPHERAL, LABKOM, ALAT
    val ruangAsal: String = "",
    val ruangTujuan: String = "",
    val tanggalMutasi: String = "",
    val namaPetugas: String = "",
    val alasanMutasi: String = "",
    val keterangan: String = "",
    val isDemo: Boolean = false
)
