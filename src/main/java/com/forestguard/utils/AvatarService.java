package com.forestguard.utils;

import javafx.scene.image.Image;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Service pour charger les avatars depuis l'API DiceBear.
 * Utilise des avatars générés automatiquement basés sur les initiales de l'utilisateur.
 */
public class AvatarService {

    private static final String DICEBEAR_API_URL = "https://api.dicebear.com/7.x/initials/png";

    /**
     * Charge un avatar depuis DiceBear en fonction du nom d'utilisateur.
     * L'avatar est chargé en arrière-plan pour ne pas bloquer le thread JavaFX.
     *
     * @param userName le nom complet de l'utilisateur (ex: "Ahmed Hassan")
     * @return une Image JavaFX chargée avec backgroundLoading=true
     */
    public static Image loadAvatar(String userName) {
        try {
            // Encoder le nom pour utilisation dans l'URL
            String encodedName = URLEncoder.encode(userName, StandardCharsets.UTF_8.toString());

            // Construire l'URL DiceBear
            String avatarUrl = DICEBEAR_API_URL + "?seed=" + encodedName + "&scale=80";

            // Charger l'image avec backgroundLoading=true pour éviter de bloquer le thread JavaFX
            return new Image(avatarUrl, true);

        } catch (UnsupportedEncodingException e) {
            System.err.println("Erreur lors de l'encodage du nom pour l'avatar: " + e.getMessage());
            // Retourner une image vide en cas d'erreur
            return new Image("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='80' height='80'%3E%3Crect fill='%23ccc' width='80' height='80'/%3E%3C/svg%3E", true);
        }
    }
}
