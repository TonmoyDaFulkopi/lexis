package com.tonmoy.lexi.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [WordEntity::class], version = 1, exportSchema = true)
abstract class LexiDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao

    companion object {
        fun build(context: Context): LexiDatabase =
            Room.databaseBuilder(context, LexiDatabase::class.java, "lexi.db").build()
    }
}
