# Panduan Agent

## Stack

- Java 17 atau lebih baru, dikompilasi langsung dengan JDK (`javac --release 17`); aplikasi tidak memakai framework runtime. Playwright dipakai sebagai dependensi pengujian UI.
- Backend menggunakan `com.sun.net.httpserver.HttpServer` dari JDK untuk REST API dan menyajikan UI.
- UI adalah satu halaman HTML/CSS/JavaScript di `web/index.html`, ditambahkan ke classpath oleh `run.bat`.
- Penyimpanan memakai `DataStore`: `InMemoryStore` sebagai bawaan, atau `SupabaseStore` melalui REST/PostgREST jika dikonfigurasi.
- Konfigurasi dibaca dari environment, dengan `.env` opsional sebagai nilai bawaan. Skema Supabase ada di `supabase/schema.sql`.
- Build, run, dan tes dijalankan melalui skrip Windows `build.bat`, `run.bat`, dan `test.bat`.

## Struktur

```text
src/
  main/checkout/
    Main.java, Config.java       # konfigurasi dan bootstrap aplikasi
    facade/                      # perakitan komponen dan API checkout tingkat aplikasi
    mediator/                    # koordinasi komponen untuk menghitung quote checkout
    model/                       # pesanan, produk, quote, status, dan log
    observer/                    # event status pesanan dan pengamat efek samping
    payment/                     # gateway, metode pembayaran, dan adapter
    singleton/                   # TransactionLog
    store/                       # DataStore, implementasi memori/Supabase, dan data awal
    util/                         # JSON dan utilitas nominal uang
    web/                          # REST API dan penyajian UI
  test/checkout/SelfTest.java    # tes mandiri
supabase/schema.sql              # skema tabel Supabase
web/index.html                   # UI aplikasi
```

## Arsitektur aplikasi

`Main` membaca konfigurasi, memilih penyimpanan, lalu merakit `AppContext` dan memulai `HttpApi`. API meneruskan operasi checkout ke `CheckoutFacade`, yang memakai `CheckoutSession`/`CheckoutMediator` untuk perhitungan dan `PaymentGateways` untuk memilih metode pembayaran. Perubahan status pesanan diterbitkan melalui `OrderStatusPublisher` kepada pengamat stok, poin, notifikasi, dan audit.

Pola utama yang dicontohkan: Facade (`CheckoutFacade`), Mediator (`CheckoutMediator`), Observer (`OrderStatusPublisher`), Factory Method (`PaymentGateway` dan gateway konkret), Adapter (`BankTransferAdapter`), Template Method (`AbstractPayment`), dan Singleton (`TransactionLog`).

## Panduan kerja

- Baca `README.md` untuk aturan bisnis, endpoint API, pola desain, dan detail menjalankan aplikasi.
- Letakkan kode Java di package `checkout` yang sesuai dengan tanggung jawabnya, dan pertahankan pendekatan JDK untuk aplikasi runtime.
- Jalankan `test.bat` untuk kompilasi dan tes Java mandiri. Jalankan `npm run test:e2e` untuk tes UI Chromium lokal dengan penyimpanan memori.
- Jika mengubah skema penyimpanan Supabase, sesuaikan `supabase/schema.sql` dan implementasi `SupabaseStore` yang terkait.
- Simpan kredensial lokal di `.env`; jangan masukkan file tersebut atau kunci Supabase ke kode maupun frontend.
- Endpoint penambahan stok tidak memiliki autentikasi; gunakan hanya pada demo lokal dan jangan publikasikan ke jaringan.
