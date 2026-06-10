# Desktop App — API Client Documentation

This document describes the API layer of the JavaFX desktop client: the `ApiClient` class in
[`src/main/java/com/spam/financialaccounting/desktop/api/ApiClient.java`](src/main/java/com/spam/financialaccounting/desktop/api/ApiClient.java)
and the model classes under
[`.../desktop/model/`](src/main/java/com/spam/financialaccounting/desktop/model).
It wraps the backend REST API (`/api/v1/...`) using Java's built-in `java.net.http.HttpClient`
and Jackson for JSON.

## Configuration

The base URL is resolved once at class load by `loadBaseUrl()`:

1. If `config.properties` exists in the working directory, read `api.base.url` from it.
2. Otherwise create `config.properties` with the default and use it.

| Setting | Value |
|---------|-------|
| Base URL | `api.base.url` in `config.properties`, default `http://localhost:8080/api/v1` |
| Connect timeout | 5 s (`HttpClient`); 3 s for `pingAsync()` |
| JSON | Jackson `ObjectMapper` + `JavaTimeModule`; unknown JSON properties ignored; dates written as ISO-8601 strings (not numeric arrays) |

`config.properties` example:

```properties
api.base.url=http://localhost:8080/api/v1
```

## Threading & error handling

- Most methods are **synchronous and blocking** and declare `throws IOException, InterruptedException`.
  Call them off the JavaFX Application Thread (the views use `CompletableFuture.runAsync(...)` and
  marshal results back with `Platform.runLater`).
- `pingAsync()` is the only async method — returns `CompletableFuture<Boolean>` and never throws
  (`exceptionally` maps failures to `false`).
- `handleErrorResponse(...)` inspects every response: on status **≥ 400** it parses Spring Boot's
  error body and throws `IOException` with the body's `message` (or `error`) field, falling back to
  `HTTP Status Code <status>`.

## Method reference

All paths are relative to the base URL. Unless noted, methods `throw IOException, InterruptedException`.

### Connection health
| Method | HTTP | Path | Returns |
|--------|------|------|---------|
| `pingAsync()` | GET | `/fagroups` | `CompletableFuture<Boolean>` (true if HTTP 200) |

### Account Groups
| Method | HTTP | Path | Body | Returns |
|--------|------|------|------|---------|
| `getGroups()` | GET | `/fagroups` | — | `List<FAGroup>` |
| `createGroup(FAGroup)` | POST | `/fagroups` | `FAGroup` | `FAGroup` |
| `updateGroup(String code, FAGroup)` | PUT | `/fagroups/{code}` | `FAGroup` | `FAGroup` |

> Note: the desktop client does not expose a `deleteGroup` method (the web client does).

### Ledger Accounts (Sub-Groups)
| Method | HTTP | Path | Body | Returns |
|--------|------|------|------|---------|
| `getLedgerAccounts()` | GET | `/ledger-accounts` | — | `List<FASubGroup>` |
| `createLedgerAccount(FASubGroup)` | POST | `/ledger-accounts` | `FASubGroup` | `FASubGroup` |
| `updateLedgerAccount(String code, FASubGroup)` | PUT | `/ledger-accounts/{code}` | `FASubGroup` | `FASubGroup` |
| `deleteLedgerAccount(String code)` | DELETE | `/ledger-accounts/{code}` | — | `void` |

### Journal Masters
| Method | HTTP | Path | Body | Returns |
|--------|------|------|------|---------|
| `getJournalMasters()` | GET | `/journal-masters` | — | `List<JournalMaster>` |
| `createJournalMaster(JournalMaster)` | POST | `/journal-masters` | `JournalMaster` | `JournalMaster` |
| `deleteJournalMaster(String jId)` | DELETE | `/journal-masters/{jId}` | — | `void` |

### Journal Details
| Method | HTTP | Path | Body | Returns |
|--------|------|------|------|---------|
| `getJournalDetailsByJournalId(String jId)` | GET | `/journal-details/journal/{jId}` | — | `List<JournalDetail>` |
| `createJournalDetail(JournalDetail)` | POST | `/journal-details` | `JournalDetail` | `JournalDetail` |
| `deleteJournalDetail(String jId, String jCode, String jDrCr)` | DELETE | `/journal-details/{jId}/{jCode}/{jDrCr}` | — | `void` |

