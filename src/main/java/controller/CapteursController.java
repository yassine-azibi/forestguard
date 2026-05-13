package controller;

import service.AlerteService;
import service.CapteurService;
import service.CapteurSimulator;
import service.WeatherApiService;
import utils.NavigationManager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CapteursController implements Initializable {

    // ── Labels capteurs ────────────────────────────────────────────────────────
    @FXML private Label lblTemp1, lblHum1, lblFumee1, lblEtat1;
    @FXML private Button btnSim1;
    @FXML private Label lblTemp2, lblHum2, lblFumee2, lblEtat2;
    @FXML private Button btnSim2;
    @FXML private Label lblTemp3, lblHum3, lblFumee3, lblEtat3;
    @FXML private Button btnSim3;
    @FXML private Label lblResultat;

    // ── Labels météo temps réel ────────────────────────────────────────────────
    @FXML private Label lblMeteoTunis;
    @FXML private Label lblMeteoBizerte;
    @FXML private Label lblMeteoJendouba;

    private final CapteurService    capteurService  = new CapteurService();
    private final WeatherApiService weatherService  = new WeatherApiService();

    // Simulateur de données fake
    private final CapteurSimulator simulator = new CapteurSimulator();

    // Scheduler météo
    private ScheduledExecutorService meteoScheduler;

    // Garde trace de si une alarme a déjà été créée en BDD pour ce pic
    private final boolean[] alarmeCreee = {false, false, false};

    // ── Styles ────────────────────────────────────────────────────────────────
    private static final String VAL_NORMAL = "-fx-text-fill:white;-fx-font-size:18;-fx-font-weight:bold;";
    private static final String VAL_DANGER = "-fx-text-fill:#EF9A9A;-fx-font-size:18;-fx-font-weight:bold;";
    private static final String VAL_WARN   = "-fx-text-fill:#FFE082;-fx-font-size:18;-fx-font-weight:bold;";
    private static final String BTN_NORMAL = "-fx-background-color:#16a34a;-fx-text-fill:white;-fx-background-radius:8;-fx-font-size:12;-fx-cursor:hand;-fx-font-weight:bold;-fx-border-width:0;";
    private static final String BTN_DANGER = "-fx-background-color:#B71C1C;-fx-text-fill:white;-fx-background-radius:8;-fx-font-size:12;-fx-cursor:hand;-fx-font-weight:bold;-fx-border-width:0;";
    private static final String ETAT_OK    = "-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:10;";
    private static final String ETAT_WARN  = "-fx-text-fill:#FFE082;-fx-font-weight:bold;-fx-font-size:10;";
    private static final String ETAT_ERR   = "-fx-text-fill:#EF9A9A;-fx-font-weight:bold;-fx-font-size:10;";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblResultat.setText("⟳ Simulation en cours...");

        // Démarrer le simulateur — mise à jour toutes les 3 secondes
        simulator.demarrer(3, this::mettreAJourUI);

        // Météo au démarrage + toutes les 5 minutes
        chargerMeteo();
        meteoScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "meteo-refresh");
            t.setDaemon(true);
            return t;
        });
        meteoScheduler.scheduleAtFixedRate(this::chargerMeteo, 5, 5, TimeUnit.MINUTES);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  MISE À JOUR UI depuis le simulateur
    // ══════════════════════════════════════════════════════════════════════════

    private void mettreAJourUI(CapteurSimulator.DonneesCapteur[] donnees) {
        // Capteur 1
        afficherCapteur(donnees[0], 0,
                lblTemp1, lblHum1, lblFumee1, lblEtat1, btnSim1);

        // Capteur 2
        afficherCapteur(donnees[1], 1,
                lblTemp2, lblHum2, lblFumee2, lblEtat2, btnSim2);

        // Capteur 3
        afficherCapteur(donnees[2], 2,
                lblTemp3, lblHum3, lblFumee3, lblEtat3, btnSim3);
    }

    private void afficherCapteur(CapteurSimulator.DonneesCapteur d, int idx,
                                   Label lblTemp, Label lblHum, Label lblFumee,
                                   Label lblEtat, Button btnSim) {
        boolean depTemp  = d.temp     > 60;
        boolean depFumee = d.fumee    > 50;
        boolean depHum   = d.humidite < 20;
        boolean alarme   = depTemp || depFumee || depHum;

        // Température
        lblTemp.setText(String.format("%.1f°C", d.temp));
        lblTemp.setStyle(depTemp ? VAL_DANGER : (d.temp > 50 ? VAL_WARN : VAL_NORMAL));

        // Humidité
        lblHum.setText(String.format("%.1f%%", d.humidite));
        lblHum.setStyle(depHum ? VAL_DANGER : (d.humidite < 30 ? VAL_WARN : VAL_NORMAL));

        // Fumée
        lblFumee.setText(String.format("%.1f ppm", d.fumee));
        lblFumee.setStyle(depFumee ? VAL_DANGER : (d.fumee > 35 ? VAL_WARN : VAL_NORMAL));

        // État + bouton
        if (alarme) {
            lblEtat.setText("⚠ ALERTE");
            lblEtat.setStyle(ETAT_ERR);
            btnSim.setStyle(BTN_DANGER);
            btnSim.setText("🔴 Alarme active");

            // Affichage seulement — pas d'alerte automatique
            // L'alerte est creee UNIQUEMENT via le bouton "Forcer alarme"
            if (!alarmeCreee[idx]) {
                alarmeCreee[idx] = true;
                Platform.runLater(() -> {
                    lblResultat.setText("Capteur " + d.id + " en alarme — cliquez Forcer pour enregistrer");
                    lblResultat.setStyle("-fx-text-fill:#FFE082;-fx-font-weight:bold;-fx-font-size:12;");
                });
            }
        } else {
            // Retour en normal — réinitialiser le flag alarme
            alarmeCreee[idx] = false;
            lblEtat.setText("● Normal");
            lblEtat.setStyle(d.temp > 50 || d.fumee > 35 ? ETAT_WARN : ETAT_OK);
            btnSim.setStyle(BTN_NORMAL);
            btnSim.setText("⚡ Forcer alarme");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  BOUTONS MANUELS (force une alarme ou réinitialise)
    // ══════════════════════════════════════════════════════════════════════════

    @FXML private void simulerC001() {
        simulator.simulerAlarme(0);
        alarmeCreee[0] = false; // Permettre la creation d'une alerte
        lblResultat.setText("Alarme C-001 activee — en attente du depassement de seuil...");
        lblResultat.setStyle("-fx-text-fill:#FFE082;-fx-font-weight:bold;-fx-font-size:12;");
    }
    @FXML private void reinitC001() {
        simulator.reinitialiser(0);
        alarmeCreee[0] = false;
        lblResultat.setText("↺ C-001 remis en état normal");
        lblResultat.setStyle("-fx-text-fill:#4ade80;-fx-font-weight:bold;-fx-font-size:12;");
    }

    @FXML private void simulerC002() {
        simulator.simulerAlarme(1);
        alarmeCreee[1] = false;
        lblResultat.setText("Alarme C-002 activee — en attente du depassement de seuil...");
        lblResultat.setStyle("-fx-text-fill:#FFE082;-fx-font-weight:bold;-fx-font-size:12;");
    }
    @FXML private void reinitC002() {
        simulator.reinitialiser(1);
        alarmeCreee[1] = false;
        lblResultat.setText("↺ C-002 remis en état normal");
        lblResultat.setStyle("-fx-text-fill:#4ade80;-fx-font-weight:bold;-fx-font-size:12;");
    }

    @FXML private void simulerC003() {
        simulator.simulerAlarme(2);
        alarmeCreee[2] = false;
        lblResultat.setText("Alarme C-003 activee — en attente du depassement de seuil...");
        lblResultat.setStyle("-fx-text-fill:#FFE082;-fx-font-weight:bold;-fx-font-size:12;");
    }
    @FXML private void reinitC003() {
        simulator.reinitialiser(2);
        alarmeCreee[2] = false;
        lblResultat.setText("↺ C-003 remis en état normal");
        lblResultat.setStyle("-fx-text-fill:#4ade80;-fx-font-weight:bold;-fx-font-size:12;");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  MÉTÉO TEMPS RÉEL
    // ══════════════════════════════════════════════════════════════════════════

    private void chargerMeteo() {
        new Thread(() -> {
            double[] tunis    = weatherService.getMeteo("Tunis");
            double[] bizerte  = weatherService.getMeteo("Bizerte");
            double[] jendouba = weatherService.getMeteo("Jendouba");
            Platform.runLater(() -> {
                if (lblMeteoTunis != null)
                    lblMeteoTunis.setText(tunis != null
                        ? String.format("🌡 %.0f°C  💧 %.0f%%", tunis[0], tunis[1])
                        : "🌡 --  💧 --");
                if (lblMeteoBizerte != null)
                    lblMeteoBizerte.setText(bizerte != null
                        ? String.format("🌡 %.0f°C  💧 %.0f%%", bizerte[0], bizerte[1])
                        : "🌡 --  💧 --");
                if (lblMeteoJendouba != null)
                    lblMeteoJendouba.setText(jendouba != null
                        ? String.format("🌡 %.0f°C  💧 %.0f%%", jendouba[0], jendouba[1])
                        : "🌡 --  💧 --");
            });
        }, "meteo-load").start();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  NAVIGATION
    // ══════════════════════════════════════════════════════════════════════════

    @FXML private void goDashboard() {
        arreterServices();
        Stage s = (Stage) lblResultat.getScene().getWindow();
        NavigationManager.navigateTo(s, "/fxml/Dashboard.fxml");
    }
    @FXML private void goAjout() {
        arreterServices();
        Stage s = (Stage) lblResultat.getScene().getWindow();
        NavigationManager.navigateTo(s, "/fxml/AjoutAlerte.fxml");
    }

    private void arreterServices() {
        simulator.arreter();
        if (meteoScheduler != null) meteoScheduler.shutdown();
    }
}
