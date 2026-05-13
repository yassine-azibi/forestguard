package dao;

import model.Alerte;
import utils.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AlerteDAO {

    private final Connection cnx;

    public AlerteDAO() {
        cnx = MyConnection.getInstance().getCnx();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CREATE
    // ══════════════════════════════════════════════════════════════════════════

    public boolean create(Alerte a) {
        if (cnx == null) return false;
        String sql = "INSERT INTO alerte (type_alerte, niveau, localisation, statut, source) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, a.getTypeAlerte());
            ps.setString(2, a.getNiveau());
            ps.setString(3, a.getLocalisation());
            ps.setString(4, a.getStatut() != null ? a.getStatut() : "Nouvelle");
            ps.setString(5, a.getSource()  != null ? a.getSource()  : "Manuelle");
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Erreur create : " + e.getMessage());
            return false;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  READ
    // ══════════════════════════════════════════════════════════════════════════

    public List<Alerte> getAll() {
        List<Alerte> list = new ArrayList<>();
        if (cnx == null) return list;
        String sql = "SELECT * FROM alerte ORDER BY date_alerte DESC";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.out.println("Erreur getAll : " + e.getMessage());
        }
        return list;
    }

    public List<Alerte> getByStatut(String statut) {
        List<Alerte> list = new ArrayList<>();
        if (cnx == null) return list;
        String sql = "SELECT * FROM alerte WHERE statut=? ORDER BY date_alerte DESC";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, statut);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.out.println("Erreur getByStatut : " + e.getMessage());
        }
        return list;
    }

    public Alerte getById(int id) {
        if (cnx == null) return null;
        String sql = "SELECT * FROM alerte WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.out.println("Erreur getById : " + e.getMessage());
        }
        return null;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  UPDATE COMPLET — modifie type, niveau, localisation ET statut
    // ══════════════════════════════════════════════════════════════════════════

    public boolean update(Alerte a) {
        if (cnx == null) return false;
        String sql = "UPDATE alerte SET type_alerte=?, niveau=?, localisation=?, statut=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, a.getTypeAlerte());
            ps.setString(2, a.getNiveau());
            ps.setString(3, a.getLocalisation());
            ps.setString(4, a.getStatut());
            ps.setInt   (5, a.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Erreur update : " + e.getMessage());
            return false;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  UPDATE STATUT SEULEMENT — utilisé par Valider / Rejeter rapide
    // ══════════════════════════════════════════════════════════════════════════

    public boolean updateStatut(int id, String statut) {
        if (cnx == null) return false;
        String sql = "UPDATE alerte SET statut=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, statut);
            ps.setInt   (2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Erreur updateStatut : " + e.getMessage());
            return false;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  DELETE
    // ══════════════════════════════════════════════════════════════════════════

    public boolean delete(int id) {
        if (cnx == null) return false;
        String sql = "DELETE FROM alerte WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Erreur delete : " + e.getMessage());
            return false;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  STATS — comptages pour les cartes
    // ══════════════════════════════════════════════════════════════════════════

    public int countAll() {
        if (cnx == null) return 0;
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM alerte")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { System.out.println(e.getMessage()); }
        return 0;
    }

    public int countByStatut(String statut) {
        if (cnx == null) return 0;
        String sql = "SELECT COUNT(*) FROM alerte WHERE statut=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, statut);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { System.out.println(e.getMessage()); }
        return 0;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  MÉTHODES SUPPLÉMENTAIRES pour l'intégration des modules
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Récupère toutes les alertes pour un ComboBox (format: "ID - Type - Niveau")
     */
    public List<String> getAllForComboBox() {
        List<String> list = new ArrayList<>();
        if (cnx == null) return list;
        String sql = "SELECT id, type_alerte, niveau FROM alerte ORDER BY date_alerte DESC";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String item = rs.getInt("id") + " - " + 
                             rs.getString("type_alerte") + " - " + 
                             rs.getString("niveau");
                list.add(item);
            }
        } catch (SQLException e) {
            System.out.println("Erreur getAllForComboBox : " + e.getMessage());
        }
        return list;
    }

    /**
     * Récupère la localisation d'une alerte par son ID
     */
    public String getLocalisationById(int id) {
        if (cnx == null) return null;
        String sql = "SELECT localisation FROM alerte WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("localisation");
        } catch (SQLException e) {
            System.out.println("Erreur getLocalisationById : " + e.getMessage());
        }
        return null;
    }

    /**
     * Récupère le niveau et le type d'une alerte par son ID
     */
    public String getNiveauEtType(int id) {
        if (cnx == null) return null;
        String sql = "SELECT niveau, type_alerte FROM alerte WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("niveau") + " - " + rs.getString("type_alerte");
            }
        } catch (SQLException e) {
            System.out.println("Erreur getNiveauEtType : " + e.getMessage());
        }
        return null;
    }

    /**
     * Récupère les nouvelles alertes (statut = "Nouvelle")
     */
    public List<Alerte> getNouvellesAlertes() {
        return getByStatut("Nouvelle");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  MAPPING privé : convertit une ligne SQL en objet Alerte
    // ══════════════════════════════════════════════════════════════════════════

    private Alerte mapRow(ResultSet rs) throws SQLException {
        Alerte a = new Alerte();
        a.setId(rs.getInt("id"));
        a.setTypeAlerte(rs.getString("type_alerte"));
        a.setNiveau(rs.getString("niveau"));
        a.setLocalisation(rs.getString("localisation"));
        a.setStatut(rs.getString("statut"));
        a.setSource(rs.getString("source"));
        Timestamp ts = rs.getTimestamp("date_alerte");
        if (ts != null) a.setDateAlerte(ts.toLocalDateTime());
        return a;
    }
}
