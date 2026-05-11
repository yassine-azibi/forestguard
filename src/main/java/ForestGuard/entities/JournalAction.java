package ForestGuard.entities;

import java.time.LocalDateTime;

public class JournalAction {
    private int id;
    private LocalDateTime dateAction;
    private String utilisateur;
    private String action;
    private String details;
    private int donneeId;

    public JournalAction() {}

    public JournalAction(LocalDateTime dateAction, String utilisateur,
                         String action, String details, int donneeId) {
        this.dateAction  = dateAction;
        this.utilisateur = utilisateur;
        this.action      = action;
        this.details     = details;
        this.donneeId    = donneeId;
    }

    public int getId()                        { return id; }
    public void setId(int id)                 { this.id = id; }
    public LocalDateTime getDateAction()      { return dateAction; }
    public void setDateAction(LocalDateTime d){ this.dateAction = d; }
    public String getUtilisateur()            { return utilisateur; }
    public void setUtilisateur(String u)      { this.utilisateur = u; }
    public String getAction()                 { return action; }
    public void setAction(String a)           { this.action = a; }
    public String getDetails()                { return details; }
    public void setDetails(String d)          { this.details = d; }
    public int getDonneeId()                  { return donneeId; }
    public void setDonneeId(int d)            { this.donneeId = d; }
}