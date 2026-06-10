import React, { useEffect, useRef, useState } from 'react';
import { Calendar, ChevronLeft, ChevronRight } from 'lucide-react';

interface DatePickerProps {
  /** Value in YYYY-MM-DD format (same as a native date input). */
  value: string;
  /** Called with the selected date in YYYY-MM-DD format. */
  onChange: (value: string) => void;
  /** Classes for the trigger button (matches the old input styling). */
  className?: string;
}

const WEEKDAYS = ['Su', 'Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa'];
const MONTHS = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December',
];

const pad = (n: number) => String(n).padStart(2, '0');
const toISO = (y: number, m: number, d: number) => `${y}-${pad(m + 1)}-${pad(d)}`;

const parse = (value: string): { y: number; m: number; d: number } | null => {
  const parts = value.split('-').map(Number);
  if (parts.length !== 3 || parts.some((n) => Number.isNaN(n))) return null;
  return { y: parts[0], m: parts[1] - 1, d: parts[2] };
};

/**
 * Compact, styled date picker to replace the bulky native browser date popup.
 * Takes and emits YYYY-MM-DD strings, so it drops in for <input type="date">.
 */
export const DatePicker: React.FC<DatePickerProps> = ({ value, onChange, className = '' }) => {
  const [open, setOpen] = useState(false);
  const selected = parse(value);
  const today = new Date();
  const [view, setView] = useState(() => ({
    y: selected ? selected.y : today.getFullYear(),
    m: selected ? selected.m : today.getMonth(),
  }));
  const ref = useRef<HTMLDivElement>(null);

  // Sync the visible month when the value changes from outside.
  useEffect(() => {
    if (selected) setView({ y: selected.y, m: selected.m });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [value]);

  // Close on outside click.
  useEffect(() => {
    if (!open) return;
    const handler = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, [open]);

  const firstWeekday = new Date(view.y, view.m, 1).getDay();
  const daysInMonth = new Date(view.y, view.m + 1, 0).getDate();
  const cells: (number | null)[] = [
    ...Array(firstWeekday).fill(null),
    ...Array.from({ length: daysInMonth }, (_, i) => i + 1),
  ];

  const prevMonth = () =>
    setView((v) => (v.m === 0 ? { y: v.y - 1, m: 11 } : { y: v.y, m: v.m - 1 }));
  const nextMonth = () =>
    setView((v) => (v.m === 11 ? { y: v.y + 1, m: 0 } : { y: v.y, m: v.m + 1 }));

  const label = selected
    ? `${MONTHS[selected.m].slice(0, 3)} ${selected.d}, ${selected.y}`
    : 'Select date';

  const isSelected = (d: number) =>
    selected && selected.y === view.y && selected.m === view.m && selected.d === d;
  const isToday = (d: number) =>
    view.y === today.getFullYear() && view.m === today.getMonth() && d === today.getDate();

  return (
    <div className="relative" ref={ref}>
      <button
        type="button"
        onClick={() => setOpen((o) => !o)}
        className={`flex items-center justify-between gap-2 ${className}`}
      >
        <span>{label}</span>
        <Calendar className="w-3 h-3 text-slate-500 shrink-0" />
      </button>

      {open && (
        <div className="absolute z-50 mt-2 w-56 bg-slate-900 border border-slate-700 rounded-lg shadow-2xl p-3 select-none">
          {/* Month nav */}
          <div className="flex items-center justify-between mb-2">
            <button
              type="button"
              onClick={prevMonth}
              className="p-1 rounded hover:bg-slate-800 text-slate-400 hover:text-slate-200 transition-colors"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <span className="text-xs font-bold text-slate-200">
              {MONTHS[view.m]} {view.y}
            </span>
            <button
              type="button"
              onClick={nextMonth}
              className="p-1 rounded hover:bg-slate-800 text-slate-400 hover:text-slate-200 transition-colors"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>

          {/* Weekday header */}
          <div className="grid grid-cols-7 gap-0.5 mb-1">
            {WEEKDAYS.map((w) => (
              <div key={w} className="text-center text-2xs font-bold text-slate-600 py-1">
                {w}
              </div>
            ))}
          </div>

          {/* Day grid */}
          <div className="grid grid-cols-7 gap-0.5">
            {cells.map((d, i) =>
              d === null ? (
                <div key={`e${i}`} />
              ) : (
                <button
                  type="button"
                  key={d}
                  onClick={() => {
                    onChange(toISO(view.y, view.m, d));
                    setOpen(false);
                  }}
                  className={`w-7 h-7 text-xs font-semibold rounded transition-colors
                    ${isSelected(d)
                      ? 'bg-indigo-600 text-white'
                      : isToday(d)
                        ? 'text-indigo-400 hover:bg-slate-800'
                        : 'text-slate-300 hover:bg-slate-800'}`}
                >
                  {d}
                </button>
              )
            )}
          </div>
        </div>
      )}
    </div>
  );
};
