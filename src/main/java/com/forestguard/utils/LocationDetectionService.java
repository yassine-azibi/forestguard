package com.forestguard.utils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service de détection de localisation pour l'application JavaFX ForestGuard.
 *
 * <h2>Stratégie en trois niveaux</h2>
 * <ol>
 *   <li><b>WebView + navigator.geolocation (cross-platform)</b> — utilise la géolocalisation du navigateur
 *       via une WebView JavaFX invisible. Supporte les permissions du système d'exploitation (Windows, macOS, Linux).
 *       Timeout : 10 secondes max, avec fallback automatique.</li>
 *   <li><b>ip-api.com</b> — retourne le gouvernorat ({@code regionName}) et les
 *       coordonnées ({@code lat}, {@code lon}) de l'IP publique. Gratuit, sans
 *       clé, 45 req/min.</li>
 *   <li><b>Nominatim (OpenStreetMap)</b> — reverse geocoding avec {@code zoom=18}
 *       (niveau rue) pour obtenir l'adresse complète : rue, quartier, ville,
 *       code postal. Fallback sur {@code zoom=10} si l'adresse est incomplète.</li>
 * </ol>
 *
 * <h2>Résultat</h2>
 * <p>Retourne un {@link DetectedLocation} contenant :</p>
 * <ul>
 *   <li>{@code governorate} — gouvernorat officiel (ex : {@code "Ariana"})</li>
 *   <li>{@code fullAddress} — adresse complète (ex : {@code "Rue de la Liberté, Ariana, Tunisie"})</li>
 *   <li>{@code lat}, {@code lon} — coordonnées brutes (fallback si adresse indisponible)</li>
 * </ul>
 *
 * <h2>Précision</h2>
 * <p>La géolocalisation GPS (WebView + navigator.geolocation) est précise aux quelques mètres.
 * Si le navigateur refuse la permission, fallback sur géolocalisation IP (~5–20 km de précision).
 * Pour un affichage fiable, utiliser les coordonnées GPS d'abord, puis IP en fallback.</p>
 */
public final class LocationDetectionService {

    // ── Configuration ─────────────────────────────────────────────────────────

    private static final Duration TIMEOUT = Duration.ofSeconds(8);

    /**
     * Mode test développeur.
     * {@code true}  → retourne instantanément une localisation fictive (Aïn Draham).
     * {@code false} → utilise la vraie géolocalisation IP + Nominatim.
     *
     * <p>Basculer à {@code false} avant de livrer en production.</p>
     */
    private static final boolean USE_FAKE_LOCATION = false;

    // ── Localisation fictive (mode test) ──────────────────────────────────────

    private static final String FAKE_GOVERNORATE = "Jendouba";
    private static final String FAKE_ADDRESS     = "Forêt de Aïn Draham, Jendouba, Tunisie";
    private static final String FAKE_LAT         = "36.7819";
    private static final String FAKE_LON         = "8.6880";

    /**
     * ip-api.com — gratuit, sans clé, 45 req/min.
     * Champs : lat, lon, city, regionName, countryCode.
     * lang=fr → noms de régions en français.
     */
    private static final String IP_API_URL =
            "http://ip-api.com/json/?fields=status,message,regionName,city,lat,lon,countryCode&lang=fr";

    /**
     * Nominatim zoom=18 → niveau adresse (rue + numéro).
     * Nominatim zoom=10 → niveau ville (fallback si adresse incomplète).
     * {@code accept-language=fr} → réponses en français pour la Tunisie.
     */
    private static final String NOMINATIM_DETAIL_URL =
            "https://nominatim.openstreetmap.org/reverse"
            + "?format=json&lat=%s&lon=%s&zoom=18&addressdetails=1&accept-language=fr";

    private static final String NOMINATIM_CITY_URL =
            "https://nominatim.openstreetmap.org/reverse"
            + "?format=json&lat=%s&lon=%s&zoom=10&addressdetails=1&accept-language=fr";

    /** User-Agent obligatoire selon la politique Nominatim (identifie l'application). */
    private static final String USER_AGENT = "ForestGuard/1.0 (contact@forestguard.tn)";

    // ── Client HTTP partagé ───────────────────────────────────────────────────

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();

    private LocationDetectionService() {}

    // ── API publique ──────────────────────────────────────────────────────────

