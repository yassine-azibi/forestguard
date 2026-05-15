# ✅ Redirection Pompier vers Gestion des Interventions

## Date : 13 mai 2026

---

## 🎯 Objectif

Quand un pompier se connecte avec ses coordonnées (email + mot de passe) de la table `pompier`, il doit être redirigé **directement vers la Gestion des Interventions**, et non vers le dashboard admin.

---

## 🔧 Modifications effectuées

### 1. **Fichier : `NavigationController.java`**

**Méthode modifiée : `ouvrirDashboardPompier()`**

#### ❌ Avant (redirection vers dashboard admin)
```java
FXMLLoader loader = new FXMLLoader(
    NavigationController.class.getResource("/GestionPompier.fxml"));
Parent root = loader.load();

Scene scene = new Scene(root);
stage.setScene(scene);
stage.setTitle("ForestGuard - Dashboard");
```

#### ✅ Après (redirection vers gestion interventions)
```java
// Rediriger directement vers la gestion des interventions
FXMLLoader loader = new FXMLLoader(
    NavigationController.class.getResource("/fxml/DashboardAgent.fxml"));
Parent root = loader.load();

// Récupérer le controller et passer les infos du pompier
DashboardAgentController controller = loader.getController();
if (controller != null && pompier != null) {
    controller.setPompierInfo(pompier.getNom() + " " + pompier.getPrenom(), pompier.getId());
}

Scene scene = new Scene(root);
stage.setScene(scene);
stage.setTitle("ForestGuard - Gestion des Interventions");
```

---

## 📊 Flux de connexion

### 🔐 Login Admin
```
Email: admin@forestguard.tn
Mot de passe: admin123
         ↓
🏠 Dashboard Admin (GestionPompier.fxml)
   - Sidebar avec tous les modules
   - Gestion complète de l'application
```

### 🚒 Login Pompier
```
Email: pompier@example.com (depuis table pompier)
Mot de passe: ******** (depuis table pompier)
         ↓
🚒 Gestion des Interventions (DashboardAgent.fxml)
   - Interface dédiée aux interventions
   - Pas de sidebar admin
   - Accès direct aux interventions
```

---

## 🗂️ Structure des fichiers

### Login
- **Fichier FXML** : `Login.fxml`
- **Contrôleur** : `edu.pompier.controllers.LoginController`
- **Méthode** : `handleLogin()`

### Navigation
- **Fichier** : `controller/NavigationController.java`
- **Méthode modifiée** : `ouvrirDashboardPompier()`

### Destinations
1. **Admin** → `/GestionPompier.fxml` (dashboard avec sidebar)
2. **Pompier** → `/fxml/DashboardAgent.fxml` (gestion interventions)

---

## ✅ Résultat

### Pour l'admin
- ✅ Login avec `admin@forestguard.tn` / `admin123`
- ✅ Accès au dashboard complet avec sidebar
- ✅ Peut naviguer vers tous les modules

### Pour les pompiers
- ✅ Login avec email/mdp de la table `pompier`
- ✅ Redirection automatique vers **Gestion des Interventions**
- ✅ Interface dédiée sans sidebar admin
- ✅ Informations du pompier passées au contrôleur

---

## 🚀 Pour tester

```bash
mvn clean javafx:run
```

### Test 1 : Login Admin
1. Email : `admin@forestguard.tn`
2. Mot de passe : `admin123`
3. **Résultat attendu** : Dashboard admin avec sidebar

### Test 2 : Login Pompier
1. Email : (email d'un pompier dans la table `pompier`)
2. Mot de passe : (mot de passe du pompier)
3. **Résultat attendu** : Gestion des Interventions directement

---

## ✅ Statut

**Modification terminée avec succès !** 🎉

Les pompiers sont maintenant redirigés directement vers la gestion des interventions après leur connexion.
