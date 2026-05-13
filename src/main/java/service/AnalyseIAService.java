package service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;

/**
 * SERVICE ANALYSE IA — Google Gemini Vision API (GRATUIT)
 *
 * Pour obtenir une clé gratuite :
 * 1. Aller sur https://aistudio.google.com
 * 2. Cliquer "Get API Key" → "Create API Key"
 * 3. Copier la clé (commence par AIza...)
 * 4. Remplacer VOTRE_CLE_GEMINI ci-dessous
 */
public class AnalyseIAService {

    // ── Clé API Gemini GRATUITE ───────────────────────────────────────────────
    // Remplace par ta clé depuis aistudio.google.com
    private static final String API_KEY = "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";

    // Modeles a essayer dans l'ordre si erreur 404
    private static final String[] MODELES = {
        "gemini-2.0-flash-exp",
        "gemini-2.0-flash",
        "gemini-1.5-flash",
        "gemini-1.5-pro",
        "gemini-2.5-flash"
    };

    // ── Résultat d'analyse ────────────────────────────────────────────────────
    public static class ResultatAnalyse {
        public String  typeAlerte   = "Incendie";
        public String  niveau       = "Haute";
        public String  description  = "";
        public String  conseils     = "";
        public int     nbPompiers   = 3;
        public String  localisation = "";
        public boolean succes       = false;
        public String  erreur       = "";
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  ANALYSER UNE IMAGE avec Gemini Vision
    // ══════════════════════════════════════════════════════════════════════════

    // MODE DEMO — mettre true pour simuler l'IA sans appel API
    private static final boolean MODE_DEMO = false;

    public ResultatAnalyse analyserImage(File imageFile) {
        // Mode demo : retourne un resultat simule
        if (MODE_DEMO) return simulerAnalyse(imageFile.getName());

        ResultatAnalyse res = new ResultatAnalyse();
        // Essayer chaque modele jusqu'a en trouver un qui marche
        for (String modele : MODELES) {
            res = tenterAnalyse(imageFile, modele);
            if (res.succes) return res;
            if (!res.erreur.contains("404") && !res.erreur.contains("NOT_FOUND")) break;
            System.out.println("Modele " + modele + " non disponible, essai suivant...");
        }
        return res;
    }

    private ResultatAnalyse tenterAnalyse(File imageFile, String modele) {
        ResultatAnalyse res = new ResultatAnalyse();
        String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/"
                      + modele + ":generateContent?key=" + API_KEY;
        try {
            byte[] imageBytes  = Files.readAllBytes(imageFile.toPath());
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String mediaType   = detecterMediaType(imageFile.getName());

            System.out.println("Essai modele: " + modele);
            String prompt =
                "Tu es un expert en surveillance forestiere en Tunisie. " +
                "Analyse cette image et reponds UNIQUEMENT en JSON valide, " +
                "sans aucun texte avant ou apres le JSON, avec cette structure exacte:\n" +
                "{\n" +
                "  \"type_alerte\": \"Incendie\" ou \"Fumee\" ou \"Chaleur excessive\" ou \"Secheresse\",\n" +
                "  \"niveau\": \"Critique\" ou \"Haute\" ou \"Moyenne\",\n" +
                "  \"description\": \"description en 2-3 phrases de ce que tu vois\",\n" +
                "  \"conseils\": \"3 conseils d intervention numerotes\",\n" +
                "  \"nb_pompiers\": nombre entier recommande entre 1 et 20,\n" +
                "  \"localisation\": \"type de zone visible (foret, montagne, zone urbaine...)\"\n" +
                "}\n" +
                "Si l image ne montre pas de risque, mets niveau Moyenne et type Chaleur excessive.";

            // Corps JSON pour Gemini
            String json =
                "{\"contents\":[{\"parts\":[" +
                "{\"inline_data\":{\"mime_type\":\"" + mediaType + "\"," +
                "\"data\":\"" + base64Image + "\"}}," +
                "{\"text\":\"" + escJson(prompt) + "\"}" +
                "]}]}";

            // Appel API Gemini
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code != 200) {
                try (InputStream is = conn.getErrorStream()) {
                    if (is != null) {
                        String err = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                        res.erreur = "Erreur API " + code + ": " + err;
                        System.out.println("Gemini erreur: " + err);
                    }
                }
                return res;
            }

            // Lire la réponse
            String reponse;
            try (InputStream is = conn.getInputStream()) {
                reponse = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }

            // Extraire le texte de la réponse Gemini
            String texte = extraireTexteGemini(reponse);
            System.out.println("Gemini reponse: " + texte);

            res = parserResultat(texte);
            res.succes = true;

        } catch (Exception e) {
            res.erreur = "Erreur: " + e.getMessage();
            System.out.println("AnalyseIA erreur: " + e.getMessage());
        }
        return res;
    }

