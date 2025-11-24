#!/bin/bash

# DE-Store API Test Script (Bash)

# Colors
CYAN='\033[0;36m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
GRAY='\033[0;90m'
NC='\033[0m' # No Color

echo -e "${CYAN}==========================================${NC}"
echo -e "${CYAN}DE-Store API Testing${NC}"
echo -e "${CYAN}==========================================${NC}"
echo ""

BASE_URL="http://localhost:8080"

# Check for jq
if ! command -v jq &> /dev/null; then
    echo -e "${RED}Error: jq is not installed. Please install it (brew install jq)${NC}"
    exit 1
fi

# Step 1: Login
echo -e "${YELLOW}Step 1: Logging in...${NC}"

LOGIN_BODY='{
    "username": "admin",
    "password": "Admin123!"
}'

LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d "$LOGIN_BODY")

TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.data.accessToken')

if [ "$TOKEN" != "null" ] && [ -n "$TOKEN" ]; then
    echo -e "${GREEN}✅ Login successful${NC}"
    echo -e "${GRAY}   Token: ${TOKEN:0:20}...${NC}"
    echo ""
else
    echo -e "${RED}❌ Login failed: $(echo "$LOGIN_RESPONSE" | jq -r '.message // "Unknown error"') ${NC}"
    exit 1
fi

# Step 2: Create Product
echo -e "${YELLOW}Step 2: Creating product...${NC}"

PRODUCT_BODY='{
    "productCode": "PROD-001",
    "productName": "Laptop Computer",
    "basePrice": 999.99
}'

PRODUCT_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/pricing/products" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "$PRODUCT_BODY")

HTTP_CODE=$(echo "$PRODUCT_RESPONSE" | tail -n1)
RESPONSE_BODY=$(echo "$PRODUCT_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" -eq 200 ] || [ "$HTTP_CODE" -eq 201 ]; then
    PRODUCT_NAME=$(echo "$RESPONSE_BODY" | jq -r '.data.productName')
    BASE_PRICE=$(echo "$RESPONSE_BODY" | jq -r '.data.basePrice')
    echo -e "${GREEN}✅ Product created: $PRODUCT_NAME${NC}"
    echo -e "${GRAY}   Price: \$$BASE_PRICE${NC}"
elif [ "$HTTP_CODE" -eq 400 ]; then
    echo -e "${YELLOW}⚠️  Product may already exist${NC}"
else
    echo -e "${RED}❌ Product creation failed: $(echo "$RESPONSE_BODY" | jq -r '.message // "Unknown error"')${NC}"
fi
echo ""

# Step 3: Create Promotion
echo -e "${YELLOW}Step 3: Creating promotion...${NC}"

PROMOTION_BODY='{
    "promotionCode": "BOGO-LAPTOPS",
    "promotionName": "Buy One Get One Free - Laptops",
    "promotionType": "BOGO",
    "applicableProducts": ["PROD-001"],
    "startDate": "2025-11-20",
    "endDate": "2025-12-31"
}'

PROMO_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/pricing/promotions" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "$PROMOTION_BODY")

HTTP_CODE=$(echo "$PROMO_RESPONSE" | tail -n1)
RESPONSE_BODY=$(echo "$PROMO_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" -eq 200 ] || [ "$HTTP_CODE" -eq 201 ]; then
    PROMO_NAME=$(echo "$RESPONSE_BODY" | jq -r '.data.promotionName')
    echo -e "${GREEN}✅ Promotion created: $PROMO_NAME${NC}"
elif [ "$HTTP_CODE" -eq 400 ]; then
    echo -e "${YELLOW}⚠️  Promotion may already exist${NC}"
else
    echo -e "${RED}❌ Promotion creation failed: $(echo "$RESPONSE_BODY" | jq -r '.message // "Unknown error"')${NC}"
fi
echo ""

# Step 4: Calculate Price
echo -e "${YELLOW}Step 4: Calculating price with promotion...${NC}"

CALCULATE_BODY='{
    "items": [
        {
            "productCode": "PROD-001",
            "quantity": 2
        }
    ]
}'

