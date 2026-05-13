package edu.capteur.controllers;

import edu.capteur.entities.Capteur;
import edu.capteur.services.AIPredictionService;
import edu.capteur.services.ForetService;
import edu.capteur.entities.Foret;
import edu.capteur.services.CapteurService;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ForestGuard - Carte OpenStreetMap avec photos reelles de capteurs IoT
 * Markers avec images SVG base64 par type de capteur
 */
public class MapController {

    @FXML private WebView mapWebView;
    @FXML private Label   lblStatutCarte;

    private final CapteurService      capteurService = new CapteurService();
    private final AIPredictionService aiService      = new AIPredictionService();
    private final ForetService        foretService   = new ForetService();

    // ── Photos SVG ultra-realistes des capteurs par type ──────────────────

    /** Capteur Temperature/Humidite (SHT30/DHT22) - PCB vert avec chip */
    private static final String SVG_TEMPERATURE =
        "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 80 80'>" +
        "<defs>" +
        "<linearGradient id='pcbT' x1='0%' y1='0%' x2='100%' y2='100%'>" +
        "<stop offset='0%' style='stop-color:#2d6a2d'/>" +
        "<stop offset='100%' style='stop-color:#1a3d1a'/></linearGradient>" +
        "<linearGradient id='chipT' x1='0%' y1='0%' x2='100%' y2='100%'>" +
        "<stop offset='0%' style='stop-color:#555'/>" +
        "<stop offset='100%' style='stop-color:#222'/></linearGradient>" +
        "</defs>" +
        "<rect x='5' y='5' width='70' height='70' rx='5' fill='url(#pcbT)' stroke='#1a5c1a' stroke-width='1.5'/>" +
        "<line x1='12' y1='35' x2='68' y2='35' stroke='#b8860b' stroke-width='1' opacity='0.5'/>" +
        "<line x1='12' y1='50' x2='68' y2='50' stroke='#b8860b' stroke-width='1' opacity='0.5'/>" +
        "<line x1='28' y1='12' x2='28' y2='68' stroke='#b8860b' stroke-width='0.8' opacity='0.35'/>" +
        "<line x1='52' y1='12' x2='52' y2='68' stroke='#b8860b' stroke-width='0.8' opacity='0.35'/>" +
        "<rect x='20' y='18' width='40' height='30' rx='2' fill='url(#chipT)' stroke='#777' stroke-width='1'/>" +
        "<rect x='13' y='22' width='7' height='3' rx='1' fill='#c8a44a'/>" +
        "<rect x='13' y='28' width='7' height='3' rx='1' fill='#c8a44a'/>" +
        "<rect x='13' y='34' width='7' height='3' rx='1' fill='#c8a44a'/>" +
        "<rect x='13' y='40' width='7' height='3' rx='1' fill='#c8a44a'/>" +
        "<rect x='60' y='22' width='7' height='3' rx='1' fill='#c8a44a'/>" +
        "<rect x='60' y='28' width='7' height='3' rx='1' fill='#c8a44a'/>" +
        "<rect x='60' y='34' width='7' height='3' rx='1' fill='#c8a44a'/>" +
        "<rect x='60' y='40' width='7' height='3' rx='1' fill='#c8a44a'/>" +
        "<text x='40' y='30' text-anchor='middle' font-family='monospace' font-size='6' fill='#ccc' font-weight='bold'>SHT30</text>" +
        "<text x='40' y='38' text-anchor='middle' font-family='monospace' font-size='4.5' fill='#999'>TEMP/HUM</text>" +
        "<text x='40' y='44' text-anchor='middle' font-family='monospace' font-size='4' fill='#666'>ForestGuard</text>" +
        "<rect x='20' y='55' width='9' height='7' rx='1' fill='#4455cc' stroke='#3344bb' stroke-width='0.5'/>" +
        "<rect x='33' y='55' width='9' height='7' rx='1' fill='#4455cc' stroke='#3344bb' stroke-width='0.5'/>" +
        "<rect x='46' y='55' width='9' height='7' rx='1' fill='#884422' stroke='#773311' stroke-width='0.5'/>" +
        "<circle cx='65' cy='62' r='5' fill='#00cc44' opacity='0.85'/>" +
        "<circle cx='65' cy='62' r='2.5' fill='#aaffcc'/>" +
        "<circle cx='11' cy='11' r='3.5' fill='none' stroke='#556655' stroke-width='1.5'/>" +
        "<circle cx='69' cy='11' r='3.5' fill='none' stroke='#556655' stroke-width='1.5'/>" +
        "<circle cx='11' cy='69' r='3.5' fill='none' stroke='#556655' stroke-width='1.5'/>" +
        "<circle cx='69' cy='69' r='3.5' fill='none' stroke='#556655' stroke-width='1.5'/>" +
        "</svg>";

    /** Capteur de fumee/gaz (MQ-135) - PCB bleu avec dome metallique */
    private static final String SVG_FUMEE =
        "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 80 80'>" +
        "<defs>" +
        "<linearGradient id='pcbF' x1='0%' y1='0%' x2='100%' y2='100%'>" +
        "<stop offset='0%' style='stop-color:#1a2a5c'/>" +
        "<stop offset='100%' style='stop-color:#0d1840'/></linearGradient>" +
        "<radialGradient id='domeF' cx='40%' cy='35%' r='60%'>" +
        "<stop offset='0%' style='stop-color:#e8cc55'/>" +
        "<stop offset='60%' style='stop-color:#aa8822'/>" +
        "<stop offset='100%' style='stop-color:#664400'/></radialGradient>" +
        "</defs>" +
        "<rect x='5' y='5' width='70' height='70' rx='5' fill='url(#pcbF)' stroke='#0d1840' stroke-width='1.5'/>" +
        "<line x1='12' y1='40' x2='68' y2='40' stroke='#3355cc' stroke-width='0.8' opacity='0.45'/>" +
        "<line x1='40' y1='12' x2='40' y2='68' stroke='#3355cc' stroke-width='0.8' opacity='0.3'/>" +
        "<circle cx='40' cy='36' r='19' fill='#776600' stroke='#554400' stroke-width='1.5'/>" +
        "<circle cx='40' cy='36' r='16' fill='url(#domeF)'/>" +
        "<line x1='24' y1='36' x2='56' y2='36' stroke='#333' stroke-width='1' opacity='0.6'/>" +
        "<line x1='40' y1='20' x2='40' y2='52' stroke='#333' stroke-width='1' opacity='0.6'/>" +
        "<line x1='29' y1='25' x2='51' y2='47' stroke='#333' stroke-width='0.7' opacity='0.45'/>" +
        "<line x1='51' y1='25' x2='29' y2='47' stroke='#333' stroke-width='0.7' opacity='0.45'/>" +
        "<circle cx='40' cy='36' r='5' fill='#111' opacity='0.75'/>" +
        "<circle cx='40' cy='36' r='2' fill='#333' opacity='0.9'/>" +
        "<rect x='18' y='61' width='5' height='10' rx='1' fill='#c8a44a'/>" +
        "<rect x='26' y='61' width='5' height='10' rx='1' fill='#c8a44a'/>" +
        "<rect x='34' y='61' width='5' height='10' rx='1' fill='#c8a44a'/>" +
        "<rect x='42' y='61' width='5' height='10' rx='1' fill='#c8a44a'/>" +
        "<rect x='50' y='61' width='5' height='10' rx='1' fill='#c8a44a'/>" +
        "<text x='40' y='58' text-anchor='middle' font-family='monospace' font-size='5' fill='#5577dd' font-weight='bold'>MQ-135</text>" +
        "<circle cx='15' cy='15' r='4.5' fill='#ff4400' opacity='0.9'/>" +
        "<circle cx='15' cy='15' r='2' fill='#ffaa77'/>" +
        "<circle cx='65' cy='15' r='4.5' fill='#00cc44' opacity='0.8'/>" +
        "<circle cx='65' cy='15' r='2' fill='#aaffcc'/>" +
        "</svg>";

