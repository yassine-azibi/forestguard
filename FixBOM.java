import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class FixBOM {
    public static void main(String[] args) throws IOException {
        String[] files = {
            "src/main/java/controller/AjouterAnimal.java",
            "src/main/java/controller/AjouterForet.java",
            "src/main/java/controller/AjouterIncendie.java",
            "src/main/java/controller/AjouterInterventionController.java",
            "src/main/java/controller/AssistantIAController.java",
            "src/main/java/controller/CarteController.java",
            "src/main/java/controller/ConfirmationSuppressionController.java",
            "src/main/java/controller/DashboardAgentController.java",
            "src/main/java/controller/EvenementManager.java",
            "src/main/java/controller/ForetCell.java",
            "src/main/java/controller/ForetPrincipal.java",
            "src/main/java/controller/IncendieStats.java",
            "src/main/java/controller/ListeAffectationsController.java",
            "src/main/java/controller/ModifierForet.java",
            "src/main/java/controller/ModifierInterventionController.java",
            "src/main/java/controller/PerimetreController.java",
            "src/main/java/controller/PopupAffectationController.java",
            "src/main/java/controller/PopupInterventionController.java",
            "src/main/java/controller/StatistiquesController.java",
            "src/main/java/controller/TTSPanel.java",
            "src/main/java/controller/TextToSpeechService.java",
            "src/main/java/controller/TileProxy.java",
            "src/main/java/controller/Validation.java",
            "src/main/java/model/Affectation.java",
            "src/main/java/model/Animal.java",
            "src/main/java/model/Equipement.java",
            "src/main/java/model/Incendie.java",
            "src/main/java/model/Intervention.java",
            "src/main/java/utils/AnimalService.java",
            "src/main/java/utils/ForetService.java",
            "src/main/java/utils/IService.java",
            "src/main/java/utils/IncendieService.java",
            "src/main/java/utils/MyConnectionForet.java"
        };
        
        int count = 0;
        for (String file : files) {
            Path path = Paths.get(file);
            if (Files.exists(path)) {
                byte[] bytes = Files.readAllBytes(path);
                // Vérifier et supprimer le BOM UTF-8 (EF BB BF)
                if (bytes.length >= 3 && bytes[0] == (byte)0xEF && bytes[1] == (byte)0xBB && bytes[2] == (byte)0xBF) {
                    byte[] newBytes = new byte[bytes.length - 3];
                    System.arraycopy(bytes, 3, newBytes, 0, newBytes.length);
                    Files.write(path, newBytes);
                    System.out.println("✓ Corrigé: " + file);
                    count++;
                }
            }
        }
        System.out.println("\n✅ " + count + " fichiers corrigés!");
    }
}
