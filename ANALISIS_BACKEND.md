# 🔧 Analisis Backend & Database Architecture

## 🗄️ Database Structure (Firebase Firestore)

### Current Collections

#### 1. **items** Collection
Menyimpan semua laporan barang hilang/ditemukan.

**Document Schema:**
```kotlin
data class LostFoundItem(
    val id: String = "",                    // Document ID (auto-generated)
    val name: String = "",                  // Nama barang
    val description: String = "",           // Deskripsi detail
    val type: ItemType = ItemType.LOST,     // LOST atau FOUND
    val category: Category = Category.LAINNYA,
    val location: String = "",              // Lokasi kejadian
    val imageUrl: String = "",              // Base64 encoded image
    val imageStoragePath: String = "",      // Unused (legacy)
    val whatsappNumber: String = "",        // Nomor WA pelapor
    val userId: String = "",                // Anonymous auth UID
    val createdAt: Timestamp = Timestamp.now(),
    val completed: Boolean = false,         // Status selesai/belum
    val isCompleted: Boolean = false        // Duplicate field (cleanup needed)
)

enum class ItemType {
    LOST,    // Barang Hilang
    FOUND    // Barang Ditemukan
}

enum class Category {
    ELEKTRONIK,
    DOKUMEN,
    KENDARAAN,
    LAINNYA
    // TODO: Perlu diperluas seperti di rekomendasi
}
```

**Firestore Structure:**
```
/items
  /{itemId}
    - name: "Kunci motor"
    - description: "..."
    - type: "LOST" / "FOUND"
    - category: "LAINNYA"
    - location: "parkiran"
    - imageUrl: "data:image/jpeg;base64,/9j/4AAQ..." (very long)
    - imageStoragePath: ""
    - whatsappNumber: "628123456789"
    - userId: "anonymous_user_xyz123"
    - createdAt: Timestamp
    - completed: false
```

**Indexes:**
- `completed` + `createdAt` (descending) - untuk query homepage
- `userId` + `completed` + `createdAt` (descending) - untuk user's items
- Fallback: Client-side sorting jika index belum dibuat

---

### 2. **Local Storage** (DataStore / SharedPreferences)
Untuk data yang tidak perlu disimpan di cloud:

**notif_prefs:**
```
- lastSeen: Long (timestamp)
- current_user_id: String
```

**settings:**
```
- theme_mode: String (light/dark/system)
- theme_color: String (color code)
- language: String (id/en)
- push_enabled: Boolean
- sound_enabled: Boolean
```

**completed_reports (LocalHistoryRepository):**
```kotlin
data class CompletedReport(
    val item: LostFoundItem,
    val completedAt: Timestamp,
    val reason: String = ""
)
```
Disimpan di local JSON file menggunakan DataStore.

---

## 🔐 Authentication System

### Current: Anonymous Authentication
```kotlin
class LostFoundRepository {
    private val auth = FirebaseAuth.getInstance()
    
    fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: run {
            // Auto sign-in anonymously
            auth.signInAnonymously()
            auth.currentUser?.uid ?: UUID.randomUUID().toString()
        }
    }
    
    private suspend fun ensureAuthenticated(): String {
        auth.currentUser?.let { return it.uid }
        
        return suspendCancellableCoroutine { cont ->
            auth.signInAnonymously()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        cont.resume(auth.currentUser!!.uid)
                    } else {
                        cont.resumeWithException(task.exception!!)
                    }
                }
        }
    }
}
```

**Pros:**
- ✅ Tidak perlu registrasi, user langsung bisa pakai
- ✅ Gratis unlimited
- ✅ Simple implementation

**Cons:**
- ❌ User ID berubah jika clear data / uninstall app
- ❌ Tidak ada identitas user (nama, email, dll)
- ❌ Tidak bisa recovery data jika device hilang
- ❌ Sulit untuk moderasi dan ban user
- ❌ Tidak bisa build trust system (rating, reputation)

---

## 📡 Data Flow

### 1. Read Operations (Real-time Listeners)

