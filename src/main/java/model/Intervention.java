package model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Intervention {

    private String alertZone;
    private String statut;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String agentName;
    private String resultat;
    private int alerteId;
    private String alerteLocalisation;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public Intervention() {}

    public Intervention(String alertZone, String statut,
                        LocalDateTime startDate, LocalDateTime endDate,
                        String agentName, String resultat,
                        int alerteId, String alerteLocalisation) {
        this.alertZone          = alertZone;
        this.statut             = statut;
        this.startDate          = startDate;
        this.endDate            = endDate;
        this.agentName          = agentName;
        this.resultat           = resultat;
        this.alerteId           = alerteId;
        this.alerteLocalisation = alerteLocalisation;
    }

    // ── Getters / Setters ──────────────────────────────────────────────────────

    public String getAlertZone()              { return alertZone; }
    public void setAlertZone(String v)        { this.alertZone = v; }

    public String getStatut()                 { return statut; }
    public void setStatut(String v)           { this.statut = v; }

    public LocalDateTime getStartDate()       { return startDate; }
    public void setStartDate(LocalDateTime v) { this.startDate = v; }

    public LocalDateTime getEndDate()         { return endDate; }
    public void setEndDate(LocalDateTime v)   { this.endDate = v; }

    public String getAgentName()              { return agentName; }
    public void setAgentName(String v)        { this.agentName = v; }

    public String getResultat()               { return resultat; }
    public void setResultat(String v)         { this.resultat = v; }

    public int getAlerteId()                  { return alerteId; }
    public void setAlerteId(int v)            { this.alerteId = v; }

    public String getAlerteLocalisation()     { return alerteLocalisation; }
    public void setAlerteLocalisation(String v){ this.alerteLocalisation = v; }

    // ── Helpers affichage TableView ────────────────────────────────────────────

    public String getStartDateFormatted() {
        return startDate != null ? startDate.format(FORMATTER) : "-";
    }

    public String getEndDateFormatted() {
        return endDate != null ? endDate.format(FORMATTER) : "-";
    }
}