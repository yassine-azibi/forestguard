package com.forestguard.utils;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service d'envoi d'emails via SMTP Gmail.
 *
 * <h2>Configuration</h2>
 * Lit {@code email.properties} depuis le classpath. Requiert un App Password
 * Gmail (pas le mot de passe du compte) — à générer sur
 * <a href="https://myaccount.google.com/apppasswords">myaccount.google.com/apppasswords</a>.
 *
 * <h2>Utilisation</h2>
 * <pre>{@code
 * // Email texte brut asynchrone
 * EmailService.getInstance().sendEmailAsync(to, subject, body);
 *
 * // Email HTML asynchrone
 * EmailService.getInstance().sendEmailHtmlAsync(to, subject, htmlBody);
 *
 * // Email de bienvenue après inscription
 * EmailService.getInstance().sendWelcomeEmailAsync("Jean Dupont", "jean@example.com");
 * }</pre>
 *
 * <h2>Thread safety</h2>
 * Singleton initialisé à la première utilisation. L'ExecutorService est partagé
 * et limité à 2 threads pour ne pas saturer la connexion SMTP.
 */
public final class EmailService {

    // ── Logging ──────────────────────────────────────────────────────────────

    private static final Logger LOG = Logger.getLogger(EmailService.class.getName());

    // ── Singleton ────────────────────────────────────────────────────────────

    private static final String PROPERTIES_FILE = "email.properties";
    private static final EmailService INSTANCE = new EmailService();

