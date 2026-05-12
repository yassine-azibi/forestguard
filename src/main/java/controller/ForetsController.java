package controller;

import dao.ForetDAO;
import model.Foret;
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

public class ForetsController implements Initializable {

    @FXML private Label lblTotal;
    @FXML private VBox conteneurCartes;
    @FXML private ScrollPane scrollPane;

    private final ForetDAO dao = new ForetDAO();

    @Override
    public void initialize(URL url, ResourceBundle rb) { chargerDonnees(); }

    private void chargerDonnees() {
        List<Foret> liste = dao.getAll();
        lblTotal.setText(String.valueOf(dao.count()));
        conteneurCartes.getChildren().clear();
        if (liste.isEmpty()) {
            Label vide = new Label("Aucune foret trouvee");
            vide.setStyle("-fx-text-fill:#4ade80;-fx-font-size:14;");
            conteneurCartes.getChildren().add(vide);
            return;
        }
        for (Foret f : liste) conteneurCartes.getChildren().add(creerCarte(f));
    }

    private HBox creerCarte(Foret f) {
        String[] risqueCfg = switch (f.getNiveauRisque() != null ? f.getNiveauRisque() : "Moyen") {
            case "Critique" -> new String[]{"#3f0000","#ef4444","#7f1d1d","#fca5a5"};
            case "Eleve","Élevé" -> new String[]{"#3f1800","#f97316","#7c2d12","#fdba74"};
            default -> new String[]{"#0d2418","#16a34a","#052e16","#4ade80"};
        };

        Label ico = new Label("🌲");
        ico.setStyle("-fx-background-color:" + risqueCfg[2] + ";-fx-background-radius:50;" +
            "-fx-min-width:46;-fx-min-height:46;-fx-max-width:46;-fx-max-height:46;" +
            "-fx-alignment:center;-fx-font-size:20;" +
            "-fx-border-color:" + risqueCfg[1] + ";-fx-border-radius:50;-fx-border-width:1.5;");

        Label lblId = new Label("#" + f.getId());
        lblId.setStyle("-fx-text-fill:#334155;-fx-font-size:10;-fx-font-weight:bold;");
        lblId.setPrefWidth(30);

        Label lblNom = new Label(f.getNom());
        lblNom.setStyle("-fx-text-fill:white;-fx-font-size:14;-fx-font-weight:bold;");
        Label lblLoc = new Label("📍  " + f.getLocalisation()
            + "    🌿  " + (f.getTypeVegetation() != null ? f.getTypeVegetation() : "—"));
        lblLoc.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:11;");
        Label lblSup = new Label("📐  " + String.format("%.0f ha", f.getSuperficie())
            + "    🗺  " + f.getCoordonnees());
        lblSup.setStyle("-fx-text-fill:#64748b;-fx-font-size:10;");

        VBox info = new VBox(4, lblNom, lblLoc, lblSup);
        info.setPrefWidth(380);

        Label badge = new Label(f.getNiveauRisque() != null ? f.getNiveauRisque() : "—");
        badge.setStyle("-fx-background-color:" + risqueCfg[2] + ";-fx-text-fill:" + risqueCfg[3] + ";" +
            "-fx-background-radius:20;-fx-padding:4 14;-fx-font-size:10;-fx-font-weight:bold;" +
            "-fx-border-color:" + risqueCfg[1] + ";-fx-border-radius:20;-fx-border-width:1;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Region bar = new Region();
        bar.setPrefWidth(4); bar.setMinWidth(4); bar.setMaxWidth(4);
        bar.setStyle("-fx-background-color:" + risqueCfg[1] + ";-fx-background-radius:4 0 0 4;");

        HBox inner = new HBox(12, lblId, ico, info, spacer, badge);
        inner.setAlignment(Pos.CENTER_LEFT);
        inner.setPadding(new Insets(12, 16, 12, 0));
        HBox.setHgrow(inner, Priority.ALWAYS);

        HBox card = new HBox(0, bar, inner);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPrefHeight(72);
        card.setStyle("-fx-background-color:" + risqueCfg[0] + ";-fx-background-radius:12;" +
            "-fx-border-color:" + risqueCfg[1] + "22;-fx-border-radius:12;-fx-border-width:1;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.3),6,0,0,2);-fx-cursor:hand;");
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color:derive(" + risqueCfg[0] + ",15%);" +
            "-fx-background-radius:12;-fx-border-color:" + risqueCfg[1] + "66;" +
            "-fx-border-radius:12;-fx-border-width:1;" +
            "-fx-effect:dropshadow(gaussian," + risqueCfg[1] + "44,10,0,0,3);-fx-cursor:hand;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color:" + risqueCfg[0] + ";" +
            "-fx-background-radius:12;-fx-border-color:" + risqueCfg[1] + "22;" +
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
    @FXML private void goPompiers() {
        Stage s = (Stage) lblTotal.getScene().getWindow();
        NavigationManager.navigateTo(s, "/fxml/Pompiers.fxml");
    }
}
