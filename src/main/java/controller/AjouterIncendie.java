package controller;

import model.Incendie;
import utils.IncendieService;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

public class AjouterIncendie {

    @FXML private Label             titreLabel;
    @FXML private WebView           mapView;
    @FXML private Label             lblZoneDetectee;
    @FXML private Label             lblSuperficieCalculee;
    @FXML private ChoiceBox<String> causeCombo;
    @FXML private DatePicker        dateDebutPicker;
    @FXML private DatePicker        dateFinPicker;
    @FXML private TextField         superficieField;
    @FXML private ChoiceBox<String> graviteCombo;
    @FXML private ChoiceBox<String> statutCombo;

    // Valeurs reçues depuis la carte
    int    idZoneDetecte      = 0;
    double superficieCalculee = 0.0;
    String polygoneJson       = null;

    private Incendie incendieEnEdition = null;
    private final IncendieService service = new IncendieService();

    // ── Bridge JS→Java : DOIT être public static pour que JSObject puisse l'appeler ──
    public static class MapBridge {
        private AjouterIncendie ctrl;
        public void setController(AjouterIncendie c) { this.ctrl = c; }

        public void onZoneDefinie(int idZone, double superficieHa, String geojson) {
            Platform.runLater(() -> {
                if (ctrl == null) return;
                ctrl.idZoneDetecte      = idZone;
                ctrl.superficieCalculee = superficieHa;
                ctrl.polygoneJson       = geojson;
                ctrl.lblZoneDetectee.setText(idZone > 0 ? "Zone " + idZone : "—");
                ctrl.lblSuperficieCalculee.setText(superficieHa > 0
                        ? String.format("%.2f ha", superficieHa) : "—");
                if (ctrl.superficieField.getText().isBlank() && superficieHa > 0)
                    ctrl.superficieField.setText(String.format("%.2f", superficieHa));
            });
        }

        public void onZoneEffacee() {
            Platform.runLater(() -> {
                if (ctrl == null) return;
                ctrl.idZoneDetecte      = 0;
                ctrl.superficieCalculee = 0;
                ctrl.polygoneJson       = null;
                ctrl.lblZoneDetectee.setText("—");
                ctrl.lblSuperficieCalculee.setText("—");
            });
        }
    }

    // Référence forte pour éviter le GC (JSObject ne garde pas de référence forte)
    private final MapBridge bridge = new MapBridge();

    @FXML
    public void initialize() {
        bridge.setController(this);

        causeCombo.getItems().addAll("Foudre", "Humain", "Spontane", "Inconnu");
        causeCombo.setValue("Inconnu");
        graviteCombo.getItems().addAll("Faible", "Moyen", "Eleve", "Critique");
        graviteCombo.setValue("Faible");
        statutCombo.getItems().addAll("En cours", "Controle", "Eteint");
        statutCombo.setValue("En cours");
        dateDebutPicker.setValue(java.time.LocalDate.now());

        // ── Restriction : superficie décimale positive ──
        Validation.decimalPositif(superficieField);
        Validation.reinitialiserAuFocus(superficieField);

        WebEngine engine = mapView.getEngine();
        engine.setJavaScriptEnabled(true);
        engine.getLoadWorker().stateProperty().addListener((obs, old, now) -> {
            if (now == Worker.State.SUCCEEDED) {
                JSObject win = (JSObject) engine.executeScript("window");
                win.setMember("javaBridge", bridge);
            }
        });
        engine.loadContent(buildMapHtml());
    }

