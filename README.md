# DE-Store - Distributed Store Management System

A cloud-native microservices-based e-commerce platform built with Spring Boot, demonstrating distributed systems architecture, service discovery, event-driven communication, and resilient patterns.

## 🏗️ Architecture Overview

### Microservices Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         API Gateway (8080)                       │
│                    Central Entry Point / Router                  │
└─────────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
┌───────▼───────┐    ┌───────▼───────┐    ┌───────▼───────┐
│  Auth Service │    │Pricing Service│    │Inventory Svc  │
│    (8081)     │    │    (8082)     │    │    (8083)     │
└───────────────┘    └───────────────┘    └───────────────┘
        │                     │                     │
┌───────▼───────┐    ┌───────▼───────┐    ┌───────▼───────┐
│  Finance Svc  │    │Notification   │    │External Finance│
│    (8084)     │    │ Service (8085)│    │ Service (9000) │
└───────────────┘    └───────────────┘    └────────────────┘
        │                     │
        └──────────┬──────────┘
                   │
        ┌──────────▼──────────┐
        │   RabbitMQ (5672)   │
        │  Message Broker     │
        └─────────────────────┘
```

### Core Services

| Service | Port | Purpose | Database |
|---------|------|---------|----------|
| **Eureka Server** | 8761 | Service Discovery & Registry | N/A |
| **API Gateway** | 8080 | Request routing & orchestration | N/A |
| **Auth Service** | 8081 | User authentication & JWT tokens | PostgreSQL (5433) |
| **Pricing Service** | 8082 | Product pricing & promotions | PostgreSQL (5434) |
| **Inventory Service** | 8083 | Stock management & reservations | PostgreSQL (5435) |
| **Finance Service** | 8084 | Purchase approval workflow | PostgreSQL (5436) |
| **Notification Service** | 8085 | Event-driven notifications | PostgreSQL (5437) |
| **Loyalty Service** | 8086 | Customer loyalty & rewards | PostgreSQL (5438) |
| **Analytics Service** | 8087 | Purchase tracking & reports | PostgreSQL (5439) |
| **Delivery Service** | 8088 | Delivery charge calculation | PostgreSQL (5440) |
| **External Finance** | 9000 | Finance approval simulator | N/A |

### Infrastructure

- **PostgreSQL**: 8 isolated databases (one per service)
- **RabbitMQ**: Message broker (Port 5672, Management UI: 15672)
- **Zipkin**: Distributed tracing (Port 9411)
- **Eureka**: Service discovery (Port 8761)

## 🚀 Quick Start

### Prerequisites

- **Java 17** or higher
- **Maven 3.8+**
- **Docker & Docker Compose**
- **Git**

### 1. Clone the Repository

```bash
git clone https://github.com/dsk1ra/de-store.git
cd de-store
```

### 2. Set Environment Variables

Create a `.env` file in the root directory:

```env
# Database Configuration
DATABASE_PASSWORD=your_secure_password_here
DATABASE_USERNAME=destore

# RabbitMQ Configuration
RABBITMQ_PASSWORD=your_rabbitmq_password
RABBITMQ_USERNAME=destore

# JWT Configuration
JWT_SECRET=your_256_bit_secret_key_here
JWT_EXPIRATION=3600000
```

> ⚠️ **Security Note**: Never commit the `.env` file to version control!

### 3. Build All Services

```bash
./scripts/build-all.sh
```

This will:
- Build the parent POM
- Build the common module
- Build all microservices
- Create JAR files in each service's `target/` directory

### 4. Start Infrastructure & Services

```bash
docker-compose up -d
```

This starts:
- All PostgreSQL databases
- RabbitMQ message broker
- Zipkin tracing server
- All microservices

### 5. Verify Services

Check service health:

```bash
# Check Eureka Dashboard
open http://localhost:8761

# Check service health endpoints
curl http://localhost:8081/auth/health  # Auth Service
curl http://localhost:8082/api/pricing/health  # Pricing Service
curl http://localhost:8083/api/inventory/health  # Inventory Service
```

## 📚 API Documentation

### Authentication API (Port 8081)

#### Login
```bash
POST /auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "password"
}

