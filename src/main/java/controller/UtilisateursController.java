package controller;

import dao.UtilisateurDAO;
import model.Utilisateur;
import utils.NavigationManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class UtilisateursController implements Initializable {

    @FXML private Label lblTotal;
    @FXML private VBox conteneurCartes;
    @FXML private ScrollPane scrollPane;

    private final UtilisateurDAO dao = new UtilisateurDAO();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        chargerDonnees();
    }

    private void chargerDonnees() {
        List<Utilisateur> liste = dao.getAll();
        lblTotal.setText(String.valueOf(dao.count()));
        conteneurCartes.getChildren().clear();

        if (liste.isEmpty()) {
            Label vide = new Label("Aucun utilisateur trouve");
            vide.setStyle("-fx-text-fill:#4ade80;-fx-font-size:14;");
            conteneurCartes.getChildren().add(vide);
            return;
        }

        for (Utilisateur u : liste) {
            conteneurCartes.getChildren().add(creerCarte(u));
        }
    }

    private HBox creerCarte(Utilisateur u) {
        // Icone cercle
        Label ico = new Label("👤");
        ico.setStyle("-fx-background-color:#1e3a5f;-fx-background-radius:50;" +
            "-fx-min-width:46;-fx-min-height:46;-fx-max-width:46;-fx-max-height:46;" +
            "-fx-alignment:center;-fx-font-size:20;" +
            "-fx-border-color:#3b82f6;-fx-border-radius:50;-fx-border-width:1.5;");

        // ID
        Label lblId = new Label("#" + u.getId());
        lblId.setStyle("-fx-text-fill:#334155;-fx-font-size:10;-fx-font-weight:bold;");
        lblId.setPrefWidth(30);

        // Nom
        Label lblNom = new Label(u.getNom());
        lblNom.setStyle("-fx-text-fill:white;-fx-font-size:14;-fx-font-weight:bold;");

        // Email
        Label lblEmail = new Label("✉  " + u.getEmail());
        lblEmail.setStyle("-fx-text-fill:#90CAF9;-fx-font-size:11;");

        // Tel + localisation
        Label lblTel = new Label("📞  " + u.getTelephone()
            + "    📍  " + (u.getLocalisation().isEmpty() ? "—" : u.getLocalisation()));
        lblTel.setStyle("-fx-text-fill:#64748b;-fx-font-size:10;");

        VBox info = new VBox(4, lblNom, lblEmail, lblTel);
        info.setPrefWidth(400);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Badge email vérifié
        Label badge = new Label("Utilisateur");
        badge.setStyle("-fx-background-color:#172554;-fx-text-fill:#93c5fd;" +
            "-fx-background-radius:20;-fx-padding:4 14;" +
            "-fx-font-size:10;-fx-font-weight:bold;" +
            "-fx-border-color:#2563eb;-fx-border-radius:20;-fx-border-width:1;");

        // Barre accent
        Region bar = new Region();
        bar.setPrefWidth(4); bar.setMinWidth(4); bar.setMaxWidth(4);
        bar.setStyle("-fx-background-color:#3b82f6;-fx-background-radius:4 0 0 4;");

        HBox inner = new HBox(12, lblId, ico, info, spacer, badge);
        inner.setAlignment(Pos.CENTER_LEFT);
        inner.setPadding(new Insets(12, 16, 12, 0));
        HBox.setHgrow(inner, Priority.ALWAYS);

        HBox card = new HBox(0, bar, inner);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPrefHeight(72);
        card.setStyle("-fx-background-color:#0d1e35;-fx-background-radius:12;" +
            "-fx-border-color:#1e3a6e22;-fx-border-radius:12;-fx-border-width:1;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.3),6,0,0,2);-fx-cursor:hand;");
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color:#0f2545;" +
            "-fx-background-radius:12;-fx-border-color:#3b82f666;" +
            "-fx-border-radius:12;-fx-border-width:1;" +
            "-fx-effect:dropshadow(gaussian,#3b82f644,10,0,0,3);-fx-cursor:hand;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color:#0d1e35;" +
            "-fx-background-radius:12;-fx-border-color:#1e3a6e22;" +
            "-fx-border-radius:12;-fx-border-width:1;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.3),6,0,0,2);-fx-cursor:hand;"));
        return card;
    }

    @FXML private void actualiser() { chargerDonnees(); }
    @FXML private void goDashboard() {
        Stage s = (Stage) lblTotal.getScene().getWindow();
        NavigationManager.navigateTo(s, "/fxml/Dashboard.fxml");
    }
    @FXML private void goCapteurs() {
        Stage s = (Stage) lblTotal.getScene().getWindow();
        NavigationManager.navigateTo(s, "/fxml/Capteurs.fxml");
    }
    @FXML private void goPompiers() {
        Stage s = (Stage) lblTotal.getScene().getWindow();
        NavigationManager.navigateTo(s, "/fxml/Pompiers.fxml");
    }
    @FXML private void goForets() {
        Stage s = (Stage) lblTotal.getScene().getWindow();
        NavigationManager.navigateTo(s, "/fxml/Forets.fxml");
    }
}
