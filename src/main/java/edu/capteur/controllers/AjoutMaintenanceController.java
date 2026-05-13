package edu.capteur.controllers;

import edu.capteur.entities.Capteur;
import edu.capteur.entities.Maintenance;
import edu.capteur.services.CapteurService;
import edu.capteur.services.MaintenanceService;
import edu.capteur.utils.DescriptionsMaintenance;
import edu.capteur.utils.Validator;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class AjoutMaintenanceController {

    @FXML private DatePicker        dpDate;
    @FXML private ComboBox<String>  cbType, cbStatut;
    @FXML private ComboBox<Capteur> cbCapteur;
    @FXML private ComboBox<String>  cbDescription;
    @FXML private Label lblErreurDate, lblErreurType, lblErreurStatut,
                        lblErreurCapteur, lblErreurDescription;

    private final MaintenanceService maintenanceService = new MaintenanceService();
    private final CapteurService     capteurService     = new CapteurService();

    @FXML
    public void initialize() {
        cbType.setItems(FXCollections.observableArrayList(Validator.TYPES_MAINTENANCE));
        cbStatut.setItems(FXCollections.observableArrayList(Validator.STATUTS_MAINTENANCE));

        // ── Charger uniquement les capteurs dédupliqués (1 par foret_id + localisation) ──
        java.util.List<Capteur> tous = capteurService.afficher();
        java.util.Set<String> cleesVues = new java.util.LinkedHashSet<>();
        java.util.List<Capteur> capteursUniques = new java.util.ArrayList<>();
        for (Capteur c : tous) {
            String cle = c.getForetId() + "|" + (c.getLocalisation() != null ? c.getLocalisation().trim().toLowerCase() : "");
            if (cleesVues.add(cle)) capteursUniques.add(c);
        }
        cbCapteur.setItems(FXCollections.observableArrayList(capteursUniques));

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
        dpDate.valueProperty().addListener((obs, old, val) -> clearErrorPicker(lblErreurDate, dpDate));
        cbStatut.valueProperty().addListener((obs, old, val) -> clearErrorCombo(lblErreurStatut, cbStatut));
        cbCapteur.valueProperty().addListener((obs, old, val) -> clearErrorCombo(lblErreurCapteur, cbCapteur));
        cbDescription.valueProperty().addListener((obs, old, val) -> clearErrorCombo(lblErreurDescription, cbDescription));
        cbDescription.getEditor().textProperty().addListener((obs, old, val) -> clearErrorCombo(lblErreurDescription, cbDescription));
    }

    private String getDescription() {
        String val = cbDescription.getEditor().getText();
        if (val == null || val.isBlank())
            val = cbDescription.getValue() != null ? cbDescription.getValue() : "";
        return val.trim();
    }

    @FXML
    public void ajouter() {
        if (!valider()) return;
        maintenanceService.ajouter(new Maintenance(
                dpDate.getValue().toString(),
                cbType.getValue(),
                cbStatut.getValue(),
                getDescription(),
                cbCapteur.getValue().getId()
        ));
        fermer();
    }

    private boolean valider() {
        clearAllErrors();
        int capteurId = (cbCapteur.getValue() != null) ? cbCapteur.getValue().getId() : -1;
        Validator.ResultatValidation resultat = Validator.validerMaintenance(
                dpDate.getValue(), cbType.getValue(), cbStatut.getValue(),
                getDescription(), capteurId, true);
        for (String erreur : resultat.getErreurs()) {
            String champ   = Validator.extraireChamp(erreur);
            String message = Validator.extraireMessage(erreur);
            switch (champ) {
                case "date"        -> setErrorPicker(lblErreurDate,        dpDate,        message);
                case "type"        -> setErrorCombo(lblErreurType,         cbType,        message);
                case "statut"      -> setErrorCombo(lblErreurStatut,       cbStatut,      message);
                case "description" -> setErrorCombo(lblErreurDescription,  cbDescription, message);
                case "capteur"     -> setErrorCombo(lblErreurCapteur,      cbCapteur,     message);
            }
        }
        return resultat.estValide();
    }

    private void clearAllErrors() {
        clearErrorPicker(lblErreurDate,        dpDate);
        clearErrorCombo(lblErreurType,         cbType);
        clearErrorCombo(lblErreurStatut,       cbStatut);
        clearErrorCombo(lblErreurDescription,  cbDescription);
        clearErrorCombo(lblErreurCapteur,      cbCapteur);
    }

    // ── Helpers d'erreur ──

    private void setErrorCombo(Label label, ComboBox<?> combo, String message) {
        if (label != null) { label.setText(message); label.setTextFill(Color.RED); label.setVisible(true); }
        if (combo != null) combo.setStyle("-fx-border-color: #dc2626; -fx-border-width: 1.5px;" +
                                          "-fx-border-radius: 10; -fx-background-radius: 10;");
    }

    private void setErrorPicker(Label label, DatePicker picker, String message) {
        if (label != null) { label.setText(message); label.setTextFill(Color.RED); label.setVisible(true); }
        if (picker != null) picker.setStyle("-fx-border-color: #dc2626; -fx-border-width: 1.5px;" +
                                            "-fx-border-radius: 10; -fx-background-radius: 10;");
    }

    private void clearErrorCombo(Label label, ComboBox<?> combo) {
        if (label != null) { label.setText(""); label.setVisible(false); }
        if (combo != null) combo.setStyle("");
    }

    private void clearErrorPicker(Label label, DatePicker picker) {
        if (label != null) { label.setText(""); label.setVisible(false); }
        if (picker != null) picker.setStyle("");
    }

    @FXML public void annuler() { fermer(); }
    private void fermer() { ((Stage) cbDescription.getScene().getWindow()).close(); }
}
