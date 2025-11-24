#!/bin/bash

# Build script for DE-Store microservices
# This script builds all services and creates Docker images

echo "========================================"
echo "Building DE-Store Microservices"
echo "========================================"
echo ""

# Set colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to build a service
build_service() {
    local service=$1
    echo -e "${YELLOW}Building $service...${NC}"
    cd "$service" || exit
    if mvn clean package -DskipTests; then
        echo -e "${GREEN}✓ $service built successfully${NC}"
        cd ..
        return 0
    else
        echo -e "${RED}✗ Failed to build $service${NC}"
        cd ..
        return 1
    fi
}

# Navigate to project root
cd "$(dirname "$0")"

# Build parent POM
echo -e "${YELLOW}Building parent POM...${NC}"
if mvn clean install -DskipTests -N; then
    echo -e "${GREEN}✓ Parent POM installed${NC}"
else
    echo -e "${RED}✗ Failed to build parent POM${NC}"
    exit 1
fi

echo ""

# Build common module first (required by other services)
echo -e "${YELLOW}Building common module...${NC}"
if build_service "common"; then
    echo ""
else
    echo -e "${RED}Common module build failed. Cannot continue.${NC}"
    exit 1
fi

# List of services to build
services=(
    "eureka-server"
    "auth-service"
    "pricing-service"
    "inventory-service"
    "finance-service"
    "notification-service"
    "external-finance-service"
    "api-gateway"
)

# Track failures
failed_services=()

# Build all services
for service in "${services[@]}"; do
    if ! build_service "$service"; then
        failed_services+=("$service")
    fi
    echo ""
done

echo "========================================"
echo "Build Summary"
echo "========================================"

if [ ${#failed_services[@]} -eq 0 ]; then
    echo -e "${GREEN}✓ All services built successfully!${NC}"
    echo ""
    echo "Next steps:"
    echo "1. Start services: docker-compose up -d"
    echo "2. Check Eureka: http://localhost:8761"
    echo "3. View traces: http://localhost:9411"
    echo ""
    exit 0
else
    echo -e "${RED}✗ The following services failed to build:${NC}"
    for service in "${failed_services[@]}"; do
        echo "  - $service"
    done
    echo ""
    exit 1
fi
