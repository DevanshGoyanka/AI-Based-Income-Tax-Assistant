import { useState, useMemo, useCallback } from "react";

// ─── Design Tokens (matching dashboard) ─────────────────────────────────────
const css = `
  @import url('https://fonts.googleapis.com/css2?family=Crimson+Pro:wght@400;500;600&family=DM+Sans:wght@300;400;500;600&family=DM+Mono:wght@400;500&display=swap');
  :root {
    --navy: #0B1929; --navy-mid: #112236; --navy-light: #1A3150;
    --gold: #C9943A; --gold-light: #E8B86D; --gold-pale: #FDF3E0;
    --accent-blue: #1D6FA4; --accent-teal: #0F766E;
    --success: #15803D; --success-bg: #DCFCE7;
    --warning: #B45309; --warning-bg: #FEF3C7;
    --danger: #B91C1C; --danger-bg: #FEE2E2;
    --info: #1E40AF; --info-bg: #DBEAFE;
    --bg: #F4F6F9; --bg-card: #FFFFFF;
    --border: #E1E7EF; --border-strong: #C8D3E0;
    --text-primary: #0F1E2D; --text-secondary: #4A5D72; --text-muted: #8A9BB0;
    --sidebar-w: 258px;
    --radius: 10px; --radius-sm: 6px;
    font-family: 'DM Sans', sans-serif;
  }
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body { overflow: hidden; }
  .crimson { font-family: 'Crimson Pro', serif; }
  .mono { font-family: 'DM Mono', monospace; }

  /* Scrollbar */
  ::-webkit-scrollbar { width: 5px; height: 5px; }
  ::-webkit-scrollbar-thumb { background: var(--border-strong); border-radius: 3px; }

  /* Badge */
  .badge { display:inline-flex;align-items:center;gap:4px;padding:3px 9px;border-radius:20px;font-size:11.5px;font-weight:600;white-space:nowrap; }
  .badge-success { background:var(--success-bg);color:var(--success); }
  .badge-warning { background:var(--warning-bg);color:var(--warning); }
  .badge-danger  { background:var(--danger-bg);color:var(--danger); }
  .badge-info    { background:var(--info-bg);color:var(--info); }
  .badge-gold    { background:var(--gold-pale);color:#92640A; }
  .badge-muted   { background:var(--bg);color:var(--text-secondary);border:1px solid var(--border); }
  .badge-navy    { background:#E0E8F4;color:var(--navy); }
  .badge-dot::before { content:'';width:5px;height:5px;border-radius:50%;background:currentColor;display:inline-block; }

  /* Table */
  table { width:100%;border-collapse:collapse; }
  thead th { text-align:left;padding:10px 14px;font-size:11.5px;font-weight:600;color:var(--text-muted);text-transform:uppercase;letter-spacing:0.5px;background:var(--bg);border-bottom:1px solid var(--border);white-space:nowrap; }
  tbody td { padding:12px 14px;font-size:13.5px;border-bottom:1px solid var(--border);color:var(--text-primary);vertical-align:middle; }
  tbody tr:last-child td { border-bottom:none; }
  tbody tr:hover { background:#FAFBFD; }

  /* Progress */
  .progress-bar { height:8px;background:var(--bg);border-radius:4px;overflow:hidden; }
  .progress-fill { height:100%;border-radius:4px; }
  .fill-gold { background:var(--gold); }
  .fill-teal { background:var(--accent-teal); }
  .fill-blue { background:var(--accent-blue); }
  .fill-danger { background:#EF4444; }

  /* Animations */
  @keyframes pulse { 0%,100%{opacity:1;transform:scale(1)} 50%{opacity:0.6;transform:scale(1.3)} }
  @keyframes slideIn { from{transform:translateX(100%);opacity:0} to{transform:translateX(0);opacity:1} }

  /* Computation specific */
  .comp-tab { display:flex;align-items:center;gap:6px;padding:10px 14px;font-size:12.5px;font-weight:500;cursor:pointer;border-bottom:2px solid transparent;transition:all 0.15s;white-space:nowrap;color:rgba(255,255,255,0.55); }
  .comp-tab:hover { color:rgba(255,255,255,0.85);background:rgba(255,255,255,0.05); }
  .comp-tab.active { color:var(--gold-light);border-bottom-color:var(--gold);background:rgba(201,148,58,0.1); }
  .comp-tab .amount { font-family:'DM Mono',monospace;font-size:10.5px;opacity:0.7; }

  .field-wrap label { font-size:11px;font-weight:600;text-transform:uppercase;letter-spacing:0.4px;color:var(--text-muted);display:block;margin-bottom:5px; }
  .field-input { width:100%;padding:8px 10px 8px 22px;background:var(--bg-card);border:1px solid var(--border-strong);border-radius:7px;font-size:13px;font-family:'DM Sans',sans-serif;color:var(--text-primary);outline:none;transition:border-color 0.2s; }
  .field-input:focus { border-color:var(--gold); }
  .field-input.computed { background:var(--gold-pale);border-color:#E8C97A;color:#6B4A0A;font-weight:600;cursor:default; }
  .field-prefix { position:absolute;left:8px;top:50%;transform:translateY(-50%);font-size:11px;color:var(--text-muted);pointer-events:none; }
`;

// ─── Helpers ─────────────────────────────────────────────────────────────────
const fmt = (n) => (Math.round(n || 0)).toLocaleString("en-IN");
const INR = (n) => `₹${fmt(n)}`;

// ─── Tax Engine ───────────────────────────────────────────────────────────────
function computeTax(d, ay, regime) {
  const hraExempt = Math.min(d.hra || 0, Math.max(0, (d.hraRent || 0) - 0.1 * ((d.basic || 0) + (d.da || 0))), (d.hraMetro ? 0.5 : 0.4) * ((d.basic || 0) + (d.da || 0)));
  const grossSalary = (d.basic||0)+(d.da||0)+(d.hra||0)+(d.bonus||0)+(d.allowances||0)+(d.perquisites||0);
  const stdDed = 75000;
  const netSalary = Math.max(0, grossSalary - hraExempt - stdDed - Math.min(d.profTax || 0, 2500));

  let hpIncome = d.hpType === "letout"
    ? Math.max(0,(d.grossRent||0)-(d.munTax||0)) - Math.round(Math.max(0,(d.grossRent||0)-(d.munTax||0))*0.30) - (d.homeLoanInt||0)
    : -Math.min(d.sopLoanInt || 0, 200000);

  const stcgRate1 = 0.20;
  const stcg111A = ((d.stcgPre||0)*0.15)+((d.stcgPost||0)*0.20);
  const ltcgExempt = ay === "2026-27" ? 125000 : 100000;
  const ltcg112A_taxable = Math.max(0,(d.ltcgPre||0)+(d.ltcgPost||0)-ltcgExempt);
  const ltcg112A_tax = ltcg112A_taxable * (ay === "2026-27" ? 0.125 : 0.10);
  const cgTax = stcg111A + ltcg112A_tax + (d.stcgOther||0)*0.20 + (d.ltcgOther||0)*0.125;
  const bizIncome = d.bizPresumptive === "44AD" ? Math.max((d.bizTurnover||0)*0.08, d.bizDeclared||0) : d.bizPresumptive === "44ADA" ? Math.max((d.bizTurnover||0)*0.50, d.bizDeclared||0) : (d.bpNetProfit||0);
  const otherIncome = (d.interestSB||0)+(d.interestFD||0)+(d.dividends||0)+(d.familyPension||0)+(d.otherMisc||0);
  const vdaGains = Math.max(0, d.vdaGains||0);
  const vdaTax = vdaGains * 0.30;
  const gti = netSalary + Math.max(0, hpIncome) + otherIncome + bizIncome;
  const hpLoss = hpIncome < 0 ? Math.abs(hpIncome) : 0;
  const bfLoss = d.bfLoss||0;

  let totalDed = Math.min(d.s80CCD1B||0, 50000);
  if (regime === "old") {
    totalDed += Math.min((d.s80C||0),150000) + Math.min(d.s80D_self||0,25000) + Math.min(d.s80D_parent||0,50000) + (d.s80E||0) + Math.min(d.s80TTA||0,10000) + (d.s80G||0);
  }

  const taxableNormal = Math.max(0, gti - hpLoss - bfLoss - totalDed);
  let normalTax = 0;

  if (regime === "new") {
    if (ay === "2026-27") {
      if (taxableNormal > 2400000) normalTax = (taxableNormal-2400000)*0.30+300000;
      else if (taxableNormal > 2000000) normalTax = (taxableNormal-2000000)*0.25+200000;
      else if (taxableNormal > 1600000) normalTax = (taxableNormal-1600000)*0.20+120000;
      else if (taxableNormal > 1200000) normalTax = (taxableNormal-1200000)*0.15+60000;
      else if (taxableNormal > 800000) normalTax = (taxableNormal-800000)*0.10+20000;
      else if (taxableNormal > 400000) normalTax = (taxableNormal-400000)*0.05;
      if (taxableNormal <= 1200000) normalTax = 0;
    } else {
      if (taxableNormal > 1500000) normalTax = (taxableNormal-1500000)*0.30+140000;
      else if (taxableNormal > 1200000) normalTax = (taxableNormal-1200000)*0.20+80000;
      else if (taxableNormal > 1000000) normalTax = (taxableNormal-1000000)*0.15+50000;
      else if (taxableNormal > 700000) normalTax = (taxableNormal-700000)*0.10+20000;
      else if (taxableNormal > 300000) normalTax = (taxableNormal-300000)*0.05;
      if (taxableNormal <= 700000) normalTax = 0;
    }
  } else {
    if (taxableNormal > 1000000) normalTax = (taxableNormal-1000000)*0.30+112500;
    else if (taxableNormal > 500000) normalTax = (taxableNormal-500000)*0.20+12500;
    else if (taxableNormal > 250000) normalTax = (taxableNormal-250000)*0.05;
    if (taxableNormal <= 500000) normalTax = 0;
  }

  const grossTax = normalTax + cgTax + vdaTax;
  const cess = Math.round(grossTax * 0.04);
  const totalTax = Math.round(grossTax + cess);
  const paid = (d.tds||0)+(d.advTax||0)+(d.selfTax||0);
  const balance = totalTax - paid;

  return { hraExempt, grossSalary, netSalary, hpIncome, hpLoss, stcgTotal:(d.stcgPre||0)+(d.stcgPost||0)+(d.stcgOther||0), ltcgTotal:(d.ltcgPre||0)+(d.ltcgPost||0)+(d.ltcgOther||0), cgTax, bizIncome, otherIncome, vdaGains, vdaTax, gti, bfLoss, totalDed, taxableNormal, normalTax, grossTax, cess, totalTax, paid, balance };
}

