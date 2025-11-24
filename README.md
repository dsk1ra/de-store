# DE-Store: Distributed Store Management System

## Overview

DE-Store is a comprehensive distributed store management system built using Service-Oriented Architecture (SOA) principles. The system provides retail store managers with tools to manage pricing, inventory, customer finance approvals, and receive automated notifications.

## Architecture

### Technology Stack

- **Language**: Java 21
- **Framework**: Spring Boot 3.2.0
- **Database**: PostgreSQL 15
- **Message Queue**: RabbitMQ 3.12
- **API Gateway**: Spring Cloud Gateway
- **Containerization**: Docker & Docker Compose
- **Build Tool**: Maven 3.9

### Microservices

1. **Authentication Service** (Port 8081)
   - User authentication and authorization
   - JWT token generation and validation
   - Role-based access control

2. **Pricing Service** (Port 8082)
   - Product price management
   - Promotional offer configuration (BOGO, 3-for-2, percentage discounts)
   - Price calculation with promotions

3. **Inventory Service** (Port 8083)
   - Stock level monitoring
   - Low-stock detection and alerting
   - Inventory reservation and confirmation

4. **Finance Integration Service** (Port 8084)
   - Integration with external Enabling finance system
   - Finance approval request processing
   - Transaction audit logging

5. **Notification Service** (Port 8085)
   - Asynchronous notification delivery
   - Email and SMS notifications (simulated)
   - Event-driven architecture via RabbitMQ

6. **Enabling Simulator** (Port 9000)
   - Simulates external Enabling finance system
   - Mock approval/rejection logic

7. **API Gateway** (Port 8080)
   - Single entry point for all client requests
   - Request routing and load balancing
   - JWT token validation
   - Service orchestration for complex workflows

## Project Structure

```
de-store/
├── common/                          # Shared DTOs, utilities, security
│   └── src/main/java/com/destore/common/
│       ├── dto/                     # Data Transfer Objects
│       │   ├── ApiResponse.java
│       │   ├── LoginRequest.java
│       │   ├── LoginResponse.java
│       │   ├── LowStockEvent.java
│       │   └── FinanceApprovalEvent.java
│       └── security/
│           └── JwtTokenProvider.java
├── auth-service/                    # Authentication & Authorization
│   └── src/main/java/com/destore/auth/
│       ├── entity/User.java
│       ├── repository/UserRepository.java
│       ├── service/AuthService.java
│       ├── controller/AuthController.java
│       └── config/SecurityConfig.java
├── pricing-service/                 # Pricing Management
├── inventory-service/               # Inventory Management
├── finance-service/                 # Finance Integration
├── notification-service/            # Notification Delivery
├── enabling-simulator/              # External System Simulator
├── api-gateway/                     # API Gateway
├── docker-compose.yml               # Docker orchestration
└── pom.xml                          # Parent POM
```

## Prerequisites

- Java 21 or higher
- Maven 3.9 or higher
- Docker and Docker Compose
- Git

## Quick Start

### 1. Clone the Repository

```bash
git clone <repository-url>
cd de-store
```

### 2. Build All Services

```bash
mvn clean package -DskipTests
```

### 3. Start Infrastructure and Services

```bash
docker-compose up -d
```

This will start:
- PostgreSQL (port 5432)
- RabbitMQ (port 5672, management UI on 15672)
- All microservices
- API Gateway

### 4. Verify Services

Check service health:

```bash
curl http://localhost:8081/auth/health
curl http://localhost:8082/pricing/health
curl http://localhost:8083/inventory/health
curl http://localhost:8084/finance/health
curl http://localhost:8085/notification/health
```

Access RabbitMQ Management UI:
```
http://localhost:15672
Username: destore
Password: destore123
```

## API Documentation

### Authentication Service (8081)

#### Login
```bash
POST /auth/login
Content-Type: application/json

{
  "username": "store.manager1",
  "password": "Password123!"
}

Response:
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGci...",
    "refreshToken": "eyJhbGci...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "username": "store.manager1",
    "role": "STORE_MANAGER"
  },
  "timestamp": "2025-11-20T10:00:00"
}
```

#### Validate Token
```bash
POST /auth/validate
Authorization: Bearer <token>

Response:
{
  "success": true,
  "data": {
    "valid": true,
    "username": "store.manager1",
    "role": "STORE_MANAGER"
  }
}
```

### Pricing Service (8082)

#### Create Product
```bash
POST /api/pricing/products
Authorization: Bearer <token>
Content-Type: application/json

{
  "productCode": "PROD-001",
  "productName": "Laptop Computer",
  "basePrice": 999.99
}
```

#### Create Promotion
```bash
POST /api/pricing/promotions
Authorization: Bearer <token>

{
  "promotionCode": "BOGO-LAPTOPS",
  "promotionName": "Buy One Get One Free",
  "promotionType": "BOGO",
  "applicableProducts": ["PROD-001"],
  "startDate": "2025-11-20",
  "endDate": "2025-12-31"
}
```

