package ForestGuard.services;

import ForestGuard.entities.JournalAction;
import ForestGuard.utils.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class JournalActionService {

    private final Connection conn = MyConnection.getInstance().getCnx();

    public void enregistrer(String utilisateur, String action,
                            String details, int donneeId) {
        String sql = "INSERT INTO journal_action " +
                "(date_action,utilisateur,action,details,donnee_id) " +
                "VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(2, utilisateur);
            ps.setString(3, action);
            ps.setString(4, details);
            ps.setInt(5, donneeId);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public List<JournalAction> getAll() {
        List<JournalAction> liste = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT * FROM journal_action ORDER BY date_action DESC")) {
            while (rs.next()) {
                JournalAction j = new JournalAction();
                j.setId(rs.getInt("id"));
                j.setDateAction(rs.getTimestamp("date_action").toLocalDateTime());
                j.setUtilisateur(rs.getString("utilisateur"));
                j.setAction(rs.getString("action"));
                j.setDetails(rs.getString("details"));
                j.setDonneeId(rs.getInt("donnee_id"));
                liste.add(j);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return liste;
    }
}