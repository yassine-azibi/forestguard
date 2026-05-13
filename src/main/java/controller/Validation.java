package controller;

import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;

/**
 * Utilitaire de validation et de contrôle de saisie.
 * Applique des restrictions en temps réel sur les champs TextField.
 */
public class Validation {

    // ── Styles visuels ────────────────────────────────────────────────────────
    private static final String STYLE_OK    =
        "-fx-border-color: #86efac; -fx-border-radius: 8; -fx-background-radius: 8;";
    private static final String STYLE_ERROR =
        "-fx-border-color: #ef4444; -fx-border-radius: 8; -fx-background-radius: 8;" +
        "-fx-background-color: #fff5f5;";
    private static final String STYLE_NORMAL =
        "-fx-border-color: #e2e8f0; -fx-border-radius: 8; -fx-background-radius: 8;";

    // ════════════════════════════════════════════
    //  RESTRICTIONS EN TEMPS RÉEL
    // ════════════════════════════════════════════

    /** Accepte uniquement des lettres, espaces, tirets et apostrophes (noms). */
    public static void lettresSeulement(TextField tf) {
        tf.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            String filtered = newVal.replaceAll("[^a-zA-ZÀ-ÿ\\s'\\-]", "");
            if (!filtered.equals(newVal)) tf.setText(filtered);
        });
    }

    /** Accepte lettres, chiffres, espaces, virgules, points, tirets (adresses). */
    public static void adresse(TextField tf) {
        tf.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            String filtered = newVal.replaceAll("[^a-zA-ZÀ-ÿ0-9\\s,.'\\-/]", "");
            if (!filtered.equals(newVal)) tf.setText(filtered);
        });
    }

    /** Accepte uniquement des chiffres entiers positifs. */
    public static void entierPositif(TextField tf) {
        tf.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            String filtered = newVal.replaceAll("[^0-9]", "");
            if (!filtered.equals(newVal)) tf.setText(filtered);
        });
    }

    /** Accepte uniquement des nombres décimaux positifs (virgule ou point). */
    public static void decimalPositif(TextField tf) {
        tf.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            // Remplacer virgule par point
            String normalized = newVal.replace(",", ".");
            // Garder uniquement chiffres et un seul point
            String filtered = normalized.replaceAll("[^0-9.]", "");
            // Éviter plusieurs points
            int firstDot = filtered.indexOf('.');
            if (firstDot >= 0) {
                filtered = filtered.substring(0, firstDot + 1)
                         + filtered.substring(firstDot + 1).replace(".", "");
            }
            if (!filtered.equals(newVal)) tf.setText(filtered);
        });
    }

    /** Accepte lettres et chiffres uniquement (nom scientifique). */
    public static void alphanumerique(TextField tf) {
        tf.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            String filtered = newVal.replaceAll("[^a-zA-ZÀ-ÿ0-9\\s.\\-]", "");
            if (!filtered.equals(newVal)) tf.setText(filtered);
        });
    }

    // ════════════════════════════════════════════
    //  VALIDATION À LA SOUMISSION
    // ════════════════════════════════════════════

    /** Valide qu'un champ n'est pas vide. Colore en rouge si invalide. */
    public static boolean requis(TextField tf, String nomChamp) {
        if (tf.getText() == null || tf.getText().isBlank()) {
            marquerErreur(tf);
            return false;
        }
        marquerOk(tf);
        return true;
    }

    /** Valide qu'un champ contient un entier positif. */
    public static boolean estEntierPositif(TextField tf) {
        try {
            int val = Integer.parseInt(tf.getText().trim());
            if (val <= 0) { marquerErreur(tf); return false; }
            marquerOk(tf);
            return true;
        } catch (NumberFormatException e) {
            marquerErreur(tf);
            return false;
        }
    }

    /** Valide qu'un champ contient un décimal positif. */
    public static boolean estDecimalPositif(TextField tf) {
        try {
            double val = Double.parseDouble(tf.getText().replace(",", ".").trim());
            if (val <= 0) { marquerErreur(tf); return false; }
            marquerOk(tf);
            return true;
        } catch (NumberFormatException e) {
            marquerErreur(tf);
            return false;
        }
    }

    /** Valide que le texte contient uniquement des lettres (pas de chiffres). */
    public static boolean estTexte(TextField tf) {
        if (tf.getText() == null || tf.getText().isBlank()) {
            marquerErreur(tf); return false;
        }
        if (tf.getText().matches(".*\\d.*")) {
            marquerErreur(tf); return false;
        }
        marquerOk(tf);
        return true;
    }

    /** Valide une longueur minimale. */
    public static boolean longueurMin(TextField tf, int min) {
        if (tf.getText() == null || tf.getText().trim().length() < min) {
            marquerErreur(tf); return false;
        }
        marquerOk(tf);
        return true;
    }

    // ── Styles ────────────────────────────────────────────────────────────────

    public static void marquerErreur(TextField tf) {
        tf.setStyle(STYLE_ERROR);
    }

    public static void marquerOk(TextField tf) {
        tf.setStyle(STYLE_OK);
    }

    public static void reinitialiser(TextField tf) {
        tf.setStyle(STYLE_NORMAL);
    }

    /** Réinitialise le style quand l'utilisateur commence à taper. */
    public static void reinitialiserAuFocus(TextField... champs) {
        for (TextField tf : champs) {
            tf.focusedProperty().addListener((obs, o, focused) -> {
                if (focused) reinitialiser(tf);
            });
        }
    }

    // ════════════════════════════════════════════
    //  MESSAGE D'ERREUR STYLISÉ
    // ════════════════════════════════════════════

    /**
     * Construit un message d'erreur lisible depuis une liste de champs invalides.
     */
    public static String messageErreurs(String... erreurs) {
        StringBuilder sb = new StringBuilder();
        for (String e : erreurs) {
            if (e != null && !e.isBlank()) sb.append("• ").append(e).append("\n");
        }
        return sb.toString().trim();
    }

    /**
     * Affiche une alerte d'erreur stylisée.
     */
    public static void afficherErreur(String titre, String message) {
        javafx.scene.control.Alert alert =
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        // Style de l'alerte
        alert.getDialogPane().setStyle(
            "-fx-background-color: white; -fx-font-size: 13;"
        );
        alert.showAndWait();
    }
}

