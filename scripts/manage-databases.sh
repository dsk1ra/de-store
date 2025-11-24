#!/bin/bash

# Database Management Script for DE-Store
# This script provides convenient commands to manage separate PostgreSQL containers

set -e

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Database configurations
declare -A DBS=(
    ["auth"]="destore-postgres-auth:auth_db:5433"
    ["pricing"]="destore-postgres-pricing:pricing_db:5434"
    ["inventory"]="destore-postgres-inventory:inventory_db:5435"
    ["finance"]="destore-postgres-finance:finance_db:5436"
    ["notification"]="destore-postgres-notification:notification_db:5437"
)

DB_USER="destore"
DB_PASS="destore123"

# Functions
print_header() {
    echo -e "${BLUE}================================${NC}"
    echo -e "${BLUE}  DE-Store Database Manager${NC}"
    echo -e "${BLUE}================================${NC}\n"
}

print_usage() {
    echo -e "${GREEN}Usage:${NC} $0 [command] [service]\n"
    echo "Commands:"
    echo "  status              - Show status of all database containers"
    echo "  connect [service]   - Connect to a database (psql shell)"
    echo "  logs [service]      - View logs for a database container"
    echo "  backup [service]    - Backup a database"
    echo "  restore [service]   - Restore a database from backup"
    echo "  list                - List all databases"
    echo "  clean               - Stop and remove all database containers and volumes"
    echo ""
    echo "Services: auth, pricing, inventory, finance, notification"
    echo ""
    echo "Examples:"
    echo "  $0 status"
    echo "  $0 connect auth"
    echo "  $0 backup finance"
    echo "  $0 logs inventory"
}

get_db_info() {
    local service=$1
    if [[ ! ${DBS[$service]+_} ]]; then
        echo -e "${RED}Error: Invalid service '$service'${NC}"
        echo "Valid services: ${!DBS[@]}"
        exit 1
    fi
    
    IFS=':' read -r container dbname port <<< "${DBS[$service]}"
    echo "$container:$dbname:$port"
}

status_all() {
    echo -e "${GREEN}Database Container Status:${NC}\n"
    for service in "${!DBS[@]}"; do
        IFS=':' read -r container dbname port <<< "${DBS[$service]}"
        
        if docker ps --format '{{.Names}}' | grep -q "^${container}$"; then
            health=$(docker inspect --format='{{.State.Health.Status}}' "$container" 2>/dev/null || echo "unknown")
            echo -e "  ${GREEN}✓${NC} ${service}: ${container} - ${dbname} (port ${port}) - Health: ${health}"
        else
            echo -e "  ${RED}✗${NC} ${service}: ${container} - Not running"
        fi
    done
    echo ""
}

connect_db() {
    local service=$1
    IFS=':' read -r container dbname port <<< "$(get_db_info "$service")"
    
    echo -e "${YELLOW}Connecting to ${service} database (${dbname})...${NC}\n"
    docker exec -it "$container" psql -U "$DB_USER" -d "$dbname"
}

view_logs() {
    local service=$1
    IFS=':' read -r container dbname port <<< "$(get_db_info "$service")"
    
    echo -e "${YELLOW}Viewing logs for ${service} database...${NC}\n"
    docker logs -f "$container"
}

backup_db() {
    local service=$1
    IFS=':' read -r container dbname port <<< "$(get_db_info "$service")"
    
    local backup_file="backup_${dbname}_$(date +%Y%m%d_%H%M%S).sql"
    
    echo -e "${YELLOW}Backing up ${service} database to ${backup_file}...${NC}"
    docker exec "$container" pg_dump -U "$DB_USER" "$dbname" > "$backup_file"
    echo -e "${GREEN}✓ Backup completed: ${backup_file}${NC}"
}

restore_db() {
    local service=$1
    IFS=':' read -r container dbname port <<< "$(get_db_info "$service")"
    
    echo -e "${YELLOW}Available backups for ${dbname}:${NC}"
    ls -1 backup_${dbname}_*.sql 2>/dev/null || { echo -e "${RED}No backups found${NC}"; exit 1; }
    
    echo -e "\n${YELLOW}Enter backup filename:${NC}"
    read -r backup_file
    
    if [[ ! -f "$backup_file" ]]; then
        echo -e "${RED}Error: Backup file not found${NC}"
        exit 1
    fi
    
    echo -e "${YELLOW}Restoring ${service} database from ${backup_file}...${NC}"
    docker exec -i "$container" psql -U "$DB_USER" "$dbname" < "$backup_file"
    echo -e "${GREEN}✓ Restore completed${NC}"
}

list_dbs() {
    echo -e "${GREEN}Available Databases:${NC}\n"
    for service in "${!DBS[@]}"; do
        IFS=':' read -r container dbname port <<< "${DBS[$service]}"
        echo -e "  ${BLUE}${service}${NC}"
        echo -e "    Container:  ${container}"
        echo -e "    Database:   ${dbname}"
        echo -e "    Host Port:  ${port}"
        echo -e "    Connection: jdbc:postgresql://localhost:${port}/${dbname}"
        echo ""
    done
}

clean_all() {
    echo -e "${RED}WARNING: This will stop and remove all database containers and volumes!${NC}"
    echo -e "${RED}All data will be lost unless you have backups.${NC}\n"
    echo -e "${YELLOW}Are you sure? (type 'yes' to confirm):${NC}"
    read -r confirm
    
    if [[ "$confirm" != "yes" ]]; then
        echo -e "${GREEN}Cancelled${NC}"
        exit 0
    fi
    
    echo -e "${YELLOW}Stopping and removing containers...${NC}"
    docker-compose down -v
    echo -e "${GREEN}✓ Cleanup completed${NC}"
}

# Main script logic
print_header

if [[ $# -eq 0 ]]; then
    print_usage
    exit 0
fi

command=$1

case $command in
    status)
        status_all
        ;;
    connect)
        if [[ $# -lt 2 ]]; then
            echo -e "${RED}Error: Service name required${NC}"
            print_usage
            exit 1
        fi
        connect_db "$2"
        ;;
    logs)
        if [[ $# -lt 2 ]]; then
            echo -e "${RED}Error: Service name required${NC}"
            print_usage
            exit 1
        fi
        view_logs "$2"
        ;;
    backup)
        if [[ $# -lt 2 ]]; then
            echo -e "${RED}Error: Service name required${NC}"
            print_usage
            exit 1
        fi
        backup_db "$2"
        ;;
    restore)
        if [[ $# -lt 2 ]]; then
            echo -e "${RED}Error: Service name required${NC}"
            print_usage
            exit 1
        fi
        restore_db "$2"
        ;;
    list)
        list_dbs
        ;;
    clean)
        clean_all
        ;;
    help|--help|-h)
        print_usage
        ;;
    *)
        echo -e "${RED}Error: Unknown command '$command'${NC}\n"
        print_usage
        exit 1
        ;;
esac
