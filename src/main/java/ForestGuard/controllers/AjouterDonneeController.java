package ForestGuard.controllers;

import ForestGuard.entities.DonCapteur;
import ForestGuard.services.DonCapteurService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class AjouterDonneeController {

    // ── Champs formulaire ─────────────────────────────────
    @FXML private Label             titreLabel;
    @FXML private ChoiceBox<String> capteurCombo;
    @FXML private ChoiceBox<String> foretCombo;
    @FXML private DatePicker        datePicker;
    @FXML private TextField         heureField;
    @FXML private TextField         tempField;
    @FXML private TextField         humField;
    @FXML private ChoiceBox<String> fumeeCombo;
    @FXML private Label             risqueLabel;
    @FXML private Button            btnEnregistrer;

    // ── Labels erreurs ────────────────────────────────────
    @FXML private Label errCapteur;
    @FXML private Label errForet;
    @FXML private Label errDate;
    @FXML private Label errHeure;
    @FXML private Label errTemp;
    @FXML private Label errHum;
    @FXML private Label errFumee;

    // ── Barre de progression ──────────────────────────────
    @FXML private ProgressBar barreProgression;
    @FXML private Label       lblProgression;

    private final DonCapteurService service = new DonCapteurService();
    private DonCapteur donneeEnEdition = null;
    private boolean    modeEdition     = false;

    // ════════════════════════════════════════════════════════
    @FXML
    public void initialize() {

        // ── Charger capteurs depuis la BD ─────────────────
        List<String> capteursBD = service.getNomsCapteursBD();
        if (capteursBD.isEmpty()) {
            // Fallback si la table capteur est vide
            capteurCombo.getItems().addAll(
                    "S001", "S002", "S003", "S004", "S005",
                    "Capteur-01", "Capteur-02", "Capteur-03", "Capteur-04");
        } else {
            capteurCombo.getItems().addAll(capteursBD);
        }

        // ── Charger forêts depuis la BD ───────────────────
        List<String> foretsBD = service.getNomsForetsBD();
        if (foretsBD.isEmpty()) {
            // Fallback si la table foret est vide
            foretCombo.getItems().addAll(
                    "Foret de Chenes", "Foret Mixte", "Pinede Sud",
                    "Centre", "Nord-Est", "Ouest");
        } else {
            foretCombo.getItems().addAll(foretsBD);
        }

        fumeeCombo.getItems().addAll("Aucune", "Niveau 1", "Niveau 2");
        fumeeCombo.setValue("Aucune");

        datePicker.setValue(java.time.LocalDate.now());
        heureField.setText(LocalTime.now()
                .format(DateTimeFormatter.ofPattern("HH:mm")));

        // ── Validation en temps réel ──────────────────────

        // Température — uniquement chiffres
        tempField.textProperty().addListener((o, old, val) -> {
            // Bloquer les lettres
            if (!val.matches("-?\\d*\\.?\\d*")) {
                tempField.setText(old);
                return;
            }
            validerTemp();
            mettreAJourRisque();
            mettreAJourProgression();
        });

        // Humidité — uniquement chiffres 0-100
        humField.textProperty().addListener((o, old, val) -> {
            if (!val.matches("\\d*\\.?\\d*")) {
                humField.setText(old);
                return;
            }
            validerHum();
            mettreAJourRisque();
            mettreAJourProgression();
        });

        // Heure
        heureField.textProperty().addListener((o, old, val) -> {
            validerHeure();
            mettreAJourProgression();
        });

        // ChoiceBox
        capteurCombo.valueProperty().addListener((o, old, val) -> {
            validerChoiceBox(capteurCombo, errCapteur, "Capteur obligatoire");
            mettreAJourProgression();
        });
        foretCombo.valueProperty().addListener((o, old, val) -> {
            validerChoiceBox(foretCombo, errForet, "Foret obligatoire");
            mettreAJourProgression();
        });
        fumeeCombo.valueProperty().addListener((o, old, val) -> {
            mettreAJourRisque();
            mettreAJourProgression();
        });
        datePicker.valueProperty().addListener((o, old, val) -> {
            validerDate();
            mettreAJourProgression();
        });
    }

    // ════════════════════════════════════════════════════════
    // Validations individuelles
    // ════════════════════════════════════════════════════════

    private boolean validerTemp() {
        String val = tempField.getText().trim();
        if (val.isEmpty()) {
            setErreur(tempField, errTemp, "Temperature obligatoire");
            return false;
        }
        try {
            double t = Double.parseDouble(val);
            if (t < -50 || t > 80) {
                setErreur(tempField, errTemp,
                        "Temperature doit etre entre -50 et 80 C");
                return false;
            }
            setOk(tempField, errTemp);
            return true;
        } catch (NumberFormatException e) {
            setErreur(tempField, errTemp, "Entrez un nombre valide (ex: 28.5)");
            return false;
        }
    }

    private boolean validerHum() {
        String val = humField.getText().trim();
        if (val.isEmpty()) {
            setErreur(humField, errHum, "Humidite obligatoire");
            return false;
        }
        try {
            double h = Double.parseDouble(val);
            if (h < 0 || h > 100) {
                setErreur(humField, errHum,
                        "Humidite doit etre entre 0 et 100 %");
                return false;
            }
            setOk(humField, errHum);
            return true;
        } catch (NumberFormatException e) {
            setErreur(humField, errHum, "Entrez un nombre valide (ex: 45)");
            return false;
        }
    }

    private boolean validerHeure() {
        String val = heureField.getText().trim();
        if (val.isEmpty()) {
            setErreur(heureField, errHeure, "Heure obligatoire");
            return false;
        }
        try {
            LocalTime.parse(val, DateTimeFormatter.ofPattern("HH:mm"));
            setOk(heureField, errHeure);
            return true;
        } catch (DateTimeParseException e) {
            setErreur(heureField, errHeure,
                    "Format invalide — utilisez HH:mm (ex: 14:30)");
            return false;
        }
    }

    private boolean validerDate() {
        if (datePicker.getValue() == null) {
            errDate.setText("Date obligatoire");
            return false;
        }
        errDate.setText("");
        return true;
    }

    private boolean validerChoiceBox(ChoiceBox<String> cb,
                                     Label errLabel, String msg) {
        if (cb.getValue() == null || cb.getValue().isEmpty()) {
            errLabel.setText(msg);
            cb.setStyle("-fx-background-color: white;" +
                    "-fx-border-color: #ef4444;" +
                    "-fx-border-radius: 8; -fx-background-radius: 8;");
            return false;
        }
        errLabel.setText("");
        cb.setStyle("-fx-background-color: white;" +
                "-fx-border-color: #22c55e;" +
                "-fx-border-radius: 8; -fx-background-radius: 8;");
        return true;
    }

    // ════════════════════════════════════════════════════════
    // Styles OK / Erreur sur les champs
    // ════════════════════════════════════════════════════════

    private void setErreur(TextField field, Label errLabel, String msg) {
        errLabel.setText("⚠ " + msg);
        field.setStyle(
                "-fx-background-color: #fff5f5;" +
                        "-fx-border-color: #ef4444;" +
                        "-fx-border-radius: 8; -fx-background-radius: 8;" +
                        "-fx-padding: 9 12; -fx-font-size: 13;");
    }

    private void setOk(TextField field, Label errLabel) {
        errLabel.setText("");
        field.setStyle(
                "-fx-background-color: #f0fdf4;" +
                        "-fx-border-color: #22c55e;" +
                        "-fx-border-radius: 8; -fx-background-radius: 8;" +
                        "-fx-padding: 9 12; -fx-font-size: 13;");
    }

    // ════════════════════════════════════════════════════════
    // Barre de progression
    // ════════════════════════════════════════════════════════

    private void mettreAJourProgression() {
        int total = 6;
        int remplis = 0;

        if (capteurCombo.getValue() != null) remplis++;
        if (foretCombo.getValue()   != null) remplis++;
        if (datePicker.getValue()   != null) remplis++;
        if (!heureField.getText().trim().isEmpty()) remplis++;
        if (!tempField.getText().trim().isEmpty()
                && validerTempSilent())     remplis++;
        if (!humField.getText().trim().isEmpty()
                && validerHumSilent())      remplis++;

        double progress = (double) remplis / total;
        barreProgression.setProgress(progress);
        lblProgression.setText(remplis + " / " + total + " champs remplis");

        // Couleur de la barre
        String couleur = progress < 0.4 ? "#ef4444" :
                progress < 0.8 ? "#f97316" : "#15803d";
        barreProgression.setStyle(
                "-fx-accent: " + couleur + ";" +
                        "-fx-background-color: #e2e8f0;" +
                        "-fx-background-radius: 4; -fx-background-insets: 0;");

        // Bouton actif seulement si tout est rempli
        btnEnregistrer.setDisable(remplis < total);
        if (remplis >= total) {
            btnEnregistrer.setStyle(
                    "-fx-background-color: #15803d;" +
                            "-fx-text-fill: white; -fx-font-size: 13;" +
                            "-fx-font-weight: bold; -fx-padding: 11 28;" +
                            "-fx-background-radius: 10; -fx-cursor: hand;" +
                            "-fx-effect: dropshadow(gaussian," +
                            "rgba(21,128,61,0.4),10,0,0,3);");
        } else {
            btnEnregistrer.setStyle(
                    "-fx-background-color: #9ca3af;" +
                            "-fx-text-fill: white; -fx-font-size: 13;" +
                            "-fx-font-weight: bold; -fx-padding: 11 28;" +
                            "-fx-background-radius: 10; -fx-cursor: default;");
        }
    }

    private boolean validerTempSilent() {
        try {
            double t = Double.parseDouble(tempField.getText().trim());
            return t >= -50 && t <= 80;
        } catch (Exception e) { return false; }
    }

    private boolean validerHumSilent() {
        try {
            double h = Double.parseDouble(humField.getText().trim());
            return h >= 0 && h <= 100;
        } catch (Exception e) { return false; }
    }

    // ════════════════════════════════════════════════════════
    // Calcul risque automatique
    // ════════════════════════════════════════════════════════
    private void mettreAJourRisque() {
        try {
            double t = Double.parseDouble(tempField.getText().trim());
            double h = Double.parseDouble(humField.getText().trim());
            String nf = fumeeCombo.getValue();
            double f = "Niveau 2".equals(nf) ? 90 :
                    "Niveau 1".equals(nf) ? 40 : 0;

            String risque, couleur, bg;
            if (t > 35 && h < 30 || f >= 90) {
                risque = "CRITIQUE"; couleur = "#dc2626"; bg = "#fef2f2";
            } else if (t > 30 || h < 40 || f >= 40) {
                risque = "ELEVE";    couleur = "#f97316"; bg = "#fff7ed";
            } else if (t > 25 || h < 55) {
                risque = "MOYEN";    couleur = "#eab308"; bg = "#fefce8";
            } else {
                risque = "FAIBLE";   couleur = "#16a34a"; bg = "#f0fdf4";
            }

            risqueLabel.setText(risque);
            risqueLabel.setStyle(
                    "-fx-font-size: 14; -fx-font-weight: bold;" +
                            "-fx-text-fill: " + couleur + ";" +
                            "-fx-background-color: " + bg + ";" +
                            "-fx-background-radius: 8;" +
                            "-fx-border-color: " + couleur + ";" +
                            "-fx-border-radius: 8; -fx-border-width: 1;" +
                            "-fx-padding: 9 12;");
        } catch (Exception ignored) {
            risqueLabel.setText("Remplissez les champs...");
            risqueLabel.setStyle(
                    "-fx-font-size: 13; -fx-text-fill: #94a3b8;" +
                            "-fx-background-color: #f8fafc;" +
                            "-fx-background-radius: 8;" +
                            "-fx-border-color: #e2e8f0;" +
                            "-fx-border-radius: 8; -fx-padding: 9 12;");
        }
    }

    // ════════════════════════════════════════════════════════
    // Valider et enregistrer
    // ════════════════════════════════════════════════════════
    @FXML
    public void validerEtAjouter(ActionEvent event) {

        // Valider tous les champs
        boolean ok = true;
        ok &= validerChoiceBox(capteurCombo, errCapteur, "Capteur obligatoire");
        ok &= validerChoiceBox(foretCombo, errForet, "Foret obligatoire");
        ok &= validerDate();
        ok &= validerHeure();
        ok &= validerTemp();
        ok &= validerHum();
        ok &= validerChoiceBox(fumeeCombo, errFumee, "Niveau fumee obligatoire");

        if (!ok) {
            agiterBouton();
            return;
        }

        try {
            double temperature = Double.parseDouble(tempField.getText().trim());
            double humidite    = Double.parseDouble(humField.getText().trim());
            String nf          = fumeeCombo.getValue();
            double fumee = "Niveau 2".equals(nf) ? 90 :
                    "Niveau 1".equals(nf) ? 40 : 0;

            String risque = risqueLabel.getText();

            // ── Construire l'horodatage ───────────────────
            LocalDateTime horodatage;
            try {
                LocalTime heure = LocalTime.parse(
                        heureField.getText().trim(),
                        DateTimeFormatter.ofPattern("HH:mm"));
                horodatage = LocalDateTime.of(datePicker.getValue(), heure);
            } catch (Exception e) {
                horodatage = LocalDateTime.now();
            }

            // ── Contrainte : pas de doublon capteur + même minute ──
            if (!modeEdition) {
                if (service.existeDejaPourCapteurEtTemps(capteurCombo.getValue(), horodatage)) {
                    afficherErreur(
                            "Une donnee existe deja pour le capteur \"" +
                            capteurCombo.getValue() +
                            "\" a cette date/heure (" +
                            horodatage.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) +
                            ").\nVeuillez changer l'heure ou le capteur.");
                    return;
                }
            }

            DonCapteur d = new DonCapteur(
                    modeEdition && donneeEnEdition != null
                            ? donneeEnEdition.getType() : "Manuelle",
                    capteurCombo.getValue(),
                    foretCombo.getValue(),   // zone = forêt choisie
                    temperature, humidite, fumee, risque);
            d.setHorodatage(horodatage);

            if (modeEdition && donneeEnEdition != null) {
                service.updateEntity(donneeEnEdition.getId(), d);
            } else {
                service.addEntity(d);
            }

            fermerModal();

        } catch (Exception e) {
            afficherErreur("Erreur : " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════
    // Animation secousse si erreur
    // ════════════════════════════════════════════════════════
    private void agiterBouton() {
        javafx.animation.TranslateTransition tt =
                new javafx.animation.TranslateTransition(
                        javafx.util.Duration.millis(60), btnEnregistrer);
        tt.setByX(8);
        tt.setCycleCount(6);
        tt.setAutoReverse(true);
        tt.play();
    }

    // ════════════════════════════════════════════════════════
    public void remplirPourEdition(DonCapteur d) {
        this.donneeEnEdition = d;
        this.modeEdition     = true;
        if (titreLabel != null) titreLabel.setText("Modifier la Donnee");

        capteurCombo.setValue(d.getCapteur());
        foretCombo.setValue(d.getZone());
        if (d.getHorodatage() != null) {
            datePicker.setValue(d.getHorodatage().toLocalDate());
            heureField.setText(d.getHorodatage().toLocalTime()
                    .format(DateTimeFormatter.ofPattern("HH:mm")));
        }
        tempField.setText(String.valueOf(d.getTemperature()));
        humField.setText(String.valueOf(d.getHumidite()));
        double f = d.getFumee();
        fumeeCombo.setValue(f == 0 ? "Aucune" : f < 50 ? "Niveau 1" : "Niveau 2");
        mettreAJourProgression();
    }

    @FXML
    public void annuler(ActionEvent event) { fermerModal(); }

    private void fermerModal() {
        ((Stage) tempField.getScene().getWindow()).close();
    }

    private void afficherErreur(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Erreur");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}