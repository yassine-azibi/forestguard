package service;

import model.Alerte;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * SERVICE RAPPORT IA — Génère un rapport complet après création d'une alerte
 * Utilise Gemini + données météo pour fournir :
 * - Conditions météo actuelles
 * - Nombre de pompiers recommandés
 * - Matériel nécessaire
 * - Conseils d'intervention
 */
public class RapportIAService {

    private static final String API_KEY = "VOTRE_CLE_GEMINI";
    private static final String[] MODELES = {
        "gemini-2.0-flash-exp",
        "gemini-2.0-flash",
        "gemini-1.5-flash",
        "gemini-2.5-flash"
    };

    // ── Résultat du rapport ───────────────────────────────────────────────────
    public static class RapportIA {
        public String meteo          = "";
        public String vent           = "";
        public String humidite       = "";
        public String temperature    = "";
        public int    nbPompiers     = 0;
        public String materiel       = "";
        public String conseils       = "";
        public String niveauRisque   = "";
        public String resumeComplet  = "";
        public boolean succes        = false;
        public String erreur         = "";
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  GÉNÉRER LE RAPPORT COMPLET
    // ══════════════════════════════════════════════════════════════════════════

    public RapportIA genererRapport(Alerte alerte) {
        // Essayer d'abord avec Gemini
        RapportIA rapport = appelGemini(alerte);
        if (!rapport.succes) {
            // Fallback : rapport basé sur les règles métier
            rapport = genererRapportLocal(alerte);
        }
        return rapport;
    }

    private RapportIA appelGemini(Alerte alerte) {
        RapportIA rapport = new RapportIA();
        String localisation = alerte.getLocalisation()
                .replaceAll("\\[.*\\]", "").trim();

        String prompt = "Tu es un expert en gestion des incendies forestiers en Tunisie. "
            + "Une alerte vient d'être créée avec ces informations :\n"
            + "- Type : " + alerte.getTypeAlerte() + "\n"
            + "- Niveau : " + alerte.getNiveau() + "\n"
            + "- Zone : " + localisation + "\n"
            + "- Date : " + alerte.getDateFormatted() + "\n\n"
            + "Génère un rapport d'intervention COMPLET en JSON avec exactement cette structure :\n"
            + "{\n"
            + "  \"temperature\": \"temperature estimee en Celsius avec contexte\",\n"
            + "  \"vent\": \"vitesse et direction du vent estimees\",\n"
            + "  \"humidite\": \"taux d humidite estime en %\",\n"
            + "  \"meteo\": \"resume des conditions meteorologiques\",\n"
            + "  \"nb_pompiers\": nombre entier de pompiers recommandes,\n"
            + "  \"materiel\": \"liste du materiel necessaire separee par virgules\",\n"
            + "  \"conseils\": \"3 conseils d intervention numerotes\",\n"
            + "  \"niveau_risque\": \"evaluation du risque de propagation\",\n"
            + "  \"resume\": \"resume executif de 2-3 phrases pour le commandant\"\n"
            + "}\n"
            + "Reponds UNIQUEMENT en JSON valide, sans texte avant ou apres.";

        for (String modele : MODELES) {
            try {
                String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/"
                        + modele + ":generateContent?key=" + API_KEY;

                String json = "{\"contents\":[{\"parts\":[{\"text\":\""
                        + escJson(prompt) + "\"}]}]}";

                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }

                int code = conn.getResponseCode();
                if (code == 200) {
                    String reponse;
                    try (InputStream is = conn.getInputStream()) {
                        reponse = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    }
                    String texte = extraireTexte(reponse);
                    rapport = parserRapport(texte);
                    rapport.succes = true;
                    System.out.println("Rapport IA genere avec modele: " + modele);
                    return rapport;
                } else if (!String.valueOf(code).equals("404")
                        && !String.valueOf(code).equals("429")) {
                    break;
                }
            } catch (Exception e) {
                System.out.println("Gemini " + modele + " erreur: " + e.getMessage());
            }
        }
        rapport.erreur = "API Gemini indisponible";
        return rapport;
    }

