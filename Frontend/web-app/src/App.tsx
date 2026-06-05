import { useState } from 'react';
import { Sidebar } from './components/Sidebar';
import { TopBar } from './components/TopBar';
import { DashboardView } from './components/DashboardView';
import { GroupsView } from './components/GroupsView';
import { LedgersView } from './components/LedgersView';
import { JournalsView } from './components/JournalsView';

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
      case 'journals':
        return <JournalsView />;
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
