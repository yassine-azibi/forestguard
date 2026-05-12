package edu.capteur.controllers;

import edu.capteur.entities.Capteur;
import edu.capteur.entities.Foret;
import edu.capteur.services.CapteurService;
import edu.capteur.services.ForetService;
import edu.capteur.utils.Validator;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class AjoutCapteurController {

    @FXML private TextField        tfNom;
    @FXML private ComboBox<String> cbLocalisation;   // ← ComboBox éditable
    @FXML private ComboBox<String> cbType, cbStatut;
    @FXML private ComboBox<Foret>  cbForet;
    @FXML private Label lblErreurNom, lblErreurLocalisation, lblErreurType, lblErreurStatut, lblErreurForet;

    private final CapteurService capteurService = new CapteurService();
    private final ForetService   foretService   = new ForetService();

    @FXML
    public void initialize() {
        cbType.setItems(FXCollections.observableArrayList(Validator.TYPES_CAPTEUR));
        cbStatut.setItems(FXCollections.observableArrayList(Validator.STATUTS_CAPTEUR));

        // Charger les forêts
        List<Foret> forets = foretService.afficher();
        cbForet.setItems(FXCollections.observableArrayList(forets));

        // Construire la liste des localisations à partir des régions des forêts
        cbLocalisation.setItems(FXCollections.observableArrayList(extraireLocalisations(forets)));
        cbLocalisation.setEditable(true);
        cbLocalisation.setPromptText("Sélectionner ou saisir une localisation...");

        // Quand une forêt est sélectionnée, pré-remplir la localisation avec sa région
        cbForet.valueProperty().addListener((obs, old, foret) -> {
            if (foret != null && (cbLocalisation.getValue() == null
                    || cbLocalisation.getValue().isEmpty())) {
                cbLocalisation.setValue(foret.getLocalisation());
            }
            clearError(lblErreurForet, null);
        });

        // Effacement des erreurs en temps réel
        tfNom.textProperty().addListener((obs, old, val) -> clearError(lblErreurNom, tfNom));
        cbLocalisation.valueProperty().addListener((obs, old, val) -> clearError(lblErreurLocalisation, null));
        cbLocalisation.getEditor().textProperty().addListener((obs, old, val) -> clearError(lblErreurLocalisation, null));
        cbType.valueProperty().addListener((obs, old, val) -> clearError(lblErreurType, null));
        cbStatut.valueProperty().addListener((obs, old, val) -> clearError(lblErreurStatut, null));
    }

    /** Retourne toutes les régions de Tunisie + celles des forêts en BD (sans doublons) */
    private List<String> extraireLocalisations(List<Foret> forets) {
        // Toutes les régions de Tunisie (gouvernorats)
        List<String> toutesRegions = new ArrayList<>(java.util.Arrays.asList(
            "Ariana", "Béja", "Ben Arous", "Bizerte", "Gabès", "Gafsa",
            "Jendouba", "Kairouan", "Kasserine", "Kébili", "Kef", "Mahdia",
            "Manouba", "Médenine", "Monastir", "Nabeul", "Sfax", "Sidi Bouzid",
            "Siliana", "Sousse", "Tataouine", "Tozeur", "Tunis", "Zaghouan"
        ));
        // Ajouter les localisations des forêts en BD si pas déjà présentes
        for (Foret f : forets) {
            if (f.getLocalisation() != null && !f.getLocalisation().isBlank()) {
                String loc = f.getLocalisation().trim();
                if (!toutesRegions.contains(loc))
                    toutesRegions.add(loc);
            }
        }
        java.util.Collections.sort(toutesRegions);
        return toutesRegions;
    }

    private String getLocalisation() {
        String val = cbLocalisation.getEditor().getText();
        if (val == null || val.isBlank()) val = cbLocalisation.getValue() != null
                ? cbLocalisation.getValue() : "";
        return val.trim();
    }

    @FXML
    public void ajouter() {
        if (!valider()) return;
        Foret foretSelectionnee = cbForet.getValue();
        capteurService.ajouter(new Capteur(
                tfNom.getText().trim(),
                cbType.getValue(),
                getLocalisation(),
                cbStatut.getValue(),
                foretSelectionnee.getId()
        ));
        fermer();
    }

    private boolean valider() {
        clearAllErrors();
        Validator.ResultatValidation resultat = Validator.validerCapteur(
                tfNom.getText(),
                getLocalisation(),
                cbType.getValue(),
                cbStatut.getValue()
        );
        if (cbForet.getValue() == null)
            resultat.ajouterErreur("foret|⚠ Veuillez sélectionner une forêt.");

        for (String erreur : resultat.getErreurs()) {
            String champ   = Validator.extraireChamp(erreur);
            String message = Validator.extraireMessage(erreur);
            switch (champ) {
                case "nom"          -> setError(lblErreurNom, tfNom, message);
                case "localisation" -> setErrorCombo(lblErreurLocalisation, cbLocalisation, message);
                case "type"         -> setErrorCombo(lblErreurType, cbType, message);
                case "statut"       -> setErrorCombo(lblErreurStatut, cbStatut, message);
                case "foret"        -> setErrorCombo(lblErreurForet, cbForet, message);
            }
        }
        return resultat.estValide();
    }

    private void clearAllErrors() {
        clearError(lblErreurNom, tfNom);
        clearErrorCombo(lblErreurLocalisation, cbLocalisation);
        clearErrorCombo(lblErreurType, cbType);
        clearErrorCombo(lblErreurStatut, cbStatut);
        clearErrorCombo(lblErreurForet, cbForet);
    }

    private void setError(Label label, TextField field, String message) {
        if (label != null) { label.setText(message); label.setTextFill(Color.RED); label.setVisible(true); }
        if (field != null) field.setStyle("-fx-border-color: #dc2626; -fx-border-width: 1.5px;");
    }

    private void setErrorCombo(Label label, ComboBox<?> combo, String message) {
        if (label != null) { label.setText(message); label.setTextFill(Color.RED); label.setVisible(true); }
        if (combo != null) combo.setStyle("-fx-border-color: #dc2626; -fx-border-width: 1.5px; -fx-border-radius: 10; -fx-background-radius: 10;");
    }

    private void clearError(Label label, TextField field) {
        if (label != null) { label.setText(""); label.setVisible(false); }
        if (field != null) field.setStyle("");
    }

    private void clearErrorCombo(Label label, ComboBox<?> combo) {
        if (label != null) { label.setText(""); label.setVisible(false); }
        if (combo != null) combo.setStyle("");
    }

    @FXML public void annuler() { fermer(); }
    private void fermer() { ((Stage) tfNom.getScene().getWindow()).close(); }
}
