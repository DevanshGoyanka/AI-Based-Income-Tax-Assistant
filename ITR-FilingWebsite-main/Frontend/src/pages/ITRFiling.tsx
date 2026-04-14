import { useState } from 'react';
import { useQuery, useMutation } from '@tanstack/react-query';
import { clientsApi } from '../api/clients';
import { filingApi } from '../api/filing';
import Card from '../components/ui/Card';
import Badge from '../components/ui/Badge';
import Button from '../components/ui/Button';
import Modal from '../components/ui/Modal';
import { useAppStore } from '../store/useAppStore';
import toast from 'react-hot-toast';

const PIPELINE_STAGES = [
  { n: '✓', label: 'Docs Downloaded', state: 'done' },
  { n: '✓', label: 'AIS Reconciled', state: 'done' },
  { n: '3', label: 'Computation Review', state: 'current' },
  { n: '4', label: 'Ready to File', state: 'pending' },
  { n: '5', label: 'Filed via ITD API', state: 'pending' },
  { n: '6', label: 'E-Verification', state: 'pending' },
  { n: '7', label: 'Computation Sent', state: 'pending' },
];

export default function ITRFiling() {
  const { activeAY, openComputation } = useAppStore();
  const [_stage, setStage] = useState('');
  const [_form, setForm] = useState('');
  const [showNewFilingModal, setShowNewFilingModal] = useState(false);
  const [selectedClientId, setSelectedClientId] = useState<number | null>(null);
  const [selectedITRForm, setSelectedITRForm] = useState('ITR-1');

  const { data: clients } = useQuery({
    queryKey: ['clients-list'],
    queryFn: () => clientsApi.list({ size: 1000 }),
    enabled: showNewFilingModal,
  });

  const { data: filings } = useQuery({
    queryKey: ['filings', activeAY],
    queryFn: () => filingApi.list({ assessmentYear: activeAY }),
  });

  const clientsList = Array.isArray(clients) ? clients : [];
  const filingsList = Array.isArray(filings) ? filings : [];

  const createFilingMutation = useMutation({
    mutationFn: () => {
      if (!selectedClientId) throw new Error('No client selected');
      // Just open computation window - backend will load/create data on demand
      return Promise.resolve({ success: true });
    },
    onSuccess: () => {
      toast.success('Opening computation window');
      setShowNewFilingModal(false);
      if (selectedClientId) openComputation(selectedClientId);
    },
    onError: () => toast.error('Failed to open computation'),
  });

  return (
    <div>
      {/* AY tabs */}
      <div className="flex gap-1.5 mb-5">
        {['AY 2025–26', 'AY 2026–27'].map((ay) => (
          <button 
            key={ay} 
            className={`px-3.5 py-1.5 rounded-[20px] text-[12.5px] font-medium cursor-pointer border transition-all ${
              activeAY === ay.slice(3) 
                ? 'bg-navy text-gold-light border-navy' 
                : 'bg-bg-card border-border-strong text-text-secondary hover:border-gold hover:text-gold'
            }`}
          >
            {ay}
          </button>
        ))}
      </div>

      {/* Filing pipeline visual */}
      <Card className="mb-5">
        <div className="flex items-center justify-between mb-4">
          <div className="text-[14px] font-semibold">Filing Workflow Pipeline</div>
          <Badge label="342 In Progress" variant="gold" />
        </div>
        <div className="flex items-center overflow-x-auto">
          {PIPELINE_STAGES.map((stage, i) => (
            <div key={i} className="flex items-center flex-shrink-0">
              <div className="flex flex-col items-center">
                <div className={`w-11 h-11 rounded-full flex items-center justify-center text-base border-2 font-semibold relative z-[1]
                  ${stage.state === 'done' ? 'bg-success-bg border-success text-success'
                  : stage.state === 'current' ? 'bg-gold-pale border-gold text-[#92640A]'
                  : 'bg-bg border-border text-text-muted'}`}
                >
                  {stage.n}
                </div>
                <div className={`text-[11px] mt-1.5 text-center max-w-[70px] leading-[1.3]
                  ${stage.state === 'done' ? 'text-success font-medium'
                  : stage.state === 'current' ? 'text-[#92640A] font-semibold'
                  : 'text-text-muted'}`}
                >
                  {stage.label}
                </div>
              </div>
              {i < PIPELINE_STAGES.length - 1 && (
                <div className={`h-0.5 w-10 flex-shrink-0 -mt-[22px] ${stage.state === 'done' ? 'bg-success' : 'bg-border'}`} />
              )}
            </div>
          ))}
        </div>
      </Card>

      {/* Filters */}
      <div className="flex items-center gap-2.5 mb-4 flex-wrap">
        <div className="relative">
          <svg className="absolute left-3 top-1/2 -translate-y-1/2 text-text-muted" width="14" height="14" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="2">
            <circle cx="9" cy="9" r="6"/>
            <path d="m15 15-3-3"/>
          </svg>
          <input 
            className="pl-8 pr-3 py-2 bg-bg-card border border-border-strong rounded-lg text-[13px] outline-none focus:border-gold w-60 font-sans" 
            placeholder="Search client name or PAN…" 
          />
        </div>
        <select 
          onChange={e => setStage(e.target.value)} 
          className="bg-bg-card border border-border-strong rounded-lg px-3 py-2 text-[13px] outline-none focus:border-gold font-sans cursor-pointer"
        >
          {['All Stages', 'Doc Pending', 'Reconciliation', 'Mismatch Review', 'Ready to File', 'Filed', 'E-Verify Pending'].map(o => 
            <option key={o}>{o}</option>
          )}
        </select>
        <select 
          onChange={e => setForm(e.target.value)} 
          className="bg-bg-card border border-border-strong rounded-lg px-3 py-2 text-[13px] outline-none focus:border-gold font-sans cursor-pointer"
        >
          {['All ITR Forms', 'ITR-1', 'ITR-2', 'ITR-3', 'ITR-4'].map(o => 
            <option key={o}>{o}</option>
          )}
        </select>
        <div className="ml-auto">
          <Button variant="primary" size="sm" onClick={() => setShowNewFilingModal(true)}>+ New ITR Filing</Button>
        </div>
      </div>

      {/* Filings table */}
      <Card>
        {filingsList && filingsList.length > 0 ? (
          <table className="w-full">
            <thead>
              <tr className="border-b border-border">
                {['Client', 'PAN', 'ITR Form', 'Stage', 'Last Updated', 'Actions'].map(h => (
                  <th key={h} className="text-left py-3 px-4 text-[11px] font-semibold text-text-muted uppercase bg-bg">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {filingsList.map((filing: any) => (
                <tr key={filing.id} className="border-b border-border hover:bg-bg">
                  <td className="py-3 px-4 text-[13px] font-medium">{filing.clientName}</td>
                  <td className="py-3 px-4 font-mono text-[12.5px]">{filing.pan}</td>
                  <td className="py-3 px-4"><Badge label={filing.itrForm} variant="blue" size="sm" /></td>
                  <td className="py-3 px-4"><Badge label={filing.stage || 'Draft'} variant="warning" size="sm" /></td>
                  <td className="py-3 px-4 text-[12px] text-text-muted">{filing.lastUpdated || 'Just now'}</td>
                  <td className="py-3 px-4">
                    <Button variant="ghost" size="sm" onClick={() => openComputation(filing.clientId)}>Open</Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : (
          <div className="text-center py-12 text-text-muted">
            No filings yet. Click "+ New ITR Filing" to create one.
          </div>
        )}
      </Card>

      {/* New Filing Modal */}
      {showNewFilingModal && (
        <Modal isOpen={showNewFilingModal} title="Create New ITR Filing" onClose={() => setShowNewFilingModal(false)}>
          <div className="space-y-4">
            <div>
              <label className="block text-[12px] font-semibold text-text-secondary mb-2">Select Client</label>
              <select
                value={selectedClientId || ''}
                onChange={e => setSelectedClientId(Number(e.target.value))}
                className="w-full px-3 py-2 border border-border-strong rounded-lg text-[13px] outline-none focus:border-gold"
              >
                <option value="">Choose a client...</option>
                {clientsList.map((client: any) => (
                  <option key={client.id} value={client.id}>
                    {client.name} ({client.pan})
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-[12px] font-semibold text-text-secondary mb-2">ITR Form</label>
              <select
                value={selectedITRForm}
                onChange={e => setSelectedITRForm(e.target.value)}
                className="w-full px-3 py-2 border border-border-strong rounded-lg text-[13px] outline-none focus:border-gold"
              >
                <option value="ITR-1">ITR-1 (Salary)</option>
                <option value="ITR-2">ITR-2 (Capital Gains)</option>
                <option value="ITR-3">ITR-3 (Business)</option>
                <option value="ITR-4">ITR-4 (Presumptive)</option>
              </select>
            </div>
            <div>
              <label className="block text-[12px] font-semibold text-text-secondary mb-2">Assessment Year</label>
              <input
                type="text"
                value={activeAY}
                disabled
                className="w-full px-3 py-2 border border-border-strong rounded-lg text-[13px] bg-bg"
              />
            </div>
            <div className="flex gap-2 pt-2">
              <Button
                variant="primary"
                className="flex-1"
                onClick={() => createFilingMutation.mutate()}
                disabled={!selectedClientId}
              >
                Create & Open Computation
              </Button>
              <Button variant="ghost" onClick={() => setShowNewFilingModal(false)}>Cancel</Button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}
