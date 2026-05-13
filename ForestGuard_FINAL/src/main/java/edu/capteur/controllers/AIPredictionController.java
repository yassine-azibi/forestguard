package edu.capteur.controllers;

import edu.capteur.entities.Capteur;
import edu.capteur.services.AIPredictionService;
import edu.capteur.services.MeteoService;
import edu.capteur.services.OllamaService;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

/**
 * ForestGuard – Contrôleur Prédiction IA
 * Sans batterie — risque basé sur statut + température + humidité.
 */
public class AIPredictionController {

    // ── Infos capteur
    @FXML private Label lblNomCapteur;
    @FXML private Label lblTypeCapteur;
    @FXML private Label lblLocCapteur;

    // ── Météo
    @FXML private Label lblMeteoIcone;
    @FXML private Label lblMeteoTemp;
    @FXML private Label lblMeteoHumidite;
    @FXML private Label lblMeteoVent;
    @FXML private Label lblMeteoDesc;
    @FXML private Label lblMeteoRisqueIncendie;
    @FXML private Label lblSeuilTemp;
    @FXML private Label lblSeuilHumid;
    @FXML private VBox  boxMeteo;

    // ── Résultat IA
    @FXML private Label       lblChargement;
    @FXML private VBox        boxResultat;
    @FXML private Label       lblScoreValeur;
    @FXML private ProgressBar barScore;
    @FXML private Label       lblNiveauBadge;
    @FXML private Label       lblAnalyse;
    @FXML private Label       lblAction1;
    @FXML private Label       lblAction2;
    @FXML private Label       lblAction3;
    @FXML private Label       lblDelai;
    @FXML private Label       lblSourceIA;
    @FXML private Label       lblHorodatage;

    private Capteur capteur;
    private String  donneeRecente;

    private final AIPredictionService aiService = new AIPredictionService();

    public void setCapteur(Capteur capteur, int batterieIgnore, String donneeRecente) {
        this.capteur       = capteur;
        this.donneeRecente = donneeRecente;

        if (lblNomCapteur  != null) lblNomCapteur.setText("Capteur #C" + capteur.getId() + " – " + capteur.getNom());
        if (lblTypeCapteur != null) lblTypeCapteur.setText(capteur.getType());
        if (lblLocCapteur  != null) lblLocCapteur.setText("📍 " + capteur.getLocalisation());

        lancerAnalyse();
    }