    public void remplirPourEdition(Incendie inc) {
        this.incendieEnEdition = inc;
        if (titreLabel != null) titreLabel.setText("Modifier Incendie");
        idZoneDetecte = inc.getIdZone();
        causeCombo.setValue(inc.getCause());
        graviteCombo.setValue(inc.getNiveauGravite());
        statutCombo.setValue(inc.getStatut());
        superficieField.setText(String.valueOf(inc.getSuperficieBrulee()));
        if (inc.getDateDebut() != null && !inc.getDateDebut().isEmpty()) {
            try { dateDebutPicker.setValue(java.time.LocalDate.parse(inc.getDateDebut())); }
            catch (Exception ignored) {}
        }
        if (inc.getDateFin() != null && !inc.getDateFin().isEmpty()) {
            try { dateFinPicker.setValue(java.time.LocalDate.parse(inc.getDateFin())); }
            catch (Exception ignored) {}
        }
        if (lblZoneDetectee != null)
            lblZoneDetectee.setText(inc.getIdZone() > 0 ? "Zone " + inc.getIdZone() : "—");
        if (lblSuperficieCalculee != null)
            lblSuperficieCalculee.setText(inc.getSuperficieBrulee() > 0
                    ? String.format("%.2f ha", inc.getSuperficieBrulee()) : "—");
    }

    @FXML
    public void validerEtAjouter(ActionEvent event) {
        boolean ok = true;
        StringBuilder erreurs = new StringBuilder();

        if (idZoneDetecte <= 0) {
            erreurs.append("• Veuillez délimiter la zone affectée sur la carte.\n");
            ok = false;
        }

        if (superficieField.getText().isBlank()) {
            Validation.marquerErreur(superficieField);
            erreurs.append("• La superficie brûlée est obligatoire.\n");
            ok = false;
        } else if (!Validation.estDecimalPositif(superficieField)) {
            erreurs.append("• La superficie doit être un nombre positif (ex: 45.5).\n");
            ok = false;
        }

        if (dateDebutPicker.getValue() == null) {
            erreurs.append("• La date de début est obligatoire.\n");
            ok = false;
        }

        if (dateFinPicker.getValue() != null && dateDebutPicker.getValue() != null
                && dateFinPicker.getValue().isBefore(dateDebutPicker.getValue())) {
            erreurs.append("• La date de fin ne peut pas être avant la date de début.\n");
            ok = false;
        }

        if (!ok) {
            Validation.afficherErreur("Erreurs de saisie", erreurs.toString().trim());
            return;
        }

        try {
            double sup       = Double.parseDouble(superficieField.getText().replace(",", "."));
            String dateDebut = dateDebutPicker.getValue().toString();
            String dateFin   = dateFinPicker.getValue() != null
                    ? dateFinPicker.getValue().toString() : null;

            if (incendieEnEdition == null) {
                service.addEntity(new Incendie(
                        idZoneDetecte, dateDebut, dateFin, sup,
                        causeCombo.getValue(),
                        graviteCombo.getValue(),
                        statutCombo.getValue()));
            } else {
                incendieEnEdition.setIdZone(idZoneDetecte);
                incendieEnEdition.setDateDebut(dateDebut);
                incendieEnEdition.setDateFin(dateFin);
                incendieEnEdition.setSuperficieBrulee(sup);
                incendieEnEdition.setCause(causeCombo.getValue());
                incendieEnEdition.setNiveauGravite(graviteCombo.getValue());
                incendieEnEdition.setStatut(statutCombo.getValue());
                service.updateEntity(incendieEnEdition.getId(), incendieEnEdition);
            }
            fermerFenetre();
        } catch (NumberFormatException e) {
            Validation.afficherErreur("Erreur", "Superficie invalide.");
        }
    }

    @FXML
    public void annuler(ActionEvent event) { fermerFenetre(); }

    private com.sun.net.httpserver.HttpServer mapProxy;
    private int mapProxyPort = 0;
    private static byte[] leafletJsCache  = null;
    private static byte[] leafletCssCache = null;

