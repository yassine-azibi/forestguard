package service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * SERVICE API — WeatherApiService
 * Consomme l'API OpenWeatherMap pour récupérer
 * les données météo réelles des zones forestières.
 *
 * API gratuite : openweathermap.org → inscription → API Keys
 */
public class WeatherApiService {

    // ⚠ Remplacer par votre clé API OpenWeatherMap (inscription gratuite)
    private static final String API_KEY = "0769b304948b1c5993ec713a85904ffa";
    private static final String BASE_URL =
        "https://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s&units=metric&lang=fr";

    /**
     * Récupère la température actuelle d'une ville
     * @param ville ex: "Tunis", "Bizerte", "Jendouba"
     * @return température en °C, ou -1 si erreur
     */
    public double getTemperature(String ville) {
        try {
            String json = appelApi(ville);
            return extraireDouble(json, "\"temp\":");
        } catch (Exception e) {
            System.out.println("Erreur API météo temp : " + e.getMessage());
            return -1;
        }
    }

    /**
     * Récupère l'humidité actuelle d'une ville
     * @param ville ex: "Tunis"
     * @return humidité en %, ou -1 si erreur
     */
    public double getHumidite(String ville) {
        try {
            String json = appelApi(ville);
            return extraireDouble(json, "\"humidity\":");
        } catch (Exception e) {
            System.out.println("Erreur API météo hum : " + e.getMessage());
            return -1;
        }
    }

    /**
     * Récupère à la fois temp et humidité en un seul appel API
     * @return tableau [temperature, humidite] ou null si erreur
     */
    public double[] getMeteo(String ville) {
        try {
            String json = appelApi(ville);
            double temp = extraireDouble(json, "\"temp\":");
            double hum  = extraireDouble(json, "\"humidity\":");
            System.out.println("🌡  Météo " + ville + " : " + temp + "°C | " + hum + "%");
            return new double[]{temp, hum};
        } catch (Exception e) {
            System.out.println("Erreur API météo : " + e.getMessage());
            return null;
        }
    }

    // ── Méthodes privées ──────────────────────────────────────────────────────

    private String appelApi(String ville) throws Exception {
        String urlStr = String.format(BASE_URL,
                ville.replace(" ", "+"), API_KEY);
        HttpURLConnection conn =
                (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        int code = conn.getResponseCode();
        if (code != 200) throw new Exception("HTTP " + code);

        BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        return sb.toString();
    }

    private double extraireDouble(String json, String cle) {
        int idx = json.indexOf(cle);
        if (idx == -1) return -1;
        int start = idx + cle.length();
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        return Double.parseDouble(json.substring(start, end).trim());
    }
}
