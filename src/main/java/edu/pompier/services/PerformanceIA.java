package edu.pompier.services;

import edu.pompier.tools.MyConnection;

import java.sql.*;
import java.util.*;

/**
 * ═══════════════════════════════════════════════════════════════
 *  PerformanceIA — Moteur d'évaluation des pompiers
 * ═══════════════════════════════════════════════════════════════
 *
 * Calcule un score de performance pour chaque pompier à partir
 * de données réelles (affectations, interventions, gardes).
 *
 * Critères pondérés :
 *   1. Nombre de missions traitées          → 25 pts max
 *   2. Rapidité d'intervention              → 20 pts max
 *   3. Taux de réussite                     → 20 pts max
 *   4. Difficulté des missions (CRITIQUE)   → 15 pts max
 *   5. Respect des gardes / présence        → 10 pts max
 *   6. Pénalité fatigue / surcharge         → -10 pts max
 *
 * Score total sur 100 pts.
 */
public class PerformanceIA {

    // ─── Résultat pour un pompier ───────────────────────────────
    public static class ResultatPerformance {
        public final int    idPompier;
        public final String nom;
        public final String prenom;
        public final String photoPath;           // chemin photo du pompier
        public final double scoreTotal;
        public final int    nbMissions;
        public final double tauxReussite;
        public final double tempsMoyenHeures;
        public final int    nbCritiques;
        public final int    nbGardes;
        public final String explication;
        public final String titre;
        public final boolean estGagnant;

        public ResultatPerformance(int idPompier, String nom, String prenom,
                                   double scoreTotal, int nbMissions, double tauxReussite,
                                   double tempsMoyenHeures, int nbCritiques, int nbGardes,
                                   String explication, String titre, boolean estGagnant) {
            this(idPompier, nom, prenom, null, scoreTotal, nbMissions, tauxReussite,
                    tempsMoyenHeures, nbCritiques, nbGardes, explication, titre, estGagnant);
        }

        public ResultatPerformance(int idPompier, String nom, String prenom, String photoPath,
                                   double scoreTotal, int nbMissions, double tauxReussite,
                                   double tempsMoyenHeures, int nbCritiques, int nbGardes,
                                   String explication, String titre, boolean estGagnant) {
            this.idPompier        = idPompier;
            this.nom              = nom;
            this.prenom           = prenom;
            this.photoPath        = photoPath;
            this.scoreTotal       = scoreTotal;
            this.nbMissions       = nbMissions;
            this.tauxReussite     = tauxReussite;
            this.tempsMoyenHeures = tempsMoyenHeures;
            this.nbCritiques      = nbCritiques;
            this.nbGardes         = nbGardes;
            this.explication      = explication;
            this.titre            = titre;
            this.estGagnant       = estGagnant;
        }
    }

    private final Connection cnx;

    public PerformanceIA() {
        this.cnx = MyConnection.getInstance().getCnx();
    }

    // ═══════════════════════════════════════════════════════════
    //  Point d'entrée principal
    // ═══════════════════════════════════════════════════════════

    /**
     * Évalue tous les pompiers sur la période donnée et retourne
     * la liste triée par score décroissant.
     *
     * @param periode "mois" ou "semaine"
     */
    public List<ResultatPerformance> evaluer(String periode) {
        String filtreDateSQL = "mois".equals(periode)
                ? "AND iv.start_date >= DATE_SUB(NOW(), INTERVAL 1 MONTH)"
                : "AND iv.start_date >= DATE_SUB(NOW(), INTERVAL 7 DAY)";

        List<ResultatPerformance> resultats = new ArrayList<>();

        // ── Récupérer tous les pompiers ──
        List<int[]>    pompierIds  = new ArrayList<>(); // {id}
        List<String[]> pompierInfo = new ArrayList<>(); // {nom, prenom, niveau_certif}

        try {
            String sqlP = "SELECT p.id, p.nom, p.prenom, c.niveau_requis, p.photo_path " +
                    "FROM pompier p " +
                    "LEFT JOIN pompier_certification pc ON pc.id_pompier = p.id " +
                    "LEFT JOIN certification c ON c.id = pc.id_certification " +
                    "WHERE p.statut != 'inactif'";
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(sqlP);
            while (rs.next()) {
                pompierIds.add(new int[]{ rs.getInt("id") });
                String photoPath = null;
                try { photoPath = rs.getString("photo_path"); } catch (Exception ignored) {}
                pompierInfo.add(new String[]{ rs.getString("nom"), rs.getString("prenom"),
                        rs.getString("niveau_requis"), photoPath });
            }
        } catch (SQLException e) {
            System.out.println("[PerformanceIA] Erreur chargement pompiers : " + e.getMessage());
        }

        if (pompierIds.isEmpty()) return resultats;

        // ── Calculer le score de chaque pompier ──
        for (int i = 0; i < pompierIds.size(); i++) {
            int    id     = pompierIds.get(i)[0];
            String nom    = pompierInfo.get(i)[0];
            String prenom = pompierInfo.get(i)[1];
            String niveau = pompierInfo.get(i)[2];
            String photo  = pompierInfo.get(i)[3];

            ResultatPerformance rp = calculerScore(id, nom, prenom, niveau, photo, filtreDateSQL);
            if (rp != null) resultats.add(rp);
        }

        // ── Trier par score décroissant ──
        resultats.sort((a, b) -> Double.compare(b.scoreTotal, a.scoreTotal));

        // ── Marquer le gagnant et générer les titres ──
        return enrichir(resultats);
    }

