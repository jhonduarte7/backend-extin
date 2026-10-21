# AGENTS.md - Backend Extin Architecture & Guidelines

## 1. Overview
This project is the enterprise backend service for **EXTIN** (Fire Safety Management, Multi-Currency Invoicing, Document RAG, and AI Assistant), built with **Spring Boot 4.1.x** and **Java 25 (LTS)** following Specification-Driven Development (SDD) and clean layered architecture principles.

## 2. Technology Stack & Standards
- **Runtime**: Java 25 (Amazon Corretto / OpenJDK 25)
- **Framework**: Spring Boot 4.1.x
- **Build Tool**: Apache Maven (via `./mvnw` / `mvnw.cmd` wrapper)
- **Persistence**: Spring Data JPA + Hibernate 6.x / 7.x
- **Database**: PostgreSQL 16 with `pgvector` extension support
- **Artificial Intelligence & RAG**: Spring AI 2.0.x
  - **LLM Engine**: Ollama (model: `llama3.1`)
  - **Embeddings**: Ollama (model: `all-minilm`, 384 dimensions)
  - **Vector Store**: `PgVectorStore` (HNSW indexing, Cosine Distance)
  - **Document Reader**: Apache PDFBox (`PagePdfDocumentReader`, `TokenTextSplitter`)
  - **Model Context Protocol (MCP)**: Tool integration (`CatalogTools`, `CustomerTools`, `DocumentTools`) and `ChatMemory`
- **Security**: Spring Security 6.x + JJWT (io.jsonwebtoken 0.12.6)
  - **Stateless Authentication**: JWT delivered securely via `HttpOnly`, `SameSite`, `Path=/` cookies
  - **Role-Based Access Control**: `ADMIN` and `USER` roles
- **Language Convention**:
  - Source code, entity mappings, identifiers, commits, and comments: **English**
  - Developer explanations, progress updates, and user communication: **Spanish**

