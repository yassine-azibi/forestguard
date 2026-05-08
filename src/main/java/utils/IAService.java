package utils;

import model.Equipement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service d'Intelligence Artificielle pour l'analyse des risques d'incendie
 * Utilise des algorithmes avancés pour calculer les risques et fournir des recommandations
 */
public class IAService {

    // ── Score de risque amélioré ──────────────────────────────────────────────
    /**
     * Calcule un score de risque d'incendie basé sur plusieurs facteurs
     * @param temperature Température en °C
     * @param humidite Humidité en %
     * @param vent Vitesse du vent en m/s
     * @param niveauAlerte Niveau de l'alerte (critique, haute, moyenne, faible)
     * @return Score de risque entre 0 et 100
     */
    public static int calculerScoreRisque(
            double temperature, double humidite,
            double vent, String niveauAlerte) {

        // Score température (0-30 points)
        // Température > 30°C = risque élevé
        double scoreTemp = 0;
        if (temperature >= 35) {
            scoreTemp = 30;
        } else if (temperature >= 30) {
            scoreTemp = 25;
        } else if (temperature >= 25) {
            scoreTemp = 15;
        } else if (temperature >= 20) {
            scoreTemp = 5;
        }

        // Score humidité (0-30 points)
        // Humidité < 30% = risque élevé
        double scoreHumidite = 0;
        if (humidite <= 20) {
            scoreHumidite = 30;
        } else if (humidite <= 30) {
            scoreHumidite = 25;
        } else if (humidite <= 40) {
            scoreHumidite = 15;
        } else if (humidite <= 50) {
            scoreHumidite = 10;
        } else if (humidite <= 60) {
            scoreHumidite = 5;
        }

        // Score vent (0-20 points)
        // Vent > 40 km/h = risque élevé
        double ventKmh = vent * 3.6;
        double scoreVent = 0;
        if (ventKmh >= 50) {
            scoreVent = 20;
        } else if (ventKmh >= 40) {
            scoreVent = 15;
        } else if (ventKmh >= 30) {
            scoreVent = 10;
        } else if (ventKmh >= 20) {
            scoreVent = 5;
        }

        // Score alerte (0-20 points)
        double scoreAlerte = switch (niveauAlerte.toLowerCase()) {
            case "critique", "critical" -> 20;
            case "haute", "high", "élevée", "elevee" -> 15;
            case "moyenne", "medium", "moyen" -> 10;
            case "faible", "low", "basse" -> 5;
            default -> 5;
        };

        int score = (int) (scoreTemp + scoreHumidite + scoreVent + scoreAlerte);
        return Math.min(score, 100);
    }

    // ── Indice de Propagation (Fire Weather Index) ────────────────────────────
    /**
     * Calcule l'indice de propagation du feu (simplifié)
     * Basé sur le Canadian Forest Fire Weather Index System
     * @return Indice entre 0 et 100
     */
    public static int calculerIndicePropagation(double temperature, double humidite, double vent) {
        // Formule simplifiée du FWI
        double fwi = 0;
        
        // Facteur température (plus chaud = plus de propagation)
        double facteurTemp = Math.max(0, (temperature - 10) / 30.0);
        
        // Facteur humidité (plus sec = plus de propagation)
        double facteurHumidite = Math.max(0, (100 - humidite) / 100.0);
        
        // Facteur vent (plus de vent = plus de propagation)
        double ventKmh = vent * 3.6;
        double facteurVent = Math.min(1.0, ventKmh / 50.0);
        
        // Calcul de l'indice (pondération: temp 30%, humidité 40%, vent 30%)
        fwi = (facteurTemp * 30) + (facteurHumidite * 40) + (facteurVent * 30);
        
        return (int) Math.min(fwi, 100);
    }

    // ── Niveau de risque ───────────────────────────────────────────────────────
    public static String getNiveauRisque(int score) {
        if (score >= 75) return "Critique";
        if (score >= 50) return "Élevé";
        if (score >= 25) return "Moyen";
        return "Faible";
    }

    // ── Couleur du risque ──────────────────────────────────────────────────────
    public static String getCouleurRisque(int score) {
        if (score >= 75) return "#dc2626"; // rouge
        if (score >= 50) return "#f97316"; // orange
        if (score >= 25) return "#eab308"; // jaune
        return "#16a34a";                  // vert
    }

