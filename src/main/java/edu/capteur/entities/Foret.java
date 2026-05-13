package edu.capteur.entities;

public class Foret {

    private int    id;
    private String nom;
    private String localisation;
    private double superficie;
    private String typeVegetation;
    private String niveauRisque;
    private String dateCreation;
    private double latitude;   // 🆕
    private double longitude;  // 🆕

    // ✅ Constructeur complet (avec id, latitude, longitude)
    public Foret(int id, String nom, String localisation, double superficie,
                 String typeVegetation, String niveauRisque, String dateCreation,
                 double latitude, double longitude) {
        this.id             = id;
        this.nom            = nom;
        this.localisation   = localisation;
        this.superficie     = superficie;
        this.typeVegetation = typeVegetation;
        this.niveauRisque   = niveauRisque;
        this.dateCreation   = dateCreation;
        this.latitude       = latitude;
        this.longitude      = longitude;
    }

    // ✅ Constructeur sans id (pour ajout)
    public Foret(String nom, String localisation, double superficie,
                 String typeVegetation, String niveauRisque, String dateCreation,
                 double latitude, double longitude) {
        this.nom            = nom;
        this.localisation   = localisation;
        this.superficie     = superficie;
        this.typeVegetation = typeVegetation;
        this.niveauRisque   = niveauRisque;
        this.dateCreation   = dateCreation;
        this.latitude       = latitude;
        this.longitude      = longitude;
    }

    // ✅ Constructeur vide
    public Foret() {}

    // ✅ Getters
    public int    getId()             { return id; }
    public String getNom()            { return nom; }
    public String getLocalisation()   { return localisation; }
    public double getSuperficie()     { return superficie; }
    public String getTypeVegetation() { return typeVegetation; }
    public String getNiveauRisque()   { return niveauRisque; }
    public String getDateCreation()   { return dateCreation; }
    public double getLatitude()       { return latitude; }
    public double getLongitude()      { return longitude; }

    // ✅ Setters
    public void setId(int id)                        { this.id = id; }
    public void setNom(String nom)                   { this.nom = nom; }
    public void setLocalisation(String localisation) { this.localisation = localisation; }
    public void setSuperficie(double superficie)     { this.superficie = superficie; }
    public void setTypeVegetation(String tv)         { this.typeVegetation = tv; }
    public void setNiveauRisque(String nr)           { this.niveauRisque = nr; }
    public void setDateCreation(String dc)           { this.dateCreation = dc; }
    public void setLatitude(double latitude)         { this.latitude = latitude; }
    public void setLongitude(double longitude)       { this.longitude = longitude; }

    @Override
    public String toString() {
        return nom;
    }
}
