package com.doki.ai.feature.permissions

import androidx.lifecycle.ViewModel
import com.doki.ai.core.permissions.PermissionItem
import com.doki.ai.core.permissions.PermissionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PermissionViewModel : ViewModel() {

    private val _permissions = MutableStateFlow(PermissionManager.getAssistantPermissions())
    val permissions: StateFlow<List<PermissionItem>> = _permissions.asStateFlow()

    fun refreshPermissions() {
        _permissions.value = PermissionManager.getAssistantPermissions()
    }
}

