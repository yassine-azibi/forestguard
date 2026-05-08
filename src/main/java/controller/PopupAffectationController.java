package controller;

import dao.AffectationDAO;
import model.Affectation;
import utils.SoundPlayer;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Contrôleur pour le popup d'affectation
 * Permet d'accepter ou refuser une affectation
 */
public class PopupAffectationController {

    @FXML private Label lblZone;
    @FXML private Label lblType;
    @FXML private Label lblNiveau;
    @FXML private Label lblDistance;
    @FXML private Button btnAccepter;
    @FXML private Button btnRefuser;

    private Affectation affectation;
    private final AffectationDAO affectationDAO = new AffectationDAO();
    
    // 🔊 Lecteur de son
    private SoundPlayer soundPlayer;

    /**
     * Définit l'affectation à afficher
     * @param affectation L'affectation reçue
     */
    public void setAffectation(Affectation affectation) {
        this.affectation = affectation;
        
        // 🔊 Démarrer le son en boucle
        soundPlayer = new SoundPlayer();
        soundPlayer.demarrerSon();
        
        if (affectation != null) {
            // Remplir les informations
            lblZone.setText(affectation.getLocalisationAlerte() != null 
                ? affectation.getLocalisationAlerte() 
                : "Zone non spécifiée");
            
            lblType.setText(affectation.getTypeAlerte() != null 
                ? affectation.getTypeAlerte() 
                : "Type inconnu");
            
            // Niveau avec couleur selon la gravité
            String niveau = affectation.getNiveauAlerte() != null 
                ? affectation.getNiveauAlerte() 
                : "Non défini";
            lblNiveau.setText(niveau);
            
            // Couleur selon le niveau
            String couleurNiveau = getCouleurNiveau(niveau);
            lblNiveau.setStyle("-fx-font-size: 14; -fx-font-weight: bold;" +
                              "-fx-text-fill: " + couleurNiveau + ";" +
                              "-fx-padding: 10 14;" +
                              "-fx-background-color: #1a2e1e;" +
                              "-fx-background-radius: 8;" +
                              "-fx-border-color: " + couleurNiveau + ";" +
                              "-fx-border-radius: 8;" +
                              "-fx-border-width: 1.5;");
            
            // Distance
            if (affectation.getDistanceKm() != null) {
                lblDistance.setText(String.format("%.1f km", affectation.getDistanceKm()));
            } else {
                lblDistance.setText("Distance non calculée");
            }
            
            System.out.println("📋 Affectation affichée: " + affectation);
        }
    }

    /**
     * Retourne la couleur selon le niveau d'alerte
     */
    private String getCouleurNiveau(String niveau) {
        if (niveau == null) return "#94a3b8";
        
        switch (niveau.toLowerCase()) {
            case "critique":
            case "critical":
                return "#dc2626"; // Rouge
            case "élevé":
            case "high":
            case "eleve":
                return "#f97316"; // Orange
            case "moyen":
            case "medium":
                return "#fbbf24"; // Jaune
            case "faible":
            case "low":
                return "#4ade80"; // Vert
            default:
                return "#94a3b8"; // Gris
        }
    }

    /**
     * Gère l'acceptation de l'affectation
     */
    @FXML
    private void handleAccepter() {
        if (affectation == null) {
            System.out.println("❌ Aucune affectation à accepter");
            return;
        }
        
        // 🔇 Arrêter le son
        if (soundPlayer != null) {
            soundPlayer.arreterSon();
        }
        
        System.out.println("✅ Affectation acceptée: " + affectation.getId());
        System.out.println("   Statut reste: 'en_cours'");
        
        // Fermer le popup
        fermerPopup();
        
        // Ouvrir AjouterIntervention avec la zone pré-remplie
        ouvrirAjouterIntervention(affectation.getLocalisationAlerte());
    }

    /**
     * Gère le refus de l'affectation
     */
    @FXML
    private void handleRefuser() {
        if (affectation == null) {
            System.out.println("❌ Aucune affectation à refuser");
            return;
        }
        
        // 🔇 Arrêter le son
        if (soundPlayer != null) {
            soundPlayer.arreterSon();
        }
        
        // Mettre à jour le statut à 'annule'
        boolean success = affectationDAO.refuserAffectation(affectation.getId());
        
        if (success) {
            System.out.println("❌ Affectation refusée: " + affectation.getId());
            System.out.println("   Statut changé à: 'annule'");
        } else {
            System.out.println("⚠️ Erreur lors du refus de l'affectation");
        }
        
        // Fermer le popup
        fermerPopup();
    }

    /**
     * Ferme le popup
     */
    private void fermerPopup() {
        // 🔇 S'assurer que le son est arrêté
        if (soundPlayer != null) {
            soundPlayer.arreterSon();
        }
        
        Stage stage = (Stage) lblZone.getScene().getWindow();
        stage.close();
    }

    /**
     * Ouvre AjouterIntervention avec la zone pré-remplie
     */
    private void ouvrirAjouterIntervention(String zone) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/AjouterIntervention.fxml"));
            Parent root = loader.load();
            
            // Pré-remplir la zone
            AjouterInterventionController ctrl = loader.getController();
            ctrl.preRemplirZone(zone);
            
            Stage stage = new Stage();
            try {
                stage.getIcons().add(new Image(getClass().getResourceAsStream("/image/foret-logo.png")));
            } catch (Exception e) {
                System.out.println("⚠️ Impossible de charger l'icône");
            }
            stage.setTitle("Nouvelle Intervention — Affectation");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
            
            System.out.println("📝 Fenêtre AjouterIntervention ouverte avec zone: " + zone);
            
        } catch (IOException e) {
            System.out.println("❌ Erreur ouverture AjouterIntervention: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
