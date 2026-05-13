package com.forestguard.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Service de chat pour la communication entre utilisateurs
 */
public class ChatService {

    private static ChatService instance;
    private List<ChatMessage> messages;

    private ChatService() {
        this.messages = new ArrayList<>();
    }

    public static ChatService getInstance() {
        if (instance == null) {
            instance = new ChatService();
        }
        return instance;
    }

    /**
     * Envoie un message
     */
    public void sendMessage(String sender, String receiver, String content) throws ChatException {
        if (sender == null || sender.trim().isEmpty()) {
            throw new ChatException("L'expéditeur ne peut pas être vide");
        }
        if (receiver == null || receiver.trim().isEmpty()) {
            throw new ChatException("Le destinataire ne peut pas être vide");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new ChatException("Le message ne peut pas être vide");
        }

        ChatMessage message = new ChatMessage(sender, receiver, content);
        messages.add(message);
        System.out.println("Message envoyé de " + sender + " à " + receiver);
    }

    /**
     * Récupère tous les messages pour un utilisateur
     */
    public List<ChatMessage> getMessagesFor(String username) {
        List<ChatMessage> userMessages = new ArrayList<>();
        for (ChatMessage msg : messages) {
            if (msg.getReceiver().equals(username) || msg.getSender().equals(username)) {
                userMessages.add(msg);
            }
        }
        return userMessages;
    }

    /**
     * Classe interne pour représenter un message
     */
    public static class ChatMessage {
        private String sender;
        private String receiver;
        private String content;
        private long timestamp;

        public ChatMessage(String sender, String receiver, String content) {
            this.sender = sender;
            this.receiver = receiver;
            this.content = content;
            this.timestamp = System.currentTimeMillis();
        }

        public String getSender() {
            return sender;
        }

        public String getReceiver() {
            return receiver;
        }

        public String getContent() {
            return content;
        }

        public long getTimestamp() {
            return timestamp;
        }
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
