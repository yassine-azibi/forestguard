package tests;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class MainFX extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/DashboardAgent.fxml"));
        Scene scene = new Scene(loader.load());
        
        // Ajouter l'icône ForestGuard
        try {
            Image icon = new Image(getClass().getResourceAsStream("/image/foret-logo.png"));
            stage.getIcons().add(icon);
            System.out.println("✅ Icône ForestGuard chargée avec succès");
        } catch (Exception e) {
            System.out.println("⚠️ Impossible de charger l'icône: " + e.getMessage());
        }
        
        stage.setTitle("ForestGuard - Gestion des Interventions");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}