package checkout;

import checkout.facade.AppContext;
import checkout.store.DataStore;
import checkout.store.InMemoryStore;
import checkout.store.StoreException;
import checkout.store.SupabaseStore;
import checkout.web.HttpApi;

import java.nio.file.Path;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws Exception {
        Config config = new Config(Path.of(".env"));
        String host = config.get("HOST", "127.0.0.1");
        int port;
        try {
            port = Integer.parseInt(config.get("PORT", "8080"));
            if (port < 0 || port > 65535) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            System.err.println("PORT tidak valid. Gunakan angka 0-65535.");
            System.exit(1);
            return;
        }

        String url = config.get("SUPABASE_URL", "");
        String key = config.get("SUPABASE_KEY", "");
        boolean useSupabase = !url.isEmpty() && !key.isEmpty()
                && !"memory".equalsIgnoreCase(config.get("STORE", ""));

        DataStore store;
        if (useSupabase) {
            try {
                SupabaseStore s = new SupabaseStore(url, key);
                s.init();
                store = s;
            } catch (StoreException | IllegalArgumentException e) {
                System.err.println("Gagal terhubung ke Supabase: " + e.getMessage());
                System.err.println("Periksa SUPABASE_URL, SUPABASE_KEY, dan jalankan supabase/schema.sql terlebih dahulu.");
                System.exit(1);
                return;
            }
        } else {
            store = new InMemoryStore();
        }

        HttpApi api = new HttpApi(new AppContext(store));
        api.start(host, port);
        Runtime.getRuntime().addShutdownHook(new Thread(api::stop));

        System.out.println("==============================================================");
        System.out.println(" Aplikasi Pemrosesan Pembayaran dan Pesanan (SIMULASI)");
        System.out.println(" Penyimpanan : " + store.name());
        System.out.println(" Alamat      : http://" + ("0.0.0.0".equals(host) ? "localhost" : host) + ":" + api.port());
        System.out.println(" Tekan Ctrl+C untuk berhenti.");
        System.out.println("==============================================================");
    }
}
