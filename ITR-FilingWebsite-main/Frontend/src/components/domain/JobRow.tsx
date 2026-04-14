import Badge from '../ui/Badge';
import ProgressBar from '../ui/ProgressBar';
import type { BatchJob } from '../../api/types';

interface JobRowProps {
  job: BatchJob;
}

export default function JobRow({ job }: JobRowProps) {
  const statusColors = {
    RUNNING: 'blue' as const,
    COMPLETED: 'success' as const,
    FAILED: 'danger' as const,
    QUEUED: 'neutral' as const,
    PARTIAL: 'warning' as const,
  };

  return (
    <div className="border border-border rounded-lg p-4 hover:border-border-strong transition-colors">
      <div className="flex items-start justify-between mb-3">
        <div>
          <div className="text-[14px] font-semibold text-text-primary">{job.jobName}</div>
          <div className="text-[12px] text-text-muted mt-0.5">
            {job.scope} · AY {job.assessmentYear}
          </div>
        </div>
        <Badge label={job.status} variant={statusColors[job.status]} />
      </div>
      <ProgressBar progress={job.progressPct} showPercentage />
      <div className="flex items-center justify-between mt-3 text-[12px] text-text-muted">
        <span>{job.success} success · {job.failed} failed</span>
        {job.duration && <span>{job.duration}</span>}
      </div>
    </div>
  );
}
