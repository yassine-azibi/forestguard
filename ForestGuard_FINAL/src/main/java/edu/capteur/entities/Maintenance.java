package edu.capteur.entities;

public class Maintenance {

    private int id;
    private String dateMaintenance;
    private String typeMaintenance;  // preventive, corrective, remplacement
    private String statut;           // planifiee, en_cours, terminee
    private String description;
    private int capteurId;           // 🔗 clé étrangère vers Capteur

    // ✅ Constructeur complet (avec id)
    public Maintenance(int id, String dateMaintenance, String typeMaintenance,
                       String statut, String description, int capteurId) {
        this.id = id;
        this.dateMaintenance = dateMaintenance;
        this.typeMaintenance = typeMaintenance;
        this.statut = statut;
        this.description = description;
        this.capteurId = capteurId;
    }

    // ✅ Constructeur sans id (pour ajout)
    public Maintenance(String dateMaintenance, String typeMaintenance,
                       String statut, String description, int capteurId) {
        this.dateMaintenance = dateMaintenance;
        this.typeMaintenance = typeMaintenance;
        this.statut = statut;
        this.description = description;
        this.capteurId = capteurId;
    }

    // ✅ Constructeur vide
    public Maintenance() {}

    // ✅ Getters
    public int getId() { return id; }
    public String getDateMaintenance() { return dateMaintenance; }
    public String getTypeMaintenance() { return typeMaintenance; }
    public String getStatut() { return statut; }
    public String getDescription() { return description; }
    public int getCapteurId() { return capteurId; }

    // ✅ Setters
    public void setId(int id) { this.id = id; }
    public void setDateMaintenance(String dateMaintenance) { this.dateMaintenance = dateMaintenance; }
    public void setTypeMaintenance(String typeMaintenance) { this.typeMaintenance = typeMaintenance; }
    public void setStatut(String statut) { this.statut = statut; }
    public void setDescription(String description) { this.description = description; }
    public void setCapteurId(int capteurId) { this.capteurId = capteurId; }

    @Override
    public String toString() {
        return "Maintenance{" +
                "id=" + id +
                ", dateMaintenance='" + dateMaintenance + '\'' +
                ", typeMaintenance='" + typeMaintenance + '\'' +
                ", statut='" + statut + '\'' +
                ", description='" + description + '\'' +
                ", capteurId=" + capteurId +
                '}';
    }
}