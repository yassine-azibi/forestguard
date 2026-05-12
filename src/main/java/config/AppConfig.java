package config;

import edu.pompier.entities.Pompier;

/**
 * Configuration globale de l'application ForestGuard
 * Stocke les informations du pompier connecté accessible depuis tous les modules
 */
public class AppConfig {
    private static AppConfig instance;
    private Pompier pompierConnecte;
    
    private AppConfig() {}
    
    /**
     * Récupère l'instance unique de AppConfig (Singleton)
     */
    public static AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }
    
    /**
     * Récupère le pompier actuellement connecté
     */
    public Pompier getPompierConnecte() {
        return pompierConnecte;
    }
    
    /**
     * Définit le pompier connecté
     */
    public void setPompierConnecte(Pompier pompier) {
        this.pompierConnecte = pompier;
        if (pompier != null) {
            System.out.println("✅ Pompier connecté: " + pompier.getNom() + " " + pompier.getPrenom());
        }
    }
    
    /**
     * Récupère l'ID du pompier connecté
     */
    public int getIdPompier() {
        return pompierConnecte != null ? pompierConnecte.getId() : 0;
    }
    
    /**
     * Récupère le nom complet du pompier connecté
     */
    public String getNomPompier() {
        if (pompierConnecte != null) {
            return pompierConnecte.getNom() + " " + pompierConnecte.getPrenom();
        }
        return "Utilisateur";
    }
    
    /**
     * Récupère le nom du pompier connecté
     */
    public String getNom() {
        return pompierConnecte != null ? pompierConnecte.getNom() : "";
    }
    
    /**
     * Récupère le prénom du pompier connecté
     */
    public String getPrenom() {
        return pompierConnecte != null ? pompierConnecte.getPrenom() : "";
    }
    
    /**
     * Récupère l'email du pompier connecté
     */
    public String getEmail() {
        return pompierConnecte != null ? pompierConnecte.getEmail() : "";
    }
    
    /**
     * Vérifie si un pompier est connecté
     */
    public boolean estConnecte() {
        return pompierConnecte != null;
    }
    
    /**
     * Déconnecte le pompier actuel
     */
    public void deconnecter() {
        if (pompierConnecte != null) {
            System.out.println("👋 Déconnexion de: " + getNomPompier());
        }
        pompierConnecte = null;
    }
}