    private void demarrerProxy() {
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
            mapProxy.createContext("/tiles", ex -> {
                String[] p = ex.getRequestURI().getPath().split("/");
                if (p.length < 5) { ex.sendResponseHeaders(400,-1); return; }
                String z=p[2],x=p[3],y=p[4].replace(".png","");
                java.io.File cache = new java.io.File(cacheDir, z+"_"+x+"_"+y+".png");
                byte[] data = null;
                if (cache.exists() && cache.length()>0) {
                    try(java.io.FileInputStream fis=new java.io.FileInputStream(cache)){data=fis.readAllBytes();}catch(Exception ignored){}
                }
                if (data == null) {
                    String sub=new String[]{"a","b","c"}[Math.abs((z+x+y).hashCode())%3];
                    try {
                        java.net.HttpURLConnection conn=(java.net.HttpURLConnection)new java.net.URL(
                            "https://"+sub+".tile.openstreetmap.org/"+z+"/"+x+"/"+y+".png").openConnection();
                        conn.setRequestProperty("User-Agent","Mozilla/5.0 ForestGuard/1.0");
                        conn.setConnectTimeout(6000); conn.setReadTimeout(10000);
                        if(conn.getResponseCode()==200){
                            try(java.io.InputStream is=conn.getInputStream()){
                                java.io.ByteArrayOutputStream baos=new java.io.ByteArrayOutputStream();
                                byte[] buf=new byte[8192];int n;
                                while((n=is.read(buf))!=-1)baos.write(buf,0,n);
                                data=baos.toByteArray();
                            }
                            final byte[] save=data;
                            new Thread(()->{try(java.io.FileOutputStream fos=new java.io.FileOutputStream(cache)){fos.write(save);}catch(Exception ignored){}}).start();
                        }
                    } catch(Exception ignored){}
                }
                if(data==null){ex.sendResponseHeaders(502,-1);return;}
                ex.getResponseHeaders().set("Content-Type","image/png");
                ex.getResponseHeaders().set("Access-Control-Allow-Origin","*");
                ex.getResponseHeaders().set("Cache-Control","max-age=86400");
                ex.sendResponseHeaders(200,data.length);
                ex.getResponseBody().write(data);
                ex.getResponseBody().close();
            });
            mapProxy.createContext("/leaflet", ex -> {
                String path=ex.getRequestURI().getPath();
                byte[] data=null; String ct="text/plain";
                if(path.endsWith("leaflet.js")){data=leafletJsCache;ct="application/javascript";}
                if(path.endsWith("leaflet.css")){data=leafletCssCache;ct="text/css";}
                if(path.endsWith("leaflet.draw.js")){
                    try(java.io.InputStream is=getClass().getResourceAsStream("/Leaflet/leaflet.draw.js")){
                        if(is!=null)data=is.readAllBytes();
                    }catch(Exception ignored){}
                    ct="application/javascript";
                }
                if(path.endsWith("leaflet.draw.css")){
                    try(java.io.InputStream is=getClass().getResourceAsStream("/Leaflet/leaflet.draw.css")){
                        if(is!=null)data=is.readAllBytes();
                    }catch(Exception ignored){}
                    ct="text/css";
                }
                if(data!=null){
                    ex.getResponseHeaders().set("Content-Type",ct);
                    ex.getResponseHeaders().set("Access-Control-Allow-Origin","*");
                    ex.getResponseHeaders().set("Cache-Control","max-age=86400");
                    ex.sendResponseHeaders(200,data.length);
                    ex.getResponseBody().write(data);
                    ex.getResponseBody().close();
                } else { ex.sendResponseHeaders(404,-1); }
            });
            mapProxy.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(8));
            mapProxy.start();
        } catch(Exception e){ System.err.println("[AjouterIncendie] Proxy: "+e.getMessage()); }
    }

    private void arreterProxy() {
        if (mapProxy != null) { mapProxy.stop(0); mapProxy = null; }
    }

    private void fermerFenetre() {
        arreterProxy();
        ((Stage) mapView.getScene().getWindow()).close();
    }

    private String buildMapHtml() {
        // Démarrer le proxy avant de construire le HTML
        demarrerProxy();

        String geojson = "null";
        try {
            java.net.URL url = getClass().getResource("/map_web/tn-governorates.geojson");
            if (url != null)
                geojson = new String(url.openStream().readAllBytes(),
                        java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("GeoJSON non trouve: " + e.getMessage());
        }

        String tileUrl   = "http://127.0.0.1:" + mapProxyPort + "/tiles/{z}/{x}/{y}.png";
        String leafletJs = "http://127.0.0.1:" + mapProxyPort + "/leaflet/leaflet.js";

        // Même approche que buildPerimetreHtml() — boutons manuels, pas Leaflet.Draw
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>" +
            "<style>" +
            "* { box-sizing:border-box; margin:0; padding:0; }" +
            "html,body,#map { width:100%; height:100%; }" +
            ".leaflet-control-attribution { display:none !important; }" +
            ".leaflet-bar a{background-color:rgba(8,22,13,0.95)!important;color:#4ade80!important;border-color:rgba(74,222,128,0.2)!important}" +
            ".ctrl { position:absolute; top:14px; left:14px; z-index:1000;" +
            "  background:rgba(8,22,13,0.92); border-radius:14px; padding:12px 14px;" +
            "  min-width:175px; border:1px solid rgba(74,222,128,0.25);" +
            "  box-shadow:0 4px 24px rgba(0,0,0,0.5); }" +
            ".ctrl-sec { font-size:10px; color:rgba(74,222,128,0.55); letter-spacing:1px; margin:10px 0 5px; font-weight:700; text-transform:uppercase; }" +
            ".ctrl-sec:first-child { margin-top:0; }" +
            ".cb { display:block; width:100%; margin:3px 0; padding:8px 12px;" +
            "  background:rgba(74,222,128,0.08); color:#4ade80;" +
            "  border:1px solid rgba(74,222,128,0.2); border-radius:9px;" +
            "  cursor:pointer; font-size:12px; font-weight:600; text-align:left; transition:all 0.15s; }" +
            ".cb:hover { background:rgba(74,222,128,0.18); border-color:rgba(74,222,128,0.4); }" +
            ".cb.on { background:#16a34a; border-color:#16a34a; color:white; }" +
            ".cb.red { color:#fca5a5; border-color:rgba(239,68,68,0.3); background:rgba(239,68,68,0.08); }" +
            ".cb.red:hover { background:rgba(239,68,68,0.18); }" +
            ".sb { position:absolute; bottom:28px; left:50%; transform:translateX(-50%);" +
            "  z-index:1000; background:rgba(22,163,74,0.92); border-radius:20px;" +
            "  padding:8px 22px; font-size:12px; color:white; white-space:nowrap;" +
            "  box-shadow:0 2px 16px rgba(0,0,0,0.4); font-weight:600; }" +
            "</style></head><body>" +
            "<div id='map'></div>" +
            "<div class='ctrl'>" +
            "  <div class='ctrl-sec'>DESSIN ZONE</div>" +
            "  <button class='cb' id='bPlacer'>&#9654; Placer des points</button>" +
            "  <button class='cb' id='bFermer' style='opacity:0.35;cursor:not-allowed;'>&#9711; Fermer la zone</button>" +
            "  <button class='cb red' id='bReset'>&#128465; Reinitialiser</button>" +
            "</div>" +
            "<div class='sb' id='sb'>Activez Placer des points puis cliquez sur la carte.</div>" +
            "<script src='" + leafletJs + "'></script>" +
            "<script>" +
            "var GEOJSON=" + geojson + ";" +
            "var map=L.map('map',{zoomControl:true,attributionControl:false}).setView([33.88,9.54],6);" +
            "L.tileLayer('" + tileUrl + "',{maxZoom:19}).addTo(map);" +
            "if(GEOJSON){L.geoJson(GEOJSON,{" +
            "  style:function(f){return{fillColor:'#16a34a',weight:1.5,opacity:0.8,color:'#fff',dashArray:'3',fillOpacity:0.15};}," +
            "  onEachFeature:function(f,l){l.bindTooltip(f.properties.gouv_fr||'',{permanent:false,direction:'center',opacity:0.9});}" +
            "}).addTo(map);}" +
            "var pts=[],mkrs=[],poly=null,placing=false,closed=false;" +
            "function hav(a,b){var R=6371000,dLat=(b.lat-a.lat)*Math.PI/180,dLon=(b.lng-a.lng)*Math.PI/180;" +
            "  var x=Math.sin(dLat/2)*Math.sin(dLat/2)+Math.cos(a.lat*Math.PI/180)*Math.cos(b.lat*Math.PI/180)*Math.sin(dLon/2)*Math.sin(dLon/2);" +
            "  return R*2*Math.atan2(Math.sqrt(x),Math.sqrt(1-x));}" +
            "function polyArea(ll){var a=0,n=ll.length;" +
            "  for(var i=0;i<n;i++){var j=(i+1)%n;" +
            "    var xi=ll[i].lng*111320*Math.cos(ll[i].lat*Math.PI/180),yi=ll[i].lat*110540;" +
            "    var xj=ll[j].lng*111320*Math.cos(ll[j].lat*Math.PI/180),yj=ll[j].lat*110540;" +
            "    a+=xi*yj-xj*yi;}return Math.abs(a)/2;}" +
            "function centroid(ll){var la=0,ln=0,n=ll.length;for(var i=0;i<n;i++){la+=ll[i].lat;ln+=ll[i].lng;}return{lat:la/n,lng:ln/n};}" +
            "function pip(lat,lng,ring){var inside=false,n=ring.length;for(var i=0,j=n-1;i<n;j=i++){" +
            "  var xi=ring[i][0],yi=ring[i][1],xj=ring[j][0],yj=ring[j][1];" +
            "  if(((yi>lat)!=(yj>lat))&&(lng<(xj-xi)*(lat-yi)/(yj-yi)+xi))inside=!inside;}return inside;}" +
            "function findZone(lat,lng){if(!GEOJSON)return 1;" +
            "  for(var i=0;i<GEOJSON.features.length;i++){" +
            "    var f=GEOJSON.features[i],c=f.geometry.coordinates,t=f.geometry.type,hit=false;" +
            "    if(t==='Polygon')hit=pip(lat,lng,c[0]);" +
            "    else if(t==='MultiPolygon'){for(var p=0;p<c.length;p++)if(pip(lat,lng,c[p][0])){hit=true;break;}}" +
            "    if(hit)return f.properties.id||i+1;}return 1;}" +
            "function flamme(col,num){" +
            "  var inner = num===1 ? '&#9733;' : num;" + // étoile pour le 1er point
            "  return '<div style=\"position:relative;width:36px;height:44px;\">" +
            "<div style=\"width:36px;height:36px;border-radius:50% 50% 50% 0;background:'+col+';" +
            "transform:rotate(-45deg);box-shadow:0 4px 12px rgba(0,0,0,0.4);border:3px solid white;\"></div>" +
            "<div style=\"position:absolute;top:4px;left:4px;width:28px;height:28px;border-radius:50%;" +
            "background:white;display:flex;align-items:center;justify-content:center;" +
            "color:'+col+';font-weight:bold;font-size:13px;transform:rotate(45deg);\">' + num + '</div>" +
            "<div style=\"position:absolute;bottom:-4px;left:50%;transform:translateX(-50%);" +
            "width:8px;height:8px;border-radius:50%;background:rgba(0,0,0,0.25);\"></div>" +
            "</div>';" +
            "}" +
            "function draw(){" +
            "  mkrs.forEach(function(m){map.removeLayer(m);});mkrs=[];" +
            "  if(poly){map.removeLayer(poly);poly=null;}" +
            "  pts.forEach(function(p,i){" +
            "    var col = i===0 ? '#ff6b00' : '#ef4444';" +
            "    var ic=L.divIcon({html:flamme(col,i+1),iconSize:[36,44],iconAnchor:[18,44],className:''});" +
            "    mkrs.push(L.marker([p.lat,p.lng],{icon:ic}).addTo(map));});" +
            "  var ll=pts.map(function(p){return[p.lat,p.lng];});" +
            "  if(closed&&pts.length>=3)poly=L.polygon(ll,{color:'#ef4444',weight:2.5,fillColor:'#ef4444',fillOpacity:0.25}).addTo(map);" +
            "  else if(pts.length>=2)poly=L.polyline(ll,{color:'#ff9500',weight:2.5,dashArray:'8,5'}).addTo(map);}" +
            "function notifyJava(idZone,ha,gj){" +
            "  window._z=idZone;window._h=ha;window._g=gj;" +
            "  var msg='Zone '+idZone+' - '+ha.toFixed(2)+' ha';" +
            "  if(ha>50000)msg='ATTENTION: '+ha.toFixed(0)+' ha - verifiez le polygone';" +
            "  document.getElementById('sb').innerHTML=msg;" +
            "  setTimeout(function(){if(window.javaBridge)try{window.javaBridge.onZoneDefinie(window._z,window._h,window._g);}catch(e){}},100);}" +
            "function setStatus(m){document.getElementById('sb').innerHTML=m;" +
            "  if(window.javaBridge)setTimeout(function(){try{window.javaBridge.setStatus(m);}catch(e){}},100);}" +
            "map.on('click',function(e){" +
            "  if(!placing||closed)return;" +
            "  pts.push({lat:e.latlng.lat,lng:e.latlng.lng});draw();" +
            "  var m='Point '+pts.length+(pts.length>=3?' - vous pouvez fermer la zone':'');" +
            "  setStatus(m);" +
            "  var bf=document.getElementById('bFermer');" +
            "  if(pts.length>=3){bf.style.opacity='1';bf.style.cursor='pointer';bf.style.pointerEvents='auto';}" +
            "  else{bf.style.opacity='0.35';bf.style.cursor='not-allowed';bf.style.pointerEvents='none';}});" +
            "document.getElementById('bPlacer').onclick=function(){" +
            "  if(closed){setStatus('Zone fermee. Reinitialisez.');return;}" +
            "  placing=!placing;this.classList.toggle('on',placing);" +
            "  setStatus(placing?'Mode placement ACTIF - cliquez sur la carte':'Mode placement inactif');};" +
            "document.getElementById('bFermer').onclick=function(){" +
            "  if(pts.length<3)return;" +
            "  closed=true;placing=false;" +
            "  document.getElementById('bPlacer').classList.remove('on');" +
            "  this.style.opacity='0.35';this.style.cursor='not-allowed';this.style.pointerEvents='none';" +
            "  draw();" +
            "  var c=centroid(pts),ha=polyArea(pts)/10000,idZone=findZone(c.lat,c.lng);" +
            "  var gj=JSON.stringify({type:'Feature',geometry:{type:'Polygon',coordinates:[pts.map(function(p){return[p.lng,p.lat];})]}});" +
            "  notifyJava(idZone,ha,gj);};" +
            "document.getElementById('bReset').onclick=function(){" +
            "  pts=[];closed=false;placing=false;" +
            "  document.getElementById('bPlacer').classList.remove('on');" +
            "  var bf=document.getElementById('bFermer');" +
            "  bf.style.opacity='0.35';bf.style.cursor='not-allowed';bf.style.pointerEvents='none';" +
            "  draw();setStatus('Reinitialise. Placez de nouveaux points.');" +
            "  if(window.javaBridge)setTimeout(function(){try{window.javaBridge.onZoneEffacee();}catch(e){}},100);};" +
            "</script></body></html>";
    }
}

