package com.janerli.delishhub.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val title: String,
    val icon: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
