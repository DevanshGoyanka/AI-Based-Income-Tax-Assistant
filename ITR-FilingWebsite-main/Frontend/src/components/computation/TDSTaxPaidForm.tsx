interface TDSTaxPaidFormProps {
  formData: any;
  onChange: (path: string, value: any) => void;
  clientId: number;
}

export default function TDSTaxPaidForm({ formData, onChange }: TDSTaxPaidFormProps) {
  const tdsEntries = formData.tdsEntries || [];
  const advanceTaxEntries = formData.advanceTaxEntries || [];

  const addTDSEntry = () => {
    const newEntry = {
      id: Date.now(),
      deductorName: '',
      tan: '',
      amountPaid: 0,
      tdsAmount: 0,
      status: 'F',
    };
    onChange('tdsEntries', [...tdsEntries, newEntry]);
  };

  const updateTDSEntry = (index: number, field: string, value: any) => {
    const updated = [...tdsEntries];
    updated[index] = { ...updated[index], [field]: value };
    onChange('tdsEntries', updated);
  };

  const removeTDSEntry = (index: number) => {
    onChange('tdsEntries', tdsEntries.filter((_: any, i: number) => i !== index));
  };

  const addAdvanceTaxEntry = () => {
    const newEntry = {
      id: Date.now(),
      type: 'Advance Tax',
      bsrCode: '',
      challanSerialNo: '',
      dateOfDeposit: '',
      amount: 0,
    };
    onChange('advanceTaxEntries', [...advanceTaxEntries, newEntry]);
  };

  const updateAdvanceTaxEntry = (index: number, field: string, value: any) => {
    const updated = [...advanceTaxEntries];
    updated[index] = { ...updated[index], [field]: value };
    onChange('advanceTaxEntries', updated);
  };

  const removeAdvanceTaxEntry = (index: number) => {
    onChange('advanceTaxEntries', advanceTaxEntries.filter((_: any, i: number) => i !== index));
  };

  const totalTDS = tdsEntries.reduce((sum: number, entry: any) => sum + (entry.tdsAmount || 0), 0);
  const totalAdvanceTax = advanceTaxEntries.reduce((sum: number, entry: any) => sum + (entry.amount || 0), 0);

  return (
    <div>
      <h2 className="font-serif text-[22px] font-semibold text-text-primary mb-6">
        TDS & Tax Paid
      </h2>

      {/* Auto-import from 26AS */}
      <div className="mb-6 p-4 bg-info-bg border border-[#BFDBFE] rounded-lg">
        <div className="flex items-center justify-between">
          <div>
            <div className="text-[14px] font-semibold text-info mb-1">
              Auto-import from 26AS / AIS
            </div>
            <div className="text-[12px] text-text-muted">
              Fetch TDS details directly from ITD portal
            </div>
          </div>
          <button className="px-4 py-2 bg-info text-white rounded-lg text-[13px] font-medium hover:bg-info/90 transition-colors">
            📥 Import from 26AS
          </button>
        </div>
      </div>

      {/* TDS Entries */}
      <div className="mb-8">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-[16px] font-semibold text-text-primary">TDS Deducted</h3>
          <button
            onClick={addTDSEntry}
            className="px-3 py-1.5 bg-gold text-navy rounded-lg text-[12px] font-medium hover:bg-gold-light transition-colors"
          >
            + Add TDS Entry
          </button>
        </div>

        {tdsEntries.length === 0 ? (
          <div className="text-center py-8 text-text-muted border border-dashed border-border rounded-lg">
            No TDS entries. Click "Add TDS Entry" or import from 26AS.
          </div>
        ) : (
          <div className="space-y-3">
            {tdsEntries.map((entry: any, index: number) => (
              <div key={entry.id} className="p-4 bg-bg border border-border rounded-lg">
                <div className="grid grid-cols-5 gap-3 mb-3">
                  <div className="col-span-2">
                    <label className="block text-[11px] font-semibold text-text-muted uppercase mb-1">
                      Deductor Name
                    </label>
                    <input
                      type="text"
                      value={entry.deductorName}
                      onChange={(e) => updateTDSEntry(index, 'deductorName', e.target.value)}
                      className="w-full px-2 py-1.5 bg-bg-card border border-border-strong rounded text-[12px] outline-none focus:border-gold"
                    />
                  </div>
                  <div>
                    <label className="block text-[11px] font-semibold text-text-muted uppercase mb-1">
                      TAN
                    </label>
                    <input
                      type="text"
                      value={entry.tan}
                      maxLength={10}
                      onChange={(e) => updateTDSEntry(index, 'tan', e.target.value.toUpperCase())}
                      className="w-full px-2 py-1.5 bg-bg-card border border-border-strong rounded text-[12px] font-mono outline-none focus:border-gold"
                      placeholder="PUNE12345A"
                    />
                  </div>
                  <div>
                    <label className="block text-[11px] font-semibold text-text-muted uppercase mb-1">
                      Amount Paid (₹)
                    </label>
                    <input
                      type="number"
                      value={entry.amountPaid}
                      onChange={(e) => updateTDSEntry(index, 'amountPaid', Number(e.target.value))}
                      className="w-full px-2 py-1.5 bg-bg-card border border-border-strong rounded text-[12px] font-mono outline-none focus:border-gold"
                    />
                  </div>
                  <div>
                    <label className="block text-[11px] font-semibold text-text-muted uppercase mb-1">
                      TDS Amount (₹)
                    </label>
                    <input
                      type="number"
                      value={entry.tdsAmount}
                      onChange={(e) => updateTDSEntry(index, 'tdsAmount', Number(e.target.value))}
                      className="w-full px-2 py-1.5 bg-bg-card border border-border-strong rounded text-[12px] font-mono outline-none focus:border-gold"
                    />
                  </div>
                </div>
                <div className="flex items-center justify-between">
                  <select
                    value={entry.status}
                    onChange={(e) => updateTDSEntry(index, 'status', e.target.value)}
                    className="px-2 py-1 bg-bg-card border border-border-strong rounded text-[11px] outline-none focus:border-gold cursor-pointer"
                  >
                    <option value="F">Fully Claimed (F)</option>
                    <option value="U">Unclaimed (U)</option>
                    <option value="O">Out of 26AS (O)</option>
                  </select>
                  <button
                    onClick={() => removeTDSEntry(index)}
                    className="text-[11px] text-danger hover:text-danger/80 transition-colors"
                  >
                    Remove
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}

        <div className="mt-4 p-3 bg-bg rounded-lg flex justify-between items-center">
          <span className="text-[13px] font-semibold text-text-secondary">Total TDS Deducted:</span>
          <span className="font-serif text-[18px] font-semibold text-success">
            ₹{totalTDS.toLocaleString('en-IN')}
          </span>
        </div>
      </div>

      {/* Advance Tax / Self Assessment Tax */}
      <div>
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-[16px] font-semibold text-text-primary">Advance Tax / Self Assessment Tax</h3>
          <button
            onClick={addAdvanceTaxEntry}
            className="px-3 py-1.5 bg-gold text-navy rounded-lg text-[12px] font-medium hover:bg-gold-light transition-colors"
          >
            + Add Payment
          </button>
        </div>

        {advanceTaxEntries.length === 0 ? (
          <div className="text-center py-8 text-text-muted border border-dashed border-border rounded-lg">
            No advance tax payments recorded.
          </div>
        ) : (
          <div className="space-y-3">
            {advanceTaxEntries.map((entry: any, index: number) => (
              <div key={entry.id} className="p-4 bg-bg border border-border rounded-lg">
                <div className="grid grid-cols-5 gap-3 mb-3">
                  <div>
                    <label className="block text-[11px] font-semibold text-text-muted uppercase mb-1">
                      Type
                    </label>
                    <select
                      value={entry.type}
                      onChange={(e) => updateAdvanceTaxEntry(index, 'type', e.target.value)}
                      className="w-full px-2 py-1.5 bg-bg-card border border-border-strong rounded text-[12px] outline-none focus:border-gold cursor-pointer"
                    >
                      <option value="Advance Tax">Advance Tax</option>
                      <option value="Self-Assessment Tax">Self-Assessment Tax</option>
                    </select>
                  </div>
                  <div>
                    <label className="block text-[11px] font-semibold text-text-muted uppercase mb-1">
                      BSR Code
                    </label>
                    <input
                      type="text"
                      value={entry.bsrCode}
                      maxLength={7}
                      onChange={(e) => updateAdvanceTaxEntry(index, 'bsrCode', e.target.value)}
                      className="w-full px-2 py-1.5 bg-bg-card border border-border-strong rounded text-[12px] font-mono outline-none focus:border-gold"
                      placeholder="0123456"
                    />
                  </div>
                  <div>
                    <label className="block text-[11px] font-semibold text-text-muted uppercase mb-1">
                      Challan Serial No
                    </label>
                    <input
                      type="text"
                      value={entry.challanSerialNo}
                      onChange={(e) => updateAdvanceTaxEntry(index, 'challanSerialNo', e.target.value)}
                      className="w-full px-2 py-1.5 bg-bg-card border border-border-strong rounded text-[12px] font-mono outline-none focus:border-gold"
                    />
                  </div>
                  <div>
                    <label className="block text-[11px] font-semibold text-text-muted uppercase mb-1">
                      Date of Deposit
                    </label>
                    <input
                      type="date"
                      value={entry.dateOfDeposit}
                      onChange={(e) => updateAdvanceTaxEntry(index, 'dateOfDeposit', e.target.value)}
                      className="w-full px-2 py-1.5 bg-bg-card border border-border-strong rounded text-[12px] outline-none focus:border-gold"
                    />
                  </div>
                  <div>
                    <label className="block text-[11px] font-semibold text-text-muted uppercase mb-1">
                      Amount (₹)
                    </label>
                    <input
                      type="number"
                      value={entry.amount}
                      onChange={(e) => updateAdvanceTaxEntry(index, 'amount', Number(e.target.value))}
                      className="w-full px-2 py-1.5 bg-bg-card border border-border-strong rounded text-[12px] font-mono outline-none focus:border-gold"
                    />
                  </div>
                </div>
                <div className="flex justify-end">
                  <button
                    onClick={() => removeAdvanceTaxEntry(index)}
                    className="text-[11px] text-danger hover:text-danger/80 transition-colors"
                  >
                    Remove
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}

        <div className="mt-4 p-3 bg-bg rounded-lg flex justify-between items-center">
          <span className="text-[13px] font-semibold text-text-secondary">Total Advance Tax Paid:</span>
          <span className="font-serif text-[18px] font-semibold text-success">
            ₹{totalAdvanceTax.toLocaleString('en-IN')}
          </span>
        </div>
      </div>

      {/* Total Summary */}
      <div className="mt-6 p-4 bg-success-bg border border-success/20 rounded-lg">
        <div className="flex justify-between items-center">
          <span className="text-[15px] font-semibold text-success">Total Tax Paid (TDS + Advance):</span>
          <span className="font-serif text-[24px] font-semibold text-success">
            ₹{(totalTDS + totalAdvanceTax).toLocaleString('en-IN')}
          </span>
        </div>
      </div>
    </div>
  );
}
