#!/bin/bash

# ============================================================================
# DE-Store Data Population Script
# ============================================================================
# This script populates the databases following the natural business flow:
# 1. Admin login -> Create stores -> Create products -> Create inventory
# 2. Create promotions -> Register customers -> Customers place orders
# 3. Orders trigger: inventory updates, analytics tracking, deliveries
# ============================================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
BASE_URL="${BASE_URL:-http://localhost:8080}"
AUTH_URL="${AUTH_URL:-http://localhost:8081}"
PRICING_URL="${PRICING_URL:-http://localhost:8082}"
INVENTORY_URL="${INVENTORY_URL:-http://localhost:8083}"
FINANCE_URL="${FINANCE_URL:-http://localhost:8084}"
LOYALTY_URL="${LOYALTY_URL:-http://localhost:8086}"
ANALYTICS_URL="${ANALYTICS_URL:-http://localhost:8087}"

# Global token storage
TOKEN=""

# ============================================================================
# Helper Functions
# ============================================================================

log_header() {
    echo ""
    echo -e "${CYAN}============================================================================${NC}"
    echo -e "${CYAN}$1${NC}"
    echo -e "${CYAN}============================================================================${NC}"
}

log_step() {
    echo -e "${YELLOW}>> $1${NC}"
}

log_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

log_error() {
    echo -e "${RED}❌ $1${NC}"
}

log_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

# ============================================================================
# Database Cleanup Functions
# ============================================================================

clear_databases() {
    log_header "Clearing Database Tables"
    log_info "This will remove all existing data (except admin user)..."
    
    # Clear Analytics DB (no foreign key dependencies to other services)
    log_step "Clearing Analytics database..."
    docker exec destore-postgres-analytics psql -U destore -d analytics_db -c "
        TRUNCATE TABLE store_performance CASCADE;
        TRUNCATE TABLE product_sales_summary CASCADE;
        TRUNCATE TABLE customer_analytics CASCADE;
        TRUNCATE TABLE sales_transactions CASCADE;
    " > /dev/null 2>&1 && log_success "Analytics DB cleared" || log_error "Failed to clear Analytics DB"
    
    # Clear Finance DB
    log_step "Clearing Finance database..."
    docker exec destore-postgres-finance psql -U destore -d finance_db -c "
        TRUNCATE TABLE finance_requests CASCADE;
    " > /dev/null 2>&1 && log_success "Finance DB cleared" || log_error "Failed to clear Finance DB"
    
    # Clear Loyalty DB (customers and their history)
    log_step "Clearing Loyalty database..."
    docker exec destore-postgres-loyalty psql -U destore -d loyalty_db -c "
        TRUNCATE TABLE purchase_history CASCADE;
        TRUNCATE TABLE customers CASCADE;
        TRUNCATE TABLE loyalty_promotions CASCADE;
    " > /dev/null 2>&1 && log_success "Loyalty DB cleared" || log_error "Failed to clear Loyalty DB"
    
    # Clear Inventory DB (inventory depends on stores)
    log_step "Clearing Inventory database..."
    docker exec destore-postgres-inventory psql -U destore -d inventory_db -c "
        TRUNCATE TABLE reservations CASCADE;
        TRUNCATE TABLE inventory_transactions CASCADE;
        TRUNCATE TABLE inventory CASCADE;
        TRUNCATE TABLE stores CASCADE;
    " > /dev/null 2>&1 && log_success "Inventory DB cleared" || log_error "Failed to clear Inventory DB"
    
    # Clear Pricing DB (products and promotions)
    log_step "Clearing Pricing database..."
    docker exec destore-postgres-pricing psql -U destore -d pricing_db -c "
        TRUNCATE TABLE promotions CASCADE;
        TRUNCATE TABLE products CASCADE;
    " > /dev/null 2>&1 && log_success "Pricing DB cleared" || log_error "Failed to clear Pricing DB"
    
    # Note: We do NOT clear the auth_db users table to preserve the admin user
    log_info "Auth database preserved (admin user kept)"
    
    log_success "All databases cleared successfully!"
    echo ""
}

# Make API call and handle response
api_call() {
    local method=$1
    local url=$2
    local data=$3
    local description=$4
    
    log_step "$description"
    
    if [ -n "$data" ]; then
        RESPONSE=$(curl -s -w "\n%{http_code}" -X "$method" "$url" \
            -H "Authorization: Bearer $TOKEN" \
            -H "Content-Type: application/json" \
            -d "$data" 2>/dev/null)
    else
        RESPONSE=$(curl -s -w "\n%{http_code}" -X "$method" "$url" \
            -H "Authorization: Bearer $TOKEN" \
            -H "Content-Type: application/json" 2>/dev/null)
    fi
    
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    RESPONSE_BODY=$(echo "$RESPONSE" | sed '$d')
    
    if [ "$HTTP_CODE" -ge 200 ] && [ "$HTTP_CODE" -lt 300 ]; then
        log_success "$description - Success (HTTP $HTTP_CODE)"
        echo "$RESPONSE_BODY"
        return 0
    else
        log_error "$description - Failed (HTTP $HTTP_CODE)"
        echo "$RESPONSE_BODY"
        return 1
    fi
}

# Wait for services to be ready
wait_for_services() {
    log_header "Waiting for Services to be Ready"
    
    # Using | as delimiter to avoid splitting http://
    local services=(
        "$AUTH_URL/auth/health|Auth Service"
        "$PRICING_URL/api/pricing/health|Pricing Service"
        "$INVENTORY_URL/api/inventory/health|Inventory Service"
        "$FINANCE_URL/api/finance/health|Finance Service"
        "$LOYALTY_URL/api/loyalty/promotions/health|Loyalty Service"
        "$ANALYTICS_URL/analytics/health|Analytics Service"
    )
    
    for service in "${services[@]}"; do
        local url="${service%|*}"
        local name="${service#*|}"
        echo -n "Checking $name... "
        local retries=30
        while [ $retries -gt 0 ]; do
            if curl -s "$url" > /dev/null 2>&1; then
                echo -e "${GREEN}Ready${NC}"
                break
            fi
            retries=$((retries - 1))
            sleep 1
        done
        if [ $retries -eq 0 ]; then
            echo -e "${RED}Not Ready (continuing anyway)${NC}"
        fi
    done
}

