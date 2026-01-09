package com.campus.lostfound.util

import com.campus.lostfound.data.model.User

/**
 * In-memory cache for user profile data
 * Reduces Firestore reads and improves performance
 * Cache expires after 5 minutes for data freshness
 */
object UserCache {
    private var cachedUser: User? = null
    private var cacheTime: Long = 0
    private const val CACHE_DURATION_MS = 5 * 60 * 1000L // 5 minutes
    
    /**
     * Get cached user if available and not expired
     * @return User object if cache is valid, null otherwise
     */
    fun get(): User? {
        if (System.currentTimeMillis() - cacheTime > CACHE_DURATION_MS) {
            // Cache expired
            cachedUser = null
            return null
        }
        return cachedUser
    }
    
    /**
     * Save user to cache with current timestamp
     * @param user User object to cache
     */
    fun set(user: User) {
        cachedUser = user
        cacheTime = System.currentTimeMillis()
    }
    
    /**
     * Clear cache (called on logout or when data needs refresh)
     */
    fun clear() {
        cachedUser = null
        cacheTime = 0
    }
    
    /**
     * Check if cache is valid (not expired)
     * @return true if cache exists and not expired
     */
    fun isValid(): Boolean {
        return cachedUser != null && 
               (System.currentTimeMillis() - cacheTime) <= CACHE_DURATION_MS
    }
}
