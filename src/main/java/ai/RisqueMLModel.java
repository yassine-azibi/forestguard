package ai;

import java.util.Random;

/**
 * ══════════════════════════════════════════════════════════════════════════════
 *  MODÈLE ML — PRÉDICTION DE RISQUE D'INCENDIE FORESTIER
 *  Algorithme : Régression Logistique (one-vs-rest, 4 classes)
 *  Entraînement : 600 exemples synthétiques, 500 epochs, pur Java 17
 * ══════════════════════════════════════════════════════════════════════════════
 */
public class RisqueMLModel {

    private static final int    N_FEATURES    = 7;
    private static final int    N_CLASSES     = 4;
    private static final int    N_TRAIN       = 600;
    private static final int    EPOCHS        = 500;
    private static final double LEARNING_RATE = 0.05;

    private final double[][] poids = new double[N_CLASSES][N_FEATURES + 1];

    private static final double TEMP_MIN  = 10,  TEMP_MAX  = 90;
    private static final double FUMEE_MIN = 0,   FUMEE_MAX = 150;
    private static final double HUM_MIN   = 5,   HUM_MAX   = 90;

    public static final String[] LABELS = {"Faible", "Modere", "Eleve", "Critique"};
    public static final String[] COLORS = {"#4ade80", "#FFE082", "#FFA726", "#EF9A9A"};
    public static final String[] ICONS  = {"OK", "WARN", "HIGH", "CRIT"};

    public RisqueMLModel() {
        double[][] X = new double[N_TRAIN][N_FEATURES];
        int[]      y = new int[N_TRAIN];
        genererDonnees(X, y);
        entrainer(X, y);
        System.out.println("[ML] Modele entraine sur " + N_TRAIN + " exemples.");
    }

    public ResultatPrediction predire(double temp, double fumee, double humidite, int heure) {
        double[] features = extraireFeatures(temp, fumee, humidite, heure);
        double[] probas   = calculerProbabilites(features);
        int classeMax = 0;
        for (int c = 1; c < N_CLASSES; c++) {
            if (probas[c] > probas[classeMax]) classeMax = c;
        }
        double scoreRisque = Math.min(1.0, probas[1]*0.25 + probas[2]*0.60 + probas[3]*1.0);
        return new ResultatPrediction(classeMax, probas, scoreRisque, features, temp, fumee, humidite);
    }

    public static class ResultatPrediction {
        public final int      classe;
        public final double[] probas;
        public final double   scoreRisque;
        public final double[] features;
        private final double  temp, fumee, humidite;

        public ResultatPrediction(int classe, double[] probas, double scoreRisque,
                                   double[] features, double temp, double fumee, double humidite) {
            this.classe      = classe;
            this.probas      = probas;
            this.scoreRisque = scoreRisque;
            this.features    = features;
            this.temp        = temp;
            this.fumee       = fumee;
            this.humidite    = humidite;
        }

        public String getLabel()         { return LABELS[classe]; }
        public String getCouleur()       { return COLORS[classe]; }
        public int    getPourcentage()   { return (int)(scoreRisque * 100); }

        public String getExplication() {
            StringBuilder sb = new StringBuilder();
            if (temp > 60)       sb.append("Temp critique (").append(String.format("%.0f", temp)).append("C). ");
            else if (temp > 45)  sb.append("Temp elevee (").append(String.format("%.0f", temp)).append("C). ");
            if (fumee > 50)      sb.append("Fumee critique (").append(String.format("%.0f", fumee)).append("ppm). ");
            else if (fumee > 30) sb.append("Fumee detectee (").append(String.format("%.0f", fumee)).append("ppm). ");
            if (humidite < 20)   sb.append("Humidite tres basse (").append(String.format("%.0f", humidite)).append("%). ");
            else if (humidite < 35) sb.append("Humidite faible (").append(String.format("%.0f", humidite)).append("%). ");
            if (sb.length() == 0) sb.append("Conditions normales.");
            return sb.toString().trim();
        }
    }