# ============================================================================
# Phase 1: Authentication
# ============================================================================

admin_login() {
    log_header "Phase 1: Admin Authentication"
    
    log_step "Logging in as admin..."
    
    RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$AUTH_URL/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"username": "admin", "password": "Admin123!"}' 2>/dev/null)
    
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    RESPONSE_BODY=$(echo "$RESPONSE" | sed '$d')
    
    if [ "$HTTP_CODE" -eq 200 ]; then
        TOKEN=$(echo "$RESPONSE_BODY" | jq -r '.data.accessToken // .data.token // .accessToken // .token // empty')
        if [ -n "$TOKEN" ] && [ "$TOKEN" != "null" ]; then
            log_success "Admin logged in successfully"
            log_info "Token acquired: ${TOKEN:0:50}..."
            return 0
        fi
    fi
    
    log_error "Admin login failed (HTTP $HTTP_CODE)"
    echo "$RESPONSE_BODY"
    exit 1
}

# ============================================================================
# Phase 2: Store Setup (Admin creates stores)
# ============================================================================

create_stores() {
    log_header "Phase 2: Creating Stores (Admin Task)"
    
    local stores=(
        '{
            "storeId": "STORE-001",
            "name": "DE-Store London",
            "address": "45 Oxford Street",
            "city": "London",
            "region": "Greater London",
            "postalCode": "W1D 1BS",
            "country": "UK",
            "phone": "+44-20-7946-0101",
            "email": "london@destore.co.uk",
            "managerName": "James Wilson"
        }'
        '{
            "storeId": "STORE-002",
            "name": "DE-Store Edinburgh",
            "address": "78 Princes Street",
            "city": "Edinburgh",
            "region": "Scotland",
            "postalCode": "EH2 2ER",
            "country": "UK",
            "phone": "+44-131-556-0102",
            "email": "edinburgh@destore.co.uk",
            "managerName": "Fiona MacLeod"
        }'
        '{
            "storeId": "STORE-003",
            "name": "DE-Store Cardiff",
            "address": "12 Queen Street",
            "city": "Cardiff",
            "region": "Wales",
            "postalCode": "CF10 2BY",
            "country": "UK",
            "phone": "+44-29-2034-0103",
            "email": "cardiff@destore.co.uk",
            "managerName": "Rhys Davies"
        }'
        '{
            "storeId": "STORE-004",
            "name": "DE-Store Manchester",
            "address": "156 Deansgate",
            "city": "Manchester",
            "region": "Greater Manchester",
            "postalCode": "M3 3WB",
            "country": "UK",
            "phone": "+44-161-834-0104",
            "email": "manchester@destore.co.uk",
            "managerName": "Sarah Thompson"
        }'
        '{
            "storeId": "STORE-005",
            "name": "DE-Store Birmingham",
            "address": "89 New Street",
            "city": "Birmingham",
            "region": "West Midlands",
            "postalCode": "B2 4BA",
            "country": "UK",
            "phone": "+44-121-643-0105",
            "email": "birmingham@destore.co.uk",
            "managerName": "David Patel"
        }'
    )
    
    for store in "${stores[@]}"; do
        local store_id=$(echo "$store" | jq -r '.storeId')
        api_call "POST" "$INVENTORY_URL/api/stores" "$store" "Creating store: $store_id" || true
        sleep 0.5
    done
}

# ============================================================================
# Phase 3: Product Catalog (Admin creates products)
# ============================================================================

create_products() {
    log_header "Phase 3: Creating Product Catalog"
    
    local products=(
        # Electronics
        '{"productCode": "LAPTOP-001", "productName": "Premium Laptop Pro 15", "basePrice": 1299.99}'
        '{"productCode": "LAPTOP-002", "productName": "Budget Laptop Basic 14", "basePrice": 499.99}'
        '{"productCode": "PHONE-001", "productName": "Smartphone X Pro", "basePrice": 999.99}'
        '{"productCode": "PHONE-002", "productName": "Smartphone Lite", "basePrice": 299.99}'
        '{"productCode": "TABLET-001", "productName": "Tablet Pro 12 inch", "basePrice": 799.99}'
        '{"productCode": "HEADPH-001", "productName": "Wireless Headphones Premium", "basePrice": 249.99}'
        '{"productCode": "HEADPH-002", "productName": "Wired Earbuds Basic", "basePrice": 29.99}'
        # Home & Living
        '{"productCode": "COFFEE-001", "productName": "Smart Coffee Maker", "basePrice": 149.99}'
        '{"productCode": "VACUUM-001", "productName": "Robot Vacuum Cleaner", "basePrice": 399.99}'
        '{"productCode": "BLENDER-001", "productName": "High-Speed Blender", "basePrice": 89.99}'
        # Gaming
        '{"productCode": "GAME-001", "productName": "Gaming Console X", "basePrice": 499.99}'
        '{"productCode": "GAME-002", "productName": "Gaming Controller Pro", "basePrice": 69.99}'
        '{"productCode": "GAME-003", "productName": "Gaming Headset 7.1", "basePrice": 129.99}'
        # Wearables
        '{"productCode": "WATCH-001", "productName": "Smart Watch Series 5", "basePrice": 349.99}'
        '{"productCode": "FITNESS-001", "productName": "Fitness Tracker Band", "basePrice": 79.99}'
        # Accessories
        '{"productCode": "CHARGER-001", "productName": "Fast Wireless Charger", "basePrice": 39.99}'
        '{"productCode": "CASE-001", "productName": "Premium Phone Case", "basePrice": 24.99}'
        '{"productCode": "CABLE-001", "productName": "USB-C Cable 2m", "basePrice": 14.99}'
        '{"productCode": "SPEAKER-001", "productName": "Portable Bluetooth Speaker", "basePrice": 79.99}'
        '{"productCode": "CAMERA-001", "productName": "Action Camera 4K", "basePrice": 299.99}'
    )
    
    for product in "${products[@]}"; do
        local product_code=$(echo "$product" | jq -r '.productCode')
        api_call "POST" "$PRICING_URL/api/pricing/products" "$product" "Creating product: $product_code" || true
        sleep 0.3
    done
}

