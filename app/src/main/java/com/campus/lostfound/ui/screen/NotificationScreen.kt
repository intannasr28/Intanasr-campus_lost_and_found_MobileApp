package com.campus.lostfound.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.campus.lostfound.data.model.NotificationItem
import com.campus.lostfound.data.model.NotificationType
import com.campus.lostfound.ui.theme.FoundGreen
import com.campus.lostfound.ui.theme.LostRed
import com.campus.lostfound.ui.viewmodel.NotificationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: ((String) -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val viewModel: NotificationViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return NotificationViewModel(context) as T
            }
        }
    )
    
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    
    var showClearAllDialog by remember { mutableStateOf(false) }
    var showDeleteAllMenu by remember { mutableStateOf(false) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedNotifications by remember { mutableStateOf(setOf<String>()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    if (isSelectionMode) {
                        Text("${selectedNotifications.size} dipilih", fontWeight = FontWeight.Bold)
                    } else {
                        Text("Notifikasi", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSelectionMode) {
                            isSelectionMode = false
                            selectedNotifications = setOf()
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            if (isSelectionMode) Icons.Default.Close else Icons.Default.ArrowBack,
                            contentDescription = if (isSelectionMode) "Cancel" else "Back"
                        )
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        // Select all button
                        IconButton(
                            onClick = {
                                selectedNotifications = if (selectedNotifications.size == notifications.size) {
                                    setOf()
                                } else {
                                    notifications.map { it.id }.toSet()
                                }
                            }
                        ) {
                            Icon(
                                if (selectedNotifications.size == notifications.size) 
                                    Icons.Default.CheckBox 
                                else 
                                    Icons.Default.CheckBoxOutlineBlank,
                                contentDescription = "Select all"
                            )
                        }
                        
                        // Delete selected button
                        if (selectedNotifications.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    selectedNotifications.forEach { id ->
                                        viewModel.deleteNotification(id)
                                    }
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            "${selectedNotifications.size} notifikasi berhasil dihapus",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                    selectedNotifications = setOf()
                                    isSelectionMode = false
                                }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Hapus yang dipilih",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    } else if (notifications.isNotEmpty()) {
                        // Selection mode toggle
                        IconButton(onClick = { isSelectionMode = true }) {
                            Icon(
                                Icons.Default.Checklist,
                                contentDescription = "Pilih notifikasi"
                            )
                        }
                        
                        // Mark all as read button
                        IconButton(onClick = { viewModel.markAllAsRead() }) {
                            Icon(
                                Icons.Default.DoneAll,
                                contentDescription = "Tandai semua dibaca",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        // Delete all button (with menu)
                        Box {
                            IconButton(onClick = { showDeleteAllMenu = true }) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "Menu lainnya"
                                )
                            }
                            
                            DropdownMenu(
                                expanded = showDeleteAllMenu,
                                onDismissRequest = { showDeleteAllMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { 
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.DeleteSweep,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                            Text("Hapus Semua Notifikasi")
                                        }
                                    },
                                    onClick = {
                                        showDeleteAllMenu = false
                                        showClearAllDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { 
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Checklist,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Text("Pilih Notifikasi")
                                        }
                                    },
                                    onClick = {
                                        showDeleteAllMenu = false
                                        isSelectionMode = true
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading && notifications.isEmpty()) {
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
                        Icons.Default.Error,
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
        } else if (notifications.isEmpty()) {
            // Empty State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        com.campus.lostfound.ui.components.EmptyStateIllustration.EmptyStateNotificationIllustration()
                        Text(
                            text = "Belum ada notifikasi",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Notifikasi akan muncul di sini saat ada aktivitas terkait laporan Anda",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                itemsIndexed(notifications) { index, notification ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(animationSpec = tween(300, delayMillis = index * 50)) +
                                slideInVertically(
                                    initialOffsetY = { it / 2 },
                                    animationSpec = tween(300, delayMillis = index * 50)
                                )
                    ) {
                        NotificationItemCard(
                            notification = notification,
                            isSelectionMode = isSelectionMode,
                            isSelected = selectedNotifications.contains(notification.id),
                            onSelectionChange = { selected ->
                                selectedNotifications = if (selected) {
                                    selectedNotifications + notification.id
                                } else {
                                    selectedNotifications - notification.id
                                }
                            },
                            onClick = {
                                if (isSelectionMode) {
                                    val selected = !selectedNotifications.contains(notification.id)
                                    selectedNotifications = if (selected) {
                                        selectedNotifications + notification.id
                                    } else {
                                        selectedNotifications - notification.id
                                    }
                                } else {
                                    viewModel.markAsRead(notification.id)
                                    notification.itemId?.let { itemId ->
                                        onNavigateToDetail?.invoke(itemId)
                                    }
                                }
                            },
                            onDelete = {
                                viewModel.deleteNotification(notification.id)
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "\"${notification.title}\" berhasil dihapus",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Clear All Notifications Confirmation Dialog
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            icon = {
                Icon(
                    Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                Text(
                    "Hapus Semua Notifikasi?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Apakah Anda yakin ingin menghapus semua notifikasi?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                "Tindakan ini hanya menghapus notifikasi di perangkat Anda. Pengguna lain tidak terpengaruh.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val count = notifications.size
                        viewModel.clearAllNotifications()
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                "$count notifikasi berhasil dihapus",
                                duration = SnackbarDuration.Short
                            )
                        }
                        showClearAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Hapus Semua")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun NotificationItemCard(
    notification: NotificationItem,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectionChange: ((Boolean) -> Unit)? = null,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val icon = remember(notification.type) { getNotificationIcon(notification.type) }
    val iconColor = getNotificationIconColor(notification.type)
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick, indication = rememberRipple(bounded = true), interactionSource = androidx.compose.foundation.interaction.MutableInteractionSource()),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.read) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Checkbox jika dalam selection mode
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = onSelectionChange,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
            
            // Icon kiri dengan warna sesuai type
            Surface(
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.2f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = iconColor
                    )
                }
            }
            
            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Judul bold
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                // Deskripsi 2-3 baris
                Text(
                    text = notification.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3
                )
                
                // Timestamp kecil: relative + formatted date
                val timeAgo = notification.getTimeAgo()
                val fullDate = notification.getFormattedDate()
                val timeText = if (fullDate.isNotBlank()) "$timeAgo • $fullDate" else timeAgo
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Right side: Delete button + Unread indicator
            if (!isSelectionMode) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Delete button (trash icon)
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus notifikasi",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                
                    // Unread indicator
                    if (!notification.read) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }
    }
    
    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    "Hapus Notifikasi?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Apakah Anda yakin ingin menghapus notifikasi ini? Tindakan ini hanya akan menghapus notifikasi di perangkat ini.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
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
}

fun getNotificationIcon(type: NotificationType): ImageVector {
    return when (type) {
        NotificationType.NEW_REPORT -> Icons.Default.FiberNew  // Icon "NEW" lebih jelas
        NotificationType.STATUS_CHANGED -> Icons.Default.Sync  // Icon sinkronisasi untuk perubahan
        NotificationType.ITEM_FOUND -> Icons.Default.CheckCircle  // Centang hijau
        NotificationType.ITEM_LOST -> Icons.Default.ErrorOutline  // Icon warning untuk kehilangan
        NotificationType.ITEM_COMPLETED -> Icons.Default.Verified  // Icon verifikasi dengan centang
        NotificationType.ITEM_RETURNED -> Icons.Default.AssignmentTurnedIn  // Icon dokumen selesai
        NotificationType.MATCH_FOUND -> Icons.Default.TravelExplore  // Icon pencarian menemukan
        NotificationType.REMINDER -> Icons.Default.NotificationImportant  // Bell dengan tanda seru
        NotificationType.OTHER -> Icons.Default.Notifications  // Bell biasa
    }
}

@Composable
fun getNotificationIconColor(type: NotificationType): Color {
    return when (type) {
        NotificationType.NEW_REPORT -> MaterialTheme.colorScheme.primary  // Biru/Teal
        NotificationType.STATUS_CHANGED -> Color(0xFFFF9800)  // Orange untuk perubahan
        NotificationType.ITEM_FOUND -> FoundGreen  // Hijau terang
        NotificationType.ITEM_LOST -> LostRed  // Merah untuk kehilangan
        NotificationType.ITEM_COMPLETED -> Color(0xFF4CAF50)  // Hijau sukses
        NotificationType.ITEM_RETURNED -> Color(0xFF00BCD4)  // Cyan untuk pengembalian
        NotificationType.MATCH_FOUND -> Color(0xFF9C27B0)  // Ungu untuk kecocokan
        NotificationType.REMINDER -> Color(0xFFFF5722)  // Orange merah untuk reminder
        NotificationType.OTHER -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    }
}

