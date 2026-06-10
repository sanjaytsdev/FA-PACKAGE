import React from "react";
import { LayoutDashboard, FolderTree, Landmark, FileSpreadsheet, Scale, Wallet, Lock, TrendingUp, BookOpen } from 'lucide-react';

interface SidebarProps {
    activeTab: string;  // was `String` (the JS object); has to be the primitive `string`
    setActiveTab: (tab: string) => void;
}

export const Sidebar: React.FC<SidebarProps> = ({ activeTab, setActiveTab }) => {
    const items = [
        { id: 'dashboard', label: 'Dashboard', icon: LayoutDashboard },
        { id: 'groups', label: 'Account Groups', icon: FolderTree },
        { id: 'ledgers', label: 'Ledger Accounts', icon: Landmark },
        { id: 'opening-balances', label: 'Opening Balances', icon: Wallet },
        { id: 'journals', label: 'Journal Vouchers', icon: FileSpreadsheet },
        { id: 'trial-balance', label: 'Trial Balance', icon: Scale },
        { id: 'profit-and-loss', label: 'Profit & Loss', icon: TrendingUp },
        { id: 'balance-sheet', label: 'Balance Sheet', icon: BookOpen },
        { id: 'period-locks', label: 'Period Locks', icon: Lock },
    ];

    return (
        <aside className="w-52 bg-slate-900 border-r border-slate-800 flex flex-col h-screen select-none shrink-0">
            <div className="h-16 px-4 border-b border-slate-800 flex items-center">
                <h1 className="text-lg font-extrabold tracking-wider bg-gradient-to-r from-indigo-500 to-purple-500 bg-clip-text text-transparent">FA PACKAGE</h1>
            </div>

            <nav className="flex-1 p-4 space-y-2 overflow-y-auto">
                {items.map((item) => {
                    const Icon = item.icon;
                    const isActive = activeTab === item.id;
                    return (
                        <button
                            key={item.id}
                            onClick={() => setActiveTab(item.id)}
                            className={`w-full flex items-center gap-2 px-2.5 py-2.5 rounded-lg text-xs font-bold transition-all duration-200 ${isActive ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-600/20' : 'text-slate-400 hover:bg-slate-800 hover:text-slate-100'}`}
                        >
                            <Icon className="w-4 h-4 shrink-0" />
                            <span className="truncate">{item.label}</span>
                        </button>
                    );
                })}
            </nav>
        </aside>
    );
};