    /** Capteur humidite sol (capteur capacitif avec sondes) */
    private static final String SVG_HUMIDITE =
        "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 80 80'>" +
        "<defs>" +
        "<linearGradient id='pcbH' x1='0%' y1='0%' x2='100%' y2='100%'>" +
        "<stop offset='0%' style='stop-color:#4a1060'/>" +
        "<stop offset='100%' style='stop-color:#2d0840'/></linearGradient>" +
        "</defs>" +
        "<rect x='10' y='4' width='60' height='45' rx='4' fill='url(#pcbH)' stroke='#2d0840' stroke-width='1.5'/>" +
        "<rect x='22' y='10' width='36' height='24' rx='2' fill='#222' stroke='#444' stroke-width='1'/>" +
        "<text x='40' y='20' text-anchor='middle' font-family='monospace' font-size='5.5' fill='#ccc' font-weight='bold'>ESP32</text>" +
        "<text x='40' y='27' text-anchor='middle' font-family='monospace' font-size='4' fill='#888'>WiFi/BLE</text>" +
        "<text x='40' y='33' text-anchor='middle' font-family='monospace' font-size='3.5' fill='#666'>ForestGuard</text>" +
        "<rect x='24' y='6' width='4' height='4' rx='0.5' fill='#c8a44a'/>" +
        "<rect x='31' y='6' width='4' height='4' rx='0.5' fill='#c8a44a'/>" +
        "<rect x='38' y='6' width='4' height='4' rx='0.5' fill='#c8a44a'/>" +
        "<rect x='45' y='6' width='4' height='4' rx='0.5' fill='#c8a44a'/>" +
        "<rect x='52' y='6' width='4' height='4' rx='0.5' fill='#c8a44a'/>" +
        "<rect x='24' y='38' width='4' height='4' rx='0.5' fill='#c8a44a'/>" +
        "<rect x='31' y='38' width='4' height='4' rx='0.5' fill='#c8a44a'/>" +
        "<rect x='38' y='38' width='4' height='4' rx='0.5' fill='#c8a44a'/>" +
        "<rect x='45' y='38' width='4' height='4' rx='0.5' fill='#c8a44a'/>" +
        "<rect x='52' y='38' width='4' height='4' rx='0.5' fill='#c8a44a'/>" +
        "<rect cx='67' cy='18' r='4' fill='#00cc44'/>" +
        "<circle cx='67' cy='18' r='4' fill='#00cc44'/>" +
        "<circle cx='67' cy='18' r='2' fill='#aaffcc'/>" +
        "<circle cx='67' cy='30' r='4' fill='#ffcc00'/>" +
        "<circle cx='67' cy='30' r='2' fill='#ffee99'/>" +
        "<rect x='12' y='49' width='8' height='31' rx='4' fill='#c0c0c0' stroke='#999' stroke-width='1'/>" +
        "<rect x='24' y='49' width='8' height='31' rx='4' fill='#c0c0c0' stroke='#999' stroke-width='1'/>" +
        "<rect x='36' y='49' width='8' height='31' rx='4' fill='#c0c0c0' stroke='#999' stroke-width='1'/>" +
        "<rect x='48' y='49' width='8' height='31' rx='4' fill='#c0c0c0' stroke='#999' stroke-width='1'/>" +
        "<rect x='60' y='49' width='8' height='31' rx='4' fill='#c0c0c0' stroke='#999' stroke-width='1'/>" +
        "<line x1='12' y1='60' x2='68' y2='60' stroke='#aaa' stroke-width='0.5' opacity='0.4'/>" +
        "</svg>";

    /** Capteur generique IoT (boitier etanche IP67) */
    private static final String SVG_GENERIQUE =
        "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 80 80'>" +
        "<defs>" +
        "<linearGradient id='boite' x1='0%' y1='0%' x2='100%' y2='100%'>" +
        "<stop offset='0%' style='stop-color:#444'/>" +
        "<stop offset='100%' style='stop-color:#1a1a1a'/></linearGradient>" +
        "<linearGradient id='face' x1='0%' y1='0%' x2='0%' y2='100%'>" +
        "<stop offset='0%' style='stop-color:#555'/>" +
        "<stop offset='100%' style='stop-color:#2a2a2a'/></linearGradient>" +
        "</defs>" +
        "<rect x='10' y='8' width='60' height='64' rx='8' fill='url(#boite)' stroke='#222' stroke-width='2'/>" +
        "<rect x='14' y='12' width='52' height='56' rx='6' fill='url(#face)'/>" +
        "<rect x='18' y='16' width='44' height='26' rx='3' fill='#111'/>" +
        "<rect x='20' y='18' width='40' height='22' rx='2' fill='#001a00'/>" +
        "<text x='40' y='27' text-anchor='middle' font-family='monospace' font-size='6' fill='#00ff44'>23.4</text>" +
        "<text x='40' y='34' text-anchor='middle' font-family='monospace' font-size='4' fill='#00aa22'>TEMPERATURE C</text>" +
        "<line x1='20' y1='46' x2='60' y2='46' stroke='#333' stroke-width='0.5'/>" +
        "<circle cx='25' cy='55' r='5' fill='#222' stroke='#444' stroke-width='1'/>" +
        "<circle cx='25' cy='55' r='3' fill='#00cc44' opacity='0.85'/>" +
        "<circle cx='25' cy='55' r='1.5' fill='#aaffcc'/>" +
        "<circle cx='40' cy='55' r='5' fill='#222' stroke='#444' stroke-width='1'/>" +
        "<circle cx='40' cy='55' r='3' fill='#ffaa00' opacity='0.85'/>" +
        "<circle cx='40' cy='55' r='1.5' fill='#ffeeaa'/>" +
        "<circle cx='55' cy='55' r='5' fill='#222' stroke='#444' stroke-width='1'/>" +
        "<circle cx='55' cy='55' r='3' fill='#3366ff' opacity='0.85'/>" +
        "<circle cx='55' cy='55' r='1.5' fill='#aaccff'/>" +
        "<rect x='30' y='63' width='20' height='5' rx='2' fill='#333'/>" +
        "<rect x='16' y='37' width='6' height='10' rx='1' fill='#444' stroke='#555' stroke-width='0.5'/>" +
        "<rect x='58' y='37' width='6' height='10' rx='1' fill='#444' stroke='#555' stroke-width='0.5'/>" +
        "<text x='40' y='72' text-anchor='middle' font-family='Arial' font-size='4' fill='#555'>IP67 IoT</text>" +
        "</svg>";

    // ── Coordonnees GPS Tunisie ────────────────────────────────────────────
    private static final Map<String, double[]> COORDS = new HashMap<>();
    static {
        COORDS.put("ain draham",  new double[]{36.7823, 8.6897});
        COORDS.put("jendouba",    new double[]{36.5012, 8.7809});
        COORDS.put("tabarka",     new double[]{36.9543, 8.7567});
        COORDS.put("bizerte",     new double[]{37.2744, 9.8739});
        COORDS.put("el kef",      new double[]{36.1825, 8.7147});
        COORDS.put("siliana",     new double[]{36.0844, 9.3700});
        COORDS.put("beja",        new double[]{36.7256, 9.1817});
        COORDS.put("kasserine",   new double[]{35.1676, 8.8365});
        COORDS.put("zaghouan",    new double[]{36.4027, 10.1434});
        COORDS.put("nabeul",      new double[]{36.4561, 10.7376});
        COORDS.put("sousse",      new double[]{35.8245, 10.6346});
        COORDS.put("tunis",       new double[]{36.8190, 10.1657});
        COORDS.put("carthage",    new double[]{36.8527, 10.3257});
        COORDS.put("hammamet",    new double[]{36.4003, 10.6138});
        COORDS.put("nefza",       new double[]{37.0300, 9.0500});
        COORDS.put("ichkeul",     new double[]{37.1500, 9.6800});
        COORDS.put("foret",       new double[]{36.7823, 8.6897});
        COORDS.put("nord",        new double[]{37.0000, 9.2000});
        COORDS.put("sud",         new double[]{33.8000, 9.0000});
        COORDS.put("centre",      new double[]{35.5000, 9.5000});
    }

    @FXML
    public void initialize() {
        if (mapWebView != null) chargerCarte();
    }

