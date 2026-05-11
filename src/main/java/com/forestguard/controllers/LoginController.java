package com.forestguard.controllers;

import com.forestguard.entities.Utilisateur;
import com.forestguard.services.UtilisateurService;
import com.forestguard.utils.Session;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML private StackPane rootPane;
    @FXML private ImageView bgImage;
    @FXML private Rectangle overlayRect;
    @FXML private Rectangle vignetteRect;
    @FXML private Canvas particleCanvas;
    @FXML private HBox mainHBox;
    @FXML private Label flameLabel;
    @FXML private Label titleLabel;
    @FXML private Rectangle separatorRect;
    @FXML private Label typingLabel;
    @FXML private Label subtitleLabel;
    @FXML private HBox badgesBox;
    @FXML private VBox badge1, badge2, badge3, badge4;
    @FXML private Rectangle dividerRect;
    @FXML private VBox formCard;
    @FXML private TextField emailField;
    @FXML private PasswordField mdpField;
    @FXML private Label errorLabel;
    @FXML private Button loginBtn;
    @FXML private Label forgotPasswordLink;
    @FXML private Label registerLink;

    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();
    private AnimationTimer particleTimer;

    private static final String TYPING_TEXT = "Protéger les forêts,\nsauver des vies.";

    private static final String FIELD_NORMAL =
            "-fx-background-color: rgba(6,28,6,0.70);" +
                    "-fx-background-radius: 14; -fx-border-color: rgba(76,175,80,0.38);" +
                    "-fx-border-radius: 14; -fx-border-width: 1.5;" +
                    "-fx-text-fill: #e8fce8; -fx-prompt-text-fill: rgba(255,255,255,0.28);" +
                    "-fx-font-size: 13.5px; -fx-padding: 13 18; -fx-font-family: 'Georgia';";

    private static final String FIELD_FOCUS =
            "-fx-background-color: rgba(10,40,10,0.80);" +
                    "-fx-background-radius: 14; -fx-border-color: rgba(100,210,100,0.75);" +
                    "-fx-border-radius: 14; -fx-border-width: 2;" +
                    "-fx-text-fill: #e8fce8; -fx-prompt-text-fill: rgba(255,255,255,0.28);" +
                    "-fx-font-size: 13.5px; -fx-padding: 13 18; -fx-font-family: 'Georgia';" +
                    "-fx-effect: dropshadow(gaussian, rgba(76,210,80,0.35), 12, 0, 0, 0);";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        particleCanvas.widthProperty().bind(rootPane.widthProperty());
        particleCanvas.heightProperty().bind(rootPane.heightProperty());
        bgImage.fitWidthProperty().bind(rootPane.widthProperty());
        bgImage.fitHeightProperty().bind(rootPane.heightProperty());

        initParticles();
        playMasterEntrance();
        setupInteractions();
    }

    // ══════════════════════════════════════════════════
    //  MASTER ENTRANCE — séquence cinématique
    // ══════════════════════════════════════════════════
    private void playMasterEntrance() {

        FadeTransition bgFade = new FadeTransition(Duration.millis(1600), bgImage);
        bgFade.setFromValue(0); bgFade.setToValue(1);
        FadeTransition overlayFade = new FadeTransition(Duration.millis(1600), overlayRect);
        overlayFade.setFromValue(0); overlayFade.setToValue(1);
        FadeTransition vignetteFade = new FadeTransition(Duration.millis(2000), vignetteRect);
        vignetteFade.setFromValue(0); vignetteFade.setToValue(1);
        vignetteFade.setDelay(Duration.millis(400));
        new ParallelTransition(bgFade, overlayFade, vignetteFade).play();

        mainHBox.setTranslateY(60);
        FadeTransition mainFade = new FadeTransition(Duration.millis(1100), mainHBox);
        mainFade.setFromValue(0); mainFade.setToValue(1);
        mainFade.setDelay(Duration.millis(800));
        TranslateTransition mainSlide = new TranslateTransition(Duration.millis(1100), mainHBox);
        mainSlide.setFromY(60); mainSlide.setToY(0);
        mainSlide.setDelay(Duration.millis(800));
        mainSlide.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(mainFade, mainSlide).play();

        FadeTransition titleFade = new FadeTransition(Duration.millis(800), titleLabel);
        titleFade.setFromValue(0); titleFade.setToValue(1);
        titleFade.setDelay(Duration.millis(1000));
        titleFade.play();

        Timeline sepGrow = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(separatorRect.widthProperty(), 0)),
                new KeyFrame(Duration.millis(1000),
                        new KeyValue(separatorRect.widthProperty(), 420, Interpolator.EASE_OUT))
        );
        sepGrow.setDelay(Duration.millis(1200));
        sepGrow.play();

        PauseTransition phase5 = new PauseTransition(Duration.millis(1500));
        phase5.setOnFinished(e -> typeTextOnce(TYPING_TEXT, () -> {
            subtitleLabel.setText(
                    "Signalez un incendie, consultez les alertes\net contribuez à protéger nos forêts.");
            FadeTransition sf = new FadeTransition(Duration.millis(800), subtitleLabel);
            sf.setFromValue(0); sf.setToValue(1); sf.play();
        }));
        phase5.play();

        Timeline divGrow = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(dividerRect.heightProperty(), 0),
                        new KeyValue(dividerRect.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(1000),
                        new KeyValue(dividerRect.heightProperty(), 560, Interpolator.EASE_OUT),
                        new KeyValue(dividerRect.opacityProperty(), 0.5))
        );
        divGrow.setDelay(Duration.millis(1300));
        divGrow.play();

        ScaleTransition cardIn = new ScaleTransition(Duration.millis(900), formCard);
        cardIn.setFromX(0.75); cardIn.setToX(1.0);
        cardIn.setFromY(0.75); cardIn.setToY(1.0);
        cardIn.setDelay(Duration.millis(1100));
        cardIn.setInterpolator(Interpolator.EASE_OUT);
        cardIn.play();

        PauseTransition phase8 = new PauseTransition(Duration.millis(2400));
        phase8.setOnFinished(e -> animateBadgesCascade());
        phase8.play();

        startFlameAnimation();

        PauseTransition cardFloatPause = new PauseTransition(Duration.millis(2100));
        cardFloatPause.setOnFinished(e -> {
            TranslateTransition cardFloat = new TranslateTransition(Duration.millis(4000), formCard);
            cardFloat.setFromY(0); cardFloat.setToY(-12);
            cardFloat.setAutoReverse(true);
            cardFloat.setCycleCount(Animation.INDEFINITE);
            cardFloat.setInterpolator(Interpolator.EASE_BOTH);
            cardFloat.play();
        });
        cardFloatPause.play();

        PauseTransition glowPause = new PauseTransition(Duration.millis(2700));
        glowPause.setOnFinished(e -> startCardGlowPulse());
        glowPause.play();
    }

    // ══════════════════════════════════════════════════
    //  FLAMME
    // ══════════════════════════════════════════════════
    private void startFlameAnimation() {
        Timeline flamePulse = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(flameLabel.scaleXProperty(), 1.0),
                        new KeyValue(flameLabel.scaleYProperty(), 1.0),
                        new KeyValue(flameLabel.rotateProperty(), -4.0)),
                new KeyFrame(Duration.millis(450),
                        new KeyValue(flameLabel.scaleXProperty(), 1.20),
                        new KeyValue(flameLabel.scaleYProperty(), 1.25),
                        new KeyValue(flameLabel.rotateProperty(), 4.0)),
                new KeyFrame(Duration.millis(900),
                        new KeyValue(flameLabel.scaleXProperty(), 1.0),
                        new KeyValue(flameLabel.scaleYProperty(), 1.0),
                        new KeyValue(flameLabel.rotateProperty(), -4.0))
        );
        flamePulse.setCycleCount(Animation.INDEFINITE);
        flamePulse.setDelay(Duration.millis(1500));
        flamePulse.play();

        String[] flameStyles = {
                "-fx-font-size: 80px;" +
                        "-fx-effect: dropshadow(gaussian, #ffaa00, 30, 0.75, 0, 0)," +
                        "dropshadow(gaussian, #ff6600, 15, 0.55, 0, 3)," +
                        "dropshadow(gaussian, #cc2200, 6,  0.35, 0, 5);",
                "-fx-font-size: 80px;" +
                        "-fx-effect: dropshadow(gaussian, #ff7700, 34, 0.80, 0, 0)," +
                        "dropshadow(gaussian, #ff4400, 16, 0.60, 0, 3)," +
                        "dropshadow(gaussian, #bb1100, 7,  0.40, 0, 5);",
                "-fx-font-size: 80px;" +
                        "-fx-effect: dropshadow(gaussian, #ff5500, 28, 0.72, 0, 0)," +
                        "dropshadow(gaussian, #ee2200, 14, 0.52, 0, 4)," +
                        "dropshadow(gaussian, #990000, 6,  0.30, 0, 6);",
                "-fx-font-size: 80px;" +
                        "-fx-effect: dropshadow(gaussian, #ff3300, 32, 0.78, 0, 0)," +
                        "dropshadow(gaussian, #cc1100, 15, 0.56, 0, 4)," +
                        "dropshadow(gaussian, #880000, 7,  0.35, 0, 6);",
                "-fx-font-size: 80px;" +
                        "-fx-effect: dropshadow(gaussian, #ff8800, 26, 0.68, 0, 0)," +
                        "dropshadow(gaussian, #ff5500, 13, 0.50, 0, 3)," +
                        "dropshadow(gaussian, #aa2200, 6,  0.32, 0, 5);"
        };

        final int[] fi = {0};
        Timeline flameColor = new Timeline(new KeyFrame(Duration.millis(280), e -> {
            flameLabel.setStyle(flameStyles[fi[0] % flameStyles.length]);
            fi[0]++;
        }));
        flameColor.setCycleCount(Animation.INDEFINITE);
        flameColor.setDelay(Duration.millis(1500));
        flameColor.play();
    }

    // ══════════════════════════════════════════════════
    //  TYPING
    // ══════════════════════════════════════════════════
    private void typeTextOnce(String text, Runnable onFinished) {
        final int[] index = {0};
        Timeline tl = new Timeline(new KeyFrame(Duration.millis(52), e -> {
            if (index[0] <= text.length()) {
                typingLabel.setText(text.substring(0, index[0]) +
                        (index[0] < text.length() ? "▌" : ""));
                index[0]++;
            }
        }));
        tl.setCycleCount(text.length() + 1);
        tl.setOnFinished(e -> {
            typingLabel.setText(text);
            if (onFinished != null) onFinished.run();
        });
        tl.play();
    }

    // ══════════════════════════════════════════════════
    //  BADGES CASCADE
    // ══════════════════════════════════════════════════
    private void animateBadgesCascade() {
        badgesBox.setOpacity(1);
        VBox[] badges = {badge1, badge2, badge3, badge4};
        for (int i = 0; i < badges.length; i++) {
            VBox b = badges[i];
            b.setOpacity(0); b.setTranslateY(25);
            FadeTransition ft = new FadeTransition(Duration.millis(550), b);
            ft.setFromValue(0); ft.setToValue(1);
            ft.setDelay(Duration.millis(i * 130));
            TranslateTransition tt = new TranslateTransition(Duration.millis(550), b);
            tt.setFromY(25); tt.setToY(0);
            tt.setDelay(Duration.millis(i * 130));
            tt.setInterpolator(Interpolator.EASE_OUT);
            new ParallelTransition(ft, tt).play();
            setupBadgeHover(b);
        }
    }

    // ══════════════════════════════════════════════════
    //  CARD GLOW PULSE
    // ══════════════════════════════════════════════════
    private void startCardGlowPulse() {
        Timeline glow = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(formCard.opacityProperty(), 0.96)),
                new KeyFrame(Duration.millis(2500),
                        new KeyValue(formCard.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(5000),
                        new KeyValue(formCard.opacityProperty(), 0.96))
        );
        glow.setCycleCount(Animation.INDEFINITE);
        glow.play();
    }

    // ══════════════════════════════════════════════════
    //  PARTICLES
    // ══════════════════════════════════════════════════
    private void initParticles() {
        for (int i = 0; i < 110; i++) {
            particles.add(createParticle(
                    random.nextDouble() * 1400,
                    random.nextDouble() * 800));
        }
        particleTimer = new AnimationTimer() {
            @Override public void handle(long now) { drawParticles(); }
        };
        particleTimer.start();
    }

    private Particle createParticle(double x, double y) {
        Particle p = new Particle();
        p.x = x; p.y = y;
        p.speed = random.nextDouble() * 1.3 + 0.3;
        p.angle = -Math.PI / 2 + (random.nextDouble() - 0.5) * 0.9;
        p.life = random.nextDouble() * 0.75 + 0.25;
        p.maxLife = p.life;
        p.type = random.nextInt(4);
        p.size = random.nextDouble() * 2.8 + 0.7;
        return p;
    }

    private void drawParticles() {
        double w = particleCanvas.getWidth();
        double h = particleCanvas.getHeight();
        if (w == 0 || h == 0) return;
        GraphicsContext gc = particleCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        for (Particle p : particles) {
            p.x += Math.cos(p.angle) * p.speed;
            p.y += Math.sin(p.angle) * p.speed;
            p.angle += (random.nextDouble() - 0.5) * 0.055;
            p.life -= 0.0028;

            if (p.life <= 0 || p.y < -20 || p.x < -20 || p.x > w + 20) {
                p.x = random.nextDouble() * w;
                p.y = h + 5;
                p.life = random.nextDouble() * 0.75 + 0.25;
                p.maxLife = p.life;
                p.speed = random.nextDouble() * 1.3 + 0.3;
                p.angle = -Math.PI / 2 + (random.nextDouble() - 0.5) * 0.9;
                p.type = random.nextInt(4);
                p.size = random.nextDouble() * 2.8 + 0.7;
                continue;
            }

            double ratio = p.life / p.maxLife;
            double alpha = ratio * 0.80;
            double sz    = p.size * ratio + 0.3;

            Color core, halo;
            switch (p.type) {
                case 0:
                    core = Color.color(0.12, 0.90, 0.25, alpha);
                    halo = Color.color(0.04, 0.60, 0.12, alpha * 0.15);
                    break;
                case 1:
                    double r1 = Math.min(1.0, 0.90 + ratio * 0.10);
                    double g1 = Math.max(0.0, ratio * 0.60);
                    core = Color.color(r1, g1, 0.0, alpha * 0.90);
                    halo = Color.color(1.0, ratio * 0.45, 0.0, alpha * 0.16);
                    break;
                case 2:
                    double r2 = Math.min(1.0, 0.80 + ratio * 0.20);
                    double g2 = Math.max(0.0, ratio * 0.20);
                    core = Color.color(r2, g2, 0.0, alpha * 0.85);
                    halo = Color.color(0.90, ratio * 0.15, 0.0, alpha * 0.13);
                    break;
                default:
                    core = Color.color(1.0, 0.95, 0.80, alpha * 0.55);
                    halo = Color.color(0.9, 0.85, 0.50, alpha * 0.10);
            }

            gc.setFill(halo);
            gc.fillOval(p.x - sz * 3, p.y - sz * 3, sz * 6, sz * 6);
            gc.setFill(core);
            gc.fillOval(p.x - sz * 0.5, p.y - sz * 0.5, sz, sz);
        }
    }

    // ══════════════════════════════════════════════════
    //  INTERACTIONS
    // ══════════════════════════════════════════════════
    private void setupInteractions() {
        loginBtn.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(130), loginBtn);
            st.setToX(1.04); st.setToY(1.04); st.play();
            loginBtn.setStyle(loginBtn.getStyle().replace(
                    "rgba(76,175,80,0.65)", "rgba(100,220,100,0.85)"));
        });
        loginBtn.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(130), loginBtn);
            st.setToX(1.0); st.setToY(1.0); st.play();
        });
        loginBtn.setOnMousePressed(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(70), loginBtn);
            st.setToX(0.97); st.setToY(0.97); st.play();
        });
        loginBtn.setOnMouseReleased(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(70), loginBtn);
            st.setToX(1.04); st.setToY(1.04); st.play();
        });

        emailField.focusedProperty().addListener((obs, o, focused) ->
                emailField.setStyle(focused ? FIELD_FOCUS : FIELD_NORMAL));
        mdpField.focusedProperty().addListener((obs, o, focused) ->
                mdpField.setStyle(focused ? FIELD_FOCUS : FIELD_NORMAL));

        // Hover sur les liens
        setupLinkHover(forgotPasswordLink);
        setupLinkHover(registerLink);
    }

    private void setupLinkHover(Label link) {
        link.setOnMouseEntered(e -> link.setStyle(link.getStyle()
                .replace("#4a9a4a", "#81c784")));
        link.setOnMouseExited(e -> link.setStyle(link.getStyle()
                .replace("#81c784", "#4a9a4a")));
    }

    private void setupBadgeHover(VBox badge) {
        badge.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), badge);
            st.setToX(1.10); st.setToY(1.10); st.play();
        });
        badge.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), badge);
            st.setToX(1.0); st.setToY(1.0); st.play();
        });
    }

    // ══════════════════════════════════════════════════
    //  LOGIN
    // ══════════════════════════════════════════════════
    @FXML
    public void handleLogin() {
        String email = emailField.getText().trim();
        String mdp   = mdpField.getText().trim();
        errorLabel.setText("");

        if (email.isEmpty() || mdp.isEmpty()) {
            shakeNode(email.isEmpty() ? emailField : mdpField);
            showError("⚠ Veuillez remplir tous les champs.");
            return;
        }

        loginBtn.setText("⏳   Vérification...");
        loginBtn.setDisable(true);

        PauseTransition loginDelay = new PauseTransition(Duration.millis(700));
        loginDelay.setOnFinished(e -> {
            loginBtn.setText("🔑   SE CONNECTER");
            loginBtn.setDisable(false);

            try {
                UtilisateurService service = new UtilisateurService();
                Optional<Utilisateur> utilisateur = service.authenticate(email, mdp);

                if (utilisateur.isPresent()) {
                    Session.setCurrentUser(utilisateur.get());
                    playExitThen(() -> ouvrirPage(
                            "/com/forestguard/views/dashboard.fxml",
                            "ForestGuard - Tableau de bord"));
                } else {
                    shakeNode(emailField);
                    shakeNode(mdpField);
                    showError("❌ Email ou mot de passe incorrect.");
                }
            } catch (Exception ex) {
                showError("⚠ Erreur de connexion à la base de données.");
                ex.printStackTrace();
            }
        });
        loginDelay.play();
    }

    // ══════════════════════════════════════════════════
    //  MOT DE PASSE OUBLIÉ
    // ══════════════════════════════════════════════════
    @FXML
    public void handleForgotPassword() {
        playExitThen(() -> ouvrirPage(
                "/com/forestguard/views/forgot_password.fxml",
                "ForestGuard - Mot de passe oublié"));
    }

    // ══════════════════════════════════════════════════
    //  INSCRIPTION
    // ══════════════════════════════════════════════════
    @FXML
    public void handleRegister() {
        playExitThen(() -> ouvrirPage(
                "/com/forestguard/views/register.fxml",
                "ForestGuard - Créer un compte"));
    }

    // ══════════════════════════════════════════════════
    //  NAVIGATION VERS L'ESPACE ADMIN (projet pompier)
    // ══════════════════════════════════════════════════
    @FXML
    public void handleNaviguerAdmin() {
        // Lance le jar du projet admin (pompier) en parallèle,
        // puis ferme cette fenêtre utilisateur.
        try {
            String adminJar = System.getProperty("forestguard.admin.jar",
                    System.getProperty("user.home") + "/pompier/pompier.jar");
            java.io.File jarFile = new java.io.File(adminJar);
            if (jarFile.exists()) {
                new ProcessBuilder("java", "-jar", adminJar)
                        .inheritIO()
                        .start();
                if (particleTimer != null) particleTimer.stop();
                ((javafx.stage.Stage) emailField.getScene().getWindow()).close();
            } else {
                // Jar absent : affiche un message informatif
                errorLabel.setText("⚠  Espace Admin non disponible sur ce poste.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            errorLabel.setText("⚠  Impossible d'ouvrir l'espace Admin.");
        }
    }

    // ══════════════════════════════════════════════════
    //  NAVIGATION
    // ══════════════════════════════════════════════════
    private void ouvrirPage(String fxmlPath, String titre) {
        try {
            if (particleTimer != null) particleTimer.stop();
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) emailField.getScene().getWindow();

            javafx.geometry.Rectangle2D screenBounds =
                    javafx.stage.Screen.getPrimary().getVisualBounds();

            Scene newScene = new Scene(root,
                    screenBounds.getWidth(),
                    screenBounds.getHeight());
            newScene.setFill(Color.web("#0d1f14"));

            root.setOpacity(0);
            root.setScaleX(0.85);
            root.setScaleY(0.85);

            stage.setScene(newScene);
            stage.setTitle(titre);
            stage.setResizable(true);
            stage.setX(screenBounds.getMinX());
            stage.setY(screenBounds.getMinY());
            stage.setWidth(screenBounds.getWidth());
            stage.setHeight(screenBounds.getHeight());
            stage.show();

            Platform.runLater(() -> {
                stage.setMaximized(true);
                Timeline enterTimeline = new Timeline(
                        new KeyFrame(Duration.ZERO,
                                new KeyValue(root.opacityProperty(), 0),
                                new KeyValue(root.scaleXProperty(), 0.85),
                                new KeyValue(root.scaleYProperty(), 0.85)
                        ),
                        new KeyFrame(Duration.millis(500),
                                new KeyValue(root.opacityProperty(), 1.0, Interpolator.EASE_OUT),
                                new KeyValue(root.scaleXProperty(), 1.0, Interpolator.EASE_OUT),
                                new KeyValue(root.scaleYProperty(), 1.0, Interpolator.EASE_OUT)
                        )
                );
                enterTimeline.play();
            });

        } catch (Exception e) {
            showError("⚠ Impossible de charger la page.");
            e.printStackTrace();
        }
    }

    private void playExitThen(Runnable action) {
        if (particleTimer != null) particleTimer.stop();

        javafx.scene.shape.Rectangle flash = new javafx.scene.shape.Rectangle();
        flash.widthProperty().bind(rootPane.widthProperty());
        flash.heightProperty().bind(rootPane.heightProperty());
        flash.setFill(Color.web("#0d1f14"));
        flash.setOpacity(0);
        rootPane.getChildren().add(flash);

        javafx.scene.transform.Scale scaleT = new javafx.scene.transform.Scale(1, 1,
                rootPane.getWidth() / 2, rootPane.getHeight() / 2);
        rootPane.getTransforms().add(scaleT);

        Timeline exitTimeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(scaleT.xProperty(), 1.0),
                        new KeyValue(scaleT.yProperty(), 1.0),
                        new KeyValue(rootPane.rotateProperty(), 0),
                        new KeyValue(rootPane.opacityProperty(), 1.0),
                        new KeyValue(flash.opacityProperty(), 0)
                ),
                new KeyFrame(Duration.millis(200),
                        new KeyValue(flash.opacityProperty(), 0.6, Interpolator.EASE_OUT),
                        new KeyValue(scaleT.xProperty(), 1.04, Interpolator.EASE_OUT),
                        new KeyValue(scaleT.yProperty(), 1.04, Interpolator.EASE_OUT)
                ),
                new KeyFrame(Duration.millis(350),
                        new KeyValue(flash.opacityProperty(), 0, Interpolator.EASE_IN),
                        new KeyValue(scaleT.xProperty(), 1.12, Interpolator.EASE_IN),
                        new KeyValue(scaleT.yProperty(), 1.12, Interpolator.EASE_IN),
                        new KeyValue(rootPane.rotateProperty(), 2.0, Interpolator.EASE_IN)
                ),
                new KeyFrame(Duration.millis(550),
                        new KeyValue(scaleT.xProperty(), 2.5, Interpolator.EASE_IN),
                        new KeyValue(scaleT.yProperty(), 2.5, Interpolator.EASE_IN),
                        new KeyValue(rootPane.rotateProperty(), -3.0, Interpolator.EASE_IN),
                        new KeyValue(rootPane.opacityProperty(), 0, Interpolator.EASE_IN)
                )
        );
        exitTimeline.setOnFinished(e -> {
            rootPane.getTransforms().remove(scaleT);
            rootPane.setRotate(0);
            rootPane.setOpacity(1);
            action.run();
        });
        exitTimeline.play();
    }

    private void shakeNode(javafx.scene.Node node) {
        TranslateTransition shake = new TranslateTransition(Duration.millis(52), node);
        shake.setFromX(0); shake.setByX(10);
        shake.setCycleCount(6); shake.setAutoReverse(true);
        shake.play();
    }

    private void showError(String msg) {
        errorLabel.setStyle(
                "-fx-text-fill: #ff5252; -fx-font-size: 11.5px;" +
                        "-fx-font-family: 'Georgia'; -fx-font-style: italic; -fx-padding: 4 0 0 0;");
        errorLabel.setText(msg);
        FadeTransition ft = new FadeTransition(Duration.millis(250), errorLabel);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    // ══════════════════════════════════════════════════
    //  PARTICLE
    // ══════════════════════════════════════════════════
    private static class Particle {
        double x, y, speed, angle, life, maxLife, size;
        int type;
    }
}