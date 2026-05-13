package controller;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Panneau TTS style Siri :
 * - Bouton central animé (pulsation quand il parle)
 * - Lecture automatique du contexte
 * - Affichage du texte en cours
 * - Contrôles vitesse et langue
 */
public class TTSPanel {

    private final TextToSpeechService tts = TextToSpeechService.getInstance();
    private Stage  stage;
    private Label  lblTexteEnCours;
    private Button btnPrincipal;
    private Circle cercleAnim;
    private Timeline pulseAnim;
    private boolean enLecture = false;

    // ── Contexte injecté depuis ForetPrincipal ────────────────────────────────
    private String contexteForets    = "";
    private String contexteIncendies = "";

    public void setContexte(String forets, String incendies) {
        this.contexteForets    = forets;
        this.contexteIncendies = incendies;
    }

    /** Ouvre la fenêtre TTS (ou la ramène au premier plan). */
    public void ouvrirFenetre() {
        if (stage != null && stage.isShowing()) {
            stage.toFront();
            return;
        }
        stage = new Stage();
        stage.setTitle("ForestGuard — Assistant vocal");
        stage.initModality(Modality.NONE);
        stage.setResizable(false);
        stage.setScene(new Scene(construireUI(), 380, 520));
        stage.show();
    }

    private VBox construireUI() {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #0d1f14;");

        // ── Header ────────────────────────────────────────────────────────────
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color:#0a150a;-fx-padding:16 20;-fx-border-color:#1e3322;-fx-border-width:0 0 1 0;");
        Label lblTitre = new Label("🔊  Assistant Vocal");
        lblTitre.setStyle("-fx-font-size:15;-fx-font-weight:bold;-fx-text-fill:white;");
        HBox.setHgrow(lblTitre, Priority.ALWAYS);
        Button btnFermer = new Button("✕");
        btnFermer.setStyle("-fx-background-color:transparent;-fx-text-fill:#94a3b8;-fx-font-size:14;-fx-cursor:hand;-fx-padding:0;");
        btnFermer.setOnAction(e -> { tts.stop(); stage.close(); });
        header.getChildren().addAll(lblTitre, btnFermer);

        // ── Zone centrale Siri ────────────────────────────────────────────────
        VBox centreBox = new VBox(20);
        centreBox.setAlignment(Pos.CENTER);
        centreBox.setStyle("-fx-padding:30 20 20 20;");
        VBox.setVgrow(centreBox, Priority.ALWAYS);

        // Cercles animés (style Siri)
        cercleAnim = new Circle(52, Color.web("#16a34a"));
        Circle cercle2 = new Circle(52, Color.TRANSPARENT);
        cercle2.setStroke(Color.web("#16a34a", 0.3));
        cercle2.setStrokeWidth(2);
        Circle cercle3 = new Circle(52, Color.TRANSPARENT);
        cercle3.setStroke(Color.web("#16a34a", 0.15));
        cercle3.setStrokeWidth(2);

        // Icône micro au centre
        Label lblMicro = new Label("🔊");
        lblMicro.setStyle("-fx-font-size:28;");

        StackPane siriStack = new StackPane(cercle3, cercle2, cercleAnim, lblMicro);
        siriStack.setMinSize(120, 120);
        siriStack.setMaxSize(120, 120);

        // Animation pulsation
        pulseAnim = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(cercleAnim.radiusProperty(), 52),
                new KeyValue(cercle2.radiusProperty(), 52),
                new KeyValue(cercle3.radiusProperty(), 52)
            ),
            new KeyFrame(Duration.millis(600),
                new KeyValue(cercleAnim.radiusProperty(), 56),
                new KeyValue(cercle2.radiusProperty(), 66),
                new KeyValue(cercle3.radiusProperty(), 76)
            ),
            new KeyFrame(Duration.millis(1200),
                new KeyValue(cercleAnim.radiusProperty(), 52),
                new KeyValue(cercle2.radiusProperty(), 52),
                new KeyValue(cercle3.radiusProperty(), 52)
            )
        );
        pulseAnim.setCycleCount(Timeline.INDEFINITE);

        // Texte en cours de lecture
        lblTexteEnCours = new Label("Appuyez pour parler");
        lblTexteEnCours.setStyle("-fx-font-size:13;-fx-text-fill:rgba(255,255,255,0.6);-fx-wrap-text:true;-fx-text-alignment:center;");
        lblTexteEnCours.setMaxWidth(320);
        lblTexteEnCours.setAlignment(Pos.CENTER);
        lblTexteEnCours.setWrapText(true);

        // Bouton principal (clic = lire/arrêter)
        btnPrincipal = new Button("▶  Lire le résumé");
        btnPrincipal.setStyle(
            "-fx-background-color:#16a34a;-fx-text-fill:white;" +
            "-fx-font-size:13;-fx-font-weight:bold;" +
            "-fx-background-radius:24;-fx-padding:12 32;-fx-cursor:hand;" +
            "-fx-effect:dropshadow(gaussian,rgba(22,163,74,0.5),12,0,0,3);"
        );
        btnPrincipal.setOnAction(e -> toggleLecture());

        centreBox.getChildren().addAll(siriStack, lblTexteEnCours, btnPrincipal);

        // ── Raccourcis contextuels ────────────────────────────────────────────
        VBox raccBox = new VBox(8);
        raccBox.setStyle("-fx-padding:0 20 16 20;");

        Label lblRacc = new Label("LECTURE RAPIDE");
        lblRacc.setStyle("-fx-font-size:10;-fx-text-fill:#4ade80;-fx-font-weight:bold;-fx-letter-spacing:1;");

        String[][] raccourcis = {
            {"🌲", "Résumé forêts",    null},   // null = généré dynamiquement
            {"🔥", "Résumé incendies", null},
            {"⚠️", "Alerte incendie",  "Attention ! Risque d'incendie élevé. Vérifiez immédiatement la liste des incendies actifs."},
            {"✅", "Tout va bien",     "Aucune alerte critique. La surveillance des forêts est active et opérationnelle."}
        };

        FlowPane flow = new FlowPane(8, 8);
        for (String[] r : raccourcis) {
            Button b = new Button(r[0] + " " + r[1]);
            b.setStyle(
                "-fx-background-color:rgba(22,163,74,0.12);-fx-text-fill:#4ade80;" +
                "-fx-font-size:11;-fx-background-radius:20;" +
                "-fx-padding:6 12;-fx-cursor:hand;" +
                "-fx-border-color:rgba(22,163,74,0.25);-fx-border-radius:20;-fx-border-width:1;"
            );
            b.setOnMouseEntered(ev -> b.setStyle(
                "-fx-background-color:rgba(22,163,74,0.22);-fx-text-fill:#4ade80;" +
                "-fx-font-size:11;-fx-background-radius:20;" +
                "-fx-padding:6 12;-fx-cursor:hand;" +
                "-fx-border-color:rgba(22,163,74,0.4);-fx-border-radius:20;-fx-border-width:1;"
            ));
            b.setOnMouseExited(ev -> b.setStyle(
                "-fx-background-color:rgba(22,163,74,0.12);-fx-text-fill:#4ade80;" +
                "-fx-font-size:11;-fx-background-radius:20;" +
                "-fx-padding:6 12;-fx-cursor:hand;" +
                "-fx-border-color:rgba(22,163,74,0.25);-fx-border-radius:20;-fx-border-width:1;"
            ));
            final String texteFixe = r[2];
            final String type = r[1];
            b.setOnAction(ev -> {
                String txt = texteFixe != null ? texteFixe : genererTexte(type);
                lireTexte(txt);
            });
            flow.getChildren().add(b);
        }
        raccBox.getChildren().addAll(lblRacc, flow);

        // ── Contrôles vitesse + langue ────────────────────────────────────────
        HBox controles = new HBox(12);
        controles.setAlignment(Pos.CENTER);
        controles.setStyle("-fx-padding:0 20 20 20;");

        // Vitesse
        Label lblV = new Label("Vitesse");
        lblV.setStyle("-fx-font-size:10;-fx-text-fill:#64748b;");
        Slider slV = new Slider(-5, 5, 0);
        slV.setPrefWidth(120);
        slV.setStyle("-fx-accent:#16a34a;");
        Label lblVVal = new Label("Normal");
        lblVVal.setStyle("-fx-font-size:10;-fx-text-fill:#4ade80;-fx-min-width:40;");
        slV.valueProperty().addListener((obs, o, n) -> {
            double v = n.doubleValue();
            String label = v == 0 ? "Normal" : (v > 0 ? "+" : "") + String.format("%.0f", v);
            lblVVal.setText(label);
            tts.setRate((float) v);
        });

        // Langue
        ComboBox<String> cbLang = new ComboBox<>();
        cbLang.getItems().addAll("fr-FR", "en-US", "ar-SA");
        cbLang.setValue("fr-FR");
        cbLang.setStyle(
            "-fx-background-color:rgba(255,255,255,0.06);-fx-text-fill:white;" +
            "-fx-border-color:#1e3322;-fx-border-radius:8;-fx-background-radius:8;-fx-font-size:11;"
        );
        cbLang.setOnAction(e -> tts.setLang(cbLang.getValue()));

        // Bouton stop
        Button btnStop = new Button("⏹");
        btnStop.setStyle(
            "-fx-background-color:rgba(239,68,68,0.15);-fx-text-fill:#ef4444;" +
            "-fx-background-radius:8;-fx-border-color:rgba(239,68,68,0.3);" +
            "-fx-border-radius:8;-fx-border-width:1;-fx-cursor:hand;" +
            "-fx-pref-width:36;-fx-pref-height:36;-fx-font-size:14;"
        );
        btnStop.setOnAction(e -> arreterLecture());

        controles.getChildren().addAll(lblV, slV, lblVVal, cbLang, btnStop);

        root.getChildren().addAll(header, centreBox, raccBox, controles);
        return root;
    }

    // ── Logique lecture ───────────────────────────────────────────────────────

    private void toggleLecture() {
        if (enLecture) {
            arreterLecture();
        } else {
            lireTexte(genererTexte("Résumé forêts"));
        }
    }

    private void arreterLecture() {
        tts.stop();
        enLecture = false;
        pulseAnim.stop();
        if (cercleAnim != null) cercleAnim.setRadius(52);
        if (btnPrincipal != null) {
            btnPrincipal.setText("▶  Lire le résumé");
            btnPrincipal.setStyle(
                "-fx-background-color:#16a34a;-fx-text-fill:white;" +
                "-fx-font-size:13;-fx-font-weight:bold;" +
                "-fx-background-radius:24;-fx-padding:12 32;-fx-cursor:hand;" +
                "-fx-effect:dropshadow(gaussian,rgba(22,163,74,0.5),12,0,0,3);"
            );
        }
        if (lblTexteEnCours != null)
            lblTexteEnCours.setText("Lecture arrêtée.");
    }

    /** Lit un texte et met à jour l'UI. */
    public void lireTexte(String texte) {
        if (texte == null || texte.isBlank()) return;
        enLecture = true;
        tts.speak(texte);

        Platform.runLater(() -> {
            // Afficher le texte tronqué
            String affiche = texte.length() > 120 ? texte.substring(0, 120) + "…" : texte;
            if (lblTexteEnCours != null) lblTexteEnCours.setText(affiche);

            // Démarrer l'animation
            if (pulseAnim != null) pulseAnim.play();

            // Bouton → Stop
            if (btnPrincipal != null) {
                btnPrincipal.setText("⏹  Arrêter");
                btnPrincipal.setStyle(
                    "-fx-background-color:#dc2626;-fx-text-fill:white;" +
                    "-fx-font-size:13;-fx-font-weight:bold;" +
                    "-fx-background-radius:24;-fx-padding:12 32;-fx-cursor:hand;" +
                    "-fx-effect:dropshadow(gaussian,rgba(220,38,38,0.5),12,0,0,3);"
                );
            }

            // Arrêter l'animation après la durée estimée
            int dureeMs = Math.max(2000, texte.length() * 60);
            new Timeline(new KeyFrame(Duration.millis(dureeMs), ev -> {
                if (enLecture) {
                    enLecture = false;
                    if (pulseAnim != null) pulseAnim.stop();
                    if (btnPrincipal != null) {
                        btnPrincipal.setText("▶  Lire le résumé");
                        btnPrincipal.setStyle(
                            "-fx-background-color:#16a34a;-fx-text-fill:white;" +
                            "-fx-font-size:13;-fx-font-weight:bold;" +
                            "-fx-background-radius:24;-fx-padding:12 32;-fx-cursor:hand;" +
                            "-fx-effect:dropshadow(gaussian,rgba(22,163,74,0.5),12,0,0,3);"
                        );
                    }
                    if (lblTexteEnCours != null)
                        lblTexteEnCours.setText("Lecture terminée.");
                }
            })).play();
        });
    }

    /** Génère automatiquement un texte depuis le contexte. */
    private String genererTexte(String type) {
        return switch (type) {
            case "Résumé forêts" -> contexteForets.isBlank()
                ? "Bienvenue dans ForestGuard. Aucune donnée de forêt disponible pour le moment."
                : contexteForets;
            case "Résumé incendies" -> contexteIncendies.isBlank()
                ? "Aucun incendie actif enregistré dans le système."
                : contexteIncendies;
            default -> type;
        };
    }
}

