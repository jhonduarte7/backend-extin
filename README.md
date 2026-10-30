# EXTIN — Fire Safety Management, Invoicing & AI Backend

[![Java](https://img.shields.io/badge/Java-21%20%7C%2025-orange.svg)](https://openjdk.org/projects/jdk/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-2.0.x-blue.svg)](https://spring.io/projects/spring-ai)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16%20%2B%20pgvector-336791.svg)](https://github.com/pgvector/pgvector)
[![CI](https://img.shields.io/badge/CI-GitHub%20Actions-2088FF.svg)](https://github.com/features/actions)
[![License](https://img.shields.io/badge/License-Proprietary-red.svg)]()

Enterprise backend service for **EXTIN / Equi-Extin**, an integrated platform for industrial fire protection equipment manufacturing, maintenance tracking, multi-currency commercial quoting, SENIAT physical slip replication, document RAG (Retrieval-Augmented Generation), and an AI Copilot powered by Spring AI, Ollama (`llama3.1`), and the Model Context Protocol (MCP).

---

## Table of Contents

- [1. Business Domain & Context](#1-business-domain--context)
- [2. System Architecture](#2-system-architecture)
  - [2.1 Architectural Blueprint](#21-architectural-blueprint)
  - [2.2 Layered Design Pattern](#22-layered-design-pattern)
  - [2.3 Multi-Currency Engine](#23-multi-currency-engine)
  - [2.4 Dual Tax Modality (Con IVA vs. Sin IVA)](#24-dual-tax-modality-con-iva-vs-sin-iva)
  - [2.5 Real-Time Asynchronous Auto-Vectorization](#25-real-time-asynchronous-auto-vectorization)
  - [2.6 Document RAG & Domain Isolation](#26-document-rag--domain-isolation)
  - [2.7 Reactive Streaming AI Assistant (SSE) & MCP Tools](#27-reactive-streaming-ai-assistant-sse--mcp-tools)
  - [2.8 Stateless Security & HttpOnly Cookies](#28-stateless-security--httponly-cookies)
  - [2.9 Unified Global Pagination](#29-unified-global-pagination)
- [3. Technology Stack](#3-technology-stack)
- [4. Project Structure](#4-project-structure)
- [5. API Reference](#5-api-reference)
  - [5.1 Authentication Endpoints](#51-authentication-endpoints)
  - [5.2 Public Web Endpoints](#52-public-web-endpoints)
  - [5.3 Commercial & Invoicing Endpoints](#53-commercial--invoicing-endpoints)
  - [5.4 Customer & Equipment Endpoints](#54-customer--equipment-endpoints)
  - [5.5 Document RAG & AI Assistant Endpoints](#55-document-rag--ai-assistant-endpoints)
- [6. Continuous Integration (CI/CD)](#6-continuous-integration-cicd)
- [7. Database Model & Seeding](#7-database-model--seeding)
- [8. Getting Started](#8-getting-started)
  - [8.1 Prerequisites](#81-prerequisites)
  - [8.2 Infrastructure Setup (Docker & Ollama)](#82-infrastructure-setup-docker--ollama)
  - [8.3 Running the Application](#83-running-the-application)
  - [8.4 Running Automated Tests (90 Tests)](#84-running-automated-tests-90-tests)

---

## 1. Business Domain & Context

**EXTIN** specializes in the distribution, assembly, recharging, hydro-static testing, and technical certification of industrial fire protection equipment across Venezuela and regional South American corridors. The system solves key industrial operational challenges:

1. **Equipment & Workshop Fleet Tracking**:
   - Manages physical equipment installed at customer premises (PQS, $CO_2$, Solkaflam, Water extinguishers, fire hose cabinets, valves, smoke detectors).
   - Tracks 5-year and 12-year hydro-static testing (PH) schedules, alerting workshop managers before equipment certifications expire.
2. **Multi-Currency Lifecycle & Physical Invoicing**:
   - Uses **USD** as the internal standard of record.
   - Calculates real-time conversions to **VES** (Venezuelan Bolívares via official BCV rates) and **BRL** (Brazilian Reais).
   - Supports dual tax modes matching Venezuelan commercial practice: **Con IVA** (16% tax breakdown) and **Sin IVA** (Contado / direct workshop delivery slips).
   - Converts shopping cart quotes into immutable invoices with fiscal control serial numbers (`00-00418X`).
3. **Automated AI Copilot & Document RAG**:
   - Indexes official safety regulations (NFPA 10, COVENIN 1040) in `pgvector`.
   - Provides real-time reactive streaming AI assistance for workshop technicians and administrative personnel.

---

## 2. System Architecture

### 2.1 Architectural Blueprint

```
+-----------------------------------------------------------------------------------+
|                                  CLIENT LAYER                                     |
|           React 19 Frontend (Vite) / Postman / Laser & Receipt Printers           |
+-----------------------------------------------------------------------------------+
                                        | (HTTPS / HttpOnly Cookie jwt_token)
                                        v
+-----------------------------------------------------------------------------------+
|                                 SECURITY LAYER                                    |
|          JwtAuthenticationFilter -> SecurityConfig -> Role-Based Access           |
+-----------------------------------------------------------------------------------+
                                        |
       +--------------------------------+-------------------------------+
       |                                |                               |
       v                                v                               v
+--------------+               +------------------+             +---------------+
| REST API     |               | REACTIVE CHAT    |             | DOCUMENT RAG  |
| Controllers  |               | McpChatController|             | Controller    |
+--------------+               +------------------+             +---------------+
       |                                | (SSE Stream Flux<String>)     |
       v                                v                               v
+--------------+               +------------------+             +---------------+
| Domain       |               | Spring AI        |             | PDF Reader    |
| Services     | <-----------> | ChatService      | <---------> | TokenSplitter |
+--------------+  MCP Tools    +------------------+             +---------------+
       |                                |                               |
       v                                v                               v
+--------------+               +------------------+             +---------------+
| JPA Repos    |               | Ollama LLM       |             | PostgreSQL    |
| & Entities   |               | llama3.1:latest  |             | pgvector (HNSW|
+--------------+               +------------------+             +---------------+
```

### 2.2 Layered Design Pattern
- **Controllers:** Expose RESTful endpoints with strict JSON input validation (`@Valid`).
- **Services:** Encapsulate business logic, transactional boundaries (`@Transactional`), multi-currency calculations, and AI prompt orchestration.
- **Mappers:** Explicit record-to-entity and entity-to-DTO converters without reflection overhead.
- **Models & DTOs:** Immutable Java `record`s for all API contracts; JPA `@Entity` classes inheriting from `Auditory` for automatic change tracking.
- **MCP Tools:** Domain tools (`CatalogTools`, `CustomerTools`, `DocumentTools`) annotated with `@Tool` for native Spring AI tool calling.

### 2.3 Multi-Currency Engine
- **Base Currency**: `USD` (Internal standard of record).
- **Target Currencies**: `VES` (BCV official rate) and `BRL` (Brazilian Reais).
- Subtotals, tax amounts, and totals are computed and stored with immutable financial snapshots in line items (`QuoteDetail`, `InvoiceDetail`).

### 2.4 Dual Tax Modality (Con IVA vs. Sin IVA)
Quotes and Invoices support dual taxation:
- **`appliesIva = true`:** `subtotal = base_price * quantity`, `taxRate = 0.16`, `taxAmount = subtotal * 0.16`, `total = subtotal + taxAmount`.
- **`appliesIva = false`:** `subtotal = total`, `taxRate = 0.00`, `taxAmount = 0.00` (Contado / simple delivery note).

### 2.5 Real-Time Asynchronous Auto-Vectorization
1. When a `CatalogItem` is created or modified, `CatalogItemEntityListener` intercepts the `@PostPersist` or `@PostUpdate` lifecycle hook.
2. Publishes an `EntityVectorizationEvent`.
3. `VectorizationService` receives the event asynchronously (`@Async`), generates vector embeddings via Ollama (`all-minilm`), and persists them into `pgvector` without locking the main thread.

### 2.6 Document RAG & Domain Isolation
- PDF manuals and standards uploaded to `/api/admin/documents/upload` are partitioned into overlapping chunks by `TokenTextSplitter`.
- Chunks are stored in `pgvector` with mandatory metadata: `{"domain": "DOCUMENT", "category": category}`.
- Re-uploading a category automatically purges obsolete chunks (`DELETE WHERE metadata->>'domain' = 'DOCUMENT' AND metadata->>'category' = ?`).
- Document similarity queries strictly filter by `domain == 'DOCUMENT'`, preventing collision with catalog vectors.

### 2.7 Reactive Streaming AI Assistant (SSE) & MCP Tools
- **Endpoint:** `POST /api/admin/chat/mcp` returns `Flux<String>` with `text/event-stream`.
- **Tool Calling:** Llama 3.1 dynamically invokes:
  - `CatalogTools.semanticProductSearch`: Vector search in equipment catalog.
  - `CustomerTools.getCustomerHistory`: Customer records and hydro-test expiration data by RIF/CNPJ.
  - `DocumentTools.searchInManuals`: Normative safety standards (NFPA 10, COVENIN).
- **Resilience:** HTTP client timeouts configured to 120s/180s. Catches timeouts or network drops gracefully via `buildContingencyResponse(Throwable e)` returning structured Markdown without throwing HTTP 500.

### 2.8 Stateless Security & HttpOnly Cookies
- Stateless session policy (`SessionCreationPolicy.STATELESS`).
- Authentication via `POST /api/auth/login` sets an `HttpOnly`, `Path=/`, `SameSite=Lax` cookie named `jwt_token`.
- `JwtAuthenticationFilter` validates token integrity and role claims (`ADMIN`, `USER`) on each request.

### 2.9 Unified Global Pagination
All administrative listing endpoints enforce standard 5-items-per-page pagination using Spring Data's `@PageableDefault(size = 5)`:
- `GET /api/admin/quotes`
- `GET /api/admin/invoices`
- `GET /api/admin/customers`
- `GET /api/public/catalog`

---

## 3. Technology Stack

| Layer | Technology | Details |
|---|---|---|
| **Language & Runtime** | Java 21 / 25 | Amazon Corretto / Eclipse Temurin |
| **Framework** | Spring Boot 4.1.x | Spring MVC, Spring Data JPA, Spring Security, Spring AI |
| **Build Tool** | Apache Maven 3.10+ | Maven Wrapper (`./mvnw`) with script-only distribution |
| **Database** | PostgreSQL 16 + pgvector | Port `5433`, vector similarity indexing (HNSW, Cosine) |
| **AI / LLM Runtime** | Ollama | `llama3.1:latest` (8B) for chat, `all-minilm:latest` for embeddings |
| **Document Processing** | Apache PDFBox | `PagePdfDocumentReader` + `TokenTextSplitter` |
| **Security & JWT** | JJWT (io.jsonwebtoken) 0.12.6 | HMAC-SHA512 token signing with HttpOnly cookie transport |
| **CI/CD** | GitHub Actions | Automated build, JDK 21 setup, and packaging verification |

---

## 4. Project Structure

```text
backend-extin/
├── .github/
│   └── workflows/
│       └── backend-ci.yml                 # GitHub Actions CI pipeline
├── .mvn/wrapper/
│   └── maven-wrapper.properties           # Maven wrapper configuration
├── src/
│   ├── main/
│   │   ├── java/com/extin/backend/
│   │   │   ├── config/                    # Seeder, JPA Auditing, Data Initializer
│   │   │   │   ├── AuditorAwareImpl.java
│   │   │   │   ├── DatabaseSeeder.java
│   │   │   │   ├── DataInitializer.java
│   │   │   │   └── JpaConfig.java
│   │   │   ├── controllers/               # REST API Controllers
│   │   │   │   ├── AdminChatController.java
│   │   │   │   ├── AdminCustomerController.java
│   │   │   │   ├── AdminQuoteController.java
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── DocumentController.java
│   │   │   │   ├── InvoiceController.java
│   │   │   │   ├── McpChatController.java
│   │   │   │   ├── PublicCatalogController.java
│   │   │   │   └── PublicQuoteController.java
│   │   │   ├── events/                    # Application Events
│   │   │   │   └── EntityVectorizationEvent.java
│   │   │   ├── exceptions/                # Exception Handling
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   └── ResourceNotFoundException.java
│   │   │   ├── mappers/                   # DTO <-> Entity Mappers
│   │   │   │   ├── CatalogItemMapper.java
│   │   │   │   ├── CustomerMapper.java
│   │   │   │   ├── InvoiceMapper.java
│   │   │   │   └── QuoteMapper.java
│   │   │   ├── mcp/                       # Model Context Protocol & Tools
│   │   │   │   ├── client/config/
│   │   │   │   │   └── McpClientConfig.java
│   │   │   │   └── server/tools/
│   │   │   │       ├── CatalogTools.java
│   │   │   │       ├── CustomerTools.java
│   │   │   │       └── DocumentTools.java
│   │   │   ├── models/
│   │   │   │   ├── dtos/                  # Immutable DTO Records
│   │   │   │   ├── entities/              # JPA Entities (Quote, Invoice, Customer, etc.)
│   │   │   │   └── enums/                 # Role, QuoteStatus, InvoiceStatus, ItemType
│   │   │   ├── repositories/              # Spring Data JPA Repositories
│   │   │   ├── security/                  # Security Filter Chain, JWT Provider
│   │   │   └── services/                  # Business Logic & AI Services
│   │   └── resources/
│   │       └── application.yml            # Database, Security, Ollama, & Timeouts
│   └── test/                              # 90 Unit & Integration Tests
├── docker-compose.yml                     # PostgreSQL + pgvector container definition
├── mvnw / mvnw.cmd                        # Maven wrapper executables
├── pom.xml                                # Maven build definition
├── AGENTS.md                              # AI Agent Engineering Standards
└── README.md                              # Enterprise Documentation
```

---

## 5. API Reference

### 5.1 Authentication Endpoints (`/api/auth`)

| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/login` | Public | Authenticates credentials; sets secure `HttpOnly` cookie `jwt_token`. |
| `POST` | `/api/auth/logout` | Public | Clears `jwt_token` cookie (`Max-Age=0`). |
| `GET` | `/api/auth/me` | Authenticated | Returns current session identity and role. |

### 5.2 Public Web Endpoints (`/api/public`)

| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/public/catalog` | Public | Paginated catalog listing (`page`, `size=5`, `search`, `type`). |
| `POST` | `/api/public/quotes` | Public | Creates quote with customer details, delivery address, and tax mode. |
| `GET` | `/api/public/quotes/{id}` | Public | Retrieves specific quote details. |

### 5.3 Commercial & Invoicing Endpoints (`/api/admin`)

| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/admin/quotes` | `ADMIN` | Paginated quotes with optional `status` filter (`size=5`). |
| `POST` | `/api/admin/invoices/pay-and-issue/{quote_id}` | `ADMIN` | Issues immutable paid invoice with BCV rate and payment reference. |
| `POST` | `/api/admin/invoices/from-quote/{quote_id}` | `ADMIN` | Converts approved quote to invoice with fiscal control number. |
| `GET` | `/api/admin/invoices` | `ADMIN` | Paginated list of all invoices (`size=5`). |
| `GET` | `/api/admin/invoices/{id}` | `ADMIN` | Retrieves single invoice details with line items. |

### 5.4 Customer & Equipment Endpoints (`/api/admin/customers`)

| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/admin/customers` | `ADMIN` | Paginated customer search by name or Tax ID (`query`, `size=5`). |

### 5.5 Document RAG & AI Assistant Endpoints

| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/admin/documents/upload` | `ADMIN` | Multipart PDF upload. Chunks and indexes into `pgvector`. |
| `POST` | `/api/admin/chat/mcp` | `ADMIN`, `USER` | Reactive streaming AI assistant (SSE `Flux<String>`) with MCP tool calling. |
| `POST` | `/api/admin/chat` | `ADMIN` | Synchronous legacy JSON AI assistant endpoint. |

---

## 6. Continuous Integration (CI/CD)

The backend features an automated **GitHub Actions** workflow defined in [`.github/workflows/backend-ci.yml`](.github/workflows/backend-ci.yml):
- **Triggers:** Every `push` and `pull_request` targeting `main`.
- **Runner:** `ubuntu-latest`.
- **JDK:** OpenJDK 21 via `actions/setup-java@v4` (Eclipse Temurin) with Maven caching.
- **Verification:** Runs `./mvnw clean package -DskipTests` ensuring clean compilation and artifact packaging.

---

## 7. Database Model & Seeding

The application automatically seeds baseline data when tables are empty (`DatabaseSeeder` and `DataInitializer`):
- **50 Mock Customers:** 25 Venezuelan entities with RIF formats (`J-`, `V-`, `G-`) and 25 Brazilian entities with CNPJ.
- **Customer Equipment:** Cylinders tracked by serial number, including units with expired hydro-static tests.
- **20 Catalog Items:** 12 fire safety products (PQS, CO2, Solkaflam, Water) and 8 workshop recharge services.
- **Seed Users:**
  - `admin` (Password: `adminpassword`, Role: `ADMIN`)
  - `user` (Password: `userpassword`, Role: `USER`)

---

## 8. Getting Started

### 8.1 Prerequisites
- **Java 21 or 25 SDK** (Eclipse Temurin or Amazon Corretto).
- **Docker & Docker Compose**.
- **Ollama** installed locally (running on port `11434`).

### 8.2 Infrastructure Setup (Docker & Ollama)

1. **Start PostgreSQL with pgvector**:
   ```bash
   docker compose up -d
   ```
   *Initializes PostgreSQL 16 on port `5433` with database `fire_safety_db` and credentials `admin`/`adminpassword`.*

2. **Pull Ollama Models**:
   ```bash
   ollama pull llama3.1
   ollama pull all-minilm
   ```

### 8.3 Running the Application

Using the Maven Wrapper:

- **Linux / macOS**:
  ```bash
  ./mvnw spring-boot:run
  ```

- **Windows**:
  ```powershell
  .\mvnw.cmd spring-boot:run
  ```

The server initializes on port **`8090`** (configured in `src/main/resources/application.yml`).

### 8.4 Running Automated Tests (90 Tests)

Execute the comprehensive test suite validating security, controllers, multi-currency invoicing, streaming chat, and Document RAG:

```bash
./mvnw test
```
*Expected Result: 90 tests run, 0 failures, 0 errors, 100% passing (`BUILD SUCCESS`).*

---

## License
Proprietary software developed for Equi-Extin Fire Safety Management. All rights reserved.