Response:
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGc...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "username": "admin",
    "role": "ADMINISTRATOR"
  }
}
```

#### Validate Token
```bash
POST /auth/validate
Authorization: Bearer {token}
```

### Pricing API (Port 8082)

#### Create Product
```bash
POST /api/pricing/products
Authorization: Bearer {token}
Content-Type: application/json

{
  "productCode": "LAPTOP-001",
  "productName": "Dell XPS 13",
  "basePrice": 1299.99
}
```

#### Create Promotion
```bash
POST /api/pricing/promotions
Authorization: Bearer {token}
Content-Type: application/json

{
  "promotionCode": "SUMMER2024",
  "promotionName": "Summer Sale",
  "promotionType": "PERCENTAGE_DISCOUNT",
  "discountValue": 20,
  "applicableProducts": ["LAPTOP-001"],
  "startDate": "2024-06-01",
  "endDate": "2024-08-31"
}
```

#### Calculate Price
```bash
POST /api/pricing/calculate
Authorization: Bearer {token}
Content-Type: application/json

{
  "items": [
    {
      "productCode": "LAPTOP-001",
      "quantity": 2
    }
  ]
}
```

### Inventory API (Port 8083)

#### Create Inventory
```bash
POST /api/inventory
Authorization: Bearer {token}
Content-Type: application/json

{
  "productCode": "LAPTOP-001",
  "quantity": 100,
  "lowStockThreshold": 10,
  "storeId": "STORE-01"
}
```

#### Reserve Stock
```bash
POST /api/inventory/{productCode}/reserve
Authorization: Bearer {token}
Content-Type: application/json

{
  "quantity": 5,
  "notes": "Reserved for order #12345"
}
```

#### Check Low Stock
```bash
GET /api/inventory/low-stock?storeId=STORE-01
Authorization: Bearer {token}
```

### Finance API (Port 8084)

#### Request Approval
```bash
POST /api/finance/approve
Authorization: Bearer {token}
Content-Type: application/json

{
  "customerId": "CUST-001",
  "amount": 2500.00,
  "purpose": "Purchase"
}
```

#### Get Queue Statistics
```bash
GET /api/finance/queue-stats
Authorization: Bearer {token}
```

### Loyalty API (Port 8086)

#### Register Customer
```bash
POST /api/loyalty/customers
Authorization: Bearer {token}
Content-Type: application/json

{
  "customerId": "CUST-001",
  "name": "John Doe",
  "email": "john@example.com",
  "phone": "+1234567890",
  "address": "123 Main St"
}
```

#### Record Purchase (Earn Points)
```bash
POST /api/loyalty/customers/{customerId}/purchases
Authorization: Bearer {token}
Content-Type: application/json

{
  "orderId": "ORDER-12345",
  "amount": 150.00,
  "items": "Laptop, Mouse"
}
```

#### Get Regular Customers
```bash
GET /api/loyalty/customers/regular
Authorization: Bearer {token}
```

#### Get Customer Discount
```bash
GET /api/loyalty/customers/{customerId}/discount
Authorization: Bearer {token}
```

#### Create Loyalty Promotion
```bash
POST /api/loyalty/promotions
Authorization: Bearer {token}
Content-Type: application/json

{
  "promotionCode": "GOLD2024",
  "promotionName": "Gold Member Exclusive",
  "description": "Special discount for Gold tier",
  "minTierRequired": "GOLD",
  "discountPercentage": 15,
  "pointsCost": 500,
  "startDate": "2024-01-01T00:00:00",
  "endDate": "2024-12-31T23:59:59"
}
```

### Orchestration API (Port 8080)

#### Process Purchase (Full Workflow)
```bash
POST /api/orchestration/purchase
Authorization: Bearer {token}
Content-Type: application/json

