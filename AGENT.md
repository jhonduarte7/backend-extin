# AGENTS.md — Engineering & Architectural Guide for AI Coding Agents

> **Repository:** `backend-extin`  
> **Framework:** Spring Boot 4.1.x + Spring AI 2.0.x + Spring Data JPA  
> **Runtime:** Java 21 (LTS) & Java 25 compatible  
> **Persistence:** PostgreSQL 16 + `pgvector`  
> **LLM Engine:** Ollama (`llama3.1:latest`, `all-minilm:latest`)  
> **Methodology:** Specification-Driven Development (SDD)

---

## 1. Mission & Scope

This document sets the mandatory engineering standards, architectural conventions, and development guidelines for any AI coding agent modifying, extending, or maintaining the `backend-extin` codebase.

Every change must strictly align with the specifications documented in `docs/system-spec.md` and `docs/api-contracts.md`. **Do not invent arbitrary patterns or unneeded dependencies.**

---

## 2. Core Architectural Principles

### 2.1 Layered Clean Architecture & Separation of Concerns
1. **Controllers (`com.extin.backend.controllers`):**
   - Strictly responsible for HTTP parameter extraction, `@Valid` payload validation, content negotiation, and routing.
   - Never inject `EntityManager`, `JdbcTemplate`, or execute business logic directly in controllers.
   - Delegate all orchestration to the corresponding domain service.
2. **Services (`com.extin.backend.services`):**
   - Encapsulate business logic, transactional boundaries (`@Transactional`), multi-currency computations, and AI prompt orchestration.
   - Return clean domain objects or DTOs.
3. **Mappers (`com.extin.backend.mappers`):**
   - Perform explicit conversions between JPA entities and immutable DTO records (`CatalogItemMapper`, `CustomerMapper`, `InvoiceMapper`, `QuoteMapper`).
   - Avoid reflection-based mapping libraries (e.g., ModelMapper) to maintain high performance and explicit typing.
4. **Data Transfer Objects (`com.extin.backend.models.dtos`):**
   - MUST be implemented as Java `record`s to ensure immutability and concise JSON serialization.
   - Match field names in `docs/api-contracts.md` exactly.
5. **JPA Entities (`com.extin.backend.models.entities`):**
   - All persistent entities MUST inherit from `Auditory` (`@MappedSuperclass`) to automatically record `created_by`, `last_modified_by`, `created_at`, and `updated_at`.
   - Never expose entities directly in controller return signatures; always convert to DTOs.

---

## 3. Artificial Intelligence & Spring AI Standards

### 3.1 Reactive Streaming Chat (SSE)
- The primary AI endpoint is `POST /api/admin/chat/mcp` returning `Flux<String>` with `produces = {MediaType.TEXT_EVENT_STREAM_VALUE, MediaType.TEXT_PLAIN_VALUE}`.
- Invokes `chatClient.prompt().system(SYSTEM_PROMPT).user(userMessage).advisors(...).toolCallbacks(uniqueToolCallbacks).stream().content()`.
- Backward compatibility: Retain content negotiation for `application/json` returning `ChatResponseDto` for legacy clients and integration tests.

### 3.2 Tool Calling & Deduplication
- Tools are declared in `com.extin.backend.mcp.server.tools`:
  - `CatalogTools.java`: `semanticProductSearch(query)`
  - `CustomerTools.java`: `getCustomerHistory(taxId)`
  - `DocumentTools.java`: `searchInManuals(query)`
- **Mandatory Rule:** Tools MUST be resolved and deduplicated through `ChatService.resolveUniqueToolCallbacks(Object... toolBeans)`. Never register duplicate tool names or call `.defaultTools()` on the global `ChatClient.Builder`.
- Keep tool responses bounded (e.g. max 4 items, max 350-700 characters per chunk) to protect the context window of local LLMs like Llama 3.1.

