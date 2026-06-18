import React, { useState, useEffect, useRef, useCallback } from 'react';
import { calculateSalary, type EmployerInput, type SalaryCalculationResponse } from '../services/salaryCalculationService';

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
  assessmentYear: string;
  taxRegime?: string;
}

const generateId = () => 'emp_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);

const formatINR = (num?: number): string => {
  if (!num || typeof num !== 'number' || isNaN(num)) return '0';
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

const Inp = (p: any) => (
  <input type="text" inputMode="numeric" value={p.value === 0 || p.value === undefined ? '' : String(p.value)} 
    onChange={(e: any) => { let val = String(e.target.value).replace(/[^\d]/g, ''); p.onChange(val === '' ? 0 : parseInt(val, 10) || 0); }}
    style={{ width: '100%', padding: '8px 10px', border: '1px solid #e2e8f0', borderRadius: 6, fontSize: 13 }} />
);

export function EmployerEntryManager({ entries = [], onChange, assessmentYear, taxRegime = 'OLD' }: Props) {
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [result, setResult] = useState<SalaryCalculationResponse | null>(null);
  const debounceRef = useRef<NodeJS.Timeout | null>(null);

  const toggleExpand = (id: string) => setExpandedId(expandedId === id ? null : id);

  const addEntry = () => onChange([...entries, { id: generateId(), customEmployerName: `Employer ${entries.length + 1}` }]);

  const updateEntry = (id: string, updates: Partial<EmployerEntry>) => {
    onChange(entries.map(e => e.id === id ? { ...e, ...updates } : e));
  };

  const removeEntry = (id: string) => onChange(entries.filter(e => e.id !== id));

  // Central calculation function
  const calculate = useCallback(async () => {
    const hasData = entries.some(e => (e.basic || e.hra || e.bonus || e.gratuity || e.leaveEncashment || e.professionalTax) > 0);
    if (!hasData) {
      setResult(null);
      return;
    }

    setResult(null);

    // Small delay to ensure state update
    await new Promise(resolve => setTimeout(resolve, 100));

    try {
      const inputs: EmployerInput[] = entries.map(e => ({
        employerName: e.employerName || 'Employer',
        employerTAN: e.employerTAN || '',
        basic: (e.basic || 0) * 100,
        da: (e.da || 0) * 100,
        hra: (e.hra || 0) * 100,
        bonus: (e.bonus || 0) * 100,
        allowances: (e.allowances || 0) * 100,
        lta: (e.lta || 0) * 100,
        rentPaid: (e.rentPaid || 0) * 100,
        isMetroCity: e.isMetroCity,
        pension: 0,
        commutedPension: (e.commutedPension || 0) * 100,
        gratuity: (e.gratuity || 0) * 100,
        leaveEncashment: (e.leaveEncashment || 0) * 100,
        professionalTax: (e.professionalTax || 0) * 100,
        entertainmentAllowance: 0,
        tdsDeducted: (e.tdsDeducted || 0) * 100,
        isDisabledEmployee: e.isDisabledEmployee,
        isGovernmentEmployee: e.isGovernmentEmployee,
        childrenEducationAllowance: (e.childrenEducationAllowance || 0) * 100,
        hostelExpenditureAllowance: (e.hostelExpenditureAllowance || 0) * 100,
        // Send actual value - backend will handle defaults
        transportAllowance: (e.transportAllowance || 0) * 100,
      }));

      const regimeToUse = taxRegime === 'NEW' ? 'NEW' : 'OLD';
      console.log('[SALARY] Recalculating with regime:', regimeToUse, 'entries:', entries.length);
      
      const res = await calculateSalary(assessmentYear, inputs, regimeToUse);
      console.log('[SALARY] Result:', res);
      
      const toRupees = (v: number | undefined | null): number => v ? Math.round(v / 100) : 0;
      setResult({
        hraExempt: toRupees(res.hraExempt),
        ltaExempt: toRupees(res.ltaExempt),
        gratuityExempt: toRupees(res.gratuityExempt),
        leaveEncashmentExempt: toRupees(res.leaveEncashmentExempt),
        transportExempt: toRupees((res as any).transportExempt),
        childrenEducationExempt: toRupees((res as any).childrenEducationExempt),
        hostelExempt: toRupees((res as any).hostelExempt),
        totalExemptions: toRupees(res.totalExemptions),
        standardDeduction: toRupees(res.standardDeduction),
        netTaxableSalary: toRupees(res.netTaxableSalary),
      });
    } catch (err) {
      console.error('[SALARY] Calc error:', err);
    }
  }, [entries, assessmentYear, taxRegime]);

  // Force recalculation when taxRegime changes
  useEffect(() => {
    calculate();
  }, [taxRegime]);
  
  // Also recalc when entry values change
  useEffect(() => {
    calculate();
  }, [entries, taxRegime]);

  const getGross = (e: EmployerEntry) => {
    const b = typeof e.basic === 'number' && e.basic > 0 ? e.basic : 0;
    const d = typeof e.da === 'number' && e.da > 0 ? e.da : 0;
    const h = typeof e.hra === 'number' && e.hra > 0 ? e.hra : 0;
    const bn = typeof e.bonus === 'number' && e.bonus > 0 ? e.bonus : 0;
    const a = typeof e.allowances === 'number' && e.allowances > 0 ? e.allowances : 0;
    const l = typeof e.lta === 'number' && e.lta > 0 ? e.lta : 0;
    const g = typeof e.gratuity === 'number' && e.gratuity > 0 ? e.gratuity : 0;
    const leave = typeof e.leaveEncashment === 'number' && e.leaveEncashment > 0 ? e.leaveEncashment : 0;
    return b + d + h + bn + a + l + g + leave;
  };

  const totalGross = () => entries.reduce((s, e) => s + getGross(e), 0);
  const totalTDS = () => entries.reduce((s, e) => s + (e.tdsDeducted || 0), 0);

  // Calculate Section 10 exemptions for display (include gratuity & leave encashment)
  const sec10Exempt = result 
    ? (result.hraExempt||0) + (result.ltaExempt||0) + ((result as any).transportExempt||0) + 
      ((result as any).childrenEducationExempt||0) + ((result as any).hostelExempt||0) +
      (result.gratuityExempt||0) + (result.leaveEncashmentExempt||0)
    : 0;
  const stdDed = result?.standardDeduction || 0;
  const taxable = totalGross() - sec10Exempt - stdDed;

  return (
    <div style={{ marginBottom: 24 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <h3 style={{ margin: 0, fontSize: 14, fontWeight: 600, color: '#64748b' }}>Salary Details ({taxRegime})</h3>
        <button onClick={addEntry} style={{ padding: '8px 16px', background: '#c9943a', color: 'white', border: 'none', borderRadius: 6, fontSize: 13, cursor: 'pointer' }}>+ Add</button>
      </div>

      {entries.length === 0 ? (
        <div style={{ textAlign: 'center', padding: 40, color: '#94a3b8', background: '#f8fafc', borderRadius: 8, border: '1px dashed #cbd5e1' }}>Click "+ Add" to add salary details</div>
      ) : entries.map(e => (
        <div key={e.id} style={{ background: 'white', border: '1px solid #e2e8f0', borderRadius: 12, padding: 20, marginBottom: 16 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16, paddingBottom: 16, borderBottom: '1px solid #f1f5f9' }}>
            <input type="text" value={e.customEmployerName || 'Employer'} onChange={(ev: any) => updateEntry(e.id, { customEmployerName: ev.target.value })}
              style={{ border: 'none', borderBottom: '1px dashed #c9943a', background: 'transparent', fontSize: 14, fontWeight: 600, minWidth: 150, outline: 'none' }} />
            <button onClick={() => removeEntry(e.id)} style={{ background: '#fef2f2', color: '#ef4444', border: 'none', width: 28, height: 28, borderRadius: '50%', cursor: 'pointer' }}>×</button>
          </div>

          <div style={{ padding: 16, background: 'linear-gradient(135deg, #fef3e2, #fff7ed)', borderRadius: 8, marginBottom: 16, border: '1px solid #fed7aa' }}>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16 }}>
              <div><div style={{ fontSize: 11, color: '#78716c' }}>Gross</div><div style={{ fontSize: 16, fontWeight: 700 }}>₹{formatINR(getGross(e))}</div></div>
              <div><div style={{ fontSize: 11, color: '#78716c' }}>Exempt</div><div style={{ fontSize: 16, fontWeight: 700, color: '#16a34a' }}>-₹{result ? formatINR((result.hraExempt||0) + ((result as any).transportExempt||0)) : '0'}</div></div>
              <div><div style={{ fontSize: 11, color: '#78716c' }}>Taxable</div><div style={{ fontSize: 16, fontWeight: 700, color: '#c9943a' }}>₹{result ? formatINR(taxable) : formatINR(getGross(e))}</div></div>
              <div><div style={{ fontSize: 11, color: '#78716c' }}>TDS</div><div style={{ fontSize: 16, fontWeight: 700 }}>₹{formatINR(e.tdsDeducted)}</div></div>
            </div>
          </div>

          <Section title="Employer Details" expanded={expandedId === `emp-${e.id}`} onClick={() => toggleExpand(`emp-${e.id}`)} badge={e.employerName || 'Req'}>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12 }}>
              <F label="Name"><Inp value={e.employerName || ''} onChange={(v: any) => updateEntry(e.id, { employerName: v })} /></F>
              <F label="TAN"><Inp value={e.employerTAN || ''} onChange={(v: any) => updateEntry(e.id, { employerTAN: v.toUpperCase() })} /></F>
              <F label="Nature">
                <select value={e.natureOfEmployment || 'NGOV'} onChange={(ev: any) => updateEntry(e.id, { natureOfEmployment: ev.target.value })} style={{ width: '100%', padding: '8px 10px', border: '1px solid #e2e8f0', borderRadius: 6 }}>
                  <option value="NGOV">Private</option><option value="GOV">Government</option><option value="PSU">PSU</option>
                </select>
              </F>
            </div>
          </Section>

          <Section title="Salary Components" expanded={expandedId === `sal-${e.id}`} onClick={() => toggleExpand(`sal-${e.id}`)} badge="">
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 12 }}>
              <F label="Basic"><Inp type="number" value={e.basic} onChange={(v: any) => updateEntry(e.id, { basic: v })} /></F>
              <F label="DA"><Inp type="number" value={e.da} onChange={(v: any) => updateEntry(e.id, { da: v })} /></F>
              <F label="HRA"><Inp type="number" value={e.hra} onChange={(v: any) => updateEntry(e.id, { hra: v })} /></F>
              <F label="Bonus"><Inp type="number" value={e.bonus} onChange={(v: any) => updateEntry(e.id, { bonus: v })} /></F>
              <F label="Allowances"><Inp type="number" value={e.allowances} onChange={(v: any) => updateEntry(e.id, { allowances: v })} /></F>
              <F label="LTA"><Inp type="number" value={e.lta} onChange={(v: any) => updateEntry(e.id, { lta: v })} /></F>
            </div>
          </Section>

          <Section title="HRA Exemption" expanded={expandedId === `hra-${e.id}`} onClick={() => toggleExpand(`hra-${e.id}`)} badge={result?.hraExempt || 0}>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12 }}>
              <F label="Rent Paid"><Inp type="number" value={e.rentPaid} onChange={(v: any) => updateEntry(e.id, { rentPaid: v })} hint="Required" /></F>
              <F label="Metro">
                <select value={e.isMetroCity ? 'yes' : 'no'} onChange={(ev: any) => updateEntry(e.id, { isMetroCity: ev.target.value === 'yes' })} style={{ width: '100%', padding: '8px 10px', border: '1px solid #e2e8f0', borderRadius: 6 }}>
                  <option value="no">No (40%)</option><option value="yes">Yes (50%)</option>
                </select>
              </F>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 20 }}>
                <input type="checkbox" checked={e.isGovernmentEmployee || false} onChange={(ev: any) => updateEntry(e.id, { isGovernmentEmployee: ev.target.checked })} />
                <span style={{ fontSize: 12 }}>Govt Employee</span>
              </div>
            </div>
          </Section>

          <Section title="Retirement" expanded={expandedId === `ret-${e.id}`} onClick={() => toggleExpand(`ret-${e.id}`)} badge="">
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12 }}>
              <F label="Commuted"><Inp type="number" value={e.commutedPension} onChange={(v: any) => updateEntry(e.id, { commutedPension: v })} /></F>
              <F label="Gratuity"><Inp type="number" value={e.gratuity} onChange={(v: any) => updateEntry(e.id, { gratuity: v })} /></F>
              <F label="Leave"><Inp type="number" value={e.leaveEncashment} onChange={(v: any) => updateEntry(e.id, { leaveEncashment: v })} /></F>
            </div>
          </Section>

          <Section title="Allowances" expanded={expandedId === `all-${e.id}`} onClick={() => toggleExpand(`all-${e.id}`)} badge="">
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12 }}>
              <F label="Transport"><input type="checkbox" checked={e.isDisabledEmployee || false} onChange={(ev: any) => updateEntry(e.id, { isDisabledEmployee: ev.target.checked })} /><span style={{fontSize:11, marginLeft:4}}>Disabled</span></F>
              <F label="Children"><Inp type="number" value={e.childrenEducationAllowance} onChange={(v: any) => updateEntry(e.id, { childrenEducationAllowance: v })} /></F>
              <F label="Hostel"><Inp type="number" value={e.hostelExpenditureAllowance} onChange={(v: any) => updateEntry(e.id, { hostelExpenditureAllowance: v })} /></F>
            </div>
          </Section>

          <Section title="Deductions" expanded={expandedId === `ded-${e.id}`} onClick={() => toggleExpand(`ded-${e.id}`)} badge="">
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 12 }}>
              <F label="Prof Tax"><Inp type="number" value={e.professionalTax} onChange={(v: any) => updateEntry(e.id, { professionalTax: v })} /></F>
              <F label="TDS"><Inp type="number" value={e.tdsDeducted} onChange={(v: any) => updateEntry(e.id, { tdsDeducted: v })} /></F>
            </div>
          </Section>
        </div>
      ))}

      {entries.length > 0 && (
        <div style={{ padding: 20, background: 'linear-gradient(135deg, #1e293b, #334155)', borderRadius: 12, color: 'white' }}>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16, textAlign: 'center' }}>
            <div><div style={{ fontSize: 12, opacity: 0.7 }}>GROSS</div><div style={{ fontSize: 24, fontWeight: 700 }}>₹{formatINR(totalGross())}</div></div>
            <div><div style={{ fontSize: 12, opacity: 0.7 }}>EXEMPT u/s 10</div><div style={{ fontSize: 24, fontWeight: 700, color: '#4ade80' }}>-₹{formatINR(sec10Exempt)}</div></div>
            <div><div style={{ fontSize: 12, opacity: 0.7 }}>TAXABLE</div><div style={{ fontSize: 24, fontWeight: 700, color: '#fbbf24' }}>₹{formatINR(taxable)}</div></div>
            <div><div style={{ fontSize: 12, opacity: 0.7 }}>TDS</div><div style={{ fontSize: 24, fontWeight: 700 }}>₹{formatINR(totalTDS())}</div></div>
          </div>
          <div style={{ fontSize: 11, marginTop: 8, textAlign: 'center', opacity: 0.7 }}>
            Std Ded: ₹{formatINR(stdDed)}
          </div>
        </div>
      )}
    </div>
  );
}
