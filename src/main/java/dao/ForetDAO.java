package dao;
import model.Foret;
import utils.MyConnection;
import java.sql.*;
import java.util.*;

public class ForetDAO {
    private final Connection cnx = MyConnection.getInstance().getCnx();

    public List<Foret> getAll() {
        List<Foret> list = new ArrayList<>();
        if (cnx == null) return list;
        try (PreparedStatement ps = cnx.prepareStatement("SELECT * FROM foret ORDER BY id DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(new Foret(
                rs.getInt("id"), rs.getString("nom"), rs.getString("localisation"),
                rs.getString("type_vegetation"), rs.getString("niveau_risque"),
                rs.getDouble("superficie"), rs.getDouble("latitude"), rs.getDouble("longitude")));
        } catch (SQLException e) { System.out.println("ForetDAO: " + e.getMessage()); }
        return list;
    }

    public int count() {
        if (cnx == null) return 0;
        try (PreparedStatement ps = cnx.prepareStatement("SELECT COUNT(*) FROM foret");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {}
        return 0;
    }
}
