package edu.capteur.controllers;

import edu.capteur.entities.Capteur;
import edu.capteur.entities.Maintenance;
import edu.capteur.services.AIPredictionService;
import edu.capteur.services.CapteurService;
import edu.capteur.services.MaintenanceService;
import edu.capteur.services.ForetService;
import edu.capteur.entities.Foret;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class CapteurController {

    // ── Sidebar
    @FXML private VBox   sidebar;
    @FXML private VBox   logoNom;
    @FXML private Button btnToggle;
    @FXML private Label  lblReducer;
    @FXML private Button btnPM, btnFR, btnCP, btnDN, btnAL, btnUS;

    // ── Stats
    @FXML private Label lblActifs, lblEnPanne, lblTotal, lblTotalM;

    // ── Onglets
    @FXML private Button tabCapteur, tabMaintenance;
    @FXML private VBox   panneauCapteurs, panneauMaintenances;

    // ── Capteurs
    @FXML private VBox      listeCapteurs;
    @FXML private TextField tfRecherche;

    // ── Maintenances
    @FXML private VBox      listeMaintenances;
    @FXML private TextField tfRechercheM;
    @FXML private Button    btnFiltreAll, btnFiltrePlanifie, btnFiltreEnCours, btnFiltreTermine;
    private String          filtreStatutActif = "tous";

    // ── SVG images (memes que sur la carte) ───────────────────────────────
    private static final String SVG_TEMPERATURE =
        "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 80 80'>" +
        "<defs>" +
        "<linearGradient id='pcbT' x1='0%' y1='0%' x2='100%' y2='100%'>" +
        "<stop offset='0%' style='stop-color:#2d6a2d'/>" +
        "<stop offset='100%' style='stop-color:#1a3d1a'/></linearGradient>" +
        "<linearGradient id='chipT' x1='0%' y1='0%' x2='100%' y2='100%'>" +
        "<stop offset='0%' style='stop-color:#555'/>" +
        "<stop offset='100%' style='stop-color:#222'/></linearGradient>" +
        "</defs>" +
        "<rect x='5' y='5' width='70' height='70' rx='5' fill='url(#pcbT)' stroke='#1a5c1a' stroke-width='1.5'/>" +
        "<line x1='12' y1='35' x2='68' y2='35' stroke='#b8860b' stroke-width='1' opacity='0.5'/>" +
        "<line x1='12' y1='50' x2='68' y2='50' stroke='#b8860b' stroke-width='1' opacity='0.5'/>" +
        "<line x1='28' y1='12' x2='28' y2='68' stroke='#b8860b' stroke-width='0.8' opacity='0.35'/>" +
        "<line x1='52' y1='12' x2='52' y2='68' stroke='#b8860b' stroke-width='0.8' opacity='0.35'/>" +
        "<rect x='20' y='18' width='40' height='30' rx='2' fill='url(#chipT)' stroke='#777' stroke-width='1'/>" +
        "<rect x='13' y='22' width='7' height='3' rx='1' fill='#c8a44a'/>" +
        "<rect x='13' y='28' width='7' height='3' rx='1' fill='#c8a44a'/>" +
        "<rect x='13' y='34' width='7' height='3' rx='1' fill='#c8a44a'/>" +
        "<rect x='13' y='40' width='7' height='3' rx='1' fill='#c8a44a'/>" +
        "<rect x='60' y='22' width='7' height='3' rx='1' fill='#c8a44a'/>" +
        "<rect x='60' y='28' width='7' height='3' rx='1' fill='#c8a44a'/>" +
        "<rect x='60' y='34' width='7' height='3' rx='1' fill='#c8a44a'/>" +
        "<rect x='60' y='40' width='7' height='3' rx='1' fill='#c8a44a'/>" +
        "<text x='40' y='30' text-anchor='middle' font-family='monospace' font-size='6' fill='#ccc' font-weight='bold'>SHT30</text>" +
        "<text x='40' y='38' text-anchor='middle' font-family='monospace' font-size='4.5' fill='#999'>TEMP/HUM</text>" +
        "<text x='40' y='44' text-anchor='middle' font-family='monospace' font-size='4' fill='#666'>ForestGuard</text>" +
        "<rect x='20' y='55' width='9' height='7' rx='1' fill='#4455cc' stroke='#3344bb' stroke-width='0.5'/>" +
        "<rect x='33' y='55' width='9' height='7' rx='1' fill='#4455cc' stroke='#3344bb' stroke-width='0.5'/>" +
        "<rect x='46' y='55' width='9' height='7' rx='1' fill='#884422' stroke='#773311' stroke-width='0.5'/>" +
        "<circle cx='65' cy='62' r='5' fill='#00cc44' opacity='0.85'/>" +
        "<circle cx='65' cy='62' r='2.5' fill='#aaffcc'/>" +
        "<circle cx='11' cy='11' r='3.5' fill='none' stroke='#556655' stroke-width='1.5'/>" +
        "<circle cx='69' cy='11' r='3.5' fill='none' stroke='#556655' stroke-width='1.5'/>" +
        "<circle cx='11' cy='69' r='3.5' fill='none' stroke='#556655' stroke-width='1.5'/>" +
        "<circle cx='69' cy='69' r='3.5' fill='none' stroke='#556655' stroke-width='1.5'/>" +
        "</svg>";

    private static final String SVG_FUMEE =
        "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 80 80'>" +
        "<defs>" +
        "<linearGradient id='pcbF' x1='0%' y1='0%' x2='100%' y2='100%'>" +
        "<stop offset='0%' style='stop-color:#1a2a5c'/>" +
        "<stop offset='100%' style='stop-color:#0d1840'/></linearGradient>" +
        "<radialGradient id='domeF' cx='40%' cy='35%' r='60%'>" +
        "<stop offset='0%' style='stop-color:#e8cc55'/>" +
        "<stop offset='60%' style='stop-color:#aa8822'/>" +
        "<stop offset='100%' style='stop-color:#664400'/></radialGradient>" +
        "</defs>" +
        "<rect x='5' y='5' width='70' height='70' rx='5' fill='url(#pcbF)' stroke='#0d1840' stroke-width='1.5'/>" +
        "<circle cx='40' cy='36' r='19' fill='#776600' stroke='#554400' stroke-width='1.5'/>" +
        "<circle cx='40' cy='36' r='16' fill='url(#domeF)'/>" +
        "<line x1='24' y1='36' x2='56' y2='36' stroke='#333' stroke-width='1' opacity='0.6'/>" +
        "<line x1='40' y1='20' x2='40' y2='52' stroke='#333' stroke-width='1' opacity='0.6'/>" +
        "<circle cx='40' cy='36' r='5' fill='#111' opacity='0.75'/>" +
        "<circle cx='40' cy='36' r='2' fill='#333' opacity='0.9'/>" +
        "<rect x='18' y='61' width='5' height='10' rx='1' fill='#c8a44a'/>" +
        "<rect x='26' y='61' width='5' height='10' rx='1' fill='#c8a44a'/>" +
        "<rect x='34' y='61' width='5' height='10' rx='1' fill='#c8a44a'/>" +
        "<rect x='42' y='61' width='5' height='10' rx='1' fill='#c8a44a'/>" +
        "<rect x='50' y='61' width='5' height='10' rx='1' fill='#c8a44a'/>" +
        "<text x='40' y='58' text-anchor='middle' font-family='monospace' font-size='5' fill='#5577dd' font-weight='bold'>MQ-135</text>" +
        "<circle cx='15' cy='15' r='4.5' fill='#ff4400' opacity='0.9'/>" +
        "<circle cx='15' cy='15' r='2' fill='#ffaa77'/>" +
        "<circle cx='65' cy='15' r='4.5' fill='#00cc44' opacity='0.8'/>" +
        "<circle cx='65' cy='15' r='2' fill='#aaffcc'/>" +
        "</svg>";

    private static final String SVG_HUMIDITE =
        "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 80 80'>" +
        "<defs>" +
        "<linearGradient id='pcbH' x1='0%' y1='0%' x2='100%' y2='100%'>" +
        "<stop offset='0%' style='stop-color:#4a1060'/>" +
        "<stop offset='100%' style='stop-color:#2d0840'/></linearGradient>" +
        "</defs>" +
        "<rect x='10' y='4' width='60' height='45' rx='4' fill='url(#pcbH)' stroke='#2d0840' stroke-width='1.5'/>" +
        "<rect x='22' y='10' width='36' height='24' rx='2' fill='#222' stroke='#444' stroke-width='1'/>" +
        "<text x='40' y='20' text-anchor='middle' font-family='monospace' font-size='5.5' fill='#ccc' font-weight='bold'>ESP32</text>" +
        "<text x='40' y='27' text-anchor='middle' font-family='monospace' font-size='4' fill='#888'>WiFi/BLE</text>" +
        "<text x='40' y='33' text-anchor='middle' font-family='monospace' font-size='3.5' fill='#666'>ForestGuard</text>" +
        "<rect x='12' y='49' width='8' height='31' rx='4' fill='#c0c0c0' stroke='#999' stroke-width='1'/>" +
        "<rect x='24' y='49' width='8' height='31' rx='4' fill='#c0c0c0' stroke='#999' stroke-width='1'/>" +
        "<rect x='36' y='49' width='8' height='31' rx='4' fill='#c0c0c0' stroke='#999' stroke-width='1'/>" +
        "<rect x='48' y='49' width='8' height='31' rx='4' fill='#c0c0c0' stroke='#999' stroke-width='1'/>" +
        "<rect x='60' y='49' width='8' height='31' rx='4' fill='#c0c0c0' stroke='#999' stroke-width='1'/>" +
        "</svg>";

    private static final String SVG_GENERIQUE =
        "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 80 80'>" +
        "<defs>" +
        "<linearGradient id='boite' x1='0%' y1='0%' x2='100%' y2='100%'>" +
        "<stop offset='0%' style='stop-color:#444'/>" +
        "<stop offset='100%' style='stop-color:#1a1a1a'/></linearGradient>" +
        "<linearGradient id='face' x1='0%' y1='0%' x2='0%' y2='100%'>" +
        "<stop offset='0%' style='stop-color:#555'/>" +
        "<stop offset='100%' style='stop-color:#2a2a2a'/></linearGradient>" +
        "</defs>" +
        "<rect x='10' y='8' width='60' height='64' rx='8' fill='url(#boite)' stroke='#222' stroke-width='2'/>" +
        "<rect x='14' y='12' width='52' height='56' rx='6' fill='url(#face)'/>" +
        "<rect x='18' y='16' width='44' height='26' rx='3' fill='#111'/>" +
        "<rect x='20' y='18' width='40' height='22' rx='2' fill='#001a00'/>" +
        "<text x='40' y='27' text-anchor='middle' font-family='monospace' font-size='6' fill='#00ff44'>23.4</text>" +
        "<text x='40' y='34' text-anchor='middle' font-family='monospace' font-size='4' fill='#00aa22'>TEMPERATURE C</text>" +
        "<circle cx='25' cy='55' r='5' fill='#222' stroke='#444' stroke-width='1'/>" +
        "<circle cx='25' cy='55' r='3' fill='#00cc44' opacity='0.85'/>" +
        "<circle cx='40' cy='55' r='5' fill='#222' stroke='#444' stroke-width='1'/>" +
        "<circle cx='40' cy='55' r='3' fill='#ffaa00' opacity='0.85'/>" +
        "<circle cx='55' cy='55' r='5' fill='#222' stroke='#444' stroke-width='1'/>" +
        "<circle cx='55' cy='55' r='3' fill='#3366ff' opacity='0.85'/>" +
        "</svg>";

    private boolean sidebarExpanded = true;
    private static final double W_CLOSED = 72;
    private static final double W_OPEN   = 240;

    private final CapteurService      capteurService     = new CapteurService();
    private final MaintenanceService  maintenanceService = new MaintenanceService();
    private final ForetService        foretService       = new ForetService();
    private final AIPredictionService aiService          = new AIPredictionService(); // ✅ NOUVEAU

    private List<Capteur>     tousLesCapteurs;
    private List<Maintenance> toutesMaintenances;

    // ════════════════════════════════════════
    //  INIT
    // ════════════════════════════════════════
    @FXML
    public void initialize() {
        Button[] navBtns = {btnPM, btnFR, btnDN, btnAL, btnUS};
        for (Button b : navBtns) {
            if (b == null) continue;
            b.setOnMouseEntered(e -> b.setStyle(
                "-fx-background-color: rgba(22,163,74,0.15); -fx-text-fill: #4ade80;" +
                "-fx-font-size: 13; -fx-alignment: CENTER_LEFT;" +
                "-fx-padding: 10 16; -fx-background-radius: 12; -fx-cursor: hand;"));
            b.setOnMouseExited(e -> b.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #94a3b8;" +
                "-fx-font-size: 13; -fx-alignment: CENTER_LEFT;" +
                "-fx-padding: 10 16; -fx-background-radius: 12; -fx-cursor: hand;"));
        }
        chargerCapteurs();
        chargerMaintenances();
    }

    // ════════════════════════════════════════
    //  ONGLETS avec transition fade
    // ════════════════════════════════════════
    @FXML
    public void afficherOngletCapteurs() {
        if (panneauCapteurs.isVisible()) return; // déjà actif
        // Fade out maintenances → fade in capteurs
        if (panneauMaintenances.isVisible()) {
            javafx.animation.FadeTransition ftOut = new javafx.animation.FadeTransition(Duration.millis(120), panneauMaintenances);
            ftOut.setFromValue(1); ftOut.setToValue(0);
            ftOut.setOnFinished(ev -> {
                panneauMaintenances.setVisible(false); panneauMaintenances.setManaged(false);
                panneauCapteurs.setOpacity(0);
                panneauCapteurs.setVisible(true); panneauCapteurs.setManaged(true);
                javafx.animation.FadeTransition ftIn = new javafx.animation.FadeTransition(Duration.millis(180), panneauCapteurs);
                ftIn.setFromValue(0); ftIn.setToValue(1); ftIn.play();
            });
            ftOut.play();
        } else {
            panneauCapteurs.setVisible(true); panneauCapteurs.setManaged(true);
        }
        tabCapteur.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white;" +
            "-fx-font-weight: bold; -fx-font-size: 13; -fx-background-radius: 9; -fx-cursor: hand;");
        tabMaintenance.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748b;" +
            "-fx-font-size: 13; -fx-background-radius: 9; -fx-cursor: hand;");
    }

    @FXML
    public void afficherOngletMaintenances() {
        if (panneauMaintenances.isVisible()) return;
        chargerMaintenances();
        if (panneauCapteurs.isVisible()) {
            javafx.animation.FadeTransition ftOut = new javafx.animation.FadeTransition(Duration.millis(120), panneauCapteurs);
            ftOut.setFromValue(1); ftOut.setToValue(0);
            ftOut.setOnFinished(ev -> {
                panneauCapteurs.setVisible(false); panneauCapteurs.setManaged(false);
                panneauMaintenances.setOpacity(0);
                panneauMaintenances.setVisible(true); panneauMaintenances.setManaged(true);
                javafx.animation.FadeTransition ftIn = new javafx.animation.FadeTransition(Duration.millis(180), panneauMaintenances);
                ftIn.setFromValue(0); ftIn.setToValue(1); ftIn.play();
            });
            ftOut.play();
        } else {
            panneauMaintenances.setVisible(true); panneauMaintenances.setManaged(true);
        }
        tabMaintenance.setStyle("-fx-background-color: #d97706; -fx-text-fill: white;" +
            "-fx-font-weight: bold; -fx-font-size: 13; -fx-background-radius: 9; -fx-cursor: hand;");
        tabCapteur.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748b;" +
            "-fx-font-size: 13; -fx-background-radius: 9; -fx-cursor: hand;");
    }

    // ════════════════════════════════════════
    //  CAPTEURS - CRUD
    // ════════════════════════════════════════
    private void chargerCapteurs() {
        // Skeleton loading : afficher des placeholders pendant le chargement
        afficherSkeletonCapteurs();
        List<Capteur> tous = capteurService.afficher();

        // ── Même filtre que la map : 1 seul capteur par (foret_id + localisation) ──
        java.util.Set<String> cleesVues = new java.util.LinkedHashSet<>();
        tousLesCapteurs = new java.util.ArrayList<>();
        for (Capteur c : tous) {
            String cle = c.getForetId() + "|" + (c.getLocalisation() != null ? c.getLocalisation().trim().toLowerCase() : "");
            if (cleesVues.add(cle)) {
                tousLesCapteurs.add(c);
            }
        }
        afficherCapteurs(tousLesCapteurs);
        long actifs  = tousLesCapteurs.stream().filter(c -> "actif".equals(c.getStatut())).count();
        long pannes  = tousLesCapteurs.stream().filter(c -> "en_panne".equals(c.getStatut())).count();
        long inactifs = tousLesCapteurs.size() - actifs - pannes;
        lblActifs.setText(String.valueOf(actifs));
        lblEnPanne.setText(String.valueOf(pannes));
        lblTotal.setText(String.valueOf(tousLesCapteurs.size()));
        // Mise à jour du header de section avec compteur dynamique
        mettreAJourHeaderCapteurs(tousLesCapteurs.size(), actifs, pannes, inactifs);
    }

    /** Affiche 3 lignes skeleton pendant le chargement */
    private void afficherSkeletonCapteurs() {
        listeCapteurs.getChildren().clear();
        for (int i = 0; i < 3; i++) {
            HBox sk = new HBox(12);
            sk.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            sk.setStyle("-fx-background-color: white; -fx-padding: 14 18;" +
                        "-fx-border-color: #f1f5f9; -fx-border-width: 0 0 1 0;");
            // Barre gauche
            javafx.scene.layout.Region skStrip = new javafx.scene.layout.Region();
            skStrip.setMinWidth(4); skStrip.setMaxWidth(4);
            skStrip.setStyle("-fx-background-color: #e2e8f0;");
            // Avatar
            javafx.scene.layout.Region skAvatar = new javafx.scene.layout.Region();
            skAvatar.setMinSize(44,44); skAvatar.setMaxSize(44,44);
            skAvatar.setStyle("-fx-background-color: #e2e8f0; -fx-background-radius: 50;");
            // Lignes de texte
            VBox skLines = new VBox(8);
            HBox.setHgrow(skLines, javafx.scene.layout.Priority.ALWAYS);
            skLines.setStyle("-fx-padding: 4 0;");
            for (int j = 0; j < 3; j++) {
                javafx.scene.layout.Region skLine = new javafx.scene.layout.Region();
                skLine.setMinHeight(10); skLine.setMaxHeight(10);
                skLine.setMaxWidth(j == 0 ? 200 : j == 1 ? 140 : 100);
                skLine.setStyle("-fx-background-color: #e2e8f0; -fx-background-radius: 5;");
                skLines.getChildren().add(skLine);
            }
            // Animation pulse sur les skeletons
            javafx.animation.Timeline skAnim = new javafx.animation.Timeline(
                new KeyFrame(Duration.ZERO,     new KeyValue(sk.opacityProperty(), 0.4)),
                new KeyFrame(Duration.millis(700), new KeyValue(sk.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(1400), new KeyValue(sk.opacityProperty(), 0.4))
            );
            skAnim.setCycleCount(javafx.animation.Animation.INDEFINITE);
            skAnim.setDelay(Duration.millis(i * 120));
            skAnim.play();
            sk.getChildren().addAll(skStrip, skAvatar, skLines);
            listeCapteurs.getChildren().add(sk);
        }
    }

    private void afficherCapteurs(List<Capteur> liste) {
        listeCapteurs.getChildren().clear();
        if (liste.isEmpty()) {
            listeCapteurs.getChildren().add(creerEmptyState(
                "📡", "Aucun capteur trouvé",
                tfRecherche != null && !tfRecherche.getText().isEmpty()
                    ? "Aucun résultat pour \"" + tfRecherche.getText() + "\""
                    : "Ajoutez votre premier capteur avec le bouton ＋",
                "#16a34a"
            ));
            return;
        }
        int idx = 0;
        for (Capteur c : liste)
            listeCapteurs.getChildren().add(creerLigneCapteur(c, idx++));
    }

    /** Met à jour le label de compteur dynamique au-dessus de la liste */
    private void mettreAJourHeaderCapteurs(long total, long actifs, long pannes, long inactifs) {
        // On cherche le label de compteur dans le FXML (lblHeaderCapteurs)
        // S'il n'existe pas, on met à jour le titre de la fenêtre
        if (lblTotal != null) {
            // Déjà géré par les stats cards — on enrichit juste le tooltip du total
            lblTotal.setTooltip(new Tooltip(
                total + " capteurs au total\n" +
                actifs + " actifs · " + pannes + " en panne · " + inactifs + " inactifs"
            ));
        }
    }

    /** Retourne un Node visuel representant le type du capteur (dessin direct JavaFX) */
    private javafx.scene.Node getSvgImage(String type) {
        javafx.scene.canvas.Canvas canvas = new javafx.scene.canvas.Canvas(38, 38);
        javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();

        String t = (type == null ? "" : type.toLowerCase());

        if (t.contains("temp")) {
            // PCB vert - Temperature
            gc.setFill(javafx.scene.paint.Color.web("#2d6a2d")); gc.fillRoundRect(1,1,36,36,5,5);
            gc.setFill(javafx.scene.paint.Color.web("#555"));     gc.fillRoundRect(8,7,22,16,2,2);
            gc.setFill(javafx.scene.paint.Color.web("#cccccc"));
            gc.setFont(javafx.scene.text.Font.font("Monospace", javafx.scene.text.FontWeight.BOLD, 4));
            gc.fillText("SHT30", 10, 15);
            gc.setFont(javafx.scene.text.Font.font("Monospace", 3)); gc.fillText("TEMP", 11, 19);
            // pins
            gc.setFill(javafx.scene.paint.Color.web("#c8a44a"));
            for (int i = 0; i < 4; i++) { gc.fillRoundRect(4, 9+i*3, 4, 2, 1, 1); gc.fillRoundRect(30, 9+i*3, 4, 2, 1, 1); }
            // LED verte
            gc.setFill(javafx.scene.paint.Color.web("#00cc44")); gc.fillOval(30,28,5,5);
            gc.setFill(javafx.scene.paint.Color.web("#aaffcc")); gc.fillOval(31.5,29.5,2,2);
            // capacitors
            gc.setFill(javafx.scene.paint.Color.web("#4455cc")); gc.fillRoundRect(8,26,5,4,1,1); gc.fillRoundRect(15,26,5,4,1,1);
            gc.setFill(javafx.scene.paint.Color.web("#884422")); gc.fillRoundRect(22,26,5,4,1,1);
        } else if (t.contains("fum") || t.contains("gaz") || t.contains("smoke")) {
            // PCB bleu - Fumee MQ-135
            gc.setFill(javafx.scene.paint.Color.web("#1a2a5c")); gc.fillRoundRect(1,1,36,36,5,5);
            // dome metallique
            gc.setFill(javafx.scene.paint.Color.web("#776600")); gc.fillOval(9,7,20,20);
            gc.setFill(javafx.scene.paint.Color.web("#e8cc55")); gc.fillOval(11,9,16,16);
            gc.setStroke(javafx.scene.paint.Color.web("#333333")); gc.setLineWidth(0.7);
            gc.strokeLine(11,19,29,19); gc.strokeLine(19,11,19,27);
            gc.setFill(javafx.scene.paint.Color.web("#111111")); gc.fillOval(17,17,4,4);
            // pins bas
            gc.setFill(javafx.scene.paint.Color.web("#c8a44a"));
            for (int i = 0; i < 5; i++) gc.fillRoundRect(7+i*5, 30, 3, 6, 1, 1);
            gc.setFill(javafx.scene.paint.Color.web("#5577dd"));
            gc.setFont(javafx.scene.text.Font.font("Monospace", javafx.scene.text.FontWeight.BOLD, 3));
            gc.fillText("MQ-135", 8, 30);
            // LEDs
            gc.setFill(javafx.scene.paint.Color.web("#ff4400")); gc.fillOval(4,4,5,5);
            gc.setFill(javafx.scene.paint.Color.web("#00cc44")); gc.fillOval(29,4,5,5);
        } else if (t.contains("hum") || t.contains("sol") || t.contains("eau")) {
            // PCB violet - Humidite ESP32
            gc.setFill(javafx.scene.paint.Color.web("#4a1060")); gc.fillRoundRect(4,1,30,22,4,4);
            gc.setFill(javafx.scene.paint.Color.web("#222222")); gc.fillRoundRect(9,4,20,12,2,2);
            gc.setFill(javafx.scene.paint.Color.web("#cccccc"));
            gc.setFont(javafx.scene.text.Font.font("Monospace", javafx.scene.text.FontWeight.BOLD, 4));
            gc.fillText("ESP32", 11, 10);
            gc.setFont(javafx.scene.text.Font.font("Monospace", 3)); gc.fillText("WiFi/BLE", 10, 14);
            // sondes capacitives
            gc.setFill(javafx.scene.paint.Color.web("#c0c0c0"));
            for (int i = 0; i < 5; i++) gc.fillRoundRect(3+i*7, 23, 4, 15, 3, 3);
            gc.setFill(javafx.scene.paint.Color.web("#00cc44")); gc.fillOval(31,6,4,4);
            gc.setFill(javafx.scene.paint.Color.web("#ffcc00")); gc.fillOval(31,12,4,4);
        } else {
            // Boitier IoT generique noir
            gc.setFill(javafx.scene.paint.Color.web("#444444")); gc.fillRoundRect(3,2,32,35,6,6);
            gc.setFill(javafx.scene.paint.Color.web("#555555")); gc.fillRoundRect(5,4,28,30,5,5);
            gc.setFill(javafx.scene.paint.Color.web("#111111")); gc.fillRoundRect(7,6,24,14,2,2);
            gc.setFill(javafx.scene.paint.Color.web("#001a00")); gc.fillRoundRect(8,7,22,12,2,2);
            gc.setFill(javafx.scene.paint.Color.web("#00ff44"));
            gc.setFont(javafx.scene.text.Font.font("Monospace", javafx.scene.text.FontWeight.BOLD, 5));
            gc.fillText("23.4", 10, 14);
            gc.setFont(javafx.scene.text.Font.font("Monospace", 3));
            gc.setFill(javafx.scene.paint.Color.web("#00aa22")); gc.fillText("TEMP C", 10, 18);
            gc.setFill(javafx.scene.paint.Color.web("#00cc44")); gc.fillOval(8,24,5,5);
            gc.setFill(javafx.scene.paint.Color.web("#ffaa00")); gc.fillOval(17,24,5,5);
            gc.setFill(javafx.scene.paint.Color.web("#3366ff")); gc.fillOval(26,24,5,5);
        }
        return canvas;
    }

    private HBox creerLigneCapteur(Capteur c, int index) {

        String statut = c.getStatut();
        String stripColor, badgeBg, hoverBg, hoverBorder;
        switch (statut) {
            case "actif"    -> { stripColor="#16a34a"; badgeBg="#16a34a"; hoverBg="#f0fdf4"; hoverBorder="#bbf7d0"; }
            case "en_panne" -> { stripColor="#dc2626"; badgeBg="#dc2626"; hoverBg="#fff5f5"; hoverBorder="#fecaca"; }
            default         -> { stripColor="#94a3b8"; badgeBg="#64748b"; hoverBg="#f8fafc"; hoverBorder="#e2e8f0"; }
        }

        // ── Mesures temp + humidité depuis la BD ──
        edu.capteur.services.MesureCapteurService mesureService =
            new edu.capteur.services.MesureCapteurService();
        edu.capteur.services.MesureCapteurService.DerniereMesure mesure =
            mesureService.getDerniereMesure(c.getId());

        HBox mesureRow = new HBox(8);
        mesureRow.setAlignment(Pos.CENTER_LEFT);
        if (mesure.disponible) {
            String tempColor = mesure.temperature > 55 ? "#dc2626" : mesure.temperature > 40 ? "#f59e0b" : "#16a34a";
            String tempBg    = mesure.temperature > 55 ? "#fee2e2" : mesure.temperature > 40 ? "#fef3c7" : "#dcfce7";
            String humColor  = mesure.humidite > 90 ? "#dc2626" : mesure.humidite > 75 ? "#f59e0b" : "#0284c7";
            String humBg     = mesure.humidite > 90 ? "#fee2e2" : mesure.humidite > 75 ? "#fef3c7" : "#dbeafe";
            Label lblTemp = new Label(String.format("🌡 %.1f°C", mesure.temperature));
            lblTemp.setStyle("-fx-font-size:10;-fx-font-weight:bold;-fx-text-fill:" + tempColor +
                             ";-fx-background-color:" + tempBg + ";-fx-background-radius:10;-fx-padding:2 7;");
            Label lblHumid = new Label(String.format("💧 %d%%", mesure.humidite));
            lblHumid.setStyle("-fx-font-size:10;-fx-font-weight:bold;-fx-text-fill:" + humColor +
                              ";-fx-background-color:" + humBg + ";-fx-background-radius:10;-fx-padding:2 7;");
            mesureRow.getChildren().addAll(lblTemp, lblHumid);
        } else {
            Label lblNoData = new Label("Aucune mesure disponible");
            lblNoData.setStyle("-fx-font-size:10;-fx-text-fill:#cbd5e1;-fx-font-style:italic;");
            mesureRow.getChildren().add(lblNoData);
        }

        // ── Conteneur principal ──
        HBox carte = new HBox(0);
        carte.setAlignment(Pos.CENTER_LEFT);
        carte.setStyle("-fx-background-color:white;-fx-border-color:#f1f5f9;-fx-border-width:0 0 1 0;");
        final String hBg = hoverBg, hBd = hoverBorder;
        carte.setOnMouseEntered(e -> carte.setStyle("-fx-background-color:" + hBg + ";-fx-border-color:" + hBd + ";-fx-border-width:0 0 1 0;"));
        carte.setOnMouseExited(e  -> carte.setStyle("-fx-background-color:white;-fx-border-color:#f1f5f9;-fx-border-width:0 0 1 0;"));

        // ── Barre colorée verticale ──
        javafx.scene.layout.Region strip = new javafx.scene.layout.Region();
        strip.setMinWidth(4); strip.setMaxWidth(4);
        strip.setStyle("-fx-background-color:" + stripColor + ";");

        // ── Contenu interne ──
        HBox inner = new HBox(12);
        inner.setAlignment(Pos.CENTER_LEFT);
        inner.setStyle("-fx-padding:13 16 13 14;");
        HBox.setHgrow(inner, javafx.scene.layout.Priority.ALWAYS);

        // ── Avatar ──
        String initiales = c.getNom().length() >= 2 ? c.getNom().substring(0,2).toUpperCase() : c.getNom().toUpperCase();
        String[] avatarColors = {"#16a34a","#0284c7","#7c3aed","#d97706","#dc2626","#0891b2"};
        int ci = Math.abs(c.getType().hashCode()) % avatarColors.length;
        Label avatar = new Label(initiales);
        avatar.setMinSize(44,44); avatar.setMaxSize(44,44);
        avatar.setAlignment(Pos.CENTER);
        avatar.setStyle("-fx-background-color:" + avatarColors[ci] + ";-fx-background-radius:50;-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:14;");
        StackPane avatarWrap = new StackPane(avatar);
        avatarWrap.setMinSize(44,44); avatarWrap.setMaxSize(44,44);

        // ── Bloc infos ──
        VBox infos = new VBox(4);
        HBox.setHgrow(infos, javafx.scene.layout.Priority.ALWAYS);

        Label lblNom = new Label(c.getNom());
        lblNom.setFont(Font.font("System", FontWeight.BOLD, 14));
        lblNom.setTextFill(Color.web("#0f172a"));

        Label lblType = new Label(c.getType());
        lblType.setStyle("-fx-background-color:#ede9fe;-fx-text-fill:#7c3aed;-fx-font-size:10;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:2 8;");
        Foret foretCapteur = foretService.getById(c.getForetId());
        String nomForet = foretCapteur != null ? foretCapteur.getNom() : "Forêt #" + c.getForetId();
        Label lblForet = new Label("🌲 " + nomForet);
        lblForet.setStyle("-fx-font-size:12;-fx-text-fill:#16a34a;-fx-font-weight:bold;");
        HBox ligne2 = new HBox(8, lblType, lblForet);
        ligne2.setAlignment(Pos.CENTER_LEFT);

        Label lblLoc = new Label("📍 " + c.getLocalisation());
        lblLoc.setStyle("-fx-font-size:11;-fx-text-fill:#64748b;");
        Label lblId = new Label("  ·  #C" + c.getId());
        lblId.setStyle("-fx-font-size:11;-fx-text-fill:#cbd5e1;");
        HBox ligne3 = new HBox(0, lblLoc, lblId);
        ligne3.setAlignment(Pos.CENTER_LEFT);

        infos.getChildren().addAll(lblNom, ligne2, ligne3, mesureRow);

        // ── Bloc droite ──
        VBox droite = new VBox(8);
        droite.setAlignment(Pos.CENTER_RIGHT);

        // Badge statut avec point pulsant
        HBox badgeBox = new HBox(6);
        badgeBox.setAlignment(Pos.CENTER);
        badgeBox.setStyle("-fx-background-color:" + badgeBg + ";-fx-background-radius:20;-fx-padding:5 12;");

        javafx.scene.layout.StackPane pulseWrap = new javafx.scene.layout.StackPane();
        pulseWrap.setMinSize(8,8); pulseWrap.setMaxSize(8,8);
        javafx.scene.layout.Region outerRing = new javafx.scene.layout.Region();
        outerRing.setMinSize(8,8); outerRing.setMaxSize(8,8);
        outerRing.setStyle("-fx-background-color:rgba(255,255,255,0.45);-fx-background-radius:50;");
        javafx.scene.layout.Region innerDot = new javafx.scene.layout.Region();
        innerDot.setMinSize(5,5); innerDot.setMaxSize(5,5);
        innerDot.setStyle("-fx-background-color:white;-fx-background-radius:50;");
        pulseWrap.getChildren().addAll(outerRing, innerDot);

        if ("actif".equals(statut) || "en_panne".equals(statut)) {
            int dur = "actif".equals(statut) ? 1500 : 650;
            Timeline pulse = new Timeline(
                new KeyFrame(Duration.ZERO,
                    new KeyValue(outerRing.scaleXProperty(), 1.0),
                    new KeyValue(outerRing.scaleYProperty(), 1.0),
                    new KeyValue(outerRing.opacityProperty(), 0.8)),
                new KeyFrame(Duration.millis(dur),
                    new KeyValue(outerRing.scaleXProperty(), 2.4),
                    new KeyValue(outerRing.scaleYProperty(), 2.4),
                    new KeyValue(outerRing.opacityProperty(), 0.0))
            );
            pulse.setCycleCount(Timeline.INDEFINITE);
            pulse.play();
        }

        Label lblStatutTxt = new Label(statut.toUpperCase().replace("_", " "));
        lblStatutTxt.setStyle("-fx-font-size:10;-fx-font-weight:bold;-fx-text-fill:white;");
        badgeBox.getChildren().addAll(pulseWrap, lblStatutTxt);

        // Boutons
        Button btnE = new Button("✏");
        btnE.setStyle("-fx-background-color:#ede9fe;-fx-text-fill:#7c3aed;-fx-background-radius:6;-fx-font-size:11;-fx-cursor:hand;-fx-min-width:30;-fx-min-height:28;");
        btnE.setOnAction(e -> ouvrirModification(c));

        Button btnX = new Button("✕");
        btnX.setStyle("-fx-background-color:#fee2e2;-fx-text-fill:#dc2626;-fx-background-radius:6;-fx-font-size:11;-fx-cursor:hand;-fx-min-width:30;-fx-min-height:28;");
        btnX.setOnAction(e -> {
            if (confirmerSuppressionCapteur(c)) {
                capteurService.supprimer(c.getId());
                chargerCapteurs();
                afficherToast("Capteur \"" + c.getNom() + "\" supprimé", "error");
            }
        });

        Button btnAI = new Button("🤖");
        if ("actif".equals(statut)) {
            btnAI.setStyle("-fx-background-color:#ecfdf5;-fx-text-fill:#059669;-fx-background-radius:6;-fx-font-size:11;-fx-cursor:hand;-fx-min-width:30;-fx-min-height:28;");
            btnAI.setTooltip(new Tooltip("Prédiction IA – Analyser le risque de panne"));
            btnAI.setOnAction(e -> ouvrirPredictionIA(c));
        } else {
            btnAI.setStyle("-fx-background-color:#f1f5f9;-fx-text-fill:#cbd5e1;-fx-background-radius:6;-fx-font-size:11;-fx-cursor:default;-fx-min-width:30;-fx-min-height:28;");
            btnAI.setTooltip(new Tooltip("en_panne".equals(statut) ? "Capteur en panne – prédiction non applicable" : "Capteur inactif – prédiction non applicable"));
            btnAI.setDisable(true);
        }

        HBox btnBox = new HBox(5, btnE, btnX, btnAI);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        droite.getChildren().addAll(badgeBox, btnBox);

        inner.getChildren().addAll(avatarWrap, infos, droite);
        carte.getChildren().addAll(strip, inner);
        return carte;
    }

    /**
     * Affiche un toast non-bloquant en bas à droite de la fenêtre principale.
     * @param message  texte à afficher
     * @param type     "success" | "error" | "info"
     */
    private void afficherToast(String message, String type) {
        try {
            // Récupérer la scène principale
            javafx.scene.Scene scene = listeCapteurs.getScene();
            if (scene == null) return;
            javafx.scene.layout.Pane root = (javafx.scene.layout.Pane) scene.getRoot();

            // Couleurs selon le type
            String bg, border, textColor, emoji;
            switch (type) {
                case "success" -> { bg="#f0fdf4"; border="#86efac"; textColor="#15803d"; emoji="✅"; }
                case "error"   -> { bg="#fff5f5"; border="#fca5a5"; textColor="#dc2626"; emoji="🗑"; }
                default        -> { bg="#eff6ff"; border="#93c5fd"; textColor="#1d4ed8"; emoji="ℹ"; }
            }

            // Créer le toast
            javafx.scene.layout.HBox toast = new javafx.scene.layout.HBox(10);
            toast.setAlignment(Pos.CENTER_LEFT);
            toast.setStyle(
                "-fx-background-color: " + bg + ";" +
                "-fx-border-color: " + border + ";" +
                "-fx-border-radius: 12; -fx-background-radius: 12;" +
                "-fx-border-width: 1; -fx-padding: 12 18;" +
                "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.18),14,0,0,4);"
            );

            Label lblEmoji = new Label(emoji);
            lblEmoji.setStyle("-fx-font-size: 14;");

            Label lblMsg = new Label(message);
            lblMsg.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: " + textColor + ";");

            toast.getChildren().addAll(lblEmoji, lblMsg);

            // Positionner en bas à droite
            toast.setLayoutX(scene.getWidth() - 340);
            toast.setLayoutY(scene.getHeight() - 70);
            toast.setOpacity(0);
            root.getChildren().add(toast);

            // Animation : slide up + fade in → attendre → fade out
            javafx.animation.TranslateTransition slideIn = new javafx.animation.TranslateTransition(Duration.millis(250), toast);
            slideIn.setFromY(20); slideIn.setToY(0);

            javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(Duration.millis(250), toast);
            fadeIn.setFromValue(0); fadeIn.setToValue(1);

            javafx.animation.ParallelTransition entree = new javafx.animation.ParallelTransition(slideIn, fadeIn);

            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(Duration.millis(2800));

            javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(Duration.millis(300), toast);
            fadeOut.setFromValue(1); fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> root.getChildren().remove(toast));

            javafx.animation.SequentialTransition seq = new javafx.animation.SequentialTransition(entree, pause, fadeOut);
            seq.play();

        } catch (Exception ex) {
            System.out.println("Toast: " + message);
        }
    }

    // ════════════════════════════════════════
    //  ✅ NOUVEAU – Prédiction IA
    // ════════════════════════════════════════
    private void ouvrirPredictionIA(Capteur c) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/prediction_ia.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load(), 600, 680));
            stage.setTitle("🤖 Prédiction IA – " + c.getNom());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(true);

            AIPredictionController ctrl = loader.getController();
            // Donnée récente selon le type du capteur (sans batterie)
            String donnees = switch (c.getType().toLowerCase()) {
                case "temperature" -> "Température mesurée: " + (int)(Math.random()*30+20) + "°C";
                case "fumee"       -> "Particules PM2.5: " + (int)(Math.random()*150) + " µg/m³";
                case "humidite"    -> "Humidité sol: " + (int)(Math.random()*60+10) + "%";
                default            -> "Signal: " + (int)(Math.random()*100) + "%";
            };
            ctrl.setCapteur(c, 0, donnees); // batterie ignorée
            stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

    // ════════════════════════════════════════
    //  ✅ NOUVEAU – Ouvrir Carte OpenStreetMap
    // ════════════════════════════════════════
    // Référence au MapController si la carte est ouverte
    private MapController mapControllerOuvert = null;

    @FXML
    public void ouvrirCarte() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/carte.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load(), 1100, 750));
            stage.setTitle("🗺️ ForestGuard – Carte des Capteurs");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(true);
            mapControllerOuvert = loader.getController();
            stage.setOnHidden(e -> mapControllerOuvert = null);
            stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

    // ════════════════════════════════════════
    //  CAPTEURS – Recherche & Ajout
    // ════════════════════════════════════════
    @FXML
    public void rechercher() {
        String mot = tfRecherche.getText().toLowerCase();
        afficherCapteurs(tousLesCapteurs.stream()
            .filter(c -> c.getNom().toLowerCase().contains(mot)
                      || c.getType().toLowerCase().contains(mot)
                      || c.getLocalisation().toLowerCase().contains(mot))
            .collect(Collectors.toList()));
    }

    @FXML
    public void ouvrirAjout() {
        try {
            Stage stage = new Stage();
            stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("/ajout_capteur.fxml"))));
            stage.sizeToScene();
            stage.setTitle("Ajouter un capteur");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            int avantAjout = capteurService.afficher().size();
            stage.showAndWait();
            chargerCapteurs();
            if (capteurService.afficher().size() > avantAjout) {
                afficherToast("Capteur ajouté avec succès", "success");
                // Mettre à jour la carte si elle est ouverte
                if (mapControllerOuvert != null) {
                    mapControllerOuvert.rechargerNouveauxCapteurs();
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void ouvrirModification(Capteur c) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/modifier_capteur.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            stage.sizeToScene();
            stage.setTitle("Modifier un capteur");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            ((ModifierCapteurController) loader.getController()).setCapteur(c);
            stage.showAndWait();
            chargerCapteurs();
            afficherToast("Capteur \"" + c.getNom() + "\" modifié", "info");
        } catch (IOException e) { e.printStackTrace(); }
    }

    // ════════════════════════════════════════
    //  MAINTENANCES
    // ════════════════════════════════════════
    private void chargerMaintenances() {
        toutesMaintenances = maintenanceService.afficher();
        afficherMaintenances(toutesMaintenances);
        long enCours   = toutesMaintenances.stream().filter(m -> "en_cours".equals(m.getStatut())).count();
        long planifiees = toutesMaintenances.stream().filter(m -> "planifiee".equals(m.getStatut())).count();
        lblTotalM.setText(String.valueOf(toutesMaintenances.size()));
        if (lblTotalM != null) {
            lblTotalM.setTooltip(new Tooltip(
                toutesMaintenances.size() + " maintenances\n" +
                planifiees + " planifiées · " + enCours + " en cours"
            ));
        }
    }

    private void afficherMaintenances(List<Maintenance> liste) {
        listeMaintenances.getChildren().clear();
        if (liste.isEmpty()) {
            listeMaintenances.getChildren().add(creerEmptyState(
                "🔧", "Aucune maintenance trouvée",
                tfRechercheM != null && !tfRechercheM.getText().isEmpty()
                    ? "Aucun résultat pour \"" + tfRechercheM.getText() + "\""
                    : "Planifiez votre première maintenance avec le bouton ＋",
                "#d97706"
            ));
            return;
        }
        int idx = 0;
        for (Maintenance m : liste)
            listeMaintenances.getChildren().add(creerLigneMaintenance(m, idx++));
    }

    /**
     * Crée un empty state illustré pour les listes vides.
     */
    private VBox creerEmptyState(String emoji, String titre, String sousTitre, String couleur) {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-padding: 48 24;");

        Label lblEmoji = new Label(emoji);
        lblEmoji.setStyle("-fx-font-size: 40; -fx-opacity: 0.5;");

        Label lblTitre = new Label(titre);
        lblTitre.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #334155;");

        Label lblSous = new Label(sousTitre);
        lblSous.setStyle("-fx-font-size: 12; -fx-text-fill: #94a3b8; -fx-text-alignment: center;");
        lblSous.setWrapText(true);
        lblSous.setMaxWidth(320);

        // Ligne décorative
        javafx.scene.layout.Region ligne = new javafx.scene.layout.Region();
        ligne.setMinHeight(3); ligne.setMaxHeight(3);
        ligne.setMinWidth(40); ligne.setMaxWidth(40);
        ligne.setStyle("-fx-background-color: " + couleur + "; -fx-background-radius: 2; -fx-opacity: 0.4;");

        box.getChildren().addAll(lblEmoji, ligne, lblTitre, lblSous);

        // Fade-in
        box.setOpacity(0);
        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(Duration.millis(400), box);
        ft.setFromValue(0); ft.setToValue(1); ft.play();

        return box;
    }

    private HBox creerLigneMaintenance(Maintenance m, int index) {
        HBox carte = new HBox(14);
        carte.setAlignment(Pos.CENTER_LEFT);
        carte.setStyle("-fx-background-color: white;" +
                       "-fx-border-color: #fef3c7; -fx-border-width: 0 0 1 0;" +
                       "-fx-padding: 14 18;");
        carte.setOnMouseEntered(e -> carte.setStyle(
            "-fx-background-color: #fffbeb; -fx-border-color: #fde68a;" +
            "-fx-border-width: 0 0 1 0; -fx-padding: 14 18;"));
        carte.setOnMouseExited(e -> carte.setStyle(
            "-fx-background-color: white; -fx-border-color: #fef3c7;" +
            "-fx-border-width: 0 0 1 0; -fx-padding: 14 18;"));

        // ── Avatar rond 🔧 ──
        Label avatar = new Label("🔧");
        avatar.setMinSize(46, 46); avatar.setMaxSize(46, 46);
        avatar.setAlignment(Pos.CENTER);
        avatar.setStyle("-fx-background-color: #fef3c7; -fx-background-radius: 50;" +
                        "-fx-font-size: 18;");

        // ── Bloc infos ──
        VBox infos = new VBox(4);
        HBox.setHgrow(infos, javafx.scene.layout.Priority.ALWAYS);

        // Ligne 1 : Type en gras
        Label lblType = new Label(m.getTypeMaintenance().toUpperCase());
        lblType.setFont(Font.font("System", FontWeight.BOLD, 14));
        lblType.setTextFill(Color.web("#1e293b"));

        // Ligne 2 : date + description
        String dateStr = m.getDateMaintenance() != null
            ? m.getDateMaintenance().substring(0, Math.min(10, m.getDateMaintenance().length())) : "";
        Label lblDate = new Label("📅 " + dateStr);
        lblDate.setStyle("-fx-font-size: 12; -fx-text-fill: #64748b;");
        Label lblDesc = new Label("📝 " + (m.getDescription() != null ? m.getDescription() : ""));
        lblDesc.setStyle("-fx-font-size: 12; -fx-text-fill: #475569;");
        HBox ligne2 = new HBox(14, lblDate, lblDesc);
        ligne2.setAlignment(Pos.CENTER_LEFT);

        // Ligne 3 : capteur concerné + ID maintenance
        Capteur capteurLie = capteurService.afficher().stream()
            .filter(cap -> cap.getId() == m.getCapteurId())
            .findFirst().orElse(null);
        String nomCapteur = capteurLie != null
            ? capteurLie.getNom() + " · " + capteurLie.getType()
            : "Capteur #" + m.getCapteurId();
        Label lblCapteur = new Label("📡 " + nomCapteur);
        lblCapteur.setStyle("-fx-font-size: 11; -fx-text-fill: #0284c7; -fx-font-weight: bold;");
        Label lblId = new Label("  ·  #M" + m.getId());
        lblId.setStyle("-fx-font-size: 11; -fx-text-fill: #94a3b8;");
        HBox ligne3 = new HBox(0, lblCapteur, lblId);
        ligne3.setAlignment(Pos.CENTER_LEFT);

        infos.getChildren().addAll(lblType, ligne2, ligne3);

        // ── Droite : statut + boutons ──
        VBox droite = new VBox(8);
        droite.setAlignment(Pos.CENTER_RIGHT);

        String sBg = switch (m.getStatut()) {
            case "planifiee" -> "#d97706";
            case "en_cours"  -> "#16a34a";
            case "terminee"  -> "#64748b";
            default          -> "#64748b";
        };
        Label lblStatut = new Label(m.getStatut().toUpperCase().replace("_", " "));
        lblStatut.setStyle("-fx-background-color: " + sBg + "; -fx-text-fill: white;" +
                           "-fx-font-size: 10; -fx-font-weight: bold;" +
                           "-fx-background-radius: 20; -fx-padding: 4 12;");

        Button btnE = new Button("✏");
        btnE.setStyle("-fx-background-color: #fef3c7; -fx-text-fill: #d97706;" +
                      "-fx-background-radius: 6; -fx-font-size: 11; -fx-cursor: hand;" +
                      "-fx-min-width: 30; -fx-min-height: 28;");
        btnE.setOnAction(e -> ouvrirModificationMaintenance(m));

        Button btnX = new Button("✕");
        btnX.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #dc2626;" +
                      "-fx-background-radius: 6; -fx-font-size: 11; -fx-cursor: hand;" +
                      "-fx-min-width: 30; -fx-min-height: 28;");
        btnX.setOnAction(e -> {
            if (confirmerSuppressionMaintenance(m)) {
                maintenanceService.supprimer(m.getId());
                chargerMaintenances();
                afficherToast("Maintenance #M" + m.getId() + " supprimée", "error");
            }
        });

        HBox btnBox = new HBox(5, btnE, btnX);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        droite.getChildren().addAll(lblStatut, btnBox);

        carte.getChildren().addAll(avatar, infos, droite);
        return carte;
    }

    // ─── Style actif / inactif pour boutons filtre ───────────────────────
    private void majStylesBoutonsFiltres() {
        // Base commun inactif
        String baseInactif = "-fx-background-color: transparent; -fx-font-size: 12;" +
                             "-fx-font-weight: bold; -fx-padding: 0 16;" +
                             "-fx-background-radius: 9; -fx-cursor: hand;";
        // Base actif : fond coloré + texte blanc
        String styleAll = "tous".equals(filtreStatutActif)
            ? "-fx-background-color: #334155; -fx-text-fill: white; -fx-font-size: 12;" +
              "-fx-font-weight: bold; -fx-padding: 0 18; -fx-background-radius: 9; -fx-cursor: hand;"
            : "-fx-background-color: rgba(255,255,255,0.0); -fx-text-fill: #64748b; -fx-font-size: 12;" +
              "-fx-font-weight: bold; -fx-padding: 0 18; -fx-background-radius: 9; -fx-cursor: hand;";

        String stylePlanifie = "planifiee".equals(filtreStatutActif)
            ? "-fx-background-color: #d97706; -fx-text-fill: white; -fx-font-size: 12;" +
              "-fx-font-weight: bold; -fx-padding: 0 16; -fx-background-radius: 9; -fx-cursor: hand;"
            : baseInactif + "-fx-text-fill: #92400e;";

        String styleEnCours = "en_cours".equals(filtreStatutActif)
            ? "-fx-background-color: #16a34a; -fx-text-fill: white; -fx-font-size: 12;" +
              "-fx-font-weight: bold; -fx-padding: 0 16; -fx-background-radius: 9; -fx-cursor: hand;"
            : baseInactif + "-fx-text-fill: #14532d;";

        String styleTermine = "terminee".equals(filtreStatutActif)
            ? "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-size: 12;" +
              "-fx-font-weight: bold; -fx-padding: 0 16; -fx-background-radius: 9; -fx-cursor: hand;"
            : baseInactif + "-fx-text-fill: #1e3a5f;";

        if (btnFiltreAll      != null) btnFiltreAll.setStyle(styleAll);
        if (btnFiltrePlanifie != null) btnFiltrePlanifie.setStyle(stylePlanifie);
        if (btnFiltreEnCours  != null) btnFiltreEnCours.setStyle(styleEnCours);
        if (btnFiltreTermine  != null) btnFiltreTermine.setStyle(styleTermine);
    }

    private void appliquerFiltreEtRecherche() {
        String mot = tfRechercheM != null ? tfRechercheM.getText().toLowerCase() : "";
        afficherMaintenances(toutesMaintenances.stream()
            .filter(m -> "tous".equals(filtreStatutActif) || m.getStatut().equals(filtreStatutActif))
            .filter(m -> mot.isEmpty()
                      || m.getTypeMaintenance().toLowerCase().contains(mot)
                      || m.getStatut().toLowerCase().contains(mot)
                      || (m.getDescription() != null && m.getDescription().toLowerCase().contains(mot)))
            .collect(Collectors.toList()));
    }

    @FXML public void filtrerMaintenanceTous()      { filtreStatutActif = "tous";      majStylesBoutonsFiltres(); appliquerFiltreEtRecherche(); }
    @FXML public void filtrerMaintenancePlanifie()  { filtreStatutActif = "planifiee"; majStylesBoutonsFiltres(); appliquerFiltreEtRecherche(); }
    @FXML public void filtrerMaintenanceEnCours()   { filtreStatutActif = "en_cours";  majStylesBoutonsFiltres(); appliquerFiltreEtRecherche(); }
    @FXML public void filtrerMaintenanceTermine()   { filtreStatutActif = "terminee";  majStylesBoutonsFiltres(); appliquerFiltreEtRecherche(); }

    @FXML
    public void rechercherMaintenance() {
        appliquerFiltreEtRecherche();
    }

    @FXML
    public void ouvrirAjoutMaintenance() {
        try {
            Stage stage = new Stage();
            stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("/ajout_maintenance.fxml")), 680, 460));
            stage.setTitle("Ajouter une maintenance");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();
            chargerMaintenances();
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void ouvrirModificationMaintenance(Maintenance m) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/modifier_maintenance.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load(), 680, 460));
            stage.setTitle("Modifier une maintenance");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            ((ModifierMaintenanceController) loader.getController()).setMaintenance(m);
            stage.showAndWait();
            chargerMaintenances();
        } catch (IOException e) { e.printStackTrace(); }
    }

    // ════════════════════════════════════════
    //  SIDEBAR TOGGLE
    // ════════════════════════════════════════
    @FXML
    public void toggleSidebar() {
        sidebarExpanded = !sidebarExpanded;
        double toWidth = sidebarExpanded ? W_OPEN : W_CLOSED;
        Timeline anim = new Timeline(new KeyFrame(Duration.millis(220),
            new KeyValue(sidebar.prefWidthProperty(), toWidth)));
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
            {btnPM, "🚒  Pompiers",   "🚒"},
            {btnFR, "🌲  Forets",     "🌲"},
            {btnCP, "ForestGuard - Gestion des Capteurs", "📡"},
            {btnDN, "📊  Donnees",    "📊"},
            {btnAL, "🔔  Alertes",    "🔔"},
            {btnUS, "👤  Utilisateurs","👤"}
        };
        for (Object[] entry : btns) {
            Button b = (Button) entry[0];
            if (b == null) continue;
            b.setText((String)(expanded ? entry[1] : entry[2]));
            b.setPrefWidth(expanded ? W_OPEN - 20 : 52);
            boolean isActive = (b == btnCP);
            b.setStyle("-fx-background-color: " + (isActive ? "#16a34a" : "transparent") + ";" +
                "-fx-text-fill: " + (isActive ? "white" : "#94a3b8") + ";" +
                "-fx-font-size: 13;" + (isActive ? "-fx-font-weight: bold;" : "") +
                "-fx-alignment: " + (expanded ? "CENTER_LEFT" : "CENTER") + ";" +
                "-fx-padding: 10 16; -fx-background-radius: 12; -fx-cursor: hand;" +
                (isActive ? "-fx-effect: dropshadow(gaussian,rgba(22,163,74,0.4),10,0,0,3);" : ""));
        }
    }

    // ════════════════════════════════════════
    //  DIALOGUES DE SUPPRESSION CUSTOM
    // ════════════════════════════════════════

    /**
     * Dialogue de confirmation de suppression d'un capteur.
     */
    private boolean confirmerSuppressionCapteur(Capteur c) {
        Foret foret = foretService.getById(c.getForetId());
        String nomForet = foret != null ? foret.getNom() : "Forêt #" + c.getForetId();
        String locForet = foret != null ? foret.getLocalisation() : "";

        Stage dialog = new Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("Supprimer le capteur");
        dialog.setResizable(false);

        // ── HEADER avec dégradé rouge ──
        VBox header = new VBox(6);
        header.setStyle("-fx-background-color: linear-gradient(to right, #991b1b, #dc2626);" +
                        "-fx-padding: 22 24 18 24;");
        HBox headerTop = new HBox(14);
        headerTop.setAlignment(Pos.CENTER_LEFT);

        // Icône dans cercle blanc semi-transparent
        javafx.scene.layout.StackPane iconCircle = new javafx.scene.layout.StackPane();
        iconCircle.setMinSize(48, 48); iconCircle.setMaxSize(48, 48);
        iconCircle.setStyle("-fx-background-color: rgba(255,255,255,0.18);" +
                            "-fx-background-radius: 50;");
        Label iconLbl = new Label("🗑");
        iconLbl.setStyle("-fx-font-size: 20;");
        iconCircle.getChildren().add(iconLbl);

        VBox titreBox = new VBox(3);
        Label titre = new Label("Supprimer le capteur");
        titre.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: white;");
        Label sousTitre = new Label("Cette action est définitive et irréversible");
        sousTitre.setStyle("-fx-font-size: 11; -fx-text-fill: rgba(255,255,255,0.75);");
        titreBox.getChildren().addAll(titre, sousTitre);
        headerTop.getChildren().addAll(iconCircle, titreBox);

        // Badge nom du capteur dans le header
        Label badgeNom = new Label("  " + c.getNom() + "  ");
        badgeNom.setStyle("-fx-background-color: rgba(255,255,255,0.2);" +
                          "-fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold;" +
                          "-fx-background-radius: 20; -fx-padding: 4 12;");
        header.getChildren().addAll(headerTop, badgeNom);

        // ── CORPS : grille 2 colonnes ──
        VBox corps = new VBox(14);
        corps.setStyle("-fx-background-color: #f8fafc; -fx-padding: 20 24 16 24;");

        Label lblSection = new Label("INFORMATIONS DU CAPTEUR");
        lblSection.setStyle("-fx-font-size: 9; -fx-font-weight: bold; -fx-text-fill: #94a3b8;" +
                            "-fx-letter-spacing: 1.5;");

        // Grille 2×3
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(12); grid.setVgap(0);
        grid.setStyle("-fx-background-color: white; -fx-background-radius: 14;" +
                      "-fx-border-color: #fee2e2; -fx-border-radius: 14; -fx-border-width: 1;" +
                      "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.06),8,0,0,2);");
        grid.setPadding(new javafx.geometry.Insets(0));

        // Colonne gauche
        grid.add(celluleInfo("🔢", "ID", "#C" + c.getId(), null, true), 0, 0);
        grid.add(celluleInfo("🔬", "Type", c.getType(), null, false), 0, 1);
        grid.add(celluleInfo("⚡", "Statut", c.getStatut().toUpperCase().replace("_", " "),
                             statutColorCapteur(c.getStatut()), false), 0, 2);
        // Colonne droite
        grid.add(celluleInfo("📛", "Nom", c.getNom(), null, true), 1, 0);
        grid.add(celluleInfo("📍", "Localisation", c.getLocalisation(), null, false), 1, 1);
        grid.add(celluleInfo("🌲", "Forêt", nomForet + (locForet.isEmpty() ? "" : " · " + locForet),
                             null, false), 1, 2);

        javafx.scene.layout.ColumnConstraints col1 = new javafx.scene.layout.ColumnConstraints();
        col1.setPercentWidth(50);
        javafx.scene.layout.ColumnConstraints col2 = new javafx.scene.layout.ColumnConstraints();
        col2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(col1, col2);

        corps.getChildren().addAll(lblSection, grid);

        // ── AVERTISSEMENT ──
        HBox warning = new HBox(10);
        warning.setAlignment(Pos.CENTER_LEFT);
        warning.setStyle("-fx-background-color: #fff1f2; -fx-padding: 12 24;" +
                         "-fx-border-color: #fecdd3; -fx-border-width: 1 0 0 0;");
        Label warnIcon = new Label("⚠");
        warnIcon.setStyle("-fx-font-size: 13; -fx-text-fill: #e11d48;");
        Label warnText = new Label("Les maintenances associées à ce capteur seront également supprimées.");
        warnText.setStyle("-fx-font-size: 11; -fx-text-fill: #9f1239; -fx-wrap-text: true;");
        warnText.setMaxWidth(360);
        warning.getChildren().addAll(warnIcon, warnText);

        // ── BOUTONS ──
        HBox boutons = new HBox(10);
        boutons.setAlignment(Pos.CENTER_RIGHT);
        boutons.setStyle("-fx-padding: 14 24 18 24; -fx-background-color: white;" +
                         "-fx-border-color: #f1f5f9; -fx-border-width: 1 0 0 0;");

        Button btnAnnuler = new Button("Annuler");
        btnAnnuler.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569;" +
                            "-fx-font-size: 13; -fx-font-weight: bold;" +
                            "-fx-background-radius: 10; -fx-border-color: #e2e8f0;" +
                            "-fx-border-radius: 10; -fx-cursor: hand; -fx-padding: 9 20;");
        btnAnnuler.setOnMouseEntered(e -> btnAnnuler.setStyle(
            "-fx-background-color: #e2e8f0; -fx-text-fill: #1e293b;" +
            "-fx-font-size: 13; -fx-font-weight: bold;" +
            "-fx-background-radius: 10; -fx-border-color: #cbd5e1;" +
            "-fx-border-radius: 10; -fx-cursor: hand; -fx-padding: 9 20;"));
        btnAnnuler.setOnMouseExited(e -> btnAnnuler.setStyle(
            "-fx-background-color: #f1f5f9; -fx-text-fill: #475569;" +
            "-fx-font-size: 13; -fx-font-weight: bold;" +
            "-fx-background-radius: 10; -fx-border-color: #e2e8f0;" +
            "-fx-border-radius: 10; -fx-cursor: hand; -fx-padding: 9 20;"));

        Button btnSupprimer = new Button("🗑  Supprimer définitivement");
        btnSupprimer.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white;" +
                              "-fx-font-size: 13; -fx-font-weight: bold;" +
                              "-fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 9 22;" +
                              "-fx-effect: dropshadow(gaussian,rgba(220,38,38,0.45),10,0,0,3);");
        btnSupprimer.setOnMouseEntered(e -> btnSupprimer.setStyle(
            "-fx-background-color: #b91c1c; -fx-text-fill: white;" +
            "-fx-font-size: 13; -fx-font-weight: bold;" +
            "-fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 9 22;" +
            "-fx-effect: dropshadow(gaussian,rgba(185,28,28,0.55),12,0,0,4);"));
        btnSupprimer.setOnMouseExited(e -> btnSupprimer.setStyle(
            "-fx-background-color: #dc2626; -fx-text-fill: white;" +
            "-fx-font-size: 13; -fx-font-weight: bold;" +
            "-fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 9 22;" +
            "-fx-effect: dropshadow(gaussian,rgba(220,38,38,0.45),10,0,0,3);"));

        final boolean[] confirmed = {false};
        btnAnnuler.setOnAction(e -> dialog.close());
        btnSupprimer.setOnAction(e -> { confirmed[0] = true; dialog.close(); });
        boutons.getChildren().addAll(btnAnnuler, btnSupprimer);

        VBox root = new VBox(0, header, corps, warning, boutons);
        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        dialog.setScene(scene);
        dialog.setMinWidth(480);
        dialog.sizeToScene();
        dialog.showAndWait();
        return confirmed[0];
    }

    /**
     * Dialogue de confirmation de suppression d'une maintenance.
     */
    private boolean confirmerSuppressionMaintenance(Maintenance m) {
        Capteur capteurLie = null;
        for (Capteur cap : capteurService.afficher()) {
            if (cap.getId() == m.getCapteurId()) { capteurLie = cap; break; }
        }
        String nomCapteur = capteurLie != null
            ? capteurLie.getNom() + " (" + capteurLie.getType() + ")"
            : "Capteur #" + m.getCapteurId();

        String dateStr = m.getDateMaintenance() != null
            ? m.getDateMaintenance().substring(0, Math.min(10, m.getDateMaintenance().length()))
            : "—";
        String desc = m.getDescription() != null && !m.getDescription().isEmpty()
            ? (m.getDescription().length() > 55 ? m.getDescription().substring(0, 52) + "…" : m.getDescription())
            : "—";

        Stage dialog = new Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("Supprimer la maintenance");
        dialog.setResizable(false);

        // ── HEADER orange ──
        VBox header = new VBox(6);
        header.setStyle("-fx-background-color: linear-gradient(to right, #92400e, #d97706);" +
                        "-fx-padding: 22 24 18 24;");
        HBox headerTop = new HBox(14);
        headerTop.setAlignment(Pos.CENTER_LEFT);

        javafx.scene.layout.StackPane iconCircle = new javafx.scene.layout.StackPane();
        iconCircle.setMinSize(48, 48); iconCircle.setMaxSize(48, 48);
        iconCircle.setStyle("-fx-background-color: rgba(255,255,255,0.18); -fx-background-radius: 50;");
        Label iconLbl = new Label("🗑");
        iconLbl.setStyle("-fx-font-size: 20;");
        iconCircle.getChildren().add(iconLbl);

        VBox titreBox = new VBox(3);
        Label titre = new Label("Supprimer la maintenance");
        titre.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: white;");
        Label sousTitre = new Label("Cette action est définitive et irréversible");
        sousTitre.setStyle("-fx-font-size: 11; -fx-text-fill: rgba(255,255,255,0.75);");
        titreBox.getChildren().addAll(titre, sousTitre);
        headerTop.getChildren().addAll(iconCircle, titreBox);

        // Badge type dans le header
        Label badgeType = new Label("  " + m.getTypeMaintenance().toUpperCase() + "  ");
        badgeType.setStyle("-fx-background-color: rgba(255,255,255,0.2);" +
                           "-fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold;" +
                           "-fx-background-radius: 20; -fx-padding: 4 12;");
        header.getChildren().addAll(headerTop, badgeType);

        // ── CORPS : grille 2 colonnes ──
        VBox corps = new VBox(14);
        corps.setStyle("-fx-background-color: #f8fafc; -fx-padding: 20 24 16 24;");

        Label lblSection = new Label("INFORMATIONS DE LA MAINTENANCE");
        lblSection.setStyle("-fx-font-size: 9; -fx-font-weight: bold; -fx-text-fill: #94a3b8;" +
                            "-fx-letter-spacing: 1.5;");

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(12); grid.setVgap(0);
        grid.setStyle("-fx-background-color: white; -fx-background-radius: 14;" +
                      "-fx-border-color: #fde68a; -fx-border-radius: 14; -fx-border-width: 1;" +
                      "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.06),8,0,0,2);");
        grid.setPadding(new javafx.geometry.Insets(0));

        // Colonne gauche
        grid.add(celluleInfo("🔢", "ID", "#M" + m.getId(), null, true), 0, 0);
        grid.add(celluleInfo("📅", "Date", dateStr, null, false), 0, 1);
        grid.add(celluleInfo("⚡", "Statut", m.getStatut().toUpperCase().replace("_", " "),
                             null, false), 0, 2);
        // Colonne droite
        grid.add(celluleInfo("🔧", "Type", m.getTypeMaintenance().toUpperCase(), null, true), 1, 0);
        grid.add(celluleInfo("📡", "Capteur", nomCapteur, null, false), 1, 1);
        grid.add(celluleInfo("📝", "Description", desc, null, false), 1, 2);

        javafx.scene.layout.ColumnConstraints col1 = new javafx.scene.layout.ColumnConstraints();
        col1.setPercentWidth(50);
        javafx.scene.layout.ColumnConstraints col2 = new javafx.scene.layout.ColumnConstraints();
        col2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(col1, col2);

        corps.getChildren().addAll(lblSection, grid);

        // ── AVERTISSEMENT ──
        HBox warning = new HBox(10);
        warning.setAlignment(Pos.CENTER_LEFT);
        warning.setStyle("-fx-background-color: #fffbeb; -fx-padding: 12 24;" +
                         "-fx-border-color: #fde68a; -fx-border-width: 1 0 0 0;");
        Label warnIcon = new Label("⚠");
        warnIcon.setStyle("-fx-font-size: 13; -fx-text-fill: #d97706;");
        Label warnText = new Label("L'historique de cette intervention sera définitivement perdu.");
        warnText.setStyle("-fx-font-size: 11; -fx-text-fill: #92400e; -fx-wrap-text: true;");
        warnText.setMaxWidth(360);
        warning.getChildren().addAll(warnIcon, warnText);

        // ── BOUTONS ──
        HBox boutons = new HBox(10);
        boutons.setAlignment(Pos.CENTER_RIGHT);
        boutons.setStyle("-fx-padding: 14 24 18 24; -fx-background-color: white;" +
                         "-fx-border-color: #f1f5f9; -fx-border-width: 1 0 0 0;");

        Button btnAnnuler = new Button("Annuler");
        btnAnnuler.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569;" +
                            "-fx-font-size: 13; -fx-font-weight: bold;" +
                            "-fx-background-radius: 10; -fx-border-color: #e2e8f0;" +
                            "-fx-border-radius: 10; -fx-cursor: hand; -fx-padding: 9 20;");
        btnAnnuler.setOnMouseEntered(e -> btnAnnuler.setStyle(
            "-fx-background-color: #e2e8f0; -fx-text-fill: #1e293b;" +
            "-fx-font-size: 13; -fx-font-weight: bold;" +
            "-fx-background-radius: 10; -fx-border-color: #cbd5e1;" +
            "-fx-border-radius: 10; -fx-cursor: hand; -fx-padding: 9 20;"));
        btnAnnuler.setOnMouseExited(e -> btnAnnuler.setStyle(
            "-fx-background-color: #f1f5f9; -fx-text-fill: #475569;" +
            "-fx-font-size: 13; -fx-font-weight: bold;" +
            "-fx-background-radius: 10; -fx-border-color: #e2e8f0;" +
            "-fx-border-radius: 10; -fx-cursor: hand; -fx-padding: 9 20;"));

        Button btnSupprimer = new Button("🗑  Supprimer définitivement");
        btnSupprimer.setStyle("-fx-background-color: #d97706; -fx-text-fill: white;" +
                              "-fx-font-size: 13; -fx-font-weight: bold;" +
                              "-fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 9 22;" +
                              "-fx-effect: dropshadow(gaussian,rgba(217,119,6,0.45),10,0,0,3);");
        btnSupprimer.setOnMouseEntered(e -> btnSupprimer.setStyle(
            "-fx-background-color: #b45309; -fx-text-fill: white;" +
            "-fx-font-size: 13; -fx-font-weight: bold;" +
            "-fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 9 22;" +
            "-fx-effect: dropshadow(gaussian,rgba(180,83,9,0.55),12,0,0,4);"));
        btnSupprimer.setOnMouseExited(e -> btnSupprimer.setStyle(
            "-fx-background-color: #d97706; -fx-text-fill: white;" +
            "-fx-font-size: 13; -fx-font-weight: bold;" +
            "-fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 9 22;" +
            "-fx-effect: dropshadow(gaussian,rgba(217,119,6,0.45),10,0,0,3);"));

        final boolean[] confirmed = {false};
        btnAnnuler.setOnAction(e -> dialog.close());
        btnSupprimer.setOnAction(e -> { confirmed[0] = true; dialog.close(); });
        boutons.getChildren().addAll(btnAnnuler, btnSupprimer);

        VBox root = new VBox(0, header, corps, warning, boutons);
        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        dialog.setScene(scene);
        dialog.setMinWidth(480);
        dialog.sizeToScene();
        dialog.showAndWait();
        return confirmed[0];
    }

    /** Couleur du statut capteur pour la cellule */
    private String statutColorCapteur(String statut) {
        return switch (statut) {
            case "actif"    -> "#16a34a";
            case "en_panne" -> "#dc2626";
            default         -> "#64748b";
        };
    }

    /**
     * Crée une cellule de grille pour les dialogues de suppression.
     * @param emoji     icône
     * @param label     libellé du champ
     * @param valeur    valeur à afficher
     * @param color     couleur de la valeur (null = défaut)
     * @param topRow    true si c'est la première ligne (pas de bordure top)
     */
    private VBox celluleInfo(String emoji, String label, String valeur,
                             String color, boolean topRow) {
        VBox cell = new VBox(3);
        cell.setStyle("-fx-padding: 10 14;" +
                      (topRow ? "" : "-fx-border-color: #f1f5f9; -fx-border-width: 1 0 0 0;"));

        HBox labelRow = new HBox(5);
        labelRow.setAlignment(Pos.CENTER_LEFT);
        Label emojiLbl = new Label(emoji);
        emojiLbl.setStyle("-fx-font-size: 11;");
        Label fieldLbl = new Label(label);
        fieldLbl.setStyle("-fx-font-size: 10; -fx-text-fill: #94a3b8; -fx-font-weight: bold;");
        labelRow.getChildren().addAll(emojiLbl, fieldLbl);

        Label valLbl = new Label(valeur != null ? valeur : "—");
        String valStyle = "-fx-font-size: 12; -fx-font-weight: bold; -fx-wrap-text: true;";
        if (color != null && !color.isEmpty()) {
            valStyle += "-fx-text-fill: " + color + ";";
        } else {
            valStyle += "-fx-text-fill: #1e293b;";
        }
        valLbl.setStyle(valStyle);
        valLbl.setMaxWidth(190);

        cell.getChildren().addAll(labelRow, valLbl);
        return cell;
    }

    /**
     * @deprecated Remplacée par celluleInfo() — conservée pour compatibilité
     */
    private HBox ligneInfo(String label, String valeur, boolean derniere) {
        HBox ligne = new HBox();
        ligne.setAlignment(Pos.CENTER_LEFT);
        ligne.setStyle("-fx-padding: 9 16;" +
                       (derniere ? "" : "-fx-border-color: #f1f5f9; -fx-border-width: 0 0 1 0;"));
        Label lblLabel = new Label(label);
        lblLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #94a3b8; -fx-min-width: 120;");
        Label lblValeur = new Label(valeur != null ? valeur : "—");
        lblValeur.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #1e293b; -fx-wrap-text: true;");
        lblValeur.setMaxWidth(260);
        HBox.setHgrow(lblValeur, javafx.scene.layout.Priority.ALWAYS);
        ligne.getChildren().addAll(lblLabel, lblValeur);
        return ligne;
    }
}
