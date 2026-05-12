package controller;

import dao.AlerteDAO;
import model.Alerte;
import service.AnalyseAlerteIA;
import utils.NavigationManager;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

public class SatelliteViewController implements Initializable {

    @FXML private Label    lblCritiques, lblHautes, lblMoyennes;
    @FXML private Label    lblDerniereAlerte, lblDerniereDate;
    @FXML private Label    lblSignal, lblStatus, lblHeure, lblTotal;
    @FXML private Pane     carteSatellite;
    @FXML private Label    lblAnalyseIA;
    @FXML private ScrollPane scrollAnalyse;

    private final AlerteDAO       dao = new AlerteDAO();
    private final AnalyseAlerteIA ia  = new AnalyseAlerteIA();
    private ScheduledExecutorService scheduler;
    private Timeline clockTimeline;
    private List<Alerte> alertes = new ArrayList<>();

    // Coordonnées Tunisie → pixels sur la carte
    private static final double MAP_W   = 750;
    private static final double MAP_H   = 580;
    private static final double LAT_MIN = 30.2;
    private static final double LAT_MAX = 37.8;
    private static final double LNG_MIN = 7.5;
    private static final double LNG_MAX = 11.7;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        demarrerHorloge();
        chargerDonnees();

        // Rafraîchir toutes les 30 secondes
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "sat-refresh");
            t.setDaemon(true); return t;
        });
        scheduler.scheduleAtFixedRate(() ->
            Platform.runLater(this::chargerDonnees), 30, 30, TimeUnit.SECONDS);
    }

    // ── Charger les données et dessiner ──────────────────────────────────────
    private void chargerDonnees() {
        alertes = dao.getAll();
        int critiques = 0, hautes = 0, moyennes = 0;
        Alerte derniere = null;

        for (Alerte a : alertes) {
            if (estActive(a)) {
                switch (a.getNiveau()) {
                    case "Critique" -> critiques++;
                    case "Haute"    -> hautes++;
                    default         -> moyennes++;
                }
                if (derniere == null) derniere = a;
            }
        }

        final int fc = critiques, fh = hautes, fm = moyennes;
        final Alerte fd = derniere;

        lblCritiques.setText(String.valueOf(fc));
        lblHautes.setText(String.valueOf(fh));
        lblMoyennes.setText(String.valueOf(fm));
        lblTotal.setText((fc + fh + fm) + " ALERTES ACTIVES");

        if (fd != null) {
            lblDerniereAlerte.setText(fd.getTypeAlerte() + " — " +
                fd.getLocalisation().replaceAll("\\[.*\\]","").trim());
            lblDerniereDate.setText(fd.getDateFormatted());
        }

        dessinerCarte();
    }

    // ── Dessiner la carte satellite en JavaFX ────────────────────────────────
    private void dessinerCarte() {
        carteSatellite.getChildren().clear();

        // Fond noir espace
        Rectangle fond = new Rectangle(MAP_W, MAP_H);
        fond.setFill(Color.web("#000510"));
        carteSatellite.getChildren().add(fond);

        // Grille de coordonnées style NASA
        for (int i = 0; i <= 10; i++) {
            double x = MAP_W * i / 10;
            double y = MAP_H * i / 10;
            Line lv = new Line(x, 0, x, MAP_H);
            lv.setStroke(Color.web("#0a2a4a", 0.4));
            lv.setStrokeWidth(0.5);
            Line lh = new Line(0, y, MAP_W, y);
            lh.setStroke(Color.web("#0a2a4a", 0.4));
            lh.setStrokeWidth(0.5);
            carteSatellite.getChildren().addAll(lv, lh);
        }

        // Contour simplifié de la Tunisie
        dessinerContourTunisie();

        // Points d'alerte animés
        for (Alerte a : alertes) {
            if (!estActive(a)) continue;
            double[] coords = getCoords(a);
            double x = lngToX(coords[1]);
            double y = latToY(coords[0]);
            ajouterPointAlerte(a, x, y);
        }

        // Forêts connues (points verts fixes)
        ajouterForets();

        // Label coordonnées
        Text coordLabel = new Text(10, MAP_H - 10,
            "TUNISIE | ForestGuard Satellite View | " +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        coordLabel.setFill(Color.web("#00d4ff", 0.6));
        coordLabel.setFont(Font.font("Monospace", 10));
        carteSatellite.getChildren().add(coordLabel);
    }

    private void dessinerContourTunisie() {
        // Points approximatifs du contour de la Tunisie
        double[][] points = {
            {37.5, 8.7}, {37.3, 9.2}, {37.2, 10.2}, {36.9, 10.5},
            {36.8, 11.1}, {36.5, 11.0}, {35.8, 10.9}, {35.0, 11.1},
            {34.5, 10.7}, {33.5, 11.0}, {33.0, 11.6}, {32.1, 11.5},
            {31.0, 10.2}, {30.2, 9.5},  {30.3, 8.5},  {31.5, 8.1},
            {32.5, 8.0},  {33.2, 8.3},  {33.9, 7.8},  {35.0, 7.6},
            {36.5, 8.0},  {37.0, 8.2},  {37.5, 8.7}
        };

        javafx.scene.shape.Polygon poly = new javafx.scene.shape.Polygon();
        for (double[] p : points) {
            poly.getPoints().addAll(lngToX(p[1]), latToY(p[0]));
        }
        poly.setFill(Color.web("#0a2a1a", 0.6));
        poly.setStroke(Color.web("#00d4ff", 0.5));
        poly.setStrokeWidth(1.5);
        carteSatellite.getChildren().add(poly);
    }

    private void ajouterForets() {
        double[] lats  = {36.8065, 36.7833, 36.8000, 36.9544, 36.7256};
        double[] lngs  = {8.7636,  8.6833, 10.8330,  8.7576,  9.1817};
        String[] noms  = {"Kroumirie","Ain Draham","Cap Bon","Tabarka","Beja"};
        for (int i = 0; i < lats.length; i++) {
            double x = lngToX(lngs[i]);
            double y = latToY(lats[i]);
            Circle dot = new Circle(x, y, 4);
            dot.setFill(Color.web("#4ade80", 0.6));
            dot.setStroke(Color.web("#22c55e"));
            dot.setStrokeWidth(1);
            String nom = noms[i];
            Text label = new Text(x + 6, y + 4, nom.substring(0, Math.min(6, nom.length())));
            label.setFill(Color.web("#4ade80", 0.7));
            label.setFont(Font.font("Monospace", 9));
            carteSatellite.getChildren().addAll(dot, label);
        }
    }

    private void ajouterPointAlerte(Alerte a, double x, double y) {
        Color col = switch (a.getNiveau()) {
            case "Critique" -> Color.web("#ef4444");
            case "Haute"    -> Color.web("#f97316");
            default         -> Color.web("#3b82f6");
        };

        // Cercles pulsants concentriques
        for (int i = 1; i <= 3; i++) {
            Circle ring = new Circle(x, y, 8 * i);
            ring.setFill(Color.TRANSPARENT);
            ring.setStroke(col.deriveColor(0, 1, 1, 0.4 / i));
            ring.setStrokeWidth(1);
            ring.setStrokeDashOffset(4);

            // Animation pulsation
            ScaleTransition pulse = new ScaleTransition(Duration.millis(1500 + i * 300), ring);
            pulse.setFromX(0.8); pulse.setToX(1.2);
            pulse.setFromY(0.8); pulse.setToY(1.2);
            pulse.setAutoReverse(true);
            pulse.setCycleCount(Animation.INDEFINITE);
            pulse.setDelay(Duration.millis(i * 200));
            pulse.play();
            carteSatellite.getChildren().add(ring);
        }

        // Point central lumineux
        Circle center = new Circle(x, y, 5);
        center.setFill(new RadialGradient(0, 0, 0.5, 0.5, 0.5, true,
            CycleMethod.NO_CYCLE,
            new Stop(0, Color.WHITE),
            new Stop(0.4, col),
            new Stop(1, col.darker())));
        center.setStroke(col);
        center.setStrokeWidth(1);

        // Animation clignotement
        FadeTransition fade = new FadeTransition(Duration.millis(800), center);
        fade.setFromValue(0.6); fade.setToValue(1.0);
        fade.setAutoReverse(true);
        fade.setCycleCount(Animation.INDEFINITE);
        fade.play();

        // Label alerte
        String labelTxt = "#" + a.getId() + " " +
            a.getTypeAlerte().substring(0, Math.min(4, a.getTypeAlerte().length())).toUpperCase();
        Text label = new Text(x + 8, y - 6, labelTxt);
        label.setFill(col);
        label.setFont(Font.font("Monospace", FontWeight.BOLD, 9));

        // Click → analyse IA
        center.setOnMouseClicked(e -> analyserAvecIA(a));
        center.setStyle("-fx-cursor:hand;");

        carteSatellite.getChildren().addAll(center, label);
    }

    // ── Analyse IA au clic ────────────────────────────────────────────────────
    private void analyserAvecIA(Alerte a) {
        lblAnalyseIA.setText("Analyse IA en cours pour alerte #" + a.getId() + "...");
        lblAnalyseIA.setStyle("-fx-text-fill:#c4b5fd;-fx-font-size:12;");

        new Thread(() -> {
            AnalyseAlerteIA.AnalyseResultat res = ia.analyser(a);
            Platform.runLater(() -> {
                String texte =
                    "ALERTE #" + a.getId() + " — " + a.getNiveau().toUpperCase() + "\n" +
                    "Type : " + a.getTypeAlerte() + "\n" +
                    "Zone : " + a.getLocalisation().replaceAll("\\[.*\\]","").trim() + "\n\n" +
                    "METEO : " + res.meteo + "\n\n" +
                    "POMPIERS : " + res.nbPompiers + "\n\n" +
                    "MATERIEL : " + res.materiel + "\n\n" +
                    "RISQUE : " + res.risquePropagation + "\n\n" +
                    "PLAN :\n" + res.planIntervention + "\n\n" +
                    "TEMPS : " + res.tempsIntervention;
                lblAnalyseIA.setText(texte);
                lblAnalyseIA.setStyle("-fx-text-fill:#e2e8f0;-fx-font-size:11;");
            });
        }, "ia-sat").start();
    }

    // ── Contrôles ─────────────────────────────────────────────────────────────
    @FXML private void actualiser() { chargerDonnees(); }

    @FXML private void zoomTunisie() { chargerDonnees(); }

    @FXML private void zoomCritique() {
        // Surligner les alertes critiques
        chargerDonnees();
        lblStatus.setText("ZOOM ZONE CRITIQUE");
    }

    // ── Horloge ───────────────────────────────────────────────────────────────
    private void demarrerHorloge() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");
        clockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            lblHeure.setText(LocalDateTime.now().format(fmt));
            dessinerCarte(); // redessiner chaque seconde pour l'animation
        }));
        clockTimeline.setCycleCount(Animation.INDEFINITE);
        clockTimeline.play();

        // Signal clignotant
        Timeline blink = new Timeline(
            new KeyFrame(Duration.millis(800),
                e -> lblSignal.setStyle("-fx-text-fill:#4ade80;-fx-font-size:12;")),
            new KeyFrame(Duration.millis(1600),
                e -> lblSignal.setStyle("-fx-text-fill:#1a3a20;-fx-font-size:12;"))
        );
        blink.setCycleCount(Animation.INDEFINITE);
        blink.play();
    }

    // ── Helpers coordonnées ───────────────────────────────────────────────────
    private double lngToX(double lng) {
        return (lng - LNG_MIN) / (LNG_MAX - LNG_MIN) * MAP_W;
    }
    private double latToY(double lat) {
        return (1 - (lat - LAT_MIN) / (LAT_MAX - LAT_MIN)) * MAP_H;
    }

    private double[] getCoords(Alerte a) {
        String loc = a.getLocalisation();
        if (loc.contains("[") && loc.contains(",")) {
            try {
                String coords = loc.replaceAll(".*\\[","").replaceAll("\\].*","");
                String[] p = coords.split(",");
                return new double[]{Double.parseDouble(p[0].trim()),
                                    Double.parseDouble(p[1].trim())};
            } catch (Exception ignored) {}
        }
        return new double[]{getLatZone(loc), getLngZone(loc)};
    }

    private double getLatZone(String loc) {
        loc = loc.toLowerCase();
        if (loc.contains("kroumirie")||loc.contains("jendouba")) return 36.8065;
        if (loc.contains("cap bon")||loc.contains("nabeul"))     return 36.8000;
        if (loc.contains("tabarka"))  return 36.9544;
        if (loc.contains("bizerte"))  return 37.2744;
        if (loc.contains("beja"))     return 36.7256;
        if (loc.contains("tunis"))    return 36.8190;
        if (loc.contains("sfax"))     return 34.7400;
        if (loc.contains("gafsa"))    return 34.4250;
        if (loc.contains("kairouan")) return 35.6781;
        if (loc.contains("kasserine"))return 35.1672;
        if (loc.contains("sousse"))   return 35.8330;
        // Position aléatoire dans la Tunisie
        return 33.5 + (loc.hashCode() & 0xFF) % 40 * 0.1;
    }

    private double getLngZone(String loc) {
        loc = loc.toLowerCase();
        if (loc.contains("kroumirie")||loc.contains("jendouba")) return 8.7636;
        if (loc.contains("cap bon")||loc.contains("nabeul"))     return 10.8330;
        if (loc.contains("tabarka"))  return 8.7576;
        if (loc.contains("bizerte"))  return 9.8739;
        if (loc.contains("beja"))     return 9.1817;
        if (loc.contains("tunis"))    return 10.1658;
        if (loc.contains("sfax"))     return 10.7600;
        if (loc.contains("gafsa"))    return 8.7842;
        if (loc.contains("kairouan")) return 10.0963;
        if (loc.contains("kasserine"))return 8.8305;
        if (loc.contains("sousse"))   return 10.6389;
        return 8.5 + (loc.hashCode() & 0xFF) % 30 * 0.1;
    }

    private boolean estActive(Alerte a) {
        String s = a.getStatut();
        return s != null && !s.startsWith("Valid") && !s.startsWith("Rejet");
    }

    @FXML private void goDashboard() {
        if (scheduler != null) scheduler.shutdown();
        if (clockTimeline != null) clockTimeline.stop();
        Stage s = (Stage) lblTotal.getScene().getWindow();
        NavigationManager.navigateTo(s, "/fxml/Dashboard.fxml");
    }
}
