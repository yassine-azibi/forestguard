package controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import service.AlerteService;
import service.AssistantVocalService;

import javax.sound.sampled.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class AssistantVocalController implements Initializable {

    @FXML private Label  lblEtat;
    @FXML private Label  lblTranscription;
    @FXML private Label  lblResultat;
    @FXML private Button btnMicro;
    @FXML private VBox   panelResultat;
    @FXML private Label  lblAction;
    @FXML private Label  lblType;
    @FXML private Label  lblNiveau;
    @FXML private Label  lblZone;
    @FXML private Button btnConfirmer;
    @FXML private Button btnAnnuler;
    @FXML private Label  lblLang;

    // Meme cle Gemini que RapportIAService
    private static final String GEMINI_KEY = "VOTRE_CLE_GEMINI";
    private static final String[] MODELES  = {
        "gemini-2.0-flash-exp", "gemini-2.0-flash",
        "gemini-1.5-flash",     "gemini-2.5-flash"
    };

    private final AssistantVocalService vocalService  = new AssistantVocalService();
    private final AlerteService         alerteService = new AlerteService();
    private Consumer<AssistantVocalService.CommandeVocale> onCommandeReconnue;
    private AssistantVocalService.CommandeVocale derniereCommande;

    private final AtomicBoolean enregistrement = new AtomicBoolean(false);
    private TargetDataLine      microLine;
    private ByteArrayOutputStream audioBuffer;

    public void setOnCommandeReconnue(Consumer<AssistantVocalService.CommandeVocale> cb) {
        this.onCommandeReconnue = cb;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        panelResultat.setVisible(false);
        panelResultat.setManaged(false);
        setEtat("Pret — Cliquez MIC pour parler", false);
    }

    @FXML
    private void toggleMicro() {
        if (enregistrement.get()) arreterEnregistrement();
        else demarrerEnregistrement();
    }

    private void demarrerEnregistrement() {
        try {
            AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            if (!AudioSystem.isLineSupported(info)) {
                setEtat("Micro non disponible", false);
                ouvrirSaisieManuelle();
                return;
            }
            microLine  = (TargetDataLine) AudioSystem.getLine(info);
            microLine.open(format);
            microLine.start();
            audioBuffer = new ByteArrayOutputStream();
            enregistrement.set(true);

            btnMicro.setText("STOP");
            btnMicro.setStyle(styleBtnStop());
            setEtat("Enregistrement... Parlez puis cliquez STOP", true);
            lblTranscription.setText("Parlez maintenant...");
            lblResultat.setText("");
            panelResultat.setVisible(false);
            panelResultat.setManaged(false);

            new Thread(() -> {
                byte[] buf = new byte[4096];
                while (enregistrement.get()) {
                    int n = microLine.read(buf, 0, buf.length);
                    if (n > 0) audioBuffer.write(buf, 0, n);
                }
                microLine.stop();
                microLine.close();

                byte[] audio = audioBuffer.toByteArray();

                Platform.runLater(() -> {
                    setEtat("Transcription en cours...", false);
                    btnMicro.setText("MIC");
                    btnMicro.setStyle(styleBtnMic());
                    lblTranscription.setText("Envoi a l'IA...");
                });

                // Transcription avec Gemini Audio
                String texte = transcrireAudio(audio,
                    new AudioFormat(16000, 16, 1, true, false));

                Platform.runLater(() -> {
                    if (texte != null && !texte.isBlank()) {
                        lblTranscription.setText("\"" + texte + "\"");
                        lblTranscription.setStyle(
                            "-fx-text-fill:white;-fx-font-size:13;-fx-font-weight:bold;");
                        traiterTexte(texte);
                    } else {
                        setEtat("Audio non reconnu — saisie manuelle", false);
                        lblTranscription.setText("");
                        ouvrirSaisieManuelle();
                    }
                });
            }, "audio-thread").start();

        } catch (Exception e) {
            e.printStackTrace();
            setEtat("Erreur micro : " + e.getMessage(), false);
            ouvrirSaisieManuelle();
        }
    }

    private void arreterEnregistrement() {
        enregistrement.set(false);
        setEtat("Traitement...", false);
    }

    // ── Transcription Gemini Audio ────────────────────────────────────────────
    private String transcrireAudio(byte[] pcm, AudioFormat fmt) {
        try {
            byte[] wav = toWav(pcm, fmt);
            String b64 = Base64.getEncoder().encodeToString(wav);

            String prompt = "Transcris exactement ce que dit cette personne. "
                + "La langue est le francais ou l arabe tunisien. "
                + "Reponds UNIQUEMENT avec le texte dit, sans ponctuation, "
                + "sans guillemets, sans explication. "
                + "Si tu n entends rien ou que c est du bruit, reponds juste: SILENCE";

            for (String modele : MODELES) {
                try {
                    String apiUrl =
                        "https://generativelanguage.googleapis.com/v1beta/models/"
                        + modele + ":generateContent?key=" + GEMINI_KEY;

                    String body =
                        "{\"contents\":[{\"parts\":["
                        + "{\"inline_data\":{\"mime_type\":\"audio/wav\","
                        + "\"data\":\"" + b64 + "\"}},"
                        + "{\"text\":\"" + escJson(prompt) + "\"}"
                        + "]}]}";

                    URL url = new URL(apiUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setDoOutput(true);
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setConnectTimeout(20000);
                    conn.setReadTimeout(20000);

                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(body.getBytes(StandardCharsets.UTF_8));
                    }

                    int code = conn.getResponseCode();
                    if (code == 200) {
                        String resp;
                        try (InputStream is = conn.getInputStream()) {
                            resp = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                        }
                        String txt = extraireTexte(resp);
                        System.out.println("Gemini transcrit [" + modele + "]: " + txt);
                        if (txt != null && !txt.equalsIgnoreCase("SILENCE")
                                && !txt.isBlank()) return txt;
                        return null;
                    } else {
                        try (InputStream es = conn.getErrorStream()) {
                            if (es != null) {
                                String err = new String(es.readAllBytes(), StandardCharsets.UTF_8);
                                System.out.println("Gemini " + code + ": " + err);
                            }
                        }
                        if (code == 429 || code == 503) continue; // quota -> essayer autre modele
                        break;
                    }
                } catch (Exception ex) {
                    System.out.println("Modele " + modele + " erreur: " + ex.getMessage());
                }
            }
        } catch (Exception e) {
            System.out.println("Transcription erreur: " + e.getMessage());
        }
        return null;
    }

    // ── Saisie manuelle ───────────────────────────────────────────────────────
    @FXML
    private void ouvrirSaisieManuelle() {
        TextInputDialog d = new TextInputDialog();
        d.setTitle("Commande ForestGuard");
        d.setHeaderText("Entrez votre commande");
        d.getEditor().setPromptText("Ex: Incendie critique a Kroumirie");

        Label hint = new Label(
            "Exemples :\n"
            + "  Incendie critique a Kroumirie\n"
            + "  Fumee haute Cap Bon\n"
            + "  Valider alerte 5\n"
            + "  Rejeter alerte 3\n"
            + "  حريق في الغابة");
        hint.setStyle("-fx-font-size:11;");
        d.getDialogPane().setExpandableContent(hint);
        d.getDialogPane().setExpanded(true);

        d.showAndWait().ifPresent(t -> {
            if (!t.isBlank()) {
                lblTranscription.setText("\"" + t + "\"");
                lblTranscription.setStyle("-fx-text-fill:white;-fx-font-size:13;");
                traiterTexte(t);
            }
        });
    }

    // ── Traiter le texte ──────────────────────────────────────────────────────
    private void traiterTexte(String texte) {
        setEtat("Analyse...", false);
        lblResultat.setText("Analyse en cours...");
        lblResultat.setStyle("-fx-text-fill:#c4b5fd;-fx-font-size:12;");
        new Thread(() -> {
            AssistantVocalService.CommandeVocale cmd = vocalService.interpreter(texte);
            Platform.runLater(() -> afficherCommande(cmd));
        }, "interpret").start();
    }

    // ── Afficher résultat ─────────────────────────────────────────────────────
    private void afficherCommande(AssistantVocalService.CommandeVocale cmd) {
        derniereCommande = cmd;
        setEtat("Commande analysee", false);
        lblResultat.setText("Compris : " + cmd.messageRetour);
        lblResultat.setStyle("-fx-text-fill:#4ade80;-fx-font-size:12;-fx-font-weight:bold;");
        panelResultat.setVisible(true);
        panelResultat.setManaged(true);

        switch (cmd.action) {
            case "AJOUTER_ALERTE" -> {
                lblAction.setText("Nouvelle Alerte");
                lblAction.setStyle("-fx-text-fill:#4ade80;-fx-font-size:15;-fx-font-weight:bold;");
                lblType.setText("Type   : " + (cmd.typeAlerte.isEmpty()   ? "Incendie" : cmd.typeAlerte));
                lblNiveau.setText("Niveau : " + (cmd.niveau.isEmpty()     ? "Haute"    : cmd.niveau));
                lblZone.setText("Zone   : " + (cmd.localisation.isEmpty() ? "A preciser" : cmd.localisation));
                btnConfirmer.setText("Enregistrer l'alerte");
                btnConfirmer.setStyle(styleBtnVert());
                btnConfirmer.setVisible(true); btnConfirmer.setManaged(true);
            }
            case "VALIDER" -> {
                lblAction.setText("Valider Alerte #" + cmd.idAlerte);
                lblAction.setStyle("-fx-text-fill:#4ade80;-fx-font-size:15;-fx-font-weight:bold;");
                lblType.setText(""); lblNiveau.setText("");
                lblZone.setText("ID : " + cmd.idAlerte);
                btnConfirmer.setText("Confirmer validation");
                btnConfirmer.setStyle(styleBtnVert());
                btnConfirmer.setVisible(true); btnConfirmer.setManaged(true);
            }
            case "REJETER" -> {
                lblAction.setText("Rejeter Alerte #" + cmd.idAlerte);
                lblAction.setStyle("-fx-text-fill:#ef4444;-fx-font-size:15;-fx-font-weight:bold;");
                lblType.setText(""); lblNiveau.setText("");
                lblZone.setText("ID : " + cmd.idAlerte);
                btnConfirmer.setText("Confirmer rejet");
                btnConfirmer.setStyle(styleBtnRouge());
                btnConfirmer.setVisible(true); btnConfirmer.setManaged(true);
            }
            default -> {
                lblAction.setText("Commande non reconnue — reessayez");
                lblAction.setStyle("-fx-text-fill:#64748b;-fx-font-size:13;");
                lblType.setText("Essayez : 'Incendie critique a Kroumirie'");
                lblNiveau.setText(""); lblZone.setText("");
                btnConfirmer.setVisible(false); btnConfirmer.setManaged(false);
            }
        }
    }

    @FXML
    private void confirmerCommande() {
        if (derniereCommande == null) return;
        switch (derniereCommande.action) {
            case "AJOUTER_ALERTE" -> {
                if (onCommandeReconnue != null) {
                    onCommandeReconnue.accept(derniereCommande);
                    fermer();
                } else {
                    model.Alerte a = new model.Alerte(
                        derniereCommande.typeAlerte.isEmpty() ? "Incendie" : derniereCommande.typeAlerte,
                        derniereCommande.niveau.isEmpty()     ? "Haute"    : derniereCommande.niveau,
                        derniereCommande.localisation.isEmpty() ? "Zone a preciser" : derniereCommande.localisation,
                        "Vocal");
                    if (alerteService.creerAlerte(a)) {
                        lblResultat.setText("Alerte enregistree !");
                        lblResultat.setStyle("-fx-text-fill:#4ade80;-fx-font-weight:bold;");
                        panelResultat.setVisible(false); panelResultat.setManaged(false);
                    }
                }
            }
            case "VALIDER" -> {
                if (derniereCommande.idAlerte > 0) alerteService.validerAlerte(derniereCommande.idAlerte);
                lblResultat.setText("Alerte #" + derniereCommande.idAlerte + " validee !");
                lblResultat.setStyle("-fx-text-fill:#4ade80;-fx-font-weight:bold;");
                panelResultat.setVisible(false); panelResultat.setManaged(false);
            }
            case "REJETER" -> {
                if (derniereCommande.idAlerte > 0) alerteService.rejeterAlerte(derniereCommande.idAlerte);
                lblResultat.setText("Alerte #" + derniereCommande.idAlerte + " rejetee !");
                lblResultat.setStyle("-fx-text-fill:#ef4444;-fx-font-weight:bold;");
                panelResultat.setVisible(false); panelResultat.setManaged(false);
            }
        }
        derniereCommande = null;
    }

    @FXML private void annulerCommande() {
        panelResultat.setVisible(false); panelResultat.setManaged(false);
        derniereCommande = null; lblResultat.setText("");
    }

    @FXML private void changerFR() {
        lblLang.setText("Langue : Francais");
        lblLang.setStyle("-fx-text-fill:#4ade80;-fx-font-size:10;");
    }
    @FXML private void changerAR() {
        lblLang.setText("Langue : Arabe");
        lblLang.setStyle("-fx-text-fill:#4ade80;-fx-font-size:10;");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void setEtat(String msg, boolean actif) {
        Platform.runLater(() -> {
            lblEtat.setText(msg);
            lblEtat.setStyle(actif
                ? "-fx-text-fill:#ef4444;-fx-font-weight:bold;-fx-font-size:13;"
                : "-fx-text-fill:#64748b;-fx-font-size:12;");
        });
    }

    private String extraireTexte(String resp) {
        try {
            int i = resp.indexOf("\"text\":");
            if (i == -1) return null;
            i = resp.indexOf("\"", i + 7) + 1;
            StringBuilder sb = new StringBuilder();
            while (i < resp.length()) {
                char ch = resp.charAt(i);
                if (ch == '\\' && i + 1 < resp.length()) {
                    char nx = resp.charAt(i + 1);
                    if (nx == '"') { sb.append('"'); i += 2; }
                    else if (nx == 'n') { sb.append('\n'); i += 2; }
                    else { sb.append(ch); i++; }
                } else if (ch == '"') break;
                else { sb.append(ch); i++; }
            }
            return sb.toString().trim();
        } catch (Exception e) { return null; }
    }

    private byte[] toWav(byte[] pcm, AudioFormat fmt) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        AudioInputStream ais = new AudioInputStream(
            new ByteArrayInputStream(pcm), fmt,
            pcm.length / fmt.getFrameSize());
        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, bos);
        return bos.toByteArray();
    }

    private String escJson(String s) {
        if (s == null) return "";
        return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n");
    }

    private String styleBtnMic() {
        return "-fx-background-color:#0d1f14;-fx-text-fill:#4ade80;" +
            "-fx-background-radius:50;-fx-min-width:90;-fx-min-height:90;" +
            "-fx-max-width:90;-fx-max-height:90;-fx-font-size:14;" +
            "-fx-font-weight:bold;-fx-cursor:hand;" +
            "-fx-border-color:#4ade80;-fx-border-radius:50;-fx-border-width:3;";
    }
    private String styleBtnStop() {
        return "-fx-background-color:#ef4444;-fx-text-fill:white;" +
            "-fx-background-radius:50;-fx-min-width:90;-fx-min-height:90;" +
            "-fx-max-width:90;-fx-max-height:90;-fx-font-size:14;" +
            "-fx-font-weight:bold;-fx-cursor:hand;-fx-border-width:0;" +
            "-fx-effect:dropshadow(gaussian,rgba(239,68,68,0.6),20,0,0,0);";
    }
    private String styleBtnVert() {
        return "-fx-background-color:#16a34a;-fx-text-fill:white;" +
            "-fx-background-radius:8;-fx-border-width:0;" +
            "-fx-padding:10 20;-fx-cursor:hand;-fx-font-size:12;-fx-font-weight:bold;";
    }
    private String styleBtnRouge() {
        return "-fx-background-color:#dc2626;-fx-text-fill:white;" +
            "-fx-background-radius:8;-fx-border-width:0;" +
            "-fx-padding:10 20;-fx-cursor:hand;-fx-font-size:12;-fx-font-weight:bold;";
    }
    private void fermer() { ((Stage) btnMicro.getScene().getWindow()).close(); }
}
