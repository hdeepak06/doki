package com.doki.ai.feature.memory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doki.ai.domain.model.Memory
import com.doki.ai.domain.repository.MemoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MemoryViewModel(
    private val memoryRepository: MemoryRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val rawMemories = memoryRepository.getAllMemories()

    val filteredMemories: StateFlow<List<Memory>> = combine(
        rawMemories,
        _searchQuery,
        _selectedCategory
    ) { memories, query, category ->
        memories.filter { mem ->
            val matchesQuery = query.isBlank() || mem.key.contains(query, ignoreCase = true) || mem.value.contains(query, ignoreCase = true)
            val matchesCategory = category == "All" || mem.category.equals(category, ignoreCase = true)
            matchesQuery && matchesCategory
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun addMemory(key: String, value: String, category: String) {
        if (key.isBlank() || value.isBlank()) return
        viewModelScope.launch {
            memoryRepository.saveMemory(
                Memory(
                    key = key.trim(),
                    value = value.trim(),
                    category = category.trim()
                )
            )
        }
    }

    fun deleteMemory(id: String) {
        viewModelScope.launch {
            memoryRepository.deleteMemory(id)
        }
    }

    fun syncCloud() {
        viewModelScope.launch {
            memoryRepository.syncWithCloud()
        }
    }
}

