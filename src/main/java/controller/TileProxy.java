package controller;

import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.*;
import java.util.concurrent.Executors;

/**
 * Proxy de tuiles OSM partagé — singleton.
 * Démarre une seule fois, réutilisé par toutes les cartes de l'application.
 * Sert : /tiles/{z}/{x}/{y}.png  → OSM
 *        /topo/{z}/{x}/{y}.png   → OpenTopoMap
 *        /leaflet/leaflet.js     → local
 *        /leaflet/leaflet.css    → local
 */
public class TileProxy {

    private static TileProxy instance;
    private HttpServer server;
    private int port = 0;

    private static final File CACHE_DIR =
        new File(System.getProperty("user.home"), ".forestguard_tiles");

    private static byte[] leafletJs  = null;
    private static byte[] leafletCss = null;

    private TileProxy() {
        CACHE_DIR.mkdirs();
        chargerLeafletLocal();
        demarrer();
    }

    public static synchronized TileProxy getInstance() {
        if (instance == null) instance = new TileProxy();
        return instance;
    }

    public int getPort() { return port; }

    /** Remplace '__PORT__' dans un HTML par le port réel. */
    public String injecterPort(String html) {
        return html.replace("'__PORT__'", "'" + port + "'");
    }

    private void chargerLeafletLocal() {
        try (InputStream is = getClass().getResourceAsStream("/Leaflet/leaflet.js")) {
            if (is != null) leafletJs = is.readAllBytes();
        } catch (Exception ignored) {}
        try (InputStream is = getClass().getResourceAsStream("/Leaflet/leaflet.css")) {
            if (is != null) leafletCss = is.readAllBytes();
        } catch (Exception ignored) {}
    }

    private void demarrer() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 32);
            port   = server.getAddress().getPort();

            // ── /tiles → OSM ──────────────────────────────────────
            server.createContext("/tiles", ex -> servirTuile(ex, "osm"));

            // ── /topo → OpenTopoMap ───────────────────────────────
            server.createContext("/topo",  ex -> servirTuile(ex, "topo"));

            // ── /leaflet → fichiers locaux ────────────────────────
            server.createContext("/leaflet", ex -> {
                String path = ex.getRequestURI().getPath();
                byte[] data = null;
                String ct   = "text/plain";
                if (path.endsWith("leaflet.js"))  { data = leafletJs;  ct = "application/javascript"; }
                if (path.endsWith("leaflet.css")) { data = leafletCss; ct = "text/css"; }
                if (data != null) {
                    ex.getResponseHeaders().set("Content-Type", ct);
                    ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                    ex.getResponseHeaders().set("Cache-Control", "max-age=86400");
                    ex.sendResponseHeaders(200, data.length);
                    ex.getResponseBody().write(data);
                    ex.getResponseBody().close();
                } else {
                    ex.sendResponseHeaders(404, -1);
                }
            });

            server.setExecutor(Executors.newFixedThreadPool(16));
            server.start();
            System.out.println("[TileProxy] Démarré sur port " + port);

        } catch (Exception e) {
            System.err.println("[TileProxy] Erreur: " + e.getMessage());
        }
    }

    private void servirTuile(com.sun.net.httpserver.HttpExchange ex, String type) throws IOException {
        String path  = ex.getRequestURI().getPath();
        String[] p   = path.split("/");
        if (p.length < 5) { ex.sendResponseHeaders(400, -1); return; }

        String z = p[2], x = p[3], y = p[4].replace(".png", "");
        String prefix = type.equals("topo") ? "topo_" : "";
        File cache = new File(CACHE_DIR, prefix + z + "_" + x + "_" + y + ".png");

        byte[] data = null;

        // 1. Cache disque
        if (cache.exists() && cache.length() > 0) {
            try (FileInputStream fis = new FileInputStream(cache)) {
                data = fis.readAllBytes();
            } catch (Exception ignored) {}
        }

        // 2. Télécharger si absent
        if (data == null) {
            String url;
            if (type.equals("topo")) {
                url = "https://tile.opentopomap.org/" + z + "/" + x + "/" + y + ".png";
            } else {
                String sub = new String[]{"a","b","c"}[Math.abs((z+x+y).hashCode()) % 3];
                url = "https://" + sub + ".tile.openstreetmap.org/" + z + "/" + x + "/" + y + ".png";
            }
            try {
                data = telecharger(url);
                final byte[] save = data;
                new Thread(() -> {
                    try (FileOutputStream fos = new FileOutputStream(cache)) {
                        fos.write(save);
                    } catch (Exception ignored) {}
                }).start();
            } catch (Exception e) {
                ex.sendResponseHeaders(502, -1);
                return;
            }
        }

        ex.getResponseHeaders().set("Content-Type", "image/png");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().set("Cache-Control", "max-age=86400");
        ex.sendResponseHeaders(200, data.length);
        ex.getResponseBody().write(data);
        ex.getResponseBody().close();
    }

    private byte[] telecharger(String urlStr) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 ForestGuard/1.0");
        conn.setConnectTimeout(6000);
        conn.setReadTimeout(10000);
        if (conn.getResponseCode() != 200) throw new IOException("HTTP " + conn.getResponseCode());
        try (InputStream is = conn.getInputStream()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192]; int n;
            while ((n = is.read(buf)) != -1) baos.write(buf, 0, n);
            return baos.toByteArray();
        }
    }
}

