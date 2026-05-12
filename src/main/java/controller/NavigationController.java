package controller;

import config.AppConfig;
import edu.pompier.entities.Pompier;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Contrôleur de navigation centralisé pour ForestGuard
 * Gère l'ouverture de tous les modules de l'application
 */
public class NavigationController {
    
    /**
     * Ouvre le module Gestion des Forêts
     */
    public static void ouvrirGestionForets(Stage stage) {
        try {
            System.out.println("🌲 Ouverture module Gestion Forêts...");
            FXMLLoader loader = new FXMLLoader(
                NavigationController.class.getResource("/fxml/ForetPrincipal.fxml"));
            Parent root = loader.load();
            
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("ForestGuard - Gestion des Forêts");
            
            System.out.println("✅ Module Gestion Forêts chargé");
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'ouverture du module Forêts: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Ouvre le module Gestion des Capteurs
     */
    public static void ouvrirGestionCapteurs(Stage stage) {
        try {
            System.out.println("📡 Ouverture module Gestion Capteurs...");
            // Path spécial pour ce module (ForestGuard_FINAL)
            FXMLLoader loader = new FXMLLoader(
                NavigationController.class.getResource("/capteur.fxml"));
            Parent root = loader.load();
            
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("ForestGuard - Gestion des Capteurs");
            
            System.out.println("✅ Module Gestion Capteurs chargé");
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'ouverture du module Capteurs: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Ouvre le module Gestion des Données
     */
    public static void ouvrirGestionDonnees(Stage stage) {
        try {
            System.out.println("📊 Ouverture module Gestion Données...");
            FXMLLoader loader = new FXMLLoader(
                NavigationController.class.getResource("/fxml/donnees.fxml"));
            Parent root = loader.load();
            
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("ForestGuard - Gestion des Données");
            
            System.out.println("✅ Module Gestion Données chargé");
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'ouverture du module Données: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Ouvre le module Gestion des Alertes
     */
    public static void ouvrirGestionAlertes(Stage stage) {
        try {
            System.out.println("🚨 Ouverture module Gestion Alertes...");
            FXMLLoader loader = new FXMLLoader(
                NavigationController.class.getResource("/fxml/Dashboard.fxml"));
            Parent root = loader.load();
            
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("ForestGuard - Gestion des Alertes");
            
            System.out.println("✅ Module Gestion Alertes chargé");
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'ouverture du module Alertes: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Ouvre le module Gestion des Interventions
     * Passe les informations du pompier connecté au controller
     */
    public static void ouvrirGestionInterventions(Stage stage, Pompier pompier) {
        try {
            System.out.println("🚒 Ouverture module Gestion Interventions...");
            
            // Stocker le pompier dans AppConfig
            if (pompier != null) {
                AppConfig.getInstance().setPompierConnecte(pompier);
            }
            
            FXMLLoader loader = new FXMLLoader(
                NavigationController.class.getResource("/fxml/DashboardAgent.fxml"));
            Parent root = loader.load();
            
            // Récupérer le controller et passer les infos du pompier
            DashboardAgentController controller = loader.getController();
            if (controller != null && pompier != null) {
                controller.setPompierInfo(pompier.getNom() + " " + pompier.getPrenom(), pompier.getId());
            }
            
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("ForestGuard - Gestion des Interventions");
            
            System.out.println("✅ Module Gestion Interventions chargé");
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'ouverture du module Interventions: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Ouvre le login utilisateur (espace utilisateur)
     */
    public static void ouvrirLoginUtilisateur(Stage stage) {
        try {
            System.out.println("👤 Ouverture Login Utilisateur...");
            FXMLLoader loader = new FXMLLoader(
                NavigationController.class.getResource("/com/forestguard/views/login.fxml"));
            Parent root = loader.load();
            
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("ForestGuard - Espace Utilisateur");
            
            System.out.println("✅ Login Utilisateur chargé");
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'ouverture du Login Utilisateur: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Retourne au login pompier (déconnexion)
     */
    public static void retourLogin(Stage stage) {
        try {
            System.out.println("🔙 Retour au login...");
            
            // Déconnecter le pompier
            AppConfig.getInstance().deconnecter();
            
            FXMLLoader loader = new FXMLLoader(
                NavigationController.class.getResource("/Login.fxml"));
            Parent root = loader.load();
            
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("ForestGuard - Connexion");
            stage.setMaximized(true);
            
            System.out.println("✅ Retour au login effectué");
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du retour au login: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Ouvre le dashboard pompier principal
     */
    public static void ouvrirDashboardPompier(Stage stage, Pompier pompier) {
        try {
            System.out.println("🏠 Ouverture Dashboard Pompier...");
            
            // Stocker le pompier dans AppConfig
            if (pompier != null) {
                AppConfig.getInstance().setPompierConnecte(pompier);
            }
            
            FXMLLoader loader = new FXMLLoader(
                NavigationController.class.getResource("/GestionPompier.fxml"));
            Parent root = loader.load();
            
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("ForestGuard - Dashboard");
            stage.setMaximized(true);
            
            System.out.println("✅ Dashboard Pompier chargé");
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'ouverture du Dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Définit l'icône ForestGuard sur une fenêtre
     */
    public static void setForestGuardIcon(Stage stage) {
        try {
            Image icon = new Image(
                NavigationController.class.getResourceAsStream("/logo.png"));
            stage.getIcons().add(icon);
        } catch (Exception e) {
            System.out.println("⚠️ Icône ForestGuard non trouvée");
        }
    }
}
