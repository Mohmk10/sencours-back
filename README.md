![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-black?style=for-the-badge&logo=JSON%20web%20tokens)

# SenCours API

Backend REST API pour SenCours, la première plateforme d'apprentissage en ligne sénégalaise.

## 🎓 Contexte

Projet de mémoire de fin de formation à la **Sonatel Academy** (Orange Digital Center), développé dans le cadre du programme de formation aux métiers du numérique.

## ✨ Fonctionnalités

- **Authentification JWT** — Inscription, connexion, gestion des sessions
- **Gestion des rôles** — SUPER_ADMIN, ADMIN, INSTRUCTEUR, ETUDIANT
- **Catalogue de cours** — CRUD complet avec catégories
- **Système d'inscription** — Enrollment et suivi de progression
- **Candidatures instructeur** — Workflow de validation
- **Reviews et notes** — Système d'évaluation des cours
- **Pagination et recherche** — API optimisée

## 🛠️ Stack Technique

| Technologie | Version | Description |
|-------------|---------|-------------|
| Java | 21 | Langage principal |
| Spring Boot | 3.4 | Framework backend |
| Spring Security | 6.x | Authentification & autorisation |
| Spring Data JPA | 3.x | Persistance des données |
| PostgreSQL | 16 | Base de données |
| JWT | — | Tokens d'authentification |
| Lombok | — | Réduction du boilerplate |
| Maven | 3.9+ | Gestion des dépendances |

## 📁 Structure du Projet

```
src/main/java/com/sencours/
├── config/          # Configuration (Security, CORS)
├── controller/      # REST Controllers
├── dto/             # Data Transfer Objects
│   ├── request/     # Requêtes entrantes
│   └── response/    # Réponses sortantes
├── entity/          # Entités JPA
├── enums/           # Énumérations (Role, Status, ApplicationStatus)
├── exception/       # Gestion des exceptions
├── repository/      # Repositories JPA
└── service/         # Logique métier
    └── impl/        # Implémentations
```

## 🚀 Installation

### Prérequis

- Java 21+
- Maven 3.9+
- PostgreSQL 16+

### Configuration

1. Cloner le repository

```bash
git clone https://github.com/Mohmk10/sencours-back.git
cd sencours-back
```

2. Configurer la base de données dans `src/main/resources/application.properties`

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/sencours
spring.datasource.username=your_username
spring.datasource.password=your_password
```

3. Lancer l'application

```bash
mvn spring-boot:run
```

L'API sera accessible sur `http://localhost:8080`

## 📚 Documentation API

### Authentification

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/api/v1/auth/register` | Inscription |
| POST | `/api/v1/auth/login` | Connexion |
| GET | `/api/v1/auth/me` | Profil utilisateur |

### Cours

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/api/v1/courses` | Liste des cours |
| GET | `/api/v1/courses/{id}` | Détail d'un cours |
| POST | `/api/v1/courses` | Créer un cours |
| PUT | `/api/v1/courses/{id}` | Modifier un cours |
| DELETE | `/api/v1/courses/{id}` | Supprimer un cours |

### Catégories

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/api/v1/categories` | Liste des catégories |
| POST | `/api/v1/categories` | Créer une catégorie |

### Inscriptions

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/api/v1/enrollments/courses/{id}` | S'inscrire à un cours |
| GET | `/api/v1/enrollments/my-enrollments` | Mes inscriptions |
| DELETE | `/api/v1/enrollments/courses/{id}` | Se désinscrire |

### Candidatures Instructeur

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/api/v1/instructor-applications` | Soumettre une candidature |
| GET | `/api/v1/instructor-applications/my-application` | Ma candidature |
| GET | `/api/v1/admin/instructor-applications` | Toutes les candidatures (Admin) |
| PUT | `/api/v1/admin/instructor-applications/{id}/review` | Valider/Rejeter |

## 🧪 Tests

```bash
# Lancer tous les tests
mvn test
```

378 tests couvrant les couches service (unit) et controller (integration).

## 🐳 Docker

```bash
# Build
docker build -t sencours-api .

# Run
docker run -p 8080:8080 --env-file .env sencours-api
```

## 🌐 Déploiement

L'API est déployée sur **Render** : `https://sencours-api.onrender.com`

## 👤 Auteur

**Mohamed Makan KOUYATE**
- GitHub: [@Mohmk10](https://github.com/Mohmk10)
- Formation: Sonatel Academy — Orange Digital Center

## 📄 Licence

Ce projet est développé dans un cadre académique pour la Sonatel Academy.
