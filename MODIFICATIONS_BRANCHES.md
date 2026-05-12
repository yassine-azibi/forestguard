# 📝 MODIFICATIONS À APPORTER AUX BRANCHES

**Date**: 12 Mai 2026
**Objectif**: Modifications minimales pour intégrer tous les modules

---

## ⚠️ RÈGLE D'OR: MODIFICATIONS MINIMALES UNIQUEMENT

- ✅ AJOUTER des onAction aux boutons
- ✅ AJOUTER des méthodes de navigation
- ❌ NE PAS modifier le code existant
- ❌ NE PAS changer les controllers
- ❌ NE PAS toucher aux DAO, models, services

---

## 📁 BRANCHE: `feature/gestion-pompiers`

### 1. Fichier: `GestionPompier.fxml`

#### AJOUTER bouton Interventions dans la sidebar (après btnAlertes):

```xml
<Button fx:id="btnInterventions" prefWidth="220" prefHeight="46" 
        text="&#x1F692;  Interventions" 
        onAction="#ouvrirGestionInterventions"
        style="-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 13; -fx-alignment: CENTER_LEFT; -fx-padding: 10 16; -fx-background-radius: 12; -fx-cursor: hand;"/>
```

#### AJOUTER onAction aux boutons existants:

```xml
<!-- Ligne ~52 -->
<Button fx:id="btnForets" ... onAction="#ouvrirGestionForets"/>

<!-- Ligne ~53 -->
<Button fx:id="btnCapteurs" ... onAction="#ouvrirGestionCapteurs"/>

<!-- Ligne ~54 -->
<Button fx:id="btnDonnees" ... onAction="#ouvrirGestionDonnees"/>

<!-- Ligne ~55 -->
<Button fx:id="btnAlertes" ... onAction="#ouvrirGestionAlertes"/>

<!-- Ligne ~56 -->
<Button fx:id="btnUtilisateurs" ... onAction="#ouvrirGestionUtilisateurs"/>
```

---

### 2. Fichier: `edu/pompier/controllers/PompierController.java`

#### AJOUTER import:

```java
import controller.NavigationController;
import config.AppConfig;
import javafx.stage.Stage;
```

#### AJOUTER méthodes à la fin de la classe:

```java
    /**
     * Ouvre le module Gestion des Forêts
     */
    @FXML
    private void ouvrirGestionForets() {
        Stage stage = (Stage) btnForets.getScene().getWindow();
        NavigationController.ouvrirGestionForets(stage);
    }
    
    /**
     * Ouvre le module Gestion des Capteurs
     */
    @FXML
    private void ouvrirGestionCapteurs() {
        Stage stage = (Stage) btnCapteurs.getScene().getWindow();
        NavigationController.ouvrirGestionCapteurs(stage);
    }
    
    /**
     * Ouvre le module Gestion des Données
     */
    @FXML
    private void ouvrirGestionDonnees() {
        Stage stage = (Stage) btnDonnees.getScene().getWindow();
        NavigationController.ouvrirGestionDonnees(stage);
    }
    
    /**
     * Ouvre le module Gestion des Alertes
     */
    @FXML
    private void ouvrirGestionAlertes() {
        Stage stage = (Stage) btnAlertes.getScene().getWindow();
        NavigationController.ouvrirGestionAlertes(stage);
    }
    
    /**
     * Ouvre le module Gestion des Interventions
     */
    @FXML
    private void ouvrirGestionInterventions() {
        Pompier pompier = AppConfig.getInstance().getPompierConnecte();
        Stage stage = (Stage) btnInterventions.getScene().getWindow();
        NavigationController.ouvrirGestionInterventions(stage, pompier);
    }
    
    /**
     * Ouvre le login utilisateur (espace utilisateur)
     */
    @FXML
    private void ouvrirGestionUtilisateurs() {
        Stage stage = (Stage) btnUtilisateurs.getScene().getWindow();
        NavigationController.ouvrirLoginUtilisateur(stage);
    }
```

---

### 3. Fichier: `edu/pompier/controllers/LoginController.java`

#### AJOUTER import:

```java
import config.AppConfig;
import controller.NavigationController;
```

#### MODIFIER la méthode `ouvrirPagePompier`:

```java
    private void ouvrirPagePompier(Pompier pompier) {
        // Stocker le pompier dans AppConfig
        AppConfig.getInstance().setPompierConnecte(pompier);
        
        // Ouvrir le dashboard pompier
        Stage stage = (Stage) emailField.getScene().getWindow();
        NavigationController.ouvrirDashboardPompier(stage, pompier);
    }
```

#### AJOUTER/MODIFIER la méthode `handleNaviguerUtilisateur`:

```java
    @FXML
    private void handleNaviguerUtilisateur() {
        Stage stage = (Stage) btnUserLogin.getScene().getWindow();
        NavigationController.ouvrirLoginUtilisateur(stage);
    }
```

---

## 📁 TOUTES LES AUTRES BRANCHES

### Aucune modification nécessaire! ✅

Les modules suivants fonctionnent de manière indépendante:
- `feature/gestion-utilisateur`
- `feature/gestion-alertes`
- `feature/gestion-des-capteurs`
- `feature/gestion-des-donnees`
- `feature/gestionforet`

Ils seront appelés via `NavigationController` depuis le dashboard pompier.

---

## 📋 RÉSUMÉ DES MODIFICATIONS

| Fichier | Branche | Type | Lignes ajoutées |
|---------|---------|------|-----------------|
| `GestionPompier.fxml` | gestion-pompiers | FXML | ~10 lignes |
| `PompierController.java` | gestion-pompiers | Java | ~60 lignes |
| `LoginController.java` | gestion-pompiers | Java | ~10 lignes |
| **TOTAL** | | | **~80 lignes** |

---

## ✅ FICHIERS CRÉÉS DANS DEV

| Fichier | Package | Lignes | Status |
|---------|---------|--------|--------|
| `AppConfig.java` | config | ~90 | ✅ Créé |
| `NavigationController.java` | controller | ~180 | ✅ Créé |
| `Main.java` | app | ~45 | ✅ Créé |
| `DashboardAgentController.java` | controller | Modifié | ✅ Méthode `setPompierInfo()` ajoutée |

---

## 🎯 PROCHAINE ÉTAPE

Merger toutes les branches dans `dev` dans cet ordre:

1. ✅ Créer les fichiers dans dev (FAIT)
2. ⏳ Merger `feature/gestion-pompiers` (Login + Dashboard)
3. ⏳ Merger `feature/gestion-utilisateur`
4. ⏳ Merger `feature/gestion-des-capteurs`
5. ⏳ Merger `feature/gestion-des-donnees`
6. ⏳ Merger `feature/gestion-alertes` (attention conflits)
7. ⏳ Merger `feature/gestionforet` (attention conflits)
8. ⏳ Tester l'application complète

---

**DOCUMENT GÉNÉRÉ LE**: 12 Mai 2026
**STATUT**: PRÊT POUR MERGE
