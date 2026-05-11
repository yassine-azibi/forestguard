package com.forestguard.controllers;

import com.forestguard.entities.Utilisateur;
import com.forestguard.services.UtilisateurService;
import com.forestguard.utils.EmailService;
import com.forestguard.utils.GovernorateUtils;
import com.forestguard.utils.LocationDetectionService;
import com.forestguard.utils.LocationDetectionService.LocationDetectionException;
import com.forestguard.utils.MyConnection;
import com.forestguard.utils.Session;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SignalController {
    private List<String> loadZonesFromDatabase() {
        List<String> zones = new ArrayList<>();
        String sql = "SELECT nom FROM foret ORDER BY nom";
        try (Connection connection = MyConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                zones.add(resultSet.getString("nom"));
            }
        } catch (SQLException e) {
            System.err.println("Erreur chargement zones : " + e.getMessage());
        }
        return zones;
    }

    @FXML
    private ComboBox<String> zoneComboBox;

    @FXML
    private ComboBox<String> locationComboBox;

    @FXML
    private Label locationStatusLabel;

    @FXML
    private Button autoDetectButton;

    @FXML
    private javafx.scene.control.TextField fullAddressField;

    @FXML
    private javafx.scene.layout.VBox fullAddressBox;

    @FXML
    private TextField gpsCoordinatesField;

    @FXML
    private VBox gpsCoordinatesBox;

    @FXML
    private Button copyGpsButton;

    @FXML
    private TextArea detailsArea;

    @FXML
    private ComboBox<String> niveauComboBox;

    @FXML
    private Label uploadStatusLabel;

    @FXML
    private ImageView photoPreview;

    @FXML
    private ScrollPane contentScrollPane;

    @FXML
    private ImageView backgroundImage;

        @FXML
        private Label tickerContent;

        @FXML
        private javafx.scene.layout.HBox  tickerBar;

        @FXML
        private Label clockLabel;

        private javafx.animation.SequentialTransition tickerLoop;

        private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(8))
            .build();

    private Utilisateur utilisateur;
    private File selectedPhoto;

    public void setUtilisateur(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
    }

    @FXML
    private void initialize() {
        if (contentScrollPane != null) {
            contentScrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-control-inner-background: transparent; -fx-padding: 0; -fx-border-color: transparent; -fx-border-width: 0;");

            Platform.runLater(() -> {
                if (contentScrollPane.getSkin() != null) {
                    var viewport = contentScrollPane.lookup(".viewport");
                    if (viewport != null) {
                        viewport.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
                    }
                    var corner = contentScrollPane.lookup(".corner");
                    if (corner != null) {
                        corner.setStyle("-fx-background-color: transparent;");
                    }
                    var trackBackground = contentScrollPane.lookup(".track-background");
                    if (trackBackground != null) {
                        trackBackground.setStyle("-fx-background-color: transparent;");
                    }
                }
            });
        }

        backgroundImage.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene != null) {
                Platform.runLater(() -> {
                    if (newScene.getWindow() != null) {
                        backgroundImage.fitWidthProperty().bind(newScene.getWindow().widthProperty());
                        backgroundImage.fitHeightProperty().bind(newScene.getWindow().heightProperty());
                    }
                });
            }
        });

        backgroundImage.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((windowObservable, oldWindow, newWindow) -> {
                    if (newWindow != null) {
                        Platform.runLater(() -> {
                            backgroundImage.fitWidthProperty().bind(newWindow.widthProperty());
                            backgroundImage.fitHeightProperty().bind(newWindow.heightProperty());
                        });
                    }
                });
            }
        });

        zoneComboBox.setItems(FXCollections.observableArrayList(loadZonesFromDatabase()));
        locationComboBox.setItems(FXCollections.observableArrayList(GovernorateUtils.getGovernorates()));
        locationComboBox.setEditable(true);

        // ── Niveau d'incendie ─────────────────────────────────────────────────
        if (niveauComboBox != null) {
            niveauComboBox.setItems(FXCollections.observableArrayList("Low", "Medium", "Critical"));
            niveauComboBox.setValue("Low");
        }

        updateLocationStatus("Choisissez une ville ou lancez l'auto-detection.");
        locationComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.isBlank()) {
                updateLocationStatus("Selectionnez une ville dans la liste ou saisissez-la manuellement.");
            } else {
                updateLocationStatus("Localisation selectionnee : " + newValue);
            }
        });

        Platform.runLater(() -> startTicker("🌿 ForestGuard — Chargement des données météo..."));
        loadWeatherForTickerAsync();

        javafx.animation.Timeline clock = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(Duration.seconds(1), e -> updateClock())
        );
        clock.setCycleCount(javafx.animation.Animation.INDEFINITE);
        clock.play();
        updateClock();
    }

    @FXML
    private void handleAutoDetectLocation() {
        if (locationComboBox == null) return;

        // Désactiver le bouton pour éviter les doubles clics
        if (autoDetectButton != null) {
            autoDetectButton.setDisable(true);
            autoDetectButton.setText("⏳ Détection...");
        }
        // Cacher l'adresse précédente pendant la détection
        setFullAddressVisible(false);
        updateLocationStatus("🔍 Détection de votre position...");

        // Appel réseau sur thread daemon — ne jamais bloquer le thread JavaFX
        Thread detectionThread = new Thread(() -> {
            try {
                LocationDetectionService.DetectedLocation result =
                        LocationDetectionService.detect();

                Platform.runLater(() -> {
                    // 1. Sélectionner le gouvernorat dans la ComboBox
                    applyDetectedLocation(result.governorate);

                    // 2. Afficher l'adresse complète
                    String address = result.fullAddress;
                    if (address != null && !address.isBlank()) {
                        if (fullAddressField != null) {
                            fullAddressField.setText(address);
                        }
                        setFullAddressVisible(true);
                    }

                    // 3. Afficher les coordonnées GPS formatées
                    String coords = formatGpsCoordinates(result.lat, result.lon);
                    if (!coords.isBlank()) {
                        if (gpsCoordinatesField != null) {
                            gpsCoordinatesField.setText(coords);
                            // Tooltip sur le champ pour indiquer le clic-copie
                            gpsCoordinatesField.setTooltip(
                                new javafx.scene.control.Tooltip("Click 📋 to copy coordinates"));
                        }
                        setGpsCoordinatesVisible(true);
                    }

                    // 4. Message de statut combiné
                    String statusMsg = "✅ " + result.governorate;
                    if (!coords.isBlank()) statusMsg += "  •  " + coords;
                    updateLocationStatus(statusMsg);
                });

            } catch (LocationDetectionException e) {
                Platform.runLater(() -> {
                    locationComboBox.getSelectionModel().clearSelection();
                    locationComboBox.setValue(null);
                    locationComboBox.getEditor().clear();
                    setFullAddressVisible(false);
                    setGpsCoordinatesVisible(false);
                    updateLocationStatus("❌ " + e.getMessage());
                    showError("Location unavailable", e.getMessage());
                });
            } finally {
                Platform.runLater(() -> {
                    if (autoDetectButton != null) {
                        autoDetectButton.setDisable(false);
                        autoDetectButton.setText("📍 Auto-détecter ma position");
                    }
                });
            }
        });
        detectionThread.setDaemon(true);
        detectionThread.start();
    }

    /** Affiche ou cache le bloc adresse complète. */
    private void setFullAddressVisible(boolean visible) {
        if (fullAddressBox != null) {
            fullAddressBox.setVisible(visible);
            fullAddressBox.setManaged(visible);
        }
    }

    /** Affiche ou cache le bloc coordonnées GPS. */
    private void setGpsCoordinatesVisible(boolean visible) {
        if (gpsCoordinatesBox != null) {
            gpsCoordinatesBox.setVisible(visible);
            gpsCoordinatesBox.setManaged(visible);
        }
    }

    /**
     * Formate les coordonnées brutes en chaîne lisible avec 6 décimales.
     *
     * <p>Format : {@code "36.806500° N, 10.181500° E"}</p>
     * <ul>
     *   <li>Latitude positive → N (North), négative → S (South)</li>
     *   <li>Longitude positive → E (East), négative → W (West)</li>
     * </ul>
     *
     * @param lat latitude décimale sous forme de chaîne (peut être {@code null})
     * @param lon longitude décimale sous forme de chaîne (peut être {@code null})
     * @return coordonnées formatées, ou chaîne vide si les valeurs sont invalides
     */
    private static String formatGpsCoordinates(String lat, String lon) {
        if (lat == null || lon == null) return "";
        try {
            double latD = Double.parseDouble(lat);
            double lonD = Double.parseDouble(lon);
            String latDir = latD >= 0 ? "N" : "S";
            String lonDir = lonD >= 0 ? "E" : "W";
            // 6 décimales = précision ~0.11 m, suffisant pour tout usage terrain
            return String.format("%.6f° %s,  %.6f° %s",
                    Math.abs(latD), latDir,
                    Math.abs(lonD), lonDir);
        } catch (NumberFormatException e) {
            return "";
        }
    }

    /**
     * Copie les coordonnées GPS dans le presse-papiers système.
     * Feedback visuel : le bouton affiche "✅" pendant 1,5 s puis revient à "📋".
     */
    @FXML
    private void handleCopyGpsCoordinates() {
        if (gpsCoordinatesField == null) return;
        String coords = gpsCoordinatesField.getText();
        if (coords == null || coords.isBlank()) return;

        // Copier dans le presse-papiers JavaFX
        ClipboardContent content = new ClipboardContent();
        content.putString(coords);
        Clipboard.getSystemClipboard().setContent(content);

        // Feedback visuel temporaire sur le bouton
        if (copyGpsButton != null) {
            copyGpsButton.setText("✅");
            copyGpsButton.setDisable(true);
            // Remettre l'icône originale après 1,5 s
            Thread resetThread = new Thread(() -> {
                try { Thread.sleep(1500); } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                Platform.runLater(() -> {
                    copyGpsButton.setText("📋");
                    copyGpsButton.setDisable(false);
                });
            });
            resetThread.setDaemon(true);
            resetThread.start();
        }
    }

    private void applyDetectedLocation(String detectedLocation) {
        String normalizedLocation = GovernorateUtils.normalize(detectedLocation);
        if (normalizedLocation.isBlank()) {
            throw new IllegalStateException("La localisation detectee n'est pas reconnue.");
        }

        locationComboBox.setValue(normalizedLocation);
        locationComboBox.getSelectionModel().select(normalizedLocation);
        locationComboBox.getEditor().setText(normalizedLocation);
        updateLocationStatus("Ville detectee : " + normalizedLocation);
    }

    private void updateLocationStatus(String message) {
        if (locationStatusLabel != null) {
            locationStatusLabel.setText(message);
        }
    }

    @FXML
    private void handleUploadPhoto() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select photo evidence");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
        );

        Stage stage = (Stage) zoneComboBox.getScene().getWindow();
        File file = chooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }

        selectedPhoto = file;
        uploadStatusLabel.setText("Photo selected: " + file.getName());
        photoPreview.setImage(new Image(file.toURI().toString(), 140, 100, true, true));
    }

    @FXML
    private void handleSubmitReport() {
        String zone = zoneComboBox.getValue() == null ? "" : zoneComboBox.getValue().trim();
        String specificLocation = resolveSpecificLocation();
        String details = detailsArea.getText() == null ? "" : detailsArea.getText().trim();

        if (zone.isBlank() || details.isBlank()) {
            showError("Incomplete report", "Forest Zone and Fire Details are required.");
            return;
        }

        String reporterName = resolveReporterName();
        String reporterEmail = resolveReporterEmail();
        String photoPath = selectedPhoto == null ? null : selectedPhoto.getAbsolutePath();
        // ── Niveau choisi par l'utilisateur (remplace determineAlertLevel) ────
        String alertLevel = (niveauComboBox != null && niveauComboBox.getValue() != null)
                ? niveauComboBox.getValue()
                : determineAlertLevel(details);
        String status = "Active";

        try {
            long alertId = saveFireAlert(zone, specificLocation, details, photoPath, reporterName, reporterEmail, alertLevel, status);
            int notifiedUsers = notifyZoneUsers(zone, specificLocation, details, reporterName, reporterEmail, photoPath, alertLevel, status);

            StringBuilder message = new StringBuilder("Report submitted successfully.\n\n");
            message.append("Alert ID: ").append(alertId).append("\n");
            message.append("Zone: ").append(zone).append("\n");
            message.append("Location: ").append(specificLocation.isBlank() ? "Not provided" : specificLocation).append("\n");
            message.append("Level: ").append(alertLevel).append("\n");
            message.append("Status: ").append(status).append("\n");
            message.append("Notifications sent to: ").append(notifiedUsers).append(" user(s)");

            showInfo("Report submitted", message.toString());
            clearForm();
        } catch (IllegalStateException exception) {
            showError("Unable to submit report", exception.getMessage());
        }
    }

    private long saveFireAlert(String zone,
                               String specificLocation,
                               String details,
                               String photoPath,
                               String reporterName,
                               String reporterEmail,
                               String alertLevel,
                               String status) {
        String sql = "INSERT INTO fire_alerts (zone, specific_location, details, photo_path, reporter_name, reporter_email, alert_level, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = MyConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, zone);
            statement.setString(2, specificLocation.isBlank() ? null : specificLocation);
            statement.setString(3, details);
            statement.setString(4, photoPath);
            statement.setString(5, reporterName);
            statement.setString(6, reporterEmail);
            statement.setString(7, alertLevel);
            statement.setString(8, status);

            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new IllegalStateException("The fire alert was not saved in the database.");
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
            }

            return -1L;
        } catch (SQLException exception) {
            throw new IllegalStateException("Database save failed: " + exception.getMessage(), exception);
        }
    }

    private int notifyZoneUsers(String zone,
                                String specificLocation,
                                String details,
                                String reporterName,
                                String reporterEmail,
                                String photoPath,
                                String alertLevel,
                                String status) {
        List<Utilisateur> users = new UtilisateurService().getData();
        String subject = "🔥 [ForestGuard] Alerte incendie — " + zone;
        String htmlBody = buildAlertEmailHtml(zone, specificLocation, details, reporterName, reporterEmail, photoPath, alertLevel, status);

        int notifiedUsers = 0;
        for (Utilisateur user : users) {
            if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
                continue;
            }

            EmailService.getInstance().sendEmailHtmlAsync(user.getEmail().trim(), subject, htmlBody);
            notifiedUsers++;
        }

        return notifiedUsers;
    }

    private boolean matchesZone(String userLocalisation, String zone) {
        String normalizedUser = normalize(userLocalisation);
        String normalizedZone = normalize(zone);

        if (normalizedUser.isBlank() || normalizedZone.isBlank()) {
            return false;
        }

        return normalizedUser.contains(normalizedZone) || normalizedZone.contains(normalizedUser);
    }

    private String buildAlertEmailHtml(String zone,
                                       String specificLocation,
                                       String details,
                                       String reporterName,
                                       String reporterEmail,
                                       String photoPath,
                                       String alertLevel,
                                       String status) {

        // ── Variable mapping ──────────────────────────────────────────────────
        String niveau       = alertLevel != null ? alertLevel.toUpperCase() : "INCONNU";
        String localisation = (specificLocation == null || specificLocation.isBlank())
                ? zone : zone + " — " + specificLocation;
        String dateHeure    = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm:ss"));
        String safeDetails  = escapeHtml(details == null ? "" : details).replace("\n", "<br/>");

        // Niveau badge color
        String niveauBg = switch (niveau) {
            case "CRITICAL", "CRITIQUE" -> "#C0392B";
            case "MEDIUM", "HIGH"       -> "#E67E22";
            default                     -> "#27AE60";
        };

        return "<!DOCTYPE html>"
             + "<html lang='fr'>"
             + "<head>"
             + "<meta charset='UTF-8'/>"
             + "<meta name='viewport' content='width=device-width,initial-scale=1.0'/>"
             + "<title>Alerte Incendie — ForestGuard</title>"
             + "</head>"

             // ── Outer wrapper ─────────────────────────────────────────────────
             + "<body style='margin:0;padding:0;background-color:#F4F4F4;"
             +              "font-family:Arial,Helvetica,sans-serif;'>"
             + "<table width='100%' cellpadding='0' cellspacing='0' border='0'"
             +        " style='background-color:#F4F4F4;padding:32px 16px;'>"
             + "<tr><td align='center'>"

             // ── Card container ────────────────────────────────────────────────
             + "<table width='600' cellpadding='0' cellspacing='0' border='0'"
             +        " style='max-width:600px;width:100%;background-color:#FFFFFF;"
             +               "border-radius:8px;overflow:hidden;"
             +               "box-shadow:0 4px 24px rgba(0,0,0,0.12);'>"

             // ══ HEADER ════════════════════════════════════════════════════════
             + "<tr>"
             + "<td align='center'"
             +     " style='background-color:#C0392B;"
             +             "background-image:linear-gradient(135deg,#C0392B 0%,#E74C3C 100%);"
             +             "padding:36px 32px 28px;'>"

             // Alerte sécurité badge
             + "<table cellpadding='0' cellspacing='0' border='0' style='margin-bottom:20px;'>"
             + "<tr><td align='center'"
             +        " style='background-color:rgba(255,255,255,0.18);"
             +               "border:1px solid rgba(255,255,255,0.45);"
             +               "border-radius:20px;padding:7px 22px;'>"
             + "<span style='color:#FFFFFF;font-size:12px;font-weight:700;"
             +              "letter-spacing:1.5px;text-transform:uppercase;'>"
             + "⚠️&nbsp;&nbsp;ALERTE SÉCURITÉ — ZONE CONCERNÉE&nbsp;&nbsp;⚠️"
             + "</span>"
             + "</td></tr></table>"

             // Fire icon
             + "<div style='font-size:52px;line-height:1;margin-bottom:12px;'>🔥</div>"

             // Title
             + "<h1 style='margin:0 0 6px;color:#FFFFFF;font-size:34px;"
             +            "font-weight:900;letter-spacing:2px;text-transform:uppercase;'>"
             + "ALERTE INCENDIE"
             + "</h1>"

             // Subtitle
             + "<p style='margin:0 0 18px;color:rgba(255,255,255,0.88);font-size:13px;"
             +           "letter-spacing:0.5px;'>"
             + "Système ForestGuard — Notification automatique"
             + "</p>"

             // Safety badge
             + "<table cellpadding='0' cellspacing='0' border='0'>"
             + "<tr><td align='center'"
             +        " style='background-color:rgba(255,255,255,0.15);"
             +               "border:1px solid rgba(255,255,255,0.35);"
             +               "border-radius:20px;padding:8px 24px;'>"
             + "<span style='color:#FFFFFF;font-size:13px;font-weight:700;'>"
             + "🛡️&nbsp; Votre sécurité est notre priorité"
             + "</span>"
             + "</td></tr></table>"

             + "</td></tr>"

             // ══ BODY ══════════════════════════════════════════════════════════
             + "<tr>"
             + "<td style='padding:36px 40px 28px;'>"

             // Intro
             + "<h2 style='margin:0 0 10px;color:#2C3E50;font-size:20px;font-weight:700;'>"
             + "Un incendie a été détecté près de votre zone"
             + "</h2>"
             + "<p style='margin:0 0 24px;color:#555555;font-size:15px;line-height:1.7;'>"
             + "Le système ForestGuard a détecté un incendie dans votre secteur. "
             + "Votre sécurité est la priorité absolue. "
             + "Veuillez suivre immédiatement les consignes ci-dessous."
             + "</p>"

             // ── Safety instructions block ─────────────────────────────────────
             + "<table width='100%' cellpadding='0' cellspacing='0' border='0'"
             +        " style='background-color:#FFF8F0;border:1px solid #FBBF24;"
             +               "border-radius:8px;margin-bottom:20px;'>"
             + "<tr><td style='padding:20px 24px;'>"
             + "<p style='margin:0 0 14px;color:#92400E;font-size:14px;font-weight:700;"
             +           "border-bottom:1px solid #FDE68A;padding-bottom:10px;'>"
             + "⚠️&nbsp; Consignes de sécurité — À suivre immédiatement"
             + "</p>"
             + "<table width='100%' cellpadding='4' cellspacing='0' border='0'>"
             + "<tr><td style='font-size:14px;color:#374151;line-height:1.7;'>"
             + "🧘 <strong>Restez calme</strong> et éloignez-vous du danger immédiatement.<br/>"
             + "🚶 <strong>Évacuez la zone</strong> en suivant les voies d'évacuation indiquées.<br/>"
             + "📢 <strong>Suivez les consignes</strong> des autorités locales et de la Protection Civile.<br/>"
             + "🚫 <strong>N'approchez pas</strong> de la zone d'incendie sous aucun prétexte.<br/>"
             + "📞 <strong>Appelez le 197</strong> (Protection Civile Tunisie) en cas d'urgence."
             + "</td></tr>"
             + "</table>"
             + "</td></tr></table>"
             + "<table width='100%' cellpadding='0' cellspacing='0' border='0'"
             +        " style='background-color:#FCEBEB;border:1px solid #F5C6C6;"
             +               "border-radius:8px;margin-bottom:28px;'>"
             + "<tr><td style='padding:22px 24px;'>"

             // Block title
             + "<p style='margin:0 0 16px;color:#C0392B;font-size:15px;font-weight:700;"
             +           "border-bottom:1px solid #F5C6C6;padding-bottom:12px;'>"
             + "🔥&nbsp; Détails de l'alerte"
             + "</p>"

             // Detail rows — table for Outlook compatibility
             + "<table width='100%' cellpadding='0' cellspacing='0' border='0'>"

             + detailRow("Type",        "Incendie de forêt")
             + detailRow("Niveau",      "<span style='background-color:" + niveauBg + ";"
                                       + "color:#FFFFFF;padding:3px 14px;border-radius:4px;"
                                       + "font-size:12px;font-weight:700;'>"
                                       + escapeHtml(niveau) + "</span>")
             + detailRow("Localisation","📍&nbsp;" + escapeHtml(localisation))
             + detailRow("Distance",    "🗺️&nbsp;— km de votre position")
             + detailRow("Détecté le",  "🕐&nbsp;" + escapeHtml(dateHeure))
             + detailRow("Statut",      escapeHtml(status))

             + "</table>"

             // Description
             + "<p style='margin:16px 0 0;background-color:#FFFFFF;border-left:4px solid #E74C3C;"
             +           "padding:12px 16px;border-radius:0 6px 6px 0;"
             +           "font-size:14px;color:#444444;line-height:1.6;'>"
             + "<strong>Description :</strong><br/>" + safeDetails
             + "</p>"

             + "</td></tr></table>"

             // ── Photo du signalement (Base64 inline) ──────────────────────────
             + buildPhotoBlock(photoPath)

             // ── CTA button ────────────────────────────────────────────────────
             + "<table cellpadding='0' cellspacing='0' border='0' style='margin-bottom:8px;'>"
             + "<tr><td align='center'"
             +        " style='background-color:#C0392B;border-radius:8px;'>"
             + "<a href='#'"
             +    " style='display:inline-block;padding:14px 36px;"
             +            "color:#FFFFFF;font-size:15px;font-weight:700;"
             +            "text-decoration:none;letter-spacing:0.5px;'>"
             + "✅&nbsp; Confirmer l'intervention"
             + "</a>"
             + "</td></tr></table>"

             + "</td></tr>"

             // ══ FOOTER ════════════════════════════════════════════════════════
             + "<tr>"
             + "<td align='center'"
             +     " style='background-color:#F8F8F8;border-top:1px solid #EEEEEE;"
             +             "padding:20px 32px;'>"
             + "<p style='margin:0;color:#AAAAAA;font-size:12px;line-height:1.7;'>"
             + "🌲&nbsp; <strong>ForestGuard</strong> — Système de surveillance automatique"
             + "<br/>Ne pas répondre à cet email — message généré automatiquement."
             + "</p>"
             + "</td></tr>"

             + "</table>"  // end card
             + "</td></tr></table>"  // end outer wrapper
             + "</body></html>";
    }

    /**
     * Encode la photo en Base64 et retourne un bloc HTML avec l'image intégrée.
     *
     * <p>Utilise {@code data:image/...;base64,...} pour que l'image soit visible
     * dans l'email sans dépendance à un serveur externe.</p>
     *
     * @param photoPath chemin absolu vers le fichier photo (peut être null)
     * @return bloc HTML avec l'image, ou chaîne vide si pas de photo
     */
    private static String buildPhotoBlock(String photoPath) {
        if (photoPath == null || photoPath.isBlank()) return "";

        java.io.File file = new java.io.File(photoPath);
        if (!file.exists() || !file.isFile()) return "";

        try {
            // ── Lire les octets du fichier ────────────────────────────────────
            byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
            String base64 = java.util.Base64.getEncoder().encodeToString(bytes);

            // ── Détecter le MIME type depuis l'extension ──────────────────────
            String name = file.getName().toLowerCase();
            String mime;
            if      (name.endsWith(".png"))  mime = "image/png";
            else if (name.endsWith(".gif"))  mime = "image/gif";
            else if (name.endsWith(".webp")) mime = "image/webp";
            else                             mime = "image/jpeg"; // jpg / jpeg par défaut

            // ── Bloc HTML avec l'image en data URI ────────────────────────────
            return "<table width='100%' cellpadding='0' cellspacing='0' border='0'"
                 + " style='background-color:#F9F9F9;border:1px solid #EEEEEE;"
                 +         "border-radius:8px;margin-bottom:24px;'>"
                 + "<tr><td style='padding:18px 22px;'>"
                 + "<p style='margin:0 0 12px;color:#555555;font-size:14px;"
                 +           "font-weight:700;'>📷&nbsp; Photo du signalement</p>"
                 + "<img src='data:" + mime + ";base64," + base64 + "'"
                 +      " alt='Photo du signalement'"
                 +      " style='max-width:100%;height:auto;border-radius:6px;"
                 +              "display:block;border:1px solid #DDDDDD;'/>"
                 + "</td></tr></table>";

        } catch (java.io.IOException e) {
            System.err.println("[SignalController] Impossible de lire la photo : " + e.getMessage());
            return "";
        }
    }

    /** Renders a single detail row for the alert details table. */
    private static String detailRow(String label, String value) {
        return "<tr>"
             + "<td style='padding:7px 0;color:#C0392B;font-size:14px;"
             +            "font-weight:700;width:130px;vertical-align:top;'>"
             + escapeHtml(label) + " :"
             + "</td>"
             + "<td style='padding:7px 0 7px 12px;color:#333333;"
             +            "font-size:14px;line-height:1.5;vertical-align:top;'>"
             + value
             + "</td>"
             + "</tr>";
    }

    private String determineAlertLevel(String details) {
        String normalizedDetails = normalize(details);

        if (normalizedDetails.contains("flame")
                || normalizedDetails.contains("wildfire")
                || normalizedDetails.contains("rapid spread")
                || normalizedDetails.contains("spreading fast")
                || normalizedDetails.contains("massive smoke")) {
            return "Critical";
        }

        if (normalizedDetails.contains("smoke")
                || normalizedDetails.contains("ember")
                || normalizedDetails.contains("heat")) {
            return "Medium";
        }

        return "Low";
    }

    private String resolveReporterName() {
        Utilisateur currentUser = utilisateur != null ? utilisateur : Session.getCurrentUser();
        if (currentUser == null) {
            return "Utilisateur inconnu";
        }

        String name = currentUser.getNom() == null ? "" : currentUser.getNom().trim();
        if (!name.isBlank()) {
            return name;
        }

        String email = currentUser.getEmail() == null ? "" : currentUser.getEmail().trim();
        if (!email.isBlank()) {
            return email;
        }

        return "Utilisateur inconnu";
    }

    private String resolveReporterEmail() {
        Utilisateur currentUser = utilisateur != null ? utilisateur : Session.getCurrentUser();
        if (currentUser == null || currentUser.getEmail() == null) {
            return null;
        }

        String email = currentUser.getEmail().trim();
        return email.isBlank() ? null : email;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private void clearForm() {
        zoneComboBox.getSelectionModel().clearSelection();
        zoneComboBox.setValue(null);
        locationComboBox.getSelectionModel().clearSelection();
        locationComboBox.setValue(null);
        locationComboBox.getEditor().clear();
        if (fullAddressField != null)    fullAddressField.clear();
        if (gpsCoordinatesField != null) gpsCoordinatesField.clear();
        setFullAddressVisible(false);
        setGpsCoordinatesVisible(false);
        updateLocationStatus("Select a city or use auto-detection.");
        detailsArea.clear();
        // ── Réinitialiser le niveau à "Low" ───────────────────────────────────
        if (niveauComboBox != null) niveauComboBox.setValue("Low");
        selectedPhoto = null;
        uploadStatusLabel.setText("No photo uploaded");
        photoPreview.setImage(null);
    }

    private String resolveSpecificLocation() {
        if (locationComboBox == null) {
            return "";
        }

        String value = locationComboBox.getEditor().getText();
        if (value == null) {
            return "";
        }

        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            return "";
        }

        String normalized = GovernorateUtils.normalize(trimmed);
        return normalized.isBlank() ? trimmed : normalized;
    }

    @FXML
    private void handleBackToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/forestguard/views/dashboard.fxml"));
            Parent root = loader.load();
            DashboardController controller = loader.getController();
            controller.setUtilisateur(utilisateur != null ? utilisateur : Session.getCurrentUser());

            Stage stage = (Stage) zoneComboBox.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("ForestGuard - Dashboard");
            stage.setMaximized(true);
        } catch (IOException exception) {
            showError("Error", "Unable to return to dashboard.");
        }
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void updateClock() {
        if (clockLabel == null) return;
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        String time = String.format("%02d:%02d:%02d",
            now.getHour(), now.getMinute(), now.getSecond());
        String date = String.format("%d/%d/%d",
            now.getDayOfMonth(), now.getMonthValue(), now.getYear());
        clockLabel.setText(time + "\n" + date);
    }

    private void startTicker(String text) {
        if (tickerContent == null || tickerBar == null) return;
        if (tickerLoop != null) { tickerLoop.stop(); tickerLoop = null; }

        tickerContent.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        tickerContent.setMaxWidth(Double.MAX_VALUE);
        tickerContent.setWrapText(false);
        tickerContent.setText(text);

        Platform.runLater(() -> Platform.runLater(() -> {
            tickerContent.applyCss();
            tickerContent.layout();
            tickerBar.applyCss();
            tickerBar.layout();

            double paneWidth  = tickerBar.getWidth() > 0 ? tickerBar.getWidth() : 1200;
            double labelWidth = tickerContent.prefWidth(-1);
            if (labelWidth <= 0) labelWidth = tickerContent.getBoundsInLocal().getWidth();
            if (labelWidth <= 0) labelWidth = 8000;

            final double fw = paneWidth;
            final double lw = labelWidth;

            tickerContent.setTranslateX(fw);

            javafx.animation.TranslateTransition slide =
                new javafx.animation.TranslateTransition(
                    Duration.seconds(lw / 80.0), tickerContent);
            slide.setFromX(fw);
            slide.setToX(-lw);
            slide.setInterpolator(javafx.animation.Interpolator.LINEAR);
            slide.setCycleCount(1);

            javafx.animation.PauseTransition pause =
                new javafx.animation.PauseTransition(Duration.seconds(6));
            pause.setOnFinished(e -> tickerContent.setTranslateX(fw));

            tickerLoop = new javafx.animation.SequentialTransition(slide, pause);
            tickerLoop.setCycleCount(javafx.animation.Animation.INDEFINITE);
            tickerLoop.play();

            tickerContent.setOnMouseEntered(e -> tickerLoop.pause());
            tickerContent.setOnMouseExited(e  -> tickerLoop.play());
        }));
    }

    private void loadWeatherForTickerAsync() {
        Thread t = new Thread(() -> {
            String tickerText = buildTickerText();
            Platform.runLater(() -> startTicker(tickerText));
        }, "ticker-weather");
        t.setDaemon(true);
        t.start();
    }

    private String buildTickerText() {
        String weatherPart = "";
        try {
            HttpRequest ipReq = HttpRequest.newBuilder()
                .uri(URI.create("http://ip-api.com/json/?fields=lat,lon,city&lang=fr"))
                .timeout(java.time.Duration.ofSeconds(8))
                .build();
            String ipJson = HTTP.send(ipReq,
                HttpResponse.BodyHandlers.ofString()).body();

            double lat  = extractDouble(ipJson, "lat");
            double lon  = extractDouble(ipJson, "lon");
            String city = extractStr(ipJson, "city");

            HttpRequest wReq = HttpRequest.newBuilder()
                .uri(URI.create(
                    "https://api.open-meteo.com/v1/forecast?latitude=" + lat +
                    "&longitude=" + lon +
                    "&current=temperature_2m,windspeed_10m,weathercode&timezone=auto"))
                .timeout(java.time.Duration.ofSeconds(8))
                .build();
            String wJson = HTTP.send(wReq,
                HttpResponse.BodyHandlers.ofString()).body();

            int ci = wJson.indexOf("\"current\"");
            String cb = ci >= 0 ? wJson.substring(ci) : wJson;

            double temp = extractDouble(cb, "temperature_2m");
            double wind = extractDouble(cb, "windspeed_10m");
            int    code = (int) extractDouble(cb, "weathercode");

            String condition = code == 0  ? "☀️ Ciel dégagé"
                             : code <= 3  ? "⛅ Nuageux"
                             : code <= 67 ? "🌧️ Pluie"
                             : code <= 77 ? "❄️ Neige"
                             :              "⛈️ Orageux";

            String risk = (temp > 35 && wind > 30) ? "⚠️ Risque incendie ÉLEVÉ !"
                        : temp > 28                 ? "⚠️ Risque incendie modéré"
                        :                             "✅ Risque incendie faible";

            weatherPart = "🌡️ " + city + " : " + temp + "°C  |  "
                        + "💨 Vent : " + wind + " km/h  |  "
                        + condition + "  |  "
                        + risk + "  |  ";

        } catch (Exception e) {
            weatherPart = "🌐 Météo indisponible  |  ";
        }

        String emergency =
            "📞 Protection Civile : 198  |  " +
            "🌲 Forêts Tunisie : 1828  |  " +
            "🚑 SAMU : 190  |  ";

        String tips =
            "🚫 Ne jamais allumer de feu en forêt en été  |  " +
            "🌳 Ne coupez pas les arbres sans autorisation  |  " +
            "🦎 Respectez la faune et la flore sauvage  |  " +
            "🗑️ Ne laissez aucun déchet en forêt  |  " +
            "⛺ Campez uniquement dans les zones autorisées  |  " +
            "🔥 En cas de fumée, éloignez-vous et appelez le 198  |  " +
            "🐦 Ne dérangez pas les nids d'oiseaux  |  " +
            "🚶 Restez sur les sentiers balisés  |  " +
            "🌿 Ne cueillez pas les plantes protégées  |  " +
            "🔇 Gardez le silence pour respecter la nature  |  " +
            "🌞 Par temps chaud, évitez la forêt l'après-midi  |  " +
            "🐜 Ne détruisez pas les insectes, ils protègent la forêt  |  " +
            "💡 Signalez tout comportement suspect en forêt  |  " +
            "🧯 Gardez un extincteur dans votre voiture en été  |  " +
            "🌱 Plantez des arbres pour compenser votre empreinte carbone  |  " +
            "🚿 Économisez l'eau près des zones forestières  |  " +
            "🐗 Ne nourrissez pas les animaux sauvages en forêt  |  " +
            "🪓 Signalez toute coupe illégale d'arbres au 1828  |  " +
            "🐝 Protégez les abeilles, elles pollinisent la forêt  |  " +
            "🌍 La forêt absorbe le CO2, protégez-la  |  " +
            "🎒 Emportez vos déchets en quittant la forêt  |  " +
            "🌺 Ne cueillez pas les fleurs sauvages protégées  |  " +
            "🐍 Attention aux serpents en été, portez des chaussures fermées  |  " +
            "🌬️ Par vent fort, le risque incendie est maximal  |  " +
            "📸 Photographiez la nature sans la déranger  |  " +
            "🦊 La forêt est un habitat naturel, respectez-la  |  " +
            "🕯️ Jamais de bougies ou lanternes en forêt  |  " +
            "🌙 Évitez la forêt la nuit, les animaux y sont actifs  |  " +
            "🔭 Observez la forêt de loin avec des jumelles  |  " +
            "🐠 Protégez les ruisseaux forestiers, ne les polluez pas  |  ";

        return weatherPart + emergency + tips;
    }

    private static double extractDouble(String json, String key) {
        Matcher m = Pattern.compile(
            "\"" + Pattern.quote(key) + "\"\\s*:\\s*([\\-0-9.]+)")
            .matcher(json);
        return m.find() ? Double.parseDouble(m.group(1)) : 0.0;
    }

    private static String extractStr(String json, String key) {
        Matcher m = Pattern.compile(
            "\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]+)\"")
            .matcher(json);
        return m.find() ? m.group(1) : "";
    }
}