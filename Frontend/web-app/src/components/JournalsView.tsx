import React, { useEffect, useState } from 'react';
import { FileSpreadsheet, PlusCircle, Trash2, Undo2, AlertCircle, Sparkles, Loader2 } from 'lucide-react';
import { apiClient } from '../api/apiClient';
import type { JournalMaster, JournalDetail } from '../types';
import { NewVoucherModal } from './NewVoucherModal';

export const JournalsView: React.FC = () => {
  const [vouchers, setVouchers] = useState<JournalMaster[]>([]);
  const [selectedVoucher, setSelectedVoucher] = useState<JournalMaster | null>(null);
  const [details, setDetails] = useState<JournalDetail[]>([]);
  
  const [loading, setLoading] = useState(false);
  const [detailsLoading, setDetailsLoading] = useState(false);
  const [alert, setAlert] = useState<{ type: 'success' | 'error' | 'warn'; title: string; message: string } | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);

  const loadVouchers = async () => {
    try {
      setLoading(true);
      const data = await apiClient.getJournalMasters();
      setVouchers(data);
      if (selectedVoucher) {
        // Refresh the details for whatever's selected
        const refreshedSelected = data.find(v => v.jId === selectedVoucher.jId);
        if (refreshedSelected) {
          setSelectedVoucher(refreshedSelected);
          await loadDetails(refreshedSelected.jId!);
        } else {
          setSelectedVoucher(null);
          setDetails([]);
        }
      }
    } catch (err: any) {
      triggerAlert('error', 'Fetch Failure', err.message || 'Failed to load journal vouchers.');
    } finally {
      setLoading(false);
    }
  };

  const loadDetails = async (jId: string) => {
    try {
      setDetailsLoading(true);
      const data = await apiClient.getJournalDetails(jId);
      setDetails(data);
    } catch (err: any) {
      triggerAlert('error', 'Fetch Failure', err.message || 'Failed to load voucher line items.');
    } finally {
      setDetailsLoading(false);
    }
  };

  useEffect(() => {
    loadVouchers();
  }, []);

  const triggerAlert = (type: 'success' | 'error' | 'warn', title: string, message: string) => {
    setAlert({ type, title, message });
    setTimeout(() => setAlert(null), 5000);
  };

  const handleSelectVoucher = async (v: JournalMaster) => {
    setSelectedVoucher(v);
    await loadDetails(v.jId!);
  };

  const handleDelete = async () => {
    if (!selectedVoucher) return;

    if (!window.confirm(`Are you sure you want to delete Voucher ID "${selectedVoucher.jId}" and all its detail lines?`)) {
      return;
    }

    try {
      setLoading(true);

      // Server deletes the header and all its lines in one shot.
      await apiClient.deleteJournalVoucher(selectedVoucher.jId!);

      triggerAlert('success', 'Voucher Deleted', `Voucher ID "${selectedVoucher.jId}" successfully deleted.`);
      setSelectedVoucher(null);
      setDetails([]);
      await loadVouchers();
    } catch (err: any) {
      triggerAlert('error', 'Deletion Failed', err.message || 'Failed to delete journal voucher.');
    } finally {
      setLoading(false);
    }
  };

  const handleReverse = async () => {
    if (!selectedVoucher) return;

    if (!window.confirm(`Post a reversing voucher that cancels Voucher ID "${selectedVoucher.jId}"?`)) {
      return;
    }

    try {
      setLoading(true);

      // Server posts a new balanced voucher with each line's DR/CR flipped.
      const reversal = await apiClient.reverseJournalVoucher(selectedVoucher.jId!);

      triggerAlert('success', 'Voucher Reversed', `Reversing voucher "${reversal.jId}" posted for "${selectedVoucher.jId}".`);
      await loadVouchers();
    } catch (err: any) {
      triggerAlert('error', 'Reversal Failed', err.message || 'Failed to reverse journal voucher.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex-1 p-8 overflow-y-auto flex flex-col space-y-6">
      <div className="flex justify-between items-start">
        <div>
          <h2 className="text-3xl font-extrabold tracking-tight text-slate-100 flex items-center gap-3">
            <FileSpreadsheet className="w-8 h-8 text-indigo-500" />
            Journal Vouchers
          </h2>
          <p className="text-sm text-slate-400 mt-2 font-medium">
            Post balanced transactions, check voucher line registers, and audit double-entry inputs.
          </p>
        </div>
        <button
          onClick={() => setIsModalOpen(true)}
          className="flex items-center gap-2 px-5 py-2.5 bg-indigo-600 hover:bg-indigo-500 font-bold rounded-lg text-sm text-white transition-colors shadow-lg shadow-indigo-600/10 cursor-pointer"
        >
          <PlusCircle className="w-5 h-5" />
          New Journal Voucher
        </button>
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
        {/* Left Side: Master List Table */}
        <div className="flex-grow w-full lg:w-3/5 bg-slate-900/60 border border-slate-800 rounded-xl overflow-hidden shadow-xl">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-slate-900 border-b border-slate-800">
                  <th className="px-6 py-4 text-xs font-bold uppercase tracking-wider text-slate-400">Voucher ID</th>
                  <th className="px-6 py-4 text-xs font-bold uppercase tracking-wider text-slate-400">Doc</th>
                  <th className="px-6 py-4 text-xs font-bold uppercase tracking-wider text-slate-400">Date</th>
                  <th className="px-6 py-4 text-xs font-bold uppercase tracking-wider text-slate-400">Total Amount</th>
                  <th className="px-6 py-4 text-xs font-bold uppercase tracking-wider text-slate-400">Narration</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {loading && vouchers.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="px-6 py-8 text-center text-sm font-semibold text-slate-500">
                      <Loader2 className="w-5 h-5 animate-spin inline-block text-indigo-500 mr-2" />
                      Loading vouchers...
                    </td>
                  </tr>
                ) : vouchers.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="px-6 py-8 text-center text-sm font-semibold text-slate-500">
                      No Journal Vouchers found. Click "New Journal Voucher" to post your first entry!
                    </td>
                  </tr>
                ) : (
                  vouchers.map((v) => {
                    const isSelected = selectedVoucher?.jId === v.jId;
                    const dateFormatted = v.jDate
                      ? new Date(v.jDate).toLocaleDateString('en-US', {
                          year: 'numeric',
                          month: 'short',
                          day: 'numeric',
                          hour: '2-digit',
                          minute: '2-digit',
                        })
                      : '';
                    return (
                      <tr
                        key={v.jId}
                        onClick={() => handleSelectVoucher(v)}
                        className={`cursor-pointer transition-colors ${
                          isSelected ? 'bg-indigo-600/20 hover:bg-indigo-600/30' : 'hover:bg-slate-800/40'
                        }`}
                      >
                        <td className="px-6 py-4 text-sm font-bold tracking-wider text-slate-200">{v.jId}</td>
                        <td className="px-6 py-4 text-sm font-medium text-slate-400">{v.jDoc}</td>
                        <td className="px-6 py-4 text-sm font-medium text-slate-400">{dateFormatted}</td>
                        <td className="px-6 py-4 text-sm font-mono font-semibold text-slate-300">
                          {Number(v.jAmount || 0).toLocaleString('en-US', { minimumFractionDigits: 2 })}
                        </td>
                        <td className="px-6 py-4 text-sm font-medium text-slate-400 max-w-xs truncate">{v.jNarr || '-'}</td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* Right Side: Details Card Panel */}
        <div className="w-full lg:w-96 bg-slate-900 border border-slate-800 rounded-xl p-6 shadow-xl shrink-0 space-y-6">
          <div className="flex items-center justify-between border-b border-slate-800 pb-4">
            <h3 className="text-md font-extrabold text-slate-200 flex items-center gap-2">
              <Sparkles className="w-4 h-4 text-indigo-400" />
              Voucher Line Register
            </h3>
            {selectedVoucher && (
              <div className="flex items-center gap-3">
                <button
                  onClick={handleReverse}
                  className="text-amber-400 hover:text-amber-300 flex items-center gap-1 text-xs font-bold transition-colors cursor-pointer"
                  title="Post a Reversing Voucher"
                >
                  <Undo2 className="w-3.5 h-3.5" />
                  Reverse
                </button>
                <button
                  onClick={handleDelete}
                  className="text-rose-400 hover:text-rose-300 flex items-center gap-1 text-xs font-bold transition-colors cursor-pointer"
                  title="Delete Entire Voucher"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                  Delete
                </button>
              </div>
            )}
          </div>

          {detailsLoading ? (
            <div className="flex flex-col items-center justify-center py-12 text-slate-500 gap-2">
              <Loader2 className="w-6 h-6 animate-spin text-indigo-500" />
              <span className="text-xs font-semibold">Loading transaction lines...</span>
            </div>
          ) : !selectedVoucher ? (
            <div className="text-center py-12 text-sm font-semibold text-slate-500">
              Select a journal voucher from the register to audit and review individual ledger entries.
            </div>
          ) : (
            <div className="space-y-4">
              <div className="bg-slate-950/40 p-3 rounded-lg border border-slate-800/40 space-y-1">
                <div className="text-2xs font-extrabold text-slate-500 uppercase tracking-widest">Active Header ID</div>
                <div className="text-sm font-black text-slate-200">{selectedVoucher.jId}</div>
                {selectedVoucher.jNarr && (
                  <>
                    <div className="text-2xs font-extrabold text-slate-500 uppercase tracking-widest mt-2">Narration</div>
                    <div className="text-xs text-slate-400 italic">"{selectedVoucher.jNarr}"</div>
                  </>
                )}
              </div>

              {/* Details table */}
              <div className="border border-slate-800 rounded-lg overflow-hidden">
                <table className="w-full text-left border-collapse">
                  <thead>
                    <tr className="bg-slate-950 border-b border-slate-800">
                      <th className="px-4 py-3 text-2xs font-bold uppercase tracking-wider text-slate-400">Account</th>
                      <th className="px-4 py-3 text-2xs font-bold uppercase tracking-wider text-slate-400">Side</th>
                      <th className="px-4 py-3 text-2xs font-bold uppercase tracking-wider text-slate-400 text-right">Amount</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/60">
                    {details.map((line, idx) => (
                      <tr key={idx} className="hover:bg-slate-850/30">
                        <td className="px-4 py-3 text-xs font-bold tracking-wider text-slate-300">{line.jCode}</td>
                        <td className={`px-4 py-3 text-2xs font-black ${line.jDrCr === 'DR' ? 'text-indigo-400' : 'text-purple-400'}`}>
                          {line.jDrCr}
                        </td>
                        <td className="px-4 py-3 text-xs font-mono font-semibold text-right text-slate-300">
                          {Number(line.jAmount || 0).toLocaleString('en-US', { minimumFractionDigits: 2 })}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>
      </div>

      <NewVoucherModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSuccess={loadVouchers}
      />
    </div>
  );
};
