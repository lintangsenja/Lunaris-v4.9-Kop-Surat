package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.InventoryDao
import com.example.data.entity.ItemEntity
import com.example.data.entity.LoanItemEntity
import com.example.data.entity.LoanTransactionEntity
import com.example.data.entity.DamagedItemEntity
import com.example.data.entity.CategoryEntity
import com.example.data.entity.UnitEntity
import com.example.data.entity.PemakaianBahanEntity
import com.example.data.entity.BahanAfkirEntity
import com.example.data.entity.ProfileEntity
import com.example.data.entity.UserEntity
import com.example.data.entity.PeripheralEntity
import com.example.data.entity.PeripheralRusakEntity
import com.example.data.entity.PeripheralStockEntity
import com.example.data.entity.MutasiPerangkatEntity
import com.example.data.entity.KopLaporanEntity
import com.example.data.entity.RecentKopEntity

@Database(
    entities = [
        ItemEntity::class, 
        LoanTransactionEntity::class, 
        LoanItemEntity::class, 
        DamagedItemEntity::class,
        CategoryEntity::class,
        UnitEntity::class,
        PemakaianBahanEntity::class,
        BahanAfkirEntity::class,
        ProfileEntity::class,
        UserEntity::class,
        PeripheralEntity::class,
        PeripheralRusakEntity::class,
        PeripheralStockEntity::class,
        MutasiPerangkatEntity::class,
        KopLaporanEntity::class,
        RecentKopEntity::class
    ],
    version = 26,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun inventoryDao(): InventoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun resetDatabaseInstance() {
            synchronized(this) {
                INSTANCE = null
            }
        }

        private fun migrateDatabaseToLatest(database: SupportSQLiteDatabase) {
            val tableCreateQueries = mapOf(
                "units" to "CREATE TABLE IF NOT EXISTS `units` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `isDemo` INTEGER NOT NULL DEFAULT 0)",
                "categories" to "CREATE TABLE IF NOT EXISTS `categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `isDemo` INTEGER NOT NULL DEFAULT 0)",
                "items" to "CREATE TABLE IF NOT EXISTS `items` (`idBarang` TEXT NOT NULL, `namaBarang` TEXT NOT NULL, `serialNumber` TEXT NOT NULL DEFAULT '', `stokAwal` INTEGER NOT NULL, `kategori` TEXT NOT NULL, `satuan` TEXT NOT NULL, `stokRusak` INTEGER NOT NULL, `merekAlat` TEXT NOT NULL, `ruang` TEXT NOT NULL, `sumberDana` TEXT, `kondisi` TEXT NOT NULL, `keterangan` TEXT NOT NULL, `isDemo` INTEGER NOT NULL DEFAULT 0, `type` TEXT NOT NULL DEFAULT 'ALAT', `isBorrowable` INTEGER NOT NULL DEFAULT 1, PRIMARY KEY(`idBarang`))",
                "bahan_afkir" to "CREATE TABLE IF NOT EXISTS `bahan_afkir` (`idAfkir` TEXT NOT NULL, `idBarang` TEXT NOT NULL, `namaBarang` TEXT NOT NULL, `jumlahAfkir` INTEGER NOT NULL, `satuan` TEXT NOT NULL, `alasan` TEXT NOT NULL, `tanggalAfkir` TEXT NOT NULL, `status` TEXT NOT NULL, `isDemo` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`idAfkir`))",
                "loan_items" to "CREATE TABLE IF NOT EXISTS `loan_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `idTransaksi` TEXT NOT NULL, `idBarang` TEXT NOT NULL, `namaBarang` TEXT NOT NULL, `jumlah` INTEGER NOT NULL, `isDemo` INTEGER NOT NULL DEFAULT 0)",
                "damaged_items" to "CREATE TABLE IF NOT EXISTS `damaged_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `idBarang` TEXT NOT NULL, `namaBarang` TEXT NOT NULL, `jumlah` INTEGER NOT NULL, `tanggalKerusakan` TEXT NOT NULL, `waktuKerusakan` TEXT NOT NULL, `keteranganKerusakan` TEXT NOT NULL, `namaPetugas` TEXT NOT NULL, `kondisiBaru` TEXT NOT NULL, `status` TEXT NOT NULL, `statusKeterangan` TEXT NOT NULL, `isDemo` INTEGER NOT NULL DEFAULT 0, `validationCount` INTEGER NOT NULL DEFAULT 0, `lastValidatedDate` TEXT NOT NULL DEFAULT '', `lastValidatedBy` TEXT NOT NULL DEFAULT '', `validationNotes` TEXT NOT NULL DEFAULT '', `isHibah` INTEGER NOT NULL DEFAULT 0, `penerimaHibah` TEXT NOT NULL DEFAULT '', `alasanHibah` TEXT NOT NULL DEFAULT '')",
                "pemakaian_bahan" to "CREATE TABLE IF NOT EXISTS `pemakaian_bahan` (`idPemakaian` TEXT NOT NULL, `idBarang` TEXT NOT NULL, `namaBarang` TEXT NOT NULL, `jumlahDiambil` INTEGER NOT NULL, `satuan` TEXT NOT NULL, `namaPeminta` TEXT NOT NULL, `jabatan` TEXT NOT NULL, `kelas` TEXT, `namaPetugas` TEXT NOT NULL, `tanggalPemakaian` TEXT NOT NULL, `keterangan` TEXT NOT NULL, `isDemo` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`idPemakaian`))",
                "loan_transactions" to "CREATE TABLE IF NOT EXISTS `loan_transactions` (`idTransaksi` TEXT NOT NULL, `tanggal` TEXT NOT NULL, `namaPeminjam` TEXT NOT NULL, `kelas` TEXT NOT NULL, `waktu` TEXT NOT NULL, `kondisi` TEXT NOT NULL, `namaPetugas` TEXT NOT NULL, `status` TEXT NOT NULL, `tanggalKembali` TEXT, `waktuKembali` TEXT, `kondisiKembali` TEXT, `petugasKembali` TEXT, `keteranganKerusakan` TEXT, `whatsappNumber` TEXT, `durasiHari` INTEGER NOT NULL DEFAULT 1, `isDemo` INTEGER NOT NULL DEFAULT 0, `tujuanPeminjaman` TEXT, `detailTujuan` TEXT, PRIMARY KEY(`idTransaksi`))",
                "profile" to "CREATE TABLE IF NOT EXISTS `profile` (`id` INTEGER NOT NULL, `namaPetugas` TEXT NOT NULL, `nip` TEXT NOT NULL, `namaInstansi` TEXT NOT NULL, `fotoUri` TEXT NOT NULL, PRIMARY KEY(`id`))",
                "users" to "CREATE TABLE IF NOT EXISTS `users` (`username` TEXT NOT NULL, `password` TEXT NOT NULL, `role` TEXT NOT NULL, `fullName` TEXT NOT NULL DEFAULT '', `createdAt` INTEGER NOT NULL DEFAULT 0, `photoUrl` TEXT NOT NULL DEFAULT '', PRIMARY KEY(`username`))",
                "peripherals" to "CREATE TABLE IF NOT EXISTS `peripherals` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `isDemo` INTEGER NOT NULL DEFAULT 0)",
                "peripheral_rusak" to "CREATE TABLE IF NOT EXISTS `peripheral_rusak` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `idBarang` TEXT NOT NULL, `namaBarang` TEXT NOT NULL, `subKategori` TEXT NOT NULL DEFAULT '🔌 Peripheral Lainnya', `jumlah` INTEGER NOT NULL DEFAULT 1, `tanggalKerusakan` TEXT NOT NULL DEFAULT '', `waktuKerusakan` TEXT NOT NULL DEFAULT '', `keteranganKerusakan` TEXT NOT NULL DEFAULT '', `namaPetugas` TEXT NOT NULL DEFAULT '', `statusDiagnosa` TEXT NOT NULL DEFAULT 'Perlu Diagnosa', `status` TEXT NOT NULL DEFAULT 'Rusak (Perlu Tindakan)', `validationCount` INTEGER NOT NULL DEFAULT 0, `lastValidatedDate` TEXT NOT NULL DEFAULT '', `lastValidatedBy` TEXT NOT NULL DEFAULT '', `validationNotes` TEXT NOT NULL DEFAULT '', `isHibah` INTEGER NOT NULL DEFAULT 0, `penerimaHibah` TEXT NOT NULL DEFAULT '', `alasanHibah` TEXT NOT NULL DEFAULT '', `isDemo` INTEGER NOT NULL DEFAULT 0)",
                "peripheral_stocks" to "CREATE TABLE IF NOT EXISTS `peripheral_stocks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `idBarang` TEXT NOT NULL, `jenisPeripheral` TEXT NOT NULL, `namaItem` TEXT NOT NULL, `merek` TEXT NOT NULL DEFAULT '', `spesifikasi` TEXT NOT NULL DEFAULT '', `satuan` TEXT NOT NULL DEFAULT 'Unit', `jumlah` INTEGER NOT NULL DEFAULT 0, `tanggalMasuk` TEXT NOT NULL DEFAULT '', `sumberDana` TEXT NOT NULL DEFAULT '', `lokasiRuang` TEXT NOT NULL DEFAULT '', `kondisi` TEXT NOT NULL DEFAULT 'Baik', `serialNumber` TEXT NOT NULL DEFAULT '', `catatanModifikasi` TEXT NOT NULL DEFAULT '', `usedCount` INTEGER NOT NULL DEFAULT 0, `usedPCNotes` TEXT NOT NULL DEFAULT '', `isDemo` INTEGER NOT NULL DEFAULT 0)",
                "mutasi_perangkat" to "CREATE TABLE IF NOT EXISTS `mutasi_perangkat` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `idMutasi` TEXT NOT NULL DEFAULT '', `idBarang` TEXT NOT NULL DEFAULT '', `namaBarang` TEXT NOT NULL DEFAULT '', `serialNumber` TEXT NOT NULL DEFAULT '', `jenisPerangkat` TEXT NOT NULL DEFAULT 'PERIPHERAL', `ruangAsal` TEXT NOT NULL DEFAULT '', `ruangTujuan` TEXT NOT NULL DEFAULT '', `tanggalMutasi` TEXT NOT NULL DEFAULT '', `namaPetugas` TEXT NOT NULL DEFAULT '', `alasanMutasi` TEXT NOT NULL DEFAULT '', `keterangan` TEXT NOT NULL DEFAULT '', `isDemo` INTEGER NOT NULL DEFAULT 0)",
                "kop_laporan" to "CREATE TABLE IF NOT EXISTS `kop_laporan` (`id` INTEGER PRIMARY KEY NOT NULL DEFAULT 1, `pemprovHeader` TEXT NOT NULL DEFAULT 'PEMERINTAH PROVINSI JAWA TENGAH', `pemprovFontSize` INTEGER NOT NULL DEFAULT 14, `dinasHeader` TEXT NOT NULL DEFAULT 'DINAS PENDIDIKAN DAN KEBUDAYAAN', `dinasFontSize` INTEGER NOT NULL DEFAULT 12, `sekolahBaris1` TEXT NOT NULL DEFAULT 'SEKOLAH MENENGAH ATAS NEGERI 1 BOBOTSARI', `sekolahBaris1FontSize` INTEGER NOT NULL DEFAULT 16, `sekolahBaris2` TEXT NOT NULL DEFAULT 'KABUPATEN PURBALINGGA', `sekolahBaris2FontSize` INTEGER NOT NULL DEFAULT 16, `alamatBaris1` TEXT NOT NULL DEFAULT 'Jalan Raya Bobotsari No. 1, Bobotsari, Purbalingga 53353', `alamatBaris1FontSize` INTEGER NOT NULL DEFAULT 10, `alamatBaris2` TEXT NOT NULL DEFAULT 'Telepon (0281) 759021 | Email: sman1bobotsari@yahoo.co.id', `alamatBaris2FontSize` INTEGER NOT NULL DEFAULT 10, `alamatBaris3` TEXT NOT NULL DEFAULT 'Website: www.sman1bobotsari.sch.id', `alamatBaris3FontSize` INTEGER NOT NULL DEFAULT 10, `lainnyaHeader` TEXT NOT NULL DEFAULT '', `lainnyaFontSize` INTEGER NOT NULL DEFAULT 10, `logoKiriPath` TEXT NOT NULL DEFAULT '', `logoKananPath` TEXT NOT NULL DEFAULT '', `rowOrder` TEXT NOT NULL DEFAULT 'pemprov,dinas,sekolah1,sekolah2,alamat1,alamat2,alamat3,lainnya', `kopFontFamily` TEXT NOT NULL DEFAULT 'Times New Roman', `tempatTanggal` TEXT NOT NULL DEFAULT '', `ttdFontFamily` TEXT NOT NULL DEFAULT 'Times New Roman', `ttdFontSize` INTEGER NOT NULL DEFAULT 10, `ttdSignersJson` TEXT NOT NULL DEFAULT '')",
                "recent_kop" to "CREATE TABLE IF NOT EXISTS `recent_kop` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL DEFAULT '', `pemprovHeader` TEXT NOT NULL DEFAULT '', `pemprovFontSize` INTEGER NOT NULL DEFAULT 14, `dinasHeader` TEXT NOT NULL DEFAULT '', `dinasFontSize` INTEGER NOT NULL DEFAULT 12, `sekolahBaris1` TEXT NOT NULL DEFAULT '', `sekolahBaris1FontSize` INTEGER NOT NULL DEFAULT 16, `sekolahBaris2` TEXT NOT NULL DEFAULT '', `sekolahBaris2FontSize` INTEGER NOT NULL DEFAULT 16, `alamatBaris1` TEXT NOT NULL DEFAULT '', `alamatBaris1FontSize` INTEGER NOT NULL DEFAULT 10, `alamatBaris2` TEXT NOT NULL DEFAULT '', `alamatBaris2FontSize` INTEGER NOT NULL DEFAULT 10, `alamatBaris3` TEXT NOT NULL DEFAULT '', `alamatBaris3FontSize` INTEGER NOT NULL DEFAULT 10, `lainnyaHeader` TEXT NOT NULL DEFAULT '', `lainnyaFontSize` INTEGER NOT NULL DEFAULT 10, `rowOrder` TEXT NOT NULL DEFAULT 'pemprov,dinas,sekolah1,sekolah2,alamat1,alamat2,alamat3,lainnya', `kopFontFamily` TEXT NOT NULL DEFAULT 'Times New Roman', `tempatTanggal` TEXT NOT NULL DEFAULT '', `ttdFontFamily` TEXT NOT NULL DEFAULT 'Times New Roman', `ttdFontSize` INTEGER NOT NULL DEFAULT 10, `ttdSignersJson` TEXT NOT NULL DEFAULT '', `timestamp` INTEGER NOT NULL DEFAULT 0)"
            )

            val tablesWithColumns = mapOf(
                "mutasi_perangkat" to listOf(
                    "id" to "INTEGER NOT NULL DEFAULT 0",
                    "idMutasi" to "TEXT NOT NULL DEFAULT ''",
                    "idBarang" to "TEXT NOT NULL DEFAULT ''",
                    "namaBarang" to "TEXT NOT NULL DEFAULT ''",
                    "serialNumber" to "TEXT NOT NULL DEFAULT ''",
                    "jenisPerangkat" to "TEXT NOT NULL DEFAULT 'PERIPHERAL'",
                    "ruangAsal" to "TEXT NOT NULL DEFAULT ''",
                    "ruangTujuan" to "TEXT NOT NULL DEFAULT ''",
                    "tanggalMutasi" to "TEXT NOT NULL DEFAULT ''",
                    "namaPetugas" to "TEXT NOT NULL DEFAULT ''",
                    "alasanMutasi" to "TEXT NOT NULL DEFAULT ''",
                    "keterangan" to "TEXT NOT NULL DEFAULT ''",
                    "isDemo" to "INTEGER NOT NULL DEFAULT 0"
                ),
                "peripheral_stocks" to listOf(
                    "id" to "INTEGER NOT NULL DEFAULT 0",
                    "idBarang" to "TEXT NOT NULL DEFAULT ''",
                    "jenisPeripheral" to "TEXT NOT NULL DEFAULT ''",
                    "namaItem" to "TEXT NOT NULL DEFAULT ''",
                    "merek" to "TEXT NOT NULL DEFAULT ''",
                    "spesifikasi" to "TEXT NOT NULL DEFAULT ''",
                    "satuan" to "TEXT NOT NULL DEFAULT 'Unit'",
                    "jumlah" to "INTEGER NOT NULL DEFAULT 0",
                    "tanggalMasuk" to "TEXT NOT NULL DEFAULT ''",
                    "sumberDana" to "TEXT NOT NULL DEFAULT ''",
                    "lokasiRuang" to "TEXT NOT NULL DEFAULT ''",
                    "kondisi" to "TEXT NOT NULL DEFAULT 'Baik'",
                    "serialNumber" to "TEXT NOT NULL DEFAULT ''",
                    "catatanModifikasi" to "TEXT NOT NULL DEFAULT ''",
                    "usedCount" to "INTEGER NOT NULL DEFAULT 0",
                    "usedPCNotes" to "TEXT NOT NULL DEFAULT ''",
                    "isDemo" to "INTEGER NOT NULL DEFAULT 0"
                ),
                "peripheral_rusak" to listOf(
                    "id" to "INTEGER NOT NULL DEFAULT 0",
                    "idBarang" to "TEXT NOT NULL DEFAULT ''",
                    "namaBarang" to "TEXT NOT NULL DEFAULT ''",
                    "subKategori" to "TEXT NOT NULL DEFAULT '🔌 Peripheral Lainnya'",
                    "jumlah" to "INTEGER NOT NULL DEFAULT 1",
                    "tanggalKerusakan" to "TEXT NOT NULL DEFAULT ''",
                    "waktuKerusakan" to "TEXT NOT NULL DEFAULT ''",
                    "keteranganKerusakan" to "TEXT NOT NULL DEFAULT ''",
                    "namaPetugas" to "TEXT NOT NULL DEFAULT ''",
                    "statusDiagnosa" to "TEXT NOT NULL DEFAULT 'Perlu Diagnosa'",
                    "status" to "TEXT NOT NULL DEFAULT 'Rusak (Perlu Tindakan)'",
                    "validationCount" to "INTEGER NOT NULL DEFAULT 0",
                    "lastValidatedDate" to "TEXT NOT NULL DEFAULT ''",
                    "lastValidatedBy" to "TEXT NOT NULL DEFAULT ''",
                    "validationNotes" to "TEXT NOT NULL DEFAULT ''",
                    "isHibah" to "INTEGER NOT NULL DEFAULT 0",
                    "penerimaHibah" to "TEXT NOT NULL DEFAULT ''",
                    "alasanHibah" to "TEXT NOT NULL DEFAULT ''",
                    "isDemo" to "INTEGER NOT NULL DEFAULT 0"
                ),
                "peripherals" to listOf(
                    "id" to "INTEGER NOT NULL DEFAULT 0",
                    "name" to "TEXT NOT NULL DEFAULT ''",
                    "isDemo" to "INTEGER NOT NULL DEFAULT 0"
                ),
                "units" to listOf(
                    "id" to "INTEGER NOT NULL DEFAULT 0",
                    "name" to "TEXT NOT NULL DEFAULT ''",
                    "isDemo" to "INTEGER NOT NULL DEFAULT 0"
                ),
                "categories" to listOf(
                    "id" to "INTEGER NOT NULL DEFAULT 0",
                    "name" to "TEXT NOT NULL DEFAULT ''",
                    "isDemo" to "INTEGER NOT NULL DEFAULT 0"
                ),
                "items" to listOf(
                    "idBarang" to "TEXT NOT NULL DEFAULT ''",
                    "namaBarang" to "TEXT NOT NULL DEFAULT ''",
                    "serialNumber" to "TEXT NOT NULL DEFAULT ''",
                    "stokAwal" to "INTEGER NOT NULL DEFAULT 0",
                    "kategori" to "TEXT NOT NULL DEFAULT ''",
                    "satuan" to "TEXT NOT NULL DEFAULT ''",
                    "stokRusak" to "INTEGER NOT NULL DEFAULT 0",
                    "merekAlat" to "TEXT NOT NULL DEFAULT ''",
                    "ruang" to "TEXT NOT NULL DEFAULT ''",
                    "sumberDana" to "TEXT",
                    "kondisi" to "TEXT NOT NULL DEFAULT ''",
                    "keterangan" to "TEXT NOT NULL DEFAULT ''",
                    "isDemo" to "INTEGER NOT NULL DEFAULT 0",
                    "type" to "TEXT NOT NULL DEFAULT 'ALAT'",
                    "isBorrowable" to "INTEGER NOT NULL DEFAULT 1"
                ),
                "bahan_afkir" to listOf(
                    "idAfkir" to "TEXT NOT NULL DEFAULT ''",
                    "idBarang" to "TEXT NOT NULL DEFAULT ''",
                    "namaBarang" to "TEXT NOT NULL DEFAULT ''",
                    "jumlahAfkir" to "INTEGER NOT NULL DEFAULT 0",
                    "satuan" to "TEXT NOT NULL DEFAULT ''",
                    "alasan" to "TEXT NOT NULL DEFAULT ''",
                    "tanggalAfkir" to "TEXT NOT NULL DEFAULT ''",
                    "status" to "TEXT NOT NULL DEFAULT 'Aktif'",
                    "isDemo" to "INTEGER NOT NULL DEFAULT 0"
                ),
                "loan_items" to listOf(
                    "id" to "INTEGER NOT NULL DEFAULT 0",
                    "idTransaksi" to "TEXT NOT NULL DEFAULT ''",
                    "idBarang" to "TEXT NOT NULL DEFAULT ''",
                    "namaBarang" to "TEXT NOT NULL DEFAULT ''",
                    "jumlah" to "INTEGER NOT NULL DEFAULT 0",
                    "isDemo" to "INTEGER NOT NULL DEFAULT 0"
                ),
                "damaged_items" to listOf(
                    "id" to "INTEGER NOT NULL DEFAULT 0",
                    "idBarang" to "TEXT NOT NULL DEFAULT ''",
                    "namaBarang" to "TEXT NOT NULL DEFAULT ''",
                    "jumlah" to "INTEGER NOT NULL DEFAULT 0",
                    "tanggalKerusakan" to "TEXT NOT NULL DEFAULT ''",
                    "waktuKerusakan" to "TEXT NOT NULL DEFAULT ''",
                    "keteranganKerusakan" to "TEXT NOT NULL DEFAULT ''",
                    "namaPetugas" to "TEXT NOT NULL DEFAULT ''",
                    "kondisiBaru" to "TEXT NOT NULL DEFAULT ''",
                    "status" to "TEXT NOT NULL DEFAULT 'Rusak (Perlu Tindakan)'",
                    "statusKeterangan" to "TEXT NOT NULL DEFAULT ''",
                    "isDemo" to "INTEGER NOT NULL DEFAULT 0",
                    "validationCount" to "INTEGER NOT NULL DEFAULT 0",
                    "lastValidatedDate" to "TEXT NOT NULL DEFAULT ''",
                    "lastValidatedBy" to "TEXT NOT NULL DEFAULT ''",
                    "validationNotes" to "TEXT NOT NULL DEFAULT ''",
                    "isHibah" to "INTEGER NOT NULL DEFAULT 0",
                    "penerimaHibah" to "TEXT NOT NULL DEFAULT ''",
                    "alasanHibah" to "TEXT NOT NULL DEFAULT ''"
                ),
                "pemakaian_bahan" to listOf(
                    "idPemakaian" to "TEXT NOT NULL DEFAULT ''",
                    "idBarang" to "TEXT NOT NULL DEFAULT ''",
                    "namaBarang" to "TEXT NOT NULL DEFAULT ''",
                    "jumlahDiambil" to "INTEGER NOT NULL DEFAULT 0",
                    "satuan" to "TEXT NOT NULL DEFAULT ''",
                    "namaPeminta" to "TEXT NOT NULL DEFAULT ''",
                    "jabatan" to "TEXT NOT NULL DEFAULT ''",
                    "kelas" to "TEXT",
                    "namaPetugas" to "TEXT NOT NULL DEFAULT ''",
                    "tanggalPemakaian" to "TEXT NOT NULL DEFAULT ''",
                    "keterangan" to "TEXT NOT NULL DEFAULT ''",
                    "isDemo" to "INTEGER NOT NULL DEFAULT 0"
                ),
                "loan_transactions" to listOf(
                    "idTransaksi" to "TEXT NOT NULL DEFAULT ''",
                    "tanggal" to "TEXT NOT NULL DEFAULT ''",
                    "namaPeminjam" to "TEXT NOT NULL DEFAULT ''",
                    "kelas" to "TEXT NOT NULL DEFAULT ''",
                    "waktu" to "TEXT NOT NULL DEFAULT ''",
                    "kondisi" to "TEXT NOT NULL DEFAULT ''",
                    "namaPetugas" to "TEXT NOT NULL DEFAULT ''",
                    "status" to "TEXT NOT NULL DEFAULT ''",
                    "tanggalKembali" to "TEXT",
                    "waktuKembali" to "TEXT",
                    "kondisiKembali" to "TEXT",
                    "petugasKembali" to "TEXT",
                    "keteranganKerusakan" to "TEXT",
                    "whatsappNumber" to "TEXT",
                    "durasiHari" to "INTEGER NOT NULL DEFAULT 1",
                    "isDemo" to "INTEGER NOT NULL DEFAULT 0",
                    "tujuanPeminjaman" to "TEXT",
                    "detailTujuan" to "TEXT"
                ),
                "profile" to listOf(
                    "id" to "INTEGER NOT NULL DEFAULT 1",
                    "namaPetugas" to "TEXT NOT NULL DEFAULT ''",
                    "nip" to "TEXT NOT NULL DEFAULT ''",
                    "namaInstansi" to "TEXT NOT NULL DEFAULT ''",
                    "fotoUri" to "TEXT NOT NULL DEFAULT ''"
                ),
                "users" to listOf(
                    "username" to "TEXT NOT NULL DEFAULT ''",
                    "password" to "TEXT NOT NULL DEFAULT ''",
                    "role" to "TEXT NOT NULL DEFAULT 'siswa'",
                    "fullName" to "TEXT NOT NULL DEFAULT ''",
                    "createdAt" to "INTEGER NOT NULL DEFAULT 0",
                    "photoUrl" to "TEXT NOT NULL DEFAULT ''"
                ),
                "kop_laporan" to listOf(
                    "id" to "INTEGER PRIMARY KEY NOT NULL DEFAULT 1",
                    "pemprovHeader" to "TEXT NOT NULL DEFAULT ''",
                    "pemprovFontSize" to "INTEGER NOT NULL DEFAULT 14",
                    "dinasHeader" to "TEXT NOT NULL DEFAULT ''",
                    "dinasFontSize" to "INTEGER NOT NULL DEFAULT 12",
                    "sekolahBaris1" to "TEXT NOT NULL DEFAULT ''",
                    "sekolahBaris1FontSize" to "INTEGER NOT NULL DEFAULT 16",
                    "sekolahBaris2" to "TEXT NOT NULL DEFAULT ''",
                    "sekolahBaris2FontSize" to "INTEGER NOT NULL DEFAULT 16",
                    "alamatBaris1" to "TEXT NOT NULL DEFAULT ''",
                    "alamatBaris1FontSize" to "INTEGER NOT NULL DEFAULT 10",
                    "alamatBaris2" to "TEXT NOT NULL DEFAULT ''",
                    "alamatBaris2FontSize" to "INTEGER NOT NULL DEFAULT 10",
                    "alamatBaris3" to "TEXT NOT NULL DEFAULT ''",
                    "alamatBaris3FontSize" to "INTEGER NOT NULL DEFAULT 10",
                    "lainnyaHeader" to "TEXT NOT NULL DEFAULT ''",
                    "lainnyaFontSize" to "INTEGER NOT NULL DEFAULT 10",
                    "logoKiriPath" to "TEXT NOT NULL DEFAULT ''",
                    "logoKananPath" to "TEXT NOT NULL DEFAULT ''",
                    "rowOrder" to "TEXT NOT NULL DEFAULT 'pemprov,dinas,sekolah1,sekolah2,alamat1,alamat2,alamat3,lainnya'",
                    "kopFontFamily" to "TEXT NOT NULL DEFAULT 'Times New Roman'",
                    "tempatTanggal" to "TEXT NOT NULL DEFAULT ''",
                    "ttdFontFamily" to "TEXT NOT NULL DEFAULT 'Times New Roman'",
                    "ttdFontSize" to "INTEGER NOT NULL DEFAULT 10",
                    "ttdSignersJson" to "TEXT NOT NULL DEFAULT ''"
                ),
                "recent_kop" to listOf(
                    "id" to "INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL",
                    "title" to "TEXT NOT NULL DEFAULT ''",
                    "pemprovHeader" to "TEXT NOT NULL DEFAULT ''",
                    "pemprovFontSize" to "INTEGER NOT NULL DEFAULT 14",
                    "dinasHeader" to "TEXT NOT NULL DEFAULT ''",
                    "dinasFontSize" to "INTEGER NOT NULL DEFAULT 12",
                    "sekolahBaris1" to "TEXT NOT NULL DEFAULT ''",
                    "sekolahBaris1FontSize" to "INTEGER NOT NULL DEFAULT 16",
                    "sekolahBaris2" to "TEXT NOT NULL DEFAULT ''",
                    "sekolahBaris2FontSize" to "INTEGER NOT NULL DEFAULT 16",
                    "alamatBaris1" to "TEXT NOT NULL DEFAULT ''",
                    "alamatBaris1FontSize" to "INTEGER NOT NULL DEFAULT 10",
                    "alamatBaris2" to "TEXT NOT NULL DEFAULT ''",
                    "alamatBaris2FontSize" to "INTEGER NOT NULL DEFAULT 10",
                    "alamatBaris3" to "TEXT NOT NULL DEFAULT ''",
                    "alamatBaris3FontSize" to "INTEGER NOT NULL DEFAULT 10",
                    "lainnyaHeader" to "TEXT NOT NULL DEFAULT ''",
                    "lainnyaFontSize" to "INTEGER NOT NULL DEFAULT 10",
                    "rowOrder" to "TEXT NOT NULL DEFAULT 'pemprov,dinas,sekolah1,sekolah2,alamat1,alamat2,alamat3,lainnya'",
                    "kopFontFamily" to "TEXT NOT NULL DEFAULT 'Times New Roman'",
                    "tempatTanggal" to "TEXT NOT NULL DEFAULT ''",
                    "ttdFontFamily" to "TEXT NOT NULL DEFAULT 'Times New Roman'",
                    "ttdFontSize" to "INTEGER NOT NULL DEFAULT 10",
                    "ttdSignersJson" to "TEXT NOT NULL DEFAULT ''",
                    "timestamp" to "INTEGER NOT NULL DEFAULT 0"
                )
            )

            // 1. Create tables if they do not exist
            for ((_, query) in tableCreateQueries) {
                database.execSQL(query)
            }

            // 2. Add any missing columns to existing tables
            for ((tableName, expectedColumns) in tablesWithColumns) {
                val existingColumns = mutableSetOf<String>()
                try {
                    database.query("PRAGMA table_info(`$tableName`)").use { cursor ->
                        val nameIndex = cursor.getColumnIndex("name")
                        if (nameIndex != -1) {
                            while (cursor.moveToNext()) {
                                existingColumns.add(cursor.getString(nameIndex).lowercase())
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AppDatabase", "Error reading table_info for $tableName", e)
                }

                for ((colName, colDef) in expectedColumns) {
                    if (!existingColumns.contains(colName.lowercase())) {
                        try {
                            database.execSQL("ALTER TABLE `$tableName` ADD COLUMN `$colName` $colDef")
                            android.util.Log.d("AppDatabase", "Successfully added column $colName to table $tableName")
                        } catch (e: Exception) {
                            android.util.Log.e("AppDatabase", "Error adding column $colName to table $tableName: ${e.message}")
                        }
                    }
                }
            }

            // 3. Seed/ensure default Super Admin user (Lintang Senja)
            seedInitialUsers(database)
        }

        private fun seedInitialUsers(database: SupportSQLiteDatabase) {
            try {
                database.execSQL(
                    "INSERT OR REPLACE INTO `users` (`username`, `password`, `role`, `fullName`, `createdAt`, `photoUrl`) " +
                    "VALUES ('lintang', 'lintangku', 'super_admin', 'Lintang Senja', ${System.currentTimeMillis()}, '')"
                )
                database.execSQL(
                    "INSERT OR IGNORE INTO `users` (`username`, `password`, `role`, `fullName`, `createdAt`, `photoUrl`) " +
                    "VALUES ('admin', 'admin123', 'super_admin', 'Super Admin', ${System.currentTimeMillis()}, '')"
                )
                database.execSQL(
                    "INSERT OR IGNORE INTO `users` (`username`, `password`, `role`, `fullName`, `createdAt`, `photoUrl`) " +
                    "VALUES ('siswa', 'siswa19', 'siswa', 'Siswa Lunaris', ${System.currentTimeMillis()}, '')"
                )
            } catch (e: Exception) {
                android.util.Log.e("AppDatabase", "Error seeding initial default users in AppDatabase", e)
            }
            try {
                database.execSQL(
                    "UPDATE `profile` SET `nip` = '199804192025211035' WHERE `nip` = '19980419202511035' OR `nip` = '' OR `nip` IS NULL"
                )
            } catch (e: Exception) {
                android.util.Log.e("AppDatabase", "Error updating default profile NIP in AppDatabase", e)
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val migrations = (1..30).flatMap { start ->
                    (start + 1..31).map { end ->
                        object : Migration(start, end) {
                            override fun migrate(database: SupportSQLiteDatabase) {
                                migrateDatabaseToLatest(database)
                            }
                        }
                    }
                }.toTypedArray()

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gudang_sman_database"
                )
                    .addMigrations(*migrations)
                    .fallbackToDestructiveMigration()
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            seedInitialUsers(db)
                        }
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            seedInitialUsers(db)
                        }
                    })
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
