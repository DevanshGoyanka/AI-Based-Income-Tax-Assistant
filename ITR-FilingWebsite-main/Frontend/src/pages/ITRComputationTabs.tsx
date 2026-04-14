import { INR } from '../utils/formatters';

export function BusinessTab({ formData, setFormData, taxResult }: any) {
  return (
    <div>
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

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16 }}>
        <Field label="Gross Turnover/Receipts" value={formData.bizTurnover} onChange={(v: any) => setFormData({ ...formData, bizTurnover: v })} />
        {formData.bizPresumptive !== 'Regular' && (
          <Field label="Declared Income" value={formData.bizDeclared} onChange={(v: any) => setFormData({ ...formData, bizDeclared: v })} />
        )}
        {formData.bizPresumptive === 'Regular' && (
          <Field label="Net Profit from P&L" value={formData.bpNetProfit} onChange={(v: any) => setFormData({ ...formData, bpNetProfit: v })} />
        )}
        <Field label="Taxable Business Income" value={taxResult.bizIncome} computed />
      </div>
    </div>
  );
}

export function OtherSourcesTab({ formData, setFormData, taxResult }: any) {
  return (
    <div>
      <h3 style={{ fontSize: 14, fontWeight: 600, marginBottom: 16, color: 'var(--text-secondary)' }}>
        Interest Income
      </h3>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16, marginBottom: 24 }}>
        <Field label="SB Interest" value={formData.interestSB} onChange={(v: any) => setFormData({ ...formData, interestSB: v })} />
        <Field label="FD Interest" value={formData.interestFD} onChange={(v: any) => setFormData({ ...formData, interestFD: v })} />
        <Field label="Bank Name (for TDS)" value={formData.interestBankName || ''} onChange={(v: any) => setFormData({ ...formData, interestBankName: v })} type="text" prefix="" />
        <Field label="Bank TAN (if TDS deducted)" value={formData.interestBankTAN || ''} onChange={(v: any) => setFormData({ ...formData, interestBankTAN: v })} type="text" prefix="" />
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
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16 }}>
        <Field label="Net VDA Gains" value={formData.vdaGains} onChange={(v: any) => setFormData({ ...formData, vdaGains: v })} />
        <Field label="VDA Tax @ 30%" value={taxResult.vdaTax} computed />
      </div>
      <div style={{ marginTop: 12, padding: 12, background: 'var(--info-bg)', borderRadius: 6, fontSize: 12, color: 'var(--info)' }}>
        No loss set-off allowed for VDA transactions
      </div>
    </div>
  );
}

export function DeductionsTab({ formData, setFormData, regime, taxResult }: any) {
  if (regime === 'new') {
    return (
      <div style={{ padding: 24, textAlign: 'center', color: 'var(--text-muted)' }}>
        No deductions available in New Regime (except 80CCD(2) - employer NPS contribution)
        <div style={{ marginTop: 16 }}>
          <Field label="80CCD(2) - Employer NPS" value={formData.s80CCD2} onChange={(v: any) => setFormData({ ...formData, s80CCD2: v })} />
        </div>
      </div>
    );
  }

  return (
    <div>
      <h3 style={{ fontSize: 14, fontWeight: 600, marginBottom: 16, color: 'var(--text-secondary)' }}>Section 80C (Max ₹1.5L)</h3>
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
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16, marginBottom: 24 }}>
        <Field label="Donation Amount" value={formData.s80G} onChange={(v: any) => setFormData({ ...formData, s80G: v })} />
        <Field label="Donee Name" value={formData.s80G_doneeName || ''} onChange={(v: any) => setFormData({ ...formData, s80G_doneeName: v })} type="text" prefix="" />
        <Field label="Donee PAN" value={formData.s80G_doneePAN || ''} onChange={(v: any) => setFormData({ ...formData, s80G_doneePAN: v })} type="text" prefix="" />
        <Field label="Receipt No" value={formData.s80G_receiptNo || ''} onChange={(v: any) => setFormData({ ...formData, s80G_receiptNo: v })} type="text" prefix="" />
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
    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16 }}>
      <Field label="Brought Forward Loss" value={formData.bfLoss} onChange={(v: any) => setFormData({ ...formData, bfLoss: v })} />
    </div>
  );
}

