import { useState } from 'react';
import { Sidebar } from './components/Sidebar';
import { TopBar } from './components/TopBar';
import { DashboardView } from './components/DashboardView';
import { GroupsView } from './components/GroupsView';
import { LedgersView } from './components/LedgersView';
import { JournalsView } from './components/JournalsView';
import { TrialBalanceView } from './components/TrialBalanceView';
import { OpeningBalancesView } from './components/OpeningBalancesView';
import { PeriodLocksView } from './components/PeriodLocksView';
import { ProfitAndLossView } from './components/ProfitAndLossView';
import { BalanceSheetView } from './components/BalanceSheetView';

function App() {
  const [activeTab, setActiveTab] = useState<string>('dashboard');

  const renderContent = () => {
    switch (activeTab) {
      case 'dashboard':
        return <DashboardView />;
      case 'groups':
        return <GroupsView />;
      case 'ledgers':
        return <LedgersView />;
      case 'opening-balances':
        return <OpeningBalancesView />;
      case 'journals':
        return <JournalsView />;
      case 'trial-balance':
        return <TrialBalanceView />;
      case 'profit-and-loss':
        return <ProfitAndLossView />;
      case 'balance-sheet':
        return <BalanceSheetView />;
      case 'period-locks':
        return <PeriodLocksView />;
      default:
        return <DashboardView />;
    }
  };

  return (
    <div className="flex h-screen w-screen overflow-hidden bg-slate-950 text-slate-100">
      {/* Navigation Sidebar */}
      <Sidebar activeTab={activeTab} setActiveTab={setActiveTab} />

      {/* Main Panel */}
      <div className="flex-grow flex flex-col min-w-0">
        {/* Connection top bar */}
        <TopBar />
        
        {/* Active Content view */}
        <main className="flex-1 flex min-h-0 bg-slate-950">
          {renderContent()}
        </main>
      </div>
    </div>
  );
}

export default App;
