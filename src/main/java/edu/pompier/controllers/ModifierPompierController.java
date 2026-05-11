package edu.pompier.controllers;

import edu.pompier.entities.Pompier;
import edu.pompier.services.PompierService;
import edu.pompier.tools.LocalisationData;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.util.List;

public class ModifierPompierController {

    @FXML private TextField tfNom, tfPrenom, tfEmail, tfTelephone;
    @FXML private ComboBox<String> cbStatut, cbForet, cbNiveauCertif;

    // ── NOUVEAUX CHAMPS LOCALISATION ──
    @FXML private ComboBox<String> cbVille, cbZone;
    @FXML private Label lblCoordonnees;

    // ── PHOTO ──
    @FXML private ImageView imgPreviewModifier;
    @FXML private Label lblPhotoPath;
    private String photoPathSelectionne = null;

    // Coordonnées GPS
    private double latitudePompier  = 0;
    private double longitudePompier = 0;

    // Flag pour éviter de recalculer les coords quand on préremplit depuis setPompier()
    private boolean chargementInitial = false;

    PompierService service = new PompierService();
    private Pompier pompierAModifier;
    private List<String[]> foretsList;

    @FXML
    public void initialize() {
        cbStatut.setItems(FXCollections.observableArrayList(
                "disponible", "en_mission", "inactif"));
        cbNiveauCertif.setItems(FXCollections.observableArrayList(
                "debutant", "intermediaire", "avance", "expert"));
        chargerForets();

        // ── Validation email en temps réel ──
        tfEmail.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isBlank()) {
                tfEmail.setStyle("-fx-background-color: #ffffff; -fx-text-fill: #111827; -fx-background-radius: 10; -fx-border-color: #d1d5db; -fx-border-radius: 10; -fx-padding: 10 13; -fx-font-size: 13;");
                return;
            }
            String email = newVal.trim();
            int excludeId = (pompierAModifier != null) ? pompierAModifier.getId() : 0;
            if (!email.contains("@") || email.indexOf("@") == 0 || email.indexOf("@") == email.length() - 1) {
                tfEmail.setStyle("-fx-background-color: #fff5f5; -fx-text-fill: #111827; -fx-background-radius: 10; -fx-border-color: #fca5a5; -fx-border-radius: 10; -fx-padding: 10 13; -fx-font-size: 13;");
            } else if (service.emailExiste(email, excludeId)) {
                tfEmail.setStyle("-fx-background-color: #fff5f5; -fx-text-fill: #111827; -fx-background-radius: 10; -fx-border-color: #f87171; -fx-border-radius: 10; -fx-padding: 10 13; -fx-font-size: 13;");
            } else {
                tfEmail.setStyle("-fx-background-color: #f0fdf4; -fx-text-fill: #111827; -fx-background-radius: 10; -fx-border-color: #86efac; -fx-border-radius: 10; -fx-padding: 10 13; -fx-font-size: 13;");
            }
        });

        // ── Charger les villes ──
        cbVille.setItems(FXCollections.observableArrayList(LocalisationData.getVilles()));

        // ── Quand la ville change → mettre à jour les zones ──
        cbVille.valueProperty().addListener((obs, ancien, nouveau) -> {
            if (nouveau == null) return;
            cbZone.setItems(FXCollections.observableArrayList(
                    LocalisationData.getZones(nouveau)));
            // Ne pas réinitialiser la zone si on est en chargement initial
            if (!chargementInitial) {
                cbZone.setValue(null);
                latitudePompier  = 0;
                longitudePompier = 0;
                lblCoordonnees.setText("Selectionnez une zone");
                lblCoordonnees.setStyle("-fx-text-fill: #86efac; -fx-font-size: 11;");
            }
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
    }

    private void chargerForets() {
        foretsList = service.getForets();
        javafx.collections.ObservableList<String> noms = FXCollections.observableArrayList();
        for (String[] f : foretsList) noms.add(f[1]);
        cbForet.setItems(noms);
    }

    /**
     * Appelé depuis PompierController pour pré-remplir le formulaire.
     */
    public void setPompier(Pompier p) {
        this.pompierAModifier = p;
        tfNom.setText(p.getNom());
        tfPrenom.setText(p.getPrenom());
        tfEmail.setText(p.getEmail());
        tfTelephone.setText(p.getTelephone());
        cbStatut.setValue(p.getStatut());

        // Foret
        if (p.getNomForet() != null && !p.getNomForet().isEmpty()) {
            cbForet.setValue(p.getNomForet());
        } else if (p.getZoneId() > 0) {
            for (String[] f : foretsList)
                if (Integer.parseInt(f[0]) == p.getZoneId()) { cbForet.setValue(f[1]); break; }
        }

        // Certification
        if (p.getNiveauCertification() != null && !p.getNiveauCertification().isEmpty()) {
            cbNiveauCertif.setValue(p.getNiveauCertification());
        } else {
            String[] certif = service.getCertificationPompier(p.getId());
            if (certif != null) cbNiveauCertif.setValue(certif[0]);
        }

        // ── Pré-remplir localisation ──
        chargementInitial = true;

        if (p.getVille() != null && !p.getVille().isEmpty()) {            // Pompier a déjà une ville/zone enregistrée
            cbVille.setValue(p.getVille());
            // Les zones se rechargent via le listener, on peut maintenant setter la zone
            cbZone.setValue(p.getZoneAdresse());
            latitudePompier  = p.getLatitude();
            longitudePompier = p.getLongitude();

            if (latitudePompier != 0) {
                lblCoordonnees.setText(
                        "📍 " + p.getVille() + " - " + p.getZoneAdresse() +
                                "   |   Lat: " + latitudePompier + "  Lon: " + longitudePompier);
                lblCoordonnees.setStyle("-fx-text-fill: #4ade80; -fx-font-size: 11; -fx-font-weight: bold;");
            }
        } else {
            // Ancien pompier sans localisation
            lblCoordonnees.setText("⚠ Aucune localisation enregistree - veuillez en ajouter une");
            lblCoordonnees.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 11;");
        }

        chargementInitial = false;

        // ── Pré-remplir photo ──
        photoPathSelectionne = p.getPhotoPath();
        if (p.getPhotoPath() != null && !p.getPhotoPath().isBlank()) {
            if (lblPhotoPath != null) {
                lblPhotoPath.setText(new File(p.getPhotoPath()).getName());
                lblPhotoPath.setStyle("-fx-text-fill: #16a34a; -fx-font-size: 10; -fx-font-weight: bold;");
            }
            if (imgPreviewModifier != null) {
                try {
                    imgPreviewModifier.setImage(new Image(new File(p.getPhotoPath()).toURI().toString()));
                } catch (Exception ignored) {}
            }
        }
    }

    @FXML
    public void modifier() {
        if (tfNom.getText().trim().isEmpty() ||
                tfPrenom.getText().trim().isEmpty() ||
                tfEmail.getText().trim().isEmpty() ||
                cbStatut.getValue() == null) {
            new Alert(Alert.AlertType.WARNING,
                    "Veuillez remplir tous les champs obligatoires.").show();
            return;
        }

        // ── Contrôle format email ──
        String emailSaisi = tfEmail.getText().trim();
        if (!emailSaisi.contains("@") || emailSaisi.indexOf("@") == 0 || emailSaisi.indexOf("@") == emailSaisi.length() - 1) {
            tfEmail.setStyle("-fx-background-color: #fff5f5; -fx-text-fill: #111827; -fx-background-radius: 10; -fx-border-color: #fca5a5; -fx-border-radius: 10; -fx-padding: 10 13; -fx-font-size: 13;");
            new Alert(Alert.AlertType.WARNING, "L'adresse email doit contenir le caractere @.").show();
            return;
        }

        // ── Contrôle unicité email ──
        if (service.emailExiste(emailSaisi, pompierAModifier.getId())) {
            tfEmail.setStyle("-fx-background-color: #fff5f5; -fx-text-fill: #111827; -fx-background-radius: 10; -fx-border-color: #f87171; -fx-border-radius: 10; -fx-padding: 10 13; -fx-font-size: 13;");
            new Alert(Alert.AlertType.WARNING, "Cet email est deja utilise par un autre pompier.").show();
            return;
        }

        // ── Localisation obligatoire ──
        if (cbVille.getValue() == null || cbZone.getValue() == null) {
            new Alert(Alert.AlertType.WARNING,
                    "Veuillez selectionner la ville et la zone du pompier.\n" +
                            "Cela est necessaire pour le calcul de distance lors des incendies.").show();
            return;
        }

        int zoneId = 0;
        if (cbForet.getValue() != null)
            for (String[] f : foretsList)
                if (f[1].equals(cbForet.getValue())) { zoneId = Integer.parseInt(f[0]); break; }

        pompierAModifier.setNom(tfNom.getText().trim());
        pompierAModifier.setPrenom(tfPrenom.getText().trim());
        pompierAModifier.setEmail(tfEmail.getText().trim());
        pompierAModifier.setTelephone(tfTelephone.getText().trim());
        pompierAModifier.setStatut(cbStatut.getValue());
        pompierAModifier.setZoneId(zoneId);

        // ── Mise à jour localisation ──
        pompierAModifier.setVille(cbVille.getValue());
        pompierAModifier.setZoneAdresse(cbZone.getValue());
        pompierAModifier.setLatitude(latitudePompier);
        pompierAModifier.setLongitude(longitudePompier);
        pompierAModifier.setPhotoPath(photoPathSelectionne);

        service.updateEntity(pompierAModifier.getId(), pompierAModifier);

        if (cbNiveauCertif.getValue() != null && !cbNiveauCertif.getValue().isEmpty()) {
            service.updateCertification(pompierAModifier.getId(), cbNiveauCertif.getValue());
            pompierAModifier.setNiveauCertification(cbNiveauCertif.getValue());
        }

        new Alert(Alert.AlertType.INFORMATION, "Pompier modifie avec succes !").show();
        ((Stage) tfNom.getScene().getWindow()).close();
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
            if (imgPreviewModifier != null) {
                try {
                    imgPreviewModifier.setImage(new Image(fichier.toURI().toString()));
                } catch (Exception ignored) {}
            }
        }
    }

    @FXML
    public void fermerFenetre() {
        ((Stage) tfNom.getScene().getWindow()).close();
    }
}