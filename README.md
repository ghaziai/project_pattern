## Menjalankan
Prasyarat: JDK 17 atau lebih baru (`java -version`).

```bat
run.bat         # Windows
# buka http://localhost:8080
test.bat        # menjalankan 86 pengujian mandiri
```

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
`POST /api/quote | orders | orders/{id}/pay | orders/{id}/cancel`
Body: `{"items":[{"code":"SKU-001","qty":2}],"method":"QRIS","voucher":"HEMAT10","points":1000}`
Kode galat: 400 input salah, 404, 405, 409 status tidak sesuai, 413, 422 pesanan tidak valid, 500, 502 penyimpanan bermasalah.

## Batasan
- Tidak ada autentikasi; satu pelanggan demo. Bukan untuk produksi.
- Konsistensi stok dijamin pada satu instance server (lock di Facade); beberapa instance memerlukan transaksi basis data.
- Integrasi Supabase diuji terhadap tiruan PostgREST; uji pada proyek Anda sendiri sebelum dipakai.
