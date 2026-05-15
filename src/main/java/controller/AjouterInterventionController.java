package controller;

import dao.AlerteDAO;
import dao.EquipementDAO;
import dao.InterventionDAO;
import model.Equipement;
import model.Intervention;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class AjouterInterventionController implements Initializable {

    @FXML private ComboBox<String>     cbAlerte;
    @FXML private ListView<Equipement> lvEquipements;
    @FXML private TextArea             taResultat;
    @FXML private ComboBox<String>     cbStatut;

    private final AlerteDAO       alerteDAO      = new AlerteDAO();
    private final EquipementDAO   equipementDAO  = new EquipementDAO();
    private final InterventionDAO interventionDAO = new InterventionDAO();

    private String currentAgent = "John Martinez";
    private int currentPompierId = 1;

    private Map<Integer, String> alertesMap;
    
    /**
     * Définit les informations de l'agent connecté
     * Appelé depuis DashboardAgentController
     */
    public void setAgentInfo(String nomComplet, int id) {
        this.currentAgent = nomComplet;
        this.currentPompierId = id;
        System.out.println("✅ Agent défini pour nouvelle intervention: " + nomComplet + " (ID: " + id + ")");
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Charger alertes dans ComboBox
        alertesMap = alerteDAO.getAllForComboBox();
        cbAlerte.setItems(FXCollections.observableArrayList(alertesMap.values()));

        // Charger équipements dans ListView (multi-sélection)
        lvEquipements.setItems(FXCollections.observableArrayList(equipementDAO.getAll()));
        lvEquipements.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Statuts
        cbStatut.setItems(FXCollections.observableArrayList("In Progress", "Completed"));
        cbStatut.setValue("In Progress");
    }
    
    /**
     * Pré-remplit la zone d'alerte dans le ComboBox
     * Utilisé quand une affectation est acceptée
     * @param zone La localisation de l'alerte
     */
    public void preRemplirZone(String zone) {
        if (zone == null || zone.isEmpty()) {
            return;
        }
        
        // Chercher dans la map l'entrée qui contient cette zone
        for (String value : alertesMap.values()) {
            if (value.contains(zone)) {
                cbAlerte.setValue(value);
                System.out.println("✅ Zone pré-remplie: " + value);
                return;
            }
        }
        
        System.out.println("⚠️ Zone non trouvée dans les alertes: " + zone);
    }

    @FXML
    private void handleCreer() {
        // Validation
        if (cbAlerte.getValue() == null) {
            showAlert("Veuillez sélectionner une alerte !");
            return;
        }
        if (lvEquipements.getSelectionModel().getSelectedItems().isEmpty()) {
            showAlert("Veuillez sélectionner au moins un équipement !");
            return;
        }

        // Trouver l'id de l'alerte sélectionnée
        String selectedLabel = cbAlerte.getValue();
        int alerteId = alertesMap.entrySet().stream()
                .filter(e -> e.getValue().equals(selectedLabel))
                .map(Map.Entry::getKey)
                .findFirst().orElse(0);

        // ✅ VÉRIFIER SI UNE INTERVENTION EN COURS EXISTE DÉJÀ POUR CETTE ALERTE
        if (interventionDAO.existeInterventionPourAlerte(alerteId)) {
            Intervention existante = interventionDAO.getInterventionParAlerte(alerteId);
            String message = "⚠️ Une intervention EN COURS existe déjà pour cette alerte!\n\n" +
                           "Zone: " + existante.getAlerteLocalisation() + "\n" +
                           "Agent: " + existante.getAgentName() + "\n" +
                           "Statut: " + existante.getStatut() + "\n" +
                           "Date: " + existante.getStartDate().toLocalDate() + "\n\n" +
                           "Vous ne pouvez pas créer deux interventions en cours pour la même alerte.\n" +
                           "Terminez d'abord l'intervention existante.";
            showAlert(message);
            System.out.println("❌ Tentative de création d'intervention dupliquée pour alerte ID: " + alerteId);
            System.out.println("   Intervention en cours existante: Agent=" + existante.getAgentName() + ", Statut=" + existante.getStatut());
            return;
        }

        String localisation = alerteDAO.getLocalisationById(alerteId);

        // Créer l'intervention
        Intervention inter = new Intervention();
        inter.setAlertZone(localisation);
        inter.setStatut(cbStatut.getValue());
        inter.setStartDate(LocalDateTime.now());
        inter.setAgentName(currentAgent);
        inter.setResultat(taResultat.getText().trim());
        inter.setAlerteId(alerteId);
        inter.setAlerteLocalisation(localisation);

        if (interventionDAO.create(inter)) {
            System.out.println("✅ Intervention créée en base de données");
            System.out.println("   Zone: " + inter.getAlertZone());
            System.out.println("   Agent: " + inter.getAgentName());
            System.out.println("   Statut: " + inter.getStatut());
            
            // Ajouter les équipements
            List<Equipement> selected = lvEquipements.getSelectionModel().getSelectedItems();
            equipementDAO.addEquipements(
                    inter.getAlertZone(),
                    inter.getAgentName(),
                    inter.getStartDate(),
                    selected
            );
            System.out.println("✅ " + selected.size() + " équipement(s) ajouté(s)");

            showSuccess("Intervention créée avec succès !");
            
            // ✅ OUVERTURE DU POPUP D'ANALYSE IA + CARTE
            ouvrirPopupAnalyse(inter, alerteId);
            
            // Fermer la fenêtre APRÈS le popup d'analyse
            System.out.println("🔒 Fermeture de la fenêtre d'ajout");
            handleAnnuler();
        } else {
            System.out.println("❌ Échec de la création de l'intervention");
            showAlert("Erreur lors de la création !");
        }
    }

    @FXML
    private void handleAnnuler() {
        cbAlerte.getScene().getWindow().hide();
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle("Attention");
        a.setContentText(msg);
        a.showAndWait();
    }

    private void showSuccess(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Succès");
        a.setContentText(msg);
        a.showAndWait();
    }
    private void ouvrirPopupAnalyse(Intervention inter, int alerteId) {
        try {
            // Récupérer niveau et type de l'alerte
            String[] infos = alerteDAO.getNiveauEtType(alerteId);
            String niveau = infos[0];
            String type = infos[1];

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/PopupIntervention.fxml"));
            Parent root = loader.load();

            PopupInterventionController ctrl = loader.getController();
            ctrl.setDonnees(inter, niveau, type);

            Stage stage = new Stage();
            try {
                stage.getIcons().add(new Image(getClass().getResourceAsStream("/image/foret-logo.png")));
            } catch (Exception e) {
                System.out.println("⚠️ Impossible de charger l'icône");
            }
            stage.setTitle("Analyse d'Intervention — ForestGuard");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
