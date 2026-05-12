package controller;

import model.Alerte;
import service.AlerteService;
import service.AnalyseAlerteIA;
import utils.NavigationManager;

import javafx.collections.FXCollections;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

import java.net.URL;
import java.util.ResourceBundle;

public class AjoutAlerteController implements Initializable {

    @FXML private ComboBox<String> cbType;
    @FXML private ComboBox<String> cbNiveau;
    @FXML private TextField        tfLocalisation;
    @FXML private TextField        tfLat;
    @FXML private TextField        tfLng;
    @FXML private Label            lblMessage;
    @FXML private Label            lblCoordonnees;
    @FXML private Label            lblErrType;
    @FXML private Label            lblErrNiveau;
    @FXML private Label            lblErrLocalisation;
    @FXML private WebView          carteView;

    private final AlerteService    service   = new AlerteService();
    private final AnalyseAlerteIA  ia        = new AnalyseAlerteIA();
    private WebEngine engine;
    private JavaBridge bridge;

    private static final String FIELD_OK =
        "-fx-background-color:#1a2e1e;-fx-background-radius:8;" +
        "-fx-border-color:#2d5a37;-fx-border-radius:8;-fx-border-width:1;" +
        "-fx-font-size:13;-fx-text-fill:white;-fx-prompt-text-fill:#64748b;-fx-padding:9 12;";
    private static final String FIELD_ERR =
        "-fx-background-color:#1a2e1e;-fx-background-radius:8;" +
        "-fx-border-color:#EF9A9A;-fx-border-radius:8;-fx-border-width:2;" +
        "-fx-font-size:13;-fx-text-fill:white;-fx-prompt-text-fill:#64748b;-fx-padding:9 12;";
    private static final String CB_OK =
        "-fx-background-color:#1a2e1e;-fx-background-radius:8;" +
        "-fx-border-color:#2d5a37;-fx-border-radius:8;-fx-border-width:1;-fx-font-size:13;";
    private static final String CB_ERR =
        "-fx-background-color:#1a2e1e;-fx-background-radius:8;" +
        "-fx-border-color:#EF9A9A;-fx-border-radius:8;-fx-border-width:2;-fx-font-size:13;";
    private static final String ERR_LABEL =
        "-fx-text-fill:#EF9A9A;-fx-font-size:10;-fx-font-weight:bold;";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cbType.setItems(FXCollections.observableArrayList(
                "Incendie", "Fumee", "Chaleur excessive", "Secheresse"));
        cbNiveau.setItems(FXCollections.observableArrayList(
                "Critique", "Haute", "Moyenne"));
        cbType.setOnAction(e -> clearError(cbType, lblErrType, CB_OK));
        cbNiveau.setOnAction(e -> clearError(cbNiveau, lblErrNiveau, CB_OK));
        tfLocalisation.textProperty().addListener((o, ov, nv) -> {
            if (!nv.trim().isEmpty()) clearFieldError(tfLocalisation, lblErrLocalisation);
        });
        chargerCarte();
    }

    // ── Validation ────────────────────────────────────────────────────────────
    private boolean valider() {
        boolean ok = true;
        if (cbType.getValue() == null || cbType.getValue().isEmpty()) {
            setError(cbType, lblErrType, "Veuillez choisir un type d'alerte", CB_ERR);
            ok = false;
        } else { clearError(cbType, lblErrType, CB_OK); }

        if (cbNiveau.getValue() == null || cbNiveau.getValue().isEmpty()) {
            setError(cbNiveau, lblErrNiveau, "Veuillez choisir un niveau de gravite", CB_ERR);
            ok = false;
        } else { clearError(cbNiveau, lblErrNiveau, CB_OK); }

        String loc = tfLocalisation.getText().trim();
        if (loc.isEmpty()) {
            setFieldError(tfLocalisation, lblErrLocalisation, "Cliquez sur la carte ou saisissez une localisation");
            ok = false;
        } else if (loc.length() < 3) {
            setFieldError(tfLocalisation, lblErrLocalisation, "La localisation doit contenir au moins 3 caracteres");
            ok = false;
        } else if (loc.length() > 150) {
            setFieldError(tfLocalisation, lblErrLocalisation, "La localisation ne peut pas depasser 150 caracteres");
            ok = false;
        } else { clearFieldError(tfLocalisation, lblErrLocalisation); }
        return ok;
    }

    private void setError(ComboBox<?> cb, Label lbl, String msg, String style) {
        cb.setStyle(style);
        if (lbl != null) { lbl.setText("⚠ " + msg); lbl.setStyle(ERR_LABEL); }
    }
    private void clearError(ComboBox<?> cb, Label lbl, String style) {
        cb.setStyle(style);
        if (lbl != null) lbl.setText("");
    }
    private void setFieldError(TextField tf, Label lbl, String msg) {
        tf.setStyle(FIELD_ERR);
        if (lbl != null) { lbl.setText("⚠ " + msg); lbl.setStyle(ERR_LABEL); }
    }
    private void clearFieldError(TextField tf, Label lbl) {
        tf.setStyle(FIELD_OK);
        if (lbl != null) lbl.setText("");
    }

    // ── Carte Leaflet ─────────────────────────────────────────────────────────
    private void chargerCarte() {
        engine = carteView.getEngine();
        engine.setJavaScriptEnabled(true);

        String html =
            "<!DOCTYPE html><html><head><meta charset='utf-8'/>" +
            "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>" +
            "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>" +
            "<style>*{margin:0;padding:0;box-sizing:border-box;}" +
            "html,body{width:100%;height:100%;overflow:hidden;background:#0d1f14;}" +
            "#map{position:absolute;top:0;left:0;width:100%;height:100%;}" +
            ".leaflet-tile{opacity:1!important;-webkit-transform:translateZ(0);}" +
            ".leaflet-tile-container{opacity:1!important;}" +
            "</style></head><body><div id='map'></div><script>" +
            "var map,marker;" +
            "function reverseGeocode(lat,lng,cb){" +
            "  var xhr=new XMLHttpRequest();" +
            "  xhr.open('GET','https://nominatim.openstreetmap.org/reverse?lat='+lat+'&lon='+lng+'&format=json',true);" +
            "  xhr.setRequestHeader('Accept-Language','fr');" +
            "  xhr.timeout=6000;" +
            "  xhr.onreadystatechange=function(){if(xhr.readyState===4){" +
            "    if(xhr.status===200){try{var d=JSON.parse(xhr.responseText);" +
            "      var a=d.address||{};var v=a.city||a.town||a.village||a.county||a.state||'Zone inconnue';" +
            "      var g=a.state||'';cb((g&&g!==v)?v+', '+g:v);" +
            "    }catch(e){cb('Zone '+lat+', '+lng);}}" +
            "    else cb('Zone '+lat+', '+lng);" +
            "  }};" +
            "  xhr.ontimeout=function(){cb('Zone '+lat+', '+lng);};" +
            "  xhr.onerror=function(){cb('Zone '+lat+', '+lng);};" +
            "  xhr.send();}" +
            "function initMap(){" +
            "  map=L.map('map',{center:[33.8869,9.5375],zoom:7,zoomControl:true});" +
            "  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'," +
            "  {attribution:'(c) OpenStreetMap',maxZoom:18,crossOrigin:true}).addTo(map);" +
            "  var redIcon=L.icon({iconUrl:'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon.png'," +
            "  shadowUrl:'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png'," +
            "  iconSize:[25,41],iconAnchor:[12,41],popupAnchor:[1,-34]});" +
            "  setTimeout(function(){map.invalidateSize(true);},200);" +
            "  setTimeout(function(){map.invalidateSize(true);},600);" +
            "  setTimeout(function(){map.invalidateSize(true);},1200);" +
            "  map.on('click',function(e){" +
            "    var lat=e.latlng.lat.toFixed(5);var lng=e.latlng.lng.toFixed(5);" +
            "    if(marker){map.removeLayer(marker);marker=null;}" +
            "    marker=L.marker([lat,lng],{icon:redIcon}).addTo(map);" +
            "    marker.bindPopup('Chargement...').openPopup();" +
            "    reverseGeocode(lat,lng,function(loc){" +
            "      if(marker)marker.setPopupContent('<b style=\"color:#16a34a\">'+loc+'</b><br>Lat:'+lat+' Lng:'+lng);" +
            "      if(window.javaBridge&&window.javaBridge.onLocationSelected){" +
            "        try{window.javaBridge.onLocationSelected(loc,lat,lng);}catch(ex){}" +
            "      }});" +
            "  });}" +
            "if(document.readyState==='complete'){initMap();}else{window.addEventListener('load',initMap);}" +
            "</script></body></html>";

        engine.loadContent(html);
        engine.getLoadWorker().stateProperty().addListener((obs, o, n) -> {
            if (n == Worker.State.SUCCEEDED) {
                bridge = new JavaBridge();
                JSObject win = (JSObject) engine.executeScript("window");
                win.setMember("javaBridge", bridge);
                engine.executeScript("setTimeout(function(){if(typeof map!='undefined')map.invalidateSize(true);},400);");
                chargerForetsSurCarte();
            }
        });
    }

    public class JavaBridge {
        public void onLocationSelected(String localisation, String lat, String lng) {
            javafx.application.Platform.runLater(() -> {
                tfLocalisation.setText(localisation);
                tfLat.setText(lat);
                tfLng.setText(lng);
                lblCoordonnees.setText("GPS: " + lat + ", " + lng);
                clearFieldError(tfLocalisation, lblErrLocalisation);
                lblMessage.setText("");
            });
        }
    }

    // ── Ajouter alerte ────────────────────────────────────────────────────────
    @FXML
    private void handleAjouter() {
        lblMessage.setText("");
        if (!valider()) {
            lblMessage.setText("Corrigez les erreurs avant d'enregistrer.");
            lblMessage.setStyle("-fx-text-fill:#EF9A9A;-fx-font-weight:bold;-fx-font-size:12;");
            return;
        }
        String loc = tfLocalisation.getText().trim();
        String lat = tfLat.getText().trim();
        String lng = tfLng.getText().trim();
        if (!lat.isEmpty() && !lng.isEmpty()) loc = loc + " [" + lat + ", " + lng + "]";
        Alerte a = new Alerte(cbType.getValue(), cbNiveau.getValue(), loc, "Manuelle");
        if (service.creerAlerte(a)) {
            lblMessage.setText("Alerte enregistree ! Analyse IA en cours...");
            lblMessage.setStyle("-fx-text-fill:#4ade80;-fx-font-weight:bold;-fx-font-size:12;");
            viderFormulaire();
            final Alerte alerteFinale = a;
            new Thread(() -> {
                AnalyseAlerteIA.AnalyseResultat res = ia.analyser(alerteFinale);
                javafx.application.Platform.runLater(() -> afficherPopupIA(res, alerteFinale));
            }, "ia-analyse").start();
        } else {
            lblMessage.setText("Erreur base de donnees. Verifiez WampServer.");
            lblMessage.setStyle("-fx-text-fill:#EF9A9A;-fx-font-weight:bold;-fx-font-size:12;");
        }
    }


    // ── Popup Analyse IA ─────────────────────────────────────────────────────
    private void afficherPopupIA(AnalyseAlerteIA.AnalyseResultat r, Alerte a) {
        javafx.stage.Stage popup = new javafx.stage.Stage();
        popup.setTitle("Analyse IA — " + a.getTypeAlerte() + " | " + a.getNiveau());
        popup.initOwner(tfLocalisation.getScene().getWindow());

        String couleur = switch (a.getNiveau()) {
            case "Critique" -> "#ef4444";
            case "Haute"    -> "#f97316";
            default         -> "#3b82f6";
        };

        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(0);
        root.setStyle("-fx-background-color:#0a1612;");

        // Header
        javafx.scene.layout.HBox header = new javafx.scene.layout.HBox(12);
        header.setStyle("-fx-background-color:" + couleur + ";-fx-padding:16 24;");
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        javafx.scene.control.Label ico = new javafx.scene.control.Label("🧠");
        ico.setStyle("-fx-font-size:26;");
        javafx.scene.layout.VBox ht = new javafx.scene.layout.VBox(3);
        javafx.scene.control.Label titre = new javafx.scene.control.Label("Analyse IA — Rapport d'Intervention");
        titre.setStyle("-fx-text-fill:white;-fx-font-size:16;-fx-font-weight:bold;");
        javafx.scene.control.Label sub = new javafx.scene.control.Label(
            a.getTypeAlerte() + " | " + a.getNiveau() + " | " +
            a.getLocalisation().replaceAll("\\[.*\\]","").trim());
        sub.setStyle("-fx-text-fill:rgba(255,255,255,0.85);-fx-font-size:11;");
        ht.getChildren().addAll(titre, sub);
        header.getChildren().addAll(ico, ht);

        // Contenu
        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(10);
        content.setStyle("-fx-padding:16;");

        // Grille meteo + temps
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10); grid.setVgap(10);
        ajCarte(grid, 0, 0, "🌡 Meteo & Conditions",    r.meteo,             "#ef4444");
        ajCarte(grid, 1, 0, "⏱ Temps d'Intervention",  r.tempsIntervention, "#f97316");
        ajCarte(grid, 0, 1, "⚠ Risque de Propagation", r.risquePropagation, "#fbbf24");
        ajCarte(grid, 1, 1, "🗺 Zones Voisines",        r.zonesVoisines,     "#6366f1");
        javafx.scene.layout.ColumnConstraints cc = new javafx.scene.layout.ColumnConstraints(270);
        grid.getColumnConstraints().addAll(cc, cc);
        content.getChildren().add(grid);

        // Pompiers
        javafx.scene.layout.HBox pompBox = new javafx.scene.layout.HBox(12);
        pompBox.setStyle("-fx-background-color:#052e16;-fx-background-radius:10;" +
            "-fx-border-color:#16a34a;-fx-border-radius:10;-fx-border-width:1;-fx-padding:14;");
        pompBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        javafx.scene.control.Label pompIco = new javafx.scene.control.Label("🚒");
        pompIco.setStyle("-fx-font-size:28;");
        javafx.scene.layout.VBox pt = new javafx.scene.layout.VBox(4);
        javafx.scene.control.Label pnb = new javafx.scene.control.Label(r.nbPompiers);
        pnb.setStyle("-fx-text-fill:#4ade80;-fx-font-size:16;-fx-font-weight:bold;");
        pnb.setWrapText(true);
        pt.getChildren().add(pnb);
        pompBox.getChildren().addAll(pompIco, pt);
        content.getChildren().add(pompBox);

        ajSection(content, "🚛 Materiel Necessaire",       r.materiel,          "#f97316");
        ajSection(content, "📋 Plan d'Intervention",       r.planIntervention,  "#4ade80");

        // Bouton fermer
        javafx.scene.control.Button btnF = new javafx.scene.control.Button("Fermer");
        btnF.setStyle("-fx-background-color:#16a34a;-fx-text-fill:white;" +
            "-fx-background-radius:8;-fx-border-width:0;-fx-padding:10 30;" +
            "-fx-font-size:13;-fx-font-weight:bold;-fx-cursor:hand;");
        btnF.setOnAction(e -> popup.close());
        javafx.scene.layout.HBox footer = new javafx.scene.layout.HBox(btnF);
        footer.setAlignment(javafx.geometry.Pos.CENTER);
        footer.setStyle("-fx-padding:14;-fx-border-color:#1a3a20;-fx-border-width:1 0 0 0;");

        javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(content);
        scroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;-fx-border-color:transparent;");
        scroll.setFitToWidth(true);

        root.getChildren().addAll(header, scroll, footer);
        javafx.scene.layout.VBox.setVgrow(scroll, javafx.scene.layout.Priority.ALWAYS);

        javafx.scene.Scene scene = new javafx.scene.Scene(root, 600, 640);
        popup.setScene(scene);
        popup.show();
    }

    private void ajCarte(javafx.scene.layout.GridPane g, int col, int row,
                          String label, String val, String couleur) {
        javafx.scene.layout.VBox card = new javafx.scene.layout.VBox(5);
        card.setStyle("-fx-background-color:#0d1f14;-fx-background-radius:10;" +
            "-fx-border-color:" + couleur + "55;-fx-border-radius:10;-fx-border-width:1;" +
            "-fx-padding:12;");
        javafx.scene.control.Label lbl = new javafx.scene.control.Label(label);
        lbl.setStyle("-fx-text-fill:" + couleur + ";-fx-font-size:11;-fx-font-weight:bold;");
        javafx.scene.control.Label v = new javafx.scene.control.Label(val);
        v.setStyle("-fx-text-fill:#e2e8f0;-fx-font-size:11;");
        v.setWrapText(true);
        card.getChildren().addAll(lbl, v);
        g.add(card, col, row);
    }

    private void ajSection(javafx.scene.layout.VBox parent, String titre,
                             String contenu, String couleur) {
        javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(6);
        box.setStyle("-fx-background-color:#0d1f14;-fx-background-radius:10;" +
            "-fx-border-color:" + couleur + "55;-fx-border-radius:10;-fx-border-width:1;" +
            "-fx-padding:12;");
        javafx.scene.control.Label t = new javafx.scene.control.Label(titre);
        t.setStyle("-fx-text-fill:" + couleur + ";-fx-font-size:12;-fx-font-weight:bold;");
        javafx.scene.control.Label ct = new javafx.scene.control.Label(contenu);
        ct.setStyle("-fx-text-fill:#e2e8f0;-fx-font-size:11;");
        ct.setWrapText(true);
        box.getChildren().addAll(t, ct);
        parent.getChildren().add(box);
    }


    private void viderFormulaire() {
        cbType.setValue(null);
        cbNiveau.setValue(null);
        tfLocalisation.clear();
        tfLat.clear();
        tfLng.clear();
        lblCoordonnees.setText("Aucune zone selectionnee");
        cbType.setStyle(CB_OK);
        cbNiveau.setStyle(CB_OK);
        tfLocalisation.setStyle(FIELD_OK);
        if (lblErrType != null)         lblErrType.setText("");
        if (lblErrNiveau != null)       lblErrNiveau.setText("");
        if (lblErrLocalisation != null) lblErrLocalisation.setText("");
        try {
            engine.executeScript(
                "if(typeof marker!='undefined'&&marker){map.removeLayer(marker);marker=null;}");
        } catch (Exception ignored) {}
    }

    @FXML
    private void handleEffacer() {
        viderFormulaire();
        lblMessage.setText("");
    }

    @FXML private void goDashboard() {
        Stage s = (Stage) tfLocalisation.getScene().getWindow();
        NavigationManager.navigateTo(s, "/fxml/Dashboard.fxml");
    }
    @FXML private void goCapteurs() {
        Stage s = (Stage) tfLocalisation.getScene().getWindow();
        NavigationManager.navigateTo(s, "/fxml/Capteurs.fxml");
    }

    // ── Forets sur la carte depuis la BDD ─────────────────────────────────────
    private void chargerForetsSurCarte() {
        new Thread(() -> {
            dao.ForetDAO foretDao = new dao.ForetDAO();
            java.util.List<model.Foret> forets = foretDao.getAll();
            if (forets.isEmpty()) return;

            StringBuilder sb = new StringBuilder("(function(){");
            for (model.Foret f : forets) {
                double lat = f.getLatitude();
                double lng = f.getLongitude();
                if (lat == 0 && lng == 0) continue;

                String nom  = f.getNom() != null ? f.getNom().replaceAll("['\"]", "") : "";
                String loc  = f.getLocalisation() != null ? f.getLocalisation().replaceAll("['\"]", "") : "";
                String veg  = f.getTypeVegetation() != null ? f.getTypeVegetation() : "";
                String risq = f.getNiveauRisque() != null ? f.getNiveauRisque() : "Moyen";
                String sup  = String.format("%.0f", f.getSuperficie());
                int    id   = f.getId();
                int    rad  = (int)(Math.sqrt(f.getSuperficie()) * 100);

                String col = risq.equals("Critique") ? "#ef4444"
                           : (risq.contains("lev") || risq.equals("Eleve")) ? "#f97316"
                           : "#4ade80";

                String popup = "<b>" + nom + "</b><br>"
                             + loc + "<br>"
                             + sup + " ha - " + veg + "<br>"
                             + "Risque: " + risq;

                String label = "<div style=background:" + col
                             + ";color:white;padding:3px 8px;border-radius:10px;"
                             + "font-size:11px;font-weight:bold>" + nom + "</div>";

                sb.append("try{")
                  .append("L.circle([").append(lat).append(",").append(lng).append("],{")
                  .append("color:'").append(col).append("',")
                  .append("fillColor:'").append(col).append("',")
                  .append("fillOpacity:0.15,")
                  .append("radius:").append(rad).append(",")
                  .append("weight:2.5,")
                  .append("dashArray:'8 4'")
                  .append("}).addTo(map).bindPopup('").append(popup).append("');")
                  .append("L.marker([").append(lat).append(",").append(lng).append("],{")
                  .append("icon:L.divIcon({html:'").append(label).append("',")
                  .append("iconAnchor:[0,0],className:''})}")
                  .append(").addTo(map);")
                  .append("}catch(ex){}");
            }
            sb.append("})();");
            final String script = sb.toString();
            javafx.application.Platform.runLater(() -> {
                try { engine.executeScript(script); }
                catch (Exception e) { System.out.println("Forets map: " + e.getMessage()); }
            });
        }, "forets-map").start();
    }
}
