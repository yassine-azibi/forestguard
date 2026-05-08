═══════════════════════════════════════════════════════════════
  DOSSIER SOUNDS — ForestGuard
═══════════════════════════════════════════════════════════════

📁 Ce dossier contient les fichiers audio utilisés par l'application.

🔊 FICHIER REQUIS :
  - notif.mp3  →  Son de notification pour les alertes et affectations

═══════════════════════════════════════════════════════════════
  UTILISATION
═══════════════════════════════════════════════════════════════

Le fichier notif.mp3 est joué en BOUCLE pendant l'affichage des popups :

1. NOTIFICATIONS D'ALERTES (NotificationService)
   - Démarre quand une nouvelle alerte est détectée
   - Joue en boucle pendant 20 secondes (durée du popup)
   - S'arrête automatiquement ou au clic sur le popup

2. AFFECTATIONS (PopupAffectationController)
   - Démarre quand une nouvelle affectation arrive
   - Joue en boucle jusqu'à ce que l'utilisateur accepte ou refuse
   - S'arrête quand le popup se ferme

═══════════════════════════════════════════════════════════════
  CONFIGURATION
═══════════════════════════════════════════════════════════════

Volume par défaut : 60% (configurable dans SoundPlayer.java)

Pour changer le volume :
  soundPlayer.setVolume(0.8); // 80%

Pour utiliser un autre fichier audio :
  1. Placer le fichier .mp3 dans ce dossier
  2. Modifier SoundPlayer.java ligne 24 :
     URL soundURL = getClass().getResource("/sounds/VOTRE_FICHIER.mp3");

═══════════════════════════════════════════════════════════════
  FORMAT AUDIO SUPPORTÉ
═══════════════════════════════════════════════════════════════

✅ MP3 (recommandé)
✅ WAV
✅ AAC
✅ M4A

Recommandations :
  - Durée : 2-5 secondes (sera répété en boucle)
  - Bitrate : 128-192 kbps
  - Taille : < 500 KB

═══════════════════════════════════════════════════════════════
  DÉPANNAGE
═══════════════════════════════════════════════════════════════

❌ "Fichier notif.mp3 introuvable"
   → Vérifier que notif.mp3 est bien dans src/main/resources/sounds/
   → Rebuild le projet Maven (mvn clean compile)

❌ "Erreur lors du chargement du son"
   → Vérifier le format du fichier (doit être MP3 valide)
   → Vérifier que JavaFX Media est bien dans le module-info.java

❌ Pas de son
   → Vérifier le volume système
   → Vérifier les logs console (🔊 ou ❌)
   → Tester avec un autre fichier audio

═══════════════════════════════════════════════════════════════
