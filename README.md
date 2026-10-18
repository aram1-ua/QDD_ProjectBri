# QDD_ProjectBri - Serveur de services dynamiques (FR)

Le but du projet est de créer un serveur capable de recevoir de nouveaux services apportés par des programmeurs et de les exécuter pour des utilisateurs amateurs via des Sockets et des Threads.


## Structure actuelle 

* bri.Service : L'interface que tous les services doivent suivre (hérite de Runnable)
* bri.ServiceRegistry : Le registre qui permet de vérifier et de garder la liste des services autorisés


## La norme BRi

Pour qu'un service soit accepté par ServiceRegistry.addService(), il doit:
* Implémenter bri.Service
* Être public et non abstract
* Avoir un constructeur public(Socket)
* Avoir la méthode public static String toStringue()


## Ce qu'il reste à faire:

Le serveur Amateur (ServiceAmateur)
-> gère les clients venus pour un service

Le serveur programmeur (ServiceProg)
-> ajoute dynamiquement de nouveaux services sur le serveur

Le serveur principal (BRiLaunch/Main)
-> Classe qui contient le main qui démarre tout au lancement de l'appli
  * Lance le serveur Amateur dans un Thread
  * Lance le serveur Programmeur dans un Thread





# QDD_ProjectBri - Dynamic Services Server (EN)

The goal of this project is to build a server capable of receiving new services submitted by programmers and running them for end-users (amateurs) using Sockets and Threads.


## Current Structure

- bri.Service: The interface that all services must implement (extends Runnable)
- bri.ServiceRegistry: The central registry used to validate and keep track of authorized services


## The BRi Standard

To be accepted by ServiceRegistry.addService(), a service must:
* Implement bri.Service
* Be public and non-abstract
* Have a public(Socket) constructor
* Have a public static String toStringue() method


## Remaining Tasks

Amateur Server (ServiceAmateur): 
-> Handles clients connecting to use a service.

Programmer Server (ServiceProg): 
-> Dynamically registers new services onto the server

Main Server (BRiLaunch / Main): 
-> Contains the main method that orchestrates application startup
  * Launches the Amateur server in a dedicated Thread
  * Launches the Programmer server in a dedicated Thread
