package com.campus.lostfound.data.repository

import android.content.Context
import android.net.Uri
import com.campus.lostfound.data.model.LostFoundItem
import com.campus.lostfound.data.model.ItemType
import com.campus.lostfound.util.ImageConverter
import com.campus.lostfound.util.ItemCache
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Source
import com.google.firebase.Timestamp
import java.util.Date
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import android.util.Log
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.util.UUID

class LostFoundRepository(private val context: Context) {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val localHistoryRepository = com.campus.lostfound.data.LocalHistoryRepository(context)
    
    fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: run {
            // Create anonymous user if not exists
            auth.signInAnonymously().addOnCompleteListener { }
            auth.currentUser?.uid ?: UUID.randomUUID().toString()
        }
    }

    private suspend fun ensureAuthenticated(): String {
        auth.currentUser?.let { return it.uid }

        return suspendCancellableCoroutine { cont ->
            val task = auth.signInAnonymously()
            val listener = { completedTask: com.google.android.gms.tasks.Task<com.google.firebase.auth.AuthResult> ->
                if (completedTask.isSuccessful) {
                    val uid = auth.currentUser?.uid
                    if (uid != null) cont.resume(uid) else cont.resumeWithException(Exception("Failed to obtain uid after anonymous sign-in"))
                } else {
                    cont.resumeWithException(completedTask.exception ?: Exception("Anonymous sign-in failed"))
                }
            }

            task.addOnCompleteListener(listener)

            cont.invokeOnCancellation {
                // no-op: listener will be GC'ed
            }
        }
    }
    
    fun getAllItems(type: ItemType? = null): Flow<List<LostFoundItem>> = callbackFlow {
        // Ensure user is authenticated before attaching listeners to avoid immediate PERMISSION_DENIED
        try {
            ensureAuthenticated()
        } catch (ex: Exception) {
            Log.e("LostFoundRepo", "Auth failed before listening: ${ex.message}")
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val collection = firestore.collection("items")

        // Filter hanya items yang belum selesai untuk home screen
        // Completed items hanya muncul di ActivityScreen -> Riwayat
        val baseQuery = collection.whereEqualTo("completed", false)
            .orderBy("createdAt", Query.Direction.DESCENDING)

        val listenerRegistration = baseQuery.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("LostFoundRepo", "Query error: ${error.message}")
                // Jika error, coba query tanpa orderBy
                if (error.message?.contains("index") == true ||
                    error.message?.contains("FAILED_PRECONDITION") == true) {

                    Log.d("LostFoundRepo", "Using fallback query without orderBy")
                    // Fallback: query tanpa orderBy, filter completed=false
                    collection.whereEqualTo("completed", false)
                        .addSnapshotListener { fallbackSnapshot, fallbackError ->
                            if (fallbackError == null) {
                                val allItems = fallbackSnapshot?.documents?.mapNotNull { doc ->
                                    doc.toObject(LostFoundItem::class.java)?.copy(id = doc.id)
                                } ?: emptyList()

                                Log.d("LostFoundRepo", "Fallback query returned ${allItems.size} items")

                                // Filter dan sort di client side
                                val filtered = if (type != null) {
                                    allItems.filter { it.type == type }
                                } else {
                                    allItems
                                }

                                // Sort by createdAt descending
                                val sorted = filtered.sortedByDescending {
                                    it.createdAt.toDate().time
                                }

                                trySend(sorted)
                            } else {
                                Log.e("LostFoundRepo", "Fallback query also failed: ${fallbackError.message}")
                                // Jika masih error, kirim empty list but don't close the app
                                trySend(emptyList())
                            }
                        }
                    return@addSnapshotListener
                }
                // Error lain, emit empty list instead of closing flow to avoid crashing
                trySend(emptyList())
                return@addSnapshotListener
            }

            val items = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(LostFoundItem::class.java)?.copy(id = doc.id)
            } ?: emptyList()

            Log.d("LostFoundRepo", "Query returned ${items.size} items, filter type: $type")
            
            // ✅ Cache all items for faster detail screen loading
            ItemCache.setAll(items)

            // Filter by type di client side
            val filtered = if (type != null) {
                items.filter { it.type == type }
            } else {
                items
            }

            Log.d("LostFoundRepo", "After filtering: ${filtered.size} items")
            trySend(filtered)
        }

        awaitClose { listenerRegistration.remove() }
    }
    
    fun getUserItems(userId: String): Flow<List<LostFoundItem>> = callbackFlow {
        // Ensure authenticated before listening
        try {
            ensureAuthenticated()
        } catch (ex: Exception) {
            Log.e("LostFoundRepo", "Auth failed before userItems listen: ${ex.message}")
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val query = firestore.collection("items")
            .whereEqualTo("userId", userId)
            .whereEqualTo("completed", false)
            .orderBy("createdAt", Query.Direction.DESCENDING)

        val listenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Jika error karena index belum ada, gunakan fallback
                if (error.message?.contains("index") == true ||
                    error.message?.contains("FAILED_PRECONDITION") == true) {

                    // Fallback: query tanpa orderBy, sort di client
                    firestore.collection("items")
                        .whereEqualTo("userId", userId)
                        .whereEqualTo("completed", false)
                        .addSnapshotListener { fallbackSnapshot, fallbackError ->
                            if (fallbackError == null) {
                                val allItems = fallbackSnapshot?.documents?.mapNotNull { doc ->
                                    doc.toObject(LostFoundItem::class.java)?.copy(id = doc.id)
                                } ?: emptyList()

                                // Sort di client side
                                val sorted = allItems.sortedByDescending {
                                    it.createdAt.toDate().time
                                }

                                trySend(sorted)
                            } else {
                                trySend(emptyList())
                            }
                        }
                    return@addSnapshotListener
                }
                Log.e("LostFoundRepo", "UserItems query error: ${error.message}")
                trySend(emptyList())
                return@addSnapshotListener
            }

            val items = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(LostFoundItem::class.java)?.copy(id = doc.id)
            } ?: emptyList()

            trySend(items)
        }

        awaitClose { listenerRegistration.remove() }
    }

    /**
     * Get user history from LOCAL storage (device only)
     * Data akan hilang jika user hapus data/uninstall aplikasi
     */
    fun getUserHistory(userId: String): Flow<List<LostFoundItem>> {
        return kotlinx.coroutines.flow.flow {
            localHistoryRepository.historyFlow.collect { completedReports ->
                // Filter by userId (untuk keamanan) dan emit items saja
                val items = completedReports
                    .filter { it.item.userId == userId }
                    .map { it.item }
                emit(items)
            }
        }
    }
    
    /**
     * Get completed reports with completion date info
     */
    fun getCompletedReportsWithDate(): Flow<List<com.campus.lostfound.data.LocalHistoryRepository.CompletedReport>> {
        return localHistoryRepository.historyFlow
    }
    
    suspend fun addItem(item: LostFoundItem, imageUri: Uri?): Result<String> {
        return try {
            val userId = ensureAuthenticated()
            
            // Store current user ID for notification filtering
            val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("current_user_id", userId).apply()
            
            val itemWithUser = item.copy(userId = userId)
            
            // Convert image to Base64 if provided (opsional)
            val imageUrl = if (imageUri != null) {
                withContext(Dispatchers.IO) {
                    ImageConverter.uriToBase64(imageUri, context)
                }
            } else {
                ""
            }
            
            val finalItem = itemWithUser.copy(
                imageUrl = imageUrl,
                imageStoragePath = "", // Tidak perlu untuk Base64
                isCompleted = false // Pastikan false (akan disimpan sebagai "completed" di Firestore)
            )
            
            val docRef = firestore.collection("items").add(finalItem).await()
            android.util.Log.d("LostFoundRepo", "Item saved with ID: ${docRef.id}")

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteItem(itemId: String, imageStoragePath: String): Result<Unit> {
        return try {
            val userId = ensureAuthenticated()
            
            // Verify ownership
            val item = firestore.collection("items").document(itemId).get().await()
                .toObject(LostFoundItem::class.java)
            
            if (item == null) {
                return Result.failure(Exception("Laporan tidak ditemukan"))
            }
            
            if (item.userId != userId) {
                android.util.Log.w("LostFoundRepo", "Delete attempt by unauthorized user. Item userId: ${item.userId}, Current userId: $userId")
                return Result.failure(Exception("Anda tidak memiliki izin untuk menghapus laporan ini"))
            }
            
            // Note: Base64 images are stored in Firestore document
            // They will be automatically deleted when document is deleted
            // No need to delete separately
            
            // Delete document (image akan ikut terhapus)
            firestore.collection("items").document(itemId).delete().await()
            android.util.Log.d("LostFoundRepo", "Item deleted successfully: $itemId")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("LostFoundRepo", "Error deleting item: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun markAsCompleted(itemId: String): Result<Unit> {
        return try {
            val userId = ensureAuthenticated()

            // Verify ownership
            val itemDoc = firestore.collection("items").document(itemId).get().await()
            val item = itemDoc.toObject(LostFoundItem::class.java)?.copy(id = itemId)

            if (item?.userId != userId) {
                return Result.failure(Exception("Anda tidak memiliki izin untuk mengubah laporan ini"))
            }

            // Save to LOCAL history (device only)
            val savedLocally = localHistoryRepository.saveCompletedReport(item)
            if (!savedLocally) {
                Log.w("LostFoundRepo", "Failed to save to local history, but continuing...")
            }
            
            Log.d("LostFoundRepo", "Report saved to LOCAL history: ${item.itemName}")

            // UPDATE completed field in Firestore (instead of deleting)
            // This triggers MODIFIED event in listener, which sends completion notification
            val completedAt = com.google.firebase.Timestamp.now()
            firestore.collection("items").document(itemId)
                .update(mapOf(
                    "completed" to true,
                    "completedAt" to completedAt
                ))
                .await()
            
            Log.d("LostFoundRepo", "Report marked as completed in Firestore: $itemId")

            // 🆕 UPDATE USER STATS: Increment totalHelped only for FOUND reports
            // Konsep: User yang menemukan barang (FOUND) dan mengembalikan → helped++
            if (item.type == com.campus.lostfound.data.model.ItemType.FOUND) {
                val userRepository = com.campus.lostfound.data.repository.UserRepository()
                userRepository.updateStats(
                    userId = userId,
                    incrementHelped = 1
                )
                Log.d("LostFoundRepo", "✅ totalHelped incremented for user: $userId (FOUND report completed)")
            } else {
                Log.d("LostFoundRepo", "ℹ️ LOST report completed, no helped increment")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("LostFoundRepo", "Error marking as completed: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Delete report from local history
     */
    fun deleteFromLocalHistory(itemId: String): Boolean {
        return localHistoryRepository.deleteFromHistory(itemId)
    }
    
    /**
     * Clear all local history
     */
    fun clearLocalHistory(): Boolean {
        return localHistoryRepository.clearAllHistory()
    }
    
    suspend fun getItemById(itemId: String): Result<LostFoundItem> {
        return try {
            // ✅ 1. Check memory cache first (instant)
            ItemCache.get(itemId)?.let {
                Log.d("LostFoundRepo", "Cache HIT for item: $itemId")
                return Result.success(it)
            }
            
            Log.d("LostFoundRepo", "Cache MISS for item: $itemId, fetching...")
            
            // ✅ 2. Try Firestore CACHE first (offline support)
            try {
                val cachedDoc = firestore.collection("items")
                    .document(itemId)
                    .get(Source.CACHE)
                    .await()
                    
                if (cachedDoc.exists()) {
                    val cachedItem = cachedDoc.toObject(LostFoundItem::class.java)?.copy(id = cachedDoc.id)
                    if (cachedItem != null) {
                        ItemCache.set(itemId, cachedItem)
                        Log.d("LostFoundRepo", "Loaded from Firestore cache")
                        return Result.success(cachedItem)
                    }
                }
            } catch (cacheError: Exception) {
                Log.d("LostFoundRepo", "Firestore cache miss: ${cacheError.message}")
            }
            
            // ✅ 3. Fetch from Firestore SERVER
            val doc = firestore.collection("items").document(itemId).get().await()
            val item = doc.toObject(LostFoundItem::class.java)?.copy(id = doc.id)
            
            if (item != null) {
                // ✅ Save to cache
                ItemCache.set(itemId, item)
                Result.success(item)
            } else {
                // ✅ 4. Fallback to local history
                val localHistory = localHistoryRepository.getHistoryById(itemId)
                if (localHistory != null) {
                    val historyItem = localHistory.item.copy(isCompleted = true)
                    ItemCache.set(itemId, historyItem)
                    Result.success(historyItem)
                } else {
                    Result.failure(Exception("Laporan tidak ditemukan"))
                }
            }
        } catch (e: Exception) {
            // ✅ 5. Final fallback to local history if network fails
            try {
                val localHistory = localHistoryRepository.getHistoryById(itemId)
                if (localHistory != null) {
                    val historyItem = localHistory.item.copy(isCompleted = true)
                    ItemCache.set(itemId, historyItem)
                    Result.success(historyItem)
                } else {
                    Result.failure(e)
                }
            } catch (localError: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun updateItem(
        itemId: String,
        itemName: String? = null,
        category: com.campus.lostfound.data.model.Category? = null,
        location: String? = null,
        description: String? = null,
        whatsappNumber: String? = null,
        imageUri: Uri? = null
    ): Result<Unit> {
        return try {
            val userId = ensureAuthenticated()
            
            // Verify ownership
            val existingItem = firestore.collection("items").document(itemId).get().await()
                .toObject(LostFoundItem::class.java)
            
            if (existingItem?.userId != userId) {
                return Result.failure(Exception("Anda tidak memiliki izin untuk mengubah laporan ini"))
            }
            
            val updates = mutableMapOf<String, Any>()
            
            if (itemName != null) updates["itemName"] = itemName
            if (category != null) updates["category"] = category.name
            if (location != null) updates["location"] = location
            if (description != null) updates["description"] = description
            if (whatsappNumber != null) {
                // Format nomor ke format internasional
                val formattedNumber = com.campus.lostfound.util.WhatsAppUtil.formatPhoneNumber(whatsappNumber)
                updates["whatsappNumber"] = formattedNumber
            }
            
            // Update image jika ada
            if (imageUri != null) {
                val imageUrl = withContext(Dispatchers.IO) {
                    ImageConverter.uriToBase64(imageUri, context)
                }
                if (imageUrl.isNotEmpty()) {
                    updates["imageUrl"] = imageUrl
                }
            }
            
            if (updates.isNotEmpty()) {
                firestore.collection("items").document(itemId)
                    .update(updates).await()
                android.util.Log.d("LostFoundRepo", "Item updated: $itemId")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("LostFoundRepo", "Error updating item: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Clean up completed items older than 7 days
     * This should be called periodically (e.g., on app start)
     */
    suspend fun cleanupOldCompletedItems(): Result<Int> {
        return try {
            val sevenDaysAgo = Timestamp(Date(System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)))
            
            val snapshot = firestore.collection("items")
                .whereEqualTo("completed", true)
                .whereLessThan("completedAt", sevenDaysAgo)
                .get()
                .await()
            
            var deletedCount = 0
            snapshot.documents.forEach { doc ->
                try {
                    doc.reference.delete().await()
                    deletedCount++
                    Log.d("LostFoundRepo", "Deleted old completed item: ${doc.id}")
                } catch (e: Exception) {
                    Log.w("LostFoundRepo", "Failed to delete old completed item: ${doc.id}", e)
                }
            }
            
            if (deletedCount > 0) {
                Log.d("LostFoundRepo", "Cleanup: Deleted $deletedCount old completed items")
            }
            
            Result.success(deletedCount)
        } catch (e: Exception) {
            Log.e("LostFoundRepo", "Error during cleanup: ${e.message}", e)
            Result.failure(e)
        }
    }
}
