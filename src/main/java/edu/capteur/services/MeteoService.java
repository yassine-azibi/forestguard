package edu.capteur.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * ForestGuard – Service Météo (OpenWeatherMap)
 * Clé gratuite : https://openweathermap.org/api
 */
public class MeteoService {

    private static final String OWM_API_KEY = "VOTRE_CLE_OPENWEATHERMAP";
    private static final String OWM_URL =
        "https://api.openweathermap.org/data/2.5/weather?lat=%s&lon=%s&appid=%s&units=metric&lang=fr";

    private final HttpClient httpClient;

    public MeteoService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8)).build();
    }

    // ── Seuils validés pour capteurs IoT forestiers ──────────────────────
    public static final double TEMP_MAX_NORMAL    = 40.0;  // °C — au-delà : attention
    public static final double TEMP_MAX_CRITIQUE  = 55.0;  // °C — au-delà : panne probable
    public static final int    HUMID_MAX_NORMAL   = 75;    // %  — au-delà : condensation
    public static final int    HUMID_MAX_CRITIQUE = 90;    // %  — au-delà : court-circuit

    // ── Classe DonneesMeteo ───────────────────────────────────────────────
    public static class DonneesMeteo {
        public final double  temperature;
        public final double  ressenti;
        public final int     humidite;
        public final double  vitesseVent;
        public final String  description;
        public final String  icone;
        public final int     nuages;
        public final double  pluie1h;
        public final boolean disponible;

        public DonneesMeteo(double temperature, double ressenti, int humidite,
                            double vitesseVent, String description, String icone,
                            int nuages, double pluie1h) {
            this.temperature = temperature;
            this.ressenti    = ressenti;
            this.humidite    = humidite;
            this.vitesseVent = vitesseVent;
            this.description = description;
            this.icone       = icone;
            this.nuages      = nuages;
            this.pluie1h     = pluie1h;
            this.disponible  = true;
        }

        /** Constructeur fallback */
        public DonneesMeteo() {
            this.temperature = 25.0; this.ressenti = 25.0; this.humidite = 50;
            this.vitesseVent = 10.0; this.description = "Données météo indisponibles";
            this.icone = "🌡"; this.nuages = 0; this.pluie1h = 0.0;
            this.disponible = false;
        }

        /**
         * Score de risque PANNE CAPTEUR (0–100)
         *
         * Température :
         *   < 40°C          →  0 pt
         *   40°C – 55°C     →  0 à 30 pts (interpolation linéaire)
         *   > 55°C          →  60 pts
         *
         * Humidité :
         *   < 75%           →  0 pt
         *   75% – 90%       →  0 à 25 pts (interpolation linéaire)
         *   > 90%           →  45 pts
         *
         * Pluie :
         *   > 10 mm/h       →  +15 pts
         *   5–10 mm/h       →  +8 pts
         */
        public int scoreRisquePanneCapteur() {
            int score = 0;

            // Température
            if (temperature > TEMP_MAX_CRITIQUE) {
                score += 60;
            } else if (temperature > TEMP_MAX_NORMAL) {
                score += (int)((temperature - TEMP_MAX_NORMAL)
                             / (TEMP_MAX_CRITIQUE - TEMP_MAX_NORMAL) * 30);
            }

            // Humidité
            if (humidite > HUMID_MAX_CRITIQUE) {
                score += 45;
            } else if (humidite > HUMID_MAX_NORMAL) {
                score += (int)((humidite - HUMID_MAX_NORMAL)
                             / (double)(HUMID_MAX_CRITIQUE - HUMID_MAX_NORMAL) * 25);
            }

            // Pluie
            if (pluie1h > 10)     score += 15;
            else if (pluie1h > 5) score += 8;

            return Math.max(0, Math.min(100, score));
        }

        /**
         * Niveau de risque panne :
         *   FAIBLE   : 0  – 19
         *   MODÉRÉ   : 20 – 49
         *   ÉLEVÉ    : 50 – 69
         *   CRITIQUE : 70 – 100
         */
        public String niveauRisquePanne() {
            int s = scoreRisquePanneCapteur();
            if (s >= 70) return "CRITIQUE";
            if (s >= 50) return "ÉLEVÉ";
            if (s >= 20) return "MODÉRÉ";
            return "FAIBLE";
        }

        /** Détail des dépassements de seuils */
        public String detailSeuils() {
            StringBuilder sb = new StringBuilder();
            if (temperature > TEMP_MAX_CRITIQUE)
                sb.append(String.format("Temp %.1f°C > seuil critique %.0f°C. ",
                        temperature, TEMP_MAX_CRITIQUE));
            else if (temperature > TEMP_MAX_NORMAL)
                sb.append(String.format("Temp %.1f°C > seuil normal %.0f°C. ",
                        temperature, TEMP_MAX_NORMAL));
            if (humidite > HUMID_MAX_CRITIQUE)
                sb.append(String.format("Humidité %d%% > seuil critique %d%%. ",
                        humidite, HUMID_MAX_CRITIQUE));
            else if (humidite > HUMID_MAX_NORMAL)
                sb.append(String.format("Humidité %d%% > seuil normal %d%%. ",
                        humidite, HUMID_MAX_NORMAL));
            return sb.length() > 0 ? sb.toString().trim() : "Paramètres dans les normes.";
        }

        /** Score risque incendie (pour la carte) */
        public int scoreRisqueIncendie() {
            int score = 0;
            if (temperature > 40) score += 40;
            else if (temperature > 35) score += 30;
            else if (temperature > 30) score += 20;
            else if (temperature > 25) score += 10;
            if (humidite < 20) score += 35;
            else if (humidite < 35) score += 25;
            else if (humidite < 50) score += 15;
            else if (humidite < 65) score += 5;
            if (vitesseVent > 60) score += 25;
            else if (vitesseVent > 40) score += 18;
            else if (vitesseVent > 25) score += 10;
            if (pluie1h > 5) score -= 30;
            else if (pluie1h > 1) score -= 15;
            return Math.max(0, Math.min(100, score));
        }

        public String niveauRisqueIncendie() {
            int s = scoreRisqueIncendie();
            if (s >= 70) return "CRITIQUE";
            if (s >= 50) return "ÉLEVÉ";
            if (s >= 30) return "MODÉRÉ";
            return "FAIBLE";
        }

        public String toResume() {
            return String.format("%s %.1f°C | Humidité %d%% | Vent %.0f km/h | %s",
                    icone, temperature, humidite, vitesseVent, description);
        }
    }

    // ── Méthodes publiques ────────────────────────────────────────────────

    /** Récupère la météo (sans statut — pour la carte) */
    public DonneesMeteo getMeteo(double latitude, double longitude) {
        return getMeteo(latitude, longitude, "actif");
    }

    /** Récupère la météo avec simulation cohérente selon le statut du capteur */
    public DonneesMeteo getMeteo(double latitude, double longitude, String statut) {
        if (OWM_API_KEY.equals("VOTRE_CLE_OPENWEATHERMAP") || OWM_API_KEY.isBlank()) {
            return simulerMeteoParStatut(statut, latitude, longitude);
        }
        try {
            String url = String.format(OWM_URL, latitude, longitude, OWM_API_KEY);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url)).GET().timeout(Duration.ofSeconds(8)).build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) return parseReponse(response.body());
            return simulerMeteoParStatut(statut, latitude, longitude);
        } catch (Exception e) {
            return simulerMeteoParStatut(statut, latitude, longitude);
        }
    }

    // ── Simulation par statut ─────────────────────────────────────────────
    /**
     * Génère des valeurs cohérentes avec le statut du capteur.
     * Utilisée quand la clé OWM n'est pas configurée.
     */
    public DonneesMeteo simulerMeteoParStatut(String statut, double lat, double lon) {
        long seed = (long)(lat * 1000 + lon * 100) + System.currentTimeMillis() / 3600000;
        java.util.Random rnd = new java.util.Random(seed);

        double temp, humidite, vent, pluie;
        String desc;

        switch (statut != null ? statut : "actif") {
            case "en_panne" -> {
                // Conditions critiques : temp > 55°C OU humidité > 90%
                if (rnd.nextBoolean()) {
                    temp     = 56 + rnd.nextDouble() * 12;
                    humidite = 45 + rnd.nextInt(30);
                } else {
                    temp     = 30 + rnd.nextDouble() * 10;
                    humidite = 91 + rnd.nextInt(8);
                }
                vent  = 5 + rnd.nextDouble() * 20;
                pluie = humidite > 85 ? rnd.nextDouble() * 8 : 0;
                desc  = temp > 55 ? "chaleur extrême" : "humidité saturée";
            }
            case "inactif" -> {
                // Conditions d'attention : temp 40–55°C OU humidité 75–90%
                if (rnd.nextBoolean()) {
                    temp     = 40 + rnd.nextDouble() * 14;
                    humidite = 50 + rnd.nextInt(25);
                } else {
                    temp     = 28 + rnd.nextDouble() * 10;
                    humidite = 76 + rnd.nextInt(14);
                }
                vent  = 5 + rnd.nextDouble() * 25;
                pluie = humidite > 80 ? rnd.nextDouble() * 3 : 0;
                desc  = temp > 40 ? "chaleur élevée" : "humidité élevée";
            }
            default -> {
                // actif — conditions normales : temp < 40°C ET humidité < 75%
                temp     = 18 + rnd.nextDouble() * 20;
                humidite = 30 + rnd.nextInt(44);
                vent     = 5 + rnd.nextDouble() * 30;
                pluie    = rnd.nextDouble() < 0.2 ? rnd.nextDouble() * 3 : 0;
                String[] descs = {"ciel dégagé", "quelques nuages", "partiellement nuageux"};
                desc = descs[rnd.nextInt(descs.length)];
            }
        }

        double ressenti = temp - 2 + rnd.nextDouble() * 4;
        int    nuages   = rnd.nextInt(70);
        String icone    = emojiMeteo(desc, temp);

        return new DonneesMeteo(temp, ressenti, (int)humidite, vent,
                desc + " (simulé)", icone, nuages, pluie);
    }

    // ── Parsing OWM ───────────────────────────────────────────────────────
    private DonneesMeteo parseReponse(String json) {
        try {
            double temp     = extraireDouble(json, "\"temp\":");
            double ressenti = extraireDouble(json, "\"feels_like\":");
            int    humidite = (int) extraireDouble(json, "\"humidity\":");
            double vent     = extraireDouble(json, "\"speed\":") * 3.6;
            int    nuages   = (int) extraireDouble(json, "\"all\":");
            String desc     = extraireString(json, "\"description\":\"");
            double pluie    = json.contains("\"rain\"") ? extraireDouble(json, "\"1h\":") : 0.0;
            return new DonneesMeteo(temp, ressenti, humidite, vent,
                    desc, emojiMeteo(desc, temp), nuages, pluie);
        } catch (Exception e) { return new DonneesMeteo(); }
    }

    private double extraireDouble(String json, String cle) {
        int idx = json.indexOf(cle);
        if (idx == -1) return 0.0;
        int start = idx + cle.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end))
                || json.charAt(end) == '.' || json.charAt(end) == '-')) end++;
        try { return Double.parseDouble(json.substring(start, end)); }
        catch (Exception e) { return 0.0; }
    }

    private String extraireString(String json, String cle) {
        int idx = json.indexOf(cle);
        if (idx == -1) return "";
        int start = idx + cle.length();
        int end = json.indexOf("\"", start);
        return end > start ? json.substring(start, end) : "";
    }

    private String emojiMeteo(String desc, double temp) {
        if (desc == null) return "🌡";
        String d = desc.toLowerCase();
        if (d.contains("orage") || d.contains("thunder")) return "⛈";
        if (d.contains("neige") || d.contains("snow"))    return "❄️";
        if (d.contains("pluie") || d.contains("rain"))    return "🌧";
        if (d.contains("bruine") || d.contains("drizzle")) return "🌦";
        if (d.contains("brouillard") || d.contains("fog")) return "🌫";
        if (d.contains("nuageux") || d.contains("cloud")) return "☁️";
        if (d.contains("partiellement") || d.contains("few")) return "⛅";
        if (temp > 35) return "🔥";
        return "☀️";
    }
}
