# REFACTOR_REPORT.md — FA-PACKAGE DesktopApp

Phase 0 discovery for the Java desktop frontend refactor. Written before any code change.

## 1. Environment

| Property | Value |
|----------|-------|
| UI toolkit | **JavaFX 17.0.8** (`javafx-controls`, `javafx-fxml`) — no Swing/AWT |
| Language level | Java 17 (`maven.compiler.source/target = 17`) |
| Local JDK / Maven | JDK 21.0.10, Maven 3.9.10 |
| Build system | **Maven** (`pom.xml`; no wrapper) |
| Other deps | Jackson 2.17.0 (`databind`, `jsr310`) for JSON; shade plugin for a fat jar; exec plugin, main class `…desktop.Main` |
| Baseline build | `mvn clean compile` → **BUILD SUCCESS** (one benign `unchecked` warning in `JournalEntriesView`) |

Note: `grep` for `javax.swing`/`java.awt` matched only the `javafx` substring — the app is pure JavaFX.

## 2. Source inventory (line counts)

Total: **23 Java files, ~3,201 lines** + `styles.css` (324) + `pom.xml`.

### Views (`desktop.view`) — the refactor hotspots
| File | Lines | Flag |
|------|------:|------|
| `JournalEntriesView.java` | **492** | 🔴 oversized; `openNewVoucherDialog()` is ~180 lines |
| `FASubGroupsView.java` | **311** | 🔴 oversized (UI + validation + async + DTO assembly) |
| `OpeningBalancesView.java` | **256** | 🟠 near limit; duplicates voucher-line logic |
| `FAGroupsView.java` | **231** | 🟠 near limit |
| `PeriodLocksView.java` | 159 | ok |
| `BalanceSheetView.java` | 138 | ok (dup `buildSection`) |
| `DashboardView.java` | 126 | ok |
| `ProfitAndLossView.java` | 116 | ok (dup `buildSection`) |
| `TrialBalanceView.java` | 106 | ok |

### Other layers
| File | Lines | Role |
|------|------:|------|
| `api/ApiClient.java` | **350** | 🟠 HTTP gateway — one method per endpoint (mostly fine, but repetitive request boilerplate + `System.err`) |
| `ui/UiUtils.java` | 73 | shared helpers (money fmt, money column, stylesheet, alert) |
| `DesktopApp.java` | 184 | Application shell: sidebar/topbar nav, view switching, connection ping |
| `Main.java` | 10 | launcher |
| `model/*` (10 files) | 38–104 | plain POJO/DTOs with getters/setters + Jackson `@JsonProperty`; **no behavior** |

### Method-level flags (> ~50 lines)
- `JournalEntriesView.openNewVoucherDialog()` — ~180 lines (dialog build + async account load + recalc + validation + submit).
- `JournalEntriesView` constructor — ~113 lines.
- `FASubGroupsView` / `FAGroupsView` constructors — ~145 / ~130 lines (table + form + listeners inline).

## 3. Coupling / concern-mixing map

```
Main → DesktopApp (shell) ──creates──> ApiClient (singleton, passed to every view)
                          └─instantiates─> each *View on nav click

Every *View  ──┬── builds its own JavaFX UI            (View concern)
               ├── calls apiClient.* on background CF   (Data-access concern)
               ├── validates form input                (Controller/Service concern)
               ├── assembles raw Map<String,Object>     (DTO/request concern)
               └── calls UiUtils.showAlert              (cross-cutting)

ApiClient  → Jackson mapper → HttpClient → backend REST (…/api/v1)
UiUtils    → Alert / TableColumn / stylesheet helpers
```

**Every view class currently plays View + Controller + light Service + request-DTO builder.** There is no controller/presenter/service/viewmodel layer, and request bodies for POST endpoints (vouchers, opening balances, period locks) are hand-built `Map<String,Object>` inside the views.

## 4. Duplication / copy-paste candidates