### 3.3 HTTP Timeouts & Resilience
- Timeouts between Spring Boot and local Ollama are configured to `120s` (connect) and `180s` (read) in `application.yml` and customizer beans (`RestClientCustomizer`, `WebClientCustomizer` in `McpClientConfig`).
- All streaming and synchronous chat methods MUST catch exceptions and wrap them via `buildContingencyResponse(Throwable e)`:
  - If a timeout or connection drop occurs, return a structured Markdown explanation ("Modo Contingencia").
  - NEVER throw unhandled 500 exceptions to the frontend for LLM inference timeouts.

### 3.4 Document RAG & Vector Isolation
- Chunks vectorized from uploaded PDF files MUST include metadata: `{"domain": "DOCUMENT", "category": category}`.
- Re-uploading a document category must first purge previous vector chunks:
  `DELETE FROM vector_store WHERE metadata->>'domain' = 'DOCUMENT' AND metadata->>'category' = ?`.
- Vector similarity searches in `DocumentTools` must strictly filter by `domain == 'DOCUMENT'` to prevent pollution from catalog items.

---

## 4. Business Domain Rules

### 4.1 Multi-Currency Engine
- **Base Currency:** `USD`.
- **Target Currencies:** `VES` (Venezuelan Bolívares via official BCV benchmark) and `BRL` (Brazilian Reais).
- Form calculations:
  - `total_amount_local = total_amount_usd * exchange_rate`.
  - Line items must capture both USD unit prices and converted unit prices at the time of creation to ensure historical immutability.

### 4.2 Dual Tax Modality
- Support both `appliesIva = true` (16% SENIAT tax calculation) and `appliesIva = false` (Contado / simple delivery note).
- Maintain tax fields across `Quote`, `QuoteDetail`, `Invoice`, and `InvoiceDetail`: `subtotal`, `taxRate`, `taxAmount`, and `total`.

### 4.3 Unified Global Pagination
- All administrative list endpoints must support Spring Data `Pageable` with a strict default page size of 5 records:
  `@PageableDefault(size = 5) Pageable pageable`.
- Standardize response wrappers using Spring Data `Page<T>` or mapped `Page<DTO>`.

---

## 5. Security & Stateless Authentication

- **Stateless Sessions:** `SessionCreationPolicy.STATELESS` in `SecurityConfig`.
- **Cookie Authentication:** JWT tokens are transported exclusively via secure `HttpOnly` cookies named `jwt_token`.
- **Filter Order:** `JwtAuthenticationFilter` runs before `UsernamePasswordAuthenticationFilter`.
- **Role Isolation:**
  - Public: `/api/auth/login`, `/api/auth/logout`, `/api/public/**`.
  - Authenticated (`ADMIN` or `USER`): `/api/auth/me`, `/api/admin/chat/**`.
  - Restricted (`ADMIN` only): `/api/admin/**`.

---

## 6. Testing, Build & CI/CD Standards

### 6.1 Testing Mandate
- Run the full test suite before any push:
  ```bash
  ./mvnw test
  ```
- All 90 unit and integration tests MUST pass with 0 failures and 0 errors (`BUILD SUCCESS`).
- Include tests for any new controller endpoint, service method, or tool calling behavior.

### 6.2 Build & Packaging
- The project is compiled targeting Java 21 bytecode compatibility:
  ```bash
  ./mvnw clean package -DskipTests
  ```
- Keep `mvnw` executable in git: `git update-index --chmod=+x mvnw`.

### 6.3 GitHub Actions Workflow
- Defined in `.github/workflows/backend-ci.yml`.
- Runs automatically on `push` and `pull_request` to `main`.
- Uses Ubuntu runner, Temurin JDK 21, and Maven caching.

---

## 7. Commit Message Standards

All Git commit messages MUST follow the [Conventional Commits](https://www.conventionalcommits.org/) specification in English:

- `feat(scope): add new feature or endpoint`
- `fix(scope): resolve bug, timeout or calculation error`
- `refactor(scope): internal code cleanup or architecture alignment`
- `docs(scope): update documentation or specifications`
- `test(scope): add or modify test coverage`
- `ci(scope): update GitHub Actions workflow or build scripts`
