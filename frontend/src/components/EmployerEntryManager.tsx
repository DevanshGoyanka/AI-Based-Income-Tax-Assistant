import React, { useState, useRef, useEffect, useCallback } from 'react';
import { calculateSalary, type EmployerInput, type SalaryCalculationResponse } from '../services/salaryCalculationService';

interface EmployerEntry {
  id: string;
  customEmployerName?: string;
  employerName: string;
  employerTAN: string;
  natureOfEmployment?: string;
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

const genId = () => Math.random().toString(36).slice(2);

export const EmployerEntryManager: React.FC<Props> = ({ entries, onChange, assessmentYear, taxRegime = 'OLD' }) => {
  // LOCAL copy for zero-lag typing — parent is NOT notified until blur or Calculate
  const [local, setLocal] = useState<EmployerEntry[]>(entries);
  const [result, setResult] = useState<SalaryCalculationResponse | null>(null);
  const [expanded, setExpanded] = useState<Record<string, boolean>>({ employer: true, salary: true, hra: false, retirement: false, allowances: false, deductions: false });
  const [calcKey, setCalcKey] = useState(0);

  // Sync from parent when entries change externally
  useEffect(() => { setLocal(entries); }, [entries]);

  const toggle = (s: string) => setExpanded(p => ({ ...p, [s]: !p[s] }));

  const addEntry = () => {
    const e: EmployerEntry = {
      id: genId(), employerName: '', employerTAN: '', customEmployerName: `Employer ${local.length + 1}`,
      natureOfEmployment: 'NGOV', basic: 0, da: 0, hra: 0, bonus: 0, allowances: 0, lta: 0,
      rentPaid: 0, isMetroCity: false, commutedPension: 0, gratuity: 0, leaveEncashment: 0,
      isGovernmentEmployee: false, isDisabledEmployee: false, childrenEducationAllowance: 0,
      hostalExpenditureAllowance: 0, professionalTax: 0, tdsDeducted: 0
    };
    const n = [...local, e];
    setLocal(n);
    onChange(n);
  };

  // Update LOCAL state only — no parent re-render
  const updateField = (id: string, field: string, value: any) => {
    setLocal(prev => prev.map(e => e.id === id ? { ...e, [field]: value } : e));
  };

  // On blur: sync to parent
  const blurField = () => {
    onChange(local);
  };

  const removeEntry = (id: string) => {
    const n = local.filter(e => e.id !== id);
    setLocal(n);
    onChange(n);
    setResult(null);
  };

  // Calculate button
  const doCalculate = async () => {
    const hasData = local.some(e => (e.basic || e.hra || e.bonus) > 0);
    if (!hasData) return;
    try {
      const inputs: EmployerInput[] = local.map(e => ({
        employerName: e.employerName || 'E', employerTAN: e.employerTAN || '',
        basic: e.basic || 0, da: e.da || 0, hra: e.hra || 0, bonus: e.bonus || 0,
        allowances: e.allowances || 0, lta: e.lta || 0, rentPaid: e.rentPaid, isMetroCity: e.isMetroCity,
        pension: 0, commutedPension: e.commutedPension || 0, gratuity: e.gratuity || 0,
        leaveEncashment: e.leaveEncashment || 0, professionalTax: e.professionalTax || 0,
        entertainmentAllowance: 0, tdsDeducted: e.tdsDeducted || 0,
        isDisabledEmployee: e.isDisabledEmployee, isGovernmentEmployee: e.isGovernmentEmployee,
        childrenEducationAllowance: e.childrenEducationAllowance, hostalExpenditureAllowance: e.hostalExpenditureAllowance,
      }));
      const res = await calculateSalary(assessmentYear, inputs, taxRegime);
      setResult(res);
      // Merge results into local
      setLocal(prev => prev.map(e => ({
        ...e,
        hraExempt: res.hraExempt || 0,
        ltaExempt: res.ltaExempt || 0,
        gratuityExempt: res.gratuityExempt || 0,
        leaveEncashmentExempt: res.leaveEncashmentExempt || 0,
      })));
      onChange(local.map(e => ({ ...e,
        hraExempt: res.hraExempt || 0, ltaExempt: res.ltaExempt || 0,
        gratuityExempt: res.gratuityExempt || 0, leaveEncashmentExempt: res.leaveEncashmentExempt || 0,
      })));
      setCalcKey(k => k + 1);
    } catch (err) { console.log('Calc err:', err); }
  };

  const getGross = (e: EmployerEntry) => result?.grossSalary || ((e.basic||0)+(e.da||0)+(e.hra||0)+(e.bonus||0)+(e.allowances||0)+(e.lta||0));
  const getExempt = (e: EmployerEntry) => (e.hraExempt||0)+(e.ltaExempt||0)+(e.gratuityExempt||0)+(e.leaveEncashmentExempt||0);
  const getNet = (e: EmployerEntry) => result?.netTaxableSalary || (getGross(e)-getExempt(e));
  const totGross = () => result?.grossSalary || local.reduce((s,e)=>s+getGross(e),0);
  const totNet = () => result?.netTaxableSalary || local.reduce((s,e)=>s+getNet(e),0);
  const totTDS = () => local.reduce((s,e)=>s+(e.tdsDeducted||0),0);

  const Sec = ({ title, sec, badge, children }: any) => (
    <div style={{ marginBottom:8, border:'1px solid #e2e8f0', borderRadius:8, overflow:'hidden' }}>
      <button onClick={()=>toggle(sec)} style={{ width:'100%', padding:12, display:'flex', justifyContent:'space-between', background:expanded[sec]?'#fef3e2':'#f8fafc', border:'none', cursor:'pointer' }}>
        <span style={{ fontSize:13, fontWeight:600, color:'#475569' }}>{title}</span>
        <div style={{ display:'flex', alignItems:'center', gap:8 }}>
          {badge&&<span style={{ fontSize:11, background:'#e2e8f0', padding:'2px 8px', borderRadius:4 }}>{badge}</span>}
          <span>{expanded[sec]?'▲':'▼'}</span>
        </div>
      </button>
      {expanded[sec]&&<div style={{ padding:16, background:'white' }}>{children}</div>}
    </div>
  );

  const Field = ({ label, hint, children }: any) => (
    <div><label style={{ display:'block', marginBottom:4, fontSize:12, fontWeight:500, color:'#64748b' }}>{label}</label>{children}{hint&&<div style={{ fontSize:10, color:'#94a3b8' }}>{hint}</div>}</div>
  );

  return (
    <div style={{ marginBottom:24 }}>
      <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:16 }}>
        <h3 style={{ margin:0, fontSize:14, fontWeight:600, color:'#64748b' }}>Salary Details</h3>
        <div style={{ display:'flex', gap:8 }}>
          <button onClick={doCalculate} style={{ padding:'8px 20px', background:'#16a34a', color:'white', border:'none', borderRadius:6, fontSize:13, fontWeight:600, cursor:'pointer' }}>Calculate</button>
          <button onClick={addEntry} style={{ padding:'8px 16px', background:'#c9943a', color:'white', border:'none', borderRadius:6, fontSize:13, cursor:'pointer' }}>+ Add</button>
        </div>
      </div>