// ─── Shared UI Primitives ─────────────────────────────────────────────────────
const Btn = ({ children, onClick, variant = "ghost", sm, style }) => {
  const styles = {
    primary: { background:"var(--gold)",color:"var(--navy)",border:"none" },
    secondary: { background:"var(--bg)",color:"var(--text-primary)",border:"1px solid var(--border-strong)" },
    ghost: { background:"transparent",color:"var(--text-secondary)",border:"1px solid var(--border)" },
    danger: { background:"var(--danger-bg)",color:"var(--danger)",border:"1px solid #FECACA" },
    navy: { background:"var(--navy)",color:"var(--gold-light)",border:"none" },
  };
  return (
    <button onClick={onClick} style={{ display:"inline-flex",alignItems:"center",gap:5,padding:sm?"5px 11px":"8px 16px",borderRadius:7,fontSize:sm?12:13,fontWeight:500,fontFamily:"'DM Sans',sans-serif",cursor:"pointer",whiteSpace:"nowrap",transition:"all 0.15s",...styles[variant],...style }}>
      {children}
    </button>
  );
};

const Card = ({ title, badge, children, action, mb = 16 }) => (
  <div style={{ background:"var(--bg-card)",border:"1px solid var(--border)",borderRadius:"var(--radius)",overflow:"hidden",marginBottom:mb }}>
    {title && (
      <div style={{ display:"flex",alignItems:"center",justifyContent:"space-between",padding:"12px 18px",borderBottom:"1px solid var(--border)",background:"var(--bg)" }}>
        <div style={{ display:"flex",alignItems:"center",gap:8 }}>
          <span style={{ fontSize:13.5,fontWeight:600,color:"var(--text-primary)" }}>{title}</span>
          {badge && <span className="badge badge-muted" style={{ fontSize:11 }}>{badge}</span>}
        </div>
        {action}
      </div>
    )}
    <div style={{ padding:"16px 18px" }}>{children}</div>
  </div>
);

const Grid = ({ cols = 3, children, gap = 14 }) => (
  <div style={{ display:"grid",gridTemplateColumns:`repeat(${cols},1fr)`,gap }}>{children}</div>
);

const Field = ({ label, value, onChange, computed, hint, type = "number", optional, readOnly }) => (
  <div className="field-wrap">
    <label>{label}{optional && <span style={{ color:"var(--text-muted)",fontWeight:400,fontSize:10,marginLeft:4 }}>optional</span>}</label>
    <div style={{ position:"relative" }}>
      {type === "number" && <span className="field-prefix">₹</span>}
      <input
        type="text"
        inputMode={type === "number" ? "numeric" : undefined}
        value={type === "number" ? (value ? fmt(value) : "") : (value || "")}
        readOnly={readOnly || computed}
        onChange={onChange && !computed && !readOnly ? (e) => onChange(type==="number" ? (parseFloat(e.target.value.replace(/,/g,""))||0) : e.target.value) : undefined}
        placeholder={computed ? "Auto" : "0"}
        className={`field-input ${computed ? "computed" : ""}`}
        style={{ paddingLeft: type==="number" ? 22 : 10 }}
      />
    </div>
    {hint && <p style={{ fontSize:11,color:"var(--text-muted)",marginTop:3 }}>{hint}</p>}
  </div>
);

const Seg = ({ options, value, onChange }) => (
  <div style={{ display:"inline-flex",background:"var(--bg)",borderRadius:7,padding:2,border:"1px solid var(--border)" }}>
    {options.map(o => (
      <button key={String(o.value)} onClick={() => onChange(o.value)} style={{ padding:"5px 11px",fontSize:12,fontWeight:500,borderRadius:5,border:"none",cursor:"pointer",transition:"all 0.15s",background:value===o.value?"var(--navy)":"transparent",color:value===o.value?"var(--gold-light)":"var(--text-secondary)" }}>
        {o.label}
      </button>
    ))}
  </div>
);

// ─── Computation Sections ─────────────────────────────────────────────────────
function SalarySection({ d, set, regime }) {
  const hraExempt = Math.min(d.hra||0, Math.max(0,(d.hraRent||0)-0.1*((d.basic||0)+(d.da||0))), (d.hraMetro?0.5:0.4)*((d.basic||0)+(d.da||0)));
  const gross = (d.basic||0)+(d.da||0)+(d.hra||0)+(d.bonus||0)+(d.allowances||0)+(d.perquisites||0);
  const net = Math.max(0,gross-hraExempt-75000-Math.min(d.profTax||0,2500));
  return (
    <div>
      <Card title="Salary Components u/s 17(1)">
        <Grid cols={3}>
          <Field label="Basic Salary" value={d.basic} onChange={v=>set("basic",v)} />
          <Field label="Dearness Allowance" value={d.da} onChange={v=>set("da",v)} />
          <Field label="HRA Received" value={d.hra} onChange={v=>set("hra",v)} />
          <Field label="Bonus / Incentives" value={d.bonus} onChange={v=>set("bonus",v)} />
          <Field label="Special Allowances" value={d.allowances} onChange={v=>set("allowances",v)} />
          <Field label="Perquisites u/s 17(2)" value={d.perquisites} onChange={v=>set("perquisites",v)} hint="ESOP, car, rent-free accom." />
          <Field label="Gross Salary" value={gross} computed />
        </Grid>
      </Card>
      <Card title="HRA Exemption u/s 10(13A)">
        <Grid cols={3}>
          <Field label="Annual Rent Paid" value={d.hraRent} onChange={v=>set("hraRent",v)} />
          <div className="field-wrap"><label>City Type</label><Seg options={[{label:"Metro (50%)",value:true},{label:"Non-Metro (40%)",value:false}]} value={d.hraMetro!==false} onChange={v=>set("hraMetro",v)} /></div>
          <Field label="HRA Exempt (Auto)" value={hraExempt} computed hint="Min of 3 conditions" />
        </Grid>
      </Card>
      <Card title="Deductions from Salary u/s 16">
        <Grid cols={3}>
          <Field label="Standard Deduction u/s 16(ia)" value={75000} computed />
          <Field label="Professional Tax u/s 16(iii)" value={d.profTax} onChange={v=>set("profTax",v)} hint="Max ₹2,500/year" />
          <Field label="Net Taxable Salary" value={net} computed />
        </Grid>
      </Card>
    </div>
  );
}

function HousePropSection({ d, set }) {
  const hpType = d.hpType || "sop";
  const nav = Math.max(0,(d.grossRent||0)-(d.munTax||0));
  const stdDedHP = Math.round(nav*0.30);
  const hpIncome = hpType==="letout" ? nav-stdDedHP-(d.homeLoanInt||0) : -Math.min(d.sopLoanInt||0,200000);
  return (
    <div>
      <div style={{ marginBottom:14 }}>
        <Seg options={[{label:"Self-Occupied",value:"sop"},{label:"Let Out",value:"letout"},{label:"Deemed Let Out",value:"deemed"}]} value={hpType} onChange={v=>set("hpType",v)} />
      </div>
      {hpType==="letout" || hpType==="deemed" ? (
        <Card title="Let-Out Income Computation">
          <Grid cols={3}>
            <Field label="Gross Annual Rent" value={d.grossRent} onChange={v=>set("grossRent",v)} />
            <Field label="Municipal Tax Paid" value={d.munTax} onChange={v=>set("munTax",v)} />
            <Field label="Net Annual Value" value={nav} computed />
            <Field label="Std. Deduction 30% u/s 24(a)" value={stdDedHP} computed />
            <Field label="Home Loan Interest u/s 24(b)" value={d.homeLoanInt} onChange={v=>set("homeLoanInt",v)} />
            <Field label="Net HP Income / (Loss)" value={hpIncome} computed hint={hpIncome<0?"Loss — capped ₹2L set-off":"Taxable at slab"} />
          </Grid>
        </Card>
      ) : (
        <Card title="Self-Occupied Property">
          <div style={{ background:"var(--info-bg)",borderRadius:7,padding:"10px 14px",marginBottom:12,fontSize:13,color:"var(--info)" }}>Annual Value of SOP = NIL. Only home loan interest u/s 24(b) deductible, capped at ₹2,00,000.</div>
          <Grid cols={3}>
            <Field label="Home Loan Interest u/s 24(b)" value={d.sopLoanInt} onChange={v=>set("sopLoanInt",v)} hint="Max ₹2,00,000 / year" />
            <Field label="Pre-Construction Interest (1/5th)" value={d.preConInt} onChange={v=>set("preConInt",v)} optional />
            <Field label="HP Loss (Auto-capped ₹2L)" value={-Math.min(d.sopLoanInt||0,200000)} computed />
          </Grid>
        </Card>
      )}
    </div>
  );
}

function CapGainsSection({ d, set, ay }) {
  const ltcgExempt = ay==="2026-27" ? 125000 : 100000;
  const ltcg112A_rate = ay==="2026-27" ? 0.125 : 0.10;
  return (
    <div>
      <div style={{ background:"var(--warning-bg)",border:"1px solid #FDE68A",borderRadius:8,padding:"10px 14px",marginBottom:14,fontSize:13,color:"var(--warning)",display:"flex",gap:8 }}>
        <span>⚡</span>
        <span>Budget 2024: STCG §111A → <strong>20%</strong> (post 23 Jul 2024). LTCG §112A → <strong>12.5%</strong> (post 23 Jul 2024).</span>
      </div>
      <Card title="Short-Term Capital Gains (STCG)" badge="§111A / §112">
        <Grid cols={3}>
          <Field label="STCG §111A — Pre 23 Jul 2024 (15%)" value={d.stcgPre} onChange={v=>set("stcgPre",v)} hint="Listed equity/MF" />
          <Field label="STCG §111A — Post 23 Jul 2024 (20%)" value={d.stcgPost} onChange={v=>set("stcgPost",v)} hint="Listed equity/MF" />
          <Field label="STCG Tax (Auto)" value={Math.round(((d.stcgPre||0)*0.15)+((d.stcgPost||0)*0.20))} computed />
          <Field label="STCG Other Assets §112 (Slab)" value={d.stcgOther} onChange={v=>set("stcgOther",v)} hint="Property/unlisted shares" />
        </Grid>
      </Card>
      <Card title="Long-Term Capital Gains (LTCG)" badge="§112A / §112">
        <Grid cols={3}>
          <Field label="LTCG §112A — Pre 23 Jul 2024 (10%)" value={d.ltcgPre} onChange={v=>set("ltcgPre",v)} hint="Listed equity/MF" />
          <Field label="LTCG §112A — Post 23 Jul 2024 (12.5%)" value={d.ltcgPost} onChange={v=>set("ltcgPost",v)} hint="Listed equity/MF" />
          <Field label={`LTCG Exempt Threshold (AY ${ay})`} value={ltcgExempt} computed hint="Auto-applied" />
          <Field label="LTCG Tax §112A (Auto)" value={Math.round(Math.max(0,(d.ltcgPre||0)+(d.ltcgPost||0)-ltcgExempt)*ltcg112A_rate)} computed />
          <Field label="LTCG §112 — Without Indexation (12.5%)" value={d.ltcgOther} onChange={v=>set("ltcgOther",v)} hint="Property/bonds post-Jul 2024" />
        </Grid>
      </Card>
    </div>
  );
}

