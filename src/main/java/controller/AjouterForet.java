package controller;

import model.Foret;
import utils.ForetService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

public class AjouterForet {

    @FXML private Label     titreLabel;
    @FXML private TextField nomField;
    @FXML private ComboBox<String> gouvernoratCombo;
    @FXML private TextField localisationField;
    @FXML private TextField superficieField;
    @FXML private ComboBox<String> vegetationCombo;
    @FXML private ComboBox<String> risqueCombo;
    @FXML private DatePicker       dateCreationPicker;
    @FXML private Label            lblCoords;

    // Coordonnées sélectionnées via la carte
    private double latSelectionnee = 0;
    private double lngSelectionnee = 0;

    private Foret foretEnEdition = null;
    private final ForetService service = new ForetService();

    @FXML
    public void initialize() {
        vegetationCombo.getItems().addAll("Coniferes", "Feuillus", "Mixte", "Savane", "Maquis");
        vegetationCombo.setValue("Mixte");
        risqueCombo.getItems().addAll("Faible", "Moyen", "Eleve", "Critique");
        risqueCombo.setValue("Faible");
        dateCreationPicker.setValue(java.time.LocalDate.now());

        // ── Restrictions en temps réel ──
        Validation.lettresSeulement(nomField);          // Nom : lettres uniquement
        Validation.adresse(localisationField);           // Localisation : adresse
        Validation.decimalPositif(superficieField);      // Superficie : décimal positif

        // Réinitialiser le style au focus
        Validation.reinitialiserAuFocus(nomField, localisationField, superficieField);

        // ── Gouvernorats de Tunisie ──
        gouvernoratCombo.getItems().addAll(
            "Ariana", "Béja", "Ben Arous", "Bizerte", "Gabès",
            "Gafsa", "Jendouba", "Kairouan", "Kasserine", "Kébili",
            "Le Kef", "Mahdia", "La Manouba", "Médenine", "Monastir",
            "Nabeul", "Sfax", "Sidi Bouzid", "Siliana", "Sousse",
            "Tataouine", "Tozeur", "Tunis", "Zaghouan"
        );
    }

    public void remplirPourEdition(Foret foret) {
        this.foretEnEdition = foret;
        if (titreLabel != null) titreLabel.setText("Modifier la Forêt");
        nomField.setText(foret.getNom());
        localisationField.setText(foret.getLocalisation());
        superficieField.setText(String.valueOf(foret.getSuperficie()));
        vegetationCombo.setValue(foret.getTypeVegetation());
        risqueCombo.setValue(foret.getNiveauRisque());
        // Pré-remplir le gouvernorat depuis la localisation
        if (foret.getLocalisation() != null) {
            for (String gov : gouvernoratCombo.getItems()) {
                if (foret.getLocalisation().toLowerCase().contains(gov.toLowerCase())) {
                    gouvernoratCombo.setValue(gov);
                    break;
                }
            }
        }
        if (foret.getDateCreation() != null && !foret.getDateCreation().isEmpty()) {
            try { dateCreationPicker.setValue(java.time.LocalDate.parse(foret.getDateCreation())); }
            catch (Exception ignored) {}
        }
        if (foret.getLatitude() != 0 || foret.getLongitude() != 0) {
            latSelectionnee = foret.getLatitude();
            lngSelectionnee = foret.getLongitude();
            if (lblCoords != null)
                lblCoords.setText(String.format("\uD83D\uDCCD %.4f, %.4f", latSelectionnee, lngSelectionnee));
        }
    }

    // ════════════════════════════════════════════
    //  BRIDGE JS → Java (doit être public)
    // ════════════════════════════════════════════
    public static class MapBridge {
        private final double[] coords;
        private final Label lblLat, lblLng, lblAddr;
        private final Button btnConfirmer;

        public MapBridge(double[] coords, Label lblLat, Label lblLng, Label lblAddr, Button btnConfirmer) {
            this.coords = coords;
            this.lblLat = lblLat; this.lblLng = lblLng;
            this.lblAddr = lblAddr; this.btnConfirmer = btnConfirmer;
        }

