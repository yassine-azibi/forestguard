package com.forestguard.utils;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordHasher {
    private PasswordHasher() {
    }

    public static String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }

    public static boolean verifyPassword(String password, String hash) {
        return BCrypt.checkpw(password, hash);
    }
}