{
  "customerId": "CUST-001",
  "items": [
    {
      "productCode": "LAPTOP-001",
      "quantity": 2
    }
  ],
  "totalAmount": 2599.98
}
```

## 🔧 Configuration

### Environment Variables

Each service supports these environment variables:

**Database:**
- `DATABASE_URL`: JDBC connection string
- `DATABASE_USERNAME`: Database username
- `DATABASE_PASSWORD`: Database password

**RabbitMQ:**
- `RABBITMQ_HOST`: RabbitMQ host (default: localhost)
- `RABBITMQ_PORT`: RabbitMQ port (default: 5672)
- `RABBITMQ_USERNAME`: RabbitMQ username
- `RABBITMQ_PASSWORD`: RabbitMQ password

**Auth Service:**
- `JWT_SECRET`: Secret key for JWT signing (minimum 256 bits)
- `JWT_EXPIRATION`: Token expiration time in milliseconds

**Service Discovery:**
- `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`: Eureka server URL

### Docker Compose Environment

The `docker-compose.yml` file passes environment variables to containers:

```yaml
environment:
  SPRING_DATASOURCE_PASSWORD: ${DATABASE_PASSWORD}
  JWT_SECRET: ${JWT_SECRET}
  RABBITMQ_PASSWORD: ${RABBITMQ_PASSWORD}
```

## 🧪 Testing

### Run Unit Tests

```bash
mvn test
```

### API Testing Script

```bash
./scripts/test-api.sh
```

This comprehensive script tests all endpoints and workflows.

## 🔍 Monitoring & Debugging

### Eureka Dashboard
Service registry and health status:
```
http://localhost:8761
```

### RabbitMQ Management
Message queue monitoring:
```
http://localhost:15672
Username: destore
Password: ${RABBITMQ_PASSWORD}
```

### Zipkin Tracing
Distributed request tracing:
```
http://localhost:9411
```

### Database Access

Connect to any database:
```bash
psql -h localhost -p 5433 -U destore -d auth_db
# Password: ${DATABASE_PASSWORD}
```

## 📁 Project Structure

```
de-store/
├── api-gateway/           # API Gateway & Orchestration
├── auth-service/          # Authentication & Authorization
├── common/                # Shared DTOs, exceptions, utilities
├── eureka-server/         # Service Discovery
├── finance-approval-automation/  # Finance Approval Automation Service
├── finance-service/       # Finance Integration Service
├── inventory-service/     # Inventory Management
├── notification-service/  # Event-Driven Notifications
├── pricing-service/       # Product Pricing & Promotions
├── docker/               # Docker configuration files
│   └── init-scripts/     # Database initialization scripts
├── scripts/              # Build and utility scripts
├── web/                  # Frontend UI (optional)
├── docker-compose.yml    # Service orchestration
├── pom.xml              # Parent Maven configuration
└── README.md            # This file
```

## 📊 Analytics Service

### Overview

The Analytics Service tracks purchase activities and generates comprehensive reports on store performance. It subscribes to purchase events via RabbitMQ and maintains an accounting database for analytical queries.

**Port**: 8087  
**Database**: PostgreSQL (5439)

### Key Features

1. **Transaction Tracking**: Automatically records all purchase transactions
2. **Sales Reports**: Generate sales reports for any date range
3. **Performance Metrics**: Track store-level performance indicators
4. **Top Products**: Identify best-selling products
5. **Customer Analytics**: Analyze customer purchase behavior

### Data Model

```
sales_transactions
├── transaction_id (unique)
├── customer_id
├── customer_name
├── store_id
├── total_amount
├── discount_amount
├── tax_amount
├── net_amount
├── payment_method
├── transaction_status
├── items (JSON)
└── transaction_date

product_sales_summary
├── product_code
├── report_date
├── quantity_sold
├── total_revenue
├── average_price
└── transaction_count

store_performance
├── store_id
├── report_date
├── total_sales
├── transaction_count
├── average_transaction_value
└── net_revenue

customer_analytics
├── customer_id
├── report_date
├── total_purchases
├── total_spent
├── average_order_value
├── first_purchase_date
└── last_purchase_date
```

### API Endpoints

**Track Transaction**
```bash
POST /analytics/transactions
Content-Type: application/json

