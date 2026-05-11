package model;
    public class Animal {
        private int id;
        private int idZone;
        private String espece;
        private String nomScientifique;
        private int populationEstimee;
        private String statutProtection;
        private String derniereObservation;

        public Animal(int idZone, String espece, String nomScientifique,
                      int populationEstimee, String statutProtection, String derniereObservation) {
            this.idZone = idZone;
            this.espece = espece;
            this.nomScientifique = nomScientifique;
            this.populationEstimee = populationEstimee;
            this.statutProtection = statutProtection;
            this.derniereObservation = derniereObservation;
        }

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public int getIdZone() { return idZone; }
        public void setIdZone(int idZone) { this.idZone = idZone; }

        public String getEspece() { return espece; }
        public void setEspece(String espece) { this.espece = espece; }

        public String getNomScientifique() { return nomScientifique; }
        public void setNomScientifique(String nomScientifique) { this.nomScientifique = nomScientifique; }

        public int getPopulationEstimee() { return populationEstimee; }
        public void setPopulationEstimee(int populationEstimee) { this.populationEstimee = populationEstimee; }

        public String getStatutProtection() { return statutProtection; }
        public void setStatutProtection(String statutProtection) { this.statutProtection = statutProtection; }

        public String getDerniereObservation() { return derniereObservation; }
        public void setDerniereObservation(String derniereObservation) { this.derniereObservation = derniereObservation; }

        @Override
        public String toString() {
            return "Animal{" +
                    "id=" + id +
                    ", idZone=" + idZone +
                    ", espece='" + espece + '\'' +
                    ", nomScientifique='" + nomScientifique + '\'' +
                    ", populationEstimee=" + populationEstimee +
                    ", statutProtection='" + statutProtection + '\'' +
                    ", derniereObservation='" + derniereObservation + '\'' +
                    '}';
        }
    }


