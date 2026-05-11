package ForestGuard.entities;

import java.time.LocalDateTime;

public class DonCapteur {
    private int           id;
    private String        type;
    private String        capteur;
    private String        zone;
    private double        temperature;
    private double        humidite;
    private double        fumee;
    private LocalDateTime horodatage;
    private String        risque;
    private String        foret;        // ← NOUVEAU
    private String        localisation; // ← NOUVEAU

    public DonCapteur() {}

    public DonCapteur(String type, String capteur, String zone,
                      double temperature, double humidite,
                      double fumee, String risque) {
        this.type        = type;
        this.capteur     = capteur;
        this.zone        = zone;
        this.temperature = temperature;
        this.humidite    = humidite;
        this.fumee       = fumee;
        this.risque      = risque;
    }

    public DonCapteur(int id, String type, String capteur, String zone,
                      double temperature, double humidite, double fumee,
                      LocalDateTime horodatage, String risque) {
        this.id          = id;
        this.type        = type;
        this.capteur     = capteur;
        this.zone        = zone;
        this.temperature = temperature;
        this.humidite    = humidite;
        this.fumee       = fumee;
        this.horodatage  = horodatage;
        this.risque      = risque;
    }

    public int    getId()                        { return id; }
    public void   setId(int id)                  { this.id = id; }
    public String getType()                      { return type; }
    public void   setType(String type)           { this.type = type; }
    public String getCapteur()                   { return capteur; }
    public void   setCapteur(String c)           { this.capteur = c; }
    public String getZone()                      { return zone; }
    public void   setZone(String zone)           { this.zone = zone; }
    public double getTemperature()               { return temperature; }
    public void   setTemperature(double t)       { this.temperature = t; }
    public double getHumidite()                  { return humidite; }
    public void   setHumidite(double h)          { this.humidite = h; }
    public double getFumee()                     { return fumee; }
    public void   setFumee(double f)             { this.fumee = f; }
    public LocalDateTime getHorodatage()         { return horodatage; }
    public void   setHorodatage(LocalDateTime h) { this.horodatage = h; }
    public String getRisque()                    { return risque; }
    public void   setRisque(String r)            { this.risque = r; }
    public String getForet()                     { return foret; }
    public void   setForet(String f)             { this.foret = f; }
    public String getLocalisation()              { return localisation; }
    public void   setLocalisation(String l)      { this.localisation = l; }

    @Override
    public String toString() {
        return "DonCapteur{id=" + id + ", type='" + type + "', capteur='" + capteur +
                "', zone='" + zone + "', foret='" + foret + "', localisation='" + localisation +
                "', temperature=" + temperature + ", humidite=" + humidite +
                ", fumee=" + fumee + ", horodatage=" + horodatage +
                ", risque='" + risque + "'}";
    }
}