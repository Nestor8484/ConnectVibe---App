package com.tuapp.eventos.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val timestamp: Long,
    val location: String,
    val isPublic: Boolean,
    val ownerId: String,
    val customRolesEnabled: Boolean,
    val finesEnabled: Boolean
)
