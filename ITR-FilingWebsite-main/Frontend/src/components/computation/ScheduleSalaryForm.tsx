interface FormField { 
  label: string; 
  field: string; 
  helpText?: string; 
  readOnly?: boolean; 
}

const SALARY_FIELDS: FormField[] = [
  { label: 'Basic Salary', field: 'basicSalary' },
  { label: 'Dearness Allowance', field: 'dearnessAllowance' },
  { label: 'House Rent Allowance (HRA)', field: 'houseRentAllowance' },
  { label: 'Leave Travel Allowance (LTA)', field: 'leaveTravelAllowance' },
  { label: 'Leave Encashment', field: 'leaveEncashment' },
  { label: 'Gratuity', field: 'gratuity' },
  { label: 'Bonus', field: 'bonus' },
  { label: 'Commission', field: 'commission' },
  { label: 'Arrears of Salary', field: 'arrearsOfSalary' },
  { label: 'Other Allowances', field: 'otherAllowances' },
  { label: 'Total u/s 17(1) [AUTO]', field: 'total17_1', readOnly: true },
  { label: 'Total Perquisites u/s 17(2)', field: 'totalPerquisites17_2' },
  { label: 'Total Profits in Lieu u/s 17(3)', field: 'totalProfitsInLieu17_3' },
  { label: 'Gross Salary [AUTO]', field: 'grossSalary', readOnly: true },
  { label: 'HRA Exemption u/s 10(13A)', field: 'hraExemption10_13A', helpText: 'Auto-computed from HRA, rent paid, metro/non-metro' },
  { label: 'LTA Exemption u/s 10(5)', field: 'ltaExemption10_5' },
  { label: 'Gratuity Exemption u/s 10(10)', field: 'gratuityExemption10_10' },
  { label: 'Leave Encashment Exemption 10(10AA)', field: 'leaveEncashmentExemption10_10AA' },
  { label: 'Total Exemptions u/s 10 [AUTO]', field: 'totalExemptionsUnder10', readOnly: true },
  { label: 'Standard Deduction (₹75,000 / ₹50,000)', field: 'standardDeduction', readOnly: true, helpText: '₹75,000 for AY 2026-27, ₹50,000 for AY 2025-26' },
  { label: 'Professional Tax', field: 'professionalTax' },
  { label: 'Entertainment Allowance', field: 'entertainmentAllowance' },
  { label: 'Income from Salary (Taxable) [AUTO]', field: 'taxableIncomeSalary', readOnly: true },
];

interface ScheduleSalaryFormProps {
  formData: any;
  onChange: (path: string, value: any) => void;
}

