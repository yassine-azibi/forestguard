package ForestGuard.entities;

import java.time.LocalDateTime;

public class RapportInspection {
    private int id;
    private LocalDateTime dateRapport;
    private String foret;
    private String responsable;
    private String statut;
    private String niveauRisque;
    private String observations;
    private int donneeId;

    public RapportInspection() {}

    public RapportInspection(LocalDateTime dateRapport, String foret,
                             String responsable, String statut,
                             String niveauRisque, String observations,
                             int donneeId) {
        this.dateRapport  = dateRapport;
        this.foret        = foret;
        this.responsable  = responsable;
        this.statut       = statut;
        this.niveauRisque = niveauRisque;
        this.observations = observations;
        this.donneeId     = donneeId;
    }

    public int getId()                         { return id; }
    public void setId(int id)                  { this.id = id; }
    public LocalDateTime getDateRapport()      { return dateRapport; }
    public void setDateRapport(LocalDateTime d){ this.dateRapport = d; }
    public String getForet()                   { return foret; }
    public void setForet(String f)             { this.foret = f; }
    public String getResponsable()             { return responsable; }
    public void setResponsable(String r)       { this.responsable = r; }
    public String getStatut()                  { return statut; }
    public void setStatut(String s)            { this.statut = s; }
    public String getNiveauRisque()            { return niveauRisque; }
    public void setNiveauRisque(String n)      { this.niveauRisque = n; }
    public String getObservations()            { return observations; }
    public void setObservations(String o)      { this.observations = o; }
    public int getDonneeId()                   { return donneeId; }
    public void setDonneeId(int d)             { this.donneeId = d; }
}