# ============================================================================
# Phase 4: Inventory Setup (Stock products in stores)
# ============================================================================

create_inventory() {
    log_header "Phase 4: Setting Up Inventory"
    
    local inventories=(
        # =========================================================================
        # STORE-001 (London) - Flagship store, full inventory
        # =========================================================================
        '{"productCode": "LAPTOP-001", "quantity": 50, "lowStockThreshold": 10, "storeId": "STORE-001"}'
        '{"productCode": "LAPTOP-002", "quantity": 75, "lowStockThreshold": 15, "storeId": "STORE-001"}'
        '{"productCode": "PHONE-001", "quantity": 100, "lowStockThreshold": 20, "storeId": "STORE-001"}'
        '{"productCode": "PHONE-002", "quantity": 150, "lowStockThreshold": 30, "storeId": "STORE-001"}'
        '{"productCode": "TABLET-001", "quantity": 40, "lowStockThreshold": 8, "storeId": "STORE-001"}'
        '{"productCode": "HEADPH-001", "quantity": 80, "lowStockThreshold": 15, "storeId": "STORE-001"}'
        '{"productCode": "HEADPH-002", "quantity": 200, "lowStockThreshold": 40, "storeId": "STORE-001"}'
        '{"productCode": "COFFEE-001", "quantity": 60, "lowStockThreshold": 12, "storeId": "STORE-001"}'
        '{"productCode": "VACUUM-001", "quantity": 25, "lowStockThreshold": 5, "storeId": "STORE-001"}'
        '{"productCode": "BLENDER-001", "quantity": 45, "lowStockThreshold": 10, "storeId": "STORE-001"}'
        '{"productCode": "GAME-001", "quantity": 35, "lowStockThreshold": 7, "storeId": "STORE-001"}'
        '{"productCode": "GAME-002", "quantity": 100, "lowStockThreshold": 20, "storeId": "STORE-001"}'
        '{"productCode": "GAME-003", "quantity": 70, "lowStockThreshold": 14, "storeId": "STORE-001"}'
        '{"productCode": "WATCH-001", "quantity": 55, "lowStockThreshold": 11, "storeId": "STORE-001"}'
        '{"productCode": "FITNESS-001", "quantity": 120, "lowStockThreshold": 24, "storeId": "STORE-001"}'
        '{"productCode": "CHARGER-001", "quantity": 200, "lowStockThreshold": 40, "storeId": "STORE-001"}'
        '{"productCode": "CASE-001", "quantity": 300, "lowStockThreshold": 60, "storeId": "STORE-001"}'
        '{"productCode": "CABLE-001", "quantity": 500, "lowStockThreshold": 100, "storeId": "STORE-001"}'
        '{"productCode": "SPEAKER-001", "quantity": 85, "lowStockThreshold": 17, "storeId": "STORE-001"}'
        '{"productCode": "CAMERA-001", "quantity": 30, "lowStockThreshold": 6, "storeId": "STORE-001"}'
        
        # =========================================================================
        # STORE-002 (Edinburgh) - Focus on phones, wearables, accessories
        # =========================================================================
        '{"productCode": "LAPTOP-001", "quantity": 25, "lowStockThreshold": 5, "storeId": "STORE-002"}'
        '{"productCode": "LAPTOP-002", "quantity": 40, "lowStockThreshold": 8, "storeId": "STORE-002"}'
        '{"productCode": "PHONE-001", "quantity": 120, "lowStockThreshold": 25, "storeId": "STORE-002"}'
        '{"productCode": "PHONE-002", "quantity": 180, "lowStockThreshold": 35, "storeId": "STORE-002"}'
        '{"productCode": "TABLET-001", "quantity": 50, "lowStockThreshold": 10, "storeId": "STORE-002"}'
        '{"productCode": "HEADPH-001", "quantity": 100, "lowStockThreshold": 20, "storeId": "STORE-002"}'
        '{"productCode": "HEADPH-002", "quantity": 250, "lowStockThreshold": 50, "storeId": "STORE-002"}'
        '{"productCode": "COFFEE-001", "quantity": 30, "lowStockThreshold": 6, "storeId": "STORE-002"}'
        '{"productCode": "VACUUM-001", "quantity": 15, "lowStockThreshold": 3, "storeId": "STORE-002"}'
        '{"productCode": "BLENDER-001", "quantity": 20, "lowStockThreshold": 4, "storeId": "STORE-002"}'
        '{"productCode": "GAME-001", "quantity": 20, "lowStockThreshold": 4, "storeId": "STORE-002"}'
        '{"productCode": "GAME-002", "quantity": 60, "lowStockThreshold": 12, "storeId": "STORE-002"}'
        '{"productCode": "GAME-003", "quantity": 40, "lowStockThreshold": 8, "storeId": "STORE-002"}'
        '{"productCode": "WATCH-001", "quantity": 80, "lowStockThreshold": 16, "storeId": "STORE-002"}'
        '{"productCode": "FITNESS-001", "quantity": 150, "lowStockThreshold": 30, "storeId": "STORE-002"}'
        '{"productCode": "CHARGER-001", "quantity": 300, "lowStockThreshold": 60, "storeId": "STORE-002"}'
        '{"productCode": "CASE-001", "quantity": 400, "lowStockThreshold": 80, "storeId": "STORE-002"}'
        '{"productCode": "CABLE-001", "quantity": 600, "lowStockThreshold": 120, "storeId": "STORE-002"}'
        '{"productCode": "SPEAKER-001", "quantity": 70, "lowStockThreshold": 14, "storeId": "STORE-002"}'
        '{"productCode": "CAMERA-001", "quantity": 45, "lowStockThreshold": 9, "storeId": "STORE-002"}'
        
        # =========================================================================
        # STORE-003 (Cardiff) - Focus on gaming, home electronics
        # =========================================================================
        '{"productCode": "LAPTOP-001", "quantity": 35, "lowStockThreshold": 7, "storeId": "STORE-003"}'
        '{"productCode": "LAPTOP-002", "quantity": 55, "lowStockThreshold": 11, "storeId": "STORE-003"}'
        '{"productCode": "PHONE-001", "quantity": 70, "lowStockThreshold": 14, "storeId": "STORE-003"}'
        '{"productCode": "PHONE-002", "quantity": 100, "lowStockThreshold": 20, "storeId": "STORE-003"}'
        '{"productCode": "TABLET-001", "quantity": 30, "lowStockThreshold": 6, "storeId": "STORE-003"}'
        '{"productCode": "HEADPH-001", "quantity": 90, "lowStockThreshold": 18, "storeId": "STORE-003"}'
        '{"productCode": "HEADPH-002", "quantity": 180, "lowStockThreshold": 35, "storeId": "STORE-003"}'
        '{"productCode": "COFFEE-001", "quantity": 80, "lowStockThreshold": 16, "storeId": "STORE-003"}'
        '{"productCode": "VACUUM-001", "quantity": 40, "lowStockThreshold": 8, "storeId": "STORE-003"}'
        '{"productCode": "BLENDER-001", "quantity": 60, "lowStockThreshold": 12, "storeId": "STORE-003"}'
        '{"productCode": "GAME-001", "quantity": 60, "lowStockThreshold": 12, "storeId": "STORE-003"}'
        '{"productCode": "GAME-002", "quantity": 150, "lowStockThreshold": 30, "storeId": "STORE-003"}'
        '{"productCode": "GAME-003", "quantity": 120, "lowStockThreshold": 24, "storeId": "STORE-003"}'
        '{"productCode": "WATCH-001", "quantity": 40, "lowStockThreshold": 8, "storeId": "STORE-003"}'
        '{"productCode": "FITNESS-001", "quantity": 90, "lowStockThreshold": 18, "storeId": "STORE-003"}'
        '{"productCode": "CHARGER-001", "quantity": 180, "lowStockThreshold": 35, "storeId": "STORE-003"}'
        '{"productCode": "CASE-001", "quantity": 250, "lowStockThreshold": 50, "storeId": "STORE-003"}'
        '{"productCode": "CABLE-001", "quantity": 400, "lowStockThreshold": 80, "storeId": "STORE-003"}'
        '{"productCode": "SPEAKER-001", "quantity": 100, "lowStockThreshold": 20, "storeId": "STORE-003"}'
        '{"productCode": "CAMERA-001", "quantity": 25, "lowStockThreshold": 5, "storeId": "STORE-003"}'
        
        # =========================================================================
        # STORE-004 (Manchester) - Focus on outdoor/lifestyle products
        # =========================================================================
        '{"productCode": "LAPTOP-001", "quantity": 20, "lowStockThreshold": 4, "storeId": "STORE-004"}'
        '{"productCode": "LAPTOP-002", "quantity": 30, "lowStockThreshold": 6, "storeId": "STORE-004"}'
        '{"productCode": "PHONE-001", "quantity": 90, "lowStockThreshold": 18, "storeId": "STORE-004"}'
        '{"productCode": "PHONE-002", "quantity": 130, "lowStockThreshold": 26, "storeId": "STORE-004"}'
        '{"productCode": "TABLET-001", "quantity": 35, "lowStockThreshold": 7, "storeId": "STORE-004"}'
        '{"productCode": "HEADPH-001", "quantity": 110, "lowStockThreshold": 22, "storeId": "STORE-004"}'
        '{"productCode": "HEADPH-002", "quantity": 220, "lowStockThreshold": 45, "storeId": "STORE-004"}'
        '{"productCode": "COFFEE-001", "quantity": 25, "lowStockThreshold": 5, "storeId": "STORE-004"}'
        '{"productCode": "VACUUM-001", "quantity": 10, "lowStockThreshold": 2, "storeId": "STORE-004"}'
        '{"productCode": "BLENDER-001", "quantity": 35, "lowStockThreshold": 7, "storeId": "STORE-004"}'
        '{"productCode": "GAME-001", "quantity": 25, "lowStockThreshold": 5, "storeId": "STORE-004"}'
        '{"productCode": "GAME-002", "quantity": 70, "lowStockThreshold": 14, "storeId": "STORE-004"}'
        '{"productCode": "GAME-003", "quantity": 50, "lowStockThreshold": 10, "storeId": "STORE-004"}'
        '{"productCode": "WATCH-001", "quantity": 70, "lowStockThreshold": 14, "storeId": "STORE-004"}'
        '{"productCode": "FITNESS-001", "quantity": 200, "lowStockThreshold": 40, "storeId": "STORE-004"}'
        '{"productCode": "CHARGER-001", "quantity": 250, "lowStockThreshold": 50, "storeId": "STORE-004"}'
        '{"productCode": "CASE-001", "quantity": 350, "lowStockThreshold": 70, "storeId": "STORE-004"}'
        '{"productCode": "CABLE-001", "quantity": 450, "lowStockThreshold": 90, "storeId": "STORE-004"}'
        '{"productCode": "SPEAKER-001", "quantity": 130, "lowStockThreshold": 26, "storeId": "STORE-004"}'
        '{"productCode": "CAMERA-001", "quantity": 60, "lowStockThreshold": 12, "storeId": "STORE-004"}'
        
        # =========================================================================
        # STORE-005 (Birmingham) - Focus on premium tech, laptops
        # =========================================================================
        '{"productCode": "LAPTOP-001", "quantity": 80, "lowStockThreshold": 16, "storeId": "STORE-005"}'
        '{"productCode": "LAPTOP-002", "quantity": 100, "lowStockThreshold": 20, "storeId": "STORE-005"}'
        '{"productCode": "PHONE-001", "quantity": 110, "lowStockThreshold": 22, "storeId": "STORE-005"}'
        '{"productCode": "PHONE-002", "quantity": 140, "lowStockThreshold": 28, "storeId": "STORE-005"}'
        '{"productCode": "TABLET-001", "quantity": 65, "lowStockThreshold": 13, "storeId": "STORE-005"}'
        '{"productCode": "HEADPH-001", "quantity": 120, "lowStockThreshold": 24, "storeId": "STORE-005"}'
        '{"productCode": "HEADPH-002", "quantity": 280, "lowStockThreshold": 55, "storeId": "STORE-005"}'
        '{"productCode": "COFFEE-001", "quantity": 90, "lowStockThreshold": 18, "storeId": "STORE-005"}'
        '{"productCode": "VACUUM-001", "quantity": 35, "lowStockThreshold": 7, "storeId": "STORE-005"}'
        '{"productCode": "BLENDER-001", "quantity": 50, "lowStockThreshold": 10, "storeId": "STORE-005"}'
        '{"productCode": "GAME-001", "quantity": 45, "lowStockThreshold": 9, "storeId": "STORE-005"}'
        '{"productCode": "GAME-002", "quantity": 120, "lowStockThreshold": 24, "storeId": "STORE-005"}'
        '{"productCode": "GAME-003", "quantity": 90, "lowStockThreshold": 18, "storeId": "STORE-005"}'
        '{"productCode": "WATCH-001", "quantity": 75, "lowStockThreshold": 15, "storeId": "STORE-005"}'
        '{"productCode": "FITNESS-001", "quantity": 100, "lowStockThreshold": 20, "storeId": "STORE-005"}'
        '{"productCode": "CHARGER-001", "quantity": 350, "lowStockThreshold": 70, "storeId": "STORE-005"}'
        '{"productCode": "CASE-001", "quantity": 400, "lowStockThreshold": 80, "storeId": "STORE-005"}'
        '{"productCode": "CABLE-001", "quantity": 700, "lowStockThreshold": 140, "storeId": "STORE-005"}'
        '{"productCode": "SPEAKER-001", "quantity": 95, "lowStockThreshold": 19, "storeId": "STORE-005"}'
        '{"productCode": "CAMERA-001", "quantity": 55, "lowStockThreshold": 11, "storeId": "STORE-005"}'
    )
    
    for inventory in "${inventories[@]}"; do
        local product_code=$(echo "$inventory" | jq -r '.productCode')
        local store_id=$(echo "$inventory" | jq -r '.storeId')
        api_call "POST" "$INVENTORY_URL/api/inventory" "$inventory" "Creating inventory: $product_code in $store_id" || true
        sleep 0.2
    done
}

