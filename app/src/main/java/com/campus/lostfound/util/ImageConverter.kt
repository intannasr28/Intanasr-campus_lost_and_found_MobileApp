package com.campus.lostfound.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.InputStream

object ImageConverter {
    /**
     * Convert image URI to Base64 string
     * Returns empty string if conversion fails
     */
    suspend fun uriToBase64(imageUri: Uri, context: Context, thresholdBytes: Int = 1_000_000, targetBytes: Int = 800_000): String {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
            if (inputStream == null) return ""

            val originalBytes = inputStream.readBytes()
            inputStream.close()

            val mimeType = context.contentResolver.getType(imageUri) ?: "image/*"

            // If already below threshold, return original bytes as Base64 (no recompression)
            if (originalBytes.size <= thresholdBytes) {
                val base64String = Base64.encodeToString(originalBytes, Base64.NO_WRAP)
                return "data:$mimeType;base64,$base64String"
            }

            // Otherwise decode bitmap and compress to target size
            val bitmap = BitmapFactory.decodeByteArray(originalBytes, 0, originalBytes.size)
            if (bitmap == null) return ""

            var quality = 90
            var baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            var bytes = baos.toByteArray()

            while (bytes.size > targetBytes && quality > 20) {
                quality -= 5
                baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
                bytes = baos.toByteArray()
            }

            val base64String = Base64.encodeToString(bytes, Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64String"
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * Compress bitmap to target size
     */
    // kept for backward compatibility but not used by uriToBase64 anymore
    private fun compressBitmap(bitmap: Bitmap, maxSizeKB: Int): Bitmap {
        var quality = 90
        var stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        var sizeKB = stream.size() / 1024

        while (sizeKB > maxSizeKB && quality > 30) {
            quality -= 10
            stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            sizeKB = stream.size() / 1024
        }

        return bitmap
    }
    
    /**
     * Check if string is Base64 image data URL
     */
    fun isBase64Image(url: String): Boolean {
        return url.startsWith("data:image")
    }
    
    /**
     * Extract Base64 string from data URL
     */
    fun extractBase64(dataUrl: String): String {
        return if (dataUrl.contains(",")) {
            dataUrl.substringAfter(",")
        } else {
            dataUrl
        }
    }
}

