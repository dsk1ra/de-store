-- Create all databases for DE-Store services

CREATE DATABASE auth_db;
CREATE DATABASE pricing_db;
CREATE DATABASE inventory_db;
CREATE DATABASE finance_db;
CREATE DATABASE notification_db;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE auth_db TO destore;
GRANT ALL PRIVILEGES ON DATABASE pricing_db TO destore;
GRANT ALL PRIVILEGES ON DATABASE inventory_db TO destore;
GRANT ALL PRIVILEGES ON DATABASE finance_db TO destore;
GRANT ALL PRIVILEGES ON DATABASE notification_db TO destore;
