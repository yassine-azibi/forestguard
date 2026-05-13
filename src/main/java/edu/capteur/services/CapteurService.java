package edu.capteur.services;

import edu.capteur.entities.Capteur;
import edu.capteur.interfaces.IService;
import edu.capteur.utils.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CapteurService implements IService<Capteur> {

    private Connection connection;

    public CapteurService() {
        connection = MyConnection.getInstance().getConnection();
    }

    // ✅ Ajouter un capteur (avec foret_id)
    @Override
    public void ajouter(Capteur capteur) {
        String sql = "INSERT INTO capteur (nom, type, localisation, statut, foret_id) VALUES (?, ?, ?, ?, ?)";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, capteur.getNom());
            ps.setString(2, capteur.getType());
            ps.setString(3, capteur.getLocalisation());
            ps.setString(4, capteur.getStatut());
            ps.setInt(5, capteur.getForetId());
            ps.executeUpdate();
            System.out.println("✅ Capteur ajouté avec succès !");
        } catch (SQLException e) {
            System.out.println("❌ Erreur ajout capteur : " + e.getMessage());
        }
    }

    // ✅ Modifier un capteur (avec foret_id)
    @Override
    public void modifier(Capteur capteur) {
        String sql = "UPDATE capteur SET nom=?, type=?, localisation=?, statut=?, foret_id=? WHERE id=?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, capteur.getNom());
            ps.setString(2, capteur.getType());
            ps.setString(3, capteur.getLocalisation());
            ps.setString(4, capteur.getStatut());
            ps.setInt(5, capteur.getForetId());
            ps.setInt(6, capteur.getId());
            ps.executeUpdate();
            System.out.println("✅ Capteur modifié avec succès !");
        } catch (SQLException e) {
            System.out.println("❌ Erreur modification capteur : " + e.getMessage());
        }
    }

    // ✅ Supprimer un capteur
    @Override
    public void supprimer(int id) {
        String sql = "DELETE FROM capteur WHERE id=?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("✅ Capteur supprimé avec succès !");
        } catch (SQLException e) {
            System.out.println("❌ Erreur suppression capteur : " + e.getMessage());
        }
    }

    // ✅ Afficher tous les capteurs (avec foret_id)
    @Override
    public List<Capteur> afficher() {
        List<Capteur> capteurs = new ArrayList<>();
        String sql = "SELECT * FROM capteur";
        try {
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                Capteur c = new Capteur(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getString("type"),
                        rs.getString("localisation"),
                        rs.getString("statut"),
                        rs.getInt("foret_id")
                );
                capteurs.add(c);
            }
        } catch (SQLException e) {
            System.out.println("❌ Erreur affichage capteurs : " + e.getMessage());
        }
        return capteurs;
    }
}