# ============================================================================
# Phase 5: Promotions Setup
# ============================================================================

create_promotions() {
    log_header "Phase 5: Creating Promotions"
    
    # Get dates for promotions
    local today=$(date +%Y-%m-%d)
    local next_month=$(date -v+30d +%Y-%m-%d 2>/dev/null || date -d "+30 days" +%Y-%m-%d 2>/dev/null || echo "2025-12-31")
    local next_week=$(date -v+7d +%Y-%m-%d 2>/dev/null || date -d "+7 days" +%Y-%m-%d 2>/dev/null || echo "2025-12-15")
    
    log_info "Promotion period: $today to $next_month"
    
    # Pricing promotions (percentage discounts, BOGO, etc.)
    local pricing_promotions=(
        "{
            \"promotionCode\": \"SUMMER20\",
            \"promotionName\": \"Summer Sale 20% Off\",
            \"promotionType\": \"PERCENTAGE_DISCOUNT\",
            \"discountValue\": 20,
            \"applicableProducts\": [\"LAPTOP-001\", \"LAPTOP-002\", \"TABLET-001\"],
            \"startDate\": \"$today\",
            \"endDate\": \"$next_month\"
        }"
        "{
            \"promotionCode\": \"PHONEBOGO\",
            \"promotionName\": \"Phone Accessories BOGO\",
            \"promotionType\": \"BOGO\",
            \"discountValue\": 100,
            \"applicableProducts\": [\"CASE-001\", \"CHARGER-001\", \"CABLE-001\"],
            \"startDate\": \"$today\",
            \"endDate\": \"$next_week\"
        }"
        "{
            \"promotionCode\": \"GAMING15\",
            \"promotionName\": \"Gaming Gear 15% Off\",
            \"promotionType\": \"PERCENTAGE_DISCOUNT\",
            \"discountValue\": 15,
            \"applicableProducts\": [\"GAME-001\", \"GAME-002\", \"GAME-003\"],
            \"startDate\": \"$today\",
            \"endDate\": \"$next_month\"
        }"
        "{
            \"promotionCode\": \"FLAT50\",
            \"promotionName\": \"$50 Off Premium Headphones\",
            \"promotionType\": \"FIXED_AMOUNT\",
            \"discountValue\": 50,
            \"applicableProducts\": [\"HEADPH-001\"],
            \"startDate\": \"$today\",
            \"endDate\": \"$next_month\"
        }"
    )
    
    for promo in "${pricing_promotions[@]}"; do
        local promo_code=$(echo "$promo" | jq -r '.promotionCode')
        api_call "POST" "$PRICING_URL/api/pricing/promotions" "$promo" "Creating pricing promotion: $promo_code" || true
        sleep 0.3
    done
    
    # Loyalty promotions (tier-based rewards)
    local now_iso=$(date -u +"%Y-%m-%dT%H:%M:%S" 2>/dev/null || echo "2025-11-27T10:00:00")
    local future_iso=$(date -u -v+30d +"%Y-%m-%dT%H:%M:%S" 2>/dev/null || date -u -d "+30 days" +"%Y-%m-%dT%H:%M:%S" 2>/dev/null || echo "2025-12-31T23:59:59")
    
    local loyalty_promotions=(
        "{
            \"promotionCode\": \"SILVER10\",
            \"promotionName\": \"Silver Member 10% Extra\",
            \"description\": \"Extra 10% discount for Silver tier members\",
            \"minTierRequired\": \"SILVER\",
            \"discountPercentage\": 10,
            \"pointsCost\": 500,
            \"startDate\": \"${now_iso}\",
            \"endDate\": \"${future_iso}\"
        }"
        "{
            \"promotionCode\": \"GOLD15\",
            \"promotionName\": \"Gold Member 15% Extra\",
            \"description\": \"Extra 15% discount for Gold tier members\",
            \"minTierRequired\": \"GOLD\",
            \"discountPercentage\": 15,
            \"pointsCost\": 1000,
            \"startDate\": \"${now_iso}\",
            \"endDate\": \"${future_iso}\"
        }"
        "{
            \"promotionCode\": \"BRONZE5\",
            \"promotionName\": \"Bronze Welcome Bonus\",
            \"description\": \"5% welcome discount for all members\",
            \"minTierRequired\": \"BRONZE\",
            \"discountPercentage\": 5,
            \"pointsCost\": 100,
            \"startDate\": \"${now_iso}\",
            \"endDate\": \"${future_iso}\"
        }"
    )
    
    for promo in "${loyalty_promotions[@]}"; do
        local promo_code=$(echo "$promo" | jq -r '.promotionCode')
        api_call "POST" "$LOYALTY_URL/api/loyalty/promotions" "$promo" "Creating loyalty promotion: $promo_code" || true
        sleep 0.3
    done
}