function BusinessSection({ d, set }) {
  const bizType = d.bizType || "44AD";
  return (
    <div>
      <div style={{ marginBottom:14 }}>
        <Seg options={[{label:"44AD — Business",value:"44AD"},{label:"44ADA — Profession",value:"44ADA"},{label:"Regular Books",value:"regular"}]} value={bizType} onChange={v=>set("bizType",v)} />
      </div>
      <Card title="Presumptive Income Details" badge={bizType}>
        <Grid cols={3}>
          <Field label="Gross Turnover / Receipts" value={d.bizTurnover} onChange={v=>set("bizTurnover",v)} />
          <Field label="Digital Receipts (for 6% rate)" value={d.bizDigital} onChange={v=>set("bizDigital",v)} optional hint="44AD: 8% cash, 6% digital" />
          <Field label="Declared Income" value={d.bizDeclared} onChange={v=>set("bizDeclared",v)} hint={bizType==="44AD"?"Min 8%/6% of turnover":"Min 50% of receipts"} />
        </Grid>
      </Card>
    </div>
  );
}

function OtherSourcesSection({ d, set }) {
  const total = (d.interestSB||0)+(d.interestFD||0)+(d.dividends||0)+(d.familyPension||0)+(d.otherMisc||0);
  return (
    <div>
      <Card title="Interest Income">
        <Grid cols={3}>
          <Field label="Savings Bank Interest §80TTA" value={d.interestSB} onChange={v=>set("interestSB",v)} hint="₹10,000 deductible u/s 80TTA" />
          <Field label="Fixed Deposit / RD Interest" value={d.interestFD} onChange={v=>set("interestFD",v)} hint="Fully taxable at slab rate" />
          <Field label="IT Refund Interest u/s 244A" value={d.interestRefund} onChange={v=>set("interestRefund",v)} optional />
        </Grid>
      </Card>
      <Card title="Dividends & Other Income">
        <Grid cols={3}>
          <Field label="Dividends (Domestic Companies / MFs)" value={d.dividends} onChange={v=>set("dividends",v)} hint="Fully taxable at slab" />
          <Field label="Family Pension" value={d.familyPension} onChange={v=>set("familyPension",v)} hint="Taxable u/s 56" />
          <Field label="Any Other Income" value={d.otherMisc} onChange={v=>set("otherMisc",v)} optional />
          <Field label="Total Other Sources" value={total} computed />
        </Grid>
      </Card>
    </div>
  );
}

function VDASection({ d, set }) {
  const vdaTax = Math.round(Math.max(0,d.vdaGains||0)*0.30);
  return (
    <div>
      <div style={{ background:"#FFF7ED",border:"1px solid #FDBA74",borderRadius:8,padding:"10px 14px",marginBottom:14,fontSize:13,color:"#9A3412",display:"flex",gap:8 }}>
        <span>₿</span>
        <span><strong>Section 115BBH — VDA Rules:</strong> 30% flat tax. No deductions except acquisition cost. Loss cannot be set off. TDS u/s 194S @ 1%.</span>
      </div>
      <Card title="VDA / Crypto Computation" badge="§115BBH">
        <Grid cols={3}>
          <Field label="Total Sale Consideration" value={d.vdaSale} onChange={v=>set("vdaSale",v)} />
          <Field label="Cost of Acquisition (FIFO)" value={d.vdaCOA} onChange={v=>set("vdaCOA",v)} hint="Only deduction allowed" />
          <Field label="Net VDA Gains / (Loss)" value={d.vdaGains} onChange={v=>set("vdaGains",v)} hint="Loss floored to ₹0" />
          <Field label="VDA Tax @ 30% (Auto)" value={vdaTax} computed />
          <Field label="TDS §194S Deducted by Exchange" value={d.vdaTDS} onChange={v=>set("vdaTDS",v)} hint="1% of consideration" />
        </Grid>
      </Card>
    </div>
  );
}

function DeductionsSection({ d, set, regime }) {
  const isOld = regime === "old";
  const d80C = Math.min((d.s80C_epf||0)+(d.s80C_ppf||0)+(d.s80C_elss||0)+(d.s80C_lic||0)+(d.s80C_home||0), 150000);
  const total80CCD1B = Math.min(d.s80CCD1B||0, 50000);
  return (
    <div>
      {!isOld && <div style={{ background:"var(--warning-bg)",border:"1px solid #FDE68A",borderRadius:8,padding:"10px 14px",marginBottom:14,fontSize:13,color:"var(--warning)" }}><strong>New Regime:</strong> Most Chapter VI-A deductions not available. Only §80CCD(1B) NPS available.</div>}
      <Card title="Section 80C — Limit ₹1,50,000" badge={isOld?`Claimed: ₹${fmt(d80C)}`:"Not available in new regime"}>
        <Grid cols={3}>
          <Field label="EPF / VPF" value={d.s80C_epf} onChange={v=>set("s80C_epf",v)} readOnly={!isOld} />
          <Field label="PPF Contribution" value={d.s80C_ppf} onChange={v=>set("s80C_ppf",v)} readOnly={!isOld} />
          <Field label="ELSS / Tax-saving MFs" value={d.s80C_elss} onChange={v=>set("s80C_elss",v)} readOnly={!isOld} />
          <Field label="LIC Premium" value={d.s80C_lic} onChange={v=>set("s80C_lic",v)} readOnly={!isOld} />
          <Field label="Home Loan Principal" value={d.s80C_home} onChange={v=>set("s80C_home",v)} readOnly={!isOld} />
          <Field label="Total §80C (capped ₹1.5L)" value={d80C} computed />
        </Grid>
      </Card>
      <Card title="NPS — §80CCD(1B)" badge="Available in both regimes">
        <Grid cols={3}>
          <Field label="NPS Tier-I Contribution §80CCD(1B)" value={d.s80CCD1B} onChange={v=>set("s80CCD1B",v)} hint="Max ₹50,000." />
          <Field label="Employer NPS §80CCD(2)" value={d.s80CCD2} onChange={v=>set("s80CCD2",v)} hint="Max 10% basic+DA" optional />
        </Grid>
      </Card>
      {isOld && (
        <Card title="Other Deductions">
          <Grid cols={3}>
            <Field label="§80D — Self/Family Health Ins." value={d.s80D_self} onChange={v=>set("s80D_self",v)} hint="Max ₹25,000" />
            <Field label="§80D — Parents Health Ins." value={d.s80D_parent} onChange={v=>set("s80D_parent",v)} hint="Max ₹25,000 / ₹50,000 senior" />
            <Field label="§80E — Education Loan Interest" value={d.s80E} onChange={v=>set("s80E",v)} hint="No upper limit; 8 years" />
            <Field label="§80TTA — SB Interest" value={d.s80TTA} onChange={v=>set("s80TTA",v)} hint="Max ₹10,000" />
            <Field label="§80G — Donations" value={d.s80G} onChange={v=>set("s80G",v)} hint="50%/100% of approved fund" optional />
          </Grid>
        </Card>
      )}
    </div>
  );
}

function LossesSection({ d, set }) {
  return (
    <div>
      <Card title="Schedule BFL — Brought Forward Losses">
        <Grid cols={3}>
          <Field label="BF Business Loss (8-yr limit)" value={d.bfBizLoss} onChange={v=>set("bfBizLoss",v)} />
          <Field label="BF STCG Loss" value={d.bfSTCG} onChange={v=>set("bfSTCG",v)} />
          <Field label="BF LTCG Loss" value={d.bfLTCG} onChange={v=>set("bfLTCG",v)} />
          <Field label="BF Speculative Loss (4-yr limit)" value={d.bfSpecLoss} onChange={v=>set("bfSpecLoss",v)} />
          <Field label="Total BF Loss Utilized" value={(d.bfBizSetOff||0)+(d.bfSTCG||0)+(d.bfLTCG||0)} computed />
        </Grid>
      </Card>
      <Card title="Schedule CYLA — Current Year Loss Rules" badge="Set-off matrix">
        <div style={{ fontSize:12.5,color:"var(--text-secondary)",lineHeight:1.7 }}>
          <p>• HP loss (SOP) — capped ₹2L against other heads</p>
          <p>• Business loss — cannot be set off against salary</p>
          <p>• STCG loss — can set off against STCG or LTCG</p>
          <p>• LTCG loss — can only be set off against LTCG</p>
          <p>• Speculative loss — only against speculative income</p>
        </div>
      </Card>
    </div>
  );
}

