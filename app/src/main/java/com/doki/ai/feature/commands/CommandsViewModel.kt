package com.doki.ai.feature.commands

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doki.ai.core.ai.tools.AndroidToolRegistry
import com.doki.ai.domain.model.CustomCommand
import com.doki.ai.domain.repository.CommandRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class CommandsViewModel(
    private val commandRepository: CommandRepository,
    private val toolRegistry: AndroidToolRegistry
) : ViewModel() {

    val commands: StateFlow<List<CustomCommand>> = commandRepository.getAllCommands()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun createCommand(phrase: String, toolName: String, paramsJson: String) {
        if (phrase.isBlank() || toolName.isBlank()) return
        viewModelScope.launch {
            val payload = JSONObject().apply {
                put("tool", toolName)
                put("params", if (paramsJson.isNotBlank()) JSONObject(paramsJson) else JSONObject())
            }.toString()

            commandRepository.saveCommand(
                CustomCommand(
                    triggerPhrase = phrase.trim(),
                    actionType = "tool",
                    actionPayload = payload
                )
            )
        }
    }

    fun executeCommand(command: CustomCommand) {
        viewModelScope.launch {
            try {
                val json = JSONObject(command.actionPayload)
                val toolName = json.getString("tool")
                val paramsObj = json.optJSONObject("params") ?: JSONObject()
                val argsMap = mutableMapOf<String, Any?>()
                val keys = paramsObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    argsMap[k] = paramsObj.get(k)
                }
                toolRegistry.executeTool(toolName, argsMap)
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    fun deleteCommand(command: CustomCommand) {
        viewModelScope.launch {
            commandRepository.deleteCommand(command)
        }
    }
}

