package edu.capteur.services;

import edu.capteur.utils.MyConnection;

import java.sql.*;

/**
 * ForestGuard – Service de lecture des mesures capteur depuis la BD.
 * Lit la dernière mesure (température + humidité) pour un capteur donné.
 */
public class MesureCapteurService {

    private final Connection connection;

    public MesureCapteurService() {
        this.connection = MyConnection.getInstance().getConnection();
    }

    /**
     * Dernière mesure enregistrée pour un capteur.
     */
    public static class DerniereMesure {
        public final double  temperature;
        public final int     humidite;
        public final String  dateMesure;
        public final boolean disponible; // false si aucune mesure en BD

        public DerniereMesure(double temperature, int humidite, String dateMesure) {
            this.temperature = temperature;
            this.humidite    = humidite;
            this.dateMesure  = dateMesure;
            this.disponible  = true;
        }

        /** Constructeur fallback — aucune mesure disponible */
        public DerniereMesure() {
            this.temperature = -1;
            this.humidite    = -1;
            this.dateMesure  = null;
            this.disponible  = false;
        }
    }

    /**
     * Retourne la dernière mesure enregistrée pour un capteur.
     * Retourne un fallback si aucune mesure n'existe.
     */
    public DerniereMesure getDerniereMesure(int capteurId) {
        String sql = "SELECT temperature, humidite, date_mesure " +
                     "FROM mesure_capteur " +
                     "WHERE capteur_id = ? " +
                     "ORDER BY date_mesure DESC LIMIT 1";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, capteurId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                double temp    = rs.getDouble("temperature");
                int    humid   = rs.getInt("humidite");
                String date    = rs.getString("date_mesure");
                System.out.println("✅ Mesure BD capteur #" + capteurId +
                    " → temp=" + temp + "°C, humidite=" + humid + "%");
                return new DerniereMesure(temp, humid, date);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur lecture mesure capteur #" + capteurId + ": " + e.getMessage());
        }
        System.out.println("⚠️ Aucune mesure en BD pour capteur #" + capteurId + " → météo simulée");
        return new DerniereMesure();
    }

    /**
     * Charge toutes les dernières mesures en UNE SEULE requête SQL.
     * Beaucoup plus performant que getDerniereMesure() appelé N fois.
     * Retourne une Map<capteurId, DerniereMesure>.
     */
    public java.util.Map<Integer, DerniereMesure> getToutesDernieresMesures() {
        java.util.Map<Integer, DerniereMesure> result = new java.util.HashMap<>();
        String sql =
            "SELECT m.capteur_id, m.temperature, m.humidite, m.date_mesure " +
            "FROM mesure_capteur m " +
            "INNER JOIN (" +
            "  SELECT capteur_id, MAX(date_mesure) AS max_date " +
            "  FROM mesure_capteur GROUP BY capteur_id" +
            ") latest ON m.capteur_id = latest.capteur_id AND m.date_mesure = latest.max_date";
        try {
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                int    id    = rs.getInt("capteur_id");
                double temp  = rs.getDouble("temperature");
                int    humid = rs.getInt("humidite");
                String date  = rs.getString("date_mesure");
                result.put(id, new DerniereMesure(temp, humid, date));
            }
            System.out.println("✅ Mesures chargées en batch : " + result.size() + " capteur(s)");
        } catch (SQLException e) {
            System.err.println("❌ Erreur chargement batch mesures : " + e.getMessage());
        }
        return result;
    }
}
