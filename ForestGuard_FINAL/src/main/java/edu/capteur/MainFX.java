package edu.capteur;

import edu.capteur.services.MaintenanceService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainFX extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // ── Réinitialiser les maintenances au démarrage ──
        new MaintenanceService().reinitialiser();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/capteur.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 1280, 800);
        primaryStage.setTitle("ForestGuard - Surveillance Intelligente des Forêts");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