export function TDSTab({ formData, setFormData, taxResult }: any) {
  return (
    <div>
      <h3 style={{ fontSize: 14, fontWeight: 600, marginBottom: 16, color: 'var(--text-secondary)' }}>
        TDS on Salary (Section 192)
      </h3>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16, marginBottom: 24 }}>
        <Field label="TDS Amount" value={formData.tdsS192} onChange={(v: any) => setFormData({ ...formData, tdsS192: v })} />
        <Field label="Deductor Name *" value={formData.tdsS192DeductorName || ''} onChange={(v: any) => setFormData({ ...formData, tdsS192DeductorName: v })} type="text" prefix="" />
        <Field label="Deductor TAN *" value={formData.tdsS192DeductorTAN || ''} onChange={(v: any) => setFormData({ ...formData, tdsS192DeductorTAN: v })} type="text" prefix="" />
      </div>

      <h3 style={{ fontSize: 14, fontWeight: 600, marginBottom: 16, color: 'var(--text-secondary)' }}>
        TDS on Interest (Section 194A)
      </h3>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16, marginBottom: 24 }}>
        <Field label="TDS Amount" value={formData.tds194A} onChange={(v: any) => setFormData({ ...formData, tds194A: v })} />
        <Field label="Deductor Name *" value={formData.tds194ADeductorName || ''} onChange={(v: any) => setFormData({ ...formData, tds194ADeductorName: v })} type="text" prefix="" />
        <Field label="Deductor TAN *" value={formData.tds194ADeductorTAN || ''} onChange={(v: any) => setFormData({ ...formData, tds194ADeductorTAN: v })} type="text" prefix="" />
        <Field label="Certificate No" value={formData.tds194ACertNo || ''} onChange={(v: any) => setFormData({ ...formData, tds194ACertNo: v })} type="text" prefix="" />
        <Field label="Deduction Date" value={formData.tds194ADate || ''} onChange={(v: any) => setFormData({ ...formData, tds194ADate: v })} type="date" prefix="" />
      </div>

      <h3 style={{ fontSize: 14, fontWeight: 600, marginBottom: 16, color: 'var(--text-secondary)' }}>
        TDS on Other Income
      </h3>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16, marginBottom: 24 }}>
        <Field label="TDS Amount" value={formData.tdsOther} onChange={(v: any) => setFormData({ ...formData, tdsOther: v })} />
        <Field label="Deductor Name *" value={formData.tdsOtherDeductorName || ''} onChange={(v: any) => setFormData({ ...formData, tdsOtherDeductorName: v })} type="text" prefix="" />
        <Field label="Deductor TAN *" value={formData.tdsOtherDeductorTAN || ''} onChange={(v: any) => setFormData({ ...formData, tdsOtherDeductorTAN: v })} type="text" prefix="" />
        <Field label="Certificate No" value={formData.tdsOtherCertNo || ''} onChange={(v: any) => setFormData({ ...formData, tdsOtherCertNo: v })} type="text" prefix="" />
        <Field label="Deduction Date" value={formData.tdsOtherDate || ''} onChange={(v: any) => setFormData({ ...formData, tdsOtherDate: v })} type="date" prefix="" />
      </div>

      <h3 style={{ fontSize: 14, fontWeight: 600, marginBottom: 16, color: 'var(--text-secondary)' }}>
        Advance Tax & Self Assessment
      </h3>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16 }}>
        <Field label="15-Jun" value={formData.adv15Jun} onChange={(v: any) => setFormData({ ...formData, adv15Jun: v })} />
        <Field label="15-Sep" value={formData.adv15Sep} onChange={(v: any) => setFormData({ ...formData, adv15Sep: v })} />
        <Field label="15-Dec" value={formData.adv15Dec} onChange={(v: any) => setFormData({ ...formData, adv15Dec: v })} />
        <Field label="15-Mar" value={formData.adv15Mar} onChange={(v: any) => setFormData({ ...formData, adv15Mar: v })} />
        <Field label="Self Assessment Tax" value={formData.selfTax} onChange={(v: any) => setFormData({ ...formData, selfTax: v })} />
        <Field label="Total Tax Paid" value={taxResult.totalTaxPaid} computed />
      </div>
    </div>
  );
}

