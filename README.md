# FeelTrack - Backend

Backend de l’application full stack **FeelTrack**, développé avec Spring Boot.

FeelTrack est une application permettant de collecter et analyser des avis utilisateurs, avec catégorisation automatique des sentiments (positif, neutre, négatif).

---

## Table des matières

- [À propos](#à-propos)
- [Fonctionnalités](#fonctionnalités)
- [Architecture](#architecture)
- [Prérequis](#prérequis)
- [Installation et exécution](#installation-et-exécution)
- [Infrastructure locale avec Docker](#-infrastructure-locale-avec-docker)
- [Fonctionnement](#fonctionnement)
- [Documentation de l’API](#documentation-de-lapi)
- [Endpoints de l'API](#endpoints-de-lapi)
- [Technologies utilisées](#technologies-utilisées)
- [Bonnes pratiques mises en œuvre](#-bonnes-pratiques-mises-en-œuvre)
- [Points d’évolution possibles](#points-dévolution-possibles)

---

## À propos

Ce dépôt contient le backend de l’application FeelTrack.  
Le frontend (Angular) est disponible dans un dépôt séparé :

https://github.com/Cherrygolo/feeltrack-frontend


## 🎯 Objectifs du projet

Ce backend a été conçu pour :

- Développer une API REST robuste avec Spring Boot
- Structurer une architecture backend maintenable et évolutive
- Intégrer un service externe d’analyse de sentiment (Hugging Face)
- Gérer la persistance des données avec JPA / Hibernate
- Fournir une base backend compatible avec une application full stack

---

## Fonctionnalités

- 📝 Création et gestion d’avis utilisateurs via API REST
- 👤 Gestion des clients associés aux avis
- 🤖 Analyse automatique du sentiment (POSITIF / NEUTRE / NÉGATIF)
- 🔄 Fallback interne si le service d’IA externe est indisponible
- 📊 Consultation et filtrage des avis par type de sentiment
- 📈 Statistiques globales des avis
- 🧪 Tests unitaires et d’intégration (JUnit, MockMvc)

---

## Architecture

L’application suit une architecture en couches, favorisant la lisibilité, la testabilité et l’évolutivité :

```
src/
 └── main/
     └── java/ld/sa_backend/
         ├── config          → configuration globale de l'application (ex: CORS)
         ├── controller      → endpoints REST (Customer, Review)
         ├── dto             → objets de transfert (DTO)
         ├── entity          → entités JPA (Customer, Review)
         ├── enums           → types métier (ReviewType)
         ├── exception       → gestion centralisée des erreurs
         ├── external        → intégration API Hugging Face
         ├── projection      → interfaces utilisées pour optimiser les requêtes (Spring Data Projections)
         ├── repository      → accès aux données (Spring Data JPA)
         ├── service         → logique métier (analyse de sentiment)
         └── wrapper         → objets de regroupement de données utilisés pour structurer ou enrichir les réponses internes
 └── resources/
     ├── application.properties
     └── docker-compose.yml
     ├──unit/ → tests unitaires
     ├──it/ → tests d’intégration
```

---

## Prérequis

- Java 17+
- Maven 3.8+
- Docker & Docker Compose
- IDE recommandé : IntelliJ IDEA, Eclipse ou VS Code

---

## Installation et exécution

### 1. Cloner le dépôt

```bash
git clone https://github.com/Cherrygolo/feeltrack-backend.git
cd feeltrack-backend
```

### 2. Lancer l’infrastructure (base de données)

```bash
cd src/main/resources
docker-compose up -d
```

### 3. Lancer l’API Spring Boot

```bash
./mvnw spring-boot:run
```

L’API est accessible à l’adresse :

```
http://localhost:8080/
```

---

## 🐳 Infrastructure locale avec Docker

Le projet utilise **Docker Compose** pour fournir les services d’infrastructure nécessaires au fonctionnement de l’API.

Les services conteneurisés sont :

- **MariaDB** : base de données relationnelle
- **Adminer** : interface web d’administration de la base

L’API Spring Boot est exécutée localement et se connecte à la base MariaDB exposée par Docker.

### Services exposés

- MariaDB : `localhost:3307`
- Adminer : http://localhost:8081

### Connexion à la base via Adminer

- Système : MySQL / MariaDB  
- Serveur : `db`  
- Utilisateur : `root`  
- Mot de passe : `root`

---

## Fonctionnement

L’API peut fonctionner selon deux modes, en fonction de la présence d’un token d’accès à l’API Hugging Face.

### 🔹 Avec un token Hugging Face

Lorsque le token est fourni, l’application utilise le modèle :
`nlptown/bert-base-multilingual-uncased-sentiment`

Le token doit être renseigné dans un fichier `config.properties` situé dans le dossier `src/main/resources` :

```
HUGGINGFACE_TOKEN=VOTRE_TOKEN_ICI
```

Étapes de l’analyse :

1. Le texte est envoyé à l’API Hugging Face via une requête HTTP POST
2. Le modèle renvoie un score de 1 à 5 étoiles
3. Le score est converti en type métier :

| Étoiles | Sentiment |
|-------|-----------|
| 1 – 2 | NEGATIVE  |
| 3     | NEUTRAL   |
| 4 – 5 | POSITIVE  |

En cas d’erreur ou de réponse invalide, un **fallback automatique** renvoie un sentiment `NEUTRAL`.

Ce modèle a été choisi car supporte **le français et plusieurs autres langues**, ce qui le rend adapté à des textes multilingues

---

### 🔹 Sans token Hugging Face

Si aucun token n’est fourni, une analyse simplifiée est appliquée :

- Détection de marqueurs de négation (`ne`, `n'`, `pas`, `jamais`, `aucun`, `sans`)  
- Analyse de quelques mots simples indicateurs de sentiment (`bon`, `bien`, `ok` pour positif ; `mal`, `non`, `nul` pour négatif)  
- Une négation s’applique au mot qui suit (ex. `pas bon` → négatif)  
- Score final calculé : positif → `POSITIVE`, négatif → `NEGATIVE` 

⚠️ Cette méthode est volontairement basique et sert uniquement de solution de secours.

---

## Documentation de l’API

La documentation de l’API est fournie directement dans ce README à travers des **exemples concrets de requêtes et de réponses JSON**.

L’intégration de Swagger / OpenAPI est identifiée comme une **évolution naturelle**, afin d’automatiser la documentation et faciliter l’intégration avec des clients externes.

---

## Endpoints de l'API

### Clients

#### POST /api/v1/customer

Crée un nouveau client.  

**Exemple de corps :**

```json
{
  "email": "alice@example.com",
  "phone": "0601020304"
}
```

**Réponses :**

201 Created : client créé avec succès

400 Bad Request : données invalides

#### GET /api/v1/customer

Récupère tous les clients.

Exemple de réponse :

```json
[
  { 
    "id": 1, 
    "email": "alice@example.com", 
    "phone": "0601020304" 
  },
  { 
    "id": 2, 
    "email": "bob@example.com", 
    "phone": "0602030405" 
  }
]
```

#### PUT /api/v1/customer/\{ID\}

Met à jour un client existant correspondant à l' ID pour correspondre aux informations pasées dans le corps


**Exemple de corps :**

```json
{
  "id": 1,
  "email": "alice.new@example.com",
  "phone": "0604050607"
}
```

Règles :

- L’id dans le corps doit correspondre à l’id dans l’URL.
- L’email doit être unique, soit ne pas être déjà utilisée par un client existant.


**Réponses :**

- 200 OK : client mis à jour avec succès, corps = client mis à jour.

- 400 Bad Request (code : ARGUMENTS_INVALID) : ID dans l’URL et id dans le corps ne correspondent pas.

- 409 Conflict (code : CONFLICT_WITH_EXISTING_DATA) : l’email est déjà utilisé par un autre client.

- 404 Not Found : aucun client avec l’ID fourni existe.


**Exemple de réponse 200 OK :**

{
  "id": 1,
  "email": "alice.new@example.com",
  "phone": "0604050607"
}

#### DELETE /api/v1/customer/\{ID\}

Supprime un client existant correspondant à l'ID fourni.

**Réponses :**

- 204 No Content : client supprimé avec succès

- 404 Not Found : aucun client trouvé avec l’ID fourni

{
  "code": "ENTITY_NOT_FOUND",
  "message": "No customer found with the ID : 1."
}


409 Conflict : le client a des avis associés et ne peut pas être supprimé

---

### Avis

#### POST /api/v1/review

Crée un avis pour un client existant ou nouveau à créer.

**Si client déjà existant :**

```json
{
  "text": "Super merci !",
  "customer": {
    "id": 12
  }
}
```

**Si nouveau client :**

```json
{
  "text": "Super merci !",
  "customer": {
    "email": "example@gmail.com",
    "phone": "123456789"
  }
}
```

email : obligatoire pour créer un nouveau client.

phone : optionnel.

**Réponse :**

Code :
- 201 Created

- 404 Bad Request : 
  - code REQUEST_BODY_INVALID : le JSON du corps est incorrect ;
  - code ARGUMENT_INVALIDS : le customer ou customer.email ou text sont manquant(s)/vide(s)

- 404 Not found : Si customer.id fourni mais non trouvé

Corps : objet Review créé, avec id, text, type (POSITIVE / NEGATIVE / NEUTRAL) et le customer associé.

####  GET /api/v1/review

Récupère tous les avis.

Paramètres : aucun

**Réponse :**

200 OK

Liste de tous les avis présents en base

#### GET /api/v1/review?type=\{TYPE\}

Récupère :
- les avis filtrés par type, si un type existant est spécifié 
- tous les avis existants, si aucune valeur est renseignée.

**Paramètres :**

type : POSITIVE, NEGATIVE ou NEUTRAL

**Exemple :**

GET /api/v1/review?type=POSITIVE


**Réponse :**

- 200 OK : Liste des avis correspondant au type demandé
- 404 Bad request - ENUM_VALUE_INVALID : le type indiqué est une autre valeur que celles attendues.


####  GET /api/v1/review/\{ID\}

Informations sur l'avis correspondant à l'ID demandé

#### DELETE /api/v1/review/\{ID\}

Supprime un avis existant.

**Paramètres :**

id : ID de l’avis à supprimer

**Réponse :**

- 204 No Content : suppression réussie

- 404 Not Found : l’avis n’existe pas

#### GET /api/v1/review/stats

Récupère les statistiques globales des avis.

Paramètres :
Aucun

**Réponse :**

200 OK

Exemple de réponse :
```json
{
  "positive": 80,
  "negative": 30,
  "neutral": 10
}
```

---

### Actuator

Les endpoints Actuator permettent de surveiller l'état et les métriques de l'application.

####GET /actuator/health

Indique l'état de l'application.

**Réponse :** 

200 OK

Exemple de réponse :
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "MariaDB",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 499963174912,
        "free": 123456789012,
        "threshold": 10485760,
        "exists": true
      }
    }
  }
}
```

Explication rapide de la réponse :

- status :

  - UP → application OK

  - DOWN → problème global

- components : chaque sous-système exposé par Actuator.
ex :
db → connectivité base de données
diskSpace → espace disque

- details : informations techniques propres à chaque composant

---

## Technologies utilisées

- Java 17
- Spring Boot 3
- Spring Web / REST
- Spring Data JPA / Hibernate
- Maven
- JUnit / MockMvc
- Docker
- Docker Compose
- MariaDB
- Adminer

---

## Bonnes pratiques mises en œuvre

- Architecture en couches (Controller / Service / Repository)
- Séparation Entity / DTO
- Validation des entrées utilisateur
- Gestion centralisée des exceptions
- Intégration externe isolée et testable
- Fallback automatique en cas d’indisponibilité d’un service externe
- Utilisation de Docker pour l’infrastructure locale
- Code modulaire et extensible

---

## Points d’évolution possibles

Les évolutions suivantes sont volontairement identifiées afin de démontrer la capacité du projet à évoluer vers un contexte de production :

- Conteneurisation complète de l’API Spring Boot
- Ajout de Swagger / OpenAPI
- Sécurisation de l’API (JWT / Spring Security)
- Centralisation de la configuration via variables d’environnement
- Ajout d’un front-end (Angular ou React)
- Mise en place de métriques et de monitoring

---

## Conclusion

Ce projet met l’accent sur la **qualité du code**, la **clarté de l’architecture** et des **choix techniques réfléchis**, notamment l’utilisation de Docker pour l’infrastructure locale.

Il constitue une base saine pour une API REST Java prête à être intégrée dans un environnement professionnel.

