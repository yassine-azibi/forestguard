package com.forestguard.utils;

import com.twilio.Twilio;
import com.twilio.exception.TwilioException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class TwilioSmsService {

    private static String accountSid;
    private static String authToken;
    private static String fromNumber;
    private static boolean initialized;

    static {
        try (InputStream input = TwilioSmsService.class.getClassLoader()
                .getResourceAsStream("twilio.properties")) {
            if (input == null) {
                System.err.println("[TwilioSMS] ❌ twilio.properties introuvable dans le classpath.");
            } else {
                Properties properties = new Properties();
                properties.load(input);

                accountSid = properties.getProperty("twilio.account.sid");
                authToken  = properties.getProperty("twilio.auth.token");
                fromNumber = PhoneNumberUtils.normalizeToE164(
                        properties.getProperty("twilio.from.number"));

                if (accountSid == null || accountSid.isBlank()
                        || authToken == null || authToken.isBlank()
                        || fromNumber == null || fromNumber.isBlank()) {
                    initialized = false;
                    System.err.println("[TwilioSMS] ❌ Configuration invalide dans twilio.properties.");
                    System.err.println("[TwilioSMS]    twilio.account.sid  : "
                            + (accountSid  != null && !accountSid.isBlank()  ? "✓" : "MANQUANT"));
                    System.err.println("[TwilioSMS]    twilio.auth.token   : "
                            + (authToken   != null && !authToken.isBlank()   ? "✓" : "MANQUANT"));
                    System.err.println("[TwilioSMS]    twilio.from.number  : "
                            + (fromNumber  != null && !fromNumber.isBlank()  ? fromNumber : "MANQUANT/INVALIDE"));
                } else {
                    Twilio.init(accountSid, authToken);
                    initialized = true;
                    System.out.println("[TwilioSMS] ✅ Service initialisé — from=" + fromNumber);
                }
            }
        } catch (IOException e) {
            System.err.println("[TwilioSMS] ❌ Impossible de lire twilio.properties : " + e.getMessage());
        }
    }

    /**
     * Envoie un SMS au numéro indiqué.
     *
     * <p>Le numéro est normalisé en E.164 avant l'envoi. Si la normalisation
     * échoue (format non reconnu), le SMS est ignoré avec un log détaillé.</p>
     *
     * @param toNumber    numéro de destination (tout format accepté par
     *                    {@link PhoneNumberUtils#normalizeToE164})
     * @param messageBody corps du SMS (max 1600 caractères)
     */
    public static void sendSms(String toNumber, String messageBody) {

        // ── Diagnostic : état d'initialisation ───────────────────────────────
        System.out.println("[TwilioSMS] 📤 Tentative d'envoi SMS à : " + toNumber);

        if (!initialized) {
            System.err.println("[TwilioSMS] ❌ Service non initialisé — SMS ignoré pour : "
                    + toNumber);
            System.err.println("[TwilioSMS]    Vérifiez twilio.properties "
                    + "(account.sid, auth.token, from.number).");
            return;
        }

        // ── Normalisation du numéro ───────────────────────────────────────────
        String raw        = toNumber != null ? toNumber.trim() : "";
        String normalized = normalizeForSms(raw);

        System.out.println("[TwilioSMS]    Numéro brut      : " + raw);
        System.out.println("[TwilioSMS]    Numéro normalisé : "
                + (normalized.isBlank() ? "❌ INVALIDE" : normalized));

        if (normalized.isBlank()) {
            System.err.println("[TwilioSMS] ❌ Numéro invalide — SMS ignoré.");
            System.err.println("[TwilioSMS]    Formats acceptés : +216XXXXXXXX, 216XXXXXXXX, "
                    + "0XXXXXXXX (Tunisie), 8 chiffres locaux.");
            return;
        }

        // ── Troncature à 1600 caractères (limite Twilio) ──────────────────────
        String body = messageBody != null ? messageBody : "";
        if (body.length() > 1600) {
            body = body.substring(0, 1597) + "...";
            System.out.println("[TwilioSMS]    Corps tronqué à 1600 caractères.");
        }

        // ── Envoi ─────────────────────────────────────────────────────────────
        try {
            Message message = Message.creator(
                    new PhoneNumber(normalized),
                    new PhoneNumber(fromNumber),
                    body
            ).create();

            System.out.println("[TwilioSMS] ✅ SMS envoyé à " + normalized
                    + " (SID : " + message.getSid() + ")");

        } catch (TwilioException e) {
            System.err.println("[TwilioSMS] ❌ Échec Twilio pour " + normalized
                    + " : " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[TwilioSMS] ❌ Erreur inattendue pour " + normalized
                    + " : " + e.getMessage());
        }
    }

    /**
     * Normalise un numéro de téléphone pour l'envoi SMS.
     *
     * <p>Gère les cas supplémentaires non couverts par
     * {@link PhoneNumberUtils#normalizeToE164} :</p>
     * <ul>
     *   <li>{@code 0XXXXXXXX} (9 chiffres, préfixe 0 local) → {@code +216XXXXXXXX}</li>
     *   <li>{@code 216XXXXXXXX} (11 chiffres sans +) → {@code +216XXXXXXXX}</li>
     * </ul>
     *
     * @param raw numéro brut (peut être null ou vide)
     * @return numéro E.164 valide, ou chaîne vide si non reconnu
     */
    private static String normalizeForSms(String raw) {
        if (raw == null || raw.isBlank()) return "";

        // Essayer la normalisation standard en premier
        String standard = PhoneNumberUtils.normalizeToE164(raw);
        if (!standard.isBlank()) return standard;

        // ── Cas supplémentaires ───────────────────────────────────────────────
        // Supprimer espaces, tirets, points, parenthèses
        String digits = raw.replaceAll("[\\s\\-().]", "").replaceAll("\\D", "");

        // 0XXXXXXXX → 9 chiffres commençant par 0 → retirer le 0, ajouter +216
        if (digits.length() == 9 && digits.startsWith("0")) {
            String local = digits.substring(1); // 8 chiffres
            if (PhoneNumberUtils.isTunisianNumber(local)) {
                return "+216" + local;
            }
        }

        // 216XXXXXXXX → 11 chiffres commençant par 216 (sans +)
        if (digits.length() == 11 && digits.startsWith("216")) {
            String local = digits.substring(3);
            if (PhoneNumberUtils.isTunisianNumber(local)) {
                return "+" + digits;
            }
        }

        return "";
    }
}
