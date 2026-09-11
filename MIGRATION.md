# Migration — Bibliothèque vers Spring Boot 3.2 / Java 17 / PostgreSQL 16 / Angular 17

Documentation complète de la migration du projet de gestion de bibliothèque.

| Composant      | Avant                    | Après                             |
|----------------|--------------------------|-----------------------------------|
| Spring Boot    | 2.4.5                    | 3.2.0                             |
| Java           | 1.8                      | 17                                |
| Base de données| MySQL                    | PostgreSQL 16                     |
| ORM            | Hibernate 5 (javax)      | Hibernate 6 (jakarta)             |
| Spring Security| WebSecurityConfigurerAdapter | SecurityFilterChain (6.1)     |
| JWT (JJWT)     | 0.9.1 (`Jwts.parser()`)  | 0.12.3 (`Jwts.parser().verifyWith()`) |
| Lombok         | géré par Spring Boot     | 1.18.30 (explicite)               |
| MapStruct      | —                        | 1.5.5.Final                       |
| Angular        | 14                       | 17                                |
| TypeScript     | 4.7                      | 5.2                               |
| Orchestration  | —                        | Docker Compose                    |

---

## 1. Backend — `pom.xml`

- Parent Spring Boot `3.2.0`, `<java.version>17</java.version>`.
- Driver MySQL → `org.postgresql:postgresql` (version `42.7.1` — corrige le bug de timezone de 42.6.0).
- JJWT 0.12.3 découpé en 3 artefacts : `jjwt-api` (compile), `jjwt-impl` et `jjwt-jackson` (runtime).
- Ajouts : `spring-boot-starter-validation`, `spring-boot-starter-actuator` (healthcheck),
  `com.vladmihalcea:hibernate-types-60:2.21.1` (types JSON Hibernate 6), Testcontainers
  `junit-jupiter` + `postgresql` (`1.21.3`), MapStruct `1.5.5.Final`.
- Lombok `1.18.30` + MapStruct déclarés explicitement dans `maven-compiler-plugin`
  (`annotationProcessorPaths`) — indispensable sous Java 17.
- **Problème résolu — API Docker** : Testcontainers force par défaut l'API Docker `1.32`,
  trop ancienne pour Docker ≥ 25 (minimum 1.40). Le plugin Surefire propage la propriété
  `api.version` (défaut `1.44`, surchargeable via `-Ddocker.api.version=1.43`).

## 2. Entités JPA — `jakarta.persistence`

Toutes les entités utilisent désormais `jakarta.persistence.*` et des identifiants
`GenerationType.SEQUENCE` avec `@SequenceGenerator(allocationSize = 1)` (séquences PostgreSQL
créées par `init-scripts/01-init.sql`). Noms de tables en minuscules.

| Entité | Table | Séquence | Changements |
|--------|-------|----------|-------------|
| `Books`  | `books`  | `books_seq`  | colonnes explicites (`book_id`, `book_name`, …) |
| `Users`  | `users`  | `users_seq`  | `@JoinTable(name = "user_role")` en minuscules |
| `Role`   | `role`   | `role_seq`   | — |
| `Borrow` | `borrow` | `borrow_seq` | `java.util.Date` → `LocalDateTime`, `@PrePersist` (issue/due date si nulles), `@PreUpdate` (return date si nulle) |

`JsonDataSerializer` sérialise désormais `LocalDateTime` (`DateTimeFormatter`).

## 3. Sécurité — Spring Security 6

- `WebSecurityConfiguration` : suppression de `WebSecurityConfigurerAdapter`.
  Nouvelle API `SecurityFilterChain` + `@EnableMethodSecurity(prePostEnabled = true)`.
- `AuthenticationManager` obtenu via `AuthenticationConfiguration` (utilise automatiquement
  le `UserDetailsService` = `JwtService` et le `PasswordEncoder` BCrypt).
- **Problème résolu — référence circulaire** (prohibée par défaut en Spring Boot 3) :
  les filtres sont injectés en **paramètres de méthode** de `securityFilterChain(...)` et
  l'`AuthenticationManager` est injecté avec **`@Lazy`** dans `JwtService`.
- `JwtRequestFilter` et `JwtAuthenticationEntryPoint` : imports `jakarta.servlet`.
- Matchers inchangés : `/authenticate`, `/borrow/**`, `/admin/books/` en accès libre ;
  le reste authentifié. CORS conservé (`CorsConfiguration` + `http.cors()`).

## 4. JWT — JJWT 0.12.3

- `JwtUtil` : nouvelle API —
  `Keys.hmacShaKeyFor(secret)` + `Jwts.builder().subject(...).issuedAt(...).expiration(...).signWith(key, Jwts.SIG.HS256)`
  et `Jwts.parser().verifyWith(key).build().parseSignedClaims(token)`.
