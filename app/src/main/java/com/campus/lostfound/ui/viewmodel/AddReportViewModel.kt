package com.campus.lostfound.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.lostfound.data.model.Category
import com.campus.lostfound.data.model.ItemType
import com.campus.lostfound.data.model.LostFoundItem
import com.campus.lostfound.data.repository.LostFoundRepository
import com.campus.lostfound.data.repository.UserRepository
import com.campus.lostfound.util.WhatsAppUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import com.google.firebase.auth.FirebaseAuth

data class FormErrors(
    val itemName: String? = null,
    val category: String? = null,
    val location: String? = null,
    val whatsappNumber: String? = null,
    val imageUri: String? = null
)

data class AddReportUiState(
    val itemType: ItemType = ItemType.LOST,
    val itemName: String = "",
    val category: Category = Category.OTHER,
    val location: String = "",
    val description: String = "",
    val whatsappNumber: String = "",
    val imageUri: Uri? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val formErrors: FormErrors = FormErrors()
)

class AddReportViewModel(
    private val context: Context,
    private val repository: LostFoundRepository = LostFoundRepository(context),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AddReportUiState())
    val uiState: StateFlow<AddReportUiState> = _uiState.asStateFlow()
    
    private val auth = FirebaseAuth.getInstance()
    
    fun setItemType(type: ItemType) {
        _uiState.value = _uiState.value.copy(itemType = type)
    }
    
    fun setItemName(name: String) {
        _uiState.value = _uiState.value.copy(
            itemName = name,
            formErrors = _uiState.value.formErrors.copy(
                itemName = if (name.isBlank()) "Nama barang wajib diisi" else null
            )
        )
    }
    
    fun setCategory(category: Category) {
        _uiState.value = _uiState.value.copy(
            category = category,
            formErrors = _uiState.value.formErrors.copy(
                category = null
            )
        )
    }
    
    fun setLocation(location: String) {
        _uiState.value = _uiState.value.copy(
            location = location,
            formErrors = _uiState.value.formErrors.copy(
                location = if (location.isBlank()) "Lokasi wajib diisi" else null
            )
        )
    }
    
    fun setDescription(description: String) {
        _uiState.value = _uiState.value.copy(
            description = description.take(500)
        )
    }
    
    fun setWhatsAppNumber(number: String) {
        // Auto-format nomor saat user input
        val cleanNumber = number.replace(Regex("[^0-9+]"), "")
        val errorMsg = when {
            cleanNumber.isBlank() -> "Nomor WhatsApp wajib diisi"
            !WhatsAppUtil.isValidIndonesianPhoneNumber(cleanNumber) -> 
                "Format nomor tidak valid (gunakan 08xxx atau 628xxx)"
            else -> null
        }
        _uiState.value = _uiState.value.copy(
            whatsappNumber = cleanNumber,
            formErrors = _uiState.value.formErrors.copy(
                whatsappNumber = errorMsg
            )
        )
    }
    
    fun setImageUri(uri: Uri?) {
        _uiState.value = _uiState.value.copy(
            imageUri = uri,
            formErrors = _uiState.value.formErrors.copy(
                imageUri = if (uri == null) "Foto barang wajib diupload" else null
            )
        )
    }
    
    fun validateForm(): Boolean {
        val state = _uiState.value
        val cleanNumber = state.whatsappNumber.replace(Regex("[^0-9]"), "")
        return state.itemName.isNotBlank() &&
                state.location.isNotBlank() &&
                cleanNumber.isNotBlank() &&
                WhatsAppUtil.isValidIndonesianPhoneNumber(state.whatsappNumber) &&
                state.imageUri != null
    }
    
    fun submitReport(onSuccess: () -> Unit) {
        if (!validateForm()) {
            val cleanNumber = _uiState.value.whatsappNumber.replace(Regex("[^0-9]"), "")
            val errorMsg = when {
                _uiState.value.itemName.isBlank() -> "Nama barang wajib diisi"
                _uiState.value.location.isBlank() -> "Lokasi wajib diisi"
                cleanNumber.isBlank() -> "Nomor WhatsApp wajib diisi"
                !WhatsAppUtil.isValidIndonesianPhoneNumber(_uiState.value.whatsappNumber) -> 
                    "Format nomor WhatsApp tidak valid. Gunakan format: 08123456789 atau 628123456789"
                _uiState.value.imageUri == null -> "Foto barang wajib diupload"
                else -> "Harap lengkapi semua field yang wajib"
            }
            _uiState.value = _uiState.value.copy(errorMessage = errorMsg)
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            // Compress image before upload
            val imageUri = _uiState.value.imageUri
            val compressedUri = if (imageUri != null) {
                try {
                    val compressed = com.campus.lostfound.util.ImageCompressor.compressImage(context, imageUri)
                    if (compressed == null) {
                        // Compression failed, show error and abort
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Gagal memproses gambar. Silakan coba foto lain atau pilih dari galeri."
                        )
                        return@launch
                    }
                    compressed
                } catch (e: Exception) {
                    Log.e("AddReportViewModel", "Image compression failed", e)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Gagal memproses gambar: ${e.message}. Silakan coba lagi."
                    )
                    return@launch
                }
            } else {
                null
            }

            // Format nomor ke format internasional sebelum disimpan
            val formattedNumber = WhatsAppUtil.formatPhoneNumber(_uiState.value.whatsappNumber)

            // Load user profile for userName and photoUrl
            val currentUserId = auth.currentUser?.uid ?: ""
            val userProfile = userRepository.getCurrentUserProfile().getOrNull()
            
            val userName = userProfile?.name 
                ?: auth.currentUser?.displayName 
                ?: "User"
            
            val userPhotoUrl = userProfile?.photoUrl 
                ?: auth.currentUser?.photoUrl?.toString() 
                ?: ""

            val item = LostFoundItem(
                userId = currentUserId,
                userName = userName,
                userPhotoUrl = userPhotoUrl,
                type = _uiState.value.itemType,
                itemName = _uiState.value.itemName,
                category = _uiState.value.category,
                location = _uiState.value.location,
                description = _uiState.value.description,
                whatsappNumber = formattedNumber
            )

            Log.d("AddReportViewModel", "Submitting report. Current auth uid=${FirebaseAuth.getInstance().currentUser?.uid}")
            val result = try {
                repository.addItem(item, compressedUri)
            } catch (ex: Exception) {
                Log.e("AddReportViewModel", "addItem threw", ex)
                Result.failure<String>(ex)
            }

            result.fold(
                onSuccess = {
                    // Update user stats
                    if (currentUserId.isNotEmpty()) {
                        userRepository.updateStats(
                            userId = currentUserId,
                            incrementReports = 1,
                            incrementFound = if (_uiState.value.itemType == ItemType.FOUND) 1 else 0
                        )
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true
                    )
                    onSuccess()
                },
                onFailure = { error ->
                    Log.e("AddReportViewModel", "addItem failed: ${error.message}", error)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Gagal mengirim laporan"
                    )
                }
            )
        }
    }
    
    fun resetState() {
        _uiState.value = AddReportUiState()
    }
}

