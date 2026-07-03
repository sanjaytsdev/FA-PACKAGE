import React, { useEffect, useState } from 'react';
import { Wallet, Plus, Trash2, CheckCircle2, AlertCircle, Loader2 } from 'lucide-react';
import { apiClient } from '../api/apiClient';
import { DatePicker } from './DatePicker';
import type { FASubGroup } from '../types';

interface OpeningLine {
  id: number;
  accountCode: string;
  drCr: 'DR' | 'CR';
  amount: string;
}

export const OpeningBalancesView: React.FC = () => {
  const [ledgers, setLedgers] = useState<FASubGroup[]>([]);
  const [openingDate, setOpeningDate] = useState(new Date().toISOString().split('T')[0]);
  const [narration, setNarration] = useState('');
  const [lines, setLines] = useState<OpeningLine[]>([
    { id: 1, accountCode: '', drCr: 'DR', amount: '' },
    { id: 2, accountCode: '', drCr: 'CR', amount: '' },
  ]);

  const [loading, setLoading] = useState(false);
  const [alert, setAlert] = useState<{ type: 'success' | 'error'; title: string; message: string } | null>(null);
  // Per-line validation messages, shown under each line.
  const [lineErrors, setLineErrors] = useState<Record<number, { accountCode?: string; amount?: string }>>({});

  useEffect(() => {
    apiClient.getLedgers().then(setLedgers).catch(() => {});
  }, []);

  const triggerAlert = (type: 'success' | 'error', title: string, message: string) => {
    setAlert({ type, title, message });
    setTimeout(() => setAlert(null), 6000);
  };

  const addLine = () => {
    setLines((prev) => [...prev, { id: Date.now(), accountCode: '', drCr: 'DR', amount: '' }]);
  };

  const removeLine = (id: number) => {
    if (lines.length <= 2) return;
    setLines((prev) => prev.filter((l) => l.id !== id));
  };

  const updateLine = (id: number, field: keyof OpeningLine, value: any) => {
    setLines((prev) => prev.map((l) => (l.id === id ? { ...l, [field]: value } : l)));
  };

  let totalDebits = 0;
  let totalCredits = 0;
  lines.forEach((l) => {
    const val = Number(l.amount) || 0;
    if (l.drCr === 'DR') totalDebits += val;
    else totalCredits += val;
  });

  const diff = Math.abs(totalDebits - totalCredits);
  const isBalanced = totalDebits > 0 && diff < 0.001;

  const resetForm = () => {
    setOpeningDate(new Date().toISOString().split('T')[0]);
    setNarration('');
    setLines([
      { id: 1, accountCode: '', drCr: 'DR', amount: '' },
      { id: 2, accountCode: '', drCr: 'CR', amount: '' },
    ]);
    setLineErrors({});
  };

  const handleImport = async () => {
    if (!isBalanced) {
      triggerAlert('error', 'Unbalanced', 'Total opening debits must equal total opening credits.');
      return;
    }

    // Check each line and pin messages to the bad fields.
    const errs: Record<number, { accountCode?: string; amount?: string }> = {};
    lines.forEach((line) => {
      const e: { accountCode?: string; amount?: string } = {};
      if (!line.accountCode) e.accountCode = 'Select a ledger account.';
      if ((Number(line.amount) || 0) <= 0) e.amount = 'Enter a positive amount.';
      if (e.accountCode || e.amount) errs[line.id] = e;
    });
    if (Object.keys(errs).length > 0) {
      setLineErrors(errs);
      return;
    }
    setLineErrors({});

    try {
      setLoading(true);
      const voucher = await apiClient.importOpeningBalances({
        openingDate,
        narration: narration.trim() || undefined,
        lines: lines.map((l) => ({
          accountCode: l.accountCode,
          drCr: l.drCr,
          amount: Number(l.amount),
        })),
      });
      triggerAlert('success', 'Opening Balances Imported', `Recorded as Opening Balance voucher "${voucher.jId}".`);
      resetForm();
    } catch (err: any) {
      triggerAlert('error', 'Import Failed', err.message || 'Failed to import opening balances.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex-1 p-8 overflow-y-auto flex flex-col space-y-6">
      <div>
        <h2 className="text-3xl font-extrabold tracking-tight text-slate-100 flex items-center gap-3">
          <Wallet className="w-8 h-8 text-indigo-500" />
          Opening Balances
        </h2>
        <p className="text-sm text-slate-400 mt-2 font-medium">
          Initialize the books. The import is accepted only when total opening debits equal total opening credits, and is recorded as a single Opening Balance voucher.
        </p>
      </div>

      {alert && (
        <div className={`p-4 border rounded-xl flex items-start gap-3 shadow-lg ${
          alert.type === 'success' ? 'bg-emerald-500/10 border-emerald-500/20 text-emerald-400' :
          'bg-rose-500/10 border-rose-500/20 text-rose-400'
        }`}>
          <AlertCircle className="w-5 h-5 shrink-0 mt-0.5" />
          <div>
            <h4 className="font-bold text-sm">{alert.title}</h4>
            <p className="text-xs mt-1 opacity-90">{alert.message}</p>
          </div>
        </div>
      )}

      <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-6 shadow-xl space-y-6 max-w-4xl">
        {/* Header fields */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 bg-slate-950/30 p-4 border border-slate-800/40 rounded-xl">
          <div className="space-y-1.5">
            <label className="text-xs font-bold text-slate-400 uppercase tracking-wider">Opening Date</label>
            <DatePicker
              value={openingDate}
              onChange={setOpeningDate}
              className="w-full bg-slate-950 border border-slate-800 focus:border-indigo-500 rounded-lg px-4 py-2.5 text-sm font-semibold text-slate-200 transition-colors"
            />
          </div>
          <div className="space-y-1.5">
            <label className="text-xs font-bold text-slate-400 uppercase tracking-wider">Narration</label>
            <input
              type="text"
              placeholder="e.g. Opening balances FY2026"
              value={narration}
              onChange={(e) => setNarration(e.target.value)}
              className="w-full bg-slate-950 border border-slate-800 focus:border-indigo-500 rounded-lg px-4 py-2.5 text-sm font-semibold text-slate-200 transition-colors placeholder-slate-700"
            />
          </div>
        </div>

        {/* Lines */}
        <div className="space-y-3">
          <div className="flex justify-between items-center">
            <h4 className="text-sm font-extrabold text-slate-200">Opening Balance Lines</h4>
            <button
              onClick={addLine}
              className="flex items-center gap-1 px-3 py-1.5 bg-slate-800 border border-slate-700 hover:bg-slate-700 text-xs font-bold rounded-lg text-slate-200 transition-colors cursor-pointer"
            >
              <Plus className="w-3.5 h-3.5" />
              Add Line
            </button>
          </div>

          <div className="space-y-3 max-h-[40vh] overflow-y-auto pr-1">
            {lines.map((line, index) => (
              <div key={line.id} className="space-y-1">
                <div className="flex gap-4 items-center">
                  <div className="text-xs text-slate-500 font-bold w-6 text-center shrink-0">#{index + 1}</div>
                  <select
                    value={line.accountCode}
                    onChange={(e) => {
                      const code = e.target.value;
                      updateLine(line.id, 'accountCode', code);
                      const ledger = ledgers.find((l) => l.sCode === code);
                      if (ledger) updateLine(line.id, 'drCr', ledger.sDrCr);
                    }}
                    className={`flex-1 bg-slate-950 border ${lineErrors[line.id]?.accountCode ? 'border-rose-500' : 'border-slate-800'} focus:border-indigo-500 rounded-lg px-4 py-2 text-sm font-semibold text-slate-200 transition-colors`}
                  >
                    <option value="">Select Ledger Account</option>
                    {ledgers.map((l) => (
                      <option key={l.sCode} value={l.sCode}>
                        {l.sCode} - {l.sDesc}
                      </option>
                    ))}
                  </select>
                  <select
                    value={line.drCr}
                    onChange={(e) => updateLine(line.id, 'drCr', e.target.value as 'DR' | 'CR')}
                    className="w-28 bg-slate-950 border border-slate-800 focus:border-indigo-500 rounded-lg px-4 py-2 text-sm font-bold text-slate-200 transition-colors"
                  >
                    <option value="DR">DR</option>
                    <option value="CR">CR</option>
                  </select>
                  <input
                    type="text"
                    placeholder="Amount"
                    value={line.amount}
                    onChange={(e) => updateLine(line.id, 'amount', e.target.value)}
                    className={`w-36 bg-slate-950 border ${lineErrors[line.id]?.amount ? 'border-rose-500' : 'border-slate-800'} focus:border-indigo-500 rounded-lg px-4 py-2 text-sm font-mono font-semibold text-slate-200 transition-colors`}
                  />
                  <button
                    onClick={() => removeLine(line.id)}
                    disabled={lines.length <= 2}
                    className="text-slate-500 hover:text-rose-400 disabled:opacity-30 transition-colors p-1"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>

                {(lineErrors[line.id]?.accountCode || lineErrors[line.id]?.amount) && (
                  <div className="flex gap-4 pl-10 text-2xs font-semibold text-rose-400">
                    <span className="flex-1">{lineErrors[line.id]?.accountCode}</span>
                    <span className="w-28" />
                    <span className="w-36">{lineErrors[line.id]?.amount}</span>
                    <span className="w-6" />
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>

        {/* Footer totals */}
        <div className="bg-slate-950 -mx-6 -mb-6 px-6 py-4 border-t border-slate-800 flex flex-col md:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-6 flex-wrap">
            <div className="flex flex-col">
              <span className="text-2xs font-bold text-slate-500 uppercase tracking-widest">Total Debit</span>
              <span className="text-sm font-black text-indigo-400 font-mono">{totalDebits.toFixed(2)}</span>
            </div>
            <div className="flex flex-col">
              <span className="text-2xs font-bold text-slate-500 uppercase tracking-widest">Total Credit</span>
              <span className="text-sm font-black text-purple-400 font-mono">{totalCredits.toFixed(2)}</span>
            </div>
            <div className="flex flex-col">
              <span className="text-2xs font-bold text-slate-500 uppercase tracking-widest">Difference</span>
              <span className={`text-sm font-black font-mono ${isBalanced ? 'text-emerald-400' : 'text-rose-400'}`}>
                {diff.toFixed(2)}
              </span>
            </div>
          </div>
          <button
            onClick={handleImport}
            disabled={!isBalanced || loading}
            className="flex items-center gap-1.5 px-5 py-2.5 bg-emerald-600 hover:bg-emerald-500 disabled:bg-slate-800 disabled:opacity-40 disabled:text-slate-500 text-white font-extrabold rounded-lg text-sm transition-all cursor-pointer shadow-lg"
          >
            {loading ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" />
                Importing...
              </>
            ) : (
              <>
                <CheckCircle2 className="w-4 h-4" />
                Import Opening Balances
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
};