> The direct master/detail write methods bypass voucher-level balancing. Prefer the Journal
> Voucher endpoints below for normal posting; the web client intentionally exposes only the
> voucher path.

### Journal Vouchers (balanced, atomic)
| Method | HTTP | Path | Body | Returns |
|--------|------|------|------|---------|
| `postJournalVoucher(Object request)` | POST | `/journal-vouchers` | voucher request | `JournalMaster` |
| `deleteJournalVoucher(String jId)` | DELETE | `/journal-vouchers/{jId}` | — | `void` |
| `reverseJournalVoucher(String jId)` | POST | `/journal-vouchers/{jId}/reverse` | — | `JournalMaster` (new reversing voucher) |

The request passed to `postJournalVoucher` is serialized as-is; it should match the backend's
voucher request shape: `{ jDoc?, jDate?, jNarr?, lines: [{ jCode, jDrCr, jAmount }] }`.

### Opening Balances
| Method | HTTP | Path | Body | Returns |
|--------|------|------|------|---------|
| `importOpeningBalances(Object request)` | POST | `/opening-balances/import` | import request | `JournalMaster` |

Request shape: `{ openingDate?, narration?, lines: [{ accountCode, drCr, amount }] }`.

### Period Locks
| Method | HTTP | Path | Body | Returns |
|--------|------|------|------|---------|
| `getPeriodLocks()` | GET | `/period-locks` | — | `List<PeriodLock>` |
| `createPeriodLock(Object request)` | POST | `/period-locks` | `{ periodStart, periodEnd }` | `PeriodLock` |
| `deletePeriodLock(long id)` | DELETE | `/period-locks/{id}` | — | `void` |

### Reports
`asOfDate` is an ISO `yyyy-MM-dd` string; pass `null` (or blank) to report as of today.

| Method | HTTP | Path | Returns |
|--------|------|------|---------|
| `getTrialBalance(String asOfDate)` | GET | `/reports/trial-balance[?asOfDate=]` | `TrialBalance` |
| `getProfitAndLoss(String asOfDate)` | GET | `/reports/profit-and-loss[?asOfDate=]` | `ProfitAndLoss` |
| `getBalanceSheet(String asOfDate)` | GET | `/reports/balance-sheet[?asOfDate=]` | `BalanceSheet` |

## Model classes

Located in `.../desktop/model/`. JSON field names match the backend (and the web app's types);
Java types are shown below.

| Model | Fields |
|-------|--------|
| `FAGroup` | `String accountCode`, `String accountDescription`, `String accountType`, `BigDecimal accountCurrentBalance` |
| `FASubGroup` | `sCode`, `sDesc`, `aCode`, `sType` (String), `sOpbal` (BigDecimal), `sDrCr`, `sFlag` (String) |
| `JournalMaster` | `String jId`, `String jDoc`, `LocalDateTime jDate`, `BigDecimal jAmount`, `String jNarr` |
| `JournalDetail` | `jId`, `jCode`, `jDrCr` (String), `jAmount` (BigDecimal) |
| `TrialBalance` | `String asOfDate`, `List<TrialBalanceRow> rows`, `BigDecimal totalDebit`, `BigDecimal totalCredit`, `boolean balanced` |
| `TrialBalanceRow` | `accountCode`, `description` (String), `debit`, `credit` (BigDecimal) |
| `ProfitAndLoss` | `asOfDate`, `List<ReportLineItem> revenue/expenses`, `totalRevenue`, `totalExpenses`, `netProfit` |
| `BalanceSheet` | `asOfDate`, `List<ReportLineItem> assets/liabilities/equity`, `totalAssets`, `totalLiabilities`, `totalEquity`, `boolean balanced` |
| `ReportLineItem` | `String accountCode`, `String description`, `BigDecimal amount` |
| `PeriodLock` | `long id`, `String periodStart`, `String periodEnd`, `String lockedAt`, `String lockedBy` |

## Usage example

```java
ApiClient api = new ApiClient();

// Off the JavaFX thread (see TrialBalanceView):
CompletableFuture.runAsync(() -> {
    try {
        TrialBalance tb = api.getTrialBalance("2026-06-09");
        Platform.runLater(() -> render(tb));
    } catch (IOException | InterruptedException ex) {
        Platform.runLater(() -> showError(ex.getMessage())); // backend message
    }
});

// Connection check (async, never throws):
api.pingAsync().thenAccept(online -> Platform.runLater(() -> setStatus(online)));
```