**Home Screen - Get All Items:**
```kotlin
fun getAllItems(type: ItemType? = null): Flow<List<LostFoundItem>> {
    // 1. Ensure user authenticated
    ensureAuthenticated()
    
    // 2. Query Firestore
    val query = firestore.collection("items")
        .whereEqualTo("completed", false)  // Hanya yang belum selesai
        .orderBy("createdAt", Query.Direction.DESCENDING)
    
    // 3. Attach real-time listener
    return callbackFlow {
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Fallback: query tanpa orderBy, sort di client
                // ...
            }
            
            val items = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(LostFoundItem::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            
            // 4. Filter by type di client (jika perlu)
            val filtered = if (type != null) {
                items.filter { it.type == type }
            } else {
                items
            }
            
            // 5. Emit ke UI
            trySend(filtered)
        }
        
        awaitClose { listener.remove() }
    }
}
```

**Flow:**
```
[Firestore] → [Real-time Listener] → [Repository Flow] → [ViewModel StateFlow] → [UI Compose]
                                          ↓
                                    [Filter & Sort]
```

**Activity Screen - Get User Items:**
```kotlin
fun getUserItems(userId: String): Flow<List<LostFoundItem>> {
    ensureAuthenticated()
    
    val query = firestore.collection("items")
        .whereEqualTo("userId", userId)
        .whereEqualTo("completed", false)
        .orderBy("createdAt", Query.Direction.DESCENDING)
    
    // Similar flow...
}
```

**Completed Reports (Local Storage):**
```kotlin
fun getUserHistory(userId: String): Flow<List<LostFoundItem>> {
    return flow {
        localHistoryRepository.historyFlow.collect { completedReports ->
            val items = completedReports
                .filter { it.item.userId == userId }
                .map { it.item }
            emit(items)
        }
    }
}
```

---

### 2. Write Operations

**Add New Report:**
```kotlin
suspend fun addItem(item: LostFoundItem, imageUri: Uri?): Result<String> {
    try {
        // 1. Ensure authenticated
        val userId = ensureAuthenticated()
        
        // 2. Convert image to Base64
        val imageUrl = if (imageUri != null) {
            ImageConverter.uriToBase64(imageUri, context)
        } else ""
        
        // 3. Create item with user ID
        val finalItem = item.copy(
            userId = userId,
            imageUrl = imageUrl,
            createdAt = Timestamp.now(),
            completed = false
        )
        
        // 4. Save to Firestore
        val docRef = firestore.collection("items")
            .add(finalItem)
            .await()
        
        // 5. Trigger OneSignal notification (background)
        triggerOneSignalNotification(finalItem)
        
        return Result.success(docRef.id)
    } catch (e: Exception) {
        return Result.failure(e)
    }
}
```

**Flow:**
```
[User Input] → [ViewModel Validation] → [Repository] → [Firestore Write]
                                            ↓
                                    [OneSignal API]
                                            ↓
                                    [Push Notification]
```

**Update Report:**
```kotlin
suspend fun updateItem(itemId: String, item: LostFoundItem): Result<Unit> {
    try {
        firestore.collection("items")
            .document(itemId)
            .set(item)  // Overwrite entire document
            .await()
        
        return Result.success(Unit)
    } catch (e: Exception) {
        return Result.failure(e)
    }
}
```

**Mark as Completed:**
```kotlin
suspend fun markItemAsCompleted(itemId: String): Result<Unit> {
    try {
        // 1. Get item first
        val snapshot = firestore.collection("items")
            .document(itemId)
            .get()
            .await()
        
        val item = snapshot.toObject(LostFoundItem::class.java)
            ?.copy(id = itemId) ?: return Result.failure(Exception("Item not found"))
        
        // 2. Update Firestore (set completed = true)
        firestore.collection("items")
            .document(itemId)
            .update("completed", true)
            .await()
        
        // 3. Save to local history (optional for user to view later)
        localHistoryRepository.addCompletedReport(
            item.copy(completed = true),
            completionReason = ""
        )
        
        return Result.success(Unit)
    } catch (e: Exception) {
        return Result.failure(e)
    }
}
```

**Delete Report:**
```kotlin
suspend fun deleteItem(itemId: String): Result<Unit> {
    try {
        firestore.collection("items")
            .document(itemId)
            .delete()
            .await()
        
        return Result.success(Unit)
    } catch (e: Exception) {
        return Result.failure(e)
    }
}
```

---

