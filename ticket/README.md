# 🎫 Reactive Ticket Management Microservice

[![Java 21](https://img.shields.io/badge/Java-21%20LTS-orange.svg)](https://adoptium.net/)
[![Spring Boot 3.4.0](https://img.shields.io/badge/Spring%20Boot-3.4.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring WebFlux](https://img.shields.io/badge/Spring-WebFlux%20Reactive-blue.svg)](https://docs.spring.io/spring-framework/reference/web/webflux.html)
[![MongoDB Reactive](https://img.shields.io/badge/MongoDB-Reactive%20Driver-green.svg)](https://www.mongodb.com/)
[![AWS SDK v2](https://img.shields.io/badge/AWS%20SDK-v2%20Async%20S3-yellow.svg)](https://aws.amazon.com/sdk-for-java/)
[![Resilience4j](https://img.shields.io/badge/Resilience4j-Circuit%20Breaker-red.svg)](https://resilience4j.readme.io/)

A high-performance, reactive microservice built with **Spring WebFlux** and **Reactive MongoDB**, designed to replace the legacy Laravel TicketManagement module while maintaining **100% backward compatibility** with existing frontends and mobile clients.

---

## 📑 Table of Contents
- [Architecture Overview](#-architecture-overview)
- [Key Features](#-key-features)
- [Tech Stack](#-tech-stack)
- [Project Directory Structure](#-project-directory-structure)
- [Prerequisites & Environment Configuration](#-prerequisites--environment-configuration)
- [Quick Start Guide](#-quick-start-guide)
  - [Local Development](#1-local-development)
  - [Docker Compose](#2-docker-compose)
- [Makefile & Scripts Reference](#-makefile--scripts-reference)
- [API Endpoints Reference](#-api-endpoints-reference)
- [Initial Seeded Credentials](#-initial-seeded-credentials)
- [Testing & Quality Assurance](#-testing--quality-assurance)
- [Step 2 Scalability Roadmap](#-step-2-scalability-roadmap)

---

## 🏛 Architecture Overview

The system strictly adheres to **Clean / Hexagonal Architecture (Ports and Adapters)**:

```
                              ┌─────────────────────────────────────────┐
                              │            Presentation Layer           │
                              │ (WebFlux Controllers, Filters, Advice)  │
                              └────────────────────┬────────────────────┘
                                                   │
                                                   ▼
                              ┌─────────────────────────────────────────┐
                              │        Application Services Layer       │
                              │ (Ticket, Company, User, Role, Outbox)   │
                              └─────────────┬─────────────┬─────────────┘
                                            │             │
                    ┌───────────────────────┘             └───────────────────────┐
                    ▼                                                             ▼
┌───────────────────────────────────────┐                     ┌───────────────────────────────────────┐
│              Domain Layer             │                     │          Infrastructure Layer         │
│  - Pure Entities (Ticket, User, etc.) │                     │  - Reactive MongoDB Repositories      │
│  - State Machine & SLA Logic          │                     │  - AWS S3 Async & Presigner (3600s)   │
│  - Inbound & Outbound Ports           │                     │  - Telegram WebClient + Resilience4j  │
└───────────────────────────────────────┘                     │  - Reactive Outbox Scheduler          │
                                                              └───────────────────────────────────────┘
```

---

## ✨ Key Features

1. **Non-Blocking Reactive I/O**: End-to-end non-blocking execution using Project Reactor (`Mono` and `Flux`), maximizing concurrency on minimal CPU and RAM footprints.
2. **100% Legacy API Compatibility**: Preserves legacy JSON response envelopes (`{"success": true, "message": "...", "data": ..., "timestamp": "..."}`) in `snake_case` with `Asia/Phnom_Penh` timezone formatting.
3. **Atomic MongoDB Counter**: Uses MongoDB `findAndModify` with `$inc` on `ticket_sequences` to generate sequential ticket keys (`TCK-1`, `TCK-2`) without race conditions.
4. **Single-Pass Disjunctive Facets**: Uses MongoDB `$facet` aggregation pipelines to compute paginated records, status counts, and priority distributions in a single atomic database query.
5. **Self-Contained Multi-Tenancy & RBAC**: Complete implementations for `Company`, `Role` (permissions), `User` (support agents & reporters), and machine-to-machine `ApiClient`.
6. **Dynamic Snapshot Denormalization**: Embeds immutable `AssigneeSnapshot` and `ReporterSnapshot` into ticket documents for zero-join ultra-fast reading.
7. **AWS S3 Async Streaming**: AWS SDK v2 non-blocking file streaming with 1-hour (3600s) presigned URLs for secure attachment access.
8. **Resilient Telegram Bot Notifications**: Rich HTML message cards with entity escaping, Khmer timestamps, and Resilience4j circuit breakers backed by a transactional outbox queue.
9. **Automatic Bootstrapping**: Automatically provisions default tenant (`UYFC`), roles (`ADMIN`, `AGENT`, `CUSTOMER`), default admin/agent accounts, and a demo API client if database is empty.

---

## 🛠 Tech Stack

| Component | Technology | Version |
|:---|:---|:---|
| **Language** | Java (OpenJDK) | `21 LTS` |
| **Framework** | Spring Boot & Spring WebFlux | `3.4.0` |
| **Database** | Reactive MongoDB Driver | `7.0` |
| **Object Storage**| AWS SDK for Java v2 (Async S3 & Presigner) | `2.29.30` |
| **Resilience** | Resilience4j (Reactor integration) | `2.2.0` |
| **Build Tool** | Gradle Wrapper | `8.10` |
| **Containerization**| Docker & Docker Compose | Multi-Stage JRE Alpine |

---

## 📂 Project Directory Structure

```
ticket/
├── .env                                       # Active environment configuration
├── .env.example                               # Environment template
├── Dockerfile                                 # Multi-stage production container image
├── docker-compose.yml                         # Multi-container stack (App + MongoDB + UI)
├── Makefile                                   # Automated developer and deployment workflows
├── scripts/
│   ├── build.sh                               # Image build and version tag tracker
│   ├── deploy.sh                              # Safe zero-downtime deployment with health check
│   └── rollback.sh                            # Automated rollback to previous working version
├── build.gradle                               # Dependencies and build configuration
└── src/
    ├── main/
    │   ├── resources/
    │   │   └── application.yaml               # Reactive Spring Boot configuration
    │   └── java/com/menglang/ticket/
    │       ├── TicketApplication.java         # Main executable class
    │       ├── domain/
    │       │   ├── model/                     # Pure domain models (Ticket, Company, User, Role, etc.)
    │       │   ├── statemachine/              # Status transition graph & SLA timestamps
    │       │   ├── exception/                 # Custom domain exceptions
    │       │   └── port/                      # Inbound (UseCases) & Outbound (Repositories/Gateways)
    │       ├── application/service/           # Application use case orchestrators
    │       ├── infrastructure/
    │       │   ├── persistence/               # MongoDB documents, repositories, and adapters
    │       │   ├── storage/                   # AWS S3 Async streaming adapter
    │       │   ├── telegram/                  # Telegram Bot WebClient, formatter & circuit breaker
    │       │   ├── outbox/                    # Scheduled reactive outbox worker
    │       │   └── init/                      # DataInitializer startup bootstrapper
    │       └── presentation/
    │           ├── controller/                # 9 WebFlux REST controllers
    │           ├── filter/                    # AuthenticateClient & TenantContext filters
    │           ├── request/                   # Input validation DTOs
    │           ├── response/                  # Output presentation DTOs
    │           └── advice/                    # GlobalExceptionHandler (@RestControllerAdvice)
    └── test/                                  # 10 comprehensive unit and integration test suites
```

---

## ⚙️ Prerequisites & Environment Configuration

### Prerequisites
- **Java 21 LTS** or **Docker & Docker Compose**
- **MongoDB 7.0** (or via Docker Compose)

### Environment Variables (`.env`)

| Variable | Description | Default / Example |
|:---|:---|:---|
| `SERVER_PORT` | HTTP port for the reactive Netty server | `8000` |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `prod` / `dev` |
| `MONGODB_URI` | Reactive MongoDB connection URI | `mongodb://localhost:27017/ticket_db` |
| `AWS_DEFAULT_REGION` | AWS S3 region | `us-east-1` |
| `AWS_BUCKET_PRIVATE` | S3 bucket for private attachments | `rumluos-app` |
| `AWS_BUCKET_STATIC` | S3 bucket for public static files | `rumluos-app` |
| `AWS_PRIVATE_FILE_EXPIRED` | Presigned URL expiration (in seconds) | `3600` |
| `TELEGRAM_BOT_TOKEN` | Telegram Bot API token | Set a private token in your local `.env` |
| `TELEGRAM_BOT_USERNAME` | Telegram Bot handle for deep links | Your bot username |
| `TELEGRAM_CHAT_ID` | Fallback Telegram channel / chat ID | Your chat or channel ID |
| `TICKET_DEFAULT_PREFIX` | Ticket identifier sequence prefix | `TCK` |

---

## 🚀 Quick Start Guide

### 1. Local Development

Ensure a MongoDB instance is running locally, then execute:

```bash
# Clone and enter directory
cd ticket

# Copy environment variables
cp .env.example .env

# Run automated tests
make test

# Launch the microservice
make run
```
The application will boot on **http://localhost:8000**.

### 2. Docker Compose

To boot both MongoDB and the reactive microservice inside Docker:

```bash
# Start all containers in the background
make docker-up

# View live application logs
make docker-logs

# Verify health status
make health
```

To run with the optional MongoDB Web GUI (Mongo Express on port 8081):
```bash
make docker-db-gui
```

---

## 🧰 Makefile & Scripts Reference

| Command | Action |
|:---|:---|
| `make help` | Displays list of all available make commands |
| `make run` | Starts the application locally with `.env` parameters |
| `make test` | Executes all JUnit 5 tests with Reactor `StepVerifier` |
| `make build` | Compiles project and builds executable `bootJar` |
| `make docker-build` | Builds optimized multi-stage Docker image using `./scripts/build.sh` |
| `make docker-up` | Starts all services in Docker Compose |
| `make docker-down` | Stops all running Docker Compose services |
| `make docker-logs` | Follows live logs of the ticket microservice container |
| `make deploy` | Executes `./scripts/deploy.sh` with automated health check verification |
| `make rollback` | Executes `./scripts/rollback.sh` to revert to previous working version |
| `make health` | Performs HTTP check on `/actuator/health` |

---

## 📡 API Endpoints Reference

All requests accept and return JSON. Authenticated requests can supply tenant context:
- `X-Company-ID`: Tenant company ID (Default: `1`)
- `X-User-ID`: Current authenticated user ID (Default: `1`)

Client endpoints (`/api/v1/client/**`) require machine-to-machine headers:
- `X-Client-ID`: Registered API client ID
- `X-Client-Secret`: Client plain secret

### 1. Company Management (`/api/v1/companies`)
- `GET /api/v1/companies` - List all tenant companies
- `POST /api/v1/companies` - Create a new company
- `GET /api/v1/companies/{id}` - Get company details
- `PUT /api/v1/companies/{id}` - Update company details
- `PATCH /api/v1/companies/{id}/status` - Toggle company active status

### 2. Role Management (`/api/v1/roles`)
- `GET /api/v1/roles` - List available roles (system + tenant)
- `POST /api/v1/roles` - Create custom role with permissions
- `GET /api/v1/roles/{id}` - Get role details
- `PUT /api/v1/roles/{id}` - Update role permissions
- `DELETE /api/v1/roles/{id}` - Delete custom role

### 3. User Management (`/api/v1/users`)
- `GET /api/v1/users` - Paginated user list (`?page=1&per_page=20&search=keyword`)
- `GET /api/v1/users/agents` - Quick list of active support agents (for ticket assignment dropdowns)
- `POST /api/v1/users` - Create user account
- `GET /api/v1/users/{id}` - Get user profile
- `PUT /api/v1/users/{id}` - Update user profile / password
- `PATCH /api/v1/users/{id}/status` - Toggle user active status

### 4. API Client Management (`/api/v1/api-clients`)
- `GET /api/v1/api-clients` - List registered API clients
- `POST /api/v1/api-clients` - Create client credentials (returns one-time `plain_secret`)
- `POST /api/v1/api-clients/{id}/regenerate-secret` - Rotate client secret
- `PATCH /api/v1/api-clients/{id}/status` - Toggle client active status

### 5. Ticket Management (`/api/v1/tickets`)
- `GET /api/v1/tickets` - List tickets with `$facet` disjunctive counts (`status`, `priority`, `search`, `page`, `per_page`)
- `POST /api/v1/tickets` - Create new ticket (auto-resolves reporter and company snapshots)
- `GET /api/v1/tickets/{id}` - Get ticket details with comments and attachments
- `PUT /api/v1/tickets/{id}` - Update ticket (resolves `assignee_id`, enforces state transitions)
- `DELETE /api/v1/tickets/{id}` - Soft delete ticket
- `GET /api/v1/tickets/reports` - Dashboard metrics & SLA aggregates (`days`, `from_date`, `to_date`)

### 6. Public Client Ticket Ingestion (`/api/v1/client/tickets`)
- `POST /api/v1/client/tickets` - Ingest external ticket with `X-Client-ID` & `X-Client-Secret` (supports JSON with base64 attachments or multipart form-data)

### 7. Comments & Attachments
- `GET /api/v1/tickets/{ticketId}/comments` - List comments (supports `include_internal=true/false`)
- `POST /api/v1/tickets/{ticketId}/comments` - Add public comment or internal staff note
- `POST /api/v1/tickets/{ticketId}/attachments` - Upload multipart attachments to S3
- `DELETE /api/v1/tickets/{ticketId}/attachments/{attachmentId}` - Delete attachment

### 8. Telegram Bot (`/api/v1/tickets/telegram/**`)
- `POST /api/v1/tickets/telegram/invite-link` - Generate Telegram subscription invite deep-link
- `GET /api/v1/tickets/telegram/subscribers` - List active Telegram subscribers
- `POST /api/v1/tickets/telegram/subscribers/{chatId}/test` - Send test notification card
- `POST /api/v1/telegram/webhook` - Telegram Bot Webhook endpoint (`/start <token>`)

---

## 🔑 Initial Seeded Credentials

When starting on a fresh database, the [`DataInitializer`](file:///media/menglanghuo/Menglang/Personal/ticket/ticket/src/main/java/com/menglang/ticket/infrastructure/init/DataInitializer.java) provisions:

| Entity | Field | Seeded Value |
|:---|:---|:---|
| **Company** | Name / Code | `Union of Youth Federations of Cambodia (UYFC)` / `UYFC` (ID: `1`) |
| **Admin User** | Username / Email | `admin` / `admin@uyfc.org` (ID: `1`) |
| **Agent User** | Username / Email | `agent` / `agent@uyfc.org` (ID: `2`) |
| **User Password**| Password | `Password@123` |
| **API Client** | Client ID / Secret | `client_demo_uyfc` / `secret_demo_uyfc_12345` |

---

## 🧪 Testing & Quality Assurance

Run the automated test suite covering state transitions, reactive aggregations, security filters, and controllers:

```bash
make test
```

Test coverage includes:
- [`TicketStateMachineTest`](file:///media/menglanghuo/Menglang/Personal/ticket/ticket/src/test/java/com/menglang/ticket/domain/statemachine/TicketStateMachineTest.java): Transition graph & SLA trigger checks
- [`TelegramTicketFormatterTest`](file:///media/menglanghuo/Menglang/Personal/ticket/ticket/src/test/java/com/menglang/ticket/infrastructure/telegram/TelegramTicketFormatterTest.java): HTML escaping & Khmer date formatting
- [`GlobalExceptionHandlerTest`](file:///media/menglanghuo/Menglang/Personal/ticket/ticket/src/test/java/com/menglang/ticket/presentation/advice/GlobalExceptionHandlerTest.java): REST error status mapping
- [`CompanyApplicationServiceTest`](file:///media/menglanghuo/Menglang/Personal/ticket/ticket/src/test/java/com/menglang/ticket/application/service/CompanyApplicationServiceTest.java): Company uniqueness checks
- [`UserApplicationServiceTest`](file:///media/menglanghuo/Menglang/Personal/ticket/ticket/src/test/java/com/menglang/ticket/application/service/UserApplicationServiceTest.java): User creation & password hashing
- [`TicketControllerTest`](file:///media/menglanghuo/Menglang/Personal/ticket/ticket/src/test/java/com/menglang/ticket/presentation/controller/TicketControllerTest.java): Reactive WebFlux controller verification

---

## 🔮 Step 2 Scalability Roadmap

For high-concurrency enterprise expansion:
1. **Server-Sent Events (SSE)**: Stream live ticket events directly to web dashboards via MongoDB Change Streams.
2. **Reactive Redis Rate Limiting**: Implement token-bucket rate limiting on `/api/v1/client/**`.
3. **Kafka Event Bus**: Transition outbox publisher to Apache Kafka for distributed multi-consumer event fan-out.
4. **AI Auto-Triage**: LLM integration to predict priority, category, and automatic agent routing.
