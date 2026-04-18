# Wallet Service

A production-ready RESTful microservice built with **Java 17** and **Spring Boot 4**, designed to manage digital wallets in a payments system. Supports user registration, wallet creation, fund deposits and withdrawals, peer-to-peer transfers, and full transaction history — all backed by PostgreSQL with atomic database operations.

---

## Author

**Sushanta Halder**  
Java Developer | Spring Boot | Microservices  
📧 haldersusanta660@gmail.com
🔗 [GitHub](https://github.com/septopus9/Digital-Wallet-Application)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 4, Spring Data JPA |
| Build Tool | Gradle 9 (Kotlin DSL) |
| Database | PostgreSQL |
| API Docs | SpringDoc OpenAPI / Swagger UI |
| Testing | JUnit 5, Mockito, Testcontainers |
| Code Quality | Checkstyle, PMD, SpotBugs, JaCoCo |
| Containerization | Docker, Docker Compose |

---

## Features

- **User Management** — Register users with username and unique email
- **Wallet Management** — Create one wallet per user, starting at zero balance
- **Deposits** — Add funds to a wallet with transaction logging
- **Withdrawals** — Deduct funds with insufficient-balance guard
- **Peer-to-Peer Transfers** — Atomic transfer between two wallets with dual transaction records
- **Balance Inquiry** — Real-time balance retrieval
- **Global Exception Handling** — Consistent error responses for 400, 404, 409, and 422 scenarios
- **Full Test Coverage** — Unit tests (Mockito), controller slice tests (MockMvc), and integration tests (Testcontainers + real PostgreSQL)

---

## Project Structure

```
wallet-service/
├── src/
│   ├── main/java/com/sh/payments/wallet/
│   │   ├── WalletServiceApplication.java
│   │   ├── controller/          # REST controllers (User, Wallet, Transfer)
│   │   ├── service/             # Service interfaces
│   │   │   └── impl/           # WalletServiceImpl, UserServiceImpl
│   │   ├── model/               # JPA entities (User, Wallet, Transaction)
│   │   ├── repository/          # Spring Data JPA repositories
│   │   ├── dto/                 # Request/response DTOs
│   │   └── exception/           # Custom exceptions + GlobalExceptionHandler
│   ├── main/resources/
│   │   └── application.yml
│   ├── test/java/               # Unit + controller slice tests
│   └── integration/java/        # Integration tests (Testcontainers)
├── config/
│   ├── checkstyle/checkstyle.xml
│   └── pmd/ruleset.xml
├── docker-compose.yml
├── Dockerfile
└── build.gradle.kts
```

---

## Prerequisites

- Java 17 (JDK)
- Gradle 9 (wrapper included — no manual install needed)
- Docker & Docker Compose (for local DB and Testcontainers)

---

## Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/sushantahalder/wallet-service.git
cd wallet-service
```

### 2. Run with Docker Compose (Recommended)

Spins up both PostgreSQL and the application:

```bash
docker-compose up --build
```

- Application: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Database: `localhost:5432` — db: `walletdb`, user: `user`, password: `s3cr3t`

Stop everything:

```bash
docker-compose down
```

### 3. Run in Development Mode

Start only the database:

```bash
docker-compose up -d db
```

Then run the application:

```bash
./gradlew bootRun
```

---

## API Reference

Full interactive docs at **http://localhost:8080/swagger-ui.html** once running.

### Users

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/users` | Register a new user |

**Request Body:**
```json
{
  "username": "sushanta",
  "email": "sushanta@example.com"
}
```

### Wallets

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/wallets` | Create a wallet for a user |
| POST | `/wallets/{id}/deposit` | Deposit funds |
| POST | `/wallets/{id}/withdraw` | Withdraw funds |
| GET | `/wallets/{id}/balance` | Get current balance |

**Deposit / Withdraw Request Body:**
```json
{
  "amount": 500.00
}
```

### Transfers

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/transfers` | Transfer funds between two wallets |

**Request Body:**
```json
{
  "fromWalletId": "uuid-of-source-wallet",
  "toWalletId": "uuid-of-destination-wallet",
  "amount": 200.00
}
```

---

## Error Handling

All errors return a consistent JSON structure:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Wallet not found with id: ...",
  "timestamp": "2025-04-18T10:30:00"
}
```

| HTTP Status | Scenario |
|-------------|----------|
| 400 | Invalid request (e.g. self-transfer, zero amount) |
| 404 | User or Wallet not found |
| 409 | Duplicate resource (e.g. email already registered) |
| 422 | Insufficient funds |

---

## Running Tests

```bash
# Unit tests only
./gradlew test

# Integration tests only (requires Docker for Testcontainers)
./gradlew integrationTest

# All tests + coverage report
./gradlew check
```

JaCoCo coverage report: `build/reports/jacoco/test/html/index.html`

---

## Code Quality

```bash
./gradlew checkstyleMain   # Checkstyle
./gradlew pmdMain          # PMD static analysis
./gradlew spotbugsMain     # SpotBugs — reports in build/reports/spotbugs/
./gradlew check            # All checks + tests
```

---

## Database Schema

Managed by Hibernate (`ddl-auto: update`). All entities use UUID primary keys.

| Table | Columns |
|-------|---------|
| `users` | `id` (UUID PK), `username`, `email` (unique) |
| `wallets` | `id` (UUID PK), `balance` (decimal), `user_id` (FK → users) |
| `transactions` | `id` (UUID PK), `wallet_id` (FK → wallets), `amount`, `type`, `description`, `timestamp` |

**Transaction Types:** `DEPOSIT`, `WITHDRAWAL`, `TRANSFER_IN`, `TRANSFER_OUT`

---

## Key Design Decisions

- **`@Transactional` on all write operations** — ensures atomicity; for transfers, both wallet debits/credits and both transaction log entries commit or rollback together
- **One wallet per user** — enforced at service layer via `walletRepository.existsByUserId()`
- **UUID primary keys** — avoids sequential ID enumeration vulnerabilities
- **DTOs for all API I/O** — JPA entities never exposed directly in the API contract
- **Constructor injection throughout** — all dependencies injected via constructor for clean testability
- **Testcontainers for integration tests** — real PostgreSQL in CI, no in-memory DB mismatch surprises

---

## Building a JAR

```bash
./gradlew build
java -jar build/libs/wallet-service-0.0.1-SNAPSHOT.jar
```
