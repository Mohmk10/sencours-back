<div align="center">

# SenCours — Backend API

**API REST pour la plateforme e-learning SenCours**

*Democratiser l'acces a la formation au Senegal*

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.1-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![JWT](https://img.shields.io/badge/JWT-Auth-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)](https://jwt.io/)
[![Maven](https://img.shields.io/badge/Maven-3.9-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)

<br/>

[![License](https://img.shields.io/badge/License-MIT-green?style=flat-square)](LICENSE)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen?style=flat-square)](CONTRIBUTING.md)
[![Status](https://img.shields.io/badge/Status-Active-success?style=flat-square)]()

</div>

---

## Table des matieres

- [A propos](#-a-propos)
- [Architecture](#-architecture)
- [Fonctionnalites](#-fonctionnalites)
- [Technologies](#-technologies)
- [Prerequis](#-prerequis)
- [Installation](#-installation)
- [Configuration](#-configuration)
- [Endpoints API](#-endpoints-api)
- [Securite](#-securite)
- [Tests](#-tests)
- [Docker](#-docker)
- [Deploiement](#-deploiement)
- [Frontend](#-frontend)
- [Contribuer](#-contribuer)
- [Licence](#-licence)
- [Auteur](#-auteur)

---

## A propos

**SenCours** est une plateforme e-learning complete inspiree d'Udemy, concue pour l'ecosysteme educatif senegalais. Ce repository contient l'**API REST** qui alimente la plateforme.

Le projet a ete developpe dans le cadre d'un memoire de fin de cycle a **Sonatel Academy** (Orange Digital Center), specialite Developpement Web/Mobile.

### Objectifs

- Democratiser l'acces a la formation en ligne au Senegal
- Permettre aux instructeurs locaux de partager leurs connaissances
- Offrir une experience d'apprentissage moderne et accessible
- Integrer les moyens de paiement locaux (Orange Money, Wave, Free Money)

---

## Architecture

Le projet suit une **architecture en couches** (Layered Architecture) :

```
+-----------------------------------------------------------+
|                    Controllers (REST)                      |
|         Exposition des endpoints, validation               |
+-----------------------------------------------------------+
|                      Services                              |
|              Logique metier, orchestration                  |
+-----------------------------------------------------------+
|                    Repositories                            |
|           Acces aux donnees (Spring Data JPA)              |
+-----------------------------------------------------------+
|                      Entities                              |
|              Modele de donnees (JPA/Hibernate)              |
+-----------------------------------------------------------+
|                     PostgreSQL                             |
|                   Base de donnees                          |
+-----------------------------------------------------------+
```

### Structure du projet

```
src/main/java/com/sencours/
├── config/          # Configuration (Security, CORS, JWT)
├── controller/      # 16 Controleurs REST
├── dto/             # Data Transfer Objects
│   ├── request/     # DTOs de requete
│   └── response/    # DTOs de reponse
├── entity/          # 11 Entites JPA
├── enums/           # Enumerations (Role, Status, LessonType...)
├── exception/       # Exceptions personnalisees
├── mapper/          # Mappers Entity <-> DTO
├── repository/      # Repositories Spring Data
├── service/         # 16 Services metier
│   └── impl/        # Implementations
└── SencoursApplication.java
```

---

## Fonctionnalites

### Gestion des utilisateurs

- Authentification JWT (token valide 24h)
- 4 roles : `SUPER_ADMIN`, `ADMIN`, `INSTRUCTEUR`, `ETUDIANT`
- Inscription et connexion securisees (BCrypt)
- Systeme de candidature instructeur avec workflow de validation
- Suspension de compte et systeme d'appel

### Gestion des cours

- CRUD complet des cours avec sections et lecons
- 6 types de contenu : `VIDEO`, `VIDEO_UPLOAD`, `TEXT`, `PDF`, `IMAGE`, `QUIZ`
- Upload de fichiers (thumbnails, videos, documents) jusqu'a 500 Mo
- Cycle de vie : `DRAFT` → `PUBLISHED` → `ARCHIVED`
- Recherche avancee multi-criteres avec pagination et tri
- Autocomplete (suggestions de recherche)

### Inscriptions et paiements

- Inscription aux cours gratuits et payants
- Simulation de paiement (Orange Money, Wave, Free Money, Carte bancaire)
- Suivi de progression lecon par lecon
- Generation de certificats PDF a la completion (100%)
- Verification publique des certificats par numero unique

### Systeme d'avis

- Notes de 1 a 5 etoiles avec commentaires
- Calcul automatique de la moyenne par cours
- Moderation par les administrateurs

### Administration

- Gestion complete des utilisateurs
- Gestion des categories de cours
- Moderation des cours et des avis
- Candidatures instructeur (validation/rejet)
- Reinitialisation de base de donnees (SuperAdmin)

---

## Technologies

| Categorie | Technologie | Version |
|-----------|-------------|---------|
| Langage | Java | 21 |
| Framework | Spring Boot | 3.4.1 |
| Securite | Spring Security + JWT (jjwt) | 6.x / 0.12.3 |
| Persistance | Spring Data JPA / Hibernate | 3.x |
| Base de donnees | PostgreSQL (Neon Cloud) | 16 |
| Build | Maven | 3.9+ |
| Documentation | SpringDoc OpenAPI (Swagger) | 2.3.0 |
| Generation PDF | OpenPDF | 1.3.30 |
| Validation | Jakarta Validation | 3.0 |
| Utilitaire | Lombok | 1.18.x |
| Conteneur | Docker | Multi-stage build |

---

## Prerequis

- **Java 21** ou superieur
- **Maven 3.9** ou superieur
- **PostgreSQL 16** ou superieur (ou un compte [Neon](https://neon.tech))
- **Git**

---

## Installation

### 1. Cloner le repository

```bash
git clone https://github.com/Mohmk10/sencours-back.git
cd sencours-back
```

### 2. Configurer la base de donnees

Creer une base de donnees PostgreSQL :

```sql
CREATE DATABASE sencours;
```

### 3. Configurer l'application

Modifier `src/main/resources/application.yml` avec vos parametres :

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/sencours
    username: your_username
    password: your_password
  jpa:
    hibernate:
      ddl-auto: update

jwt:
  secret: votre-cle-secrete-base64-encodee-minimum-256-bits
  expiration: 86400000  # 24 heures en millisecondes
```

### 4. Lancer l'application

```bash
mvn spring-boot:run
```

L'API sera accessible sur `http://localhost:8080`

La documentation Swagger est disponible sur `http://localhost:8080/swagger-ui.html`

---

## Configuration

### Variables d'environnement (production)

| Variable | Description | Exemple |
|----------|-------------|---------|
| `DATABASE_URL` | URL de connexion PostgreSQL | `jdbc:postgresql://host:5432/sencours` |
| `JWT_SECRET` | Cle secrete JWT (min 256 bits, base64) | `VG9rZW5TZWNyZXRLZXk...` |
| `SPRING_PROFILES_ACTIVE` | Profil Spring actif | `prod` |
| `PORT` | Port du serveur | `8080` |

### Creation du SuperAdmin

Le SuperAdmin est cree directement en base de donnees :

**1. Generer le hash du mot de passe :**

```bash
curl -X POST http://localhost:8080/api/v1/utility/hash \
  -H "Content-Type: application/json" \
  -d '{"password": "VotreMotDePasse123!"}'
```

**2. Inserer en base :**

```sql
INSERT INTO users (first_name, last_name, email, password, role, is_active, created_at, updated_at)
VALUES ('Prenom', 'Nom', 'superadmin@sencours.sn',
        '$2a$10$HASH_GENERE', 'SUPER_ADMIN', true, NOW(), NOW());
```

---

## Endpoints API

> **99 endpoints** au total — Documentation interactive complete sur `/swagger-ui.html`

### Authentification

| Methode | Endpoint | Description | Auth |
|---------|----------|-------------|------|
| `POST` | `/api/v1/auth/register` | Inscription (role ETUDIANT) | Non |
| `POST` | `/api/v1/auth/login` | Connexion (retourne JWT) | Non |
| `GET` | `/api/v1/auth/me` | Profil utilisateur connecte | Oui |

### Cours

| Methode | Endpoint | Description | Auth |
|---------|----------|-------------|------|
| `GET` | `/api/v1/courses` | Liste de tous les cours | Non |
| `GET` | `/api/v1/courses/{id}` | Detail d'un cours | Non |
| `GET` | `/api/v1/courses/search?q=&categoryId=&minPrice=&maxPrice=&free=` | Recherche avancee avec filtres | Non |
| `GET` | `/api/v1/courses/search/quick?q=` | Recherche rapide | Non |
| `GET` | `/api/v1/courses/search/suggestions?q=` | Autocomplete | Non |
| `POST` | `/api/v1/courses` | Creer un cours | Instructeur |
| `PUT` | `/api/v1/courses/{id}` | Modifier un cours | Instructeur |
| `PATCH` | `/api/v1/courses/{id}/status` | Changer le statut | Instructeur |
| `DELETE` | `/api/v1/courses/{id}` | Supprimer un cours | Instructeur |

### Sections et Lecons

| Methode | Endpoint | Description | Auth |
|---------|----------|-------------|------|
| `POST` | `/api/v1/courses/{id}/sections` | Ajouter une section | Instructeur |
| `PUT` | `/api/v1/sections/{id}` | Modifier une section | Instructeur |
| `PUT` | `/api/v1/courses/{id}/sections/reorder` | Reordonner les sections | Instructeur |
| `POST` | `/api/v1/sections/{id}/lessons` | Ajouter une lecon | Instructeur |
| `PUT` | `/api/v1/lessons/{id}` | Modifier une lecon | Instructeur |
| `GET` | `/api/v1/lessons/{id}/preview` | Apercu d'une lecon (public) | Non |

### Inscriptions et Progression

| Methode | Endpoint | Description | Auth |
|---------|----------|-------------|------|
| `POST` | `/api/v1/enrollments/courses/{id}/pay` | Initier le paiement | Oui |
| `POST` | `/api/v1/enrollments/courses/{id}` | Finaliser l'inscription | Oui |
| `POST` | `/api/v1/enrollments/courses/{id}/free` | Inscription gratuite | Oui |
| `GET` | `/api/v1/enrollments/my-enrollments` | Mes inscriptions | Oui |
| `POST` | `/api/v1/progress/lessons/{id}/complete` | Marquer une lecon completee | Oui |
| `GET` | `/api/v1/progress/courses/{id}` | Progression d'un cours | Oui |

### Certificats

| Methode | Endpoint | Description | Auth |
|---------|----------|-------------|------|
| `GET` | `/api/v1/certificates/courses/{id}/download` | Telecharger le PDF | Oui |
| `GET` | `/api/v1/certificates/my-certificates` | Mes certificats | Oui |
| `GET` | `/api/v1/certificates/verify/{number}` | Verifier un certificat | Non |

### Avis

| Methode | Endpoint | Description | Auth |
|---------|----------|-------------|------|
| `POST` | `/api/v1/reviews/courses/{id}` | Laisser un avis | Oui |
| `GET` | `/api/v1/reviews/courses/{id}` | Avis d'un cours | Non |
| `GET` | `/api/v1/reviews/courses/{id}/average` | Note moyenne | Non |
| `DELETE` | `/api/v1/reviews/{id}` | Supprimer mon avis | Oui |

### Administration

| Methode | Endpoint | Description | Auth |
|---------|----------|-------------|------|
| `GET` | `/api/v1/admin/users` | Liste des utilisateurs | Admin |
| `PATCH` | `/api/v1/admin/users/{id}/toggle-status` | Activer/suspendre | Admin |
| `DELETE` | `/api/v1/admin/users/{id}` | Supprimer un utilisateur | SuperAdmin |
| `GET` | `/api/v1/admin/instructor-applications` | Candidatures instructeur | Admin |
| `PUT` | `/api/v1/admin/instructor-applications/{id}/review` | Valider/Refuser | Admin |
| `POST` | `/api/v1/super-admin/admins` | Creer un admin | SuperAdmin |
| `POST` | `/api/v1/super-admin/instructors` | Creer un instructeur | SuperAdmin |

---

## Securite

### Authentification JWT

1. L'utilisateur se connecte via `POST /api/v1/auth/login`
2. Le serveur retourne un token JWT (valide 24h)
3. Le client inclut le token dans chaque requete : `Authorization: Bearer <token>`
4. Le `JwtAuthenticationFilter` intercepte et valide le token

### Hierarchie des roles

```
SUPER_ADMIN
    |
    +-- Creer/supprimer des ADMIN
    +-- Creer des INSTRUCTEUR directement
    +-- Reinitialiser la base de donnees
    +-- Acces total
         |
      ADMIN
         |
         +-- Valider/refuser les candidatures instructeur
         +-- Gerer les categories et utilisateurs
         +-- Moderer les cours et avis
              |
         INSTRUCTEUR
              |
              +-- Creer/modifier/supprimer ses cours
              +-- Gerer sections et lecons
              +-- Upload de fichiers
                   |
              ETUDIANT
                   |
                   +-- S'inscrire aux cours
                   +-- Suivre sa progression
                   +-- Laisser des avis
                   +-- Candidater pour devenir instructeur
```

### Protections

- **Mots de passe** : haches avec BCrypt
- **Endpoints** : proteges par role via `@PreAuthorize` et matrice dans `SecurityConfig`
- **CSRF** : desactive (API stateless)
- **CORS** : configure pour le frontend
- **Sessions** : aucune (stateless via JWT)

---

## Tests

Le projet contient **36 fichiers de tests** couvrant toutes les couches :

- **6** tests d'entites JPA
- **3** tests de mappers
- **15** tests d'integration (controllers)
- **11** tests unitaires (services)

```bash
# Lancer tous les tests
mvn test

# Lancer avec couverture
mvn test jacoco:report

# Lancer un test specifique
mvn test -Dtest=CourseServiceTest
```

---

## Docker

```bash
# Build de l'image
docker build -t sencours-api .

# Lancer le conteneur
docker run -p 8080:8080 \
  -e DATABASE_URL=jdbc:postgresql://host:5432/sencours \
  -e JWT_SECRET=votre-cle-secrete \
  -e SPRING_PROFILES_ACTIVE=prod \
  sencours-api
```

Le Dockerfile utilise un **multi-stage build** (Maven 3.9 + Eclipse Temurin JDK 21 Alpine) pour une image legere.

---

## Deploiement

L'API est deployee sur **[Render](https://render.com)** avec Docker.

### Variables d'environnement sur Render

| Variable | Description |
|----------|-------------|
| `DATABASE_URL` | URL PostgreSQL (Neon) |
| `JWT_SECRET` | Cle secrete JWT |
| `SPRING_PROFILES_ACTIVE` | `prod` |

### Health Check

Le service utilise l'endpoint `GET /api/v1/categories` comme health check pour maintenir l'instance active.

---

## Frontend

Le frontend Angular est disponible dans un repository separe :

**[github.com/Mohmk10/sencours-front](https://github.com/Mohmk10/sencours-front)**

---

## Contribuer

Les contributions sont les bienvenues ! Consultez [CONTRIBUTING.md](CONTRIBUTING.md) pour les details.

1. Fork le projet
2. Creer une branche (`git checkout -b feature/ma-fonctionnalite`)
3. Commit (`git commit -m 'feat: ajouter ma fonctionnalite'`)
4. Push (`git push origin feature/ma-fonctionnalite`)
5. Ouvrir une Pull Request

---

## Licence

Distribue sous la licence MIT. Voir [LICENSE](LICENSE) pour plus d'informations.

---

## Auteur

**Mohamed Makan KOUYATE**

Projet de memoire de fin de cycle — Sonatel Academy (Orange Digital Center)
Specialite Developpement Web/Mobile

[![GitHub](https://img.shields.io/badge/GitHub-Mohmk10-181717?style=flat-square&logo=github)](https://github.com/Mohmk10)

---

<div align="center">

**Si ce projet vous a ete utile, n'hesitez pas a lui donner une etoile !**

</div>
