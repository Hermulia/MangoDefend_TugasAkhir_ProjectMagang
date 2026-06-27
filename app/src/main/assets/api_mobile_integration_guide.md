# Dokumentasi Integrasi API MangoDefend untuk Mobile (iOS/Android)

Dokumentasi ini ditujukan bagi *Mobile Developer* untuk memahami alur kerja dan implementasi API **MangoDefend** berdasarkan struktur *database* (ERD) terbaru. Dokumentasi ini berfokus pada alur Autentikasi & Perangkat, Langganan (Subscriptions), Transaksi (Transactions), dan Pemindaian (Scans).

URL Base API: `https://date-navigate-roommate-obtaining.trycloudflare.com/api` (atau URL API Production)

---

## 1. Autentikasi & Manajemen Perangkat (Auth & Devices)

### Konsep Database
- **`user`**: Menyimpan identitas pengguna (bisa menggunakan email/password lokal atau melalui Firebase UID).
- **`device`**: Memungkinkan pengguna untuk melacak *multiple devices*. Setiap perangkat mencatat `hardware_id`, `hostname`, `os_type`, `app_type`, dan kapan terakhir kali login (`last_login`).

### Alur Kerja (User Flow)
1. **Login / Register:**
   - Mobile app memanggil `/auth/login`, `/auth/firebase-login`, atau `/register`.
   - API mengembalikan Bearer Token (JWT).
2. **Device Tracking (Opsional namun disarankan):**
   - Backend kemungkinan akan mendaftarkan/memperbarui data perangkat pengguna (menyimpan ID perangkat mobile). Mobile Developer harus siap mengirimkan metadata perangkat (seperti jenis OS atau hardware ID) jika diperlukan di payload Login.
   - App dapat mengambil daftar perangkat lewat `GET /{id}/devices`.

---

## 2. Manajemen Paket (Plans) & Langganan (Subscriptions)

### Konsep Database
1. **`plans`**: Merupakan katalog paket (misal: Free, Premium). Menyimpan parameter penting:
   - `durationDays`: Durasi paket.
   - `price`: Harga.
   - `upload_file_limit` & `full_scan_limit`: Batas maksimal penggunaan.
   - **`model_id`**: Berelasi langsung dengan tabel `ml_models`. Artinya, paket tertentu dapat menikmati model Machine Learning yang lebih canggih/berbeda.
2. **`subscriptions`**: Menyimpan status kepemilikan paket oleh `user`. Record ini memiliki `start_date`, `end_date`, dan status aktif (`is_active`).

### Alur Kerja
1. **Melihat Katalog (Pricing Screen):**
   - App memanggil `GET /subscriptions/plan` untuk menampilkan semua paket yang tersedia beserta detail limit (`upload_file_limit`).
2. **Cek Langganan Saat Ini (Dashboard):**
   - App memanggil `GET /subscriptions/active/{userId}`.
   - Backend akan memeriksa baris pada `subscriptions` di mana `is_active = true` dan `end_date` masih berlaku. Tampilkan sisa kuota fitur kepada user.

---

## 3. Transaksi Pembayaran (Transactions)

### Konsep Database
Tabel **`transactions`** menjembatani niat pembelian. Menyimpan `user_id`, `plan_id`, `method` (metode bayar seperti VA/QRIS), `amount`, `status`, dan referensi eksternal dari Payment Gateway (`external_id`).

### Alur Kerja
1. **Checkout:**
   - User memilih paket dan menekan "Beli".
   - App mengirim `POST /transactions/checkout` dengan *payload*:
     ```json
     {
       "plan_id": 1,
       "method": "virtual_account" // atau "qris"
     }
     ```
   - API mengembalikan detail seperti Nomor VA / Link QRIS untuk ditampilkan di UI, sambil menyimpan data transaksi berstatus "pending".
2. **Webhook (Di Sisi Server):**
   - Saat pembayaran lunas, Payment Gateway memanggil `POST /transactions/webhook`.
   - Backend mengupdate `transactions` menjadi "success".
   - **Sangat Penting:** Saat status sukses, backend akan otomatis membuat/mengupdate record di tabel `subscriptions` (mengatur `start_date` dan `end_date` berdasarkan `plans.durationDays`), mengaktifkan layanan untuk *user* tersebut.
3. **Penyelesaian di Mobile:**
   - Anda dapat menaruh tombol "Cek Pembayaran" yang akan me-*refresh* UI dan mengecek kembali `GET /subscriptions/active/{userId}` untuk memvalidasi bahwa paket sudah aktif.

---

## 4. Pemindaian Malware (Scans)

### Konsep Database
Fitur pemindaian dipecah menjadi *One-to-Many* untuk mendukung proses pemindaian beberapa file sekaligus (*batch*).
1. **`summary_scans`**: Bertindak sebagai *header*. Merangkum 1 sesi *upload*. Menyimpan metrik total (`total_files_scanned`, `total_malware_detected`, dan `status`).
2. **`scan_details`**: Merinci hasil pemindaian per-file, direlasikan ke `summary_scans`. Memuat `file_name`, `file_hash`, `is_malware`, dan `matched_in_library`.

### Alur Kerja
1. **Proses Upload & Scan:**
   - App memanggil `POST /scans` (`multipart/form-data`) dengan membawa parameter array file.
   - Di sisi server, backend akan mengecek kuota langganan (`subscriptions`) dan mengambil `model_id` (dari tabel `plans` yang aktif) untuk melakukan *inference* ML.
   - Server lalu membuat 1 entri di `summary_scans` dan memecahnya ke beberapa entri `scan_details`.
2. **Layar Hasil / Laporan:**
   - Server mengembalikan ID `summary_scans`. Mobile app dapat memanggil `GET /scans/detail/{id}` untuk menjabarkan list file mana yang lolos dan mana yang terindikasi malware (`is_malware = true`).
3. **Layar Riwayat (History):**
   - Panggil `GET /scans/history/{userId}` untuk menampilkan daftar seluruh `summary_scans` yang pernah dilakukan user beserta angka "Malware Detected vs Files Scanned".

---

> [!WARNING]
> Selalu tangani skenario **Limit Kuota Habis**. Karena proses Scan sangat terikat dengan tabel `plans` (seperti `upload_file_limit`), pastikan aplikasi Anda menangani gracefully *HTTP Error 400/403* dari `POST /scans` yang me-*return* pesan limit telah tercapai, dengan menampilkan popup *Call-to-Action* untuk *Upgrade Plan*.