1. **Async fetch-then-populate** (~25 occurrences): `CompletableFuture.runAsync(() -> { try { …apiClient…; Platform.runLater(populate) } catch { Platform.runLater(showAlert ERROR) } })`. In every view's `load*` method.
2. **Confirm-then-async-delete** dialog: `JournalEntriesView` (delete + reverse), `FASubGroupsView.deleteSubGroup`, `PeriodLocksView.deleteLock` — same `Alert CONFIRMATION → applyStylesheet → showAndWait → runAsync` shape.
3. **Voucher line grid + balance check**: `JournalEntriesView` (`VoucherLineRow`, `addLineRow`, recalc, DR/CR balance) and `OpeningBalancesView` (`LineRow`, `addLineRow`, `recalculate`) are ~90% identical.
4. **Report section builder**: `BalanceSheetView.buildSection` and `ProfitAndLossView.buildSection` are near-identical; `TrialBalanceView` repeats the same table+total scaffolding.
5. **Inline hex-color styling**: `#10b981` (green/ok), `#ef4444` (red/error), `#94a3b8` (muted), `-fx-font-weight: bold` repeated via `setStyle(...)` across ~8 views instead of CSS classes.
6. **Two copies of the default base URL** string inside `ApiClient.loadBaseUrl()`.
7. `System.err.println` used for logging in `ApiClient` (3×) and `UiUtils` (2×) — spec Phase 4 wants a real logger. No `printStackTrace()` exists.

## 5. Chosen pattern

**Humble-View + Service, with shared UI helpers** (a pragmatic MVP; the view stays its own controller, business/data orchestration is pushed out). Rationale under the project's simplicity/surgical constraints:

- The app is a thin REST client. `ApiClient` already *is* the repository/gateway. A full MVVM with observable ViewModels + a Presenter per screen would roughly double the class count for a pure, behavior-preserving refactor — high churn, high risk, low payoff. A senior reviewer would call that over-engineered here.
- The real pain is **duplication** and **oversized view constructors/methods**, not the absence of a formal MVP triad. So the highest-value, lowest-risk moves are:
  - Extract the repeated async patterns into an `AsyncTask`/`Ui` helper (kills ~25 copies).
  - Extract the voucher-line editor into one reusable `LedgerLineEditor` component (kills dup #3).
  - Extract a report-section factory (kills dup #4).
  - Move request-body assembly into small request/service objects so views stop hand-building `Map`s.
  - Centralize colors/labels into constants + CSS classes; swap `System.err` for `java.util.logging`.
  - Split the two 🔴 files by pulling the dialog/editor and handlers into their own classes.

### Target package layout (adapting the spec to what exists)
```
com.spam.financialaccounting.desktop
  api/         ApiClient (gateway — keep)
  model/       POJO DTOs (keep) + request DTOs (new, replaces inline Maps)
  service/     thin orchestration where a view needs >1 call / request assembly
  ui/          reusable widgets & helpers: UiUtils, AsyncUi, LedgerLineEditor,
               ReportSection, form factories
  view/        screens (humble views: build UI, delegate work)
  config/      UiConstants (colors, dims, labels), AppConfig (base URL)
```

## 6. Verification strategy (important constraint)

`mvn clean compile` is the gate after **every** change (green baseline established). The GUI cannot be fully exercised headlessly here and needs the Spring backend running, so behavior-preservation otherwise rests on careful, surgical, review-checked edits — no logic changes bundled with renames/extractions.

## 7. Proposed commit sequence

1. Phase 1: `.editorconfig`, formatting, unused imports/vars, `final`/`@Override`, magic values → `UiConstants`/`AppConfig`.
2. Phase 3a: `AsyncUi` helper + migrate all `load*`/delete flows (dedupe #1, #2).
3. Phase 3b: `LedgerLineEditor` reusable component (dedupe #3) → shrinks Journal + OpeningBalances.
4. Phase 3c: `ReportSection` factory (dedupe #4) → shrinks the 3 report views.
5. Phase 2: split `JournalEntriesView` (extract new-voucher dialog) and `FASubGroupsView`.
6. Phase 4: `java.util.logging` for `ApiClient`/`UiUtils`; try-with-resources already used in `ApiClient.loadBaseUrl`.
7. Phase 5: Javadoc on public classes + `ARCHITECTURE.md`.

Each step compiles before the next.
</content>
