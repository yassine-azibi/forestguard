package com.forestguard.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Service de chat bot pour l'assistance ForestGuard
 */
public class ChatService {

    private List<String> conversationHistory;
    private String userLocation;
    private String alertLevel;

    public ChatService() {
        this.conversationHistory = new ArrayList<>();
        this.userLocation = "";
        this.alertLevel = "Faible";
    }

    /**
     * Envoie un message au bot et retourne la réponse
     */
    public String sendMessage(String message) throws ChatException {
        if (message == null || message.trim().isEmpty()) {
            throw new ChatException("Le message ne peut pas être vide");
        }

        conversationHistory.add("User: " + message);
        
        // Réponse simple basée sur des mots-clés
        String response = generateResponse(message.toLowerCase());
        conversationHistory.add("Bot: " + response);
        
        return response;
    }

    /**
     * Efface l'historique de conversation
     */
    public void clearHistory() {
        conversationHistory.clear();
    }

    /**
     * Définit le contexte utilisateur (localisation et niveau d'alerte)
     */
    public void setUserContext(String location, String alertLevel) {
        this.userLocation = location != null ? location : "";
        this.alertLevel = alertLevel != null ? alertLevel : "Faible";
    }

    /**
     * Génère une réponse basée sur le message de l'utilisateur
     */
    private String generateResponse(String message) {
        // Réponses basées sur des mots-clés
        if (message.contains("feu") || message.contains("incendie")) {
            return "🔥 En cas d'incendie:\n" +
                   "1. Appelez immédiatement les pompiers (193)\n" +
                   "2. Évacuez la zone en toute sécurité\n" +
                   "3. Ne tentez pas d'éteindre un grand feu\n" +
                   "4. Éloignez-vous dans la direction opposée au vent";
        }
        
        if (message.contains("évacuation") || message.contains("evacuer")) {
            return "🚨 Consignes d'évacuation:\n" +
                   "1. Restez calme et suivez les instructions\n" +
                   "2. Prenez vos documents importants\n" +
                   "3. Fermez portes et fenêtres\n" +
                   "4. Dirigez-vous vers le point de rassemblement";
        }
        
        if (message.contains("prévention") || message.contains("prevention")) {
            return "🌲 Prévention des incendies:\n" +
                   "1. Ne jetez jamais de mégots dans la nature\n" +
                   "2. Évitez les barbecues en période sèche\n" +
                   "3. Débroussaillez autour de votre propriété\n" +
                   "4. Signalez tout départ de feu immédiatement";
        }
        
        if (message.contains("alerte") || message.contains("signaler")) {
            return "📢 Pour signaler une alerte:\n" +
                   "1. Utilisez le module 'Gestion des Alertes'\n" +
                   "2. Indiquez la localisation précise\n" +
                   "3. Décrivez la situation\n" +
                   "4. Les pompiers seront notifiés automatiquement";
        }
        
        if (message.contains("aide") || message.contains("help")) {
            return "💡 Je peux vous aider avec:\n" +
                   "• Consignes de sécurité incendie\n" +
                   "• Procédures d'évacuation\n" +
                   "• Prévention des incendies\n" +
                   "• Signalement d'alertes\n" +
                   "Posez-moi une question!";
        }
        
        // Réponse par défaut
        return "Je suis là pour vous aider avec la sécurité incendie. " +
               "Vous pouvez me poser des questions sur les incendies, l'évacuation, " +
               "la prévention ou le signalement d'alertes.";
    }

    /**
     * Exception personnalisée pour le chat
     */
    public static class ChatException extends Exception {
        public ChatException(String message) {
            super(message);
        }
    }
}
