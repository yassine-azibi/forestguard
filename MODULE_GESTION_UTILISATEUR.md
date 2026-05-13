# Module Gestion Utilisateur — ForestGuard

## 📋 Description

Module complet de gestion des utilisateurs pour l'application ForestGuard. Permet l'administration, l'authentification, l'inscription et la gestion des profils utilisateurs.

## ✨ Fonctionnalités

### 🔐 Authentification
- Connexion classique (email + mot de passe)
- Connexion via Google OAuth2
- Mot de passe oublié avec code par email
- Hachage sécurisé BCrypt (12 rounds)

### 📝 Inscription
- Inscription classique avec validation complète
- Inscription via Google OAuth2
- Auto-détection de la localisation (gouvernorat)
- Validation téléphone tunisien (format E.164)
- Email de bienvenue automatique

### 👤 Profil Utilisateur
- Modification des informations personnelles
- Changement de mot de passe
- Suppression de compte
- Avatar généré automatiquement (DiceBear)
- Alertes et signaux

### 🛡️ Panneau Administration
- **Statistiques** : Total utilisateurs, comptes Google, comptes classiques, gouvernorats
- **Liste paginée** : 10 utilisateurs par page
- **Recherche temps réel** : Nom, email, téléphone, localisation
- **Filtrage** : Par gouvernorat
- **Actions** : Ajouter, modifier, supprimer
- **Protection** : Impossible de supprimer son propre compte admin

## 🏗️ Architecture

### Packages

```
com.forestguard
├── app/                    # Point d'entrée application
│   ├── AppLauncher.java
│   └── ForestGuardApp.java
├── controllers/            # Contrôleurs JavaFX
│   ├── AdminUtilisateurController.java
│   ├── LoginController.java
│   ├── RegisterController.java
│   ├── ProfileController.java
│   ├── DashboardController.java
│   ├── ForgotPasswordController.java
│   ├── AlertsController.java
│   ├── SignalController.java
│   ├── ChatController.java
│   └── EvenementController.java
├── entities/               # Entités métier
│   └── Utilisateur.java
├── services/               # Services métier
│   └── UtilisateurService.java
├── interfaces/             # Interfaces
│   └── IService.java
└── utils/                  # Utilitaires
    ├── MyConnection.java
    ├── PasswordHasher.java
    ├── Session.java
    ├── EmailService.java
    ├── GoogleAuthService.java
    ├── GovernorateUtils.java
    ├── PhoneNumberUtils.java
    ├── LocationDetectionService.java
    ├── AvatarService.java
    ├── SchemaInitializer.java
    ├── GpsDetector.java
    └── TwilioSmsService.java
```

### Vues FXML

```
src/main/resources/com/forestguard/views/
├── login.fxml              # Page de connexion
├── register.fxml           # Page d'inscription
├── profile.fxml            # Page profil utilisateur
├── dashboard.fxml          # Tableau de bord
├── admin_utilisateur.fxml  # Panneau administration
├── forgot_password.fxml    # Réinitialisation mot de passe
├── alerts.fxml             # Alertes incendie
├── signal.fxml             # Signalement incendie
├── chat.fxml               # Chat IA
└── evenement.fxml          # Événements forestiers
```

## 🗄️ Base de données

### Table `utilisateur`

```sql
CREATE TABLE utilisateur (
    id INT NOT NULL AUTO_INCREMENT,
    nom VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    telephone VARCHAR(30) NOT NULL,
    localisation VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL DEFAULT '',
    google_id VARCHAR(255) DEFAULT NULL,
    google_picture_url VARCHAR(512) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_utilisateur_email (email),
    UNIQUE KEY uq_utilisateur_google_id (google_id)
);
```

## ⚙️ Configuration

### Fichiers `.properties` requis

Créer ces fichiers dans `src/main/resources/` avec vos propres valeurs :

