
package edu.pompier.services;

import edu.pompier.entities.Pompier;
import edu.pompier.tools.MyConnection;

import java.sql.*;
import java.util.*;

/**
 * Service de prediction de disponibilite des pompiers.
 *
 * Score = (historique_gardes x 0.4) + (fatigue_recente x 0.3) + (performance_IA x 0.3)
 *
 * - historique_gardes  : taux de presence aux gardes passees (0-100)
 * - fatigue_recente    : inverse du nombre de gardes sur 7 jours (0-100, 100 = pas fatigue)
 * - performance_IA     : score IA deja calcule dans PerformanceIA (0-100)
 */
public class PredictionDisponibilite {

    // ─── Résultat pour un pompier ───────────────────────────────
    public static class ResultatPrediction {
        public final int    idPompier;
        public final String nom;
        public final String prenom;
        public final String photoPath;
        public final double scoreTotal;          // 0-100
        public final double scoreHistorique;     // composante historique (0-100)
        public final double scoreFatigue;        // composante fatigue (0-100, 100=repose)
        public final double scorePerformance;    // composante IA (0-100)
        public final int    nbGardes7Jours;      // gardes sur les 7 derniers jours
        public final int    nbGardes30Jours;     // gardes sur les 30 derniers jours
        public final int    nbGardesPlanifiees;  // gardes futures planifiees
        public final String statut;              // statut actuel du pompier
        public final String niveauDispo;         // "OPTIMAL", "DISPONIBLE", "FATIGUE", "SURCHARGE"
        public final String explication;         // texte explicatif detaille
        public final String recommandation;      // conseil court

        public ResultatPrediction(int idPompier, String nom, String prenom, String photoPath,
                                  double scoreTotal, double scoreHistorique, double scoreFatigue,
                                  double scorePerformance, int nbGardes7Jours, int nbGardes30Jours,
                                  int nbGardesPlanifiees, String statut,
                                  String niveauDispo, String explication, String recommandation) {
            this.idPompier         = idPompier;
            this.nom               = nom;
            this.prenom            = prenom;
            this.photoPath         = photoPath;
            this.scoreTotal        = scoreTotal;
            this.scoreHistorique   = scoreHistorique;
            this.scoreFatigue      = scoreFatigue;
            this.scorePerformance  = scorePerformance;
            this.nbGardes7Jours    = nbGardes7Jours;
            this.nbGardes30Jours   = nbGardes30Jours;
            this.nbGardesPlanifiees = nbGardesPlanifiees;
            this.statut            = statut;
            this.niveauDispo       = niveauDispo;
            this.explication       = explication;
            this.recommandation    = recommandation;
        }
    }

    private final Connection cnx;

    public PredictionDisponibilite() {
        this.cnx = MyConnection.getInstance().getCnx();
    }

    // ═══════════════════════════════════════════════════════════
    //  Filtrage par jour spécifique
    // ═══════════════════════════════════════════════════════════

