import Badge from '../ui/Badge';
import type { Notice } from '../../api/types';

interface NoticeCardProps {
  notice: Notice;
  onClick?: () => void;
}

export default function NoticeCard({ notice, onClick }: NoticeCardProps) {
  const priorityColors = {
    Urgent: 'danger' as const,
    High: 'warning' as const,
    Normal: 'neutral' as const,
  };

  return (
    <div
      onClick={onClick}
      className="border border-border rounded-lg p-4 hover:border-border-strong transition-colors cursor-pointer"
    >
      <div className="flex items-start justify-between mb-2">
        <div>
          <div className="text-[14px] font-semibold text-text-primary">{notice.clientName}</div>
          <div className="text-[12px] text-text-muted font-mono">{notice.pan}</div>
        </div>
        <Badge label={notice.priority} variant={priorityColors[notice.priority]} />
      </div>
      <div className="text-[13px] text-text-primary mb-2">Section {notice.section}</div>
      <div className="flex items-center justify-between text-[12px] text-text-muted">
        <span>Due: {notice.dueDate}</span>
        {notice.amount && <span className="font-mono">₹{notice.amount.toLocaleString('en-IN')}</span>}
      </div>
    </div>
  );
}
