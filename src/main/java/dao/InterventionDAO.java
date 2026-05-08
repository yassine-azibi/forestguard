package dao;

import model.Intervention;
import utils.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
public class InterventionDAO {

    private Connection cnx;

    public InterventionDAO() {
        cnx = MyConnection.getInstance().getCnx();
    }

    // ── CREATE ────────────────────────────────────────────────────────────────
    public boolean create(Intervention i) {
        String sql = "INSERT INTO intervention (alert_zone, statut, start_date, "
                + "end_date, agent_name, resultat, alerte_id, alerte_localisation) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, i.getAlertZone());
            ps.setString(2, i.getStatut());
            ps.setTimestamp(3, Timestamp.valueOf(i.getStartDate()));
            ps.setTimestamp(4, i.getEndDate() != null
                    ? Timestamp.valueOf(i.getEndDate()) : null);
            ps.setString(5, i.getAgentName());
            ps.setString(6, i.getResultat());
            ps.setInt(7, i.getAlerteId());
            ps.setString(8, i.getAlerteLocalisation());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    // ── READ ALL ──────────────────────────────────────────────────────────────
    public List<Intervention> getAll() {
        List<Intervention> list = new ArrayList<>();
        String sql = "SELECT * FROM intervention ORDER BY start_date DESC";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return list;
    }

    // ── READ BY AGENT ─────────────────────────────────────────────────────────
    public List<Intervention> getByAgent(String agentName) {
        List<Intervention> list = new ArrayList<>();
        String sql = "SELECT * FROM intervention WHERE agent_name = ? ORDER BY start_date DESC";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, agentName);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return list;
    }

    // ── MARK AS COMPLETED ─────────────────────────────────────────────────────
    public boolean markAsCompleted(String alertZone, String agentName) {
        String sql = "UPDATE intervention SET statut = 'Completed', end_date = NOW() "
                + "WHERE alert_zone = ? AND agent_name = ? AND statut = 'In Progress'";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, alertZone);
            ps.setString(2, agentName);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    // ── UPDATE COMPLET ────────────────────────────────────────────────────────
    public boolean update(Intervention i) {
        String sql = "UPDATE intervention SET statut=?, end_date=?, resultat=?, "
                + "alerte_id=?, alerte_localisation=? "
                + "WHERE alert_zone=? AND agent_name=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, i.getStatut());
            ps.setTimestamp(2, i.getEndDate() != null
                    ? Timestamp.valueOf(i.getEndDate()) : null);
            ps.setString(3, i.getResultat());
            ps.setInt(4, i.getAlerteId());
            ps.setString(5, i.getAlerteLocalisation());
            ps.setString(6, i.getAlertZone());
            ps.setString(7, i.getAgentName());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    // ── DELETE ────────────────────────────────────────────────────────────────
    public boolean delete(String alertZone, String agentName) {
        String sql = "DELETE FROM intervention WHERE alert_zone = ? AND agent_name = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, alertZone);
            ps.setString(2, agentName);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    // ── STATS ─────────────────────────────────────────────────────────────────
    public int countByStatut(String agentName, String statut) {
        String sql = "SELECT COUNT(*) FROM intervention WHERE agent_name = ? AND statut = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, agentName);
            ps.setString(2, statut);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return 0;
    }

    public int countThisWeek(String agentName) {
        String sql = "SELECT COUNT(*) FROM intervention WHERE agent_name = ? "
                + "AND YEARWEEK(start_date, 1) = YEARWEEK(NOW(), 1)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, agentName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return 0;
    }
    // ── STATS PAR JOUR ────────────────────────────────────────────────────────
    public Map<String, Long> countByDay(String agentName) {
        Map<String, Long> map = new java.util.LinkedHashMap<>();
        String sql = "SELECT DATE(start_date) as jour, COUNT(*) as total "
                + "FROM intervention WHERE agent_name = ? "
                + "GROUP BY DATE(start_date) ORDER BY jour ASC LIMIT 14";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, agentName);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                map.put(rs.getString("jour"), rs.getLong("total"));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return map;
    }
    
    // ── VÉRIFIER SI INTERVENTION EXISTE POUR UNE ALERTE ──────────────────────
    /**
     * Vérifie si une intervention EN COURS existe déjà pour une alerte donnée
     * Empêche la création de doublons (une seule intervention active par alerte)
     * Les interventions terminées (Completed) ne bloquent pas la création
     * @param alerteId L'ID de l'alerte
     * @return true si une intervention EN COURS existe déjà, false sinon
     */
    public boolean existeInterventionPourAlerte(int alerteId) {
        String sql = "SELECT COUNT(*) FROM intervention WHERE alerte_id = ? AND statut = 'In Progress'";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, alerteId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int count = rs.getInt(1);
                return count > 0;
            }
        } catch (SQLException e) {
            System.out.println("❌ Erreur vérification intervention: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Récupère l'intervention EN COURS pour une alerte donnée
     * @param alerteId L'ID de l'alerte
     * @return L'intervention EN COURS ou null si aucune
     */
    public Intervention getInterventionParAlerte(int alerteId) {
        String sql = "SELECT * FROM intervention WHERE alerte_id = ? AND statut = 'In Progress' LIMIT 1";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, alerteId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (SQLException e) {
            System.out.println("❌ Erreur récupération intervention: " + e.getMessage());
        }
        return null;
    }
    // ── MAPPING ───────────────────────────────────────────────────────────────
    private Intervention mapRow(ResultSet rs) throws SQLException {
        Intervention i = new Intervention();
        i.setAlertZone(rs.getString("alert_zone"));
        i.setStatut(rs.getString("statut"));
        Timestamp start = rs.getTimestamp("start_date");
        if (start != null) i.setStartDate(start.toLocalDateTime());
        Timestamp end = rs.getTimestamp("end_date");
        if (end != null) i.setEndDate(end.toLocalDateTime());
        i.setAgentName(rs.getString("agent_name"));
        i.setResultat(rs.getString("resultat"));
        i.setAlerteId(rs.getInt("alerte_id"));
        i.setAlerteLocalisation(rs.getString("alerte_localisation"));
        return i;
    }
}