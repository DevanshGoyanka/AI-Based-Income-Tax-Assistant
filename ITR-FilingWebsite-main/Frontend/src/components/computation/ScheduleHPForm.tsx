interface ScheduleHPFormProps {
  formData: any;
  onChange: (path: string, value: any) => void;
}

export default function ScheduleHPForm({ formData, onChange }: ScheduleHPFormProps) {
  const properties = formData.scheduleHP || [];

  const addProperty = () => {
    const newProperty = {
      id: Date.now(),
      propertyType: 'Self Occupied',
      grossRentReceived: 0,
      municipalTaxPaid: 0,
      netAnnualValue: 0,
      standardDeduction30Pct: 0,
      interestOnHousingLoan: 0,
      priorPeriodInterest: 0,
      incomeFromHP: 0,
      isPreConstruction: false,
    };
    onChange('scheduleHP', [...properties, newProperty]);
  };

  const updateProperty = (index: number, field: string, value: any) => {
    const updated = [...properties];
    const prop = { ...updated[index], [field]: value };
    
    // Auto-calculations
    prop.netAnnualValue = Math.max(0, prop.grossRentReceived - prop.municipalTaxPaid);
    prop.standardDeduction30Pct = prop.netAnnualValue * 0.30;
    
    // Interest cap based on property type
    let interestCap = Infinity;
    if (prop.propertyType === 'Self Occupied') {
      interestCap = 200000; // ₹2L cap
    }
    prop.interestOnHousingLoan = Math.min(prop.interestOnHousingLoan, interestCap);
    
    prop.incomeFromHP = prop.netAnnualValue - prop.standardDeduction30Pct - prop.interestOnHousingLoan - prop.priorPeriodInterest;
    
    updated[index] = prop;
    onChange('scheduleHP', updated);
  };

  const removeProperty = (index: number) => {
    onChange('scheduleHP', properties.filter((_: any, i: number) => i !== index));
  };

  const totalIncomeHP = properties.reduce((sum: number, p: any) => sum + (p.incomeFromHP || 0), 0);

  return (
    <div>
      <h2 className="font-serif text-[22px] font-semibold text-text-primary mb-6">
        Schedule: House Property
      </h2>

      <div className="bg-info-bg border border-[#BFDBFE] rounded-lg p-3 mb-5 text-[12.5px] text-info">
        ℹ Standard deduction: 30% of NAV. Interest cap: ₹2L (self-occupied), ₹30K (pre-2000), unlimited (let-out).
      </div>

      <div className="flex justify-end mb-4">
        <button
          onClick={addProperty}
          className="px-4 py-2 bg-gold text-navy rounded-lg text-[13px] font-medium hover:bg-gold-light transition-colors"
        >
          + Add Property
        </button>
      </div>

      {properties.length === 0 ? (
        <div className="text-center py-12 text-text-muted border border-dashed border-border rounded-lg">
          No properties added. Click "+ Add Property" to begin.
        </div>
      ) : (
        <div className="space-y-6">
          {properties.map((property: any, index: number) => (
            <div key={property.id} className="p-5 bg-bg border border-border rounded-lg">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-[15px] font-semibold text-text-primary">Property {index + 1}</h3>
                <button
                  onClick={() => removeProperty(index)}
                  className="text-[12px] text-danger hover:text-danger/80 transition-colors"
                >
                  Remove
                </button>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-[12px] font-semibold text-text-secondary uppercase mb-1.5">
                    Property Type
                  </label>
                  <select
                    value={property.propertyType}
                    onChange={(e) => updateProperty(index, 'propertyType', e.target.value)}
                    className="w-full px-3 py-2 bg-bg-card border border-border-strong rounded-lg text-[13px] outline-none focus:border-gold cursor-pointer"
                  >
                    <option value="Self Occupied">Self Occupied</option>
                    <option value="Let Out">Let Out</option>
                    <option value="Deemed Let Out">Deemed Let Out</option>
                  </select>
                </div>

                <div>
                  <label className="block text-[12px] font-semibold text-text-secondary uppercase mb-1.5">
                    Gross Rent Received (₹)
                  </label>
                  <input
                    type="number"
                    value={property.grossRentReceived}
                    onChange={(e) => updateProperty(index, 'grossRentReceived', Number(e.target.value))}
                    className="w-full px-3 py-2 bg-bg-card border border-border-strong rounded-lg text-[13px] font-mono outline-none focus:border-gold"
                  />
                </div>

                <div>
                  <label className="block text-[12px] font-semibold text-text-secondary uppercase mb-1.5">
                    Municipal Tax Paid (₹)
                  </label>
                  <input
                    type="number"
                    value={property.municipalTaxPaid}
                    onChange={(e) => updateProperty(index, 'municipalTaxPaid', Number(e.target.value))}
                    className="w-full px-3 py-2 bg-bg-card border border-border-strong rounded-lg text-[13px] font-mono outline-none focus:border-gold"
                  />
                </div>

                <div>
                  <label className="block text-[12px] font-semibold text-text-secondary uppercase mb-1.5">
                    Net Annual Value [AUTO] (₹)
                  </label>
                  <input
                    type="number"
                    value={property.netAnnualValue}
                    readOnly
                    className="w-full px-3 py-2 bg-bg text-text-secondary border border-border rounded-lg text-[13px] font-mono cursor-not-allowed"
                  />
                </div>

                <div>
                  <label className="block text-[12px] font-semibold text-text-secondary uppercase mb-1.5">
                    Standard Deduction 30% [AUTO] (₹)
                  </label>
                  <input
                    type="number"
                    value={property.standardDeduction30Pct}
                    readOnly
                    className="w-full px-3 py-2 bg-bg text-text-secondary border border-border rounded-lg text-[13px] font-mono cursor-not-allowed"
                  />
                </div>

                <div>
                  <label className="block text-[12px] font-semibold text-text-secondary uppercase mb-1.5">
                    Interest on Housing Loan (₹)
                  </label>
                  <input
                    type="number"
                    value={property.interestOnHousingLoan}
                    onChange={(e) => updateProperty(index, 'interestOnHousingLoan', Number(e.target.value))}
                    className="w-full px-3 py-2 bg-bg-card border border-border-strong rounded-lg text-[13px] font-mono outline-none focus:border-gold"
                  />
                  <p className="text-[10px] text-text-muted mt-1">
                    {property.propertyType === 'Self Occupied' ? 'Capped at ₹2,00,000' : 'No cap for let-out'}
                  </p>
                </div>

                <div>
                  <label className="block text-[12px] font-semibold text-text-secondary uppercase mb-1.5">
                    Prior Period Interest (₹)
                  </label>
                  <input
                    type="number"
                    value={property.priorPeriodInterest}
                    onChange={(e) => updateProperty(index, 'priorPeriodInterest', Number(e.target.value))}
                    className="w-full px-3 py-2 bg-bg-card border border-border-strong rounded-lg text-[13px] font-mono outline-none focus:border-gold"
                  />
                </div>

                <div>
                  <label className="block text-[12px] font-semibold text-text-secondary uppercase mb-1.5">
                    Income from HP [AUTO] (₹)
                  </label>
                  <input
                    type="number"
                    value={property.incomeFromHP}
                    readOnly
                    className="w-full px-3 py-2 bg-bg text-text-secondary border border-border rounded-lg text-[13px] font-mono cursor-not-allowed"
                  />
                </div>
              </div>

              <div className="mt-3">
                <label className="flex items-center gap-2 text-[13px] text-text-secondary cursor-pointer">
                  <input
                    type="checkbox"
                    checked={property.isPreConstruction}
                    onChange={(e) => updateProperty(index, 'isPreConstruction', e.target.checked)}
                    className="cursor-pointer"
                  />
                  Pre-construction period interest (1/5th deduction over 5 years)
                </label>
              </div>
            </div>
          ))}
        </div>
      )}

      {properties.length > 0 && (
        <div className="mt-6 p-4 bg-bg rounded-lg flex justify-between items-center">
          <span className="text-[15px] font-semibold text-text-secondary">Total Income from House Property:</span>
          <span className={`font-serif text-[24px] font-semibold ${totalIncomeHP < 0 ? 'text-danger' : 'text-success'}`}>
            ₹{totalIncomeHP.toLocaleString('en-IN')}
          </span>
        </div>
      )}
    </div>
  );
}
