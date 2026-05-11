package edu.pompier.controllers;

import edu.pompier.entities.Pompier;
import edu.pompier.services.PompierService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;

import java.time.LocalDate;
import java.util.List;

public class GestionGardesController {

    // ── Panneau gauche ──
    @FXML private TextField tfRecherchePompier;
    @FXML private ListView<Pompier> listPompiers;

    // ── Panneau droit ──
    @FXML private Label lblPompierSelectionne;
    @FXML private Label lblTitreForm;
    @FXML private Button btnSauvegarder;
    @FXML private ComboBox<String> cbCreneau;
    @FXML private DatePicker dpDateGarde;
    @FXML private ListView<GardeInfo> listGardes;

    private PompierService service = new PompierService();
    private ObservableList<Pompier> tousLesPompiers = FXCollections.observableArrayList();
    private Pompier pompierSelectionne = null;
    private int gardeIdEnEdition = -1;

    @FXML
    public void initialize() {
        cbCreneau.setItems(FXCollections.observableArrayList(
                "matin", "apres-midi", "nuit", "24h"));
        dpDateGarde.setValue(LocalDate.now());

        chargerPompiers();
        chargerGardesListe();

        // ── Recherche par frappe ──
        tfRecherchePompier.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                listPompiers.setItems(tousLesPompiers);
            } else {
                String filtre = newVal.toLowerCase();
                FilteredList<Pompier> filtered = new FilteredList<>(tousLesPompiers,
                        p -> p.getNom().toLowerCase().contains(filtre)
                                || p.getPrenom().toLowerCase().contains(filtre)
                                || (p.getTelephone() != null && p.getTelephone().contains(filtre)));
                listPompiers.setItems(filtered);
            }
        });

        // ── Sélection d'un pompier ──
        listPompiers.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        pompierSelectionne = newVal;
                        lblPompierSelectionne.setText(
                                "Pompier : " + newVal.getNom() + " " + newVal.getPrenom());
                    }
                });

        // ── CellFactory liste pompiers ──
        listPompiers.setCellFactory(lv -> new ListCell<Pompier>() {
            @Override
            protected void updateItem(Pompier p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) {
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                    return;
                }

                Label lblNom = new Label(p.getNom() + " " + p.getPrenom());
                lblNom.setPrefWidth(200);
                lblNom.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

                Label lblTel = new Label(p.getTelephone() != null ? p.getTelephone() : "-");
                lblTel.setPrefWidth(140);
                lblTel.setStyle("-fx-font-size: 12; -fx-text-fill: #475569;");

                String couleur = switch (p.getStatut() != null ? p.getStatut() : "") {
                    case "disponible" -> "#16a34a";
                    case "en_mission" -> "#E65100";
                    default           -> "#757575";
                };
                Label lblStatut = new Label(
                        p.getStatut() != null ? p.getStatut().replace("_", " ").toUpperCase() : "-");
                lblStatut.setStyle(
                        "-fx-background-color: " + couleur + ";" +
                                "-fx-text-fill: white; -fx-background-radius: 10;" +
                                "-fx-padding: 3 8; -fx-font-size: 10; -fx-font-weight: bold;");

                HBox row = new HBox(0, lblNom, lblTel, lblStatut);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-padding: 10 14;");

                String bgNormal = getIndex() % 2 == 0
                        ? "rgba(255,255,255,0.95)" : "rgba(248,250,252,0.95)";
                setStyle("-fx-background-color: " + bgNormal + ";" +
                        "-fx-border-color: #f1f5f9; -fx-border-width: 0 0 1 0;");
                setOnMouseEntered(e -> setStyle(
                        "-fx-background-color: #f0fdf4;" +
                                "-fx-border-color: #bbf7d0; -fx-border-width: 0 0 1 0;"));
                setOnMouseExited(e -> setStyle(
                        "-fx-background-color: " + bgNormal + ";" +
                                "-fx-border-color: #f1f5f9; -fx-border-width: 0 0 1 0;"));
                setGraphic(row);
            }
        });
    }

    private void chargerPompiers() {
        tousLesPompiers = FXCollections.observableArrayList(service.getData());
        listPompiers.setItems(tousLesPompiers);
    }

    public void chargerGardesListe() {
        List<GardeInfo> gardes = service.getAllGardesInfo();
        ObservableList<GardeInfo> obs = FXCollections.observableArrayList(gardes);

        listGardes.setCellFactory(lv -> new ListCell<GardeInfo>() {
            @Override
            protected void updateItem(GardeInfo g, boolean empty) {
                super.updateItem(g, empty);
                if (empty || g == null) {
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                    return;
                }

                Label lblNom = new Label(g.nomPompier);
                lblNom.setPrefWidth(160);
                lblNom.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

                Label lblTel = new Label(g.telephone != null ? g.telephone : "-");
                lblTel.setPrefWidth(120);
                lblTel.setStyle("-fx-font-size: 12; -fx-text-fill: #475569;");

                String creneauBg = switch (g.creneau != null ? g.creneau : "") {
                    case "matin"      -> "#fef9c3";
                    case "apres-midi" -> "#dbeafe";
                    case "nuit"       -> "#ede9fe";
                    default           -> "#dcfce7";
                };
                String creneauFg = switch (g.creneau != null ? g.creneau : "") {
                    case "matin"      -> "#ca8a04";
                    case "apres-midi" -> "#1d4ed8";
                    case "nuit"       -> "#6d28d9";
                    default           -> "#15803d";
                };
                Label lblCreneau = new Label(g.creneau != null ? g.creneau : "-");
                lblCreneau.setPrefWidth(100);
                lblCreneau.setStyle(
                        "-fx-background-color: " + creneauBg + ";" +
                                "-fx-text-fill: " + creneauFg + ";" +
                                "-fx-font-size: 10; -fx-font-weight: bold;" +
                                "-fx-background-radius: 20; -fx-padding: 3 7;");

                Label lblDate = new Label(g.dateGarde != null ? g.dateGarde : "-");
                lblDate.setPrefWidth(90);
                lblDate.setStyle("-fx-font-size: 11; -fx-text-fill: #64748b;");

                Button btnModifier = new Button("✎");
                btnModifier.setStyle(
                        "-fx-background-color: #ede9fe; -fx-text-fill: #7c3aed;" +
                                "-fx-background-radius: 6; -fx-font-size: 11; -fx-cursor: hand;" +
                                "-fx-min-width: 28; -fx-min-height: 28;");
                btnModifier.setOnAction(e -> {
                    gardeIdEnEdition = g.idGarde;
                    // Sélectionner le pompier dans la liste gauche
                    for (Pompier p : tousLesPompiers)
                        if (p.getId() == g.idPompier) {
                            listPompiers.getSelectionModel().select(p);
                            pompierSelectionne = p;
                            lblPompierSelectionne.setText(
                                    "Pompier : " + p.getNom() + " " + p.getPrenom());
                            break;
                        }
                    cbCreneau.setValue(g.creneau);
                    try { dpDateGarde.setValue(LocalDate.parse(g.dateGarde)); }
                    catch (Exception ex) { dpDateGarde.setValue(LocalDate.now()); }
                    lblTitreForm.setText("Modifier la garde");
                    btnSauvegarder.setText("Mettre a jour");
                });

                Button btnSupprimer = new Button("✖");
                btnSupprimer.setStyle(
                        "-fx-background-color: #fee2e2; -fx-text-fill: #dc2626;" +
                                "-fx-background-radius: 6; -fx-font-size: 11; -fx-cursor: hand;" +
                                "-fx-min-width: 28; -fx-min-height: 28;");
                btnSupprimer.setOnAction(e -> {
                    // ── Dialogue de suppression stylisé ──
                    javafx.stage.Stage dialog = new javafx.stage.Stage();
                    dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
                    dialog.setTitle("Supprimer la garde");
                    dialog.setResizable(false);

                    // En-tête orange
                    Label icone = new Label("🗑");
                    icone.setStyle("-fx-font-size: 28; -fx-text-fill: white;");
                    Label titre = new Label("Supprimer la garde");
                    titre.setStyle("-fx-font-size: 17; -fx-font-weight: bold; -fx-text-fill: white;");
                    Label sousTitre = new Label("Cette action est irréversible");
                    sousTitre.setStyle("-fx-font-size: 11; -fx-text-fill: rgba(255,255,255,0.75);");
                    javafx.scene.layout.VBox texteHeader = new javafx.scene.layout.VBox(3, titre, sousTitre);
                    texteHeader.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    javafx.scene.layout.HBox header = new javafx.scene.layout.HBox(14, icone, texteHeader);
                    header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    header.setStyle("-fx-background-color: linear-gradient(to right, #b45309, #d97706); -fx-padding: 18 24; -fx-background-radius: 16 16 0 0;");

                    // Fiche garde
                    String suppCreneauBg = switch (g.creneau != null ? g.creneau : "") {
                        case "matin" -> "#fef9c3"; case "apres-midi" -> "#dbeafe";
                        case "nuit" -> "#ede9fe"; default -> "#dcfce7";
                    };
                    String suppCreneauFg = switch (g.creneau != null ? g.creneau : "") {
                        case "matin" -> "#ca8a04"; case "apres-midi" -> "#1d4ed8";
                        case "nuit" -> "#6d28d9"; default -> "#15803d";
                    };

                    // Avatar initiales
                    String[] parts = g.nomPompier != null ? g.nomPompier.split(" ", 2) : new String[]{"?", ""};
                    String init = (parts[0].length() > 0 ? parts[0].substring(0,1).toUpperCase() : "?")
                            + (parts.length > 1 && parts[1].length() > 0 ? parts[1].substring(0,1).toUpperCase() : "");
                    Label avatar = new Label(init);
                    avatar.setStyle("-fx-background-color: #D97706; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16; -fx-min-width: 46; -fx-min-height: 46; -fx-background-radius: 50; -fx-alignment: center;");

                    Label lblNomP = new Label(g.nomPompier != null ? g.nomPompier : "-");
                    lblNomP.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #1a1a1a;");
                    Label lblTelP = new Label("📞  " + (g.telephone != null ? g.telephone : "—"));
                    lblTelP.setStyle("-fx-font-size: 12; -fx-text-fill: #475569;");
                    javafx.scene.layout.VBox infoP = new javafx.scene.layout.VBox(3, lblNomP, lblTelP);
                    infoP.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    javafx.scene.layout.HBox.setHgrow(infoP, javafx.scene.layout.Priority.ALWAYS);
                    javafx.scene.layout.HBox ficheTop = new javafx.scene.layout.HBox(12, avatar, infoP);
                    ficheTop.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    ficheTop.setStyle("-fx-padding: 0 0 12 0; -fx-border-color: #e5e7eb; -fx-border-width: 0 0 1 0;");

                    // Détails garde
                    Label lblCreneauBadge = new Label(g.creneau != null ? g.creneau.toUpperCase() : "-");
                    lblCreneauBadge.setStyle("-fx-background-color: " + suppCreneauBg + "; -fx-text-fill: " + suppCreneauFg + "; -fx-background-radius: 8; -fx-padding: 4 12; -fx-font-size: 12; -fx-font-weight: bold;");
                    Label lblDateGarde = new Label("📅  " + (g.dateGarde != null ? g.dateGarde : "—"));
                    lblDateGarde.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
                    javafx.scene.layout.HBox ligneGarde = new javafx.scene.layout.HBox(10, lblCreneauBadge, lblDateGarde);
                    ligneGarde.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    ligneGarde.setStyle("-fx-padding: 12 0 0 0;");

                    javafx.scene.layout.VBox fiche = new javafx.scene.layout.VBox(0, ficheTop, ligneGarde);
                    fiche.setStyle("-fx-background-color: #fffbeb; -fx-background-radius: 12; -fx-border-color: #fcd34d; -fx-border-radius: 12; -fx-border-width: 1; -fx-padding: 14 16;");

                    // Avertissement
                    Label avert = new Label("ℹ  Seule cette garde sera supprimée. Le pompier restera dans le système.");
                    avert.setWrapText(true);
                    avert.setStyle("-fx-font-size: 11; -fx-text-fill: #1e40af; -fx-background-color: #eff6ff; -fx-background-radius: 8; -fx-border-color: #bfdbfe; -fx-border-radius: 8; -fx-border-width: 1; -fx-padding: 10 14;");

                    // Boutons
                    Button btnAnnuler = new Button("Annuler");
                    btnAnnuler.setPrefWidth(110); btnAnnuler.setPrefHeight(38);
                    btnAnnuler.setStyle("-fx-background-color: white; -fx-text-fill: #374151; -fx-font-size: 13; -fx-font-weight: bold; -fx-border-color: #d1d5db; -fx-border-radius: 10; -fx-background-radius: 10; -fx-cursor: hand;");
                    btnAnnuler.setOnAction(ev -> dialog.close());

                    Button btnConfirmer = new Button("🗑  Supprimer la garde");
                    btnConfirmer.setPrefHeight(38);
                    btnConfirmer.setStyle("-fx-background-color: #d97706; -fx-text-fill: white; -fx-font-size: 13; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand; -fx-effect: dropshadow(gaussian,rgba(217,119,6,0.5),10,0,0,3);");
                    btnConfirmer.setOnAction(ev -> {
                        service.deleteGardeById(g.idGarde);
                        chargerGardesListe();
                        dialog.close();
                    });
                    btnConfirmer.setOnMouseEntered(ev -> btnConfirmer.setStyle("-fx-background-color: #b45309; -fx-text-fill: white; -fx-font-size: 13; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand; -fx-effect: dropshadow(gaussian,rgba(180,83,9,0.7),14,0,0,4);"));
                    btnConfirmer.setOnMouseExited(ev -> btnConfirmer.setStyle("-fx-background-color: #d97706; -fx-text-fill: white; -fx-font-size: 13; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand; -fx-effect: dropshadow(gaussian,rgba(217,119,6,0.5),10,0,0,3);"));

                    javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
                    javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
                    javafx.scene.layout.HBox boutons = new javafx.scene.layout.HBox(10, spacer, btnAnnuler, btnConfirmer);
                    boutons.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
                    boutons.setStyle("-fx-padding: 14 0 0 0; -fx-border-color: #e5e7eb; -fx-border-width: 1 0 0 0;");

                    javafx.scene.layout.VBox corps = new javafx.scene.layout.VBox(14, fiche, avert, boutons);
                    corps.setStyle("-fx-padding: 20 24 20 24;");
                    javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(0, header, corps);
                    root.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-effect: dropshadow(gaussian,rgba(0,0,0,0.35),24,0,0,8);");
                    root.setPrefWidth(420);

                    dialog.setScene(new javafx.scene.Scene(root));
                    dialog.show();
                });

                HBox actions = new HBox(6, btnModifier, btnSupprimer);
                actions.setAlignment(Pos.CENTER_LEFT);

                HBox row = new HBox(0, lblNom, lblTel, lblCreneau, lblDate, actions);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-padding: 9 14;");

                String bgNormal = getIndex() % 2 == 0
                        ? "rgba(255,255,255,0.95)" : "rgba(248,250,252,0.95)";
                setStyle("-fx-background-color: " + bgNormal + ";" +
                        "-fx-border-color: #f1f5f9; -fx-border-width: 0 0 1 0;");
                setOnMouseEntered(e -> setStyle(
                        "-fx-background-color: #f0fdf4;" +
                                "-fx-border-color: #bbf7d0; -fx-border-width: 0 0 1 0;"));
                setOnMouseExited(e -> setStyle(
                        "-fx-background-color: " + bgNormal + ";" +
                                "-fx-border-color: #f1f5f9; -fx-border-width: 0 0 1 0;"));
                setGraphic(row);
            }
        });

        listGardes.setItems(obs);
    }

    @FXML
    public void sauvegarderGarde() {
        if (pompierSelectionne == null) {
            new Alert(Alert.AlertType.WARNING,
                    "Veuillez selectionner un pompier dans la liste de gauche.").show();
            return;
        }
        if (cbCreneau.getValue() == null || dpDateGarde.getValue() == null) {
            new Alert(Alert.AlertType.WARNING,
                    "Veuillez choisir un creneau et une date.").show();
            return;
        }

        String creneau = cbCreneau.getValue();
        String date = dpDateGarde.getValue().toString();

        if (gardeIdEnEdition == -1) {
            service.addGarde(pompierSelectionne.getId(), creneau, date);
        } else {
            service.updateGardeById(gardeIdEnEdition, creneau, date);
        }

        annulerEdition();
        chargerGardesListe();
    }

    @FXML
    public void annulerEdition() {
        gardeIdEnEdition = -1;
        pompierSelectionne = null;
        listPompiers.getSelectionModel().clearSelection();
        cbCreneau.setValue(null);
        dpDateGarde.setValue(LocalDate.now());
        lblTitreForm.setText("Assigner une garde");
        lblPompierSelectionne.setText("Aucun pompier selectionne");
        btnSauvegarder.setText("Sauvegarder");
    }

    // ── Classe interne GardeInfo ──
    public static class GardeInfo {
        public int idGarde, idPompier;
        public String nomPompier, telephone, creneau, dateGarde;

        public GardeInfo(int idGarde, int idPompier, String nomPompier,
                         String telephone, String creneau, String dateGarde) {
            this.idGarde    = idGarde;
            this.idPompier  = idPompier;
            this.nomPompier = nomPompier;
            this.telephone  = telephone;
            this.creneau    = creneau;
            this.dateGarde  = dateGarde;
        }
    }
}
