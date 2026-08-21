# ============================================================
# Dockerfile — bibliotheque-frontend (Angular 17)
# Build multi-stage : Node 20 + Nginx
# ============================================================

# ---------- Étape 1 : build Angular ----------
FROM node:20-alpine AS build
WORKDIR /app

# Installation des dépendances (package-lock.json requis pour npm ci)
COPY package.json package-lock.json ./
RUN npm ci

# Copie des sources et compilation de production
COPY . .
RUN npm run build -- --configuration production

# ---------- Étape 2 : serveur Nginx ----------
FROM nginx:alpine
COPY --from=build /app/dist/bibliotheque-frontend /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Le port 4200 est mappé vers 80 dans docker-compose.yml
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
