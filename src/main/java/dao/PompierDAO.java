package dao;
import model.Pompier;
import utils.MyConnection;
import java.sql.*;
import java.util.*;

public class PompierDAO {
    private final Connection cnx = MyConnection.getInstance().getCnx();

    public List<Pompier> getAll() {
        List<Pompier> list = new ArrayList<>();
        if (cnx == null) return list;
        try (PreparedStatement ps = cnx.prepareStatement("SELECT * FROM pompier ORDER BY id DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(new Pompier(
                rs.getInt("id"), rs.getString("nom"), rs.getString("prenom"),
                rs.getString("email"), rs.getString("telephone"),
                rs.getString("statut"), rs.getString("ville")));
        } catch (SQLException e) { System.out.println("PompierDAO: " + e.getMessage()); }
        return list;
    }

    public int countDisponibles() {
        if (cnx == null) return 0;
        try (PreparedStatement ps = cnx.prepareStatement("SELECT COUNT(*) FROM pompier WHERE statut='disponible'");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {}
        return 0;
    }

    public int count() {
        if (cnx == null) return 0;
        try (PreparedStatement ps = cnx.prepareStatement("SELECT COUNT(*) FROM pompier");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {}
        return 0;
    }
}
