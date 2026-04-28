import { INR } from '../utils/formatters';
import { BankInterestEntryManager } from '../components/BankInterestEntryManager';
import { DonationEntryManager } from '../components/DonationEntryManager';

export function BusinessTab({ formData, setFormData, taxResult }: any) {
  return (
    <div>
      <h3 style={{ fontSize: 14, fontWeight: 600, marginBottom: 16, color: 'var(--text-secondary)' }}>
        Income from Business or Profession (CBDT Schedule BP - Section 28-44)
      </h3>
      <div style={{ marginBottom: 16 }}>
        <label style={{ display: 'block', marginBottom: 8, fontSize: 12, fontWeight: 500 }}>Presumptive Scheme</label>
        <div style={{ display: 'flex', gap: 12 }}>
          {['44AD', '44ADA', 'Regular'].map(scheme => (
            <button
              key={scheme}
              onClick={() => setFormData({ ...formData, bizPresumptive: scheme })}
              style={{
                padding: '8px 16px',
                background: formData.bizPresumptive === scheme ? 'var(--gold)' : 'var(--bg)',
                color: formData.bizPresumptive === scheme ? 'white' : 'var(--text-primary)',
                border: '1px solid var(--border)',
                borderRadius: 6,
                fontSize: 13,
                cursor: 'pointer'
              }}
            >
              {scheme}
            </button>
          ))}
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16, marginBottom: 20 }}>
        <Field label="Gross Turnover/Receipts" value={formData.bizTurnover} onChange={(v: any) => setFormData({ ...formData, bizTurnover: v })} />
        {formData.bizPresumptive !== 'Regular' && (
          <Field label="Declared Income" value={formData.bizDeclared} onChange={(v: any) => setFormData({ ...formData, bizDeclared: v })} />
        )}
        {formData.bizPresumptive === 'Regular' && (
          <Field label="Net Profit from P&L" value={formData.bpNetProfit} onChange={(v: any) => setFormData({ ...formData, bpNetProfit: v })} />
        )}
        <Field label="Taxable Business Income" value={taxResult.bizIncome} computed />
      </div>

      <h4 style={{ fontSize: 13, fontWeight: 600, marginBottom: 12, color: 'var(--text-secondary)' }}>
        Brought Forward Losses - Business/Profession
      </h4>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16 }}>
        <Field label="Business Loss B/F" value={formData.bfLossBusiness || 0} onChange={(v: any) => setFormData({ ...formData, bfLossBusiness: v })} />
        <Field label="Speculation Loss B/F" value={formData.bfLossSpeculation || 0} onChange={(v: any) => setFormData({ ...formData, bfLossSpeculation: v })} />
      </div>
    </div>
  );
}

export function OtherSourcesTab({ formData, setFormData, taxResult }: any) {
  return (
    <div>
      <h3 style={{ fontSize: 14, fontWeight: 600, marginBottom: 16, color: 'var(--text-secondary)' }}>
        Income from Other Sources (CBDT Schedule OS - Section 56-59)
      </h3>
      
      {/* Bank Interest Multi-Entry Manager */}
      <BankInterestEntryManager
        entries={formData.bankInterestEntries || []}
        onChange={(entries) => setFormData({ ...formData, bankInterestEntries: entries })}
      />
      
      <div style={{ marginTop: 24 }}>
        <h4 style={{ fontSize: 13, fontWeight: 600, marginBottom: 12, color: 'var(--text-secondary)' }}>
          Legacy Single-Value Fields (Use multi-entry above for CBDT compliance)
        </h4>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16, marginBottom: 24 }}>
          <Field label="SB Interest" value={formData.interestSB} onChange={(v: any) => setFormData({ ...formData, interestSB: v })} />
          <Field label="FD Interest" value={formData.interestFD} onChange={(v: any) => setFormData({ ...formData, interestFD: v })} />
        </div>
      </div>

      <h3 style={{ fontSize: 14, fontWeight: 600, marginBottom: 16, color: 'var(--text-secondary)' }}>
        Dividend Income
      </h3>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16, marginBottom: 24 }}>
        <Field label="Dividend Income" value={formData.dividends} onChange={(v: any) => setFormData({ ...formData, dividends: v })} />
        <Field label="Company Name (for TDS)" value={formData.dividendCompanyName || ''} onChange={(v: any) => setFormData({ ...formData, dividendCompanyName: v })} type="text" prefix="" />
        <Field label="Company TAN (if TDS deducted)" value={formData.dividendCompanyTAN || ''} onChange={(v: any) => setFormData({ ...formData, dividendCompanyTAN: v })} type="text" prefix="" />
      </div>

      <h3 style={{ fontSize: 14, fontWeight: 600, marginBottom: 16, color: 'var(--text-secondary)' }}>
        Other Income
      </h3>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16 }}>
        <Field label="Family Pension" value={formData.familyPension} onChange={(v: any) => setFormData({ ...formData, familyPension: v })} />
        <Field label="Other Misc Income" value={formData.otherMisc} onChange={(v: any) => setFormData({ ...formData, otherMisc: v })} />
        <Field label="Total Other Sources" value={taxResult.otherIncome} computed />
      </div>
    </div>
  );
}

export function VDATab({ formData, setFormData, taxResult }: any) {
  return (
    <div>
      <h3 style={{ fontSize: 14, fontWeight: 600, marginBottom: 16, color: 'var(--text-secondary)' }}>
        Virtual Digital Assets (VDA) - Section 115BBH
      </h3>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16 }}>
        <Field label="Net VDA Gains" value={formData.vdaGains} onChange={(v: any) => setFormData({ ...formData, vdaGains: v })} />
        <Field label="VDA Tax @ 30%" value={taxResult.vdaTax} computed />
      </div>
      <div style={{ marginTop: 12, padding: 12, background: 'var(--info-bg)', borderRadius: 6, fontSize: 12, color: 'var(--info)' }}>
        No loss set-off allowed for VDA transactions as per CBDT rules
      </div>
    </div>
  );
}

