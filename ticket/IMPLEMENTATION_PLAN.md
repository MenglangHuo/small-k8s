# Implementation Plan: Laravel to Spring WebFlux & Reactive MongoDB Migration

Migrate the **`TicketManagement`** Laravel module into the standalone **`ticket`** Spring Boot application using **Spring WebFlux (Reactive)**, **Reactive MongoDB**, **AWS S3 file storage**, and **Telegram Bot notifications**. The architecture follows **Clean / Hexagonal Architecture (Ports and Adapters)** and proven enterprise design patterns while preserving **100% backward compatibility** with the existing frontend JSON API contract.

---

## 1. Environment & Configuration Matrix

Mapping configuration parameters into `application.yaml`:

```yaml
server:
  port: 8000

spring:
  application:
    name: ticket
  data:
    mongodb:
      uri: ${MONGODB_URI:mongodb://localhost:27017/ticket_db}
      auto-index-creation: true
  jackson:
    property-naming-strategy: SNAKE_CASE
    date-format: "yyyy-MM-dd HH:mm:ss"
    time-zone: "Asia/Phnom_Penh"

aws:
  s3:
    region: ${AWS_DEFAULT_REGION:us-east-1}
    bucket-private: ${AWS_BUCKET_PRIVATE:rumluos-app}
    bucket-static: ${AWS_BUCKET_STATIC:rumluos-app}
    presigned-expiry-seconds: ${AWS_PRIVATE_FILE_EXPIRED:3600}
    static-url: ${AWS_STATIC_URL:}
    private-url: ${AWS_PRIVATE_URL:}

telegram:
  bot-token: ${TELEGRAM_BOT_TOKEN:}
  bot-username: ${TELEGRAM_BOT_USERNAME:menglang6969Bot}
  default-chat-id: ${TELEGRAM_CHAT_ID:-1004247055582}
  webhook-secret: ${TELEGRAM_WEBHOOK_SECRET:}
  invite-expiry-days: 7

resilience4j:
  circuitbreaker:
    instances:
      telegram_api:
        sliding-window-size: 10
        minimum-number-of-calls: 5
        failure-rate-threshold: 50
        wait-duration-in-open-state: 300s
        permitted-number-of-calls-in-half-open-state: 3
```

---

## 2. Architectural Blueprint & Design Patterns

```
                                      ┌──────────────────────────────────────────────┐
                                      │              HTTP Presentation Layer         │
                                      │   (Controllers, WebFilters, RestAdvice)     │
                                      └──────────────────────┬───────────────────────┘
                                                             │
                                                             ▼
                                      ┌──────────────────────────────────────────────┐
                                      │           Inbound Ports (Use Cases)          │
                                      └──────────────────────┬───────────────────────┘
                                                             │
                                                             ▼
┌───────────────────────────┐         ┌──────────────────────────────────────────────┐
│       Domain Layer        │         │              Application Services            │
│  - Pure Domain Models     │◄───────►│  - TicketApplicationService                  │
│  - TicketStateMachine     │         │  - CommentApplicationService                 │
│  - SLA Timestamp Engine   │         │  - OutboxProcessorService                    │
└───────────────────────────┘         └──────────────────────┬───────────────────────┘
                                                             │
                                                             ▼
                                      ┌──────────────────────────────────────────────┐
                                      │        Outbound Ports (Repository / SPI)     │
                                      └──────────────────────┬───────────────────────┘
                                                             │
                      ┌──────────────────────────────────────┼──────────────────────────────────────┐
                      ▼                                      ▼                                      ▼
       ┌──────────────────────────────┐       ┌──────────────────────────────┐       ┌──────────────────────────────┐
       │   Reactive MongoDB Adapter   │       │      AWS S3 Async Adapter    │       │     Telegram WebClient       │
       │ - Atomic Sequence ($inc)     │       │ - Non-blocking File Streaming│       │ - Resilience4j CircuitBreaker│
       │ - Reactive Aggregation $facet│       │ - Presigned S3 URLs (3600s)  │       │ - HTML Sanitizer & Formatter │
       └──────────────────────────────┘       └──────────────────────────────┘       └──────────────────────────────┘
```

