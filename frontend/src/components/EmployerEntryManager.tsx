import React, { useState } from 'react';

interface EmployerEntry {
  id: string;
  customEmployerName?: string;
  employerName?: string;
  employerTAN?: string;
  natureOfEmployment?: string;
  basic?: number;
  da?: number;
  hra?: number;
  bonus?: number;
  allowances?: number;
  lta?: number;
  rentPaid?: number;
  isMetroCity?: boolean;
  commutedPension?: number;
  gratuity?: number;
  leaveEncashment?: number;
  isGovernmentEmployee?: boolean;
  isDisabledEmployee?: boolean;
  childrenEducationAllowance?: number;
  hostelExpenditureAllowance?: number;
  professionalTax?: number;
  tdsDeducted?: number;
}

interface Props {
  entries: EmployerEntry[];
  onChange: (entries: EmployerEntry[]) => void;
}

const generateId = () => {
  return 'emp_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
};

const formatINR = (num?: number): string => {
  if (!num || isNaN(num) || num === 0) return '0';
  return Math.round(num).toLocaleString('en-IN');
};

const Section = ({ title, expanded, onClick, badge, children }: any) => (
  <div style={{ marginBottom: 8, border: '1px solid #e2e8f0', borderRadius: 8, overflow: 'hidden' }}>
    <button onClick={onClick} style={{ width: '100%', padding: 12, display: 'flex', justifyContent: 'space-between', background: expanded ? '#fef3e2' : '#f8fafc', border: 'none', cursor: 'pointer' }}>
      <span style={{ fontSize: 13, fontWeight: 600, color: '#475569' }}>{title}</span>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        {badge && <span style={{ fontSize: 11, background: '#e2e8f0', padding: '2px 8px', borderRadius: 4 }}>{badge}</span>}
        <span style={{ fontSize: 12, color: '#94a3b8' }}>{expanded ? '▲' : '▼'}</span>
      </div>
    </button>
    {expanded && <div style={{ padding: 16, background: 'white' }}>{children}</div>}
  </div>
);

const F = ({ label, hint, children }: any) => (
  <div>
    <label style={{ display: 'block', marginBottom: 4, fontSize: 12, fontWeight: 500, color: '#64748b' }}>{label}</label>
    {children}
    {hint && <div style={{ fontSize: 10, color: '#94a3b8', marginTop: 2 }}>{hint}</div>}
  </div>
);

const Inp = (p: any) => {
  const val = p.value ?? '';
  // Ensure we always pass a valid number to parent
  const handleChange = (e: any) => {
    let raw = e.target.value;
    // Only allow digits
    let clean = raw.replace(/[^\d]/g, '');
    let num = clean === '' ? 0 : parseInt(clean, 10);
    if (isNaN(num)) num = 0;
    // Cap at reasonable max (10 crore = 100000000)
    if (num > 100000000) num = 100000000;
    p.onChange(num);
  };
  return (
    <input 
      type="text" 
      inputMode="numeric"
      value={val === 0 || val === '' ? '' : val.toString()} 
      onChange={handleChange}
      style={{ width: '100%', padding: '8px 10px', border: '1px solid #e2e8f0', borderRadius: 6, fontSize: 13, ...p.style }} 
    />
  );
};