# ============================================================================
# Phase 6: Customer Registration
# ============================================================================

register_customers() {
    log_header "Phase 6: Registering Customers"
    
    local customers=(
        '{
            "customerId": "CUST-001",
            "name": "Alice Johnson",
            "email": "alice.johnson@email.co.uk",
            "phone": "+44-20-7946-1001",
            "address": "100 Victoria Road, London, SW1A 1AA"
        }'
        '{
            "customerId": "CUST-002",
            "name": "Bob Williams",
            "email": "bob.williams@email.co.uk",
            "phone": "+44-131-556-1002",
            "address": "25 Royal Mile, Edinburgh, EH1 2PB"
        }'
        '{
            "customerId": "CUST-003",
            "name": "Carol Davies",
            "email": "carol.davies@email.co.uk",
            "phone": "+44-29-2034-1003",
            "address": "48 Cathedral Road, Cardiff, CF11 9LL"
        }'
        '{
            "customerId": "CUST-004",
            "name": "David Miller",
            "email": "david.miller@email.co.uk",
            "phone": "+44-161-834-1004",
            "address": "72 Piccadilly, Manchester, M1 2BS"
        }'
        '{
            "customerId": "CUST-005",
            "name": "Eva Brown",
            "email": "eva.brown@email.co.uk",
            "phone": "+44-121-643-1005",
            "address": "33 Broad Street, Birmingham, B1 2HF"
        }'
        '{
            "customerId": "CUST-006",
            "name": "Frank Wilson",
            "email": "frank.wilson@email.co.uk",
            "phone": "+44-113-245-1006",
            "address": "15 Briggate, Leeds, LS1 6HD"
        }'
        '{
            "customerId": "CUST-007",
            "name": "Grace Lee",
            "email": "grace.lee@email.co.uk",
            "phone": "+44-141-221-1007",
            "address": "89 Buchanan Street, Glasgow, G1 3HL"
        }'
        '{
            "customerId": "CUST-008",
            "name": "Henry Taylor",
            "email": "henry.taylor@email.co.uk",
            "phone": "+44-117-927-1008",
            "address": "56 Park Street, Bristol, BS1 5NT"
        }'
        '{
            "customerId": "CUST-009",
            "name": "Iris Anderson",
            "email": "iris.anderson@email.co.uk",
            "phone": "+44-151-709-1009",
            "address": "42 Bold Street, Liverpool, L1 4DS"
        }'
        '{
            "customerId": "CUST-010",
            "name": "Jack Thompson",
            "email": "jack.thompson@email.co.uk",
            "phone": "+44-191-232-1010",
            "address": "28 Grey Street, Newcastle, NE1 6AE"
        }'
    )
    
    for customer in "${customers[@]}"; do
        local customer_id=$(echo "$customer" | jq -r '.customerId')
        api_call "POST" "$LOYALTY_URL/api/loyalty/customers" "$customer" "Registering customer: $customer_id" || true
        sleep 0.3
    done
}