    private double[] extraireFeatures(double temp, double fumee, double humidite, int heure) {
        double[] f = new double[N_FEATURES];
        f[0] = normaliser(temp,     TEMP_MIN,  TEMP_MAX);
        f[1] = normaliser(fumee,    FUMEE_MIN, FUMEE_MAX);
        f[2] = 1.0 - normaliser(humidite, HUM_MIN, HUM_MAX);
        f[3] = (Math.sin(2 * Math.PI * heure / 24.0) + 1) / 2.0;
        f[4] = (Math.cos(2 * Math.PI * heure / 24.0) + 1) / 2.0;
        f[5] = f[0] * f[1];
        f[6] = f[0] * f[2];
        return f;
    }

    private double[] calculerProbabilites(double[] features) {
        double[] scores = new double[N_CLASSES];
        for (int c = 0; c < N_CLASSES; c++) {
            double score = poids[c][0];
            for (int f = 0; f < N_FEATURES; f++) score += poids[c][f+1] * features[f];
            scores[c] = sigmoid(score);
        }
        double somme = 0;
        for (double s : scores) somme += s;
        if (somme == 0) somme = 1;
        double[] probas = new double[N_CLASSES];
        for (int c = 0; c < N_CLASSES; c++) probas[c] = scores[c] / somme;
        return probas;
    }

    private void genererDonnees(double[][] X, int[] y) {
        Random rng = new Random(42);
        int idx = 0;
        // Faible
        for (int i = 0; i < 180 && idx < N_TRAIN; i++, idx++) {
            X[idx] = extraireFeatures(15 + rng.nextDouble()*30, rng.nextDouble()*20, 40+rng.nextDouble()*50, rng.nextInt(24));
            y[idx] = 0;
        }
        // Modere
        for (int i = 0; i < 150 && idx < N_TRAIN; i++, idx++) {
            X[idx] = extraireFeatures(40+rng.nextDouble()*20, 15+rng.nextDouble()*30, 20+rng.nextDouble()*30, 8+rng.nextInt(10));
            y[idx] = 1;
        }
        // Eleve
        for (int i = 0; i < 150 && idx < N_TRAIN; i++, idx++) {
            X[idx] = extraireFeatures(55+rng.nextDouble()*15, 35+rng.nextDouble()*40, 10+rng.nextDouble()*20, 10+rng.nextInt(8));
            y[idx] = 2;
        }
        // Critique
        for (int i = 0; i < 120 && idx < N_TRAIN; i++, idx++) {
            X[idx] = extraireFeatures(65+rng.nextDouble()*25, 60+rng.nextDouble()*90, 3+rng.nextDouble()*17, 11+rng.nextInt(6));
            y[idx] = 3;
        }
    }

    private void entrainer(double[][] X, int[] y) {
        Random rng = new Random(42);
        for (int c = 0; c < N_CLASSES; c++)
            for (int f = 0; f <= N_FEATURES; f++)
                poids[c][f] = (rng.nextDouble() - 0.5) * 0.1;

        for (int epoch = 0; epoch < EPOCHS; epoch++) {
            for (int idx : shuffleIndices(N_TRAIN, rng)) {
                double[] features = X[idx];
                int      vrai     = y[idx];
                double   lr       = LEARNING_RATE * (1.0 / (1 + epoch * 0.001));
                for (int c = 0; c < N_CLASSES; c++) {
                    int label = (vrai == c) ? 1 : 0;
                    double score = poids[c][0];
                    for (int f = 0; f < N_FEATURES; f++) score += poids[c][f+1] * features[f];
                    double erreur = sigmoid(score) - label;
                    poids[c][0] -= lr * erreur;
                    for (int f = 0; f < N_FEATURES; f++) poids[c][f+1] -= lr * erreur * features[f];
                }
            }
        }
    }

    private double sigmoid(double x) { return 1.0 / (1.0 + Math.exp(-Math.max(-500, Math.min(500, x)))); }
    private double normaliser(double val, double min, double max) { return Math.max(0, Math.min(1, (val-min)/(max-min))); }
    private int[] shuffleIndices(int n, Random rng) {
        int[] idx = new int[n];
        for (int i = 0; i < n; i++) idx[i] = i;
        for (int i = n-1; i > 0; i--) { int j = rng.nextInt(i+1); int t = idx[i]; idx[i] = idx[j]; idx[j] = t; }
        return idx;
    }
}
