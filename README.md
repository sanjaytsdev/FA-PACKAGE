# FA-PACKAGE — Financial Accounting

A double-entry bookkeeping application built with **Spring Boot 3.3.5 (Java 17)** and a **React + Vite + Tailwind** web frontend. It manages Account Groups, Ledger Accounts, and Journal Vouchers, and produces standard accounting reports (Trial Balance, Profit & Loss, Balance Sheet).

## Features

- **Chart of Accounts** — Account Groups (Assets, Liabilities, Equity, Income, Expense) and Ledger Accounts (sub-groups).
- **Journal Vouchers** — double-entry postings with automatic, atomic voucher-ID generation and voucher reversal.
- **Opening Balances** — import opening balances as a balanced opening-balance voucher.
- **Period Locks** — close accounting periods to reject further postings.
- **Reports** — Trial Balance, Profit & Loss, and Balance Sheet, each computed "as of" a chosen date.
- **Audit Log** — append-only trail of every successful write.

## Architecture

The backend follows a Clean / Hexagonal architecture with four layers:

| Layer | Location | Responsibility |
|-------|----------|----------------|
| Domain | `domain/` | Core entities (`FAGroup`, `FASubGroup`, `JournalMaster`, `JournalDetail`) and repository interfaces |
| Application | `application/usecases/` | Business logic — one `@Service` use case per operation |
| Infrastructure | `infrastructure/persistence/` | Plain-JDBC repository implementations (`JdbcTemplate`), RowMappers |
| Presentation | `presentation/` | REST controllers, DTOs, and exception handling |

Data access uses **plain JDBC** (no JPA/Hibernate). The REST API is served under `/api/v1/...`.

## Tech Stack

- **Backend:** Java 17, Spring Boot 3.3.5, Spring JDBC, HikariCP
- **Databases:** SQLite (local dev), MySQL 8 (Docker / production)
- **Frontend:** React 19, Vite, TypeScript, Tailwind CSS
- **Desktop (optional):** JavaFX client under `Frontend/DesktopApp`
- **API docs:** Swagger UI

## Project Structure

```
FA-PACKAGE/
├── Backend/                  # Spring Boot REST API
│   └── src/main/resources/   # schema.sql (SQLite), schema-mysql.sql, profiles
├── Frontend/
│   ├── web-app/              # React + Vite web client
│   └── DesktopApp/           # JavaFX desktop client (optional)
└── docker-compose.yml        # MySQL + backend + frontend stack
```

## Getting Started

### Prerequisites

- Java 17+
- Node.js 22+ (for the web frontend)
- Docker & Docker Compose (only for the containerized stack)

### Option 1 — Run everything with Docker (MySQL)

The simplest way to run the full stack (MySQL + backend + frontend):

```bash
docker-compose up --build
```

- Web app: http://localhost:5173
- Backend API: http://localhost:8080/api/v1
- Swagger UI: http://localhost:8080/swagger-ui.html

### Option 2 — Run locally for development

**1. Backend (SQLite, default `dev` profile):**

```bash
cd Backend
./mvnw spring-boot:run
```

The API starts on http://localhost:8080. The SQLite database (`Backend/sqlite/FA.db`) and schema are created automatically on first run.

**2. Web frontend:**

```bash
cd Frontend/web-app
npm install
npm run dev
```

The web app starts on http://localhost:5173 and talks to the backend at `http://localhost:8080/api/v1` by default.

## Build

**Backend (executable jar):**

```bash
cd Backend
./mvnw clean package
```

**Frontend (static build):**

```bash
cd Frontend/web-app
npm run build
```

## Run Tests

```bash
cd Backend
./mvnw test
```

## Configuration Profiles

| Profile | Database | Activated by |
|---------|----------|--------------|
| `dev` (default) | SQLite | `spring.profiles.active=dev` |
| `docker` | MySQL 8 | `SPRING_PROFILES_ACTIVE=docker` (set in `docker-compose.yml`) |
| `prod` | MySQL + Redis | `SPRING_PROFILES_ACTIVE=prod` |

## API Documentation

With the backend running, the interactive Swagger UI is available at:

```
http://localhost:8080/swagger-ui.html
```

For the full REST endpoint reference, see the [Backend API Documentation](Backend/API_DOCUMENTATION.md).

### Frontend API clients

Each frontend wraps the backend REST API in a typed client. See:

- [Web App API client](Frontend/web-app/API.md) — `apiClient` (fetch + TypeScript)
- [Desktop App API client](Frontend/DesktopApp/API.md) — `ApiClient` (Java `HttpClient` + Jackson)

## Contributors

Roles below reflect each person's contributions as recorded in the git history.

- **Sanjay** — [LinkedIn](https://www.linkedin.com/in/sanjay-t-s-312452313/)
  Laid the **backend foundation**: initial project setup, the clean/hexagonal architecture base,
  the plain-JDBC + SQLite schema, FAGroup and Chart of Accounts endpoints, custom exception
  handlers, and the initial Docker/Maven build setup.

- **Ashna** — [LinkedIn](https://www.linkedin.com/in/ashnacp/)
  Contributed **backend use cases and controllers** (e.g. Journal Master controller and
  `GetAllJournalMaster`, FASubGroup unit tests) and served as the team's **integrator**,
  reviewing and merging the bulk of the feature pull requests.

- **Amith Krishnan** — [LinkedIn](https://www.linkedin.com/in/amith-krishnan-developer/)
  Built **both frontends** — the React/Vite/Tailwind web app and the JavaFX desktop client
  (views, models, API clients, styling) — and worked across the backend on the **test suite**,
  the **CORS filter**, **configuration profiles** (dev/docker/prod), and repository/build hygiene.
