package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

const val DEFAULT_KOP_ROW_ORDER = "pemprov,dinas,sekolah1,sekolah2,alamat1,alamat2,alamat3,lainnya"
const val DEFAULT_KOP_FONT_FAMILY = "Times New Roman"
const val DEFAULT_TTD_FONT_FAMILY = "Times New Roman"
const val DEFAULT_TTD_FONT_SIZE = 10

data class TtdSignerItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val jabatan: String = "",
    val nama: String = "",
    val nip: String = "",
    val isEnabled: Boolean = true
)

val DEFAULT_JABATAN_OPTIONS = listOf(
    "Kepala Sekolah",
    "Waka Sarpras",
    "Kepala Tata Usaha",
    "Kepala Laboratorium",
    "Teknisi/Toolman"
)

fun getDefaultTtdSigners(): List<TtdSignerItem> {
    return DEFAULT_JABATAN_OPTIONS.map { jabatan ->
        TtdSignerItem(
            jabatan = jabatan,
            nama = "",
            nip = "",
            isEnabled = true
        )
    }
}

fun parseTtdSigners(jsonStr: String?): List<TtdSignerItem> {
    if (jsonStr.isNullOrBlank()) return getDefaultTtdSigners()
    return try {
        val array = org.json.JSONArray(jsonStr)
        val list = mutableListOf<TtdSignerItem>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                TtdSignerItem(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    jabatan = obj.optString("jabatan", ""),
                    nama = obj.optString("nama", ""),
                    nip = obj.optString("nip", ""),
                    isEnabled = obj.optBoolean("isEnabled", true)
                )
            )
        }
        if (list.isEmpty()) getDefaultTtdSigners() else list
    } catch (e: Exception) {
        getDefaultTtdSigners()
    }
}

fun serializeTtdSigners(list: List<TtdSignerItem>): String {
    return try {
        val array = org.json.JSONArray()
        for (item in list) {
            val obj = org.json.JSONObject()
            obj.put("id", item.id)
            obj.put("jabatan", item.jabatan)
            obj.put("nama", item.nama)
            obj.put("nip", item.nip)
            obj.put("isEnabled", item.isEnabled)
            array.put(obj)
        }
        array.toString()
    } catch (e: Exception) {
        ""
    }
}

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
    val rowOrder: String = DEFAULT_KOP_ROW_ORDER,
    val kopFontFamily: String = DEFAULT_KOP_FONT_FAMILY,
    val tempatTanggal: String = "",
    val ttdFontFamily: String = DEFAULT_TTD_FONT_FAMILY,
    val ttdFontSize: Int = DEFAULT_TTD_FONT_SIZE,
    val ttdSignersJson: String = ""
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
    val kopFontFamily: String = DEFAULT_KOP_FONT_FAMILY,
    val tempatTanggal: String = "",
    val ttdFontFamily: String = DEFAULT_TTD_FONT_FAMILY,
    val ttdFontSize: Int = DEFAULT_TTD_FONT_SIZE,
    val ttdSignersJson: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
