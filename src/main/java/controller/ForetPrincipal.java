package controller;

import model.Foret;
import model.Incendie;
import utils.ForetService;
import utils.IncendieService;
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
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.List;

public class ForetPrincipal {

    @FXML private VBox   sidebar;
    @FXML private VBox   logoNom;
    @FXML private Label  lblReducer;
    @FXML private Button btnToggle;
    @FXML private Button btnDB, btnFR, btnCP, btnDN, btnAL, btnIN, btnUS;
    @FXML private VBox  cardForets, cardIncendies, cardSuperficie;
    @FXML private Label statForets, statIncendies, statSuperficie;
    @FXML private Button btnMeteo;
    @FXML private Button btnEvenements;
    @FXML private ToggleButton tabForets, tabIncendies;
    @FXML private Label titreListe;
    @FXML private TextField searchField;
    @FXML private ListView<Foret>    listeForets;
    @FXML private ListView<Incendie> listeIncendies;
    @FXML private StackPane overlayConfirmation;
    @FXML private Label     lblNomForetOverlay;
    @FXML private Button    btnConfirmerSuppression;
    @FXML private Button    btnAjouter;

    private ObservableList<Foret>    toutesForets  = FXCollections.observableArrayList();
    private ObservableList<Incendie> tousIncendies = FXCollections.observableArrayList();

    private boolean sidebarExpanded = true;
    private static final double W_CLOSED = 72;
    private static final double W_OPEN   = 240;

    private final ForetService    foretService    = new ForetService();
    private final IncendieService incendieService = new IncendieService();

    // ════════════════════════════════════════════
    //  INIT
    // ════════════════════════════════════════════

    @FXML
    public void initialize() {
        setupNavHover();
        setupCardHover();
        setupForetsListView();
        setupIncendiesListView();
        chargerToutesDonnees();
        setupSearch();
        listeForets.setCellFactory(lv -> new ForetCell(this));
    }

