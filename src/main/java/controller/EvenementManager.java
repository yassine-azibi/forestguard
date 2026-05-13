package controller;

import model.Foret;
import utils.ForetService;
import utils.MyConnection;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

public class EvenementManager {

    // ─── Modèle ──────────────────────────────────────────────────────────────

    public static class Evenement {
        public int    id;
        public String titre, foretNom, date, statut, description;
        public int    foretId, maxParticipants, participants;
        public final List<Avis>        avisList        = new ArrayList<>();
        public final List<Participant> participantList = new ArrayList<>();

        public double moyenneEtoiles() {
            if (avisList.isEmpty()) return 0;
            return avisList.stream().mapToInt(a -> a.etoiles).average().orElse(0);
        }
        public long nbSatisfaits() { return avisList.stream().filter(a -> a.etoiles >= 4).count(); }
        public int tauxSatisfaction() {
            if (avisList.isEmpty()) return 0;
            return (int) Math.round(nbSatisfaits() * 100.0 / avisList.size());
        }
    }

    public static class Avis {
        public int id; public String auteur, email, commentaire; public int etoiles; public String date;
        public Avis(int id, String auteur, String email, int etoiles, String commentaire, String date) {
            this.id=id; this.auteur=auteur; this.email=email;
            this.etoiles=etoiles; this.commentaire=commentaire; this.date=date;
        }
    }

    public static class Participant {
        public int id, nbPersonnes; public String nom, email, date;
        public Participant(int id, String nom, String email, int nb, String date) {
            this.id=id; this.nom=nom; this.email=email; this.nbPersonnes=nb; this.date=date;
        }
    }

    // ─── Services ────────────────────────────────────────────────────────────
    private final ForetService foretService = new ForetService();

    // ════════════════════════════════════════════
    //  POINT D'ENTRÉE
    // ════════════════════════════════════════════

    public void ouvrir(Stage owner) {
        initialiserTables();
        Stage stage = new Stage();
        stage.setTitle("Gestion des Événements — ForestGuard");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(owner);
        stage.setScene(new Scene((javafx.scene.Parent) construireInterface(stage), 820, 700));
        stage.setResizable(true);
        stage.show();
    }

    // ════════════════════════════════════════════
    //  INIT TABLES BDD
    // ════════════════════════════════════════════

    private void initialiserTables() {
        Connection cnx = MyConnection.getInstance().getCnx();
        if (cnx == null) { System.err.println("Init tables: pas de connexion MySQL."); return; }
        try (Statement st = cnx.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS evenement (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "titre VARCHAR(200) NOT NULL," +
                "foret_id INT," +
                "foret_nom VARCHAR(200)," +
                "date_evenement VARCHAR(20)," +
                "max_participants INT DEFAULT 50," +
                "participants INT DEFAULT 0," +
                "statut VARCHAR(20) DEFAULT 'OUVERT'," +
                "description TEXT)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS participant_evenement (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "evenement_id INT NOT NULL," +
                "nom VARCHAR(200)," +
                "email VARCHAR(200)," +
                "nb_personnes INT DEFAULT 1," +
                "date_inscription TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS avis_evenement (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "evenement_id INT NOT NULL," +
                "auteur VARCHAR(200)," +
                "email VARCHAR(200)," +
                "etoiles INT," +
                "commentaire TEXT," +
                "date_avis TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        } catch (Exception e) { System.err.println("Init tables: " + e.getMessage()); }
    }

    // ════════════════════════════════════════════
    //  CRUD BDD
    // ════════════════════════════════════════════

    private List<Evenement> chargerEvenements() {
        List<Evenement> list = new ArrayList<>();
        Connection cnx = MyConnection.getInstance().getCnx();
        if (cnx == null) { System.err.println("chargerEvenements: pas de connexion MySQL."); return list; }
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM evenement ORDER BY id DESC")) {
            while (rs.next()) {
                Evenement ev = new Evenement();
                ev.id              = rs.getInt("id");
                ev.titre           = rs.getString("titre");
                ev.foretId         = rs.getInt("foret_id");
                ev.foretNom        = rs.getString("foret_nom");
                ev.date            = rs.getString("date_evenement");
                ev.maxParticipants = rs.getInt("max_participants");
                ev.participants    = rs.getInt("participants");
                ev.statut          = rs.getString("statut");
                ev.description     = rs.getString("description");
                chargerAvis(cnx, ev);
                chargerParticipants(cnx, ev);
                list.add(ev);
            }
        } catch (Exception e) { System.err.println("Chargement événements: " + e.getMessage()); }
        return list;
    }

    private void chargerAvis(Connection cnx, Evenement ev) {
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT * FROM avis_evenement WHERE evenement_id=? ORDER BY date_avis DESC")) {
            ps.setInt(1, ev.id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ev.avisList.add(new Avis(
                    rs.getInt("id"), rs.getString("auteur"), rs.getString("email"),
                    rs.getInt("etoiles"), rs.getString("commentaire"),
                    rs.getString("date_avis")));
            }
        } catch (Exception e) { System.err.println("Avis: " + e.getMessage()); }
    }