    private void lancerAnalyse() {
        if (lblChargement != null) lblChargement.setVisible(true);
        if (boxResultat   != null) boxResultat.setVisible(false);
        if (boxMeteo      != null) boxMeteo.setVisible(false);

        CompletableFuture
            .supplyAsync(() -> aiService.analyserComplet(capteur, donneeRecente))
            .thenAccept(r -> Platform.runLater(() -> afficherResultat(r)))
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    MeteoService.DonneesMeteo mv = new MeteoService.DonneesMeteo();
                    OllamaService.ResultatIA  ia = new OllamaService().analyseLocaleEnrichie(capteur, mv);
                    afficherResultat(new AIPredictionService.ResultatComplet(
                        ia, mv, false, mv.temperature, mv.humidite));
                });
                return null;
            });
    }

    private void afficherResultat(AIPredictionService.ResultatComplet r) {
        if (lblChargement != null) lblChargement.setVisible(false);

        afficherMeteo(r.meteo);

        // ── Score + jauge animée ──
        int score = r.ia.scoreRisque;
        if (lblScoreValeur != null) lblScoreValeur.setText(score + "/100");

        if (barScore != null) {
            barScore.setProgress(0);
            String barColor = score >= 70 ? "#ef4444"
                            : score >= 50 ? "#f59e0b"
                            : score >= 30 ? "#eab308" : "#16a34a";
            barScore.setStyle("-fx-accent: " + barColor + ";");
            Timeline anim = new Timeline(new KeyFrame(Duration.millis(800),
                    new KeyValue(barScore.progressProperty(), score / 100.0)));
            anim.play();
        }

        // ── Badge niveau ──
        if (lblNiveauBadge != null) {
            String[] s = niveauStyle(r.ia.niveau);
            lblNiveauBadge.setText(s[0] + "  " + r.ia.niveau);
            lblNiveauBadge.setStyle(s[1]);
        }

        if (lblAnalyse  != null) lblAnalyse.setText(r.ia.analyse);
        if (lblAction1  != null) lblAction1.setText("①  " + r.ia.action1);
        if (lblAction2  != null) lblAction2.setText("②  " + r.ia.action2);
        if (lblAction3  != null) lblAction3.setText("③  " + r.ia.action3);
        if (lblDelai    != null) lblDelai.setText("⏱  Délai recommandé : " + r.ia.delaiEstime);

        // ── Source des données ──
        if (lblSourceIA != null) {
            String sourceTexte;
            if (r.mesureBD) {
                sourceTexte = "📊 Données BD capteur | Temp: " +
                    String.format("%.1f°C", r.tempCapteur) +
                    " | Humidité: " + r.humidCapteur + "%";
                if (r.ollamaActif) sourceTexte += " | 🦙 Ollama Llama3";
            } else {
                sourceTexte = r.ollamaActif
                    ? "🦙 Ollama Llama3 (local) | Météo simulée"
                    : "⚡ Analyse locale | Météo simulée (aucune mesure BD)";
            }
            lblSourceIA.setText(sourceTexte);
            lblSourceIA.setStyle(r.mesureBD
                ? "-fx-font-size:10;-fx-text-fill:#bbf7d0;"
                : "-fx-font-size:10;-fx-text-fill:rgba(255,255,255,0.6);");
        }

        if (lblHorodatage != null)
            lblHorodatage.setText("Analysé le " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        if (boxResultat != null) {
            boxResultat.setVisible(true);
            boxResultat.setOpacity(0);
            new Timeline(new KeyFrame(Duration.millis(350),
                    new KeyValue(boxResultat.opacityProperty(), 1.0))).play();
        }
    }

    private void afficherMeteo(MeteoService.DonneesMeteo meteo) {
        if (boxMeteo == null) return;

        if (lblMeteoIcone    != null) lblMeteoIcone.setText(meteo.icone);
        if (lblMeteoTemp     != null) {
            String couleurTemp = meteo.temperature > MeteoService.TEMP_MAX_CRITIQUE ? "#dc2626"
                               : meteo.temperature > MeteoService.TEMP_MAX_NORMAL   ? "#f59e0b"
                               : "#0284c7";
            lblMeteoTemp.setText(String.format("%.1f°C", meteo.temperature));
            lblMeteoTemp.setStyle("-fx-font-size:15;-fx-font-weight:bold;-fx-text-fill:" + couleurTemp + ";");
        }
        if (lblMeteoHumidite != null) {
            String couleurHum = meteo.humidite > MeteoService.HUMID_MAX_CRITIQUE ? "#dc2626"
                              : meteo.humidite > MeteoService.HUMID_MAX_NORMAL   ? "#f59e0b"
                              : "#0284c7";
            lblMeteoHumidite.setText(meteo.humidite + "%");
            lblMeteoHumidite.setStyle("-fx-font-size:15;-fx-font-weight:bold;-fx-text-fill:" + couleurHum + ";");
        }
        if (lblMeteoVent  != null) lblMeteoVent.setText(String.format("%.0f km/h", meteo.vitesseVent));
        if (lblMeteoDesc  != null) lblMeteoDesc.setText(meteo.description);

        // Seuils dépassés
        if (lblSeuilTemp != null) {
            if (meteo.temperature > MeteoService.TEMP_MAX_CRITIQUE)
                lblSeuilTemp.setText("⚠ > " + (int)MeteoService.TEMP_MAX_CRITIQUE + "°C critique");
            else if (meteo.temperature > MeteoService.TEMP_MAX_NORMAL)
                lblSeuilTemp.setText("⚠ > " + (int)MeteoService.TEMP_MAX_NORMAL + "°C attention");
            else
                lblSeuilTemp.setText("✓ < " + (int)MeteoService.TEMP_MAX_NORMAL + "°C normal");
            lblSeuilTemp.setStyle(meteo.temperature > MeteoService.TEMP_MAX_CRITIQUE
                ? "-fx-font-size:10;-fx-text-fill:#dc2626;-fx-font-weight:bold;"
                : meteo.temperature > MeteoService.TEMP_MAX_NORMAL
                ? "-fx-font-size:10;-fx-text-fill:#f59e0b;-fx-font-weight:bold;"
                : "-fx-font-size:10;-fx-text-fill:#16a34a;");
        }
        if (lblSeuilHumid != null) {
            if (meteo.humidite > MeteoService.HUMID_MAX_CRITIQUE)
                lblSeuilHumid.setText("⚠ > " + MeteoService.HUMID_MAX_CRITIQUE + "% critique");
            else if (meteo.humidite > MeteoService.HUMID_MAX_NORMAL)
                lblSeuilHumid.setText("⚠ > " + MeteoService.HUMID_MAX_NORMAL + "% attention");
            else
                lblSeuilHumid.setText("✓ < " + MeteoService.HUMID_MAX_NORMAL + "% normal");
            lblSeuilHumid.setStyle(meteo.humidite > MeteoService.HUMID_MAX_CRITIQUE
                ? "-fx-font-size:10;-fx-text-fill:#dc2626;-fx-font-weight:bold;"
                : meteo.humidite > MeteoService.HUMID_MAX_NORMAL
                ? "-fx-font-size:10;-fx-text-fill:#f59e0b;-fx-font-weight:bold;"
                : "-fx-font-size:10;-fx-text-fill:#16a34a;");
        }

        if (lblMeteoRisqueIncendie != null) {
            String niv = meteo.niveauRisqueIncendie();
            String[] s = niveauStyle(niv);
            lblMeteoRisqueIncendie.setText(s[0] + "  " + niv);
            String bgMeteo = switch (niv) {
                case "CRITIQUE" -> "-fx-background-color:#fee2e2;-fx-text-fill:#dc2626;";
                case "ÉLEVÉ"    -> "-fx-background-color:#fef3c7;-fx-text-fill:#d97706;";
                case "MODÉRÉ"   -> "-fx-background-color:#fefce8;-fx-text-fill:#ca8a04;";
                default         -> "-fx-background-color:#dcfce7;-fx-text-fill:#16a34a;";
            };
            lblMeteoRisqueIncendie.setStyle(bgMeteo +
                "-fx-font-size:10;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:3 10;");
        }

        boxMeteo.setVisible(true);
    }

    private String[] niveauStyle(String niveau) {
        if (niveau == null) return new String[]{"✅",
            "-fx-background-color:#dcfce7;-fx-text-fill:#16a34a;-fx-font-weight:bold;" +
            "-fx-font-size:12;-fx-background-radius:20;-fx-padding:6 16;"};
        return switch (niveau.toUpperCase()) {
            case "CRITIQUE" -> new String[]{"🔴",
                "-fx-background-color:#fee2e2;-fx-text-fill:#dc2626;-fx-font-weight:bold;" +
                "-fx-font-size:12;-fx-background-radius:20;-fx-padding:6 16;"};
            case "ÉLEVÉ", "ELEVE" -> new String[]{"⚠️",
                "-fx-background-color:#fef3c7;-fx-text-fill:#d97706;-fx-font-weight:bold;" +
                "-fx-font-size:12;-fx-background-radius:20;-fx-padding:6 16;"};
            case "MODÉRÉ", "MODERE" -> new String[]{"🟡",
                "-fx-background-color:#fefce8;-fx-text-fill:#ca8a04;-fx-font-weight:bold;" +
                "-fx-font-size:12;-fx-background-radius:20;-fx-padding:6 16;"};
            default -> new String[]{"✅",
                "-fx-background-color:#dcfce7;-fx-text-fill:#16a34a;-fx-font-weight:bold;" +
                "-fx-font-size:12;-fx-background-radius:20;-fx-padding:6 16;"};
        };
    }

    @FXML public void relancerAnalyse() { lancerAnalyse(); }

    @FXML
    public void planifierMaintenance() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ajout_maintenance.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            stage.sizeToScene();
            stage.setTitle("Planifier une maintenance – " + capteur.getNom());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    public void fermer() {
        ((Stage) lblNomCapteur.getScene().getWindow()).close();
    }
}
