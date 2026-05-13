package ForestGuard.services;

import org.json.JSONObject;
import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MeteoService {

    //   API OpenWeatherMap
    private static final String API_KEY = "94085706302336109cf1c0eae525b6e6";
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather";
    private static final String CITY     = "Tunis";   // votre ville
    private static final String UNITS    = "metric";  // Celsius
    private static final String LANG     = "fr";

    // ════════════════════════════════════════════════════════
    // Résultat météo
    // ════════════════════════════════════════════════════════
    public static class DonneesMeteo {
        public final double  temperature;
        public final double  humidite;
        public final double  vent;         // km/h
        public final String  description;
        public final String  ville;
        public final String  icone;        // code icone
        public final double  tempRessentie;
        public final double  tempMin;
        public final double  tempMax;
        public final int     nuages;       // % couverture nuageuse
        public final boolean succes;
        public final String  erreur;

        // Constructeur succès
        public DonneesMeteo(double temperature, double humidite,
                            double vent, String description, String ville,
                            String icone, double tempRessentie,
                            double tempMin, double tempMax, int nuages) {
            this.temperature  = temperature;
            this.humidite     = humidite;
            this.vent         = vent;
            this.description  = description;
            this.ville        = ville;
            this.icone        = icone;
            this.tempRessentie = tempRessentie;
            this.tempMin      = tempMin;
            this.tempMax      = tempMax;
            this.nuages       = nuages;
            this.succes       = true;
            this.erreur       = null;
        }

        // Constructeur erreur
        public DonneesMeteo(String erreur) {
            this.temperature  = 0;
            this.humidite     = 0;
            this.vent         = 0;
            this.description  = "";
            this.ville        = "";
            this.icone        = "";
            this.tempRessentie = 0;
            this.tempMin      = 0;
            this.tempMax      = 0;
            this.nuages       = 0;
            this.succes       = false;
            this.erreur       = erreur;
        }

        // Évaluation impact sur forêt
        public String getImpactForet() {
            if (temperature > 35 && humidite < 30 && vent > 40)
                return "TRES DANGEREUX";
            if (temperature > 30 && humidite < 40)
                return "DANGEREUX";
            if (temperature > 25 && humidite < 50)
                return "MODERE";
            return "FAVORABLE";
        }

        public String getCouleurImpact() {
            return switch (getImpactForet()) {
                case "TRES DANGEREUX" -> "#dc2626";
                case "DANGEREUX"      -> "#f97316";
                case "MODERE"         -> "#eab308";
                default               -> "#16a34a";
            };
        }

        public String getEmojiMeteo() {
            if (description.contains("pluie"))  return "🌧";
            if (description.contains("nuage"))  return "⛅";
            if (description.contains("orage"))  return "⛈";
            if (description.contains("neige"))  return "❄";
            if (description.contains("brume"))  return "🌫";
            return "☀";
        }
    }

    // ════════════════════════════════════════════════════════
    // Appel API principal
    // ════════════════════════════════════════════════════════
    public static DonneesMeteo getMeteo() {
        return getMeteoVille(CITY);
    }

    public static DonneesMeteo getMeteoVille(String ville) {
        try {
            String urlStr = BASE_URL +
                    "?q=" + ville +
                    "&appid=" + API_KEY +
                    "&units=" + UNITS +
                    "&lang=" + LANG;

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int code = conn.getResponseCode();
            if (code != 200) {
                return new DonneesMeteo("Erreur API : code " + code);
            }

            BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String ligne;
            while ((ligne = br.readLine()) != null) sb.append(ligne);
            br.close();

            return parseReponse(sb.toString());

        } catch (Exception e) {
            return new DonneesMeteo("Connexion impossible : " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════
    // Parser la réponse JSON
    // ════════════════════════════════════════════════════════
    private static DonneesMeteo parseReponse(String json) {
        try {
            JSONObject obj  = new JSONObject(json);
            JSONObject main = obj.getJSONObject("main");
            JSONObject wind = obj.getJSONObject("wind");
            JSONObject clouds = obj.getJSONObject("clouds");
            JSONArray  weather = obj.getJSONArray("weather");

            double temperature  = main.getDouble("temp");
            double humidite     = main.getDouble("humidity");
            double tempRessentie = main.getDouble("feels_like");
            double tempMin      = main.getDouble("temp_min");
            double tempMax      = main.getDouble("temp_max");
            double ventMs       = wind.getDouble("speed");
            double ventKmh      = ventMs * 3.6;
            int    nuages       = clouds.getInt("all");
            String description  = weather.getJSONObject(0)
                    .getString("description");
            String icone        = weather.getJSONObject(0)
                    .getString("icon");
            String ville        = obj.getString("name");

            return new DonneesMeteo(temperature, humidite, ventKmh,
                    description, ville, icone, tempRessentie,
                    tempMin, tempMax, nuages);

        } catch (Exception e) {
            return new DonneesMeteo("Erreur parsing : " + e.getMessage());
        }
    }
}