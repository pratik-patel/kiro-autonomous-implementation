---
name: code-quality-steering
mode: fileMatch
fileMatch: ["**/*.java", "**/pom.xml", "**/*.properties", "**/*.yml", "**/*.yaml"]
---

# Code Quality Steering (Strict Enforcement)

## Purpose
This document defines the **non-negotiable** code quality standards for the project. These rules are enforced by the `workflow-orchestrator`, `code-reviewer`, and SonarQube/SonarLint.

## 1. Strict Quality Gates (BLOCKERS)

> [!IMPORTANT]
> **Adherence to these gates is MANDATORY.** The Orchestrator Agent WILL NOT create a Pull Request if any of these are violated.

### 1.1 SonarQube / SonarLint
- **BLOCKER**: Any issues with severity `BLOCKER` or `CRITICAL`.
- **BLOCKER**: Any issues with severity `MAJOR`.
- **BLOCKER**: Reliability Rating MUST be A.
- **BLOCKER**: Security Rating MUST be A.
- **BLOCKER**: Maintainability Rating MUST be A.

### 1.2 Design Pattern Violations
The `code-reviewer` agent will block PRs for the following violations:
- **Circular Dependencies**: Components must not depend on each other.
- **Layer Violation**: `Controller` -> `Service` -> `Repository`. Controllers MUST NOT call Repositories directly. Domain models MUST NOT depend on infrastructure.
- **God Classes**: Classes with > 20 fields or > 500 lines are banned. Refactor immediately.
- **Anemic Domain Model**: Business logic belongs in Entities/Domain Services, not in "Helper" classes.

### 1.3 Naming Conventions
- **Classes**: `Noun` (e.g., `LoanReviewService`).
- **Methods**: `Verb` + `Noun` (e.g., `calculateRisk`).
- **Variables**: Descriptive, no hungarian notation (e.g., `customerList`, not `lstCust`).
- **Constants**: `UPPER_SNAKE_CASE` (e.g., `MAX_RETRY_ATTEMPTS`).

## 2. Java / Spring Boot Standards

- **Dependency Injection**: Always use Constructor Injection. Field injection (`@Autowired` on fields) is **FORBIDDEN**.
- **Lombok**: Use `@Data`, `@Value`, `@Builder` sparingly. Prefer `@Getter`/`@Setter` or Records for simple DTOs.
- **Exceptions**: Use unchecked exceptions (runtime) for business logic. Defined custom exceptions for domain errors.
- **Logging**: Use SLF4J. Log at proper levels (ERROR for failures, WARN for recoverable issues, INFO for business events).

## 3. Tool Usage Guidelines (SonarQube MCP)

### Analysis Workflow
1.  **Generate Code**: Implement feature.
2.  **Lint Locally**: Run `mvn sonar:sonar` (or equivalent).
3.  **Fix Issues**: Resolve all reported issues *before* asking for review.
4.  **Verify**: Re-run scan to confirm clean state.

### Troubleshooting
- **False Positives**: If you believe a rule is wrong, mark it as "Won't Fix" in Sonar extraction *with a valid reason comment*.
- **Authentication**: Use `USER` tokens for MCP access.

## 4. Documentation
- **JavaDoc**: Required for all Public Interfaces and Service methods.
- **Comments**: Explain "Why", not "What".
- **README**: Update module `README.md` if architecture changes.
