# FA-PACKAGE — Backend API Documentation

REST API for the Financial Accounting backend (Spring Boot, plain JDBC). Double-entry
bookkeeping over Account Groups, Ledger Accounts, and Journal Vouchers, plus opening
balances, period locks, and financial reports.

- **Base URL:** `http://localhost:8080/api/v1`
- **Content type:** `application/json` for all request and response bodies
- **Interactive docs:** Swagger UI at `http://localhost:8080/swagger-ui.html`

---

## Conventions

### Data types used below

| Type in this doc | JSON form | Notes |
|---|---|---|
| `string` | string | |
| `integer` | number (no decimals) | |
| `long` | number (no decimals) | 64-bit; used for DB-generated ids |
| `decimal` | number | money/amount; up to 3 decimal places (`DECIMAL(18,3)`) |
| `datetime` | string | ISO-8601 local date-time, no zone — e.g. `"2026-06-05T00:00:00"` |
| `date` | string | ISO-8601 local date — e.g. `"2026-06-30"` |
| `boolean` | `true` / `false` | |
| `T[]` | array | array of type `T` |

### Standard error response

Every handled error returns this shape (`ErrorResponse`):

| Field | Type | Description |
|---|---|---|
| `timestamp` | datetime | when the error was produced |
| `status` | integer | HTTP status code |
| `error` | string | HTTP reason phrase (e.g. `"Bad Request"`) |
| `message` | string | human-readable detail |
| `path` | string | request URI that failed |

```json
{
  "timestamp": "2026-06-09T11:42:13.512",
  "status": 400,
  "error": "Bad Request",
  "message": "Voucher is not balanced: total debits (100) must equal total credits (200)",
  "path": "/api/v1/journal-vouchers"
}
```

### Status code map (global)

| Status | When |
|---|---|
| `200 OK` | successful GET / PUT |
| `201 Created` | successful resource creation / posting |
| `204 No Content` | successful DELETE |
| `400 Bad Request` | bean-validation failure, malformed/missing body, `IllegalArgumentException`, and `*ValidationException` — incl. **voucher not balanced**, duplicate line on the same side, amount ≤ 0, and **deleting a ledger that has postings** |
| `404 Not Found` | `*NotFoundException` |
| `409 Conflict` | duplicate / already-exists / already-reversed / period overlap / **deleting a posted voucher** |
| `422 Unprocessable Entity` | accounting-rule violation: period locked, inactive account, incompatible account type, **opening-balance** imbalance |
| `500 Internal Server Error` | unmapped exceptions (see `InvalidNormalBalanceException` below) |

