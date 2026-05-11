package com.forestguard.controllers;

import com.forestguard.entities.Utilisateur;
import com.forestguard.services.UtilisateurService;
import com.forestguard.utils.AvatarService;
import com.forestguard.utils.GovernorateUtils;
import com.forestguard.utils.PhoneNumberUtils;
import com.forestguard.utils.Session;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
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
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;

public class ProfileController {
    @FXML
    private TextField nomField;

    @FXML
    private TextField emailField;

    @FXML
    private TextField telephoneField;

    @FXML
    private ComboBox<String> localisationComboBox;

    @FXML
    private Button localisationButton;

    @FXML
    private Button editButton;

    @FXML
    private Button saveButton;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label feedbackLabel;

    @FXML
    private ScrollPane contentScrollPane;

    @FXML
    private VBox profileSection;

    @FXML
    private ImageView avatarImageView;

    @FXML
    private Label profileNameLabel;

    @FXML
    private Label profileEmailLabel;

    @FXML
    private Label profileTelephoneLabel;

    @FXML
    private Label profileLocationLabel;

    @FXML
    private ImageView backgroundImage;

    private final UtilisateurService utilisateurService = new UtilisateurService();
    private final BooleanProperty editMode = new SimpleBooleanProperty(false);
    private final ObservableList<String> alertesRecues = FXCollections.observableArrayList(
            "2026-04-29 10:42 - Zone Nord: fumee detectee pres du point de surveillance 4.",
            "2026-04-29 09:15 - Secteur Est: intervention en cours, gardez le perimetre degage.",
            "2026-04-28 18:05 - Nouvelle alerte prioritaire transmise par le centre de supervision."
    );
    private Utilisateur utilisateur;

    public void setUtilisateur(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
        if (nomField != null) {
            populateFields();
            populateProfileSummary();
        }
    }

    @FXML
    private void initialize() {
        configureEditModeBindings();

        Platform.runLater(() -> {
            if (backgroundImage != null && nomField.getScene() != null) {
                backgroundImage.fitWidthProperty().bind(nomField.getScene().getWindow().widthProperty());
                backgroundImage.fitHeightProperty().bind(nomField.getScene().getWindow().heightProperty());
            }
        });

        // TextFormatter: intercepts input before it's applied — no re-entrant setText() calls.
        // Allows only digits, max 8 characters. Formatting (XX XXX XXX) is applied on submit.
        if (telephoneField != null) {
            telephoneField.setTextFormatter(buildPhoneFormatter());
        }

        if (utilisateur == null) {
            utilisateur = Session.getCurrentUser();
        }

        if (utilisateur != null) {
            populateFields();
            populateProfileSummary();
        }

        setEditMode(false);
    }

    @FXML
    private void handleEnableEditing() {
        setEditMode(true);
        setFeedback("Mode modification activé. Modifiez vos informations puis cliquez sur Enregistrer.", false);
        // Mettre le focus sur le premier champ pour guider l'utilisateur
        if (nomField != null) {
            Platform.runLater(nomField::requestFocus);
        }
    }

    @FXML
    private void handleDetectLocalization() {
        if (!editMode.get() || localisationComboBox == null) return;

        setFeedback("Détection en cours...", false);

        // Unbind avant de modifier manuellement (propriété bindée = lecture seule)
        localisationButton.disableProperty().unbind();
        localisationButton.setDisable(true);
        localisationButton.setText("⏳");

        Thread detectionThread = new Thread(() -> {
            try {
                String governorate = com.forestguard.utils.LocationDetectionService.detectGovernorate();
                Platform.runLater(() -> {
                    localisationComboBox.setValue(governorate);
                    localisationComboBox.getSelectionModel().select(governorate);
                    localisationComboBox.getEditor().setText(governorate);
                    setFeedback("✅ Localisation détectée : " + governorate, false);
                });
            } catch (com.forestguard.utils.LocationDetectionService.LocationDetectionException e) {
                Platform.runLater(() -> setFeedback("❌ " + e.getMessage(), true));
            } finally {
                Platform.runLater(() -> {
                    // Rebind après la détection pour restaurer le comportement normal
                    localisationButton.disableProperty().bind(editMode.not());
                    localisationButton.setText("📍");
                });
            }
        });
        detectionThread.setDaemon(true);
        detectionThread.start();
    }

