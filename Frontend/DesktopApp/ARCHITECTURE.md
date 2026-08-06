# ARCHITECTURE.md — FA-PACKAGE DesktopApp

JavaFX 17 desktop client for the FA-PACKAGE accounting backend. This document
describes the package layout, the chosen pattern, and where to add new features.
For the pre-refactor discovery findings see `REFACTOR_REPORT.md`.

## Pattern

**Humble View + shared UI helpers** (a pragmatic MVP). Each screen is a JavaFX
node (`view.*View`) that builds its own controls and delegates the work that isn't
pure layout:

- **Data access** goes through the single `api.ApiClient` gateway (one method per
  REST endpoint). It is created once in `DesktopApp` and passed to every view.
- **Threading** goes through `ui.AsyncUi`: it runs a blocking `ApiClient` call off
  the FX Application Thread and delivers the result (or error) back on it. Views no
  longer hand-roll `CompletableFuture` + `Platform.runLater` + try/catch.
- **Reusable UI** lives in `ui.*`: `LedgerLineEditor` (the double-entry line grid),
  `ReportSection` (a report table + total card), and `UiUtils` (money formatting,
  money columns, stylesheet application, alerts).
- **Cross-cutting constants** live in `config.*`: `AppConfig` (base URL / config
  file) and `UiConstants` (status colors and reusable style fragments).

A formal Presenter/ViewModel layer was deliberately *not* introduced: the app is a
thin REST client where `ApiClient` already plays the repository role, so the payoff
would not justify roughly doubling the class count for a behavior-preserving
refactor. See `REFACTOR_REPORT.md` §5.

## Package layout

```
com.spam.financialaccounting.desktop
  Main.java              launches the JavaFX Application
  DesktopApp.java        application shell: sidebar/top-bar, view switching, ping
  api/
    ApiClient            REST gateway (blocking; one method per endpoint)
  config/
    AppConfig            base URL resolution (config.properties)
    UiConstants          status colors + style fragments
  model/                 plain POJO DTOs (Jackson-mapped, no behavior)
  ui/                    reusable, screen-agnostic UI + helpers
    AsyncUi              run API calls off the FX thread, callbacks back on it
    UiUtils              money formatting/columns, stylesheet, alerts
    LedgerLineEditor     editable DR/CR ledger-line grid + totals
    ReportSection        report table-in-a-card factory
  view/                  one class per screen (humble views)
    DashboardView, FAGroupsView, FASubGroupsView, OpeningBalancesView,
    JournalEntriesView, NewVoucherDialog, TrialBalanceView, ProfitAndLossView,
    BalanceSheetView, PeriodLocksView
```

## Data flow (typical screen)

```
User → View (build UI, gather input, validate)
     → AsyncUi.fetch/run( ApiClient.someEndpoint() )   [background thread]
     → ApiClient → HttpClient → backend REST /api/v1/...
     ← result mapped by Jackson
     ← onSuccess / onError callback   [FX thread] → View updates controls
```

## Where to add a new feature

- **New screen**: add a `SomethingView` in `view/` extending an appropriate JavaFX
  container; take `ApiClient` in the constructor; load data with `AsyncUi.fetch`.
  Register it in `DesktopApp` (a nav button + a `showSomething()` method).
- **New endpoint**: add one method to `ApiClient` mirroring the existing ones
  (build request, `handleErrorResponse`, map the body).
- **New request body**: prefer a small typed object or the existing inline
  `Map<String,Object>` pattern used by the voucher/opening-balance/period-lock
  posts; keep assembly in the view or a dedicated dialog class.
- **Repeated UI**: if a control pattern appears in two or more screens, extract it
  into `ui/` (as was done for `LedgerLineEditor` and `ReportSection`).
- **Colors / status styling**: use `UiConstants`; add semantic constants there
  rather than inlining hex values.

## Conventions

- 4-space indentation, ≤120 col (see `.editorconfig`). Standard Java naming.
- Views stay "humble": no `CompletableFuture`/`Platform.runLater` boilerplate and no
  hand-built HTTP — go through `AsyncUi` and `ApiClient`.
- Log via `java.util.logging`; no `System.out`/`System.err`/`printStackTrace`.
- Keep screen classes focused; extract modal dialogs (e.g. `NewVoucherDialog`) and
  shared widgets rather than growing a view past ~300 lines.

## File-size status

All screens are under ~300 lines after the refactor. `api/ApiClient` (~334) is a
flat gateway with one short method per endpoint (no mixed concerns) and is left as a
single cohesive file. `view/FASubGroupsView` (~292) is a cohesive master-detail CRUD
screen kept intact rather than split artificially.