    /**
     * Recalcule les disponibilités pour un jour précis.
     * Tient compte des gardes déjà planifiées CE jour-là.
     * Retourne la liste triée : disponibles en premier.
     */
    public List<ResultatPrediction> predirePourJour(java.time.LocalDate jour,
                                                     List<ResultatPrediction> base) {
        String jourStr = jour.toString();
        List<ResultatPrediction> resultats = new ArrayList<>();

        for (ResultatPrediction r : base) {
            // Vérifier si ce pompier a déjà une garde planifiée CE jour précis
            boolean aGardeCeJour = false;
            int nbGardesCeJour = 0;
            try {
                PreparedStatement ps = cnx.prepareStatement(
                        "SELECT COUNT(*) FROM garde WHERE id_pompier=? AND date_garde=?");
                ps.setInt(1, r.idPompier);
                ps.setString(2, jourStr);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) nbGardesCeJour = rs.getInt(1);
                aGardeCeJour = nbGardesCeJour > 0;
            } catch (SQLException e) {
                System.out.println("[Prediction] Erreur garde jour : " + e.getMessage());
            }

            // Ajuster le score selon la situation ce jour précis
            double scoreAjuste = r.scoreTotal;
            String niveauAjuste = r.niveauDispo;
            String recoAjustee = r.recommandation;

            if (aGardeCeJour) {
                // Déjà une garde ce jour → SURCHARGE forcé pour ce jour
                scoreAjuste = Math.max(0, r.scoreTotal - 40);
                niveauAjuste = "SURCHARGE"; // forcé : ne pas assigner deux gardes le même jour
                recoAjustee = "Garde deja planifiee le " + jour.getDayOfMonth() + "/" + jour.getMonthValue()
                        + " — ne pas assigner un 2eme creneau ce jour";
            } else if ("en_mission".equals(r.statut)) {
                scoreAjuste = Math.max(0, r.scoreTotal - 25);
                niveauAjuste = scoreAjuste >= 50 ? "DISPONIBLE" : "FATIGUE";
                recoAjustee = "Actuellement en mission — disponibilite incertaine";
            }

            // Construire un résultat ajusté avec explication précise pour ce jour
            String explAjustee;
            if (aGardeCeJour) {
                explAjustee = r.prenom + " a deja une garde planifiee le "
                        + jour.getDayOfMonth() + "/" + jour.getMonthValue() + "/" + jour.getYear()
                        + ".\n\nAssigner un 2eme creneau le meme jour est deconseille — risque de fatigue excessive et d'erreur en intervention.\n\n"
                        + "Si c'est indispensable, choisissez un creneau different (ex: matin si elle a la nuit).";
            } else if ("en_mission".equals(r.statut)) {
                explAjustee = r.prenom + " est actuellement EN MISSION.\n\n"
                        + "Sa disponibilite pour ce jour est incertaine. Attendez la fin de sa mission avant de planifier.\n\n"
                        + r.explication;
            } else {
                explAjustee = r.explication;
            }

            resultats.add(new ResultatPrediction(
                    r.idPompier, r.nom, r.prenom, r.photoPath,
                    scoreAjuste, r.scoreHistorique, r.scoreFatigue, r.scorePerformance,
                    r.nbGardes7Jours, r.nbGardes30Jours,
                    r.nbGardesPlanifiees + nbGardesCeJour,
                    r.statut, niveauAjuste,
                    explAjustee,
                    recoAjustee
            ));
        }

        // Trier : disponibles en premier, puis par score décroissant
        resultats.sort((a, b) -> {
            // SURCHARGE toujours en dernier
            int ordreA = ordreNiveau(a.niveauDispo);
            int ordreB = ordreNiveau(b.niveauDispo);
            if (ordreA != ordreB) return Integer.compare(ordreA, ordreB);
            return Double.compare(b.scoreTotal, a.scoreTotal);
        });