      {local.length===0 ? (
        <div style={{ textAlign:'center', padding:40, color:'#94a3b8', background:'#f8fafc', borderRadius:8, border:'1px dashed #cbd5e1' }}>Click "Add" then enter values, then "Calculate"</div>
      ) : local.map(e => (
        <div key={e.id} style={{ background:'white', border:'1px solid #e2e8f0', borderRadius:12, padding:20, marginBottom:16 }}>
          <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:16, paddingBottom:16, borderBottom:'1px solid #f1f5f9' }}>
            <input type="text" value={e.customEmployerName||'Employer'} onChange={(ev:any)=>updateField(e.id,'customEmployerName',ev.target.value)} onBlur={blurField}
              style={{ border:'none', borderBottom:'1px dashed #c9943a', background:'transparent', fontSize:14, fontWeight:600, minWidth:150, outline:'none' }} />
            <button onClick={()=>removeEntry(e.id)} style={{ background:'#fef2f2', color:'#ef4444', border:'none', width:28, height:28, borderRadius:'50%', cursor:'pointer' }}>×</button>
          </div>

          <div style={{ padding:16, background:'linear-gradient(135deg,#fef3e2,#fff7ed)', borderRadius:8, marginBottom:16, border:'1px solid #fed7aa' }}>
            <div style={{ display:'grid', gridTemplateColumns:'repeat(4,1fr)', gap:16 }}>
              <div><div style={{ fontSize:11, color:'#78716c' }}>Gross</div><div style={{ fontSize:16, fontWeight:700 }}>₹{formatINR(getGross(e))}</div></div>
              <div><div style={{ fontSize:11, color:'#78716c' }}>Exempt</div><div style={{ fontSize:16, fontWeight:700, color:'#16a34a' }}>-₹{formatINR(getExempt(e))}</div></div>
              <div><div style={{ fontSize:11, color:'#78716c' }}>Taxable</div><div style={{ fontSize:16, fontWeight:700, color:'#c9943a' }}>₹{formatINR(getNet(e))}</div></div>
              <div><div style={{ fontSize:11, color:'#78716c' }}>TDS</div><div style={{ fontSize:16, fontWeight:700 }}>₹{formatINR(e.tdsDeducted)}</div></div>
            </div>
          </div>

          <Sec title="Employer Details" sec="employer" badge={e.employerName||'Req'}>
            <div style={{ display:'grid', gridTemplateColumns:'repeat(3,1fr)', gap:12 }}>
              <Field label="Name"><input type="text" value={e.employerName} onChange={(ev:any)=>updateField(e.id,'employerName',ev.target.value)} onBlur={blurField} style={{ width:'100%', padding:'8px 10px', border:'1px solid #e2e8f0', borderRadius:6, fontSize:13 }} /></Field>
              <Field label="TAN"><input type="text" value={e.employerTAN} onChange={(ev:any)=>updateField(e.id,'employerTAN',ev.target.value.toUpperCase())} onBlur={blurField} style={{ width:'100%', padding:'8px 10px', border:'1px solid #e2e8f0', borderRadius:6, fontSize:13 }} /></Field>
              <Field label="Nature">
                <select value={e.natureOfEmployment||'NGOV'} onChange={(ev:any)=>updateField(e.id,'natureOfEmployment',ev.target.value)} onBlur={blurField} style={{ width:'100%', padding:'8px 10px', border:'1px solid #e2e8f0', borderRadius:6, fontSize:13 }}>
                  <option value="NGOV">Private</option><option value="GOV">Govt</option><option value="PSU">PSU</option>
                </select>
              </Field>
            </div>
          </Sec>

          <Sec title="Salary" sec="salary" badge="">
            <div style={{ display:'grid', gridTemplateColumns:'repeat(4,1fr)', gap:12 }}>
              <Field label="Basic"><input type="number" value={e.basic||''} onChange={(ev:any)=>updateField(e.id,'basic',parseInt(ev.target.value)||0)} onBlur={blurField} style={{ width:'100%', padding:'8px 10px', border:'1px solid #e2e8f0', borderRadius:6, fontSize:13 }} /></Field>
              <Field label="DA"><input type="number" value={e.da||''} onChange={(ev:any)=>updateField(e.id,'da',parseInt(ev.target.value)||0)} onBlur={blurField} style={{ width:'100%', padding:'8px 10px', border:'1px solid #e2e8f0', borderRadius:6, fontSize:13 }} /></Field>
              <Field label="HRA"><input type="number" value={e.hra||''} onChange={(ev:any)=>updateField(e.id,'hra',parseInt(ev.target.value)||0)} onBlur={blurField} style={{ width:'100%', padding:'8px 10px', border:'1px solid #e2e8f0', borderRadius:6, fontSize:13 }} /></Field>
              <Field label="Bonus"><input type="number" value={e.bonus||''} onChange={(ev:any)=>updateField(e.id,'bonus',parseInt(ev.target.value)||0)} onBlur={blurField} style={{ width:'100%', padding:'8px 10px', border:'1px solid #e2e8f0', borderRadius:6, fontSize:13 }} /></Field>
              <Field label="Allowances"><input type="number" value={e.allowances||''} onChange={(ev:any)=>updateField(e.id,'allowances',parseInt(ev.target.value)||0)} onBlur={blurField} style={{ width:'100%', padding:'8px 10px', border:'1px solid #e2e8f0', borderRadius:6, fontSize:13 }} /></Field>
              <Field label="LTA"><input type="number" value={e.lta||''} onChange={(ev:any)=>updateField(e.id,'lta',parseInt(ev.target.value)||0)} onBlur={blurField} style={{ width:'100%', padding:'8px 10px', border:'1px solid #e2e8f0', borderRadius:6, fontSize:13 }} /></Field>
            </div>
          </Sec>

          <Sec title="HRA Exemption" sec="hra" badge={e.hraExempt?`₹${formatINR(e.hraExempt)}`:''}>
            <div style={{ display:'grid', gridTemplateColumns:'repeat(3,1fr)', gap:12 }}>
              <Field label="Annual Rent"><input type="number" value={e.rentPaid||''} onChange={(ev:any)=>updateField(e.id,'rentPaid',parseInt(ev.target.value)||0)} onBlur={blurField} style={{ width:'100%', padding:'8px 10px', border:'1px solid #e2e8f0', borderRadius:6, fontSize:13 }} /></Field>
              <Field label="Metro">
                <select value={e.isMetroCity?'yes':'no'} onChange={(ev:any)=>updateField(e.id,'isMetroCity',ev.target.value==='yes')} onBlur={blurField} style={{ width:'100%', padding:'8px 10px', border:'1px solid #e2e8f0', borderRadius:6, fontSize:13 }}>
                  <option value="no">No (40%)</option><option value="yes">Yes (50%)</option>
                </select>
              </Field>
              <div style={{marginTop:20}}><input type="checkbox" checked={e.isGovernmentEmployee||false} onChange={(ev:any)=>{updateField(e.id,'isGovernmentEmployee',ev.target.checked);blurField();}} /><span style={{fontSize:12, marginLeft:6}}>Govt</span></div>
            </div>
          </Sec>

          <Sec title="Retirement Benefits" sec="retirement" badge="">
            <div style={{ display:'grid', gridTemplateColumns:'repeat(3,1fr)', gap:12 }}>
              <Field label="Commuted Pension"><input type="number" value={e.commutedPension||''} onChange={(ev:any)=>updateField(e.id,'commutedPension',parseInt(ev.target.value)||0)} onBlur={blurField} style={{ width:'100%', padding:'8px 10px', border:'1px solid #e2e8f0', borderRadius:6, fontSize:13 }} /></Field>
              <Field label="Gratuity"><input type="number" value={e.gratuity||''} onChange={(ev:any)=>updateField(e.id,'gratuity',parseInt(ev.target.value)||0)} onBlur={blurField} style={{ width:'100%', padding:'8px 10px', border:'1px solid #e2e8f0', borderRadius:6, fontSize:13 }} /></Field>
              <Field label="Leave Encash"><input type="number" value={e.leaveEncashment||''} onChange={(ev:any)=>updateField(e.id,'leaveEncashment',parseInt(ev.target.value)||0)} onBlur={blurField} style={{ width:'100%', padding:'8px 10px', border:'1px solid #e2e8f0', borderRadius:6, fontSize:13 }} /></Field>
            </div>
          </Sec>

          <Sec title="Special Allowances" sec="allowances" badge="">
            <div style={{ display:'grid', gridTemplateColumns:'repeat(3,1fr)', gap:12 }}>
              <Field label="Transport"><input type="checkbox" checked={e.isDisabledEmployee||false} onChange={(ev:any)=>{updateField(e.id,'isDisabledEmployee',ev.target.checked);blurField();}} /><span style={{fontSize:11,marginLeft:4}}>Disabled</span></Field>
              <Field label="Children"><input type="number" value={e.childrenEducationAllowance||''} onChange={(ev:any)=>updateField(e.id,'childrenEducationAllowance',parseInt(ev.target.value)||0)} onBlur={blurField} style={{ width:'100%', padding:'8px 10px', border:'1px solid #e2e8f0', borderRadius:6, fontSize:13 }} /></Field>
              <Field label="Hostel"><input type="number" value={e.hostalExpenditureAllowance||''} onChange={(ev:any)=>updateField(e.id,'hostalExpenditureAllowance',parseInt(ev.target.value)||0)} onBlur={blurField} style={{ width:'100%', padding:'8px 10px', border:'1px solid #e2e8f0', borderRadius:6, fontSize:13 }} /></Field>
            </div>
          </Sec>

          <Sec title="Deductions & TDS" sec="deductions" badge="">
            <div style={{ display:'grid', gridTemplateColumns:'repeat(2,1fr)', gap:12 }}>
              <Field label="Professional Tax"><input type="number" value={e.professionalTax||''} onChange={(ev:any)=>updateField(e.id,'professionalTax',parseInt(ev.target.value)||0)} onBlur={blurField} style={{ width:'100%', padding:'8px 10px', border:'1px solid #e2e8f0', borderRadius:6, fontSize:13 }} /></Field>
              <Field label="TDS"><input type="number" value={e.tdsDeducted||''} onChange={(ev:any)=>updateField(e.id,'tdsDeducted',parseInt(ev.target.value)||0)} onBlur={blurField} style={{ width:'100%', padding:'8px 10px', border:'1px solid #e2e8f0', borderRadius:6, fontSize:13 }} /></Field>
            </div>
          </Sec>
        </div>
      ))}

      {local.length>0 && (
        <div style={{ padding:20, background:'linear-gradient(135deg,#1e293b,#334155)', borderRadius:12, color:'white' }}>
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
