package controller;

import dao.AlerteDAO;
import model.Alerte;
import service.AlerteService;
import service.AnalyseAlerteIA;
import utils.MyConnection;
import utils.NavigationManager;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

public class ReclamationsController implements Initializable {

    @FXML private VBox    conteneurReclamations;
    @FXML private Label   lblTotal;
    @FXML private Label   lblNouveaux;
    @FXML private Button  btnActualiser;
    @FXML private ScrollPane scrollReclamations;
    @FXML private ComboBox<String> cbFiltreStatut;

    private final AlerteService    alerteService = new AlerteService();
    private final AnalyseAlerteIA  ia            = new AnalyseAlerteIA();
    private ScheduledExecutorService scheduler;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Vérifier si cbFiltreStatut existe (peut être null si sidebar retirée)
        if (cbFiltreStatut != null) {
            cbFiltreStatut.setItems(FXCollections.observableArrayList(
                "Tous", "Active", "Resolved", "Pending"));
            cbFiltreStatut.setValue("Tous");
            cbFiltreStatut.setOnAction(e -> chargerReclamations());
        }

        chargerReclamations();

        // Rafraîchir toutes les 30 secondes
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "recl-refresh");
            t.setDaemon(true); return t;
        });
        scheduler.scheduleAtFixedRate(() ->
            Platform.runLater(this::chargerReclamations), 30, 30, TimeUnit.SECONDS);
    }

    // ── Charger les réclamations depuis fire_alerts ───────────────────────────
    private void chargerReclamations() {
        conteneurReclamations.getChildren().clear();
        Connection cnx = MyConnection.getInstance().getCnx();
        if (cnx == null) {
            Label err = new Label("Base de données non connectée");
            err.setStyle("-fx-text-fill:#EF9A9A;-fx-font-size:13;");
            conteneurReclamations.getChildren().add(err);
            return;
        }

        List<Map<String, String>> reclamations = new ArrayList<>();
        // Gérer le cas où cbFiltreStatut est null (sidebar retirée)
        String filtre = (cbFiltreStatut != null) ? cbFiltreStatut.getValue() : "Tous";
        String sql = "SELECT * FROM fire_alerts" +
            (filtre != null && !filtre.equals("Tous") ? " WHERE status=?" : "") +
            " ORDER BY created_at DESC";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            if (filtre != null && !filtre.equals("Tous")) ps.setString(1, filtre);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, String> r = new LinkedHashMap<>();
                r.put("id",             String.valueOf(rs.getInt("id")));
                r.put("zone",           nvl(rs.getString("zone")));
                r.put("location",       nvl(rs.getString("specific_location")));
                r.put("details",        nvl(rs.getString("details")));
                r.put("photo_path",     nvl(rs.getString("photo_path")));
                r.put("reporter_name",  nvl(rs.getString("reporter_name")));
                r.put("reporter_email", nvl(rs.getString("reporter_email")));
                r.put("alert_level",    nvl(rs.getString("alert_level")));
                r.put("status",         nvl(rs.getString("status")));
                r.put("created_at",     nvl(rs.getString("created_at")));
                reclamations.add(r);
            }
        } catch (Exception e) {
            System.out.println("FireAlerts: " + e.getMessage());
        }

        long actives = reclamations.stream()
            .filter(r -> "Active".equals(r.get("status"))).count();
        
        // Mettre à jour les labels seulement s'ils existent (sidebar peut être retirée)
        if (lblTotal != null) {
            lblTotal.setText(reclamations.size() + " réclamations");
        }
        if (lblNouveaux != null) {
            lblNouveaux.setText(actives + " actives");
        }

        if (reclamations.isEmpty()) {
            Label vide = new Label("Aucune réclamation citoyenne pour le moment");
            vide.setStyle("-fx-text-fill:#64748b;-fx-font-size:13;-fx-padding:20;");
            conteneurReclamations.getChildren().add(vide);
            return;
        }

        for (Map<String, String> r : reclamations) {
            conteneurReclamations.getChildren().add(creerCarteReclamation(r));
        }
    }

    // ── Créer une carte de réclamation ────────────────────────────────────────
    private VBox creerCarteReclamation(Map<String, String> r) {
        String level  = r.get("alert_level");
        String status = r.get("status");

        String colLevel = switch (level) {
            case "High"   -> "#ef4444";
            case "Medium" -> "#f97316";
            default       -> "#3b82f6";
        };
        String colStatus = "Active".equals(status) ? "#ef4444" : "#4ade80";

        // Carte principale
        VBox card = new VBox(10);
        card.setStyle(
            "-fx-background-color:#0d1f14;-fx-background-radius:14;" +
            "-fx-border-color:" + colLevel + "44;-fx-border-radius:14;" +
            "-fx-border-width:1;-fx-padding:16;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.3),6,0,0,2);");
        card.setPadding(new Insets(16));

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label badgeLevel = new Label(level.toUpperCase());
        badgeLevel.setStyle("-fx-background-color:" + colLevel + ";-fx-text-fill:white;" +
            "-fx-background-radius:20;-fx-padding:3 12;-fx-font-size:11;-fx-font-weight:bold;");

        Label badgeStatus = new Label(status);
        badgeStatus.setStyle("-fx-background-color:" + colStatus + "33;-fx-text-fill:" + colStatus + ";" +
            "-fx-background-radius:20;-fx-padding:3 12;-fx-font-size:11;" +
            "-fx-border-color:" + colStatus + ";-fx-border-radius:20;-fx-border-width:1;");

        Label lblId = new Label("#" + r.get("id"));
        lblId.setStyle("-fx-text-fill:#64748b;-fx-font-size:11;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblDate = new Label(r.get("created_at").substring(0, Math.min(16, r.get("created_at").length())));
        lblDate.setStyle("-fx-text-fill:#64748b;-fx-font-size:10;");

        header.getChildren().addAll(badgeLevel, badgeStatus, lblId, spacer, lblDate);
        card.getChildren().add(header);

        // Zone + Localisation
        Label lblZone = new Label("📍 " + r.get("zone") +
            (r.get("location").isEmpty() ? "" : " — " + r.get("location")));
        lblZone.setStyle("-fx-text-fill:white;-fx-font-size:14;-fx-font-weight:bold;");
        lblZone.setWrapText(true);
        card.getChildren().add(lblZone);

        // Détails
        String det = r.get("details");
        if (!det.isEmpty()) {
            Label lblDet = new Label(det.length() > 120 ? det.substring(0, 120) + "..." : det);
            lblDet.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:12;");
            lblDet.setWrapText(true);
            card.getChildren().add(lblDet);
        }

        // Photo si disponible
        String photoPath = r.get("photo_path");
        if (!photoPath.isEmpty()) {
            File photoFile = new File(photoPath);
            if (photoFile.exists()) {
                try {
                    ImageView img = new ImageView(
                        new Image(new FileInputStream(photoFile)));
                    img.setFitWidth(300);
                    img.setFitHeight(160);
                    img.setPreserveRatio(true);
                    img.setStyle("-fx-background-radius:8;");
                    card.getChildren().add(img);
                } catch (Exception ignored) {}
            } else {
                Label lblPhoto = new Label("📷 Photo: " + photoPath);
                lblPhoto.setStyle("-fx-text-fill:#334155;-fx-font-size:10;");
                card.getChildren().add(lblPhoto);
            }
        }

        // Reporter
        Label lblReporter = new Label("👤 " + r.get("reporter_name") +
            (r.get("reporter_email").isEmpty() ? "" : " — " + r.get("reporter_email")));
        lblReporter.setStyle("-fx-text-fill:#64748b;-fx-font-size:11;");
        card.getChildren().add(lblReporter);

        // Boutons
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_LEFT);

        Button btnAnalyser = new Button("🧠 Analyser avec l'IA");
        btnAnalyser.setStyle(
            "-fx-background-color:linear-gradient(to right,#7c3aed,#6d28d9);" +
            "-fx-text-fill:white;-fx-background-radius:8;-fx-border-width:0;" +
            "-fx-padding:8 16;-fx-cursor:hand;-fx-font-size:12;-fx-font-weight:bold;");
        btnAnalyser.setOnAction(e -> analyserReclamation(r, card));

        Button btnCreer = new Button("➕ Créer alerte");
        btnCreer.setStyle(
            "-fx-background-color:#16a34a;-fx-text-fill:white;" +
            "-fx-background-radius:8;-fx-border-width:0;" +
            "-fx-padding:8 16;-fx-cursor:hand;-fx-font-size:12;-fx-font-weight:bold;");
        btnCreer.setOnAction(e -> creerAlerteDepuisReclamation(r, level, btnCreer));

        actions.getChildren().addAll(btnAnalyser, btnCreer);
        card.getChildren().add(actions);

        // Zone résultat IA
        Label lblIA = new Label("");
        lblIA.setStyle("-fx-text-fill:#c4b5fd;-fx-font-size:11;");
        lblIA.setWrapText(true);
        lblIA.setId("ia-" + r.get("id"));
        card.getChildren().add(lblIA);

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle(
            "-fx-background-color:#0f2a1a;-fx-background-radius:14;" +
            "-fx-border-color:" + colLevel + "88;-fx-border-radius:14;" +
            "-fx-border-width:1;-fx-padding:16;" +
            "-fx-effect:dropshadow(gaussian," + colLevel + "44,12,0,0,3);"));
        card.setOnMouseExited(e -> card.setStyle(
            "-fx-background-color:#0d1f14;-fx-background-radius:14;" +
            "-fx-border-color:" + colLevel + "44;-fx-border-radius:14;" +
            "-fx-border-width:1;-fx-padding:16;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.3),6,0,0,2);"));

        VBox wrapper = new VBox(card);
        wrapper.setPadding(new Insets(4, 0, 4, 0));
        return wrapper;
    }

    // ── Analyser avec Claude IA ───────────────────────────────────────────────
    private void analyserReclamation(Map<String, String> r, VBox card) {
        // Trouver le label IA dans la carte
        Label lblIA = (Label) card.lookup("#ia-" + r.get("id"));
        if (lblIA != null) {
            lblIA.setText("🧠 Analyse IA en cours...");
            lblIA.setStyle("-fx-text-fill:#c4b5fd;-fx-font-size:11;");
        }

        // Créer une alerte temporaire pour l'analyse
        String niveau = switch (r.get("alert_level")) {
            case "High"   -> "Critique";
            case "Medium" -> "Haute";
            default       -> "Moyenne";
        };
        String typeAlerte = detecterType(r.get("details"));
        String localisation = r.get("zone") +
            (r.get("location").isEmpty() ? "" : ", " + r.get("location"));

        Alerte alerteTemp = new Alerte(typeAlerte, niveau, localisation, "Citoyen");

        new Thread(() -> {
            AnalyseAlerteIA.AnalyseResultat res = ia.analyser(alerteTemp);
            Platform.runLater(() -> {
                afficherPopupAnalyse(res, alerteTemp, r);
                if (lblIA != null) {
                    lblIA.setText("✅ Analyse terminée — voir popup");
                    lblIA.setStyle("-fx-text-fill:#4ade80;-fx-font-size:11;");
                }
            });
        }, "ia-recl-" + r.get("id")).start();
    }

    // ── Afficher popup analyse ────────────────────────────────────────────────
    private void afficherPopupAnalyse(AnalyseAlerteIA.AnalyseResultat res,
                                       Alerte a, Map<String, String> r) {
        javafx.stage.Stage popup = new javafx.stage.Stage();
        popup.setTitle("Analyse IA — Réclamation #" + r.get("id"));
        
        // Utiliser conteneurReclamations au lieu de lblTotal pour obtenir la fenêtre
        if (conteneurReclamations != null && conteneurReclamations.getScene() != null) {
            popup.initOwner(conteneurReclamations.getScene().getWindow());
        }

        String couleur = switch (a.getNiveau()) {
            case "Critique" -> "#ef4444";
            case "Haute"    -> "#f97316";
            default         -> "#3b82f6";
        };

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color:#0a1612;");

        // Header
        HBox header = new HBox(12);
        header.setStyle("-fx-background-color:" + couleur + ";-fx-padding:16 24;");
        header.setAlignment(Pos.CENTER_LEFT);
        Label ico = new Label("🧠");
        ico.setStyle("-fx-font-size:26;");
        VBox ht = new VBox(3);
        Label titre = new Label("Analyse IA — Réclamation Citoyenne #" + r.get("id"));
        titre.setStyle("-fx-text-fill:white;-fx-font-size:15;-fx-font-weight:bold;");
        Label sub = new Label(a.getTypeAlerte() + " | " + a.getNiveau() + " | " + r.get("zone"));
        sub.setStyle("-fx-text-fill:rgba(255,255,255,0.85);-fx-font-size:11;");
        ht.getChildren().addAll(titre, sub);
        header.getChildren().addAll(ico, ht);

        VBox content = new VBox(10);
        content.setStyle("-fx-padding:16;");

        // Info réclamation
        VBox infoBox = new VBox(4);
        infoBox.setStyle("-fx-background-color:#0d1a2e;-fx-background-radius:10;" +
            "-fx-border-color:#1e3a5f;-fx-border-radius:10;-fx-border-width:1;-fx-padding:12;");
        Label infoTitre = new Label("📋 Réclamation Citoyenne");
        infoTitre.setStyle("-fx-text-fill:#90CAF9;-fx-font-size:12;-fx-font-weight:bold;");
        Label infoDetails = new Label("Reporté par : " + r.get("reporter_name") +
            "\nDétails : " + r.get("details"));
        infoDetails.setStyle("-fx-text-fill:#e2e8f0;-fx-font-size:11;");
        infoDetails.setWrapText(true);
        infoBox.getChildren().addAll(infoTitre, infoDetails);
        content.getChildren().add(infoBox);

        // Grille analyse IA
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10); grid.setVgap(10);
        ajCarte(grid, 0, 0, "🌡 Météo & Conditions",   res.meteo,             "#ef4444");
        ajCarte(grid, 1, 0, "⏱ Temps d'Intervention", res.tempsIntervention, "#f97316");
        ajCarte(grid, 0, 1, "⚠ Risque Propagation",   res.risquePropagation, "#fbbf24");
        ajCarte(grid, 1, 1, "🗺 Zones Voisines",       res.zonesVoisines,     "#6366f1");
        javafx.scene.layout.ColumnConstraints cc = new javafx.scene.layout.ColumnConstraints(260);
        grid.getColumnConstraints().addAll(cc, cc);
        content.getChildren().add(grid);

        // Pompiers
        HBox pompBox = new HBox(12);
        pompBox.setStyle("-fx-background-color:#052e16;-fx-background-radius:10;" +
            "-fx-border-color:#16a34a;-fx-border-radius:10;-fx-border-width:1;-fx-padding:14;");
        pompBox.setAlignment(Pos.CENTER_LEFT);
        Label pompIco = new Label("🚒");
        pompIco.setStyle("-fx-font-size:26;");
        Label pnb = new Label(res.nbPompiers);
        pnb.setStyle("-fx-text-fill:#4ade80;-fx-font-size:15;-fx-font-weight:bold;");
        pnb.setWrapText(true);
        pompBox.getChildren().addAll(pompIco, pnb);
        content.getChildren().add(pompBox);

        ajSection(content, "🚛 Matériel Nécessaire",    res.materiel,         "#f97316");
        ajSection(content, "📋 Plan d'Intervention",    res.planIntervention, "#4ade80");

        // Boutons
        Button btnCreer = new Button("➕ Créer alerte officielle");
        btnCreer.setStyle("-fx-background-color:#16a34a;-fx-text-fill:white;" +
            "-fx-background-radius:8;-fx-border-width:0;-fx-padding:10 20;" +
            "-fx-font-size:12;-fx-font-weight:bold;-fx-cursor:hand;");
        btnCreer.setOnAction(ev -> {
            creerAlerteDepuisReclamation(r, r.get("alert_level"), btnCreer);
            popup.close();
        });

        Button btnFermer = new Button("Fermer");
        btnFermer.setStyle("-fx-background-color:#1a2e1e;-fx-text-fill:#94a3b8;" +
            "-fx-background-radius:8;-fx-border-color:#2d5a37;" +
            "-fx-border-radius:8;-fx-border-width:1;-fx-padding:10 20;" +
            "-fx-font-size:12;-fx-cursor:hand;");
        btnFermer.setOnAction(ev -> popup.close());

        HBox footer = new HBox(10, btnCreer, btnFermer);
        footer.setAlignment(Pos.CENTER);
        footer.setStyle("-fx-padding:14;-fx-border-color:#1a3a20;-fx-border-width:1 0 0 0;");

        ScrollPane scroll = new ScrollPane(content);
        scroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;" +
            "-fx-border-color:transparent;");
        scroll.setFitToWidth(true);

        root.getChildren().addAll(header, scroll, footer);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        popup.setScene(new javafx.scene.Scene(root, 580, 640));
        popup.show();
    }

    // ── Créer alerte officielle depuis réclamation ────────────────────────────
    private void creerAlerteDepuisReclamation(Map<String, String> r,
                                               String level, Button btn) {
        String niveau = switch (level) {
            case "High"   -> "Critique";
            case "Medium" -> "Haute";
            default       -> "Moyenne";
        };
        String type = detecterType(r.get("details"));
        String loc  = r.get("zone") +
            (r.get("location").isEmpty() ? "" : ", " + r.get("location"));

        Alerte a = new Alerte(type, niveau, loc, "Citoyen");
        if (alerteService.creerAlerte(a)) {
            btn.setText("✅ Alerte créée !");
            btn.setStyle("-fx-background-color:#0d2418;-fx-text-fill:#4ade80;" +
                "-fx-background-radius:8;-fx-border-width:0;" +
                "-fx-padding:8 16;-fx-cursor:default;-fx-font-size:12;");
            btn.setDisable(true);
        }
    }

    // ── Détecter le type d'alerte depuis les détails ──────────────────────────
    private String detecterType(String details) {
        if (details == null) return "Incendie";
        String d = details.toLowerCase();
        if (d.contains("incendie") || d.contains("feu") || d.contains("flamme") || d.contains("fire"))
            return "Incendie";
        if (d.contains("fumee") || d.contains("fumée") || d.contains("smoke") || d.contains("دخان"))
            return "Fumee";
        if (d.contains("chaleur") || d.contains("temperature") || d.contains("chaud"))
            return "Chaleur excessive";
        return "Incendie";
    }

    private void ajCarte(javafx.scene.layout.GridPane g, int col, int row,
                          String label, String val, String couleur) {
        VBox card = new VBox(5);
        card.setStyle("-fx-background-color:#0d1f14;-fx-background-radius:10;" +
            "-fx-border-color:" + couleur + "55;-fx-border-radius:10;" +
            "-fx-border-width:1;-fx-padding:12;");
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill:" + couleur + ";-fx-font-size:11;-fx-font-weight:bold;");
        Label v = new Label(val);
        v.setStyle("-fx-text-fill:#e2e8f0;-fx-font-size:11;");
        v.setWrapText(true);
        card.getChildren().addAll(lbl, v);
        g.add(card, col, row);
    }

    private void ajSection(VBox parent, String titre, String contenu, String couleur) {
        VBox box = new VBox(6);
        box.setStyle("-fx-background-color:#0d1f14;-fx-background-radius:10;" +
            "-fx-border-color:" + couleur + "55;-fx-border-radius:10;" +
            "-fx-border-width:1;-fx-padding:12;");
        Label t = new Label(titre);
        t.setStyle("-fx-text-fill:" + couleur + ";-fx-font-size:12;-fx-font-weight:bold;");
        Label ct = new Label(contenu);
        ct.setStyle("-fx-text-fill:#e2e8f0;-fx-font-size:11;");
        ct.setWrapText(true);
        box.getChildren().addAll(t, ct);
        parent.getChildren().add(box);
    }

    private String nvl(String s) { return s != null ? s : ""; }

    @FXML private void actualiser() { chargerReclamations(); }

    @FXML private void goDashboard() {
        if (scheduler != null) scheduler.shutdown();
        // Utiliser conteneurReclamations au lieu de lblTotal pour obtenir la fenêtre
        if (conteneurReclamations != null && conteneurReclamations.getScene() != null) {
            Stage s = (Stage) conteneurReclamations.getScene().getWindow();
            NavigationManager.navigateTo(s, "/fxml/Dashboard.fxml");
        }
    }
}