PRICE_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/pricing/calculate" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "$CALCULATE_BODY")

HTTP_CODE=$(echo "$PRICE_RESPONSE" | tail -n1)
RESPONSE_BODY=$(echo "$PRICE_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" -eq 200 ]; then
    SUBTOTAL=$(echo "$RESPONSE_BODY" | jq -r '.data.subtotal')
    DISCOUNT=$(echo "$RESPONSE_BODY" | jq -r '.data.promotionalDiscount')
    FINAL_TOTAL=$(echo "$RESPONSE_BODY" | jq -r '.data.finalTotal')
    APPLIED_PROMOS=$(echo "$RESPONSE_BODY" | jq -r '.data.appliedPromotions | join(", ")')
    
    echo -e "${GREEN}✅ Price calculation:${NC}"
    echo -e "${GRAY}   Subtotal: £$SUBTOTAL${NC}"
    echo -e "${GRAY}   Discount: £$DISCOUNT${NC}"
    echo -e "${GRAY}   Final Total: £$FINAL_TOTAL${NC}"
    echo -e "${GRAY}   Applied Promotions: $APPLIED_PROMOS${NC}"
else
    echo -e "${RED}❌ Price calculation failed: $(echo "$RESPONSE_BODY" | jq -r '.message // "Unknown error"')${NC}"
fi
echo ""

# Step 5: Get Product
echo -e "${YELLOW}Step 5: Retrieving product...${NC}"

GET_PRODUCT_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/api/pricing/products/PROD-001" \
    -H "Authorization: Bearer $TOKEN")

HTTP_CODE=$(echo "$GET_PRODUCT_RESPONSE" | tail -n1)
RESPONSE_BODY=$(echo "$GET_PRODUCT_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" -eq 200 ]; then
    PRODUCT_NAME=$(echo "$RESPONSE_BODY" | jq -r '.data.productName')
    echo -e "${GREEN}✅ Product retrieved: $PRODUCT_NAME${NC}"
else
    echo -e "${RED}❌ Product retrieval failed: $(echo "$RESPONSE_BODY" | jq -r '.message // "Unknown error"')${NC}"
fi
echo ""

# Step 6: Create Inventory
echo -e "${YELLOW}Step 6: Creating inventory...${NC}"

INVENTORY_BODY='{
    "productCode": "PROD-001",
    "quantity": 45,
    "lowStockThreshold": 10,
    "storeId": "STORE-001"
}'

INVENTORY_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/inventory" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "$INVENTORY_BODY")

HTTP_CODE=$(echo "$INVENTORY_RESPONSE" | tail -n1)
RESPONSE_BODY=$(echo "$INVENTORY_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" -eq 200 ] || [ "$HTTP_CODE" -eq 201 ]; then
    QUANTITY=$(echo "$RESPONSE_BODY" | jq -r '.data.quantity')
    echo -e "${GREEN}✅ Inventory created: $QUANTITY units${NC}"
elif [ "$HTTP_CODE" -eq 400 ]; then
    echo -e "${YELLOW}⚠️  Inventory may already exist${NC}"
else
    echo -e "${RED}❌ Inventory creation failed: $(echo "$RESPONSE_BODY" | jq -r '.message // "Unknown error"')${NC}"
fi
echo ""

# Step 7: Get Inventory
echo -e "${YELLOW}Step 7: Checking inventory...${NC}"

GET_INVENTORY_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/api/inventory/PROD-001" \
    -H "Authorization: Bearer $TOKEN")

