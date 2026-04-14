interface ScheduleCGFormProps {
  formData: any;
  onChange: (path: string, value: any) => void;
}

export default function ScheduleCGForm({ formData, onChange }: ScheduleCGFormProps) {
  const cg = formData.scheduleCG || {};

  const update = (field: string, value: number) => {
    onChange(`scheduleCG.${field}`, value);
  };

  const stcgTotal = (cg.stcg111A || 0) + (cg.stcgOtherAssets || 0);
  const ltcg112AExempt = 125000;
  const ltcg112ATaxable = Math.max(0, (cg.ltcg112A || 0) - ltcg112AExempt);
  const ltcgTotal = ltcg112ATaxable + (cg.ltcgOther112 || 0);
  const totalCG = stcgTotal + ltcgTotal;

  return (
    <div>
      <h2 className="font-serif text-[22px] font-semibold text-text-primary mb-6">
        Schedule: Capital Gains
      </h2>

      <div className="bg-info-bg border border-[#BFDBFE] rounded-lg p-3 mb-6 text-[12.5px] text-info">
        ℹ STCG u/s 111A @ 15%. LTCG u/s 112A @ 10% above ₹1.25L exemption. LTCG other @ 20% with indexation.
      </div>

      {/* STCG Section */}
      <div className="mb-8">
        <h3 className="text-[16px] font-semibold text-text-primary mb-4 pb-2 border-b border-border">
          Short-Term Capital Gains (STCG)
        </h3>
        <div className="grid grid-cols-2 gap-5">
          <NumField
            label="STCG u/s 111A — Equity / MF (15%)"
            value={cg.stcg111A || 0}
            onChange={(v) => update('stcg111A', v)}
            helpText="Shares, equity MF held ≤ 1 year"
          />
          <NumField
            label="STCG — Other Assets (Slab Rate)"
            value={cg.stcgOtherAssets || 0}
            onChange={(v) => update('stcgOtherAssets', v)}
            helpText="Debt MF, property, gold, etc."
          />
        </div>
        <div className="mt-4 p-3 bg-bg rounded-lg flex justify-between items-center">
          <span className="text-[13px] font-semibold text-text-secondary">Total STCG:</span>
          <span className="font-serif text-[18px] font-semibold">₹{stcgTotal.toLocaleString('en-IN')}</span>
        </div>
      </div>

      {/* LTCG 112A Section */}
      <div className="mb-8">
        <h3 className="text-[16px] font-semibold text-text-primary mb-4 pb-2 border-b border-border">
          Long-Term Capital Gains u/s 112A — Equity / MF (10%)
        </h3>
        <div className="grid grid-cols-2 gap-5">
          <NumField
            label="LTCG u/s 112A — Total Gains (₹)"
            value={cg.ltcg112A || 0}
            onChange={(v) => update('ltcg112A', v)}
          />
          <NumField
            label="Cost of Acquisition (₹)"
            value={cg.costOfAcquisition112A || 0}
            onChange={(v) => update('costOfAcquisition112A', v)}
          />
        </div>
        <div className="mt-4 p-3 bg-bg rounded-lg">
          <div className="flex justify-between text-[13px] mb-1">
            <span className="text-text-secondary">LTCG 112A (Total):</span>
            <span className="font-mono">₹{(cg.ltcg112A || 0).toLocaleString('en-IN')}</span>
          </div>
          <div className="flex justify-between text-[13px] mb-1">
            <span className="text-text-secondary">Less: Exemption ₹1.25L:</span>
            <span className="font-mono text-success">−₹{Math.min(cg.ltcg112A || 0, ltcg112AExempt).toLocaleString('en-IN')}</span>
          </div>
          <div className="flex justify-between text-[13px] font-semibold border-t border-border pt-1 mt-1">
            <span>Taxable LTCG 112A:</span>
            <span className="font-serif text-[18px]">₹{ltcg112ATaxable.toLocaleString('en-IN')}</span>
          </div>
        </div>
      </div>

      {/* LTCG Other */}
      <div className="mb-8">
        <h3 className="text-[16px] font-semibold text-text-primary mb-4 pb-2 border-b border-border">
          LTCG u/s 112 — Other Assets (20% with Indexation)
        </h3>
        <div className="grid grid-cols-2 gap-5">
          <NumField
            label="LTCG — Property, Gold, Debt MF (₹)"
            value={cg.ltcgOther112 || 0}
            onChange={(v) => update('ltcgOther112', v)}
          />
          <NumField
            label="Exemption u/s 54 / 54EC (₹)"
            value={cg.exemptionUnder10_38 || 0}
            onChange={(v) => update('exemptionUnder10_38', v)}
          />
        </div>
      </div>

      <div className="mt-4 p-4 bg-bg rounded-lg flex justify-between items-center">
        <span className="text-[15px] font-semibold text-text-secondary">Total Capital Gains:</span>
        <span className="font-serif text-[24px] font-semibold">₹{totalCG.toLocaleString('en-IN')}</span>
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
