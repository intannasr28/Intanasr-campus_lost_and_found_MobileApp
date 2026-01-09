package com.campus.lostfound.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.lostfound.data.LocalHistoryRepository
import com.campus.lostfound.data.model.LostFoundItem
import com.campus.lostfound.data.repository.LostFoundRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ActivityViewModel(
    private val context: Context,
    private val repository: LostFoundRepository = LostFoundRepository(context)
) : ViewModel() {
    
    private val _myReports = MutableStateFlow<List<LostFoundItem>>(emptyList())
    val myReports: StateFlow<List<LostFoundItem>> = _myReports.asStateFlow()

    // Ubah ke CompletedReport untuk menyimpan info tanggal selesai
    private val _historyWithDate = MutableStateFlow<List<LocalHistoryRepository.CompletedReport>>(emptyList())
    val historyWithDate: StateFlow<List<LocalHistoryRepository.CompletedReport>> = _historyWithDate.asStateFlow()
    
    // Keep legacy flow for backward compatibility
    private val _history = MutableStateFlow<List<LostFoundItem>>(emptyList())
    val history: StateFlow<List<LostFoundItem>> = _history.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    init {
        loadMyReports()
        loadMyHistory()
    }
    
    fun loadMyReports() {
        viewModelScope.launch {
            _isLoading.value = true
            val userId = repository.getCurrentUserId()
            repository.getUserItems(userId).collect { items ->
                _myReports.value = items
                _isLoading.value = false
            }
        }
    }

    fun loadMyHistory() {
        viewModelScope.launch {
            // Load dengan info tanggal selesai
            repository.getCompletedReportsWithDate().collect { completedReports ->
                val userId = repository.getCurrentUserId()
                // Filter by current user
                val userReports = completedReports.filter { it.item.userId == userId }
                _historyWithDate.value = userReports
                _history.value = userReports.map { it.item }
            }
        }
    }
    
    fun deleteReport(item: LostFoundItem, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            val result = repository.deleteItem(item.id, item.imageStoragePath)
            
            result.fold(
                onSuccess = {
                    _isLoading.value = false
                    onSuccess()
                },
                onFailure = { error ->
                    _isLoading.value = false
                    _errorMessage.value = error.message ?: "Gagal menghapus laporan"
                }
            )
        }
    }
    
    /**
     * Hapus dari riwayat lokal (hanya untuk laporan yang sudah selesai)
     */
    fun deleteFromHistory(itemId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            val success = repository.deleteFromLocalHistory(itemId)
            
            if (success) {
                _isLoading.value = false
                onSuccess()
            } else {
                _isLoading.value = false
                _errorMessage.value = "Gagal menghapus riwayat"
            }
        }
    }
    
    fun markAsCompleted(item: LostFoundItem, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            val result = repository.markAsCompleted(item.id)
            
            result.fold(
                onSuccess = {
                    _isLoading.value = false
                    // Reload both active reports and history to reflect changes immediately
                    loadMyReports()
                    loadMyHistory()
                    onSuccess()
                },
                onFailure = { error ->
                    _isLoading.value = false
                    _errorMessage.value = error.message ?: "Gagal memperbarui laporan"
                }
            )
        }
    }
    
    fun updateReport(
        itemId: String,
        itemName: String? = null,
        category: com.campus.lostfound.data.model.Category? = null,
        location: String? = null,
        description: String? = null,
        whatsappNumber: String? = null,
        imageUri: android.net.Uri? = null,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            val result = repository.updateItem(
                itemId = itemId,
                itemName = itemName,
                category = category,
                location = location,
                description = description,
                whatsappNumber = whatsappNumber,
                imageUri = imageUri
            )
            
            result.fold(
                onSuccess = {
                    _isLoading.value = false
                    onSuccess()
                },
                onFailure = { error ->
                    _isLoading.value = false
                    _errorMessage.value = error.message ?: "Gagal memperbarui laporan"
                }
            )
        }
    }
}

