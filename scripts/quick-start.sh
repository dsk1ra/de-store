#!/bin/bash

# Set JAVA_HOME to Java 17 if available via Homebrew
if [ -d "/opt/homebrew/opt/openjdk@17" ]; then
    export JAVA_HOME="/opt/homebrew/opt/openjdk@17"
    export PATH="$JAVA_HOME/bin:$PATH"
    echo "Using Java from $JAVA_HOME"
fi

# DE-Store Quick Start Script
# This script builds and starts the entire DE-Store system

echo "=========================================="
echo "DE-Store Quick Start"
echo "=========================================="
echo ""

# Check prerequisites
echo "Checking prerequisites..."

if ! command -v java &> /dev/null; then
    echo "❌ Java not found. Please install Java 17 or higher."
    exit 1
fi

if ! command -v mvn &> /dev/null; then
    echo "❌ Maven not found. Please install Maven 3.9 or higher."
    exit 1
fi

if ! command -v docker &> /dev/null; then
    echo "❌ Docker not found. Please install Docker."
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose not found. Please install Docker Compose."
    exit 1
fi

echo "✅ All prerequisites found"
echo ""

# Build all services
echo "=========================================="
echo "Building all services..."
echo "=========================================="
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "❌ Build failed. Please check the error messages above."
    exit 1
fi

echo "✅ Build successful"
echo ""

# Start Docker Compose
echo "=========================================="
echo "Starting Docker Compose..."
echo "=========================================="
docker-compose up -d

if [ $? -ne 0 ]; then
    echo "❌ Docker Compose failed to start. Please check the error messages above."
    exit 1
fi

echo "✅ Docker Compose started successfully"
echo ""

# Wait for services to be ready
echo "Waiting for services to be ready..."
sleep 30

# Check service health
echo ""
echo "=========================================="
echo "Checking service health..."
echo "=========================================="

services=(
    "http://localhost:8081/auth/health|Authentication Service"
    "http://localhost:8082/api/pricing/health|Pricing Service"
    "http://localhost:8083/api/inventory/health|Inventory Service"
    "http://localhost:8084/api/finance/health|Finance Service"
    "http://localhost:8086/api/loyalty/promotions/health|Loyalty Service"
    "http://localhost:8087/analytics/health|Analytics Service"
    "http://localhost:9000/api/finance-approval/health|Finance Approval Automation"
)

all_healthy=true

for service in "${services[@]}"; do
    IFS='|' read -r url name <<< "$service"
    
    if curl -s -f "$url" > /dev/null; then
        echo "✅ $name"
    else
        echo "❌ $name (not responding)"
        all_healthy=false
    fi
done

echo ""

if [ "$all_healthy" = true ]; then
    echo "=========================================="
    echo "All services are healthy!"
    echo "=========================================="
    echo ""
    echo "Service URLs:"
    echo "  - API Gateway: http://localhost:8080"
    echo "  - Auth Service: http://localhost:8081"
    echo "  - Pricing Service: http://localhost:8082"
    echo "  - Inventory Service: http://localhost:8083"
    echo "  - Finance Service: http://localhost:8084"
    echo "  - Loyalty Service: http://localhost:8086"
    echo "  - Analytics Service: http://localhost:8087"
    echo "  - External Finance Service: http://localhost:9000"
    echo "  - RabbitMQ Management: http://localhost:15672"
    echo ""
    echo "Default Test Users:"
    echo "  - Username: store.manager1, Password: Password123!"
    echo "  - Username: store.manager2, Password: Password123!"
    echo "  - Username: admin, Password: Admin123!"
    echo ""
    echo "Next Steps:"
    echo "  1. Test login: ./test-login.sh"
    echo "  2. View logs: docker-compose logs -f"
    echo "  3. Stop services: docker-compose down"
    echo ""
else
    echo "⚠️  Some services are not responding. Please check logs:"
    echo "  docker-compose logs -f"
fi