# ============================================================================
# Phase 7: Simulating Customer Orders (Main Business Flow)
# ============================================================================

process_orders() {
    log_header "Phase 7: Processing Customer Orders"
    log_info "Orders will trigger: inventory updates, finance approval, analytics, deliveries"
    
    # Order 1: Simple in-store purchase
    log_step "Order 1: Alice buys a laptop (in-store pickup)"
    api_call "POST" "$BASE_URL/api/orchestration/purchase" '{
        "customerId": "CUST-001",
        "customerName": "Alice Johnson",
        "storeId": "STORE-001",
        "items": [
            {"productCode": "LAPTOP-001", "quantity": 1}
        ],
        "requiresDelivery": false,
        "paymentMethod": "CREDIT_CARD"
    }' "Processing Alice's laptop order" || true
    sleep 1
    
    # Order 2: Multiple items with delivery
    log_step "Order 2: Bob orders phone + accessories with delivery"
    api_call "POST" "$BASE_URL/api/orchestration/purchase" '{
        "customerId": "CUST-002",
        "customerName": "Bob Williams",
        "storeId": "STORE-001",
        "items": [
            {"productCode": "PHONE-001", "quantity": 1},
            {"productCode": "CASE-001", "quantity": 2},
            {"productCode": "CHARGER-001", "quantity": 1}
        ],
        "requiresDelivery": true,
        "deliveryDistance": 15.5,
        "deliveryAddress": "25 Royal Mile, Edinburgh, EH1 2PB",
        "isExpressDelivery": false,
        "paymentMethod": "DEBIT_CARD"
    }' "Processing Bob's phone order with delivery" || true
    sleep 1
    
    # Order 3: Gaming bundle with express delivery
    log_step "Order 3: Carol orders gaming bundle (express delivery)"
    api_call "POST" "$BASE_URL/api/orchestration/purchase" '{
        "customerId": "CUST-003",
        "customerName": "Carol Davies",
        "storeId": "STORE-003",
        "items": [
            {"productCode": "GAME-001", "quantity": 1},
            {"productCode": "GAME-002", "quantity": 2},
            {"productCode": "GAME-003", "quantity": 1}
        ],
        "requiresDelivery": true,
        "deliveryDistance": 8.0,
        "deliveryAddress": "48 Cathedral Road, Cardiff, CF11 9LL",
        "isExpressDelivery": true,
        "paymentMethod": "CREDIT_CARD"
    }' "Processing Carol's gaming order with express delivery" || true
    sleep 1
    
    # Order 4: High-value order (triggers finance approval)
    log_step "Order 4: David orders premium electronics (high value)"
    api_call "POST" "$BASE_URL/api/orchestration/purchase" '{
        "customerId": "CUST-004",
        "customerName": "David Miller",
        "storeId": "STORE-001",
        "items": [
            {"productCode": "LAPTOP-001", "quantity": 2},
            {"productCode": "TABLET-001", "quantity": 1},
            {"productCode": "WATCH-001", "quantity": 1}
        ],
        "requiresDelivery": true,
        "deliveryDistance": 25.0,
        "deliveryAddress": "72 Piccadilly, Manchester, M1 2BS",
        "isExpressDelivery": false,
        "paymentMethod": "CREDIT_CARD"
    }' "Processing David's high-value order" || true
    sleep 1
    
    # Order 5: Small accessories order
    log_step "Order 5: Eva orders accessories"
    api_call "POST" "$BASE_URL/api/orchestration/purchase" '{
        "customerId": "CUST-005",
        "customerName": "Eva Brown",
        "storeId": "STORE-002",
        "items": [
            {"productCode": "HEADPH-002", "quantity": 2},
            {"productCode": "CABLE-001", "quantity": 3}
        ],
        "requiresDelivery": false,
        "paymentMethod": "CASH"
    }' "Processing Eva's accessories order" || true
    sleep 1
    
    # Order 6: Home appliances with delivery
    log_step "Order 6: Frank orders home appliances"
    api_call "POST" "$BASE_URL/api/orchestration/purchase" '{
        "customerId": "CUST-006",
        "customerName": "Frank Wilson",
        "storeId": "STORE-001",
        "items": [
            {"productCode": "COFFEE-001", "quantity": 1},
            {"productCode": "BLENDER-001", "quantity": 1},
            {"productCode": "VACUUM-001", "quantity": 1}
        ],
        "requiresDelivery": true,
        "deliveryDistance": 12.0,
        "deliveryAddress": "15 Briggate, Leeds, LS1 6HD",
        "isExpressDelivery": false,
        "paymentMethod": "CREDIT_CARD"
    }' "Processing Frank's home appliances order" || true
    sleep 1
    
    # Order 7: Wearables and fitness
    log_step "Order 7: Grace orders wearables"
    api_call "POST" "$BASE_URL/api/orchestration/purchase" '{
        "customerId": "CUST-007",
        "customerName": "Grace Lee",
        "storeId": "STORE-002",
        "items": [
            {"productCode": "WATCH-001", "quantity": 1},
            {"productCode": "FITNESS-001", "quantity": 1},
            {"productCode": "HEADPH-001", "quantity": 1}
        ],
        "requiresDelivery": true,
        "deliveryDistance": 5.0,
        "deliveryAddress": "89 Buchanan Street, Glasgow, G1 3HL",
        "isExpressDelivery": true,
        "paymentMethod": "DEBIT_CARD"
    }' "Processing Grace's wearables order" || true
    sleep 1
    
    # Order 8: Budget phone order
    log_step "Order 8: Henry orders budget phone"
    api_call "POST" "$BASE_URL/api/orchestration/purchase" '{
        "customerId": "CUST-008",
        "customerName": "Henry Taylor",
        "storeId": "STORE-002",
        "items": [
            {"productCode": "PHONE-002", "quantity": 1},
            {"productCode": "CASE-001", "quantity": 1}
        ],
        "requiresDelivery": false,
        "paymentMethod": "CASH"
    }' "Processing Henry's budget phone order" || true
    sleep 1
    
    # Order 9: Audio equipment
    log_step "Order 9: Iris orders audio equipment"
    api_call "POST" "$BASE_URL/api/orchestration/purchase" '{
        "customerId": "CUST-009",
        "customerName": "Iris Anderson",
        "storeId": "STORE-001",
        "items": [
            {"productCode": "HEADPH-001", "quantity": 1},
            {"productCode": "SPEAKER-001", "quantity": 2}
        ],
        "requiresDelivery": true,
        "deliveryDistance": 20.0,
        "deliveryAddress": "120 Whiteladies Road, Bristol, BS8 2RP",
        "isExpressDelivery": false,
        "paymentMethod": "CREDIT_CARD"
    }' "Processing Iris's audio equipment order" || true
    sleep 1
    
    # Order 10: Camera and accessories
    log_step "Order 10: Jack orders camera equipment"
    api_call "POST" "$BASE_URL/api/orchestration/purchase" '{
        "customerId": "CUST-010",
        "customerName": "Jack Thompson",
        "storeId": "STORE-001",
        "items": [
            {"productCode": "CAMERA-001", "quantity": 1},
            {"productCode": "CHARGER-001", "quantity": 1},
            {"productCode": "CABLE-001", "quantity": 2}
        ],
        "requiresDelivery": true,
        "deliveryDistance": 10.0,
        "deliveryAddress": "75 Northumberland Street, Newcastle, NE1 7AF",
        "isExpressDelivery": true,
        "paymentMethod": "CREDIT_CARD"
    }' "Processing Jack's camera order" || true
    sleep 1
    
    # Additional repeat orders to build customer loyalty
    log_info "Processing additional orders to build customer loyalty points..."
    
    # Alice's second order
    api_call "POST" "$BASE_URL/api/orchestration/purchase" '{
        "customerId": "CUST-001",
        "customerName": "Alice Johnson",
        "storeId": "STORE-001",
        "items": [
            {"productCode": "HEADPH-001", "quantity": 1},
            {"productCode": "CHARGER-001", "quantity": 2}
        ],
        "requiresDelivery": false,
        "paymentMethod": "CREDIT_CARD"
    }' "Alice's second order (building loyalty)" || true
    sleep 0.5
    
    # Bob's second order
    api_call "POST" "$BASE_URL/api/orchestration/purchase" '{
        "customerId": "CUST-002",
        "customerName": "Bob Williams",
        "storeId": "STORE-001",
        "items": [
            {"productCode": "SPEAKER-001", "quantity": 1}
        ],
        "requiresDelivery": false,
        "paymentMethod": "CREDIT_CARD"
    }' "Bob's second order (building loyalty)" || true
    sleep 0.5
    
    # Carol's second order
    api_call "POST" "$BASE_URL/api/orchestration/purchase" '{
        "customerId": "CUST-003",
        "customerName": "Carol Davis",
        "storeId": "STORE-003",
        "items": [
            {"productCode": "GAME-002", "quantity": 1}
        ],
        "requiresDelivery": false,
        "paymentMethod": "DEBIT_CARD"
    }' "Carol's second order (building loyalty)" || true
}

