package com.forestguard.utils;

import com.forestguard.entities.Utilisateur;

public final class Session {
    private static Utilisateur currentUser;

    private Session() {
    }

    public static Utilisateur getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(Utilisateur currentUser) {
        Session.currentUser = currentUser;
    }

    public static void clear() {
        currentUser = null;
    }
}