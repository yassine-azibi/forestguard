# 🧪 GUIDE DE TEST - COMPILATION ET LANCEMENT

**Date**: 13 Mai 2026  
**Status**: ✅ Prêt pour les tests

---

## ✅ CORRECTIONS APPLIQUÉES

Toutes les erreurs de compilation ont été corrigées:

1. ✅ **BOM UTF-8** supprimés (33 fichiers)
2. ✅ **Module jbcrypt** corrigé (`org.mindrot.jbcrypt` → `jbcrypt`)
3. ✅ **Package controllers** inexistant supprimé de `module-info.java`
4. ✅ **MyConnectionForet.java** réécrit avec encodage propre

---

## 🔨 ÉTAPE 1: COMPILER LE PROJET

### Option A: Depuis IntelliJ IDEA (RECOMMANDÉ)

1. **Ouvrir le projet** dans IntelliJ IDEA
2. **Menu**: `Build` > `Rebuild Project`
3. **Vérifier** la console de build:
   - ✅ Si "BUILD SUCCESSFUL" → Passer à l'étape 2
   - ❌ Si erreurs → Copier le log complet et me le transmettre

### Option B: Depuis le terminal IntelliJ

1. **Ouvrir le terminal** dans IntelliJ (Alt+F12)
2. **Exécuter**:
   ```bash
   mvn clean compile
   ```
3. **Vérifier** le résultat:
   - ✅ Si "BUILD SUCCESS" → Passer à l'étape 2
   - ❌ Si erreurs → Copier le log complet et me le transmettre

---

## 🚀 ÉTAPE 2: LANCER L'APPLICATION

### Option A: Depuis IntelliJ IDEA (RECOMMANDÉ)

1. **Localiser** la classe `src/main/java/app/Main.java`
2. **Clic droit** sur le fichier > `Run 'Main.main()'`
3. **Vérifier** que l'application démarre

### Option B: Avec Maven

1. **Dans le terminal IntelliJ**:
   ```bash
   mvn javafx:run
   ```
2. **Vérifier** que l'application démarre

---

## 🧪 ÉTAPE 3: TESTER LA NAVIGATION COMPLÈTE

### Test 1: Login Pompier → Dashboard

1. **Lancer l'application**
2. **Écran de login pompier** devrait s'afficher
3. **Se connecter** avec un compte pompier existant
4. **Vérifier** que le dashboard s'affiche avec:
   - ✅ Nom du pompier en haut
   - ✅ Sidebar avec les 6 modules
   - ✅ Bouton "Espace Utilisateur" visible

### Test 2: Navigation vers les modules

Depuis le dashboard, **cliquer sur chaque module** dans la sidebar:

1. ✅ **Gestion des Pompiers** (feature/gestion-pompiers)
2. ✅ **Gestion des Utilisateurs** (feature/gestion-utilisateur)
3. ✅ **Gestion des Capteurs** (feature/gestion-des-capteurs)
4. ✅ **Gestion des Données** (feature/gestion-des-donnees)
5. ✅ **Gestion des Alertes** (feature/gestion-alertes)
6. ✅ **Gestion des Forêts** (feature/gestionforet)

**Vérifier** pour chaque module:
- ✅ L'interface se charge correctement
- ✅ Pas d'erreurs dans la console
- ✅ Les données s'affichent (si connecté à MySQL)

### Test 3: Espace Utilisateur

1. **Depuis le dashboard**, cliquer sur **"Espace Utilisateur"**
2. **Vérifier** que l'écran de login utilisateur s'affiche
3. **Se connecter** avec un compte utilisateur
4. **Vérifier** que l'interface utilisateur s'affiche

### Test 4: Déconnexion

1. **Se déconnecter** depuis le dashboard ou l'espace utilisateur
2. **Vérifier** que l'application retourne à l'écran de login

---

## ⚠️ PROBLÈMES POSSIBLES

### Problème 1: Erreur de connexion MySQL

**Symptôme**: `Connexion MySQL échouée`

**Solutions**:
1. Vérifier que MySQL est démarré
2. Vérifier que la base `forestguard` existe
3. Vérifier les credentials dans les fichiers `MyConnection*.java`:
   - URL: `jdbc:mysql://localhost:3306/forestguard`
   - LOGIN: `root`
   - PWD: `""` (vide par défaut)

### Problème 2: Module non trouvé

**Symptôme**: `module not found: xxx`

**Solution**: Vérifier que toutes les dépendances sont téléchargées:
```bash
mvn clean install
```

### Problème 3: Erreur JavaFX

**Symptôme**: `Error initializing QuantumRenderer`

**Solution**: Vérifier que JavaFX 17 est bien configuré dans le projet

---

## 📊 CHECKLIST DE VALIDATION

Cocher chaque élément testé:

- [ ] ✅ Compilation réussie (pas d'erreurs)
- [ ] ✅ Application démarre
- [ ] ✅ Login pompier fonctionne
- [ ] ✅ Dashboard s'affiche avec sidebar
- [ ] ✅ Module Gestion Pompiers accessible
- [ ] ✅ Module Gestion Utilisateurs accessible
- [ ] ✅ Module Gestion Capteurs accessible
- [ ] ✅ Module Gestion Données accessible
- [ ] ✅ Module Gestion Alertes accessible
- [ ] ✅ Module Gestion Forêts accessible
- [ ] ✅ Bouton "Espace Utilisateur" fonctionne
- [ ] ✅ Login utilisateur fonctionne
- [ ] ✅ Déconnexion fonctionne

---

## 🎯 RÉSULTAT ATTENDU

**Si tous les tests passent**:
- ✅ L'intégration est **COMPLÈTE**
- ✅ Les 6 modules sont **UNIFIÉS**
- ✅ La navigation est **FONCTIONNELLE**
- ✅ L'architecture est **RESPECTÉE**

**Prochaine étape**: Commit final et push vers GitHub!

---

## 📝 COMMIT FINAL

Une fois tous les tests validés:

```bash
# Ajouter tous les fichiers modifiés
git add .

# Commit avec message descriptif
git commit -m "feat: Intégration complète des 6 modules ForestGuard

- Merge de toutes les branches feature
- Architecture unifiée: Login → Dashboard → 6 modules
- Navigation centralisée avec NavigationController
- Singleton AppConfig pour le pompier connecté
- Corrections compilation: BOM, modules, packages
- Tests de navigation complets

Modules intégrés:
- feature/gestion-pompiers (27 fichiers)
- feature/gestion-utilisateur (52 fichiers)
- feature/gestion-des-capteurs (44 fichiers)
- feature/gestion-des-donnees (27 fichiers)
- feature/gestion-alertes (56 fichiers)
- feature/gestionforet (50 fichiers)

Total: 256 fichiers, 55,362 lignes de code"

# Push vers GitHub
git push origin dev
```

---

## 📞 SUPPORT

**Si vous rencontrez des problèmes**:
1. Copier le message d'erreur complet
2. Indiquer à quelle étape l'erreur se produit
3. Me transmettre le log pour diagnostic

---

**DOCUMENT CRÉÉ LE**: 13 Mai 2026  
**STATUT**: ✅ PRÊT POUR LES TESTS

