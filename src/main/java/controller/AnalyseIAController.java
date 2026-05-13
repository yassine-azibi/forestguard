package controller;

import model.Alerte;
import service.AlerteService;
import service.AnalyseIAService;
import utils.MyConnection;
import utils.NavigationManager;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AnalyseIAController implements Initializable {

    // ── Upload manuel ─────────────────────────────────────────────────────────
    @FXML private VBox      zoneUpload;
    @FXML private VBox      panelPreview;
    @FXML private ImageView imgPreview;
    @FXML private Label     lblNomFichier;

    // ── Analyse ───────────────────────────────────────────────────────────────
    @FXML private VBox  panelAnalyse;
    @FXML private Label lblEtatAnalyse;
    @FXML private ProgressIndicator progressAnalyse;

    // ── Résultat IA ───────────────────────────────────────────────────────────
    @FXML private VBox  panelResultat;
    @FXML private Label lblNiveauIA;
    @FXML private Label lblTypeIA;
    @FXML private Label lblPompiersIA;
    @FXML private Label lblDescIA;
    @FXML private Label lblConseilsIA;

    // ── Formulaire ────────────────────────────────────────────────────────────
    @FXML private ComboBox<String> cbType;
    @FXML private ComboBox<String> cbNiveau;
    @FXML private TextField        tfLocalisation;
    @FXML private TextField        tfPompiers;
    @FXML private Label            lblErrType;
    @FXML private Label            lblErrNiveau;
    @FXML private Label            lblErrLocalisation;
    @FXML private Label            lblMessage;

    // ── Alertes collègue ──────────────────────────────────────────────────────
    @FXML private VBox    conteneurFireAlerts;
    @FXML private Label   lblNbFireAlerts;
    @FXML private ScrollPane scrollFireAlerts;

    private final AlerteService    alerteService = new AlerteService();
    private final AnalyseIAService iaService     = new AnalyseIAService();
    private File fichierPhoto;
    private ScheduledExecutorService scheduler;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cbType.setItems(FXCollections.observableArrayList(
                "Incendie", "Fumee", "Chaleur excessive", "Secheresse"));
        cbNiveau.setItems(FXCollections.observableArrayList(
                "Critique", "Haute", "Moyenne"));

        // Charger les alertes collègue immédiatement
        chargerFireAlerts();

        // Rafraîchir toutes les 30 secondes
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "fire-alerts-refresh");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(() ->
            Platform.runLater(this::chargerFireAlerts), 30, 30, TimeUnit.SECONDS);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CHARGER LES ALERTES DU COLLÈGUE depuis fire_alerts
    // ══════════════════════════════════════════════════════════════════════════

    private void chargerFireAlerts() {
        if (conteneurFireAlerts == null) return;
        conteneurFireAlerts.getChildren().clear();

        Connection cnx = MyConnection.getInstance().getCnx();
        if (cnx == null) {
            Label err = new Label("BDD non connectee");
            err.setStyle("-fx-text-fill:#EF9A9A;-fx-font-size:12;");
            conteneurFireAlerts.getChildren().add(err);
            return;
        }

        List<String[]> alertes = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT id, zone, specific_location, details, photo_path, " +
                "reporter_name, alert_level, status, created_at " +
                "FROM fire_alerts ORDER BY created_at DESC LIMIT 20");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                alertes.add(new String[]{
                    rs.getString("id"),
                    rs.getString("zone"),
                    rs.getString("specific_location"),
                    rs.getString("details"),
                    rs.getString("photo_path"),
                    rs.getString("reporter_name"),
                    rs.getString("alert_level"),
                    rs.getString("status"),
                    rs.getString("created_at")
                });
            }
        } catch (SQLException e) {
            System.out.println("FireAlerts erreur: " + e.getMessage());
        }

        if (lblNbFireAlerts != null)
            lblNbFireAlerts.setText(alertes.size() + " alerte(s) recue(s)");

        if (alertes.isEmpty()) {
            Label vide = new Label("Aucun signalement citoyen pour le moment");
            vide.setStyle("-fx-text-fill:#64748b;-fx-font-size:12;");
            conteneurFireAlerts.getChildren().add(vide);
            return;
        }

        for (String[] a : alertes) {
            conteneurFireAlerts.getChildren().add(creerCarteFireAlert(a));
        }
    }

    private VBox creerCarteFireAlert(String[] a) {
        String id       = a[0];
        String zone     = a[1] != null ? a[1] : "";
        String loc      = a[2] != null ? a[2] : "";
        String details  = a[3] != null ? a[3] : "";
        String photo    = a[4] != null ? a[4] : "";
        String reporter = a[5] != null ? a[5] : "";
        String level    = a[6] != null ? a[6] : "Low";
        String status   = a[7] != null ? a[7] : "";
        String date     = a[8] != null ? a[8] : "";

        String col = level.equals("High") ? "#ef4444"
                   : level.equals("Medium") ? "#f97316" : "#3b82f6";

        // Barre accent
        Region bar = new Region();
        bar.setPrefWidth(4); bar.setMinWidth(4);
        bar.setStyle("-fx-background-color:" + col + ";-fx-background-radius:4 0 0 4;");

        // Contenu
        Label lblZone = new Label("📍 " + zone + (loc.isEmpty() ? "" : " — " + loc));
        lblZone.setStyle("-fx-text-fill:white;-fx-font-size:13;-fx-font-weight:bold;");
        lblZone.setWrapText(true);

        Label lblDetails = new Label(details.length() > 80 ? details.substring(0, 80) + "..." : details);
        lblDetails.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:11;");
        lblDetails.setWrapText(true);

        Label lblInfo = new Label("👤 " + reporter + "   📅 " + date.substring(0, Math.min(16, date.length())));
        lblInfo.setStyle("-fx-text-fill:#64748b;-fx-font-size:10;");

        // Badge niveau
        Label badgeLevel = new Label(level.toUpperCase());
        badgeLevel.setStyle("-fx-background-color:" + col + ";-fx-text-fill:white;" +
            "-fx-background-radius:20;-fx-padding:3 10;-fx-font-size:10;-fx-font-weight:bold;");

        // Badge statut
        String statCol = status.equals("Active") ? "#ef4444" : "#4ade80";
        Label badgeStatus = new Label(status);
        badgeStatus.setStyle("-fx-background-color:" + statCol + "33;-fx-text-fill:" + statCol + ";" +
            "-fx-background-radius:20;-fx-padding:3 10;-fx-font-size:10;" +
            "-fx-border-color:" + statCol + ";-fx-border-radius:20;-fx-border-width:1;");

        // Bouton Analyser
        Button btnAnalyser = new Button("🤖  Analyser avec l'IA");
        btnAnalyser.setStyle(
            "-fx-background-color:linear-gradient(to right,#7c3aed,#6d28d9);" +
            "-fx-text-fill:white;-fx-background-radius:8;-fx-border-width:0;" +
            "-fx-padding:7 14;-fx-font-size:11;-fx-font-weight:bold;-fx-cursor:hand;");
        btnAnalyser.setOnAction(e -> analyserFireAlert(zone, loc, details, photo, level));

        HBox badges = new HBox(6, badgeLevel, badgeStatus);
        badges.setAlignment(Pos.CENTER_LEFT);

        HBox actions = new HBox();
        actions.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(actions, Priority.ALWAYS);
        actions.getChildren().add(btnAnalyser);

        HBox topRow = new HBox(8, badges, actions);
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox content = new VBox(6, topRow, lblZone, lblDetails, lblInfo);
        content.setPadding(new Insets(10, 14, 10, 10));
        HBox.setHgrow(content, Priority.ALWAYS);

        HBox card = new HBox(0, bar, content);
        card.setStyle(
            "-fx-background-color:#0d1a2e;-fx-background-radius:10;" +
            "-fx-border-color:" + col + "33;-fx-border-radius:10;-fx-border-width:1;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.3),6,0,0,2);");

        card.setOnMouseEntered(ev -> card.setStyle(
            "-fx-background-color:#0f2040;-fx-background-radius:10;" +
            "-fx-border-color:" + col + "88;-fx-border-radius:10;-fx-border-width:1;" +
            "-fx-effect:dropshadow(gaussian," + col + "44,10,0,0,3);"));
        card.setOnMouseExited(ev -> card.setStyle(
            "-fx-background-color:#0d1a2e;-fx-background-radius:10;" +
            "-fx-border-color:" + col + "33;-fx-border-radius:10;-fx-border-width:1;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.3),6,0,0,2);"));

        VBox wrapper = new VBox(card);
        wrapper.setPadding(new Insets(2, 0, 2, 0));
        return wrapper;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  ANALYSER UNE ALERTE COLLEGUE
    // ══════════════════════════════════════════════════════════════════════════

    private void analyserFireAlert(String zone, String loc, String details,
                                    String photoPath, String level) {
        // Essayer d'ouvrir la photo si le chemin existe
        File photo = new File(photoPath);
        if (photo.exists()) {
            // La photo existe sur ce PC (même réseau/PC)
            fichierPhoto = photo;
            imgPreview.setImage(new Image(photo.toURI().toString()));
            lblNomFichier.setText(photo.getName());
            zoneUpload.setVisible(false); zoneUpload.setManaged(false);
            panelPreview.setVisible(true); panelPreview.setManaged(true);
            analyserPhoto();
        } else {
            // Photo non accessible — analyser le texte avec Gemini
            analyserTexteFireAlert(zone, loc, details, level);
        }
    }

    // Analyser via texte si photo non accessible
    private void analyserTexteFireAlert(String zone, String loc,
                                         String details, String level) {
        panelAnalyse.setVisible(true); panelAnalyse.setManaged(true);
        lblEtatAnalyse.setText("Analyse du signalement de " + zone + "...");

        new Thread(() -> {
            // Créer un résultat basé sur les données textuelles
            AnalyseIAService.ResultatAnalyse res = new AnalyseIAService.ResultatAnalyse();
            res.succes = true;
            res.localisation = zone + (loc != null && !loc.isEmpty() ? ", " + loc : "");

            // Niveau basé sur alert_level
            res.niveau = switch (level) {
                case "High"   -> "Critique";
                case "Medium" -> "Haute";
                default       -> "Moyenne";
            };

            // Type basé sur les détails
            String det = details != null ? details.toLowerCase() : "";
            if (det.contains("incendie") || det.contains("feu") || det.contains("flamme")) {
                res.typeAlerte = "Incendie";
            } else if (det.contains("fumee") || det.contains("fumée") || det.contains("smoke")) {
                res.typeAlerte = "Fumee";
            } else if (det.contains("chaleur") || det.contains("temperature")) {
                res.typeAlerte = "Chaleur excessive";
            } else {
                res.typeAlerte = "Incendie";
            }

            res.description = "Signalement citoyen : " + details;
            res.conseils = "1. Verifier la zone " + zone + " immediatement\n"
                         + "2. Contacter les pompiers locaux\n"
                         + "3. Evacuer si necessaire\n"
                         + "4. Enregistrer l'alerte pour suivi";
            res.nbPompiers = level.equals("High") ? 8 : level.equals("Medium") ? 4 : 2;

            Platform.runLater(() -> {
                panelAnalyse.setVisible(false); panelAnalyse.setManaged(false);
                afficherResultat(res);
            });
        }, "fire-alert-analyse").start();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  UPLOAD MANUEL
    // ══════════════════════════════════════════════════════════════════════════

    @FXML private void choisirPhoto() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une photo d'incendie");
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        Stage stage = (Stage) zoneUpload.getScene().getWindow();
        fichierPhoto = fc.showOpenDialog(stage);
        if (fichierPhoto != null) {
            imgPreview.setImage(new Image(fichierPhoto.toURI().toString()));
            lblNomFichier.setText(fichierPhoto.getName()
                + " (" + (fichierPhoto.length() / 1024) + " Ko)");
            zoneUpload.setVisible(false); zoneUpload.setManaged(false);
            panelPreview.setVisible(true); panelPreview.setManaged(true);
            panelResultat.setVisible(false); panelResultat.setManaged(false);
            lblMessage.setText("");
        }
    }

    @FXML private void supprimerPhoto() {
        fichierPhoto = null;
        imgPreview.setImage(null);
        zoneUpload.setVisible(true); zoneUpload.setManaged(true);
        panelPreview.setVisible(false); panelPreview.setManaged(false);
        panelResultat.setVisible(false); panelResultat.setManaged(false);
        panelAnalyse.setVisible(false); panelAnalyse.setManaged(false);
    }

    @FXML private void analyserPhoto() {
        if (fichierPhoto == null) return;
        panelAnalyse.setVisible(true); panelAnalyse.setManaged(true);
        panelResultat.setVisible(false); panelResultat.setManaged(false);
        lblEtatAnalyse.setText("Gemini analyse votre photo...");

        new Thread(() -> {
            AnalyseIAService.ResultatAnalyse res = iaService.analyserImage(fichierPhoto);
            Platform.runLater(() -> {
                panelAnalyse.setVisible(false); panelAnalyse.setManaged(false);
                if (res.succes) afficherResultat(res);
                else {
                    lblMessage.setText("Erreur IA: " + res.erreur);
                    lblMessage.setStyle("-fx-text-fill:#EF9A9A;-fx-font-weight:bold;-fx-font-size:12;");
                }
            });
        }, "ia-analyse").start();
    }

    private void afficherResultat(AnalyseIAService.ResultatAnalyse res) {
        panelResultat.setVisible(true); panelResultat.setManaged(true);
        String col = switch (res.niveau) {
            case "Critique" -> "#ef4444";
            case "Haute"    -> "#f97316";
            default         -> "#3b82f6";
        };
        lblNiveauIA.setText(res.niveau.toUpperCase());
        lblNiveauIA.setStyle("-fx-text-fill:white;-fx-background-color:" + col + ";" +
            "-fx-background-radius:20;-fx-padding:4 14;-fx-font-size:11;-fx-font-weight:bold;");
        lblTypeIA.setText(res.typeAlerte);
        lblPompiersIA.setText(res.nbPompiers + " pompier(s)");
        lblDescIA.setText(res.description);
        lblConseilsIA.setText(res.conseils);

        cbType.setValue(res.typeAlerte);
        cbNiveau.setValue(res.niveau);
        tfLocalisation.setText(res.localisation.isEmpty()
            ? "Zone forestiere — a confirmer" : res.localisation);
        tfPompiers.setText(String.valueOf(res.nbPompiers));

        lblMessage.setText("Formulaire rempli automatiquement. Verifiez et enregistrez.");
        lblMessage.setStyle("-fx-text-fill:#c4b5fd;-fx-font-weight:bold;-fx-font-size:11;");
    }

    @FXML private void enregistrerAlerte() {
        if (cbType.getValue() == null || cbNiveau.getValue() == null
            || tfLocalisation.getText().trim().length() < 3) {
            lblMessage.setText("Remplissez tous les champs obligatoires.");
            lblMessage.setStyle("-fx-text-fill:#EF9A9A;-fx-font-weight:bold;");
            return;
        }
        Alerte a = new Alerte(cbType.getValue(), cbNiveau.getValue(),
                              tfLocalisation.getText().trim(), "IA Vision");
        if (alerteService.creerAlerte(a)) {
            lblMessage.setText("Alerte enregistree ! Email + SMS envoyes.");
            lblMessage.setStyle("-fx-text-fill:#4ade80;-fx-font-weight:bold;-fx-font-size:12;");
            effacer();
        } else {
            lblMessage.setText("Erreur BDD.");
            lblMessage.setStyle("-fx-text-fill:#EF9A9A;-fx-font-weight:bold;");
        }
    }

    @FXML private void effacer() {
        cbType.setValue(null); cbNiveau.setValue(null);
        tfLocalisation.clear(); tfPompiers.clear();
        supprimerPhoto();
        panelResultat.setVisible(false); panelResultat.setManaged(false);
    }

    @FXML private void actualiserAlerts() { chargerFireAlerts(); }

    @FXML private void goDashboard() {
        if (scheduler != null) scheduler.shutdown();
        Stage s = (Stage) lblMessage.getScene().getWindow();
        NavigationManager.navigateTo(s, "/fxml/Dashboard.fxml");
    }
    @FXML private void goCapteurs() {
        if (scheduler != null) scheduler.shutdown();
        Stage s = (Stage) lblMessage.getScene().getWindow();
        NavigationManager.navigateTo(s, "/fxml/Capteurs.fxml");
    }
    @FXML private void goAjout() {
        if (scheduler != null) scheduler.shutdown();
        Stage s = (Stage) lblMessage.getScene().getWindow();
        NavigationManager.navigateTo(s, "/fxml/AjoutAlerte.fxml");
    }
}