        return resultats;
    }

    private int ordreNiveau(String niveau) {
        return switch (niveau) {
            case "OPTIMAL"    -> 0;
            case "DISPONIBLE" -> 1;
            case "FATIGUE"    -> 2;
            default           -> 3; // SURCHARGE
        };
    }

    // ═══════════════════════════════════════════════════════════
    //  Point d'entrée : calcule les prédictions pour tous les pompiers
    // ═══════════════════════════════════════════════════════════

    public List<ResultatPrediction> predireSemaineSuivante() {
        List<ResultatPrediction> resultats = new ArrayList<>();

        // Récupérer tous les pompiers actifs
        String sqlP = "SELECT p.id, p.nom, p.prenom, p.statut, p.photo_path, " +
                "c.niveau_requis as niveau_certif " +
                "FROM pompier p " +
                "LEFT JOIN pompier_certification pc ON pc.id_pompier = p.id " +
                "LEFT JOIN certification c ON c.id = pc.id_certification " +
                "WHERE p.statut != 'inactif'";
        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(sqlP);
            while (rs.next()) {
                int id = rs.getInt("id");
                String nom = rs.getString("nom");
                String prenom = rs.getString("prenom");
                String statut = rs.getString("statut");
                String photoPath = null;
                try { photoPath = rs.getString("photo_path"); } catch (Exception ignored) {}
                String niveau = rs.getString("niveau_certif");

                ResultatPrediction rp = calculerPrediction(id, nom, prenom, photoPath, statut, niveau);
                if (rp != null) resultats.add(rp);
            }
        } catch (SQLException e) {
            System.out.println("[PredictionDisponibilite] Erreur : " + e.getMessage());
        }

        // Trier par score décroissant
        resultats.sort((a, b) -> Double.compare(b.scoreTotal, a.scoreTotal));
        return resultats;
    }

    // ═══════════════════════════════════════════════════════════
    //  Calcul du score pour un pompier
    // ═══════════════════════════════════════════════════════════

    private ResultatPrediction calculerPrediction(int idPompier, String nom, String prenom,
                                                   String photoPath, String statut, String niveau) {
        // ── 1. Historique gardes (40%) ──
        // Taux de présence : gardes effectuées / gardes planifiées sur 30 jours
        int nbGardes30 = 0;
        int nbGardes7  = 0;
        int nbGardesFutures = 0;
        try {
            // ── Gardes sur les 30 derniers jours ET les 30 prochains jours ──
            // (les gardes sont planifiées à l'avance, donc on regarde passé + futur proche)
            PreparedStatement ps30 = cnx.prepareStatement(
                    "SELECT COUNT(*) FROM garde WHERE id_pompier=? " +
                    "AND date_garde >= DATE_SUB(CURDATE(), INTERVAL 30 DAY) " +
                    "AND date_garde <= DATE_ADD(CURDATE(), INTERVAL 30 DAY)");
            ps30.setInt(1, idPompier);
            ResultSet rs30 = ps30.executeQuery();
            if (rs30.next()) nbGardes30 = rs30.getInt(1);

            // ── Gardes sur les 7 derniers jours ET les 7 prochains jours ──
            PreparedStatement ps7 = cnx.prepareStatement(
                    "SELECT COUNT(*) FROM garde WHERE id_pompier=? " +
                    "AND date_garde >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) " +
                    "AND date_garde <= DATE_ADD(CURDATE(), INTERVAL 7 DAY)");
            ps7.setInt(1, idPompier);
            ResultSet rs7 = ps7.executeQuery();
            if (rs7.next()) nbGardes7 = rs7.getInt(1);

            // ── Gardes planifiées la semaine prochaine ──
            PreparedStatement psFut = cnx.prepareStatement(
                    "SELECT COUNT(*) FROM garde WHERE id_pompier=? " +
                    "AND date_garde > CURDATE() " +
                    "AND date_garde <= DATE_ADD(CURDATE(), INTERVAL 7 DAY)");
            psFut.setInt(1, idPompier);
            ResultSet rsFut = psFut.executeQuery();
            if (rsFut.next()) nbGardesFutures = rsFut.getInt(1);
        } catch (SQLException e) {
            System.out.println("[Prediction] Erreur gardes : " + e.getMessage());
        }

        // ── 1. Score disponibilité (basé sur la fatigue récente) ──
        // Logique : 0 gardes sur 7j = 100 (reposé/nouveau), 7+ gardes = 0 (épuisé)
        // Un nouveau pompier (0 gardes) est REPOSÉ → score élevé
        double scoreFatigue = Math.max(0, 100.0 - (nbGardes7 * 15.0));
        // Pénalité si gardes déjà planifiées la semaine prochaine
        scoreFatigue = Math.max(0, scoreFatigue - (nbGardesFutures * 12.0));

        // ── 2. Score historique (fiabilité) ──
        // Nouveau pompier (0 gardes) → score NEUTRE (60), pas pénalisé
        // Pompier actif (5+ gardes/mois) → score élevé (fiable)
        // Pompier surchargé (10+ gardes/mois) → score baisse (risque d'absence)
        double scoreHistorique;
        if (nbGardes30 == 0) {
            scoreHistorique = 60.0; // nouveau pompier → neutre, présumé disponible
        } else if (nbGardes30 <= 5) {
            scoreHistorique = 60.0 + (nbGardes30 * 8.0); // 1-5 gardes → fiable (68-100)
        } else {
            scoreHistorique = Math.max(30.0, 100.0 - ((nbGardes30 - 5) * 10.0)); // surcharge → baisse
        }

        // ── 3. Performance IA (30%) ──
        double scorePerformance = 50.0; // valeur neutre par défaut
        try {
            // Récupérer le score IA depuis les affectations et interventions
            String sqlPerf = "SELECT COUNT(*) as nb_missions, " +
                    "SUM(CASE WHEN af.statut='termine' THEN 1 ELSE 0 END) as nb_terminees " +
                    "FROM affectation af WHERE af.id_pompier=? AND af.statut != 'annule' " +
                    "AND af.date_affectation >= DATE_SUB(NOW(), INTERVAL 30 DAY)";
            PreparedStatement psPerf = cnx.prepareStatement(sqlPerf);
            psPerf.setInt(1, idPompier);
            ResultSet rsPerf = psPerf.executeQuery();
            if (rsPerf.next()) {
                int nbMissions = rsPerf.getInt("nb_missions");
                int nbTerminees = rsPerf.getInt("nb_terminees");
                if (nbMissions > 0) {
                    double tauxReussite = (double) nbTerminees / nbMissions;
                    scorePerformance = Math.min(100.0, (nbMissions * 8.0) + (tauxReussite * 40.0));
                }
            }
            // Bonus certification
            if (niveau != null) {
                scorePerformance += switch (niveau.toUpperCase()) {
                    case "EXPERT" -> 10; case "AVANCE" -> 7;
                    case "INTERMEDIAIRE" -> 4; default -> 1;
                };
            }
            scorePerformance = Math.min(100.0, scorePerformance);
        } catch (SQLException e) {
            System.out.println("[Prediction] Erreur performance : " + e.getMessage());
        }

        // ── Score final ──
        // Fatigue = facteur principal (50%) — si reposé, il est disponible
        // Historique = fiabilité (30%)
        // Performance = qualité (20%)
        double scoreTotal = (scoreFatigue * 0.50) + (scoreHistorique * 0.30) + (scorePerformance * 0.20);
        scoreTotal = Math.max(0, Math.min(100, scoreTotal));

        // Pénalité si en mission actuellement
        if ("en_mission".equals(statut)) scoreTotal = Math.max(0, scoreTotal - 25);

        // ── Niveau de disponibilité — seuils recalibrés ──
        // Un nouveau pompier reposé doit être OPTIMAL ou DISPONIBLE
        String niveauDispo;
        if      (scoreTotal >= 70) niveauDispo = "OPTIMAL";
        else if (scoreTotal >= 50) niveauDispo = "DISPONIBLE";
        else if (scoreTotal >= 30) niveauDispo = "FATIGUE";
        else                       niveauDispo = "SURCHARGE";

        // ── Explication détaillée ──
        String explication = genererExplication(nom, prenom, scoreTotal, scoreHistorique,
                scoreFatigue, scorePerformance, nbGardes7, nbGardes30, nbGardesFutures, statut, niveauDispo);

        // ── Recommandation courte ──
        String recommandation = genererRecommandation(scoreTotal, nbGardes7, nbGardesFutures, statut);

        return new ResultatPrediction(idPompier, nom, prenom, photoPath,
                scoreTotal, scoreHistorique, scoreFatigue, scorePerformance,
                nbGardes7, nbGardes30, nbGardesFutures, statut,
                niveauDispo, explication, recommandation);
    }

    // ═══════════════════════════════════════════════════════════
    //  Génération des textes
    // ═══════════════════════════════════════════════════════════

    private String genererExplication(String nom, String prenom, double scoreTotal,
                                       double scoreHistorique, double scoreFatigue,
                                       double scorePerformance, int nbGardes7,
                                       int nbGardes30, int nbGardesFutures,
                                       String statut, String niveauDispo) {
        StringBuilder sb = new StringBuilder();

        // Phrase d'accroche
        switch (niveauDispo) {
            case "OPTIMAL"    -> sb.append(prenom).append(" est en excellente forme et tres disponible pour la semaine prochaine.\n\n");
            case "DISPONIBLE" -> sb.append(prenom).append(" est disponible mais a deja fourni un effort notable recemment.\n\n");
            case "FATIGUE"    -> sb.append(prenom).append(" montre des signes de fatigue — a utiliser avec moderation.\n\n");
            default           -> sb.append(prenom).append(" est en surcharge. Il est deconseille de lui assigner de nouvelles gardes.\n\n");
        }

        // Détail des 3 composantes
        sb.append("Historique (40%) : ").append(String.format("%.0f/100", scoreHistorique))
          .append(" — ").append(nbGardes30).append(" garde(s) sur les 30 derniers jours.\n");

        sb.append("Fatigue (30%) : ").append(String.format("%.0f/100", scoreFatigue))
          .append(" — ").append(nbGardes7).append(" garde(s) sur les 7 derniers jours");
        if (nbGardesFutures > 0)
            sb.append(", ").append(nbGardesFutures).append(" garde(s) deja planifiee(s) la semaine prochaine");
        sb.append(".\n");

        sb.append("Performance IA (30%) : ").append(String.format("%.0f/100", scorePerformance))
          .append(" — base sur les missions et le taux de reussite.\n");

        // Statut actuel
        if ("en_mission".equals(statut))
            sb.append("\nAttention : ").append(prenom).append(" est actuellement EN MISSION (-20 pts de penalite).");

        return sb.toString().trim();
    }

    private String genererRecommandation(double scoreTotal, int nbGardes7,
                                          int nbGardesFutures, String statut) {
        if ("en_mission".equals(statut))
            return "Actuellement en mission — attendre son retour";
        if (nbGardesFutures >= 3)
            return "Deja " + nbGardesFutures + " gardes planifiees — eviter de surcharger";
        if (nbGardes7 >= 4)
            return "A enchaine " + nbGardes7 + " gardes en 7 jours — repos recommande";
        if (scoreTotal >= 75)
            return "Recommande pour les gardes de la semaine prochaine";
        if (scoreTotal >= 55)
            return "Peut etre assigne avec moderation";
        return "Deconseille cette semaine — risque d'absence";
    }
}
