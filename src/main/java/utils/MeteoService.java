package utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MeteoService {

    private static final String API_KEY = "a516592a893263af249c49fcfe3f5e1e";
    private static final String BASE_URL =
            "https://api.openweathermap.org/data/2.5/weather";

    public static class DonneesMeteo {
        public double temperature;
        public double humidite;
        public double vent;
        public String description;
        public double latitude;
        public double longitude;
        public boolean succes;

        @Override
        public String toString() {
            return String.format(
                    "Temp: %.1f°C | Humidité: %.0f%% | Vent: %.1f km/h",
                    temperature, humidite, vent * 3.6);
        }
    }

    public DonneesMeteo getMeteo(String ville) {
        DonneesMeteo meteo = new DonneesMeteo();
        try {
            String villeClean = extraireVille(ville);
            String urlStr = BASE_URL + "?q=" + villeClean
                    + ",TN&appid=" + API_KEY
                    + "&units=metric&lang=fr";

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONObject json = new JSONObject(sb.toString());

                JSONObject main    = json.getJSONObject("main");
                JSONObject wind    = json.getJSONObject("wind");
                JSONObject coord   = json.getJSONObject("coord");
                JSONArray weatherArr = json.getJSONArray("weather");
                JSONObject weather = weatherArr.getJSONObject(0);

                meteo.temperature = main.getDouble("temp");
                meteo.humidite    = main.getDouble("humidity");
                meteo.vent        = wind.getDouble("speed");
                meteo.description = weather.getString("description");
                meteo.latitude    = coord.getDouble("lat");
                meteo.longitude   = coord.getDouble("lon");
                meteo.succes      = true;

            } else {
                meteo = getMeteoDefaut();
            }
        } catch (Exception e) {
            System.out.println("Erreur météo: " + e.getMessage());
            meteo = getMeteoDefaut();
        }
        return meteo;
    }

    private DonneesMeteo getMeteoDefaut() {
        DonneesMeteo meteo = new DonneesMeteo();
        meteo.temperature = 28.0;
        meteo.humidite    = 45.0;
        meteo.vent        = 3.5;
        meteo.description = "données par défaut";
        meteo.latitude    = 36.8065;
        meteo.longitude   = 10.1815;
        meteo.succes      = false;
        return meteo;
    }

    private String extraireVille(String zone) {
        if (zone == null || zone.isEmpty()) {
            System.out.println("⚠️ Zone vide, utilisation de Tunis par défaut");
            return "Tunis";
        }
        
        System.out.println("🔍 Extraction ville depuis zone: '" + zone + "'");
        
        // Si la zone contient une virgule, prendre la partie après la virgule
        // Exemple: "Zone Ouest-A, Ain Draham" → "Ain Draham"
        if (zone.contains(",")) {
            String partie = zone.substring(zone.lastIndexOf(",") + 1).trim();
            System.out.println("📍 Zone avec virgule détectée, extraction: '" + partie + "'");
            String result = partie.replace(" ", "+");
            System.out.println("   Résultat final: '" + result + "'");
            return result;
        }
        
        // Sinon, utiliser la zone telle quelle
        System.out.println("📍 Utilisation de la zone directement: '" + zone + "'");
        String result = zone.trim().replace(" ", "+");
        System.out.println("   Résultat final: '" + result + "'");
        return result;
    }
}