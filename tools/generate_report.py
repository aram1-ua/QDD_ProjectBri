"""Generate the submission report. Requires reportlab; Java has no Python dependency."""
from pathlib import Path
from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import mm
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / "output/pdf/Presentation_BRi.pdf"
DEST.parent.mkdir(parents=True, exist_ok=True)
styles = getSampleStyleSheet()
styles.add(ParagraphStyle(name="BodyBRi", fontName="Helvetica", fontSize=10.5, leading=15, spaceAfter=9, alignment=TA_LEFT))
styles.add(ParagraphStyle(name="TitleBRi", fontName="Helvetica-Bold", fontSize=25, leading=30, textColor=colors.HexColor("#18354A"), spaceAfter=13))
styles.add(ParagraphStyle(name="SectionBRi", fontName="Helvetica-Bold", fontSize=14, leading=18, textColor=colors.HexColor("#18354A"), spaceBefore=13, spaceAfter=8))
styles.add(ParagraphStyle(name="SmallBRi", fontName="Helvetica", fontSize=9, leading=12, spaceAfter=7))
story = []


def p(text, style="BodyBRi"):
    story.append(Paragraph(text, styles[style]))


def heading(text):
    p(text, "SectionBRi")


def table(rows, widths):
    cells = [[Paragraph(str(value), styles["SmallBRi"]) for value in row] for row in rows]
    result = Table(cells, colWidths=widths, hAlign="LEFT")
    result.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#E4EDF2")),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("BOX", (0, 0), (-1, -1), .5, colors.HexColor("#BBCBD5")),
        ("INNERGRID", (0, 0), (-1, -1), .35, colors.HexColor("#D3DFE5")),
        ("LEFTPADDING", (0, 0), (-1, -1), 8),
        ("RIGHTPADDING", (0, 0), (-1, -1), 8),
        ("TOPPADDING", (0, 0), (-1, -1), 6),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
    ]))
    story.append(result)


p("BRi : services dynamiques", "TitleBRi")
p("R5A08 - Qualité de développement | Présentation technique<br/>État vérifié le 8 novembre 2026", "SmallBRi")
heading("1. Objectif et périmètre réalisé")
p("La plateforme met en relation des programmeurs Java et des utilisateurs amateurs. Le serveur BRiLaunch reçoit les deux publics sur des ports distincts. Un programmeur inscrit fournit l'adresse de son répertoire FTP et peut installer ou mettre à jour une classe de service pendant que le serveur reste en fonctionnement.")
p("Le périmètre vise le cahier des charges minimum : services sans ressources partagées, échangeant des textes. Trois exemples sont fournis au démarrage : inversion d'un texte, conversion en majuscules et comptage de mots. Le client amateur et le client programmeur sont deux applications console distinctes.")
table([
    ["Exigence obligatoire", "Réalisation"],
    ["Deux ports et deux applications clientes", "BRiLaunch : 3000 / 4000 ; ClientAma et ClientProg"],
    ["Compte programmeur et changement FTP", "Login/mot de passe, certification automatique, validation du login et de l'URL"],
    ["Installation depuis le FTP", "Téléchargement binaire de la classe, contrôle BRi et contrôle du package"],
    ["Mise à jour sans redémarrage", "Nouveau chargeur par version ; remplacement atomique au même numéro"],
    ["Choix et exécution d'un service amateur", "Menu dynamique ; lancement avec la socket de la session"],
], [70 * mm, 98 * mm])
heading("2. Organisation du code")
p("<b>src_serveur/bri</b> contient les listeners, les gestionnaires de sessions, les registres et le chargeur FTP. <b>src_client</b> contient les deux clients et les trois services d'exemple. <b>test</b> contient des tests autonomes sans bibliothèque Java externe. Les scripts fournis construisent le projet avec un JDK 17 ou supérieur, en ciblant Java 17. Le README donne aussi les commandes javac directes et les étapes d'import Eclipse.")
p("Les extensions JAR, les services avec fichiers ou ressources partagées, la messagerie, l'envoi de courriels et les commandes explicites démarrer/arrêter/désinstaller sont optionnels dans l'énoncé et ne sont pas réalisés.")
story.append(PageBreak())

