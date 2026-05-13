package model;
public class Utilisateur {
    private int id;
    private String nom, email, telephone, localisation;
    public Utilisateur(int id, String nom, String email, String telephone, String localisation) {
        this.id=id; this.nom=nom; this.email=email; this.telephone=telephone; this.localisation=localisation;
    }
    public int getId(){return id;}
    public String getNom(){return nom;}
    public String getEmail(){return email;}
    public String getTelephone(){return telephone;}
    public String getLocalisation(){return localisation;}
}
