package service;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * SIMULATEUR DE CAPTEURS IoT — Données fake réalistes
 *
 * Chaque capteur a :
 *  - Un état normal  : valeurs dans les plages sûres avec bruit aléatoire
 *  - Un mode "feu"   : montée progressive jusqu'au dépassement de seuil
 *  - Des pics spontanés (5% de chance par tick) pour simuler des anomalies
 *
 * Seuils d'alerte :
 *   Température  > 60°C
 *   Fumée        > 50 ppm
 *   Humidité     < 20%
 */
public class CapteurSimulator {

    // ── Données en temps réel pour chaque capteur ──────────────────────────────
    public static class DonneesCapteur {
        public double temp;
        public double fumee;
        public double humidite;
        public boolean enAlarme;
        public String zone;
        public String id;

        public DonneesCapteur(String id, String zone, double temp, double fumee, double humidite) {
            this.id       = id;
            this.zone     = zone;
            this.temp     = temp;
            this.fumee    = fumee;
            this.humidite = humidite;
            this.enAlarme = false;
        }
    }

    // ── État interne de chaque capteur ─────────────────────────────────────────
    private enum ModeCapteur { NORMAL, MONTEE, ALARME, DESCENTE }

    private static final Random rng = new Random();

    // Valeurs de base pour chaque capteur (normales pour la Tunisie forestière)
    private final double[] baseTemp     = {28, 31, 25};
    private final double[] baseFumee    = {5,  8,  3};
    private final double[] baseHumidite = {55, 48, 62};

    private final double[] currentTemp     = baseTemp.clone();
    private final double[] currentFumee    = baseFumee.clone();
    private final double[] currentHumidite = baseHumidite.clone();

    private final ModeCapteur[] modes = {
        ModeCapteur.NORMAL, ModeCapteur.NORMAL, ModeCapteur.NORMAL
    };

    // Compteur de ticks en mode montée
    private final int[] ticksMontee = {0, 0, 0};

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?>       task;

    // Callback appelé à chaque tick avec les nouvelles données
    private Consumer<DonneesCapteur[]> onUpdate;

    // Zones des 3 capteurs
    private final String[] zones = {
        "Zone Nord-A, Cap Bon",
        "Zone Est-B, Kroumirie",
        "Zone Sud-C, Jendouba"
    };
    private final String[] ids = {"C-001", "C-002", "C-003"};

    // ══════════════════════════════════════════════════════════════════════════
    //  API PUBLIQUE
    // ══════════════════════════════════════════════════════════════════════════