    public void chargerCarte() {
        WebEngine engine = mapWebView.getEngine();
        engine.setJavaScriptEnabled(true);

        if (lblStatutCarte != null)
            lblStatutCarte.setText("Chargement de la carte...");

        // Charger les données en arrière-plan pour ne pas bloquer l'UI
        Thread thread = new Thread(() -> {
            List<Capteur> capteurs = capteurService.afficher();
            String html = genererHTML(capteurs);

            Platform.runLater(() -> {
                engine.getLoadWorker().stateProperty().addListener((obs, old, newState) -> {
                    if (newState == Worker.State.SUCCEEDED) {
                        Platform.runLater(() -> {
                            if (lblStatutCarte != null)
                                lblStatutCarte.setText("Carte chargée — " + capteurs.size() + " capteur(s) en BD | " + capteurs.size() + " affiché(s)");
                        });
                    }
                });
                engine.loadContent(html, "text/html");
            });
        });
        thread.setDaemon(true);
        thread.setName("ForestGuard-MapLoader");
        thread.start();
    }
    /**
     * Recharge uniquement les nouveaux capteurs ajoutés depuis le dernier chargement.
     * Appelé depuis CapteurController après un ajout.
     */
    public void rechargerNouveauxCapteurs() {
        List<Capteur> tousLesCapteurs = capteurService.afficher();

        // Récupérer les IDs déjà affichés via JS
        try {
            Object idsObj = mapWebView.getEngine().executeScript(
                "allMarkers.map(function(m){return m.id;}).join(',')");
            String idsStr = idsObj != null ? idsObj.toString() : "";

            java.util.Set<Integer> idsAffiches = new java.util.HashSet<>();
            if (!idsStr.isEmpty()) {
                for (String id : idsStr.split(",")) {
                    try { idsAffiches.add(Integer.parseInt(id.trim())); }
                    catch (NumberFormatException ignored) {}
                }
            }

            // Filtrer uniquement les nouveaux capteurs
            List<Capteur> nouveaux = tousLesCapteurs.stream()
                .filter(c -> !idsAffiches.contains(c.getId()))
                .collect(java.util.stream.Collectors.toList());

            if (nouveaux.isEmpty()) {
                if (lblStatutCarte != null)
                    lblStatutCarte.setText("Carte à jour — " + tousLesCapteurs.size() + " capteur(s)");
                return;
            }

            // Ajouter les nouveaux markers via JS sans recharger toute la carte
            StringBuilder jsAjout = new StringBuilder();
            edu.capteur.services.MesureCapteurService mesureService =
                new edu.capteur.services.MesureCapteurService();

            for (Capteur c : nouveaux) {
                Foret foret = foretService.getById(c.getForetId());
                double[] coords;
                if (foret != null && foret.getLatitude() != 0.0) {
                    // Offset simple pour les nouveaux capteurs ajoutés dynamiquement
                    double rayon = 0.012;
                    double angle = c.getId() * 2.399963;
                    double offsetLat = rayon * Math.cos(angle);
                    double offsetLng = rayon * Math.sin(angle) / Math.cos(Math.toRadians(foret.getLatitude()));
                    coords = new double[]{foret.getLatitude() + offsetLat, foret.getLongitude() + offsetLng};
                } else {
                    coords = trouverCoords(c.getLocalisation());
                }

                String nomForet = foret != null ? foret.getNom() : "Forêt inconnue";
                String locForet = foret != null ? foret.getLocalisation() : c.getLocalisation();
                String statut   = c.getStatut() != null ? c.getStatut() : "inactif";

                edu.capteur.services.MesureCapteurService.DerniereMesure mesure =
                    mesureService.getDerniereMesure(c.getId());

                String tempStr, humidStr, risqueStr, risqueColor;
                if (mesure.disponible) {
                    tempStr = String.format(java.util.Locale.US, "%.1f°C", mesure.temperature);
                    humidStr = mesure.humidite + "%";
                    int score = 0;
                    if (mesure.temperature > 55) score += 60;
                    else if (mesure.temperature > 40) score += (int)((mesure.temperature - 40) / 15.0 * 30);
                    if (mesure.humidite > 90) score += 45;
                    else if (mesure.humidite > 75) score += (int)((mesure.humidite - 75) / 15.0 * 25);
                    score = Math.min(100, score);
                    if (score >= 70)      { risqueStr = "CRITIQUE"; risqueColor = "#ef4444"; }
                    else if (score >= 50) { risqueStr = "ELEVE";    risqueColor = "#f59e0b"; }
                    else if (score >= 20) { risqueStr = "MODERE";   risqueColor = "#eab308"; }
                    else                  { risqueStr = "FAIBLE";   risqueColor = "#22c55e"; }
                } else {
                    tempStr = "N/A"; humidStr = "N/A"; risqueStr = "N/A"; risqueColor = "#94a3b8";
                }

                String borderColor, statusLabel;
                switch (statut) {
                    case "actif"    -> { borderColor = "#22c55e"; statusLabel = "ACTIF"; }
                    case "en_panne" -> { borderColor = "#ef4444"; statusLabel = "EN PANNE"; }
                    default         -> { borderColor = "#94a3b8"; statusLabel = "INACTIF"; }
                }

                String imgB64 = getImageBase64(c.getType(), c.getId());
                boolean isPanne = "en_panne".equals(statut);
                String markerHtml =
                    "<div style='width:28px;height:28px;border-radius:50%;border:2.5px solid " + borderColor +
                    ";box-shadow:0 2px 6px rgba(0,0,0,0.45);overflow:hidden;cursor:pointer;background:white;" +
                    (isPanne ? "animation:pulse 1.8s infinite;" : "") +
                    "'><img src='data:image/svg+xml;base64," + imgB64 +
                    "' width='28' height='28' style='display:block;'/></div>";

                // Prédiction
                edu.capteur.services.MeteoService.DonneesMeteo meteoReelle = mesure.disponible
                    ? new edu.capteur.services.MeteoService.DonneesMeteo(
                        mesure.temperature, mesure.temperature - 2, mesure.humidite, 10.0, "mesure BD", "🌡", 0, 0.0)
                    : new edu.capteur.services.MeteoService.DonneesMeteo();
                edu.capteur.services.OllamaService.ResultatIA ia =
                    new edu.capteur.services.OllamaService().analyseLocaleEnrichie(c, meteoReelle);
                String pred = ia.analyse.replaceAll("[\\p{So}\\p{Cn}]", "").replaceAll("\\s+", " ").trim();

                jsAjout.append("addMarker(")
                    .append(coords[0]).append(",").append(coords[1])
                    .append(",'").append(jsStr(c.getNom())).append("','").append(statut).append("','")
                    .append(statusLabel).append("','")
                    .append(jsStr(c.getType())).append("','").append(jsStr(locForet))
                    .append("','").append(jsStr(nomForet)).append("','")
                    .append(jsStr(tempStr)).append("','").append(jsStr(humidStr)).append("','")
                    .append(jsStr(risqueStr)).append("','").append(risqueColor).append("','")
                    .append(borderColor).append("','").append(jsStr(pred)).append("','")
                    .append(markerHtml.replace("'", "\\'")).append("',")
                    .append(c.getId()).append(",'").append(imgB64).append("');\n");
            }

            // Exécuter le JS pour ajouter les nouveaux markers
            final String jsCode = jsAjout.toString();
            Platform.runLater(() -> {
                mapWebView.getEngine().executeScript(jsCode);
                if (lblStatutCarte != null)
                    lblStatutCarte.setText("Carte mise à jour — " + tousLesCapteurs.size() +
                        " capteur(s) | +" + nouveaux.size() + " nouveau(x)");
            });

        } catch (Exception e) {
            // Si JS échoue (carte pas encore chargée), recharger complètement
            chargerCarte();
        }
    }

    /** Appelé depuis JavaFX pour filtrer les markers via JS */
    @FXML
    public void filtrerActifs() {
        mapWebView.getEngine().executeScript("filtrerStatut('actif')");
    }

    @FXML
    public void filtrerEnPanne() {
        mapWebView.getEngine().executeScript("filtrerStatut('en_panne')");
    }

    @FXML
    public void filtrerInactifs() {
        mapWebView.getEngine().executeScript("filtrerStatut('inactif')");
    }

    @FXML
    public void filtrerTous() {
        mapWebView.getEngine().executeScript("filtrerStatut('tous')");
    }

    /** Retourne l'image SVG base64 unique pour ce capteur (type + id) */
    private String getImageBase64(String type) {
        // Fallback sans ID — utilise les SVG statiques
        String svg;
        if (type == null) svg = SVG_GENERIQUE;
        else {
            String t = type.toLowerCase();
            if (t.contains("temp"))                                                  svg = SVG_TEMPERATURE;
            else if (t.contains("fum") || t.contains("gaz") || t.contains("smoke")) svg = SVG_FUMEE;
            else if (t.contains("hum") || t.contains("sol") || t.contains("eau"))   svg = SVG_HUMIDITE;
            else                                                                     svg = SVG_GENERIQUE;
        }
        return Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
    }

    /** Retourne l'image SVG base64 UNIQUE par capteur (type + capteurId) */
    private String getImageBase64(String type, int capteurId) {
        String svg = genererSVGCapteur(type, capteurId);
        return Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
    }

    // ── Génération SVG unique par capteur ────────────────────────────────