    @FXML
    private void handleSave() {
        if (utilisateur == null) {
            setFeedback("Aucun utilisateur chargé.", true);
            return;
        }

        String nom         = nomField.getText()        == null ? "" : nomField.getText().trim();
        String email       = emailField.getText()      == null ? "" : emailField.getText().trim();
        String telephone   = telephoneField.getText()  == null ? "" : telephoneField.getText().trim();
        String localisation = localisationComboBox     == null ? "" : GovernorateUtils.normalize(localisationComboBox.getEditor().getText());
        String newPassword = passwordField.getText()   == null ? "" : passwordField.getText();

        // ── Validation des champs obligatoires ────────────────────────────────
        if (nom.isBlank() || email.isBlank() || telephone.isBlank()) {
            setFeedback("Le nom, l'email et le téléphone sont obligatoires.", true);
            return;
        }

        if (localisation.isBlank()) {
            setFeedback("Veuillez sélectionner un gouvernorat valide.", true);
            return;
        }

        telephone = PhoneNumberUtils.normalizeToE164(telephone);
        if (telephone.isBlank()) {
            setFeedback("Le numéro de téléphone est invalide.", true);
            return;
        }

        // ── Unicité email / téléphone ─────────────────────────────────────────
        boolean emailTaken = utilisateurService.emailTakenByAnotherUser(utilisateur.getId(), email);
        boolean phoneTaken = utilisateurService.telephoneTakenByAnotherUser(utilisateur.getId(), telephone);
        if (emailTaken || phoneTaken) {
            StringBuilder sb = new StringBuilder();
            if (emailTaken) sb.append("Cet email est déjà utilisé.");
            if (phoneTaken) {
                if (sb.length() > 0) sb.append("\n");
                sb.append("Ce numéro de téléphone est déjà utilisé.");
            }
            setFeedback(sb.toString(), true);
            return;
        }

        // ── Sauvegarde ────────────────────────────────────────────────────────
        Utilisateur updated = new Utilisateur();
        updated.setNom(nom);
        updated.setEmail(email);
        updated.setTelephone(telephone);
        updated.setLocalisation(localisation);
        updated.setMotDePasse(newPassword);

        try {
            utilisateurService.updateEntity(utilisateur.getId(), updated);
            refreshSessionUser();
            populateProfileSummary();
            passwordField.clear();
            setEditMode(false);
            setFeedback("✅ Profil mis à jour avec succès.", false);
        } catch (IllegalArgumentException exception) {
            setFeedback(exception.getMessage(), true);
        } catch (IllegalStateException exception) {
            showError("Erreur", exception.getMessage());
        }
    }

