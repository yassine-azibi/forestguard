package app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Point d'entrée unique de l'application ForestGuard
 * Lance le Login pompier qui est le point de départ de toute l'application
 */
public class Main extends Application {
    
    @Override
    public void start(Stage stage) throws Exception {
        System.out.println("🌲 Démarrage de ForestGuard...");
        
        // Charger le Login pompier (point d'entrée de l'application)
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/Login.fxml"));
        Parent root = loader.load();
        
        Scene scene = new Scene(root);
        
        // Définir l'icône ForestGuard
        try {
            Image icon = new Image(getClass().getResourceAsStream("/logo.png"));
            stage.getIcons().add(icon);
            System.out.println("✅ Icône ForestGuard chargée");
        } catch (Exception e) {
            System.out.println("⚠️ Icône ForestGuard non trouvée");
        }
        
        stage.setTitle("ForestGuard - Connexion");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
        
        System.out.println("✅ ForestGuard démarré avec succès");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
