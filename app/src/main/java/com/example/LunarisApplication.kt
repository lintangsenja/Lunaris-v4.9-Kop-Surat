package com.example

import android.app.Application
import com.example.data.database.DatabaseInitializer
import com.example.data.network.FirebaseManager

class LunarisApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase cleanly and safely at application startup
        FirebaseManager.initialize(this)
        DatabaseInitializer.initialize(this)
    }
}
