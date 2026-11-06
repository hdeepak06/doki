package com.doki.ai.data.repository

import com.doki.ai.data.local.dao.CustomCommandDao
import com.doki.ai.data.local.entity.CustomCommandEntity
import com.doki.ai.domain.model.CustomCommand
import com.doki.ai.domain.repository.CommandRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CommandRepositoryImpl(
    private val commandDao: CustomCommandDao
) : CommandRepository {

    override fun getAllCommands(): Flow<List<CustomCommand>> {
        return commandDao.getAllCommands().map { list -> list.map { it.toDomain() } }
    }

    override fun getActiveCommands(): Flow<List<CustomCommand>> {
        return commandDao.getAllActiveCommands().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun findMatchingCommand(phrase: String): CustomCommand? {
        return commandDao.findMatchingCommand(phrase)?.toDomain()
    }

    override suspend fun saveCommand(command: CustomCommand) {
        commandDao.insertCommand(CustomCommandEntity.fromDomain(command))
    }

    override suspend fun deleteCommand(command: CustomCommand) {
        commandDao.deleteCommand(CustomCommandEntity.fromDomain(command))
    }
}

