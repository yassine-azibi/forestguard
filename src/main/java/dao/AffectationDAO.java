package dao;

import model.Affectation;
import utils.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO pour gérer les affectations de pompiers aux alertes
 */
public class AffectationDAO {
    
    private Connection cnx;
    
    public AffectationDAO() {
        cnx = MyConnection.getInstance().getCnx();
    }
    
    /**
     * Récupère toutes les affectations en cours pour un pompier
     * @param idPompier ID du pompier
     * @return Liste des affectations avec statut 'en_cours'
     */
    public List<Affectation> getAffectationsEnCours(int idPompier) {
        List<Affectation> affectations = new ArrayList<>();
        
        String sql = "SELECT a.*, al.type_alerte, al.niveau, al.localisation " +
                     "FROM affectation a " +
                     "INNER JOIN alerte al ON a.id_alerte = al.id " +
                     "WHERE a.id_pompier = ? AND a.statut = 'en_cours' " +
                     "ORDER BY a.date_affectation DESC";
        
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idPompier);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                Affectation affectation = new Affectation();
                affectation.setId(rs.getInt("id"));
                affectation.setIdAlerte(rs.getInt("id_alerte"));
                affectation.setIdPompier(rs.getInt("id_pompier"));
                
                Timestamp ts = rs.getTimestamp("date_affectation");
                if (ts != null) {
                    affectation.setDateAffectation(ts.toLocalDateTime());
                }
                
                affectation.setStatut(rs.getString("statut"));
                
                Double score = rs.getDouble("score_selection");
                if (!rs.wasNull()) {
                    affectation.setScoreSelection(score);
                }
                
                Double distance = rs.getDouble("distance_km");
                if (!rs.wasNull()) {
                    affectation.setDistanceKm(distance);
                }
                
                affectation.setModeAffectation(rs.getString("mode_affectation"));
                
                // Informations de l'alerte
                affectation.setTypeAlerte(rs.getString("type_alerte"));
                affectation.setNiveauAlerte(rs.getString("niveau"));
                affectation.setLocalisationAlerte(rs.getString("localisation"));
                
                affectations.add(affectation);
            }
        } catch (SQLException e) {
            System.out.println("Erreur getAffectationsEnCours: " + e.getMessage());
            e.printStackTrace();
        }
        
        return affectations;
    }
    
    /**
     * Refuse une affectation en changeant son statut à 'annule'
     * @param idAffectation ID de l'affectation
     * @return true si la mise à jour a réussi
     */
    public boolean refuserAffectation(int idAffectation) {
        String sql = "UPDATE affectation SET statut = 'annule' WHERE id = ?";
        
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idAffectation);
            int rows = ps.executeUpdate();
            
            if (rows > 0) {
                System.out.println("✅ Affectation " + idAffectation + " refusée (statut = 'annule')");
                return true;
            }
        } catch (SQLException e) {
            System.out.println("Erreur refuserAffectation: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Récupère les informations d'une alerte par son ID
     * @param idAlerte ID de l'alerte
     * @return Tableau [type_alerte, niveau, localisation] ou null
     */
    public String[] getAlerteById(int idAlerte) {
        String sql = "SELECT type_alerte, niveau, localisation FROM alerte WHERE id = ?";
        
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idAlerte);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                return new String[] {
                    rs.getString("type_alerte"),
                    rs.getString("niveau"),
                    rs.getString("localisation")
                };
            }
        } catch (SQLException e) {
            System.out.println("Erreur getAlerteById: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Récupère une affectation par son ID
     * @param idAffectation ID de l'affectation
     * @return Affectation ou null
     */
    public Affectation getAffectationById(int idAffectation) {
        String sql = "SELECT a.*, al.type_alerte, al.niveau, al.localisation " +
                     "FROM affectation a " +
                     "INNER JOIN alerte al ON a.id_alerte = al.id " +
                     "WHERE a.id = ?";
        
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idAffectation);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                Affectation affectation = new Affectation();
                affectation.setId(rs.getInt("id"));
                affectation.setIdAlerte(rs.getInt("id_alerte"));
                affectation.setIdPompier(rs.getInt("id_pompier"));
                
                Timestamp ts = rs.getTimestamp("date_affectation");
                if (ts != null) {
                    affectation.setDateAffectation(ts.toLocalDateTime());
                }
                
                affectation.setStatut(rs.getString("statut"));
                
                Double score = rs.getDouble("score_selection");
                if (!rs.wasNull()) {
                    affectation.setScoreSelection(score);
                }
                
                Double distance = rs.getDouble("distance_km");
                if (!rs.wasNull()) {
                    affectation.setDistanceKm(distance);
                }
                
                affectation.setModeAffectation(rs.getString("mode_affectation"));
                
                // Informations de l'alerte
                affectation.setTypeAlerte(rs.getString("type_alerte"));
                affectation.setNiveauAlerte(rs.getString("niveau"));
                affectation.setLocalisationAlerte(rs.getString("localisation"));
                
                return affectation;
            }
        } catch (SQLException e) {
            System.out.println("Erreur getAffectationById: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Compte le nombre d'affectations en cours pour un pompier
     * @param idPompier ID du pompier
     * @return Nombre d'affectations en cours
     */
    public int countAffectationsEnCours(int idPompier) {
        String sql = "SELECT COUNT(*) FROM affectation WHERE id_pompier = ? AND statut = 'en_cours'";
        
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idPompier);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.out.println("Erreur countAffectationsEnCours: " + e.getMessage());
        }
        
        return 0;
    }
}
