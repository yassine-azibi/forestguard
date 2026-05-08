package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyConnection {

    private String url   = "jdbc:mysql://localhost:3306/forestguard";
    private String login = "root";
    private String pwd   = "";

    private Connection cnx;
    private static MyConnection instance;

    public Connection getCnx() {
        return cnx;
    }

    private MyConnection() {
        try {
            cnx = DriverManager.getConnection(url, login, pwd);
            System.out.println("Connexion etablie!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static MyConnection getInstance() {
        if (instance == null) {
            instance = new MyConnection();
        }
        return instance;
    }
}