package ForestGuard.controllers;

import ForestGuard.entities.Anomalie;
import ForestGuard.entities.DonCapteur;
import ForestGuard.entities.JournalAction;
import ForestGuard.ia.PredicteurIncendie;
import ForestGuard.services.*;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

public class DonnesController {

    // ─── Sidebar ──────────────────────────────────────────
    @FXML private VBox   sidebar;
    @FXML private VBox   logoNom;
    @FXML private VBox   navBox;
    @FXML private Label  lblReducer;
    @FXML private Button btnToggle;
    @FXML private Button btnDB, btnFR, btnCP, btnDN, btnAL, btnIN, btnUS;

    // ─── Stats cards ──────────────────────────────────────
    @FXML private VBox  cardTemp, cardHum, cardFumee, cardTotal;
    @FXML private Label statTemp, statHum, statFumee, labelTotalDonnees;

    // ─── Graphiques ───────────────────────────────────────
    @FXML private AreaChart<String, Number> chartEvolution;
    @FXML private AreaChart<String, Number> chartTemp24h;
    @FXML private AreaChart<String, Number> chartHum24h;
    @FXML private CategoryAxis              xAxisChart;
    @FXML private NumberAxis                yAxisChart;

    // ─── Onglets ──────────────────────────────────────────
    @FXML private Button tabDonnees, tabAnomalies, tabJournal;
    @FXML private VBox   vueDonnees, vueAnomalies, vueJournal;

    // ─── Listes ───────────────────────────────────────────
    @FXML private ListView<DonCapteur>    listeDonnees;
    @FXML private ListView<Anomalie>      listeAnomalies;
    @FXML private ListView<JournalAction> listeJournal;
    @FXML private Label                   nbAnomalies;

    // ─── Filtres ──────────────────────────────────────────
    @FXML private DatePicker        dateDebut;
    @FXML private ChoiceBox<String> foretFiltre;
    @FXML private ChoiceBox<String> capteurFiltre;
    @FXML private ChoiceBox<String> typeFiltre;
    @FXML private TextField         champRecherche;
    @FXML private Button            btnEffacerRecherche;
    @FXML private Label             lblNbResultats;

    // ─── IA ───────────────────────────────────────────────
    @FXML private VBox        cardIA;
    @FXML private Label       statIA, statIAPct;
    @FXML private HBox        banniereIA;
    @FXML private Label       iaEmoji, iaMessage, iaPrediction2h, iaReco;
    @FXML private ProgressBar barreRisque;
    @FXML private ListView<String> listeRisqueForets;

    // ─── Météo ────────────────────────────────────────────
    @FXML private HBox  widgetMeteo;
    @FXML private Label meteoEmoji, meteoVille, meteoDesc;
    @FXML private Label meteoTemp, meteoHum, meteoVent;
    @FXML private Label meteoMinMax, meteoRessenti;
    @FXML private Label meteoImpact, meteoImpactDetail;

    // ─── ESP32 ────────────────────────────────────────────
    @FXML private ChoiceBox<String> portCombo;
    @FXML private ChoiceBox<String> baudrateCombo;
    @FXML private Button            btnConnecterESP;
    @FXML private Label             lblStatutESP;
    @FXML private CheckBox          cbSauvegarderAuto;
    @FXML private Label             lblDernieredonnee;
    @FXML private ChoiceBox<String> capteurESPCombo;

    @FXML private VBox      espCardTemp, espCardHum, espCardFumee;
    @FXML private Label     espTemp, espHum, espFumee;
    @FXML private TextField champRechercheForet;
    @FXML private ListView<String> listeResultatsForets;
    @FXML private VBox      boxForetSelectionnee;
    @FXML private Label     lblForetSelectNom, lblForetSelectTemp;
    @FXML private Label     lblForetSelectHum, lblForetSelectNb;

    // ─── State ────────────────────────────────────────────
    private boolean sidebarExpanded = true;
    private static final double W_CLOSED = 72;
    private static final double W_OPEN   = 240;

    // ─── Services ─────────────────────────────────────────
    private final DonCapteurService        service         = new DonCapteurService();
    private final AnomalieService          anomalieService = new AnomalieService();
    private final JournalActionService     journalService  = new JournalActionService();
    private final RapportInspectionService rapportService  = new RapportInspectionService();
    private final ESP32Service             esp32           = new ESP32Service();

