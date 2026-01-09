package com.campus.lostfound.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class LostFoundItem(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val userName: String = "",              // Nama pelapor (untuk ditampilkan)
    val userPhotoUrl: String = "",          // Foto pelapor (Google photo atau empty)
    val type: ItemType = ItemType.LOST,
    val itemName: String = "",
    val category: Category = Category.OTHER,
    val location: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val whatsappNumber: String = "",
    @PropertyName("completed")
    val isCompleted: Boolean = false,
    val completedAt: Timestamp? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val imageStoragePath: String = ""
) {
    fun getTimeAgo(): String {
        val now = System.currentTimeMillis()
        val created = createdAt.toDate().time
        val diff = now - created
        
        return when {
            diff < 60000 -> "Baru saja"
            diff < 3600000 -> "${diff / 60000} menit lalu"
            diff < 86400000 -> "${diff / 3600000} jam lalu"
            diff < 604800000 -> "${diff / 86400000} hari lalu"
            else -> "${diff / 604800000} minggu lalu"
        }
    }
}

enum class ItemType(val displayName: String) {
    LOST("Hilang"),
    FOUND("Ditemukan")
}

enum class Category(val displayName: String) {
    ELECTRONICS("Elektronik"),
    DOCUMENTS("Dokumen"),
    KEYS_ACCESSORIES("Kunci & Aksesoris"),
    BAGS_WALLETS("Tas & Dompet"),
    BOOKS_STATIONERY("Buku & Alat Tulis"),
    OTHER("Lainnya")
}

// Campus Locations Constants
object CampusLocations {
    val ALL_LOCATIONS = listOf(
        "Perpustakaan Pusat",
        "Parkiran Motor",
        "Parkiran Mobil",
        "Kantin Utama",
        "Kantin Fakultas",
        "Ruang Kelas",
        "Laboratorium",
        "Masjid/Musholla",
        "Lapangan Olahraga",
        "Auditorium",
        "Gedung Rektorat",
        "Gedung Fakultas",
        "Area Taman",
        "Toilet",
        "Sekitar Gerbang",
        "Lainnya"
    )
}

