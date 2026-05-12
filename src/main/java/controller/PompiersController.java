package controller;

import dao.PompierDAO;
import model.Pompier;
import utils.NavigationManager;
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

public class PompiersController implements Initializable {

    @FXML private Label lblTotal, lblDisponibles, lblEnMission;
    @FXML private VBox conteneurCartes;
    @FXML private ScrollPane scrollPane;

    private final PompierDAO dao = new PompierDAO();

    @Override
    public void initialize(URL url, ResourceBundle rb) { chargerDonnees(); }

    private void chargerDonnees() {
        List<Pompier> liste = dao.getAll();
        int total = dao.count();
        int dispo  = dao.countDisponibles();
        lblTotal.setText(String.valueOf(total));
        lblDisponibles.setText(String.valueOf(dispo));
        lblEnMission.setText(String.valueOf(total - dispo));
        conteneurCartes.getChildren().clear();

        if (liste.isEmpty()) {
            Label vide = new Label("Aucun pompier trouve");
            vide.setStyle("-fx-text-fill:#4ade80;-fx-font-size:14;");
            conteneurCartes.getChildren().add(vide);
            return;
        }
        for (Pompier p : liste) conteneurCartes.getChildren().add(creerCarte(p));
    }

    private HBox creerCarte(Pompier p) {
        boolean dispo = "disponible".equals(p.getStatut());
        String accent = dispo ? "#16a34a" : "#e65100";
        String bgCard = dispo ? "#0d2418"  : "#1a1000";
        String badgeBg = dispo ? "#052e16" : "#2d1500";
        String badgeFg = dispo ? "#4ade80" : "#fdba74";
        String badgeBorder = dispo ? "#16a34a" : "#f97316";
        String statutTxt = dispo ? "Disponible" : "En mission";

        Label ico = new Label("🚒");
        ico.setStyle("-fx-background-color:" + badgeBg + ";-fx-background-radius:50;" +
            "-fx-min-width:46;-fx-min-height:46;-fx-max-width:46;-fx-max-height:46;" +
            "-fx-alignment:center;-fx-font-size:20;" +
            "-fx-border-color:" + accent + ";-fx-border-radius:50;-fx-border-width:1.5;");

        Label lblId = new Label("#" + p.getId());
        lblId.setStyle("-fx-text-fill:#334155;-fx-font-size:10;-fx-font-weight:bold;");
        lblId.setPrefWidth(30);

        Label lblNom = new Label(p.getNomComplet());
        lblNom.setStyle("-fx-text-fill:white;-fx-font-size:14;-fx-font-weight:bold;");
        Label lblEmail = new Label("✉  " + p.getEmail());
        lblEmail.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:11;");
        Label lblInfo = new Label("📞  " + p.getTelephone()
            + "    📍  " + (p.getVille() != null ? p.getVille() : "—"));
        lblInfo.setStyle("-fx-text-fill:#64748b;-fx-font-size:10;");

        VBox info = new VBox(4, lblNom, lblEmail, lblInfo);
        info.setPrefWidth(380);

        Label badge = new Label(statutTxt);
        badge.setStyle("-fx-background-color:" + badgeBg + ";-fx-text-fill:" + badgeFg + ";" +
            "-fx-background-radius:20;-fx-padding:4 14;-fx-font-size:10;-fx-font-weight:bold;" +
            "-fx-border-color:" + badgeBorder + ";-fx-border-radius:20;-fx-border-width:1;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Region bar = new Region();
        bar.setPrefWidth(4); bar.setMinWidth(4); bar.setMaxWidth(4);
        bar.setStyle("-fx-background-color:" + accent + ";-fx-background-radius:4 0 0 4;");

        HBox inner = new HBox(12, lblId, ico, info, spacer, badge);
        inner.setAlignment(Pos.CENTER_LEFT);
        inner.setPadding(new Insets(12, 16, 12, 0));
        HBox.setHgrow(inner, Priority.ALWAYS);

        HBox card = new HBox(0, bar, inner);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPrefHeight(72);
        card.setStyle("-fx-background-color:" + bgCard + ";-fx-background-radius:12;" +
            "-fx-border-color:" + accent + "22;-fx-border-radius:12;-fx-border-width:1;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.3),6,0,0,2);-fx-cursor:hand;");
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color:derive(" + bgCard + ",15%);" +
            "-fx-background-radius:12;-fx-border-color:" + accent + "66;" +
            "-fx-border-radius:12;-fx-border-width:1;" +
            "-fx-effect:dropshadow(gaussian," + accent + "44,10,0,0,3);-fx-cursor:hand;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color:" + bgCard + ";" +
            "-fx-background-radius:12;-fx-border-color:" + accent + "22;" +
            "-fx-border-radius:12;-fx-border-width:1;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.3),6,0,0,2);-fx-cursor:hand;"));
        return card;
    }

    @FXML private void actualiser() { chargerDonnees(); }
    @FXML private void goDashboard() {
        Stage s = (Stage) lblTotal.getScene().getWindow();
        NavigationManager.navigateTo(s, "/fxml/Dashboard.fxml");
    }
    @FXML private void goUtilisateurs() {
        Stage s = (Stage) lblTotal.getScene().getWindow();
        NavigationManager.navigateTo(s, "/fxml/Utilisateurs.fxml");
    }
    @FXML private void goForets() {
        Stage s = (Stage) lblTotal.getScene().getWindow();
        NavigationManager.navigateTo(s, "/fxml/Forets.fxml");
    }
}
