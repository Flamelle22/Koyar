🚲 Vélo-ESP32 Dashboard & Controller
💡 Le Concept
Ce projet né d'une idée simple : pourquoi se contenter de pédaler quand on peut piloter un cockpit high-tech depuis son guidon ?

Le système repose sur un couplage direct entre un microcontrôleur ESP32 embarqué sur le vélo et une application mobile Flutter dédiée. L'objectif est d'offrir un contrôle total, instantané et sans compromis sur la sécurité et l'esthétique, en transformant le smartphone en un véritable tableau de bord de véhicule connecté.

Pas de fioritures graphiques inutiles : l'application adopte une identité visuelle minimaliste, géométrique et monochrome (fond noir absolu, contrastes en blanc pur) pour garantir une lisibilité parfaite en plein jour comme de nuit, tout en conservant une ergonomie taillée pour une utilisation rapide en roulant.

📱 Architecture et Organisation de l'Application
L'application s'articule autour de trois écrans principaux, pensés pour basculer de la configuration à l'action en une fraction de seconde :

1. L'Accueil (Mode Portrait)
La tour de contrôle de poche : Centralise l'état de la liaison Bluetooth, l'adresse MAC de l'ESP32 et l'état du verrouillage.

Le visuel dynamique : Intègre un schéma minimaliste du vélo reflétant l'état des feux en temps réel.

Les commandes rapides : Permet de verrouiller/déverrouiller le système, de couper/allumer l'éclairage global ou la sirène, et d'ajuster la couleur ou la luminosité globale via des curseurs (sliders) continus.

La passerelle : Un bouton d'accès direct bascule l'interface vers le mode grand écran pour le pilotage.

2. Les Paramètres (Mode Portrait)
Le panneau de configuration : Permet de lier l'application à l'ESP32 via l'adresse MAC (avec sauvegarde et option de connexion automatique au démarrage).

La personnalisation des Presets : Une grille dédiée où chaque utilisateur peut nommer et configurer précisément la chaîne de commande shell associée à chacun des 4 presets d'éclairage.

Le suivi technique : Intègre un terminal textuel (Shell Full History) pour garder un œil sur les interactions et un bloc fixe affichant l'historique de la dernière alerte de sécurité (Last Alarme en minutes avant).

3. Le Dashboard (Mode Paysage / Plein Écran - Le mode pilote)
Conçu pour être fixé directement sur le guidon, cet écran maximise l'espace horizontal en séparant les informations en trois blocs verticaux distincts :

Bloc Gauche (Métriques) : Affiche l'horloge temps réel, la vitesse instantanée et le kilométrage côte à côte, avec un accès discret pour revenir au menu.

Bloc Central (Presets) : Une colonne unique regroupant les 4 boutons de presets personnalisés pour changer d'ambiance lumineuse en un tap.

Bloc Droit (Commandes d'éclairage & Clignotants) :

Une colonne dédiée aux modes d'éclairage stricts : Code (Low), Phares (High), Éteint (Off), et Inhibe (Zéro feu, coupe tout y compris le stop).

Une colonne extrême-droite dédiée aux clignotants (flèches gauche/droite encadrant un bouton d'arrêt central STOP).

⚡ Le Protocole de Communication (Série / Bluetooth)
La communication entre le téléphone et l'ESP32 repose sur un protocole textuel simple, léger et ultra-rapide basé sur des flux série délimités par des retours à la ligne (\n).

Émissions (TX - Téléphone ➔ ESP32) :

Les appuis sur les boutons envoient les chaînes de commandes shell textuelles préconfigurées.

Les mouvements des curseurs de couleur crachent en continu le flux : color:HEX\n.

Réceptions (RX - ESP32 ➔ Téléphone) :

L'application écoute le flux en arrière-plan et parse dynamiquement les clés reçues : speed:, distance:, lock:, break:, light:, et lastalarme:.
