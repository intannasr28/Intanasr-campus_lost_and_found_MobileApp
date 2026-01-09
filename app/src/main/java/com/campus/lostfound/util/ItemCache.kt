package com.campus.lostfound.util

import com.campus.lostfound.data.model.LostFoundItem

/**
 * In-memory cache for LostFoundItem to reduce Firestore reads
 * Cache expires after 3 minutes for data freshness
 */
object ItemCache {
    private val cachedItems = mutableMapOf<String, CachedItem>()
    private const val CACHE_DURATION_MS = 3 * 60 * 1000L // 3 minutes
    
    data class CachedItem(
        val item: LostFoundItem,
        val timestamp: Long
    )
    
    /**
     * Get cached item by ID if available and not expired
     */
    fun get(itemId: String): LostFoundItem? {
        val cached = cachedItems[itemId] ?: return null
        
        if (System.currentTimeMillis() - cached.timestamp > CACHE_DURATION_MS) {
            // Cache expired, remove it
            cachedItems.remove(itemId)
            return null
        }
        
        return cached.item
    }
    
    /**
     * Save item to cache with current timestamp
     */
    fun set(itemId: String, item: LostFoundItem) {
        cachedItems[itemId] = CachedItem(
            item = item,
            timestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * Save multiple items to cache at once
     */
    fun setAll(items: List<LostFoundItem>) {
        val currentTime = System.currentTimeMillis()
        items.forEach { item ->
            cachedItems[item.id] = CachedItem(
                item = item,
                timestamp = currentTime
            )
        }
    }
    
    /**
     * Clear cache for specific item
     */
    fun remove(itemId: String) {
        cachedItems.remove(itemId)
    }
    
    /**
     * Clear all cached items
     */
    fun clear() {
        cachedItems.clear()
    }
    
    /**
     * Remove expired items from cache
     */
    fun cleanExpired() {
        val currentTime = System.currentTimeMillis()
        val expired = cachedItems.filter { (_, cached) ->
            currentTime - cached.timestamp > CACHE_DURATION_MS
        }
        expired.keys.forEach { cachedItems.remove(it) }
    }
}
