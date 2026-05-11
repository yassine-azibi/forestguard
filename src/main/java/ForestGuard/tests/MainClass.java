package ForestGuard.tests;

import ForestGuard.entities.DonCapteur;
import ForestGuard.services.DonCapteurService;
import ForestGuard.utils.MyConnection;

import java.sql.*;

public class MainClass {
    public static void main(String[] args) throws Exception {

        System.out.println("=== Test Connexion BD ===");
        Connection cnx = MyConnection.getInstance().getCnx();
        System.out.println("Connexion OK : " + cnx);

        System.out.println("\n=== Structure de la table donnee_capteur ===");
        ResultSet cols = cnx.getMetaData().getColumns(null, null, "donnee_capteur", null);
        while (cols.next()) {
            System.out.println("  Colonne : " + cols.getString("COLUMN_NAME")
                    + "  Type : " + cols.getString("TYPE_NAME"));
        }

        System.out.println("\n=== Nombre de lignes ===");
        ResultSet rs = cnx.createStatement().executeQuery("SELECT COUNT(*) FROM donnee_capteur");
        if (rs.next()) System.out.println("  Total : " + rs.getInt(1));

        System.out.println("\n=== 3 premieres lignes ===");
        ResultSet rs2 = cnx.createStatement().executeQuery(
                "SELECT * FROM donnee_capteur LIMIT 3");
        ResultSetMetaData meta = rs2.getMetaData();
        while (rs2.next()) {
            StringBuilder sb = new StringBuilder("  ");
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                sb.append(meta.getColumnName(i)).append("=")
                  .append(rs2.getString(i)).append(" | ");
            }
            System.out.println(sb);
        }

        System.out.println("\n=== Test getData() ===");
        DonCapteurService service = new DonCapteurService();
        java.util.List<DonCapteur> liste = service.getDataSimple();
        System.out.println("getDataSimple retourne : " + liste.size() + " elements");
        if (!liste.isEmpty()) {
            DonCapteur premier = liste.get(0);
            System.out.println("Premier : capteur=" + premier.getCapteur()
                    + " zone=" + premier.getZone()
                    + " foret=" + premier.getForet()
                    + " temp=" + premier.getTemperature());
        }
    }
}