## 🔔 Notification System

### 3-Layer Notification Architecture

#### Layer 1: OneSignal Push Notifications (Cloud)
```kotlin
// OneSignalNotificationService.kt
class OneSignalNotificationService(private val context: Context) {
    
    fun triggerNotification(item: LostFoundItem) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "https://onesignal.com/api/v1/notifications"
                val json = JSONObject().apply {
                    put("app_id", BuildConfig.ONESIGNAL_APP_ID)
                    put("included_segments", listOf("All"))
                    put("headings", JSONObject().put("en", "New Campus Report"))
                    put("contents", JSONObject().put("en", 
                        "${item.type.displayName}: ${item.name} di ${item.location}"
                    ))
                    put("data", JSONObject().apply {
                        put("itemId", item.id)
                        put("type", "new_report")
                    })
                }
                
                // Send HTTP POST to OneSignal API
                // ...
            } catch (e: Exception) {
                Log.e("OneSignal", "Failed to send notification", e)
            }
        }
    }
}
```

**Trigger:** Setiap kali ada laporan baru dibuat

**Target:** Semua users yang subscribe ke "All" segment

**Payload:**
```json
{
  "app_id": "xxx",
  "included_segments": ["All"],
  "headings": {"en": "New Campus Report"},
  "contents": {"en": "Hilang: Kunci motor di parkiran"},
  "data": {
    "itemId": "abc123",
    "type": "new_report"
  }
}
```

#### Layer 2: Firebase Cloud Messaging (Backup)
```kotlin
// FirebaseMessagingService
class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Handle FCM message
        val data = remoteMessage.data
        val itemId = data["itemId"]
        val type = data["type"]
        
        // Show local notification
        showNotification(remoteMessage.notification?.title, 
                        remoteMessage.notification?.body)
    }
}
```

**Subscribe Topics:**
- `all` - Semua notifikasi
- `campus_reports` - Laporan kampus

#### Layer 3: Local Notifications (Real-time Listener)
```kotlin
// LocalNotificationService.kt
class LocalNotificationService(private val context: Context) {
    
    private val notificationManager = context.getSystemService(
        Context.NOTIFICATION_SERVICE
    ) as NotificationManager
    
    fun showNewReportNotification(item: LostFoundItem) {
        // Skip if user is the reporter
        val currentUserId = getCurrentUserId()
        if (item.userId == currentUserId) return
        
        // Skip if seen before
        val prefs = context.getSharedPreferences("notif_prefs", Context.MODE_PRIVATE)
        val lastSeen = prefs.getLong("lastSeen", 0)
        if (item.createdAt.toDate().time <= lastSeen) return
        
        // Build notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Laporan Baru: ${item.type.displayName}")
            .setContentText("${item.name} di ${item.location}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(createPendingIntent(item.id))
            .build()
        
        notificationManager.notify(item.id.hashCode(), notification)
    }
}
```

**Trigger:** Firestore real-time listener deteksi DocumentChange.Type.ADDED

**Listener Setup (RealtimeNotificationListener):**
```kotlin
class RealtimeNotificationListener(
    context: Context,
    private val notificationService: LocalNotificationService
) {
    private val firestore = FirebaseFirestore.getInstance()
    
    fun startListening() {
        val query = firestore.collection("items")
            .whereEqualTo("completed", false)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
        
        itemsListener = query.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            
            for (change in snapshot.documentChanges) {
                when (change.type) {
                    DocumentChange.Type.ADDED -> {
                        val item = change.document.toObject(LostFoundItem::class.java)
                            .copy(id = change.document.id)
                        
                        notificationService.showNewReportNotification(item)
                    }
                    DocumentChange.Type.MODIFIED -> {
                        // Handle updates (e.g., marked completed)
                    }
                    else -> {}
                }
            }
        }
    }
    
    fun stopListening() {
        itemsListener?.remove()
    }
}
```

**Notification Flow:**
```
[New Report Created]
        ↓
    ┌───┴───┐
    │       │
    ↓       ↓
[OneSignal] [Firestore Write]
    ↓           ↓
[Push to All] [Real-time Listener]
                ↓
            [Local Notification]
```

---

## 💾 Image Storage Strategy

### Current: Base64 Encoding

