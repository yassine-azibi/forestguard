package edu.capteur.services;

import edu.capteur.entities.Capteur;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * ForestGuard – Service IA Local (Ollama + Llama3)
 * Uniquement pour capteurs ACTIFS.
 * Score basé sur température + humidité selon les seuils validés.
 */
public class OllamaService {

    private static final String OLLAMA_URL  = "http://localhost:11434/api/generate";
    private static final String MODEL       = "llama3.2";
    private static final int    TIMEOUT_SEC = 30;

    private final HttpClient httpClient;

    public OllamaService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5)).build();
    }

    // ── Résultat structuré ──────────────────────────────────────────────
    public static class ResultatIA {
        public final int     scoreRisque;
        public final String  niveau;
        public final String  analyse;
        public final String  action1;
        public final String  action2;
        public final String  action3;
        public final String  delaiEstime;
        public final boolean ollamaDisponible;

        public ResultatIA(int scoreRisque, String niveau, String analyse,
                          String action1, String action2, String action3,
                          String delaiEstime, boolean ollamaDisponible) {
            this.scoreRisque      = scoreRisque;
            this.niveau           = niveau;
            this.analyse          = analyse;
            this.action1          = action1;
            this.action2          = action2;
            this.action3          = action3;
            this.delaiEstime      = delaiEstime;
            this.ollamaDisponible = ollamaDisponible;
        }
    }

    // ── Analyse via Ollama ──────────────────────────────────────────────
    public ResultatIA analyser(Capteur capteur,
                               MeteoService.DonneesMeteo meteo,
                               String donneeRecente) {
        if (!isOllamaDisponible()) {
            return analyseLocaleEnrichie(capteur, meteo);
        }
        String prompt = construirePrompt(capteur, meteo, donneeRecente);
        try {
            String jsonBody = "{"
                + "\"model\": \"" + MODEL + "\","
                + "\"prompt\": \"" + escapeJson(prompt) + "\","
                + "\"stream\": false,"
                + "\"options\": {\"temperature\": 0.3, \"num_predict\": 400}"
                + "}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OLLAMA_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(TIMEOUT_SEC))
                    .build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String texteIA = extraireReponseOllama(response.body());
                return parseResultatIA(texteIA, capteur, meteo);
            }
            return analyseLocaleEnrichie(capteur, meteo);
        } catch (Exception e) {
            return analyseLocaleEnrichie(capteur, meteo);
        }
    }

    public boolean isOllamaDisponible() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:11434/api/tags"))
                    .GET().timeout(Duration.ofSeconds(3)).build();
            return httpClient.send(req, HttpResponse.BodyHandlers.ofString())
                             .statusCode() == 200;
        } catch (Exception e) { return false; }
    }

    // ── Prompt ──────────────────────────────────────────────────────────
    private String construirePrompt(Capteur capteur,
                                    MeteoService.DonneesMeteo meteo,
                                    String donneeRecente) {
        return "Tu es un expert en maintenance predictive de capteurs IoT forestiers. "
            + "Ce capteur est ACTIF. Analyse le risque qu il tombe en panne "
            + "selon la temperature et l humidite actuelles.\n\n"
            + "CAPTEUR:\n"
            + "- ID: C" + capteur.getId() + "\n"
            + "- Nom: " + capteur.getNom() + "\n"
            + "- Type: " + capteur.getType() + "\n"
            + "- Localisation: " + capteur.getLocalisation() + "\n"
            + (donneeRecente != null ? "- Mesure recente: " + donneeRecente + "\n" : "")
            + "\nCONDITIONS ENVIRONNEMENTALES:\n"
            + "- Temperature: " + String.format("%.1f", meteo.temperature) + "°C"
            + " (normale < " + MeteoService.TEMP_MAX_NORMAL + "°C"
            + " | critique > " + MeteoService.TEMP_MAX_CRITIQUE + "°C)\n"
            + "- Humidite: " + meteo.humidite + "%"
            + " (normale < " + MeteoService.HUMID_MAX_NORMAL + "%"
            + " | critique > " + MeteoService.HUMID_MAX_CRITIQUE + "%)\n"
            + "- Depassements: " + meteo.detailSeuils() + "\n"
            + "- Score risque panne: " + meteo.scoreRisquePanneCapteur() + "/100\n"
            + "\nRetourne ce JSON exact:\n"
            + "{\n"
            + "  \"score\": 45,\n"
            + "  \"niveau\": \"MODERE\",\n"
            + "  \"analyse\": \"Prediction du risque de panne en 2-3 phrases en francais\",\n"
            + "  \"action1\": \"Action preventive concrete\",\n"
            + "  \"action2\": \"Deuxieme action\",\n"
            + "  \"action3\": \"Troisieme action\",\n"
            + "  \"delai\": \"3 jours\"\n"
            + "}\n"
            + "Niveaux: FAIBLE (0-19), MODERE (20-49), ELEVE (50-69), CRITIQUE (70-100).";
    }

    // ── Parsing réponse Ollama ───────────────────────────────────────────
    private String extraireReponseOllama(String json) {
        int idx = json.indexOf("\"response\":\"");
        if (idx == -1) return "";
        int start = idx + 12;
        StringBuilder sb = new StringBuilder();
        int i = start;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                if (next == '"')  { sb.append('"');  i += 2; continue; }
                if (next == 'n')  { sb.append('\n'); i += 2; continue; }
                if (next == '\\') { sb.append('\\'); i += 2; continue; }
                if (next == 't')  { sb.append('\t'); i += 2; continue; }
            }
            if (c == '"') break;
            sb.append(c); i++;
        }
        return sb.toString().trim();
    }

    private ResultatIA parseResultatIA(String texte, Capteur capteur,
                                       MeteoService.DonneesMeteo meteo) {
        try {
            int debut = texte.indexOf('{');
            int fin   = texte.lastIndexOf('}');
            if (debut == -1 || fin == -1 || fin <= debut)
                return analyseLocaleEnrichie(capteur, meteo);
            String jsonBloc = texte.substring(debut, fin + 1);
            int    score   = (int) extraireDouble(jsonBloc, "\"score\":");
            String niveau  = extraireString(jsonBloc, "\"niveau\":\"");
            String analyse = extraireString(jsonBloc, "\"analyse\":\"");
            String act1    = extraireString(jsonBloc, "\"action1\":\"");
            String act2    = extraireString(jsonBloc, "\"action2\":\"");
            String act3    = extraireString(jsonBloc, "\"action3\":\"");
            String delai   = extraireString(jsonBloc, "\"delai\":\"");
            niveau = normaliserNiveau(niveau, score);
            if (analyse.isEmpty()) analyse = "Analyse IA disponible.";
            if (act1.isEmpty())    act1    = "Vérifier l'état du capteur";
            if (act2.isEmpty())    act2    = "Contrôler les conditions environnementales";
            if (act3.isEmpty())    act3    = "Planifier une inspection";
            if (delai.isEmpty())   delai   = score >= 70 ? "Immédiat" : score >= 50 ? "48h" : "7 jours";
            return new ResultatIA(score, niveau, analyse, act1, act2, act3, delai, true);
        } catch (Exception e) {
            return analyseLocaleEnrichie(capteur, meteo);
        }
    }

    // ── Normalisation niveau ─────────────────────────────────────────────
    private String normaliserNiveau(String niveau, int score) {
        if (niveau == null || niveau.isEmpty()) {
            // Seuils validés : FAIBLE 0-19 | MODÉRÉ 20-49 | ÉLEVÉ 50-69 | CRITIQUE 70-100
            if (score >= 70) return "CRITIQUE";
            if (score >= 50) return "ÉLEVÉ";
            if (score >= 20) return "MODÉRÉ";
            return "FAIBLE";
        }
        return niveau.toUpperCase()
                .replace("MODERE", "MODÉRÉ")
                .replace("ELEVE", "ÉLEVÉ");
    }

    // ── Analyse locale enrichie (fallback sans Ollama) ───────────────────
    /**
     * Uniquement pour capteurs ACTIFS.
     * Score (0–100) basé sur température + humidité :
     *   Temp < 40°C          →  0 pt
     *   Temp 40–55°C         →  0 à 30 pts (interpolation)
     *   Temp > 55°C          →  60 pts
     *   Humidité < 75%       →  0 pt
     *   Humidité 75–90%      →  0 à 25 pts (interpolation)
     *   Humidité > 90%       →  45 pts
     *   Pluie > 10 mm/h      →  +15 pts
     *
     * Niveaux : FAIBLE (0–19) | MODÉRÉ (20–49) | ÉLEVÉ (50–69) | CRITIQUE (70–100)
     */
    public ResultatIA analyseLocaleEnrichie(Capteur capteur,
                                             MeteoService.DonneesMeteo meteo) {
        int    score  = Math.min(100, meteo.scoreRisquePanneCapteur());
        String niveau = normaliserNiveau("", score);

        boolean tempCrit = meteo.temperature > MeteoService.TEMP_MAX_CRITIQUE;
        boolean tempAttn = meteo.temperature > MeteoService.TEMP_MAX_NORMAL;
        boolean humCrit  = meteo.humidite    > MeteoService.HUMID_MAX_CRITIQUE;
        boolean humAttn  = meteo.humidite    > MeteoService.HUMID_MAX_NORMAL;

        String analyse, act1, act2, act3, delai;

        // CAS 1 : Temp > 55°C ET Humidité > 90% → CRITIQUE (~70–100)
        if (tempCrit && humCrit) {
            analyse = String.format(
                "Panne imminente sur le capteur %s (score %d/100). "
                + "Température %.1f°C dépasse le seuil critique de %.0f°C "
                + "ET humidité %d%% dépasse le seuil critique de %d%%. "
                + "La surchauffe combinée à la condensation va endommager les composants électroniques.",
                capteur.getNom(), score,
                meteo.temperature, MeteoService.TEMP_MAX_CRITIQUE,
                meteo.humidite, MeteoService.HUMID_MAX_CRITIQUE);
            act1  = "Intervenir immédiatement pour protéger le capteur";
            act2  = "Vérifier l'étanchéité du boîtier et refroidir le capteur";
            act3  = "Déployer un capteur de remplacement en parallèle";
            delai = "Immédiat";

        // CAS 2 : Temp > 55°C seule → ÉLEVÉ/CRITIQUE (~60–85)
        } else if (tempCrit) {
            analyse = String.format(
                "Risque de panne élevé sur le capteur %s (score %d/100). "
                + "Température %.1f°C dépasse le seuil critique de %.0f°C. "
                + "Surchauffe des composants électroniques probable — humidité %d%% dans les normes.",
                capteur.getNom(), score,
                meteo.temperature, MeteoService.TEMP_MAX_CRITIQUE,
                meteo.humidite);
            act1  = "Vérifier la ventilation autour du capteur en urgence";
            act2  = "Inspecter les composants électroniques (condensateurs, circuits)";
            act3  = "Planifier une maintenance corrective sous 24h";
            delai = "24h";

        // CAS 3 : Humidité > 90% seule → ÉLEVÉ (~45–70)
        } else if (humCrit) {
            analyse = String.format(
                "Risque de panne par condensation sur le capteur %s (score %d/100). "
                + "Humidité %d%% dépasse le seuil critique de %d%%. "
                + "Court-circuit ou corrosion des contacts probable — température %.1f°C dans les normes.",
                capteur.getNom(), score,
                meteo.humidite, MeteoService.HUMID_MAX_CRITIQUE,
                meteo.temperature);
            act1  = "Vérifier l'étanchéité du boîtier IP du capteur";
            act2  = "Contrôler les joints d'étanchéité et les connecteurs";
            act3  = "Appliquer un traitement anti-humidité sur les circuits";
            delai = "24h";

        // CAS 4 : Temp 40–55°C ET Humidité 75–90% → MODÉRÉ (~20–45)
        } else if (tempAttn && humAttn) {
            analyse = String.format(
                "Risque modéré sur le capteur %s (score %d/100). "
                + "Température %.1f°C (seuil normal %.0f°C) "
                + "ET humidité %d%% (seuil normal %d%%) sont toutes deux au-dessus des normes. "
                + "Surveillance renforcée recommandée.",
                capteur.getNom(), score,
                meteo.temperature, MeteoService.TEMP_MAX_NORMAL,
                meteo.humidite, MeteoService.HUMID_MAX_NORMAL);
            act1  = "Surveiller l'évolution des deux paramètres quotidiennement";
            act2  = "Anticiper une maintenance préventive dans les 3 jours";
            act3  = "Vérifier l'état du boîtier de protection";
            delai = "3 jours";

        // CAS 5 : Temp 40–55°C seule → MODÉRÉ (~5–30)
        } else if (tempAttn) {
            analyse = String.format(
                "Surveillance recommandée pour le capteur %s (score %d/100). "
                + "Température %.1f°C dépasse le seuil normal de %.0f°C. "
                + "Humidité %d%% normale. "
                + "Risque de dégradation progressive des composants si la chaleur persiste.",
                capteur.getNom(), score,
                meteo.temperature, MeteoService.TEMP_MAX_NORMAL,
                meteo.humidite);
            act1  = "Surveiller l'évolution de la température";
            act2  = "Vérifier la ventilation et l'exposition solaire du capteur";
            act3  = "Maintenir le cycle de maintenance préventive";
            delai = "7 jours";

        // CAS 6 : Humidité 75–90% seule → MODÉRÉ (~5–25)
        } else if (humAttn) {
            analyse = String.format(
                "Surveillance recommandée pour le capteur %s (score %d/100). "
                + "Humidité %d%% dépasse le seuil normal de %d%%. "
                + "Température %.1f°C normale. "
                + "Risque de condensation légère à surveiller.",
                capteur.getNom(), score,
                meteo.humidite, MeteoService.HUMID_MAX_NORMAL,
                meteo.temperature);
            act1  = "Surveiller l'évolution de l'humidité";
            act2  = "Contrôler l'étanchéité du boîtier lors de la prochaine inspection";
            act3  = "Maintenir le cycle de maintenance préventive";
            delai = "7 jours";

        // CAS 7 : Tout normal → FAIBLE (0–19)
        } else {
            analyse = String.format(
                "Capteur %s en bon état de fonctionnement (score %d/100). "
                + "Température %.1f°C (seuil %.0f°C) "
                + "et humidité %d%% (seuil %d%%) dans les normes. "
                + "Aucun risque de panne prévu dans les conditions actuelles.",
                capteur.getNom(), score,
                meteo.temperature, MeteoService.TEMP_MAX_NORMAL,
                meteo.humidite, MeteoService.HUMID_MAX_NORMAL);
            act1  = "Maintenir le cycle de maintenance préventive planifié";
            act2  = "Vérifier les données lors de la prochaine inspection de routine";
            act3  = "Aucune action urgente requise";
            delai = "Prochaine maintenance planifiée";
        }

        return new ResultatIA(score, niveau, analyse, act1, act2, act3, delai, false);
    }

    // ── Utilitaires ──────────────────────────────────────────────────────
    private double extraireDouble(String json, String cle) {
        int idx = json.indexOf(cle);
        if (idx == -1) return 0.0;
        int start = idx + cle.length();
        while (start < json.length() && json.charAt(start) == ' ') start++;
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
        int end = start;
        while (end < json.length()) {
            char c = json.charAt(end);
            if (c == '"' && (end == 0 || json.charAt(end - 1) != '\\')) break;
            end++;
        }
        return end > start ? json.substring(start, end)
                .replace("\\n", " ").replace("\\\"", "\"").trim() : "";
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\").replace("\"", "\\\"")
                   .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
