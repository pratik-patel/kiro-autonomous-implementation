---
name: project-tech-steering
inclusion: fileMatch
fileMatchPattern: "src/**/*.java"
description: Project technology stack and standards
---

# Crew Ops Lite: Project & Technology Guide

## Product Overview

**Crew Ops Lite: Crew Reassignment Workflow**

A Spring Boot application that automates crew reassignment for disrupted flights. When a flight is delayed, canceled, or a crew member calls out sick, the system validates the disruption event, retrieves flight and crew data, and produces an assignment outcome or escalates to manual review.

### Core Problem

Manual crew reassignment is slow and error-prone. Mistakes can violate FAA duty/rest rules or contractual constraints. This system provides rapid, compliant crew reassignment decisions.

### Key Features

- **Event Validation**: Validates disruption events for required fields, types, and business constraints
- **Flight Data Retrieval**: Fetches flight details and validates consistency with event data
- **Crew Data Retrieval**: Retrieves crew roster and filters by required roles
- **Event Enrichment**: Combines event, flight, and crew data with metadata for downstream processing
- **Audit Logging**: Persists validation decisions to DynamoDB for compliance and audit trails
- **Error Handling**: Comprehensive error logging to CloudWatch and SNS notifications

### Demo Scenario

Input: Crew disruption event (e.g., captain sick call on DL123 departing in 90 minutes)
Output: Assignment outcome with crew assignments or manual review escalation

### Deployment Target

AWS Lambda + Step Functions orchestration (Spring Boot runs as Lambda function)

---

## Core Principles

- **MUST follow Spec-Driven Development**: Requirements → Design → Tasks → Implementation
- **MUST keep changes small, reviewable, and traceable** to a spec or ticket
- **MUST avoid introducing unnecessary dependencies** or architectural complexity

---

## Technology Stack

### Core Framework

- **Spring Boot 3.x**: REST API and application framework
- **Java 17+**: Primary language
- **Maven**: Build system and dependency management

### Testing & Quality

- **JUnit 5**: Unit testing framework
- **Mockito**: Mocking library for unit tests
- **Property-Based Testing**: Fast-check or Hypothesis equivalent for Java (e.g., QuickCheck, jqwik)
- **SonarLint**: Code quality and static analysis
- **Code Coverage**: Minimum 80% coverage required before merge

### AWS Services

- **Lambda**: Function execution (Spring Boot packaged as Lambda)
- **Step Functions**: Workflow orchestration
- **DynamoDB**: Audit logging and validation metadata persistence
- **CloudWatch**: Logging and monitoring
- **SNS**: Error event notifications
- **IAM**: Access control and least-privilege policies

### Infrastructure as Code

- **Terraform**: AWS resource provisioning
- **AWS CLI**: Deployment and management

### Dependencies

- **Spring Web**: REST endpoints
- **Spring Data DynamoDB**: DynamoDB integration
- **AWS SDK v2**: AWS service clients
- **Jackson**: JSON serialization/deserialization
- **Resilience4j**: Circuit breaker pattern for external calls
- **Lombok**: Reduce boilerplate (optional but recommended)

---

## Code Style & Standards

- Follow Google Java Style Guide
- Use meaningful variable and method names
- Keep methods focused and under 20 lines when possible
- Use dependency injection for all services
- Implement interfaces for testability
- Add comprehensive logging with correlation IDs
- Use UTC timezone for all timestamps

---

## Common Commands

### Build & Package
```bash
mvn clean package          # Build and package application
mvn clean install          # Build and install to local repository
```

### Testing
```bash
mvn test                   # Run all unit tests
mvn test -Dtest=ClassName # Run specific test class
mvn verify                 # Run tests + code quality checks
```

### Code Quality
```bash
mvn sonar:sonar            # Run SonarQube analysis (requires SonarQube server)
# SonarLint runs automatically in IDE during development
```

### Local Development
```bash
mvn spring-boot:run        # Run Spring Boot application locally
```

### Terraform
```bash
terraform init             # Initialize Terraform working directory
terraform plan             # Preview infrastructure changes
terraform apply            # Apply infrastructure changes
terraform destroy          # Destroy infrastructure
```

---

## Project Structure