## 3. Package Structure
```
com.extin.backend
├── BackendExtinApplication.java           # Spring Boot bootstrap & async activation
├── config/                                # Configuration beans
│   ├── AuditorAwareImpl.java              # JPA AuditorAware resolving authenticated principal
│   ├── DatabaseSeeder.java                # Mock data seeder (50 customers, 20 items, equipment, quotes, invoices)
│   ├── DataInitializer.java               # Baseline catalog vectorization and admin account init
│   └── JpaConfig.java                     # JPA Auditing and repository scanning configuration
├── controllers/                           # REST API Controllers (Public, Admin, Auth, Chat)
│   ├── AdminChatController.java           # Admin AI Chat assistant endpoint (/api/admin/chat)
│   ├── AdminCustomerController.java       # Customer management & search (/api/admin/customers)
│   ├── AdminQuoteController.java          # Admin quote management (/api/admin/quotes)
│   ├── AuthController.java                # Authentication, cookie-based login & logout (/api/auth)
│   ├── DocumentController.java            # PDF Manuals & Regulations upload & RAG vectorization (/api/admin/documents)
│   ├── InvoiceController.java             # Invoicing, pay-and-issue, fiscal issuance (/api/admin/invoices)
│   ├── McpChatController.java             # MCP AI Assistant endpoint with tools & memory (/api/admin/chat/mcp)
│   ├── PublicCatalogController.java       # Public fire safety catalog (/api/public/catalog)
│   └── PublicQuoteController.java         # Public quote creation & retrieval (/api/public/quotes)
├── events/                                # Internal Spring Application Events
│   └── EntityVectorizationEvent.java      # Async event triggered on entity persistence/update
├── exceptions/                            # Error handling & custom exceptions
│   ├── GlobalExceptionHandler.java        # Centralized REST exception handler (RFC 7807 compliant)
│   └── ResourceNotFoundException.java     # HTTP 404 entity not found exception
├── mappers/                               # Explicit Entity <-> DTO mappers
│   ├── CatalogItemMapper.java
│   ├── CustomerMapper.java
│   ├── InvoiceMapper.java
│   └── QuoteMapper.java
├── mcp/                                   # Model Context Protocol (MCP) & Spring AI Tools
│   ├── client/
│   │   └── config/
│   │       └── McpClientConfig.java       # ChatMemory buffer bean configuration
│   └── server/
│       └── tools/
│           ├── CatalogTools.java          # Tool: semanticProductSearch
│           ├── CustomerTools.java         # Tool: getCustomerHistory
│           └── DocumentTools.java         # Tool: searchInManuals (domain='DOCUMENT')
├── models/                                # Domain Entities, DTO records, and Enums
│   ├── dtos/                              # Immutable DTO records matching API contracts exactly
│   │   ├── CatalogItemDto.java
│   │   ├── ChatRequestDto.java
│   │   ├── ChatResponseDto.java
│   │   ├── CustomerInfoDto.java
│   │   ├── CustomerResponseDto.java
│   │   ├── DocumentUploadResponseDto.java
│   │   ├── ErrorResponseDto.java
│   │   ├── InvoiceResponseDto.java
│   │   ├── LoginRequestDto.java
│   │   ├── LoginResponseDto.java
│   │   ├── PayAndIssueInvoiceRequestDto.java
│   │   ├── QuoteItemRequestDto.java
│   │   ├── QuoteRequestDto.java
│   │   └── QuoteResponseDto.java
│   ├── entities/                          # JPA Entities with Auditing
│   │   ├── Auditory.java                  # MappedSuperclass with @CreatedDate, @LastModifiedBy, etc.
│   │   ├── CatalogItem.java               # Fire safety equipment and maintenance services
│   │   ├── CatalogItemEntityListener.java # PostPersist/PostUpdate listener triggering vectorization
│   │   ├── Customer.java                  # Fiscal customer records (RIF / CNPJ)
│   │   ├── CustomerEquipment.java         # Physical customer equipment tracking & expiration
│   │   ├── Invoice.java                   # Immutable fiscal invoices
│   │   ├── InvoiceDetail.java             # Invoice line items with multi-currency snapshots
│   │   ├── Quote.java                     # Commercial quotes
│   │   ├── QuoteDetail.java               # Quote line items
│   │   └── User.java                      # Spring Security UserDetails entity
│   └── enums/                             # Domain Enums
│       ├── InvoiceStatus.java             # ISSUED, PAID, CANCELLED
│       ├── ItemType.java                  # PRODUCT, SERVICE
│       ├── QuoteStatus.java               # PENDING, APPROVED, REJECTED, INVOICED
│       └── Role.java                      # ADMIN, USER
├── repositories/                          # Spring Data JPA Repository Interfaces
│   ├── CatalogItemRepository.java
│   ├── CustomerEquipmentRepository.java
│   ├── CustomerRepository.java
│   ├── InvoiceDetailRepository.java
│   ├── InvoiceRepository.java
│   ├── QuoteDetailRepository.java
│   ├── QuoteRepository.java
│   └── UserRepository.java
├── security/                              # Spring Security & JWT components
│   ├── JwtAuthenticationFilter.java       # OncePerRequestFilter reading HttpOnly jwt_token cookie
│   ├── JwtService.java                    # HMAC-SHA256 JWT builder, parser, and validator
│   ├── SecurityBeansConfig.java           # BCryptPasswordEncoder & AuthenticationManager beans
│   ├── SecurityConfig.java                # Stateless SecurityFilterChain, CORS, and endpoint rules
│   └── UserDetailsServiceImpl.java        # UserDetailsService bridge with UserRepository
└── services/                              # Domain Services & Business Logic Orchestration
    ├── CatalogService.java                # Catalog query & pagination
    ├── ChatService.java                   # AI Assistant orchestration, system prompt & ChatClient
    ├── CurrencyExchangeService.java       # Multi-currency exchange rate engine (USD, VES, BRL)
    ├── CustomerService.java               # Customer search & registration
    ├── DocumentRagService.java            # PDF ingestion, chunking, and pgvector storage
    ├── InvoiceService.java                # Invoicing, settlement, and fiscal control numbers
    ├── QuoteService.java                  # Multi-currency cart calculation & quote persistence
    └── VectorizationService.java          # Real-time async vectorization into pgvector
```

## 4. API Contracts & Endpoints

### 4.1 Authentication (`/api/auth`)
*   `POST /api/auth/login`: Authenticates username and password; attaches secure `HttpOnly` cookie `jwt_token`.
*   `POST /api/auth/logout`: Clears the authentication cookie (`Max-Age=0`).
*   `GET /api/auth/me`: Returns identity and role of currently authenticated user.

### 4.2 Public Endpoints (`/api/public`)
*   `GET /api/public/catalog`: Returns paginated list of active products and services with base USD pricing (default 10 items/page).
*   `POST /api/public/quotes`: Accepts cart items, computes line subtotals in USD, applies exchange rate (`VES` = 36.50, `BRL` = 5.50, `USD` = 1.00), persists quote and details, returns JSON response.
*   `GET /api/public/quotes/{id}`: Returns details of a specific quote.

