interface ScheduleOSFormProps {
  formData: any;
  onChange: (path: string, value: any) => void;
}

export default function ScheduleOSForm({ formData, onChange }: ScheduleOSFormProps) {
  const os = formData.scheduleOS || {};

  const update = (field: string, value: number) => onChange(`scheduleOS.${field}`, value);

  const familyPensionDeduction = Math.min((os.familyPension || 0) / 3, 15000);
  const totalOS = (
    (os.savingsInterest || 0) +
    (os.fdInterest || 0) +
    (os.dividendIncome || 0) +
    Math.max(0, (os.familyPension || 0) - familyPensionDeduction) +
    (os.winningsLottery || 0) +
    (os.otherIncome || 0)
  );

  return (
    <div>
      <h2 className="font-serif text-[22px] font-semibold text-text-primary mb-6">
        Schedule: Other Sources
      </h2>

      <div className="bg-info-bg border border-[#BFDBFE] rounded-lg p-3 mb-6 text-[12.5px] text-info">
        ℹ Family pension: standard deduction of 1/3rd or ₹15,000 whichever is lower.
        Lottery/game winnings taxed at 30% flat.
      </div>

      <div className="grid grid-cols-2 gap-5">
        <NumField
          label="Savings Bank Interest (₹)"
          value={os.savingsInterest || 0}
          onChange={(v) => update('savingsInterest', v)}
          helpText="Eligible for 80TTA deduction (max ₹10K)"
        />
        <NumField
          label="FD / RD / Post Office Interest (₹)"
          value={os.fdInterest || 0}
          onChange={(v) => update('fdInterest', v)}
        />
        <NumField
          label="Dividend Income (₹)"
          value={os.dividendIncome || 0}
          onChange={(v) => update('dividendIncome', v)}
        />
        <NumField
          label="Family Pension (₹)"
          value={os.familyPension || 0}
          onChange={(v) => update('familyPension', v)}
        />
        <NumField
          label="Winnings — Lottery / Games (₹)"
          value={os.winningsLottery || 0}
          onChange={(v) => update('winningsLottery', v)}
          helpText="Taxed at flat 30%"
        />
        <NumField
          label="Other Income (₹)"
          value={os.otherIncome || 0}
          onChange={(v) => update('otherIncome', v)}
        />
      </div>

      <div className="mt-6 p-4 bg-bg rounded-lg flex justify-between items-center">
        <span className="text-[15px] font-semibold text-text-secondary">Total Income from Other Sources:</span>
        <span className="font-serif text-[24px] font-semibold">₹{totalOS.toLocaleString('en-IN')}</span>
      </div>
    </div>
  );
}

function NumField({ label, value, onChange, helpText }: { label: string; value: number; onChange: (v: number) => void; helpText?: string }) {
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
