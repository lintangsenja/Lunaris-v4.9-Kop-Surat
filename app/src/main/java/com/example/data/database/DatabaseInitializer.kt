package com.example.data.database

import android.content.Context
import android.util.Log
import com.example.data.repository.SettingsRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object DatabaseInitializer {

    fun initialize(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val firestore = FirebaseFirestore.getInstance()

                // 1. Seed Units collection in Firestore if empty
                val unitsRef = firestore.collection("units")
                val unitsSnapshot = unitsRef.get().await()
                if (unitsSnapshot.isEmpty) {
                    val defaultUnits = listOf(
                        "Pcs", "Unit", "Set", "Box", "Pak", "Dus", "Lembar", "Rim",
                        "Batang", "Bungkus", "Botol", "Kg", "Meter", "Roll", "Liter"
                    )
                    for (unit in defaultUnits) {
                        val docId = unit.lowercase().replace(" ", "_")
                        unitsRef.document(docId).set(mapOf("name" to unit), SetOptions.merge())
                    }
                    Log.d("DatabaseInitializer", "Seeded units to Firestore cloud successfully.")
                }

                // 2. Seed Peripherals collection in Firestore if empty
                val peripheralsRef = firestore.collection("peripherals")
                val peripheralsSnapshot = peripheralsRef.get().await()
                if (peripheralsSnapshot.isEmpty) {
                    val defaultPeripherals = listOf(
                        "🧠 RAM",
                        "💽 Internal Storage",
                        "💽 External Storage",
                        "⌨️ Mouse & Keyboard",
                        "⚡ UPS & PSU",
                        "🔌 Peripheral Lainnya"
                    )
                    for (item in defaultPeripherals) {
                        val docId = item.lowercase().replace(" ", "_")
                        peripheralsRef.document(docId).set(mapOf("name" to item), SetOptions.merge())
                    }
                    Log.d("DatabaseInitializer", "Seeded peripherals to Firestore cloud successfully.")
                }

                // 3. Seed Sumber Dana & Kondisi local defaults
                val settingsRepo = SettingsRepository(context)
                val currentSumberDana = settingsRepo.getSumberDana()
                if (currentSumberDana.isEmpty()) {
                    val defaultSumberDana = listOf(
                        "BOS", "BOP", "Bantuan Komite", "Bantuan Pempus", "Bantuan Pemrov", "Hibah"
                    ).sorted()
                    settingsRepo.saveSumberDana(defaultSumberDana)
                }

                val currentKondisi = settingsRepo.getKondisi()
                if (currentKondisi.isEmpty()) {
                    val defaultKondisi = listOf(
                        "Baik", "Rusak", "Pemeliharaan", "Expired"
                    ).sorted()
                    settingsRepo.saveKondisi(defaultKondisi)
                }

                Log.d("DatabaseInitializer", "Cloud Firestore initialization complete.")
            } catch (e: Exception) {
                Log.e("DatabaseInitializer", "Error during Cloud Firestore initialization", e)
            }
        }
    }
}
