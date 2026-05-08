package controller;

import dao.AffectationDAO;
import model.Affectation;
import utils.SoundPlayer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Contrôleur pour la liste des affectations
 */
public class ListeAffectationsController implements Initializable {

    @FXML private ListView<Affectation> listeAffectations;
    @FXML private Label lblCompteur;

    private final AffectationDAO affectationDAO = new AffectationDAO();
    private final ObservableList<Affectation> data = FXCollections.observableArrayList();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    // ID du pompier (à adapter selon votre système)
    private final int idPompier = 1; // TODO: Récupérer depuis la session

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupListView();
        chargerAffectations();
    }

    /**
     * Configure la ListView avec un rendu personnalisé
     */
    private void setupListView() {
        listeAffectations.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Affectation affectation, boolean empty) {
                super.updateItem(affectation, empty);
                
                if (empty || affectation == null) {
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent; -fx-padding: 0;");
                    return;
                }

                // Container principal
                VBox container = new VBox(12);
                container.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.95);" +
                    "-fx-background-radius: 12;" +
                    "-fx-padding: 16;" +
                    "-fx-border-color: #e2e8f0;" +
                    "-fx-border-radius: 12;" +
                    "-fx-border-width: 1;" +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0, 0, 2);"
                );

                // Header avec zone et niveau
                HBox header = new HBox(12);
                header.setAlignment(Pos.CENTER_LEFT);

                Label lblZone = new Label("📍 " + affectation.getLocalisationAlerte());
                lblZone.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

                Label lblNiveau = new Label(affectation.getNiveauAlerte());
                lblNiveau.setStyle(
                    "-fx-background-color: " + getCouleurNiveau(affectation.getNiveauAlerte()) + ";" +
                    "-fx-text-fill: white;" +
                    "-fx-font-size: 10;" +
                    "-fx-font-weight: bold;" +
                    "-fx-background-radius: 10;" +
                    "-fx-padding: 4 10;"
                );

                header.getChildren().addAll(lblZone, lblNiveau);

                // Infos
                HBox infos = new HBox(20);
                infos.setAlignment(Pos.CENTER_LEFT);

                Label lblType = new Label("Type: " + affectation.getTypeAlerte());
                lblType.setStyle("-fx-font-size: 12; -fx-text-fill: #475569;");

                Label lblDistance = new Label("Distance: " + 
                    (affectation.getDistanceKm() != null ? 
                        String.format("%.1f km", affectation.getDistanceKm()) : "N/A"));
                lblDistance.setStyle("-fx-font-size: 12; -fx-text-fill: #475569;");

                Label lblDate = new Label("Reçue: " + 
                    (affectation.getDateAffectation() != null ? 
                        affectation.getDateAffectation().format(FMT) : "N/A"));
                lblDate.setStyle("-fx-font-size: 11; -fx-text-fill: #64748b; -fx-font-style: italic;");

                infos.getChildren().addAll(lblType, lblDistance, lblDate);

                // Boutons d'action
                HBox actions = new HBox(10);
                actions.setAlignment(Pos.CENTER_RIGHT);

                Button btnAccepter = new Button("✅ ACCEPTER");
                btnAccepter.setStyle(
                    "-fx-background-color: #16a34a;" +
                    "-fx-text-fill: white;" +
                    "-fx-font-size: 12;" +
                    "-fx-font-weight: bold;" +
                    "-fx-background-radius: 8;" +
                    "-fx-padding: 8 16;" +
                    "-fx-cursor: hand;"
                );
                btnAccepter.setOnMouseEntered(e -> btnAccepter.setStyle(
                    "-fx-background-color: #15803d;" +
                    "-fx-text-fill: white;" +
                    "-fx-font-size: 12;" +
                    "-fx-font-weight: bold;" +
                    "-fx-background-radius: 8;" +
                    "-fx-padding: 8 16;" +
                    "-fx-cursor: hand;"
                ));
                btnAccepter.setOnMouseExited(e -> btnAccepter.setStyle(
                    "-fx-background-color: #16a34a;" +
                    "-fx-text-fill: white;" +
                    "-fx-font-size: 12;" +
                    "-fx-font-weight: bold;" +
                    "-fx-background-radius: 8;" +
                    "-fx-padding: 8 16;" +
                    "-fx-cursor: hand;"
                ));
                btnAccepter.setOnAction(e -> accepterAffectation(affectation));

                Button btnRefuser = new Button("❌ REFUSER");
                btnRefuser.setStyle(
                    "-fx-background-color: #dc2626;" +
                    "-fx-text-fill: white;" +
                    "-fx-font-size: 12;" +
                    "-fx-font-weight: bold;" +
                    "-fx-background-radius: 8;" +
                    "-fx-padding: 8 16;" +
                    "-fx-cursor: hand;"
                );
                btnRefuser.setOnMouseEntered(e -> btnRefuser.setStyle(
                    "-fx-background-color: #b91c1c;" +
                    "-fx-text-fill: white;" +
                    "-fx-font-size: 12;" +
                    "-fx-font-weight: bold;" +
                    "-fx-background-radius: 8;" +
                    "-fx-padding: 8 16;" +
                    "-fx-cursor: hand;"
                ));
                btnRefuser.setOnMouseExited(e -> btnRefuser.setStyle(
                    "-fx-background-color: #dc2626;" +
                    "-fx-text-fill: white;" +
                    "-fx-font-size: 12;" +
                    "-fx-font-weight: bold;" +
                    "-fx-background-radius: 8;" +
                    "-fx-padding: 8 16;" +
                    "-fx-cursor: hand;"
                ));
                btnRefuser.setOnAction(e -> refuserAffectation(affectation));

                actions.getChildren().addAll(btnAccepter, btnRefuser);

                container.getChildren().addAll(header, infos, actions);

                setGraphic(container);
                setStyle("-fx-background-color: transparent; -fx-padding: 0 0 12 0;");
            }
        });

        listeAffectations.setItems(data);
    }

    /**
     * Charge les affectations en cours
     */
    private void chargerAffectations() {
        List<Affectation> affectations = affectationDAO.getAffectationsEnCours(idPompier);
        data.setAll(affectations);
        
        // Mettre à jour le compteur
        lblCompteur.setText(affectations.size() + " en attente");
        
        System.out.println("📋 " + affectations.size() + " affectation(s) chargée(s)");
    }

    /**
     * Accepte une affectation
     */
    private void accepterAffectation(Affectation affectation) {
        System.out.println("✅ Affectation acceptée: " + affectation.getId());
        
        // Retirer de la liste
        data.remove(affectation);
        lblCompteur.setText(data.size() + " en attente");
        
        // Fermer la fenêtre
        handleFermer();
        
        // Ouvrir AjouterIntervention avec la zone pré-remplie
        ouvrirAjouterIntervention(affectation.getLocalisationAlerte());
    }

    /**
     * Refuse une affectation
     */
    private void refuserAffectation(Affectation affectation) {
        // Confirmation
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer le refus");
        confirm.setHeaderText("Refuser cette affectation ?");
        confirm.setContentText("Zone: " + affectation.getLocalisationAlerte());
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Mettre à jour le statut dans la DB
                boolean success = affectationDAO.refuserAffectation(affectation.getId());
                
                if (success) {
                    System.out.println("❌ Affectation refusée: " + affectation.getId());
                    
                    // Retirer de la liste
                    data.remove(affectation);
                    lblCompteur.setText(data.size() + " en attente");
                    
                    // Message de confirmation
                    Alert info = new Alert(Alert.AlertType.INFORMATION);
                    info.setTitle("Affectation refusée");
                    info.setHeaderText(null);
                    info.setContentText("L'affectation a été refusée avec succès.");
                    info.showAndWait();
                } else {
                    Alert error = new Alert(Alert.AlertType.ERROR);
                    error.setTitle("Erreur");
                    error.setHeaderText("Erreur lors du refus");
                    error.setContentText("Impossible de refuser l'affectation.");
                    error.showAndWait();
                }
            }
        });
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
     * Ferme la fenêtre
     */
    @FXML
    private void handleFermer() {
        Stage stage = (Stage) listeAffectations.getScene().getWindow();
        stage.close();
    }
}
