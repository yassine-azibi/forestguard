package com.forestguard.app;

import com.forestguard.utils.SchemaInitializer;
import javafx.application.Application;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class ForestGuardApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/com/forestguard/views/login.fxml"));
        Scene scene = new Scene(root);
        stage.setTitle("ForestGuard - Gestion des utilisateurs");
        stage.setScene(scene);
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
        stage.setResizable(false);
        stage.setMaximized(true);
        stage.show();

        try {
            SchemaInitializer.initialize();
        } catch (IllegalStateException exception) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Base de donnees indisponible");
            alert.setHeaderText(null);
            alert.setContentText("L'application a demarre, mais la base MySQL n'a pas pu etre initialisee. Verifiez MySQL et db.properties.");
            alert.showAndWait();
        }
    }

    public static void launchApplication(String[] args) {
        launch(args);
    }
}