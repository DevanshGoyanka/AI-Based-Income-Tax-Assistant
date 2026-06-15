import React from 'react';
import { calculateSalary, type EmployerInput, type SalaryCalculationResponse } from '../services/salaryCalculationService';

interface EmployerEntry {
  customEmployerName?: string;
  employerName: string;
  employerTAN: string;
  employerPAN?: string;
  natureOfEmployment?: 'GOV' | 'NGOV' | 'PSU' | 'PENSIONER' | 'NA';
  basic: number;
  da: number;
  hra: number;
  bonus: number;
  allowances: number;
  lta: number;
  rentPaid?: number;
  isMetroCity?: boolean;
  commutedPension: number;
  gratuity: number;
  leaveEncashment: number;
  isGovernmentEmployee?: boolean;
  isDisabledEmployee?: boolean;
  childrenEducationAllowance?: number;
  hostalExpenditureAllowance?: number;
  professionalTax: number;
  tdsDeducted: number;
  grossSalary: number;
  netSalary: number;
  hraExempt?: number;
  ltaExempt?: number;
  gratuityExempt?: number;
  leaveEncashmentExempt?: number;
}

interface Props {
  entries: EmployerEntry[];
  onChange: (entries: EmployerEntry[]) => void;
  assessmentYear: string;
  taxRegime?: string;
}

const formatINR = (num: number | undefined | null): string => {
  if (!num || num === 0) return '0';
  return Math.round(num).toLocaleString('en-IN');
};

