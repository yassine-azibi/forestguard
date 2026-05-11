package controller;

import model.Incendie;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import com.sun.net.httpserver.HttpServer;

/**
 * PerimetreController
 *
 * Remplace ouvrirModalPerimetre(Incendie inc) dans ForetPrincipal.java
 *
 * Usage :
 *   private void ouvrirModalPerimetre(Incendie inc) {
 *       new PerimetreController(inc).ouvrir();
 *   }
 */
public class PerimetreController {

    private final Incendie incendie;
    private WebEngine engine;
    private HttpServer proxy;
    private int proxyPort = 0;

    // Cache Leaflet partagé avec CarteController si déjà téléchargé
    private static byte[] leafletJsCache = null;

    public PerimetreController(Incendie incendie) {
        this.incendie = incendie;
    }

    // ════════════════════════════════════════════════════════════════
    //  OUVERTURE
    // ════════════════════════════════════════════════════════════════
    public void ouvrir() {
        demarrerProxy();

        Stage stage = new Stage();
        stage.setTitle("Périmètre — Incendie #" + incendie.getId());
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(true);

        // ── En-tête ──────────────────────────────────────────────
        Label titre = new Label("🔥 Incendie #" + incendie.getId()
                + "  ·  Zone " + incendie.getIdZone()
                + "  ·  " + (incendie.getDateDebut() != null ? incendie.getDateDebut() : ""));
        titre.setStyle("-fx-text-fill:#e8f5e8; -fx-font-size:15; -fx-font-weight:bold;");

        // ── Boutons toolbar ──────────────────────────────────────
        Button btnPlacer  = btn("📍 Placer des points", "#ea580c", "#c2410c");
        Button btnFermer2 = btn("⬡ Fermer périmètre",  "#16a34a", "#15803d");
        Button btnReset   = btn("🗑 Réinitialiser",     "#dc2626", "#b91c1c");
        btnFermer2.setDisable(true);

        ComboBox<String> comboEchelle = new ComboBox<>();
        comboEchelle.getItems().addAll("1 px = 1 m","1 px = 5 m","1 px = 10 m","1 px = 20 m","1 px = 50 m");
        comboEchelle.setValue("1 px = 10 m");
        comboEchelle.setStyle("-fx-font-size:12;");

        Label lblHint = new Label("Activez 'Placer points' puis cliquez sur la carte.");
        lblHint.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:12;");

        // ── WebView carte ─────────────────────────────────────────
        WebView webView = new WebView();
        webView.setMinHeight(420);
        webView.setPrefHeight(460);
        engine = webView.getEngine();
        engine.setJavaScriptEnabled(true);
        webView.setContextMenuEnabled(false);

        // ── Stats bar ─────────────────────────────────────────────
        Label lblPts   = stat("Points : 0");
        Label lblPeri  = stat("Périmètre : —");
        Label lblArea  = stat("Surface : —");
        Label lblCit   = stat("Citernes : —");
        Label lblDetail = new Label("");
        lblDetail.setStyle("-fx-text-fill:#ffb347; -fx-font-size:12; -fx-wrap-text:true;");
        lblDetail.setMaxWidth(860);

        HBox statsBox = new HBox(28, lblPts, lblPeri, lblArea, lblCit);
        statsBox.setStyle("-fx-background-color:rgba(0,0,0,0.4); -fx-padding:10 16; -fx-background-radius:8;");
        statsBox.setAlignment(Pos.CENTER_LEFT);

        // ── Toolbar ───────────────────────────────────────────────
        Label lblEch = new Label("  Échelle:");
        lblEch.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:12;");
        HBox toolbar = new HBox(10, btnPlacer, btnFermer2, btnReset, lblEch, comboEchelle);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        // ── Layout ───────────────────────────────────────────────
        VBox root = new VBox(10, titre, toolbar, lblHint, webView, statsBox, lblDetail);
        root.setStyle("-fx-background-color:#0f1f0f; -fx-padding:18;");

        // ── Bridge Java exposé au JS ──────────────────────────────
        engine.getLoadWorker().stateProperty().addListener((obs, old, now) -> {
            if (now == javafx.concurrent.Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");

                // Objet Java → accessible depuis JS comme window.javaPerimetre
                window.setMember("javaPerimetre", new BridgePerimetre(
                        lblPts, lblPeri, lblArea, lblCit, lblDetail,
                        btnFermer2, lblHint, comboEchelle
                ));

                // Passer l'échelle initiale
                engine.executeScript("setEchelle(10);");
            }
        });

        // ── Actions boutons ──────────────────────────────────────
        btnPlacer.setOnAction(e -> engine.executeScript("togglePlacer();"));

        btnFermer2.setOnAction(e -> {
            int scale = parseScale(comboEchelle.getValue());
            engine.executeScript("fermerPerimetre(" + scale + ");");
        });

        btnReset.setOnAction(e -> {
            engine.executeScript("resetPerimetre();");
            lblPts.setText("Points : 0"); lblPeri.setText("Périmètre : —");
            lblArea.setText("Surface : —"); lblCit.setText("Citernes : —");
            lblDetail.setText(""); btnFermer2.setDisable(true);
            lblHint.setText("Réinitialisé.");
        });

        comboEchelle.setOnAction(e -> {
            int scale = parseScale(comboEchelle.getValue());
            engine.executeScript("setEchelle(" + scale + ");");
        });

        // ── Charger le HTML de la carte ──────────────────────────
        engine.loadContent(buildMapHtml(), "text/html");

        stage.setScene(new Scene(root, 900, 700));
        stage.setOnCloseRequest(e -> arreterProxy());
        stage.showAndWait();
    }

