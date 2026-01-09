package com.campus.lostfound.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.campus.lostfound.data.repository.UserRepository
import com.campus.lostfound.ui.components.LargeUserAvatar
import kotlinx.coroutines.launch

/**
 * EditProfileScreenSimple
 * Simplified version compatible with Firebase Free Tier
 * No photo upload, uses hybrid Google photo / initial avatar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreenSimple(
    onNavigateBack: () -> Unit
) {
    val userRepository = remember { UserRepository() }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    var name by remember { mutableStateOf("") }
    var nim by remember { mutableStateOf("") }
    var faculty by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var showPhonePublicly by remember { mutableStateOf(false) }
    var showEmailPublicly by remember { mutableStateOf(false) }
    
    var photoUrl by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Load current profile
    LaunchedEffect(Unit) {
        scope.launch {
            userRepository.getCurrentUserProfile().onSuccess { user ->
                name = user.name
                nim = user.nim
                faculty = user.faculty
                department = user.department
                phoneNumber = user.phoneNumber
                photoUrl = user.photoUrl
                showPhonePublicly = user.showPhonePublicly
                showEmailPublicly = user.showEmailPublicly
                isLoading = false
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Profile Photo (Display only - no upload)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LargeUserAvatar(
                        photoUrl = photoUrl,
                        name = name
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (photoUrl.isNotEmpty()) "Google Photo" else "Avatar Initial",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Basic Info Section
                Text(
                    text = "Basic Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display Name") },
                    leadingIcon = {
                        Icon(Icons.Filled.Person, contentDescription = null)
                    },
                    supportingText = {
                        Text("This name will appear when you create reports")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number (Optional)") },
                    leadingIcon = {
                        Icon(Icons.Filled.Phone, contentDescription = null)
                    },
                    placeholder = { Text("081234567890") },
                    supportingText = {
                        Text("Can be used as default when creating reports")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Student Info Section
                Text(
                    text = "Student Information (Optional)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                OutlinedTextField(
                    value = nim,
                    onValueChange = { nim = it },
                    label = { Text("NIM") },
                    leadingIcon = {
                        Icon(Icons.Filled.Badge, contentDescription = null)
                    },
                    placeholder = { Text("2021001234") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = faculty,
                    onValueChange = { faculty = it },
                    label = { Text("Faculty") },
                    leadingIcon = {
                        Icon(Icons.Filled.School, contentDescription = null)
                    },
                    placeholder = { Text("Teknik") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = department,
                    onValueChange = { department = it },
                    label = { Text("Department/Major") },
                    leadingIcon = {
                        Icon(Icons.Filled.Class, contentDescription = null)
                    },
                    placeholder = { Text("Teknik Informatika") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Divider()
                
                // Privacy Settings Section
                Text(
                    text = "Privacy Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = "Control what information is visible when other users view your profile",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Phone Number Privacy
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Show Phone Number",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Display your phone number on public profile",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = showPhonePublicly,
                            onCheckedChange = { showPhonePublicly = it }
                        )
                    }
                }
                
                // Email Privacy
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Show Email",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Display your email on public profile",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = showEmailPublicly,
                            onCheckedChange = { showEmailPublicly = it }
                        )
                    }
                }
                
                // Save Button
                Button(
                    onClick = {
                        scope.launch {
                            isSaving = true
                            
                            userRepository.updateProfile(
                                name = name,
                                nim = nim.ifEmpty { null },
                                faculty = faculty.ifEmpty { null },
                                department = department.ifEmpty { null },
                                phoneNumber = phoneNumber.ifEmpty { null },
                                showPhonePublicly = showPhonePublicly,
                                showEmailPublicly = showEmailPublicly
                            ).fold(
                                onSuccess = {
                                    isSaving = false
                                    snackbarHostState.showSnackbar("Profile updated successfully!")
                                    onNavigateBack()
                                },
                                onFailure = { error ->
                                    isSaving = false
                                    snackbarHostState.showSnackbar(
                                        error.message ?: "Failed to update profile"
                                    )
                                }
                            )
                        }
                    },
                    enabled = !isSaving && name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Save Changes")
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
