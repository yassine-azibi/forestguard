package com.forestguard.controllers;

import com.forestguard.entities.Utilisateur;
import com.forestguard.utils.Session;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AlertsController {
    private static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    private ComboBox<String> zoneFilterCombo;

    @FXML
    private ComboBox<String> levelFilterCombo;

    @FXML
    private VBox alertsListBox;

    @FXML
    private ImageView backgroundImage;

    private Utilisateur utilisateur;

    private final ObservableList<FireAlert> allAlerts = FXCollections.observableArrayList();

    public void setUtilisateur(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
    }

    @FXML
    private void initialize() {
        Platform.runLater(() -> {
            if (backgroundImage != null && zoneFilterCombo.getScene() != null) {
                backgroundImage.fitWidthProperty().bind(zoneFilterCombo.getScene().getWindow().widthProperty());
                backgroundImage.fitHeightProperty().bind(zoneFilterCombo.getScene().getWindow().heightProperty());
            }
        });

        levelFilterCombo.setItems(FXCollections.observableArrayList("Tous les niveaux", "Critical", "Medium", "Faible"));
        levelFilterCombo.setValue("Tous les niveaux");

        zoneFilterCombo.setItems(FXCollections.observableArrayList("Toutes les zones"));
        zoneFilterCombo.setValue("Toutes les zones");

        zoneFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> renderAlerts());
        levelFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> renderAlerts());

        loadAlertsFromDatabase();
        refreshZoneFilters();
        renderAlerts();
    }

    @FXML
    private void handleBackToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/forestguard/views/dashboard.fxml"));
            Parent root = loader.load();
            DashboardController controller = loader.getController();
            controller.setUtilisateur(utilisateur != null ? utilisateur : Session.getCurrentUser());

            Stage stage = (Stage) alertsListBox.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("ForestGuard - Dashboard");
            stage.setMaximized(true);
        } catch (IOException exception) {
            showError("Error", "Unable to return to dashboard.");
        }
    }

    private void loadAlertsFromDatabase() {
        allAlerts.clear();

        String sql = "SELECT id, zone, specific_location, details, photo_path, reporter_name, reporter_email, alert_level, status, created_at "
                + "FROM fire_alerts ORDER BY created_at DESC, id DESC";

        try (Connection connection = com.forestguard.utils.MyConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                allAlerts.add(mapFireAlert(resultSet));
            }
        } catch (SQLException exception) {
            showError("Database error", "Unable to load fire alerts from the database.");
            System.err.println("ERROR: Failed to load fire alerts: " + exception.getMessage());
        }
    }

    private void refreshZoneFilters() {
        String selectedZone = zoneFilterCombo.getValue();
        ObservableList<String> zones = FXCollections.observableArrayList("Toutes les zones");
        zones.addAll(allAlerts.stream()
                .map(FireAlert::zone)
                .filter(zone -> zone != null && !zone.isBlank())
                .distinct()
                .sorted()
                .toList());

        zoneFilterCombo.setItems(zones);
        if (selectedZone != null && zones.contains(selectedZone)) {
            zoneFilterCombo.setValue(selectedZone);
        } else {
            zoneFilterCombo.setValue("Toutes les zones");
        }
    }

    private void renderAlerts() {
        alertsListBox.getChildren().clear();

        List<FireAlert> filtered = allAlerts.stream()
                .filter(this::matchesZone)
                .filter(this::matchesLevel)
                .toList();

        if (filtered.isEmpty()) {
            Label emptyLabel = new Label("Aucune alerte trouvée pour les filtres actuels.");
            emptyLabel.getStyleClass().add("alerts-empty");
            alertsListBox.getChildren().add(emptyLabel);
            return;
        }

        for (FireAlert alert : filtered) {
            alertsListBox.getChildren().add(createAlertCard(alert));
        }
    }

    private boolean matchesZone(FireAlert fireAlert) {
        String selectedZone = zoneFilterCombo.getValue();
        return selectedZone == null || selectedZone.equals("Toutes les zones") || fireAlert.zone().equals(selectedZone);
    }

    private boolean matchesLevel(FireAlert fireAlert) {
        String selectedLevel = levelFilterCombo.getValue();
        return selectedLevel == null || selectedLevel.equals("Tous les niveaux") || fireAlert.level().equalsIgnoreCase(selectedLevel);
    }

    private HBox createAlertCard(FireAlert fireAlert) {
        Label zoneName = new Label(fireAlert.zone());
        zoneName.getStyleClass().add("alert-zone-name");

        Label levelBadge = new Label(fireAlert.level());
        levelBadge.getStyleClass().addAll("alert-badge", levelStyleClass(fireAlert.level()));

        Label statusBadge = new Label(fireAlert.status());
        statusBadge.getStyleClass().addAll("alert-badge", statusStyleClass(fireAlert.status()));

        HBox badges = new HBox(8, levelBadge, statusBadge);
        badges.setAlignment(Pos.CENTER_LEFT);

        Label locationLabel = new Label("📍 " + fireAlert.coordinates());
        locationLabel.getStyleClass().add("alert-meta");

        Label timestamp = new Label("🕒 " + fireAlert.dateTime());
        timestamp.getStyleClass().add("alert-meta");

        HBox metadata = new HBox(16, locationLabel, timestamp);
        metadata.setAlignment(Pos.CENTER_LEFT);

        VBox leftContent = new VBox(10, zoneName, badges, metadata);
        HBox.setHgrow(leftContent, Priority.ALWAYS);

        Button viewDetailsButton = new Button("Voir les détails");
        viewDetailsButton.getStyleClass().add("view-details-button");
        viewDetailsButton.setOnAction(event -> showDetails(fireAlert));

        HBox card = new HBox(16, leftContent, viewDetailsButton);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("alert-card");
        card.setPadding(new Insets(16, 18, 16, 18));
        return card;
    }

    private String levelStyleClass(String level) {
        return switch (level.toLowerCase()) {
            case "critical" -> "badge-critical";
            case "medium"   -> "badge-medium";
            default         -> "badge-low";
        };
    }

    private String statusStyleClass(String status) {
        return switch (status.toLowerCase()) {
            case "active", "actif" -> "badge-active";
            default                -> "badge-contained";
        };
    }

    /**
     * Affiche la popup "Voir les détails" avec toutes les informations du rapport,
     * y compris la photo si elle existe.
     * Les SMS/Email ne sont PAS envoyés ici — ils sont déclenchés uniquement
     * lors de l'envoi d'un signal dans SignalController.
     */
    private void showDetails(FireAlert fireAlert) {

        // ── Fenêtre modale ────────────────────────────────────────────────────
        Stage detailStage = new Stage();
        detailStage.initModality(Modality.APPLICATION_MODAL);
        detailStage.setTitle("Détails de l'alerte — " + fireAlert.zone());
        detailStage.setMinWidth(480);

        // ── En-tête ───────────────────────────────────────────────────────────
        Label titleLabel = new Label(fireAlert.zone() + " — " + fireAlert.level());
        titleLabel.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:#c0392b;");

        // ── Champs de détail ──────────────────────────────────────────────────
        VBox infoBox = new VBox(8);
        infoBox.getChildren().addAll(
                makeDetailRow("Statut",       fireAlert.status()),
                makeDetailRow("Localisation", "📍 " + fireAlert.coordinates()),
                makeDetailRow("Signalé par",  fireAlert.reporterName()),
                makeDetailRow("Heure",        "🕒 " + fireAlert.dateTime()),
                makeDetailRow("Détails",      fireAlert.details())
        );

        // ── Photo (si disponible) ─────────────────────────────────────────────
        VBox photoBox = new VBox(6);
        String photoPath = fireAlert.photoPath();
        if (photoPath != null && !photoPath.isBlank()) {
            File photoFile = new File(photoPath);
            if (photoFile.exists() && photoFile.isFile()) {
                try (InputStream is = new FileInputStream(photoFile)) {
                    Image img = new Image(is);
                    ImageView imageView = new ImageView(img);
                    imageView.setPreserveRatio(true);
                    imageView.setFitWidth(440);
                    imageView.setFitHeight(260);
                    imageView.setStyle("-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.25),8,0,0,2);"
                            + "-fx-border-radius:6;");

                    Label photoLabel = new Label("📷 Photo du signalement");
                    photoLabel.setStyle("-fx-font-weight:bold;-fx-text-fill:#374151;-fx-font-size:13px;");
                    photoBox.getChildren().addAll(photoLabel, imageView);
                } catch (IOException e) {
                    System.err.println("[AlertsController] Impossible de charger la photo : " + e.getMessage());
                }
            }
        }

        // ── Bouton Fermer ─────────────────────────────────────────────────────
        Button closeBtn = new Button("Fermer");
        closeBtn.setStyle("-fx-background-color:#c0392b;-fx-text-fill:white;"
                + "-fx-font-weight:bold;-fx-padding:8 24;-fx-border-radius:6;-fx-background-radius:6;");
        closeBtn.setOnAction(e -> detailStage.close());
        HBox btnBox = new HBox(closeBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        // ── Assemblage ────────────────────────────────────────────────────────
        VBox content = new VBox(16, titleLabel, infoBox, photoBox, btnBox);
        content.setPadding(new Insets(24));
        content.setStyle("-fx-background-color:white;");

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color:white;-fx-border-color:transparent;");

        detailStage.setScene(new Scene(scrollPane, 500, 520));
        detailStage.showAndWait();
    }

    /** Crée une ligne label : valeur pour la popup de détails. */
    private HBox makeDetailRow(String label, String value) {
        Label lbl = new Label(label + " :");
        lbl.setStyle("-fx-font-weight:bold;-fx-text-fill:#c0392b;-fx-min-width:110;-fx-font-size:13px;");
        Label val = new Label(value == null ? "—" : value);
        val.setStyle("-fx-text-fill:#334155;-fx-font-size:13px;");
        val.setWrapText(true);
        HBox.setHgrow(val, Priority.ALWAYS);
        HBox row = new HBox(8, lbl, val);
        row.setAlignment(Pos.TOP_LEFT);
        return row;
    }


    private FireAlert mapFireAlert(ResultSet resultSet) throws SQLException {
        Timestamp createdAt = resultSet.getTimestamp("created_at");
        String dateTime = createdAt == null ? "Unknown" : DISPLAY_DATE_FORMAT.format(createdAt.toLocalDateTime());

        return new FireAlert(
                resultSet.getLong("id"),
                defaultIfBlank(resultSet.getString("zone"), "Zone inconnue"),
                defaultIfBlank(resultSet.getString("alert_level"), "Faible"),
                defaultIfBlank(resultSet.getString("status"), "Actif"),
                defaultIfBlank(resultSet.getString("specific_location"), "Non renseigné"),
                dateTime,
                defaultIfBlank(resultSet.getString("details"), "Aucun détail fourni"),
                resultSet.getString("photo_path"),
                defaultIfBlank(resultSet.getString("reporter_name"), "Utilisateur inconnu"),
                resultSet.getString("reporter_email")
        );
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private record FireAlert(long id,
                             String zone,
                             String level,
                             String status,
                             String coordinates,
                             String dateTime,
                             String details,
                             String photoPath,
                             String reporterName,
                             String reporterEmail) {
    }
}