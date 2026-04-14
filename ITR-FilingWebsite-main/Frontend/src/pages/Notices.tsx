import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { noticesApi } from '../api/notices';
import Card from '../components/ui/Card';
import Badge from '../components/ui/Badge';
import Button from '../components/ui/Button';
import StatCard from '../components/ui/StatCard';

export default function Notices() {
  const [selectedNotice, setSelectedNotice] = useState<any>(null);

  const { data: stats } = useQuery({
    queryKey: ['notices-stats'],
    queryFn: () => ({ active: 31, due: 7, scrutiny: 4, closed: 12 }),
  });

  const { data: notices } = useQuery({
    queryKey: ['notices-list'],
    queryFn: () => noticesApi.list(),
  });

  return (
    <div>
      <div className="grid grid-cols-4 gap-3.5 mb-5">
        <StatCard accent="danger" label="Active Notices" value={stats?.active ?? 31} />
        <StatCard accent="warning" label="Due This Week" value={stats?.due ?? 7} />
        <StatCard accent="rose" label="Scrutiny" value={stats?.scrutiny ?? 4} />
        <StatCard accent="success" label="Closed" value={stats?.closed ?? 12} />
      </div>

      <div className="grid grid-cols-[1fr_400px] gap-4">
        <Card>
          <div className="text-[14px] font-semibold mb-4">Active Notices</div>
          <div className="space-y-2">
            {(notices ?? [
              { id: 1, section: '143(1)(a)', priority: 'Urgent', clientName: 'Priya Nair', pan: 'ABCDE1234F', ay: '2024-25', receivedDate: 'Apr 8', dueDate: 'Today 5 PM' },
              { id: 2, section: '143(2)', priority: 'High', clientName: 'Rajesh Kumar', pan: 'XYZAB5678C', ay: '2023-24', receivedDate: 'Apr 5', dueDate: 'Apr 15' },
            ]).map((notice: any) => (
              <div
                key={notice.id}
                onClick={() => setSelectedNotice(notice)}
                className={`p-4 border rounded-lg cursor-pointer transition-all ${
                  selectedNotice?.id === notice.id ? 'border-gold bg-gold-pale' : 'border-border hover:bg-bg'
                } ${notice.priority === 'Urgent' ? 'border-l-4 border-l-danger' : notice.priority === 'High' ? 'border-l-4 border-l-warning' : ''}`}
              >
                <div className="flex items-start justify-between">
                  <div className="flex items-start gap-3">
                    <div className="text-[24px]">{notice.priority === 'Urgent' ? '⚠️' : notice.priority === 'High' ? '📋' : '📄'}</div>
                    <div>
                      <div className="text-[13.5px] font-semibold">{notice.section} — {notice.clientName}</div>
                      <div className="text-[12px] text-text-muted font-mono">{notice.pan} · AY {notice.ay}</div>
                      <div className="text-[11px] text-text-muted mt-1">Received: {notice.receivedDate} · Due: {notice.dueDate}</div>
                    </div>
                  </div>
                  <Badge label={notice.priority} variant={notice.priority === 'Urgent' ? 'danger' : notice.priority === 'High' ? 'warning' : 'info'} />
                </div>
              </div>
            ))}
          </div>
        </Card>

        {selectedNotice ? (
          <Card>
            <div className="text-[14px] font-semibold mb-4">Notice Details</div>
            <div className="space-y-4">
              <div>
                <div className="text-[11px] text-text-muted uppercase mb-1">Section</div>
                <div className="text-[15px] font-semibold">{selectedNotice.section}</div>
              </div>
              <div>
                <div className="text-[11px] text-text-muted uppercase mb-1">Client</div>
                <div className="text-[13px]">{selectedNotice.clientName}</div>
                <div className="text-[12px] text-text-muted font-mono">{selectedNotice.pan}</div>
              </div>
              <div>
                <div className="text-[11px] text-text-muted uppercase mb-1">Deadline Countdown</div>
                <div className="text-[20px] font-serif font-semibold text-danger">4h 18m</div>
              </div>
              <div>
                <label className="block text-[12px] font-semibold text-text-secondary mb-2">Response Notes</label>
                <textarea
                  className="w-full px-3 py-2 border border-border-strong rounded-lg text-[13px] outline-none focus:border-gold resize-none"
                  rows={4}
                  placeholder="Add response notes..."
                />
              </div>
              <div>
                <label className="block text-[12px] font-semibold text-text-secondary mb-2">Attach Documents</label>
                <Button variant="ghost" className="w-full">📎 Upload Files</Button>
              </div>
              <Button variant="primary" className="w-full">Submit Response</Button>
            </div>
          </Card>
        ) : (
          <Card>
            <div className="text-center py-12 text-text-muted">
              Select a notice to view details
            </div>
          </Card>
        )}
      </div>
    </div>
  );
}
