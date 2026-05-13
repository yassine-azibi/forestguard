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

public class SmsService {

    private static final String ACCOUNT_SID = "AC746ebd987555164fa5eda204d2165785aaa";
    private static final String AUTH_TOKEN   = "5b7d5def8c50925e622164b1a95d2198aaaa";
    private static final String FROM_NUMBER  = "+16196326477aaa";

    private static final String TWILIO_URL =
        "https://api.twilio.com/2010-04-01/Accounts/" + ACCOUNT_SID + "/Messages.json";

    // ── Lire les telephones depuis la table UTILISATEUR ───────────────────────
    private List<String[]> getAdmins() {
        List<String[]> admins = new ArrayList<>();
        Connection cnx = MyConnection.getInstance().getCnx();
        if (cnx != null) {
            try (PreparedStatement ps = cnx.prepareStatement(
                    "SELECT telephone, nom FROM utilisateur " +
                    "WHERE telephone IS NOT NULL AND telephone != '' " +
                    "LIMIT 10");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String tel = rs.getString("telephone").trim();
                    String nom = rs.getString("nom").trim();
                    if (!tel.startsWith("+")) tel = "+" + tel;
                    admins.add(new String[]{tel, nom});
                }
            } catch (Exception e) {
                System.out.println("SmsService BDD: " + e.getMessage());
            }
        }
        // Fallback si BDD vide
        if (admins.isEmpty()) {
            admins.add(new String[]{"+21624597114", "Admin 1"});
            admins.add(new String[]{"+21653900510", "Admin 2"});
        }
        return admins;
    }

    public void envoyerSmsATous(Alerte a) {
        List<String[]> admins = getAdmins();
        String message = construireMessage(a);
        int ok = 0, erreur = 0;
        System.out.println("SMS : envoi a " + admins.size() + " utilisateur(s)...");
        for (String[] admin : admins) {
            if (envoyerSms(admin[0], message)) {
                System.out.println("   SMS envoye -> " + admin[1] + " (" + admin[0] + ")");
                ok++;
            } else {
                System.out.println("   Echec SMS -> " + admin[0]);
                erreur++;
            }
        }
        System.out.println("SMS : " + ok + " envoyes, " + erreur + " echoues sur " + admins.size());
    }

    private boolean envoyerSms(String numero, String message) {
        try {
            String credentials = ACCOUNT_SID + ":" + AUTH_TOKEN;
            String basicAuth = Base64.getEncoder()
                    .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

            String body = "To=" + encode(numero)
                    + "&From=" + encode(FROM_NUMBER)
                    + "&Body=" + encode(message);

            URL url = new URL(TWILIO_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Authorization", "Basic " + basicAuth);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code == 201) return true;
            try (var is = conn.getErrorStream()) {
                if (is != null) {
                    String err = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    System.out.println("Twilio erreur HTTP " + code + " : " + err);
                }
            }
            return false;
        } catch (Exception e) {
            System.out.println("Erreur SMS : " + e.getMessage());
            return false;
        }
    }

    // ── Message SMS bien structuré (max 160 caractères) ───────────────────────
    private String construireMessage(Alerte a) {
        String localPropre = a.getLocalisation()
                .replaceAll("\\[.*\\]", "").trim();

        String emoji = switch (a.getNiveau()) {
            case "Critique" -> "CRITIQUE";
            case "Haute"    -> "HAUTE";
            default         -> "MOYENNE";
        };

        return "FORESTGUARD ALERTE " + emoji + "\n"
             + "Type: " + a.getTypeAlerte() + "\n"
             + "Zone: " + truncate(localPropre, 35) + "\n"
             + "Date: " + a.getDateFormatted() + "\n"
             + "Ouvrez ForestGuard pour agir.";
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }

    private String encode(String s) throws Exception {
        return java.net.URLEncoder.encode(s, "UTF-8");
    }
}
