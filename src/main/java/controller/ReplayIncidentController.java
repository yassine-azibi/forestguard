package controller;

import dao.AlerteDAO;
import model.Alerte;
import utils.MyConnection;
import utils.NavigationManager;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

import java.net.URL;
import java.sql.*;
import java.util.*;
import java.util.ResourceBundle;

public class ReplayIncidentController implements Initializable {

    @FXML private ComboBox<String>  cbAlertes;
    @FXML private WebView           carteReplay;
    @FXML private Label             lblTitre;
    @FXML private Label             lblStatut;
    @FXML private Label             lblEtape;
    @FXML private Label             lblNarration;
    @FXML private Button            btnPlay;
    @FXML private Button            btnPause;
    @FXML private Button            btnReset;
    @FXML private Slider            sliderVitesse;
    @FXML private ProgressBar       progressReplay;
    @FXML private VBox              panelTimeline;
    @FXML private Label             lblDuree;

    private WebEngine engine;
    private ReplayBridge bridge;
    private final AlerteDAO dao = new AlerteDAO();

    // Données du replay
    private List<Alerte>      toutesAlertes = new ArrayList<>();
    private Alerte            alerteSelectionnee;
    private List<EtapeReplay> etapes        = new ArrayList<>();
    private int               etapeActuelle = 0;
    private boolean           enLecture     = false;
    private javafx.animation.Timeline timeline;

    // ── Modèle d'étape ────────────────────────────────────────────────────────
    public static class EtapeReplay {
        public int    index;
        public String temps;
        public String evenement;
        public String description;
        public double lat, lng;
        public double rayon;
        public String couleur;
        public String icone;
        public EtapeReplay(int i, String t, String e, String d,
                           double la, double lo, double r, String c, String ic) {
            index=i; temps=t; evenement=e; description=d;
            lat=la; lng=lo; rayon=r; couleur=c; icone=ic;
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        chargerCarte();
        chargerListeAlertes();
        sliderVitesse.setValue(1.0);
        sliderVitesse.setMin(0.5);
        sliderVitesse.setMax(3.0);
        progressReplay.setProgress(0);
        btnPause.setDisable(true);
        btnReset.setDisable(true);
    }