### 4.3 Admin Commercial & Fiscal Endpoints (`/api/admin`)
*   `GET /api/admin/quotes`: Returns paginated quotes with optional `status` filter (e.g. `APPROVED`, `PENDING`, `INVOICED`).
*   `POST /api/admin/invoices/pay-and-issue/{quote_id}`: Issues immutable paid invoice upon payment notification, computing exact local amount (`total_amount_usd * exchange_rate`), recording `PAID` status, `rate_date`, and `payment_reference`.
*   `POST /api/admin/invoices/from-quote/{quote_id}`: Converts approved quote into immutable fiscal invoice with unique SENIAT control number.
*   `GET /api/admin/invoices`: Returns paginated list of all invoices.
*   `GET /api/admin/invoices/{id}`: Returns single invoice details.
*   `GET /api/admin/customers`: Returns paginated list of customers (default 10 items/page) with search query.

### 4.4 Artificial Intelligence & Document RAG Endpoints
*   `POST /api/admin/documents/upload`: Multipart upload of PDF manuals or safety standards. Purges previous vectors for the category, chunks content via `TokenTextSplitter`, and embeds into `pgvector` under `domain = 'DOCUMENT'`.
*   `POST /api/admin/chat`: AI Assistant endpoint processing prompts and executing domain tools.
*   `POST /api/admin/chat/mcp`: Advanced conversational MCP AI Assistant endpoint supporting conversation sessions (`X-Chat-Id`), ChatMemory buffer, and execution of tools:
    - `semanticProductSearch`: Semantic search across catalog equipment and services.
    - `getCustomerHistory`: Exact historical equipment and fiscal data lookup by RIF/CNPJ.
    - `searchInManuals`: Semantic retrieval across uploaded safety manuals and fire codes (NFPA/COVENIN).

## 5. Architectural Subsystems

### 5.1 Multi-Currency Engine
*   **Base Currency**: `USD` (Internal standard of record).
*   **Target Currencies**: `VES` (Venezuelan Bolívares - BCV official benchmark) and `BRL` (Brazilian Reais).
*   **Formulas**:
    - `total_amount_usd` = $\sum (\text{item.basePrice} \times \text{quantity})$
    - `total_amount_local` = $\text{total\_amount\_usd} \times \text{exchange\_rate}$
*   Line item details lock unit prices and converted values at quote and invoice creation time to ensure immutability.

### 5.2 Real-Time Asynchronous Auto-Vectorization
1. When a `CatalogItem` is created or modified, `CatalogItemEntityListener` intercepts the `@PostPersist` or `@PostUpdate` event.
2. An `EntityVectorizationEvent` is published into Spring's `ApplicationEventPublisher`.
3. `VectorizationService` receives the event asynchronously via `@Async` and `@EventListener`, generates vector embeddings via Ollama, and persists them into `pgvector` without blocking the database transaction.

### 5.3 Document RAG (Retrieval-Augmented Generation)
*   PDF files uploaded to `/api/admin/documents/upload` are processed by Apache PDFBox `PagePdfDocumentReader`.
*   Extracted text is partitioned into overlapping semantic chunks via `TokenTextSplitter`.
*   Chunks are tagged with metadata: `domain = 'DOCUMENT'`, `category`, and `file_name`.
*   When queried via `searchInManuals`, similarity searches strictly filter by `domain == 'DOCUMENT'`, preventing collision with catalog item embeddings.

### 5.4 Security Architecture
*   Authentication is 100% stateless.
*   Upon login, a signed JWT (HMAC-SHA256) is returned inside a secured `HttpOnly` cookie named `jwt_token`.
*   `JwtAuthenticationFilter` validates the cookie on each incoming request and establishes the Spring Security context.
*   Endpoints are protected with route-based authorization:
    - Public: `/api/auth/**`, `/api/public/**`
    - User/Admin: `/api/admin/chat/**`
    - Admin-only: `/api/admin/**`

### 5.5 JPA Auditing
All persistent entities inherit from `Auditory`, providing automated tracking for:
- `created_by` (email or username of the creator via `AuditorAwareImpl`)
- `last_modified_by` (email or username of the updater)
- `created_at` (immutable timestamp)
- `updated_at` (modification timestamp)

## 6. Build, Testing & Execution
*   **Start Database**: `docker compose up -d`
*   **Run Test Suite (72 tests)**: `./mvnw test`
*   **Run Spring Boot Application**: `./mvnw spring-boot:run`
