# FeelTrack - Backend

Backend de l’application full stack **FeelTrack**, développé avec Spring Boot.

FeelTrack est une application permettant de collecter et analyser des avis utilisateurs, avec catégorisation automatique des sentiments (positif, neutre, négatif).

---

## Table des matières

- [À propos](#à-propos)
- [Objectifs du projet](#-objectifs-du-projet)
- [Fonctionnalités](#fonctionnalités)
- [Architecture](#architecture)
- [Prérequis](#prérequis)
- [Configuration](#️-configuration)
- [Installation et exécution](#-installation-et-exécution)
- [Infrastructure locale (Docker)](#-infrastructure-locale-docker)
- [Containerisation (Dockerfile - déploiement cloud)](#-containerisation-dockerfile---déploiement-cloud)
- [Fonctionnement de l'analyse de sentiment](#-fonctionnement-de-lanalyse-de-sentiment)
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

- Java 21+
- Maven 3.8+
- Docker & Docker Compose
- IDE recommandé : IntelliJ IDEA, Eclipse ou VS Code

---

## ⚙️ Configuration

Le projet utilise deux environnements principaux :

- **Développement local** : MariaDB via Docker Compose
- **Production** : PostgreSQL (Supabase)

### Variables d’environnement (production uniquement)

Ces variables sont nécessaires uniquement pour le profil `prod` (déploiement Render ou exécution locale en mode production).

- SPRING_PROFILES_ACTIVE=prod
- SPRING_DATASOURCE_URL
- SPRING_DATASOURCE_USERNAME
- SPRING_DATASOURCE_PASSWORD

---

## 🚀 Installation et exécution

### 1. Prérequis

Avant de lancer le projet, assure-toi d’avoir :

Java 21+
Maven 3.8+
Docker (optionnel si utilisation de Supabase)
Une connexion internet (pour Hugging Face et Supabase)

### 2. Choisir un mode d’exécution

Le projet peut fonctionner dans deux configurations :

#### Option A — Mode développement local (recommandé pour tester)

Utilise une base MariaDB locale via Docker.

1. Lancer la base de données

```bash
cd src/main/resources
docker-compose up -d
```

Cela démarre :
- MariaDB (port 3307)
- Adminer (http://localhost:8081)

2. Lancer l’API Spring Boot
```bash
./mvnw spring-boot:run
```

Accès :
API : http://localhost:8080/api
Adminer : http://localhost:8081

#### Option B — Mode production local (Supabase)

Utilise PostgreSQL hébergé sur Supabase.

1. Définir le profil prod
```bash
SPRING_PROFILES_ACTIVE=prod
```

2. Définir les variables d’environnement
```bash
SPRING_DATASOURCE_URL=...
SPRING_DATASOURCE_USERNAME=...
SPRING_DATASOURCE_PASSWORD=...
HUGGINGFACE_TOKEN=...
```

Pour avoir plus de détails sur HUGGINGFACE_TOKEN voir la section [Fonctionnement de l'analyse de sentiment](#-fonctionnement-de-lanalyse-de-sentiment) .

3. Lancer l’application
./mvnw spring-boot:run

---

## 🐳 Infrastructure locale (Docker)

Le projet fournit une infrastructure de développement via Docker Compose.

Services disponibles :
MariaDB → base de données locale
Adminer → interface web (http://localhost:8081)

Ce mode est uniquement destiné au développement local.

---

## 📦 Containerisation (Dockerfile - déploiement cloud)

Cette section est importante mais elle n’est PAS une alternative à l’exécution locale

Le Dockerfile sert uniquement à :

construire une image de production
déployer sur Render / cloud
exécuter l’application sans Java installé

### Rôle du Dockerfile

Le Dockerfile permet de :

- construire le projet Spring Boot en .jar
- exécuter l’application dans un conteneur Java léger
- garantir un environnement reproductible
- faciliter le déploiement cloud

### Build multi-stage
1. Build stage
  - compilation Maven
  - génération du .jar
2. Runtime stage
  - image Java légère
  - exécution du .jar

__Avantages :__
- image plus légère
- démarrage plus rapide
- sécurité améliorée

### ☁️ Déploiement cloud

Sur une plateforme comme Render :

1. pull du repo Git
2. build Docker image
3. run container
4. exposition via URL publique

__⚠️Important :__
le Dockerfile n’est PAS utilisé pour le développement local
il ne remplace pas spring-boot:run
il est uniquement destiné au cloud / production

---

## 🤖 Fonctionnement de l’analyse de sentiment

L’API peut fonctionner selon deux modes, en fonction de la présence d’un token Hugging Face.


### Avec un token Hugging Face (mode IA)

Lorsque la variable d’environnement `HUGGINGFACE_TOKEN` est fournie, l’application utilise un modèle pré-entraîné :

`nlptown/bert-base-multilingual-uncased-sentiment`

#### Étapes de traitement :

1. Le texte utilisateur est envoyé au modèle via une requête HTTP POST
2. Le modèle retourne un score de sentiment compris entre 1 et 5 étoiles
3. Le backend interprète ce score pour produire une classification métier :

| Score | Sentiment |
|------|-----------|
| 1 – 2 | NEGATIVE  |
| 3     | NEUTRAL   |
| 4 – 5 | POSITIVE  |

#### ⚠️ Gestion des erreurs

En cas d’erreur, timeout ou réponse invalide du service externe, une valeur par défaut `NEUTRAL` est retournée (fallback).

#### Remarque

Ce modèle est multilingue et fonctionne correctement avec le français.

---

### Sans token Hugging Face (mode fallback local)

Si la variable `HUGGINGFACE_TOKEN` n’est pas définie, l’application utilise une analyse locale simplifiée.

#### Méthode utilisée :

- Détection de mots de négation :
  `ne`, `n'`, `pas`, `jamais`, `aucun`, `sans`
- Détection de mots positifs :
  `bon`, `bien`, `ok`
- Détection de mots négatifs :
  `mal`, `non`, `nul`
- Gestion simple des inversions :
  ex : `pas bon` → NEGATIVE

#### Résultat final :

- POSITIVE
- NEGATIVE
- NEUTRAL (par défaut si ambigu)

⚠️ Cette approche est volontairement simplifiée et sert uniquement de solution de secours lorsque le service IA externe n’est pas disponible.

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

#### GET /api/v1/review/stats/timeline

Récupère la série temporelle des avis agrégés selon une granularité et une période données.

Ce endpoint est utilisé pour alimenter les graphiques de type timeline dans le dashboard analytics.

---

Paramètres :

- `days` *(optional, default = 30)*  
  Nombre de jours à remonter à partir d’aujourd’hui.

- `granularity` *(optional)*  
  Niveau d’agrégation des données.

  Valeurs possibles :
  - `DAY`
  - `WEEK`
  - `MONTH`

Si `granularity` n’est pas fourni, il est automatiquement déterminé en fonction de `days`.

---

**Règle de granularité automatique**

Si `granularity` est omis, le backend applique la règle suivante :

- `days <= 14` → `DAY`
- `15 <= days <= 60` → `WEEK`
- `days > 60` → `MONTH`

---

Réponse :

**200 OK**

```json
[
  {
    "startingPeriodDate": "2025-10-06",
    "positive": 3,
    "negative": 0,
    "neutral": 1
  }
]
```

**Exemples :**
##### Cas 1 — granularité automatique (DAY)

GET /api/v1/review/stats/timeline?days=7

Réponse :
```json
{
  "granularity": "DAY",
  "data": [
    {
      "startingPeriodDate": "2025-06-11",
      "positive": 2,
      "negative": 1,
      "neutral": 0
    }
  ]
}
```

##### Cas 2 — granularité automatique (WEEK)

GET /api/v1/review/stats/timeline?days=30

Réponse :
```json
{
  "granularity": "WEEK",
  "data": [
    {
      "startingPeriodDate": "2025-09-29",
      "positive": 10,
      "negative": 3,
      "neutral": 2
    },
    {
      "startingPeriodDate": "2025-10-06",
      "positive": 7,
      "negative": 1,
      "neutral": 4
    }
  ]
}
```

##### Cas 3 — granularité explicite (MONTH)

GET /api/v1/review/stats/timeline?days=90&granularity=MONTH

Réponse :
```json
{
  "granularity": "MONTH",
  "data": [
    {
      "startingPeriodDate": "2025-10-01",
      "positive": 2,
      "negative": 1,
      "neutral": 0
    }
  ]
}
```

##### Explication du format
startingPeriodDate représente le début de la période agrégée
Les périodes sans données apparaissent avec zéro comme valeur pour ```positive```,```negative```,```neutral```.
Le backend contrôle la granularité pour optimiser la lisibilité des graphiques

__Cas particulier :__
Si aucun avis n’existe sur la période → retourne []

---

### Actuator

Les endpoints Actuator permettent de surveiller l'état et les métriques de l'application.

#### GET /actuator/health

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

- Déploiement dans le cloud
- Ajout de Swagger / OpenAPI
- Sécurisation de l’API (JWT / Spring Security)
- Centralisation de la configuration via variables d’environnement
- Ajout d’un front-end (développement du front-end Angular en cours)

---

## Conclusion

Ce projet met l’accent sur la **qualité du code**, la **clarté de l’architecture** et des **choix techniques réfléchis**, notamment l’utilisation de Docker pour l’infrastructure locale.

Il constitue une base saine pour une API REST Java prête à être intégrée dans un environnement professionnel.