    // ── Charger la carte Leaflet ──────────────────────────────────────────────
    private void chargerCarte() {
        engine = carteReplay.getEngine();
        engine.setJavaScriptEnabled(true);

        String html =
            "<!DOCTYPE html><html><head><meta charset='utf-8'/>" +
            "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>" +
            "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>" +
            "<style>" +
            "*{margin:0;padding:0;box-sizing:border-box;}" +
            "html,body{width:100%;height:100%;background:#0d1f14;}" +
            "#map{width:100%;height:100%;}" +
            ".pulse-marker{" +
            "  width:20px;height:20px;border-radius:50%;" +
            "  animation:pulse 1s infinite;" +
            "}" +
            "@keyframes pulse{" +
            "  0%{box-shadow:0 0 0 0 rgba(239,68,68,0.7);}" +
            "  70%{box-shadow:0 0 0 20px rgba(239,68,68,0);}" +
            "  100%{box-shadow:0 0 0 0 rgba(239,68,68,0);}" +
            "}" +
            "</style></head><body><div id='map'></div>" +
            "<script>" +
            "var map = L.map('map',{center:[36.8,9.5],zoom:7});" +
            "L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'," +
            "{attribution:'OpenStreetMap',maxZoom:18}).addTo(map);" +
            "var layers = [];" +
            "var markers = [];" +
            "function clearAll(){" +
            "  layers.forEach(function(l){map.removeLayer(l);});" +
            "  markers.forEach(function(m){map.removeLayer(m);});" +
            "  layers=[];markers=[];" +
            "}" +
            "function addCircle(lat,lng,radius,color,opacity){" +
            "  var c=L.circle([lat,lng],{" +
            "    radius:radius,color:color,fillColor:color," +
            "    fillOpacity:opacity,weight:2" +
            "  }).addTo(map);" +
            "  layers.push(c);" +
            "}" +
            "function addMarker(lat,lng,label,color){" +
            "  var icon=L.divIcon({" +
            "    html:'<div style=\"background:'+color+';color:white;padding:4px 8px;" +
            "border-radius:20px;font-size:11px;font-weight:bold;white-space:nowrap;" +
            "box-shadow:0 2px 8px rgba(0,0,0,0.5)\">'+label+'</div>'," +
            "    iconAnchor:[0,0],className:''" +
            "  });" +
            "  var m=L.marker([lat,lng],{icon:icon}).addTo(map);" +
            "  markers.push(m);" +
            "}" +
            "function flyTo(lat,lng,zoom){" +
            "  map.flyTo([lat,lng],zoom,{duration:1.5});" +
            "}" +
            "function showEtape(lat,lng,rayon,couleur,label){" +
            "  clearAll();" +
            "  // Cercle principal anime" +
            "  addCircle(lat,lng,rayon,couleur,0.25);" +
            "  addCircle(lat,lng,rayon*0.6,couleur,0.35);" +
            "  addCircle(lat,lng,rayon*0.3,couleur,0.5);" +
            "  addMarker(lat,lng,label,couleur);" +
            "  flyTo(lat,lng,10);" +
            "}" +
            "function showResolu(lat,lng){" +
            "  clearAll();" +
            "  addCircle(lat,lng,3000,'#4ade80',0.3);" +
            "  addMarker(lat,lng,'Incident resolu','#16a34a');" +
            "  flyTo(lat,lng,10);" +
            "}" +
            "setTimeout(function(){map.invalidateSize(true);},500);" +
            "</script></body></html>";

        engine.loadContent(html);
        engine.getLoadWorker().stateProperty().addListener((obs, o, n) -> {
            if (n == Worker.State.SUCCEEDED) {
                bridge = new ReplayBridge();
                JSObject win = (JSObject) engine.executeScript("window");
                win.setMember("javaBridge", bridge);
                // Carte prete - activer le bouton play si alerte selectionnee
                Platform.runLater(() -> {
                    if (alerteSelectionnee != null) btnPlay.setDisable(false);
                });
            }
        });
    }

    // ── Charger la liste des alertes ──────────────────────────────────────────
    private void chargerListeAlertes() {
        toutesAlertes = dao.getAll();
        List<String> items = new ArrayList<>();
        items.add("-- Selectionnez une alerte --");
        for (Alerte a : toutesAlertes) {
            items.add("#" + a.getId() + " | " + a.getTypeAlerte()
                + " | " + a.getNiveau()
                + " | " + a.getLocalisation().replaceAll("\\[.*\\]","").trim()
                    .substring(0, Math.min(30, a.getLocalisation().replaceAll("\\[.*\\]","").trim().length())));
        }
        cbAlertes.setItems(FXCollections.observableArrayList(items));
        cbAlertes.getSelectionModel().selectFirst();
    }

    // ── Sélection d'une alerte ────────────────────────────────────────────────
    @FXML
    private void selectionnerAlerte() {
        int idx = cbAlertes.getSelectionModel().getSelectedIndex();
        if (idx <= 0) return;
        alerteSelectionnee = toutesAlertes.get(idx - 1);
        construireEtapes(alerteSelectionnee);
        afficherInfoAlerte(alerteSelectionnee);
        resetReplay();
        btnPlay.setDisable(false);
    }

