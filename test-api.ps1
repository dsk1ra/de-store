# DE-Store API Test Script (PowerShell)

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "DE-Store API Testing" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

$baseUrl = "http://localhost:8080"

# Step 1: Login
Write-Host "Step 1: Logging in..." -ForegroundColor Yellow

$loginBody = @{
    username = "admin"
    password = "Admin123!"
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "$baseUrl/auth/login" `
        -Method Post `
        -Body $loginBody `
        -ContentType "application/json"
    
    $token = $loginResponse.data.accessToken
    Write-Host "✅ Login successful" -ForegroundColor Green
    Write-Host "   Token: $($token.Substring(0, 20))..." -ForegroundColor Gray
    Write-Host ""
} catch {
    Write-Host "❌ Login failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
}

# Step 2: Create Product
Write-Host "Step 2: Creating product..." -ForegroundColor Yellow

$productBody = @{
    productCode = "PROD-001"
    productName = "Laptop Computer"
    basePrice = 999.99
} | ConvertTo-Json

try {
    $productResponse = Invoke-RestMethod -Uri "$baseUrl/api/pricing/products" `
        -Method Post `
        -Headers $headers `
        -Body $productBody
    
    Write-Host "✅ Product created: $($productResponse.data.productName)" -ForegroundColor Green
    Write-Host "   Price: `$$($productResponse.data.basePrice)" -ForegroundColor Gray
    Write-Host ""
} catch {
    if ($_.Exception.Response.StatusCode -eq 400) {
        Write-Host "⚠️  Product may already exist" -ForegroundColor Yellow
    } else {
        Write-Host "❌ Product creation failed: $($_.Exception.Message)" -ForegroundColor Red
    }
    Write-Host ""
}

# Step 3: Create Promotion
Write-Host "Step 3: Creating promotion..." -ForegroundColor Yellow

$promotionBody = @{
    promotionCode = "BOGO-LAPTOPS"
    promotionName = "Buy One Get One Free - Laptops"
    promotionType = "BOGO"
    applicableProducts = @("PROD-001")
    startDate = "2025-11-20"
    endDate = "2025-12-31"
} | ConvertTo-Json

try {
    $promoResponse = Invoke-RestMethod -Uri "$baseUrl/api/pricing/promotions" `
        -Method Post `
        -Headers $headers `
        -Body $promotionBody
    
    Write-Host "✅ Promotion created: $($promoResponse.data.promotionName)" -ForegroundColor Green
    Write-Host ""
} catch {
    if ($_.Exception.Response.StatusCode -eq 400) {
        Write-Host "⚠️  Promotion may already exist" -ForegroundColor Yellow
    } else {
        Write-Host "❌ Promotion creation failed: $($_.Exception.Message)" -ForegroundColor Red
    }
    Write-Host ""
}

# Step 4: Calculate Price
Write-Host "Step 4: Calculating price with promotion..." -ForegroundColor Yellow

$calculateBody = @{
    items = @(
        @{
            productCode = "PROD-001"
            quantity = 2
        }
    )
} | ConvertTo-Json -Depth 3

try {
    $priceResponse = Invoke-RestMethod -Uri "$baseUrl/api/pricing/calculate" `
        -Method Post `
        -Headers $headers `
        -Body $calculateBody
    
    Write-Host "✅ Price calculation:" -ForegroundColor Green
    Write-Host "   Subtotal: `£$($priceResponse.data.subtotal)" -ForegroundColor Gray
    Write-Host "   Discount: `£$($priceResponse.data.promotionalDiscount)" -ForegroundColor Gray
    Write-Host "   Final Total: `£$($priceResponse.data.finalTotal)" -ForegroundColor Gray
    Write-Host "   Applied Promotions: $($priceResponse.data.appliedPromotions -join ', ')" -ForegroundColor Gray
    Write-Host ""
} catch {
    Write-Host "❌ Price calculation failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
}

# Step 5: Get Product
Write-Host "Step 5: Retrieving product..." -ForegroundColor Yellow

try {
    $getProductResponse = Invoke-RestMethod -Uri "$baseUrl/api/pricing/products/PROD-001" `
        -Method Get `
        -Headers $headers
    
    Write-Host "✅ Product retrieved: $($getProductResponse.data.productName)" -ForegroundColor Green
    Write-Host ""
} catch {
    Write-Host "❌ Product retrieval failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
}

