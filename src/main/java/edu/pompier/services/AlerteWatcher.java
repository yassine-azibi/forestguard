package edu.pompier.services;

import edu.pompier.tools.EmailService;
import javafx.application.Platform;

/**
 * Thread démon qui surveille la table alerte toutes les 5 secondes.
 * Dès qu'une alerte "Nouvelle" sans affectation est détectée :
 *   1. Sélectionne le/les meilleur(s) pompier(s) automatiquement
 *   2. Insère la ligne dans la table affectation
 *   3. Envoie un email d'alerte à chaque pompier affecté
 *   4. Rafraîchit l'interface JavaFX
 */
public class AlerteWatcher implements Runnable {

    private static final int INTERVALLE_MS = 5000;

    private final PompierService service;
    private volatile boolean actif = true;
    private final Runnable onAffectation;

    public AlerteWatcher(PompierService service, Runnable onAffectation) {
        this.service       = service;
        this.onAffectation = onAffectation;
    }

    @Override
    public void run() {
        System.out.println("[AlerteWatcher] Surveillance demarree (intervalle : "
                + INTERVALLE_MS / 1000 + "s)");
        while (actif) {
            try {
                verifierEtAffecter();
                Thread.sleep(INTERVALLE_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.out.println("[AlerteWatcher] Erreur : " + e.getMessage());
            }
        }
        System.out.println("[AlerteWatcher] Surveillance arretee.");
    }

    private void verifierEtAffecter() {
        java.util.List<String[]> alertes = service.getAlertesNonAffectees();

        for (String[] alerte : alertes) {
            int    idAlerte     = Integer.parseInt(alerte[0]);
            String typeAlerte   = alerte[1];
            String niveauAlerte = alerte[2];
            String localisation = alerte[3];

            System.out.println("[AlerteWatcher] Alerte detectee -> ID#" + idAlerte
                    + " | " + typeAlerte + " | " + niveauAlerte + " | " + localisation);

            // Affecte le/les meilleur(s) pompier(s) (plusieurs si score ex-aequo)
            java.util.List<PompierService.ResultatAffectation> resultats =
                    service.affecterAutomatiquementTous(idAlerte, localisation);

            if (resultats == null || resultats.isEmpty()) {
                System.out.println("[AlerteWatcher] Aucun pompier disponible pour ID#" + idAlerte);
                continue;
            }

            for (PompierService.ResultatAffectation r : resultats) {
                System.out.println("[AlerteWatcher] Affecte : "
                        + r.pompier.getNom() + " " + r.pompier.getPrenom()
                        + " | " + String.format("%.1f", r.distanceKm) + " km"
                        + " | score " + String.format("%.1f", r.score));

                // Convertir les coords en nom lisible pour l'email
                String nomLieu = PompierService.coordsVersNom(localisation);

                // Envoi de l'email d'affectation au pompier
                EmailService.envoyerEmailAffectation(
                        r.pompier.getEmail(),
                        r.pompier.getPrenom() + " " + r.pompier.getNom(),
                        typeAlerte,
                        niveauAlerte,
                        nomLieu,   // ✅ "Mateur, Bizerte" au lieu de "37.0574,9.6639"
                        r.distanceKm,
                        r.score
                );
            }

            // Rafraichir l'interface JavaFX
            if (onAffectation != null) {
                Platform.runLater(onAffectation);
            }
        }
    }

    public void arreter() {
        actif = false;
    }
}