    // ── Construire les étapes du replay ───────────────────────────────────────
    private void construireEtapes(Alerte a) {
        etapes.clear();
        panelTimeline.getChildren().clear();

        String loc = a.getLocalisation().replaceAll("\\[.*\\]","").trim();

        // Extraire coordonnées GPS si disponibles
        double lat = 36.8065, lng = 9.5375; // défaut : Tunisie centre
        String locStr = a.getLocalisation();
        if (locStr.contains("[") && locStr.contains(",")) {
            try {
                String coords = locStr.replaceAll(".*\\[","").replaceAll("\\].*","");
                String[] parts = coords.split(",");
                lat = Double.parseDouble(parts[0].trim());
                lng = Double.parseDouble(parts[1].trim());
            } catch (Exception ignored) {}
        }

        // Récupérer données capteurs de la zone
        List<double[]> capteurs = getDonneesCapteurs(loc);

        // Couleur selon niveau
        String couleurBase = switch (a.getNiveau()) {
            case "Critique" -> "#ef4444";
            case "Haute"    -> "#f97316";
            default         -> "#3b82f6";
        };

        final double latF = lat, lngF = lng;

        // Étape 0 — Signal initial
        etapes.add(new EtapeReplay(0,
            a.getDateFormatted(),
            "Signal initial detecte",
            "Le capteur " + a.getSource() + " detecte une anomalie dans la zone " + loc,
            latF, lngF, 1000, couleurBase, "Signal"));

        // Étape 1 — Confirmation
        etapes.add(new EtapeReplay(1,
            a.getDateFormatted() + " +5min",
            "Alerte confirmee",
            "Type : " + a.getTypeAlerte() + " | Niveau : " + a.getNiveau()
            + " | Zone : " + loc,
            latF, lngF, 2500, couleurBase, a.getTypeAlerte()));

        // Étapes capteurs si disponibles
        int etapeIdx = 2;
        for (double[] cap : capteurs) {
            String risque = cap[0] > 40 ? "DANGER" : cap[0] > 30 ? "ATTENTION" : "SUR";
            String couleur = cap[0] > 40 ? "#ef4444" : cap[0] > 30 ? "#f97316" : "#4ade80";
            etapes.add(new EtapeReplay(etapeIdx++,
                "Lecture capteur",
                "Donnees IoT — " + risque,
                String.format("Temperature : %.1f°C | Humidite : %.1f%% | Fumee : %.1f ppm",
                    cap[0], cap[1], cap[2]),
                latF, lngF, 3000 + (etapeIdx * 500), couleur, "Capteur " + etapeIdx));
        }

        // Étape propagation si critique
        if ("Critique".equals(a.getNiveau())) {
            etapes.add(new EtapeReplay(etapeIdx++,
                "+15min",
                "Propagation detectee",
                "L incendie se propage. Zone d evacuation elargie. Vent fort detecte.",
                latF + 0.01, lngF + 0.01, 5000, "#ef4444", "Propagation"));
        }

        // Étape intervention
        etapes.add(new EtapeReplay(etapeIdx++,
            "+20min",
            "Equipes d intervention mobilisees",
            "Pompiers dispatches vers la zone. Vehicules en route.",
            latF, lngF, 4000, "#f97316", "Intervention"));

        // Étape statut final
        if ("Validee".equals(a.getStatut()) || "Validée".equals(a.getStatut())) {
            etapes.add(new EtapeReplay(etapeIdx,
                "+45min",
                "Incident maîtrise",
                "L alerte a ete validee et traitee. Zone securisee.",
                latF, lngF, 2000, "#4ade80", "Resolu"));
        } else if ("Rejetee".equals(a.getStatut()) || "Rejetée".equals(a.getStatut())) {
            etapes.add(new EtapeReplay(etapeIdx,
                "+10min",
                "Alerte annulee",
                "Apres verification, l alerte a ete rejetee. Fausse alarme.",
                latF, lngF, 1500, "#64748b", "Annule"));
        } else {
            etapes.add(new EtapeReplay(etapeIdx,
                "En cours",
                "Alerte en attente de traitement",
                "L alerte est toujours en cours. Surveillance active.",
                latF, lngF, 3000, "#fbbf24", "En cours"));
        }

        // Construire la timeline visuelle
        construireTimeline();
    }

