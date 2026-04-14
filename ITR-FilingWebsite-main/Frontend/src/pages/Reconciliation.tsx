import { useState } from 'react';
import { useQuery, useMutation } from '@tanstack/react-query';
import { reconciliationApi } from '../api/reconciliation';
import Card from '../components/ui/Card';
import Badge from '../components/ui/Badge';
import Button from '../components/ui/Button';
import ProgressBar from '../components/ui/ProgressBar';
import StatCard from '../components/ui/StatCard';
import toast from 'react-hot-toast';

export default function Reconciliation() {
  const [selectedMismatch, setSelectedMismatch] = useState<any>(null);

  const { data: stats } = useQuery({
    queryKey: ['recon-stats'],
    queryFn: reconciliationApi.getStats,
  });

  const { data: mismatches } = useQuery({
    queryKey: ['mismatches'],
    queryFn: reconciliationApi.listMismatches,
  });

  const resolveMutation = useMutation({
    mutationFn: (id: number) => reconciliationApi.resolve(id),
    onSuccess: () => {
      toast.success('Mismatch resolved');
      setSelectedMismatch(null);
    },
  });

  return (
    <div>
      <div className="grid grid-cols-3 gap-3.5 mb-5">
        <StatCard accent="danger" label="Total Mismatches" value={stats?.total ?? 218} />
        <StatCard accent="warning" label="Under Review" value={stats?.underReview ?? 18} />
        <StatCard accent="success" label="Resolved" value={stats?.resolved ?? 34} />
      </div>

      <div className="grid grid-cols-[1fr_400px] gap-4">
        {/* Left: Mismatch table */}
        <Card>
          <div className="text-[14px] font-semibold mb-4">Mismatches Detected</div>
          <div className="space-y-2">
            {(mismatches ?? [
              { id: 1, clientName: 'Rajesh Kumar', pan: 'ABCDE1234F', type: 'TDS', difference: 12500, status: 'Pending' },
              { id: 2, clientName: 'Priya Sharma', pan: 'XYZAB5678C', type: '26AS', difference: -8400, status: 'Under Review' },
            ]).map((m: any) => (
              <div
                key={m.id}
                onClick={() => setSelectedMismatch(m)}
                className={`p-4 border rounded-lg cursor-pointer transition-all ${
                  selectedMismatch?.id === m.id ? 'border-gold bg-gold-pale' : 'border-border hover:bg-bg'
                }`}
              >
                <div className="flex items-center justify-between">
                  <div>
                    <div className="text-[13.5px] font-semibold">{m.clientName}</div>
                    <div className="text-[12px] text-text-muted font-mono">{m.pan}</div>
                  </div>
                  <div className="flex items-center gap-3">
                    <Badge label={m.type} variant={m.type === 'TDS' ? 'info' : 'warning'} />
                    <div className={`text-[15px] font-mono font-semibold ${m.difference > 0 ? 'text-danger' : 'text-success'}`}>
                      {m.difference > 0 ? '+' : ''}₹{Math.abs(m.difference).toLocaleString('en-IN')}
                    </div>
                    <Badge label={m.status} variant={m.status === 'Pending' ? 'warning' : 'info'} />
                  </div>
                </div>
              </div>
            ))}
          </div>
        </Card>

        {/* Right: Detail panel */}
        {selectedMismatch ? (
          <Card>
            <div className="text-[14px] font-semibold mb-4">Mismatch Details</div>
            
            <div className="space-y-4 mb-5">
              <div>
                <div className="text-[11px] text-text-muted uppercase mb-1">26AS Amount</div>
                <div className="text-[16px] font-mono font-semibold">₹45,000</div>
                <ProgressBar progress={100} color="blue" />
              </div>
              <div>
                <div className="text-[11px] text-text-muted uppercase mb-1">AIS Amount</div>
                <div className="text-[16px] font-mono font-semibold">₹42,500</div>
                <ProgressBar progress={94} color="teal" />
              </div>
              <div>
                <div className="text-[11px] text-text-muted uppercase mb-1">Computation Amount</div>
                <div className="text-[16px] font-mono font-semibold">₹32,500</div>
                <ProgressBar progress={72} color="gold" />
              </div>
            </div>

            <div className="p-3 bg-danger-bg border border-danger/20 rounded-lg mb-4">
              <div className="text-[12px] font-semibold text-danger mb-1">Mismatch Summary</div>
              <div className="text-[11.5px] text-danger">
                TDS claimed (₹32,500) is ₹12,500 less than 26AS (₹45,000). Possible missing deductor entry.
              </div>
            </div>

            <div className="mb-4">
              <label className="block text-[12px] font-semibold text-text-secondary mb-2">Advocate Notes</label>
              <textarea
                className="w-full px-3 py-2 border border-border-strong rounded-lg text-[13px] outline-none focus:border-gold resize-none"
                rows={4}
                placeholder="Add resolution notes..."
              />
            </div>

            <div className="flex gap-2">
              <Button variant="primary" className="flex-1" onClick={() => resolveMutation.mutate(selectedMismatch.id)}>
                Mark Resolved
              </Button>
              <Button variant="danger" className="flex-1">Escalate</Button>
            </div>
            <Button variant="ghost" className="w-full mt-2">Download Report</Button>
          </Card>
        ) : (
          <Card>
            <div className="text-center py-12 text-text-muted">
              Select a mismatch to view details
            </div>
          </Card>
        )}
      </div>
    </div>
  );
}
