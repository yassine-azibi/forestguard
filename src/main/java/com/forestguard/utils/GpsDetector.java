package com.forestguard.utils;

import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Détecteur de position via géolocalisation IP (ipapi.co).
 *
 * <h2>Approche</h2>
 * <p>Appelle {@code https://ipapi.co/json/} via {@link HttpClient} (Java 11+)
 * et extrait {@code latitude} / {@code longitude} depuis la réponse JSON.</p>
 *
 * <h2>Pourquoi pas WebView + navigator.geolocation ?</h2>
 * <p>JavaFX WebView bloque l'API Geolocation par défaut :
 * <em>"Origin does not have permission to use Geolocation service"</em>.
 * Il n'existe pas de moyen fiable de contourner cette restriction sans
 * modifier le code natif de WebKit embarqué.</p>
 *
 * <h2>Cache</h2>
 * <p>Le résultat est mis en cache après le premier appel réussi.
 * Les appels suivants retournent immédiatement la valeur cachée
 * sans faire de nouvelle requête HTTP.</p>
 *
 * <h2>Fallback</h2>
 * <p>Si l'appel HTTP échoue (pas de réseau, quota dépassé, etc.),
 * retourne les coordonnées par défaut de Tunis, Tunisie.</p>
 *
 * <h2>Thread safety</h2>
 * <p>Peut être appelé depuis n'importe quel thread (FX ou non).
 * Le cache est géré via {@link AtomicReference}.</p>
 */
public final class GpsDetector {

    // ── Configuration ─────────────────────────────────────────────────────────

    private static final String IPAPI_URL = "https://ipapi.co/json/";
    private static final Duration TIMEOUT = Duration.ofSeconds(8);

    // ── Fallback : Tunis, Tunisie ─────────────────────────────────────────────
    private static final String FALLBACK_LAT = "36.806500";
    private static final String FALLBACK_LON = "10.181500";

    // ── Cache thread-safe ─────────────────────────────────────────────────────
    // null = pas encore appelé ; valeur non-null = résultat mis en cache.
    private static final AtomicReference<Result> CACHE = new AtomicReference<>(null);

    // ── Client HTTP partagé ───────────────────────────────────────────────────
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();

    private GpsDetector() {}

    // ── Types publics ─────────────────────────────────────────────────────────

    /**
     * Coordonnées GPS retournées par {@link #detect()}.
     */
    public static final class Result {
        /** Latitude décimale (ex : {@code "36.806500"}). */
        public final String lat;
        /** Longitude décimale (ex : {@code "10.181500"}). */
        public final String lon;

        public Result(String lat, String lon) {
            this.lat = lat;
            this.lon = lon;
        }

        @Override
        public String toString() {
            return lat + ", " + lon;
        }
    }

    // ── API publique ──────────────────────────────────────────────────────────

    /**
     * Retourne les coordonnées GPS de l'utilisateur via géolocalisation IP.
     *
     * <p>Le résultat est mis en cache après le premier appel réussi.
     * Ne retourne jamais {@code null} — utilise le fallback Tunis si nécessaire.</p>
     *
     * @return {@link Result} avec lat/lon, jamais {@code null}
     */
    public static Result detect() {
        // ── Retourner le cache si disponible ──────────────────────────────────
        Result cached = CACHE.get();
        if (cached != null) {
            return cached;
        }

        // ── Appel HTTP à ipapi.co ─────────────────────────────────────────────
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(IPAPI_URL))
                    .timeout(TIMEOUT)
                    .header("User-Agent", "ForestGuard/1.0 (contact@forestguard.tn)")
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP.send(
                    request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("[GpsDetector] ❌ HTTP " + response.statusCode()
                        + " — utilisation du fallback Tunis.");
                return fallback();
            }

            // ── Parser le JSON ────────────────────────────────────────────────
            JSONObject json = new JSONObject(response.body());

            // ipapi.co retourne un champ "error" si le quota est dépassé
            if (json.optBoolean("error", false)) {
                String reason = json.optString("reason", "quota dépassé");
                System.err.println("[GpsDetector] ❌ ipapi.co : " + reason
                        + " — utilisation du fallback Tunis.");
                return fallback();
            }

            double latD = json.optDouble("latitude",  Double.NaN);
            double lonD = json.optDouble("longitude", Double.NaN);

            if (Double.isNaN(latD) || Double.isNaN(lonD)) {
                System.err.println("[GpsDetector] ❌ Coordonnées manquantes dans la réponse"
                        + " — utilisation du fallback Tunis.");
                return fallback();
            }

            String lat = String.format("%.6f", latD);
            String lon = String.format("%.6f", lonD);
            String city = json.optString("city", "?");

            Result result = new Result(lat, lon);
            CACHE.set(result); // mettre en cache
            System.out.println("[GpsDetector] ✅ Position IP : " + city
                    + " (" + lat + ", " + lon + ")");
            return result;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("[GpsDetector] ❌ Interrompu — utilisation du fallback Tunis.");
            return fallback();
        } catch (Exception e) {
            System.err.println("[GpsDetector] ❌ " + e.getMessage()
                    + " — utilisation du fallback Tunis.");
            return fallback();
        }
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    /** Retourne le fallback Tunis et le met en cache pour éviter de réessayer. */
    private static Result fallback() {
        Result fb = new Result(FALLBACK_LAT, FALLBACK_LON);
        CACHE.compareAndSet(null, fb); // ne pas écraser un résultat réel déjà mis en cache
        return fb;
    }
}
