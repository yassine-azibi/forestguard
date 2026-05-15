# Solution pour la Navigation Dashboard

## Problème
Après avoir navigué vers un module externe (Forêts, Capteurs, Données, Alertes, Réclamations, Utilisateurs), les boutons "Dashboard" et "Pompiers" ne fonctionnent plus.

## Cause
Quand on charge un module externe dans `mainStackPane`, on remplace le contenu du dashboard. Les vues `vueDashboard` et `vuePompiers` n'existent plus, donc les méthodes `afficherVueDashboard()` et `afficherVuePompiers()` ne peuvent plus les manipuler.

## Solution Recommandée

### Option 1 : Utiliser NavigationController (RECOMMANDÉ)
Au lieu de charger les modules dans `mainStackPane`, utiliser `NavigationController` pour remplacer toute la scène. Cela garantit que chaque module a sa propre sidebar fonctionnelle.

**Avantages** :
- Chaque module est indépendant
- Pas de conflit entre sidebars
- Navigation propre et claire

**Modifications nécessaires** :
1. Dans `PompierController.java`, modifier les méthodes `ouvrirGestionXXX()` pour utiliser `NavigationController`
2. Ajouter des boutons "Retour au Dashboard" dans chaque module externe

### Option 2 : Recharger GestionPompier.fxml (ACTUEL)
La solution actuelle recharge `GestionPompier.fxml` quand on détecte que les vues n'existent plus.

**Problème actuel** :
- La méthode `rechargerDashboardPrincipal()` remplace toute la scène
- Mais les modules externes sont toujours chargés dans `mainStackPane`
- Cela crée une incohérence

## Instructions pour Corriger

### Étape 1 : Vérifier que tous les modules FXML n'ont PAS de sidebar

Vérifiez que ces fichiers n'ont PAS de `<VBox fx:id="menuLateral">` :
- `src/main/resources/fxml/ForetPrincipal.fxml` ✅
- `src/main/resources/capteur.fxml` ✅
- `src/main/resources/fxml/donnees.fxml` ✅
- `src/main/resources/fxml/Dashboard.fxml` ✅
- `src/main/resources/fxml/Reclamations.fxml` ✅
- `src/main/resources/fxml/Utilisateurs.fxml` ✅

### Étape 2 : Tester la navigation

1. Lancez l'application
2. Connectez-vous en tant qu'admin
3. Cliquez sur "Forêts" → Le module Forêts s'affiche
4. Cliquez sur "Dashboard" → Devrait retourner au dashboard
5. Cliquez sur "Capteurs" → Le module Capteurs s'affiche
6. Cliquez sur "Pompiers" → Devrait afficher la gestion des pompiers

### Étape 3 : Si la navigation ne fonctionne toujours pas

Le problème est que `rechargerDashboardPrincipal()` remplace la scène, mais les boutons de la nouvelle sidebar ne sont pas connectés au contrôleur.

**Solution** : Au lieu de recharger dans `rechargerDashboardPrincipal()`, fermez la fenêtre et rouvrez `GestionPompier.fxml` :

```java
private void rechargerDashboardPrincipal() {
    try {
        System.out.println("🔄 Rechargement du dashboard principal...");
        
        // Fermer la fenêtre actuelle
        Stage currentStage = (Stage) btnDashboard.getScene().getWindow();
        
        // Charger le nouveau dashboard
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/GestionPompier.fxml"));
        Parent root = loader.load();
        
        // Créer une nouvelle scène
        Scene scene = new Scene(root);
        currentStage.setScene(scene);
        currentStage.setTitle("ForestGuard - Administration");
        
        System.out.println("✅ Dashboard principal rechargé");
    } catch (Exception e) {
        System.err.println("❌ Erreur lors du rechargement du dashboard: " + e.getMessage());
        e.printStackTrace();
    }
}
```

## Test Final

Après avoir appliqué les corrections :

1. ✅ Dashboard → Forêts → Dashboard (fonctionne)
2. ✅ Dashboard → Capteurs → Pompiers (fonctionne)
3. ✅ Dashboard → Données → Dashboard (fonctionne)
4. ✅ Dashboard → Alertes → Pompiers → Dashboard (fonctionne)
5. ✅ Pas de double sidebar
6. ✅ Navigation fluide

## Notes

- Les modules Forêts, Capteurs, Données, Alertes, Réclamations et Utilisateurs n'ont PLUS de sidebar
- La sidebar est uniquement dans `GestionPompier.fxml`
- Quand on charge un module, on remplace le contenu de `mainStackPane`
- Quand on clique sur Dashboard/Pompiers, on recharge `GestionPompier.fxml` complètement