export function DeductionsTab({ formData, setFormData, regime, taxResult }: any) {
  if (regime === 'new') {
    return (
      <div style={{ padding: 24, textAlign: 'center', color: 'var(--text-muted)' }}>
        No deductions available in New Regime (except 80CCD(2) - employer NPS contribution as per CBDT)
        <div style={{ marginTop: 16 }}>
          <Field label="80CCD(2) - Employer NPS" value={formData.s80CCD2} onChange={(v: any) => setFormData({ ...formData, s80CCD2: v })} />
        </div>
      </div>
    );
  }

  return (
    <div>
      <h3 style={{ fontSize: 14, fontWeight: 600, marginBottom: 16, color: 'var(--text-secondary)' }}>
        Deductions under Chapter VI-A (CBDT Schedule VIA)
      </h3>
      <h4 style={{ fontSize: 13, fontWeight: 600, marginBottom: 12, color: 'var(--text-secondary)' }}>Section 80C (Max 1.5L)</h4>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16, marginBottom: 24 }}>
        <Field label="EPF" value={formData.s80C_epf} onChange={(v: any) => setFormData({ ...formData, s80C_epf: v })} />
        <Field label="PPF" value={formData.s80C_ppf} onChange={(v: any) => setFormData({ ...formData, s80C_ppf: v })} />
        <Field label="PPF Account No" value={formData.s80C_ppfAccNo || ''} onChange={(v: any) => setFormData({ ...formData, s80C_ppfAccNo: v })} type="text" prefix="" />
        <Field label="ELSS" value={formData.s80C_elss} onChange={(v: any) => setFormData({ ...formData, s80C_elss: v })} />
        <Field label="LIC Premium" value={formData.s80C_lic} onChange={(v: any) => setFormData({ ...formData, s80C_lic: v })} />
        <Field label="LIC Policy No" value={formData.s80C_licPolicyNo || ''} onChange={(v: any) => setFormData({ ...formData, s80C_licPolicyNo: v })} type="text" prefix="" />
        <Field label="Home Loan Principal" value={formData.s80C_home} onChange={(v: any) => setFormData({ ...formData, s80C_home: v })} />
        <Field label="Lender Name" value={formData.s80C_homeLenderName || ''} onChange={(v: any) => setFormData({ ...formData, s80C_homeLenderName: v })} type="text" prefix="" />
      </div>

      <h3 style={{ fontSize: 14, fontWeight: 600, marginBottom: 16, color: 'var(--text-secondary)' }}>NPS</h3>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16, marginBottom: 24 }}>
        <Field label="80CCD(1B) - Max ₹50K" value={formData.s80CCD1B} onChange={(v: any) => setFormData({ ...formData, s80CCD1B: v })} />
        <Field label="NPS PRAN" value={formData.s80CCD1B_PRAN || ''} onChange={(v: any) => setFormData({ ...formData, s80CCD1B_PRAN: v })} type="text" prefix="" />
        <Field label="80CCD(2) - Employer" value={formData.s80CCD2} onChange={(v: any) => setFormData({ ...formData, s80CCD2: v })} />
      </div>

      <h3 style={{ fontSize: 14, fontWeight: 600, marginBottom: 16, color: 'var(--text-secondary)' }}>Health Insurance (80D)</h3>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16, marginBottom: 24 }}>
        <Field label="Self + Family" value={formData.s80D_self} onChange={(v: any) => setFormData({ ...formData, s80D_self: v })} />
        <Field label="Insurer Name" value={formData.s80D_selfInsurerName || ''} onChange={(v: any) => setFormData({ ...formData, s80D_selfInsurerName: v })} type="text" prefix="" />
        <Field label="Policy No" value={formData.s80D_selfPolicyNo || ''} onChange={(v: any) => setFormData({ ...formData, s80D_selfPolicyNo: v })} type="text" prefix="" />
        <Field label="Parents" value={formData.s80D_parent} onChange={(v: any) => setFormData({ ...formData, s80D_parent: v })} />
        <Field label="Insurer Name" value={formData.s80D_parentInsurerName || ''} onChange={(v: any) => setFormData({ ...formData, s80D_parentInsurerName: v })} type="text" prefix="" />
        <Field label="Policy No" value={formData.s80D_parentPolicyNo || ''} onChange={(v: any) => setFormData({ ...formData, s80D_parentPolicyNo: v })} type="text" prefix="" />
      </div>

      <h3 style={{ fontSize: 14, fontWeight: 600, marginBottom: 16, color: 'var(--text-secondary)' }}>Donations (80G)</h3>
      
      {/* Donation Multi-Entry Manager */}
      <DonationEntryManager
        entries={formData.donationEntries || []}
        onChange={(entries) => setFormData({ ...formData, donationEntries: entries })}
      />
      
      <div style={{ marginTop: 24 }}>
        <h4 style={{ fontSize: 13, fontWeight: 600, marginBottom: 12, color: 'var(--text-secondary)' }}>
          Legacy Single-Value Field (Use multi-entry above for CBDT compliance)
        </h4>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16, marginBottom: 24 }}>
          <Field label="Donation Amount" value={formData.s80G} onChange={(v: any) => setFormData({ ...formData, s80G: v })} />
          <Field label="Donee Name" value={formData.s80G_doneeName || ''} onChange={(v: any) => setFormData({ ...formData, s80G_doneeName: v })} type="text" prefix="" />
          <Field label="Donee PAN" value={formData.s80G_doneePAN || ''} onChange={(v: any) => setFormData({ ...formData, s80G_doneePAN: v })} type="text" prefix="" />
          <Field label="Receipt No" value={formData.s80G_receiptNo || ''} onChange={(v: any) => setFormData({ ...formData, s80G_receiptNo: v })} type="text" prefix="" />
        </div>
      </div>

      <h3 style={{ fontSize: 14, fontWeight: 600, marginBottom: 16, color: 'var(--text-secondary)' }}>Others</h3>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16 }}>
        <Field label="80E - Education Loan" value={formData.s80E} onChange={(v: any) => setFormData({ ...formData, s80E: v })} />
        <Field label="Lender Name" value={formData.s80E_lenderName || ''} onChange={(v: any) => setFormData({ ...formData, s80E_lenderName: v })} type="text" prefix="" />
        <Field label="80TTA - SB Interest (Max ₹10K)" value={formData.s80TTA} onChange={(v: any) => setFormData({ ...formData, s80TTA: v })} />
        <Field label="Total Deductions" value={taxResult.totalDeductions} computed />
      </div>
    </div>
  );
}

