package ForestGuard;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainFx extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(
                getClass().getResource("/donnees.fxml"));
        stage.setTitle("ForestGuard");
        stage.setScene(new Scene(root, 1280, 800));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
