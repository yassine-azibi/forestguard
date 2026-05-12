# 🚀 GUIDE POUR CONTINUER L'INTÉGRATION

**Date**: 12 Mai 2026
**Étape actuelle**: 1/6 branches mergées
**Prochaine étape**: Merger `feature/gestion-utilisateur`

---

## ✅ CE QUI A ÉTÉ FAIT

1. ✅ Fichiers d'intégration créés dans `dev`:
   - `src/main/java/config/AppConfig.java`
   - `src/main/java/controller/NavigationController.java`
   - `src/main/java/app/Main.java`

2. ✅ Branche `feature/gestion-pompiers` mergée et modifiée:
   - `GestionPompier.fxml` - Ajout onAction aux boutons
   - `PompierController.java` - Ajout méthodes de navigation
   - `LoginController.java` - Stockage pompier + navigation
   - `pom.xml` - Point d'entrée changé vers `app.Main`

3. ✅ Commits effectués:
   - Fichiers d'intégration
   - Modifications de navigation

---

## 🎯 PROCHAINES ÉTAPES

### ÉTAPE 2: Merger `feature/gestion-utilisateur`

#### Commandes:

```bash
# S'assurer d'être sur dev
git checkout dev

# Merger la branche
git merge intervention/feature/gestion-utilisateur
```

#### Conflits attendus:

1. **pom.xml** - Dépendances supplémentaires:
   - BCrypt (jbcrypt)
   - Jakarta Mail
   - Twilio
   - **Solution**: Garder TOUTES les dépendances

2. **module-info.java** - Requires/opens supplémentaires:
   - `requires jbcrypt;`
   - `requires jakarta.mail;`
   - `requires twilio;`
   - `opens com.forestguard.controllers to javafx.fxml;`
   - `opens com.forestguard.entities to javafx.base;`
   - **Solution**: Garder TOUS les requires/opens

#### Résolution des conflits:

**Pour pom.xml**:
```bash
# Ouvrir le fichier
nano pom.xml

# Chercher les marqueurs <<<<<<< HEAD
# Garder TOUTES les dépendances des deux branches
# Supprimer les marqueurs de conflit
# Supprimer les doublons
```

**Pour module-info.java**:
```bash
# Ouvrir le fichier
nano src/main/java/module-info.java

# Garder TOUS les requires et opens
# Supprimer les doublons
# Supprimer les marqueurs de conflit
```

#### Vérification:

```bash
# Compiler (si Maven est configuré)
mvn clean compile

# OU utiliser IntelliJ IDEA:
# Build > Build Project
```

#### Commit:

```bash
git add .
git commit -m "feat: Merge feature/gestion-utilisateur - Ajout module utilisateur"
```

---

### ÉTAPE 3: Merger `feature/gestion-des-capteurs`

#### Commandes:

```bash
git merge intervention/feature/gestion-des-capteurs
```

#### ⚠️ PROBLÈME: Dossier `ForestGuard_FINAL/`

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

#### Commit:

```bash
git add .
git commit -m "feat: Merge feature/gestion-des-capteurs - Ajout module capteurs"
```

---

### ÉTAPE 4: Merger `feature/gestion-des-donnees`

#### Commandes:

```bash
git merge intervention/feature/gestion-des-donnees
```

#### Conflits possibles:

- Fichiers CSS avec mêmes noms
- Images avec mêmes noms

#### Solution:

- Garder les deux versions si différentes
- Renommer si nécessaire

#### Commit:

```bash
git add .
git commit -m "feat: Merge feature/gestion-des-donnees - Ajout module données"
```

---

### ÉTAPE 5: Merger `feature/gestion-alertes` ⚠️ ATTENTION

#### ⚠️ CONFLITS MAJEURS ATTENDUS

Cette branche utilise le même package racine que `dev`:
- `controller/`
- `dao/`
- `model/`

#### Commandes:

```bash
git merge intervention/feature/gestion-alertes
```

#### Conflits attendus:

Fichiers avec mêmes noms:
- `DashboardController.java` (mais différent de DashboardAgentController)
- `AjouterInterventionController.java`
- `AssistantIAController.java`
- etc.

#### Solution recommandée:

**OPTION 1: Renommer les fichiers de gestion-alertes**

Après le merge, renommer:
```bash
# Controllers
mv src/main/java/controller/DashboardController.java src/main/java/controller/DashboardAlertesController.java

# FXML
mv src/main/resources/fxml/Dashboard.fxml src/main/resources/fxml/DashboardAlertes.fxml

# Mettre à jour les références dans les fichiers
```

