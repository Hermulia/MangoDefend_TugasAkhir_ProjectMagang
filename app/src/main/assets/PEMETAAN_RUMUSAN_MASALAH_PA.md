# Panduan Penyelesaian Proyek Akhir (PA): MangoDefend

Dokumen ini memetakan secara spesifik antara **Rumusan Masalah** penelitian Anda dengan **Implementasi Teknis** yang ada di dalam kode program. Gunakan ini sebagai referensi utama saat menulis Bab IV (Hasil dan Pembahasan).

---

## Rumusan Masalah 1: Antarmuka (UI) Informatif & Efisien
> *Bagaimana mengimplementasikan antarmuka aplikasi antivirus Android yang dapat menampilkan proses dan hasil deteksi malware secara informatif, mudah dipahami, serta efisien bagi pengguna?*

### Solusi Teknis dalam Proyek:
*   **Framework:** Menggunakan **Jetpack Compose** dengan standar **Material 3** untuk menjamin UI yang modern dan responsif.
*   **Visualisasi Hirarki Keamanan:** 
    *   Implementasi sistem warna dinamis pada `DashboardScreen.kt`: Hijau (Aman), Kuning (Mencurigakan), dan Merah (Bahaya).
    *   Komponen `ScanResultCard` memberikan detail instan mengenai skor probabilitas malware.
*   **Efisiensi Update:** Menggunakan `StateFlow` dan `collectAsStateWithLifecycle()` yang memastikan UI hanya merender ulang komponen yang berubah saja (hemat penggunaan GPU).
*   **File Terkait:** 
    *   `ui/screens/DashboardScreen.kt`
    *   `ui/screens/ScanScreen.kt`
    *   `ui/theme/Theme.kt`

---

## Rumusan Masalah 2: Integrasi Frontend-Backend & Hash
> *Bagaimana menghubungkan sisi frontend Android dengan sistem backend untuk mengirimkan nilai hash file yang akan dianalisis dan menerima hasil deteksi dari server?*

### Solusi Teknis dalam Proyek:
*   **Fingerprinting Digital:** Implementasi fungsi `FileUtils.calculateFileHash` menggunakan algoritma **SHA-256**. Nilai hash unik ini dikirim ke server sebagai identitas file (fingerprint).
*   **Network Layer:** Menggunakan library **Retrofit 2** dan **OkHttp 4** untuk menangani komunikasi asinkron dengan server.
*   **Keamanan Integrasi:** Implementasi `AuthInterceptor.kt` yang secara otomatis menyematkan token JWT pada setiap request, menjamin data scan terhubung dengan akun user yang benar.
*   **File Terkait:**
    *   `util/FileUtils.kt` (Logika penghitungan Hash)
    *   `data/network/ApiService.kt` (Definisi endpoint `/scans`)
    *   `ml/MalwareRepository.kt` (Proses pengiriman file + hash ke API)

---

## Rumusan Masalah 3: Optimasi Performa & Sumber Daya
> *Bagaimana memastikan performa aplikasi Android tetap optimal saat melakukan proses deteksi malware, baik dari sisi waktu eksekusi maupun penggunaan sumber daya perangkat?*

### Solusi Teknis dalam Proyek:
*   **Multithreading (Coroutines):** Seluruh proses berat (I/O file dan ML Inference) dipisahkan dari thread utama menggunakan `Dispatchers.IO` untuk mencegah aplikasi macet (ANR).
*   **Background Processing (WorkManager):** Implementasi `ScanWorker.kt` memungkinkan pemindaian ribuan file tetap berjalan di latar belakang tanpa membebani sistem secara berlebih.
*   **Throttling Progress:** Pada `MalwareRepository.scanFiles`, terdapat logika pembatasan update progres (maksimal setiap 500ms). Hal ini secara signifikan mengurangi beban CPU saat memproses antrian file yang banyak.
*   **File Terkait:**
    *   `service/ScanWorker.kt`
    *   `ml/MalwareRepository.kt` (Logika `scanFiles` asinkron)

---

## Rumusan Masalah 4: Integrasi ML Real-Time
> *Bagaimana mengintegrasikan hasil analisis dari model machine learning agar dapat ditampilkan secara real-time pada aplikasi Android?*

### Solusi Teknis dalam Proyek:
*   **On-Device Inference:** Menggunakan library **ONNX Runtime Android**. Model ML dijalankan langsung di CPU/NNAPI perangkat, sehingga hasil deteksi muncul dalam hitungan milidetik tanpa menunggu respon internet.
*   **Pipeline Transformasi:** `BinaryImagePreprocessor.kt` mengubah file APK/Binary menjadi tensor secara instan untuk diinput ke model.
*   **Reactive Update:** Hasil deteksi dari model langsung dikirim melalui `callback` ke UI secara streaming, memberikan pengalaman "Real-time" kepada pengguna saat pemindaian berlangsung.
*   **File Terkait:**
    *   `ml/OnnxMalwareClassifier.kt` (Inti Engine ML)
    *   `ml/BinaryImagePreprocessor.kt` (Preprocessing data)

---

## Ringkasan Teknologi Utama
1.  **Bahasa:** Kotlin
2.  **Arsitektur:** MVVM (Model-View-ViewModel)
3.  **Dependency Injection:** Hilt
4.  **Database:** Room (Lokal) & Cloud API (Remote)
5.  **ML Engine:** ONNX Runtime
6.  **Network:** Retrofit & OkHttp
