package dao;
import model.Utilisateur;
import utils.MyConnection;
import java.sql.*;
import java.util.*;

public class UtilisateurDAO {
    private final Connection cnx = MyConnection.getInstance().getCnx();

    public List<Utilisateur> getAll() {
        List<Utilisateur> list = new ArrayList<>();
        if (cnx == null) return list;
        try (PreparedStatement ps = cnx.prepareStatement("SELECT * FROM utilisateur ORDER BY id DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(new Utilisateur(
                rs.getInt("id"), rs.getString("nom"), rs.getString("email"),
                rs.getString("telephone"), rs.getString("localisation")));
        } catch (SQLException e) { System.out.println("UtilisateurDAO: " + e.getMessage()); }
        return list;
    }

    public int count() {
        if (cnx == null) return 0;
        try (PreparedStatement ps = cnx.prepareStatement("SELECT COUNT(*) FROM utilisateur");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {}
        return 0;
    }
}
