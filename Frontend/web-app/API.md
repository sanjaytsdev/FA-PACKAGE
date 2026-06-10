# Web App — API Client Documentation

This document describes the API layer of the React web app: the `apiClient` wrapper in
[`src/api/apiClient.ts`](src/api/apiClient.ts) and the TypeScript types in
[`src/types/index.ts`](src/types/index.ts). The client is a thin wrapper over the backend
REST API (`/api/v1/...`) using the browser `fetch` API.

## Configuration

| Setting | Value |
|---------|-------|
| Base URL | `import.meta.env.VITE_API_BASE_URL`, falling back to `http://localhost:8080/api/v1` |
| Request timeout | 8000 ms (`AbortSignal.timeout`), 3000 ms for `ping()` |
| Content type (writes) | `application/json` |

`VITE_API_BASE_URL` is baked in at build time. In Docker it is set to `/api/v1` so the browser
talks to the backend same-origin through the nginx `/api/` reverse proxy (no CORS).

## Error & response handling

All calls go through `handleResponse<T>(response)`:

- **Non-2xx** → throws `Error` whose message is the backend body's `message` or `error` field,
  falling back to `HTTP Error <status>`.
- **204 No Content** → resolves to `null`.
- **Otherwise** → resolves to the parsed JSON body typed as `T`.
- **Network error / timeout** → the underlying `fetch` rejection propagates to the caller.

Every method is `async` and returns a `Promise`. Callers should `try/catch` (see the views,
e.g. `TrialBalanceView.tsx`).

## Method reference

All paths below are relative to the base URL.

### Connection health
| Method | HTTP | Path | Returns |
|--------|------|------|---------|
| `ping()` | GET | `/fagroups` | `Promise<boolean>` — `true` if reachable (never throws) |

### Account Groups
| Method | HTTP | Path | Request | Returns |
|--------|------|------|---------|---------|
| `getGroups()` | GET | `/fagroups` | — | `FAGroup[]` |
| `createGroup(group)` | POST | `/fagroups` | `FAGroup` | `FAGroup` |
| `updateGroup(code, group)` | PUT | `/fagroups/{code}` | `FAGroup` | `FAGroup` |
| `deleteGroup(code)` | DELETE | `/fagroups/{code}` | — | `void` |

### Ledger Accounts (Sub-Groups)
| Method | HTTP | Path | Request | Returns |
|--------|------|------|---------|---------|
| `getLedgers()` | GET | `/ledger-accounts` | — | `FASubGroup[]` |
| `createLedger(ledger)` | POST | `/ledger-accounts` | `FASubGroup` | `FASubGroup` |
| `updateLedger(code, ledger)` | PUT | `/ledger-accounts/{code}` | `FASubGroup` | `FASubGroup` |
| `deleteLedger(code)` | DELETE | `/ledger-accounts/{code}` | — | `void` |

### Journals (read-only)
Writes are **not** exposed here — journal data is created/changed only through Journal Vouchers
so that postings always balance atomically.

| Method | HTTP | Path | Request | Returns |
|--------|------|------|---------|---------|
| `getJournalMasters()` | GET | `/journal-masters` | — | `JournalMaster[]` |
| `getJournalDetails(jId)` | GET | `/journal-details/journal/{jId}` | — | `JournalDetail[]` |

### Journal Vouchers (balanced, atomic)
| Method | HTTP | Path | Request | Returns |
|--------|------|------|---------|---------|
| `postJournalVoucher(voucher)` | POST | `/journal-vouchers` | `JournalVoucherRequest` | `JournalMaster` |
| `deleteJournalVoucher(jId)` | DELETE | `/journal-vouchers/{jId}` | — | `void` |
| `reverseJournalVoucher(jId)` | POST | `/journal-vouchers/{jId}/reverse` | — | `JournalMaster` (the new reversing voucher) |

### Opening Balances
| Method | HTTP | Path | Request | Returns |
|--------|------|------|---------|---------|
| `importOpeningBalances(request)` | POST | `/opening-balances/import` | `OpeningBalanceImportRequest` | `JournalMaster` (the opening-balance voucher) |

