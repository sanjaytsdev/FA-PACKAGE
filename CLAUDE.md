# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

FA-PACKAGE is a Financial Accounting application built with Spring Boot 3.3.5 (Java 17). It implements double-entry bookkeeping with Account Groups, Ledger Accounts (SubGroups), and Journal Vouchers.

## Build and Run Commands

### Local Development (SQLite)
```bash
cd Backend
./mvnw spring-boot:run
```

### Using Docker (MySQL)
```bash
docker-compose up --build
```

### Build
```bash
cd Backend
./mvnw clean package
```

### Run Tests
```bash
cd Backend
./mvnw test
```

## Architecture

The application follows Clean/Hexagonal Architecture with four main layers:

### Domain Layer (`domain/`)
Contains core business entities and repository interfaces:
- **Entities**: `FAGroup`, `FASubGroup`, `JournalMaster`, `JournalDetail`
- **Repository Interfaces**: Define contracts for data access (e.g., `FAGroupRepository`)

### Application Layer (`application/usecases/`)
Contains business logic/use cases that orchestrate domain operations:
- `CreateFAGroup`, `UpdateFAGroup`, `GetAllFAGroup`, `GetFAGroupWithSubGroups`
- `CreateFASubGroup`, `CreateJournalDetail`
- Each use case is a Spring `@Service` that validates business rules and delegates to repositories

### Infrastructure Layer (`infrastructure/persistence/`)
Contains JDBC implementations of repository interfaces:
- Repository classes (e.g., `FAGroupRepositoryJDBC`) implement domain interfaces
- RowMappers map SQL result sets to domain entities
- DTOMappers convert between DTOs and domain entities

### Presentation Layer (`presentation/`)
Contains REST API controllers and DTOs:
- Controllers handle HTTP requests and delegate to use cases
- DTOs define the API contract
- Custom exceptions (`FAGroupNotFoundException`, `FAGroupAlreadyExistsException`, etc.) with `GlobalExceptionHandler`

## Database

The application uses **plain JDBC** (NOT JPA/Hibernate) for database access.

### SQLite (Default)
- Database file: `Backend/sqlite/FA.db`
- Schema auto-loaded from `Backend/src/main/resources/schema.sql` on startup

### MySQL (Docker / Production)
- Runs as the `mysql:8.0` service defined in `docker-compose.yml` (credentials via environment variables)
- Schema auto-loaded from `Backend/src/main/resources/schema-mysql.sql` on startup (the `docker` and `prod` profiles use the MySQL driver)

## Database Schema

- **FAGroup**: Account groups (Assets, Liabilities, Equity, Income, Expense)
  - `A_CODE` (2-char PK), `A_DESC`, `A_TYPE` (0-4), `A_CURRB`
- **FASubGroup**: Ledger accounts
  - `S_CODE` (5-char PK), `S_DESC`, `A_CODE` (FK), `S_TYPE`, `S_OPBAL`, `S_DRCR`, `S_FLAG`
- **JournalMaster**: Journal voucher headers
  - `J_ID` (10-char PK), `J_DOC`, `J_DATE`, `J_AMOUNT`, `J_NARR`
- **JournalDetail**: Journal line items
  - Composite PK (`J_ID`, `J_CODE`, `J_DRCR`), `J_AMOUNT`

## API Documentation

Swagger UI available at: `http://localhost:8080/swagger-ui.html`

## Code Conventions

- Use plain JDBC with `JdbcTemplate` for database operations
- Follow the layered architecture pattern when adding new features
- Create separate use case classes for each business operation
- Use DTOs for API contracts, convert to/from domain entities in controllers
- Throw custom domain exceptions with descriptive messages
- Account Types: '0'=Asset, '1'=Liability, '2'=Equity, '3'=Income, '4'=Expense
- Debit/Credit values: 'DR' or 'CR'
