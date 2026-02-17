---
name: test-automation-steering
inclusion: fileMatch
fileMatchPattern: "src/test/**/*.java"
description: Testing standards for Java/JUnit/Spring testing
---

<!------------------------------------------------------------------------------------
  Testing Steering (fileMatch)

  Applies when creating/modifying tests. Ensures consistency, maintainability,
  and industry best practices for Java/JUnit/Spring testing.
------------------------------------------------------------------------------------->

# Testing Steering (when editing tests)

## Core Principles

- **Test Pyramid**: Favor unit tests (80%), integration tests (20%), end-to-end tests (10%)
- **Deterministic**: No flaky tests; avoid sleeps, timing dependencies, or external state
- **Meaningful**: Every test must have clear intent and meaningful assertions
- **Isolated**: Unit tests must not depend on external services, databases, or other tests
- **Fast**: Unit tests should complete in milliseconds; integration tests in seconds
- **Maintainable**: Clear naming, minimal setup, easy to understand and modify

## General Requirements

- MUST use JUnit 5 (Jupiter)
- MUST ensure tests are deterministic (no sleeps/flaky timing dependencies)
- MUST contain meaningful assertions (no empty tests or placeholder assertions)
- MUST follow AAA (Arrange / Act / Assert) or Given/When/Then pattern
- MUST keep tests small and focused on one behavior per test
- MUST use descriptive test names that explain what is being tested and expected outcome
- MUST reference acceptance criteria IDs when available (e.g., in @DisplayName or comments)
- MUST achieve minimum 80% code coverage before merge
- MUST NOT test implementation details; test behavior and contracts

## Unit Testing

### Structure & Naming

- **File naming**: `{ClassName}Test.java` (e.g., `ValidationServiceTest.java`)
- **Test method naming**: Use descriptive names following pattern `test{Scenario}_{ExpectedOutcome}` or `should{ExpectedOutcome}When{Condition}`
  - Example: `testValidateEvent_ThrowsExceptionWhenEventIsNull()`
  - Example: `shouldReturnValidationErrorsWhenRequiredFieldsMissing()`
- **Display names**: Use `@DisplayName("descriptive text")` for complex scenarios
  - Example: `@DisplayName("should reject event when departure time is in the past")`

### Setup & Teardown

- Use `@BeforeEach` for test-specific setup (not shared state)
- Use `@BeforeAll` only for expensive, truly shared setup (static resources)
- Avoid `@AfterEach` unless explicitly cleaning up resources (files, connections)
- Prefer constructor injection or field initialization over setup methods when possible

### Mocking External Dependencies

- MUST mock all external dependencies (services, repositories, AWS clients)
- Use Mockito for mocking: `@Mock`, `@InjectMocks`, `when()`, `verify()`
- MUST NOT mock the class under test; use real instances
- Mock at the boundary: mock interfaces, not concrete implementations
- Use `ArgumentCaptor` to verify complex argument passing
- Use `ArgumentMatchers` for flexible argument matching (`any()`, `eq()`, `argThat()`)

#### Mocking Best Practices

```java
// DO: Mock the dependency interface
@Mock
private FlightRepository flightRepository;

@InjectMocks
private FlightService flightService;

// DON'T: Mock the class under test
@Mock
private FlightService flightService; // Wrong!

// DO: Use specific matchers
when(repository.findById(eq(123))).thenReturn(Optional.of(flight));

// DON'T: Use overly broad matchers
when(repository.findById(any())).thenReturn(Optional.of(flight)); // Too loose

// DO: Verify interactions
verify(repository, times(1)).save(any(Flight.class));
verify(logger, never()).error(anyString());

// DO: Use ArgumentCaptor for complex assertions
ArgumentCaptor<Flight> captor = ArgumentCaptor.forClass(Flight.class);
verify(repository).save(captor.capture());
assertThat(captor.getValue().getFlightNumber()).isEqualTo("DL123");
```

### Assertions

### Assertions (Strict)

- **MUST** Use AssertJ for fluent, readable assertions: `assertThat()`, `isEqualTo()`, `contains()`, etc.
- **BLOCKER**: Using JUnit's `assertEquals()` or `assertTrue()` for complex objects.
- **BLOCKER**: Tests with NO assertions.
- **MUST** Use `assertThatThrownBy()` for exception testing (Never use `@Test(expected=...)`).

```java
// DO: Fluent, readable assertions
assertThat(result)
    .isNotNull()
    .hasFieldOrPropertyWithValue("status", "VALID")
    .extracting("errors")
    .asList()
    .isEmpty();

// DO: Exception testing
assertThatThrownBy(() -> service.validate(null))
    .isInstanceOf(ValidationException.class)
    .hasMessage("Event cannot be null");

// DON'T: Weak assertions
assertTrue(result != null); // Weak
assertEquals(true, result.isValid()); // Weak
```

### Test Data & Builders

