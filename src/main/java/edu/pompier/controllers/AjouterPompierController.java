package edu.pompier.controllers;

import edu.pompier.entities.Pompier;
import edu.pompier.services.PompierService;
import edu.pompier.tools.EmailService;
import edu.pompier.tools.LocalisationData;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.util.List;

public class AjouterPompierController {

    @FXML private TextField tfNom, tfPrenom, tfEmail, tfTelephone;
    @FXML private PasswordField tfMotDePasse;
    @FXML private ComboBox<String> cbStatut, cbForet, cbNiveauCertif;

    // ── NOUVEAUX CHAMPS LOCALISATION ──
    @FXML private ComboBox<String> cbVille, cbZone;
    @FXML private Label lblCoordonnees;
    @FXML private Label lblStatutMail;

    // ── PHOTO ──
    @FXML private ImageView imgPreviewAjouter;
    @FXML private Label lblPhotoPath;
    private String photoPathSelectionne = null;

    // Coordonnées GPS calculées automatiquement
    private double latitudePompier  = 0;
    private double longitudePompier = 0;

    PompierService service = new PompierService();
    private List<String[]> foretsList;

    @FXML
    public void initialize() {
        cbStatut.setItems(FXCollections.observableArrayList("disponible", "en_mission", "inactif"));
        cbStatut.setValue("disponible");
        cbNiveauCertif.setItems(FXCollections.observableArrayList("debutant", "intermediaire", "avance", "expert"));
        chargerForets();

        // ── Charger les villes ──
        cbVille.setItems(FXCollections.observableArrayList(LocalisationData.getVilles()));

        // ── Quand la ville change → mettre à jour les zones ──
        cbVille.valueProperty().addListener((obs, ancien, nouveau) -> {
            cbZone.setItems(FXCollections.observableArrayList(
                    LocalisationData.getZones(nouveau)));
            cbZone.setValue(null);
            latitudePompier  = 0;
            longitudePompier = 0;
            lblCoordonnees.setText("Selectionnez une zone pour obtenir les coordonnees");
            lblCoordonnees.setStyle("-fx-text-fill: #86efac; -fx-font-size: 11;");
        });

        // ── Quand la zone change → calculer les coordonnées ──
        cbZone.valueProperty().addListener((obs, ancien, nouveau) -> {
            if (nouveau == null || cbVille.getValue() == null) return;
            double[] coords = LocalisationData.getCoordonnees(cbVille.getValue(), nouveau);
            if (coords != null) {
                latitudePompier  = coords[0];
                longitudePompier = coords[1];
                lblCoordonnees.setText(
                        "📍 " + cbVille.getValue() + " - " + nouveau +
                                "   |   Lat: " + coords[0] + "  Lon: " + coords[1]);
                lblCoordonnees.setStyle("-fx-text-fill: #4ade80; -fx-font-size: 11; -fx-font-weight: bold;");
            }
        });

        // ── Validation email en temps réel ──
        tfEmail.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isBlank()) {
                tfEmail.setStyle("-fx-background-color: #ffffff; -fx-text-fill: #111827; -fx-background-radius: 10; -fx-border-color: #d1d5db; -fx-border-radius: 10; -fx-padding: 10 13; -fx-font-size: 13;");
                if (lblStatutMail != null) lblStatutMail.setText("");
                return;
            }
            String email = newVal.trim();
            // Contrôle format : doit contenir @
            if (!email.contains("@") || email.indexOf("@") == 0 || email.indexOf("@") == email.length() - 1) {
                tfEmail.setStyle("-fx-background-color: #fff5f5; -fx-text-fill: #111827; -fx-background-radius: 10; -fx-border-color: #fca5a5; -fx-border-radius: 10; -fx-padding: 10 13; -fx-font-size: 13;");
                if (lblStatutMail != null) {
                    lblStatutMail.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 11;");
                    lblStatutMail.setText("\u26A0 Format invalide — l'email doit contenir @");
                }
                return;
            }
            // Contrôle unicité (seulement si format valide)
            boolean existe = service.emailExiste(email, 0);
            if (existe) {
                tfEmail.setStyle("-fx-background-color: #fff5f5; -fx-text-fill: #111827; -fx-background-radius: 10; -fx-border-color: #f87171; -fx-border-radius: 10; -fx-padding: 10 13; -fx-font-size: 13;");
                if (lblStatutMail != null) {
                    lblStatutMail.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 11; -fx-font-weight: bold;");
                    lblStatutMail.setText("\u274C Cet email est deja utilise par un autre pompier.");
                }
            } else {
                tfEmail.setStyle("-fx-background-color: #f0fdf4; -fx-text-fill: #111827; -fx-background-radius: 10; -fx-border-color: #86efac; -fx-border-radius: 10; -fx-padding: 10 13; -fx-font-size: 13;");
                if (lblStatutMail != null) {
                    lblStatutMail.setStyle("-fx-text-fill: #16a34a; -fx-font-size: 11;");
                    lblStatutMail.setText("\u2705 Email disponible");
                }
            }
        });

        // ── Validation téléphone en temps réel ──
        tfTelephone.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused && !tfTelephone.getText().isBlank()) {
                boolean existe = service.telephoneExiste(tfTelephone.getText().trim(), 0);
                tfTelephone.setStyle(existe
                        ? "-fx-border-color: #C62828; -fx-border-radius: 8; -fx-background-radius: 8;"
                        : "-fx-border-color: #16a34a; -fx-border-radius: 8; -fx-background-radius: 8;");
            }
        });
    }

    private void chargerForets() {
        foretsList = service.getForets();
        javafx.collections.ObservableList<String> noms = FXCollections.observableArrayList();
        for (String[] f : foretsList) noms.add(f[1]);
        cbForet.setItems(noms);
    }

    @FXML
    public void sauvegarder() {
        // ── Champs obligatoires ──
        if (tfNom.getText().trim().isEmpty() || tfPrenom.getText().trim().isEmpty()
                || tfEmail.getText().trim().isEmpty() || tfMotDePasse.getText().trim().isEmpty()
                || cbStatut.getValue() == null) {
            afficherErreur("Champs manquants",
                    "Veuillez remplir tous les champs obligatoires (nom, prenom, email, mot de passe, statut).");
            return;
        }

        // ── Localisation obligatoire ──
        if (cbVille.getValue() == null || cbZone.getValue() == null) {
            afficherErreur("Localisation manquante",
                    "Veuillez selectionner la ville et la zone du pompier.\n" +
                            "Cela est necessaire pour le calcul de distance lors des incendies.");
            return;
        }

        String email      = tfEmail.getText().trim();
        String telephone  = tfTelephone.getText().trim();
        String motDePasse = tfMotDePasse.getText().trim();
        String nomComplet = tfPrenom.getText().trim() + " " + tfNom.getText().trim();

        // ── Contrôle format email ──
        if (!email.contains("@") || email.indexOf("@") == 0 || email.indexOf("@") == email.length() - 1) {
            tfEmail.setStyle("-fx-background-color: #fff5f5; -fx-text-fill: #111827; -fx-background-radius: 10; -fx-border-color: #fca5a5; -fx-border-radius: 10; -fx-padding: 10 13; -fx-font-size: 13;");
            if (lblStatutMail != null) {
                lblStatutMail.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 11; -fx-font-weight: bold;");
                lblStatutMail.setText("\u26A0 Format invalide — l'email doit contenir @");
            }
            afficherErreur("Email invalide", "L'adresse email doit contenir le caractere @.");
            return;
        }

        // ── Contrôle unicité email ──
        if (service.emailExiste(email, 0)) {
            tfEmail.setStyle("-fx-border-color: #C62828; -fx-border-radius: 8; -fx-background-radius: 8;");
            afficherErreur("Email deja utilise",
                    "Un pompier avec l'adresse « " + email + " » existe deja.");
            return;
        }

        // ── Contrôle unicité téléphone ──
        if (!telephone.isEmpty() && service.telephoneExiste(telephone, 0)) {
            tfTelephone.setStyle("-fx-border-color: #C62828; -fx-border-radius: 8; -fx-background-radius: 8;");
            afficherErreur("Telephone deja utilise",
                    "Un pompier avec le numero « " + telephone + " » existe deja.");
            return;
        }

        // ── Zone forêt ──
        int zoneId = 0;
        if (cbForet.getValue() != null)
            for (String[] f : foretsList)
                if (f[1].equals(cbForet.getValue())) { zoneId = Integer.parseInt(f[0]); break; }

        // ── Création du pompier avec coordonnées GPS ──
        Pompier p = new Pompier(
                tfNom.getText().trim(), tfPrenom.getText().trim(),
                email, telephone, motDePasse, cbStatut.getValue(), zoneId
        );
        p.setVille(cbVille.getValue());
        p.setZoneAdresse(cbZone.getValue());
        p.setLatitude(latitudePompier);
        p.setLongitude(longitudePompier);
        p.setPhotoPath(photoPathSelectionne);

        int idPompier = service.addEntityAndGetId(p);

        if (cbNiveauCertif.getValue() != null && idPompier > 0)
            service.addCertification(idPompier, cbNiveauCertif.getValue());

        // ── Envoi de l'email de bienvenue ──
        if (lblStatutMail != null) {
            lblStatutMail.setStyle("-fx-text-fill: #2E7D32; -fx-font-size: 11;");
            lblStatutMail.setText("📧 Envoi de l'email de bienvenue en cours...");
        }

        EmailService.envoyerEmailBienvenue(email, nomComplet, motDePasse);

        Platform.runLater(() -> {
            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle("Succes");
            ok.setHeaderText(null);
            ok.setContentText(
                    "Pompier « " + nomComplet + " » ajoute avec succes !\n" +
                            "Localisation : " + cbVille.getValue() + " - " + cbZone.getValue() + "\n" +
                            "Un email de bienvenue a ete envoye a " + email + ".");
            ok.showAndWait();
            ((Stage) tfNom.getScene().getWindow()).close();
        });
    }

    private void afficherErreur(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    public void choisirPhoto() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une photo");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
        File fichier = fc.showOpenDialog(tfNom.getScene().getWindow());
        if (fichier != null) {
            photoPathSelectionne = fichier.getAbsolutePath();
            if (lblPhotoPath != null) {
                lblPhotoPath.setText(fichier.getName());
                lblPhotoPath.setStyle("-fx-text-fill: #16a34a; -fx-font-size: 10; -fx-font-weight: bold;");
            }
            if (imgPreviewAjouter != null) {
                try {
                    imgPreviewAjouter.setImage(new Image(fichier.toURI().toString()));
                } catch (Exception ignored) {}
            }
        }
    }

    @FXML
    public void fermerFenetre() {
        ((Stage) tfNom.getScene().getWindow()).close();
    }
}