function TDSSection({ d, set }) {
  const totalTDS = (d.tdsS192||0)+(d.tds194A||0)+(d.tds194B||0)+(d.tds194IA||0)+(d.tds194S||0)+(d.tdsOther||0);
  const totalAdvTax = (d.adv15Jun||0)+(d.adv15Sep||0)+(d.adv15Dec||0)+(d.adv15Mar||0);
  return (
    <div>
      <Card title="TDS Credits — from 26AS / AIS" badge="Auto-matched">
        <Grid cols={3}>
          <Field label="TDS u/s 192 — Salary (Form 16)" value={d.tdsS192} onChange={v=>set("tdsS192",v)} hint="From employer TAN" />
          <Field label="TDS u/s 194A — Interest" value={d.tds194A} onChange={v=>set("tds194A",v)} hint="Bank FD, NSS etc." />
          <Field label="TDS u/s 194B — Lottery" value={d.tds194B} onChange={v=>set("tds194B",v)} optional />
          <Field label="TDS u/s 194-IA — Property Sale" value={d.tds194IA} onChange={v=>set("tds194IA",v)} optional />
          <Field label="TDS u/s 194S — VDA Transfer" value={d.tds194S} onChange={v=>set("tds194S",v)} optional />
          <Field label="Total TDS/TCS (Auto)" value={totalTDS} computed />
        </Grid>
      </Card>
      <Card title="Advance Tax Challans">
        <Grid cols={4}>
          <Field label="15 June (15%)" value={d.adv15Jun} onChange={v=>set("adv15Jun",v)} />
          <Field label="15 September (45%)" value={d.adv15Sep} onChange={v=>set("adv15Sep",v)} />
          <Field label="15 December (75%)" value={d.adv15Dec} onChange={v=>set("adv15Dec",v)} />
          <Field label="15 March (100%)" value={d.adv15Mar} onChange={v=>set("adv15Mar",v)} />
          <Field label="Total Advance Tax" value={totalAdvTax} computed />
          <Field label="Self-Assessment Tax" value={d.selfTax} onChange={v=>set("selfTax",v)} />
        </Grid>
      </Card>
    </div>
  );
}

