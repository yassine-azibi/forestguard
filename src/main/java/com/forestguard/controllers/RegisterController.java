package com.forestguard.controllers;

import com.forestguard.entities.Utilisateur;
import com.forestguard.services.UtilisateurService;
import com.forestguard.utils.EmailService;
import com.forestguard.utils.GovernorateUtils;
import com.forestguard.utils.GoogleAuthService;
import com.forestguard.utils.GoogleAuthService.GoogleUser;
import com.forestguard.utils.LocationDetectionService;
import com.forestguard.utils.LocationDetectionService.LocationDetectionException;
import com.forestguard.utils.PhoneNumberUtils;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

public class RegisterController {

    // ── Champs FXML ───────────────────────────────────────────────────────────

    @FXML private TextField     nomField;
    @FXML private TextField     emailField;
    @FXML private TextField     telephoneField;
    @FXML private ComboBox<String> localisationComboBox;
    @FXML private Button        autoDetectButton;
    @FXML private Label         locationStatusLabel;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label         feedbackLabel;
    @FXML private ImageView     backgroundImage;
    @FXML private Button        googleRegisterButton;

    private final UtilisateurService utilisateurService = new UtilisateurService();

    /** Profil Google en attente de finalisation (téléphone + gouvernorat). */
    private GoogleUser pendingGoogleUser = null;

    // ── Initialisation ────────────────────────────────────────────────────────

