import React, { useEffect, useState } from 'react';
import { Scale, AlertCircle, Loader2, RefreshCw } from 'lucide-react';
import { apiClient } from '../api/apiClient';
import { DatePicker } from './DatePicker';
import type { TrialBalance } from '../types';

export const TrialBalanceView: React.FC = () => {
  const [report, setReport] = useState<TrialBalance | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  // Empty means as of today; a date limits it to vouchers posted on or before it.
  const [asOfDate, setAsOfDate] = useState('');

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setReport(await apiClient.getTrialBalance(asOfDate || undefined));
    } catch (err: any) {
      setError(err.message || 'Failed to load trial balance.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [asOfDate]);

  // Refresh clears the date so the report reloads as of today.
  // Clearing a set date triggers the effect above; if it's already empty,
  // just reload directly.
  const handleRefresh = () => {
    if (asOfDate) setAsOfDate('');
    else load();
  };

  const fmt = (n: number) =>
    Number(n || 0).toLocaleString('en-US', { minimumFractionDigits: 2 });

  return (
    <div className="flex-1 p-8 overflow-y-auto flex flex-col space-y-6">
      <div className="flex justify-between items-start">
        <div>
          <h2 className="text-3xl font-extrabold tracking-tight text-slate-100 flex items-center gap-3">
            <Scale className="w-8 h-8 text-indigo-500" />
            Trial Balance
          </h2>
          <p className="text-sm text-slate-400 mt-2 font-medium">
            Account balances derived from opening balances and posted journals. The books balance when total debits equal total credits.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <div className="flex flex-col gap-1">
            <label className="text-2xs font-bold text-slate-500 uppercase tracking-widest">As of date</label>
            <DatePicker
              value={asOfDate}
              onChange={setAsOfDate}
              className="bg-slate-950 border border-slate-800 focus:border-indigo-500 rounded-lg px-3 py-2 text-sm font-semibold text-slate-200 transition-colors"
            />
          </div>
          <button
            onClick={handleRefresh}
            disabled={loading}
            className="flex items-center gap-2 px-4 py-2.5 bg-slate-800 border border-slate-700 hover:bg-slate-700 font-bold rounded-lg text-sm text-slate-200 transition-colors cursor-pointer disabled:opacity-50 self-end"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
            Refresh
          </button>
        </div>
      </div>

      {error && (
        <div className="p-4 border rounded-xl flex items-start gap-3 shadow-lg bg-rose-500/10 border-rose-500/20 text-rose-400">
          <AlertCircle className="w-5 h-5 shrink-0 mt-0.5" />
          <div className="text-xs font-semibold">{error}</div>
        </div>
      )}

      {report && (
        <div
          className={`p-4 border rounded-xl flex items-center gap-3 shadow-lg ${
            report.balanced
              ? 'bg-emerald-500/10 border-emerald-500/20 text-emerald-400'
              : 'bg-rose-500/10 border-rose-500/20 text-rose-400'
          }`}
        >
          <div className={`w-2.5 h-2.5 rounded-full ${report.balanced ? 'bg-emerald-500 animate-pulse' : 'bg-rose-500'}`} />
          <span className="text-sm font-extrabold uppercase tracking-wide">
            {report.balanced ? 'In Balance' : 'Out of Balance'}
          </span>
        </div>
      )}

      <div className="shrink-0 w-full bg-slate-900/60 border border-slate-800 rounded-xl overflow-hidden shadow-xl">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-slate-900 border-b border-slate-800">
                <th className="px-4 py-2.5 text-xs font-bold uppercase tracking-wider text-slate-400">Account</th>
                <th className="px-4 py-2.5 text-xs font-bold uppercase tracking-wider text-slate-400">Description</th>
                <th className="px-4 py-2.5 text-xs font-bold uppercase tracking-wider text-slate-400 text-right">Debit</th>
                <th className="px-4 py-2.5 text-xs font-bold uppercase tracking-wider text-slate-400 text-right">Credit</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60">
              {loading && !report ? (
                <tr>
                  <td colSpan={4} className="px-6 py-8 text-center text-sm font-semibold text-slate-500">
                    <Loader2 className="w-5 h-5 animate-spin inline-block text-indigo-500 mr-2" />
                    Loading trial balance...
                  </td>
                </tr>
              ) : !report || report.rows.length === 0 ? (
                <tr>
                  <td colSpan={4} className="px-6 py-8 text-center text-sm font-semibold text-slate-500">
                    No ledger accounts found.
                  </td>
                </tr>
              ) : (
                report.rows.map((row) => (
                  <tr key={row.accountCode} className="hover:bg-slate-800/40">
                    <td className="px-4 py-2.5 text-sm font-bold tracking-wider text-slate-200">{row.accountCode}</td>
                    <td className="px-4 py-2.5 text-sm font-medium text-slate-300">{row.description}</td>
                    <td className="px-4 py-2.5 text-sm font-mono font-semibold text-right text-indigo-400">
                      {row.debit ? fmt(row.debit) : '-'}
                    </td>
                    <td className="px-4 py-2.5 text-sm font-mono font-semibold text-right text-purple-400">
                      {row.credit ? fmt(row.credit) : '-'}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
            {report && report.rows.length > 0 && (
              <tfoot>
                <tr className="bg-slate-900 border-t-2 border-slate-700">
                  <td className="px-4 py-2.5 text-sm font-extrabold text-slate-200" colSpan={2}>Total</td>
                  <td className="px-4 py-2.5 text-sm font-mono font-black text-right text-indigo-300">{fmt(report.totalDebit)}</td>
                  <td className="px-4 py-2.5 text-sm font-mono font-black text-right text-purple-300">{fmt(report.totalCredit)}</td>
                </tr>
              </tfoot>
            )}
          </table>
        </div>
      </div>
    </div>
  );
};
