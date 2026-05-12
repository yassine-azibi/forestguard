package edu.capteur.controllers;

import edu.capteur.entities.Maintenance;
import edu.capteur.services.MaintenanceService;
import edu.capteur.utils.DescriptionsMaintenance;
import edu.capteur.utils.Validator;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import java.time.LocalDate;

public class ModifierMaintenanceController {

    @FXML private DatePicker       dpDate;
    @FXML private ComboBox<String> cbType, cbStatut, cbDescription;
    @FXML private Label            lblErreurDate, lblErreurType,
                                   lblErreurStatut, lblErreurDescription;

    private final MaintenanceService maintenanceService = new MaintenanceService();
    private Maintenance maintenance;

    @FXML
    public void initialize() {
        cbType.setItems(FXCollections.observableArrayList(Validator.TYPES_MAINTENANCE));
        cbStatut.setItems(FXCollections.observableArrayList(Validator.STATUTS_MAINTENANCE));

        cbDescription.setEditable(true);
        cbDescription.setPromptText("Sélectionner ou saisir une description...");
        cbDescription.setItems(FXCollections.observableArrayList(DescriptionsMaintenance.toutes()));

        // Quand le type change → filtrer les suggestions
        cbType.valueProperty().addListener((obs, old, type) -> {
            String valActuelle = getDescription();
            cbDescription.setItems(FXCollections.observableArrayList(
                    DescriptionsMaintenance.parType(type)));
            if (!valActuelle.isEmpty()) cbDescription.getEditor().setText(valActuelle);
            else cbDescription.getEditor().clear();
            clearErrorCombo(lblErreurType, cbType);
        });

        // Effacement des erreurs en temps réel
        dpDate.valueProperty().addListener((obs, old, val) ->
                clearErrorPicker(lblErreurDate, dpDate));
        cbStatut.valueProperty().addListener((obs, old, val) ->
                clearErrorCombo(lblErreurStatut, cbStatut));
        cbDescription.valueProperty().addListener((obs, old, val) ->
                clearErrorCombo(lblErreurDescription, cbDescription));
        cbDescription.getEditor().textProperty().addListener((obs, old, val) ->
                clearErrorCombo(lblErreurDescription, cbDescription));
    }

    public void setMaintenance(Maintenance m) {
        this.maintenance = m;
        try {
            dpDate.setValue(LocalDate.parse(m.getDateMaintenance().substring(0, 10)));
        } catch (Exception e) {
            dpDate.setValue(LocalDate.now());
        }
        cbType.setValue(m.getTypeMaintenance());
        cbStatut.setValue(m.getStatut());

        // Charger les suggestions du type actuel, champ description vide
        cbDescription.setItems(FXCollections.observableArrayList(
                DescriptionsMaintenance.parType(m.getTypeMaintenance())));
        cbDescription.setValue(null);
        cbDescription.getEditor().clear();
    }

    private String getDescription() {
        String val = cbDescription.getEditor().getText();
        if (val == null || val.isBlank())
            val = cbDescription.getValue() != null ? cbDescription.getValue() : "";
        return val.trim();
    }

    @FXML
    public void modifier() {
        if (!valider()) return;
        maintenance.setDateMaintenance(dpDate.getValue().toString());
        maintenance.setTypeMaintenance(cbType.getValue());
        maintenance.setStatut(cbStatut.getValue());
        maintenance.setDescription(getDescription());
        maintenanceService.modifier(maintenance);
        fermer();
    }

    private boolean valider() {
        clearAllErrors();
        Validator.ResultatValidation resultat = Validator.validerMaintenance(
                dpDate.getValue(),
                cbType.getValue(),
                cbStatut.getValue(),
                getDescription(),
                maintenance != null ? maintenance.getCapteurId() : -1,
                false);

        for (String erreur : resultat.getErreurs()) {
            String champ   = Validator.extraireChamp(erreur);
            String message = Validator.extraireMessage(erreur);
            switch (champ) {
                case "date"        -> setErrorPicker(lblErreurDate,        dpDate,        message);
                case "type"        -> setErrorCombo(lblErreurType,         cbType,        message);
                case "statut"      -> setErrorCombo(lblErreurStatut,       cbStatut,      message);
                case "description" -> setErrorCombo(lblErreurDescription,  cbDescription, message);
            }
        }
        return resultat.estValide();
    }

    private void clearAllErrors() {
        clearErrorPicker(lblErreurDate,        dpDate);
        clearErrorCombo(lblErreurType,         cbType);
        clearErrorCombo(lblErreurStatut,       cbStatut);
        clearErrorCombo(lblErreurDescription,  cbDescription);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private void setErrorCombo(Label label, ComboBox<?> combo, String message) {
        if (label != null) {
            label.setText(message);
            label.setTextFill(Color.RED);
            label.setVisible(true);
            label.setManaged(true);
        }
        if (combo != null)
            combo.setStyle("-fx-border-color: #dc2626; -fx-border-width: 1.5px;" +
                           "-fx-border-radius: 10; -fx-background-radius: 10;");
    }

    private void setErrorPicker(Label label, DatePicker picker, String message) {
        if (label != null) {
            label.setText(message);
            label.setTextFill(Color.RED);
            label.setVisible(true);
            label.setManaged(true);
        }
        if (picker != null)
            picker.setStyle("-fx-border-color: #dc2626; -fx-border-width: 1.5px;" +
                            "-fx-border-radius: 10; -fx-background-radius: 10;");
    }

    private void clearErrorCombo(Label label, ComboBox<?> combo) {
        if (label != null) { label.setText(""); label.setVisible(false); label.setManaged(false); }
        if (combo != null) combo.setStyle("");
    }

    private void clearErrorPicker(Label label, DatePicker picker) {
        if (label != null) { label.setText(""); label.setVisible(false); label.setManaged(false); }
        if (picker != null) picker.setStyle("");
    }

    @FXML public void annuler() { fermer(); }
    private void fermer() { ((Stage) cbDescription.getScene().getWindow()).close(); }
}