#### Calculate Price
```bash
POST /api/pricing/calculate
Authorization: Bearer <token>

{
  "items": [
    {
      "productCode": "PROD-001",
      "quantity": 2
    }
  ]
}

Response:
{
  "success": true,
  "data": {
    "subtotal": 1999.98,
    "promotionalDiscount": 999.99,
    "finalTotal": 999.99,
    "appliedPromotions": ["BOGO-LAPTOPS"]
  }
}
```

### Inventory Service (8083)

#### Get Inventory
```bash
GET /api/inventory/PROD-001
Authorization: Bearer <token>

Response:
{
  "success": true,
  "data": {
    "productCode": "PROD-001",
    "quantity": 45,
    "reservedQuantity": 5,
    "availableQuantity": 40,
    "lowStockThreshold": 10,
    "storeId": "STORE-001"
  }
}
```

#### Update Stock
```bash
PUT /api/inventory/PROD-001
Authorization: Bearer <token>

{
  "quantity": 50,
  "transactionType": "RESTOCK"
}
```

#### Reserve Items
```bash
POST /api/inventory/PROD-001/reserve
Authorization: Bearer <token>

{
  "quantity": 2,
  "referenceId": "ORDER-789"
}
```

#### Get Low Stock Warnings
```bash
GET /api/inventory/warnings
Authorization: Bearer <token>

Response:
{
  "success": true,
  "data": {
    "warnings": [
      {
        "productCode": "PROD-002",
        "currentQuantity": 5,
        "threshold": 10,
        "severity": "HIGH"
      }
    ]
  }
}
```

### Finance Integration Service (8084)

#### Submit Finance Approval Request
```bash
POST /api/finance/approve-request
Authorization: Bearer <token>

{
  "customerName": "John Doe",
  "customerEmail": "john.doe@email.com",
  "purchaseAmount": 2499.99,
  "productCodes": ["PROD-001", "PROD-003"],
  "requestedBy": "store.manager1"
}

Response:
{
  "success": true,
  "data": {
    "requestId": "FIN-REQ-001",
    "status": "APPROVED",
    "approvalCode": "ENBLN-12345",
    "message": "Finance approved for $2499.99"
  }
}
```

### API Gateway (8080) - Orchestrated Workflows

#### Finance-Approved Purchase (Complete Workflow)
```bash
POST /api/orchestration/finance-purchase
Authorization: Bearer <token>

{
  "customerName": "John Doe",
  "customerEmail": "john.doe@email.com",
  "items": [
    {
      "productCode": "PROD-001",
      "quantity": 1
    }
  ]
}

Response:
{
  "success": true,
  "data": {
    "orderId": "ORD-12345",
    "reservationId": "RES-456",
    "financeRequestId": "FIN-REQ-001",
    "financeStatus": "APPROVED",
    "totalAmount": 999.99,
    "message": "Purchase approved and processed successfully"
  }
}
```

This endpoint orchestrates:
1. Inventory reservation
2. Finance approval from Enabling system
3. Transaction recording
4. Inventory confirmation
5. Notification delivery

## Default Test Users

| Username | Password | Role | Email |
|----------|----------|------|-------|
| store.manager1 | Password123! | STORE_MANAGER | manager1@destore.com |
| store.manager2 | Password123! | STORE_MANAGER | manager2@destore.com |
| admin | Admin123! | ADMINISTRATOR | admin@destore.com |

## Sample Test Data

### Products
```json
[
  {"productCode": "PROD-001", "productName": "Laptop Computer", "basePrice": 999.99},
  {"productCode": "PROD-002", "productName": "Wireless Mouse", "basePrice": 29.99},
  {"productCode": "PROD-003", "productName": "USB-C Cable", "basePrice": 19.99},
  {"productCode": "PROD-004", "productName": "Monitor 27\"", "basePrice": 349.99},
  {"productCode": "PROD-005", "productName": "Keyboard", "basePrice": 79.99}
]
```

### Promotions
- **BOGO-MICE**: Buy One Get One Free on Wireless Mouse
- **3FOR2-CABLES**: 3 for 2 on USB-C Cables
- **WINTER25**: 25% off on all products

## Message Queue Events

### Low Stock Event
```json
{
  "eventType": "LOW_STOCK",
  "productCode": "PROD-002",
  "productName": "Wireless Mouse",
  "currentQuantity": 5,
  "threshold": 10,
  "storeId": "STORE-001",
  "severity": "HIGH",
  "timestamp": "2025-11-20T10:00:00Z"
}
```

Exchange: `low_stock_exchange`
Routing Key: `inventory.low_stock`

### Finance Approval Event
```json
{
  "eventType": "FINANCE_APPROVED",
  "requestId": "FIN-REQ-001",
  "customerName": "John Doe",
  "customerEmail": "john.doe@email.com",
  "purchaseAmount": 2499.99,
  "approvalCode": "ENBLN-12345",
  "timestamp": "2025-11-20T10:00:05Z"
}
```

