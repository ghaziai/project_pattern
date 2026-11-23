# Status Repositori dan Hasil End-to-End

Tanggal pemeriksaan: 2026-11-23

## Ringkasan

- Aplikasi Java berhasil dikompilasi dan seluruh 86 tes mandiri lulus.
- Alur checkout diuji di Chromium melalui server lokal dengan penyimpanan `memory`; tidak ada akses atau perubahan ke Supabase.
- UI berhasil membuat pesanan, membayar pesanan simulasi, membatalkan pesanan lain, dan menampilkan notifikasi serta log. Tidak ada uncaught page error.
- Server pengujian sudah dihentikan. Port 18080 bebas.

## Lingkungan

- Windows
- OpenJDK 25.0.3; skrip build mengompilasi dengan target Java 17.
- Node.js 24.21.0 dan Playwright 1.65.1 tersedia.
- Browser Chromium Playwright tersedia.
- File `.env` lokal ada, tetapi nilainya tidak dibaca atau disalin ke laporan. Pengujian memaksa `STORE=memory` agar kredensial Supabase tidak digunakan.

## Hasil pengujian

### Tes Java

Perintah: `.\test.bat`

Hasil: **86 lulus, 0 gagal**.

Suite mencakup serialisasi JSON, metode pembayaran, quote dan voucher, alur checkout, reservasi stok/poin, konkurensi, isolasi observer, mock Supabase/PostgREST, dan REST API.

### End-to-end UI

Server dijalankan pada `127.0.0.1:18080` dengan `STORE=memory`, lalu diuji melalui Chromium headless.

| Alur | Hasil |
|---|---|
| UI memuat dan menampilkan penyimpanan memori serta 5 produk | Lulus |
| Memilih produk menghasilkan quote valid | Lulus |
| Checkout membuat pesanan `PENDING_PAYMENT` | Lulus |
| Aksi bayar mengubah status menjadi `PAID` | Lulus |
| Checkout kedua lalu membatalkannya menjadi `CANCELLED` | Lulus |
| Notifikasi dan log transaksi tampil; tidak ada page error | Lulus |

Supabase langsung tidak diuji. Tes penyimpanan yang berjalan menggunakan mock lokal.

## Kondisi Git saat pemeriksaan

Branch `dev`, HEAD `11ee0b6`. Snapshot berikut diambil sebelum laporan ini dibuat:

```text
 D .env.example
 M .gitignore
?? .github/
?? AGENTS.md
?? package-lock.json
?? package.json
?? playwright.config.ts
?? tests/
```

`.env` ada di lokal dan diabaikan Git. Build menghasilkan `out/`, yang juga diabaikan. Tes tidak mengubah perubahan Git yang tercantum di atas.

## Catatan kondisi saat ini

- `package.json` memiliki dependensi pengembangan Playwright, tetapi `scripts` masih kosong.
- `tests/example.spec.ts` adalah tes contoh yang membuka `playwright.dev`, bukan aplikasi checkout. Karena itu tes tersebut tidak dijalankan sebagai verifikasi aplikasi; pengujian browser dilakukan langsung terhadap server lokal.
- `playwright.config.ts` mencakup Chromium, Firefox, dan WebKit. Workflow `.github/workflows/playwright.yml` menjalankan `npx playwright test`.
- `.env.example` sedang terhapus dari worktree, sementara `AGENTS.md` merujuknya. Selain itu, `AGENTS.md` menyebut tidak ada dependensi pihak ketiga, sedangkan `package.json` saat ini memiliki dependensi pengembangan Playwright. Dokumentasi tersebut perlu diselaraskan jika perubahan worktree ini memang dipertahankan.
