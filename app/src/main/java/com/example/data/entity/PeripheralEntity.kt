package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "peripherals")
data class PeripheralEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val isDemo: Boolean = false
)
