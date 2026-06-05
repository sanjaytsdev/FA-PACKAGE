import React, { useCallback, useEffect, useState } from 'react';
import { RefreshCw, Radio } from 'lucide-react';
import { apiClient } from '../api/apiClient';

export const TopBar: React.FC = () => {
  const [status, setStatus] = useState<'checking' | 'connected' | 'disconnected'>('checking');

  const checkConnection = useCallback(async () => {
    setStatus('checking');
    const connected = await apiClient.ping();
    setStatus(connected ? 'connected' : 'disconnected');
  }, []);

  useEffect(() => {
    checkConnection();
    const interval = setInterval(checkConnection, 15000);
    return () => clearInterval(interval);
  }, [checkConnection]);

  return (
    <header className="h-16 bg-slate-900 border-b border-slate-800 flex items-center justify-end px-8 select-none shrink-0">
      <div className="flex items-center gap-6">
        <div className="flex items-center gap-2">
          <Radio className={`w-4 h-4 animate-pulse ${
            status === 'connected' ? 'text-emerald-500' : status === 'disconnected' ? 'text-rose-500' : 'text-amber-500'
          }`} />
          <span className={`text-xs font-semibold uppercase tracking-wider ${
            status === 'connected' ? 'text-emerald-400' : status === 'disconnected' ? 'text-rose-400' : 'text-slate-500'
          }`}>
            {status === 'connected' ? 'Connected' : status === 'disconnected' ? 'Disconnected' : 'Checking Link...'}
          </span>
        </div>
        <button
          onClick={checkConnection}
          className="flex items-center gap-1.5 px-3 py-1.5 border border-slate-700 hover:bg-slate-800 text-xs font-bold rounded-md text-slate-300 transition-colors"
        >
          <RefreshCw className="w-3.5 h-3.5" />
          Refresh Link
        </button>
      </div>
    </header>
  );
};