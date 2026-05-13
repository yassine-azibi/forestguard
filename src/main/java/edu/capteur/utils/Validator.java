package edu.capteur.utils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe utilitaire de validation centralisée pour ForestGuard.
 * Toutes les règles métier de saisie sont définies ici,
 * utilisables depuis les controllers ET les services.
 */
public class Validator {

    // ─────────────────────────────────────────────
    //  TYPES ET STATUTS AUTORISÉS
    // ─────────────────────────────────────────────

    public static final List<String> TYPES_CAPTEUR  = List.of("temperature", "fumee", "humidite");
    public static final List<String> STATUTS_CAPTEUR = List.of("actif", "inactif", "en_panne");

    public static final List<String> TYPES_MAINTENANCE   = List.of("preventive", "corrective", "remplacement");
    public static final List<String> STATUTS_MAINTENANCE = List.of("planifiee", "en_cours", "terminee");

    // ─────────────────────────────────────────────
    //  RÉSULTAT DE VALIDATION
    // ─────────────────────────────────────────────

    public static class ResultatValidation {
        private final List<String> erreurs = new ArrayList<>();

        public void ajouterErreur(String message) {
            erreurs.add(message);
        }

        public boolean estValide() {
            return erreurs.isEmpty();
        }

        public List<String> getErreurs() {
            return erreurs;
        }

        /** Première erreur, utile pour un affichage simple. */
        public String premiereErreur() {
            return erreurs.isEmpty() ? "" : erreurs.get(0);
        }
    }

    // ─────────────────────────────────────────────
    //  VALIDATION CAPTEUR
    // ─────────────────────────────────────────────

    /**
     * Valide les champs d'un capteur.
     *
     * @param nom          Nom du capteur
     * @param localisation Localisation du capteur
     * @param type         Type (temperature / fumee / humidite)
     * @param statut       Statut (actif / inactif / en_panne)
     * @return ResultatValidation contenant les erreurs éventuelles
     */
    public static ResultatValidation validerCapteur(String nom, String localisation,
                                                     String type, String statut) {
        ResultatValidation resultat = new ResultatValidation();

        // --- Nom ---
        String nomTrim = nom != null ? nom.trim() : "";
        if (nomTrim.isEmpty()) {
            resultat.ajouterErreur("nom|⚠ Le nom est obligatoire.");
        } else if (nomTrim.length() < 2) {
            resultat.ajouterErreur("nom|⚠ Le nom doit contenir au moins 2 caractères.");
        } else if (nomTrim.length() > 50) {
            resultat.ajouterErreur("nom|⚠ Le nom ne peut pas dépasser 50 caractères.");
        } else if (!nomTrim.matches("[a-zA-ZÀ-ÿ0-9 _\\-]+")) {
            resultat.ajouterErreur("nom|⚠ Caractères spéciaux non autorisés dans le nom.");
        }

        // --- Localisation ---
        String locTrim = localisation != null ? localisation.trim() : "";
        if (locTrim.isEmpty()) {
            resultat.ajouterErreur("localisation|⚠ La localisation est obligatoire.");
        } else if (locTrim.length() < 2) {
            resultat.ajouterErreur("localisation|⚠ Localisation trop courte (min 2 caractères).");
        } else if (locTrim.length() > 100) {
            resultat.ajouterErreur("localisation|⚠ Localisation trop longue (max 100 caractères).");
        }

        // --- Type ---
        if (type == null || type.isEmpty()) {
            resultat.ajouterErreur("type|⚠ Veuillez sélectionner un type.");
        } else if (!TYPES_CAPTEUR.contains(type)) {
            resultat.ajouterErreur("type|⚠ Type invalide : " + type);
        }

        // --- Statut ---
        if (statut == null || statut.isEmpty()) {
            resultat.ajouterErreur("statut|⚠ Veuillez sélectionner un statut.");
        } else if (!STATUTS_CAPTEUR.contains(statut)) {
            resultat.ajouterErreur("statut|⚠ Statut invalide : " + statut);
        }

        return resultat;
    }

    // ─────────────────────────────────────────────
    //  VALIDATION MAINTENANCE
    // ─────────────────────────────────────────────

    /**
     * Valide les champs d'une maintenance.
     *
     * @param date        Date choisie (peut être null)
     * @param type        Type de maintenance
     * @param statut      Statut de la maintenance
     * @param description Description libre (optionnelle)
     * @param capteurId   ID du capteur associé (-1 si non sélectionné)
     * @param validerCapteur true pour valider la présence du capteur (ajout), false sinon
     * @return ResultatValidation contenant les erreurs éventuelles
     */
    public static ResultatValidation validerMaintenance(LocalDate date, String type,
                                                         String statut, String description,
                                                         int capteurId, boolean validerCapteur) {
        ResultatValidation resultat = new ResultatValidation();

        // --- Date ---
        if (date == null) {
            resultat.ajouterErreur("date|⚠ La date est obligatoire.");
        } else if (date.isBefore(LocalDate.now().minusYears(10))) {
            resultat.ajouterErreur("date|⚠ La date est trop ancienne (max 10 ans en arrière).");
        } else if (date.isAfter(LocalDate.now().plusYears(5))) {
            resultat.ajouterErreur("date|⚠ La date ne peut pas dépasser 5 ans dans le futur.");
        }

        // --- Type ---
        if (type == null || type.isEmpty()) {
            resultat.ajouterErreur("type|⚠ Veuillez sélectionner un type.");
        } else if (!TYPES_MAINTENANCE.contains(type)) {
            resultat.ajouterErreur("type|⚠ Type de maintenance invalide : " + type);
        }

        // --- Statut ---
        if (statut == null || statut.isEmpty()) {
            resultat.ajouterErreur("statut|⚠ Veuillez sélectionner un statut.");
        } else if (!STATUTS_MAINTENANCE.contains(statut)) {
            resultat.ajouterErreur("statut|⚠ Statut invalide : " + statut);
        }

        // --- Description (optionnelle mais contrôlée) ---
        String desc = description != null ? description.trim() : "";
        if (!desc.isEmpty()) {
            if (desc.length() < 5) {
                resultat.ajouterErreur("description|⚠ Description trop courte (min 5 caractères si renseignée).");
            } else if (desc.length() > 255) {
                resultat.ajouterErreur("description|⚠ Description trop longue (max 255 caractères).");
            }
        }

        // --- Capteur associé (uniquement à l'ajout) ---
        if (validerCapteur && capteurId <= 0) {
            resultat.ajouterErreur("capteur|⚠ Veuillez sélectionner un capteur.");
        }

        return resultat;
    }

    // ─────────────────────────────────────────────
    //  UTILITAIRES
    // ─────────────────────────────────────────────

    /**
     * Extrait le champ cible depuis une erreur formatée "champ|message".
     * Ex : "nom|⚠ Le nom est obligatoire." → "nom"
     */
    public static String extraireChamp(String erreur) {
        int sep = erreur.indexOf('|');
        return sep >= 0 ? erreur.substring(0, sep) : "";
    }

    /**
     * Extrait le message depuis une erreur formatée "champ|message".
     * Ex : "nom|⚠ Le nom est obligatoire." → "⚠ Le nom est obligatoire."
     */
    public static String extraireMessage(String erreur) {
        int sep = erreur.indexOf('|');
        return sep >= 0 ? erreur.substring(sep + 1) : erreur;
    }
}
