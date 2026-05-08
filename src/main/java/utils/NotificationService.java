package utils;

import dao.AlerteDAO;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.*;

/**
 * Service de notifications temps réel pour ForestGuard
 * Surveille la table alerte et affiche des popups pour les nouvelles alertes
 */
public class NotificationService {

    private Timeline timeline;
    private final AlerteDAO alerteDAO;
    private int dernierIdVu = 0;
    private final Set<Integer> alertesAffichees = new HashSet<>();

    public NotificationService() {
        this.alerteDAO = new AlerteDAO();
    }

    /**
     * Démarre le service de surveillance des alertes
     * Vérifie toutes les 5 secondes
     */
    public void demarrer() {
        if (timeline != null) {
            timeline.stop();
        }

        timeline = new Timeline(new KeyFrame(Duration.seconds(5), event -> {
            verifierAlertes();
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        // Vérification immédiate au démarrage
        verifierAlertes();
        
        System.out.println("✅ NotificationService démarré - vérification toutes les 5 secondes");
    }

    /**
     * Arrête le service de surveillance
     */
    public void arreter() {
        if (timeline != null) {
            timeline.stop();
            System.out.println("🛑 NotificationService arrêté");
        }
    }

    /**
     * Vérifie les nouvelles alertes et affiche les popups
     */
    private void verifierAlertes() {
        List<Map<String, Object>> nouvellesAlertes = alerteDAO.getNouvellesAlertes();
        
        if (nouvellesAlertes.isEmpty()) {
            return;
        }

        for (Map<String, Object> alerte : nouvellesAlertes) {
            int id = (int) alerte.get("id");
            
            // Ne pas réafficher une alerte déjà vue
            if (id <= dernierIdVu || alertesAffichees.contains(id)) {
                continue;
            }

            // Afficher la popup sur le thread JavaFX
            Platform.runLater(() -> afficherPopup(alerte));
            
            alertesAffichees.add(id);
            if (id > dernierIdVu) {
                dernierIdVu = id;
            }
        }
    }

    /**
     * Affiche une popup stylisée pour une alerte
     */
    private void afficherPopup(Map<String, Object> alerte) {
        Stage popup = new Stage();
        popup.initStyle(StageStyle.UNDECORATED);
        popup.setAlwaysOnTop(true);
        
        // 🔊 Démarrer le son en boucle
        SoundPlayer soundPlayer = new SoundPlayer();
        soundPlayer.demarrerSon();

        // Extraction des données
        String niveau = (String) alerte.get("niveau");
        String type = (String) alerte.get("type_alerte");
        String localisation = (String) alerte.get("localisation");

        // Couleur selon le niveau
        String couleur = getCouleurNiveau(niveau);
        String couleurTexte = getNiveauEstCritique(niveau) ? "#ffffff" : "#1e293b";

        // Emoji selon le type
        String emoji = getEmojiType(type);

        // Construction du contenu
        VBox container = new VBox(12);
        container.setAlignment(Pos.TOP_LEFT);
        container.setPadding(new Insets(20));
        container.setStyle(
            "-fx-background-color: " + couleur + ";" +
            "-fx-background-radius: 16;" +
            "-fx-border-color: rgba(255,255,255,0.3);" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 16;"
        );
        container.setPrefWidth(380);

        // Effet glow
        DropShadow glow = new DropShadow();
        glow.setColor(Color.web(couleur.replace("0.95", "0.6")));
        glow.setRadius(20);
        glow.setSpread(0.4);
        container.setEffect(glow);

        // Titre
        Label lblTitre = new Label("🚨 NOUVELLE ALERTE " + niveau.toUpperCase());
        lblTitre.setStyle(
            "-fx-font-size: 16;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: " + couleurTexte + ";"
        );

        // Type
        Label lblType = new Label(emoji + " " + type + " détecté(e)");
        lblType.setStyle(
            "-fx-font-size: 14;" +
            "-fx-font-weight: 600;" +
            "-fx-text-fill: " + couleurTexte + ";"
        );

        // Localisation
        Label lblLoc = new Label("📍 " + localisation);
        lblLoc.setStyle(
            "-fx-font-size: 13;" +
            "-fx-text-fill: " + couleurTexte + ";"
        );

        // Action
        Label lblAction = new Label("▶ Créer une intervention");
        lblAction.setStyle(
            "-fx-font-size: 12;" +
            "-fx-font-style: italic;" +
            "-fx-text-fill: " + couleurTexte + ";" +
            "-fx-opacity: 0.8;"
        );

        container.getChildren().addAll(lblTitre, lblType, lblLoc, lblAction);

        Scene scene = new Scene(container);
        scene.setFill(Color.TRANSPARENT);
        popup.setScene(scene);

        // Position : coin supérieur droit
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double startX = screenBounds.getMaxX();
        double finalX = screenBounds.getMaxX() - 400;
        double y = screenBounds.getMinY() + 20;

        popup.setX(startX);
        popup.setY(y);

        // Animation slide depuis la droite
        TranslateTransition slideIn = new TranslateTransition(Duration.millis(500), container);
        slideIn.setFromX(400);
        slideIn.setToX(0);
        slideIn.setInterpolator(Interpolator.EASE_OUT);

        // Animation pulse pour les alertes critiques
        if (getNiveauEstCritique(niveau)) {
            ScaleTransition pulse = new ScaleTransition(Duration.millis(800), container);
            pulse.setFromX(1.0);
            pulse.setFromY(1.0);
            pulse.setToX(1.05);
            pulse.setToY(1.05);
            pulse.setCycleCount(Timeline.INDEFINITE);
            pulse.setAutoReverse(true);
            pulse.play();
        }

        popup.show();
        popup.setX(finalX); // Repositionner après show()
        slideIn.play();

        // Auto-fermeture après 20 secondes
        Timeline autoClose = new Timeline(new KeyFrame(Duration.seconds(20), e -> {
            // 🔇 Arrêter le son
            soundPlayer.arreterSon();
            
            // Animation slide out
            TranslateTransition slideOut = new TranslateTransition(Duration.millis(400), container);
            slideOut.setToX(400);
            slideOut.setInterpolator(Interpolator.EASE_IN);
            slideOut.setOnFinished(ev -> popup.close());
            slideOut.play();
        }));
        autoClose.play();

        // Fermeture au clic
        container.setOnMouseClicked(e -> {
            // 🔇 Arrêter le son
            soundPlayer.arreterSon();
            
            TranslateTransition slideOut = new TranslateTransition(Duration.millis(300), container);
            slideOut.setToX(400);
            slideOut.setOnFinished(ev -> popup.close());
            slideOut.play();
        });

        System.out.println("🔔 Notification affichée : " + type + " - " + niveau + " - " + localisation);
    }

    /**
     * Retourne la couleur selon le niveau d'alerte
     */
    private String getCouleurNiveau(String niveau) {
        if (niveau == null) return "rgba(22, 163, 74, 0.95)"; // Vert par défaut
        
        switch (niveau.toLowerCase()) {
            case "critique":
                return "rgba(220, 38, 38, 0.95)"; // Rouge #dc2626
            case "haute":
            case "élevée":
            case "elevee":
                return "rgba(249, 115, 22, 0.95)"; // Orange #f97316
            case "moyenne":
                return "rgba(234, 179, 8, 0.95)"; // Jaune #eab308
            case "faible":
            case "basse":
                return "rgba(22, 163, 74, 0.95)"; // Vert #16a34a
            default:
                return "rgba(22, 163, 74, 0.95)"; // Vert par défaut
        }
    }

    /**
     * Vérifie si le niveau est critique (pour l'animation pulse)
     */
    private boolean getNiveauEstCritique(String niveau) {
        return niveau != null && niveau.equalsIgnoreCase("critique");
    }

    /**
     * Retourne l'emoji selon le type d'alerte
     */
    private String getEmojiType(String type) {
        if (type == null) return "⚠️";
        
        String typeLower = type.toLowerCase();
        if (typeLower.contains("incendie") || typeLower.contains("feu")) {
            return "🔥";
        } else if (typeLower.contains("fumée") || typeLower.contains("fumee")) {
            return "💨";
        } else if (typeLower.contains("chaleur") || typeLower.contains("température") || typeLower.contains("temperature")) {
            return "🌡️";
        } else if (typeLower.contains("vent")) {
            return "🌪️";
        } else if (typeLower.contains("sécheresse") || typeLower.contains("secheresse")) {
            return "☀️";
        } else {
            return "⚠️";
        }
    }
}
