package edu.pompier.entities;

public class Pompier {
    private int id;
    private String nom, prenom, email, telephone, mot_de_passe, statut;
    private int zoneId;
    private String nomForet;
    private String niveauCertification;
    private String creneauGarde;
    private String dateGarde;
    private String ville;
    private String zoneAdresse;
    private double latitude;
    private double longitude;

    // ── Champs ajoutés pour l'affectation automatique ──
    private String localisationForet;  // localisation textuelle de la forêt assignée
    private int nbGardes;              // nombre de gardes planifiées (charge)
    private int nbMissionsActives;     // nombre de missions en_cours

    // ── Photo du pompier ──
    private String photoPath;          // chemin vers la photo (URL ou path local)

    public Pompier() {
    }

    public Pompier(String nom, String prenom, String email, String telephone,
                   String mot_de_passe, String statut, int zoneId) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.telephone = telephone;
        this.mot_de_passe = mot_de_passe;
        this.statut = statut;
        this.zoneId = zoneId;
    }

    public String getVille() {
        return ville;
    }

    public void setVille(String ville) {
        this.ville = ville;
    }

    public String getZoneAdresse() {
        return zoneAdresse;
    }

    public void setZoneAdresse(String z) {
        this.zoneAdresse = z;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double lat) {
        this.latitude = lat;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double lon) {
        this.longitude = lon;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getMot_de_passe() {
        return mot_de_passe;
    }

    public void setMot_de_passe(String mot_de_passe) {
        this.mot_de_passe = mot_de_passe;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public int getZoneId() {
        return zoneId;
    }

    public void setZoneId(int zoneId) {
        this.zoneId = zoneId;
    }

    public String getNomForet() {
        return nomForet;
    }

    public void setNomForet(String nomForet) {
        this.nomForet = nomForet;
    }

    public String getNiveauCertification() {
        return niveauCertification;
    }

    public void setNiveauCertification(String niveauCertification) {
        this.niveauCertification = niveauCertification;
    }

    public String getCreneauGarde() {
        return creneauGarde;
    }

    public void setCreneauGarde(String creneauGarde) {
        this.creneauGarde = creneauGarde;
    }

    public String getDateGarde() {
        return dateGarde;
    }

    public void setDateGarde(String dateGarde) {
        this.dateGarde = dateGarde;
    }

    // ── Getters/Setters pour l'affectation automatique ──
    public String getLocalisationForet() {
        return localisationForet;
    }

    public void setLocalisationForet(String localisationForet) {
        this.localisationForet = localisationForet;
    }

    public int getNbGardes() {
        return nbGardes;
    }

    public void setNbGardes(int nbGardes) {
        this.nbGardes = nbGardes;
    }

    public int getNbMissionsActives() {
        return nbMissionsActives;
    }

    public void setNbMissionsActives(int nbMissionsActives) {
        this.nbMissionsActives = nbMissionsActives;
    }

    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }

    @Override
    public String toString() {
        return nom + " " + prenom + " (" + email + ")";
    }
}
