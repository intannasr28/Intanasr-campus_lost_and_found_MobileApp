package com.campus.lostfound.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.lostfound.data.model.User
import com.campus.lostfound.data.repository.AuthRepository
import com.campus.lostfound.util.UserCache
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val currentUser: User? = null,
    val error: String? = null,
    val isSuccess: Boolean = false
)

data class LoginFormState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null
)

data class RegisterFormState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val phoneNumber: String = "",
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val phoneError: String? = null
)

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {
    
    private val _authState = MutableStateFlow(AuthUiState())
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()
    
    private val _loginForm = MutableStateFlow(LoginFormState())
    val loginForm: StateFlow<LoginFormState> = _loginForm.asStateFlow()
    
    private val _registerForm = MutableStateFlow(RegisterFormState())
    val registerForm: StateFlow<RegisterFormState> = _registerForm.asStateFlow()
    
    init {
        observeAuthState()
        loadCurrentUser()
    }
    
    private var loadUserJob: Job? = null
    
    private fun observeAuthState() {
        viewModelScope.launch {
            repository.authState.collect { firebaseUser ->
                if (firebaseUser != null) {
                    loadCurrentUser()
                } else {
                    _authState.value = _authState.value.copy(currentUser = null)
                }
            }
        }
    }
    
    private fun loadCurrentUser() {
        val firebaseUser = repository.currentUser ?: return
        
        // ✅ Cancel previous load job (debounce)
        loadUserJob?.cancel()
        loadUserJob = viewModelScope.launch {
            delay(300) // ✅ Debounce 300ms
            repository.getUserById(firebaseUser.uid)
                .onSuccess { user ->
                    // ✅ Save to cache
                    UserCache.set(user)
                    _authState.value = _authState.value.copy(currentUser = user)
                }
                .onFailure { error ->
                    _authState.value = _authState.value.copy(
                        error = error.message ?: "Failed to load user"
                    )
                }
        }
    }
    
    // Login Form Methods
    fun setLoginEmail(email: String) {
        _loginForm.value = _loginForm.value.copy(
            email = email,
            emailError = validateEmail(email)
        )
    }
    
    fun setLoginPassword(password: String) {
        _loginForm.value = _loginForm.value.copy(
            password = password,
            passwordError = if (password.isEmpty()) "Password required" else null
        )
    }
    
    // Register Form Methods
    fun setRegisterName(name: String) {
        _registerForm.value = _registerForm.value.copy(
            name = name,
            nameError = if (name.isBlank()) "Name is required" else null
        )
    }
    
    fun setRegisterEmail(email: String) {
        _registerForm.value = _registerForm.value.copy(
            email = email,
            emailError = validateEmail(email)
        )
    }
    
    fun setRegisterPassword(password: String) {
        _registerForm.value = _registerForm.value.copy(
            password = password,
            passwordError = validatePassword(password)
        )
    }
    
    fun setRegisterConfirmPassword(confirmPassword: String) {
        _registerForm.value = _registerForm.value.copy(
            confirmPassword = confirmPassword,
            confirmPasswordError = if (confirmPassword != _registerForm.value.password) {
                "Passwords do not match"
            } else null
        )
    }
    
    fun setRegisterPhone(phone: String) {
        _registerForm.value = _registerForm.value.copy(
            phoneNumber = phone,
            phoneError = validatePhoneNumber(phone)
        )
    }
    
    // Validation helpers
    private fun validateEmail(email: String): String? {
        return when {
            email.isBlank() -> "Email is required"
            !email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\$")) -> 
                "Invalid email format"
            else -> null
        }
    }
    
    private fun validatePassword(password: String): String? {
        return when {
            password.isEmpty() -> "Password is required"
            password.length < 6 -> "Password must be at least 6 characters"
            else -> null
        }
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
    
    // Login
    fun login() {
        val form = _loginForm.value
        
        // Validate
        val emailError = validateEmail(form.email)
        val passwordError = validatePassword(form.password)
        
        if (emailError != null || passwordError != null) {
            _loginForm.value = form.copy(
                emailError = emailError,
                passwordError = passwordError
            )
            return
        }
        
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            
            repository.loginWithEmail(form.email, form.password)
                .onSuccess { user ->
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        currentUser = user,
                        isSuccess = true
                    )
                }
                .onFailure { error ->
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = getReadableError(error)
                    )
                }
        }
    }
    
    // Register
    fun register() {
        val form = _registerForm.value
        
        // Validate all fields
        val nameError = if (form.name.isBlank()) "Name is required" else null
        val emailError = validateEmail(form.email)
        val passwordError = validatePassword(form.password)
        val confirmError = if (form.confirmPassword != form.password) "Passwords do not match" else null
        val phoneError = validatePhoneNumber(form.phoneNumber)
        
        if (nameError != null || emailError != null || passwordError != null || 
            confirmError != null || phoneError != null) {
            _registerForm.value = form.copy(
                nameError = nameError,
                emailError = emailError,
                passwordError = passwordError,
                confirmPasswordError = confirmError,
                phoneError = phoneError
            )
            return
        }
        
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            
            repository.registerWithEmail(
                email = form.email,
                password = form.password,
                name = form.name,
                phoneNumber = form.phoneNumber
            )
                .onSuccess { user ->
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        currentUser = user,
                        isSuccess = true
                    )
                }
                .onFailure { error ->
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = getReadableError(error)
                    )
                }
        }
    }
    
    // Login with Google
    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            
            repository.loginWithGoogle(idToken)
                .onSuccess { user ->
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        currentUser = user,
                        isSuccess = true
                    )
                }
                .onFailure { error ->
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = getReadableError(error)
                    )
                }
        }
    }
    
    // Logout
    fun logout() {
        viewModelScope.launch {
            // ✅ Clear user cache on logout
            UserCache.clear()
            loadUserJob?.cancel()
            repository.logout()
            _authState.value = AuthUiState() // Reset state
        }
    }
    
    // Send password reset email
    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            
            repository.sendPasswordResetEmail(email)
                .onSuccess {
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        isSuccess = true
                    )
                }
                .onFailure { error ->
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = getReadableError(error)
                    )
                }
        }
    }
    
    // Clear error
    fun clearError() {
        _authState.value = _authState.value.copy(error = null)
    }
    
    // Clear success
    fun clearSuccess() {
        _authState.value = _authState.value.copy(isSuccess = false)
    }
    
    // Get readable error message
    private fun getReadableError(error: Throwable): String {
        val message = error.message ?: ""
        return when {
            // Firebase Auth error codes
            message.contains("INVALID_LOGIN_CREDENTIALS") || 
            message.contains("ERROR_WRONG_PASSWORD") ||
            message.contains("wrong-password") -> 
                "Password salah. Silakan coba lagi."
            
            message.contains("ERROR_USER_NOT_FOUND") ||
            message.contains("user-not-found") ||
            message.contains("no user record") ->
                "Akun tidak ditemukan. Silakan daftar terlebih dahulu."
            
            message.contains("ERROR_INVALID_EMAIL") ||
            message.contains("invalid-email") ->
                "Format email tidak valid."
            
            message.contains("ERROR_EMAIL_ALREADY_IN_USE") ||
            message.contains("email-already-in-use") ->
                "Email sudah terdaftar. Silakan login atau gunakan email lain."
            
            message.contains("ERROR_WEAK_PASSWORD") ||
            message.contains("weak-password") ->
                "Password terlalu lemah. Minimal 6 karakter."
            
            message.contains("ERROR_TOO_MANY_REQUESTS") ||
            message.contains("too-many-requests") ->
                "Terlalu banyak percobaan. Silakan coba lagi nanti."
            
            message.contains("network") ||
            message.contains("Network") ->
                "Koneksi internet bermasalah. Periksa koneksi Anda."
            
            message.contains("timeout") ->
                "Waktu koneksi habis. Coba lagi."
            
            else -> "Login gagal. Periksa email dan password Anda."
        }
    }
}
