package com.campus.lostfound.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.lostfound.data.model.User
import com.campus.lostfound.data.repository.AuthRepository
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class ProfileEditState(
    val name: String = "",
    val phoneNumber: String = "",
    val photoUri: Uri? = null,
    val photoUrl: String = "",
    val nameError: String? = null,
    val phoneError: String? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

class ProfileViewModel(
    private val context: Context,
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {
    
    private val _editState = MutableStateFlow(ProfileEditState())
    val editState: StateFlow<ProfileEditState> = _editState.asStateFlow()
    
    private val storage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference
    
    fun loadUserProfile(user: User) {
        _editState.value = _editState.value.copy(
            name = user.name,
            phoneNumber = user.phoneNumber,
            photoUrl = user.photoUrl
        )
    }
    
    fun setName(name: String) {
        _editState.value = _editState.value.copy(
            name = name,
            nameError = if (name.isBlank()) "Name is required" else null
        )
    }
    
    fun setPhoneNumber(phone: String) {
        _editState.value = _editState.value.copy(
            phoneNumber = phone,
            phoneError = validatePhoneNumber(phone)
        )
    }
    
    fun setPhotoUri(uri: Uri?) {
        _editState.value = _editState.value.copy(photoUri = uri)
    }
    
    private fun validatePhoneNumber(phone: String): String? {
        if (phone.isBlank()) return "Phone number is required"
        
        val cleanPhone = phone.replace(Regex("[^0-9+]"), "")
        return when {
            cleanPhone.length < 10 -> "Phone number too short"
            !cleanPhone.matches(Regex("^(\\+62|62|0)[0-9]{9,12}\$")) -> 
                "Invalid Indonesian phone number"
            else -> null
        }
    }
    
    fun updateProfile() {
        val state = _editState.value
        val currentUser = authRepository.currentUser ?: return
        
        // Validate
        val nameError = if (state.name.isBlank()) "Name is required" else null
        val phoneError = validatePhoneNumber(state.phoneNumber)
        
        if (nameError != null || phoneError != null) {
            _editState.value = state.copy(
                nameError = nameError,
                phoneError = phoneError
            )
            return
        }
        
        viewModelScope.launch {
            _editState.value = _editState.value.copy(isLoading = true, error = null)
            
            try {
                // Upload photo if new photo selected
                var photoUrl = state.photoUrl
                if (state.photoUri != null) {
                    photoUrl = uploadProfilePhoto(currentUser.uid, state.photoUri)
                }
                
                // Update Firestore
                val updates = mapOf(
                    "name" to state.name,
                    "phoneNumber" to state.phoneNumber,
                    "photoUrl" to photoUrl
                )
                
                authRepository.updateUserProfile(currentUser.uid, updates)
                    .onSuccess {
                        _editState.value = _editState.value.copy(
                            isLoading = false,
                            isSuccess = true,
                            photoUrl = photoUrl
                        )
                    }
                    .onFailure { error ->
                        _editState.value = _editState.value.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to update profile"
                        )
                    }
            } catch (e: Exception) {
                _editState.value = _editState.value.copy(
                    isLoading = false,
                    error = e.message ?: "An error occurred"
                )
            }
        }
    }
    
    private suspend fun uploadProfilePhoto(userId: String, uri: Uri): String {
        val fileName = "profile_${userId}_${UUID.randomUUID()}.jpg"
        val imageRef = storageRef.child("profile_photos/$fileName")
        
        // Upload file
        imageRef.putFile(uri).await()
        
        // Get download URL
        return imageRef.downloadUrl.await().toString()
    }
    
    fun clearSuccess() {
        _editState.value = _editState.value.copy(isSuccess = false)
    }
    
    fun clearError() {
        _editState.value = _editState.value.copy(error = null)
    }
}
