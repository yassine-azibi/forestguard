package controller;

import model.Foret;
import model.Incendie;
import utils.ForetService;
import utils.IncendieService;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.*;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

public class IncendieStats {

    // ── Services ──────────────────────────────────────────────────────────────
    private final IncendieService incendieService;
    private final ForetService    foretService;

    // ── Couleurs thème ────────────────────────────────────────────────────────
    private static final String BG_MAIN   = "#0a0f0a";
    private static final String BG_CARD   = "rgba(255,255,255,0.04)";
    private static final String COL_FIRE  = "#ff6b35";
    private static final String COL_GOLD  = "#ffd700";
    private static final String COL_GREEN = "#4ade80";
    private static final String COL_RED   = "#ef4444";
    private static final String COL_BLUE  = "#60a5fa";

    // ── Palette barres par mois ────────────────────────────────────────────────
    private static final Color[] BAR_COLORS = {
            Color.web("#ff6b35"), Color.web("#ff8c42"), Color.web("#ffa500"),
            Color.web("#ffcc02"), Color.web("#ff6b6b"), Color.web("#ee5a24"),
            Color.web("#fd9644"), Color.web("#e84393"), Color.web("#f97316"),
            Color.web("#dc2626"), Color.web("#ea580c"), Color.web("#c2410c")
    };

    // ── Données ───────────────────────────────────────────────────────────────
    private List<Incendie> tousIncendies;
    private List<Foret>    toutesForets;

    // ── UI refs ───────────────────────────────────────────────────────────────
    private Canvas     canvasBar;
    private Canvas     canvasPie;
    private VBox       topForetsList;
    private Label      lblTotalInc;
    private Label      lblSupTotale;
    private Label      lblMoisPic;
    private Label      lblForetTop;
    private ComboBox<String> comboAnnee;
    private ComboBox<String> comboPeriode;
    private DatePicker dpDebut;
    private DatePicker dpFin;

    // ── Filtre courant ────────────────────────────────────────────────────────
    private int       filtreAnnee = -1;
    private LocalDate filtreDebut = null;
    private LocalDate filtreFin   = null;

