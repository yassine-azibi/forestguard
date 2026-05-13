package service;

import model.Alerte;
import utils.MyConnection;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class EmailService {

    private static final String API_KEY    = "0769b304948b1c5993ec713a85904ffa";
    private static final String API_SECRET = "VOTRE_SECRET_MAILJET";
    private static final String FROM_EMAIL = "forestguard@alert.tn";
    private static final String FROM_NAME  = "ForestGuard Alertes";

    // ── Lire les emails depuis la table POMPIER ───────────────────────────────
    private List<String[]> getDestinataires() {
        List<String[]> dest = new ArrayList<>();
        Connection cnx = MyConnection.getInstance().getCnx();
        if (cnx != null) {
            try (PreparedStatement ps = cnx.prepareStatement(
                    "SELECT email, CONCAT(prenom,' ',nom) AS nom_complet " +
                    "FROM pompier " +
                    "WHERE email IS NOT NULL AND email != '' " +
                    "LIMIT 10");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String email = rs.getString("email").trim();
                    String nom   = rs.getString("nom_complet").trim();
                    dest.add(new String[]{email, nom});
                }
            } catch (Exception e) {
                System.out.println("EmailService BDD: " + e.getMessage());
            }
        }
        // Fallback si BDD vide
        if (dest.isEmpty()) {
            dest.add(new String[]{"ybrinis6@gmail.com",    "Admin ForestGuard"});
            dest.add(new String[]{"mboukhriss34@gmail.com","Chef Surveillance"});
        }
        return dest;
    }

    public void envoyerATous(Alerte a) {
        List<String[]> destinataires = getDestinataires();
        int ok = 0, erreur = 0;
        System.out.println("Email : envoi a " + destinataires.size() + " pompier(s)...");
        for (String[] dest : destinataires) {
            if (envoyerEmail(dest[0], dest[1], a)) {
                System.out.println("   Email envoye -> " + dest[1] + " <" + dest[0] + ">");
                ok++;
            } else {
                System.out.println("   Echec email -> " + dest[0]);
                erreur++;
            }
        }
        System.out.println("Email : " + ok + " envoyes, " + erreur + " echoues.");
    }

    private boolean envoyerEmail(String email, String nom, Alerte a) {
        try {
            String credentials = API_KEY + ":" + API_SECRET;
            String basicAuth = Base64.getEncoder()
                    .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

            String niveauColor = switch (a.getNiveau()) {
                case "Critique" -> "#C62828";
                case "Haute"    -> "#E65100";
                default         -> "#1565C0";
            };

            String niveauEmoji = switch (a.getNiveau()) {
                case "Critique" -> "🔴";
                case "Haute"    -> "🟠";
                default         -> "🔵";
            };

            String typeEmoji = switch (a.getTypeAlerte()) {
                case "Incendie"          -> "🔥";
                case "Fumee", "Fumée"    -> "💨";
                case "Chaleur excessive" -> "🌡";
                default                  -> "🌿";
            };

            String localPropre = a.getLocalisation()
                    .replaceAll("\\[.*\\]", "").trim();

            String corps =
                "<div style='font-family:Arial,sans-serif;max-width:600px;margin:0 auto;" +
                "background:#0d1f14;color:white;border-radius:12px;overflow:hidden;" +
                "border:1px solid #2d5a37;'>" +

                // Header
                "<div style='background:" + niveauColor + ";padding:24px;text-align:center;'>" +
                "<div style='font-size:48px;margin-bottom:8px;'>" + niveauEmoji + "</div>" +
                "<h1 style='margin:0;color:white;font-size:22px;font-weight:bold;'>" +
                "ALERTE FORESTGUARD</h1>" +
                "<p style='margin:6px 0 0;color:rgba(255,255,255,0.85);font-size:14px;'>" +
                "Niveau : <b>" + a.getNiveau().toUpperCase() + "</b></p>" +
                "</div>" +

                // Salutation
                "<div style='padding:24px 24px 0;'>" +
                "<p style='color:#94a3b8;font-size:13px;margin:0 0 16px;'>" +
                "Bonjour <b style='color:white;'>" + nom + "</b>,</p>" +
                "<p style='color:#cbd5e1;font-size:13px;margin:0 0 20px;'>" +
                "Une alerte a ete detectee sur le systeme ForestGuard. " +
                "Voici les details :</p>" +
                "</div>" +

                // Details
                "<div style='margin:0 24px;background:#1a2e1e;border-radius:10px;" +
                "border:1px solid #2d5a37;padding:16px;'>" +

                "<div style='display:flex;align-items:center;margin-bottom:12px;" +
                "padding-bottom:12px;border-bottom:1px solid #2d5a37;'>" +
                "<span style='font-size:24px;margin-right:12px;'>" + typeEmoji + "</span>" +
                "<div><div style='color:#64748b;font-size:11px;text-transform:uppercase;" +
                "letter-spacing:1px;'>Type d alerte</div>" +
                "<div style='color:white;font-size:16px;font-weight:bold;'>" +
                a.getTypeAlerte() + "</div></div></div>" +

                "<div style='display:grid;grid-template-columns:1fr 1fr;gap:12px;'>" +

                "<div style='background:#0d1f14;border-radius:8px;padding:12px;'>" +
                "<div style='color:#64748b;font-size:11px;text-transform:uppercase;" +
                "letter-spacing:1px;margin-bottom:4px;'>Zone</div>" +
                "<div style='color:#4ade80;font-size:14px;font-weight:bold;'>" +
                "📍 " + localPropre + "</div></div>" +

                "<div style='background:#0d1f14;border-radius:8px;padding:12px;'>" +
                "<div style='color:#64748b;font-size:11px;text-transform:uppercase;" +
                "letter-spacing:1px;margin-bottom:4px;'>Date</div>" +
                "<div style='color:white;font-size:14px;'>🕐 " +
                a.getDateFormatted() + "</div></div>" +

                "<div style='background:#0d1f14;border-radius:8px;padding:12px;'>" +
                "<div style='color:#64748b;font-size:11px;text-transform:uppercase;" +
                "letter-spacing:1px;margin-bottom:4px;'>Statut</div>" +
                "<div style='color:#90CAF9;font-size:14px;'>" +
                a.getStatut() + "</div></div>" +

                "<div style='background:#0d1f14;border-radius:8px;padding:12px;'>" +
                "<div style='color:#64748b;font-size:11px;text-transform:uppercase;" +
                "letter-spacing:1px;margin-bottom:4px;'>Source</div>" +
                "<div style='color:white;font-size:14px;'>" +
                a.getSource() + "</div></div>" +
                "</div></div>" +

                // CTA
                "<div style='padding:24px;text-align:center;'>" +
                "<p style='color:#cbd5e1;font-size:13px;margin:0 0 16px;'>" +
                "Connectez-vous a l application ForestGuard pour valider ou rejeter cette alerte.</p>" +
                "<div style='display:inline-block;background:" + niveauColor + ";" +
                "color:white;padding:12px 28px;border-radius:8px;" +
                "font-weight:bold;font-size:14px;'>ForestGuard — Intervenir maintenant</div>" +
                "</div>" +

                // Footer
                "<div style='padding:16px 24px;border-top:1px solid #1a3a20;" +
                "text-align:center;'>" +
                "<p style='color:#334155;font-size:11px;margin:0;'>" +
                "ForestGuard — Systeme de surveillance forestiere | Tunisie</p>" +
                "</div></div>";

            String json = "{"
                + "\"Messages\":[{"
                + "\"From\":{\"Email\":\"" + escJson(FROM_EMAIL) + "\","
                + "\"Name\":\"" + escJson(FROM_NAME) + "\"},"
                + "\"To\":[{\"Email\":\"" + escJson(email) + "\","
                + "\"Name\":\"" + escJson(nom) + "\"}],"
                + "\"Subject\":\"" + niveauEmoji + " Alerte ForestGuard — "
                + escJson(a.getNiveau()) + " : " + escJson(a.getTypeAlerte())
                + " — " + escJson(localPropre) + "\","
                + "\"HTMLPart\":\"" + escJson(corps) + "\""
                + "}]}";

            URL url = new URL("https://api.mailjet.com/v3.1/send");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Authorization", "Basic " + basicAuth);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code != 200) {
                try (var is = conn.getErrorStream()) {
                    if (is != null) {
                        String err = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                        System.out.println("Mailjet erreur HTTP " + code + " : " + err);
                    }
                }
            }
            return code == 200;
        } catch (Exception e) {
            System.out.println("Erreur email : " + e.getMessage());
            return false;
        }
    }

    private String escJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}
