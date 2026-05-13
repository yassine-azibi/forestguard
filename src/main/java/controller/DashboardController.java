package controller;

import model.Alerte;
import service.AlerteService;
import service.RapportService;
import service.WeatherApiService;
import utils.NavigationManager;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DashboardController implements Initializable {

    @FXML private Label lblTotal, lblNouvelles, lblValidees, lblRejetees;
    @FXML private javafx.scene.control.ScrollPane scrollAlertes;
    @FXML private javafx.scene.layout.VBox        conteneurCartes;
    @FXML private Label                            lblCompteurVisible;
    @FXML private ComboBox<String> cbFiltre;

    @FXML private TableView<Alerte>           tableAlertes;
    @FXML private TableColumn<Alerte, String> colType, colNiveau, colLocalisation;
    @FXML private TableColumn<Alerte, String> colDate, colStatut, colSource;
    @FXML private TableColumn<Alerte, Void>   colActions;

    // ── Labels météo dans le Dashboard ──────────────────────────────────────
    @FXML private Label lblMeteoTunis;
    @FXML private Label lblMeteoGouberlatine;
    @FXML private Label lblMeteoHeure;

    private final AlerteService    service        = new AlerteService();
    private final RapportService   rapportService = new RapportService();
    private final WeatherApiService weatherService = new WeatherApiService();
    private final ObservableList<Alerte> data      = FXCollections.observableArrayList();

    private ScheduledExecutorService scheduler;

    private static final String CELL_STYLE =
        "-fx-text-fill: #e2e8f0; -fx-font-size: 12; -fx-background-color: transparent; -fx-padding: 8 6;";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cbFiltre.setItems(FXCollections.observableArrayList(
                "Toutes", "Nouvelle", "Validée", "Rejetée"));
        cbFiltre.setValue("Toutes");
        cbFiltre.setOnAction(e -> chargerDonnees());
        configurerColonnes();
        appliquerStyleTable();
        chargerDonnees();

        // Charger la météo au démarrage puis toutes les 5 minutes
        chargerMeteo();
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "dashboard-meteo");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::chargerMeteo, 5, 5, TimeUnit.MINUTES);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  MÉTÉO EN TEMPS RÉEL
    // ══════════════════════════════════════════════════════════════════════════

    private void chargerMeteo() {
        new Thread(() -> {
            double[] tunis = weatherService.getMeteo("Tunis");
            double[] sfax  = weatherService.getMeteo("Sfax");

            String heure = java.time.LocalTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));

            Platform.runLater(() -> {
                if (lblMeteoTunis != null)
                    lblMeteoTunis.setText(tunis != null
                        ? String.format("☁ Tunis  🌡 %.0f°C  💧 %.0f%%", tunis[0], tunis[1])
                        : "☁ Tunis  --°C  --%");

                if (lblMeteoGouberlatine != null)
                    lblMeteoGouberlatine.setText(sfax != null
                        ? String.format("☁ Sfax  🌡 %.0f°C  💧 %.0f%%", sfax[0], sfax[1])
                        : "☁ Sfax  --°C  --%");

                if (lblMeteoHeure != null)
                    lblMeteoHeure.setText("⏱ MàJ " + heure);
            });
        }, "dashboard-meteo-load").start();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  DONNÉES ALERTES
    // ══════════════════════════════════════════════════════════════════════════

    private void chargerDonnees() {
        String f = cbFiltre.getValue();
        List<Alerte> liste = "Toutes".equals(f)
                ? service.getToutesAlertes()
                : service.getAlertesByStatut(f);
        data.setAll(liste);
        tableAlertes.refresh();
        lblTotal.setText(String.valueOf(service.countAll()));
        lblNouvelles.setText(String.valueOf(service.countByStatut("Nouvelle")));
        lblValidees.setText(String.valueOf(service.countByStatut("Validée")));
        lblRejetees.setText(String.valueOf(service.countByStatut("Rejetée")));

        // Construire les cartes visuelles
        if (conteneurCartes != null) {
            construireCartes(liste);
        }
        if (lblCompteurVisible != null) {
            lblCompteurVisible.setText(liste.size() + " alerte(s) affichee(s)");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CARTES VISUELLES — remplace le TableView
    // ══════════════════════════════════════════════════════════════════════════

    private void construireCartes(List<Alerte> liste) {
        conteneurCartes.getChildren().clear();

        if (liste.isEmpty()) {
            VBox empty = new VBox(12);
            empty.setAlignment(javafx.geometry.Pos.CENTER);
            empty.setStyle("-fx-padding: 60 0;");
            Label ico = new Label("🌲");
            ico.setStyle("-fx-font-size: 48;");
            Label msg = new Label("Aucune alerte trouvee");
            msg.setStyle("-fx-text-fill: #4ade80; -fx-font-size: 16; -fx-font-weight: bold;");
            Label sub = new Label("La foret est calme pour le moment");
            sub.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12;");
            empty.getChildren().addAll(ico, msg, sub);
            conteneurCartes.getChildren().add(empty);
            return;
        }

        for (Alerte a : liste) {
            conteneurCartes.getChildren().add(creerCarte(a));
        }
    }

    private javafx.scene.layout.HBox creerCarte(Alerte a) {
        // Couleurs selon niveau
        String[] couleurs = switch (a.getNiveau()) {
            case "Critique" -> new String[]{"#3f0000", "#ef4444", "#7f1d1d", "#fca5a5"};
            case "Haute"    -> new String[]{"#3f1800", "#f97316", "#7c2d12", "#fdba74"};
            default         -> new String[]{"#001535", "#3b82f6", "#1e3a5f", "#93c5fd"};
        };
        String bgCard    = couleurs[0];
        String accent    = couleurs[1];
        String badgeBg   = couleurs[2];
        String badgeFg   = couleurs[3];

        String icone = switch (a.getTypeAlerte()) {
            case "Incendie"           -> "🔥";
            case "Fumee", "Fumée"     -> "💨";
            case "Chaleur excessive"  -> "🌡";
            default                   -> "🌿";
        };

        String statutColor = switch (a.getStatut()) {
            case "Validée", "Validee" -> "#4ade80";
            case "Rejetée", "Rejetee" -> "#f87171";
            default                   -> "#93c5fd";
        };
        String statutBg = switch (a.getStatut()) {
            case "Validée", "Validee" -> "#052e16";
            case "Rejetée", "Rejetee" -> "#450a0a";
            default                   -> "#172554";
        };
        String statutBorder = switch (a.getStatut()) {
            case "Validée", "Validee" -> "#16a34a";
            case "Rejetée", "Rejetee" -> "#dc2626";
            default                   -> "#2563eb";
        };

        // ── Icône cercle ──────────────────────────────────────────────────
        Label lblIco = new Label(icone);
        lblIco.setStyle(
            "-fx-background-color:" + badgeBg + ";" +
            "-fx-background-radius: 50;" +
            "-fx-min-width: 48; -fx-min-height: 48;" +
            "-fx-max-width: 48; -fx-max-height: 48;" +
            "-fx-alignment: center; -fx-font-size: 22;" +
            "-fx-border-color:" + accent + ";" +
            "-fx-border-radius: 50; -fx-border-width: 1.5;"
        );

        // ── Infos principales ─────────────────────────────────────────────
        Label lblType = new Label(icone + "  " + a.getTypeAlerte());
        lblType.setStyle("-fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold;");

        Label lblLoc = new Label("📍  " + a.getLocalisation().replaceAll("\\[.*\\]", "").trim());
        lblLoc.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11;");
        lblLoc.setMaxWidth(280);
        lblLoc.setWrapText(false);

        Label lblDate = new Label("🕐  " + a.getDateFormatted()
            + "   •   Source : " + a.getSource());
        lblDate.setStyle("-fx-text-fill: #64748b; -fx-font-size: 10;");

        VBox infoBox = new VBox(4, lblType, lblLoc, lblDate);
        infoBox.setPrefWidth(320);

        // ── Badges ───────────────────────────────────────────────────────
        Label badgeNiv = new Label(a.getNiveau().toUpperCase());
        badgeNiv.setStyle(
            "-fx-background-color:" + badgeBg + ";" +
            "-fx-text-fill:" + badgeFg + ";" +
            "-fx-background-radius: 20; -fx-padding: 4 14;" +
            "-fx-font-size: 10; -fx-font-weight: bold;" +
            "-fx-border-color:" + accent + ";" +
            "-fx-border-radius: 20; -fx-border-width: 1;"
        );

        Label badgeStat = new Label(a.getStatut());
        badgeStat.setStyle(
            "-fx-background-color:" + statutBg + ";" +
            "-fx-text-fill:" + statutColor + ";" +
            "-fx-background-radius: 20; -fx-padding: 4 14;" +
            "-fx-font-size: 10; -fx-font-weight: bold;" +
            "-fx-border-color:" + statutBorder + ";" +
            "-fx-border-radius: 20; -fx-border-width: 1;"
        );

        VBox badgesBox = new VBox(6, badgeNiv, badgeStat);
        badgesBox.setAlignment(javafx.geometry.Pos.CENTER);
        badgesBox.setPrefWidth(100);

        // ── Boutons actions ───────────────────────────────────────────────
        Button bVal = btnCarte("✔  Valider",  "#052e16", "#4ade80", "#16a34a");
        Button bRej = btnCarte("✕  Rejeter",  "#450a0a", "#f87171", "#dc2626");
        Button bMod = btnCarte("✏  Modifier", "#1a1a00", "#fde047", "#ca8a04");
        Button bDel = btnCarte("🗑",          "#1a0a0a", "#94a3b8", "#334155");

        bVal.setDisable("Validée".equals(a.getStatut()) || "Validee".equals(a.getStatut()));
        bRej.setDisable("Rejetée".equals(a.getStatut()) || "Rejetee".equals(a.getStatut()));

        bVal.setOnAction(e -> { service.validerAlerte(a.getId()); chargerDonnees(); });
        bRej.setOnAction(e -> { service.rejeterAlerte(a.getId()); chargerDonnees(); });
        bMod.setOnAction(e -> ouvrirModification(a));
        bDel.setOnAction(e -> supprimerAlerte(a));

        HBox btnsBox = new HBox(6, bVal, bRej, bMod, bDel);
        btnsBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        btnsBox.setPrefWidth(240);

        // ── ID badge ────────────────────────────────────────────────────
        Label lblId = new Label("#" + a.getId());
        lblId.setStyle("-fx-text-fill: #334155; -fx-font-size: 10; -fx-font-weight: bold;");
        lblId.setPrefWidth(32);

        // ── Separateur vertical accent ──────────────────────────────────
        Region accentBar = new Region();
        accentBar.setPrefWidth(4);
        accentBar.setMinWidth(4);
        accentBar.setMaxWidth(4);
        accentBar.setStyle(
            "-fx-background-color:" + accent + ";" +
            "-fx-background-radius: 4 0 0 4;"
        );

        // ── Carte complète ───────────────────────────────────────────────
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        HBox card = new HBox(0);
        card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        card.setStyle(
            "-fx-background-color:" + bgCard + ";" +
            "-fx-background-radius: 12;" +
            "-fx-border-color:" + accent + "22;" +
            "-fx-border-radius: 12; -fx-border-width: 1;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 8, 0, 0, 2);" +
            "-fx-cursor: hand;"
        );
        card.setPrefHeight(72);

        HBox inner = new HBox(14);
        inner.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        inner.setPadding(new Insets(12, 16, 12, 0));
        inner.setStyle("-fx-background-color: transparent;");
        HBox.setHgrow(inner, javafx.scene.layout.Priority.ALWAYS);

        inner.getChildren().addAll(lblId, lblIco, infoBox, spacer, badgesBox, btnsBox);
        card.getChildren().addAll(accentBar, inner);

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle(
            "-fx-background-color: derive(" + bgCard + ", 15%);" +
            "-fx-background-radius: 12;" +
            "-fx-border-color:" + accent + "66;" +
            "-fx-border-radius: 12; -fx-border-width: 1;" +
            "-fx-effect: dropshadow(gaussian, " + accent + "44, 14, 0, 0, 4);" +
            "-fx-cursor: hand;"
        ));
        card.setOnMouseExited(e -> card.setStyle(
            "-fx-background-color:" + bgCard + ";" +
            "-fx-background-radius: 12;" +
            "-fx-border-color:" + accent + "22;" +
            "-fx-border-radius: 12; -fx-border-width: 1;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 8, 0, 0, 2);" +
            "-fx-cursor: hand;"
        ));

        return card;
    }

    private Button btnCarte(String txt, String bg, String fg, String border) {
        Button b = new Button(txt);
        b.setStyle(
            "-fx-background-color:" + bg + ";" +
            "-fx-text-fill:" + fg + ";" +
            "-fx-background-radius: 20; -fx-padding: 5 12;" +
            "-fx-font-size: 10; -fx-cursor: hand; -fx-font-weight: bold;" +
            "-fx-border-color:" + border + ";" +
            "-fx-border-radius: 20; -fx-border-width: 1;"
        );
        b.setOnMouseEntered(e -> b.setStyle(
            "-fx-background-color:" + border + ";" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 20; -fx-padding: 5 12;" +
            "-fx-font-size: 10; -fx-cursor: hand; -fx-font-weight: bold;" +
            "-fx-border-color:" + border + ";" +
            "-fx-border-radius: 20; -fx-border-width: 1;"
        ));
        b.setOnMouseExited(e -> b.setStyle(
            "-fx-background-color:" + bg + ";" +
            "-fx-text-fill:" + fg + ";" +
            "-fx-background-radius: 20; -fx-padding: 5 12;" +
            "-fx-font-size: 10; -fx-cursor: hand; -fx-font-weight: bold;" +
            "-fx-border-color:" + border + ";" +
            "-fx-border-radius: 20; -fx-border-width: 1;"
        ));
        return b;
    }

    @FXML private void actualiser() { chargerDonnees(); }

    // ── UPDATE via Dialog ─────────────────────────────────────────────────────
    private void ouvrirModification(Alerte a) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Modifier l'alerte");
        dialog.setHeaderText("Modifier — " + a.getTypeAlerte()
                + " · " + a.getLocalisation());

        ComboBox<String> cbType = new ComboBox<>(FXCollections.observableArrayList(
                "Incendie", "Fumée", "Chaleur excessive", "Sécheresse"));
        cbType.setValue(a.getTypeAlerte()); cbType.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> cbNiv = new ComboBox<>(FXCollections.observableArrayList(
                "Critique", "Haute", "Moyenne"));
        cbNiv.setValue(a.getNiveau()); cbNiv.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> cbStat = new ComboBox<>(FXCollections.observableArrayList(
                "Nouvelle", "Validée", "Rejetée"));
        cbStat.setValue(a.getStatut()); cbStat.setMaxWidth(Double.MAX_VALUE);

        TextField tfLoc = new TextField(a.getLocalisation());
        tfLoc.setMaxWidth(Double.MAX_VALUE);

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(14); grid.setVgap(12);
        grid.setPadding(new Insets(20)); grid.setPrefWidth(440);

        Label lType = new Label("Type :"); lType.setStyle("-fx-font-weight:bold;-fx-font-size:13;");
        Label lNiv  = new Label("Niveau :"); lNiv.setStyle("-fx-font-weight:bold;-fx-font-size:13;");
        Label lLoc  = new Label("Localisation :"); lLoc.setStyle("-fx-font-weight:bold;-fx-font-size:13;");
        Label lStat = new Label("Statut :"); lStat.setStyle("-fx-font-weight:bold;-fx-font-size:13;");

        grid.add(lType, 0, 0); grid.add(cbType, 1, 0);
        grid.add(lNiv,  0, 1); grid.add(cbNiv,  1, 1);
        grid.add(lLoc,  0, 2); grid.add(tfLoc,  1, 2);
        grid.add(lStat, 0, 3); grid.add(cbStat, 1, 3);

        javafx.scene.layout.ColumnConstraints c1 = new javafx.scene.layout.ColumnConstraints(120);
        javafx.scene.layout.ColumnConstraints c2 = new javafx.scene.layout.ColumnConstraints();
        c2.setHgrow(javafx.scene.layout.Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c1, c2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button btnOk = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        btnOk.setText("💾  Sauvegarder");
        btnOk.setStyle("-fx-background-color:#16a34a;-fx-text-fill:white;" +
                       "-fx-font-weight:bold;-fx-font-size:13;-fx-padding:8 18;");

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String loc = tfLoc.getText().trim();
            if (loc.isEmpty()) return;
            a.setTypeAlerte(cbType.getValue());
            a.setNiveau(cbNiv.getValue());
            a.setLocalisation(loc);
            a.setStatut(cbStat.getValue());
            if (service.modifierAlerte(a)) chargerDonnees();
        }
    }

    // ── DELETE ────────────────────────────────────────────────────────────────
    private void supprimerAlerte(Alerte a) {
        Alert dlg = new Alert(Alert.AlertType.CONFIRMATION);
        dlg.setTitle("Confirmation");
        dlg.setHeaderText("Supprimer cette alerte ?");
        dlg.setContentText("Type : " + a.getTypeAlerte()
                + "\nZone : " + a.getLocalisation());
        Optional<ButtonType> r = dlg.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.OK) {
            service.supprimerAlerte(a.getId());
            chargerDonnees();
        }
    }

    // ── STYLE TABLE ───────────────────────────────────────────────────────────
    private void appliquerStyleTable() {
        // Style global du tableau
        tableAlertes.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-border-color: transparent;" +
            "-fx-table-cell-border-color: transparent;"
        );

        tableAlertes.setRowFactory(tv -> {
            TableRow<Alerte> row = new TableRow<>() {
                @Override
                protected void updateItem(Alerte item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setStyle("-fx-background-color: transparent;");
                        return;
                    }
                    // Alterner couleur de fond selon l'index
                    String base = (getIndex() % 2 == 0)
                        ? "-fx-background-color: rgba(13,30,20,0.95);"
                        : "-fx-background-color: rgba(17,38,25,0.95);";
                    setStyle(base +
                        "-fx-border-color: transparent transparent rgba(30,74,42,0.6) transparent;" +
                        "-fx-border-width: 0 0 1 0;");
                }
            };

            // Hover effect
            row.hoverProperty().addListener((obs, was, is) -> {
                if (!row.isEmpty()) {
                    if (is) {
                        row.setStyle(
                            "-fx-background-color: rgba(22,163,74,0.12);" +
                            "-fx-border-color: transparent transparent rgba(74,222,128,0.3) transparent;" +
                            "-fx-border-width: 0 0 1 0;" +
                            "-fx-effect: dropshadow(gaussian, rgba(22,163,74,0.15), 6, 0, 0, 1);"
                        );
                    } else {
                        String base = (row.getIndex() % 2 == 0)
                            ? "-fx-background-color: rgba(13,30,20,0.95);"
                            : "-fx-background-color: rgba(17,38,25,0.95);";
                        row.setStyle(base +
                            "-fx-border-color: transparent transparent rgba(30,74,42,0.6) transparent;" +
                            "-fx-border-width: 0 0 1 0;");
                    }
                }
            });

            // Selected effect
            row.selectedProperty().addListener((obs, was, is) -> {
                if (!row.isEmpty() && is) {
                    row.setStyle(
                        "-fx-background-color: rgba(22,163,74,0.22);" +
                        "-fx-border-color: #4ade80 transparent #4ade80 transparent;" +
                        "-fx-border-width: 1 0 1 0;"
                    );
                }
            });

            return row;
        });

        // Style des colonnes header
        tableAlertes.getStylesheets().clear();
    }

    // ── COLONNES ──────────────────────────────────────────────────────────────
    private void configurerColonnes() {
        colType.setCellValueFactory(new PropertyValueFactory<>("typeAlerte"));
        colType.setCellFactory(c -> cellBlanc());
        colLocalisation.setCellValueFactory(new PropertyValueFactory<>("localisation"));
        colLocalisation.setCellFactory(c -> cellBlanc());
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateFormatted"));
        colDate.setCellFactory(c -> cellBlanc());
        colSource.setCellValueFactory(new PropertyValueFactory<>("source"));
        colSource.setCellFactory(c -> cellBlanc());

        colNiveau.setCellValueFactory(new PropertyValueFactory<>("niveau"));
        colNiveau.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                setStyle("-fx-background-color:transparent;");
                if (empty || v == null) { setGraphic(null); return; }
                Label b = new Label(v);
                b.setPadding(new Insets(3, 12, 3, 12));
                b.setStyle(badgeNiveau(v));
                setGraphic(b); setText(null);
            }
        });

        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colStatut.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                setStyle("-fx-background-color:transparent;");
                if (empty || v == null) { setGraphic(null); return; }
                Label b = new Label(v);
                b.setPadding(new Insets(3, 12, 3, 12));
                b.setStyle(badgeStatut(v));
                setGraphic(b); setText(null);
            }
        });

        colActions.setCellFactory(c -> new TableCell<>() {
            final Button bVal = btn("✔ Valider",  "#1B5E20", "white");
            final Button bRej = btn("✕ Rejeter",  "#B71C1C", "white");
            final Button bMod = btn("✏ Modifier", "#E65100", "white");
            final Button bDel = btn("🗑",         "#2d5a37", "#cccccc");
            {
                bVal.setOnAction(e -> {
                    service.validerAlerte(
                            getTableView().getItems().get(getIndex()).getId());
                    chargerDonnees();
                });
                bRej.setOnAction(e -> {
                    service.rejeterAlerte(
                            getTableView().getItems().get(getIndex()).getId());
                    chargerDonnees();
                });
                bMod.setOnAction(e ->
                    ouvrirModification(getTableView().getItems().get(getIndex())));
                bDel.setOnAction(e ->
                    supprimerAlerte(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setStyle("-fx-background-color:transparent;");
                if (empty) { setGraphic(null); return; }
                Alerte a = getTableView().getItems().get(getIndex());
                bVal.setDisable("Validée".equals(a.getStatut()));
                bRej.setDisable("Rejetée".equals(a.getStatut()));
                HBox box = new HBox(4, bVal, bRej, bMod, bDel);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });

        tableAlertes.setItems(data);
    }

    private TableCell<Alerte, String> cellBlanc() {
        return new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                setStyle(CELL_STYLE);
                setText(empty || v == null ? null : v);
            }
        };
    }

    private Button btn(String txt, String bg, String fg) {
        Button b = new Button(txt);
        b.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg +
                   ";-fx-background-radius:20;-fx-padding:5 12;" +
                   "-fx-font-size:10;-fx-cursor:hand;-fx-font-weight:bold;" +
                   "-fx-border-width:0;");
        b.setOnMouseEntered(e -> b.setStyle(
            "-fx-background-color:derive(" + bg + ",20%);" +
            "-fx-text-fill:" + fg + ";-fx-background-radius:20;-fx-padding:5 12;" +
            "-fx-font-size:10;-fx-cursor:hand;-fx-font-weight:bold;-fx-border-width:0;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.3),6,0,0,2);"));
        b.setOnMouseExited(e -> b.setStyle(
            "-fx-background-color:" + bg + ";-fx-text-fill:" + fg +
            ";-fx-background-radius:20;-fx-padding:5 12;" +
            "-fx-font-size:10;-fx-cursor:hand;-fx-font-weight:bold;-fx-border-width:0;"));
        return b;
    }

    private String badgeNiveau(String n) {
        return switch (n) {
            case "Critique" -> "-fx-background-color:#7f1d1d;-fx-text-fill:#fca5a5;" +
                               "-fx-background-radius:20;-fx-font-size:10;-fx-font-weight:bold;" +
                               "-fx-border-color:#ef4444;-fx-border-radius:20;-fx-border-width:1;" +
                               "-fx-padding:3 10;";
            case "Haute"    -> "-fx-background-color:#7c2d12;-fx-text-fill:#fdba74;" +
                               "-fx-background-radius:20;-fx-font-size:10;-fx-font-weight:bold;" +
                               "-fx-border-color:#f97316;-fx-border-radius:20;-fx-border-width:1;" +
                               "-fx-padding:3 10;";
            default         -> "-fx-background-color:#1e3a5f;-fx-text-fill:#93c5fd;" +
                               "-fx-background-radius:20;-fx-font-size:10;-fx-font-weight:bold;" +
                               "-fx-border-color:#3b82f6;-fx-border-radius:20;-fx-border-width:1;" +
                               "-fx-padding:3 10;";
        };
    }

    private String badgeStatut(String s) {
        return switch (s) {
            case "Validée", "Validee" ->
                "-fx-background-color:#052e16;-fx-text-fill:#4ade80;" +
                "-fx-background-radius:20;-fx-font-size:10;-fx-font-weight:bold;" +
                "-fx-border-color:#16a34a;-fx-border-radius:20;-fx-border-width:1;" +
                "-fx-padding:3 10;";
            case "Rejetée", "Rejetee" ->
                "-fx-background-color:#450a0a;-fx-text-fill:#f87171;" +
                "-fx-background-radius:20;-fx-font-size:10;-fx-font-weight:bold;" +
                "-fx-border-color:#dc2626;-fx-border-radius:20;-fx-border-width:1;" +
                "-fx-padding:3 10;";
            default ->
                "-fx-background-color:#172554;-fx-text-fill:#93c5fd;" +
                "-fx-background-radius:20;-fx-font-size:10;-fx-font-weight:bold;" +
                "-fx-border-color:#2563eb;-fx-border-radius:20;-fx-border-width:1;" +
                "-fx-padding:3 10;";
        };
    }

    @FXML
    private void exporterRapport() {
        try {
            String userHome = System.getProperty("user.home");
            String bureau   = userHome + java.io.File.separator + "Desktop";
            // Verifier si le Bureau existe sinon utiliser le dossier home
            java.io.File dossier = new java.io.File(bureau);
            if (!dossier.exists()) dossier = new java.io.File(userHome);

            String chemin = rapportService.genererRapportPDF(dossier.getAbsolutePath());
            java.io.File fichier = new java.io.File(chemin);

            // Ouvrir le PDF
            boolean ouvert = false;
            if (java.awt.Desktop.isDesktopSupported()) {
                try { java.awt.Desktop.getDesktop().open(fichier); ouvert = true; }
                catch (Exception ignored) {}
            }
            if (!ouvert) {
                String os = System.getProperty("os.name").toLowerCase();
                ProcessBuilder pb = os.contains("win")
                    ? new ProcessBuilder("cmd", "/c", "start", chemin)
                    : os.contains("mac") ? new ProcessBuilder("open", chemin)
                    : new ProcessBuilder("xdg-open", chemin);
                pb.start();
            }

            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("Rapport PDF genere");
            info.setHeaderText("Rapport PDF exporte avec succes !");
            info.setContentText("Fichier PDF sauvegarde :\n" + chemin);
            info.showAndWait();
        } catch (Exception e) {
            Alert err = new Alert(Alert.AlertType.ERROR);
            err.setTitle("Erreur");
            err.setHeaderText("Impossible de generer le rapport PDF");
            err.setContentText(e.getMessage());
            err.showAndWait();
        }
    }

            @FXML private void goUtilisateurs() {
        if (scheduler != null) scheduler.shutdown();
        Stage s = (Stage) tableAlertes.getScene().getWindow();
        NavigationManager.navigateTo(s, "/fxml/Utilisateurs.fxml");
    }
    @FXML private void goPompiers() {
        if (scheduler != null) scheduler.shutdown();
        Stage s = (Stage) tableAlertes.getScene().getWindow();
        NavigationManager.navigateTo(s, "/fxml/Pompiers.fxml");
    }

            @FXML private void goReclamations() {
        if (scheduler != null) scheduler.shutdown();
        Stage s = (Stage) tableAlertes.getScene().getWindow();
        NavigationManager.navigateTo(s, "/fxml/Reclamations.fxml");
    }
    @FXML private void goAjout() {
        if (scheduler != null) scheduler.shutdown();
        Stage s = (Stage) tableAlertes.getScene().getWindow();
        NavigationManager.navigateTo(s, "/fxml/AjoutAlerte.fxml");
    }
    @FXML private void goCapteurs() {
        if (scheduler != null) scheduler.shutdown();
        Stage s = (Stage) tableAlertes.getScene().getWindow();
        NavigationManager.navigateTo(s, "/fxml/Capteurs.fxml");
    }
}
