package com.campus.lostfound.ui.screen

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.request.CachePolicy
import com.campus.lostfound.data.LocalHistoryRepository
import com.campus.lostfound.data.model.LostFoundItem
import com.campus.lostfound.ui.viewmodel.ActivityViewModel
import com.campus.lostfound.ui.components.ItemCard
import com.campus.lostfound.util.rememberImagePicker


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    onNavigateToDetail: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val viewModel = remember { ActivityViewModel(context) }
    ActivityScreenContent(
        context = context, 
        viewModel = viewModel,
        onNavigateToDetail = onNavigateToDetail
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivityScreenContent(
    context: android.content.Context,
    viewModel: ActivityViewModel,
    onNavigateToDetail: ((String) -> Unit)? = null
) {
    val myReports by viewModel.myReports.collectAsState(initial = emptyList())
    val historyWithDate by viewModel.historyWithDate.collectAsState(initial = emptyList())
    val isLoading by viewModel.isLoading.collectAsState(initial = false)
    val errorMessage by viewModel.errorMessage.collectAsState(initial = null)

    var showDeleteDialog by remember { mutableStateOf<LostFoundItem?>(null) }
    var showCompleteDialog by remember { mutableStateOf<LostFoundItem?>(null) }
    var showEditDialog by remember { mutableStateOf<LostFoundItem?>(null) }
    var showHistory by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Aktivitas Saya",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Kelola laporan Anda",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !showHistory,
                    onClick = { showHistory = false },
                    label = { Text("Aktif (${myReports.size})") }
                )
                FilterChip(
                    selected = showHistory,
                    onClick = { showHistory = true },
                    label = { Text("Riwayat (${historyWithDate.size})") }
                )
            }
        }

        // Error message
        errorMessage?.let { error ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(text = error, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }

        when {
            isLoading && myReports.isEmpty() && historyWithDate.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
            !showHistory && myReports.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "Belum ada laporan aktif", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "Buat laporan pertama Anda", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            showHistory && historyWithDate.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "Belum ada riwayat", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "Laporan yang selesai akan muncul di sini", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "📱 Riwayat tersimpan di perangkat ini saja", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            !showHistory -> {
                // Active Reports List
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                    itemsIndexed(myReports) { index, item ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(animationSpec = tween(300, delayMillis = index * 50)) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(300, delayMillis = index * 50))) {
                            ActiveReportCard(
                                item = item,
                                context = context,
                                onEdit = { showEditDialog = item },
                                onComplete = { showCompleteDialog = item },
                                onDelete = { showDeleteDialog = item },
                                onNavigateToDetail = onNavigateToDetail
                            )
                        }
                    }
                }
            }
            else -> {
                // History List with completion date
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                    itemsIndexed(historyWithDate) { index, completedReport ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(animationSpec = tween(300, delayMillis = index * 50)) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(300, delayMillis = index * 50))) {
                            HistoryReportCard(
                                completedReport = completedReport,
                                context = context,
                                onNavigateToDetail = onNavigateToDetail
                            )
                        }
                    }
                    
                    // Info footer
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Riwayat hanya tersimpan di perangkat ini. Jika Anda menghapus data aplikasi atau uninstall, riwayat akan hilang.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation for Active Reports
    showDeleteDialog?.let { item ->
        ModalBottomSheet(onDismissRequest = { showDeleteDialog = null }, containerColor = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                Text(text = "Hapus Laporan?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(text = "Apakah Anda yakin ingin menghapus laporan \"${item.itemName}\"? Tindakan ini tidak dapat dibatalkan.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showDeleteDialog = null }, modifier = Modifier.weight(1f)) { Text("Batal") }
                    Button(onClick = { viewModel.deleteReport(item) { showDeleteDialog = null } }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Hapus") }
                }
            }
        }
    }
    
    // Complete Confirmation Bottom Sheet
    showCompleteDialog?.let { item ->
        ModalBottomSheet(onDismissRequest = { showCompleteDialog = null }, containerColor = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                Text(text = "Tandai Selesai?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(text = "Apakah Anda yakin ingin menandai laporan \"${item.itemName}\" sebagai selesai?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(
                            text = "Laporan akan dihapus dari daftar publik dan dipindahkan ke riwayat lokal di perangkat Anda.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showCompleteDialog = null }, modifier = Modifier.weight(1f)) { Text("Batal") }
                    Button(onClick = { viewModel.markAsCompleted(item) { showCompleteDialog = null } }, modifier = Modifier.weight(1f)) { Text("Ya, Selesai") }
                }
            }
        }
    }

    // Edit Dialog
    showEditDialog?.let { item ->
        EditReportDialog(item = item, onDismiss = { showEditDialog = null }, onSave = { updatedItem, imageUri ->
            viewModel.updateReport(itemId = item.id, itemName = updatedItem.itemName.takeIf { it != item.itemName }, category = updatedItem.category.takeIf { it != item.category }, location = updatedItem.location.takeIf { it != item.location }, description = updatedItem.description.takeIf { it != item.description }, whatsappNumber = updatedItem.whatsappNumber.takeIf { it != item.whatsappNumber }, imageUri = imageUri, onSuccess = { showEditDialog = null })
        })
    }
}

