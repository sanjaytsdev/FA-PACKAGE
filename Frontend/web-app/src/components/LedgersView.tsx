import React, { useEffect, useState } from 'react';
import { Landmark, Save, Trash2, X, AlertCircle, Sparkles } from 'lucide-react';
import { apiClient } from '../api/apiClient';
import type { FASubGroup, FAGroup } from '../types';

export const LedgersView: React.FC = () => {
  const [ledgers, setLedgers] = useState<FASubGroup[]>([]);
  const [groups, setGroups] = useState<FAGroup[]>([]);
  const [selectedLedger, setSelectedLedger] = useState<FASubGroup | null>(null);

  // Form Fields
  const [code, setCode] = useState('');
  const [description, setDescription] = useState('');
  const [parentGroupCode, setParentGroupCode] = useState('');
  const [subType, setSubType] = useState('');
  const [opBalance, setOpBalance] = useState('0.00');
  const [drCr, setDrCr] = useState<'DR' | 'CR' | ''>('');
  const [status, setStatus] = useState<'Active' | 'Inactive'>('Active');

  const [loading, setLoading] = useState(false);
  const [alert, setAlert] = useState<{ type: 'success' | 'error' | 'warn'; title: string; message: string } | null>(null);
  // Per-field validation messages, shown under each input.
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const loadData = async () => {
    try {
      setLoading(true);
      const [ledgersData, groupsData] = await Promise.all([
        apiClient.getLedgers(),
        apiClient.getGroups(),
      ]);
      setLedgers(ledgersData);
      setGroups(groupsData);
    } catch (err: any) {
      triggerAlert('error', 'Fetch Failure', err.message || 'Failed to load chart of accounts.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const triggerAlert = (type: 'success' | 'error' | 'warn', title: string, message: string) => {
    setAlert({ type, title, message });
    setTimeout(() => setAlert(null), 5000);
  };

  const handleSelectLedger = (l: FASubGroup) => {
    setSelectedLedger(l);
    setCode(l.sCode);
    setDescription(l.sDesc);
    setParentGroupCode(l.aCode);
    setSubType(l.sType);
    setOpBalance(l.sOpbal?.toString() || '0.00');
    setDrCr(l.sDrCr);
    setStatus(l.sFlag === 'T' ? 'Active' : 'Inactive');
  };

  const clearForm = () => {
    setSelectedLedger(null);
    setCode('');
    setDescription('');
    setParentGroupCode('');
    setSubType('');
    setOpBalance('0.00');
    setDrCr('');
    setStatus('Active');
    setFieldErrors({});
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();

    const trimmedCode = code.trim();
    const trimmedDesc = description.trim();
    const trimmedSubType = subType.trim();
    const numBal = Number(opBalance);

    const errors: Record<string, string> = {};
    if (trimmedCode.length !== 5) errors.code = 'Ledger Code must be exactly 5 characters.';
    if (!trimmedDesc) errors.description = 'Please enter an account description.';
    if (!parentGroupCode) errors.parentGroupCode = 'Please choose a parent account group.';
    if (trimmedSubType.length !== 2) errors.subType = 'Sub-Type must be exactly 2 characters (e.g. 00).';
    if (isNaN(numBal)) errors.opBalance = 'Please enter a valid numeric opening balance.';
    if (!drCr) errors.drCr = 'Please select DR or CR balance side.';

    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      return;
    }
    setFieldErrors({});

    const payload: FASubGroup = {
      sCode: trimmedCode,
      sDesc: trimmedDesc,
      aCode: parentGroupCode,
      sType: trimmedSubType,
      sOpbal: numBal,
      sDrCr: drCr as 'DR' | 'CR', // checked non-empty above
      sFlag: status === 'Active' ? 'T' : 'F',
    };

    try {
      setLoading(true);
      if (!selectedLedger) {
        await apiClient.createLedger(payload);
        triggerAlert('success', 'Account Created', `Ledger account "${trimmedDesc}" successfully added.`);
      } else {
        await apiClient.updateLedger(trimmedCode, payload);
        triggerAlert('success', 'Account Updated', `Ledger account "${trimmedDesc}" successfully updated.`);
      }
      clearForm();
      await loadData();
    } catch (err: any) {
      triggerAlert('error', 'Save Failure', err.message || 'Failed to submit account.');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!selectedLedger) return;

    if (!window.confirm(`Are you absolutely sure you want to permanently delete Ledger Account "${selectedLedger.sDesc}"? This cannot be undone.`)) {
      return;
    }

    try {
      setLoading(true);
      await apiClient.deleteLedger(selectedLedger.sCode);
      triggerAlert('success', 'Account Deleted', `Ledger account "${selectedLedger.sDesc}" successfully deleted.`);
      clearForm();
      await loadData();
    } catch (err: any) {
      triggerAlert('error', 'Deletion Error', err.message || 'Failed to delete ledger account.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex-1 p-8 overflow-y-auto flex flex-col space-y-6">
      <div className="flex justify-between items-start">
        <div>
          <h2 className="text-3xl font-extrabold tracking-tight text-slate-100 flex items-center gap-3">
            <Landmark className="w-8 h-8 text-indigo-500" />
            Ledger Accounts
          </h2>
          <p className="text-sm text-slate-400 mt-2 font-medium">
            Manage your Chart of Accounts, opening balances, normal sides, and operational status.
          </p>
        </div>
      </div>

      {alert && (
        <div className={`p-4 border rounded-xl flex items-start gap-3 shadow-lg transition-all duration-300 ${
          alert.type === 'success' ? 'bg-emerald-500/10 border-emerald-500/20 text-emerald-400' :
          alert.type === 'error' ? 'bg-rose-500/10 border-rose-500/20 text-rose-400' :
          'bg-amber-500/10 border-amber-500/20 text-amber-400'
        }`}>
          <AlertCircle className="w-5 h-5 shrink-0 mt-0.5" />
          <div>
            <h4 className="font-bold text-sm">{alert.title}</h4>
            <p className="text-xs mt-1 opacity-90">{alert.message}</p>
          </div>
        </div>
      )}

      <div className="flex flex-col lg:flex-row gap-8 items-start flex-grow">
        {/* Left Side: Table List */}
        <div className="flex-grow w-full lg:w-2/3 bg-slate-900/60 border border-slate-800 rounded-xl overflow-hidden shadow-xl">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-slate-900 border-b border-slate-800">
                  <th className="px-6 py-4 text-xs font-bold uppercase tracking-wider text-slate-400">Ledger Code</th>
                  <th className="px-6 py-4 text-xs font-bold uppercase tracking-wider text-slate-400">Description</th>
                  <th className="px-6 py-4 text-xs font-bold uppercase tracking-wider text-slate-400">Group</th>
                  <th className="px-6 py-4 text-xs font-bold uppercase tracking-wider text-slate-400">Sub-Type</th>
                  <th className="px-6 py-4 text-xs font-bold uppercase tracking-wider text-slate-400">Opening Bal</th>
                  <th className="px-6 py-4 text-xs font-bold uppercase tracking-wider text-slate-400">Side</th>
                  <th className="px-6 py-4 text-xs font-bold uppercase tracking-wider text-slate-400">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {ledgers.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="px-6 py-8 text-center text-sm font-semibold text-slate-500">
                      No ledger accounts found. Use the editor to add your first account!
                    </td>
                  </tr>
                ) : (
                  ledgers.map((ledger) => {
                    const isSelected = selectedLedger?.sCode === ledger.sCode;
                    return (
                      <tr
                        key={ledger.sCode}
                        onClick={() => handleSelectLedger(ledger)}
                        className={`cursor-pointer transition-colors ${
                          isSelected ? 'bg-indigo-600/20 hover:bg-indigo-600/30' : 'hover:bg-slate-800/40'
                        }`}
                      >
                        <td className="px-6 py-4 text-sm font-bold tracking-wider text-slate-200">{ledger.sCode}</td>
                        <td className="px-6 py-4 text-sm font-medium text-slate-300">{ledger.sDesc}</td>
                        <td className="px-6 py-4 text-sm font-medium text-slate-400">{ledger.aCode}</td>
                        <td className="px-6 py-4 text-sm font-medium text-slate-500">{ledger.sType}</td>
                        <td className="px-6 py-4 text-sm font-mono font-semibold text-slate-300">
                          {Number(ledger.sOpbal || 0).toLocaleString('en-US', { minimumFractionDigits: 2 })}
                        </td>
                        <td className={`px-6 py-4 text-xs font-bold ${ledger.sDrCr === 'DR' ? 'text-indigo-400' : 'text-purple-400'}`}>{ledger.sDrCr}</td>
                        <td className="px-6 py-4">
                          <span className={`px-2.5 py-1 text-2xs font-extrabold uppercase rounded-full ${
                            ledger.sFlag === 'T' ? 'bg-emerald-500/10 text-emerald-400' : 'bg-slate-800 text-slate-500'
                          }`}>
                            {ledger.sFlag === 'T' ? 'Active' : 'Inactive'}
                          </span>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* Right Side: Manage Form Card */}
        <div className="w-full lg:w-96 bg-slate-900 border border-slate-800 rounded-xl p-6 shadow-xl shrink-0 space-y-6">
          <div className="flex items-center justify-between border-b border-slate-800 pb-4">
            <h3 className="text-md font-extrabold text-slate-200 flex items-center gap-2">
              <Sparkles className="w-4 h-4 text-indigo-400" />
              {selectedLedger ? 'Update Ledger Account' : 'Manage Ledger Account'}
            </h3>
            {selectedLedger && (
              <button
                onClick={clearForm}
                className="text-slate-500 hover:text-slate-300 transition-colors"
                title="Cancel Edit"
              >
                <X className="w-4 h-4" />
              </button>
            )}
          </div>

          <form onSubmit={handleSave} className="space-y-4">
            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-400 uppercase tracking-wider">Ledger Account Code</label>
              <input
                type="text"
                maxLength={5}
                disabled={!!selectedLedger}
                placeholder="5 digits (e.g. 10001)"
                value={code}
                onChange={(e) => setCode(e.target.value.replace(/\D/g, ''))}
                className={`w-full bg-slate-950 border ${fieldErrors.code ? 'border-rose-500' : 'border-slate-800'} focus:border-indigo-500 rounded-lg px-4 py-2.5 text-sm font-semibold text-slate-200 disabled:opacity-50 transition-colors placeholder-slate-600`}
              />
              {fieldErrors.code && <p className="text-2xs font-semibold text-rose-400 mt-1">{fieldErrors.code}</p>}
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-400 uppercase tracking-wider">Account Description</label>
              <input
                type="text"
                placeholder="e.g. Cash at Bank"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                className={`w-full bg-slate-950 border ${fieldErrors.description ? 'border-rose-500' : 'border-slate-800'} focus:border-indigo-500 rounded-lg px-4 py-2.5 text-sm font-semibold text-slate-200 transition-colors placeholder-slate-600`}
              />
              {fieldErrors.description && <p className="text-2xs font-semibold text-rose-400 mt-1">{fieldErrors.description}</p>}
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-400 uppercase tracking-wider">Parent Account Group</label>
              <select
                value={parentGroupCode}
                onChange={(e) => {
                  const newCode = e.target.value;
                  setParentGroupCode(newCode);
                  // Auto-fill sub-type and normal side on create, not on edit
                  if (!selectedLedger) {
                    const group = groups.find((g) => g.accountCode === newCode);
                    if (group) {
                      setSubType(group.accountType + '0');
                      setDrCr(['0', '4'].includes(group.accountType) ? 'DR' : 'CR');
                    }
                  }
                }}
                className={`w-full bg-slate-950 border ${fieldErrors.parentGroupCode ? 'border-rose-500' : 'border-slate-800'} focus:border-indigo-500 rounded-lg px-4 py-2.5 text-sm font-semibold text-slate-200 transition-colors`}
              >
                <option value="">Select Parent Group</option>
                {groups.map((g) => (
                  <option key={g.accountCode} value={g.accountCode}>
                    {g.accountCode} - {g.accountDescription}
                  </option>
                ))}
              </select>
              {fieldErrors.parentGroupCode && <p className="text-2xs font-semibold text-rose-400 mt-1">{fieldErrors.parentGroupCode}</p>}
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-400 uppercase tracking-wider">Sub-Type Code</label>
              <input
                type="text"
                maxLength={2}
                placeholder="2-digit classification (e.g. 00)"
                value={subType}
                onChange={(e) => setSubType(e.target.value.replace(/\D/g, ''))}
                className={`w-full bg-slate-950 border ${fieldErrors.subType ? 'border-rose-500' : 'border-slate-800'} focus:border-indigo-500 rounded-lg px-4 py-2.5 text-sm font-semibold text-slate-200 transition-colors placeholder-slate-600`}
              />
              {fieldErrors.subType && <p className="text-2xs font-semibold text-rose-400 mt-1">{fieldErrors.subType}</p>}
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-400 uppercase tracking-wider">Opening Balance</label>
              <input
                type="text"
                placeholder="0.00"
                value={opBalance}
                onChange={(e) => setOpBalance(e.target.value)}
                className={`w-full bg-slate-950 border ${fieldErrors.opBalance ? 'border-rose-500' : 'border-slate-800'} focus:border-indigo-500 rounded-lg px-4 py-2.5 text-sm font-mono font-semibold text-slate-200 transition-colors placeholder-slate-600`}
              />
              {fieldErrors.opBalance && <p className="text-2xs font-semibold text-rose-400 mt-1">{fieldErrors.opBalance}</p>}
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <label className="text-xs font-bold text-slate-400 uppercase tracking-wider">Normal Side</label>
                <select
                  value={drCr}
                  onChange={(e) => setDrCr(e.target.value as 'DR' | 'CR')}
                  className={`w-full bg-slate-950 border ${fieldErrors.drCr ? 'border-rose-500' : 'border-slate-800'} focus:border-indigo-500 rounded-lg px-4 py-2.5 text-sm font-semibold text-slate-200 transition-colors`}
                >
                  <option value="">DR / CR</option>
                  <option value="DR">Debit (DR)</option>
                  <option value="CR">Credit (CR)</option>
                </select>
                {fieldErrors.drCr && <p className="text-2xs font-semibold text-rose-400 mt-1">{fieldErrors.drCr}</p>}
              </div>

              <div className="space-y-1.5">
                <label className="text-xs font-bold text-slate-400 uppercase tracking-wider">Status Flag</label>
                <select
                  value={status}
                  onChange={(e) => setStatus(e.target.value as 'Active' | 'Inactive')}
                  className="w-full bg-slate-950 border border-slate-800 focus:border-indigo-500 rounded-lg px-4 py-2.5 text-sm font-semibold text-slate-200 transition-colors"
                >
                  <option value="Active">Active</option>
                  <option value="Inactive">Inactive</option>
                </select>
              </div>
            </div>

            <div className="space-y-2 pt-2">
              <button
                type="submit"
                disabled={loading}
                className="w-full bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 text-white font-bold rounded-lg py-2.5 text-sm transition-colors flex items-center justify-center gap-2 shadow-lg shadow-indigo-600/10 cursor-pointer"
              >
                <Save className="w-4 h-4" />
                {selectedLedger ? 'Update Account' : 'Create Account'}
              </button>

              {selectedLedger && (
                <button
                  type="button"
                  onClick={handleDelete}
                  disabled={loading}
                  className="w-full bg-rose-600 hover:bg-rose-500 disabled:opacity-50 text-white font-bold rounded-lg py-2.5 text-sm transition-colors flex items-center justify-center gap-2 shadow-lg cursor-pointer"
                >
                  <Trash2 className="w-4 h-4" />
                  Delete Account
                </button>
              )}

              <button
                type="button"
                onClick={clearForm}
                className="w-full bg-transparent border border-slate-700 hover:bg-slate-800 text-slate-300 font-bold rounded-lg py-2.5 text-sm transition-colors cursor-pointer"
              >
                Clear Form
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};
