package service;

import model.Alerte;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * ANALYSE INTELLIGENTE D'ALERTE — Claude API (Anthropic)
 * ═══════════════════════════════════════════════════════
 * Colle ta cle API Claude ici :
 * https://console.anthropic.com → API Keys → Create Key
 */
public class AnalyseAlerteIA {

    private static final String API_KEY = "COLLE_TA_CLE_ICI";
    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String MODEL   = "claude-haiku-4-5-20251001"; // rapide + moins cher

    // ── Résultat de l'analyse ─────────────────────────────────────────────────
    public static class AnalyseResultat {
        public String meteo          = "";
        public String nbPompiers     = "";
        public String materiel       = "";
        public String risquePropagation = "";
        public String planIntervention  = "";
        public String zonesVoisines  = "";
        public String tempsIntervention = "";
        public boolean succes        = false;
        public String erreur         = "";
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  ANALYSER UNE ALERTE
    // ══════════════════════════════════════════════════════════════════════════
    public AnalyseResultat analyser(Alerte alerte) {
        AnalyseResultat res = new AnalyseResultat();

        String localisation = alerte.getLocalisation()
                .replaceAll("\\[.*\\]", "").trim();

        String prompt =
            "Tu es un expert en gestion des incendies forestiers en Tunisie. " +
            "Une alerte vient d'etre creee avec ces informations :\n" +
            "- Type : " + alerte.getTypeAlerte() + "\n" +
            "- Niveau : " + alerte.getNiveau() + "\n" +
            "- Zone : " + localisation + "\n" +
            "- Date : " + alerte.getDateFormatted() + "\n" +
            "- Source : " + alerte.getSource() + "\n\n" +
            "Reponds UNIQUEMENT en JSON valide sans texte avant ou apres :\n" +
            "{\n" +
            "  \"meteo\": \"conditions meteorologiques typiques de cette zone et saison\",\n" +
            "  \"nb_pompiers\": \"nombre exact recommande avec justification\",\n" +
            "  \"materiel\": \"liste du materiel necessaire\",\n" +
            "  \"risque_propagation\": \"evaluation du risque de propagation\",\n" +
            "  \"plan_intervention\": \"plan etape par etape en 4 points\",\n" +
            "  \"zones_voisines\": \"zones forestieres voisines a surveiller\",\n" +
            "  \"temps_intervention\": \"temps recommande pour intervenir\"\n" +
            "}";

        try {
            String body = "{"
                + "\"model\":\"" + MODEL + "\","
                + "\"max_tokens\":1000,"
                + "\"messages\":[{"
                + "\"role\":\"user\","
                + "\"content\":\"" + escJson(prompt) + "\""
                + "}]}";

            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("x-api-key", API_KEY);
            conn.setRequestProperty("anthropic-version", "2023-06-01");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(20000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code == 200) {
                String reponse;
                try (InputStream is = conn.getInputStream()) {
                    reponse = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
                String texte = extraireTexte(reponse);
                res = parserResultat(texte);
                res.succes = true;
                System.out.println("AnalyseIA OK pour alerte " + alerte.getId());
            } else {
                try (InputStream es = conn.getErrorStream()) {
                    if (es != null) {
                        String err = new String(es.readAllBytes(), StandardCharsets.UTF_8);
                        System.out.println("AnalyseIA erreur " + code + ": " + err);
                        res.erreur = "Erreur API " + code;
                    }
                }
                // Fallback local si API indisponible
                res = genererAnalyseLocale(alerte);
                res.succes = true;
            }
        } catch (Exception e) {
            System.out.println("AnalyseIA exception: " + e.getMessage());
            // Fallback local
            res = genererAnalyseLocale(alerte);
            res.succes = true;
        }
        return res;
    }

    // ── Fallback local (marche SANS API) ─────────────────────────────────────
    private AnalyseResultat genererAnalyseLocale(Alerte alerte) {
        AnalyseResultat r = new AnalyseResultat();
        boolean critique = "Critique".equals(alerte.getNiveau());
        boolean haute    = "Haute".equals(alerte.getNiveau());
        boolean incendie = "Incendie".equals(alerte.getTypeAlerte());

        r.meteo = critique
            ? "Temperatures elevees > 35C, humidite < 20%, vent fort 40-60 km/h"
            : haute
            ? "Temperatures 28-35C, humidite 20-40%, vent modere 20-40 km/h"
            : "Conditions moderees, surveillance recommandee";

        r.nbPompiers = critique ? "12 pompiers minimum (3 equipes de 4)"
                     : haute    ? "6 pompiers (2 equipes de 3)"
                     :            "3 pompiers (1 equipe)";

        r.materiel = critique
            ? "4 camions-citernes, 2 helicopteres, 3 ambulances, groupe electrogene, drones"
            : haute
            ? "2 camions-citernes, 1 helicoptere, 2 ambulances, equipements de protection"
            : "1 camion-citerne, 1 ambulance, equipements de base";

        r.risquePropagation = critique
            ? "CRITIQUE — Propagation rapide probable dans les 30 minutes"
            : haute
            ? "ELEVE — Risque de propagation si non controle rapidement"
            : "MODERE — Surveillance active suffisante";

        r.planIntervention =
            "1. Etablir perimetre de securite 500m\n" +
            "2. Positionner vehicules face au vent\n" +
            "3. Evacuer habitants dans rayon " + (critique ? "2km" : "500m") + "\n" +
            "4. Coordonner avec Protection Civile et autorités locales";

        r.zonesVoisines = "Surveiller forets voisines dans rayon 5km, alerter agents locaux";

        r.tempsIntervention = critique ? "Intervention IMMEDIATE — moins de 10 minutes"
                             : haute    ? "Intervention rapide — moins de 20 minutes"
                             :            "Intervention dans l'heure";
        return r;
    }

    // ── Parser JSON ───────────────────────────────────────────────────────────
    private AnalyseResultat parserResultat(String texte) {
        AnalyseResultat r = new AnalyseResultat();
        texte = texte.trim()
            .replaceAll("(?s)```json\\s*", "")
            .replaceAll("(?s)```\\s*", "").trim();

        r.meteo              = extraireChamp(texte, "meteo",              "");
        r.nbPompiers         = extraireChamp(texte, "nb_pompiers",        "");
        r.materiel           = extraireChamp(texte, "materiel",           "");
        r.risquePropagation  = extraireChamp(texte, "risque_propagation", "");
        r.planIntervention   = extraireChamp(texte, "plan_intervention",  "");
        r.zonesVoisines      = extraireChamp(texte, "zones_voisines",     "");
        r.tempsIntervention  = extraireChamp(texte, "temps_intervention", "");

        // Si champs vides -> fallback
        if (r.meteo.isEmpty()) return genererAnalyseLocale(null);
        return r;
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
            }
        } catch (Exception e) {}
        return defaut;
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
                    if (next == '"') { sb.append('"'); idx += 2; }
                    else if (next == 'n') { sb.append('\n'); idx += 2; }
                    else if (next == '\\') { sb.append('\\'); idx += 2; }
                    else { sb.append(c); idx++; }
                } else if (c == '"') break;
                else { sb.append(c); idx++; }
            }
            return sb.toString().trim();
        } catch (Exception e) { return reponse; }
    }

    private String escJson(String s) {
        if (s == null) return "";
        return s.replace("\\","\\\\").replace("\"","\\\"")
                .replace("\n","\\n").replace("\r","");
    }
}
