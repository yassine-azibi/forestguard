# 🔀 GUIDE DE MERGE ET INTÉGRATION - FORESTGUARD

**Date**: 12 Mai 2026
**Chef de projet**: Yassine Azibi
**Objectif**: Intégrer tous les modules en une application unifiée

---

## 📋 PRÉREQUIS

✅ Tous les fichiers d'intégration créés dans `dev`:
- `src/main/java/config/AppConfig.java`
- `src/main/java/controller/NavigationController.java`
- `src/main/java/app/Main.java`
- `src/main/java/controller/DashboardAgentController.java` (modifié)

✅ Toutes les branches analysées et documentées

---

## 🎯 ORDRE DE MERGE (IMPORTANT!)

```
dev (base)
  ↓
1. feature/gestion-pompiers (Login + Dashboard principal)
  ↓
2. feature/gestion-utilisateur (indépendant)
  ↓
3. feature/gestion-des-capteurs (indépendant)
  ↓
4. feature/gestion-des-donnees (indépendant)
  ↓
5. feature/gestion-alertes (conflits possibles)
  ↓
6. feature/gestionforet (conflits possibles)
```

**Raison de cet ordre**:
- Modules indépendants d'abord (pas de conflits)
- Modules avec conflits à la fin (plus facile à résoudre)

---

## 🔀 ÉTAPE 1: MERGER `feature/gestion-pompiers`

### Commandes Git:

```bash
# S'assurer d'être sur dev
git checkout dev

# Merger gestion-pompiers
git merge intervention/feature/gestion-pompiers

# Si conflits, les résoudre (voir section Résolution de conflits)
```

### Modifications à apporter APRÈS le merge:

#### 1. Modifier `src/main/resources/GestionPompier.fxml`:

Ajouter le bouton Interventions après `btnAlertes` (ligne ~56):

```xml
<Button fx:id="btnInterventions" prefWidth="220" prefHeight="46" 
        text="&#x1F692;  Interventions" 
        onAction="#ouvrirGestionInterventions"
        style="-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 13; -fx-alignment: CENTER_LEFT; -fx-padding: 10 16; -fx-background-radius: 12; -fx-cursor: hand;"/>
```

Ajouter onAction aux boutons existants:

```xml
<Button fx:id="btnForets" ... onAction="#ouvrirGestionForets"/>
<Button fx:id="btnCapteurs" ... onAction="#ouvrirGestionCapteurs"/>
<Button fx:id="btnDonnees" ... onAction="#ouvrirGestionDonnees"/>
<Button fx:id="btnAlertes" ... onAction="#ouvrirGestionAlertes"/>
<Button fx:id="btnUtilisateurs" ... onAction="#ouvrirGestionUtilisateurs"/>
```

#### 2. Modifier `src/main/java/edu/pompier/controllers/PompierController.java`:

Ajouter les imports:

```java
import controller.NavigationController;
import config.AppConfig;
import javafx.stage.Stage;
```

Ajouter les méthodes de navigation (voir MODIFICATIONS_BRANCHES.md pour le code complet)

#### 3. Modifier `src/main/java/edu/pompier/controllers/LoginController.java`:

Ajouter les imports:

```java
import config.AppConfig;
import controller.NavigationController;
```

Modifier `ouvrirPagePompier()` et `handleNaviguerUtilisateur()` (voir MODIFICATIONS_BRANCHES.md)

### Vérification:

```bash
# Compiler
mvn clean compile

# Vérifier qu'il n'y a pas d'erreurs
```

---

## 🔀 ÉTAPE 2: MERGER `feature/gestion-utilisateur`

### Commandes Git:

```bash
git merge intervention/feature/gestion-utilisateur
```

### Conflits possibles:

- `pom.xml` - Dépendances (BCrypt, Jakarta Mail, Twilio)
- `module-info.java` - Requires/opens

### Résolution:

**Pour `pom.xml`**:
- Garder TOUTES les dépendances des deux branches
- Fusionner les sections `<dependencies>`

**Pour `module-info.java`**:
- Garder TOUS les `requires` et `opens` des deux branches
- Supprimer les doublons

### Vérification:

```bash
mvn clean compile
```

---

