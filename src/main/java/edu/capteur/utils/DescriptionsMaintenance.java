package edu.capteur.utils;

import java.util.List;

/**
 * Descriptions prédéfinies partagées entre AjoutMaintenanceController
 * et ModifierMaintenanceController.
 */
public class DescriptionsMaintenance {

    public static final List<String> PREVENTIVE = List.of(
        "Inspection capteur",
        "Nettoyage capteur",
        "Recalibrage capteur",
        "Vérification connexions",
        "Test fonctionnement",
        "Remplacement batterie",
        "Contrôle boîtier",
        "Mise à jour firmware"
    );

    public static final List<String> CORRECTIVE = List.of(
        "Réparation panne",
        "Remplacement module",
        "Correction anomalie mesure",
        "Réparation câblage",
        "Remplacement capteur défectueux",
        "Résolution problème réseau",
        "Remise en service"
    );

    public static final List<String> REMPLACEMENT = List.of(
        "Remplacement capteur",
        "Remplacement batterie",
        "Remplacement module solaire",
        "Changement boîtier",
        "Remplacement sonde",
        "Mise à niveau matériel",
        "Remplacement suite dommage"
    );

    public static final List<String> GENERIQUES = List.of(
        "Maintenance standard",
        "Intervention technique",
        "Contrôle général",
        "Mise à jour configuration"
    );

    /** Retourne les suggestions selon le type de maintenance */
    public static List<String> parType(String type) {
        if (type == null) return toutes();
        return switch (type.toLowerCase()) {
            case "preventive", "préventive" -> PREVENTIVE;
            case "corrective"               -> CORRECTIVE;
            case "remplacement"             -> REMPLACEMENT;
            default                         -> toutes();
        };
    }

    /** Retourne toutes les descriptions (toutes catégories confondues, sans doublons) */
    public static List<String> toutes() {
        java.util.LinkedHashSet<String> all = new java.util.LinkedHashSet<>();
        all.addAll(PREVENTIVE);
        all.addAll(CORRECTIVE);
        all.addAll(REMPLACEMENT);
        return new java.util.ArrayList<>(all);
    }
}
