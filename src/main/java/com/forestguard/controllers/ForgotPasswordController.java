package com.forestguard.controllers;

import com.forestguard.services.UtilisateurService;
import com.forestguard.utils.EmailService;
import com.forestguard.utils.PasswordHasher;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Random;

public class ForgotPasswordController {
    @FXML
    private TextField emailField;

    @FXML
    private TextField codeField;

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label feedbackLabel;

    @FXML
    private ImageView backgroundImage;

    private final UtilisateurService utilisateurService = new UtilisateurService();
    private final EmailService emailService = EmailService.getInstance();

    private String currentEmail;
    private String currentCode;

    @FXML
    private void initialize() {
        Platform.runLater(() -> {
            if (backgroundImage != null && emailField.getScene() != null) {
                backgroundImage.fitWidthProperty().bind(emailField.getScene().getWindow().widthProperty());
                backgroundImage.fitHeightProperty().bind(emailField.getScene().getWindow().heightProperty());
            }
        });
    }

    @FXML
    private void handleSendCode() {
        String email = emailField.getText() == null ? "" : emailField.getText().trim();

        if (email.isBlank()) {
            feedbackLabel.setText("Veuillez entrer votre email.");
            return;
        }

        if (!utilisateurService.emailExists(email)) {
            feedbackLabel.setText("Email introuvable.");
            return;
        }

        // generate 6-digit code
        String code = String.format("%06d", new Random().nextInt(1_000_000));
        currentEmail = email;
        currentCode = code;

        String subject = "ForestGuard - Réinitialisation du mot de passe";
        String body = "Votre code de réinitialisation est : " + code;

        emailService.sendEmailAsync(email, subject, body);

        feedbackLabel.setText("Code envoyé. Vérifiez votre boîte mail.");
    }

    @FXML
    private void handleResetPassword() {
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String code = codeField.getText() == null ? "" : codeField.getText().trim();
        String pw = newPasswordField.getText() == null ? "" : newPasswordField.getText();
        String confirm = confirmPasswordField.getText() == null ? "" : confirmPasswordField.getText();

        if (email.isBlank() || code.isBlank() || pw.isBlank() || confirm.isBlank()) {
            feedbackLabel.setText("Veuillez remplir tous les champs.");
            return;
        }

        if (currentEmail == null || !currentEmail.equals(email) || currentCode == null) {
            feedbackLabel.setText("Veuillez d'abord demander un code pour cet email.");
            return;
        }

        if (!currentCode.equals(code)) {
            feedbackLabel.setText("Code invalide.");
            return;
        }

        if (!pw.equals(confirm)) {
            feedbackLabel.setText("Les mots de passe ne correspondent pas.");
            return;
        }

        // Update password
        utilisateurService.updatePasswordByEmail(email, pw);

        feedbackLabel.setText("Mot de passe réinitialisé. Vous pouvez vous connecter.");
    }

    @FXML
    private void goToLogin() throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/com/forestguard/views/login.fxml"));
        Stage stage = (Stage) emailField.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle("Connexion");
        stage.setMaximized(true);
    }
}
