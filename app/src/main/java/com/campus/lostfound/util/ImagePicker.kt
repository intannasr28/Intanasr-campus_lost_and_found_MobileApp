package com.campus.lostfound.util

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import android.Manifest
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.io.File
import androidx.core.content.FileProvider
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun rememberImagePicker(
    onImageSelected: (Uri?) -> Unit
): ImagePickerLauncher {
    val context = LocalContext.current
    
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    val imagePickerRef = remember { arrayOfNulls<ImagePickerLauncher>(1) }
    
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraImageUri != null) {
            onImageSelected(cameraImageUri)
        }
    }
    
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        onImageSelected(uri)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        imagePickerRef[0]?.onPermissionResult(granted)
    }
    
    val launcher = remember {
        ImagePickerLauncher(
            cameraLauncher = cameraLauncher,
            galleryLauncher = galleryLauncher,
            context = context,
            onUriCreated = { uri ->
                cameraImageUri = uri
            },
            permissionLauncher = permissionLauncher
        )
    }
    imagePickerRef[0] = launcher
    return launcher
}

class ImagePickerLauncher(
    val cameraLauncher: androidx.activity.result.ActivityResultLauncher<Uri>,
    val galleryLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    val context: Context,
    val onUriCreated: (Uri) -> Unit,
    val permissionLauncher: ActivityResultLauncher<String>
) {
    private var pendingTakePhoto: Boolean = false

    fun pickFromGallery() {
        galleryLauncher.launch("image/*")
    }

    fun takePhoto() {
        val hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (hasCamera) {
            doTakePhoto()
        } else {
            pendingTakePhoto = true
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    internal fun onPermissionResult(granted: Boolean) {
        if (granted && pendingTakePhoto) {
            pendingTakePhoto = false
            doTakePhoto()
        } else {
            pendingTakePhoto = false
        }
    }

    private fun doTakePhoto() {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        val imageFile = File.createTempFile(imageFileName, ".jpg", storageDir)
        val imageUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
        onUriCreated(imageUri)
        cameraLauncher.launch(imageUri)
    }
}

