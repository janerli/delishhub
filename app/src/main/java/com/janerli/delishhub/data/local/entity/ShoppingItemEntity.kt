package com.janerli.delishhub.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "shopping_items",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["userId", "isChecked"])
    ]
)
data class ShoppingItemEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val amount: Double?,
    val unit: String?,
    val qtyText: String?,
    val isChecked: Boolean,
    val createdAt: Long,
    val updatedAt: Long,

    /**
     * 0=SYNCED, 1=CREATED, 2=UPDATED, 3=DELETED
     */
    val syncStatus: Int = 0
)