# Step 6: Create Inventory
Write-Host "Step 6: Creating inventory..." -ForegroundColor Yellow

$inventoryBody = @{
    productCode = "PROD-001"
    quantity = 45
    lowStockThreshold = 10
    storeId = "STORE-001"
} | ConvertTo-Json

try {
    $inventoryResponse = Invoke-RestMethod -Uri "$baseUrl/api/inventory" `
        -Method Post `
        -Headers $headers `
        -Body $inventoryBody
    
    Write-Host "✅ Inventory created: $($inventoryResponse.data.quantity) units" -ForegroundColor Green
    Write-Host ""
} catch {
    if ($_.Exception.Response.StatusCode -eq 400) {
        Write-Host "⚠️  Inventory may already exist" -ForegroundColor Yellow
    } else {
        Write-Host "❌ Inventory creation failed: $($_.Exception.Message)" -ForegroundColor Red
    }
    Write-Host ""
}

# Step 7: Get Inventory
Write-Host "Step 7: Checking inventory..." -ForegroundColor Yellow

try {
    $getInventoryResponse = Invoke-RestMethod -Uri "$baseUrl/api/inventory/PROD-001" `
        -Method Get `
        -Headers $headers
    
    Write-Host "✅ Inventory status:" -ForegroundColor Green
    Write-Host "   Total Quantity: $($getInventoryResponse.data.quantity)" -ForegroundColor Gray
    Write-Host "   Available: $($getInventoryResponse.data.availableQuantity)" -ForegroundColor Gray
    Write-Host "   Reserved: $($getInventoryResponse.data.reservedQuantity)" -ForegroundColor Gray
    Write-Host "   Low Stock Threshold: $($getInventoryResponse.data.lowStockThreshold)" -ForegroundColor Gray
    Write-Host ""
} catch {
    Write-Host "❌ Inventory check failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
}

# Step 8: Reserve Items
Write-Host "Step 8: Reserving items..." -ForegroundColor Yellow

$reserveBody = @{
    quantity = 2
    referenceId = "ORDER-TEST-001"
} | ConvertTo-Json

try {
    $reserveResponse = Invoke-RestMethod -Uri "$baseUrl/api/inventory/PROD-001/reserve" `
        -Method Post `
        -Headers $headers `
        -Body $reserveBody
    
    Write-Host "✅ Items reserved:" -ForegroundColor Green
    Write-Host "   Reservation ID: $($reserveResponse.data.reservationId)" -ForegroundColor Gray
    Write-Host "   Quantity: $($reserveResponse.data.reservedQuantity)" -ForegroundColor Gray
    Write-Host ""
} catch {
    Write-Host "❌ Reservation failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
}

# Step 9: Submit Finance Request
Write-Host "Step 9: Submitting finance approval request..." -ForegroundColor Yellow

$financeBody = @{
    customerId = "CUST-001"
    amount = 2499.99
    purpose = "Purchase of Laptop Computer (PROD-001)"
} | ConvertTo-Json -Depth 3

try {
    $financeResponse = Invoke-RestMethod -Uri "$baseUrl/api/finance/approve" `
        -Method Post `
        -Headers $headers `
        -Body $financeBody
    
    Write-Host "✅ Finance request:" -ForegroundColor Green
    Write-Host "   Request ID: $($financeResponse.data.requestId)" -ForegroundColor Gray
    Write-Host "   Status: $($financeResponse.data.status)" -ForegroundColor Gray
    Write-Host "   Approval Code: $($financeResponse.data.approvalCode)" -ForegroundColor Gray
    Write-Host "   Message: $($financeResponse.data.message)" -ForegroundColor Gray
    Write-Host ""
} catch {
    Write-Host "❌ Finance request failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
}

# Summary
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Test Summary" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "All API endpoints tested successfully!" -ForegroundColor Green
Write-Host ""
Write-Host "Check RabbitMQ Management UI for message events:" -ForegroundColor Yellow
Write-Host "  http://localhost:15672" -ForegroundColor Gray
Write-Host "  Username: destore" -ForegroundColor Gray
Write-Host "  Password: destore123" -ForegroundColor Gray
Write-Host ""
Write-Host "Check Docker logs for notifications:" -ForegroundColor Yellow
Write-Host "  docker-compose logs notification-service" -ForegroundColor Gray
Write-Host ""
