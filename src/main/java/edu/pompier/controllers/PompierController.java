package edu.pompier.controllers;

import edu.pompier.entities.Pompier;
import edu.pompier.services.PompierService;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.AnimationTimer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.ButtonBar;
import java.util.function.Supplier;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PompierController {

    // Sidebar
    @FXML private VBox menuLateral;
    @FXML private Button btnDashboard, btnPompiers, btnForets, btnCapteurs, btnDonnees, btnAlertes, btnUtilisateurs;
    @FXML private Button btnDeconnexion;

    // Background
    @FXML private javafx.scene.layout.StackPane mainStackPane;
    @FXML private javafx.scene.image.ImageView bgImageView;

    // Header dynamique
    @FXML private Label lblTitrePage;
    @FXML private Label lblSousTitrePage;
    @FXML private Button btnActionHeader;
    @FXML private Button btnExportHeader;
    @FXML private HBox statsBar;
    @FXML private HBox barreOnglets;

    // Dashboard
    @FXML private VBox vueDashboard;
    @FXML private Label lblDateHeure;
    @FXML private Label lblDashDispo;
    @FXML private Label lblDashMission;
    @FXML private Label lblDashTotal;
    @FXML private Label lblDashGardes;

    // Stats
    @FXML private Label lblDisponibles, lblEnMission, lblTotal, lblGardes;

    // Onglets
    @FXML private Button btnTabPompiers, btnTabGardes, btnTabAffectations;

    // Vue Pompiers
    @FXML private VBox vuePompiers;
    @FXML private ListView<Pompier> listView;
    @FXML private TextField tfRecherche;
    @FXML private HBox barreRecherche;

    // Vue Gardes
    @FXML private VBox vueGardes;
    @FXML private ListView<Pompier> listPompiers;
    @FXML private ListView<GestionGardesController.GardeInfo> listGardes;
    @FXML private Label lblPompierSelectionne, lblTitreForm;
    @FXML private ComboBox<String> cbCreneau;
    @FXML private DatePicker dpDateDebut, dpDateFin;
    @FXML private Button btnSauvegarder;
    @FXML private Label lblErreurGarde;
    @FXML private TextField tfRechercheGardes;
    // Heures de garde
    @FXML private ComboBox<String> cbHeureDebut;
    @FXML private ComboBox<String> cbHeureFin;
    @FXML private Label lblHoraireCreneau;
    @FXML private HBox hboxHoraireCreneau;
    // Prédiction IA
    @FXML private VBox panneauPrediction;
    @FXML private ListView<edu.pompier.services.PredictionDisponibilite.ResultatPrediction> listPredictions;
    @FXML private javafx.scene.control.DatePicker dpSemainePrediction;
    @FXML private Label lblSemaineInfo;
    @FXML private Label lblJourSelectionne;
    @FXML private HBox hboxJoursPrediction;
    @FXML private Button btnJourLun, btnJourMar, btnJourMer, btnJourJeu, btnJourVen, btnJourSam, btnJourDim;
    // Données brutes prédiction pour filtrage par jour
    private List<edu.pompier.services.PredictionDisponibilite.ResultatPrediction> predictionsBrutes = new ArrayList<>();
    private java.time.LocalDate jourSelectionne = java.time.LocalDate.now();
    // Filtres gardes
    @FXML private ComboBox<String> cbFiltreCreneauGarde;
    @FXML private TextField tfFiltreNomGarde;
    @FXML private TextField tfFiltreTelGarde;
    @FXML private Label lblNbGardesFiltrees;
    // Filtre forêt global (pompiers + gardes + prédiction)
    @FXML private ComboBox<String> cbFiltreForetGardes;
    @FXML private Label lblInfoFiltreForet;
    private String foretFiltreActuelle = null; // null = toutes les forêts
    // Données brutes gardes pour filtrage
    private List<GestionGardesController.GardeInfo> gardesBrutes = new ArrayList<>();

    // Vue Affectations
    @FXML private VBox vueAffectations;
    @FXML private ComboBox<String> cbAlertes;
    @FXML private Label lblResultatAffectation;
    @FXML private ListView<String[]> listAffectations;
    @FXML private Label lblStatutAffectation;
    @FXML private ListView<String[]> listAffectationsAuto;
    @FXML private ListView<String[]> listInterventions;
    @FXML private Label lblNbAffectations;
    @FXML private Label lblNbInterventions;
    // Filtres interventions
    @FXML private javafx.scene.control.DatePicker dpFiltreDate;
    @FXML private TextField tfFiltreNom;
    @FXML private TextField tfFiltreZone;
    // Filtres affectations automatiques
    @FXML private javafx.scene.control.DatePicker dpFiltreDateAff;
    @FXML private TextField tfFiltreNomAff;
    @FXML private TextField tfFiltreZoneAff;
    // Données brutes pour le filtrage
    private List<String[]> interventionsBrutes = new ArrayList<>();
    private List<String[]> affectationsAutosBrutes = new ArrayList<>();

    // Vue IA Performance
    @FXML private VBox vueIA;
    // Vue Chatbot
    @FXML private VBox vueChatbot;
    @FXML private Button btnTabChatbot;
    @FXML private javafx.scene.control.ScrollPane scrollChat;
    @FXML private VBox vboxMessages;
    @FXML private javafx.scene.control.TextField tfMessageChat;
    @FXML private Button btnTabIA;
    @FXML private ComboBox<String> cbPeriodeIA;
    @FXML private ListView<edu.pompier.services.PerformanceIA.ResultatPerformance> listPerformances;
    @FXML private VBox panneauGagnant;
    @FXML private Label lblGagnantNom;
    @FXML private Label lblGagnantScore;
    @FXML private Label lblGagnantExplication;
    @FXML private Label lblGagnantTitre;

    private boolean menuOuvert = false;
    PompierService service = new PompierService();
    private ObservableList<Pompier> listeTotale = FXCollections.observableArrayList();
    private ObservableList<Pompier> listePompiersGardes = FXCollections.observableArrayList();
    private Pompier pompierSelectionne = null;
    private int gardeIdEnEdition = -1;

    // Stockage des alertes chargées (id → index dans cbAlertes)
    private List<String[]> alertesDisponibles;

    // ── Surveillance automatique des alertes ──
    private edu.pompier.services.AlerteWatcher alerteWatcher;
    private Thread watcherThread;
    // ── Chatbot ──
    private edu.pompier.services.ChatbotService chatbotService;

    // ══════════════════════════════════════════
    //  INITIALIZE
    // ══════════════════════════════════════════
    @FXML
    public void initialize() {

        // ── Lier l'image de fond à la taille du conteneur ──
        if (bgImageView != null && mainStackPane != null) {
            bgImageView.fitWidthProperty().bind(mainStackPane.widthProperty());
            bgImageView.fitHeightProperty().bind(mainStackPane.heightProperty());
        }

        // ── CellFactory vue Pompiers ──
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Pompier p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) { setGraphic(null); setStyle("-fx-background-color: transparent;"); return; }

                String initiales = p.getNom().substring(0, 1).toUpperCase() + p.getPrenom().substring(0, 1).toUpperCase();

                // ── Avatar : photo si disponible, sinon initiales ──
                javafx.scene.Node avatar;
                if (p.getPhotoPath() != null && !p.getPhotoPath().isBlank()) {
                    try {
                        javafx.scene.image.Image img = new javafx.scene.image.Image(
                                new java.io.File(p.getPhotoPath()).toURI().toString(), 46, 46, true, true);
                        javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
                        iv.setFitWidth(46); iv.setFitHeight(46);
                        // Clip circulaire
                        javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(23, 23, 23);
                        iv.setClip(clip);
                        javafx.scene.layout.StackPane photoPane = new javafx.scene.layout.StackPane(iv);
                        photoPane.setMinWidth(46); photoPane.setMinHeight(46);
                        photoPane.setMaxWidth(46); photoPane.setMaxHeight(46);
                        photoPane.setStyle("-fx-background-radius: 50; -fx-border-color: #A5D6A7; -fx-border-radius: 50; -fx-border-width: 2;");
                        avatar = photoPane;
                    } catch (Exception ex) {
                        Label lbl = new Label(initiales);
                        lbl.setStyle("-fx-background-color: #1B5E20; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16; -fx-min-width: 46; -fx-min-height: 46; -fx-background-radius: 50; -fx-alignment: center;");
                        avatar = lbl;
                    }
                } else {
                    Label lbl = new Label(initiales);
                    lbl.setStyle("-fx-background-color: #1B5E20; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16; -fx-min-width: 46; -fx-min-height: 46; -fx-background-radius: 50; -fx-alignment: center;");
                    avatar = lbl;
                }

                Label lblNom = new Label(p.getNom() + " " + p.getPrenom());
                lblNom.setStyle("-fx-font-weight: bold; -fx-font-size: 14; -fx-text-fill: #1B5E20;");
                Label lblEmail = new Label("\u2709  " + p.getEmail());
                lblEmail.setStyle("-fx-text-fill: #2E7D32; -fx-font-size: 12;");
                Label lblTel = new Label("\ud83d\udcde  " + (p.getTelephone() != null ? p.getTelephone() : "-"));
                lblTel.setStyle("-fx-text-fill: #2E7D32; -fx-font-size: 12;");
                Label lblForet = new Label("\uD83C\uDF32  " + (p.getNomForet() != null ? p.getNomForet() : "Aucune foret"));
                lblForet.setStyle("-fx-text-fill: #1B5E20; -fx-font-size: 11; -fx-font-weight: bold;");
                Label lblCertif = new Label("\uD83C\uDF96  " + (p.getNiveauCertification() != null ? p.getNiveauCertification().toUpperCase() : "Aucune"));
                lblCertif.setStyle("-fx-text-fill: #4A148C; -fx-font-size: 11; -fx-font-weight: bold;");
                // ✅ CORRIGÉ : affichage ville / zone du pompier
                String locText = (p.getVille() != null && p.getZoneAdresse() != null)
                        ? "\uD83D\uDCCD  " + p.getVille() + " - " + p.getZoneAdresse()
                        : "\uD83D\uDCCD  Localisation non renseignee";
                Label lblLocalisation = new Label(locText);
                lblLocalisation.setStyle((p.getVille() != null)
                        ? "-fx-text-fill: #0369a1; -fx-font-size: 11; -fx-font-weight: bold;"
                        : "-fx-text-fill: #94a3b8; -fx-font-size: 11; -fx-font-style: italic;");

                boolean aUneGarde = p.getCreneauGarde() != null;
                String gardeTexte = aUneGarde
                        ? "\uD83D\uDD50  " + p.getCreneauGarde().toUpperCase()
                        + (p.getDateGarde() != null ? "  \u2022  " + p.getDateGarde() : "")
                        : "\uD83D\uDD50  Aucune garde";

                Label lblGardeInfo = new Label(gardeTexte);
                lblGardeInfo.setStyle(aUneGarde
                        ? "-fx-text-fill: #92400E; -fx-font-size: 11; -fx-font-weight: bold;"
                        : "-fx-text-fill: #BF360C; -fx-font-size: 11; -fx-font-weight: bold;");

                HBox ligneGarde = new HBox(8, lblGardeInfo);
                ligneGarde.setAlignment(Pos.CENTER_LEFT);

                if (aUneGarde) {
                    Button btnModifGarde = new Button("\u270E Garde");
                    btnModifGarde.setStyle("-fx-background-color: #FFF3E0; -fx-text-fill: #E65100; -fx-background-radius: 6; -fx-font-size: 10; -fx-font-weight: bold; -fx-padding: 3 8; -fx-cursor: hand; -fx-border-color: #FFCC80; -fx-border-radius: 6;");
                    Button btnSuppGarde = new Button("\u2716 Garde");
                    btnSuppGarde.setStyle("-fx-background-color: #FFEBEE; -fx-text-fill: #C62828; -fx-background-radius: 6; -fx-font-size: 10; -fx-font-weight: bold; -fx-padding: 3 8; -fx-cursor: hand; -fx-border-color: #FFCDD2; -fx-border-radius: 6;");
                    btnModifGarde.setOnAction(e -> ouvrirModifGardeDepuisPompier(p));
                    btnSuppGarde.setOnAction(e -> supprimerGardeDepuisPompier(p));
                    ligneGarde.getChildren().addAll(btnModifGarde, btnSuppGarde);
                } else {
                    Button btnAssigner = new Button("+ Assigner garde");
                    btnAssigner.setStyle("-fx-background-color: #E8F5E9; -fx-text-fill: #2E7D32; -fx-background-radius: 6; -fx-font-size: 10; -fx-font-weight: bold; -fx-padding: 3 8; -fx-cursor: hand; -fx-border-color: #A5D6A7; -fx-border-radius: 6;");
                    btnAssigner.setOnAction(e -> {
                        afficherVueGardes();
                        preselectPompierGardes(p);
                    });
                    ligneGarde.getChildren().add(btnAssigner);
                }

                VBox infos = new VBox(3, lblNom, lblEmail, lblTel, lblForet, lblLocalisation, lblCertif, ligneGarde);
                infos.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(infos, Priority.ALWAYS);

                String couleur = "disponible".equals(p.getStatut()) ? "#2E7D32" : "en_mission".equals(p.getStatut()) ? "#E65100" : "#757575";
                Label lblStatut = new Label(p.getStatut().replace("_", " ").toUpperCase());
                lblStatut.setStyle("-fx-background-color: " + couleur + "; -fx-text-fill: white; -fx-background-radius: 12; -fx-padding: 4 10; -fx-font-size: 10; -fx-font-weight: bold;");

                Button btnModifier = new Button("\u270E");
                btnModifier.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-size: 14; -fx-padding: 6 12;");
                Button btnSupprimer = new Button("\u2716");
                btnSupprimer.setStyle("-fx-background-color: #C62828; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-size: 14; -fx-padding: 6 12;");

                btnModifier.setOnAction(e -> {
                    try {
                        URL fxmlUrl = getClass().getClassLoader().getResource("ModifierPompier.fxml");
                        FXMLLoader loader = new FXMLLoader(fxmlUrl);
                        Parent root = loader.load();
                        ModifierPompierController controller = loader.getController();
                        controller.setPompier(p);
                        Stage stage = new Stage();
                        stage.setTitle("Modifier un pompier");
                        stage.setScene(new Scene(root));
                        stage.setOnHidden(ev -> chargerListe());
                        stage.show();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        Alert err = new Alert(Alert.AlertType.ERROR);
                        err.setTitle("Erreur");
                        err.setContentText("Impossible d'ouvrir la fenetre de modification :\n" + ex.getMessage());
                        err.showAndWait();
                    }
                });
                btnSupprimer.setOnAction(e -> afficherDialogueSuppressionPompier(p));

                VBox droite = new VBox(8, lblStatut, new HBox(6, btnModifier, btnSupprimer));
                droite.setAlignment(Pos.CENTER_RIGHT);

                HBox row = new HBox(12, avatar, infos, droite);
                row.setAlignment(Pos.CENTER_LEFT);

                String styleNormal = "-fx-padding: 12 15; -fx-background-color: rgba(220,245,220,0.85); -fx-background-radius: 10; -fx-border-color: #A5D6A7; -fx-border-radius: 10; -fx-border-width: 1;";
                String styleHover  = "-fx-padding: 12 15; -fx-background-color: rgba(200,240,200,0.95); -fx-background-radius: 10; -fx-border-color: #2E7D32; -fx-border-radius: 10; -fx-border-width: 1.5;";
                row.setStyle(styleNormal);
                row.setOnMouseEntered(e -> row.setStyle(styleHover));
                row.setOnMouseExited(e -> row.setStyle(styleNormal));
                setGraphic(row);
                setStyle("-fx-background-color: transparent; -fx-padding: 4 2;");
            }
        });

        // Recherche
        tfRecherche.textProperty().addListener((obs, o, n) -> {
            if (n == null || n.isEmpty()) { listView.setItems(listeTotale); }
            else {
                FilteredList<Pompier> f = new FilteredList<>(listeTotale,
                        p -> p.getNom().toLowerCase().contains(n.toLowerCase())
                                || p.getPrenom().toLowerCase().contains(n.toLowerCase())
                                || p.getEmail().toLowerCase().contains(n.toLowerCase()));
                listView.setItems(f);
            }
        });

        // CellFactory liste pompiers dans vue Gardes — version carte améliorée
        listPompiers.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Pompier p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) { setGraphic(null); setStyle("-fx-background-color:transparent;"); return; }

                boolean estSelectionne = isSelected();

                // ── Couleurs statut ──
                String couleurStatut = switch (p.getStatut() != null ? p.getStatut() : "") {
                    case "disponible" -> "#16a34a";
                    case "en_mission" -> "#E65100";
                    default           -> "#757575";
                };
                String bgStatut = switch (p.getStatut() != null ? p.getStatut() : "") {
                    case "disponible" -> "#dcfce7";
                    case "en_mission" -> "#ffedd5";
                    default           -> "#f1f5f9";
                };
                String emojiStatut = switch (p.getStatut() != null ? p.getStatut() : "") {
                    case "disponible" -> "\uD83D\uDFE2";
                    case "en_mission" -> "\uD83D\uDFE0";
                    default           -> "\u26AA";
                };

                // ── Avatar photo ou initiales ──
                javafx.scene.Node avatar;
                String initiales = p.getNom().substring(0,1).toUpperCase() + p.getPrenom().substring(0,1).toUpperCase();
                if (p.getPhotoPath() != null && !p.getPhotoPath().isBlank()) {
                    try {
                        javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(
                                new javafx.scene.image.Image(new java.io.File(p.getPhotoPath()).toURI().toString(), 42, 42, true, true));
                        iv.setFitWidth(42); iv.setFitHeight(42);
                        javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(21, 21, 21);
                        iv.setClip(clip);
                        javafx.scene.layout.StackPane sp = new javafx.scene.layout.StackPane(iv);
                        sp.setMinWidth(42); sp.setMinHeight(42); sp.setMaxWidth(42); sp.setMaxHeight(42);
                        sp.setStyle("-fx-border-color:" + couleurStatut + ";-fx-border-radius:50;-fx-border-width:2;");
                        avatar = sp;
                    } catch (Exception ex) {
                        Label lbl = new Label(initiales);
                        lbl.setStyle("-fx-background-color:" + couleurStatut + ";-fx-text-fill:white;-fx-font-weight:bold;"
                                + "-fx-font-size:14;-fx-min-width:42;-fx-min-height:42;-fx-background-radius:50;-fx-alignment:center;");
                        avatar = lbl;
                    }
                } else {
                    Label lbl = new Label(initiales);
                    lbl.setStyle("-fx-background-color:" + couleurStatut + ";-fx-text-fill:white;-fx-font-weight:bold;"
                            + "-fx-font-size:14;-fx-min-width:42;-fx-min-height:42;-fx-background-radius:50;-fx-alignment:center;");
                    avatar = lbl;
                }

                // ── Infos ──
                Label lblNom = new Label(p.getNom() + " " + p.getPrenom());
                lblNom.setStyle("-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:#1e293b;");
                Label lblTel = new Label(p.getTelephone() != null ? "\uD83D\uDCDE " + p.getTelephone() : "");
                lblTel.setStyle("-fx-font-size:10;-fx-text-fill:#64748b;");
                Label lblForet = new Label(p.getNomForet() != null ? "\uD83C\uDF32 " + p.getNomForet() : "");
                lblForet.setStyle("-fx-font-size:10;-fx-text-fill:#15803d;");
                VBox infos = new VBox(2, lblNom, new HBox(8, lblTel, lblForet));
                infos.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(infos, Priority.ALWAYS);

                // ── Badge statut ──
                Label lblStatut = new Label(emojiStatut + "  " + (p.getStatut() != null ? p.getStatut().replace("_"," ").toUpperCase() : "-"));
                lblStatut.setStyle("-fx-background-color:" + bgStatut + ";-fx-text-fill:" + couleurStatut + ";"
                        + "-fx-background-radius:20;-fx-padding:3 10;-fx-font-size:9;-fx-font-weight:bold;");

                HBox row = new HBox(10, avatar, infos, lblStatut);
                row.setAlignment(Pos.CENTER_LEFT);

                // Style selon sélection
                if (estSelectionne) {
                    row.setStyle("-fx-background-color:#dcfce7;-fx-background-radius:12;"
                            + "-fx-border-color:#16a34a;-fx-border-radius:12;-fx-border-width:2;-fx-padding:8 12;");
                    setStyle("-fx-background-color:transparent;-fx-padding:3 4;");
                } else {
                    row.setStyle("-fx-background-color:white;-fx-background-radius:12;"
                            + "-fx-border-color:#e2e8f0;-fx-border-radius:12;-fx-border-width:1;-fx-padding:8 12;");
                    setStyle("-fx-background-color:transparent;-fx-padding:3 4;");
                    row.setOnMouseEntered(e -> row.setStyle("-fx-background-color:#f0fdf4;-fx-background-radius:12;"
                            + "-fx-border-color:#86efac;-fx-border-radius:12;-fx-border-width:1.5;-fx-padding:8 12;-fx-cursor:hand;"));
                    row.setOnMouseExited(e -> row.setStyle("-fx-background-color:white;-fx-background-radius:12;"
                            + "-fx-border-color:#e2e8f0;-fx-border-radius:12;-fx-border-width:1;-fx-padding:8 12;"));
                }
                setGraphic(row);
            }
        });

        // Sélection pompier dans vue Gardes
        listPompiers.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) {
                pompierSelectionne = n;
                lblPompierSelectionne.setText(n.getNom() + " " + n.getPrenom() + "  |  " + (n.getTelephone() != null ? n.getTelephone() : "-"));
                lblErreurGarde.setText("");
            }
        });

        dpDateDebut.valueProperty().addListener((obs, o, n) -> {
            if (n != null && (dpDateFin.getValue() == null || dpDateFin.getValue().isBefore(n))) {
                dpDateFin.setValue(n);
            }
            lblErreurGarde.setText("");
        });
        dpDateFin.valueProperty().addListener((obs, o, n) -> lblErreurGarde.setText(""));

        cbCreneau.setItems(FXCollections.observableArrayList("matin", "apres-midi", "nuit", "24h"));
        dpDateDebut.setValue(LocalDate.now());
        dpDateFin.setValue(LocalDate.now());

        // ── Bloquer les dates passées ──
        dpDateDebut.setDayCellFactory(picker -> new javafx.scene.control.DateCell() {
            @Override public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
                if (date != null && date.isBefore(LocalDate.now()))
                    setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #94a3b8;");
            }
        });
        dpDateFin.setDayCellFactory(picker -> new javafx.scene.control.DateCell() {
            @Override public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
                if (date != null && date.isBefore(LocalDate.now()))
                    setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #94a3b8;");
            }
        });

        // ── Affichage automatique des heures selon le créneau ──
        cbCreneau.valueProperty().addListener((obs, o, n) -> mettreAJourAffichageHoraire(n));

        try { chargerListe(); } catch (Exception e) {
            System.out.println("BD non connectee : " + e.getMessage());
            lblDisponibles.setText("0"); lblEnMission.setText("0"); lblTotal.setText("0"); lblGardes.setText("0");
        }

        // ── Charger les alertes annulées en mémoire (anti-réaffectation) ──
        service.chargerAlertesAnnulees();
        // ── Initialiser le chatbot ──
        chatbotService = new edu.pompier.services.ChatbotService();

        // ── Initialiser le ComboBox période IA ──
        if (cbPeriodeIA != null) {
            cbPeriodeIA.setItems(javafx.collections.FXCollections.observableArrayList(
                    "Ce mois-ci", "Cette semaine"));
            cbPeriodeIA.setValue("Ce mois-ci");
        }

        // ── Démarrage de la surveillance automatique des alertes ──
        demarrerSurveillance();

        // ── Afficher le Dashboard par défaut ──
        afficherVueDashboard();
    }

    /**
     * Démarre le thread de surveillance en arrière-plan.
     * Toutes les 5 secondes, il vérifie les alertes non affectées
     * et déclenche l'affectation automatique si besoin.
     */
    private void demarrerSurveillance() {
        alerteWatcher = new edu.pompier.services.AlerteWatcher(service, () -> {
            // Vérifier fins d'interventions → remettre pompiers disponibles
            new edu.pompier.services.PerformanceIA().verifierFinInterventions();
            chargerListe();
            if (listAffectations != null) chargerHistoriqueAffectations();
            if (cbAlertes != null)        chargerAlertesNonAffectees();
        });

        watcherThread = new Thread(alerteWatcher);
        watcherThread.setDaemon(true);  // s'arrête quand la fenêtre se ferme
        watcherThread.setName("AlerteWatcher");
        watcherThread.start();
        System.out.println("[PompierController] Surveillance automatique démarrée.");
    }

    /** Arrête proprement le watcher (appelé si besoin à la fermeture). */
    public void arreterSurveillance() {
        if (alerteWatcher != null) alerteWatcher.arreter();
    }

    // ══════════════════════════════════════════
    //  HELPERS GARDES
    // ══════════════════════════════════════════

    private void preselectPompierGardes(Pompier p) {
        for (Pompier pg : listePompiersGardes) {
            if (pg.getId() == p.getId()) {
                listPompiers.getSelectionModel().select(pg);
                pompierSelectionne = pg;
                lblPompierSelectionne.setText(pg.getNom() + " " + pg.getPrenom() + "  |  " + pg.getTelephone());
                break;
            }
        }
    }

    private void ouvrirModifGardeDepuisPompier(Pompier p) {
        afficherVueGardes();
        List<GestionGardesController.GardeInfo> gardes = service.getAllGardesInfo();
        String dateMin = null, dateMax = null, creneau = null;
        int premierIdGarde = -1;
        for (GestionGardesController.GardeInfo g : gardes) {
            if (g.idPompier == p.getId()) {
                if (premierIdGarde == -1) { premierIdGarde = g.idGarde; creneau = g.creneau; dateMin = g.dateGarde; }
                dateMax = g.dateGarde;
            }
        }
        if (premierIdGarde == -1) return;
        gardeIdEnEdition = premierIdGarde;
        preselectPompierGardes(p);
        cbCreneau.setValue(creneau);
        try { dpDateDebut.setValue(LocalDate.parse(dateMin)); } catch (Exception ex) { dpDateDebut.setValue(LocalDate.now()); }
        try { dpDateFin.setValue(LocalDate.parse(dateMax)); }   catch (Exception ex) { dpDateFin.setValue(LocalDate.now()); }
        lblTitreForm.setText("Modifier la garde");
        btnSauvegarder.setText("Mettre a jour");
        lblErreurGarde.setText("");
    }

    private void supprimerGardeDepuisPompier(Pompier p) {
        afficherDialogueSuppressionGarde(p);
    }

    // ══════════════════════════════════════════
    //  DIALOGUES DE SUPPRESSION PERSONNALISÉS
    // ══════════════════════════════════════════

    /**
     * Affiche une boîte de dialogue stylisée pour confirmer la suppression d'un pompier.
     * Présente toutes les informations du pompier avant de demander confirmation.
     */
    private void afficherDialogueSuppressionPompier(Pompier p) {
        Stage dialog = new Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("Supprimer un pompier");
        dialog.setResizable(false);

        // ── En-tête rouge ──
        Label icone = new Label("⚠");
        icone.setStyle("-fx-font-size: 32; -fx-text-fill: white;");
        Label titre = new Label("Supprimer le pompier");
        titre.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: white;");
        Label sousTitre = new Label("Cette action est irréversible");
        sousTitre.setStyle("-fx-font-size: 11; -fx-text-fill: rgba(255,255,255,0.75);");
        VBox texteHeader = new VBox(3, titre, sousTitre);
        texteHeader.setAlignment(Pos.CENTER_LEFT);
        HBox header = new HBox(14, icone, texteHeader);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: linear-gradient(to right, #b91c1c, #dc2626); "
                + "-fx-padding: 20 24; -fx-background-radius: 16 16 0 0;");

        // ── Fiche identité ──
        String initiales = p.getNom().substring(0, 1).toUpperCase() + p.getPrenom().substring(0, 1).toUpperCase();
        Label avatar = new Label(initiales);
        avatar.setStyle("-fx-background-color: #C62828; -fx-text-fill: white; -fx-font-weight: bold; "
                + "-fx-font-size: 20; -fx-min-width: 54; -fx-min-height: 54; "
                + "-fx-background-radius: 50; -fx-alignment: center;");

        Label lblNomComplet = new Label(p.getNom().toUpperCase() + " " + p.getPrenom());
        lblNomComplet.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #1a1a1a;");

        String couleurStatut = "disponible".equals(p.getStatut()) ? "#16a34a" : "en_mission".equals(p.getStatut()) ? "#E65100" : "#757575";
        Label lblStatutBadge = new Label(p.getStatut() != null ? p.getStatut().replace("_", " ").toUpperCase() : "-");
        lblStatutBadge.setStyle("-fx-background-color: " + couleurStatut + "; -fx-text-fill: white; "
                + "-fx-background-radius: 10; -fx-padding: 3 10; -fx-font-size: 10; -fx-font-weight: bold;");

        HBox ligneNom = new HBox(10, lblNomComplet, lblStatutBadge);
        ligneNom.setAlignment(Pos.CENTER_LEFT);

        VBox identite = new VBox(4, ligneNom);
        identite.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(identite, Priority.ALWAYS);

        HBox ficheTop = new HBox(14, avatar, identite);
        ficheTop.setAlignment(Pos.CENTER_LEFT);
        ficheTop.setStyle("-fx-padding: 0 0 12 0; -fx-border-color: #e5e7eb; -fx-border-width: 0 0 1 0;");

        // ── Détails en grille ──
        VBox details = new VBox(8);
        details.setStyle("-fx-padding: 12 0 0 0;");

        details.getChildren().addAll(
                ligneDetail("✉  Email",       p.getEmail() != null ? p.getEmail() : "—"),
                ligneDetail("📞  Téléphone",   p.getTelephone() != null ? p.getTelephone() : "—"),
                ligneDetail("🌲  Forêt",       p.getNomForet() != null ? p.getNomForet() : "Aucune"),
                ligneDetail("📍  Localisation", (p.getVille() != null ? p.getVille() : "—")
                        + (p.getZoneAdresse() != null ? " — " + p.getZoneAdresse() : "")),
                ligneDetail("🎖  Certification", p.getNiveauCertification() != null
                        ? p.getNiveauCertification().toUpperCase() : "Aucune"),
                ligneDetail("🕐  Garde",       p.getCreneauGarde() != null
                        ? p.getCreneauGarde().toUpperCase() + (p.getDateGarde() != null ? "  •  " + p.getDateGarde() : "")
                        : "Aucune garde assignée")
        );

        VBox fiche = new VBox(0, ficheTop, details);
        fiche.setStyle("-fx-background-color: #f9fafb; -fx-background-radius: 12; "
                + "-fx-border-color: #e5e7eb; -fx-border-radius: 12; -fx-border-width: 1; -fx-padding: 16 18;");

        // ── Avertissement ──
        Label avertissement = new Label("⚠  Toutes les données de ce pompier seront définitivement supprimées,\n"
                + "y compris ses gardes et son historique d'affectations.");
        avertissement.setWrapText(true);
        avertissement.setStyle("-fx-font-size: 11; -fx-text-fill: #92400E; -fx-background-color: #FEF3C7; "
                + "-fx-background-radius: 8; -fx-border-color: #FCD34D; -fx-border-radius: 8; "
                + "-fx-border-width: 1; -fx-padding: 10 14;");

        // ── Boutons ──
        Button btnAnnuler = new Button("Annuler");
        btnAnnuler.setPrefWidth(120);
        btnAnnuler.setPrefHeight(40);
        btnAnnuler.setStyle("-fx-background-color: white; -fx-text-fill: #374151; -fx-font-size: 13; "
                + "-fx-font-weight: bold; -fx-border-color: #d1d5db; -fx-border-radius: 10; "
                + "-fx-background-radius: 10; -fx-cursor: hand;");
        btnAnnuler.setOnAction(e -> dialog.close());

        Button btnConfirmer = new Button("🗑  Supprimer définitivement");
        btnConfirmer.setPrefHeight(40);
        btnConfirmer.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-size: 13; "
                + "-fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand; "
                + "-fx-effect: dropshadow(gaussian,rgba(220,38,38,0.5),10,0,0,3);");
        btnConfirmer.setOnAction(e -> {
            service.deleteEntity(p);
            chargerListe();
            dialog.close();
        });

        // Hover sur le bouton supprimer
        btnConfirmer.setOnMouseEntered(e -> btnConfirmer.setStyle("-fx-background-color: #b91c1c; -fx-text-fill: white; "
                + "-fx-font-size: 13; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand; "
                + "-fx-effect: dropshadow(gaussian,rgba(185,28,28,0.7),14,0,0,4);"));
        btnConfirmer.setOnMouseExited(e -> btnConfirmer.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; "
                + "-fx-font-size: 13; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand; "
                + "-fx-effect: dropshadow(gaussian,rgba(220,38,38,0.5),10,0,0,3);"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox boutons = new HBox(10, spacer, btnAnnuler, btnConfirmer);
        boutons.setAlignment(Pos.CENTER_RIGHT);
        boutons.setStyle("-fx-padding: 16 0 0 0; -fx-border-color: #e5e7eb; -fx-border-width: 1 0 0 0;");

        // ── Assemblage ──
        VBox corps = new VBox(14, fiche, avertissement, boutons);
        corps.setStyle("-fx-padding: 20 24 20 24;");

        VBox root = new VBox(0, header, corps);
        root.setStyle("-fx-background-color: white; -fx-background-radius: 16; "
                + "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.35),24,0,0,8);");
        root.setPrefWidth(460);

        Scene scene = new Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.show();
    }

    /**
     * Ligne de détail clé / valeur pour la fiche pompier dans le dialogue.
     */
    private HBox ligneDetail(String cle, String valeur) {
        Label lblCle = new Label(cle);
        lblCle.setMinWidth(130);
        lblCle.setStyle("-fx-font-size: 11; -fx-font-weight: bold; -fx-text-fill: #6b7280;");
        Label lblVal = new Label(valeur);
        lblVal.setStyle("-fx-font-size: 12; -fx-text-fill: #111827;");
        lblVal.setWrapText(true);
        HBox ligne = new HBox(8, lblCle, lblVal);
        ligne.setAlignment(Pos.CENTER_LEFT);
        return ligne;
    }

    /**
     * Affiche une boîte de dialogue stylisée pour confirmer la suppression des gardes d'un pompier.
     */
    private void afficherDialogueSuppressionGarde(Pompier p) {
        Stage dialog = new Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("Supprimer les gardes");
        dialog.setResizable(false);

        // ── En-tête orange ──
        Label icone = new Label("🕐");
        icone.setStyle("-fx-font-size: 28; -fx-text-fill: white;");
        Label titre = new Label("Supprimer les gardes");
        titre.setStyle("-fx-font-size: 17; -fx-font-weight: bold; -fx-text-fill: white;");
        Label sousTitre = new Label("Toutes les gardes de ce pompier seront supprimées");
        sousTitre.setStyle("-fx-font-size: 11; -fx-text-fill: rgba(255,255,255,0.75);");
        VBox texteHeader = new VBox(3, titre, sousTitre);
        texteHeader.setAlignment(Pos.CENTER_LEFT);
        HBox header = new HBox(14, icone, texteHeader);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: linear-gradient(to right, #b45309, #d97706); "
                + "-fx-padding: 18 24; -fx-background-radius: 16 16 0 0;");

        // ── Info pompier ──
        String initiales = p.getNom().substring(0, 1).toUpperCase() + p.getPrenom().substring(0, 1).toUpperCase();
        Label avatar = new Label(initiales);
        avatar.setStyle("-fx-background-color: #D97706; -fx-text-fill: white; -fx-font-weight: bold; "
                + "-fx-font-size: 18; -fx-min-width: 46; -fx-min-height: 46; "
                + "-fx-background-radius: 50; -fx-alignment: center;");

        Label lblNom = new Label(p.getNom() + " " + p.getPrenom());
        lblNom.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #1a1a1a;");

        // Récupérer les gardes pour afficher le détail
        List<GestionGardesController.GardeInfo> gardes = service.getAllGardesInfo();
        long nbGardes = gardes.stream().filter(g -> g.idPompier == p.getId()).count();

        Label lblGardeInfo = new Label("🕐  " + (p.getCreneauGarde() != null
                ? p.getCreneauGarde().toUpperCase() + (p.getDateGarde() != null ? "  •  " + p.getDateGarde() : "")
                : "Créneau non défini"));
        lblGardeInfo.setStyle("-fx-font-size: 12; -fx-text-fill: #92400E; -fx-font-weight: bold;");

        Label lblNbGardes = new Label("📋  " + nbGardes + " entrée(s) de garde à supprimer");
        lblNbGardes.setStyle("-fx-font-size: 11; -fx-text-fill: #6b7280;");

        VBox infoPompier = new VBox(4, lblNom, lblGardeInfo, lblNbGardes);
        infoPompier.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(infoPompier, Priority.ALWAYS);

        HBox ficheTop = new HBox(14, avatar, infoPompier);
        ficheTop.setAlignment(Pos.CENTER_LEFT);
        ficheTop.setStyle("-fx-background-color: #fffbeb; -fx-background-radius: 12; "
                + "-fx-border-color: #fcd34d; -fx-border-radius: 12; -fx-border-width: 1; -fx-padding: 14 16;");

        // ── Avertissement ──
        Label avertissement = new Label("ℹ  Le pompier ne sera pas supprimé, uniquement ses gardes.\n"
                + "Il restera dans la liste avec le statut actuel.");
        avertissement.setWrapText(true);
        avertissement.setStyle("-fx-font-size: 11; -fx-text-fill: #1e40af; -fx-background-color: #eff6ff; "
                + "-fx-background-radius: 8; -fx-border-color: #bfdbfe; -fx-border-radius: 8; "
                + "-fx-border-width: 1; -fx-padding: 10 14;");

        // ── Boutons ──
        Button btnAnnuler = new Button("Annuler");
        btnAnnuler.setPrefWidth(110);
        btnAnnuler.setPrefHeight(38);
        btnAnnuler.setStyle("-fx-background-color: white; -fx-text-fill: #374151; -fx-font-size: 13; "
                + "-fx-font-weight: bold; -fx-border-color: #d1d5db; -fx-border-radius: 10; "
                + "-fx-background-radius: 10; -fx-cursor: hand;");
        btnAnnuler.setOnAction(e -> dialog.close());

        Button btnConfirmer = new Button("🗑  Supprimer les gardes");
        btnConfirmer.setPrefHeight(38);
        btnConfirmer.setStyle("-fx-background-color: #d97706; -fx-text-fill: white; -fx-font-size: 13; "
                + "-fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand; "
                + "-fx-effect: dropshadow(gaussian,rgba(217,119,6,0.5),10,0,0,3);");
        btnConfirmer.setOnAction(e -> {
            for (GestionGardesController.GardeInfo g : gardes) {
                if (g.idPompier == p.getId()) service.deleteGardeById(g.idGarde);
            }
            chargerListe();
            dialog.close();
        });

        btnConfirmer.setOnMouseEntered(e -> btnConfirmer.setStyle("-fx-background-color: #b45309; -fx-text-fill: white; "
                + "-fx-font-size: 13; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand; "
                + "-fx-effect: dropshadow(gaussian,rgba(180,83,9,0.7),14,0,0,4);"));
        btnConfirmer.setOnMouseExited(e -> btnConfirmer.setStyle("-fx-background-color: #d97706; -fx-text-fill: white; "
                + "-fx-font-size: 13; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand; "
                + "-fx-effect: dropshadow(gaussian,rgba(217,119,6,0.5),10,0,0,3);"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox boutons = new HBox(10, spacer, btnAnnuler, btnConfirmer);
        boutons.setAlignment(Pos.CENTER_RIGHT);
        boutons.setStyle("-fx-padding: 14 0 0 0; -fx-border-color: #e5e7eb; -fx-border-width: 1 0 0 0;");

        // ── Assemblage ──
        VBox corps = new VBox(14, ficheTop, avertissement, boutons);
        corps.setStyle("-fx-padding: 20 24 20 24;");

        VBox root = new VBox(0, header, corps);
        root.setStyle("-fx-background-color: white; -fx-background-radius: 16; "
                + "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.35),24,0,0,8);");
        root.setPrefWidth(440);

        Scene scene = new Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.show();
    }

    // ══════════════════════════════════════════
    //  ONGLETS
    // ══════════════════════════════════════════

    /** Affiche ou cache la barre de recherche selon la vue active */
    private void setBarreRechercheVisible(boolean visible) {
        if (barreRecherche != null) {
            barreRecherche.setVisible(visible);
            barreRecherche.setManaged(visible);
        }
    }

    /** Met à jour le header selon la vue active */
    private void setHeader(String titre, String sousTitre, boolean showActions, boolean showStats, boolean showOnglets) {
        if (lblTitrePage != null) lblTitrePage.setText(titre);
        if (lblSousTitrePage != null) lblSousTitrePage.setText(sousTitre);
        if (btnActionHeader != null) { btnActionHeader.setVisible(showActions); btnActionHeader.setManaged(showActions); }
        if (btnExportHeader != null) { btnExportHeader.setVisible(showActions); btnExportHeader.setManaged(showActions); }
        if (statsBar != null) { statsBar.setVisible(showStats); statsBar.setManaged(showStats); }
        if (barreOnglets != null) { barreOnglets.setVisible(showOnglets); barreOnglets.setManaged(showOnglets); }
    }

    /** Met à jour le bouton actif dans la sidebar */
    private void setSidebarActif(Button actif) {
        String styleActif = "-fx-background-color: #16a34a; -fx-text-fill: white; -fx-font-size: 13; -fx-font-weight: bold; -fx-alignment: CENTER_LEFT; -fx-padding: 10 16; -fx-background-radius: 12; -fx-cursor: hand; -fx-effect: dropshadow(gaussian,rgba(22,163,74,0.4),10,0,0,3);";
        String styleInactif = "-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 13; -fx-alignment: CENTER_LEFT; -fx-padding: 10 16; -fx-background-radius: 12; -fx-cursor: hand;";
        Button[] tous = {btnDashboard, btnPompiers, btnForets, btnCapteurs, btnDonnees, btnAlertes, btnUtilisateurs};
        for (Button b : tous) {
            if (b != null) b.setStyle(b == actif ? styleActif : styleInactif);
        }
    }

    @FXML
    public void afficherVueDashboard() {
        // Cacher toutes les vues
        if (vueDashboard != null)      { vueDashboard.setVisible(true);      vueDashboard.setManaged(true); }
        if (vuePompiers != null)       { vuePompiers.setVisible(false);       vuePompiers.setManaged(false); }
        if (vueGardes != null)         { vueGardes.setVisible(false);         vueGardes.setManaged(false); }
        if (vueAffectations != null)   { vueAffectations.setVisible(false);   vueAffectations.setManaged(false); }
        if (vueIA != null)             { vueIA.setVisible(false);             vueIA.setManaged(false); }
        if (vueChatbot != null)        { vueChatbot.setVisible(false);        vueChatbot.setManaged(false); }

        setHeader("Tableau de Bord", "Vue d'ensemble de l'application ForestGuard", false, false, false);
        setBarreRechercheVisible(false);
        setSidebarActif(btnDashboard);

        // Date et heure
        if (lblDateHeure != null) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy  •  HH:mm", java.util.Locale.FRENCH);
            lblDateHeure.setText(LocalDateTime.now().format(fmt));
        }

        // Charger les stats dans les cartes du dashboard
        try {
            List<Pompier> liste = service.getData();
            long dispo   = liste.stream().filter(p -> "disponible".equals(p.getStatut())).count();
            long mission = liste.stream().filter(p -> "en_mission".equals(p.getStatut())).count();
            long total   = liste.size();
            long gardes  = service.getAllGardesInfo().size();

            if (lblDashDispo    != null) lblDashDispo.setText(String.valueOf(dispo));
            if (lblDashMission  != null) lblDashMission.setText(String.valueOf(mission));
            if (lblDashTotal    != null) lblDashTotal.setText(String.valueOf(total));
            if (lblDashGardes   != null) lblDashGardes.setText(String.valueOf(gardes));

            // Synchroniser aussi les labels du header stats
            if (lblDisponibles  != null) lblDisponibles.setText(String.valueOf(dispo));
            if (lblEnMission    != null) lblEnMission.setText(String.valueOf(mission));
            if (lblTotal        != null) lblTotal.setText(String.valueOf(total));
            if (lblGardes       != null) lblGardes.setText(String.valueOf(gardes));
        } catch (Exception e) {
            System.out.println("[Dashboard] BD non connectée : " + e.getMessage());
        }
    }

    @FXML
    public void afficherVuePompiers() {
        if (vueDashboard != null)  { vueDashboard.setVisible(false);  vueDashboard.setManaged(false); }
        vuePompiers.setVisible(true);      vuePompiers.setManaged(true);
        vueGardes.setVisible(false);       vueGardes.setManaged(false);
        vueAffectations.setVisible(false); vueAffectations.setManaged(false);
        if (vueIA != null) { vueIA.setVisible(false); vueIA.setManaged(false); }
        if (vueChatbot != null) { vueChatbot.setVisible(false); vueChatbot.setManaged(false); }
        setHeader("Gestion des Pompiers", "Liste, gardes et affectations automatiques", true, true, true);
        setSidebarActif(btnPompiers);
        if (btnTabPompiers != null) btnTabPompiers.setStyle("-fx-background-color:#16a34a;-fx-text-fill:white;-fx-font-size:13;-fx-font-weight:bold;-fx-padding:8 20;-fx-cursor:hand;-fx-effect:dropshadow(gaussian,rgba(22,163,74,0.6),10,0,0,3);-fx-border-color:#15803d;-fx-border-width:1;-fx-background-radius:10 0 0 10;-fx-border-radius:10 0 0 10;");
        if (btnTabGardes != null) btnTabGardes.setStyle("-fx-background-color:rgba(255,255,255,0.10);-fx-text-fill:rgba(255,255,255,0.65);-fx-font-size:13;-fx-font-weight:bold;-fx-padding:8 20;-fx-cursor:hand;-fx-border-color:rgba(255,255,255,0.2);-fx-border-width:1;-fx-background-radius:0;-fx-border-radius:0;");
        if (btnTabAffectations != null) btnTabAffectations.setStyle("-fx-background-color:rgba(255,255,255,0.10);-fx-text-fill:rgba(255,255,255,0.65);-fx-font-size:13;-fx-font-weight:bold;-fx-padding:8 20;-fx-cursor:hand;-fx-border-color:rgba(255,255,255,0.2);-fx-border-width:1;-fx-background-radius:0;-fx-border-radius:0;");
        if (btnTabIA != null) btnTabIA.setStyle("-fx-background-color:rgba(255,255,255,0.10);-fx-text-fill:rgba(255,255,255,0.65);-fx-font-size:13;-fx-font-weight:bold;-fx-padding:8 20;-fx-cursor:hand;-fx-border-color:rgba(255,255,255,0.2);-fx-border-width:1;-fx-background-radius:0;-fx-border-radius:0;");
        if (btnTabChatbot != null) btnTabChatbot.setStyle("-fx-background-color:rgba(255,255,255,0.10);-fx-text-fill:rgba(255,255,255,0.65);-fx-font-size:13;-fx-font-weight:bold;-fx-padding:8 20;-fx-cursor:hand;-fx-border-color:rgba(255,255,255,0.2);-fx-border-width:1;-fx-background-radius:0 10 10 0;-fx-border-radius:0 10 10 0;");
        chargerListe();
        setBarreRechercheVisible(true);
    }

    @FXML
    public void afficherVueGardes() {
        if (vueDashboard != null)  { vueDashboard.setVisible(false);  vueDashboard.setManaged(false); }
        vuePompiers.setVisible(false);     vuePompiers.setManaged(false);
        vueGardes.setVisible(true);        vueGardes.setManaged(true);
        vueAffectations.setVisible(false); vueAffectations.setManaged(false);
        if (vueIA != null) { vueIA.setVisible(false); vueIA.setManaged(false); }
        if (vueChatbot != null) { vueChatbot.setVisible(false); vueChatbot.setManaged(false); }
        setHeader("Gestion des Gardes", "Planifier et suivre les gardes des pompiers", false, true, true);
        setSidebarActif(btnPompiers);
        if (btnTabGardes != null) btnTabGardes.setStyle("-fx-background-color:#16a34a;-fx-text-fill:white;-fx-font-size:13;-fx-font-weight:bold;-fx-padding:8 20;-fx-cursor:hand;-fx-effect:dropshadow(gaussian,rgba(22,163,74,0.6),10,0,0,3);-fx-border-color:#15803d;-fx-border-width:1;-fx-background-radius:0;-fx-border-radius:0;");
        if (btnTabPompiers != null) btnTabPompiers.setStyle("-fx-background-color:rgba(255,255,255,0.10);-fx-text-fill:rgba(255,255,255,0.65);-fx-font-size:13;-fx-font-weight:bold;-fx-padding:8 20;-fx-cursor:hand;-fx-border-color:rgba(255,255,255,0.2);-fx-border-width:1;-fx-background-radius:10 0 0 10;-fx-border-radius:10 0 0 10;");
        if (btnTabAffectations != null) btnTabAffectations.setStyle("-fx-background-color:rgba(255,255,255,0.10);-fx-text-fill:rgba(255,255,255,0.65);-fx-font-size:13;-fx-font-weight:bold;-fx-padding:8 20;-fx-cursor:hand;-fx-border-color:rgba(255,255,255,0.2);-fx-border-width:1;-fx-background-radius:0;-fx-border-radius:0;");
        if (btnTabIA != null) btnTabIA.setStyle("-fx-background-color:rgba(255,255,255,0.10);-fx-text-fill:rgba(255,255,255,0.65);-fx-font-size:13;-fx-font-weight:bold;-fx-padding:8 20;-fx-cursor:hand;-fx-border-color:rgba(255,255,255,0.2);-fx-border-width:1;-fx-background-radius:0;-fx-border-radius:0;");
        if (btnTabChatbot != null) btnTabChatbot.setStyle("-fx-background-color:rgba(255,255,255,0.10);-fx-text-fill:rgba(255,255,255,0.65);-fx-font-size:13;-fx-font-weight:bold;-fx-padding:8 20;-fx-cursor:hand;-fx-border-color:rgba(255,255,255,0.2);-fx-border-width:1;-fx-background-radius:0 10 10 0;-fx-border-radius:0 10 10 0;");
        chargerPompiersGardes();
        chargerGardesListe();
        setBarreRechercheVisible(false);

        // ── Initialiser le filtre forêt (une seule fois) ──
        if (cbFiltreForetGardes != null && cbFiltreForetGardes.getUserData() == null) {
            cbFiltreForetGardes.setUserData("init");
            // Charger la liste des forêts depuis la BDD
            List<String[]> forets = service.getForets();
            javafx.collections.ObservableList<String> nomsForets = FXCollections.observableArrayList();
            nomsForets.add("Toutes les forets");
            for (String[] f : forets) nomsForets.add(f[1]);
            cbFiltreForetGardes.setItems(nomsForets);
            cbFiltreForetGardes.setValue("Toutes les forets");
            cbFiltreForetGardes.valueProperty().addListener((obs, o, n) -> {
                foretFiltreActuelle = (n == null || "Toutes les forets".equals(n)) ? null : n;
                appliquerFiltreForet();
            });
        }
    }

    @FXML
    public void afficherVueAffectations() {
        if (vueDashboard != null)  { vueDashboard.setVisible(false);  vueDashboard.setManaged(false); }
        vuePompiers.setVisible(false);     vuePompiers.setManaged(false);
        vueGardes.setVisible(false);       vueGardes.setManaged(false);
        vueAffectations.setVisible(true);  vueAffectations.setManaged(true);
        if (vueIA != null) { vueIA.setVisible(false); vueIA.setManaged(false); }
        if (vueChatbot != null) { vueChatbot.setVisible(false); vueChatbot.setManaged(false); }
        setHeader("Affectations & Alertes", "Gerer les alertes et affecter les pompiers aux interventions", false, true, true);
        setSidebarActif(btnPompiers);
        if (btnTabAffectations != null) btnTabAffectations.setStyle("-fx-background-color:#16a34a;-fx-text-fill:white;-fx-font-size:13;-fx-font-weight:bold;-fx-padding:8 20;-fx-cursor:hand;-fx-effect:dropshadow(gaussian,rgba(22,163,74,0.6),10,0,0,3);-fx-border-color:#15803d;-fx-border-width:1;-fx-background-radius:0;-fx-border-radius:0;");
        if (btnTabPompiers != null) btnTabPompiers.setStyle("-fx-background-color:rgba(255,255,255,0.10);-fx-text-fill:rgba(255,255,255,0.65);-fx-font-size:13;-fx-font-weight:bold;-fx-padding:8 20;-fx-cursor:hand;-fx-border-color:rgba(255,255,255,0.2);-fx-border-width:1;-fx-background-radius:10 0 0 10;-fx-border-radius:10 0 0 10;");
        if (btnTabGardes != null) btnTabGardes.setStyle("-fx-background-color:rgba(255,255,255,0.10);-fx-text-fill:rgba(255,255,255,0.65);-fx-font-size:13;-fx-font-weight:bold;-fx-padding:8 20;-fx-cursor:hand;-fx-border-color:rgba(255,255,255,0.2);-fx-border-width:1;-fx-background-radius:0;-fx-border-radius:0;");
        if (btnTabIA != null) btnTabIA.setStyle("-fx-background-color:rgba(255,255,255,0.10);-fx-text-fill:rgba(255,255,255,0.65);-fx-font-size:13;-fx-font-weight:bold;-fx-padding:8 20;-fx-cursor:hand;-fx-border-color:rgba(255,255,255,0.2);-fx-border-width:1;-fx-background-radius:0;-fx-border-radius:0;");
        if (btnTabChatbot != null) btnTabChatbot.setStyle("-fx-background-color:rgba(255,255,255,0.10);-fx-text-fill:rgba(255,255,255,0.65);-fx-font-size:13;-fx-font-weight:bold;-fx-padding:8 20;-fx-cursor:hand;-fx-border-color:rgba(255,255,255,0.2);-fx-border-width:1;-fx-background-radius:0 10 10 0;-fx-border-radius:0 10 10 0;");
        chargerAlertesNonAffectees();
        chargerHistoriqueAffectations();
        setBarreRechercheVisible(false);
    }

    @FXML
    public void afficherVueIA() {
        if (vueDashboard != null)  { vueDashboard.setVisible(false);  vueDashboard.setManaged(false); }
        vuePompiers.setVisible(false);     vuePompiers.setManaged(false);
        vueGardes.setVisible(false);       vueGardes.setManaged(false);
        vueAffectations.setVisible(false); vueAffectations.setManaged(false);
        if (vueIA != null) { vueIA.setVisible(true); vueIA.setManaged(true); }
        if (vueChatbot != null) { vueChatbot.setVisible(false); vueChatbot.setManaged(false); }
        setHeader("IA Performance", "Analyser les performances des pompiers par intelligence artificielle", false, true, true);
        setSidebarActif(btnPompiers);
        if (btnTabIA != null) btnTabIA.setStyle("-fx-background-color:#16a34a;-fx-text-fill:white;-fx-font-size:13;-fx-font-weight:bold;-fx-padding:8 20;-fx-cursor:hand;-fx-effect:dropshadow(gaussian,rgba(22,163,74,0.6),10,0,0,3);-fx-border-color:#15803d;-fx-border-width:1;-fx-background-radius:0;-fx-border-radius:0;");
        if (btnTabPompiers != null) btnTabPompiers.setStyle("-fx-background-color:rgba(255,255,255,0.10);-fx-text-fill:rgba(255,255,255,0.65);-fx-font-size:13;-fx-font-weight:bold;-fx-padding:8 20;-fx-cursor:hand;-fx-border-color:rgba(255,255,255,0.2);-fx-border-width:1;-fx-background-radius:10 0 0 10;-fx-border-radius:10 0 0 10;");
        if (btnTabGardes != null) btnTabGardes.setStyle("-fx-background-color:rgba(255,255,255,0.10);-fx-text-fill:rgba(255,255,255,0.65);-fx-font-size:13;-fx-font-weight:bold;-fx-padding:8 20;-fx-cursor:hand;-fx-border-color:rgba(255,255,255,0.2);-fx-border-width:1;-fx-background-radius:0;-fx-border-radius:0;");
        if (btnTabAffectations != null) btnTabAffectations.setStyle("-fx-background-color:rgba(255,255,255,0.10);-fx-text-fill:rgba(255,255,255,0.65);-fx-font-size:13;-fx-font-weight:bold;-fx-padding:8 20;-fx-cursor:hand;-fx-border-color:rgba(255,255,255,0.2);-fx-border-width:1;-fx-background-radius:0;-fx-border-radius:0;");
        if (btnTabChatbot != null) btnTabChatbot.setStyle("-fx-background-color:rgba(255,255,255,0.10);-fx-text-fill:rgba(255,255,255,0.65);-fx-font-size:13;-fx-font-weight:bold;-fx-padding:8 20;-fx-cursor:hand;-fx-border-color:rgba(255,255,255,0.2);-fx-border-width:1;-fx-background-radius:0 10 10 0;-fx-border-radius:0 10 10 0;");
        chargerPerformances();
        setBarreRechercheVisible(false);
    }

    @FXML
    public void chargerPerformances() {
        if (listPerformances == null) return;
        String periode = (cbPeriodeIA != null && cbPeriodeIA.getValue() != null
                && cbPeriodeIA.getValue().contains("semaine")) ? "semaine" : "mois";

        edu.pompier.services.PerformanceIA ia = new edu.pompier.services.PerformanceIA();
        List<edu.pompier.services.PerformanceIA.ResultatPerformance> resultats = ia.evaluer(periode);

        // Mise à jour labels cachés (compatibilité)
        if (!resultats.isEmpty()) {
            edu.pompier.services.PerformanceIA.ResultatPerformance g = resultats.get(0);
            if (lblGagnantNom   != null) lblGagnantNom.setText(g.prenom + " " + g.nom);
            if (lblGagnantScore != null) lblGagnantScore.setText(String.format("%.1f", g.scoreTotal));
        }

        listPerformances.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(edu.pompier.services.PerformanceIA.ResultatPerformance r, boolean empty) {
                super.updateItem(r, empty);
                setGraphic(null); setText(null);
                if (empty || r == null) { setStyle("-fx-background-color:transparent;"); return; }

                int rang = getIndex();

                // ══════════════════════════════════
                // PODIUM (rangs 0, 1, 2) — rendu SVG
                // ══════════════════════════════════
                if (rang == 0) {
                    // Podium complet : 2ème | 1er | 3ème
                    // Nécessite les 3 premiers résultats
                    edu.pompier.services.PerformanceIA.ResultatPerformance r1 = resultats.get(0);
                    edu.pompier.services.PerformanceIA.ResultatPerformance r2 = resultats.size() > 1 ? resultats.get(1) : null;
                    edu.pompier.services.PerformanceIA.ResultatPerformance r3 = resultats.size() > 2 ? resultats.get(2) : null;

                    VBox podiumContainer = construirePodium(r1, r2, r3);
                    setGraphic(podiumContainer);
                    setStyle("-fx-background-color:transparent;-fx-padding:0;");
                    setPrefHeight(javafx.scene.control.Control.USE_COMPUTED_SIZE);
                    return;
                }
                // Sauter les rangs 1 et 2 (déjà affichés dans le podium)
                if (rang == 1 || rang == 2) {
                    setPrefHeight(0); setMaxHeight(0); setStyle("-fx-background-color:transparent;-fx-padding:0;");
                    return;
                }

                // ══════════════════════════════════
                // En-tête "Autres participants" avant le rang 3
                // ══════════════════════════════════
                if (rang == 3) {
                    Label sep = new Label("  Autres participants");
                    sep.setStyle("-fx-font-size:12;-fx-font-weight:bold;-fx-text-fill:#374151;" +
                            "-fx-background-color:rgba(255,255,255,0.85);-fx-background-radius:8;" +
                            "-fx-padding:8 16;-fx-border-color:#e2e8f0;-fx-border-radius:8;-fx-border-width:1;");
                    VBox wrap = new VBox(sep);
                    wrap.setStyle("-fx-padding:8 0 4 0;");
                    setGraphic(wrap);
                    setStyle("-fx-background-color:transparent;");
                    // PUIS afficher la ligne du rang 3 dans la même cellule
                }

                // ══════════════════════════════════
                // Tableau des autres (rang >= 3)
                // ══════════════════════════════════
                int rangAff = rang + 1; // 1-based
                String scoreBarColor = r.scoreTotal >= 60 ? "#16a34a" : r.scoreTotal >= 40 ? "#0369a1" : "#94a3b8";

                Label lblRangNum = new Label(String.valueOf(rangAff));
                lblRangNum.setStyle("-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:#64748b;" +
                        "-fx-min-width:28;-fx-alignment:center;");

                String initiales = r.nom.substring(0,1).toUpperCase() + r.prenom.substring(0,1).toUpperCase();
                Label avatar = new Label(initiales);
                avatar.setStyle("-fx-background-color:#e2e8f0;-fx-text-fill:#475569;-fx-font-weight:bold;" +
                        "-fx-font-size:12;-fx-min-width:34;-fx-min-height:34;-fx-background-radius:50;-fx-alignment:center;");

                Label lblNom = new Label(r.prenom + " " + r.nom);
                lblNom.setStyle("-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:#1e293b;");
                HBox.setHgrow(lblNom, Priority.ALWAYS);

                Label lblMiss = new Label(r.nbMissions + " miss.");
                lblMiss.setStyle("-fx-font-size:10;-fx-background-color:#dbeafe;-fx-text-fill:#1d4ed8;-fx-background-radius:5;-fx-padding:2 6;");
                Label lblTaux = new Label(r.tauxReussite > 0 ? Math.round(r.tauxReussite*100)+"%" : "N/A");
                lblTaux.setStyle("-fx-font-size:10;-fx-background-color:#dcfce7;-fx-text-fill:#166534;-fx-background-radius:5;-fx-padding:2 6;");
                Label lblScore = new Label(String.format("%.0f pts", r.scoreTotal));
                lblScore.setStyle("-fx-font-size:12;-fx-font-weight:bold;-fx-text-fill:" + scoreBarColor + ";");

                HBox row = new HBox(10, lblRangNum, avatar, lblNom, lblMiss, lblTaux, lblScore);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-background-color:rgba(255,255,255,0.90);-fx-background-radius:10;" +
                        "-fx-border-color:#e2e8f0;-fx-border-radius:10;-fx-border-width:1;-fx-padding:10 14;");

                VBox wrapper;
                if (rang == 3) {
                    Label sep2 = new Label("  Autres participants");
                    sep2.setStyle("-fx-font-size:12;-fx-font-weight:bold;-fx-text-fill:#374151;");
                    VBox sepBox = new VBox(sep2);
                    sepBox.setStyle("-fx-background-color:rgba(255,255,255,0.7);-fx-background-radius:8;" +
                            "-fx-padding:8 16 4 16;");
                    wrapper = new VBox(6, sepBox, row);
                } else {
                    wrapper = new VBox(row);
                }
                wrapper.setStyle("-fx-padding:2 0;");
                setGraphic(wrapper);
                setStyle("-fx-background-color:transparent;-fx-padding:2 2;");
            }
        });
        listPerformances.setItems(javafx.collections.FXCollections.observableArrayList(resultats));
    }

    /**
     * Construit le podium spectaculaire avec photos, animations et détails cliquables.
     */
    private VBox construirePodium(
            edu.pompier.services.PerformanceIA.ResultatPerformance r1,
            edu.pompier.services.PerformanceIA.ResultatPerformance r2,
            edu.pompier.services.PerformanceIA.ResultatPerformance r3) {

        String nom1 = r1.prenom + " " + r1.nom;
        String nom2 = r2 != null ? r2.prenom + " " + r2.nom : "-";
        String nom3 = r3 != null ? r3.prenom + " " + r3.nom : "-";
        String sc1  = String.format("%.0f pts", r1.scoreTotal);
        String sc2  = r2 != null ? String.format("%.0f pts", r2.scoreTotal) : "";
        String sc3  = r3 != null ? String.format("%.0f pts", r3.scoreTotal) : "";

        // ── Colonnes podium spectaculaires ──
        VBox col2 = creerColonnePodium(r2, 2, "#4ade80", "#16a34a", 120, 95, false);
        VBox col1 = creerColonnePodium(r1, 1, "#fbbf24", "#d97706", 140, 130, true);
        VBox col3 = creerColonnePodium(r3, 3, "#94a3b8", "#64748b", 110, 70, false);

        HBox podiumRow = new HBox(12, col2, col1, col3);
        podiumRow.setAlignment(Pos.BOTTOM_CENTER);

        Label lblTitre = new Label("\uD83C\uDFC6  Classement des Pompiers");
        lblTitre.setStyle("-fx-font-size:16;-fx-font-weight:bold;-fx-text-fill:#1e293b;");

        Label lblExpl = new Label(r1.explication);
        lblExpl.setStyle("-fx-font-size:11;-fx-text-fill:#166534;-fx-wrap-text:true;" +
                "-fx-font-style:italic;-fx-background-color:#f0fdf4;" +
                "-fx-background-radius:8;-fx-padding:8 14;" +
                "-fx-border-color:#bbf7d0;-fx-border-radius:8;-fx-border-width:1;");
        lblExpl.setWrapText(true);

        Label lblHint = new Label("\uD83D\uDC46 Cliquez sur un pompier pour voir ses détails");
        lblHint.setStyle("-fx-font-size:10;-fx-text-fill:#94a3b8;-fx-font-style:italic;");

        VBox podiumBox = new VBox(10, lblTitre, podiumRow, lblHint, lblExpl);
        podiumBox.setAlignment(Pos.CENTER);
        podiumBox.setStyle(
                "-fx-background-color:linear-gradient(to bottom,rgba(255,255,255,0.97),rgba(240,253,244,0.97));" +
                        "-fx-background-radius:20;-fx-border-color:#fbbf24;" +
                        "-fx-border-radius:20;-fx-border-width:2;" +
                        "-fx-padding:20 24 16 24;" +
                        "-fx-effect:dropshadow(gaussian,rgba(251,191,36,0.4),24,0,0,8);");

        VBox container = new VBox(10, podiumBox);
        container.setStyle("-fx-padding:4 2 8 2;");
        return container;
    }

    /**
     * Crée une colonne de podium avec photo circulaire, nom, score, bloc coloré et clic détails.
     */
    private VBox creerColonnePodium(
            edu.pompier.services.PerformanceIA.ResultatPerformance r,
            int rang, String couleur, String couleurFonce,
            double largeur, double hauteurBloc, boolean estPremier) {

        if (r == null) { VBox vide = new VBox(); vide.setPrefWidth(largeur); return vide; }

        // ── Photo circulaire : vraie photo ou initiales ──
        double photoSize = largeur * 0.72;
        javafx.scene.layout.StackPane photoPane = new javafx.scene.layout.StackPane();
        photoPane.setMinWidth(photoSize); photoPane.setMinHeight(photoSize);
        photoPane.setMaxWidth(photoSize); photoPane.setMaxHeight(photoSize);

        boolean aPhoto = r.photoPath != null && !r.photoPath.isBlank();
        if (aPhoto) {
            try {
                javafx.scene.image.Image img = new javafx.scene.image.Image(
                        new java.io.File(r.photoPath).toURI().toString(),
                        photoSize, photoSize, true, true);
                javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
                iv.setFitWidth(photoSize); iv.setFitHeight(photoSize);
                javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(photoSize/2, photoSize/2, photoSize/2);
                iv.setClip(clip);
                // Bordure colorée
                javafx.scene.canvas.Canvas border = new javafx.scene.canvas.Canvas(photoSize, photoSize);
                javafx.scene.canvas.GraphicsContext gcB = border.getGraphicsContext2D();
                gcB.setStroke(javafx.scene.paint.Color.web(estPremier ? "#fbbf24" : couleur));
                gcB.setLineWidth(estPremier ? 3.5 : 2);
                gcB.strokeOval(1, 1, photoSize - 2, photoSize - 2);
                border.setMouseTransparent(true);
                photoPane.getChildren().addAll(iv, border);
            } catch (Exception ex) {
                aPhoto = false; // fallback initiales
            }
        }
        if (!aPhoto) {
            // Canvas initiales
            javafx.scene.canvas.Canvas photoCanvas = new javafx.scene.canvas.Canvas(photoSize, photoSize);
            javafx.scene.canvas.GraphicsContext gc = photoCanvas.getGraphicsContext2D();
            double cx = photoSize / 2, cy = photoSize / 2, rayon = photoSize * 0.42;
            gc.setFill(javafx.scene.paint.Color.web(couleur + "33"));
            gc.fillOval(cx - rayon - 5, cy - rayon - 5, (rayon + 5) * 2, (rayon + 5) * 2);
            if (estPremier) {
                gc.setStroke(javafx.scene.paint.Color.web("#fbbf24"));
                gc.setLineWidth(3.5);
                gc.strokeOval(cx - rayon - 2, cy - rayon - 2, (rayon + 2) * 2, (rayon + 2) * 2);
            }
            gc.setFill(javafx.scene.paint.Color.web(couleur));
            gc.fillOval(cx - rayon, cy - rayon, rayon * 2, rayon * 2);
            String initiales = r.nom.substring(0,1).toUpperCase() + r.prenom.substring(0,1).toUpperCase();
            gc.setFill(javafx.scene.paint.Color.WHITE);
            gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, rayon * 0.72));
            javafx.scene.text.Text t = new javafx.scene.text.Text(initiales);
            t.setFont(gc.getFont());
            gc.fillText(initiales, cx - t.getBoundsInLocal().getWidth() / 2, cy + t.getBoundsInLocal().getHeight() / 4);
            String medaille = switch (rang) { case 1 -> "\uD83C\uDFC6"; case 2 -> "\uD83E\uDD48"; default -> "\uD83E\uDD49"; };
            gc.setFont(javafx.scene.text.Font.font("Arial", rayon * 0.5));
            gc.fillText(medaille, cx + rayon * 0.42, cy - rayon * 0.42);
            photoPane.getChildren().add(photoCanvas);
        }

        // ── Nom + score ──
        Label lblNom = new Label(r.prenom + "\n" + r.nom.toUpperCase());
        lblNom.setStyle("-fx-font-size:" + (estPremier ? "12" : "11") + ";-fx-font-weight:bold;-fx-text-fill:#1e293b;-fx-alignment:center;");
        lblNom.setWrapText(true); lblNom.setMaxWidth(largeur); lblNom.setAlignment(Pos.CENTER);

        Label lblScore = new Label(String.format("%.0f pts", r.scoreTotal));
        lblScore.setStyle("-fx-font-size:" + (estPremier ? "13" : "11") + ";-fx-font-weight:bold;-fx-text-fill:" + couleurFonce + ";" +
                "-fx-background-color:white;-fx-background-radius:10;-fx-padding:2 10;" +
                "-fx-border-color:" + couleur + ";-fx-border-radius:10;-fx-border-width:1.5;");

        Label lblTitreR = new Label(r.titre != null && !r.titre.isEmpty() ? r.titre : "");
        lblTitreR.setStyle("-fx-font-size:9;-fx-text-fill:" + couleurFonce + ";-fx-font-weight:bold;");
        lblTitreR.setWrapText(true); lblTitreR.setMaxWidth(largeur); lblTitreR.setAlignment(Pos.CENTER);

        // ── Bloc podium ──
        Label lblRangNum = new Label(String.valueOf(rang));
        lblRangNum.setStyle("-fx-font-size:" + (estPremier ? "36" : "28") + ";-fx-font-weight:bold;-fx-text-fill:white;-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.3),4,0,0,2);");
        javafx.scene.layout.StackPane blocCorps = new javafx.scene.layout.StackPane(lblRangNum);
        blocCorps.setPrefWidth(largeur); blocCorps.setPrefHeight(hauteurBloc);
        blocCorps.setStyle("-fx-background-color:linear-gradient(to bottom," + couleur + "," + couleurFonce + ");-fx-background-radius:0 0 12 12;");
        javafx.scene.layout.HBox barreSup = new javafx.scene.layout.HBox();
        barreSup.setPrefHeight(6); barreSup.setPrefWidth(largeur);
        barreSup.setStyle("-fx-background-color:#fbbf24;-fx-background-radius:12 12 0 0;");
        VBox blocPodium = new VBox(0, barreSup, blocCorps);
        blocPodium.setPrefWidth(largeur);

        // ── Assemblage colonne ──
        VBox col = new VBox(4, photoPane, lblTitreR, lblNom, lblScore, blocPodium);
        col.setAlignment(Pos.BOTTOM_CENTER);
        col.setPrefWidth(largeur);

        // ── Clic → popup détails + guirlandes ──
        col.setOnMouseClicked(e -> afficherDetailsEtGuirlandes(r, rang, couleur));
        col.setStyle("-fx-cursor:hand;");
        col.setOnMouseEntered(ev -> col.setStyle("-fx-cursor:hand;-fx-effect:dropshadow(gaussian," + couleur + ",16,0,0,5);-fx-scale-x:1.03;-fx-scale-y:1.03;"));
        col.setOnMouseExited(ev -> col.setStyle("-fx-cursor:hand;-fx-scale-x:1.0;-fx-scale-y:1.0;"));
        return col;
    }

    /** Affiche un popup spectaculaire avec les détails du pompier + animation guirlandes. */
    private void afficherDetailsEtGuirlandes(
            edu.pompier.services.PerformanceIA.ResultatPerformance r,
            int rang, String couleur) {

        Stage popup = new Stage();
        popup.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        popup.setTitle("Détails — " + r.prenom + " " + r.nom);
        popup.setResizable(false);

        javafx.scene.canvas.Canvas canvasGuirlandes = new javafx.scene.canvas.Canvas(460, 560);
        javafx.scene.canvas.GraphicsContext gcG = canvasGuirlandes.getGraphicsContext2D();

        String couleurFonce = switch (rang) { case 1 -> "#d97706"; case 2 -> "#16a34a"; default -> "#475569"; };
        String gradient = switch (rang) {
            case 1 -> "linear-gradient(to right,#fbbf24,#f59e0b,#d97706)";
            case 2 -> "linear-gradient(to right,#4ade80,#22c55e,#16a34a)";
            default -> "linear-gradient(to right,#94a3b8,#64748b,#475569)";
        };
        String medaille = switch (rang) { case 1 -> "\uD83C\uDFC6"; case 2 -> "\uD83E\uDD48"; default -> "\uD83E\uDD49"; };

        Label lblMedaille = new Label(medaille);
        lblMedaille.setStyle("-fx-font-size:52;");

        // Photo grande dans le popup
        javafx.scene.layout.StackPane photoGrandePane = new javafx.scene.layout.StackPane();
        photoGrandePane.setMinWidth(110); photoGrandePane.setMinHeight(110);
        photoGrandePane.setMaxWidth(110); photoGrandePane.setMaxHeight(110);

        boolean aPhotoPopup = r.photoPath != null && !r.photoPath.isBlank();
        if (aPhotoPopup) {
            try {
                javafx.scene.image.Image img = new javafx.scene.image.Image(
                        new java.io.File(r.photoPath).toURI().toString(), 110, 110, true, true);
                javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
                iv.setFitWidth(110); iv.setFitHeight(110);
                javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(55, 55, 55);
                iv.setClip(clip);
                javafx.scene.canvas.Canvas borderC = new javafx.scene.canvas.Canvas(110, 110);
                javafx.scene.canvas.GraphicsContext gcBorder = borderC.getGraphicsContext2D();
                gcBorder.setStroke(javafx.scene.paint.Color.web("#fbbf24"));
                gcBorder.setLineWidth(rang == 1 ? 4 : 2);
                gcBorder.strokeOval(2, 2, 106, 106);
                borderC.setMouseTransparent(true);
                photoGrandePane.getChildren().addAll(iv, borderC);
            } catch (Exception ex) {
                aPhotoPopup = false;
            }
        }
        if (!aPhotoPopup) {
            javafx.scene.canvas.Canvas photoGrande = new javafx.scene.canvas.Canvas(110, 110);
            javafx.scene.canvas.GraphicsContext gcP = photoGrande.getGraphicsContext2D();
            double cx = 55, cy = 55, rayon = 48;
            gcP.setFill(javafx.scene.paint.Color.web(couleur + "55"));
            gcP.fillOval(cx - rayon - 5, cy - rayon - 5, (rayon + 5) * 2, (rayon + 5) * 2);
            gcP.setStroke(javafx.scene.paint.Color.web("#fbbf24"));
            gcP.setLineWidth(rang == 1 ? 4 : 2);
            gcP.strokeOval(cx - rayon - 2, cy - rayon - 2, (rayon + 2) * 2, (rayon + 2) * 2);
            gcP.setFill(javafx.scene.paint.Color.web(couleur));
            gcP.fillOval(cx - rayon, cy - rayon, rayon * 2, rayon * 2);
            String initiales = r.nom.substring(0,1).toUpperCase() + r.prenom.substring(0,1).toUpperCase();
            gcP.setFill(javafx.scene.paint.Color.WHITE);
            gcP.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 34));
            javafx.scene.text.Text tP = new javafx.scene.text.Text(initiales);
            tP.setFont(gcP.getFont());
            gcP.fillText(initiales, cx - tP.getBoundsInLocal().getWidth() / 2, cy + tP.getBoundsInLocal().getHeight() / 4);
            photoGrandePane.getChildren().add(photoGrande);
        }

        Label lblNomGrand = new Label(r.prenom + " " + r.nom.toUpperCase());
        lblNomGrand.setStyle("-fx-font-size:20;-fx-font-weight:bold;-fx-text-fill:white;-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.5),6,0,0,2);");
        Label lblTitreGrand = new Label(r.titre != null ? r.titre : "");
        lblTitreGrand.setStyle("-fx-font-size:13;-fx-text-fill:rgba(255,255,255,0.9);-fx-font-style:italic;");

        VBox headerContent = new VBox(6, lblMedaille, photoGrandePane, lblNomGrand, lblTitreGrand);
        headerContent.setAlignment(Pos.CENTER);
        headerContent.setStyle(gradient + ";-fx-padding:20 24 16 24;-fx-background-radius:16 16 0 0;");

        // Stats détaillées
        VBox statsBox = new VBox(8);
        statsBox.setStyle("-fx-padding:16 24;");
        statsBox.getChildren().addAll(
                ligneStatDetail("\uD83D\uDCCA Score total",       String.format("%.1f / 100 pts", r.scoreTotal),    couleurFonce),
                ligneStatDetail("\uD83D\uDD25 Missions traitées", r.nbMissions + " mission(s)",                     "#991b1b"),
                ligneStatDetail("\u2705 Taux de réussite",        r.tauxReussite > 0 ? Math.round(r.tauxReussite * 100) + "%" : "N/A", "#166534"),
                ligneStatDetail("\u26A1 Temps moyen",             r.tempsMoyenHeures > 0 ? String.format("%.1f h / intervention", r.tempsMoyenHeures) : "N/A", "#0369a1"),
                ligneStatDetail("\uD83D\uDEA8 Missions critiques",r.nbCritiques + " incendie(s) CRITIQUE",          "#7f1d1d"),
                ligneStatDetail("\uD83D\uDD50 Gardes honorées",   r.nbGardes + " garde(s)",                         "#d97706")
        );

        Label lblExpl = new Label(r.explication);
        lblExpl.setWrapText(true);
        lblExpl.setStyle("-fx-font-size:11;-fx-text-fill:#166534;-fx-font-style:italic;" +
                "-fx-background-color:#f0fdf4;-fx-background-radius:8;-fx-padding:10 14;" +
                "-fx-border-color:#bbf7d0;-fx-border-radius:8;-fx-border-width:1;");
        VBox explBox = new VBox(lblExpl);
        explBox.setStyle("-fx-padding:0 24 12 24;");

        Button btnFermer = new Button("Fermer");
        btnFermer.setStyle("-fx-background-color:" + couleur + ";-fx-text-fill:white;-fx-font-size:13;" +
                "-fx-font-weight:bold;-fx-background-radius:10;-fx-padding:8 30;-fx-cursor:hand;");
        btnFermer.setOnAction(e -> popup.close());
        HBox btnBox = new HBox(btnFermer);
        btnBox.setAlignment(Pos.CENTER);
        btnBox.setStyle("-fx-padding:0 0 18 0;");

        VBox corps = new VBox(0, headerContent, statsBox, explBox, btnBox);
        corps.setStyle("-fx-background-color:white;-fx-background-radius:16;");
        corps.setPrefWidth(460);

        javafx.scene.layout.StackPane root = new javafx.scene.layout.StackPane(corps, canvasGuirlandes);
        canvasGuirlandes.setMouseTransparent(true);

        Scene scene = new Scene(root, 460, 580);
        popup.setScene(scene);
        popup.show();

        if (rang == 1) lancerAnimationGuirlandes(gcG, canvasGuirlandes);
    }

    private HBox ligneStatDetail(String label, String valeur, String couleur) {
        Label lblLabel = new Label(label);
        lblLabel.setMinWidth(165);
        lblLabel.setStyle("-fx-font-size:12;-fx-text-fill:#64748b;-fx-font-weight:bold;");
        Label lblVal = new Label(valeur);
        lblVal.setStyle("-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:" + couleur + ";" +
                "-fx-background-color:" + couleur + "18;-fx-background-radius:6;-fx-padding:2 10;");
        HBox ligne = new HBox(10, lblLabel, lblVal);
        ligne.setAlignment(Pos.CENTER_LEFT);
        ligne.setStyle("-fx-background-color:#f8fafc;-fx-background-radius:8;-fx-padding:7 12;" +
                "-fx-border-color:#e2e8f0;-fx-border-radius:8;-fx-border-width:1;");
        return ligne;
    }

    private void lancerAnimationGuirlandes(javafx.scene.canvas.GraphicsContext gc,
                                            javafx.scene.canvas.Canvas canvas) {
        int NB = 70;
        double[][] p = new double[NB][8];
        java.util.Random rnd = new java.util.Random();
        String[] couleurs = {"#fbbf24","#f87171","#4ade80","#60a5fa","#c084fc","#fb923c","#34d399","#f472b6","#facc15","#a78bfa"};
        for (int i = 0; i < NB; i++) {
            p[i][0] = rnd.nextDouble() * canvas.getWidth();
            p[i][1] = -rnd.nextDouble() * canvas.getHeight() * 0.6;
            p[i][2] = (rnd.nextDouble() - 0.5) * 3.5;
            p[i][3] = rnd.nextDouble() * 4.5 + 1.5;
            p[i][4] = rnd.nextDouble() * 12 + 5;
            p[i][5] = rnd.nextInt(couleurs.length);
            p[i][6] = rnd.nextDouble() * 360;
            p[i][7] = (rnd.nextDouble() - 0.5) * 10;
        }
        javafx.animation.AnimationTimer timer = new javafx.animation.AnimationTimer() {
            int frame = 0;
            @Override
            public void handle(long now) {
                gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
                boolean tousHors = true;
                for (int i = 0; i < NB; i++) {
                    p[i][0] += p[i][2]; p[i][1] += p[i][3]; p[i][6] += p[i][7];
                    if (p[i][1] < canvas.getHeight() + 20) tousHors = false;
                    gc.save();
                    gc.translate(p[i][0], p[i][1]);
                    gc.rotate(p[i][6]);
                    gc.setFill(javafx.scene.paint.Color.web(couleurs[(int) p[i][5]]));
                    gc.setGlobalAlpha(Math.max(0, 1.0 - p[i][1] / canvas.getHeight()));
                    if (i % 3 == 0) gc.fillRect(-p[i][4]/2, -p[i][4]/4, p[i][4], p[i][4]/2);
                    else if (i % 3 == 1) gc.fillOval(-p[i][4]/2, -p[i][4]/2, p[i][4], p[i][4]);
                    else gc.fillRect(-p[i][4]/4, -p[i][4]/2, p[i][4]/2, p[i][4]);
                    gc.setGlobalAlpha(1.0);
                    gc.restore();
                }
                frame++;
                if (tousHors || frame > 350) stop();
            }
        };
        timer.start();
    }

    // ── Méthodes helper podium (conservées pour compatibilité) ──
    private VBox creerBlocPodium(String rang, String couleurPrincipale, String couleurBarre, double largeur, double hauteur) {
        // Barre du haut (couleur foncée)
        javafx.scene.layout.HBox barre = new javafx.scene.layout.HBox();
        barre.setPrefHeight(10);
        barre.setPrefWidth(largeur);
        barre.setStyle("-fx-background-color:" + couleurBarre + ";-fx-background-radius:8 8 0 0;");

        // Corps principal
        Label lblRang = new Label(rang);
        lblRang.setStyle("-fx-font-size:32;-fx-font-weight:bold;-fx-text-fill:white;");

        javafx.scene.layout.StackPane corps = new javafx.scene.layout.StackPane(lblRang);
        corps.setPrefWidth(largeur);
        corps.setPrefHeight(hauteur - 10);
        corps.setStyle("-fx-background-color:" + couleurPrincipale + ";-fx-background-radius:0 0 8 8;");

        VBox bloc = new VBox(0, barre, corps);
        bloc.setPrefWidth(largeur);
        bloc.setPrefHeight(hauteur);
        return bloc;
    }

    private VBox buildPodiumName(String nom, String score, String icon, String color, double width) {
        Label lblIcon = new Label(icon);
        lblIcon.setStyle("-fx-font-size:18;");
        lblIcon.setAlignment(Pos.CENTER);

        Label lblNom = new Label(nom);
        lblNom.setStyle("-fx-font-size:12;-fx-font-weight:bold;-fx-text-fill:#0f172a;");
        lblNom.setWrapText(true);
        lblNom.setMaxWidth(width);
        lblNom.setAlignment(Pos.CENTER);

        Label lblScore = new Label(score);
        lblScore.setStyle("-fx-font-size:10;-fx-font-weight:bold;-fx-text-fill:" + color + ";" +
                "-fx-background-color:#f0fdf4;-fx-background-radius:5;-fx-padding:1 6;");

        VBox box = new VBox(2, lblIcon, lblNom, lblScore);
        box.setAlignment(Pos.CENTER);
        box.setPrefWidth(width);
        box.setMaxWidth(width);
        box.setStyle("-fx-padding:0 4 6 4;");
        return box;
    }


    @FXML
    public void afficherVueChatbot() {
        if (vueDashboard != null)  { vueDashboard.setVisible(false);  vueDashboard.setManaged(false); }
        vuePompiers.setVisible(false);     vuePompiers.setManaged(false);
        vueGardes.setVisible(false);       vueGardes.setManaged(false);
        vueAffectations.setVisible(false); vueAffectations.setManaged(false);
        if (vueIA != null) { vueIA.setVisible(false); vueIA.setManaged(false); }
        vueChatbot.setVisible(true);       vueChatbot.setManaged(true);
        setHeader("Assistant IA", "Posez vos questions a l'assistant intelligent ForestGuard", false, true, true);
        setSidebarActif(btnPompiers);
        if (btnTabChatbot != null) btnTabChatbot.setStyle("-fx-background-color:#16a34a;-fx-text-fill:white;-fx-font-size:13;-fx-font-weight:bold;-fx-padding:8 20;-fx-cursor:hand;-fx-effect:dropshadow(gaussian,rgba(22,163,74,0.6),10,0,0,3);-fx-border-color:#15803d;-fx-border-width:1;-fx-background-radius:0 10 10 0;-fx-border-radius:0 10 10 0;");
        if (btnTabPompiers != null) btnTabPompiers.setStyle("-fx-background-color:rgba(255,255,255,0.10);-fx-text-fill:rgba(255,255,255,0.65);-fx-font-size:13;-fx-font-weight:bold;-fx-padding:8 20;-fx-cursor:hand;-fx-border-color:rgba(255,255,255,0.2);-fx-border-width:1;-fx-background-radius:10 0 0 10;-fx-border-radius:10 0 0 10;");
        if (btnTabGardes != null) btnTabGardes.setStyle("-fx-background-color:rgba(255,255,255,0.10);-fx-text-fill:rgba(255,255,255,0.65);-fx-font-size:13;-fx-font-weight:bold;-fx-padding:8 20;-fx-cursor:hand;-fx-border-color:rgba(255,255,255,0.2);-fx-border-width:1;-fx-background-radius:0;-fx-border-radius:0;");
        if (btnTabAffectations != null) btnTabAffectations.setStyle("-fx-background-color:rgba(255,255,255,0.10);-fx-text-fill:rgba(255,255,255,0.65);-fx-font-size:13;-fx-font-weight:bold;-fx-padding:8 20;-fx-cursor:hand;-fx-border-color:rgba(255,255,255,0.2);-fx-border-width:1;-fx-background-radius:0;-fx-border-radius:0;");
        if (btnTabIA != null) btnTabIA.setStyle("-fx-background-color:rgba(255,255,255,0.10);-fx-text-fill:rgba(255,255,255,0.65);-fx-font-size:13;-fx-font-weight:bold;-fx-padding:8 20;-fx-cursor:hand;-fx-border-color:rgba(255,255,255,0.2);-fx-border-width:1;-fx-background-radius:0;-fx-border-radius:0;");
        // Message d        // Message d'accueil si premiere ouverture
        if (vboxMessages != null && vboxMessages.getChildren().isEmpty()) {
            ajouterBulleBot(
                    "Bonjour ! Je suis l'assistant IA de ForestGuard.\n\n" +
                            "Vous pouvez me poser n'importe quelle question : sur vos pompiers, " +
                            "les incendies, les interventions, les performances... ou meme des questions " +
                            "generales sur la gestion des incendies, la securite forestiere, ou autre chose.\n\n" +
                            "Je suis la pour vous aider !"
            );
        }
        setBarreRechercheVisible(false);
    }

    @FXML
    public void envoyerMessageChat() {
        if (tfMessageChat == null || tfMessageChat.getText().isBlank()) return;
        String question = tfMessageChat.getText().trim();
        tfMessageChat.clear();

        // Bulle utilisateur
        ajouterBulleUtilisateur(question);

        // Bulle "en cours..."
        javafx.scene.control.Label lblTyping = new javafx.scene.control.Label("...");
        lblTyping.setStyle("-fx-background-color:#e2e8f0;-fx-background-radius:12 12 12 2;" +
                "-fx-padding:10 14;-fx-font-size:13;-fx-text-fill:#64748b;");
        javafx.scene.layout.HBox typingBox = new javafx.scene.layout.HBox(lblTyping);
        typingBox.setAlignment(Pos.CENTER_LEFT);
        typingBox.setStyle("-fx-padding:2 60 2 8;");
        if (vboxMessages != null) vboxMessages.getChildren().add(typingBox);
        scrollToBottom();

        // Appel API dans un thread séparé
        Thread t = new Thread(() -> {
            String reponse = chatbotService.envoyerMessage(question);
            javafx.application.Platform.runLater(() -> {
                if (vboxMessages != null) vboxMessages.getChildren().remove(typingBox);
                ajouterBulleBot(reponse);
            });
        });
        t.setDaemon(true);
        t.start();
    }

    /** Bulle utilisateur — droite, vert brillant */
    private void ajouterBulleUtilisateur(String texte) {
        if (vboxMessages == null) return;
        javafx.scene.control.Label lbl = new javafx.scene.control.Label(texte);
        lbl.setWrapText(true);
        lbl.setMaxWidth(380);
        lbl.setStyle(
                "-fx-background-color:linear-gradient(to bottom right,#16a34a,#15803d);" +
                        "-fx-text-fill:white;" +
                        "-fx-background-radius:18 18 4 18;" +
                        "-fx-padding:11 16;" +
                        "-fx-font-size:13;" +
                        "-fx-effect:dropshadow(gaussian,rgba(22,163,74,0.5),10,0,0,3);" +
                        "-fx-border-color:rgba(134,239,172,0.4);-fx-border-radius:18 18 4 18;-fx-border-width:1;");
        javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(lbl);
        box.setAlignment(Pos.CENTER_RIGHT);
        box.setStyle("-fx-padding:2 4 2 80;");
        vboxMessages.getChildren().add(box);
        scrollToBottom();
    }

    /** Bulle bot — gauche, sombre translucide avec bordure verte */
    private void ajouterBulleBot(String texte) {
        if (vboxMessages == null) return;
        // Avatar feuille
        javafx.scene.layout.StackPane avatarBox = new javafx.scene.layout.StackPane();
        avatarBox.setMinWidth(36); avatarBox.setMinHeight(36);
        avatarBox.setMaxWidth(36); avatarBox.setMaxHeight(36);
        avatarBox.setStyle(
                "-fx-background-color:linear-gradient(to bottom,#16a34a,#14532d);" +
                        "-fx-background-radius:50;" +
                        "-fx-effect:dropshadow(gaussian,rgba(74,222,128,0.6),8,0,0,2);" +
                        "-fx-border-color:rgba(74,222,128,0.5);-fx-border-radius:50;-fx-border-width:1.5;");
        javafx.scene.control.Label iconLbl = new javafx.scene.control.Label("FG");
        iconLbl.setStyle("-fx-font-size:11;-fx-font-weight:bold;-fx-text-fill:#bbf7d0;");
        avatarBox.getChildren().add(iconLbl);

        javafx.scene.control.Label lbl = new javafx.scene.control.Label(texte);
        lbl.setWrapText(true);
        lbl.setMaxWidth(440);
        lbl.setStyle(
                "-fx-background-color:rgba(20,83,45,0.7);" +
                        "-fx-text-fill:#dcfce7;" +
                        "-fx-background-radius:4 18 18 18;" +
                        "-fx-padding:12 16;" +
                        "-fx-font-size:13;" +
                        "-fx-border-color:rgba(74,222,128,0.3);" +
                        "-fx-border-radius:4 18 18 18;" +
                        "-fx-border-width:1;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.3),8,0,0,2);");
        javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(10, avatarBox, lbl);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle("-fx-padding:2 80 2 4;");
        vboxMessages.getChildren().add(box);
        scrollToBottom();
    }

    private void scrollToBottom() {
        if (scrollChat != null) {
            javafx.application.Platform.runLater(() -> scrollChat.setVvalue(1.0));
        }
    }

    @FXML public void envoyerSuggestion1() { envoyerSuggestion("Qui sont les pompiers disponibles maintenant ?"); }
    @FXML public void envoyerSuggestion2() { envoyerSuggestion("Qui est le meilleur pompier du mois ?"); }
    @FXML public void envoyerSuggestion3() { envoyerSuggestion("Quelles sont les alertes recentes ?"); }

    private void envoyerSuggestion(String texte) {
        if (tfMessageChat != null) {
            tfMessageChat.setText(texte);
            envoyerMessageChat();
        }
    }

    @FXML public void ouvrirGestionGardes() { afficherVueGardes(); }

    // ══════════════════════════════════════════
    //  EXPORT PDF
    // ══════════════════════════════════════════

    @FXML
    public void ouvrirMenuExport() {
        // ── Dialogue de choix du type d'export ──
        Stage dialog = new Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("Export PDF");
        dialog.setResizable(false);

        // En-tête rouge
        Label icone = new Label("\uD83D\uDCC4");
        icone.setStyle("-fx-font-size: 28; -fx-text-fill: white;");
        Label titre = new Label("Export PDF");
        titre.setStyle("-fx-font-size: 17; -fx-font-weight: bold; -fx-text-fill: white;");
        Label sousTitre = new Label("Choisissez le type de rapport a generer");
        sousTitre.setStyle("-fx-font-size: 11; -fx-text-fill: rgba(255,255,255,0.75);");
        VBox texteHeader = new VBox(3, titre, sousTitre);
        texteHeader.setAlignment(Pos.CENTER_LEFT);
        HBox header = new HBox(14, icone, texteHeader);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: linear-gradient(to right,#b91c1c,#dc2626);-fx-padding:18 24;-fx-background-radius:16 16 0 0;");

        // Boutons d'export
        Button btn1 = creerBoutonExport("\uD83D\uDC64  Fiche individuelle d'un pompier",
                "Nom, email, statut, gardes, score IA...", "#1e40af", "#dbeafe");
        Button btn2 = creerBoutonExport("\uD83D\uDCCB  Rapport complet de tous les pompiers",
                "Liste detaillee avec stats globales", "#166534", "#dcfce7");
        Button btn3 = creerBoutonExport("\uD83D\uDD50  Rapport gardes & affectations",
                "Historique des gardes et affectations", "#92400e", "#fef3c7");
        Button btn4 = creerBoutonExport("\uD83D\uDCCA  Tableau de bord imprimable",
                "Stats globales, top IA, certifications...", "#7c3aed", "#ede9fe");

        btn1.setOnAction(e -> { dialog.close(); exportFicheIndividuelle(); });
        btn2.setOnAction(e -> { dialog.close(); exportRapportComplet(); });
        btn3.setOnAction(e -> { dialog.close(); exportRapportGardesAffectations(); });
        btn4.setOnAction(e -> { dialog.close(); exportTableauDeBord(); });

        Button btnAnnuler = new Button("Annuler");
        btnAnnuler.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #374151; -fx-font-size: 12; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 20; -fx-cursor: hand;");
        btnAnnuler.setOnAction(e -> dialog.close());
        HBox btnBox = new HBox(btnAnnuler);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        btnBox.setStyle("-fx-padding: 8 24 16 24;");

        VBox corps = new VBox(10, btn1, btn2, btn3, btn4, btnBox);
        corps.setStyle("-fx-padding: 20 24 0 24;");

        VBox root = new VBox(0, header, corps);
        root.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-effect: dropshadow(gaussian,rgba(0,0,0,0.3),20,0,0,6);");
        root.setPrefWidth(460);

        dialog.setScene(new javafx.scene.Scene(root));
        dialog.show();
    }

    private Button creerBoutonExport(String titre, String description, String couleur, String bgClair) {
        Label lblTitre = new Label(titre);
        lblTitre.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: " + couleur + ";");
        Label lblDesc = new Label(description);
        lblDesc.setStyle("-fx-font-size: 10; -fx-text-fill: #64748b;");
        VBox content = new VBox(2, lblTitre, lblDesc);
        content.setAlignment(Pos.CENTER_LEFT);
        Button btn = new Button();
        btn.setGraphic(content);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle("-fx-background-color: " + bgClair + "; -fx-background-radius: 10; -fx-border-color: " + couleur + "44; -fx-border-radius: 10; -fx-border-width: 1; -fx-padding: 10 14; -fx-cursor: hand; -fx-alignment: CENTER_LEFT;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: " + bgClair.replace("e", "c") + "; -fx-background-radius: 10; -fx-border-color: " + couleur + "; -fx-border-radius: 10; -fx-border-width: 1.5; -fx-padding: 10 14; -fx-cursor: hand; -fx-alignment: CENTER_LEFT;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: " + bgClair + "; -fx-background-radius: 10; -fx-border-color: " + couleur + "44; -fx-border-radius: 10; -fx-border-width: 1; -fx-padding: 10 14; -fx-cursor: hand; -fx-alignment: CENTER_LEFT;"));
        return btn;
    }

    private void exportFicheIndividuelle() {
        if (listeTotale.isEmpty()) { afficherAlertePdf("Aucun pompier disponible."); return; }

        // ── Dialogue personnalisé stylisé ──
        Stage dialog = new Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("Fiche individuelle");
        dialog.setResizable(false);

        // En-tête bleu
        Label icone = new Label("\uD83D\uDC64");
        icone.setStyle("-fx-font-size: 26; -fx-text-fill: white;");
        Label titre = new Label("Fiche individuelle");
        titre.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: white;");
        Label sousTitre = new Label("Selectionnez le pompier a exporter");
        sousTitre.setStyle("-fx-font-size: 11; -fx-text-fill: rgba(255,255,255,0.75);");
        VBox texteHeader = new VBox(3, titre, sousTitre);
        texteHeader.setAlignment(Pos.CENTER_LEFT);
        HBox header = new HBox(14, icone, texteHeader);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: linear-gradient(to right,#166534,#16a34a);"
                + "-fx-padding: 18 24; -fx-background-radius: 16 16 0 0;");

        // ── Liste des pompiers avec avatar ──
        javafx.scene.control.ListView<Pompier> listChoix = new javafx.scene.control.ListView<>();
        listChoix.setPrefHeight(220);
        listChoix.setStyle("-fx-background-color: transparent; -fx-border-color: #e2e8f0; -fx-border-radius: 10; -fx-background-radius: 10;");

        listChoix.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Pompier p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) { setGraphic(null); setStyle("-fx-background-color:transparent;"); return; }

                String initiales = p.getNom().substring(0,1).toUpperCase() + p.getPrenom().substring(0,1).toUpperCase();
                Label avatar = new Label(initiales);
                avatar.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white; -fx-font-weight: bold;"
                        + "-fx-font-size: 12; -fx-min-width: 36; -fx-min-height: 36;"
                        + "-fx-background-radius: 50; -fx-alignment: center;");

                Label lblNom = new Label(p.getNom() + " " + p.getPrenom());
                lblNom.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
                Label lblEmail = new Label(p.getEmail() != null ? p.getEmail() : "");
                lblEmail.setStyle("-fx-font-size: 10; -fx-text-fill: #64748b;");

                String couleur = "disponible".equals(p.getStatut()) ? "#16a34a"
                        : "en_mission".equals(p.getStatut()) ? "#E65100" : "#757575";
                Label lblStatut = new Label(p.getStatut() != null ? p.getStatut().replace("_"," ").toUpperCase() : "");
                lblStatut.setStyle("-fx-background-color:" + couleur + "; -fx-text-fill: white;"
                        + "-fx-background-radius: 8; -fx-padding: 2 8; -fx-font-size: 9; -fx-font-weight: bold;");

                VBox infos = new VBox(2, lblNom, lblEmail);
                infos.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(infos, Priority.ALWAYS);

                HBox row = new HBox(10, avatar, infos, lblStatut);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-padding: 8 12;");

                String bgNormal = getIndex() % 2 == 0 ? "white" : "#f8fafc";
                setStyle("-fx-background-color:" + bgNormal + "; -fx-border-color: #f1f5f9; -fx-border-width: 0 0 1 0;");
                setOnMouseEntered(e -> setStyle("-fx-background-color: #f0fdf4; -fx-border-color: #86efac; -fx-border-width: 0 0 1 0;"));
                setOnMouseExited(e -> setStyle("-fx-background-color:" + bgNormal + "; -fx-border-color: #f1f5f9; -fx-border-width: 0 0 1 0;"));
                setGraphic(row);
            }
        });

        listChoix.setItems(listeTotale);
        listChoix.getSelectionModel().selectFirst();

        // ── Barre de recherche ──
        javafx.scene.control.TextField tfSearch = new javafx.scene.control.TextField();
        tfSearch.setPromptText("\uD83D\uDD0D  Rechercher un pompier...");
        tfSearch.setStyle("-fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #e2e8f0;"
                + "-fx-padding: 8 12; -fx-font-size: 12; -fx-background-color: #f8fafc;");
        tfSearch.textProperty().addListener((obs, o, n) -> {
            if (n == null || n.isBlank()) {
                listChoix.setItems(listeTotale);
            } else {
                String q = n.toLowerCase();
                javafx.collections.ObservableList<Pompier> filtres = javafx.collections.FXCollections.observableArrayList(
                        listeTotale.stream().filter(p ->
                                p.getNom().toLowerCase().contains(q) ||
                                p.getPrenom().toLowerCase().contains(q) ||
                                (p.getEmail() != null && p.getEmail().toLowerCase().contains(q))
                        ).toList()
                );
                listChoix.setItems(filtres);
            }
        });

        // ── Boutons ──
        Button btnExporter = new Button("\uD83D\uDCC4  Exporter PDF");
        btnExporter.setPrefHeight(40);
        btnExporter.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white; -fx-font-size: 13;"
                + "-fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand;"
                + "-fx-effect: dropshadow(gaussian,rgba(22,163,74,0.5),10,0,0,3);");
        btnExporter.setOnMouseEntered(e -> btnExporter.setStyle("-fx-background-color: #15803d; -fx-text-fill: white; -fx-font-size: 13;"
                + "-fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand;"));
        btnExporter.setOnMouseExited(e -> btnExporter.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white; -fx-font-size: 13;"
                + "-fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand;"
                + "-fx-effect: dropshadow(gaussian,rgba(22,163,74,0.5),10,0,0,3);"));

        Button btnAnnuler = new Button("\u2715  Annuler");
        btnAnnuler.setPrefHeight(40);
        btnAnnuler.setStyle("-fx-background-color: white; -fx-text-fill: #dc2626; -fx-font-size: 13;"
                + "-fx-font-weight: bold; -fx-background-radius: 10; -fx-border-color: #fca5a5;"
                + "-fx-border-radius: 10; -fx-border-width: 1.5; -fx-cursor: hand;");
        btnAnnuler.setOnAction(e -> dialog.close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox boutons = new HBox(10, spacer, btnAnnuler, btnExporter);
        boutons.setAlignment(Pos.CENTER_RIGHT);
        boutons.setStyle("-fx-padding: 12 0 0 0; -fx-border-color: #e5e7eb; -fx-border-width: 1 0 0 0;");

        btnExporter.setOnAction(e -> {
            Pompier selectionne = listChoix.getSelectionModel().getSelectedItem();
            if (selectionne == null) { afficherAlertePdf("Veuillez selectionner un pompier."); return; }
            dialog.close();
            javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
            fc.setTitle("Enregistrer la fiche PDF");
            fc.setInitialFileName("fiche_" + selectionne.getNom() + "_" + selectionne.getPrenom() + ".pdf");
            fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF", "*.pdf"));
            java.io.File f = fc.showSaveDialog(listView.getScene().getWindow());
            if (f != null) {
                String res = edu.pompier.tools.PdfExportService.exportFicheIndividuelle(selectionne, f.getAbsolutePath());
                if (res != null) afficherSuccesPdf(f.getAbsolutePath());
                else afficherAlertePdf("Erreur lors de la generation du PDF.");
            }
        });

        // ── Assemblage ──
        VBox corps = new VBox(10, tfSearch, listChoix, boutons);
        corps.setStyle("-fx-padding: 16 24 20 24;");

        VBox root = new VBox(0, header, corps);
        root.setStyle("-fx-background-color: white; -fx-background-radius: 16;"
                + "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.3),20,0,0,6);");
        root.setPrefWidth(460);

        dialog.setScene(new javafx.scene.Scene(root));
        dialog.show();
    }

    private void exportRapportComplet() {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Enregistrer le rapport PDF");
        fc.setInitialFileName("rapport_pompiers.pdf");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF", "*.pdf"));
        java.io.File f = fc.showSaveDialog(listView.getScene().getWindow());
        if (f != null) {
            String res = edu.pompier.tools.PdfExportService.exportRapportComplet(f.getAbsolutePath());
            if (res != null) afficherSuccesPdf(f.getAbsolutePath());
            else afficherAlertePdf("Erreur lors de la generation du PDF.");
        }
    }

    private void exportRapportGardesAffectations() {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Enregistrer le rapport PDF");
        fc.setInitialFileName("rapport_gardes_affectations.pdf");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF", "*.pdf"));
        java.io.File f = fc.showSaveDialog(listView.getScene().getWindow());
        if (f != null) {
            String res = edu.pompier.tools.PdfExportService.exportRapportGardesAffectations(f.getAbsolutePath());
            if (res != null) afficherSuccesPdf(f.getAbsolutePath());
            else afficherAlertePdf("Erreur lors de la generation du PDF.");
        }
    }

    private void exportTableauDeBord() {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Enregistrer le tableau de bord PDF");
        fc.setInitialFileName("tableau_de_bord.pdf");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF", "*.pdf"));
        java.io.File f = fc.showSaveDialog(listView.getScene().getWindow());
        if (f != null) {
            String res = edu.pompier.tools.PdfExportService.exportTableauDeBord(f.getAbsolutePath());
            if (res != null) afficherSuccesPdf(f.getAbsolutePath());
            else afficherAlertePdf("Erreur lors de la generation du PDF.");
        }
    }

    private void afficherSuccesPdf(String chemin) {
        Alert ok = new Alert(Alert.AlertType.INFORMATION);
        ok.setTitle("PDF genere");
        ok.setHeaderText(null);
        ok.setContentText("\u2705 PDF genere avec succes !\n\nFichier : " + chemin);
        ok.showAndWait();
    }

    private void afficherAlertePdf(String msg) {
        Alert err = new Alert(Alert.AlertType.WARNING);
        err.setTitle("Export PDF");
        err.setHeaderText(null);
        err.setContentText(msg);
        err.showAndWait();
    }

    // ══════════════════════════════════════════
    //  FORMULAIRE GARDE
    // ══════════════════════════════════════════

    /** Affiche les heures correspondant au créneau sélectionné */
    private void mettreAJourAffichageHoraire(String creneau) {
        if (lblHoraireCreneau == null) return;
        String horaire = switch (creneau != null ? creneau : "") {
            case "matin"      -> "06:00 → 12:00  (6 heures)";
            case "apres-midi" -> "12:00 → 18:00  (6 heures)";
            case "nuit"       -> "18:00 → 06:00  (12 heures)";
            case "24h"        -> "00:00 → 00:00  (24 heures)";
            default           -> "Selectionnez un creneau";
        };
        lblHoraireCreneau.setText(horaire);
        if (hboxHoraireCreneau != null) {
            String couleur = switch (creneau != null ? creneau : "") {
                case "matin"      -> "-fx-background-color:#fef9c3;-fx-border-color:#fde68a;";
                case "apres-midi" -> "-fx-background-color:#dbeafe;-fx-border-color:#93c5fd;";
                case "nuit"       -> "-fx-background-color:#ede9fe;-fx-border-color:#c4b5fd;";
                case "24h"        -> "-fx-background-color:#dcfce7;-fx-border-color:#86efac;";
                default           -> "-fx-background-color:#f0fdf4;-fx-border-color:#86efac;";
            };
            hboxHoraireCreneau.setStyle(couleur
                    + "-fx-background-radius:8;-fx-border-radius:8;-fx-border-width:1;-fx-padding:8 12;");
        }
    }

    @FXML
    public void sauvegarderGarde() {
        lblErreurGarde.setText("");

        // ── Validation pompier ──
        if (pompierSelectionne == null) {
            afficherErreurGarde("Veuillez selectionner un pompier dans la liste de gauche.");
            return;
        }

        // ── Validation créneau ──
        if (cbCreneau.getValue() == null) {
            afficherErreurGarde("Veuillez choisir un creneau (matin, apres-midi, nuit ou 24h).");
            return;
        }

        // ── Validation dates ──
        if (dpDateDebut.getValue() == null || dpDateFin.getValue() == null) {
            afficherErreurGarde("Veuillez choisir une date de debut et de fin.");
            return;
        }

        LocalDate debut = dpDateDebut.getValue();
        LocalDate fin   = dpDateFin.getValue();

        // Bloquer les dates passées
        if (debut.isBefore(LocalDate.now())) {
            afficherErreurGarde("La date de debut ne peut pas etre dans le passe.");
            dpDateDebut.setValue(LocalDate.now());
            return;
        }
        if (fin.isBefore(debut)) {
            afficherErreurGarde("La date de fin doit etre apres ou egale a la date de debut.");
            dpDateFin.setValue(debut);
            return;
        }

        // ── Contrôle horaire si date = aujourd'hui ──
        String creneau = cbCreneau.getValue();
        if (debut.equals(LocalDate.now())) {
            int heureActuelle = java.time.LocalTime.now().getHour();
            boolean creneauPasse = switch (creneau) {
                case "matin"      -> heureActuelle >= 12; // matin = 06h-12h
                case "apres-midi" -> heureActuelle >= 18; // après-midi = 12h-18h
                default           -> false; // nuit et 24h toujours valides
            };
            if (creneauPasse) {
                String heuresFin = switch (creneau) {
                    case "matin"      -> "12:00";
                    case "apres-midi" -> "18:00";
                    default           -> "";
                };
                afficherErreurGarde("Le creneau \"" + creneau + "\" (jusqu'a " + heuresFin
                        + ") est deja passe. Il est actuellement "
                        + java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) + ".");
                return;
            }
        }

        String dateDebut = debut.toString();
        String dateFin   = fin.toString();

        // ── Vérifier conflit ──
        if (service.gardeConflitExiste(pompierSelectionne.getId(), creneau, dateDebut, dateFin, gardeIdEnEdition)) {
            afficherErreurGarde("Ce pompier a deja une garde \"" + creneau + "\" sur cette periode. Conflit detecte.");
            return;
        }

        // ── Sauvegarder ──
        if (gardeIdEnEdition == -1) {
            service.addGardePeriode(pompierSelectionne.getId(), creneau, dateDebut, dateFin);
        } else {
            service.deleteGardesByPompierCreneau(pompierSelectionne.getId(), creneau);
            service.addGardePeriode(pompierSelectionne.getId(), creneau, dateDebut, dateFin);
        }

        // ── Réinitialiser et mettre à jour ──
        annulerEdition();
        chargerGardesListe();  // met à jour la liste des gardes
        chargerListe();        // met à jour les stats
    }

    private void afficherErreurGarde(String msg) {
        if (lblErreurGarde != null) {
            lblErreurGarde.setText("⚠ " + msg);
            lblErreurGarde.setStyle("-fx-text-fill: #C62828; -fx-font-size: 11; -fx-font-weight: bold;"
                    + "-fx-background-color: #FFEBEE; -fx-background-radius: 6; -fx-padding: 6 10;");
        }
    }

    @FXML
    public void annulerEdition() {
        gardeIdEnEdition = -1;
        pompierSelectionne = null;
        listPompiers.getSelectionModel().clearSelection();
        cbCreneau.setValue(null);
        dpDateDebut.setValue(LocalDate.now());
        dpDateFin.setValue(LocalDate.now());
        lblTitreForm.setText("Nouvelle Garde");
        lblPompierSelectionne.setText("Aucun pompier selectionne");
        btnSauvegarder.setText("Enregistrer");
        lblErreurGarde.setText("");
        mettreAJourAffichageHoraire(null);
    }

    // ══════════════════════════════════════════
    //  AFFECTATION AUTOMATIQUE
    // ══════════════════════════════════════════

    /** Charge les alertes non encore affectées - met à jour le badge uniquement */
    private void chargerAlertesNonAffectees() {
        alertesDisponibles = service.getAlertesNonAffectees();
        if (lblStatutAffectation == null) return;
        if (alertesDisponibles.isEmpty()) {
            lblStatutAffectation.setText("Aucune alerte en attente");
            lblStatutAffectation.setStyle("-fx-background-color:#e2e8f0;-fx-text-fill:#64748b;-fx-background-radius:8;-fx-padding:4 10;-fx-font-weight:bold;");
        } else {
            lblStatutAffectation.setText(alertesDisponibles.size() + " alerte(s) en attente");
            lblStatutAffectation.setStyle("-fx-background-color:#fee2e2;-fx-text-fill:#dc2626;-fx-background-radius:8;-fx-padding:4 10;-fx-font-weight:bold;");
        }
    }

    /**
     * Ouvre un dialogue pour affecter manuellement un pompier à une alerte existante.
     * Appelé par le bouton "+ Affecter un autre pompier" dans chaque ligne du tableau.
     */
    private void ouvrirDialogueAffectationManuelle(String idAlerte, String nomLieu, String localisation) {
        List<edu.pompier.entities.Pompier> tous = service.getPompiersDisponiblesPourAffectation();
        if (tous.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Aucun pompier disponible");
            alert.setContentText("Tous les pompiers sont actuellement en mission ou inactifs.");
            alert.showAndWait();
            return;
        }

        // ── Tri : même région en premier, puis par niveau décroissant ──
        java.util.List<edu.pompier.entities.Pompier> memeRegion = new java.util.ArrayList<>();
        java.util.List<edu.pompier.entities.Pompier> autreRegion = new java.util.ArrayList<>();
        // Extraire le gouvernorat depuis le nomLieu (ex: "Mateur, Bizerte" → "Bizerte")
        String gouvernorat = nomLieu.contains(",") ? nomLieu.split(",")[1].trim().toLowerCase() : nomLieu.toLowerCase();
        for (edu.pompier.entities.Pompier p : tous) {
            String villeP = p.getVille() != null ? p.getVille().toLowerCase() : "";
            String foretP = p.getNomForet() != null ? p.getNomForet().toLowerCase() : "";
            if (villeP.contains(gouvernorat) || foretP.contains(gouvernorat)
                    || gouvernorat.contains(villeP) && !villeP.isEmpty())
                memeRegion.add(p);
            else
                autreRegion.add(p);
        }
        // Tri par niveau dans chaque groupe
        java.util.Comparator<edu.pompier.entities.Pompier> parNiveau = (a, b) -> {
            int nA = niveauScore(a.getNiveauCertification());
            int nB = niveauScore(b.getNiveauCertification());
            return Integer.compare(nB, nA); // décroissant
        };
        memeRegion.sort(parNiveau);
        autreRegion.sort(parNiveau);

        java.util.List<edu.pompier.entities.Pompier> ordonnes = new java.util.ArrayList<>(memeRegion);
        ordonnes.addAll(autreRegion);

        // ── Dialogue stylisé ──
        Dialog<edu.pompier.entities.Pompier> dialog = new Dialog<>();
        dialog.setTitle("Affecter un pompier supplémentaire");

        ButtonType btnAffecter = new ButtonType("✔  Affecter", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnAffecter, ButtonType.CANCEL);

        // En-tête custom
        VBox header = new VBox(4);
        header.setStyle("-fx-background-color:linear-gradient(to right,#dc2626,#991b1b);-fx-padding:16 20;");
        Label hTitre = new Label("🚒  Affecter un pompier supplémentaire");
        hTitre.setStyle("-fx-font-size:15;-fx-font-weight:bold;-fx-text-fill:white;");
        Label hSub   = new Label("🔥  Alerte : " + nomLieu);
        hSub.setStyle("-fx-font-size:12;-fx-text-fill:rgba(255,255,255,0.85);");
        header.getChildren().addAll(hTitre, hSub);
        dialog.getDialogPane().setHeader(header);

        // Barre de recherche
        javafx.scene.control.TextField tfSearch = new javafx.scene.control.TextField();
        tfSearch.setPromptText("🔍  Rechercher un pompier...");
        tfSearch.setStyle("-fx-padding:9 14;-fx-font-size:12;-fx-background-radius:10;-fx-border-radius:10;" +
                "-fx-border-color:#e2e8f0;-fx-background-color:#f8fafc;");

        // Séparateur région
        Label lblSepRegion = new Label("  📍 Même région — prioritaires");
        lblSepRegion.setStyle("-fx-font-size:10;-fx-font-weight:bold;-fx-text-fill:#dc2626;" +
                "-fx-background-color:#fee2e2;-fx-background-radius:6;-fx-padding:4 12;");
        Label lblSepAutre  = new Label("  🗺 Autres régions");
        lblSepAutre.setStyle("-fx-font-size:10;-fx-font-weight:bold;-fx-text-fill:#64748b;" +
                "-fx-background-color:#f1f5f9;-fx-background-radius:6;-fx-padding:4 12;");
        lblSepAutre.setVisible(!autreRegion.isEmpty());
        lblSepAutre.setManaged(!autreRegion.isEmpty());

        // ListView
        ListView<Object> listChoix = new ListView<>(); // Object = Pompier | String (séparateur)
        listChoix.setPrefSize(480, 300);
        java.util.function.Supplier<Void> remplirListe = () -> {
            listChoix.getItems().clear();
            String q = tfSearch.getText().toLowerCase().trim();
            java.util.List<edu.pompier.entities.Pompier> filtreRegion = memeRegion.stream()
                    .filter(p -> q.isBlank() || (p.getNom()+" "+p.getPrenom()).toLowerCase().contains(q))
                    .toList();
            java.util.List<edu.pompier.entities.Pompier> filtreAutre = autreRegion.stream()
                    .filter(p -> q.isBlank() || (p.getNom()+" "+p.getPrenom()).toLowerCase().contains(q))
                    .toList();
            if (!filtreRegion.isEmpty()) {
                listChoix.getItems().add("SEP_REGION");
                listChoix.getItems().addAll(filtreRegion);
            }
            if (!filtreAutre.isEmpty()) {
                listChoix.getItems().add("SEP_AUTRE");
                listChoix.getItems().addAll(filtreAutre);
            }
            return null;
        };
        remplirListe.get();
        tfSearch.textProperty().addListener((obs, o, n) -> remplirListe.get());

        listChoix.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(null); setText(null);
                if (empty || item == null) { setStyle("-fx-background-color:transparent;"); return; }

                // Séparateurs
                if ("SEP_REGION".equals(item)) {
                    Label sep = new Label("  📍  Même région — prioritaires");
                    sep.setStyle("-fx-font-size:10;-fx-font-weight:bold;-fx-text-fill:#991b1b;");
                    HBox box = new HBox(sep);
                    box.setStyle("-fx-background-color:#fee2e2;-fx-padding:5 12;");
                    setGraphic(box);
                    setStyle("-fx-background-color:transparent;-fx-padding:0;");
                    setMouseTransparent(true);
                    return;
                }
                if ("SEP_AUTRE".equals(item)) {
                    Label sep = new Label("  🗺  Autres régions");
                    sep.setStyle("-fx-font-size:10;-fx-font-weight:bold;-fx-text-fill:#475569;");
                    HBox box = new HBox(sep);
                    box.setStyle("-fx-background-color:#f1f5f9;-fx-padding:5 12;");
                    setGraphic(box);
                    setStyle("-fx-background-color:transparent;-fx-padding:0;");
                    setMouseTransparent(true);
                    return;
                }

                // Carte pompier
                edu.pompier.entities.Pompier p = (edu.pompier.entities.Pompier) item;
                boolean estRegion = memeRegion.contains(p);
                String certif = p.getNiveauCertification() != null ? p.getNiveauCertification().toUpperCase() : "N/A";
                String loc    = p.getVille() != null ? p.getVille() + (p.getZoneAdresse() != null ? " — " + p.getZoneAdresse() : "") : "Localisation inconnue";

                // Avatar initiales
                String init = p.getNom().substring(0,1).toUpperCase() + p.getPrenom().substring(0,1).toUpperCase();
                Label avatar = new Label(init);
                avatar.setStyle("-fx-background-color:" + (estRegion ? "#dc2626" : "#475569") + ";-fx-text-fill:white;" +
                        "-fx-font-weight:bold;-fx-font-size:13;-fx-min-width:36;-fx-min-height:36;" +
                        "-fx-background-radius:50;-fx-alignment:center;");

                Label lblNom  = new Label(p.getNom() + " " + p.getPrenom());
                lblNom.setStyle("-fx-font-weight:bold;-fx-font-size:13;-fx-text-fill:#0f172a;");

                // Badge certification
                String certifBg = switch (certif) {
                    case "EXPERT" -> "#fef9c3"; case "AVANCE" -> "#dcfce7";
                    case "INTERMEDIAIRE" -> "#dbeafe"; default -> "#f1f5f9";
                };
                String certifFg = switch (certif) {
                    case "EXPERT" -> "#854d0e"; case "AVANCE" -> "#166534";
                    case "INTERMEDIAIRE" -> "#1d4ed8"; default -> "#64748b";
                };
                Label lblCertif = new Label(certif);
                lblCertif.setStyle("-fx-background-color:"+certifBg+";-fx-text-fill:"+certifFg+
                        ";-fx-background-radius:6;-fx-padding:2 8;-fx-font-size:10;-fx-font-weight:bold;");

                Label lblLoc = new Label("📍 " + loc);
                lblLoc.setStyle("-fx-font-size:11;-fx-text-fill:#64748b;");

                if (estRegion) {
                    Label badge = new Label("Même région");
                    badge.setStyle("-fx-background-color:#fee2e2;-fx-text-fill:#dc2626;" +
                            "-fx-background-radius:6;-fx-padding:2 8;-fx-font-size:9;-fx-font-weight:bold;");
                    HBox topRow = new HBox(8, lblNom, lblCertif, badge);
                    topRow.setAlignment(Pos.CENTER_LEFT);
                    VBox info = new VBox(3, topRow, lblLoc);
                    HBox row = new HBox(10, avatar, info);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setStyle("-fx-padding:8 12;");
                    setGraphic(row);
                } else {
                    HBox topRow = new HBox(8, lblNom, lblCertif);
                    topRow.setAlignment(Pos.CENTER_LEFT);
                    VBox info = new VBox(3, topRow, lblLoc);
                    HBox row = new HBox(10, avatar, info);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setStyle("-fx-padding:8 12;");
                    setGraphic(row);
                }

                String bgNormal = estRegion ? "rgba(254,242,242,0.6)" : "rgba(255,255,255,0.95)";
                setStyle("-fx-background-color:" + bgNormal + ";-fx-border-color:#f1f5f9;-fx-border-width:0 0 1 0;");
                setOnMouseEntered(e -> setStyle("-fx-background-color:#f0fdf4;-fx-border-color:#bbf7d0;-fx-border-width:0 0 1 0;"));
                setOnMouseExited(e -> setStyle("-fx-background-color:" + bgNormal + ";-fx-border-color:#f1f5f9;-fx-border-width:0 0 1 0;"));
            }
        });

        // Désactiver Affecter si séparateur sélectionné ou rien
        VBox content = new VBox(10, tfSearch, listChoix);
        content.setStyle("-fx-padding:14;-fx-background-color:#ffffff;");
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(500);
        dialog.getDialogPane().setStyle("-fx-background-color:#ffffff;");

        javafx.scene.Node btnAff = dialog.getDialogPane().lookupButton(btnAffecter);
        btnAff.setDisable(true);
        btnAff.setStyle("-fx-background-color:#16a34a;-fx-text-fill:white;-fx-font-weight:bold;-fx-background-radius:8;-fx-padding:8 18;-fx-font-size:13;-fx-cursor:hand;-fx-effect:dropshadow(gaussian,rgba(22,163,74,0.5),8,0,0,2);");

        // ── Styliser le bouton Annuler ──
        javafx.scene.Node btnCancel = dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        if (btnCancel != null) {
            btnCancel.setStyle("-fx-background-color:white;-fx-text-fill:#dc2626;-fx-font-weight:bold;" +
                    "-fx-font-size:13;-fx-background-radius:8;-fx-border-color:#fca5a5;" +
                    "-fx-border-radius:8;-fx-border-width:1.5;-fx-padding:8 18;-fx-cursor:hand;");
            btnCancel.setOnMouseEntered(e -> btnCancel.setStyle(
                    "-fx-background-color:#fee2e2;-fx-text-fill:#dc2626;-fx-font-weight:bold;" +
                    "-fx-font-size:13;-fx-background-radius:8;-fx-border-color:#f87171;" +
                    "-fx-border-radius:8;-fx-border-width:1.5;-fx-padding:8 18;-fx-cursor:hand;"));
            btnCancel.setOnMouseExited(e -> btnCancel.setStyle(
                    "-fx-background-color:white;-fx-text-fill:#dc2626;-fx-font-weight:bold;" +
                    "-fx-font-size:13;-fx-background-radius:8;-fx-border-color:#fca5a5;" +
                    "-fx-border-radius:8;-fx-border-width:1.5;-fx-padding:8 18;-fx-cursor:hand;"));
            // Changer le texte en "✕ Annuler"
            if (btnCancel instanceof javafx.scene.control.Button b) b.setText("\u2715  Annuler");
        }
        listChoix.getSelectionModel().selectedItemProperty().addListener((obs, o, n) ->
                btnAff.setDisable(n == null || "SEP_REGION".equals(n) || "SEP_AUTRE".equals(n)));

        dialog.setResultConverter(bt -> {
            if (bt != btnAffecter) return null;
            Object sel = listChoix.getSelectionModel().getSelectedItem();
            return (sel instanceof edu.pompier.entities.Pompier) ? (edu.pompier.entities.Pompier) sel : null;
        });

        dialog.showAndWait().ifPresent(pompierChoisi -> {
            PompierService.ResultatAffectation res = service.affecterManuellement(
                    Integer.parseInt(idAlerte), pompierChoisi.getId(), localisation);
            if (res != null) {
                edu.pompier.tools.EmailService.envoyerEmailAffectation(
                        res.pompier.getEmail(),
                        res.pompier.getPrenom() + " " + res.pompier.getNom(),
                        "Incendie", "MANUEL", nomLieu, res.distanceKm, res.score);
                chargerHistoriqueAffectations();
                chargerListe();
                Alert ok = new Alert(Alert.AlertType.INFORMATION);
                ok.setTitle("Affectation réussie");
                ok.setContentText("✅ " + res.pompier.getNom() + " " + res.pompier.getPrenom() +
                        " affecté avec succès !\nUn email lui a été envoyé.");
                ok.showAndWait();
            }
        });
    }

    /** Score numérique pour le tri par niveau */
    private int niveauScore(String niveau) {
        if (niveau == null) return 0;
        return switch (niveau.toUpperCase()) {
            case "EXPERT"        -> 4;
            case "AVANCE"        -> 3;
            case "INTERMEDIAIRE" -> 2;
            case "DEBUTANT"      -> 1;
            default              -> 0;
        };
    }

    /** Charge l'historique des affectations dans la listView — format carte par alerte */
    private void chargerHistoriqueAffectations() {
        if (listAffectations == null) return;
        List<String[]> toutes = service.getAffectations();

        List<String[]> affectationsAuto = new ArrayList<>();
        interventionsBrutes = new ArrayList<>();
        for (String[] a : toutes) {
            boolean estIntervention = a.length > 14 && "intervention".equals(a[14]);
            if (estIntervention) interventionsBrutes.add(a);
            else affectationsAuto.add(a);
        }

        java.util.Comparator<String[]> parDateDesc = (x, y) -> {
            String dx = (x[6] != null) ? x[6] : "";
            String dy = (y[6] != null) ? y[6] : "";
            return dy.compareTo(dx);
        };
        affectationsAuto.sort(parDateDesc);
        interventionsBrutes.sort(parDateDesc);

        // Stocker les brutes pour le filtrage
        affectationsAutosBrutes = new ArrayList<>(affectationsAuto);

        if (lblNbAffectations != null) lblNbAffectations.setText(String.valueOf(affectationsAuto.size()));
        if (lblNbInterventions != null) lblNbInterventions.setText(String.valueOf(interventionsBrutes.size()));

        listAffectations.setItems(FXCollections.observableArrayList(toutes));

        // ── Initialiser les listeners de filtres interventions (une seule fois) ──
        if (dpFiltreDate != null && dpFiltreDate.getUserData() == null) {
            dpFiltreDate.setUserData("init");
            dpFiltreDate.valueProperty().addListener((obs, o, n) -> appliquerFiltresInterventions());
            if (tfFiltreNom  != null) tfFiltreNom.textProperty().addListener((obs, o, n) -> appliquerFiltresInterventions());
            if (tfFiltreZone != null) tfFiltreZone.textProperty().addListener((obs, o, n) -> appliquerFiltresInterventions());
        }

        // ── Initialiser les listeners de filtres affectations (une seule fois) ──
        if (dpFiltreDateAff != null && dpFiltreDateAff.getUserData() == null) {
            dpFiltreDateAff.setUserData("init");
            dpFiltreDateAff.valueProperty().addListener((obs, o, n) -> appliquerFiltresAffectations());
            if (tfFiltreNomAff  != null) tfFiltreNomAff.textProperty().addListener((obs, o, n) -> appliquerFiltresAffectations());
            if (tfFiltreZoneAff != null) tfFiltreZoneAff.textProperty().addListener((obs, o, n) -> appliquerFiltresAffectations());
        }

        appliquerFiltresAffectations();
        appliquerFiltresInterventions();
    }

    // ── Bloc obsolète supprimé ──

    /** Applique les filtres date/nom/zone sur interventionsBrutes et rafraîchit la liste */
    private void appliquerFiltresInterventions() {
        if (listInterventions == null) return;
        java.time.LocalDate dateFiltre = (dpFiltreDate != null) ? dpFiltreDate.getValue() : null;
        String nomFiltre  = (tfFiltreNom  != null && !tfFiltreNom.getText().isBlank())  ? tfFiltreNom.getText().trim().toLowerCase()  : null;
        String zoneFiltre = (tfFiltreZone != null && !tfFiltreZone.getText().isBlank()) ? tfFiltreZone.getText().trim().toLowerCase() : null;

        List<String[]> filtrees = new ArrayList<>();
        for (String[] a : interventionsBrutes) {
            if (dateFiltre != null) {
                String dateRaw = a[6] != null ? a[6] : "";
                String datePart = dateRaw.contains(" ") ? dateRaw.split(" ")[0] : dateRaw;
                if (!datePart.equals(dateFiltre.toString())) continue;
            }
            if (nomFiltre != null) {
                String nomComplet = ((a[1] != null ? a[1] : "") + " " + (a[2] != null ? a[2] : "")).toLowerCase();
                if (!nomComplet.contains(nomFiltre)) continue;
            }
            if (zoneFiltre != null) {
                String lieu = (a[5] != null ? a[5] : "").toLowerCase();
                if (!lieu.contains(zoneFiltre)) continue;
            }
            filtrees.add(a);
        }
        if (lblNbInterventions != null) lblNbInterventions.setText(String.valueOf(filtrees.size()));

        // Grouper par date avec en-têtes
        List<String[]> avecGroupes = new ArrayList<>();
        String derniereDateGroupe = null;
        for (String[] a : filtrees) {
            String dateRaw = a[6] != null ? a[6] : "";
            String datePart = dateRaw.contains(" ") ? dateRaw.split(" ")[0] : dateRaw;
            if (!datePart.equals(derniereDateGroupe)) {
                avecGroupes.add(new String[]{"DATE_HEADER", datePart});
                derniereDateGroupe = datePart;
            }
            avecGroupes.add(a);
        }
        afficherInterventions(avecGroupes);
    }

    @FXML
    public void reinitialiserFiltres() {
        if (dpFiltreDate != null) dpFiltreDate.setValue(null);
        if (tfFiltreNom  != null) tfFiltreNom.clear();
        if (tfFiltreZone != null) tfFiltreZone.clear();
    }

    @FXML
    public void reinitialiserFiltresAff() {
        if (dpFiltreDateAff != null) dpFiltreDateAff.setValue(null);
        if (tfFiltreNomAff  != null) tfFiltreNomAff.clear();
        if (tfFiltreZoneAff != null) tfFiltreZoneAff.clear();
    }

    /** Applique les filtres date/nom/zone sur affectationsAutosBrutes et rafraîchit la liste */
    private void appliquerFiltresAffectations() {
        if (listAffectationsAuto == null) return;

        java.time.LocalDate dateFiltre = (dpFiltreDateAff != null) ? dpFiltreDateAff.getValue() : null;
        String nomFiltre  = (tfFiltreNomAff  != null && !tfFiltreNomAff.getText().isBlank())  ? tfFiltreNomAff.getText().trim().toLowerCase()  : null;
        String zoneFiltre = (tfFiltreZoneAff != null && !tfFiltreZoneAff.getText().isBlank()) ? tfFiltreZoneAff.getText().trim().toLowerCase() : null;

        List<String[]> filtrees = new ArrayList<>();
        for (String[] a : affectationsAutosBrutes) {
            // Filtre date
            if (dateFiltre != null) {
                String dateRaw = a[6] != null ? a[6] : "";
                String datePart = dateRaw.contains(" ") ? dateRaw.split(" ")[0] : dateRaw;
                if (!datePart.equals(dateFiltre.toString())) continue;
            }
            // Filtre nom (pompier)
            if (nomFiltre != null) {
                String nomComplet = ((a[1] != null ? a[1] : "") + " " + (a[2] != null ? a[2] : "")).toLowerCase();
                if (!nomComplet.contains(nomFiltre)) continue;
            }
            // Filtre zone/lieu
            if (zoneFiltre != null) {
                String lieu = (a[5] != null ? a[5] : "").toLowerCase();
                if (!lieu.contains(zoneFiltre)) continue;
            }
            filtrees.add(a);
        }

        if (lblNbAffectations != null) lblNbAffectations.setText(String.valueOf(filtrees.size()));
        afficherAffectationsAuto(filtrees);
    }

    private void afficherAffectationsAuto(List<String[]> affectations) {
        if (listAffectationsAuto == null) return;

        // ── Grouper par date avec en-têtes (même logique que les interventions) ──
        List<String[]> avecGroupes = new ArrayList<>();
        String derniereDateGroupe = null;
        for (String[] a : affectations) {
            String dateRaw = a[6] != null ? a[6] : "";
            String datePart = dateRaw.contains(" ") ? dateRaw.split(" ")[0] : dateRaw;
            if (!datePart.equals(derniereDateGroupe)) {
                avecGroupes.add(new String[]{"DATE_HEADER", datePart});
                derniereDateGroupe = datePart;
            }
            avecGroupes.add(a);
        }

        listAffectationsAuto.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String[] a, boolean empty) {
                super.updateItem(a, empty);
                if (empty || a == null) { setGraphic(null); setStyle("-fx-background-color:transparent;"); return; }

                // ── En-tête de date ──
                if ("DATE_HEADER".equals(a[0])) {
                    Label lblDate = new Label("\uD83D\uDCC5  " + a[1]);
                    lblDate.setStyle("-fx-font-size:12;-fx-font-weight:bold;-fx-text-fill:#991b1b;");
                    HBox header = new HBox(lblDate);
                    header.setStyle("-fx-background-color:linear-gradient(to right,#fee2e2,#fff1f2);" +
                            "-fx-background-radius:8;-fx-padding:6 14;" +
                            "-fx-border-color:#fca5a5;-fx-border-radius:8;-fx-border-width:0 0 0 3;");
                    setGraphic(header); setStyle("-fx-background-color:transparent;-fx-padding:6 2 2 2;");
                    return;
                }

                // ── Ligne normale ──
                String dateRaw = a[6] != null ? a[6] : "";
                String heurePart = dateRaw.contains(" ") ? dateRaw.split(" ")[1] : "";
                Label lblHeure = new Label("\uD83D\uDD50 " + heurePart);
                lblHeure.setStyle("-fx-font-size:10;-fx-text-fill:#94a3b8;-fx-min-width:60;");

                String nvBg = switch (a[4] != null ? a[4].toUpperCase() : "") {
                    case "CRITIQUE" -> "#fee2e2"; case "ELEVE" -> "#fecaca"; case "MOYEN" -> "#fef3c7"; default -> "#f1f5f9";
                };
                String nvFg = switch (a[4] != null ? a[4].toUpperCase() : "") {
                    case "CRITIQUE" -> "#7f1d1d"; case "ELEVE" -> "#991b1b"; case "MOYEN" -> "#b45309"; default -> "#374151";
                };
                Label lblType = new Label("\uD83D\uDD25 " + (a[3] != null ? a[3] : "-"));
                lblType.setStyle("-fx-font-size:11;-fx-font-weight:bold;-fx-text-fill:#991b1b;");
                Label lblNiv = new Label(a[4] != null ? a[4].toUpperCase() : "-");
                lblNiv.setStyle("-fx-background-color:"+nvBg+";-fx-text-fill:"+nvFg+
                        ";-fx-background-radius:5;-fx-padding:1 7;-fx-font-size:9;-fx-font-weight:bold;");
                Label lblLieu = new Label("\uD83D\uDCCD " + (a[5] != null ? a[5] : "-"));
                lblLieu.setStyle("-fx-font-size:10;-fx-text-fill:#475569;");
                lblLieu.setWrapText(true); lblLieu.setMaxWidth(150);
                VBox alerteBox = new VBox(2, new HBox(4, lblType, lblNiv), lblLieu);
                alerteBox.setMinWidth(155); alerteBox.setMaxWidth(155);
                alerteBox.setAlignment(Pos.CENTER_LEFT);

                String initiales = "";
                if (a[1] != null && !a[1].isEmpty()) initiales += a[1].substring(0,1).toUpperCase();
                if (a[2] != null && !a[2].isEmpty()) initiales += a[2].substring(0,1).toUpperCase();
                Label avatar = new Label(initiales);
                avatar.setStyle("-fx-background-color:#fee2e2;-fx-text-fill:#991b1b;-fx-font-weight:bold;" +
                        "-fx-font-size:11;-fx-min-width:30;-fx-min-height:30;-fx-background-radius:50;-fx-alignment:center;");
                Label lblNomP = new Label(a[1] + " " + a[2]);
                lblNomP.setStyle("-fx-font-size:12;-fx-font-weight:bold;-fx-text-fill:#1e293b;");
                Label lblDist = new Label("\uD83D\uDDFA " + a[8] + " km");
                lblDist.setStyle("-fx-font-size:10;-fx-text-fill:#0369a1;-fx-background-color:#dbeafe;-fx-background-radius:5;-fx-padding:1 6;");
                Label lblScore = new Label("\u2B50 " + a[9] + " pts");
                lblScore.setStyle("-fx-font-size:10;-fx-text-fill:#d97706;-fx-background-color:#fef3c7;-fx-background-radius:5;-fx-padding:1 6;");
                VBox pompierBox = new VBox(2, new HBox(6, avatar, lblNomP), new HBox(4, lblDist, lblScore));
                pompierBox.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(pompierBox, Priority.ALWAYS);

                String statutNorm = a[7] != null ? a[7].toLowerCase() : "";
                String stBg = switch (statutNorm) {
                    case "en_cours","en cours" -> "#fef3c7"; case "termine","terminee","completed" -> "#dcfce7";
                    case "annule","annulee" -> "#fee2e2"; default -> "#f1f5f9";
                };
                String stFg = switch (statutNorm) {
                    case "en_cours","en cours" -> "#d97706"; case "termine","terminee","completed" -> "#16a34a";
                    case "annule","annulee" -> "#dc2626"; default -> "#64748b";
                };
                Label lblStatutBadge = new Label(a[7] != null ? a[7].toUpperCase().replace("_"," ") : "-");
                lblStatutBadge.setStyle("-fx-background-color:"+stBg+";-fx-text-fill:"+stFg+
                        ";-fx-background-radius:8;-fx-padding:3 8;-fx-font-size:9;-fx-font-weight:bold;");
                lblStatutBadge.setMinWidth(80); lblStatutBadge.setMaxWidth(80);

                String idAffLocal = a[0], idPompierLocal = a[11], nomLieuLocal = a[5];
                String locRaw = (a.length > 13 && a[13] != null) ? a[13] : a[5];
                String idAlerteLocal = a[12];

                Button btnAutre = new Button("+ Autre");
                btnAutre.setStyle("-fx-background-color:#16a34a;-fx-text-fill:white;-fx-background-radius:7;-fx-font-size:10;-fx-font-weight:bold;-fx-padding:3 10;-fx-cursor:hand;");
                btnAutre.setOnAction(ev -> ouvrirDialogueAffectationManuelle(idAlerteLocal, nomLieuLocal, locRaw));

                Button btnSuppr = new Button("\u2716");
                btnSuppr.setStyle("-fx-background-color:#fee2e2;-fx-text-fill:#dc2626;-fx-background-radius:7;-fx-font-size:10;-fx-font-weight:bold;-fx-padding:3 8;-fx-cursor:hand;-fx-border-color:#fca5a5;-fx-border-radius:7;-fx-border-width:1;");
                btnSuppr.setOnAction(ev -> {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Supprimer l'affectation"); confirm.setHeaderText(null);
                    confirm.setContentText("Supprimer l'affectation de " + a[1] + " " + a[2] + " pour l'alerte a " + nomLieuLocal + " ?\n\nLe pompier redeviendra disponible.");
                    confirm.showAndWait().ifPresent(r -> {
                        if (r == ButtonType.OK) { service.supprimerAffectation(Integer.parseInt(idAffLocal), Integer.parseInt(idPompierLocal)); chargerHistoriqueAffectations(); chargerListe(); }
                    });
                });

                VBox actionsBox = new VBox(4, lblStatutBadge, new HBox(4, btnAutre, btnSuppr));
                actionsBox.setAlignment(Pos.CENTER_RIGHT);

                HBox row = new HBox(8, lblHeure, alerteBox, pompierBox, actionsBox);
                row.setAlignment(Pos.CENTER_LEFT);
                String rowBg = switch (statutNorm) {
                    case "en_cours","en cours" -> "rgba(254,243,199,0.6)"; case "termine","terminee","completed" -> "rgba(220,252,231,0.5)"; default -> "white";
                };
                row.setStyle("-fx-background-color:"+rowBg+";-fx-background-radius:10;-fx-border-color:#fecaca;-fx-border-radius:10;-fx-border-width:1;-fx-padding:8 12;");
                row.setOnMouseEntered(e -> row.setStyle("-fx-background-color:#fff1f2;-fx-background-radius:10;-fx-border-color:#f87171;-fx-border-radius:10;-fx-border-width:1.5;-fx-padding:8 12;"));
                row.setOnMouseExited(e -> row.setStyle("-fx-background-color:"+rowBg+";-fx-background-radius:10;-fx-border-color:#fecaca;-fx-border-radius:10;-fx-border-width:1;-fx-padding:8 12;"));
                setGraphic(row); setStyle("-fx-background-color:transparent;-fx-padding:2 2;");
            }
        });
        listAffectationsAuto.setItems(FXCollections.observableArrayList(avecGroupes));
        Label vide = new Label("Aucune affectation enregistree");
        vide.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:13;-fx-font-style:italic;");
        listAffectationsAuto.setPlaceholder(vide);
    }

    private void afficherInterventions(List<String[]> items) {
        if (listInterventions == null) return;
        listInterventions.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String[] a, boolean empty) {
                super.updateItem(a, empty);
                if (empty || a == null) { setGraphic(null); setStyle("-fx-background-color:transparent;"); return; }

                // ── En-tête de date ──
                if ("DATE_HEADER".equals(a[0])) {
                    Label lblDate = new Label("\uD83D\uDCC5  " + a[1]);
                    lblDate.setStyle("-fx-font-size:12;-fx-font-weight:bold;-fx-text-fill:#166534;");
                    HBox header = new HBox(lblDate);
                    header.setStyle("-fx-background-color:linear-gradient(to right,#dcfce7,#f0fdf4);-fx-background-radius:8;-fx-padding:6 14;-fx-border-color:#86efac;-fx-border-radius:8;-fx-border-width:0 0 0 3;");
                    setGraphic(header); setStyle("-fx-background-color:transparent;-fx-padding:6 2 2 2;");
                    return;
                }

                String dateRaw = a[6] != null ? a[6] : "";
                String heurePart = dateRaw.contains(" ") ? dateRaw.split(" ")[1] : "";
                Label lblHeure = new Label("\uD83D\uDD50 " + heurePart);
                lblHeure.setStyle("-fx-font-size:10;-fx-text-fill:#94a3b8;-fx-min-width:60;");

                String nvBg = switch (a[4] != null ? a[4].toUpperCase() : "") {
                    case "CRITIQUE" -> "#fee2e2"; case "ELEVE" -> "#fecaca"; case "MOYEN" -> "#fef3c7"; default -> "#f1f5f9";
                };
                String nvFg = switch (a[4] != null ? a[4].toUpperCase() : "") {
                    case "CRITIQUE" -> "#7f1d1d"; case "ELEVE" -> "#991b1b"; case "MOYEN" -> "#b45309"; default -> "#374151";
                };
                Label lblType = new Label("\uD83D\uDD25 " + (a[3] != null ? a[3] : "-"));
                lblType.setStyle("-fx-font-size:11;-fx-font-weight:bold;-fx-text-fill:#166534;");
                Label lblNiv = new Label(a[4] != null ? a[4].toUpperCase() : "-");
                lblNiv.setStyle("-fx-background-color:"+nvBg+";-fx-text-fill:"+nvFg+";-fx-background-radius:5;-fx-padding:1 7;-fx-font-size:9;-fx-font-weight:bold;");
                Label lblLieu = new Label("\uD83D\uDCCD " + (a[5] != null ? a[5] : "-"));
                lblLieu.setStyle("-fx-font-size:10;-fx-text-fill:#475569;");
                lblLieu.setWrapText(true); lblLieu.setMaxWidth(150);
                VBox alerteBox = new VBox(2, new HBox(4, lblType, lblNiv), lblLieu);
                alerteBox.setMinWidth(155); alerteBox.setMaxWidth(155);
                alerteBox.setAlignment(Pos.CENTER_LEFT);

                String initiales = "";
                if (a[1] != null && !a[1].isEmpty()) initiales += a[1].substring(0,1).toUpperCase();
                if (a[2] != null && !a[2].isEmpty()) initiales += a[2].substring(0,1).toUpperCase();
                Label avatar = new Label(initiales);
                avatar.setStyle("-fx-background-color:#dcfce7;-fx-text-fill:#166534;-fx-font-weight:bold;-fx-font-size:11;-fx-min-width:30;-fx-min-height:30;-fx-background-radius:50;-fx-alignment:center;");
                Label lblNomA = new Label(a[1] + " " + a[2]);
                lblNomA.setStyle("-fx-font-size:12;-fx-font-weight:bold;-fx-text-fill:#1e293b;");
                Label lblVol = new Label("\uD83D\uDC4B VOLONTAIRE");
                lblVol.setStyle("-fx-background-color:#dcfce7;-fx-text-fill:#166534;-fx-background-radius:5;-fx-padding:1 6;-fx-font-size:9;-fx-font-weight:bold;");
                VBox agentBox = new VBox(2, new HBox(6, avatar, lblNomA), lblVol);
                agentBox.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(agentBox, Priority.ALWAYS);

                String statutNorm = a[7] != null ? a[7].toLowerCase() : "";
                String stBg = switch (statutNorm) {
                    case "en_cours","en cours","active" -> "#fef3c7"; case "termine","terminee","completed" -> "#dcfce7";
                    case "annule","annulee" -> "#fee2e2"; default -> "#f1f5f9";
                };
                String stFg = switch (statutNorm) {
                    case "en_cours","en cours","active" -> "#d97706"; case "termine","terminee","completed" -> "#16a34a";
                    case "annule","annulee" -> "#dc2626"; default -> "#64748b";
                };
                Label lblStatutBadge = new Label(a[7] != null ? a[7].toUpperCase().replace("_"," ") : "-");
                lblStatutBadge.setStyle("-fx-background-color:"+stBg+";-fx-text-fill:"+stFg+";-fx-background-radius:8;-fx-padding:3 8;-fx-font-size:9;-fx-font-weight:bold;");
                lblStatutBadge.setMinWidth(80); lblStatutBadge.setMaxWidth(80);

                HBox row = new HBox(8, lblHeure, alerteBox, agentBox, lblStatutBadge);
                row.setAlignment(Pos.CENTER_LEFT);
                String rowBg = switch (statutNorm) {
                    case "en_cours","en cours","active" -> "rgba(240,253,244,0.8)"; case "termine","terminee","completed" -> "rgba(220,252,231,0.5)"; default -> "white";
                };
                row.setStyle("-fx-background-color:"+rowBg+";-fx-background-radius:10;-fx-border-color:#86efac;-fx-border-radius:10;-fx-border-width:1;-fx-padding:8 12;");
                row.setOnMouseEntered(e -> row.setStyle("-fx-background-color:#f0fdf4;-fx-background-radius:10;-fx-border-color:#4ade80;-fx-border-radius:10;-fx-border-width:1.5;-fx-padding:8 12;"));
                row.setOnMouseExited(e -> row.setStyle("-fx-background-color:"+rowBg+";-fx-background-radius:10;-fx-border-color:#86efac;-fx-border-radius:10;-fx-border-width:1;-fx-padding:8 12;"));
                setGraphic(row); setStyle("-fx-background-color:transparent;-fx-padding:2 2;");
            }
        });
        listInterventions.setItems(FXCollections.observableArrayList(items));
        Label vide = new Label("Aucune intervention trouvee");
        vide.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:13;-fx-font-style:italic;");
        listInterventions.setPlaceholder(vide);
    }

    // ── Méthode supprimée — remplacée par chargerHistoriqueAffectations() ci-dessus ──

    // ══════════════════════════════════════════
    //  DONNÉES
    // ══════════════════════════════════════════

    public void chargerListe() {
        try {
            listeTotale.clear();
            listeTotale.addAll(service.getData());
            listView.setItems(listeTotale);
            listView.refresh();
            long disponibles = listeTotale.stream().filter(p -> "disponible".equals(p.getStatut())).count();
            long enMission   = listeTotale.stream().filter(p -> "en_mission".equals(p.getStatut())).count();
            lblDisponibles.setText(String.valueOf(disponibles));
            lblEnMission.setText(String.valueOf(enMission));
            lblTotal.setText(String.valueOf(listeTotale.size()));
            lblGardes.setText(String.valueOf(service.getAllGardesInfo().size()));
        } catch (Exception e) {
            System.out.println("Erreur chargerListe : " + e.getMessage());
        }
    }

    private void chargerPompiersGardes() {
        listePompiersGardes.clear();
        listePompiersGardes.addAll(service.getData());
        listPompiers.setItems(listePompiersGardes);

        // ── Barre de recherche pompiers (initialiser une seule fois) ──
        if (tfRechercheGardes != null && tfRechercheGardes.getUserData() == null) {
            tfRechercheGardes.setUserData("init");
            tfRechercheGardes.textProperty().addListener((obs, o, n) -> {
                if (n == null || n.isBlank()) {
                    listPompiers.setItems(listePompiersGardes);
                } else {
                    String q = n.trim().toLowerCase();
                    FilteredList<Pompier> filtres = new FilteredList<>(listePompiersGardes, p ->
                            p.getNom().toLowerCase().contains(q)
                            || p.getPrenom().toLowerCase().contains(q)
                            || (p.getTelephone() != null && p.getTelephone().contains(q))
                    );
                    listPompiers.setItems(filtres);
                }
            });
        } else if (tfRechercheGardes != null) {
            // Réappliquer le filtre actuel si déjà initialisé
            String q = tfRechercheGardes.getText();
            if (q != null && !q.isBlank()) {
                String ql = q.trim().toLowerCase();
                FilteredList<Pompier> filtres = new FilteredList<>(listePompiersGardes, p ->
                        p.getNom().toLowerCase().contains(ql)
                        || p.getPrenom().toLowerCase().contains(ql)
                        || (p.getTelephone() != null && p.getTelephone().contains(ql))
                );
                listPompiers.setItems(filtres);
            }
        }
    }

    // ══════════════════════════════════════════
    //  PRÉDICTION IA DISPONIBILITÉ
    // ══════════════════════════════════════════

    @FXML
    public void afficherPrediction() {
        if (panneauPrediction != null) {
            panneauPrediction.setVisible(true);
            panneauPrediction.setManaged(true);
        }
        lancerPrediction();
    }

    @FXML
    public void cacherPrediction() {
        if (panneauPrediction != null) {
            panneauPrediction.setVisible(false);
            panneauPrediction.setManaged(false);
        }
    }

    @FXML
    public void lancerPrediction() {
        if (listPredictions == null) return;

        // Initialiser le DatePicker sur la semaine prochaine (une seule fois)
        if (dpSemainePrediction != null && dpSemainePrediction.getUserData() == null) {
            dpSemainePrediction.setUserData("init");
            // Aller au lundi de la semaine prochaine
            java.time.LocalDate lundiProchain = java.time.LocalDate.now()
                    .with(java.time.temporal.TemporalAdjusters.next(java.time.DayOfWeek.MONDAY));
            dpSemainePrediction.setValue(lundiProchain);
            dpSemainePrediction.valueProperty().addListener((obs, o, n) -> {
                if (n != null) {
                    // Recaler sur le lundi de la semaine choisie
                    java.time.LocalDate lundi = n.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
                    jourSelectionne = lundi;
                    mettreAJourBoutonJours(lundi);
                    filtrerPredictionsParJour(lundi);
                }
            });
        }

        Label placeholder = new Label("Analyse en cours...");
        placeholder.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:12;-fx-font-style:italic;");
        listPredictions.setPlaceholder(placeholder);

        Thread t = new Thread(() -> {
            edu.pompier.services.PredictionDisponibilite svc = new edu.pompier.services.PredictionDisponibilite();
            predictionsBrutes = svc.predireSemaineSuivante();
            javafx.application.Platform.runLater(() -> {
                // Afficher pour le jour sélectionné (lundi par défaut)
                java.time.LocalDate lundi = dpSemainePrediction != null && dpSemainePrediction.getValue() != null
                        ? dpSemainePrediction.getValue().with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                        : java.time.LocalDate.now().with(java.time.temporal.TemporalAdjusters.next(java.time.DayOfWeek.MONDAY));
                jourSelectionne = lundi;
                mettreAJourBoutonJours(lundi);
                filtrerPredictionsParJour(lundi);
            });
        });
        t.setDaemon(true);
        t.start();
    }

    @FXML
    public void filtrerParJour(javafx.event.ActionEvent event) {
        if (predictionsBrutes.isEmpty()) {
            // Lancer l'analyse d'abord
            lancerPrediction();
            return;
        }
        // Identifier quel bouton a été cliqué
        Button btnClique = (Button) event.getSource();
        java.time.LocalDate base = dpSemainePrediction != null && dpSemainePrediction.getValue() != null
                ? dpSemainePrediction.getValue().with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                : java.time.LocalDate.now().with(java.time.temporal.TemporalAdjusters.next(java.time.DayOfWeek.MONDAY));

        java.time.LocalDate jourCible = base;
        if      (btnClique == btnJourLun) jourCible = base;
        else if (btnClique == btnJourMar) jourCible = base.plusDays(1);
        else if (btnClique == btnJourMer) jourCible = base.plusDays(2);
        else if (btnClique == btnJourJeu) jourCible = base.plusDays(3);
        else if (btnClique == btnJourVen) jourCible = base.plusDays(4);
        else if (btnClique == btnJourSam) jourCible = base.plusDays(5);
        else if (btnClique == btnJourDim) jourCible = base.plusDays(6);

        jourSelectionne = jourCible;
        mettreAJourBoutonJours(jourCible);
        filtrerPredictionsParJour(jourCible);
    }

    private void mettreAJourBoutonJours(java.time.LocalDate jourActif) {
        if (hboxJoursPrediction == null) return;
        java.time.LocalDate base = jourActif.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));

        Button[] boutons = {btnJourLun, btnJourMar, btnJourMer, btnJourJeu, btnJourVen, btnJourSam, btnJourDim};
        String[] noms = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};
        String[] nomsLongs = {"Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche"};

        for (int i = 0; i < boutons.length; i++) {
            if (boutons[i] == null) continue;
            java.time.LocalDate jourBouton = base.plusDays(i);
            String dateStr = jourBouton.getDayOfMonth() + "/" + jourBouton.getMonthValue();
            boolean actif = jourBouton.equals(jourActif);
            boutons[i].setText(noms[i] + "\n" + dateStr);
            if (actif) {
                boutons[i].setStyle("-fx-background-color:#16a34a;-fx-text-fill:white;-fx-font-size:10;"
                        + "-fx-font-weight:bold;-fx-background-radius:8;-fx-padding:5 10;-fx-cursor:hand;"
                        + "-fx-effect:dropshadow(gaussian,rgba(22,163,74,0.5),8,0,0,2);");
                if (lblJourSelectionne != null)
                    lblJourSelectionne.setText(nomsLongs[i] + " " + dateStr);
            } else {
                boutons[i].setStyle("-fx-background-color:#f1f5f9;-fx-text-fill:#374151;-fx-font-size:10;"
                        + "-fx-font-weight:bold;-fx-background-radius:8;-fx-padding:5 10;-fx-cursor:hand;");
            }
        }

        // Mettre à jour le label info semaine
        java.time.LocalDate dimanche = base.plusDays(6);
        if (lblSemaineInfo != null)
            lblSemaineInfo.setText("Semaine du " + base.getDayOfMonth() + "/" + base.getMonthValue()
                    + " au " + dimanche.getDayOfMonth() + "/" + dimanche.getMonthValue());
    }

    private void filtrerPredictionsParJour(java.time.LocalDate jour) {
        if (listPredictions == null || predictionsBrutes.isEmpty()) return;

        // Appliquer d'abord le filtre forêt sur les prédictions brutes
        List<edu.pompier.services.PredictionDisponibilite.ResultatPrediction> baseFiltre;
        if (foretFiltreActuelle == null) {
            baseFiltre = predictionsBrutes;
        } else {
            String foret = foretFiltreActuelle;
            baseFiltre = predictionsBrutes.stream()
                    .filter(r -> listePompiersGardes.stream()
                            .anyMatch(p -> p.getId() == r.idPompier && foret.equals(p.getNomForet())))
                    .toList();
        }

        edu.pompier.services.PredictionDisponibilite svc = new edu.pompier.services.PredictionDisponibilite();
        List<edu.pompier.services.PredictionDisponibilite.ResultatPrediction> filtrees =
                svc.predirePourJour(jour, baseFiltre);

        afficherPredictions(filtrees);

        Label vide = new Label("Aucun pompier disponible pour ce jour");
        vide.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:12;-fx-font-style:italic;");
        listPredictions.setPlaceholder(vide);
    }

    private void afficherPredictions(List<edu.pompier.services.PredictionDisponibilite.ResultatPrediction> resultats) {
        if (listPredictions == null) return;

        listPredictions.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(edu.pompier.services.PredictionDisponibilite.ResultatPrediction r, boolean empty) {
                super.updateItem(r, empty);
                if (empty || r == null) { setGraphic(null); setStyle("-fx-background-color:transparent;"); return; }

                // ── Couleurs et icônes selon niveau ──
                String couleur, bgCard, bgBadge, emoji, labelNiveau;
                switch (r.niveauDispo) {
                    case "OPTIMAL" -> {
                        couleur = "#16a34a"; bgCard = "#f0fdf4"; bgBadge = "#dcfce7";
                        emoji = "\uD83D\uDFE2"; labelNiveau = "RECOMMANDE";
                    }
                    case "DISPONIBLE" -> {
                        couleur = "#0369a1"; bgCard = "#f0f9ff"; bgBadge = "#dbeafe";
                        emoji = "\uD83D\uDFE1"; labelNiveau = "DISPONIBLE";
                    }
                    case "FATIGUE" -> {
                        couleur = "#d97706"; bgCard = "#fffbeb"; bgBadge = "#fef3c7";
                        emoji = "\uD83D\uDFE0"; labelNiveau = "FATIGUE";
                    }
                    default -> {
                        couleur = "#dc2626"; bgCard = "#fff5f5"; bgBadge = "#fee2e2";
                        emoji = "\uD83D\uDD34"; labelNiveau = "SURCHARGE";
                    }
                }

                // ── Avatar circulaire ──
                javafx.scene.canvas.Canvas avatarCanvas = new javafx.scene.canvas.Canvas(44, 44);
                javafx.scene.canvas.GraphicsContext gc = avatarCanvas.getGraphicsContext2D();
                gc.setFill(javafx.scene.paint.Color.web(couleur));
                gc.fillOval(0, 0, 44, 44);
                gc.setFill(javafx.scene.paint.Color.WHITE);
                gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 15));
                String init = r.nom.substring(0,1).toUpperCase() + r.prenom.substring(0,1).toUpperCase();
                javafx.scene.text.Text t = new javafx.scene.text.Text(init);
                t.setFont(gc.getFont());
                gc.fillText(init, 22 - t.getBoundsInLocal().getWidth()/2, 22 + t.getBoundsInLocal().getHeight()/4);

                // ── Nom + recommandation ──
                Label lblNom = new Label(r.prenom + " " + r.nom);
                lblNom.setStyle("-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:#1e293b;");
                Label lblReco = new Label(r.recommandation);
                lblReco.setStyle("-fx-font-size:10;-fx-text-fill:#64748b;");
                VBox infoBox = new VBox(2, lblNom, lblReco);
                infoBox.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(infoBox, Priority.ALWAYS);

                // ── Indicateurs rapides ──
                Label lblGardes7 = new Label("\uD83D\uDD50 " + r.nbGardes7Jours + "j/7j");
                lblGardes7.setStyle("-fx-font-size:10;-fx-text-fill:#64748b;"
                        + "-fx-background-color:#f1f5f9;-fx-background-radius:6;-fx-padding:2 6;");
                Label lblGardes30 = new Label("\uD83D\uDCC5 " + r.nbGardes30Jours + "/30j");
                lblGardes30.setStyle("-fx-font-size:10;-fx-text-fill:#64748b;"
                        + "-fx-background-color:#f1f5f9;-fx-background-radius:6;-fx-padding:2 6;");

                // ── Badge niveau ──
                Label lblBadge = new Label(emoji + "  " + labelNiveau);
                lblBadge.setStyle("-fx-background-color:" + bgBadge + ";-fx-text-fill:" + couleur + ";"
                        + "-fx-background-radius:20;-fx-padding:4 12;-fx-font-size:11;-fx-font-weight:bold;");

                VBox droite = new VBox(5, lblBadge, new HBox(4, lblGardes7, lblGardes30));
                droite.setAlignment(Pos.CENTER_RIGHT);

                HBox row = new HBox(12, new javafx.scene.layout.StackPane(avatarCanvas), infoBox, droite);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-background-color:" + bgCard + ";-fx-background-radius:12;"
                        + "-fx-border-color:" + couleur + "44;-fx-border-radius:12;-fx-border-width:1.5;"
                        + "-fx-padding:10 14;-fx-cursor:hand;");
                row.setOnMouseEntered(e -> row.setStyle("-fx-background-color:" + bgBadge + ";-fx-background-radius:12;"
                        + "-fx-border-color:" + couleur + ";-fx-border-radius:12;-fx-border-width:2;"
                        + "-fx-padding:10 14;-fx-cursor:hand;"
                        + "-fx-effect:dropshadow(gaussian," + couleur + "44,10,0,0,3);"));
                row.setOnMouseExited(e -> row.setStyle("-fx-background-color:" + bgCard + ";-fx-background-radius:12;"
                        + "-fx-border-color:" + couleur + "44;-fx-border-radius:12;-fx-border-width:1.5;"
                        + "-fx-padding:10 14;-fx-cursor:hand;"));
                // Clic sur la carte → popup détails
                row.setOnMouseClicked(e -> afficherDetailsPrediction(r));
                setGraphic(row);
                setStyle("-fx-background-color:transparent;-fx-padding:3 4;");
            }
        });

        listPredictions.setItems(javafx.collections.FXCollections.observableArrayList(resultats));

        Label vide = new Label("Cliquez sur 'Analyser' pour lancer la prediction");
        vide.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:12;-fx-font-style:italic;");
        listPredictions.setPlaceholder(vide);
    }

    private void afficherDetailsPrediction(edu.pompier.services.PredictionDisponibilite.ResultatPrediction r) {
        Stage popup = new Stage();
        popup.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        popup.setTitle("Disponibilite — " + r.prenom + " " + r.nom);
        popup.setResizable(false);

        String couleur, gradient, bgCard;
        String titreNiveau, descNiveau, emojiNiveau;
        switch (r.niveauDispo) {
            case "OPTIMAL" -> {
                couleur = "#16a34a"; gradient = "linear-gradient(to right,#166534,#16a34a)";
                bgCard = "#f0fdf4"; titreNiveau = "RECOMMANDE CETTE SEMAINE";
                descNiveau = "Ce pompier est repose et fiable. C'est le moment ideal pour lui assigner une garde.";
                emojiNiveau = "\uD83D\uDFE2";
            }
            case "DISPONIBLE" -> {
                couleur = "#0369a1"; gradient = "linear-gradient(to right,#1e3a8a,#0369a1)";
                bgCard = "#f0f9ff"; titreNiveau = "DISPONIBLE AVEC MODERATION";
                descNiveau = "Il peut assurer une garde, mais a deja fourni un effort notable. Evitez de le surcharger.";
                emojiNiveau = "\uD83D\uDFE1";
            }
            case "FATIGUE" -> {
                couleur = "#d97706"; gradient = "linear-gradient(to right,#92400e,#d97706)";
                bgCard = "#fffbeb"; titreNiveau = "FATIGUE — A MENAGER";
                descNiveau = "Ce pompier a enchaine plusieurs gardes recemment. Lui accorder du repos est conseille.";
                emojiNiveau = "\uD83D\uDFE0";
            }
            default -> {
                couleur = "#dc2626"; gradient = "linear-gradient(to right,#991b1b,#dc2626)";
                bgCard = "#fff5f5"; titreNiveau = "SURCHARGE — NE PAS ASSIGNER";
                descNiveau = "Ce pompier est en surcharge. L'assigner cette semaine risque de provoquer une absence ou une erreur.";
                emojiNiveau = "\uD83D\uDD34";
            }
        }

        // ── En-tête ──
        Label iconeGrand = new Label(emojiNiveau);
        iconeGrand.setStyle("-fx-font-size:36;");

        // Avatar
        javafx.scene.canvas.Canvas avatarGrand = new javafx.scene.canvas.Canvas(70, 70);
        javafx.scene.canvas.GraphicsContext gcA = avatarGrand.getGraphicsContext2D();
        gcA.setFill(javafx.scene.paint.Color.web(couleur + "33"));
        gcA.fillOval(0, 0, 70, 70);
        gcA.setFill(javafx.scene.paint.Color.web(couleur));
        gcA.fillOval(4, 4, 62, 62);
        gcA.setFill(javafx.scene.paint.Color.WHITE);
        gcA.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 22));
        String init = r.nom.substring(0,1).toUpperCase() + r.prenom.substring(0,1).toUpperCase();
        javafx.scene.text.Text tA = new javafx.scene.text.Text(init);
        tA.setFont(gcA.getFont());
        gcA.fillText(init, 35 - tA.getBoundsInLocal().getWidth()/2, 35 + tA.getBoundsInLocal().getHeight()/4);

        Label lblNomGrand = new Label(r.prenom + " " + r.nom.toUpperCase());
        lblNomGrand.setStyle("-fx-font-size:17;-fx-font-weight:bold;-fx-text-fill:white;");
        Label lblNiveauGrand = new Label(emojiNiveau + "  " + titreNiveau);
        lblNiveauGrand.setStyle("-fx-font-size:11;-fx-text-fill:rgba(255,255,255,0.9);-fx-font-weight:bold;");

        VBox headerText = new VBox(4, lblNomGrand, lblNiveauGrand);
        headerText.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(headerText, Priority.ALWAYS);
        HBox header = new HBox(14, new javafx.scene.layout.StackPane(avatarGrand), headerText, iconeGrand);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle(gradient + ";-fx-padding:20 24;-fx-background-radius:16 16 0 0;");

        // ── Explication en langage naturel ──
        Label lblDesc = new Label(descNiveau);
        lblDesc.setWrapText(true);
        lblDesc.setStyle("-fx-font-size:12;-fx-text-fill:#374151;-fx-font-style:italic;");
        VBox descBox = new VBox(lblDesc);
        descBox.setStyle("-fx-background-color:" + bgCard + ";-fx-padding:14 24;"
                + "-fx-border-color:" + couleur + "33;-fx-border-width:0 0 1 0;");

        // ── 4 faits avec les VRAIES données (passé + futur) ──
        VBox faitsBox = new VBox(8);
        faitsBox.setStyle("-fx-padding:14 24 10 24;-fx-border-color:#e2e8f0;-fx-border-width:0 0 1 0;");

        // Fait 1 : charge sur 14 jours (±7j autour d'aujourd'hui)
        String fait1Texte, fait1Couleur;
        if (r.nbGardes7Jours == 0) {
            fait1Texte = "Aucune garde sur les 14 jours — completement disponible";
            fait1Couleur = "#16a34a";
        } else if (r.nbGardes7Jours <= 2) {
            fait1Texte = r.nbGardes7Jours + " garde(s) sur 14 jours — rythme normal";
            fait1Couleur = "#0369a1";
        } else if (r.nbGardes7Jours <= 5) {
            fait1Texte = r.nbGardes7Jours + " gardes sur 14 jours — charge elevee";
            fait1Couleur = "#d97706";
        } else {
            fait1Texte = r.nbGardes7Jours + " gardes sur 14 jours — surcharge, repos necessaire";
            fait1Couleur = "#dc2626";
        }
        faitsBox.getChildren().add(carteFait("\uD83D\uDD50", "Charge recente (14 jours)", fait1Texte, fait1Couleur));

        // Fait 2 : activite sur 60 jours (±30j)
        String fait2Texte, fait2Couleur;
        if (r.nbGardes30Jours == 0) {
            fait2Texte = "Aucune garde sur 60 jours — pompier nouveau ou inactif";
            fait2Couleur = "#64748b";
        } else if (r.nbGardes30Jours <= 8) {
            fait2Texte = r.nbGardes30Jours + " gardes sur 60 jours — engagement regulier";
            fait2Couleur = "#16a34a";
        } else if (r.nbGardes30Jours <= 15) {
            fait2Texte = r.nbGardes30Jours + " gardes sur 60 jours — tres actif";
            fait2Couleur = "#d97706";
        } else {
            fait2Texte = r.nbGardes30Jours + " gardes sur 60 jours — rythme excessif";
            fait2Couleur = "#dc2626";
        }
        faitsBox.getChildren().add(carteFait("\uD83D\uDCC5", "Activite sur 60 jours", fait2Texte, fait2Couleur));

        // Fait 3 : gardes planifiées semaine prochaine
        String fait3Texte, fait3Couleur;
        if (r.nbGardesPlanifiees == 0) {
            fait3Texte = "Aucune garde planifiee la semaine prochaine — agenda libre";
            fait3Couleur = "#16a34a";
        } else if (r.nbGardesPlanifiees <= 2) {
            fait3Texte = r.nbGardesPlanifiees + " garde(s) planifiee(s) — peut en assurer une autre";
            fait3Couleur = "#0369a1";
        } else {
            fait3Texte = r.nbGardesPlanifiees + " gardes planifiees — agenda tres charge";
            fait3Couleur = "#dc2626";
        }
        faitsBox.getChildren().add(carteFait("\uD83D\uDCC6", "Semaine prochaine", fait3Texte, fait3Couleur));

        // Fait 4 : statut actuel
        String fait4Texte = "en_mission".equals(r.statut)
                ? "Actuellement EN MISSION — attendre son retour avant d'assigner"
                : "Actuellement DISPONIBLE — peut etre contacte immediatement";
        String fait4Couleur = "en_mission".equals(r.statut) ? "#dc2626" : "#16a34a";
        faitsBox.getChildren().add(carteFait("\uD83D\uDC68\u200D\uD83D\uDE92", "Statut actuel", fait4Texte, fait4Couleur));

        // ── Boutons ──
        Button btnAssigner = new Button("\uD83D\uDD50  Assigner une garde a " + r.prenom);
        btnAssigner.setStyle("-fx-background-color:" + couleur + ";-fx-text-fill:white;-fx-font-size:12;"
                + "-fx-font-weight:bold;-fx-background-radius:10;-fx-padding:9 18;-fx-cursor:hand;");
        btnAssigner.setOnAction(e -> {
            popup.close();
            for (Pompier pg : listePompiersGardes) {
                if (pg.getId() == r.idPompier) {
                    listPompiers.getSelectionModel().select(pg);
                    pompierSelectionne = pg;
                    lblPompierSelectionne.setText(pg.getNom() + " " + pg.getPrenom() + "  |  " + pg.getTelephone());
                    break;
                }
            }
        });

        Button btnFermer = new Button("Fermer");
        btnFermer.setStyle("-fx-background-color:white;-fx-text-fill:#374151;-fx-font-size:12;"
                + "-fx-font-weight:bold;-fx-background-radius:10;-fx-border-color:#e2e8f0;"
                + "-fx-border-radius:10;-fx-border-width:1;-fx-padding:9 18;-fx-cursor:hand;");
        btnFermer.setOnAction(e -> popup.close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox boutons = new HBox(10, spacer, btnFermer, btnAssigner);
        boutons.setAlignment(Pos.CENTER_RIGHT);
        boutons.setStyle("-fx-padding:14 24 18 24;");

        VBox root = new VBox(0, header, descBox, faitsBox, boutons);
        root.setStyle("-fx-background-color:white;-fx-background-radius:16;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.3),20,0,0,6);");
        root.setPrefWidth(460);

        popup.setScene(new javafx.scene.Scene(root));
        popup.show();
    }

    /** Carte de fait avec icône, titre et texte coloré */
    private HBox carteFait(String icone, String titre, String texte, String couleur) {
        Label lblIcone = new Label(icone);
        lblIcone.setStyle("-fx-font-size:18;-fx-min-width:28;");
        Label lblTitre = new Label(titre);
        lblTitre.setStyle("-fx-font-size:10;-fx-font-weight:bold;-fx-text-fill:#94a3b8;");
        Label lblTexte = new Label(texte);
        lblTexte.setStyle("-fx-font-size:12;-fx-font-weight:bold;-fx-text-fill:" + couleur + ";");
        lblTexte.setWrapText(true);
        VBox textBox = new VBox(1, lblTitre, lblTexte);
        textBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textBox, Priority.ALWAYS);
        HBox carte = new HBox(10, lblIcone, textBox);
        carte.setAlignment(Pos.CENTER_LEFT);
        carte.setStyle("-fx-background-color:#f8fafc;-fx-background-radius:10;-fx-padding:8 12;"
                + "-fx-border-color:#e2e8f0;-fx-border-radius:10;-fx-border-width:1;");
        return carte;
    }

    @FXML
    public void reinitialiserFiltreForetGardes() {
        if (cbFiltreForetGardes != null) cbFiltreForetGardes.setValue("Toutes les forets");
        // Le listener s'occupe du reste
    }

    /** Applique le filtre forêt sur les 3 listes : pompiers, gardes, prédiction */
    private void appliquerFiltreForet() {
        // ── Mise à jour du label info ──
        if (lblInfoFiltreForet != null) {
            lblInfoFiltreForet.setText(foretFiltreActuelle != null
                    ? "Foret : " + foretFiltreActuelle : "");
        }

        // ── 1. Filtrer la liste des pompiers à sélectionner ──
        if (foretFiltreActuelle == null) {
            listPompiers.setItems(listePompiersGardes);
        } else {
            String foret = foretFiltreActuelle;
            FilteredList<Pompier> filtres = new FilteredList<>(listePompiersGardes,
                    p -> foret.equals(p.getNomForet()));
            listPompiers.setItems(filtres);
        }

        // ── 2. Filtrer la liste des gardes ──
        appliquerFiltresGardes();

        // ── 3. Filtrer la prédiction si elle est visible ──
        if (panneauPrediction != null && panneauPrediction.isVisible() && !predictionsBrutes.isEmpty()) {
            List<edu.pompier.services.PredictionDisponibilite.ResultatPrediction> filtrees;
            if (foretFiltreActuelle == null) {
                filtrees = predictionsBrutes;
            } else {
                String foret = foretFiltreActuelle;
                filtrees = predictionsBrutes.stream()
                        .filter(r -> {
                            // Chercher la forêt du pompier dans la liste
                            for (Pompier p : listePompiersGardes) {
                                if (p.getId() == r.idPompier)
                                    return foret.equals(p.getNomForet());
                            }
                            return false;
                        }).toList();
            }
            filtrerPredictionsParJour(jourSelectionne);
        }
    }

    public void chargerGardesListe() {
        gardesBrutes = service.getAllGardesInfo();

        // ── Trier par date puis créneau ──
        gardesBrutes.sort((a, b) -> {
            int cmpDate = (a.dateGarde != null ? a.dateGarde : "").compareTo(b.dateGarde != null ? b.dateGarde : "");
            if (cmpDate != 0) return cmpDate;
            return (a.creneau != null ? a.creneau : "").compareTo(b.creneau != null ? b.creneau : "");
        });

        // ── Initialiser le ComboBox créneau (une seule fois) ──
        if (cbFiltreCreneauGarde != null && cbFiltreCreneauGarde.getUserData() == null) {
            cbFiltreCreneauGarde.setUserData("init");
            cbFiltreCreneauGarde.setItems(FXCollections.observableArrayList(
                    "Tous les creneaux", "matin", "apres-midi", "nuit", "24h"));
            cbFiltreCreneauGarde.setValue("Tous les creneaux");
            cbFiltreCreneauGarde.valueProperty().addListener((obs, o, n) -> appliquerFiltresGardes());
            if (tfFiltreNomGarde != null) tfFiltreNomGarde.textProperty().addListener((obs, o, n) -> appliquerFiltresGardes());
            if (tfFiltreTelGarde != null) tfFiltreTelGarde.textProperty().addListener((obs, o, n) -> appliquerFiltresGardes());
        }

        appliquerFiltresGardes();
    }

    private void appliquerFiltresGardes() {
        if (listGardes == null) return;

        String creneauFiltre = (cbFiltreCreneauGarde != null && cbFiltreCreneauGarde.getValue() != null
                && !"Tous les creneaux".equals(cbFiltreCreneauGarde.getValue()))
                ? cbFiltreCreneauGarde.getValue() : null;
        String nomFiltre = (tfFiltreNomGarde != null && !tfFiltreNomGarde.getText().isBlank())
                ? tfFiltreNomGarde.getText().trim().toLowerCase() : null;
        String telFiltre = (tfFiltreTelGarde != null && !tfFiltreTelGarde.getText().isBlank())
                ? tfFiltreTelGarde.getText().trim() : null;

        List<GestionGardesController.GardeInfo> filtrees = new ArrayList<>();
        for (GestionGardesController.GardeInfo g : gardesBrutes) {
            if (creneauFiltre != null && !creneauFiltre.equals(g.creneau)) continue;
            if (nomFiltre != null && (g.nomPompier == null || !g.nomPompier.toLowerCase().contains(nomFiltre))) continue;
            if (telFiltre != null && (g.telephone == null || !g.telephone.contains(telFiltre))) continue;
            // ── Filtre forêt ──
            if (foretFiltreActuelle != null) {
                boolean pompierDansForet = false;
                for (Pompier p : listePompiersGardes) {
                    if (p.getId() == g.idPompier && foretFiltreActuelle.equals(p.getNomForet())) {
                        pompierDansForet = true; break;
                    }
                }
                if (!pompierDansForet) continue;
            }
            filtrees.add(g);
        }

        if (lblNbGardesFiltrees != null)
            lblNbGardesFiltrees.setText(filtrees.size() + " garde(s)");

        // ── Construire la liste avec en-têtes de date ──
        // Chaque item est soit une GardeInfo normale, soit un "header" avec dateGarde="DATE_HEADER"
        List<GestionGardesController.GardeInfo> avecGroupes = new ArrayList<>();
        String derniereDateGroupe = null;
        for (GestionGardesController.GardeInfo g : filtrees) {
            String date = g.dateGarde != null ? g.dateGarde : "";
            if (!date.equals(derniereDateGroupe)) {
                // En-tête de date
                GestionGardesController.GardeInfo header = new GestionGardesController.GardeInfo(
                        -1, -1, "DATE_HEADER", date, "", date);
                avecGroupes.add(header);
                derniereDateGroupe = date;
            }
            avecGroupes.add(g);
        }

        afficherListeGardes(avecGroupes);
    }

    @FXML
    public void reinitialiserFiltresGardes() {
        if (cbFiltreCreneauGarde != null) cbFiltreCreneauGarde.setValue("Tous les creneaux");
        if (tfFiltreNomGarde != null) tfFiltreNomGarde.clear();
        if (tfFiltreTelGarde != null) tfFiltreTelGarde.clear();
    }

    private void afficherListeGardes(List<GestionGardesController.GardeInfo> items) {
        listGardes.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(GestionGardesController.GardeInfo g, boolean empty) {
                super.updateItem(g, empty);
                if (empty || g == null) { setGraphic(null); setStyle("-fx-background-color:transparent;"); return; }

                // ── En-tête de date ──
                if ("DATE_HEADER".equals(g.nomPompier)) {
                    // Formater la date lisiblement
                    String dateAff = g.dateGarde != null ? g.dateGarde : "";
                    // Compter les gardes de ce jour
                    long nbJour = gardesBrutes.stream()
                            .filter(x -> dateAff.equals(x.dateGarde)).count();

                    Label lblDate = new Label("\uD83D\uDCC5  " + dateAff);
                    lblDate.setStyle("-fx-font-size:12;-fx-font-weight:bold;-fx-text-fill:#166534;");
                    Label lblCount = new Label(nbJour + " garde(s)");
                    lblCount.setStyle("-fx-background-color:#dcfce7;-fx-text-fill:#166534;"
                            + "-fx-background-radius:20;-fx-padding:2 10;-fx-font-size:10;-fx-font-weight:bold;");
                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);
                    HBox header = new HBox(10, lblDate, spacer, lblCount);
                    header.setAlignment(Pos.CENTER_LEFT);
                    header.setStyle("-fx-background-color:linear-gradient(to right,#dcfce7,#f0fdf4);"
                            + "-fx-background-radius:8;-fx-padding:7 14;"
                            + "-fx-border-color:#86efac;-fx-border-radius:8;-fx-border-width:0 0 0 4;");
                    setGraphic(header);
                    setStyle("-fx-background-color:transparent;-fx-padding:6 2 2 2;");
                    return;
                }

                // ── Ligne normale ──
                String creneauBg = switch (g.creneau != null ? g.creneau : "") {
                    case "matin"      -> "#fef9c3"; case "apres-midi" -> "#dbeafe";
                    case "nuit"       -> "#ede9fe"; default           -> "#dcfce7";
                };
                String creneauFg = switch (g.creneau != null ? g.creneau : "") {
                    case "matin"      -> "#ca8a04"; case "apres-midi" -> "#1d4ed8";
                    case "nuit"       -> "#6d28d9"; default           -> "#15803d";
                };

                // Badge créneau
                Label lblCreneau = new Label(g.creneau != null ? g.creneau.toUpperCase() : "-");
                lblCreneau.setStyle("-fx-background-color:" + creneauBg + ";-fx-text-fill:" + creneauFg
                        + ";-fx-font-size:10;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:3 10;");
                lblCreneau.setMinWidth(90);

                // Heures correspondantes sous le badge
                String heures = switch (g.creneau != null ? g.creneau.toLowerCase().split(" ")[0] : "") {
                    case "matin"      -> "06:00 - 12:00";
                    case "apres-midi" -> "12:00 - 18:00";
                    case "nuit"       -> "18:00 - 06:00";
                    case "24h"        -> "00:00 - 00:00";
                    default           -> "";
                };
                Label lblHeures = new Label(heures);
                lblHeures.setStyle("-fx-font-size:9;-fx-text-fill:" + creneauFg + ";-fx-font-style:italic;");
                VBox creneauBox = new VBox(2, lblCreneau, lblHeures);
                creneauBox.setAlignment(Pos.CENTER_LEFT);
                creneauBox.setMinWidth(90);

                // Initiales avatar
                String[] parts = g.nomPompier != null ? g.nomPompier.split(" ", 2) : new String[]{"?", ""};
                String init = (parts[0].length() > 0 ? parts[0].substring(0,1).toUpperCase() : "?")
                        + (parts.length > 1 && parts[1].length() > 0 ? parts[1].substring(0,1).toUpperCase() : "");
                Label avatar = new Label(init);
                avatar.setStyle("-fx-background-color:#1B5E20;-fx-text-fill:white;-fx-font-weight:bold;"
                        + "-fx-font-size:11;-fx-min-width:32;-fx-min-height:32;"
                        + "-fx-background-radius:50;-fx-alignment:center;");

                Label lblNom = new Label(g.nomPompier != null ? g.nomPompier : "-");
                lblNom.setStyle("-fx-font-size:12;-fx-font-weight:bold;-fx-text-fill:#1e293b;");
                Label lblTel = new Label(g.telephone != null ? "\uD83D\uDCDE " + g.telephone : "-");
                lblTel.setStyle("-fx-font-size:11;-fx-text-fill:#475569;");
                VBox pompierBox = new VBox(2, lblNom, lblTel);
                pompierBox.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(pompierBox, Priority.ALWAYS);

                Label lblStatut = new Label("PLANIFIEE");
                lblStatut.setStyle("-fx-background-color:#fef3c7;-fx-text-fill:#d97706;"
                        + "-fx-font-size:9;-fx-font-weight:bold;-fx-background-radius:8;-fx-padding:3 8;");

                Button btnMod = new Button("\u270E");
                btnMod.setStyle("-fx-background-color:#ede9fe;-fx-text-fill:#7c3aed;"
                        + "-fx-background-radius:6;-fx-font-size:12;-fx-cursor:hand;-fx-min-width:30;-fx-min-height:28;");
                btnMod.setOnAction(e -> {
                    gardeIdEnEdition = g.idGarde;
                    for (Pompier pg : listePompiersGardes) {
                        if (pg.getId() == g.idPompier) {
                            listPompiers.getSelectionModel().select(pg);
                            pompierSelectionne = pg;
                            lblPompierSelectionne.setText(pg.getNom() + " " + pg.getPrenom() + "  |  " + pg.getTelephone());
                            break;
                        }
                    }
                    cbCreneau.setValue(g.creneau);
                    try { dpDateDebut.setValue(LocalDate.parse(g.dateGarde)); dpDateFin.setValue(LocalDate.parse(g.dateGarde)); }
                    catch (Exception ex) { dpDateDebut.setValue(LocalDate.now()); dpDateFin.setValue(LocalDate.now()); }
                    lblTitreForm.setText("Modifier la garde");
                    btnSauvegarder.setText("Mettre a jour");
                    lblErreurGarde.setText("");
                });

                Button btnSup = new Button("\u2716");
                btnSup.setStyle("-fx-background-color:#fee2e2;-fx-text-fill:#dc2626;"
                        + "-fx-background-radius:6;-fx-font-size:12;-fx-cursor:hand;-fx-min-width:30;-fx-min-height:28;");
                btnSup.setOnAction(e -> {
                    // Dialogue de suppression stylisé (réutilise la logique existante)
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Supprimer la garde");
                    confirm.setHeaderText(null);
                    confirm.setContentText("Supprimer la garde du " + g.dateGarde + " (" + (g.creneau != null ? g.creneau.toUpperCase() : "") + ") pour " + g.nomPompier + " ?");
                    confirm.showAndWait().ifPresent(r -> {
                        if (r == ButtonType.OK) { service.deleteGardeById(g.idGarde); chargerGardesListe(); chargerListe(); }
                    });
                });

                HBox actions = new HBox(6, btnMod, btnSup);
                actions.setAlignment(Pos.CENTER_RIGHT);

                HBox row = new HBox(10, creneauBox, avatar, pompierBox, lblStatut, actions);
                row.setAlignment(Pos.CENTER_LEFT);

                String bgNormal = getIndex() % 2 == 0 ? "white" : "#f8fafc";
                row.setStyle("-fx-padding:8 14;");
                setStyle("-fx-background-color:" + bgNormal + ";-fx-border-color:#f1f5f9;-fx-border-width:0 0 1 0;");
                setOnMouseEntered(e -> setStyle("-fx-background-color:#f0fdf4;-fx-border-color:#bbf7d0;-fx-border-width:0 0 1 0;"));
                setOnMouseExited(e -> setStyle("-fx-background-color:" + bgNormal + ";-fx-border-color:#f1f5f9;-fx-border-width:0 0 1 0;"));
                setGraphic(row);
            }
        });
        listGardes.setItems(FXCollections.observableArrayList(items));
        Label vide = new Label("Aucune garde trouvee");
        vide.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:13;-fx-font-style:italic;");
        listGardes.setPlaceholder(vide);
    }

    // ══════════════════════════════════════════
    //  AJOUTER POMPIER
    // ══════════════════════════════════════════
    @FXML
    public void ouvrirAjout() {
        try {
            URL fxmlUrl = getClass().getClassLoader().getResource("AjouterPompier.fxml");
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Ajouter un pompier");
            stage.setScene(new Scene(root));
            stage.setOnHidden(e -> chargerListe());
            stage.show();
        } catch (IOException e) { System.out.println(e.getMessage()); }
    }

    // ══════════════════════════════════════════
    //  DÉCONNEXION
    // ══════════════════════════════════════════
    @FXML
    public void handleDeconnexion() {
        arreterSurveillance();
        try {
            Stage stage = (Stage) menuLateral.getScene().getWindow();

            // Récupérer les dimensions de l'écran
            javafx.geometry.Rectangle2D screenBounds =
                    javafx.stage.Screen.getPrimary().getVisualBounds();

            // Charger le FXML
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/Login.fxml"));
            javafx.scene.Parent root = loader.load();

            // Préparer l'animation d'entrée
            root.setOpacity(0);

            // Créer la scène avec les dimensions exactes de l'écran
            javafx.scene.Scene scene = new javafx.scene.Scene(root,
                    screenBounds.getWidth(), screenBounds.getHeight());
            scene.setFill(javafx.scene.paint.Color.web("#020c02"));

            // Appliquer la scène et positionner la fenêtre AVANT show()
            stage.setMaximized(false);
            stage.setScene(scene);
            stage.setTitle("ForestGuard - Connexion");
            stage.setResizable(true);
            stage.setX(screenBounds.getMinX());
            stage.setY(screenBounds.getMinY());
            stage.setWidth(screenBounds.getWidth());
            stage.setHeight(screenBounds.getHeight());
            stage.show();

            // Maximiser et animer dans le prochain pulse de rendu
            javafx.application.Platform.runLater(() -> {
                stage.setMaximized(true);
                javafx.animation.FadeTransition ft =
                        new javafx.animation.FadeTransition(Duration.millis(600), root);
                ft.setFromValue(0);
                ft.setToValue(1);
                ft.play();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════
    //  SIDEBAR
    // ══════════════════════════════════════════
    @FXML
    public void toggleMenu() {
        Timeline tl = new Timeline();
        if (!menuOuvert) {
            menuLateral.setMinWidth(200); menuLateral.setMaxWidth(200);
            tl.getKeyFrames().add(new KeyFrame(Duration.millis(300), new KeyValue(menuLateral.prefWidthProperty(), 200)));
            tl.setOnFinished(e -> {
                if (btnDashboard != null) { btnDashboard.setText("\uD83C\uDFE0  Dashboard"); btnDashboard.setPrefWidth(184); btnDashboard.setMinWidth(184); }
                btnPompiers.setText("\uD83D\uDE92  Pompiers"); btnPompiers.setPrefWidth(184); btnPompiers.setMinWidth(184);
                btnForets.setText("\uD83C\uDF32  Forets"); btnForets.setPrefWidth(184); btnForets.setMinWidth(184);
                btnCapteurs.setText("\uD83D\uDCE1  Capteurs"); btnCapteurs.setPrefWidth(184); btnCapteurs.setMinWidth(184);
                btnDonnees.setText("\uD83D\uDCCA  Donnees"); btnDonnees.setPrefWidth(184); btnDonnees.setMinWidth(184);
                btnAlertes.setText("\uD83D\uDEA8  Alertes"); btnAlertes.setPrefWidth(184); btnAlertes.setMinWidth(184);
                btnUtilisateurs.setText("\uD83D\uDC64  Utilisateurs"); btnUtilisateurs.setPrefWidth(184); btnUtilisateurs.setMinWidth(184);
            });
            menuOuvert = true;
        } else {
            tl.setOnFinished(e -> {
                menuLateral.setMinWidth(70); menuLateral.setMaxWidth(70);
                if (btnDashboard != null) { btnDashboard.setText("\uD83C\uDFE0"); btnDashboard.setPrefWidth(54); btnDashboard.setMinWidth(54); }
                btnPompiers.setText("\uD83D\uDE92"); btnPompiers.setPrefWidth(54); btnPompiers.setMinWidth(54);
                btnForets.setText("\uD83C\uDF32"); btnForets.setPrefWidth(54); btnForets.setMinWidth(54);
                btnCapteurs.setText("\uD83D\uDCE1"); btnCapteurs.setPrefWidth(54); btnCapteurs.setMinWidth(54);
                btnDonnees.setText("\uD83D\uDCCA"); btnDonnees.setPrefWidth(54); btnDonnees.setMinWidth(54);
                btnAlertes.setText("\uD83D\uDEA8"); btnAlertes.setPrefWidth(54); btnAlertes.setMinWidth(54);
                btnUtilisateurs.setText("\uD83D\uDC64"); btnUtilisateurs.setPrefWidth(54); btnUtilisateurs.setMinWidth(54);
            });
            tl.getKeyFrames().add(new KeyFrame(Duration.millis(300), new KeyValue(menuLateral.prefWidthProperty(), 70)));
            menuOuvert = false;
        }
        tl.play();
    }
}