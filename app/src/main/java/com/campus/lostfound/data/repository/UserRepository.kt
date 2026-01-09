package com.campus.lostfound.data.repository

import com.campus.lostfound.data.model.User
import com.campus.lostfound.util.UserCache
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val usersCollection = firestore.collection("users")
    
    // Get current user profile with cache
    suspend fun getCurrentUserProfile(): Result<User> {
        // ✅ Check cache first
        UserCache.get()?.let { 
            return Result.success(it) 
        }
        
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
            val snapshot = usersCollection.document(userId).get().await()
            
            if (snapshot.exists()) {
                val user = snapshot.toObject(User::class.java)
                if (user != null) {
                    // ✅ Save to cache
                    UserCache.set(user)
                    Result.success(user)
                } else {
                    Result.failure(Exception("Failed to parse user data"))
                }
            } else {
                Result.failure(Exception("User profile not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Get user profile by ID (untuk public profile)
    suspend fun getUserProfile(userId: String): Result<User> {
        return try {
            val snapshot = usersCollection.document(userId).get().await()
            
            if (snapshot.exists()) {
                val user = snapshot.toObject(User::class.java)
                if (user != null) {
                    Result.success(user)
                } else {
                    Result.failure(Exception("Failed to parse user data"))
                }
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Create or update user profile (dipanggil saat register/login)
    suspend fun createOrUpdateProfile(
        userId: String,
        email: String,
        name: String = "",
        photoUrl: String = ""
    ): Result<Unit> {
        return try {
            // Check if profile already exists
            val existingProfile = usersCollection.document(userId).get().await()
            
            if (existingProfile.exists()) {
                // Update existing profile (jika login Google, update foto & nama)
                val updates = mutableMapOf<String, Any>(
                    "lastActive" to Timestamp.now()
                )
                
                // Update nama & foto jika dari Google Sign-In
                if (name.isNotEmpty()) {
                    updates["name"] = name
                }
                if (photoUrl.isNotEmpty()) {
                    updates["photoUrl"] = photoUrl
                }
                
                usersCollection.document(userId).update(updates).await()
            } else {
                // Create new profile
                val newUser = User(
                    id = userId,
                    email = email,
                    name = name,
                    photoUrl = photoUrl,
                    createdAt = Timestamp.now(),
                    lastActive = Timestamp.now()
                )
                usersCollection.document(userId).set(newUser).await()
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Update profile (nama, NIM, fakultas, dll)
    suspend fun updateProfile(
        name: String? = null,
        nim: String? = null,
        faculty: String? = null,
        department: String? = null,
        phoneNumber: String? = null,
        showPhonePublicly: Boolean? = null,
        showEmailPublicly: Boolean? = null
    ): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
            
            val updates = mutableMapOf<String, Any>()
            name?.let { updates["name"] = it }
            nim?.let { updates["nim"] = it }
            faculty?.let { updates["faculty"] = it }
            department?.let { updates["department"] = it }
            phoneNumber?.let { updates["phoneNumber"] = it }
            showPhonePublicly?.let { updates["showPhonePublicly"] = it }
            showEmailPublicly?.let { updates["showEmailPublicly"] = it }
            
            if (updates.isNotEmpty()) {
                usersCollection.document(userId).update(updates).await()
                
                // ✅ Clear cache after update so ProfileScreen shows fresh data
                com.campus.lostfound.util.UserCache.clear()
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Update user stats (dipanggil saat create/complete laporan)
    suspend fun updateStats(
        userId: String,
        incrementReports: Int = 0,
        incrementFound: Int = 0,
        incrementHelped: Int = 0
    ): Result<Unit> {
        return try {
            val userRef = usersCollection.document(userId)
            
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val currentReports = snapshot.getLong("totalReports")?.toInt() ?: 0
                val currentFound = snapshot.getLong("totalFound")?.toInt() ?: 0
                val currentHelped = snapshot.getLong("totalHelped")?.toInt() ?: 0
                
                transaction.update(userRef, mapOf(
                    "totalReports" to (currentReports + incrementReports),
                    "totalFound" to (currentFound + incrementFound),
                    "totalHelped" to (currentHelped + incrementHelped),
                    "totalReturned" to (currentHelped + incrementHelped), // Backward compatibility
                    "lastActive" to Timestamp.now()
                ))
            }.await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Get user stats for ProfileScreen
    suspend fun getUserStats(userId: String): Result<Triple<Int, Int, Int>> {
        return try {
            val snapshot = usersCollection.document(userId).get().await()
            
            if (snapshot.exists()) {
                val totalReports = snapshot.getLong("totalReports")?.toInt() ?: 0
                val totalFound = snapshot.getLong("totalFound")?.toInt() ?: 0
                val totalHelped = snapshot.getLong("totalHelped")?.toInt() ?: 0
                
                Result.success(Triple(totalReports, totalFound, totalHelped))
            } else {
                Result.success(Triple(0, 0, 0))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
