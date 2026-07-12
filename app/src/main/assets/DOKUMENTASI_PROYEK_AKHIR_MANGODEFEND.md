# Dokumentasi Teknis Proyek Akhir: MangoDefend Antivirus

Dokumen ini disusun untuk memberikan penjelasan detail mengenai arsitektur, fitur, dan implementasi teknis aplikasi **MangoDefend** sebagai bahan penyusunan Laporan Proyek Akhir (PA).

---

## 1. Ringkasan Proyek (Abstract)
**MangoDefend** adalah aplikasi antivirus berbasis Android yang mengintegrasikan teknologi **Machine Learning (On-Device)** untuk deteksi malware secara real-time dan **Sistem Cloud (Backend)** untuk manajemen riwayat, sinkronisasi data, dan fitur langganan premium. Fokus utama pengembangan adalah pada performa deteksi lokal yang cepat serta keamanan data yang terpusat.

---

## 2. Arsitektur Sistem
Aplikasi menggunakan arsitektur **MVVM (Model-View-ViewModel)** di sisi frontend Android dan berkomunikasi dengan backend melalui **RESTful API**.

### Komponen Utama:
1.  **Frontend (Android):** Jetpack Compose, Kotlin Coroutines, StateFlow.
2.  **ML Engine:** ONNX Runtime Android (Inference model `.onnx` langsung di perangkat).
3.  **Local Storage:** Room Database (Offline-first data riwayat).
4.  **Networking:** Retrofit & OkHttp dengan mekanisme JWT Authentication.
5.  **Background Processing:** WorkManager (untuk Full Scan) & Foreground Service (untuk Real-time Monitor).

---

## 3. Detail Fitur & Alur Kerja (Workflow)

### A. Autentikasi Pengguna (Integrasi Multi-Level)
Sistem ini menggunakan autentikasi tiga lapis: Google -> Firebase -> Private Backend.
*   **Alur Kerja:**
    1.  User melakukan **Google Sign-In** di aplikasi.
    2.  Aplikasi mendapatkan ID Token dan memvalidasinya ke **Firebase**.
    3.  Aplikasi mengirimkan Firebase Token ke API `/auth/firebase-login`.
    4.  Backend memberikan **JWT (JSON Web Token)** yang disimpan di `TokenManager` menggunakan SharedPreferences.
*   **Fungsi:** Mengamankan akses API dan mempersonalisasi riwayat scan antar perangkat.

### B. Deteksi Malware (ML-Based Detection)
Fitur inti yang menjawab rumusan masalah tentang integrasi ML secara real-time.
*   **Alur Kerja:**
    1.  **Preprocessing:** File mentah diubah menjadi citra biner melalui `BinaryImagePreprocessor`.
    2.  **Fingerprinting:** Aplikasi menghitung nilai **Hash SHA-256** dari file tersebut (`FileUtils.calculateFileHash`).
    3.  **Inference:** Model ONNX memberikan skor probabilitas malware.
    4.  **Sinkronisasi:** Hasil klasifikasi + Nilai Hash + File diupload ke backend via endpoint `/scans` secara asinkron.

### C. Pemindaian Menyeluruh (Full System Scan)
Fitur untuk memindai ribuan file tanpa mengganggu stabilitas aplikasi.
*   **Teknologi:** Menggunakan **WorkManager** (`ScanWorker.kt`).
*   **Alur Kerja:** 
    1.  Aplikasi mengiterasi seluruh folder publik (Download, Documents) dan APK terinstal.
    2.  Proses berjalan di background thread (`Dispatchers.IO`).
    3.  Pembaruan progres ke UI dibatasi (*throttling*) setiap 500ms agar performa tetap optimal (Poin 3 Rumusan Masalah).

### D. Perlindungan Real-Time (Active Monitoring)
*   **Teknologi:** Android **Foreground Service** & **FileObserver**.
*   **Alur Kerja:**
    1.  Aplikasi menjalankan service persisten dengan notifikasi.
    2.  Setiap ada file baru masuk atau instalasi aplikasi, aplikasi langsung memicu deteksi cepat.
    3.  Jika terindikasi berbahaya, sistem mengirimkan **Notifikasi Peringatan** instan ke pengguna.

### E. Manajemen Langganan & Transaksi
Fitur untuk mendukung model bisnis aplikasi antivirus.
*   **Alur Kerja:**
    1.  Ambil katalog paket (`GET /subscriptions/plan`).
    2.  Proses checkout (`POST /transactions/checkout`).
    3.  Tampilkan metode pembayaran (QRIS/VA) dari server.
    4.  Validasi status aktif untuk membuka limit pemindaian (User Free vs Pro).

---

## 4. Jawaban Atas Rumusan Masalah

| Rumusan Masalah | Solusi Implementasi dalam Proyek |
| :--- | :--- |
| **1. UI Informatif & Efisien** | Implementasi Dashboard berbasis Jetpack Compose dengan indikator warna (Red/Yellow/Green) untuk status keamanan dan `LinearProgressIndicator` untuk progres scan. |
| **2. Integrasi Backend & Hash** | Penggunaan Retrofit untuk sinkronisasi. Penambahan fungsi `calculateFileHash` (SHA-256) untuk mengirimkan sidik jari digital file ke server. |
| **3. Performa Optimal** | Penggunaan Kotlin Coroutines untuk non-blocking IO, WorkManager untuk tugas berat di latar belakang, dan optimasi *throttling* pada pembaruan UI. |
| **4. Integrasi ML Real-time** | Penggunaan library ONNX Runtime yang memungkinkan eksekusi model ML langsung di perangkat (*on-device inference*) tanpa delay latency server. |

---

## 5. Daftar Endpoint API Utama

1.  `POST /auth/firebase-login`: Tukar token Firebase dengan JWT.
2.  `POST /scans`: Upload file dan hasil deteksi (termasuk field `fileHash`).
3.  `GET /scans/history/{userId}`: Mengambil data riwayat pemindaian cloud.
4.  `GET /subscriptions/plan`: List paket harga (Free/Premium).
5.  `POST /transactions/checkout`: Inisiasi pembayaran fitur premium.

---

## 6. Struktur Kode Penting
*   `com.riset.mangodefendd.ml`: Inti logika Machine Learning (Classifier & Preprocessor).
*   `com.riset.mangodefendd.data.network`: Definisi API dan Interceptor keamanan.
*   `com.riset.mangodefendd.service`: Layanan latar belakang (Worker & Monitor).
*   `com.riset.mangodefendd.ui.viewmodel`: Pengelola logika bisnis untuk tampilan.

---
**Catatan:** Pastikan SHA-1 didebug lokal sudah terdaftar di Firebase Console agar fitur Autentikasi (Poin A) dapat berfungsi sempurna saat dijalankan.