**Pros:**
- ✅ **Gratis**: Tidak perlu Firebase Storage (Spark Plan)
- ✅ **Sederhana**: No upload/download logic
- ✅ **Offline-friendly**: Image tersimpan di document

**Cons:**
- ❌ **Size Limit**: Firestore document max 1MB (image harus <750KB after encoding)
- ❌ **Performance**: Large base64 strings slow down queries
- ❌ **Bandwidth**: Transfer full image setiap query (no caching)
- ❌ **Quality**: Harus compress image heavily

**Implementation:**
```kotlin
object ImageConverter {
    fun uriToBase64(uri: Uri, context: Context): String {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            
            // Compress to fit 1MB limit
            val compressedBitmap = compressBitmap(bitmap, maxSize = 600) // 600x600
            
            val outputStream = ByteArrayOutputStream()
            compressedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            
            val bytes = outputStream.toByteArray()
            return "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.DEFAULT)
        } catch (e: Exception) {
            return ""
        }
    }
    
    private fun compressBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val ratio = (maxSize.toFloat() / maxOf(bitmap.width, bitmap.height))
        val width = (bitmap.width * ratio).toInt()
        val height = (bitmap.height * ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }
}
```

### Alternative: Firebase Storage (Recommended untuk Scale)

**Schema Change:**
```kotlin
data class LostFoundItem(
    // ...
    val imageUrl: String = "",           // Firebase Storage download URL
    val imageStoragePath: String = "",   // gs://bucket/items/{itemId}/image1.jpg
    val thumbnailUrl: String = "",       // Smaller version for list view
    // ...
)
```

**Upload Flow:**
```kotlin
suspend fun uploadImage(uri: Uri, itemId: String): String {
    val storageRef = FirebaseStorage.getInstance().reference
    val imagePath = "items/$itemId/${System.currentTimeMillis()}.jpg"
    
    // Compress image
    val compressedUri = compressImage(uri)
    
    // Upload to Storage
    val uploadTask = storageRef.child(imagePath).putFile(compressedUri).await()
    
    // Get download URL
    val downloadUrl = uploadTask.storage.downloadUrl.await().toString()
    
    return downloadUrl
}
```

**Pros:**
- ✅ No size limit per file (up to 5GB)
- ✅ Better performance (CDN caching)
- ✅ Dapat simpan multiple images
- ✅ Bisa generate thumbnails (Cloud Functions)

**Cons:**
- ❌ Tidak gratis (Blaze Plan required)
- ❌ More complex implementation
- ❌ Perlu handle offline scenarios

**Cost Estimate:**
```
Assumptions:
- 1000 reports/month
- Average 2 images per report
- Average 500KB per image
- 10,000 views/month

Storage: 1000 * 2 * 0.5MB * 12 months = 12GB/year
Cost: 12GB * $0.026/GB = $0.31/year

Download: 10,000 * 2 * 0.5MB = 10GB/month
Cost: 10GB * $0.12/GB = $1.20/month = $14.40/year

Total: ~$15/year (sangat murah!)
```

---

## 🔍 Search Implementation

### Current: Client-side Filtering Only
```kotlin
// HomeViewModel.kt
val filteredItems = combine(
    allItems,
    searchQuery,
    selectedType
) { items, query, type ->
    items
        .filter { item ->
            // Filter by type
            type == null || item.type == type
        }
        .filter { item ->
            // Filter by search query (case-insensitive)
            if (query.isBlank()) true
            else {
                item.name.contains(query, ignoreCase = true) ||
                item.location.contains(query, ignoreCase = true) ||
                item.category.displayName.contains(query, ignoreCase = true)
            }
        }
}.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
```

**Pros:**
- ✅ Simple, no backend changes
- ✅ Gratis

**Cons:**
- ❌ Harus load semua data dulu
- ❌ Slow untuk dataset besar (>1000 items)
- ❌ No full-text search
- ❌ No fuzzy matching

### Recommended: Firestore Queries + Algolia

