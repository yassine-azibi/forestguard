package edu.capteur.services;

import edu.capteur.entities.Maintenance;
import edu.capteur.interfaces.IService;
import edu.capteur.utils.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MaintenanceService implements IService<Maintenance> {

    private Connection connection;

    public MaintenanceService() {
        connection = MyConnection.getInstance().getConnection();
    }

    // ✅ Ajouter une maintenance
    @Override
    public void ajouter(Maintenance maintenance) {
        String sql = "INSERT INTO maintenance (date_maintenance, type_maintenance, statut, description, capteur_id) VALUES (?, ?, ?, ?, ?)";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, maintenance.getDateMaintenance());
            ps.setString(2, maintenance.getTypeMaintenance());
            ps.setString(3, maintenance.getStatut());
            ps.setString(4, maintenance.getDescription());
            ps.setInt(5, maintenance.getCapteurId());
            ps.executeUpdate();
            System.out.println("✅ Maintenance ajoutée avec succès !");
        } catch (SQLException e) {
            System.out.println("❌ Erreur ajout maintenance : " + e.getMessage());
        }
    }

    // ✅ Modifier une maintenance
    @Override
    public void modifier(Maintenance maintenance) {
        String sql = "UPDATE maintenance SET date_maintenance=?, type_maintenance=?, statut=?, description=?, capteur_id=? WHERE id=?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, maintenance.getDateMaintenance());
            ps.setString(2, maintenance.getTypeMaintenance());
            ps.setString(3, maintenance.getStatut());
            ps.setString(4, maintenance.getDescription());
            ps.setInt(5, maintenance.getCapteurId());
            ps.setInt(6, maintenance.getId());
            ps.executeUpdate();
            System.out.println("✅ Maintenance modifiée avec succès !");
        } catch (SQLException e) {
            System.out.println("❌ Erreur modification maintenance : " + e.getMessage());
        }
    }

    // ✅ Supprimer une maintenance
    @Override
    public void supprimer(int id) {
        String sql = "DELETE FROM maintenance WHERE id=?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("✅ Maintenance supprimée avec succès !");
        } catch (SQLException e) {
            System.out.println("❌ Erreur suppression maintenance : " + e.getMessage());
        }
    }

    // ✅ Supprimer TOUTES les maintenances et créer 1 maintenance par capteur affiché
    public void reinitialiser() {
        try {
            // 1. Vider la table
            connection.createStatement().executeUpdate("DELETE FROM maintenance");
            System.out.println("🗑️ Maintenances supprimées.");

            // 2. Récupérer les capteurs dédupliqués (même logique que la table/map)
            //    1 seul capteur par (foret_id + localisation)
            List<Integer> capteurIds = new java.util.ArrayList<>();
            java.util.Set<String> cleesVues = new java.util.LinkedHashSet<>();
            ResultSet rs = connection.createStatement()
                .executeQuery("SELECT id, foret_id, localisation FROM capteur ORDER BY id");
            while (rs.next()) {
                String cle = rs.getInt("foret_id") + "|" +
                    (rs.getString("localisation") != null
                        ? rs.getString("localisation").trim().toLowerCase() : "");
                if (cleesVues.add(cle)) {
                    capteurIds.add(rs.getInt("id"));
                }
            }

            if (capteurIds.isEmpty()) {
                System.out.println("⚠️ Aucun capteur en BD — maintenances non créées.");
                return;
            }

            // 3. Données des maintenances : 1 par capteur, types/statuts variés
            String[] types   = {"preventive", "corrective", "remplacement",
                                 "preventive", "corrective", "preventive",
                                 "remplacement", "corrective", "preventive", "corrective"};
            String[] statuts = {"planifiee", "en_cours", "terminee",
                                 "planifiee", "terminee", "en_cours",
                                 "terminee", "planifiee", "terminee", "en_cours"};
            String[] descs   = {
                "Inspection capteur",
                "Réparation panne",
                "Remplacement capteur",
                "Vérification connexions",
                "Réparation câblage",
                "Nettoyage capteur",
                "Remplacement module",
                "Correction anomalie mesure",
                "Recalibrage capteur",
                "Remise en service"
            };

            // Dates espacées à partir du 2026-05-01
            java.time.LocalDate baseDate = java.time.LocalDate.of(2026, 5, 1);

            String sql = "INSERT INTO maintenance (date_maintenance, type_maintenance, statut, description, capteur_id) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement ps = connection.prepareStatement(sql);

            for (int i = 0; i < capteurIds.size(); i++) {
                int    idx       = i % types.length;
                String date      = baseDate.plusDays(i * 2L).toString();
                String type      = types[idx];
                String statut    = statuts[idx];
                String desc      = descs[idx];
                int    capteurId = capteurIds.get(i);

                ps.setString(1, date);
                ps.setString(2, type);
                ps.setString(3, statut);
                ps.setString(4, desc);
                ps.setInt(5, capteurId);
                ps.addBatch();
            }
            ps.executeBatch();
            System.out.println("✅ " + capteurIds.size() + " maintenances créées (1 par capteur).");

        } catch (SQLException e) {
            System.out.println("❌ Erreur réinitialisation maintenances : " + e.getMessage());
        }
    }

    // ✅ Afficher toutes les maintenances
    @Override
    public List<Maintenance> afficher() {
        List<Maintenance> maintenances = new ArrayList<>();
        String sql = "SELECT * FROM maintenance";
        try {
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                Maintenance m = new Maintenance(
                        rs.getInt("id"),
                        rs.getString("date_maintenance"),
                        rs.getString("type_maintenance"),
                        rs.getString("statut"),
                        rs.getString("description"),
                        rs.getInt("capteur_id")
                );
                maintenances.add(m);
            }
        } catch (SQLException e) {
            System.out.println("❌ Erreur affichage maintenances : " + e.getMessage());
        }
        return maintenances;
    }
}