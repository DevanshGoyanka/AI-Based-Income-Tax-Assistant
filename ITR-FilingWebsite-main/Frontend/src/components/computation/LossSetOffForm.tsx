interface LossSetOffFormProps {
  formData: any;
  onChange: (path: string, value: any) => void;
}

export default function LossSetOffForm({ formData, onChange }: LossSetOffFormProps) {
  const loss = formData.lossSetOff || {};

  const update = (field: string, value: number) => onChange(`lossSetOff.${field}`, value);

  const hpLossCurrentYear = Math.max(0, -(formData.scheduleHP || []).reduce((sum: number, p: any) => sum + (p.incomeFromHP || 0), 0));
  const totalSetOff = (loss.hpLossSetOff || 0) + (loss.stcgLossBroughtForward || 0) + (loss.ltcgLossBroughtForward || 0);

  return (
    <div>
      <h2 className="font-serif text-[22px] font-semibold text-text-primary mb-6">
        Loss Set-Off (CYLA / BFLA)
      </h2>

      <div className="bg-info-bg border border-[#BFDBFE] rounded-lg p-3 mb-6 text-[12.5px] text-info">
        ℹ HP loss can be set off against any head (max ₹2L). Business loss carried forward 8 years.
        STCG loss can offset STCG/LTCG. LTCG loss only offsets LTCG.
      </div>

      <div className="mb-8">
        <h3 className="text-[15px] font-semibold mb-4 pb-2 border-b border-border">
          Current Year — House Property Loss
        </h3>
        <div className="grid grid-cols-2 gap-5">
          <div>
            <label className="block text-[12.5px] font-semibold text-text-secondary uppercase tracking-[0.4px] mb-1.5">
              Current Year HP Loss [AUTO] (₹)
            </label>
            <input
              type="number"
              value={hpLossCurrentYear}
              readOnly
              className="w-full px-3 py-[9px] bg-bg text-text-secondary border border-border rounded-lg text-[13.5px] font-mono cursor-not-allowed"
            />
          </div>
          <NumField
            label="HP Loss Set-Off (₹)"
            value={loss.hpLossSetOff || 0}
            onChange={(v) => update('hpLossSetOff', Math.min(v, 200000))}
            helpText="Max ₹2L can be set off"
          />
        </div>
      </div>

      <div className="mb-8">
        <h3 className="text-[15px] font-semibold mb-4 pb-2 border-b border-border">
          Brought Forward Losses (BFLA)
        </h3>
        <div className="grid grid-cols-2 gap-5">
          <NumField
            label="HP Loss Brought Forward (₹)"
            value={loss.hpLossBroughtForward || 0}
            onChange={(v) => update('hpLossBroughtForward', v)}
          />
          <NumField
            label="Business Loss Brought Forward (₹)"
            value={loss.businessLossBroughtForward || 0}
            onChange={(v) => update('businessLossBroughtForward', v)}
            helpText="8-year carry forward"
          />
          <NumField
            label="STCG Loss Brought Forward (₹)"
            value={loss.stcgLossBroughtForward || 0}
            onChange={(v) => update('stcgLossBroughtForward', v)}
          />
          <NumField
            label="LTCG Loss Brought Forward (₹)"
            value={loss.ltcgLossBroughtForward || 0}
            onChange={(v) => update('ltcgLossBroughtForward', v)}
          />
        </div>
      </div>

      <div className="p-4 bg-bg rounded-lg flex justify-between items-center">
        <span className="text-[15px] font-semibold text-text-secondary">Total Loss Set-Off This Year:</span>
        <span className="font-serif text-[24px] font-semibold text-success">
          ₹{totalSetOff.toLocaleString('en-IN')}
        </span>
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
