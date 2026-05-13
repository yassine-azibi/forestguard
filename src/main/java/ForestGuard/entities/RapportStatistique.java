package ForestGuard.entities;

import java.time.LocalDateTime;

/**
 * Rapport statistique d'incendie par forêt et par période (mois/année).
 */
public class RapportStatistique {

    private int           id;
    private String        foret;
    private String        localisation;
    private int           mois;           // 1 = Janvier … 12 = Décembre
    private int           annee;
    private double        pourcentageIncendie; // 0.0 – 100.0
    private int           nbDanger;
    private int           nbAttention;
    private int           nbSur;
    private double        tempMoyenne;
    private double        humMoyenne;
    private double        fumeeMoyenne;
    private LocalDateTime dateCalcul;

    // ── Constructeur vide ─────────────────────────────────
    public RapportStatistique() {}

    // ── Constructeur complet (sans id) ───────────────────
    public RapportStatistique(String foret, String localisation,
                              int mois, int annee,
                              double pourcentageIncendie,
                              int nbDanger, int nbAttention, int nbSur,
                              double tempMoyenne, double humMoyenne,
                              double fumeeMoyenne) {
        this.foret               = foret;
        this.localisation        = localisation;
        this.mois                = mois;
        this.annee               = annee;
        this.pourcentageIncendie = pourcentageIncendie;
        this.nbDanger            = nbDanger;
        this.nbAttention         = nbAttention;
        this.nbSur               = nbSur;
        this.tempMoyenne         = tempMoyenne;
        this.humMoyenne          = humMoyenne;
        this.fumeeMoyenne        = fumeeMoyenne;
        this.dateCalcul          = LocalDateTime.now();
    }

    // ── Getters / Setters ─────────────────────────────────
    public int    getId()                              { return id; }
    public void   setId(int id)                        { this.id = id; }

    public String getForet()                           { return foret; }
    public void   setForet(String foret)               { this.foret = foret; }

    public String getLocalisation()                    { return localisation; }
    public void   setLocalisation(String localisation) { this.localisation = localisation; }

    public int    getMois()                            { return mois; }
    public void   setMois(int mois)                    { this.mois = mois; }

    public int    getAnnee()                           { return annee; }
    public void   setAnnee(int annee)                  { this.annee = annee; }

    public double getPourcentageIncendie()             { return pourcentageIncendie; }
    public void   setPourcentageIncendie(double p)     { this.pourcentageIncendie = p; }

    public int    getNbDanger()                        { return nbDanger; }
    public void   setNbDanger(int nbDanger)            { this.nbDanger = nbDanger; }

    public int    getNbAttention()                     { return nbAttention; }
    public void   setNbAttention(int nbAttention)      { this.nbAttention = nbAttention; }

    public int    getNbSur()                           { return nbSur; }
    public void   setNbSur(int nbSur)                  { this.nbSur = nbSur; }

    public double getTempMoyenne()                     { return tempMoyenne; }
    public void   setTempMoyenne(double tempMoyenne)   { this.tempMoyenne = tempMoyenne; }

    public double getHumMoyenne()                      { return humMoyenne; }
    public void   setHumMoyenne(double humMoyenne)     { this.humMoyenne = humMoyenne; }

    public double getFumeeMoyenne()                    { return fumeeMoyenne; }
    public void   setFumeeMoyenne(double fumeeMoyenne) { this.fumeeMoyenne = fumeeMoyenne; }

    public LocalDateTime getDateCalcul()               { return dateCalcul; }
    public void   setDateCalcul(LocalDateTime d)       { this.dateCalcul = d; }

    // ── Nom du mois en français ───────────────────────────
    public String getNomMois() {
        String[] mois = {"", "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
                "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"};
        return (this.mois >= 1 && this.mois <= 12) ? mois[this.mois] : "?";
    }

    // ── Niveau de risque global ───────────────────────────
    public String getNiveauRisqueGlobal() {
        if (pourcentageIncendie >= 60) return "CRITIQUE";
        if (pourcentageIncendie >= 35) return "ELEVE";
        if (pourcentageIncendie >= 15) return "MOYEN";
        return "FAIBLE";
    }

    public String getCouleurRisque() {
        return switch (getNiveauRisqueGlobal()) {
            case "CRITIQUE" -> "#dc2626";
            case "ELEVE"    -> "#f97316";
            case "MOYEN"    -> "#eab308";
            default         -> "#16a34a";
        };
    }

    @Override
    public String toString() {
        return "RapportStatistique{foret='" + foret + "', " +
                getNomMois() + " " + annee + ", " +
                "risque=" + String.format("%.1f", pourcentageIncendie) + "%, " +
                "D=" + nbDanger + " A=" + nbAttention + " S=" + nbSur + "}";
    }
}
