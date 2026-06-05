import type { FAGroup, FASubGroup, JournalDetail, JournalMaster } from "../types";

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api/v1";

/** Default timeout for all API calls (8 seconds) */
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
      // Ignored — response body may not be JSON
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

  //  Journal Masters 
  async getJournalMasters(): Promise<JournalMaster[]> {
    return handleResponse<JournalMaster[]>(
      await fetch(`${BASE_URL}/journal-masters`, { signal: makeSignal() }),
    );
  },

  async createJournalMaster(master: JournalMaster): Promise<JournalMaster> {
    return handleResponse<JournalMaster>(
      await fetch(`${BASE_URL}/journal-masters`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(master),
        signal: makeSignal(),
      }),
    );
  },

  async deleteJournalMaster(jId: string): Promise<void> {
    return handleResponse<void>(
      await fetch(`${BASE_URL}/journal-masters/${jId}`, {
        method: "DELETE",
        signal: makeSignal(),
      }),
    );
  },

  // Journal Details 
  async getJournalDetails(jId: string): Promise<JournalDetail[]> {
    return handleResponse<JournalDetail[]>(
      await fetch(`${BASE_URL}/journal-details/journal/${jId}`, {
        signal: makeSignal(),
      }),
    );
  },

  async createJournalDetail(detail: JournalDetail): Promise<JournalDetail> {
    return handleResponse<JournalDetail>(
      await fetch(`${BASE_URL}/journal-details`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(detail),
        signal: makeSignal(),
      }),
    );
  },

  async deleteJournalDetail(
    jId: string,
    jCode: string,
    jDrCr: string,
  ): Promise<void> {
    return handleResponse<void>(
      await fetch(`${BASE_URL}/journal-details/${jId}/${jCode}/${jDrCr}`, {
        method: "DELETE",
        signal: makeSignal(),
      }),
    );
  },
};