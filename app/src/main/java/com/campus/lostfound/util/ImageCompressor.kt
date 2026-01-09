package com.campus.lostfound.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object ImageCompressor {
    
    private const val TAG = "ImageCompressor"
    private const val MAX_SIZE_BYTES = 800 * 1024 // 800KB
    private const val MAX_DIMENSION = 1920
    
    /**
     * Compress image to max 800KB with quality optimization
     * Returns URI of compressed image, or null if failed
     */
    fun compressImage(context: Context, imageUri: Uri): Uri? {
        try {
            Log.d(TAG, "Starting compression for: $imageUri")
            
            val inputStream = context.contentResolver.openInputStream(imageUri)
            if (inputStream == null) {
                Log.e(TAG, "Failed to open input stream")
                return null
            }
            
            // Decode original image
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            
            if (originalBitmap == null) {
                Log.e(TAG, "Failed to decode bitmap")
                return null
            }
            
            Log.d(TAG, "Original image size: ${originalBitmap.width}x${originalBitmap.height}")
            
            // Fix image orientation based on EXIF data
            val rotatedBitmap = fixImageOrientation(context, imageUri, originalBitmap)
            
            // Resize if too large (max 1920x1920)
            val resizedBitmap = resizeImage(rotatedBitmap, MAX_DIMENSION)
            Log.d(TAG, "Resized to: ${resizedBitmap.width}x${resizedBitmap.height}")
            
            // Compress to target size
            val compressedFile = compressToTargetSize(context, resizedBitmap, MAX_SIZE_BYTES)
            Log.d(TAG, "Compressed file size: ${compressedFile.length()} bytes (${compressedFile.length() / 1024}KB)")
            
            // Clean up
            if (rotatedBitmap != originalBitmap) rotatedBitmap.recycle()
            if (resizedBitmap != rotatedBitmap) resizedBitmap.recycle()
            originalBitmap.recycle()
            
            return Uri.fromFile(compressedFile)
        } catch (e: Exception) {
            Log.e(TAG, "Compression failed", e)
            return null
        }
    }
    
    private fun fixImageOrientation(context: Context, imageUri: Uri, bitmap: Bitmap): Bitmap {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri) ?: return bitmap
            val exif = ExifInterface(inputStream)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            inputStream.close()
            
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                else -> return bitmap
            }
            
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            return bitmap
        }
    }
    
    private fun resizeImage(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= maxSize && height <= maxSize) {
            return bitmap
        }
        
        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int
        
        if (width > height) {
            newWidth = maxSize
            newHeight = (maxSize / ratio).toInt()
        } else {
            newHeight = maxSize
            newWidth = (maxSize * ratio).toInt()
        }
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
    
    private fun compressToTargetSize(context: Context, bitmap: Bitmap, maxSizeBytes: Int): File {
        val outputFile = File(context.cacheDir, "compressed_${System.currentTimeMillis()}.jpg")
        
        var quality = 95
        var outputStream: FileOutputStream
        var finalByteArray: ByteArray? = null
        
        // Try to compress with decreasing quality
        while (quality >= 10) {
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            
            Log.d(TAG, "Quality $quality: ${byteArray.size} bytes (${byteArray.size / 1024}KB)")
            
            if (byteArray.size <= maxSizeBytes) {
                finalByteArray = byteArray
                break
            }
            
            // Reduce quality and try again
            quality -= 10
        }
        
        // If still too large at quality 10, use it anyway (better than failing)
        if (finalByteArray == null) {
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 10, byteArrayOutputStream)
            finalByteArray = byteArrayOutputStream.toByteArray()
            Log.w(TAG, "Image still large at quality 10: ${finalByteArray.size} bytes")
        }
        
        // Save final result
        outputStream = FileOutputStream(outputFile)
        outputStream.write(finalByteArray)
        outputStream.close()
        
        Log.d(TAG, "Final compressed file: ${outputFile.absolutePath}, size: ${outputFile.length()} bytes")
        
        return outputFile
    }
}
