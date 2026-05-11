package ForestGuard.services;

import ForestGuard.entities.Anomalie;
import ForestGuard.entities.DonCapteur;
import ForestGuard.utils.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AnomalieService {

    private final Connection conn = MyConnection.getInstance().getCnx();

    public void detecterEtSauvegarder(List<DonCapteur> donnees) {
        for (DonCapteur d : donnees) {
            if (d.getTemperature() > 30) {
                sauvegarderSiNouveau(new Anomalie(
                        LocalDateTime.now(), d.getCapteur(), d.getZone(),
                        "Temperature elevee", d.getTemperature(), 30, false, d.getId()));
            }
            if (d.getHumidite() < 40) {
                sauvegarderSiNouveau(new Anomalie(
                        LocalDateTime.now(), d.getCapteur(), d.getZone(),
                        "Humidite faible", d.getHumidite(), 40, false, d.getId()));
            }
            if (d.getFumee() > 0) {
                sauvegarderSiNouveau(new Anomalie(
                        LocalDateTime.now(), d.getCapteur(), d.getZone(),
                        "Fumee detectee", d.getFumee(), 0, false, d.getId()));
            }
        }
    }

    private void sauvegarderSiNouveau(Anomalie a) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM anomalie WHERE donnee_id=? AND type_anomalie=?")) {
            ps.setInt(1, a.getDonneeId());
            ps.setString(2, a.getTypeAnomalie());
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getInt(1) == 0) {
                ajouter(a);
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void ajouter(Anomalie a) {
        String sql = "INSERT INTO anomalie " +
                "(date_detection,capteur,foret,type_anomalie," +
                "valeur_detectee,seuil_depasse,traite,donnee_id) " +
                "VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(a.getDateDetection()));
            ps.setString(2, a.getCapteur());
            ps.setString(3, a.getForet());
            ps.setString(4, a.getTypeAnomalie());
            ps.setDouble(5, a.getValeurDetectee());
            ps.setDouble(6, a.getSeuilDepasse());
            ps.setBoolean(7, a.isTraite());
            ps.setInt(8, a.getDonneeId());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void marquerTraite(int id) {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE anomalie SET traite=TRUE WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public List<Anomalie> getAll() {
        List<Anomalie> liste = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT * FROM anomalie ORDER BY date_detection DESC")) {
            while (rs.next()) {
                Anomalie a = new Anomalie();
                a.setId(rs.getInt("id"));
                a.setDateDetection(rs.getTimestamp("date_detection").toLocalDateTime());
                a.setCapteur(rs.getString("capteur"));
                a.setForet(rs.getString("foret"));
                a.setTypeAnomalie(rs.getString("type_anomalie"));
                a.setValeurDetectee(rs.getDouble("valeur_detectee"));
                a.setSeuilDepasse(rs.getDouble("seuil_depasse"));
                a.setTraite(rs.getBoolean("traite"));
                a.setDonneeId(rs.getInt("donnee_id"));
                liste.add(a);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return liste;
    }

    public long getNombreNonTraites() {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT COUNT(*) FROM anomalie WHERE traite=FALSE")) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }
}