    /** Démarre la simulation, appelle onUpdate toutes les `intervalleSecondes` */
    public void demarrer(int intervalleSecondes, Consumer<DonneesCapteur[]> onUpdate) {
        this.onUpdate = onUpdate;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "capteur-sim");
            t.setDaemon(true);
            return t;
        });
        task = scheduler.scheduleAtFixedRate(this::tick, 0, intervalleSecondes, TimeUnit.SECONDS);
    }

    /** Arrête la simulation */
    public void arreter() {
        if (task     != null) task.cancel(false);
        if (scheduler != null) scheduler.shutdown();
    }

    /** Force un dépassement immédiat sur le capteur i (0, 1, ou 2) */
    public void simulerAlarme(int index) {
        if (index >= 0 && index < 3) {
            modes[index]      = ModeCapteur.MONTEE;
            ticksMontee[index] = 0;
        }
    }

    /** Remet le capteur i en état normal */
    public void reinitialiser(int index) {
        if (index >= 0 && index < 3) {
            modes[index]          = ModeCapteur.DESCENTE;
            ticksMontee[index]    = 0;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  LOGIQUE DE SIMULATION
    // ══════════════════════════════════════════════════════════════════════════

    private synchronized void tick() {
        DonneesCapteur[] donnees = new DonneesCapteur[3];

        for (int i = 0; i < 3; i++) {
            // Transition spontanée : 4% de chance de passer en mode montée
            if (modes[i] == ModeCapteur.NORMAL && rng.nextDouble() < 0.04) {
                modes[i] = ModeCapteur.MONTEE;
                ticksMontee[i] = 0;
            }

            switch (modes[i]) {
                case NORMAL -> {
                    // Fluctuation gaussienne autour des valeurs de base
                    currentTemp[i]     = clamp(baseTemp[i]     + gauss(0, 2.0),  15, 55);
                    currentFumee[i]    = clamp(baseFumee[i]    + gauss(0, 1.5),   1, 35);
                    currentHumidite[i] = clamp(baseHumidite[i] + gauss(0, 3.0),  30, 85);
                }
                case MONTEE -> {
                    // Montée progressive vers les seuils d'alarme
                    ticksMontee[i]++;
                    double progression = Math.min(ticksMontee[i] / 8.0, 1.0); // 8 ticks pour atteindre l'alarme

                    currentTemp[i]     = clamp(baseTemp[i]  + progression * 50 + gauss(0, 1), baseTemp[i], 85);
                    currentFumee[i]    = clamp(baseFumee[i] + progression * 80 + gauss(0, 2), baseFumee[i], 110);
                    currentHumidite[i] = clamp(baseHumidite[i] - progression * 45 + gauss(0, 1), 5, baseHumidite[i]);

                    // Passer en ALARME une fois les seuils dépassés
                    if (currentTemp[i] > 62 || currentFumee[i] > 52 || currentHumidite[i] < 18) {
                        modes[i] = ModeCapteur.ALARME;
                    }
                }
                case ALARME -> {
                    // Valeurs élevées + bruit pour simuler l'incendie actif
                    currentTemp[i]     = clamp(currentTemp[i]     + gauss(0, 3),   60, 90);
                    currentFumee[i]    = clamp(currentFumee[i]    + gauss(0, 5),   50, 130);
                    currentHumidite[i] = clamp(currentHumidite[i] + gauss(0, 1.5),  3, 20);

                    // Auto-descente après 10 ticks (intervention simulée)
                    ticksMontee[i]++;
                    if (ticksMontee[i] > 10) {
                        modes[i] = ModeCapteur.DESCENTE;
                        ticksMontee[i] = 0;
                    }
                }
                case DESCENTE -> {
                    // Retour progressif aux valeurs normales
                    currentTemp[i]     += (baseTemp[i]     - currentTemp[i])     * 0.25 + gauss(0, 1);
                    currentFumee[i]    += (baseFumee[i]    - currentFumee[i])    * 0.25 + gauss(0, 1);
                    currentHumidite[i] += (baseHumidite[i] - currentHumidite[i]) * 0.25 + gauss(0, 1);

                    // Retour en NORMAL quand proche des valeurs de base
                    if (Math.abs(currentTemp[i] - baseTemp[i]) < 3
                     && Math.abs(currentFumee[i] - baseFumee[i]) < 3
                     && Math.abs(currentHumidite[i] - baseHumidite[i]) < 5) {
                        modes[i] = ModeCapteur.NORMAL;
                        currentTemp[i]     = baseTemp[i];
                        currentFumee[i]    = baseFumee[i];
                        currentHumidite[i] = baseHumidite[i];
                    }
                }
            }

            DonneesCapteur d = new DonneesCapteur(ids[i], zones[i],
                    round1(currentTemp[i]),
                    round1(currentFumee[i]),
                    round1(currentHumidite[i]));
            d.enAlarme = (modes[i] == ModeCapteur.ALARME || modes[i] == ModeCapteur.MONTEE);
            donnees[i] = d;
        }

        if (onUpdate != null) {
            Platform.runLater(() -> onUpdate.accept(donnees));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private double gauss(double mean, double std) {
        return mean + rng.nextGaussian() * std;
    }

    private double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }

    private double round1(double val) {
        return Math.round(val * 10.0) / 10.0;
    }
}
