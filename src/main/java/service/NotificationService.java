package service;

import model.Alerte;

/**
 * SERVICE MÉTIER — NotificationService
 * Gère les notifications quand une alerte est créée.
 * Pour l'instant affiche dans la console (peut être étendu avec email/SMS).
 */
public class NotificationService {

    /**
     * Notifie selon le niveau de l'alerte.
     * Critique → notification urgente
     * Haute    → notification standard
     * Moyenne  → log simple
     */
    public void notifier(Alerte a) {
        switch (a.getNiveau()) {
            case "Critique" -> {
                System.out.println("╔══════════════════════════════════════╗");
                System.out.println("║  ⚠  ALERTE CRITIQUE DÉTECTÉE !       ║");
                System.out.println("╚══════════════════════════════════════╝");
                System.out.println("  Type       : " + a.getTypeAlerte());
                System.out.println("  Zone       : " + a.getLocalisation());
                System.out.println("  Source     : " + a.getSource());
                System.out.println("  Date       : " + a.getDateFormatted());
                System.out.println("  → Intervention urgente requise !");
                System.out.println("─────────────────────────────────────────");
            }
            case "Haute" -> {
                System.out.println("⚠  [ALERTE HAUTE] " + a.getTypeAlerte()
                        + " — Zone : " + a.getLocalisation());
            }
            default -> {
                System.out.println("ℹ  [ALERTE MOYENNE] " + a.getTypeAlerte()
                        + " — " + a.getLocalisation());
            }
        }
    }

    /**
     * Notifie un changement de statut
     */
    public void notifierChangementStatut(int id, String ancienStatut, String nouveauStatut) {
        System.out.println("📋 Alerte #" + id + " : "
                + ancienStatut + " → " + nouveauStatut);
    }
}