    // ── Extraire le texte de la réponse Gemini ────────────────────────────────
    private String extraireTexteGemini(String reponse) {
        try {
            // Structure Gemini: candidates[0].content.parts[0].text
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
                } else if (c == '"') {
                    break;
                } else {
                    sb.append(c);
                    idx++;
                }
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return reponse;
        }
    }

    // ── Parser le JSON retourné par Gemini ────────────────────────────────────
    private ResultatAnalyse parserResultat(String texte) {
        ResultatAnalyse res = new ResultatAnalyse();
        try {
            // Nettoyer les backticks markdown
            texte = texte.trim()
                .replaceAll("(?s)```json\\s*", "")
                .replaceAll("(?s)```\\s*", "")
                .trim();

            res.typeAlerte   = extraireChamp(texte, "type_alerte",  "Incendie");
            res.niveau       = extraireChamp(texte, "niveau",        "Haute");
            res.description  = extraireChamp(texte, "description",   "Situation detectee par l'IA.");
            res.conseils     = extraireChamp(texte, "conseils",      "1. Alerter les autorites\n2. Evacuer la zone\n3. Ne pas intervenir seul");
            res.localisation = extraireChamp(texte, "localisation",  "Zone forestiere");

            String nbStr = extraireChamp(texte, "nb_pompiers", "3");
            try {
                res.nbPompiers = Integer.parseInt(nbStr.replaceAll("[^0-9]", "").trim());
                if (res.nbPompiers < 1)  res.nbPompiers = 1;
                if (res.nbPompiers > 20) res.nbPompiers = 20;
            } catch (Exception e) { res.nbPompiers = 3; }

            // Valider type
            if (!res.typeAlerte.matches("Incendie|Fumee|Fumée|Chaleur excessive|Secheresse|Sécheresse")) {
                res.typeAlerte = "Incendie";
            }
            // Normaliser sans accents
            res.typeAlerte = res.typeAlerte
                .replace("Fumée","Fumee")
                .replace("Sécheresse","Secheresse");

            // Valider niveau
            if (!res.niveau.matches("Critique|Haute|Moyenne")) {
                res.niveau = "Haute";
            }

        } catch (Exception e) {
            System.out.println("Parser erreur: " + e.getMessage());
        }
        return res;
    }

    // ── Extraire un champ JSON ────────────────────────────────────────────────
    private String extraireChamp(String json, String cle, String defaut) {
        try {
            String pattern = "\"" + cle + "\"";
            int idx = json.indexOf(pattern);
            if (idx == -1) return defaut;
            idx = json.indexOf(":", idx) + 1;
            while (idx < json.length() && json.charAt(idx) == ' ') idx++;

            if (idx >= json.length()) return defaut;

            if (json.charAt(idx) == '"') {
                idx++;
                StringBuilder sb = new StringBuilder();
                while (idx < json.length() && json.charAt(idx) != '"') {
                    if (json.charAt(idx) == '\\' && idx + 1 < json.length()) {
                        char next = json.charAt(idx + 1);
                        if      (next == 'n')  sb.append('\n');
                        else if (next == '"')  sb.append('"');
                        else if (next == '\\') sb.append('\\');
                        else                   sb.append(next);
                        idx += 2;
                    } else {
                        sb.append(json.charAt(idx++));
                    }
                }
                return sb.toString().trim();
            } else {
                // Valeur numérique
                StringBuilder sb = new StringBuilder();
                while (idx < json.length() &&
                       (Character.isDigit(json.charAt(idx)) || json.charAt(idx) == '.')) {
                    sb.append(json.charAt(idx++));
                }
                return sb.toString().trim();
            }
        } catch (Exception e) {
            return defaut;
        }
    }

    private String detecterMediaType(String nom) {
        String lower = nom.toLowerCase();
        if (lower.endsWith(".png"))  return "image/png";
        if (lower.endsWith(".gif"))  return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }

    private String escJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
    // ── Mode demo : simule une reponse IA realiste ─────────────────────────
    private ResultatAnalyse simulerAnalyse(String nomFichier) {
        ResultatAnalyse res = new ResultatAnalyse();
        res.succes       = true;
        res.typeAlerte   = "Incendie";
        res.niveau       = "Critique";
        res.description  = "Incendie de grande ampleur detecte sur une zone forestiere. "
                         + "Flammes visibles avec fumee noire dense. "
                         + "Propagation rapide favorisee par les conditions meteorologiques.";
        res.conseils     = "1. Alerter immediatement les services de secours (198)\n"
                         + "2. Evacuer les habitants dans un rayon de 2 km\n"
                         + "3. Ne pas intervenir sans equipement specialise\n"
                         + "4. Baliser les acces pour les vehicules de secours";
        res.nbPompiers   = 8;
        res.localisation = "Zone forestiere — foret dense avec vegetation seche";
        System.out.println("[DEMO] Analyse simulee pour: " + nomFichier);
        return res;
    }

}
