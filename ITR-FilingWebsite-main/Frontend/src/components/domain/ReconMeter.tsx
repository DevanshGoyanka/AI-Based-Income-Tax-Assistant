import ProgressBar from '../ui/ProgressBar';

interface ReconMeterProps {
  total: number;
  clean: number;
  mismatches: number;
  pending: number;
}

export default function ReconMeter({ total, clean, mismatches, pending }: ReconMeterProps) {
  const cleanPct = (clean / total) * 100;
  const mismatchPct = (mismatches / total) * 100;
  const pendingPct = (pending / total) * 100;

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <span className="text-[13px] font-medium text-text-primary">Reconciliation Status</span>
        <span className="text-[12px] text-text-muted">{total} clients</span>
      </div>
      <div className="space-y-3">
        <ProgressBar progress={cleanPct} label={`Clean (${clean})`} color="success" />
        <ProgressBar progress={mismatchPct} label={`Mismatches (${mismatches})`} color="gold" />
        <ProgressBar progress={pendingPct} label={`Pending (${pending})`} color="blue" />
      </div>
    </div>
  );
}
