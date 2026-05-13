package ForestGuard.services;

import com.fazecast.jSerialComm.SerialPort;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Service de communication avec l'ESP32 via port série USB.
 *
 * Sketch Arduino attendu :
 *   Serial.begin(9600);
 *   Envoie JSON toutes les 5 secondes :
 *   {"capteur":"S001","zone":"Foret de Chenes","temperature":25.3,
 *    "humidite":60.0,"fumee":0,"flamme":false,"type":"Automatique"}
 */
public class ESP32Service {

    private SerialPort       port;
    private Thread           threadLecture;
    private volatile boolean enCours       = false;
    private volatile boolean donneesRecues = false;

    // ════════════════════════════════════════════════════════
    // Données reçues de l'ESP32
    // ════════════════════════════════════════════════════════
    public static class DonneesESP32 {
        public final String  capteur;
        public final String  zone;
        public final double  temperature;
        public final double  humidite;
        public final double  fumee;
        public final boolean flamme;
        public final String  type;
        public final java.time.LocalDateTime horodatage;

        public DonneesESP32(String capteur, String zone,
                            double temperature, double humidite,
                            double fumee, boolean flamme, String type) {
            this.capteur     = capteur;
            this.zone        = zone;
            this.temperature = temperature;
            this.humidite    = humidite;
            this.fumee       = fumee;
            this.flamme      = flamme;
            this.type        = type;
            this.horodatage  = java.time.LocalDateTime.now();
        }
    }

    // ════════════════════════════════════════════════════════
    // Lister les ports disponibles
    // ════════════════════════════════════════════════════════
    public static List<String> getPortsDisponibles() {
        List<String> ports = new ArrayList<>();
        for (SerialPort p : SerialPort.getCommPorts())
            ports.add(p.getSystemPortName() + " — " + p.getDescriptivePortName());
        return ports;
    }

    /** Retourne les ports avec COM5 en premier */
    public static String[] getNomsPortsSysteme() {
        SerialPort[] ports = SerialPort.getCommPorts();
        List<String> noms = new ArrayList<>();
        for (SerialPort p : ports)
            if (p.getSystemPortName().equalsIgnoreCase("COM5"))
                noms.add(0, p.getSystemPortName());
            else
                noms.add(p.getSystemPortName());
        System.out.println("[ESP32] Ports détectés : " + noms);
        return noms.toArray(new String[0]);
    }

    // ════════════════════════════════════════════════════════
    // Connexion avec flush du buffer de démarrage
    // ════════════════════════════════════════════════════════
    public boolean connecter(String nomPort, int baudRate) {
        return connecterAvecFlush(nomPort, baudRate);
    }