    // ── Rapport local basé sur les règles métier (sans API) ───────────────────
    private RapportIA genererRapportLocal(Alerte alerte) {
        RapportIA r = new RapportIA();
        r.succes = true;

        boolean critique = "Critique".equals(alerte.getNiveau());
        boolean haute    = "Haute".equals(alerte.getNiveau());

        r.temperature  = critique ? "Estimée > 35°C (conditions propices aux incendies)"
                       : haute    ? "Estimée 28-35°C (chaleur élevée)"
                       :            "Estimée 20-28°C (conditions modérées)";

        r.vent         = critique ? "Vent fort estimé 40-60 km/h — risque de propagation rapide"
                       : haute    ? "Vent modéré 20-40 km/h — surveiller la direction"
                       :            "Vent faible < 20 km/h";

        r.humidite     = critique ? "< 20% — Très sèche, danger maximal"
                       : haute    ? "20-40% — Sèche, favorable aux incendies"
                       :            "40-60% — Modérée";

        r.meteo        = "Conditions " + (critique ? "extrêmement dangereuses"
                       : haute ? "dangereuses" : "défavorables") + " pour les incendies.";

        r.nbPompiers   = critique ? 12 : haute ? 6 : 3;

        r.materiel     = critique
            ? "4 camions-citernes, 2 hélicoptères bombardiers d'eau, 3 ambulances, groupe électrogène, drones de surveillance"
            : haute
            ? "2 camions-citernes, 1 hélicoptère, 2 ambulances, équipements de protection"
            : "1 camion-citerne, 1 ambulance, équipements de base";

        r.conseils     = "1. Établir un périmètre de sécurité immédiat\n"
                       + "2. Positionner les véhicules face au vent\n"
                       + "3. Coordonner avec la Protection Civile et les autorités locales";

        r.niveauRisque = critique ? "CRITIQUE — Propagation rapide probable"
                       : haute    ? "ÉLEVÉ — Intervention urgente requise"
                       :            "MODÉRÉ — Surveillance renforcée";

        r.resumeComplet = "Alerte " + alerte.getNiveau() + " de type " + alerte.getTypeAlerte()
                        + " détectée à " + alerte.getLocalisation().replaceAll("\\[.*\\]","").trim()
                        + ". Déploiement de " + r.nbPompiers + " pompiers recommandé. "
                        + r.niveauRisque + ".";
        return r;
    }

    // ── Parser le JSON Gemini ─────────────────────────────────────────────────
    private RapportIA parserRapport(String texte) {
        RapportIA r = new RapportIA();
        texte = texte.trim()
            .replaceAll("(?s)```json\\s*", "")
            .replaceAll("(?s)```\\s*", "").trim();

        r.temperature   = extraireChamp(texte, "temperature",  "Non disponible");
        r.vent          = extraireChamp(texte, "vent",         "Non disponible");
        r.humidite      = extraireChamp(texte, "humidite",     "Non disponible");
        r.meteo         = extraireChamp(texte, "meteo",        "Non disponible");
        r.materiel      = extraireChamp(texte, "materiel",     "Non disponible");
        r.conseils      = extraireChamp(texte, "conseils",     "Non disponible");
        r.niveauRisque  = extraireChamp(texte, "niveau_risque","Non disponible");
        r.resumeComplet = extraireChamp(texte, "resume",       "Non disponible");

        String nb = extraireChamp(texte, "nb_pompiers", "3");
        try { r.nbPompiers = Integer.parseInt(nb.replaceAll("[^0-9]","").trim()); }
        catch (Exception e) { r.nbPompiers = 3; }
        return r;
    }

    private String extraireTexte(String reponse) {
        try {
            int idx = reponse.indexOf("\"text\":");
            if (idx == -1) return reponse;
            idx = reponse.indexOf("\"", idx + 7) + 1;
            StringBuilder sb = new StringBuilder();
            while (idx < reponse.length()) {
                char c = reponse.charAt(idx);
                if (c == '\\' && idx + 1 < reponse.length()) {
                    char next = reponse.charAt(idx + 1);
                    if      (next == '"')  { sb.append('"');  idx += 2; }
                    else if (next == 'n')  { sb.append('\n'); idx += 2; }
                    else if (next == '\\') { sb.append('\\'); idx += 2; }
                    else                   { sb.append(c);    idx++; }
                } else if (c == '"') break;
                else { sb.append(c); idx++; }
            }
            return sb.toString().trim();
        } catch (Exception e) { return reponse; }
    }

    private String extraireChamp(String json, String cle, String defaut) {
        try {
            int idx = json.indexOf("\"" + cle + "\"");
            if (idx == -1) return defaut;
            idx = json.indexOf(":", idx) + 1;
            while (idx < json.length() && json.charAt(idx) == ' ') idx++;
            if (json.charAt(idx) == '"') {
                idx++;
                StringBuilder sb = new StringBuilder();
                while (idx < json.length() && json.charAt(idx) != '"') {
                    if (json.charAt(idx) == '\\' && idx + 1 < json.length()) {
                        char next = json.charAt(idx + 1);
                        if (next == 'n') sb.append('\n');
                        else sb.append(next);
                        idx += 2;
                    } else sb.append(json.charAt(idx++));
                }
                return sb.toString().trim();
            } else {
                StringBuilder sb = new StringBuilder();
                while (idx < json.length() && Character.isDigit(json.charAt(idx)))
                    sb.append(json.charAt(idx++));
                return sb.toString().trim();
            }
        } catch (Exception e) { return defaut; }
    }

    private String escJson(String s) {
        if (s == null) return "";
        return s.replace("\\","\\\\").replace("\"","\\\"")
                .replace("\n","\\n").replace("\r","");
    }
}
