package com.janerli.delishhub.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserLocalEntity(
    @PrimaryKey val id: String, // firebase uid или "guest"
    val name: String? = null,
    val email: String? = null,
    val role: String = "GUEST", // GUEST/USER/ADMIN
    val avatarUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
