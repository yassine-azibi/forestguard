package edu.pompier.tools;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.function.Consumer;

/**
 * Service d'authentification par reconnaissance faciale.
 * Délègue à un script Python (face_auth.py) via ProcessBuilder.
 * Aucune dépendance Java supplémentaire requise.
 */
public class FaceAuthService {

    public enum ResultatAuth {
        SUCCES, ECHEC, PAS_DE_VISAGE, ERREUR_CAMERA, ERREUR_REFERENCE, TIMEOUT, ERREUR_PYTHON
    }

    public static class ResultatAuthentification {
        public final ResultatAuth statut;
        public final double       similarite;
        public final String       message;

        public ResultatAuthentification(ResultatAuth s, double sim, String msg) {
            statut = s; similarite = sim; message = msg;
        }
    }

    // ── Authentification ────────────────────────────────────────
    /**
     * Lance le script Python face_auth.py et retourne le résultat.
     * Appelé dans un thread séparé — callback sur le thread JavaFX.
     */
    public void authentifier(Consumer<ResultatAuthentification> callback) {
        Thread t = new Thread(() -> {
            try {
                // Trouver la photo de référence
                String photoRef = trouverPhotoRef();
                if (photoRef == null) {
                    notifier(callback, ResultatAuth.ERREUR_REFERENCE, 0,
                            "Photo admin.jpg introuvable.\nAjoutez votre photo dans src/main/resources/admin.jpg");
                    return;
                }

                // Trouver le script Python
                String scriptPath = trouverScript();
                if (scriptPath == null) {
                    notifier(callback, ResultatAuth.ERREUR_PYTHON, 0,
                            "Script face_auth.py introuvable.\nVérifiez qu'il est dans le dossier du projet.");
                    return;
                }

                // Lancer Python — essayer plusieurs commandes selon l'OS
                String pythonCmd = trouverPython();
                if (pythonCmd == null) {
                    notifier(callback, ResultatAuth.ERREUR_PYTHON, 0,
                            "Modules Python manquants.\n\n" +
                            "Ouvrez Anaconda Prompt et exécutez :\n" +
                            "  pip install dlib-bin face-recognition opencv-python\n\n" +
                            "Ou créez un environnement dédié :\n" +
                            "  conda create -n faceid python=3.10\n" +
                            "  conda activate faceid\n" +
                            "  pip install dlib face-recognition opencv-python");
                    return;
                }

                ProcessBuilder pb = new ProcessBuilder(pythonCmd, scriptPath, photoRef);
                pb.redirectErrorStream(true);
                pb.directory(new File(System.getProperty("user.dir")));
                Process process = pb.start();

                // Lire la sortie
                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                        System.out.println("[FaceAuth] " + line);
                    }
                }

                int exitCode = process.waitFor();
                String result = output.toString().trim();

                // Extraire la dernière ligne significative (ignorer les lignes [DEBUG])
                String lastLine = result;
                for (String line : result.split("\n")) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty() && !trimmed.startsWith("[DEBUG]")) {
                        lastLine = trimmed;
                    }
                }
                result = lastLine;