function TaxComputationSection({ d, c, ay, regime }) {
  const rows = [
    ["Salary Income (Net)", c.netSalary, false],
    ["House Property Income / (Loss)", c.hpIncome, false],
    ["Capital Gains (Total)", c.stcgTotal + c.ltcgTotal, false],
    ["Business / Profession Income", c.bizIncome, false],
    ["Other Sources", c.otherIncome, false],
    ["Gross Total Income (GTI)", c.gti, true],
    ["Less: Chapter VI-A Deductions", -c.totalDed, false],
    ["Less: HP Loss Set-off", -c.hpLoss, false],
    ["Total Taxable Income", c.taxableNormal, true],
    ["Tax on Normal Income", c.normalTax, false],
    ["Tax on CG (Special Rates)", c.cgTax, false],
    ["Tax on VDA §115BBH (30%)", c.vdaTax, false],
    ["Sub-total Tax", c.grossTax, true],
    ["Health & Education Cess @ 4%", c.cess, false],
    ["Gross Tax Liability", c.totalTax, true],
    ["Less: TDS / TCS", -((d.tdsS192||0)+(d.tds194A||0)+(d.tds194S||0)+(d.tdsOther||0)), false],
    ["Less: Advance Tax Paid", -((d.adv15Jun||0)+(d.adv15Sep||0)+(d.adv15Dec||0)+(d.adv15Mar||0)), false],
    ["Less: Self-Assessment Tax", -(d.selfTax||0), false],
    [c.balance>=0?"TAX PAYABLE":"REFUND DUE", Math.abs(c.balance), true, c.balance>=0?"var(--danger)":"var(--success)"],
  ];
  return (
    <div>
      <Grid cols={2} gap={14}>
        <div style={{ background:"var(--navy)",borderRadius:12,padding:20,color:"white" }}>
          <div style={{ fontSize:11,color:"rgba(255,255,255,0.5)",textTransform:"uppercase",letterSpacing:1,marginBottom:6 }}>Total Tax Liability · AY {ay}</div>
          <div style={{ fontFamily:"'Crimson Pro',serif",fontSize:36,fontWeight:600,color:"var(--gold-light)" }}>{INR(c.totalTax)}</div>
          <div style={{ fontSize:12,color:"rgba(255,255,255,0.4)",marginTop:4 }}>{regime==="new"?"New Regime §115BAC":"Old Regime"}</div>
        </div>
        <div style={{ background:c.balance>=0?"var(--danger-bg)":"var(--success-bg)",border:`2px solid ${c.balance>=0?"#FECACA":"#BBF7D0"}`,borderRadius:12,padding:20 }}>
          <div style={{ fontSize:11,color:c.balance>=0?"var(--danger)":"var(--success)",textTransform:"uppercase",letterSpacing:1,marginBottom:6 }}>{c.balance>=0?"Tax Payable":"Refund Due"}</div>
          <div style={{ fontFamily:"'Crimson Pro',serif",fontSize:36,fontWeight:600,color:c.balance>=0?"var(--danger)":"var(--success)" }}>{INR(Math.abs(c.balance))}</div>
          <div style={{ fontSize:12,color:c.balance>=0?"#DC6B6B":"#4CAF50",marginTop:4 }}>{c.balance>=0?"Pay before due date to avoid §234B interest":"Expected refund to pre-validated bank a/c"}</div>
        </div>
      </Grid>
      <Card title="Statement of Income Computation" badge={`AY ${ay}`} mb={0}>
        <table style={{ width:"100%" }}>
          <tbody>
            {rows.map(([label, value, bold, color], i) => (
              <tr key={i} style={{ borderBottom:"1px solid var(--border)",background:bold?"var(--bg)":"transparent" }}>
                <td style={{ padding:"10px 0",fontSize:13,fontWeight:bold?600:400,color:color||(bold?"var(--text-primary)":"var(--text-secondary)") }}>{label}</td>
                <td style={{ padding:"10px 0",textAlign:"right",fontFamily:"'DM Mono',monospace",fontSize:13,fontWeight:bold?600:400,color:color||(bold?"var(--text-primary)":"var(--text-secondary)") }}>
                  {value < 0 ? `(${INR(Math.abs(value))})` : INR(value)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>
    </div>
  );
}

// ─── ITR Computation View ─────────────────────────────────────────────────────
function ITRComputationView({ client, onBack }) {
  const [activeSection, setActiveSection] = useState("salary");
  const [ay, setAY] = useState("2025-26");
  const [regime, setRegime] = useState("new");
  const [itrForm, setITRForm] = useState("ITR-2");
  const [importBanner, setImportBanner] = useState(null);
  const [d, setD] = useState({
    empName:"Wipro Technologies Ltd", empTAN:"BNGA12345B", empType:"private",
    basic:600000, da:60000, hra:120000, bonus:50000, allowances:24000,
    hraRent:168000, hraMetro:true,
    hpType:"sop", sopLoanInt:180000,
    stcgPost:55000, ltcgPost:180000,
    dividends:8500, interestFD:32000, interestSB:12000,
    s80C_epf:72000, s80C_elss:50000, s80CCD1B:50000,
    s80D_self:20000, s80D_parent:18000,
    tdsS192:82000, tds194A:3200, adv15Mar:8000,
  });

  const set = useCallback((k, v) => setD(prev => ({ ...prev, [k]: v })), []);
  const c = useMemo(() => computeTax(d, ay, regime), [d, ay, regime]);

  const sections = [
    { id:"salary", label:"Salary", icon:"💼", amount:c.netSalary },
    { id:"hp", label:"House Property", icon:"🏠", amount:c.hpIncome },
    { id:"cg", label:"Capital Gains", icon:"📈", amount:c.stcgTotal+c.ltcgTotal },
    { id:"biz", label:"Business", icon:"📋", amount:c.bizIncome },
    { id:"other", label:"Other Sources", icon:"💰", amount:c.otherIncome },
    { id:"vda", label:"VDA / Crypto", icon:"₿", amount:c.vdaGains },
    { id:"ded", label:"Deductions VI-A", icon:"🔖", amount:-c.totalDed },
    { id:"loss", label:"Losses", icon:"↩", amount:-(c.hpLoss+c.bfLoss) },
    { id:"tds", label:"TDS & Advance Tax", icon:"🧾", amount:c.paid },
    { id:"tax", label:"Tax Computation", icon:"⚖", amount:c.totalTax },
  ];

  const sectionContent = {
    salary: <SalarySection d={d} set={set} regime={regime} />,
    hp: <HousePropSection d={d} set={set} />,
    cg: <CapGainsSection d={d} set={set} ay={ay} />,
    biz: <BusinessSection d={d} set={set} />,
    other: <OtherSourcesSection d={d} set={set} />,
    vda: <VDASection d={d} set={set} />,
    ded: <DeductionsSection d={d} set={set} regime={regime} />,
    loss: <LossesSection d={d} set={set} />,
    tds: <TDSSection d={d} set={set} />,
    tax: <TaxComputationSection d={d} c={c} ay={ay} regime={regime} />,
  };

  return (
    <div style={{ display:"flex",flexDirection:"column",height:"100%",background:"var(--bg)",overflow:"hidden" }}>

      {/* ── Client Identity Bar ── */}
      <div style={{ background:"var(--navy)",color:"white",padding:"0 24px",borderBottom:"1px solid rgba(255,255,255,0.1)",flexShrink:0 }}>
        <div style={{ display:"flex",alignItems:"center",gap:16,height:52 }}>
          <button onClick={onBack} style={{ background:"rgba(255,255,255,0.1)",border:"1px solid rgba(255,255,255,0.15)",borderRadius:7,padding:"5px 12px",color:"rgba(255,255,255,0.8)",cursor:"pointer",fontSize:12,display:"flex",alignItems:"center",gap:5 }}>
            ← Back
          </button>
          <div style={{ width:1,height:28,background:"rgba(255,255,255,0.15)" }} />
          <div style={{ width:32,height:32,borderRadius:"50%",background:"var(--gold)",display:"flex",alignItems:"center",justifyContent:"center",fontSize:13,fontWeight:700,color:"var(--navy)",flexShrink:0 }}>
            {client?.initials || "AR"}
          </div>
          <div>
            <div style={{ fontSize:13.5,fontWeight:600,lineHeight:1.2 }}>{client?.name || "Aditya Raj Sharma"}</div>
            <div style={{ fontSize:11,color:"rgba(255,255,255,0.45)",letterSpacing:0.3 }}>PAN: {client?.pan || "ABCPS1234R"} · Individual</div>
          </div>
          <div style={{ width:1,height:28,background:"rgba(255,255,255,0.15)" }} />
          {/* AY */}
          <div style={{ display:"flex",alignItems:"center",gap:6 }}>
            <span style={{ fontSize:11,color:"rgba(255,255,255,0.45)" }}>AY</span>
            <select value={ay} onChange={e=>setAY(e.target.value)} style={{ background:"rgba(255,255,255,0.1)",color:"white",border:"1px solid rgba(255,255,255,0.2)",borderRadius:5,padding:"3px 8px",fontSize:12,cursor:"pointer",outline:"none" }}>
              <option value="2025-26">2025-26</option>
              <option value="2026-27">2026-27</option>
            </select>
          </div>
          <div style={{ display:"flex",alignItems:"center",gap:6 }}>
            <span style={{ fontSize:11,color:"rgba(255,255,255,0.45)" }}>Form</span>
            <select value={itrForm} onChange={e=>setITRForm(e.target.value)} style={{ background:"rgba(255,255,255,0.1)",color:"white",border:"1px solid rgba(255,255,255,0.2)",borderRadius:5,padding:"3px 8px",fontSize:12,cursor:"pointer",outline:"none" }}>
              {["ITR-1","ITR-2","ITR-3","ITR-4"].map(f=><option key={f}>{f}</option>)}
            </select>
          </div>
          <div style={{ display:"flex",alignItems:"center",gap:6 }}>
            <span style={{ fontSize:11,color:"rgba(255,255,255,0.45)" }}>Regime</span>
            <div style={{ display:"flex",background:"rgba(255,255,255,0.08)",border:"1px solid rgba(255,255,255,0.15)",borderRadius:6,padding:2 }}>
              {[{v:"new",l:"New"},{v:"old",l:"Old"}].map(({v,l})=>(
                <button key={v} onClick={()=>setRegime(v)} style={{ padding:"3px 10px",borderRadius:4,border:"none",cursor:"pointer",fontSize:12,fontWeight:500,transition:"all 0.15s",background:regime===v?"var(--gold)":"transparent",color:regime===v?"var(--navy)":"rgba(255,255,255,0.6)" }}>{l}</button>
              ))}
            </div>
          </div>
          <div style={{ marginLeft:"auto",display:"flex",alignItems:"center",gap:8 }}>
            <div style={{ fontSize:11,color:"rgba(255,255,255,0.35)" }}>Last saved: 2 min ago</div>
            <div style={{ width:1,height:20,background:"rgba(255,255,255,0.15)" }} />
            <div style={{ fontSize:12,color:c.balance>=0?"#FCA5A5":"#86EFAC",fontWeight:600 }}>
              {c.balance>=0?"Payable:":"Refund:"} {INR(Math.abs(c.balance))}
            </div>
          </div>
        </div>
      </div>

      {/* ── Import / Action Bar ── */}
      <div style={{ background:"var(--bg-card)",borderBottom:"1px solid var(--border)",padding:"8px 24px",display:"flex",alignItems:"center",gap:8,flexShrink:0,flexWrap:"wrap" }}>
        <span style={{ fontSize:11,fontWeight:700,color:"var(--text-muted)",textTransform:"uppercase",letterSpacing:0.5,marginRight:4 }}>Import</span>
        {[{icon:"📥",label:"AIS / TIS JSON",id:"ais"},{icon:"📄",label:"Form 16 JSON",id:"form16"},{icon:"🔍",label:"Form 16 PDF (OCR)",id:"pdf"}].map(btn=>(
          <Btn key={btn.id} variant="secondary" sm onClick={()=>setImportBanner(btn.id)}>
            <span>{btn.icon}</span>{btn.label}
          </Btn>
        ))}
        <div style={{ width:1,height:20,background:"var(--border)",margin:"0 4px" }} />
        <Btn variant="secondary" sm>💾 Save</Btn>
        <Btn variant="ghost" sm style={{ borderColor:"var(--accent-blue)",color:"var(--accent-blue)" }}>✓ Validate ITR</Btn>
        <div style={{ width:1,height:20,background:"var(--border)",margin:"0 4px" }} />
        <Btn variant="secondary" sm>📊 Download PDF</Btn>
        <Btn variant="navy" sm>⬇ Export ITR JSON</Btn>
        {importBanner && (
          <div style={{ marginLeft:"auto",display:"flex",alignItems:"center",gap:8,background:"var(--success-bg)",border:"1px solid #BBF7D0",borderRadius:8,padding:"6px 12px",fontSize:12,color:"var(--success)" }}>
            <span>✓</span>
            <span>{importBanner==="ais"?"AIS/TIS imported. 1 mismatch flagged.":importBanner==="form16"?"Form 16 JSON imported & auto-populated.":"PDF parsed via OCR. Review highlighted fields."}</span>
            <button onClick={()=>setImportBanner(null)} style={{ background:"none",border:"none",cursor:"pointer",color:"var(--success)",fontSize:14,marginLeft:4 }}>✕</button>
          </div>
        )}
      </div>

      {/* ── Section Tabs Bar (replaces left panel) ── */}
      <div style={{ background:"var(--navy-mid)",borderBottom:"1px solid rgba(255,255,255,0.08)",display:"flex",alignItems:"stretch",overflowX:"auto",flexShrink:0,scrollbarWidth:"none" }}>
        {sections.map(s => (
          <div key={s.id} className={`comp-tab ${activeSection===s.id?"active":""}`} onClick={()=>setActiveSection(s.id)}>
            <span style={{ fontSize:13 }}>{s.icon}</span>
            <span>{s.label}</span>
            {s.amount!==0 && (
              <span className="amount" style={{ color:s.amount<0?"#FCA5A5":activeSection===s.id?"var(--gold-light)":"rgba(255,255,255,0.4)" }}>
                {s.amount<0?`(${fmt(Math.abs(s.amount))})`:fmt(s.amount)}
              </span>
            )}
          </div>
        ))}
      </div>

      {/* ── Content + Right Summary ── */}
      <div style={{ flex:1,display:"flex",overflow:"hidden" }}>
        {/* Main Content */}
        <div style={{ flex:1,overflowY:"auto",padding:"20px 24px" }}>
          <div style={{ maxWidth:900 }}>
            <div style={{ display:"flex",alignItems:"center",justifyContent:"space-between",marginBottom:16 }}>
              <div>
                <h2 style={{ fontSize:15,fontWeight:600,color:"var(--text-primary)" }}>{sections.find(s=>s.id===activeSection)?.label}</h2>
                <p style={{ fontSize:11.5,color:"var(--text-muted)",marginTop:2 }}>
                  {activeSection==="salary"&&"Income under §17 · Standard deduction u/s 16(ia)"}
                  {activeSection==="hp"&&"Annual value computation · u/s 22–27"}
                  {activeSection==="cg"&&"STCG §111A · LTCG §112A · Property gains"}
                  {activeSection==="biz"&&"Presumptive §44AD/44ADA · Regular Schedule BP"}
                  {activeSection==="other"&&"Interest · Dividends · Family Pension · §56"}
                  {activeSection==="vda"&&"Virtual Digital Assets · §115BBH · TDS §194S"}
                  {activeSection==="ded"&&"Chapter VI-A Deductions · §80C to §80U"}
                  {activeSection==="loss"&&"Schedule CYLA · Schedule BFL · §70–80"}
                  {activeSection==="tds"&&"TDS Credits · Advance Tax Challans · 26AS match"}
                  {activeSection==="tax"&&"Final Computation · Regime Comparison · §234A/B/C"}
                </p>
              </div>
              <div style={{ display:"flex",gap:6 }}>
                <Btn sm variant="ghost" onClick={()=>{const idx=sections.findIndex(s=>s.id===activeSection);if(idx>0)setActiveSection(sections[idx-1].id)}}>← Prev</Btn>
                <Btn sm variant="navy" onClick={()=>{const idx=sections.findIndex(s=>s.id===activeSection);if(idx<sections.length-1)setActiveSection(sections[idx+1].id)}}>Next →</Btn>
              </div>
            </div>
            {sectionContent[activeSection]}
          </div>
        </div>

        {/* Right Summary Panel */}
        <div style={{ width:220,background:"var(--bg-card)",borderLeft:"1px solid var(--border)",overflowY:"auto",padding:16,flexShrink:0 }}>
          <div style={{ fontSize:10.5,fontWeight:700,color:"var(--text-muted)",textTransform:"uppercase",letterSpacing:0.8,marginBottom:12 }}>Live Summary</div>
          <div style={{ background:c.balance>=0?"var(--danger-bg)":"var(--success-bg)",border:`1px solid ${c.balance>=0?"#FECACA":"#BBF7D0"}`,borderRadius:8,padding:12,marginBottom:12 }}>
            <div style={{ fontSize:11,fontWeight:600,color:c.balance>=0?"var(--danger)":"var(--success)",marginBottom:4 }}>{c.balance>=0?"Tax Payable":"Refund Due"}</div>
            <div style={{ fontFamily:"'Crimson Pro',serif",fontSize:22,fontWeight:600,color:c.balance>=0?"var(--danger)":"var(--success)" }}>{INR(Math.abs(c.balance))}</div>
          </div>
          {[
            ["GTI", c.gti, false],
            ["Deductions", c.totalDed, true],
            ["Taxable Income", c.taxableNormal, false, true],
            ["Normal Tax", c.normalTax, false],
            ["CG Tax", c.cgTax, false],
            ["VDA Tax", c.vdaTax, false],
            ["Cess 4%", c.cess, false],
            ["Gross Tax", c.grossTax, false, true],
            ["TDS Paid", c.paid, true],
          ].map(([label, val, neg, bold], i) => (
            <div key={i} style={{ display:"flex",justifyContent:"space-between",alignItems:"center",padding:"5px 0",borderBottom:"1px solid var(--border)" }}>
              <span style={{ fontSize:11.5,color:"var(--text-muted)",fontWeight:bold?600:400 }}>{label}</span>
              <span style={{ fontSize:11.5,fontFamily:"'DM Mono',monospace",color:neg?"var(--success)":"var(--text-primary)",fontWeight:bold?600:400 }}>
                {neg?`(${INR(val)})`:INR(val)}
              </span>
            </div>
          ))}
          <div style={{ marginTop:12,paddingTop:12,borderTop:"1px solid var(--border)" }}>
            <div style={{ fontSize:10.5,fontWeight:700,color:"var(--text-muted)",textTransform:"uppercase",letterSpacing:0.8,marginBottom:8 }}>Income Heads</div>
            {sections.map(s=>(
              <div key={s.id} onClick={()=>setActiveSection(s.id)} style={{ display:"flex",justifyContent:"space-between",alignItems:"center",padding:"4px 0",cursor:"pointer" }}>
                <span style={{ fontSize:11.5,color:activeSection===s.id?"var(--gold)":"var(--text-muted)",fontWeight:activeSection===s.id?600:400 }}>{s.label}</span>
                <span style={{ fontSize:11,fontFamily:"'DM Mono',monospace",color:s.amount<0?"var(--danger)":s.amount>0?"var(--text-primary)":"var(--border-strong)" }}>
                  {s.amount===0?"–":s.amount<0?`(${fmt(Math.abs(s.amount))})`:fmt(s.amount)}
                </span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

// ─── Dashboard Views ──────────────────────────────────────────────────────────
const clients = [
  { name:"Ramesh Verma", pan:"DEFPV1122K", type:"Individual", mobile:"98765 43210", itr:"ITR-2", sync:"Apr 2", initials:"RV", filingStatus:"Filed", filingCls:"badge-success", ev:"Verified", evCls:"badge-success", watch:false },
  { name:"Priya Nair", pan:"QRSPN4521K", type:"Individual", mobile:"87654 32109", itr:"ITR-1", sync:"Apr 1", initials:"PN", filingStatus:"In Progress", filingCls:"badge-info", ev:"Pending", evCls:"badge-warning", watch:true },
  { name:"Suresh Kumar", pan:"ABCPK1234R", type:"Individual", mobile:"76543 21098", itr:"ITR-2", sync:"Mar 31", initials:"SK", filingStatus:"Mismatch", filingCls:"badge-danger", ev:"—", evCls:"badge-muted", watch:false },
  { name:"Anita Desai", pan:"DEFPA5678S", type:"Individual", mobile:"65432 10987", itr:"ITR-3", sync:"Mar 30", initials:"AD", filingStatus:"Doc Pending", filingCls:"badge-warning", ev:"—", evCls:"badge-muted", watch:false },
  { name:"Vikram Shah", pan:"GHIPV9012T", type:"HUF", mobile:"54321 09876", itr:"ITR-2", sync:"Mar 29", initials:"VS", filingStatus:"Ready to File", filingCls:"badge-gold", ev:"Verified", evCls:"badge-success", watch:true },
  { name:"Meena Agarwal", pan:"ABCMA3344R", type:"Individual", mobile:"43210 98765", itr:"ITR-1", sync:"Apr 2", initials:"MA", filingStatus:"Filed", filingCls:"badge-success", ev:"Verified", evCls:"badge-success", watch:false },
  { name:"Rohit Bansal", pan:"UVWRB7823L", type:"Individual", mobile:"32109 87654", itr:"ITR-3", sync:"Mar 28", initials:"RB", filingStatus:"In Progress", filingCls:"badge-info", ev:"Pending", evCls:"badge-warning", watch:true },
  { name:"Kavya Reddy", pan:"JKLKR3456U", type:"Individual", mobile:"21098 76543", itr:"ITR-1", sync:"Mar 30", initials:"KR", filingStatus:"Mismatch", filingCls:"badge-danger", ev:"—", evCls:"badge-muted", watch:false },
  { name:"Sunita Khanna", pan:"ABCSK5634N", type:"Individual", mobile:"10987 65432", itr:"ITR-2", sync:"Mar 27", initials:"SK2", filingStatus:"In Progress", filingCls:"badge-info", ev:"Pending", evCls:"badge-warning", watch:true },
  { name:"Deepak Joshi", pan:"XYZPJ0012M", type:"Firm", mobile:"09876 54321", itr:"ITR-4", sync:"Mar 26", initials:"DJ", filingStatus:"Doc Pending", filingCls:"badge-warning", ev:"—", evCls:"badge-muted", watch:false },
];

function StatCard({ label, value, meta, color, icon }) {
  const colors = { gold:"var(--gold)", blue:"var(--accent-blue)", teal:"var(--accent-teal)", rose:"#BE185D", success:"var(--success)" };
  return (
    <div style={{ background:"var(--bg-card)",border:"1px solid var(--border)",borderRadius:10,padding:"18px 20px",position:"relative",overflow:"hidden",borderTop:`3px solid ${colors[color]||colors.gold}` }}>
      <div style={{ fontSize:11,fontWeight:600,color:"var(--text-muted)",textTransform:"uppercase",letterSpacing:0.5 }}>{label}</div>
      <div style={{ fontFamily:"'Crimson Pro',serif",fontSize:30,fontWeight:600,color:"var(--text-primary)",margin:"6px 0 4px" }}>{value}</div>
      <div style={{ fontSize:12,color:"var(--text-muted)" }}>{meta}</div>
      <div style={{ position:"absolute",top:14,right:14,fontSize:22,opacity:0.12 }}>{icon}</div>
    </div>
  );
}

function DashboardView({ onOpenFiling }) {
  return (
    <div style={{ padding:"24px 28px",overflowY:"auto",height:"100%" }}>
      {/* Filing Progress */}
      <div style={{ background:"var(--bg-card)",border:"1px solid var(--border)",borderRadius:10,padding:"18px 22px",marginBottom:18 }}>
        <div style={{ display:"flex",alignItems:"center",justifyContent:"space-between",marginBottom:12 }}>
          <div><div style={{ fontSize:14,fontWeight:600 }}>AY 2025–26 Filing Season Progress</div><div style={{ fontSize:12,color:"var(--text-muted)",marginTop:2 }}>Last updated: Today, 10:42 AM</div></div>
          <span className="badge badge-gold badge-dot">Peak Season Active</span>
        </div>
        <div style={{ display:"flex",border:"1px solid var(--border)",borderRadius:8,overflow:"hidden",marginBottom:12 }}>
          {[["4,217","Total"],["2,891","Filed ✓"],["342","In Progress"],["518","Doc Pending"],["248","Ready"],["218","Mismatch"]].map(([n,l],i)=>(
            <div key={i} style={{ flex:1,padding:"12px 10px",textAlign:"center",borderRight:i<5?"1px solid var(--border)":"none",background:i===1?"var(--gold-pale)":"transparent" }}>
              <div style={{ fontFamily:"'Crimson Pro',serif",fontSize:22,fontWeight:600,color:i===1?"#92640A":"var(--text-primary)" }}>{n}</div>
              <div style={{ fontSize:11,color:i===1?"#92640A":"var(--text-muted)",marginTop:3,fontWeight:500 }}>{l}</div>
            </div>
          ))}
        </div>
        <div style={{ display:"flex",alignItems:"center",gap:10 }}>
          <div className="progress-bar" style={{ flex:1,height:10 }}><div className="progress-fill fill-gold" style={{ width:"68.6%" }} /></div>
          <span style={{ fontSize:13,fontWeight:600,color:"#92640A",whiteSpace:"nowrap" }}>68.6% Complete</span>
        </div>
      </div>

      {/* Stats */}
      <div style={{ display:"grid",gridTemplateColumns:"repeat(4,1fr)",gap:14,marginBottom:18 }}>
        <StatCard label="Revenue This Month" value="₹8.4L" meta="↑ 23% vs last month" color="gold" icon="💰" />
        <StatCard label="Pending E-Verify" value="143" meta="12 expiring in 7 days" color="blue" icon="⏰" />
        <StatCard label="Active Notices" value="31" meta="7 require response today" color="teal" icon="🔔" />
        <StatCard label="Outstanding Dues" value="₹3.2L" meta="₹84K overdue 90+ days" color="rose" icon="💳" />
      </div>

      {/* Bottom row */}
      <div style={{ display:"grid",gridTemplateColumns:"2fr 1fr",gap:16 }}>
        {/* Recent Activity */}
        <div style={{ background:"var(--bg-card)",border:"1px solid var(--border)",borderRadius:10,padding:"18px 22px" }}>
          <div style={{ fontSize:14,fontWeight:600,marginBottom:14 }}>Recent Activity</div>
          {[
            { dot:"var(--accent-teal)", text:<><strong>Ramesh Verma</strong> — ITR-2 filed successfully via ITD API. E-verify pending.</>, time:"2 minutes ago · AY 2025–26", badge:"Filed", cls:"badge-success" },
            { dot:"#EF4444", text:<><strong>Priya Nair</strong> — Notice u/s 143(1)(a) auto-detected on ITD portal.</>, time:"14 min ago · Response due Apr 18", badge:"Notice", cls:"badge-danger" },
            { dot:"var(--gold)", text:<><strong>Bulk Sync Job #847</strong> — Completed for 200 clients. 194 success, 6 CAPTCHA failed.</>, time:"1 hour ago · Batch 5 of 25", badge:"Partial", cls:"badge-warning" },
            { dot:"var(--accent-blue)", text:<><strong>Suresh Kumar</strong> — AIS mismatch of ₹34,500 flagged for reconciliation.</>, time:"2 hours ago · AY 2025–26", badge:"Mismatch", cls:"badge-warning" },
            { dot:"var(--accent-teal)", text:<><strong>Meena Agarwal</strong> — E-verification completed via Aadhaar OTP.</>, time:"3 hours ago", badge:"E-Verified", cls:"badge-success" },
          ].map((item, i) => (
            <div key={i} style={{ display:"flex",alignItems:"flex-start",gap:10,padding:"10px 0",borderBottom:i<4?"1px solid var(--border)":"none" }}>
              <div style={{ width:8,height:8,borderRadius:"50%",background:item.dot,marginTop:5,flexShrink:0 }} />
              <div style={{ flex:1 }}>
                <div style={{ fontSize:13,color:"var(--text-primary)",lineHeight:1.4 }}>{item.text}</div>
                <div style={{ fontSize:11.5,color:"var(--text-muted)",marginTop:2 }}>{item.time}</div>
              </div>
              <span className={`badge ${item.cls}`}>{item.badge}</span>
            </div>
          ))}
        </div>

        {/* Right column */}
        <div style={{ display:"flex",flexDirection:"column",gap:14 }}>
          <div style={{ background:"var(--bg-card)",border:"1px solid var(--border)",borderRadius:10,padding:"18px 22px" }}>
            <div style={{ fontSize:14,fontWeight:600,marginBottom:12 }}>ITR Type Filed</div>
            {[["ITR-1","var(--success)","1,382"],["ITR-2","var(--gold)","647"],["ITR-3","var(--accent-blue)","492"],["ITR-4","#EF4444","370"]].map(([type,color,count])=>(
              <div key={type} style={{ display:"flex",alignItems:"center",gap:8,marginBottom:6 }}>
                <div style={{ width:10,height:10,borderRadius:"50%",background:color,flexShrink:0 }} />
                <span style={{ fontSize:12.5,color:"var(--text-secondary)",flex:1 }}>{type}</span>
                <span style={{ fontSize:12.5,fontWeight:600 }}>{count}</span>
              </div>
            ))}
          </div>
          <div style={{ background:"var(--bg-card)",border:"1px solid var(--border)",borderRadius:10,padding:"18px 22px" }}>
            <div style={{ display:"flex",alignItems:"center",justifyContent:"space-between",marginBottom:12 }}>
              <div style={{ fontSize:14,fontWeight:600 }}>Today's Deadlines</div>
              <span className="badge badge-danger">7 urgent</span>
            </div>
            {[["Notice 143(1)(a) — Priya Nair","Respond by Today 5 PM","#EF4444"],["Advance Tax Q4 — 5 clients","Mar 15 · Payment deadline","#EF4444"],["E-Verify Expiry — 3 clients","120-day window closing","var(--gold)"]].map(([t,s,c],i)=>(
              <div key={i} style={{ display:"flex",gap:8,padding:"8px 0",borderBottom:i<2?"1px solid var(--border)":"none" }}>
                <div style={{ width:6,height:6,borderRadius:"50%",background:c,marginTop:6,flexShrink:0 }} />
                <div><div style={{ fontSize:12.5,fontWeight:500 }}>{t}</div><div style={{ fontSize:11.5,color:"var(--text-muted)" }}>{s}</div></div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

function ClientsView({ onOpenComputation }) {
  return (
    <div style={{ padding:"24px 28px",overflowY:"auto",height:"100%" }}>
      <div style={{ display:"flex",alignItems:"center",gap:10,marginBottom:14 }}>
        <div style={{ position:"relative",flex:1,maxWidth:320 }}>
          <span style={{ position:"absolute",left:10,top:"50%",transform:"translateY(-50%)",color:"var(--text-muted)",fontSize:13 }}>🔍</span>
          <input placeholder="Search by name, PAN, mobile…" style={{ width:"100%",padding:"8px 12px 8px 32px",background:"var(--bg-card)",border:"1px solid var(--border-strong)",borderRadius:8,fontSize:13.5,fontFamily:"'DM Sans',sans-serif",color:"var(--text-primary)",outline:"none" }} />
        </div>
        <select style={{ padding:"8px 12px",background:"var(--bg-card)",border:"1px solid var(--border-strong)",borderRadius:8,fontSize:13,fontFamily:"'DM Sans',sans-serif",outline:"none",cursor:"pointer" }}>
          <option>All Statuses</option><option>Filed</option><option>In Progress</option><option>Doc Pending</option><option>Mismatch</option>
        </select>
        <div style={{ marginLeft:"auto",display:"flex",gap:8 }}>
          <Btn variant="secondary" sm>Export Excel</Btn>
          <Btn variant="navy" sm>+ Add Client</Btn>
        </div>
      </div>
      <div style={{ background:"var(--bg-card)",border:"1px solid var(--border)",borderRadius:10,overflow:"hidden" }}>
        <div style={{ padding:"14px 18px",borderBottom:"1px solid var(--border)",background:"var(--bg)",display:"flex",alignItems:"center",justifyContent:"space-between" }}>
          <div><div style={{ fontSize:14,fontWeight:600 }}>All Clients</div><div style={{ fontSize:12,color:"var(--text-muted)" }}>Showing {clients.length} clients · AY 2025–26</div></div>
          <div style={{ display:"flex",gap:8 }}><span className="badge badge-success badge-dot">2,891 Filed</span><span className="badge badge-warning badge-dot">518 Pending</span></div>
        </div>
        <div style={{ overflowX:"auto" }}>
          <table>
            <thead><tr>
              <th>Client Name</th><th>PAN</th><th>Type</th><th>ITR Form</th><th>AIS Sync</th><th>Filing Status</th><th>E-Verify</th><th>Watch</th><th>Actions</th>
            </tr></thead>
            <tbody>
              {clients.map((c,i) => (
                <tr key={i} onClick={()=>onOpenComputation(c)} style={{ cursor:"pointer" }}>
                  <td>
                    <div style={{ display:"flex",alignItems:"center",gap:10 }}>
                      <div style={{ width:30,height:30,borderRadius:8,background:"var(--navy)",display:"flex",alignItems:"center",justifyContent:"center",fontFamily:"'Crimson Pro',serif",fontSize:12,color:"var(--gold-light)",fontWeight:600,flexShrink:0 }}>{c.initials}</div>
                      <span style={{ fontWeight:500 }}>{c.name}</span>
                    </div>
                  </td>
                  <td style={{ fontFamily:"'DM Mono',monospace",fontSize:12.5 }}>{c.pan}</td>
                  <td><span className="badge badge-muted">{c.type}</span></td>
                  <td><span className="badge badge-navy">{c.itr}</span></td>
                  <td style={{ fontSize:12.5,color:"var(--text-muted)" }}>{c.sync}</td>
                  <td><span className={`badge ${c.filingCls}`}>{c.filingStatus}</span></td>
                  <td><span className={`badge ${c.evCls}`}>{c.ev}</span></td>
                  <td>{c.watch?<span className="badge badge-danger" style={{ fontSize:10.5 }}>⚠ Watch</span>:<span style={{ color:"var(--text-muted)" }}>—</span>}</td>
                  <td>
                    <div style={{ display:"flex",gap:5 }} onClick={e=>e.stopPropagation()}>
                      <Btn sm variant="ghost">Sync</Btn>
                      <Btn sm variant="navy" onClick={()=>onOpenComputation(c)}>Open ITR</Btn>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

function FilingView({ onOpenComputation }) {
  return (
    <div style={{ padding:"24px 28px",overflowY:"auto",height:"100%" }}>
      <div style={{ background:"var(--bg-card)",border:"1px solid var(--border)",borderRadius:10,padding:"18px 22px",marginBottom:16 }}>
        <div style={{ fontSize:14,fontWeight:600,marginBottom:12 }}>Filing Workflow Pipeline</div>
        <div style={{ display:"flex",alignItems:"center",overflowX:"auto" }}>
          {[["✓","Docs Downloaded","done"],["✓","AIS Reconciled","done"],["3","Computation Review","current"],["4","Ready to File","pending"],["5","Filed via ITD API","pending"],["6","E-Verification","pending"],["7","Computation Sent","pending"]].map(([n,l,s],i,arr)=>(
            <div key={i} style={{ display:"flex",alignItems:"center",flexShrink:0 }}>
              <div style={{ display:"flex",flexDirection:"column",alignItems:"center" }}>
                <div style={{ width:42,height:42,borderRadius:"50%",display:"flex",alignItems:"center",justifyContent:"center",fontSize:15,fontWeight:600,border:`2px solid ${s==="done"?"var(--success)":s==="current"?"var(--gold)":"var(--border)"}`,background:s==="done"?"var(--success-bg)":s==="current"?"var(--gold-pale)":"var(--bg)",color:s==="done"?"var(--success)":s==="current"?"#92640A":"var(--text-muted)" }}>{n}</div>
                <div style={{ fontSize:11,marginTop:6,textAlign:"center",maxWidth:70,lineHeight:1.3,color:s==="done"?"var(--success)":s==="current"?"#92640A":"var(--text-muted)",fontWeight:s==="current"?600:400 }}>{l}</div>
              </div>
              {i<arr.length-1 && <div style={{ width:36,height:2,background:s==="done"?"var(--success)":"var(--border)",flexShrink:0,marginBottom:14 }} />}
            </div>
          ))}
        </div>
      </div>
      <div style={{ background:"var(--bg-card)",border:"1px solid var(--border)",borderRadius:10,overflow:"hidden" }}>
        <div style={{ padding:"14px 18px",borderBottom:"1px solid var(--border)",background:"var(--bg)",display:"flex",alignItems:"center",justifyContent:"space-between" }}>
          <div style={{ fontSize:14,fontWeight:600 }}>In-Progress Clients</div>
          <Btn variant="navy" sm>+ New ITR Filing</Btn>
        </div>
        <table>
          <thead><tr><th>Client</th><th>PAN</th><th>ITR Form</th><th>AIS Synced</th><th>Recon Status</th><th>Filing Stage</th><th>Tax Payable</th><th>Actions</th></tr></thead>
          <tbody>
            {clients.filter(c=>c.filingStatus!=="Filed").map((c,i) => (
              <tr key={i} style={{ cursor:"pointer" }} onClick={()=>onOpenComputation(c)}>
                <td><span style={{ fontWeight:500 }}>{c.name}</span></td>
                <td style={{ fontFamily:"'DM Mono',monospace",fontSize:12.5 }}>{c.pan}</td>
                <td><span className="badge badge-navy">{c.itr}</span></td>
                <td style={{ fontSize:12.5,color:"var(--text-muted)" }}>{c.sync}</td>
                <td><span className={`badge ${c.filingStatus==="Mismatch"?"badge-danger":c.filingStatus==="Doc Pending"?"badge-warning":"badge-info"}`}>{c.filingStatus==="Mismatch"?"Mismatch":c.filingStatus==="Doc Pending"?"Pending":"In Progress"}</span></td>
                <td><span className={`badge ${c.filingCls}`}>{c.filingStatus}</span></td>
                <td style={{ fontWeight:600,color:"var(--danger)",fontSize:12.5 }}>—</td>
                <td onClick={e=>e.stopPropagation()}>
                  <Btn sm variant="navy" onClick={()=>onOpenComputation(c)}>Open ITR ↗</Btn>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function PlaceholderView({ title, sub }) {
  return (
    <div style={{ display:"flex",alignItems:"center",justifyContent:"center",height:"100%",flexDirection:"column",gap:12,color:"var(--text-muted)" }}>
      <div style={{ fontSize:40,opacity:0.3 }}>🚧</div>
      <div style={{ fontSize:16,fontWeight:600,color:"var(--text-secondary)" }}>{title}</div>
      <div style={{ fontSize:13 }}>{sub}</div>
    </div>
  );
}

// ─── Sidebar Nav ──────────────────────────────────────────────────────────────
const navItems = [
  { section:"Overview", items:[{ id:"dashboard", label:"Dashboard", icon:<svg viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.6" width={17} height={17}><rect x="2" y="2" width="7" height="7" rx="1.5"/><rect x="11" y="2" width="7" height="7" rx="1.5"/><rect x="2" y="11" width="7" height="7" rx="1.5"/><rect x="11" y="11" width="7" height="7" rx="1.5"/></svg> }] },
  { section:"Clients & Filing", items:[
    { id:"clients", label:"Client Master", badge:"5,000", icon:<svg viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.6" width={17} height={17}><circle cx="8" cy="6" r="3"/><path d="M2 17c0-3.3 2.7-6 6-6"/><circle cx="15" cy="12" r="3"/><path d="M11 17.9c.3-3.1 2.5-5.5 5.4-5.9"/></svg> },
    { id:"filing", label:"ITR Filing", badge:"342", badgeCls:"blue", icon:<svg viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.6" width={17} height={17}><path d="M14 2H6a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2V4a2 2 0 0 0-2-2z"/><path d="M8 10h4M8 13h4M8 7h2"/></svg> },
    { id:"reconciliation", label:"Reconciliation", badge:"18", badgeCls:"red", icon:<svg viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.6" width={17} height={17}><path d="M4 7h12M4 10h12M4 13h8"/><circle cx="16" cy="13" r="2"/></svg> },
  ]},
  { section:"Compliance", items:[
    { id:"notices", label:"Notices", badge:"7", badgeCls:"red", icon:<svg viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.6" width={17} height={17}><path d="M10 2a6 6 0 0 1 6 6v2l1.5 3h-15L4 10V8a6 6 0 0 1 6-6z"/><path d="M8 15a2 2 0 0 0 4 0"/></svg> },
    { id:"calendar", label:"Calendar", icon:<svg viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.6" width={17} height={17}><rect x="2" y="4" width="16" height="14" rx="1.5"/><path d="M6 2v4M14 2v4M2 9h16"/></svg> },
    { id:"tasks", label:"Tasks", badge:"24", icon:<svg viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.6" width={17} height={17}><path d="M4 6h12M4 10h8M4 14h6"/><circle cx="16" cy="14" r="2"/><path d="M14.6 12.6l1 1 2-2"/></svg> },
  ]},
  { section:"Finance", items:[
    { id:"billing", label:"Billing & Fees", icon:<svg viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.6" width={17} height={17}><rect x="2" y="5" width="16" height="12" rx="1.5"/><path d="M2 9h16"/><path d="M6 13h2M10 13h4"/></svg> },
    { id:"sync", label:"Portal Sync", icon:<svg viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.6" width={17} height={17}><path d="M16.5 10A6.5 6.5 0 0 1 5.1 14.9"/><path d="M3.5 10A6.5 6.5 0 0 1 14.9 5.1"/><path d="M3.5 14V10h4"/><path d="M16.5 6v4h-4"/></svg> },
  ]},
];

// ─── Main App ─────────────────────────────────────────────────────────────────
export default function IncomeTaxERP() {
  const [activeView, setActiveView] = useState("dashboard");
  const [computationClient, setComputationClient] = useState(null);

  const pageMeta = {
    dashboard: { title:"Dashboard", sub:"AY 2025–26 & 2026–27 · 5,000 Active Clients" },
    clients: { title:"Client Master", sub:"5,000 clients · AY 2025–26" },
    filing: { title:"ITR Filing", sub:"342 in progress · AY 2025–26" },
    reconciliation: { title:"Reconciliation", sub:"218 mismatches detected" },
    notices: { title:"Notice Management", sub:"31 active notices · 7 due this week" },
    calendar: { title:"Compliance Calendar", sub:"April 2026 · Key deadlines" },
    tasks: { title:"Tasks & Work Queue", sub:"24 open tasks" },
    billing: { title:"Billing & Fees", sub:"₹12.6L billed · ₹3.2L outstanding" },
    sync: { title:"ITD Portal Sync", sub:"Playwright automation · AIS / TIS / 26AS" },
  };

  const openComputation = (client) => {
    setComputationClient(client);
    setActiveView("computation");
  };

  const meta = pageMeta[activeView] || { title:"", sub:"" };

  const viewContent = computationClient && activeView === "computation"
    ? <ITRComputationView client={computationClient} onBack={() => { setActiveView("filing"); setComputationClient(null); }} />
    : activeView === "dashboard" ? <DashboardView onOpenFiling={()=>setActiveView("filing")} />
    : activeView === "clients" ? <ClientsView onOpenComputation={openComputation} />
    : activeView === "filing" ? <FilingView onOpenComputation={openComputation} />
    : <PlaceholderView title={meta.title} sub="Feature view — click Open ITR on a filing client to open computation." />;

  return (
    <>
      <style>{css}</style>
      <div style={{ display:"flex",height:"100vh",overflow:"hidden",fontFamily:"'DM Sans',sans-serif",background:"var(--bg)" }}>

        {/* ── Sidebar ── */}
        <aside style={{ width:"var(--sidebar-w)",background:"var(--navy)",display:"flex",flexDirection:"column",flexShrink:0,overflow:"hidden",zIndex:100 }}>
          {/* Brand */}
          <div style={{ padding:"20px 22px 16px",borderBottom:"1px solid rgba(255,255,255,0.08)",flexShrink:0 }}>
            <div style={{ display:"flex",alignItems:"center",gap:10 }}>
              <div style={{ width:36,height:36,background:"var(--gold)",borderRadius:8,display:"flex",alignItems:"center",justifyContent:"center",fontFamily:"'Crimson Pro',serif",fontSize:18,fontWeight:600,color:"var(--navy)",flexShrink:0 }}>IT</div>
              <div>
                <div style={{ fontFamily:"'Crimson Pro',serif",fontSize:16,fontWeight:600,color:"white",letterSpacing:0.2,lineHeight:1.2 }}>IncomeTax ERP</div>
                <div style={{ fontSize:10.5,color:"var(--gold-light)",letterSpacing:0.4,textTransform:"uppercase",fontWeight:500 }}>Advocate Practice</div>
              </div>
            </div>
          </div>

          {/* Search */}
          <div style={{ padding:"12px 14px 8px",flexShrink:0 }}>
            <input placeholder="Search clients, PAN…" style={{ width:"100%",background:"rgba(255,255,255,0.07)",border:"1px solid rgba(255,255,255,0.10)",borderRadius:7,padding:"7px 10px 7px 30px",color:"white",fontSize:13,fontFamily:"'DM Sans',sans-serif",outline:"none",backgroundImage:"url(\"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='13' height='13' viewBox='0 0 24 24' fill='none' stroke='%238A9BB0' stroke-width='2'%3E%3Ccircle cx='11' cy='11' r='8'/%3E%3Cpath d='m21 21-4.35-4.35'/%3E%3C/svg%3E\")",backgroundRepeat:"no-repeat",backgroundPosition:"10px center" }} />
          </div>

          {/* Nav */}
          <nav style={{ flex:1,overflowY:"auto",padding:"4px 10px 10px" }}>
            {navItems.map(section => (
              <div key={section.section}>
                <div style={{ fontSize:10,fontWeight:600,letterSpacing:1,textTransform:"uppercase",color:"rgba(255,255,255,0.28)",padding:"12px 12px 6px" }}>{section.section}</div>
                {section.items.map(item => {
                  const isActive = activeView === item.id;
                  return (
                    <div key={item.id} onClick={()=>setActiveView(item.id)} style={{ display:"flex",alignItems:"center",gap:10,padding:"9px 12px",borderRadius:8,cursor:"pointer",marginBottom:1,transition:"background 0.15s",background:isActive?"rgba(201,148,58,0.18)":"transparent",color:isActive?"var(--gold-light)":"rgba(255,255,255,0.65)",fontSize:13.5,fontWeight:isActive?500:400,position:"relative",borderLeft:isActive?"3px solid var(--gold)":"3px solid transparent" }}>
                      <span style={{ opacity:isActive?1:0.8 }}>{item.icon}</span>
                      <span style={{ flex:1 }}>{item.label}</span>
                      {item.badge && <span style={{ background:item.badgeCls==="red"?"#EF4444":item.badgeCls==="blue"?"#3B82F6":"var(--gold)",color:item.badgeCls?"white":"var(--navy)",fontSize:10,fontWeight:700,padding:"1px 6px",borderRadius:10,minWidth:18,textAlign:"center" }}>{item.badge}</span>}
                    </div>
                  );
                })}
              </div>
            ))}
          </nav>

          {/* Footer user */}
          <div style={{ padding:"14px 16px",borderTop:"1px solid rgba(255,255,255,0.08)",flexShrink:0,display:"flex",alignItems:"center",gap:10 }}>
            <div style={{ width:34,height:34,borderRadius:"50%",background:"var(--gold)",display:"flex",alignItems:"center",justifyContent:"center",fontSize:13,fontWeight:700,color:"var(--navy)",flexShrink:0 }}>AP</div>
            <div style={{ flex:1,minWidth:0 }}>
              <div style={{ fontSize:13,fontWeight:500,color:"white",whiteSpace:"nowrap",overflow:"hidden",textOverflow:"ellipsis" }}>Adv. Prakash Mehta</div>
              <div style={{ fontSize:11,color:"rgba(255,255,255,0.4)" }}>Senior Advocate · Admin</div>
            </div>
          </div>
        </aside>

        {/* ── Main Area ── */}
        <div style={{ flex:1,display:"flex",flexDirection:"column",overflow:"hidden" }}>
          {/* Topbar (hide when in computation view) */}
          {activeView !== "computation" && (
            <header style={{ height:62,background:"var(--bg-card)",borderBottom:"1px solid var(--border)",display:"flex",alignItems:"center",padding:"0 28px",gap:14,flexShrink:0,zIndex:50 }}>
              <div style={{ flex:1 }}>
                <div style={{ fontFamily:"'Crimson Pro',serif",fontSize:20,fontWeight:600,color:"var(--text-primary)",lineHeight:1.2 }}>{meta.title}</div>
                <div style={{ fontSize:12,color:"var(--text-muted)",marginTop:1 }}>{meta.sub}</div>
              </div>
              <div style={{ display:"flex",alignItems:"center",gap:10 }}>
                <select style={{ background:"var(--bg)",border:"1px solid var(--border-strong)",borderRadius:7,padding:"7px 12px",fontSize:13,fontFamily:"'DM Sans',sans-serif",color:"var(--text-primary)",fontWeight:500,cursor:"pointer",outline:"none" }}>
                  <option>AY 2025–26</option><option>AY 2026–27</option><option>Both AYs</option>
                </select>
                <div style={{ width:36,height:36,background:"var(--bg)",border:"1px solid var(--border)",borderRadius:8,display:"flex",alignItems:"center",justifyContent:"center",cursor:"pointer",color:"var(--text-secondary)",position:"relative" }}>
                  🔔
                  <span style={{ position:"absolute",top:6,right:6,width:7,height:7,background:"#EF4444",borderRadius:"50%",border:"1.5px solid white" }} />
                </div>
                <Btn variant="primary" onClick={()=>setActiveView("sync")}>
                  🔄 Run Bulk Sync
                </Btn>
              </div>
            </header>
          )}

          {/* Content */}
          <div style={{ flex:1,overflow:"hidden",display:"flex",flexDirection:"column" }}>
            {viewContent}
          </div>
        </div>
      </div>
    </>
  );
}
