import Badge from '../ui/Badge';

interface FilingPipelineProps {
  stats: {
    docPending: number;
    reconciliation: number;
    mismatchReview: number;
    readyToFile: number;
    filed: number;
    eVerifyPending: number;
  };
}

export default function FilingPipeline({ stats }: FilingPipelineProps) {
  const stages = [
    { label: 'Doc Pending', count: stats.docPending, color: 'neutral' as const },
    { label: 'Reconciliation', count: stats.reconciliation, color: 'blue' as const },
    { label: 'Mismatch Review', count: stats.mismatchReview, color: 'warning' as const },
    { label: 'Ready to File', count: stats.readyToFile, color: 'success' as const },
    { label: 'Filed', count: stats.filed, color: 'teal' as const },
    { label: 'E-Verify Pending', count: stats.eVerifyPending, color: 'rose' as const },
  ];

  return (
    <div className="flex gap-2 overflow-x-auto pb-2">
      {stages.map((stage) => (
        <div
          key={stage.label}
          className="flex-1 min-w-[140px] bg-bg-card border border-border rounded-lg p-3 text-center"
        >
          <div className="text-[11px] text-text-muted mb-2">{stage.label}</div>
          <div className="font-serif text-[24px] font-semibold text-text-primary mb-2">{stage.count}</div>
          <Badge label={stage.label} variant={stage.color} size="sm" />
        </div>
      ))}
    </div>
  );
}
