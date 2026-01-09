package com.campus.lostfound.ui.screen

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlin.math.sin
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.campus.lostfound.data.model.Category
import com.campus.lostfound.data.model.ItemType
import com.campus.lostfound.data.repository.UserRepository
import com.campus.lostfound.ui.viewmodel.AddReportViewModel
import com.campus.lostfound.ui.viewmodel.AddReportUiState
import com.campus.lostfound.util.WhatsAppUtil
import com.campus.lostfound.util.rememberImagePicker
import com.campus.lostfound.util.ImagePickerLauncher
import kotlinx.coroutines.delay
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReportScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    
    var showSuccessDialog by remember { mutableStateOf(false) }
    
    // Check guest mode
    val settingsRepository = remember { com.campus.lostfound.data.SettingsRepository(context) }
    val isGuestMode by settingsRepository.isGuestModeFlow.collectAsState(initial = false)
    
    // Show guest restriction dialog if in guest mode
    if (isGuestMode) {
        AlertDialog(
            onDismissRequest = onNavigateBack,
            icon = {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    "Fitur Terbatas",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Anda sedang dalam mode tamu. Untuk membuat laporan, silakan login terlebih dahulu.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Dengan login, Anda dapat:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text("• Membuat laporan barang hilang/ditemukan", style = MaterialTheme.typography.bodySmall)
                        Text("• Mengelola profil Anda", style = MaterialTheme.typography.bodySmall)
                        Text("• Menerima notifikasi terbaru", style = MaterialTheme.typography.bodySmall)
                        Text("• Tracking laporan Anda", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = onNavigateBack) {
                    Text("Kembali")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
        return
    }
    
    val viewModel: AddReportViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AddReportViewModel(context) as T
            }
        }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    
    // User profile phone state
    val userRepository = remember { UserRepository() }
    var userProfilePhone by remember { mutableStateOf("") }
    var useProfilePhone by remember { mutableStateOf(false) }
    
    // Load user profile phone
    LaunchedEffect(Unit) {
        userRepository.getCurrentUserProfile().onSuccess { user ->
            userProfilePhone = user.phoneNumber
            // Auto-use profile phone if available and current phone is empty
            if (userProfilePhone.isNotEmpty() && uiState.whatsappNumber.isBlank()) {
                useProfilePhone = true
                viewModel.setWhatsAppNumber(userProfilePhone)
            }
        }
    }
    
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }
    
    val imagePicker = rememberImagePicker { uri ->
        tempImageUri = uri
        viewModel.setImageUri(uri)
    }
    
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onNavigateBack()
            viewModel.resetState()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Buat Laporan", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(
                            text = if (uiState.itemType == ItemType.LOST) "Laporkan Barang Hilang" else "Laporkan Barang Ditemukan",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Animated Error Message
            AnimatedVisibility(
                visible = uiState.errorMessage != null,
                enter = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                ) + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                uiState.errorMessage?.let { error ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            
            // Single Form Content
            SinglePageFormContent(
                uiState = uiState,
                viewModel = viewModel,
                userProfilePhone = userProfilePhone,
                useProfilePhone = useProfilePhone,
                onUseProfilePhoneChange = { useProfilePhone = it },
                showImageSourceDialog = showImageSourceDialog,
                onShowImageSourceDialog = { showImageSourceDialog = it },
                imagePicker = imagePicker,
                tempImageUri = tempImageUri,
                onTempImageUriChange = { tempImageUri = it }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Submit Button with Gradient & Haptic Feedback
            val buttonScale by animateFloatAsState(
                targetValue = if (uiState.isLoading) 0.95f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "buttonScale"
            )
            
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.submitReport {
                        showSuccessDialog = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .scale(buttonScale),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                ),
                enabled = uiState.itemName.isNotBlank() && 
                         uiState.location.isNotBlank() &&
                         uiState.whatsappNumber.isNotBlank() &&
                         WhatsAppUtil.isValidIndonesianPhoneNumber(uiState.whatsappNumber) &&
                         uiState.imageUri != null && 
                         !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Text("Mengirim...", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(20.dp))
                        Text("Kirim Laporan", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
    
    // Success Celebration Dialog
    if (showSuccessDialog) {
        SuccessCelebrationDialog(
            onDismiss = {
                showSuccessDialog = false
                onNavigateBack()
            }
        )
    }
    
    // Image Source Dialog
    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("Pilih Sumber Gambar", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Pilih dari mana Anda ingin mengambil foto:")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        imagePicker.takePhoto()
                        showImageSourceDialog = false
                    }
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Kamera")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        imagePicker.pickFromGallery()
                        showImageSourceDialog = false
                    }
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Galeri")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// Single Page Form Content
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SinglePageFormContent(
    uiState: AddReportUiState,
    viewModel: AddReportViewModel,
    userProfilePhone: String,
    useProfilePhone: Boolean,
    onUseProfilePhoneChange: (Boolean) -> Unit,
    showImageSourceDialog: Boolean,
    onShowImageSourceDialog: (Boolean) -> Unit,
    imagePicker: ImagePickerLauncher,
    tempImageUri: Uri?,
    onTempImageUriChange: (Uri?) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // SECTION 1: Enhanced Photo Upload Zone
        EnhancedPhotoUploadCard(
            uiState = uiState,
            viewModel = viewModel,
            imagePicker = imagePicker,
            onTempImageUriChange = onTempImageUriChange
        )
        
        // SECTION 2: Info Barang
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Inventory,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Informasi Barang",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Jenis Laporan
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        selected = uiState.itemType == ItemType.LOST,
                        onClick = { viewModel.setItemType(ItemType.LOST) },
                        label = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ErrorOutline, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Barang Hilang")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = uiState.itemType == ItemType.FOUND,
                        onClick = { viewModel.setItemType(ItemType.FOUND) },
                        label = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Barang Ditemukan")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Nama Barang
                OutlinedTextField(
                    value = uiState.itemName,
                    onValueChange = { viewModel.setItemName(it) },
                    label = { Text("Nama Barang *") },
                    placeholder = { Text("e.g. iPhone 13 Pro") },
                    leadingIcon = { Icon(Icons.Default.Edit, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    isError = uiState.itemName.isBlank()
                )
                
                // Visual Category Selector
                VisualCategorySelector(
                    selectedCategory = uiState.category,
                    onCategorySelected = { viewModel.setCategory(it) }
                )
                
                // Interactive Location Picker
                InteractiveLocationPicker(
                    selectedLocation = uiState.location,
                    onLocationSelected = { viewModel.setLocation(it) }
                )
                
                // Enhanced Description Editor
                EnhancedDescriptionEditor(
                    description = uiState.description,
                    onDescriptionChange = { viewModel.setDescription(it) },
                    maxLength = 500
                )
            }
        }
        
        // SECTION 3: Contact Number
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Nomor Kontak",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Use Profile Phone Option
                if (userProfilePhone.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().clickable {
                            val newValue = !useProfilePhone
                            onUseProfilePhoneChange(newValue)
                            if (newValue) {
                                viewModel.setWhatsAppNumber(userProfilePhone)
                            } else {
                                viewModel.setWhatsAppNumber("")
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = useProfilePhone,
                                onCheckedChange = null
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Use number from profile",
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    userProfilePhone,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                // WhatsApp Number
                val isPhoneValid = uiState.whatsappNumber.isBlank() || 
                                  WhatsAppUtil.isValidIndonesianPhoneNumber(uiState.whatsappNumber)
                
                OutlinedTextField(
                    value = uiState.whatsappNumber,
                    onValueChange = { 
                        val clean = it.filter { ch -> ch.isDigit() }
                        viewModel.setWhatsAppNumber(clean)
                        if (useProfilePhone && clean != userProfilePhone) {
                            onUseProfilePhoneChange(false)
                        }
                    },
                    label = { Text("Contact Number *") },
                    placeholder = { Text("+62 812-3456-7890") },
                    leadingIcon = { Icon(Icons.Default.Phone, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !useProfilePhone,
                    shape = RoundedCornerShape(12.dp),
                    isError = !isPhoneValid,
                    supportingText = {
                        if (!isPhoneValid) {
                            Text("⚠️ Invalid phone format", color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("💡 This will be shown to users who want to contact you")
                        }
                    }
                )
            }
        }
    }
}