    /**
     * Détecte la localisation complète de l'utilisateur.
     *
     * <p>Retourne un {@link DetectedLocation} avec le gouvernorat officiel,
     * l'adresse complète et les coordonnées brutes.</p>
     *
     * @return localisation détectée, jamais {@code null}
     * @throws LocationDetectionException si la détection échoue complètement
     */
    public static DetectedLocation detect() throws LocationDetectionException {
        // ── Mode test : localisation fictive ─────────────────────────────────
        if (USE_FAKE_LOCATION) {
            return new DetectedLocation(FAKE_GOVERNORATE, FAKE_ADDRESS, FAKE_LAT, FAKE_LON);
        }

        // ── Niveau 1 : GPS via GpsDetector (WebView + navigator.geolocation) ─
        // Précision GPS (~quelques mètres) >> IP (~5–20 km).
        // Si l'utilisateur refuse la permission ou si le timeout expire → fallback IP.
        GpsDetector.Result gps = GpsDetector.detect();
        if (gps != null) {
            try {
                double latD = Double.parseDouble(gps.lat);
                double lonD = Double.parseDouble(gps.lon);
                // Valider que les coordonnées sont dans la bounding box de la Tunisie
                if (latD >= 30.0 && latD <= 38.0 && lonD >= 7.0 && lonD <= 12.0) {
                    NominatimResult nom = fetchNominatimDetail(gps.lat, gps.lon);
                    String fullAddress  = buildFullAddress(nom, gps.lat, gps.lon);
                    String governorate  = resolveGovernorateFromNominatim(nom);
                    if (!governorate.isBlank()) {
                        System.out.println("[LocationDetection] GPS → " + governorate);
                        return new DetectedLocation(governorate, fullAddress, gps.lat, gps.lon);
                    }
                } else {
                    System.err.println("[LocationDetection] Coordonnées GPS hors Tunisie : "
                            + gps.lat + ", " + gps.lon);
                }
            } catch (NumberFormatException | LocationDetectionException e) {
                System.err.println("[LocationDetection] GPS → Nominatim échoué : " + e.getMessage());
            }
        }

        // ── Niveau 2 : fallback IP (ip-api.com) ──────────────────────────────
        IpApiResult ip = fetchIpApiResult();

        if (!"TN".equalsIgnoreCase(ip.countryCode)) {
            throw new LocationDetectionException(
                    "Localisation hors Tunisie détectée (" + ip.countryCode + "). "
                    + "Veuillez sélectionner votre gouvernorat manuellement.");
        }

        // Mapper le gouvernorat depuis regionName
        String governorate = "";
        if (ip.regionName != null && !ip.regionName.isBlank()) {
            governorate = GovernorateUtils.normalize(ip.regionName);
        }
        // Fallback : essayer avec la ville retournée par ip-api
        if (governorate.isBlank() && ip.city != null && !ip.city.isBlank()) {
            governorate = GovernorateUtils.normalize(ip.city);
        }

        // ── Nominatim → adresse complète à partir des coordonnées IP ─────────
        String fullAddress = "";
        String detailedGovernorate = "";

        if (ip.lat != null && ip.lon != null) {
            NominatimResult nominatim = fetchNominatimDetail(ip.lat, ip.lon);

            // Construire l'adresse complète
            fullAddress = buildFullAddress(nominatim, ip.lat, ip.lon);

            // Nominatim peut affiner le gouvernorat (plus fiable que ip-api sur les zones limites)
            detailedGovernorate = resolveGovernorateFromNominatim(nominatim);
        }

        // Priorité : gouvernorat Nominatim > gouvernorat ip-api
        String finalGovernorate = !detailedGovernorate.isBlank() ? detailedGovernorate : governorate;

        if (finalGovernorate.isBlank()) {
            throw new LocationDetectionException(
                    "Gouvernorat non reconnu : \""
                    + (ip.regionName != null ? ip.regionName : "inconnu")
                    + "\". Veuillez sélectionner manuellement.");
        }

        return new DetectedLocation(finalGovernorate, fullAddress, ip.lat, ip.lon);
    }

    /**
     * Méthode de compatibilité — retourne uniquement le gouvernorat.
     * Préférer {@link #detect()} pour obtenir l'adresse complète.
     *
     * @return nom officiel du gouvernorat
     * @throws LocationDetectionException si la détection échoue
     */
    public static String detectGovernorate() throws LocationDetectionException {
        return detect().governorate;
    }

    // ── Étape 1 : ip-api.com ─────────────────────────────────────────────────

    private static IpApiResult fetchIpApiResult() throws LocationDetectionException {
        String json = httpGet(IP_API_URL, null);

        String status = extractJsonString(json, "status");
        if (!"success".equalsIgnoreCase(status)) {
            String msg = extractJsonString(json, "message");
            throw new LocationDetectionException(
                    "ip-api.com : " + (msg != null ? msg : "réponse invalide"));
        }

        IpApiResult r = new IpApiResult();
        r.regionName  = extractJsonString(json, "regionName");
        r.city        = extractJsonString(json, "city");
        r.countryCode = extractJsonString(json, "countryCode");
        r.lat         = extractJsonNumber(json, "lat");
        r.lon         = extractJsonNumber(json, "lon");
        return r;
    }

    // ── Étape 2 : Nominatim ──────────────────────────────────────────────────