export const EmployerEntryManager: React.FC<Props> = ({ entries, onChange, assessmentYear, taxRegime = 'OLD' }) => {
  const [result, setResult] = React.useState<SalaryCalculationResponse | null>(null);
  const [expanded, setExpanded] = React.useState<{[key: string]: boolean}>({
    employer: true, salary: true, hra: false, retirement: false, allowances: false, deductions: false
  });
  const calcTimer = React.useRef<any>(null);

  const toggle = (s: string) => setExpanded(p => ({ ...p, [s]: !p[s] }));

  const addEntry = () => {
    const e: EmployerEntry = {
      employerName: '', employerTAN: '', customEmployerName: `Employer ${entries.length + 1}`,
      natureOfEmployment: 'NGOV', basic: 0, da: 0, hra: 0, bonus: 0, allowances: 0, lta: 0,
      rentPaid: 0, isMetroCity: false, commutedPension: 0, gratuity: 0, leaveEncashment: 0,
      isGovernmentEmployee: false, isDisabledEmployee: false, childrenEducationAllowance: 0,
      hostalExpenditureAllowance: 0, professionalTax: 0, tdsDeducted: 0, grossSalary: 0, netSalary: 0
    };
    onChange([...entries, e]);
  };

  const removeEntry = (idx: number) => { onChange(entries.filter((_, i) => i !== idx)); setResult(null); };

  const updateEntry = (idx: number, field: string, value: any) => {
    const updated = entries.map((e, i) => i === idx ? { ...e, [field]: value } : e);
    onChange(updated);
    // Fire and forget calculation - no debounce
    doCalculate(updated);
  };

  const doCalculate = async (data: EmployerEntry[]) => {
    const hasData = data.some(e => (e.basic || 0) > 0 || (e.hra || 0) > 0);
    if (!hasData) { setResult(null); return; }

    try {
      const inputs: EmployerInput[] = data.map(e => ({
        employerName: e.employerName || 'E', employerTAN: e.employerTAN || '',
        basic: e.basic || 0, da: e.da || 0, hra: e.hra || 0,
        bonus: e.bonus || 0, allowances: e.allowances || 0, lta: e.lta || 0,
        rentPaid: e.rentPaid, isMetroCity: e.isMetroCity,
        pension: 0, commutedPension: e.commutedPension || 0,
        gratuity: e.gratuity || 0, leaveEncashment: e.leaveEncashment || 0,
        professionalTax: e.professionalTax || 0, entertainmentAllowance: 0,
        tdsDeducted: e.tdsDeducted || 0,
        isDisabledEmployee: e.isDisabledEmployee,
        isGovernmentEmployee: e.isGovernmentEmployee,
        childrenEducationAllowance: e.childrenEducationAllowance,
        hostalExpenditureAllowance: e.hostalExpenditureAllowance,
      }));
      const res = await calculateSalary(assessmentYear, inputs, taxRegime);
      setResult(res);
    } catch (err) { console.error('Calc fail:', err); }
  };

  const getGross = (e: EmployerEntry) => result?.grossSalary || 
    (e.basic||0)+(e.da||0)+(e.hra||0)+(e.bonus||0)+(e.allowances||0)+(e.lta||0);
  const getExempt = (e: EmployerEntry) => (e.hraExempt||0)+(e.ltaExempt||0)+(e.gratuityExempt||0)+(e.leaveEncashmentExempt||0);
  const getNet = (e: EmployerEntry) => result?.netTaxableSalary || (getGross(e) - getExempt(e));
  const totGross = () => result?.grossSalary || entries.reduce((s,e)=>s+getGross(e),0);
  const totNet = () => result?.netTaxableSalary || entries.reduce((s,e)=>s+getNet(e),0);
  const totTDS = () => entries.reduce((s,e)=>s+(e.tdsDeducted||0),0);

  const C = ({ title, sec, badge, children }: any) => (
    <div style={{ marginBottom:8, border:'1px solid #e2e8f0', borderRadius:8, overflow:'hidden' }}>
      <button onClick={()=>toggle(sec)} style={{ width:'100%', padding:'12px 16px', display:'flex',
        justifyContent:'space-between', alignItems:'center', background:expanded[sec]?'#fef3e2':'#f8fafc',
        border:'none', cursor:'pointer', textAlign:'left' }}>
        <span style={{ fontSize:13, fontWeight:600, color:'#475569' }}>{title}</span>
        <div style={{ display:'flex', alignItems:'center', gap:8 }}>
          {badge&&<span style={{ fontSize:11, background:'#e2e8f0', padding:'2px 8px', borderRadius:4 }}>{badge}</span>}
          <span style={{ color:'#94a3b8' }}>{expanded[sec]?'▲':'▼'}</span>
        </div>
      </button>
      {expanded[sec]&&<div style={{ padding:16, background:'white' }}>{children}</div>}
    </div>
  );

  const F = ({ label, hint, children }: any) => (
    <div><label style={{ display:'block', marginBottom:4, fontSize:12, fontWeight:500, color:'#64748b' }}>{label}</label>
    {children}{hint&&<div style={{ fontSize:10, color:'#94a3b8', marginTop:2 }}>{hint}</div>}</div>
  );

  const Inp = (p: any) => (
    <input type={p.type||'text'} {...p} value={p.value??''}
      style={{ width:'100%', padding:'8px 10px', border:'1px solid #e2e8f0', borderRadius:6, fontSize:13, ...(p.style||{}) }} />
  );

  return (
    <div style={{ marginBottom:24 }}>
      <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:16 }}>
        <h3 style={{ margin:0, fontSize:14, fontWeight:600, color:'#64748b' }}>Salary Details</h3>
        <button onClick={addEntry} style={{ padding:'8px 16px', background:'#c9943a', color:'white', border:'none', borderRadius:6, fontSize:13, fontWeight:500, cursor:'pointer' }}>+ Add Employer</button>
      </div>

      {entries.length===0 ? (
        <div style={{ textAlign:'center', padding:40, color:'#94a3b8', background:'#f8fafc', borderRadius:8, border:'1px dashed #cbd5e1' }}>
          Click "Add Employer" to enter salary details
        </div>
      ) : entries.map((e, idx) => (
        <div key={idx} style={{ background:'white', border:'1px solid #e2e8f0', borderRadius:12, padding:20, marginBottom:16 }}>
          <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:16, paddingBottom:16, borderBottom:'1px solid #f1f5f9' }}>
            <div style={{ display:'flex', alignItems:'center', gap:12 }}>
              <span style={{ background:'#c9943a', color:'white', padding:'4px 12px', borderRadius:6, fontSize:12, fontWeight:600 }}>#{idx+1}</span>
              <input type="text" value={e.customEmployerName||`Employer ${idx+1}`}
                onChange={(ev)=>updateEntry(idx,'customEmployerName',ev.target.value)}
                style={{ border:'none', borderBottom:'1px dashed #c9943a', background:'transparent', fontSize:14, fontWeight:600, color:'#1e293b', outline:'none', minWidth:150 }} />
            </div>
            <button onClick={()=>removeEntry(idx)} style={{ background:'#fef2f2', color:'#ef4444', border:'none', width:28, height:28, borderRadius:'50%', cursor:'pointer', fontSize:16 }}>×</button>
          </div>

          <div style={{ padding:16, background:'linear-gradient(135deg,#fef3e2 0%,#fff7ed 100%)', borderRadius:8, marginBottom:16, border:'1px solid #fed7aa' }}>
            <div style={{ display:'grid', gridTemplateColumns:'repeat(4,1fr)', gap:16 }}>
              <div><div style={{ fontSize:11, color:'#78716c', marginBottom:4 }}>Gross</div>
                <div style={{ fontSize:16, fontWeight:700, color:'#1e293b' }}>₹{formatINR(getGross(e))}</div></div>
              <div><div style={{ fontSize:11, color:'#78716c', marginBottom:4 }}>Exemptions</div>
                <div style={{ fontSize:16, fontWeight:700, color:'#16a34a' }}>- ₹{formatINR(getExempt(e))}</div></div>
              <div><div style={{ fontSize:11, color:'#78716c', marginBottom:4 }}>Taxable</div>
                <div style={{ fontSize:16, fontWeight:700, color:'#c9943a' }}>₹{formatINR(getNet(e))}</div></div>
              <div><div style={{ fontSize:11, color:'#78716c', marginBottom:4 }}>TDS</div>
                <div style={{ fontSize:16, fontWeight:700, color:'#1e293b' }}>₹{formatINR(e.tdsDeducted)}</div></div>
            </div>
          </div>

          <C title="Employer Details" sec="employer" badge={e.employerName||'Required'}>
            <div style={{ display:'grid', gridTemplateColumns:'repeat(3,1fr)', gap:12 }}>
              <F label="Employer Name"><Inp value={e.employerName} onChange={(ev:any)=>updateEntry(idx,'employerName',ev.target.value)} /></F>
              <F label="TAN"><Inp value={e.employerTAN} onChange={(ev:any)=>updateEntry(idx,'employerTAN',ev.target.value.toUpperCase())} maxLength={10} /></F>
              <F label="Nature">
                <select value={e.natureOfEmployment||'NGOV'} onChange={(ev:any)=>updateEntry(idx,'natureOfEmployment',ev.target.value)}
                  style={{ width:'100%', padding:'8px 10px', border:'1px solid #e2e8f0', borderRadius:6, fontSize:13 }}>
                  <option value="NGOV">Private</option><option value="GOV">Government</option><option value="PSU">PSU</option>
                </select>
              </F>
            </div>
          </C>

          <C title="Salary Components" sec="salary" badge="">
            <div style={{ display:'grid', gridTemplateColumns:'repeat(4,1fr)', gap:12 }}>
              <F label="Basic Salary"><Inp type="number" value={e.basic} placeholder="0" onChange={(ev:any)=>updateEntry(idx,'basic',parseInt(ev.target.value)||0)} /></F>
              <F label="DA"><Inp type="number" value={e.da} onChange={(ev:any)=>updateEntry(idx,'da',parseInt(ev.target.value)||0)} /></F>
              <F label="HRA"><Inp type="number" value={e.hra} onChange={(ev:any)=>updateEntry(idx,'hra',parseInt(ev.target.value)||0)} /></F>
              <F label="Bonus"><Inp type="number" value={e.bonus} onChange={(ev:any)=>updateEntry(idx,'bonus',parseInt(ev.target.value)||0)} /></F>
              <F label="Other Allowances"><Inp type="number" value={e.allowances} onChange={(ev:any)=>updateEntry(idx,'allowances',parseInt(ev.target.value)||0)} /></F>
              <F label="LTA"><Inp type="number" value={e.lta} onChange={(ev:any)=>updateEntry(idx,'lta',parseInt(ev.target.value)||0)} /></F>
              <F label="Arrears"><Inp type="number" value={0} disabled /></F>
              <F label="Perquisites"><Inp type="number" value={0} disabled /></F>
            </div>
          </C>

          <C title="HRA Exemption" sec="hra" badge={e.hraExempt?`₹${formatINR(e.hraExempt)}`:'Optional'}>
            <div style={{ display:'grid', gridTemplateColumns:'repeat(3,1fr)', gap:12 }}>
              <F label="Annual Rent Paid"><Inp type="number" value={e.rentPaid} onChange={(ev:any)=>updateEntry(idx,'rentPaid',parseInt(ev.target.value)||0)} hint="Required for exemption" /></F>
              <F label="Metro City">
                <select value={e.isMetroCity?'yes':'no'} onChange={(ev:any)=>updateEntry(idx,'isMetroCity',ev.target.value==='yes')}
                  style={{ width:'100%', padding:'8px 10px', border:'1px solid #e2e8f0', borderRadius:6, fontSize:13 }}>
                  <option value="no">No (40%)</option><option value="yes">Yes (50%)</option>
                </select>
              </F>
              <div style={{ display:'flex', alignItems:'center', gap:8, marginTop:20 }}>
                <input type="checkbox" checked={e.isGovernmentEmployee||false} onChange={(ev:any)=>updateEntry(idx,'isGovernmentEmployee',ev.target.checked)} />
                <span style={{ fontSize:12, color:'#64748b' }}>Govt Employee</span>
              </div>
            </div>
          </C>

          <C title="Retirement Benefits" sec="retirement" badge={(e.gratuity||e.leaveEncashment)?'Entered':'Optional'}>
            <div style={{ display:'grid', gridTemplateColumns:'repeat(4,1fr)', gap:12 }}>
              <F label="Commuted Pension"><Inp type="number" value={e.commutedPension} onChange={(ev:any)=>updateEntry(idx,'commutedPension',parseInt(ev.target.value)||0)} /></F>
              <F label="Gratuity"><Inp type="number" value={e.gratuity} onChange={(ev:any)=>updateEntry(idx,'gratuity',parseInt(ev.target.value)||0)} /></F>
              <F label="Leave Encashment"><Inp type="number" value={e.leaveEncashment} onChange={(ev:any)=>updateEntry(idx,'leaveEncashment',parseInt(ev.target.value)||0)} /></F>
            </div>
          </C>

          <C title="Special Allowances" sec="allowances" badge="">
            <div style={{ display:'grid', gridTemplateColumns:'repeat(3,1fr)', gap:12 }}>
              <F label="Transport (Disabled)">
                <div style={{ display:'flex', alignItems:'center', gap:8 }}><input type="checkbox" checked={e.isDisabledEmployee||false} onChange={(ev:any)=>updateEntry(idx,'isDisabledEmployee',ev.target.checked)} /><span style={{ fontSize:11 }}>Disabled</span></div>
              </F>
              <F label="Children Education"><Inp type="number" value={e.childrenEducationAllowance} onChange={(ev:any)=>updateEntry(idx,'childrenEducationAllowance',parseInt(ev.target.value)||0)} /></F>
              <F label="Hostel"><Inp type="number" value={e.hostalExpenditureAllowance} onChange={(ev:any)=>updateEntry(idx,'hostalExpenditureAllowance',parseInt(ev.target.value)||0)} /></F>
            </div>
          </C>

          <C title="Deductions & TDS" sec="deductions" badge="">
            <div style={{ display:'grid', gridTemplateColumns:'repeat(2,1fr)', gap:12 }}>
              <F label="Professional Tax"><Inp type="number" value={e.professionalTax} onChange={(ev:any)=>updateEntry(idx,'professionalTax',parseInt(ev.target.value)||0)} /></F>
              <F label="TDS Deducted"><Inp type="number" value={e.tdsDeducted} onChange={(ev:any)=>updateEntry(idx,'tdsDeducted',parseInt(ev.target.value)||0)} /></F>
            </div>
          </C>
        </div>
      ))}

      {entries.length>0 && (
        <div style={{ padding:20, background:'linear-gradient(135deg,#1e293b 0%,#334155 100%)', borderRadius:12, color:'white' }}>
          <div style={{ display:'grid', gridTemplateColumns:'repeat(3,1fr)', gap:24, textAlign:'center' }}>
            <div><div style={{ fontSize:12, opacity:0.7 }}>TOTAL GROSS</div><div style={{ fontSize:24, fontWeight:700 }}>₹{formatINR(totGross())}</div></div>
            <div><div style={{ fontSize:12, opacity:0.7 }}>TAXABLE</div><div style={{ fontSize:24, fontWeight:700, color:'#fbbf24' }}>₹{formatINR(totNet())}</div></div>
            <div><div style={{ fontSize:12, opacity:0.7 }}>TDS</div><div style={{ fontSize:24, fontWeight:700 }}>₹{formatINR(totTDS())}</div></div>
          </div>
        </div>
      )}
    </div>
  );
};
