package com.doki.ai.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.doki.ai.data.local.entity.CustomCommandEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomCommandDao {
    @Query("SELECT * FROM custom_commands WHERE isActive = 1")
    fun getAllActiveCommands(): Flow<List<CustomCommandEntity>>

    @Query("SELECT * FROM custom_commands ORDER BY createdAt DESC")
    fun getAllCommands(): Flow<List<CustomCommandEntity>>

    @Query("SELECT * FROM custom_commands WHERE triggerPhrase LIKE '%' || :phrase || '%' LIMIT 1")
    suspend fun findMatchingCommand(phrase: String): CustomCommandEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommand(command: CustomCommandEntity)

    @Update
    suspend fun updateCommand(command: CustomCommandEntity)

    @Delete
    suspend fun deleteCommand(command: CustomCommandEntity)
}