export function LossesTab({ formData, setFormData }: any) {
  return (
    <div>
      <h3 style={{ fontSize: 14, fontWeight: 600, marginBottom: 16, color: 'var(--text-secondary)' }}>
        Brought Forward Losses (CBDT Schedule CYLA)
      </h3>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16, marginBottom: 24 }}>
        <Field label="House Property Loss B/F (Max ₹2L)" value={formData.bfLossHP || 0} onChange={(v: any) => setFormData({ ...formData, bfLossHP: v })} />
        <Field label="Business Loss B/F" value={formData.bfLossBusiness || 0} onChange={(v: any) => setFormData({ ...formData, bfLossBusiness: v })} />
        <Field label="Speculation Loss B/F" value={formData.bfLossSpeculation || 0} onChange={(v: any) => setFormData({ ...formData, bfLossSpeculation: v })} />
        <Field label="Short Term Capital Loss B/F" value={formData.bfLossSTCG || 0} onChange={(v: any) => setFormData({ ...formData, bfLossSTCG: v })} />
        <Field label="Long Term Capital Loss B/F" value={formData.bfLossLTCG || 0} onChange={(v: any) => setFormData({ ...formData, bfLossLTCG: v })} />
      </div>
      <div style={{ marginTop: 12, padding: 12, background: 'var(--info-bg)', borderRadius: 6, fontSize: 12, color: 'var(--info)' }}>
        <strong>CBDT Loss Set-off Rules:</strong>
        <ul style={{ marginTop: 8, paddingLeft: 20 }}>
          <li>House Property loss: Maximum ₹2,00,000 can be set off against other heads</li>
          <li>Business loss: Can be set off against any head except salary</li>
          <li>Speculation loss: Can only be set off against speculation income</li>
          <li>STCG loss: Can be set off against STCG or LTCG</li>
          <li>LTCG loss: Can only be set off against LTCG</li>
        </ul>
      </div>
    </div>
  );
}

