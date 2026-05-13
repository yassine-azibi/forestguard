package model;

import java.time.LocalDate;

public class Incendie {
        private int id;
        private int idZone;
        private String dateDebut;
        private String dateFin;
        private double superficieBrulee;
        private String cause;
        private String niveauGravite;
        private String statut;

        public Incendie(int idZone, String dateDebut, String dateFin, double superficieBrulee,
                        String cause, String niveauGravite, String statut) {
            this.idZone = idZone;
            this.dateDebut = dateDebut;
            this.dateFin = dateFin;
            this.superficieBrulee = superficieBrulee;
            this.cause = cause;
            this.niveauGravite = niveauGravite;
            this.statut = statut;
        }

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public int getIdZone() { return idZone; }
        public void setIdZone(int idZone) { this.idZone = idZone; }

        public String getDateDebut() { return dateDebut; }
        public void setDateDebut(String dateDebut) { this.dateDebut = dateDebut; }

        public String getDateFin() { return dateFin; }
        public void setDateFin(String dateFin) { this.dateFin = dateFin; }

        public double getSuperficieBrulee() { return superficieBrulee; }
        public void setSuperficieBrulee(double superficieBrulee) { this.superficieBrulee = superficieBrulee; }

        public String getCause() { return cause; }
        public void setCause(String cause) { this.cause = cause; }

        public String getNiveauGravite() { return niveauGravite; }
        public void setNiveauGravite(String niveauGravite) { this.niveauGravite = niveauGravite; }

        public String getStatut() { return statut; }
        public void setStatut(String statut) { this.statut = statut; }

        @Override
        public String toString() {
            return "Incendie{" +
                    "id=" + id +
                    ", idZone=" + idZone +
                    ", dateDebut='" + dateDebut + '\'' +
                    ", dateFin='" + dateFin + '\'' +
                    ", superficieBrulee=" + superficieBrulee +
                    ", cause='" + cause + '\'' +
                    ", niveauGravite='" + niveauGravite + '\'' +
                    ", statut='" + statut + '\'' +
                    '}';
        }
    }