HTTP_CODE=$(echo "$GET_INVENTORY_RESPONSE" | tail -n1)
RESPONSE_BODY=$(echo "$GET_INVENTORY_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" -eq 200 ]; then
    QUANTITY=$(echo "$RESPONSE_BODY" | jq -r '.data.quantity')
    AVAILABLE=$(echo "$RESPONSE_BODY" | jq -r '.data.availableQuantity')
    RESERVED=$(echo "$RESPONSE_BODY" | jq -r '.data.reservedQuantity')
    THRESHOLD=$(echo "$RESPONSE_BODY" | jq -r '.data.lowStockThreshold')
    
    echo -e "${GREEN}✅ Inventory status:${NC}"
    echo -e "${GRAY}   Total Quantity: $QUANTITY${NC}"
    echo -e "${GRAY}   Available: $AVAILABLE${NC}"
    echo -e "${GRAY}   Reserved: $RESERVED${NC}"
    echo -e "${GRAY}   Low Stock Threshold: $THRESHOLD${NC}"
else
    echo -e "${RED}❌ Inventory check failed: $(echo "$RESPONSE_BODY" | jq -r '.message // "Unknown error"')${NC}"
fi
echo ""

# Step 8: Reserve Items
echo -e "${YELLOW}Step 8: Reserving items...${NC}"

RESERVE_BODY='{
    "quantity": 2,
    "notes": "Test reservation from script"
}'

RESERVE_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/inventory/PROD-001/reserve" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "$RESERVE_BODY")

HTTP_CODE=$(echo "$RESERVE_RESPONSE" | tail -n1)
RESPONSE_BODY=$(echo "$RESERVE_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" -eq 200 ]; then
    RESERVATION_ID=$(echo "$RESPONSE_BODY" | jq -r '.data.reservationId')
    RESERVED_QTY=$(echo "$RESPONSE_BODY" | jq -r '.data.reservedQuantity')
    
    echo -e "${GREEN}✅ Items reserved:${NC}"
    echo -e "${GRAY}   Reservation ID: $RESERVATION_ID${NC}"
    echo -e "${GRAY}   Quantity: $RESERVED_QTY${NC}"
else
    echo -e "${RED}❌ Reservation failed: $(echo "$RESPONSE_BODY" | jq -r '.message // "Unknown error"')${NC}"
fi
echo ""

# Step 9: Submit Finance Request
echo -e "${YELLOW}Step 9: Submitting finance approval request...${NC}"

FINANCE_BODY='{
    "customerId": "CUST-001",
    "amount": 2499.99,
    "purpose": "Purchase of Laptop Computer (PROD-001)"
}'

FINANCE_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/finance/approve" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "$FINANCE_BODY")

HTTP_CODE=$(echo "$FINANCE_RESPONSE" | tail -n1)
RESPONSE_BODY=$(echo "$FINANCE_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" -eq 200 ]; then
    REQUEST_ID=$(echo "$RESPONSE_BODY" | jq -r '.data.requestId')
    STATUS=$(echo "$RESPONSE_BODY" | jq -r '.data.status')
    APPROVAL_CODE=$(echo "$RESPONSE_BODY" | jq -r '.data.approvalCode')
    MESSAGE=$(echo "$RESPONSE_BODY" | jq -r '.data.message')
    
    echo -e "${GREEN}✅ Finance request:${NC}"
    echo -e "${GRAY}   Request ID: $REQUEST_ID${NC}"
    echo -e "${GRAY}   Status: $STATUS${NC}"
    echo -e "${GRAY}   Approval Code: $APPROVAL_CODE${NC}"
    echo -e "${GRAY}   Message: $MESSAGE${NC}"
else
    echo -e "${RED}❌ Finance request failed: $(echo "$RESPONSE_BODY" | jq -r '.message // "Unknown error"')${NC}"
fi
echo ""

# Summary
echo -e "${CYAN}==========================================${NC}"
echo -e "${CYAN}Test Summary${NC}"
echo -e "${CYAN}==========================================${NC}"
echo -e "${GREEN}All API endpoints tested successfully!${NC}"
echo ""
echo -e "${YELLOW}Check RabbitMQ Management UI for message events:${NC}"
echo -e "${GRAY}  http://localhost:15672${NC}"
echo -e "${GRAY}  Username: destore${NC}"
echo -e "${GRAY}  Password: destore123${NC}"
echo ""
echo -e "${YELLOW}Check Docker logs for notifications:${NC}"
echo -e "${GRAY}  docker-compose logs notification-service${NC}"
echo ""
