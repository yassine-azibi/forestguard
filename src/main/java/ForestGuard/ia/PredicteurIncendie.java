package ForestGuard.ia;

import ForestGuard.entities.DonCapteur;
import java.util.List;


public class PredicteurIncendie {

    // ── Seuils critiques ─────────────────────────────────────
    private static final double SEUIL_TEMP_CRITIQUE  = 35.0;
    private static final double SEUIL_TEMP_ELEVE     = 30.0;
    private static final double SEUIL_HUM_CRITIQUE   = 25.0;
    private static final double SEUIL_HUM_ELEVE      = 40.0;
    private static final double SEUIL_FUMEE_CRITIQUE = 70.0;
    private static final double SEUIL_FUMEE_ELEVE    = 30.0;

    // ── Poids du modèle (calibrés sur données forestières) ──
    private static final double POIDS_TEMP   = 0.40;
    private static final double POIDS_HUM    = 0.35;
    private static final double POIDS_FUMEE  = 0.25;

    // ────────────────────────────────────────────────────────
    // Résultat de la prédiction
    // ────────────────────────────────────────────────────────
    public static class ResultatPrediction {
        public final String  niveauRisque;   // "FAIBLE", "MOYEN", "ELEVE", "CRITIQUE"
        public final double  scoreRisque;    // 0.0 → 1.0
        public final int     pourcentage;    // 0 → 100
        public final String  couleur;        // couleur hex
        public final String  emoji;
        public final String  messageIA;      // explication
        public final String  prediction2h;   // prédiction dans 2h
        public final String  recommandation;

        public ResultatPrediction(String niveau, double score,
                                  String couleur, String emoji,
                                  String message, String pred2h, String recommandation) {
            this.niveauRisque   = niveau;
            this.scoreRisque    = score;
            this.pourcentage    = (int)(score * 100);
            this.couleur        = couleur;
            this.emoji          = emoji;
            this.messageIA      = message;
            this.prediction2h   = pred2h;
            this.recommandation = recommandation;
        }
    }

    // ────────────────────────────────────────────────────────
    // Prédiction principale
    // ────────────────────────────────────────────────────────
    public static ResultatPrediction predire(List<DonCapteur> donnees) {
        if (donnees == null || donnees.isEmpty()) {
            return new ResultatPrediction("INCONNU", 0, "#94a3b8", "?",
                    "Donnees insuffisantes", "Impossible de predire", "Collecter plus de donnees");
        }

        // ── Moyennes actuelles ───────────────────────────────
        double avgTemp  = donnees.stream()
                .mapToDouble(DonCapteur::getTemperature).average().orElse(0);
        double avgHum   = donnees.stream()
                .mapToDouble(DonCapteur::getHumidite).average().orElse(0);
        double avgFumee = donnees.stream()
                .mapToDouble(DonCapteur::getFumee).average().orElse(0);

        // ── Tendances (comparaison 1ère moitié vs 2ème moitié) ─
        double tendanceTemp  = calculerTendance(donnees, "temperature");
        double tendanceHum   = calculerTendance(donnees, "humidite");
        double tendanceFumee = calculerTendance(donnees, "fumee");

        // ── Scores normalisés 0→1 ────────────────────────────
        double scoreTemp  = normaliserTemp(avgTemp);
        double scoreHum   = normaliserHum(avgHum);
        double scoreFumee = normaliserFumee(avgFumee);

        // ── Score final pondéré ──────────────────────────────
        double scoreFinal = (scoreTemp  * POIDS_TEMP) +
                (scoreHum   * POIDS_HUM)  +
                (scoreFumee * POIDS_FUMEE);

        // ── Bonus tendances (aggravation = +score) ───────────
        if (tendanceTemp  > 2)  scoreFinal += 0.08;
        if (tendanceHum   < -5) scoreFinal += 0.08;
        if (tendanceFumee > 5)  scoreFinal += 0.10;

        // Clamp 0→1
        scoreFinal = Math.min(1.0, Math.max(0.0, scoreFinal));

        // ── Prédiction dans 2h (projection linéaire) ─────────
        double tempFuture  = avgTemp  + (tendanceTemp  * 2);
        double humFuture   = avgHum   + (tendanceHum   * 2);
        double fumeeFuture = avgFumee + (tendanceFumee * 2);
        double scoreFutur  = (normaliserTemp(tempFuture)  * POIDS_TEMP) +
                (normaliserHum(humFuture)    * POIDS_HUM)  +
                (normaliserFumee(fumeeFuture)* POIDS_FUMEE);
        scoreFutur = Math.min(1.0, Math.max(0.0, scoreFutur));

        return construireResultat(scoreFinal, scoreFutur,
                avgTemp, avgHum, avgFumee,
                tendanceTemp, tendanceHum, tendanceFumee);
    }

    // ────────────────────────────────────────────────────────
    // Normalisation des valeurs
    // ────────────────────────────────────────────────────────
    private static double normaliserTemp(double t) {
        if (t >= SEUIL_TEMP_CRITIQUE) return 1.0;
        if (t <= 10) return 0.0;
        return (t - 10) / (SEUIL_TEMP_CRITIQUE - 10);
    }

    private static double normaliserHum(double h) {
        // Humidité faible = risque élevé
        if (h <= SEUIL_HUM_CRITIQUE) return 1.0;
        if (h >= 80) return 0.0;
        return 1.0 - ((h - SEUIL_HUM_CRITIQUE) / (80 - SEUIL_HUM_CRITIQUE));
    }

