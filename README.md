# Projet BRi — Services dynamiques

Projet de Qualité de développement : une plateforme Java permettant aux
programmeurs d'installer et de mettre à jour des services depuis un serveur FTP,
et aux utilisateurs amateurs de les exécuter. Trois services sont fournis :
inversion d'un texte, conversion en majuscules et comptage de mots.

## Membres du groupe

- ALUM MILLARES Anja Raphaela
- TOUHAMI Malak
- BEN HADJ YOUNES Dora

## Prérequis

Un **JDK 17 ou supérieur** et Eclipse ou IntelliJ IDEA. Encodage : **UTF-8**.
Aucune bibliothèque Java externe n'est nécessaire.

## Exécution dans Eclipse

1. Importer le dossier avec **File > Import > General > Existing Projects into Workspace**.
2. Associer **JavaSE-17** à un JDK installé, puis faire **Project > Clean**.
3. Dans le dossier `launch`, lancer **BRi Server.launch** avec **Run As > BRi Server**.
4. Laisser le serveur actif, puis lancer **BRi Amateur Client.launch** ou
   **BRi Programmer Client.launch** de la même façon. Les arguments sont déjà configurés.

Si le projet est déjà importé, le sélectionner et appuyer sur **F5** avant de compiler.

## Exécution dans IntelliJ IDEA

Ouvrir le projet, sélectionner le JDK et vérifier que `src_client`, `src_serveur`
et `test` sont des **Sources Root**. Lancer les classes avec les configurations suivantes :

| Application | Classe principale | Arguments |
| --- | --- | --- |
| Serveur, à lancer en premier | `bri.BRiLaunch` | Aucun |
| Client amateur | `clientama.ClientAma` | `localhost 3000` |
| Client programmeur | `clientprog.ClientProg` | `localhost 4000` |

## Utilisation

Dans la console du client amateur, choisir `1`, `2` ou `3`, puis saisir le texte.
Exemple : `1`, puis `bonjour` donne `ruojnob`.

Le client programmeur propose l'inscription, l'installation, le changement
d'adresse FTP et la mise à jour. Un **véritable serveur FTP** est nécessaire
pour déposer des services ; le package de la classe doit correspondre au login.

Chaque client effectue une action puis se termine : le relancer pour continuer.
Arrêter le serveur avec **Terminate/Stop** dans l'IDE. Les comptes et les services
ajoutés sont conservés uniquement jusqu'à l'arrêt du serveur.
