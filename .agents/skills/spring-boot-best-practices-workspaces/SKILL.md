---
name: spring-boot-best-practices-workspaces
description: Defines the general workspace rules, development standards, communication language, and skill orchestration guidelines for the Spring Boot development workspace. Trigger this skill whenever working in this workspace, generating Spring Boot code, communicating with the user, or coordinating between workspace skills.
---

# Spring Boot Workspace Best Practices & Governance

This skill establishes the general workspace rules, development standards, language requirements, and skill usage guidelines for all agents operating within this Spring Boot workspace.

## 1. Workspace Rules & Standards

### 1.1 Code Language
All generated source code, project files, and technical artifacts must be written in **English**. This applies to:
- Package names
- Class, interface, enum, and record names
- Method, variable, and parameter names
- Database entity names, table/column annotations, and SQL scripts
- Technical comments, javadoc, and docstrings inside code files

### 1.2 Communication Language
All user responses, explanations, progress reports, system messages, and inter-agent messages within this workspace must always be written in **Spanish**.

### 1.3 Java Version
All Spring Boot applications generated or maintained in this workspace must target **Java 25** unless the user explicitly requests another version.

### 1.4 Architecture & Separation of Concerns
All Spring Boot projects must adhere to a strict **layered architecture**:
- **Controllers**: REST endpoints, request validation, response wrapping.
- **Services**: Business logic, orchestration, transactional boundaries.
- **Repositories**: Data access interfaces extending Spring Data `JpaRepository`.
- **Models / Entities**: Persistence entities mapping database tables.
- **DTOs**: Immutable data carriers (`record`), separating API payload contracts from persistence entities. Exclude sensitive and internal audit fields.
- **Mappers**: Dedicated conversion components between Entities and DTOs.

---

## 2. Available Workspace Skills & Roles

| Skill Name | Path | Primary Responsibility |
| :--- | :--- | :--- |
| **`spring-boot-best-practices-workspaces`** | `.agents/skills/spring-boot-best-practices-workspaces/SKILL.md` | Workspace-level governance, language policy (Code in English, Communication in Spanish), and skill orchestration. |
| **`spring-boot-best-practices`** | `.agents/skills/spring-boot-best-practices/SKILL.md` | Concrete technical implementation guidelines for Spring Boot APIs, entities, services, controllers, DTOs, mappers, and Thymeleaf monoliths. |
| **`skill-creator`** | `.agents/skills/skill-creator/SKILL.md` | Creating, evaluating, benchmarking, and maintaining agent skills. |

---

## 3. Skill Trigger & Orchestration Rules

### Skill Activation for `spring-boot-best-practices`
Activate and apply `spring-boot-best-practices` whenever the user requests:
- Creating a basic Spring Boot API or project.
- Creating a Spring Web monolithic application with Thymeleaf and Tailwind CSS.
- Creating, modifying, or extending entities, repositories, services, controllers, or mappers.
- Structuring or refactoring Spring Boot application layers.

### Hierarchy & Conflict Resolution
- `spring-boot-best-practices-workspaces` governs the **workspace environment and communication rules**.
- `spring-boot-best-practices` governs the **technical implementation details**.
- When both skills apply, the workspace-wide conventions (e.g. English code, Spanish communication, Java 25) take precedence and frame the implementation rules of `spring-boot-best-practices`.
