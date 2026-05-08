package controller;

import dao.InterventionDAO;
import model.Affectation;
import model.Intervention;
import utils.AffectationService;
import utils.NotificationService;
import utils.PDFGenerator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class DashboardAgentController implements Initializable {

    @FXML private Label lblAgentName;
    @FXML private Label lblInProgress, lblCompleted, lblWeek;
    @FXML private ListView<Intervention> listeInterventions;
    @FXML private VBox cardEnCours, cardTerminees, cardSemaine;
    @FXML private Button btnGenererPDF; // Bouton génération PDF
    @FXML private Button btnAffectations; // Bouton Mes Affectations
    @FXML private Label badgeAffectations; // Badge compteur affectations

    private final InterventionDAO dao = new InterventionDAO();
    private final ObservableList<Intervention> data = FXCollections.observableArrayList();
    private final String currentAgent = "John Martinez";
    private final int    currentPompier = 1;
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    // Service de notifications temps réel
    private NotificationService notificationService;
    
    // Service d'affectations temps réel
    private AffectationService affectationService;
    
    // ID du pompier (à adapter selon votre système d'authentification)
    private final int idPompier = 1; // TODO: Récupérer depuis la session/authentification
    
    // Popup d'affectation actuellement ouvert (pour éviter les doublons)
    private Stage popupAffectationStage = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (lblAgentName != null) lblAgentName.setText(currentAgent);
        setupListView();
        setupCardHover();
        loadData();
        
        // Listener pour activer/désactiver le bouton PDF selon la sélection
        listeInterventions.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> {
                if (btnGenererPDF != null) {
                    btnGenererPDF.setDisable(newVal == null);
                    btnGenererPDF.setOpacity(newVal == null ? 0.5 : 1.0);
                }
            }
        );
        
        // Démarrer les services après le chargement de la scène
        javafx.application.Platform.runLater(() -> {
            if (listeInterventions.getScene() != null) {
                // Service de notifications
                notificationService = new NotificationService();
                notificationService.demarrer();
                System.out.println("🔔 Service de notifications démarré pour " + currentAgent);
                
                // Service d'affectations
                affectationService = new AffectationService(idPompier);
                affectationService.setOnNouvelleAffectation(this::afficherPopupAffectation);
                affectationService.demarrer();
                System.out.println("📋 Service d'affectations démarré pour pompier ID: " + idPompier);
                
                // Mettre à jour le badge des affectations
                mettreAJourBadgeAffectations();
                
                // Mettre à jour le badge toutes les 30 secondes
                javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(javafx.util.Duration.seconds(30), e -> mettreAJourBadgeAffectations())
                );
                timeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
                timeline.play();
            }
        });
    }

    private void setupCardHover() {
        VBox[] cards = {cardEnCours, cardTerminees, cardSemaine};
        for (VBox card : cards) {
            if (card == null) continue;
            String base = card.getStyle();
            card.setOnMouseEntered(e -> card.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.18);" +
                            "-fx-background-radius: 20;" +
                            "-fx-border-color: rgba(255,255,255,0.35);" +
                            "-fx-border-radius: 20; -fx-border-width: 1;" +
                            "-fx-padding: 20 24; -fx-cursor: hand;" +
                            "-fx-scale-x: 1.04; -fx-scale-y: 1.04;" +
                            "-fx-effect: dropshadow(gaussian,rgba(22,163,74,0.3),16,0,0,4);"));
            card.setOnMouseExited(e -> card.setStyle(base));
        }
    }

    private void setupListView() {
        listeInterventions.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Intervention inter, boolean empty) {
                super.updateItem(inter, empty);
                if (empty || inter == null) {
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent; -fx-padding: 0;");
                    return;
                }

                // Zone d'alerte
                Label lblZone = new Label(inter.getAlertZone());
                lblZone.setStyle("-fx-font-size: 13; -fx-text-fill: #1e293b;" +
                        "-fx-font-weight: bold;");
                lblZone.setPrefWidth(200);

                // Badge statut
                boolean inProgress = "In Progress".equals(inter.getStatut());
                Label badgeStatut = new Label(inProgress ? "En Cours" : "Terminée");
                badgeStatut.setStyle(
                        "-fx-background-color: " + (inProgress ? "#fff7ed" : "#f0fdf4") + ";" +
                                "-fx-text-fill: "         + (inProgress ? "#f97316" : "#16a34a") + ";" +
                                "-fx-font-size: 11; -fx-font-weight: bold;" +
                                "-fx-background-radius: 20; -fx-padding: 4 10;");
                badgeStatut.setPrefWidth(120);

                // Dates
                Label lblStart = new Label(inter.getStartDate() != null
                        ? inter.getStartDate().format(FMT) : "-");
                lblStart.setStyle("-fx-font-size: 12; -fx-text-fill: #64748b;");
                lblStart.setPrefWidth(150);

                Label lblEnd = new Label(inter.getEndDate() != null
                        ? inter.getEndDate().format(FMT) : "-");
                lblEnd.setStyle("-fx-font-size: 12; -fx-text-fill: #64748b;");
                lblEnd.setPrefWidth(150);

                // Résultat
                Label lblResultat = new Label(inter.getResultat() != null
                        ? inter.getResultat() : "-");
                lblResultat.setStyle("-fx-font-size: 12; -fx-text-fill: #475569;");
                lblResultat.setPrefWidth(200);
                lblResultat.setWrapText(true);

                // Bouton Modifier
                Button btnModifier = new Button("✏");
                btnModifier.setStyle(
                        "-fx-background-color: #dbeafe; -fx-text-fill: #2563eb;" +
                                "-fx-background-radius: 6; -fx-font-size: 11;" +
                                "-fx-cursor: hand; -fx-min-width: 30; -fx-min-height: 28;");
                btnModifier.setOnMouseEntered(e -> btnModifier.setStyle(
                        "-fx-background-color: #bfdbfe; -fx-text-fill: #1d4ed8;" +
                                "-fx-background-radius: 6; -fx-font-size: 11;" +
                                "-fx-cursor: hand; -fx-min-width: 30; -fx-min-height: 28;"));
                btnModifier.setOnMouseExited(e -> btnModifier.setStyle(
                        "-fx-background-color: #dbeafe; -fx-text-fill: #2563eb;" +
                                "-fx-background-radius: 6; -fx-font-size: 11;" +
                                "-fx-cursor: hand; -fx-min-width: 30; -fx-min-height: 28;"));
                btnModifier.setOnAction(e -> ouvrirModifier(inter));

                // Bouton Supprimer
                Button btnX = new Button("🗑");
                btnX.setStyle(
                        "-fx-background-color: #fee2e2; -fx-text-fill: #dc2626;" +
                                "-fx-background-radius: 6; -fx-font-size: 11;" +
                                "-fx-cursor: hand; -fx-min-width: 30; -fx-min-height: 28;");
                btnX.setOnMouseEntered(e -> btnX.setStyle(
                        "-fx-background-color: #fecaca; -fx-text-fill: #b91c1c;" +
                                "-fx-background-radius: 6; -fx-font-size: 11;" +
                                "-fx-cursor: hand; -fx-min-width: 30; -fx-min-height: 28;"));
                btnX.setOnMouseExited(e -> btnX.setStyle(
                        "-fx-background-color: #fee2e2; -fx-text-fill: #dc2626;" +
                                "-fx-background-radius: 6; -fx-font-size: 11;" +
                                "-fx-cursor: hand; -fx-min-width: 30; -fx-min-height: 28;"));
                btnX.setOnAction(e -> supprimerIntervention(inter));

                HBox actions = new HBox(6, btnModifier, btnX);
                actions.setAlignment(Pos.CENTER_LEFT);
                actions.setPrefWidth(120);

                HBox row = new HBox(0,
                        lblZone, badgeStatut, lblStart, lblEnd, lblResultat, actions);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-padding: 13 16;");

                // Fond alterné
                String bgNormal = getIndex() % 2 == 0
                        ? "rgba(255,255,255,0.95)"
                        : "rgba(248,250,252,0.95)";
                setStyle("-fx-background-color: " + bgNormal + ";" +
                        "-fx-border-color: #f1f5f9;" +
                        "-fx-border-width: 0 0 1 0;");
                setOnMouseEntered(e -> setStyle(
                        "-fx-background-color: #f0fdf4;" +
                                "-fx-border-color: #bbf7d0;" +
                                "-fx-border-width: 0 0 1 0;"));
                setOnMouseExited(e -> setStyle(
                        "-fx-background-color: " + bgNormal + ";" +
                                "-fx-border-color: #f1f5f9;" +
                                "-fx-border-width: 0 0 1 0;"));

                setGraphic(row);
            }
        });

        listeInterventions.setItems(data);
    }

    private void loadData() {
        data.setAll(dao.getByAgent(currentAgent));
        listeInterventions.refresh();
        updateStats();
    }

    private void updateStats() {
        lblInProgress.setText(String.valueOf(
                dao.countByStatut(currentAgent, "In Progress")));
        lblCompleted.setText(String.valueOf(
                dao.countByStatut(currentAgent, "Completed")));
        lblWeek.setText(String.valueOf(
                dao.countThisWeek(currentAgent)));
    }

    @FXML
    private void handleNewIntervention() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/AjouterIntervention.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Nouvelle Intervention");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadData();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void ouvrirModifier(Intervention intervention) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/ModifierIntervention.fxml"));
            Parent root = loader.load();
            ModifierInterventionController ctrl = loader.getController();
            ctrl.setIntervention(intervention);
            Stage stage = new Stage();
            stage.setTitle("Modifier Intervention");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadData();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void supprimerIntervention(Intervention inter) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer l'intervention : " + inter.getAlertZone() + " ?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmer la suppression");
        confirm.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.YES) {
                dao.delete(inter.getAlertZone(), inter.getAgentName());
                loadData();
            }
        });
    }

    @FXML
    private void handleLogout() {
        // Arrêter les services avant de fermer
        if (notificationService != null) {
            notificationService.arreter();
            System.out.println("🛑 Service de notifications arrêté");
        }
        
        if (affectationService != null) {
            affectationService.arreter();
            System.out.println("🛑 Service d'affectations arrêté");
        }
        
        listeInterventions.getScene().getWindow().hide();
    }
    
    /**
     * Affiche le popup d'affectation
     * Un seul popup à la fois pour éviter les doublons
     */
    private void afficherPopupAffectation(Affectation affectation) {
        // Vérifier si un popup est déjà ouvert
        if (popupAffectationStage != null && popupAffectationStage.isShowing()) {
            System.out.println("⚠️ Un popup d'affectation est déjà ouvert, ignoré");
            return;
        }
        
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/PopupAffectation.fxml"));
            Parent root = loader.load();
            
            // Passer l'affectation au contrôleur
            PopupAffectationController ctrl = loader.getController();
            ctrl.setAffectation(affectation);
            
            // Créer et afficher le popup
            popupAffectationStage = new Stage();
            popupAffectationStage.setTitle("🚨 Nouvelle Affectation");
            popupAffectationStage.initModality(Modality.APPLICATION_MODAL);
            popupAffectationStage.setScene(new Scene(root));
            popupAffectationStage.setResizable(false);
            
            // Nettoyer la référence quand le popup se ferme
            popupAffectationStage.setOnHidden(e -> {
                popupAffectationStage = null;
                System.out.println("🔒 Popup d'affectation fermé");
            });
            
            popupAffectationStage.show();
            
            System.out.println("🚨 Popup d'affectation affiché pour: " + affectation.getLocalisationAlerte());
            
        } catch (IOException e) {
            System.out.println("❌ Erreur affichage popup affectation: " + e.getMessage());
            e.printStackTrace();
        }
    }
    @FXML
    private void handleStatistiques() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/Statistiques.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Statistiques");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void handleOuvrirAssistant() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/AssistantIA.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("🤖 Assistant IA — ForestGuard");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleGenererPDF() {
        Intervention selected = listeInterventions.getSelectionModel().getSelectedItem();
        
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Attention");
            alert.setHeaderText(null);
            alert.setContentText("Veuillez sélectionner une intervention dans la liste !");
            alert.showAndWait();
            return;
        }
        
        // FileChooser pour choisir l'emplacement du PDF
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Enregistrer le rapport PDF");
        fc.getExtensionFilters().add(
            new javafx.stage.FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf")
        );
        
        // Nom de fichier par défaut
        String nomFichier = "Rapport_" + 
                           selected.getAlertZone().replaceAll("[^a-zA-Z0-9]", "_") + 
                           "_" + java.time.LocalDate.now() + ".pdf";
        fc.setInitialFileName(nomFichier);
        
        java.io.File file = fc.showSaveDialog(listeInterventions.getScene().getWindow());
        
        if (file != null) {
            try {
                // Génération du PDF
                PDFGenerator.genererRapportIntervention(selected, file);
                
                System.out.println("✅ Rapport PDF généré : " + file.getAbsolutePath());
                
                // 📄 Ouvrir automatiquement le PDF avec le lecteur par défaut
                ouvrirPDF(file);
                
                // Message de succès
                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setTitle("Succès");
                success.setHeaderText("Rapport généré avec succès !");
                success.setContentText("Le rapport a été enregistré et ouvert :\n" + file.getAbsolutePath());
                success.showAndWait();
                
            } catch (Exception e) {
                // Message d'erreur
                Alert error = new Alert(Alert.AlertType.ERROR);
                error.setTitle("Erreur");
                error.setHeaderText("Erreur lors de la génération du PDF");
                error.setContentText("Détails : " + e.getMessage());
                error.showAndWait();
                
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Ouvre le fichier PDF avec l'application par défaut du système
     * @param file Le fichier PDF à ouvrir
     */
    private void ouvrirPDF(java.io.File file) {
        try {
            // Vérifier que Desktop est supporté
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                
                // Vérifier que l'action OPEN est supportée
                if (desktop.isSupported(java.awt.Desktop.Action.OPEN)) {
                    // Ouvrir le fichier avec l'application par défaut
                    desktop.open(file);
                    System.out.println("📄 PDF ouvert avec le lecteur par défaut");
                } else {
                    System.out.println("⚠️ L'ouverture de fichiers n'est pas supportée sur ce système");
                }
            } else {
                System.out.println("⚠️ Desktop n'est pas supporté sur ce système");
            }
        } catch (Exception e) {
            System.out.println("❌ Erreur lors de l'ouverture du PDF : " + e.getMessage());
            e.printStackTrace();
            
            // Afficher un message à l'utilisateur
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Attention");
            alert.setHeaderText("Impossible d'ouvrir le PDF automatiquement");
            alert.setContentText("Le fichier a été créé mais ne peut pas être ouvert automatiquement.\n" +
                               "Veuillez l'ouvrir manuellement :\n" + file.getAbsolutePath());
            alert.showAndWait();
        }
    }
    
    /**
     * Met à jour le badge du nombre d'affectations en attente
     */
    private void mettreAJourBadgeAffectations() {
        if (badgeAffectations == null) return;
        
        try {
            dao.AffectationDAO affectationDAO = new dao.AffectationDAO();
            int count = affectationDAO.countAffectationsEnCours(idPompier);
            
            if (count > 0) {
                badgeAffectations.setText(String.valueOf(count));
                badgeAffectations.setVisible(true);
                System.out.println("📬 Badge affectations mis à jour: " + count);
            } else {
                badgeAffectations.setVisible(false);
            }
        } catch (Exception e) {
            System.out.println("❌ Erreur mise à jour badge: " + e.getMessage());
        }
    }
    
    /**
     * Ouvre la fenêtre de liste des affectations
     */
    @FXML
    private void handleAffectations() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/ListeAffectations.fxml"));
            Parent root = loader.load();
            
            Stage stage = new Stage();
            stage.setTitle("📬 Mes Affectations — ForestGuard");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
            
            // Mettre à jour le badge après fermeture
            mettreAJourBadgeAffectations();
            
        } catch (IOException e) {
            System.out.println("❌ Erreur ouverture liste affectations: " + e.getMessage());
            e.printStackTrace();
        }
    }
}