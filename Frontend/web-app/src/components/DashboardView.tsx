import React, { useEffect, useState } from 'react';
import { LayoutDashboard, Users, CreditCard, Scale, Loader2 } from 'lucide-react';
import { apiClient } from '../api/apiClient';

interface DashboardStats {
  groupsCount: number;
  ledgersCount: number;
  journalsCount: number;
  trialBalanceDiff: number;
  loading: boolean;
  error: string | null;
}

export const DashboardView: React.FC = () => {
  const [stats, setStats] = useState<DashboardStats>({
    groupsCount: 0,
    ledgersCount: 0,
    journalsCount: 0,
    trialBalanceDiff: 0,
    loading: true,
    error: null,
  });

  const loadData = async () => {
    try {
      setStats((prev) => ({ ...prev, loading: true, error: null }));
      const [groups, ledgers, journals] = await Promise.all([
        apiClient.getGroups(),
        apiClient.getLedgers(),
        apiClient.getJournalMasters(),
      ]);

      // Calculate trial balance. Debits - Credits should equal 0
      let balance = 0;
      ledgers.forEach((ledger) => {
        const val = Number(ledger.sOpbal) || 0;
        if (ledger.sDrCr === 'CR') {
          balance -= val;
        } else {
          balance += val;
        }
      });

      setStats({
        groupsCount: groups.length,
        ledgersCount: ledgers.length,
        journalsCount: journals.length,
        trialBalanceDiff: balance,
        loading: false,
        error: null,
      });
    } catch (err: any) {
      setStats((prev) => ({
        ...prev,
        loading: false,
        error: err.message || 'Failed to load dashboard data',
      }));
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  if (stats.loading) {
    return (
      <div className="flex-1 flex flex-col items-center justify-center text-slate-400 gap-3">
        <Loader2 className="w-10 h-10 animate-spin text-indigo-500" />
        <p className="text-sm font-semibold tracking-wide">Aggregating Ledger Statistics...</p>
      </div>
    );
  }

  if (stats.error) {
    return (
      <div className="flex-1 flex flex-col items-center justify-center text-slate-400 gap-4">
        <div className="p-4 bg-rose-500/10 border border-rose-500/20 text-rose-400 rounded-lg max-w-md text-center">
          <p className="text-sm font-bold">Data Fetch Pipeline Error</p>
          <p className="text-xs mt-1 opacity-80">{stats.error}</p>
        </div>
        <button
          onClick={loadData}
          className="px-4 py-2 bg-slate-800 border border-slate-700 hover:bg-slate-700 font-bold rounded-lg text-xs tracking-wide transition-all"
        >
          Retry Connection
        </button>
      </div>
    );
  }

  const statCards = [
    {
      title: 'Total Account Groups',
      value: stats.groupsCount,
      icon: Users,
      color: 'from-indigo-500 to-indigo-600',
      shadow: 'shadow-indigo-500/10',
    },
    {
      title: 'Total Ledger Accounts',
      value: stats.ledgersCount,
      icon: CreditCard,
      color: 'from-purple-500 to-purple-600',
      shadow: 'shadow-purple-500/10',
    },
    {
      title: 'Total Journal Entries',
      value: stats.journalsCount,
      icon: LayoutDashboard,
      color: 'from-cyan-500 to-cyan-600',
      shadow: 'shadow-cyan-500/10',
    },
  ];

  const isBalanced = Math.abs(stats.trialBalanceDiff) < 0.001;

  return (
    <div className="flex-1 p-8 overflow-y-auto space-y-8">
      <div>
        <h2 className="text-3xl font-extrabold tracking-tight text-slate-100 flex items-center gap-3">
          📊 Dashboard Overview
        </h2>
        <p className="text-sm text-slate-400 mt-2 font-medium">
          Real-time double-entry integrity metrics of your financial records.
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {statCards.map((card, idx) => {
          const Icon = card.icon;
          return (
            <div
              key={idx}
              className={`bg-slate-900/60 border border-slate-800/80 rounded-xl p-6 flex items-center justify-between shadow-xl ${card.shadow} transition-all duration-300 hover:border-slate-700 hover:translate-y-[-2px]`}
            >
              <div className="space-y-2">
                <span className="text-xs font-bold text-slate-400 uppercase tracking-widest">{card.title}</span>
                <p className="text-4xl font-extrabold text-slate-100">{card.value}</p>
              </div>
              <div className={`p-4 bg-gradient-to-br ${card.color} rounded-lg text-white`}>
                <Icon className="w-6 h-6" />
              </div>
            </div>
          );
        })}
      </div>

      <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-8 flex flex-col md:flex-row items-center justify-between gap-6 shadow-xl">
        <div className="space-y-3 max-w-xl text-center md:text-left">
          <h3 className="text-lg font-bold text-slate-200 flex items-center gap-2.5 justify-center md:justify-start">
            <Scale className="w-5 h-5 text-indigo-400" />
            Trial Balance Arithmetic Verification
          </h3>
          <p className="text-sm text-slate-400 leading-relaxed font-medium">
            Verifies that all debit balances equal credit balances across subgroups. Any net mismatch signifies a ledger integrity failure.
          </p>
        </div>
        <div className="flex flex-col items-center md:items-end gap-2 shrink-0">
          <span className="text-xs font-bold text-slate-500 uppercase tracking-widest">Aggregate Equation Status</span>
          <div className="flex items-center gap-3">
            <div className={`w-3.5 h-3.5 rounded-full animate-pulse ${isBalanced ? 'bg-emerald-500' : 'bg-rose-500'}`} />
            <p className={`text-2xl font-black tracking-tight ${isBalanced ? 'text-emerald-400' : 'text-rose-400'}`}>
              {isBalanced ? 'BALANCED (0.00)' : `MISMATCH (${stats.trialBalanceDiff.toFixed(2)})`}
            </p>
          </div>
          <span className="text-xs text-slate-500 font-medium">
            {isBalanced ? 'Ledger is mathematically sound.' : 'Warning: Double-entry imbalance detected!'}
          </span>
        </div>
      </div>
    </div>
  );
};
