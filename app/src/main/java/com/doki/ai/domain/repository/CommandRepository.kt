package com.doki.ai.domain.repository

import com.doki.ai.domain.model.CustomCommand
import kotlinx.coroutines.flow.Flow

interface CommandRepository {
    fun getAllCommands(): Flow<List<CustomCommand>>
    fun getActiveCommands(): Flow<List<CustomCommand>>
    suspend fun findMatchingCommand(phrase: String): CustomCommand?
    suspend fun saveCommand(command: CustomCommand)
    suspend fun deleteCommand(command: CustomCommand)
}