    // ── Suggestion équipements améliorée ───────────────────────────────────────
    public static List<String> suggererEquipements(
            int score, String typeAlerte, double vent) {

        List<String> suggestions = new ArrayList<>();

        // Équipement de base (toujours)
        suggestions.add("🚒 Camion Citerne (10 000L)");
        suggestions.add("🚑 Ambulance de secours");
        suggestions.add("👨‍🚒 Équipe de 6 pompiers minimum");

        // Risque moyen (score >= 25)
        if (score >= 25) {
            suggestions.add("🚒 Camion Citerne supplémentaire");
            suggestions.add("🔥 Lance à incendie haute pression");
            suggestions.add("📡 Équipement de communication");
        }

        // Risque élevé (score >= 50)
        if (score >= 50) {
            suggestions.add("🚁 Hélicoptère bombardier d'eau");
            suggestions.add("🚒 Camion Grande Échelle");
            suggestions.add("👨‍🚒 Équipe de renfort (12 pompiers)");
            suggestions.add("🔍 Drone de surveillance thermique");
        }

        // Risque critique (score >= 75)
        if (score >= 75) {
            suggestions.add("🚁 Canadair (avion bombardier)");
            suggestions.add("🚒 Unité de commandement mobile");
            suggestions.add("👨‍🚒 Équipes multiples (30+ pompiers)");
            suggestions.add("🚗 Véhicules d'évacuation");
            suggestions.add("⚕️ Poste médical avancé");
        }

        // Vents forts (> 30 km/h)
        double ventKmh = vent * 3.6;
        if (ventKmh > 30) {
            suggestions.add("🌬️ Équipe anti-propagation");
            suggestions.add("🚧 Barrières coupe-feu");
        }

        // Vents très forts (> 50 km/h)
        if (ventKmh > 50) {
            suggestions.add("⚠️ Équipe d'évacuation préventive");
            suggestions.add("📢 Système d'alerte population");
        }

        // Type d'alerte spécifique
        String typeLower = typeAlerte.toLowerCase();
        if (typeLower.contains("fumée") || typeLower.contains("fumee")) {
            suggestions.add("😷 Masques respiratoires (SCBA)");
            suggestions.add("🌫️ Ventilateurs extracteurs");
        }

        if (typeLower.contains("incendie")) {
            suggestions.add("🧯 Extincteurs CO2 et poudre");
            suggestions.add("🔥 Mousse anti-feu");
        }

        if (typeLower.contains("forêt") || typeLower.contains("foret")) {
            suggestions.add("🪓 Outils de débroussaillage");
            suggestions.add("💧 Système d'irrigation d'urgence");
        }

        return suggestions;
    }

    // ── Durée estimée améliorée ────────────────────────────────────────────────
    public static String estimerDuree(int score, String typeAlerte) {
        String typeLower = typeAlerte.toLowerCase();
        boolean incendieForet = typeLower.contains("forêt") || typeLower.contains("foret");
        
        if (score >= 75) {
            if (incendieForet) {
                return "8 à 24 heures (incendie majeur de forêt)";
            }
            return "4 à 8 heures (intervention majeure)";
        }
        
        if (score >= 50) {
            if (incendieForet) {
                return "4 à 8 heures (incendie important de forêt)";
            }
            return "2 à 4 heures (intervention importante)";
        }
        
        if (score >= 25) {
            return "1 à 2 heures (intervention modérée)";
        }
        
        return "30 min à 1 heure (intervention légère)";
    }

