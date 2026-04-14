interface ScheduleVDAFormProps {
  formData: any;
  onChange: (path: string, value: any) => void;
}

export default function ScheduleVDAForm({ formData, onChange }: ScheduleVDAFormProps) {
  const vdaTransactions = formData.vdaTransactions || [];

  const addTransaction = () => {
    onChange('vdaTransactions', [...vdaTransactions, {
      id: Date.now(),
      assetName: '',
      purchaseDate: '',
      purchaseCost: 0,
      saleDate: '',
      saleConsideration: 0,
      tds194S: 0,
    }]);
  };

  const updateTransaction = (index: number, field: string, value: any) => {
    const updated = [...vdaTransactions];
    updated[index] = { ...updated[index], [field]: value };
    onChange('vdaTransactions', updated);
  };

  const removeTransaction = (index: number) => {
    onChange('vdaTransactions', vdaTransactions.filter((_: any, i: number) => i !== index));
  };

  const totalProfit = vdaTransactions.reduce((sum: number, t: any) =>
    sum + Math.max(0, (t.saleConsideration || 0) - (t.purchaseCost || 0)), 0);
  const totalTDS194S = vdaTransactions.reduce((sum: number, t: any) => sum + (t.tds194S || 0), 0);

  return (
    <div>
      <h2 className="font-serif text-[22px] font-semibold text-text-primary mb-6">
        Schedule: VDA / Crypto
      </h2>

      <div className="bg-warning-bg border border-warning/20 rounded-lg p-4 mb-6">
        <div className="font-semibold text-warning text-[13px] mb-1">⚠ Important — Section 115BBH</div>
        <ul className="text-[12.5px] text-warning space-y-1">
          <li>• VDA income taxed flat at <strong>30%</strong> (+ 4% cess) — no slab benefit</li>
          <li>• <strong>No loss set-off</strong> — VDA loss cannot be adjusted against any income</li>
          <li>• TDS u/s 194S @ 1% by buyer — deductible from tax payable</li>
        </ul>
      </div>

      <div className="flex justify-end mb-4">
        <button
          onClick={addTransaction}
          className="px-4 py-2 bg-gold text-navy rounded-lg text-[13px] font-medium hover:bg-gold-light transition-colors"
        >
          + Add VDA Transaction
        </button>
      </div>

      {vdaTransactions.length === 0 ? (
        <div className="text-center py-12 text-text-muted border border-dashed border-border rounded-lg">
          No VDA transactions added.
        </div>
      ) : (
        <div className="space-y-4">
          {vdaTransactions.map((txn: any, index: number) => (
            <div key={txn.id} className="p-5 bg-bg border border-border rounded-lg">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-[14px] font-semibold">Transaction {index + 1}</h3>
                <button onClick={() => removeTransaction(index)} className="text-[12px] text-danger">Remove</button>
              </div>
              <div className="grid grid-cols-3 gap-4">
                <div className="col-span-3">
                  <label className="block text-[11px] font-semibold text-text-muted uppercase mb-1">Asset Name</label>
                  <input
                    type="text"
                    value={txn.assetName}
                    onChange={(e) => updateTransaction(index, 'assetName', e.target.value)}
                    className="w-full px-3 py-2 bg-bg-card border border-border-strong rounded-lg text-[13px] outline-none focus:border-gold"
                    placeholder="e.g. Bitcoin"
                  />
                </div>
                <div>
                  <label className="block text-[11px] font-semibold text-text-muted uppercase mb-1">Purchase Cost (₹)</label>
                  <input
                    type="number"
                    value={txn.purchaseCost}
                    onChange={(e) => updateTransaction(index, 'purchaseCost', Number(e.target.value))}
                    className="w-full px-3 py-2 bg-bg-card border border-border-strong rounded-lg text-[12px] font-mono outline-none focus:border-gold"
                  />
                </div>
                <div>
                  <label className="block text-[11px] font-semibold text-text-muted uppercase mb-1">Sale Consideration (₹)</label>
                  <input
                    type="number"
                    value={txn.saleConsideration}
                    onChange={(e) => updateTransaction(index, 'saleConsideration', Number(e.target.value))}
                    className="w-full px-3 py-2 bg-bg-card border border-border-strong rounded-lg text-[12px] font-mono outline-none focus:border-gold"
                  />
                </div>
                <div>
                  <label className="block text-[11px] font-semibold text-text-muted uppercase mb-1">TDS u/s 194S (₹)</label>
                  <input
                    type="number"
                    value={txn.tds194S}
                    onChange={(e) => updateTransaction(index, 'tds194S', Number(e.target.value))}
                    className="w-full px-3 py-2 bg-bg-card border border-border-strong rounded-lg text-[12px] font-mono outline-none focus:border-gold"
                  />
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {vdaTransactions.length > 0 && (
        <div className="mt-6 p-4 bg-bg rounded-lg space-y-2">
          <div className="flex justify-between text-[13px]">
            <span className="text-text-secondary">Total VDA Profit (taxable @ 30%):</span>
            <span className="font-serif text-[18px] font-semibold text-danger">₹{totalProfit.toLocaleString('en-IN')}</span>
          </div>
          <div className="flex justify-between text-[13px]">
            <span className="text-text-secondary">Total TDS u/s 194S:</span>
            <span className="font-mono font-semibold text-success">₹{totalTDS194S.toLocaleString('en-IN')}</span>
          </div>
        </div>
      )}
    </div>
  );
}
