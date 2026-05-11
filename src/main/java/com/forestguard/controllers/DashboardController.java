package com.forestguard.controllers;

import com.forestguard.entities.Utilisateur;
import com.forestguard.utils.Session;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DashboardController {

    // ── FXML fields ───────────────────────────────────────────────────────────

    @FXML private Label     greetingLabel;
    @FXML private Label     nameLabel;
    @FXML private Label     emailLabel;
    @FXML private ImageView backgroundImage;
    @FXML private Label     navAvatarLabel;

    // Ticker bar
    @FXML private Label tickerContent;
    @FXML private HBox  tickerBar;
    @FXML private Label clockLabel;

    /** Running scroll animation — restarted after text update. */
    private javafx.animation.SequentialTransition tickerLoop;

    // ── State ─────────────────────────────────────────────────────────────────

    private Utilisateur utilisateur;

    // ── HTTP client ───────────────────────────────────────────────────────────

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(8))
            .build();

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    public void setUtilisateur(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
        if (greetingLabel != null) populateHeader();
    }

    @FXML
    private void initialize() {
        Platform.runLater(() -> {
            if (backgroundImage != null && greetingLabel.getScene() != null) {
                backgroundImage.fitWidthProperty().bind(
                        greetingLabel.getScene().getWindow().widthProperty());
                backgroundImage.fitHeightProperty().bind(
                        greetingLabel.getScene().getWindow().heightProperty());
            }
        });

        if (utilisateur == null) utilisateur = Session.getCurrentUser();
        if (utilisateur != null) populateHeader();

        // Start ticker with placeholder, update once weather loads
        Platform.runLater(() -> startTicker("🌿 ForestGuard — Chargement des données météo..."));
        loadWeatherForTickerAsync();

        // Live clock — updates every second
        javafx.animation.Timeline clock = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(Duration.seconds(1), e -> updateClock()));
        clock.setCycleCount(javafx.animation.Animation.INDEFINITE);
        clock.play();
        updateClock(); // show immediately on load
    }

    /** Updates the clock label with current time and date. */
    private void updateClock() {
        if (clockLabel == null) return;
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        String time = String.format("%02d:%02d:%02d",
                now.getHour(), now.getMinute(), now.getSecond());
        String date = String.format("%02d/%02d/%d",
                now.getDayOfMonth(), now.getMonthValue(), now.getYear());
        clockLabel.setText(time + "\n" + date);
        clockLabel.setAlignment(javafx.geometry.Pos.CENTER);
        clockLabel.setWrapText(true);
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @FXML
    private void handleOpenChat() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/forestguard/views/chat.fxml"));
            Parent root = loader.load();

            ChatController chatController = loader.getController();
            chatController.setUtilisateur(utilisateur != null ? utilisateur : Session.getCurrentUser());

            javafx.stage.Stage chatStage = new javafx.stage.Stage();
            chatStage.initModality(javafx.stage.Modality.NONE);
            chatStage.initOwner(greetingLabel.getScene().getWindow());
            chatStage.setTitle("ForestBot — Assistant sécurité incendie");
            chatStage.setScene(new javafx.scene.Scene(root));
            chatStage.setResizable(true);
            chatStage.setMinWidth(420);
            chatStage.setMinHeight(500);
            chatStage.show();
        } catch (IOException e) {
            showError("Erreur", "Impossible d'ouvrir le chatbot : " + e.getMessage());
        }
    }

    @FXML
    private void handleOpenAlerts() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/forestguard/views/alerts.fxml"));
            Parent root = loader.load();
            AlertsController controller = loader.getController();
            controller.setUtilisateur(utilisateur != null ? utilisateur : Session.getCurrentUser());

            Stage stage = (Stage) greetingLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Alertes Incendie");
            stage.setMaximized(true);
        } catch (IOException exception) {
            showError("Erreur", "Impossible d'ouvrir la page Alertes.");
        }
    }

    @FXML
    private void handleOpenSignal() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/forestguard/views/signal.fxml"));
            Parent root = loader.load();
            SignalController controller = loader.getController();
            controller.setUtilisateur(utilisateur != null ? utilisateur : Session.getCurrentUser());

            Stage stage = (Stage) greetingLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Signaler un incendie");
            stage.setMaximized(true);
        } catch (IOException exception) {
            showError("Erreur", "Impossible d'ouvrir la page de declaration d'incendie.");
        }
    }

    @FXML
    private void handleOpenProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/forestguard/views/profile.fxml"));
            Parent root = loader.load();
            ProfileController controller = loader.getController();
            controller.setUtilisateur(utilisateur != null ? utilisateur : Session.getCurrentUser());

            Stage stage = (Stage) greetingLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Profil utilisateur");
            stage.setMaximized(true);
        } catch (IOException exception) {
            showError("Erreur", "Impossible d'ouvrir le profil.");
        }
    }

    @FXML
    private void handleOpenAdminUtilisateurs() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/forestguard/views/admin_utilisateur.fxml"));
            Parent root = loader.load();
            AdminUtilisateurController controller = loader.getController();
            controller.setAdminUser(utilisateur != null ? utilisateur : Session.getCurrentUser());

            Stage stage = (Stage) greetingLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("ForestGuard - Gestion des utilisateurs");
            stage.setMaximized(true);
        } catch (IOException exception) {
            showError("Erreur", "Impossible d'ouvrir le panneau d'administration.");
        }
    }

    @FXML
    private void handleOpenEvenements() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/forestguard/views/evenement.fxml"));
            Parent root = loader.load();
            EvenementController controller = loader.getController();
            controller.setUtilisateur(utilisateur != null ? utilisateur : Session.getCurrentUser());

            Stage stage = (Stage) greetingLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Événements Forestiers");
            stage.setMaximized(true);
        } catch (IOException exception) {
            showError("Erreur", "Impossible d'ouvrir la page Événements.");
        }
    }

    @FXML
    private void handleLogout() {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Retour a l'accueil");
        confirmation.setHeaderText(null);
        confirmation.setContentText("Voulez-vous vraiment revenir a l'accueil ?");

        Optional<javafx.scene.control.ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
            Session.clear();
            try {
                Parent root = FXMLLoader.load(
                        getClass().getResource("/com/forestguard/views/login.fxml"));
                Stage stage = (Stage) greetingLabel.getScene().getWindow();
                stage.setScene(new Scene(root, 1200, 800));
                stage.setTitle("Connexion");
                stage.setMaximized(true);
            } catch (IOException exception) {
                showError("Erreur", "Impossible de revenir a la page de connexion.");
            }
        }
    }

    // ── Ticker ────────────────────────────────────────────────────────────────

    /**
     * Starts the ticker: scrolls {@code text} across {@code tickerBar} once,
     * pauses 6 seconds, then repeats indefinitely.
     *
     * <p>Uses {@link javafx.animation.SequentialTransition} chaining a
     * {@link javafx.animation.TranslateTransition} (scroll) with a
     * {@link javafx.animation.PauseTransition} (6 s gap), set to
     * {@code INDEFINITE} cycles — clean, no manual frame loop needed.</p>
     */
    private void startTicker(String text) {
        if (tickerContent == null || tickerBar == null) return;

        // Stop any existing animation
        if (tickerLoop != null) {
            tickerLoop.stop();
            tickerLoop = null;
        }

        // Prevent Label from truncating with "..."
        tickerContent.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        tickerContent.setMaxWidth(Double.MAX_VALUE);
        tickerContent.setWrapText(false);
        tickerContent.setText(text);

        // Double runLater forces JavaFX to finish rendering before measuring
        Platform.runLater(() -> Platform.runLater(() -> {
            tickerContent.applyCss();
            tickerContent.layout();
            tickerBar.applyCss();
            tickerBar.layout();

            double paneWidth  = tickerBar.getWidth() > 0 ? tickerBar.getWidth() : 1200;
            double labelWidth = tickerContent.prefWidth(-1);
            if (labelWidth <= 0) labelWidth = tickerContent.getBoundsInLocal().getWidth();
            if (labelWidth <= 0) labelWidth = 8000; // fallback

            final double fw = paneWidth;
            final double lw = labelWidth;

            // Position label off-screen to the right before starting
            tickerContent.setTranslateX(fw);

            // Step 1 — slow scroll from right to left
            javafx.animation.TranslateTransition slide =
                    new javafx.animation.TranslateTransition(
                            Duration.seconds(lw / 80.0), tickerContent);
            slide.setFromX(fw);
            slide.setToX(-lw);
            slide.setInterpolator(javafx.animation.Interpolator.LINEAR);
            slide.setCycleCount(1);

            // Step 2 — pause 6 seconds at end
            javafx.animation.PauseTransition pause =
                    new javafx.animation.PauseTransition(Duration.seconds(6));

            // Reset position BEFORE next cycle starts
            pause.setOnFinished(e -> tickerContent.setTranslateX(fw));

            // Chain scroll → pause, repeat forever
            tickerLoop = new javafx.animation.SequentialTransition(slide, pause);
            tickerLoop.setCycleCount(javafx.animation.Animation.INDEFINITE);
            tickerLoop.play();

            // Hover pauses scroll; exit resumes
            tickerContent.setOnMouseEntered(e -> tickerLoop.pause());
            tickerContent.setOnMouseExited(e  -> tickerLoop.play());
        }));
    }

    // ── Weather fetch (ticker only) ───────────────────────────────────────────

    /**
     * Spawns a named daemon thread that builds the full ticker text
     * (weather + emergency numbers + tips) and hands it to startTicker().
     */
    private void loadWeatherForTickerAsync() {
        Thread t = new Thread(() -> {
            String tickerText = buildTickerText();
            Platform.runLater(() -> startTicker(tickerText));
        }, "ticker-weather");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Fetches live weather from ip-api.com + open-meteo.com and assembles
     * the complete ticker string. Falls back to a static message on any error.
     */
    private String buildTickerText() {
        String weatherPart;
        try {
            // Step 1 — get coordinates + city (try 3 APIs as fallback)
            String ipJson = null;
            String[] ipApis = {
                    "http://ip-api.com/json/?fields=lat,lon,city&lang=fr",
                    "https://ipwho.is/",
                    "https://freeipapi.com/api/json"
            };
            for (String api : ipApis) {
                try {
                    HttpRequest ipReq = HttpRequest.newBuilder()
                            .uri(URI.create(api))
                            .timeout(java.time.Duration.ofSeconds(5))
                            .header("User-Agent", "ForestGuard/1.0")
                            .build();
                    String resp = HTTP.send(ipReq, HttpResponse.BodyHandlers.ofString()).body();
                    if (resp != null && resp.contains("lat")) { ipJson = resp; break; }
                } catch (Exception ignored) {}
            }
            // Default to Tunis coords if all APIs fail
            double lat  = (ipJson != null) ? extractDouble(ipJson, "lat")  : 36.8065;
            double lon  = (ipJson != null) ? extractDouble(ipJson, "lon")  : 10.1815;
            String city = (ipJson != null) ? extractStr(ipJson, "city")    : "Tunis";
            if (lat == 0.0 && lon == 0.0) { lat = 36.8065; lon = 10.1815; city = "Tunis"; }
            if (city == null || city.isEmpty()) city = "Tunis";

            // Step 2 — get current weather
            HttpRequest wReq = HttpRequest.newBuilder()
                    .uri(URI.create(
                            "https://api.open-meteo.com/v1/forecast"
                                    + "?latitude=" + lat + "&longitude=" + lon
                                    + "&current=temperature_2m,windspeed_10m,weathercode"
                                    + "&timezone=auto"))
                    .timeout(java.time.Duration.ofSeconds(8))
                    .build();
            String wJson = HTTP.send(wReq, HttpResponse.BodyHandlers.ofString()).body();

            // Parse current block
            int ci = wJson.indexOf("\"current\"");
            String currentBlock = ci >= 0 ? wJson.substring(ci) : wJson;

            double temp = extractDouble(currentBlock, "temperature_2m");
            double wind = extractDouble(currentBlock, "windspeed_10m");
            int    code = (int) extractDouble(currentBlock, "weathercode");

            String condition = code == 0  ? "☀️ Ciel dégagé"
                    : code <= 3  ? "⛅ Nuageux"
                    : code <= 67 ? "🌧️ Pluie"
                    : code <= 77 ? "❄️ Neige"
                    :              "⛈️ Orageux";

            String risk = (temp > 35 && wind > 30) ? "⚠️ Risque incendie ÉLEVÉ !"
                    : temp > 28                 ? "⚠️ Risque incendie modéré"
                    :                             "✅ Risque incendie faible";

            weatherPart = "🌡️ " + city + " : " + temp + "°C  |  "
                    + "💨 Vent : " + wind + " km/h  |  "
                    + condition + "  |  "
                    + risk + "  |  ";

        } catch (Exception e) {
            weatherPart = "🌐 Météo indisponible  |  ";
        }

        String emergency =
                "📞 Protection Civile : 198  |  "
                        + "🌲 Forêts Tunisie : 1828  |  "
                        + "🚑 SAMU : 190  |  ";

        String tips =
                "🚫 Ne jamais allumer de feu en forêt en été  |  "
                        + "🌳 Ne coupez pas les arbres sans autorisation  |  "
                        + "🦎 Respectez la faune et la flore sauvage  |  "
                        + "🗑️ Ne laissez aucun déchet en forêt  |  "
                        + "⛺ Campez uniquement dans les zones autorisées  |  "
                        + "🔥 En cas de fumée, éloignez-vous et appelez le 198  |  "
                        + "🐦 Ne dérangez pas les nids d'oiseaux  |  "
                        + "🚶 Restez sur les sentiers balisés  |  "
                        + "🌿 Ne cueillez pas les plantes protégées  |  "
                        + "🔇 Gardez le silence pour respecter la nature  |  "
                        + "🌞 Par temps chaud, évitez la forêt l'après-midi  |  "
                        + "🐜 Ne détruisez pas les insectes, ils protègent la forêt  |  "
                        + "💡 Signalez tout comportement suspect en forêt  |  "
                        + "🧯 Gardez un extincteur dans votre voiture en été  |  "
                        + "🌱 Plantez des arbres pour compenser votre empreinte carbone  |  "
                        + "🚿 Économisez l'eau près des zones forestières  |  "
                        + "🐗 Ne nourrissez pas les animaux sauvages en forêt  |  "
                        + "🪓 Signalez toute coupe illégale d'arbres au 1828  |  "
                        + "🐝 Protégez les abeilles, elles pollinisent la forêt  |  "
                        + "🌍 La forêt absorbe le CO2, protégez-la  |  "
                        + "🎒 Emportez vos déchets en quittant la forêt  |  "
                        + "🌺 Ne cueillez pas les fleurs sauvages protégées  |  "
                        + "🐍 Attention aux serpents en été, portez des chaussures fermées  |  "
                        + "🌬️ Par vent fort, le risque incendie est maximal  |  "
                        + "📸 Photographiez la nature sans la déranger  |  "
                        + "🦊 La forêt est un habitat naturel, respectez-la  |  "
                        + "🕯️ Jamais de bougies ou lanternes en forêt  |  "
                        + "🌙 Évitez la forêt la nuit, les animaux y sont actifs  |  "
                        + "🔭 Observez la forêt de loin avec des jumelles  |  "
                        + "🐠 Protégez les ruisseaux forestiers, ne les polluez pas  |  ";

        return weatherPart + emergency + tips;
    }

    /** Extracts a numeric value from a JSON string without an external library. */
    private static double extractDouble(String json, String key) {
        Matcher m = Pattern.compile(
                        "\"" + Pattern.quote(key) + "\"\\s*:\\s*([\\-0-9.]+)")
                .matcher(json);
        return m.find() ? Double.parseDouble(m.group(1)) : 0.0;
    }

    /** Extracts a string value from a JSON string without an external library. */
    private static String extractStr(String json, String key) {
        Matcher m = Pattern.compile(
                        "\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]+)\"")
                .matcher(json);
        return m.find() ? m.group(1) : "";
    }

    // ── HTTP + JSON helpers ───────────────────────────────────────────────────

    private static String httpGet(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(java.time.Duration.ofSeconds(8))
                .header("User-Agent", "ForestGuard/1.0")
                .GET().build();
        return HTTP.send(req, HttpResponse.BodyHandlers.ofString()).body();
    }

    private static String extractString(String json, String field) {
        if (json == null) return null;
        Matcher m = Pattern.compile(
                        "\"" + Pattern.quote(field) + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"")
                .matcher(json);
        return m.find() ? m.group(1) : null;
    }

    private static String extractNumber(String json, String field) {
        if (json == null) return null;
        Matcher m = Pattern.compile(
                        "\"" + Pattern.quote(field) + "\"\\s*:\\s*(-?[0-9]+(?:\\.[0-9]+)?)")
                .matcher(json);
        return m.find() ? m.group(1) : null;
    }

    private static String extractObject(String json, String field) {
        if (json == null) return null;
        Matcher sm = Pattern.compile(
                "\"" + Pattern.quote(field) + "\"\\s*:\\s*\\{").matcher(json);
        if (!sm.find()) return null;
        int start = sm.end() - 1, depth = 0;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') depth++;
            else if (c == '}' && --depth == 0) return json.substring(start, i + 1);
        }
        return null;
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private void populateHeader() {
        if (utilisateur == null) return;
        String fullName = utilisateur.getNom() == null || utilisateur.getNom().isBlank()
                ? "Utilisateur" : utilisateur.getNom();
        if (greetingLabel  != null) greetingLabel.setText("Bienvenue, " + fullName);
        if (nameLabel      != null) nameLabel.setText(fullName);
        if (emailLabel     != null) emailLabel.setText(utilisateur.getEmail());

        // Avatar initials — first 2 letters of name, uppercase
        if (navAvatarLabel != null) {
            String initials = fullName.length() >= 2
                    ? fullName.substring(0, 2).toUpperCase()
                    : fullName.toUpperCase();
            navAvatarLabel.setText(initials);
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}