package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

const val DEFAULT_KOP_ROW_ORDER = "pemprov,dinas,sekolah1,sekolah2,alamat1,alamat2,alamat3,lainnya"

fun parseKopRowOrder(orderStr: String?): List<String> {
    val allKeys = listOf("pemprov", "dinas", "sekolah1", "sekolah2", "alamat1", "alamat2", "alamat3", "lainnya")
    if (orderStr.isNullOrBlank()) return allKeys
    val parsed = orderStr.split(",").map { it.trim() }.filter { it in allKeys }
    val missing = allKeys.filter { it !in parsed }
    return parsed + missing
}

@Entity(tableName = "kop_laporan")
data class KopLaporanEntity(
    @PrimaryKey val id: Int = 1,
    val pemprovHeader: String = "PEMERINTAH PROVINSI JAWA TENGAH",
    val pemprovFontSize: Int = 14,
    val dinasHeader: String = "DINAS PENDIDIKAN DAN KEBUDAYAAN",
    val dinasFontSize: Int = 12,
    val sekolahBaris1: String = "SEKOLAH MENENGAH ATAS NEGERI 1 BOBOTSARI",
    val sekolahBaris1FontSize: Int = 16,
    val sekolahBaris2: String = "KABUPATEN PURBALINGGA",
    val sekolahBaris2FontSize: Int = 16,
    val alamatBaris1: String = "Jalan Raya Bobotsari No. 1, Bobotsari, Purbalingga 53353",
    val alamatBaris1FontSize: Int = 10,
    val alamatBaris2: String = "Telepon (0281) 759021 | Email: sman1bobotsari@yahoo.co.id",
    val alamatBaris2FontSize: Int = 10,
    val alamatBaris3: String = "Website: www.sman1bobotsari.sch.id",
    val alamatBaris3FontSize: Int = 10,
    val lainnyaHeader: String = "",
    val lainnyaFontSize: Int = 10,
    val logoKiriPath: String = "",
    val logoKananPath: String = "",
    val rowOrder: String = DEFAULT_KOP_ROW_ORDER
)

@Entity(tableName = "recent_kop")
data class RecentKopEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String = "",
    val pemprovHeader: String = "",
    val pemprovFontSize: Int = 14,
    val dinasHeader: String = "",
    val dinasFontSize: Int = 12,
    val sekolahBaris1: String = "",
    val sekolahBaris1FontSize: Int = 16,
    val sekolahBaris2: String = "",
    val sekolahBaris2FontSize: Int = 16,
    val alamatBaris1: String = "",
    val alamatBaris1FontSize: Int = 10,
    val alamatBaris2: String = "",
    val alamatBaris2FontSize: Int = 10,
    val alamatBaris3: String = "",
    val alamatBaris3FontSize: Int = 10,
    val lainnyaHeader: String = "",
    val lainnyaFontSize: Int = 10,
    val rowOrder: String = DEFAULT_KOP_ROW_ORDER,
    val timestamp: Long = System.currentTimeMillis()
)
