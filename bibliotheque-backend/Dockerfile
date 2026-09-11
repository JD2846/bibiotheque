# ============================================================
# Dockerfile — bibliotheque-backend (Spring Boot 3.2 / Java 17)
# Build multi-stage : Maven 3.9 + Eclipse Temurin 17
# ============================================================

# ---------- Étape 1 : compilation ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copie du POM pour profiter du cache des dépendances Maven
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copie des sources et compilation (les tests sont exécutés en CI, pas dans l'image)
COPY src ./src
RUN mvn clean package -DskipTests

# ---------- Étape 2 : image d'exécution ----------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Fuseau horaire
ENV TZ=Europe/Paris

# Copie du JAR généré
COPY --from=build /app/target/*.jar app.jar

# Port exposé par l'API REST
EXPOSE 8080

# Le profil "docker" active application-docker.properties
ENV SPRING_PROFILES_ACTIVE=docker

ENTRYPOINT ["java", "-jar", "app.jar"]
