![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-black?style=for-the-badge&logo=JSON%20web%20tokens)

# SenCours API

Backend REST API pour SenCours, la première plateforme d'apprentissage en ligne sénégalaise.

## 🎓 Contexte

Projet de mémoire de fin de formation à la **Sonatel Academy** (Orange Digital Center), développé dans le cadre du programme de formation aux métiers du numérique.

## ✨ Fonctionnalités

- **Authentification JWT** - Inscription, connexion, gestion des sessions
- **Gestion des rôles** - SUPER_ADMIN, ADMIN, INSTRUCTEUR, ETUDIANT
- **Catalogue de cours** - CRUD complet avec catégories
- **Gestion des sections et leçons** - Structure complète des cours
- **Système d'inscription** - Enrollment et suivi de progression
- **Candidatures instructeur** - Workflow de validation
- **Reviews et notes** - Système d'évaluation des cours
- **Pagination et recherche** - API optimisée
- **Reset de base de données** - Fonction SuperAdmin pour réinitialisation complète

## 🛠️ Stack Technique

| Technologie | Version | Description |
|-------------|---------|-------------|
| Java | 21 | Langage principal |
| Spring Boot | 3.4 | Framework backend |
| Spring Security | 6.x | Authentification & autorisation |
| Spring Data JPA | 3.x | Persistance des données |
| PostgreSQL | 16+ | Base de données |
| JWT | - | Tokens d'authentification |
| BCrypt | - | Hashage des mots de passe |
| Lombok | - | Réduction du boilerplate |
| Maven | 3.9+ | Gestion des dépendances |

## 📁 Structure du Projet

```
src/main/java/com/sencours/
├── config/          # Configuration (Security, CORS, JWT)
├── controller/      # REST Controllers
├── dto/             # Data Transfer Objects
├── entity/          # Entités JPA
├── enums/           # Énumérations (Role, Status)
├── exception/       # Gestion des exceptions
├── repository/      # Repositories JPA
├── security/        # JWT Filter, UserDetails
└── service/         # Logique métier
```

## 🚀 Installation

### Prérequis

- Java 21+
- Maven 3.9+
- PostgreSQL 16+ (ou compte Neon)

### Configuration

1. Cloner le repository

```bash
git clone https://github.com/Mohmk10/sencours-back.git
cd sencours-back
```

2. Configurer la base de données

Créer un fichier `src/main/resources/application.properties` :

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/sencours
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update

jwt.secret=your_jwt_secret_key_here
jwt.expiration=86400000
```

3. Lancer l'application

```bash
./mvnw spring-boot:run
```

L'API sera accessible sur `http://localhost:8080`

## 🔐 Création du SuperAdmin

Le SuperAdmin ne peut être créé que directement en base de données.

### Étape 1 : Générer le hash du mot de passe

```bash
curl -X POST http://localhost:8080/api/v1/utility/hash \
  -H "Content-Type: application/json" \
  -d '{"password": "VotreMotDePasse123!"}'
```

Réponse :

```json
{
  "password": "VotreMotDePasse123!",
  "hash": "$2a$10$xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
  "usage": "INSERT INTO users (..., password, ...) VALUES (..., '$2a$10$...', ...)"
}
```

### Étape 2 : Insérer le SuperAdmin en base

```sql
INSERT INTO users (first_name, last_name, email, password, role, is_active, created_at, updated_at)
VALUES (
  'Prénom',
  'Nom',
  'superadmin@sencours.sn',
  '$2a$10$xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx',  -- Le hash généré
  'SUPER_ADMIN',
  true,
  NOW(),
  NOW()
);
```

## 📚 Documentation API

### Endpoints Publics

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/api/v1/auth/register` | Inscription (rôle ETUDIANT) |
| POST | `/api/v1/auth/login` | Connexion |
| GET | `/api/v1/courses` | Liste des cours publiés |
| GET | `/api/v1/courses/{id}` | Détail d'un cours |
| GET | `/api/v1/categories` | Liste des catégories |
| POST | `/api/v1/utility/hash` | Générer un hash BCrypt |
| POST | `/api/v1/utility/verify` | Vérifier un hash BCrypt |

### Endpoints Authentifiés

| Méthode | Endpoint | Rôle requis | Description |
|---------|----------|-------------|-------------|
| GET | `/api/v1/auth/me` | Tous | Profil utilisateur |
| POST | `/api/v1/courses` | INSTRUCTEUR+ | Créer un cours |
| PUT | `/api/v1/courses/{id}` | INSTRUCTEUR+ | Modifier un cours |
| DELETE | `/api/v1/courses/{id}` | INSTRUCTEUR+ | Supprimer un cours |
| POST | `/api/v1/courses/{id}/sections` | INSTRUCTEUR+ | Ajouter une section |
| POST | `/api/v1/sections/{id}/lessons` | INSTRUCTEUR+ | Ajouter une leçon |
| POST | `/api/v1/enrollments/courses/{id}` | ETUDIANT+ | S'inscrire à un cours |

### Endpoints Admin

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/api/v1/admin/users` | Liste des utilisateurs |
| DELETE | `/api/v1/admin/users/{id}` | Supprimer un utilisateur |
| GET | `/api/v1/admin/applications` | Candidatures instructeur |
| PUT | `/api/v1/admin/applications/{id}` | Valider/Refuser candidature |

### Endpoints SuperAdmin

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/api/v1/super-admin/admins` | Créer un admin |
| POST | `/api/v1/super-admin/instructors` | Créer un instructeur |
| DELETE | `/api/v1/super-admin/admins/{id}` | Supprimer un admin |
| DELETE | `/api/v1/super-admin/reset-database` | Réinitialiser la BD |

## 🔄 Hiérarchie des Rôles

```
SUPER_ADMIN
    │
    ├── Peut créer/supprimer des ADMIN
    ├── Peut créer des INSTRUCTEUR directement
    ├── Peut réinitialiser la base de données
    └── Accès total à toutes les fonctionnalités
         │
         ▼
      ADMIN
         │
         ├── Peut valider/refuser les candidatures instructeur
         ├── Peut gérer les catégories
         ├── Peut gérer les utilisateurs (sauf SUPER_ADMIN)
         └── Peut modérer le contenu
              │
              ▼
         INSTRUCTEUR
              │
              ├── Peut créer/modifier/supprimer ses cours
              ├── Peut ajouter des sections et leçons
              └── Peut voir les statistiques de ses cours
                   │
                   ▼
              ETUDIANT
                   │
                   ├── Peut s'inscrire aux cours
                   ├── Peut suivre sa progression
                   ├── Peut laisser des reviews
                   └── Peut candidater pour devenir instructeur
```

## 🧪 Tests

```bash
# Lancer tous les tests
./mvnw test

# Lancer avec couverture
./mvnw test jacoco:report
```

## 🐳 Docker

```bash
# Build
docker build -t sencours-api .

# Run
docker run -p 8080:8080 --env-file .env sencours-api
```

## 🌐 Déploiement

L'API est déployée sur **Render**.

### Variables d'environnement requises

| Variable | Description |
|----------|-------------|
| `DATABASE_URL` | URL de connexion PostgreSQL |
| `JWT_SECRET` | Clé secrète pour les tokens JWT |
| `SPRING_PROFILES_ACTIVE` | `prod` pour la production |

## 👤 Auteur

**Mohamed Makan KOUYATE**
- GitHub: [@Mohmk10](https://github.com/Mohmk10)
- Formation: Sonatel Academy - Orange Digital Center

## 📄 Licence

Ce projet est développé dans un cadre académique pour la Sonatel Academy.
