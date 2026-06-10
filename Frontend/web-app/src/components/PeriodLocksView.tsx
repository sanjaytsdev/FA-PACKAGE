import React, { useEffect, useState } from 'react';
import { Lock, Plus, Trash2, AlertCircle, Loader2 } from 'lucide-react';
import { apiClient } from '../api/apiClient';
import { DatePicker } from './DatePicker';
import type { PeriodLock } from '../types';

export const PeriodLocksView: React.FC = () => {
  const [locks, setLocks] = useState<PeriodLock[]>([]);
  const [periodStart, setPeriodStart] = useState('');
  const [periodEnd, setPeriodEnd] = useState('');

  const [loading, setLoading] = useState(false);
  const [creating, setCreating] = useState(false);
  const [alert, setAlert] = useState<{ type: 'success' | 'error'; title: string; message: string } | null>(null);
  // Per-field validation messages, shown under each date input.
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const triggerAlert = (type: 'success' | 'error', title: string, message: string) => {
    setAlert({ type, title, message });
    setTimeout(() => setAlert(null), 6000);
  };

  const load = async () => {
    try {
      setLoading(true);
      setLocks(await apiClient.getPeriodLocks());
    } catch (err: any) {
      triggerAlert('error', 'Fetch Failure', err.message || 'Failed to load period locks.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const handleCreate = async () => {
    const errors: Record<string, string> = {};
    if (!periodStart) errors.periodStart = 'Period start date is required.';
    if (!periodEnd) errors.periodEnd = 'Period end date is required.';
    if (periodStart && periodEnd && periodStart > periodEnd) {
      errors.periodEnd = 'Period start must be on or before period end.';
    }
    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      return;
    }
    setFieldErrors({});

    try {
      setCreating(true);
      await apiClient.createPeriodLock({ periodStart, periodEnd });
      triggerAlert('success', 'Period Locked', `Period ${periodStart} → ${periodEnd} is now closed.`);
      setPeriodStart('');
      setPeriodEnd('');
      await load();
    } catch (err: any) {
      triggerAlert('error', 'Lock Failed', err.message || 'Failed to create period lock.');
    } finally {
      setCreating(false);
    }
  };

  const handleDelete = async (lock: PeriodLock) => {
    if (!window.confirm(`Unlock period ${lock.periodStart} → ${lock.periodEnd}?`)) return;
    try {
      setLoading(true);
      await apiClient.deletePeriodLock(lock.id);
      triggerAlert('success', 'Period Unlocked', `Period ${lock.periodStart} → ${lock.periodEnd} reopened.`);
      await load();
    } catch (err: any) {
      triggerAlert('error', 'Unlock Failed', err.message || 'Failed to delete period lock.');
    } finally {
      setLoading(false);
    }
  };

  const fmtDateTime = (iso: string) =>
    iso ? new Date(iso).toLocaleString('en-US', { dateStyle: 'medium', timeStyle: 'short' }) : '';

  return (
    <div className="flex-1 p-8 overflow-y-auto flex flex-col space-y-6">
      <div>
        <h2 className="text-3xl font-extrabold tracking-tight text-slate-100 flex items-center gap-3">
          <Lock className="w-8 h-8 text-indigo-500" />
          Period Locks
        </h2>
        <p className="text-sm text-slate-400 mt-2 font-medium">
          Close accounting periods. While a period is locked, no voucher can be posted or reversed into any date it covers.
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

      {/* Create form */}
      <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-6 shadow-xl flex flex-col md:flex-row items-end gap-4">
        <div className="space-y-1.5">
          <label className="text-xs font-bold text-slate-400 uppercase tracking-wider">Period Start</label>
          <DatePicker
            value={periodStart}
            onChange={setPeriodStart}
            className={`w-full bg-slate-950 border ${fieldErrors.periodStart ? 'border-rose-500' : 'border-slate-800'} focus:border-indigo-500 rounded-lg px-4 py-2.5 text-sm font-semibold text-slate-200 transition-colors`}
          />
          {fieldErrors.periodStart && <p className="text-2xs font-semibold text-rose-400">{fieldErrors.periodStart}</p>}
        </div>
        <div className="space-y-1.5">
          <label className="text-xs font-bold text-slate-400 uppercase tracking-wider">Period End</label>
          <DatePicker
            value={periodEnd}
            onChange={setPeriodEnd}
            className={`w-full bg-slate-950 border ${fieldErrors.periodEnd ? 'border-rose-500' : 'border-slate-800'} focus:border-indigo-500 rounded-lg px-4 py-2.5 text-sm font-semibold text-slate-200 transition-colors`}
          />
          {fieldErrors.periodEnd && <p className="text-2xs font-semibold text-rose-400">{fieldErrors.periodEnd}</p>}
        </div>
        <button
          onClick={handleCreate}
          disabled={creating}
          className="flex items-center gap-2 px-5 py-2.5 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-40 font-bold rounded-lg text-sm text-white transition-colors shadow-lg cursor-pointer"
        >
          {creating ? <Loader2 className="w-4 h-4 animate-spin" /> : <Plus className="w-4 h-4" />}
          Lock Period
        </button>
      </div>

      {/* Locks table */}
      <div className="flex-grow w-full bg-slate-900/60 border border-slate-800 rounded-xl overflow-hidden shadow-xl">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-slate-900 border-b border-slate-800">
                <th className="px-6 py-4 text-xs font-bold uppercase tracking-wider text-slate-400">Period Start</th>
                <th className="px-6 py-4 text-xs font-bold uppercase tracking-wider text-slate-400">Period End</th>
                <th className="px-6 py-4 text-xs font-bold uppercase tracking-wider text-slate-400">Locked At</th>
                <th className="px-6 py-4 text-xs font-bold uppercase tracking-wider text-slate-400">Locked By</th>
                <th className="px-6 py-4 text-xs font-bold uppercase tracking-wider text-slate-400 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60">
              {loading && locks.length === 0 ? (
                <tr>
                  <td colSpan={5} className="px-6 py-8 text-center text-sm font-semibold text-slate-500">
                    <Loader2 className="w-5 h-5 animate-spin inline-block text-indigo-500 mr-2" />
                    Loading period locks...
                  </td>
                </tr>
              ) : locks.length === 0 ? (
                <tr>
                  <td colSpan={5} className="px-6 py-8 text-center text-sm font-semibold text-slate-500">
                    No locked periods. All dates are currently open for posting.
                  </td>
                </tr>
              ) : (
                locks.map((lock) => (
                  <tr key={lock.id} className="hover:bg-slate-800/40">
                    <td className="px-6 py-4 text-sm font-bold tracking-wider text-slate-200">{lock.periodStart}</td>
                    <td className="px-6 py-4 text-sm font-bold tracking-wider text-slate-200">{lock.periodEnd}</td>
                    <td className="px-6 py-4 text-sm font-medium text-slate-400">{fmtDateTime(lock.lockedAt)}</td>
                    <td className="px-6 py-4 text-sm font-medium text-slate-400">{lock.lockedBy}</td>
                    <td className="px-6 py-4 text-right">
                      <button
                        onClick={() => handleDelete(lock)}
                        className="text-rose-400 hover:text-rose-300 inline-flex items-center gap-1 text-xs font-bold transition-colors cursor-pointer"
                        title="Unlock Period"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                        Unlock
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