    private void construireTimeline() {
        panelTimeline.getChildren().clear();
        for (int i = 0; i < etapes.size(); i++) {
            EtapeReplay e = etapes.get(i);
            HBox row = new HBox(10);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            row.setStyle("-fx-padding:6 8;-fx-background-radius:8;");

            Label dot = new Label("●");
            dot.setStyle("-fx-text-fill:" + e.couleur + ";-fx-font-size:14;");

            VBox info = new VBox(2);
            Label ev = new Label(e.evenement);
            ev.setStyle("-fx-text-fill:white;-fx-font-size:11;-fx-font-weight:bold;");
            Label t = new Label(e.temps);
            t.setStyle("-fx-text-fill:#64748b;-fx-font-size:10;");
            info.getChildren().addAll(ev, t);

            row.getChildren().addAll(dot, info);
            final int idx = i;
            row.setOnMouseClicked(ev2 -> sauterEtape(idx));
            row.setOnMouseEntered(ev2 -> row.setStyle(
                "-fx-padding:6 8;-fx-background-color:rgba(255,255,255,0.05);-fx-background-radius:8;-fx-cursor:hand;"));
            row.setOnMouseExited(ev2 -> row.setStyle(
                "-fx-padding:6 8;-fx-background-radius:8;"));

            panelTimeline.getChildren().add(row);

            // Séparateur
            if (i < etapes.size() - 1) {
                Label sep = new Label("│");
                sep.setStyle("-fx-text-fill:#1e4a2a;-fx-padding:0 0 0 6;");
                panelTimeline.getChildren().add(sep);
            }
        }
    }

    // ── Contrôles de lecture ──────────────────────────────────────────────────
    @FXML
    private void play() {
        if (etapes.isEmpty()) return;
        enLecture = true;
        btnPlay.setDisable(true);
        btnPause.setDisable(false);
        btnReset.setDisable(false);
        lancerLecture();
    }

    @FXML
    private void pause() {
        enLecture = false;
        if (timeline != null) timeline.pause();
        btnPlay.setDisable(false);
        btnPause.setDisable(true);
        lblStatut.setText("En pause");
    }

    @FXML
    private void reset() {
        resetReplay();
    }

    private void resetReplay() {
        enLecture = false;
        if (timeline != null) timeline.stop();
        etapeActuelle = 0;
        progressReplay.setProgress(0);
        lblEtape.setText("Etape 0 / " + etapes.size());
        lblNarration.setText("Appuyez sur Play pour demarrer le replay");
        lblStatut.setText("En attente");
        btnPlay.setDisable(alerteSelectionnee == null);
        btnPause.setDisable(true);
        btnReset.setDisable(true);
        try {
            Object test = engine.executeScript("typeof clearAll");
            if ("function".equals(String.valueOf(test))) {
                engine.executeScript("clearAll();map.setView([36.8,9.5],7);");
            }
        } catch (Exception ex) {
            System.out.println("Map pas encore prete: " + ex.getMessage());
        }
        surlignerEtapeTimeline(-1);
    }

    private void lancerLecture() {
        double vitesse = sliderVitesse.getValue();
        double delai   = 2000 / vitesse; // ms entre chaque étape

        timeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(
                javafx.util.Duration.millis(delai),
                e -> {
                    if (!enLecture || etapeActuelle >= etapes.size()) {
                        if (etapeActuelle >= etapes.size()) finReplay();
                        return;
                    }
                    jouerEtape(etapeActuelle);
                    etapeActuelle++;
                }
            )
        );
        timeline.setCycleCount(etapes.size() - etapeActuelle);
        timeline.play();
    }

