package edu.capteur.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyConnection {

    // ✅ Paramètres de connexion WAMP
    private final String URL      = "jdbc:mysql://localhost:3306/forestguard";
    private final String USER     = "root";
    private final String PASSWORD = "";        // WAMP = pas de mot de passe par défaut

    // ✅ Instance unique (Singleton)
    private static MyConnection instance;
    private Connection connection;

    // ✅ Constructeur privé
    private MyConnection() {
        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✅ Connexion à la base de données réussie !");
        } catch (SQLException e) {
            System.out.println("❌ Erreur de connexion : " + e.getMessage());
        }
    }

    // ✅ Méthode pour obtenir l'instance unique
    public static MyConnection getInstance() {
        if (instance == null) {
            instance = new MyConnection();
        }
        return instance;
    }

    // ✅ Méthode pour obtenir la connexion
    public Connection getConnection() {
        return connection;
    }
}