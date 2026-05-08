package model;

public class Equipement {

    private int id;
    private String nom;
    private String type;

    public Equipement() {}

    public Equipement(int id, String nom, String type) {
        this.id   = id;
        this.nom  = nom;
        this.type = type;
    }

    public int getId()          { return id; }
    public void setId(int v)    { this.id = v; }

    public String getNom()      { return nom; }
    public void setNom(String v){ this.nom = v; }

    public String getType()      { return type; }
    public void setType(String v){ this.type = v; }

    // Important pour affichage dans ComboBox/ListView
    @Override
    public String toString() {
        return nom + " (" + type + ")";
    }
}