package model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Alerte {

    private int           id;
    private String        typeAlerte;
    private String        niveau;
    private String        localisation;
    private LocalDateTime dateAlerte;
    private String        statut;
    private String        source;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public Alerte() {}

    public Alerte(String typeAlerte, String niveau, String localisation, String source) {
        this.typeAlerte   = typeAlerte;
        this.niveau       = niveau;
        this.localisation = localisation;
        this.statut       = "Nouvelle";
        this.source       = source;
        this.dateAlerte   = LocalDateTime.now();
    }

    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }
    public String getTypeAlerte()               { return typeAlerte; }
    public void setTypeAlerte(String v)         { this.typeAlerte = v; }
    public String getNiveau()                   { return niveau; }
    public void setNiveau(String v)             { this.niveau = v; }
    public String getLocalisation()             { return localisation; }
    public void setLocalisation(String v)       { this.localisation = v; }
    public LocalDateTime getDateAlerte()        { return dateAlerte; }
    public void setDateAlerte(LocalDateTime v)  { this.dateAlerte = v; }
    public String getStatut()                   { return statut; }
    public void setStatut(String v)             { this.statut = v; }
    public String getSource()                   { return source; }
    public void setSource(String v)             { this.source = v; }

    public String getDateFormatted() {
        return dateAlerte != null ? dateAlerte.format(FMT) : "-";
    }

    @Override
    public String toString() {
        return "Alerte{id=" + id + ", type='" + typeAlerte + "', statut='" + statut + "'}";
    }
}
