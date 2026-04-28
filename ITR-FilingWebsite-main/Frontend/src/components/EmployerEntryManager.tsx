// Multiple Employer Entry Manager - CBDT Compliant
import React from 'react';
import { calculateSalary, type EmployerInput } from '../services/salaryCalculationService';

interface EmployerEntry {
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
  
  // Exemptions u/s 10
  hraExempt: number;
  ltaExempt: number;
  gratuityExempt: number;
  leaveEncashmentExempt: number;
  otherExemptions: number;
  
  // Deductions u/s 16
  professionalTax: number;
  entertainmentAllowance: number;
  
  tdsDeducted: number;
  grossSalary: number;
  netSalary: number;
}

interface EmployerEntryManagerProps {
  entries: EmployerEntry[];
  onChange: (entries: EmployerEntry[]) => void;
  assessmentYear: string;
}

export const EmployerEntryManager: React.FC<EmployerEntryManagerProps> = ({ entries, onChange, assessmentYear }) => {
  const [expandedAdvanced, setExpandedAdvanced] = React.useState<{[key: number]: boolean}>({});
  const [perquisitesModalOpen, setPerquisitesModalOpen] = React.useState<number | null>(null);
  
  const getStandardDeduction = () => assessmentYear === '2026-27' ? 75000 : 50000;

  const toggleAdvanced = (entryIndex: number) => {
    setExpandedAdvanced(prev => ({
      ...prev,
      [entryIndex]: !prev[entryIndex]
    }));
  };

  const openPerquisitesModal = (entryIndex: number) => {
    setPerquisitesModalOpen(entryIndex);
  };

  const closePerquisitesModal = () => {
    setPerquisitesModalOpen(null);
  };

  const addEntry = () => {
    const newEntry: EmployerEntry = {
      employerName: '',
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
        employerName: entry.employerName,
        employerTAN: entry.employerTAN,
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
        profitsCompensationTermination: entry.profitsCompensationTermination || 0,
        profitsNonCompete: entry.profitsNonCompete || 0,
        ltaExempt: entry.ltaExempt || 0,
        gratuityExempt: entry.gratuityExempt || 0,
        leaveEncashmentExempt: entry.leaveEncashmentExempt || 0,
        otherExemptions: entry.otherExemptions || 0,
        professionalTax: entry.professionalTax || 0,
        entertainmentAllowance: entry.entertainmentAllowance || 0,
        tdsDeducted: entry.tdsDeducted || 0,
      }));

      console.log('Calling API with inputs:', employerInputs);
      console.log('Full request payload:', JSON.stringify({ assessmentYear, employers: employerInputs }, null, 2));
      const response = await calculateSalary(assessmentYear, employerInputs);
      console.log('API Response:', response);
      console.log('Response structure:', JSON.stringify(response, null, 2));
      
      const recalculated = updatedEntries.map((entry, idx) => {
        const calc = response.employers[idx];
        console.log(`Employer ${idx}: Gross=${calc.grossSalary}, Net=${calc.netSalary}`);
        return {
          ...entry,
          hraExempt: calc.hraExempt,
          grossSalary: calc.grossSalary,
          netSalary: calc.netSalary,
          perquisites: calc.perquisites,
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

  const getTotalGross = () => entries.reduce((sum, e) => sum + e.grossSalary, 0);
  const getTotalNet = () => entries.reduce((sum, e) => sum + e.netSalary, 0);
  const getTotalTDS = () => entries.reduce((sum, e) => sum + e.tdsDeducted, 0);

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
            <span style={{ 
              background: '#c9943a', 
              color: 'white', 
              padding: '6px 12px', 
              borderRadius: 6, 
              fontSize: 13,
              fontWeight: 500
            }}>
              Employer {index + 1}
            </span>
            <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
              <button 
                onClick={() => openPerquisitesModal(index)} 
                style={{ 
                  background: '#f1f5f9',
                  color: '#64748b',
                  border: '1px solid #e2e8f0',
                  padding: '6px 12px',
                  borderRadius: 6,
                  cursor: 'pointer',
                  fontSize: 12,
                  fontWeight: 500,
                  transition: 'all 0.2s'
                }}
              >
                Detailed Allowances
              </button>
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

          <div style={{ marginTop: 16, padding: 12, background: '#fef3e2', borderRadius: 6, display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 12, marginBottom: 16 }}>
            <div>
              <div style={{ fontSize: 11, color: '#64748b' }}>Gross Salary</div>
              <div style={{ fontSize: 16, fontWeight: 600, color: '#1e293b' }}>₹{entry.grossSalary.toLocaleString('en-IN')}</div>
            </div>
            <div>
              <div style={{ fontSize: 11, color: '#64748b' }}>Net Taxable Salary</div>
              <div style={{ fontSize: 16, fontWeight: 600, color: '#1e293b' }}>₹{entry.netSalary.toLocaleString('en-IN')}</div>
            </div>
          </div>

          {/* Optional Fields Expandable Section */}
          <button
            onClick={() => toggleAdvanced(index)}
            style={{
              width: '100%',
              padding: '12px 16px',
              background: '#f1f5f9',
              border: '2px solid #e2e8f0',
              borderRadius: 8,
              fontSize: 13,
              fontWeight: 600,
              color: '#475569',
              cursor: 'pointer',
              textAlign: 'left',
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              transition: 'all 0.2s',
              marginBottom: 12
            }}
          >
            <span>Optional Fields (Pension, Profits, Exemptions)</span>
            <span style={{ fontSize: 16 }}>{expandedAdvanced[index] ? '▼' : '▶'}</span>
          </button>
          
          {expandedAdvanced[index] && (
            <div style={{ padding: 16, background: '#f8fafc', borderRadius: 8, marginBottom: 16 }}>
              
              {/* Pension & Retirement Benefits */}
              <h5 style={{ fontSize: 12, fontWeight: 600, color: '#475569', marginBottom: 12, marginTop: 0 }}>Pension & Retirement Benefits</h5>
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

              {/* Profits in Lieu of Salary */}
              <h5 style={{ fontSize: 12, fontWeight: 600, color: '#475569', marginBottom: 12 }}>Section 17(3) - Profits in Lieu of Salary</h5>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 16, marginBottom: 20 }}>
                <div>
                  <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>Compensation on Termination</label>
                  <input type="number" value={entry.profitsCompensationTermination} onChange={(e) => updateEntry(index, 'profitsCompensationTermination', parseFloat(e.target.value) || 0)} 
                    style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }} />
                </div>
                <div>
                  <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>Payment for Non-Compete Agreement</label>
                  <input type="number" value={entry.profitsNonCompete} onChange={(e) => updateEntry(index, 'profitsNonCompete', parseFloat(e.target.value) || 0)} 
                    style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }} />
                </div>
              </div>

              {/* Exemptions u/s 10 */}
              <h5 style={{ fontSize: 12, fontWeight: 600, color: '#475569', marginBottom: 12 }}>Exemptions u/s 10</h5>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16 }}>
                <div>
                  <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>
                    HRA Exempt u/s 10(13A) 
                    <span style={{ fontSize: 10, color: '#10b981', marginLeft: 4 }}>● Auto-calculated</span>
                  </label>
                  <input type="number" value={entry.hraExempt} readOnly
                    style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13, background: '#f8fafc', cursor: 'not-allowed' }} />
                </div>
                <div>
                  <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>LTA Exempt u/s 10(5)</label>
                  <input type="number" value={entry.ltaExempt} onChange={(e) => updateEntry(index, 'ltaExempt', parseFloat(e.target.value) || 0)} 
                    style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }} />
                </div>
                <div>
                  <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>Gratuity Exempt u/s 10(10)</label>
                  <input type="number" value={entry.gratuityExempt} onChange={(e) => updateEntry(index, 'gratuityExempt', parseFloat(e.target.value) || 0)} 
                    style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }} />
                </div>
                <div>
                  <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>Leave Encashment Exempt u/s 10(10AA)</label>
                  <input type="number" value={entry.leaveEncashmentExempt} onChange={(e) => updateEntry(index, 'leaveEncashmentExempt', parseFloat(e.target.value) || 0)} 
                    style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }} />
                </div>
                <div>
                  <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>Other Exemptions u/s 10</label>
                  <input type="number" value={entry.otherExemptions} onChange={(e) => updateEntry(index, 'otherExemptions', parseFloat(e.target.value) || 0)} 
                    style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }} />
                </div>
              </div>
            </div>
          )}
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

      {/* Perquisites Modal */}
      {perquisitesModalOpen !== null && (
        <div 
          style={{ 
            position: 'fixed', 
            top: 0, 
            left: 0, 
            right: 0, 
            bottom: 0, 
            background: 'rgba(0, 0, 0, 0.5)', 
            display: 'flex', 
            alignItems: 'center', 
            justifyContent: 'center',
            zIndex: 1000
          }}
          onClick={closePerquisitesModal}
        >
          <div 
            style={{ 
              background: 'white', 
              borderRadius: 12, 
              padding: 24, 
              maxWidth: 900, 
              width: '90%',
              maxHeight: '80vh',
              overflow: 'auto',
              boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04)'
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
              <h3 style={{ margin: 0, fontSize: 18, fontWeight: 600, color: '#1e293b' }}>
                Section 17(2) - Perquisites Breakdown
              </h3>
              <button 
                onClick={closePerquisitesModal}
                style={{ 
                  background: 'none', 
                  border: 'none', 
                  fontSize: 24, 
                  cursor: 'pointer', 
                  color: '#64748b',
                  padding: 0,
                  width: 32,
                  height: 32,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center'
                }}
              >
                ×
              </button>
            </div>
            
            <div style={{ marginBottom: 16, padding: 12, background: '#fef3e2', borderRadius: 6, fontSize: 12, color: '#64748b' }}>
              Enter detailed perquisites breakdown. Total will auto-calculate and be included in gross salary.
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 16 }}>
              <div>
                <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>Rent-free Accommodation</label>
                <input type="number" value={entries[perquisitesModalOpen].perqRentFreeAccommodation} onChange={(e) => updateEntry(perquisitesModalOpen, 'perqRentFreeAccommodation', parseFloat(e.target.value) || 0)} 
                  style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }} />
              </div>
              <div>
                <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>Concessional Rent</label>
                <input type="number" value={entries[perquisitesModalOpen].perqConcessionalRent} onChange={(e) => updateEntry(perquisitesModalOpen, 'perqConcessionalRent', parseFloat(e.target.value) || 0)} 
                  style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }} />
              </div>
              <div>
                <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>Motor Car Facility</label>
                <input type="number" value={entries[perquisitesModalOpen].perqMotorCar} onChange={(e) => updateEntry(perquisitesModalOpen, 'perqMotorCar', parseFloat(e.target.value) || 0)} 
                  style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }} />
              </div>
              <div>
                <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>Sweeper/Gardener/Watchman</label>
                <input type="number" value={entries[perquisitesModalOpen].perqSweeper} onChange={(e) => updateEntry(perquisitesModalOpen, 'perqSweeper', parseFloat(e.target.value) || 0)} 
                  style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }} />
              </div>
              <div>
                <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>Gas/Electricity/Water</label>
                <input type="number" value={entries[perquisitesModalOpen].perqGasElectricityWater} onChange={(e) => updateEntry(perquisitesModalOpen, 'perqGasElectricityWater', parseFloat(e.target.value) || 0)} 
                  style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }} />
              </div>
              <div>
                <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>Interest-free/Concessional Loan</label>
                <input type="number" value={entries[perquisitesModalOpen].perqInterestFreeLoan} onChange={(e) => updateEntry(perquisitesModalOpen, 'perqInterestFreeLoan', parseFloat(e.target.value) || 0)} 
                  style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }} />
              </div>
              <div>
                <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>Holiday Expenses</label>
                <input type="number" value={entries[perquisitesModalOpen].perqHolidayExpenses} onChange={(e) => updateEntry(perquisitesModalOpen, 'perqHolidayExpenses', parseFloat(e.target.value) || 0)} 
                  style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }} />
              </div>
              <div>
                <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>Free/Concessional Education</label>
                <input type="number" value={entries[perquisitesModalOpen].perqFreeEducation} onChange={(e) => updateEntry(perquisitesModalOpen, 'perqFreeEducation', parseFloat(e.target.value) || 0)} 
                  style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }} />
              </div>
              <div>
                <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>Gifts/Vouchers/Tokens</label>
                <input type="number" value={entries[perquisitesModalOpen].perqGiftsVouchers} onChange={(e) => updateEntry(perquisitesModalOpen, 'perqGiftsVouchers', parseFloat(e.target.value) || 0)} 
                  style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }} />
              </div>
              <div>
                <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>Credit Card Expenses</label>
                <input type="number" value={entries[perquisitesModalOpen].perqCreditCard} onChange={(e) => updateEntry(perquisitesModalOpen, 'perqCreditCard', parseFloat(e.target.value) || 0)} 
                  style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }} />
              </div>
              <div>
                <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>Club Expenses</label>
                <input type="number" value={entries[perquisitesModalOpen].perqClubExpenses} onChange={(e) => updateEntry(perquisitesModalOpen, 'perqClubExpenses', parseFloat(e.target.value) || 0)} 
                  style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }} />
              </div>
              <div>
                <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>Use of Movable Assets</label>
                <input type="number" value={entries[perquisitesModalOpen].perqMovableAssets} onChange={(e) => updateEntry(perquisitesModalOpen, 'perqMovableAssets', parseFloat(e.target.value) || 0)} 
                  style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }} />
              </div>
              <div>
                <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: '#64748b' }}>Other Perquisites</label>
                <input type="number" value={entries[perquisitesModalOpen].perqOthers} onChange={(e) => updateEntry(perquisitesModalOpen, 'perqOthers', parseFloat(e.target.value) || 0)} 
                  style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 13 }} />
              </div>
            </div>

            <div style={{ marginTop: 20, display: 'flex', justifyContent: 'flex-end' }}>
              <button 
                onClick={closePerquisitesModal}
                style={{ 
                  background: '#3b82f6', 
                  color: 'white', 
                  border: 'none', 
                  padding: '10px 24px', 
                  borderRadius: 6, 
                  cursor: 'pointer', 
                  fontSize: 14,
                  fontWeight: 500
                }}
              >
                Done
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
