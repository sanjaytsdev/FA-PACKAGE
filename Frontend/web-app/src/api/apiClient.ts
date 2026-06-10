import type {
  BalanceSheet,
  FAGroup,
  FASubGroup,
  JournalDetail,
  JournalMaster,
  JournalVoucherRequest,
  OpeningBalanceImportRequest,
  PeriodLock,
  PeriodLockRequest,
  ProfitAndLoss,
  TrialBalance,
} from "../types";

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api/v1";

/** Timeout for API calls (8 seconds) */
const DEFAULT_TIMEOUT_MS = 8000;

function makeSignal(): AbortSignal {
  return AbortSignal.timeout(DEFAULT_TIMEOUT_MS);
}

async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    let errMsg = `HTTP Error ${response.status}`;
    try {
      const data = await response.json();
      errMsg = data.message || data.error || errMsg;
    } catch {
      // body might not be JSON, ignore
    }
    throw new Error(errMsg);
  }

  if (response.status === 204) return null as T;

  return response.json();
}

export const apiClient = {
  //  Connection Health 
  async ping(): Promise<boolean> {
    try {
      const res = await fetch(`${BASE_URL}/fagroups`, {
        method: "GET",
        signal: AbortSignal.timeout(3000),
      });
      return res.ok;
    } catch {
      return false;
    }
  },

  // Account Groups
  async getGroups(): Promise<FAGroup[]> {
    return handleResponse<FAGroup[]>(
      await fetch(`${BASE_URL}/fagroups`, { signal: makeSignal() }),
    );
  },

  async createGroup(group: FAGroup): Promise<FAGroup> {
    return handleResponse<FAGroup>(
      await fetch(`${BASE_URL}/fagroups`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(group),
        signal: makeSignal(),
      }),
    );
  },

  async updateGroup(code: string, group: FAGroup): Promise<FAGroup> {
    return handleResponse<FAGroup>(
      await fetch(`${BASE_URL}/fagroups/${code}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(group),
        signal: makeSignal(),
      }),
    );
  },

  async deleteGroup(code: string): Promise<void> {
    return handleResponse<void>(
      await fetch(`${BASE_URL}/fagroups/${code}`, {
        method: "DELETE",
        signal: makeSignal(),
      }),
    );
  },

  //  Ledger Accounts (Sub-Groups) 
  async getLedgers(): Promise<FASubGroup[]> {
    return handleResponse<FASubGroup[]>(
      await fetch(`${BASE_URL}/ledger-accounts`, { signal: makeSignal() }),
    );
  },

  async createLedger(ledger: FASubGroup): Promise<FASubGroup> {
    return handleResponse<FASubGroup>(
      await fetch(`${BASE_URL}/ledger-accounts`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(ledger),
        signal: makeSignal(),
      }),
    );
  },

  async updateLedger(code: string, ledger: FASubGroup): Promise<FASubGroup> {
    return handleResponse<FASubGroup>(
      await fetch(`${BASE_URL}/ledger-accounts/${code}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(ledger),
        signal: makeSignal(),
      }),
    );
  },

  async deleteLedger(code: string): Promise<void> {
    return handleResponse<void>(
      await fetch(`${BASE_URL}/ledger-accounts/${code}`, {
        method: "DELETE",
        signal: makeSignal(),
      }),
    );
  },

  //  Journal Masters (read-only; writes go through Journal Vouchers)
  async getJournalMasters(): Promise<JournalMaster[]> {
    return handleResponse<JournalMaster[]>(
      await fetch(`${BASE_URL}/journal-masters`, { signal: makeSignal() }),
    );
  },

  // Journal Details (read-only; writes go through Journal Vouchers)
  async getJournalDetails(jId: string): Promise<JournalDetail[]> {
    return handleResponse<JournalDetail[]>(
      await fetch(`${BASE_URL}/journal-details/journal/${jId}`, {
        signal: makeSignal(),
      }),
    );
  },

  //  Journal Vouchers (balanced, atomic post/reverse/delete)
  async postJournalVoucher(
    voucher: JournalVoucherRequest,
  ): Promise<JournalMaster> {
    return handleResponse<JournalMaster>(
      await fetch(`${BASE_URL}/journal-vouchers`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(voucher),
        signal: makeSignal(),
      }),
    );
  },

  async deleteJournalVoucher(jId: string): Promise<void> {
    return handleResponse<void>(
      await fetch(`${BASE_URL}/journal-vouchers/${jId}`, {
        method: "DELETE",
        signal: makeSignal(),
      }),
    );
  },

  // Posts a reversing voucher that cancels the given one and returns the new reversal header.
  async reverseJournalVoucher(jId: string): Promise<JournalMaster> {
    return handleResponse<JournalMaster>(
      await fetch(`${BASE_URL}/journal-vouchers/${jId}/reverse`, {
        method: "POST",
        signal: makeSignal(),
      }),
    );
  },

  //  Opening Balances (imported as one atomic Opening Balance voucher)
  async importOpeningBalances(
    request: OpeningBalanceImportRequest,
  ): Promise<JournalMaster> {
    return handleResponse<JournalMaster>(
      await fetch(`${BASE_URL}/opening-balances/import`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(request),
        signal: makeSignal(),
      }),
    );
  },

  //  Period Locks (closed accounting periods)
  async getPeriodLocks(): Promise<PeriodLock[]> {
    return handleResponse<PeriodLock[]>(
      await fetch(`${BASE_URL}/period-locks`, { signal: makeSignal() }),
    );
  },

  async createPeriodLock(request: PeriodLockRequest): Promise<PeriodLock> {
    return handleResponse<PeriodLock>(
      await fetch(`${BASE_URL}/period-locks`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(request),
        signal: makeSignal(),
      }),
    );
  },

  async deletePeriodLock(id: number): Promise<void> {
    return handleResponse<void>(
      await fetch(`${BASE_URL}/period-locks/${id}`, {
        method: "DELETE",
        signal: makeSignal(),
      }),
    );
  },

  //  Reports
  // asOfDate (ISO yyyy-MM-dd) limits the report to vouchers posted on or before
  // that date. Leave it off to report as of today.
  async getTrialBalance(asOfDate?: string): Promise<TrialBalance> {
    const query = asOfDate ? `?asOfDate=${encodeURIComponent(asOfDate)}` : '';
    return handleResponse<TrialBalance>(
      await fetch(`${BASE_URL}/reports/trial-balance${query}`, { signal: makeSignal() }),
    );
  },

  async getProfitAndLoss(asOfDate?: string): Promise<ProfitAndLoss> {
    const query = asOfDate ? `?asOfDate=${encodeURIComponent(asOfDate)}` : '';
    return handleResponse<ProfitAndLoss>(
      await fetch(`${BASE_URL}/reports/profit-and-loss${query}`, { signal: makeSignal() }),
    );
  },

  async getBalanceSheet(asOfDate?: string): Promise<BalanceSheet> {
    const query = asOfDate ? `?asOfDate=${encodeURIComponent(asOfDate)}` : '';
    return handleResponse<BalanceSheet>(
      await fetch(`${BASE_URL}/reports/balance-sheet${query}`, { signal: makeSignal() }),
    );
  },
};