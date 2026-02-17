---
name: backend-steering
inclusion: fileMatch
fileMatchPattern: "**/*.java"
description: Java engineering standards for Java 17+ and Spring Boot
---

# Java Engineering Standards (Java 17+)

## Core Principles

**Single Responsibility**: One class = one reason to change. Never mix business logic with IO, validation, or formatting.

**Depend on Abstractions**: High-level modules depend on interfaces, not implementations.

**Favor Composition**: Prefer composition and polymorphism over inheritance and conditional branching.

**Eliminate Boilerplate**: Use Lombok and MapStruct to reduce noise. Never write what can be generated.

## Lombok Usage (Required)

Use Lombok annotations instead of manual boilerplate:

| Instead of writing... | Use |
|----------------------|-----|
| Getters/Setters | `@Getter`, `@Setter` or `@Data` |
| Constructors | `@NoArgsConstructor`, `@AllArgsConstructor`, `@RequiredArgsConstructor` |
| Builder pattern | `@Builder` |
| equals/hashCode | `@EqualsAndHashCode` |
| Logging field | `@Slf4j` or `@Log4j2` |
| Immutable DTOs | `@Value` (preferred for DTOs and value objects) |

**Preferred patterns:**
- Entities: `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`
- DTOs/Value Objects: `@Value @Builder` (immutable by default)
- Services: `@RequiredArgsConstructor` + `final` fields for DI

**Prohibited:**
- Manual getters/setters when Lombok is available
- Manual builder implementations
- `@Data` on JPA entities (breaks lazy loading)—use explicit annotations instead

## Object Mapping (Required)

Use **MapStruct** for entity/DTO conversions. Never write manual mapping methods.
```java
// PROHIBITED - manual conversion
public UserDto toDto(User user) {
    UserDto dto = new UserDto();
    dto.setId(user.getId());
    dto.setName(user.getName());
    // ... more tedious mapping
    return dto;
}

// REQUIRED - MapStruct
@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDto toDto(User user);
    User toEntity(UserDto dto);
    List<UserDto> toDtoList(List<User> users);
}
```

**MapStruct guidelines:**
- One mapper interface per aggregate/domain boundary
- Use `@Mapping` for field name mismatches
- Use `@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)` for partial updates
- Inject mappers via constructor, not field injection

## Naming

- Classes: PascalCase nouns (`OrderValidator`)
- Methods: camelCase verbs (`validateOrder`)
- Booleans: `is`, `has`, `can` prefixes
- No abbreviations, no type encoding (`users` not `userList`)

## Method Design

- One thing per method, ≤20 lines preferred
- Maximum 3-4 parameters; group related data into value objects
- Maximum 2 levels of nesting
- No magic numbers or strings—use named constants

## Immutability

- Default to `final` for fields and variables
- Use `@Value` for immutable objects
- Never expose mutable internal state
- Return defensive copies of collections (or use immutable collections)

## Error Handling

- Fail fast on violated invariants
- Never swallow exceptions
- Exceptions must be meaningful and actionable
- Never log credentials, tokens, or sensitive data

## Security

- Treat all external input as untrusted
- Validate at trust boundaries
- Avoid unsafe reflection and deserialization
- Error messages must not expose internals

## Prohibited

- God classes and utility dumping grounds
- Empty catch blocks
- Hardcoded configuration
- Duplicated logic
- Premature optimization
- Commented-out code
- Manual boilerplate that Lombok/MapStruct can generate
- `@Data` on JPA entities
- Manual entity↔DTO conversion methods