    // ════════════════════════════════════════════════════════════════
    //  BRIDGE Java ← JavaScript
    //  JS appelle window.javaPerimetre.updateStats(pts, peri, area, cit)
    // ════════════════════════════════════════════════════════════════
    public class BridgePerimetre {
        private final Label lblPts, lblPeri, lblArea, lblCit, lblDetail;
        private final Button btnFermer2;
        private final Label lblHint;
        private final ComboBox<String> comboEchelle;

        public BridgePerimetre(Label p, Label pe, Label a, Label c, Label d,
                               Button f, Label h, ComboBox<String> combo) {
            lblPts=p; lblPeri=pe; lblArea=a; lblCit=c; lblDetail=d;
            btnFermer2=f; lblHint=h; comboEchelle=combo;
        }

        // Appelé depuis JS à chaque nouveau point / fermeture
        public void updateStats(String pts, String peri, String area, String cit, boolean canClose) {
            javafx.application.Platform.runLater(() -> {
                lblPts.setText(pts);
                lblPeri.setText(peri);
                lblArea.setText(area);
                lblCit.setText(cit);
                btnFermer2.setDisable(!canClose);
            });
        }

        // Appelé depuis JS quand le périmètre est fermé avec détails
        public void onFerme(String detail) {
            javafx.application.Platform.runLater(() -> {
                lblDetail.setText(detail);
                lblHint.setText("✅ Périmètre fermé. Consultez les estimations.");
                btnFermer2.setDisable(true);
            });
        }

