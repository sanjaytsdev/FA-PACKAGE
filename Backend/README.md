# Financial Accounting Backend

A Spring Boot application implementing **Clean Architecture** principles for financial accounting management. This backend provides REST APIs for managing financial accounts, groups, and journal entries with a focus on maintainability, testability, and scalability.

## 🏗️ Clean Architecture Overview

### What is Clean Architecture?

**Clean Architecture** is a software design philosophy that separates concerns into distinct layers, making your code:
- **Testable** - Business rules can be tested without UI, database, web server, or any external element
- **Independent of frameworks** - Business rules don't depend on external libraries
- **Independent of UI** - The UI can change easily without changing the rest of the system
- **Independent of database** - Business rules are not bound to the database
- **Independent of external agencies** - Business rules don't know anything about the outside world

### Architecture Layers

Our implementation follows the classic Clean Architecture pattern with four distinct layers:

```package com.spam.financialaccounting.application.usecases;

import org.springframework.stereotype.Service;

import com.spam.financialaccounting.domain.entity.FAGroup;
import com.spam.financialaccounting.domain.repository.FAGroupRepository;

@Service
public class CreateFAGroup {
    private FAGroupRepository faGroupRepository;

    public CreateFAGroup(FAGroupRepository faGroupRepository) {
        this.faGroupRepository = faGroupRepository;
    }

    public void execute(FAGroup faGroup) {
        faGroupRepository.save(faGroup);  
    }
}

┌─────────────────────────────────────────────────────────────┐
│                     Presentation Layer                        │
│                   (Controllers & DTOs)                      │
│              Handles HTTP requests/responses                 │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                   Application Layer                          │
│                    (Use Cases)                               │
│         Orchestrates business logic and workflows            │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                     Domain Layer                             │
│              (Entities & Repository Interfaces)             │
│         Core business rules and abstractions                 │
└─────────────────────────────────────────────────────────────┘
                              ↑
┌─────────────────────────────────────────────────────────────┐
│                  Infrastructure Layer                        │
│         (Repository Implementations & Mappers)               │
│      Database access, external services, implementations     │
└─────────────────────────────────────────────────────────────┘
```

## 📂 Project Structure

```
src/main/java/com/spam/financialaccounting/
│
├── 📁 presentation/
│   ├── controller/          # REST controllers (entry points)
│   └── dto/                 # Data Transfer Objects for API
│
├── 📁 application/
│   └── usecases/            # Business logic orchestration
│
├── 📁 domain/
│   ├── entity/              # Core business entities
│   └── repository/          # Repository interfaces (abstractions)
│
└── 📁 infrastructure/
    └── persistence/
        ├── repository/      # Repository implementations
        └── mapper/          # Entity/DTO mappers
```

## 🔍 Layer-by-Layer Explanation

### 1. **Presentation Layer** (`presentation/`)

**Purpose:** Handles HTTP requests and responses. This is the entry point for external clients.

**Components:**
- **Controllers:** REST endpoints that receive HTTP requests
- **DTOs (Data Transfer Objects):** Simple objects to transfer data between layers

**Example:** `FAGroupController.java`

```java
@RestController
public class FAGroupController {
    private final CreateFAGroup createUseCase;

    @PostMapping
    public void create(@RequestBody FAGroupDTO dto) {
        FAGroup entity = FAGroupDTOMapper.toEntity(dto);
        createUseCase.execute(entity);
    }
}
```

**Key Points:**
- Controllers are thin - they don't contain business logic
- They delegate work to use cases
- They use DTOs to communicate with the outside world
- They don't know about database implementation details

### 2. **Application Layer** (`application/`)

**Purpose:** Orchestrates business logic and coordinates different components. This layer contains "use cases" - specific business operations.

**Components:**
- **Use Cases:** Classes that implement specific business operations

**Example:** `CreateFAGroup.java`

```java
@Service
public class CreateFAGroup {
    private FAGroupRepository faGroupRepository;

    public void execute(FAGroup faGroup) {
        faGroupRepository.save(faGroup);
    }
}
```

**Key Points:**
- Use cases define what the system can do
- They coordinate between entities and repositories
- They contain business logic that spans multiple entities
- They depend on abstractions (repository interfaces), not implementations

### 3. **Domain Layer** (`domain/`)

**Purpose:** Contains the core business logic and rules. This is the most important layer and should be independent of everything else.

**Components:**
- **Entities:** Core business objects with behavior
- **Repository Interfaces:** Abstractions for data access

**Example - Entity:** `FAGroup.java`

```java
public class FAGroup {
    private String accountCode;
    private String accountDescription;
    private String accountType;
    private BigDecimal accountCurrentBalance;

    // Constructor, getters, setters...
}
```

**Example - Repository Interface:** `FAGroupRepository.java`

```java
public interface FAGroupRepository {
    void save(FAGroup faGroup);
    FAGroup findByCode(String aCode);
    List<FAGroup> findAll();
    void update(FAGroup faGroup);
    void delete(String aCode);
}
```

**Key Points:**
- **Entities** contain core business rules
- **Repository interfaces** define what data operations are needed
- This layer has NO dependencies on other layers
- Changes to UI, database, or frameworks don't affect this layer

### 4. **Infrastructure Layer** (`infrastructure/`)

**Purpose:** Provides concrete implementations for the abstractions defined in the domain layer. This is where external concerns like databases live.

**Components:**
- **Repository Implementations:** Actual database operations
- **Mappers:** Convert between entities and DTOs/database records

