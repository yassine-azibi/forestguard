package utils;

import model.Incendie;
import utils.IService;
import utils.MyConnection;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class IncendieService implements IService<Incendie> {

    private Connection cnx() {
        return MyConnectionForet.getInstance().getCnx();
    }

    @Override
    public void addEntity(Incendie incendie) {
        if (cnx() == null) { System.err.println("addEntity Incendie: pas de connexion MySQL."); return; }
        String requete = "INSERT INTO incendie (id_zone, date_debut, date_fin, superficie_brulee, cause, niveau_gravite, statut)"
                + " VALUES ("
                + incendie.getIdZone() + ",'"
                + incendie.getDateDebut() + "','"
                + incendie.getDateFin() + "',"
                + incendie.getSuperficieBrulee() + ",'"
                + incendie.getCause() + "','"
                + incendie.getNiveauGravite() + "','"
                + incendie.getStatut() + "')";
        try {
            cnx().createStatement().executeUpdate(requete);
            System.out.println("Incendie ajouté.");
        } catch (SQLException e) {
            System.err.println("addEntity Incendie : " + e.getMessage());
        }
    }

    @Override
    public void deleteEntity(Incendie incendie) {
        if (cnx() == null) { System.err.println("deleteEntity Incendie: pas de connexion MySQL."); return; }
        String requete = "DELETE FROM incendie WHERE id=" + incendie.getId();
        try {
            cnx().createStatement().executeUpdate(requete);
            System.out.println("Incendie supprimé.");
        } catch (SQLException e) {
            System.err.println("deleteEntity Incendie : " + e.getMessage());
        }
    }

    @Override
    public void updateEntity(int id, Incendie incendie) {
        if (cnx() == null) { System.err.println("updateEntity Incendie: pas de connexion MySQL."); return; }
        String requete = "UPDATE incendie SET "
                + "id_zone=" + incendie.getIdZone() + ","
                + "date_debut='" + incendie.getDateDebut() + "',"
                + "date_fin='" + incendie.getDateFin() + "',"
                + "superficie_brulee=" + incendie.getSuperficieBrulee() + ","
                + "cause='" + incendie.getCause() + "',"
                + "niveau_gravite='" + incendie.getNiveauGravite() + "',"
                + "statut='" + incendie.getStatut() + "' "
                + "WHERE id=" + id;
        try {
            cnx().createStatement().executeUpdate(requete);
            System.out.println("Incendie modifié.");
        } catch (SQLException e) {
            System.err.println("updateEntity Incendie : " + e.getMessage());
        }
    }

    @Override
    public List<Incendie> getData() {
        List<Incendie> incendies = new ArrayList<>();
        if (cnx() == null) { System.err.println("getData Incendie: pas de connexion MySQL."); return incendies; }
        String requete = "SELECT * FROM incendie";
        try {
            Statement st = cnx().createStatement();
            ResultSet rs = st.executeQuery(requete);
            while (rs.next()) {
                Incendie i = new Incendie(
                        rs.getInt("id_zone"),
                        rs.getString("date_debut"),
                        rs.getString("date_fin"),
                        rs.getDouble("superficie_brulee"),
                        rs.getString("cause"),
                        rs.getString("niveau_gravite"),
                        rs.getString("statut")
                );
                i.setId(rs.getInt("id"));
                incendies.add(i);
            }
        } catch (SQLException e) {
            System.err.println("getData Incendie : " + e.getMessage());
        }
        return incendies;
    }
}

