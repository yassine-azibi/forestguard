package controller;

import utils.IAService;
import utils.MeteoService;
import utils.AdvancedIAService;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class PopupInterventionController implements Initializable {

    @FXML private Label lblZoneHeader;
    @FXML private Label lblScoreRisque;
    @FXML private Label lblNiveauRisque;
    @FXML private ImageView imgCarte;
    @FXML private Label lblTemperature;
    @FXML private Label lblHumidite;
    @FXML private Label lblVent;
    @FXML private Label lblDescMeteo;
    @FXML private Label lblRecommandation;
    @FXML private Label lblDuree;
    @FXML private VBox vboxEquipements;

    private final MeteoService meteoService = new MeteoService();

    // ✅ VOTRE CLÉ API GOOGLE MAPS
    private static final String GOOGLE_MAPS_API_KEY = "AIzaSyAxsKQeALiO9mrdODgfRh39mWLyYVNUZ3c";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        imgCarte.setStyle("-fx-background-color: #1a2e1e;");
    }

    public void setDonnees(model.Intervention inter, String niveau, String type) {
        this.lblZoneHeader.setText("Zone : " + inter.getAlertZone());
        
        System.out.println("🗺️ PopupIntervention - Zone reçue: '" + inter.getAlertZone() + "'");

        MeteoService.DonneesMeteo meteo = meteoService.getMeteo(inter.getAlertZone());
        
        System.out.println("📍 Coordonnées obtenues: lat=" + meteo.latitude + ", lon=" + meteo.longitude);

        // Afficher les données météo avec indication si c'est par défaut
        String suffixe = meteo.succes ? "" : " ⚠️";
        lblTemperature.setText(String.format("%.1f°C%s", meteo.temperature, suffixe));
        lblHumidite.setText(String.format("%.0f%%%s", meteo.humidite, suffixe));
        lblVent.setText(String.format("%.1f km/h%s", meteo.vent * 3.6, suffixe));
        lblDescMeteo.setText(meteo.description + (meteo.succes ? "" : " (données par défaut)"));

        // ═══════════════════════════════════════════════════════════════════════
        // UTILISER LE SYSTÈME D'IA AVANCÉ
        // ═══════════════════════════════════════════════════════════════════════
        
        System.out.println("🤖 Calcul de prédiction avancée avec analyse historique...");
        
        AdvancedIAService.PredictionAvancee prediction = 
            AdvancedIAService.calculerPredictionAvancee(
                inter.getAlertZone(),
                meteo.temperature,
                meteo.humidite,
                meteo.vent,
                niveau
            );

        // Afficher le score et le niveau
        lblScoreRisque.setText(String.valueOf(prediction.scoreRisque));
        lblNiveauRisque.setText(prediction.niveauRisque);
        lblNiveauRisque.setStyle(
                "-fx-text-fill: " + IAService.getCouleurRisque(prediction.scoreRisque) + ";");

        // Afficher les recommandations avancées
        StringBuilder recText = new StringBuilder();
        
        // Informations historiques
        if (prediction.historiqueZone.nombreIncendies > 0) {
            recText.append("📊 HISTORIQUE DE LA ZONE:\n");
            recText.append("• ").append(prediction.historiqueZone.nombreIncendies)
                   .append(" incendie(s) passé(s)\n");
            recText.append("• Risque historique: ").append(prediction.historiqueZone.risqueHistorique).append("\n");
            recText.append("• Mois le plus dangereux: ").append(prediction.historiqueZone.moisPlusDangereux).append("\n");
            recText.append("• Tendance: ").append(prediction.historiqueZone.tendanceRecente).append("\n\n");
        }

        // Probabilité de propagation
        recText.append("🔥 PROBABILITÉ DE PROPAGATION RAPIDE: ")
               .append(String.format("%.0f%%", prediction.probabilitePropagationRapide)).append("\n\n");

        // Facteurs de risque
        if (!prediction.facteursRisque.isEmpty()) {
            recText.append("⚠️ FACTEURS DE RISQUE IDENTIFIÉS:\n");
            for (String facteur : prediction.facteursRisque) {
                recText.append("• ").append(facteur).append("\n");
            }
            recText.append("\n");
        }

        // Recommandations
        recText.append("📋 RECOMMANDATIONS:\n");
        for (String rec : prediction.recommandations) {
            recText.append("• ").append(rec).append("\n");
        }

        lblRecommandation.setText(recText.toString());
        
        // Durée estimée avec historique
        String dureeText = String.format("%.1f à %.1f heures", 
            prediction.dureeEstimeeHeures * 0.8, 
            prediction.dureeEstimeeHeures * 1.2);
        
        if (prediction.historiqueZone.dureeMovenneHeures > 0) {
            dureeText += String.format(" (historique: %.1fh)", 
                prediction.historiqueZone.dureeMovenneHeures);
        }
        
        lblDuree.setText(dureeText);

        // Équipements suggérés (utiliser l'ancien système pour la compatibilité)
        List<String> equipements = IAService.suggererEquipements(
            prediction.scoreRisque, type, meteo.vent);
        vboxEquipements.getChildren().clear();
        for (String eq : equipements) {
            Label lbl = new Label("  " + eq);
            lbl.setStyle("-fx-font-size: 12; -fx-text-fill: #e2e8f0; -fx-padding: 2 0;");
            vboxEquipements.getChildren().add(lbl);
        }

        System.out.println("✅ Prédiction avancée calculée:");
        System.out.println("   Score: " + prediction.scoreRisque);
        System.out.println("   Probabilité propagation: " + String.format("%.0f%%", prediction.probabilitePropagationRapide));
        System.out.println("   Durée estimée: " + String.format("%.1fh", prediction.dureeEstimeeHeures));
        System.out.println("   Historique zone: " + prediction.historiqueZone.nombreIncendies + " incendie(s)");

        chargerCarteGoogle(meteo.latitude, meteo.longitude);
    }

    private void chargerCarteGoogle(double lat, double lon) {
        // Vérifier que les coordonnées sont valides
        if (lat == 0.0 && lon == 0.0) {
            System.out.println("⚠️ Coordonnées invalides (0,0) - Carte non chargée");
            afficherPlaceholder(lat, lon);
            return;
        }
        
        // URL Google Maps Static API
        String url = String.format(
                "https://maps.googleapis.com/maps/api/staticmap?" +
                        "center=%.6f,%.6f" +
                        "&zoom=14" +
                        "&size=520x400" +
                        "&maptype=roadmap" +
                        "&markers=color:red%%7C%.6f,%.6f" +
                        "&key=%s",
                lat, lon, lat, lon, GOOGLE_MAPS_API_KEY
        );

        System.out.println("🗺️ Chargement carte pour: " + lat + ", " + lon);

        try {
            // Charger l'image en arrière-plan avec gestion d'erreur
            Image image = new Image(url, 520, 400, false, true); // backgroundLoading = true
            
            // Listener pour détecter les erreurs de chargement
            image.errorProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    System.out.println("❌ Erreur chargement carte Google Maps");
                    if (image.getException() != null) {
                        System.out.println("   Détails: " + image.getException().getMessage());
                    }
                    javafx.application.Platform.runLater(() -> afficherPlaceholder(lat, lon));
                }
            });
            
            // Listener pour confirmer le chargement réussi
            image.progressProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() >= 1.0 && !image.isError()) {
                    System.out.println("✅ Carte Google Maps chargée avec succès !");
                }
            });
            
            imgCarte.setImage(image);
            
        } catch (Exception e) {
            System.out.println("❌ Exception chargement carte: " + e.getMessage());
            e.printStackTrace();
            afficherPlaceholder(lat, lon);
        }
    }

    private void afficherPlaceholder(double lat, double lon) {
        imgCarte.setVisible(false);

        Label lblPlaceholder = new Label(
                "Carte indisponible\n" +
                        "Lat: " + String.format("%.4f", lat) + "\n" +
                        "Lon: " + String.format("%.4f", lon) + "\n\n" +
                        "Cliquez pour ouvrir Google Maps"
        );
        lblPlaceholder.setStyle(
                "-fx-text-fill: #4ade80;" +
                        "-fx-font-size: 14;" +
                        "-fx-alignment: center;" +
                        "-fx-padding: 20;"
        );
        lblPlaceholder.setOnMouseClicked(e -> {
            String url = String.format("https://www.google.com/maps?q=%.6f,%.6f", lat, lon);
            try {
                java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
            } catch (Exception ex) {
                System.out.println("Impossible d'ouvrir le navigateur");
            }
        });

        if (imgCarte.getParent() instanceof VBox) {
            VBox parent = (VBox) imgCarte.getParent();
            parent.getChildren().add(lblPlaceholder);
        }
    }

    @FXML
    private void handleFermer() {
        imgCarte.getScene().getWindow().hide();
    }
}