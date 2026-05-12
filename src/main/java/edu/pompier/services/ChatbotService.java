package edu.pompier.services;

import edu.pompier.tools.MyConnection;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/**
 * ═══════════════════════════════════════════════════════════════
 *  ChatbotService — Chatbot IA ForestGuard
 * ═══════════════════════════════════════════════════════════════
 *
 * Utilise l'API Claude (claude-haiku, rapide + économique).
 * Avant chaque appel, collecte un contexte complet depuis la BD
 * (pompiers, alertes, affectations, interventions, performances)
 * et l'injecte dans le system prompt pour des réponses précises.
 *
 * Exemples de questions :
 *  - "Qui est le pompier le plus proche de l'incendie à Bizerte ?"
 *  - "Quel est le meilleur pompier cette semaine ?"
 *  - "Combien d'incendies a traité Islem ce mois-ci ?"
 *  - "Pourquoi Eya Krifi a été choisie pour cette mission ?"
 *  - "Quels pompiers sont disponibles maintenant ?"
 *  - "Donne-moi un résumé des activités de la semaine."
 */
public class ChatbotService {

    private static final String API_URL   = "https://openrouter.ai/api/v1/chat/completions";
    private static final String MODEL     = "openai/gpt-4o-mini"; // modele rapide et economique
    private static final int    MAX_TOKENS = 800;

    private final Connection cnx;

    // Historique de la conversation (pour le contexte multi-tour)
    private final List<Map<String, String>> historique = new ArrayList<>();

    public ChatbotService() {
        this.cnx = MyConnection.getInstance().getCnx();
    }

    // ═══════════════════════════════════════════════════════════
    //  Point d'entrée : envoyer un message et obtenir une réponse
    // ═══════════════════════════════════════════════════════════

    /**
     * Envoie un message au chatbot et retourne la réponse.
     * @param messageUtilisateur  Question de l'utilisateur
     * @return Réponse du chatbot
     */
    public String envoyerMessage(String messageUtilisateur) {
        // 1. Construire le contexte depuis la BD
        String contexte = construireContexte();

        // 2. System prompt avec contexte
        String systemPrompt =
                "Tu es l'assistant IA de ForestGuard, un systeme de gestion des pompiers\n" +
                        "et de detection d'incendies en Tunisie.\n" +
                        "Tu reponds toujours en francais, avec bienveillance et professionnalisme.\n\n" +
                        "Tu peux repondre a N'IMPORTE QUELLE question :\n" +
                        "- Questions sur ForestGuard (pompiers, alertes, interventions, performances)\n" +
                        "- Questions generales : incendies, forets, securite civile, nature\n" +
                        "- Conversations informelles, salutations, conseils, blagues\n" +
                        "- Toute autre question — tu es un assistant general intelligent\n\n" +
                        "DONNEES ACTUELLES FORESTGUARD (utilise si pertinent) :\n" +
                        contexte + "\n\n" +
                        "STYLE : Sois naturel et conversationnel. Pour les donnees : cite noms/scores/dates.\n" +
                        "Pour les questions generales : reponds librement. Adapte la longueur a la question.";

        // 3. Ajouter le message à l'historique
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", messageUtilisateur);
        historique.add(userMsg);

        // 4. Appel API
        String reponse = appelAPI(systemPrompt);

        // 5. Ajouter la réponse à l'historique
        if (reponse != null && !reponse.startsWith("ERREUR")) {
            Map<String, String> assistantMsg = new HashMap<>();
            assistantMsg.put("role", "assistant");
            assistantMsg.put("content", reponse);
            historique.add(assistantMsg);
        }

        // Limiter l'historique à 10 échanges
        while (historique.size() > 20) historique.remove(0);

        return reponse != null ? reponse : "Je n'ai pas pu traiter ta question. Réessaie.";
    }

    /** Remet à zéro l'historique de conversation */
    public void reinitialiser() {
        historique.clear();
    }

    // ═══════════════════════════════════════════════════════════
    //  Construction du contexte depuis la BD
    // ═══════════════════════════════════════════════════════════

    private String construireContexte() {
        StringBuilder ctx = new StringBuilder();

        ctx.append(getContextePompiers());
        ctx.append(getContexteAlertes());
        ctx.append(getContexteAffectations());
        ctx.append(getContexteInterventions());
        ctx.append(getContextePerformances());

        return ctx.toString();
    }