{
  "transactionId": "TXN-001",
  "customerId": "CUST-123",
  "customerName": "John Doe",
  "storeId": "STORE-01",
  "totalAmount": 150.00,
  "discountAmount": 15.00,
  "taxAmount": 12.00,
  "netAmount": 135.00,
  "paymentMethod": "CREDIT_CARD",
  "transactionStatus": "COMPLETED",
  "items": "{\"products\": [...]}",
  "transactionDate": "2024-01-15T10:30:00"
}
```

**Generate Sales Report**
```bash
GET /analytics/reports/sales?startDate=2024-01-01&endDate=2024-01-31

Response:
{
  "startDate": "2024-01-01",
  "endDate": "2024-01-31",
  "totalSales": 45000.00,
  "totalDiscounts": 4500.00,
  "netRevenue": 40500.00,
  "transactionCount": 350,
  "averageTransactionValue": 128.57,
  "customerCount": 180
}
```

**Get Store Performance**
```bash
GET /analytics/reports/performance?reportDate=2024-01-15

Response:
[
  {
    "storeId": "STORE-01",
    "period": "2024-01-15",
    "totalRevenue": 5200.00,
    "totalTransactions": 42,
    "averageOrderValue": 123.81,
    "status": "Active"
  }
]
```

**Get Top Customers**
```bash
GET /analytics/customers/top?reportDate=2024-01-15&limit=10

Response:
[
  {
    "customerId": "CUST-123",
    "totalPurchases": 15,
    "totalSpent": 2500.00,
    "averageOrderValue": 166.67,
    "firstPurchaseDate": "2023-06-01",
    "lastPurchaseDate": "2024-01-15",
    "daysSinceLastPurchase": 0
  }
]
```

**Get Top Products**
```bash
GET /analytics/products/top?reportDate=2024-01-15&limit=10

Response:
[
  {
    "productCode": "PROD-001",
    "reportDate": "2024-01-15",
    "quantitySold": 85,
    "totalRevenue": 4250.00,
    "averagePrice": 50.00,
    "transactionCount": 42
  }
]
```

### Event Integration

The Analytics Service automatically listens to purchase events:

**RabbitMQ Configuration**:
- Exchange: `purchase.exchange`
- Queue: `purchase.tracking.queue`
- Routing Key: `purchase.tracking`

Other services can publish purchase events that will be automatically tracked:

```java
rabbitTemplate.convertAndSend(
    "purchase.exchange",
    "purchase.tracking",
    transactionRequest
);
```

### Web Dashboard

Access the Analytics Dashboard at:
```
http://localhost/pages/analytics.html
```

Features:
- Real-time metrics (Today/Week/Month)
- Sales report generation
- Top products ranking
- Top customers analysis
- Store performance comparison

## 🚚 Delivery Service

### Overview

The Delivery Service calculates delivery charges based on multiple factors including distance, order value, delivery zone, express delivery options, and peak hours. It implements sophisticated pricing rules with automatic discounts.

**Port**: 8088  
**Database**: PostgreSQL (5440)

### Key Features

1. **Distance-Based Pricing**: $1.50/km with first 5km free
2. **Zone-Based Surcharges**: 
   - City Center (0-10km): 1.0x multiplier
   - Suburban (10-25km): 1.2x multiplier
   - Rural (25-50km): 1.5x multiplier
   - Remote (50-100km): 2.0x multiplier
3. **Order Value Discounts**:
   - Orders $50-$99: 50% off delivery
   - Orders $100+: FREE delivery
4. **Express Delivery**: +$10 surcharge, 2-hour delivery
5. **Peak Hour Pricing**: +20% during 11-13 and 18-20
6. **Order Tracking**: Full lifecycle from pending to delivered

### Pricing Calculation Formula

```
Base Charge = $5.00
Distance Charge = (distance - 5km) × $1.50/km  [if distance > 5km]
Zone Charge = Distance Charge × (zone_multiplier - 1.0)
Express Charge = $10.00  [if express]
Peak Hour Charge = (Base + Distance + Zone) × 20%  [if peak hour]

Subtotal = Base + Distance + Zone + Express + Peak Hour

Discount:
  - If order_value >= $100: discount = Subtotal (FREE)
  - If order_value >= $50: discount = Subtotal × 50%
  - Else: discount = $0

