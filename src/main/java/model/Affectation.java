package model;

import java.time.LocalDateTime;

/**
 * Modèle représentant une affectation de pompier à une alerte
 */
public class Affectation {
    
    private int id;
    private int idAlerte;
    private int idPompier;
    private LocalDateTime dateAffectation;
    private String statut; // 'en_cours' ou 'annule'
    private Double scoreSelection;
    private Double distanceKm;
    private String modeAffectation; // 'automatique' ou 'annule'
    
    // Informations de l'alerte (jointure)
    private String typeAlerte;
    private String niveauAlerte;
    private String localisationAlerte;
    
    // Constructeurs
    public Affectation() {}
    
    public Affectation(int id, int idAlerte, int idPompier, LocalDateTime dateAffectation,
                      String statut, Double scoreSelection, Double distanceKm, String modeAffectation) {
        this.id = id;
        this.idAlerte = idAlerte;
        this.idPompier = idPompier;
        this.dateAffectation = dateAffectation;
        this.statut = statut;
        this.scoreSelection = scoreSelection;
        this.distanceKm = distanceKm;
        this.modeAffectation = modeAffectation;
    }
    
    // Getters et Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getIdAlerte() {
        return idAlerte;
    }
    
    public void setIdAlerte(int idAlerte) {
        this.idAlerte = idAlerte;
    }
    
    public int getIdPompier() {
        return idPompier;
    }
    
    public void setIdPompier(int idPompier) {
        this.idPompier = idPompier;
    }
    
    public LocalDateTime getDateAffectation() {
        return dateAffectation;
    }
    
    public void setDateAffectation(LocalDateTime dateAffectation) {
        this.dateAffectation = dateAffectation;
    }
    
    public String getStatut() {
        return statut;
    }
    
    public void setStatut(String statut) {
        this.statut = statut;
    }
    
    public Double getScoreSelection() {
        return scoreSelection;
    }
    
    public void setScoreSelection(Double scoreSelection) {
        this.scoreSelection = scoreSelection;
    }
    
    public Double getDistanceKm() {
        return distanceKm;
    }
    
    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }
    
    public String getModeAffectation() {
        return modeAffectation;
    }
    
    public void setModeAffectation(String modeAffectation) {
        this.modeAffectation = modeAffectation;
    }
    
    public String getTypeAlerte() {
        return typeAlerte;
    }
    
    public void setTypeAlerte(String typeAlerte) {
        this.typeAlerte = typeAlerte;
    }
    
    public String getNiveauAlerte() {
        return niveauAlerte;
    }
    
    public void setNiveauAlerte(String niveauAlerte) {
        this.niveauAlerte = niveauAlerte;
    }
    
    public String getLocalisationAlerte() {
        return localisationAlerte;
    }
    
    public void setLocalisationAlerte(String localisationAlerte) {
        this.localisationAlerte = localisationAlerte;
    }
    
    @Override
    public String toString() {
        return "Affectation{" +
                "id=" + id +
                ", idAlerte=" + idAlerte +
                ", idPompier=" + idPompier +
                ", statut='" + statut + '\'' +
                ", distanceKm=" + distanceKm +
                '}';
    }
}
