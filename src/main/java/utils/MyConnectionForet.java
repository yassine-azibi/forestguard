package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyConnectionForet {
    private static final String URL   = "jdbc:mysql://localhost:3306/forestguard";
    private static final String LOGIN = "root";
    private static final String PWD   = "";

    private static MyConnection instance;
    private Connection cnx;

    private MyConnection() {
        connect();
    }

    /** Singleton — une seule instance partagée dans toute l'application. */
    public static MyConnection getInstance() {
        if (instance == null) {
            instance = new MyConnection();
        }
        return instance;
    }

    private void connect() {
        try {
            cnx = DriverManager.getConnection(URL, LOGIN, PWD);
            System.out.println("Connexion MySQL établie.");
        } catch (SQLException e) {
            cnx = null;
            System.err.println("Connexion MySQL échouée : " + e.getMessage());
        }
    }

    /**
     * Retourne la connexion active.
     * Si elle est nulle ou fermée, tente une reconnexion.
     * Retourne null si MySQL est inaccessible (l'appelant doit gérer ce cas).
     */
    public Connection getCnx() {
        try {
            if (cnx == null || cnx.isClosed()) {
                System.out.println("Reconnexion MySQL...");
                connect();
            }
        } catch (SQLException e) {
            connect();
        }
        return cnx;
    }
}

