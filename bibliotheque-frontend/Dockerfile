# ============================================================
# Dockerfile — bibliotheque-frontend (Angular 22)
# Build multi-stage : Node 24 + Nginx
# ============================================================

# ---------- Étape 1 : build Angular ----------
FROM node:24-alpine AS build
WORKDIR /app

# Installation des dépendances (package-lock.json requis pour npm ci)
COPY package.json package-lock.json ./
RUN npm ci

# Copie des sources et compilation de production
COPY . .
RUN npm run build -- --configuration production

# ---------- Étape 2 : serveur Nginx ----------
# Depuis le passage au builder @angular/build:application (esbuild), la sortie
# du build browser est dans dist/bibliotheque-frontend/browser/ (et non plus
# directement dans dist/bibliotheque-frontend/).
FROM nginx:alpine
COPY --from=build /app/dist/bibliotheque-frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Le port 4200 est mappé vers 80 dans docker-compose.yml
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
