package ForestGuard.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyConnection {
    // ─── Les 3 valeurs de connexion ─────────────────────
    private String url   = "jdbc:mysql://localhost:3306/forestguard";
    private String login = "root";
    private String pwd   = "";   // ← laisser vide si pas de mot de passe

    private Connection cnx;
    private static MyConnection instance;

    // ─── Constructeur privé ──────────────────────────────
    private MyConnection() {
        try {
            cnx = DriverManager.getConnection(url, login, pwd);
            System.out.println("Connexion BD réussie !");
        } catch (SQLException e) {
            System.out.println("Erreur connexion : " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    // ─── Singleton ───────────────────────────────────────
    public static MyConnection getInstance() {
        if (instance == null) {
            instance = new MyConnection();
        }
        return instance;
    }

    // ─── Reset connexion (reconnexion) ───────────────────
    public static void reset() {
        if (instance != null) {
            try {
                if (instance.cnx != null && !instance.cnx.isClosed())
                    instance.cnx.close();
            } catch (Exception ignored) {}
            instance = null;
        }
    }

    // ─── Getter connexion ────────────────────────────────
    public Connection getCnx() {
        return cnx;
    }
}
