---
name: spring-boot-best-practices
description: Generate and maintain Spring Boot projects following a layered architecture with dedicated controllers, services, repositories, models, and mappers. Trigger this skill whenever the user asks to create a Spring Boot API, build a Spring Web monolithic application with Thymeleaf, scaffold or structure a Spring Boot project, or create, add, modify, or extend Spring entities, repositories, services, controllers, DTOs, or mappers.
---

# Spring Boot Best Practices

Follow this guide to scaffold, develop, and maintain Spring Boot applications following industry best practices and a strict layered architecture.

## 1. Project Initialization & Setup

### Bootstrap Options
- **Spring Initializr Web**: Use [start.spring.io](https://start.spring.io/) to generate or verify project metadata and dependencies.
- **cURL / CLI Generation**: When initializing from terminal:
  ```bash
  curl https://start.spring.io/starter.zip \
    -d type=maven-project \
    -d language=java \
    -d bootVersion=3.4.3 \
    -d javaVersion=25 \
    -d packaging=jar \
    -d groupId=com.venefast.springboot \
    -d artifactId=<workspace-dir-name> \
    -d name=<workspace-dir-name> \
    -d packageName=com.venefast.springboot.<sanitized_artifact_id>.app \
    -d dependencies=web,validation,data-jpa,h2,devtools,actuator \
    -o project.zip
  ```
  *(Note: Check [spring.io](https://spring.io) for the latest available Spring Boot version, using 4.1.0 or higher when released, and Java 25).*

### Configuration & Tooling Standards
- **Build Tool**: Maven with Maven Wrapper included (`.mvn/`, `./mvnw`, `mvnw.cmd`).
- **Packaging**: JAR packaging.
- **Java Version**: Java 25.
- **Configuration Format**: Always use `application.properties` (never YAML).
- **Group ID**: `com.venefast.springboot`
- **Artifact ID & Project Name**: Matches the workspace folder name (kebab-case or lowercase).
- **Base Package**: `com.venefast.springboot.{artifactId}.app` (sanitize hyphens/special characters for valid Java package identifiers, e.g. `backend_api` or `backendapi`).

### Base Dependencies
Include in `pom.xml`:
- `spring-boot-starter-web` (REST / Web MVC)
- `spring-boot-starter-validation` (Bean Validation / Hibernate Validator)
- `spring-boot-starter-data-jpa` (Persistence & Transactions)
- `com.h2database:h2` (Runtime/In-memory database for rapid dev/test)
- `spring-boot-devtools` (Hot reload & developer tooling, runtime/optional)
- `spring-boot-starter-actuator` (Health checks and operational metrics)
- *Optional for Server-Rendered UI*: `spring-boot-starter-thymeleaf`

---

## 2. Layered Architecture

Organize the application under `src/main/java/com/venefast/springboot/{artifactId}/app/` using these dedicated packages:

```
com.venefast.springboot.{artifactId}.app/
├── Application.java
├── controllers/
│   └── UserController.java
├── services/
│   ├── UserService.java
│   └── impl/
│       └── UserServiceImpl.java (or direct Service classes)
├── repositories/
│   └── UserRepository.java
├── models/
│   ├── entities/
│   │   └── User.java
│   └── dtos/
│       └── UserDto.java
└── mappers/
    └── UserMapper.java
```

### Layer Responsibilities & Rules

#### Repositories (`repositories/`)
- Define as Java interfaces extending `JpaRepository<Entity, ID>`.
- Use Spring Data derived query methods or `@Query` annotations for complex lookups.
- Never place business logic or direct API presentation logic inside repositories.

#### Models: Entities (`models/entities/`)
- Standard JPA `@Entity` classes representing the database schema.
- Keep internal fields like `id`, audit timestamps (`createdAt`, `updatedAt`), and sensitive data (`passwordHash`).
- Never return JPA entities directly to API clients.

#### Models: DTOs (`models/dtos/`)
- **Use Java `record`** instead of classes:
  ```java
  public record UserDto(
      Long id,
      @NotBlank(message = "Username is required")
      String username,
      @Email(message = "Email must be valid")
      @NotBlank(message = "Email is required")
      String email
  ) {}
  ```
- **Exclude Sensitive Fields**: Do not include `password`, credentials, or encryption keys in DTOs.
- **Exclude Internal Audit Fields**: Omit `created_at`, `updated_at`, `create_at`, and `update_at` unless explicitly needed by the client contract.
- **Single DTO Policy**: Use the same DTO for both requests and responses unless specific validation or response requirements strictly necessitate separate Command/Query objects.

#### Mappers (`mappers/`)
- Create dedicated Mapper classes (annotated with `@Component`).
- Provide explicit bidirectional mapping methods:
  - `toDto(Entity entity)`: converts JPA Entity to DTO Record.
  - `toEntity(DTO dto)`: converts DTO Record to JPA Entity.
- Services must invoke the Mapper rather than performing manual object conversions inline.

#### Services (`services/`)
- Encapsulate business logic, domain validation, and transactional boundaries (`@Transactional`).
- Orchestrate repository operations.
- Receive and return DTOs (not entities) from/to the controller layer, transforming them via the injected `Mapper`.

#### Controllers (`controllers/`)
- Expose RESTful endpoints using `@RestController` and `@RequestMapping("/api/...")`.
- Use `@Valid` or `@Validated` on `@RequestBody` to validate input payloads.
- Strictly return DTOs or `ResponseEntity<DTO>` (never JPA entities).
- Keep controllers lean: delegate all business execution to services.

---

## 3. Server-Rendered Monoliths (Thymeleaf + Tailwind CSS)

When building server-rendered monolithic web interfaces:
- Include dependency `spring-boot-starter-thymeleaf`.
- Use **Tailwind CSS** (via CDN for quick setup or build toolchain).
- Structure templates under `src/main/resources/templates/`:
  ```
  templates/
  ├── layouts/
  │   └── base.html
  ├── fragments/
  │   ├── header.html
  │   ├── navbar.html
  │   └── footer.html
  └── pages/
      └── index.html
  ```
- **Semantic HTML & Reusable Layout**:
  - Always use semantic `<header>`, `<nav>`, `<main>`, and `<footer>` elements.
  - Use `base.html` with Thymeleaf fragment insertion (`th:replace` or `th:insert`) to keep main content centered and navigation consistent.

---

## 4. Running the Application Locally

Always prefer the Maven Wrapper over a global `mvn` installation.

- **Linux / macOS**:
  ```bash
  ./mvnw -DskipTests spring-boot:run
  ```

- **Windows**:
  ```cmd
  mvnw.cmd -DskipTests spring-boot:run
  ```

---

## 5. Configuration & AGENTS.md Governance

1. Maintain all configuration in `src/main/resources/application.properties`.
2. Always ensure the root of the project contains an `AGENTS.md` documenting this skill and any other active project skills, keeping triggers and patterns synchronized.
