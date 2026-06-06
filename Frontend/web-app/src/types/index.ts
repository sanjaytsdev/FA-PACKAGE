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
  jId?: string; // omit on create — backend auto-generates
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