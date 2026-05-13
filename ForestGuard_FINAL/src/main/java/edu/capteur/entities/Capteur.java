package edu.capteur.entities;

public class Capteur {

    private int    id;
    private String nom;
    private String type;        // temperature, fumee, humidite
    private String localisation;
    private String statut;      // actif, inactif, en_panne
    private int    foretId;     // 🔗 liaison avec la table foret

    // ✅ Constructeur complet (avec id et foretId)
    public Capteur(int id, String nom, String type, String localisation, String statut, int foretId) {
        this.id           = id;
        this.nom          = nom;
        this.type         = type;
        this.localisation = localisation;
        this.statut       = statut;
        this.foretId      = foretId;
    }

    // ✅ Constructeur sans id (pour ajout)
    public Capteur(String nom, String type, String localisation, String statut, int foretId) {
        this.nom          = nom;
        this.type         = type;
        this.localisation = localisation;
        this.statut       = statut;
        this.foretId      = foretId;
    }

    // ✅ Constructeur vide
    public Capteur() {}

    // ✅ Getters
    public int    getId()           { return id; }
    public String getNom()          { return nom; }
    public String getType()         { return type; }
    public String getLocalisation() { return localisation; }
    public String getStatut()       { return statut; }
    public int    getForetId()      { return foretId; }

    // ✅ Setters
    public void setId(int id)                       { this.id = id; }
    public void setNom(String nom)                  { this.nom = nom; }
    public void setType(String type)                { this.type = type; }
    public void setLocalisation(String localisation){ this.localisation = localisation; }
    public void setStatut(String statut)            { this.statut = statut; }
    public void setForetId(int foretId)             { this.foretId = foretId; }

    @Override
    public String toString() {
        return nom + " · " + type + " (" + localisation + ")";
    }
}