    @FXML
    private void handleDeleteAccount() throws IOException {
        if (utilisateur == null) {
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Suppression du compte");
        alert.setHeaderText(null);
        alert.setContentText("Voulez-vous vraiment supprimer votre compte ?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            utilisateurService.deleteEntity(utilisateur);
            Session.clear();
            goToLogin();
        }
    }

    @FXML
    private void handleOpenAlerts() {
        Stage dialog = createModalStage("Alertes", 560, 520);

        VBox wrapper = new VBox(16);
        wrapper.setPadding(new Insets(24));
        wrapper.getStyleClass().add("modal-card");

        Label title = new Label("Alertes recues");
        title.getStyleClass().add("modal-title");

        Label subtitle = new Label("Les alertes envoyees par le module de supervision s'affichent ici.");
        subtitle.getStyleClass().add("modal-subtitle");
        subtitle.setWrapText(true);

        ListView<String> listView = new ListView<>(alertesRecues);
        listView.getStyleClass().add("alert-list");
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label text = new Label(item);
                    text.setWrapText(true);
                    text.getStyleClass().add("alert-item-text");

                    VBox card = new VBox(text);
                    card.getStyleClass().add("alert-item");
                    card.setPadding(new Insets(12, 14, 12, 14));
                    setGraphic(card);
                }
            }
        });
        VBox.setVgrow(listView, Priority.ALWAYS);

        Button closeButton = new Button("Fermer");
        closeButton.getStyleClass().add("secondary-button");
        closeButton.setOnAction(event -> dialog.close());

        wrapper.getChildren().addAll(title, subtitle, listView, closeButton);
        Scene scene = new Scene(wrapper);
        scene.getStylesheets().add(getClass().getResource("/com/forestguard/styles/app.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    @FXML
    private void handleOpenSignal() {
        Stage dialog = createModalStage("Signal de flamme", 620, 560);

        VBox wrapper = new VBox(16);
        wrapper.setPadding(new Insets(24));
        wrapper.getStyleClass().add("modal-card");

        Label title = new Label("Declarer un signal");
        title.getStyleClass().add("modal-title");

        Label subtitle = new Label("Renseigne la zone et la description de la flamme observee.");
        subtitle.getStyleClass().add("modal-subtitle");
        subtitle.setWrapText(true);

        TextField zoneField = new TextField();
        zoneField.setPromptText("Zone ou secteur");

        TextField localisationDialogField = new TextField();
        localisationDialogField.setPromptText("Localisation precise / repere");

        TextArea detailsArea = new TextArea();
        detailsArea.setPromptText("Decris la fumee, la flamme, le vent, les risques observes...");
        detailsArea.setPrefRowCount(8);
        detailsArea.setWrapText(true);

        Button sendButton = new Button("Envoyer le signal");
        sendButton.getStyleClass().add("primary-button");
        sendButton.setOnAction(event -> {
            String zone = zoneField.getText() == null ? "" : zoneField.getText().trim();
            String localisation = localisationDialogField.getText() == null ? "" : localisationDialogField.getText().trim();
            String details = detailsArea.getText() == null ? "" : detailsArea.getText().trim();

            if (zone.isBlank() || details.isBlank()) {
                showError("Signal incomplet", "La zone et la description sont obligatoires.");
                return;
            }

            alertesRecues.add(0, "Nouveau signal - " + zone + (localisation.isBlank() ? "" : " - " + localisation) + " : " + details);
            showInfo("Signal envoye", "Le signal a ete prepare pour le module d'alertes.");
            dialog.close();
        });

        Button cancelButton = new Button("Annuler");
        cancelButton.getStyleClass().add("secondary-button");
        cancelButton.setOnAction(event -> dialog.close());

        HBox actions = new HBox(12, cancelButton, sendButton);
        actions.setAlignment(Pos.CENTER_RIGHT);

        wrapper.getChildren().addAll(title, subtitle, zoneField, localisationDialogField, detailsArea, actions);
        Scene scene = new Scene(wrapper);
        scene.getStylesheets().add(getClass().getResource("/com/forestguard/styles/app.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    @FXML
    private void scrollToProfileSection() {
        if (contentScrollPane != null) {
            contentScrollPane.setVvalue(1.0);
        }

        if (profileSection != null) {
            profileSection.requestFocus();
        }
    }

    @FXML
    private void handleLogout() throws IOException {
        Session.clear();
        goToLogin();
    }
    @FXML
    private void handleBackToHome() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/forestguard/views/dashboard.fxml"));
            Parent root = loader.load();
            DashboardController controller = loader.getController();
            controller.setUtilisateur(utilisateur != null ? utilisateur : Session.getCurrentUser());

            Stage stage = (Stage) nomField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("ForestGuard - Dashboard");
            stage.setMaximized(true);
        } catch (IOException exception) {
            showError("Erreur", "Impossible de revenir au dashboard.");
        }
    }

    private void refreshSessionUser() {
        Utilisateur refreshed = utilisateurService.findById(utilisateur.getId())
                .orElseThrow(() -> new IllegalStateException("Utilisateur non trouve apres mise a jour"));
        utilisateur = refreshed;
        Session.setCurrentUser(refreshed);
    }

    private void populateFields() {
        nomField.setText(utilisateur.getNom());
        emailField.setText(utilisateur.getEmail());
        // Strip +216 prefix — the field shows only the 8-digit local number
        telephoneField.setText(PhoneNumberUtils.toLocalDisplay(utilisateur.getTelephone()));
        if (localisationComboBox != null) {
            localisationComboBox.setEditable(true);
            localisationComboBox.setValue(utilisateur.getLocalisation());
        }
        passwordField.clear();
        setEditMode(false);
    }

    private void populateProfileSummary() {
        if (utilisateur == null) {
            return;
        }

        // Charger l'avatar dans un thread séparé pour ne pas bloquer l'UI
        if (avatarImageView != null && utilisateur.getNom() != null && !utilisateur.getNom().isBlank()) {
            Thread avatarThread = new Thread(() -> {
                // Charger l'avatar depuis DiceBear
                javafx.scene.image.Image avatarImage = AvatarService.loadAvatar(utilisateur.getNom());

                // Mettre à jour l'UI sur le thread JavaFX
                Platform.runLater(() -> avatarImageView.setImage(avatarImage));
            });
            avatarThread.setDaemon(true);
            avatarThread.start();
        }

        if (profileNameLabel != null) {
            profileNameLabel.setText(utilisateur.getNom());
        }

        if (profileEmailLabel != null) {
            profileEmailLabel.setText(utilisateur.getEmail());
        }

        if (profileTelephoneLabel != null) {
            profileTelephoneLabel.setText(formatPhoneDisplay(utilisateur.getTelephone()));
        }

        if (profileLocationLabel != null) {
            profileLocationLabel.setText(utilisateur.getLocalisation());
        }
    }

    private Stage createModalStage(String title, int width, int height) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(nomField.getScene().getWindow());
        stage.setTitle(title);
        stage.setWidth(width);
        stage.setHeight(height);
        return stage;
    }

    private void goToLogin() throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/com/forestguard/views/login.fxml"));
        Stage stage = (Stage) nomField.getScene().getWindow();
        stage.setScene(new Scene(root, 1200, 800));
        stage.setTitle("Connexion");
        stage.setMaximized(true);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void configureEditModeBindings() {
        // ── Champs : éditables uniquement en mode édition ─────────────────────
        if (nomField != null) {
            nomField.editableProperty().bind(editMode);
        }
        if (emailField != null) {
            emailField.editableProperty().bind(editMode);
        }
        if (telephoneField != null) {
            telephoneField.editableProperty().bind(editMode);
        }
        if (passwordField != null) {
            passwordField.editableProperty().bind(editMode);
        }

        // ComboBox localisation : désactivée hors mode édition
        if (localisationComboBox != null) {
            localisationComboBox.disableProperty().bind(editMode.not());
        }

        // Bouton auto-détection localisation : désactivé hors mode édition
        if (localisationButton != null) {
            localisationButton.disableProperty().bind(editMode.not());
        }

        // Bouton "Enregistrer" : visible et actif uniquement en mode édition
        if (saveButton != null) {
            saveButton.disableProperty().bind(editMode.not());
            saveButton.visibleProperty().bind(editMode);
            saveButton.managedProperty().bind(editMode); // ne prend pas de place quand invisible
        }

        // Bouton "Modifier" : actif hors mode édition, désactivé pendant l'édition
        // CORRECTION : était lié à editMode.not() → désactivé au démarrage (bug)
        // Maintenant lié à editMode → désactivé pendant l'édition (correct)
        if (editButton != null) {
            editButton.disableProperty().bind(editMode);
        }
    }

    private void setEditMode(boolean enabled) {
        editMode.set(enabled);
    }

    /**
     * Met à jour le label de feedback avec une couleur adaptée au contexte.
     *
     * @param message message à afficher
     * @param isError {@code true} pour rouge (erreur), {@code false} pour vert (succès/info)
     */
    private void setFeedback(String message, boolean isError) {
        if (feedbackLabel == null) return;
        feedbackLabel.setText(message);
        feedbackLabel.setStyle(isError
                ? "-fx-text-fill: #ffcbc7; -fx-font-weight: 600;"   // rouge clair
                : "-fx-text-fill: #7ef5ae; -fx-font-weight: 600;"); // vert clair
    }

    /**
     * Formats a stored phone number for sidebar display.
     * Input:  {@code +21654770867} or {@code 54770867} or {@code +987654321}
     * Output: {@code +216 54 770 867}
     */
    private static String formatPhoneDisplay(String phone) {
        if (phone == null || phone.isBlank()) return "Non renseigné";

        // Strip +216, leading +, and all spaces — keep only digits
        String cleaned = phone.trim()
                .replace("+216", "")
                .replaceAll("\\+", "")
                .replaceAll("\\s", "")
                .replaceAll("\\D", "");

        // Take the last 8 digits (handles any leftover country code digits)
        if (cleaned.length() > 8) {
            cleaned = cleaned.substring(cleaned.length() - 8);
        }
        if (cleaned.length() < 8) return "+216 " + cleaned;

        // Format: XX XXX XXX
        return "+216 "
                + cleaned.substring(0, 2) + " "
                + cleaned.substring(2, 5) + " "
                + cleaned.substring(5, 8);
    }

    /**
     * Builds a {@link javafx.scene.control.TextFormatter} for the phone field.
     * Allows only digits, max 8 characters — no re-entrant setText() calls.
     */
    private static javafx.scene.control.TextFormatter<String> buildPhoneFormatter() {
        return new javafx.scene.control.TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.matches("[0-9]{0,8}")) {
                return change;
            }
            return null;
        });
    }
}