        public void onClic(double lat, double lng, String adresse) {
            javafx.application.Platform.runLater(() -> {
                coords[0] = lat; coords[1] = lng;
                lblLat.setText(String.format("Lat : %.5f", lat));
                lblLng.setText(String.format("Lng : %.5f", lng));
                lblAddr.setText(String.format("\uD83D\uDCCD %.5f, %.5f", lat, lng));
                btnConfirmer.setDisable(false);
            });
        }
    }

    // ════════════════════════════════════════════
    //  PROXY LOCAL (même code que CarteController)
    // ════════════════════════════════════════════
    private com.sun.net.httpserver.HttpServer mapProxy;
    private int mapProxyPort = 0;

    private static byte[] leafletJsCache  = null;
    private static byte[] leafletCssCache = null;

    private void demarrerProxyCarte() {
        // Charger Leaflet local une seule fois
        if (leafletJsCache == null) {
            try (java.io.InputStream is = getClass().getResourceAsStream("/Leaflet/leaflet.js")) {
                if (is != null) leafletJsCache = is.readAllBytes();
            } catch (Exception ignored) {}
        }
        if (leafletCssCache == null) {
            try (java.io.InputStream is = getClass().getResourceAsStream("/Leaflet/leaflet.css")) {
                if (is != null) leafletCssCache = is.readAllBytes();
            } catch (Exception ignored) {}
        }

        java.io.File cacheDir = new java.io.File(System.getProperty("user.home"), ".forestguard_tiles");
        cacheDir.mkdirs();

        try {
            mapProxy = com.sun.net.httpserver.HttpServer.create(
                new java.net.InetSocketAddress("127.0.0.1", 0), 20);
            mapProxyPort = mapProxy.getAddress().getPort();

            // /tiles → OSM avec cache disque
            mapProxy.createContext("/tiles", ex -> {
                String[] p = ex.getRequestURI().getPath().split("/");
                if (p.length < 5) { ex.sendResponseHeaders(400, -1); return; }
                String z = p[2], x = p[3], y = p[4].replace(".png", "");
                java.io.File cache = new java.io.File(cacheDir, z + "_" + x + "_" + y + ".png");
                byte[] data = null;
                if (cache.exists() && cache.length() > 0) {
                    try (java.io.FileInputStream fis = new java.io.FileInputStream(cache)) {
                        data = fis.readAllBytes();
                    } catch (Exception ignored) {}
                }
                if (data == null) {
                    String sub = new String[]{"a","b","c"}[Math.abs((z+x+y).hashCode()) % 3];
                    try {
                        java.net.HttpURLConnection conn = (java.net.HttpURLConnection)
                            new java.net.URL("https://" + sub + ".tile.openstreetmap.org/" + z + "/" + x + "/" + y + ".png").openConnection();
                        conn.setRequestProperty("User-Agent", "Mozilla/5.0 ForestGuard/1.0");
                        conn.setConnectTimeout(6000); conn.setReadTimeout(10000);
                        if (conn.getResponseCode() == 200) {
                            try (java.io.InputStream is = conn.getInputStream()) {
                                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                                byte[] buf = new byte[8192]; int n;
                                while ((n = is.read(buf)) != -1) baos.write(buf, 0, n);
                                data = baos.toByteArray();
                            }
                            final byte[] save = data;
                            new Thread(() -> {
                                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(cache)) {
                                    fos.write(save);
                                } catch (Exception ignored) {}
                            }).start();
                        }
                    } catch (Exception ignored) {}
                }
                if (data == null) { ex.sendResponseHeaders(502, -1); return; }
                ex.getResponseHeaders().set("Content-Type", "image/png");
                ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                ex.getResponseHeaders().set("Cache-Control", "max-age=86400");
                ex.sendResponseHeaders(200, data.length);
                ex.getResponseBody().write(data);
                ex.getResponseBody().close();
            });

            // /leaflet → fichiers locaux
            mapProxy.createContext("/leaflet", ex -> {
                String path = ex.getRequestURI().getPath();
                byte[] data = null; String ct = "text/plain";
                if (path.endsWith("leaflet.js"))  { data = leafletJsCache;  ct = "application/javascript"; }
                if (path.endsWith("leaflet.css")) { data = leafletCssCache; ct = "text/css"; }
                if (data != null) {
                    ex.getResponseHeaders().set("Content-Type", ct);
                    ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                    ex.getResponseHeaders().set("Cache-Control", "max-age=86400");
                    ex.sendResponseHeaders(200, data.length);
                    ex.getResponseBody().write(data);
                    ex.getResponseBody().close();
                } else { ex.sendResponseHeaders(404, -1); }
            });

            mapProxy.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(8));
            mapProxy.start();
        } catch (Exception e) {
            System.err.println("[AjouterForet] Proxy: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════
    //  CARTE DE LOCALISATION
    // ════════════════════════════════════════════

    @FXML
    public void ouvrirCarteLocalisation() {
        // Démarrer le proxy pour cette carte
        demarrerProxyCarte();

        Stage mapStage = new Stage();
        mapStage.setTitle("Choisir la localisation");
        mapStage.initModality(Modality.APPLICATION_MODAL);

        double initLat  = latSelectionnee != 0 ? latSelectionnee : 33.88;
        double initLng  = lngSelectionnee != 0 ? lngSelectionnee : 9.54;
        int    initZoom = latSelectionnee != 0 ? 12 : 6;

        javafx.scene.web.WebView webView = new javafx.scene.web.WebView();
        VBox.setVgrow(webView, Priority.ALWAYS);
        javafx.scene.web.WebEngine eng = webView.getEngine();
        eng.setJavaScriptEnabled(true);
        webView.setContextMenuEnabled(false);

        Label lblLat  = new Label("Lat : \u2014");
        lblLat.setStyle("-fx-font-size:13;-fx-text-fill:#1e293b;-fx-font-weight:bold;");
        Label lblLng  = new Label("Lng : \u2014");
        lblLng.setStyle("-fx-font-size:13;-fx-text-fill:#1e293b;-fx-font-weight:bold;");
        Label lblAddr = new Label("Cliquez sur la carte pour choisir la position");
        lblAddr.setStyle("-fx-font-size:12;-fx-text-fill:#6b7280;");

        Button btnConfirmer = new Button("\u2705 Confirmer cette position");
        btnConfirmer.setStyle("-fx-background-color:#16a34a;-fx-text-fill:white;-fx-font-size:13;-fx-font-weight:bold;-fx-background-radius:10;-fx-cursor:hand;-fx-padding:10 20;");
        btnConfirmer.setDisable(true);

        Button btnAnnuler = new Button("Annuler");
        btnAnnuler.setStyle("-fx-background-color:white;-fx-text-fill:#374151;-fx-font-size:13;-fx-background-radius:10;-fx-border-color:#d1d5db;-fx-border-radius:10;-fx-cursor:hand;-fx-padding:10 20;");
        btnAnnuler.setOnAction(e -> { mapStage.close(); arreterProxyCarte(); });

        double[] selectedCoords = {0, 0};
        MapBridge bridge = new MapBridge(selectedCoords, lblLat, lblLng, lblAddr, btnConfirmer);

        eng.getLoadWorker().stateProperty().addListener((obs, old, now) -> {
            if (now == javafx.concurrent.Worker.State.SUCCEEDED) {
                netscape.javascript.JSObject win = (netscape.javascript.JSObject) eng.executeScript("window");
                win.setMember("javaMap", bridge);
            }
        });

        btnConfirmer.setOnAction(e -> {
            latSelectionnee = selectedCoords[0];
            lngSelectionnee = selectedCoords[1];
            if (localisationField.getText().isBlank())
                localisationField.setText(String.format("%.4f, %.4f", latSelectionnee, lngSelectionnee));
            if (lblCoords != null)
                lblCoords.setText(String.format("\uD83D\uDCCD %.4f, %.4f", latSelectionnee, lngSelectionnee));
            mapStage.close();
            arreterProxyCarte();
        });

        // HTML avec proxy local — même mécanisme que CarteController
        String tileUrl   = "http://127.0.0.1:" + mapProxyPort + "/tiles/{z}/{x}/{y}.png";
        String leafletJs = "http://127.0.0.1:" + mapProxyPort + "/leaflet/leaflet.js";
        String markerInit = (latSelectionnee != 0)
            ? "var marker=L.marker([" + initLat + "," + initLng + "]).addTo(map).bindPopup('Position actuelle').openPopup();"
            : "var marker=null;";

        String html =
            "<!DOCTYPE html><html><head><meta charset='UTF-8'>" +
            "<style>" +
            "*{margin:0;padding:0;box-sizing:border-box}" +
            "html,body,#map{width:100%;height:100%;overflow:hidden}" +
            ".leaflet-control-attribution{display:none!important}" +
            ".leaflet-bar a{background-color:rgba(8,22,13,0.95)!important;color:#4ade80!important;border-color:rgba(74,222,128,0.2)!important}" +
            ".hint{position:absolute;bottom:16px;left:50%;transform:translateX(-50%);z-index:1000;" +
            "  background:rgba(22,163,74,0.92);color:white;padding:8px 20px;border-radius:20px;" +
            "  font-size:13px;white-space:nowrap;pointer-events:none;font-weight:bold;}" +
            "</style></head><body>" +
            "<div id='map'></div>" +
            "<div class='hint' id='hint'>Cliquez sur la carte pour placer le marqueur</div>" +
            "<script src='" + leafletJs + "'></script>" +
            "<script>" +
            "var map=L.map('map',{zoomControl:true,attributionControl:false}).setView([" + initLat + "," + initLng + "]," + initZoom + ");" +
            "L.tileLayer('" + tileUrl + "',{maxZoom:19}).addTo(map);" +
            // Forcer le recalcul de la taille après chargement
            "setTimeout(function(){map.invalidateSize(true);},300);" +
            "window.addEventListener('resize',function(){map.invalidateSize(true);});" +
            markerInit +
            "map.on('click',function(e){" +
            "  var lat=e.latlng.lat,lng=e.latlng.lng;" +
            "  if(marker)map.removeLayer(marker);" +
            // Pin de localisation style moderne
            "  var pin='<div style=\"position:relative;width:36px;height:44px;\">" +
            "<div style=\"width:36px;height:36px;border-radius:50% 50% 50% 0;background:#16a34a;" +
            "transform:rotate(-45deg);box-shadow:0 4px 12px rgba(0,0,0,0.4);border:3px solid white;\"></div>" +
            "<div style=\"position:absolute;top:4px;left:4px;width:28px;height:28px;border-radius:50%;" +
            "background:white;display:flex;align-items:center;justify-content:center;" +
            "color:#16a34a;font-weight:bold;font-size:16px;transform:rotate(45deg);\">&#9651;</div>" +
            "</div>';" +
            "  var ic=L.divIcon({html:pin,iconSize:[36,44],iconAnchor:[18,44],className:''});" +
            "  marker=L.marker([lat,lng],{icon:ic}).addTo(map);" +
            "  marker.bindPopup('<b>Position selectionnee</b><br>'+lat.toFixed(5)+', '+lng.toFixed(5)).openPopup();" +
            "  document.getElementById('hint').textContent=lat.toFixed(4)+', '+lng.toFixed(4);" +
            "  if(window.javaMap){try{window.javaMap.onClic(lat,lng,'');}catch(err){}}" +
            "});" +
            "</script></body></html>";

        eng.loadContent(html, "text/html");

        mapStage.setOnCloseRequest(e -> arreterProxyCarte());

        HBox infoBar = new HBox(24, lblLat, lblLng);
        infoBar.setAlignment(Pos.CENTER_LEFT);
        infoBar.setStyle("-fx-background-color:#f8fafc;-fx-padding:10 16;-fx-border-color:#e2e8f0;-fx-border-width:1 0 0 0;");

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox btnBar = new HBox(12, lblAddr, spacer, btnAnnuler, btnConfirmer);
        btnBar.setAlignment(Pos.CENTER_LEFT);
        btnBar.setStyle("-fx-background-color:white;-fx-padding:12 16;-fx-border-color:#e2e8f0;-fx-border-width:1 0 0 0;");

        VBox root = new VBox(0, webView, infoBar, btnBar);
        VBox.setVgrow(webView, Priority.ALWAYS);

        mapStage.setScene(new Scene(root, 900, 640));
        mapStage.setResizable(true);
        mapStage.show();
        // Forcer le recalcul de la taille de la carte après affichage
        javafx.application.Platform.runLater(() -> {
            try { eng.executeScript("if(typeof map!=='undefined')map.invalidateSize(true);"); }
            catch (Exception ignored) {}
        });
    }

    private void arreterProxyCarte() {
        if (mapProxy != null) { mapProxy.stop(0); mapProxy = null; }
    }

    // ════════════════════════════════════════════
    //  ENREGISTRER
    // ════════════════════════════════════════════

    @FXML
    public void validerEtAjouter(ActionEvent event) {
        // ── Validation complète ──
        boolean ok = true;
        StringBuilder erreurs = new StringBuilder();

        if (!Validation.requis(nomField, "Nom")) {
            erreurs.append("• Le nom est obligatoire et ne doit contenir que des lettres.\n");
            ok = false;
        } else if (!Validation.longueurMin(nomField, 2)) {
            erreurs.append("• Le nom doit contenir au moins 2 caractères.\n");
            ok = false;
        }

        if (!Validation.requis(localisationField, "Localisation")) {
            erreurs.append("• La localisation est obligatoire.\n");
            ok = false;
        }

        if (gouvernoratCombo.getValue() == null) {
            erreurs.append("• Le gouvernorat est obligatoire.\n");
            ok = false;
        }

        if (!Validation.requis(superficieField, "Superficie")) {
            erreurs.append("• La superficie est obligatoire.\n");
            ok = false;
        } else if (!Validation.estDecimalPositif(superficieField)) {
            erreurs.append("• La superficie doit être un nombre positif (ex: 1500.5).\n");
            ok = false;
        }

        if (dateCreationPicker.getValue() == null) {
            erreurs.append("• La date de création est obligatoire.\n");
            ok = false;
        }

        if (!ok) {
            Validation.afficherErreur("Erreurs de saisie", erreurs.toString().trim());
            return;
        }

        try {
            double superficie = Double.parseDouble(superficieField.getText().replace(",", "."));
            String dateCreation = dateCreationPicker.getValue().toString();
            // Combiner gouvernorat + localisation
            String gouvernorat = gouvernoratCombo.getValue() != null ? gouvernoratCombo.getValue() : "";
            String localisation = localisationField.getText().trim();
            if (!gouvernorat.isEmpty() && !localisation.toLowerCase().contains(gouvernorat.toLowerCase())) {
                localisation = gouvernorat + (localisation.isEmpty() ? "" : " - " + localisation);
            }

            if (foretEnEdition == null) {
                Foret f = new Foret(
                        nomField.getText().trim(),
                        localisation,
                        superficie,
                        vegetationCombo.getValue(),
                        risqueCombo.getValue(),
                        dateCreation);
                f.setLatitude(latSelectionnee);
                f.setLongitude(lngSelectionnee);
                service.addEntity(f);
            } else {
                foretEnEdition.setNom(nomField.getText().trim());
                foretEnEdition.setLocalisation(localisation);
                foretEnEdition.setSuperficie(superficie);
                foretEnEdition.setTypeVegetation(vegetationCombo.getValue());
                foretEnEdition.setNiveauRisque(risqueCombo.getValue());
                foretEnEdition.setDateCreation(dateCreation);
                if (latSelectionnee != 0) foretEnEdition.setLatitude(latSelectionnee);
                if (lngSelectionnee != 0) foretEnEdition.setLongitude(lngSelectionnee);
                service.updateEntity(foretEnEdition.getId(), foretEnEdition);
            }
            fermerFenetre();
        } catch (NumberFormatException e) {
            Validation.afficherErreur("Erreur", "Superficie invalide — entrez un nombre valide.");
        }
    }

    @FXML
    public void annuler(ActionEvent event) { fermerFenetre(); }

    private void fermerFenetre() {
        ((Stage) nomField.getScene().getWindow()).close();
    }
}