    private String getContextePompiers() {
        StringBuilder sb = new StringBuilder("=== POMPIERS ===\n");
        String sql =
                "SELECT p.id, p.nom, p.prenom, p.statut, p.ville, p.zone_adresse, " +
                        "p.latitude, p.longitude, f.nom as foret, c.niveau_requis as certif " +
                        "FROM pompier p " +
                        "LEFT JOIN foret f ON f.id = p.zone_id " +
                        "LEFT JOIN pompier_certification pc ON pc.id_pompier = p.id " +
                        "LEFT JOIN certification c ON c.id = pc.id_certification " +
                        "ORDER BY p.statut, p.nom";
        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                sb.append(String.format(
                        "- %s %s | statut:%s | niveau:%s | ville:%s | zone:%s | GPS:(%.4f,%.4f) | foret:%s\n",
                        rs.getString("prenom"), rs.getString("nom"),
                        rs.getString("statut"),
                        rs.getString("certif") != null ? rs.getString("certif") : "N/A",
                        rs.getString("ville") != null ? rs.getString("ville") : "?",
                        rs.getString("zone_adresse") != null ? rs.getString("zone_adresse") : "?",
                        rs.getDouble("latitude"), rs.getDouble("longitude"),
                        rs.getString("foret") != null ? rs.getString("foret") : "aucune"
                ));
            }
        } catch (SQLException e) {
            sb.append("(erreur lecture pompiers)\n");
        }
        return sb.toString();
    }

    private String getContexteAlertes() {
        StringBuilder sb = new StringBuilder("\n=== ALERTES RECENTES (30 derniers jours) ===\n");
        String sql =
                "SELECT id, type_alerte, niveau, localisation, date_alerte, statut " +
                        "FROM alerte " +
                        "WHERE date_alerte >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
                        "ORDER BY date_alerte DESC LIMIT 20";
        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                String loc = rs.getString("localisation");
                String nomLieu = PompierService.coordsVersNom(loc);
                sb.append(String.format(
                        "- ID#%d | %s | niveau:%s | lieu:%s | date:%s | statut:%s\n",
                        rs.getInt("id"),
                        rs.getString("type_alerte"),
                        rs.getString("niveau"),
                        nomLieu,
                        rs.getString("date_alerte") != null
                                ? rs.getString("date_alerte").substring(0, Math.min(16, rs.getString("date_alerte").length()))
                                : "?",
                        rs.getString("statut")
                ));
            }
        } catch (SQLException e) {
            sb.append("(erreur lecture alertes)\n");
        }
        return sb.toString();
    }

    private String getContexteAffectations() {
        StringBuilder sb = new StringBuilder("\n=== AFFECTATIONS RECENTES ===\n");
        String sql =
                "SELECT af.id, p.nom, p.prenom, a.type_alerte, a.niveau, a.localisation, " +
                        "af.date_affectation, af.statut, af.distance_km, af.score_selection, af.mode_affectation " +
                        "FROM affectation af " +
                        "JOIN pompier p ON p.id = af.id_pompier " +
                        "JOIN alerte a ON a.id = af.id_alerte " +
                        "WHERE af.statut != 'annule' AND af.date_affectation >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
                        "ORDER BY af.date_affectation DESC LIMIT 20";
        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                String nomLieu = PompierService.coordsVersNom(rs.getString("localisation"));
                sb.append(String.format(
                        "- %s %s → %s à %s | score:%.1f | dist:%.1fkm | statut:%s | mode:%s | date:%s\n",
                        rs.getString("prenom"), rs.getString("nom"),
                        rs.getString("type_alerte"), nomLieu,
                        rs.getDouble("score_selection"), rs.getDouble("distance_km"),
                        rs.getString("statut"), rs.getString("mode_affectation"),
                        rs.getString("date_affectation") != null
                                ? rs.getString("date_affectation").substring(0, Math.min(16, rs.getString("date_affectation").length()))
                                : "?"
                ));
            }
        } catch (SQLException e) {
            sb.append("(erreur lecture affectations)\n");
        }
        return sb.toString();
    }

    private String getContexteInterventions() {
        StringBuilder sb = new StringBuilder("\n=== INTERVENTIONS RECENTES ===\n");
        String sql =
                "SELECT agent_name, alert_zone, statut, start_date, end_date, resultat " +
                        "FROM intervention " +
                        "WHERE start_date >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
                        "ORDER BY start_date DESC LIMIT 20";
        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                String end = rs.getString("end_date") != null ? rs.getString("end_date").substring(0, 16) : "en cours";
                sb.append(String.format(
                        "- %s | zone:%s | statut:%s | debut:%s | fin:%s | resultat:%s\n",
                        rs.getString("agent_name"),
                        rs.getString("alert_zone"),
                        rs.getString("statut"),
                        rs.getString("start_date") != null ? rs.getString("start_date").substring(0, 16) : "?",
                        end,
                        rs.getString("resultat") != null ? rs.getString("resultat") : "N/A"
                ));
            }
        } catch (SQLException e) {
            sb.append("(erreur lecture interventions)\n");
        }
        return sb.toString();
    }

    private String getContextePerformances() {
        StringBuilder sb = new StringBuilder("\n=== PERFORMANCES DU MOIS ===\n");
        try {
            PerformanceIA ia = new PerformanceIA();
            List<PerformanceIA.ResultatPerformance> resultats = ia.evaluer("mois");
            if (resultats.isEmpty()) {
                sb.append("(aucune donnée de performance ce mois)\n");
            } else {
                for (int i = 0; i < Math.min(5, resultats.size()); i++) {
                    PerformanceIA.ResultatPerformance r = resultats.get(i);
                    sb.append(String.format(
                            "- #%d %s %s | score:%.1f/100 | missions:%d | reussite:%.0f%% | critiques:%d\n",
                            i + 1, r.prenom, r.nom, r.scoreTotal,
                            r.nbMissions, r.tauxReussite * 100, r.nbCritiques
                    ));
                }
            }
        } catch (Exception e) {
            sb.append("(erreur calcul performances)\n");
        }
        return sb.toString();
    }

    // ═══════════════════════════════════════════════════════════
    //  Appel API Claude
    // ═══════════════════════════════════════════════════════════


    private String appelAPI(String systemPrompt) {
        try {
            // Format OpenAI/OpenRouter : system + historique
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

            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + getApiKey());
            conn.setRequestProperty("HTTP-Referer", "https://forestguard.app");
            conn.setRequestProperty("X-OpenRouter-Title", "ForestGuard");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            InputStream is = code == 200 ? conn.getInputStream() : conn.getErrorStream();
            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) response.append(line);
            }

            if (code == 200) {
                return extraireTexteOpenAI(response.toString());
            } else {
                System.out.println("[Chatbot] Erreur API " + code + " : " + response);
                return "ERREUR : Je n'ai pas pu contacter le serveur IA (code " + code + ").";
            }
        } catch (Exception e) {
            System.out.println("[Chatbot] Exception : " + e.getMessage());
            return "ERREUR : " + e.getMessage();
        }
    }

    /**
     * Extrait le texte depuis format OpenAI/OpenRouter :
     * {"choices":[{"message":{"content":"REPONSE"}}]}
     */
    private String extraireTexteOpenAI(String json) {
        int idx = json.indexOf("\"content\":");
        if (idx == -1) return "Reponse vide du serveur.";
        int debut = json.indexOf("\"", idx + 10) + 1;
        if (debut <= 0) return "Reponse vide.";
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

    private String extraireTexte(String json) {
        // Pattern simple : "text":"..."
        int idx = json.indexOf("\"text\":");
        if (idx == -1) return "Réponse incompréhensible du serveur.";
        int debut = json.indexOf("\"", idx + 7) + 1;
        int fin   = debut;
        while (fin < json.length()) {
            if (json.charAt(fin) == '"' && json.charAt(fin - 1) != '\\') break;
            fin++;
        }
        return json.substring(debut, fin)
                .replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\'", "'")
                .replace("\\\\", "\\");
    }

    /** Échappe les caractères spéciaux JSON */
    private String echapper(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Récupère la clé API depuis :
     *   1. Variable d'environnement ANTHROPIC_API_KEY
     *   2. Fichier config.properties dans les resources
     *   3. Valeur par défaut (à remplacer)
     */
    private String getApiKey() {
        // Methode 1 : variable d'environnement
        String key = System.getenv("OPENROUTER_API_KEY");
        if (key != null && !key.isBlank()) return key;

        // Methode 2 : fichier config.properties dans resources
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                key = props.getProperty("openrouter.api.key");
                if (key != null && !key.isBlank()) return key;
            }
        } catch (Exception ignored) {}

        // Methode 3 : cle directe
        return "VOTRE_CLE_OPENROUTER_ICI";
    }
}
