package controller;

import dao.EquipementDAO;
import dao.InterventionDAO;
import model.Equipement;
import model.Intervention;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class ModifierInterventionController implements Initializable {

    @FXML private TextField           tfZone;
    @FXML private ListView<Equipement> lvEquipements;
    @FXML private TextArea            taResultat;
    @FXML private ComboBox<String>    cbStatut;

    private final EquipementDAO   equipementDAO   = new EquipementDAO();
    private final InterventionDAO interventionDAO = new InterventionDAO();

    private Intervention intervention;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Charger tous les équipements
        lvEquipements.setItems(FXCollections.observableArrayList(equipementDAO.getAll()));
        lvEquipements.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Statuts
        cbStatut.setItems(FXCollections.observableArrayList("In Progress", "Completed"));
    }

    // Appelé depuis DashboardAgentController pour passer l'intervention
    public void setIntervention(Intervention inter) {
        this.intervention = inter;

        tfZone.setText(inter.getAlertZone());
        taResultat.setText(inter.getResultat() != null ? inter.getResultat() : "");
        cbStatut.setValue(inter.getStatut());

        // Pré-sélectionner les équipements déjà associés
        List<Equipement> dejaSel = equipementDAO.getByIntervention(
                inter.getAlertZone(), inter.getAgentName());

        for (int i = 0; i < lvEquipements.getItems().size(); i++) {
            Equipement eq = lvEquipements.getItems().get(i);
            for (Equipement sel : dejaSel) {
                if (eq.getId() == sel.getId()) {
                    lvEquipements.getSelectionModel().select(i);
                }
            }
        }
    }

    @FXML
    private void handleEnregistrer() {
        if (lvEquipements.getSelectionModel().getSelectedItems().isEmpty()) {
            showAlert("Veuillez sélectionner au moins un équipement !");
            return;
        }

        // Mettre à jour l'intervention
        intervention.setStatut(cbStatut.getValue());
        intervention.setResultat(taResultat.getText().trim());
        if ("Completed".equals(cbStatut.getValue()) && intervention.getEndDate() == null) {
            intervention.setEndDate(java.time.LocalDateTime.now());
        }

        if (interventionDAO.update(intervention)) {
            // Mettre à jour les équipements
            equipementDAO.deleteByIntervention(
                    intervention.getAlertZone(), intervention.getAgentName());
            equipementDAO.addEquipements(
                    intervention.getAlertZone(),
                    intervention.getAgentName(),
                    intervention.getStartDate(),
                    lvEquipements.getSelectionModel().getSelectedItems()
            );
            showSuccess("Intervention modifiée avec succès !");
            handleAnnuler();
        } else {
            showAlert("Erreur lors de la modification !");
        }
    }

    @FXML
    private void handleAnnuler() {
        tfZone.getScene().getWindow().hide();
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle("Attention");
        a.setContentText(msg);
        a.showAndWait();
    }

    private void showSuccess(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Succès");
        a.setContentText(msg);
        a.showAndWait();
    }
}
