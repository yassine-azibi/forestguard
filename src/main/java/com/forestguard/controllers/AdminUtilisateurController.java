package com.forestguard.controllers;

import com.forestguard.entities.Utilisateur;
import com.forestguard.services.UtilisateurService;
import com.forestguard.utils.GovernorateUtils;
import com.forestguard.utils.PhoneNumberUtils;
import com.forestguard.utils.Session;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Contrôleur du panneau d'administration — Gestion des utilisateurs.
 *
 * <p>Fonctionnalités :
 * <ul>
 *   <li>Tableau de bord avec statistiques (total, comptes Google, localisations)</li>
 *   <li>Liste paginée de tous les utilisateurs</li>
 *   <li>Recherche en temps réel (nom, email, téléphone, localisation)</li>
 *   <li>Filtrage par gouvernorat</li>
 *   <li>Modification d'un utilisateur via formulaire modal</li>
 *   <li>Suppression avec confirmation</li>
 *   <li>Ajout d'un nouvel utilisateur</li>
 * </ul>
 * </p>
 */
public class AdminUtilisateurController {

    // ── FXML — Toolbar ────────────────────────────────────────────────────────

    @FXML private Label     navAvatarLabel;
    @FXML private ImageView backgroundImage;

    // ── FXML — Stats cards ────────────────────────────────────────────────────

    @FXML private Label totalUsersLabel;
    @FXML private Label googleUsersLabel;
    @FXML private Label classicUsersLabel;
    @FXML private Label governoratesLabel;

    // ── FXML — Filtres ────────────────────────────────────────────────────────

    @FXML private TextField          searchField;
    @FXML private ComboBox<String>   filterGovCombo;
    @FXML private Button             resetFilterButton;

    // ── FXML — Tableau ────────────────────────────────────────────────────────

    @FXML private TableView<Utilisateur>            tableView;
    @FXML private TableColumn<Utilisateur, Integer> colId;
    @FXML private TableColumn<Utilisateur, String>  colNom;
    @FXML private TableColumn<Utilisateur, String>  colEmail;
    @FXML private TableColumn<Utilisateur, String>  colTelephone;
    @FXML private TableColumn<Utilisateur, String>  colLocalisation;
    @FXML private TableColumn<Utilisateur, String>  colType;
    @FXML private TableColumn<Utilisateur, Void>    colActions;

    // ── FXML — Pagination ─────────────────────────────────────────────────────

    @FXML private Label  pageInfoLabel;
    @FXML private Button prevPageButton;
    @FXML private Button nextPageButton;

    // ── FXML — Feedback ───────────────────────────────────────────────────────

    @FXML private Label feedbackLabel;

    // ── State ─────────────────────────────────────────────────────────────────

    private final UtilisateurService utilisateurService = new UtilisateurService();
    private final ObservableList<Utilisateur> allUsers  = FXCollections.observableArrayList();
    private FilteredList<Utilisateur>         filtered;

    private static final int PAGE_SIZE = 10;
    private int currentPage = 0;

    private Utilisateur adminUser;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    public void setAdminUser(Utilisateur user) {
        this.adminUser = user;
        if (navAvatarLabel != null) {
            String name = user != null && user.getNom() != null ? user.getNom() : "AD";
            navAvatarLabel.setText(name.length() >= 2
                    ? name.substring(0, 2).toUpperCase()
                    : name.toUpperCase());
        }
    }

    @FXML
    private void initialize() {
        // Bind background to window size
        Platform.runLater(() -> {
            if (backgroundImage != null && backgroundImage.getScene() != null) {
                backgroundImage.fitWidthProperty().bind(
                        backgroundImage.getScene().getWindow().widthProperty());
                backgroundImage.fitHeightProperty().bind(
                        backgroundImage.getScene().getWindow().heightProperty());
            }
        });

        if (adminUser == null) adminUser = Session.getCurrentUser();
        if (adminUser != null) setAdminUser(adminUser);

        setupTable();
        setupFilters();
        loadUsers();
    }

    // ── Table setup ───────────────────────────────────────────────────────────

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        // Téléphone — format d'affichage +216 XX XXX XXX
        colTelephone.setCellValueFactory(data ->
                new SimpleStringProperty(formatPhone(data.getValue().getTelephone())));

        colLocalisation.setCellValueFactory(new PropertyValueFactory<>("localisation"));

