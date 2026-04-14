import Badge from '../ui/Badge';
import type { Invoice } from '../../api/types';

interface InvoiceRowProps {
  invoice: Invoice;
  onClick?: () => void;
}

export default function InvoiceRow({ invoice, onClick }: InvoiceRowProps) {
  const statusColors = {
    Paid: 'success' as const,
    Pending: 'warning' as const,
    Overdue: 'danger' as const,
    Partial: 'blue' as const,
  };

  return (
    <div
      onClick={onClick}
      className="flex items-center justify-between py-3 border-b border-border last:border-0 hover:bg-bg transition-colors cursor-pointer"
    >
      <div className="flex-1">
        <div className="text-[13px] font-medium text-text-primary">{invoice.clientName}</div>
        <div className="text-[12px] text-text-muted mt-0.5">
          {invoice.service} · {invoice.createdAt}
        </div>
      </div>
      <div className="flex items-center gap-4">
        <div className="text-[14px] font-semibold text-text-primary font-mono">
          ₹{invoice.amount.toLocaleString('en-IN')}
        </div>
        <Badge label={invoice.status} variant={statusColors[invoice.status]} />
      </div>
    </div>
  );
}