### Key Design Patterns Implemented:
1. **Hexagonal Architecture (Ports and Adapters)**: Clean separation between business domain, application use cases, and infrastructure adapters (Mongo, S3, Telegram).
2. **State Machine Pattern**: Enforces status transitions (`open` $\rightarrow$ `in_progress` $\rightarrow$ `resolved` $\rightarrow$ `closed`) and triggers SLA timestamps (`first_response_at`, `resolved_at`, `closed_at`).
3. **Transactional Outbox Pattern**: Decouples ticket submission from Telegram API dispatching, guaranteeing zero dropped notifications with dead-letter queue recovery.
4. **Atomic Sequence Counter Pattern**: Utilizes MongoDB `findAndModify` with `$inc: { last_number: 1 }` to produce sequential keys (`TCK-101`) without race conditions.
5. **Disjunctive Faceting Pattern**: Employs MongoDB Aggregation Pipeline (`$facet`) to compute paginated records, status badges, and priority distributions in a single non-blocking database pass.
6. **Circuit Breaker & Retry Patterns (Resilience4j)**: Prevents external Telegram Bot or AWS S3 network degradation from stalling the reactive event loop.
7. **Snapshot Denormalization Pattern**: Embeds employee and company profile snapshots inside tickets and comments to eliminate relational joins.

---

## 3. Handling Non-Existent Tables (`users`, `role`, `company`, `employees`)

1. **Document Denormalization (Snapshot Pattern)**:
   In MongoDB, tickets and comments store lightweight user and company snapshots directly inside the document:
   ```json
   {
     "assignee": { "id": 2, "name": "Branch Admin" },
     "reporter": { "id": 1, "name": "Sophea Seng" },
     "company": "UYFC"
   }
   ```
   This guarantees high-speed reads without multi-collection joins and preserves immutable history.
2. **Tenant Context Propagation**:
   `X-Company-ID` or JWT claims are extracted via a non-blocking `WebFilter` and attached to Project Reactor's `ContextView`, making tenant context accessible across every reactive stream.
3. **Pluggable Stub Collections**:
   Provide stub documents and initial seeders for `companies`, `users`, and `roles` so the ticket microservice runs completely standalone in development and testing.

---

## 4. Package Structure in `ticket/`

```
com.menglang.ticket
├── domain/                          # Pure business domain (Framework-agnostic)
│   ├── model/                      # Ticket, TicketStatus, Priority, Comment, Attachment, etc.
│   ├── statemachine/               # TicketStateMachine (Transitions & SLA triggers)
│   └── exception/                  # Domain-specific exceptions
├── application/                     # Use Cases & Application Services
│   ├── port/in/                    # Inbound ports (Service interfaces)
│   ├── port/out/                   # Outbound ports (Repository & Gateway interfaces)
│   ├── service/                    # Reactive Service Implementations
│   └── dto/                        # Command & Query DTOs
├── infrastructure/                  # Adapters for DB, External APIs, Storage
│   ├── persistence/
│   │   ├── document/               # Mongo Document classes with @Document & @Indexed
│   │   ├── repository/             # ReactiveMongoRepository interfaces
│   │   └── adapter/                # Outbound repository implementations
│   ├── client/telegram/            # WebClient adapter for Telegram Bot API
│   ├── outbox/                     # Reactive Outbox scheduler & worker with Resilience4j
│   ├── storage/                    # S3 / MinIO / Local file storage reactive adapter
│   └── config/                     # Mongo, WebClient, Security, Jackson configurations
└── presentation/                    # Inbound HTTP Adapters
    ├── controller/                 # @RestController WebFlux controllers
    ├── request/                    # Inbound request bodies with JSR-380 validation
    ├── response/                   # Outbound DTOs matching legacy JSON schema
    ├── filter/                     # Reactive Client Auth & JWT Security filters
    └── advice/                     # @RestControllerAdvice Global Error Handler
```

---

## 5. Endpoints & Strict Compatibility Matrix

