package com.campus.lostfound.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.graphics.Bitmap
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult

object WhatsAppUtil {
    // Nomor WhatsApp untuk bug report
    private const val BUG_REPORT_PHONE = "6289604054315"
    
    /**
     * Format nomor telepon Indonesia ke format internasional
     * Contoh: 08123456789 -> 628123456789
     *         0812-3456-7890 -> 6281234567890
     *         628123456789 -> 628123456789 (sudah benar)
     */
    fun formatPhoneNumber(phoneNumber: String): String {
        // Hapus semua karakter non-digit
        val cleanNumber = phoneNumber.replace(Regex("[^0-9]"), "")
        
        // Jika nomor dimulai dengan 0, ganti dengan 62
        return when {
            cleanNumber.startsWith("0") -> {
                "62${cleanNumber.substring(1)}"
            }
            cleanNumber.startsWith("62") -> {
                cleanNumber
            }
            cleanNumber.startsWith("+62") -> {
                cleanNumber.substring(1) // Hapus tanda +
            }
            else -> {
                // Jika tidak dimulai dengan 0 atau 62, anggap sudah format internasional
                // atau tambahkan 62 jika panjangnya 10-12 digit (nomor Indonesia)
                if (cleanNumber.length in 10..12) {
                    "62$cleanNumber"
                } else {
                    cleanNumber
                }
            }
        }
    }
    
    /**
     * Validasi format nomor telepon Indonesia
     */
    fun isValidIndonesianPhoneNumber(phoneNumber: String): Boolean {
        val cleanNumber = phoneNumber.replace(Regex("[^0-9]"), "")
        return when {
            cleanNumber.startsWith("0") -> cleanNumber.length in 10..13
            cleanNumber.startsWith("62") -> cleanNumber.length in 11..14
            cleanNumber.startsWith("+62") -> cleanNumber.length in 12..15
            else -> false
        }
    }
    
    fun openWhatsApp(context: Context, phoneNumber: String, itemName: String, type: String) {
        try {
            val formattedNumber = formatPhoneNumber(phoneNumber)
            val message = "Halo, saya melihat laporan $type \"$itemName\" di Campus Lost & Found. Apakah barang ini masih tersedia?"
            
            val whatsappIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://wa.me/$formattedNumber?text=${Uri.encode(message)}")
                setPackage("com.whatsapp")
            }
            
            if (whatsappIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(whatsappIntent)
            } else {
                // Fallback to web WhatsApp
                val webIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://wa.me/$formattedNumber?text=${Uri.encode(message)}")
                }
                context.startActivity(webIntent)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membuka WhatsApp", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Open WhatsApp with image sharing
     * Downloads image from URL and shares via WhatsApp
     */
    suspend fun openWhatsAppWithImage(
        context: Context, 
        phoneNumber: String, 
        itemName: String, 
        type: String,
        imageUrl: String,
        location: String
    ) {
        try {
            val formattedNumber = formatPhoneNumber(phoneNumber)
            val message = buildString {
                append("📦 *CAMPUS LOST & FOUND*\n\n")
                append("Halo! Saya lihat  laporan $type:\n\n")
                append("🏷️ *Nama Barang:* $itemName\n")
                append("📍 *Lokasi:* $location\n\n")
                append("Apakah barang ini masih ada?\n")
                append("Terima kasih! 🙏")
            }
            
            // Try to download and share image
            val imageUri = downloadImageToCache(context, imageUrl, itemName)
            
            withContext(Dispatchers.Main) {
                if (imageUri != null) {
                    // Share with image using direct WhatsApp intent
                    try {
                        // Grant URI permission to WhatsApp before sharing
                        context.grantUriPermission(
                            "com.whatsapp",
                            imageUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                        
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            setType("image/jpeg")
                            putExtra(Intent.EXTRA_STREAM, imageUri)
                            putExtra(Intent.EXTRA_TEXT, message)
                            setPackage("com.whatsapp")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        
                        if (shareIntent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(shareIntent)
                        } else {
                            // Try without package restriction
                            val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                                setType("image/jpeg")
                                putExtra(Intent.EXTRA_STREAM, imageUri)
                                putExtra(Intent.EXTRA_TEXT, message)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(Intent.createChooser(fallbackIntent, "Kirim via"))
                        }
                    } catch (e: Exception) {
                        // Fallback to simple WhatsApp
                        openWhatsApp(context, phoneNumber, itemName, type)
                    }
                } else {
                    // Fallback if image download fails
                    openWhatsApp(context, phoneNumber, itemName, type)
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Gagal membuka WhatsApp: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Download image from URL to cache directory
     */
    private suspend fun downloadImageToCache(context: Context, imageUrl: String, fileName: String): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                if (imageUrl.isBlank()) return@withContext null
                
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .allowHardware(false)
                    .build()
                
                val result = (loader.execute(request) as? SuccessResult)?.drawable
                val bitmap = (result as? android.graphics.drawable.BitmapDrawable)?.bitmap
                    ?: return@withContext null
                
                // Save bitmap to cache
                val cacheDir = File(context.cacheDir, "shared_images")
                if (!cacheDir.exists()) cacheDir.mkdirs()
                
                val sanitizedName = fileName.replace(Regex("[^a-zA-Z0-9]"), "_")
                val file = File(cacheDir, "lost_found_${sanitizedName}_${System.currentTimeMillis()}.jpg")
                
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Open WhatsApp for bug report
     */
    fun openBugReportWhatsApp(context: Context, bugDescription: String = "") {
        try {
            val deviceInfo = buildString {
                append("📱 *Device Info:*\n")
                append("- Model: ${android.os.Build.MODEL}\n")
                append("- Android: ${android.os.Build.VERSION.RELEASE}\n")
                append("- App Version: ${getAppVersion(context)}\n")
            }
            
            val message = buildString {
                append("🐛 *BUG REPORT - Campus Lost & Found*\n\n")
                append(deviceInfo)
                append("\n📝 *Deskripsi Masalah:*\n")
                if (bugDescription.isNotBlank()) {
                    append(bugDescription)
                } else {
                    append("[Jelaskan masalah yang Anda temui di sini]")
                }
                append("\n\n📸 *Screenshot:* (lampirkan jika ada)")
            }
            
            val bugReportIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://wa.me/$BUG_REPORT_PHONE?text=${Uri.encode(message)}")
                setPackage("com.whatsapp")
            }
            
            if (bugReportIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(bugReportIntent)
            } else {
                // Fallback to web WhatsApp
                val webIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://wa.me/$BUG_REPORT_PHONE?text=${Uri.encode(message)}")
                }
                context.startActivity(webIntent)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membuka WhatsApp", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun getAppVersion(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }
}