    public boolean connecterAvecFlush(String nomPort, int baudRate) {
        try {
            // Fermer proprement si déjà ouvert
            if (port != null && port.isOpen()) {
                enCours = false;
                port.closePort();
                Thread.sleep(500);
            }

            port = SerialPort.getCommPort(nomPort);
            port.setBaudRate(baudRate);
            port.setNumDataBits(8);
            port.setNumStopBits(SerialPort.ONE_STOP_BIT);
            port.setParity(SerialPort.NO_PARITY);
            port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 3000, 0);

            if (!port.openPort()) {
                System.out.println("[ESP32]  Impossible d'ouvrir : " + nomPort + " @ " + baudRate);
                return false;
            }

            System.out.println("[ESP32]  Port ouvert : " + nomPort + " @ " + baudRate + " bauds");

            // Attendre le reset DTR de l'ESP32 (2.5s)
            Thread.sleep(3500);

            // Vider le buffer de démarrage (messages parasites avant le JSON)
            // Vider tout le buffer de démarrage en boucle
            System.out.println("[ESP32] Vidage buffer démarrage...");
            long deadline = System.currentTimeMillis() + 1000;
            byte[] tmp = new byte[256];
            int totalVide = 0;
            while (System.currentTimeMillis() < deadline) {
                int n = port.readBytes(tmp, tmp.length);
                if (n <= 0) break;
                totalVide += n;
            }
            if (totalVide > 0)
                System.out.println("[ESP32] Buffer vidé : " + totalVide + " octets ignorés au démarrage");

            return true;

        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            System.out.println("[ESP32] ❌ Erreur connecter(" + nomPort + "@" + baudRate + ") : " + e.getMessage());
            return false;
        }
    }

    // ════════════════════════════════════════════════════════
    // Lecture en continu
    // ════════════════════════════════════════════════════════
    public void demarrerLecture(Consumer<DonneesESP32> callback,
                                Consumer<String> onErreur) {
        if (port == null || !port.isOpen()) {
            if (onErreur != null) onErreur.accept("Port non connecté");
            return;
        }

        enCours = true;

        threadLecture = new Thread(() -> {
            StringBuilder buffer = new StringBuilder();
            System.out.println("[ESP32] Thread lecture démarré — " + port.getSystemPortName());

            while (enCours && port.isOpen()) {
                try {
                    byte[] data = new byte[512];
                    int nb = port.readBytes(data, data.length);

                    if (nb > 0) {
                        String recu = new String(data, 0, nb, "UTF-8");
                        buffer.append(recu);
                        System.out.print("[ESP32 RAW] " + recu);

                        int idx;
                        while ((idx = buffer.indexOf("\n")) >= 0) {
                            String ligne = buffer.substring(0, idx).trim();
                            buffer.delete(0, idx + 1);

                            if (ligne.isEmpty()) continue;

                            // Ignorer les messages texte non-JSON
                            if (!ligne.startsWith("{")) {
                                System.out.println("[ESP32 MSG] " + ligne);
                                continue;
                            }

                            DonneesESP32 d = parseJSON(ligne);
                            if (d != null && callback != null) {
                                donneesRecues = true;
                                javafx.application.Platform.runLater(() -> callback.accept(d));
                            }
                        }
                    } else {
                        Thread.sleep(50);
                    }

                } catch (InterruptedException ie) {
                    System.out.println("[ESP32] Thread interrompu proprement");
                    break;
                } catch (Exception e) {
                    if (enCours) {
                        System.out.println("[ESP32] ❌ Erreur lecture : " + e.getMessage());
                        if (onErreur != null)
                            javafx.application.Platform.runLater(
                                    () -> onErreur.accept(e.getMessage()));
                    }
                    break;
                }
            }
            System.out.println("[ESP32] Thread lecture terminé");
        });

        threadLecture.setDaemon(true);
        threadLecture.setName("ESP32-Reader");
        threadLecture.start();
    }

    // ════════════════════════════════════════════════════════
    // Parser JSON — compatible avec le sketch Arduino fourni
    // ════════════════════════════════════════════════════════
    private DonneesESP32 parseJSON(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            System.out.println("[ESP32] ✅ JSON reçu : " + json);
            return new DonneesESP32(
                    obj.optString("capteur",     "S001"),
                    obj.optString("zone",        "Foret de Chenes"),
                    obj.optDouble("temperature", 0),
                    obj.optDouble("humidite",    0),
                    obj.optDouble("fumee",       0),
                    obj.optBoolean("flamme",     false),
                    obj.optString("type",        "Automatique")
            );
        } catch (Exception e) {
            System.out.println("[ESP32] ❌ JSON invalide : [" + json + "]");
            return null;
        }
    }

    // ════════════════════════════════════════════════════════
    // Déconnexion propre
    // ════════════════════════════════════════════════════════
    public void deconnecter() {
        enCours       = false;


        if (threadLecture != null && threadLecture.isAlive()) {
            threadLecture.interrupt();
            try { threadLecture.join(2000); } catch (InterruptedException ignored) {}
        }

        if (port != null && port.isOpen()) {
            port.closePort();
            System.out.println("[ESP32] 🔌 Déconnecté de " + port.getSystemPortName());
        }
    }

    // ════════════════════════════════════════════════════════
    // Getters
    // ════════════════════════════════════════════════════════
    public boolean estConnecte()   { return port != null && port.isOpen(); }
    public boolean aRecuDonnees()  { return donneesRecues; }
    public String  getPortActuel() { return port != null ? port.getSystemPortName() : "Aucun"; }
}