### Period Locks
| Method | HTTP | Path | Request | Returns |
|--------|------|------|---------|---------|
| `getPeriodLocks()` | GET | `/period-locks` | — | `PeriodLock[]` |
| `createPeriodLock(request)` | POST | `/period-locks` | `PeriodLockRequest` | `PeriodLock` |
| `deletePeriodLock(id)` | DELETE | `/period-locks/{id}` | — | `void` |

### Reports
`asOfDate` is an optional ISO `yyyy-MM-dd` string limiting the report to vouchers posted on or
before that date; omit it to report as of today.

| Method | HTTP | Path | Returns |
|--------|------|------|---------|
| `getTrialBalance(asOfDate?)` | GET | `/reports/trial-balance[?asOfDate=]` | `TrialBalance` |
| `getProfitAndLoss(asOfDate?)` | GET | `/reports/profit-and-loss[?asOfDate=]` | `ProfitAndLoss` |
| `getBalanceSheet(asOfDate?)` | GET | `/reports/balance-sheet[?asOfDate=]` | `BalanceSheet` |

## Types

Defined in [`src/types/index.ts`](src/types/index.ts).

```ts
interface FAGroup {
  accountCode: string;          // 2-char group code
  accountDescription: string;
  accountType: string;          // '0'..'4' (Asset/Liability/Equity/Income/Expense)
  accountCurrentBalance: number;
}

interface FASubGroup {
  sCode: string;                // 5-char ledger code
  sDesc: string;
  aCode: string;               // parent group code
  sType: string;
  sOpbal: number;              // opening balance
  sDrCr: "DR" | "CR";          // normal balance side
  sFlag: "T" | "F";            // active flag
}

interface JournalMaster {
  jId?: string;                // omit on create; server generates (e.g. JV2026000001)
  jDoc: string;
  jDate: string;               // ISO datetime
  jAmount: number;
  jNarr: string;
}

interface JournalDetail {
  jId: string;
  jCode: string;
  jDrCr: "DR" | "CR";
  jAmount: number;
}

interface JournalVoucherLineInput {
  jCode: string;
  jDrCr: "DR" | "CR";
  jAmount: number;
}

interface JournalVoucherRequest {
  jDoc?: string;
  jDate?: string;              // ISO datetime
  jNarr?: string;
  lines: JournalVoucherLineInput[];
}

interface OpeningBalanceLineInput {
  accountCode: string;
  drCr: "DR" | "CR";
  amount: number;
}

interface OpeningBalanceImportRequest {
  openingDate?: string;        // ISO date; server defaults to today
  narration?: string;
  lines: OpeningBalanceLineInput[];
}

interface PeriodLock {
  id: number;
  periodStart: string;         // ISO date
  periodEnd: string;           // ISO date
  lockedAt: string;            // ISO datetime
  lockedBy: string;
}

interface PeriodLockRequest {
  periodStart: string;         // ISO date
  periodEnd: string;           // ISO date
}

interface TrialBalanceRow { accountCode: string; description: string; debit: number; credit: number; }
interface TrialBalance {
  asOfDate: string;
  rows: TrialBalanceRow[];
  totalDebit: number;
  totalCredit: number;
  balanced: boolean;
}

interface ReportLineItem { accountCode: string; description: string; amount: number; }
interface ProfitAndLoss {
  asOfDate: string;
  revenue: ReportLineItem[];
  expenses: ReportLineItem[];
  totalRevenue: number;
  totalExpenses: number;
  netProfit: number;
}
interface BalanceSheet {
  asOfDate: string;
  assets: ReportLineItem[];
  liabilities: ReportLineItem[];
  equity: ReportLineItem[];
  totalAssets: number;
  totalLiabilities: number;
  totalEquity: number;
  balanced: boolean;
}
```

## Usage example

```ts
import { apiClient } from "./api/apiClient";

// Read a report
try {
  const tb = await apiClient.getTrialBalance("2026-06-09");
  console.log(tb.balanced, tb.rows.length);
} catch (err) {
  console.error((err as Error).message); // backend-provided message
}

// Post a balanced voucher
await apiClient.postJournalVoucher({
  jNarr: "Cash sale",
  lines: [
    { jCode: "10001", jDrCr: "DR", jAmount: 500 },
    { jCode: "40001", jDrCr: "CR", jAmount: 500 },
  ],
});
```
