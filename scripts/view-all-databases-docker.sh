#!/usr/bin/env bash

# Script to view contents of all PostgreSQL databases in the DE-Store project using Docker
# Author: Auto-generated
# Date: 2025-11-25

set -e

# Color codes for better readability
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Database configurations
DB_CONTAINERS=("destore-postgres-auth" "destore-postgres-pricing" "destore-postgres-inventory" "destore-postgres-finance" "destore-postgres-notification")
DB_NAMES=("auth_db" "pricing_db" "inventory_db" "finance_db" "notification_db")

DB_USER="destore"

# Function to print a separator
print_separator() {
    echo -e "${CYAN}========================================================================================================${NC}"
}

# Function to print a header
print_header() {
    echo -e "${GREEN}$1${NC}"
}

# Function to print database name
print_db_name() {
    echo -e "${YELLOW}### DATABASE: $1 (Container: $2) ###${NC}"
}

# Function to query and display table contents
query_database() {
    local container_name=$1
    local db_name=$2
    
    print_separator
    print_db_name "$db_name" "$container_name"
    print_separator
    
    # Check if container is running
    if ! docker ps --format '{{.Names}}' | grep -q "^${container_name}$"; then
        echo -e "${RED}❌ Container $container_name is not running${NC}"
        echo ""
        return 1
    fi
    
    # Get list of tables
    echo -e "${BLUE}📋 Tables in $db_name:${NC}"
    TABLES=$(docker exec "$container_name" psql -U "$DB_USER" -d "$db_name" -t -c "
        SELECT table_name 
        FROM information_schema.tables 
        WHERE table_schema = 'public' 
        AND table_type = 'BASE TABLE'
        ORDER BY table_name;
    " 2>/dev/null | xargs)
    
    if [ -z "$TABLES" ]; then
        echo -e "${YELLOW}⚠️  No tables found in $db_name${NC}"
        echo ""
        return 0
    fi
    
    echo "$TABLES"
    echo ""
    
    # Query each table
    for table in $TABLES; do
        echo -e "${CYAN}┌─── Table: $table ───${NC}"
        
        # Get row count
        ROW_COUNT=$(docker exec "$container_name" psql -U "$DB_USER" -d "$db_name" -t -c "SELECT COUNT(*) FROM $table;" 2>/dev/null | xargs)
        echo -e "${BLUE}│ Row count: $ROW_COUNT${NC}"
        
        if [ "$ROW_COUNT" -eq 0 ]; then
            echo -e "${YELLOW}│ (empty table)${NC}"
        else
            echo -e "${BLUE}│ Contents:${NC}"
            docker exec "$container_name" psql -U "$DB_USER" -d "$db_name" -c "SELECT * FROM $table LIMIT 100;" 2>/dev/null | sed 's/^/│ /'
        fi
        
        echo -e "${CYAN}└─────────────────────────────────${NC}"
        echo ""
    done
}

# Main execution
echo ""
print_separator
print_header "🗄️  DE-STORE DATABASE CONTENTS VIEWER"
print_separator
echo ""

# Iterate through all databases
for i in "${!DB_NAMES[@]}"; do
    container_name="${DB_CONTAINERS[$i]}"
    db_name="${DB_NAMES[$i]}"
    query_database "$container_name" "$db_name"
    echo ""
done

print_separator
print_header "✅ Database scan complete!"
print_separator
echo ""
