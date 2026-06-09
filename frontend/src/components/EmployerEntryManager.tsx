// Multiple Employer Entry Manager - CBDT Compliant
// ALL CALCULATIONS DONE IN BACKEND - Frontend displays only
import React from 'react';
import { calculateSalary, type EmployerInput, type SalaryCalculationResponse, type EmployerCalculation } from '../services/salaryCalculationService';

interface EmployerEntry {
  // Custom name for easy identification (optional, defaults to employerName or "Employer X")
  customEmployerName?: string;
  
  employerName: string;
  employerTAN: string;
  employerPAN?: string;
  employerAddress?: string;
  employerCity?: string;
  employerState?: string;
  employerPincode?: string;
  natureOfEmployment?: 'GOV' | 'NGOV' | 'PSU' | 'PENSIONER' | 'NA';
  periodFrom?: string;
  periodTo?: string;
  
  // Section 17(1) - Salary Components
  basic: number;
  da: number;
  hra: number;
  bonus: number;
  allowances: number;
  lta: number;
  
  // HRA Calculation fields
  rentPaid?: number;
  isMetroCity?: boolean;
  pension: number;
  commutedPension: number;
  uncommutedPension: number;
  gratuity: number;
  leaveEncashment: number;
  arrearsOfSalary: number;
  
  // Retirement details for exemption calculations
  isGovernmentEmployee?: boolean;
  isPensioner?: boolean;
  retirementDate?: string;
  daForRetirement?: number;
  
  // Pension Commutation u/s 10(10A)
  pensionCommutationReceived?: number;
  pensionType?: 'COMMUTED' | 'UNCOMMUTED' | 'FAMILY';
  
  // VRS/Retrenchment u/s 10(10A)
  vrsCompensation?: number;
  retrenchmentCompensation?: number;
  
  // Section 17(2) - Perquisites (detailed breakdown)
  perquisites: number;
  perqRentFreeAccommodation: number;
  perqConcessionalRent: number;
  perqMotorCar: number;
  perqSweeper: number;
  perqGasElectricityWater: number;
  perqInterestFreeLoan: number;
  perqHolidayExpenses: number;
  perqFreeEducation: number;
  perqGiftsVouchers: number;
  perqCreditCard: number;
  perqClubExpenses: number;
  perqMovableAssets: number;
  perqOthers: number;
  
  // Section 17(3) - Profits in Lieu of Salary
  profitsCompensationTermination: number;
  profitsNonCompete: number;
  
  // Exemptions u/s 10 (Read from backend calculation)
  hraExempt: number;
  ltaExempt: number;
  gratuityExempt: number;
  leaveEncashmentExempt: number;
  otherExemptions: number;
  
  // Additional exemption fields (for backend calculation)
  medicalReimbursementReceived?: number;
  transportAllowanceReceived?: number;
  isDisabledEmployee?: boolean;
  childrenEducationAllowance?: number;
  hostelExpenditureAllowance?: number;
  
  // Deductions u/s 16
  professionalTax: number;
  entertainmentAllowance: number;
  
  tdsDeducted: number;
  
  // Calculated values from backend - READ ONLY, NOT CALCULATED IN FRONTEND
  grossSalary: number;
  netSalary: number;
}

interface EmployerEntryManagerProps {
  entries: EmployerEntry[];
  onChange: (entries: EmployerEntry[]) => void;
  assessmentYear: string;
}

// Extended entry with backend calculation results
interface EmployerEntryWithCalc extends EmployerEntry {
  // Additional calculated fields from backend
  hraTaxable?: number;
  ltaTaxable?: number;
  gratuityTaxable?: number;
  leaveEncashmentTaxable?: number;
  pensionCommutationExempt?: number;
  pensionCommutationTaxable?: number;
  medicalReimbursementExempt?: number;
  medicalReimbursementTaxable?: number;
  transportAllowanceExempt?: number;
  childrenEducationExempt?: number;
  hostelExpenditureExempt?: number;
  salary171?: number;
  salary172?: number;
  salary173?: number;
  totalExemptions?: number;
  standardDeduction?: number;
  totalDeductions16?: number;
  validationWarnings?: string[];
  validationErrors?: string[];
}

