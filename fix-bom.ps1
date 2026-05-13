# Script pour supprimer les BOM (Byte Order Mark) des fichiers Java

$files = @(
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
)

$count = 0
foreach ($file in $files) {
    if (Test-Path $file) {
        $content = Get-Content $file -Raw -Encoding UTF8
        # Supprimer le BOM
        $content = $content -replace "^\xEF\xBB\xBF", ""
        $content = $content -replace "^`ufeff", ""
        # Sauvegarder sans BOM
        [System.IO.File]::WriteAllText($file, $content, (New-Object System.Text.UTF8Encoding $false))
        $count++
        Write-Host "✓ Corrigé: $file"
    }
}

Write-Host "`n✅ $count fichiers corrigés!"
