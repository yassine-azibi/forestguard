package controller;

import javafx.application.Platform;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service Text-to-Speech utilisant PowerShell + System.Speech.Synthesis
 * (natif Windows, comme Cortana) — fonctionne sans dépendance externe.
 */
public class TextToSpeechService {

    private static TextToSpeechService instance;

    private float  rate   = 0f;    // -10 à +10 (0 = normal)
    private float  volume = 100f;  // 0 à 100
    private String lang   = "fr-FR";

    // Thread dédié pour ne pas bloquer l'UI
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "TTS-Thread");
        t.setDaemon(true);
        return t;
    });

    // Processus PowerShell persistant
    private Process psProcess;
    private BufferedWriter psWriter;

    private TextToSpeechService() {
        initPowerShell();
    }

    public static TextToSpeechService getInstance() {
        if (instance == null) instance = new TextToSpeechService();
        return instance;
    }

    private void initPowerShell() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "powershell.exe", "-NoProfile", "-NonInteractive", "-Command", "-"
            );
            pb.redirectErrorStream(true);
            psProcess = pb.start();
            psWriter  = new BufferedWriter(
                new OutputStreamWriter(psProcess.getOutputStream(), "UTF-8")
            );
            // Initialiser le synthétiseur une seule fois
            sendCommand("Add-Type -AssemblyName System.Speech");
            sendCommand("$tts = New-Object System.Speech.Synthesis.SpeechSynthesizer");
            sendCommand("$tts.Volume = 100");
            sendCommand("$tts.Rate = 0");
        } catch (Exception e) {
            System.err.println("TTS init PowerShell: " + e.getMessage());
        }
    }

    private void sendCommand(String cmd) {
        try {
            if (psWriter != null) {
                psWriter.write(cmd);
                psWriter.newLine();
                psWriter.flush();
            }
        } catch (Exception e) {
            System.err.println("TTS sendCommand: " + e.getMessage());
        }
    }

    /** Lit un texte à voix haute (asynchrone). */
    public void speak(String text) {
        if (text == null || text.isBlank()) return;
        executor.submit(() -> {
            try {
                // Arrêter la lecture en cours
                sendCommand("$tts.SpeakAsyncCancelAll()");
                Thread.sleep(100);

                // Appliquer les paramètres
                sendCommand("$tts.Rate = " + (int) rate);
                sendCommand("$tts.Volume = " + (int) volume);

                // Choisir la voix selon la langue
                String voiceCmd = getVoiceCommand();
                if (!voiceCmd.isEmpty()) sendCommand(voiceCmd);

                // Lire le texte — échapper les apostrophes
                String safe = text.replace("'", "''")
                                  .replace("\n", " ")
                                  .replace("\r", "");
                sendCommand("$tts.Speak('" + safe + "')");

            } catch (Exception e) {
                System.err.println("TTS speak: " + e.getMessage());
                // Fallback : nouvelle instance PowerShell
                speakFallback(text);
            }
        });
    }

    /** Fallback : lance un nouveau processus PowerShell si le persistant échoue. */
    private void speakFallback(String text) {
        try {
            String safe = text.replace("'", "''")
                              .replace("\n", " ")
                              .replace("\"", "");
            String script =
                "Add-Type -AssemblyName System.Speech; " +
                "$s = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                "$s.Rate = " + (int) rate + "; " +
                "$s.Volume = " + (int) volume + "; " +
                "$s.Speak('" + safe + "')";

            new ProcessBuilder(
                "powershell.exe", "-NoProfile", "-NonInteractive", "-Command", script
            ).start();
        } catch (Exception e) {
            System.err.println("TTS fallback: " + e.getMessage());
        }
    }

    /** Arrête la lecture en cours. */
    public void stop() {
        executor.submit(() -> sendCommand("$tts.SpeakAsyncCancelAll()"));
    }

    private String getVoiceCommand() {
        // Sélectionner la voix selon la langue installée sur Windows
        return switch (lang) {
            case "fr-FR" -> "$tts.SelectVoiceByHints([System.Globalization.CultureInfo]'fr-FR') 2>$null";
            case "en-US" -> "$tts.SelectVoiceByHints([System.Globalization.CultureInfo]'en-US') 2>$null";
            case "ar-SA" -> "$tts.SelectVoiceByHints([System.Globalization.CultureInfo]'ar-SA') 2>$null";
            default      -> "";
        };
    }

    // ── Setters ──────────────────────────────────────────────────────────────

    /** Vitesse : -10 (très lent) à +10 (très rapide), 0 = normal. */
    public TextToSpeechService setRate(float rate) {
        this.rate = Math.max(-10, Math.min(10, rate));
        return this;
    }

    public TextToSpeechService setVolume(float volume) {
        this.volume = Math.max(0, Math.min(100, volume));
        return this;
    }

    public TextToSpeechService setLang(String lang) {
        this.lang = lang;
        return this;
    }
}

