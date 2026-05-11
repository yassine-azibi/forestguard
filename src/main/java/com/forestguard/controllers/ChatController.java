package com.forestguard.controllers;

import com.forestguard.entities.Utilisateur;
import com.forestguard.utils.ChatService;
import com.forestguard.utils.ChatService.ChatException;
import com.forestguard.utils.Session;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Contrôleur du chatbot ForestGuard.
 *
 * <p>Gère l'interface de chat : affichage des bulles, envoi des messages,
 * indicateur de frappe, et sélection du niveau d'alerte.</p>
 *
 * <h2>Architecture</h2>
 * <ul>
 *   <li>Les appels API se font sur un thread daemon pour ne pas bloquer l'UI.</li>
 *   <li>Les mises à jour UI passent toujours par {@code Platform.runLater()}.</li>
 *   <li>{@link ChatService} maintient l'historique de conversation.</li>
 * </ul>
 */
public class ChatController {

    // ── Champs FXML ───────────────────────────────────────────────────────────

    @FXML private VBox       messagesContainer;
    @FXML private ScrollPane messagesScrollPane;
    @FXML private TextField  inputField;
    @FXML private Button     sendButton;
    @FXML private ComboBox<String> alertLevelCombo;
    @FXML private Label      locationLabel;
    @FXML private Button     clearButton;

    // ── État ──────────────────────────────────────────────────────────────────

    private final ChatService chatService = new ChatService();
    private Utilisateur utilisateur;

    /** Bulle "..." affichée pendant que le bot réfléchit. */
    private HBox typingIndicator;

    // ── Initialisation ────────────────────────────────────────────────────────

    @FXML
    private void initialize() {
        // Niveaux d'alerte disponibles
        alertLevelCombo.getItems().addAll("Faible", "Moyen", "Critique");
        alertLevelCombo.setValue("Faible");
        alertLevelCombo.valueProperty().addListener((obs, old, newVal) -> updateContext());

        // Charger l'utilisateur depuis la session si non injecté
        if (utilisateur == null) {
            utilisateur = Session.getCurrentUser();
        }
        updateContext();

        // Message de bienvenue
        appendBotMessage(
                "🌿 Bonjour ! Je suis ForestBot, votre assistant sécurité incendie.\n"
                + "Posez-moi vos questions sur les consignes de sécurité, "
                + "l'évacuation ou la prévention des incendies de forêt.");

        // Envoyer avec Entrée
        inputField.setOnAction(e -> handleSend());

        // Scroll automatique vers le bas à chaque nouveau message
        messagesContainer.heightProperty().addListener((obs, old, newH) ->
                Platform.runLater(() ->
                        messagesScrollPane.setVvalue(1.0)));
    }

    /**
     * Injecte l'utilisateur connecté pour personnaliser le contexte.
     * Appelé depuis {@link DashboardController}.
     */
    public void setUtilisateur(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
        updateContext();
    }

    // ── Envoi de message ──────────────────────────────────────────────────────