    // ════════════════════════════════════════════
    //  HELPER — convertit java.util.Date → LocalDate
    //  Gère java.util.Date, java.sql.Date, java.sql.Timestamp
    // ════════════════════════════════════════════
    private static LocalDate toLocalDate(Object raw) {
        if (raw == null) return null;
        if (raw instanceof LocalDate)
            return (LocalDate) raw;
        if (raw instanceof java.sql.Date)
            return ((java.sql.Date) raw).toLocalDate();
        if (raw instanceof java.sql.Timestamp)
            return ((java.sql.Timestamp) raw).toLocalDateTime().toLocalDate();
        if (raw instanceof java.util.Date)
            return ((java.util.Date) raw).toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate();
        // String: "2024-05-12", "2024-05-12 10:30:00", "12/05/2024"
        String s = raw.toString().trim();
        if (s.isEmpty()) return null;
        try { return LocalDate.parse(s.length() >= 10 ? s.substring(0, 10) : s,
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")); }
        catch (Exception e1) {}
        try { return LocalDate.parse(s,
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")); }
        catch (Exception e2) {}
        return null;
    }

    // ════════════════════════════════════════════════════════════════════════
    public IncendieStats(IncendieService incendieService, ForetService foretService) {
        this.incendieService = incendieService;
        this.foretService    = foretService;
    }

    // ════════════════════════════════════════════
    //  OUVERTURE
    // ════════════════════════════════════════════

    public void ouvrir(Stage owner) {
        tousIncendies = incendieService.getData();
        toutesForets  = foretService.getData();

        Stage stage = new Stage();
        stage.setTitle("\uD83D\uDCCA Statistiques des Incendies — ForestGuard");
        stage.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) stage.initOwner(owner);

        // ── Onglet 1 uniquement : Statistiques incendies ─────────────────
        ScrollPane scroll1 = new ScrollPane(buildContent(stage));
        scroll1.setFitToWidth(true);
        scroll1.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll1.setStyle("-fx-background:" + BG_MAIN + ";-fx-background-color:" + BG_MAIN + ";");

        Scene scene = new Scene(scroll1, 1100, 780);
        stage.setScene(scene);
        stage.setResizable(true);
        stage.show();

        rafraichirTout();
    }

    // ════════════════════════════════════════════
    //  CONSTRUCTION UI
    // ════════════════════════════════════════════

    private VBox buildContent(Stage stage) {
        VBox root = new VBox(20);
        root.setStyle("-fx-background-color: " + BG_MAIN + "; -fx-padding: 24;");
        root.getChildren().addAll(
                buildHeader(stage),
                buildFiltres(),
                buildKpiRow(),
                buildChartsRow(),
                buildTopForets()
        );
        return root;
    }

    // ── HEADER ────────────────────────────────────────────────────────────────
    private HBox buildHeader(Stage stage) {
        HBox hb = new HBox(14);
        hb.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("🔥");
        icon.setStyle("-fx-font-size: 32;");

        VBox titreBox = new VBox(2);
        Label titre = new Label("Statistiques des Incendies");
        titre.setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: white;");
        Label sousTitre = new Label("Analyse par période · Forêts les plus touchées · Tendances");
        sousTitre.setStyle("-fx-font-size: 12; -fx-text-fill: rgba(255,255,255,0.4);");
        titreBox.getChildren().addAll(titre, sousTitre);
        HBox.setHgrow(titreBox, Priority.ALWAYS);

        Button btnFermer = styledBtn("✕ Fermer",
                "rgba(255,255,255,0.06)", "rgba(255,255,255,0.12)",
                "rgba(255,255,255,0.5)", "white");
        btnFermer.setOnAction(e -> stage.close());

        hb.getChildren().addAll(icon, titreBox, btnFermer);
        return hb;
    }

    // ── FILTRES ───────────────────────────────────────────────────────────────
    private HBox buildFiltres() {
        HBox hb = new HBox(14);
        hb.setAlignment(Pos.CENTER_LEFT);
        hb.setStyle(card() + "-fx-padding: 14 18;");

        // Collecter les années disponibles via toLocalDate()
        Set<Integer> annees = tousIncendies.stream()
                .filter(i -> i.getDateDebut() != null)
                .map(i -> toLocalDate(i.getDateDebut()))
                .filter(Objects::nonNull)
                .map(LocalDate::getYear)
                .collect(Collectors.toCollection(TreeSet::new));

        List<String> anneeItems = new ArrayList<>();
        anneeItems.add("Toutes les années");
        annees.stream().sorted(Comparator.reverseOrder())
                .map(String::valueOf).forEach(anneeItems::add);

        Label lblAnn = small("Année :");
        comboAnnee = new ComboBox<>(FXCollections.observableArrayList(anneeItems));
        comboAnnee.setValue("Toutes les années");
        styleCombo(comboAnnee, 160);

        Label lblPer = small("Période :");
        comboPeriode = new ComboBox<>(FXCollections.observableArrayList(
                "Tout", "Ce mois", "3 derniers mois", "6 derniers mois", "Plage personnalisée"
        ));
        comboPeriode.setValue("Tout");
        styleCombo(comboPeriode, 190);

        Label lblDe = small("Du :");
        dpDebut = new DatePicker();
        dpDebut.setStyle("-fx-font-size: 12; -fx-pref-width: 135;");
        dpDebut.setDisable(false);

        Label lblAu = small("Au :");
        dpFin = new DatePicker();
        dpFin.setStyle("-fx-font-size: 12; -fx-pref-width: 135;");
        dpFin.setDisable(false);

        Button btnAppliquer = styledBtn("▶ Appliquer", "#16a34a", "#15803d", "white", "white");

        // Quand l'utilisateur choisit une date → passer automatiquement en "Plage personnalisée"
        // sans déclencher appliquerFiltres() (le bouton Appliquer s'en charge)
        dpDebut.valueProperty().addListener((obs, o, n) -> {
            if (n != null && !"Plage personnalisée".equals(comboPeriode.getValue()))
                comboPeriode.getSelectionModel().select("Plage personnalisée");
        });
        dpFin.valueProperty().addListener((obs, o, n) -> {
            if (n != null && !"Plage personnalisée".equals(comboPeriode.getValue()))
                comboPeriode.getSelectionModel().select("Plage personnalisée");
        });

        // Le combo période ne désactive plus les DatePickers
        comboAnnee.setOnAction(e -> appliquerFiltres());
        comboPeriode.setOnAction(e -> {
            // Ne pas appliquer si c'est "Plage personnalisée" — attendre le bouton
            if (!"Plage personnalisée".equals(comboPeriode.getValue())) appliquerFiltres();
        });
        btnAppliquer.setOnAction(e -> appliquerFiltres());

        hb.getChildren().addAll(lblAnn, comboAnnee, sep(),
                lblPer, comboPeriode, sep(),
                lblDe, dpDebut, lblAu, dpFin,
                btnAppliquer);
        return hb;
    }

    // ── KPI ROW ───────────────────────────────────────────────────────────────
    private HBox buildKpiRow() {
        HBox hb = new HBox(14);

        lblTotalInc  = kpiVal("0", COL_FIRE);
        lblSupTotale = kpiVal("0 ha", COL_RED);
        lblMoisPic   = kpiVal("—", COL_GOLD);
        lblForetTop  = kpiVal("—", COL_GREEN);

        hb.getChildren().addAll(
                kpiCard("🔥", "Total incendies",       lblTotalInc),
                kpiCard("🌲", "Surface brûlée totale", lblSupTotale),
                kpiCard("📅", "Mois le plus actif",    lblMoisPic),
                kpiCard("🏆", "Forêt la plus touchée", lblForetTop)
        );
        for (int i = 0; i < 4; i++) HBox.setHgrow(hb.getChildren().get(i), Priority.ALWAYS);
        return hb;
    }

    private VBox kpiCard(String icon, String label, Label valLabel) {
        Label ico = new Label(icon);
        ico.setStyle("-fx-font-size: 28;");
        Label lbl = new Label(label.toUpperCase());
        lbl.setStyle("-fx-font-size: 10; -fx-text-fill: rgba(255,255,255,0.35); -fx-font-weight: bold;");
        VBox card = new VBox(6, ico, valLabel, lbl);
        card.setAlignment(Pos.CENTER);
        card.setStyle(card() + "-fx-padding: 20 16;");
        card.setPrefHeight(120);
        return card;
    }

    private Label kpiVal(String txt, String color) {
        Label l = new Label(txt);
        l.setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        return l;
    }

    // ── CHARTS ROW ────────────────────────────────────────────────────────────
    private HBox buildChartsRow() {
        HBox hb = new HBox(14);

        canvasBar = new Canvas(620, 280);
        VBox barCard = new VBox(10);
        Label titreBar = new Label("📊 Incendies par mois");
        titreBar.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: white;");
        barCard.getChildren().addAll(titreBar, canvasBar);
        barCard.setStyle(card() + "-fx-padding: 18;");
        HBox.setHgrow(barCard, Priority.ALWAYS);

        canvasPie = new Canvas(340, 280);
        VBox pieCard = new VBox(10);
        Label titrePie = new Label("🎯 Répartition par gravité");
        titrePie.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: white;");
        pieCard.getChildren().addAll(titrePie, canvasPie);
        pieCard.setStyle(card() + "-fx-padding: 18;");

        hb.getChildren().addAll(barCard, pieCard);
        return hb;
    }

    // ── TOP FORÊTS ────────────────────────────────────────────────────────────
    private VBox buildTopForets() {
        VBox section = new VBox(14);
        Label titre = new Label("🏆 Forêts les plus touchées par les incendies");
        titre.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: white;");
        topForetsList = new VBox(10);
        section.getChildren().addAll(titre, topForetsList);
        section.setStyle(card() + "-fx-padding: 20;");
        return section;
    }

    // ════════════════════════════════════════════
    //  FILTRAGE
    // ════════════════════════════════════════════

    private void appliquerFiltres() {
        String anneeStr = comboAnnee.getValue();
        filtreAnnee = "Toutes les années".equals(anneeStr) ? -1 : Integer.parseInt(anneeStr);

        String periode = comboPeriode.getValue();
        LocalDate now = LocalDate.now();
        switch (periode) {
            case "Ce mois":
                filtreDebut = now.withDayOfMonth(1);
                filtreFin   = now;
                break;
            case "3 derniers mois":
                filtreDebut = now.minusMonths(3);
                filtreFin   = now;
                break;
            case "6 derniers mois":
                filtreDebut = now.minusMonths(6);
                filtreFin   = now;
                break;
            case "Plage personnalisée":
                filtreDebut = dpDebut.getValue();
                filtreFin   = dpFin.getValue();
                break;
            default:
                filtreDebut = null;
                filtreFin   = null;
        }
        rafraichirTout();
    }

    // ════════════════════════════════════════════
    //  FILTRE SUR LES DONNÉES
    //  Utilise toLocalDate() pour convertir
    //  java.util.Date → LocalDate proprement
    // ════════════════════════════════════════════
    private List<Incendie> incendiesFiltres() {
        return tousIncendies.stream().filter(i -> {
            if (i.getDateDebut() == null) return false;

            LocalDate d = toLocalDate(i.getDateDebut());
            if (d == null) return false;

            if (filtreAnnee != -1 && d.getYear() != filtreAnnee) return false;
            if (filtreDebut != null && d.isBefore(filtreDebut))   return false;
            if (filtreFin   != null && d.isAfter(filtreFin))      return false;
            return true;
        }).collect(Collectors.toList());
    }

    // ════════════════════════════════════════════
    //  RAFRAÎCHISSEMENT
    // ════════════════════════════════════════════

    private void rafraichirTout() {
        List<Incendie> filtres = incendiesFiltres();
        mettreAJourKpi(filtres);
        dessinerBarres(filtres);
        dessinerDonut(filtres);
        mettreAJourTopForets(filtres);
    }

    // ── KPI ──────────────────────────────────────────────────────────────────
    private void mettreAJourKpi(List<Incendie> liste) {
        lblTotalInc.setText(String.valueOf(liste.size()));

        double sup = liste.stream().mapToDouble(Incendie::getSuperficieBrulee).sum();
        lblSupTotale.setText(String.format("%.1f ha", sup));

        // ✅ FIX : utilise toLocalDate() pour getMonthValue()
        Map<Integer, Long> parMois = liste.stream()
                .filter(i -> i.getDateDebut() != null)
                .collect(Collectors.groupingBy(
                        i -> toLocalDate(i.getDateDebut()).getMonthValue(),
                        Collectors.counting()));

        if (!parMois.isEmpty()) {
            int moisPic = parMois.entrySet().stream()
                    .max(Map.Entry.comparingByValue()).get().getKey();
            String nomMois = Month.of(moisPic).getDisplayName(TextStyle.FULL, Locale.FRENCH);
            lblMoisPic.setText(cap(nomMois));
        } else {
            lblMoisPic.setText("—");
        }

        Map<Integer, Long> parZone = liste.stream()
                .collect(Collectors.groupingBy(Incendie::getIdZone, Collectors.counting()));
        if (!parZone.isEmpty()) {
            int zoneTop = parZone.entrySet().stream()
                    .max(Map.Entry.comparingByValue()).get().getKey();
            String nomForet = toutesForets.stream()
                    .filter(f -> f.getId() == zoneTop)
                    .map(Foret::getNom)
                    .findFirst()
                    .orElse("Zone " + zoneTop);
            lblForetTop.setText(truncate(nomForet, 18));
        } else {
            lblForetTop.setText("—");
        }
    }

    // ── GRAPHIQUE BARRES ─────────────────────────────────────────────────────
    private void dessinerBarres(List<Incendie> liste) {
        GraphicsContext gc = canvasBar.getGraphicsContext2D();
        double W = canvasBar.getWidth();
        double H = canvasBar.getHeight();

        gc.setFill(Color.web("#0a0f0a"));
        gc.fillRect(0, 0, W, H);

        // ✅ FIX : utilise toLocalDate() pour getMonthValue()
        int[] compteMois = new int[13];
        for (Incendie i : liste) {
            if (i.getDateDebut() != null) {
                LocalDate d = toLocalDate(i.getDateDebut());
                if (d != null) compteMois[d.getMonthValue()]++;
            }
        }

        int max = Arrays.stream(compteMois).max().orElse(1);
        if (max == 0) max = 1;

        double padding = 48;
        double graphW  = W - padding * 2;
        double graphH  = H - 60;
        double barW    = graphW / 12 * 0.6;
        double gap     = graphW / 12;

        // Grille horizontale
        gc.setStroke(Color.web("#ffffff", 0.06));
        gc.setLineWidth(1);
        int gridLines = 5;
        for (int g = 0; g <= gridLines; g++) {
            double y = padding + graphH - (double) g / gridLines * graphH;
            gc.strokeLine(padding, y, W - padding, y);
            gc.setFill(Color.web("#ffffff", 0.25));
            gc.setFont(Font.font("Arial", 10));
            gc.fillText(String.valueOf(g * max / gridLines), 4, y + 4);
        }

        String[] moisLabels = {"Jan","Fév","Mar","Avr","Mai","Jun","Jul","Aoû","Sep","Oct","Nov","Déc"};
        for (int m = 1; m <= 12; m++) {
            double x       = padding + (m - 1) * gap + gap / 2 - barW / 2;
            double hauteur = (double) compteMois[m] / max * graphH;
            double y       = padding + graphH - hauteur;

            if (compteMois[m] > 0) {
                LinearGradient grad = new LinearGradient(0, y, 0, y + hauteur, false,
                        CycleMethod.NO_CYCLE,
                        new Stop(0, BAR_COLORS[m - 1].brighter()),
                        new Stop(1, BAR_COLORS[m - 1].darker()));
                gc.setFill(grad);
                gc.fillRoundRect(x, y, barW, hauteur, 6, 6);

                gc.setFill(Color.WHITE);
                gc.setFont(Font.font("Arial", FontWeight.BOLD, 11));
                gc.fillText(String.valueOf(compteMois[m]), x + barW / 2 - 4, y - 6);
            }

            gc.setFill(Color.web("#ffffff", 0.45));
            gc.setFont(Font.font("Arial", 10));
            gc.fillText(moisLabels[m - 1], x + barW / 2 - 10, H - 10);
        }
    }

    // ── GRAPHIQUE DONUT ───────────────────────────────────────────────────────
    private void dessinerDonut(List<Incendie> liste) {
        GraphicsContext gc = canvasPie.getGraphicsContext2D();
        double W = canvasPie.getWidth();
        double H = canvasPie.getHeight();

        gc.setFill(Color.web("#0a0f0a"));
        gc.fillRect(0, 0, W, H);

        Map<String, Long> parGravite = liste.stream()
                .filter(i -> i.getNiveauGravite() != null)
                .collect(Collectors.groupingBy(
                        i -> i.getNiveauGravite().toUpperCase(), Collectors.counting()));

        if (parGravite.isEmpty()) {
            gc.setFill(Color.web("#ffffff", 0.3));
            gc.setFont(Font.font("Arial", 14));
            gc.fillText("Aucune donnée", W / 2 - 50, H / 2);
            return;
        }

        Map<String, Color> graviteColors = new LinkedHashMap<>();
        graviteColors.put("CRITIQUE", Color.web("#dc2626"));
        graviteColors.put("ÉLEVÉ",    Color.web("#f97316"));
        graviteColors.put("ELEVE",    Color.web("#f97316"));
        graviteColors.put("MOYEN",    Color.web("#eab308"));
        graviteColors.put("FAIBLE",   Color.web("#22c55e"));

        long   total      = parGravite.values().stream().mapToLong(Long::longValue).sum();
        double cx         = W / 2 - 10, cy = H / 2 - 20;
        double outerR     = 90, innerR = 52;
        double startAngle = -90;
        int    legendY    = 0;

        for (Map.Entry<String, Long> entry : parGravite.entrySet()) {
            String key   = entry.getKey();
            long   count = entry.getValue();
            double pct   = (double) count / total;
            double sweep = pct * 360;
            Color  col   = graviteColors.getOrDefault(key, Color.web("#64748b"));

            gc.setFill(col);
            gc.fillArc(cx - outerR, cy - outerR, outerR * 2, outerR * 2,
                    startAngle, sweep, javafx.scene.shape.ArcType.ROUND);

            double legX    = 14;
            double legYpos = 50 + legendY * 48;
            gc.setFill(col);
            gc.fillRoundRect(legX, legYpos, 12, 12, 4, 4);
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 11));
            gc.fillText(key, legX + 18, legYpos + 10);
            gc.setFill(Color.web("#ffffff", 0.5));
            gc.setFont(Font.font("Arial", 10));
            gc.fillText(count + " (" + String.format("%.0f%%", pct * 100) + ")", legX + 18, legYpos + 24);

            startAngle += sweep;
            legendY++;
        }

        // Trou donut
        gc.setFill(Color.web("#0a0f0a"));
        gc.fillOval(cx - innerR, cy - innerR, innerR * 2, innerR * 2);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        String totalStr = String.valueOf(total);
        gc.fillText(totalStr, cx - totalStr.length() * 5, cy + 6);
        gc.setFill(Color.web("#ffffff", 0.4));
        gc.setFont(Font.font("Arial", 10));
        gc.fillText("incendies", cx - 28, cy + 22);
    }

    // ── TOP FORÊTS ────────────────────────────────────────────────────────────
    private void mettreAJourTopForets(List<Incendie> liste) {
        topForetsList.getChildren().clear();

        Map<Integer, Long>   compteParZone = liste.stream()
                .collect(Collectors.groupingBy(Incendie::getIdZone, Collectors.counting()));
        Map<Integer, Double> supParZone    = liste.stream()
                .collect(Collectors.groupingBy(Incendie::getIdZone,
                        Collectors.summingDouble(Incendie::getSuperficieBrulee)));

        if (compteParZone.isEmpty()) {
            Label vide = new Label("Aucune donnée pour la période sélectionnée.");
            vide.setStyle("-fx-text-fill: rgba(255,255,255,0.35); -fx-font-size: 13;");
            topForetsList.getChildren().add(vide);
            return;
        }

        long maxCount = compteParZone.values().stream().max(Long::compareTo).orElse(1L);

        compteParZone.entrySet().stream()
                .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed())
                .limit(8)
                .forEach(entry -> {
                    int    zoneId  = entry.getKey();
                    long   count   = entry.getValue();
                    double supBrul = supParZone.getOrDefault(zoneId, 0.0);
                    double pct     = (double) count / maxCount;

                    String nomForet = toutesForets.stream()
                            .filter(f -> f.getId() == zoneId)
                            .map(Foret::getNom)
                            .findFirst().orElse("Zone " + zoneId);
                    String locForet = toutesForets.stream()
                            .filter(f -> f.getId() == zoneId)
                            .map(Foret::getLocalisation)
                            .findFirst().orElse("");

                    String barColor = count == maxCount ? COL_RED
                            : pct > 0.6 ? COL_FIRE
                            : pct > 0.3 ? COL_GOLD
                            : COL_GREEN;

                    int rang = topForetsList.getChildren().size() + 1;
                    Label lblRang = new Label(rang <= 3 ? new String[]{"🥇","🥈","🥉"}[rang-1] : "#" + rang);
                    lblRang.setStyle("-fx-font-size: 18; -fx-min-width: 36;");

                    Label lblNom = new Label(nomForet);
                    lblNom.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: white;");
                    Label lblLoc = new Label("📍 " + locForet);
                    lblLoc.setStyle("-fx-font-size: 11; -fx-text-fill: rgba(255,255,255,0.4);");
                    VBox infos = new VBox(2, lblNom, lblLoc);
                    HBox.setHgrow(infos, Priority.ALWAYS);

                    Label lblCount = new Label(count + " incendie" + (count > 1 ? "s" : ""));
                    lblCount.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: " + barColor + ";");
                    Label lblSup = new Label(String.format("%.1f ha brûlés", supBrul));
                    lblSup.setStyle("-fx-font-size: 11; -fx-text-fill: rgba(255,255,255,0.4);");
                    VBox stats = new VBox(2, lblCount, lblSup);
                    stats.setAlignment(Pos.CENTER_RIGHT);

                    HBox row = new HBox(14, lblRang, infos, stats);
                    row.setAlignment(Pos.CENTER_LEFT);

                    Rectangle barBg = new Rectangle(0, 5);
                    barBg.setFill(Color.web("#ffffff", 0.06));
                    barBg.setArcWidth(4); barBg.setArcHeight(4);

                    Rectangle barFg = new Rectangle(0, 5);
                    barFg.setFill(Color.web(barColor));
                    barFg.setArcWidth(4); barFg.setArcHeight(4);

                    StackPane progressBar = new StackPane(barBg, barFg);
                    progressBar.setAlignment(Pos.CENTER_LEFT);
                    progressBar.widthProperty().addListener((obs, oldW, newW) -> {
                        double totalW = newW.doubleValue();
                        barBg.setWidth(totalW);
                        barFg.setWidth(totalW * pct);
                    });

                    VBox cellule = new VBox(8, row, progressBar);
                    cellule.setStyle(
                            "-fx-background-color: rgba(255,255,255,0.03);" +
                                    "-fx-border-color: rgba(255,255,255,0.07); -fx-border-radius: 10;" +
                                    "-fx-background-radius: 10; -fx-padding: 14 16;");
                    cellule.setOnMouseEntered(e -> cellule.setStyle(
                            "-fx-background-color: rgba(255,107,53,0.08);" +
                                    "-fx-border-color: rgba(255,107,53,0.2); -fx-border-radius: 10;" +
                                    "-fx-background-radius: 10; -fx-padding: 14 16;"));
                    cellule.setOnMouseExited(e -> cellule.setStyle(
                            "-fx-background-color: rgba(255,255,255,0.03);" +
                                    "-fx-border-color: rgba(255,255,255,0.07); -fx-border-radius: 10;" +
                                    "-fx-background-radius: 10; -fx-padding: 14 16;"));

                    // ── Bouton "Voir sur la carte" ──────────────────────────
                    Foret foretCible = toutesForets.stream()
                            .filter(f -> f.getId() == zoneId).findFirst().orElse(null);

                    // Coordonnées : utilise lat/lng de la BDD, sinon estime depuis la localisation
                    double[] coords = getCoords(foretCible);
                    Button btnCarte = new Button("\uD83D\uDDFA Voir sur la carte");
                    btnCarte.setStyle("-fx-background-color:rgba(56,189,248,0.12);-fx-text-fill:#38bdf8;" +
                            "-fx-background-radius:8;-fx-border-color:rgba(56,189,248,0.3);-fx-border-radius:8;" +
                            "-fx-font-size:11;-fx-font-weight:bold;-fx-cursor:hand;-fx-padding:5 12;");
                    btnCarte.setOnMouseEntered(ev -> btnCarte.setStyle("-fx-background-color:rgba(56,189,248,0.22);-fx-text-fill:#7dd3fc;" +
                            "-fx-background-radius:8;-fx-border-color:rgba(56,189,248,0.5);-fx-border-radius:8;" +
                            "-fx-font-size:11;-fx-font-weight:bold;-fx-cursor:hand;-fx-padding:5 12;"));
                    btnCarte.setOnMouseExited(ev -> btnCarte.setStyle("-fx-background-color:rgba(56,189,248,0.12);-fx-text-fill:#38bdf8;" +
                            "-fx-background-radius:8;-fx-border-color:rgba(56,189,248,0.3);-fx-border-radius:8;" +
                            "-fx-font-size:11;-fx-font-weight:bold;-fx-cursor:hand;-fx-padding:5 12;"));
                    final double lat = coords[0];
                    final double lng = coords[1];
                    final String nomF = nomForet;
                    btnCarte.setOnAction(ev -> ouvrirCarteZoom(lat, lng, nomF));
                    row.getChildren().add(btnCarte);

                    topForetsList.getChildren().add(cellule);
                });
    }