- **Problème résolu — clé trop courte** : la clé `learn_programming_yourself` (27 octets)
  est rejetée par `Keys.hmacShaKeyFor` (minimum 32 octets). Nouvelle clé
  `learn_programming_yourself_spring_boot_3_jwt_secret_key` (à externaliser en variable
  d'environnement en production).
- `JwtService` : `loadUserByUsername` lève `UsernameNotFoundException` proprement
  (`orElseThrow`) au lieu de `NoSuchElementException`.

## 5. Configuration

- `application.properties` : PostgreSQL local (`jdbc:postgresql://localhost:5432/bibliotheque`,
  `postgres/postgres`), dialecte `org.hibernate.dialect.PostgreSQLDialect`, `ddl-auto=update`
  pour le développement.
- `application-docker.properties` (profil `docker`, activé par la variable d'environnement
  `SPRING_PROFILES_ACTIVE=docker`) : URL `jdbc:postgresql://postgres:5432/bibliotheque`,
  **`ddl-auto=none`** car le schéma est créé par `init-scripts/01-init.sql`, exposition
  Actuator `health,info`.

## 6. Conteneurs

- `bibliotheque-backend/Dockerfile` : multi-stage Maven 3.9 + Eclipse Temurin 17 →
  exécution `eclipse-temurin:17-jre-alpine`, JAR dans `/app`, port 8080, profil `docker`.
- `bibliotheque-frontend/Dockerfile` : multi-stage Node 20 (`npm ci` + `ng build --configuration production`)
  → Nginx (`nginx.conf` : fallback SPA, gzip, cache statique). Port exposé 80 → 4200.
- `docker-compose.yml` (racine) : services `postgres` (healthcheck `pg_isready`),
  `backend` (healthcheck Actuator via `wget`), `frontend`, `adminer` (port 8081),
  volume `postgres_data`, réseau `bibliotheque-network`.
- Le frontend appelle l'API via `http://localhost:8080` (port publié sur l'hôte) avec CORS
  autorisé pour `http://localhost:4200` — les services Angular n'ont pas été modifiés.

## 7. Initialisation — `init-scripts/01-init.sql`

- Séquences `role_seq`, `users_seq`, `books_seq`, `borrow_seq` (démarrage à 100 pour éviter
  tout conflit avec les données initiales).
- Tables `role`, `users`, `user_role`, `books`, `borrow` avec clés étrangères et index.
- Rôles `Admin` (1) et `User` (2).
- Compte **admin / admin123** — hash BCrypt `$2b$10$RN5ij7XXjDpRBALhITW.2uzYGontX4U9c9ZRH5i3e.5l6RvkjZ696`
  (vérifié par le test `adminPasswordHashMatchesAdmin123`).
- 6 livres de test.

## 8. Frontend — Angular 17

- `package.json` : `@angular/*@^17.0.0`, `rxjs ~7.8.0`, `zone.js ~0.14.0`,
  `typescript ~5.2.2`, CLI 17, karma 6.4 / jasmine 5.1.
- `angular.json` **non modifié** (le build 17 fonctionne tel quel ; seule un avertissement
  budget `initial` de 630 kB > 500 kB subsiste, sans blocage).
- `package-lock.json` régénéré (installation propre `npm ci` dans Docker).

## 9. Lancement

```bash
./start.sh
# ou manuellement :
docker compose up -d --build
```

| Service   | URL                            | Identifiants        |
|-----------|--------------------------------|---------------------|
| Frontend  | http://localhost:4200          | admin / admin123    |
| Backend   | http://localhost:8080          | —                   |
| Actuator  | http://localhost:8080/actuator/health | —            |
| Adminer   | http://localhost:8081          | postgres / postgres |
| PostgreSQL| localhost:5432                | postgres / postgres |

## 10. Vérifications

- `mvn clean compile` : **OK** (Java 17).
- `mvn test` : **2 tests OK** (context Spring + hash BCrypt) via Testcontainers PostgreSQL.
- `npm run build` : **OK** (Angular 17.3.12).
- API : `POST /authenticate` avec `admin/admin123` renvoie le JWT ; CRUD `/admin/books` protégé
  par `hasRole('Admin')`.

## 11. Problèmes rencontrés et résolus

| Problème | Cause | Solution |
|----------|-------|----------|
| `NoSuchFieldError: JCTree$JCImport` | Lombok ancien incompatible Java 17 | Lombok 1.18.30 |
| `client version 1.32 is too old` | Testcontainers force API 1.32 | propriété `api.version=1.44` via Surefire |
| Référence circulaire de beans | interdite en Spring Boot 3 | injection par paramètres + `@Lazy` |
| `Incompatible types` JJWT | API 0.12.x refondue | `Keys.hmacShaKeyFor` / `verifyWith` |
| Dialecte MySQL introuvable | migration BDD | `PostgreSQLDialect` |
| CORS | nouvelle architecture | `http.cors()` + `CorsConfiguration` conservée |

> **Note port 8081** : si le port 8081 est déjà occupé par un autre conteneur Adminer
> (ex. `gestion-stock-adminer`), le service `adminer` de ce projet ne pourra pas démarrer.
> Arrêtez le conteneur concurrent ou changez le mapping dans `docker-compose.yml`.
