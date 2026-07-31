package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "peripheral_stocks")
data class PeripheralStockEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val idBarang: String,
    val jenisPeripheral: String,
    val namaItem: String,
    val merek: String = "",
    val spesifikasi: String = "",
    val satuan: String = "Unit",
    val jumlah: Int = 0,
    val tanggalMasuk: String = "",
    val sumberDana: String = "",
    val lokasiRuang: String = "",
    val kondisi: String = "Baik",
    val serialNumber: String = "",
    val catatanModifikasi: String = "",
    val usedCount: Int = 0,
    val usedPCNotes: String = "",
    val isDemo: Boolean = false
)