    @FXML
    private void handleSend() {
        String text = inputField.getText() == null ? "" : inputField.getText().trim();
        if (text.isBlank()) return;

        // Afficher le message utilisateur immédiatement
        appendUserMessage(text);
        inputField.clear();

        // Désactiver l'input pendant la réponse
        setInputEnabled(false);
        showTypingIndicator();

        // Appel API sur thread daemon
        Thread thread = new Thread(() -> {
            try {
                String reply = chatService.sendMessage(text);
                Platform.runLater(() -> {
                    hideTypingIndicator();
                    appendBotMessage(reply);
                    setInputEnabled(true);
                });
            } catch (ChatException e) {
                Platform.runLater(() -> {
                    hideTypingIndicator();
                    appendErrorMessage(e.getMessage());
                    setInputEnabled(true);
                });
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleClear() {
        messagesContainer.getChildren().clear();
        chatService.clearHistory();
        appendBotMessage(
                "🌿 Conversation réinitialisée. Comment puis-je vous aider ?");
    }

    // ── Bulles de messages ────────────────────────────────────────────────────

    /**
     * Ajoute une bulle de message utilisateur (alignée à droite, fond vert).
     */
    private void appendUserMessage(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(420);
        label.getStyleClass().add("chat-bubble-user");

        HBox row = new HBox(label);
        row.setAlignment(Pos.CENTER_RIGHT);
        row.setPadding(new Insets(4, 12, 4, 60));

        animateIn(row);
        messagesContainer.getChildren().add(row);
    }

    /**
     * Ajoute une bulle de réponse du bot (alignée à gauche, fond sombre).
     */
    private void appendBotMessage(String text) {
        Label icon  = new Label("🤖");
        icon.getStyleClass().add("chat-bot-icon");

        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(420);
        label.getStyleClass().add("chat-bubble-bot");

        HBox bubble = new HBox(8, icon, label);
        bubble.setAlignment(Pos.CENTER_LEFT);

        HBox row = new HBox(bubble);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(4, 60, 4, 12));

        animateIn(row);
        messagesContainer.getChildren().add(row);
    }

    /**
     * Ajoute une bulle d'erreur (fond rouge clair).
     */
    private void appendErrorMessage(String text) {
        Label label = new Label("⚠️ " + text);
        label.setWrapText(true);
        label.setMaxWidth(420);
        label.getStyleClass().add("chat-bubble-error");

        HBox row = new HBox(label);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(4, 60, 4, 12));

        messagesContainer.getChildren().add(row);
    }

    // ── Indicateur de frappe ──────────────────────────────────────────────────

    /** Affiche l'indicateur "..." pendant que le bot génère sa réponse. */
    private void showTypingIndicator() {
        Label dots = new Label("● ● ●");
        dots.getStyleClass().add("chat-typing-dots");

        // Animation de pulsation sur les points
        Timeline pulse = new Timeline(
                new KeyFrame(Duration.ZERO,       new KeyValue(dots.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(600), new KeyValue(dots.opacityProperty(), 0.3)),
                new KeyFrame(Duration.millis(1200),new KeyValue(dots.opacityProperty(), 1.0))
        );
        pulse.setCycleCount(Timeline.INDEFINITE);
        pulse.play();
        dots.setUserData(pulse); // stocker pour l'arrêter plus tard

        Label icon = new Label("🤖");
        icon.getStyleClass().add("chat-bot-icon");

        typingIndicator = new HBox(8, icon, dots);
        typingIndicator.setAlignment(Pos.CENTER_LEFT);
        typingIndicator.setPadding(new Insets(4, 60, 4, 12));

        messagesContainer.getChildren().add(typingIndicator);
    }

    /** Retire l'indicateur de frappe. */
    private void hideTypingIndicator() {
        if (typingIndicator != null) {
            // Arrêter l'animation
            typingIndicator.getChildren().stream()
                    .filter(n -> n.getUserData() instanceof Timeline)
                    .forEach(n -> ((Timeline) n.getUserData()).stop());
            messagesContainer.getChildren().remove(typingIndicator);
            typingIndicator = null;
        }
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    /** Met à jour le contexte utilisateur dans le ChatService. */
    private void updateContext() {
        String location = "";
        if (utilisateur != null && utilisateur.getLocalisation() != null) {
            location = utilisateur.getLocalisation();
        }
        if (locationLabel != null) {
            locationLabel.setText(location.isBlank() ? "Localisation inconnue" : "📍 " + location);
        }

        String alert = alertLevelCombo.getValue() != null
                ? alertLevelCombo.getValue() : "Faible";
        chatService.setUserContext(location, alert);
    }

    /** Active ou désactive le champ de saisie et le bouton d'envoi. */
    private void setInputEnabled(boolean enabled) {
        inputField.setDisable(!enabled);
        sendButton.setDisable(!enabled);
        if (enabled) {
            Platform.runLater(inputField::requestFocus);
        }
    }

    /** Animation d'apparition en fondu pour les bulles. */
    private static void animateIn(HBox row) {
        row.setOpacity(0);
        new Timeline(
                new KeyFrame(Duration.ZERO,       new KeyValue(row.opacityProperty(), 0.0)),
                new KeyFrame(Duration.millis(200), new KeyValue(row.opacityProperty(), 1.0))
        ).play();
    }
}
