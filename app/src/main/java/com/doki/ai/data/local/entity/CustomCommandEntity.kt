package com.doki.ai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.doki.ai.domain.model.CustomCommand

@Entity(tableName = "custom_commands")
data class CustomCommandEntity(
    @PrimaryKey
    val id: String,
    val userId: String?,
    val triggerPhrase: String,
    val actionType: String,
    val actionPayload: String,
    val isActive: Boolean,
    val createdAt: Long
) {
    fun toDomain(): CustomCommand = CustomCommand(
        id = id,
        userId = userId,
        triggerPhrase = triggerPhrase,
        actionType = actionType,
        actionPayload = actionPayload,
        isActive = isActive,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(c: CustomCommand): CustomCommandEntity = CustomCommandEntity(
            id = c.id,
            userId = c.userId,
            triggerPhrase = c.triggerPhrase,
            actionType = c.actionType,
            actionPayload = c.actionPayload,
            isActive = c.isActive,
            createdAt = c.createdAt
        )
    }
}

