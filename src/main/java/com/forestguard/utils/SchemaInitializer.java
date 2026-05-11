package com.forestguard.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class SchemaInitializer {
    private static boolean initialized;

    private SchemaInitializer() {
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }

        String script = loadScript("sql/schema.sql");
        String[] statements = script.split(";");

        Connection connection = MyConnection.getConnection();
        try (Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                String normalized = sql.trim();
                if (!normalized.isEmpty()) {
                    try {
                        statement.execute(normalized);
                    } catch (SQLException e) {
                        // Ignorer les erreurs de table existante
                        if (!e.getMessage().toLowerCase().contains("already exists")) {
                            System.err.println("Erreur SQL: " + e.getMessage());
                        }
                    }
                }
            }
            initialized = true;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible d'initialiser le schema SQL", e);
        }
    }

    private static String loadScript(String resourcePath) {
        InputStream inputStream = SchemaInitializer.class.getClassLoader().getResourceAsStream(resourcePath);
        if (inputStream == null) {
            throw new IllegalStateException("Script SQL introuvable: " + resourcePath);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
            return builder.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de lire le script SQL", e);
        }
    }
}