**Example - Repository Implementation:** `FAGroupRepositoryJDBC.java`

```java
@Repository
public class FAGroupRepositoryJDBC implements FAGroupRepository {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void save(FAGroup faGroup) {
        String sql = "INSERT INTO FAGroup (A_CODE, A_DESC, A_TYPE, A_CURRB) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, /* parameters */);
    }
}
```

**Example - Mapper:** `FAGroupDTOMapper.java`

```java
public class FAGroupDTOMapper {
    public static FAGroup toEntity(FAGroupDTO dto) {
        return new FAGroup(/* map fields */);
    }
}
```

**Key Points:**
- Implements interfaces defined in the domain layer
- Contains database-specific code
- Can be replaced without affecting business logic
- Depends on domain layer, domain layer doesn't depend on it

## 🔄 Dependency Flow

**The Golden Rule of Clean Architecture:**

```
Dependencies point INWARD, not OUTWARD!

Infrastructure → Domain ← Application ← Presentation
```

This means:
- **Presentation** depends on **Application**
- **Application** depends on **Domain**
- **Infrastructure** depends on **Domain**
- **Domain** depends on NOTHING

**Why this matters:**
- You can change your database without changing business logic
- You can change your UI framework without breaking anything
- Your core business rules are protected from external changes

## 💡 Real-World Example: Creating a FAGroup

Let's trace how a request flows through the architecture:

1. **Request comes in:**
   ```
   POST /fagroups
   Body: { "accountCode": "01", "accountDescription": "Asset", ... }
   ```

2. **Presentation Layer:**
   - `FAGroupController` receives the HTTP request
   - Converts `FAGroupDTO` to `FAGroup` entity using mapper
   - Calls `CreateFAGroup` use case

3. **Application Layer:**
   - `CreateFAGroup` use case executes
   - Calls repository interface method to save

4. **Domain Layer:**
   - `FAGroupRepository` interface defines the contract
   - `FAGroup` entity represents the business object

5. **Infrastructure Layer:**
   - `FAGroupRepositoryJDBC` implements the repository
   - Executes actual SQL to save to database

6. **Response flows back:**
   - Success response sent to client

## 🎯 Benefits of This Architecture

### For Beginners:
- **Clear separation:** Each layer has a specific responsibility
- **Easy to understand:** You know where to look for specific code
- **Better learning:** You can learn one layer at a time

### For Maintenance:
- **Easy to fix bugs:** Issues are isolated to specific layers
- **Safe refactoring:** Changes in one layer don't break others
- **Better testing:** Each layer can be tested independently

### For Scalability:
- **Easy to add features:** New features follow the same pattern
- **Team collaboration:** Different developers can work on different layers
- **Technology flexibility:** Can swap databases, frameworks, or UI easily

## 🛠️ Technology Stack

- **Framework:** Spring Boot 4.0.3
- **Language:** Java 17
- **Database:** SQLite
- **Build Tool:** Maven
- **API Documentation:** SpringDoc OpenAPI (Swagger)
- **Data Access:** Spring JDBC (JdbcTemplate)

## 🚀 Getting Started

### Prerequisites
- Java 17 or higher
- Maven 3.6+

### Running the Application

```bash
# Navigate to the backend directory
cd FA-PACKAGE/Backend

# Build the project
./mvnw clean install

# Run the application
./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`

### Accessing API Documentation

Once the application is running, access Swagger UI at:
```
http://localhost:8080/swagger-ui.html
```

## 📚 Key Architecture Patterns Used

### 1. **Dependency Inversion Principle (DIP)**
- High-level modules (use cases) don't depend on low-level modules (database implementations)
- Both depend on abstractions (repository interfaces)

### 2. **Dependency Injection**
- Spring Boot automatically wires dependencies
- Makes testing easier (can mock dependencies)
- Promotes loose coupling

### 3. **Separation of Concerns**
- Each layer has a single, well-defined responsibility
- Changes in one area don't affect others

### 4. **Interface Segregation**
- Repository interfaces are focused and specific
- Clients don't depend on methods they don't use

## 🧪 Testing the Architecture

Because of clean architecture, each layer can be tested independently:

```java
// Test domain logic without database
@Test
void testFAGroupCreation() {
    FAGroup group = new FAGroup("01", "Asset", "0", BigDecimal.ZERO);
    assertNotNull(group.getAccountCode());
}

// Test use cases with mocked repository
@Test
void testCreateFAGroupUseCase() {
    FAGroupRepository mockRepo = mock(FAGroupRepository.class);
    CreateFAGroup useCase = new CreateFAGroup(mockRepo);
    useCase.execute(new FAGroup("01", "Asset", "0", BigDecimal.ZERO));
    verify(mockRepo).save(any(FAGroup.class));
}
```

## 📖 Next Steps for Learning

1. **Study the existing code:** Look at how `FAGroup` flows through all layers
2. **Add a new entity:** Try creating a new entity following the same pattern
3. **Add a new use case:** Implement a new business operation
4. **Write tests:** Practice testing different layers independently

## 🤝 Contributing

When adding new features, always follow the clean architecture pattern:
1. Start with domain entities and repository interfaces
2. Implement use cases in the application layer
3. Create repository implementations in infrastructure layer
4. Add controllers and DTOs in presentation layer

Remember: **Dependencies should always point inward toward the domain layer!**

---

**Note:** This backend is part of a larger financial accounting system. The clean architecture ensures that as the system grows, it remains maintainable and adaptable to changing requirements.