- Use builder pattern or factory methods for complex test objects
- Create reusable test fixtures in `@BeforeEach` or static factory methods
- Use meaningful default values that reflect realistic scenarios
- Document non-obvious test data choices

```java
// DO: Builder pattern for test data
private DisruptionEvent createValidEvent() {
    return new DisruptionEvent.Builder()
        .eventId("EVT-123")
        .flightNumber("DL456")
        .eventType("CREW_CALLOUT")
        .timestamp(Instant.now())
        .build();
}

// DO: Factory method with clear intent
private FlightData createFlightWithMinimalCrew() {
    return new FlightData("DL123", 2, 3); // 2 crew, 3 required
}
```

### Parameterized Tests

- Use `@ParameterizedTest` with `@ValueSource`, `@CsvSource`, or `@MethodSource` for testing multiple scenarios
- Reduces code duplication and improves coverage
- Use descriptive display names for each parameter set

```java
@ParameterizedTest
@CsvSource({
    "null, INVALID",
    "'', INVALID",
    "DL123, VALID"
})
@DisplayName("should validate flight number format")
void testFlightNumberValidation(String flightNumber, String expected) {
    // test implementation
}
```

## Integration Testing

### File Naming & Scope

- **File naming**: `{ClassName}IntegrationTest.java` or `{ClassName}IT.java`
- **Scope**: Test interactions between multiple components (service + repository, service + external client)
- **NOT end-to-end**: Do not test full HTTP request/response cycle in integration tests (use `@WebMvcTest` or `@SpringBootTest` for that)

### Spring Test Annotations

- Use `@SpringBootTest` for full application context (slower, comprehensive)
- Use `@DataJpaTest` for repository-only testing with embedded database
- Use `@WebMvcTest` for controller testing with mocked services
- Use `@MockBean` for replacing beans in Spring context
- Use `@TestPropertySource` or `@DynamicPropertySource` for test-specific configuration

```java
// DO: Full integration test with Spring context
@SpringBootTest
class EventValidationIntegrationTest {
    @Autowired
    private EventValidationService service;
    
    @MockBean
    private FlightClient flightClient;
    
    @Test
    void testEventValidationWithMockedFlightClient() {
        // test implementation
    }
}

// DO: Repository-only test with embedded database
@DataJpaTest
class AuditLogRepositoryTest {
    @Autowired
    private AuditLogRepository repository;
    
    @Test
    void testSaveAndRetrieveAuditLog() {
        // test implementation
    }
}

// DO: Controller test with mocked services
@WebMvcTest(EventValidationController.class)
class EventValidationControllerTest {
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private EventValidationService service;
    
    @Test
    void testPostValidateEvent() throws Exception {
        // test implementation
    }
}
```

### Mocking AWS Services

- Use `@MockBean` to replace AWS service beans in Spring context
- Mock at the client level (e.g., `DynamoDBClient`, `CloudWatchClient`)
- Use AWS SDK v2 mocking patterns or libraries like `LocalStack` for local testing
- For DynamoDB: use embedded DynamoDB or `@DataJpaTest` with test containers

```java
// DO: Mock AWS client in Spring context
@SpringBootTest
class AuditLoggingIntegrationTest {
    @MockBean
    private DynamoDBClient dynamoDBClient;
    
    @Autowired
    private AuditLogger auditLogger;
    
    @Test
    void testAuditLogPersistence() {
        // Mock DynamoDB response
        when(dynamoDBClient.putItem(any())).thenReturn(PutItemResponse.builder().build());
        
        // test implementation
    }
}

// DO: Use TestContainers for real DynamoDB (optional, for comprehensive testing)
@SpringBootTest
@Testcontainers
class AuditLoggingWithTestContainersTest {
    @Container
    static LocalStackContainer localstack = new LocalStackContainer()
        .withServices(DYNAMODB);
    
    // test implementation
}
```

### Database Testing

- Use `@DataJpaTest` for repository tests with embedded H2 database
- Use `@Transactional` to rollback changes after each test (default behavior)
- Avoid relying on test execution order; each test must be independent
- Use `@Sql` or `@SqlGroup` for loading test data

```java
@DataJpaTest
class AuditLogRepositoryTest {
    @Autowired
    private AuditLogRepository repository;
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Test
    @Transactional
    void testFindByCorrelationId() {
        // Arrange
        AuditLog log = new AuditLog("CORR-123", "VALIDATION_PASSED");
        entityManager.persistAndFlush(log);
        
        // Act
        Optional<AuditLog> result = repository.findByCorrelationId("CORR-123");
        
        // Assert
        assertThat(result).isPresent().hasValueSatisfying(l -> 
            assertThat(l.getStatus()).isEqualTo("VALIDATION_PASSED")
        );
    }
}
```

### HTTP Testing with MockMvc

- Use `MockMvc` for testing REST endpoints without starting a server
- Test request/response structure, status codes, and headers
- Use `@WebMvcTest` for controller-only testing (faster than `@SpringBootTest`)

