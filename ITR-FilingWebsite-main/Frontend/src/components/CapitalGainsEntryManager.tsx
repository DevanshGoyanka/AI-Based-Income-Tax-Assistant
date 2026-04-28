// Capital Gains Transaction Manager - CBDT Compliant
import React, { useState } from 'react';

interface CapitalGainTransaction {
  assetType: string;
  assetDescription: string;
  purchaseDate: string;
  saleDate: string;
  purchaseCost: number;
  saleCost: number;
  expenses: number;
  indexedCost?: number;
  fmvJan2018?: number;
  buyerName?: string;
  buyerPAN?: string;
  gainType: 'STCG_111A' | 'STCG_OTHER' | 'LTCG_112A' | 'LTCG_112';
  gain: number;
  exemptionClaimed?: number;
  exemptionSection?: string;
}

interface CapitalGainsEntryManagerProps {
  entries: CapitalGainTransaction[];
  onChange: (entries: CapitalGainTransaction[]) => void;
}

export const CapitalGainsEntryManager: React.FC<CapitalGainsEntryManagerProps> = ({ entries, onChange }) => {
  const [editingIndex, setEditingIndex] = useState<number | null>(null);

  const calculateHoldingPeriod = (purchaseDate: string, saleDate: string): number => {
    if (!purchaseDate || !saleDate) return 0;
    const p = new Date(purchaseDate);
    const s = new Date(saleDate);
    return Math.floor((s.getTime() - p.getTime()) / (1000 * 60 * 60 * 24 * 30));
  };

  const determineGainType = (assetType: string, holdingMonths: number): string => {
    if (assetType === 'EQUITY' || assetType === 'MUTUAL_FUND') {
      return holdingMonths >= 12 ? 'LTCG_112A' : 'STCG_111A';
    } else if (assetType === 'PROPERTY' || assetType === 'LAND') {
      return holdingMonths >= 24 ? 'LTCG_112' : 'STCG_OTHER';
    } else {
      return holdingMonths >= 36 ? 'LTCG_112' : 'STCG_OTHER';
    }
  };

  const addEntry = () => {
    const newEntry: CapitalGainTransaction = {
      assetType: 'EQUITY',
      assetDescription: '',
      purchaseDate: '',
      saleDate: '',
      purchaseCost: 0,
      saleCost: 0,
      expenses: 0,
      gainType: 'STCG_111A',
      gain: 0,
    };
    onChange([...entries, newEntry]);
    setEditingIndex(entries.length);
  };

  const removeEntry = (index: number) => {
    onChange(entries.filter((_, i) => i !== index));
  };

  const updateEntry = (index: number, field: keyof CapitalGainTransaction, value: any) => {
    const updated = [...entries];
    updated[index] = { ...updated[index], [field]: value };
    
    const entry = updated[index];
    
    // Auto-calculate holding period and gain type
    if (field === 'purchaseDate' || field === 'saleDate' || field === 'assetType') {
      const holdingMonths = calculateHoldingPeriod(entry.purchaseDate, entry.saleDate);
      entry.gainType = determineGainType(entry.assetType, holdingMonths) as any;
    }
    
    // Auto-calculate gain
    if (field === 'purchaseCost' || field === 'saleCost' || field === 'expenses' || field === 'indexedCost') {
      const cost = entry.gainType === 'LTCG_112' && entry.indexedCost ? entry.indexedCost : entry.purchaseCost;
      entry.gain = entry.saleCost - cost - entry.expenses - (entry.exemptionClaimed || 0);
    }
    
    onChange(updated);
  };

  const getTotalGains = () => entries.reduce((sum, e) => sum + e.gain, 0);
  const getSTCG111A = () => entries.filter(e => e.gainType === 'STCG_111A').reduce((sum, e) => sum + e.gain, 0);
  const getLTCG112A = () => entries.filter(e => e.gainType === 'LTCG_112A').reduce((sum, e) => sum + e.gain, 0);
  const getSTCGOther = () => entries.filter(e => e.gainType === 'STCG_OTHER').reduce((sum, e) => sum + e.gain, 0);
  const getLTCG112 = () => entries.filter(e => e.gainType === 'LTCG_112').reduce((sum, e) => sum + e.gain, 0);

  return (
    <div style={{ padding: 20, background: '#f9f9f9', borderRadius: 8 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
        <h3 style={{ margin: 0 }}>Capital Gains Transactions (CBDT Compliant)</h3>
        <button onClick={addEntry} style={{ background: '#4CAF50', color: 'white', border: 'none', padding: '10px 20px', borderRadius: 4, cursor: 'pointer' }}>
          + Add Transaction
        </button>
      </div>

      {entries.length === 0 && (
        <div style={{ textAlign: 'center', padding: 40, color: '#666' }}>
          No capital gains transactions. Click "Add Transaction" to add sales.
        </div>
      )}

      {entries.map((entry, index) => (
        <div key={index} style={{ background: 'white', border: '1px solid #ddd', borderRadius: 8, padding: 20, marginBottom: 15 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 15 }}>
            <span style={{ background: entry.gainType.includes('LTCG') ? '#4CAF50' : '#FF9800', color: 'white', padding: '5px 10px', borderRadius: 4, fontSize: 14 }}>
              {entry.gainType.replace('_', ' ')}
            </span>
            <button onClick={() => removeEntry(index)} style={{ background: '#f44336', color: 'white', border: 'none', width: 30, height: 30, borderRadius: '50%', cursor: 'pointer', fontSize: 20 }}>
              ×
            </button>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: 15 }}>
            <div>
              <label style={{ display: 'block', marginBottom: 5, fontWeight: 500 }}>Asset Type *</label>
              <select
                value={entry.assetType}
                onChange={(e) => updateEntry(index, 'assetType', e.target.value)}
                style={{ width: '100%', padding: 8, border: '1px solid #ddd', borderRadius: 4 }}
              >
                <option value="EQUITY">Equity Shares</option>
                <option value="MUTUAL_FUND">Mutual Funds</option>
                <option value="PROPERTY">Immovable Property</option>
                <option value="LAND">Land</option>
                <option value="GOLD">Gold/Jewellery</option>
                <option value="BONDS">Bonds/Debentures</option>
                <option value="OTHER">Other Assets</option>
              </select>
            </div>

            <div>
              <label style={{ display: 'block', marginBottom: 5, fontWeight: 500 }}>Asset Description *</label>
              <input
                type="text"
                value={entry.assetDescription}
                onChange={(e) => updateEntry(index, 'assetDescription', e.target.value)}
                placeholder="e.g., 100 shares of Reliance"
                style={{ width: '100%', padding: 8, border: '1px solid #ddd', borderRadius: 4 }}
              />
            </div>

            <div>
              <label style={{ display: 'block', marginBottom: 5, fontWeight: 500 }}>Purchase Date *</label>
              <input
                type="date"
                value={entry.purchaseDate}
                onChange={(e) => updateEntry(index, 'purchaseDate', e.target.value)}
                style={{ width: '100%', padding: 8, border: '1px solid #ddd', borderRadius: 4 }}
              />
            </div>

            <div>
              <label style={{ display: 'block', marginBottom: 5, fontWeight: 500 }}>Sale Date *</label>
              <input
                type="date"
                value={entry.saleDate}
                onChange={(e) => updateEntry(index, 'saleDate', e.target.value)}
                style={{ width: '100%', padding: 8, border: '1px solid #ddd', borderRadius: 4 }}
              />
            </div>

            <div>
              <label style={{ display: 'block', marginBottom: 5, fontWeight: 500 }}>Purchase Cost *</label>
              <input
                type="number"
                value={entry.purchaseCost}
                onChange={(e) => updateEntry(index, 'purchaseCost', parseFloat(e.target.value) || 0)}
                placeholder="0"
                style={{ width: '100%', padding: 8, border: '1px solid #ddd', borderRadius: 4 }}
              />
            </div>

            <div>
              <label style={{ display: 'block', marginBottom: 5, fontWeight: 500 }}>Sale Price *</label>
              <input
                type="number"
                value={entry.saleCost}
                onChange={(e) => updateEntry(index, 'saleCost', parseFloat(e.target.value) || 0)}
                placeholder="0"
                style={{ width: '100%', padding: 8, border: '1px solid #ddd', borderRadius: 4 }}
              />
            </div>

            <div>
              <label style={{ display: 'block', marginBottom: 5, fontWeight: 500 }}>Transfer Expenses</label>
              <input
                type="number"
                value={entry.expenses}
                onChange={(e) => updateEntry(index, 'expenses', parseFloat(e.target.value) || 0)}
                placeholder="0"
                style={{ width: '100%', padding: 8, border: '1px solid #ddd', borderRadius: 4 }}
              />
            </div>

            {entry.gainType === 'LTCG_112' && (
              <div>
                <label style={{ display: 'block', marginBottom: 5, fontWeight: 500 }}>Indexed Cost (if applicable)</label>
                <input
                  type="number"
                  value={entry.indexedCost || 0}
                  onChange={(e) => updateEntry(index, 'indexedCost', parseFloat(e.target.value) || 0)}
                  placeholder="0"
                  style={{ width: '100%', padding: 8, border: '1px solid #ddd', borderRadius: 4 }}
                />
              </div>
            )}

            {entry.gainType === 'LTCG_112A' && entry.purchaseDate && new Date(entry.purchaseDate) < new Date('2018-01-31') && (
              <div>
                <label style={{ display: 'block', marginBottom: 5, fontWeight: 500 }}>FMV as on 31-Jan-2018</label>
                <input
                  type="number"
                  value={entry.fmvJan2018 || 0}
                  onChange={(e) => updateEntry(index, 'fmvJan2018', parseFloat(e.target.value) || 0)}
                  placeholder="0"
                  style={{ width: '100%', padding: 8, border: '1px solid #ddd', borderRadius: 4 }}
                />
              </div>
            )}

            <div>
              <label style={{ display: 'block', marginBottom: 5, fontWeight: 500 }}>Exemption Claimed (54/54EC/54F)</label>
              <input
                type="number"
                value={entry.exemptionClaimed || 0}
                onChange={(e) => updateEntry(index, 'exemptionClaimed', parseFloat(e.target.value) || 0)}
                placeholder="0"
                style={{ width: '100%', padding: 8, border: '1px solid #ddd', borderRadius: 4 }}
              />
            </div>

            <div>
              <label style={{ display: 'block', marginBottom: 5, fontWeight: 500 }}>Computed Gain</label>
              <input
                type="number"
                value={entry.gain}
                disabled
                style={{ width: '100%', padding: 8, border: '1px solid #ddd', borderRadius: 4, background: '#f5f5f5', fontWeight: 'bold' }}
              />
            </div>
          </div>
        </div>
      ))}

      {entries.length > 0 && (
        <div style={{ marginTop: 20, padding: 15, background: '#e3f2fd', borderRadius: 4 }}>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: 15 }}>
            <div>
              <strong>STCG 111A (15%):</strong> ₹{getSTCG111A().toLocaleString('en-IN')}
            </div>
            <div>
              <strong>LTCG 112A (12.5%):</strong> ₹{getLTCG112A().toLocaleString('en-IN')}
            </div>
            <div>
              <strong>STCG Other (Slab):</strong> ₹{getSTCGOther().toLocaleString('en-IN')}
            </div>
            <div>
              <strong>LTCG 112 (20%/12.5%):</strong> ₹{getLTCG112().toLocaleString('en-IN')}
            </div>
            <div style={{ gridColumn: '1 / -1', borderTop: '2px solid #2196F3', paddingTop: 10, marginTop: 10 }}>
              <strong style={{ fontSize: 18 }}>Total Capital Gains:</strong> <span style={{ fontSize: 18 }}>₹{getTotalGains().toLocaleString('en-IN')}</span>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
