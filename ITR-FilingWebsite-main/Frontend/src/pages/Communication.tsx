import { useState } from 'react';
import { useQuery, useMutation } from '@tanstack/react-query';
import { communicationApi } from '../api/communication';
import Card from '../components/ui/Card';
import Badge from '../components/ui/Badge';
import Button from '../components/ui/Button';
import toast from 'react-hot-toast';

export default function Communication() {
  const [template, setTemplate] = useState('advance_tax_reminder');
  const [recipients, setRecipients] = useState('ALL');
  const [message, setMessage] = useState('');

  const { data: templates } = useQuery({ queryKey: ['templates'], queryFn: communicationApi.getTemplates });
  const { data: history } = useQuery({ queryKey: ['comm-history'], queryFn: communicationApi.getHistory });

  const broadcastMutation = useMutation({
    mutationFn: () => communicationApi.broadcast(template, recipients),
    onSuccess: () => toast.success('Broadcast scheduled'),
  });

  const MOCK_TEMPLATES = [
    { id: 'advance_tax_reminder', name: 'Advance Tax Reminder', channel: 'WhatsApp' },
    { id: 'filing_deadline', name: 'Filing Deadline Alert', channel: 'Email' },
    { id: 'notice_received', name: 'Notice Received', channel: 'SMS' },
  ];

  const MOCK_HISTORY = [
    { id: 1, template: 'Advance Tax Reminder', sent: 148, delivered: 142, failed: 6, timestamp: '2h ago' },
    { id: 2, template: 'Filing Deadline Alert', sent: 87, delivered: 87, failed: 0, timestamp: 'Yesterday' },
  ];

  return (
    <div className="grid grid-cols-[1fr_360px] gap-4">
      <div>
        <Card className="mb-4">
          <div className="text-[14px] font-semibold mb-4">Compose Broadcast</div>
          <div className="space-y-4">
            <div>
              <label className="block text-[12px] font-semibold text-text-secondary mb-2">Template</label>
              <select value={template} onChange={e => setTemplate(e.target.value)}
                className="w-full px-3 py-2 border border-border-strong rounded-lg text-[13px] outline-none focus:border-gold">
                {(templates ?? MOCK_TEMPLATES).map((t: any) => (
                  <option key={t.id} value={t.id}>{t.name} ({t.channel})</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-[12px] font-semibold text-text-secondary mb-2">Recipients</label>
              <select value={recipients} onChange={e => setRecipients(e.target.value)}
                className="w-full px-3 py-2 border border-border-strong rounded-lg text-[13px] outline-none focus:border-gold">
                <option value="ALL">All Clients (1,247)</option>
                <option value="PENDING">Pending Filings (142)</option>
                <option value="WATCHLIST">Watch List (18)</option>
                <option value="CUSTOM">Custom Selection</option>
              </select>
            </div>
            <div>
              <label className="block text-[12px] font-semibold text-text-secondary mb-2">Message Preview</label>
              <textarea
                value={message}
                onChange={e => setMessage(e.target.value)}
                className="w-full px-3 py-2 border border-border-strong rounded-lg text-[13px] outline-none focus:border-gold resize-none"
                rows={6}
                placeholder="Dear {{client_name}}, your advance tax payment for Q4 is due on {{due_date}}..."
              />
            </div>
            <div className="flex gap-2">
              <Button variant="primary" onClick={() => broadcastMutation.mutate()}>Send Now</Button>
              <Button variant="secondary">Schedule</Button>
              <Button variant="ghost">Test Send</Button>
            </div>
          </div>
        </Card>

        <Card>
          <div className="text-[14px] font-semibold mb-4">Broadcast History</div>
          <table className="w-full">
            <thead>
              <tr className="border-b border-border">
                {['Template', 'Sent', 'Delivered', 'Failed', 'Time'].map(h => (
                  <th key={h} className="text-left py-2 text-[11px] font-semibold text-text-muted uppercase">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {(history ?? MOCK_HISTORY).map((h: any) => (
                <tr key={h.id} className="border-b border-border">
                  <td className="py-3 text-[13px]">{h.template}</td>
                  <td className="py-3 text-[13px] font-mono">{h.sent}</td>
                  <td className="py-3 text-[13px] font-mono text-success">{h.delivered}</td>
                  <td className="py-3 text-[13px] font-mono text-danger">{h.failed}</td>
                  <td className="py-3 text-[12px] text-text-muted">{h.timestamp}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
      </div>

      <div className="space-y-4">
        <Card>
          <div className="text-[14px] font-semibold mb-4">Channel Status</div>
          <div className="space-y-3">
            <div className="p-3 bg-success-bg border border-success/20 rounded-lg">
              <div className="flex items-center justify-between mb-1">
                <span className="text-[13px] font-semibold text-success">WhatsApp Business</span>
                <Badge label="Active" variant="success" />
              </div>
              <div className="text-[11px] text-success">Connected · 1,247 contacts</div>
            </div>
            <div className="p-3 bg-success-bg border border-success/20 rounded-lg">
              <div className="flex items-center justify-between mb-1">
                <span className="text-[13px] font-semibold text-success">Email (SMTP)</span>
                <Badge label="Active" variant="success" />
              </div>
              <div className="text-[11px] text-success">Connected · 98.2% delivery rate</div>
            </div>
            <div className="p-3 bg-warning-bg border border-warning/20 rounded-lg">
              <div className="flex items-center justify-between mb-1">
                <span className="text-[13px] font-semibold text-warning">SMS Gateway</span>
                <Badge label="Low Credits" variant="warning" />
              </div>
              <div className="text-[11px] text-warning">142 credits remaining</div>
            </div>
          </div>
        </Card>

        <Card>
          <div className="text-[14px] font-semibold mb-4">Quick Stats</div>
          <div className="space-y-3">
            <div className="flex justify-between py-2 border-b border-border">
              <span className="text-[12px] text-text-muted">Messages Today</span>
              <span className="text-[13px] font-mono font-semibold">148</span>
            </div>
            <div className="flex justify-between py-2 border-b border-border">
              <span className="text-[12px] text-text-muted">Delivery Rate</span>
              <span className="text-[13px] font-mono font-semibold text-success">96.8%</span>
            </div>
            <div className="flex justify-between py-2">
              <span className="text-[12px] text-text-muted">Avg. Response Time</span>
              <span className="text-[13px] font-mono font-semibold">2.4h</span>
            </div>
          </div>
        </Card>
      </div>
    </div>
  );
}
