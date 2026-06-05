import React, { useEffect, useState } from 'react';
import { FolderTree, Save, X, AlertCircle, Sparkles } from 'lucide-react';
import { apiClient } from '../api/apiClient';
import type { FAGroup } from '../types';

export const GroupsView: React.FC = () => {
  const [groups, setGroups] = useState<FAGroup[]>([]);
  const [selectedGroup, setSelectedGroup] = useState<FAGroup | null>(null);
  
  // Form fields
  const [code, setCode] = useState('');
  const [description, setDescription] = useState('');
  const [type, setType] = useState('');
  const [balance, setBalance] = useState('0.00');

  const [loading, setLoading] = useState(false);
  const [alert, setAlert] = useState<{ type: 'success' | 'error' | 'warn'; title: string; message: string } | null>(null);

  const loadGroups = async () => {
    try {
      setLoading(true);
      const data = await apiClient.getGroups();
      setGroups(data);
    } catch (err: any) {
      triggerAlert('error', 'Fetch Failure', err.message || 'Failed to load groups');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadGroups();
  }, []);

  const triggerAlert = (type: 'success' | 'error' | 'warn', title: string, message: string) => {
    setAlert({ type, title, message });
    setTimeout(() => setAlert(null), 5000);
  };

  const handleSelectGroup = (g: FAGroup) => {
    setSelectedGroup(g);
    setCode(g.accountCode);
    setDescription(g.accountDescription);
    setType(g.accountType);
    setBalance(g.accountCurrentBalance?.toString() || '0.00');
  };

  const clearForm = () => {
    setSelectedGroup(null);
    setCode('');
    setDescription('');
    setType('');
    setBalance('0.00');
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();

    const trimmedCode = code.trim();
    const trimmedDesc = description.trim();
    
    if (trimmedCode.length !== 2) {
      triggerAlert('warn', 'Validation Error', 'Group Code must be exactly 2 characters long.');
      return;
    }

    if (!trimmedDesc) {
      triggerAlert('warn', 'Validation Error', 'Please enter a valid description.');
      return;
    }

    if (!type) {
      triggerAlert('warn', 'Validation Error', 'Please choose a Group Class type.');
      return;
    }

    const numBal = Number(balance);
    if (isNaN(numBal)) {
      triggerAlert('warn', 'Validation Error', 'Please enter a valid numeric aggregated balance.');
      return;
    }

    const groupPayload: FAGroup = {
      accountCode: trimmedCode,
      accountDescription: trimmedDesc,
      accountType: type,
      accountCurrentBalance: numBal,
    };

    try {
      setLoading(true);
      if (!selectedGroup) {
        // Create new
        await apiClient.createGroup(groupPayload);
        triggerAlert('success', 'Group Created', `Account group "${trimmedDesc}" successfully added.`);
      } else {
        // Update existing
        await apiClient.updateGroup(trimmedCode, groupPayload);
        triggerAlert('success', 'Group Updated', `Account group "${trimmedDesc}" successfully updated.`);
      }
      clearForm();
      await loadGroups();
    } catch (err: any) {
      triggerAlert('error', 'Operation Failed', err.message || 'Failed to submit group.');
    } finally {
      setLoading(false);
    }
  };

  const groupTypes: Record<string, string> = {
    '0': '0 - Asset',
    '1': '1 - Liability',
    '2': '2 - Equity',
    '3': '3 - Income',
    '4': '4 - Expense',
  };

  return (
    <div className="flex-1 p-8 overflow-y-auto flex flex-col space-y-6">
      <div className="flex justify-between items-start">
        <div>
          <h2 className="text-3xl font-extrabold tracking-tight text-slate-100 flex items-center gap-3">
            <FolderTree className="w-8 h-8 text-indigo-500" />
            Account Groups
          </h2>
          <p className="text-sm text-slate-400 mt-2 font-medium">
            Manage high-level chart classifications (Asset, Liability, Equity, etc.).
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
                  <th className="px-6 py-4 text-xs font-bold uppercase tracking-wider text-slate-400">Group Code</th>
                  <th className="px-6 py-4 text-xs font-bold uppercase tracking-wider text-slate-400">Description</th>
                  <th className="px-6 py-4 text-xs font-bold uppercase tracking-wider text-slate-400">Type Class</th>
                  <th className="px-6 py-4 text-xs font-bold uppercase tracking-wider text-slate-400 text-right">Aggregate Balance</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {groups.length === 0 ? (
                  <tr>
                    <td colSpan={4} className="px-6 py-8 text-center text-sm font-semibold text-slate-500">
                      No account groups found. Use the editor to add your first group!
                    </td>
                  </tr>
                ) : (
                  groups.map((group) => {
                    const isSelected = selectedGroup?.accountCode === group.accountCode;
                    return (
                      <tr
                        key={group.accountCode}
                        onClick={() => handleSelectGroup(group)}
                        className={`cursor-pointer transition-colors ${
                          isSelected ? 'bg-indigo-600/20 hover:bg-indigo-600/30' : 'hover:bg-slate-800/40'
                        }`}
                      >
                        <td className="px-6 py-4 text-sm font-bold tracking-wider text-slate-200">{group.accountCode}</td>
                        <td className="px-6 py-4 text-sm font-medium text-slate-300">{group.accountDescription}</td>
                        <td className="px-6 py-4 text-sm font-medium text-slate-400">{groupTypes[group.accountType] || group.accountType}</td>
                        <td className="px-6 py-4 text-sm font-mono font-semibold text-right text-slate-300">
                          {Number(group.accountCurrentBalance || 0).toLocaleString('en-US', { minimumFractionDigits: 2 })}
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
              {selectedGroup ? 'Update Account Group' : 'Manage Account Group'}
            </h3>
            {selectedGroup && (
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
              <label className="text-xs font-bold text-slate-400 uppercase tracking-wider">Account Group Code</label>
              <input
                type="text"
                maxLength={2}
                disabled={!!selectedGroup}
                placeholder="e.g. 01 (Exactly 2 Chars)"
                value={code}
                onChange={(e) => setCode(e.target.value.toUpperCase())}
                className="w-full bg-slate-950 border border-slate-800 focus:border-indigo-500 rounded-lg px-4 py-2.5 text-sm font-semibold text-slate-200 disabled:opacity-50 transition-colors placeholder-slate-600"
              />
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-400 uppercase tracking-wider">Description</label>
              <input
                type="text"
                placeholder="e.g. Current Assets"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                className="w-full bg-slate-950 border border-slate-800 focus:border-indigo-500 rounded-lg px-4 py-2.5 text-sm font-semibold text-slate-200 transition-colors placeholder-slate-600"
              />
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-400 uppercase tracking-wider">Group Class</label>
              <select
                value={type}
                onChange={(e) => setType(e.target.value)}
                className="w-full bg-slate-950 border border-slate-800 focus:border-indigo-500 rounded-lg px-4 py-2.5 text-sm font-semibold text-slate-200 transition-colors"
              >
                <option value="">Select Group Class</option>
                <option value="0">0 - Asset</option>
                <option value="1">1 - Liability</option>
                <option value="2">2 - Equity</option>
                <option value="3">3 - Income</option>
                <option value="4">4 - Expense</option>
              </select>
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-400 uppercase tracking-wider">Aggregated Balance</label>
              <input
                type="text"
                placeholder="0.00"
                value={balance}
                onChange={(e) => setBalance(e.target.value)}
                className="w-full bg-slate-950 border border-slate-800 focus:border-indigo-500 rounded-lg px-4 py-2.5 text-sm font-mono font-semibold text-slate-200 transition-colors placeholder-slate-600"
              />
            </div>

            <div className="space-y-2 pt-2">
              <button
                type="submit"
                disabled={loading}
                className="w-full bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 text-white font-bold rounded-lg py-2.5 text-sm transition-colors flex items-center justify-center gap-2 shadow-lg shadow-indigo-600/10 cursor-pointer"
              >
                <Save className="w-4 h-4" />
                {selectedGroup ? 'Update Group' : 'Create Group'}
              </button>
              
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