## 🔀 ÉTAPE 3: MERGER `feature/gestion-des-capteurs`

### Commandes Git:

```bash
git merge intervention/feature/gestion-des-capteurs
```

### ⚠️ PROBLÈME: Dossier `ForestGuard_FINAL/`

Après le merge, déplacer les fichiers:

```bash
# Déplacer les fichiers Java
mv ForestGuard_FINAL/src/main/java/edu/capteur src/main/java/edu/

# Déplacer les ressources
mv ForestGuard_FINAL/src/main/resources/*.fxml src/main/resources/
mv ForestGuard_FINAL/src/main/resources/*.jpg src/main/resources/
mv ForestGuard_FINAL/src/main/resources/*.png src/main/resources/

# Supprimer le dossier vide
rm -rf ForestGuard_FINAL/
```

### Vérification:

```bash
mvn clean compile
```

---

## 🔀 ÉTAPE 4: MERGER `feature/gestion-des-donnees`

### Commandes Git:

```bash
git merge intervention/feature/gestion-des-donnees
```

### Conflits possibles:

- Fichiers FXML avec mêmes noms (DashboardAgent.fxml, etc.)
- CSS (forestguard.css)

### Résolution:

**Pour les FXML**:
- Garder les deux versions
- Renommer si nécessaire

**Pour les CSS**:
- Fusionner les styles
- Garder les deux si différents

### Vérification:

```bash
mvn clean compile
```

---

## 🔀 ÉTAPE 5: MERGER `feature/gestion-alertes`

### ⚠️ ATTENTION: Conflits probables

Cette branche utilise le même package racine que `dev`:
- `controller/`
- `dao/`
- `model/`

### Commandes Git:

```bash
git merge intervention/feature/gestion-alertes
```

### Conflits attendus:

1. **Controllers avec mêmes noms**:
   - `DashboardAgentController.java`
   - `AjouterInterventionController.java`
   - `AssistantIAController.java`
   - `ListeAffectationsController.java`
   - `ModifierInterventionController.java`
   - `PopupAffectationController.java`
   - `PopupInterventionController.java`
   - `StatistiquesController.java`

2. **FXML avec mêmes noms**:
   - `DashboardAgent.fxml`
   - `AjouterIntervention.fxml`
   - `AssistantIA.fxml`
   - etc.

### Résolution:

**OPTION 1: Renommer les fichiers de gestion-alertes**

Renommer tous les fichiers conflictuels:
- `DashboardAgentController.java` → `DashboardAlertesController.java`
- `DashboardAgent.fxml` → `DashboardAlertes.fxml`
- etc.

**OPTION 2: Créer un sous-package**

Déplacer les fichiers de gestion-alertes dans:
- `controller/alertes/`
- `fxml/alertes/`

**RECOMMANDATION**: OPTION 1 (plus simple)

### Vérification:

```bash
mvn clean compile
```

---

## 🔀 ÉTAPE 6: MERGER `feature/gestionforet`

### ⚠️ ATTENTION: Modifie des fichiers existants

Cette branche modifie:
- `AjouterInterventionController.java`
- `AssistantIAController.java`

### Commandes Git:

```bash
git merge intervention/feature/gestionforet
```

### Conflits attendus:

1. **Controllers modifiés**:
   - Comparer les versions
   - Garder les modifications utiles des deux côtés

2. **Package `controllers` vs `controller`**:
   - La branche utilise `controllers` (avec s)
   - Dev utilise `controller` (sans s)

### Résolution:

**Pour les controllers modifiés**:
- Ouvrir les deux versions
- Fusionner manuellement les modifications
- Garder toutes les fonctionnalités

**Pour le package**:
- Garder `controller` (sans s) comme standard
- Renommer `controllers.ForetPrincipal` → `controller.ForetPrincipalController`

### Vérification:

```bash
mvn clean compile
```

---

## 🧪 ÉTAPE 7: TESTS COMPLETS

### 1. Compilation:

```bash
mvn clean compile
```

### 2. Vérifier les erreurs:

- Imports manquants
- Classes non trouvées
- FXML mal référencés

### 3. Lancer l'application:

```bash
mvn javafx:run
```

### 4. Tester le workflow complet:

1. ✅ Login pompier → Dashboard
2. ✅ Dashboard → Gestion Forêts
3. ✅ Dashboard → Gestion Capteurs
4. ✅ Dashboard → Gestion Données
5. ✅ Dashboard → Gestion Alertes
6. ✅ Dashboard → Gestion Interventions
7. ✅ Dashboard → Espace Utilisateur → Login Utilisateur
8. ✅ Déconnexion → Retour Login

---

## 🔧 RÉSOLUTION DE CONFLITS GÉNÉRIQUES

### Conflit dans `pom.xml`:

```bash
# Ouvrir le fichier
nano pom.xml

# Chercher les marqueurs de conflit
<<<<<<< HEAD
=======
>>>>>>> feature/xxx

# Garder TOUTES les dépendances
# Supprimer les marqueurs
# Supprimer les doublons
```

### Conflit dans `module-info.java`:

```bash
# Ouvrir le fichier
nano src/main/java/module-info.java

# Garder TOUS les requires et opens
# Supprimer les doublons
# Supprimer les marqueurs de conflit
```

### Conflit dans un fichier Java:

```bash
# Ouvrir le fichier
nano src/main/java/...

# Analyser les deux versions
# Fusionner manuellement
# Garder toutes les fonctionnalités
# Supprimer les marqueurs
```

---

## 📝 CHECKLIST FINALE

Avant de considérer l'intégration terminée:

- [ ] Toutes les branches mergées dans dev
- [ ] Aucune erreur de compilation
- [ ] Application démarre sur Login.fxml
- [ ] Login pompier fonctionne
- [ ] Dashboard pompier s'affiche
- [ ] Tous les boutons sidebar fonctionnent
- [ ] Navigation vers tous les modules fonctionne
- [ ] Bouton "Espace Utilisateur" fonctionne
- [ ] Login utilisateur s'ouvre
- [ ] Déconnexion fonctionne
- [ ] Retour au login fonctionne
- [ ] Aucune exception dans la console
- [ ] Toutes les dépendances Maven résolues
- [ ] module-info.java correct

---

## 🎯 CONFIGURATION FINALE

### Modifier `pom.xml` pour le point d'entrée:

```xml
<plugin>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-maven-plugin</artifactId>
    <version>0.0.8</version>
    <configuration>
        <mainClass>app.Main</mainClass>
    </configuration>
</plugin>
```

### Vérifier `module-info.java`:

```java
module gestion.des.interventions {
    // JavaFX
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.base;
    requires javafx.media;
    requires javafx.web;
    
    // MySQL
    requires java.sql;
    
    // JSON
    requires org.json;
    
    // PDF
    requires com.github.librepdf.openpdf;
    
    // BCrypt (gestion-utilisateur)
    requires jbcrypt;
    
    // Jakarta Mail (gestion-utilisateur)
    requires jakarta.mail;
    requires jakarta.activation;
    
    // Twilio (gestion-utilisateur)
    requires twilio;
    
    // Opens pour JavaFX
    opens controller to javafx.fxml;
    opens edu.pompier.controllers to javafx.fxml;
    opens com.forestguard.controllers to javafx.fxml;
    opens edu.capteur.controllers to javafx.fxml;
    opens ForestGuard.controllers to javafx.fxml;
    opens controllers to javafx.fxml;
    
    // Opens pour entities
    opens model to javafx.base;
    opens edu.pompier.entities to javafx.base;
    opens com.forestguard.entities to javafx.base;
    
    // Exports
    exports app;
    exports controller;
    exports config;
}
```

---

## 🚀 LANCEMENT FINAL

```bash
# Nettoyer et compiler
mvn clean compile

# Lancer l'application
mvn javafx:run
```

---

## 📊 RÉSULTAT ATTENDU

```
🌲 Démarrage de ForestGuard...
✅ Icône ForestGuard chargée
✅ ForestGuard démarré avec succès

[Login.fxml s'affiche]
[Utilisateur se connecte]
[Dashboard pompier s'affiche avec sidebar]
[Navigation vers tous les modules fonctionne]
```

---

**GUIDE CRÉÉ LE**: 12 Mai 2026
**STATUT**: PRÊT POUR INTÉGRATION
**DURÉE ESTIMÉE**: 2-3 heures