    @FXML
    private void initialize() {
        if (localisationComboBox != null) {
            localisationComboBox.getItems().setAll(GovernorateUtils.getGovernorates());
            localisationComboBox.setEditable(true);
        }

        // TextFormatter: intercepts input before it's applied — no re-entrant setText() calls.
        // Allows only digits, max 8 characters. Formatting (XX XXX XXX) is applied on submit.
        if (telephoneField != null) {
            telephoneField.setTextFormatter(buildPhoneFormatter());
        }

        backgroundImage.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((wObs, oldW, newW) -> {
                    if (newW != null && !backgroundImage.fitWidthProperty().isBound()) {
                        backgroundImage.fitWidthProperty().bind(newW.widthProperty());
                        backgroundImage.fitHeightProperty().bind(newW.heightProperty());
                    }
                });
            }
        });

        Platform.runLater(() -> {
            if (backgroundImage.getScene() != null
                    && backgroundImage.getScene().getWindow() != null
                    && !backgroundImage.fitWidthProperty().isBound()) {
                backgroundImage.fitWidthProperty().bind(
                        backgroundImage.getScene().getWindow().widthProperty());
                backgroundImage.fitHeightProperty().bind(
                        backgroundImage.getScene().getWindow().heightProperty());
            }
        });
    }

    // ── Auto-détection localisation ───────────────────────────────────────────

    @FXML
    private void handleAutoDetectLocation() {
        if (localisationComboBox == null) return;

        animateDetectButton();
        autoDetectButton.setDisable(true);
        autoDetectButton.setText("⏳ Détection...");
        setLocationStatus("Détection en cours...", false);

        Thread t = new Thread(() -> {
            try {
                String gov = LocationDetectionService.detectGovernorate();
                Platform.runLater(() -> applyDetectedLocation(gov));
            } catch (LocationDetectionException e) {
                Platform.runLater(() -> handleDetectionError(e.getMessage()));
            } finally {
                Platform.runLater(() -> {
                    autoDetectButton.setDisable(false);
                    autoDetectButton.setText("📍 Auto-détecter");
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void applyDetectedLocation(String city) {
        String normalized = GovernorateUtils.normalize(city);
        if (normalized.isBlank()) { handleDetectionError("Gouvernorat non reconnu : " + city); return; }
        localisationComboBox.setValue(normalized);
        localisationComboBox.getSelectionModel().select(normalized);
        localisationComboBox.getEditor().setText(normalized);
        setLocationStatus("✅ Localisation détectée : " + normalized, false);
    }

    private void handleDetectionError(String reason) {
        localisationComboBox.getSelectionModel().clearSelection();
        localisationComboBox.setValue(null);
        localisationComboBox.getEditor().clear();
        setLocationStatus("❌ " + reason, true);
    }

    private void setLocationStatus(String message, boolean isError) {
        if (locationStatusLabel == null) return;
        locationStatusLabel.setText(message);
        locationStatusLabel.setStyle(isError ? "-fx-text-fill: #ffcbc7;" : "-fx-text-fill: #7ef5ae;");
    }

    private void animateDetectButton() {
        if (autoDetectButton == null) return;
        new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(autoDetectButton.scaleXProperty(), 1.0),
                        new KeyValue(autoDetectButton.scaleYProperty(), 1.0)),
                new KeyFrame(Duration.millis(100),
                        new KeyValue(autoDetectButton.scaleXProperty(), 0.93),
                        new KeyValue(autoDetectButton.scaleYProperty(), 0.93)),
                new KeyFrame(Duration.millis(200),
                        new KeyValue(autoDetectButton.scaleXProperty(), 1.0),
                        new KeyValue(autoDetectButton.scaleYProperty(), 1.0))
        ).play();
    }

    // ── Connexion Google ──────────────────────────────────────────────────────

    /**
     * Lance le flux OAuth2 Google depuis le formulaire d'inscription.
     * Pré-remplit nom + email et masque les champs mot de passe.
     */
    @FXML
    private void handleGoogleRegister() {
        if (googleRegisterButton != null) {
            googleRegisterButton.setDisable(true);
            googleRegisterButton.setText("⏳ Connexion...");
        }

        GoogleAuthService.authenticate(
                googleUser -> Platform.runLater(() -> {
                    if (googleRegisterButton != null) {
                        googleRegisterButton.setDisable(false);
                        googleRegisterButton.setText("Continuer avec Google");
                    }
                    prefillFromGoogle(googleUser);
                }),
                error -> Platform.runLater(() -> {
                    if (googleRegisterButton != null) {
                        googleRegisterButton.setDisable(false);
                        googleRegisterButton.setText("Continuer avec Google");
                    }
                    feedbackLabel.setStyle("-fx-text-fill: #ffcbc7;");
                    feedbackLabel.setText(error);
                })
        );
    }

    /**
     * Pré-remplit le formulaire avec les données Google.
     * Appelé depuis {@link LoginController} (nouvel utilisateur Google)
     * ou depuis {@link #handleGoogleRegister()}.
     *
     * @param googleUser profil Google reçu après authentification
     */
    public void prefillFromGoogle(GoogleUser googleUser) {
        if (googleUser == null) return;

        if (nomField != null && googleUser.name != null) {
            nomField.setText(googleUser.name);
            nomField.setEditable(false);   // fourni par Google
        }
        if (emailField != null && googleUser.email != null) {
            emailField.setText(googleUser.email);
            emailField.setEditable(false); // vérifié par Google
        }

        // Masquer les champs mot de passe — inutiles pour un compte Google
        if (passwordField != null) {
            passwordField.setManaged(false);
            passwordField.setVisible(false);
        }
        if (confirmPasswordField != null) {
            confirmPasswordField.setManaged(false);
            confirmPasswordField.setVisible(false);
        }

        this.pendingGoogleUser = googleUser;

        if (feedbackLabel != null) {
            feedbackLabel.setStyle("-fx-text-fill: #7ef5ae;");
            feedbackLabel.setText("✅ Connecté avec Google : " + googleUser.email
                    + "\nComplétez votre téléphone et gouvernorat pour finaliser.");
        }
    }

    // ── Inscription ───────────────────────────────────────────────────────────

    @FXML
    private void handleRegister() {
        if (pendingGoogleUser != null) {
            // Flux Google : seuls téléphone + gouvernorat sont requis
            handleRegisterWithGoogle();
        } else {
            // Flux classique
            handleRegisterClassic();
        }
    }

    /** Inscription classique avec email + mot de passe. */
    private void handleRegisterClassic() {
        String nom          = text(nomField);
        String email        = text(emailField);
        String telephone    = text(telephoneField);
        String localisation = localisationComboBox == null ? ""
                : GovernorateUtils.normalize(localisationComboBox.getEditor().getText());
        String password     = passwordField.getText()        == null ? "" : passwordField.getText();
        String confirm      = confirmPasswordField.getText() == null ? "" : confirmPasswordField.getText();

        if (nom.isBlank() || email.isBlank() || telephone.isBlank()
                || localisation.isBlank() || password.isBlank()) {
            feedbackLabel.setText("Tous les champs sont obligatoires.");
            return;
        }

        telephone = PhoneNumberUtils.normalizeToE164(telephone);
        if (telephone.isBlank()) { feedbackLabel.setText("Le numéro de téléphone est invalide."); return; }
        if (localisation.isBlank()) { feedbackLabel.setText("Veuillez sélectionner un gouvernorat valide."); return; }

        boolean emailExists = utilisateurService.emailExists(email);
        boolean phoneExists = utilisateurService.telephoneExists(telephone);
        if (emailExists || phoneExists) {
            StringBuilder sb = new StringBuilder();
            if (emailExists) sb.append("Cet email est déjà utilisé.");
            if (phoneExists) { if (sb.length() > 0) sb.append("\n"); sb.append("Ce numéro est déjà utilisé."); }
            feedbackLabel.setText(sb.toString());
            return;
        }

        if (!password.equals(confirm)) { feedbackLabel.setText("Les mots de passe ne correspondent pas."); return; }

        Utilisateur utilisateur = new Utilisateur(nom, email, telephone, localisation, password);
        try {
            utilisateurService.addEntity(utilisateur);
            EmailService.getInstance().sendWelcomeEmailAsync(utilisateur.getNom(), utilisateur.getEmail());
            showInfo("Inscription", "Compte créé avec succès.");
            goToLogin();
        } catch (IllegalArgumentException e) {
            feedbackLabel.setText(e.getMessage());
        } catch (IllegalStateException e) {
            showError("Erreur", e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** Inscription via Google : crée le compte avec google_id. */
    private void handleRegisterWithGoogle() {
        String telephone    = text(telephoneField);
        String localisation = localisationComboBox == null ? ""
                : GovernorateUtils.normalize(localisationComboBox.getEditor().getText());

        if (telephone.isBlank() || localisation.isBlank()) {
            feedbackLabel.setText("Veuillez renseigner votre téléphone et votre gouvernorat.");
            return;
        }

        telephone = PhoneNumberUtils.normalizeToE164(telephone);
        if (telephone.isBlank()) { feedbackLabel.setText("Le numéro de téléphone est invalide."); return; }
        if (localisation.isBlank()) { feedbackLabel.setText("Veuillez sélectionner un gouvernorat valide."); return; }

        if (utilisateurService.telephoneExists(telephone)) {
            feedbackLabel.setText("Ce numéro de téléphone est déjà utilisé.");
            return;
        }

        try {
            Utilisateur created = utilisateurService.addGoogleUser(
                    pendingGoogleUser, telephone, localisation);
            // ── Email avec mot de passe généré (carte jaune) ──────────────────
            EmailService.getInstance().sendWelcomeGoogleEmailAsync(
                    created.getNom(),
                    created.getEmail(),
                    created.getGeneratedPassword()
            );
            showInfo("Inscription", "Compte créé avec succès via Google.\n"
                    + "Un email avec votre mot de passe temporaire vous a été envoyé.");
            goToLogin();
        } catch (IllegalStateException e) {
            showError("Erreur", e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @FXML
    private void goToLogin() throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/com/forestguard/views/login.fxml"));
        Stage stage = (Stage) nomField.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle("Connexion");
        stage.setMaximized(true);
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    /**
     * Builds a {@link javafx.scene.control.TextFormatter} for the phone field.
     *
     * <p>Allows only digit characters, maximum 8 digits. Intercepts the change
     * before it is applied to the field — no re-entrant {@code setText()} calls,
     * which eliminates the {@code IllegalArgumentException: start must be <= end}
     * crash that occurs when a listener modifies text while the caret is mid-update.</p>
     */
    private static javafx.scene.control.TextFormatter<String> buildPhoneFormatter() {
        return new javafx.scene.control.TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            // Accept only pure digits, max 8 characters
            if (newText.matches("[0-9]{0,8}")) {
                return change;
            }
            // Reject the change — field stays unchanged, no exception
            return null;
        });
    }

    private static String text(TextField field) {
        return field == null || field.getText() == null ? "" : field.getText().trim();
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
}
