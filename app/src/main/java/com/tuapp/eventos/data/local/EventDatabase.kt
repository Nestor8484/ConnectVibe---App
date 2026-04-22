package com.tuapp.eventos.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tuapp.eventos.data.local.dao.EventDao
import com.tuapp.eventos.data.local.model.EventEntity

@Database(entities = [EventEntity::class], version = 1)
abstract class EventDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
}
