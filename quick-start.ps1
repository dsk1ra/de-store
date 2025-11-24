# DE-Store Quick Start (PowerShell)
# This script builds and starts the entire DE-Store system

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "DE-Store Quick Start" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

# Check prerequisites
Write-Host "Checking prerequisites..." -ForegroundColor Yellow

$javaInstalled = Get-Command java -ErrorAction SilentlyContinue
$mavenInstalled = Get-Command mvn -ErrorAction SilentlyContinue
$dockerInstalled = Get-Command docker -ErrorAction SilentlyContinue

if (-not $javaInstalled) {
    Write-Host "❌ Java not found. Please install Java 17 or higher." -ForegroundColor Red
    exit 1
}

if (-not $mavenInstalled) {
    Write-Host "❌ Maven not found. Please install Maven 3.9 or higher." -ForegroundColor Red
    exit 1
}

if (-not $dockerInstalled) {
    Write-Host "❌ Docker not found. Please install Docker." -ForegroundColor Red
    exit 1
}

Write-Host "✅ All prerequisites found" -ForegroundColor Green
Write-Host ""

# Build all services
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Building all services..." -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

mvn clean package -DskipTests

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Build failed. Please check the error messages above." -ForegroundColor Red
    exit 1
}

Write-Host "✅ Build successful" -ForegroundColor Green
Write-Host ""

# Start Docker Compose
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Starting Docker Compose..." -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

docker-compose up -d

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Docker Compose failed to start. Please check the error messages above." -ForegroundColor Red
    exit 1
}

Write-Host "✅ Docker Compose started successfully" -ForegroundColor Green
Write-Host ""

# Wait for services to be ready
Write-Host "Waiting for services to be ready..." -ForegroundColor Yellow
Start-Sleep -Seconds 30

# Check service health
Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Checking service health..." -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

$services = @(
    @{Url="http://localhost:8081/auth/health"; Name="Authentication Service"}
    @{Url="http://localhost:8082/pricing/health"; Name="Pricing Service"}
    @{Url="http://localhost:8083/inventory/health"; Name="Inventory Service"}
    @{Url="http://localhost:8084/finance/health"; Name="Finance Service"}
    @{Url="http://localhost:8085/notification/health"; Name="Notification Service"}
    @{Url="http://localhost:9000/api/enabling/health"; Name="Enabling Simulator"}
)

$allHealthy = $true

foreach ($service in $services) {
    try {
        $response = Invoke-WebRequest -Uri $service.Url -UseBasicParsing -TimeoutSec 5
        if ($response.StatusCode -eq 200) {
            Write-Host "✅ $($service.Name)" -ForegroundColor Green
        } else {
            Write-Host "❌ $($service.Name) (not responding)" -ForegroundColor Red
            $allHealthy = $false
        }
    } catch {
        Write-Host "❌ $($service.Name) (not responding)" -ForegroundColor Red
        $allHealthy = $false
    }
}

Write-Host ""

if ($allHealthy) {
    Write-Host "==========================================" -ForegroundColor Green
    Write-Host "🎉 All services are healthy!" -ForegroundColor Green
    Write-Host "==========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Service URLs:" -ForegroundColor Cyan
    Write-Host "  - API Gateway: http://localhost:8080"
    Write-Host "  - Auth Service: http://localhost:8081"
    Write-Host "  - Pricing Service: http://localhost:8082"
    Write-Host "  - Inventory Service: http://localhost:8083"
    Write-Host "  - Finance Service: http://localhost:8084"
    Write-Host "  - Notification Service: http://localhost:8085"
    Write-Host "  - Enabling Simulator: http://localhost:9000"
    Write-Host "  - RabbitMQ Management: http://localhost:15672"
    Write-Host ""
    Write-Host "Default Test Users:" -ForegroundColor Cyan
    Write-Host "  - Username: store.manager1, Password: Password123!"
    Write-Host "  - Username: store.manager2, Password: Password123!"
    Write-Host "  - Username: admin, Password: Admin123!"
    Write-Host ""
    Write-Host "Next Steps:" -ForegroundColor Cyan
    Write-Host "  1. Test login: .\test-login.ps1"
    Write-Host "  2. View logs: docker-compose logs -f"
    Write-Host "  3. Stop services: docker-compose down"
    Write-Host ""
} else {
    Write-Host "⚠️  Some services are not responding. Please check logs:" -ForegroundColor Yellow
    Write-Host "  docker-compose logs -f"
}
