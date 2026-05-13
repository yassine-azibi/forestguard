package service;

import model.Alerte;

/**
 * SERVICE MÉTIER — CapteurService
 * Traite les données reçues des capteurs IoT.
 * Décide si une alerte doit être créée et délègue à AlerteService.
 */
public class CapteurService {

    private final AlerteService alerteService = new AlerteService();

    /**
     * Point d'entrée principal : traite les données d'un capteur.
     * Si les seuils sont dépassés → crée automatiquement une alerte en BDD.
     *
     * @param capteurId  identifiant du capteur (ex: "Capteur C-001")
     * @param zone       zone géographique (ex: "Zone Nord-A, Cap Bon")
     * @param temp       température en °C
     * @param fumee      taux de fumée en ppm
     * @param humidite   humidité en %
     * @return true si une alerte a été créée
     */
    public boolean traiterDonnees(String capteurId, String zone,
                                   double temp, double fumee, double humidite) {

        System.out.println("📡 Capteur " + capteurId + " → T:" + temp
                + "°C | Fumée:" + fumee + "ppm | Hum:" + humidite + "%");

        // Règle métier : vérifier si seuils dépassés
        if (!alerteService.seuilDepasse(temp, fumee, humidite)) {
            System.out.println("   ✔ Valeurs normales — aucune alerte");
            return false;
        }

        // Calculer niveau et type automatiquement
        String niveau = alerteService.calculerNiveau(temp, fumee, humidite);
        String type   = alerteService.determinerType(temp, fumee);

        // Créer l'alerte via le service métier
        Alerte alerte = new Alerte(type, niveau, zone, capteurId);
        boolean ok = alerteService.creerAlerte(alerte);

        if (ok) {
            System.out.println("   ⚠  Alerte créée : " + type
                    + " [" + niveau + "] → " + zone);
        }
        return ok;
    }

    /**
     * Retourne une description textuelle de l'état du capteur
     */
    public String getEtatCapteur(double temp, double fumee, double humidite) {
        if (!alerteService.seuilDepasse(temp, fumee, humidite)) {
            return "✔ Normal";
        }
        String niveau = alerteService.calculerNiveau(temp, fumee, humidite);
        return "⚠ " + niveau;
    }
}
