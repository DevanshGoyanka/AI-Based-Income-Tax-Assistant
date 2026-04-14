import { useQuery } from '@tanstack/react-query';
import { clientsApi } from '../../api/clients';
import Badge from '../ui/Badge';
import Button from '../ui/Button';
import { useAppStore } from '../../store/useAppStore';

interface Props { 
  clientId: number; 
  onClose: () => void; 
}

export default function ClientDetailPanel({ clientId, onClose }: Props) {
  const { setActiveView } = useAppStore();
  
  const { data: client, isLoading } = useQuery({
    queryKey: ['client', clientId],
    queryFn: () => clientsApi.get(clientId),
  });

  return (
    <>
      <div className="fixed inset-0 bg-black/20 z-[199]" onClick={onClose} />
      <div className="fixed top-0 right-0 w-[440px] h-screen bg-bg-card border-l border-border z-[200] flex flex-col shadow-[-4px_0_20px_rgba(0,0,0,0.08)] overflow-hidden">
        {/* Header */}
        <div className="px-6 pt-5 pb-4 border-b border-border flex items-start gap-3 flex-shrink-0">
          <div className="w-[46px] h-[46px] rounded-xl bg-navy flex items-center justify-center font-serif text-[18px] font-semibold text-gold-light flex-shrink-0">
            {client?.initials ?? '??'}
          </div>
          <div className="flex-1">
            <div className="text-[16px] font-semibold text-text-primary">{client?.name ?? '—'}</div>
            <div className="font-mono text-[12px] text-text-muted mt-0.5">{client?.pan} · {client?.type}</div>
          </div>
          <button 
            onClick={onClose} 
            className="text-text-muted hover:text-text-primary hover:bg-bg rounded p-1 transition-colors"
          >
            <svg width="16" height="16" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M15 5 5 15M5 5l10 10"/>
            </svg>
          </button>
        </div>

        <div className="flex-1 overflow-y-auto px-6 py-5">
          {isLoading ? (
            <div className="space-y-3">
              {Array.from({ length: 8 }).map((_, i) => (
                <div key={i} className="h-5 bg-border rounded animate-pulse" />
              ))}
            </div>
          ) : client ? (
            <>
              {/* Status badges */}
              <div className="flex gap-1.5 flex-wrap mb-4">
                <Badge 
                  label={client.filingStatus} 
                  variant={client.filingStatus === 'Filed' ? 'success' : 'warning'} 
                />
                <Badge 
                  label={client.evStatus} 
                  variant={client.evStatus === 'Verified' ? 'info' : 'neutral'} 
                />
                <Badge label={client.itrForm} variant="neutral" />
              </div>

              {/* Personal details */}
              <PanelSection title="Personal Details">
                <DetailRow label="Full Name" value={client.name} />
                <DetailRow label="PAN" value={client.pan} mono />
                <DetailRow label="Date of Birth" value={client.dateOfBirth} />
                <DetailRow label="Aadhaar" value={client.aadhaarMasked} mono />
                <DetailRow label="Mobile" value={client.mobile} />
                <DetailRow label="Email" value={client.email} />
                <DetailRow 
                  label="Comm. Channel" 
                  value={<Badge label={client.communicationChannel} variant="success" />} 
                />
              </PanelSection>

              {/* Filing details */}
              <PanelSection title={`AY ${client.assessmentYear} Filing`}>
                <DetailRow label="ITR Form" value={client.itrForm} />
                <DetailRow 
                  label="Filing Status" 
                  value={<Badge label={client.filingStatus} variant={client.filingStatus === 'Filed' ? 'success' : 'info'} />} 
                />
                <DetailRow 
                  label="Recon Status" 
                  value={<Badge label={client.reconStatus} variant={client.reconStatus === 'Clean' ? 'success' : client.reconStatus === 'Mismatch' ? 'danger' : 'neutral'} />} 
                />
                <DetailRow label="Last Synced" value={client.lastSynced} />
              </PanelSection>

              {/* Actions */}
              <div className="flex flex-col gap-2 mt-4">
                <Button 
                  variant="primary" 
                  className="w-full justify-center" 
                  onClick={() => { setActiveView('filing'); onClose(); }}
                >
                  Open Computation
                </Button>
                <Button variant="secondary" className="w-full justify-center">
                  Download Computation
                </Button>
                <Button variant="ghost" className="w-full justify-center">
                  Send WhatsApp
                </Button>
                <Button variant="ghost" className="w-full justify-center">
                  View All AYs
                </Button>
              </div>
            </>
          ) : null}
        </div>
      </div>
    </>
  );
}

function PanelSection({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="mb-[22px]">
      <div className="text-[11px] font-bold uppercase tracking-[0.8px] text-text-muted mb-2.5 pb-2 border-b border-border">
        {title}
      </div>
      {children}
    </div>
  );
}

function DetailRow({ label, value, mono }: { label: string; value: React.ReactNode; mono?: boolean }) {
  return (
    <div className="flex items-start gap-2.5 py-1.5">
      <div className="text-[12px] text-text-muted w-[130px] flex-shrink-0">{label}</div>
      <div className={`text-[13px] text-text-primary flex-1 ${mono ? 'font-mono text-[12.5px]' : ''}`}>
        {value}
      </div>
    </div>
  );
}
