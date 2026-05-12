package edu.pompier.services;

import edu.pompier.entities.Pompier;
import edu.pompier.interfaces.IService;
import edu.pompier.tools.MyConnection;
import edu.pompier.controllers.GestionGardesController;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PompierService implements IService<Pompier> {

    private Connection getConnection() {
        return MyConnection.getInstance().getCnx();
    }

    // ═════════════════════════════════════════
    //  CRUD POMPIER
    // ═════════════════════════════════════════

    @Override
    public void addEntity(Pompier p) {
        // Essayer avec photo_path, fallback sans si la colonne n'existe pas encore
        String req = "INSERT INTO pompier (nom, prenom, email, telephone, mot_de_passe, statut, zone_id, " +
                "ville, zone_adresse, latitude, longitude, photo_path) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement ps = getConnection().prepareStatement(req);
            ps.setString(1, p.getNom());
            ps.setString(2, p.getPrenom());
            ps.setString(3, p.getEmail());
            ps.setString(4, p.getTelephone());
            ps.setString(5, p.getMot_de_passe());
            ps.setString(6, p.getStatut());
            if (p.getZoneId() == 0) ps.setNull(7, Types.INTEGER); else ps.setInt(7, p.getZoneId());
            ps.setString(8, p.getVille());
            ps.setString(9, p.getZoneAdresse());
            ps.setDouble(10, p.getLatitude());
            ps.setDouble(11, p.getLongitude());
            ps.setString(12, p.getPhotoPath());
            ps.executeUpdate();
        } catch (SQLException e) {
            // Fallback sans photo_path si la colonne n'existe pas encore
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("photo_path")) {
                String reqFallback = "INSERT INTO pompier (nom, prenom, email, telephone, mot_de_passe, statut, zone_id, " +
                        "ville, zone_adresse, latitude, longitude) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try {
                    PreparedStatement ps2 = getConnection().prepareStatement(reqFallback);
                    ps2.setString(1, p.getNom()); ps2.setString(2, p.getPrenom());
                    ps2.setString(3, p.getEmail()); ps2.setString(4, p.getTelephone());
                    ps2.setString(5, p.getMot_de_passe()); ps2.setString(6, p.getStatut());
                    if (p.getZoneId() == 0) ps2.setNull(7, Types.INTEGER); else ps2.setInt(7, p.getZoneId());
                    ps2.setString(8, p.getVille()); ps2.setString(9, p.getZoneAdresse());
                    ps2.setDouble(10, p.getLatitude()); ps2.setDouble(11, p.getLongitude());
                    ps2.executeUpdate();
                } catch (SQLException e2) { System.out.println("Erreur addEntity fallback : " + e2.getMessage()); }
            } else { System.out.println("Erreur addEntity : " + e.getMessage()); }
        }
    }

    public int addEntityAndGetId(Pompier p) {
        String req = "INSERT INTO pompier (nom, prenom, email, telephone, mot_de_passe, statut, zone_id, " +
                "ville, zone_adresse, latitude, longitude, photo_path) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement ps = getConnection().prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, p.getNom()); ps.setString(2, p.getPrenom());
            ps.setString(3, p.getEmail()); ps.setString(4, p.getTelephone());
            ps.setString(5, p.getMot_de_passe()); ps.setString(6, p.getStatut());
            if (p.getZoneId() == 0) ps.setNull(7, Types.INTEGER); else ps.setInt(7, p.getZoneId());
            ps.setString(8, p.getVille()); ps.setString(9, p.getZoneAdresse());
            ps.setDouble(10, p.getLatitude()); ps.setDouble(11, p.getLongitude());
            ps.setString(12, p.getPhotoPath());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            // Fallback sans photo_path
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("photo_path")) {
                String reqFallback = "INSERT INTO pompier (nom, prenom, email, telephone, mot_de_passe, statut, zone_id, " +
                        "ville, zone_adresse, latitude, longitude) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try {
                    PreparedStatement ps2 = getConnection().prepareStatement(reqFallback, Statement.RETURN_GENERATED_KEYS);
                    ps2.setString(1, p.getNom()); ps2.setString(2, p.getPrenom());
                    ps2.setString(3, p.getEmail()); ps2.setString(4, p.getTelephone());
                    ps2.setString(5, p.getMot_de_passe()); ps2.setString(6, p.getStatut());
                    if (p.getZoneId() == 0) ps2.setNull(7, Types.INTEGER); else ps2.setInt(7, p.getZoneId());
                    ps2.setString(8, p.getVille()); ps2.setString(9, p.getZoneAdresse());
                    ps2.setDouble(10, p.getLatitude()); ps2.setDouble(11, p.getLongitude());
                    ps2.executeUpdate();
                    ResultSet rs2 = ps2.getGeneratedKeys();
                    if (rs2.next()) return rs2.getInt(1);
                } catch (SQLException e2) { System.out.println("Erreur addEntityAndGetId fallback : " + e2.getMessage()); }
            } else { System.out.println("Erreur addEntityAndGetId : " + e.getMessage()); }
        }
        return 0;
    }

    @Override
    public void deleteEntity(Pompier p) {
        try {
            PreparedStatement ps1 = getConnection().prepareStatement("DELETE FROM pompier_certification WHERE id_pompier=?");
            ps1.setInt(1, p.getId()); ps1.executeUpdate();
            PreparedStatement ps2 = getConnection().prepareStatement("DELETE FROM garde WHERE id_pompier=?");
            ps2.setInt(1, p.getId()); ps2.executeUpdate();
            PreparedStatement ps3 = getConnection().prepareStatement("DELETE FROM pompier WHERE id=?");
            ps3.setInt(1, p.getId()); ps3.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur deleteEntity : " + e.getMessage());
        }
    }

    @Override
    public void updateEntity(int id, Pompier p) {
        String req = "UPDATE pompier SET nom=?, prenom=?, email=?, telephone=?, statut=?, zone_id=?, " +
                "ville=?, zone_adresse=?, latitude=?, longitude=?, photo_path=? WHERE id=?";
        try {
            PreparedStatement ps = getConnection().prepareStatement(req);
            ps.setString(1, p.getNom()); ps.setString(2, p.getPrenom());
            ps.setString(3, p.getEmail()); ps.setString(4, p.getTelephone());
            ps.setString(5, p.getStatut());
            if (p.getZoneId() == 0) ps.setNull(6, Types.INTEGER); else ps.setInt(6, p.getZoneId());
            ps.setString(7, p.getVille()); ps.setString(8, p.getZoneAdresse());
            ps.setDouble(9, p.getLatitude()); ps.setDouble(10, p.getLongitude());
            ps.setString(11, p.getPhotoPath());
            ps.setInt(12, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            // Fallback sans photo_path
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("photo_path")) {
                String reqFallback = "UPDATE pompier SET nom=?, prenom=?, email=?, telephone=?, statut=?, zone_id=?, " +
                        "ville=?, zone_adresse=?, latitude=?, longitude=? WHERE id=?";
                try {
                    PreparedStatement ps2 = getConnection().prepareStatement(reqFallback);
                    ps2.setString(1, p.getNom()); ps2.setString(2, p.getPrenom());
                    ps2.setString(3, p.getEmail()); ps2.setString(4, p.getTelephone());
                    ps2.setString(5, p.getStatut());
                    if (p.getZoneId() == 0) ps2.setNull(6, Types.INTEGER); else ps2.setInt(6, p.getZoneId());
                    ps2.setString(7, p.getVille()); ps2.setString(8, p.getZoneAdresse());
                    ps2.setDouble(9, p.getLatitude()); ps2.setDouble(10, p.getLongitude());
                    ps2.setInt(11, id);
                    ps2.executeUpdate();
                } catch (SQLException e2) { System.out.println("Erreur updateEntity fallback : " + e2.getMessage()); }
            } else { System.out.println("Erreur updateEntity : " + e.getMessage()); }
        }
    }

    @Override
    public List<Pompier> getData() {
        List<Pompier> liste = new ArrayList<>();
        // ✅ CORRIGÉ : relit ville, zone_adresse, latitude, longitude depuis la base
        String req =
                "SELECT p.*, f.nom as nom_foret, f.localisation as localisation_foret, " +
                        "c.niveau_requis as niveau_certif, " +
                        "MIN(g.creneau) as creneau_garde, " +
                        "MIN(g.date_garde) as date_garde_min, " +
                        "MAX(g.date_garde) as date_garde_max " +
                        "FROM pompier p " +
                        "LEFT JOIN foret f ON p.zone_id = f.id " +
                        "LEFT JOIN pompier_certification pc ON pc.id_pompier = p.id " +
                        "LEFT JOIN certification c ON c.id = pc.id_certification " +
                        "LEFT JOIN garde g ON g.id_pompier = p.id " +
                        "GROUP BY p.id";
        try {
            Statement st = getConnection().createStatement();
            ResultSet rs = st.executeQuery(req);
            while (rs.next()) {
                Pompier p = new Pompier(
                        rs.getString("nom"), rs.getString("prenom"),
                        rs.getString("email"), rs.getString("telephone"),
                        rs.getString("mot_de_passe"), rs.getString("statut"),
                        rs.getInt("zone_id")
                );
                p.setId(rs.getInt("id"));
                p.setNomForet(rs.getString("nom_foret"));
                p.setLocalisationForet(rs.getString("localisation_foret"));
                p.setNiveauCertification(rs.getString("niveau_certif"));
                // ✅ CORRIGÉ : récupération des champs localisation
                p.setVille(rs.getString("ville"));
                p.setZoneAdresse(rs.getString("zone_adresse"));
                p.setLatitude(rs.getDouble("latitude"));
                p.setLongitude(rs.getDouble("longitude"));
                // photo_path : colonne optionnelle (peut ne pas exister encore en BDD)
                try { p.setPhotoPath(rs.getString("photo_path")); } catch (Exception ignored) {}
                // Garde
                String creneau = rs.getString("creneau_garde");
                String dateMin  = rs.getString("date_garde_min");
                String dateMax  = rs.getString("date_garde_max");
                p.setCreneauGarde(creneau);
                if (dateMin != null && dateMax != null && !dateMin.equals(dateMax))
                    p.setDateGarde(dateMin + " → " + dateMax);
                else
                    p.setDateGarde(dateMin);
                liste.add(p);
            }
        } catch (SQLException e) {
            System.out.println("Erreur getData : " + e.getMessage());
        }
        return liste;
    }

    // ═════════════════════════════════════════
    //  CONTRÔLES D'UNICITÉ
    // ═════════════════════════════════════════

    public boolean emailExiste(String email, int excludeId) {
        try {
            String sql = excludeId > 0
                    ? "SELECT COUNT(*) FROM pompier WHERE email=? AND id!=?"
                    : "SELECT COUNT(*) FROM pompier WHERE email=?";
            PreparedStatement ps = getConnection().prepareStatement(sql);
            ps.setString(1, email);
            if (excludeId > 0) ps.setInt(2, excludeId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.out.println("Erreur emailExiste : " + e.getMessage());
        }
        return false;
    }

    public boolean telephoneExiste(String telephone, int excludeId) {
        if (telephone == null || telephone.isBlank()) return false;
        try {
            String sql = excludeId > 0
                    ? "SELECT COUNT(*) FROM pompier WHERE telephone=? AND id!=?"
                    : "SELECT COUNT(*) FROM pompier WHERE telephone=?";
            PreparedStatement ps = getConnection().prepareStatement(sql);
            ps.setString(1, telephone);
            if (excludeId > 0) ps.setInt(2, excludeId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.out.println("Erreur telephoneExiste : " + e.getMessage());
        }
        return false;
    }

    // ═════════════════════════════════════════
    //  FORETS
    // ═════════════════════════════════════════

    public List<String[]> getForets() {
        List<String[]> forets = new ArrayList<>();
        try {
            Statement st = getConnection().createStatement();
            ResultSet rs = st.executeQuery("SELECT id, nom FROM foret");
            while (rs.next())
                forets.add(new String[]{rs.getString("id"), rs.getString("nom")});
        } catch (SQLException e) {
            System.out.println("Erreur getForets : " + e.getMessage());
        }
        return forets;
    }

    // ═════════════════════════════════════════
    //  CERTIFICATION
    // ═════════════════════════════════════════

    public void addCertification(int idPompier, String niveau) {
        try {
            PreparedStatement ps = getConnection().prepareStatement("SELECT id FROM certification WHERE niveau_requis=? LIMIT 1");
            ps.setString(1, niveau);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int idCertif = rs.getInt("id");
                PreparedStatement ps2 = getConnection().prepareStatement(
                        "INSERT INTO pompier_certification (id_pompier, id_certification, date_obtention, date_expiration) VALUES (?, ?, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 1 YEAR))");
                ps2.setInt(1, idPompier); ps2.setInt(2, idCertif);
                ps2.executeUpdate();
            }
        } catch (SQLException e) {
            System.out.println("Erreur addCertification : " + e.getMessage());
        }
    }

    public String[] getCertificationPompier(int idPompier) {
        try {
            PreparedStatement ps = getConnection().prepareStatement(
                    "SELECT c.niveau_requis FROM certification c JOIN pompier_certification pc ON c.id = pc.id_certification WHERE pc.id_pompier=? LIMIT 1");
            ps.setInt(1, idPompier);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return new String[]{ rs.getString("niveau_requis") };
        } catch (SQLException e) {
            System.out.println("Erreur getCertificationPompier : " + e.getMessage());
        }
        return null;
    }

    public void updateCertification(int idPompier, String niveau) {
        try {
            PreparedStatement ps = getConnection().prepareStatement("SELECT id FROM certification WHERE niveau_requis=? LIMIT 1");
            ps.setString(1, niveau);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return;
            int idCertif = rs.getInt("id");
            PreparedStatement psCheck = getConnection().prepareStatement("SELECT id FROM pompier_certification WHERE id_pompier=? LIMIT 1");
            psCheck.setInt(1, idPompier);
            ResultSet rsCheck = psCheck.executeQuery();
            if (rsCheck.next()) {
                PreparedStatement psUp = getConnection().prepareStatement(
                        "UPDATE pompier_certification SET id_certification=?, date_obtention=CURDATE(), date_expiration=DATE_ADD(CURDATE(), INTERVAL 1 YEAR) WHERE id_pompier=?");
                psUp.setInt(1, idCertif); psUp.setInt(2, idPompier);
                psUp.executeUpdate();
            } else {
                addCertification(idPompier, niveau);
            }
        } catch (SQLException e) {
            System.out.println("Erreur updateCertification : " + e.getMessage());
        }
    }

    // ═════════════════════════════════════════
    //  GARDE
    // ═════════════════════════════════════════

    public boolean gardeConflitExiste(int idPompier, String creneau, String dateDebut, String dateFin, int excludeGardeId) {
        try {
            String sql = excludeGardeId > 0
                    ? "SELECT COUNT(*) FROM garde WHERE id_pompier=? AND creneau=? AND date_garde BETWEEN ? AND ? AND id!=?"
                    : "SELECT COUNT(*) FROM garde WHERE id_pompier=? AND creneau=? AND date_garde BETWEEN ? AND ?";
            PreparedStatement ps = getConnection().prepareStatement(sql);
            ps.setInt(1, idPompier); ps.setString(2, creneau);
            ps.setString(3, dateDebut); ps.setString(4, dateFin);
            if (excludeGardeId > 0) ps.setInt(5, excludeGardeId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.out.println("Erreur gardeConflitExiste : " + e.getMessage());
        }
        return false;
    }

    public void addGardePeriode(int idPompier, String creneau, String dateDebut, String dateFin) {
        try {
            PreparedStatement ps = getConnection().prepareStatement(
                    "INSERT INTO garde (id_pompier, date_garde, creneau, disponible) VALUES (?, ?, ?, 1)");
            java.time.LocalDate d = java.time.LocalDate.parse(dateDebut);
            java.time.LocalDate fin = java.time.LocalDate.parse(dateFin);
            while (!d.isAfter(fin)) {
                ps.setInt(1, idPompier); ps.setString(2, d.toString()); ps.setString(3, creneau);
                ps.addBatch(); d = d.plusDays(1);
            }
            ps.executeBatch();
        } catch (SQLException e) {
            System.out.println("Erreur addGardePeriode : " + e.getMessage());
        }
    }

    public void deleteGardesByPompierCreneau(int idPompier, String creneau) {
        try {
            PreparedStatement ps = getConnection().prepareStatement("DELETE FROM garde WHERE id_pompier=? AND creneau=?");
            ps.setInt(1, idPompier); ps.setString(2, creneau); ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur deleteGardesByPompierCreneau : " + e.getMessage());
        }
    }

    public void addGarde(int idPompier, String creneau, String dateGarde) {
        addGardePeriode(idPompier, creneau, dateGarde, dateGarde);
    }

    public void updateGardeById(int idGarde, String creneau, String dateGarde) {
        try {
            PreparedStatement ps = getConnection().prepareStatement("UPDATE garde SET creneau=?, date_garde=? WHERE id=?");
            ps.setString(1, creneau); ps.setString(2, dateGarde); ps.setInt(3, idGarde);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur updateGardeById : " + e.getMessage());
        }
    }

    public void deleteGardeById(int idGarde) {
        try {
            PreparedStatement ps = getConnection().prepareStatement("DELETE FROM garde WHERE id=?");
            ps.setInt(1, idGarde); ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur deleteGardeById : " + e.getMessage());
        }
    }

    public List<GestionGardesController.GardeInfo> getAllGardesInfo() {
        List<GestionGardesController.GardeInfo> list = new ArrayList<>();
        String req = "SELECT g.id, g.id_pompier, p.nom, p.prenom, p.telephone, g.creneau, g.date_garde " +
                "FROM garde g JOIN pompier p ON g.id_pompier = p.id ORDER BY g.id_pompier, g.date_garde ASC";
        try {
            Statement st = getConnection().createStatement();
            ResultSet rs = st.executeQuery(req);
            while (rs.next()) {
                list.add(new GestionGardesController.GardeInfo(
                        rs.getInt("id"), rs.getInt("id_pompier"),
                        rs.getString("nom") + " " + rs.getString("prenom"),
                        rs.getString("telephone"), rs.getString("creneau"),
                        rs.getString("date_garde") != null ? rs.getString("date_garde") : ""
                ));
            }
        } catch (SQLException e) {
            System.out.println("Erreur getAllGardesInfo : " + e.getMessage());
        }
        return list;
    }

    // ═══════════════════════════════════════════════════════════════
    //  AFFECTATION AUTOMATIQUE
    // ═══════════════════════════════════════════════════════════════

    public static class ResultatAffectation {
        public final Pompier pompier;
        public final double score;
        public final double distanceKm;
        public final String raison;

        public ResultatAffectation(Pompier pompier, double score, double distanceKm, String raison) {
            this.pompier = pompier; this.score = score;
            this.distanceKm = distanceKm; this.raison = raison;
        }
    }

    public ResultatAffectation affecterAutomatiquement(int idAlerte, String localisationIncendie) {
        List<Pompier> candidats = getPompiersDisponibles();
        if (candidats.isEmpty()) return null;

        Pompier meilleur = null;
        double meilleurScore = Double.NEGATIVE_INFINITY;
        double distanceMeilleur = 0;

        for (Pompier p : candidats) {
            double dist  = calculerDistancePompier(p, localisationIncendie);
            double score = calculerScore(p, dist);
            if (score > meilleurScore) {
                meilleurScore = score; meilleur = p; distanceMeilleur = dist;
            }
        }

        if (meilleur == null) return null;
        insererAffectation(idAlerte, meilleur.getId(), meilleurScore, distanceMeilleur);
        passerEnMission(meilleur.getId());
        return new ResultatAffectation(meilleur, meilleurScore, distanceMeilleur,
                construireRaison(meilleur, distanceMeilleur, meilleurScore));
    }

    /**
     * Variante qui gère le cas ex-aequo :
     * Si plusieurs pompiers ont exactement le même score maximum,
     * ils sont TOUS affectés et reçoivent chacun un email.
     * Cas normal (un seul meilleur) : liste d'un seul élément.
     */
    // Verrou pour éviter d'affecter deux fois la même alerte entre deux cycles du watcher
    private final java.util.Set<Integer> alertesEnCours = java.util.Collections.synchronizedSet(new java.util.HashSet<>());

    /**
     * Liste noire des alertes dont l'affectation a été supprimée manuellement par l'admin.
     * Le watcher ignore ces alertes → elles ne seront plus ré-affectées automatiquement.
     * Persisté en base dans la table affectation avec statut='annule'.
     */
    private final java.util.Set<Integer> alertesAnnulees = java.util.Collections.synchronizedSet(new java.util.HashSet<>());

    public List<ResultatAffectation> affecterAutomatiquementTous(int idAlerte, String localisationIncendie) {
        // Si déjà en cours de traitement, on ignore
        if (!alertesEnCours.add(idAlerte)) return new ArrayList<>();
        try {
            List<Pompier> candidats = getPompiersDisponibles();
            if (candidats.isEmpty()) { alertesEnCours.remove(idAlerte); return new ArrayList<>(); }

            // Calcul des scores pour tous les candidats
            double meilleurScore = Double.NEGATIVE_INFINITY;
            for (Pompier p : candidats) {
                double dist  = calculerDistancePompier(p, localisationIncendie);
                double score = calculerScore(p, dist);
                if (score > meilleurScore) meilleurScore = score;
            }

            // Collecte tous ceux qui ont le score maximum
            List<ResultatAffectation> resultats = new ArrayList<>();
            for (Pompier p : candidats) {
                double dist  = calculerDistancePompier(p, localisationIncendie);
                double score = calculerScore(p, dist);
                if (Math.abs(score - meilleurScore) < 0.001) { // ex-aequo
                    insererAffectation(idAlerte, p.getId(), score, dist);
                    passerEnMission(p.getId());
                    resultats.add(new ResultatAffectation(p, score, dist,
                            construireRaison(p, dist, score)));
                }
            }
            return resultats;
        } finally {
            // Libère le verrou après traitement (l'alerte n'est plus "Nouvelle" de toute façon)
            alertesEnCours.remove(idAlerte);
        }
    }

    private List<Pompier> getPompiersDisponibles() {
        List<Pompier> liste = new ArrayList<>();
        String req =
                "SELECT p.*, f.nom as nom_foret, f.localisation as localisation_foret, " +
                        "c.niveau_requis as niveau_certif, " +
                        "COUNT(DISTINCT g.id) as nb_gardes, " +
                        "COUNT(DISTINCT a.id) as nb_missions_actives " +
                        "FROM pompier p " +
                        "LEFT JOIN foret f ON p.zone_id = f.id " +
                        "LEFT JOIN pompier_certification pc ON pc.id_pompier = p.id " +
                        "LEFT JOIN certification c ON c.id = pc.id_certification " +
                        "LEFT JOIN garde g ON g.id_pompier = p.id " +
                        "LEFT JOIN affectation a ON a.id_pompier = p.id AND a.statut = 'en_cours' " +
                        "WHERE p.statut = 'disponible' GROUP BY p.id";
        try {
            Statement st = getConnection().createStatement();
            ResultSet rs = st.executeQuery(req);
            while (rs.next()) {
                Pompier p = new Pompier(rs.getString("nom"), rs.getString("prenom"),
                        rs.getString("email"), rs.getString("telephone"),
                        rs.getString("mot_de_passe"), rs.getString("statut"), rs.getInt("zone_id"));
                p.setId(rs.getInt("id"));
                p.setNomForet(rs.getString("nom_foret"));
                p.setLocalisationForet(rs.getString("localisation_foret"));
                p.setNiveauCertification(rs.getString("niveau_certif"));
                p.setNbGardes(rs.getInt("nb_gardes"));
                p.setNbMissionsActives(rs.getInt("nb_missions_actives"));
                // ✅ Coordonnées GPS du pompier pour calcul de distance précis
                p.setVille(rs.getString("ville"));
                p.setZoneAdresse(rs.getString("zone_adresse"));
                p.setLatitude(rs.getDouble("latitude"));
                p.setLongitude(rs.getDouble("longitude"));
                liste.add(p);
            }
        } catch (SQLException e) {
            System.out.println("Erreur getPompiersDisponibles : " + e.getMessage());
        }
        return liste;
    }

    /**
     * Priorité 1 : GPS du pompier (lat/lon) si renseigné → Haversine exacte
     * Priorité 2 : localisation textuelle de la forêt assignée → fallback
     */
    private double calculerDistancePompier(Pompier p, String localisationIncendie) {
        if (p.getLatitude() != 0 && p.getLongitude() != 0) {
            double[] coordIncendie = extraireCoordonnees(localisationIncendie);
            if (coordIncendie != null)
                return haversine(p.getLatitude(), p.getLongitude(), coordIncendie[0], coordIncendie[1]);
        }
        return calculerDistance(p.getLocalisationForet(), localisationIncendie);
    }

    /**
     * Calcule le score de sélection d'un pompier pour une alerte.
     * Critères (pondérés) :
     *   - Distance       : max 100 pts (0 pt à 100+ km)
     *   - Expertise      : EXPERT +40, AVANCE +30, INTERMEDIAIRE +20, DEBUTANT +5
     *   - Foret assignée : bonus +25 si le pompier est assigné à la même zone que l'incendie
     *   - Charge gardes  : -5 pts par garde planifiée
     *   - Missions actives : -10 pts par mission en cours
     */
    private double calculerScore(Pompier p, double distanceKm) {
        double score = Math.max(0, 100.0 - distanceKm);
        String niveau = p.getNiveauCertification();
        if (niveau != null) {
            switch (niveau.toUpperCase()) {
                case "EXPERT"        -> score += 40;
                case "AVANCE"        -> score += 30;
                case "INTERMEDIAIRE" -> score += 20;
                default              -> score += 5;
            }
        }
        score -= p.getNbGardes() * 5;
        score -= p.getNbMissionsActives() * 10;
        return score;
    }

    public double calculerDistance(String localisationPompier, String localisationIncendie) {
        if (localisationPompier == null || localisationIncendie == null) return 999.0;
        double[] c1 = extraireCoordonnees(localisationPompier);
        double[] c2 = extraireCoordonnees(localisationIncendie);
        if (c1 != null && c2 != null) return haversine(c1[0], c1[1], c2[0], c2[1]);
        String lp = localisationPompier.toLowerCase().trim();
        String li = localisationIncendie.toLowerCase().trim();
        if (lp.equals(li)) return 5.0;
        for (String mot : lp.split("[\\s,]+"))
            if (mot.length() > 3 && li.contains(mot)) return 20.0;
        return 50.0;
    }

    private double[] extraireCoordonnees(String localisation) {
        try {
            String[] parts = localisation.split(",");
            if (parts.length == 2) {
                double lat = Double.parseDouble(parts[0].trim());
                double lng = Double.parseDouble(parts[1].trim());
                if (lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180) return new double[]{lat, lng};
            }
        } catch (NumberFormatException ignored) {}
        return null;
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2)
                + Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon/2)*Math.sin(dLon/2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private void insererAffectation(int idAlerte, int idPompier, double score, double distanceKm) {
        try {
            PreparedStatement ps = getConnection().prepareStatement(
                    "INSERT INTO affectation (id_alerte, id_pompier, statut, score_selection, distance_km, mode_affectation) " +
                            "VALUES (?, ?, 'en_cours', ?, ?, 'automatique')");
            ps.setInt(1, idAlerte); ps.setInt(2, idPompier);
            ps.setDouble(3, score); ps.setDouble(4, distanceKm);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur insererAffectation : " + e.getMessage());
        }
    }

    private void passerEnMission(int idPompier) {
        try {
            PreparedStatement ps = getConnection().prepareStatement("UPDATE pompier SET statut='en_mission' WHERE id=?");
            ps.setInt(1, idPompier); ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur passerEnMission : " + e.getMessage());
        }
    }

    private String construireRaison(Pompier p, double distanceKm, double score) {
        String expertise = p.getNiveauCertification() != null ? p.getNiveauCertification().toUpperCase() : "N/A";
        String localisation = (p.getVille() != null && p.getZoneAdresse() != null)
                ? p.getVille() + " - " + p.getZoneAdresse()
                : (p.getNomForet() != null ? "Forêt " + p.getNomForet() : "Inconnue");
        return String.format(
                "Pompier sélectionné : %s %s\n• Localisation : %s\n• Distance : %.1f km\n• Expertise : %s\n• Gardes planifiées : %d\n• Score final : %.1f pts",
                p.getNom(), p.getPrenom(), localisation, distanceKm, expertise, p.getNbGardes(), score);
    }

    // ─────────────────────────────────────────
    //  Alertes & Affectations (UI)
    // ─────────────────────────────────────────

    public List<String[]> getAlertesNonAffectees() {
        List<String[]> alertes = new ArrayList<>();
        // Exclut : alertes déjà en cours + alertes annulées (statut='annule' en base)
        String sql = "SELECT a.id, a.type_alerte, a.niveau, a.localisation, a.date_alerte FROM alerte a " +
                "WHERE a.statut = 'Nouvelle' " +
                "AND a.id NOT IN (SELECT id_alerte FROM affectation WHERE statut IN ('en_cours','annule')) " +
                "ORDER BY a.date_alerte DESC";
        try {
            Statement st = getConnection().createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                int id = rs.getInt("id");
                // Double protection : liste noire en mémoire
                if (alertesAnnulees.contains(id)) continue;
                alertes.add(new String[]{rs.getString("id"), rs.getString("type_alerte"),
                        rs.getString("niveau"), rs.getString("localisation"), rs.getString("date_alerte")});
            }
        } catch (SQLException e) {
            System.out.println("Erreur getAlertesNonAffectees : " + e.getMessage());
        }
        return alertes;
    }

    /** Charge en mémoire les alertes déjà annulées (au démarrage) */
    public void chargerAlertesAnnulees() {
        String sql = "SELECT DISTINCT id_alerte FROM affectation WHERE statut='annule'";
        try {
            Statement st = getConnection().createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) alertesAnnulees.add(rs.getInt("id_alerte"));
            System.out.println("[Service] " + alertesAnnulees.size() + " alerte(s) annulée(s) chargées.");
        } catch (SQLException e) {
            System.out.println("Erreur chargerAlertesAnnulees : " + e.getMessage());
        }
    }

    /**
     * Retourne les affectations avec le nom lisible du lieu.
     * Les coordonnées GPS "lat,lng" sont converties en nom lisible via coordsVersNom().
     * Aucune modification de la table alerte n'est nécessaire.
     *
     * Format retourné : { id_aff[0], nom[1], prenom[2], type_alerte[3], niveau[4],
     *   nom_lieu[5], date_aff[6], statut[7], distance_km[8], score[9],
     *   mode[10], id_pompier[11], id_alerte[12] }
     */
    /**
     * Retourne la liste unifiée des affectations + interventions déclarées.
     *
     * Format String[] :
     *   [0]  id (affectation) ou "IV-xxx" (intervention)
     *   [1]  nom
     *   [2]  prenom
     *   [3]  type_alerte
     *   [4]  niveau
     *   [5]  nom_lieu lisible
     *   [6]  date
     *   [7]  statut
     *   [8]  distance_km (ou "-" pour intervention)
     *   [9]  score (ou "-" pour intervention)
     *   [10] mode ("automatique", "manuelle", ou "volontaire")
     *   [11] id_pompier (ou "0" pour intervention)
     *   [12] id_alerte
     *   [13] localisation brute (coords GPS)
     *   [14] source : "affectation" | "intervention"
     */
    public List<String[]> getAffectations() {
        List<String[]> list = new ArrayList<>();

        // ── 1. Affectations normales ──
        String sql = "SELECT af.id, p.id as id_pompier, p.nom, p.prenom, a.id as id_alerte, " +
                "a.type_alerte, a.niveau, a.localisation, " +
                "af.date_affectation, af.statut, af.distance_km, af.score_selection, af.mode_affectation " +
                "FROM affectation af JOIN pompier p ON p.id = af.id_pompier " +
                "JOIN alerte a ON a.id = af.id_alerte " +
                "WHERE af.statut != 'annule' " +
                "ORDER BY af.date_affectation DESC";
        try {
            Statement st = getConnection().createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                String locBrute = rs.getString("localisation");
                list.add(new String[]{
                        rs.getString("id"),                                          // [0]
                        rs.getString("nom"),                                          // [1]
                        rs.getString("prenom"),                                       // [2]
                        rs.getString("type_alerte"),                                  // [3]
                        rs.getString("niveau"),                                       // [4]
                        coordsVersNom(locBrute),                                      // [5]
                        rs.getString("date_affectation"),                             // [6]
                        rs.getString("statut"),                                       // [7]
                        String.format("%.1f", rs.getDouble("distance_km")),           // [8]
                        String.format("%.1f", rs.getDouble("score_selection")),       // [9]
                        rs.getString("mode_affectation"),                             // [10]
                        rs.getString("id_pompier"),                                   // [11]
                        rs.getString("id_alerte"),                                    // [12]
                        locBrute,                                                     // [13]
                        "affectation"                                                 // [14]
                });
            }
        } catch (SQLException e) {
            System.out.println("Erreur getAffectations (affectations) : " + e.getMessage());
        }

        // ── 2. Interventions volontaires (déclarées par l'agent lui-même) ──
        // Exclure celles dont l'agent est déjà dans une affectation pour la même alerte
        String sqlIv = "SELECT iv.id, iv.agent_name, iv.alerte_id, iv.alerte_localisation, " +
                "iv.statut, iv.start_date, iv.alert_zone " +
                "FROM intervention iv " +
                "WHERE iv.alerte_id IS NOT NULL " +
                "AND NOT EXISTS (" +
                "  SELECT 1 FROM affectation af " +
                "  JOIN pompier p ON p.id = af.id_pompier " +
                "  WHERE af.id_alerte = iv.alerte_id " +
                "  AND CONCAT(p.prenom, ' ', p.nom) = iv.agent_name " +
                "  AND af.statut != 'annule'" +
                ") " +
                "ORDER BY iv.start_date DESC";
        // Aussi chercher les interventions sans alerte_id via alerte_localisation
        String sqlIvLoc = "SELECT iv.id, iv.agent_name, iv.alerte_id, iv.alerte_localisation, " +
                "iv.statut, iv.start_date, iv.alert_zone " +
                "FROM intervention iv " +
                "WHERE iv.alerte_id IS NULL AND iv.alerte_localisation IS NOT NULL " +
                "ORDER BY iv.start_date DESC";
        try {
            // Récupérer les alertes pour matcher localisation → id/type/niveau
            java.util.Map<String, String[]> alertesParLoc = new java.util.HashMap<>();
            java.util.Map<Integer, String[]> alertesParId  = new java.util.HashMap<>();
            String sqlA = "SELECT id, type_alerte, niveau, localisation FROM alerte";
            Statement stA = getConnection().createStatement();
            ResultSet rsA = stA.executeQuery(sqlA);
            while (rsA.next()) {
                String[] info = { rsA.getString("type_alerte"), rsA.getString("niveau"), rsA.getString("localisation") };
                alertesParLoc.put(rsA.getString("localisation"), info);
                alertesParId.put(rsA.getInt("id"), info);
            }

            for (String q : new String[]{sqlIv, sqlIvLoc}) {
                Statement stIv = getConnection().createStatement();
                ResultSet rsIv = stIv.executeQuery(q);
                while (rsIv.next()) {
                    String agentName = rsIv.getString("agent_name");
                    // Séparer "Prenom Nom" → nom=[1], prenom=[0]
                    String[] parts = agentName != null ? agentName.split(" ", 2) : new String[]{"", ""};
                    String prenom = parts[0];
                    String nom    = parts.length > 1 ? parts[1] : "";

                    // Récupérer infos alerte
                    int idAl = rsIv.getInt("alerte_id");
                    String locBrute = rsIv.getString("alerte_localisation");
                    String typeAl = "Incendie", niveauAl = "-";
                    if (idAl > 0 && alertesParId.containsKey(idAl)) {
                        String[] info = alertesParId.get(idAl);
                        typeAl = info[0]; niveauAl = info[1];
                        if (locBrute == null) locBrute = info[2];
                    } else if (locBrute != null && alertesParLoc.containsKey(locBrute)) {
                        String[] info = alertesParLoc.get(locBrute);
                        typeAl = info[0]; niveauAl = info[1];
                    }

                    String startDate = rsIv.getString("start_date");
                    if (startDate != null && startDate.length() > 16) startDate = startDate.substring(0, 16);

                    list.add(new String[]{
                            "IV-" + rsIv.getString("id"),  // [0] id préfixé "IV-"
                            nom,                            // [1]
                            prenom,                         // [2]
                            typeAl != null ? typeAl : "Incendie", // [3]
                            niveauAl != null ? niveauAl : "-",    // [4]
                            locBrute != null ? coordsVersNom(locBrute) : rsIv.getString("alert_zone"), // [5]
                            startDate != null ? startDate : "-",  // [6]
                            rsIv.getString("statut") != null ? rsIv.getString("statut") : "en cours",  // [7]
                            "-",                            // [8] pas de distance calculée
                            "-",                            // [9] pas de score
                            "volontaire",                   // [10] mode
                            "0",                            // [11] pas d'id pompier en base
                            String.valueOf(idAl),           // [12]
                            locBrute != null ? locBrute : "", // [13]
                            "intervention"                  // [14] source
                    });
                }
            }
        } catch (SQLException e) {
            System.out.println("[Service] Interventions non disponibles : " + e.getMessage());
        }

        // Trier par date décroissante
        list.sort((x, y) -> {
            String dx = x[6] != null ? x[6] : "";
            String dy = y[6] != null ? y[6] : "";
            return dy.compareTo(dx);
        });

        return list;
    }

    /**
     * Convertit une localisation stockée en base en nom lisible.
     *
     * Cas 1 : coordonnées GPS "lat,lng" → cherche le nom dans le dictionnaire
     *         intégré (villes tunisiennes + ajout facile), sinon affiche "lat, lng"
     *         formaté proprement.
     * Cas 2 : texte libre → retourné tel quel.
     *
     * Pour ajouter une ville : ajouter une ligne dans le switch ci-dessous.
     */
    public static String coordsVersNom(String localisation) {
        if (localisation == null || localisation.isBlank()) return "Localisation inconnue";

        // Vérifier si c'est des coordonnées GPS "lat,lng"
        String[] parts = localisation.trim().split(",");
        if (parts.length != 2) return localisation; // texte libre → retourner tel quel
        try {
            double lat = Double.parseDouble(parts[0].trim());
            double lng = Double.parseDouble(parts[1].trim());
            if (lat < -90 || lat > 90 || lng < -180 || lng > 180) return localisation;

            // Dictionnaire de correspondance coords → nom
            // Clé : "lat_arrondie,lng_arrondie" (2 décimales)
            String cle = String.format("%.2f,%.2f", lat, lng);
            String nom = COORDS_VERS_NOM.get(cle);
            if (nom != null) return nom;

            // Recherche approchée : on cherche le point connu le plus proche (< 5 km)
            double distMin = Double.MAX_VALUE;
            String nomMin  = null;
            for (java.util.Map.Entry<String, String> entry : COORDS_VERS_NOM.entrySet()) {
                String[] kp = entry.getKey().split(",");
                double klat = Double.parseDouble(kp[0]);
                double klng = Double.parseDouble(kp[1]);
                double d = haversineStatic(lat, lng, klat, klng);
                if (d < distMin) { distMin = d; nomMin = entry.getValue(); }
            }
            if (distMin < 5.0 && nomMin != null) return nomMin;

            // Fallback : afficher les coords de façon lisible
            return String.format("Zone %.4f°N, %.4f°E", lat, lng);

        } catch (NumberFormatException e) {
            return localisation; // pas des coords → retourner tel quel
        }
    }

    /**
     * Dictionnaire GPS → Nom de lieu (Tunisie).
     * Format clé : "lat,lng" arrondi à 2 décimales.
     * Ajoutez librement de nouvelles entrées selon vos zones de test.
     */
    private static final java.util.Map<String, String> COORDS_VERS_NOM = new java.util.LinkedHashMap<>() {{
        // ── Bizerte & région ──
        put("37.27,9.87",  "Bizerte Centre");
        put("37.26,9.87",  "Bizerte");
        put("37.06,9.66",  "Mateur, Bizerte");
        put("37.05,9.66",  "Mateur, Bizerte");
        put("37.07,9.66",  "Mateur, Bizerte");
        put("37.14,9.97",  "Menzel Bourguiba, Bizerte");
        put("37.16,9.80",  "Sejnane, Bizerte");
        put("37.22,9.94",  "Zarzouna, Bizerte");

        // ── Tunis & Grand Tunis ──
        put("36.82,10.18", "Tunis Centre");
        put("36.81,10.18", "Tunis");
        put("36.89,10.19", "Ariana");
        put("36.84,10.23", "Ben Arous");
        put("36.75,10.23", "Rades, Ben Arous");
        put("36.80,10.12", "Manouba");
        put("36.87,10.32", "Hammam Lif");

        // ── Nabeul ──
        put("36.45,10.73", "Nabeul");
        put("36.40,10.61", "Hammamet, Nabeul");
        put("36.46,10.72", "Nabeul Centre");

        // ── Sousse ──
        put("35.83,10.64", "Sousse");
        put("35.73,10.68", "Monastir");

        // ── Sfax ──
        put("34.74,10.76", "Sfax");
        put("34.73,10.77", "Sfax Centre");

        // ── Béja ──
        put("36.73,9.18",  "Beja");
        put("36.46,8.78",  "Jendouba");

        // ── Kef ──
        put("36.18,8.71",  "Le Kef");

        // ── Siliana ──
        put("36.09,9.37",  "Siliana");

        // ── Zaghouan ──
        put("36.40,10.14", "Zaghouan");

        // ── Kairouan ──
        put("35.67,10.10", "Kairouan");

        // ── Kasserine ──
        put("35.17,8.83",  "Kasserine");

        // ── Sidi Bouzid ──
        put("35.03,9.48",  "Sidi Bouzid");

        // ── Gabes ──
        put("33.88,10.10", "Gabes");

        // ── Medenine ──
        put("33.35,10.50", "Medenine");

        // ── Tatouine ──
        put("32.92,10.45", "Tataouine");

        // ── Gafsa ──
        put("34.42,8.78",  "Gafsa");

        // ── Tozeur ──
        put("33.92,8.13",  "Tozeur");

        // ── Kebili ──
        put("33.70,8.97",  "Kebili");

        // ── Mahdia ──
        put("35.50,11.06", "Mahdia");

        // ── Ariana (détail) ──
        put("36.86,10.16", "La Marsa, Tunis");
        put("36.83,10.29", "Hammam Chott");
    }};

    /** Haversine statique pour le dictionnaire (sans instance) */
    private static double haversineStatic(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2)
                + Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon/2)*Math.sin(dLon/2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    }

    /**
     * Affectation manuelle : affecte un pompier spécifique à une alerte donnée
     * et lui envoie un email.
     */
    public ResultatAffectation affecterManuellement(int idAlerte, int idPompier, String localisationIncendie) {
        List<Pompier> disponibles = getPompiersDisponibles();
        Pompier cible = null;
        for (Pompier p : disponibles) {
            if (p.getId() == idPompier) { cible = p; break; }
        }
        if (cible == null) return null; // pompier non disponible
        double dist  = calculerDistancePompier(cible, localisationIncendie);
        double score = calculerScore(cible, dist);
        insererAffectation(idAlerte, idPompier, score, dist);
        passerEnMission(idPompier);
        return new ResultatAffectation(cible, score, dist, construireRaison(cible, dist, score));
    }

    /** Retourne les pompiers disponibles pour le dialogue d'affectation manuelle */
    public List<Pompier> getPompiersDisponiblesPourAffectation() {
        return getPompiersDisponibles();
    }

    public void terminerAffectation(int idAffectation, int idPompier) {
        try {
            PreparedStatement ps1 = getConnection().prepareStatement("UPDATE affectation SET statut='termine' WHERE id=?");
            ps1.setInt(1, idAffectation); ps1.executeUpdate();
            PreparedStatement ps2 = getConnection().prepareStatement("UPDATE pompier SET statut='disponible' WHERE id=?");
            ps2.setInt(1, idPompier); ps2.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur terminerAffectation : " + e.getMessage());
        }
    }

    /**
     * Supprime une affectation et bloque définitivement le watcher pour cette alerte.
     *
     * Stratégie anti-réaffectation :
     *   1. Récupère l'id_alerte depuis la ligne à supprimer
     *   2. Supprime la ligne d'affectation
     *   3. Insère une ligne "fantôme" statut='annule' pour cette alerte
     *      → getAlertesNonAffectees() l'exclura désormais via le SQL
     *   4. Ajoute l'id_alerte dans alertesAnnulees (protection mémoire immédiate)
     *   5. Remet le pompier disponible si plus de missions actives
     */
    /**
     * Retourne les interventions liées à une alerte donnée.
     * Jointure sur alerte_id ou alerte_localisation si alerte_id est null.
     * Retourne String[] { statut, agent_name, start_date, end_date, resultat, alert_zone }
     */
    public List<String[]> getInterventionsPourAlerte(int idAlerte, String localisation) {
        List<String[]> list = new ArrayList<>();
        // Cherche par alerte_id en priorité, puis par localisation (compatibilité)
        String sql = "SELECT statut, agent_name, start_date, end_date, resultat, alert_zone " +
                "FROM intervention " +
                "WHERE alerte_id = ? OR alerte_localisation = ? " +
                "ORDER BY start_date DESC";
        try {
            PreparedStatement ps = getConnection().prepareStatement(sql);
            ps.setInt(1, idAlerte);
            ps.setString(2, localisation != null ? localisation : "");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new String[]{
                        rs.getString("statut"),      // [0]
                        rs.getString("agent_name"),  // [1]
                        rs.getString("start_date") != null ? rs.getString("start_date").substring(0, Math.min(16, rs.getString("start_date").length())) : "-", // [2]
                        rs.getString("end_date")   != null ? rs.getString("end_date").substring(0, Math.min(16, rs.getString("end_date").length()))   : "En cours", // [3]
                        rs.getString("resultat")   != null ? rs.getString("resultat")   : "",     // [4]
                        rs.getString("alert_zone") != null ? rs.getString("alert_zone") : "-"     // [5]
                });
            }
        } catch (SQLException e) {
            // Table intervention peut ne pas exister encore
            System.out.println("[Service] Table intervention non disponible : " + e.getMessage());
        }
        return list;
    }

    public void supprimerAffectation(int idAffectation, int idPompier) {
        try {
            // Récupérer l'id_alerte avant de supprimer
            PreparedStatement psGet = getConnection().prepareStatement(
                    "SELECT id_alerte FROM affectation WHERE id=?");
            psGet.setInt(1, idAffectation);
            ResultSet rsGet = psGet.executeQuery();
            int idAlerte = rsGet.next() ? rsGet.getInt("id_alerte") : -1;

            // Supprimer l'affectation
            PreparedStatement ps1 = getConnection().prepareStatement("DELETE FROM affectation WHERE id=?");
            ps1.setInt(1, idAffectation);
            ps1.executeUpdate();

            // Insérer une ligne "annule" pour bloquer le watcher (protection base de données)
            if (idAlerte > 0) {
                PreparedStatement psBlock = getConnection().prepareStatement(
                        "INSERT INTO affectation (id_alerte, id_pompier, statut, mode_affectation) " +
                                "VALUES (?, 0, 'annule', 'annule')");
                psBlock.setInt(1, idAlerte);
                psBlock.executeUpdate();
                // Protection mémoire immédiate (avant le prochain cycle SQL)
                alertesAnnulees.add(idAlerte);
                alertesEnCours.remove(idAlerte);
                System.out.println("[Service] Alerte #" + idAlerte + " bloquée — watcher ne la re-traitera plus.");
            }

            // Remettre disponible si plus de missions actives
            PreparedStatement psCheck = getConnection().prepareStatement(
                    "SELECT COUNT(*) FROM affectation WHERE id_pompier=? AND statut='en_cours'");
            psCheck.setInt(1, idPompier);
            ResultSet rs = psCheck.executeQuery();
            if (rs.next() && rs.getInt(1) == 0) {
                PreparedStatement ps2 = getConnection().prepareStatement(
                        "UPDATE pompier SET statut='disponible' WHERE id=?");
                ps2.setInt(1, idPompier);
                ps2.executeUpdate();
            }
            System.out.println("[Service] Affectation #" + idAffectation + " supprimée, pompier #" + idPompier + " libéré.");
        } catch (SQLException e) {
            System.out.println("Erreur supprimerAffectation : " + e.getMessage());
        }
    }
}