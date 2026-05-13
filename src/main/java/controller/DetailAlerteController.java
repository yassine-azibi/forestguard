package controller;

import dao.AlerteDAO;
import model.Alerte;
import utils.NavigationManager;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class DetailAlerteController implements Initializable {

    // L'alerte sélectionnée depuis le Dashboard (passage de données entre controllers)
    private static Alerte alerteSelectionnee;

    @FXML private Label lblId;
    @FXML private Label lblType;
    @FXML private Label lblNiveau;
    @FXML private Label lblLocalisation;
    @FXML private Label lblDate;
    @FXML private Label lblSource;
    @FXML private Label lblStatutActuel;
    @FXML private ComboBox<String> cbNouveauStatut;
    @FXML private Label lblMessage;

    private final AlerteDAO dao = new AlerteDAO();

    // Méthode statique pour passer l'alerte depuis DashboardController
    public static void setAlerteSelectionnee(Alerte a) {
        alerteSelectionnee = a;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cbNouveauStatut.getItems().addAll("Nouvelle", "Validée", "Rejetée");
        lblMessage.setText("");

        if (alerteSelectionnee != null) {
            remplirChamps(alerteSelectionnee);
        } else {
            lblId.setText("—");
            lblType.setText("Aucune alerte sélectionnée");
        }
    }

    private void remplirChamps(Alerte a) {
        lblId.setText("#" + a.getId());
        lblType.setText(a.getTypeAlerte());
        lblNiveau.setText(a.getNiveau());
        lblLocalisation.setText(a.getLocalisation());
        lblDate.setText(a.getDateFormatted());
        lblSource.setText(a.getSource());
        lblStatutActuel.setText(a.getStatut());
        cbNouveauStatut.setValue(a.getStatut());

        // Couleur selon statut
        lblStatutActuel.setStyle(switch (a.getStatut()) {
            case "Validée" -> "-fx-text-fill:#1e8449;-fx-font-weight:bold;-fx-font-size:14";
            case "Rejetée" -> "-fx-text-fill:#c0392b;-fx-font-weight:bold;-fx-font-size:14";
            default        -> "-fx-text-fill:#1a5276;-fx-font-weight:bold;-fx-font-size:14";
        });
    }

    @FXML
    private void handleModifierStatut() {
        if (alerteSelectionnee == null) return;

        String nouveauStatut = cbNouveauStatut.getValue();
        if (dao.updateStatut(alerteSelectionnee.getId(), nouveauStatut)) {
            alerteSelectionnee.setStatut(nouveauStatut);
            lblStatutActuel.setText(nouveauStatut);
            lblMessage.setText("✔ Statut mis à jour avec succès !");
            lblMessage.setStyle("-fx-text-fill:#1e8449;-fx-font-weight:bold");
        } else {
            lblMessage.setText("✕ Erreur lors de la mise à jour.");
            lblMessage.setStyle("-fx-text-fill:#c0392b;-fx-font-weight:bold");
        }
    }

    @FXML
    private void handleValider() {
        if (alerteSelectionnee == null) return;
        dao.updateStatut(alerteSelectionnee.getId(), "Validée");
        alerteSelectionnee.setStatut("Validée");
        remplirChamps(alerteSelectionnee);
        lblMessage.setText("✔ Alerte validée !");
        lblMessage.setStyle("-fx-text-fill:#1e8449;-fx-font-weight:bold");
    }

    @FXML
    private void handleRejeter() {
        if (alerteSelectionnee == null) return;
        dao.updateStatut(alerteSelectionnee.getId(), "Rejetée");
        alerteSelectionnee.setStatut("Rejetée");
        remplirChamps(alerteSelectionnee);
        lblMessage.setText("✔ Alerte rejetée.");
        lblMessage.setStyle("-fx-text-fill:#c0392b;-fx-font-weight:bold");
    }

    @FXML
    private void goDashboard() {
        alerteSelectionnee = null;
        Stage s = (Stage) lblId.getScene().getWindow();
        NavigationManager.navigateTo(s, "/fxml/Dashboard.fxml");
    }
}
