import { useState } from 'react';
import { useQuery, useMutation } from '@tanstack/react-query';
import { syncApi } from '../api/sync';
import Card from '../components/ui/Card';
import Button from '../components/ui/Button';
import Badge from '../components/ui/Badge';
import toast from 'react-hot-toast';

export default function Sync() {
  const [scope, setScope] = useState('ALL');
  const [ay, setAY] = useState('2025-26');
  const [documents, setDocuments] = useState('26AS,AIS,TIS');
  const [batchSize, setBatchSize] = useState('50');

  const { data: history } = useQuery({
    queryKey: ['sync-history'],
    queryFn: syncApi.getHistory,
  });

  const triggerMutation = useMutation({
    mutationFn: () => syncApi.trigger({ scope, assessmentYear: ay, documents, batchSize: Number(batchSize) }),
    onSuccess: () => toast.success('Sync job started'),
  });

  return (
    <div className="grid grid-cols-[1fr_360px] gap-4">
      {/* Left: Trigger form */}
      <div>
        <Card className="mb-4">
          <div className="text-[14px] font-semibold mb-4">Trigger ITD Portal Sync</div>
          <div className="grid grid-cols-2 gap-4 mb-4">
            <div>
              <label className="block text-[12px] font-semibold text-text-secondary mb-2">Scope</label>
              <select value={scope} onChange={e => setScope(e.target.value)}
                className="w-full px-3 py-2 border border-border-strong rounded-lg text-[13px] outline-none focus:border-gold">
                <option value="ALL">All Clients</option>
                <option value="PENDING">Pending Only</option>
                <option value="WATCHLIST">Watch List</option>
                <option value="CUSTOM">Custom Selection</option>
              </select>
            </div>
            <div>
              <label className="block text-[12px] font-semibold text-text-secondary mb-2">Assessment Year</label>
              <select value={ay} onChange={e => setAY(e.target.value)}
                className="w-full px-3 py-2 border border-border-strong rounded-lg text-[13px] outline-none focus:border-gold">
                <option value="2025-26">AY 2025–26</option>
                <option value="2026-27">AY 2026–27</option>
              </select>
            </div>
            <div>
              <label className="block text-[12px] font-semibold text-text-secondary mb-2">Documents</label>
              <select value={documents} onChange={e => setDocuments(e.target.value)}
                className="w-full px-3 py-2 border border-border-strong rounded-lg text-[13px] outline-none focus:border-gold">
                <option value="26AS,AIS,TIS">All (26AS + AIS + TIS)</option>
                <option value="26AS">26AS Only</option>
                <option value="AIS">AIS Only</option>
                <option value="TIS">TIS Only</option>
              </select>
            </div>
            <div>
              <label className="block text-[12px] font-semibold text-text-secondary mb-2">Batch Size</label>
              <input type="number" value={batchSize} onChange={e => setBatchSize(e.target.value)}
                className="w-full px-3 py-2 border border-border-strong rounded-lg text-[13px] font-mono outline-none focus:border-gold" />
            </div>
          </div>

          <div className="p-3 bg-warning-bg border border-warning/20 rounded-lg mb-4 text-[12px] text-warning">
            ⚠ Sync will use Playwright automation. Ensure CAPTCHA solver is active. Large batches may take 2-4 hours.
          </div>

          <div className="flex gap-2">
            <Button variant="primary" onClick={() => triggerMutation.mutate()}>Start Sync Job</Button>
            <Button variant="secondary">Schedule for Tonight</Button>
            <Button variant="ghost">Test Single Client</Button>
          </div>
        </Card>

        {/* Sync history */}
        <Card>
          <div className="text-[14px] font-semibold mb-4">Sync History</div>
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b border-border">
                  <th className="text-left text-[11px] font-semibold text-text-muted uppercase pb-2">Job ID</th>
                  <th className="text-left text-[11px] font-semibold text-text-muted uppercase pb-2">Started</th>
                  <th className="text-left text-[11px] font-semibold text-text-muted uppercase pb-2">Scope</th>
                  <th className="text-left text-[11px] font-semibold text-text-muted uppercase pb-2">AY</th>
                  <th className="text-right text-[11px] font-semibold text-text-muted uppercase pb-2">Total</th>
                  <th className="text-right text-[11px] font-semibold text-text-muted uppercase pb-2">Success</th>
                  <th className="text-right text-[11px] font-semibold text-text-muted uppercase pb-2">Failed</th>
                  <th className="text-left text-[11px] font-semibold text-text-muted uppercase pb-2">Duration</th>
                  <th className="text-left text-[11px] font-semibold text-text-muted uppercase pb-2">Status</th>
                  <th className="text-right text-[11px] font-semibold text-text-muted uppercase pb-2">Actions</th>
                </tr>
              </thead>
              <tbody>
                {(history ?? [
                  { jobId: 847, startedAt: '11:42 AM', scope: 'ALL', ay: '2025-26', total: 200, success: 148, failed: 4, duration: '2h 18m', status: 'RUNNING' },
                  { jobId: 846, startedAt: 'Yesterday', scope: 'PENDING', ay: '2025-26', total: 150, success: 150, failed: 0, duration: '1h 45m', status: 'COMPLETED' },
                ]).map((job: any) => (
                  <tr key={job.jobId} className="border-b border-border">
                    <td className="py-3 text-[13px] font-mono">{job.jobId}</td>
                    <td className="py-3 text-[12px] text-text-muted">{job.startedAt}</td>
                    <td className="py-3 text-[12px]">{job.scope}</td>
                    <td className="py-3 text-[12px] font-mono">{job.ay}</td>
                    <td className="py-3 text-[13px] font-mono text-right">{job.total}</td>
                    <td className="py-3 text-[13px] font-mono text-right text-success">{job.success}</td>
                    <td className="py-3 text-[13px] font-mono text-right text-danger">{job.failed}</td>
                    <td className="py-3 text-[12px] text-text-muted">{job.duration}</td>
                    <td className="py-3">
                      <Badge label={job.status} variant={job.status === 'RUNNING' ? 'info' : job.status === 'COMPLETED' ? 'success' : 'danger'} />
                    </td>
                    <td className="py-3 text-right">
                      <div className="flex gap-1 justify-end">
                        {job.status === 'RUNNING' && <Button variant="danger" size="sm">Stop</Button>}
                        <Button variant="ghost" size="sm">Details</Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      </div>

      {/* Right: CAPTCHA solver status */}
      <Card>
        <div className="text-[14px] font-semibold mb-4">CAPTCHA Solver Status</div>
        <div className="space-y-3">
          <div className="p-3 bg-success-bg border border-success/20 rounded-lg">
            <div className="flex items-center justify-between mb-1">
              <span className="text-[13px] font-semibold text-success">Tesseract OCR</span>
              <Badge label="Active" variant="success" />
            </div>
            <div className="text-[11px] text-success">Primary solver · 94% success rate</div>
          </div>
          <div className="p-3 bg-info-bg border border-info/20 rounded-lg">
            <div className="flex items-center justify-between mb-1">
              <span className="text-[13px] font-semibold text-info">2Captcha API</span>
              <Badge label="Standby" variant="info" />
            </div>
            <div className="text-[11px] text-info">Fallback solver · 12 credits remaining</div>
          </div>
          <div className="p-3 bg-bg border border-border rounded-lg">
            <div className="text-[12px] font-semibold text-text-secondary mb-2">Retry Configuration</div>
            <div className="text-[11px] text-text-muted">Max retries: 3 per client</div>
            <div className="text-[11px] text-text-muted">Timeout: 45 seconds</div>
          </div>
        </div>
      </Card>
    </div>
  );
}