    /**
     * Appelle Nominatim en deux passes :
     * <ol>
     *   <li>zoom=18 (niveau rue) — adresse la plus précise possible.</li>
     *   <li>zoom=10 (niveau ville) — si la première passe ne retourne pas de rue.</li>
     * </ol>
     */
    private static NominatimResult fetchNominatimDetail(String lat, String lon)
            throws LocationDetectionException {

        // Passe 1 : zoom=18 (rue)
        String url18 = String.format(NOMINATIM_DETAIL_URL, lat, lon);
        String json18 = httpGet(url18, USER_AGENT);
        NominatimResult result = parseNominatimJson(json18);

        // Passe 2 : zoom=10 si pas de rue trouvée (zone rurale, forêt, etc.)
        if (result.road == null && result.suburb == null && result.city == null) {
            String url10 = String.format(NOMINATIM_CITY_URL, lat, lon);
            String json10 = httpGet(url10, USER_AGENT);
            NominatimResult result10 = parseNominatimJson(json10);
            // Fusionner : garder les champs non-nuls de la passe 2
            if (result.displayName == null) result.displayName = result10.displayName;
            if (result.city == null)        result.city        = result10.city;
            if (result.state == null)       result.state       = result10.state;
            if (result.county == null)      result.county      = result10.county;
            if (result.country == null)     result.country     = result10.country;
            if (result.postcode == null)    result.postcode    = result10.postcode;
        }

        return result;
    }

    /** Parse le JSON Nominatim et remplit un {@link NominatimResult}. */
    private static NominatimResult parseNominatimJson(String json) {
        NominatimResult r = new NominatimResult();
        r.displayName = extractJsonString(json, "display_name");

        String addressJson = extractJsonObject(json, "address");
        if (addressJson == null) return r;

        r.road         = extractJsonString(addressJson, "road");
        r.houseNumber  = extractJsonString(addressJson, "house_number");
        r.suburb       = firstNonNull(
                             extractJsonString(addressJson, "suburb"),
                             extractJsonString(addressJson, "neighbourhood"),
                             extractJsonString(addressJson, "quarter")
                         );
        r.city         = firstNonNull(
                             extractJsonString(addressJson, "city"),
                             extractJsonString(addressJson, "town"),
                             extractJsonString(addressJson, "village"),
                             extractJsonString(addressJson, "municipality")
                         );
        r.county       = extractJsonString(addressJson, "county");
        r.state        = extractJsonString(addressJson, "state");
        r.postcode     = extractJsonString(addressJson, "postcode");
        r.country      = extractJsonString(addressJson, "country");
        return r;
    }

    // ── Construction de l'adresse complète ───────────────────────────────────

    /**
     * Retourne l'adresse au niveau gouvernorat uniquement.
     *
     * <p>La géolocalisation IP est précise à ~5–20 km — elle pointe vers le
     * serveur de l'ISP, pas vers la position physique réelle. Retourner une
     * adresse de rue serait donc trompeuse. On affiche uniquement le gouvernorat
     * et le pays, ce qui correspond à la précision réelle de la source.</p>
     *
     * <p>Fallback : coordonnées GPS brutes si aucun gouvernorat n'est disponible.</p>
     */
    private static String buildFullAddress(NominatimResult n, String lat, String lon) {

        // ── Résoudre le gouvernorat depuis les champs Nominatim ───────────────
        // Ordre de priorité : county → state → city (même logique que resolveGovernorateFromNominatim)
        String governorate = null;
        for (String candidate : new String[]{n.county, n.state, n.city}) {
            if (candidate != null && !candidate.isBlank()) {
                String mapped = GovernorateUtils.normalize(candidate);
                if (!mapped.isBlank()) {
                    governorate = mapped;
                    break;
                }
                // Candidat non reconnu comme gouvernorat tunisien — garder quand même
                if (governorate == null) governorate = candidate;
            }
        }

        // ── Format : "Gouvernorat, Tunisie" ───────────────────────────────────
        if (governorate != null && !governorate.isBlank()) {
            String country = n.country != null && !n.country.isBlank() ? n.country : "Tunisie";
            return governorate + ", " + country;
        }

        // ── Fallback : coordonnées GPS brutes ─────────────────────────────────
        if (lat != null && lon != null) {
            return formatCoordinates(lat, lon);
        }

        return "";
    }

    /**
     * Résout le gouvernorat officiel depuis les champs Nominatim.
     * Ordre : county → state → city → town → village.
     */
    private static String resolveGovernorateFromNominatim(NominatimResult n) {
        for (String candidate : new String[]{n.county, n.state, n.city}) {
            if (candidate != null && !candidate.isBlank()) {
                String mapped = GovernorateUtils.normalize(candidate);
                if (!mapped.isBlank()) return mapped;
            }
        }
        return "";
    }

