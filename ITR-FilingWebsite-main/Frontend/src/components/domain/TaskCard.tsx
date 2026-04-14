import Badge from '../ui/Badge';

interface TaskCardProps {
  title: string;
  description: string;
  assignee: string;
  dueDate: string;
  priority: 'High' | 'Medium' | 'Low';
  status: 'Pending' | 'In Progress' | 'Completed';
}

export default function TaskCard({ title, description, assignee, dueDate, priority, status }: TaskCardProps) {
  const priorityColors = {
    High: 'danger' as const,
    Medium: 'warning' as const,
    Low: 'neutral' as const,
  };

  const statusColors = {
    Pending: 'neutral' as const,
    'In Progress': 'blue' as const,
    Completed: 'success' as const,
  };

  return (
    <div className="border border-border rounded-lg p-4 hover:border-border-strong transition-colors">
      <div className="flex items-start justify-between mb-2">
        <div className="flex-1">
          <div className="text-[14px] font-semibold text-text-primary">{title}</div>
          <div className="text-[12px] text-text-muted mt-1">{description}</div>
        </div>
        <Badge label={status} variant={statusColors[status]} />
      </div>
      <div className="flex items-center justify-between mt-3">
        <div className="flex items-center gap-2">
          <Badge label={priority} variant={priorityColors[priority]} size="sm" />
          <span className="text-[12px] text-text-muted">{assignee}</span>
        </div>
        <span className="text-[11px] text-text-muted">Due: {dueDate}</span>
      </div>
    </div>
  );
}
