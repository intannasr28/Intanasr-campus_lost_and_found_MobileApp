package com.campus.lostfound.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class User(
    @DocumentId
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val photoUrl: String = "",
    
    // Verification fields (OPTIONAL - untuk mahasiswa)
    val isVerifiedStudent: Boolean = false,
    val nim: String = "",                    // Empty jika bukan mahasiswa
    val faculty: String = "",                // Empty jika bukan mahasiswa
    val department: String = "",             // Empty jika bukan mahasiswa
    val ktmPhotoUrl: String = "",           // Empty jika belum verify
    val verifiedAt: Timestamp? = null,      // Null jika belum verify
    
    // Stats & reputation
    val rating: Double = 0.0,
    val totalReports: Int = 0,          // Total LOST + FOUND yang dibuat
    val totalFound: Int = 0,            // Total FOUND yang dibuat (user menemukan barang)
    val totalHelped: Int = 0,           // Total laporan yang completed (dikembalikan)
    val totalReturned: Int = 0,         // Alias untuk totalHelped (backward compatibility)
    val totalReviews: Int = 0,
    
    // Privacy Settings
    val showPhonePublicly: Boolean = false,     // Privacy: tampilkan nomor di public profile
    val showEmailPublicly: Boolean = false,      // Privacy: tampilkan email di public profile
    
    // Timestamps
    val createdAt: Timestamp = Timestamp.now(),
    val lastActive: Timestamp = Timestamp.now()
) {
    // Helper untuk display name (Google atau Email)
    fun getDisplayName(): String {
        return name.ifEmpty { email.substringBefore("@") }
    }
    
    // Helper untuk initial avatar (untuk user tanpa foto Google)
    fun getInitial(): String {
        val displayName = getDisplayName()
        return displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    }
    
    // Helper untuk badge
    fun getBadge(): String {
        return when {
            totalReports >= 50 && rating >= 4.8 -> "🏆 Legend"
            totalReports >= 30 && rating >= 4.5 -> "✨ Hero"
            totalReports >= 16 && rating >= 4.0 -> "💫 Terpercaya"
            totalReports >= 6 -> "🌟 Aktif"
            else -> "⭐ Newbie"
        }
    }
    
    // Success rate percentage (helped / total reports)
    fun getSuccessRate(): Int {
        return if (totalReports > 0) {
            ((totalHelped.toDouble() / totalReports) * 100).toInt()
        } else {
            0
        }
    }
    
    // Check if user has Google photo
    fun hasGooglePhoto(): Boolean {
        return photoUrl.isNotEmpty() && photoUrl.startsWith("http")
    }
}
