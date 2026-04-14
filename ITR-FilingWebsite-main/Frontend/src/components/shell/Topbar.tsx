import { useAppStore } from '../../store/useAppStore';
import type { AssessmentYear } from '../../api/types';

const PAGE_META: Record<string, { title: string; sub: string }> = {
  dashboard: { title: 'Dashboard', sub: 'AY 2025–26 & 2026–27 · 5,000 Active Clients' },
  clients: { title: 'Client Master', sub: '5,000 clients · AY 2025–26' },
  filing: { title: 'ITR Filing', sub: '342 in progress · AY 2025–26' },
  reconciliation: { title: 'Reconciliation', sub: '218 mismatches detected · 18 under review' },
  sync: { title: 'ITD Portal Sync', sub: 'Playwright automation · AIS / TIS / 26AS' },
  jobs: { title: 'Background Jobs', sub: '2 running · 1 queued · Spring Batch engine' },
  notices: { title: 'Notice Management', sub: '31 active notices · 7 due this week' },
  calendar: { title: 'Compliance Calendar', sub: 'April 2026 · Key deadlines & reminders' },
  tasks: { title: 'Tasks & Work Queue', sub: '24 open tasks · 8 pending, 11 in progress' },
  billing: { title: 'Billing & Fees', sub: 'Invoice management · ₹12.5L pending' },
  accounting: { title: 'Firm Accounting', sub: 'Practice financial management' },
  reports: { title: 'Reports & Analytics', sub: 'Business intelligence & insights' },
  communication: { title: 'Communication Hub', sub: 'WhatsApp · Email · SMS' },
};

export default function Topbar() {
  const { activeView, activeAY, setActiveAY } = useAppStore();
  const meta = PAGE_META[activeView] ?? { title: activeView, sub: '' };

  return (
    <header className="h-[62px] bg-bg-card border-b border-border flex items-center px-7 gap-4 flex-shrink-0 z-50">
      <div className="flex-1">
        <h1 className="font-serif text-[20px] font-semibold text-text-primary leading-[1.2]">
          {meta.title}
        </h1>
        <p className="text-[12px] text-text-muted mt-px">{meta.sub}</p>
      </div>

      <div className="flex items-center gap-2.5">
        <select
          value={activeAY}
          onChange={(e) => setActiveAY(e.target.value as AssessmentYear)}
          className="bg-bg border border-border-strong rounded-[7px] px-3 py-[7px] text-[13px] font-medium text-text-primary cursor-pointer outline-none font-sans"
        >
          <option value="2025-26">AY 2025–26</option>
          <option value="2026-27">AY 2026–27</option>
        </select>

        <button className="w-9 h-9 bg-bg border border-border rounded-lg flex items-center justify-center text-text-secondary hover:bg-border hover:text-text-primary transition-all relative">
          <svg width="16" height="16" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.8"><path d="M10 2a6 6 0 0 1 6 6v2l1.5 3h-15L4 10V8a6 6 0 0 1 6-6z"/><path d="M8 15a2 2 0 0 0 4 0"/></svg>
          <span className="absolute top-1.5 right-1.5 w-[7px] h-[7px] bg-[#EF4444] rounded-full border-[1.5px] border-white" />
        </button>
      </div>
    </header>
  );
}
