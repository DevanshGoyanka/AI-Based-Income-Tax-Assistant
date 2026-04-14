interface PersonalDetailsFormProps {
  formData: any;
  onChange: (path: string, value: any) => void;
}

export default function PersonalDetailsForm({ formData, onChange }: PersonalDetailsFormProps) {
  return (
    <div>
      <h2 className="font-serif text-[22px] font-semibold text-text-primary mb-6">
        Personal Details
      </h2>

      <div className="grid grid-cols-2 gap-5">
        <FormField
          label="PAN"
          value={formData.pan || ''}
          onChange={(v) => onChange('pan', v.toUpperCase())}
          maxLength={10}
          placeholder="ABCDE1234F"
          mono
        />
        <FormField
          label="Full Name"
          value={formData.name || ''}
          onChange={(v) => onChange('name', v)}
        />
        <FormField
          label="Date of Birth"
          value={formData.dateOfBirth || ''}
          onChange={(v) => onChange('dateOfBirth', v)}
          type="date"
        />
        <FormField
          label="Aadhaar Number"
          value={formData.aadhaar || ''}
          onChange={(v) => onChange('aadhaar', v)}
          maxLength={12}
          placeholder="XXXX-XXXX-1234"
          mono
        />
        <FormField
          label="Mobile"
          value={formData.mobile || ''}
          onChange={(v) => onChange('mobile', v)}
          placeholder="+91 98765 43210"
        />
        <FormField
          label="Email"
          value={formData.email || ''}
          onChange={(v) => onChange('email', v)}
          type="email"
        />
        
        <div>
          <label className="block text-[12.5px] font-semibold text-text-secondary uppercase tracking-[0.4px] mb-1.5">
            Residency Status
          </label>
          <select
            value={formData.residencyStatus || 'ROR'}
            onChange={(e) => onChange('residencyStatus', e.target.value)}
            className="w-full px-3 py-[9px] bg-bg-card border border-border-strong rounded-lg text-[13.5px] outline-none focus:border-gold cursor-pointer"
          >
            <option value="ROR">Resident Ordinary Resident (ROR)</option>
            <option value="RNOR">Resident Not Ordinary Resident (RNOR)</option>
            <option value="NR">Non-Resident (NR)</option>
          </select>
        </div>

        <div>
          <label className="block text-[12.5px] font-semibold text-text-secondary uppercase tracking-[0.4px] mb-1.5">
            ITR Form
          </label>
          <select
            value={formData.itrForm || 'ITR-1'}
            onChange={(e) => onChange('itrForm', e.target.value)}
            className="w-full px-3 py-[9px] bg-bg-card border border-border-strong rounded-lg text-[13.5px] outline-none focus:border-gold cursor-pointer"
          >
            <option value="ITR-1">ITR-1 (Sahaj)</option>
            <option value="ITR-2">ITR-2</option>
            <option value="ITR-3">ITR-3</option>
            <option value="ITR-4">ITR-4 (Sugam)</option>
          </select>
        </div>

        <div>
          <label className="block text-[12.5px] font-semibold text-text-secondary uppercase tracking-[0.4px] mb-1.5">
            Tax Regime
          </label>
          <select
            value={formData.taxRegime || 'NEW'}
            onChange={(e) => onChange('taxRegime', e.target.value)}
            className="w-full px-3 py-[9px] bg-bg-card border border-border-strong rounded-lg text-[13.5px] outline-none focus:border-gold cursor-pointer"
          >
            <option value="NEW">New Regime (Default)</option>
            <option value="OLD">Old Regime</option>
          </select>
        </div>

        <div>
          <label className="block text-[12.5px] font-semibold text-text-secondary uppercase tracking-[0.4px] mb-1.5">
            Filing Type
          </label>
          <select
            value={formData.originalOrRevised || 'O'}
            onChange={(e) => onChange('originalOrRevised', e.target.value)}
            className="w-full px-3 py-[9px] bg-bg-card border border-border-strong rounded-lg text-[13.5px] outline-none focus:border-gold cursor-pointer"
          >
            <option value="O">Original Return</option>
            <option value="R">Revised Return</option>
          </select>
        </div>
      </div>

      {formData.originalOrRevised === 'R' && (
        <div className="mt-6 p-4 bg-warning-bg border border-warning/20 rounded-lg">
          <h3 className="text-[14px] font-semibold text-warning mb-3">Revised Return Details</h3>
          <div className="grid grid-cols-2 gap-4">
            <FormField
              label="Original Acknowledgement Number"
              value={formData.originalAckNo || ''}
              onChange={(v) => onChange('originalAckNo', v)}
              placeholder="123456789012345"
            />
            <FormField
              label="Original Filing Date"
              value={formData.originalFilingDate || ''}
              onChange={(v) => onChange('originalFilingDate', v)}
              type="date"
            />
          </div>
        </div>
      )}
    </div>
  );
}

function FormField({ 
  label, 
  value, 
  onChange, 
  type = 'text',
  maxLength,
  placeholder,
  mono = false
}: { 
  label: string; 
  value: string; 
  onChange: (v: string) => void;
  type?: string;
  maxLength?: number;
  placeholder?: string;
  mono?: boolean;
}) {
  return (
    <div>
      <label className="block text-[12.5px] font-semibold text-text-secondary uppercase tracking-[0.4px] mb-1.5">
        {label}
      </label>
      <input
        type={type}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        maxLength={maxLength}
        placeholder={placeholder}
        className={`w-full px-3 py-[9px] bg-bg-card border border-border-strong rounded-lg text-[13.5px] outline-none focus:border-gold ${mono ? 'font-mono' : ''}`}
      />
    </div>
  );
}
