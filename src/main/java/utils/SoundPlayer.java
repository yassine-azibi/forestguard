package utils;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.net.URL;

/**
 * Gestionnaire de son pour les notifications et affectations
 * Joue le son notif.mp3 en boucle pendant l'affichage des popups
 */
public class SoundPlayer {
    
    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;
    
    /**
     * Démarre la lecture du son en boucle
     */
    public void demarrerSon() {
        if (isPlaying) {
            System.out.println("⚠️ Son déjà en cours de lecture");
            return;
        }
        
        try {
            // Charger le fichier audio depuis resources/sounds/
            URL soundURL = getClass().getResource("/sounds/notif.mp3");
            
            if (soundURL == null) {
                System.out.println("❌ Fichier notif.mp3 introuvable dans src/main/resources/sounds/");
                return;
            }
            
            Media sound = new Media(soundURL.toString());
            mediaPlayer = new MediaPlayer(sound);
            
            // Configuration pour la boucle infinie
            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            
            // Volume (0.0 à 1.0)
            mediaPlayer.setVolume(0.6); // 60% du volume max
            
            // Démarrer la lecture
            mediaPlayer.play();
            isPlaying = true;
            
            System.out.println("🔊 Son notif.mp3 démarré en boucle (volume: 60%)");
            
        } catch (Exception e) {
            System.out.println("❌ Erreur lors du chargement du son: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Arrête la lecture du son
     */
    public void arreterSon() {
        if (mediaPlayer != null && isPlaying) {
            mediaPlayer.stop();
            mediaPlayer.dispose(); // Libérer les ressources
            isPlaying = false;
            System.out.println("🔇 Son notif.mp3 arrêté");
        }
    }
    
    /**
     * Vérifie si le son est en cours de lecture
     * @return true si le son joue
     */
    public boolean isPlaying() {
        return isPlaying;
    }
    
    /**
     * Change le volume du son
     * @param volume Volume entre 0.0 (muet) et 1.0 (max)
     */
    public void setVolume(double volume) {
        if (mediaPlayer != null) {
            // Limiter entre 0.0 et 1.0
            double vol = Math.max(0.0, Math.min(1.0, volume));
            mediaPlayer.setVolume(vol);
            System.out.println("🔊 Volume changé: " + (int)(vol * 100) + "%");
        }
    }
    
    /**
     * Redémarre le son depuis le début
     */
    public void redemarrer() {
        if (mediaPlayer != null) {
            mediaPlayer.seek(Duration.ZERO);
            if (!isPlaying) {
                mediaPlayer.play();
                isPlaying = true;
            }
        }
    }
}
