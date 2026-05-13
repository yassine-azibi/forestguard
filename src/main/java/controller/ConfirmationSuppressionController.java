package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

/**
 * Controller du panneau de confirmation de suppression.
 *
 * Utilisation dans ForetPrincipal :
 *
 *   FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ConfirmationSuppression.fxml"));
 *   Parent root = loader.load();
 *   ConfirmationSuppressionController ctrl = loader.getController();
 *   ctrl.setNomForet("Forêt de Siliana");
 *   ctrl.setOnConfirmer(() -> {
 *       // votre logique de suppression ici
 *       foretService.supprimer(foret);
 *       rafraichirListe();
 *   });
 *   Stage dialog = new Stage();
 *   dialog.initModality(Modality.APPLICATION_MODAL);
 *   dialog.initStyle(StageStyle.TRANSPARENT);
 *   dialog.setScene(new Scene(root, Color.TRANSPARENT));
 *   dialog.showAndWait();
 */
public class ConfirmationSuppressionController {

    @FXML private Label lblNomForet;

    private Runnable onConfirmer;

    /** Injecter le nom de la forêt à afficher dans le panneau. */
    public void setNomForet(String nom) {
        lblNomForet.setText(nom);
    }

    /** Callback exécuté si l'utilisateur clique sur "Supprimer". */
    public void setOnConfirmer(Runnable action) {
        this.onConfirmer = action;
    }

    @FXML
    private void confirmerSuppression() {
        fermer();
        if (onConfirmer != null) {
            onConfirmer.run();
        }
    }

    @FXML
    private void fermer() {
        Stage stage = (Stage) lblNomForet.getScene().getWindow();
        stage.close();
    }
}

