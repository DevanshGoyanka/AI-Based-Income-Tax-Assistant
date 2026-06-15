// Dividend Entry Manager - Schedule OS
import React, { useState } from 'react';

interface DividendEntry {
  companyName: string;
  companyPAN: string;
  dividendAmount: number;
  tdsDeducted: number;
  deductorTAN: string;
  isin?: string;
  category: 'SHARES' | 'MF_UNITS' | 'OTHER';
  section: string; // 194 for dividends
}

interface DividendEntryManagerProps {
  entries: DividendEntry[];
  onChange: (entries: DividendEntry[]) => void;
}

export const DividendEntryManager: React.FC<DividendEntryManagerProps> = ({ entries, onChange }) => {
  const [editingIndex, setEditingIndex] = useState<number | null>(null);

  const addEntry = () => {
    const newEntry: DividendEntry = {
      companyName: '',
      companyPAN: '',
      dividendAmount: 0,
      tdsDeducted: 0,
      deductorTAN: '',
      isin: '',
      category: 'SHARES',
      section: '194', // Default section for dividends
    };
    onChange([...entries, newEntry]);
    setEditingIndex(entries.length);
  };

  const removeEntry = (index: number) => {
    onChange(entries.filter((_, i) => i !== index));
    setEditingIndex(null);
  };

  const updateEntry = (index: number, field: keyof DividendEntry, value: any) => {
    const updated = [...entries];
    updated[index] = { ...updated[index], [field]: value };
    onChange(updated);
  };

  const getTotalDividend = () => entries.reduce((sum, e) => sum + e.dividendAmount, 0);
  const getTotalTDS = () => entries.reduce((sum, e) => sum + e.tdsDeducted, 0);

  return (
    <div style={{ padding: 16, background: '#f9f9f9', borderRadius: 8 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <h3 style={{ margin: 0, fontSize: 14, fontWeight: 600 }}>Dividend Details</h3>
          <span style={{ background: '#4CAF50', color: 'white', padding: '2px 8px', borderRadius: 4, fontSize: 10, fontWeight: 600 }}>
            Sec 194
          </span>
        </div>
        <button
          onClick={addEntry}
          style={{ background: '#4CAF50', color: 'white', border: 'none', padding: '8px 16px', borderRadius: 4, cursor: 'pointer', fontSize: 13 }}
        >
          + Add Dividend
        </button>
      </div>

      {entries.length === 0 && (
        <div style={{ textAlign: 'center', padding: 24, color: '#666', fontSize: 13 }}>
          No dividend entries. Click "Add Dividend" to add company-wise dividend details.
        </div>
      )}

      {entries.map((entry, index) => (
        <div key={index} style={{ background: 'white', border: '1px solid #ddd', borderRadius: 8, padding: 16, marginBottom: 12 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 12 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <span style={{ background: '#9c27b0', color: 'white', padding: '4px 12px', borderRadius: 4, fontSize: 12 }}>
                Dividend #{index + 1}
              </span>
              <span style={{ background: '#e3f2fd', color: '#1565c0', padding: '4px 8px', borderRadius: 4, fontSize: 11, fontWeight: 600 }}>
                Sec {entry.section || '194'}
              </span>
            </div>
            <button onClick={() => removeEntry(index)} style={{ background: '#f44336', color: 'white', border: 'none', width: 28, height: 28, borderRadius: '50%', cursor: 'pointer', fontSize: 18 }}>
              ×
            </button>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: 12 }}>
            <div>
              <label style={{ display: 'block', marginBottom: 4, fontWeight: 500, fontSize: 12 }}>Company Name *</label>
              <input
                type="text"
                value={entry.companyName}
                onChange={(e) => updateEntry(index, 'companyName', e.target.value)}
                placeholder="e.g., LIFE INSURANCE CORPORATION"
                style={{ width: '100%', padding: 7, border: '1px solid #ddd', borderRadius: 4, fontSize: 13 }}
              />
            </div>

            <div>
              <label style={{ display: 'block', marginBottom: 4, fontWeight: 500, fontSize: 12 }}>Company PAN</label>
              <input
                type="text"
                value={entry.companyPAN}
                onChange={(e) => updateEntry(index, 'companyPAN', e.target.value.toUpperCase())}
                placeholder="ABCDE1234F"
                maxLength={10}
                style={{ width: '100%', padding: 7, border: '1px solid #ddd', borderRadius: 4, fontSize: 13, textTransform: 'uppercase' }}
              />
            </div>

            <div>
              <label style={{ display: 'block', marginBottom: 4, fontWeight: 500, fontSize: 12 }}>Deductor TAN</label>
              <input
                type="text"
                value={entry.deductorTAN}
                onChange={(e) => updateEntry(index, 'deductorTAN', e.target.value.toUpperCase())}
                placeholder="MUMS89569E"
                maxLength={10}
                style={{ width: '100%', padding: 7, border: '1px solid #ddd', borderRadius: 4, fontSize: 13, textTransform: 'uppercase' }}
              />
            </div>

            <div>
              <label style={{ display: 'block', marginBottom: 4, fontWeight: 500, fontSize: 12 }}>Section</label>
              <select
                value={entry.section || '194'}
                onChange={(e) => updateEntry(index, 'section', e.target.value)}
                style={{ width: '100%', padding: 7, border: '1px solid #ddd', borderRadius: 4, fontSize: 13 }}
              >
                <option value="194">194 - Dividends</option>
                <option value="194K">194K - MF/UTI Income</option>
                <option value="196A">196A - Units of Non-Residents</option>
                <option value="196B">196B - Offshore Fund Units</option>
                <option value="196C">196C - Foreign Currency Bonds</option>
                <option value="196D">196D - FII Securities</option>
              </select>
            </div>

            <div>
              <label style={{ display: 'block', marginBottom: 4, fontWeight: 500, fontSize: 12 }}>ISIN (if listed)</label>
              <input
                type="text"
                value={entry.isin || ''}
                onChange={(e) => updateEntry(index, 'isin', e.target.value.toUpperCase())}
                placeholder="INE123A01012"
                maxLength={12}
                style={{ width: '100%', padding: 7, border: '1px solid #ddd', borderRadius: 4, fontSize: 13, textTransform: 'uppercase' }}
              />
            </div>

            <div>
              <label style={{ display: 'block', marginBottom: 4, fontWeight: 500, fontSize: 12 }}>Category *</label>
              <select
                value={entry.category}
                onChange={(e) => updateEntry(index, 'category', e.target.value)}
                style={{ width: '100%', padding: 7, border: '1px solid #ddd', borderRadius: 4, fontSize: 13 }}
              >
                <option value="SHARES">Shares / Equity</option>
                <option value="MF_UNITS">Mutual Fund Units</option>
                <option value="OTHER">Other</option>
              </select>
            </div>

            <div>
              <label style={{ display: 'block', marginBottom: 4, fontWeight: 500, fontSize: 12 }}>Dividend Received (₹) *</label>
              <input
                type="number"
                value={entry.dividendAmount || ''}
                onChange={(e) => updateEntry(index, 'dividendAmount', parseFloat(e.target.value) || 0)}
                placeholder="0"
                min="0"
                style={{ width: '100%', padding: 7, border: '1px solid #ddd', borderRadius: 4, fontSize: 13 }}
              />
            </div>

            <div>
              <label style={{ display: 'block', marginBottom: 4, fontWeight: 500, fontSize: 12 }}>TDS Deducted (₹)</label>
              <input
                type="number"
                value={entry.tdsDeducted || ''}
                onChange={(e) => updateEntry(index, 'tdsDeducted', parseFloat(e.target.value) || 0)}
                placeholder="0"
                min="0"
                style={{ width: '100%', padding: 7, border: '1px solid #ddd', borderRadius: 4, fontSize: 13 }}
              />
            </div>
          </div>
        </div>
      ))}

      {entries.length > 0 && (
        <div style={{ display: 'flex', gap: 24, padding: '12px 16px', background: '#f3e5f5', borderRadius: 6, fontSize: 13, fontWeight: 500 }}>
          <span>Total Dividend: <strong>₹{getTotalDividend().toLocaleString('en-IN')}</strong></span>
          <span>Total TDS: <strong>₹{getTotalTDS().toLocaleString('en-IN')}</strong></span>
          <span>Net: <strong>₹{(getTotalDividend() - getTotalTDS()).toLocaleString('en-IN')}</strong></span>
        </div>
      )}
    </div>
  );
};
