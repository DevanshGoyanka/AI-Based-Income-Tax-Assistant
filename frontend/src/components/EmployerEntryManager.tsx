import React, { useState, useEffect, memo } from 'react';
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

const generateId = () => Math.random().toString(36).substr(2, 9);

export const EmployerEntryManager: React.FC<Props> = ({ entries, onChange, assessmentYear, taxRegime = 'OLD' }) => {
  const [result, setResult] = useState<SalaryCalculationResponse | null>(null);
  const [expanded, setExpanded] = useState<{[key: string]: boolean}>({
    employer: true, salary: true, hra: false, retirement: false, allowances: false, deductions: false
  });
  const [inpKey, setInpKey] = useState(0);

  const toggle = (s: string) => setExpanded(p => ({ ...p, [s]: !p[s] }));

  const addEntry = () => {
    const e: EmployerEntry = {
      id: generateId(),
      employerName: '', employerTAN: '', customEmployerName: `Employer ${entries.length + 1}`,
      natureOfEmployment: 'NGOV', basic: 0, da: 0, hra: 0, bonus: 0, allowances: 0, lta: 0,
      rentPaid: 0, isMetroCity: false, commutedPensation: 0, gratuity: 0, leaveEncashment: 0,
      isGovernmentEmployee: false, isDisabledEmployee: false, childrenEducationAllowance: 0,
      hostalExpenditureAllowance: 0, professionalTax: 0, tdsDeducted: 0
    };
    onChange([...entries, e]);
  };

  const removeEntry = (id: string) => { onChange(entries.filter(e => e.id !== id)); setResult(null); };

  const handleChange = (id: string, field: string, value: any) => {
    const updated = entries.map(e => e.id === id ? { ...e, [field]: value } : e);
    onChange(updated);
  };

  // Called manually from Calculate button, not on every keystroke
  const doCalculate = async () => {
    const hasData = entries.some(e => (e.basic || e.hra || e.bonus) > 0);
    if (!hasData) return;
    try {
      const inputs: EmployerInput[] = entries.map(e => ({
        employerName: e.employerName || 'E', employerTAN: e.employerTAN || '',
        basic: e.basic || 0, da: e.da || 0, hra: e.hra || 0, bonus: e.bonus || 0, 
        allowances: e.allowances || 0, lta: e.lta || 0, rentPaid: e.rentPaid, isMetroCity: e.isMetroCity,
        pension: 0, commutedPension: e.commutedPension || 0, gratuity: e.gratuity || 0,
        leaveEncashment: e.leaveEncashment || 0, professionalTax: e.professionalTax || 0,
        entertainmentAllowance: 0, tdsDeducted: e.tdsDeducted || 0,
        isDisabledEmployee: e.isDisabledEmployee, isGovernmentEmployee: e.isGovernmentEmployee,
        childrenEducationAllowance: e.childrenEducationAllowance,
        hostalExpenditureAllowance: e.hostalExpenditureAllowance,
      }));
      const res = await calculateSalary(assessmentYear, inputs, taxRegime);
      setResult(res);
      // Also update entries with results - but keep ID intact
      const updated = entries.map(e => ({
        ...e,
        hraExempt: res.hraExempt || 0,
        ltaExempt: res.ltaExempt || 0,
        gratuityExempt: res.gratuityExempt || 0,
        leaveEncashmentExempt: res.leaveEncashmentExempt || 0,
      }));
      onChange(updated);
    } catch (err) { console.log('Calc failed:', err); }
  };

  // Calc result utility - local values if no backend
  const getGross = (e: EmployerEntry) => result?.grossSalary || 
    (e.basic||0)+(e.da||0)+(e.hra||0)+(e.bonus||0)+(e.allowances||0)+(e.lta||0);
  const getExempt = (e: EmployerEntry) => (e.hraExempt||0)+(e.ltaExempt||0)+(e.gratuityExempt||0)+(e.leaveEncashmentExempt||0);
  const getNet = (e: EmployerEntry) => (result?.netTaxableSalary) || (getGross(e) - getExempt(e));
  const totalGross = () => result?.grossSalary || entries.reduce((s,e)=>s+getGross(e),0);
  const totalNet = () => result?.netTaxableSalary || entries.reduce((s,e)=>s+getNet(e),0);
  const totalTDS = () => entries.reduce((s,e)=>s+(e.tdsDeducted||0),0);

  const Sec = ({ title, sec, badge, children }: any) => (
    <div style={{ marginBottom:8, border:'1px solid #e2e8f0', borderRadius:8, overflow:'hidden' }}>
      <button onClick={()=>toggle(sec)} style={{ width:'100%', padding:12, display:'flex',
        justifyContent:'space-between', background:expanded[sec]?'#fef3e2':'#f8fafc', border:'none', cursor:'pointer' }}>
        <span style={{ fontWeight:600, color:'#475569', fontSize:13 }}>{title}</span>
        <div style={{ display:'flex', alignItems:'center', gap:8 }}>
          {badge&&<span style={{ fontSize:11, background:'#e2e8f0', padding:'2px 8px', borderRadius:4 }}>{badge}</span>}
          <span style={{ color:'#94a3b8' }}>{expanded[sec]?'▲':'▼'}</span>
        </div>
      </button>
      {expanded[sec]&&<div style={{ padding:16, background:'white' }}>{children}</div>}
    </div>
  );

  const Field = ({ label, hint, children }: any) => (
    <div><label style={{ fontSize:12, fontWeight:500, color:'#64748b', display:'block', marginBottom:4 }}>{label}</label>
    {children}{hint&&<div style={{ fontSize:10, color:'#94a3b8' }}>{hint}</div>}</div>
  );

  const Input = (p: any) => (
    <input {...p} 
      onChange={(e: any) => handleChange(p._id, p._field, p.type==='number' ? parseInt(e.target.value)||0 : e.target.value)}
      value={p.value??''}
      style={{ width:'100%', padding:'8px 10px', border:'1px solid #e2e8f0', borderRadius:6, fontSize:13 }} />
  );

  // Use memo to prevent unnecessary re-renders
  const EmployerCard = memo(({ entry }: { entry: EmployerEntry }) => (
    <div style={{ background:'white', border:'1px solid #e2e8f0', borderRadius:12, padding:20, marginBottom:16 }}>
      <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:16, borderBottom:'1px solid #f1f5f9', paddingBottom:16 }}>
        <input type="text" value={entry.customEmployerName||'Employer'}
          onChange={(e: any) => handleChange(entry.id, 'customEmployerName', e.target.value)}
          style={{ border:'none', borderBottom:'1px dashed #c9943a', background:'transparent', fontSize:14, fontWeight:600, minWidth:150, outline:'none' }} />
        <button onClick={()=>removeEntry(entry.id)} style={{ background:'#fef2f2', color:'#ef4444', border:'none', width:28, height:28, borderRadius:'50%', cursor:'pointer' }}>×</button>
      </div>

      <div style={{ padding:16, background:'linear-gradient(135deg,#fef3e2,#fff7ed)', borderRadius:8, marginBottom:16, border:'1px solid #fed7aa' }}>
        <div style={{ display:'grid', gridTemplateColumns:'repeat(4,1fr)', gap:16 }}>
          <div><div style={{ fontSize:11, color:'#78716c' }}>Gross</div><div style={{ fontSize:16, fontWeight:700 }}>₹{formatINR(getGross(entry))}</div></div>
          <div><div style={{ fontSize:11, color:'#78716c' }}>Exempt</div><div style={{ fontSize:16, fontWeight:700, color:'#16a34a' }}>-₹{formatINR(getExempt(entry))}</div></div>
          <div><div style={{ fontSize:11, color:'#78716c' }}>Taxable</div><div style={{ fontSize:16, fontWeight:700, color:'#c9943a' }}>₹{formatINR(getNet(entry))}</div></div>
          <div><div style={{ fontSize:11, color:'#78716c' }}>TDS</div><div style={{ fontSize:16, fontWeight:700 }}>₹{formatINR(entry.tdsDeducted)}</div></div>
        </div>
      </div>

      <Sec title="Employer Details" sec="employer" badge={entry.employerName||'Req'}>
        <div style={{ display:'grid', gridTemplateColumns:'repeat(3,1fr)', gap:12 }}>
          <Field label="Name"><Input _id={entry.id} _field="employerName" value={entry.employerName} /></Field>
          <Field label="TAN"><Input _id={entry.id} _field="employerTAN" value={entry.employerTAN} maxLength={10} /></Field>
          <Field label="Nature">
            <select value={entry.natureOfEmployment||'NGOV'} onChange={(e:any)=>handleChange(entry.id,'natureOfEmployment',e.target.value)}
              style={{ width:'100%', padding:'8px 10px', border:'1px solid #e2e8f0', borderRadius:6 }}>
              <option value="NGOV">Private</option><option value="GOV">Govt</option><option value="PSU">PSU</option>
            </select>
          </Field>
        </div>
      </Sec>

      <Sec title="Salary" sec="salary" badge="">
        <div style={{ display:'grid', gridTemplateColumns:'repeat(4,1fr)', gap:12 }}>
          <Field label="Basic"><Input _id={entry.id} _field="basic" type="number" value={entry.basic} /></Field>
          <Field label="DA"><Input _id={entry.id} _field="da" type="number" value={entry.da} /></Field>
          <Field label="HRA"><Input _id={entry.id} _field="hra" type="number" value={entry.hra} /></Field>
          <Field label="Bonus"><Input _id={entry.id} _field="bonus" type="number" value={entry.bonus} /></Field>
          <Field label="Allowances"><Input _id={entry.id} _field="allowances" type="number" value={entry.allowances} /></Field>
          <Field label="LTA"><Input _id={entry.id} _field="lta" type="number" value={entry.lta} /></Field>
        </div>
      </Sec>

      <Sec title="HRA Ex" sec="hra" badge={entry.hraExempt?`₹${formatINR(entry.hraExempt)}`:''}>
        <div style={{ display:'grid', gridTemplateColumns:'repeat(3,1fr)', gap:12 }}>
          <Field label="Rent Paid"><Input _id={entry.id} _field="rentPaid" type="number" value={entry.rentPaid} /></Field>
          <Field label="Metro">
            <select value={entry.isMetroCity?'yes':'no'} onChange={(e:any)=>handleChange(entry.id,'isMetroCity',e.target.value==='yes')}
              style={{ width:'100%', padding:'8px', border:'1px solid #e2e8f0', borderRadius:6 }}>
              <option value="no">No</option><option value="yes">Yes</option>
            </select>
          </Field>
          <div style={{ display:'flex', alignItems:'center', gap:8, marginTop:20 }}>
            <input type="checkbox" checked={entry.isGovernmentEmployee||false} 
              onChange={(e:any)=>handleChange(entry.id,'isGovernmentEmployee',e.target.checked)} />
            <span style={{ fontSize:12 }}>Govt Employee</span>
          </div>
        </div>
      </Sec>

      <Sec title="Retirement" sec="retirement" badge="">
        <div style={{ display:'grid', gridTemplateColumns:'repeat(3,1fr)', gap:12 }}>
          <Field label="Commuted"><Input _id={entry.id} _field="commutedPension" type="number" value={entry.commutedPension} /></Field>
          <Field label="Gratuity"><Input _id={entry.id} _field="gratuity" type="number" value={entry.gratuity} /></Field>
          <Field label="Leave"><Input _id={entry.id} _field="leaveEncashment" type="number" value={entry.leaveEncashment} /></Field>
        </div>
      </Sec>

      <Sec title="Allowances" sec="allowances" badge="">
        <div style={{ display:'grid', gridTemplateColumns:'repeat(3,1fr)', gap:12 }}>
          <Field label="Transport"><input type="checkbox" checked={entry.isDisabledEmployee||false} 
            onChange={(e:any)=>handleChange(entry.id,'isDisabledEmployee',e.target.checked)} /><span style={{fontSize:11}}> Disabled</span></Field>
          <Field label="Children"><Input _id={entry.id} _field="childrenEducationAllowance" type="number" value={entry.childrenEducationAllowance} /></Field>
          <Field label="Hostel"><Input _id={entry.id} _field="hostalExpenditureAllowance" type="number" value={entry.hostalExpenditureAllowance} /></Field>
        </div>
      </Sec>

      <Sec title="Deductions" sec="deductions" badge="">
        <div style={{ display:'grid', gridTemplateColumns:'repeat(2,1fr)', gap:12 }}>
          <Field label="Prof Tax"><Input _id={entry.id} _field="professionalTax" type="number" value={entry.professionalTax} /></Field>
          <Field label="TDS"><Input _id={entry.id} _field="tdsDeducted" type="number" value={entry.tdsDeducted} /></Field>
        </div>
      </Sec>
    </div>
  ));

  return (
    <div style={{ marginBottom:24 }}>
      <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:16 }}>
        <h3 style={{ margin:0, fontSize:14, fontWeight:600, color:'#64748b' }}>Salary Details</h3>
        <div style={{ display:'flex', gap:8 }}>
          <button onClick={doCalculate} disabled={entries.length===0}
            style={{ padding:'8px 20px', background:'#16a34a', color:'white', border:'none', borderRadius:6, fontSize:13, fontWeight:600, cursor:'pointer' }}>
            Calculate
          </button>
          <button onClick={addEntry} style={{ padding:'8px 16px', background:'#c9943a', color:'white', border:'none', borderRadius:6, fontSize:13, fontWeight:500, cursor:'pointer' }}>
            + Add
          </button>
        </div>
      </div>

      {entries.length===0 ? (
        <div style={{ textAlign:'center', padding:40, color:'#94a3b8', background:'#f8fafc', borderRadius:8 }}>
          Click "Add" then enter values, then "Calculate"
        </div>
      ) : entries.map(e => <EmployerCard key={e.id} entry={e} />)}

      {entries.length>0 && (
        <div style={{ padding:20, background:'linear-gradient(135deg,#1e293b,#334155)', borderRadius:12, color:'white'}}>
          <div style={{ display:'grid', gridTemplateColumns:'repeat(3,1fr)', gap:24, textAlign:'center' }}>
            <div><div style={{fontSize:12, opacity:0.7}}>TOTAL</div><div style={{fontSize:24, fontWeight:700}}>₹{formatINR(totalGross())}</div></div>
            <div><div style={{fontSize:12, opacity:0.7}}>TAXABLE</div><div style={{fontSize:24, fontWeight:700, color:'#fbbf24'}}>₹{formatINR(totalNet())}</div></div>
            <div><div style={{fontSize:12, opacity:0.7}}>TDS</div><div style={{fontSize:24, fontWeight:700}}>₹{formatINR(totalTDS())}</div></div>
          </div>
        </div>
      )}
    </div>
  );
};