Exchange: `finance_approval_exchange`
Routing Key: `finance.approved`

## Development

### Running Individual Services Locally

Each service can be run independently:

```bash
cd auth-service
mvn spring-boot:run
```

Configure PostgreSQL and RabbitMQ locally or point to Docker instances.

### Building Individual Service

```bash
cd <service-name>
mvn clean package
```

### Running Tests

```bash
mvn test
```

### Creating Docker Image for a Service

```bash
cd <service-name>
docker build -t destore/<service-name>:latest .
```

## Configuration

### Database Configuration

Each service has its own PostgreSQL database:
- auth_db
- pricing_db
- inventory_db
- finance_db
- notification_db

Connection details in `application.yml` for each service:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/<db_name>
    username: destore
    password: destore123
```

### RabbitMQ Configuration

Services that use messaging (Inventory, Finance, Notification):

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: destore
    password: destore123
```

### JWT Configuration

JWT secret and expiration (in common/application.yml):

```yaml
jwt:
  secret: MySecretKeyForDEStoreApplicationMustBe256BitsOrLongerForHS256Algorithm
  expiration: 3600000  # 1 hour
```

## Monitoring and Logging

### Service Health Checks

All services expose health endpoints:
```bash
GET /<service-context>/health
```

### RabbitMQ Management

Access at: http://localhost:15672
- Username: destore
- Password: destore123

Monitor queues, exchanges, and message rates.

### Logs

View service logs:
```bash
docker-compose logs -f <service-name>

# Example:
docker-compose logs -f auth-service
docker-compose logs -f api-gateway
```

## Troubleshooting

### Services won't start

1. Check if ports are already in use:
```bash
netstat -ano | findstr "8080 8081 8082 8083 8084 8085 5432 5672"
```

2. Check Docker logs:
```bash
docker-compose logs <service-name>
```

3. Restart services:
```bash
docker-compose restart
```

### Database connection issues

1. Verify PostgreSQL is running:
```bash
docker-compose ps postgres
```

2. Check database creation:
```bash
docker exec -it destore-postgres psql -U destore -c "\l"
```

### RabbitMQ issues

1. Check RabbitMQ status:
```bash
docker-compose ps rabbitmq
```

2. View RabbitMQ logs:
```bash
docker-compose logs rabbitmq
```

### JWT token issues

- Ensure the JWT secret is the same across all services
- Check token expiration (default 1 hour)
- Validate token format (Bearer <token>)

## Production Considerations

### Security Enhancements

1. **HTTPS**: Enable SSL/TLS for all external communication
2. **Secret Management**: Use environment variables or secret managers (AWS Secrets Manager, HashiCorp Vault)
3. **API Rate Limiting**: Implement rate limiting at API Gateway
4. **Database Encryption**: Enable encryption at rest and in transit
5. **Network Segmentation**: Use VPCs and security groups

### Scalability

1. **Horizontal Scaling**: Deploy multiple instances of each service
2. **Load Balancing**: Use NGINX or cloud load balancers
3. **Auto-Scaling**: Configure Kubernetes HPA (Horizontal Pod Autoscaler)
4. **Database Replication**: Set up read replicas for reporting service
5. **Caching**: Implement Redis for frequently accessed data

### Monitoring

1. **APM**: Application Performance Monitoring (New Relic, Datadog)
2. **Centralized Logging**: ELK Stack (Elasticsearch, Logstash, Kibana)
3. **Distributed Tracing**: Jaeger or Zipkin
4. **Metrics**: Prometheus and Grafana
5. **Alerting**: Configure alerts for service downtime, high latency, errors

### High Availability

1. **Service Redundancy**: Minimum 2 instances per service
2. **Database Clustering**: PostgreSQL with replication
3. **Message Queue Clustering**: RabbitMQ cluster
4. **Health Checks**: Liveness and readiness probes
5. **Circuit Breakers**: Implement resilience patterns

## Testing

### Unit Tests

```bash
mvn test
```

### Integration Tests

```bash
mvn verify
```

### Load Testing

Use Apache JMeter or Gatling:

```bash
# Example: 100 concurrent users for 60 seconds
jmeter -n -t load-test.jmx -l results.jtl
```

### End-to-End Testing

Test complete workflows:

1. Login → Get JWT token
2. Create product → Set price
3. Create promotion → Calculate price
4. Update inventory → Trigger low-stock alert
5. Submit finance request → Receive approval
6. Complete purchase workflow

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License.

## Support

For issues and questions:
- GitHub Issues: <repository-url>/issues
- Email: support@destore.com

## Acknowledgments

- Spring Boot and Spring Cloud teams
- PostgreSQL community
- RabbitMQ team
- All open-source contributors

---

**Version**: 1.0.0  
**Last Updated**: November 20, 2025  
**Maintainer**: DE-Store Development Team