export function TDSTab({ formData, setFormData, taxResult }: any) {
  const tdsEntries = formData.tdsEntries || [];
  const selfAssessmentTaxEntries = formData.selfAssessmentTaxEntries || [];

  const addTDSEntry = () => {
    const newEntry = {
      section: '192',
      deductorName: '',
      deductorTAN: '',
      deductorPAN: '',
      incomeAmount: 0,
      tdsDeducted: 0,
      certificateNo: '',
      deductionDate: '',
      uniqueTransactionNo: '',
      financialYear: '2024-25',
      verified26AS: false,
      claimedInReturn: true
    };
    setFormData({ ...formData, tdsEntries: [...tdsEntries, newEntry] });
  };

  const updateTDSEntry = (index: number, field: string, value: any) => {
    const updated = [...tdsEntries];
    updated[index] = { ...updated[index], [field]: value };
    setFormData({ ...formData, tdsEntries: updated });
  };

  const removeTDSEntry = (index: number) => {
    const updated = tdsEntries.filter((_: any, i: number) => i !== index);
    setFormData({ ...formData, tdsEntries: updated });
  };

  const addSelfAssessmentEntry = () => {
    const newEntry = {
      bsrCode: '',
      challanNo: '',
      depositDate: '',
      amount: 0,
      cin: ''
    };
    setFormData({ ...formData, selfAssessmentTaxEntries: [...selfAssessmentTaxEntries, newEntry] });
  };

  const updateSelfAssessmentEntry = (index: number, field: string, value: any) => {
    const updated = [...selfAssessmentTaxEntries];
    updated[index] = { ...updated[index], [field]: value };
    setFormData({ ...formData, selfAssessmentTaxEntries: updated });
  };

  const removeSelfAssessmentEntry = (index: number) => {
    const updated = selfAssessmentTaxEntries.filter((_: any, i: number) => i !== index);
    setFormData({ ...formData, selfAssessmentTaxEntries: updated });
  };

  return (
    <div>
      {/* Auto-populated TDS entries from AIS */}
      {formData.aisImported && (
        <div style={{ marginBottom: 24, padding: 16, background: 'var(--success-bg)', borderRadius: 6, border: '1px solid var(--success)' }}>
          <h3 style={{ fontSize: 14, fontWeight: 600, marginBottom: 12, color: 'var(--success)' }}>
            ✓ AIS Data Imported
          </h3>
          <div style={{ fontSize: 12, color: 'var(--text-secondary)' }}>
            TDS entries have been auto-populated from your AIS/26AS. Review and update if needed.
          </div>
        </div>
      )}

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <h3 style={{ fontSize: 14, fontWeight: 600, color: 'var(--text-secondary)' }}>
          TDS Entries (CBDT Compliant)
        </h3>
        <button
          onClick={addTDSEntry}
          style={{
            padding: '6px 12px',
            background: 'var(--gold)',
            color: 'white',
            border: 'none',
            borderRadius: 6,
            fontSize: 12,
            cursor: 'pointer'
          }}
        >
          + Add TDS Entry
        </button>
      </div>

      {tdsEntries.length === 0 && (
        <div style={{ padding: 24, textAlign: 'center', color: 'var(--text-muted)', background: 'var(--bg)', borderRadius: 6, marginBottom: 24 }}>
          No TDS entries. Click "Add TDS Entry" to add deductions.
        </div>
      )}

      {tdsEntries.map((entry: any, index: number) => (
        <div key={index} style={{ marginBottom: 24, padding: 16, background: 'var(--bg)', borderRadius: 6, border: '1px solid var(--border)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
            <h4 style={{ fontSize: 13, fontWeight: 600, color: 'var(--text-secondary)' }}>
              TDS Entry #{index + 1}
            </h4>
            <button
              onClick={() => removeTDSEntry(index)}
              style={{
                padding: '4px 8px',
                background: 'var(--danger)',
                color: 'white',
                border: 'none',
                borderRadius: 4,
                fontSize: 11,
                cursor: 'pointer'
              }}
            >
              Remove
            </button>
          </div>
          
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16 }}>
            <div>
              <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: 'var(--text-secondary)' }}>
                Section *
              </label>
              <select
                value={entry.section || '192'}
                onChange={(e) => updateTDSEntry(index, 'section', e.target.value)}
                style={{
                  width: '100%',
                  padding: '8px 12px',
                  border: '1px solid var(--border)',
                  borderRadius: 6,
                  fontSize: 13
                }}
              >
                <option value="192">192 - Salary</option>
                <option value="194A">194A - Interest</option>
                <option value="194C">194C - Contractor</option>
                <option value="194H">194H - Commission</option>
                <option value="194J">194J - Professional Fees</option>
                <option value="194K">194K - Dividend</option>
                <option value="OTHER">Other</option>
              </select>
            </div>
            <Field label="Deductor Name *" value={entry.deductorName || ''} onChange={(v: any) => updateTDSEntry(index, 'deductorName', v)} type="text" prefix="" required />
            <Field label="Deductor TAN *" value={entry.deductorTAN || ''} onChange={(v: any) => updateTDSEntry(index, 'deductorTAN', v)} type="text" prefix="" required />
            <Field label="Deductor PAN" value={entry.deductorPAN || ''} onChange={(v: any) => updateTDSEntry(index, 'deductorPAN', v)} type="text" prefix="" />
            <Field label="Income Amount *" value={entry.incomeAmount || 0} onChange={(v: any) => updateTDSEntry(index, 'incomeAmount', v)} required />
            <Field label="TDS Deducted *" value={entry.tdsDeducted || 0} onChange={(v: any) => updateTDSEntry(index, 'tdsDeducted', v)} required />
            <Field label="Certificate No *" value={entry.certificateNo || ''} onChange={(v: any) => updateTDSEntry(index, 'certificateNo', v)} type="text" prefix="" required />
            <Field label="Deduction Date *" value={entry.deductionDate || ''} onChange={(v: any) => updateTDSEntry(index, 'deductionDate', v)} type="date" prefix="" required />
            <Field label="Unique Transaction No" value={entry.uniqueTransactionNo || ''} onChange={(v: any) => updateTDSEntry(index, 'uniqueTransactionNo', v)} type="text" prefix="" />
            <Field label="Financial Year *" value={entry.financialYear || '2024-25'} onChange={(v: any) => updateTDSEntry(index, 'financialYear', v)} type="text" prefix="" required />
            <div>
              <label style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13, marginTop: 24 }}>
                <input
                  type="checkbox"
                  checked={entry.verified26AS || false}
                  onChange={(e) => updateTDSEntry(index, 'verified26AS', e.target.checked)}
                />
                Verified in 26AS
              </label>
            </div>
            <div>
              <label style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13, marginTop: 24 }}>
                <input
                  type="checkbox"
                  checked={entry.claimedInReturn !== false}
                  onChange={(e) => updateTDSEntry(index, 'claimedInReturn', e.target.checked)}
                />
                Claim in Return
              </label>
            </div>
          </div>
        </div>
      ))}

      <h3 style={{ fontSize: 14, fontWeight: 600, marginTop: 32, marginBottom: 16, color: 'var(--text-secondary)' }}>
        Advance Tax Payments
      </h3>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16, marginBottom: 24 }}>
        <Field label="15-Jun" value={formData.adv15Jun || 0} onChange={(v: any) => setFormData({ ...formData, adv15Jun: v })} />
        <Field label="15-Sep" value={formData.adv15Sep || 0} onChange={(v: any) => setFormData({ ...formData, adv15Sep: v })} />
        <Field label="15-Dec" value={formData.adv15Dec || 0} onChange={(v: any) => setFormData({ ...formData, adv15Dec: v })} />
        <Field label="15-Mar" value={formData.adv15Mar || 0} onChange={(v: any) => setFormData({ ...formData, adv15Mar: v })} />
      </div>

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <h3 style={{ fontSize: 14, fontWeight: 600, color: 'var(--text-secondary)' }}>
          Self Assessment Tax (CBDT Compliant)
        </h3>
        <button
          onClick={addSelfAssessmentEntry}
          style={{
            padding: '6px 12px',
            background: 'var(--gold)',
            color: 'white',
            border: 'none',
            borderRadius: 6,
            fontSize: 12,
            cursor: 'pointer'
          }}
        >
          + Add SAT Entry
        </button>
      </div>

      {selfAssessmentTaxEntries.length === 0 && (
        <div style={{ padding: 24, textAlign: 'center', color: 'var(--text-muted)', background: 'var(--bg)', borderRadius: 6, marginBottom: 24 }}>
          No self assessment tax entries. Click "Add SAT Entry" to add payments.
        </div>
      )}

      {selfAssessmentTaxEntries.map((entry: any, index: number) => (
        <div key={index} style={{ marginBottom: 16, padding: 16, background: 'var(--bg)', borderRadius: 6, border: '1px solid var(--border)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
            <h4 style={{ fontSize: 13, fontWeight: 600, color: 'var(--text-secondary)' }}>
              SAT Entry #{index + 1}
            </h4>
            <button
              onClick={() => removeSelfAssessmentEntry(index)}
              style={{
                padding: '4px 8px',
                background: 'var(--danger)',
                color: 'white',
                border: 'none',
                borderRadius: 4,
                fontSize: 11,
                cursor: 'pointer'
              }}
            >
              Remove
            </button>
          </div>
          
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16 }}>
            <Field label="BSR Code *" value={entry.bsrCode || ''} onChange={(v: any) => updateSelfAssessmentEntry(index, 'bsrCode', v)} type="text" prefix="" required />
            <Field label="Challan Serial No *" value={entry.challanNo || ''} onChange={(v: any) => updateSelfAssessmentEntry(index, 'challanNo', v)} type="text" prefix="" required />
            <Field label="Date of Deposit *" value={entry.depositDate || ''} onChange={(v: any) => updateSelfAssessmentEntry(index, 'depositDate', v)} type="date" prefix="" required />
            <Field label="Amount *" value={entry.amount || 0} onChange={(v: any) => updateSelfAssessmentEntry(index, 'amount', v)} required />
            <Field label="CIN (Challan ID) *" value={entry.cin || ''} onChange={(v: any) => updateSelfAssessmentEntry(index, 'cin', v)} type="text" prefix="" required />
          </div>
        </div>
      ))}

      <div style={{ marginTop: 24, padding: 16, background: 'var(--gold-pale)', borderRadius: 6 }}>
        <Field label="Total Tax Paid" value={taxResult.totalTaxPaid} computed />
      </div>
    </div>
  );
}

