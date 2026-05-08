package dao;

import model.Equipement;
import utils.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EquipementDAO {

    private Connection cnx;

    public EquipementDAO() {
        cnx = MyConnection.getInstance().getCnx();
    }

    // ── GET ALL équipements ───────────────────────────────────────────────────
    public List<Equipement> getAll() {
        List<Equipement> list = new ArrayList<>();
        String sql = "SELECT * FROM equipement ORDER BY type, nom";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Equipement(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getString("type")
                ));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return list;
    }

    // ── Ajouter équipements d'une intervention ────────────────────────────────
    public boolean addEquipements(String alertZone, String agentName,
                                  java.time.LocalDateTime startDate,
                                  List<Equipement> equipements) {
        String sql = "INSERT INTO intervention_equipement "
                + "(alert_zone, agent_name, start_date, equipement_id) "
                + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            for (Equipement eq : equipements) {
                ps.setString(1, alertZone);
                ps.setString(2, agentName);
                ps.setTimestamp(3, Timestamp.valueOf(startDate));
                ps.setInt(4, eq.getId());
                ps.addBatch();
            }
            ps.executeBatch();
            return true;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    // ── GET équipements d'une intervention ────────────────────────────────────
    public List<Equipement> getByIntervention(String alertZone, String agentName) {
        List<Equipement> list = new ArrayList<>();
        String sql = "SELECT e.* FROM equipement e "
                + "JOIN intervention_equipement ie ON e.id = ie.equipement_id "
                + "WHERE ie.alert_zone = ? AND ie.agent_name = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, alertZone);
            ps.setString(2, agentName);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Equipement(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getString("type")
                ));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return list;
    }

    // ── Supprimer équipements d'une intervention ──────────────────────────────
    public boolean deleteByIntervention(String alertZone, String agentName) {
        String sql = "DELETE FROM intervention_equipement "
                + "WHERE alert_zone = ? AND agent_name = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, alertZone);
            ps.setString(2, agentName);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }
}