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
| **External Finance** | 9000 | Finance approval simulator | N/A |

### Infrastructure

- **PostgreSQL**: 5 isolated databases (one per service)
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
