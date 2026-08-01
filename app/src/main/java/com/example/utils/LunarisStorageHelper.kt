package com.example.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

data class ExportFileResult(
    val file: File?,
    val uri: Uri,
    val displayPath: String,
    val mimeType: String,
    val filename: String
)

object LunarisStorageHelper {

    /**
     * Ensures dedicated "Lunaris" folder structure exists on device storage
     */
    fun ensureLunarisFoldersExist(context: Context) {
        try {
            // 1. Primary Public Documents/Lunaris folder
            val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val lunarisDocs = File(docsDir, "Lunaris/Unduh Laporan")
            if (!lunarisDocs.exists()) {
                lunarisDocs.mkdirs()
            }

            // 2. Public Downloads/Lunaris folder
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val lunarisDownloads = File(downloadsDir, "Lunaris/Unduh Laporan")
            if (!lunarisDownloads.exists()) {
                lunarisDownloads.mkdirs()
            }

            // 3. Root Storage Lunaris folder (/storage/emulated/0/Lunaris)
            val rootStorage = Environment.getExternalStorageDirectory()
            val lunarisRoot = File(rootStorage, "Lunaris/Unduh Laporan")
            if (!lunarisRoot.exists()) {
                lunarisRoot.mkdirs()
            }

            // 4. App external files folder as fallback
            val appExternal = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            val appLunaris = File(appExternal, "Lunaris/Unduh Laporan")
            if (!appLunaris.exists()) {
                appLunaris.mkdirs()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Saves exported byte contents (.pdf, .xlsx, .docx) directly into the Lunaris folder.
     * Compatible with Scoped Storage (Android 10+) and legacy storage permissions.
     */
    fun saveExportFile(
        context: Context,
        subfolderName: String,
        filename: String,
        bytes: ByteArray,
        mimeType: String
    ): ExportFileResult? {
        ensureLunarisFoldersExist(context)

        var savedFile: File? = null
        var contentUri: Uri? = null
        var displayPath = "Penyimpanan Internal/Documents/Lunaris/Unduh Laporan/$subfolderName/"

        try {
            // Try saving via MediaStore for Android 10+ (API 29+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val relativePath = "${Environment.DIRECTORY_DOCUMENTS}/Lunaris/Unduh Laporan/$subfolderName"
                
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val collectionUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val itemUri = context.contentResolver.insert(collectionUri, values)

                if (itemUri != null) {
                    context.contentResolver.openOutputStream(itemUri)?.use { os ->
                        os.write(bytes)
                    }

                    values.clear()
                    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    context.contentResolver.update(itemUri, values, null, null)

                    contentUri = itemUri
                    displayPath = "Penyimpanan Utama/Documents/Lunaris/Unduh Laporan/$subfolderName/"
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Always save a physical File copy as well to guarantee FileProvider URI generation & physical path access
        try {
            val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            var targetFolder = File(docsDir, "Lunaris/Unduh Laporan/$subfolderName")
            
            var canWrite = false
            try {
                if (!targetFolder.exists()) targetFolder.mkdirs()
                val testFile = File(targetFolder, ".nomedia_test")
                if (testFile.createNewFile()) {
                    testFile.delete()
                    canWrite = true
                }
            } catch (e: Exception) {
                canWrite = false
            }

            if (!canWrite) {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                targetFolder = File(downloadsDir, "Lunaris/Unduh Laporan/$subfolderName")
                try {
                    if (!targetFolder.exists()) targetFolder.mkdirs()
                    val testFile = File(targetFolder, ".nomedia_test")
                    if (testFile.createNewFile()) {
                        testFile.delete()
                        canWrite = true
                    }
                } catch (e: Exception) {
                    canWrite = false
                }
            }

            if (!canWrite) {
                targetFolder = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Lunaris/Unduh Laporan/$subfolderName")
                if (!targetFolder.exists()) targetFolder.mkdirs()
                displayPath = "Penyimpanan Aplikasi/Lunaris/Unduh Laporan/$subfolderName/"
            }

            val destFile = File(targetFolder, filename)
            FileOutputStream(destFile).use { fos ->
                fos.write(bytes)
            }
            savedFile = destFile

            if (contentUri == null) {
                contentUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    destFile
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val finalUri = contentUri ?: run {
            savedFile?.let {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", it)
            }
        } ?: return null

        return ExportFileResult(
            file = savedFile,
            uri = finalUri,
            displayPath = displayPath,
            mimeType = mimeType,
            filename = filename
        )
    }

    /**
     * Automatically opens exported document using third-party viewer intent
     */
    fun openFile(context: Context, uri: Uri, mimeType: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Tidak ada aplikasi penampil dokumen terpasang di HP Anda untuk membuka file ini.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Shares document via ACTION_SEND chooser
     */
    fun shareFile(context: Context, uri: Uri, mimeType: String, title: String = "Bagikan Laporan Lunaris") {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, title))
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membagikan file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
