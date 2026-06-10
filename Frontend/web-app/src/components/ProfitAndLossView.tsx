import React, { useEffect, useState } from 'react';
import { TrendingUp, AlertCircle, Loader2, RefreshCw } from 'lucide-react';
import { apiClient } from '../api/apiClient';
import { DatePicker } from './DatePicker';
import type { ProfitAndLoss, ReportLineItem } from '../types';

export const ProfitAndLossView: React.FC = () => {
  const [report, setReport] = useState<ProfitAndLoss | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  // Empty means as of today; a date limits it to vouchers posted on or before it.
  const [asOfDate, setAsOfDate] = useState('');

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setReport(await apiClient.getProfitAndLoss(asOfDate || undefined));
    } catch (err: any) {
      setError(err.message || 'Failed to load profit & loss.');
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

  const section = (title: string, rows: ReportLineItem[], total: number, accent: string) => (
    <div className="shrink-0 bg-slate-900/60 border border-slate-800 rounded-xl overflow-hidden shadow-xl">
      <div className="px-6 py-3 bg-slate-900 border-b border-slate-800">
        <h3 className="text-sm font-extrabold uppercase tracking-wider text-slate-300">{title}</h3>
      </div>
      <table className="w-full text-left border-collapse">
        <tbody className="divide-y divide-slate-800/60">
          {rows.length === 0 ? (
            <tr>
              <td className="px-6 py-4 text-sm font-semibold text-slate-500" colSpan={3}>No accounts.</td>
            </tr>
          ) : (
            rows.map((row) => (
              <tr key={row.accountCode} className="hover:bg-slate-800/40">
                <td className="px-6 py-3 text-sm font-bold tracking-wider text-slate-200 w-32">{row.accountCode}</td>
                <td className="px-6 py-3 text-sm font-medium text-slate-300">{row.description}</td>
                <td className={`px-6 py-3 text-sm font-mono font-semibold text-right ${accent}`}>{fmt(row.amount)}</td>
              </tr>
            ))
          )}
        </tbody>
        <tfoot>
          <tr className="bg-slate-900 border-t-2 border-slate-700">
            <td className="px-6 py-3 text-sm font-extrabold text-slate-200" colSpan={2}>Total {title}</td>
            <td className={`px-6 py-3 text-sm font-mono font-black text-right ${accent}`}>{fmt(total)}</td>
          </tr>
        </tfoot>
      </table>
    </div>
  );

  const profitable = report ? report.netProfit >= 0 : true;

  return (
    <div className="flex-1 p-8 overflow-y-auto flex flex-col space-y-6">
      <div className="flex justify-between items-start">
        <div>
          <h2 className="text-3xl font-extrabold tracking-tight text-slate-100 flex items-center gap-3">
            <TrendingUp className="w-8 h-8 text-indigo-500" />
            Profit &amp; Loss
          </h2>
          <p className="text-sm text-slate-400 mt-2 font-medium">
            Income statement cumulative to the reporting date: revenue less expenses for every voucher dated on or before it.
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

      {loading && !report ? (
        <div className="flex flex-col items-center justify-center py-16 text-slate-500 gap-2">
          <Loader2 className="w-6 h-6 animate-spin text-indigo-500" />
          <span className="text-xs font-semibold">Loading profit &amp; loss...</span>
        </div>
      ) : report && (
        <>
          {section('Revenue', report.revenue, report.totalRevenue, 'text-emerald-400')}
          {section('Expenses', report.expenses, report.totalExpenses, 'text-rose-400')}

          {/* Net result */}
          <div
            className={`p-6 border rounded-xl flex items-center justify-between shadow-xl ${
              profitable
                ? 'bg-emerald-500/10 border-emerald-500/20'
                : 'bg-rose-500/10 border-rose-500/20'
            }`}
          >
            <div className="space-y-1">
              <span className="text-xs font-bold text-slate-400 uppercase tracking-widest">
                {profitable ? 'Net Profit' : 'Net Loss'}
              </span>
              <p className="text-sm text-slate-400 font-medium">Total Revenue − Total Expenses</p>
            </div>
            <p className={`text-3xl font-black tracking-tight font-mono ${profitable ? 'text-emerald-400' : 'text-rose-400'}`}>
              {fmt(report.netProfit)}
            </p>
          </div>
        </>
      )}
    </div>
  );
};
