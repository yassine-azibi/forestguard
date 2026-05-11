package controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.concurrent.Worker;
import netscape.javascript.JSObject;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class CarteController implements Initializable {

    @FXML private WebView mapWebView;
    @FXML private ComboBox<String> regionCombo;
    @FXML private ComboBox<String> cityCombo;
    @FXML private Label latLabel;
    @FXML private Label lngLabel;
    @FXML private TextField forestNameField;
    @FXML private TextField forestAreaField;
    @FXML private TextArea forestDescField;
    @FXML private Button saveButton;
    @FXML private Label statusLabel;

    private WebEngine webEngine;
    private double currentLat = 36.365;
    private double currentLng = 6.6147;

    private Map<String, Map<String, double[]>> regionsData = new HashMap<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        System.out.println("[CarteController] Initialisation...");
        initRegionsData();
        setupRegionCombo();
        setupCityCombo();
        setupWebView();
        setupSaveButton();
    }

    private void initRegionsData() {
        Map<String, double[]> constantineCities = new HashMap<>();
        constantineCities.put("Constantine", new double[]{36.365, 6.6147});
        constantineCities.put("Bejaia", new double[]{36.751, 5.0643});
        constantineCities.put("Biskra", new double[]{34.850, 5.728});
        constantineCities.put("Tizi-Ouzou", new double[]{36.711, 4.050});
        constantineCities.put("Jijel", new double[]{36.820, 5.766});
        constantineCities.put("Skikda", new double[]{36.873, 6.909});

        Map<String, double[]> palmaCities = new HashMap<>();
        palmaCities.put("Sidi Bouzid", new double[]{35.038, 9.485});
        palmaCities.put("Tamanrasset", new double[]{22.785, 5.523});
        palmaCities.put("Oued Djerid", new double[]{33.900, 8.150});
        palmaCities.put("Ouargla", new double[]{31.950, 5.317});
        palmaCities.put("Timimoun", new double[]{29.263, 0.231});
        palmaCities.put("El Oued", new double[]{33.356, 6.863});

        regionsData.put("🌍 Constantine", constantineCities);
        regionsData.put("🌴 Palma & Sahara", palmaCities);
    }

    private void setupRegionCombo() {
        regionCombo.getItems().addAll("🌍 Constantine", "🌴 Palma & Sahara");
        regionCombo.setValue("🌍 Constantine");
        regionCombo.setOnAction(e -> updateCityList());
        updateCityList();
    }

    private void updateCityList() {
        String selectedRegion = regionCombo.getValue();
        if (selectedRegion != null && regionsData.containsKey(selectedRegion)) {
            cityCombo.getItems().clear();
            cityCombo.getItems().addAll(regionsData.get(selectedRegion).keySet());
            if (!cityCombo.getItems().isEmpty()) {
                cityCombo.setValue(cityCombo.getItems().get(0));
                updateMapFromCity();
            }
        }
    }

    private void setupCityCombo() {
        cityCombo.setOnAction(e -> updateMapFromCity());
    }

    private void updateMapFromCity() {
        String selectedRegion = regionCombo.getValue();
        String selectedCity = cityCombo.getValue();

        if (selectedRegion != null && selectedCity != null && regionsData.containsKey(selectedRegion)) {
            double[] coords = regionsData.get(selectedRegion).get(selectedCity);
            if (coords != null) {
                currentLat = coords[0];
                currentLng = coords[1];
                updateMapMarker();
                updateLabels();
            }
        }
    }

    private void setupWebView() {
        webEngine = mapWebView.getEngine();

        // Désactiver le cache pour éviter les problèmes
        webEngine.setJavaScriptEnabled(true);
        webEngine.loadContent(getHtmlMapContent());

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                System.out.println("[CarteController] Page chargée avec succès");
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaApp", this);
                webEngine.executeScript("updateMarker(" + currentLat + ", " + currentLng + ")");
            } else if (newState == Worker.State.FAILED) {
                System.err.println("[CarteController] Échec chargement page");
            }
        });
    }

    private void updateMapMarker() {
        if (webEngine != null) {
            webEngine.executeScript("updateMarker(" + currentLat + ", " + currentLng + ")");
        }
    }

    private void updateLabels() {
        latLabel.setText("Latitude : " + String.format("%.5f", currentLat));
        lngLabel.setText("Longitude : " + String.format("%.5f", currentLng));
    }

    // Appelée depuis JavaScript
    public void onMapClick(double lat, double lng) {
        currentLat = lat;
        currentLng = lng;
        javafx.application.Platform.runLater(() -> {
            updateLabels();
            System.out.println("[CarteController] Position mise à jour: " + lat + ", " + lng);
        });
    }

    private void setupSaveButton() {
        saveButton.setOnAction(e -> saveForest());
    }

    private void saveForest() {
        String name = forestNameField.getText().trim();
        String area = forestAreaField.getText().trim();
        String desc = forestDescField.getText().trim();

        if (name.isEmpty()) {
            showStatus("❌ Veuillez entrer un nom de forêt", "red");
            return;
        }

        String message = String.format("🌲 Forêt ajoutée !\n\nNom: %s\nPosition: %.5f, %.5f\nSurface: %s ha\nDescription: %s",
                name, currentLat, currentLng, area.isEmpty() ? "Non spécifiée" : area + " ha",
                desc.isEmpty() ? "Aucune" : desc);

        showStatus("✅ Forêt \"" + name + "\" enregistrée !", "green");

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText("Forêt ajoutée avec succès");
        alert.setContentText(message);
        alert.showAndWait();

        // Réinitialiser
        forestNameField.clear();
        forestAreaField.clear();
        forestDescField.clear();
    }

    private void showStatus(String message, String color) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: " + color + ";");
        new Thread(() -> {
            try { Thread.sleep(3000); } catch (InterruptedException e) {}
            javafx.application.Platform.runLater(() -> statusLabel.setText(""));
        }).start();
    }

    private String getHtmlMapContent() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                <style>
                    * { margin: 0; padding: 0; }
                    body { background: #e9f5e9; }
                    #map { height: 100vh; width: 100%; }
                </style>
            </head>
            <body>
                <div id="map"></div>
                <script>
                    var map = L.map('map').setView([36.365, 6.6147], 7);
                    L.tileLayer('https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png', {
                        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OSM</a>',
                        subdomains: 'abcd',
                        maxZoom: 18
                    }).addTo(map);
                    
                    var marker = L.marker([36.365, 6.6147]).addTo(map);
                    
                    function updateMarker(lat, lng) {
                        marker.setLatLng([lat, lng]);
                        map.setView([lat, lng], 10);
                        marker.bindPopup("<b>📍 Position</b><br>Lat: " + lat.toFixed(5) + "<br>Lng: " + lng.toFixed(5)).openPopup();
                    }
                    
                    map.on('click', function(e) {
                        var lat = e.latlng.lat;
                        var lng = e.latlng.lng;
                        updateMarker(lat, lng);
                        if (window.javaApp) {
                            window.javaApp.onMapClick(lat, lng);
                        }
                    });
                    
                    console.log("Carte Leaflet prête");
                </script>
            </body>
            </html>
        """;
    }
}
