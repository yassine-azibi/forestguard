package utils;

import model.Animal;
import utils.IService;
import utils.MyConnection;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class AnimalService implements IService<Animal> {

    private Connection cnx() {
        return MyConnectionForet.getInstance().getCnx();
    }

    @Override
    public void addEntity(Animal animal) {
        if (cnx() == null) { System.err.println("addEntity Animal: pas de connexion MySQL."); return; }
        String requete = "INSERT INTO animal (id_zone, espece, nom_scientifique, "
                + "population_estimee, statut_protection, derniere_observation) "
                + "VALUES ("
                + animal.getIdZone() + ",'"
                + animal.getEspece() + "','"
                + animal.getNomScientifique() + "',"
                + animal.getPopulationEstimee() + ",'"
                + animal.getStatutProtection() + "','"
                + animal.getDerniereObservation() + "')";
        try {
            cnx().createStatement().executeUpdate(requete);
            System.out.println("Animal ajouté.");
        } catch (SQLException e) {
            System.err.println("addEntity Animal : " + e.getMessage());
        }
    }

    @Override
    public void deleteEntity(Animal animal) {
        if (cnx() == null) { System.err.println("deleteEntity Animal: pas de connexion MySQL."); return; }
        String requete = "DELETE FROM animal WHERE id=" + animal.getId();
        try {
            cnx().createStatement().executeUpdate(requete);
            System.out.println("Animal supprimé.");
        } catch (SQLException e) {
            System.err.println("deleteEntity Animal : " + e.getMessage());
        }
    }

    @Override
    public void updateEntity(int id, Animal animal) {
        if (cnx() == null) { System.err.println("updateEntity Animal: pas de connexion MySQL."); return; }
        String requete = "UPDATE animal SET "
                + "id_zone=" + animal.getIdZone() + ","
                + "espece='" + animal.getEspece() + "',"
                + "nom_scientifique='" + animal.getNomScientifique() + "',"
                + "population_estimee=" + animal.getPopulationEstimee() + ","
                + "statut_protection='" + animal.getStatutProtection() + "',"
                + "derniere_observation='" + animal.getDerniereObservation() + "' "
                + "WHERE id=" + id;
        try {
            cnx().createStatement().executeUpdate(requete);
            System.out.println("Animal modifié.");
        } catch (SQLException e) {
            System.err.println("updateEntity Animal : " + e.getMessage());
        }
    }

    @Override
    public List<Animal> getData() {
        List<Animal> animaux = new ArrayList<>();
        if (cnx() == null) { System.err.println("getData Animal: pas de connexion MySQL."); return animaux; }
        String requete = "SELECT * FROM animal";
        try {
            Statement st = cnx().createStatement();
            ResultSet rs = st.executeQuery(requete);
            while (rs.next()) {
                Animal a = new Animal(
                        rs.getInt("id_zone"),
                        rs.getString("espece"),
                        rs.getString("nom_scientifique"),
                        rs.getInt("population_estimee"),
                        rs.getString("statut_protection"),
                        rs.getString("derniere_observation")
                );
                a.setId(rs.getInt("id"));
                animaux.add(a);
            }
        } catch (SQLException e) {
            System.err.println("getData Animal : " + e.getMessage());
        }
        return animaux;
    }
}