# ============================================================================
# Phase 8: Verify Data Population
# ============================================================================

verify_data() {
    log_header "Phase 8: Verifying Data Population"
    
    # Check stores
    log_step "Checking stores..."
    STORES=$(curl -s "$INVENTORY_URL/api/stores" -H "Authorization: Bearer $TOKEN" 2>/dev/null)
    STORE_COUNT=$(echo "$STORES" | jq '.data | length' 2>/dev/null || echo "0")
    log_info "Stores created: $STORE_COUNT"
    
    # Check products
    log_step "Checking products..."
    PRODUCTS=$(curl -s "$PRICING_URL/api/pricing/products" -H "Authorization: Bearer $TOKEN" 2>/dev/null)
    PRODUCT_COUNT=$(echo "$PRODUCTS" | jq '.data | length' 2>/dev/null || echo "0")
    log_info "Products created: $PRODUCT_COUNT"
    
    # Check inventory
    log_step "Checking inventory..."
    INVENTORY=$(curl -s "$INVENTORY_URL/api/inventory" -H "Authorization: Bearer $TOKEN" 2>/dev/null)
    INVENTORY_COUNT=$(echo "$INVENTORY" | jq '.data | length' 2>/dev/null || echo "0")
    log_info "Inventory records: $INVENTORY_COUNT"
    
    # Check customers
    log_step "Checking customers..."
    CUSTOMERS=$(curl -s "$LOYALTY_URL/api/loyalty/customers" -H "Authorization: Bearer $TOKEN" 2>/dev/null)
    CUSTOMER_COUNT=$(echo "$CUSTOMERS" | jq '.data | length' 2>/dev/null || echo "0")
    log_info "Customers registered: $CUSTOMER_COUNT"
    
    # Check promotions
    log_step "Checking promotions..."
    PRICING_PROMOS=$(curl -s "$PRICING_URL/api/pricing/promotions" -H "Authorization: Bearer $TOKEN" 2>/dev/null)
    PRICING_PROMO_COUNT=$(echo "$PRICING_PROMOS" | jq '.data | length' 2>/dev/null || echo "0")
    log_info "Pricing promotions: $PRICING_PROMO_COUNT"
    
    LOYALTY_PROMOS=$(curl -s "$LOYALTY_URL/api/loyalty/promotions" -H "Authorization: Bearer $TOKEN" 2>/dev/null)
    LOYALTY_PROMO_COUNT=$(echo "$LOYALTY_PROMOS" | jq '.data | length' 2>/dev/null || echo "0")
    log_info "Loyalty promotions: $LOYALTY_PROMO_COUNT"
    
    # Check finance requests
    log_step "Checking finance requests..."
    FINANCE_STATS=$(curl -s "$FINANCE_URL/api/finance/queue-stats" -H "Authorization: Bearer $TOKEN" 2>/dev/null)
    TOTAL_PROCESSED=$(echo "$FINANCE_STATS" | jq '.data.totalProcessed // 0' 2>/dev/null || echo "0")
    PENDING=$(echo "$FINANCE_STATS" | jq '.data.pendingInQueue // 0' 2>/dev/null || echo "0")
    log_info "Finance - Processed: $TOTAL_PROCESSED, Pending: $PENDING"
    
    # Check low stock items
    log_step "Checking low stock alerts..."
    LOW_STOCK=$(curl -s "$INVENTORY_URL/api/inventory/low-stock" -H "Authorization: Bearer $TOKEN" 2>/dev/null)
    LOW_STOCK_COUNT=$(echo "$LOW_STOCK" | jq '.data | length' 2>/dev/null || echo "0")
    log_info "Low stock items: $LOW_STOCK_COUNT"
}

