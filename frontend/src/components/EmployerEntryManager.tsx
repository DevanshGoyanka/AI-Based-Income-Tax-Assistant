// Simplified Employer Entry Manager for Tax Professionals
import React, { useState, useRef, useCallback } from 'react';
import { calculateSalary, type EmployerInput, type SalaryCalculationResponse } from '../services/salaryCalculationService';

interface EmployerEntry {
  customEmployerName?: string;
  employerName: string;
  employerTAN: string;
  employerPAN?: string;
  natureOfEmployment?: 'GOV' | 'NGOV' | 'PSU' | 'PENSIONER' | 'NA';
  periodFrom?: string;
  periodTo?: string;
  
  // Main salary components
  basic: number;
  da: number;
  hra: number;
  bonus: number;
  allowances: number;
  lta: number;
  
  // HRA calculation
  rentPaid?: number;
  isMetroCity?: boolean;
  
  // Retirement benefits
  pension: number;
  commutedPension: number;
  uncommutedPension: number;
  gratuity: number;
  leaveEncashment: number;
  arrearsOfSalary: number;
  
  // Employer type flags
  isGovernmentEmployee?: boolean;
  isPensioner?: boolean;
  isDisabledEmployee?: boolean;
  
  // Additional allowances
  childrenEducationAllowance?: number;
  hostelExpenditureAllowance?: number;
  transportAllowanceReceived?: number;
  
  // Perquisites
  perquisites: number;
  
  // Deductions & TDS
  professionalTax: number;
  entertainmentAllowance: number;
  tdsDeducted: number;
  
  // Calculated values (from backend)
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
  taxRegime?: string; // NEW - pass regime from parent
}

// Format number with Indian commas (lakhs, crores)
const formatINR = (num: number | undefined | null): string => {
  if (!num || num === 0) return '0';
  return Math.round(num).toLocaleString('en-IN');
};

