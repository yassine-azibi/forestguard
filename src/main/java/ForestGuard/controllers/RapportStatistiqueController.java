package ForestGuard.controllers;

import ForestGuard.entities.RapportStatistique;
import ForestGuard.services.RapportStatistiqueService;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class RapportStatistiqueController {

    // ── Saisie ────────────────────────────────────────────
    @FXML private ComboBox<String> comboForet;
    @FXML private ComboBox<String> comboMois;
    @FXML private ComboBox<String> comboAnnee;
    @FXML private Button           btnCalculer;
    @FXML private Label            errForet, errMois, errAnnee;

    // ── Résultats ─────────────────────────────────────────
    @FXML private VBox   panneauResultats;
    @FXML private VBox   panneauVide;
    @FXML private VBox   carteRisque;
    @FXML private Label  lblTitreResultat;
    @FXML private Label  lblDateCalcul;
    @FXML private Label  lblPourcentage;
    @FXML private Label  lblNiveauRisque;
    @FXML private Label  lblNbDanger;
    @FXML private Label  lblNbAttention;
    @FXML private Label  lblNbSur;
    @FXML private Label  lblTempMoy;
    @FXML private Label  lblHumMoy;
    @FXML private Label  lblFumeeMoy;
    @FXML private Label  lblTotalMesures;
    @FXML private Label  lblStatutSauvegarde;
    @FXML private Label  lblMessageVide;

    // ── Graphique ─────────────────────────────────────────
    @FXML private BarChart<String, Number> chartRisque;

    // ── Historique ────────────────────────────────────────
    @FXML private VBox              panneauHistorique;
    @FXML private ListView<String>  listeHistorique;

    // ── Service ───────────────────────────────────────────
    private final RapportStatistiqueService service = new RapportStatistiqueService();

    // ── Rapport courant ───────────────────────────────────
    private RapportStatistique rapportCourant = null;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final String[] NOMS_MOIS = {
            "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
            "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"
    };

    // ════════════════════════════════════════════════════════
    @FXML
    public void initialize() {

        // ── Forêts depuis la BD ───────────────────────────
        List<String> forets = service.getForetsDisponibles()
                .stream()
                .distinct()
                .sorted()
                .collect(java.util.stream.Collectors.toList());
        comboForet.setItems(FXCollections.observableArrayList(forets));
        if (!forets.isEmpty()) comboForet.setValue(forets.get(0));

        // ── Mois ──────────────────────────────────────────
        comboMois.setItems(FXCollections.observableArrayList(NOMS_MOIS));
        int moisActuel = LocalDateTime.now().getMonthValue();
        comboMois.setValue(NOMS_MOIS[moisActuel - 1]);

        // ── Années (5 ans en arrière + année courante) ────
        int anneeActuelle = LocalDateTime.now().getYear();
        for (int a = anneeActuelle; a >= anneeActuelle - 4; a--)
            comboAnnee.getItems().add(String.valueOf(a));
        comboAnnee.setValue(String.valueOf(anneeActuelle));

        // ── Style graphique ───────────────────────────────
        if (chartRisque != null) {
            chartRisque.setAnimated(true);
            chartRisque.setBarGap(2);
            chartRisque.setCategoryGap(6);
        }
    }

    // ════════════════════════════════════════════════════════
    // Calcul du rapport
    // ════════════════════════════════════════════════════════
    @FXML
    public void calculerRapport() {
        // ── Validation ───────────────────────────────────
        boolean ok = true;
        if (comboForet.getValue() == null || comboForet.getValue().isBlank()) {
            errForet.setText("Choisissez une forêt");
            ok = false;
        } else { errForet.setText(""); }

        if (comboMois.getValue() == null) {
            errMois.setText("Choisissez un mois");
            ok = false;
        } else { errMois.setText(""); }

        if (comboAnnee.getValue() == null) {
            errAnnee.setText("Choisissez une année");
            ok = false;
        } else { errAnnee.setText(""); }

        if (!ok) return;

        // ── Paramètres ───────────────────────────────────
        String foret = comboForet.getValue();
        int    mois  = java.util.Arrays.asList(NOMS_MOIS).indexOf(comboMois.getValue()) + 1;
        int    annee = Integer.parseInt(comboAnnee.getValue());

        // ── Désactiver le bouton pendant le calcul ────────
        btnCalculer.setDisable(true);
        btnCalculer.setText("Calcul en cours...");

        // ── Calcul dans un thread séparé ──────────────────
        new Thread(() -> {
            RapportStatistique rapport = service.calculerEtSauvegarder(foret, mois, annee);
            Map<String, Double> risqueParJour = (rapport != null)
                    ? service.getRisqueParJour(foret, mois, annee)
                    : null;

            javafx.application.Platform.runLater(() -> {
                btnCalculer.setDisable(false);
                btnCalculer.setText("Calculer le rapport");

                if (rapport == null) {
                    afficherPanneauVide(foret, mois, annee);
                } else {
                    rapportCourant = rapport;
                    afficherResultats(rapport, risqueParJour);
                }
            });
        }) {{ setDaemon(true); }}.start();
    }

    // ════════════════════════════════════════════════════════
    // Affichage des résultats
    // ════════════════════════════════════════════════════════
    private void afficherResultats(RapportStatistique r,
                                   Map<String, Double> risqueParJour) {
        // Cacher le panneau vide
        panneauVide.setVisible(false);
        panneauVide.setManaged(false);

        // Titre
        lblTitreResultat.setText(r.getForet() + "  —  " +
                r.getNomMois() + " " + r.getAnnee());
        if (r.getLocalisation() != null && !r.getLocalisation().isBlank())
            lblTitreResultat.setText(lblTitreResultat.getText() +
                    "  (" + r.getLocalisation() + ")");

        lblDateCalcul.setText("Calculé le " +
                (r.getDateCalcul() != null ? r.getDateCalcul().format(FMT) : ""));

        // Risque global
        String couleur = r.getCouleurRisque();
        lblPourcentage.setText(String.format("%.1f%%", r.getPourcentageIncendie()));
        lblPourcentage.setStyle("-fx-font-size: 32; -fx-font-weight: bold;" +
                "-fx-text-fill: " + couleur + ";");
        lblNiveauRisque.setText(r.getNiveauRisqueGlobal());
        lblNiveauRisque.setStyle("-fx-font-size: 13; -fx-font-weight: bold;" +
                "-fx-text-fill: white; -fx-background-color: " + couleur + ";" +
                "-fx-background-radius: 20; -fx-padding: 4 14;");
        carteRisque.setStyle("-fx-background-color: " + couleur + "22;" +
                "-fx-background-radius: 16; -fx-border-color: " + couleur + ";" +
                "-fx-border-radius: 16; -fx-border-width: 2; -fx-padding: 18 20;");

        // Compteurs
        lblNbDanger.setText(String.valueOf(r.getNbDanger()));
        lblNbAttention.setText(String.valueOf(r.getNbAttention()));
        lblNbSur.setText(String.valueOf(r.getNbSur()));
        int total = r.getNbDanger() + r.getNbAttention() + r.getNbSur();
        lblTotalMesures.setText(String.valueOf(total));

        // Moyennes
        lblTempMoy.setText(String.format("%.1f °C", r.getTempMoyenne()));
        lblHumMoy.setText(String.format("%.1f %%", r.getHumMoyenne()));
        lblFumeeMoy.setText(String.format("%.1f", r.getFumeeMoyenne()));

        // Statut sauvegarde
        if (lblStatutSauvegarde != null)
            lblStatutSauvegarde.setText("✓ Sauvegardé en base");

        // Graphique
        if (risqueParJour != null && !risqueParJour.isEmpty())
            remplirGraphique(risqueParJour, couleur);

        // Afficher le panneau avec animation
        panneauResultats.setOpacity(0);
        panneauResultats.setVisible(true);
        panneauResultats.setManaged(true);
        new Timeline(new KeyFrame(Duration.millis(300),
                new KeyValue(panneauResultats.opacityProperty(), 1.0))).play();
    }

    private void afficherPanneauVide(String foret, int mois, int annee) {
        panneauResultats.setVisible(false);
        panneauResultats.setManaged(false);
        panneauVide.setVisible(true);
        panneauVide.setManaged(true);
        if (lblMessageVide != null)
            lblMessageVide.setText("Aucune mesure trouvée pour \"" + foret +
                    "\" en " + NOMS_MOIS[mois - 1] + " " + annee);
    }

    // ════════════════════════════════════════════════════════
    // Graphique BarChart
    // ════════════════════════════════════════════════════════
    private void remplirGraphique(Map<String, Double> parJour, String couleur) {
        if (chartRisque == null) return;
        chartRisque.getData().clear();

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("Score risque");
        parJour.forEach((jour, score) ->
                serie.getData().add(new XYChart.Data<>(jour, score)));
        chartRisque.getData().add(serie);

        // Colorier les barres selon le score
        javafx.application.Platform.runLater(() -> {
            for (XYChart.Data<String, Number> data : serie.getData()) {
                double score = data.getYValue().doubleValue();
                String barColor = score >= 80 ? "#dc2626"
                        : score >= 50 ? "#f97316"
                        : score >= 25 ? "#eab308"
                        : "#16a34a";
                if (data.getNode() != null)
                    data.getNode().setStyle(
                            "-fx-bar-fill: " + barColor + ";" +
                            "-fx-background-radius: 4 4 0 0;");
            }
        });
    }

    // ════════════════════════════════════════════════════════
    // Historique
    // ════════════════════════════════════════════════════════
    @FXML
    public void afficherHistorique() {
        boolean visible = panneauHistorique.isVisible();
        panneauHistorique.setVisible(!visible);
        panneauHistorique.setManaged(!visible);

        if (!visible) {
            List<RapportStatistique> hist = service.getHistorique();
            listeHistorique.getItems().clear();
            if (hist.isEmpty()) {
                listeHistorique.getItems().add("Aucun rapport sauvegardé.");
            } else {
                for (RapportStatistique r : hist) {
                    String ligne = String.format(
                            "%-30s  %s %-4d  Risque: %5.1f%%  D:%d A:%d S:%d",
                            r.getForet(), r.getNomMois(), r.getAnnee(),
                            r.getPourcentageIncendie(),
                            r.getNbDanger(), r.getNbAttention(), r.getNbSur());
                    listeHistorique.getItems().add(ligne);
                }
            }
        }
    }

    // ════════════════════════════════════════════════════════
    // Export PDF
    // ════════════════════════════════════════════════════════
    @FXML
    public void exporterPDF() {
        if (rapportCourant == null) {
            new Alert(Alert.AlertType.WARNING,
                    "Calculez d'abord un rapport.").showAndWait();
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Sauvegarder le rapport PDF");
        fc.setInitialFileName("rapport_" +
                rapportCourant.getForet().replaceAll("\\s+", "_") + "_" +
                rapportCourant.getMois() + "_" + rapportCourant.getAnnee() + ".pdf");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        Stage stage = (Stage) btnCalculer.getScene().getWindow();
        File fichier = fc.showSaveDialog(stage);
        if (fichier == null) return;

        try {
            genererPDF(fichier, rapportCourant);
            new Alert(Alert.AlertType.INFORMATION,
                    "PDF sauvegardé : " + fichier.getAbsolutePath()).show();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR,
                    "Erreur PDF : " + e.getMessage()).show();
        }
    }

    private void genererPDF(File fichier, RapportStatistique r) throws Exception {
        org.apache.pdfbox.pdmodel.PDDocument doc =
                new org.apache.pdfbox.pdmodel.PDDocument();
        org.apache.pdfbox.pdmodel.PDPage page =
                new org.apache.pdfbox.pdmodel.PDPage(
                        org.apache.pdfbox.pdmodel.common.PDRectangle.A4);
        doc.addPage(page);

        float W = page.getMediaBox().getWidth();
        float H = page.getMediaBox().getHeight();

        org.apache.pdfbox.pdmodel.PDPageContentStream cs =
                new org.apache.pdfbox.pdmodel.PDPageContentStream(doc, page);

        // Fond blanc
        cs.setNonStrokingColor(java.awt.Color.WHITE);
        cs.addRect(0, 0, W, H); cs.fill();

        // Header vert
        cs.setNonStrokingColor(new java.awt.Color(21, 128, 61));
        cs.addRect(0, H - 70, W, 70); cs.fill();

        // Titre
        cs.setNonStrokingColor(java.awt.Color.WHITE);
        cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 16);
        cs.beginText();
        cs.newLineAtOffset(30, H - 38);
        cs.showText("ForestGuard — Rapport Statistique d'Incendie");
        cs.endText();

        cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 11);
        cs.beginText();
        cs.newLineAtOffset(30, H - 58);
        cs.showText(r.getForet() + "  |  " + r.getNomMois() + " " + r.getAnnee());
        cs.endText();

        // Contenu
        float y = H - 100;
        cs.setNonStrokingColor(new java.awt.Color(15, 23, 42));

        String[] lignes = {
            "Foret          : " + r.getForet(),
            "Localisation   : " + (r.getLocalisation() != null ? r.getLocalisation() : "-"),
            "Periode        : " + r.getNomMois() + " " + r.getAnnee(),
            "",
            "RISQUE GLOBAL  : " + String.format("%.1f%%", r.getPourcentageIncendie()) +
                    "  (" + r.getNiveauRisqueGlobal() + ")",
            "",
            "Mesures Danger    : " + r.getNbDanger(),
            "Mesures Attention : " + r.getNbAttention(),
            "Mesures Sur       : " + r.getNbSur(),
            "Total mesures     : " + (r.getNbDanger() + r.getNbAttention() + r.getNbSur()),
            "",
            "Temperature moyenne : " + String.format("%.1f C", r.getTempMoyenne()),
            "Humidite moyenne    : " + String.format("%.1f %%", r.getHumMoyenne()),
            "Fumee moyenne       : " + String.format("%.1f", r.getFumeeMoyenne()),
            "",
            "Date du calcul : " + (r.getDateCalcul() != null ?
                    r.getDateCalcul().format(FMT) : "-"),
        };

        cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 12);
        for (String ligne : lignes) {
            if (ligne.startsWith("RISQUE")) {
                cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 13);
                cs.setNonStrokingColor(new java.awt.Color(220, 38, 38));
            } else {
                cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 12);
                cs.setNonStrokingColor(new java.awt.Color(15, 23, 42));
            }
            cs.beginText();
            cs.newLineAtOffset(40, y);
            cs.showText(ligne);
            cs.endText();
            y -= 22;
        }

        // Footer
        cs.setNonStrokingColor(new java.awt.Color(21, 128, 61));
        cs.addRect(0, 0, W, 28); cs.fill();
        cs.setNonStrokingColor(java.awt.Color.WHITE);
        cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 9);
        cs.beginText();
        cs.newLineAtOffset(30, 10);
        cs.showText("ForestGuard — Systeme de Surveillance Intelligente des Forets");
        cs.endText();

        cs.close();
        doc.save(fichier);
        doc.close();
    }

    // ════════════════════════════════════════════════════════
    @FXML
    public void fermer() {
        ((Stage) btnCalculer.getScene().getWindow()).close();
    }
}