**OPTION 2: Créer un sous-package**

Déplacer les fichiers de gestion-alertes:
```bash
mkdir -p src/main/java/controller/alertes
mv src/main/java/controller/Dashboard*.java src/main/java/controller/alertes/
# etc.
```

#### Commit:

```bash
git add .
git commit -m "feat: Merge feature/gestion-alertes - Ajout module alertes (fichiers renommés)"
```

---

### ÉTAPE 6: Merger `feature/gestionforet` ⚠️ ATTENTION

#### ⚠️ MODIFIE DES FICHIERS EXISTANTS

Cette branche modifie:
- `AjouterInterventionController.java`
- `AssistantIAController.java`

#### Commandes:

```bash
git merge intervention/feature/gestionforet
```

#### Conflits attendus:

1. **Controllers modifiés**:
   - Comparer les versions
   - Fusionner manuellement les modifications

2. **Package `controllers` vs `controller`**:
   - La branche utilise `controllers` (avec s)
   - Dev utilise `controller` (sans s)

#### Solution:

**Pour les controllers modifiés**:
```bash
# Ouvrir les deux versions
# Fusionner manuellement les modifications
# Garder toutes les fonctionnalités
```

**Pour le package**:
```bash
# Renommer controllers.ForetPrincipal → controller.ForetPrincipalController
# Mettre à jour les imports
```

#### Commit:

```bash
git add .
git commit -m "feat: Merge feature/gestionforet - Ajout module forêts (conflits résolus)"
```

---

## 🧪 TESTS FINAUX

### 1. Vérifier module-info.java

Après tous les merges, vérifier que `module-info.java` contient:

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

### 2. Compiler le projet

```bash
# Avec Maven (si configuré)
mvn clean compile

# OU avec IntelliJ IDEA
# Build > Build Project
```

### 3. Lancer l'application

```bash
# Avec Maven
mvn javafx:run

# OU avec IntelliJ IDEA
# Run > Run 'Main'
```

### 4. Tester le workflow complet

1. ✅ Login pompier (email + mot de passe)
2. ✅ Dashboard s'affiche
3. ⏳ Cliquer sur "Forêts" → Module forêts s'ouvre
4. ⏳ Cliquer sur "Capteurs" → Module capteurs s'ouvre
5. ⏳ Cliquer sur "Données" → Module données s'ouvre
6. ⏳ Cliquer sur "Alertes" → Module alertes s'ouvre
7. ⏳ Cliquer sur "Interventions" → Module interventions s'ouvre
8. ⏳ Cliquer sur "Utilisateurs" → Login utilisateur s'ouvre
9. ✅ Déconnexion → Retour au login

---

## 📝 CHECKLIST FINALE

Avant de considérer l'intégration terminée:

- [ ] Toutes les branches mergées (6/6)
- [ ] Aucune erreur de compilation
- [ ] `module-info.java` complet
- [ ] `pom.xml` avec toutes les dépendances
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

---

## 🆘 EN CAS DE PROBLÈME

### Erreur de compilation

1. Vérifier `module-info.java`
2. Vérifier `pom.xml`
3. Vérifier les imports dans les fichiers Java
4. Nettoyer et recompiler:
   ```bash
   mvn clean
   mvn compile
   ```

### Erreur au lancement

1. Vérifier que `mainClass` dans `pom.xml` est `app.Main`
2. Vérifier que `Login.fxml` existe dans `src/main/resources/`
3. Vérifier les logs dans la console

### Module ne s'ouvre pas

1. Vérifier le chemin FXML dans `NavigationController.java`
2. Vérifier que le FXML existe
3. Vérifier que le controller est bien référencé dans le FXML

---

## 📞 SUPPORT

Si vous rencontrez des problèmes:

1. Lire les messages d'erreur dans la console
2. Vérifier les fichiers de documentation:
   - `ANALYSE_COMPLETE_BRANCHES.md`
   - `MODIFICATIONS_BRANCHES.md`
   - `GUIDE_MERGE_INTEGRATION.md`
   - `INTEGRATION_ETAPE1_COMPLETE.md`

3. Vérifier les commits Git:
   ```bash
   git log --oneline
   ```

---

**DOCUMENT CRÉÉ LE**: 12 Mai 2026
**STATUT**: GUIDE PRÊT - ÉTAPE 1 TERMINÉE
**PROCHAINE ACTION**: Merger `feature/gestion-utilisateur`
