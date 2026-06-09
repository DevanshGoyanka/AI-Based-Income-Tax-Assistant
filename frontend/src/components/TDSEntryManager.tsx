// TDS Entry Management Component - CBDT Compliant Multi-Entry Support
import React, { useState } from 'react';

interface TDSEntry {
  section: string;
  deductorName: string;
  deductorTAN: string;
  deductorPAN?: string;
  incomeAmount: number;
  tdsDeducted: number;
  certificateNo?: string;
  deductionDate?: string;
  uniqueTransactionNo?: string;
  financialYear: string;
  verified26AS: boolean;
  claimedInReturn: boolean;
}

interface TDSEntryManagerProps {
  entries: TDSEntry[];
  onChange: (entries: TDSEntry[]) => void;
}

export const TDSEntryManager: React.FC<TDSEntryManagerProps> = ({ entries, onChange }) => {
  const [editingIndex, setEditingIndex] = useState<number | null>(null);

  const addEntry = () => {
    const newEntry: TDSEntry = {
      section: '194A',
      deductorName: '',
      deductorTAN: '',
      incomeAmount: 0,
      tdsDeducted: 0,
      financialYear: '2024-25',
      verified26AS: false,
      claimedInReturn: true,
    };
    onChange([...entries, newEntry]);
    setEditingIndex(entries.length);
  };

  const removeEntry = (index: number) => {
    onChange(entries.filter((_, i) => i !== index));
  };

  const updateEntry = (index: number, field: keyof TDSEntry, value: any) => {
    const updated = [...entries];
    updated[index] = { ...updated[index], [field]: value };
    onChange(updated);
  };

  const getTotalTDS = () => entries.reduce((sum, e) => sum + e.tdsDeducted, 0);

  return (
    <div className="tds-entry-manager">
      <div className="header">
        <h3>TDS Entries (CBDT Compliant)</h3>
        <button onClick={addEntry} className="btn-add">+ Add TDS Entry</button>
      </div>

      {entries.length === 0 && (
        <div className="empty-state">
          No TDS entries. Click "Add TDS Entry" to add deductors.
        </div>
      )}

      {entries.map((entry, index) => (
        <div key={index} className="tds-entry-card">
          <div className="card-header">
            <span className="section-badge">Section {entry.section}</span>
            <button onClick={() => removeEntry(index)} className="btn-remove">×</button>
          </div>

          <div className="form-grid">
            <div className="form-group">
              <label>Section *</label>
              <select
                value={entry.section}
                onChange={(e) => updateEntry(index, 'section', e.target.value)}
              >
                <option value="192">192 - Salary</option>
                <option value="194A">194A - Interest</option>
                <option value="194C">194C - Contractor</option>
                <option value="194J">194J - Professional Fees</option>
                <option value="194H">194H - Commission</option>
                <option value="194I">194I - Rent</option>
                <option value="OTHER">Other</option>
              </select>
            </div>

            <div className="form-group">
              <label>Deductor Name *</label>
              <input
                type="text"
                value={entry.deductorName}
                onChange={(e) => updateEntry(index, 'deductorName', e.target.value)}
                placeholder="e.g., State Bank of India"
              />
            </div>

            <div className="form-group">
              <label>Deductor TAN *</label>
              <input
                type="text"
                value={entry.deductorTAN}
                onChange={(e) => updateEntry(index, 'deductorTAN', e.target.value.toUpperCase())}
                placeholder="e.g., MUMS89569E"
                maxLength={10}
                pattern="[A-Z]{4}[0-9]{5}[A-Z]"
              />
            </div>

            <div className="form-group">
              <label>Deductor PAN</label>
              <input
                type="text"
                value={entry.deductorPAN || ''}
                onChange={(e) => updateEntry(index, 'deductorPAN', e.target.value.toUpperCase())}
                placeholder="e.g., AAAAA1234A"
                maxLength={10}
              />
            </div>

            <div className="form-group">
              <label>Income Amount *</label>
              <input
                type="number"
                value={entry.incomeAmount}
                onChange={(e) => updateEntry(index, 'incomeAmount', parseFloat(e.target.value) || 0)}
                placeholder="0"
              />
            </div>

            <div className="form-group">
              <label>TDS Deducted *</label>
              <input
                type="number"
                value={entry.tdsDeducted}
                onChange={(e) => updateEntry(index, 'tdsDeducted', parseFloat(e.target.value) || 0)}
                placeholder="0"
              />
            </div>

            <div className="form-group">
              <label>Certificate No</label>
              <input
                type="text"
                value={entry.certificateNo || ''}
                onChange={(e) => updateEntry(index, 'certificateNo', e.target.value)}
                placeholder="Certificate number"
              />
            </div>

            <div className="form-group">
              <label>Deduction Date</label>
              <input
                type="date"
                value={entry.deductionDate || ''}
                onChange={(e) => updateEntry(index, 'deductionDate', e.target.value)}
              />
            </div>

            <div className="form-group checkbox-group">
              <label>
                <input
                  type="checkbox"
                  checked={entry.verified26AS}
                  onChange={(e) => updateEntry(index, 'verified26AS', e.target.checked)}
                />
                Verified in 26AS
              </label>
            </div>

            <div className="form-group checkbox-group">
              <label>
                <input
                  type="checkbox"
                  checked={entry.claimedInReturn}
                  onChange={(e) => updateEntry(index, 'claimedInReturn', e.target.checked)}
                />
                Claim in Return
              </label>
            </div>
          </div>
        </div>
      ))}

      {entries.length > 0 && (
        <div className="summary">
          <strong>Total TDS:</strong> ₹{getTotalTDS().toLocaleString('en-IN')}
        </div>
      )}

      <style>{`
        .tds-entry-manager {
          padding: 20px;
          background: #f9f9f9;
          border-radius: 8px;
        }
        .header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 20px;
        }
        .btn-add {
          background: #4CAF50;
          color: white;
          border: none;
          padding: 10px 20px;
          border-radius: 4px;
          cursor: pointer;
        }
        .empty-state {
          text-align: center;
          padding: 40px;
          color: #666;
        }
        .tds-entry-card {
          background: white;
          border: 1px solid #ddd;
          border-radius: 8px;
          padding: 20px;
          margin-bottom: 15px;
        }
        .card-header {
          display: flex;
          justify-content: space-between;
          margin-bottom: 15px;
        }
        .section-badge {
          background: #2196F3;
          color: white;
          padding: 5px 10px;
          border-radius: 4px;
          font-size: 14px;
        }
        .btn-remove {
          background: #f44336;
          color: white;
          border: none;
          width: 30px;
          height: 30px;
          border-radius: 50%;
          cursor: pointer;
          font-size: 20px;
        }
        .form-grid {
          display: grid;
          grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
          gap: 15px;
        }
        .form-group label {
          display: block;
          margin-bottom: 5px;
          font-weight: 500;
        }
        .form-group input,
        .form-group select {
          width: 100%;
          padding: 8px;
          border: 1px solid #ddd;
          border-radius: 4px;
        }
        .checkbox-group {
          display: flex;
          align-items: center;
        }
        .checkbox-group label {
          display: flex;
          align-items: center;
          gap: 8px;
        }
        .summary {
          margin-top: 20px;
          padding: 15px;
          background: #e3f2fd;
          border-radius: 4px;
          text-align: right;
          font-size: 18px;
        }
      `}</style>
    </div>
  );
};
