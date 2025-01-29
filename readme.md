# MarlowBank - ATM Banking Application

## Overview
MarlowBank is a **Spring Boot-based ATM Banking Application** that allows users to:
- **Deposit and withdraw money** from an account.
- **Support joint account access** with concurrency handling.
- **Prevent overdrafts** (balance cannot fall below 0).
- **Log all transactions asynchronously using Kafka**.
- **Store transaction logs in an audit (change log) table**.
- **Fully containerized setup using Docker.**

---

## 🚀 Getting Started
### **1. Prerequisites**
Ensure you have the following installed:
- Java 17+ (Recommended: OpenJDK 17 or higher)
- Maven 3+
- Docker & Docker Compose
- PostgreSQL
- Kafka (or run via Docker)

### **2. Clone the Repository**
```bash
git clone https://github.com/Mykiiii/marlowbank.git
cd marlowbank
```

### **3. Set Up PostgreSQL Database**
#### **Option 1: Run PostgreSQL via Docker**
```bash
docker run --name postgres-db -e POSTGRES_USER=marlow_bank_user -e POSTGRES_PASSWORD=yourpassword -e POSTGRES_DB=marlow_bank -p 5432:5432 -d postgres:latest
```

#### **Option 2: Manual Setup**
1. Install PostgreSQL.
2. Create a new database `marlow_bank`.
3. Configure `application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/marlow_bank
spring.datasource.username=marlow_bank_user
spring.datasource.password=yourpassword
```

### **4. Run Kafka using Docker**
```bash
docker-compose up -d
```
Or manually start Kafka:
```bash
docker network create kafka_network

docker run -d --name zookeeper --net kafka_network -p 2181:2181 \
    -e ALLOW_ANONYMOUS_LOGIN=yes bitnami/zookeeper:latest

docker run -d --name kafka --net kafka_network -p 9092:9092 \
    -e KAFKA_BROKER_ID=1 -e KAFKA_CFG_ZOOKEEPER_CONNECT=zookeeper:2181 \
    -e KAFKA_CFG_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
    -e ALLOW_PLAINTEXT_LISTENER=yes bitnami/kafka:latest
```

### **5. Build and Run the Application**
```bash
mvn clean install
mvn spring-boot:run
```

---

## 🛠 API Endpoints
### **1. Create an Account**
```bash
curl -X POST "http://localhost:8080/api/accounts/create?accountNumber=123456&name=JohnDoe"
```

### **2. Deposit Money**
```bash
curl -X POST "http://localhost:8080/api/accounts/123456/deposit?amount=500"
```

### **3. Withdraw Money**
```bash
curl -X POST "http://localhost:8080/api/accounts/123456/withdraw?amount=200"
```

### **4. Check Kafka Logs**
```bash
docker exec -it kafka kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic change-log-topic --from-beginning
```

---

## 🔥 Improvements and Future Enhancements
### ✅ **1. Exception Handling Improvements**
- Implemented **Global Exception Handler** to return structured JSON error messages.
- **Example Error Response:**
```json
{
  "error": "Account not found",
  "status": 404,
  "timestamp": "2025-01-30T12:00:00Z"
}
```

### ✅ **2. Optimized Kafka Integration**
- Kafka **now logs every deposit/withdrawal event**.
- **Consumers store messages in the `change_log` audit table.**

### ✅ **3. Added Change Log (Audit) Table**
- A new `change_log` table tracks all transactions asynchronously.
- **Query Logs:**
```sql
SELECT * FROM change_log;
```

### ✅ **4. Dockerized the Entire System**
- **Kafka, PostgreSQL, and the Spring Boot app** can now run via Docker Compose.
- **Future work:** Add Kubernetes support for cloud scalability.

---

## 📌 Next Steps
- **Implement JWT authentication** for secure API access.
- **Enable dynamic pricing for withdrawals (e.g., peak hours).**
- **Proper Exception handeling, service discovery & cors.**
- **Enhance logging & monitoring using Prometheus + Grafana.**

---



# Docker Setup for MarlowBank Application

## Step 1: Install Docker Desktop

---

## Step 2: Create `Dockerfile`
Create a `Dockerfile` in the root project directory:
```dockerfile
# Use OpenJDK 17 runtime
FROM openjdk:17-jdk-slim

# Set working directory
WORKDIR /app

# Copy built JAR file
COPY target/marlowbank-0.0.1-SNAPSHOT.jar marlowbank.jar

# Expose port
EXPOSE 8080

# Run application
ENTRYPOINT ["java", "-jar", "marlowbank.jar"]
```

---

## Step 3: Create `docker-compose.yml`
Create `docker-compose.yml` in the root directory:
```yaml
version: '3.9'

services:
  postgres:
    image: postgres:15
    container_name: marlowbank_postgres
    restart: always
    environment:
      POSTGRES_DB: marlow_bank
      POSTGRES_USER: marlow_bank_user
      POSTGRES_PASSWORD: password123
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data

  zookeeper:
    image: confluentinc/cp-zookeeper:7.4.0
    container_name: zookeeper
    restart: always
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:7.4.0
    container_name: kafka
    depends_on:
      - zookeeper
    restart: always
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1

  marlowbank:
    build: .
    container_name: marlowbank_app
    depends_on:
      - postgres
      - kafka
    restart: always
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/marlow_bank
      SPRING_DATASOURCE_USERNAME: marlow_bank_user
      SPRING_DATASOURCE_PASSWORD: password123
      SPRING_JPA_HIBERNATE_DDL_AUTO: update
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
    ports:
      - "8080:8080"

volumes:
  pgdata:
```


---

## Step 4: Build and Run Docker Containers
Run the following commands in the project root:

```bash
# Build JAR file
mvn clean package -DskipTests

# Build Docker image
docker build -t marlowbank-app .

# Start all services
docker-compose up -d
```

---

## Step 5: Stop and Cleanup
To stop containers:
```bash
docker-compose down
```

To remove unused images and volumes:
```bash
docker system prune -a -f
docker volume prune -f
```

---