| Method | Endpoint | Auth Mechanism | Notes / Response Contract |
|---|---|---|---|
| `POST` | `/api/v1/client/tickets` | `X-Client-ID` + `X-Client-Secret` | Accepts Multipart or Base64 JSON. Enqueues outbox Telegram alert. Returns HTTP 201 with `TicketResource`. |
| `GET` | `/api/v1/tickets` | Bearer Token / Admin Context | Returns faceted pagination: `{ content: [...], pagination: {...}, applied_filters: {...}, facets: { status: {...}, priority: {...}, total: N } }`. |
| `GET` | `/api/v1/tickets/reports` | Bearer Token / Admin Context | Executive BI summary (status, priority, type distribution, SLA resolution rate). |
| `POST` | `/api/v1/tickets` | Bearer Token / Admin Context | Internal ticket creation (does not enqueue Telegram alert). |
| `GET` | `/api/v1/tickets/{id}` | Bearer Token / Admin Context | Returns ticket details with eager-loaded attachments and comments. |
| `PUT` | `/api/v1/tickets/{id}` | Bearer Token / Admin Context | Update fields or change status via `TicketStateMachine`. |
| `DELETE` | `/api/v1/tickets/{id}` | Bearer Token / Admin Context | Soft delete (`is_deleted = true`). Returns `{ success: true, message: "..." }`. |
| `GET` | `/api/v1/tickets/{id}/attachments` | Bearer Token / Admin Context | List attachments with 1-hour presigned S3 URLs. |
| `POST` | `/api/v1/tickets/{id}/attachments` | Bearer Token / Admin Context | Upload attachments directly to S3 bucket `rumluos-app`. |
| `DELETE` | `/api/v1/tickets/{id}/attachments/{id}` | Bearer Token / Admin Context | Soft delete attachment metadata. |
| `GET` | `/api/v1/tickets/{id}/comments` | Bearer Token / Admin Context | List public replies & internal notes with pagination. |
| `POST` | `/api/v1/tickets/{id}/comments` | Bearer Token / Admin Context | Add comment (supports `is_internal = true` and file attachments). |
| `PUT` | `/api/v1/tickets/{id}/comments/{id}` | Bearer Token / Admin Context | Edit comment body. |
| `DELETE` | `/api/v1/tickets/{id}/comments/{id}` | Bearer Token / Admin Context | Soft delete comment. |
| `GET` | `/api/v1/api-clients` | Bearer Token / Admin Context | List external API client credentials. |
| `POST` | `/api/v1/api-clients` | Bearer Token / Admin Context | Create API client; returns plain secret once. |
| `PATCH` | `/api/v1/api-clients/{id}/status` | Bearer Token / Admin Context | Toggle active state. |
| `POST` | `/api/v1/api-clients/{id}/regenerate-secret` | Bearer Token / Admin Context | Rotate secret key. |
| `POST` | `/api/v1/tickets/telegram/invite-link` | Bearer Token / Admin Context | Generates 7-day single-use deep-link: `https://t.me/menglang6969Bot?start=TOKEN`. |
| `GET` | `/api/v1/tickets/telegram/counts` | Bearer Token / Admin Context | Returns `{ totalSubscribers, activeSubscribers, totalTelegramInvites }`. |
| `GET` | `/api/v1/tickets/telegram/subscribers` | Bearer Token / Admin Context | Paginated list of subscribed staff Telegram chat IDs. |
| `PATCH` | `/api/v1/tickets/telegram/subscribers/{chatId}/status` | Bearer Token / Admin Context | Toggle notification mute status. |
| `POST` | `/api/v1/tickets/telegram/subscribers/{chatId}/test` | Bearer Token / Admin Context | Dispatches direct test message to subscriber. |
| `POST` | `/api/v1/telegram/webhook` | `X-Telegram-Bot-Api-Secret-Token` | Handles `/start <token>` onboarding and `/status`. |

---

## 6. Dependencies (`ticket/build.gradle`)

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.4.0'
    id 'io.spring.dependency-management' version '1.1.6'
}

group = 'com.menglang'
version = '0.0.1-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-webflux'
    implementation 'org.springframework.boot:spring-boot-starter-data-mongodb-reactive'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'

    // AWS SDK v2 Async S3 & Presigner
    implementation platform('software.amazon.awssdk:bom:2.29.30')
    implementation 'software.amazon.awssdk:s3'
    implementation 'software.amazon.awssdk:s3-presigner'
    implementation 'software.amazon.awssdk:netty-nio-client'

    // Resilience4j for WebClient Circuit Breaker
    implementation 'io.github.resilience4j:resilience4j-spring-boot3:2.2.0'
    implementation 'io.github.resilience4j:resilience4j-reactor:2.2.0'

    // Boilerplate & Serialization
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    implementation 'com.fasterxml.jackson.datatype:jackson-datatype-jsr310'

    // Testing
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'io.projectreactor:reactor-test'
    testImplementation 'de.flapdoodle.embed:de.flapdoodle.embed.mongo.spring3x:4.12.2'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

---

## 7. Step 2 Advanced Scaling Features (Roadmap)
1. **Server-Sent Events (SSE) Live Feed**: Streaming live ticket mutations directly to the dashboard via MongoDB Change Streams (`GET /api/v1/tickets/stream`).
2. **Distributed Redis Rate Limiter**: Non-blocking token-bucket throttling via Reactive Redis.
3. **Kafka / RabbitMQ Outbox Bus**: Upgrading the outbox poller to a high-throughput event streaming bus.
4. **Direct-to-S3 Presigned Uploads**: Allowing clients to stream files straight to S3 without buffering through the API server.
5. **Dynamic SLA Escalation Engine**: Proactively alerting managers on Telegram before a critical ticket breaches its SLA deadline.
6. **Interactive Telegram Actions**: Inline bot buttons (`Claim`, `Resolve`) allowing staff to act on issues directly in Telegram.
7. **AI Copilot Triage**: Spring AI integration for automated categorization, priority recommendations, and draft replies.