    /**
     * Génère un SVG unique pour chaque capteur selon son type et son ID.
     * Les couleurs, modèles et détails varient pour chaque capteur.
     */
    private String genererSVGCapteur(String type, int capteurId) {
        // Palettes de couleurs PCB selon l'ID (cycle sur 8 variantes)
        String[][] palettes = {
            {"#1a3d1a", "#2d6a2d", "#c8a44a"}, // vert foncé
            {"#0d1840", "#1a2a5c", "#5577dd"}, // bleu marine
            {"#2d0840", "#4a1060", "#cc44aa"}, // violet
            {"#3d1a00", "#6a3000", "#dd8833"}, // brun
            {"#003d3d", "#006060", "#44cccc"}, // teal
            {"#1a001a", "#3d003d", "#cc44cc"}, // magenta foncé
            {"#001a3d", "#003060", "#4488ff"}, // bleu royal
            {"#1a1a00", "#3d3d00", "#aaaa00"}, // olive
        };
        String[] pal = palettes[capteurId % palettes.length];
        String pcbDark = pal[0], pcbLight = pal[1], pinColor = pal[2];

        // Modèles de puces selon l'ID
        String[] modeles = {"SHT30","DHT22","BME280","DS18B20","HTU21","AM2302","SHT31","LM35",
                            "MQ-135","MQ-2","MQ-7","MQ-9","MQ-4","MQ-6","MQ-8","MQ-3",
                            "FC-28","YL-69","SEN0193","HL-69","VH400","SM-S","GS3","SKU114"};
        String modele = modeles[capteurId % modeles.length];

        // Couleur LED selon ID
        String[] ledColors = {"#00cc44","#ffcc00","#ff4400","#00aaff","#cc44ff","#ff8800","#44ffcc","#ff44aa"};
        String ledColor = ledColors[capteurId % ledColors.length];
        String ledGlow  = ledColor + "88";

        int chipVariant = capteurId % 3;
        String t = (type != null) ? type.toLowerCase() : "";

        if (t.contains("temp")) {
            return "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 80 80'>"
                + "<defs><linearGradient id='p" + capteurId + "' x1='0%' y1='0%' x2='100%' y2='100%'>"
                + "<stop offset='0%' style='stop-color:" + pcbLight + "'/>"
                + "<stop offset='100%' style='stop-color:" + pcbDark + "'/></linearGradient></defs>"
                + "<rect x='5' y='5' width='70' height='70' rx='6' fill='url(#p" + capteurId + ")' stroke='" + pcbDark + "' stroke-width='1.5'/>"
                + "<line x1='5' y1='" + (25 + capteurId % 10) + "' x2='75' y2='" + (25 + capteurId % 10) + "' stroke='" + pinColor + "' stroke-width='0.7' opacity='0.4'/>"
                + "<line x1='5' y1='" + (45 + capteurId % 8) + "' x2='75' y2='" + (45 + capteurId % 8) + "' stroke='" + pinColor + "' stroke-width='0.7' opacity='0.4'/>"
                + "<line x1='" + (20 + capteurId % 10) + "' y1='5' x2='" + (20 + capteurId % 10) + "' y2='75' stroke='" + pinColor + "' stroke-width='0.5' opacity='0.3'/>"
                + "<line x1='" + (55 + capteurId % 8) + "' y1='5' x2='" + (55 + capteurId % 8) + "' y2='75' stroke='" + pinColor + "' stroke-width='0.5' opacity='0.3'/>"
                + (chipVariant == 0
                    ? "<rect x='22' y='18' width='36' height='28' rx='2' fill='#1a1a1a' stroke='#555' stroke-width='1'/>"
                    : chipVariant == 1
                    ? "<rect x='18' y='20' width='44' height='24' rx='3' fill='#111' stroke='#444' stroke-width='1'/>"
                    : "<rect x='24' y='16' width='32' height='32' rx='4' fill='#222' stroke='#666' stroke-width='1'/>")
                + "<rect x='10' y='22' width='8' height='3' rx='1' fill='" + pinColor + "'/>"
                + "<rect x='10' y='29' width='8' height='3' rx='1' fill='" + pinColor + "'/>"
                + "<rect x='10' y='36' width='8' height='3' rx='1' fill='" + pinColor + "'/>"
                + "<rect x='10' y='43' width='8' height='3' rx='1' fill='" + pinColor + "'/>"
                + "<rect x='62' y='22' width='8' height='3' rx='1' fill='" + pinColor + "'/>"
                + "<rect x='62' y='29' width='8' height='3' rx='1' fill='" + pinColor + "'/>"
                + "<rect x='62' y='36' width='8' height='3' rx='1' fill='" + pinColor + "'/>"
                + "<rect x='62' y='43' width='8' height='3' rx='1' fill='" + pinColor + "'/>"
                + "<text x='40' y='30' text-anchor='middle' font-family='monospace' font-size='6' fill='#ddd' font-weight='bold'>" + modele + "</text>"
                + "<text x='40' y='38' text-anchor='middle' font-family='monospace' font-size='4' fill='#999'>TEMP</text>"
                + "<text x='40' y='44' text-anchor='middle' font-family='monospace' font-size='3.5' fill='#666'>C" + String.format("%03d", capteurId) + "</text>"
                + "<rect x='18' y='55' width='10' height='8' rx='1' fill='#334' stroke='#556' stroke-width='0.5'/>"
                + "<rect x='32' y='55' width='10' height='8' rx='1' fill='#443' stroke='#665' stroke-width='0.5'/>"
                + "<rect x='46' y='55' width='10' height='8' rx='1' fill='#344' stroke='#566' stroke-width='0.5'/>"
                + "<circle cx='65' cy='63' r='5' fill='" + ledColor + "' opacity='0.9'/>"
                + "<circle cx='65' cy='63' r='2.5' fill='" + ledGlow + "'/>"
                + "<circle cx='11' cy='11' r='3' fill='none' stroke='#556655' stroke-width='1.5'/>"
                + "<circle cx='69' cy='11' r='3' fill='none' stroke='#556655' stroke-width='1.5'/>"
                + "<circle cx='11' cy='69' r='3' fill='none' stroke='#556655' stroke-width='1.5'/>"
                + "<circle cx='69' cy='69' r='3' fill='none' stroke='#556655' stroke-width='1.5'/>"
                + "</svg>";

        } else if (t.contains("fum") || t.contains("gaz") || t.contains("smoke")) {
            String[][] domes = {
                {"#e8cc55","#aa8822","#664400"}, {"#cc4422","#882200","#441100"},
                {"#55aacc","#2277aa","#114466"}, {"#88cc44","#448822","#224411"},
                {"#cc88cc","#885588","#442244"}, {"#ccaa44","#886622","#443311"},
                {"#44ccaa","#228877","#114433"}, {"#cc6644","#884422","#442211"}
            };
            String[] dc = domes[capteurId % domes.length];
            int cy = 32 + capteurId % 6;
            return "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 80 80'>"
                + "<defs><linearGradient id='p" + capteurId + "' x1='0%' y1='0%' x2='100%' y2='100%'>"
                + "<stop offset='0%' style='stop-color:" + pcbLight + "'/>"
                + "<stop offset='100%' style='stop-color:" + pcbDark + "'/></linearGradient>"
                + "<radialGradient id='d" + capteurId + "' cx='40%' cy='35%' r='60%'>"
                + "<stop offset='0%' style='stop-color:" + dc[0] + "'/>"
                + "<stop offset='60%' style='stop-color:" + dc[1] + "'/>"
                + "<stop offset='100%' style='stop-color:" + dc[2] + "'/></radialGradient></defs>"
                + "<rect x='5' y='5' width='70' height='70' rx='6' fill='url(#p" + capteurId + ")' stroke='" + pcbDark + "' stroke-width='1.5'/>"
                + "<line x1='12' y1='40' x2='68' y2='40' stroke='" + pinColor + "' stroke-width='0.8' opacity='0.4'/>"
                + "<line x1='40' y1='12' x2='40' y2='68' stroke='" + pinColor + "' stroke-width='0.8' opacity='0.3'/>"
                + "<circle cx='40' cy='" + cy + "' r='19' fill='#554400' stroke='#332200' stroke-width='1.5'/>"
                + "<circle cx='40' cy='" + cy + "' r='16' fill='url(#d" + capteurId + ")'/>"
                + "<line x1='24' y1='" + cy + "' x2='56' y2='" + cy + "' stroke='#222' stroke-width='1' opacity='0.5'/>"
                + "<line x1='40' y1='" + (cy-16) + "' x2='40' y2='" + (cy+16) + "' stroke='#222' stroke-width='1' opacity='0.5'/>"
                + "<line x1='29' y1='" + (cy-11) + "' x2='51' y2='" + (cy+11) + "' stroke='#222' stroke-width='0.7' opacity='0.4'/>"
                + "<line x1='51' y1='" + (cy-11) + "' x2='29' y2='" + (cy+11) + "' stroke='#222' stroke-width='0.7' opacity='0.4'/>"
                + "<circle cx='40' cy='" + cy + "' r='4' fill='#111' opacity='0.8'/>"
                + "<rect x='" + (14 + capteurId % 4) + "' y='61' width='5' height='10' rx='1' fill='" + pinColor + "'/>"
                + "<rect x='" + (22 + capteurId % 4) + "' y='61' width='5' height='10' rx='1' fill='" + pinColor + "'/>"
                + "<rect x='" + (30 + capteurId % 4) + "' y='61' width='5' height='10' rx='1' fill='" + pinColor + "'/>"
                + "<rect x='" + (38 + capteurId % 4) + "' y='61' width='5' height='10' rx='1' fill='" + pinColor + "'/>"
                + "<rect x='" + (46 + capteurId % 4) + "' y='61' width='5' height='10' rx='1' fill='" + pinColor + "'/>"
                + "<text x='40' y='58' text-anchor='middle' font-family='monospace' font-size='5' fill='" + pinColor + "' font-weight='bold'>" + modele + "</text>"
                + "<circle cx='14' cy='14' r='4' fill='" + ledColor + "' opacity='0.9'/>"
                + "<circle cx='14' cy='14' r='2' fill='" + ledGlow + "'/>"
                + "<circle cx='66' cy='14' r='4' fill='#00cc44' opacity='0.8'/>"
                + "<circle cx='66' cy='14' r='2' fill='#aaffcc'/>"
                + "</svg>";

        } else if (t.contains("hum") || t.contains("sol") || t.contains("eau")) {
            String[] probeColors = {"#c0c0c0","#d4af37","#b87333","#aaaaaa","#e8e8e8","#ccaa44","#88aacc","#cc8844"};
            String probeColor = probeColors[capteurId % probeColors.length];
            int nSondes = 3 + (capteurId % 3);
            StringBuilder sondes = new StringBuilder();
            int spacing = 60 / (nSondes + 1);
            for (int i = 0; i < nSondes; i++) {
                int x = 10 + spacing * (i + 1);
                int h = 25 + (capteurId * (i + 1)) % 12;
                sondes.append("<rect x='").append(x).append("' y='49' width='8' height='").append(h)
                      .append("' rx='4' fill='").append(probeColor).append("' stroke='#888' stroke-width='0.8'/>");
            }
            return "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 80 80'>"
                + "<defs><linearGradient id='p" + capteurId + "' x1='0%' y1='0%' x2='100%' y2='100%'>"
                + "<stop offset='0%' style='stop-color:" + pcbLight + "'/>"
                + "<stop offset='100%' style='stop-color:" + pcbDark + "'/></linearGradient></defs>"
                + "<rect x='5' y='4' width='70' height='45' rx='4' fill='url(#p" + capteurId + ")' stroke='" + pcbDark + "' stroke-width='1.5'/>"
                + "<rect x='18' y='9' width='44' height='26' rx='2' fill='#1a1a1a' stroke='#444' stroke-width='1'/>"
                + "<text x='40' y='19' text-anchor='middle' font-family='monospace' font-size='5.5' fill='#ddd' font-weight='bold'>" + modele + "</text>"
                + "<text x='40' y='26' text-anchor='middle' font-family='monospace' font-size='4' fill='#888'>HUMIDITY</text>"
                + "<text x='40' y='32' text-anchor='middle' font-family='monospace' font-size='3.5' fill='#666'>C" + String.format("%03d", capteurId) + "</text>"
                + "<rect x='22' y='5' width='4' height='4' rx='0.5' fill='" + pinColor + "'/>"
                + "<rect x='30' y='5' width='4' height='4' rx='0.5' fill='" + pinColor + "'/>"
                + "<rect x='38' y='5' width='4' height='4' rx='0.5' fill='" + pinColor + "'/>"
                + "<rect x='46' y='5' width='4' height='4' rx='0.5' fill='" + pinColor + "'/>"
                + "<rect x='54' y='5' width='4' height='4' rx='0.5' fill='" + pinColor + "'/>"
                + "<circle cx='66' cy='17' r='4' fill='" + ledColor + "' opacity='0.9'/>"
                + "<circle cx='66' cy='17' r='2' fill='" + ledGlow + "'/>"
                + "<circle cx='66' cy='29' r='4' fill='#ffcc00' opacity='0.8'/>"
                + "<circle cx='66' cy='29' r='2' fill='#ffee99'/>"
                + sondes
                + "<line x1='5' y1='60' x2='75' y2='60' stroke='" + probeColor + "' stroke-width='0.5' opacity='0.3'/>"
                + "</svg>";

        } else {
            String[] screenBgs = {"#001a00","#00001a","#1a0000","#001a1a","#1a001a","#0a0a00","#000a1a","#0a001a"};
            String[] textColors = {"#00ff44","#4488ff","#ff4444","#44ffff","#ff44ff","#ffff44","#ff8800","#88ff00"};
            String screenBg = screenBgs[capteurId % screenBgs.length];
            String textColor = textColors[capteurId % textColors.length];
            return "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 80 80'>"
                + "<defs><linearGradient id='p" + capteurId + "' x1='0%' y1='0%' x2='100%' y2='100%'>"
                + "<stop offset='0%' style='stop-color:#555'/>"
                + "<stop offset='100%' style='stop-color:#1a1a1a'/></linearGradient></defs>"
                + "<rect x='8' y='6' width='64' height='68' rx='8' fill='url(#p" + capteurId + ")' stroke='#222' stroke-width='2'/>"
                + "<rect x='12' y='10' width='56' height='60' rx='6' fill='#2a2a2a'/>"
                + "<rect x='16' y='14' width='48' height='28' rx='3' fill='#111'/>"
                + "<rect x='18' y='16' width='44' height='24' rx='2' fill='" + screenBg + "'/>"
                + "<text x='40' y='26' text-anchor='middle' font-family='monospace' font-size='6' fill='" + textColor + "' font-weight='bold'>" + modele + "</text>"
                + "<text x='40' y='34' text-anchor='middle' font-family='monospace' font-size='4' fill='" + textColor + "88'>C" + String.format("%03d", capteurId) + "</text>"
                + "<circle cx='24' cy='54' r='5' fill='#333' stroke='#555' stroke-width='1'/>"
                + "<circle cx='24' cy='54' r='3' fill='" + ledColor + "' opacity='0.85'/>"
                + "<circle cx='40' cy='54' r='5' fill='#333' stroke='#555' stroke-width='1'/>"
                + "<circle cx='40' cy='54' r='3' fill='#ffaa00' opacity='0.85'/>"
                + "<circle cx='56' cy='54' r='5' fill='#333' stroke='#555' stroke-width='1'/>"
                + "<circle cx='56' cy='54' r='3' fill='#3366ff' opacity='0.85'/>"
                + "<rect x='28' y='64' width='24' height='5' rx='2' fill='#444'/>"
                + "<rect x='14' y='38' width='6' height='10' rx='1' fill='#444' stroke='#555' stroke-width='0.5'/>"
                + "<rect x='60' y='38' width='6' height='10' rx='1' fill='#444' stroke='#555' stroke-width='0.5'/>"
                + "</svg>";
        }
    }

