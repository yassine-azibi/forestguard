package model;
public class Pompier {
    private int id;
    private String nom, prenom, email, telephone, statut, ville;
    public Pompier(int id,String nom,String prenom,String email,String telephone,String statut,String ville){
        this.id=id;this.nom=nom;this.prenom=prenom;this.email=email;
        this.telephone=telephone;this.statut=statut;this.ville=ville;
    }
    public int getId(){return id;}
    public String getNom(){return nom;}
    public String getPrenom(){return prenom;}
    public String getNomComplet(){return prenom+" "+nom;}
    public String getEmail(){return email;}
    public String getTelephone(){return telephone;}
    public String getStatut(){return statut;}
    public String getVille(){return ville;}
}