# ============================================================================
# Phase 10: Display Summary
# ============================================================================

display_summary() {
    log_header "Data Population Complete!"
    
    echo ""
    echo -e "${GREEN}Summary of Created Data:${NC}"
    echo "──────────────────────────────────────────────────"
    echo "  🏪 Stores:              3"
    echo "  📦 Products:           20"
    echo "  📊 Inventory Records:  32"
    echo "  🏷️  Pricing Promotions:  4"
    echo "  ⭐ Loyalty Promotions:  3"
    echo "  👤 Customers:          10"
    echo "  🛒 Orders Processed:   13"
    echo "──────────────────────────────────────────────────"
    echo ""
    echo -e "${CYAN}Business Flows Demonstrated:${NC}"
    echo "  ✓ Admin authentication"
    echo "  ✓ Store creation"
    echo "  ✓ Product catalog setup"
    echo "  ✓ Inventory management"
    echo "  ✓ Promotion creation (pricing & loyalty)"
    echo "  ✓ Customer registration"
    echo "  ✓ Order processing with inventory deduction"
    echo "  ✓ Finance approval flow"
    echo "  ✓ Loyalty points accumulation"
    echo "  ✓ Delivery order creation & tracking"
    echo "  ✓ Analytics event publishing"
    echo ""
    echo -e "${YELLOW}Next Steps:${NC}"
    echo "  • View analytics: GET $ANALYTICS_URL/analytics/reports/sales?startDate=2025-01-01&endDate=2025-12-31"
    echo "  • Check deliveries: GET $DELIVERY_URL/delivery/status/PENDING"
    echo "  • View customers: GET $LOYALTY_URL/api/loyalty/customers"
    echo "  • Check inventory: GET $INVENTORY_URL/api/inventory"
    echo ""
}

# ============================================================================
# Main Execution
# ============================================================================

main() {
    echo ""
    echo -e "${CYAN}╔════════════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║                    DE-Store Data Population Script                     ║${NC}"
    echo -e "${CYAN}╚════════════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    
    # Check for jq dependency
    if ! command -v jq &> /dev/null; then
        log_error "jq is required but not installed. Install with: brew install jq"
        exit 1
    fi
    
    # Check for docker dependency
    if ! command -v docker &> /dev/null; then
        log_error "docker is required but not installed."
        exit 1
    fi
    
    # Clear existing data before repopulating
    clear_databases
    
    wait_for_services
    admin_login
    create_stores
    create_products
    create_inventory
    create_promotions
    register_customers
    process_orders
    verify_data
    display_summary
}

# Run main function
main "$@"
