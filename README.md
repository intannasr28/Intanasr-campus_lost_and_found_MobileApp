# Campus Lost & Found 🎓🔍

Aplikasi Android modern untuk membantu mahasiswa kampus melaporkan dan menemukan barang hilang atau ditemukan.

## ✨ Fitur Utama

### 🚀 UI/UX Modern
- **Splash Screen** dengan animasi logo profesional
- **Bottom Navigation** dengan indikator aktif yang responsif
- **Gradient Header** yang menarik di halaman Beranda
- **Filter Chips** interaktif dengan animasi
- **Card Design** konsisten dengan border radius 16dp

### 🏠 Halaman Beranda
- Header modern dengan gradient background
- Search bar dengan shadow dan animasi focus
- Icon notifikasi di header
- Filter: Semua, Hilang, Ditemukan
- List card laporan dengan gambar, badge status, dan tombol hubungi

### ➕ Tambah Laporan
- Form dengan icon di setiap input field
- Upload foto dengan preview modern
- Validasi nomor WhatsApp Indonesia
- Helper text yang jelas
- Tombol submit dengan loading indicator

### 📋 Aktivitas Saya
- Kelola laporan milik user
- Tombol aksi: Edit, Tandai Selesai, Hapus
- Card dengan visual berbeda untuk laporan user
- Dialog konfirmasi sebelum hapus

### 🔔 Notifikasi
- Icon lonceng di header Beranda dengan badge unread count
- List notifikasi dengan icon sesuai type (New Report, Completed, dll)
- Notifikasi disimpan lokal di device (device-specific)
- **Fitur Hapus Individual**: Icon tong sampah di setiap notifikasi
- **Hapus Semua**: Menu dropdown untuk clear all notifications
- Konfirmasi dialog sebelum menghapus
- **Device Specific**: Hapus notifikasi hanya di perangkat Anda, tidak mempengaruhi user lain
- Animasi smooth saat menghapus notifikasi
- Mark as read / Mark all as read
- Empty state illustration yang menarik

### ⚙️ Pengaturan
- Section jelas: Tema, Notifikasi, Privasi, Tentang
- Icon konsisten di setiap section
- Toggle untuk notifikasi
- Informasi privasi dan tentang aplikasi

### 🎨 Konsistensi Desain
- **Border Radius**: Card 16dp, Button 12dp, Input 12dp
- **Warna Status**: 
  - Hilang: Red (#EF5350 / #FFEBEE)
  - Ditemukan: Green (#66BB6A / #E8F5E9)
- **Font Hierarchy**: Bold untuk title, Regular untuk body
- **Icon Style**: Material Icons dengan size konsisten
- **Spacing**: 4dp, 8dp, 12dp, 16dp, 24dp

### 🎭 Animasi & Transisi
- Fade in/out antar halaman
- Slide up untuk halaman tambah
- Ripple effect pada button
- Scale animation pada icon bottom navigation
- Content size animation pada card

## 🛠️ Teknologi

- **Kotlin** - Bahasa pemrograman
- **Jetpack Compose** - UI Framework modern
- **Firebase Firestore** - Database real-time
- **Firebase Auth** - Autentikasi anonim
- **Coil** - Image loading
- **Material 3** - Design system
- **Navigation Compose** - Navigasi antar screen
- **ViewModel** - State management
- **Coroutines** - Async operations

## 📱 Screenshot

*(Tambahkan screenshot aplikasi Anda di sini)*

## 🎯 Target Pengguna

Mahasiswa kampus yang ingin:
- Melaporkan barang hilang
- Melaporkan barang ditemukan
- Mencari barang yang hilang
- Membantu mengembalikan barang temuan

## 🔒 Privasi

- Data bersifat anonim
- Nomor WhatsApp hanya digunakan untuk komunikasi terkait laporan
- Tidak ada pengumpulan data pribadi lainnya

## 👨‍💻 Developer

Tim Kampus - Aplikasi ini dibuat sebagai project kampus untuk membantu komunitas mahasiswa.

## 📄 Lisensi

Aplikasi ini dibuat untuk keperluan edukasi dan pengembangan portofolio.

## 🚀 Cara Build

1. Clone repository
2. Buka dengan Android Studio
3. Sync Gradle
4. Setup Firebase: minta `google-services.json` dari pengembang utama dan taruh file tersebut di `app/`.
  - Repo tidak menyimpan `google-services.json` untuk keamanan.
  - Ada file `app/google-services.json.template` sebagai contoh/instruksi.
5. Run aplikasi

## 📝 Catatan Versi

### Version 1.0.0 (Current)
- ✅ Splash screen dengan animasi
- ✅ Bottom navigation responsif
- ✅ CRUD laporan lengkap
- ✅ Upload gambar dengan preview
- ✅ Integrasi WhatsApp
- ✅ Filter dan search
- ✅ UI/UX modern dan konsisten
- ✅ Animasi dan transisi halus

---

**Made with ❤️ for Campus Community**
