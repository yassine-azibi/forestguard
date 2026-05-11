package com.forestguard.utils;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class MyConnection {
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/forestguard?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";

    private static final String URL;
    private static final String USER;
    private static final String PASSWORD;
    private static Connection connection;

    static {
        Properties properties = new Properties();

        try (InputStream inputStream = MyConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de charger db.properties", e);
        }

        URL = properties.getProperty("db.url", DEFAULT_URL);
        USER = properties.getProperty("db.user", DEFAULT_USER);
        PASSWORD = properties.getProperty("db.password", DEFAULT_PASSWORD);
    }

    private MyConnection() {
    }

    public static synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(2)) {
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible d'etablir la connexion MySQL", e);
        }
        return connection;
    }
    }
