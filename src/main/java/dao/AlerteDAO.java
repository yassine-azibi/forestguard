package dao;

import utils.MyConnection;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class AlerteDAO {

    private Connection cnx;

    public AlerteDAO() {
        cnx = MyConnection.getInstance().getCnx();
    }

    // ── GET toutes les alertes pour ComboBox ──────────────────────────────────
    // Retourne Map<id, "localisation - type_alerte (niveau)">
    public Map<Integer, String> getAllForComboBox() {
        Map<Integer, String> map = new LinkedHashMap<>();
        String sql = "SELECT id, type_alerte, niveau, localisation FROM alerte "
                + "WHERE statut = 'Nouvelle' ORDER BY date_alerte DESC";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                int    id     = rs.getInt("id");
                String label  = rs.getString("localisation")
                        + " — " + rs.getString("type_alerte")
                        + " (" + rs.getString("niveau") + ")";
                map.put(id, label);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return map;
    }

    // ── GET localisation par id ───────────────────────────────────────────────
    public String getLocalisationById(int id) {
        String sql = "SELECT localisation FROM alerte WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("localisation");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return "";
    }
    // ── GET niveau et type par id ─────────────────────────────────────────────
    public String[] getNiveauEtType(int alerteId) {
        String[] result = {"", ""};
        String sql = "SELECT niveau, type_alerte FROM alerte WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, alerteId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                result[0] = rs.getString("niveau");
                result[1] = rs.getString("type_alerte");
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return result;
    }

    // ── GET nouvelles alertes (5 dernières minutes) ───────────────────────────
    /**
     * Retourne toutes les alertes créées dans les 5 dernières minutes
     * avec le statut 'Nouvelle', ordonnées par date décroissante
     * 
     * @return Liste de Map contenant : id, type_alerte, niveau, localisation, date_alerte
     */
    public java.util.List<java.util.Map<String, Object>> getNouvellesAlertes() {
        java.util.List<java.util.Map<String, Object>> alertes = new java.util.ArrayList<>();
        
        // Alertes des 5 dernières minutes avec statut 'Nouvelle'
        String sql = "SELECT id, type_alerte, niveau, localisation, date_alerte " +
                     "FROM alerte " +
                     "WHERE statut = 'Nouvelle' " +
                     "AND date_alerte >= DATE_SUB(NOW(), INTERVAL 5 MINUTE) " +
                     "ORDER BY date_alerte DESC";
        
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            
            while (rs.next()) {
                java.util.Map<String, Object> alerte = new java.util.HashMap<>();
                alerte.put("id", rs.getInt("id"));
                alerte.put("type_alerte", rs.getString("type_alerte"));
                alerte.put("niveau", rs.getString("niveau"));
                alerte.put("localisation", rs.getString("localisation"));
                alerte.put("date_alerte", rs.getTimestamp("date_alerte"));
                alertes.add(alerte);
            }
        } catch (SQLException e) {
            System.out.println("Erreur getNouvellesAlertes: " + e.getMessage());
        }
        
        return alertes;
    }
}