export interface FAGroup {
  accountCode: string;
  accountDescription: string;
  accountType: string;
  accountCurrentBalance: number;
}

export interface FASubGroup {
  sCode: string;
  sDesc: string;
  aCode: string;
  sType: string;
  sOpbal: number;
  sDrCr: "DR" | "CR";
  sFlag: "T" | "F";
}

export interface JournalMaster {
  jId?: string; // leave out on create, backend generates it
  jDoc: string;
  jDate: string; // ISO datetime
  jAmount: number;
  jNarr: string;
}

export interface JournalDetail {
  jId: string;
  jCode: string;
  jDrCr: "DR" | "CR";
  jAmount: number;
}

export interface JournalVoucherLineInput {
  jCode: string;
  jDrCr: "DR" | "CR";
  jAmount: number;
}

export interface JournalVoucherRequest {
  jDoc?: string;
  jDate?: string; // ISO datetime
  jNarr?: string;
  lines: JournalVoucherLineInput[];
}

export interface TrialBalanceRow {
  accountCode: string;
  description: string;
  debit: number;
  credit: number;
}

export interface TrialBalance {
  asOfDate: string;
  rows: TrialBalanceRow[];
  totalDebit: number;
  totalCredit: number;
  balanced: boolean;
}

export interface OpeningBalanceLineInput {
  accountCode: string;
  drCr: "DR" | "CR";
  amount: number;
}

export interface OpeningBalanceImportRequest {
  openingDate?: string; // ISO date yyyy-MM-dd; server defaults to today
  narration?: string;
  lines: OpeningBalanceLineInput[];
}

export interface PeriodLock {
  id: number;
  periodStart: string; // ISO date yyyy-MM-dd
  periodEnd: string; // ISO date yyyy-MM-dd
  lockedAt: string; // ISO datetime
  lockedBy: string;
}

export interface PeriodLockRequest {
  periodStart: string; // ISO date yyyy-MM-dd
  periodEnd: string; // ISO date yyyy-MM-dd
}

export interface ReportLineItem {
  accountCode: string;
  description: string;
  amount: number;
}

export interface ProfitAndLoss {
  asOfDate: string;
  revenue: ReportLineItem[];
  expenses: ReportLineItem[];
  totalRevenue: number;
  totalExpenses: number;
  netProfit: number;
}

export interface BalanceSheet {
  asOfDate: string;
  assets: ReportLineItem[];
  liabilities: ReportLineItem[];
  equity: ReportLineItem[];
  totalAssets: number;
  totalLiabilities: number;
  totalEquity: number;
  balanced: boolean;
}