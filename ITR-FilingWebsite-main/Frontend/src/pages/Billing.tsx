import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { billingApi } from '../api/billing';
import Card from '../components/ui/Card';
import Badge from '../components/ui/Badge';
import Button from '../components/ui/Button';
import StatCard from '../components/ui/StatCard';

const STATUS_VARIANT: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
  Paid: 'success',
  Pending: 'warning',
  Overdue: 'danger',
  Partial: 'info',
};

export default function Billing() {
  const [search, setSearch] = useState('');

  const { data: stats } = useQuery({ queryKey: ['billing-stats'], queryFn: billingApi.getStats });
  const { data: invoices } = useQuery({ queryKey: ['invoices'], queryFn: () => billingApi.list() });

  const MOCK_INVOICES = [
    { id: 'INV-2001', clientName: 'Rajesh Kumar', pan: 'ABCDE1234F', service: 'ITR-2 Filing', amount: 3500, status: 'Paid', createdAt: 'Apr 8' },
    { id: 'INV-2002', clientName: 'Priya Sharma', pan: 'XYZAB5678C', service: 'ITR-1 Filing', amount: 2500, status: 'Pending', createdAt: 'Apr 9' },
    { id: 'INV-2003', clientName: 'Amit Patel', pan: 'PQRST9012G', service: 'Consultation', amount: 1500, status: 'Overdue', createdAt: 'Mar 31' },
  ];

  return (
    <div>
      <div className="grid grid-cols-4 gap-3.5 mb-5">
        <StatCard accent="teal" label="Total Billed" value="₹12.6L" meta="This financial year" />
        <StatCard accent="success" label="Collected" value="₹9.4L" meta="74.5% collection rate" />
        <StatCard accent="warning" label="Outstanding" value="₹3.2L" meta="From 142 invoices" />
        <StatCard accent="blue" label="Total Invoices" value={stats?.total ?? 487} />
      </div>

      <div className="flex items-center gap-2.5 mb-4 flex-wrap">
        <div className="relative">
          <svg className="absolute left-3 top-1/2 -translate-y-1/2 text-text-muted" width="14" height="14" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="2">
            <circle cx="9" cy="9" r="6"/><path d="m15 15-3-3"/>
          </svg>
          <input
            value={search}
            onChange={e => setSearch(e.target.value)}
            className="pl-8 pr-3 py-2 bg-bg-card border border-border-strong rounded-lg text-[13px] outline-none focus:border-gold w-64"
            placeholder="Search by client, invoice ID…"
          />
        </div>
        <select className="px-3 py-2 border border-border-strong rounded-lg text-[13px] outline-none focus:border-gold">
          <option>All Statuses</option>
          <option>Paid</option>
          <option>Pending</option>
          <option>Overdue</option>
        </select>
        <div className="ml-auto"><Button variant="primary">+ New Invoice</Button></div>
      </div>

      <Card>
        <table className="w-full">
          <thead>
            <tr className="border-b border-border">
              {['Invoice ID', 'Client', 'Service', 'Amount', 'Status', 'Date', 'Actions'].map(h => (
                <th key={h} className="text-left py-3 px-4 text-[11px] font-semibold text-text-muted uppercase bg-bg">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {(invoices ?? MOCK_INVOICES).map((inv: any) => (
              <tr key={inv.id} className="border-b border-border hover:bg-bg cursor-pointer">
                <td className="py-3 px-4 font-mono text-[12.5px]">{inv.id}</td>
                <td className="py-3 px-4">
                  <div className="text-[13px] font-medium">{inv.clientName}</div>
                  <div className="text-[11px] text-text-muted font-mono">{inv.pan}</div>
                </td>
                <td className="py-3 px-4 text-[13px]">{inv.service}</td>
                <td className="py-3 px-4 font-serif text-[15px] font-semibold">₹{inv.amount.toLocaleString('en-IN')}</td>
                <td className="py-3 px-4"><Badge label={inv.status} variant={STATUS_VARIANT[inv.status]} /></td>
                <td className="py-3 px-4 text-[12px] text-text-muted">{inv.createdAt}</td>
                <td className="py-3 px-4">
                  <div className="flex gap-1">
                    <Button variant="ghost" size="sm">View</Button>
                    {inv.status === 'Pending' && <Button variant="ghost" size="sm">Remind</Button>}
                    <Button variant="ghost" size="sm">↓</Button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>
    </div>
  );
}
