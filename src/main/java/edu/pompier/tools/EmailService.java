package edu.pompier.tools;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

public class EmailService {

    private static final String EXPEDITEUR_EMAIL = "forestg106@gmail.com";
    private static final String EXPEDITEUR_MDP   = "blxqmarzetlbqukj";

    // ═════════════════════════════════════════
    //  EMAIL DE BIENVENUE (existant)
    // ═════════════════════════════════════════

    public static void envoyerEmailBienvenue(String destinataireEmail, String nomPompier, String motDePasse) {
        Thread thread = new Thread(() -> {
            try {
                Session session = creerSession();
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(EXPEDITEUR_EMAIL, "ForestGuard"));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinataireEmail));
                message.setSubject("Bienvenue dans ForestGuard, " + nomPompier + " !");
                message.setContent(construireCorpsBienvenue(nomPompier, destinataireEmail, motDePasse), "text/html; charset=UTF-8");
                Transport.send(message);
                System.out.println("Email de bienvenue envoye a : " + destinataireEmail);
            } catch (Exception e) {
                System.err.println("Erreur email bienvenue :"); e.printStackTrace();
            }
        });
        thread.setDaemon(false);
        thread.start();
    }

    // ═════════════════════════════════════════
    //  EMAIL D'AFFECTATION (nouveau)
    // ═════════════════════════════════════════

    /**
     * Envoie un email d'urgence au pompier affecté automatiquement à un incendie.
     *
     * @param destinataireEmail  email du pompier
     * @param nomPompier         "Prenom NOM"
     * @param typeAlerte         ex: "Incendie"
     * @param niveauAlerte       ex: "ELEVE"
     * @param localisation       ex: "Mateur, Bizerte"
     * @param distanceKm         distance calculée entre le pompier et l'incendie
     * @param scoreSelection     score de l'algorithme de selection
     */
    public static void envoyerEmailAffectation(String destinataireEmail,
                                               String nomPompier,
                                               String typeAlerte,
                                               String niveauAlerte,
                                               String localisation,
                                               double distanceKm,
                                               double scoreSelection) {
        Thread thread = new Thread(() -> {
            try {
                Session session = creerSession();
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(EXPEDITEUR_EMAIL, "ForestGuard"));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinataireEmail));
                message.setSubject("ALERTE INCENDIE - Vous etes mobilise, " + nomPompier + " !");
                message.setContent(
                        construireCorpsAffectation(nomPompier, typeAlerte, niveauAlerte,
                                localisation, distanceKm, scoreSelection),
                        "text/html; charset=UTF-8"
                );
                Transport.send(message);
                System.out.println("Email d'affectation envoye a : " + destinataireEmail);
            } catch (Exception e) {
                System.err.println("Erreur email affectation :"); e.printStackTrace();
            }
        });
        thread.setDaemon(false);
        thread.start();
    }

    // ═════════════════════════════════════════
    //  SESSION SMTP (factorisee)
    // ═════════════════════════════════════════

    private static Session creerSession() {
        Properties props = new Properties();
        props.put("mail.smtp.host",            "smtp.gmail.com");
        props.put("mail.smtp.port",            "587");
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.trust",       "smtp.gmail.com");
        props.put("mail.smtp.connectiontimeout", "10000"); // 10s
        props.put("mail.smtp.timeout",           "10000"); // 10s
        props.put("mail.smtp.writetimeout",      "10000"); // 10s
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EXPEDITEUR_EMAIL, EXPEDITEUR_MDP);
            }
        });
        session.setDebug(false); // true pour voir les logs SMTP dans la console
        return session;
    }

    // ═════════════════════════════════════════
    //  CORPS EMAIL BIENVENUE
    // ═════════════════════════════════════════

    private static String construireCorpsBienvenue(String nomPompier, String email, String motDePasse) {
        return "<!DOCTYPE html><html lang='fr'><head><meta charset='UTF-8'/><style>" +
                "body{font-family:Arial,sans-serif;background:#f4f4f4;margin:0;padding:0}" +
                ".container{max-width:600px;margin:30px auto;background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,.1)}" +
                ".header{background:linear-gradient(135deg,#1B5E20,#2E7D32);padding:36px 30px;text-align:center}" +
                ".header h1{color:#fff;margin:0;font-size:28px}" +
                ".header p{color:rgba(255,255,255,.8);margin:8px 0 0;font-size:14px}" +
                ".badge{display:inline-block;background:rgba(255,255,255,.2);color:#fff;border-radius:20px;padding:4px 16px;font-size:12px;margin-top:12px}" +
                ".body{padding:36px 30px}" +
                ".greeting{font-size:20px;font-weight:bold;color:#1B5E20;margin-bottom:16px}" +
                ".text{color:#374151;font-size:15px;line-height:1.7}" +
                ".credentials{background:#F0FDF4;border-left:4px solid #16a34a;border-radius:8px;padding:20px 24px;margin:24px 0}" +
                ".credentials p{margin:6px 0;font-size:14px;color:#374151}" +
                ".credentials strong{color:#1B5E20}" +
                ".value{font-family:'Courier New',monospace;background:#dcfce7;padding:2px 8px;border-radius:4px;color:#166534;font-weight:bold}" +
                ".warning{background:#FFF7ED;border:1px solid #FED7AA;border-radius:8px;padding:14px 18px;margin:20px 0;font-size:13px;color:#92400E}" +
                ".footer{background:#F8FAF8;padding:20px 30px;text-align:center;font-size:12px;color:#9CA3AF;border-top:1px solid #E5E7EB}" +
                "</style></head><body><div class='container'>" +
                "<div class='header'><div style='font-size:48px;margin-bottom:10px'>&#127794;</div>" +
                "<h1>ForestGuard</h1><p>Systeme de Surveillance Forestiere Intelligente</p>" +
                "<span class='badge'>&#128658; Nouveau compte pompier</span></div>" +
                "<div class='body'><p class='greeting'>Bienvenue, " + nomPompier + " !</p>" +
                "<p class='text'>Votre compte pompier a ete cree avec succes sur <strong>ForestGuard</strong>.</p>" +
                "<div class='credentials'>" +
                "<p>&#128231; <strong>Email :</strong> <span class='value'>" + email + "</span></p>" +
                "<p>&#128273; <strong>Mot de passe :</strong> <span class='value'>" + motDePasse + "</span></p>" +
                "</div>" +
                "<div class='warning'>&#9888;&#65039; <strong>Securite :</strong> Ne partagez jamais votre mot de passe.</div>" +
                "</div><div class='footer'>" +
                "<p>&#169; 2026 <strong>ForestGuard</strong> — Surveillance Intelligente des Forets</p>" +
                "<p>Message automatique — Ne pas repondre</p>" +
                "</div></div></body></html>";
    }

    // ═════════════════════════════════════════
    //  CORPS EMAIL AFFECTATION
    // ═════════════════════════════════════════

    private static String construireCorpsAffectation(String nomPompier,
                                                     String typeAlerte,
                                                     String niveauAlerte,
                                                     String localisation,
                                                     double distanceKm,
                                                     double scoreSelection) {

        String couleurNiveau = switch (niveauAlerte.toUpperCase()) {
            case "CRITIQUE" -> "#7f1d1d";
            case "ELEVE"    -> "#991b1b";
            case "MOYEN"    -> "#b45309";
            default         -> "#374151";
        };
        String bgNiveau = switch (niveauAlerte.toUpperCase()) {
            case "CRITIQUE" -> "#fee2e2";
            case "ELEVE"    -> "#fecaca";
            case "MOYEN"    -> "#fef3c7";
            default         -> "#f1f5f9";
        };

        String heureActuelle = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy 'a' HH:mm:ss"));

        return "<!DOCTYPE html><html lang='fr'><head><meta charset='UTF-8'/><style>" +
                "body{font-family:Arial,sans-serif;background:#f4f4f4;margin:0;padding:0}" +
                ".container{max-width:620px;margin:30px auto;background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,.15)}" +
                ".urgence-bar{background:#dc2626;color:#fff;text-align:center;padding:10px;font-size:13px;font-weight:bold;letter-spacing:1px}" +
                ".header{background:linear-gradient(135deg,#7f1d1d,#dc2626);padding:30px;text-align:center}" +
                ".header h1{color:#fff;margin:0;font-size:26px;letter-spacing:1px}" +
                ".header p{color:rgba(255,255,255,.85);margin:6px 0 0;font-size:13px}" +
                ".alerte-badge{display:inline-block;background:rgba(255,255,255,.2);color:#fff;border-radius:20px;padding:5px 18px;font-size:13px;font-weight:bold;margin-top:10px;border:1px solid rgba(255,255,255,.4)}" +
                ".body{padding:30px}" +
                ".greeting{font-size:19px;font-weight:bold;color:#7f1d1d;margin-bottom:14px}" +
                ".text{color:#374151;font-size:14px;line-height:1.7;margin-bottom:16px}" +
                ".alerte-card{background:#fff5f5;border:2px solid #fca5a5;border-radius:10px;padding:20px 24px;margin:20px 0}" +
                ".alerte-card h3{color:#991b1b;margin:0 0 14px;font-size:16px;border-bottom:1px solid #fca5a5;padding-bottom:8px}" +
                ".alerte-row{display:flex;align-items:center;margin:8px 0;font-size:14px;color:#374151}" +
                ".alerte-row .label{min-width:140px;font-weight:bold;color:#7f1d1d}" +
                ".niveau-badge{display:inline-block;background:" + bgNiveau + ";color:" + couleurNiveau + ";border-radius:6px;padding:2px 12px;font-weight:bold;font-size:13px}" +
                ".mission-card{background:#f0fdf4;border-left:4px solid #16a34a;border-radius:8px;padding:16px 20px;margin:20px 0}" +
                ".mission-card p{margin:5px 0;font-size:14px;color:#374151}" +
                ".mission-card strong{color:#166534}" +
                ".footer{background:#1a1a1a;padding:20px 30px;text-align:center;font-size:12px;color:#9CA3AF}" +
                ".footer strong{color:#dc2626}" +
                "</style></head><body><div class='container'>" +

                // Bandeau rouge
                "<div class='urgence-bar'>&#128680; &nbsp; MOBILISATION IMMEDIATE REQUISE &nbsp; &#128680;</div>" +

                // Header
                "<div class='header'>" +
                "<div style='font-size:44px;margin-bottom:8px'>&#128293;</div>" +
                "<h1>ALERTE INCENDIE</h1>" +
                "<p>Systeme ForestGuard — Notification automatique</p>" +
                "<span class='alerte-badge'>&#128658; Vous etes affecte a cette intervention</span>" +
                "</div>" +

                // Corps
                "<div class='body'>" +
                "<p class='greeting'>Agent " + nomPompier + ",</p>" +
                "<p class='text'>Le systeme ForestGuard vous a selectionne automatiquement pour intervenir " +
                "sur un incendie detecte. Preparez-vous immediatement et rejoignez la zone d'intervention.</p>" +

                // Carte alerte
                "<div class='alerte-card'>" +
                "<h3>&#128293; Details de l'alerte</h3>" +
                "<div class='alerte-row'><span class='label'>Type :</span><span>" + typeAlerte + "</span></div>" +
                "<div class='alerte-row'><span class='label'>Niveau :</span><span class='niveau-badge'>" + niveauAlerte.toUpperCase() + "</span></div>" +
                "<div class='alerte-row'><span class='label'>Localisation :</span><span>&#128205; " + localisation + "</span></div>" +
                "<div class='alerte-row'><span class='label'>Distance :</span><span>&#128506; " + String.format("%.1f", distanceKm) + " km de votre position</span></div>" +
                "<div class='alerte-row'><span class='label'>Detecte le :</span><span>&#128336; " + heureActuelle + "</span></div>" +
                "</div>" +

                // Carte mission
                "<div class='mission-card'>" +
                "<p>&#9989; <strong>Score de selection :</strong> " + String.format("%.1f", scoreSelection) + " pts</p>" +
                "<p>&#127919; <strong>Statut :</strong> Vous etes maintenant <strong>EN MISSION</strong></p>" +
                "<p>&#128222; <strong>Action requise :</strong> Rejoignez immediatement la zone indiquee et confirmez votre intervention aupres de votre responsable.</p>" +
                "</div>" +

                "<p class='text' style='color:#dc2626;font-weight:bold;font-size:13px'>" +
                "&#9888;&#65039; Ce message a ete genere automatiquement par ForestGuard. Ne tardez pas a intervenir.</p>" +
                "</div>" +

                // Footer
                "<div class='footer'>" +
                "<p>&#169; 2026 <strong>ForestGuard</strong> — Surveillance Intelligente des Forets</p>" +
                "<p>Message automatique — Ne pas repondre a cet email</p>" +
                "</div></div></body></html>";
    }
}