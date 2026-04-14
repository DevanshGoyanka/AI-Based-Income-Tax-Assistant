import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { clientsApi } from '../api/clients';
import type { ClientFilters } from '../api/clients';
import type { Client } from '../api/types';
import Card from '../components/ui/Card';
import Badge from '../components/ui/Badge';
import Button from '../components/ui/Button';
import Modal from '../components/ui/Modal';
import Pagination from '../components/ui/Pagination';
import { useAppStore } from '../store/useAppStore';
import toast from 'react-hot-toast';

const FILING_BADGE: Record<string, 'success' | 'info' | 'warning' | 'danger' | 'gold'> = {
  'Filed': 'success',
  'Ready to File': 'gold',
  'Reconciliation': 'info',
  'Mismatch Review': 'danger',
  'Doc Pending': 'warning',
  'E-Verify Pending': 'warning',
};

export default function ClientMaster() {
  const { activeAY, setSelectedClientId } = useAppStore();
  const [filters, setFilters] = useState<ClientFilters>({ 
    page: 0, 
    size: 25, 
    assessmentYear: activeAY 
  });
  const [showAddModal, setShowAddModal] = useState(false);
  const [newClient, setNewClient] = useState<Partial<Client>>({
    name: '',
    pan: '',
    mobile: '',
    email: '',
    type: 'Individual',
  });
  const queryClient = useQueryClient();

  const { data, isLoading, error } = useQuery({
    queryKey: ['clients', filters],
    queryFn: () => clientsApi.list(filters),
    placeholderData: (prev) => prev,
  });

  // Handle API errors gracefully
  if (error) {
    console.error('Client list error:', error);
  }

  // Backend returns array, not paginated response
  const clients = Array.isArray(data) ? data : [];
  const totalElements = clients.length;

  const createClientMutation = useMutation({
    mutationFn: () => clientsApi.create(newClient),
    onSuccess: () => {
      toast.success('Client created successfully');
      setShowAddModal(false);
      setNewClient({ name: '', pan: '', mobile: '', email: '', type: 'Individual' });
      queryClient.invalidateQueries({ queryKey: ['clients'] });
      queryClient.invalidateQueries({ queryKey: ['clients-list'] });
    },
    onError: () => toast.error('Failed to create client'),
  });

  function updateFilter(key: keyof ClientFilters, value: any) {
    setFilters(prev => ({ ...prev, [key]: value, page: 0 }));
  }

  return (
    <div>
      {/* Filter bar */}
      <div className="flex items-center gap-2.5 mb-4 flex-wrap">
        <div className="relative">
          <svg className="absolute left-3 top-1/2 -translate-y-1/2 text-text-muted" width="14" height="14" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="2">
            <circle cx="9" cy="9" r="6"/>
            <path d="m15 15-3-3"/>
          </svg>
          <input
            className="pl-8 pr-3 py-2 bg-bg-card border border-border-strong rounded-lg text-[13px] outline-none focus:border-gold w-64 font-sans"
            placeholder="Search by name, PAN, mobile…"
            onChange={e => updateFilter('search', e.target.value)}
          />
        </div>
        <select 
          onChange={e => updateFilter('type', e.target.value)}
          className="bg-bg-card border border-border-strong rounded-lg px-3 py-2 text-[13px] text-text-primary outline-none focus:border-gold font-sans cursor-pointer"
        >
          <option value="">All Types</option>
          <option value="Individual">Individual</option>
          <option value="HUF">HUF</option>
          <option value="Firm">Firm</option>
          <option value="Company">Company</option>
        </select>
        <select 
          onChange={e => updateFilter('filingStatus', e.target.value)}
          className="bg-bg-card border border-border-strong rounded-lg px-3 py-2 text-[13px] text-text-primary outline-none focus:border-gold font-sans cursor-pointer"
        >
          <option value="">All Statuses</option>
          <option value="Filed">Filed</option>
          <option value="Ready to File">Ready to File</option>
          <option value="Reconciliation">Reconciliation</option>
          <option value="Mismatch Review">Mismatch Review</option>
          <option value="Doc Pending">Doc Pending</option>
        </select>
        <div className="ml-auto flex gap-2">
          <Button variant="secondary" size="sm">↓ Export Excel</Button>
          <Button variant="primary" size="sm" onClick={() => setShowAddModal(true)}>+ Add Client</Button>
        </div>
      </div>

      <Card>
        <div className="flex items-center justify-between p-5 px-[22px] pb-0 mb-0">
          <div>
            <div className="text-[14px] font-semibold text-text-primary">All Clients</div>
            <div className="text-[12px] text-text-muted mt-0.5">
              {totalElements > 0 ? `${totalElements.toLocaleString()} clients` : 'Loading…'}
            </div>
          </div>
          <div className="flex items-center gap-2">
            <Badge label="2,891 Filed" variant="success" size="sm" />
            <Badge label="518 Pending" variant="warning" size="sm" />
            <Badge label="31 Watch List" variant="neutral" size="sm" />
          </div>
        </div>

        <div className="overflow-x-auto mt-4">
          <table className="w-full border-collapse">
            <thead>
              <tr>
                {['', 'Client Name', 'PAN', 'Type', 'Mobile', 'ITR Form', 'AIS Sync', 'Filing Status', 'E-Verify', 'Watch', 'Actions'].map(h => (
                  <th key={h} className="text-left px-3.5 py-2.5 text-[11.5px] font-semibold text-text-muted uppercase tracking-[0.5px] bg-bg border-b border-border whitespace-nowrap">
                    {h === '' ? <input type="checkbox" className="cursor-pointer" /> : h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                Array.from({ length: 8 }).map((_, i) => (
                  <tr key={i} className="border-b border-border">
                    {Array.from({ length: 11 }).map((_, j) => (
                      <td key={j} className="px-3.5 py-3">
                        <div className="h-4 bg-border rounded animate-pulse" />
                      </td>
                    ))}
                  </tr>
                ))
              ) : (data?.content ?? []).map((client: Client) => (
                <tr
                  key={client.id}
                  onClick={() => setSelectedClientId(client.id)}
                  className="cursor-pointer hover:bg-[#FAFBFD] border-b border-border last:border-b-0"
                >
                  <td className="px-3.5 py-3">
                    <input type="checkbox" onClick={e => e.stopPropagation()} className="cursor-pointer" />
                  </td>
                  <td className="px-3.5 py-3">
                    <div className="flex items-center gap-2.5">
                      <div className="w-[30px] h-[30px] rounded-lg bg-navy flex items-center justify-center font-serif text-[12px] text-gold-light font-semibold flex-shrink-0">
                        {client.initials}
                      </div>
                      <div className="font-medium text-[13.5px]">{client.name}</div>
                    </div>
                  </td>
                  <td className="px-3.5 py-3 font-mono text-[12.5px] tracking-[0.3px]">{client.pan}</td>
                  <td className="px-3.5 py-3">
                    <Badge label={client.type} variant="neutral" size="sm" />
                  </td>
                  <td className="px-3.5 py-3 text-text-secondary text-[12.5px]">{client.mobile}</td>
                  <td className="px-3.5 py-3">
                    <Badge label={client.itrForm} variant="blue" size="sm" />
                  </td>
                  <td className="px-3.5 py-3 text-text-secondary text-[12.5px]">{client.lastSynced}</td>
                  <td className="px-3.5 py-3">
                    <Badge label={client.filingStatus} variant={FILING_BADGE[client.filingStatus] ?? 'neutral'} size="sm" />
                  </td>
                  <td className="px-3.5 py-3">
                    <Badge 
                      label={client.evStatus} 
                      variant={client.evStatus === 'Verified' ? 'success' : client.evStatus === 'Pending' ? 'warning' : 'neutral'} 
                      size="sm" 
                    />
                  </td>
                  <td className="px-3.5 py-3">
                    {client.watchList ? (
                      <Badge label="⚠ Watch" variant="danger" size="sm" />
                    ) : (
                      <span className="text-text-muted text-[13px]">—</span>
                    )}
                  </td>
                  <td className="px-3.5 py-3">
                    <div className="flex gap-1.5" onClick={e => e.stopPropagation()}>
                      <Button variant="ghost" size="sm">File</Button>
                      <Button variant="ghost" size="sm">Sync</Button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="px-[22px] pb-4">
          {totalElements > 0 && (
            <Pagination
              currentPage={(filters.page ?? 0) + 1}
              totalPages={Math.ceil(totalElements / (filters.size ?? 25))}
              totalItems={totalElements}
              pageSize={filters.size ?? 25}
              onPageChange={(p) => setFilters(prev => ({ ...prev, page: p - 1 }))}
            />
          )}
        </div>
      </Card>

      {/* Add Client Modal */}
      {showAddModal && (
        <Modal isOpen={showAddModal} title="Add New Client" onClose={() => setShowAddModal(false)}>
          <div className="space-y-4">
            <div>
              <label className="block text-[12px] font-semibold text-text-secondary mb-2">Full Name *</label>
              <input
                type="text"
                value={newClient.name}
                onChange={e => setNewClient(prev => ({ ...prev, name: e.target.value }))}
                className="w-full px-3 py-2 border border-border-strong rounded-lg text-[13px] outline-none focus:border-gold"
                placeholder="Rajesh Kumar"
              />
            </div>
            <div>
              <label className="block text-[12px] font-semibold text-text-secondary mb-2">PAN *</label>
              <input
                type="text"
                value={newClient.pan}
                onChange={e => setNewClient(prev => ({ ...prev, pan: e.target.value.toUpperCase() }))}
                className="w-full px-3 py-2 border border-border-strong rounded-lg text-[13px] font-mono outline-none focus:border-gold"
                placeholder="ABCDE1234F"
                maxLength={10}
              />
            </div>
            <div>
              <label className="block text-[12px] font-semibold text-text-secondary mb-2">Mobile *</label>
              <input
                type="tel"
                value={newClient.mobile}
                onChange={e => setNewClient(prev => ({ ...prev, mobile: e.target.value }))}
                className="w-full px-3 py-2 border border-border-strong rounded-lg text-[13px] outline-none focus:border-gold"
                placeholder="+91 98765 43210"
              />
            </div>
            <div>
              <label className="block text-[12px] font-semibold text-text-secondary mb-2">Email</label>
              <input
                type="email"
                value={newClient.email}
                onChange={e => setNewClient(prev => ({ ...prev, email: e.target.value }))}
                className="w-full px-3 py-2 border border-border-strong rounded-lg text-[13px] outline-none focus:border-gold"
                placeholder="rajesh@example.com"
              />
            </div>
            <div>
              <label className="block text-[12px] font-semibold text-text-secondary mb-2">Type *</label>
              <select
                value={newClient.type}
                onChange={e => setNewClient(prev => ({ ...prev, type: e.target.value as Client['type'] }))}
                className="w-full px-3 py-2 border border-border-strong rounded-lg text-[13px] outline-none focus:border-gold"
              >
                <option value="Individual">Individual</option>
                <option value="HUF">HUF</option>
                <option value="Firm">Firm</option>
                <option value="Company">Company</option>
              </select>
            </div>
            <div className="flex gap-2 pt-2">
              <Button
                variant="primary"
                className="flex-1"
                onClick={() => createClientMutation.mutate()}
                disabled={!newClient.name || !newClient.pan || !newClient.mobile}
              >
                Create Client
              </Button>
              <Button variant="ghost" onClick={() => setShowAddModal(false)}>Cancel</Button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}