#### `db.properties`
```properties
db.url=jdbc:mysql://localhost:3306/forestguard?useSSL=false&serverTimezone=UTC
db.user=root
db.password=VOTRE_MOT_DE_PASSE
```

#### `email.properties`
```properties
mail.smtp.host=smtp.gmail.com
mail.smtp.port=465
mail.smtp.username=VOTRE_EMAIL@gmail.com
mail.smtp.password=VOTRE_APP_PASSWORD_GMAIL
mail.from.address=VOTRE_EMAIL@gmail.com
mail.from.name=ForestGuard
```

#### `google.properties`
```properties
google.client.id=VOTRE_CLIENT_ID.apps.googleusercontent.com
google.client.secret=VOTRE_CLIENT_SECRET
google.redirect.uri=http://localhost:8080/callback
```

#### `twilio.properties`
```properties
twilio.account.sid=VOTRE_ACCOUNT_SID
twilio.auth.token=VOTRE_AUTH_TOKEN
twilio.from.number=+1XXXXXXXXXX
```

#### `openai.properties`
```properties
openai.api.key=VOTRE_GROQ_API_KEY
openai.model=llama-3.1-8b-instant
openai.max.tokens=300
openai.temperature=0.7
```

## 📦 Dépendances Maven

```xml
<!-- BCrypt pour hachage mots de passe -->
<dependency>
    <groupId>org.mindrot</groupId>
    <artifactId>jbcrypt</artifactId>
    <version>0.4</version>
</dependency>

<!-- Jakarta Mail pour emails -->
<dependency>
    <groupId>com.sun.mail</groupId>
    <artifactId>jakarta.mail</artifactId>
    <version>2.0.1</version>
</dependency>

<!-- Twilio SDK pour SMS -->
<dependency>
    <groupId>com.twilio.sdk</groupId>
    <artifactId>twilio</artifactId>
    <version>9.14.0</version>
</dependency>
```

## 🚀 Lancement

### Depuis IntelliJ IDEA

1. Ouvrir le projet
2. Configurer JDK 17
3. Créer la base de données MySQL `forestguard`
4. Remplir les fichiers `.properties`
5. Run → `com.forestguard.app.AppLauncher`

### Depuis Maven

```bash
mvn clean javafx:run
```

## 🎨 Design

- **Thème** : Vert forêt avec dégradés sombres
- **Animations** : Transitions fluides, effets de survol
- **Responsive** : Adapté aux différentes résolutions
- **Accessibilité** : Labels clairs, feedback visuel

## 🔒 Sécurité

- ✅ Hachage BCrypt (12 rounds)
- ✅ Validation côté serveur
- ✅ Protection CSRF (state parameter OAuth2)
- ✅ PKCE pour OAuth2
- ✅ Unicité email/téléphone
- ✅ Secrets non versionnés

## 📝 Validation

### Email
- Format RFC 5322
- Unicité en base

### Téléphone
- Format E.164 (+216XXXXXXXX)
- 8 chiffres tunisiens
- Commence par 2, 4, 5, 7 ou 9
- Unicité en base

### Gouvernorat
- Liste des 24 gouvernorats tunisiens
- Normalisation avec alias
- Auto-détection via IP

## 🧪 Tests

Pour tester le module :

1. **Inscription classique** : Créer un compte avec email/mot de passe
2. **Inscription Google** : Créer un compte via Google OAuth2
3. **Connexion** : Se connecter avec les deux types de comptes
4. **Profil** : Modifier les informations, changer le mot de passe
5. **Admin** : Accéder au panneau, rechercher, filtrer, modifier, supprimer
6. **Mot de passe oublié** : Tester la réinitialisation par email

## 👥 Auteur

**Module développé par** : Équipe Gestion Utilisateur
**Projet** : ForestGuard — Surveillance intelligente des forêts
**Date** : Mai 2026

## 📄 Licence

Projet académique — Esprit School of Engineering
