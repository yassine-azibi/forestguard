package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyConnection {

    private final String url   = "jdbc:mysql://localhost:3306/forestguard";
    private final String login = "root";
    private final String pwd   = "";

    private Connection cnx;
    private static MyConnection instance;

    private MyConnection() {
        try {
            cnx = DriverManager.getConnection(url, login, pwd);
            System.out.println("Connexion établie avec forestguard !");
        } catch (SQLException e) {
            System.out.println("Erreur connexion BDD : " + e.getMessage());
            cnx = null;
        }
    }

    public static MyConnection getInstance() {
        if (instance == null) {
            instance = new MyConnection();
        }
        return instance;
    }

    public Connection getCnx() {
        return cnx;
    }

    public boolean isConnected() {
        return cnx != null;
    }
}