> **Known quirks — verified live against the running server (2026-06-09):**
> - **Wrong normal-balance side** on a ledger throws `InvalidNormalBalanceException`, which has
>   no dedicated handler and surfaces as **500** (`"Unexpected error occurred"`) — unlike the
>   sibling incompatible-type check, which returns 422.
> - **Voucher imbalance returns 400**, not 422 (it's a `JournalMasterValidationException`), even
>   though the analogous **opening-balance imbalance returns 422**.
> - **`DELETE /journal-vouchers/{jId}` always returns 409** for any existing voucher: there is no
>   draft state yet, so every voucher is "posted" and undeletable (reverse it instead). Returns
>   404 if the id is unknown. The 204 path is reserved for a future draft status.
> - **`DELETE /ledger-accounts/{code}` with postings returns 400** (a `FASubGroupValidationException`),
>   not 422. The defined `FASubGroupHasTransactionsException` (422) is currently unused by the delete path.

---

## 1. Account Groups — `/api/v1/fagroups`

Top-level account classes (Assets, Liabilities, Equity, Income, Expense).

### `FAGroupDTO` (request & response)

| Field | Type | Required | Constraints |
|---|---|---|---|
| `accountCode` | string | yes | exactly 2 chars |
| `accountDescription` | string | yes | ≤ 50 chars |
| `accountType` | string | yes | one of `"0"`,`"1"`,`"2"`,`"3"`,`"4"` (0=Asset,1=Liability,2=Equity,3=Income,4=Expense) |
| `accountCurrentBalance` | decimal | no | aggregate balance; usually `0` |

### Endpoints

| Method | Path | Request body | Success | Response body |
|---|---|---|---|---|
| POST | `/fagroups` | `FAGroupDTO` | 201 | `FAGroupDTO` |
| GET | `/fagroups` | — | 200 | `FAGroupDTO[]` |
| GET | `/fagroups/{code}` | — | 200 | `FAGroupDTO` |
| PUT | `/fagroups/{code}` | `FAGroupDTO` | 200 | `FAGroupDTO` |
| GET | `/fagroups/{code}/subgroups` | — | 200 | `FAGroupWithSubGroupsDTO` |

- Path param `code`: string (2-char group code).
- Errors: 400 (validation), 404 (`{code}` not found), 409 (code already exists).

**`FAGroupWithSubGroupsDTO` (response):** all `FAGroupDTO` fields **plus** `subGroups: FASubGroupDTO[]`.

**Example — POST `/fagroups`**
```json
{ "accountCode": "10", "accountDescription": "Current Assets", "accountType": "0", "accountCurrentBalance": 0 }
```

---

## 2. Ledger Accounts — `/api/v1/ledger-accounts`

The chart of accounts (a.k.a. SubGroups). Each ledger belongs to one Account Group.

### `FASubGroupDTO` (request & response)

| Field | Type | Required | Constraints |
|---|---|---|---|
| `sCode` | string | yes | exactly 5 chars (ledger code) |
| `sDesc` | string | yes | ≤ 50 chars |
| `aCode` | string | yes | exactly 2 chars; parent group code (must exist) |
| `sType` | string | yes | exactly 2 chars matching `[0-4][0-9]`; **first digit must equal parent group's `accountType`** |
| `sOpbal` | decimal | no | ≥ 0, max 15 integer + 3 fraction digits; defaults `0` |
| `sDrCr` | string | yes* | `"DR"` or `"CR"` (case-insensitive); **must match the natural side** for the class (Asset/Expense→DR, Liability/Equity/Income→CR) |
| `sFlag` | string | no | `"T"` (active) or `"F"` (inactive); defaults `"T"` |

\* validated as a pattern when present; business logic requires a valid DR/CR.

### Endpoints

| Method | Path | Request body | Success | Response body |
|---|---|---|---|---|
| POST | `/ledger-accounts` | `FASubGroupDTO` | 201 | `FASubGroupDTO` |
| GET | `/ledger-accounts` | — | 200 | `FASubGroupDTO[]` |
| GET | `/ledger-accounts/{code}` | — | 200 | `FASubGroupDTO` |
| PUT | `/ledger-accounts/{code}` | `FASubGroupDTO` | 200 | `FASubGroupDTO` |
| DELETE | `/ledger-accounts/{code}` | — | 204 | — |

- Path param `code`: string (5-char ledger code).
- `DELETE` returns 204 only when the ledger has **no postings**; if it has journal entries the
  delete is blocked with **400**.
- Errors: 400 (validation / bad DR/CR; also delete blocked when the ledger has postings),
  404 (ledger or parent group not found), 409 (already exists),
  422 (`IncompatibleAccountTypeException`),
  500 (`InvalidNormalBalanceException` — see quirks above).

**Example — POST `/ledger-accounts`**
```json
{ "sCode": "10001", "sDesc": "Cash in Hand", "aCode": "10", "sType": "00", "sOpbal": 0, "sDrCr": "DR", "sFlag": "T" }
```

---

## 3. Journal Vouchers — `/api/v1/journal-vouchers`  (write path)

The **only** way to create/remove postings. Posts a complete, balanced voucher
(header + lines) atomically; total debits must equal total credits.

### Request: `JournalVoucherRequestDTO`

| Field | Type | Required | Constraints |
|---|---|---|---|
| `jDoc` | string | no | ≤ 2 chars; server defaults to `"JV"` |
| `jDate` | datetime | no | server defaults to now if omitted |
| `jNarr` | string | yes | 5–100 chars |
| `lines` | `Line[]` | yes | at least 2 lines |

**`Line`**

| Field | Type | Required | Constraints |
|---|---|---|---|
| `jCode` | string | yes | exactly 5 chars; ledger must exist **and be active** (`sFlag="T"`) |
| `jDrCr` | string | yes | `"DR"` or `"CR"` (case-insensitive) |
| `jAmount` | decimal | yes | ≥ 0.001 (greater than zero) |

Additional server rules: total debits must equal total credits; the same account
may not appear twice on the same side; the voucher date must not fall in a locked period.

### Response: `JournalMasterDTO`

| Field | Type | Description |
|---|---|---|
| `jId` | string | server-generated id, e.g. `"JV2026000001"` |
| `jDoc` | string | document type (`"JV"`, or `"OB"` for opening balance) |
| `jDate` | datetime | voucher date |
| `jAmount` | decimal | total, derived from lines (never trusted from client) |
| `jNarr` | string | narration (trimmed) |

### Endpoints

| Method | Path | Path params | Request body | Success | Response body |
|---|---|---|---|---|---|
| POST | `/journal-vouchers` | — | `JournalVoucherRequestDTO` | 201 | `JournalMasterDTO` |
| POST | `/journal-vouchers/{jId}/reverse` | `jId`: string | — | 201 | `JournalMasterDTO` (the reversal) |
| DELETE | `/journal-vouchers/{jId}` | `jId`: string | — | 409* | `ErrorResponse` |

\* Currently every existing voucher is posted and **cannot be deleted → 409** (reverse it
instead); 404 if the id is unknown. The 204 success path is reserved for a future draft state.

- Errors: 400 (validation incl. **not balanced**, duplicate line on the same side, amount ≤ 0),
  404 (`{jId}` not found on reverse/delete),
  409 (`VoucherAlreadyReversedException`; `PostedVoucherCannotBeDeletedException` on delete),
  422 (inactive account, period locked).

**Example — POST `/journal-vouchers`**
```json
{
  "jDoc": "JV",
  "jDate": "2026-06-05T00:00:00",
  "jNarr": "Cash sales deposited to bank",
  "lines": [
    { "jCode": "10002", "jDrCr": "DR", "jAmount": 30000 },
    { "jCode": "40001", "jDrCr": "CR", "jAmount": 30000 }
  ]
}
```

---

## 4. Journal Masters — `/api/v1/journal-masters`  (read-only)

Voucher headers. No create/update/delete — writes go through `/journal-vouchers`.

| Method | Path | Path params | Success | Response body |
|---|---|---|---|---|
| GET | `/journal-masters` | — | 200 | `JournalMasterDTO[]` |
| GET | `/journal-masters/{jId}` | `jId`: string | 200 | `JournalMasterDTO` |

- Errors: 404 (`{jId}` not found).
- `JournalMasterDTO` shape: see section 3.

---

## 5. Journal Details — `/api/v1/journal-details`  (read-only)

Voucher line items. No create/update/delete — writes go through `/journal-vouchers`.

### Response: `JournalDetailDTO`

| Field | Type | Description |
|---|---|---|
| `jId` | string | parent voucher id |
| `jCode` | string | ledger account code (5 chars) |
| `jDrCr` | string | `"DR"` or `"CR"` |
| `jAmount` | decimal | line amount |

### Endpoints

| Method | Path | Path params | Success | Response body |
|---|---|---|---|---|
| GET | `/journal-details` | — | 200 | `JournalDetailDTO[]` |
| GET | `/journal-details/{jId}/{jCode}/{jDrCr}` | `jId`,`jCode`,`jDrCr`: string | 200 | `JournalDetailDTO` |
| GET | `/journal-details/journal/{jId}` | `jId`: string | 200 | `JournalDetailDTO[]` (all lines of one voucher) |
| GET | `/journal-details/account/{jCode}` | `jCode`: string | 200 | `JournalDetailDTO[]` (all lines for one ledger) |

- Errors: 404 (composite key not found on the single-item GET).

---

## 6. Opening Balances — `/api/v1/opening-balances`

Initializes the books **once** with a balanced Opening Balance voucher (`jDoc="OB"`).
Total opening debits must equal total opening credits.

### Request: `OpeningBalanceImportDTO`

| Field | Type | Required | Constraints |
|---|---|---|---|
| `openingDate` | date | no | defaults to today |
| `narration` | string | no | ≤ 100 chars; defaults to `"Opening Balance"` |
| `lines` | `Line[]` | yes | at least 2 lines |

**`Line`**

| Field | Type | Required | Constraints |
|---|---|---|---|
| `accountCode` | string | yes | exactly 5 chars; ledger must exist and be active |
| `drCr` | string | yes | `"DR"` or `"CR"` |
| `amount` | decimal | yes | ≥ 0.001 |

### Endpoint

| Method | Path | Request body | Success | Response body |
|---|---|---|---|---|
| POST | `/opening-balances/import` | `OpeningBalanceImportDTO` | 201 | `JournalMasterDTO` (the created `OB` voucher) |

- Errors: 400 (validation), 422 (`OpeningBalanceImbalanceException` — debits ≠ credits),
  409 (`OpeningBalanceAlreadyInitializedException` — books already initialized).

**Example — POST `/opening-balances/import`**
```json
{
  "openingDate": "2026-06-01",
  "narration": "Opening balances FY26",
  "lines": [
    { "accountCode": "10001", "drCr": "DR", "amount": 20000 },
    { "accountCode": "10002", "drCr": "DR", "amount": 80000 },
    { "accountCode": "30001", "drCr": "CR", "amount": 100000 }
  ]
}
```

---

## 7. Period Locks — `/api/v1/period-locks`

Closed accounting periods. Postings/reversals dated inside a locked period are rejected.

### Request: `PeriodLockRequestDTO`

| Field | Type | Required | Constraints |
|---|---|---|---|
| `periodStart` | date | yes | inclusive start; must be ≤ `periodEnd` |
| `periodEnd` | date | yes | inclusive end |

### Response: `PeriodLock`

| Field | Type | Description |
|---|---|---|
| `id` | long | DB-generated id |
| `periodStart` | date | inclusive start |
| `periodEnd` | date | inclusive end |
| `lockedAt` | datetime | when the lock was created |
| `lockedBy` | string | actor (`"SYSTEM"` until auth is added) |

### Endpoints

| Method | Path | Path params | Request body | Success | Response body |
|---|---|---|---|---|---|
| POST | `/period-locks` | — | `PeriodLockRequestDTO` | 201 | `PeriodLock` |
| GET | `/period-locks` | — | — | 200 | `PeriodLock[]` |
| DELETE | `/period-locks/{id}` | `id`: long | — | 204 | — |

- Errors: 400 (missing fields, or `periodStart` after `periodEnd`),
  409 (`PeriodLockOverlapException` — overlaps/duplicates an existing lock).

**Example — POST `/period-locks`**
```json
{ "periodStart": "2026-06-01", "periodEnd": "2026-06-30" }
```

---

## 8. Reports — `/api/v1/reports`

All three reports accept an optional `asOfDate` query param and default to today.

- **Query param** `asOfDate`: date (`yyyy-MM-dd`), optional. Scopes the report to opening
  balances and vouchers dated **on or before** that date.

| Method | Path | Query | Success | Response body |
|---|---|---|---|---|
| GET | `/reports/trial-balance` | `asOfDate?` | 200 | `TrialBalanceDTO` |
| GET | `/reports/profit-and-loss` | `asOfDate?` | 200 | `ProfitAndLossDTO` |
| GET | `/reports/balance-sheet` | `asOfDate?` | 200 | `BalanceSheetDTO` |

### `TrialBalanceDTO`

| Field | Type | Description |
|---|---|---|
| `asOfDate` | date | reporting date |
| `rows` | `Row[]` | one per account with a balance |
| `totalDebit` | decimal | sum of all debit balances |
| `totalCredit` | decimal | sum of all credit balances |
| `balanced` | boolean | `totalDebit == totalCredit` |

**`Row`:** `accountCode: string`, `description: string`, `debit: decimal`, `credit: decimal`.

### `ProfitAndLossDTO`

| Field | Type | Description |
|---|---|---|
| `asOfDate` | date | reporting date |
| `revenue` | `LineItem[]` | income accounts |
| `expenses` | `LineItem[]` | expense accounts |
| `totalRevenue` | decimal | |
| `totalExpenses` | decimal | |
| `netProfit` | decimal | `totalRevenue - totalExpenses` |

**`LineItem`:** `accountCode: string`, `description: string`, `amount: decimal`.

### `BalanceSheetDTO`

| Field | Type | Description |
|---|---|---|
| `asOfDate` | date | reporting date |
| `assets` | `LineItem[]` | asset accounts |
| `liabilities` | `LineItem[]` | liability accounts |
| `equity` | `LineItem[]` | equity accounts, incl. a `"Net Income"` line for unclosed earnings |
| `totalAssets` | decimal | |
| `totalLiabilities` | decimal | |
| `totalEquity` | decimal | |
| `balanced` | boolean | `totalAssets == totalLiabilities + totalEquity` |

**`LineItem`:** `accountCode: string`, `description: string`, `amount: decimal`.

**Example — GET `/reports/trial-balance?asOfDate=2026-06-30` → 200**
```json
{
  "asOfDate": "2026-06-30",
  "rows": [
    { "accountCode": "10001", "description": "Cash in Hand", "debit": 20000, "credit": 0 },
    { "accountCode": "40001", "description": "Sales Revenue", "debit": 0, "credit": 30000 }
  ],
  "totalDebit": 155000,
  "totalCredit": 155000,
  "balanced": true
}
```

---

## Endpoint index

| # | Method | Path | Auth |
|---|---|---|---|
| 1 | POST | `/api/v1/fagroups` | none |
| 2 | GET | `/api/v1/fagroups` | none |
| 3 | GET | `/api/v1/fagroups/{code}` | none |
| 4 | PUT | `/api/v1/fagroups/{code}` | none |
| 5 | GET | `/api/v1/fagroups/{code}/subgroups` | none |
| 6 | POST | `/api/v1/ledger-accounts` | none |
| 7 | GET | `/api/v1/ledger-accounts` | none |
| 8 | GET | `/api/v1/ledger-accounts/{code}` | none |
| 9 | PUT | `/api/v1/ledger-accounts/{code}` | none |
| 10 | DELETE | `/api/v1/ledger-accounts/{code}` | none |
| 11 | POST | `/api/v1/journal-vouchers` | none |
| 12 | POST | `/api/v1/journal-vouchers/{jId}/reverse` | none |
| 13 | DELETE | `/api/v1/journal-vouchers/{jId}` | none |
| 14 | GET | `/api/v1/journal-masters` | none |
| 15 | GET | `/api/v1/journal-masters/{jId}` | none |
| 16 | GET | `/api/v1/journal-details` | none |
| 17 | GET | `/api/v1/journal-details/{jId}/{jCode}/{jDrCr}` | none |
| 18 | GET | `/api/v1/journal-details/journal/{jId}` | none |
| 19 | GET | `/api/v1/journal-details/account/{jCode}` | none |
| 20 | POST | `/api/v1/opening-balances/import` | none |
| 21 | POST | `/api/v1/period-locks` | none |
| 22 | GET | `/api/v1/period-locks` | none |
| 23 | DELETE | `/api/v1/period-locks/{id}` | none |
| 24 | GET | `/api/v1/reports/trial-balance` | none |
| 25 | GET | `/api/v1/reports/profit-and-loss` | none |
| 26 | GET | `/api/v1/reports/balance-sheet` | none |

> Authentication is not yet wired in; all endpoints are currently open and audit
> entries are attributed to `"SYSTEM"`.
