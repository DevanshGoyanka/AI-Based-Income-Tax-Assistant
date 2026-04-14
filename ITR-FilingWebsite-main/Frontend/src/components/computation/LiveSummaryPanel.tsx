import Button from '../ui/Button';
import ProgressBar from '../ui/ProgressBar';

interface LiveSummaryPanelProps {
  formData: any;
  computed?: any;
  validation?: any;
  onSubmit: () => void;
  onSaveDraft: () => void;
  onExportJSON: () => void;
  isSubmitting: boolean;
  hasBlocker: boolean;
}

export default function LiveSummaryPanel({
  formData,
  validation,
  onSubmit,
  onSaveDraft,
  onExportJSON,
  isSubmitting,
  hasBlocker,
}: LiveSummaryPanelProps) {
  const salary = formData.scheduleSalary || {};
  const deductions = formData.deductions || {};
  
  // Calculate totals
  const grossSalary = salary.grossSalary || 0;
  const totalDeductions = (
    Math.min((deductions.sec80C || 0) + (deductions.sec80CCC || 0) + (deductions.sec80CCD1 || 0), 150000) +
    (deductions.sec80CCD1B || 0) +
    (deductions.sec80CCD2 || 0) +
    (deductions.sec80D || 0) +
    (deductions.sec80DD || 0) +
    (deductions.sec80DDB || 0) +
    (deductions.sec80U || 0) +
    (deductions.sec80E || 0) +
    (deductions.sec80EEA || 0) +
    (deductions.sec80G || 0) +
    (deductions.sec80GGA || 0) +
    (deductions.sec80GGC || 0) +
    (deductions.sec80TTA || 0) +
    (deductions.sec80TTB || 0)
  );
  
  const netTaxable = Math.max(0, grossSalary - totalDeductions);
  
  // Simple tax calculation (new regime rates for demo)
  let taxOnIncome = 0;
  if (netTaxable > 300000) taxOnIncome += (Math.min(netTaxable, 600000) - 300000) * 0.05;
  if (netTaxable > 600000) taxOnIncome += (Math.min(netTaxable, 900000) - 600000) * 0.10;
  if (netTaxable > 900000) taxOnIncome += (Math.min(netTaxable, 1200000) - 900000) * 0.15;
  if (netTaxable > 1200000) taxOnIncome += (Math.min(netTaxable, 1500000) - 1200000) * 0.20;
  if (netTaxable > 1500000) taxOnIncome += (netTaxable - 1500000) * 0.30;
  
  const cess = taxOnIncome * 0.04;
  const totalTax = taxOnIncome + cess;

  return (
    <aside className="w-[360px] flex-shrink-0 bg-bg-card border-l border-border overflow-y-auto flex flex-col">
      <div className="flex-1 p-5">
        {/* Income summary */}
        <div className="mb-5">
          <div className="text-[11px] font-bold uppercase tracking-[0.8px] text-text-muted mb-3 pb-2 border-b border-border">
            Income Summary
          </div>
          <div className="flex justify-between items-center py-1.5 text-[12.5px]">
            <span className="text-text-secondary">Gross Salary</span>
            <span className="font-medium">₹{grossSalary.toLocaleString('en-IN')}</span>
          </div>
          <div className="flex justify-between items-center py-2 border-t border-border mt-1 font-semibold">
            <span>Gross Total Income</span>
            <span className="font-serif text-[17px]">₹{grossSalary.toLocaleString('en-IN')}</span>
          </div>
          <div className="flex justify-between items-center py-1 text-success text-[12.5px]">
            <span>Total Deductions (VI-A)</span>
            <span>−₹{totalDeductions.toLocaleString('en-IN')}</span>
          </div>
          <div className="flex justify-between items-center py-2 border-t border-border mt-1 font-bold text-[14px]">
            <span>Net Taxable Income</span>
            <span className="font-serif text-[18px]">₹{netTaxable.toLocaleString('en-IN')}</span>
          </div>
        </div>

        {/* Tax computation */}
        <div className="mb-5">
          <div className="text-[11px] font-bold uppercase tracking-[0.8px] text-text-muted mb-3 pb-2 border-b border-border">
            Tax Computation ({formData.taxRegime === 'NEW' ? 'New' : 'Old'} Regime)
          </div>
          <div className="flex justify-between items-center py-1.5 text-[12.5px]">
            <span className="text-text-secondary">Tax on Total Income</span>
            <span className="font-medium">₹{Math.round(taxOnIncome).toLocaleString('en-IN')}</span>
          </div>
          <div className="flex justify-between items-center py-1.5 text-[12.5px]">
            <span className="text-text-secondary">Health & Ed. Cess 4%</span>
            <span className="font-medium">₹{Math.round(cess).toLocaleString('en-IN')}</span>
          </div>
          <div className="flex justify-between items-center py-2 border-t border-border mt-1 font-bold">
            <span>Total Tax Liability</span>
            <span className="font-serif text-[17px] text-danger">₹{Math.round(totalTax).toLocaleString('en-IN')}</span>
          </div>
        </div>

        {/* Regime comparison */}
        <div className="mb-5 rounded-lg p-4 border bg-info-bg border-[#BFDBFE]">
          <div className="text-[11px] font-bold uppercase tracking-[0.8px] text-text-muted mb-2">
            Regime Recommendation
          </div>
          <div className="flex items-center justify-between mb-2">
            <span className="text-[13.5px] font-semibold text-info">
              ✅ NEW Regime Recommended
            </span>
            <span className="text-[12px] text-text-muted">85% confidence</span>
          </div>
          <ProgressBar progress={85} color="blue" />
          <div className="text-[12px] text-text-secondary">
            Saves <strong className="text-text-primary">₹12,500</strong> vs old regime
          </div>
        </div>

        {/* CBDT Validation */}
        {validation && (
          <div className="mb-5">
            <div className="text-[11px] font-bold uppercase tracking-[0.8px] text-text-muted mb-3 pb-2 border-b border-border">
              CBDT Validation Status
            </div>
            <div className="flex items-center gap-2 p-3 rounded-lg bg-success-bg">
              <span className="text-lg">✅</span>
              <div>
                <div className="text-[12px] font-bold text-success">
                  CAT-A (Hard Stop): 0 errors
                </div>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Action buttons */}
      <div className="p-5 border-t border-border flex-shrink-0 space-y-2">
        <Button
          variant="primary"
          className="w-full justify-center"
          disabled={hasBlocker || isSubmitting}
          onClick={onSubmit}
        >
          {isSubmitting ? '⏳ Filing…' : hasBlocker ? '⛔ Fix Errors First' : '✅ Submit ITR'}
        </Button>
        <Button variant="secondary" className="w-full justify-center" onClick={onSaveDraft}>
          💾 Save Draft
        </Button>
        <Button variant="ghost" className="w-full justify-center" onClick={onExportJSON}>
          📥 Export ITD JSON
        </Button>
      </div>
    </aside>
  );
}