    private static double normaliserFumee(double f) {
        if (f >= SEUIL_FUMEE_CRITIQUE) return 1.0;
        if (f == 0) return 0.0;
        return f / SEUIL_FUMEE_CRITIQUE;
    }

    // ────────────────────────────────────────────────────────
    // Calcul de tendance (variation moyenne par mesure)
    // ────────────────────────────────────────────────────────
    private static double calculerTendance(List<DonCapteur> donnees, String champ) {
        int n = donnees.size();
        if (n < 2) return 0;
        int milieu = n / 2;
        double moyDebut = 0, moyFin = 0;
        for (int i = 0; i < milieu; i++) {
            DonCapteur d = donnees.get(i);
            moyDebut += valeur(d, champ);
        }
        for (int i = milieu; i < n; i++) {
            DonCapteur d = donnees.get(i);
            moyFin += valeur(d, champ);
        }
        moyDebut /= milieu;
        moyFin   /= (n - milieu);
        return moyFin - moyDebut;
    }

    private static double valeur(DonCapteur d, String champ) {
        return switch (champ) {
            case "temperature" -> d.getTemperature();
            case "humidite"    -> d.getHumidite();
            case "fumee"       -> d.getFumee();
            default            -> 0;
        };
    }

    // ────────────────────────────────────────────────────────
    // Construction du résultat final
    // ────────────────────────────────────────────────────────
    private static ResultatPrediction construireResultat(
            double score, double scoreFutur,
            double temp, double hum, double fumee,
            double dT, double dH, double dF) {

        String niveau, couleur, emoji, message, pred2h, reco;

        if (score >= 0.75) {
            niveau  = "CRITIQUE";
            couleur = "#dc2626";
            emoji   = "🔥";
            message = String.format(
                    "ALERTE CRITIQUE - Temp %.1fC, Hum %.0f%%, Fumee %.0f",
                    temp, hum, fumee);
            reco    = "Evacuation immediate recommandee !";
        } else if (score >= 0.50) {
            niveau  = "ELEVE";
            couleur = "#f97316";
            emoji   = "⚠";
            message = String.format(
                    "Risque eleve detecte - Temp %.1fC en hausse, Hum %.0f%% en baisse",
                    temp, hum);
            reco    = "Alerter les equipes de surveillance";
        } else if (score >= 0.25) {
            niveau  = "MOYEN";
            couleur = "#eab308";
            emoji   = "~";
            message = String.format(
                    "Conditions moderees - Temp %.1fC, Hum %.0f%%", temp, hum);
            reco    = "Augmenter la frequence de surveillance";
        } else {
            niveau  = "FAIBLE";
            couleur = "#16a34a";
            emoji   = "OK";
            message = String.format(
                    "Conditions normales - Temp %.1fC, Hum %.0f%%", temp, hum);
            reco    = "Surveillance standard suffisante";
        }

        // Prédiction 2h
        int pct2h = (int)(scoreFutur * 100);
        String tendStr = scoreFutur > score + 0.05 ? "en aggravation" :
                scoreFutur < score - 0.05 ? "en amelioration" : "stable";
        pred2h = String.format("Dans 2h : %s (%d%%) — %s",
                niveauFutur(scoreFutur), pct2h, tendStr);

        return new ResultatPrediction(niveau, score, couleur,
                emoji, message, pred2h, reco);
    }

    private static String niveauFutur(double s) {
        if (s >= 0.75) return "CRITIQUE";
        if (s >= 0.50) return "ELEVE";
        if (s >= 0.25) return "MOYEN";
        return "FAIBLE";
    }
    /**
     * Prédit le risque PAR FORÊT
     * Retourne une Map : nomForet → ResultatPrediction
     */
    public static java.util.Map<String, ResultatPrediction> predireParForet(
            List<DonCapteur> donnees) {

        java.util.Map<String, ResultatPrediction> resultats =
                new java.util.LinkedHashMap<>();

        if (donnees == null || donnees.isEmpty()) return resultats;

        // ── Grouper les données par forêt ─────────────────────
        java.util.Map<String, List<DonCapteur>> parForet = new java.util.LinkedHashMap<>();
        for (DonCapteur d : donnees) {
            String foret = d.getZone() != null ? d.getZone() : "Inconnue";
            parForet.computeIfAbsent(foret, k -> new java.util.ArrayList<>()).add(d);
        }

        // ── Prédire pour chaque forêt ─────────────────────────
        for (java.util.Map.Entry<String, List<DonCapteur>> entry : parForet.entrySet()) {
            String foret = entry.getKey();
            List<DonCapteur> donneesForet = entry.getValue();
            ResultatPrediction resultat = predire(donneesForet);
            resultats.put(foret, resultat);
        }

        // ── Trier par score décroissant (plus dangereux en premier) ─
        return resultats.entrySet().stream()
                .sorted((a, b) -> Double.compare(
                        b.getValue().scoreRisque,
                        a.getValue().scoreRisque))
                .collect(java.util.stream.Collectors.toMap(
                        java.util.Map.Entry::getKey,
                        java.util.Map.Entry::getValue,
                        (e1, e2) -> e1,
                        java.util.LinkedHashMap::new));
    }
}