export function TaxComputationTab({ taxResult, regime, itrForm }: any) {
  return (
    <div style={{ maxWidth: 800, margin: '0 auto' }}>
      <table style={{ width: '100%', borderCollapse: 'collapse' }}>
        <tbody>
          <tr style={{ background: 'var(--bg)' }}>
            <td colSpan={2} style={{ padding: '8px 12px', fontWeight: 600, fontSize: 13 }}>Income Summary</td>
          </tr>
          {taxResult.netSalary > 0 && (
            <tr>
              <td style={{ padding: '8px 12px', fontSize: 13 }}>Salary (Net Taxable)</td>
              <td className="mono" style={{ padding: '8px 12px', fontSize: 13, textAlign: 'right' }}>{INR(taxResult.netSalary)}</td>
            </tr>
          )}
          {taxResult.hpIncome !== 0 && (
            <tr>
              <td style={{ padding: '8px 12px', fontSize: 13 }}>House Property Income</td>
              <td className="mono" style={{ padding: '8px 12px', fontSize: 13, textAlign: 'right' }}>{INR(taxResult.hpIncome)}</td>
            </tr>
          )}
          {taxResult.bizIncome > 0 && (
            <tr>
              <td style={{ padding: '8px 12px', fontSize: 13 }}>Business Income</td>
              <td className="mono" style={{ padding: '8px 12px', fontSize: 13, textAlign: 'right' }}>{INR(taxResult.bizIncome)}</td>
            </tr>
          )}
          {taxResult.otherIncome > 0 && (
            <tr>
              <td style={{ padding: '8px 12px', fontSize: 13 }}>Other Sources</td>
              <td className="mono" style={{ padding: '8px 12px', fontSize: 13, textAlign: 'right' }}>{INR(taxResult.otherIncome)}</td>
            </tr>
          )}
          {taxResult.vdaGains > 0 && (
            <tr>
              <td style={{ padding: '8px 12px', fontSize: 13 }}>VDA Income</td>
              <td className="mono" style={{ padding: '8px 12px', fontSize: 13, textAlign: 'right' }}>{INR(taxResult.vdaGains)}</td>
            </tr>
          )}
          <tr style={{ borderTop: '2px solid var(--border)' }}>
            <td style={{ padding: '8px 12px', fontSize: 13, fontWeight: 600 }}>Gross Total Income (GTI)</td>
            <td className="mono" style={{ padding: '8px 12px', fontSize: 13, textAlign: 'right', fontWeight: 600 }}>{INR(taxResult.gti)}</td>
          </tr>
          {(taxResult.gti - taxResult.gtiAfterSetOff) > 0 && itrForm !== 'ITR-1' && (
            <>
              <tr>
                <td style={{ padding: '8px 12px', fontSize: 13 }}>Less: B/F Loss</td>
                <td className="mono" style={{ padding: '8px 12px', fontSize: 13, textAlign: 'right', color: 'var(--danger)' }}>({INR(taxResult.gti - taxResult.gtiAfterSetOff)})</td>
              </tr>
              <tr>
                <td style={{ padding: '8px 12px', fontSize: 13, fontWeight: 600 }}>GTI After Set-offs</td>
                <td className="mono" style={{ padding: '8px 12px', fontSize: 13, textAlign: 'right', fontWeight: 600 }}>{INR(taxResult.gtiAfterSetOff)}</td>
              </tr>
            </>
          )}
          {regime === 'old' && taxResult.totalDeductions > 0 && (
            <tr>
              <td style={{ padding: '8px 12px', fontSize: 13 }}>Less: Total Deductions</td>
              <td className="mono" style={{ padding: '8px 12px', fontSize: 13, textAlign: 'right', color: 'var(--danger)' }}>({INR(taxResult.totalDeductions)})</td>
            </tr>
          )}
          <tr style={{ borderTop: '2px solid var(--border)', background: 'var(--gold-pale)' }}>
            <td style={{ padding: '8px 12px', fontSize: 14, fontWeight: 600 }}>Total Taxable Income</td>
            <td className="mono" style={{ padding: '8px 12px', fontSize: 14, textAlign: 'right', fontWeight: 600 }}>{INR(taxResult.totalIncome)}</td>
          </tr>
          <tr style={{ borderTop: '2px solid var(--border)', background: 'var(--bg)' }}>
            <td colSpan={2} style={{ padding: '8px 12px', fontWeight: 600, fontSize: 13 }}>Tax Calculation</td>
          </tr>
          <tr>
            <td style={{ padding: '8px 12px', fontSize: 13 }}>Tax on Normal Income</td>
            <td className="mono" style={{ padding: '8px 12px', fontSize: 13, textAlign: 'right' }}>{INR(taxResult.normalTax)}</td>
          </tr>
          {taxResult.rebate87A > 0 && (
            <tr>
              <td style={{ padding: '8px 12px', fontSize: 13, paddingLeft: '24px' }}>Less: Rebate u/s 87A</td>
              <td className="mono" style={{ padding: '8px 12px', fontSize: 13, textAlign: 'right', color: 'var(--success)' }}>({INR(taxResult.rebate87A)})</td>
            </tr>
          )}
          {taxResult.surcharge > 0 && (
            <tr>
              <td style={{ padding: '8px 12px', fontSize: 13 }}>Surcharge</td>
              <td className="mono" style={{ padding: '8px 12px', fontSize: 13, textAlign: 'right' }}>{INR(taxResult.surcharge)}</td>
            </tr>
          )}
          <tr>
            <td style={{ padding: '8px 12px', fontSize: 13 }}>Health & Education Cess (4%)</td>
            <td className="mono" style={{ padding: '8px 12px', fontSize: 13, textAlign: 'right' }}>{INR(taxResult.cess)}</td>
          </tr>
          <tr>
            <td style={{ padding: '8px 12px', fontSize: 13 }}>VDA Tax @ 30%</td>
            <td className="mono" style={{ padding: '8px 12px', fontSize: 13, textAlign: 'right' }}>{INR(taxResult.vdaTax)}</td>
          </tr>
          <tr>
            <td style={{ padding: '8px 12px', fontSize: 13 }}>Capital Gains Tax (Special)</td>
            <td className="mono" style={{ padding: '8px 12px', fontSize: 13, textAlign: 'right' }}>{INR(taxResult.cgTax)}</td>
          </tr>
          <tr style={{ borderTop: '2px solid var(--border)', background: 'var(--navy)' }}>
            <td style={{ padding: '12px', fontSize: 15, fontWeight: 600, color: 'white' }}>TOTAL TAX LIABILITY</td>
            <td className="mono" style={{ padding: '12px', fontSize: 15, textAlign: 'right', fontWeight: 600, color: 'white' }}>{INR(taxResult.totalTaxLiability)}</td>
          </tr>
          <tr style={{ background: 'var(--bg)' }}>
            <td colSpan={2} style={{ padding: '8px 12px', fontWeight: 600, fontSize: 13 }}>Less: Tax Payments</td>
          </tr>
          <tr>
            <td style={{ padding: '8px 12px', fontSize: 13, paddingLeft: '24px' }}>TDS Deducted ({(taxResult.tdsEntries || []).filter((e: any) => e.claimedInReturn !== false).length} entries)</td>
            <td className="mono" style={{ padding: '8px 12px', fontSize: 13, textAlign: 'right', color: 'var(--success)' }}>({INR((taxResult.tdsEntries || []).reduce((sum: number, e: any) => sum + (e.claimedInReturn !== false ? (e.tdsDeducted || 0) : 0), 0) || ((taxResult.tdsS192 || 0) + (taxResult.tds194A || 0) + (taxResult.tdsOther || 0)))})</td>
          </tr>
          <tr>
            <td style={{ padding: '8px 12px', fontSize: 13, paddingLeft: '24px' }}>Advance Tax Paid</td>
            <td className="mono" style={{ padding: '8px 12px', fontSize: 13, textAlign: 'right', color: 'var(--success)' }}>({INR((taxResult.adv15Jun || 0) + (taxResult.adv15Sep || 0) + (taxResult.adv15Dec || 0) + (taxResult.adv15Mar || 0))})</td>
          </tr>
          <tr>
            <td style={{ padding: '8px 12px', fontSize: 13, paddingLeft: '24px' }}>Self Assessment Tax ({(taxResult.selfAssessmentTaxEntries || []).length} entries)</td>
            <td className="mono" style={{ padding: '8px 12px', fontSize: 13, textAlign: 'right', color: 'var(--success)' }}>({INR((taxResult.selfAssessmentTaxEntries || []).reduce((sum: number, e: any) => sum + (e.amount || 0), 0) || (taxResult.selfTax || 0))})</td>
          </tr>
          <tr style={{ borderTop: '1px solid var(--border)' }}>
            <td style={{ padding: '8px 12px', fontSize: 13, fontWeight: 600 }}>Total Tax Paid</td>
            <td className="mono" style={{ padding: '8px 12px', fontSize: 13, textAlign: 'right', fontWeight: 600, color: 'var(--success)' }}>({INR(taxResult.totalTaxPaid)})</td>
          </tr>
          <tr style={{ borderTop: '2px solid var(--border)', background: taxResult.taxPayable > 0 ? 'var(--danger-bg)' : 'var(--success-bg)' }}>
            <td style={{ padding: '12px', fontSize: 15, fontWeight: 600, color: taxResult.taxPayable > 0 ? 'var(--danger)' : 'var(--success)' }}>
              {taxResult.taxPayable > 0 ? 'TAX PAYABLE' : 'REFUND'}
            </td>
            <td className="mono" style={{ padding: '12px', fontSize: 15, textAlign: 'right', fontWeight: 600, color: taxResult.taxPayable > 0 ? 'var(--danger)' : 'var(--success)' }}>
              {INR(taxResult.taxPayable > 0 ? taxResult.taxPayable : taxResult.refund)}
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  );
}