    /** Pool de 2 threads pour les envois asynchrones. */
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2);

    // ── Champs de configuration ───────────────────────────────────────────────

    private final String username;
    private final String password;
    private final String fromAddress;
    private final String fromName;
    private final List<String> defaultAlertRecipients;
    private final Session session;

    // ── Constructeur privé ────────────────────────────────────────────────────

    private EmailService() {
        Properties loaded = loadProperties();

        this.username    = requiredProperty(loaded, "mail.smtp.username");
        this.password    = requiredProperty(loaded, "mail.smtp.password");
        this.fromAddress = loaded.getProperty("mail.from.address", username);
        this.fromName    = loaded.getProperty("mail.from.name", "ForestGuard");
        this.defaultAlertRecipients = parseRecipients(
                loaded.getProperty("mail.alert.recipients", ""));

        // ── SMTP Gmail SSL direct (port 465) ──────────────────────────────────
        // Port 465 + SSLSocketFactory = connexion chiffrée dès le départ.
        // Alternative : port 587 + STARTTLS (mail.smtp.starttls.enable=true).
        Properties smtp = new Properties();
        smtp.put("mail.smtp.host",                 requiredProperty(loaded, "mail.smtp.host"));
        smtp.put("mail.smtp.port",                 requiredProperty(loaded, "mail.smtp.port"));
        smtp.put("mail.smtp.auth",                 loaded.getProperty("mail.smtp.auth", "true"));
        smtp.put("mail.smtp.ssl.enable",           "true");
        smtp.put("mail.smtp.ssl.protocols",        "TLSv1.2");
        smtp.put("mail.smtp.socketFactory.port",   requiredProperty(loaded, "mail.smtp.port"));
        smtp.put("mail.smtp.socketFactory.class",  "javax.net.ssl.SSLSocketFactory");
        smtp.put("mail.smtp.socketFactory.fallback","false");
        smtp.put("mail.smtp.connectiontimeout",    loaded.getProperty("mail.smtp.connectiontimeout", "10000"));
        smtp.put("mail.smtp.timeout",              loaded.getProperty("mail.smtp.timeout",           "10000"));
        smtp.put("mail.smtp.writetimeout",         loaded.getProperty("mail.smtp.writetimeout",      "10000"));
        smtp.put("mail.smtp.ssl.trust",            loaded.getProperty("mail.smtp.ssl.trust",
                                                   requiredProperty(loaded, "mail.smtp.host")));

        this.session = Session.getInstance(smtp, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }

    // ── Accès singleton ───────────────────────────────────────────────────────

    public static EmailService getInstance() {
        return INSTANCE;
    }

    // ── API publique ──────────────────────────────────────────────────────────

    /**
     * Envoie un email HTML d'alerte incendie avec photo embarquée en Base64.
     *
     * <p>Si {@code photoPath} est non-null et que le fichier existe sur disque,
     * l'image est encodée en Base64 et injectée directement dans le HTML via
     * une balise {@code <img src="data:image/...;base64,..."/>}.
     * Cette approche fonctionne dans tous les clients email (Gmail, Outlook, etc.)
     * sans être bloquée, contrairement aux pièces jointes CID.</p>
     *
     * <p>Le {@code htmlBody} doit contenir le marqueur {@code {PHOTO_PLACEHOLDER}}
     * à l'endroit où l'image doit être insérée. Si le marqueur est absent,
     * l'image est ajoutée juste avant la balise {@code </body>}.</p>
     *
     * <p>Si pas de photo ou fichier introuvable → envoie le HTML sans modification.</p>
     *
     * @param to        adresse de destination
     * @param subject   sujet
     * @param htmlBody  corps HTML avec {@code {PHOTO_PLACEHOLDER}} optionnel
     * @param photoPath chemin absolu vers la photo (peut être null ou vide)
     */
    public void sendAlertEmailWithPhotoAsync(String to, String subject,
                                              String htmlBody, String photoPath) {
        EXECUTOR.submit(() -> {
            if (to == null || to.isBlank() || !isValidEmail(to)) {
                LOG.warning(() -> logLine("SKIP", to != null ? to : "<null>", "Adresse invalide"));
                return;
            }

            try {
                // ── Envoyer comme email HTML simple (text/html) ───────────────
                // L'HTML contient déjà la photo intégrée en Base64 (générée par buildPhotoBlock)
                MimeMessage msg = new MimeMessage(session);
                msg.setFrom(new InternetAddress(fromAddress, fromName,
                        StandardCharsets.UTF_8.name()));
                msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to.trim()));
                msg.setSubject(
                        Objects.requireNonNullElse(subject, "ForestGuard Alerte"),
                        StandardCharsets.UTF_8.name());
                msg.setContent(htmlBody, "text/html; charset=UTF-8");

                Transport.send(msg);

                boolean hasPhoto = photoPath != null && !photoPath.isBlank()
                        && new java.io.File(photoPath).exists();
                LOG.info(() -> logLine("SENT", to,
                        "Alerte avec photo=" + hasPhoto + " | Sujet : " + subject));

            } catch (MessagingException | java.io.IOException e) {
                LOG.log(Level.SEVERE, logLine("FAIL", to, e.getMessage()), e);
            }
        });
    }


    /**
     * Envoie un email de bienvenue Google avec le mot de passe généré.
     * Version asynchrone — ne bloque pas l'UI JavaFX.
     *
     * @param userName          nom complet de l'utilisateur
     * @param userEmail         adresse email de destination
     * @param generatedPassword mot de passe en clair généré (affiché dans une carte jaune)
     */
    public void sendWelcomeGoogleEmailAsync(String userName, String userEmail, String generatedPassword) {
        EXECUTOR.submit(() -> {
            try {
                if (!isValidEmail(userEmail)) {
                    LOG.warning(() -> logLine("SKIP", userEmail, "Adresse email invalide"));
                    return;
                }
                String subject = "Bienvenue sur ForestGuard 🌿 — Vos identifiants de connexion";
                String html    = buildWelcomeGoogleHtml(userName, generatedPassword);
                sendEmailInternal(userEmail, subject, html, true);
            } catch (Exception e) {
                LOG.log(Level.SEVERE, logLine("ERROR", userEmail, e.getMessage()), e);
            }
        });
    }

    // ── Template HTML bienvenue Google (avec carte mot de passe) ─────────────

    /**
     * Génère le corps HTML de l'email de bienvenue pour un compte créé via Google.
     *
     * <p>Inclut une carte jaune bien visible avec le mot de passe généré.</p>
     *
     * @param userName          nom de l'utilisateur
     * @param generatedPassword mot de passe en clair à afficher
     * @return chaîne HTML complète
     */
    private String buildWelcomeGoogleHtml(String userName, String generatedPassword) {
        String safeName = escapeHtml(userName == null || userName.isBlank() ? "Utilisateur" : userName);
        String safePwd  = escapeHtml(generatedPassword == null ? "" : generatedPassword);
        String year     = String.valueOf(LocalDateTime.now().getYear());

        return "<!DOCTYPE html>"
             + "<html lang='fr'>"
             + "<head><meta charset='UTF-8'>"
             + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
             + "<title>Bienvenue sur ForestGuard</title></head>"
             + "<body style='margin:0;padding:0;background-color:#f0fdf4;"
             +              "font-family:Arial,Helvetica,sans-serif;'>"

             // ── Conteneur principal ──────────────────────────────────────────
             + "<table width='100%' cellpadding='0' cellspacing='0' border='0'"
             +        " style='background-color:#f0fdf4;padding:32px 16px;'>"
             + "<tr><td align='center'>"
             + "<table width='600' cellpadding='0' cellspacing='0' border='0'"
             +        " style='max-width:600px;background-color:#ffffff;"
             +               "border-radius:16px;overflow:hidden;"
             +               "box-shadow:0 4px 24px rgba(0,0,0,0.08);'>"

             // ── En-tête vert ─────────────────────────────────────────────────
             + "<tr><td align='center'"
             +        " style='background:linear-gradient(135deg,#16a34a,#15803d);padding:36px 32px;'>"
             + "<p style='margin:0;font-size:36px;'>🌿</p>"
             + "<h1 style='margin:12px 0 4px;color:#ffffff;font-size:26px;"
             +            "font-weight:800;letter-spacing:-0.5px;'>ForestGuard</h1>"
             + "<p style='margin:0;color:rgba(255,255,255,0.82);font-size:13px;"
             +           "letter-spacing:1px;text-transform:uppercase;'>"
             +    "Surveillance intelligente des forêts</p>"
             + "</td></tr>"

             // ── Corps ────────────────────────────────────────────────────────
             + "<tr><td style='padding:36px 40px 28px;'>"
             + "<h2 style='margin:0 0 8px;color:#14532d;font-size:22px;font-weight:700;'>"
             +    "Bienvenue, " + safeName + "&nbsp;! 👋</h2>"
             + "<p style='margin:0 0 20px;color:#6b7280;font-size:14px;'>"
             +    "Votre compte ForestGuard a été créé avec succès via Google.</p>"

             // ── Carte jaune mot de passe ─────────────────────────────────────
             + "<table width='100%' cellpadding='0' cellspacing='0' border='0'"
             +        " style='background-color:#fefce8;border:2px solid #eab308;"
             +               "border-radius:12px;margin-bottom:28px;'>"
             + "<tr><td style='padding:22px 26px;'>"
             + "<p style='margin:0 0 6px;color:#854d0e;font-size:12px;"
             +           "font-weight:800;text-transform:uppercase;letter-spacing:1px;'>"
             +    "🔑 Votre mot de passe temporaire</p>"
             + "<p style='margin:0 0 14px;color:#713f12;font-size:13px;line-height:1.5;'>"
             +    "Un mot de passe a été généré automatiquement pour votre compte. "
             +    "Vous pouvez l'utiliser pour vous connecter sans Google, "
             +    "ou le modifier depuis votre profil.</p>"
             // Mot de passe en monospace dans un bloc bien visible
             + "<div style='background:#ffffff;border:1px solid #eab308;border-radius:8px;"
             +             "padding:14px 20px;text-align:center;'>"
             + "<span style='font-family:\"Courier New\",Courier,monospace;"
             +              "font-size:22px;font-weight:900;letter-spacing:4px;"
             +              "color:#1c1917;'>" + safePwd + "</span>"
             + "</div>"
             + "<p style='margin:12px 0 0;color:#92400e;font-size:12px;'>"
             +    "⚠️ Conservez ce mot de passe en lieu sûr. "
             +    "Changez-le dès votre première connexion.</p>"
             + "</td></tr></table>"

             // ── Message principal ────────────────────────────────────────────
             + "<p style='margin:0 0 16px;color:#374151;font-size:15px;line-height:1.7;'>"
             +    "Bonjour <strong>" + safeName + "</strong>,</p>"
             + "<p style='margin:0 0 16px;color:#374151;font-size:15px;line-height:1.7;'>"
             +    "Votre compte <strong>ForestGuard</strong> a été créé avec succès. "
             +    "Merci de contribuer à la protection de nos forêts.</p>"

             // ── Signature ────────────────────────────────────────────────────
             + "<p style='margin:0;color:#374151;font-size:14px;line-height:1.6;'>"
             +    "Cordialement,<br>"
             +    "<strong style='color:#15803d;'>L'équipe ForestGuard</strong></p>"
             + "</td></tr>"

             // ── Pied de page ─────────────────────────────────────────────────
             + "<tr><td align='center'"
             +        " style='background-color:#f9fafb;border-top:1px solid #e5e7eb;padding:20px 32px;'>"
             + "<p style='margin:0;color:#9ca3af;font-size:12px;line-height:1.6;'>"
             +    "Cet email a été envoyé automatiquement suite à la création de votre compte.<br>"
             +    "© " + year + " ForestGuard — Tous droits réservés.</p>"
             + "</td></tr>"

             + "</table>"
             + "</td></tr></table>"
             + "</body></html>";
    }

    /**
     * Envoie un email de bienvenue HTML après la création d'un compte classique.
     * Méthode synchrone — préférer {@link #sendWelcomeEmailAsync} depuis le thread JavaFX.
     *
     * @param userName  nom complet de l'utilisateur
     * @param userEmail adresse email de destination
     */
    public void sendWelcomeEmail(String userName, String userEmail) {
        if (!isValidEmail(userEmail)) {
            LOG.warning(() -> logLine("SKIP", userEmail, "Adresse email invalide"));
            return;
        }

        String subject = "Bienvenue sur ForestGuard 🌿";
        String html    = buildWelcomeHtml(userName);
        sendEmailInternal(userEmail, subject, html, true);
    }

    /**
     * Version asynchrone de {@link #sendWelcomeEmail} — ne bloque pas l'UI.
     *
     * @param userName  nom complet de l'utilisateur
     * @param userEmail adresse email de destination
     */
    public void sendWelcomeEmailAsync(String userName, String userEmail) {
        EXECUTOR.submit(() -> {
            try {
                sendWelcomeEmail(userName, userEmail);
            } catch (Exception e) {
                LOG.log(Level.SEVERE, logLine("ERROR", userEmail, e.getMessage()), e);
            }
        });
    }

    /**
     * Envoie un email en texte brut (synchrone).
     *
     * @param to      adresse de destination
     * @param subject sujet
     * @param body    corps en texte brut
     */
    public void sendEmail(String to, String subject, String body) {
        sendEmailInternal(to, subject, body, false);
    }

    /**
     * Envoie un email HTML (synchrone).
     *
     * @param to          adresse de destination
     * @param subject     sujet
     * @param htmlMessage corps HTML
     */
    public void sendEmailHtml(String to, String subject, String htmlMessage) {
        sendEmailInternal(to, subject, htmlMessage, true);
    }

    /**
     * Envoie un email en texte brut de façon asynchrone.
     * Les erreurs sont loguées mais ne remontent pas à l'appelant.
     */
    public void sendEmailAsync(String to, String subject, String body) {
        EXECUTOR.submit(() -> {
            try {
                sendEmail(to, subject, body);
            } catch (Exception e) {
                LOG.log(Level.SEVERE, logLine("ERROR", to, e.getMessage()), e);
            }
        });
    }

    /**
     * Envoie un email HTML de façon asynchrone.
     * Les erreurs sont loguées mais ne remontent pas à l'appelant.
     */
    public void sendEmailHtmlAsync(String to, String subject, String htmlMessage) {
        EXECUTOR.submit(() -> {
            try {
                sendEmailHtml(to, subject, htmlMessage);
            } catch (Exception e) {
                LOG.log(Level.SEVERE, logLine("ERROR", to, e.getMessage()), e);
            }
        });
    }

    /**
     * Envoie un email HTML à tous les destinataires d'alerte configurés
     * dans {@code mail.alert.recipients}.
     */
    public void notifyDefaultAlertRecipientsAsync(String subject, String htmlMessage) {
        if (defaultAlertRecipients.isEmpty()) {
            LOG.warning("Aucun destinataire d'alerte configuré (mail.alert.recipients). Email ignoré.");
            return;
        }
        for (String recipient : defaultAlertRecipients) {
            sendEmailHtmlAsync(recipient, subject, htmlMessage);
        }
    }

    // ── Implémentation interne ────────────────────────────────────────────────

    /**
     * Construit et envoie le message MIME.
     *
     * @param to      adresse de destination (validée avant appel)
     * @param subject sujet du message
     * @param content corps (texte brut ou HTML selon {@code html})
     * @param html    {@code true} pour Content-Type text/html, {@code false} pour text/plain
     */
    private void sendEmailInternal(String to, String subject, String content, boolean html) {
        if (to == null || to.isBlank()) {
            LOG.warning(() -> logLine("SKIP", "<vide>", "Adresse de destination vide"));
            return;
        }

        if (!isValidEmail(to)) {
            LOG.warning(() -> logLine("SKIP", to, "Format d'adresse email invalide"));
            return;
        }

        try {
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(fromAddress, fromName, StandardCharsets.UTF_8.name()));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to.trim()));
            msg.setSubject(
                    Objects.requireNonNullElse(subject, "ForestGuard Notification"),
                    StandardCharsets.UTF_8.name()
            );

            if (html) {
                msg.setContent(
                        Objects.requireNonNullElse(content, ""),
                        "text/html; charset=UTF-8"
                );
            } else {
                msg.setText(
                        Objects.requireNonNullElse(content, ""),
                        StandardCharsets.UTF_8.name()
                );
            }

            Transport.send(msg);
            LOG.info(() -> logLine("SENT", to, "Sujet : " + subject));

        } catch (MessagingException | IOException e) {
            LOG.log(Level.SEVERE, logLine("FAIL", to, e.getMessage()), e);
        }
    }

    // ── Template HTML de bienvenue ────────────────────────────────────────────

    /**
     * Génère le corps HTML de l'email de bienvenue.
     *
     * <p>Design minimaliste compatible avec les principaux clients email
     * (Gmail, Outlook, Apple Mail) : table-based layout, styles inline.</p>
     *
     * @param userName nom de l'utilisateur à personnaliser
     * @return chaîne HTML complète
     */
    private String buildWelcomeHtml(String userName) {
        String safeName = escapeHtml(userName == null || userName.isBlank() ? "Utilisateur" : userName);
        String year     = String.valueOf(LocalDateTime.now().getYear());

        return "<!DOCTYPE html>"
             + "<html lang='fr'>"
             + "<head><meta charset='UTF-8'>"
             + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
             + "<title>Bienvenue sur ForestGuard</title></head>"
             + "<body style='margin:0;padding:0;background-color:#f0fdf4;font-family:Arial,Helvetica,sans-serif;'>"

             // ── Conteneur principal ──────────────────────────────────────────
             + "<table width='100%' cellpadding='0' cellspacing='0' border='0'"
             +        " style='background-color:#f0fdf4;padding:32px 16px;'>"
             + "<tr><td align='center'>"
             + "<table width='600' cellpadding='0' cellspacing='0' border='0'"
             +        " style='max-width:600px;background-color:#ffffff;"
             +               "border-radius:16px;overflow:hidden;"
             +               "box-shadow:0 4px 24px rgba(0,0,0,0.08);'>"

             // ── En-tête vert ─────────────────────────────────────────────────
             + "<tr>"
             + "<td align='center'"
             +     " style='background:linear-gradient(135deg,#16a34a,#15803d);"
             +             "padding:36px 32px;'>"
             + "<p style='margin:0;font-size:36px;'>🌿</p>"
             + "<h1 style='margin:12px 0 4px;color:#ffffff;font-size:26px;"
             +            "font-weight:800;letter-spacing:-0.5px;'>ForestGuard</h1>"
             + "<p style='margin:0;color:rgba(255,255,255,0.82);font-size:13px;"
             +           "letter-spacing:1px;text-transform:uppercase;'>"
             +    "Surveillance intelligente des forêts</p>"
             + "</td></tr>"

             // ── Corps du message ─────────────────────────────────────────────
             + "<tr>"
             + "<td style='padding:40px 40px 32px;'>"

             + "<h2 style='margin:0 0 8px;color:#14532d;font-size:22px;font-weight:700;'>"
             +    "Bienvenue, " + safeName + "&nbsp;! 👋</h2>"
             + "<p style='margin:0 0 24px;color:#6b7280;font-size:14px;'>"
             +    "Votre compte a été créé avec succès.</p>"

             // ── Carte de confirmation ────────────────────────────────────────
             + "<table width='100%' cellpadding='0' cellspacing='0' border='0'"
             +        " style='background-color:#f0fdf4;border-radius:12px;"
             +               "border:1px solid #bbf7d0;margin-bottom:28px;'>"
             + "<tr><td style='padding:20px 24px;'>"
             + "<p style='margin:0 0 6px;color:#15803d;font-size:13px;"
             +           "font-weight:700;text-transform:uppercase;letter-spacing:0.5px;'>"
             +    "✅ Compte activé</p>"
             + "<p style='margin:0;color:#374151;font-size:14px;line-height:1.6;'>"
             +    "Vous pouvez dès maintenant vous connecter à ForestGuard et contribuer "
             +    "à la protection de nos forêts en signalant des incendies ou en "
             +    "consultant les alertes actives.</p>"
             + "</td></tr></table>"

             // ── Message principal ────────────────────────────────────────────
             + "<p style='margin:0 0 16px;color:#374151;font-size:15px;line-height:1.7;'>"
             +    "Bonjour <strong>" + safeName + "</strong>,</p>"
             + "<p style='margin:0 0 16px;color:#374151;font-size:15px;line-height:1.7;'>"
             +    "Votre compte <strong>ForestGuard</strong> a été créé avec succès. "
             +    "Merci de contribuer à la protection de nos forêts.</p>"
             + "<p style='margin:0 0 32px;color:#374151;font-size:15px;line-height:1.7;'>"
             +    "En cas de problème ou de question, n'hésitez pas à nous contacter "
             +    "en répondant directement à cet email.</p>"

             // ── Signature ────────────────────────────────────────────────────
             + "<p style='margin:0;color:#374151;font-size:14px;line-height:1.6;'>"
             +    "Cordialement,<br>"
             +    "<strong style='color:#15803d;'>L'équipe ForestGuard</strong></p>"
             + "</td></tr>"

             // ── Pied de page ─────────────────────────────────────────────────
             + "<tr>"
             + "<td align='center'"
             +     " style='background-color:#f9fafb;border-top:1px solid #e5e7eb;"
             +             "padding:20px 32px;'>"
             + "<p style='margin:0;color:#9ca3af;font-size:12px;line-height:1.6;'>"
             +    "Cet email a été envoyé automatiquement suite à la création de votre compte.<br>"
             +    "© " + year + " ForestGuard — Tous droits réservés.</p>"
             + "</td></tr>"

             + "</table>"  // fin table intérieure
             + "</td></tr></table>"  // fin table extérieure
             + "</body></html>";
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    /**
     * Valide le format d'une adresse email via {@link InternetAddress}.
     *
     * @param email adresse à valider
     * @return {@code true} si le format est valide
     */
    private static boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) return false;
        try {
            new InternetAddress(email.trim(), true).validate();
            return true;
        } catch (AddressException e) {
            return false;
        }
    }

    /**
     * Échappe les caractères HTML spéciaux pour éviter les injections dans le template.
     */
    private static String escapeHtml(String value) {
        if (value == null) return "";
        return value
                .replace("&",  "&amp;")
                .replace("<",  "&lt;")
                .replace(">",  "&gt;")
                .replace("\"", "&quot;")
                .replace("'",  "&#39;");
    }

    /**
     * Formate une ligne de log structurée.
     *
     * <p>Format : {@code [HH:mm:ss] [STATUS] to=<email> — message}</p>
     */
    private static String logLine(String status, String to, String message) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        return String.format("[%s] [EMAIL-%s] to=%s — %s", time, status, to, message);
    }

    /**
     * Charge {@code email.properties} depuis le classpath.
     *
     * @throws IllegalStateException si le fichier est absent ou illisible
     */
    private static Properties loadProperties() {
        try (InputStream in = EmailService.class.getClassLoader()
                                                .getResourceAsStream(PROPERTIES_FILE)) {
            if (in == null) {
                throw new IllegalStateException(
                        "Fichier manquant : " + PROPERTIES_FILE
                        + " (doit être dans src/main/resources/)");
            }
            Properties props = new Properties();
            props.load(in);
            return props;
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de charger " + PROPERTIES_FILE, e);
        }
    }

    /**
     * Lit une propriété obligatoire et lève une exception claire si elle est absente.
     */
    private static String requiredProperty(Properties props, String key) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Propriété obligatoire manquante dans " + PROPERTIES_FILE + " : " + key);
        }
        return value.trim();
    }

    /**
     * Parse une liste de destinataires séparés par des virgules.
     */
    private static List<String> parseRecipients(String raw) {
        if (raw == null || raw.isBlank()) return Collections.emptyList();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }
}