Final Delivery Charge = Subtotal - Discount
```

### Data Model

```
delivery_orders
├── order_id (unique)
├── customer_id
├── store_id
├── order_value
├── delivery_charge
├── distance
├── zone (CITY_CENTER/SUBURBAN/RURAL/REMOTE)
├── is_express
├── is_peak_hour
├── status (PENDING/CONFIRMED/ASSIGNED/IN_TRANSIT/DELIVERED)
├── delivery_address
├── pickup_address
├── estimated_delivery_time
├── actual_delivery_time
├── driver_name
├── driver_phone
├── vehicle_number
├── base_charge
├── distance_charge
├── zone_charge
├── express_charge
├── peak_hour_charge
└── discount

delivery_zones
├── zone (enum)
├── multiplier
├── min_distance
├── max_distance
└── description

delivery_pricing
├── base_charge
├── rate_per_km
├── free_distance_threshold
├── free_delivery_threshold
├── reduced_rate_threshold
├── reduced_rate_percentage
├── express_surcharge
├── peak_hour_surcharge_percentage
├── effective_from
└── effective_until
```

### API Endpoints

**Calculate Delivery Charge**
```bash
POST /delivery/calculate
Content-Type: application/json

{
  "orderId": "ORDER-001",
  "customerId": "CUST-001",
  "customerName": "John Doe",
  "storeId": "STORE-01",
  "orderValue": 75.00,
  "distance": 15.0,
  "deliveryAddress": "456 Oak Ave, Suburban Area",
  "pickupAddress": "Store 01, Downtown",
  "isExpress": false,
  "notes": "Ring doorbell"
}

Response:
{
  "orderId": "ORDER-001",
  "orderValue": 75.0,
  "distance": 15.0,
  "zone": "SUBURBAN",
  "isExpress": false,
  "isPeakHour": false,
  "baseCharge": 5.0,
  "distanceCharge": 15.0,
  "zoneCharge": 3.0,
  "expressCharge": 0,
  "peakHourCharge": 0,
  "discount": 11.5,
  "totalDeliveryCharge": 11.5,
  "discountReason": "50% off delivery for orders over $50",
  "chargeBreakdown": "Base: $5.00\nDistance: $15.00\nZone: $3.00\nDiscount: -$11.50",
  "grandTotal": 86.5
}
```

**Get Delivery Order**
```bash
GET /delivery/orders/{orderId}

Response:
{
  "id": 1,
  "orderId": "ORDER-001",
  "customerId": "CUST-001",
  "orderValue": 75.0,
  "deliveryCharge": 11.5,
  "distance": 15.0,
  "zone": "SUBURBAN",
  "status": "PENDING",
  "deliveryAddress": "456 Oak Ave",
  "estimatedDeliveryTime": "2024-01-15T15:00:00"
}
```

**Get Customer Deliveries**
```bash
GET /delivery/customers/{customerId}

Response: Array of delivery orders
```

**Get Deliveries by Status**
```bash
GET /delivery/status/{status}
# status: PENDING, CONFIRMED, ASSIGNED, IN_TRANSIT, DELIVERED, CANCELLED
```

**Update Delivery Status**
```bash
PUT /delivery/status
Content-Type: application/json

