import React, { useState, useCallback } from 'react';
import { useQuery, useMutation } from '@tanstack/react-query';
import { useAppStore } from '../store/useAppStore';
import { useDebounce } from '../hooks/useDebounce';
import { filingApi } from '../api/filing';
import Badge from '../components/ui/Badge';
import Button from '../components/ui/Button';
import PersonalDetailsForm from '../components/computation/PersonalDetailsForm';
import ScheduleSalaryForm from '../components/computation/ScheduleSalaryForm';
import ScheduleHPForm from '../components/computation/ScheduleHPForm';
import ScheduleCGForm from '../components/computation/ScheduleCGForm';
import ScheduleVDAForm from '../components/computation/ScheduleVDAForm';
import ScheduleOSForm from '../components/computation/ScheduleOSForm';
import DeductionsForm from '../components/computation/DeductionsForm';
import TDSTaxPaidForm from '../components/computation/TDSTaxPaidForm';
import LossSetOffForm from '../components/computation/LossSetOffForm';
import LiveSummaryPanel from '../components/computation/LiveSummaryPanel';
import toast from 'react-hot-toast';

type Section = 'personal' | 'salary' | 'hp' | 'cg' | 'vda' | 'os' | 'deductions' | 'tds' | 'loss';

const NAV_ITEMS = [
  { id: 'personal', icon: '👤', label: 'Personal Details' },
  { id: 'salary', icon: '💼', label: 'Schedule: Salary' },
  { id: 'hp', icon: '🏠', label: 'Schedule: House Property' },
  { id: 'cg', icon: '📈', label: 'Schedule: Capital Gains' },
  { id: 'vda', icon: '₿', label: 'Schedule: VDA / Crypto' },
  { id: 'os', icon: '💰', label: 'Schedule: Other Sources' },
  { id: 'deductions', icon: '🧾', label: 'Chapter VI-A Deductions' },
  { id: 'tds', icon: '🏦', label: 'TDS & Tax Paid' },
  { id: 'loss', icon: '📉', label: 'Loss Set-Off' },
];

