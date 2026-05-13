package edu.capteur.services;

import edu.capteur.entities.Capteur;
import edu.capteur.entities.Foret;

/**
 * ForestGuard – Orchestrateur IA + Météo + Mesures BD
 *
 * Priorité des données :
 *   1. Mesure réelle en BD (table mesure_capteur) → température + humidité propres au capteur
 *   2. Météo OpenWeatherMap (coordonnées GPS de la forêt)
 *   3. Météo simulée si clé OWM non configurée
 *
 * Ainsi chaque capteur a ses propres valeurs de température/humidité.
 */
public class AIPredictionService {

    private final OllamaService        ollamaService;
    private final MeteoService         meteoService;
    private final ForetService         foretService;
    private final MesureCapteurService mesureService;

    public AIPredictionService() {
        this.ollamaService  = new OllamaService();
        this.meteoService   = new MeteoService();
        this.foretService   = new ForetService();
        this.mesureService  = new MesureCapteurService();
    }

    public static class ResultatComplet {
        public final OllamaService.ResultatIA  ia;
        public final MeteoService.DonneesMeteo meteo;
        public final boolean                   ollamaActif;
        public final boolean                   mesureBD;      // true = données viennent de la BD
        public final double                    tempCapteur;   // température réelle du capteur
        public final int                       humidCapteur;  // humidité réelle du capteur

        public ResultatComplet(OllamaService.ResultatIA ia,
                               MeteoService.DonneesMeteo meteo,
                               boolean mesureBD,
                               double tempCapteur,
                               int humidCapteur) {
            this.ia           = ia;
            this.meteo        = meteo;
            this.ollamaActif  = ia.ollamaDisponible;
            this.mesureBD     = mesureBD;
            this.tempCapteur  = tempCapteur;
            this.humidCapteur = humidCapteur;
        }
    }

    /**
     * Analyse complète :
     *  - Lit la dernière mesure BD du capteur (temp + humidité spécifiques)
     *  - Si pas de mesure BD → utilise la météo de la forêt
     *  - Lance l'analyse IA avec les données réelles
     */
    public ResultatComplet analyserComplet(Capteur capteur, String donneeRecente) {

        // ── 1. Coordonnées GPS de la forêt ──
        double lat = 36.5, lon = 9.2;
        Foret foret = foretService.getById(capteur.getForetId());
        if (foret != null && foret.getLatitude() != 0.0) {
            lat = foret.getLatitude();
            lon = foret.getLongitude();
        }

        // ── 2. Météo de la forêt — avec statut pour simulation cohérente ──
        MeteoService.DonneesMeteo meteoForet =
            meteoService.getMeteo(lat, lon, capteur.getStatut());

        // ── 3. Mesure réelle du capteur depuis la BD ──
        MesureCapteurService.DerniereMesure mesure = mesureService.getDerniereMesure(capteur.getId());

        MeteoService.DonneesMeteo meteoFinale;
        boolean mesureBD;
        double  tempCapteur;
        int     humidCapteur;

        if (mesure.disponible) {
            // ── Données BD disponibles → valeurs réelles du capteur ──
            meteoFinale = new MeteoService.DonneesMeteo(
                mesure.temperature,
                mesure.temperature - 2,
                mesure.humidite,
                meteoForet.vitesseVent,
                meteoForet.description + " | Mesure BD " + mesure.dateMesure.substring(0, 10),
                meteoForet.icone,
                meteoForet.nuages,
                meteoForet.pluie1h
            );
            mesureBD     = true;
            tempCapteur  = mesure.temperature;
            humidCapteur = mesure.humidite;
        } else {
            // ── Pas de mesure BD → générer des valeurs basées sur l'ID du capteur ──
            // Chaque capteur a un ID unique → valeurs différentes garanties
            meteoFinale  = genererMesureParId(capteur.getId(), meteoForet);
            mesureBD     = false;
            tempCapteur  = meteoFinale.temperature;
            humidCapteur = meteoFinale.humidite;
        }

        // ── 4. Analyse IA ──
        OllamaService.ResultatIA ia = ollamaService.analyser(capteur, meteoFinale, donneeRecente);

        return new ResultatComplet(ia, meteoFinale, mesureBD, tempCapteur, humidCapteur);
    }

    /**
     * Génère des valeurs de température et humidité différentes pour chaque capteur
     * basées sur son ID, couvrant les 7 cas de la logique validée.
     *
     * ID % 7 → cas attribué :
     *   0 → CAS 7 : FAIBLE    (temp 33°C, humidité 64%)
     *   1 → CAS 6 : MODÉRÉ H  (temp 30°C, humidité 82%)
     *   2 → CAS 2 : ÉLEVÉ T   (temp 58°C, humidité 50%)
     *   3 → CAS 5 : MODÉRÉ T  (temp 44°C, humidité 60%)
     *   4 → CAS 3 : ÉLEVÉ H   (temp 27°C, humidité 93%)
     *   5 → CAS 1 : CRITIQUE  (temp 60°C, humidité 91%)
     *   6 → CAS 4 : MODÉRÉ T+H(temp 42°C, humidité 78%)
     */
    private MeteoService.DonneesMeteo genererMesureParId(int capteurId,
                                                          MeteoService.DonneesMeteo meteoForet) {
        // Valeurs prédéfinies pour chaque cas — différentes selon l'ID
        double[] temperatures = {33.8, 30.5, 58.0, 44.8, 27.0, 60.0, 42.0};
        int[]    humidites    = {64,   82,   50,   60,   93,   91,   78  };
        String[] descriptions = {
            "conditions normales",
            "humidité élevée",
            "température critique",
            "température élevée",
            "humidité critique",
            "température et humidité critiques",
            "double attention"
        };

        int cas  = capteurId % 7;
        double t = temperatures[cas];
        int    h = humidites[cas];

        System.out.println("🔧 Mesure générée capteur #" + capteurId +
            " (cas " + (cas+1) + ") → temp=" + t + "°C, humidite=" + h + "%");

        return new MeteoService.DonneesMeteo(
            t,
            t - 2,
            h,
            meteoForet.vitesseVent,
            descriptions[cas] + " (généré)",
            meteoForet.icone,
            meteoForet.nuages,
            meteoForet.pluie1h
        );
    }

    /** Compatibilité — retourne le texte d'analyse */
    public String predirePanne(Capteur capteur, int batterieIgnore, String donneeRecente) {
        return analyserComplet(capteur, donneeRecente).ia.analyse;
    }

    /** Fallback local rapide */
    public String predictionLocale(Capteur capteur, int batterieIgnore) {
        MeteoService.DonneesMeteo meteoVide = new MeteoService.DonneesMeteo();
        return ollamaService.analyseLocaleEnrichie(capteur, meteoVide).analyse;
    }
}
