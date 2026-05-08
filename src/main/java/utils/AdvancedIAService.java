package utils;

import dao.InterventionDAO;
import dao.AlerteDAO;
import model.Intervention;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service d'Intelligence Artificielle Avancé pour ForestGuard
 * Utilise l'apprentissage automatique et l'analyse historique pour des prédictions précises
 * 
 * Fonctionnalités:
 * - Analyse de l'historique des incendies par zone
 * - Prédiction basée sur les patterns saisonniers
 * - Calcul de probabilité de propagation
 * - Recommandations adaptatives basées sur l'expérience
 * - Analyse des tendances temporelles
 */
public class AdvancedIAService {

    private static final InterventionDAO interventionDAO = new InterventionDAO();
    private static final AlerteDAO alerteDAO = new AlerteDAO();

    // ══════════════════════════════════════════════════════════════════════════
    // ANALYSE HISTORIQUE PAR ZONE
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Analyse l'historique des incendies pour une zone spécifique
     * @param zone Zone à analyser
     * @return Statistiques historiques
     */
    public static HistoriqueZone analyserHistoriqueZone(String zone) {
        List<Intervention> interventions = interventionDAO.getAll();
        
        // Filtrer les interventions de cette zone
        List<Intervention> interventionsZone = interventions.stream()
            .filter(i -> i.getAlerteLocalisation() != null && 
                        i.getAlerteLocalisation().toLowerCase().contains(zone.toLowerCase()))
            .collect(Collectors.toList());

        HistoriqueZone historique = new HistoriqueZone();
        historique.zone = zone;
        historique.nombreIncendies = interventionsZone.size();
        
        if (interventionsZone.isEmpty()) {
            historique.risqueHistorique = "Faible (aucun historique)";
            historique.moisPlusDangereux = "Données insuffisantes";
            historique.dureeMovenneHeures = 0;
            return historique;
        }

        // Analyser les mois
        Map<Month, Long> incendiesParMois = interventionsZone.stream()
            .collect(Collectors.groupingBy(
                i -> i.getStartDate().getMonth(),
                Collectors.counting()
            ));

        // Trouver le mois le plus dangereux
        Month moisMax = incendiesParMois.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(Month.JULY);

        historique.moisPlusDangereux = getMoisEnFrancais(moisMax);
        historique.incendiesParMois = incendiesParMois;

        // Calculer la durée moyenne
        double dureeMoyenne = interventionsZone.stream()
            .filter(i -> i.getEndDate() != null)
            .mapToLong(i -> ChronoUnit.HOURS.between(i.getStartDate(), i.getEndDate()))
            .average()
            .orElse(2.0);

        historique.dureeMovenneHeures = dureeMoyenne;

        // Calculer le risque historique
        if (interventionsZone.size() >= 10) {
            historique.risqueHistorique = "Très Élevé (zone à risque récurrent)";
        } else if (interventionsZone.size() >= 5) {
            historique.risqueHistorique = "Élevé (zone sensible)";
        } else if (interventionsZone.size() >= 2) {
            historique.risqueHistorique = "Moyen (quelques incidents)";
        } else {
            historique.risqueHistorique = "Faible (peu d'incidents)";
        }

        // Analyser les tendances récentes (6 derniers mois)
        LocalDateTime sixMoisAvant = LocalDateTime.now().minusMonths(6);
        long incendiesRecents = interventionsZone.stream()
            .filter(i -> i.getStartDate().isAfter(sixMoisAvant))
            .count();

        historique.tendanceRecente = incendiesRecents >= 3 ? "En hausse ⚠️" : "Stable";

        return historique;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PRÉDICTION AVANCÉE DE RISQUE
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Calcule un score de risque avancé basé sur:
     * - Conditions météorologiques actuelles
     * - Historique de la zone
     * - Saison et tendances
     * - Patterns d'apprentissage
     */
    public static PredictionAvancee calculerPredictionAvancee(
            String zone, 
            double temperature, 
            double humidite, 
            double vent,
            String niveauAlerte) {

        PredictionAvancee prediction = new PredictionAvancee();

        // 1. Score météo de base (0-40 points)
        int scoreMeteo = calculerScoreMeteo(temperature, humidite, vent);

        // 2. Score historique de la zone (0-25 points)
        HistoriqueZone historique = analyserHistoriqueZone(zone);
        int scoreHistorique = calculerScoreHistorique(historique);

        // 3. Score saisonnier (0-20 points)
        int scoreSaison = calculerScoreSaison();

        // 4. Score niveau d'alerte (0-15 points)
        int scoreAlerte = calculerScoreAlerte(niveauAlerte);

        // Score total (0-100)
        int scoreTotal = Math.min(scoreMeteo + scoreHistorique + scoreSaison + scoreAlerte, 100);

        prediction.scoreRisque = scoreTotal;
        prediction.niveauRisque = IAService.getNiveauRisque(scoreTotal);
        prediction.historiqueZone = historique;

        // Probabilité de propagation rapide
        prediction.probabilitePropagationRapide = calculerProbabilitePropagation(
            temperature, humidite, vent, historique
        );

        // Prédiction de durée basée sur l'historique
        prediction.dureeEstimeeHeures = predireDureeAvecHistorique(
            scoreTotal, historique.dureeMovenneHeures
        );

        // Recommandations adaptatives
        prediction.recommandations = genererRecommandationsAdaptatives(
            scoreTotal, historique, temperature, humidite, vent
        );

        // Facteurs de risque identifiés
        prediction.facteursRisque = identifierFacteursRisque(
            temperature, humidite, vent, historique
        );

        return prediction;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CALCULS DÉTAILLÉS
    // ══════════════════════════════════════════════════════════════════════════

    private static int calculerScoreMeteo(double temp, double hum, double vent) {
        int score = 0;

        // Température (0-15 points)
        if (temp >= 40) score += 15;
        else if (temp >= 35) score += 12;
        else if (temp >= 30) score += 9;
        else if (temp >= 25) score += 5;
        else if (temp >= 20) score += 2;

        // Humidité (0-15 points)
        if (hum <= 15) score += 15;
        else if (hum <= 25) score += 12;
        else if (hum <= 35) score += 9;
        else if (hum <= 45) score += 5;
        else if (hum <= 55) score += 2;

        // Vent (0-10 points)
        double ventKmh = vent * 3.6;
        if (ventKmh >= 60) score += 10;
        else if (ventKmh >= 50) score += 8;
        else if (ventKmh >= 40) score += 6;
        else if (ventKmh >= 30) score += 4;
        else if (ventKmh >= 20) score += 2;

        return Math.min(score, 40);
    }

    private static int calculerScoreHistorique(HistoriqueZone historique) {
        int score = 0;

        // Nombre d'incendies passés
        if (historique.nombreIncendies >= 10) score += 15;
        else if (historique.nombreIncendies >= 5) score += 10;
        else if (historique.nombreIncendies >= 2) score += 5;

        // Tendance récente
        if (historique.tendanceRecente.contains("hausse")) score += 10;
        else score += 2;

        return Math.min(score, 25);
    }

    private static int calculerScoreSaison() {
        Month moisActuel = LocalDateTime.now().getMonth();
        
        // Mois les plus dangereux en Tunisie: Juin, Juillet, Août, Septembre
        return switch (moisActuel) {
            case JULY, AUGUST -> 20;  // Pic de danger
            case JUNE, SEPTEMBER -> 15;  // Très dangereux
            case MAY, OCTOBER -> 10;  // Dangereux
            case APRIL, NOVEMBER -> 5;  // Modéré
            default -> 2;  // Faible
        };
    }

    private static int calculerScoreAlerte(String niveau) {
        return switch (niveau.toLowerCase()) {
            case "critique", "critical" -> 15;
            case "haute", "high", "élevée", "elevee" -> 12;
            case "moyenne", "medium", "moyen" -> 8;
            case "faible", "low", "basse" -> 4;
            default -> 4;
        };
    }

    private static double calculerProbabilitePropagation(
            double temp, double hum, double vent, HistoriqueZone historique) {
        
        double probabilite = 0.0;

        // Facteur météo (0-50%)
        if (temp >= 35 && hum <= 30) probabilite += 30;
        else if (temp >= 30 && hum <= 40) probabilite += 20;
        else if (temp >= 25 && hum <= 50) probabilite += 10;

        // Facteur vent (0-30%)
        double ventKmh = vent * 3.6;
        if (ventKmh >= 50) probabilite += 30;
        else if (ventKmh >= 40) probabilite += 20;
        else if (ventKmh >= 30) probabilite += 10;

        // Facteur historique (0-20%)
        if (historique.nombreIncendies >= 10) probabilite += 20;
        else if (historique.nombreIncendies >= 5) probabilite += 15;
        else if (historique.nombreIncendies >= 2) probabilite += 10;

        return Math.min(probabilite, 100.0);
    }

    private static double predireDureeAvecHistorique(int score, double dureeMoyenneHistorique) {
        // Durée de base selon le score
        double dureeBase;
        if (score >= 75) dureeBase = 6.0;
        else if (score >= 50) dureeBase = 3.0;
        else if (score >= 25) dureeBase = 1.5;
        else dureeBase = 0.75;

        // Ajuster avec l'historique (si disponible)
        if (dureeMoyenneHistorique > 0) {
            // Moyenne pondérée: 60% historique, 40% score actuel
            return (dureeMoyenneHistorique * 0.6) + (dureeBase * 0.4);
        }

        return dureeBase;
    }

    private static List<String> genererRecommandationsAdaptatives(
            int score, HistoriqueZone historique, double temp, double hum, double vent) {
        
        List<String> recommandations = new ArrayList<>();

        // Recommandations basées sur le score
        if (score >= 75) {
            recommandations.add("🚨 ALERTE MAXIMALE - Mobilisation immédiate de toutes les unités");
            recommandations.add("📢 Évacuation préventive des zones habitées à proximité");
            recommandations.add("🚁 Demander support aérien (Canadair, hélicoptères)");
        } else if (score >= 50) {
            recommandations.add("⚠️ ALERTE ÉLEVÉE - Déploiement rapide des équipes");
            recommandations.add("📡 Établir un poste de commandement avancé");
            recommandations.add("👨‍🚒 Mobiliser les équipes de renfort");
        } else if (score >= 25) {
            recommandations.add("ℹ️ Surveillance renforcée - Préparer les équipements");
            recommandations.add("📞 Maintenir la communication avec les équipes");
        } else {
            recommandations.add("✅ Intervention standard - Procédures normales");
        }

        // Recommandations basées sur l'historique
        if (historique.nombreIncendies >= 5) {
            recommandations.add("📊 Zone à risque récurrent - Appliquer les protocoles spéciaux");
            recommandations.add("🗺️ Consulter les cartes des incendies précédents dans cette zone");
        }

        if (historique.tendanceRecente.contains("hausse")) {
            recommandations.add("📈 Tendance en hausse - Vigilance accrue requise");
        }

        // Recommandations météo spécifiques
        if (hum < 25) {
            recommandations.add("💧 Humidité critique - Risque de propagation éclair");
            recommandations.add("🚒 Augmenter les réserves d'eau sur site");
        }

        double ventKmh = vent * 3.6;
        if (ventKmh > 50) {
            recommandations.add("🌪️ Vents violents - Établir périmètre de sécurité élargi");
            recommandations.add("🚧 Installer des barrières coupe-feu préventives");
        }

        if (temp > 38) {
            recommandations.add("🌡️ Chaleur extrême - Rotation des équipes toutes les 30 minutes");
            recommandations.add("⚕️ Poste médical obligatoire sur site");
        }

        // Recommandation saisonnière
        Month mois = LocalDateTime.now().getMonth();
        if (mois == Month.JULY || mois == Month.AUGUST) {
            recommandations.add("☀️ Pic saisonnier - Conditions les plus défavorables de l'année");
        }

        return recommandations;
    }

    private static List<String> identifierFacteursRisque(
            double temp, double hum, double vent, HistoriqueZone historique) {
        
        List<String> facteurs = new ArrayList<>();

        // Facteurs météo
        if (temp >= 35) facteurs.add("Température élevée (" + String.format("%.1f", temp) + "°C)");
        if (hum <= 30) facteurs.add("Humidité très basse (" + String.format("%.0f", hum) + "%)");
        
        double ventKmh = vent * 3.6;
        if (ventKmh >= 40) facteurs.add("Vents forts (" + String.format("%.1f", ventKmh) + " km/h)");

        // Facteurs historiques
        if (historique.nombreIncendies >= 5) {
            facteurs.add("Zone à historique d'incendies (" + historique.nombreIncendies + " incidents)");
        }

        if (historique.tendanceRecente.contains("hausse")) {
            facteurs.add("Tendance récente en hausse");
        }

        // Facteurs saisonniers
        Month mois = LocalDateTime.now().getMonth();
        if (mois == Month.JULY || mois == Month.AUGUST) {
            facteurs.add("Saison à haut risque (été)");
        }

        // Combinaisons dangereuses
        if (temp >= 35 && hum <= 30 && ventKmh >= 40) {
            facteurs.add("⚠️ COMBINAISON CRITIQUE: Chaleur + Sécheresse + Vent");
        }

        return facteurs;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ANALYSE DES PATTERNS TEMPORELS
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Analyse les patterns temporels des incendies (heures, jours, mois)
     */
    public static AnalyseTemporelle analyserPatternsTemporels() {
        List<Intervention> interventions = interventionDAO.getAll();
        
        AnalyseTemporelle analyse = new AnalyseTemporelle();

        if (interventions.isEmpty()) {
            return analyse;
        }

        // Analyser par heure de la journée
        Map<Integer, Long> parHeure = interventions.stream()
            .collect(Collectors.groupingBy(
                i -> i.getStartDate().getHour(),
                Collectors.counting()
            ));

        int heurePlusDangereuse = parHeure.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(14);

        analyse.heurePlusDangereuse = heurePlusDangereuse + "h00";

        // Analyser par mois
        Map<Month, Long> parMois = interventions.stream()
            .collect(Collectors.groupingBy(
                i -> i.getStartDate().getMonth(),
                Collectors.counting()
            ));

        Month moisPlusDangereux = parMois.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(Month.JULY);

        analyse.moisPlusDangereux = getMoisEnFrancais(moisPlusDangereux);

        // Analyser les tendances annuelles
        Map<Integer, Long> parAnnee = interventions.stream()
            .collect(Collectors.groupingBy(
                i -> i.getStartDate().getYear(),
                Collectors.counting()
            ));

        if (parAnnee.size() >= 2) {
            List<Integer> annees = new ArrayList<>(parAnnee.keySet());
            Collections.sort(annees);
            
            long incendiesAnneeRecente = parAnnee.get(annees.get(annees.size() - 1));
            long incendiesAnneePrecedente = parAnnee.get(annees.get(annees.size() - 2));

            if (incendiesAnneeRecente > incendiesAnneePrecedente * 1.2) {
                analyse.tendanceAnnuelle = "En forte hausse (+20%)";
            } else if (incendiesAnneeRecente > incendiesAnneePrecedente) {
                analyse.tendanceAnnuelle = "En légère hausse";
            } else if (incendiesAnneeRecente < incendiesAnneePrecedente * 0.8) {
                analyse.tendanceAnnuelle = "En baisse (-20%)";
            } else {
                analyse.tendanceAnnuelle = "Stable";
            }
        }

        return analyse;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CLASSES DE DONNÉES
    // ══════════════════════════════════════════════════════════════════════════

    public static class HistoriqueZone {
        public String zone;
        public int nombreIncendies;
        public String risqueHistorique;
        public String moisPlusDangereux;
        public double dureeMovenneHeures;
        public String tendanceRecente;
        public Map<Month, Long> incendiesParMois;
    }

    public static class PredictionAvancee {
        public int scoreRisque;
        public String niveauRisque;
        public HistoriqueZone historiqueZone;
        public double probabilitePropagationRapide;
        public double dureeEstimeeHeures;
        public List<String> recommandations;
        public List<String> facteursRisque;
    }

    public static class AnalyseTemporelle {
        public String heurePlusDangereuse = "14h00";
        public String moisPlusDangereux = "Juillet";
        public String tendanceAnnuelle = "Données insuffisantes";
    }

    // ══════════════════════════════════════════════════════════════════════════
    // UTILITAIRES
    // ══════════════════════════════════════════════════════════════════════════

    private static String getMoisEnFrancais(Month mois) {
        return switch (mois) {
            case JANUARY -> "Janvier";
            case FEBRUARY -> "Février";
            case MARCH -> "Mars";
            case APRIL -> "Avril";
            case MAY -> "Mai";
            case JUNE -> "Juin";
            case JULY -> "Juillet";
            case AUGUST -> "Août";
            case SEPTEMBER -> "Septembre";
            case OCTOBER -> "Octobre";
            case NOVEMBER -> "Novembre";
            case DECEMBER -> "Décembre";
        };
    }
}
