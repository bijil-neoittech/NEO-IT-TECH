package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val password: String,
    val role: String, // "ADMIN", "TECHNICIAN", "CUSTOMER"
    val fullName: String,
    val phone: String,
    val email: String
)
