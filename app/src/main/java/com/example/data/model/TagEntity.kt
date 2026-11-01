package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_tags")
data class TagEntity(
    @PrimaryKey
    val name: String,
    val colorIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
