package model;

public class Foret {
    private int id;
    private String nom;
    private String localisation;
    private double superficie;
    private String typeVegetation;
    private String niveauRisque;
    private String datecreation;
    private double latitude;
    private double longitude;
    
    // Constructeur complet (pour gestion-donnees)
    public Foret(int id, String nom, String localisation, String typeVegetation, String niveauRisque, double superficie, double latitude, double longitude) {
        this.id = id;
        this.nom = nom;
        this.localisation = localisation;
        this.typeVegetation = typeVegetation;
        this.niveauRisque = niveauRisque;
        this.superficie = superficie;
        this.latitude = latitude;
        this.longitude = longitude;
    }
    
    // Constructeur sans id (pour gestionforet)
    public Foret(String nom, String localisation, double superficie, String typeVegetation, String niveauRisque, String datecreation) {
        this.nom = nom;
        this.localisation = localisation;
        this.superficie = superficie;
        this.typeVegetation = typeVegetation;
        this.niveauRisque = niveauRisque;
        this.datecreation = datecreation;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public String getLocalisation() {
        return localisation;
    }

    public double getSuperficie() {
        return superficie;
    }

    public String getTypeVegetation() {
        return typeVegetation;
    }

    public String getNiveauRisque() {
        return niveauRisque;
    }

    public String getDateCreation() {
        return datecreation;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getCoordonnees() {
        return String.format("%.4f, %.4f", latitude, longitude);
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public void setLocalisation(String localisation) {
        this.localisation = localisation;
    }

    public void setSuperficie(double superficie) {
        this.superficie = superficie;
    }

    public void setTypeVegetation(String typeVegetation) {
        this.typeVegetation = typeVegetation;
    }

    public void setNiveauRisque(String niveauRisque) {
        this.niveauRisque = niveauRisque;
    }

    public void setDateCreation(String dateCreation) {
        this.datecreation = dateCreation;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    @Override
    public String toString() {
        return "Foret{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", localisation='" + localisation + '\'' +
                ", superficie=" + superficie +
                ", typeVegetation='" + typeVegetation + '\'' +
                ", niveauRisque='" + niveauRisque + '\'' +
                ", datecreation='" + datecreation + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }
}