export default function ScheduleSalaryForm({ formData, onChange }: ScheduleSalaryFormProps) {
  const salaryData = formData.scheduleSalary || {};

  return (
    <div>
      <h2 className="font-serif text-[22px] font-semibold text-text-primary mb-6">
        Schedule: Salary Income
      </h2>
      
      <div className="bg-info-bg border border-[#BFDBFE] rounded-lg p-3 mb-5 text-[12.5px] text-info">
        ℹ All amounts in ₹ (Rupees). Read-only fields are auto-computed.
        Standard deduction: ₹75,000 (AY 2026-27 new regime), ₹50,000 (AY 2025-26).
      </div>

      <div className="grid grid-cols-2 gap-5">
        {SALARY_FIELDS.map(({ label, field, helpText, readOnly }) => {
          const val = salaryData[field] ?? '';
          return (
            <div key={field}>
              <label className="block text-[12.5px] font-semibold text-text-secondary uppercase tracking-[0.4px] mb-1.5">
                {label}
              </label>
              <input
                type="number"
                value={val}
                readOnly={readOnly}
                onChange={e => !readOnly && onChange(`scheduleSalary.${field}`, Number(e.target.value))}
                className={`w-full px-3 py-[9px] bg-bg-card border border-border-strong rounded-lg text-[13.5px] font-mono outline-none transition-colors
                  ${readOnly
                    ? 'bg-bg text-text-secondary cursor-not-allowed border-border'
                    : 'focus:border-gold'}`}
                placeholder="0"
              />
              {helpText && <p className="text-[11px] text-text-muted mt-1">{helpText}</p>}
            </div>
          );
        })}
      </div>

      {/* Employer details */}
      <div className="mt-8 border-t border-border pt-6">
        <h3 className="text-[15px] font-semibold mb-4">Employer TDS Details</h3>
        <div className="grid grid-cols-2 gap-5">
          <div>
            <label className="block text-[12.5px] font-semibold text-text-secondary uppercase tracking-[0.4px] mb-1.5">
              Employer Name
            </label>
            <input 
              type="text" 
              value={salaryData.employerName || ''}
              onChange={e => onChange('scheduleSalary.employerName', e.target.value)}
              className="w-full px-3 py-[9px] bg-bg-card border border-border-strong rounded-lg text-[13.5px] outline-none focus:border-gold" 
            />
          </div>
          <div>
            <label className="block text-[12.5px] font-semibold text-text-secondary uppercase tracking-[0.4px] mb-1.5">
              Employer TAN
            </label>
            <input 
              type="text" 
              maxLength={10} 
              value={salaryData.employerTan || ''}
              onChange={e => onChange('scheduleSalary.employerTan', e.target.value.toUpperCase())}
              placeholder="PUNE12345A"
              className="w-full px-3 py-[9px] bg-bg-card border border-border-strong rounded-lg text-[13.5px] font-mono outline-none focus:border-gold" 
            />
          </div>
          <div>
            <label className="block text-[12.5px] font-semibold text-text-secondary uppercase tracking-[0.4px] mb-1.5">
              Nature of Employment
            </label>
            <select 
              value={salaryData.natureOfEmployment || 'Others'}
              onChange={e => onChange('scheduleSalary.natureOfEmployment', e.target.value)}
              className="w-full px-3 py-[9px] bg-bg-card border border-border-strong rounded-lg text-[13.5px] outline-none focus:border-gold cursor-pointer"
            >
              {['Central Govt', 'State Govt', 'PSU', 'Pensioner', 'Others'].map(o => 
                <option key={o} value={o}>{o}</option>
              )}
            </select>
          </div>
          <div>
            <label className="block text-[12.5px] font-semibold text-text-secondary uppercase tracking-[0.4px] mb-1.5">
              TDS Deducted (₹)
            </label>
            <input 
              type="number" 
              value={salaryData.tdsDeducted || ''}
              onChange={e => onChange('scheduleSalary.tdsDeducted', Number(e.target.value))}
              className="w-full px-3 py-[9px] bg-bg-card border border-border-strong rounded-lg text-[13.5px] font-mono outline-none focus:border-gold" 
            />
          </div>
        </div>
      </div>

      {/* Form 16 OCR Upload */}
      <div className="mt-6 border border-dashed border-border-strong rounded-lg p-5 text-center">
        <div className="text-[16px] mb-2">📄</div>
        <div className="text-[13px] font-medium mb-1">Upload Form 16 (Auto-fill via OCR)</div>
        <div className="text-[12px] text-text-muted mb-3">PDF — Part A (TRACES) and Part B (Employer)</div>
        <div className="flex justify-center gap-3">
          <label className="cursor-pointer">
            <input type="file" accept=".pdf" className="hidden" />
            <span className="px-3 py-1.5 text-[12px] bg-bg border border-border-strong rounded-lg text-text-secondary hover:bg-border cursor-pointer">
              Upload Part A
            </span>
          </label>
          <label className="cursor-pointer">
            <input type="file" accept=".pdf" className="hidden" />
            <span className="px-3 py-1.5 text-[12px] bg-bg border border-border-strong rounded-lg text-text-secondary hover:bg-border cursor-pointer">
              Upload Part B
            </span>
          </label>
        </div>
      </div>
    </div>
  );
}