    /**
     * Formate les coordonnées décimales en degrés lisibles.
     * Ex : {@code "36.8190°N, 10.1658°E"}
     */
    private static String formatCoordinates(String lat, String lon) {
        try {
            double latD = Double.parseDouble(lat);
            double lonD = Double.parseDouble(lon);
            String latDir = latD >= 0 ? "N" : "S";
            String lonDir = lonD >= 0 ? "E" : "W";
            return String.format("%.4f°%s, %.4f°%s",
                    Math.abs(latD), latDir, Math.abs(lonD), lonDir);
        } catch (NumberFormatException e) {
            return lat + ", " + lon;
        }
    }

    // ── HTTP ─────────────────────────────────────────────────────────────────

    private static String httpGet(String url, String userAgent) throws LocationDetectionException {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(TIMEOUT)
                    .GET();

            if (userAgent != null) {
                builder.header("User-Agent", userAgent);
            }

            HttpResponse<String> response = HTTP_CLIENT.send(
                    builder.build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200) {
                throw new LocationDetectionException(
                        "HTTP error " + response.statusCode() + " for " + url);
            }

            return response.body();

        } catch (IOException e) {
            throw new LocationDetectionException(
                    "No network connection. Please check your Internet access.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LocationDetectionException("Detection interrupted.", e);
        }
    }

    // ── Parsing JSON minimal (sans dépendance externe) ────────────────────────

    /** Extrait un champ JSON de type chaîne. */
    static String extractJsonString(String json, String field) {
        if (json == null || field == null) return null;
        Pattern p = Pattern.compile(
                "\"" + Pattern.quote(field) + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
        Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1)
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                    .replace("\\/", "/");
        }
        return null;
    }

    /** Extrait un champ JSON de type nombre (retourné comme chaîne). */
    static String extractJsonNumber(String json, String field) {
        if (json == null || field == null) return null;
        Pattern p = Pattern.compile(
                "\"" + Pattern.quote(field) + "\"\\s*:\\s*(-?[0-9]+(?:\\.[0-9]+)?)");
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : null;
    }

    /** Extrait le contenu brut d'un objet JSON imbriqué. */
    static String extractJsonObject(String json, String field) {
        if (json == null || field == null) return null;
        Pattern sp = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*\\{");
        Matcher sm = sp.matcher(json);
        if (!sm.find()) return null;
        int start = sm.end() - 1;
        int depth = 0;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return json.substring(start, i + 1);
            }
        }
        return null;
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    /** Retourne le premier argument non-null, ou {@code null} si tous sont null. */
    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        for (T v : values) {
            if (v != null) return v;
        }
        return null;
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }

    // ── Types de données ──────────────────────────────────────────────────────

    /** Résultat enrichi de la détection de localisation. */
    public static final class DetectedLocation {

        /** Gouvernorat officiel tunisien (ex : {@code "Ariana"}). Jamais vide. */
        public final String governorate;

        /**
         * Adresse complète lisible.
         * Ex : {@code "Rue de la Liberté, Ariana, Tunisie"}
         * Fallback : {@code "36.8190°N, 10.1658°E"} si adresse indisponible.
         * Peut être vide si les coordonnées sont aussi indisponibles.
         */
        public final String fullAddress;

        /** Latitude décimale brute (peut être {@code null}). */
        public final String lat;

        /** Longitude décimale brute (peut être {@code null}). */
        public final String lon;

        DetectedLocation(String governorate, String fullAddress, String lat, String lon) {
            this.governorate = governorate;
            this.fullAddress = fullAddress != null ? fullAddress : "";
            this.lat         = lat;
            this.lon         = lon;
        }

        /**
         * Retourne l'adresse complète si disponible, sinon le gouvernorat seul.
         * Utile pour afficher dans un champ texte unique.
         */
        public String bestAddress() {
            if (fullAddress != null && !fullAddress.isBlank()) return fullAddress;
            return governorate;
        }

        @Override
        public String toString() {
            return "DetectedLocation{governorate='" + governorate
                    + "', fullAddress='" + fullAddress
                    + "', lat=" + lat + ", lon=" + lon + "}";
        }
    }

    /** Résultat partiel de l'appel ip-api.com. */
    private static final class IpApiResult {
        String regionName;
        String city;
        String countryCode;
        String lat;
        String lon;
    }

    /** Champs extraits de la réponse Nominatim. */
    private static final class NominatimResult {
        String displayName;
        String houseNumber;
        String road;
        String suburb;
        String city;
        String county;
        String state;
        String postcode;
        String country;
    }

    // ── Exception dédiée ─────────────────────────────────────────────────────

    /**
     * Levée quand la détection échoue complètement.
     * Le message est toujours lisible par l'utilisateur final.
     */
    public static final class LocationDetectionException extends Exception {
        public LocationDetectionException(String message) {
            super(message);
        }
        public LocationDetectionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