export default function Computation() {
  const { computationClientId, closeComputation, activeAY } = useAppStore();
  const [activeSection, setActiveSection] = useState<Section>('personal');
  const [formData, setFormData] = useState({
    clientId: computationClientId,
    assessmentYear: activeAY,
    pan: 'ABCDE1234F',
    name: 'Sample Client',
    itrForm: 'ITR-2',
    taxRegime: 'NEW',
    personalDetails: {},
    scheduleSalary: {},
    scheduleHP: {},
    scheduleOS: {},
    deductions: {},
    tdsTaxPaid: {},
  });

  // Load saved ITR data
  const { data: savedData } = useQuery({
    queryKey: ['itr-data', computationClientId, activeAY],
    queryFn: () => filingApi.getITRData(computationClientId!, activeAY),
    enabled: !!computationClientId,
  });

  // Update form when data loads
  React.useEffect(() => {
    if (savedData) {
      // Map backend Itr1FormData structure to frontend formData structure
      const mappedData = {
        clientId: computationClientId,
        assessmentYear: activeAY,
        pan: savedData.personalInfo?.pan || '',
        name: savedData.personalInfo?.assesseeName || '',
        itrForm: savedData.personalInfo?.regime === 'NEW' ? 'ITR-1' : 'ITR-1',
        taxRegime: savedData.personalInfo?.regime || 'NEW',
        
        // Map all schedules from backend structure
        personalDetails: savedData.personalInfo || {},
        scheduleSalary: savedData.salaryIncome || {},
        scheduleHP: savedData.housePropertyIncome || {},
        scheduleCG: savedData.capitalGains || {},
        scheduleVDA: savedData.vdaIncome || {},
        scheduleOS: savedData.otherSourcesIncome || {},
        deductions: savedData.deductions || {},
        tdsTaxPaid: savedData.taxPayments || {},
        lossSetOff: savedData.lossSetOff || {},
        
        // Include computation results if available
        taxComputation: savedData.taxComputation || {},
      };
      
      setFormData(mappedData);
    }
  }, [savedData, computationClientId, activeAY]);

  // Debounced computation - only if personalInfo exists
  const debouncedFormData = useDebounce(formData, 800);
  const { data: computedResult } = useQuery({
    queryKey: ['compute', debouncedFormData],
    queryFn: () => {
      // Build proper Itr1FormData structure for backend
      const itr1Data = {
        personalInfo: formData.personalDetails || {
          pan: formData.pan,
          assesseeName: formData.name,
          assessmentYear: activeAY,
          regime: formData.taxRegime,
        },
        salaryIncome: formData.scheduleSalary || {},
        housePropertyIncome: formData.scheduleHP || {},
        otherSourcesIncome: formData.scheduleOS || {},
        deductions: formData.deductions || {},
        taxPayments: formData.tdsTaxPaid || {},
      };
      return filingApi.compute(itr1Data);
    },
    enabled: !!formData.pan && formData.pan.length === 10,
  });

  // Validation - only if personalInfo exists
  const { data: validation } = useQuery({
    queryKey: ['validate', debouncedFormData],
    queryFn: () => {
      const itr1Data = {
        personalInfo: formData.personalDetails || {
          pan: formData.pan,
          assesseeName: formData.name,
          assessmentYear: activeAY,
          regime: formData.taxRegime,
        },
        salaryIncome: formData.scheduleSalary || {},
        housePropertyIncome: formData.scheduleHP || {},
        otherSourcesIncome: formData.scheduleOS || {},
        deductions: formData.deductions || {},
        taxPayments: formData.tdsTaxPaid || {},
      };
      return filingApi.validate(itr1Data);
    },
    enabled: !!formData.pan && formData.pan.length === 10,
  });

  // Save draft mutation
  const saveDraft = useMutation({
    mutationFn: () => filingApi.saveDraft(computationClientId!, formData),
    onSuccess: () => toast.success('Draft saved'),
    onError: () => toast.error('Save failed'),
  });

  // Submit mutation
  const submit = useMutation({
    mutationFn: () => filingApi.submitITR({ ...formData, taxComputation: computedResult?.taxComputation }),
    onSuccess: () => {
      toast.success('ITR submitted successfully!');
      closeComputation();
    },
    onError: (err: any) => toast.error(err?.response?.data?.message ?? 'Filing failed'),
  });

  // Export JSON
  const exportJSON = async () => {
    const blob = await filingApi.exportJSON(computationClientId!, activeAY);
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `ITR_${formData.pan}_${activeAY}.json`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const updateField = useCallback((path: string, value: any) => {
    setFormData(prev => {
      const parts = path.split('.');
      if (parts.length === 1) {
        return { ...prev, [path]: value };
      }
      const [section, field] = parts;
      return {
        ...prev,
        [section]: {
          ...(prev[section as keyof typeof prev] as any),
          [field]: value,
        },
      };
    });
  }, []);

  if (!computationClientId) return null;

  return (
    <div className="fixed inset-0 z-[300] bg-bg flex flex-col">
      {/* Topbar */}
      <div className="h-[62px] bg-bg-card border-b border-border flex items-center px-6 gap-4 flex-shrink-0">
        <button 
          onClick={closeComputation} 
          className="flex items-center gap-2 text-[13px] font-medium text-text-secondary hover:text-text-primary transition-colors"
        >
          <svg width="14" height="14" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M12 18l-6-6 6-6"/>
          </svg>
          Back
        </button>
        <div className="h-5 w-px bg-border" />
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 bg-navy rounded-lg flex items-center justify-center font-serif text-[14px] text-gold-light font-semibold">
            {formData.pan?.slice(0, 2) ?? '??'}
          </div>
          <div>
            <div className="text-[14px] font-semibold">{formData.name}</div>
            <div className="font-mono text-[11.5px] text-text-muted">{formData.pan}</div>
          </div>
        </div>
        <div className="flex items-center gap-2 ml-4">
          <Badge label={formData.itrForm} variant="blue" />
          <Badge label={`AY ${activeAY}`} variant="neutral" />
          <Badge label={formData.taxRegime === 'NEW' ? 'New Regime' : 'Old Regime'} variant="info" />
        </div>
        <div className="ml-auto flex items-center gap-2.5">
          <Button variant="ghost" size="sm">Save Draft</Button>
          <Button variant="primary" size="sm">Submit ITR</Button>
        </div>
      </div>

      {/* 3-column body */}
      <div className="flex flex-1 overflow-hidden">
        {/* Left nav */}
        <nav className="w-[260px] flex-shrink-0 bg-bg-card border-r border-border overflow-y-auto py-4 px-2.5">
          {NAV_ITEMS.map((section) => (
            <button
              key={section.id}
              onClick={() => setActiveSection(section.id as Section)}
              className={`relative w-full text-left flex items-center gap-2 px-3 py-2.5 rounded-lg text-[13.5px] mb-0.5 transition-all ${
                activeSection === section.id
                  ? 'bg-gold/[0.12] text-[#92640A] font-medium'
                  : 'text-text-secondary hover:bg-bg hover:text-text-primary'
              }`}
            >
              {activeSection === section.id && (
                <span className="absolute left-0 top-1/2 -translate-y-1/2 w-[3px] h-5 bg-gold rounded-r-sm" />
              )}
              <span className="text-[16px]">{section.icon}</span>
              <span>{section.label}</span>
            </button>
          ))}
        </nav>

        {/* Center form */}
        <div className="flex-1 overflow-y-auto p-7">
          <div className="max-w-4xl">
            {activeSection === 'personal' && (
              <PersonalDetailsForm formData={formData} onChange={updateField} />
            )}
            {activeSection === 'salary' && (
              <ScheduleSalaryForm formData={formData} onChange={updateField} />
            )}
            {activeSection === 'hp' && (
              <ScheduleHPForm formData={formData} onChange={updateField} />
            )}
            {activeSection === 'cg' && (
              <ScheduleCGForm formData={formData} onChange={updateField} />
            )}
            {activeSection === 'vda' && (
              <ScheduleVDAForm formData={formData} onChange={updateField} />
            )}
            {activeSection === 'os' && (
              <ScheduleOSForm formData={formData} onChange={updateField} />
            )}
            {activeSection === 'deductions' && (
              <DeductionsForm formData={formData} onChange={updateField} />
            )}
            {activeSection === 'tds' && (
              <TDSTaxPaidForm formData={formData} onChange={updateField} clientId={computationClientId} />
            )}
            {activeSection === 'loss' && (
              <LossSetOffForm formData={formData} onChange={updateField} />
            )}
          </div>
        </div>

        {/* Right summary */}
        <LiveSummaryPanel
          formData={formData}
          validation={validation}
          onSubmit={() => submit.mutate()}
          onSaveDraft={() => saveDraft.mutate()}
          onExportJSON={exportJSON}
          isSubmitting={submit.isPending}
          hasBlocker={!!validation?.catAErrors?.length}
        />
      </div>
    </div>
  );
}
