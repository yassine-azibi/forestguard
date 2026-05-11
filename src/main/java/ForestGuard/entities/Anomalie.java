package ForestGuard.entities;

import java.time.LocalDateTime;

public class Anomalie {
    private int id;
    private LocalDateTime dateDetection;
    private String capteur;
    private String foret;
    private String typeAnomalie;
    private double valeurDetectee;
    private double seuilDepasse;
    private boolean traite;
    private int donneeId;

    public Anomalie() {}

    public Anomalie(LocalDateTime dateDetection, String capteur, String foret,
                    String typeAnomalie, double valeurDetectee,
                    double seuilDepasse, boolean traite, int donneeId) {
        this.dateDetection  = dateDetection;
        this.capteur        = capteur;
        this.foret          = foret;
        this.typeAnomalie   = typeAnomalie;
        this.valeurDetectee = valeurDetectee;
        this.seuilDepasse   = seuilDepasse;
        this.traite         = traite;
        this.donneeId       = donneeId;
    }

    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }
    public LocalDateTime getDateDetection()     { return dateDetection; }
    public void setDateDetection(LocalDateTime d){ this.dateDetection = d; }
    public String getCapteur()                  { return capteur; }
    public void setCapteur(String c)            { this.capteur = c; }
    public String getForet()                    { return foret; }
    public void setForet(String f)              { this.foret = f; }
    public String getTypeAnomalie()             { return typeAnomalie; }
    public void setTypeAnomalie(String t)       { this.typeAnomalie = t; }
    public double getValeurDetectee()           { return valeurDetectee; }
    public void setValeurDetectee(double v)     { this.valeurDetectee = v; }
    public double getSeuilDepasse()             { return seuilDepasse; }
    public void setSeuilDepasse(double s)       { this.seuilDepasse = s; }
    public boolean isTraite()                   { return traite; }
    public void setTraite(boolean t)            { this.traite = t; }
    public int getDonneeId()                    { return donneeId; }
    public void setDonneeId(int d)              { this.donneeId = d; }
}