export function TaxComputationTab({ taxResult, regime }: any) {
  return (
    <div style={{ maxWidth: 800, margin: '0 auto' }}>
      <table style={{ width: '100%', borderCollapse: 'collapse' }}>
        <tbody>
          <tr style={{ background: 'var(--bg)' }}>
            <td colSpan={2} style={{ padding: '8px 12px', fontWeight: 600, fontSize: 13 }}>Income Summary</td>
          </tr>
          <tr>
            <td style={{ padding: '8px 12px', fontSize: 13 }}>Salary (Net Taxable)</td>
            <td className="mono" style={{ padding: '8px 12px', fontSize: 13, textAlign: 'right' }}>{INR(taxResult.netSalary)}</td>
          </tr>
          <tr>
            <td style={{ padding: '8px 12px', fontSize: 13 }}>House Property Income</td>
            <td className="mono" style={{ padding: '8px 12px', fontSize: 13, textAlign: 'right' }}>{INR(taxResult.hpIncome)}</td>
          </tr>
          <tr>
            <td style={{ padding: '8px 12px', fontSize: 13 }}>Business Income</td>
            <td className="mono" style={{ padding: '8px 12px', fontSize: 13, textAlign: 'right' }}>{INR(taxResult.bizIncome)}</td>
          </tr>
          <tr>
            <td style={{ padding: '8px 12px', fontSize: 13 }}>Other Sources</td>
            <td className="mono" style={{ padding: '8px 12px', fontSize: 13, textAlign: 'right' }}>{INR(taxResult.otherIncome)}</td>
          </tr>
          <tr>
            <td style={{ padding: '8px 12px', fontSize: 13 }}>VDA Income</td>
            <td className="mono" style={{ padding: '8px 12px', fontSize: 13, textAlign: 'right' }}>{INR(taxResult.vdaGains || 0)}</td>
          </tr>
          <tr style={{ borderTop: '2px solid var(--border)' }}>
            <td style={{ padding: '8px 12px', fontSize: 13, fontWeight: 600 }}>Gross Total Income (GTI)</td>
            <td className="mono" style={{ padding: '8px 12px', fontSize: 13, textAlign: 'right', fontWeight: 600 }}>{INR(taxResult.gti)}</td>
          </tr>
          <tr>
            <td style={{ padding: '8px 12px', fontSize: 13 }}>Less: B/F Loss</td>
            <td className="mono" style={{ padding: '8px 12px', fontSize: 13, textAlign: 'right', color: 'var(--danger)' }}>({INR(taxResult.gti - taxResult.gtiAfterSetOff)})</td>
          </tr>
          <tr>
            <td style={{ padding: '8px 12px', fontSize: 13, fontWeight: 600 }}>GTI After Set-offs</td>
            <td className="mono" style={{ padding: '8px 12px', fontSize: 13, textAlign: 'right', fontWeight: 600 }}>{INR(taxResult.gtiAfterSetOff)}</td>
          </tr>
          {regime === 'old' && (
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
          <tr>
            <td style={{ padding: '8px 12px', fontSize: 13 }}>Less: Rebate u/s 87A</td>
            <td className="mono" style={{ padding: '8px 12px', fontSize: 13, textAlign: 'right', color: 'var(--success)' }}>({INR(taxResult.rebate87A)})</td>
          </tr>
          <tr>
            <td style={{ padding: '8px 12px', fontSize: 13 }}>Surcharge</td>
            <td className="mono" style={{ padding: '8px 12px', fontSize: 13, textAlign: 'right' }}>{INR(taxResult.surcharge)}</td>
          </tr>
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
          <tr>
            <td style={{ padding: '8px 12px', fontSize: 13 }}>Less: TDS Deducted</td>
            <td className="mono" style={{ padding: '8px 12px', fontSize: 13, textAlign: 'right', color: 'var(--success)' }}>({INR(taxResult.totalTaxPaid)})</td>
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

function Field({ label, value, onChange, computed, prefix = '₹', type = 'number' }: any) {
  return (
    <div style={{ marginBottom: 16 }}>
      <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: 'var(--text-secondary)' }}>
        {label}
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

export function computeTax(d: any, ay: string, regime: string) {
  const hraExempt = Math.min(
    d.hra || 0,
    Math.max(0, (d.hraRent || 0) - 0.1 * ((d.basic || 0) + (d.da || 0))),
    (d.hraMetro ? 0.5 : 0.4) * ((d.basic || 0) + (d.da || 0))
  );
  
  const grossSalary = (d.basic||0)+(d.da||0)+(d.hra||0)+(d.bonus||0)+(d.allowances||0)+(d.perquisites||0);
  const stdDed = 75000;
  const netSalary = Math.max(0, grossSalary - hraExempt - stdDed - Math.min(d.profTax||0, 2500));

  let hpIncome = d.hpType === 'letout'
    ? Math.max(0,(d.grossRent||0)-(d.munTax||0)) * 0.70 - (d.homeLoanInt||0)
    : -Math.min(d.sopLoanInt||0, 200000);

  const stcg111A = ((d.stcgPre||0)*0.15) + ((d.stcgPost||0)*0.20);
  const ltcgExempt = ay === '2026-27' ? 125000 : 100000;
  const ltcg112A_tax = Math.max(0,(d.ltcgPre||0)+(d.ltcgPost||0)-ltcgExempt) * (ay === '2026-27' ? 0.125 : 0.10);
  const cgTax = stcg111A + ltcg112A_tax + (d.stcgOther||0)*0.20 + (d.ltcgOther||0)*0.125;
  
  const bizIncome = d.bizPresumptive === '44AD'
    ? Math.max((d.bizTurnover||0)*0.08, d.bizDeclared||0)
    : d.bizPresumptive === '44ADA'
    ? Math.max((d.bizTurnover||0)*0.50, d.bizDeclared||0)
    : (d.bpNetProfit||0);
  
  const otherIncome = (d.interestSB||0)+(d.interestFD||0)+(d.dividends||0)+(d.familyPension||0)+(d.otherMisc||0);
  
  const vdaGains = Math.max(0, d.vdaGains||0);
  const vdaTax = vdaGains * 0.30;
  
  const gti = netSalary + Math.max(0, hpIncome) + otherIncome + bizIncome;
  const hpLoss = hpIncome < 0 ? Math.abs(hpIncome) : 0;
  const bfLoss = d.bfLoss || 0;
  const gtiAfterSetOff = Math.max(0, gti - hpLoss - bfLoss);
  
  let totalDeductions = 0;
  if (regime === 'old') {
    const s80C = Math.min(150000, (d.s80C_epf||0)+(d.s80C_ppf||0)+(d.s80C_elss||0)+(d.s80C_lic||0)+(d.s80C_home||0));
    const s80CCD1B = Math.min(50000, d.s80CCD1B||0);
    const s80D = (d.s80D_self||0) + (d.s80D_parent||0);
    totalDeductions = s80C + s80CCD1B + (d.s80CCD2||0) + s80D + (d.s80E||0) + Math.min(10000, d.s80TTA||0) + (d.s80G||0);
  }
  
  const totalIncome = Math.max(0, gtiAfterSetOff - totalDeductions);
  
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
      if (totalIncome > 1500000) normalTax = (totalIncome - 1500000)*0.30 + 150000;
      else if (totalIncome > 1200000) normalTax = (totalIncome - 1200000)*0.20 + 90000;
      else if (totalIncome > 900000) normalTax = (totalIncome - 900000)*0.15 + 45000;
      else if (totalIncome > 600000) normalTax = (totalIncome - 600000)*0.10 + 15000;
      else if (totalIncome > 300000) normalTax = (totalIncome - 300000)*0.05;
    }
  } else {
    const age = d.age || 30;
    const basic_exempt = age >= 80 ? 500000 : age >= 60 ? 300000 : 250000;
    if (totalIncome > 1000000) normalTax = (totalIncome - 1000000)*0.30 + 112500;
    else if (totalIncome > 500000) normalTax = (totalIncome - 500000)*0.20 + 12500;
    else if (totalIncome > basic_exempt) normalTax = (totalIncome - basic_exempt)*0.05;
  }
  
  const rebateLimit = (regime === 'new' && ay === '2026-27') ? 1200000 : (regime === 'new' ? 700000 : 500000);
  const rebate87A = totalIncome <= rebateLimit ? Math.min(normalTax, (regime === 'new' ? 60000 : 12500)) : 0;
  
  const taxAfterRebate = Math.max(0, normalTax - rebate87A);
  
  let surcharge = 0;
  if (totalIncome > 50000000) surcharge = taxAfterRebate * 0.37;
  else if (totalIncome > 20000000) surcharge = taxAfterRebate * 0.25;
  else if (totalIncome > 10000000) surcharge = taxAfterRebate * 0.15;
  else if (totalIncome > 5000000) surcharge = taxAfterRebate * 0.10;
  
  const cess = (taxAfterRebate + surcharge) * 0.04;
  const totalTaxLiability = taxAfterRebate + surcharge + cess + vdaTax + cgTax;
  
  const totalTaxPaid = (d.tdsS192||0)+(d.tds194A||0)+(d.tdsOther||0)+(d.adv15Jun||0)+(d.adv15Sep||0)+(d.adv15Dec||0)+(d.adv15Mar||0)+(d.selfTax||0);
  
  const taxPayable = Math.max(0, totalTaxLiability - totalTaxPaid);
  const refund = Math.max(0, totalTaxPaid - totalTaxLiability);
  
  return {
    grossSalary, hraExempt, netSalary, hpIncome, cgTax, bizIncome,
    otherIncome, vdaTax, gti, gtiAfterSetOff, totalDeductions, totalIncome,
    normalTax, rebate87A, surcharge, cess, totalTaxLiability, totalTaxPaid, taxPayable, refund, vdaGains
  };
}
