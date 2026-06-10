import React, { useEffect, useState } from 'react';
import { X, Plus, Trash2, CheckCircle2, AlertTriangle, Loader2 } from 'lucide-react';
import { apiClient } from '../api/apiClient';
import { DatePicker } from './DatePicker';
import type { FASubGroup } from '../types';

interface NewVoucherModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

interface VoucherLine {
  id: number;
  sCode: string;
  jDrCr: 'DR' | 'CR';
  jAmount: string;
}

export const NewVoucherModal: React.FC<NewVoucherModalProps> = ({ isOpen, onClose, onSuccess }) => {
  const [ledgers, setLedgers] = useState<FASubGroup[]>([]);
  const [voucherDate, setVoucherDate] = useState(new Date().toISOString().split('T')[0]);
  const [narration, setNarration] = useState('');

  const [lines, setLines] = useState<VoucherLine[]>([
    { id: 1, sCode: '', jDrCr: 'DR', jAmount: '' },
    { id: 2, sCode: '', jDrCr: 'CR', jAmount: '' },
  ]);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  // Per-line validation messages, shown under each entry line.
  const [lineErrors, setLineErrors] = useState<Record<number, { sCode?: string; amount?: string }>>({});

  useEffect(() => {
    if (isOpen) {
      apiClient.getLedgers().then(setLedgers).catch(() => {});
      // Reset form
      setVoucherDate(new Date().toISOString().split('T')[0]);
      setNarration('');
      setLines([
        { id: 1, sCode: '', jDrCr: 'DR', jAmount: '' },
        { id: 2, sCode: '', jDrCr: 'CR', jAmount: '' },
      ]);
      setError(null);
      setLineErrors({});
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const addLine = () => {
    setLines((prev) => [...prev, { id: Date.now(), sCode: '', jDrCr: 'DR', jAmount: '' }]);
  };

  const removeLine = (id: number) => {
    if (lines.length <= 2) return;
    setLines((prev) => prev.filter((l) => l.id !== id));
  };

  const updateLine = (id: number, field: keyof VoucherLine, value: any) => {
    setLines((prev) =>
      prev.map((l) => (l.id === id ? { ...l, [field]: value } : l))
    );
  };

  // Recalculate totals
  let totalDebits = 0;
  let totalCredits = 0;

  lines.forEach((l) => {
    const val = Number(l.jAmount) || 0;
    if (l.jDrCr === 'DR') {
      totalDebits += val;
    } else {
      totalCredits += val;
    }
  });

  const diff = Math.abs(totalDebits - totalCredits);
  const isBalanced = totalDebits > 0 && Math.abs(diff) < 0.001;

  const handlePost = async () => {
    // Must be balanced before submitting (belt and braces; server checks too)
    if (!isBalanced) {
      setError('Voucher is not balanced. Total debits must equal total credits.');
      return;
    }

    // Check each line and pin messages to the bad fields.
    const errs: Record<number, { sCode?: string; amount?: string }> = {};
    lines.forEach((line) => {
      const e: { sCode?: string; amount?: string } = {};
      if (!line.sCode) e.sCode = 'Select a ledger account.';
      if ((Number(line.jAmount) || 0) <= 0) e.amount = 'Enter a positive amount.';
      if (e.sCode || e.amount) errs[line.id] = e;
    });
    if (Object.keys(errs).length > 0) {
      setLineErrors(errs);
      return;
    }
    setLineErrors({});

    try {
      setLoading(true);
      setError(null);

      // Post the whole voucher in one server-validated call.
      // The backend re-checks the debit/credit balance and works out the total.
      await apiClient.postJournalVoucher({
        jDoc: 'JV',
        jDate: new Date(voucherDate).toISOString(),
        jNarr: narration.trim(),
        lines: lines.map((line) => ({
          jCode: line.sCode,
          jDrCr: line.jDrCr,
          jAmount: Number(line.jAmount),
        })),
      });

      onSuccess();
      onClose();
    } catch (err: any) {
      setError(err.message || 'Submission failed.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm select-none">
      <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-4xl max-h-[90vh] flex flex-col shadow-2xl overflow-hidden transition-all duration-200">
        
        {/* Header */}
        <div className="px-6 py-4 border-b border-slate-800 flex justify-between items-center">
          <h3 className="text-lg font-extrabold text-slate-100 flex items-center gap-2">
            📝 Create Balanced Journal Voucher
          </h3>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-200 transition-colors">
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="p-6 overflow-y-auto flex-1 space-y-6">
          {error && (
            <div className="p-4 bg-rose-500/10 border border-rose-500/20 text-rose-400 rounded-xl flex items-start gap-2.5">
              <AlertTriangle className="w-5 h-5 shrink-0 mt-0.5" />
              <div className="text-xs font-semibold">{error}</div>
            </div>
          )}

          {/* Master Fields */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6 bg-slate-950/30 p-4 border border-slate-800/40 rounded-xl">
            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-400 uppercase tracking-wider">Voucher Date</label>
              <DatePicker
                value={voucherDate}
                onChange={setVoucherDate}
                className="w-full bg-slate-950 border border-slate-800 focus:border-indigo-500 rounded-lg px-4 py-2.5 text-sm font-semibold text-slate-200 transition-colors"
              />
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-400 uppercase tracking-wider">Narration</label>
              <input
                type="text"
                placeholder="e.g. Funds transfer"
                value={narration}
                onChange={(e) => setNarration(e.target.value)}
                className="w-full bg-slate-950 border border-slate-800 focus:border-indigo-500 rounded-lg px-4 py-2.5 text-sm font-semibold text-slate-200 transition-colors placeholder-slate-700"
              />
            </div>
          </div>

          {/* Lines Header */}
          <div className="space-y-3">
            <div className="flex justify-between items-center">
              <h4 className="text-sm font-extrabold text-slate-200">Double-Entry Transaction Lines</h4>
              <button
                onClick={addLine}
                className="flex items-center gap-1 px-3 py-1.5 bg-slate-800 border border-slate-700 hover:bg-slate-700 text-xs font-bold rounded-lg text-slate-200 transition-colors cursor-pointer"
              >
                <Plus className="w-3.5 h-3.5" />
                Add Entry Line
              </button>
            </div>

            {/* Lines List */}
            <div className="space-y-3 max-h-[35vh] overflow-y-auto pr-1">
              {lines.map((line, index) => (
                <div key={line.id} className="space-y-1">
                  <div className="flex gap-4 items-center">
                    <div className="text-xs text-slate-500 font-bold w-6 text-center shrink-0">#{index + 1}</div>

                    {/* Account select */}
                    <select
                      value={line.sCode}
                      onChange={(e) => {
                        const selectedCode = e.target.value;
                        updateLine(line.id, 'sCode', selectedCode);
                        // Default DR/CR to the account's normal side
                        const ledger = ledgers.find((l) => l.sCode === selectedCode);
                        if (ledger) updateLine(line.id, 'jDrCr', ledger.sDrCr);
                      }}
                      className={`flex-1 bg-slate-950 border ${lineErrors[line.id]?.sCode ? 'border-rose-500' : 'border-slate-800'} focus:border-indigo-500 rounded-lg px-4 py-2 text-sm font-semibold text-slate-200 transition-colors`}
                    >
                      <option value="">Select Ledger Account</option>
                      {ledgers.map((l) => (
                        <option key={l.sCode} value={l.sCode}>
                          {l.sCode} - {l.sDesc}
                        </option>
                      ))}
                    </select>

                    {/* DR/CR select */}
                    <select
                      value={line.jDrCr}
                      onChange={(e) => updateLine(line.id, 'jDrCr', e.target.value as 'DR' | 'CR')}
                      className="w-28 bg-slate-950 border border-slate-800 focus:border-indigo-500 rounded-lg px-4 py-2 text-sm font-bold text-slate-200 transition-colors"
                    >
                      <option value="DR">DR</option>
                      <option value="CR">CR</option>
                    </select>

                    {/* Amount input */}
                    <input
                      type="text"
                      placeholder="Amount"
                      value={line.jAmount}
                      onChange={(e) => updateLine(line.id, 'jAmount', e.target.value)}
                      className={`w-36 bg-slate-950 border ${lineErrors[line.id]?.amount ? 'border-rose-500' : 'border-slate-800'} focus:border-indigo-500 rounded-lg px-4 py-2 text-sm font-mono font-semibold text-slate-200 transition-colors placeholder-slate-850`}
                    />

                    {/* Remove button */}
                    <button
                      onClick={() => removeLine(line.id)}
                      disabled={lines.length <= 2}
                      className="text-slate-500 hover:text-rose-400 disabled:opacity-30 transition-colors p-1"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>

                  {(lineErrors[line.id]?.sCode || lineErrors[line.id]?.amount) && (
                    <div className="flex gap-4 pl-10 text-2xs font-semibold text-rose-400">
                      <span className="flex-1">{lineErrors[line.id]?.sCode}</span>
                      <span className="w-28" />
                      <span className="w-36">{lineErrors[line.id]?.amount}</span>
                      <span className="w-6" />
                    </div>
                  )}
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Footer Calculation Summaries */}
        <div className="bg-slate-950 px-6 py-4 border-t border-slate-800 flex flex-col md:flex-row items-center justify-between gap-4">
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
              <span className="text-2xs font-bold text-slate-500 uppercase tracking-widest">Aggregate Difference</span>
              <span className={`text-sm font-black font-mono ${isBalanced ? 'text-emerald-400' : 'text-rose-400'}`}>
                {diff.toFixed(2)}
              </span>
            </div>
          </div>

          <div className="flex items-center gap-4 w-full md:w-auto justify-end">
            <div className="flex items-center gap-2">
              <div className={`w-2.5 h-2.5 rounded-full ${isBalanced ? 'bg-emerald-500 animate-pulse' : 'bg-rose-500'}`} />
              <span className={`text-xs font-extrabold uppercase tracking-wide ${isBalanced ? 'text-emerald-400' : 'text-rose-400'}`}>
                {isBalanced ? 'Voucher Balanced!' : 'Unbalanced'}
              </span>
            </div>

            <button
              onClick={handlePost}
              disabled={!isBalanced || loading}
              className="flex items-center gap-1.5 px-5 py-2.5 bg-emerald-600 hover:bg-emerald-500 disabled:bg-slate-800 disabled:opacity-40 disabled:text-slate-500 text-white font-extrabold rounded-lg text-sm transition-all cursor-pointer shadow-lg"
            >
              {loading ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  Posting...
                </>
              ) : (
                <>
                  <CheckCircle2 className="w-4 h-4" />
                  Validation & Post Voucher
                </>
              )}
            </button>
          </div>
        </div>

      </div>
    </div>
  );
};
