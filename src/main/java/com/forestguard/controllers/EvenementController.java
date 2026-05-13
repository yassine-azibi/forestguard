package com.forestguard.controllers;

import com.forestguard.entities.Utilisateur;
import com.forestguard.utils.MyConnection;
import com.forestguard.utils.Session;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EvenementController {

    @FXML private VBox evenementsListBox;
    @FXML private ImageView backgroundImage;
    @FXML private ComboBox<String> typeFilterCombo;
    @FXML private Button btnNouvelEvenement;

    private Utilisateur utilisateur;

    private static final String[] TYPES = {"Tous", "Randonnée", "Reboisement", "Atelier", "Sensibilisation"};

    @FXML
    private void initialize() {
        // Binding arrière-plan : l'ImageView couvre toujours toute la fenêtre
        if (backgroundImage != null) {
            backgroundImage.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    newScene.windowProperty().addListener((obs2, oldWin, newWin) -> {
                        if (newWin != null) {
                            backgroundImage.fitWidthProperty().bind(newWin.widthProperty());
                            backgroundImage.fitHeightProperty().bind(newWin.heightProperty());
                        }
                    });
                    if (newScene.getWindow() != null) {
                        backgroundImage.fitWidthProperty().bind(newScene.getWindow().widthProperty());
                        backgroundImage.fitHeightProperty().bind(newScene.getWindow().heightProperty());
                    }
                }
            });
        }

        if (typeFilterCombo != null) {
            typeFilterCombo.setItems(FXCollections.observableArrayList(TYPES));
            typeFilterCombo.setValue("Tous");
            typeFilterCombo.valueProperty().addListener((o, oldv, newv) -> loadEvents());
        }

        if (btnNouvelEvenement != null) {
            btnNouvelEvenement.setOnAction(e -> openNouvelEvenementDialog());
        }

        loadEvents();
    }

    public void setUtilisateur(Utilisateur u) {
        this.utilisateur = (u == null) ? Session.getCurrentUser() : u;
    }

    // ---------------------------------------------------------------
    // Chargement des événements
    // ---------------------------------------------------------------
    private void loadEvents() {
        if (evenementsListBox == null) return;
        evenementsListBox.getChildren().clear();

        String filter = (typeFilterCombo == null) ? "Tous" : typeFilterCombo.getValue();
        boolean filtered = filter != null && !filter.equals("Tous");

        // FIX CRITIQUE : on charge la note moyenne dans la MÊME requête via LEFT JOIN
        // pour éviter "Operation not allowed after ResultSet closed" causé par
        // l'ouverture d'une 2e connexion (getAverageRating) à l'intérieur du ResultSet.
        String sql = "SELECT e.id, e.titre, e.description, e.foret, e.date_evenement, "
                + "e.capacite, e.places_restantes, e.type_evenement, "
                + "AVG(a.note) AS avg_note "
                + "FROM evenement_forestier e "
                + "LEFT JOIN avis_evenement a ON a.evenement_id = e.id "
                + (filtered ? "WHERE e.type_evenement = ? " : "")
                + "GROUP BY e.id, e.titre, e.description, e.foret, e.date_evenement, "
                + "e.capacite, e.places_restantes, e.type_evenement "
                + "ORDER BY e.date_evenement ASC";

        try (Connection conn = MyConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (filtered) stmt.setString(1, filter);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int id        = rs.getInt("id");
                    String titre  = rs.getString("titre");
                    String desc   = rs.getString("description");
                    String foret  = rs.getString("foret");
                    Date date     = rs.getDate("date_evenement");
                    int capacite  = rs.getInt("capacite");
                    int places    = rs.getInt("places_restantes");
                    String type   = rs.getString("type_evenement");
                    double avg    = rs.getDouble("avg_note"); // 0.0 si NULL (pas d'avis)

                    // protection contre date NULL
                    LocalDate localDate = (date != null) ? date.toLocalDate() : LocalDate.now();

                    VBox card = buildEventCard(id, titre, desc, foret, localDate, capacite, places, type, avg);
                    evenementsListBox.getChildren().add(card);
                }
            }
        } catch (SQLException ex) {
            showError("Erreur BD", "Impossible de charger les événements : " + ex.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Construction d'une carte événement
    // ---------------------------------------------------------------
    private VBox buildEventCard(int id, String titre, String desc, String foret,
                                LocalDate date, int capacite, int places, String type, double avg) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: rgba(10,30,14,0.75); "
                + "-fx-border-color: rgba(34,197,94,0.25); "
                + "-fx-border-radius:8; -fx-background-radius:8; -fx-padding:12;");

        Label titleLbl = new Label(titre);
        titleLbl.setStyle("-fx-text-fill: white; -fx-font-size:16px; -fx-font-weight:700;");
        HBox.setHgrow(titleLbl, Priority.ALWAYS);

        Label badge = new Label(type);
        badge.setStyle("-fx-background-color: rgba(34,197,94,0.20); -fx-text-fill: #bbf7d0; "
                + "-fx-padding:3 8 3 8; -fx-border-radius:4; -fx-background-radius:4;");

        HBox topRow = new HBox(10, titleLbl, badge);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label meta = new Label("🌲 " + foret + "   •   📅 " + date.toString());
        meta.setStyle("-fx-text-fill: #cfead0;");

        // FIX 4: label description avec wrapping et style lisible
        Label descLbl = new Label(desc != null ? desc : "");
        descLbl.setWrapText(true);
        descLbl.setStyle("-fx-text-fill: #d4edda; -fx-font-size:13px;");

        Label placesLbl = new Label("Places restantes: " + places + "  |  Capacité: " + capacite);
        placesLbl.setStyle("-fx-text-fill: #e6f9ea;");

        Label ratingLbl = new Label(renderStars(avg));
        ratingLbl.setStyle("-fx-text-fill: #ffd166; -fx-font-size:14px;");

        Button btnInscrire = new Button("+ Inscrire");
        btnInscrire.setStyle("-fx-background-color: linear-gradient(to right,#22c55e,#15803d); "
                + "-fx-text-fill: white; -fx-cursor: hand; -fx-background-radius:4;");

        // FIX 5: désactiver le bouton si plus de places
        if (places <= 0) {
            btnInscrire.setDisable(true);
            btnInscrire.setText("Complet");
        } else {
            btnInscrire.setOnAction(e -> openInscriptionDialog(id, titre, places));
        }

        Button btnAvis = new Button("★ Avis");
        btnAvis.setStyle("-fx-background-color: transparent; -fx-text-fill: #ffd166; "
                + "-fx-border-color: #ffd166; -fx-border-width:1; "
                + "-fx-border-radius:4; -fx-background-radius:4; -fx-cursor: hand;");
        btnAvis.setOnAction(e -> openAvisDialog(id));

        HBox bottomRow = new HBox(12, placesLbl, ratingLbl, btnInscrire, btnAvis);
        bottomRow.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(topRow, meta, descLbl, bottomRow);
        return card;
    }

    // ---------------------------------------------------------------
    // Affichage des étoiles
    // ---------------------------------------------------------------
    private String renderStars(double avg) {
        int full = (int) Math.round(avg);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) sb.append(i < full ? "★" : "☆");
        return sb.toString();
    }

    // ---------------------------------------------------------------
    // Dialogue inscription
    // ---------------------------------------------------------------
    private void openInscriptionDialog(int evenementId, String titre, int placesRestantes) {
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle("Inscription — " + titre);

        VBox root = new VBox(10);
        root.setPadding(new Insets(14));
        root.setStyle("-fx-background-color: #0d2318;");

        String fieldStyle = "-fx-background-color: rgba(34,60,34,0.9); -fx-text-fill: white;";

        TextField nomField   = new TextField();
        TextField emailField = new TextField();
        int max = Math.max(1, placesRestantes);
        Spinner<Integer> spinner = new Spinner<>(1, max, 1);
        spinner.setEditable(true);  // FIX 7: spinner éditable

        nomField.setStyle(fieldStyle);
        emailField.setStyle(fieldStyle);
        spinner.setStyle(fieldStyle);

        Utilisateur u = (utilisateur == null) ? Session.getCurrentUser() : utilisateur;
        if (u != null) {
            nomField.setText(u.getNom());
            emailField.setText(u.getEmail());
        }

        Label lblInfo = new Label("Places restantes: " + placesRestantes);
        lblInfo.setStyle("-fx-text-fill: #cfead0;");

        Button submit  = new Button("S'inscrire");
        submit.setStyle("-fx-background-color: linear-gradient(to right,#22c55e,#15803d); "
                + "-fx-text-fill: white; -fx-cursor: hand; -fx-background-radius:4;");
        Label feedback = new Label("");

        submit.setOnAction(ev -> {
            String nom   = nomField.getText().trim();
            String email = emailField.getText().trim();
            int nb       = spinner.getValue();

            if (nom.isBlank() || email.isBlank()) {
                feedback.setText("Veuillez remplir le nom et l'email.");
                feedback.setStyle("-fx-text-fill: #f87171;");
                return;
            }
            // FIX 8: validation email basique
            if (!email.contains("@") || !email.contains(".")) {
                feedback.setText("Adresse email invalide.");
                feedback.setStyle("-fx-text-fill: #f87171;");
                return;
            }
            if (nb <= 0 || nb > placesRestantes) {
                feedback.setText("Nombre de participants invalide (max: " + placesRestantes + ").");
                feedback.setStyle("-fx-text-fill: #f87171;");
                return;
            }

            try (Connection conn = MyConnection.getConnection()) {
                conn.setAutoCommit(false);
                try (PreparedStatement ins = conn.prepareStatement(
                        "INSERT INTO inscription_evenement "
                                + "(evenement_id, nom_responsable, email, nb_participants) VALUES (?,?,?,?)")) {
                    ins.setInt(1, evenementId);
                    ins.setString(2, nom);
                    ins.setString(3, email);
                    ins.setInt(4, nb);
                    ins.executeUpdate();
                }
                try (PreparedStatement upd = conn.prepareStatement(
                        "UPDATE evenement_forestier "
                                + "SET places_restantes = GREATEST(0, places_restantes - ?) WHERE id = ?")) {
                    upd.setInt(1, nb);
                    upd.setInt(2, evenementId);
                    upd.executeUpdate();
                }
                conn.commit();
                feedback.setText("Inscription réussie !");
                feedback.setStyle("-fx-text-fill: #4ade80;");
                loadEvents();
                modal.close();
            } catch (SQLException ex) {
                feedback.setText("Erreur inscription : " + ex.getMessage());
                feedback.setStyle("-fx-text-fill: #f87171;");
            }
        });

        applyLabelStyle(root);
        root.getChildren().addAll(
                new Label("Nom responsable:"), nomField,
                new Label("Email:"), emailField,
                new Label("Nombre de participants:"), spinner,
                lblInfo, submit, feedback);

        modal.setScene(new Scene(root, 360, 320));
        modal.showAndWait();
    }

    // ---------------------------------------------------------------
    // Dialogue avis
    // ---------------------------------------------------------------
    private void openAvisDialog(int evenementId) {
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle("Laisser un avis");

        VBox root = new VBox(10);
        root.setPadding(new Insets(12));
        root.setStyle("-fx-background-color: #0d2318;");

        String fieldStyle = "-fx-background-color: rgba(34,60,34,0.9); -fx-text-fill: white; -fx-control-inner-background: rgba(34,60,34,0.9);";

        TextField auteur = new TextField();
        auteur.setStyle(fieldStyle);

        TextArea commentaire = new TextArea();
        commentaire.setPrefRowCount(4);
        commentaire.setWrapText(true);
        // FIX NPE : ne jamais utiliser lookup(".content") — le nœud interne
        // n'existe pas encore quand le CSS est appliqué → lookup() retourne null.
        // -fx-control-inner-background est la propriété correcte pour le fond
        // du TextArea sans toucher aux nœuds internes.
        commentaire.setStyle(fieldStyle);

        Utilisateur u = (utilisateur == null) ? Session.getCurrentUser() : utilisateur;
        if (u != null) auteur.setText(u.getNom());

        // FIX 9: suppression du ToggleGroup – les étoiles doivent s'allumer
        // de 1 jusqu'à la note choisie (pas une seule sélectionnée).
        HBox starsBox  = new HBox(6);
        final int[] rating = {5};
        List<ToggleButton> stars = new ArrayList<>();

        for (int i = 1; i <= 5; i++) {
            ToggleButton tb = new ToggleButton("★");
            tb.setSelected(true);   // 5 étoiles par défaut
            tb.setUserData(i);
            tb.setStyle("-fx-font-size:20px; -fx-text-fill: #ffd166; "
                    + "-fx-background-color: transparent; -fx-cursor: hand;");
            final int starVal = i;
            tb.setOnAction(ev -> {
                rating[0] = starVal;
                for (ToggleButton s : stars) {
                    boolean on = (int) s.getUserData() <= rating[0];
                    s.setText(on ? "★" : "☆");
                    s.setSelected(on);
                }
            });
            stars.add(tb);
            starsBox.getChildren().add(tb);
        }

        Button submit  = new Button("Envoyer");
        submit.setStyle("-fx-background-color: linear-gradient(to right,#22c55e,#15803d); "
                + "-fx-text-fill: white; -fx-cursor: hand; -fx-background-radius:4;");
        Label feedback = new Label("");

        submit.setOnAction(ev -> {
            String nom  = auteur.getText().trim();
            String comm = commentaire.getText().trim();
            int note    = rating[0];

            if (nom.isBlank()) {
                feedback.setText("Veuillez indiquer votre nom.");
                feedback.setStyle("-fx-text-fill: #f87171;");
                return;
            }
            // FIX 10: note doit être valide
            if (note < 1 || note > 5) {
                feedback.setText("Veuillez sélectionner une note (1-5 étoiles).");
                feedback.setStyle("-fx-text-fill: #f87171;");
                return;
            }

            try (Connection conn = MyConnection.getConnection();
                 PreparedStatement ins = conn.prepareStatement(
                         "INSERT INTO avis_evenement "
                                 + "(evenement_id, nom_auteur, note, commentaire) VALUES (?,?,?,?)")) {
                ins.setInt(1, evenementId);
                ins.setString(2, nom);
                ins.setInt(3, note);
                ins.setString(4, comm);
                ins.executeUpdate();
                feedback.setText("Merci pour votre avis !");
                feedback.setStyle("-fx-text-fill: #4ade80;");
                loadEvents();
                modal.close();
            } catch (SQLException ex) {
                feedback.setText("Erreur avis : " + ex.getMessage());
                feedback.setStyle("-fx-text-fill: #f87171;");
            }
        });

        applyLabelStyle(root);
        root.getChildren().addAll(
                new Label("Votre nom :"), auteur,
                new Label("Note :"), starsBox,
                new Label("Commentaire :"), commentaire,
                submit, feedback);

        modal.setScene(new Scene(root, 360, 380));
        modal.showAndWait();
    }

    // ---------------------------------------------------------------
    // Dialogue nouvel événement
    // ---------------------------------------------------------------
    private void openNouvelEvenementDialog() {
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle("Nouvel événement");

        VBox root = new VBox(10);
        root.setPadding(new Insets(12));
        root.setStyle("-fx-background-color: #0d2318;");

        String fieldStyle = "-fx-background-color: rgba(34,60,34,0.9); -fx-text-fill: white;";

        TextField titre   = new TextField();
        TextField foret   = new TextField();
        DatePicker datePicker = new DatePicker(LocalDate.now().plusDays(7));
        ComboBox<String> typeCombo = new ComboBox<>(
                FXCollections.observableArrayList("Randonnée", "Reboisement", "Atelier", "Sensibilisation"));
        typeCombo.setValue("Randonnée");
        Spinner<Integer> capacite = new Spinner<>(1, 1000, 20);
        capacite.setEditable(true);
        TextArea desc = new TextArea();
        desc.setPrefRowCount(4);
        desc.setWrapText(true);

        titre.setStyle(fieldStyle);
        foret.setStyle(fieldStyle);
        datePicker.setStyle(fieldStyle);
        typeCombo.setStyle(fieldStyle);
        capacite.setStyle(fieldStyle);
        desc.setStyle(fieldStyle);

        Button submit  = new Button("Créer");
        submit.setStyle("-fx-background-color: linear-gradient(to right,#22c55e,#15803d); "
                + "-fx-text-fill: white; -fx-cursor: hand; -fx-background-radius:4;");
        Label feedback = new Label("");

        submit.setOnAction(ev -> {
            String t           = titre.getText().trim();
            String f           = foret.getText().trim();
            LocalDate d        = datePicker.getValue();
            String tp          = typeCombo.getValue();
            int cap            = capacite.getValue();
            String description = desc.getText().trim();

            if (t.isBlank() || f.isBlank() || d == null) {
                feedback.setText("Veuillez remplir les champs obligatoires (titre, forêt, date).");
                feedback.setStyle("-fx-text-fill: #f87171;");
                return;
            }
            // FIX 11: interdire les dates passées
            if (d.isBefore(LocalDate.now())) {
                feedback.setText("La date doit être aujourd'hui ou dans le futur.");
                feedback.setStyle("-fx-text-fill: #f87171;");
                return;
            }

            try (Connection conn = MyConnection.getConnection();
                 PreparedStatement ins = conn.prepareStatement(
                         "INSERT INTO evenement_forestier "
                                 + "(titre, description, foret, date_evenement, capacite, places_restantes, type_evenement) "
                                 + "VALUES (?,?,?,?,?,?,?)")) {
                ins.setString(1, t);
                ins.setString(2, description);
                ins.setString(3, f);
                ins.setDate(4, Date.valueOf(d));
                ins.setInt(5, cap);
                ins.setInt(6, cap);
                ins.setString(7, tp);
                ins.executeUpdate();
                feedback.setText("Événement créé avec succès !");
                feedback.setStyle("-fx-text-fill: #4ade80;");
                loadEvents();
                modal.close();
            } catch (SQLException ex) {
                feedback.setText("Erreur création : " + ex.getMessage());
                feedback.setStyle("-fx-text-fill: #f87171;");
            }
        });

        applyLabelStyle(root);
        root.getChildren().addAll(
                new Label("Titre *:"), titre,
                new Label("Forêt *:"), foret,
                new Label("Date *:"), datePicker,
                new Label("Type:"), typeCombo,
                new Label("Capacité:"), capacite,
                new Label("Description:"), desc,
                submit, feedback);

        modal.setScene(new Scene(root, 400, 520));
        modal.showAndWait();
    }

    // ---------------------------------------------------------------
    // Retour au dashboard
    // ---------------------------------------------------------------
    @FXML
    private void handleBackToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/forestguard/views/dashboard.fxml"));
            Parent root = loader.load();
            DashboardController controller = loader.getController();
            controller.setUtilisateur(utilisateur != null ? utilisateur : Session.getCurrentUser());

            Stage stage = (Stage) evenementsListBox.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("ForestGuard - Dashboard");
            stage.setMaximized(true);
        } catch (IOException exception) {
            showError("Erreur", "Impossible de retourner au tableau de bord : " + exception.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Utilitaires
    // ---------------------------------------------------------------
    private void applyLabelStyle(VBox root) {
        root.getChildren().stream()
                .filter(n -> n instanceof Label)
                .forEach(n -> {
                    Label l = (Label) n;
                    if (l.getStyle() == null || l.getStyle().isBlank()) {
                        l.setStyle("-fx-text-fill: #cfead0; -fx-font-weight: bold;");
                    }
                });
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