export function EmployerEntryManager({ entries = [], onChange }: Props) {
  const [expandedId, setExpandedId] = useState<string | null>(null);

  const addEntry = () => {
    onChange([...entries, { id: generateId(), customEmployerName: `Employer ${entries.length + 1}` }]);
  };

  const updateEntry = (id: string, updates: Partial<EmployerEntry>) => {
    onChange(entries.map(e => e.id === id ? { ...e, ...updates } : e));
  };

  const removeEntry = (id: string) => {
    onChange(entries.filter(e => e.id !== id));
    if (expandedId === id) setExpandedId(null);
  };

  const toggleExpand = (id: string) => {
    setExpandedId(expandedId === id ? null : id);
  };

  // Calculate local gross
  const getGross = (e: EmployerEntry) => (e.basic || 0) + (e.da || 0) + (e.hra || 0) + (e.bonus || 0) + (e.allowances || 0) + (e.lta || 0);
  const totalGross = () => entries.reduce((s, e) => s + getGross(e), 0);
  const totalTDS = () => entries.reduce((s, e) => s + (e.tdsDeducted || 0), 0);

  return (
    <div style={{ marginBottom: 24 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <h3 style={{ margin: 0, fontSize: 14, fontWeight: 600, color: '#64748b' }}>Salary Details</h3>
        <button onClick={addEntry} style={{ padding: '8px 16px', background: '#c9943a', color: 'white', border: 'none', borderRadius: 6, fontSize: 13, fontWeight: 500, cursor: 'pointer' }}>+ Add Employer</button>
      </div>

      {entries.length === 0 ? (
        <div style={{ textAlign: 'center', padding: 40, color: '#94a3b8', background: '#f8fafc', borderRadius: 8, border: '1px dashed #cbd5e1' }}>
          Click "+ Add Employer" to add salary details
        </div>
      ) : entries.map(entry => (
        <div key={entry.id} style={{ background: 'white', border: '1px solid #e2e8f0', borderRadius: 12, padding: 20, marginBottom: 16 }}>
          {/* Header */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16, paddingBottom: 16, borderBottom: '1px solid #f1f5f9' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <span style={{ background: '#c9943a', color: 'white', padding: '4px 12px', borderRadius: 6, fontSize: 12, fontWeight: 600 }}>#{entries.indexOf(entry) + 1}</span>
              <input type="text" value={entry.customEmployerName || `Employer ${entries.indexOf(entry) + 1}`}
                onChange={(e: any) => updateEntry(entry.id, { customEmployerName: e.target.value })}
                style={{ border: 'none', borderBottom: '1px dashed #c9943a', background: 'transparent', fontSize: 14, fontWeight: 600, color: '#1e293b', outline: 'none', minWidth: 150 }} />
            </div>
            <button onClick={() => removeEntry(entry.id)} style={{ background: '#fef2f2', color: '#ef4444', border: 'none', width: 28, height: 28, borderRadius: '50%', cursor: 'pointer', fontSize: 16 }}>×</button>
          </div>

          {/* Summary */}
          <div style={{ padding: 16, background: 'linear-gradient(135deg, #fef3e2, #fff7ed)', borderRadius: 8, marginBottom: 16, border: '1px solid #fed7aa' }}>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16 }}>
              <div><div style={{ fontSize: 11, color: '#78716c' }}>Gross</div><div style={{ fontSize: 16, fontWeight: 700 }}>₹{formatINR(getGross(entry))}</div></div>
              <div><div style={{ fontSize: 11, color: '#78716c' }}>Taxable</div><div style={{ fontSize: 16, fontWeight: 700, color: '#c9943a' }}>₹{formatINR(getGross(entry))}</div></div>
              <div><div style={{ fontSize: 11, color: '#78716c' }}>TDS</div><div style={{ fontSize: 16, fontWeight: 700 }}>₹{formatINR(entry.tdsDeducted)}</div></div>
            </div>
          </div>

          {/* Sections */}
          <Section title="Employer Details" expanded={expandedId === `emp-${entry.id}`} onClick={() => toggleExpand(`emp-${entry.id}`)} badge={entry.employerName || 'Required'}>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12 }}>
              <F label="Employer Name"><Inp value={entry.employerName || ''} onChange={(v: any) => updateEntry(entry.id, { employerName: v })} /></F>
              <F label="TAN"><Inp value={entry.employerTAN || ''} onChange={(v: any) => updateEntry(entry.id, { employerTAN: v.toUpperCase() })} /></F>
              <F label="Nature">
                <select value={entry.natureOfEmployment || 'NGOV'} onChange={(e: any) => updateEntry(entry.id, { natureOfEmployment: e.target.value })} style={{ width: '100%', padding: '8px 10px', border: '1px solid #e2e8f0', borderRadius: 6, fontSize: 13 }}>
                  <option value="NGOV">Private</option><option value="GOV">Government</option><option value="PSU">PSU</option>
                </select>
              </F>
            </div>
          </Section>

          <Section title="Salary Components" expanded={expandedId === `sal-${entry.id}`} onClick={() => toggleExpand(`sal-${entry.id}`)} badge="">
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 12 }}>
              <F label="Basic Salary"><Inp type="number" value={entry.basic || ''} onChange={(v: any) => updateEntry(entry.id, { basic: v })} /></F>
              <F label="DA"><Inp type="number" value={entry.da || ''} onChange={(v: any) => updateEntry(entry.id, { da: v })} /></F>
              <F label="HRA"><Inp type="number" value={entry.hra || ''} onChange={(v: any) => updateEntry(entry.id, { hra: v })} /></F>
              <F label="Bonus"><Inp type="number" value={entry.bonus || ''} onChange={(v: any) => updateEntry(entry.id, { bonus: v })} /></F>
              <F label="Other Allowances"><Inp type="number" value={entry.allowances || ''} onChange={(v: any) => updateEntry(entry.id, { allowances: v })} /></F>
              <F label="LTA"><Inp type="number" value={entry.lta || ''} onChange={(v: any) => updateEntry(entry.id, { lta: v })} /></F>
            </div>
          </Section>

          <Section title="HRA Exemption" expanded={expandedId === `hra-${entry.id}`} onClick={() => toggleExpand(`hra-${entry.id}`)} badge="">
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12 }}>
              <F label="Annual Rent Paid"><Inp type="number" value={entry.rentPaid || ''} onChange={(v: any) => updateEntry(entry.id, { rentPaid: v })} hint="Required for exemption" /></F>
              <F label="Metro City">
                <select value={entry.isMetroCity ? 'yes' : 'no'} onChange={(e: any) => updateEntry(entry.id, { isMetroCity: e.target.value === 'yes' })} style={{ width: '100%', padding: '8px 10px', border: '1px solid #e2e8f0', borderRadius: 6, fontSize: 13 }}>
                  <option value="no">No (40%)</option><option value="yes">Yes (50%)</option>
                </select>
              </F>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 20 }}>
                <input type="checkbox" checked={entry.isGovernmentEmployee || false} onChange={(e: any) => updateEntry(entry.id, { isGovernmentEmployee: e.target.checked })} />
                <span style={{ fontSize: 12, color: '#64748b' }}>Govt Employee</span>
              </div>
            </div>
          </Section>

          <Section title="Retirement Benefits" expanded={expandedId === `ret-${entry.id}`} onClick={() => toggleExpand(`ret-${entry.id}`)} badge="">
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 12 }}>
              <F label="Commuted Pension"><Inp type="number" value={entry.commutedPension || ''} onChange={(v: any) => updateEntry(entry.id, { commutedPension: v })} /></F>
              <F label="Gratuity"><Inp type="number" value={entry.gratuity || ''} onChange={(v: any) => updateEntry(entry.id, { gratuity: v })} /></F>
              <F label="Leave Encashment"><Inp type="number" value={entry.leaveEncashment || ''} onChange={(v: any) => updateEntry(entry.id, { leaveEncashment: v })} /></F>
            </div>
          </Section>

          <Section title="Special Allowances" expanded={expandedId === `all-${entry.id}`} onClick={() => toggleExpand(`all-${entry.id}`)} badge="">
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12 }}>
              <F label="Transport (Disabled)">
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}><input type="checkbox" checked={entry.isDisabledEmployee || false} onChange={(e: any) => updateEntry(entry.id, { isDisabledEmployee: e.target.checked })} /><span style={{ fontSize: 11 }}>Disabled</span></div>
              </F>
              <F label="Children Education"><Inp type="number" value={entry.childrenEducationAllowance || ''} onChange={(v: any) => updateEntry(entry.id, { childrenEducationAllowance: v })} /></F>
              <F label="Hostel Expenditure"><Inp type="number" value={entry.hostelExpenditureAllowance || ''} onChange={(v: any) => updateEntry(entry.id, { hostelExpenditureAllowance: v })} /></F>
            </div>
          </Section>

          <Section title="Deductions & TDS" expanded={expandedId === `ded-${entry.id}`} onClick={() => toggleExpand(`ded-${entry.id}`)} badge="">
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12 }}>
              <F label="Professional Tax"><Inp type="number" value={entry.professionalTax || ''} onChange={(v: any) => updateEntry(entry.id, { professionalTax: v })} /></F>
              <F label="Entertainment (Govt)"><Inp type="number" value={0} disabled style={{ background: '#f0f0f0' }} /></F>
              <F label="TDS Deducted"><Inp type="number" value={entry.tdsDeducted || ''} onChange={(v: any) => updateEntry(entry.id, { tdsDeducted: v })} /></F>
            </div>
          </Section>
        </div>
      ))}

      {entries.length > 0 && (
        <div style={{ padding: 20, background: 'linear-gradient(135deg, #1e293b, #334155)', borderRadius: 12, color: 'white' }}>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 24, textAlign: 'center' }}>
            <div><div style={{ fontSize: 12, opacity: 0.7 }}>TOTAL GROSS</div><div style={{ fontSize: 24, fontWeight: 700 }}>₹{formatINR(totalGross())}</div></div>
            <div><div style={{ fontSize: 12, opacity: 0.7 }}>TAXABLE INCOME</div><div style={{ fontSize: 24, fontWeight: 700, color: '#fbbf24' }}>₹{formatINR(totalGross())}</div></div>
            <div><div style={{ fontSize: 12, opacity: 0.7 }}>TOTAL TDS</div><div style={{ fontSize: 24, fontWeight: 700 }}>₹{formatINR(totalTDS())}</div></div>
          </div>
        </div>
      )}
    </div>
  );
}
