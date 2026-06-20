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
  // New exemption fields
  transportAllowance?: number;
  entertainmentAllowance?: number;
  vrsCompensation?: number;
  retrenchmentCompensation?: number;
  perquisites?: number;
  profitsInLieu?: number;
  commission?: number;
  otherAllowance?: number;
  ltaExempt?: number;
  otherExempt?: number;
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

  const toggleExpand = (id: string) => setExpandedId(expandedId === id ? null : id);

  const addEntry = () => onChange([...entries, { id: generateId(), customEmployerName: `Employer ${entries.length + 1}` }]);

  const updateEntry = (id: string, updates: Partial<EmployerEntry>) => {
    onChange(entries.map(e => e.id === id ? { ...e, ...updates } : e));
  };

  const removeEntry = (id: string) => onChange(entries.filter(e => e.id !== id));

  const getGross = (e: EmployerEntry) => {
    const b = typeof e.basic === 'number' && e.basic > 0 ? e.basic : 0;
    const d = typeof e.da === 'number' && e.da > 0 ? e.da : 0;
    const h = typeof e.hra === 'number' && e.hra > 0 ? e.hra : 0;
    const bn = typeof e.bonus === 'number' && e.bonus > 0 ? e.bonus : 0;
    const a = typeof e.allowances === 'number' && e.allowances > 0 ? e.allowances : 0;
    const l = typeof e.lta === 'number' && e.lta > 0 ? e.lta : 0;
    const cp = typeof e.commutedPension === 'number' && e.commutedPension > 0 ? e.commutedPension : 0;
    const g = typeof e.gratuity === 'number' && e.gratuity > 0 ? e.gratuity : 0;
    const leave = typeof e.leaveEncashment === 'number' && e.leaveEncashment > 0 ? e.leaveEncashment : 0;
    const perq = typeof e.perquisites === 'number' && e.perquisites > 0 ? e.perquisites : 0;
    const pil = typeof e.profitsInLieu === 'number' && e.profitsInLieu > 0 ? e.profitsInLieu : 0;
    const comm = typeof e.commission === 'number' && e.commission > 0 ? e.commission : 0;
    const oa = typeof e.otherAllowance === 'number' && e.otherAllowance > 0 ? e.otherAllowance : 0;
    const vrs = typeof e.vrsCompensation === 'number' && e.vrsCompensation > 0 ? e.vrsCompensation : 0;
    const retrench = typeof e.retrenchmentCompensation === 'number' && e.retrenchmentCompensation > 0 ? e.retrenchmentCompensation : 0;
    return b + d + h + bn + a + l + cp + g + leave + perq + pil + comm + oa + vrs + retrench;
  };

  // Calculate exemptions for OLD regime
  const calculateExemptions = (e: EmployerEntry) => {
    const gross = getGross(e);
    if (gross <= 0) return 0;
    if (taxRegime === 'NEW') {
      // New Regime: Only Standard Deduction of ₹75,000 (capped at gross)
      return Math.min(75000, gross);
    }
    if (taxRegime !== 'OLD') return 0;
    const basic = e.basic || 0;
    const da = e.da || 0;
    const basicDA = basic + da;
    const hra = e.hra || 0;
    const lta = e.lta || 0;
    const gratuity = e.gratuity || 0;
    const leaveEnc = e.leaveEncashment || 0;
    const commutedPen = e.commutedPension || 0;
    const rentPaid = e.rentPaid || 0;
    const isMetro = e.isMetroCity || false;
    const isGovt = e.isGovernmentEmployee || false;

    // HRA Exemption u/s 10(13A)
    const percentOfBasic = isMetro ? basicDA * 50 / 100 : basicDA * 40 / 100;
    const rentMinusTenPercent = Math.max(0, rentPaid - basicDA * 10 / 100);
    const hraExempt = Math.min(hra, Math.min(percentOfBasic, rentMinusTenPercent));

    // Transport Allowance u/s 10(14): max ₹19,200
    const transportExempt = Math.min(e.transportAllowance || 0, 19200);

    // Children Education Allowance u/s 10(14): max ₹2,400
    const ceaExempt = Math.min(e.childrenEducationAllowance || 0, 2400);

    // Hostel Expenditure u/s 10(14): max ₹7,200
    const hostelExempt = Math.min(e.hostelExpenditureAllowance || 0, 7200);

    // LTA Exemption u/s 10(5): min of LTA received, LTA exempt claimed
    const ltaExempt = Math.min(lta, e.ltaExempt || 0);

    // Gratuity Exemption u/s 10(10): max ₹20,00,000
    const gratuityExempt = Math.min(gratuity, 2000000);

    // Leave Encashment u/s 10(10AA): max ₹25,00,000
    const leaveExempt = Math.min(leaveEnc, 2500000);

    // Standard Deduction u/s 16(ia): ₹50,000
    const stdDed = 50000;

    // Professional Tax u/s 16(iii): max ₹2,500
    const profTaxExempt = Math.min(e.professionalTax || 0, 2500);

    // Entertainment Allowance u/s 16(ii): ₹5,000 (Govt only)
    const entExempt = isGovt ? Math.min(e.entertainmentAllowance || 0, 5000) : 0;

    // VRS Compensation u/s 10(10C): max ₹5,00,000
    const vrsExempt = Math.min(e.vrsCompensation || 0, 500000);

    // Retrenchment Compensation u/s 10(10B): max ₹5,00,000
    const retrenchExempt = Math.min(e.retrenchmentCompensation || 0, 500000);

    // Commuted Pension u/s 10(10A): 50% (Govt) / 33% (Non-Govt)
    const commutedExempt = isGovt ? commutedPen * 50 / 100 : commutedPen * 33 / 100;

    // Other Exemptions
    const otherExempt = e.otherExempt || 0;

    const total = hraExempt + transportExempt + ceaExempt + hostelExempt + ltaExempt +
           gratuityExempt + leaveExempt + stdDed + profTaxExempt + entExempt +
           vrsExempt + retrenchExempt + commutedExempt + otherExempt;

    // Cap exemptions at gross salary (can't exempt more than earned)
    return Math.min(total, gross);
  };

  const totalGross = () => entries.reduce((s, e) => s + getGross(e), 0);

  // CBDT Guidelines: Standard Deduction u/s 16(ia) is allowed ONLY ONCE per year,
  // not per employer. Same applies to Professional Tax u/s 16(iii).
  // HRA, LTA, Gratuity, etc. are per-employer specific.
  const totalExemptions = () => {
    if (taxRegime === 'NEW') {
      // NEW Regime: Standard Deduction ₹75,000 allowed only ONCE
      const totalGrossSalary = totalGross();
      return totalGrossSalary > 0 ? Math.min(75000, totalGrossSalary) : 0;
    }
    // OLD Regime: Sum per-employer exemptions, but Std Ded & Prof Tax only ONCE
    let perEmployerExemptions = 0;
    let totalProfTax = 0;
    for (const e of entries) {
      const gross = getGross(e);
      if (gross <= 0) continue;
      const basic = e.basic || 0;
      const da = e.da || 0;
      const basicDA = basic + da;
      const hra = e.hra || 0;
      const lta = e.lta || 0;
      const gratuity = e.gratuity || 0;
      const leaveEnc = e.leaveEncashment || 0;
      const commutedPen = e.commutedPension || 0;
      const rentPaid = e.rentPaid || 0;
      const isMetro = e.isMetroCity || false;
      const isGovt = e.isGovernmentEmployee || false;

      const percentOfBasic = isMetro ? basicDA * 50 / 100 : basicDA * 40 / 100;
      const rentMinusTenPercent = Math.max(0, rentPaid - basicDA * 10 / 100);
      const hraExempt = Math.min(hra, Math.min(percentOfBasic, rentMinusTenPercent));
      const transportExempt = Math.min(e.transportAllowance || 0, 19200);
      const ceaExempt = Math.min(e.childrenEducationAllowance || 0, 2400);
      const hostelExempt = Math.min(e.hostelExpenditureAllowance || 0, 7200);
      const ltaExempt = Math.min(lta, e.ltaExempt || 0);
      const gratuityExempt = Math.min(gratuity, 2000000);
      const leaveExempt = Math.min(leaveEnc, 2500000);
      const entExempt = isGovt ? Math.min(e.entertainmentAllowance || 0, 5000) : 0;
      const vrsExempt = Math.min(e.vrsCompensation || 0, 500000);
      const retrenchExempt = Math.min(e.retrenchmentCompensation || 0, 500000);
      const commutedExempt = isGovt ? commutedPen * 50 / 100 : commutedPen * 33 / 100;
      const otherExempt = e.otherExempt || 0;

      const perEmpTotal = hraExempt + transportExempt + ceaExempt + hostelExempt + ltaExempt +
             gratuityExempt + leaveExempt + entExempt + vrsExempt + retrenchExempt +
             commutedExempt + otherExempt;

      perEmployerExemptions += Math.min(perEmpTotal, gross);
      totalProfTax += e.professionalTax || 0;
    }
    // Standard Deduction ₹50,000 only ONCE
    const stdDed = 50000;
    // Professional Tax max ₹2,500 only ONCE (aggregate from all employers)
    const profTaxExempt = Math.min(totalProfTax, 2500);
    return perEmployerExemptions + stdDed + profTaxExempt;
  };

  const totalTDS = () => entries.reduce((s, e) => s + (e.tdsDeducted || 0), 0);

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
              <div>
                <div style={{ fontSize: 11, color: '#78716c' }}>
                  {taxRegime === 'NEW' ? 'Std Ded u/s 16(ia)' : 'Exempt u/s 10 & 16'}
                </div>
                <div style={{ fontSize: 16, fontWeight: 700, color: '#16a34a' }}>₹{formatINR(calculateExemptions(e))}</div>
              </div>
              <div><div style={{ fontSize: 11, color: '#78716c' }}>Taxable</div><div style={{ fontSize: 16, fontWeight: 700, color: '#c9943a' }}>₹{formatINR(Math.max(0, getGross(e) - calculateExemptions(e)))}</div></div>
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
              <F label="Commission"><Inp type="number" value={e.commission} onChange={(v: any) => updateEntry(e.id, { commission: v })} /></F>
              <F label="Perquisites"><Inp type="number" value={e.perquisites} onChange={(v: any) => updateEntry(e.id, { perquisites: v })} /></F>
              <F label="Profits in Lieu"><Inp type="number" value={e.profitsInLieu} onChange={(v: any) => updateEntry(e.id, { profitsInLieu: v })} /></F>
              <F label="Other Allowance"><Inp type="number" value={e.otherAllowance} onChange={(v: any) => updateEntry(e.id, { otherAllowance: v })} /></F>
            </div>
          </Section>

          <Section title="HRA Exemption" expanded={expandedId === `hra-${e.id}`} onClick={() => toggleExpand(`hra-${e.id}`)} badge={taxRegime}>
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

          <Section title="Retirement & VRS" expanded={expandedId === `ret-${e.id}`} onClick={() => toggleExpand(`ret-${e.id}`)} badge="">
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12 }}>
              <F label="Commuted Pension" hint="u/s 10(10A)"><Inp type="number" value={e.commutedPension} onChange={(v: any) => updateEntry(e.id, { commutedPension: v })} /></F>
              <F label="Gratuity (max ₹20L)" hint="u/s 10(10)"><Inp type="number" value={e.gratuity} onChange={(v: any) => updateEntry(e.id, { gratuity: v })} /></F>
              <F label="Leave Encash (max ₹25L)" hint="u/s 10(10AA)"><Inp type="number" value={e.leaveEncashment} onChange={(v: any) => updateEntry(e.id, { leaveEncashment: v })} /></F>
              <F label="VRS Compensation (max ₹5L)" hint="u/s 10(10C)"><Inp type="number" value={e.vrsCompensation} onChange={(v: any) => updateEntry(e.id, { vrsCompensation: v })} /></F>
              <F label="Retrenchment (max ₹5L)" hint="u/s 10(10B)"><Inp type="number" value={e.retrenchmentCompensation} onChange={(v: any) => updateEntry(e.id, { retrenchmentCompensation: v })} /></F>
            </div>
          </Section>

          <Section title="Allowances & Exemptions" expanded={expandedId === `all-${e.id}`} onClick={() => toggleExpand(`all-${e.id}`)} badge="">
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12 }}>
              <F label="Transport (max ₹19,200)" hint="u/s 10(14)"><Inp type="number" value={e.transportAllowance} onChange={(v: any) => updateEntry(e.id, { transportAllowance: v })} /></F>
              <F label="Children Edu (max ₹2,400)" hint="u/s 10(14)"><Inp type="number" value={e.childrenEducationAllowance} onChange={(v: any) => updateEntry(e.id, { childrenEducationAllowance: v })} /></F>
              <F label="Hostel (max ₹7,200)" hint="u/s 10(14)"><Inp type="number" value={e.hostelExpenditureAllowance} onChange={(v: any) => updateEntry(e.id, { hostelExpenditureAllowance: v })} /></F>
              <F label="Entertainment (Govt only, max ₹5,000)" hint="u/s 16(ii)"><Inp type="number" value={e.entertainmentAllowance} onChange={(v: any) => updateEntry(e.id, { entertainmentAllowance: v })} /></F>
              <F label="LTA Exempt Claimed" hint="u/s 10(5)"><Inp type="number" value={e.ltaExempt} onChange={(v: any) => updateEntry(e.id, { ltaExempt: v })} /></F>
              <F label="Other Exemptions" hint="Schedule EI"><Inp type="number" value={e.otherExempt} onChange={(v: any) => updateEntry(e.id, { otherExempt: v })} /></F>
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
            <div>
              <div style={{ fontSize: 12, opacity: 0.7 }}>
                {taxRegime === 'NEW' ? 'STD DEDUCTION u/s 16(ia)' : 'EXEMPTIONS u/s 10 & 16'}
              </div>
              <div style={{ fontSize: 24, fontWeight: 700, color: '#4ade80' }}>₹{formatINR(totalExemptions())}</div>
            </div>
            <div><div style={{ fontSize: 12, opacity: 0.7 }}>TAXABLE</div><div style={{ fontSize: 24, fontWeight: 700, color: '#fbbf24' }}>₹{formatINR(Math.max(0, totalGross() - totalExemptions()))}</div></div>
            <div><div style={{ fontSize: 12, opacity: 0.7 }}>TDS</div><div style={{ fontSize: 24, fontWeight: 700 }}>₹{formatINR(totalTDS())}</div></div>
          </div>
          <div style={{ fontSize: 11, marginTop: 8, textAlign: 'center', opacity: 0.7 }}>
            Final tax computation in Tax Computation tab
          </div>
        </div>
      )}
    </div>
  );
}
