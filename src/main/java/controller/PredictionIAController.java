package controller;

import ai.RisqueMLModel;
import service.CapteurSimulator;
import utils.NavigationManager;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PredictionIAController implements Initializable {

    // ── Saisie manuelle ───────────────────────────────────────────────────────
    @FXML private Slider sliderTemp, sliderFumee, sliderHum, sliderHeure;
    @FXML private Label  lblSliderTemp, lblSliderFumee, lblSliderHum, lblSliderHeure;

    // ── Résultat prédiction manuelle ──────────────────────────────────────────
    @FXML private Label       lblRisqueLabel, lblRisquePct, lblExplication;
    @FXML private ProgressBar progressRisque;
    @FXML private ProgressBar probFaible, probModere, probEleve, probCritique;
    @FXML private Label       pctFaible, pctModere, pctEleve, pctCritique;

    // ── Prédictions temps réel capteurs ──────────────────────────────────────
    @FXML private Label       lblRisque1, lblRisque2, lblRisque3;
    @FXML private ProgressBar barRisque1, barRisque2, barRisque3;
    @FXML private Label       pctRisque1, pctRisque2, pctRisque3;
    @FXML private Label       lblExpl1, lblExpl2, lblExpl3;

    // ── Statut modèle + historique ────────────────────────────────────────────
    @FXML private Label                  lblStatutModele;
    @FXML private ListView<String>       listeHistorique;

    private final ObservableList<String> historique = FXCollections.observableArrayList();

    // ── Services ──────────────────────────────────────────────────────────────
    private RisqueMLModel      modele;
    private CapteurSimulator   simulator;
    private ScheduledExecutorService scheduler;

    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        listeHistorique.setItems(historique);
        listeHistorique.setStyle("-fx-background-color:#0a1a0e;-fx-control-inner-background:#0a1a0e;");
        lblStatutModele.setText("Entrainement en cours...");

        // Entraîner le modèle dans un thread séparé (évite de bloquer l'UI)
        new Thread(() -> {
            modele = new RisqueMLModel();
            Platform.runLater(() -> {
                lblStatutModele.setText("Modele pret — 600 exemples, 500 epochs");
                lblStatutModele.setStyle("-fx-text-fill:#4ade80;-fx-font-size:11;" +
                        "-fx-background-color:rgba(0,0,0,0.4);-fx-background-radius:6;-fx-padding:5 10;");
            });
        }, "ml-train").start();

        // Lier les sliders aux labels
        sliderTemp.valueProperty().addListener((o, ov, nv) ->
            lblSliderTemp.setText(String.format("%.0f C", nv.doubleValue())));
        sliderFumee.valueProperty().addListener((o, ov, nv) ->
            lblSliderFumee.setText(String.format("%.0f ppm", nv.doubleValue())));
        sliderHum.valueProperty().addListener((o, ov, nv) ->
            lblSliderHum.setText(String.format("%.0f %%", nv.doubleValue())));
        sliderHeure.valueProperty().addListener((o, ov, nv) ->
            lblSliderHeure.setText(String.format("%.0fh", nv.doubleValue())));

        // Démarrer le simulateur pour les prédictions temps réel
        simulator = new CapteurSimulator();
        simulator.demarrer(3, this::mettreAJourPredictionsTempsReel);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PRÉDICTION MANUELLE (bouton)
    // ══════════════════════════════════════════════════════════════════════════

    @FXML
    private void predireManuel() {
        if (modele == null) {
            lblExplication.setText("Modele en cours d'entrainement, patientez...");
            return;
        }
        double temp  = sliderTemp.getValue();
        double fumee = sliderFumee.getValue();
        double hum   = sliderHum.getValue();
        int    heure = (int) sliderHeure.getValue();

        RisqueMLModel.ResultatPrediction res = modele.predire(temp, fumee, hum, heure);
        afficherResultatManuel(res, temp, fumee, hum);

        // Ajouter à l'historique
        String entry = LocalTime.now().format(HM) + "  [Manuel]  "
                + res.getLabel() + "  " + res.getPourcentage() + "%"
                + "  T:" + String.format("%.0f", temp) + "C"
                + "  F:" + String.format("%.0f", fumee) + "ppm"
                + "  H:" + String.format("%.0f", hum) + "%";
        ajouterHistorique(entry, res.classe);
    }

    private void afficherResultatManuel(RisqueMLModel.ResultatPrediction res,
                                         double temp, double fumee, double hum) {
        lblRisqueLabel.setText(res.getLabel());
        lblRisqueLabel.setStyle("-fx-text-fill:" + res.getCouleur() +
                ";-fx-font-size:28;-fx-font-weight:bold;");

        progressRisque.setProgress(res.scoreRisque);
        progressRisque.setStyle("-fx-accent:" + res.getCouleur() + ";");
        lblRisquePct.setText(res.getPourcentage() + "% de risque");
        lblRisquePct.setStyle("-fx-text-fill:" + res.getCouleur() + ";-fx-font-size:12;");
        lblExplication.setText(res.getExplication());

        // Barres de probabilité
        probFaible.setProgress(res.probas[0]);
        pctFaible.setText(String.format("%.0f%%", res.probas[0]*100));
        probModere.setProgress(res.probas[1]);
        pctModere.setText(String.format("%.0f%%", res.probas[1]*100));
        probEleve.setProgress(res.probas[2]);
        pctEleve.setText(String.format("%.0f%%", res.probas[2]*100));
        probCritique.setProgress(res.probas[3]);
        pctCritique.setText(String.format("%.0f%%", res.probas[3]*100));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PRÉDICTIONS TEMPS RÉEL depuis le simulateur
    // ══════════════════════════════════════════════════════════════════════════

    private void mettreAJourPredictionsTempsReel(CapteurSimulator.DonneesCapteur[] donnees) {
        if (modele == null) return;
        int heure = LocalTime.now().getHour();

        for (int i = 0; i < 3; i++) {
            CapteurSimulator.DonneesCapteur d = donnees[i];
            RisqueMLModel.ResultatPrediction res =
                modele.predire(d.temp, d.fumee, d.humidite, heure);

            final int idx = i;
            final RisqueMLModel.ResultatPrediction r = res;
            final CapteurSimulator.DonneesCapteur  dc = d;

            // Déjà sur le JavaFX thread (Platform.runLater dans simulator)
            afficherCapteurIA(idx, r, dc);

            // Ajouter à l'historique si risque >= Élevé
            if (res.classe >= 2) {
                String entry = LocalTime.now().format(HM)
                        + "  [C-00" + (i+1) + "]  "
                        + res.getLabel() + "  " + res.getPourcentage() + "%"
                        + "  T:" + String.format("%.0f", d.temp) + "C"
                        + "  F:" + String.format("%.0f", d.fumee) + "ppm";
                ajouterHistorique(entry, res.classe);
            }
        }
    }

    private void afficherCapteurIA(int idx, RisqueMLModel.ResultatPrediction res,
                                    CapteurSimulator.DonneesCapteur d) {
        String couleur = res.getCouleur();
        String label   = res.getLabel();
        int    pct     = res.getPourcentage();
        String bg      = "rgba(" + hexToRgb(couleur) + ",0.15)";
        String expl    = res.getExplication();

        switch (idx) {
            case 0 -> {
                lblRisque1.setText(label);
                lblRisque1.setStyle("-fx-text-fill:" + couleur + ";-fx-font-size:13;-fx-font-weight:bold;"
                        + "-fx-background-color:" + bg + ";-fx-background-radius:6;-fx-padding:3 10;");
                barRisque1.setProgress(res.scoreRisque);
                barRisque1.setStyle("-fx-accent:" + couleur + ";");
                pctRisque1.setText(pct + "%");
                lblExpl1.setText(expl);
            }
            case 1 -> {
                lblRisque2.setText(label);
                lblRisque2.setStyle("-fx-text-fill:" + couleur + ";-fx-font-size:13;-fx-font-weight:bold;"
                        + "-fx-background-color:" + bg + ";-fx-background-radius:6;-fx-padding:3 10;");
                barRisque2.setProgress(res.scoreRisque);
                barRisque2.setStyle("-fx-accent:" + couleur + ";");
                pctRisque2.setText(pct + "%");
                lblExpl2.setText(expl);
            }
            case 2 -> {
                lblRisque3.setText(label);
                lblRisque3.setStyle("-fx-text-fill:" + couleur + ";-fx-font-size:13;-fx-font-weight:bold;"
                        + "-fx-background-color:" + bg + ";-fx-background-radius:6;-fx-padding:3 10;");
                barRisque3.setProgress(res.scoreRisque);
                barRisque3.setStyle("-fx-accent:" + couleur + ";");
                pctRisque3.setText(pct + "%");
                lblExpl3.setText(expl);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  HISTORIQUE
    // ══════════════════════════════════════════════════════════════════════════

    private void ajouterHistorique(String entry, int classe) {
        Platform.runLater(() -> {
            historique.add(0, entry);
            if (historique.size() > 20) historique.remove(20, historique.size());
        });
    }

    // ── Helper couleur ────────────────────────────────────────────────────────
    private String hexToRgb(String hex) {
        hex = hex.replace("#", "");
        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);
        return r + "," + g + "," + b;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  NAVIGATION
    // ══════════════════════════════════════════════════════════════════════════

    @FXML private void goDashboard() {
        arreter(); Stage s = (Stage) lblRisqueLabel.getScene().getWindow();
        NavigationManager.navigateTo(s, "/fxml/Dashboard.fxml");
    }
    @FXML private void goCapteurs() {
        arreter(); Stage s = (Stage) lblRisqueLabel.getScene().getWindow();
        NavigationManager.navigateTo(s, "/fxml/Capteurs.fxml");
    }
    @FXML private void goAjout() {
        arreter(); Stage s = (Stage) lblRisqueLabel.getScene().getWindow();
        NavigationManager.navigateTo(s, "/fxml/AjoutAlerte.fxml");
    }

    private void arreter() {
        if (simulator  != null) simulator.arreter();
        if (scheduler  != null) scheduler.shutdown();
    }
}
