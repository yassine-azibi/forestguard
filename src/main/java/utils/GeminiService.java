package utils;

import dao.InterventionDAO;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * GeminiService — Assistant IA ForestGuard via OpenRouter (gpt-4o-mini)
 * Remplace l'ancien appel Google Gemini (quota épuisé)
 */
public class GeminiService {

    private static final String API_URL    = "https://openrouter.ai/api/v1/chat/completions";
    private static final String MODEL      = "openai/gpt-4o-mini";
    private static final int    MAX_TOKENS = 800;

    private final InterventionDAO interventionDAO = new InterventionDAO();
    private final MeteoService meteoService = new MeteoService();
    private final String agentName;

    // Historique conversation (multi-tour)
    private final List<Map<String, String>> historique = new ArrayList<>();

    public GeminiService(String agentName) {
        this.agentName = agentName;
    }

    /**
     * Envoie un message et retourne la réponse IA
     */
    public String envoyerMessage(String question) {
        String contexte = construireContexte();

        String systemPrompt =
                "Tu es l'assistant IA de ForestGuard, système de gestion des interventions " +
                        "pour la surveillance des incendies forestiers en Tunisie.\n" +
                        "Tu réponds en français, de manière concise et professionnelle.\n\n" +
                        "DONNÉES ACTUELLES :\n" + contexte;

        // Ajouter à l'historique
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", question);
        historique.add(userMsg);

        // Appel API
        String reponse = appelAPI(systemPrompt);

        // Ajouter réponse à l'historique
        if (reponse != null && !reponse.startsWith("ERREUR") && !reponse.startsWith("❌")) {
            Map<String, String> assistantMsg = new HashMap<>();
            assistantMsg.put("role", "assistant");
            assistantMsg.put("content", reponse);
            historique.add(assistantMsg);
        }

        // Limiter à 20 messages
        while (historique.size() > 20) historique.remove(0);

        return reponse != null ? reponse : "Je n'ai pas pu traiter votre question.";
    }

    /** Réinitialise la conversation */
    public void reinitialiser() {
        historique.clear();
    }

    // ═══════════════════════════════════════════════════════════
    //  Contexte depuis vos données
    // ═══════════════════════════════════════════════════════════

    private String construireContexte() {
        StringBuilder ctx = new StringBuilder();

        ctx.append("Agent : ").append(agentName).append("\n");

        int enCours = interventionDAO.countByStatut(agentName, "In Progress");
        int terminees = interventionDAO.countByStatut(agentName, "Completed");
        int cetteSemaine = interventionDAO.countThisWeek(agentName);

        ctx.append("Interventions en cours : ").append(enCours).append("\n");
        ctx.append("Terminées : ").append(terminees).append("\n");
        ctx.append("Cette semaine : ").append(cetteSemaine).append("\n\n");

        MeteoService.DonneesMeteo meteo = meteoService.getMeteo("Tunis");
        if (meteo.succes) {
            int score = IAService.calculerScoreRisque(
                    meteo.temperature, meteo.humidite, meteo.vent, "haute");

            ctx.append(String.format("Météo : %.1f°C, %.0f%% humidité, vent %.1f km/h\n",
                    meteo.temperature, meteo.humidite, meteo.vent * 3.6));
            ctx.append("Conditions : ").append(meteo.description).append("\n");
            ctx.append(String.format("Score risque : %d/100 (%s)\n\n",
                    score, IAService.getNiveauRisque(score)));
        }

        ctx.append("Interventions récentes :\n");
        interventionDAO.getByAgent(agentName).stream().limit(3).forEach(i -> {
            ctx.append(String.format("- %s | %s\n", i.getAlertZone(), i.getStatut()));
        });

        return ctx.toString();
    }

    // ═══════════════════════════════════════════════════════════
    //  Appel API OpenRouter
    // ═══════════════════════════════════════════════════════════

    private String appelAPI(String systemPrompt) {
        try {
            StringBuilder messagesJson = new StringBuilder("[");
            messagesJson.append("{\"role\":\"system\",\"content\":\"")
                    .append(echapper(systemPrompt))
                    .append("\"}");

            for (Map<String, String> msg : historique) {
                messagesJson.append(",{\"role\":\"")
                        .append(echapper(msg.get("role")))
                        .append("\",\"content\":\"")
                        .append(echapper(msg.get("content")))
                        .append("\"}");
            }
            messagesJson.append("]");

            String body = "{\"model\":\"" + MODEL + "\"," +
                    "\"max_tokens\":" + MAX_TOKENS + "," +
                    "\"messages\":" + messagesJson + "}";

            System.out.println("[IA] Appel OpenRouter...");

            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + getApiKey());
            conn.setRequestProperty("HTTP-Referer", "https://forestguard.app");
            conn.setRequestProperty("X-Title", "ForestGuard");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            InputStream is = (code == 200) ? conn.getInputStream() : conn.getErrorStream();
            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) response.append(line);
            }

            if (code == 200) {
                return extraireTexte(response.toString());
            } else {
                System.out.println("[IA] Erreur " + code + " : " + response);
                if (code == 401) return "❌ Clé API invalide.";
                if (code == 429) return "⏳ Trop de requêtes. Attendez 1 minute.";
                return "❌ Erreur serveur (code " + code + ")";
            }

        } catch (Exception e) {
            System.out.println("[IA] Exception : " + e.getMessage());
            return "❌ Erreur réseau : " + e.getMessage();
        }
    }

    private String extraireTexte(String json) {
        int idx = json.indexOf("\"content\":");
        if (idx == -1) return "Réponse vide.";
        int debut = json.indexOf("\"", idx + 10) + 1;
        if (debut <= 0) return "Réponse vide.";
        int fin = debut;
        while (fin < json.length()) {
            if (json.charAt(fin) == '"' && json.charAt(fin - 1) != '\\') break;
            fin++;
        }
        return json.substring(debut, fin)
                .replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private String echapper(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Récupère la clé API OpenRouter
     */
    private String getApiKey() {
        // 1. Variable d'environnement
        String key = System.getenv("OPENROUTER_API_KEY");
        if (key != null && !key.isBlank()) return key;

        // 2. Fichier config.properties
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                key = props.getProperty("openrouter.api.key");
                if (key != null && !key.isBlank()) return key;
            }
        } catch (Exception ignored) {}

        // 3. Clé directe (⚠️ REMPLACEZ PAR VOTRE VRAIE CLÉ)
        return "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";
    }

    // ═══════════════════════════════════════════════════════════
    //  Méthodes de compatibilité (gardées pour ne pas casser les controllers)
    // ═══════════════════════════════════════════════════════════

    public String analyseRisqueActuel() {
        return envoyerMessage("Analyse le risque actuel et donne 3 recommandations tactiques.");
    }

    public String conseilsEquipements(String typeAlerte, String niveau) {
        return envoyerMessage("Quels équipements pour " + typeAlerte + " niveau " + niveau + " ?");
    }

    public String protocoleSecurite(String zone) {
        return envoyerMessage("Protocole sécurité pour zone : " + zone);
    }
}