```java
@WebMvcTest(EventValidationController.class)
class EventValidationControllerTest {
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private EventValidationService service;
    
    @Test
    void testPostValidateEventSuccess() throws Exception {
        // Arrange
        DisruptionEvent event = createValidEvent();
        ValidationResult result = new ValidationResult(true, Collections.emptyList());
        when(service.validate(any())).thenReturn(result);
        
        // Act & Assert
        mockMvc.perform(post("/validate-event")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(true))
            .andExpect(jsonPath("$.errors").isArray());
    }
    
    @Test
    void testPostValidateEventBadRequest() throws Exception {
        mockMvc.perform(post("/validate-event")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
    }
}
```

## Property-Based Testing

- Use jqwik or QuickCheck for property-based tests (file suffix: `*PropertyTest.java`)
- Test invariants and properties that should hold for all inputs
- Useful for validation logic, date calculations, and edge cases
- Combine with unit tests, not replace them

```java
// DO: Property-based test with jqwik
class ValidationServicePropertyTest {
    @Property
    void eventIdShouldNeverBeNull(@ForAll String eventId) {
        Assume.that(eventId != null && !eventId.isEmpty());
        
        DisruptionEvent event = new DisruptionEvent.Builder()
            .eventId(eventId)
            .flightNumber("DL123")
            .build();
        
        assertThat(event.getEventId()).isNotNull();
    }
    
    @Property
    void departureTimeShouldBeInFuture(@ForAll @LongRange(min = 0) long minutesFromNow) {
        Instant departure = Instant.now().plusSeconds(minutesFromNow * 60);
        
        assertThat(departure).isAfter(Instant.now());
    }
}
```

## Test Organization & Maintenance

### Nested Test Classes

- Use `@Nested` to organize related tests by scenario or method
- Improves readability and reduces duplication

```java
class EventValidationServiceTest {
    @Nested
    @DisplayName("validateEvent")
    class ValidateEventTests {
        @Test
        void shouldReturnValidWhenEventIsCorrect() { }
        
        @Test
        void shouldReturnInvalidWhenEventIsNull() { }
    }
    
    @Nested
    @DisplayName("enrichEvent")
    class EnrichEventTests {
        @Test
        void shouldAddFlightDataToEvent() { }
    }
}
```

### Test Fixtures & Shared Setup

- Use `@BeforeEach` for per-test setup
- Use static factory methods for reusable test data
- Consider test builder classes for complex objects

```java
class EventValidationServiceTest {
    private EventValidationService service;
    private FlightRepository flightRepository;
    
    @BeforeEach
    void setUp() {
        flightRepository = mock(FlightRepository.class);
        service = new EventValidationService(flightRepository);
    }
    
    private DisruptionEvent createValidEvent() {
        return new DisruptionEvent.Builder()
            .eventId("EVT-123")
            .flightNumber("DL456")
            .build();
    }
}

## Strict Quality Gates (BLOCKERS)

> [!IMPORTANT]
> **Adherence to these gates is MANDATORY.** Agent MUST NOT proceed if any of these are violated.

1.  **Strict Coverage Gate**:
    *   **BLOCKER**: Total Line Coverage < 80%.
    *   **BLOCKER**: Zero coverage on new/modified business logic.

2.  **Test Type Requirements**:
    *   **BLOCKER**: Missing Unit Tests for Service/Domain logic.
    *   **BLOCKER**: Missing Integration Tests for Repository/Controller logic.
    *   **BLOCKER**: Tests depend on external services (must use Mocks/TestContainers).

3.  **Code Quality in Tests**:
    *   **BLOCKER**: Using `System.out.println` instead of Assertions.
    *   **BLOCKER**: Empty test methods or disabled tests (`@Disabled`) without a linked ticket.
    *   **BLOCKER**: Hardcoded credentials/secrets in test code.

4.  **Performance**:
    *   **BLOCKER**: Unit tests taking > 500ms (indicates bad isolation).

## Common Pitfalls to Avoid

- **Flaky tests**: Avoid `Thread.sleep()`, use `@Timeout`, or use `Awaitility` for async operations
- **Over-mocking**: Mock only external dependencies, not internal collaborators
- **Test interdependence**: Each test must be independent and runnable in any order
- **Assertion overload**: One logical assertion per test (multiple related assertions are OK)
- **Testing implementation**: Test behavior and contracts, not internal implementation details
- **Ignoring exceptions**: Always assert on exception type and message
- **Hardcoded values**: Use constants or test data builders for clarity

## Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=EventValidationServiceTest

# Run specific test method
mvn test -Dtest=EventValidationServiceTest#testValidateEvent_ThrowsExceptionWhenEventIsNull

# Run with coverage report
mvn clean verify

# Run integration tests only
mvn test -Dgroups=integration

# Run with SonarLint
mvn sonar:sonar
```