    // ════════════════════════════════════════════
    //  HELPERS UI
    // ════════════════════════════════════════════

    private String card() {
        return "-fx-background-color: rgba(255,255,255,0.04);" +
                "-fx-border-color: rgba(255,255,255,0.07); -fx-border-radius: 14;" +
                "-fx-background-radius: 14;";
    }

    private Label small(String txt) {
        Label l = new Label(txt);
        l.setStyle("-fx-text-fill: rgba(255,255,255,0.45); -fx-font-size: 12;");
        return l;
    }

    private Region sep() {
        Region r = new Region();
        r.setMinWidth(1); r.setMaxWidth(1); r.setMinHeight(20);
        r.setStyle("-fx-background-color: rgba(255,255,255,0.1);");
        return r;
    }

    private void styleCombo(ComboBox<String> cb, double w) {
        cb.setPrefWidth(w);
        cb.setStyle("-fx-background-color: rgba(255,255,255,0.06);" +
                "-fx-border-color: rgba(255,255,255,0.12); -fx-border-radius: 8;" +
                "-fx-text-fill: white; -fx-background-radius: 8; -fx-font-size: 12;");
    }

    private Button styledBtn(String txt, String bg, String bgHov, String fg, String fgHov) {
        Button b = new Button(txt);
        String base = "-fx-background-color: " + bg + "; -fx-text-fill: " + fg + ";" +
                "-fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 12;" +
                "-fx-padding: 8 16; -fx-font-weight: bold;";
        String hov  = "-fx-background-color: " + bgHov + "; -fx-text-fill: " + fgHov + ";" +
                "-fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 12;" +
                "-fx-padding: 8 16; -fx-font-weight: bold;";
        b.setStyle(base);
        b.setOnMouseEntered(e -> b.setStyle(hov));
        b.setOnMouseExited(e  -> b.setStyle(base));
        return b;
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) + "\u2026" : s;
    }

    private String cap(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // ── Retourne les coordonnées d'une forêt (BDD ou estimation par localisation) ──
    private double[] getCoords(Foret f) {
        // Si la BDD a des coordonnées valides, on les utilise
        if (f != null && f.getLatitude() != 0 && f.getLongitude() != 0) {
            return new double[]{f.getLatitude(), f.getLongitude()};
        }
        // Sinon on estime depuis la localisation (villes tunisiennes/algériennes courantes)
        String loc = f != null && f.getLocalisation() != null ? f.getLocalisation().toLowerCase() : "";
        // Tunisie
        if (loc.contains("jendouba"))          return new double[]{36.50, 8.78};
        if (loc.contains("ain draham"))        return new double[]{36.78, 8.69};
        if (loc.contains("bizerte"))           return new double[]{37.27, 9.87};
        if (loc.contains("beja"))              return new double[]{36.73, 9.18};
        if (loc.contains("kef"))               return new double[]{36.18, 8.71};
        if (loc.contains("siliana"))           return new double[]{36.08, 9.37};
        if (loc.contains("kasserine"))         return new double[]{35.17, 8.83};
        if (loc.contains("zaghouan"))          return new double[]{36.40, 10.14};
        if (loc.contains("nabeul"))            return new double[]{36.45, 10.73};
        if (loc.contains("tunis"))             return new double[]{36.82, 10.17};
        if (loc.contains("sousse"))            return new double[]{35.83, 10.64};
        if (loc.contains("sfax"))              return new double[]{34.74, 10.76};
        if (loc.contains("gabes"))             return new double[]{33.88, 10.10};
        if (loc.contains("gafsa"))             return new double[]{34.42, 8.78};
        // Algérie
        if (loc.contains("alger"))             return new double[]{36.74, 3.06};
        if (loc.contains("bejaia") || loc.contains("béjaïa")) return new double[]{36.75, 5.08};
        if (loc.contains("tizi"))              return new double[]{36.71, 4.05};
        if (loc.contains("jijel"))             return new double[]{36.82, 5.77};
        if (loc.contains("skikda"))            return new double[]{36.88, 6.90};
        if (loc.contains("annaba"))            return new double[]{36.90, 7.76};
        if (loc.contains("constantine"))       return new double[]{36.37, 6.61};
        if (loc.contains("tlemcen"))           return new double[]{34.88, -1.32};
        if (loc.contains("oran"))              return new double[]{35.69, -0.63};
        if (loc.contains("frontiere"))         return new double[]{36.50, 2.50};
        if (loc.contains("nord"))              return new double[]{36.80, 3.00};
        if (loc.contains("sud"))               return new double[]{28.00, 3.00};
        if (loc.contains("est"))               return new double[]{36.50, 7.50};
        if (loc.contains("ouest"))             return new double[]{35.00, -0.50};
        // Défaut : centre Tunisie
        return new double[]{36.80, 10.18};
    }

    // ── Ouvre une WebView carte centrée sur la forêt ──────────────────────────

    // ════════════════════════════════════════════
    //  ONGLET ANALYSE CAPTEURS (table fire_alerts)
    // ════════════════════════════════════════════

    private static class AlerteRow {
        String foret, localisation; int mois, annee;
        double pctIncendie, tempMoy, humMoy, fumeeMoy;
        int nbDanger, nbAttention, nbSur;
        String dateCalc;
    }

    private List<AlerteRow> chargerAlertes(Integer annee, Integer mois, String dateDebut, String dateFin) {
        List<AlerteRow> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM fire_alerts WHERE 1=1");
        if (annee != null)     sql.append(" AND annee=").append(annee);
        if (mois  != null)     sql.append(" AND mois=").append(mois);
        if (dateDebut != null) sql.append(" AND date_calcul >= '").append(dateDebut).append("'");
        if (dateFin   != null) sql.append(" AND date_calcul <= '").append(dateFin).append("'");
        sql.append(" ORDER BY annee DESC, mois DESC");
        java.sql.Connection cnxAlertes = edu.gestionincendies.tools.MyConnection.getInstance().getCnx();
        if (cnxAlertes == null) { System.err.println("chargerAlertes: pas de connexion MySQL."); return list; }
        try (java.sql.Statement st = cnxAlertes.createStatement();
             java.sql.ResultSet rs = st.executeQuery(sql.toString())) {
            while (rs.next()) {
                AlerteRow r = new AlerteRow();
                r.foret        = rs.getString("foret");
                r.localisation = rs.getString("localisation");
                r.mois         = rs.getInt("mois");
                r.annee        = rs.getInt("annee");
                r.pctIncendie  = rs.getDouble("pourcentage_incendie");
                r.nbDanger     = rs.getInt("nb_danger");
                r.nbAttention  = rs.getInt("nb_attention");
                r.nbSur        = rs.getInt("nb_sur");
                r.tempMoy      = rs.getDouble("temp_moyenne");
                r.humMoy       = rs.getDouble("hum_moyenne");
                r.fumeeMoy     = rs.getDouble("fumee_moyenne");
                r.dateCalc     = rs.getString("date_calcul");
                list.add(r);
            }
        } catch (Exception e) { System.err.println("fire_alerts: " + e.getMessage()); }
        return list;
    }

    private ScrollPane buildCapteurStats() {
        VBox root = new VBox(20);
        root.setStyle("-fx-background-color:" + BG_MAIN + ";-fx-padding:24;");

        // ── Filtres ───────────────────────────────────────────────────────
        HBox filtres = new HBox(14); filtres.setAlignment(Pos.CENTER_LEFT);
        filtres.setStyle(card() + "-fx-padding:14 18;");

        // Années disponibles
        List<Integer> annees = new ArrayList<>();
        java.sql.Connection cnxAnnees = edu.gestionincendies.tools.MyConnection.getInstance().getCnx();
        if (cnxAnnees != null) {
        try (java.sql.Statement st = cnxAnnees.createStatement();
             java.sql.ResultSet rs = st.executeQuery("SELECT DISTINCT annee FROM fire_alerts ORDER BY annee DESC")) {
            while (rs.next()) annees.add(rs.getInt(1));
        } catch (Exception e) { System.err.println("Années: " + e.getMessage()); }
        }

        ComboBox<String> cbAnnee = new ComboBox<>();
        cbAnnee.getItems().add("Toutes les années");
        annees.forEach(a -> cbAnnee.getItems().add(String.valueOf(a)));
        cbAnnee.setValue("Toutes les années");
        styleCombo(cbAnnee, 160);

        ComboBox<String> cbMois = new ComboBox<>();
        cbMois.getItems().addAll("Tous les mois","Jan","Fév","Mar","Avr","Mai","Jun","Jul","Aoû","Sep","Oct","Nov","Déc");
        cbMois.setValue("Tous les mois");
        styleCombo(cbMois, 140);

        DatePicker dpDeb = new DatePicker(); dpDeb.setStyle("-fx-font-size:12;-fx-pref-width:130;"); dpDeb.setDisable(false);
        DatePicker dpFin = new DatePicker(); dpFin.setStyle("-fx-font-size:12;-fx-pref-width:130;"); dpFin.setDisable(false);

        CheckBox cbPeriode = new CheckBox("Période personnalisée");
        cbPeriode.setStyle("-fx-text-fill:rgba(255,255,255,0.6);-fx-font-size:12;");
        // Quand l'utilisateur choisit une date → cocher automatiquement la période personnalisée
        dpDeb.setOnAction(e -> { if (dpDeb.getValue() != null) cbPeriode.setSelected(true); });
        dpFin.setOnAction(e -> { if (dpFin.getValue() != null) cbPeriode.setSelected(true); });

        Button btnAppliquer = styledBtn("▶ Appliquer","#16a34a","#15803d","white","white");

        filtres.getChildren().addAll(
            small("Année :"), cbAnnee, sep(),
            small("Mois :"), cbMois, sep(),
            cbPeriode, small("Du :"), dpDeb, small("Au :"), dpFin,
            btnAppliquer);

        // ── Zone résultats ────────────────────────────────────────────────
        VBox resultsBox = new VBox(16);

        Runnable appliquer = () -> {
            resultsBox.getChildren().clear();
            Integer annee = cbAnnee.getValue().equals("Toutes les années") ? null : Integer.parseInt(cbAnnee.getValue());
            Integer mois  = cbMois.getValue().equals("Tous les mois") ? null : cbMois.getSelectionModel().getSelectedIndex();
            String deb = cbPeriode.isSelected() && dpDeb.getValue() != null ? dpDeb.getValue().toString() : null;
            String fin = cbPeriode.isSelected() && dpFin.getValue() != null ? dpFin.getValue().toString() : null;

            List<AlerteRow> alertes = chargerAlertes(annee, mois, deb, fin);

            if (alertes.isEmpty()) {
                Label vide = new Label("Aucune donnée pour les filtres sélectionnés.");
                vide.setStyle("-fx-text-fill:rgba(255,255,255,0.4);-fx-font-size:14;");
                resultsBox.getChildren().add(vide);
                return;
            }

            // ── KPI capteurs ──────────────────────────────────────────────
            double avgPct  = alertes.stream().mapToDouble(r -> r.pctIncendie).average().orElse(0);
            double avgTemp = alertes.stream().mapToDouble(r -> r.tempMoy).average().orElse(0);
            double avgHum  = alertes.stream().mapToDouble(r -> r.humMoy).average().orElse(0);
            double avgFum  = alertes.stream().mapToDouble(r -> r.fumeeMoy).average().orElse(0);
            long   nbDang  = alertes.stream().mapToLong(r -> r.nbDanger).sum();

            HBox kpiRow = new HBox(14);
            kpiRow.getChildren().addAll(
                kpiCapteur("\uD83D\uDD25", "Risque moyen", String.format("%.1f%%", avgPct), avgPct>60?"#ef4444":avgPct>30?"#f97316":"#4ade80"),
                kpiCapteur("\uD83C\uDF21\uFE0F", "Temp. moyenne", String.format("%.1f °C", avgTemp), avgTemp>35?"#ef4444":avgTemp>28?"#fbbf24":"#60a5fa"),
                kpiCapteur("\uD83D\uDCA7", "Humidité moy.", String.format("%.1f %%", avgHum), avgHum<20?"#ef4444":avgHum<40?"#fbbf24":"#4ade80"),
                kpiCapteur("\uD83D\uDCA8", "Fumée moyenne", String.format("%.1f", avgFum), avgFum>50?"#ef4444":avgFum>25?"#fbbf24":"#94a3b8"),
                kpiCapteur("\u26A0\uFE0F", "Alertes danger", String.valueOf(nbDang), nbDang>0?"#ef4444":"#4ade80"));
            for (int i=0;i<5;i++) HBox.setHgrow(kpiRow.getChildren().get(i), Priority.ALWAYS);
            resultsBox.getChildren().add(kpiRow);

            // ── Graphique % incendie par mois ─────────────────────────────
            resultsBox.getChildren().add(buildGraphiqueCapteurs(alertes));

            // ── Tableau détaillé ──────────────────────────────────────────
            resultsBox.getChildren().add(buildTableauAlertes(alertes));
        };

        btnAppliquer.setOnAction(e -> appliquer.run());
        appliquer.run(); // Charger au démarrage

        root.getChildren().addAll(
            buildTitreCapteur(),
            filtres,
            resultsBox);

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background:" + BG_MAIN + ";-fx-background-color:" + BG_MAIN + ";");
        return scroll;
    }

    private HBox buildTitreCapteur() {
        HBox hb = new HBox(14); hb.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label("\uD83D\uDCE1"); icon.setStyle("-fx-font-size:28;");
        VBox tb = new VBox(2);
        Label t = new Label("Analyse des Capteurs — Risque Incendie"); t.setStyle("-fx-font-size:20;-fx-font-weight:bold;-fx-text-fill:white;");
        Label s = new Label("Données issues de la table fire_alerts · Température · Humidité · Fumée · Risque"); s.setStyle("-fx-font-size:12;-fx-text-fill:rgba(255,255,255,0.4);");
        tb.getChildren().addAll(t,s); HBox.setHgrow(tb, Priority.ALWAYS);
        hb.getChildren().addAll(icon, tb);
        return hb;
    }

    private VBox kpiCapteur(String icon, String label, String val, String color) {
        Label lblV = new Label(icon + " " + val); lblV.setStyle("-fx-font-size:18;-fx-font-weight:bold;-fx-text-fill:"+color+";");
        Label lblL = new Label(label); lblL.setStyle("-fx-font-size:10;-fx-text-fill:rgba(255,255,255,0.4);");
        VBox card = new VBox(2, lblV, lblL); card.setAlignment(Pos.CENTER);
        card.setStyle(card()+"-fx-padding:14 16;"); return card;
    }

    private VBox buildGraphiqueCapteurs(List<AlerteRow> alertes) {
        // Regrouper par mois : moyenne % incendie
        double[] moyMois = new double[13];
        int[]    cntMois = new int[13];
        for (AlerteRow r : alertes) {
            if (r.mois >= 1 && r.mois <= 12) { moyMois[r.mois] += r.pctIncendie; cntMois[r.mois]++; }
        }
        for (int m=1;m<=12;m++) if (cntMois[m]>0) moyMois[m] /= cntMois[m];

        double max = 0; for (int m=1;m<=12;m++) if (moyMois[m]>max) max=moyMois[m];
        if (max==0) max=100;

        Canvas canvas = new Canvas(900, 220);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web("#0a0f0a")); gc.fillRect(0,0,900,220);

        double pad=48, gW=900-pad*2, gH=160;
        // Grille
        gc.setStroke(Color.web("#ffffff",0.06)); gc.setLineWidth(1);
        for (int g=0;g<=4;g++) {
            double y=pad+gH-(double)g/4*gH;
            gc.strokeLine(pad,y,900-pad,y);
            gc.setFill(Color.web("#ffffff",0.25)); gc.setFont(Font.font("Arial",10));
            gc.fillText(String.format("%.0f%%",g*max/4),4,y+4);
        }

        String[] moisL={"Jan","Fév","Mar","Avr","Mai","Jun","Jul","Aoû","Sep","Oct","Nov","Déc"};
        double barW=gW/12*0.6, gap=gW/12;
        for (int m=1;m<=12;m++) {
            double x=pad+(m-1)*gap+gap/2-barW/2;
            double h=moyMois[m]/max*gH, y=pad+gH-h;
            if (moyMois[m]>0) {
                String col=moyMois[m]>60?"#ef4444":moyMois[m]>30?"#f97316":"#4ade80";
                gc.setFill(Color.web(col)); gc.fillRoundRect(x,y,barW,h,6,6);
                gc.setFill(Color.WHITE); gc.setFont(Font.font("Arial",FontWeight.BOLD,10));
                gc.fillText(String.format("%.0f%%",moyMois[m]),x+barW/2-12,y-5);
            }
            gc.setFill(Color.web("#ffffff",0.45)); gc.setFont(Font.font("Arial",10));
            gc.fillText(moisL[m-1],x+barW/2-10,220-8);
        }

        Label titre = new Label("\uD83D\uDCCA Risque incendie moyen par mois (%)");
        titre.setStyle("-fx-font-size:14;-fx-font-weight:bold;-fx-text-fill:white;");
        VBox card = new VBox(10, titre, canvas);
        card.setStyle(card()+"-fx-padding:18;"); return card;
    }

    private VBox buildTableauAlertes(List<AlerteRow> alertes) {
        VBox box = new VBox(0);
        Label titre = new Label("\uD83D\uDCCB Détail par zone et période");
        titre.setStyle("-fx-font-size:14;-fx-font-weight:bold;-fx-text-fill:white;-fx-padding:0 0 10 0;");

        // En-tête
        HBox entete = new HBox(0);
        entete.setStyle("-fx-background-color:rgba(255,255,255,0.08);-fx-padding:8 12;-fx-background-radius:8 8 0 0;");
        String[][] cols = {{"Forêt","140"},{"Localisation","120"},{"Mois/Année","100"},{"Risque %","90"},{"Temp °C","80"},{"Humidité %","90"},{"Fumée","80"},{"Danger","70"},{"Attention","80"},{"Sûr","60"}};
        for (String[] c : cols) {
            Label l = new Label(c[0]); l.setPrefWidth(Double.parseDouble(c[1]));
            l.setStyle("-fx-font-size:11;-fx-font-weight:bold;-fx-text-fill:rgba(255,255,255,0.5);");
            entete.getChildren().add(l);
        }
        box.getChildren().addAll(titre, entete);

        for (AlerteRow r : alertes) {
            HBox row = new HBox(0);
            String bgRow = r.pctIncendie>60?"rgba(239,68,68,0.08)":r.pctIncendie>30?"rgba(249,115,22,0.08)":"rgba(255,255,255,0.02)";
            row.setStyle("-fx-background-color:"+bgRow+";-fx-padding:8 12;-fx-border-color:rgba(255,255,255,0.04);-fx-border-width:0 0 1 0;");
            String colRisque = r.pctIncendie>60?"#ef4444":r.pctIncendie>30?"#f97316":"#4ade80";
            String[] vals = {r.foret, r.localisation, r.mois+"/"+r.annee,
                String.format("%.1f%%",r.pctIncendie), String.format("%.1f",r.tempMoy),
                String.format("%.1f",r.humMoy), String.format("%.1f",r.fumeeMoy),
                String.valueOf(r.nbDanger), String.valueOf(r.nbAttention), String.valueOf(r.nbSur)};
            double[] widths = {140,120,100,90,80,90,80,70,80,60};
            for (int i=0;i<vals.length;i++) {
                Label l = new Label(vals[i]); l.setPrefWidth(widths[i]);
                String style = "-fx-font-size:12;-fx-text-fill:";
                if (i==3) style += colRisque+";-fx-font-weight:bold;";
                else if (i==7 && r.nbDanger>0) style += "#ef4444;-fx-font-weight:bold;";
                else style += "#e2e8f0;";
                l.setStyle(style); row.getChildren().add(l);
            }
            box.getChildren().add(row);
        }
        VBox card = new VBox(box); card.setStyle(card()+"-fx-padding:18;"); return card;
    }

    private void ouvrirCarteZoom(double lat, double lng, String nomForet) {
        Stage mapStage = new Stage();
        mapStage.setTitle("\uD83D\uDDFA Carte \u2014 " + nomForet);
        mapStage.initModality(Modality.APPLICATION_MODAL);

        String geojson = "null";
        try {
            java.net.URL url = getClass().getResource("/map_web/tn-governorates.geojson");
            if (url != null) geojson = new String(url.openStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) { System.err.println("GeoJSON: " + e.getMessage()); }

        javafx.scene.web.WebView webView = new javafx.scene.web.WebView();
        webView.setPrefSize(900, 580);
        javafx.scene.web.WebEngine eng = webView.getEngine();
        eng.setJavaScriptEnabled(true);

        // Proxy local pour les tuiles
        int proxyPort = TileProxy.getInstance().getPort();
        String tileUrl = proxyPort > 0
            ? "http://127.0.0.1:" + proxyPort + "/tiles/{z}/{x}/{y}.png"
            : "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png";
        String leafletSrc = proxyPort > 0
            ? "http://127.0.0.1:" + proxyPort + "/leaflet/leaflet.js"
            : "https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.js";

        String safeNom = nomForet.replace("'", "\\'").replace("\"", "\\\"");
        String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'>" +
            "<style>*{margin:0;padding:0;box-sizing:border-box;}html,body,#map{width:100%;height:100%;background:#0a1a0e;}" +
            ".leaflet-control-attribution{display:none!important;}" +
            ".leaflet-bar a{background-color:rgba(8,22,13,0.95)!important;color:#4ade80!important;border-color:rgba(74,222,128,0.2)!important}" +
            "</style>" +
            "</head><body><div id='map'></div>" +
            "<script src='" + leafletSrc + "'></script>" +
            "<script>" +
            "var GEOJSON=" + geojson + ";" +
            "var regionColors=['#c8e6c9','#a5d6a7','#81c784','#66bb6a','#4caf50','#43a047','#388e3c','#2e7d32'];" +
            "var map=L.map('map',{zoomControl:true,attributionControl:false}).setView([" + lat + "," + lng + "],10);" +
            "L.tileLayer('" + tileUrl + "',{maxZoom:19}).addTo(map);" +
            "var gl=L.geoJson(GEOJSON,{" +
            "  style:function(f){return{fillColor:regionColors[Math.floor(Math.random()*regionColors.length)],weight:1.5,opacity:1,color:'white',dashArray:'3',fillOpacity:0.50};}," +
            "  onEachFeature:function(f,l){" +
            "    l.on({mouseover:function(e){e.target.setStyle({weight:3,color:'#fff',fillOpacity:0.75});e.target.bringToFront();}," +
            "          mouseout:function(e){gl.resetStyle(e.target);}});" +
            "    l.bindTooltip(f.properties.gouv_fr||'',{permanent:false,direction:'center',opacity:0.9});" +
            "  }" +
            "}).addTo(map);" +
            "var col='#e53935';" +
            "var pin='<div style=\"position:relative;width:36px;height:44px;\"><div style=\"width:36px;height:36px;border-radius:50% 50% 50% 0;background:'+col+';transform:rotate(-45deg);box-shadow:0 3px 12px rgba(0,0,0,0.35);border:3px solid white;\"></div><div style=\"position:absolute;top:7px;left:7px;width:22px;height:22px;border-radius:50%;background:white;display:flex;align-items:center;justify-content:center;font-size:14px;transform:rotate(45deg);\">&#127795;</div></div>';" +
            "var ic=L.divIcon({html:pin,iconSize:[36,44],iconAnchor:[18,44],className:''});" +
            "L.marker([" + lat + "," + lng + "],{icon:ic}).addTo(map)" +
            "  .bindPopup('<b>&#127794; " + safeNom + "</b><br><small>" + String.format("%.4f", lat) + ", " + String.format("%.4f", lng) + "</small>').openPopup();" +
            "L.circle([" + lat + "," + lng + "],{color:'#e53935',fillColor:'#e53935',fillOpacity:0.12,radius:8000,weight:2,dashArray:'6,4'}).addTo(map);" +
            "</script></body></html>";

        eng.loadContent(html, "text/html");

        Label lblTitre = new Label("\uD83C\uDF32 " + nomForet);
        lblTitre.setStyle("-fx-font-size:15;-fx-font-weight:bold;-fx-text-fill:white;");
        Button btnFermer = new Button("\u2715 Fermer");
        btnFermer.setStyle("-fx-background-color:rgba(255,255,255,0.15);-fx-text-fill:white;-fx-background-radius:8;-fx-cursor:hand;-fx-padding:6 14;");
        btnFermer.setOnAction(e -> mapStage.close());
        HBox topBar = new HBox(lblTitre);
        topBar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(lblTitre, Priority.ALWAYS);
        topBar.getChildren().add(btnFermer);
        topBar.setStyle("-fx-background-color:#1b5e20;-fx-padding:10 16;");
        VBox root = new VBox(0, topBar, webView);
        VBox.setVgrow(webView, Priority.ALWAYS);
        mapStage.setScene(new javafx.scene.Scene(root, 900, 640));
        mapStage.setResizable(true);
        mapStage.show();
    }
}