    /**
     * Construit la ligne JS addMarker(...) pour un capteur.
     * Méthode centralisée utilisée par genererHTML ET rechargerNouveauxCapteurs.
     */
    private String construireMarkerJS(Capteur c, Foret foret,
                                      edu.capteur.services.MesureCapteurService.DerniereMesure mesure,
                                      double[] coords, int[] counts) {

        String nomForet = foret != null ? foret.getNom() : "Forêt inconnue";
        String statut   = c.getStatut() != null ? c.getStatut() : "inactif";

        // ── Temp + humidité + risque ──
        String tempStr, humidStr, risqueStr, risqueColor;
        if (mesure.disponible) {
            tempStr  = String.format(java.util.Locale.US, "%.1f°C", mesure.temperature);
            humidStr = mesure.humidite + "%";
            int score = 0;
            if (mesure.temperature > 55) score += 60;
            else if (mesure.temperature > 40)
                score += (int)((mesure.temperature - 40) / 15.0 * 30);
            if (mesure.humidite > 90) score += 45;
            else if (mesure.humidite > 75)
                score += (int)((mesure.humidite - 75) / 15.0 * 25);
            score = Math.min(100, score);
            if (score >= 70)      { risqueStr = "CRITIQUE"; risqueColor = "#ef4444"; }
            else if (score >= 50) { risqueStr = "ELEVE";    risqueColor = "#f59e0b"; }
            else if (score >= 20) { risqueStr = "MODERE";   risqueColor = "#eab308"; }
            else                  { risqueStr = "FAIBLE";   risqueColor = "#22c55e"; }
        } else {
            tempStr = "N/A"; humidStr = "N/A"; risqueStr = "N/A"; risqueColor = "#94a3b8";
        }

        // ── Prédiction basée sur les mesures réelles ──
        String pred;
        if (mesure.disponible) {
            edu.capteur.services.MeteoService.DonneesMeteo meteoReelle =
                new edu.capteur.services.MeteoService.DonneesMeteo(
                    mesure.temperature, mesure.temperature - 2, mesure.humidite,
                    10.0, "mesure BD", "🌡", 0, 0.0);
            edu.capteur.services.OllamaService.ResultatIA ia =
                new edu.capteur.services.OllamaService().analyseLocaleEnrichie(c, meteoReelle);
            pred = ia.analyse;
        } else {
            pred = aiService.predictionLocale(c, 0);
        }
        pred = pred.replaceAll("[\\p{So}\\p{Cn}]", "").replaceAll("\\s+", " ").trim();

        // ── Couleurs statut ──
        String borderColor, statusLabel;
        switch (statut) {
            case "actif"    -> { borderColor = "#22c55e"; statusLabel = "ACTIF";
                                 if (counts != null) counts[0]++; }
            case "en_panne" -> { borderColor = "#ef4444"; statusLabel = "EN PANNE";
                                 if (counts != null) counts[1]++; }
            default         -> { borderColor = "#94a3b8"; statusLabel = "INACTIF";
                                 if (counts != null) counts[2]++; }
        }

        String imgB64   = getImageBase64(c.getType(), c.getId());
        boolean isPanne = "en_panne".equals(statut);

        String markerHtml =
            "<div style='width:28px;height:28px;border-radius:50%;" +
            "border:2.5px solid " + borderColor + ";" +
            "box-shadow:0 2px 6px rgba(0,0,0,0.45);" +
            "overflow:hidden;cursor:pointer;background:white;" +
            (isPanne ? "animation:pulse 1.8s infinite;" : "") +
            "'><img src='data:image/svg+xml;base64," + imgB64 +
            "' width='28' height='28' style='display:block;'/></div>";

        String locForet = foret != null ? foret.getLocalisation() : c.getLocalisation();

        return "addMarker("
            + coords[0] + "," + coords[1]
            + ",'" + jsStr(c.getNom())           + "','" + statut       + "','"
            + statusLabel                         + "','" + jsStr(c.getType()) + "','"
            + jsStr(locForet)                     + "','" + jsStr(nomForet)    + "','"
            + jsStr(tempStr)                      + "','" + jsStr(humidStr)    + "','"
            + jsStr(risqueStr)                    + "','" + risqueColor         + "','"
            + borderColor                         + "','" + jsStr(pred)         + "','"
            + markerHtml.replace("'", "\\'")      + "',"  + c.getId()
            + ",'" + imgB64 + "');\n";
    }