export const EmployerEntryManager: React.FC<Props> = ({ entries, onChange, assessmentYear, taxRegime = 'OLD' }) => {
  const [expandedSection, setExpandedSection] = useState<{[key: number]: Set<string>}>(() => {
    const initial: {[key: number]: Set<string>} = {};
    entries.forEach((_, idx) => {
      initial[idx] = new Set(['employer', 'salary']);
    });
    return initial;
  });
  
  const [calculationResponse, setCalculationResponse] = useState<SalaryCalculationResponse | null>(null);
  const [isCalculating, setIsCalculating] = useState(false);
  
  // Debounce ref to prevent API spam
  const debounceTimer = useRef<NodeJS.Timeout | null>(null);

  const getExpanded = (index: number, section: string) => 
    expandedSection[index]?.has(section) || false;
  
  const toggleSection = (index: number, section: string) => {
    setExpandedSection(prev => {
      const current = prev[index] || new Set<string>();
      const next = new Set(current);
      if (next.has(section)) next.delete(section);
      else next.add(section);
      return { ...prev, [index]: next };
    });
  };

  const addEntry = () => {
    const newEntry: EmployerEntry = {
      employerName: '', employerTAN: '', customEmployerName: `Employer ${entries.length + 1}`,
      natureOfEmployment: 'NGOV', basic: 0, da: 0, hra: 0, bonus: 0, allowances: 0, lta: 0,
      rentPaid: 0, isMetroCity: false, pension: 0, commutedPension: 0, uncommutedPension: 0,
      gratuity: 0, leaveEncashment: 0, arrearsOfSalary: 0, isGovernmentEmployee: false,
      isPensioner: false, isDisabledEmployee: false, childrenEducationAllowance: 0,
      hostelExpenditureAllowance: 0, transportAllowanceReceived: 0,
      perquisites: 0, professionalTax: 0, entertainmentAllowance: 0, tdsDeducted: 0, grossSalary: 0, netSalary: 0
    };
    const newEntries = [...entries, newEntry];
    onChange(newEntries);
    // Auto-expand employer and salary for new entry
    setExpandedSection(prev => ({
      ...prev,
      [entries.length]: new Set(['employer', 'salary'])
    }));
  };

  const removeEntry = (index: number) => {
    onChange(entries.filter((_, i) => i !== index));
    if (entries[index].grossSalary > 0) {
      // Recalculate if removing an entry with data
      setTimeout(() => recalculateAllEntries(entries.filter((_, i) => i !== index)), 100);
    }
  };

  // Update entry WITHOUT triggering immediate recalculation
  // The user can type freely, calculation happens via debounced call
  const updateEntry = (index: number, field: keyof EmployerEntry, value: any) => {
    const updated = [...entries];
    updated[index] = { ...updated[index], [field]: value };
    
    // Update UI immediately (local state only)
    onChange(updated);
    
    // Debounce the backend calculation - wait 800ms after user stops typing
    if (debounceTimer.current) {
      clearTimeout(debounceTimer.current);
    }
    debounceTimer.current = setTimeout(() => {
      recalculateAllEntries(updated);
    }, 800);
  };

  const recalculateAllEntries = async (updatedEntries: EmployerEntry[]) => {
    if (isCalculating) return;
    
    // Only calculate if there's actual data
    const hasData = updatedEntries.some(e => (e.basic || 0) > 0);
    if (!hasData) {
      // Clear calculations if no data
      setCalculationResponse(null);
      return;
    }

    setIsCalculating(true);
    try {
      const employerInputs: EmployerInput[] = updatedEntries.map(entry => ({
        employerName: entry.employerName || 'Employer',
        employerTAN: entry.employerTAN || '',
        basic: entry.basic || 0,
        da: entry.da || 0,
        hra: entry.hra || 0,
        bonus: entry.bonus || 0,
        allowances: entry.allowances || 0,
        lta: entry.lta || 0,
        rentPaid: entry.rentPaid,
        isMetroCity: entry.isMetroCity,
        pension: entry.pension || 0,
        commutedPension: entry.commutedPension || 0,
        gratuity: entry.gratuity || 0,
        leaveEncashment: entry.leaveEncashment || 0,
        professionalTax: entry.professionalTax || 0,
        entertainmentAllowance: entry.entertainmentAllowance || 0,
        tdsDeducted: entry.tdsDeducted || 0,
        isDisabledEmployee: entry.isDisabledEmployee,
        isGovernmentEmployee: entry.isGovernmentEmployee,
        childrenEducationAllowance: entry.childrenEducationAllowance,
        hostelExpenditureAllowance: entry.hostelExpenditureAllowance,
      }));

      const response = await calculateSalary(assessmentYear, employerInputs, taxRegime);
      console.log('[SALARY] API Response:', JSON.stringify(response, null, 2));
      setCalculationResponse(response);
      
      // Log the key values
      console.log('[SALARY] Gross:', response.grossSalary, 'HRA Exempt:', response.hraExempt, 'Total Exempt:', response.totalExemptions);
      
      // Update entries with calculated values from BACKEND response
      const recalc = updatedEntries.map((entry, idx) => ({
        ...entry,
        hraExempt: response.hraExempt || 0,
        ltaExempt: response.ltaExempt || 0,
        gratuityExempt: response.gratuityExempt || 0,
        leaveEncashmentExempt: response.leaveEncashmentExempt || 0,
        grossSalary: response.grossSalary || 0,
        netSalary: response.netTaxableSalary || 0,
      }));
      onChange(recalc);
    } catch (error) {
      console.error('Calculation failed:', error);
    } finally {
      setIsCalculating(false);
    }
  };

  // Section component - collapsible
  const Section: React.FC<{
    title: string;
    index: number;
    sectionKey: string;
    badge?: string;
    children: React.ReactNode;
  }> = ({ title, index, sectionKey, badge, children }) => {
    const isOpen = getExpanded(index, sectionKey);
    return (
      <div style={{ marginBottom: 8, border: '1px solid #e2e8f0', borderRadius: 8, overflow: 'hidden' }}>
        <button
          onClick={() => toggleSection(index, sectionKey)}
          style={{
            width: '100%', padding: '12px 16px', display: 'flex', justifyContent: 'space-between',
            alignItems: 'center', background: isOpen ? '#fef3e2' : '#f8fafc', border: 'none',
            cursor: 'pointer', textAlign: 'left'
          }}
        >
          <span style={{ fontSize: 13, fontWeight: 600, color: '#475569' }}>{title}</span>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            {badge && <span style={{ fontSize: 11, background: '#e2e8f0', padding: '2px 8px', borderRadius: 4 }}>{badge}</span>}
            <span style={{ color: '#94a3b8', fontSize: 12 }}>{isOpen ? '▲' : '▼'}</span>
          </div>
        </button>
        {isOpen && <div style={{ padding: 16, background: 'white' }}>{children}</div>}
      </div>
    );
  };

  // Form field component  
  const Field: React.FC<{ label: string; children: React.ReactNode; hint?: string }> = ({ label, children, hint }) => (
    <div>
      <label style={{ display: 'block', marginBottom: 4, fontSize: 12, fontWeight: 500, color: '#64748b' }}>{label}</label>
      {children}
      {hint && <div style={{ fontSize: 10, color: '#94a3b8', marginTop: 2 }}>{hint}</div>}
    </div>
  );

  // Number input - cleared of any problematic styling
  const NumberInput = (props: React.InputHTMLAttributes<HTMLInputElement> & { onChangeValue?: (v: number) => void }) => {
    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
      const val = e.target.value.replace(/[^\d]/g, '');
      const num = parseInt(val || '0', 10);
      if (props.onChangeValue) {
        props.onChangeValue(num);
      }
      if (props.onChange) {
        props.onChange(e);
      }
    };
    
    return (
      <input
        type="text"
        inputMode="numeric"
        {...props}
        value={props.value === 0 || !props.value ? '' : props.value.toString()}
        onChange={handleChange}
        placeholder="0"
        style={{ 
          width: '100%', 
          padding: '8px 10px', 
          border: '1px solid #e2e8f0', 
          borderRadius: 6, 
          fontSize: 13,
          ...props.style 
        }}
      />
    );
  };

  // Get computed values - use backend response or calculate locally for display
  const getGross = (entry: EmployerEntry) => {
    const calc = calculationResponse?.employers?.[0];
    if (calc?.grossSalary) return calc.grossSalary;
    // Fallback: show entered values
    return (entry.basic || 0) + (entry.da || 0) + (entry.hra || 0) + 
           (entry.bonus || 0) + (entry.allowances || 0) + (entry.lta || 0) +
           (entry.perquisites || 0) + (entry.arrearsOfSalary || 0);
  };

  const getExemptions = (entry: EmployerEntry) => {
    const calc = calculationResponse?.employers?.[0];
    if (calc?.hraExempt) return (calc.hraExempt || 0) + (calc.ltaExempt || 0) + 
                              (calc.gratuityExempt || 0) + (calc.leaveEncashmentExempt || 0);
    return 0; // Will show based on backend response
  };

  const getNet = (entry: EmployerEntry) => {
    const calc = calculationResponse?.employers?.[0];
    if (calc?.netSalary) return calc.netSalary;
    return getGross(entry) - getExemptions(entry);
  };

  const getTotalGross = () => Math.round(calculationResponse?.grossSalary ?? entries.reduce((sum, e) => sum + getGross(e), 0));
  const getTotalNet = () => Math.round(calculationResponse?.netTaxableSalary ?? entries.reduce((sum, e) => sum + getNet(e), 0));
  const getTotalTDS = () => Math.round(entries.reduce((sum, e) => sum + (e.tdsDeducted || 0), 0));

  return (
    <div style={{ marginBottom: 24 }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <h3 style={{ margin: 0, fontSize: 14, fontWeight: 600, color: '#64748b' }}>Salary Details</h3>
        <button onClick={addEntry} style={{ padding: '8px 16px', background: '#c9943a', color: 'white', border: 'none', borderRadius: 6, fontSize: 13, fontWeight: 500, cursor: 'pointer' }}>
          + Add Employer
        </button>
      </div>

      {entries.length === 0 ? (
        <div style={{ textAlign: 'center', padding: 40, color: '#94a3b8', background: '#f8fafc', borderRadius: 8, border: '1px dashed #cbd5e1' }}>
          Click "Add Employer" to enter salary details
        </div>
      ) : (
        entries.map((entry, index) => (
          <div key={index} style={{ background: 'white', border: '1px solid #e2e8f0', borderRadius: 12, padding: 20, marginBottom: 16 }}>
            {/* Employer Header */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16, paddingBottom: 16, borderBottom: '1px solid #f1f5f9' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                <span style={{ background: '#c9943a', color: 'white', padding: '4px 12px', borderRadius: 6, fontSize: 12, fontWeight: 600 }}>#{index + 1}</span>
                <input
                  type="text"
                  value={entry.customEmployerName || `Employer ${index + 1}`}
                  onChange={(e) => updateEntry(index, 'customEmployerName', e.target.value)}
                  style={{ border: 'none', borderBottom: '1px dashed #c9943a', background: 'transparent', fontSize: 14, fontWeight: 600, color: '#1e293b', outline: 'none', minWidth: 150 }}
                />
              </div>
              <button onClick={() => removeEntry(index)} style={{ background: '#fef2f2', color: '#ef4444', border: 'none', width: 28, height: 28, borderRadius: '50%', cursor: 'pointer', fontSize: 16 }}>×</button>
            </div>

            {/* Summary Card */}
            <div style={{ padding: 16, background: 'linear-gradient(135deg, #fef3e2 0%, #fff7ed 100%)', borderRadius: 8, marginBottom: 16, border: '1px solid #fed7aa' }}>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16 }}>
                <div>
                  <div style={{ fontSize: 11, color: '#78716c', marginBottom: 4 }}>Gross Salary</div>
                  <div style={{ fontSize: 16, fontWeight: 700, color: '#1e293b' }}>₹{formatINR(getGross(entry))}</div>
                </div>
                <div>
                  <div style={{ fontSize: 11, color: '#78716c', marginBottom: 4 }}>Exemptions u/s 10</div>
                  <div style={{ fontSize: 16, fontWeight: 700, color: '#16a34a' }}>- ₹{formatINR(getExemptions(entry))}</div>
                </div>
                <div>
                  <div style={{ fontSize: 11, color: '#78716c', marginBottom: 4 }}>Gross Taxable Income</div>
                  <div style={{ fontSize: 16, fontWeight: 700, color: '#c9943a' }}>₹{formatINR(getNet(entry))}</div>
                </div>
                <div>
                  <div style={{ fontSize: 11, color: '#78716c', marginBottom: 4 }}>TDS Deducted</div>
                  <div style={{ fontSize: 16, fontWeight: 700, color: '#1e293b' }}>₹{formatINR(entry.tdsDeducted)}</div>
                </div>
              </div>
            </div>

            {/* Collapsible Sections */}
            <Section title="Employer Details" index={index} sectionKey="employer" badge={entry.employerName || 'Required'}>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12 }}>
                <Field label="Employer Name *">
                  <input type="text" value={entry.employerName} onChange={(e) => updateEntry(index, 'employerName', e.target.value)} 
                    style={{ width: '100%', padding: '8px 10px', border: '1px solid #e2e8f0', borderRadius: 6, fontSize: 13 }} />
                </Field>
                <Field label="TAN *">
                  <input type="text" value={entry.employerTAN} onChange={(e) => updateEntry(index, 'employerTAN', e.target.value.toUpperCase())} 
                    maxLength={10} style={{ width: '100%', padding: '8px 10px', border: '1px solid #e2e8f0', borderRadius: 6, fontSize: 13 }} />
                </Field>
                <Field label="Nature">
                  <select value={entry.natureOfEmployment || 'NGOV'} onChange={(e) => updateEntry(index, 'natureOfEmployment', e.target.value)} 
                    style={{ width: '100%', padding: '8px 10px', border: '1px solid #e2e8f0', borderRadius: 6, fontSize: 13 }}>
                    <option value="NGOV">Private</option>
                    <option value="GOV">Government</option>
                    <option value="PSU">PSU</option>
                    <option value="PENSIONER">Pensioner</option>
                  </select>
                </Field>
              </div>
            </Section>

            <Section title="Salary Components" index={index} sectionKey="salary" badge="">
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 12 }}>
                <Field label="Basic Salary">
                  <NumberInput value={entry.basic} onChangeValue={(v) => updateEntry(index, 'basic', v)} />
                </Field>
                <Field label="DA">
                  <NumberInput value={entry.da} onChangeValue={(v) => updateEntry(index, 'da', v)} />
                </Field>
                <Field label="HRA">
                  <NumberInput value={entry.hra} onChangeValue={(v) => updateEntry(index, 'hra', v)} />
                </Field>
                <Field label="Bonus">
                  <NumberInput value={entry.bonus} onChangeValue={(v) => updateEntry(index, 'bonus', v)} />
                </Field>
                <Field label="Other Allowances">
                  <NumberInput value={entry.allowances} onChangeValue={(v) => updateEntry(index, 'allowances', v)} />
                </Field>
                <Field label="LTA">
                  <NumberInput value={entry.lta} onChangeValue={(v) => updateEntry(index, 'lta', v)} />
                </Field>
                <Field label="Arrears">
                  <NumberInput value={entry.arrearsOfSalary} onChangeValue={(v) => updateEntry(index, 'arrearsOfSalary', v)} />
                </Field>
                <Field label="Perquisites">
                  <NumberInput value={entry.perquisites} onChangeValue={(v) => updateEntry(index, 'perquisites', v)} />
                </Field>
              </div>
            </Section>

            <Section title="HRA Exemption" index={index} sectionKey="hra" badge={(entry.hraExempt || 0) > 0 ? `₹${formatINR(entry.hraExempt)}` : 'Optional'}>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12 }}>
                <Field label="Annual Rent Paid">
                  <NumberInput value={entry.rentPaid || 0} onChangeValue={(v) => updateEntry(index, 'rentPaid', v)} hint="Required for exemption" />
                </Field>
                <Field label="Metro City">
                  <select value={entry.isMetroCity ? 'yes' : 'no'} onChange={(e) => updateEntry(index, 'isMetroCity', e.target.value === 'yes')} 
                    style={{ width: '100%', padding: '8px 10px', border: '1px solid #e2e8f0', borderRadius: 6, fontSize: 13 }}>
                    <option value="no">No (40%)</option>
                    <option value="yes">Yes (50%)</option>
                  </select>
                </Field>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 20 }}>
                  <input type="checkbox" checked={entry.isGovernmentEmployee || false} 
                    onChange={(e) => updateEntry(index, 'isGovernmentEmployee', e.target.checked)} />
                  <span style={{ fontSize: 12, color: '#64748b' }}>Govt Employee (Full HRA exempt)</span>
                </div>
              </div>
            </Section>

            <Section title="Retirement Benefits" index={index} sectionKey="retirement" 
              badge={(entry.gratuity > 0 || entry.leaveEncashment > 0) ? 'Entered' : 'Optional'}>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 12 }}>
                <Field label="Pension"><NumberInput value={entry.pension} onChangeValue={(v) => updateEntry(index, 'pension', v)} /></Field>
                <Field label="Commuted Pension"><NumberInput value={entry.commutedPension} onChangeValue={(v) => updateEntry(index, 'commutedPension', v)} /></Field>
                <Field label="Gratuity"><NumberInput value={entry.gratuity} onChangeValue={(v) => updateEntry(index, 'gratuity', v)} /></Field>
                <Field label="Leave Encashment"><NumberInput value={entry.leaveEncashment} onChangeValue={(v) => updateEntry(index, 'leaveEncashment', v)} /></Field>
              </div>
            </Section>

            <Section title="Special Allowances" index={index} sectionKey="allowances" badge="">
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12 }}>
                <Field label="Transport (Disabled)">
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4 }}>
                    <input type="checkbox" checked={entry.isDisabledEmployee || false} 
                      onChange={(e) => updateEntry(index, 'isDisabledEmployee', e.target.checked)} />
                    <span style={{ fontSize: 11, color: '#64748b' }}>Disabled Employee</span>
                  </div>
                </Field>
                <Field label="Children Education"><NumberInput value={entry.childrenEducationAllowance || 0} onChangeValue={(v) => updateEntry(index, 'childrenEducationAllowance', v)} hint="Max ₹2,400/yr" /></Field>
                <Field label="Hostel Expenditure"><NumberInput value={entry.hostelExpenditureAllowance || 0} onChangeValue={(v) => updateEntry(index, 'hostelExpenditureAllowance', v)} hint="Max ₹7,200/yr" /></Field>
              </div>
            </Section>

            <Section title="Deductions & TDS" index={index} sectionKey="deductions" badge="">
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12 }}>
                <Field label="Professional Tax"><NumberInput value={entry.professionalTax} onChangeValue={(v) => updateEntry(index, 'professionalTax', v)} hint="Max ₹2,500" /></Field>
                <Field label="Entertainment (Govt)"><NumberInput value={entry.entertainmentAllowance} onChangeValue={(v) => updateEntry(index, 'entertainmentAllowance', v)} hint="Max ₹5,000" /></Field>
                <Field label="TDS Deducted"><NumberInput value={entry.tdsDeducted} onChangeValue={(v) => updateEntry(index, 'tdsDeducted', v)} hint="Form 16 TDS" /></Field>
              </div>
            </Section>
          </div>
        ))
      )}

      {/* Grand Total Summary */}
      {entries.length > 0 && (
        <div style={{ padding: 20, background: 'linear-gradient(135deg, #1e293b 0%, #334155 100%)', borderRadius: 12, color: 'white' }}>
          {isCalculating && (
            <div style={{ textAlign: 'center', marginBottom: 10, fontSize: 12, opacity: 0.7 }}>Calculating...</div>
          )}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 24, textAlign: 'center' }}>
            <div>
              <div style={{ fontSize: 12, opacity: 0.7, marginBottom: 4 }}>TOTAL GROSS SALARY</div>
              <div style={{ fontSize: 24, fontWeight: 700 }}>₹{formatINR(getTotalGross())}</div>
            </div>
            <div>
              <div style={{ fontSize: 12, opacity: 0.7, marginBottom: 4 }}>TOTAL TAXABLE INCOME</div>
              <div style={{ fontSize: 24, fontWeight: 700, color: '#fbbf24' }}>₹{formatINR(getTotalNet())}</div>
            </div>
            <div>
              <div style={{ fontSize: 12, opacity: 0.7, marginBottom: 4 }}>TOTAL TDS</div>
              <div style={{ fontSize: 24, fontWeight: 700 }}>₹{formatINR(getTotalTDS())}</div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
