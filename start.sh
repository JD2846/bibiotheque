#!/usr/bin/env bash
# ============================================================
# start.sh — Lancement de la bibliothèque (Docker Compose)
# Backend Spring Boot 3.2 / Frontend Angular 17 / PostgreSQL 16
# ============================================================
set -e

# ---------- Couleurs pour les messages ----------
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo -e "${CYAN}==============================================${NC}"
echo -e "${CYAN}   Bibliothèque — Démarrage Docker Compose   ${NC}"
echo -e "${CYAN}==============================================${NC}"

# ---------- 1. Vérification de Docker ----------
if ! command -v docker &> /dev/null; then
    echo -e "${RED}[ERREUR] Docker n'est pas installé.${NC}"
    echo -e "Installez Docker Desktop / Docker Engine puis relancez ce script."
    exit 1
fi

if ! docker info &> /dev/null; then
    echo -e "${RED}[ERREUR] Le démon Docker ne tourne pas.${NC}"
    echo -e "Démarrez Docker Desktop (ou systemctl start docker) puis relancez ce script."
    exit 1
fi
echo -e "${GREEN}[OK]${NC} Docker est opérationnel ($(docker --version | awk '{print $3}'))"

# ---------- 2. Vérification des ports ----------
for port in 8080 4200 5432 8081; do
    if ss -ltn 2>/dev/null | grep -q ":${port} "; then
        echo -e "${YELLOW}[AVERTISSEMENT]${NC} Le port ${port} est déjà utilisé par un autre processus."
        echo -e "  ${YELLOW}Le service correspondant risque de ne pas démarrer.${NC}"
    fi
done

# ---------- 3. Lancement des conteneurs ----------
echo -e "${CYAN}>> Construction et démarrage des conteneurs (docker compose up -d --build)...${NC}"
docker compose up -d --build

# ---------- 4. Attente du backend ----------
echo -e "${CYAN}>> Attente du backend sur http://localhost:8080 ...${NC}"
BACKEND_READY=0
for i in $(seq 1 60); do
    if curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
        BACKEND_READY=1
        break
    fi
    sleep 2
done

if [ "$BACKEND_READY" -eq 1 ]; then
    echo -e "${GREEN}[OK]${NC} Le backend est prêt."
else
    echo -e "${YELLOW}[ATTENTION]${NC} Le backend n'a pas répondu dans le délai imparti."
    echo -e "  Consultez les logs : ${CYAN}docker compose logs -f backend${NC}"
fi

# ---------- 5. Récapitulatif ----------
echo ""
echo -e "${CYAN}==============================================${NC}"
echo -e "${CYAN}   Application démarrée !                     ${NC}"
echo -e "${CYAN}==============================================${NC}"
echo -e "  ${GREEN}Frontend (Angular) :${NC}  http://localhost:4200"
echo -e "  ${GREEN}Backend  (API REST) :${NC}  http://localhost:8080"
echo -e "  ${GREEN}Actuator (santé)   :${NC}  http://localhost:8080/actuator/health"
echo -e "  ${GREEN}Adminer (base)     :${NC}  http://localhost:8081"
echo ""
echo -e "  Identifiants PostgreSQL :"
echo -e "    - Hôte     : localhost:5432"
echo -e "    - Base     : bibliotheque"
echo -e "    - Utilisateur : postgres"
echo -e "    - Mot de passe  : postgres"
echo ""
echo -e "  ${YELLOW}Compte administrateur :${NC}"
echo -e "    - Utilisateur : admin"
echo -e "    - Mot de passe  : admin123"
echo ""
echo -e "${CYAN}----------------------------------------------${NC}"
echo -e "  Commandes utiles :"
echo -e "  ${CYAN}docker compose ps${NC}                  # état des conteneurs"
echo -e "  ${CYAN}docker compose logs -f backend${NC}     # logs backend"
echo -e "  ${CYAN}docker compose logs -f frontend${NC}    # logs frontend"
echo -e "  ${CYAN}docker compose stop${NC}                # arrêter les services"
echo -e "  ${CYAN}docker compose down${NC}                # arrêter + supprimer les conteneurs"
echo -e "  ${CYAN}docker compose down -v${NC}             # + supprimer les volumes (reset complet)"
echo -e "  ${CYAN}docker compose exec postgres psql -U postgres -d bibliotheque${NC}"
echo -e "                            # shell SQL dans PostgreSQL"
echo -e "${CYAN}==============================================${NC}"
