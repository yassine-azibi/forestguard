package controller;

import model.Animal;
import utils.AnimalService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class AjouterAnimal {

    @FXML private Label             titreLabel;
    @FXML private TextField         zoneField;
    @FXML private TextField         especeField;
    @FXML private TextField         nomScientifiqueField;
    @FXML private TextField         populationField;
    @FXML private ChoiceBox<String> statutCombo;
    @FXML private DatePicker        observationPicker;

    private Animal animalEnEdition = null;
    private final AnimalService service = new AnimalService();

    @FXML
    public void initialize() {
        statutCombo.getItems().addAll("Protege", "Vulnerable", "En danger", "Eteint");
        statutCombo.setValue("Protege");
        observationPicker.setValue(java.time.LocalDate.now());

        // ── Restrictions en temps réel ──
        Validation.lettresSeulement(especeField);           // Espèce : lettres
        Validation.alphanumerique(nomScientifiqueField);     // Nom scientifique
        Validation.entierPositif(zoneField);                 // Zone : entier
        Validation.entierPositif(populationField);           // Population : entier

        Validation.reinitialiserAuFocus(especeField, nomScientifiqueField, zoneField, populationField);
    }

    /** Appelez cette methode avant d'afficher le modal pour pre-remplir en mode edition. */
    public void remplirPourEdition(Animal animal) {
        this.animalEnEdition = animal;
        if (titreLabel != null) titreLabel.setText("Modifier Animal");
        zoneField.setText(String.valueOf(animal.getIdZone()));
        especeField.setText(animal.getEspece());
        nomScientifiqueField.setText(animal.getNomScientifique());
        populationField.setText(String.valueOf(animal.getPopulationEstimee()));
        statutCombo.setValue(animal.getStatutProtection());
        if (animal.getDerniereObservation() != null && !animal.getDerniereObservation().isEmpty()) {
            try {
                observationPicker.setValue(java.time.LocalDate.parse(animal.getDerniereObservation()));
            } catch (Exception ignored) {}
        }
    }

    @FXML
    public void validerEtAjouter(ActionEvent event) {
        boolean ok = true;
        StringBuilder erreurs = new StringBuilder();

        if (!Validation.requis(especeField, "Espèce")) {
            erreurs.append("• L'espèce est obligatoire (lettres uniquement).\n");
            ok = false;
        }

        if (!Validation.requis(zoneField, "Zone") || !Validation.estEntierPositif(zoneField)) {
            erreurs.append("• La zone doit être un entier positif.\n");
            Validation.marquerErreur(zoneField);
            ok = false;
        }

        if (!Validation.requis(populationField, "Population") || !Validation.estEntierPositif(populationField)) {
            erreurs.append("• La population doit être un entier positif.\n");
            Validation.marquerErreur(populationField);
            ok = false;
        }

        if (observationPicker.getValue() == null) {
            erreurs.append("• La date d'observation est obligatoire.\n");
            ok = false;
        }

        if (!ok) {
            Validation.afficherErreur("Erreurs de saisie", erreurs.toString().trim());
            return;
        }

        try {
            int    idZone     = Integer.parseInt(zoneField.getText().trim());
            int    population = Integer.parseInt(populationField.getText().trim());
            String obs        = observationPicker.getValue().toString();

            if (animalEnEdition == null) {
                Animal a = new Animal(
                        idZone,
                        especeField.getText().trim(),
                        nomScientifiqueField.getText().trim(),
                        population,
                        statutCombo.getValue(),
                        obs);
                service.addEntity(a);
            } else {
                animalEnEdition.setIdZone(idZone);
                animalEnEdition.setEspece(especeField.getText().trim());
                animalEnEdition.setNomScientifique(nomScientifiqueField.getText().trim());
                animalEnEdition.setPopulationEstimee(population);
                animalEnEdition.setStatutProtection(statutCombo.getValue());
                animalEnEdition.setDerniereObservation(obs);
                service.updateEntity(animalEnEdition.getId(), animalEnEdition);
            }
            fermerFenetre();
        } catch (NumberFormatException e) {
            Validation.afficherErreur("Erreur", "Valeur numérique invalide.");
        }
    }

    @FXML
    public void annuler(ActionEvent event) {
        fermerFenetre();
    }

    private void fermerFenetre() {
        ((Stage) especeField.getScene().getWindow()).close();
    }
}
