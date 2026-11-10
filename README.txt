IPTV Player (Android, Kotlin + Media3/ExoPlayer)

1. Décompressez le dossier, ouvrez-le avec Android Studio (File > Open > IPTVPlayer).
2. Attendez la synchronisation Gradle (connexion Internet nécessaire la première fois).
3. Branchez votre téléphone (débogage USB) ou lancez un émulateur, puis cliquez sur Run.
4. Au premier lancement, la fenêtre des paramètres s'ouvre :
   - Lien M3U : collez le lien complet dans "Serveur", laissez utilisateur/mot de passe vides.
   - Xtream Codes : "Serveur" = http://adresse:port, puis utilisateur et mot de passe.
5. Pour générer un APK : Build > Build Bundle(s) / APK(s) > Build APK(s).

--- Nouveautés ---
Changement de chaîne dans le lecteur :
  * Téléphone : touchez l'écran, puis les boutons ▲ (suivante) et ▼ (précédente).
  * Télécommande : CH+ / CH-, ou flèche haut (suivante) / flèche bas (précédente)
    quand les contrôles ne sont pas affichés. OK/Entrée affiche les contrôles.
Android TV : l'application apparaît dans le lanceur TV (LEANBACK_LAUNCHER),
  la liste se parcourt avec les flèches de la télécommande (élément surligné en bleu).
  Astuce : saisir l'adresse du serveur avec la télécommande est long ; utilisez un clavier
  Bluetooth/USB ou l'appli Google TV sur le téléphone pour écrire le texte.

--- Compiler en ligne (sans Android Studio) ---
1. Créez un dépôt PRIVÉ sur github.com et envoyez-y le CONTENU du dossier IPTVPlayer
   (settings.gradle.kts doit être à la racine du dépôt), y compris .github/workflows/build.yml.
2. Onglet Actions > Build APK > Run workflow.
3. Quand c'est vert, ouvrez l'exécution et téléchargez l'artefact IPTVPlayer-debug-apk (zip contenant app-debug.apk).
4. Installez l'APK sur le téléphone/TV, ou testez-le dans le navigateur avec appetize.io.