**Option 1: Firestore Compound Queries** (Good enough untuk <10K docs)
```kotlin
fun searchItems(query: String): Flow<List<LostFoundItem>> {
    val lowercaseQuery = query.lowercase()
    
    return callbackFlow {
        // Firestore doesn't support full-text search
        // Workaround: Use array-contains for keywords
        val snapshot = firestore.collection("items")
            .whereArrayContains("keywords", lowercaseQuery)
            .whereEqualTo("completed", false)
            .limit(50)
            .get()
            .await()
        
        // ...
    }
}

// Update Item schema to include keywords
data class LostFoundItem(
    // ...
    val keywords: List<String> = emptyList() // ["kunci", "motor", "parkiran"]
)
```

**Option 2: Algolia Search** (Best for scale)
```kotlin
// Add Algolia dependency
implementation("com.algolia:algoliasearch-android:3.+")

// Index items to Algolia whenever created/updated
suspend fun indexItemToAlgolia(item: LostFoundItem) {
    val client = ClientSearch(
        applicationID = "YOUR_APP_ID",
        apiKey = "YOUR_API_KEY"
    )
    val index = client.initIndex("items")
    
    index.saveObject(item.toAlgoliaObject())
}

// Search
suspend fun searchItems(query: String): List<LostFoundItem> {
    val results = index.search(query)
    return results.hits.map { it.deserialize(LostFoundItem.serializer()) }
}
```

**Algolia Pros:**
- ✅ Full-text search
- ✅ Typo tolerance
- ✅ Instant results
- ✅ Faceting & filtering
- ✅ Geo-search (jika ada GPS)

**Algolia Cons:**
- ❌ Paid service ($1/month untuk 10K records + 100K searches)
- ❌ Extra dependency

---

## 📊 Analytics & Monitoring

### Current: None (Manual checking in Firebase Console)

### Recommended: Firebase Analytics

```kotlin
// Initialize in Application class
class CampusLostFoundApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseAnalytics.getInstance(this)
    }
}

// Log events
class AnalyticsHelper(private val context: Context) {
    private val analytics = FirebaseAnalytics.getInstance(context)
    
    fun logReportCreated(item: LostFoundItem) {
        analytics.logEvent("report_created") {
            param("type", item.type.name)
            param("category", item.category.name)
            param("has_image", if (item.imageUrl.isNotEmpty()) "yes" else "no")
        }
    }
    
    fun logReportCompleted(itemId: String, daysToComplete: Int) {
        analytics.logEvent("report_completed") {
            param("item_id", itemId)
            param("days_to_complete", daysToComplete.toLong())
        }
    }
    
    fun logSearchPerformed(query: String, resultCount: Int) {
        analytics.logEvent("search") {
            param(FirebaseAnalytics.Param.SEARCH_TERM, query)
            param("result_count", resultCount.toLong())
        }
    }
}
```

**Key Metrics to Track:**
- Daily/Monthly Active Users
- Reports created (by type, category, location)
- Success rate (% completed)
- Average time to completion
- Search queries
- Popular categories
- Popular locations
- User retention

---

## 🔒 Security Rules

### Current Firestore Rules (Assumed)
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /items/{itemId} {
      // Allow read for all authenticated users
      allow read: if request.auth != null;
      
      // Allow create for authenticated users
      allow create: if request.auth != null
                    && request.resource.data.userId == request.auth.uid;
      
      // Allow update/delete only by owner
      allow update, delete: if request.auth != null
                            && resource.data.userId == request.auth.uid;
    }
  }
}
```

### Issues:
- ⚠️ Anonymous users bisa create unlimited reports (spam)
- ⚠️ No validation untuk data fields
- ⚠️ No rate limiting

### Recommended Rules dengan Validation:
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Helper functions
    function isAuthenticated() {
      return request.auth != null;
    }
    
    function isOwner(userId) {
      return isAuthenticated() && request.auth.uid == userId;
    }
    
    function isValidItem() {
      let data = request.resource.data;
      return data.name.size() > 0 && data.name.size() <= 100
          && data.location.size() > 0 && data.location.size() <= 100
          && data.whatsappNumber.matches('^628[0-9]{8,12}$')
          && data.imageUrl.size() < 1000000 // 1MB in base64 ~750KB original
          && data.type in ['LOST', 'FOUND']
          && data.userId == request.auth.uid;
    }
    
    // Items collection
    match /items/{itemId} {
      // Anyone can read non-completed items
      allow read: if isAuthenticated() && resource.data.completed == false;
      
      // Own items can always be read
      allow read: if isOwner(resource.data.userId);
      
      // Create with validation
      allow create: if isAuthenticated() 
                    && isValidItem()
                    && request.resource.data.createdAt == request.time;
      
      // Update only by owner, can't change userId or createdAt
      allow update: if isOwner(resource.data.userId)
                    && request.resource.data.userId == resource.data.userId
                    && request.resource.data.createdAt == resource.data.createdAt;
      
      // Delete only by owner
      allow delete: if isOwner(resource.data.userId);
    }
    
    // Future: Users collection untuk profile
    match /users/{userId} {
      allow read: if isAuthenticated();
      allow write: if isOwner(userId);
    }
    
    // Future: Messages collection untuk chat
    match /conversations/{conversationId} {
      allow read, write: if isAuthenticated() 
                         && request.auth.uid in resource.data.participants;
    }
  }
}
```