@Composable
private fun ActiveReportCard(
    item: LostFoundItem,
    context: android.content.Context,
    onEdit: () -> Unit,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    onNavigateToDetail: ((String) -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ItemCard(
                item = item,
                onContactClick = {
                    com.campus.lostfound.util.WhatsAppUtil.openWhatsApp(
                        context = context,
                        phoneNumber = item.whatsappNumber,
                        itemName = item.itemName,
                        type = if (item.type == com.campus.lostfound.data.model.ItemType.LOST) "barang hilang" else "barang ditemukan"
                    )
                },
                onCardClick = {
                    onNavigateToDetail?.invoke(item.id)
                },
                modifier = Modifier.fillMaxWidth()
            )

            Divider()

            // Action buttons - stacked vertically for better layout
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Edit and Complete in one row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit", maxLines = 1, fontSize = 12.sp)
                    }

                    Button(
                        onClick = onComplete,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Selesai", maxLines = 1, fontSize = 12.sp)
                    }
                }
                
                // Delete button full width
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Hapus Laporan", maxLines = 1, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun HistoryReportCard(
    completedReport: LocalHistoryRepository.CompletedReport,
    context: android.content.Context,
    onNavigateToDetail: ((String) -> Unit)? = null
) {
    val item = completedReport.item
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Completed Badge with date
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "✅ Laporan Selesai",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Diselesaikan pada ${completedReport.completedAtFormatted}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            
            ItemCard(
                item = item,
                onContactClick = { },
                onCardClick = {
                    onNavigateToDetail?.invoke(item.id)
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Info card instead of delete button
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Riwayat tersimpan di device ini dan akan hilang saat uninstall app.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditReportDialog(
    item: LostFoundItem,
    onDismiss: () -> Unit,
    onSave: (LostFoundItem, Uri?) -> Unit
) {
    var itemName by remember { mutableStateOf(item.itemName) }
    var category by remember { mutableStateOf(item.category) }
    var location by remember { mutableStateOf(item.location) }
    var description by remember { mutableStateOf(item.description) }
    var whatsappNumber by remember { mutableStateOf(item.whatsappNumber) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var showImageSourceDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val imagePicker = rememberImagePicker { uri ->
        imageUri = uri
    }

    val scrollState = rememberScrollState()
    val isPhoneValid = whatsappNumber.isBlank() ||
            com.campus.lostfound.util.WhatsAppUtil.isValidIndonesianPhoneNumber(whatsappNumber)
    val formattedPreview = if (whatsappNumber.isNotBlank() && isPhoneValid) {
        com.campus.lostfound.util.WhatsAppUtil.formatPhoneNumber(whatsappNumber)
    } else {
        null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Laporan", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("Nama Barang *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = category.displayName,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Kategori *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        com.campus.lostfound.data.model.Category.values().forEach { cat ->
                            DropdownMenuItem(text = { Text(cat.displayName) }, onClick = { category = cat; expanded = false })
                        }
                    }
                }

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Lokasi *") },
                    placeholder = { Text("Contoh: Perpustakaan Lt. 2") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) }
                )

                Text(text = "Foto Barang", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                if (imageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            ImageRequest.Builder(LocalContext.current)
                                .data(imageUri)
                                .size(400) // ✅ Resize preview
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .build()
                        ),
                        contentDescription = "Selected image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(MaterialTheme.shapes.medium)
                    )
                } else if (item.imageUrl.isNotEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            ImageRequest.Builder(LocalContext.current)
                                .data(item.imageUrl)
                                .size(400) // ✅ Resize preview
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .build()
                        ),
                        contentDescription = "Current image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(MaterialTheme.shapes.medium)
                    )
                }

                OutlinedButton(onClick = { showImageSourceDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (item.imageUrl.isNotEmpty()) "Ganti Foto" else "Tambah Foto")
                }

                OutlinedTextField(
                    value = whatsappNumber,
                    onValueChange = { whatsappNumber = it },
                    label = { Text("Nomor WhatsApp *") },
                    placeholder = { Text("08123456789 atau 628123456789") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    isError = !isPhoneValid && whatsappNumber.isNotBlank(),
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it.take(500) },
                    label = { Text("Deskripsi (Opsional)") },
                    placeholder = { Text("Tambahkan detail tambahan...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (itemName.isNotBlank() && location.isNotBlank() && whatsappNumber.isNotBlank() && isPhoneValid) {
                    val updatedItem = item.copy(
                        itemName = itemName,
                        category = category,
                        location = location,
                        description = description,
                        whatsappNumber = whatsappNumber
                    )
                    onSave(updatedItem, imageUri)
                }
            }, enabled = itemName.isNotBlank() && location.isNotBlank() && whatsappNumber.isNotBlank() && isPhoneValid) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )

    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { 
                Text(
                    "Pilih Sumber Foto",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Gallery Option
                    Surface(
                        onClick = {
                            showImageSourceDialog = false
                            imagePicker.pickFromGallery()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Photo,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Pilih dari Galeri",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Camera Option
                    Surface(
                        onClick = {
                            showImageSourceDialog = false
                            imagePicker.takePhoto()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Ambil Foto",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showImageSourceDialog = false }) {
                    Text("Batal")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