    private String genererHTML(List<Capteur> capteurs) {
        StringBuilder markersData = new StringBuilder();
        int[] counts = {0, 0, 0};

        // ── Chargement batch : 1 requête SQL pour toutes les forêts ──
        java.util.Map<Integer, Foret> foretsMap = foretService.getToutesForetsMap();

        // ── Chargement batch : 1 requête SQL pour toutes les mesures ──
        edu.capteur.services.MesureCapteurService mesureService =
            new edu.capteur.services.MesureCapteurService();
        java.util.Map<Integer, edu.capteur.services.MesureCapteurService.DerniereMesure> mesuresMap =
            mesureService.getToutesDernieresMesures();

        // ── Ollama vérifié UNE SEULE FOIS ──
        edu.capteur.services.OllamaService ollamaService = new edu.capteur.services.OllamaService();

        // ── Compteur par forêt pour placement en spirale ──
        java.util.Map<Integer, Integer> indexParForet = new java.util.HashMap<>();

        // ── Réduire les doublons : 1 seul capteur par (foret_id + localisation) ──
        // Si plusieurs capteurs ont la même forêt ET la même localisation,
        // on garde uniquement le premier. Chaque capteur conserve ses propres données.
        java.util.Set<String> cleesVues = new java.util.LinkedHashSet<>();
        java.util.List<Capteur> capteursAffiches = new java.util.ArrayList<>();
        for (Capteur c : capteurs) {
            String cle = c.getForetId() + "|" + (c.getLocalisation() != null ? c.getLocalisation().trim().toLowerCase() : "");
            if (cleesVues.add(cle)) {
                capteursAffiches.add(c);
            }
        }

        for (Capteur c : capteursAffiches) {
            Foret foret = foretsMap.get(c.getForetId());

            // ── Si foret_id invalide, trouver la forêt la plus proche par localisation ──
            if (foret == null) {
                String loc = c.getLocalisation() != null ? c.getLocalisation().toLowerCase() : "";
                for (Foret f : foretsMap.values()) {
                    String fNom = f.getNom().toLowerCase().replace("forêt de ", "").replace("foret de ", "");
                    String fLoc = f.getLocalisation().toLowerCase();
                    if (loc.contains(fLoc) || fLoc.contains(loc) || loc.contains(fNom)) {
                        foret = f;
                        break;
                    }
                }
                if (foret == null && !foretsMap.isEmpty()) {
                    foret = foretsMap.values().iterator().next();
                }
            }
            double[] coords;
            if (foret != null && (foret.getLatitude() != 0.0 || foret.getLongitude() != 0.0)) {
                int foretId = foret.getId(); // utiliser l'id réel de la forêt trouvée
                int idx = indexParForet.getOrDefault(foretId, 0);
                indexParForet.put(foretId, idx + 1);

                double offsetLat, offsetLng;
                if (idx == 0) {
                    offsetLat = 0.0;
                    offsetLng = 0.0;
                } else {
                    // Spirale golden angle : chaque capteur bien séparé
                    double rayon = 0.025 * (1 + idx / 6.0);
                    double angle = idx * 2.399963;
                    offsetLat = rayon * Math.cos(angle);
                    offsetLng = rayon * Math.sin(angle) / Math.cos(Math.toRadians(foret.getLatitude()));
                }
                coords = new double[]{foret.getLatitude() + offsetLat, foret.getLongitude() + offsetLng};
            } else {
                coords = trouverCoords(c.getLocalisation());
            }

            String nomForet = foret != null ? foret.getNom() : "Forêt inconnue";
            String statut   = c.getStatut() != null ? c.getStatut() : "inactif";

            // ── Mesure depuis le cache batch ──
            edu.capteur.services.MesureCapteurService.DerniereMesure mesure =
                mesuresMap.getOrDefault(c.getId(),
                    new edu.capteur.services.MesureCapteurService.DerniereMesure());

            String tempStr, humidStr, risqueStr, risqueColor;
            if (mesure.disponible) {
                tempStr   = String.format(java.util.Locale.US, "%.1f°C", mesure.temperature);
                humidStr  = mesure.humidite + "%";
                int score = 0;
                if (mesure.temperature > 55) score += 60;
                else if (mesure.temperature > 40) score += (int)((mesure.temperature - 40) / 15.0 * 30);
                if (mesure.humidite > 90) score += 45;
                else if (mesure.humidite > 75) score += (int)((mesure.humidite - 75) / 15.0 * 25);
                score = Math.min(100, score);
                if (score >= 70)      { risqueStr = "CRITIQUE";  risqueColor = "#ef4444"; }
                else if (score >= 50) { risqueStr = "ELEVE";     risqueColor = "#f59e0b"; }
                else if (score >= 20) { risqueStr = "MODERE";    risqueColor = "#eab308"; }
                else                  { risqueStr = "FAIBLE";    risqueColor = "#22c55e"; }
            } else {
                tempStr   = "N/A";
                humidStr  = "N/A";
                risqueStr = "N/A";
                risqueColor = "#94a3b8";
            }

            // ── Prédiction locale (pas d'appel HTTP Ollama ici) ──
            String pred;
            if (mesure.disponible) {
                edu.capteur.services.MeteoService.DonneesMeteo meteoReelle =
                    new edu.capteur.services.MeteoService.DonneesMeteo(
                        mesure.temperature, mesure.temperature - 2, mesure.humidite,
                        10.0, "mesure BD", "🌡", 0, 0.0);
                edu.capteur.services.OllamaService.ResultatIA ia =
                    ollamaService.analyseLocaleEnrichie(c, meteoReelle);
                pred = ia.analyse;
            } else {
                pred = aiService.predictionLocale(c, 0);
            }
            pred = pred.replaceAll("[\\p{So}\\p{Cn}]", "").replaceAll("\\s+", " ").trim();

            String borderColor, statusLabel;
            switch (statut) {
                case "actif"    -> { borderColor="#22c55e"; statusLabel="ACTIF";    counts[0]++; }
                case "en_panne" -> { borderColor="#ef4444"; statusLabel="EN PANNE"; counts[1]++; }
                default         -> { borderColor="#94a3b8"; statusLabel="INACTIF";  counts[2]++; }
            }

            String imgB64  = getImageBase64(c.getType(), c.getId());
            String nom     = jsStr(c.getNom());
            String type    = jsStr(c.getType());
            String loc     = jsStr(foret != null ? foret.getLocalisation() : c.getLocalisation());
            String foretJs = jsStr(nomForet);
            String predJs  = jsStr(pred);
            boolean isPanne = "en_panne".equals(statut);

            String markerHtml =
                "<div style='" +
                "width:28px;height:28px;border-radius:50%;" +
                "border:2.5px solid " + borderColor + ";" +
                "box-shadow:0 2px 6px rgba(0,0,0,0.45);" +
                "overflow:hidden;cursor:pointer;background:white;" +
                (isPanne ? "animation:pulse 1.8s infinite;" : "") +
                "'><img src='data:image/svg+xml;base64," + imgB64 +
                "' width='28' height='28' style='display:block;'/></div>";

            markersData.append("addMarker(")
                .append(coords[0]).append(",").append(coords[1])
                .append(",'").append(nom).append("','").append(statut).append("','")
                .append(statusLabel).append("','")
                .append(type).append("','").append(loc).append("','").append(foretJs).append("','")
                .append(jsStr(tempStr)).append("','").append(jsStr(humidStr)).append("','")
                .append(jsStr(risqueStr)).append("','").append(risqueColor).append("','")
                .append(borderColor).append("','").append(predJs).append("','")
                .append(markerHtml.replace("'", "\\'")).append("',")
                .append(c.getId()).append(",'").append(imgB64).append("');\n");
        }

        return "<!DOCTYPE html>\n<html>\n<head>\n"
            + "<meta charset='utf-8'>\n"
            + "<title>ForestGuard Map</title>\n"
            + "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>\n"
            + "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>\n"
            + "<style>\n"
            + "html,body{margin:0;padding:0;width:100%;height:100%;font-family:Arial,Helvetica,sans-serif}\n"
            + "#map{position:absolute;top:62px;left:0;right:0;bottom:0}\n"
            // Topbar
            + "#topbar{position:absolute;top:0;left:0;right:0;height:62px;"
            + "  background:rgba(10,20,13,0.97);border-bottom:2px solid #16a34a;"
            + "  display:flex;align-items:center;padding:0 16px;gap:12px;z-index:1000;"
            + "  box-shadow:0 2px 12px rgba(0,0,0,0.5)}\n"
            + "#topbar h1{color:white;font-size:13px;font-weight:bold;white-space:nowrap;"
            + "  letter-spacing:0.3px;margin-right:6px}\n"
            // Boutons filtre
            + ".filter-group{display:flex;gap:6px;align-items:center}\n"
            + ".filter-sep{width:1px;height:28px;background:rgba(255,255,255,0.15);margin:0 4px}\n"
            + ".fbtn{font-size:11px;font-weight:bold;padding:5px 12px;border-radius:20px;"
            + "  border:1.5px solid transparent;cursor:pointer;transition:all 0.18s;white-space:nowrap}\n"
            + ".fbtn-all{background:rgba(255,255,255,0.12);color:#e2e8f0;border-color:rgba(255,255,255,0.25)}\n"
            + ".fbtn-all:hover,.fbtn-all.active{background:#334155;color:white;border-color:#94a3b8}\n"
            + ".fbtn-green{background:rgba(34,197,94,0.15);color:#4ade80;border-color:rgba(34,197,94,0.4)}\n"
            + ".fbtn-green:hover,.fbtn-green.active{background:#16a34a;color:white;border-color:#16a34a}\n"
            + ".fbtn-red{background:rgba(239,68,68,0.15);color:#fca5a5;border-color:rgba(239,68,68,0.4)}\n"
            + ".fbtn-red:hover,.fbtn-red.active{background:#dc2626;color:white;border-color:#dc2626}\n"
            + ".fbtn-gray{background:rgba(148,163,184,0.15);color:#cbd5e1;border-color:rgba(148,163,184,0.35)}\n"
            + ".fbtn-gray:hover,.fbtn-gray.active{background:#475569;color:white;border-color:#475569}\n"
            // Stats badges
            + ".stats{display:flex;gap:7px;margin-left:auto}\n"
            + ".st{font-size:11px;font-weight:bold;padding:4px 11px;border-radius:20px}\n"
            + ".st-green{background:#dcfce7;color:#15803d}\n"
            + ".st-red{background:#fee2e2;color:#dc2626}\n"
            + ".st-gray{background:#f1f5f9;color:#475569}\n"
            // Popup
            + ".leaflet-popup-content-wrapper{border-radius:14px;border:none;"
            + "  box-shadow:0 12px 32px rgba(0,0,0,0.22);padding:0;overflow:hidden}\n"
            + ".leaflet-popup-content{margin:0;min-width:260px}\n"
            + ".leaflet-popup-tip-container{display:none}\n"
            + ".popup-hdr{padding:13px 15px 11px;color:white;display:flex;align-items:center;gap:11px}\n"
            + ".popup-img{width:46px;height:46px;border-radius:8px;overflow:hidden;flex-shrink:0;"
            + "  border:2px solid rgba(255,255,255,0.3);background:white}\n"
            + ".popup-img img{width:46px;height:46px;display:block}\n"
            + ".popup-nom{font-weight:bold;font-size:13px;margin-bottom:3px;line-height:1.2}\n"
            + ".popup-badge{display:inline-block;font-size:10px;font-weight:bold;"
            + "  padding:2px 8px;border-radius:20px;background:rgba(255,255,255,0.25)}\n"
            + ".popup-body{padding:11px 15px 13px;background:white}\n"
            + ".popup-row{display:flex;align-items:center;justify-content:space-between;"
            + "  font-size:12px;padding:4px 0;border-bottom:1px solid #f1f5f9;color:#374151}\n"
            + ".popup-row:last-child{border:none}\n"
            + ".popup-lbl{color:#9ca3af;font-size:11px;min-width:76px}\n"
            + ".popup-val{font-weight:600;color:#1e293b;text-align:right}\n"
            + ".popup-mesure{display:flex;gap:8px;margin-top:8px}\n"
            + ".mesure-card{flex:1;background:#f8fafc;border-radius:8px;padding:7px 10px;"
            + "  text-align:center;border:1px solid #e2e8f0}\n"
            + ".mesure-val{font-size:15px;font-weight:bold;color:#0f172a}\n"
            + ".mesure-lbl{font-size:9px;color:#94a3b8;font-weight:bold;letter-spacing:1px;margin-top:2px}\n"
            + ".risque-badge{display:inline-block;font-size:10px;font-weight:bold;"
            + "  padding:3px 10px;border-radius:20px;margin-top:6px}\n"
            + ".popup-pred{background:#f0fdf4;border-left:3px solid #16a34a;padding:7px 9px;"
            + "  font-size:11px;color:#166534;margin-top:9px;border-radius:0 6px 6px 0;"
            + "  line-height:1.5;font-style:italic}\n"
            // Legende
            + ".map-legend{background:white;border-radius:10px;padding:9px 13px;"
            + "  box-shadow:0 2px 12px rgba(0,0,0,0.15);font-size:12px}\n"
            + ".map-legend b{font-size:12px;display:block;margin-bottom:5px;color:#1e293b}\n"
            + ".leg-row{display:flex;align-items:center;gap:7px;margin-bottom:3px;color:#374151}\n"
            + ".leg-img{width:20px;height:20px;border-radius:4px;overflow:hidden;border:1px solid #e2e8f0}\n"
            + ".leg-img img{width:20px;height:20px;display:block}\n"
            + ".leg-dot{width:10px;height:10px;border-radius:50%;flex-shrink:0}\n"
            // Pulse
            + "@keyframes pulse{"
            + "  0%,100%{box-shadow:0 2px 6px rgba(0,0,0,0.45),0 0 0 0 rgba(239,68,68,0.7)}"
            + "  50%{box-shadow:0 2px 6px rgba(0,0,0,0.45),0 0 0 8px rgba(239,68,68,0)}}\n"
            + "</style>\n</head><body>\n"
            // Topbar avec filtres
            + "<div id='topbar'>\n"
            + "  <h1>ForestGuard</h1>\n"
            + "  <div class='filter-sep'></div>\n"
            + "  <div class='filter-group'>\n"
            + "    <button class='fbtn fbtn-all active' id='btn-tous' onclick='filtrerStatut(\"tous\")'>Tous (" + (counts[0]+counts[1]+counts[2]) + ")</button>\n"
            + "    <button class='fbtn fbtn-green' id='btn-actif' onclick='filtrerStatut(\"actif\")'>Actifs (" + counts[0] + ")</button>\n"
            + "    <button class='fbtn fbtn-red' id='btn-en_panne' onclick='filtrerStatut(\"en_panne\")'>En panne (" + counts[1] + ")</button>\n"
            + "    <button class='fbtn fbtn-gray' id='btn-inactif' onclick='filtrerStatut(\"inactif\")'>Inactifs (" + counts[2] + ")</button>\n"
            + "  </div>\n"
            + "  <div class='stats'>\n"
            + "    <span class='st st-green'>" + counts[0] + " Actifs</span>\n"
            + "    <span class='st st-red'>" + counts[1] + " En panne</span>\n"
            + "    <span class='st st-gray'>" + counts[2] + " Inactifs</span>\n"
            + "  </div>\n"
            + "</div>\n"
            + "<div id='map'></div>\n"
            + "<script>\n"
            + "var map = L.map('map',{zoomControl:true}).setView([36.5,9.2],7);\n"
            + "L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{\n"
            + "  attribution:'&copy; <a href=\"https://openstreetmap.org\">OpenStreetMap</a> | ForestGuard',\n"
            + "  maxZoom:18\n"
            + "}).addTo(map);\n"
            + buildLegendJS()
            // Tableau global des markers pour le filtrage
            + "var allMarkers=[];\n"
            + "var filtreActif='tous';\n"
            // Fonction filtrage
            + "function filtrerStatut(statut){\n"
            + "  filtreActif=statut;\n"
            + "  document.querySelectorAll('.fbtn').forEach(function(b){b.classList.remove('active')});\n"
            + "  var bid=document.getElementById('btn-'+statut);\n"
            + "  if(bid) bid.classList.add('active');\n"
            + "  allMarkers.forEach(function(m){\n"
            + "    if(statut==='tous'||m.statut===statut){\n"
            + "      if(!map.hasLayer(m.marker)) m.marker.addTo(map);\n"
            + "    } else {\n"
            + "      if(map.hasLayer(m.marker)) map.removeLayer(m.marker);\n"
            + "    }\n"
            + "  });\n"
            + "}\n"
            // Fonction addMarker avec statut stocke
            + "function addMarker(lat,lng,nom,statut,statutLabel,type,loc,foret,temp,humid,risque,risqueColor,borderColor,pred,markerHtml,capteurId,imgB64){\n"
            + "  var ico=L.divIcon({className:'',html:markerHtml,\n"
            + "    iconSize:[28,28],iconAnchor:[14,14],popupAnchor:[0,-18]});\n"
            + "  var hasMesure=(temp!='N/A');\n"
            // Couleur de fond du badge risque
            + "  var risqueBg=risqueColor+'22';\n"
            + "  var popup='<div class=\"popup-hdr\" style=\"background:'+borderColor+'\">'\n"
            + "    +'<div class=\"popup-img\"><img src=\"data:image/svg+xml;base64,'+imgB64+'\" alt=\"capteur\"/></div>'\n"
            + "    +'<div><div class=\"popup-nom\">'+nom+'</div>'\n"
            + "    +'<span class=\"popup-badge\">'+statutLabel+'</span></div></div>'\n"
            + "    +'<div class=\"popup-body\">'\n"
            + "    +'<div class=\"popup-row\"><span class=\"popup-lbl\">Type</span><span class=\"popup-val\">'+type+'</span></div>'\n"
            + "    +'<div class=\"popup-row\"><span class=\"popup-lbl\">For&ecirc;t</span><span class=\"popup-val\">'+foret+'</span></div>'\n"
            + "    +'<div class=\"popup-row\"><span class=\"popup-lbl\">Localisation</span><span class=\"popup-val\">'+loc+'</span></div>'\n"
            // Mesures temp + humidité en cartes
            + "    +(hasMesure?'<div class=\"popup-mesure\">'\n"
            + "      +'<div class=\"mesure-card\"><div class=\"mesure-val\">'+temp+'</div><div class=\"mesure-lbl\">TEMPERATURE</div></div>'\n"
            + "      +'<div class=\"mesure-card\"><div class=\"mesure-val\">'+humid+'</div><div class=\"mesure-lbl\">HUMIDITE</div></div>'\n"
            + "      +'<div class=\"mesure-card\"><div class=\"mesure-val\" style=\"color:'+risqueColor+'\">'+risque+'</div><div class=\"mesure-lbl\">RISQUE</div></div>'\n"
            + "      +'</div>':'')\n"
            + "    +(pred?'<div class=\"popup-pred\">'+pred+'</div>':'')\n"
            + "    +'</div>';\n"
            + "  var mk=L.marker([lat,lng],{icon:ico}).addTo(map)\n"
            + "    .bindPopup(popup,{maxWidth:300,closeButton:true})\n"
            + "    .bindTooltip('<b style=\"font-size:12px\">'+nom+'</b><br><span style=\"color:#666;font-size:11px\">'+type+' | '+foret+'</span>',\n"
            + "      {direction:'top',offset:[0,-6],className:'leaflet-tooltip'});\n"
            + "  allMarkers.push({marker:mk,statut:statut,id:capteurId});\n"
            + "}\n"
            + buildImgFunctionJS()
            + markersData.toString()
            + "</script>\n</body></html>";
    }

