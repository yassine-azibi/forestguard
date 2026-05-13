package controller;

import model.Foret;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class ForetCell extends ListCell<Foret> {

    private final HBox  conteneur       = new HBox(14);
    private final VBox  infos           = new VBox(3);
    private final Label lblNom          = new Label();
    private final Label lblLocalisation = new Label();
    private final Label lblSuperficie   = new Label();
    private final Label lblVegetation   = new Label();
    private final Label lblDate         = new Label();
    private final Label lblRisque       = new Label();

    // Boutons identiques à la liste incendie
    private final Button btnModifier  = new Button("\u270F");
    private final Button btnSupprimer = new Button("\uD83D\uDDD1");

    private final ForetPrincipal controller;

    public ForetCell(ForetPrincipal controller) {
        this.controller = controller;
        construireUI();
    }

    private void construireUI() {

        // ── Avatar vert rond avec icône arbre (comme flamme pour incendie) ──
        Label lblIcone = new Label("\uD83C\uDF32");
        lblIcone.setStyle("-fx-font-size:18;");
        Circle cercle = new Circle(24, Color.web("#16a34a"));
        StackPane avatar = new StackPane(cercle, lblIcone);
        avatar.setMinSize(48, 48);
        avatar.setMaxSize(48, 48);

        // ── Nom ──
        lblNom.setStyle("-fx-font-size:14;-fx-font-weight:bold;-fx-text-fill:#1a2e1e;");

        // ── Ligne 1 : localisation + superficie ──
        lblLocalisation.setStyle("-fx-font-size:11;-fx-text-fill:#475569;");
        lblSuperficie.setStyle("-fx-font-size:11;-fx-text-fill:#475569;");
        HBox ligne1 = new HBox(14, lblLocalisation, lblSuperficie);
        ligne1.setAlignment(Pos.CENTER_LEFT);

        // ── Ligne 2 : végétation + date ──
        lblVegetation.setStyle("-fx-font-size:11;-fx-text-fill:#475569;");
        lblDate.setStyle("-fx-font-size:11;-fx-text-fill:#475569;");
        HBox ligne2 = new HBox(14, lblVegetation, lblDate);
        ligne2.setAlignment(Pos.CENTER_LEFT);

        infos.getChildren().addAll(lblNom, ligne1, ligne2);
        infos.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(infos, Priority.ALWAYS);

        // ── Badge risque (texte coloré seul, aligné à droite) ──
        lblRisque.setStyle("-fx-font-size:12;-fx-font-weight:bold;");
        lblRisque.setMinWidth(70);
        lblRisque.setAlignment(Pos.CENTER_RIGHT);

        // ── Bouton Modifier (icône seule, style identique incendie) ──
        String styleModifier =
            "-fx-background-color:#ede9fe;-fx-text-fill:#7c3aed;" +
            "-fx-background-radius:8;-fx-border-color:#ddd6fe;" +
            "-fx-border-radius:8;-fx-border-width:1;" +
            "-fx-cursor:hand;-fx-pref-width:32;-fx-pref-height:32;-fx-padding:0;";
        btnModifier.setStyle(styleModifier);
        btnModifier.setOnMouseEntered(e -> btnModifier.setStyle(styleModifier.replace("#ede9fe","#ddd6fe")));
        btnModifier.setOnMouseExited(e  -> btnModifier.setStyle(styleModifier));
        btnModifier.setOnAction(e -> {
            Foret foret = getItem();
            if (foret != null) controller.ouvrirModalModifierForet(foret);
        });

        // ── Bouton Supprimer (icône seule, style identique incendie) ──
        String styleSupprimer =
            "-fx-background-color:#fee2e2;-fx-text-fill:#dc2626;" +
            "-fx-background-radius:8;-fx-border-color:#fecaca;" +
            "-fx-border-radius:8;-fx-border-width:1;" +
            "-fx-cursor:hand;-fx-pref-width:32;-fx-pref-height:32;-fx-padding:0;";
        btnSupprimer.setStyle(styleSupprimer);
        btnSupprimer.setOnMouseEntered(e -> btnSupprimer.setStyle(styleSupprimer.replace("#fee2e2","#fecaca")));
        btnSupprimer.setOnMouseExited(e  -> btnSupprimer.setStyle(styleSupprimer));
        btnSupprimer.setOnAction(e -> {
            Foret foret = getItem();
            if (foret != null) controller.ouvrirOverlayConfirmation(foret);
        });

        // ── Actions : badge + boutons sur une ligne (comme incendie) ──
        HBox actions = new HBox(6, lblRisque, btnModifier, btnSupprimer);
        actions.setAlignment(Pos.CENTER_RIGHT);

        // ── Assemblage ──
        conteneur.getChildren().addAll(avatar, infos, actions);
        conteneur.setAlignment(Pos.CENTER_LEFT);
        conteneur.setPadding(new Insets(14, 16, 14, 16));

        String styleNormal =
            "-fx-background-color:rgba(255,255,255,0.92);" +
            "-fx-background-radius:14;" +
            "-fx-border-color:#e2e8f0;" +
            "-fx-border-radius:14;" +
            "-fx-border-width:1;";
        conteneur.setStyle(styleNormal);

        conteneur.setOnMouseEntered(e -> conteneur.setStyle(
            "-fx-background-color:#f0fdf4;" +
            "-fx-background-radius:14;" +
            "-fx-border-color:#bbf7d0;" +
            "-fx-border-radius:14;" +
            "-fx-border-width:1;"
        ));
        conteneur.setOnMouseExited(e -> conteneur.setStyle(styleNormal));
    }

    @Override
    protected void updateItem(Foret foret, boolean empty) {
        super.updateItem(foret, empty);

        if (empty || foret == null) {
            setGraphic(null);
            setStyle("-fx-background-color:transparent;");
            return;
        }

        lblNom.setText(foret.getNom());
        lblLocalisation.setText("\uD83D\uDCCD " + (foret.getLocalisation() != null ? foret.getLocalisation() : ""));
        lblSuperficie.setText("\uD83C\uDF32 " + foret.getSuperficie() + " ha");
        lblVegetation.setText("\uD83C\uDF31 " + (foret.getTypeVegetation() != null ? foret.getTypeVegetation() : ""));
        lblDate.setText("\uD83D\uDCC5 " + (foret.getDateCreation() != null ? foret.getDateCreation() : ""));

        // Badge risque — texte coloré seul (comme incendie)
        String risque = foret.getNiveauRisque() != null ? foret.getNiveauRisque() : "";
        lblRisque.setText(risque.toUpperCase());
        String badgeFg = switch (risque.toLowerCase()) {
            case "élevé", "eleve", "high" -> "#ea580c";
            case "critique", "critical"   -> "#dc2626";
            case "moyen", "medium"        -> "#ca8a04";
            default                       -> "#16a34a";
        };
        lblRisque.setStyle("-fx-font-size:12;-fx-font-weight:bold;-fx-text-fill:" + badgeFg + ";");

        setGraphic(conteneur);
        setStyle("-fx-background-color:transparent;-fx-padding:4 0;");
    }
}

