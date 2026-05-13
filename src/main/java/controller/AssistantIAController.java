package controller;

import utils.GeminiService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;

import java.net.URL;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.function.Supplier;

public class AssistantIAController implements Initializable {

    @FXML private VBox       vboxMessages;
    @FXML private TextField  tfMessage;
    @FXML private ScrollPane scrollMessages;
    @FXML private HBox       hboxLoading;
    @FXML private Label      lblStatutIA;

    private GeminiService geminiService;
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        geminiService = new GeminiService("John Martinez");

        ajouterMessageIA(
                "👋 Bonjour ! Je suis votre assistant IA ForestGuard.\n\n" +
                        "Je peux vous aider avec :\n" +
                        "• 🔥 Analyse du risque d'incendie actuel\n" +
                        "• 🚁 Choix des équipements selon l'alerte\n" +
                        "• 🛡️ Protocoles de sécurité\n" +
                        "• 📊 Analyse de vos interventions\n\n" +
                        "Utilisez les boutons rapides ou posez votre question !");
    }

    @FXML
    private void handleEnvoyer() {
        String message = tfMessage.getText().trim();
        if (message.isEmpty()) return;
        ajouterMessageAgent(message);
        tfMessage.clear();
        envoyerAGemini(message);
    }

    @FXML
    private void handleRisqueActuel() {
        ajouterMessageAgent("🔥 Analyse du risque actuel");
        envoyerAGeminiDirecte(() -> geminiService.analyseRisqueActuel());
    }

    @FXML
    private void handleEquipements() {
        ajouterMessageAgent("🚁 Quels équipements mobiliser ?");
        envoyerAGemini(
                "Quels équipements dois-je prioritairement mobiliser " +
                        "pour les interventions de la région Tunisie en ce moment ?");
    }

    @FXML
    private void handleProtocoles() {
        ajouterMessageAgent("🛡️ Protocoles de sécurité");
        envoyerAGemini(
                "Donne-moi les protocoles de sécurité essentiels " +
                        "pour une intervention forestière en Tunisie.");
    }

    @FXML
    private void handleMesStats() {
        ajouterMessageAgent("📊 Analyse de mes interventions");
        envoyerAGemini(
                "Analyse mes statistiques d'interventions et " +
                        "donne-moi des conseils pour améliorer mes performances.");
    }

    private void envoyerAGemini(String question) {
        afficherChargement(true);
        new Thread(() -> {
            String reponse = geminiService.envoyerMessage(question);
            Platform.runLater(() -> {
                afficherChargement(false);
                ajouterMessageIA(reponse);
            });
        }).start();
    }

    private void envoyerAGeminiDirecte(Supplier<String> appel) {
        afficherChargement(true);
        new Thread(() -> {
            String reponse = appel.get();
            Platform.runLater(() -> {
                afficherChargement(false);
                ajouterMessageIA(reponse);
            });
        }).start();
    }

    // ── Message agent (droite, vert) ──────────────────────────────────────
    private void ajouterMessageAgent(String texte) {
        String heure = LocalTime.now().format(TIME_FMT);

        Label lblMsg = new Label(texte);
        lblMsg.setWrapText(true);
        lblMsg.setMaxWidth(420);
        lblMsg.setStyle(
                "-fx-background-color: #16a34a;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 13;" +
                        "-fx-padding: 10 14;" +
                        "-fx-background-radius: 18 18 4 18;");

        Label lblHeure = new Label(heure);
        lblHeure.setStyle("-fx-font-size: 9; -fx-text-fill: #64748b;");

        VBox bubble = new VBox(4, lblMsg, lblHeure);
        bubble.setAlignment(Pos.CENTER_RIGHT);

        HBox row = new HBox(bubble);
        row.setAlignment(Pos.CENTER_RIGHT);
        vboxMessages.getChildren().add(row);
        scrollToBottom();
    }

    // ── Message IA (gauche, violet) ───────────────────────────────────────
    private void ajouterMessageIA(String texte) {
        String heure = LocalTime.now().format(TIME_FMT);

        Label badge = new Label("🤖");
        badge.setStyle(
                "-fx-background-color: #7c3aed;" +
                        "-fx-background-radius: 50;" +
                        "-fx-padding: 6 8;" +
                        "-fx-font-size: 14;");

        Label lblMsg = new Label(texte);
        lblMsg.setWrapText(true);
        lblMsg.setMaxWidth(420);
        lblMsg.setTextAlignment(TextAlignment.LEFT);
        lblMsg.setStyle(
                "-fx-background-color: rgba(255,255,255,0.08);" +
                        "-fx-text-fill: #e2e8f0;" +
                        "-fx-font-size: 13;" +
                        "-fx-padding: 10 14;" +
                        "-fx-background-radius: 4 18 18 18;" +
                        "-fx-border-color: rgba(124,58,237,0.3);" +
                        "-fx-border-radius: 4 18 18 18;" +
                        "-fx-border-width: 1;");

        Label lblHeure = new Label("Assistant IA • " + heure);
        lblHeure.setStyle("-fx-font-size: 9; -fx-text-fill: #64748b;");

        VBox bubble = new VBox(4, lblMsg, lblHeure);

        HBox row = new HBox(10, badge, bubble);
        row.setAlignment(Pos.CENTER_LEFT);
        vboxMessages.getChildren().add(row);
        scrollToBottom();
    }

    private void afficherChargement(boolean visible) {
        hboxLoading.setVisible(visible);
        hboxLoading.setManaged(visible);
    }

    private void scrollToBottom() {
        Platform.runLater(() ->
                scrollMessages.setVvalue(scrollMessages.getVmax()));
    }
}
