package edu.capteur.tests;

import edu.capteur.entities.Capteur;
import edu.capteur.entities.Foret;
import edu.capteur.entities.Maintenance;
import edu.capteur.services.CapteurService;
import edu.capteur.services.ForetService;
import edu.capteur.services.MaintenanceService;

public class MainClass {
    public static void main(String[] args) {

        ForetService   fs = new ForetService();
        CapteurService cs = new CapteurService();
        MaintenanceService ms = new MaintenanceService();

        // ✅ Ajouter les forêts (INSERT IGNORE pour éviter les doublons)
        fs.ajouterSiAbsent(new Foret("Forêt de Ain Draham",  "Jendouba",  12500.0, "Chêne-liège",  "élevé",  "2010-03-15", 36.7823,  8.6897));
        fs.ajouterSiAbsent(new Foret("Forêt de Tabarka",     "Jendouba",   9800.0, "Pin maritime", "élevé",  "2009-05-10", 36.9543,  8.7567));
        fs.ajouterSiAbsent(new Foret("Forêt de Feija",       "Jendouba",   6800.0, "Chêne-liège",  "élevé",  "2012-01-10", 36.5500,  8.3200));
        fs.ajouterSiAbsent(new Foret("Forêt de Nefza",       "Béja",       7600.0, "Eucalyptus",   "faible", "2011-04-22", 37.0300,  9.0500));
        fs.ajouterSiAbsent(new Foret("Forêt de Amdoun",      "Béja",       9400.0, "Pin d'Alep",   "moyen",  "2009-09-05", 36.6800,  9.0200));
        fs.ajouterSiAbsent(new Foret("Forêt de Ichkeul",     "Bizerte",    8900.0, "Mixte",        "élevé",  "2007-11-30", 37.1500,  9.6800));
        fs.ajouterSiAbsent(new Foret("Forêt de Zaghouan",    "Zaghouan",   4200.0, "Pin d'Alep",   "moyen",  "2014-02-14", 36.4027, 10.1434));
        fs.ajouterSiAbsent(new Foret("Forêt de Siliana",     "Siliana",    6100.0, "Pin d'Alep",   "faible", "2010-08-08", 36.0844,  9.3700));
        fs.ajouterSiAbsent(new Foret("Forêt de Kasserine",   "Kasserine",  7800.0, "Pin d'Alep",   "élevé",  "2006-05-25", 35.1676,  8.8365));
        fs.ajouterSiAbsent(new Foret("Forêt de Bou Hedma",   "Gafsa",      5200.0, "Acacia",       "moyen",  "2008-03-12", 34.5500,  9.5200));

        // ✅ Afficher toutes les forêts
        System.out.println("--- Liste des forêts ---");
        fs.afficher().forEach(f -> System.out.println(f.getNom() + " | " + f.getLocalisation() + " | " + f.getNiveauRisque()));

        // ✅ Ajouter plusieurs capteurs (1 par type par forêt)
        cs.ajouter(new Capteur("Capteur Ain Draham - Temp",  "temperature", "Ain Draham",  "actif",    1));
        cs.ajouter(new Capteur("Capteur Ain Draham - Humid", "humidite",    "Ain Draham",  "actif",    1));
        cs.ajouter(new Capteur("Capteur Ain Draham - Fumee", "fumee",       "Ain Draham",  "en_panne", 1));
        cs.ajouter(new Capteur("Capteur Tabarka - Temp",     "temperature", "Tabarka",     "actif",    2));
        cs.ajouter(new Capteur("Capteur Tabarka - Humid",    "humidite",    "Tabarka",     "inactif",  2));
        cs.ajouter(new Capteur("Capteur Tabarka - Fumee",    "fumee",       "Tabarka",     "actif",    2));

        // ✅ Afficher tous les capteurs
        System.out.println("--- Liste des capteurs ---");
        cs.afficher().forEach(System.out::println);

        // ✅ Ajouter plusieurs maintenances (basées sur les capteurs réels de la BD)
        // Types valides    : preventive | corrective | remplacement
        // Statuts valides  : planifiee  | en_cours   | terminee
        // Descriptions     : issues de DescriptionsMaintenance
        ms.ajouter(new Maintenance("2026-05-01", "preventive",   "terminee",  "Inspection capteur",            1));
        ms.ajouter(new Maintenance("2026-05-03", "corrective",   "terminee",  "Réparation panne",              3));
        ms.ajouter(new Maintenance("2026-05-05", "preventive",   "en_cours",  "Nettoyage capteur",             2));
        ms.ajouter(new Maintenance("2026-05-07", "remplacement", "terminee",  "Remplacement capteur",          5));
        ms.ajouter(new Maintenance("2026-05-10", "preventive",   "planifiee", "Vérification connexions",       4));
        ms.ajouter(new Maintenance("2026-05-12", "corrective",   "terminee",  "Réparation câblage",            9));
        ms.ajouter(new Maintenance("2026-05-14", "preventive",   "planifiee", "Recalibrage capteur",           7));
        ms.ajouter(new Maintenance("2026-05-16", "corrective",   "en_cours",  "Correction anomalie mesure",    6));
        ms.ajouter(new Maintenance("2026-05-18", "preventive",   "planifiee", "Contrôle boîtier",              10));
        ms.ajouter(new Maintenance("2026-05-20", "remplacement", "terminee",  "Remplacement module",           8));
        ms.ajouter(new Maintenance("2026-05-22", "preventive",   "planifiee", "Test fonctionnement",           11));
        ms.ajouter(new Maintenance("2026-05-24", "corrective",   "terminee",  "Remise en service",             12));
        ms.ajouter(new Maintenance("2026-05-26", "preventive",   "planifiee", "Mise à jour firmware",          13));
        ms.ajouter(new Maintenance("2026-05-28", "corrective",   "en_cours",  "Remplacement capteur défectueux", 14));
        ms.ajouter(new Maintenance("2026-05-30", "preventive",   "planifiee", "Remplacement batterie",         15));

        // ✅ Afficher toutes les maintenances
        System.out.println("--- Liste des maintenances ---");
        ms.afficher().forEach(System.out::println);
    }
}
