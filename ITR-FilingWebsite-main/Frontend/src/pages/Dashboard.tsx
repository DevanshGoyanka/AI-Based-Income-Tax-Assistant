import { useQuery } from '@tanstack/react-query';
import { clientsApi } from '../api/clients';
import StatCard from '../components/ui/StatCard';
import Card from '../components/ui/Card';
import Button from '../components/ui/Button';
import ProgressBar from '../components/ui/ProgressBar';
import ActivityItem from '../components/ui/ActivityItem';
import { useAppStore } from '../store/useAppStore';

export default function Dashboard() {
  const { setActiveView } = useAppStore();

  const { data: stats } = useQuery({
    queryKey: ['dashboard-stats'],
    queryFn: clientsApi.getDashboardStats,
  });

  return (
    <div>
      {/* Stats Grid */}
      <div className="grid grid-cols-4 gap-3.5 mb-5">
        <StatCard 
          accent="gold" 
          label="Total Active Clients" 
          value={stats?.total?.toLocaleString() ?? '5,000'} 
          meta="AY 2025–26" 
        />
        <StatCard 
          accent="teal" 
          label="Returns Filed" 
          value={stats?.filed?.toLocaleString() ?? '2,891'} 
          meta={<span className="text-success font-medium">↑ 18% vs last year</span>} 
        />
        <StatCard 
          accent="blue" 
          label="In Progress" 
          value={stats?.inProgress?.toLocaleString() ?? '342'} 
          meta="Computation + Reconciliation" 
        />
        <StatCard 
          accent="rose" 
          label="Active Notices" 
          value={stats?.totalNotices?.toLocaleString() ?? '31'} 
          meta="7 response due this week" 
        />
      </div>

      {/* 2-column grid */}
      <div className="grid grid-cols-[1fr_340px] gap-4 mb-5">
        {/* Filing Progress Donut */}
        <Card>
          <div className="flex items-center justify-between mb-[18px]">
            <div>
              <div className="text-[14px] font-semibold text-text-primary">AY 2025–26 Filing Progress</div>
              <div className="text-[12px] text-text-muted mt-0.5">5,000 clients · As of today</div>
            </div>
            <Button variant="ghost" size="sm" onClick={() => setActiveView('filing')}>
              View Filing Queue →
            </Button>
          </div>
          <div className="flex items-center gap-6">
            {/* Donut Chart */}
            <div 
              className="relative w-[120px] h-[120px] rounded-full flex-shrink-0"
              style={{ 
                background: 'conic-gradient(#15803D 0deg 172deg, #C9943A 172deg 230deg, #1D6FA4 230deg 280deg, #EF4444 280deg 310deg, #C8D3E0 310deg 360deg)' 
              }}
            >
              <div className="absolute inset-0 flex items-center justify-center">
                <div className="w-[72px] h-[72px] bg-bg-card rounded-full flex items-center justify-center text-center">
                  <div>
                    <div className="font-serif text-[18px] font-semibold leading-none">58%</div>
                    <div className="text-[10px] text-text-muted">Filed</div>
                  </div>
                </div>
              </div>
            </div>
            {/* Legend */}
            <div className="flex flex-col gap-2 flex-1">
              {[
                { label: 'Filed', value: '2,891', color: 'bg-success' },
                { label: 'Ready to File', value: '342', color: 'bg-gold' },
                { label: 'Reconciliation', value: '284', color: 'bg-accent-blue' },
                { label: 'Mismatch Review', value: '218', color: 'bg-[#EF4444]' },
                { label: 'Doc Pending', value: '265', color: 'bg-border-strong' },
              ].map(item => (
                <div key={item.label} className="flex items-center gap-2 text-[12.5px]">
                  <div className={`w-2.5 h-2.5 rounded-full flex-shrink-0 ${item.color}`} />
                  <span className="text-text-secondary flex-1">{item.label}</span>
                  <span className="font-semibold text-text-primary">{item.value}</span>
                </div>
              ))}
            </div>
          </div>
        </Card>

        {/* Deadlines */}
        <Card>
          <div className="flex items-center justify-between mb-[18px]">
            <div className="text-[14px] font-semibold text-text-primary">⚠ Upcoming Deadlines</div>
          </div>
          <div className="flex flex-col">
            {[
              { label: 'Notice 143(1)(a)', sub: 'Priya Nair · Respond by Today 5 PM', dot: 'red' },
              { label: 'Advance Tax Q4', sub: 'Mar 15 · Payment deadline', dot: 'red' },
              { label: 'E-Verify Expiry', sub: '3 clients · 120-day window closing', dot: 'gold' },
            ].map(item => (
              <div key={item.label} className="flex gap-3 py-3 border-b border-border last:border-b-0 items-start">
                <div className={`w-2 h-2 rounded-full mt-[5px] flex-shrink-0 ${item.dot === 'red' ? 'bg-[#EF4444]' : 'bg-gold'}`} />
                <div className="flex-1">
                  <div className="text-[13px] font-semibold">{item.label}</div>
                  <div className="text-[11.5px] text-text-muted mt-0.5">{item.sub}</div>
                </div>
              </div>
            ))}
          </div>
        </Card>
      </div>

      {/* Background Jobs */}
      <Card className="mb-5">
        <div className="flex items-center justify-between mb-[18px]">
          <div className="text-[14px] font-semibold text-text-primary">Background Jobs Status</div>
          <Button variant="ghost" size="sm" onClick={() => setActiveView('jobs')}>
            View all jobs →
          </Button>
        </div>
        {[
          { jobId: 847, jobName: 'Annual Bulk Sync — Batch 6/25 (AY 2025–26)', jobDetail: '200 clients · 148 done · 52 pending', progressPct: 74, status: 'RUNNING' },
          { jobId: 848, jobName: 'WhatsApp Advance Tax Reminder Broadcast', jobDetail: 'Sending to 48 clients · 31 delivered · 17 queued', progressPct: 65, status: 'RUNNING' },
        ].map((job: any) => (
          <div key={job.jobId} className="flex items-center gap-3.5 py-3.5 border-b border-border last:border-b-0">
            <div className={`w-2.5 h-2.5 rounded-full flex-shrink-0 ${job.status === 'RUNNING' ? 'bg-accent-blue animate-pulse-dot' : 'bg-success'}`} />
            <div className="flex-1">
              <div className="text-[13.5px] font-medium">{job.jobName}</div>
              <div className="text-[12px] text-text-muted mt-0.5">{job.jobDetail}</div>
            </div>
            <div className="w-[120px]">
              <div className="text-[12px] font-medium text-text-secondary text-right mb-1">{job.progressPct}%</div>
              <ProgressBar progress={job.progressPct} color="blue" />
            </div>
            <Button variant="secondary" size="sm">Stop</Button>
          </div>
        ))}
      </Card>

      {/* Recent Activity */}
      <Card>
        <div className="flex items-center justify-between mb-[18px]">
          <div className="text-[14px] font-semibold text-text-primary">Recent Activity</div>
        </div>
        <div>
          <ActivityItem
            icon={<svg width="14" height="14" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="2"><path d="M14 2H6a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2V4a2 2 0 0 0-2-2z"/></svg>}
            title="ITR-2 Filed Successfully"
            description="Ramesh Verma · DEFPV1122K · AY 2025-26"
            time="2 min ago"
            color="teal"
          />
          <ActivityItem
            icon={<svg width="14" height="14" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="10" cy="10" r="8"/><path d="M10 6v4l2 2"/></svg>}
            title="AIS Sync Completed"
            description="Batch 12/25 · 200 clients synced"
            time="15 min ago"
            color="blue"
          />
          <ActivityItem
            icon={<svg width="14" height="14" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="2"><path d="M10 2a6 6 0 0 1 6 6v2l1.5 3h-15L4 10V8a6 6 0 0 1 6-6z"/></svg>}
            title="New Notice Received"
            description="Priya Nair · Section 143(1)(a)"
            time="1 hour ago"
            color="rose"
          />
        </div>
      </Card>
    </div>
  );
}