        // Appelé depuis JS pour mettre à jour l'indice dans lblHint
        public void setHint(String hint) {
            javafx.application.Platform.runLater(() -> lblHint.setText(hint));
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  HTML LEAFLET PÉRIMÈTRE
    // ════════════════════════════════════════════════════════════════
    private String buildMapHtml() {
        // Coordonnées de la zone : utiliser les coordonnées de l'incendie si dispo
        // sinon centrer sur la Tunisie
        double lat = 36.8;  // coordonnées par défaut (Tunis)
        double lng = 10.18;
        int    zoom = 12;

        // Si l'incendie a des coordonnées GPS → les utiliser
        // (décommentez si vous avez getLatitude/getLongitude sur Incendie)
        // if (incendie.getLatitude() != 0) { lat = incendie.getLatitude(); lng = incendie.getLongitude(); }

        String tileUrl = proxyPort > 0
                ? "http://127.0.0.1:" + proxyPort + "/tiles/{z}/{x}/{y}.png"
                : "https://tile.openstreetmap.org/{z}/{x}/{y}.png";

        String leafletUrl = proxyPort > 0
                ? "http://127.0.0.1:" + proxyPort + "/leaflet/leaflet.js"
                : "https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.js";

        return "<!DOCTYPE html><html><head><meta charset='UTF-8'/>" +
                "<style>" +
                "*{margin:0;padding:0;box-sizing:border-box;}" +
                "html,body,#map{width:100%;height:100%;overflow:hidden;background:#0a1a0e;}" +
                // CSS Leaflet minimal inline
                ".leaflet-pane,.leaflet-tile,.leaflet-marker-icon,.leaflet-marker-shadow," +
                ".leaflet-tile-container,.leaflet-pane>svg,.leaflet-pane>canvas," +
                ".leaflet-zoom-box,.leaflet-image-layer,.leaflet-layer{position:absolute;left:0;top:0}" +
                ".leaflet-container{overflow:hidden;background:#0a1a0e;cursor:crosshair}" +
                ".leaflet-tile{visibility:hidden}.leaflet-tile-loaded{visibility:inherit}" +
                ".leaflet-pane{z-index:400}.leaflet-tile-pane{z-index:200}" +
                ".leaflet-overlay-pane{z-index:400}.leaflet-shadow-pane{z-index:500}" +
                ".leaflet-marker-pane{z-index:600}.leaflet-tooltip-pane{z-index:650}" +
                ".leaflet-popup-pane{z-index:700}" +
                ".leaflet-map-pane canvas{z-index:100}.leaflet-map-pane svg{z-index:200}" +
                ".leaflet-interactive{cursor:crosshair}" +
                ".leaflet-top,.leaflet-bottom{position:absolute;z-index:1000;pointer-events:none}" +
                ".leaflet-top{top:0}.leaflet-right{right:0}.leaflet-bottom{bottom:0}.leaflet-left{left:0}" +
                ".leaflet-control{float:left;clear:both;pointer-events:visiblePainted;pointer-events:auto}" +
                ".leaflet-right .leaflet-control{float:right}" +
                ".leaflet-top .leaflet-control{margin-top:10px}" +
                ".leaflet-bottom .leaflet-control{margin-bottom:10px}" +
                ".leaflet-left .leaflet-control{margin-left:10px}" +
                ".leaflet-right .leaflet-control{margin-right:10px}" +
                ".leaflet-bar{box-shadow:0 1px 5px rgba(0,0,0,.65);border-radius:4px}" +
                ".leaflet-bar a{background:rgba(8,22,13,0.95);border-bottom:1px solid rgba(74,222,128,0.15);" +
                "width:26px;height:26px;line-height:26px;display:block;text-align:center;text-decoration:none;color:#4ade80}" +
                ".leaflet-bar a:hover{background:rgba(74,222,128,0.15)}" +
                ".leaflet-bar a:first-child{border-top-left-radius:4px;border-top-right-radius:4px}" +
                ".leaflet-bar a:last-child{border-bottom-left-radius:4px;border-bottom-right-radius:4px;border-bottom:none}" +
                ".leaflet-control-zoom-in,.leaflet-control-zoom-out{font:bold 18px monospace;text-indent:1px}" +
                ".leaflet-control-attribution{background:rgba(8,22,13,0.7);color:rgba(255,255,255,0.3);font-size:9px;padding:2px 6px}" +
                ".leaflet-control-scale-line{border:2px solid rgba(74,222,128,0.4);border-top:none;line-height:1.1;" +
                "padding:2px 5px;font-size:10px;white-space:nowrap;background:rgba(8,22,13,0.7);color:rgba(255,255,255,0.5)}" +
                ".leaflet-zoom-animated{-webkit-transform-origin:0 0;transform-origin:0 0}" +
                ".leaflet-zoom-anim .leaflet-zoom-animated{-webkit-transition:-webkit-transform .25s cubic-bezier(0,0,.25,1);transition:transform .25s cubic-bezier(0,0,.25,1)}" +
                ".leaflet-zoom-anim .leaflet-tile,.leaflet-pan-anim .leaflet-tile{-webkit-transition:none;transition:none}" +
                ".leaflet-tile-osm{filter:hue-rotate(130deg) saturate(0.55) brightness(0.5);}" +
                "#status{position:absolute;top:50%;left:50%;transform:translate(-50%,-50%);" +
                "z-index:2000;background:rgba(8,22,13,0.97);border:1px solid rgba(74,222,128,0.3);" +
                "border-radius:10px;padding:14px 24px;color:#4ade80;font-size:13px;font-family:Arial;}" +
                ".pt-label{background:rgba(8,22,13,0.85);color:#ffd700;border:1px solid rgba(255,215,0,0.4);" +
                "border-radius:50%;width:20px;height:20px;line-height:20px;text-align:center;font-size:10px;font-weight:bold;}" +
                "</style></head><body>" +
                "<div id='map'></div>" +
                "<div id='status'>Chargement de la carte...</div>" +
                "<script>" +
                "var map, polygonLayer=null, markersLayer=null;" +
                "var points=[], ferme=false, actif=false, echelle=10;" +
                "var TILE_URL='" + tileUrl + "';" +
                "var LEAFLET_URL='" + leafletUrl + "';" +
                "var LEAFLET_FALLBACKS=[" +
                "  'https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.js'," +
                "  'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.js'" +
                "];" +
                "var fallbackIdx=0;" +
                "" +
                "function chargerLeaflet(url){" +
                "  var s=document.createElement('script');" +
                "  s.src=url;" +
                "  s.onload=initMap;" +
                "  s.onerror=function(){" +
                "    if(fallbackIdx<LEAFLET_FALLBACKS.length){" +
                "      chargerLeaflet(LEAFLET_FALLBACKS[fallbackIdx++]);" +
                "    } else {" +
                "      document.getElementById('status').textContent='Erreur: Leaflet indisponible';" +
                "    }" +
                "  };" +
                "  document.head.appendChild(s);" +
                "}" +
                "" +
                "function initMap(){" +
                "  document.getElementById('status').style.display='none';" +
                "  map=L.map('map',{center:[" + lat + "," + lng + "],zoom:" + zoom + "," +
                "    zoomControl:true,attributionControl:true,preferCanvas:true});" +
                "  L.tileLayer(TILE_URL,{" +
                "    attribution:'&copy; OpenStreetMap',maxZoom:19,crossOrigin:true," +
                "    className:'leaflet-tile-osm'" +
                "  }).addTo(map);" +
                "  L.control.scale({metric:true,imperial:false}).addTo(map);" +
                "  markersLayer=L.layerGroup().addTo(map);" +
                "  polygonLayer=L.layerGroup().addTo(map);" +
                "  map.on('click',onMapClick);" +
                "}" +
                "" +
                "function onMapClick(e){" +
                "  if(!actif||ferme) return;" +
                "  points.push([e.latlng.lat,e.latlng.lng]);" +
                "  redessiner();" +
                "  var n=points.length;" +
                "  var hint='Point '+n+' placé.'+(n>=3?' Vous pouvez fermer le périmètre.':'');" +
                "  if(window.javaPerimetre) window.javaPerimetre.setHint(hint);" +
                "  mettreAJourStats(false);" +
                "}" +
                "" +
                "function redessiner(){" +
                "  markersLayer.clearLayers();" +
                "  polygonLayer.clearLayers();" +
                "  if(points.length===0) return;" +
                "  // Ligne / polygone" +
                "  if(ferme && points.length>2){" +
                "    L.polygon(points,{color:'#ff6e00',weight:2.5,fillColor:'#ff4600',fillOpacity:0.22}).addTo(polygonLayer);" +
                "  } else if(points.length>1){" +
                "    L.polyline(points,{color:'#ffa000',weight:2.5,dashArray:'8,5'}).addTo(polygonLayer);" +
                "  }" +
                "  // Marqueurs numérotés" +
                "  points.forEach(function(p,i){" +
                "    var ic=L.divIcon({className:'',html:" +
                "      '<div class=\"pt-label\" style=\"background:'+(i===0?'rgba(255,215,0,0.9)':'rgba(255,69,0,0.85)')+';color:'+(i===0?'#000':'#fff')+'\">'+((i+1))+'</div>'," +
                "      iconSize:[20,20],iconAnchor:[10,10]});" +
                "    L.marker(p,{icon:ic}).addTo(markersLayer);" +
                "  });" +
                "}" +
                "" +
                "function mettreAJourStats(closed){" +
                "  var n=points.length;" +
                "  var peri=0;" +
                "  for(var i=0;i<n-1;i++){" +
                "    peri+=map.distance(points[i],points[i+1]);" +
                "  }" +
                "  if(closed&&n>1) peri+=map.distance(points[n-1],points[0]);" +
                "  var periStr=peri>=1000?'Périmètre : '+(peri/1000).toFixed(2)+' km':'Périmètre : '+Math.round(peri)+' m';" +
                "  var areaStr='Surface : —', citStr='Citernes : —';" +
                "  if(closed&&n>2){" +
                "    var areaM2=Math.abs(L.GeometryUtil?0:calcAireM2());" +
                "    // Calcul shoelace en m² approximatif via distance haversine" +
                "    var cit=Math.ceil(areaM2*10/10000);" +
                "    areaStr=areaM2>=10000?'Surface : '+(areaM2/10000).toFixed(2)+' ha':'Surface : '+Math.round(areaM2)+' m²';" +
                "    citStr='Citernes : '+cit;" +
                "  }" +
                "  if(window.javaPerimetre){" +
                "    window.javaPerimetre.updateStats('Points : '+n, periStr, areaStr, citStr, n>=3&&!closed);" +
                "  }" +
                "}" +
                "" +
                // Calcul approximatif de l'aire en m² (shoelace sur lat/lng → m²)
                "function calcAireM2(){" +
                "  var n=points.length; var a=0;" +
                "  for(var i=0;i<n;i++){" +
                "    var j=(i+1)%n;" +
                "    var xi=points[i][1]*111320*Math.cos(points[i][0]*Math.PI/180);" +
                "    var yi=points[i][0]*110540;" +
                "    var xj=points[j][1]*111320*Math.cos(points[j][0]*Math.PI/180);" +
                "    var yj=points[j][0]*110540;" +
                "    a+=xi*yj-xj*yi;" +
                "  }" +
                "  return Math.abs(a)/2;" +
                "}" +
                "" +
                "function togglePlacer(){" +
                "  actif=!actif;" +
                "  if(window.javaPerimetre){" +
                "    window.javaPerimetre.setHint(actif?'Cliquez sur la carte pour poser des points...':'Placement pausé.');" +
                "  }" +
                "}" +
                "" +
                "function fermerPerimetre(scale){" +
                "  if(points.length<3) return;" +
                "  ferme=true; actif=false;" +
                "  redessiner();" +
                "  mettreAJourStats(true);" +
                "  var areaM2=calcAireM2();" +
                "  var volumeL=Math.round(areaM2*10);" +
                "  var cit=Math.ceil(volumeL/10000);" +
                "  var equipes=Math.max(1,Math.ceil(cit/3));" +
                "  var durMin=volumeL>0?Math.round(volumeL/(400*equipes)):0;" +
                "  var crit=areaM2>=50000?'CRITIQUE':areaM2>=5000?'ELEVE':'MODERE';" +
                "  var detail='Volume total : '+volumeL.toLocaleString()+' L  |  '" +
                "    +'Citernes (10 000 L) : '+cit+'  |  '" +
                "    +'Equipes : '+equipes+'  |  '" +
                "    +'Duree estimee : ~'+durMin+' min  |  '" +
                "    +'Criticite : '+crit;" +
                "  if(window.javaPerimetre) window.javaPerimetre.onFerme(detail);" +
                "}" +
                "" +
                "function resetPerimetre(){" +
                "  points=[]; ferme=false; actif=false;" +
                "  redessiner();" +
                "}" +
                "" +
                "function setEchelle(s){ echelle=s; }" +
                "" +
                "// Démarrer le chargement Leaflet" +
                "chargerLeaflet(LEAFLET_URL);" +
                "</script></body></html>";
    }

    // ════════════════════════════════════════════════════════════════
    //  PROXY LOCAL (même logique que CarteController)
    // ════════════════════════════════════════════════════════════════
    private void demarrerProxy() {
        try {
            proxy = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 10);
            proxyPort = proxy.getAddress().getPort();

            proxy.createContext("/tiles", exchange -> {
                String[] p = exchange.getRequestURI().getPath().split("/");
                if (p.length < 5) { exchange.sendResponseHeaders(400,-1); return; }
                String sub = new String[]{"a","b","c"}[Math.abs((p[2]+p[3]+p[4]).hashCode())%3];
                String url = "https://"+sub+".tile.openstreetmap.org/"+p[2]+"/"+p[3]+"/"+p[4].replace(".png","")+".png";
                servirUrl(exchange, url, "image/png");
            });

            proxy.createContext("/leaflet", exchange -> {
                if (!exchange.getRequestURI().getPath().endsWith(".js")) {
                    exchange.sendResponseHeaders(404,-1); return;
                }
                if (leafletJsCache == null) {
                    for (String cdn : new String[]{
                            "https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.js",
                            "https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.js"}) {
                        try { leafletJsCache = fetch(cdn); break; }
                        catch (Exception ignored) {}
                    }
                }
                if (leafletJsCache != null) {
                    exchange.getResponseHeaders().set("Content-Type","application/javascript");
                    exchange.getResponseHeaders().set("Access-Control-Allow-Origin","*");
                    exchange.sendResponseHeaders(200, leafletJsCache.length);
                    exchange.getResponseBody().write(leafletJsCache);
                    exchange.getResponseBody().close();
                } else {
                    exchange.sendResponseHeaders(503,-1);
                }
            });

            proxy.setExecutor(Executors.newFixedThreadPool(8));
            proxy.start();
            System.out.println("[PerimetreController] Proxy démarré port " + proxyPort);
        } catch (IOException e) {
            System.err.println("[PerimetreController] Erreur proxy : " + e.getMessage());
            proxyPort = 0;
        }
    }