        // Type de compte
        colType.setCellValueFactory(data -> {
            boolean isGoogle = data.getValue().isGoogleAccount();
            return new SimpleStringProperty(isGoogle ? "🔵 Google" : "🔑 Classique");
        });

        // Colonne Actions — boutons Modifier / Supprimer
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn   = new Button("✏ Modifier");
            private final Button deleteBtn = new Button("🗑 Supprimer");
            private final HBox   box       = new HBox(8, editBtn, deleteBtn);

            {
                editBtn.getStyleClass().add("admin-action-edit");
                deleteBtn.getStyleClass().add("admin-action-delete");
                box.setAlignment(Pos.CENTER);

                editBtn.setOnAction(e -> {
                    Utilisateur u = getTableView().getItems().get(getIndex());
                    openEditDialog(u);
                });
                deleteBtn.setOnAction(e -> {
                    Utilisateur u = getTableView().getItems().get(getIndex());
                    handleDelete(u);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        tableView.setPlaceholder(new Label("Aucun utilisateur trouvé."));
        tableView.getStyleClass().add("admin-table");
    }

    // ── Filters setup ─────────────────────────────────────────────────────────

    private void setupFilters() {
        filterGovCombo.getItems().add("Tous les gouvernorats");
        filterGovCombo.getItems().addAll(GovernorateUtils.getGovernorates());
        filterGovCombo.setValue("Tous les gouvernorats");

        searchField.textProperty().addListener((obs, old, val) -> applyFilters());
        filterGovCombo.valueProperty().addListener((obs, old, val) -> applyFilters());
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private void loadUsers() {
        Thread t = new Thread(() -> {
            try {
                List<Utilisateur> users = utilisateurService.getData();
                Platform.runLater(() -> {
                    allUsers.setAll(users);
                    filtered = new FilteredList<>(allUsers, u -> true);
                    applyFilters();
                    updateStats();
                    setFeedback("✅ " + users.size() + " utilisateur(s) chargé(s).", false);
                });
            } catch (Exception e) {
                Platform.runLater(() ->
                        setFeedback("❌ Erreur de chargement : " + e.getMessage(), true));
            }
        }, "admin-load-users");
        t.setDaemon(true);
        t.start();
    }

    // ── Filters & pagination ──────────────────────────────────────────────────

    private void applyFilters() {
        if (filtered == null) return;

        String search = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String gov    = filterGovCombo.getValue();
        boolean allGov = gov == null || gov.equals("Tous les gouvernorats");

        filtered.setPredicate(u -> {
            boolean matchSearch = search.isBlank()
                    || (u.getNom()          != null && u.getNom().toLowerCase().contains(search))
                    || (u.getEmail()        != null && u.getEmail().toLowerCase().contains(search))
                    || (u.getTelephone()    != null && u.getTelephone().contains(search))
                    || (u.getLocalisation() != null && u.getLocalisation().toLowerCase().contains(search));

            boolean matchGov = allGov
                    || (u.getLocalisation() != null && u.getLocalisation().equalsIgnoreCase(gov));

            return matchSearch && matchGov;
        });

        currentPage = 0;
        refreshPage();
    }

    private void refreshPage() {
        if (filtered == null) return;

        int total      = filtered.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
        if (currentPage >= totalPages) currentPage = totalPages - 1;

        int from = currentPage * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, total);

        ObservableList<Utilisateur> page = FXCollections.observableArrayList(
                filtered.subList(from, to));
        tableView.setItems(page);

        pageInfoLabel.setText("Page " + (currentPage + 1) + " / " + totalPages
                + "  (" + total + " résultat(s))");
        prevPageButton.setDisable(currentPage == 0);
        nextPageButton.setDisable(currentPage >= totalPages - 1);
    }

    @FXML
    private void handlePrevPage() {
        if (currentPage > 0) { currentPage--; refreshPage(); }
    }

    @FXML
    private void handleNextPage() {
        if (filtered == null) return;
        int totalPages = (int) Math.ceil((double) filtered.size() / PAGE_SIZE);
        if (currentPage < totalPages - 1) { currentPage++; refreshPage(); }
    }

    @FXML
    private void handleResetFilters() {
        searchField.clear();
        filterGovCombo.setValue("Tous les gouvernorats");
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

    private void updateStats() {
        long total   = allUsers.size();
        long google  = allUsers.stream().filter(Utilisateur::isGoogleAccount).count();
        long classic = total - google;
        long govs    = allUsers.stream()
                .map(Utilisateur::getLocalisation)
                .filter(l -> l != null && !l.isBlank())
                .distinct().count();

        totalUsersLabel.setText(String.valueOf(total));
        googleUsersLabel.setText(String.valueOf(google));
        classicUsersLabel.setText(String.valueOf(classic));
        governoratesLabel.setText(String.valueOf(govs));
    }

    // ── Add user ──────────────────────────────────────────────────────────────

    @FXML
    private void handleAddUser() {
        openEditDialog(null);
    }

    // ── Edit dialog ───────────────────────────────────────────────────────────

    private void openEditDialog(Utilisateur target) {
        boolean isNew = (target == null);

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(tableView.getScene().getWindow());
        dialog.setTitle(isNew ? "Ajouter un utilisateur" : "Modifier l'utilisateur");
        dialog.setResizable(false);

        // ── Form fields ───────────────────────────────────────────────────────
        TextField nomField   = new TextField(isNew ? "" : target.getNom());
        nomField.setPromptText("Nom complet");

        TextField emailField = new TextField(isNew ? "" : target.getEmail());
        emailField.setPromptText("Email");

        TextField phoneField = new TextField(
                isNew ? "" : PhoneNumberUtils.toLocalDisplay(target.getTelephone()));
        phoneField.setPromptText("Téléphone (8 chiffres)");
        phoneField.setTextFormatter(new TextFormatter<>(change -> {
            if (change.getControlNewText().matches("[0-9]{0,8}")) return change;
            return null;
        }));

        ComboBox<String> govCombo = new ComboBox<>();
        govCombo.getItems().addAll(GovernorateUtils.getGovernorates());
        govCombo.setEditable(true);
        govCombo.setPromptText("Gouvernorat");
        if (!isNew && target.getLocalisation() != null) {
            govCombo.setValue(target.getLocalisation());
        }

        PasswordField passField = new PasswordField();
        passField.setPromptText(isNew
                ? "Mot de passe"
                : "Nouveau mot de passe (laisser vide = inchangé)");

        Label errorLbl = new Label();
        errorLbl.setWrapText(true);
        errorLbl.setStyle("-fx-text-fill: #ffcbc7; -fx-font-size: 12px; -fx-font-weight: 600;");

        // ── Buttons ───────────────────────────────────────────────────────────
        Button saveBtn   = new Button(isNew ? "Ajouter" : "Enregistrer");
        Button cancelBtn = new Button("Annuler");
        saveBtn.getStyleClass().add("primary-button");
        cancelBtn.getStyleClass().add("secondary-button");

        saveBtn.setOnAction(e -> {
            String nom          = nomField.getText()   == null ? "" : nomField.getText().trim();
            String email        = emailField.getText() == null ? "" : emailField.getText().trim();
            String phone        = phoneField.getText() == null ? "" : phoneField.getText().trim();
            String localisation = govCombo.getEditor().getText() == null ? ""
                    : GovernorateUtils.normalize(govCombo.getEditor().getText());
            String password     = passField.getText()  == null ? "" : passField.getText();

            if (nom.isBlank() || email.isBlank() || phone.isBlank()) {
                errorLbl.setText("Le nom, l'email et le téléphone sont obligatoires.");
                return;
            }
            if (localisation.isBlank()) {
                errorLbl.setText("Veuillez sélectionner un gouvernorat valide.");
                return;
            }
            if (isNew && password.isBlank()) {
                errorLbl.setText("Le mot de passe est obligatoire pour un nouvel utilisateur.");
                return;
            }

            String e164 = PhoneNumberUtils.normalizeToE164(phone);
            if (e164.isBlank()) {
                errorLbl.setText("Numéro de téléphone invalide.");
                return;
            }

            try {
                if (isNew) {
                    if (utilisateurService.emailExists(email)) {
                        errorLbl.setText("Cet email est déjà utilisé.");
                        return;
                    }
                    if (utilisateurService.telephoneExists(e164)) {
                        errorLbl.setText("Ce numéro est déjà utilisé.");
                        return;
                    }
                    Utilisateur newUser = new Utilisateur(nom, email, e164, localisation, password);
                    utilisateurService.addEntity(newUser);
                    setFeedback("✅ Utilisateur ajouté avec succès.", false);
                } else {
                    if (utilisateurService.emailTakenByAnotherUser(target.getId(), email)) {
                        errorLbl.setText("Cet email est déjà utilisé par un autre compte.");
                        return;
                    }
                    if (utilisateurService.telephoneTakenByAnotherUser(target.getId(), e164)) {
                        errorLbl.setText("Ce numéro est déjà utilisé par un autre compte.");
                        return;
                    }
                    Utilisateur updated = new Utilisateur(nom, email, e164, localisation, password);
                    utilisateurService.updateEntity(target.getId(), updated);
                    setFeedback("✅ Utilisateur modifié avec succès.", false);
                }
                dialog.close();
                loadUsers();
            } catch (Exception ex) {
                errorLbl.setText("Erreur : " + ex.getMessage());
            }
        });

        cancelBtn.setOnAction(e -> dialog.close());

        // ── Layout ────────────────────────────────────────────────────────────
        VBox form = new VBox(14);
        form.setPadding(new Insets(28));
        form.getStyleClass().add("modal-card");
        form.setPrefWidth(420);

        Label title = new Label(isNew ? "➕ Ajouter un utilisateur" : "✏ Modifier l'utilisateur");
        title.getStyleClass().add("modal-title");

        HBox actions = new HBox(12, cancelBtn, saveBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);

        form.getChildren().addAll(
                title,
                buildFieldLabel("Nom complet"),   nomField,
                buildFieldLabel("Email"),          emailField,
                buildFieldLabel("Téléphone"),      phoneField,
                buildFieldLabel("Gouvernorat"),    govCombo,
                buildFieldLabel(isNew ? "Mot de passe" : "Nouveau mot de passe"), passField,
                errorLbl,
                actions
        );

        Scene scene = new Scene(form);
        scene.getStylesheets().add(
                getClass().getResource("/com/forestguard/styles/app.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    private void handleDelete(Utilisateur u) {
        if (adminUser != null && adminUser.getId() == u.getId()) {
            setFeedback("❌ Vous ne pouvez pas supprimer votre propre compte.", true);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer l'utilisateur");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer le compte de " + u.getNom()
                + " (" + u.getEmail() + ") ?\nCette action est irréversible.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                utilisateurService.deleteEntity(u);
                setFeedback("✅ Compte de " + u.getNom() + " supprimé.", false);
                loadUsers();
            } catch (Exception e) {
                setFeedback("❌ Erreur lors de la suppression : " + e.getMessage(), true);
            }
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @FXML
    private void handleBackToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/forestguard/views/dashboard.fxml"));
            Parent root = loader.load();
            DashboardController ctrl = loader.getController();
            ctrl.setUtilisateur(adminUser != null ? adminUser : Session.getCurrentUser());

            Stage stage = (Stage) tableView.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("ForestGuard - Dashboard");
            stage.setMaximized(true);
        } catch (IOException e) {
            setFeedback("❌ Impossible de revenir au dashboard.", true);
        }
    }

    @FXML
    private void handleLogout() {
        Session.clear();
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/com/forestguard/views/login.fxml"));
            Stage stage = (Stage) tableView.getScene().getWindow();
            stage.setScene(new Scene(root, 1200, 800));
            stage.setTitle("Connexion");
            stage.setMaximized(true);
        } catch (IOException e) {
            setFeedback("❌ Impossible de revenir à la connexion.", true);
        }
    }

    @FXML
    private void handleRefresh() {
        setFeedback("Rechargement...", false);
        loadUsers();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Label buildFieldLabel(String text) {
        Label lbl = new Label(text);
        lbl.getStyleClass().add("field-label");
        return lbl;
    }

    private void setFeedback(String message, boolean isError) {
        if (feedbackLabel == null) return;
        feedbackLabel.setText(message);
        feedbackLabel.setStyle(isError
                ? "-fx-text-fill: #ffcbc7; -fx-font-weight: 600;"
                : "-fx-text-fill: #7ef5ae; -fx-font-weight: 600;");
    }

    private static String formatPhone(String phone) {
        if (phone == null || phone.isBlank()) return "—";
        String local     = PhoneNumberUtils.toLocalDisplay(phone);
        String formatted = PhoneNumberUtils.formatDisplay(local);
        return "+216 " + formatted;
    }
}
