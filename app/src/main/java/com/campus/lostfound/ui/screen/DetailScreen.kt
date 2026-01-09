package com.campus.lostfound.ui.screen

import android.graphics.BitmapFactory
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.request.CachePolicy
import com.campus.lostfound.data.model.ItemType
import com.campus.lostfound.data.model.LostFoundItem
import com.campus.lostfound.data.repository.LostFoundRepository
import com.campus.lostfound.data.repository.UserRepository
import com.campus.lostfound.ui.components.SmallUserAvatar
import com.campus.lostfound.ui.theme.FoundGreen
import com.campus.lostfound.ui.theme.FoundGreenLight
import com.campus.lostfound.ui.theme.LostRed
import com.campus.lostfound.ui.theme.LostRedLight
import com.campus.lostfound.util.ImageConverter
import com.campus.lostfound.util.WhatsAppUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.rememberCoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    itemId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: ((String) -> Unit)? = null,
    onNavigateToPublicProfile: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel: DetailViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DetailViewModel(context, itemId) as T
            }
        }
    )

    val item by viewModel.item.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isOwner by viewModel.isOwner.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val currentUserName by viewModel.currentUserName.collectAsStateWithLifecycle()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showCompleteDialog by remember { mutableStateOf(false) }
    var showFullscreenImage by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Laporan") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isOwner) {
                        // First row: Edit and Selesai
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onNavigateToEdit?.invoke(itemId) },
                                modifier = Modifier.weight(1f),
                                enabled = !(item?.isCompleted ?: false)
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Edit", maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = { showCompleteDialog = true },
                                modifier = Modifier.weight(1f),
                                enabled = !(item?.isCompleted ?: false)
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Selesai", maxLines = 1)
                            }
                        }
                        
                        // Second row: Hapus (full width)
                        OutlinedButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Hapus Laporan", maxLines = 1)
                        }
                    }

                    Button(
                        onClick = {
                            item?.let { currentItem ->
                                scope.launch(Dispatchers.IO) {
                                    WhatsAppUtil.openWhatsAppWithImage(
                                        context = context,
                                        phoneNumber = currentItem.whatsappNumber,
                                        itemName = currentItem.itemName,
                                        type = if (currentItem.type == ItemType.LOST) "barang hilang" else "barang ditemukan",
                                        imageUrl = currentItem.imageUrl,
                                        location = currentItem.location
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = item != null && !(item?.isCompleted ?: false),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (item?.isCompleted == true) 
                                MaterialTheme.colorScheme.surfaceVariant
                            else 
                                MaterialTheme.colorScheme.secondary,
                            contentColor = if (item?.isCompleted == true)
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else
                                MaterialTheme.colorScheme.onSecondary
                        ),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        if (item?.isCompleted == true) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Laporan Sudah Selesai", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Hubungi via WhatsApp", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Filled.Error,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = errorMessage ?: "Terjadi kesalahan",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        } else if (item != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    if (item!!.imageUrl.isNotEmpty()) {
                        if (ImageConverter.isBase64Image(item!!.imageUrl)) {
                            val bitmapResult = remember(item!!.imageUrl) {
                                runCatching {
                                    val base64String = ImageConverter.extractBase64(item!!.imageUrl)
                                    val imageBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
                                    BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                }
                            }

                            bitmapResult.getOrNull()?.let { bitmap ->
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = item!!.itemName,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable { showFullscreenImage = true },
                                    contentScale = ContentScale.Crop
                                )
                            } ?: ImagePlaceholder(modifier = Modifier.fillMaxSize())
                        } else {
                            // ✅ Optimized URL image with Coil caching
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(item!!.imageUrl)
                                    .crossfade(true)
                                    .size(800) // ✅ Max 800px for detail view
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                    .build(),
                                contentDescription = item!!.itemName,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { showFullscreenImage = true },
                                contentScale = ContentScale.Crop
                            )
                        }
                    } else {
                        ImagePlaceholder(modifier = Modifier.fillMaxSize())
                    }

                    Surface(
                        color = if (item!!.type == ItemType.LOST) LostRedLight else FoundGreenLight,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = if (item!!.type == ItemType.LOST) "Hilang" else "Ditemukan",
                            color = if (item!!.type == ItemType.LOST) LostRed else FoundGreen,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = item!!.itemName,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )

                    // Banner Info untuk Completed Items
                    if (item!!.isCompleted) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = FoundGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = FoundGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "Laporan Selesai",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = FoundGreen
                                    )
                                    Text(
                                        text = "Barang ini telah dikembalikan ke pemiliknya",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            InfoRow(
                                icon = Icons.Filled.Category,
                                label = "Kategori",
                                value = item!!.category.displayName
                            )

                            HorizontalDivider()

                            InfoRow(
                                icon = Icons.Filled.LocationOn,
                                label = "Lokasi",
                                value = item!!.location
                            )

                            HorizontalDivider()

                            InfoRow(
                                icon = Icons.Filled.Schedule,
                                label = "Waktu",
                                value = item!!.getTimeAgo()
                            )
                        }
                    }

                    // Reporter Info Card
                    if (!item!!.userName.isNullOrEmpty() && !item!!.userId.isNullOrEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onNavigateToPublicProfile?.invoke(item!!.userId)
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                SmallUserAvatar(
                                    photoUrl = item!!.userPhotoUrl,
                                    name = currentUserName ?: item!!.userName ?: ""
                                )
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "Dilaporkan oleh",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = currentUserName ?: item!!.userName ?: "Memuat...",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 2,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    Icons.Filled.ChevronRight,
                                    contentDescription = "View Profile",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (item!!.description.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Deskripsi",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = item!!.description,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (item!!.isCompleted) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Laporan ini telah ditandai selesai",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus Laporan?") },
            text = {
                Text("Apakah Anda yakin ingin menghapus laporan \"${item?.itemName}\"? Tindakan ini tidak dapat dibatalkan.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteItem {
                            showDeleteDialog = false
                            onNavigateBack()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    if (showCompleteDialog) {
        AlertDialog(
            onDismissRequest = { showCompleteDialog = false },
            title = { Text("Tandai Selesai?") },
            text = {
                Text("Apakah Anda yakin ingin menandai laporan \"${item?.itemName}\" sebagai selesai?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.markAsCompleted {
                            showCompleteDialog = false
                        }
                    }
                ) {
                    Text("Ya, Tandai Selesai")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCompleteDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Fullscreen Image Viewer
    if (showFullscreenImage && item != null && item!!.imageUrl.isNotEmpty()) {
        FullscreenImageViewer(
            imageUrl = item!!.imageUrl,
            itemName = item!!.itemName,
            onDismiss = { showFullscreenImage = false }
        )
    }
}

@Composable
private fun FullscreenImageViewer(
    imageUrl: String,
    itemName: String,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    if (scale > 1f) {
                        offsetX += pan.x
                        offsetY += pan.y
                    } else {
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            }
    ) {
        // Image
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (ImageConverter.isBase64Image(imageUrl)) {
                val bitmapResult = remember(imageUrl) {
                    runCatching {
                        val base64String = ImageConverter.extractBase64(imageUrl)
                        val imageBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    }
                }

                bitmapResult.getOrNull()?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = itemName,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            ),
                        contentScale = ContentScale.Fit
                    )
                }
            } else {
                Image(
                    painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .size(1200) // ✅ Max 1200px for fullscreen
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .build()
                    ),
                    contentDescription = itemName,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        ),
                    contentScale = ContentScale.Fit
                )
            }
        }

        // Close button
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(48.dp),
            shape = androidx.compose.foundation.shape.CircleShape,
            color = Color.Black.copy(alpha = 0.6f)
        ) {
            IconButton(
                onClick = onDismiss
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Zoom indicator
        if (scale > 1f) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Text(
                    text = "${(scale * 100).toInt()}%",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        // Hint text (only at normal scale)
        if (scale == 1f) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Text(
                    text = "Pinch untuk zoom",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ImagePlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.Image,
            contentDescription = "No image",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

class DetailViewModel(
    private val context: android.content.Context,
    private val itemId: String
) : ViewModel() {
    private val repository = LostFoundRepository(context)
    private val localHistoryRepository = com.campus.lostfound.data.LocalHistoryRepository(context)
    private val userRepository = UserRepository()
    
    private val _item = MutableStateFlow<LostFoundItem?>(null)
    val item: StateFlow<LostFoundItem?> = _item.asStateFlow()
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _isOwner = MutableStateFlow(false)
    val isOwner: StateFlow<Boolean> = _isOwner.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    // State untuk menyimpan nama user yang di-fetch secara real-time
    private val _currentUserName = MutableStateFlow<String?>(null)
    val currentUserName: StateFlow<String?> = _currentUserName.asStateFlow()
    
    init {
        loadItem()
    }
    
    private fun loadItem() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            // Hybrid System: Try Firestore first, then fallback to Local Storage
            val result = repository.getItemById(itemId)
            
            if (result.isSuccess) {
                // Found in Firestore (within 7 days grace period)
                val loadedItem = result.getOrNull()!!
                _item.value = loadedItem
                _isOwner.value = loadedItem.userId == repository.getCurrentUserId()
                android.util.Log.d("DetailViewModel", "✅ Item loaded from Firestore: ${loadedItem.itemName}")
                
                // Fetch current user profile name secara real-time
                fetchCurrentUserName(loadedItem.userId)
            } else {
                // Not found in Firestore, try Local Storage (after 7 days cleanup)
                android.util.Log.d("DetailViewModel", "⚠️ Item not in Firestore, checking local storage...")
                
                val localItem = localHistoryRepository.getHistoryById(itemId)
                if (localItem != null) {
                    _item.value = localItem.item
                    _isOwner.value = true // If in local history, user is the owner
                    android.util.Log.d("DetailViewModel", "✅ Item loaded from Local Storage: ${localItem.item.itemName}")
                    
                    // Fetch current user profile name untuk local item juga
                    fetchCurrentUserName(localItem.item.userId)
                } else {
                    // Not found anywhere
                    _errorMessage.value = "Laporan tidak ditemukan"
                    android.util.Log.e("DetailViewModel", "❌ Item not found in Firestore or Local Storage")
                }
            }
            
            _isLoading.value = false
        }
    }
    
    private fun fetchCurrentUserName(userId: String) {
        viewModelScope.launch {
            val result = userRepository.getUserProfile(userId)
            result.onSuccess { userProfile ->
                // Gunakan nama dari profile yang up-to-date
                _currentUserName.value = userProfile.name
                android.util.Log.d("DetailViewModel", "✅ Fetched current user name: ${userProfile.name}")
            }.onFailure { error ->
                // Fallback ke userName historis jika fetch gagal
                _currentUserName.value = _item.value?.userName
                android.util.Log.w("DetailViewModel", "⚠️ Failed to fetch user name, using historical: ${_item.value?.userName}")
            }
        }
    }
    
    fun deleteItem(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val currentItem = _item.value ?: return@launch
            val result = repository.deleteItem(currentItem.id, currentItem.imageStoragePath)
            result.onSuccess {
                onSuccess()
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Gagal menghapus laporan"
            }
        }
    }
    
    fun markAsCompleted(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val result = repository.markAsCompleted(itemId)
            result.onSuccess {
                loadItem() // Reload untuk update status
                onSuccess()
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Gagal menandai selesai"
            }
        }
    }
}