    private void servirUrl(com.sun.net.httpserver.HttpExchange ex, String url, String ct) {
        try {
            byte[] data = fetch(url);
            ex.getResponseHeaders().set("Content-Type", ct);
            ex.getResponseHeaders().set("Access-Control-Allow-Origin","*");
            ex.getResponseHeaders().set("Cache-Control","max-age=3600");
            ex.sendResponseHeaders(200, data.length);
            ex.getResponseBody().write(data);
            ex.getResponseBody().close();
        } catch (Exception e) {
            try { ex.sendResponseHeaders(502,-1); } catch (IOException ignored) {}
        }
    }

    private byte[] fetch(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setRequestProperty("User-Agent","Mozilla/5.0 ForestGuard/1.0");
        c.setConnectTimeout(6000); c.setReadTimeout(10000);
        c.setInstanceFollowRedirects(true);
        if (c.getResponseCode() != 200) throw new IOException("HTTP " + c.getResponseCode());
        try (InputStream is = c.getInputStream()) {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            byte[] buf = new byte[8192]; int n;
            while ((n = is.read(buf)) != -1) b.write(buf,0,n);
            return b.toByteArray();
        }
    }

    private void arreterProxy() {
        if (proxy != null) { proxy.stop(0); System.out.println("[PerimetreController] Proxy arrêté"); }
    }

    // ════════════════════════════════════════════════════════════════
    //  HELPERS UI
    // ════════════════════════════════════════════════════════════════
    private Button btn(String txt, String bg, String bgH) {
        Button b = new Button(txt);
        String s = "-fx-background-color:"+bg+";-fx-text-fill:white;-fx-background-radius:8;" +
                "-fx-cursor:hand;-fx-font-weight:bold;-fx-font-size:12;-fx-padding:8 14;";
        String h = "-fx-background-color:"+bgH+";-fx-text-fill:white;-fx-background-radius:8;" +
                "-fx-cursor:hand;-fx-font-weight:bold;-fx-font-size:12;-fx-padding:8 14;";
        b.setStyle(s);
        b.setOnMouseEntered(e -> b.setStyle(h));
        b.setOnMouseExited(e  -> b.setStyle(s));
        return b;
    }

    private Label stat(String txt) {
        Label l = new Label(txt);
        l.setStyle("-fx-text-fill:#e8f5e8; -fx-font-size:13; -fx-font-weight:bold;");
        return l;
    }

    private int parseScale(String v) {
        try { return Integer.parseInt(v.replaceAll("[^0-9]","")); }
        catch (Exception e) { return 10; }
    }
}

