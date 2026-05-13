package utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class NavigationManager {

    public static void navigateTo(Stage stage, String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(
                NavigationManager.class.getResource(fxmlPath));
            Parent root = loader.load();
            // Toutes les pages ont la même taille → transitions fluides
            Scene scene = new Scene(root, 1200, 720);
            stage.setScene(scene);
            stage.setWidth(1200);
            stage.setHeight(720);
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            System.out.println("Erreur navigation : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
