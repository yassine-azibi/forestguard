package service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * ASSISTANT VOCAL ForestGuard
 *
 * Utilise Gemini pour interpreter la commande vocale transcrite
 * et retourner une action structurée JSON.
 *
 * Exemples vocaux supportés :
 * FR : "Incendie critique à Kroumirie"
 * FR : "Fumée niveau haute zone Cap Bon"
 * AR : "حريق في الغابة"
 * FR : "Valider l'alerte numéro 5"
 * FR : "Rejeter l'alerte 3"
 */
public class AssistantVocalService {

    private static final String API_KEY = "AIzaSyC5oAYiXLn1NipDoaKfiVS18bUx9tySaE0";
    private static final String[] MODELES = {
        "gemini-2.0-flash-exp", "gemini-2.0-flash",
        "gemini-1.5-flash", "gemini-2.5-flash"
    };

    // ── Résultat d'interprétation ─────────────────────────────────────────────
    public static class CommandeVocale {
        public String action       = "";  // AJOUTER_ALERTE, VALIDER, REJETER, INCONNU
        public String typeAlerte   = "";
        public String niveau       = "";
        public String localisation = "";
        public int    idAlerte     = -1;
        public String messageRetour = "";
        public boolean succes      = false;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  INTERPRÉTER UNE COMMANDE VOCALE
    // ══════════════════════════════════════════════════════════════════════════

    public CommandeVocale interpreter(String texteVocal) {
        if (texteVocal == null || texteVocal.trim().isEmpty()) {
            CommandeVocale c = new CommandeVocale();
            c.messageRetour = "Commande vide — réessayez";
            return c;
        }

        // Essayer d'abord avec Gemini
        CommandeVocale cmd = appelGemini(texteVocal);
        if (!cmd.succes) {
            // Fallback : analyse locale par mots-clés
            cmd = analyserLocal(texteVocal);
        }
        return cmd;
    }

    // ── Appel Gemini ──────────────────────────────────────────────────────────
    private CommandeVocale appelGemini(String texte) {
        CommandeVocale cmd = new CommandeVocale();

        String prompt = "Tu es l'assistant vocal de ForestGuard, systeme de surveillance forestiere en Tunisie. "
            + "Analyse cette commande vocale (en francais ou arabe) et reponds UNIQUEMENT en JSON valide :\n\n"
            + "Commande : \"" + texte + "\"\n\n"
            + "Structure JSON attendue :\n"
            + "{\n"
            + "  \"action\": \"AJOUTER_ALERTE\" ou \"VALIDER\" ou \"REJETER\" ou \"INCONNU\",\n"
            + "  \"type_alerte\": \"Incendie\" ou \"Fumee\" ou \"Chaleur excessive\" ou \"Secheresse\" ou \"\",\n"
            + "  \"niveau\": \"Critique\" ou \"Haute\" ou \"Moyenne\" ou \"\",\n"
            + "  \"localisation\": \"nom de la zone mentionnee ou vide\",\n"
            + "  \"id_alerte\": numero entier si valider/rejeter sinon -1,\n"
            + "  \"message_retour\": \"confirmation en francais de ce que tu as compris\"\n"
            + "}\n\n"
            + "Exemples :\n"
            + "- 'Incendie critique a Kroumirie' -> action AJOUTER_ALERTE, type Incendie, niveau Critique, localisation Kroumirie\n"
            + "- 'Valider alerte 5' -> action VALIDER, id_alerte 5\n"
            + "- 'Rejeter l alerte numero 3' -> action REJETER, id_alerte 3\n"
            + "- 'حريق في منطقة قرمبالية' -> action AJOUTER_ALERTE, type Incendie, localisation Grombalia\n"
            + "Reponds UNIQUEMENT en JSON, sans texte avant ou apres.";

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
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }

