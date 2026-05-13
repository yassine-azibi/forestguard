package model;
public class Foret {
    private int id;
    private String nom, localisation, typeVegetation, niveauRisque;
    private double superficie, latitude, longitude;
    public Foret(int id,String nom,String localisation,String typeVegetation,String niveauRisque,double superficie,double latitude,double longitude){
        this.id=id;this.nom=nom;this.localisation=localisation;
        this.typeVegetation=typeVegetation;this.niveauRisque=niveauRisque;
        this.superficie=superficie;this.latitude=latitude;this.longitude=longitude;
    }
    public int getId(){return id;}
    public String getNom(){return nom;}
    public String getLocalisation(){return localisation;}
    public String getTypeVegetation(){return typeVegetation;}
    public String getNiveauRisque(){return niveauRisque;}
    public double getSuperficie(){return superficie;}
    public double getLatitude(){return latitude;}
    public double getLongitude(){return longitude;}
    public String getCoordonnees(){return String.format("%.4f, %.4f",latitude,longitude);}
}
