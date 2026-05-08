package utils;

import dao.AffectationDAO;
import model.Affectation;
import javafx.application.Platform;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Service de surveillance des affectations en arrière-plan
 * Vérifie toutes les 30 secondes s'il y a de nouvelles affectations
 */
public class AffectationService {
    
    private Thread thread;
    private volatile boolean running = false;
    private final int idPompier;
    private final AffectationDAO affectationDAO;
    private final Set<Integer> affectationsTraitees;
    private Consumer<Affectation> onNouvelleAffectation;
    
    /**
     * Constructeur
     * @param idPompier ID du pompier connecté
     */
    public AffectationService(int idPompier) {
        this.idPompier = idPompier;
        this.affectationDAO = new AffectationDAO();
        this.affectationsTraitees = new HashSet<>();
    }
    
    /**
     * Définit le callback à appeler quand une nouvelle affectation est détectée
     * @param callback Consumer qui reçoit l'affectation
     */
    public void setOnNouvelleAffectation(Consumer<Affectation> callback) {
        this.onNouvelleAffectation = callback;
    }
    
    /**
     * Démarre le service de surveillance
     */
    public void demarrer() {
        if (running) {
            System.out.println("⚠️ AffectationService déjà démarré");
            return;
        }
        
        running = true;
        
        thread = new Thread(() -> {
            System.out.println("🚀 AffectationService démarré pour pompier ID: " + idPompier);
            
            while (running) {
                try {
                    // Vérifier les nouvelles affectations
                    verifierAffectations();
                    
                    // Attendre 30 secondes avant la prochaine vérification
                    Thread.sleep(30000); // 30 secondes
                    
                } catch (InterruptedException e) {
                    System.out.println("🛑 AffectationService interrompu");
                    break;
                } catch (Exception e) {
                    System.out.println("❌ Erreur dans AffectationService: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            System.out.println("🛑 AffectationService arrêté");
        });
        
        // Thread daemon pour qu'il s'arrête automatiquement avec l'application
        thread.setDaemon(true);
        thread.setName("AffectationService-Thread");
        thread.start();
    }
    
    /**
     * Arrête le service de surveillance
     */
    public void arreter() {
        if (!running) {
            return;
        }
        
        running = false;
        
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
        
        System.out.println("🛑 AffectationService arrêté pour pompier ID: " + idPompier);
    }
    
    /**
     * Vérifie s'il y a de nouvelles affectations
     */
    private void verifierAffectations() {
        try {
            List<Affectation> affectations = affectationDAO.getAffectationsEnCours(idPompier);
            
            if (affectations.isEmpty()) {
                return;
            }
            
            for (Affectation affectation : affectations) {
                // Vérifier si cette affectation n'a pas déjà été traitée
                if (!affectationsTraitees.contains(affectation.getId())) {
                    // Marquer comme traitée
                    affectationsTraitees.add(affectation.getId());
                    
                    // Notifier sur le thread JavaFX
                    if (onNouvelleAffectation != null) {
                        Platform.runLater(() -> {
                            onNouvelleAffectation.accept(affectation);
                        });
                    }
                    
                    System.out.println("🔔 Nouvelle affectation détectée: " + affectation);
                }
            }
            
            // Nettoyer les anciennes affectations traitées (garder max 100)
            if (affectationsTraitees.size() > 100) {
                affectationsTraitees.clear();
                System.out.println("🧹 Cache des affectations traitées nettoyé");
            }
            
        } catch (Exception e) {
            System.out.println("❌ Erreur verifierAffectations: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Marque une affectation comme traitée manuellement
     * @param idAffectation ID de l'affectation
     */
    public void marquerCommeTraitee(int idAffectation) {
        affectationsTraitees.add(idAffectation);
    }
    
    /**
     * Vérifie si le service est en cours d'exécution
     * @return true si le service tourne
     */
    public boolean isRunning() {
        return running;
    }
    
    /**
     * Obtient le nombre d'affectations traitées
     * @return Nombre d'affectations dans le cache
     */
    public int getNombreAffectationsTraitees() {
        return affectationsTraitees.size();
    }
}