    // ── Recommandation détaillée ───────────────────────────────────────────────
    public static String getRecommandation(int score, double humidite, double vent) {
        StringBuilder rec = new StringBuilder();
        double ventKmh = vent * 3.6;

        // Niveau de risque principal
        if (score >= 75) {
            rec.append("🚨 DANGER CRITIQUE — Mobilisation maximale immédiate requise. ");
            rec.append("Évacuation préventive à considérer. ");
        } else if (score >= 50) {
            rec.append("⚠️ Risque élevé — Intervention rapide recommandée. ");
            rec.append("Surveillance continue nécessaire. ");
        } else if (score >= 25) {
            rec.append("ℹ️ Risque modéré — Surveillance renforcée conseillée. ");
            rec.append("Préparation des équipes recommandée. ");
        } else {
            rec.append("✅ Risque faible — Intervention standard suffisante. ");
        }

        // Facteurs aggravants
        List<String> facteurs = new ArrayList<>();
        
        if (humidite < 20) {
            facteurs.add("humidité critique (< 20%)");
        } else if (humidite < 30) {
            facteurs.add("humidité très basse (< 30%)");
        }
        
        if (ventKmh > 50) {
            facteurs.add("vents violents (> 50 km/h)");
        } else if (ventKmh > 40) {
            facteurs.add("vents forts (> 40 km/h)");
        } else if (ventKmh > 30) {
            facteurs.add("vents modérés (> 30 km/h)");
        }

        if (!facteurs.isEmpty()) {
            rec.append("\n\n⚠️ Facteurs aggravants: ");
            rec.append(String.join(", ", facteurs));
            rec.append(". Risque de propagation rapide.");
        }

        // Recommandations spécifiques
        rec.append("\n\n📋 Actions recommandées:\n");
        if (score >= 75) {
            rec.append("• Mobiliser toutes les unités disponibles\n");
            rec.append("• Établir un périmètre de sécurité large\n");
            rec.append("• Coordonner avec les autorités locales\n");
            rec.append("• Préparer l'évacuation si nécessaire");
        } else if (score >= 50) {
            rec.append("• Déployer les équipes rapidement\n");
            rec.append("• Établir un poste de commandement\n");
            rec.append("• Surveiller l'évolution de la situation");
        } else if (score >= 25) {
            rec.append("• Préparer les équipements\n");
            rec.append("• Surveiller les conditions météo\n");
            rec.append("• Maintenir la communication");
        } else {
            rec.append("• Intervention standard\n");
            rec.append("• Surveillance de routine");
        }

        return rec.toString();
    }

    // ── Prédiction de propagation ──────────────────────────────────────────────
    /**
     * Prédit la vitesse de propagation du feu en mètres par minute
     * @return Vitesse estimée en m/min
     */
    public static double predireVitessePropagation(double temperature, double humidite, double vent) {
        // Formule simplifiée basée sur le modèle de Rothermel
        double ventKmh = vent * 3.6;
        
        // Vitesse de base (conditions normales)
        double vitesseBase = 0.5; // m/min
        
        // Facteur température (augmente avec la chaleur)
        double facteurTemp = 1.0 + ((temperature - 20) / 50.0);
        
        // Facteur humidité (augmente quand c'est sec)
        double facteurHumidite = 1.0 + ((60 - humidite) / 100.0);
        
        // Facteur vent (impact majeur)
        double facteurVent = 1.0 + (ventKmh / 20.0);
        
        double vitesse = vitesseBase * facteurTemp * facteurHumidite * facteurVent;
        
        return Math.max(0.1, Math.min(vitesse, 50.0)); // Entre 0.1 et 50 m/min
    }

    // ── Estimation de la surface affectée ──────────────────────────────────────
    /**
     * Estime la surface qui pourrait être affectée en hectares
     * @param dureeHeures Durée de l'intervention en heures
     * @return Surface estimée en hectares
     */
    public static double estimerSurfaceAffectee(double temperature, double humidite, double vent, double dureeHeures) {
        double vitessePropagation = predireVitessePropagation(temperature, humidite, vent);
        
        // Conversion en mètres parcourus
        double distanceMetres = vitessePropagation * dureeHeures * 60;
        
        // Surface approximative (cercle)
        double rayonMetres = distanceMetres;
        double surfaceM2 = Math.PI * rayonMetres * rayonMetres;
        
        // Conversion en hectares
        double surfaceHectares = surfaceM2 / 10000.0;
        
        return Math.round(surfaceHectares * 10) / 10.0; // Arrondi à 1 décimale
    }

    // ── Heure optimale d'intervention ──────────────────────────────────────────
    /**
     * Détermine si c'est le moment optimal pour intervenir
     * @return Message sur le timing de l'intervention
     */
    public static String getTimingIntervention() {
        LocalDateTime now = LocalDateTime.now();
        int heure = now.getHour();
        
        // Conditions optimales: tôt le matin ou tard le soir
        if (heure >= 5 && heure < 9) {
            return "⏰ Timing optimal: Conditions matinales favorables (humidité élevée, température basse)";
        } else if (heure >= 18 && heure < 22) {
            return "⏰ Timing favorable: Conditions du soir (température en baisse, vent diminué)";
        } else if (heure >= 11 && heure < 16) {
            return "⚠️ Timing difficile: Pic de chaleur et conditions défavorables (prudence accrue requise)";
        } else if (heure >= 22 || heure < 5) {
            return "🌙 Intervention nocturne: Visibilité réduite mais conditions météo favorables";
        } else {
            return "⏰ Timing standard: Conditions normales d'intervention";
        }
    }
}