    private void jouerEtape(int idx) {
        if (idx >= etapes.size()) return;
        EtapeReplay e = etapes.get(idx);

        // Mettre à jour UI
        lblEtape.setText("Etape " + (idx + 1) + " / " + etapes.size());
        lblNarration.setText(e.description);
        progressReplay.setProgress((double)(idx + 1) / etapes.size());
        lblStatut.setText(e.evenement);
        lblDuree.setText(e.temps);
        surlignerEtapeTimeline(idx);

        // Animation carte
        String script;
        if (idx == etapes.size() - 1 && e.couleur.equals("#4ade80")) {
            script = String.format("showResolu(%f,%f);", e.lat, e.lng);
        } else {
            script = String.format(
                "showEtape(%f,%f,%f,'%s','%s');",
                e.lat, e.lng, e.rayon, e.couleur,
                e.icone.replace("'", ""));
        }
        final String s = script;
        Platform.runLater(() -> {
            try {
                Object test = engine.executeScript("typeof showEtape");
                if ("function".equals(String.valueOf(test))) {
                    engine.executeScript(s);
                } else {
                    // Carte pas encore prete - retry apres 500ms
                    new javafx.animation.PauseTransition(javafx.util.Duration.millis(600))
                        .setOnFinished(ev -> {
                            try { engine.executeScript(s); }
                            catch (Exception ex2) {}
                        });
                }
            } catch (Exception ex) { System.out.println("Map erreur: " + ex.getMessage()); }
        });
    }

    private void sauterEtape(int idx) {
        if (enLecture) pause();
        etapeActuelle = idx;
        jouerEtape(idx);
        progressReplay.setProgress((double)(idx + 1) / etapes.size());
        btnReset.setDisable(false);
    }

    private void finReplay() {
        enLecture = false;
        btnPlay.setDisable(false);
        btnPause.setDisable(true);
        lblStatut.setText("Replay termine");
        progressReplay.setProgress(1.0);
        surlignerEtapeTimeline(etapes.size() - 1);
    }

    private void surlignerEtapeTimeline(int idx) {
        // Surligner l'étape active dans la timeline
        var children = panelTimeline.getChildren();
        int etapeVue = 0;
        for (var child : children) {
            if (child instanceof HBox row) {
                if (etapeVue == idx) {
                    row.setStyle(
                        "-fx-padding:6 8;-fx-background-color:rgba(74,222,128,0.15);" +
                        "-fx-background-radius:8;-fx-border-color:#4ade80;" +
                        "-fx-border-radius:8;-fx-border-width:1;");
                } else {
                    row.setStyle("-fx-padding:6 8;-fx-background-radius:8;");
                }
                etapeVue++;
            }
        }
    }

    private void afficherInfoAlerte(Alerte a) {
        String loc = a.getLocalisation().replaceAll("\\[.*\\]","").trim();
        lblTitre.setText("#" + a.getId() + " — " + a.getTypeAlerte() + " | " + loc);
        lblStatut.setText("Pret — " + a.getStatut());
        lblEtape.setText("0 / " + etapes.size() + " etapes");
        lblDuree.setText(a.getDateFormatted());
        progressReplay.setProgress(0);
    }

    // ── Données capteurs depuis la BDD ────────────────────────────────────────
    private List<double[]> getDonneesCapteurs(String zone) {
        List<double[]> data = new ArrayList<>();
        Connection cnx = MyConnection.getInstance().getCnx();
        if (cnx == null) return data;
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT temperature, humidite, fumee FROM donnee_capteur " +
                "WHERE zone LIKE ? ORDER BY horodatage DESC LIMIT 3");
        ) {
            ps.setString(1, "%" + zone.substring(0, Math.min(10, zone.length())) + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                data.add(new double[]{
                    rs.getDouble("temperature"),
                    rs.getDouble("humidite"),
                    rs.getDouble("fumee")
                });
            }
        } catch (Exception e) {
            System.out.println("Capteurs: " + e.getMessage());
        }
        return data;
    }

    public class ReplayBridge {
        public void onMapReady() {
            System.out.println("Carte replay prete");
        }
    }

    @FXML private void goDashboard() {
        if (timeline != null) timeline.stop();
        Stage s = (Stage) lblTitre.getScene().getWindow();
        NavigationManager.navigateTo(s, "/fxml/Dashboard.fxml");
    }
}