    // ─── Formats date ─────────────────────────────────────
    private static final DateTimeFormatter FMT       = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FMT_DAY   = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter FMT_H     = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FMT_HEURE = DateTimeFormatter.ofPattern("HH:mm:ss");
    // ─── Timer sauvegarde ─────────────────────────────────
    private LocalDateTime derniereSauvegarde = LocalDateTime.now();
    private static final int INTERVALLE_SAUVEGARDE_MINUTES = 10;
    // ════════════════════════════════════════════════════════
    @FXML
    public void initialize() {

        // ── Filtres depuis la BD ─────────────────────────────
        chargerFiltresDepuisBD();

        typeFiltre.getItems().addAll("Tous", "Automatique", "Manuelle");
        typeFiltre.setValue("Tous");
        dateDebut.setValue(LocalDate.now().minusDays(30));

        // ── Recherche en temps réel ───────────────────────────
        if (champRecherche != null) {
            champRecherche.textProperty().addListener((obs, old, val) -> {
                // Recherche instantanée sur les données déjà chargées
                filtrerEnTempsReel(val);
            });
        }

        // Graphiques
        styleChart(chartEvolution);
        styleChart(chartTemp24h);
        styleChart(chartHum24h);

        // Hover nav
        Button[] navBtns = {btnDB, btnFR, btnCP, btnAL, btnIN, btnUS};
        for (Button b : navBtns) {
            if (b == null) continue;
            b.setOnMouseEntered(e -> b.setStyle(
                    "-fx-background-color: rgba(22,163,74,0.15);" +
                            "-fx-text-fill: #4ade80; -fx-font-size: 13;" +
                            "-fx-alignment: " + (sidebarExpanded ? "CENTER_LEFT" : "CENTER") +
                            "; -fx-padding: 10 16; -fx-background-radius: 12; -fx-cursor: hand;"));
            b.setOnMouseExited(e -> b.setStyle(
                    "-fx-background-color: transparent;" +
                            "-fx-text-fill: #94a3b8; -fx-font-size: 13;" +
                            "-fx-alignment: " + (sidebarExpanded ? "CENTER_LEFT" : "CENTER") +
                            "; -fx-padding: 10 16; -fx-background-radius: 12; -fx-cursor: hand;"));
        }

        // Hover cartes stats
        VBox[] cards = {cardTemp, cardHum, cardFumee, cardTotal};
        for (VBox card : cards) {
            if (card == null) continue;
            String base = card.getStyle();
            card.setOnMouseEntered(e -> card.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.18);" +
                            "-fx-background-radius: 20;" +
                            "-fx-border-color: rgba(255,255,255,0.35);" +
                            "-fx-border-radius: 20; -fx-border-width: 1;" +
                            "-fx-padding: 20 24; -fx-cursor: hand;" +
                            "-fx-scale-x: 1.04; -fx-scale-y: 1.04;" +
                            "-fx-effect: dropshadow(gaussian,rgba(22,163,74,0.3),16,0,0,4);"));
            card.setOnMouseExited(e -> card.setStyle(base));
        }

        // La cellFactory de listeDonnees est définie dans afficherDonneesGroupees()
        // qui est appelée par chargerDonnees() — ne pas la définir ici

        // ListView anomalies
        listeAnomalies.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Anomalie a, boolean empty) {
                super.updateItem(a, empty);
                if (empty || a == null) {
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent; -fx-padding: 4 0;");
                    return;
                }
                String iconTxt, iconBg;
                if (a.getTypeAnomalie().contains("Temperature")) {
                    iconTxt = "T"; iconBg = "#ef4444";
                } else if (a.getTypeAnomalie().contains("Humidite")) {
                    iconTxt = "H"; iconBg = "#3b82f6";
                } else {
                    iconTxt = "F"; iconBg = "#f97316";
                }
                Label icone = new Label(iconTxt);
                icone.setStyle("-fx-background-color: " + iconBg + ";" +
                        "-fx-text-fill: white; -fx-font-size: 16;" +
                        "-fx-font-weight: bold; -fx-background-radius: 12;" +
                        "-fx-padding: 10 14; -fx-min-width: 46; -fx-alignment: CENTER;");
                Label point = new Label("•");
                point.setStyle("-fx-text-fill: #f97316; -fx-font-size: 16;");
                Label titre = new Label(a.getTypeAnomalie());
                titre.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
                HBox titreLigne = new HBox(6, titre, point);
                titreLigne.setAlignment(Pos.CENTER_LEFT);
                Label sous = new Label(a.getForet() + " — Capteur " + a.getCapteur());
                sous.setStyle("-fx-font-size: 12; -fx-text-fill: #64748b;");
                Label heure = new Label(a.getDateDetection() != null ?
                        a.getDateDetection().format(FMT_H) : "");
                heure.setStyle("-fx-font-size: 11; -fx-text-fill: #94a3b8;");
                Label valeur = new Label(String.format("%.1f", a.getValeurDetectee()));
                valeur.setStyle("-fx-font-size: 12; -fx-text-fill: #ef4444; -fx-font-weight: bold;");
                VBox textes = new VBox(4, titreLigne, sous, heure);
                textes.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(textes, javafx.scene.layout.Priority.ALWAYS);
                Button btnT = new Button(a.isTraite() ? "Traite" : "Traiter");
                btnT.setDisable(a.isTraite());
                btnT.setStyle(a.isTraite()
                        ? "-fx-background-color: #dcfce7; -fx-text-fill: #16a34a;" +
                        "-fx-background-radius: 20; -fx-font-size: 11;" +
                        "-fx-font-weight: bold; -fx-padding: 6 14; -fx-cursor: hand;"
                        : "-fx-background-color: #fee2e2; -fx-text-fill: #dc2626;" +
                        "-fx-background-radius: 20; -fx-font-size: 11;" +
                        "-fx-font-weight: bold; -fx-padding: 6 14; -fx-cursor: hand;");
                btnT.setOnAction(e -> {
                    anomalieService.marquerTraite(a.getId());
                    journalService.enregistrer("Admin", "Traitement",
                            "Anomalie traitee : " + a.getTypeAnomalie(), a.getDonneeId());
                    chargerAnomalies();
                });
                HBox carte = new HBox(14, icone, textes, valeur, btnT);
                carte.setAlignment(Pos.CENTER_LEFT);
                carte.setStyle("-fx-padding: 16 20;");
                String cardBg = a.isTraite() ? "#f0fdf4" :
                        (a.getTypeAnomalie().contains("Temperature") ? "#fff7ed" : "#fff1f2");
                String cardBorder = a.isTraite() ? "#bbf7d0" :
                        (a.getTypeAnomalie().contains("Temperature") ? "#fed7aa" : "#fecdd3");
                setStyle("-fx-background-color: " + cardBg + ";" +
                        "-fx-background-radius: 16; -fx-border-color: " + cardBorder + ";" +
                        "-fx-border-radius: 16; -fx-border-width: 1; -fx-padding: 4 8;");
                setOnMouseEntered(e -> setStyle(
                        "-fx-background-color: white; -fx-background-radius: 16;" +
                                "-fx-border-color: " + iconBg + "; -fx-border-radius: 16;" +
                                "-fx-border-width: 1; -fx-padding: 4 8;" +
                                "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.12),10,0,0,3);"));
                setOnMouseExited(e -> setStyle(
                        "-fx-background-color: " + cardBg + ";" +
                                "-fx-background-radius: 16; -fx-border-color: " + cardBorder + ";" +
                                "-fx-border-radius: 16; -fx-border-width: 1; -fx-padding: 4 8;"));
                setGraphic(carte);
            }
        });

        // ListView journal
        listeJournal.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(JournalAction j, boolean empty) {
                super.updateItem(j, empty);
                if (empty || j == null) {
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent; -fx-padding: 4 0;");
                    return;
                }
                String iconTxt, iconBg, cardBg, cardBorder;
                switch (j.getAction() != null ? j.getAction() : "") {
                    case "Ajout"       -> { iconTxt = "+"; iconBg = "#16a34a";
                        cardBg = "#f0fdf4"; cardBorder = "#bbf7d0"; }
                    case "Suppression" -> { iconTxt = "X"; iconBg = "#ef4444";
                        cardBg = "#fff1f2"; cardBorder = "#fecdd3"; }
                    case "Export"      -> { iconTxt = "E"; iconBg = "#3b82f6";
                        cardBg = "#eff6ff"; cardBorder = "#bfdbfe"; }
                    case "Traitement"  -> { iconTxt = "T"; iconBg = "#f97316";
                        cardBg = "#fff7ed"; cardBorder = "#fed7aa"; }
                    default            -> { iconTxt = "A"; iconBg = "#8b5cf6";
                        cardBg = "#f5f3ff"; cardBorder = "#ddd6fe"; }
                }
                Label icone = new Label(iconTxt);
                icone.setStyle("-fx-background-color: " + iconBg + ";" +
                        "-fx-text-fill: white; -fx-font-size: 15;" +
                        "-fx-font-weight: bold; -fx-background-radius: 12;" +
                        "-fx-padding: 10 14; -fx-min-width: 46; -fx-alignment: CENTER;");
                Label lblAction = new Label(j.getAction());
                lblAction.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
                Label lblUser = new Label("Admin");
                lblUser.setStyle("-fx-font-size: 11; -fx-background-color: #f0fdf4;" +
                        "-fx-text-fill: #16a34a; -fx-font-weight: bold;" +
                        "-fx-background-radius: 20; -fx-padding: 2 8;");
                HBox topLine = new HBox(8, lblAction, lblUser);
                topLine.setAlignment(Pos.CENTER_LEFT);
                Label lblDetails = new Label(j.getDetails());
                lblDetails.setStyle("-fx-font-size: 12; -fx-text-fill: #64748b;");
                lblDetails.setWrapText(true);
                Label lblDate = new Label(j.getDateAction() != null ?
                        j.getDateAction().format(FMT) : "");
                lblDate.setStyle("-fx-font-size: 11; -fx-text-fill: #94a3b8;");
                VBox textes = new VBox(4, topLine, lblDetails, lblDate);
                HBox.setHgrow(textes, javafx.scene.layout.Priority.ALWAYS);
                HBox carte = new HBox(14, icone, textes);
                carte.setAlignment(Pos.CENTER_LEFT);
                carte.setStyle("-fx-padding: 14 20;");
                String fCb = cardBg, fCbd = cardBorder, fIb = iconBg;
                setStyle("-fx-background-color: " + fCb + "; -fx-background-radius: 16;" +
                        "-fx-border-color: " + fCbd + "; -fx-border-radius: 16;" +
                        "-fx-border-width: 1; -fx-padding: 4 8;");
                setOnMouseEntered(e -> setStyle(
                        "-fx-background-color: white; -fx-background-radius: 16;" +
                                "-fx-border-color: " + fIb + "; -fx-border-radius: 16;" +
                                "-fx-border-width: 1; -fx-padding: 4 8;" +
                                "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.12),10,0,0,3);"));
                setOnMouseExited(e -> setStyle(
                        "-fx-background-color: " + fCb + "; -fx-background-radius: 16;" +
                                "-fx-border-color: " + fCbd + "; -fx-border-radius: 16;" +
                                "-fx-border-width: 1; -fx-padding: 4 8;"));
                setGraphic(carte);
            }
        });

        chargerDonnees();
        chargerAnomalies();
        chargerJournal();
        actualiserMeteo();
        demarrerESP32Auto();
        initialiserRechercheForets();
        // Dans initialize(), après chargerFiltresDepuisBD() :
        if (capteurESPCombo != null) {
            List<String> capteurs = service.getNomsCapteurs();
            capteurESPCombo.getItems().addAll(capteurs);
            if (!capteurs.isEmpty())
                capteurESPCombo.setValue(capteurs.get(0));

            // Écouter le changement de capteur
            capteurESPCombo.getSelectionModel().selectedItemProperty()
                    .addListener((obs, old, val) -> {
                        if (val != null) {
                            // Filtrer les données live par capteur sélectionné
                            System.out.println("[ESP32] Capteur sélectionné : " + val);
                        }
                    });
        }


    }

    // ════════════════════════════════════════════════════════
    // Onglets
    // ════════════════════════════════════════════════════════
    @FXML public void switchToDonnees() {
        activerTab(tabDonnees); desactiverTab(tabAnomalies); desactiverTab(tabJournal);
        afficher(vueDonnees); cacher(vueAnomalies); cacher(vueJournal);
    }
    @FXML public void switchToAnomalies() {
        desactiverTab(tabDonnees); activerTab(tabAnomalies); desactiverTab(tabJournal);
        cacher(vueDonnees); afficher(vueAnomalies); cacher(vueJournal);
        chargerAnomalies();
    }
    @FXML public void switchToJournal() {
        desactiverTab(tabDonnees); desactiverTab(tabAnomalies); activerTab(tabJournal);
        cacher(vueDonnees); cacher(vueAnomalies); afficher(vueJournal);
        chargerJournal();
    }
    private void activerTab(Button tab) {
        if (tab == null) return;
        tab.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white;" +
                "-fx-font-size: 13; -fx-font-weight: bold; -fx-padding: 10 26;" +
                "-fx-background-radius: 12; -fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian,rgba(22,163,74,0.5),10,0,0,3);");
    }

    private void desactiverTab(Button tab) {
        if (tab == null) return;
        tab.setStyle("-fx-background-color: transparent;" +
                "-fx-text-fill: rgba(255,255,255,0.65);" +
                "-fx-font-size: 13; -fx-padding: 10 26;" +
                "-fx-background-radius: 12; -fx-cursor: hand;");
        // Hover effect
        tab.setOnMouseEntered(e -> {
            if (!tab.getStyle().contains("#16a34a"))
                tab.setStyle("-fx-background-color: rgba(255,255,255,0.12);" +
                        "-fx-text-fill: white; -fx-font-size: 13;" +
                        "-fx-padding: 10 26; -fx-background-radius: 12; -fx-cursor: hand;");
        });
        tab.setOnMouseExited(e -> {
            if (!tab.getStyle().contains("#16a34a"))
                tab.setStyle("-fx-background-color: transparent;" +
                        "-fx-text-fill: rgba(255,255,255,0.65);" +
                        "-fx-font-size: 13; -fx-padding: 10 26;" +
                        "-fx-background-radius: 12; -fx-cursor: hand;");
        });
    }
    private void afficher(VBox vue) {
        if (vue == null) return;
        vue.setVisible(true); vue.setManaged(true); vue.setOpacity(0);
        new Timeline(new KeyFrame(Duration.millis(200),
                new KeyValue(vue.opacityProperty(), 1.0))).play();
    }
    private void cacher(VBox vue) {
        if (vue == null) return;
        vue.setVisible(false); vue.setManaged(false);
    }

    // ════════════════════════════════════════════════════════
    // Chargements ← MODIFIÉ
    // ════════════════════════════════════════════════════════
    private void chargerDonnees() {
        List<DonCapteur> raw = service.getDataAvecForet();
        System.out.println("chargerDonnees : " + raw.size() + " donnees chargees");
        // Ne pas appeler listeDonnees.setItems ici — afficherDonneesGroupees s'en charge
        anomalieService.detecterEtSauvegarder(raw);
        if (labelTotalDonnees != null)
            labelTotalDonnees.setText(String.valueOf(raw.size()));
        if (!raw.isEmpty()) {
            double avgT = raw.stream().mapToDouble(DonCapteur::getTemperature).average().orElse(0);
            double avgH = raw.stream().mapToDouble(DonCapteur::getHumidite).average().orElse(0);
            long nb = anomalieService.getNombreNonTraites();
            if (statTemp  != null) statTemp.setText(String.format("%.1fC", avgT));
            if (statHum   != null) statHum.setText(String.format("%.0f%%", avgH));
            if (statFumee != null) statFumee.setText(String.valueOf(nb));
        }
        chargerGraphiques(raw);
        mettreAJourIA(raw);
        afficherRisqueParForet(raw);
        afficherDonneesGroupees(raw);
    }

    private void chargerAnomalies() {
        List<Anomalie> all = anomalieService.getAll();
        listeAnomalies.setItems(FXCollections.observableArrayList(all));
        long nb = all.stream().filter(a -> !a.isTraite()).count();
        if (nbAnomalies != null) {
            nbAnomalies.setText(nb + " non traitee(s)");
            nbAnomalies.setStyle("-fx-font-size: 12; -fx-font-weight: bold;" +
                    "-fx-background-radius: 20; -fx-padding: 4 12;" +
                    (nb > 0 ? "-fx-text-fill: #dc2626; -fx-background-color: #fee2e2;"
                            : "-fx-text-fill: #16a34a; -fx-background-color: #dcfce7;"));
        }
    }

    private void chargerJournal() {
        listeJournal.setItems(FXCollections.observableArrayList(journalService.getAll()));
    }

    // ════════════════════════════════════════════════════════
    // Graphiques
    // ════════════════════════════════════════════════════════
    private void styleChart(AreaChart<String, Number> chart) {
        if (chart == null) return;
        chart.setAnimated(true);
        chart.setCreateSymbols(false);
    }

    private void chargerGraphiques(List<DonCapteur> donnees) {
        if (chartEvolution != null) {
            chartEvolution.getData().clear();
            Map<String, List<DonCapteur>> parJour = new TreeMap<>();
            for (DonCapteur d : donnees) {
                if (d.getHorodatage() == null) continue;
                parJour.computeIfAbsent(d.getHorodatage().format(FMT_DAY),
                        k -> new java.util.ArrayList<>()).add(d);
            }
            XYChart.Series<String, Number> sT = new XYChart.Series<>();
            XYChart.Series<String, Number> sH = new XYChart.Series<>();
            XYChart.Series<String, Number> sF = new XYChart.Series<>();
            sT.setName("Temperature"); sH.setName("Humidite"); sF.setName("Fumee");
            for (Map.Entry<String, List<DonCapteur>> e : parJour.entrySet()) {
                List<DonCapteur> l = e.getValue();
                sT.getData().add(new XYChart.Data<>(e.getKey(),
                        l.stream().mapToDouble(DonCapteur::getTemperature).average().orElse(0)));
                sH.getData().add(new XYChart.Data<>(e.getKey(),
                        l.stream().mapToDouble(DonCapteur::getHumidite).average().orElse(0)));
                sF.getData().add(new XYChart.Data<>(e.getKey(),
                        l.stream().mapToDouble(DonCapteur::getFumee).average().orElse(0)));
            }
            chartEvolution.getData().addAll(sT, sH, sF);
            javafx.application.Platform.runLater(() -> {
                colorerSerie(chartEvolution, 0, "#ef4444", "rgba(239,68,68,0.15)");
                colorerSerie(chartEvolution, 1, "#3b82f6", "rgba(59,130,246,0.15)");
                colorerSerie(chartEvolution, 2, "#6b7280", "rgba(107,114,128,0.10)");
            });
        }
        if (chartTemp24h != null) {
            chartTemp24h.getData().clear();
            XYChart.Series<String, Number> s = new XYChart.Series<>();
            donnees.stream().filter(d -> d.getHorodatage() != null)
                    .sorted((a, b) -> a.getHorodatage().compareTo(b.getHorodatage()))
                    .limit(20)
                    .forEach(d -> s.getData().add(
                            new XYChart.Data<>(d.getHorodatage().format(FMT_DAY), d.getTemperature())));
            chartTemp24h.getData().add(s);
            javafx.application.Platform.runLater(() ->
                    colorerSerie(chartTemp24h, 0, "#ef4444", "rgba(239,68,68,0.18)"));
        }
        if (chartHum24h != null) {
            chartHum24h.getData().clear();
            XYChart.Series<String, Number> s = new XYChart.Series<>();
            donnees.stream().filter(d -> d.getHorodatage() != null)
                    .sorted((a, b) -> a.getHorodatage().compareTo(b.getHorodatage()))
                    .limit(20)
                    .forEach(d -> s.getData().add(
                            new XYChart.Data<>(d.getHorodatage().format(FMT_DAY), d.getHumidite())));
            chartHum24h.getData().add(s);
            javafx.application.Platform.runLater(() ->
                    colorerSerie(chartHum24h, 0, "#3b82f6", "rgba(59,130,246,0.18)"));
        }
    }

    private void colorerSerie(AreaChart<String, Number> chart, int index,
                              String stroke, String fill) {
        if (chart.getData().size() <= index) return;
        XYChart.Series<String, Number> serie = chart.getData().get(index);
        if (serie.getNode() == null) return;
        javafx.scene.Node line = serie.getNode().lookup(".chart-series-area-line");
        javafx.scene.Node area = serie.getNode().lookup(".chart-series-area-fill");
        if (line != null) line.setStyle("-fx-stroke: " + stroke + "; -fx-stroke-width: 2.5;");
        if (area != null) area.setStyle("-fx-fill: " + fill + "; -fx-stroke: transparent;");
    }

    // ════════════════════════════════════════════════════════
    // Sidebar
    // ════════════════════════════════════════════════════════
    @FXML
    public void toggleSidebar() {
        sidebarExpanded = !sidebarExpanded;
        Timeline anim = new Timeline(new KeyFrame(Duration.millis(220),
                new KeyValue(sidebar.prefWidthProperty(),
                        sidebarExpanded ? W_OPEN : W_CLOSED)));
        if (sidebarExpanded) {
            anim.setOnFinished(e -> {
                if (logoNom    != null) { logoNom.setVisible(true);    logoNom.setManaged(true);    }
                if (lblReducer != null) { lblReducer.setVisible(true); lblReducer.setManaged(true); }
                if (btnToggle  != null) btnToggle.setText("<");
                setBoutonTextes(true);
            });
        } else {
            if (logoNom    != null) { logoNom.setVisible(false);    logoNom.setManaged(false);    }
            if (lblReducer != null) { lblReducer.setVisible(false); lblReducer.setManaged(false); }
            if (btnToggle  != null) btnToggle.setText(">");
            setBoutonTextes(false);
        }
        anim.play();
    }

    private void setBoutonTextes(boolean expanded) {
        Object[][] btns = {
                {btnDB, "  Tableau de Bord", "DB"},
                {btnFR, "  Forets",          "FR"},
                {btnCP, "  Capteurs",        "CP"},
                {btnDN, "  Donnees",         "DN"},
                {btnAL, "  Alertes",         "AL"},
                {btnIN, "  Interventions",   "IN"},
                {btnUS, "  Utilisateurs",    "US"}
        };
        for (Object[] entry : btns) {
            Button b = (Button) entry[0];
            if (b == null) continue;
            b.setText((String)(expanded ? entry[1] : entry[2]));
            b.setPrefWidth(expanded ? W_OPEN - 20 : 52);
            boolean isActive = b == btnDN;
            b.setStyle("-fx-background-color: " + (isActive ? "#16a34a" : "transparent") + ";" +
                    "-fx-text-fill: " + (isActive ? "white" : "#94a3b8") + "; -fx-font-size: 13;" +
                    (isActive ? "-fx-font-weight: bold;" : "") +
                    "-fx-alignment: " + (expanded ? "CENTER_LEFT" : "CENTER") + ";" +
                    "-fx-padding: 10 16; -fx-background-radius: 12; -fx-cursor: hand;" +
                    (isActive ? "-fx-effect: dropshadow(gaussian,rgba(22,163,74,0.4),10,0,0,3);" : ""));
        }
    }

    // ════════════════════════════════════════════════════════
    // Actions FXML
    // ════════════════════════════════════════════════════════
    @FXML
    public void appliquerFiltres(ActionEvent event) {
        LocalDateTime debut = dateDebut.getValue() != null
                ? dateDebut.getValue().atStartOfDay()
                : LocalDateTime.now().minusDays(30);
        String recherche = (champRecherche != null) ? champRecherche.getText() : null;
        List<DonCapteur> raw = service.getFiltered(debut, LocalDateTime.now(),
                foretFiltre.getValue(), capteurFiltre.getValue(), recherche);
        // Filtrer par type si nécessaire
        if (typeFiltre.getValue() != null && !typeFiltre.getValue().equals("Tous")) {
            String type = typeFiltre.getValue();
            raw = raw.stream()
                    .filter(d -> type.equalsIgnoreCase(d.getType()))
                    .collect(java.util.stream.Collectors.toList());
        }
        if (labelTotalDonnees != null) labelTotalDonnees.setText(String.valueOf(raw.size()));
        if (lblNbResultats != null) {
            lblNbResultats.setText(raw.size() + " résultat(s)");
            lblNbResultats.setVisible(true);
        }
        chargerGraphiques(raw);
        mettreAJourIA(raw);
        afficherRisqueParForet(raw);
        afficherDonneesGroupees(raw);
    }

    @FXML
    public void reinitialiserFiltres() {
        if (champRecherche != null) champRecherche.clear();
        if (foretFiltre    != null) foretFiltre.setValue("Toutes");
        if (capteurFiltre  != null) capteurFiltre.setValue("Tous");
        if (typeFiltre     != null) typeFiltre.setValue("Tous");
        if (dateDebut      != null) dateDebut.setValue(LocalDate.now().minusDays(30));
        if (lblNbResultats != null) lblNbResultats.setVisible(false);
        chargerDonnees();
    }

    @FXML
    public void effacerRecherche() {
        if (champRecherche != null) champRecherche.clear();
    }

    // Filtre en temps réel sur les données déjà dans la ListView
    private void filtrerEnTempsReel(String texte) {
        if (texte == null || texte.trim().isEmpty()) {
            // Pas de texte → afficher tout
            chargerDonnees();
            return;
        }
        String lower = texte.trim().toLowerCase();
        // Filtrer la liste actuelle sans requête BD
        List<DonCapteur> toutesLesDonnees = service.getDataSimple();
        List<DonCapteur> filtrees = toutesLesDonnees.stream()
                .filter(d -> {
                    String cap  = d.getCapteur()  != null ? d.getCapteur().toLowerCase()  : "";
                    String zone = d.getZone()     != null ? d.getZone().toLowerCase()     : "";
                    String foret= d.getForet()    != null ? d.getForet().toLowerCase()    : "";
                    String type = d.getType()     != null ? d.getType().toLowerCase()     : "";
                    return cap.contains(lower) || zone.contains(lower)
                            || foret.contains(lower) || type.contains(lower);
                })
                .collect(java.util.stream.Collectors.toList());
        if (lblNbResultats != null) {
            lblNbResultats.setText(filtrees.size() + " résultat(s)");
            lblNbResultats.setVisible(true);
        }
        afficherDonneesGroupees(filtrees);
    }

    @FXML
    public void ouvrirRapportStatistique() {
        try {
            java.net.URL url = getClass().getResource("/RapportStatistique.fxml");
            if (url == null) { System.err.println("RapportStatistique.fxml introuvable"); return; }
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(url);
            javafx.scene.Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Rapport Statistique d'Incendie");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(true);
            stage.setMinWidth(860);
            stage.setMinHeight(600);
            stage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    public void ouvrirModalAjouter() {
        try {
            java.net.URL url = getClass().getResource("/AjouterDonne.fxml");
            if (url == null) { System.err.println("AjouterDonne.fxml introuvable"); return; }
            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Ajouter une donnee");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();
            journalService.enregistrer("Admin", "Ajout", "Nouvelle donnee ajoutee", 0);
            // Recharger aussi les filtres capteurs/forêts après ajout
            rafraichirFiltres();
            chargerDonnees(); chargerAnomalies(); chargerJournal();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void ouvrirModalEditer(DonCapteur donnee) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjouterDonne.fxml"));
            Parent root = loader.load();
            AjouterDonneeController ctrl = loader.getController();
            ctrl.remplirPourEdition(donnee);
            Stage modal = new Stage();
            modal.setTitle("Modifier");
            modal.setScene(new Scene(root));
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setResizable(false);
            modal.showAndWait();
            journalService.enregistrer("Admin", "Modification",
                    "Donnee modifiee : capteur " + donnee.getCapteur(), donnee.getId());
            chargerDonnees(); chargerJournal();
        } catch (IOException e) { afficherErreur("Erreur : " + e.getMessage()); }
    }

    private void supprimerDonnee(DonCapteur donnee) {
        // ── Dialog stylisé ForestGuard ────────────────────
        javafx.scene.layout.VBox contenu = new javafx.scene.layout.VBox(16);
        contenu.setStyle("-fx-background-color: white; -fx-padding: 0;");

        // Header rouge
        javafx.scene.layout.HBox header = new javafx.scene.layout.HBox(12);
        header.setStyle("-fx-background-color: #dc2626;" +
                "-fx-padding: 18 24; -fx-background-radius: 12 12 0 0;");
        header.setAlignment(Pos.CENTER_LEFT);
        Label iconeHeader = new Label("🗑");
        iconeHeader.setStyle("-fx-font-size: 20;");
        Label titreHeader = new Label("Supprimer la donnée");
        titreHeader.setStyle("-fx-font-size: 16; -fx-font-weight: bold;" +
                "-fx-text-fill: white;");
        header.getChildren().addAll(iconeHeader, titreHeader);

        // Corps
        javafx.scene.layout.VBox corps = new javafx.scene.layout.VBox(10);
        corps.setStyle("-fx-padding: 20 24;");

        Label question = new Label("Êtes-vous sûr de vouloir supprimer cette mesure ?");
        question.setStyle("-fx-font-size: 13; -fx-text-fill: #374151;");
        question.setWrapText(true);

        // Carte info donnée
        javafx.scene.layout.VBox carteInfo = new javafx.scene.layout.VBox(6);
        carteInfo.setStyle("-fx-background-color: #fef2f2;" +
                "-fx-background-radius: 10;" +
                "-fx-border-color: #fecaca; -fx-border-radius: 10;" +
                "-fx-border-width: 1; -fx-padding: 12 16;");

        String foretAff = (donnee.getForet() != null && !donnee.getForet().isEmpty())
                ? donnee.getForet() : (donnee.getZone() != null ? donnee.getZone() : "—");
        String dateAff  = donnee.getHorodatage() != null
                ? donnee.getHorodatage().format(FMT) : "—";

        Label[] infos = {
            new Label("Capteur  :  " + donnee.getCapteur()),
            new Label("Forêt    :  " + foretAff),
            new Label("Date     :  " + dateAff),
            new Label(String.format("Temp: %.1f°C  |  Hum: %.0f%%  |  Fumée: %s",
                    donnee.getTemperature(), donnee.getHumidite(),
                    donnee.getFumee() == 0 ? "Aucune"
                            : donnee.getFumee() < 50 ? "Niveau 1" : "Niveau 2"))
        };
        for (Label l : infos) {
            l.setStyle("-fx-font-size: 12; -fx-text-fill: #7f1d1d; -fx-font-weight: bold;");
            carteInfo.getChildren().add(l);
        }

        Label avertissement = new Label("⚠  Cette action est irréversible.");
        avertissement.setStyle("-fx-font-size: 11; -fx-text-fill: #dc2626;" +
                "-fx-font-weight: bold;");

        corps.getChildren().addAll(question, carteInfo, avertissement);

        // Boutons
        javafx.scene.layout.HBox boutons = new javafx.scene.layout.HBox(10);
        boutons.setAlignment(Pos.CENTER_RIGHT);
        boutons.setStyle("-fx-padding: 0 24 20 24;");

        Button btnAnnuler = new Button("Annuler");
        btnAnnuler.setStyle("-fx-background-color: white;" +
                "-fx-text-fill: #374151; -fx-font-size: 12; -fx-font-weight: bold;" +
                "-fx-padding: 10 24; -fx-background-radius: 10;" +
                "-fx-border-color: #d1d5db; -fx-border-radius: 10; -fx-cursor: hand;");

        Button btnConfirmer = new Button("🗑  Supprimer");
        btnConfirmer.setStyle("-fx-background-color: #dc2626;" +
                "-fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold;" +
                "-fx-padding: 10 24; -fx-background-radius: 10;" +
                "-fx-border-color: #b91c1c; -fx-border-radius: 10; -fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian,rgba(220,38,38,0.4),8,0,0,2);");
        btnConfirmer.setOnMouseEntered(e -> btnConfirmer.setStyle(
                "-fx-background-color: #b91c1c; -fx-text-fill: white;" +
                "-fx-font-size: 12; -fx-font-weight: bold;" +
                "-fx-padding: 10 24; -fx-background-radius: 10;" +
                "-fx-border-color: #991b1b; -fx-border-radius: 10; -fx-cursor: hand;"));
        btnConfirmer.setOnMouseExited(e -> btnConfirmer.setStyle(
                "-fx-background-color: #dc2626; -fx-text-fill: white;" +
                "-fx-font-size: 12; -fx-font-weight: bold;" +
                "-fx-padding: 10 24; -fx-background-radius: 10;" +
                "-fx-border-color: #b91c1c; -fx-border-radius: 10; -fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian,rgba(220,38,38,0.4),8,0,0,2);"));

        boutons.getChildren().addAll(btnAnnuler, btnConfirmer);
        contenu.getChildren().addAll(header, corps, boutons);

        // Stage du dialog
        javafx.scene.Scene scene = new javafx.scene.Scene(contenu, 420,
                javafx.scene.layout.Region.USE_COMPUTED_SIZE);
        Stage dialog = new Stage();
        dialog.setTitle("Supprimer");
        dialog.setScene(scene);
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.initOwner(listeDonnees.getScene().getWindow());
        dialog.setResizable(false);

        // Actions boutons
        btnAnnuler.setOnAction(e -> dialog.close());
        btnConfirmer.setOnAction(e -> {
            dialog.close();
            service.deleteEntity(donnee);
            journalService.enregistrer("Admin", "Suppression",
                    "Donnee supprimee : capteur " + donnee.getCapteur(), donnee.getId());
            chargerDonnees();
            chargerJournal();
        });

        dialog.showAndWait();
    }

    @FXML
    public void allerVersDashboard(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Dashboard.fxml"));
            Stage stage = (Stage) listeDonnees.getScene().getWindow();
            stage.setScene(new Scene(root, 1280, 800));
        } catch (IOException e) { System.out.println("Dashboard non trouve : " + e.getMessage()); }
    }

    @FXML
    public void exporterCSV() {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Exporter les donnees");
        fc.setInitialFileName("donnees_forestguard.xlsx");
        fc.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("Excel", "*.xlsx"),
                new javafx.stage.FileChooser.ExtensionFilter("CSV", "*.csv"));
        Stage stage = (Stage) listeDonnees.getScene().getWindow();
        java.io.File fichier = fc.showSaveDialog(stage);
        if (fichier == null) return;
        if (fichier.getName().endsWith(".xlsx")) exporterExcel(fichier);
        else exporterCSVSemicolon(fichier);
    }

    private void exporterExcel(java.io.File fichier) {
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb =
                     new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.xssf.usermodel.XSSFSheet sheet = wb.createSheet("Donnees ForestGuard");
            org.apache.poi.xssf.usermodel.XSSFCellStyle se = wb.createCellStyle();
            se.setFillForegroundColor(new org.apache.poi.xssf.usermodel.XSSFColor(
                    new byte[]{22, (byte)163, 74}, null));
            se.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            se.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
            org.apache.poi.xssf.usermodel.XSSFFont fe = wb.createFont();
            fe.setBold(true); fe.setColor(new org.apache.poi.xssf.usermodel.XSSFColor(
                    new byte[]{(byte)255,(byte)255,(byte)255}, null));
            se.setFont(fe);
            org.apache.poi.xssf.usermodel.XSSFCellStyle sp = wb.createCellStyle();
            sp.setFillForegroundColor(new org.apache.poi.xssf.usermodel.XSSFColor(
                    new byte[]{(byte)255,(byte)255,(byte)255}, null));
            sp.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            sp.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            org.apache.poi.xssf.usermodel.XSSFCellStyle si = wb.createCellStyle();
            si.setFillForegroundColor(new org.apache.poi.xssf.usermodel.XSSFColor(
                    new byte[]{(byte)240,(byte)253,(byte)244}, null));
            si.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            si.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            org.apache.poi.xssf.usermodel.XSSFCellStyle sa = wb.createCellStyle();
            sa.cloneStyleFrom(sp);
            org.apache.poi.xssf.usermodel.XSSFFont fa = wb.createFont();
            fa.setBold(true); fa.setColor(new org.apache.poi.xssf.usermodel.XSSFColor(
                    new byte[]{(byte)220,38,38}, null));
            sa.setFont(fa);
            org.apache.poi.ss.usermodel.Row r0 = sheet.createRow(0);
            r0.setHeightInPoints(30);
            org.apache.poi.ss.usermodel.Cell ct = r0.createCell(0);
            ct.setCellValue("ForestGuard — Rapport des Donnees Capteurs");
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0,0,0,8));
            org.apache.poi.ss.usermodel.Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue("Genere le : " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(1,1,0,8));
            sheet.createRow(2);
            // ── MODIFIÉ : ajout colonne Localisation ──
            String[] cols = {"Capteur","Foret","Localisation","Temperature (C)",
                    "Humidite (%)","Fumee","Date / Heure","Type","Risque"};
            org.apache.poi.ss.usermodel.Row rh = sheet.createRow(3);
            rh.setHeightInPoints(22);
            for (int i = 0; i < cols.length; i++) {
                org.apache.poi.ss.usermodel.Cell c = rh.createCell(i);
                c.setCellValue(cols[i]); c.setCellStyle(se);
            }
            int nl = 4;
            for (DonCapteur d : listeDonnees.getItems()) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(nl);
                row.setHeightInPoints(18);
                org.apache.poi.xssf.usermodel.XSSFCellStyle sb = nl % 2 == 0 ? sp : si;
                row.createCell(0).setCellValue(d.getCapteur());
                row.getCell(0).setCellStyle(sb);
                // ── MODIFIÉ : foret + localisation ──
                String nomF = (d.getForet() != null && !d.getForet().isEmpty())
                        ? d.getForet() : d.getZone();
                row.createCell(1).setCellValue(nomF);
                row.getCell(1).setCellStyle(sb);
                row.createCell(2).setCellValue(
                        d.getLocalisation() != null ? d.getLocalisation() : "");
                row.getCell(2).setCellStyle(sb);
                org.apache.poi.ss.usermodel.Cell c2 = row.createCell(3);
                c2.setCellValue(d.getTemperature());
                c2.setCellStyle(d.getTemperature() > 30 ? sa : sb);
                org.apache.poi.ss.usermodel.Cell c3 = row.createCell(4);
                c3.setCellValue(d.getHumidite());
                c3.setCellStyle(d.getHumidite() < 40 ? sa : sb);
                String ft = d.getFumee() == 0 ? "Aucune" :
                        d.getFumee() < 50 ? "Niveau 1" : "Niveau 2";
                org.apache.poi.ss.usermodel.Cell c4 = row.createCell(5);
                c4.setCellValue(ft); c4.setCellStyle(d.getFumee() > 0 ? sa : sb);
                org.apache.poi.ss.usermodel.Cell c5 = row.createCell(6);
                c5.setCellValue(d.getHorodatage() != null ? d.getHorodatage().format(FMT) : "");
                c5.setCellStyle(sb);
                org.apache.poi.ss.usermodel.Cell c6 = row.createCell(7);
                c6.setCellValue(d.getType() != null ? d.getType() : ""); c6.setCellStyle(sb);
                String risque = d.getTemperature() > 35 && d.getHumidite() < 30 ? "CRITIQUE" :
                        d.getTemperature() > 30 ? "ELEVE" :
                                d.getHumidite() < 40 ? "MOYEN" : "FAIBLE";
                org.apache.poi.ss.usermodel.Cell c7 = row.createCell(8);
                c7.setCellValue(risque);
                c7.setCellStyle("FAIBLE".equals(risque) ? sb : sa);
                nl++;
            }
            for (int i = 0; i < cols.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1024);
            }
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(fichier)) {
                wb.write(fos);
            }
            journalService.enregistrer("Admin", "Export",
                    "Export Excel " + listeDonnees.getItems().size() + " lignes", 0);
            chargerJournal();
            new Alert(Alert.AlertType.INFORMATION,
                    listeDonnees.getItems().size() + " lignes exportees !").show();
        } catch (Exception e) { afficherErreur("Erreur Excel : " + e.getMessage()); e.printStackTrace(); }
    }

    private void exporterCSVSemicolon(java.io.File fichier) {
        try (java.io.PrintWriter w = new java.io.PrintWriter(
                new java.io.OutputStreamWriter(
                        new java.io.FileOutputStream(fichier), "UTF-8"))) {
            w.write('\uFEFF');
            // ── MODIFIÉ : ajout colonne Localisation ──
            w.println("Capteur;Foret;Localisation;Temperature;Humidite;Fumee;Date;Type");
            for (DonCapteur d : listeDonnees.getItems()) {
                String nomF = (d.getForet() != null && !d.getForet().isEmpty())
                        ? d.getForet() : d.getZone();
                w.println(d.getCapteur() + ";" + nomF + ";" +
                        (d.getLocalisation() != null ? d.getLocalisation() : "") + ";" +
                        d.getTemperature() + ";" + d.getHumidite() + ";" +
                        (d.getFumee() == 0 ? "Aucune" : d.getFumee() < 50 ? "Niveau 1" : "Niveau 2") +
                        ";" + (d.getHorodatage() != null ? d.getHorodatage().format(FMT) : "") +
                        ";" + (d.getType() != null ? d.getType() : ""));
            }
            new Alert(Alert.AlertType.INFORMATION, "CSV exporte !").show();
        } catch (Exception e) { afficherErreur("Erreur CSV : " + e.getMessage()); }
    }

    // ════════════════════════════════════════════════════════
    // IA
    // ════════════════════════════════════════════════════════
    private void mettreAJourIA(List<DonCapteur> donnees) {
        PredicteurIncendie.ResultatPrediction res = PredicteurIncendie.predire(donnees);
        if (statIA         != null) statIA.setText(res.niveauRisque);
        if (statIAPct      != null) statIAPct.setText(res.pourcentage + "%");
        if (iaEmoji        != null) iaEmoji.setText(res.emoji);
        if (iaMessage      != null) iaMessage.setText(res.messageIA);
        if (iaPrediction2h != null) iaPrediction2h.setText(res.prediction2h);
        if (iaReco         != null) iaReco.setText(res.recommandation);
        if (barreRisque    != null) barreRisque.setProgress(res.scoreRisque);
        if (cardIA != null) {
            String bg = "rgba(" + hexToRgb(res.couleur) + ",0.15)";
            String bd = "rgba(" + hexToRgb(res.couleur) + ",0.35)";
            cardIA.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 20;" +
                    "-fx-border-color: " + bd + "; -fx-border-radius: 20;" +
                    "-fx-border-width: 1.5; -fx-padding: 18 20; -fx-cursor: hand;");
            if (statIA != null) statIA.setStyle("-fx-font-size: 20; -fx-font-weight: bold;" +
                    "-fx-text-fill: " + res.couleur + ";");
        }
        if (banniereIA != null) {
            banniereIA.setStyle(
                    "-fx-background-color: rgba(" + hexToRgb(res.couleur) + ",0.12);" +
                            "-fx-background-radius: 14;" +
                            "-fx-border-color: rgba(" + hexToRgb(res.couleur) + ",0.25);" +
                            "-fx-border-radius: 14; -fx-border-width: 1; -fx-padding: 12 18;");
            if (barreRisque != null) barreRisque.setStyle(
                    "-fx-accent: " + res.couleur + ";" +
                            "-fx-background-color: rgba(255,255,255,0.15);" +
                            "-fx-background-radius: 4; -fx-background-insets: 0;");
        }
        if ("CRITIQUE".equals(res.niveauRisque) && cardIA != null) {
            Timeline pulse = new Timeline(
                    new KeyFrame(Duration.ZERO,         new KeyValue(cardIA.opacityProperty(), 1.0)),
                    new KeyFrame(Duration.millis(600),  new KeyValue(cardIA.opacityProperty(), 0.6)),
                    new KeyFrame(Duration.millis(1200), new KeyValue(cardIA.opacityProperty(), 1.0)));
            pulse.setCycleCount(5); pulse.play();
        }
    }

    private String hexToRgb(String hex) {
        try {
            return Integer.parseInt(hex.substring(1,3),16) + "," +
                    Integer.parseInt(hex.substring(3,5),16) + "," +
                    Integer.parseInt(hex.substring(5,7),16);
        } catch (Exception e) { return "22,163,74"; }
    }

    private void afficherRisqueParForet(List<DonCapteur> donnees) {
        if (listeRisqueForets == null) return;
        java.util.Map<String, PredicteurIncendie.ResultatPrediction> risques =
                PredicteurIncendie.predireParForet(donnees);
        listeRisqueForets.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String foret, boolean empty) {
                super.updateItem(foret, empty);
                if (empty || foret == null) {
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                    return;
                }
                PredicteurIncendie.ResultatPrediction res = risques.get(foret);
                if (res == null) return;
                Label icone = new Label(res.emoji);
                icone.setStyle("-fx-background-color: " + res.couleur + "; -fx-text-fill: white;" +
                        "-fx-font-size: 14; -fx-font-weight: bold; -fx-background-radius: 10;" +
                        "-fx-padding: 8 12; -fx-min-width: 44; -fx-alignment: CENTER;");
                Label lblForet = new Label(foret);
                lblForet.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
                long nb = donnees.stream()
                        .filter(d -> foret.equals(
                                d.getForet() != null ? d.getForet() : d.getZone()))
                        .map(DonCapteur::getCapteur).distinct().count();
                Label lblNb = new Label(nb + " capteur(s)");
                lblNb.setStyle("-fx-font-size: 11; -fx-text-fill: #64748b;");
                VBox infos = new VBox(3, lblForet, lblNb);
                HBox.setHgrow(infos, javafx.scene.layout.Priority.ALWAYS);
                Label badge = new Label(res.niveauRisque);
                badge.setStyle("-fx-background-color: " + res.couleur + "; -fx-text-fill: white;" +
                        "-fx-font-size: 11; -fx-font-weight: bold;" +
                        "-fx-background-radius: 20; -fx-padding: 4 12;");
                javafx.scene.control.ProgressBar barre =
                        new javafx.scene.control.ProgressBar(res.scoreRisque);
                barre.setPrefWidth(120); barre.setPrefHeight(8);
                barre.setStyle("-fx-accent: " + res.couleur +
                        "; -fx-background-color: #f1f5f9;" +
                        "-fx-background-radius: 4; -fx-background-insets: 0;");
                Label lblPct = new Label(res.pourcentage + "%");
                lblPct.setStyle("-fx-font-size: 12; -fx-font-weight: bold;" +
                        "-fx-text-fill: " + res.couleur + "; -fx-min-width: 38;");
                VBox barreBox = new VBox(4, barre, lblPct);
                barreBox.setAlignment(javafx.geometry.Pos.CENTER);
                HBox row = new HBox(12, icone, infos, badge, barreBox);
                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                row.setStyle("-fx-padding: 10 16;");
                String bgC = switch (res.niveauRisque) {
                    case "CRITIQUE" -> "#fff1f2"; case "ELEVE" -> "#fff7ed";
                    case "MOYEN"    -> "#fefce8"; default       -> "#f0fdf4";
                };
                String bdC = switch (res.niveauRisque) {
                    case "CRITIQUE" -> "#fecdd3"; case "ELEVE" -> "#fed7aa";
                    case "MOYEN"    -> "#fef08a"; default       -> "#bbf7d0";
                };
                setStyle("-fx-background-color: " + bgC + "; -fx-background-radius: 12;" +
                        "-fx-border-color: " + bdC + "; -fx-border-radius: 12;" +
                        "-fx-border-width: 1; -fx-padding: 4 8;");
                setOnMouseEntered(e -> setStyle(
                        "-fx-background-color: white; -fx-background-radius: 12;" +
                                "-fx-border-color: " + res.couleur + "; -fx-border-radius: 12;" +
                                "-fx-border-width: 1.5; -fx-padding: 4 8;" +
                                "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.12),8,0,0,2);"));
                setOnMouseExited(e -> setStyle(
                        "-fx-background-color: " + bgC + "; -fx-background-radius: 12;" +
                                "-fx-border-color: " + bdC + "; -fx-border-radius: 12;" +
                                "-fx-border-width: 1; -fx-padding: 4 8;"));
                setGraphic(row);
            }
        });
        listeRisqueForets.setItems(FXCollections.observableArrayList(risques.keySet()));
    }

    // ════════════════════════════════════════════════════════
    // Météo
    // ════════════════════════════════════════════════════════
    @FXML
    public void actualiserMeteo() {
        if (meteoDesc != null) meteoDesc.setText("Chargement...");
        Thread t = new Thread(() -> {
            MeteoService.DonneesMeteo meteo = MeteoService.getMeteo();
            javafx.application.Platform.runLater(() -> afficherMeteo(meteo));
        });
        t.setDaemon(true); t.start();
    }

    private void afficherMeteo(MeteoService.DonneesMeteo meteo) {
        if (!meteo.succes) {
            if (meteoDesc != null) meteoDesc.setText("Erreur : " + meteo.erreur);
            return;
        }
        if (meteoEmoji    != null) meteoEmoji.setText(meteo.getEmojiMeteo());
        if (meteoVille    != null) meteoVille.setText(meteo.ville);
        if (meteoDesc     != null) meteoDesc.setText(meteo.description);
        if (meteoTemp     != null) meteoTemp.setText(String.format("%.1f°C", meteo.temperature));
        if (meteoHum      != null) meteoHum.setText(String.format("%.0f%%", meteo.humidite));
        if (meteoVent     != null) meteoVent.setText(String.format("%.1f km/h", meteo.vent));
        if (meteoMinMax   != null) meteoMinMax.setText(String.format("%.0f° / %.0f°",
                meteo.tempMin, meteo.tempMax));
        if (meteoRessenti != null) meteoRessenti.setText(String.format("%.1f°C", meteo.tempRessentie));
        if (meteoImpact   != null) {
            meteoImpact.setText(meteo.getImpactForet());
            meteoImpact.setStyle("-fx-font-size: 16; -fx-font-weight: bold;" +
                    "-fx-text-fill: " + meteo.getCouleurImpact() + ";" +
                    "-fx-background-color: " + meteo.getCouleurImpact() + "22;" +
                    "-fx-background-radius: 10; -fx-padding: 8 16;");
        }
        if (meteoImpactDetail != null)
            meteoImpactDetail.setText(String.format(
                    "Temp %.1f°C | Hum %.0f%% | Vent %.0f km/h",
                    meteo.temperature, meteo.humidite, meteo.vent));
        mettreAJourIAAvecMeteo(meteo);
    }

    private void mettreAJourIAAvecMeteo(MeteoService.DonneesMeteo meteo) {
        List<DonCapteur> donnees = new java.util.ArrayList<>(listeDonnees.getItems());
        if (donnees.isEmpty()) return;
        double bonus = 0;
        if (meteo.temperature > 35) bonus += 0.10;
        if (meteo.humidite    < 30) bonus += 0.10;
        if (meteo.vent        > 40) bonus += 0.08;
        PredicteurIncendie.ResultatPrediction res = PredicteurIncendie.predire(donnees);
        double scoreAjuste = Math.min(1.0, res.scoreRisque + bonus);
        if (barreRisque != null) barreRisque.setProgress(scoreAjuste);
        if (statIAPct   != null) statIAPct.setText((int)(scoreAjuste * 100) + "%");
        if (iaMessage   != null && bonus > 0)
            iaMessage.setText(res.messageIA + " | Meteo aggravante : +" + (int)(bonus * 100) + "%");
    }

    // ════════════════════════════════════════════════════════
    // PDF
    // ════════════════════════════════════════════════════════
    @FXML public void exporterGraphePDF() {
        exporterChartEnPDF(chartEvolution, "Evolution_7jours.pdf",
                "Evolution des parametres (7 derniers jours)");
    }
    @FXML public void exporterTempPDF() {
        exporterChartEnPDF(chartTemp24h, "Temperature_24h.pdf", "Evolution Temperature (24h)");
    }
    @FXML public void exporterHumPDF() {
        exporterChartEnPDF(chartHum24h, "Humidite_24h.pdf", "Evolution Humidite (24h)");
    }
    @FXML public void exporterToutPDF() {
        javafx.stage.DirectoryChooser dc = new javafx.stage.DirectoryChooser();
        dc.setTitle("Choisir le dossier");
        Stage stage = (Stage) listeDonnees.getScene().getWindow();
        java.io.File dossier = dc.showDialog(stage);
        if (dossier == null) return;
        exporterChartEnPDFDossier(chartEvolution, dossier, "Evolution_7jours.pdf",
                "Evolution des parametres (7 derniers jours)");
        exporterChartEnPDFDossier(chartTemp24h, dossier, "Temperature_24h.pdf",
                "Evolution Temperature (24h)");
        exporterChartEnPDFDossier(chartHum24h, dossier, "Humidite_24h.pdf",
                "Evolution Humidite (24h)");
        new Alert(Alert.AlertType.INFORMATION,
                "3 PDF exportes dans : " + dossier.getAbsolutePath()).show();
    }

    private void exporterChartEnPDF(javafx.scene.chart.Chart chart,
                                    String nom, String titre) {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Sauvegarder PDF"); fc.setInitialFileName(nom);
        fc.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("PDF","*.pdf"));
        java.io.File f = fc.showSaveDialog((Stage) listeDonnees.getScene().getWindow());
        if (f != null) genererPDF(chart, f, titre);
    }

    private void exporterChartEnPDFDossier(javafx.scene.chart.Chart chart,
                                           java.io.File dossier,
                                           String nom, String titre) {
        genererPDF(chart, new java.io.File(dossier, nom), titre);
    }

    private void genererPDF(javafx.scene.chart.Chart chart,
                            java.io.File fichier, String titre) {
        try {
            javafx.scene.image.WritableImage img =
                    chart.snapshot(new javafx.scene.SnapshotParameters(), null);
            java.awt.image.BufferedImage buf = new java.awt.image.BufferedImage(
                    (int)img.getWidth(), (int)img.getHeight(),
                    java.awt.image.BufferedImage.TYPE_INT_ARGB);
            javafx.embed.swing.SwingFXUtils.fromFXImage(img, buf);
            org.apache.pdfbox.pdmodel.PDDocument doc =
                    new org.apache.pdfbox.pdmodel.PDDocument();
            org.apache.pdfbox.pdmodel.PDPage page =
                    new org.apache.pdfbox.pdmodel.PDPage(
                            org.apache.pdfbox.pdmodel.common.PDRectangle.A4);
            doc.addPage(page);
            org.apache.pdfbox.pdmodel.PDPageContentStream cs =
                    new org.apache.pdfbox.pdmodel.PDPageContentStream(doc, page);
            cs.setNonStrokingColor(java.awt.Color.WHITE);
            cs.addRect(0,0,page.getMediaBox().getWidth(),
                    page.getMediaBox().getHeight()); cs.fill();
            cs.setNonStrokingColor(new java.awt.Color(22,163,74));
            cs.addRect(0,page.getMediaBox().getHeight()-60,
                    page.getMediaBox().getWidth(),60); cs.fill();
            cs.setNonStrokingColor(java.awt.Color.WHITE);
            cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 16);
            cs.beginText();
            cs.newLineAtOffset(30, page.getMediaBox().getHeight()-38);
            cs.showText("ForestGuard — " + titre); cs.endText();
            cs.setNonStrokingColor(new java.awt.Color(100,116,139));
            cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 10);
            cs.beginText();
            cs.newLineAtOffset(30, page.getMediaBox().getHeight()-80);
            cs.showText("Genere le : " + LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))); cs.endText();
            org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject pdImg =
                    org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory
                            .createFromImage(doc, buf);
            float w = page.getMediaBox().getWidth()-60;
            float h = w * buf.getHeight() / buf.getWidth();
            float y = page.getMediaBox().getHeight()-100-h;
            cs.drawImage(pdImg, 30, y, w, h);
            cs.setNonStrokingColor(new java.awt.Color(22,163,74));
            cs.addRect(0,0,page.getMediaBox().getWidth(),30); cs.fill();
            cs.setNonStrokingColor(java.awt.Color.WHITE);
            cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 9);
            cs.beginText(); cs.newLineAtOffset(30,10);
            cs.showText("ForestGuard — Systeme de Surveillance Intelligente des Forets");
            cs.endText();
            cs.close(); doc.save(fichier); doc.close();
            new Alert(Alert.AlertType.INFORMATION,
                    "PDF sauvegarde : " + fichier.getAbsolutePath()).show();
        } catch (Exception e) {
            afficherErreur("Erreur PDF : " + e.getMessage());
        }
    }

    private void chargerFiltresDepuisBD() {
        // Forêts
        foretFiltre.getItems().clear();
        foretFiltre.getItems().add("Toutes");
        List<String> forets = service.getNomsForetsBD();
        foretFiltre.getItems().addAll(forets);
        foretFiltre.setValue("Toutes");
        System.out.println("Forêts chargées : " + forets);

        // Capteurs
        capteurFiltre.getItems().clear();
        capteurFiltre.getItems().add("Tous");
        List<String> capteurs = service.getNomsCapteurs();
        capteurFiltre.getItems().addAll(capteurs);
        capteurFiltre.setValue("Tous");
        System.out.println("Capteurs chargés : " + capteurs);
    }

    private void rafraichirFiltres() {
        String foretActuelle  = foretFiltre.getValue();
        String capteurActuel  = capteurFiltre.getValue();
        chargerFiltresDepuisBD();
        // Restaurer la sélection si elle existe encore
        if (foretActuelle  != null && foretFiltre.getItems().contains(foretActuelle))
            foretFiltre.setValue(foretActuelle);
        if (capteurActuel  != null && capteurFiltre.getItems().contains(capteurActuel))
            capteurFiltre.setValue(capteurActuel);
    }

    // ════════════════════════════════════════════════════════
    // ESP32 — connexion directe COM5 + fallback auto
    // ════════════════════════════════════════════════════════
    private void demarrerESP32Auto() {
        afficherStatutESP("● Connexion COM5 @ 115200...", "#f59e0b");

        Thread t = new Thread(() -> {

            // ── COM5 @ 115200 en premier (Serial.begin(115200) dans le sketch) ──
            System.out.println("[ESP32] Tentative COM5 @ 115200...");
            boolean ok = esp32.connecterAvecFlush("COM5", 115200);

            if (!ok) {
                // ── COM5 @ 9600 en fallback ──
                System.out.println("[ESP32] Tentative COM5 @ 9600...");
                javafx.application.Platform.runLater(() ->
                        afficherStatutESP("● COM5 @ 9600...", "#f59e0b"));
                ok = esp32.connecterAvecFlush("COM5", 9600);
            }

            if (!ok) {
                // ── Fallback : scanner tous les autres ports ──
                javafx.application.Platform.runLater(() ->
                        afficherStatutESP("● Scan des autres ports...", "#f59e0b"));
                for (String nomPort : ESP32Service.getNomsPortsSysteme()) {
                    if (nomPort.equalsIgnoreCase("COM5")) continue;
                    javafx.application.Platform.runLater(() ->
                            afficherStatutESP("● Test " + nomPort + "...", "#f59e0b"));
                    ok = esp32.connecterAvecFlush(nomPort, 115200);
                    if (!ok) ok = esp32.connecterAvecFlush(nomPort, 9600);
                    if (ok) break;
                }
            }

            if (!ok) {
                javafx.application.Platform.runLater(() -> {
                    afficherStatutESP("● Capteur non disponible", "#dc2626");
                    if (lblDernieredonnee != null)
                        lblDernieredonnee.setText(
                                "Capteur introuvable");
                    afficherAlerteESPNonConnecte(    "Capteur introuvable",
                            "Assurez-vous que le capteur est bien branché\npuis cliquez sur Reconnecter.");
                });
                return;
            }

            // ── Port ouvert — démarrer la lecture ────────────
            final String portOuvert = esp32.getPortActuel();
            javafx.application.Platform.runLater(() ->
                    afficherStatutESP("● " + portOuvert + " ouvert, attente données...", "#f59e0b"));

            esp32.demarrerLecture(
                    d -> mettreAJourCartesExistantes(d),
                    err -> javafx.application.Platform.runLater(() ->
                            afficherStatutESP("● Erreur : " + err, "#dc2626")));

            // ── Attendre les données (max 15s = 3 cycles Arduino de 5s) ──
            int attente = 0;
            while (attente < 15000 && !esp32.aRecuDonnees()) {
                try { Thread.sleep(500); } catch (InterruptedException ignored) { break; }
                attente += 500;
                final int restant = (15000 - attente) / 1000;
                if (restant >= 0)
                    javafx.application.Platform.runLater(() ->
                            afficherStatutESP("● " + portOuvert + " — attente (" + restant + "s)...", "#f59e0b"));
            }

            if (esp32.aRecuDonnees()) {
                javafx.application.Platform.runLater(() -> {
                    afficherStatutESP("● ESP32 connecté ✓ (" + portOuvert + ")", "#16a34a");

                    // ← AJOUTER : remplir le ChoiceBox avec les capteurs BD
                    if (capteurESPCombo != null) {
                        List<String> capteurs = service.getNomsCapteurs();
                        capteurESPCombo.getItems().clear();
                        capteurESPCombo.getItems().addAll(capteurs);
                        if (!capteurs.isEmpty())
                            capteurESPCombo.setValue(capteurs.get(0));
                    }
                });
            } else {
                esp32.deconnecter();
                javafx.application.Platform.runLater(() -> {
                    afficherStatutESP("● Aucune donnée reçue", "#dc2626");
                    if (lblDernieredonnee != null)
                        lblDernieredonnee.setText(
                                "Port ouvert mais pas de JSON — sketch uploadé sur l'ESP32 ?");
                    afficherAlerteESPNonConnecte(    "Capteur ne répond pas",
                            "Le capteur est détecté mais n'envoie pas de données.\nVérifiez qu'il est correctement alimenté.");
                });
            }
        });

        t.setDaemon(true);
        t.setName("ESP32-Connect");
        t.start();
    }

    /** Met à jour le label statut ESP32 */
    private void afficherStatutESP(String texte, String couleur) {
        if (lblStatutESP != null) {
            lblStatutESP.setText(texte);
            lblStatutESP.setStyle("-fx-font-size: 12; -fx-font-weight: bold;" +
                    "-fx-text-fill: " + couleur + ";");
        }
    }

    /** Reconnecter manuellement — appelé par le bouton Reconnecter */
    @FXML
    public void reconnecterESP() {
        esp32.deconnecter();
        demarrerESP32Auto();
    }

    private void mettreAJourCartesExistantes(ESP32Service.DonneesESP32 d) {

        // ── Bloc LIVE ESP32 uniquement (cartes BD inchangées) ──
        if (espTemp != null)
            espTemp.setText(String.format("%.1f°C", d.temperature));

        if (espHum != null)
            espHum.setText(String.format("%.0f%%", d.humidite));

        if (espFumee != null) {
            espFumee.setText(d.flamme ? "🔥 FEU !"
                    : d.fumee == 0 ? "OK"
                    : d.fumee < 30 ? "Faible" : "Élevé");
            espFumee.setStyle("-fx-font-size: 18; -fx-font-weight: bold;" +
                    "-fx-text-fill: " + (d.flamme ? "#dc2626"
                    : d.fumee == 0 ? "#16a34a" : "#f87171") + ";");
        }

        // ── Alerte flamme ──
        if (lblStatutESP != null) {
            if (d.flamme) {
                lblStatutESP.setText("🔥 FLAMME DÉTECTÉE — " + esp32.getPortActuel());
                lblStatutESP.setStyle("-fx-font-size: 12; -fx-font-weight: bold;" +
                        "-fx-text-fill: #dc2626;");
            } else {
                lblStatutESP.setText("● ESP32 connecté ✓ (" + esp32.getPortActuel() + ")");
                lblStatutESP.setStyle("-fx-font-size: 12; -fx-font-weight: bold;" +
                        "-fx-text-fill: #16a34a;");
            }
        }

        // ── Pulse animation sur les boxes LIVE uniquement ──
        VBox[] espCards = {espCardTemp, espCardHum, espCardFumee};
        for (VBox c : espCards) {
            if (c == null) continue;
            javafx.animation.ScaleTransition p =
                    new javafx.animation.ScaleTransition(
                            javafx.util.Duration.millis(150), c);
            p.setFromX(1.0); p.setToX(1.05);
            p.setFromY(1.0); p.setToY(1.05);
            p.setCycleCount(2); p.setAutoReverse(true); p.play();
        }

        // ── Dernière donnée reçue ──
        if (lblDernieredonnee != null)
            lblDernieredonnee.setText(
                    d.horodatage.format(FMT_HEURE) +
                            "  |  " + d.capteur +
                            "  |  T: " + String.format("%.1f°C", d.temperature) +
                            "  H: " + String.format("%.0f%%", d.humidite) +
                            "  Flamme: " + (d.fumee == 0 ? "OK" : (int)d.fumee + "%") +
                            (d.flamme ? "  🔥 ALERTE" : ""));

        // ── Mettre à jour l'IA avec les nouvelles données ──
        List<DonCapteur> donneesTmp = new java.util.ArrayList<>(listeDonnees.getItems());
        DonCapteur nouvelleDonnee = new DonCapteur(
                d.type, d.capteur, d.zone,
                d.temperature, d.humidite, d.fumee, "Automatique");
        nouvelleDonnee.setHorodatage(d.horodatage);
        donneesTmp.add(0, nouvelleDonnee);
        mettreAJourIA(donneesTmp);

// ── Sauvegarde automatique — toujours actif ──
        LocalDateTime maintenant = LocalDateTime.now();
        boolean anomalie = d.temperature > 35
                || d.humidite < 30
                || d.fumee > 50
                || d.flamme;

        boolean intervallePasse = java.time.Duration
                .between(derniereSauvegarde, maintenant)
                .toMinutes() >= INTERVALLE_SAUVEGARDE_MINUTES;

// ✅ Sauvegarde toujours — anomalie OU 10 min passées
// (checkbox ignorée)
        if (anomalie || intervallePasse) {
            derniereSauvegarde = maintenant;
            sauvegarderEnBD(d);
            System.out.println("[SAVE] ✅ Sauvegarde effectuée !");
            if (anomalie) envoyerAlerteAmi(d);

        }
    }
    private void sauvegarderEnBD(ESP32Service.DonneesESP32 d) {
        new Thread(() -> {
            try {
                DonCapteur dc = new DonCapteur(
                        d.type, d.capteur, d.zone,
                        d.temperature, d.humidite, d.fumee, "Automatique");
                dc.setHorodatage(d.horodatage);
                dc.setForet(d.zone); // ← associe la zone comme forêt
                service.addEntity(dc);
                journalService.enregistrer("ESP32", "Ajout",
                        "Auto ESP32 : " + d.capteur + " | " + d.zone, 0);
            } catch (Exception e) {
                System.out.println("Erreur BD : " + e.getMessage());
            }
        }) {{ setDaemon(true); }}.start();
    }

    // ════════════════════════════════════════════════════════
    // Helpers
    // ════════════════════════════════════════════════════════
    private Label badge(String text, String bg, String fg) {
        Label l = new Label(text);
        l.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg + ";" +
                "-fx-font-size: 11; -fx-font-weight: bold;" +
                "-fx-background-radius: 20; -fx-padding: 4 10;");
        return l;
    }

    private Label colored(String text, boolean alerte, double width) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 13; -fx-font-weight: " +
                (alerte ? "bold" : "normal") +
                "; -fx-text-fill: " + (alerte ? "#ef4444" : "#1e293b") + ";");
        l.setPrefWidth(width);
        return l;
    }

    /** Bouton Modifier — vert ForestGuard avec icône crayon */
    private Button btnModifier(DonCapteur d) {
        Button b = new Button("✏ Modifier");
        String s1 = "-fx-background-color: #dcfce7;" +
                "-fx-text-fill: #15803d;" +
                "-fx-font-size: 11; -fx-font-weight: bold;" +
                "-fx-background-radius: 8;" +
                "-fx-border-color: #86efac; -fx-border-radius: 8;" +
                "-fx-padding: 5 12; -fx-cursor: hand;";
        String s2 = "-fx-background-color: #16a34a;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 11; -fx-font-weight: bold;" +
                "-fx-background-radius: 8;" +
                "-fx-border-color: #15803d; -fx-border-radius: 8;" +
                "-fx-padding: 5 12; -fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian,rgba(21,128,61,0.35),6,0,0,2);";
        b.setStyle(s1);
        b.setOnMouseEntered(e -> b.setStyle(s2));
        b.setOnMouseExited(e  -> b.setStyle(s1));
        b.setOnAction(e -> ouvrirModalEditer(d));
        return b;
    }

    /** Bouton Supprimer — rouge avec icône poubelle */
    private Button btnSupprimer(DonCapteur d) {
        Button b = new Button("🗑 Suppr.");
        String s1 = "-fx-background-color: #fee2e2;" +
                "-fx-text-fill: #dc2626;" +
                "-fx-font-size: 11; -fx-font-weight: bold;" +
                "-fx-background-radius: 8;" +
                "-fx-border-color: #fca5a5; -fx-border-radius: 8;" +
                "-fx-padding: 5 12; -fx-cursor: hand;";
        String s2 = "-fx-background-color: #dc2626;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 11; -fx-font-weight: bold;" +
                "-fx-background-radius: 8;" +
                "-fx-border-color: #b91c1c; -fx-border-radius: 8;" +
                "-fx-padding: 5 12; -fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian,rgba(220,38,38,0.35),6,0,0,2);";
        b.setStyle(s1);
        b.setOnMouseEntered(e -> b.setStyle(s2));
        b.setOnMouseExited(e  -> b.setStyle(s1));
        b.setOnAction(e -> supprimerDonnee(d));
        return b;
    }

    private void afficherErreur(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).show();
    }

    private void afficherDonneesGroupees(List<DonCapteur> donnees) {

        // ── 3. CellFactory avec header — définie EN PREMIER ──
        listeDonnees.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(DonCapteur d, boolean empty) {
                super.updateItem(d, empty);
                if (empty || d == null) {
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent; -fx-padding: 0;");
                    return;
                }

                // ── Header forêt ──────────────────────────────
                if ("__HEADER__".equals(d.getCapteur())) {
                    String nomForet = d.getForet() != null ? d.getForet() : "Zone inconnue";
                    String loc      = d.getLocalisation() != null ? d.getLocalisation() : "";
                    int    nbData   = (int) d.getTemperature();

                    Label lblNom = new Label("🌲 " + nomForet);
                    lblNom.setStyle("-fx-font-size: 15; -fx-font-weight: bold;" +
                            "-fx-text-fill: #0f172a;");
                    Label lblLoc = new Label(loc);
                    lblLoc.setStyle("-fx-font-size: 12; -fx-text-fill: #64748b;");
                    Label lblNb  = new Label(nbData + " mesure(s)");
                    lblNb.setStyle("-fx-font-size: 11; -fx-font-weight: bold;" +
                            "-fx-text-fill: #16a34a;" +
                            "-fx-background-color: #dcfce7;" +
                            "-fx-background-radius: 20; -fx-padding: 3 10;");

                    javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
                    HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
                    HBox header = new HBox(12, lblNom, lblLoc, spacer, lblNb);
                    header.setAlignment(Pos.CENTER_LEFT);
                    header.setStyle("-fx-padding: 12 16;");

                    setStyle("-fx-background-color: #f0fdf4;" +
                            "-fx-border-color: #bbf7d0;" +
                            "-fx-border-width: 0 0 2 0;");
                    setGraphic(header);
                    return;
                }

                // ── Ligne normale ─────────────────────────────
                Label badgeCapteur = badge(d.getCapteur() != null ? d.getCapteur() : "?", "#f3e8ff", "#7c3aed");
                badgeCapteur.setPrefWidth(130);

                String nomForet = (d.getForet() != null && !d.getForet().isEmpty())
                        ? d.getForet() : (d.getZone() != null ? d.getZone() : "");
                String loc = (d.getLocalisation() != null && !d.getLocalisation().isEmpty())
                        ? " (" + d.getLocalisation() + ")" : "";
                Label lblForet = new Label(nomForet + loc);
                lblForet.setStyle("-fx-font-size: 12; -fx-text-fill: #1e293b;");
                lblForet.setPrefWidth(175);

                boolean tA = d.getTemperature() > 30;
                Label lblTemp = colored(d.getTemperature() + "C", tA, 130);
                boolean hA = d.getHumidite() < 40;
                Label lblHum = colored(d.getHumidite() + "%", hA, 110);

                String fTxt = d.getFumee() == 0 ? "Aucune" :
                        d.getFumee() < 50 ? "Niveau 1" : "Niveau 2";
                String fBg  = d.getFumee() == 0 ? "#dcfce7" :
                        d.getFumee() < 50 ? "#fee2e2" : "#fef9c3";
                String fFg  = d.getFumee() == 0 ? "#16a34a" :
                        d.getFumee() < 50 ? "#dc2626" : "#ca8a04";
                Label badgeFumee = badge(fTxt, fBg, fFg);
                badgeFumee.setPrefWidth(130);

                Label lblDate = new Label(
                        d.getHorodatage() != null ? d.getHorodatage().format(FMT) : "");
                lblDate.setStyle("-fx-font-size: 12; -fx-text-fill: #64748b;");
                lblDate.setPrefWidth(155);

                String type  = d.getType() != null ? d.getType() : "Manuel";
                String tBg   = "Automatique".equals(type) ? "#ede9fe" : "#fce7f3";
                String tFg   = "Automatique".equals(type) ? "#7c3aed" : "#db2777";
                String tTxt  = type.substring(0, Math.min(4, type.length()));
                Label badgeType = badge(tTxt, tBg, tFg);
                badgeType.setPrefWidth(100);

                Button btnE = btnModifier(d);
                Button btnX = btnSupprimer(d);

                HBox actions = new HBox(6, btnE, btnX);
                actions.setAlignment(Pos.CENTER_RIGHT);

                HBox row = new HBox(0, badgeCapteur, lblForet, lblTemp, lblHum,
                        badgeFumee, lblDate, badgeType, actions);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-padding: 8 16 8 32;");

                String bgN = getIndex() % 2 == 0
                        ? "rgba(255,255,255,0.95)" : "rgba(248,250,252,0.95)";
                setStyle("-fx-background-color: " + bgN +
                        "; -fx-border-color: #f1f5f9; -fx-border-width: 0 0 1 0;");
                setOnMouseEntered(e -> setStyle(
                        "-fx-background-color: #f0fdf4;" +
                                "-fx-border-color: #bbf7d0; -fx-border-width: 0 0 1 0;"));
                setOnMouseExited(e -> setStyle(
                        "-fx-background-color: " + bgN +
                                "; -fx-border-color: #f1f5f9; -fx-border-width: 0 0 1 0;"));
                setGraphic(row);
            }
        });

        // ── 1. Grouper par forêt ─────────────────────────────
        java.util.Map<String, List<DonCapteur>> parForet = new java.util.LinkedHashMap<>();
        for (DonCapteur d : donnees) {
            String nomForet = (d.getForet() != null && !d.getForet().isEmpty())
                    ? d.getForet() : (d.getZone() != null ? d.getZone() : "Zone inconnue");
            parForet.computeIfAbsent(nomForet, k -> new java.util.ArrayList<>()).add(d);
        }

        // ── 2. Construire la liste avec séparateurs ───────────
        ObservableList<DonCapteur> listeAvecGroupes = FXCollections.observableArrayList();
        for (Map.Entry<String, List<DonCapteur>> entry : parForet.entrySet()) {
            DonCapteur header = new DonCapteur();
            header.setForet(entry.getKey());
            header.setCapteur("__HEADER__");
            String loc = entry.getValue().stream()
                    .map(DonCapteur::getLocalisation)
                    .filter(l -> l != null && !l.isEmpty())
                    .findFirst().orElse("");
            header.setLocalisation(loc);
            header.setTemperature(entry.getValue().size());
            listeAvecGroupes.add(header);
            listeAvecGroupes.addAll(entry.getValue());
        }

        // ── Mettre à jour la liste ────────────────────────────
        listeDonnees.setItems(listeAvecGroupes);
        System.out.println("afficherDonneesGroupees : " + listeAvecGroupes.size() +
                " items dans la ListView (" + donnees.size() + " donnees + " +
                parForet.size() + " headers)");
    }
    private void envoyerAlerteAmi(ESP32Service.DonneesESP32 d) {
        new Thread(() -> {
            try {
                String jsonBody = String.format(
                        "{\"capteur\":\"%s\",\"zone\":\"%s\"," +
                                "\"temperature\":%.1f,\"humidite\":%.1f," +
                                "\"fumee\":%.0f,\"flamme\":%b," +
                                "\"niveau\":\"CRITIQUE\",\"source\":\"ForestGuard\"}",
                        d.capteur, d.zone,
                        d.temperature, d.humidite,
                        d.fumee, d.flamme);

                // ← remplace par l'IP et port de ton ami
                java.net.URL url = new java.net.URL(
                        "http://IP_AMI:PORT/api/alertes");
                java.net.HttpURLConnection conn =
                        (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                conn.setDoOutput(true);
                conn.getOutputStream().write(
                        jsonBody.getBytes(java.nio.charset.StandardCharsets.UTF_8));

                int code = conn.getResponseCode();
                System.out.println("[ALERTE] Envoyée → HTTP " + code);
                conn.disconnect();

                // Log dans le journal
                javafx.application.Platform.runLater(() ->
                        journalService.enregistrer("ESP32", "Alerte",
                                "CRITIQUE : " + d.zone +
                                        " T:" + d.temperature +
                                        " Flamme:" + d.flamme, 0));

            } catch (Exception e) {
                System.out.println("[ALERTE] Erreur envoi : " + e.getMessage());
            }
        }) {{ setDaemon(true); }}.start();
    }

    private void initialiserRechercheForets() {
        if (champRechercheForet == null) return;
        afficherResultatsForets("");
        champRechercheForet.textProperty().addListener((obs, old, val) -> {
            afficherResultatsForets(val);
            if (boxForetSelectionnee != null) {
                boxForetSelectionnee.setVisible(false);
                boxForetSelectionnee.setManaged(false);
            }
        });
        if (listeResultatsForets != null) {
            listeResultatsForets.setOnMouseClicked(e -> {
                String f = listeResultatsForets
                        .getSelectionModel().getSelectedItem();
                if (f != null) afficherStatsForet(f);
            });
            listeResultatsForets.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(String foret, boolean empty) {
                    super.updateItem(foret, empty);
                    if (empty || foret == null) {
                        setGraphic(null);
                        setStyle("-fx-background-color: transparent;");
                        return;
                    }
                    Label lbl = new Label("🌲  " + foret);
                    lbl.setStyle("-fx-font-size: 12; -fx-text-fill: white;");
                    setGraphic(lbl);
                    setStyle("-fx-background-color: transparent; -fx-padding: 5 12;");
                    setOnMouseEntered(e -> setStyle(
                            "-fx-background-color: rgba(22,163,74,0.2);" +
                                    "-fx-padding: 5 12; -fx-cursor: hand;"));
                    setOnMouseExited(e -> setStyle(
                            "-fx-background-color: transparent; -fx-padding: 5 12;"));
                }
            });
        }
    }

    private void afficherResultatsForets(String recherche) {
        if (listeResultatsForets == null) return;
        String lower = recherche == null ? "" : recherche.trim().toLowerCase();
        List<String> filtrees = service.getNomsForetsBD().stream()
                .distinct()   //  protection supplémentaire
                .filter(f -> lower.isEmpty() || f.toLowerCase().contains(lower))
                .sorted()
                .collect(Collectors.toList());
        listeResultatsForets.setItems(
                FXCollections.observableArrayList(filtrees));
    }

    private void afficherStatsForet(String nomForet) {
        if (boxForetSelectionnee == null) return;
        List<DonCapteur> data = service.getDataAvecForet().stream()
                .filter(d -> nomForet.equals(
                        d.getForet() != null ? d.getForet() : d.getZone()))
                .collect(Collectors.toList());
        if (data.isEmpty()) return;
        double avgT = data.stream()
                .mapToDouble(DonCapteur::getTemperature).average().orElse(0);
        double avgH = data.stream()
                .mapToDouble(DonCapteur::getHumidite).average().orElse(0);
        if (lblForetSelectNom  != null)
            lblForetSelectNom.setText("🌲 " + nomForet);
        if (lblForetSelectTemp != null)
            lblForetSelectTemp.setText(String.format("%.1f°C", avgT));
        if (lblForetSelectHum  != null)
            lblForetSelectHum.setText(String.format("%.0f%%", avgH));
        if (lblForetSelectNb   != null)
            lblForetSelectNb.setText(data.size() + " mes.");
        boxForetSelectionnee.setVisible(true);
        boxForetSelectionnee.setManaged(true);
    }

    private void afficherAlerteESPNonConnecte(String cause, String conseil) {
        VBox contenu = new VBox(0);
        contenu.setStyle("-fx-background-color: #0d1f14;");

        // Header
        HBox header = new HBox(12);
        header.setStyle("-fx-background-color: #f59e0b; -fx-padding: 16 24;" +
                "-fx-background-radius: 12 12 0 0;");
        header.setAlignment(Pos.CENTER_LEFT);
        Label icone = new Label("⚠");
        icone.setStyle("-fx-font-size: 22; -fx-text-fill: white;");
        VBox titreBox = new VBox(2);
        Label titre = new Label("Capteur non connecté");
        titre.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: white;");
        Label sous = new Label("Forêt Aïn Drahem — " + cause);
        sous.setStyle("-fx-font-size: 11; -fx-text-fill: rgba(255,255,255,0.85);");
        titreBox.getChildren().addAll(titre, sous);
        header.getChildren().addAll(icone, titreBox);

        // Corps
        VBox corps = new VBox(14);
        corps.setStyle("-fx-padding: 20 24;");
        Label msg = new Label(
                "Le capteur de la Forêt Aïn Drahem\nn'a pas pu être contacté au démarrage.");
        msg.setStyle("-fx-font-size: 13; -fx-text-fill: rgba(255,255,255,0.9);");
        msg.setWrapText(true);

        VBox carte = new VBox(8);
        carte.setStyle("-fx-background-color: rgba(245,158,11,0.15);" +
                "-fx-background-radius: 10;" +
                "-fx-border-color: rgba(245,158,11,0.4);" +
                "-fx-border-radius: 10; -fx-border-width: 1; -fx-padding: 12 16;");
        Label lblCause = new Label("Cause : " + cause);
        lblCause.setStyle("-fx-font-size: 12; -fx-text-fill: #fcd34d; -fx-font-weight: bold;");
        Label lblConseil = new Label("→ " + conseil);
        lblConseil.setStyle("-fx-font-size: 12; -fx-text-fill: #4ade80; -fx-font-weight: bold;");
        lblConseil.setWrapText(true);
        carte.getChildren().addAll(lblCause, lblConseil);
        corps.getChildren().addAll(msg, carte);

        // Boutons
        HBox boutons = new HBox(10);
        boutons.setAlignment(Pos.CENTER_RIGHT);
        boutons.setStyle("-fx-padding: 0 24 20 24;");
        Button btnIgnorer = new Button("Ignorer");
        btnIgnorer.setStyle(
                "-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: #94a3b8;" +
                        "-fx-font-size: 12; -fx-font-weight: bold; -fx-padding: 10 22;" +
                        "-fx-background-radius: 10; -fx-border-color: rgba(255,255,255,0.2);" +
                        "-fx-border-radius: 10; -fx-cursor: hand;");
        Button btnReconnecter = new Button("↺  Reconnecter");
        btnReconnecter.setStyle(
                "-fx-background-color: #16a34a; -fx-text-fill: white;" +
                        "-fx-font-size: 12; -fx-font-weight: bold; -fx-padding: 10 22;" +
                        "-fx-background-radius: 10; -fx-border-color: #15803d;" +
                        "-fx-border-radius: 10; -fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian,rgba(22,163,74,0.5),10,0,0,3);");
        boutons.getChildren().addAll(btnIgnorer, btnReconnecter);
        contenu.getChildren().addAll(header, corps, boutons);

        Stage dialog = new Stage();
        dialog.setTitle("Avertissement — Capteur IoT");
        dialog.setScene(new javafx.scene.Scene(contenu, 400,
                javafx.scene.layout.Region.USE_COMPUTED_SIZE));
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setResizable(false);
        try {
            Stage principal = (Stage) listeDonnees.getScene().getWindow();
            dialog.initOwner(principal);
            dialog.setX(principal.getX() + principal.getWidth() / 2 - 200);
            dialog.setY(principal.getY() + principal.getHeight() / 2 - 150);
        } catch (Exception ignored) {}

        btnIgnorer.setOnAction(e -> dialog.close());
        btnReconnecter.setOnAction(e -> { dialog.close(); reconnecterESP(); });
        dialog.show();
    }
}