---

## 🚀 Performance Optimizations

### 1. **Pagination** (Sangat penting!)
```kotlin
// Current: Load all items at once (❌ bad for scale)
fun getAllItems(): Flow<List<LostFoundItem>>

// Better: Paginated loading
suspend fun getItemsPage(
    pageSize: Int = 20,
    lastDocument: DocumentSnapshot? = null
): Pair<List<LostFoundItem>, DocumentSnapshot?> {
    
    val query = firestore.collection("items")
        .whereEqualTo("completed", false)
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .limit(pageSize.toLong())
    
    val finalQuery = if (lastDocument != null) {
        query.startAfter(lastDocument)
    } else {
        query
    }
    
    val snapshot = finalQuery.get().await()
    val items = snapshot.documents.mapNotNull { 
        it.toObject(LostFoundItem::class.java)?.copy(id = it.id)
    }
    
    val lastVisible = snapshot.documents.lastOrNull()
    
    return Pair(items, lastVisible)
}
```

### 2. **Caching Strategy**
```kotlin
// Enable offline persistence
val settings = FirebaseFirestoreSettings.Builder()
    .setPersistenceEnabled(true)
    .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
    .build()

firestore.firestoreSettings = settings
```

### 3. **Image Loading Optimization**
```kotlin
// Current: Load full base64 every time
AsyncImage(model = item.imageUrl, ...)

// Better: Use Coil with memory cache
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data(item.imageUrl)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .diskCachePolicy(CachePolicy.ENABLED)
        .crossfade(true)
        .build(),
    contentDescription = null
)
```

### 4. **Debounce Search**
```kotlin
// ViewModel
val searchQuery = MutableStateFlow("")

val searchResults = searchQuery
    .debounce(300) // Wait 300ms after user stops typing
    .flatMapLatest { query ->
        if (query.length < 2) {
            flowOf(emptyList())
        } else {
            repository.searchItems(query)
        }
    }
    .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
```

---

## 📈 Scalability Considerations

### Current System Limits:
- ✅ **Up to 1,000 users**: OK
- ⚠️ **1,000 - 10,000 users**: Need pagination, better caching
- ❌ **10,000+ users**: Need major changes

### Changes Needed for Scale:

#### 1. **Database Optimization**
- ✅ Add composite indexes for common queries
- ✅ Implement pagination
- ✅ Use Firestore bundles for initial load
- ✅ Consider sharding large collections

#### 2. **Storage Strategy**
- ✅ Migrate to Firebase Storage
- ✅ Generate thumbnails (Cloud Functions)
- ✅ Implement CDN caching

#### 3. **Search**
- ✅ Integrate Algolia or Elasticsearch
- ✅ Implement search analytics

#### 4. **Authentication**
- ✅ Migrate to email/password + Google Sign-In
- ✅ Implement rate limiting per user
- ✅ Add spam detection

#### 5. **Notification Optimization**
- ✅ Segment users by interests (category, location)
- ✅ Implement notification preferences
- ✅ Add quiet hours