    /** Construit la legende avec les 3 types de capteurs */
    private String buildLegendJS() {
        String b64Temp = Base64.getEncoder().encodeToString(SVG_TEMPERATURE.getBytes(StandardCharsets.UTF_8));
        String b64Fum  = Base64.getEncoder().encodeToString(SVG_FUMEE.getBytes(StandardCharsets.UTF_8));
        String b64Hum  = Base64.getEncoder().encodeToString(SVG_HUMIDITE.getBytes(StandardCharsets.UTF_8));
        String b64Gen  = Base64.getEncoder().encodeToString(SVG_GENERIQUE.getBytes(StandardCharsets.UTF_8));

        return "var leg = L.control({position:'bottomright'});\n"
            + "leg.onAdd = function() {\n"
            + "  var d = L.DomUtil.create('div','map-legend');\n"
            + "  d.innerHTML = '<b>ForestGuard - Capteurs</b>'\n"
            + "    + '<div class=\"leg-row\"><div class=\"leg-dot\" style=\"background:#22c55e\"></div> Capteur actif</div>'\n"
            + "    + '<div class=\"leg-row\"><div class=\"leg-dot\" style=\"background:#ef4444\"></div> En panne</div>'\n"
            + "    + '<div class=\"leg-row\"><div class=\"leg-dot\" style=\"background:#94a3b8\"></div> Inactif</div>'\n"
            + "    + '<hr style=\"margin:6px 0;border-color:#f1f5f9\"/>'\n"
            + "    + '<div class=\"leg-row\"><div class=\"leg-img\"><img src=\"data:image/svg+xml;base64," + b64Temp + "\" width=24 height=24/></div> Temperature</div>'\n"
            + "    + '<div class=\"leg-row\"><div class=\"leg-img\"><img src=\"data:image/svg+xml;base64," + b64Fum  + "\" width=24 height=24/></div> Fumee/Gaz</div>'\n"
            + "    + '<div class=\"leg-row\"><div class=\"leg-img\"><img src=\"data:image/svg+xml;base64," + b64Hum  + "\" width=24 height=24/></div> Humidite</div>'\n"
            + "    + '<div class=\"leg-row\"><div class=\"leg-img\"><img src=\"data:image/svg+xml;base64," + b64Gen  + "\" width=24 height=24/></div> Autre</div>';\n"
            + "  return d;\n"
            + "}; leg.addTo(map);\n";
    }