    // ═══════════════════════════════════════════════════════════
    //  Calcul du score pour un pompier
    // ═══════════════════════════════════════════════════════════

    private ResultatPerformance calculerScore(int idPompier, String nom, String prenom,
                                              String niveau, String photoPath, String filtreDateSQL) {
        double score = 0;

        // ── Données interventions (table intervention) ──
        int    nbMissions       = 0;
        int    nbTerminees      = 0;
        int    nbCritiques      = 0;
        double totalHeures      = 0;
        int    nbAvecDuree      = 0;

        try {
            // Cherche les interventions de ce pompier (par nom complet)
            String sqlIv = "SELECT iv.statut, iv.start_date, iv.end_date, a.niveau " +
                    "FROM intervention iv " +
                    "LEFT JOIN alerte a ON a.id = iv.alerte_id " +
                    "WHERE iv.agent_name LIKE ? " + filtreDateSQL;
            PreparedStatement ps = cnx.prepareStatement(sqlIv);
            ps.setString(1, "%" + prenom + "%" + nom + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                nbMissions++;
                String statut = rs.getString("statut");
                if (statut != null && (statut.toLowerCase().contains("termin")
                        || statut.toLowerCase().contains("complet"))) {
                    nbTerminees++;
                }
                String niveauAlerte = rs.getString("niveau");
                if ("CRITIQUE".equalsIgnoreCase(niveauAlerte)) nbCritiques++;

                // Calcul durée
                Timestamp start = rs.getTimestamp("start_date");
                Timestamp end   = rs.getTimestamp("end_date");
                if (start != null && end != null && end.after(start)) {
                    double heures = (end.getTime() - start.getTime()) / 3600000.0;
                    if (heures > 0 && heures < 72) { // ignorer les valeurs aberrantes
                        totalHeures += heures;
                        nbAvecDuree++;
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("[PerformanceIA] Erreur interventions pompier " + idPompier + " : " + e.getMessage());
        }

        // ── Données affectations (table affectation) ──
        int nbAffectations = 0;
        try {
            String sqlAf = "SELECT COUNT(*) FROM affectation af " +
                    "JOIN alerte a ON a.id = af.id_alerte " +
                    "WHERE af.id_pompier = ? AND af.statut != 'annule' " +
                    filtreDateSQL.replace("iv.start_date", "af.date_affectation");
            PreparedStatement ps = cnx.prepareStatement(sqlAf);
            ps.setInt(1, idPompier);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) nbAffectations = rs.getInt(1);
        } catch (SQLException e) {
            System.out.println("[PerformanceIA] Erreur affectations : " + e.getMessage());
        }

        // ── Données gardes (table garde) ──
        int nbGardes = 0;
        try {
            String sqlG = "SELECT COUNT(*) FROM garde WHERE id_pompier = ? AND disponible = 1 " +
                    filtreDateSQL.replace("iv.start_date", "date_garde");
            PreparedStatement ps = cnx.prepareStatement(sqlG);
            ps.setInt(1, idPompier);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) nbGardes = rs.getInt(1);
        } catch (SQLException e) {
            System.out.println("[PerformanceIA] Erreur gardes : " + e.getMessage());
        }

        // ── Calcul des composantes du score ──

        // 1. Missions traitées (25 pts max) — 5 pts par mission jusqu'à 5
        int totalMissions = nbMissions + nbAffectations;
        double scoreMissions = Math.min(25.0, totalMissions * 5.0);

        // 2. Rapidité (20 pts max) — score inversement proportionnel au temps moyen
        double scorRapidite = 0;
        double tempsMoyen   = nbAvecDuree > 0 ? totalHeures / nbAvecDuree : -1;
        if (tempsMoyen > 0) {
            // < 1h = 20 pts, 1-2h = 15 pts, 2-4h = 10 pts, 4-8h = 5 pts, > 8h = 0
            if      (tempsMoyen < 1)  scorRapidite = 20;
            else if (tempsMoyen < 2)  scorRapidite = 15;
            else if (tempsMoyen < 4)  scorRapidite = 10;
            else if (tempsMoyen < 8)  scorRapidite = 5;
        } else {
            scorRapidite = 10; // neutre si pas de données
        }

        // 3. Taux de réussite (20 pts max)
        double tauxReussite = nbMissions > 0 ? (double) nbTerminees / nbMissions : 0;
        double scorReussite = tauxReussite * 20.0;

        // 4. Difficulté / missions critiques (15 pts max) — 5 pts par mission critique
        double scorDifficulte = Math.min(15.0, nbCritiques * 5.0);

        // 5. Présence / gardes (10 pts max) — 2 pts par garde jusqu'à 5
        double scorGardes = Math.min(10.0, nbGardes * 2.0);

        // 6. Bonus niveau certification
        double bonusNiveau = 0;
        if (niveau != null) {
            bonusNiveau = switch (niveau.toUpperCase()) {
                case "EXPERT"        -> 5;
                case "AVANCE"        -> 3;
                case "INTERMEDIAIRE" -> 1;
                default              -> 0;
            };
        }

        // 7. Pénalité surcharge (missions > 8 en 1 mois → fatigue)
        double penalite = totalMissions > 8 ? Math.min(10, (totalMissions - 8) * 2.0) : 0;

        double scoreTotal = scoreMissions + scorRapidite + scorReussite
                + scorDifficulte + scorGardes + bonusNiveau - penalite;
        scoreTotal = Math.max(0, Math.min(100, scoreTotal));

        // Ignorer les pompiers sans aucune activité
        if (totalMissions == 0 && nbGardes == 0) return null;

        // ── Génération de l'explication ──
        String explication = genererExplication(nom, prenom, totalMissions, tauxReussite,
                tempsMoyen, nbCritiques, nbGardes, scorRapidite, scoreTotal);

        return new ResultatPerformance(idPompier, nom, prenom, photoPath, scoreTotal,
                totalMissions, tauxReussite, tempsMoyen, nbCritiques, nbGardes,
                explication, "", false);
    }

    // ═══════════════════════════════════════════════════════════
    //  Génération du texte explicatif (IA narrative)
    // ═══════════════════════════════════════════════════════════

    private String genererExplication(String nom, String prenom, int nbMissions,
                                      double tauxReussite, double tempsMoyen,
                                      int nbCritiques, int nbGardes,
                                      double scorRapidite, double scoreTotal) {
        StringBuilder sb = new StringBuilder();

        // Phrase d'accroche selon le score
        if      (scoreTotal >= 80) sb.append("Performance exceptionnelle. ");
        else if (scoreTotal >= 60) sb.append("Très bonne performance. ");
        else if (scoreTotal >= 40) sb.append("Performance satisfaisante. ");
        else                       sb.append("Performance en développement. ");

        // Missions
        if (nbMissions >= 5)
            sb.append(prenom).append(" a traité ").append(nbMissions).append(" missions ce mois — très actif. ");
        else if (nbMissions > 0)
            sb.append(nbMissions).append(" mission(s) traitée(s). ");

        // Rapidité
        if (tempsMoyen > 0) {
            if (tempsMoyen < 1)
                sb.append("Intervention ultra-rapide (moins d'1h en moyenne). ");
            else if (tempsMoyen < 2)
                sb.append(String.format("Temps moyen d'intervention : %.1fh — excellent. ", tempsMoyen));
            else
                sb.append(String.format("Temps moyen d'intervention : %.1fh. ", tempsMoyen));
        }

        // Taux de réussite
        if (tauxReussite >= 0.9)
            sb.append("Taux de réussite exceptionnel (").append(Math.round(tauxReussite * 100)).append("%). ");
        else if (tauxReussite >= 0.7)
            sb.append("Bon taux de réussite (").append(Math.round(tauxReussite * 100)).append("%). ");
        else if (tauxReussite > 0)
            sb.append("Taux de réussite : ").append(Math.round(tauxReussite * 100)).append("%. ");

        // Missions critiques
        if (nbCritiques >= 2)
            sb.append(nbCritiques).append(" incendies critiques gérés — courage et efficacité. ");
        else if (nbCritiques == 1)
            sb.append("A géré 1 incendie de niveau CRITIQUE. ");

        // Gardes
        if (nbGardes >= 5)
            sb.append("Présence exemplaire : ").append(nbGardes).append(" gardes honorées. ");
        else if (nbGardes > 0)
            sb.append(nbGardes).append(" garde(s) effectuée(s). ");

        return sb.toString().trim();
    }

    // ═══════════════════════════════════════════════════════════
    //  Attribution des titres et enrichissement
    // ═══════════════════════════════════════════════════════════

    private List<ResultatPerformance> enrichir(List<ResultatPerformance> liste) {
        if (liste.isEmpty()) return liste;

        // Trouver les champions par catégorie
        ResultatPerformance plusRapide   = null;
        ResultatPerformance plusActif    = null;
        ResultatPerformance plusCourageux = null;

        double minTemps = Double.MAX_VALUE;
        int    maxMiss  = 0;
        int    maxCrit  = 0;

        for (ResultatPerformance r : liste) {
            if (r.tempsMoyenHeures > 0 && r.tempsMoyenHeures < minTemps) {
                minTemps   = r.tempsMoyenHeures;
                plusRapide = r;
            }
            if (r.nbMissions > maxMiss) { maxMiss = r.nbMissions; plusActif = r; }
            if (r.nbCritiques > maxCrit) { maxCrit = r.nbCritiques; plusCourageux = r; }
        }

        // Construire la liste enrichie
        List<ResultatPerformance> enrichie = new ArrayList<>();
        for (int i = 0; i < liste.size(); i++) {
            ResultatPerformance r = liste.get(i);
            boolean gagnant = (i == 0);

            // Titre spécial
            String titre = "";
            if (gagnant)                          titre = "🏆 Meilleur pompier du mois";
            else if (r == plusRapide && i > 0)    titre = "⚡ Le plus rapide";
            else if (r == plusActif && i > 0)     titre = "🔥 Le plus actif";
            else if (r == plusCourageux && i > 0) titre = "💪 Le plus courageux";
            else if (i == 1)                      titre = "🥈 2ème place";
            else if (i == 2)                      titre = "🥉 3ème place";

            enrichie.add(new ResultatPerformance(
                    r.idPompier, r.nom, r.prenom, r.photoPath, r.scoreTotal,
                    r.nbMissions, r.tauxReussite, r.tempsMoyenHeures,
                    r.nbCritiques, r.nbGardes, r.explication, titre, gagnant));
        }
        return enrichie;
    }

    // ═══════════════════════════════════════════════════════════
    //  Mise à jour statut pompier quand end_date est renseignée
    // ═══════════════════════════════════════════════════════════

    /**
     * Vérifie si des interventions ont une end_date renseignée
     * dont le pompier correspondant est encore en_mission,
     * et le remet automatiquement en disponible.
     */
    public void verifierFinInterventions() {
        String sql = "SELECT DISTINCT iv.agent_name FROM intervention iv " +
                "WHERE iv.end_date IS NOT NULL AND iv.end_date <= NOW() " +
                "AND iv.statut NOT IN ('annulee','cancelled') " +
                "AND EXISTS (" +
                "  SELECT 1 FROM pompier p " +
                "  WHERE CONCAT(p.prenom,' ',p.nom) LIKE CONCAT('%',iv.agent_name,'%') " +
                "  AND p.statut = 'en_mission'" +
                ")";
        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                String agentName = rs.getString("agent_name");
                // Chercher le pompier correspondant
                String[] parts = agentName != null ? agentName.split(" ", 2) : new String[]{"",""};
                String prenom = parts[0].trim();
                String nom    = parts.length > 1 ? parts[1].trim() : "";

                // Vérifier qu'il n'a plus d'affectation active
                String sqlCheck = "SELECT p.id FROM pompier p " +
                        "WHERE p.prenom LIKE ? AND p.nom LIKE ? AND p.statut = 'en_mission' " +
                        "AND NOT EXISTS (" +
                        "  SELECT 1 FROM affectation af " +
                        "  WHERE af.id_pompier = p.id AND af.statut = 'en_cours'" +
                        ")";
                PreparedStatement psCheck = cnx.prepareStatement(sqlCheck);
                psCheck.setString(1, "%" + prenom + "%");
                psCheck.setString(2, "%" + nom + "%");
                ResultSet rsCheck = psCheck.executeQuery();

                while (rsCheck.next()) {
                    int idPompier = rsCheck.getInt("id");
                    PreparedStatement psUp = cnx.prepareStatement(
                            "UPDATE pompier SET statut='disponible' WHERE id=?");
                    psUp.setInt(1, idPompier);
                    psUp.executeUpdate();
                    System.out.println("[PerformanceIA] Pompier #" + idPompier +
                            " (" + prenom + " " + nom + ") remis disponible après fin d'intervention.");
                }
            }
        } catch (SQLException e) {
            System.out.println("[PerformanceIA] Erreur verifierFinInterventions : " + e.getMessage());
        }
    }
}