```
crew-ops-lite/
├── src/
│   ├── main/
│   │   ├── java/com/crewops/
│   │   │   ├── config/              # Spring configuration classes
│   │   │   │   ├── CloudWatchConfig.java
│   │   │   │   ├── DynamoDBConfig.java
│   │   │   │   ├── DynamoDBRepositoryConfig.java
│   │   │   │   └── SNSConfig.java
│   │   │   ├── controller/          # REST endpoints
│   │   │   │   ├── ExceptionController.java
│   │   │   │   ├── StatusController.java
│   │   │   │   ├── UploadController.java
│   │   │   │   └── VerifiedRecordsController.java
│   │   │   ├── service/             # Business logic services
│   │   │   │   ├── AuditLogService.java
│   │   │   │   ├── AuditService.java
│   │   │   │   ├── EventPublisher.java
│   │   │   │   ├── ExceptionQueryService.java
│   │   │   │   ├── ExceptionService.java
│   │   │   │   ├── TrainingRepositoryService.java
│   │   │   │   ├── UploadService.java
│   │   │   │   ├── VTSEligibilityService.java
│   │   │   │   └── ValidationService.java
│   │   │   ├── model/               # Data models and DTOs
│   │   │   │   ├── AuditLog.java
│   │   │   │   ├── BidPeriodControl.java
│   │   │   │   ├── ExceptionSummary.java
│   │   │   │   ├── FunctionalException.java
│   │   │   │   ├── TrainingRecord.java
│   │   │   │   └── VTSEligibility.java
│   │   │   ├── repository/          # Data access layer
│   │   │   │   ├── AuditLogRepository.java
│   │   │   │   ├── BidPeriodControlRepository.java
│   │   │   │   ├── FunctionalExceptionRepository.java
│   │   │   │   ├── TrainingRecordRepository.java
│   │   │   │   └── VTSEligibilityRepository.java
│   │   │   ├── exception/           # Custom exceptions
│   │   │   │   ├── EventPublishingException.java
│   │   │   │   ├── FileProcessingException.java
│   │   │   │   ├── StagingException.java
│   │   │   │   └── ValidationException.java
│   │   │   ├── util/                # Utility classes
│   │   │   │   ├── CorrelationIdGenerator.java
│   │   │   │   ├── DateConverter.java
│   │   │   │   └── TTLCalculator.java
│   │   │   └── Application.java     # Spring Boot entry point
│   │   └── resources/
│   │       └── application.yml      # Spring configuration
│   └── test/
│       ├── java/com/crewops/        # Unit and integration tests
│       │   ├── exception/
│       │   │   └── StagingExceptionTest.java
│       │   ├── service/
│       │   │   ├── AuditLogServiceTest.java
│       │   │   ├── AuditServiceTest.java
│       │   │   ├── ExceptionQueryServicePropertyTest.java
│       │   │   ├── ExceptionQueryServiceTest.java
│       │   │   ├── ExceptionServiceSLAPropertyTest.java
│       │   │   ├── ExceptionServiceTest.java
│       │   │   ├── TrainingRepositoryServiceTest.java
│       │   │   ├── UploadServiceTest.java
│       │   │   ├── VTSEligibilityServiceTest.java
│       │   │   └── ValidationServiceTest.java
│       │   └── util/
│       │       └── DateConverterTest.java
│       └── resources/               # Test configuration
├── terraform/                       # Infrastructure as Code
├── .kiro/
│   ├── specs/                       # Feature specifications
│   └── steering/                    # Project guidance
├── pom.xml                          # Maven configuration
├── postman_collection.json          # API test collection
├── sample_training_package.csv      # Sample data
└── README.md                        # Project documentation
```

### Package Organization

**`config/`** - Spring configuration classes for beans, clients, and external service setup
- `CloudWatchConfig`: CloudWatch logging configuration
- `DynamoDBConfig`: DynamoDB client setup
- `SNSConfig`: SNS topic and subscription configuration

**`controller/`** - REST endpoints handling HTTP requests
- `ExceptionController`: Exception management endpoints
- `StatusController`: Health and status endpoints
- `UploadController`: File upload endpoints
- `VerifiedRecordsController`: Verified records retrieval

**`service/`** - Business logic and orchestration
- `AuditLogService`: Audit log persistence and retrieval
- `AuditService`: Audit trail management
- `EventPublisher`: SNS event publishing
- `ExceptionQueryService`: Exception querying and filtering
- `ExceptionService`: Exception handling and SLA management
- `TrainingRepositoryService`: Training record management
- `UploadService`: File upload processing
- `VTSEligibilityService`: VTS eligibility determination
- `ValidationService`: Event and data validation

**`model/`** - Data transfer objects and domain models
- `AuditLog`: Audit log records
- `BidPeriodControl`: Bid period metadata
- `ExceptionSummary`: Exception summaries
- `FunctionalException`: Functional exception details
- `TrainingRecord`: Training record data
- `VTSEligibility`: VTS eligibility information

**`repository/`** - Data access abstraction (DAO pattern)
- Extends Spring Data repositories for DynamoDB operations
- Handles CRUD operations and custom queries
- Implements retry logic and error handling

**`exception/`** - Custom exception hierarchy
- `EventPublishingException`: SNS publishing failures
- `FileProcessingException`: File processing errors
- `StagingException`: Staging/validation errors
- `ValidationException`: Validation failures

**`util/`** - Utility classes for common operations
- `CorrelationIdGenerator`: Generates unique correlation IDs
- `DateConverter`: Date/time conversion utilities
- `TTLCalculator`: TTL calculation for DynamoDB records

### Naming Conventions

- **Classes**: PascalCase (e.g., `EventValidationService`, `TrainingRecord`)
- **Methods**: camelCase, verb-based (e.g., `validateEvent()`, `retrieveFlightData()`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `DEFAULT_TIMEOUT_SECONDS`)
- **Variables**: camelCase, descriptive (e.g., `flightNumber`, `crewRoster`)
- **Packages**: lowercase with dots (e.g., `com.crewops.service`)
- **Booleans**: prefix with `is`, `has`, `can` (e.g., `isValid`, `hasPermission`)

### Testing Structure

Tests mirror source structure:
- `src/test/java/com/crewops/service/EventValidationServiceTest.java`
- `src/test/java/com/crewops/controller/EventValidationControllerTest.java`
- Property-based tests use `*PropertyTest.java` suffix
- Integration tests use `*IntegrationTest.java` suffix

---

## Definition of Done

A change is "done" only when:

- Requirements and acceptance criteria are satisfied (and evidence exists)
- Tests are updated/added where behavior changed
- Security and privacy controls are addressed
- Observability is considered (logs/metrics/traces as relevant)
- Documentation/config updates are included when needed