function Field({ label, value, onChange, computed, prefix = '₹', type = 'number', required = false }: any) {
  return (
    <div style={{ marginBottom: 16 }}>
      <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: 'var(--text-secondary)' }}>
        {label}{required && ' *'}
      </label>
      <div style={{ position: 'relative' }}>
        {prefix && !computed && (
          <span style={{
            position: 'absolute',
            left: 12,
            top: '50%',
            transform: 'translateY(-50%)',
            color: 'var(--text-muted)',
            fontSize: 13
          }}>
            {prefix}
          </span>
        )}
        <input
          type={type}
          value={value}
          onChange={(e) => !computed && onChange(type === 'number' ? Number(e.target.value) : e.target.value)}
          readOnly={computed}
          style={{
            width: '100%',
            padding: '8px 12px',
            paddingLeft: prefix && !computed ? 28 : 12,
            border: '1px solid var(--border)',
            borderRadius: 6,
            fontSize: 13,
            background: computed ? 'var(--gold-pale)' : 'white',
            cursor: computed ? 'default' : 'text',
            fontFamily: type === 'number' ? 'DM Mono' : 'inherit'
          }}
        />
      </div>
    </div>
  );
}

// CBDT Rounding Helper: Round to nearest ₹10 as per Income Tax Rules
const roundToNearest10 = (amount: number) => {
  const remainder = amount % 10;
  if (remainder < 5) {
    return Math.floor(amount / 10) * 10;
  } else {
    return Math.ceil(amount / 10) * 10;
  }
};

