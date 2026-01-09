# OneSignal Setup Guide - Campus Lost & Found

## 📋 **Step 1: Create OneSignal Account**

1. **Buka browser** → https://onesignal.com/
2. Click **"Get Started Free"**
3. Sign up dengan:
   - Email kampus Anda
   - Password yang kuat
   - Verify email

---

## 📱 **Step 2: Create New App**

1. **Login** ke OneSignal Dashboard
2. Click **"New App/Website"**
3. Fill in:
   - **App Name**: `Campus Lost & Found`
   - **Category**: `Utilities`
   - **Platform**: Select **Android**

4. Click **"Next: Configure Your Platform"**

---

## 🔧 **Step 3: Android Configuration (UPDATED - FCM v1)**

### **✅ Anda Sudah Punya File JSON!**

File yang sudah di-download:
- **Nama file**: `campus-lost-and-found-1f5ca-firebase-adminsdk-fbsvc-c2928a32ae.json`
- **Lokasi**: `C:\Users\DELL\Downloads\`
- **Status**: ✅ Ready untuk upload ke OneSignal

---

### **📱 STEP-BY-STEP: Setup OneSignal dengan Service Account JSON**

#### **STEP 1: Buka OneSignal Dashboard**

1. **Buka browser baru** atau kembali ke tab OneSignal
2. **URL**: https://dashboard.onesignal.com/apps/bff31d8e-34a5-4b4a-860b-3b7798604916
3. Jika belum login, login dulu dengan akun OneSignal Anda

---

#### **STEP 2: Masuk ke Settings → Keys & IDs**

1. Di **sidebar kiri**, klik **"Settings"** (icon ⚙️)
2. Klik **"Keys & IDs"**
3. Scroll ke section **"Google Android (FCM)"** atau **"Android Configuration"**

**ATAU jika baru setup:**

1. Dari dashboard utama, akan ada wizard setup
2. Pilih **"Configure"** atau **"Set up Android Platform"**

---

#### **STEP 3: Pilih Configuration Method**

Di halaman konfigurasi Android, akan ada pilihan:

**Option 1: Upload Service Account JSON** ✅ (Pilih ini!)
- Klik button **"Upload JSON"** atau **"Choose File"**
- Browse ke: `C:\Users\DELL\Downloads\`
- Pilih file: `campus-lost-and-found-1f5ca-firebase-adminsdk-fbsvc-c2928a32ae.json`
- Klik **"Open"**

**ATAU**

**Option 2: Copy-Paste JSON Content**
- Klik **"Enter JSON manually"** atau ada text box besar
- Buka file JSON di VS Code atau Notepad
- **Copy semua isi file** (dari `{` pertama sampai `}` terakhir)
- **Paste** ke text box di OneSignal

---

#### **STEP 4: Input Firebase Sender ID**

Masih di halaman yang sama, akan ada field:

**"Firebase Sender ID"** atau **"FCM Sender ID"**

**Input nilai ini**: `608664009990`

---

#### **STEP 5: Save Configuration**

1. Klik button **"Save"** atau **"Save & Continue"** (biasanya di bawah form)
2. Tunggu beberapa detik sampai muncul notifikasi **"Configuration saved successfully"** ✅
3. Jika ada error, cek apakah file JSON ter-upload dengan benar

---

#### **STEP 6: Copy OneSignal App ID**

Setelah save berhasil, akan muncul:

**OneSignal App ID**: `bff31d8e-34a5-4b4a-860b-3b7798604916`

**Cara cek App ID:**

**Metode 1 (dari URL):**
```
https://dashboard.onesignal.com/apps/bff31d8e-34a5-4b4a-860b-3b7798604916
                                    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
                                    INI APP ID ANDA!
