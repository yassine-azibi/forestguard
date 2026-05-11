package ForestGuard.services;

import ForestGuard.entities.DonCapteur;
import ForestGuard.interfaces.IService;
import ForestGuard.utils.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DonCapteurService implements IService<DonCapteur> {

    // ── Connexion robuste avec reconnexion automatique ────
    private Connection getConnection() {
        try {
            Connection cnx = MyConnection.getInstance().getCnx();
            // Vérifier si la connexion est encore valide
            if (cnx == null || cnx.isClosed() || !cnx.isValid(2)) {
                System.out.println("Connexion perdue — reconnexion...");
                MyConnection.reset();
                cnx = MyConnection.getInstance().getCnx();
            }
            return cnx;
        } catch (SQLException e) {
            System.out.println("Erreur getConnection : " + e.getMessage());
            return MyConnection.getInstance().getCnx();
        }
    }

    // ════════════════════════════════════════════════════════
    // Détecter le vrai nom de la colonne zone/foret
    // ════════════════════════════════════════════════════════
    private String detecterColonneZone() {
        try {
            ResultSet cols = getConnection().getMetaData()
                    .getColumns(null, null, "donnee_capteur", null);
            while (cols.next()) {
                String col = cols.getString("COLUMN_NAME").toLowerCase();
                if (col.equals("zone") || col.equals("foret") || col.equals("zone_foret"))
                    return cols.getString("COLUMN_NAME");
            }
        } catch (SQLException e) {
            System.out.println("Erreur detecterColonneZone : " + e.getMessage());
        }
        return "zone"; // valeur par défaut
    }

    // ════════════════════════════════════════════════════════
    // INSERT
    // ════════════════════════════════════════════════════════
    @Override
    public void addEntity(DonCapteur d) {
        String colZone = detecterColonneZone();
        String requete = "INSERT INTO donnee_capteur " +
                "(type, capteur, " + colZone + ", temperature, humidite, fumee, risque, horodatage) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = getConnection().prepareStatement(requete)) {
            pst.setString(1, d.getType() != null ? d.getType() : "Manuelle");
            pst.setString(2, d.getCapteur());
            pst.setString(3, d.getZone());
            pst.setDouble(4, d.getTemperature());
            pst.setDouble(5, d.getHumidite());
            pst.setDouble(6, d.getFumee());
            pst.setString(7, d.getRisque());
            pst.setTimestamp(8, d.getHorodatage() != null
                    ? Timestamp.valueOf(d.getHorodatage())
                    : Timestamp.valueOf(LocalDateTime.now()));
            int rows = pst.executeUpdate();
            System.out.println("addEntity OK (" + rows + " ligne) capteur=" +
                    d.getCapteur() + " " + colZone + "=" + d.getZone());
        } catch (SQLException e) {
            System.out.println("Erreur addEntity : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ════════════════════════════════════════════════════════
    // DELETE
    // ════════════════════════════════════════════════════════
    @Override
    public void deleteEntity(DonCapteur d) {
        try (PreparedStatement pst = getConnection()
                .prepareStatement("DELETE FROM donnee_capteur WHERE id = ?")) {
            pst.setInt(1, d.getId());
            pst.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur deleteEntity : " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════
    // UPDATE
    // ════════════════════════════════════════════════════════
    @Override
    public void updateEntity(int id, DonCapteur d) {
        String colZone = detecterColonneZone();
        String requete = "UPDATE donnee_capteur SET " +
                "type=?, capteur=?, " + colZone + "=?, " +
                "temperature=?, humidite=?, fumee=?, risque=? WHERE id=?";
        try (PreparedStatement pst = getConnection().prepareStatement(requete)) {
            pst.setString(1, d.getType());
            pst.setString(2, d.getCapteur());
            pst.setString(3, d.getZone());
            pst.setDouble(4, d.getTemperature());
            pst.setDouble(5, d.getHumidite());
            pst.setDouble(6, d.getFumee());
            pst.setString(7, d.getRisque());
            pst.setInt(8, id);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur updateEntity : " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════
    // SELECT ALL — robuste, lit toutes les colonnes dynamiquement
    // ════════════════════════════════════════════════════════
    @Override
    public List<DonCapteur> getData() {
        return getDataSimple();
    }

    public List<DonCapteur> getDataAvecForet() {
        return getDataSimple();
    }

    public List<DonCapteur> getDataSimple() {
        List<DonCapteur> liste = new ArrayList<>();
        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT * FROM donnee_capteur ORDER BY horodatage DESC")) {

            ResultSetMetaData meta = rs.getMetaData();
            java.util.Set<String> cols = new java.util.HashSet<>();
            for (int i = 1; i <= meta.getColumnCount(); i++)
                cols.add(meta.getColumnName(i).toLowerCase());

            // Détecter la colonne zone une seule fois
            String colZone = cols.contains("zone") ? "zone"
                    : cols.contains("foret") ? "foret"
                    : cols.contains("zone_foret") ? "zone_foret" : null;

            String colDate = cols.contains("horodatage") ? "horodatage"
                    : cols.contains("date_heure") ? "date_heure"
                    : cols.contains("date") ? "date" : null;

            System.out.println("getDataSimple — colZone=" + colZone +
                    " colDate=" + colDate + " colonnes=" + cols);

            while (rs.next()) {
                DonCapteur d = new DonCapteur();
                if (cols.contains("id"))          d.setId(rs.getInt("id"));
                if (cols.contains("type"))         d.setType(rs.getString("type"));
                if (cols.contains("capteur"))      d.setCapteur(rs.getString("capteur"));
                if (cols.contains("temperature"))  d.setTemperature(rs.getDouble("temperature"));
                if (cols.contains("humidite"))     d.setHumidite(rs.getDouble("humidite"));
                if (cols.contains("fumee"))        d.setFumee(rs.getDouble("fumee"));
                if (cols.contains("risque"))       d.setRisque(rs.getString("risque"));

                String zoneVal = (colZone != null) ? rs.getString(colZone) : "";
                if (zoneVal == null) zoneVal = "";
                d.setZone(zoneVal);
                d.setForet(zoneVal);   // utilisé pour l'affichage colonne Forêt
                d.setLocalisation(""); // pas de JOIN ici

                if (colDate != null) {
                    Timestamp ts = rs.getTimestamp(colDate);
                    if (ts != null) d.setHorodatage(ts.toLocalDateTime());
                }
                liste.add(d);
            }
            System.out.println("getDataSimple : " + liste.size() + " lignes");
        } catch (SQLException e) {
            System.out.println("Erreur getDataSimple : " + e.getMessage());
            e.printStackTrace();
        }
        return liste;
    }

    // ════════════════════════════════════════════════════════
    // FILTRE — robuste, sans JOIN obligatoire
    // ════════════════════════════════════════════════════════
    public List<DonCapteur> getFiltered(LocalDateTime debut, LocalDateTime fin,
                                        String zone, String capteur, String recherche) {
        String colZone = detecterColonneZone();

        StringBuilder sql = new StringBuilder(
                "SELECT * FROM donnee_capteur WHERE horodatage BETWEEN ? AND ?");

        if (zone != null && !zone.equals("Toutes") && !zone.isEmpty())
            sql.append(" AND ").append(colZone).append(" = ?");
        if (capteur != null && !capteur.equals("Tous") && !capteur.isEmpty())
            sql.append(" AND capteur = ?");
        if (recherche != null && !recherche.trim().isEmpty())
            sql.append(" AND (capteur LIKE ? OR ").append(colZone).append(" LIKE ?)");

        sql.append(" ORDER BY horodatage DESC");

        List<DonCapteur> liste = new ArrayList<>();
        try (PreparedStatement pst = getConnection().prepareStatement(sql.toString())) {
            int idx = 1;
            pst.setTimestamp(idx++, Timestamp.valueOf(debut));
            pst.setTimestamp(idx++, Timestamp.valueOf(fin));
            if (zone != null && !zone.equals("Toutes") && !zone.isEmpty())
                pst.setString(idx++, zone);
            if (capteur != null && !capteur.equals("Tous") && !capteur.isEmpty())
                pst.setString(idx++, capteur);
            if (recherche != null && !recherche.trim().isEmpty()) {
                String like = "%" + recherche.trim() + "%";
                pst.setString(idx++, like);
                pst.setString(idx++, like);
            }

            ResultSet rs = pst.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();
            java.util.Set<String> cols = new java.util.HashSet<>();
            for (int i = 1; i <= meta.getColumnCount(); i++)
                cols.add(meta.getColumnName(i).toLowerCase());

            String colDate = cols.contains("horodatage") ? "horodatage"
                    : cols.contains("date_heure") ? "date_heure"
                    : cols.contains("date") ? "date" : null;

            while (rs.next()) {
                DonCapteur d = new DonCapteur();
                if (cols.contains("id"))         d.setId(rs.getInt("id"));
                if (cols.contains("type"))        d.setType(rs.getString("type"));
                if (cols.contains("capteur"))     d.setCapteur(rs.getString("capteur"));
                if (cols.contains("temperature")) d.setTemperature(rs.getDouble("temperature"));
                if (cols.contains("humidite"))    d.setHumidite(rs.getDouble("humidite"));
                if (cols.contains("fumee"))       d.setFumee(rs.getDouble("fumee"));
                if (cols.contains("risque"))      d.setRisque(rs.getString("risque"));
                String zoneVal = rs.getString(colZone);
                if (zoneVal == null) zoneVal = "";
                d.setZone(zoneVal);
                d.setForet(zoneVal);
                d.setLocalisation("");
                if (colDate != null) {
                    Timestamp ts = rs.getTimestamp(colDate);
                    if (ts != null) d.setHorodatage(ts.toLocalDateTime());
                }
                liste.add(d);
            }
        } catch (SQLException e) {
            System.out.println("Erreur getFiltered : " + e.getMessage());
            e.printStackTrace();
        }
        return liste;
    }

    // Surcharge sans recherche textuelle (compatibilité)
    public List<DonCapteur> getFiltered(LocalDateTime debut, LocalDateTime fin,
                                        String zone, String capteur) {
        return getFiltered(debut, fin, zone, capteur, null);
    }

    // ════════════════════════════════════════════════════════
    // Listes pour les filtres — depuis la table foret directement
    // ════════════════════════════════════════════════════════
    public List<String> getNomsForetsBD() {
        List<String> liste = new ArrayList<>();

        // ✅ DISTINCT sur la table foret
        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT DISTINCT nom FROM foret " +
                             "WHERE nom IS NOT NULL AND nom != '' " +
                             "ORDER BY nom")) {
            while (rs.next()) {
                String v = rs.getString("nom");
                if (v != null && !v.isEmpty()) liste.add(v);
            }
            if (!liste.isEmpty()) return liste;
        } catch (SQLException ignored) {}

        // Fallback : valeurs distinctes dans donnee_capteur
        String colZone = detecterColonneZone();
        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT DISTINCT " + colZone + " FROM donnee_capteur " +
                             "WHERE " + colZone + " IS NOT NULL AND " + colZone + " != '' " +
                             "ORDER BY " + colZone)) {
            while (rs.next()) {
                String v = rs.getString(1);
                if (v != null && !v.isEmpty()) liste.add(v);
            }
        } catch (SQLException e) {
            System.out.println("Erreur getNomsForetsBD : " + e.getMessage());
        }
        return liste;
    }

    public List<String> getNomsForets() {
        return getNomsForetsBD();
    }

    public List<String> getNomsCapteurs() {
        List<String> liste = new ArrayList<>();
        // Essayer d'abord la table capteur
        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT nom FROM capteur ORDER BY nom")) {
            while (rs.next()) liste.add(rs.getString("nom"));
            if (!liste.isEmpty()) return liste;
        } catch (SQLException ignored) {}

        // Fallback : valeurs distinctes dans donnee_capteur
        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT DISTINCT capteur FROM donnee_capteur " +
                     "WHERE capteur IS NOT NULL ORDER BY capteur")) {
            while (rs.next()) {
                String v = rs.getString("capteur");
                if (v != null && !v.isEmpty()) liste.add(v);
            }
        } catch (SQLException e) {
            System.out.println("Erreur getNomsCapteurs : " + e.getMessage());
        }
        return liste;
    }

    public List<String> getNomsCapteursBD() {
        return getNomsCapteurs();
    }

    // ════════════════════════════════════════════════════════
    // Contrainte doublon capteur + même minute
    // ════════════════════════════════════════════════════════
    public boolean existeDejaPourCapteurEtTemps(String capteur, LocalDateTime horodatage) {
        String sql = "SELECT COUNT(*) FROM donnee_capteur " +
                "WHERE capteur = ? " +
                "AND YEAR(horodatage)   = YEAR(?) " +
                "AND MONTH(horodatage)  = MONTH(?) " +
                "AND DAY(horodatage)    = DAY(?) " +
                "AND HOUR(horodatage)   = HOUR(?) " +
                "AND MINUTE(horodatage) = MINUTE(?)";
        try (PreparedStatement pst = getConnection().prepareStatement(sql)) {
            Timestamp ts = Timestamp.valueOf(horodatage);
            pst.setString(1, capteur);
            for (int i = 2; i <= 6; i++) pst.setTimestamp(i, ts);
            ResultSet rs = pst.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.out.println("Erreur existeDeja : " + e.getMessage());
            return false;
        }
    }

    // ════════════════════════════════════════════════════════
    // Calcul risque
    // ════════════════════════════════════════════════════════
    public static String calculerRisque(double temperature, double humidite, double fumee) {
        if (temperature > 35 || fumee > 80) return "Danger";
        if (temperature > 28 || fumee > 50 || humidite < 30) return "Attention";
        return "Sur";
    }

    // ── mapRow (gardé pour compatibilité interne) ─────────
    private DonCapteur mapRow(ResultSet rs, boolean avecForet) throws SQLException {
        DonCapteur d = new DonCapteur();
        d.setId(rs.getInt("id"));
        d.setType(rs.getString("type"));
        d.setCapteur(rs.getString("capteur"));
        try { d.setZone(rs.getString("zone")); } catch (SQLException ignored) {}
        d.setTemperature(rs.getDouble("temperature"));
        d.setHumidite(rs.getDouble("humidite"));
        d.setFumee(rs.getDouble("fumee"));
        try {
            Timestamp ts = rs.getTimestamp("horodatage");
            if (ts != null) d.setHorodatage(ts.toLocalDateTime());
        } catch (SQLException ignored) {}
        try { d.setRisque(rs.getString("risque")); } catch (SQLException ignored) {}
        if (avecForet) {
            try { d.setForet(rs.getString("foret_nom")); } catch (SQLException ignored) {
                d.setForet(d.getZone());
            }
            try { d.setLocalisation(rs.getString("localisation")); } catch (SQLException ignored) {}
        }
        return d;
    }
}
