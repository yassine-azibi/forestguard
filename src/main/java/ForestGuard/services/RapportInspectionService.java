package ForestGuard.services;

import ForestGuard.entities.RapportInspection;
import ForestGuard.utils.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RapportInspectionService {

    private final Connection conn = MyConnection.getInstance().getCnx();

    public void ajouter(RapportInspection r) {
        String sql = "INSERT INTO rapport_inspection " +
                "(date_rapport,foret,responsable,statut," +
                "niveau_risque,observations,donnee_id) " +
                "VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(r.getDateRapport()));
            ps.setString(2, r.getForet());
            ps.setString(3, r.getResponsable());
            ps.setString(4, r.getStatut());
            ps.setString(5, r.getNiveauRisque());
            ps.setString(6, r.getObservations());
            ps.setInt(7, r.getDonneeId());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void modifier(RapportInspection r) {
        String sql = "UPDATE rapport_inspection SET statut=?, " +
                "niveau_risque=?, observations=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, r.getStatut());
            ps.setString(2, r.getNiveauRisque());
            ps.setString(3, r.getObservations());
            ps.setInt(4, r.getId());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void supprimer(int id) {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM rapport_inspection WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public List<RapportInspection> getAll() {
        List<RapportInspection> liste = new ArrayList<>();
        String sql = "SELECT * FROM rapport_inspection ORDER BY date_rapport DESC";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                RapportInspection r = new RapportInspection();
                r.setId(rs.getInt("id"));
                r.setDateRapport(rs.getTimestamp("date_rapport").toLocalDateTime());
                r.setForet(rs.getString("foret"));
                r.setResponsable(rs.getString("responsable"));
                r.setStatut(rs.getString("statut"));
                r.setNiveauRisque(rs.getString("niveau_risque"));
                r.setObservations(rs.getString("observations"));
                r.setDonneeId(rs.getInt("donnee_id"));
                liste.add(r);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return liste;
    }
}