package model;

public class Foret {
        private int id;
        private String nom;
        private String localisation;
        private double superficie;
        private String typeVegetation;
        private String niveauRisque;
        private String datecreation;
        private double latitude;
        private double longitude;
        public Foret(String nom, String localisation, double superficie, String typeVegetation, String niveauRisque,String datecreation) {
            this.nom = nom;
            this.localisation = localisation;
            this.superficie = superficie;
            this.typeVegetation = typeVegetation;
            this.niveauRisque = niveauRisque;
            this.datecreation = datecreation;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getNom() {
            return nom;
        }

        public void setNom(String nom) {
            this.nom = nom;
        }

        public String getLocalisation() {
            return localisation;
        }

        public void setLocalisation(String localisation) {
            this.localisation = localisation;
        }

        public double getSuperficie() {
            return superficie;
        }

        public void setSuperficie(double superficie) {
            this.superficie = superficie;
        }

        public String getTypeVegetation() {
            return typeVegetation;
        }

        public void setTypeVegetation(String typeVegetation) {
            this.typeVegetation = typeVegetation;
        }

        public String getNiveauRisque() {
            return niveauRisque;
        }

        public void setNiveauRisque(String niveauRisque) {
            this.niveauRisque = niveauRisque;
        }

        public String getDateCreation() {
            return datecreation;
        }
        public double getLatitude() {
            return latitude;
    }

        public double getLongitude() {
            return longitude;
    }

        public void setDateCreation(String dateCreation) {
            this.datecreation = dateCreation;
        }
        public void setLatitude(double latitude) {
            this.latitude = latitude;
    }

        public void setLongitude(double longitude) {
            this.longitude = longitude;
    }


        @Override
        public String toString() {
            return "Foret{" +
                    "id=" + id +
                    ", nom='" + nom + '\'' +
                    ", localisation='" + localisation + '\'' +
                    ", superficie=" + superficie +
                    ", typeVegetation='" + typeVegetation + '\'' +
                    ", niveauRisque='" + niveauRisque + '\'' +
                    ", datecreation='" + datecreation + '\''+
                    ", latitude='" + latitude + '\'' +
                    ", longitude='" + longitude + '\''+

                    '}';
        }
    }