    /** Fonction JS qui retourne le bon b64 selon le type */
    private String buildImgFunctionJS() {
        String b64Temp = Base64.getEncoder().encodeToString(SVG_TEMPERATURE.getBytes(StandardCharsets.UTF_8));
        String b64Fum  = Base64.getEncoder().encodeToString(SVG_FUMEE.getBytes(StandardCharsets.UTF_8));
        String b64Hum  = Base64.getEncoder().encodeToString(SVG_HUMIDITE.getBytes(StandardCharsets.UTF_8));
        String b64Gen  = Base64.getEncoder().encodeToString(SVG_GENERIQUE.getBytes(StandardCharsets.UTF_8));

        return "function getImg(type){\n"
            + "  var t=(type||'').toLowerCase();\n"
            + "  if(t.indexOf('temp')>=0) return '" + b64Temp + "';\n"
            + "  if(t.indexOf('fum')>=0||t.indexOf('gaz')>=0) return '" + b64Fum + "';\n"
            + "  if(t.indexOf('hum')>=0||t.indexOf('sol')>=0) return '" + b64Hum + "';\n"
            + "  return '" + b64Gen + "';\n"
            + "}\n";
    }

    private double[] trouverCoords(String loc) {
        if (loc == null) return new double[]{36.5, 9.2};
        String key = loc.toLowerCase().trim();
        if (COORDS.containsKey(key)) return COORDS.get(key);
        for (Map.Entry<String, double[]> e : COORDS.entrySet()) {
            if (key.contains(e.getKey()) || e.getKey().contains(key)) return e.getValue();
        }
        long seed = loc.hashCode() & 0xFFFFL;
        return new double[]{35.5 + (seed % 200)/100.0, 8.5 + (seed % 250)/100.0};
    }

    private String jsStr(String s) {
        if (s == null) return "";
        // Conserver les caractères accentués (régions tunisiennes)
        // Supprimer uniquement les emojis et caractères non imprimables
        s = s.replaceAll("[\\p{So}\\p{Cn}]", ""); // emojis et non assignés
        return s.replace("\\","\\\\").replace("'","\\'")
                .replace("\"","&quot;").replace("\n"," ").replace("\r","");
    }

    @FXML public void fermer() {
        ((Stage) mapWebView.getScene().getWindow()).close();
    }
}
