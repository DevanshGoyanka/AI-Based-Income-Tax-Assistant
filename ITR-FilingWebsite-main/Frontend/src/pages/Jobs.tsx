import { useQuery } from '@tanstack/react-query';
import { jobsApi } from '../api/jobs';
import Card from '../components/ui/Card';
import Badge from '../components/ui/Badge';
import Button from '../components/ui/Button';
import ProgressBar from '../components/ui/ProgressBar';
import StatCard from '../components/ui/StatCard';

export default function Jobs() {
  const { data: stats } = useQuery({
    queryKey: ['jobs-stats'],
    queryFn: jobsApi.getStats,
    refetchInterval: 5000,
  });

  const { data: jobs } = useQuery({
    queryKey: ['jobs-list'],
    queryFn: () => jobsApi.list({}),
    refetchInterval: 5000,
  });

  return (
    <div>
      <div className="grid grid-cols-4 gap-3.5 mb-5">
        <StatCard accent="blue" label="Running" value={stats?.running ?? 2} />
        <StatCard accent="warning" label="Queued" value={stats?.queued ?? 1} />
        <StatCard accent="success" label="Completed Today" value={stats?.completedToday ?? 47} />
        <StatCard accent="danger" label="Failed" value={stats?.failed ?? 3} />
      </div>

      <Card>
        <div className="text-[14px] font-semibold mb-4">Background Jobs</div>
        <div className="space-y-3">
          {(jobs?.content ?? [
            { jobId: 847, jobName: 'Annual Bulk Sync — Batch 6/25 (AY 2025–26)', detail: '200 clients · 148 done · 52 pending', progressPct: 74, status: 'RUNNING' },
            { jobId: 848, jobName: 'WhatsApp Advance Tax Reminder Broadcast', detail: 'Sending to 48 clients · 31 delivered · 17 queued', progressPct: 65, status: 'RUNNING' },
            { jobId: 849, jobName: 'Notice Deadline Alert — 7 clients', detail: 'Queued for 11:00 PM', progressPct: 0, status: 'QUEUED' },
          ]).map((job: any) => (
            <div key={job.jobId} className="flex items-center gap-3.5 p-4 border border-border rounded-lg">
              <div className={`w-2.5 h-2.5 rounded-full flex-shrink-0 ${
                job.status === 'RUNNING' ? 'bg-accent-blue animate-pulse-dot' : 
                job.status === 'COMPLETED' ? 'bg-success' : 
                job.status === 'QUEUED' ? 'bg-warning' : 'bg-danger'
              }`} />
              <div className="flex-1">
                <div className="text-[13.5px] font-medium">{job.jobName}</div>
                <div className="text-[12px] text-text-muted mt-0.5">{job.detail}</div>
              </div>
              <div className="w-[120px]">
                <ProgressBar progress={job.progressPct} showPercentage={true} />
              </div>
              <Badge label={job.status} variant={
                job.status === 'RUNNING' ? 'info' : 
                job.status === 'COMPLETED' ? 'success' : 
                job.status === 'QUEUED' ? 'warning' : 'danger'
              } />
              <div className="flex gap-1">
                {job.status === 'RUNNING' && <Button variant="danger" size="sm">Stop</Button>}
                {job.status === 'QUEUED' && <Button variant="ghost" size="sm">Cancel</Button>}
                <Button variant="ghost" size="sm">Details</Button>
              </div>
            </div>
          ))}
        </div>
      </Card>
    </div>
  );
}