export function computeTax(d: any, ay: string, regime: string) {
  const roundToNearest10 = (n: number) => Math.round(n / 10) * 10;
  
  // Salary - Multi-employer support
  let grossSalary = 0;
  let netSalary = 0;
  let hraExempt = 0;
  
  if (d.employerEntries && d.employerEntries.length > 0) {
    // Use multi-employer entries
    grossSalary = d.employerEntries.reduce((sum: number, emp: any) => sum + (emp.grossSalary || 0), 0);
    netSalary = d.employerEntries.reduce((sum: number, emp: any) => sum + (emp.netSalary || 0), 0);
    hraExempt = 0; // Already computed within employer entries
  } else {
    // Legacy single employer calculation
    hraExempt = Math.round(Math.min(
      d.hra || 0,
      Math.max(0, (d.hraRent || 0) - 0.1 * ((d.basic || 0) + (d.da || 0))),
      (d.hraMetro ? 0.5 : 0.4) * ((d.basic || 0) + (d.da || 0))
    ));
    
    grossSalary = (d.basic||0)+(d.da||0)+(d.hra||0)+(d.bonus||0)+(d.allowances||0)+(d.perquisites||0);
    const stdDed = 75000;
    netSalary = Math.round(Math.max(0, grossSalary - hraExempt - stdDed - Math.min(d.profTax||0, 2500)));
  }

  let hpIncome = d.hpType === 'letout'
    ? Math.round(Math.max(0,(d.grossRent||0)-(d.munTax||0)) * 0.70 - (d.homeLoanInt||0))
    : -Math.min(d.sopLoanInt||0, 200000);

  // === CAPITAL GAINS TAX (SPECIAL RATES) - CBDT Compliant ===
  // STCG on equity: 15% (pre-Jul 2024) and 20% (post-Jul 2024)
  const stcgEquityPre = d.stcgEquityPre || 0;
  const stcgEquityPost = d.stcgEquityPost || 0;
  const stcgOtherSlab = d.stcgOtherSlab || 0; // Added to GTI below
  
  // LTCG 112A (equity)
  const ltcg112APre = d.ltcg112APre || 0;
  const ltcg112APost = d.ltcg112APost || 0;
  // LTCG 112 (other assets)
  const ltcgOtherPre = d.ltcgOtherPre || 0;   // 20% with indexation
  const ltcgOtherPost = d.ltcgOtherPost || 0;  // 12.5% no indexation
  
  // LTCG 112A exemption (₹1,25,000 for AY 2025-26, ₹1,00,000 for AY 2024-25)
  const ltcg112AExemption = ay === '2025-26' ? 125000 : 100000;
  const taxableLtcg112APre = Math.max(0, ltcg112APre - ltcg112AExemption);
  const remainingExemption = Math.max(0, ltcg112AExemption - ltcg112APre);
  const taxableLtcg112APost = Math.max(0, ltcg112APost - remainingExemption);
  
  const cgTax =
    stcgEquityPre * 0.15 +        // STCG equity pre-Jul: 15%
    stcgEquityPost * 0.20 +       // STCG equity post-Jul: 20%
    taxableLtcg112APre * 0.10 +   // LTCG 112A pre-Jul: 10%
    taxableLtcg112APost * 0.125 + // LTCG 112A post-Jul: 12.5%
    ltcgOtherPre * 0.20 +         // LTCG other pre: 20%
    ltcgOtherPost * 0.125;        // LTCG other post: 12.5%
  
  const bizIncome = Math.round(d.bizPresumptive === '44AD'
    ? Math.max((d.bizTurnover||0)*0.08, d.bizDeclared||0)
    : d.bizPresumptive === '44ADA'
    ? Math.max((d.bizTurnover||0)*0.50, d.bizDeclared||0)
    : (d.bpNetProfit||0));
  
  // Other Sources - Bank interest multi-entry support
  let interestSB = 0, interestFD = 0;
  if (d.bankInterestEntries && d.bankInterestEntries.length > 0) {
    d.bankInterestEntries.forEach((bank: any) => {
      if (bank.accountType === 'SAVINGS') interestSB += bank.interestEarned || 0;
      else interestFD += bank.interestEarned || 0;
    });
  } else {
    interestSB = d.interestSB || 0;
    interestFD = d.interestFD || 0;
  }
  
  const otherIncome = Math.round(interestSB + interestFD + (d.dividends||0) + (d.familyPension||0) + (d.otherMisc||0));
  
  const vdaGains = Math.max(0, d.vdaGains||0);
  const vdaTax = vdaGains * 0.30;
  
  // stcgOtherSlab → add to GTI (slab taxed, not special rate)
  const gti = roundToNearest10(netSalary + Math.max(0, hpIncome) + otherIncome + bizIncome + stcgOtherSlab);
  const hpLoss = hpIncome < 0 ? Math.abs(hpIncome) : 0;
  
  // CBDT Loss Set-off: HP loss (max 2L), Business loss, CG losses
  const bfLossHP = Math.min(d.bfLossHP || 0, 200000); // HP loss max 2L set-off
  const bfLossBusiness = d.bfLossBusiness || 0;
  const bfLossSTCG = d.bfLossSTCG || 0;
  const bfLossLTCG = d.bfLossLTCG || 0;
  const bfLossSpeculation = d.bfLossSpeculation || 0;
  
  const totalBFLoss = bfLossHP + bfLossBusiness + bfLossSTCG + bfLossLTCG + bfLossSpeculation;
  const gtiAfterSetOff = roundToNearest10(Math.max(0, gti - hpLoss - totalBFLoss));
  
  let totalDeductions = 0;
  if (regime === 'old') {
    const s80C = Math.min(150000, (d.s80C_epf||0)+(d.s80C_ppf||0)+(d.s80C_elss||0)+(d.s80C_lic||0)+(d.s80C_home||0));
    const s80CCD1B = Math.min(50000, d.s80CCD1B||0);
    const s80D = (d.s80D_self||0) + (d.s80D_parent||0);
    
    // 80G Donations - Multi-entry support
    let s80G = 0;
    if (d.donationEntries && d.donationEntries.length > 0) {
      s80G = d.donationEntries.reduce((sum: number, don: any) => sum + (don.eligibleAmount || 0), 0);
    } else {
      s80G = d.s80G || 0;
    }
    
    totalDeductions = s80C + s80CCD1B + (d.s80CCD2||0) + s80D + (d.s80E||0) + Math.min(10000, d.s80TTA||0) + s80G;
  }
  
  // CBDT Rule: Total Income must be rounded to nearest ₹10
  const totalIncome = roundToNearest10(Math.max(0, gtiAfterSetOff - totalDeductions));
  
  let normalTax = 0;
  if (regime === 'new') {
    if (ay === '2026-27') {
      if (totalIncome > 2400000) normalTax = (totalIncome - 2400000)*0.30 + 540000;
      else if (totalIncome > 2000000) normalTax = (totalIncome - 2000000)*0.25 + 440000;
      else if (totalIncome > 1600000) normalTax = (totalIncome - 1600000)*0.20 + 280000;
      else if (totalIncome > 1200000) normalTax = (totalIncome - 1200000)*0.15 + 120000;
      else if (totalIncome > 800000) normalTax = (totalIncome - 800000)*0.10 + 40000;
      else if (totalIncome > 400000) normalTax = (totalIncome - 400000)*0.05;
    } else {
      // AY 2025-26 Finance Act 2024: 3L-7L-10L-12L-15L CORRECTED
      if (totalIncome > 1500000) normalTax = (totalIncome - 1500000)*0.30 + 150000;
      else if (totalIncome > 1200000) normalTax = (totalIncome - 1200000)*0.20 + 80000;
      else if (totalIncome > 1000000) normalTax = (totalIncome - 1000000)*0.15 + 50000;
      else if (totalIncome > 700000)  normalTax = (totalIncome - 700000)*0.10 + 20000;
      else if (totalIncome > 300000)  normalTax = (totalIncome - 300000)*0.05;
    }
  } else {
    const age = d.age || 30;
    const basic_exempt = age >= 80 ? 500000 : age >= 60 ? 300000 : 250000;
    if (totalIncome > 1000000) normalTax = (totalIncome - 1000000)*0.30 + 112500;
    else if (totalIncome > 500000) normalTax = (totalIncome - 500000)*0.20 + 12500;
    else if (totalIncome > basic_exempt) normalTax = (totalIncome - basic_exempt)*0.05;
  }
  
  // === REBATE u/s 87A ===
  // Special-rate incomes are EXCLUDED from rebate eligibility:
  const specialRateIncome = stcgEquityPre + stcgEquityPost +
    taxableLtcg112APre + taxableLtcg112APost + ltcgOtherPre + ltcgOtherPost + vdaGains;
  const normalRateIncome = totalIncome - specialRateIncome;
  
  let rebate87A = 0;
  if (regime === 'new') {
    if (ay === '2026-27') {
      // AY 2026-27: threshold 12L, max 60,000
      if (normalRateIncome <= 1200000) rebate87A = Math.min(normalTax, 60000);
    } else {
      // AY 2025-26: threshold 7L, max 25,000
      if (normalRateIncome <= 700000) rebate87A = Math.min(normalTax, 25000);
    }
  } else {
    // Old regime: Rebate if total income ≤ ₹5,00,000
    if (totalIncome <= 500000) {
      rebate87A = Math.min(normalTax, 12500);
    }
  }
  
  const taxAfterRebate = Math.max(0, normalTax - rebate87A);
  
  let surcharge = 0;
  if (totalIncome > 50000000) surcharge = taxAfterRebate * 0.37;
  else if (totalIncome > 20000000) surcharge = taxAfterRebate * 0.25;
  else if (totalIncome > 10000000) surcharge = taxAfterRebate * 0.15;
  else if (totalIncome > 5000000) surcharge = taxAfterRebate * 0.10;
  
  // CBDT Rule: Round cess to nearest rupee, then add to get total
  const cessRaw = (taxAfterRebate + surcharge) * 0.04;
  const cess = Math.round(cessRaw);
  
  // CBDT Rule: Final tax liability is sum of rounded components
  const totalTaxLiability = taxAfterRebate + surcharge + cess + vdaTax + cgTax;
  
  // Calculate totals from multi-entry arrays
  const totalTDSFromEntries = (d.tdsEntries || []).reduce((sum: number, entry: any) => 
    sum + (entry.claimedInReturn !== false ? (entry.tdsDeducted || 0) : 0), 0);
  const totalSATFromEntries = (d.selfAssessmentTaxEntries || []).reduce((sum: number, entry: any) => 
    sum + (entry.amount || 0), 0);
  
  // Use multi-entry totals if array exists, otherwise fall back to legacy fields
  const totalTDS = (d.tdsEntries && d.tdsEntries.length >= 0) ? totalTDSFromEntries : ((d.tdsS192||0)+(d.tds194A||0)+(d.tdsOther||0));
  const totalSAT = (d.selfAssessmentTaxEntries && d.selfAssessmentTaxEntries.length >= 0) ? totalSATFromEntries : (d.selfTax||0);
  const totalAdvTax = (d.adv15Jun||0)+(d.adv15Sep||0)+(d.adv15Dec||0)+(d.adv15Mar||0);
  
  const totalTaxPaid = totalTDS + totalAdvTax + totalSAT;
  
  // CBDT Rule: Tax payable/refund rounded to nearest ₹10
  const taxPayable = roundToNearest10(Math.max(0, totalTaxLiability - totalTaxPaid));
  const refund = roundToNearest10(Math.max(0, totalTaxPaid - totalTaxLiability));
  
  return {
    grossSalary, hraExempt, netSalary, hpIncome, cgTax, bizIncome,
    otherIncome, vdaTax, gti, gtiAfterSetOff, totalDeductions, totalIncome,
    normalTax, rebate87A, surcharge, cess, totalTaxLiability, totalTaxPaid, taxPayable, refund, vdaGains,
    tdsS192: d.tdsS192||0, tds194A: d.tds194A||0, tdsOther: d.tdsOther||0,
    adv15Jun: d.adv15Jun||0, adv15Sep: d.adv15Sep||0, adv15Dec: d.adv15Dec||0, adv15Mar: d.adv15Mar||0,
    selfTax: d.selfTax||0,
    tdsEntries: d.tdsEntries || [],
    selfAssessmentTaxEntries: d.selfAssessmentTaxEntries || []
  };
}