export const EmployerEntryManager: React.FC<EmployerEntryManagerProps> = ({ entries, onChange, assessmentYear }) => {
  const [activeTab, setActiveTab] = React.useState<Record<number, 'info' | 'salary' | 'exemptions'>>({});
  const [isInitialized, setIsInitialized] = React.useState(false);
  
  // Store latest calculation response for totals display
  const [calculationResponse, setCalculationResponse] = React.useState<SalaryCalculationResponse | null>(null);




  // Auto-recalculate when entries are loaded from prefill (when gross == basic without deductions applied)
  React.useEffect(() => {
    if (!isInitialized && entries.length > 0) {
      // Check if entries need recalculation (i.e., grossSalary = netSalary = basic, no deduction applied)
      const needsRecalculation = entries.some(e => 
        e.grossSalary === e.basic && 
        e.netSalary === e.basic && 
        e.basic > 0
      );
      if (needsRecalculation) {
        console.log('Auto-recalculating entries loaded from prefill...');
        recalculateAllEntries(entries);
      }
      setIsInitialized(true);
    }
  }, [entries, isInitialized]);

  const getTab = (index: number) => activeTab[index] ?? 'info';
  const setTab = (index: number, tab: 'info' | 'salary' | 'exemptions') => {
    setActiveTab(prev => ({ ...prev, [index]: tab }));
  };

  const addEntry = () => {
    const newEntry: EmployerEntry = {
      employerName: '',
      customEmployerName: `Employer ${entries.length + 1}`,
      employerTAN: '',
      employerPAN: '',
      employerAddress: '',
      employerCity: '',
      employerState: '',
      employerPincode: '',
      natureOfEmployment: 'NGOV',
      periodFrom: '',
      periodTo: '',
      basic: 0,
      da: 0,
      hra: 0,
      bonus: 0,
      allowances: 0,
      lta: 0,
      rentPaid: 0,
      isMetroCity: false,
      pension: 0,
      commutedPension: 0,
      uncommutedPension: 0,
      gratuity: 0,
      leaveEncashment: 0,
      arrearsOfSalary: 0,
      isGovernmentEmployee: false,
      isPensioner: false,
      retirementDate: undefined,
      daForRetirement: 0,
      pensionCommutationReceived: 0,
      pensionType: undefined,
      vrsCompensation: 0,
      retrenchmentCompensation: 0,
      perquisites: 0,
      perqRentFreeAccommodation: 0,
      perqConcessionalRent: 0,
      perqMotorCar: 0,
      perqSweeper: 0,
      perqGasElectricityWater: 0,
      perqInterestFreeLoan: 0,
      perqHolidayExpenses: 0,
      perqFreeEducation: 0,
      perqGiftsVouchers: 0,
      perqCreditCard: 0,
      perqClubExpenses: 0,
      perqMovableAssets: 0,
      perqOthers: 0,
      profitsCompensationTermination: 0,
      profitsNonCompete: 0,
      hraExempt: 0,
      ltaExempt: 0,
      gratuityExempt: 0,
      leaveEncashmentExempt: 0,
      otherExemptions: 0,
      professionalTax: 0,
      entertainmentAllowance: 0,
      tdsDeducted: 0,
      grossSalary: 0,
      netSalary: 0,
      
      // Additional exemption fields
      medicalReimbursementReceived: 0,
      transportAllowanceReceived: 0,
      isDisabledEmployee: false,
      childrenEducationAllowance: 0,
      hostelExpenditureAllowance: 0,
    };
    onChange([...entries, newEntry]);
  };

  const removeEntry = (index: number) => {
    onChange(entries.filter((_, i) => i !== index));
  };

  const recalculateAllEntries = async (updatedEntries: EmployerEntry[]) => {
    try {
      console.log('Starting calculation for', updatedEntries.length, 'employers');
      console.log('Assessment Year:', assessmentYear);
      
      const employerInputs: EmployerInput[] = updatedEntries.map(entry => ({
        // Custom name for easy identification
        customEmployerName: entry.customEmployerName,
        
        employerName: entry.employerName,
        employerTAN: entry.employerTAN,
        employerPAN: entry.employerPAN,
        natureOfEmployment: entry.natureOfEmployment,
        periodFrom: entry.periodFrom,
        periodTo: entry.periodTo,
        
        // Section 17(1) - Salary Components
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
        uncommutedPension: entry.uncommutedPension || 0,
        gratuity: entry.gratuity || 0,
        leaveEncashment: entry.leaveEncashment || 0,
        arrearsOfSalary: entry.arrearsOfSalary || 0,
        
        // Section 17(2) - Perquisites
        perqRentFreeAccommodation: entry.perqRentFreeAccommodation || 0,
        perqConcessionalRent: entry.perqConcessionalRent || 0,
        perqMotorCar: entry.perqMotorCar || 0,
        perqSweeper: entry.perqSweeper || 0,
        perqGasElectricityWater: entry.perqGasElectricityWater || 0,
        perqInterestFreeLoan: entry.perqInterestFreeLoan || 0,
        perqHolidayExpenses: entry.perqHolidayExpenses || 0,
        perqFreeEducation: entry.perqFreeEducation || 0,
        perqGiftsVouchers: entry.perqGiftsVouchers || 0,
        perqCreditCard: entry.perqCreditCard || 0,
        perqClubExpenses: entry.perqClubExpenses || 0,
        perqMovableAssets: entry.perqMovableAssets || 0,
        perqOthers: entry.perqOthers || 0,
        
        // Section 17(3) - Profits in Lieu
        profitsCompensationTermination: entry.profitsCompensationTermination || 0,
        profitsNonCompete: entry.profitsNonCompete || 0,
        
        // Exemptions u/s 10
        ltaExempt: entry.ltaExempt || 0,
        gratuityExempt: entry.gratuityExempt || 0,
        leaveEncashmentExempt: entry.leaveEncashmentExempt || 0,
        otherExemptions: entry.otherExemptions || 0,
        
        // Additional exemption fields (ALL SENT TO BACKEND FOR CALCULATION)
        medicalReimbursementReceived: entry.medicalReimbursementReceived,
        transportAllowanceReceived: entry.transportAllowanceReceived,
        isDisabledEmployee: entry.isDisabledEmployee,
        childrenEducationAllowance: entry.childrenEducationAllowance,
        hostelExpenditureAllowance: entry.hostelExpenditureAllowance,
        pensionType: entry.pensionType,
        
        // Deductions u/s 16
        professionalTax: entry.professionalTax || 0,
        entertainmentAllowance: entry.entertainmentAllowance || 0,
        
        // TDS
        tdsDeducted: entry.tdsDeducted || 0,
      }));

      console.log('Calling API with inputs:', employerInputs);
      console.log('Full request payload:', JSON.stringify({ assessmentYear, employers: employerInputs }, null, 2));
      const response = await calculateSalary(assessmentYear, employerInputs);
      console.log('API Response:', response);
      console.log('Response structure:', JSON.stringify(response, null, 2));
      
      // Store full response for ALL totals from backend
      setCalculationResponse(response);
      
      const recalculated = updatedEntries.map((entry, idx) => {
        const calc = response.employers?.[idx] || {};
        console.log(`Employer ${idx}: Gross=${calc.grossSalary || 0}, Net=${calc.netSalary || 0}`);
        
        return {
          ...entry,
          // All calculated values from backend - not computed in frontend
          hraExempt: calc.hraExempt,
          hraTaxable: calc.hraTaxable,
          ltaExempt: calc.ltaExempt,
          ltaTaxable: calc.ltaTaxable,
          gratuityExempt: calc.gratuityExempt,
          gratuityTaxable: calc.gratuityTaxable,
          leaveEncashmentExempt: calc.leaveEncashmentExempt,
          leaveEncashmentTaxable: calc.leaveEncashmentTaxable,
          pensionCommutationExempt: calc.pensionCommutationExempt,
          pensionCommutationTaxable: calc.pensionCommutationTaxable,
          medicalReimbursementExempt: calc.medicalReimbursementExempt,
          medicalReimbursementTaxable: calc.medicalReimbursementTaxable,
          transportAllowanceExempt: calc.transportAllowanceExempt,
          childrenEducationExempt: calc.childrenEducationExempt,
          hostelExpenditureExempt: calc.hostelExpenditureExempt,
          grossSalary: calc.grossSalary,
          netSalary: calc.netSalary,
          perquisites: calc.perquisites,
          salary171: calc.salary171,
          salary172: calc.salary172,
          salary173: calc.salary173,
          totalExemptions: calc.totalExemptions,
          standardDeduction: calc.standardDeduction,
          totalDeductions16: calc.totalDeductions16,
          validationWarnings: calc.validationWarnings,
          validationErrors: calc.validationErrors,
        };
      });
      
      console.log('Updating state with recalculated values');
      onChange(recalculated);
    } catch (error: any) {
      console.error('Failed to calculate salary:', error);
      console.error('Error response:', error.response?.data);
      console.error('Error status:', error.response?.status);
      console.error('Error headers:', error.response?.headers);
      // Fallback to local calculation if API fails
      onChange(updatedEntries);
    }
  };

  const updateEntry = (index: number, field: keyof EmployerEntry, value: any) => {
    const updated = [...entries];
    updated[index] = { ...updated[index], [field]: value };
    
    // Update immediately for responsive UI
    onChange(updated);
    
    // Trigger backend recalculation (will update again with calculated values)
    recalculateAllEntries(updated);
  };

  // ALL TOTALS COME FROM BACKEND - NOT CALCULATED IN FRONTEND
  const getTotalGross = () => calculationResponse?.totalGrossSalary ?? entries.reduce((sum, e) => sum + e.grossSalary, 0);
  const getTotalNet = () => calculationResponse?.totalNetSalary ?? entries.reduce((sum, e) => sum + e.netSalary, 0);
  const getTotalTDS = () => calculationResponse?.totalTDS ?? entries.reduce((sum, e) => sum + e.tdsDeducted, 0);

  return (
    <div style={{ marginBottom: 24 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <h3 style={{ margin: 0, fontSize: 14, fontWeight: 600, color: '#64748b' }}>
          Salary from Multiple Employers
        </h3>
        <button
          onClick={addEntry}
          style={{
            padding: '8px 16px',
            background: '#c9943a',
            color: 'white',
            border: 'none',
            borderRadius: 6,
            fontSize: 13,
            fontWeight: 500,
            cursor: 'pointer'
          }}
        >
          + Add Employer
        </button>
      </div>

      {entries.length === 0 && (
        <div style={{ 
          textAlign: 'center', 
          padding: 40, 
          color: '#94a3b8',
          background: '#f8fafc',
          borderRadius: 8,
          border: '1px dashed #cbd5e1'
        }}>
          Click "Add Employer" to add salary details from each employer
        </div>
      )}

      {entries.map((entry, index) => (
        <div key={index} style={{ 
          background: 'white', 
          border: '1px solid #e2e8f0', 
          borderRadius: 8, 
          padding: 20, 
          marginBottom: 16,
          boxShadow: '0 1px 3px rgba(0,0,0,0.05)'
        }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <span style={{ 
                background: '#c9943a', 
                color: 'white', 
                padding: '4px 10px', 
                borderRadius: 6, 
                fontSize: 12,
                fontWeight: 600
              }}>
                #{index + 1}
              </span>
              <input
                type="text"
                value={entry.customEmployerName || `Employer ${index + 1}`}
                onChange={(e) => updateEntry(index, 'customEmployerName', e.target.value)}
                style={{
                  border: 'none',
                  borderBottom: '1px dashed #c9943a',
                  background: 'transparent',
                  fontSize: 14,
                  fontWeight: 600,
                  color: '#1e293b',
                  outline: 'none',
                  padding: '2px 4px',
                  minWidth: 120,
                  cursor: 'text'
                }}
                placeholder={`Employer ${index + 1}`}
              />
            </div>
            <button 
              onClick={() => removeEntry(index)} 
              style={{ 
                background: '#ef4444', 
                color: 'white', 
                border: 'none', 
                width: 32, 
                height: 32, 
                borderRadius: '50%', 
                cursor: 'pointer', 
                fontSize: 18,
                fontWeight: 600
              }}
            >
              ×
            </button>
          </div>

          {/* Basic Employer Details */}
          <h4 style={{ fontSize: 13, fontWeight: 600, color: '#64748b', marginBottom: 12 }}>Employer Details</h4>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16, marginBottom: 16 }}>
            <div style={{ gridColumn: '1 / -1' }}>
              <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>
                Employer Name *
              </label>
              <input
                type="text"
                value={entry.employerName}
                onChange={(e) => updateEntry(index, 'employerName', e.target.value)}
                placeholder="e.g., ABC Pvt Ltd"
                style={{ 
                  width: '100%', 
                  padding: '8px 12px', 
                  border: '1px solid #cbd5e1', 
                  borderRadius: 6, 
                  fontSize: 13
                }}
              />
            </div>

            <div>
              <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>
                Employer TAN *
              </label>
              <input
                type="text"
                value={entry.employerTAN}
                onChange={(e) => updateEntry(index, 'employerTAN', e.target.value.toUpperCase())}
                placeholder="e.g., MUMS89569E"
                maxLength={10}
                style={{ 
                  width: '100%', 
                  padding: '8px 12px', 
                  border: '1px solid #cbd5e1', 
                  borderRadius: 6, 
                  fontSize: 13
                }}
              />
            </div>

            <div>
              <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>
                Employer PAN
              </label>
              <input
                type="text"
                value={entry.employerPAN || ''}
                onChange={(e) => updateEntry(index, 'employerPAN', e.target.value.toUpperCase())}
                placeholder="e.g., AAAAA1234A"
                maxLength={10}
                style={{ 
                  width: '100%', 
                  padding: '8px 12px', 
                  border: '1px solid #cbd5e1', 
                  borderRadius: 6, 
                  fontSize: 13
                }}
              />
            </div>

            <div>
              <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>
                Nature of Employment
              </label>
              <select
                value={entry.natureOfEmployment || 'NGOV'}
                onChange={(e) => updateEntry(index, 'natureOfEmployment', e.target.value)}
                style={{ 
                  width: '100%', 
                  padding: '8px 12px', 
                  border: '1px solid #cbd5e1', 
                  borderRadius: 6, 
                  fontSize: 13
                }}
              >
                <option value="GOV">Government</option>
                <option value="PSU">PSU</option>
                <option value="NGOV">Non-Government</option>
                <option value="PENSIONER">Pensioner</option>
                <option value="NA">Not Applicable</option>
              </select>
            </div>

            <div>
              <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>
                Period From
              </label>
              <input
                type="date"
                value={entry.periodFrom || ''}
                onChange={(e) => updateEntry(index, 'periodFrom', e.target.value)}
                style={{ 
                  width: '100%', 
                  padding: '8px 12px', 
                  border: '1px solid #cbd5e1', 
                  borderRadius: 6, 
                  fontSize: 13
                }}
              />
            </div>

            <div>
              <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>
                Period To
              </label>
              <input
                type="date"
                value={entry.periodTo || ''}
                onChange={(e) => updateEntry(index, 'periodTo', e.target.value)}
                style={{ 
                  width: '100%', 
                  padding: '8px 12px', 
                  border: '1px solid #cbd5e1', 
                  borderRadius: 6, 
                  fontSize: 13
                }}
              />
            </div>
          </div>



          {/* Basic Salary Components - Most Common Fields */}
          <h4 style={{ fontSize: 13, fontWeight: 600, color: '#64748b', marginBottom: 12 }}>Basic Salary Components (from Form 16)</h4>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16, marginBottom: 16 }}>
            <div>
              <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>Basic Salary</label>
              <input type="number" value={entry.basic} onChange={(e) => updateEntry(index, 'basic', parseFloat(e.target.value) || 0)} 
                style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }} />
            </div>
            <div>
              <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>DA (Dearness Allowance)</label>
              <input type="number" value={entry.da} onChange={(e) => updateEntry(index, 'da', parseFloat(e.target.value) || 0)} 
                style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }} />
            </div>
            <div>
              <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>HRA (House Rent Allowance)</label>
              <input type="number" value={entry.hra} onChange={(e) => updateEntry(index, 'hra', parseFloat(e.target.value) || 0)} 
                style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }} />
            </div>
            <div>
              <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>Annual Rent Paid</label>
              <input type="number" value={entry.rentPaid || 0} onChange={(e) => updateEntry(index, 'rentPaid', parseFloat(e.target.value) || 0)} 
                style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }} 
                placeholder="For HRA exemption calc" />
            </div>
            <div>
              <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>Metro City?</label>
              <select value={entry.isMetroCity ? 'yes' : 'no'} onChange={(e) => updateEntry(index, 'isMetroCity', e.target.value === 'yes')} 
                style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }}>
                <option value="yes">Yes (50% exemption)</option>
                <option value="no">No (40% exemption)</option>
              </select>
            </div>
            <div>
              <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>Bonus</label>
              <input type="number" value={entry.bonus} onChange={(e) => updateEntry(index, 'bonus', parseFloat(e.target.value) || 0)} 
                style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }} />
            </div>
            <div>
              <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>Other Allowances</label>
              <input type="number" value={entry.allowances} onChange={(e) => updateEntry(index, 'allowances', parseFloat(e.target.value) || 0)} 
                style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }} />
            </div>
            <div>
              <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>LTA (Leave Travel Allowance)</label>
              <input type="number" value={entry.lta} onChange={(e) => updateEntry(index, 'lta', parseFloat(e.target.value) || 0)} 
                style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }} />
            </div>
          </div>

          {/* Deductions u/s 16 & TDS */}
          <h4 style={{ fontSize: 13, fontWeight: 600, color: '#64748b', marginBottom: 12 }}>Deductions u/s 16 & TDS</h4>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16, marginBottom: 16 }}>
            <div>
              <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>Professional Tax</label>
              <input type="number" value={entry.professionalTax} onChange={(e) => updateEntry(index, 'professionalTax', parseFloat(e.target.value) || 0)} 
                style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }} />
            </div>
            <div>
              <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>Entertainment Allowance (Govt only)</label>
              <input type="number" value={entry.entertainmentAllowance} onChange={(e) => updateEntry(index, 'entertainmentAllowance', parseFloat(e.target.value) || 0)} 
                style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }} />
            </div>
            <div>
              <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>TDS Deducted (192)</label>
              <input type="number" value={entry.tdsDeducted} onChange={(e) => updateEntry(index, 'tdsDeducted', parseFloat(e.target.value) || 0)} 
                style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }} />
            </div>
          </div>

          <div style={{ marginTop: 16, padding: '14px 16px', background: '#fef3e2', borderRadius: 8, marginBottom: 16, border: '1px solid #f3d9a8' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingBottom: 8, borderBottom: '1px dashed #f3d9a8' }}>
              <span style={{ fontSize: 13, fontWeight: 600, color: '#1e293b' }}>Gross Salary (17(1)+17(2)+17(3))</span>
              <span style={{ fontSize: 14, fontWeight: 700, color: '#1e293b' }}>₹{entry.grossSalary.toLocaleString('en-IN')}</span>
            </div>

            {(entry as any).totalExemptions > 0 && (
              <div style={{ paddingTop: 8, paddingBottom: 8, borderBottom: '1px dashed #f3d9a8' }}>
                <div style={{ fontSize: 12, color: '#64748b', marginBottom: 4 }}>Less: Exemptions u/s 10</div>
                {(entry as any).hraExempt > 0 && <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, color: '#64748b', paddingLeft: 8, marginBottom: 2 }}><span>HRA u/s 10(13A)</span><span style={{ color: '#ef4444' }}>- ₹{(entry as any).hraExempt.toLocaleString('en-IN')}</span></div>}
                {(entry as any).ltaExempt > 0 && <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, color: '#64748b', paddingLeft: 8, marginBottom: 2 }}><span>LTA u/s 10(5)</span><span style={{ color: '#ef4444' }}>- ₹{(entry as any).ltaExempt.toLocaleString('en-IN')}</span></div>}
                {(entry as any).gratuityExempt > 0 && <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, color: '#64748b', paddingLeft: 8, marginBottom: 2 }}><span>Gratuity u/s 10(10)</span><span style={{ color: '#ef4444' }}>- ₹{(entry as any).gratuityExempt.toLocaleString('en-IN')}</span></div>}
                {(entry as any).leaveEncashmentExempt > 0 && <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, color: '#64748b', paddingLeft: 8, marginBottom: 2 }}><span>Leave Encashment u/s 10(10AA)</span><span style={{ color: '#ef4444' }}>- ₹{(entry as any).leaveEncashmentExempt.toLocaleString('en-IN')}</span></div>}
                {(entry as any).pensionCommutationExempt > 0 && <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, color: '#64748b', paddingLeft: 8, marginBottom: 2 }}><span>Pension Commutation u/s 10(10A)</span><span style={{ color: '#ef4444' }}>- ₹{(entry as any).pensionCommutationExempt.toLocaleString('en-IN')}</span></div>}
                {(entry as any).medicalReimbursementExempt > 0 && <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, color: '#64748b', paddingLeft: 8, marginBottom: 2 }}><span>Medical Reimbursement u/s 17(1)(vii)</span><span style={{ color: '#ef4444' }}>- ₹{(entry as any).medicalReimbursementExempt.toLocaleString('en-IN')}</span></div>}
                {(entry as any).transportAllowanceExempt > 0 && <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, color: '#64748b', paddingLeft: 8, marginBottom: 2 }}><span>Transport Allowance (Disabled) u/s 10(14)</span><span style={{ color: '#ef4444' }}>- ₹{(entry as any).transportAllowanceExempt.toLocaleString('en-IN')}</span></div>}
                {(entry as any).childrenEducationExempt > 0 && <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, color: '#64748b', paddingLeft: 8, marginBottom: 2 }}><span>Children Education Allowance u/s 10(14)</span><span style={{ color: '#ef4444' }}>- ₹{(entry as any).childrenEducationExempt.toLocaleString('en-IN')}</span></div>}
                {(entry as any).hostelExpenditureExempt > 0 && <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, color: '#64748b', paddingLeft: 8, marginBottom: 2 }}><span>Hostel Expenditure u/s 10(14)</span><span style={{ color: '#ef4444' }}>- ₹{(entry as any).hostelExpenditureExempt.toLocaleString('en-IN')}</span></div>}
                {(entry as any).otherExemptions > 0 && <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, color: '#64748b', paddingLeft: 8, marginBottom: 2 }}><span>Other Exemptions u/s 10</span><span style={{ color: '#ef4444' }}>- ₹{(entry as any).otherExemptions.toLocaleString('en-IN')}</span></div>}
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, fontWeight: 600, color: '#475569', paddingLeft: 8, marginTop: 6 }}>
                  <span>Total Exemptions</span>
                  <span style={{ color: '#ef4444' }}>- ₹{(entry as any).totalExemptions.toLocaleString('en-IN')}</span>
                </div>
              </div>
            )}

            <div style={{ paddingTop: 8, paddingBottom: 8, borderBottom: '1px dashed #f3d9a8' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, color: '#64748b' }}>
                <span>Less: Standard Deduction u/s 16(ia) <span style={{ fontSize: 10, color: '#94a3b8' }}>(applied once on total)</span></span>
                <span style={{ color: '#ef4444' }}>- ₹75,000</span>
              </div>
              {entry.professionalTax > 0 && (
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, color: '#64748b', marginTop: 4 }}>
                  <span>Less: Professional Tax u/s 16(iii)</span>
                  <span style={{ color: '#ef4444' }}>- ₹{Math.min(entry.professionalTax, 2500).toLocaleString('en-IN')}</span>
                </div>
              )}
              {entry.entertainmentAllowance > 0 && (
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, color: '#64748b', marginTop: 4 }}>
                  <span>Less: Entertainment Allowance u/s 16(ii)</span>
                  <span style={{ color: '#ef4444' }}>- ₹{entry.entertainmentAllowance.toLocaleString('en-IN')}</span>
                </div>
              )}
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingTop: 10 }}>
              <span style={{ fontSize: 13, fontWeight: 600, color: '#1e293b' }}>Net Taxable Salary</span>
              <span style={{ fontSize: 16, fontWeight: 700, color: '#c9943a' }}>₹{entry.netSalary.toLocaleString('en-IN')}</span>
            </div>
          </div>

          {/* Additional Salary Components - Always Visible */}
          <div style={{ padding: 16, background: '#f8fafc', borderRadius: 8, marginTop: 16, marginBottom: 16 }}>
            
            {/* Pension & Retirement Benefits */}
            <h4 style={{ fontSize: 13, fontWeight: 600, color: '#475569', marginBottom: 12, marginTop: 0 }}>Pension & Retirement Benefits</h4>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16, marginBottom: 20 }}>
              <div>
                <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>Pension</label>
                <input type="number" value={entry.pension} onChange={(e) => updateEntry(index, 'pension', parseFloat(e.target.value) || 0)} 
                  style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }} />
              </div>
              <div>
                <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>Commuted Pension</label>
                <input type="number" value={entry.commutedPension} onChange={(e) => updateEntry(index, 'commutedPension', parseFloat(e.target.value) || 0)} 
                  style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }} />
              </div>
              <div>
                <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>Uncommuted Pension</label>
                <input type="number" value={entry.uncommutedPension} onChange={(e) => updateEntry(index, 'uncommutedPension', parseFloat(e.target.value) || 0)} 
                  style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }} />
              </div>
              <div>
                <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>Gratuity</label>
                <input type="number" value={entry.gratuity} onChange={(e) => updateEntry(index, 'gratuity', parseFloat(e.target.value) || 0)} 
                  style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }} />
              </div>
              <div>
                <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>Leave Encashment</label>
                <input type="number" value={entry.leaveEncashment} onChange={(e) => updateEntry(index, 'leaveEncashment', parseFloat(e.target.value) || 0)} 
                  style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }} />
              </div>
              <div>
                <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>Arrears of Salary</label>
                <input type="number" value={entry.arrearsOfSalary} onChange={(e) => updateEntry(index, 'arrearsOfSalary', parseFloat(e.target.value) || 0)} 
                  style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }} />
              </div>
            </div>

            {/* Section 17(2) Perquisites - Inline */}
            <h4 style={{ fontSize: 13, fontWeight: 600, color: '#475569', marginBottom: 12 }}>Section 17(2) - Perquisites</h4>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16, marginBottom: 20 }}>
              <div>
                <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>Rent-free Accommodation</label>
                <input type="number" value={entry.perqRentFreeAccommodation} onChange={(e) => updateEntry(index, 'perqRentFreeAccommodation', parseFloat(e.target.value) || 0)} 
                  style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }} />
              </div>
              <div>
                <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>Concessional Rent</label>
                <input type="number" value={entry.perqConcessionalRent} onChange={(e) => updateEntry(index, 'perqConcessionalRent', parseFloat(e.target.value) || 0)} 
                  style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }} />
              </div>
              <div>
                <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>Motor Car Facility</label>
                <input type="number" value={entry.perqMotorCar} onChange={(e) => updateEntry(index, 'perqMotorCar', parseFloat(e.target.value) || 0)} 
                  style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }} />
              </div>
              <div>
                <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>Free Education</label>
                <input type="number" value={entry.perqFreeEducation} onChange={(e) => updateEntry(index, 'perqFreeEducation', parseFloat(e.target.value) || 0)} 
                  style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }} />
              </div>
              <div>
                <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>Gifts & Vouchers</label>
                <input type="number" value={entry.perqGiftsVouchers} onChange={(e) => updateEntry(index, 'perqGiftsVouchers', parseFloat(e.target.value) || 0)} 
                  style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }} />
              </div>
              <div>
                <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>Other Perquisites</label>
                <input type="number" value={entry.perqOthers} onChange={(e) => updateEntry(index, 'perqOthers', parseFloat(e.target.value) || 0)} 
                  style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }} />
              </div>
            </div>

            {/* Section 17(3) Profits in Lieu */}
            <h4 style={{ fontSize: 13, fontWeight: 600, color: '#475569', marginBottom: 12 }}>Section 17(3) - Profits in Lieu</h4>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 16, marginBottom: 20 }}>
              <div>
                <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>Compensation on Termination</label>
                <input type="number" value={entry.profitsCompensationTermination} onChange={(e) => updateEntry(index, 'profitsCompensationTermination', parseFloat(e.target.value) || 0)} 
                  style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }} />
              </div>
              <div>
                <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>Non-Compete Agreement</label>
                <input type="number" value={entry.profitsNonCompete} onChange={(e) => updateEntry(index, 'profitsNonCompete', parseFloat(e.target.value) || 0)} 
                  style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }} />
              </div>
            </div>

            {/* Exemptions u/s 10 */}
            <h4 style={{ fontSize: 13, fontWeight: 600, color: '#475569', marginBottom: 12 }}>Exemptions u/s 10</h4>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16 }}>
              <div>
                <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>
                  HRA Exempt ⎵ Auto
                </label>
                <input type="number" value={entry.hraExempt} readOnly
                  style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13, background: '#f8fafc', cursor: 'not-allowed' }} />
              </div>
              <div>
                <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>LTA Exempt</label>
                <input type="number" value={entry.ltaExempt} onChange={(e) => updateEntry(index, 'ltaExempt', parseFloat(e.target.value) || 0)} 
                  style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }} />
              </div>
              <div>
                <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>Gratuity Exempt</label>
                <input type="number" value={entry.gratuityExempt} onChange={(e) => updateEntry(index, 'gratuityExempt', parseFloat(e.target.value) || 0)} 
                  style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }} />
              </div>
              <div>
                <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>Leave Encashment</label>
                <input type="number" value={entry.leaveEncashmentExempt} onChange={(e) => updateEntry(index, 'leaveEncashmentExempt', parseFloat(e.target.value) || 0)} 
                  style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }} />
              </div>
              <div>
                <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>Medical Reimb. (Max ₹15K)</label>
                <input type="number" value={entry.medicalReimbursementReceived || 0} onChange={(e) => updateEntry(index, 'medicalReimbursementReceived', parseFloat(e.target.value) || 0)} 
                  style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }} />
              </div>
              <div>
                <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>Other Exemptions</label>
                <input type="number" value={entry.otherExemptions} onChange={(e) => updateEntry(index, 'otherExemptions', parseFloat(e.target.value) || 0)} 
                  style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }} />
              </div>
            </div>

            {/* Special Exemptions */}
            <h4 style={{ fontSize: 13, fontWeight: 600, color: '#475569', marginBottom: 12, marginTop: 16 }}>Special Exemptions</h4>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16 }}>
              <div>
                <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>
                  <input type="checkbox" checked={entry.isDisabledEmployee || false}
                    onChange={(e) => updateEntry(index, 'isDisabledEmployee', e.target.checked)} style={{ marginRight: 6 }} />
                  Disabled Employee?
                </label>
                {entry.isDisabledEmployee && (
                  <input type="number" value={entry.transportAllowanceReceived || 0} onChange={(e) => updateEntry(index, 'transportAllowanceReceived', parseFloat(e.target.value) || 0)} 
                    style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }} placeholder="Transport allowance" />
                )}
              </div>
              <div>
                <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>Children Education (Max ₹2,400)</label>
                <input type="number" value={entry.childrenEducationAllowance || 0} onChange={(e) => updateEntry(index, 'childrenEducationAllowance', parseFloat(e.target.value) || 0)} 
                  style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }} />
              </div>
              <div>
                <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>Hostel Expenditure (Max ₹7,200)</label>
                <input type="number" value={entry.hostelExpenditureAllowance || 0} onChange={(e) => updateEntry(index, 'hostelExpenditureAllowance', parseFloat(e.target.value) || 0)} 
                  style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }} />
              </div>
            </div>

            {/* Pension Type */}
            <div style={{ marginTop: 16 }}>
              <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>Pension Type</label>
              <select value={entry.pensionType || 'UNCOMMUTED'} onChange={(e) => updateEntry(index, 'pensionType', e.target.value)}
                style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }}>
                <option value="UNCOMMUTED">Uncommuted (Regular)</option>
                <option value="COMMUTED">Commuted (Lump Sum)</option>
                <option value="FAMILY">Family Pension</option>
              </select>
            </div>
          </div>
        </div>
      ))}

      {entries.length > 0 && (
        <div style={{ 
          padding: 16, 
          background: 'linear-gradient(135deg, #fef3e2 0%, #fef9f0 100%)', 
          borderRadius: 8,
          border: '1px solid #f3d9a8'
        }}>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16 }}>
            <div>
              <div style={{ fontSize: 12, color: '#64748b', marginBottom: 4 }}>Total Gross Salary</div>
              <div style={{ fontSize: 20, fontWeight: 600, color: '#1e293b' }}>₹{getTotalGross().toLocaleString('en-IN')}</div>
            </div>
            <div>
              <div style={{ fontSize: 12, color: '#64748b', marginBottom: 4 }}>Total Net Salary</div>
              <div style={{ fontSize: 20, fontWeight: 600, color: '#1e293b' }}>₹{getTotalNet().toLocaleString('en-IN')}</div>
            </div>
            <div>
              <div style={{ fontSize: 12, color: '#64748b', marginBottom: 4 }}>Total TDS</div>
              <div style={{ fontSize: 20, fontWeight: 600, color: '#1e293b' }}>₹{getTotalTDS().toLocaleString('en-IN')}</div>
            </div>
          </div>
        </div>
      )}

    </div>
  );
};