                int code = conn.getResponseCode();
                if (code == 200) {
                    String reponse;
                    try (InputStream is = conn.getInputStream()) {
                        reponse = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    }
                    String texteReponse = extraireTexte(reponse);
                    cmd = parserCommande(texteReponse);
                    cmd.succes = true;
                    return cmd;
                }
            } catch (Exception e) {
                System.out.println("AssistantVocal Gemini erreur: " + e.getMessage());
            }
        }
        return cmd;
    }

    // ── Analyse locale par mots-clés (fallback sans API) ──────────────────────
    private CommandeVocale analyserLocal(String texte) {
        CommandeVocale cmd = new CommandeVocale();
        cmd.succes = true;
        String t = texte.toLowerCase().trim();

        // Détecter action
        if (t.contains("valider") || t.contains("valide") || t.contains("تأكيد")) {
            cmd.action = "VALIDER";
            // Chercher un numéro
            java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\\d+").matcher(t);
            if (m.find()) cmd.idAlerte = Integer.parseInt(m.group());
            cmd.messageRetour = "Validation de l'alerte " + (cmd.idAlerte > 0 ? "#" + cmd.idAlerte : "");

        } else if (t.contains("rejeter") || t.contains("rejete") || t.contains("annuler") || t.contains("رفض")) {
            cmd.action = "REJETER";
            java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\\d+").matcher(t);
            if (m.find()) cmd.idAlerte = Integer.parseInt(m.group());
            cmd.messageRetour = "Rejet de l'alerte " + (cmd.idAlerte > 0 ? "#" + cmd.idAlerte : "");

        } else {
            cmd.action = "AJOUTER_ALERTE";

            // Détecter type
            if (t.contains("incendie") || t.contains("feu") || t.contains("حريق"))
                cmd.typeAlerte = "Incendie";
            else if (t.contains("fumee") || t.contains("fumée") || t.contains("دخان"))
                cmd.typeAlerte = "Fumee";
            else if (t.contains("chaleur") || t.contains("chaud") || t.contains("حرارة"))
                cmd.typeAlerte = "Chaleur excessive";
            else if (t.contains("secheresse") || t.contains("sécheresse") || t.contains("جفاف"))
                cmd.typeAlerte = "Secheresse";
            else
                cmd.typeAlerte = "Incendie";

            // Détecter niveau
            if (t.contains("critique") || t.contains("urgent") || t.contains("حرج"))
                cmd.niveau = "Critique";
            else if (t.contains("haute") || t.contains("haut") || t.contains("grave") || t.contains("خطير"))
                cmd.niveau = "Haute";
            else
                cmd.niveau = "Moyenne";

            // Zones tunisiennes connues
            String[] zones = {"kroumirie","tabarka","bizerte","jendouba","nabeul",
                "cap bon","tunis","beja","zaghouan","siliana","kairouan","sfax",
                "gabes","gafsa","tozeur","kebili","medenine","tataouine",
                "monastir","sousse","mahdia","kasserine"};
            for (String z : zones) {
                if (t.contains(z)) {
                    cmd.localisation = z.substring(0,1).toUpperCase() + z.substring(1);
                    break;
                }
            }

            cmd.messageRetour = cmd.typeAlerte + " " + cmd.niveau
                + (cmd.localisation.isEmpty() ? "" : " à " + cmd.localisation)
                + " — Formulaire rempli !";
        }
        return cmd;
    }

    // ── Parser JSON Gemini ────────────────────────────────────────────────────
    private CommandeVocale parserCommande(String texte) {
        CommandeVocale cmd = new CommandeVocale();
        texte = texte.trim()
            .replaceAll("(?s)```json\\s*","")
            .replaceAll("(?s)```\\s*","").trim();

        cmd.action        = extraireChamp(texte, "action",         "INCONNU");
        cmd.typeAlerte    = extraireChamp(texte, "type_alerte",    "");
        cmd.niveau        = extraireChamp(texte, "niveau",         "");
        cmd.localisation  = extraireChamp(texte, "localisation",   "");
        cmd.messageRetour = extraireChamp(texte, "message_retour", "Commande comprise");

        String nb = extraireChamp(texte, "id_alerte", "-1");
        try { cmd.idAlerte = Integer.parseInt(nb.replaceAll("[^0-9\\-]","")); }
        catch (Exception e) { cmd.idAlerte = -1; }

        return cmd;
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
                        sb.append(json.charAt(idx + 1)); idx += 2;
                    } else sb.append(json.charAt(idx++));
                }
                return sb.toString().trim();
            } else {
                StringBuilder sb = new StringBuilder();
                while (idx < json.length() &&
                    (Character.isDigit(json.charAt(idx)) || json.charAt(idx) == '-'))
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
