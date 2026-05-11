package ForestGuard.services;

import ForestGuard.entities.RapportStatistique;
import ForestGuard.utils.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service de calcul et persistance des rapports statistiques d'incendie.
 * Logique de risque :
 *   temperature > 35 || fumee > 80  → Danger
 *   temperature > 28 || fumee > 50 || humidite < 30 → Attention
 *   sinon → Sûr
 */
public class RapportStatistiqueService {

    // ── Connexion ─────────────────────────────────────────
    private Connection getConn() {
        try {
            Connection c = MyConnection.getInstance().getCnx();
            if (c == null || c.isClosed() || !c.isValid(2)) {
                MyConnection.reset();
                c = MyConnection.getInstance().getCnx();
            }
            return c;
        } catch (SQLException e) {
            return MyConnection.getInstance().getCnx();
        }
    }

    // ════════════════════════════════════════════════════════
    // Créer la table si elle n'existe pas encore
    // ════════════════════════════════════════════════════════
    public void creerTableSiAbsente() {
        String sql = """
                CREATE TABLE IF NOT EXISTS rapport_statistique (
                    id                   INT AUTO_INCREMENT PRIMARY KEY,
                    foret                VARCHAR(150)  NOT NULL,
                    localisation         VARCHAR(200)  DEFAULT '',
                    mois                 INT           NOT NULL,
                    annee                INT           NOT NULL,
                    pourcentage_incendie DOUBLE        NOT NULL DEFAULT 0,
                    nb_danger            INT           NOT NULL DEFAULT 0,
                    nb_attention         INT           NOT NULL DEFAULT 0,
                    nb_sur               INT           NOT NULL DEFAULT 0,
                    temp_moyenne         DOUBLE        NOT NULL DEFAULT 0,
                    hum_moyenne          DOUBLE        NOT NULL DEFAULT 0,
                    fumee_moyenne        DOUBLE        NOT NULL DEFAULT 0,
                    date_calcul          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE KEY uq_foret_periode (foret, mois, annee)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """;
        try (Statement st = getConn().createStatement()) {
            st.executeUpdate(sql);
        } catch (SQLException e) {
            System.out.println("creerTableSiAbsente : " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════
    // Calcul principal + sauvegarde
    // ════════════════════════════════════════════════════════
    /**
     * Calcule le rapport statistique pour une forêt et une période donnée,
     * puis le sauvegarde (INSERT ou UPDATE si déjà existant).
     *
     * @param foret  Nom de la forêt (correspond à la colonne foret de donnee_capteur)
     * @param mois   Mois (1–12)
     * @param annee  Année (ex: 2026)
     * @return Le rapport calculé, ou null si aucune donnée trouvée
     */
    public RapportStatistique calculerEtSauvegarder(String foret, int mois, int annee) {
        creerTableSiAbsente();

        // ── 1. Récupérer les données de la période ────────
        String colForet = detecterColonneForet();
        String sql = "SELECT temperature, humidite, fumee, horodatage " +
                "FROM donnee_capteur " +
                "WHERE " + colForet + " = ? " +
                "AND MONTH(horodatage) = ? " +
                "AND YEAR(horodatage)  = ? " +
                "ORDER BY horodatage ASC";

        int    nbDanger    = 0, nbAttention = 0, nbSur = 0;
        double sumTemp     = 0, sumHum = 0, sumFumee = 0;
        int    total       = 0;

        try (PreparedStatement pst = getConn().prepareStatement(sql)) {
            pst.setString(1, foret);
            pst.setInt(2, mois);
            pst.setInt(3, annee);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                double temp  = rs.getDouble("temperature");
                double hum   = rs.getDouble("humidite");
                double fumee = rs.getDouble("fumee");

                sumTemp  += temp;
                sumHum   += hum;
                sumFumee += fumee;
                total++;

                // Logique de risque
                if (temp > 35 || fumee > 80)                    nbDanger++;
                else if (temp > 28 || fumee > 50 || hum < 30)  nbAttention++;
                else                                             nbSur++;
            }
        } catch (SQLException e) {
            System.out.println("Erreur calcul rapport : " + e.getMessage());
            e.printStackTrace();
            return null;
        }

        if (total == 0) {
            System.out.println("Aucune donnée pour " + foret +
                    " en " + mois + "/" + annee);
            return null;
        }

        // ── 2. Calculer les moyennes et le pourcentage ────
        double tempMoy   = sumTemp  / total;
        double humMoy    = sumHum   / total;
        double fumeeMoy  = sumFumee / total;
        // Pourcentage = (Danger*100 + Attention*50) / total
        double pct = ((nbDanger * 100.0) + (nbAttention * 50.0)) / total;
        pct = Math.min(100.0, Math.round(pct * 10.0) / 10.0);

        // ── 3. Récupérer la localisation depuis la table foret ──
        String localisation = getLocalisation(foret);

        // ── 4. Construire l'objet rapport ─────────────────
        RapportStatistique rapport = new RapportStatistique(
                foret, localisation, mois, annee,
                pct, nbDanger, nbAttention, nbSur,
                Math.round(tempMoy  * 10.0) / 10.0,
                Math.round(humMoy   * 10.0) / 10.0,
                Math.round(fumeeMoy * 10.0) / 10.0);

        // ── 5. Sauvegarder (INSERT ou UPDATE) ─────────────
        sauvegarder(rapport);
        System.out.println("Rapport calculé : " + rapport);
        return rapport;
    }

    // ════════════════════════════════════════════════════════
    // Données jour par jour pour le graphique
    // ════════════════════════════════════════════════════════
    /**
     * Retourne le score de risque (0–100) par jour du mois.
     * Clé = "01", "02" … "31", Valeur = score risque
     */
    public Map<String, Double> getRisqueParJour(String foret, int mois, int annee) {
        Map<String, Double> parJour = new LinkedHashMap<>();
        String colForet = detecterColonneForet();
        String sql = "SELECT DAY(horodatage) AS jour, " +
                "AVG(temperature) AS t, AVG(humidite) AS h, AVG(fumee) AS f " +
                "FROM donnee_capteur " +
                "WHERE " + colForet + " = ? " +
                "AND MONTH(horodatage) = ? AND YEAR(horodatage) = ? " +
                "GROUP BY DAY(horodatage) ORDER BY jour";
        try (PreparedStatement pst = getConn().prepareStatement(sql)) {
            pst.setString(1, foret);
            pst.setInt(2, mois);
            pst.setInt(3, annee);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                int    jour  = rs.getInt("jour");
                double t     = rs.getDouble("t");
                double h     = rs.getDouble("h");
                double f     = rs.getDouble("f");
                double score;
                if (t > 35 || f > 80)               score = 100;
                else if (t > 28 || f > 50 || h < 30) score = 60;
                else if (t > 25 || h < 50)            score = 30;
                else                                   score = 10;
                parJour.put(String.format("%02d", jour), score);
            }
        } catch (SQLException e) {
            System.out.println("Erreur getRisqueParJour : " + e.getMessage());
        }
        return parJour;
    }

    // ════════════════════════════════════════════════════════
    // Historique des rapports sauvegardés
    // ════════════════════════════════════════════════════════
    public List<RapportStatistique> getHistorique() {
        List<RapportStatistique> liste = new ArrayList<>();
        String sql = "SELECT * FROM rapport_statistique ORDER BY annee DESC, mois DESC";
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) liste.add(mapRow(rs));
        } catch (SQLException e) {
            System.out.println("Erreur getHistorique : " + e.getMessage());
        }
        return liste;
    }

    public List<RapportStatistique> getHistoriqueParForet(String foret) {
        List<RapportStatistique> liste = new ArrayList<>();
        String sql = "SELECT * FROM rapport_statistique WHERE foret = ? " +
                "ORDER BY annee DESC, mois DESC";
        try (PreparedStatement pst = getConn().prepareStatement(sql)) {
            pst.setString(1, foret);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) liste.add(mapRow(rs));
        } catch (SQLException e) {
            System.out.println("Erreur getHistoriqueParForet : " + e.getMessage());
        }
        return liste;
    }

    public void supprimer(int id) {
        try (PreparedStatement pst = getConn().prepareStatement(
                "DELETE FROM rapport_statistique WHERE id = ?")) {
            pst.setInt(1, id);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur supprimer rapport : " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════
    // Liste des forêts disponibles dans donnee_capteur
    // ════════════════════════════════════════════════════════
    public List<String> getForetsDisponibles() {
        List<String> liste = new ArrayList<>();
        // Essayer d'abord la table foret
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery("SELECT nom FROM foret ORDER BY nom")) {
            while (rs.next()) liste.add(rs.getString("nom"));
            if (!liste.isEmpty()) return liste;
        } catch (SQLException ignored) {}
        // Fallback : valeurs distinctes dans donnee_capteur
        String col = detecterColonneForet();
        try (PreparedStatement pst = getConn().prepareStatement(
                "SELECT DISTINCT " + col + " FROM donnee_capteur " +
                "WHERE " + col + " IS NOT NULL AND " + col + " != '' ORDER BY " + col)) {
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                String v = rs.getString(1);
                if (v != null && !v.isBlank()) liste.add(v);
            }
        } catch (SQLException e) {
            System.out.println("Erreur getForetsDisponibles : " + e.getMessage());
        }
        return liste;
    }

    // ════════════════════════════════════════════════════════
    // Helpers privés
    // ════════════════════════════════════════════════════════
    private void sauvegarder(RapportStatistique r) {
        // INSERT … ON DUPLICATE KEY UPDATE (clé unique foret+mois+annee)
        String sql = """
                INSERT INTO rapport_statistique
                  (foret, localisation, mois, annee, pourcentage_incendie,
                   nb_danger, nb_attention, nb_sur,
                   temp_moyenne, hum_moyenne, fumee_moyenne, date_calcul)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
                ON DUPLICATE KEY UPDATE
                  localisation        = VALUES(localisation),
                  pourcentage_incendie= VALUES(pourcentage_incendie),
                  nb_danger           = VALUES(nb_danger),
                  nb_attention        = VALUES(nb_attention),
                  nb_sur              = VALUES(nb_sur),
                  temp_moyenne        = VALUES(temp_moyenne),
                  hum_moyenne         = VALUES(hum_moyenne),
                  fumee_moyenne       = VALUES(fumee_moyenne),
                  date_calcul         = VALUES(date_calcul)
                """;
        try (PreparedStatement pst = getConn().prepareStatement(sql)) {
            pst.setString(1, r.getForet());
            pst.setString(2, r.getLocalisation() != null ? r.getLocalisation() : "");
            pst.setInt(3, r.getMois());
            pst.setInt(4, r.getAnnee());
            pst.setDouble(5, r.getPourcentageIncendie());
            pst.setInt(6, r.getNbDanger());
            pst.setInt(7, r.getNbAttention());
            pst.setInt(8, r.getNbSur());
            pst.setDouble(9, r.getTempMoyenne());
            pst.setDouble(10, r.getHumMoyenne());
            pst.setDouble(11, r.getFumeeMoyenne());
            pst.setTimestamp(12, Timestamp.valueOf(LocalDateTime.now()));
            pst.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur sauvegarder rapport : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String detecterColonneForet() {
        try {
            ResultSet cols = getConn().getMetaData()
                    .getColumns(null, null, "donnee_capteur", null);
            while (cols.next()) {
                String c = cols.getString("COLUMN_NAME").toLowerCase();
                if (c.equals("foret") || c.equals("zone") || c.equals("zone_foret"))
                    return cols.getString("COLUMN_NAME");
            }
        } catch (SQLException ignored) {}
        return "foret";
    }

    private String getLocalisation(String foret) {
        try (PreparedStatement pst = getConn().prepareStatement(
                "SELECT localisation FROM foret WHERE nom = ? LIMIT 1")) {
            pst.setString(1, foret);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return rs.getString("localisation");
        } catch (SQLException ignored) {}
        return "";
    }

    private RapportStatistique mapRow(ResultSet rs) throws SQLException {
        RapportStatistique r = new RapportStatistique();
        r.setId(rs.getInt("id"));
        r.setForet(rs.getString("foret"));
        r.setLocalisation(rs.getString("localisation"));
        r.setMois(rs.getInt("mois"));
        r.setAnnee(rs.getInt("annee"));
        r.setPourcentageIncendie(rs.getDouble("pourcentage_incendie"));
        r.setNbDanger(rs.getInt("nb_danger"));
        r.setNbAttention(rs.getInt("nb_attention"));
        r.setNbSur(rs.getInt("nb_sur"));
        r.setTempMoyenne(rs.getDouble("temp_moyenne"));
        r.setHumMoyenne(rs.getDouble("hum_moyenne"));
        r.setFumeeMoyenne(rs.getDouble("fumee_moyenne"));
        Timestamp ts = rs.getTimestamp("date_calcul");
        if (ts != null) r.setDateCalcul(ts.toLocalDateTime());
        return r;
    }
}