```

**Metode 2 (dari Settings):**
- Sidebar kiri → **Settings** → **Keys & IDs**
- Lihat section **"OneSignal App ID"**
- **Copy ID tersebut**

**App ID Anda**: `bff31d8e-34a5-4b4a-860b-3b7798604916`

---

#### **STEP 7: Update local.properties**

1. **Buka VS Code**
2. **Buka file**: `local.properties` (di root project)
3. **Tambahkan baris ini di bawah semua isi file**:

```properties
onesignal.app.id=bff31d8e-34a5-4b4a-860b-3b7798604916
```

**Contoh lengkap isi file**:
```properties
sdk.dir=C\:\\Users\\DELL\\AppData\\Local\\Android\\Sdk
onesignal.app.id=bff31d8e-34a5-4b4a-860b-3b7798604916
```

4. **Save file** (Ctrl+S)

---

#### **STEP 8: Sync Gradle Project**

1. **Di Android Studio** (atau VS Code dengan Gradle extension)
2. Klik **"Sync Project with Gradle Files"** 
   - Icon: 🐘 dengan panah melingkar
   - Atau: File → Sync Project with Gradle Files
3. **Tunggu sync selesai** (lihat di bawah ada progress bar)

---

### **✅ Checklist - Pastikan Semua Sudah:**

- [x] ✅ File JSON downloaded: `campus-lost-and-found-1f5ca-firebase-adminsdk-fbsvc-c2928a32ae.json`
- [ ] ⏳ OneSignal Dashboard dibuka
- [ ] ⏳ Upload/Paste JSON ke OneSignal
- [ ] ⏳ Input Sender ID: `608664009990`
- [ ] ⏳ Save configuration
- [ ] ⏳ Copy App ID: `bff31d8e-34a5-4b4a-860b-3b7798604916`
- [ ] ⏳ Update `local.properties` dengan App ID
- [ ] ⏳ Sync Gradle

---

### **🚨 Troubleshooting**

**Error: "Invalid JSON format"**
- Pastikan copy **SEMUA isi file JSON** (termasuk `{` dan `}`)
- Jangan ada karakter tambahan di awal/akhir
- Coba upload file langsung instead of copy-paste

**Error: "Invalid Sender ID"**
- Pastikan Sender ID: `608664009990` (11 digit angka)
- Tidak ada spasi di awal/akhir

**Cannot find Settings menu**
- Dari dashboard, cari icon ⚙️ di sidebar kiri
- Atau klik nama app di atas → Settings

---

## 🔑 **Step 4: Get Your App ID & REST API Key**

Setelah setup selesai, Anda akan melihat:

```
OneSignal App ID: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
REST API Key: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

**SIMPAN KEDUA KEY INI!** Anda akan perlu:
- **App ID** → untuk Android code
- **REST API Key** → untuk trigger notifications dari Firestore

---

## 📝 **Step 5: Update Android Code**

Setelah dapat **App ID**, update file berikut:

### **File: local.properties**

Tambahkan di bawah file:
```properties
onesignal.app.id=YOUR_ONESIGNAL_APP_ID_HERE
```

Contoh:
```properties
sdk.dir=C\:\\Users\\DELL\\AppData\\Local\\Android\\Sdk
onesignal.app.id=12345678-1234-1234-1234-123456789012
```

---

## ✅ **Verification Checklist**

Sebelum lanjut coding, pastikan sudah punya:

- [ ] OneSignal Account (verified email)
- [ ] New App created in OneSignal
- [ ] Firebase Server Key & Sender ID configured
- [ ] **OneSignal App ID** (xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx)
- [ ] **REST API Key** (untuk Firestore integration)
- [ ] App ID sudah di-paste ke `local.properties`

---

## 🚀 **Next Steps**

Setelah semua setup di atas selesai:

1. **Sync Gradle** (saya akan bantu setup code)
2. **Test notification** dari OneSignal Dashboard
3. **Integrate dengan Firestore** untuk auto-trigger

---

## ❓ **Troubleshooting**

### **Problem: Can't find Server Key**

**Solution:**
- Firebase Console → Project Settings
- Tab: **Cloud Messaging**
- Scroll ke **Cloud Messaging API (Legacy)**
- Klik **"⋮"** → **Manage API in Google Cloud Console**
- Enable **"Cloud Messaging API"**

### **Problem: No Sender ID**

**Solution:**
- Same page: Cloud Messaging
- Look for **"Sender ID"** (angka panjang)
- Copy angka tersebut

---

## 📞 **Need Help?**

Setelah dapat App ID, lanjut ke implementasi code. Saya akan:
1. Setup OneSignal SDK di MainActivity
2. Create service untuk trigger notifications
3. Integrate dengan Firestore listener
4. Test end-to-end flow

**Status**: ⏸️ Waiting for OneSignal App ID

**Next**: Update `local.properties` dengan App ID, lalu sync Gradle