#### 6. **Backend Services (Cloud Functions)**
```javascript
// Auto-delete old completed reports (older than 6 months)
exports.cleanupOldReports = functions.pubsub
  .schedule('every 24 hours')
  .onRun(async (context) => {
    const sixMonthsAgo = new Date();
    sixMonthsAgo.setMonth(sixMonthsAgo.getMonth() - 6);
    
    const snapshot = await admin.firestore()
      .collection('items')
      .where('completed', '==', true)
      .where('createdAt', '<', sixMonthsAgo)
      .get();
    
    const batch = admin.firestore().batch();
    snapshot.docs.forEach(doc => batch.delete(doc.ref));
    await batch.commit();
    
    console.log(`Deleted ${snapshot.size} old reports`);
  });

// Generate thumbnails when image uploaded
exports.generateThumbnail = functions.storage
  .object()
  .onFinalize(async (object) => {
    // Use Sharp or ImageMagick to create thumbnail
    // Upload thumbnail to separate path
  });

// Send notification when new report created
exports.onReportCreated = functions.firestore
  .document('items/{itemId}')
  .onCreate(async (snap, context) => {
    const item = snap.data();
    
    // Send to OneSignal
    await sendOneSignalNotification(item);
    
    // Update user stats
    await updateUserStats(item.userId);
  });
```

---

## 🔄 Migration Path untuk Authentication

### Phase 1: Add User Collection (Parallel)
```kotlin
// Buat User collection baru tanpa break existing system
data class User(
    val id: String,
    val email: String = "",
    val name: String = "",
    val photoUrl: String = "",
    val anonymousId: String = "", // Link ke anonymous auth UID
    val createdAt: Timestamp = Timestamp.now()
)

// Migration function
suspend fun migrateAnonymousToEmail(email: String, password: String) {
    val currentUser = auth.currentUser ?: return
    
    // Link anonymous account dengan email
    val credential = EmailAuthProvider.getCredential(email, password)
    currentUser.linkWithCredential(credential).await()
    
    // Create User document
    val user = User(
        id = currentUser.uid,
        email = email,
        anonymousId = currentUser.uid
    )
    
    firestore.collection("users")
        .document(currentUser.uid)
        .set(user)
        .await()
}
```

### Phase 2: Gradual Migration
- Existing users tetap anonymous
- New users harus sign up dengan email
- Prompt existing users untuk upgrade account

### Phase 3: Deprecate Anonymous
- Setelah 6 bulan, require email authentication
- Migrate atau delete orphaned data

---

## 📋 Summary & Recommendations

### ✅ Current Strengths:
1. Simple architecture yang mudah di-maintain
2. Real-time updates dengan Firestore listeners
3. Multi-layer notification system
4. Offline-friendly dengan anonymous auth
5. Gratis di Spark Plan

### ⚠️ Current Weaknesses:
1. Anonymous auth = no user identity
2. Base64 images = size & performance limits
3. No search implementation
4. No pagination = performance issues at scale
5. Minimal security validation
6. No analytics

### 🎯 Priority Fixes:

#### 🔥 URGENT
1. **Add Email Authentication** - Security & trust
2. **Implement Pagination** - Performance
3. **Add Security Rules Validation** - Prevent spam
4. **Debounce Search** - UX improvement

#### ⚡ HIGH
5. **Migrate to Firebase Storage** - Better image quality
6. **Add User Profile System** - Essential for trust
7. **Implement Rate Limiting** - Prevent abuse
8. **Add Firebase Analytics** - Data-driven decisions

#### 📊 MEDIUM
9. **Integrate Algolia Search** - Better search UX
10. **Add Cloud Functions** - Automation
11. **Implement Caching Strategy** - Performance
12. **Add Admin Dashboard** - Moderation

### 💰 Cost Projection

**Current (Spark Plan - Free):**
- ✅ 0-1000 users: $0/month
- ⚠️ 1000-5000 users: Might hit limits

**After Upgrades (Blaze Plan - Pay as you go):**
```
Estimates for 5,000 active users:
- Firestore: ~$5/month
- Firebase Storage: ~$2/month
- Cloud Functions: ~$3/month
- Algolia: ~$1/month
- Hosting: ~$1/month
---------------------------
Total: ~$12/month = $144/year

Still very affordable!
```

---

**Next Steps:**
1. Review dan prioritaskan rekomendasi
2. Buat timeline implementation
3. Setup monitoring & analytics
4. Test dengan real load (stress testing)
5. Plan migration strategy untuk authentication

📧 Questions? Let's discuss!