                // Interpréter le résultat
                if (result.startsWith("SUCCES")) {
                    double simil = extraireSimilarite(result);
                    notifier(callback, ResultatAuth.SUCCES, simil,
                            String.format("Visage reconnu (%.0f%% de correspondance)", simil * 100));
                } else if (result.startsWith("ECHEC")) {
                    double simil = extraireSimilarite(result);
                    notifier(callback, ResultatAuth.ECHEC, simil,
                            String.format("Visage non reconnu (%.0f%%)\nEssayez de mieux vous placer face à la caméra.", simil * 100));
                } else if (result.contains("PAS_DE_VISAGE")) {
                    notifier(callback, ResultatAuth.PAS_DE_VISAGE, 0,
                            "Aucun visage détecté.\nPlacez-vous face à la caméra dans un endroit bien éclairé.");
                } else if (result.contains("ERREUR_CAMERA")) {
                    notifier(callback, ResultatAuth.ERREUR_CAMERA, 0,
                            "Impossible d'accéder à la caméra.\nVérifiez qu'elle est connectée.");
                } else if (result.contains("TIMEOUT")) {
                    notifier(callback, ResultatAuth.TIMEOUT, 0,
                            "Délai dépassé. Aucun visage reconnu.\nRéessayez en vous plaçant mieux face à la caméra.");
                } else if (result.contains("MODULE_MANQUANT")) {
                    notifier(callback, ResultatAuth.ERREUR_PYTHON, 0,
                            "Modules Python manquants.\nExécutez : pip install face_recognition opencv-python");
                } else {
                    notifier(callback, ResultatAuth.ERREUR_PYTHON, 0,
                            "Erreur Python (code " + exitCode + ") :\n" + result);
                }

            } catch (Exception e) {
                notifier(callback, ResultatAuth.ERREUR_PYTHON, 0,
                        "Erreur : " + e.getMessage() + "\nVérifiez que Python est installé.");
            }
        });
        t.setDaemon(true);
        t.setName("FaceAuth");
        t.start();
    }

    // ── Helpers ──────────────────────────────────────────────────
    private String trouverPhotoRef() {
        String[] chemins = {
                "src/main/resources/admin.jpg",
                "src/main/resources/admin.png",
                "resources/admin.jpg"
        };
        for (String c : chemins) {
            File f = new File(c);
            if (f.exists()) return f.getAbsolutePath();
        }
        try {
            java.net.URL url = getClass().getResource("/admin.jpg");
            if (url != null) return new File(url.toURI()).getAbsolutePath();
            url = getClass().getResource("/admin.png");
            if (url != null) return new File(url.toURI()).getAbsolutePath();
        } catch (Exception ignored) {}
        return null;
    }

    private String trouverScript() {
        String[] chemins = {
                "face_auth.py",
                "src/main/resources/face_auth.py",
                "resources/face_auth.py"
        };
        for (String c : chemins) {
            File f = new File(c);
            if (f.exists()) return f.getAbsolutePath();
        }
        return null;
    }

    /** Cherche une commande Python valide dans l'ordre de préférence */
    private String trouverPython() {
        String home = System.getProperty("user.home");
        String[] candidats = {
                // Environnement conda dédié face_recognition (priorité maximale)
                home + "\\anaconda3\\envs\\faceid\\python.exe",
                home + "\\miniconda3\\envs\\faceid\\python.exe",
                // Anaconda / Miniconda base
                home + "\\anaconda3\\python.exe",
                home + "\\miniconda3\\python.exe",
                home + "\\Anaconda3\\python.exe",
                home + "\\Miniconda3\\python.exe",
                "C:\\ProgramData\\Anaconda3\\python.exe",
                "C:\\ProgramData\\Miniconda3\\python.exe",
                // Python standard
                "python", "python3", "py",
                "C:\\Python312\\python.exe",
                "C:\\Python311\\python.exe",
                "C:\\Python310\\python.exe",
                "C:\\Python39\\python.exe",
                home + "\\AppData\\Local\\Programs\\Python\\Python312\\python.exe",
                home + "\\AppData\\Local\\Programs\\Python\\Python311\\python.exe",
                home + "\\AppData\\Local\\Programs\\Python\\Python310\\python.exe",
                home + "\\AppData\\Local\\Programs\\Python\\Python39\\python.exe"
        };
        for (String cmd : candidats) {
            try {
                // Vérifier que Python existe ET que face_recognition est installé
                ProcessBuilder pb = new ProcessBuilder(cmd, "-c",
                        "import face_recognition, cv2; print('OK')");
                pb.redirectErrorStream(true);
                Process p = pb.start();
                String out = new java.io.BufferedReader(
                        new java.io.InputStreamReader(p.getInputStream()))
                        .lines().collect(java.util.stream.Collectors.joining());
                int exit = p.waitFor();
                if (exit == 0 && out.contains("OK")) {
                    System.out.println("[FaceAuth] Python avec modules trouvé : " + cmd);
                    return cmd;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private double extraireSimilarite(String result) {
        try {
            // Format attendu : "SUCCES:0.87" ou "ECHEC:0.42"
            String[] parts = result.split(":");
            if (parts.length >= 2) return Double.parseDouble(parts[1].trim());
        } catch (Exception ignored) {}
        return 0;
    }

    private void notifier(Consumer<ResultatAuthentification> cb,
                          ResultatAuth s, double sim, String msg) {
        javafx.application.Platform.runLater(() ->
                cb.accept(new ResultatAuthentification(s, sim, msg)));
    }
}
