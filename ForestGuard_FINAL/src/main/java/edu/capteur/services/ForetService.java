package edu.capteur.services;

import edu.capteur.entities.Foret;
import edu.capteur.utils.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ForetService {

    private final Connection connection;

    public ForetService() {
        connection = MyConnection.getInstance().getConnection();
    }

    // ✅ Ajouter une forêt seulement si elle n'existe pas déjà (par nom)
    public void ajouterSiAbsent(Foret foret) {
        String check = "SELECT COUNT(*) FROM foret WHERE nom = ?";
        String insert = "INSERT INTO foret (nom, localisation, superficie, type_vegetation, niveau_risque, date_creation, latitude, longitude) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement ps = connection.prepareStatement(check);
            ps.setString(1, foret.getNom());
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getInt(1) == 0) {
                PreparedStatement ins = connection.prepareStatement(insert);
                ins.setString(1, foret.getNom());
                ins.setString(2, foret.getLocalisation());
                ins.setDouble(3, foret.getSuperficie());
                ins.setString(4, foret.getTypeVegetation());
                ins.setString(5, foret.getNiveauRisque());
                ins.setString(6, foret.getDateCreation());
                ins.setDouble(7, foret.getLatitude());
                ins.setDouble(8, foret.getLongitude());
                ins.executeUpdate();
                System.out.println("✅ Forêt ajoutée : " + foret.getNom());
            } else {
                System.out.println("ℹ️ Déjà existante : " + foret.getNom());
            }
        } catch (SQLException e) {
            System.out.println("❌ Erreur ajout forêt : " + e.getMessage());
        }
    }

    // ✅ Récupérer toutes les forêts
    public List<Foret> afficher() {
        List<Foret> forets = new ArrayList<>();
        String sql = "SELECT * FROM foret ORDER BY nom";
        try {
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                forets.add(new Foret(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getString("localisation"),
                        rs.getDouble("superficie"),
                        rs.getString("type_vegetation"),
                        rs.getString("niveau_risque"),
                        rs.getString("date_creation"),
                        rs.getDouble("latitude"),   // 🆕
                        rs.getDouble("longitude")   // 🆕
                ));
            }
        } catch (SQLException e) {
            System.out.println("❌ Erreur lecture forêts : " + e.getMessage());
        }
        return forets;
    }

    // ✅ Récupérer une forêt par son id
    public Foret getById(int id) {
        String sql = "SELECT * FROM foret WHERE id = ?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Foret(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getString("localisation"),
                        rs.getDouble("superficie"),
                        rs.getString("type_vegetation"),
                        rs.getString("niveau_risque"),
                        rs.getString("date_creation"),
                        rs.getDouble("latitude"),   // 🆕
                        rs.getDouble("longitude")   // 🆕
                );
            }
        } catch (SQLException e) {
            System.out.println("❌ Erreur lecture forêt #" + id + " : " + e.getMessage());
        }
        return null;
    }

    // ✅ Charger toutes les forêts en une Map<id, Foret> pour accès rapide
    public java.util.Map<Integer, Foret> getToutesForetsMap() {
        java.util.Map<Integer, Foret> map = new java.util.HashMap<>();
        for (Foret f : afficher()) {
            map.put(f.getId(), f);
        }
        return map;
    }
}
