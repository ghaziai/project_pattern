## Menjalankan dengan Supabase Owner

Gunakan project dan kredensial Supabase yang sudah disediakan owner. Jangan membuat project baru atau mengganti `.env` yang sudah diberikan.

1. Pastikan file `.env` dari owner berada di folder utama repositori, sejajar dengan `run.bat`. Jika belum ada, minta owner mengirim konfigurasi melalui jalur aman. Jangan membuat project baru.
2. Pastikan konfigurasi memuat `SUPABASE_URL` dan `SUPABASE_KEY`. Jangan mengganti nilai yang diberikan owner. Jika `STORE=memory`, ubah hanya nilai tersebut menjadi `STORE=supabase`.
3. Minta konfirmasi owner bahwa skema aplikasi sudah dipasang. Skema harus mencakup tabel `products`, `orders`, `loyalty`, `transaction_log`, dan fungsi RPC `add_product_stock`.
4. Jika skema belum dipasang, minta izin owner sebelum mengubah project. Setelah disetujui, buka **SQL Editor** di Supabase Dashboard, pilih **New query**, salin isi [`supabase/schema.sql`](supabase/schema.sql), lalu jalankan query tersebut.
5. Pastikan JDK 17 atau lebih baru terpasang dan tersedia di terminal:

   ```bat
   java -version
   javac -version
   ```

6. Buka Command Prompt atau PowerShell pada folder utama repositori. Kompilasi program:

   ```bat
   .\build.bat
   ```

   Tunggu sampai terminal menampilkan `Build selesai -> out\`.
7. Jalankan program:

   ```bat
   .\run.bat
   ```

8. Pastikan terminal menampilkan `Penyimpanan : supabase`, lalu buka http://localhost:8080 di browser.
9. Untuk menghentikan program, kembali ke terminal lalu tekan `Ctrl+C`.

Saat startup, aplikasi menambahkan produk demo jika tabel `products` kosong dan poin demo jika entri pelanggan demo di `loyalty` belum ada. Karena itu, gunakan project owner hanya dengan izinnya, terutama jika project berisi data bersama atau produksi.

Jangan menyalin `SUPABASE_KEY` ke chat, commit ke Git, atau memasukkannya ke `web/index.html`. File `.env` sudah diabaikan Git. Key `service_role` dapat melewati kebijakan RLS dan hanya boleh dipakai di server.

Jika program gagal terhubung, periksa nama variabel di `.env`, pastikan `STORE` bukan `memory`, dan minta owner memastikan skema sudah dijalankan.

### Menjalankan tanpa Supabase

Jika Supabase belum dikonfigurasi, program memakai penyimpanan memori. Data demo kembali ke kondisi awal saat program dijalankan ulang. Untuk memaksa mode memori meskipun `.env` berisi kredensial Supabase, gunakan `STORE=memory`. Jalankan `.\build.bat`, lalu `.\run.bat` dan buka http://localhost:8080.

## Pemetaan Pattern
| Kategori | Pattern | Lokasi | Peran |
|---|---|---|---|
| Creational | Factory Method | `payment/PaymentGateway` + `QrisGateway`, `VirtualAccountGateway`, `EWalletGateway`, `BankTransferGateway` | Membuat objek metode pembayaran |
| Creational | Singleton | `singleton/TransactionLog` | Satu log transaksi bersama |
| Structural | Adapter | `payment/BankTransferAdapter` | Membungkus `LegacyBankGateway` |
| Structural | Facade | `facade/CheckoutFacade` | Satu pintu alur checkout |
| Behavioral | Template Method | `payment/AbstractPayment` | Kerangka validasi, otorisasi, struk |
| Behavioral | Mediator | `mediator/CheckoutMediator` | Koordinasi keranjang, voucher, poin, metode, stok, ringkasan |
| Behavioral | Observer | `observer/OrderStatusPublisher` + 4 observer | Reaksi atas perubahan status pesanan |

## Aturan Bisnis
- Status: `PENDING_PAYMENT` menjadi `PAID`, `FAILED`, atau `CANCELLED`. Stok dan poin direservasi saat pesanan dibuat dan dikembalikan bila gagal atau batal.
- Biaya: QRIS 0,7%; Virtual Account Rp4.000; E-Wallet 1,5% (gagal bila > Rp1.000.000, simulasi); Transfer Bank Rp2.500 (batas Rp10.000.000).
- Voucher: `HEMAT10` (10%, maks Rp25.000, min belanja Rp50.000); `POTONG5K` (Rp5.000, min Rp30.000).
- Poin: 1 poin = Rp1, maksimal 50% dari nilai setelah voucher; dapat 1 poin per Rp10.000 yang dibayar. Pelanggan demo `demo` memulai dengan 20.000 poin.

## REST API
`GET /api/health | products | methods | me | orders | orders/{id} | notifications | log`
`POST /api/quote | orders | orders/{id}/pay | orders/{id}/cancel | products/{code}/stock`
Body: `{"items":[{"code":"SKU-001","qty":2}],"method":"QRIS","voucher":"HEMAT10","points":1000}`
Restock memakai `{"quantity":5}` dan menambahkan jumlah tersebut ke stok saat ini. Jumlah harus bilangan bulat positif; produk yang tidak ada menghasilkan 404 dan stok yang melampaui kapasitas menghasilkan 409.

Endpoint restock tidak memiliki autentikasi. Gunakan hanya untuk demo lokal dengan host loopback; jangan publikasikan server ke jaringan.
Untuk Supabase, jalankan `supabase/schema.sql` agar fungsi RPC restock tersedia.

Kode galat lainnya: 400 input salah, 404, 405, 409 status tidak sesuai, 413, 422 pesanan tidak valid, 500, 502 penyimpanan bermasalah.
