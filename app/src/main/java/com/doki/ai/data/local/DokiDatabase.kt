package com.doki.ai.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.doki.ai.data.local.dao.ConversationDao
import com.doki.ai.data.local.dao.CustomCommandDao
import com.doki.ai.data.local.dao.MemoryDao
import com.doki.ai.data.local.dao.MessageDao
import com.doki.ai.data.local.dao.ReminderDao
import com.doki.ai.data.local.entity.ConversationEntity
import com.doki.ai.data.local.entity.CustomCommandEntity
import com.doki.ai.data.local.entity.MemoryEntity
import com.doki.ai.data.local.entity.MessageEntity
import com.doki.ai.data.local.entity.ReminderEntity

@Database(
    entities = [
        MemoryEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
        ReminderEntity::class,
        CustomCommandEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DokiDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun reminderDao(): ReminderDao
    abstract fun customCommandDao(): CustomCommandDao

    companion object {
        private const val DATABASE_NAME = "doki_database.db"

        @Volatile
        private var INSTANCE: DokiDatabase? = null

        fun getInstance(context: Context): DokiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DokiDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

