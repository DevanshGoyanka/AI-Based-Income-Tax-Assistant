import { useState } from 'react';
import Card from '../components/ui/Card';
import Badge from '../components/ui/Badge';

const MONTHS = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];
const DAYS_IN_MONTH = 30;

export default function Calendar() {
  const [selectedMonth, setSelectedMonth] = useState('April');
  const [selectedYear] = useState(2026);

  const deadlines = [
    { day: 15, label: 'Advance Tax Q4', type: 'deadline' },
    { day: 31, label: 'ITR Filing Deadline', type: 'deadline' },
    { day: 8, label: 'Notice Response Due', type: 'urgent' },
    { day: 22, label: 'Team Meeting', type: 'event' },
  ];

  return (
    <div>
      <div className="flex items-center justify-between mb-5">
        <div className="flex items-center gap-3">
          <select value={selectedMonth} onChange={e => setSelectedMonth(e.target.value)}
            className="px-3 py-2 border border-border-strong rounded-lg text-[14px] font-semibold outline-none focus:border-gold">
            {MONTHS.map(m => <option key={m}>{m}</option>)}
          </select>
          <span className="text-[14px] font-semibold text-text-secondary">{selectedYear}</span>
        </div>
        <div className="flex gap-4 text-[12px]">
          <div className="flex items-center gap-2">
            <div className="w-2 h-2 rounded-full bg-danger" />
            <span className="text-text-muted">Deadline</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="w-2 h-2 rounded-full bg-accent-blue" />
            <span className="text-text-muted">Event</span>
          </div>
        </div>
      </div>

      <Card>
        <div className="grid grid-cols-7 gap-2 mb-3">
          {['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'].map(day => (
            <div key={day} className="text-center text-[11px] font-semibold text-text-muted uppercase py-2">
              {day}
            </div>
          ))}
        </div>
        <div className="grid grid-cols-7 gap-2">
          {Array.from({ length: DAYS_IN_MONTH }, (_, i) => i + 1).map(day => {
            const dayDeadlines = deadlines.filter(d => d.day === day);
            return (
              <div key={day} className="aspect-square border border-border rounded-lg p-2 hover:bg-bg transition-colors cursor-pointer">
                <div className="text-[13px] font-medium text-text-primary mb-1">{day}</div>
                {dayDeadlines.map((d, idx) => (
                  <div key={idx} className={`w-2 h-2 rounded-full mb-1 ${d.type === 'urgent' ? 'bg-danger' : d.type === 'deadline' ? 'bg-warning' : 'bg-accent-blue'}`} />
                ))}
              </div>
            );
          })}
        </div>
      </Card>

      <Card className="mt-4">
        <div className="text-[14px] font-semibold mb-4">Deadlines This Month</div>
        <div className="space-y-2">
          {deadlines.map((d, idx) => (
            <div key={idx} className="flex items-center justify-between p-3 border border-border rounded-lg">
              <div className="flex items-center gap-3">
                <div className="text-[18px] font-serif font-semibold text-text-primary w-8">{d.day}</div>
                <div>
                  <div className="text-[13px] font-medium">{d.label}</div>
                  <div className="text-[11px] text-text-muted">{selectedMonth} {d.day}, {selectedYear}</div>
                </div>
              </div>
              <Badge label={d.type === 'urgent' ? 'Urgent' : d.type === 'deadline' ? 'Deadline' : 'Event'} 
                variant={d.type === 'urgent' ? 'danger' : d.type === 'deadline' ? 'warning' : 'info'} />
            </div>
          ))}
        </div>
      </Card>
    </div>
  );
}
