import React, { useEffect, useState } from 'react';
import { X, Plus, Trash2, CheckCircle2, AlertTriangle, Loader2 } from 'lucide-react';
import { apiClient } from '../api/apiClient';
import type { FASubGroup, JournalMaster, JournalDetail } from '../types';

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
  const [voucherId, setVoucherId] = useState('');
  const [voucherDate, setVoucherDate] = useState(new Date().toISOString().split('T')[0]);
  const [narration, setNarration] = useState('');
  
  const [lines, setLines] = useState<VoucherLine[]>([
    { id: 1, sCode: '', jDrCr: 'DR', jAmount: '' },
    { id: 2, sCode: '', jDrCr: 'CR', jAmount: '' },
  ]);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen) {
      apiClient.getLedgers().then(setLedgers).catch(() => {});
      // Reset form
      setVoucherId('');
      setVoucherDate(new Date().toISOString().split('T')[0]);
      setNarration('');
      setLines([
        { id: 1, sCode: '', jDrCr: 'DR', jAmount: '' },
        { id: 2, sCode: '', jDrCr: 'CR', jAmount: '' },
      ]);
      setError(null);
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
    // Guard: must be balanced before submitting (defence-in-depth)
    if (!isBalanced) {
      setError('Voucher is not balanced. Total debits must equal total credits.');
      return;
    }

    const trimmedId = voucherId.trim();
    if (trimmedId.length !== 10) {
      setError('Voucher ID must be exactly 10 characters.');
      return;
    }

    // Validate details
    for (let i = 0; i < lines.length; i++) {
      const line = lines[i];
      if (!line.sCode) {
        setError(`Please select a valid ledger account for line item #${i + 1}.`);
        return;
      }
      const val = Number(line.jAmount) || 0;
      if (val <= 0) {
        setError(`Please enter a valid positive non-zero amount for line item #${i + 1}.`);
        return;
      }
    }

    let masterCreated = false;
    try {
      setLoading(true);
      setError(null);

      // Step 1: Create Master header
      const masterPayload: JournalMaster = {
        jId: trimmedId,
        jDoc: 'JV',
        jDate: new Date(voucherDate).toISOString(),
        jAmount: totalDebits,
        jNarr: narration.trim(),
      };
      await apiClient.createJournalMaster(masterPayload);
      masterCreated = true;

      // Step 2: Create Detail lines sequentially
      for (const line of lines) {
        const detailPayload: JournalDetail = {
          jId: trimmedId,
          jCode: line.sCode,
          jDrCr: line.jDrCr,
          jAmount: Number(line.jAmount),
        };
        await apiClient.createJournalDetail(detailPayload);
      }

      onSuccess();
      onClose();
    } catch (err: any) {
      // Rollback: if master was already created but details failed,
      // delete the orphaned master to maintain DB integrity
      if (masterCreated) {
        const trimmedId = voucherId.trim();
        try {
          // Delete any partial details first
          const partialDetails = await apiClient.getJournalDetails(trimmedId);
          for (const d of partialDetails) {
            await apiClient.deleteJournalDetail(d.jId, d.jCode, d.jDrCr);
          }
          await apiClient.deleteJournalMaster(trimmedId);
        } catch {
          // Rollback best-effort only — log silently
          console.error('[FA] Rollback attempt for orphaned master failed. Manual cleanup may be required for ID:', trimmedId);
        }
      }
      setError(err.message || 'Submission failed. Any partial data has been rolled back.');
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
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 bg-slate-950/30 p-4 border border-slate-800/40 rounded-xl">
            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-400 uppercase tracking-wider">Voucher ID (PK)</label>
              <input
                type="text"
                maxLength={10}
                placeholder="JV00000001 (10 Chars)"
                value={voucherId}
                onChange={(e) => setVoucherId(e.target.value.toUpperCase())}
                className="w-full bg-slate-950 border border-slate-800 focus:border-indigo-500 rounded-lg px-4 py-2.5 text-sm font-semibold text-slate-200 transition-colors placeholder-slate-700"
              />
            </div>
            
            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-400 uppercase tracking-wider">Voucher Date</label>
              <input
                type="date"
                value={voucherDate}
                onChange={(e) => setVoucherDate(e.target.value)}
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
                <div key={line.id} className="flex gap-4 items-center">
                  <div className="text-xs text-slate-500 font-bold w-6 text-center shrink-0">#{index + 1}</div>
                  
                  {/* Account select */}
                  <select
                    value={line.sCode}
                    onChange={(e) => updateLine(line.id, 'sCode', e.target.value)}
                    className="flex-1 bg-slate-950 border border-slate-800 focus:border-indigo-500 rounded-lg px-4 py-2 text-sm font-semibold text-slate-200 transition-colors"
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
                    className="w-36 bg-slate-950 border border-slate-800 focus:border-indigo-500 rounded-lg px-4 py-2 text-sm font-mono font-semibold text-slate-200 transition-colors placeholder-slate-850"
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