p("Choix de développement", "TitleBRi")
heading("3. Chargement réellement distant")
p("Un nom tel que alice.MonService correspond au fichier alice/MonService.class sous le répertoire FTP déclaré. Chaque installation ou mise à jour utilise un nouveau FtpServiceLoader. Les classes du package du programmeur sont téléchargées directement : aucune classe déjà présente dans le classpath du serveur ne peut masquer un FTP inaccessible. L'interface bri.Service conserve en revanche le chargeur de la plateforme afin de préserver son identité Java.")
p("Ce choix évite le défaut d'un URLClassLoader parent-first qui pourrait retrouver une ancienne classe locale. Un nouveau chargeur donne aussi une nouvelle identité à chaque version : les futures sessions voient la nouvelle version tandis qu'une session déjà commencée termine avec son ancienne classe. Les ressources sont libérées lorsque les anciennes classes et leur chargeur ne sont plus référencés.")
heading("4. Validation et remplacement atomique")
p("Le registre exige une classe publique, concrète, implémentant bri.Service, un constructeur public(Socket) et une méthode déclarée public static String toStringue(). La description est évaluée avant installation et doit tenir sur une ligne non vide. Le package doit être exactement le login du programmeur. Les règles implémentées correspondent au contrat BRi disponible dans le dépôt ; d'éventuelles règles supplémentaires du TP5 restent à comparer au support de cours non fourni.")
p("Chaque entrée conserve sa classe, sa description et son propriétaire. Une installation en double est refusée ; la commande de mise à jour exige une entrée existante appartenant au même programmeur. Tous les contrôles précèdent la modification du registre. Une classe invalide, un téléchargement échoué ou un bytecode corrompu laisse donc la version précédente disponible. Le numéro du service est conservé.")
heading("5. Sockets, protocole et concurrence")
p("Les messages sont des lignes UTF-8. Un menu se termine par une ligne vide. Le client amateur choisit un numéro, attend la consigne du service, envoie son texte puis lit le résultat. Le client programmeur envoie un numéro d'action, le login, le mot de passe et une valeur : URL FTP pour inscription/changement, nom complet de classe pour installation/mise à jour. Chaque connexion réalise une action et reçoit une réponse OK ou ERROR.")
p("Les acceptations et sessions s'exécutent dans des tâches concurrentes. Les opérations de compte et de registre sont synchronisées, notamment pour empêcher deux inscriptions simultanées avec le même login. Les sessions ont un délai de lecture de 30 secondes ; les téléchargements ont des délais de connexion/lecture de 5 secondes et une limite de 4 Mio par classe. La fermeture du serveur ferme les listeners et les sockets actives. Les deux ports sont réservés avant de démarrer les tâches : un échec de bind ne laisse pas un demi-serveur.")
story.append(PageBreak())

p("Validation et utilisation", "TitleBRi")
heading("6. Résultats de vérification")
p("La commande <b>test.cmd</b> sous Windows (ou ./test.ps1) compile avec --release 17 -Xlint:all et lance les trois exécutables de tests. Résultat observé : <b>110 vérifications réussies</b>, sans avertissement de compilation. Les tests utilisent leurs propres ports locaux et n'exigent ni accès Internet ni serveur FTP externe.")
table([
    ["Test", "Nombre", "Ce qui est vérifié"],
    ["ProgrammerAccountTest", "38", "Validation, authentification, changement FTP, inscription concurrente en double"],
    ["AmateurFlowTest", "16", "Services texte, Unicode, texte vide, saisies invalides, abandon, concurrence"],
    ["PlatformFlowTest", "56", "Vrais handlers et clients, FTP binaire, installations et mises à jour en fonctionnement"],
], [49 * mm, 18 * mm, 101 * mm])
p("Le test de plateforme compile un service hors du classpath applicatif et le distribue via un petit serveur FTP de test. Il installe v1, ouvre une session, publie v2 sans redémarrer, vérifie v2 pour un nouveau client et v1 pour l'ancienne session. Il change ensuite de répertoire FTP et publie v3. Il contrôle aussi les mots de passe/packages incorrects, doublons, classes non conformes, bytecode corrompu, requêtes tronquées, propriété des services et indisponibilité du FTP.")
heading("7. Démonstration reproductible")
p("Exécuter <b>build.cmd</b> sous Windows, puis <b>java -cp build/classes bri.BRiLaunch</b>. Dans deux autres terminaux, lancer respectivement <b>clientama.ClientAma localhost 3000</b> et <b>clientprog.ClientProg localhost 4000</b> avec le même préfixe java -cp build/classes. Le README décrit aussi la compilation sans script, le dépôt sur un vrai FTP et les actions du client programmeur. Les clients se relancent pour chaque nouvelle action.")
heading("8. Limites et améliorations possibles")
p("Les comptes et services installés sont en mémoire et repartent à zéro au redémarrage. Les mots de passe sont conservés en mémoire ; les protocoles FTP et socket de l'exercice ne sont pas chiffrés. Les services s'exécutent dans la JVM du serveur et doivent être de confiance. Le pool de sessions n'est pas borné. Les délais réseau limitent une lecture bloquée, mais pas la durée totale d'un transfert qui progresse lentement. Toutes les configurations de serveurs FTP externes n'ont pas été testées.")
p("Les améliorations possibles sont la persistance, le stockage dérivé des mots de passe, un transport protégé, une isolation des services et un pool borné. Ces points ne sont pas présentés comme déjà réalisés. Les tests étayent le scénario pédagogique minimum ; ils ne garantissent pas l'absence absolue de défauts pour tout service tiers.")
p("<b>Remise :</b> package.ps1 produit une archive du code source et de ce rapport sous la limite de 2 Mo. Utiliser -GroupName avec les noms réels des membres du groupe avant le dépôt Moodle. Les dépendances et classes compilées sont exclues ; les fichiers d'import Eclipse sont inclus.", "SmallBRi")


def footer(canvas, doc):
    canvas.setStrokeColor(colors.HexColor("#BBCBD5"))
    canvas.line(21 * mm, 17 * mm, 189 * mm, 17 * mm)
    canvas.setFont("Helvetica", 8)
    canvas.setFillColor(colors.HexColor("#546979"))
    canvas.drawString(21 * mm, 12 * mm, "BRi | R5A08 | Présentation technique")
    canvas.drawRightString(189 * mm, 12 * mm, str(doc.page))


SimpleDocTemplate(str(DEST), pagesize=A4, rightMargin=21 * mm, leftMargin=21 * mm,
                  topMargin=20 * mm, bottomMargin=23 * mm,
                  title="BRi - Présentation technique", author="Groupe projet BRi").build(
                      story, onFirstPage=footer, onLaterPages=footer)
print(DEST)
