interface DeductionsFormProps {
  formData: any;
  onChange: (path: string, value: any) => void;
}

export default function DeductionsForm({ formData, onChange }: DeductionsFormProps) {
  const deductions = formData.deductions || {};
  
  // 80C cluster total (capped at 1.5L)
  const sec80CTotal = (deductions.sec80C || 0) + (deductions.sec80CCC || 0) + (deductions.sec80CCD1 || 0);
  const sec80CCapped = Math.min(sec80CTotal, 150000);
  const remaining80C = 150000 - sec80CCapped;

  return (
    <div>
      <h2 className="font-serif text-[22px] font-semibold text-text-primary mb-6">
        Chapter VI-A Deductions
      </h2>

      {/* 80C Cluster */}
      <div className="bg-gold-pale border border-gold/20 rounded-lg p-4 mb-6">
        <div className="flex items-center justify-between mb-3">
          <h3 className="text-[15px] font-semibold text-text-primary">
            Section 80C + 80CCC + 80CCD(1) — Combined Limit ₹1,50,000
          </h3>
          <div className="text-[13px] font-mono font-semibold text-text-primary">
            Remaining: ₹{remaining80C.toLocaleString('en-IN')}
          </div>
        </div>
        
        <div className="grid grid-cols-3 gap-4">
          <FormField
            label="80C (LIC, PPF, ELSS, etc.)"
            value={deductions.sec80C || 0}
            onChange={(v) => onChange('deductions.sec80C', v)}
          />
          <FormField
            label="80CCC (Pension Plans)"
            value={deductions.sec80CCC || 0}
            onChange={(v) => onChange('deductions.sec80CCC', v)}
          />
          <FormField
            label="80CCD(1) (NPS Employee)"
            value={deductions.sec80CCD1 || 0}
            onChange={(v) => onChange('deductions.sec80CCD1', v)}
          />
        </div>
        
        <div className="mt-3 pt-3 border-t border-gold/20 flex justify-between text-[13px]">
          <span className="text-text-secondary">Total 80C Cluster:</span>
          <span className="font-mono font-semibold">₹{sec80CTotal.toLocaleString('en-IN')}</span>
        </div>
        <div className="flex justify-between text-[13px] mt-1">
          <span className="text-text-secondary">Allowed (capped):</span>
          <span className="font-mono font-semibold text-success">₹{sec80CCapped.toLocaleString('en-IN')}</span>
        </div>
      </div>

      {/* Additional NPS */}
      <div className="grid grid-cols-2 gap-5 mb-6">
        <FormField
          label="80CCD(1B) — NPS Additional (₹50,000)"
          value={deductions.sec80CCD1B || 0}
          onChange={(v) => onChange('deductions.sec80CCD1B', Math.min(v, 50000))}
          helpText="Additional ₹50K over 80C limit"
        />
        <FormField
          label="80CCD(2) — Employer NPS (No Cap)"
          value={deductions.sec80CCD2 || 0}
          onChange={(v) => onChange('deductions.sec80CCD2', v)}
          helpText="Employer contribution — no limit"
        />
      </div>

      {/* Medical & Health */}
      <h3 className="text-[15px] font-semibold mb-4 mt-6">Medical & Health Insurance</h3>
      <div className="grid grid-cols-2 gap-5 mb-6">
        <FormField
          label="80D — Medical Insurance Premium"
          value={deductions.sec80D || 0}
          onChange={(v) => onChange('deductions.sec80D', v)}
          helpText="Self/Family: ₹25K, Parents: ₹25K (₹50K if senior)"
        />
        <FormField
          label="80DD — Disabled Dependent"
          value={deductions.sec80DD || 0}
          onChange={(v) => onChange('deductions.sec80DD', v)}
          helpText="₹75K normal, ₹1.25L severe"
        />
        <FormField
          label="80DDB — Medical Treatment"
          value={deductions.sec80DDB || 0}
          onChange={(v) => onChange('deductions.sec80DDB', v)}
          helpText="Specified diseases — ₹40K/₹1L"
        />
        <FormField
          label="80U — Self Disability"
          value={deductions.sec80U || 0}
          onChange={(v) => onChange('deductions.sec80U', v)}
          helpText="₹75K normal, ₹1.25L severe"
        />
      </div>

      {/* Interest & Donations */}
      <h3 className="text-[15px] font-semibold mb-4 mt-6">Interest & Donations</h3>
      <div className="grid grid-cols-2 gap-5 mb-6">
        <FormField
          label="80E — Education Loan Interest"
          value={deductions.sec80E || 0}
          onChange={(v) => onChange('deductions.sec80E', v)}
          helpText="No limit — for higher education"
        />
        <FormField
          label="80EEA — Housing Loan Interest"
          value={deductions.sec80EEA || 0}
          onChange={(v) => onChange('deductions.sec80EEA', Math.min(v, 150000))}
          helpText="Affordable housing — max ₹1.5L"
        />
        <FormField
          label="80G — Donations (Qualifying)"
          value={deductions.sec80G || 0}
          onChange={(v) => onChange('deductions.sec80G', v)}
          helpText="50%/100% of donation amount"
        />
        <FormField
          label="80GGA — Scientific Research"
          value={deductions.sec80GGA || 0}
          onChange={(v) => onChange('deductions.sec80GGA', v)}
        />
        <FormField
          label="80GGC — Political Donations"
          value={deductions.sec80GGC || 0}
          onChange={(v) => onChange('deductions.sec80GGC', v)}
        />
        <FormField
          label="80TTA — Savings Interest (₹10K)"
          value={deductions.sec80TTA || 0}
          onChange={(v) => onChange('deductions.sec80TTA', Math.min(v, 10000))}
          helpText="Max ₹10K — non-senior citizens"
        />
        <FormField
          label="80TTB — Senior Citizen Interest (₹50K)"
          value={deductions.sec80TTB || 0}
          onChange={(v) => onChange('deductions.sec80TTB', Math.min(v, 50000))}
          helpText="Max ₹50K — senior citizens only"
        />
      </div>

      {/* Total Summary */}
      <div className="mt-6 pt-6 border-t border-border bg-bg rounded-lg p-4">
        <div className="flex justify-between items-center">
          <span className="text-[15px] font-semibold">Total Chapter VI-A Deductions:</span>
          <span className="font-serif text-[24px] font-semibold text-success">
            ₹{(
              sec80CCapped +
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
            ).toLocaleString('en-IN')}
          </span>
        </div>
      </div>
    </div>
  );
}

function FormField({ label, value, onChange, helpText }: { 
  label: string; 
  value: number; 
  onChange: (v: number) => void; 
  helpText?: string;
}) {
  return (
    <div>
      <label className="block text-[12.5px] font-semibold text-text-secondary uppercase tracking-[0.4px] mb-1.5">
        {label}
      </label>
      <input
        type="number"
        value={value}
        onChange={(e) => onChange(Number(e.target.value))}
        className="w-full px-3 py-[9px] bg-bg-card border border-border-strong rounded-lg text-[13.5px] font-mono outline-none focus:border-gold"
        placeholder="0"
      />
      {helpText && <p className="text-[11px] text-text-muted mt-1">{helpText}</p>}
    </div>
  );
}
