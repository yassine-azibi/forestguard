package utils;

import model.Foret;
import utils.IService;
import utils.MyConnection;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ForetService implements IService<Foret> {

    private Connection cnx() {
        return MyConnectionForet.getInstance().getCnx();
    }

    @Override
    public void addEntity(Foret foret) {
        if (cnx() == null) { System.err.println("addEntity: pas de connexion MySQL."); return; }
        String requete = "INSERT INTO foret (nom, localisation, superficie, type_vegetation, niveau_risque, date_creation, latitude, longitude)"
                + " VALUES ('" + foret.getNom() + "','" + foret.getLocalisation() + "'," + foret.getSuperficie()
                + ",'" + foret.getTypeVegetation() + "','" + foret.getNiveauRisque() + "','" + foret.getDateCreation()
                + "'," + foret.getLatitude() + "," + foret.getLongitude() + ")";
        try {
            cnx().createStatement().executeUpdate(requete);
            System.out.println("Foret ajoutée.");
        } catch (SQLException e) {
            System.err.println("addEntity Foret : " + e.getMessage());
        }
    }

    @Override
    public void deleteEntity(Foret foret) {
        if (cnx() == null) { System.err.println("deleteEntity: pas de connexion MySQL."); return; }
        String requete = "DELETE FROM foret WHERE id=" + foret.getId();
        try {
            cnx().createStatement().executeUpdate(requete);
            System.out.println("Foret supprimée.");
        } catch (SQLException e) {
            System.err.println("deleteEntity Foret : " + e.getMessage());
        }
    }

    @Override
    public void updateEntity(int id, Foret foret) {
        if (cnx() == null) { System.err.println("updateEntity: pas de connexion MySQL."); return; }
        String requete = "UPDATE foret SET "
                + "nom='" + foret.getNom() + "',"
                + "localisation='" + foret.getLocalisation() + "',"
                + "superficie=" + foret.getSuperficie() + ","
                + "type_vegetation='" + foret.getTypeVegetation() + "',"
                + "niveau_risque='" + foret.getNiveauRisque() + "',"
                + "date_creation='" + foret.getDateCreation() + "' "
                + "WHERE id=" + id;
        try {
            cnx().createStatement().executeUpdate(requete);
            System.out.println("Foret modifiée.");
        } catch (SQLException e) {
            System.err.println("updateEntity Foret : " + e.getMessage());
        }
    }

    @Override
    public List<Foret> getData() {
        List<Foret> forets = new ArrayList<>();
        if (cnx() == null) { System.err.println("getData Foret: pas de connexion MySQL."); return forets; }
        String requete = "SELECT * FROM foret";
        try {
            Statement st = cnx().createStatement();
            ResultSet rs = st.executeQuery(requete);
            while (rs.next()) {
                Foret f = new Foret(
                        rs.getString("nom"),
                        rs.getString("localisation"),
                        rs.getDouble("superficie"),
                        rs.getString("type_vegetation"),
                        rs.getString("niveau_risque"),
                        rs.getString("date_creation")
                );
                f.setId(rs.getInt("id"));
                f.setLatitude(rs.getDouble("latitude"));
                f.setLongitude(rs.getDouble("longitude"));
                forets.add(f);
            }
        } catch (SQLException e) {
            System.err.println("getData Foret : " + e.getMessage());
        }
        return forets;
    }
}

