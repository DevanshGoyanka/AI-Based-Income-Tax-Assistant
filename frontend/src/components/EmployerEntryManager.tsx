// Simplified Employer Entry Manager for Tax Professionals
import React from 'react';
import { calculateSalary, type EmployerInput, type SalaryCalculationResponse } from '../services/salaryCalculationService';

interface EmployerEntry {
  // Core identification
  customEmployerName?: string;
  employerName: string;
  employerTAN: string;
  employerPAN?: string;
  natureOfEmployment?: 'GOV' | 'NGOV' | 'PSU' | 'PENSIONER' | 'NA';
  periodFrom?: string;
  periodTo?: string;
  
  // Main salary components (most used)
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
  medicalReimbursementReceived?: number;
  
  // Perquisites
  perquisites: number;
  
  // Profits in lieu
  profitsCompensationTermination?: number;
  profitsNonCompete?: number;
  
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
}

export const EmployerEntryManager: React.FC<Props> = ({ entries, onChange, assessmentYear }) => {
  // Initialize with employer and salary sections open by default
  const [expandedSection, setExpandedSection] = React.useState<Record<number, Set<string>>>(() => {
    const initial: Record<number, Set<string>> = {};
    entries.forEach((_, idx) => {
      initial[idx] = new Set(['employer', 'salary']);
    });
    return initial;
  });
  const [calculationResponse, setCalculationResponse] = React.useState<SalaryCalculationResponse | null>(null);

  // Get/set expanded state for an employer
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
      hostelExpenditureAllowance: 0, transportAllowanceReceived: 0, medicalReimbursementReceived: 0,
      perquisites: 0, profitsCompensationTermination: 0, profitsNonCompete: 0,
      professionalTax: 0, entertainmentAllowance: 0, tdsDeducted: 0, grossSalary: 0, netSalary: 0
    };
    onChange([...entries, newEntry]);
    // Auto-expand employer and salary for new entry
    setExpandedSection(prev => ({
      ...prev,
      [entries.length]: new Set(['employer', 'salary'])
    }));
  };

  const removeEntry = (index: number) => onChange(entries.filter((_, i) => i !== index));

  const updateEntry = (index: number, field: keyof EmployerEntry, value: any) => {
    const updated = [...entries];
    updated[index] = { ...updated[index], [field]: value };
    onChange(updated);
    recalculateAllEntries(updated);
  };

  const recalculateAllEntries = async (updatedEntries: EmployerEntry[]) => {
    try {
      const employerInputs: EmployerInput[] = updatedEntries.map(entry => ({
        employerName: entry.employerName, employerTAN: entry.employerTAN,
        basic: entry.basic || 0, da: entry.da || 0, hra: entry.hra || 0,
        bonus: entry.bonus || 0, allowances: entry.allowances || 0, lta: entry.lta || 0,
        rentPaid: entry.rentPaid, isMetroCity: entry.isMetroCity,
        pension: entry.pension || 0, commutedPension: entry.commutedPension || 0,
        uncommutedPension: entry.uncommutedPension || 0, gratuity: entry.gratuity || 0,
        leaveEncashment: entry.leaveEncashment || 0, arrearsOfSalary: entry.arrearsOfSalary || 0,
        perqRentFreeAccommodation: 0, perqConcessionalRent: 0, perqMotorCar: 0,
        perqSweeper: 0, perqGasElectricityWater: 0, perqInterestFreeLoan: 0,
        perqHolidayExpenses: 0, perqFreeEducation: 0, perqGiftsVouchers: 0,
        perqCreditCard: 0, perqClubExpenses: 0, perqMovableAssets: 0, perqOthers: 0,
        profitsCompensationTermination: entry.profitsCompensationTermination || 0,
        profitsNonCompete: entry.profitsNonCompete || 0,
        professionalTax: entry.professionalTax || 0, entertainmentAllowance: entry.entertainmentAllowance || 0,
        tdsDeducted: entry.tdsDeducted || 0,
        childrenEducationAllowance: entry.childrenEducationAllowance,
        hostelExpenditureAllowance: entry.hostelExpenditureAllowance,
        transportAllowanceReceived: entry.transportAllowanceReceived,
        isDisabledEmployee: entry.isDisabledEmployee,
        isGovernmentEmployee: entry.isGovernmentEmployee,
      }));

      const response = await calculateSalary(assessmentYear, employerInputs);
      setCalculationResponse(response);
      
      const recalculated = updatedEntries.map((entry, idx) => {
        const calc = response.employers?.[idx] || {};
        return {
          ...entry,
          hraExempt: calc.hraExempt,
          ltaExempt: calc.ltaExempt,
          gratuityExempt: calc.gratuityExempt,
          leaveEncashmentExempt: calc.leaveEncashmentExempt,
          grossSalary: calc.grossSalary,
          netSalary: calc.netSalary,
        };
      });
      onChange(recalculated);
    } catch (error) {
      console.error('Calculation failed:', error);
    }
  };

  // Collapsible Section Component
  const Section: React.FC<{
    title: string;
    index: number;
    sectionKey: string;
    icon: string;
    badge?: string;
    children: React.ReactNode;
  }> = ({ title, index, sectionKey, icon, badge, children }) => {
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
          <span style={{ fontSize: 13, fontWeight: 600, color: '#475569' }}>
            {icon} {title}
          </span>
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

  // Simple number input
  const NumberInput = (props: React.InputHTMLAttributes<HTMLInputElement>) => (
    <input
      type="number"
      step="1"
      {...props}
      style={{ width: '100%', padding: '8px 10px', border: '1px solid #e2e8f0', borderRadius: 6, fontSize: 13, ...props.style }}
    />
  );

  const fmt = (val: number | undefined | null): string => {
    if (!val) return '0';
    return Math.round(val).toLocaleString('en-IN');
  };

  const getTotalGross = () => Math.round(calculationResponse?.totalGrossSalary ?? entries.reduce((sum, e) => sum + (e.grossSalary || 0), 0));
  const getTotalNet = () => Math.round(calculationResponse?.totalNetSalary ?? entries.reduce((sum, e) => sum + (e.netSalary || 0), 0));
  const getTotalTDS = () => Math.round(calculationResponse?.totalTDS ?? entries.reduce((sum, e) => sum + (e.tdsDeducted || 0), 0));

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
          <div key={index} style={{ background: 'white', border: '1px solid #e2e8f0', borderRadius: 12, padding: 20, marginBottom: 16, boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
            {/* Employer Header */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16, paddingBottom: 16, borderBottom: '1px solid #f1f5f9' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                <span style={{ background: '#c9943a', color: 'white', padding: '4px 12px', borderRadius: 6, fontSize: 12, fontWeight: 600 }}>#{index + 1}</span>
                <input
                  type="text"
                  value={entry.customEmployerName || `Employer ${index + 1}`}
                  onChange={(e) => updateEntry(index, 'customEmployerName', e.target.value)}
                  placeholder="Employer name"
                  style={{ border: 'none', borderBottom: '1px dashed #c9943a', background: 'transparent', fontSize: 14, fontWeight: 600, color: '#1e293b', outline: 'none', minWidth: 150 }}
                />
              </div>
              <button onClick={() => removeEntry(index)} style={{ background: '#fef2f2', color: '#ef4444', border: 'none', width: 28, height: 28, borderRadius: '50%', cursor: 'pointer', fontSize: 16 }}>×</button>
            </div>

            {/* Summary Card - Always Visible */}
            <div style={{ padding: 16, background: 'linear-gradient(135deg, #fef3e2 0%, #fff7ed 100%)', borderRadius: 8, marginBottom: 16, border: '1px solid #fed7aa' }}>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16 }}>
                <div>
                  <div style={{ fontSize: 11, color: '#78716c', marginBottom: 4 }}>Gross Salary</div>
                  <div style={{ fontSize: 16, fontWeight: 700, color: '#1e293b' }}>₹{fmt(entry.grossSalary)}</div>
                </div>
                <div>
                  <div style={{ fontSize: 11, color: '#78716c', marginBottom: 4 }}>Total Exemptions</div>
                  <div style={{ fontSize: 16, fontWeight: 700, color: '#16a34a' }}>- ₹{fmt((entry.hraExempt || 0) + (entry.ltaExempt || 0) + (entry.gratuityExempt || 0) + (entry.leaveEncashmentExempt || 0))}</div>
                </div>
                <div>
                  <div style={{ fontSize: 11, color: '#78716c', marginBottom: 4 }}>Net Taxable</div>
                  <div style={{ fontSize: 16, fontWeight: 700, color: '#c9943a' }}>₹{fmt(entry.netSalary)}</div>
                </div>
                <div>
                  <div style={{ fontSize: 11, color: '#78716c', marginBottom: 4 }}>TDS</div>
                  <div style={{ fontSize: 16, fontWeight: 700, color: '#1e293b' }}>₹{fmt(entry.tdsDeducted)}</div>
                </div>
              </div>
            </div>

            {/* Collapsible Sections */}
            <Section title="Employer Details" index={index} sectionKey="employer" icon="🏢" badge={entry.employerName || 'Required'}>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12 }}>
                <Field label="Employer Name *"><NumberInput value={entry.employerName} onChange={(e) => updateEntry(index, 'employerName', e.target.value)} placeholder="Company name" /></Field>
                <Field label="TAN *"><NumberInput value={entry.employerTAN} onChange={(e) => updateEntry(index, 'employerTAN', e.target.value.toUpperCase())} placeholder="ABCD1234E" maxLength={10} /></Field>
                <Field label="Nature">
                  <select value={entry.natureOfEmployment || 'NGOV'} onChange={(e) => updateEntry(index, 'natureOfEmployment', e.target.value)} style={{ width: '100%', padding: '8px 10px', border: '1px solid #e2e8f0', borderRadius: 6, fontSize: 13 }}>
                    <option value="NGOV">Private</option>
                    <option value="GOV">Government</option>
                    <option value="PSU">PSU</option>
                    <option value="PENSIONER">Pensioner</option>
                  </select>
                </Field>
                <Field label="Period From"><input type="date" value={entry.periodFrom || ''} onChange={(e) => updateEntry(index, 'periodFrom', e.target.value)} style={{ width: '100%', padding: '8px 10px', border: '1px solid #e2e8f0', borderRadius: 6, fontSize: 13 }} /></Field>
                <Field label="Period To"><input type="date" value={entry.periodTo || ''} onChange={(e) => updateEntry(index, 'periodTo', e.target.value)} style={{ width: '100%', padding: '8px 10px', border: '1px solid #e2e8f0', borderRadius: 6, fontSize: 13 }} /></Field>
              </div>
            </Section>

            <Section title="Salary Components" index={index} sectionKey="salary" icon="💰" badge="">
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 12 }}>
                <Field label="Basic Salary"><NumberInput value={entry.basic} onChange={(e) => updateEntry(index, 'basic', parseFloat(e.target.value) || 0)} /></Field>
                <Field label="DA"><NumberInput value={entry.da} onChange={(e) => updateEntry(index, 'da', parseFloat(e.target.value) || 0)} /></Field>
                <Field label="HRA"><NumberInput value={entry.hra} onChange={(e) => updateEntry(index, 'hra', parseFloat(e.target.value) || 0)} /></Field>
                <Field label="Bonus"><NumberInput value={entry.bonus} onChange={(e) => updateEntry(index, 'bonus', parseFloat(e.target.value) || 0)} /></Field>
                <Field label="Other Allowances"><NumberInput value={entry.allowances} onChange={(e) => updateEntry(index, 'allowances', parseFloat(e.target.value) || 0)} /></Field>
                <Field label="LTA"><NumberInput value={entry.lta} onChange={(e) => updateEntry(index, 'lta', parseFloat(e.target.value) || 0)} /></Field>
                <Field label="Arrears"><NumberInput value={entry.arrearsOfSalary} onChange={(e) => updateEntry(index, 'arrearsOfSalary', parseFloat(e.target.value) || 0)} /></Field>
                <Field label="Perquisites"><NumberInput value={entry.perquisites} onChange={(e) => updateEntry(index, 'perquisites', parseFloat(e.target.value) || 0)} /></Field>
              </div>
            </Section>

            <Section title="HRA Exemption" index={index} sectionKey="hra" icon="🏠" badge={(entry.hraExempt || 0) > 0 ? `₹${(entry.hraExempt).toLocaleString('en-IN')}` : 'Optional'}>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12 }}>
                <Field label="Annual Rent Paid"><NumberInput value={entry.rentPaid || 0} onChange={(e) => updateEntry(index, 'rentPaid', parseFloat(e.target.value) || 0)} hint="Required for exemption" /></Field>
                <Field label="Metro City">
                  <select value={entry.isMetroCity ? 'yes' : 'no'} onChange={(e) => updateEntry(index, 'isMetroCity', e.target.value === 'yes')} style={{ width: '100%', padding: '8px 10px', border: '1px solid #e2e8f0', borderRadius: 6, fontSize: 13 }}>
                    <option value="no">No (40%)</option>
                    <option value="yes">Yes (50%)</option>
                  </select>
                </Field>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <input type="checkbox" checked={entry.isGovernmentEmployee || false} onChange={(e) => updateEntry(index, 'isGovernmentEmployee', e.target.checked)} />
                  <span style={{ fontSize: 12, color: '#64748b' }}>Govt Employee (Full HRA exempt)</span>
                </div>
              </div>
            </Section>

            <Section title="Retirement Benefits" index={index} sectionKey="retirement" icon="[R]" badge={entry.gratuity > 0 || entry.leaveEncashment > 0 ? 'Entered' : 'Optional'}>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 12 }}>
                <Field label="Pension"><NumberInput value={entry.pension} onChange={(e) => updateEntry(index, 'pension', parseFloat(e.target.value) || 0)} /></Field>
                <Field label="Commuted Pension"><NumberInput value={entry.commutedPension} onChange={(e) => updateEntry(index, 'commutedPension', parseFloat(e.target.value) || 0)} /></Field>
                <Field label="Gratuity"><NumberInput value={entry.gratuity} onChange={(e) => updateEntry(index, 'gratuity', parseFloat(e.target.value) || 0)} /></Field>
                <Field label="Leave Encashment"><NumberInput value={entry.leaveEncashment} onChange={(e) => updateEntry(index, 'leaveEncashment', parseFloat(e.target.value) || 0)} /></Field>
              </div>
              <div style={{ marginTop: 12, padding: 12, background: '#f0fdf4', borderRadius: 6, fontSize: 12, color: '#166534' }}>
                Info: Govt employees get full exemption on gratuity & leave encashment (no caps). Non-govt employees: capped at Rs 20L (gratuity) & Rs 25L (leave encashment).
              </div>
            </Section>

            <Section title="Special Allowances" index={index} sectionKey="allowances" icon="📋">
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12 }}>
                <Field label="Transport (Disabled)">
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4 }}>
                    <input type="checkbox" checked={entry.isDisabledEmployee || false} onChange={(e) => updateEntry(index, 'isDisabledEmployee', e.target.checked)} />
                    <span style={{ fontSize: 11, color: '#64748b' }}>Disabled Employee</span>
                  </div>
                  {entry.isDisabledEmployee && <NumberInput value={entry.transportAllowanceReceived || 0} onChange={(e) => updateEntry(index, 'transportAllowanceReceived', parseFloat(e.target.value) || 0)} placeholder="₹38,400/yr" />}
                </Field>
                <Field label="Children Education"><NumberInput value={entry.childrenEducationAllowance || 0} onChange={(e) => updateEntry(index, 'childrenEducationAllowance', parseFloat(e.target.value) || 0)} hint="Max ₹2,400/yr" /></Field>
                <Field label="Hostel Expenditure"><NumberInput value={entry.hostelExpenditureAllowance || 0} onChange={(e) => updateEntry(index, 'hostelExpenditureAllowance', parseFloat(e.target.value) || 0)} hint="Max ₹7,200/yr" /></Field>
              </div>
            </Section>

            <Section title="Deductions & TDS" index={index} sectionKey="deductions" icon="📝">
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12 }}>
                <Field label="Professional Tax"><NumberInput value={entry.professionalTax} onChange={(e) => updateEntry(index, 'professionalTax', parseFloat(e.target.value) || 0)} hint="Max ₹2,500" /></Field>
                <Field label="Entertainment (Govt)"><NumberInput value={entry.entertainmentAllowance} onChange={(e) => updateEntry(index, 'entertainmentAllowance', parseFloat(e.target.value) || 0)} hint="Max ₹5,000" /></Field>
                <Field label="TDS Deducted"><NumberInput value={entry.tdsDeducted} onChange={(e) => updateEntry(index, 'tdsDeducted', parseFloat(e.target.value) || 0)} hint="Form 16 TDS" /></Field>
              </div>
            </Section>
          </div>
        ))
      )}

      {/* Grand Total Summary */}
      {entries.length > 0 && (
        <div style={{ padding: 20, background: 'linear-gradient(135deg, #1e293b 0%, #334155 100%)', borderRadius: 12, color: 'white' }}>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 24, textAlign: 'center' }}>
            <div>
              <div style={{ fontSize: 12, opacity: 0.7, marginBottom: 4 }}>TOTAL GROSS SALARY</div>
              <div style={{ fontSize: 24, fontWeight: 700 }}>₹{getTotalGross().toLocaleString('en-IN')}</div>
            </div>
            <div>
              <div style={{ fontSize: 12, opacity: 0.7, marginBottom: 4 }}>TOTAL TAXABLE INCOME</div>
              <div style={{ fontSize: 24, fontWeight: 700, color: '#fbbf24' }}>₹{getTotalNet().toLocaleString('en-IN')}</div>
            </div>
            <div>
              <div style={{ fontSize: 12, opacity: 0.7, marginBottom: 4 }}>TOTAL TDS</div>
              <div style={{ fontSize: 24, fontWeight: 700 }}>₹{getTotalTDS().toLocaleString('en-IN')}</div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