{
  "orderId": "ORDER-001",
  "status": "IN_TRANSIT",
  "driverName": "Mike Johnson",
  "driverPhone": "+1-555-0123",
  "vehicleNumber": "DEL-123",
  "notes": "Out for delivery"
}
```

### Pricing Examples

**Example 1: Short Distance, Low Order Value**
- Order: $45.00, Distance: 3.5km
- Result: Base $5.00 + Distance $0 (within free 5km) = **$5.00**

**Example 2: Suburban, Medium Order Value**
- Order: $75.00, Distance: 15km  
- Base: $5.00
- Distance: (15-5) × $1.50 = $15.00
- Zone (1.2x): $15.00 × 0.2 = $3.00
- Subtotal: $23.00
- Discount (50%): -$11.50
- Result: **$11.50**

**Example 3: Rural, High Order Value, Express**
- Order: $150.00, Distance: 30km, Express
- Base: $5.00
- Distance: (30-5) × $1.50 = $37.50
- Zone (1.5x): $37.50 × 0.5 = $18.75
- Express: $10.00
- Subtotal: $71.25
- Discount (100%): -$71.25
- Result: **$0.00 (FREE)**

### Web Dashboard

Access the Delivery Calculator at:
```
http://localhost/pages/delivery.html
```

Features:
- Interactive delivery charge calculator
- Real-time pricing with all factors
- Zone visualization
- Discount calculation display
- Recent delivery orders table
- Charge breakdown details

### Configuration

Pricing configuration is stored in the database and can be updated via the `delivery_pricing` table. Initial defaults:
- Base charge: $5.00
- Rate per km: $1.50
- Free distance: 5km
- Free delivery threshold: $100
- Reduced rate threshold: $50
- Express surcharge: $10.00
- Peak hour surcharge: 20%

## 🏛️ Design Patterns & Best Practices

### Implemented Patterns

1. **Microservices Architecture**: Independently deployable services
2. **Service Discovery**: Eureka for dynamic service registration
3. **API Gateway**: Centralized routing and orchestration
4. **Event-Driven Communication**: RabbitMQ for async messaging
5. **Circuit Breaker**: Resilience4j for fault tolerance
6. **Distributed Tracing**: Zipkin for request tracking
7. **Database per Service**: Isolated data stores
8. **Global Exception Handling**: Centralized error management
9. **DTO Validation**: Jakarta Validation API

### Security Features

- ✅ JWT-based authentication
- ✅ Environment variable configuration
- ✅ Input validation on all endpoints
- ✅ Secure password hashing (BCrypt)
- ✅ CORS configuration
- ✅ Stateless session management

## 🛠️ Development

### Adding a New Service

1. Create service module in parent POM
2. Implement service with Spring Boot
3. Add Eureka client configuration
4. Create Dockerfile
5. Add service to docker-compose.yml
6. Update API Gateway routes

### Database Migrations

Use the initialization scripts in `docker/init-scripts/`:
- `init-auth-db.sql`
- `init-pricing-db.sql`
- `init-inventory-db.sql`
- etc.

## 🐛 Troubleshooting

### Service Won't Start

1. Check if port is already in use:
   ```bash
   lsof -i :8081
   ```

2. Verify environment variables are set
3. Check logs:
   ```bash
   docker-compose logs -f auth-service
   ```

### Database Connection Issues

1. Verify PostgreSQL is running:
   ```bash
   docker ps | grep postgres
   ```

2. Check connection string in application.yml
3. Ensure DATABASE_PASSWORD is set

### RabbitMQ Message Not Delivered

1. Check RabbitMQ Management UI
2. Verify queue bindings
3. Check consumer logs

## 📊 Performance Considerations

- **Connection Pooling**: Configured for optimal database performance
- **Async Processing**: Event-driven architecture for non-blocking operations
- **Caching**: Consider adding Redis for frequently accessed data
- **Load Balancing**: Multiple instances supported via Eureka

## 🔐 Security Recommendations for Production

1. ✅ Use strong, unique passwords (implemented via env vars)
2. ✅ Rotate JWT secrets regularly
3. ✅ Enable HTTPS/TLS for all communications
4. ✅ Implement rate limiting
5. ✅ Use a secrets management system (AWS Secrets Manager, Vault)
6. ✅ Enable database encryption at rest
7. ✅ Implement API authentication on all endpoints
8. ✅ Regular security audits and dependency updates

## 📝 License

This project is for educational purposes.

## 👥 Contributing

Contributions welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## 📞 Support

For issues and questions:
- GitHub Issues: [Create an issue](https://github.com/dsk1ra/de-store/issues)
- Documentation: This README
- API Documentation: See API section above

## 🎯 Future Enhancements

- [ ] API rate limiting with Redis
- [ ] GraphQL API gateway
- [ ] Kubernetes deployment manifests
- [ ] Comprehensive integration tests
- [ ] Performance monitoring with Prometheus/Grafana
- [ ] API documentation with Swagger/OpenAPI
- [ ] Frontend UI implementation
- [ ] Caching layer with Redis
- [ ] Event sourcing for audit trails

---

**Built with ❤️ using Spring Boot, Spring Cloud, and modern microservices patterns**