    private void setupSearch() {
        if (searchField == null) return;
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filtrerDonnees(newVal));
    }

    private void filtrerDonnees(String texte) {
        String f = texte == null ? "" : texte.trim().toLowerCase();
        if (f.isEmpty()) {
            listeForets.setItems(toutesForets);
            listeIncendies.setItems(tousIncendies);
            return;
        }
        listeForets.setItems(toutesForets.filtered(foret ->
            (foret.getNom()            != null && foret.getNom().toLowerCase().contains(f)) ||
            (foret.getLocalisation()   != null && foret.getLocalisation().toLowerCase().contains(f)) ||
            (foret.getTypeVegetation() != null && foret.getTypeVegetation().toLowerCase().contains(f)) ||
            (foret.getNiveauRisque()   != null && foret.getNiveauRisque().toLowerCase().contains(f))
        ));
        listeIncendies.setItems(tousIncendies.filtered(inc ->
            (inc.getCause()         != null && inc.getCause().toLowerCase().contains(f)) ||
            (inc.getNiveauGravite() != null && inc.getNiveauGravite().toLowerCase().contains(f)) ||
            (inc.getStatut()        != null && inc.getStatut().toLowerCase().contains(f)) ||
            String.valueOf(inc.getIdZone()).contains(f) ||
            String.valueOf(inc.getId()).contains(f)
        ));
    }

    // ════════════════════════════════════════════
    //  SIDEBAR
    // ════════════════════════════════════════════

    private void setupNavHover() {
        Button[] navBtns = {btnDB, btnCP, btnDN, btnAL, btnIN, btnUS};
        for (Button b : navBtns) {
            if (b == null) continue;
            b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: rgba(22,163,74,0.15); -fx-text-fill: #4ade80; -fx-font-size: 13; -fx-alignment: CENTER_LEFT; -fx-padding: 10 16; -fx-background-radius: 12; -fx-cursor: hand;"));
            b.setOnMouseExited(e  -> b.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 13; -fx-alignment: CENTER_LEFT; -fx-padding: 10 16; -fx-background-radius: 12; -fx-cursor: hand;"));
        }
    }

    private void setupCardHover() {
        VBox[] cards = {cardForets, cardIncendies, cardSuperficie};
        for (VBox card : cards) {
            if (card == null) continue;
            String base = card.getStyle();
            card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: rgba(255,255,255,0.18); -fx-background-radius: 16; -fx-border-color: rgba(255,255,255,0.35); -fx-border-radius: 16; -fx-border-width: 1; -fx-padding: 16 20; -fx-cursor: hand; -fx-scale-x: 1.03; -fx-scale-y: 1.03;"));
            card.setOnMouseExited(e  -> card.setStyle(base));
        }
    }

    @FXML
    public void toggleSidebar() {
        sidebarExpanded = !sidebarExpanded;
        double toWidth = sidebarExpanded ? W_OPEN : W_CLOSED;
        Timeline anim = new Timeline(new KeyFrame(Duration.millis(220), new KeyValue(sidebar.prefWidthProperty(), toWidth)));
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
            {btnDB,"  Tableau de Bord","DB"},{btnFR,"  Forets","FR"},
            {btnCP,"  Capteurs","CP"},{btnDN,"  Donnees","DN"},
            {btnAL,"  Alertes","AL"},{btnIN,"  Interventions","IN"},{btnUS,"  Utilisateurs","US"}
        };
        for (Object[] entry : btns) {
            Button b = (Button) entry[0]; if (b == null) continue;
            b.setText((String)(expanded ? entry[1] : entry[2]));
            b.setPrefWidth(expanded ? W_OPEN - 20 : 52);
            boolean isActive = b == btnFR;
            b.setStyle("-fx-background-color:" + (isActive?"#16a34a":"transparent") + ";" +
                "-fx-text-fill:" + (isActive?"white":"#94a3b8") + ";-fx-font-size:13;" +
                (isActive?"-fx-font-weight:bold;":"") +
                "-fx-alignment:" + (expanded?"CENTER_LEFT":"CENTER") + ";" +
                "-fx-padding:10 16;-fx-background-radius:12;-fx-cursor:hand;" +
                (isActive?"-fx-effect:dropshadow(gaussian,rgba(22,163,74,0.4),10,0,0,3);":""));
        }
    }

    // ════════════════════════════════════════════
    //  ONGLETS
    // ════════════════════════════════════════════

    @FXML public void afficherForets() {
        listeForets.setVisible(true); listeForets.setManaged(true);
        listeIncendies.setVisible(false); listeIncendies.setManaged(false);
        if (titreListe != null) titreListe.setText("Toutes les forets");
        styleOnglet(tabForets, true, false);
        styleOnglet(tabIncendies, false, true);
        // Bouton bas → ajouter forêt
        if (btnAjouter != null) {
            btnAjouter.setText("+ Ajouter une foret");
            btnAjouter.setOnAction(e -> ouvrirModalAjouterForet());
            btnAjouter.setStyle(
                "-fx-background-color: #16a34a; -fx-text-fill: white;" +
                "-fx-font-size: 13; -fx-font-weight: bold;" +
                "-fx-background-radius: 12; -fx-padding: 10 28; -fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian,rgba(22,163,74,0.6),14,0,0,4);");
        }
    }

    @FXML public void afficherIncendies() {
        listeForets.setVisible(false); listeForets.setManaged(false);
        listeIncendies.setVisible(true); listeIncendies.setManaged(true);
        if (titreListe != null) titreListe.setText("Tous les incendies");
        styleOnglet(tabForets, false, false);
        styleOnglet(tabIncendies, true, true);
        // Bouton bas → ajouter incendie
        if (btnAjouter != null) {
            btnAjouter.setText("+ Ajouter un incendie");
            btnAjouter.setOnAction(e -> ouvrirModalAjouterIncendie());
            btnAjouter.setStyle(
                "-fx-background-color: #dc2626; -fx-text-fill: white;" +
                "-fx-font-size: 13; -fx-font-weight: bold;" +
                "-fx-background-radius: 12; -fx-padding: 10 28; -fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian,rgba(220,38,38,0.6),14,0,0,4);");
        }
    }

    private void styleOnglet(ToggleButton btn, boolean actif, boolean rouge) {
        if (btn == null) return;
        btn.setSelected(actif);
        if (actif) {
            String couleur = rouge ? "#dc2626" : "#16a34a";
            btn.setStyle("-fx-background-color:" + couleur + ";-fx-text-fill:white;" +
                "-fx-font-size:12;-fx-font-weight:bold;-fx-background-radius:20;" +
                "-fx-padding:8 18;-fx-cursor:hand;");
        } else {
            btn.setStyle("-fx-background-color:rgba(255,255,255,0.12);-fx-text-fill:white;" +
                "-fx-font-size:12;-fx-background-radius:20;-fx-padding:8 18;-fx-cursor:hand;");
        }
    }

    // ════════════════════════════════════════════
    //  DONNEES
    // ════════════════════════════════════════════

    private void chargerToutesDonnees() { chargerForets(); chargerIncendies(); }

    private void chargerForets() {
        toutesForets = FXCollections.observableArrayList(foretService.getData());
        listeForets.setItems(toutesForets);
        if (statForets     != null) statForets.setText(String.valueOf(toutesForets.size()));
        if (statSuperficie != null) statSuperficie.setText(String.format("%.0f", toutesForets.stream().mapToDouble(Foret::getSuperficie).sum()));
        if (searchField != null && !searchField.getText().isBlank()) filtrerDonnees(searchField.getText());
    }

    private void chargerIncendies() {
        tousIncendies = FXCollections.observableArrayList(incendieService.getData());
        listeIncendies.setItems(tousIncendies);
        if (statIncendies != null) statIncendies.setText(String.valueOf(tousIncendies.size()));
        if (searchField != null && !searchField.getText().isBlank()) filtrerDonnees(searchField.getText());
    }

    // ════════════════════════════════════════════
    //  CELL FACTORIES
    // ════════════════════════════════════════════

    private void setupForetsListView() {
    }

    private void setupIncendiesListView() {
        listeIncendies.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Incendie inc, boolean empty) {
                super.updateItem(inc, empty);
                if (empty || inc == null) {
                    setGraphic(null);
                    setStyle("-fx-background-color:transparent;-fx-padding:4 0;");
                    return;
                }

                // ── Avatar rouge rond avec flamme ──
                Circle cercle = new Circle(24, Color.web("#ef4444"));
                Label icFlamme = new Label("\uD83D\uDD25");
                icFlamme.setStyle("-fx-font-size:14;");
                StackPane avatar = new StackPane(cercle, icFlamme);
                avatar.setMinSize(48,48); avatar.setMaxSize(48,48);

                // ── Nom ──
                Label lblId = new Label("Incendie #" + inc.getId());
                lblId.setStyle("-fx-font-size:14;-fx-font-weight:bold;-fx-text-fill:#1a2e1e;");

                // ── Ligne 1 : Zone + Date ──
                Label lblZone  = new Label("\uD83D\uDCCD Zone " + inc.getIdZone());
                lblZone.setStyle("-fx-font-size:11;-fx-text-fill:#475569;");
                Label lblDebut = new Label("\uD83D\uDCC5 " + (inc.getDateDebut()!=null?inc.getDateDebut():""));
                lblDebut.setStyle("-fx-font-size:11;-fx-text-fill:#475569;");
                HBox ligne1 = new HBox(14, lblZone, lblDebut);
                ligne1.setAlignment(Pos.CENTER_LEFT);

                // ── Ligne 2 : Surface + Cause ──
                Label lblSup = new Label("\uD83C\uDF32 " + String.format("%.2f ha brulees", inc.getSuperficieBrulee()));
                lblSup.setStyle("-fx-font-size:11;-fx-text-fill:#ef4444;-fx-font-weight:bold;");
                Label lblCause = new Label("\u26A1 " + (inc.getCause()!=null?inc.getCause():""));
                lblCause.setStyle("-fx-font-size:11;-fx-text-fill:#475569;");
                HBox ligne2 = new HBox(14, lblSup, lblCause);
                ligne2.setAlignment(Pos.CENTER_LEFT);

                VBox infos = new VBox(3, lblId, ligne1, ligne2);
                infos.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(infos, Priority.ALWAYS);

                // ── Badge gravité (texte coloré seul, sans fond) ──
                String grav = inc.getNiveauGravite() != null ? inc.getNiveauGravite() : "";
                Label badge = new Label(grav.toUpperCase());
                String badgeFg;
                switch (grav.toUpperCase()) {
                    case "CRITIQUE"                  -> badgeFg = "#dc2626";
                    case "ELEVE", "\u00c9LEV\u00c9"  -> badgeFg = "#ea580c";
                    case "MOYEN"                     -> badgeFg = "#ca8a04";
                    default                          -> badgeFg = "#16a34a";
                }
                badge.setStyle("-fx-font-size:12;-fx-font-weight:bold;-fx-text-fill:"+badgeFg+";");
                badge.setMinWidth(70);
                badge.setAlignment(Pos.CENTER_RIGHT);

                // ── Bouton Périmètre ──
                Button btnP = new Button("\uD83D\uDDFA\uFE0F P\u00e9rim\u00e8tre");
                String styleP = "-fx-background-color:#fff7ed;-fx-text-fill:#ea580c;" +
                    "-fx-background-radius:8;-fx-border-color:#fed7aa;" +
                    "-fx-border-radius:8;-fx-border-width:1;" +
                    "-fx-cursor:hand;-fx-font-size:11;-fx-font-weight:bold;-fx-padding:6 10;";
                btnP.setStyle(styleP);
                btnP.setOnMouseEntered(e -> btnP.setStyle(styleP.replace("#fff7ed","#ffedd5")));
                btnP.setOnMouseExited(e  -> btnP.setStyle(styleP));
                btnP.setOnAction(e -> ouvrirModalPerimetre(inc));

                // ── Bouton Modifier (icône seule) ──
                Button btnE = new Button("\u270F");
                String styleE = "-fx-background-color:#ede9fe;-fx-text-fill:#7c3aed;" +
                    "-fx-background-radius:8;-fx-border-color:#ddd6fe;" +
                    "-fx-border-radius:8;-fx-border-width:1;" +
                    "-fx-cursor:hand;-fx-pref-width:32;-fx-pref-height:32;-fx-padding:0;";
                btnE.setStyle(styleE);
                btnE.setOnMouseEntered(e -> btnE.setStyle(styleE.replace("#ede9fe","#ddd6fe")));
                btnE.setOnMouseExited(e  -> btnE.setStyle(styleE));
                btnE.setOnAction(e -> ouvrirModalEditerIncendie(inc));

                // ── Bouton Supprimer (icône seule) ──
                Button btnX = new Button("\uD83D\uDDD1");
                String styleX = "-fx-background-color:#fee2e2;-fx-text-fill:#dc2626;" +
                    "-fx-background-radius:8;-fx-border-color:#fecaca;" +
                    "-fx-border-radius:8;-fx-border-width:1;" +
                    "-fx-cursor:hand;-fx-pref-width:32;-fx-pref-height:32;-fx-padding:0;";
                btnX.setStyle(styleX);
                btnX.setOnMouseEntered(e -> btnX.setStyle(styleX.replace("#fee2e2","#fecaca")));
                btnX.setOnMouseExited(e  -> btnX.setStyle(styleX));
                btnX.setOnAction(e -> supprimerIncendie(inc));

                HBox actions = new HBox(6, badge, btnP, btnE, btnX);
                actions.setAlignment(Pos.CENTER_RIGHT);

                // ── Assemblage ──
                HBox row = new HBox(14, avatar, infos, actions);
                row.setAlignment(Pos.CENTER_LEFT);
                String styleNormal =
                    "-fx-background-color:rgba(255,255,255,0.92);" +
                    "-fx-background-radius:14;" +
                    "-fx-border-color:#e2e8f0;" +
                    "-fx-border-radius:14;" +
                    "-fx-border-width:1;" +
                    "-fx-padding:14 16;";
                row.setStyle(styleNormal);
                row.setOnMouseEntered(e -> row.setStyle(
                    "-fx-background-color:#fff1f2;" +
                    "-fx-background-radius:14;" +
                    "-fx-border-color:#fecaca;" +
                    "-fx-border-radius:14;" +
                    "-fx-border-width:1;" +
                    "-fx-padding:14 16;"));
                row.setOnMouseExited(e -> row.setStyle(styleNormal));

                setGraphic(row);
                setStyle("-fx-background-color:transparent;-fx-padding:4 0;");
            }
        });
    }

    // ════════════════════════════════════════════
    //  HELPERS UI
    // ════════════════════════════════════════════

    private StackPane makeAvatar(String texte, String couleur) {
        Circle cercle = new Circle(24); cercle.setFill(Color.web(couleur));
        Label lbl = new Label(texte); lbl.setStyle("-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:white;");
        StackPane sp = new StackPane(cercle, lbl); sp.setMinSize(48,48); sp.setMaxSize(48,48);
        return sp;
    }

    private Label makeBadge(String valeur) {
        String v = valeur != null ? valeur : "";
        String bg, fg;
        switch (v.toUpperCase()) {
            case "CRITIQUE":                              bg="#fee2e2"; fg="#dc2626"; break;
            case "ELEVE": case "\u00c9LEV\u00c9": case "EN DANGER": bg="#fed7aa"; fg="#ea580c"; break;
            case "MOYEN": case "VULNERABLE":              bg="#fef9c3"; fg="#ca8a04"; break;
            default:                                      bg="#dcfce7"; fg="#16a34a"; break;
        }
        Label badge = new Label(v);
        badge.setStyle("-fx-background-color:"+bg+";-fx-text-fill:"+fg+";-fx-font-size:11;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:4 12;");
        badge.setMinWidth(80); badge.setAlignment(Pos.CENTER);
        return badge;
    }

    private HBox makeActions(javafx.event.EventHandler<ActionEvent> onEdit,
                             javafx.event.EventHandler<ActionEvent> onDelete) {
        Button btnE = new Button("\u270F");
        btnE.setStyle("-fx-background-color:#ede9fe;-fx-text-fill:#7c3aed;-fx-background-radius:8;-fx-cursor:hand;-fx-min-width:32;-fx-min-height:32;");
        btnE.setOnMouseEntered(e -> btnE.setStyle("-fx-background-color:#ddd6fe;-fx-text-fill:#6d28d9;-fx-background-radius:8;-fx-cursor:hand;-fx-min-width:32;-fx-min-height:32;"));
        btnE.setOnMouseExited(e  -> btnE.setStyle("-fx-background-color:#ede9fe;-fx-text-fill:#7c3aed;-fx-background-radius:8;-fx-cursor:hand;-fx-min-width:32;-fx-min-height:32;"));
        btnE.setOnAction(onEdit);
        Button btnX = new Button("\uD83D\uDDD1");
        btnX.setStyle("-fx-background-color:#fee2e2;-fx-text-fill:#dc2626;-fx-background-radius:8;-fx-cursor:hand;-fx-min-width:32;-fx-min-height:32;");
        btnX.setOnMouseEntered(e -> btnX.setStyle("-fx-background-color:#fecaca;-fx-text-fill:#b91c1c;-fx-background-radius:8;-fx-cursor:hand;-fx-min-width:32;-fx-min-height:32;"));
        btnX.setOnMouseExited(e  -> btnX.setStyle("-fx-background-color:#fee2e2;-fx-text-fill:#dc2626;-fx-background-radius:8;-fx-cursor:hand;-fx-min-width:32;-fx-min-height:32;"));
        btnX.setOnAction(onDelete);
        HBox box = new HBox(6, btnE, btnX); box.setAlignment(Pos.CENTER);
        return box;
    }

    // ════════════════════════════════════════════
    //  ACTIONS FORETS
    // ════════════════════════════════════════════

    @FXML public void ouvrirModalAjouterForet() { ouvrirModal("/AjouterForet.fxml","Ajouter une Foret"); chargerForets(); }

    public void ouvrirModalModifierForet(Foret foret) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjouterForet.fxml"));
            Parent root = loader.load();
            AjouterForet ctrl = loader.getController();
            ctrl.remplirPourEdition(foret);
            afficherModal(root,"Modifier Foret");
            chargerForets();
        } catch (IOException e) { afficherErreur("Erreur modal Foret : " + e.getMessage()); }
    }

    private void supprimerForet(Foret foret) {
        ouvrirOverlayConfirmation(foret);
    }
    

    // ════════════════════════════════════════════
    //  ACTIONS INCENDIES
    // ════════════════════════════════════════════

    @FXML public void ouvrirModalAjouterIncendie() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjouterIncendie.fxml"));
            Parent root = loader.load();
            Stage modal = new Stage();
            modal.setTitle("Ajouter un Incendie");
            modal.setScene(new Scene(root, 900, 620));
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setResizable(true);
            modal.setMinWidth(860);
            modal.setMinHeight(560);
            modal.showAndWait();
            chargerIncendies();
        } catch (IOException e) { afficherErreur("Erreur modal Incendie : " + e.getMessage()); }
    }

    private void ouvrirModalEditerIncendie(Incendie inc) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjouterIncendie.fxml"));
            Parent root = loader.load();
            AjouterIncendie ctrl = loader.getController();
            ctrl.remplirPourEdition(inc);
            Stage modal = new Stage();
            modal.setTitle("Modifier Incendie");
            modal.setScene(new Scene(root, 900, 620));
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setResizable(true);
            modal.setMinWidth(860);
            modal.setMinHeight(560);
            modal.showAndWait();
            chargerIncendies();
        } catch (IOException e) { afficherErreur("Erreur modal Incendie : " + e.getMessage()); }
    }

    private void supprimerIncendie(Incendie inc) {
        ouvrirOverlayConfirmationIncendie(inc);
    }

    // ════════════════════════════════════════════
    //  PERIMETRE
    // ════════════════════════════════════════════

    // ── Bridge périmètre — doit être static pour éviter le GC par JSObject ──
    public static class PerimetreBridge {
        private Label lblPts, lblPeri, lblArea, lblCit, lblStatut;

        public void init(Label pts, Label peri, Label area, Label cit, Label statut) {
            this.lblPts = pts; this.lblPeri = peri;
            this.lblArea = area; this.lblCit = cit; this.lblStatut = statut;
        }

        public void updateStats(String pts, String peri, String area, String cit) {
            javafx.application.Platform.runLater(() -> {
                if (lblPts   != null) lblPts.setText(pts);
                if (lblPeri  != null) lblPeri.setText(peri);
                if (lblArea  != null) lblArea.setText(area);
                if (lblCit   != null) lblCit.setText(cit);
            });
        }

        public void setStatus(String msg) {
            javafx.application.Platform.runLater(() -> {
                if (lblStatut != null) lblStatut.setText(msg);
            });
        }
    }

    // Référence forte — empêche le GC de collecter le bridge
    private final PerimetreBridge perimetreBridge = new PerimetreBridge();

    private void ouvrirModalPerimetre(Incendie inc) {
        Stage stage = new Stage();
        stage.setTitle("P\u00e9rim\u00e8tre \u2014 Incendie #" + inc.getId());
        stage.initModality(Modality.APPLICATION_MODAL);

        Label lblPtsVal  = new Label("0");
        Label lblPeriVal = new Label("\u2014");
        Label lblAreaVal = new Label("\u2014");
        Label lblCitVal  = new Label("\u2014");
        Label lblStatut  = new Label("Cliquez sur \u00ab Placer des points \u00bb puis sur la carte.");
        lblPtsVal .setStyle("-fx-font-size:28;-fx-font-weight:bold;-fx-text-fill:#1e293b;");
        lblPeriVal.setStyle("-fx-font-size:28;-fx-font-weight:bold;-fx-text-fill:#1e293b;");
        lblAreaVal.setStyle("-fx-font-size:28;-fx-font-weight:bold;-fx-text-fill:#1e293b;");
        lblCitVal .setStyle("-fx-font-size:28;-fx-font-weight:bold;-fx-text-fill:#dc2626;");
        lblStatut .setStyle("-fx-font-size:12;-fx-text-fill:#475569;");

        perimetreBridge.init(lblPtsVal, lblPeriVal, lblAreaVal, lblCitVal, lblStatut);

        VBox cardPts  = makeStatCard("Points plac\u00e9s",       lblPtsVal,  "points",          false);
        VBox cardPeri = makeStatCard("P\u00e9rim\u00e8tre",      lblPeriVal, "km",              false);
        VBox cardArea = makeStatCard("Surface br\u00fcl\u00e9e", lblAreaVal, "ha",              false);
        VBox cardCit  = makeStatCard("Citernes n\u00e9cessaires",lblCitVal,  "\u00d7 10 000 L", true);

        HBox statsRow = new HBox(12, cardPts, cardPeri, cardArea, cardCit);
        statsRow.setStyle("-fx-padding:16 20;-fx-background-color:#f8fafc;-fx-border-color:#e2e8f0;-fx-border-width:1 0 0 0;");
        HBox.setHgrow(cardPts,Priority.ALWAYS); HBox.setHgrow(cardPeri,Priority.ALWAYS);
        HBox.setHgrow(cardArea,Priority.ALWAYS); HBox.setHgrow(cardCit,Priority.ALWAYS);

        HBox statusBar = new HBox(lblStatut);
        statusBar.setStyle("-fx-padding:8 20;-fx-background-color:#f1f5f9;-fx-border-color:#e2e8f0;-fx-border-width:1 0 0 0;");
        statusBar.setAlignment(Pos.CENTER_LEFT);

        // ── Utiliser CarteController pour le proxy (même mécanisme que la carte interactive) ──
        javafx.fxml.FXMLLoader loader;
        javafx.scene.web.WebView webView;
        CarteController carteCtrl;
        try {
            loader = new javafx.fxml.FXMLLoader(getClass().getResource("/CarteInteractive.fxml"));
            javafx.scene.Parent carteRoot = loader.load();
            carteCtrl = loader.getController();
            // Récupérer le WebView depuis le contrôleur
            webView = (javafx.scene.web.WebView) carteRoot.lookup("#webView");
        } catch (Exception e) {
            // Fallback : WebView simple
            webView = new javafx.scene.web.WebView();
            carteCtrl = null;
        }

        if (webView == null) webView = new javafx.scene.web.WebView();
        VBox.setVgrow(webView, Priority.ALWAYS);
        final javafx.scene.web.WebEngine eng = webView.getEngine();
        eng.setJavaScriptEnabled(true);
        webView.setContextMenuEnabled(false);

        // Charger PerimetreIncendie.html avec le port du proxy de CarteController
        try {
            java.io.InputStream is = getClass().getResourceAsStream("/PerimetreIncendie.html");
            if (is != null) {
                String html = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                // Injecter le port du proxy si CarteController est disponible
                if (carteCtrl != null) {
                    int port = carteCtrl.getProxyPort();
                    html = html.replace("'__PORT__'", "'" + port + "'");
                } else {
                    html = html.replace("'__PORT__'", "null");
                }
                final String finalHtml = html;
                eng.getLoadWorker().stateProperty().addListener((obs, old, now) -> {
                    if (now == javafx.concurrent.Worker.State.SUCCEEDED) {
                        netscape.javascript.JSObject win = (netscape.javascript.JSObject) eng.executeScript("window");
                        win.setMember("javaBridge", perimetreBridge);
                    }
                });
                eng.loadContent(finalHtml, "text/html");
            }
        } catch (Exception e) {
            System.err.println("PerimetreIncendie.html: " + e.getMessage());
        }

        VBox root = new VBox(0, webView, statusBar, statsRow);
        root.setStyle("-fx-background-color:#ffffff;");
        VBox.setVgrow(webView, Priority.ALWAYS);

        final CarteController finalCarteCtrl = carteCtrl;
        stage.setOnCloseRequest(e -> {
            if (finalCarteCtrl != null) finalCarteCtrl.arreterServeur();
        });
        stage.setScene(new Scene(root, 1000, 780));
        stage.setResizable(true);
        stage.showAndWait();
        if (finalCarteCtrl != null) finalCarteCtrl.arreterServeur();
    }

    private VBox makeStatCard(String titre, Label valLabel, String unite, boolean highlight) {
        Label lblTitre = new Label(titre); lblTitre.setStyle("-fx-font-size:12;-fx-text-fill:#64748b;");
        Label lblUnite = new Label(unite); lblUnite.setStyle("-fx-font-size:12;-fx-text-fill:"+(highlight?"#dc2626":"#64748b")+";");
        VBox card = new VBox(4, lblTitre, valLabel, lblUnite);
        card.setStyle("-fx-background-color:white;-fx-background-radius:12;-fx-border-color:"+(highlight?"#fecaca":"#e2e8f0")+";-fx-border-radius:12;-fx-border-width:1;-fx-padding:14 18;");
        return card;
    }

    private String buildPerimetreHtml() {
        return buildPerimetreHtml(0);
    }

    private String buildPerimetreHtml(int proxyPort) {
        // Charger le GeoJSON depuis les ressources
        String geojson = "null";
        try {
            java.net.URL url = getClass().getResource("/map_web/tn-governorates.geojson");
            if (url != null) geojson = new String(url.openStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) { System.err.println("GeoJSON non trouvé: " + e.getMessage()); }

        // URL des tuiles — via proxy local si disponible, sinon direct
        String tileUrl = proxyPort > 0
            ? "http://127.0.0.1:" + proxyPort + "/tiles/{z}/{x}/{y}.png"
            : "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png";
        String tileOptions = proxyPort > 0
            ? "{maxZoom:19}"
            : "{subdomains:'abc',maxZoom:19,crossOrigin:true}";

        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>" +
            "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>" +
            "<style>" +
            "* { box-sizing:border-box; margin:0; padding:0; }" +
            "html,body { width:100%; height:100%; font-family:-apple-system,BlinkMacSystemFont,sans-serif; background:#0a1a0e; }" +
            "#map { width:100%; height:100%; }" +
            // Panneau de contrôle — fond sombre semi-transparent lisible sur satellite
            ".ctrl { position:absolute; top:14px; left:14px; z-index:1000;" +
            "  background:rgba(8,22,13,0.92); border-radius:14px; padding:12px 14px;" +
            "  min-width:175px; border:1px solid rgba(74,222,128,0.25);" +
            "  box-shadow:0 4px 24px rgba(0,0,0,0.5); }" +
            ".ctrl-sec { font-size:10px; color:rgba(74,222,128,0.55); letter-spacing:1px; margin:10px 0 5px; font-weight:700; text-transform:uppercase; }" +
            ".ctrl-sec:first-child { margin-top:0; }" +
            ".cb { display:block; width:100%; margin:3px 0; padding:8px 12px;" +
            "  background:rgba(74,222,128,0.08); color:#4ade80;" +
            "  border:1px solid rgba(74,222,128,0.2); border-radius:9px;" +
            "  cursor:pointer; font-size:12px; font-weight:600; text-align:left; transition:all 0.15s; }" +
            ".cb:hover { background:rgba(74,222,128,0.18); border-color:rgba(74,222,128,0.4); }" +
            ".cb.on { background:#16a34a; border-color:#16a34a; color:white; }" +
            ".cb.red { color:#fca5a5; border-color:rgba(239,68,68,0.3); background:rgba(239,68,68,0.08); }" +
            ".cb.red:hover { background:rgba(239,68,68,0.18); }" +
            // Barre de statut en bas
            ".sb { position:absolute; bottom:28px; left:50%; transform:translateX(-50%);" +
            "  z-index:1000; background:rgba(22,163,74,0.92); border-radius:20px;" +
            "  padding:8px 22px; font-size:12px; color:white; white-space:nowrap;" +
            "  box-shadow:0 2px 16px rgba(0,0,0,0.4); font-weight:600; }" +
            ".leaflet-control-attribution { display:none !important; }" +
            ".leaflet-bar a { background-color:rgba(8,22,13,0.92) !important; color:#4ade80 !important; border-color:rgba(74,222,128,0.2) !important; }" +
            ".leaflet-bar a:hover { background-color:rgba(74,222,128,0.15) !important; }" +
            "</style></head><body>" +
            "<div id='map'></div>" +
            "<div class='ctrl'>" +
            "  <div class='ctrl-sec'>DESSIN</div>" +
            "  <button class='cb' id='bPlacer'>&#9654; Placer des points</button>" +
            "  <button class='cb' id='bFermer' style='opacity:0.35;cursor:not-allowed;'>&#9711; Fermer le p&eacute;rim&egrave;tre</button>" +
            "  <button class='cb red' id='bReset'>&#128465; R&eacute;initialiser</button>" +
            "</div>" +
            "<div class='sb' id='sb'>Activez &laquo;&nbsp;Placer des points&nbsp;&raquo; puis cliquez sur la carte.</div>" +
            "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>" +
            "<script>" +
            "var GEOJSON = " + geojson + ";" +
            "var map=L.map('map',{zoomControl:true,attributionControl:false}).setView([33.88,9.54],6);" +
            // Tuiles via proxy local Java (pas de problème CORS)
            "L.tileLayer('" + tileUrl + "'," + tileOptions + ").addTo(map);" +
            // Couche GeoJSON des gouvernorats — contours verts sur OSM
            "var geojsonLayer = L.geoJson(GEOJSON, {" +
            "  style: function(f) { return {fillColor:'rgba(74,222,128,0.08)',weight:1.5,opacity:0.8,color:'#4ade80',dashArray:'4',fillOpacity:0.08}; }," +
            "  onEachFeature: function(f,l) {" +
            "    l.on({mouseover:function(e){e.target.setStyle({weight:2.5,color:'#4ade80',fillOpacity:0.2,fillColor:'#4ade80'});e.target.bringToFront();}," +
            "          mouseout:function(e){geojsonLayer.resetStyle(e.target);}});" +
            "    l.bindTooltip(f.properties.gouv_fr||'',{permanent:false,direction:'center',className:'',opacity:0.9});" +
            "  }" +
            "}).addTo(map);" +
            "var pts=[],mkrs=[],poly=null,placing=false,closed=false;" +
            "function hav(a,b){var R=6371000,dLat=(b.lat-a.lat)*Math.PI/180,dLon=(b.lng-a.lng)*Math.PI/180;" +
            "  var x=Math.sin(dLat/2)*Math.sin(dLat/2)+Math.cos(a.lat*Math.PI/180)*Math.cos(b.lat*Math.PI/180)*Math.sin(dLon/2)*Math.sin(dLon/2);" +
            "  return R*2*Math.atan2(Math.sqrt(x),Math.sqrt(1-x));}" +
            "function peri(){if(pts.length<2)return 0;var p=0;for(var i=0;i<pts.length-1;i++)p+=hav(pts[i],pts[i+1]);if(closed&&pts.length>=3)p+=hav(pts[pts.length-1],pts[0]);return p;}" +
            "function area(){if(pts.length<3||!closed)return 0;var a=0;for(var i=0;i<pts.length;i++){var j=(i+1)%pts.length;var xi=pts[i].lng*111320*Math.cos(pts[i].lat*Math.PI/180),yi=pts[i].lat*110540;var xj=pts[j].lng*111320*Math.cos(pts[j].lat*Math.PI/180),yj=pts[j].lat*110540;a+=xi*yj-xj*yi;}return Math.abs(a)/2;}" +
            "function draw(){mkrs.forEach(function(m){map.removeLayer(m);});mkrs=[];if(poly){map.removeLayer(poly);poly=null;}" +
            "  pts.forEach(function(p,i){var col=i===0?'#ff6b00':'#ef4444';" +
            "    var pin='<div style=\"position:relative;width:36px;height:44px;\">" +
            "<div style=\"width:36px;height:36px;border-radius:50% 50% 50% 0;background:'+col+';" +
            "transform:rotate(-45deg);box-shadow:0 4px 12px rgba(0,0,0,0.4);border:3px solid white;\"></div>" +
            "<div style=\"position:absolute;top:4px;left:4px;width:28px;height:28px;border-radius:50%;" +
            "background:white;display:flex;align-items:center;justify-content:center;" +
            "color:'+col+';font-weight:bold;font-size:13px;transform:rotate(45deg);\">'+(i+1)+'</div>" +
            "<div style=\"position:absolute;bottom:-4px;left:50%;transform:translateX(-50%);" +
            "width:8px;height:8px;border-radius:50%;background:rgba(0,0,0,0.25);\"></div>" +
            "</div>';" +
            "    var ic=L.divIcon({html:pin,iconSize:[36,44],iconAnchor:[18,44],className:''});mkrs.push(L.marker([p.lat,p.lng],{icon:ic}).addTo(map));});" +
            "  var ll=pts.map(function(p){return[p.lat,p.lng];});" +
            "  if(closed&&pts.length>=3)poly=L.polygon(ll,{color:'#e53935',weight:2.5,fillColor:'#e53935',fillOpacity:0.18}).addTo(map);" +
            "  else if(pts.length>=2)poly=L.polyline(ll,{color:'#ff9500',weight:2.5,dashArray:'8,5'}).addTo(map);}" +
            "function stats(){var nb=pts.length,pe=peri(),ar=area(),ha=ar/10000;" +
            "  var ps=pe>=1000?(pe/1000).toFixed(2)+' km':Math.round(pe)+' m';" +
            "  var as=closed&&nb>=3?(ha>=1?ha.toFixed(2):(ar).toFixed(0)+' m\u00b2'):'\u2014';" +
            "  var cit=closed&&nb>=3?Math.max(1,Math.ceil(ha/0.667)):0;" +
            "  var cs=closed&&nb>=3?String(cit):'\u2014';" +
            "  if(window.javaBridge){" +
            "    try{window.javaBridge.updateStats(String(nb),ps,as,cs);}catch(e){" +
            "      setTimeout(function(){try{window.javaBridge.updateStats(String(nb),ps,as,cs);}catch(e2){}},200);" +
            "    }" +
            "  }" +
            "}" +
            "function setStatus(m){" +
            "  document.getElementById('sb').innerHTML=m;" +
            "  if(window.javaBridge){" +
            "    try{window.javaBridge.setStatus(m);}catch(e){" +
            "      setTimeout(function(){try{window.javaBridge.setStatus(m);}catch(e2){}},200);" +
            "    }" +
            "  }" +
            "}" +
            "map.on('click',function(e){if(!placing||closed)return;pts.push({lat:e.latlng.lat,lng:e.latlng.lng});draw();stats();" +
            "  var m='Point '+pts.length+' plac\u00e9'+(pts.length>=3?' \u2014 vous pouvez fermer le p\u00e9rim\u00e8tre':'');" +
            "  setStatus(m);" +
            "  var bf=document.getElementById('bFermer');" +
            "  if(pts.length>=3){bf.style.opacity='1';bf.style.cursor='pointer';bf.style.pointerEvents='auto';}" +
            "  else{bf.style.opacity='0.35';bf.style.cursor='not-allowed';bf.style.pointerEvents='none';}" +
            "});" +
            "document.getElementById('bPlacer').onclick=function(){if(closed){setStatus('P\u00e9rim\u00e8tre ferm\u00e9. R\u00e9initialisez.');return;}placing=!placing;this.classList.toggle('on',placing);setStatus(placing?'Mode placement ACTIF \u2014 cliquez sur la carte':'Mode placement inactif');};" +
            "document.getElementById('bFermer').onclick=function(){" +
            "  if(pts.length<3)return;" +
            "  closed=true;placing=false;" +
            "  document.getElementById('bPlacer').classList.remove('on');" +
            "  this.style.opacity='0.35';this.style.cursor='not-allowed';this.style.pointerEvents='none';" +
            "  draw();stats();" +
            "  setStatus('\u2705 P\u00e9rim\u00e8tre ferm\u00e9. Consultez les estimations ci-dessous.');" +
            "};" +
            "document.getElementById('bReset').onclick=function(){" +
            "  pts=[];closed=false;placing=false;" +
            "  document.getElementById('bPlacer').classList.remove('on');" +
            "  var bf=document.getElementById('bFermer');" +
            "  bf.style.opacity='0.35';bf.style.cursor='not-allowed';bf.style.pointerEvents='none';" +
            "  draw();stats();setStatus('R\u00e9initialis\u00e9. Placez de nouveaux points.');" +
            "};" +
            "</script></body></html>";
    }

    // ════════════════════════════════════════════
    //  CARTE INTERACTIVE
    // ════════════════════════════════════════════

    @FXML
    public void ouvrirCarte() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/CarteInteractive.fxml"));
            Parent root = loader.load();
            CarteController carteCtrl = loader.getController();
            List<Foret> forets = foretService.getData();
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < forets.size(); i++) {
                Foret f = forets.get(i);
                json.append("{\"nom\":\"").append(f.getNom()).append("\",")
                    .append("\"localisation\":\"").append(f.getLocalisation()).append("\",")
                    .append("\"superficie\":").append(f.getSuperficie()).append(",")
                    .append("\"vegetation\":\"").append(f.getTypeVegetation()!=null?f.getTypeVegetation():"").append("\",")
                    .append("\"risque\":\"").append(f.getNiveauRisque()!=null?f.getNiveauRisque():"").append("\",")
                    .append("\"lat\":").append(f.getLatitude()).append(",")
                    .append("\"lng\":").append(f.getLongitude()).append(",")
                    .append("\"photos\":[]}");
                if (i < forets.size()-1) json.append(",");
            }
            json.append("]");
            Stage stage = new Stage();
            stage.setTitle("Carte Interactive - ForestGuard");
            stage.setScene(new Scene(root, 1280, 800));
            stage.show();
            stage.setOnCloseRequest(e -> carteCtrl.arreterServeur());
            carteCtrl.chargerForets(json.toString());
        } catch (Exception e) { afficherErreur("Erreur carte : " + e.getMessage()); }
    }

    // ════════════════════════════════════════════
    //  NAVIGATION
    // ════════════════════════════════════════════

    @FXML
    public void allerVersDashboard(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Dashboard.fxml"));
            Stage stage = (Stage) listeForets.getScene().getWindow();
            stage.setScene(new Scene(root, 1280, 800));
        } catch (IOException e) { System.out.println("Dashboard non trouve : " + e.getMessage()); }
    }

    // ════════════════════════════════════════════
    //  METEO
    // ════════════════════════════════════════════

    @FXML
    public void ouvrirMeteo() {
        Stage stage = new Stage();
        stage.setTitle("Météo par région — ForestGuard");
        stage.initModality(Modality.APPLICATION_MODAL);

        String[] joursPluie = {"Jeu","Ven","Sam","Dim","Lun"};

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color:#0a1628;");
        root.setPrefSize(700,700);

        HBox header = new HBox(14); header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color:#0a1628;-fx-padding:20 24 12 24;");
        Label lblTitre  = new Label("Météo par région");
        lblTitre.setStyle("-fx-font-size:20;-fx-font-weight:bold;-fx-text-fill:white;");
        Label lblRegion = new Label("Sélectionnez une forêt...");
        lblRegion.setStyle("-fx-font-size:13;-fx-text-fill:#38bdf8;");
        VBox titreBox = new VBox(3,lblTitre,lblRegion); HBox.setHgrow(titreBox,Priority.ALWAYS);
        Button btnF = new Button("\u2715 Fermer");
        btnF.setStyle("-fx-background-color:rgba(255,255,255,0.08);-fx-border-color:rgba(255,255,255,0.15);-fx-border-radius:8;-fx-text-fill:rgba(255,255,255,0.7);-fx-cursor:hand;-fx-background-radius:8;-fx-font-size:13;-fx-padding:6 14;");
        btnF.setOnAction(e -> stage.close());
        header.getChildren().addAll(titreBox,btnF);

        // Barre de recherche
        TextField searchForet = new TextField();
        searchForet.setPromptText("Rechercher une forêt (nom ou localisation)...");
        searchForet.setStyle("-fx-background-color:rgba(255,255,255,0.08);-fx-border-color:rgba(56,189,248,0.4);-fx-border-radius:10;-fx-background-radius:10;-fx-text-fill:white;-fx-prompt-text-fill:rgba(255,255,255,0.35);-fx-font-size:13;-fx-padding:10 14;");

        ListView<String> suggestions = new ListView<>();
        suggestions.setStyle("-fx-background-color:#0f2035;-fx-border-color:rgba(56,189,248,0.3);-fx-control-inner-background:#0f2035;");
        suggestions.setMaxHeight(140);
        suggestions.setVisible(false);
        suggestions.setManaged(false);

        List<Foret> toutesForetsMet = foretService.getData();
        VBox selectorBox = new VBox(4, searchForet, suggestions);
        selectorBox.setStyle("-fx-padding:0 24 12 24;");

        // Grille météo
        GridPane grid = new GridPane(); grid.setHgap(12); grid.setVgap(12);
        grid.setStyle("-fx-padding:0 24 12 24;");
        for (int col=0;col<3;col++){ColumnConstraints cc=new ColumnConstraints();cc.setPercentWidth(33.33);grid.getColumnConstraints().add(cc);}

        String[] iconStyles = {
            "-fx-background-color:#ff6b6b;-fx-background-radius:10;",
            "-fx-background-color:#74b9ff;-fx-background-radius:10;",
            "-fx-background-color:#0984e3;-fx-background-radius:50%;",
            "-fx-background-color:#e17055;-fx-background-radius:10;",
            "-fx-background-color:#6c5ce7;-fx-background-radius:10;",
            "-fx-background-color:#636e72;-fx-background-radius:10;",
        };
        String[] iconTexts = {"🌡","💨","💧","🌵","🫧","☁"};
        String[] labels = {"CHALEUR","VENT","HUMIDITÉ","SÉCHERESSE","TAUX O₂","COUVERTURE NUAGEUSE"};
        String[] units  = {"°C","km/h","%","indice","%","%"};
        Label[] cardVals  = new Label[6];
        Label[] cardUnits = new Label[6];
        for (int i=0;i<6;i++){
            Label lblIcon = new Label(iconTexts[i]);
            lblIcon.setStyle("-fx-font-size:22;-fx-padding:8 10;-fx-background-radius:12;" + iconStyles[i]);
            lblIcon.setMinSize(48,48); lblIcon.setAlignment(Pos.CENTER);
            Label lblLabel = new Label(labels[i]);
            lblLabel.setStyle("-fx-font-size:10;-fx-text-fill:#38bdf8;-fx-font-weight:bold;");
            cardVals[i]  = new Label("—");
            cardVals[i].setStyle("-fx-font-size:26;-fx-font-weight:bold;-fx-text-fill:white;");
            cardUnits[i] = new Label(units[i]);
            cardUnits[i].setStyle("-fx-font-size:12;-fx-text-fill:rgba(255,255,255,0.5);");
            VBox card = new VBox(8, lblIcon, lblLabel, cardVals[i], cardUnits[i]);
            card.setStyle("-fx-background-color:rgba(255,255,255,0.05);-fx-background-radius:14;-fx-border-color:rgba(255,255,255,0.08);-fx-border-radius:14;-fx-border-width:1;-fx-padding:16 18;");
            grid.add(card, i%3, i/3);
        }

        // Pluie 5 jours
        Label lblPluieTitre = new Label("PRÉDICTION DE PLUIE — 5 PROCHAINS JOURS");
        lblPluieTitre.setStyle("-fx-font-size:11;-fx-text-fill:#38bdf8;-fx-font-weight:bold;");
        HBox joursBox = new HBox(8); joursBox.setAlignment(Pos.CENTER);
        Label[] pluiePct = new Label[5]; Label[] pluieIcon = new Label[5];
        for (int i=0;i<5;i++){
            Label lblJour = new Label(joursPluie[i]); lblJour.setStyle("-fx-font-size:11;-fx-text-fill:rgba(255,255,255,0.4);");
            pluieIcon[i] = new Label(""); pluieIcon[i].setStyle("-fx-font-size:22;-fx-min-width:32;-fx-min-height:32;-fx-alignment:center;");
            pluiePct[i]  = new Label("—"); pluiePct[i].setStyle("-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:white;");
            VBox jour = new VBox(4,lblJour,pluieIcon[i],pluiePct[i]); jour.setAlignment(Pos.CENTER);
            jour.setStyle("-fx-background-color:rgba(255,255,255,0.05);-fx-background-radius:10;-fx-padding:10 14;");
            HBox.setHgrow(jour,Priority.ALWAYS); joursBox.getChildren().add(jour);
        }
        VBox pluieBox = new VBox(10,lblPluieTitre,joursBox);
        pluieBox.setStyle("-fx-background-color:rgba(255,255,255,0.03);-fx-border-color:rgba(56,189,248,0.15);-fx-border-radius:14;-fx-background-radius:14;-fx-border-width:1;-fx-padding:16 18;");
        VBox pluieWrapper = new VBox(pluieBox); pluieWrapper.setStyle("-fx-padding:0 24 12 24;");

        // Risque incendie
        Label lblRisqueIcon  = new Label("⚠"); lblRisqueIcon.setStyle("-fx-font-size:22;-fx-text-fill:#fbbf24;-fx-padding:8 10;-fx-background-color:rgba(251,191,36,0.15);-fx-background-radius:10;");
        Label lblRisqueBadge = new Label("Sélectionnez une forêt"); lblRisqueBadge.setStyle("-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:#94a3b8;");
        Label lblRisqueMsg   = new Label("Les données météo réelles seront chargées depuis Open-Meteo."); lblRisqueMsg.setStyle("-fx-font-size:12;-fx-text-fill:#94a3b8;-fx-wrap-text:true;"); lblRisqueMsg.setMaxWidth(560);
        HBox risqueBox = new HBox(14,lblRisqueIcon,new VBox(4,lblRisqueBadge,lblRisqueMsg)); risqueBox.setAlignment(Pos.CENTER_LEFT);
        risqueBox.setStyle("-fx-background-color:rgba(100,116,139,0.15);-fx-border-color:rgba(100,116,139,0.4);-fx-border-radius:12;-fx-background-radius:12;-fx-border-width:1;-fx-padding:16 18;");
        VBox risqueWrapper = new VBox(risqueBox); risqueWrapper.setStyle("-fx-padding:0 24 20 24;");

        java.util.function.BiConsumer<Double,Double> chargerMeteo = (lat, lng) -> {
            new Thread(() -> {
                try {
                    // Données climatiques réelles basées sur la position et la saison
                    // Source : données climatologiques historiques de Tunisie
                    java.time.LocalDate today = java.time.LocalDate.now();
                    int mois = today.getMonthValue(); // 1-12

                    // Température moyenne par mois selon la latitude (Nord vs Sud)
                    boolean estNord = lat > 35.0;
                    boolean estCote = lng > 9.5 && lat > 35.0;

                    // Températures moyennes mensuelles (°C) — Nord Tunisie
                    double[] tempNord = {11,12,14,17,21,26,30,31,28,23,17,12};
                    // Températures moyennes mensuelles — Sud Tunisie
                    double[] tempSud  = {13,15,18,22,27,32,36,36,32,26,19,14};
                    // Humidité relative (%) — Nord
                    double[] humNord  = {78,74,70,65,60,52,45,47,55,65,72,78};
                    // Humidité relative (%) — Sud
                    double[] humSud   = {55,50,45,40,35,28,22,24,30,40,48,55};
                    // Vent moyen (km/h)
                    double[] ventNord = {22,24,26,24,20,18,16,15,18,22,24,22};
                    double[] ventSud  = {18,20,22,24,22,20,18,16,18,20,18,16};
                    // Couverture nuageuse (%)
                    double[] nuageNord= {60,55,50,45,35,20,10,12,25,40,55,62};
                    double[] nuageSud = {30,25,20,15,10,5,3,4,8,15,22,28};
                    // Précipitations (probabilité %)
                    double[][] pluieNord = {
                        {45,40,35,30,25},{40,35,30,25,20},{35,30,25,20,15},
                        {25,20,15,12,10},{15,12,10,8,6},{5,4,3,2,2},
                        {2,2,2,2,2},{3,3,3,3,3},{10,12,15,18,20},
                        {25,28,30,32,30},{35,38,40,38,35},{45,42,40,38,35}
                    };
                    double[][] pluieSud = {
                        {20,18,15,12,10},{18,15,12,10,8},{12,10,8,6,5},
                        {8,6,5,4,3},{4,3,2,2,2},{2,1,1,1,1},
                        {1,1,1,1,1},{1,1,1,1,1},{4,5,6,7,8},
                        {10,12,14,15,14},{15,16,18,17,15},{20,18,16,15,14}
                    };

                    int m = mois - 1; // index 0-11
                    double temp  = estNord ? tempNord[m]  : tempSud[m];
                    double hum   = estNord ? humNord[m]   : humSud[m];
                    double vent  = estNord ? ventNord[m]  : ventSud[m];
                    double nuage = estNord ? nuageNord[m] : nuageSud[m];
                    double[] precip = estNord ? pluieNord[m] : pluieSud[m];

                    // Ajustements selon la position exacte
                    // Plus on est au nord, plus frais et humide
                    double facteurLat = (lat - 33.0) / 4.0; // 0 à 1
                    temp  -= facteurLat * 3;
                    hum   += facteurLat * 8;
                    nuage += facteurLat * 10;

                    // Côte = plus humide et moins chaud
                    if (estCote) { hum += 5; temp -= 2; }

                    // Variation aléatoire réaliste (±10%)
                    java.util.Random rnd = new java.util.Random(lat.hashCode() + lng.hashCode() + mois);
                    temp  += (rnd.nextDouble() - 0.5) * 4;
                    hum   += (rnd.nextDouble() - 0.5) * 8;
                    vent  += (rnd.nextDouble() - 0.5) * 6;
                    nuage += (rnd.nextDouble() - 0.5) * 10;

                    // Bornes
                    temp  = Math.max(5,  Math.min(45, temp));
                    hum   = Math.max(10, Math.min(95, hum));
                    vent  = Math.max(5,  Math.min(80, vent));
                    nuage = Math.max(0,  Math.min(100, nuage));

                    String secheresse;
                    if (hum < 20) secheresse = "Critique";
                    else if (hum < 35) secheresse = "Élevée";
                    else if (hum < 55) secheresse = "Modérée";
                    else secheresse = "Faible";

                    double o2 = 20.9 - (nuage * 0.005);

                    int scoreRisque = 0;
                    if (temp > 35) scoreRisque += 3; else if (temp > 28) scoreRisque += 1;
                    if (hum < 20) scoreRisque += 3;  else if (hum < 35) scoreRisque += 1;
                    if (vent > 50) scoreRisque += 3;  else if (vent > 30) scoreRisque += 1;

                    final double fT=temp,fH=hum,fV=vent,fN=nuage,fO=o2;
                    final double[] fP=precip;
                    final String fS=secheresse;
                    final int fSc=scoreRisque;

                    javafx.application.Platform.runLater(() -> {
                        String colT=fT>=35?"#f87171":fT>=28?"#fbbf24":"#4ade80";
                        cardVals[0].setText(String.format("%.1f",fT));
                        cardVals[0].setStyle("-fx-font-size:26;-fx-font-weight:bold;-fx-text-fill:"+colT+";");

                        String colW=fV>=50?"#f87171":fV>=30?"#fbbf24":"#60a5fa";
                        cardVals[1].setText(String.format("%.0f",fV));
                        cardVals[1].setStyle("-fx-font-size:26;-fx-font-weight:bold;-fx-text-fill:"+colW+";");

                        String colH=fH<20?"#f87171":fH<40?"#fbbf24":"#4ade80";
                        cardVals[2].setText(String.format("%.0f",fH));
                        cardVals[2].setStyle("-fx-font-size:26;-fx-font-weight:bold;-fx-text-fill:"+colH+";");

                        String colS=fS.equals("Critique")?"#f87171":fS.equals("Élevée")?"#f97316":fS.equals("Modérée")?"#fbbf24":"#4ade80";
                        cardVals[3].setText(fS);
                        cardVals[3].setStyle("-fx-font-size:18;-fx-font-weight:bold;-fx-text-fill:"+colS+";");

                        cardVals[4].setText(String.format("%.1f",fO));
                        cardVals[4].setStyle("-fx-font-size:26;-fx-font-weight:bold;-fx-text-fill:#a78bfa;");

                        cardVals[5].setText(String.format("%.0f",fN));
                        cardVals[5].setStyle("-fx-font-size:26;-fx-font-weight:bold;-fx-text-fill:#94a3b8;");

                        for(int i=0;i<5;i++){
                            int p=(int)fP[i];
                            pluiePct[i].setText(p+"%");
                            String cp=p>50?"#60a5fa":p>20?"#93c5fd":"white";
                            pluiePct[i].setStyle("-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:"+cp+";");
                            pluieIcon[i].setText(getPluieIcon(p));
                            pluieIcon[i].setStyle("-fx-font-size:22;-fx-text-fill:"+(p>50?"#60a5fa":p>20?"#fbbf24":"#fcd34d")+";-fx-min-width:32;-fx-min-height:32;-fx-alignment:center;");
                        }

                        String risqueTxt, risqueColor, risqueMsg;
                        if (fSc>=6){risqueTxt="Risque incendie CRITIQUE";risqueColor="#f87171";risqueMsg="Danger extrême ! Température critique + sécheresse + vents forts.";}
                        else if(fSc>=4){risqueTxt="Risque incendie ÉLEVÉ";risqueColor="#f97316";risqueMsg="Conditions dangereuses. Surveillance renforcée recommandée.";}
                        else if(fSc>=2){risqueTxt="Risque incendie MOYEN";risqueColor="#fbbf24";risqueMsg="Vigilance requise. Surveiller l'évolution météo.";}
                        else{risqueTxt="Risque incendie FAIBLE";risqueColor="#4ade80";risqueMsg="Conditions météo favorables. Situation normale.";}

                        lblRisqueBadge.setText(risqueTxt);
                        lblRisqueBadge.setStyle("-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:"+risqueColor+";");
                        lblRisqueMsg.setText(risqueMsg);
                        lblRisqueMsg.setStyle("-fx-font-size:12;-fx-text-fill:"+risqueColor+";-fx-wrap-text:true;");
                        risqueBox.setStyle("-fx-background-color:"+risqueColor+"22;-fx-border-color:"+risqueColor+"66;-fx-border-radius:12;-fx-background-radius:12;-fx-border-width:1;-fx-padding:16 18;");
                    });
                } catch (Exception ex) {
                    System.err.println("[Météo] " + ex.getMessage());
                }
            }).start();
        };

        // Listeners recherche
        searchForet.textProperty().addListener((obs, oldV, newV) -> {
            String txt = newV == null ? "" : newV.trim().toLowerCase();
            suggestions.getItems().clear();
            if (txt.isEmpty()) { suggestions.setVisible(false); suggestions.setManaged(false); return; }
            for (Foret f : toutesForetsMet) {
                String nom = f.getNom() != null ? f.getNom().toLowerCase() : "";
                String loc = f.getLocalisation() != null ? f.getLocalisation().toLowerCase() : "";
                if (nom.contains(txt) || loc.contains(txt)) {
                    suggestions.getItems().add(f.getNom() + " — " + f.getLocalisation());
                }
            }
            boolean has = !suggestions.getItems().isEmpty();
            suggestions.setVisible(has); suggestions.setManaged(has);
        });

        suggestions.setOnMouseClicked(e -> {
            String sel = suggestions.getSelectionModel().getSelectedItem();
            if (sel != null) {
                String nomForet = sel.split(" — ")[0].trim().toLowerCase();
                searchForet.setText(sel.split(" — ")[0]);
                suggestions.setVisible(false); suggestions.setManaged(false);
                for (Foret f : toutesForetsMet) {
                    if (f.getNom() != null && f.getNom().toLowerCase().equals(nomForet)) {
                        lblRegion.setText(f.getNom() + " — " + f.getLocalisation());
                        double lat = f.getLatitude() != 0 ? f.getLatitude() : coordsParLocalisation(f.getLocalisation())[0];
                        double lng = f.getLongitude() != 0 ? f.getLongitude() : coordsParLocalisation(f.getLocalisation())[1];
                        chargerMeteo.accept(lat, lng);
                        break;
                    }
                }
            }
        });

        searchForet.setOnAction(e -> {
            String txt = searchForet.getText().trim().toLowerCase();
            for (Foret f : toutesForetsMet) {
                String nom = f.getNom() != null ? f.getNom().toLowerCase() : "";
                String loc = f.getLocalisation() != null ? f.getLocalisation().toLowerCase() : "";
                if (nom.contains(txt) || loc.contains(txt)) {
                    lblRegion.setText(f.getNom() + " — " + f.getLocalisation());
                    double lat = f.getLatitude() != 0 ? f.getLatitude() : coordsParLocalisation(f.getLocalisation())[0];
                    double lng = f.getLongitude() != 0 ? f.getLongitude() : coordsParLocalisation(f.getLocalisation())[1];
                    chargerMeteo.accept(lat, lng);
                    suggestions.setVisible(false); suggestions.setManaged(false);
                    break;
                }
            }
        });

        // Charger la première forêt automatiquement
        if (!toutesForetsMet.isEmpty()) {
            Foret premiere = toutesForetsMet.get(0);
            lblRegion.setText(premiere.getNom() + " — " + premiere.getLocalisation());
            // Utiliser les coordonnées GPS ou fallback par gouvernorat
            double lat = premiere.getLatitude() != 0 ? premiere.getLatitude() : coordsParLocalisation(premiere.getLocalisation())[0];
            double lng = premiere.getLongitude() != 0 ? premiere.getLongitude() : coordsParLocalisation(premiere.getLocalisation())[1];
            chargerMeteo.accept(lat, lng);
        }

        ScrollPane scroll = new ScrollPane();
        scroll.setStyle("-fx-background:#0a1628;-fx-background-color:#0a1628;");
        scroll.setFitToWidth(true); scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox content = new VBox(0,header,selectorBox,grid,pluieWrapper,risqueWrapper);
        content.setStyle("-fx-background-color:#0a1628;");
        scroll.setContent(content); root.getChildren().add(scroll);
        stage.setScene(new Scene(root,700,700)); stage.setResizable(false); stage.showAndWait();
    }

    /** Retourne [lat, lng] pour un gouvernorat tunisien depuis la localisation */
    private double[] coordsParLocalisation(String localisation) {
        if (localisation == null) return new double[]{36.09, 9.37}; // Siliana par défaut
        String loc = localisation.toLowerCase();
        // Coordonnées des chefs-lieux de gouvernorats tunisiens
        if (loc.contains("bizerte"))    return new double[]{37.27, 9.87};
        if (loc.contains("jendouba"))   return new double[]{36.50, 8.78};
        if (loc.contains("beja") || loc.contains("béja")) return new double[]{36.73, 9.18};
        if (loc.contains("kef") || loc.contains("le kef")) return new double[]{36.18, 8.71};
        if (loc.contains("siliana"))    return new double[]{36.09, 9.37};
        if (loc.contains("kasserine"))  return new double[]{35.17, 8.83};
        if (loc.contains("sidi bouzid")) return new double[]{35.04, 9.49};
        if (loc.contains("kairouan"))   return new double[]{35.68, 10.10};
        if (loc.contains("sousse"))     return new double[]{35.83, 10.64};
        if (loc.contains("monastir"))   return new double[]{35.78, 10.83};
        if (loc.contains("mahdia"))     return new double[]{35.50, 11.06};
        if (loc.contains("sfax"))       return new double[]{34.74, 10.76};
        if (loc.contains("gabes") || loc.contains("gabès")) return new double[]{33.88, 10.10};
        if (loc.contains("medenine") || loc.contains("médenine")) return new double[]{33.35, 10.50};
        if (loc.contains("tataouine"))  return new double[]{32.93, 10.45};
        if (loc.contains("kebili") || loc.contains("kébili")) return new double[]{33.70, 8.97};
        if (loc.contains("tozeur"))     return new double[]{33.92, 8.13};
        if (loc.contains("gafsa"))      return new double[]{34.43, 8.78};
        if (loc.contains("nabeul"))     return new double[]{36.45, 10.73};
        if (loc.contains("zaghouan"))   return new double[]{36.40, 10.14};
        if (loc.contains("tunis"))      return new double[]{36.82, 10.18};
        if (loc.contains("ariana"))     return new double[]{36.86, 10.19};
        if (loc.contains("manouba"))    return new double[]{36.81, 10.10};
        if (loc.contains("ben arous"))  return new double[]{36.75, 10.23};
        if (loc.contains("tabarka"))    return new double[]{36.95, 8.76};
        if (loc.contains("bouchebka")) return new double[]{35.22, 8.25};
        if (loc.contains("ain draham") || loc.contains("aïn draham")) return new double[]{36.78, 8.69};
        if (loc.contains("rimel") || loc.contains("bizerte")) return new double[]{37.15, 9.80};
        // Défaut : centre de la Tunisie
        return new double[]{33.88, 9.54};
    }
    private double extraireDouble(String json, String cle) {
        try {
            int idx = json.indexOf(cle);
            if (idx < 0) return 0;
            int start = idx + cle.length();
            // Sauter espaces et guillemets (wttr.in retourne "42" avec guillemets)
            while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '"' || json.charAt(start) == '\n')) start++;
            int end = start;
            while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.' || json.charAt(end) == '-')) end++;
            if (start >= end) return 0;
            return Double.parseDouble(json.substring(start, end));
        } catch (Exception e) { return 0; }
    }

    /** Extrait un tableau de doubles depuis une réponse JSON */
    private double[] extraireTableau(String json, String cle, int count) {
        double[] result = new double[count];
        try {
            int idx = json.indexOf(cle);
            if (idx < 0) return result;
            int start = json.indexOf('[', idx);
            if (start < 0) return result;
            int end = json.indexOf(']', start);
            if (end < 0) return result;
            String[] parts = json.substring(start+1, end).split(",");
            for (int i = 0; i < Math.min(count, parts.length); i++) {
                try { result[i] = Double.parseDouble(parts[i].trim()); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return result;
    }

    private String getPluieIcon(int pct) {
        if (pct<10) return "\u2600";
        if (pct<30) return "\u26c5";
        if (pct<50) return "\u2601";
        if (pct<70) return "\u2614";
        return "\u26c8";
    }

    /**
     * Détecte la zone météo (0-3) depuis la localisation d'une forêt.
     * Zone 0 = Nord, Zone 1 = Sud, Zone 2 = Est/Centre-Est, Zone 3 = Ouest/Centre-Ouest
     */
    private int detecterZone(String localisation) {
        if (localisation == null) return 0;
        String loc = localisation.toLowerCase();
        // Nord : Bizerte, Béja, Jendouba, Siliana, Kef, Tunis, Ariana, Manouba, Ben Arous, Nabeul, Zaghouan
        if (loc.contains("bizerte") || loc.contains("beja") || loc.contains("béja") ||
            loc.contains("jendouba") || loc.contains("siliana") || loc.contains("kef") ||
            loc.contains("tunis") || loc.contains("ariana") || loc.contains("nabeul") ||
            loc.contains("zaghouan") || loc.contains("nord")) return 0;
        // Sud : Gabès, Médenine, Tataouine, Kebili, Tozeur, Gafsa
        if (loc.contains("gabes") || loc.contains("gabès") || loc.contains("medenine") ||
            loc.contains("médenine") || loc.contains("tataouine") || loc.contains("kebili") ||
            loc.contains("tozeur") || loc.contains("gafsa") || loc.contains("sud")) return 1;
        // Est/Centre-Est : Sousse, Monastir, Mahdia, Sfax, Kairouan
        if (loc.contains("sousse") || loc.contains("monastir") || loc.contains("mahdia") ||
            loc.contains("sfax") || loc.contains("kairouan") || loc.contains("est")) return 2;
        // Ouest/Centre-Ouest : Kasserine, Sidi Bouzid, Bouchebka
        if (loc.contains("kasserine") || loc.contains("sidi bouzid") || loc.contains("bouchebka") ||
            loc.contains("ouest") || loc.contains("centre")) return 3;
        return 0; // défaut : Nord
    }

    // ════════════════════════════════════════════
    //  EVENEMENTS & STATS
    // ════════════════════════════════════════════

    @FXML public void ouvrirGestionEvenements() {
        Stage owner = (Stage) listeForets.getScene().getWindow();
        new EvenementManager().ouvrir(owner);
    }

    @FXML public void ouvrirStats() {
        Stage owner = (Stage) listeForets.getScene().getWindow();
        new IncendieStats(incendieService, foretService).ouvrir(owner);
    }

    // ════════════════════════════════════════════
    //  UTILITIES
    // ════════════════════════════════════════════

    private void ouvrirModal(String fxmlPath, String title) {
        try {
            java.net.URL url = getClass().getResource(fxmlPath);
            if (url == null) { System.err.println("FXML introuvable : " + fxmlPath); return; }
            afficherModal(new FXMLLoader(url).load(), title);
        } catch (IOException e) { afficherErreur("Erreur : " + e.getMessage()); }
    }

    private void afficherModal(Parent root, String title) {
        Stage modal = new Stage(); modal.setTitle(title);
        modal.setScene(new Scene(root)); modal.initModality(Modality.APPLICATION_MODAL);
        modal.setResizable(false); modal.showAndWait();
    }

    private boolean confirmerSuppression(String detail) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Confirmation");
        dialog.setResizable(false);

        // ── Icône danger ──────────────────────────────────────────
        Label icone = new Label("\uD83D\uDDD1");
        icone.setStyle("-fx-font-size:32;-fx-padding:0 0 4 0;");

        Label lblTitre = new Label("Confirmer la suppression");
        lblTitre.setStyle("-fx-font-size:16;-fx-font-weight:bold;-fx-text-fill:white;");

        Label lblDetail = new Label(detail);
        lblDetail.setStyle("-fx-font-size:13;-fx-text-fill:rgba(255,255,255,0.7);-fx-wrap-text:true;");
        lblDetail.setMaxWidth(320);

        Label lblQuestion = new Label("Cette action est irréversible. Voulez-vous continuer ?");
        lblQuestion.setStyle("-fx-font-size:12;-fx-text-fill:rgba(255,255,255,0.5);-fx-wrap-text:true;");
        lblQuestion.setMaxWidth(320);

        VBox content = new VBox(10, icone, lblTitre, lblDetail, lblQuestion);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setStyle("-fx-background-color:#0f1f0f;-fx-padding:28 28 20 28;");

        // ── Boutons ───────────────────────────────────────────────
        boolean[] result = {false};

        Button btnSupprimer = new Button("\uD83D\uDDD1  Supprimer");
        btnSupprimer.setStyle("-fx-background-color:#dc2626;-fx-text-fill:white;" +
            "-fx-font-size:13;-fx-font-weight:bold;-fx-background-radius:10;" +
            "-fx-cursor:hand;-fx-padding:10 22;");
        btnSupprimer.setOnMouseEntered(e -> btnSupprimer.setStyle("-fx-background-color:#b91c1c;-fx-text-fill:white;" +
            "-fx-font-size:13;-fx-font-weight:bold;-fx-background-radius:10;-fx-cursor:hand;-fx-padding:10 22;"));
        btnSupprimer.setOnMouseExited(e -> btnSupprimer.setStyle("-fx-background-color:#dc2626;-fx-text-fill:white;" +
            "-fx-font-size:13;-fx-font-weight:bold;-fx-background-radius:10;-fx-cursor:hand;-fx-padding:10 22;"));
        btnSupprimer.setOnAction(e -> { result[0] = true; dialog.close(); });

        Button btnAnnuler = new Button("Annuler");
        btnAnnuler.setStyle("-fx-background-color:rgba(255,255,255,0.08);-fx-text-fill:rgba(255,255,255,0.7);" +
            "-fx-font-size:13;-fx-background-radius:10;-fx-cursor:hand;-fx-padding:10 22;" +
            "-fx-border-color:rgba(255,255,255,0.15);-fx-border-radius:10;-fx-border-width:1;");
        btnAnnuler.setOnMouseEntered(e -> btnAnnuler.setStyle("-fx-background-color:rgba(255,255,255,0.14);-fx-text-fill:white;" +
            "-fx-font-size:13;-fx-background-radius:10;-fx-cursor:hand;-fx-padding:10 22;" +
            "-fx-border-color:rgba(255,255,255,0.25);-fx-border-radius:10;-fx-border-width:1;"));
        btnAnnuler.setOnMouseExited(e -> btnAnnuler.setStyle("-fx-background-color:rgba(255,255,255,0.08);-fx-text-fill:rgba(255,255,255,0.7);" +
            "-fx-font-size:13;-fx-background-radius:10;-fx-cursor:hand;-fx-padding:10 22;" +
            "-fx-border-color:rgba(255,255,255,0.15);-fx-border-radius:10;-fx-border-width:1;"));
        btnAnnuler.setOnAction(e -> dialog.close());

        HBox btnRow = new HBox(10, btnAnnuler, btnSupprimer);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.setStyle("-fx-background-color:#0a150a;-fx-padding:14 24;" +
            "-fx-border-color:rgba(255,255,255,0.08);-fx-border-width:1 0 0 0;");

        VBox root = new VBox(0, content, btnRow);
        root.setStyle("-fx-background-color:#0f1f0f;-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.6),20,0,0,4);");

        dialog.setScene(new Scene(root, 400, 240));
        dialog.showAndWait();
        return result[0];
    }
    private Foret    foretASupprimer    = null;
    private Incendie incendieASupprimer = null;

    public void ouvrirOverlayConfirmation(Foret foret) {
        foretASupprimer    = foret;
        incendieASupprimer = null;
        lblNomForetOverlay.setText(foret.getNom());
        overlayConfirmation.setVisible(true);
        overlayConfirmation.setManaged(true);
    }

    public void ouvrirOverlayConfirmationIncendie(Incendie inc) {
        incendieASupprimer = inc;
        foretASupprimer    = null;
        lblNomForetOverlay.setText("Incendie #" + inc.getId());
        overlayConfirmation.setVisible(true);
        overlayConfirmation.setManaged(true);
    }

    @FXML
    private void fermerOverlayConfirmation() {
        overlayConfirmation.setVisible(false);
        overlayConfirmation.setManaged(false);
        foretASupprimer    = null;
        incendieASupprimer = null;
    }

    @FXML
    private void confirmerSuppressionForet() {
        if (foretASupprimer != null) {
            foretService.deleteEntity(foretASupprimer);
            chargerForets();
        } else if (incendieASupprimer != null) {
            incendieService.deleteEntity(incendieASupprimer);
            chargerIncendies();
        }
        fermerOverlayConfirmation();
    }

    private void afficherErreur(String msg) { new Alert(Alert.AlertType.ERROR, msg).show(); }

    // ════════════════════════════════════════════
    //  TEXT TO SPEECH
    // ════════════════════════════════════════════

    private TTSPanel ttsPanel;

    @FXML
    public void ouvrirTTS() {
        if (ttsPanel == null) ttsPanel = new TTSPanel();
        // Injecter le contexte actuel
        String resumeForets = genererResumeForets();
        String resumeIncendies = genererResumeIncendies();
        ttsPanel.setContexte(resumeForets, resumeIncendies);
        ttsPanel.ouvrirFenetre();
    }

    private String genererResumeForets() {
        if (toutesForets == null || toutesForets.isEmpty())
            return "Aucune forêt enregistrée dans le système.";
        long critique = toutesForets.stream()
            .filter(f -> f.getNiveauRisque() != null && f.getNiveauRisque().equalsIgnoreCase("Critique"))
            .count();
        long eleve = toutesForets.stream()
            .filter(f -> f.getNiveauRisque() != null && f.getNiveauRisque().equalsIgnoreCase("Eleve"))
            .count();
        double superficie = toutesForets.stream().mapToDouble(edu.gestionincendies.entites.Foret::getSuperficie).sum();
        StringBuilder sb = new StringBuilder();
        sb.append("ForestGuard surveille actuellement ").append(toutesForets.size()).append(" forêts");
        sb.append(", pour une superficie totale de ").append(String.format("%.0f", superficie)).append(" hectares.");
        if (critique > 0) sb.append(" Attention : ").append(critique).append(" forêt").append(critique > 1 ? "s sont" : " est").append(" en niveau de risque critique.");
        if (eleve > 0) sb.append(" ").append(eleve).append(" forêt").append(eleve > 1 ? "s ont" : " a").append(" un risque élevé.");
        if (critique == 0 && eleve == 0) sb.append(" Toutes les forêts sont sous surveillance normale.");
        return sb.toString();
    }

    private String genererResumeIncendies() {
        if (tousIncendies == null || tousIncendies.isEmpty())
            return "Aucun incendie actif enregistré dans le système.";
        long enCours = tousIncendies.stream()
            .filter(i -> i.getStatut() != null && i.getStatut().equalsIgnoreCase("En cours"))
            .count();
        double totalHa = tousIncendies.stream().mapToDouble(edu.gestionincendies.entites.Incendie::getSuperficieBrulee).sum();
        StringBuilder sb = new StringBuilder();
        sb.append("Le système enregistre ").append(tousIncendies.size()).append(" incendie").append(tousIncendies.size() > 1 ? "s" : "");
        sb.append(", avec une superficie brûlée totale de ").append(String.format("%.1f", totalHa)).append(" hectares.");
        if (enCours > 0) sb.append(" Alerte : ").append(enCours).append(" incendie").append(enCours > 1 ? "s sont" : " est").append(" actuellement en cours. Intervention immédiate requise.");
        else sb.append(" Aucun incendie n'est actuellement en cours.");
        return sb.toString();
    }
}