    private void chargerParticipants(Connection cnx, Evenement ev) {
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT * FROM participant_evenement WHERE evenement_id=? ORDER BY date_inscription DESC")) {
            ps.setInt(1, ev.id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ev.participantList.add(new Participant(
                    rs.getInt("id"), rs.getString("nom"), rs.getString("email"),
                    rs.getInt("nb_personnes"), rs.getString("date_inscription")));
            }
        } catch (Exception e) { System.err.println("Participants: " + e.getMessage()); }
    }

    private int creerEvenement(Evenement ev) {
        Connection cnx = MyConnection.getInstance().getCnx();
        if (cnx == null) { System.err.println("creerEvenement: pas de connexion MySQL."); return -1; }
        try (PreparedStatement ps = cnx.prepareStatement(
                "INSERT INTO evenement (titre,foret_id,foret_nom,date_evenement,max_participants,statut,description) VALUES (?,?,?,?,?,'OUVERT',?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, ev.titre); ps.setInt(2, ev.foretId);
            ps.setString(3, ev.foretNom); ps.setString(4, ev.date);
            ps.setInt(5, ev.maxParticipants); ps.setString(6, ev.description);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) { System.err.println("Créer événement: " + e.getMessage()); }
        return -1;
    }

    private void inscrireParticipant(int evId, String nom, String email, int nb) {
        Connection cnx = MyConnection.getInstance().getCnx();
        if (cnx == null) { System.err.println("inscrireParticipant: pas de connexion MySQL."); return; }
        try (PreparedStatement ps = cnx.prepareStatement(
                "INSERT INTO participant_evenement (evenement_id,nom,email,nb_personnes) VALUES (?,?,?,?)")) {
            ps.setInt(1, evId); ps.setString(2, nom);
            ps.setString(3, email); ps.setInt(4, nb);
            ps.executeUpdate();
            // Mettre à jour le compteur
            try (PreparedStatement upd = cnx.prepareStatement(
                    "UPDATE evenement SET participants=participants+? WHERE id=?")) {
                upd.setInt(1, nb); upd.setInt(2, evId); upd.executeUpdate();
            }
            // Clôturer si plein
            try (PreparedStatement chk = cnx.prepareStatement(
                    "UPDATE evenement SET statut='CLOTURE' WHERE id=? AND participants>=max_participants")) {
                chk.setInt(1, evId); chk.executeUpdate();
            }
        } catch (Exception e) { System.err.println("Inscription: " + e.getMessage()); }
    }

    private void ajouterAvis(int evId, String auteur, String email, int etoiles, String commentaire) {
        Connection cnx = MyConnection.getInstance().getCnx();
        if (cnx == null) { System.err.println("ajouterAvis: pas de connexion MySQL."); return; }
        try (PreparedStatement ps = cnx.prepareStatement(
                "INSERT INTO avis_evenement (evenement_id,auteur,email,etoiles,commentaire) VALUES (?,?,?,?,?)")) {
            ps.setInt(1, evId); ps.setString(2, auteur); ps.setString(3, email);
            ps.setInt(4, etoiles); ps.setString(5, commentaire);
            ps.executeUpdate();
        } catch (Exception e) { System.err.println("Avis: " + e.getMessage()); }
    }

    // ════════════════════════════════════════════
    //  EMAIL — envoi aux utilisateurs
    // ════════════════════════════════════════════

    private List<String[]> getUtilisateurs() {
        List<String[]> users = new ArrayList<>();
        Connection cnx = MyConnection.getInstance().getCnx();
        if (cnx == null) { System.err.println("getUtilisateurs: pas de connexion MySQL."); return users; }
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery("SELECT nom, email FROM utilisateur WHERE email IS NOT NULL AND email != ''")) {
            while (rs.next()) {
                users.add(new String[]{rs.getString("nom"), rs.getString("email")});
            }
        } catch (Exception e) { System.err.println("Utilisateurs: " + e.getMessage()); }
        return users;
    }

    private List<String> getEmailsUtilisateurs() {
        List<String> emails = new ArrayList<>();
        getUtilisateurs().forEach(u -> emails.add(u[1]));
        return emails;
    }

    private void envoyerEmailsEvenement(Evenement ev) {
        new Thread(() -> {
            List<String[]> users = getUtilisateurs();
            if (users.isEmpty()) {
                System.out.println("[Email] Aucun utilisateur trouvé dans la table 'utilisateur'.");
                return;
            }
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");
            // ⚠️ Remplacez par votre email Gmail et mot de passe d'application
            String from     = "forestg106@gmail.com";
            String password = "blxqmarzetlbqukj";
            Session session = Session.getInstance(props, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(from, password);
                }
            });
            int sent = 0;
            for (String[] user : users) {
                String nomUser  = user[0] != null ? user[0] : "Utilisateur";
                String toEmail  = user[1];
                try {
                    String sujet = "\uD83C\uDF32 Nouvel événement ForestGuard : " + ev.titre;
                    String corps = "<html><body style='font-family:Arial;background:#f0fdf4;padding:20px'>" +
                        "<div style='max-width:600px;margin:auto;background:white;border-radius:12px;padding:30px;border:1px solid #bbf7d0'>" +
                        "<div style='background:#16a34a;border-radius:8px 8px 0 0;padding:16px 24px;margin:-30px -30px 24px'>" +
                        "<h2 style='color:white;margin:0'>\uD83C\uDF32 ForestGuard</h2>" +
                        "<p style='color:rgba(255,255,255,0.8);margin:4px 0 0'>Gestion des Forêts</p></div>" +
                        "<p style='color:#374151'>Bonjour <b>" + nomUser + "</b>,</p>" +
                        "<p style='color:#374151;margin:12px 0'>Un nouvel événement forestier est disponible :</p>" +
                        "<div style='background:#f0fdf4;border-radius:10px;padding:16px;border-left:4px solid #16a34a;margin:16px 0'>" +
                        "<h3 style='color:#15803d;margin:0 0 12px'>" + ev.titre + "</h3>" +
                        "<table style='width:100%;border-collapse:collapse'>" +
                        "<tr><td style='padding:6px 0;color:#6b7280;width:120px'>\uD83D\uDCC5 Date</td><td style='padding:6px 0;font-weight:bold;color:#1f2937'>" + ev.date + "</td></tr>" +
                        "<tr><td style='padding:6px 0;color:#6b7280'>\uD83C\uDF32 Forêt</td><td style='padding:6px 0;font-weight:bold;color:#1f2937'>" + ev.foretNom + "</td></tr>" +
                        "<tr><td style='padding:6px 0;color:#6b7280'>\uD83D\uDC65 Places</td><td style='padding:6px 0;font-weight:bold;color:#1f2937'>" + ev.maxParticipants + " places disponibles</td></tr>" +
                        "</table>" +
                        (ev.description != null && !ev.description.isEmpty() ? "<p style='color:#374151;margin-top:12px'>" + ev.description + "</p>" : "") +
                        "</div>" +
                        "<p style='color:#374151;margin-top:16px'>Ouvrez l'application <b>ForestGuard</b> pour vous inscrire et laisser votre avis.</p>" +
                        "<hr style='border:none;border-top:1px solid #e5e7eb;margin:20px 0'/>" +
                        "<p style='color:#9ca3af;font-size:12px'>Vous recevez cet email car vous êtes inscrit sur ForestGuard.</p>" +
                        "</div></body></html>";
                    Message msg = new MimeMessage(session);
                    msg.setFrom(new InternetAddress(from, "ForestGuard"));
                    msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
                    msg.setSubject(sujet);
                    msg.setContent(corps, "text/html; charset=utf-8");
                    Transport.send(msg);
                    sent++;
                    System.out.println("[Email] Envoyé à " + nomUser + " <" + toEmail + ">");
                } catch (Exception e) {
                    System.err.println("[Email] Erreur vers " + toEmail + ": " + e.getMessage());
                }
            }
            System.out.println("[Email] " + sent + "/" + users.size() + " emails envoyés.");
        }).start();
    }

    // ════════════════════════════════════════════
    //  INTERFACE PRINCIPALE
    // ════════════════════════════════════════════

    private javafx.scene.Parent construireInterface(Stage stage) {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: transparent;");

        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #0a150a; -fx-padding: 18 24 14 24;" +
                "-fx-border-color: rgba(22,163,74,0.25); -fx-border-width: 0 0 1 0;");
        Label lblTitre = new Label("\uD83D\uDDD3  Événements Forestiers");
        lblTitre.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: white;");
        HBox.setHgrow(lblTitre, Priority.ALWAYS);
        Button btnNouvel = creerBouton("+ Nouvel événement", "#16a34a", "#15803d");
        Button btnFermer = creerBouton("\u2715 Fermer", "#374151", "#4b5563");
        btnFermer.setOnAction(e -> stage.close());
        header.getChildren().addAll(lblTitre, btnNouvel, btnFermer);

        VBox[] statsBarRef = {new VBox()};
        VBox listeBox = new VBox(8);
        listeBox.setStyle("-fx-padding: 16 20; -fx-background-color: transparent;");
        ScrollPane scroll = new ScrollPane(listeBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        Runnable[] refreshRef = {null};
        Runnable refresh = () -> {
            List<Evenement> evs = chargerEvenements();
            listeBox.getChildren().clear();
            for (Evenement ev : evs)
                listeBox.getChildren().add(construireCarteEvenement(ev, stage, refreshRef[0]));
            HBox statsBar = construireStatsBar(evs);
            if (root.getChildren().size() > 1) root.getChildren().set(1, statsBar);
        };
        refreshRef[0] = refresh;

        btnNouvel.setOnAction(e -> ouvrirModalCreer(stage, refresh));

        List<Evenement> evs = chargerEvenements();
        HBox statsBar = construireStatsBar(evs);
        root.getChildren().addAll(header, statsBar, scroll);
        for (Evenement ev : evs)
            listeBox.getChildren().add(construireCarteEvenement(ev, stage, refresh));

        // ── Fond : image forêt + overlay sombre ──────────────────────────
        javafx.scene.image.ImageView bgImg = new javafx.scene.image.ImageView();
        try {
            java.net.URL bgUrl = getClass().getResource("/foret_bg.jpg");
            if (bgUrl != null) bgImg.setImage(new javafx.scene.image.Image(bgUrl.toExternalForm()));
        } catch (Exception ignored) {}
        bgImg.setPreserveRatio(false);
        bgImg.setFitWidth(820); bgImg.setFitHeight(700);
        bgImg.setOpacity(0.30);

        javafx.scene.layout.Region overlay = new javafx.scene.layout.Region();
        overlay.setStyle("-fx-background-color: rgba(5,20,8,0.72);");

        VBox.setVgrow(root, Priority.ALWAYS);
        StackPane wrapper = new StackPane(bgImg, overlay, root);
        wrapper.setStyle("-fx-background-color: #0a150a;");

        // Faire en sorte que l'image suive la taille de la fenêtre
        wrapper.widthProperty().addListener((obs, o, n) -> bgImg.setFitWidth(n.doubleValue()));
        wrapper.heightProperty().addListener((obs, o, n) -> bgImg.setFitHeight(n.doubleValue()));

        return wrapper;
    }

    private HBox construireStatsBar(List<Evenement> evs) {
        int totalEvt   = evs.size();
        int totalPart  = evs.stream().mapToInt(e -> e.participants).sum();
        long totalAvis = evs.stream().mapToLong(e -> e.avisList.size()).sum();
        long totalSat  = evs.stream().mapToLong(Evenement::nbSatisfaits).sum();
        int pctSat     = totalAvis > 0 ? (int) Math.round(totalSat * 100.0 / totalAvis) : 0;
        HBox bar = new HBox(12);
        bar.setStyle("-fx-background-color: rgba(0,0,0,0.35); -fx-padding: 12 20;");
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getChildren().addAll(
            creerStatCard("\uD83D\uDDD3", "Événements", String.valueOf(totalEvt), "#4ade80"),
            creerStatCard("\uD83D\uDC65", "Participants", String.valueOf(totalPart), "#60a5fa"),
            creerStatCard("\uD83D\uDCAC", "Avis reçus", String.valueOf(totalAvis), "#f59e0b"),
            creerStatCard("\u2705", "Satisfaction", pctSat + "%", "#a78bfa"));
        return bar;
    }

    private VBox creerStatCard(String icone, String label, String valeur, String couleur) {
        Label lblIcon  = new Label(icone + " " + valeur);
        lblIcon.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: " + couleur + ";");
        Label lblLabel = new Label(label);
        lblLabel.setStyle("-fx-font-size: 10; -fx-text-fill: rgba(255,255,255,0.4);");
        VBox card = new VBox(2, lblIcon, lblLabel);
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 10;" +
                "-fx-padding: 10 20; -fx-border-color: rgba(255,255,255,0.07); -fx-border-radius: 10;");
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    // ════════════════════════════════════════════
    //  CARTE ÉVÉNEMENT
    // ════════════════════════════════════════════

    private VBox construireCarteEvenement(Evenement ev, Stage owner, Runnable refresh) {
        Circle circle = new Circle(22);
        circle.setFill(Color.web(ev.statut.equals("OUVERT") ? "#16a34a" : "#6b7280"));
        Label avatarLbl = new Label("EV");
        avatarLbl.setStyle("-fx-font-size: 11; -fx-font-weight: bold; -fx-text-fill: white;");
        StackPane avatar = new StackPane(circle, avatarLbl);
        avatar.setMinSize(44, 44); avatar.setMaxSize(44, 44);

        Label lblTitre   = new Label(ev.titre);
        lblTitre.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #e2e8f0;");
        Label lblForet   = new Label("\uD83C\uDF32 " + ev.foretNom);
        lblForet.setStyle("-fx-font-size: 11; -fx-text-fill: #4ade80;");
        Label lblDate    = new Label("\uD83D\uDCC5 " + ev.date);
        lblDate.setStyle("-fx-font-size: 11; -fx-text-fill: #94a3b8;");
        Label lblPart    = new Label("\uD83D\uDC65 " + ev.participants + "/" + ev.maxParticipants);
        lblPart.setStyle("-fx-font-size: 11; -fx-text-fill: #94a3b8;");
        String etoiles   = ev.avisList.isEmpty() ? "Aucun avis"
                : String.format("%.1f \u2605 (%d avis)", ev.moyenneEtoiles(), ev.avisList.size());
        Label lblEtoiles = new Label("\u2B50 " + etoiles);
        lblEtoiles.setStyle("-fx-font-size: 11; -fx-text-fill: #fbbf24;");
        VBox infos = new VBox(5, lblTitre, new HBox(16, lblForet, lblDate, lblPart), new HBox(16, lblEtoiles));
        infos.setAlignment(Pos.CENTER_LEFT); HBox.setHgrow(infos, Priority.ALWAYS);

        Label badge = new Label(ev.statut);
        badge.setStyle("-fx-background-color: " + (ev.statut.equals("OUVERT") ? "#14532d" : "#374151") + ";" +
                "-fx-text-fill: " + (ev.statut.equals("OUVERT") ? "#4ade80" : "#9ca3af") + ";" +
                "-fx-font-size: 10; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 4 12;");

        int pct = ev.maxParticipants > 0 ? (int) Math.round(ev.participants * 100.0 / ev.maxParticipants) : 0;
        Canvas barCanvas = new Canvas(120, 6);
        GraphicsContext gc = barCanvas.getGraphicsContext2D();
        gc.setFill(Color.web("#374151")); gc.fillRoundRect(0,0,120,6,3,3);
        gc.setFill(Color.web(pct >= 90 ? "#f87171" : pct >= 60 ? "#fbbf24" : "#4ade80"));
        gc.fillRoundRect(0, 0, pct * 1.2, 6, 3, 3);
        Label lblPct = new Label(pct + "%");
        lblPct.setStyle("-fx-font-size: 10; -fx-text-fill: #6b7280;");
        VBox progBox = new VBox(2, barCanvas, lblPct); progBox.setAlignment(Pos.CENTER);

        Button btnVoir = creerBoutonSmall("\uD83D\uDC41 Voir", "#1e3a5f", "#2563eb");
        btnVoir.setOnAction(e -> ouvrirModalVoir(ev, owner, refresh));

        HBox actions = new HBox(6, btnVoir);
        actions.setAlignment(Pos.CENTER);

        HBox row = new HBox(12, avatar, infos, badge, progBox, actions);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 18, 14, 18));
        String styleN = "-fx-background-color:rgba(255,255,255,0.05);-fx-background-radius:14;-fx-border-color:rgba(255,255,255,0.08);-fx-border-radius:14;-fx-border-width:1;";
        row.setStyle(styleN);
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color:rgba(22,163,74,0.08);-fx-background-radius:14;-fx-border-color:rgba(22,163,74,0.3);-fx-border-radius:14;-fx-border-width:1;"));
        row.setOnMouseExited(e  -> row.setStyle(styleN));

        VBox wrapper = new VBox(row); wrapper.setStyle("-fx-padding: 2 0;");
        return wrapper;
    }

    // ════════════════════════════════════════════
    //  MODAL — VOIR (participants + avis)
    // ════════════════════════════════════════════

    private void ouvrirModalVoir(Evenement ev, Stage owner, Runnable refresh) {
        Stage modal = new Stage();
        modal.setTitle("Événement : " + ev.titre);
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.initOwner(owner);

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #0f1f0f;");
        root.getChildren().add(creerHeaderModal("\uD83D\uDCCB  " + ev.titre));

        // Onglets : Détails | Participants | Avis
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setStyle("-fx-background-color: #0f1f0f;");

        // ── Onglet Détails ──────────────────────────────────────────────────
        VBox detailsBody = new VBox(14); detailsBody.setStyle("-fx-padding: 20;");
        detailsBody.getChildren().addAll(
            creerSectionTitre("Informations"), creerInfoGrid(ev),
            creerSectionTitre("Statistiques"), creerStatsSatisfaction(ev),
            creerSectionTitre("Distribution des notes"), creerDistributionEtoiles(ev));
        ScrollPane scrollDetails = new ScrollPane(detailsBody);
        scrollDetails.setFitToWidth(true);
        scrollDetails.setStyle("-fx-background:#0f1f0f;-fx-background-color:#0f1f0f;");
        Tab tabDetails = new Tab("📋 Détails", scrollDetails);

        // ── Onglet Participants ─────────────────────────────────────────────
        VBox partBody = new VBox(10); partBody.setStyle("-fx-padding: 20;");
        Label lblNbPart = new Label("\uD83D\uDC65 " + ev.participantList.size() + " participant(s) inscrit(s)");
        lblNbPart.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #60a5fa;");
        partBody.getChildren().add(lblNbPart);
        if (ev.participantList.isEmpty()) {
            Label vide = new Label("Aucun participant inscrit.");
            vide.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 13;");
            partBody.getChildren().add(vide);
        } else {
            // En-tête tableau
            HBox entete = new HBox(0);
            entete.setStyle("-fx-background-color:rgba(255,255,255,0.08);-fx-padding:8 12;-fx-background-radius:8 8 0 0;");
            for (String[] col : new String[][]{{"Nom","200"},{"Email","220"},{"Nb","60"},{"Date","160"}}) {
                Label l = new Label(col[0]); l.setStyle("-fx-font-size:11;-fx-font-weight:bold;-fx-text-fill:rgba(255,255,255,0.5);");
                l.setPrefWidth(Double.parseDouble(col[1])); entete.getChildren().add(l);
            }
            partBody.getChildren().add(entete);
            for (Participant p : ev.participantList) {
                HBox row = new HBox(0);
                row.setStyle("-fx-background-color:rgba(255,255,255,0.03);-fx-padding:8 12;-fx-border-color:rgba(255,255,255,0.05);-fx-border-width:0 0 1 0;");
                Label lNom   = new Label(p.nom   != null ? p.nom   : ""); lNom.setPrefWidth(200);   lNom.setStyle("-fx-text-fill:#e2e8f0;-fx-font-size:12;");
                Label lEmail = new Label(p.email != null ? p.email : ""); lEmail.setPrefWidth(220); lEmail.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:12;");
                Label lNb    = new Label(String.valueOf(p.nbPersonnes)); lNb.setPrefWidth(60);      lNb.setStyle("-fx-text-fill:#4ade80;-fx-font-size:12;-fx-font-weight:bold;");
                Label lDate  = new Label(p.date  != null ? p.date.substring(0,Math.min(16,p.date.length())) : ""); lDate.setPrefWidth(160); lDate.setStyle("-fx-text-fill:#6b7280;-fx-font-size:11;");
                row.getChildren().addAll(lNom, lEmail, lNb, lDate);
                partBody.getChildren().add(row);
            }
        }
        ScrollPane scrollPart = new ScrollPane(partBody);
        scrollPart.setFitToWidth(true);
        scrollPart.setStyle("-fx-background:#0f1f0f;-fx-background-color:#0f1f0f;");
        Tab tabPart = new Tab("\uD83D\uDC65 Participants (" + ev.participantList.size() + ")", scrollPart);

        // ── Onglet Avis ─────────────────────────────────────────────────────
        VBox avisBody = new VBox(10); avisBody.setStyle("-fx-padding: 20;");
        Label lblNbAvis = new Label("\uD83D\uDCAC " + ev.avisList.size() + " avis reçus");
        lblNbAvis.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #f59e0b;");
        avisBody.getChildren().add(lblNbAvis);
        if (ev.avisList.isEmpty()) {
            Label vide = new Label("Aucun avis pour cet événement.");
            vide.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 13;");
            avisBody.getChildren().add(vide);
        } else {
            for (Avis a : ev.avisList) avisBody.getChildren().add(creerCarteAvis(a));
        }
        ScrollPane scrollAvis = new ScrollPane(avisBody);
        scrollAvis.setFitToWidth(true);
        scrollAvis.setStyle("-fx-background:#0f1f0f;-fx-background-color:#0f1f0f;");
        Tab tabAvis = new Tab("\u2B50 Avis (" + ev.avisList.size() + ")", scrollAvis);

        tabs.getTabs().addAll(tabDetails, tabPart, tabAvis);
        VBox.setVgrow(tabs, Priority.ALWAYS);

        Button btnFermer = creerBouton("\u2715 Fermer", "#374151", "#4b5563");
        btnFermer.setOnAction(e -> modal.close());
        HBox btnRow = new HBox(btnFermer); btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.setStyle("-fx-padding: 12 20;");

        root.getChildren().addAll(tabs, btnRow);
        modal.setScene(new Scene(root, 680, 640));
        modal.show();
    }

    // ════════════════════════════════════════════
    //  MODAL — CRÉER ÉVÉNEMENT
    // ════════════════════════════════════════════

    private void ouvrirModalCreer(Stage owner, Runnable refresh) {
        Stage modal = new Stage();
        modal.setTitle("Nouvel Événement");
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.initOwner(owner);

        List<Foret> forets = foretService.getData();
        VBox root = new VBox(0); root.setStyle("-fx-background-color: white;");
        root.getChildren().add(creerHeaderModal("\uD83D\uDDD3  Créer un nouvel événement"));

        VBox form = new VBox(12); form.setStyle("-fx-padding: 20; -fx-background-color: white;");
        TextField tfTitre = creerTextField("Titre de l'événement");
        TextArea  taDes   = creerTextArea("Description...", 3);
        TextField tfDate  = creerTextField("Date (AAAA-MM-JJ)");
        tfDate.setText(LocalDate.now().plusDays(7).toString());
        TextField tfMax   = creerTextField("Capacité maximale"); tfMax.setText("50");

        ComboBox<String> cbForet = new ComboBox<>();
        cbForet.setMaxWidth(Double.MAX_VALUE);
        cbForet.setStyle("-fx-background-color: #f0fdf4; -fx-text-fill: #1e293b; -fx-font-size: 13; -fx-background-radius: 8; -fx-border-color: #86efac; -fx-border-radius: 8;");
        for (Foret f : forets) cbForet.getItems().add(f.getNom());
        if (!forets.isEmpty()) cbForet.setValue(forets.get(0).getNom());

        // Option envoi email
        CheckBox cbEmail = new CheckBox("Envoyer un email de notification aux utilisateurs");
        cbEmail.setSelected(true);
        cbEmail.setStyle("-fx-text-fill: #15803d; -fx-font-size: 12;");

        form.getChildren().addAll(
            creerLabel("Titre"), tfTitre,
            creerLabel("Forêt concernée"), cbForet,
            creerLabel("Date"), tfDate,
            creerLabel("Capacité max"), tfMax,
            creerLabel("Description"), taDes,
            cbEmail);

        Button btnCreer   = creerBouton("\u2705 Créer l'événement", "#16a34a", "#15803d");
        Button btnAnnuler = creerBouton("Annuler", "#64748b", "#475569");
        HBox btnRow = new HBox(10, btnAnnuler, btnCreer);
        btnRow.setAlignment(Pos.CENTER_RIGHT); btnRow.setStyle("-fx-padding: 0 20 20 20; -fx-background-color: white;");

        btnAnnuler.setOnAction(e -> modal.close());
        btnCreer.setOnAction(e -> {
            if (tfTitre.getText().trim().isEmpty() || cbForet.getValue() == null) {
                afficherAlerte("Champs requis", "Veuillez remplir le titre et choisir une forêt."); return;
            }
            int idx = cbForet.getSelectionModel().getSelectedIndex();
            Foret f = forets.get(idx < 0 ? 0 : idx);
            int maxP = 50; try { maxP = Integer.parseInt(tfMax.getText().trim()); } catch (Exception ignored) {}

            Evenement ev = new Evenement();
            ev.foretId = f.getId(); ev.foretNom = f.getNom();
            ev.titre = tfTitre.getText().trim(); ev.date = tfDate.getText().trim();
            ev.maxParticipants = maxP; ev.description = taDes.getText().trim();

            int newId = creerEvenement(ev);
            if (newId > 0) {
                ev.id = newId;
                if (cbEmail.isSelected()) {
                    envoyerEmailsEvenement(ev);
                    afficherInfo("Événement créé", "L'événement a été créé.\nEmails de notification envoyés aux utilisateurs.");
                } else {
                    afficherInfo("Événement créé", "L'événement « " + ev.titre + " » a été créé.");
                }
                refresh.run();
                modal.close();
            } else {
                afficherAlerte("Erreur", "Impossible de créer l'événement en base de données.");
            }
        });

        root.getChildren().addAll(form, btnRow);
        modal.setScene(new Scene(root, 480, 560));
        modal.setResizable(false);
        modal.show();
    }

    // ════════════════════════════════════════════
    //  MODAL — INSCRIRE
    // ════════════════════════════════════════════

    private void ouvrirModalInscrire(Evenement ev, Stage owner, Runnable refresh) {
        Stage modal = new Stage();
        modal.setTitle("Inscription — " + ev.titre);
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.initOwner(owner);

        VBox root = new VBox(0); root.setStyle("-fx-background-color: #0f1f0f;");
        root.getChildren().add(creerHeaderModal("\uD83D\uDC65  Inscrire des participants"));

        VBox form = new VBox(12); form.setStyle("-fx-padding: 20;");
        int restants = ev.maxParticipants - ev.participants;
        Label lblInfo = new Label("\uD83C\uDF32 " + ev.foretNom + "  ·  \uD83D\uDCC5 " + ev.date + "  ·  " + restants + " place(s) restante(s)");
        lblInfo.setStyle("-fx-text-fill: #4ade80; -fx-font-size: 12; -fx-padding: 0 0 8 0;");

        TextField tfNom   = creerTextField("Prénom Nom");
        TextField tfEmail = creerTextField("email@exemple.com");
        TextField tfNb    = creerTextField("Nombre de personnes"); tfNb.setText("1");

        form.getChildren().addAll(lblInfo, creerLabel("Nom"), tfNom, creerLabel("Email"), tfEmail, creerLabel("Nombre"), tfNb);

        Button btnOk     = creerBouton("\u2705 Confirmer", "#16a34a", "#15803d");
        Button btnAnnuler = creerBouton("Annuler", "#374151", "#4b5563");
        HBox btnRow = new HBox(10, btnAnnuler, btnOk);
        btnRow.setAlignment(Pos.CENTER_RIGHT); btnRow.setStyle("-fx-padding: 0 20 20 20;");

        btnAnnuler.setOnAction(e -> modal.close());
        btnOk.setOnAction(e -> {
            int nb = 1; try { nb = Integer.parseInt(tfNb.getText().trim()); } catch (Exception ignored) {}
            if (nb <= 0) { afficherAlerte("Erreur", "Le nombre doit être > 0."); return; }
            if (ev.participants + nb > ev.maxParticipants) {
                afficherAlerte("Capacité dépassée", "Seulement " + restants + " place(s) disponible(s)."); return;
            }
            inscrireParticipant(ev.id, tfNom.getText().trim(), tfEmail.getText().trim(), nb);
            refresh.run();
            afficherInfo("Inscription confirmée", nb + " participant(s) inscrit(s) à « " + ev.titre + " ».");
            modal.close();
        });

        root.getChildren().addAll(form, btnRow);
        modal.setScene(new Scene(root, 420, 380));
        modal.setResizable(false);
        modal.show();
    }

    // ════════════════════════════════════════════
    //  MODAL — AVIS
    // ════════════════════════════════════════════

    private void ouvrirModalAvis(Evenement ev, Stage owner, Runnable refresh) {
        Stage modal = new Stage();
        modal.setTitle("Avis — " + ev.titre);
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.initOwner(owner);

        VBox root = new VBox(0); root.setStyle("-fx-background-color: #0f1f0f;");
        root.getChildren().add(creerHeaderModal("\u2B50  Laisser un avis"));

        VBox form = new VBox(12); form.setStyle("-fx-padding: 20;");
        TextField tfAuteur = creerTextField("Votre prénom et nom");
        TextField tfEmail  = creerTextField("email@exemple.com");
        TextArea  taComm   = creerTextArea("Partagez votre expérience...", 4);

        int[] noteSel = {0};
        HBox starBox = new HBox(8); starBox.setAlignment(Pos.CENTER_LEFT);
        Button[] etoilesBtns = new Button[5];
        for (int i = 0; i < 5; i++) {
            int idx = i + 1;
            Button btn = new Button("\u2606");
            btn.setStyle("-fx-background-color:transparent;-fx-text-fill:#6b7280;-fx-font-size:28;-fx-cursor:hand;-fx-padding:2 4;-fx-border-color:transparent;");
            btn.setOnAction(ev2 -> {
                noteSel[0] = idx;
                for (int j = 0; j < 5; j++) {
                    etoilesBtns[j].setText(j < idx ? "\u2605" : "\u2606");
                    etoilesBtns[j].setStyle("-fx-background-color:transparent;-fx-text-fill:" + (j < idx ? "#f59e0b" : "#6b7280") + ";-fx-font-size:28;-fx-cursor:hand;-fx-padding:2 4;-fx-border-color:transparent;");
                }
            });
            etoilesBtns[i] = btn; starBox.getChildren().add(btn);
        }

        form.getChildren().addAll(creerLabel("Votre nom"), tfAuteur, creerLabel("Email"), tfEmail,
            creerLabel("Note (1 à 5 étoiles)"), starBox, creerLabel("Commentaire"), taComm);

        Button btnEnvoyer = creerBouton("\uD83D\uDCE9 Envoyer mon avis", "#16a34a", "#15803d");
        Button btnAnnuler = creerBouton("Annuler", "#374151", "#4b5563");
        HBox btnRow = new HBox(10, btnAnnuler, btnEnvoyer);
        btnRow.setAlignment(Pos.CENTER_RIGHT); btnRow.setStyle("-fx-padding: 0 20 20 20;");

        btnAnnuler.setOnAction(e -> modal.close());
        btnEnvoyer.setOnAction(e -> {
            if (tfAuteur.getText().trim().isEmpty()) { afficherAlerte("Champ requis", "Veuillez entrer votre nom."); return; }
            if (noteSel[0] == 0) { afficherAlerte("Note manquante", "Veuillez sélectionner une note."); return; }
            ajouterAvis(ev.id, tfAuteur.getText().trim(), tfEmail.getText().trim(), noteSel[0],
                taComm.getText().trim().isEmpty() ? "(sans commentaire)" : taComm.getText().trim());
            refresh.run();
            afficherInfo("Avis enregistré", "Merci pour votre avis de " + noteSel[0] + " étoile(s) !");
            modal.close();
        });

        root.getChildren().addAll(form, btnRow);
        modal.setScene(new Scene(root, 420, 520));
        modal.setResizable(false);
        modal.show();
    }

    // ════════════════════════════════════════════
    //  HELPERS UI
    // ════════════════════════════════════════════

    private HBox creerHeaderModal(String titre) {
        Label lbl = new Label(titre); lbl.setStyle("-fx-font-size:15;-fx-font-weight:bold;-fx-text-fill:white;");
        HBox hb = new HBox(lbl); hb.setStyle("-fx-background-color:#16a34a;-fx-padding:16 20;-fx-border-color:#15803d;-fx-border-width:0 0 1 0;");
        return hb;
    }

    private Label creerSectionTitre(String texte) {
        Label l = new Label(texte.toUpperCase());
        l.setStyle("-fx-font-size:10;-fx-text-fill:rgba(255,255,255,0.3);-fx-letter-spacing:1;-fx-padding:8 0 2 0;");
        return l;
    }

    private HBox creerInfoGrid(Evenement ev) {
        HBox grid = new HBox(10); grid.setStyle("-fx-background-color:rgba(255,255,255,0.04);-fx-background-radius:10;-fx-padding:12 16;");
        String[][] infos = {{"\uD83C\uDF32","Forêt",ev.foretNom},{"\uD83D\uDCC5","Date",ev.date},{"\uD83D\uDC65","Participants",ev.participants+"/"+ev.maxParticipants},{"\uD83D\uDD16","Statut",ev.statut}};
        for (String[] info : infos) {
            VBox card = new VBox(3);
            Label lIcon = new Label(info[0]+" "+info[1]); lIcon.setStyle("-fx-font-size:10;-fx-text-fill:rgba(255,255,255,0.35);");
            Label lVal  = new Label(info[2]); lVal.setStyle("-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:#e2e8f0;");
            card.getChildren().addAll(lIcon, lVal); HBox.setHgrow(card, Priority.ALWAYS); grid.getChildren().add(card);
        }
        return grid;
    }

    private HBox creerStatsSatisfaction(Evenement ev) {
        HBox box = new HBox(10); box.setStyle("-fx-background-color:rgba(255,255,255,0.04);-fx-background-radius:10;-fx-padding:14 16;");
        String[][] stats = {{"\uD83D\uDCAC","Avis reçus",String.valueOf(ev.avisList.size()),"#60a5fa"},{"\u2705","Satisfaits",String.valueOf(ev.nbSatisfaits()),"#4ade80"},{"\uD83D\uDCCA","Satisfaction",ev.tauxSatisfaction()+"%","#a78bfa"},{"\u2B50","Moyenne",String.format("%.1f/5",ev.moyenneEtoiles()),"#f59e0b"}};
        for (String[] s : stats) {
            VBox card = new VBox(3); card.setAlignment(Pos.CENTER);
            card.setStyle("-fx-background-color:rgba(255,255,255,0.04);-fx-background-radius:8;-fx-padding:10 16;");
            Label lbl = new Label(s[0]+" "+s[2]); lbl.setStyle("-fx-font-size:16;-fx-font-weight:bold;-fx-text-fill:"+s[3]+";");
            Label lblL = new Label(s[1]); lblL.setStyle("-fx-font-size:10;-fx-text-fill:rgba(255,255,255,0.35);");
            card.getChildren().addAll(lbl, lblL); HBox.setHgrow(card, Priority.ALWAYS); box.getChildren().add(card);
        }
        return box;
    }

    private VBox creerDistributionEtoiles(Evenement ev) {
        VBox box = new VBox(6); box.setStyle("-fx-background-color:rgba(255,255,255,0.04);-fx-background-radius:10;-fx-padding:12 16;");
        int total = ev.avisList.size();
        for (int star = 5; star >= 1; star--) {
            final int s = star;
            long count = ev.avisList.stream().filter(a -> a.etoiles == s).count();
            int pct = total > 0 ? (int) Math.round(count * 100.0 / total) : 0;
            Label lblStar = new Label(star + " \u2605"); lblStar.setStyle("-fx-text-fill:#f59e0b;-fx-font-size:12;-fx-min-width:36;");
            Canvas bar = new Canvas(200, 8); GraphicsContext gc = bar.getGraphicsContext2D();
            gc.setFill(Color.web("#374151")); gc.fillRoundRect(0,0,200,8,4,4);
            gc.setFill(Color.web("#f59e0b")); gc.fillRoundRect(0,0,pct*2.0,8,4,4);
            Label lblCount = new Label(count+""); lblCount.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:11;-fx-min-width:20;");
            HBox row = new HBox(10, lblStar, bar, lblCount); row.setAlignment(Pos.CENTER_LEFT);
            box.getChildren().add(row);
        }
        return box;
    }

    private VBox creerCarteAvis(Avis a) {
        String etoilesStr = "\u2605".repeat(a.etoiles) + "\u2606".repeat(5 - a.etoiles);
        Label lblAuteur  = new Label(a.auteur != null ? a.auteur : "Anonyme"); lblAuteur.setStyle("-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:#e2e8f0;");
        Label lblEmail   = new Label(a.email  != null ? a.email  : ""); lblEmail.setStyle("-fx-font-size:10;-fx-text-fill:#6b7280;");
        Label lblEtoiles = new Label(etoilesStr); lblEtoiles.setStyle("-fx-text-fill:#f59e0b;-fx-font-size:14;");
        Label lblDate    = new Label(a.date   != null ? a.date.substring(0,Math.min(16,a.date.length())) : ""); lblDate.setStyle("-fx-font-size:10;-fx-text-fill:#4b5563;");
        HBox topRow = new HBox(10, new VBox(2,lblAuteur,lblEmail), lblEtoiles, lblDate);
        topRow.setAlignment(Pos.CENTER_LEFT); HBox.setHgrow(topRow.getChildren().get(0), Priority.ALWAYS);
        Label lblComm = new Label(a.commentaire); lblComm.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:12;-fx-wrap-text:true;"); lblComm.setMaxWidth(600);
        VBox card = new VBox(6, topRow, lblComm);
        card.setStyle("-fx-background-color:rgba(255,255,255,0.04);-fx-background-radius:10;-fx-padding:12 14;-fx-border-color:rgba(255,255,255,0.07);-fx-border-radius:10;");
        return card;
    }

    private Button creerBouton(String texte, String bgN, String bgH) {
        Button btn = new Button(texte);
        String s = "-fx-background-color:"+bgN+";-fx-text-fill:white;-fx-background-radius:8;-fx-cursor:hand;-fx-font-size:13;-fx-font-weight:bold;-fx-padding:8 16;";
        btn.setStyle(s); btn.setOnMouseEntered(e -> btn.setStyle(s.replace(bgN,bgH))); btn.setOnMouseExited(e -> btn.setStyle(s));
        return btn;
    }

    private Button creerBoutonSmall(String texte, String bgN, String bgH) {
        Button btn = new Button(texte);
        String s = "-fx-background-color:"+bgN+";-fx-text-fill:white;-fx-background-radius:7;-fx-cursor:hand;-fx-font-size:11;-fx-font-weight:bold;-fx-padding:5 10;";
        btn.setStyle(s); btn.setOnMouseEntered(e -> btn.setStyle(s.replace(bgN,bgH))); btn.setOnMouseExited(e -> btn.setStyle(s));
        return btn;
    }

    private TextField creerTextField(String placeholder) {
        TextField tf = new TextField(); tf.setPromptText(placeholder);
        tf.setStyle("-fx-background-color:#f0fdf4;-fx-text-fill:#1e293b;-fx-prompt-text-fill:#94a3b8;" +
                    "-fx-font-size:13;-fx-background-radius:8;-fx-padding:8 12;" +
                    "-fx-border-color:#86efac;-fx-border-radius:8;");
        return tf;
    }

    private TextArea creerTextArea(String placeholder, int rows) {
        TextArea ta = new TextArea(); ta.setPromptText(placeholder); ta.setPrefRowCount(rows); ta.setWrapText(true);
        ta.setStyle("-fx-background-color:#f0fdf4;-fx-text-fill:#1e293b;-fx-prompt-text-fill:#94a3b8;" +
                    "-fx-font-size:13;-fx-background-radius:8;" +
                    "-fx-border-color:#86efac;-fx-border-radius:8;");
        return ta;
    }

    private Label creerLabel(String texte) {
        Label l = new Label(texte);
        l.setStyle("-fx-text-fill:#16a34a;-fx-font-size:11;-fx-font-weight:bold;-fx-padding:4 0 1 0;");
        return l;
    }

    private void afficherAlerte(String titre, String msg) { Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK); a.setTitle(titre); a.setHeaderText(null); a.show(); }
    private void afficherInfo(String titre, String msg)   { Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK); a.setTitle(titre); a.setHeaderText(null); a.show(); }
}

