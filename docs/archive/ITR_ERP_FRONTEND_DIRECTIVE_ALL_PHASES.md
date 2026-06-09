# ITR ERP — COMPLETE FRONTEND BUILD DIRECTIVE
## React + TypeScript + Tailwind CSS — 4-Phase Production Build
## Pixel-Perfect Replica with Full Backend Integration

**Date:** April 11, 2026
**Prerequisite:** Backend Phase 1–9 complete, Spring Boot running on `http://localhost:8080`
**Target:** Production-ready frontend — exact visual replica of both UI designs, fully wired to every backend endpoint

---

## DESIGN SYSTEM CONTRACT — READ BEFORE WRITING ANY CODE

The design system is derived **exactly** from the source HTML. Do not deviate from any of these values.

### CSS Custom Properties → Tailwind CSS Variables

```typescript
// tailwind.config.ts — EXACT values from source HTML
export default {
  content: ['./src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        navy:          '#0B1929',
        'navy-mid':    '#112236',
        'navy-light':  '#1A3150',
        gold:          '#C9943A',
        'gold-light':  '#E8B86D',
        'gold-pale':   '#FDF3E0',
        'accent-blue': '#1D6FA4',
        'accent-teal': '#0F766E',
        'accent-rose': '#BE185D',
        success:       '#15803D',
        'success-bg':  '#DCFCE7',
        warning:       '#B45309',
        'warning-bg':  '#FEF3C7',
        danger:        '#B91C1C',
        'danger-bg':   '#FEE2E2',
        info:          '#1E40AF',
        'info-bg':     '#DBEAFE',
        bg:            '#F4F6F9',
        'bg-card':     '#FFFFFF',
        border:        '#E1E7EF',
        'border-strong':'#C8D3E0',
        'text-primary': '#0F1E2D',
        'text-secondary':'#4A5D72',
        'text-muted':  '#8A9BB0',
      },
      fontFamily: {
        serif:  ['"Crimson Pro"', 'serif'],
        sans:   ['"DM Sans"', 'sans-serif'],
        mono:   ['"DM Mono"', 'monospace'],
      },
      borderRadius: {
        DEFAULT: '10px',
        sm: '6px',
      },
      width: { sidebar: '258px' },
      height: { topbar: '62px' },
    },
  },
  plugins: [],
}
```

### Google Fonts — MANDATORY (add to `index.html`)

```html
<link rel="preconnect" href="https://fonts.googleapis.com">
<link href="https://fonts.googleapis.com/css2?family=Crimson+Pro:wght@400;500;600&family=DM+Sans:wght@300;400;500;600&family=DM+Mono:wght@400;500&display=swap" rel="stylesheet">
```

### Design Tokens (use these class names throughout — do not inline styles)

```css
/* src/index.css — global styles that Tailwind cannot replicate */
:root {
  --sidebar-w: 258px;
  --header-h: 62px;
}

* { scrollbar-width: thin; scrollbar-color: #C8D3E0 transparent; }
*::-webkit-scrollbar { width: 5px; height: 5px; }
*::-webkit-scrollbar-thumb { background: #C8D3E0; border-radius: 3px; }

.font-serif { font-family: 'Crimson Pro', serif; }
.font-mono  { font-family: 'DM Mono', monospace; }

/* Sidebar scrollbar — lighter */
.sidebar-nav { scrollbar-color: rgba(255,255,255,0.1) transparent; }
.sidebar-nav::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.1); }

/* Stat card accent bar */
.stat-card-accent::before {
  content: '';
  position: absolute;
  top: 0; left: 0;
  width: 100%; height: 3px;
}

/* Animated pulse for running jobs */
@keyframes pulse-dot {
  0%, 100% { opacity: 1; transform: scale(1); }
  50%       { opacity: 0.6; transform: scale(1.3); }
}
.animate-pulse-dot { animation: pulse-dot 1.5s infinite; }

/* Nav active left bar */
.nav-active::before {
  content: '';
  position: absolute;
  left: 0; top: 50%;
  transform: translateY(-50%);
  width: 3px; height: 20px;
  background: #C9943A;
  border-radius: 0 3px 3px 0;
}
```

---

## PROJECT STRUCTURE

```
src/
├── api/
│   ├── axios.ts                  # Axios instance + interceptors
│   ├── clients.ts                # Client CRUD endpoints
│   ├── filing.ts                 # ITR filing endpoints
│   ├── computation.ts            # Tax computation endpoints
│   ├── reconciliation.ts         # AIS/26AS reconciliation endpoints
│   ├── sync.ts                   # ITD portal sync endpoints
│   ├── jobs.ts                   # Spring Batch job endpoints
│   ├── notices.ts                # Notice management endpoints
│   ├── billing.ts                # Billing & invoice endpoints
│   ├── accounting.ts             # Firm accounting endpoints
│   ├── communication.ts          # WhatsApp/email endpoints
│   ├── evc.ts                    # E-verification endpoints
│   └── types.ts                  # All TypeScript interfaces
├── components/
│   ├── shell/
│   │   ├── Sidebar.tsx
│   │   ├── Topbar.tsx
│   │   └── Layout.tsx
│   ├── ui/
│   │   ├── Badge.tsx
│   │   ├── Button.tsx
│   │   ├── Card.tsx
│   │   ├── StatCard.tsx
│   │   ├── DataTable.tsx
│   │   ├── FilterBar.tsx
│   │   ├── Tabs.tsx
│   │   ├── Modal.tsx
│   │   ├── SlidePanel.tsx
│   │   ├── ProgressBar.tsx
│   │   ├── FormField.tsx
│   │   ├── Pagination.tsx
│   │   ├── Avatar.tsx
│   │   ├── ActivityItem.tsx
│   │   └── EmptyState.tsx
│   └── domain/
│       ├── ClientDetailPanel.tsx
│       ├── FilingPipeline.tsx
│       ├── ReconMeter.tsx
│       ├── JobRow.tsx
│       ├── NoticeCard.tsx
│       ├── TaskCard.tsx
│       └── InvoiceRow.tsx
├── pages/
│   ├── Dashboard.tsx
│   ├── ClientMaster.tsx
│   ├── ITRFiling.tsx
│   ├── Computation.tsx           # Full computation window
│   ├── Reconciliation.tsx
│   ├── Sync.tsx
│   ├── Jobs.tsx
│   ├── Notices.tsx
│   ├── Calendar.tsx
│   ├── Tasks.tsx
│   ├── Billing.tsx
│   ├── Accounting.tsx
│   ├── Reports.tsx
│   └── Communication.tsx
├── store/
│   ├── useAppStore.ts            # Zustand: global AY, activeView, user
│   └── useFilingStore.ts         # Zustand: active filing session
├── hooks/
│   ├── useClients.ts
│   ├── useFiling.ts
│   ├── useComputation.ts
│   └── useJobs.ts
└── main.tsx
```

---

# PHASE 1 — PROJECT SETUP + DESIGN SYSTEM + SHELL

**Deliverable:** Running app with sidebar, topbar, layout shell, and all primitive UI components
**Gate:** `npm run build` produces zero TypeScript errors. Sidebar navigation switches views. All design tokens match the source pixel-for-pixel.

---

## 1.1 Project Initialization

```bash
npm create vite@latest itr-erp-frontend -- --template react-ts
cd itr-erp-frontend
npm install tailwindcss postcss autoprefixer @tailwindcss/forms
npm install axios @tanstack/react-query zustand
npm install react-router-dom
npm install lucide-react
npm install recharts
npm install react-hot-toast
npm install date-fns
npx tailwindcss init -p
```

## 1.2 `tailwind.config.ts` — COPY EXACTLY FROM DESIGN SYSTEM CONTRACT ABOVE

## 1.3 `src/index.css` — COPY EXACTLY FROM DESIGN SYSTEM CONTRACT ABOVE

## 1.4 `src/api/axios.ts` — Axios instance

```typescript
import axios from 'axios';
import toast from 'react-hot-toast';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
});

// Request interceptor — attach JWT from localStorage
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('itr_erp_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Response interceptor — global error handling
api.interceptors.response.use(
  (res) => res,
  (error) => {
    const status = error.response?.status;
    const message = error.response?.data?.message || 'Something went wrong';
    if (status === 401) {
      localStorage.removeItem('itr_erp_token');
      window.location.href = '/login';
    } else if (status === 429) {
      toast.error('Too many requests — please wait');
    } else if (status >= 500) {
      toast.error('Server error: ' + message);
    }
    return Promise.reject(error);
  }
);

export default api;
```

## 1.5 `src/api/types.ts` — Complete TypeScript interfaces

```typescript
// ─── Enums ───────────────────────────────────────────────────────────────
export type AssessmentYear = '2025-26' | '2026-27';
export type ITRForm = 'ITR-1' | 'ITR-2' | 'ITR-3' | 'ITR-4';
export type TaxRegime = 'NEW' | 'OLD';
export type ClientType = 'Individual' | 'HUF' | 'Firm' | 'Company' | 'AOP' | 'BOI';
export type FilingStatus =
  | 'Doc Pending' | 'Reconciliation' | 'Mismatch Review'
  | 'Ready to File' | 'Filed' | 'E-Verify Pending';
export type EVCMode = 'AADHAAR_OTP' | 'NET_BANKING' | 'BANK_ACCOUNT' | 'DSC';
export type ReconStatus = 'Clean' | 'Mismatch' | 'Pending' | 'In Progress';
export type NoticeSection =
  | '143(1)(a)' | '143(2)' | '144' | '148' | '156' | '154' | '139(9)';
export type NoticePriority = 'Urgent' | 'High' | 'Normal';

// ─── Client ──────────────────────────────────────────────────────────────
export interface Client {
  id: number;
  name: string;
  pan: string;
  aadhaarMasked: string;
  type: ClientType;
  mobile: string;
  email: string;
  dateOfBirth: string;        // DD/MM/YYYY
  residencyStatus: 'ROR' | 'RNOR' | 'NR';
  itrForm: ITRForm;
  assessmentYear: AssessmentYear;
  filingStatus: FilingStatus;
  reconStatus: ReconStatus;
  evStatus: 'Verified' | 'Pending' | 'Not Filed';
  lastSynced: string;
  watchList: boolean;
  initials: string;
  communicationChannel: 'WhatsApp' | 'Email' | 'SMS';
}

// ─── Tax Computation ──────────────────────────────────────────────────────
export interface ScheduleSalary {
  basicSalary: number;
  dearnessAllowance: number;
  houseRentAllowance: number;
  leaveTravelAllowance: number;
  leaveEncashment: number;
  gratuity: number;
  bonus: number;
  commission: number;
  arrearsOfSalary: number;
  otherAllowances: number;
  total17_1: number;
  totalPerquisites17_2: number;
  totalProfitsInLieu17_3: number;
  grossSalary: number;
  hraExemption10_13A: number;
  ltaExemption10_5: number;
  gratuityExemption10_10: number;
  leaveEncashmentExemption10_10AA: number;
  totalExemptionsUnder10: number;
  standardDeduction: number;
  professionalTax: number;
  entertainmentAllowance: number;
  taxableIncomeSalary: number;
}

export interface ScheduleHP {
  propertyType: 'Self Occupied' | 'Let Out' | 'Deemed Let Out';
  grossRentReceived: number;
  municipalTaxPaid: number;
  netAnnualValue: number;
  standardDeduction30Pct: number;
  interestOnHousingLoan: number;
  priorPeriodInterest: number;
  incomeFromHP: number;
  isPreConstruction: boolean;
}

export interface ScheduleCG {
  // STCG
  stcg111A: number;           // 15% rate
  stcgOtherAssets: number;    // Slab rate
  // LTCG
  ltcg112A: number;           // 10% on gains > 1.25L (post Jul 23)
  ltcg112APreJul23: number;   // 10% grandfathered
  ltcgOther112: number;       // 20% with indexation
  // Derived
  exemptionUnder10_38: number;
  costOfAcquisition112A: number;
  fairMarketValue112A: number;
  totalCG: number;
}

export interface ScheduleVDA {
  totalVDACost: number;
  totalVDAConsideration: number;
  vdaProfit: number;
  vdaLoss: number;          // Always floored to 0
  tds194S: number;
}

export interface ScheduleOS {
  savingsInterest: number;
  fdInterest: number;
  dividendIncome: number;
  familyPension: number;
  winningsLottery: number;   // 30%
  otherIncome: number;
  totalOS: number;
}

export interface ScheduleBP {
  netProfitAsPerPL: number;
  addbackPersonalExpenses: number;
  addbackCapitalExpenses: number;
  addbackExcessDepreciation: number;
  addback40A3CashPayments: number;
  addbackEmployeePFESI: number;
  addback14ADisallowance: number;
  addback43BDisallowances: number;
  msme43BhDisallowance: number;
  partnerRemuneration40BDisallowance: number;
  lessOtherHeadIncome: number;
  lessITActDepreciation: number;
  lessAdditionalDepreciation: number;
  lessOtherDeductions: number;
  businessIncome: number;
  foTurnover: number;
  foProfitLoss: number;
  isAuditRequired: boolean;
}

export interface ScheduleDeductions {
  // 80C cluster (capped at 1.5L)
  sec80C: number;
  sec80CCC: number;
  sec80CCD1: number;
  totalCSection: number;       // auto-capped at 1.5L
  sec80CCD1B: number;          // NPS — additional 50K
  sec80CCD2: number;           // Employer NPS — no cap
  // Others
  sec80D: number;              // Medical insurance
  sec80DD: number;             // Disabled dependent
  sec80DDB: number;            // Medical treatment
  sec80E: number;              // Education loan interest
  sec80EEA: number;            // Housing loan interest (affordable)
  sec80G: number;              // Donations
  sec80GGA: number;            // Scientific research donations
  sec80GGC: number;            // Political party donations
  sec80TTA: number;            // Savings interest (max 10K)
  sec80TTB: number;            // Senior citizen interest (max 50K)
  sec80U: number;              // Self disability
  totalDeductions: number;
}

export interface LossSetOff {
  hpLossBroughtForward: number;
  hpLossCurrentYear: number;
  hpLossSetOff: number;
  businessLossBroughtForward: number;
  speculativeLossBroughtForward: number;
  stcgLossBroughtForward: number;
  ltcgLossBroughtForward: number;
  totalLossSetOff: number;
}

export interface TaxComputation {
  totalIncome: number;
  taxOnTotalIncome: number;
  rebate87A: number;
  taxAfterRebate: number;
  surcharge: number;
  healthEducationCess: number;
  grossTaxLiability: number;
  reliefUnder89: number;
  amtLiability: number;        // AMT u/s 115JC
  totalTaxPayable: number;
  // TDS / advance tax
  tdsSalary: number;
  tdsOtherIncome: number;
  tcs: number;
  advanceTax: number;
  selfAssessmentTax: number;
  totalTaxPaid: number;
  // Result
  taxPayable: number;
  refundDue: number;
}

export interface RegimeComparison {
  oldRegimeTax: number;
  newRegimeTax: number;
  recommendedRegime: TaxRegime;
  saving: number;
  confidenceScore: number;
  primaryReason: string;
  tippingPointDeductions: number;
  warnings: string[];
}

export interface ITRFormData {
  // Personal
  pan: string;
  assessmentYear: AssessmentYear;
  taxRegime: TaxRegime;
  itrType: ITRForm;
  returnFilingSection: string;  // '139(1)', '139(4)', '139(5)'
  originalOrRevised: 'O' | 'R';
  originalReturnAcknowledgementNumber?: string;
  originalReturnFilingDate?: string;
  intermediaryCity: string;
  // Income schedules
  scheduleSalary?: ScheduleSalary;
  scheduleHP?: ScheduleHP[];
  scheduleCG?: ScheduleCG;
  scheduleVDA?: ScheduleVDA;
  scheduleOS?: ScheduleOS;
  scheduleBP?: ScheduleBP;       // ITR-3
  // Presumptive (ITR-4)
  presumptive44AD?: { grossTurnover: number; digitalTurnover: number; cashTurnover: number; presumptiveIncome: number; };
  presumptive44ADA?: { grossReceipts: number; presumptiveIncome: number; };
  presumptive44AE?: { numberOfVehicles: number; totalIncome: number; };
  // Deductions & Loss
  deductions?: ScheduleDeductions;
  lossSetOff?: LossSetOff;
  // Computation
  taxComputation?: TaxComputation;
  regimeComparison?: RegimeComparison;
  // Foreign
  scheduleFA?: ScheduleFA;
  // TDS
  tdsSalaryEntries?: TDSEntry[];
  tdsOtherEntries?: TDSEntry[];
  advanceTaxEntries?: AdvanceTaxEntry[];
  // AMT (ITR-3)
  amtResult?: AMTResult;
}

export interface TDSEntry {
  deductorName: string;
  deductorTan: string;
  amountPaid: number;
  tdsAmount: number;
  status: 'F' | 'U' | 'O';
}

export interface AdvanceTaxEntry {
  type: 'Advance Tax' | 'Self-Assessment Tax';
  bsrCode: string;
  challanSerialNo: string;
  dateOfDeposit: string;
  amount: number;
}

export interface AMTResult {
  amtApplicable: boolean;
  adjustedTotalIncome: number;
  amtBeforeSurcharge: number;
  totalAMT: number;
  regularTaxLiability: number;
  amtPayable: number;
  excessAMTOverRegular: number;
  amtCreditAvailable: number;
  scheduleAMTRequired: boolean;
}

export interface ScheduleFA {
  foreignBankAccounts: ForeignBankAccount[];
  foreignImmovableProperty: ForeignImmovableProperty[];
  incomeFromForeignSources: ForeignSourceIncome[];
}

export interface ForeignBankAccount {
  countryName: string;
  bankName: string;
  swiftCode: string;
  peakValueInINR: number;
  interestAccrued: number;
}

export interface ForeignImmovableProperty {
  countryName: string;
  addressOutsideIndia: string;
  ownershipStatus: string;
}

export interface ForeignSourceIncome {
  incomeType: string;
  countryName: string;
  amountInINR: number;
  taxPaidOutside: number;
}

// ─── Filing Result ────────────────────────────────────────────────────────
export interface FilingResult {
  successful: boolean;
  pan: string;
  assessmentYear: string;
  itrType: string;
  acknowledgementNumber: string;
  jsonDigest: string;
  totalIncome: number;
  netTaxPayable: number;
  refundClaimed: number;
  evcStatus: string;
  errors: string[];
  warnings: string[];
}

// ─── Validation Result ───────────────────────────────────────────────────
export interface ValidationResult {
  valid: boolean;
  catAErrors: CBDTValidationError[];
  catBWarnings: CBDTValidationError[];
  catDWarnings: CBDTValidationError[];
}

export interface CBDTValidationError {
  ruleId: string;
  category: 'A' | 'B' | 'D';
  field: string;
  message: string;
}

// ─── TDS Mismatch ────────────────────────────────────────────────────────
export interface TDSMismatchReport {
  pan: string;
  assessmentYear: string;
  mismatches: TDSMismatch[];
  summary: string;
}

export interface TDSMismatch {
  type: string;
  tan?: string;
  description: string;
  severity: 'HIGH' | 'MEDIUM' | 'LOW';
  suggestions: string[];
}

// ─── AIS Reconciliation ──────────────────────────────────────────────────
export interface AISReconciliationResult {
  hasMismatches: boolean;
  discrepancies: string[];
  totalTds26AS: number;
  totalTdsITR: number;
  fetchFailed: boolean;
  fetchError?: string;
}

// ─── Notice ──────────────────────────────────────────────────────────────
export interface Notice {
  id: number;
  clientId: number;
  clientName: string;
  pan: string;
  section: NoticeSection;
  assessmentYear: string;
  receivedDate: string;
  dueDate: string;
  priority: NoticePriority;
  amount?: number;
  status: 'Open' | 'Under Review' | 'Responded' | 'Closed';
  notes?: string;
}

// ─── Background Job ───────────────────────────────────────────────────────
export interface BatchJob {
  jobId: number;
  jobName: string;
  startedAt: string;
  scope: string;
  assessmentYear: string;
  total: number;
  success: number;
  failed: number;
  duration?: string;
  status: 'RUNNING' | 'COMPLETED' | 'FAILED' | 'QUEUED' | 'PARTIAL';
  progressPct: number;
}

// ─── Invoice ─────────────────────────────────────────────────────────────
export interface Invoice {
  id: string;
  clientId: number;
  clientName: string;
  pan: string;
  service: string;
  amount: number;
  status: 'Paid' | 'Pending' | 'Overdue' | 'Partial';
  createdAt: string;
}

// ─── EVC ─────────────────────────────────────────────────────────────────
export interface EVCResult {
  status: 'OTP_SENT' | 'VERIFIED' | 'FAILED';
  transactionId?: string;
  evcCode?: string;
  message: string;
}

// ─── Pagination ───────────────────────────────────────────────────────────
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}
```

## 1.6 `src/store/useAppStore.ts` — Zustand global store

```typescript
import { create } from 'zustand';
import { AssessmentYear } from '../api/types';

type View =
  | 'dashboard' | 'clients' | 'filing' | 'reconciliation'
  | 'sync' | 'jobs' | 'notices' | 'calendar' | 'tasks'
  | 'billing' | 'accounting' | 'reports' | 'communication';

interface AppStore {
  activeView: View;
  setActiveView: (v: View) => void;
  activeAY: AssessmentYear;
  setActiveAY: (ay: AssessmentYear) => void;
  globalSearch: string;
  setGlobalSearch: (q: string) => void;
  // Client detail panel
  selectedClientId: number | null;
  setSelectedClientId: (id: number | null) => void;
  // Computation modal
  computationClientId: number | null;
  openComputation: (id: number) => void;
  closeComputation: () => void;
}

export const useAppStore = create<AppStore>((set) => ({
  activeView: 'dashboard',
  setActiveView: (v) => set({ activeView: v }),
  activeAY: '2025-26',
  setActiveAY: (ay) => set({ activeAY: ay }),
  globalSearch: '',
  setGlobalSearch: (q) => set({ globalSearch: q }),
  selectedClientId: null,
  setSelectedClientId: (id) => set({ selectedClientId: id }),
  computationClientId: null,
  openComputation: (id) => set({ computationClientId: id }),
  closeComputation: () => set({ computationClientId: null }),
}));
```

## 1.7 Shell Components

### `src/components/shell/Layout.tsx`

```tsx
import React from 'react';
import Sidebar from './Sidebar';
import Topbar from './Topbar';
import { useAppStore } from '../../store/useAppStore';

// Import all page components
import Dashboard from '../../pages/Dashboard';
import ClientMaster from '../../pages/ClientMaster';
import ITRFiling from '../../pages/ITRFiling';
import Reconciliation from '../../pages/Reconciliation';
import Sync from '../../pages/Sync';
import Jobs from '../../pages/Jobs';
import Notices from '../../pages/Notices';
import Calendar from '../../pages/Calendar';
import Tasks from '../../pages/Tasks';
import Billing from '../../pages/Billing';
import Accounting from '../../pages/Accounting';
import Reports from '../../pages/Reports';
import Communication from '../../pages/Communication';
import ClientDetailPanel from '../domain/ClientDetailPanel';

const viewMap: Record<string, React.ReactNode> = {
  dashboard:     <Dashboard />,
  clients:       <ClientMaster />,
  filing:        <ITRFiling />,
  reconciliation:<Reconciliation />,
  sync:          <Sync />,
  jobs:          <Jobs />,
  notices:       <Notices />,
  calendar:      <Calendar />,
  tasks:         <Tasks />,
  billing:       <Billing />,
  accounting:    <Accounting />,
  reports:       <Reports />,
  communication: <Communication />,
};

export default function Layout() {
  const { activeView, selectedClientId, setSelectedClientId } = useAppStore();
  return (
    <div className="h-screen overflow-hidden flex bg-bg">
      <Sidebar />
      <div className="flex flex-col flex-1 ml-[258px] overflow-hidden">
        <Topbar />
        {/* Content area */}
        <main className="flex-1 overflow-y-auto p-7 scrollbar-thin">
          {viewMap[activeView] ?? <Dashboard />}
        </main>
      </div>
      {/* Slide-in client detail panel */}
      {selectedClientId !== null && (
        <>
          <div
            className="fixed inset-0 bg-black/20 z-[199]"
            onClick={() => setSelectedClientId(null)}
          />
          <ClientDetailPanel clientId={selectedClientId} onClose={() => setSelectedClientId(null)} />
        </>
      )}
    </div>
  );
}
```

### `src/components/shell/Sidebar.tsx`

```tsx
import React from 'react';
import { useAppStore } from '../../store/useAppStore';

type NavItem = {
  id: string;
  label: string;
  badge?: string | number;
  badgeColor?: 'gold' | 'blue' | 'red';
  icon: React.ReactNode;
};

const SECTIONS: { label: string; items: NavItem[] }[] = [
  {
    label: 'Overview',
    items: [
      {
        id: 'dashboard', label: 'Dashboard',
        icon: <svg className="w-[18px] h-[18px]" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.6"><rect x="2" y="2" width="7" height="7" rx="1.5"/><rect x="11" y="2" width="7" height="7" rx="1.5"/><rect x="2" y="11" width="7" height="7" rx="1.5"/><rect x="11" y="11" width="7" height="7" rx="1.5"/></svg>,
      },
    ],
  },
  {
    label: 'Clients & Filing',
    items: [
      {
        id: 'clients', label: 'Client Master', badge: '5,000',
        icon: <svg className="w-[18px] h-[18px]" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.6"><circle cx="8" cy="6" r="3"/><path d="M2 17c0-3.3 2.7-6 6-6"/><circle cx="15" cy="12" r="3"/><path d="M11 17.9c.3-3.1 2.5-5.5 5.4-5.9"/></svg>,
      },
      {
        id: 'filing', label: 'ITR Filing', badge: 342, badgeColor: 'blue',
        icon: <svg className="w-[18px] h-[18px]" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.6"><path d="M14 2H6a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2V4a2 2 0 0 0-2-2z"/><path d="M8 10h4M8 13h4M8 7h2"/></svg>,
      },
      {
        id: 'reconciliation', label: 'Reconciliation', badge: 18, badgeColor: 'red',
        icon: <svg className="w-[18px] h-[18px]" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.6"><path d="M4 7h12M4 10h12M4 13h8"/><circle cx="16" cy="13" r="2"/></svg>,
      },
    ],
  },
  {
    label: 'Automation',
    items: [
      {
        id: 'sync', label: 'ITD Portal Sync',
        icon: <svg className="w-[18px] h-[18px]" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.6"><path d="M16.5 10A6.5 6.5 0 0 1 5.1 14.9"/><path d="M3.5 10A6.5 6.5 0 0 1 14.9 5.1"/><path d="M3.5 14V10h4"/><path d="M16.5 6v4h-4"/></svg>,
      },
      {
        id: 'jobs', label: 'Background Jobs', badge: 3, badgeColor: 'blue',
        icon: <svg className="w-[18px] h-[18px]" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.6"><rect x="2" y="5" width="16" height="12" rx="1.5"/><path d="M6 5V3M14 5V3"/><circle cx="14" cy="13" r="3"/><path d="M14 11.5V13l1 1"/></svg>,
      },
    ],
  },
  {
    label: 'Compliance',
    items: [
      {
        id: 'notices', label: 'Notice Management', badge: 7, badgeColor: 'red',
        icon: <svg className="w-[18px] h-[18px]" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.6"><path d="M10 2a6 6 0 0 1 6 6v2l1.5 3h-15L4 10V8a6 6 0 0 1 6-6z"/><path d="M8 15a2 2 0 0 0 4 0"/></svg>,
      },
      {
        id: 'calendar', label: 'Compliance Calendar',
        icon: <svg className="w-[18px] h-[18px]" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.6"><rect x="2" y="4" width="16" height="14" rx="1.5"/><path d="M6 2v4M14 2v4M2 9h16"/></svg>,
      },
      {
        id: 'tasks', label: 'Tasks & Work Queue', badge: 24,
        icon: <svg className="w-[18px] h-[18px]" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.6"><path d="M4 6h12M4 10h8M4 14h6"/><circle cx="16" cy="14" r="2"/><path d="M14.6 12.6l1 1 2-2"/></svg>,
      },
    ],
  },
  {
    label: 'Finance',
    items: [
      {
        id: 'billing', label: 'Billing & Fees',
        icon: <svg className="w-[18px] h-[18px]" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.6"><rect x="2" y="5" width="16" height="12" rx="1.5"/><path d="M2 9h16"/><path d="M6 13h2M10 13h4"/></svg>,
      },
      {
        id: 'accounting', label: 'Firm Accounting',
        icon: <svg className="w-[18px] h-[18px]" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.6"><path d="M6 3v14M14 3v14M3 9h14M3 14h14"/></svg>,
      },
    ],
  },
  {
    label: 'Analytics',
    items: [
      {
        id: 'reports', label: 'Reports & Analytics',
        icon: <svg className="w-[18px] h-[18px]" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.6"><path d="M4 16V8l4-4 4 4 4-4v12"/><path d="M4 16h12"/></svg>,
      },
      {
        id: 'communication', label: 'Communication',
        icon: <svg className="w-[18px] h-[18px]" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.6"><path d="M17 8.5c0 3-2.7 5.5-6 5.5a6.8 6.8 0 0 1-2.7-.5L3 15l1.5-4.5A5.3 5.3 0 0 1 5 8.5C5 5.5 7.7 3 11 3s6 2.5 6 5.5z"/></svg>,
      },
    ],
  },
];

function BadgePill({ value, color }: { value: string | number; color?: 'gold' | 'blue' | 'red' }) {
  const colorMap = {
    gold: 'bg-gold text-navy',
    blue: 'bg-[#3B82F6] text-white',
    red: 'bg-[#EF4444] text-white',
  };
  return (
    <span className={`ml-auto text-[10px] font-bold px-1.5 py-px rounded-[10px] min-w-[18px] text-center ${colorMap[color ?? 'gold']}`}>
      {value}
    </span>
  );
}

export default function Sidebar() {
  const { activeView, setActiveView, globalSearch, setGlobalSearch } = useAppStore();

  return (
    <aside className="fixed top-0 left-0 w-[258px] h-screen bg-navy flex flex-col z-[100] overflow-hidden">
      {/* Brand */}
      <div className="px-[22px] py-5 pb-4 border-b border-white/[0.08] flex-shrink-0">
        <div className="flex items-center gap-2.5">
          <div className="w-9 h-9 bg-gold rounded-lg flex items-center justify-center font-serif text-[18px] font-semibold text-navy flex-shrink-0">
            IT
          </div>
          <div className="flex flex-col">
            <span className="font-serif text-base font-semibold text-white tracking-[0.2px] leading-[1.2]">
              IncomeTax ERP
            </span>
            <span className="text-[10.5px] text-gold-light uppercase tracking-[0.4px] font-medium">
              Advocate Practice
            </span>
          </div>
        </div>
      </div>

      {/* Search */}
      <div className="px-4 pt-3.5 pb-2.5 flex-shrink-0">
        <div className="relative">
          <svg className="absolute left-3 top-1/2 -translate-y-1/2 text-text-muted" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/></svg>
          <input
            value={globalSearch}
            onChange={(e) => setGlobalSearch(e.target.value)}
            placeholder="Search clients, PAN…"
            className="w-full bg-white/[0.07] border border-white/10 rounded-[7px] py-2 pl-8 pr-3 text-white text-[13px] placeholder:text-text-muted outline-none focus:border-gold transition-colors font-sans"
          />
        </div>
      </div>

      {/* Nav */}
      <nav className="flex-1 overflow-y-auto px-2.5 pb-2.5 sidebar-nav">
        {SECTIONS.map((section) => (
          <React.Fragment key={section.label}>
            <div className="text-[10px] font-semibold tracking-[1px] uppercase text-white/30 px-3 pt-3.5 pb-1.5">
              {section.label}
            </div>
            {section.items.map((item) => {
              const isActive = activeView === item.id;
              return (
                <button
                  key={item.id}
                  onClick={() => setActiveView(item.id as any)}
                  className={`
                    relative w-full flex items-center gap-2.5 px-3 py-[9px] rounded-lg cursor-pointer
                    text-[13.5px] font-sans transition-all duration-150 mb-px text-left
                    ${isActive
                      ? 'bg-gold/[0.18] text-gold-light font-medium nav-active'
                      : 'text-white/65 hover:bg-white/[0.07] hover:text-white/90 font-normal'}
                  `}
                >
                  <span className={`flex-shrink-0 ${isActive ? 'opacity-100' : 'opacity-80'}`}>
                    {item.icon}
                  </span>
                  {item.label}
                  {item.badge !== undefined && (
                    <BadgePill value={item.badge} color={item.badgeColor} />
                  )}
                </button>
              );
            })}
          </React.Fragment>
        ))}
      </nav>

      {/* Footer user */}
      <div className="px-4 py-3.5 border-t border-white/[0.08] flex-shrink-0 flex items-center gap-2.5">
        <div className="w-[34px] h-[34px] rounded-full bg-gold flex items-center justify-center text-[13px] font-bold text-navy flex-shrink-0">
          AP
        </div>
        <div className="flex-1 min-w-0">
          <div className="text-[13px] font-medium text-white truncate">Adv. Prakash Mehta</div>
          <div className="text-[11px] text-white/45">Senior Partner</div>
        </div>
        <button className="text-white/35 hover:text-white/70 transition-colors p-1 rounded">
          <svg width="15" height="15" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.8"><path d="M13 10H3M7 6l-4 4 4 4M17 4v12"/></svg>
        </button>
      </div>
    </aside>
  );
}
```

### `src/components/shell/Topbar.tsx`

```tsx
import { useAppStore } from '../../store/useAppStore';
import { AssessmentYear } from '../../api/types';

const PAGE_META: Record<string, { title: string; sub: string }> = {
  dashboard:      { title: 'Dashboard', sub: 'AY 2025–26 & 2026–27 · 5,000 Active Clients' },
  clients:        { title: 'Client Master', sub: '5,000 clients · AY 2025–26' },
  filing:         { title: 'ITR Filing', sub: '342 in progress · AY 2025–26' },
  reconciliation: { title: 'Reconciliation', sub: '218 mismatches detected · 18 under review' },
  sync:           { title: 'ITD Portal Sync', sub: 'Playwright automation · AIS / TIS / 26AS' },
  jobs:           { title: 'Background Jobs', sub: '2 running · 1 queued · Spring Batch engine' },
  notices:        { title: 'Notice Management', sub: '31 active notices · 7 due this week' },
  calendar:       { title: 'Compliance Calendar', sub: 'April 2026 · Key deadlines & reminders' },
  tasks:          { title: 'Tasks & Work Queue', sub: '24 open tasks · 8 pending, 11 in progress' },
  billing:        { title: 'Billing & Fee Management', sub: '₹12.6L billed · ₹3.2L outstanding' },
  accounting:     { title: 'Firm Accounting', sub: 'Monthly P&L · Bank register · Partner drawings' },
  reports:        { title: 'Reports & Analytics', sub: 'AY completion, revenue, notices, sync status' },
  communication:  { title: 'Communication', sub: 'WhatsApp · Email · E-Verify OTP Bot' },
};

export default function Topbar() {
  const { activeView, activeAY, setActiveAY } = useAppStore();
  const meta = PAGE_META[activeView] ?? { title: activeView, sub: '' };

  return (
    <header className="h-[62px] bg-bg-card border-b border-border flex items-center px-7 gap-4 flex-shrink-0 z-50">
      <div className="flex-1">
        <h1 className="font-serif text-[20px] font-semibold text-text-primary leading-[1.2]">
          {meta.title}
        </h1>
        <p className="text-[12px] text-text-muted mt-px">{meta.sub}</p>
      </div>

      <div className="flex items-center gap-2.5">
        {/* AY Selector */}
        <select
          value={activeAY}
          onChange={(e) => setActiveAY(e.target.value as AssessmentYear)}
          className="bg-bg border border-border-strong rounded-[7px] px-3 py-[7px] text-[13px] font-medium text-text-primary cursor-pointer outline-none font-sans"
        >
          <option value="2025-26">AY 2025–26</option>
          <option value="2026-27">AY 2026–27</option>
        </select>

        {/* Notification bell */}
        <button className="w-9 h-9 bg-bg border border-border rounded-lg flex items-center justify-center text-text-secondary hover:bg-border hover:text-text-primary transition-all relative">
          <svg width="16" height="16" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.8"><path d="M10 2a6 6 0 0 1 6 6v2l1.5 3h-15L4 10V8a6 6 0 0 1 6-6z"/><path d="M8 15a2 2 0 0 0 4 0"/></svg>
          <span className="absolute top-1.5 right-1.5 w-[7px] h-[7px] bg-[#EF4444] rounded-full border-[1.5px] border-white" />
        </button>

        {/* Settings */}
        <button className="w-9 h-9 bg-bg border border-border rounded-lg flex items-center justify-center text-text-secondary hover:bg-border hover:text-text-primary transition-all">
          <svg width="16" height="16" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.8"><circle cx="10" cy="10" r="3"/><path d="M10 2v2M10 16v2M2 10h2M16 10h2M4.2 4.2l1.4 1.4M14.4 14.4l1.4 1.4M4.2 15.8l1.4-1.4M14.4 5.6l1.4-1.4"/></svg>
        </button>
      </div>
    </header>
  );
}
```

## 1.8 Primitive UI Components

### `src/components/ui/Badge.tsx`

```tsx
type BadgeVariant =
  | 'success' | 'warning' | 'danger' | 'info'
  | 'gold' | 'muted' | 'navy' | 'dot-success'
  | 'dot-warning' | 'dot-info' | 'dot-muted';

const VARIANT_MAP: Record<BadgeVariant, string> = {
  success:     'bg-success-bg text-success',
  warning:     'bg-warning-bg text-warning',
  danger:      'bg-danger-bg text-danger',
  info:        'bg-info-bg text-info',
  gold:        'bg-gold-pale text-[#92640A]',
  muted:       'bg-bg text-text-secondary border border-border',
  navy:        'bg-[#E0E8F4] text-navy',
  'dot-success': 'bg-success-bg text-success',
  'dot-warning': 'bg-warning-bg text-warning',
  'dot-info':    'bg-info-bg text-info',
  'dot-muted':   'bg-bg text-text-secondary border border-border',
};

const DOT_VARIANTS: BadgeVariant[] = ['dot-success', 'dot-warning', 'dot-info', 'dot-muted'];

interface BadgeProps {
  variant: BadgeVariant;
  children: React.ReactNode;
  className?: string;
}

export default function Badge({ variant, children, className = '' }: BadgeProps) {
  const hasDot = DOT_VARIANTS.includes(variant);
  return (
    <span className={`inline-flex items-center gap-1 px-[9px] py-[3px] rounded-[20px] text-[11.5px] font-semibold whitespace-nowrap leading-[1.4] ${VARIANT_MAP[variant]} ${className}`}>
      {hasDot && <span className="w-[5px] h-[5px] rounded-full bg-current inline-block flex-shrink-0" />}
      {children}
    </span>
  );
}
```

### `src/components/ui/Button.tsx`

```tsx
interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger';
  size?: 'sm' | 'md';
  children: React.ReactNode;
}

const VARIANT_MAP = {
  primary:   'bg-gold text-navy hover:bg-gold-light',
  secondary: 'bg-bg border border-border-strong text-text-primary hover:bg-border',
  ghost:     'bg-transparent border border-border text-text-secondary hover:bg-bg',
  danger:    'bg-danger-bg text-danger border border-[#FECACA] hover:bg-[#FECACA]',
};
const SIZE_MAP = { sm: 'px-[11px] py-[5px] text-[12px]', md: 'px-4 py-2 text-[13px]' };

export default function Button({ variant = 'primary', size = 'md', children, className = '', ...props }: ButtonProps) {
  return (
    <button
      className={`inline-flex items-center gap-1.5 rounded-lg font-medium font-sans cursor-pointer border-none transition-all duration-150 whitespace-nowrap ${VARIANT_MAP[variant]} ${SIZE_MAP[size]} ${className}`}
      {...props}
    >
      {children}
    </button>
  );
}
```

### `src/components/ui/Card.tsx`

```tsx
interface CardProps {
  children: React.ReactNode;
  className?: string;
  padding?: boolean;
}
export default function Card({ children, className = '', padding = true }: CardProps) {
  return (
    <div className={`bg-bg-card border border-border rounded-[10px] ${padding ? 'p-5 px-[22px]' : ''} ${className}`}>
      {children}
    </div>
  );
}

export function CardHeader({ children, className = '' }: { children: React.ReactNode; className?: string }) {
  return <div className={`flex items-center justify-between mb-[18px] ${className}`}>{children}</div>;
}
export function CardTitle({ children }: { children: React.ReactNode }) {
  return <div className="text-[14px] font-semibold text-text-primary">{children}</div>;
}
export function CardSub({ children }: { children: React.ReactNode }) {
  return <div className="text-[12px] text-text-muted mt-0.5">{children}</div>;
}
```

### `src/components/ui/StatCard.tsx`

```tsx
type StatAccent = 'gold' | 'blue' | 'teal' | 'rose' | 'success' | 'warning';
const ACCENT_MAP: Record<StatAccent, string> = {
  gold:    'before:bg-gold',
  blue:    'before:bg-accent-blue',
  teal:    'before:bg-accent-teal',
  rose:    'before:bg-accent-rose',
  success: 'before:bg-success',
  warning: 'before:bg-warning',
};

interface StatCardProps {
  accent: StatAccent;
  label: string;
  value: string | number;
  meta?: React.ReactNode;
}
export default function StatCard({ accent, label, value, meta }: StatCardProps) {
  return (
    <div className={`bg-bg-card border border-border rounded-[10px] p-[18px] px-5 relative overflow-hidden before:absolute before:top-0 before:left-0 before:w-full before:h-[3px] stat-card-accent ${ACCENT_MAP[accent]}`}>
      <div className="text-[11.5px] font-medium text-text-muted uppercase tracking-[0.5px]">{label}</div>
      <div className="font-serif text-[32px] font-semibold text-text-primary leading-[1.1] my-1.5">{value}</div>
      {meta && <div className="text-[12px] text-text-muted">{meta}</div>}
    </div>
  );
}
```

### `src/components/ui/ProgressBar.tsx`

```tsx
type ProgressColor = 'gold' | 'blue' | 'teal' | 'success' | 'danger';
const COLOR_MAP: Record<ProgressColor, string> = {
  gold:    'bg-gold',
  blue:    'bg-accent-blue',
  teal:    'bg-accent-teal',
  success: 'bg-success',
  danger:  'bg-danger',
};
export default function ProgressBar({ value, color = 'gold', className = '' }: { value: number; color?: ProgressColor; className?: string }) {
  return (
    <div className={`h-1.5 bg-border rounded-full overflow-hidden ${className}`}>
      <div className={`h-full rounded-full transition-all duration-500 ${COLOR_MAP[color]}`} style={{ width: `${Math.min(100, Math.max(0, value))}%` }} />
    </div>
  );
}
```

### `src/components/ui/Pagination.tsx`

```tsx
interface PaginationProps {
  page: number;
  totalPages: number;
  totalElements: number;
  size: number;
  onChange: (page: number) => void;
}
export default function Pagination({ page, totalPages, totalElements, size, onChange }: PaginationProps) {
  const from = page * size + 1;
  const to = Math.min((page + 1) * size, totalElements);
  return (
    <div className="flex items-center justify-between pt-3.5 border-t border-border mt-1">
      <span className="text-[12.5px] text-text-muted">Showing {from}–{to} of {totalElements.toLocaleString()}</span>
      <div className="flex items-center gap-2">
        <button onClick={() => onChange(page - 1)} disabled={page === 0} className="px-[11px] py-[5px] text-[12px] bg-transparent border border-border text-text-secondary rounded-lg hover:bg-bg disabled:opacity-40 disabled:cursor-not-allowed">← Prev</button>
        {Array.from({ length: Math.min(3, totalPages) }, (_, i) => (
          <button key={i} onClick={() => onChange(i)}
            className={`px-[11px] py-[5px] text-[12px] rounded-lg border ${i === page ? 'bg-gold text-navy border-gold' : 'border-border text-text-secondary hover:bg-bg'}`}>
            {i + 1}
          </button>
        ))}
        {totalPages > 3 && <span className="text-[13px] text-text-muted">… {totalPages}</span>}
        <button onClick={() => onChange(page + 1)} disabled={page >= totalPages - 1} className="px-[11px] py-[5px] text-[12px] bg-transparent border border-border text-text-secondary rounded-lg hover:bg-bg disabled:opacity-40 disabled:cursor-not-allowed">Next →</button>
      </div>
    </div>
  );
}
```

---

# PHASE 2 — DASHBOARD, CLIENT MASTER, ITR FILING LIST, RECONCILIATION, CLIENT DETAIL PANEL

**Deliverable:** All primary workflow views functional with real API calls, pagination, search, filters, and the slide-in client detail panel.
**Gate:** All views render with live data from backend. Client panel opens with correct data. No console errors.

---

## 2.1 API Layer — `src/api/clients.ts`

```typescript
import api from './axios';
import { Client, PageResponse } from './types';

export interface ClientFilters {
  search?: string;
  type?: string;
  filingStatus?: string;
  assessmentYear?: string;
  page?: number;
  size?: number;
}

export const clientsApi = {
  list: (filters: ClientFilters) =>
    api.get<PageResponse<Client>>('/clients', { params: filters }).then(r => r.data),

  get: (id: number) =>
    api.get<Client>(`/clients/${id}`).then(r => r.data),

  create: (data: Partial<Client>) =>
    api.post<Client>('/clients', data).then(r => r.data),

  update: (id: number, data: Partial<Client>) =>
    api.put<Client>(`/clients/${id}`, data).then(r => r.data),

  delete: (id: number) =>
    api.delete(`/clients/${id}`).then(r => r.data),

  getFilingHistory: (id: number) =>
    api.get<any[]>(`/clients/${id}/filing-history`).then(r => r.data),

  exportExcel: (filters: ClientFilters) =>
    api.get('/clients/export', { params: filters, responseType: 'blob' }).then(r => r.data),

  bulkImport: (file: File) => {
    const fd = new FormData();
    fd.append('file', file);
    return api.post('/clients/bulk-import', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then(r => r.data);
  },

  getDashboardStats: () =>
    api.get<{
      total: number; filed: number; inProgress: number;
      docPending: number; watchList: number;
      totalMismatches: number; totalNotices: number;
    }>('/dashboard/stats').then(r => r.data),

  getActivityFeed: () =>
    api.get<{ text: string; time: string; dotColor: string }[]>('/dashboard/activity').then(r => r.data),
};
```

## 2.2 `src/pages/Dashboard.tsx`

```tsx
import { useQuery } from '@tanstack/react-query';
import { clientsApi } from '../api/clients';
import { jobsApi } from '../api/jobs';
import StatCard from '../components/ui/StatCard';
import Card, { CardHeader, CardTitle } from '../components/ui/Card';
import Button from '../components/ui/Button';
import ProgressBar from '../components/ui/ProgressBar';
import Badge from '../components/ui/Badge';
import { useAppStore } from '../store/useAppStore';

export default function Dashboard() {
  const { setActiveView } = useAppStore();
  const { data: stats } = useQuery({ queryKey: ['dashboard-stats'], queryFn: clientsApi.getDashboardStats });
  const { data: activity } = useQuery({ queryKey: ['dashboard-activity'], queryFn: clientsApi.getActivityFeed });
  const { data: jobs } = useQuery({ queryKey: ['jobs-running'], queryFn: () => jobsApi.list({ status: 'RUNNING', size: 3 }) });

  return (
    <div>
      {/* Stats Grid — 4 columns */}
      <div className="grid grid-cols-4 gap-3.5 mb-5">
        <StatCard accent="gold"    label="Total Active Clients" value={stats?.total?.toLocaleString() ?? '5,000'} meta="AY 2025–26" />
        <StatCard accent="teal"    label="Returns Filed"        value={stats?.filed?.toLocaleString() ?? '2,891'} meta={<span className="text-success font-medium">↑ 18% vs last year</span>} />
        <StatCard accent="blue"    label="In Progress"          value={stats?.inProgress?.toLocaleString() ?? '342'} meta="Computation + Reconciliation" />
        <StatCard accent="rose"    label="Active Notices"       value={stats?.totalNotices?.toLocaleString() ?? '31'} meta="7 response due this week" />
      </div>

      {/* 2-column main grid */}
      <div className="grid grid-cols-[1fr_340px] gap-4 mb-5">
        {/* Filing status donut */}
        <Card>
          <CardHeader>
            <div>
              <CardTitle>AY 2025–26 Filing Progress</CardTitle>
              <div className="text-[12px] text-text-muted mt-0.5">5,000 clients · As of today</div>
            </div>
            <div className="flex gap-2">
              <div className="ay-tab-btn active" onClick={() => setActiveView('filing')}>
                <Button variant="ghost" size="sm">View Filing Queue →</Button>
              </div>
            </div>
          </CardHeader>
          <div className="flex items-center gap-6">
            {/* Donut chart */}
            <div className="relative w-[120px] h-[120px] rounded-full flex-shrink-0"
              style={{ background: 'conic-gradient(#15803D 0deg 172deg, #C9943A 172deg 230deg, #1D6FA4 230deg 280deg, #EF4444 280deg 310deg, #C8D3E0 310deg 360deg)' }}>
              <div className="absolute inset-0 flex items-center justify-center">
                <div className="w-[72px] h-[72px] bg-bg-card rounded-full flex items-center justify-center text-center">
                  <div>
                    <div className="font-serif text-[18px] font-semibold leading-none">58%</div>
                    <div className="text-[10px] text-text-muted">Filed</div>
                  </div>
                </div>
              </div>
            </div>
            {/* Legend */}
            <div className="flex flex-col gap-2 flex-1">
              {[
                { label: 'Filed', value: '2,891', color: 'bg-success' },
                { label: 'Ready to File', value: '342', color: 'bg-gold' },
                { label: 'Reconciliation', value: '284', color: 'bg-accent-blue' },
                { label: 'Mismatch Review', value: '218', color: 'bg-[#EF4444]' },
                { label: 'Doc Pending', value: '265', color: 'bg-border-strong' },
              ].map(item => (
                <div key={item.label} className="flex items-center gap-2 text-[12.5px]">
                  <div className={`w-2.5 h-2.5 rounded-full flex-shrink-0 ${item.color}`} />
                  <span className="text-text-secondary flex-1">{item.label}</span>
                  <span className="font-semibold text-text-primary">{item.value}</span>
                </div>
              ))}
            </div>
          </div>
        </Card>

        {/* Deadlines */}
        <Card>
          <CardHeader><CardTitle>⚠ Upcoming Deadlines</CardTitle></CardHeader>
          <div className="flex flex-col">
            {[
              { label: 'Notice 143(1)(a)', sub: 'Priya Nair · Respond by Today 5 PM', dot: 'red' },
              { label: 'Advance Tax Q4', sub: 'Mar 15 · Payment deadline', dot: 'red' },
              { label: 'E-Verify Expiry', sub: '3 clients · 120-day window closing', dot: 'gold' },
            ].map(item => (
              <div key={item.label} className="flex gap-3 py-3 border-b border-border last:border-b-0 items-start">
                <div className={`w-2 h-2 rounded-full mt-[5px] flex-shrink-0 ${item.dot === 'red' ? 'bg-[#EF4444]' : 'bg-gold'}`} />
                <div className="flex-1">
                  <div className="text-[13px] font-semibold">{item.label}</div>
                  <div className="text-[11.5px] text-text-muted mt-0.5">{item.sub}</div>
                </div>
              </div>
            ))}
          </div>
        </Card>
      </div>

      {/* Background Jobs Quick View */}
      <Card className="mb-5">
        <CardHeader>
          <CardTitle>Background Jobs Status</CardTitle>
          <Button variant="ghost" size="sm" onClick={() => setActiveView('jobs')}>View all jobs →</Button>
        </CardHeader>
        {(jobs?.content ?? [
          { jobId: 847, jobName: 'Annual Bulk Sync — Batch 6/25 (AY 2025–26)', jobDetail: '200 clients · 148 done · 52 pending', progressPct: 74, status: 'RUNNING' },
          { jobId: 848, jobName: 'WhatsApp Advance Tax Reminder Broadcast', jobDetail: 'Sending to 48 clients · 31 delivered · 17 queued', progressPct: 65, status: 'RUNNING' },
        ]).map((job: any) => (
          <div key={job.jobId} className="flex items-center gap-3.5 py-3.5 border-b border-border last:border-b-0">
            <div className={`w-2.5 h-2.5 rounded-full flex-shrink-0 ${job.status === 'RUNNING' ? 'bg-accent-blue animate-pulse-dot' : job.status === 'COMPLETED' ? 'bg-success' : 'bg-danger'}`} />
            <div className="flex-1">
              <div className="text-[13.5px] font-medium">{job.jobName}</div>
              <div className="text-[12px] text-text-muted mt-0.5">{job.jobDetail}</div>
            </div>
            <div className="w-[120px]">
              <div className="text-[12px] font-medium text-text-secondary text-right mb-1">{job.progressPct}%</div>
              <ProgressBar value={job.progressPct} color={job.status === 'RUNNING' ? 'blue' : 'teal'} />
            </div>
            <Button variant="danger" size="sm">Stop</Button>
          </div>
        ))}
      </Card>
    </div>
  );
}
```

## 2.3 `src/pages/ClientMaster.tsx`

```tsx
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { clientsApi, ClientFilters } from '../api/clients';
import { Client } from '../api/types';
import Card from '../components/ui/Card';
import Badge from '../components/ui/Badge';
import Button from '../components/ui/Button';
import Pagination from '../components/ui/Pagination';
import { useAppStore } from '../store/useAppStore';

const FILING_BADGE: Record<string, 'success' | 'info' | 'warning' | 'danger' | 'gold'> = {
  'Filed':          'success',
  'In Progress':    'info',
  'Doc Pending':    'warning',
  'Mismatch':       'danger',
  'Ready to File':  'gold',
};

export default function ClientMaster() {
  const { activeAY, setSelectedClientId } = useAppStore();
  const [filters, setFilters] = useState<ClientFilters>({ page: 0, size: 25, assessmentYear: activeAY });

  const { data, isLoading } = useQuery({
    queryKey: ['clients', filters],
    queryFn: () => clientsApi.list(filters),
    placeholderData: (prev) => prev,
  });

  function updateFilter(key: keyof ClientFilters, value: any) {
    setFilters(prev => ({ ...prev, [key]: value, page: 0 }));
  }

  return (
    <div>
      {/* Filter bar */}
      <div className="flex items-center gap-2.5 mb-4 flex-wrap">
        <div className="relative">
          <svg className="absolute left-3 top-1/2 -translate-y-1/2 text-text-muted" width="14" height="14" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="9" cy="9" r="6"/><path d="m15 15-3-3"/></svg>
          <input
            className="pl-8 pr-3 py-2 bg-bg-card border border-border-strong rounded-lg text-[13px] outline-none focus:border-gold w-64 font-sans"
            placeholder="Search by name, PAN, mobile…"
            onChange={e => updateFilter('search', e.target.value)}
          />
        </div>
        {[
          { key: 'type', options: ['All Types', 'Individual', 'HUF', 'Firm', 'Company'] },
          { key: 'filingStatus', options: ['All Statuses', 'Filed', 'In Progress', 'Doc Pending', 'Mismatch', 'Watch List'] },
          { key: 'assessmentYear', options: ['AY 2025–26', 'AY 2026–27'] },
        ].map(({ key, options }) => (
          <select key={key} onChange={e => updateFilter(key as any, e.target.value)}
            className="bg-bg-card border border-border-strong rounded-lg px-3 py-2 text-[13px] text-text-primary outline-none focus:border-gold font-sans cursor-pointer">
            {options.map(o => <option key={o}>{o}</option>)}
          </select>
        ))}
        <div className="ml-auto flex gap-2">
          <Button variant="secondary" size="sm">↓ Export Excel</Button>
          <Button variant="primary" size="sm">+ Add Client</Button>
          <Button variant="secondary" size="sm">↑ Bulk Import</Button>
        </div>
      </div>

      <Card padding={false}>
        <div className="flex items-center justify-between p-5 px-[22px] pb-0 mb-0">
          <div>
            <div className="text-[14px] font-semibold text-text-primary">All Clients</div>
            <div className="text-[12px] text-text-muted mt-0.5">
              {data ? `${data.totalElements.toLocaleString()} clients` : 'Loading…'}
            </div>
          </div>
          <div className="flex items-center gap-2">
            <Badge variant="dot-success">2,891 Filed</Badge>
            <Badge variant="dot-warning">518 Pending</Badge>
            <Badge variant="dot-muted">31 Watch List</Badge>
          </div>
        </div>

        <div className="overflow-x-auto mt-4">
          <table className="w-full border-collapse">
            <thead>
              <tr>
                {['', 'Client Name', 'PAN', 'Type', 'Mobile', 'ITR Form', 'AIS Sync', 'Filing Status', 'E-Verify', 'Watch', 'Actions'].map(h => (
                  <th key={h} className="text-left px-3.5 py-2.5 text-[11.5px] font-semibold text-text-muted uppercase tracking-[0.5px] bg-bg border-b border-border whitespace-nowrap">
                    {h === '' ? <input type="checkbox" className="cursor-pointer" /> : h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                Array.from({ length: 8 }).map((_, i) => (
                  <tr key={i} className="border-b border-border">
                    {Array.from({ length: 11 }).map((_, j) => (
                      <td key={j} className="px-3.5 py-3"><div className="h-4 bg-border rounded animate-pulse" /></td>
                    ))}
                  </tr>
                ))
              ) : (data?.content ?? []).map((client: Client) => (
                <tr
                  key={client.id}
                  onClick={() => setSelectedClientId(client.id)}
                  className="cursor-pointer hover:bg-[#FAFBFD] border-b border-border last:border-b-0"
                >
                  <td className="px-3.5 py-3">
                    <input type="checkbox" onClick={e => e.stopPropagation()} className="cursor-pointer" />
                  </td>
                  <td className="px-3.5 py-3">
                    <div className="flex items-center gap-2.5">
                      <div className="w-[30px] h-[30px] rounded-lg bg-navy flex items-center justify-center font-serif text-[12px] text-gold-light font-semibold flex-shrink-0">
                        {client.initials}
                      </div>
                      <div className="font-medium text-[13.5px]">{client.name}</div>
                    </div>
                  </td>
                  <td className="px-3.5 py-3 font-mono text-[12.5px] tracking-[0.3px]">{client.pan}</td>
                  <td className="px-3.5 py-3"><Badge variant="muted">{client.type}</Badge></td>
                  <td className="px-3.5 py-3 text-text-secondary text-[12.5px]">{client.mobile}</td>
                  <td className="px-3.5 py-3"><Badge variant="navy">{client.itrForm}</Badge></td>
                  <td className="px-3.5 py-3 text-text-secondary text-[12.5px]">{client.lastSynced}</td>
                  <td className="px-3.5 py-3">
                    <Badge variant={FILING_BADGE[client.filingStatus] ?? 'muted'}>{client.filingStatus}</Badge>
                  </td>
                  <td className="px-3.5 py-3">
                    <Badge variant={client.evStatus === 'Verified' ? 'success' : client.evStatus === 'Pending' ? 'warning' : 'muted'}>
                      {client.evStatus}
                    </Badge>
                  </td>
                  <td className="px-3.5 py-3">
                    {client.watchList
                      ? <Badge variant="danger">⚠ Watch</Badge>
                      : <span className="text-text-muted text-[13px]">—</span>}
                  </td>
                  <td className="px-3.5 py-3">
                    <div className="flex gap-1.5" onClick={e => e.stopPropagation()}>
                      <Button variant="ghost" size="sm">File</Button>
                      <Button variant="ghost" size="sm">Sync</Button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="px-[22px] pb-4">
          {data && (
            <Pagination
              page={filters.page ?? 0}
              totalPages={data.totalPages}
              totalElements={data.totalElements}
              size={filters.size ?? 25}
              onChange={(p) => setFilters(prev => ({ ...prev, page: p }))}
            />
          )}
        </div>
      </Card>
    </div>
  );
}
```

## 2.4 `src/components/domain/ClientDetailPanel.tsx`

```tsx
import { useQuery } from '@tanstack/react-query';
import { clientsApi } from '../../api/clients';
import Badge from '../ui/Badge';
import Button from '../ui/Button';
import { useAppStore } from '../../store/useAppStore';

interface Props { clientId: number; onClose: () => void; }

export default function ClientDetailPanel({ clientId, onClose }: Props) {
  const { openComputation } = useAppStore();
  const { data: client, isLoading } = useQuery({
    queryKey: ['client', clientId],
    queryFn: () => clientsApi.get(clientId),
  });

  return (
    <div className="fixed top-0 right-0 w-[440px] h-screen bg-bg-card border-l border-border z-[200] flex flex-col shadow-[-4px_0_20px_rgba(0,0,0,0.08)] overflow-hidden">
      {/* Header */}
      <div className="px-6 pt-5 pb-4 border-b border-border flex items-start gap-3 flex-shrink-0">
        <div className="w-[46px] h-[46px] rounded-xl bg-navy flex items-center justify-center font-serif text-[18px] font-semibold text-gold-light flex-shrink-0">
          {client?.initials ?? '??'}
        </div>
        <div className="flex-1">
          <div className="text-[16px] font-semibold text-text-primary">{client?.name ?? '—'}</div>
          <div className="font-mono text-[12px] text-text-muted mt-0.5">{client?.pan} · {client?.type}</div>
        </div>
        <button onClick={onClose} className="text-text-muted hover:text-text-primary hover:bg-bg rounded p-1 transition-colors">
          <svg width="16" height="16" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="2"><path d="M15 5 5 15M5 5l10 10"/></svg>
        </button>
      </div>

      <div className="flex-1 overflow-y-auto px-6 py-5">
        {isLoading ? (
          <div className="space-y-3">
            {Array.from({ length: 8 }).map((_, i) => (
              <div key={i} className="h-5 bg-border rounded animate-pulse" />
            ))}
          </div>
        ) : client ? (
          <>
            {/* Status badges */}
            <div className="flex gap-1.5 flex-wrap mb-4">
              <Badge variant={client.filingStatus === 'Filed' ? 'dot-success' : 'dot-warning'}>{client.filingStatus}</Badge>
              <Badge variant={client.evStatus === 'Verified' ? 'info' : 'muted'}>{client.evStatus}</Badge>
              <Badge variant="muted">{client.itrForm}</Badge>
            </div>

            {/* Personal details */}
            <PanelSection title="Personal Details">
              <DetailRow label="Full Name"     value={client.name} />
              <DetailRow label="PAN"           value={client.pan} mono />
              <DetailRow label="Date of Birth" value={client.dateOfBirth} />
              <DetailRow label="Aadhaar"       value={client.aadhaarMasked} mono />
              <DetailRow label="Mobile"        value={client.mobile} />
              <DetailRow label="Email"         value={client.email} />
              <DetailRow label="Comm. Channel" value={<Badge variant="success">{client.communicationChannel}</Badge>} />
            </PanelSection>

            {/* Filing details */}
            <PanelSection title={`AY ${client.assessmentYear} Filing`}>
              <DetailRow label="ITR Form"     value={client.itrForm} />
              <DetailRow label="Filing Status" value={<Badge variant={client.filingStatus === 'Filed' ? 'success' : 'info'}>{client.filingStatus}</Badge>} />
              <DetailRow label="Recon Status" value={<Badge variant={client.reconStatus === 'Clean' ? 'success' : client.reconStatus === 'Mismatch' ? 'danger' : 'muted'}>{client.reconStatus}</Badge>} />
              <DetailRow label="Last Synced"  value={client.lastSynced} />
            </PanelSection>

            {/* Actions */}
            <div className="flex flex-col gap-2 mt-4">
              <Button variant="primary" className="w-full justify-center" onClick={() => { openComputation(clientId); onClose(); }}>
                Open Computation
              </Button>
              <Button variant="secondary" className="w-full justify-center">Download Computation</Button>
              <Button variant="ghost" className="w-full justify-center">Send WhatsApp</Button>
              <Button variant="ghost" className="w-full justify-center">View All AYs</Button>
            </div>
          </>
        ) : null}
      </div>
    </div>
  );
}

function PanelSection({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="mb-[22px]">
      <div className="text-[11px] font-bold uppercase tracking-[0.8px] text-text-muted mb-2.5 pb-2 border-b border-border">
        {title}
      </div>
      {children}
    </div>
  );
}

function DetailRow({ label, value, mono }: { label: string; value: React.ReactNode; mono?: boolean }) {
  return (
    <div className="flex items-start gap-2.5 py-1.5">
      <div className="text-[12px] text-text-muted w-[130px] flex-shrink-0">{label}</div>
      <div className={`text-[13px] text-text-primary flex-1 ${mono ? 'font-mono text-[12.5px]' : ''}`}>{value}</div>
    </div>
  );
}
```

## 2.5 `src/pages/ITRFiling.tsx`

```tsx
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { filingApi } from '../api/filing';
import Card from '../components/ui/Card';
import Badge from '../components/ui/Badge';
import Button from '../components/ui/Button';
import Pagination from '../components/ui/Pagination';
import { useAppStore } from '../store/useAppStore';

const STAGE_BADGE: Record<string, any> = {
  'Mismatch Review': { variant: 'danger',  label: 'Mismatch Review' },
  'Reconciliation':  { variant: 'info',    label: 'Reconciliation' },
  'Ready to File':   { variant: 'gold',    label: 'Ready to File' },
  'Doc Pending':     { variant: 'warning', label: 'Doc Pending' },
  'Filed':           { variant: 'success', label: 'Filed' },
};

const PIPELINE_STAGES = [
  { n: '✓', label: 'Docs Downloaded', state: 'done' },
  { n: '✓', label: 'AIS Reconciled',  state: 'done' },
  { n: '3', label: 'Computation Review', state: 'current' },
  { n: '4', label: 'Ready to File',   state: 'pending' },
  { n: '5', label: 'Filed via ITD API', state: 'pending' },
  { n: '6', label: 'E-Verification',  state: 'pending' },
  { n: '7', label: 'Computation Sent', state: 'pending' },
];

export default function ITRFiling() {
  const { activeAY, openComputation } = useAppStore();
  const [page, setPage] = useState(0);
  const [stage, setStage] = useState('');
  const [form, setForm] = useState('');

  const { data, isLoading } = useQuery({
    queryKey: ['filing-list', activeAY, stage, form, page],
    queryFn: () => filingApi.list({ assessmentYear: activeAY, stage, itrForm: form, page, size: 25 }),
    placeholderData: (prev) => prev,
  });

  return (
    <div>
      {/* AY tabs */}
      <div className="flex gap-1.5 mb-5">
        {['AY 2025–26', 'AY 2026–27'].map((ay) => (
          <button key={ay} className={`px-3.5 py-1.5 rounded-[20px] text-[12.5px] font-medium cursor-pointer border transition-all ${activeAY === ay.slice(3) ? 'bg-navy text-gold-light border-navy' : 'bg-bg-card border-border-strong text-text-secondary hover:border-gold hover:text-gold'}`}>
            {ay}
          </button>
        ))}
      </div>

      {/* Filing pipeline visual */}
      <Card className="mb-5">
        <div className="flex items-center justify-between mb-4">
          <div className="text-[14px] font-semibold">Filing Workflow Pipeline</div>
          <Badge variant="gold">342 In Progress</Badge>
        </div>
        <div className="flex items-center overflow-x-auto">
          {PIPELINE_STAGES.map((stage, i) => (
            <div key={i} className="flex items-center flex-shrink-0">
              <div className="flex flex-col items-center">
                <div className={`w-11 h-11 rounded-full flex items-center justify-center text-base border-2 font-semibold relative z-[1]
                  ${stage.state === 'done'    ? 'bg-success-bg border-success text-success'
                  : stage.state === 'current' ? 'bg-gold-pale border-gold text-[#92640A]'
                  :                             'bg-bg border-border text-text-muted'}`}>
                  {stage.n}
                </div>
                <div className={`text-[11px] mt-1.5 text-center max-w-[70px] leading-[1.3]
                  ${stage.state === 'done'    ? 'text-success font-medium'
                  : stage.state === 'current' ? 'text-[#92640A] font-semibold'
                  :                             'text-text-muted'}`}>
                  {stage.label}
                </div>
              </div>
              {i < PIPELINE_STAGES.length - 1 && (
                <div className={`h-0.5 w-10 flex-shrink-0 -mt-[22px] ${stage.state === 'done' ? 'bg-success' : 'bg-border'}`} />
              )}
            </div>
          ))}
        </div>
      </Card>

      {/* Filters */}
      <div className="flex items-center gap-2.5 mb-4 flex-wrap">
        <div className="relative">
          <svg className="absolute left-3 top-1/2 -translate-y-1/2 text-text-muted" width="14" height="14" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="9" cy="9" r="6"/><path d="m15 15-3-3"/></svg>
          <input className="pl-8 pr-3 py-2 bg-bg-card border border-border-strong rounded-lg text-[13px] outline-none focus:border-gold w-60 font-sans" placeholder="Search client name or PAN…" />
        </div>
        <select onChange={e => setStage(e.target.value)} className="bg-bg-card border border-border-strong rounded-lg px-3 py-2 text-[13px] outline-none focus:border-gold font-sans cursor-pointer">
          {['All Stages', 'Doc Pending', 'Reconciliation', 'Mismatch Review', 'Ready to File', 'Filed', 'E-Verify Pending'].map(o => <option key={o}>{o}</option>)}
        </select>
        <select onChange={e => setForm(e.target.value)} className="bg-bg-card border border-border-strong rounded-lg px-3 py-2 text-[13px] outline-none focus:border-gold font-sans cursor-pointer">
          {['All ITR Forms', 'ITR-1', 'ITR-2', 'ITR-3', 'ITR-4'].map(o => <option key={o}>{o}</option>)}
        </select>
        <div className="ml-auto">
          <Button variant="primary" size="sm">+ New ITR Filing</Button>
        </div>
      </div>

      {/* Table */}
      <Card padding={false}>
        <div className="overflow-x-auto">
          <table className="w-full border-collapse">
            <thead>
              <tr>
                {['Client', 'PAN', 'ITR Form', 'AIS Synced', 'Recon Status', 'Filing Stage', 'Tax Payable', 'Refund', 'E-Verify', 'Actions'].map(h => (
                  <th key={h} className="text-left px-3.5 py-2.5 text-[11.5px] font-semibold text-text-muted uppercase tracking-[0.5px] bg-bg border-b border-border whitespace-nowrap">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {(data?.content ?? []).map((row: any) => (
                <tr key={row.id} className="cursor-pointer hover:bg-[#FAFBFD] border-b border-border last:border-b-0">
                  <td className="px-3.5 py-3 font-medium text-[13.5px]">{row.clientName}</td>
                  <td className="px-3.5 py-3 font-mono text-[12.5px]">{row.pan}</td>
                  <td className="px-3.5 py-3"><Badge variant="navy">{row.itrForm}</Badge></td>
                  <td className="px-3.5 py-3 text-text-secondary text-[12.5px]">{row.lastSynced}</td>
                  <td className="px-3.5 py-3">
                    <Badge variant={row.reconStatus === 'Clean' ? 'success' : row.reconStatus === 'Mismatch' ? 'danger' : row.reconStatus === 'In Progress' ? 'info' : 'muted'}>
                      {row.reconStatus}
                    </Badge>
                  </td>
                  <td className="px-3.5 py-3">
                    <Badge variant={STAGE_BADGE[row.filingStage]?.variant ?? 'muted'}>{row.filingStage}</Badge>
                  </td>
                  <td className="px-3.5 py-3 font-semibold" style={{ color: row.taxPayable > 0 ? '#B91C1C' : '#8A9BB0' }}>
                    {row.taxPayable > 0 ? `₹${row.taxPayable.toLocaleString()}` : '—'}
                  </td>
                  <td className="px-3.5 py-3 text-text-secondary text-[12.5px]">
                    {row.refundDue > 0 ? `₹${row.refundDue.toLocaleString()}` : '—'}
                  </td>
                  <td className="px-3.5 py-3 text-text-secondary text-[12.5px]">{row.evcStatus ?? '—'}</td>
                  <td className="px-3.5 py-3">
                    <div className="flex gap-1.5">
                      <Button variant="primary" size="sm" onClick={() => openComputation(row.clientId)}>Open</Button>
                      <Button variant="ghost" size="sm">Sync</Button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {data && (
          <div className="px-[22px] pb-4">
            <Pagination page={page} totalPages={data.totalPages} totalElements={data.totalElements} size={25} onChange={setPage} />
          </div>
        )}
      </Card>
    </div>
  );
}
```

## 2.6 API Service — `src/api/filing.ts`

```typescript
import api from './axios';
import { FilingResult, ITRFormData, PageResponse, ValidationResult, TDSMismatchReport, AISReconciliationResult, RegimeComparison } from './types';

export const filingApi = {
  // Filing list
  list: (params: { assessmentYear?: string; stage?: string; itrForm?: string; page?: number; size?: number }) =>
    api.get<PageResponse<any>>('/filing', { params }).then(r => r.data),

  // Load saved ITR data for a client
  getITRData: (clientId: number, assessmentYear: string) =>
    api.get<ITRFormData>(`/filing/${clientId}/${assessmentYear}`).then(r => r.data),

  // Save draft
  saveDraft: (clientId: number, data: ITRFormData) =>
    api.put<ITRFormData>(`/filing/${clientId}/draft`, data).then(r => r.data),

  // Submit ITR-1
  submitITR1: (data: ITRFormData) =>
    api.post<FilingResult>('/filing/itr1', data).then(r => r.data),

  // Submit ITR-2
  submitITR2: (data: ITRFormData) =>
    api.post<FilingResult>('/filing/itr2', data).then(r => r.data),

  // Submit ITR-3 (async)
  submitITR3: (data: ITRFormData) =>
    api.post<{ jobId: number; status: string; message: string }>('/filing/async/itr3', data).then(r => r.data),

  // Submit ITR-4 (async)
  submitITR4: (data: ITRFormData) =>
    api.post<{ jobId: number; status: string; message: string }>('/filing/async/itr4', data).then(r => r.data),

  // Poll async filing status
  getAsyncStatus: (jobId: number) =>
    api.get<{ jobId: number; status: string; result?: FilingResult }>(`/filing/async/status/${jobId}`).then(r => r.data),

  // Validate without submitting
  validate: (data: ITRFormData) =>
    api.post<ValidationResult>('/filing/validate', data).then(r => r.data),

  // Tax computation
  compute: (data: ITRFormData) =>
    api.post<{ taxComputation: any; regimeComparison: RegimeComparison }>('/filing/compute', data).then(r => r.data),

  // Regime recommendation
  getRegimeRecommendation: (data: ITRFormData) =>
    api.post<RegimeComparison>('/filing/regime-recommendation', data).then(r => r.data),

  // TDS mismatch analysis
  getTDSMismatch: (clientId: number, assessmentYear: string) =>
    api.get<TDSMismatchReport>(`/filing/${clientId}/${assessmentYear}/tds-mismatch`).then(r => r.data),

  // AIS reconciliation
  reconcileAIS: (clientId: number, assessmentYear: string) =>
    api.post<AISReconciliationResult>(`/filing/${clientId}/${assessmentYear}/reconcile`).then(r => r.data),

  // Export JSON for ITD upload
  exportJSON: (clientId: number, assessmentYear: string) =>
    api.get(`/filing/${clientId}/${assessmentYear}/export-json`, { responseType: 'blob' }).then(r => r.data),

  // Download ITR-V PDF
  downloadITRV: (clientId: number, assessmentYear: string) =>
    api.get(`/filing/${clientId}/${assessmentYear}/itrv`, { responseType: 'blob' }).then(r => r.data),

  // Form 16 upload (PDF OCR)
  uploadForm16: (clientId: number, partA: File, partB: File) => {
    const fd = new FormData();
    fd.append('partA', partA);
    fd.append('partB', partB);
    return api.post(`/import/form16/${clientId}`, fd, { headers: { 'Content-Type': 'multipart/form-data' } }).then(r => r.data);
  },

  // E-verification
  initiateEVC: (pan: string, mode: string) =>
    api.post('/evc/initiate', { pan, mode }).then(r => r.data),

  confirmEVC: (pan: string, transactionId: string, otp: string, ackNo: string) =>
    api.post('/evc/confirm', { pan, transactionId, otp, acknowledgementNumber: ackNo }).then(r => r.data),

  // Revised return
  submitRevisedReturn: (clientId: number, data: ITRFormData & { originalAckNo: string; originalFilingDate: string }) =>
    api.post(`/filing/${clientId}/revised`, data).then(r => r.data),

  // Rectification u/s 154
  submitRectification: (clientId: number, data: { originalAckNo: string; reason: string; correctedData: ITRFormData }) =>
    api.post(`/filing/${clientId}/rectification`, data).then(r => r.data),
};
```

---

# PHASE 3 — COMPUTATION WINDOW (COMPLETE ITR FILING FORM)

**Deliverable:** Full computation window with all income schedules for ITR-1/2/3/4, real-time tax computation, regime comparison, CBDT validation display, TDS mismatch panel, e-verification modal.
**Gate:** Opening any client's computation shows correct data. Tax recomputes on every field change (debounced 800ms). CAT-A errors block submit. JSON export downloads valid file.

---

## 3.1 Computation Window Architecture

The computation window renders as a **full-page overlay** triggered from `useAppStore().computationClientId`. It has a 3-column layout:
- **Left (320px):** Accordion navigation for income schedule sections
- **Center (flex-1):** Active schedule form with all fields
- **Right (360px):** Live computation summary + pre-submission checklist + action buttons

```
┌─────────────────────────────────────────────────────────────┐
│  ← Back  |  Ramesh Verma  DEFPV1122K  |  ITR-2  AY 2025-26 │ ← Top bar (62px)
├──────────┬──────────────────────────────┬───────────────────┤
│  NAV     │  FORM (scrollable)           │  LIVE SUMMARY     │
│  320px   │  flex-1                      │  360px            │
│          │                              │                   │
│ ○ Personal Details                      │  Gross Income     │
│ ● Schedule: Salary                      │  ₹18,42,000       │
│ ○ Schedule: HP                          │  ─────────────    │
│ ○ Schedule: Capital Gains               │  Deductions       │
│ ○ Schedule: VDA                         │  ₹2,00,000        │
│ ○ Schedule: Other Sources               │  ─────────────    │
│ ○ Chapter VI-A Deductions               │  Net Taxable      │
│ ○ TDS & Tax Paid                        │  ₹16,42,000       │
│ ○ Loss Set-Off (CYLA/BFLA)              │  ─────────────    │
│ ○ Pre-Submission Checklist              │  Tax (New)        │
│ ○ Foreign Assets (FA)                   │  ₹2,54,300        │
│   [ITR-3 only]                          │  Tax (Old)        │
│ ○ Schedule BP                           │  ₹3,12,000        │
│ ○ Schedule AMT                          │  ─────────────    │
│ ○ E-Verification                        │  ✅ New Regime     │
│                                         │  saves ₹57,700    │
│                                         │  ─────────────    │
│                                         │  CBDT Validation  │
│                                         │  ● 0 CAT-A Errors │
│                                         │  ▲ 2 Warnings     │
│                                         │  ─────────────    │
│                                         │  [Save Draft]     │
│                                         │  [Validate Only]  │
│                                         │  [Submit ITR]     │
│                                         │  [Export JSON]    │
└──────────┴──────────────────────────────┴───────────────────┘
```

## 3.2 `src/pages/Computation.tsx` — Master controller

```tsx
import { useState, useCallback, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useDebounce } from '../hooks/useDebounce';
import { filingApi } from '../api/filing';
import { ITRFormData } from '../api/types';
import { useAppStore } from '../store/useAppStore';
import toast from 'react-hot-toast';

// Section components
import PersonalDetails from '../components/computation/PersonalDetails';
import ScheduleSalaryForm from '../components/computation/ScheduleSalaryForm';
import ScheduleHPForm from '../components/computation/ScheduleHPForm';
import ScheduleCGForm from '../components/computation/ScheduleCGForm';
import ScheduleVDAForm from '../components/computation/ScheduleVDAForm';
import ScheduleOSForm from '../components/computation/ScheduleOSForm';
import ScheduleBPForm from '../components/computation/ScheduleBPForm';
import DeductionsForm from '../components/computation/DeductionsForm';
import TDSTaxPaidForm from '../components/computation/TDSTaxPaidForm';
import LossSetOffForm from '../components/computation/LossSetOffForm';
import PresumptiveForm from '../components/computation/PresumptiveForm';
import ForeignAssetsForm from '../components/computation/ForeignAssetsForm';
import AMTForm from '../components/computation/AMTForm';
import LiveSummaryPanel from '../components/computation/LiveSummaryPanel';
import EVCModal from '../components/computation/EVCModal';
import TDSMismatchPanel from '../components/computation/TDSMismatchPanel';
import Button from '../components/ui/Button';
import Badge from '../components/ui/Badge';

type Section =
  | 'personal' | 'salary' | 'hp' | 'cg' | 'vda' | 'os'
  | 'bp' | 'presumptive' | 'deductions' | 'tds'
  | 'loss' | 'foreign' | 'amt';

export default function Computation() {
  const { computationClientId, closeComputation, activeAY } = useAppStore();
  if (!computationClientId) return null;

  const qc = useQueryClient();
  const [activeSection, setActiveSection] = useState<Section>('personal');
  const [formData, setFormData] = useState<ITRFormData>({
    pan: '', assessmentYear: activeAY, taxRegime: 'NEW',
    itrType: 'ITR-1', returnFilingSection: '139(1)',
    originalOrRevised: 'O', intermediaryCity: '',
  });
  const [showEVCModal, setShowEVCModal] = useState(false);
  const [filingResult, setFilingResult] = useState<any>(null);

  // Load existing data
  const { data: savedData, isLoading } = useQuery({
    queryKey: ['itr-data', computationClientId, activeAY],
    queryFn: () => filingApi.getITRData(computationClientId, activeAY),
    onSuccess: (data) => setFormData(data),
  });

  // Debounced compute on every change
  const debouncedFormData = useDebounce(formData, 800);
  const { data: computedResult } = useQuery({
    queryKey: ['compute', debouncedFormData],
    queryFn: () => filingApi.compute(debouncedFormData),
    enabled: !!debouncedFormData.pan,
  });

  // Validation
  const { data: validationResult } = useQuery({
    queryKey: ['validate', debouncedFormData],
    queryFn: () => filingApi.validate(debouncedFormData),
    enabled: !!debouncedFormData.pan,
  });

  // Save draft mutation
  const saveDraftMutation = useMutation({
    mutationFn: () => filingApi.saveDraft(computationClientId, formData),
    onSuccess: () => toast.success('Draft saved'),
    onError: () => toast.error('Save failed'),
  });

  // Submit mutation
  const submitMutation = useMutation({
    mutationFn: () => {
      const enriched = { ...formData, taxComputation: computedResult?.taxComputation };
      switch (formData.itrType) {
        case 'ITR-1': return filingApi.submitITR1(enriched);
        case 'ITR-2': return filingApi.submitITR2(enriched);
        case 'ITR-3': return filingApi.submitITR3(enriched);
        case 'ITR-4': return filingApi.submitITR4(enriched);
        default:      return filingApi.submitITR1(enriched);
      }
    },
    onSuccess: (result: any) => {
      setFilingResult(result);
      if (result.acknowledgementNumber) {
        toast.success(`ITR filed! ACK: ${result.acknowledgementNumber}`);
        setShowEVCModal(true);
      } else if (result.jobId) {
        toast.success(`ITR submitted for async processing. Job ID: ${result.jobId}`);
      }
      qc.invalidateQueries({ queryKey: ['filing-list'] });
    },
    onError: (err: any) => toast.error(err?.response?.data?.message ?? 'Filing failed'),
  });

  // Export JSON
  async function handleExportJSON() {
    const blob = await filingApi.exportJSON(computationClientId, activeAY);
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `ITR_${formData.pan}_${activeAY}.json`;
    a.click();
    URL.revokeObjectURL(url);
  }

  const updateField = useCallback((path: string, value: any) => {
    setFormData(prev => {
      const parts = path.split('.');
      const updated = { ...prev };
      let current: any = updated;
      for (let i = 0; i < parts.length - 1; i++) {
        current[parts[i]] = { ...current[parts[i]] };
        current = current[parts[i]];
      }
      current[parts[parts.length - 1]] = value;
      return updated;
    });
  }, []);

  // Nav items based on ITR type
  const navItems = buildNavItems(formData.itrType);
  const catAErrors  = validationResult?.catAErrors ?? [];
  const catBWarnings = validationResult?.catBWarnings ?? [];
  const hasBlocker  = catAErrors.length > 0;

  return (
    <div className="fixed inset-0 z-[300] bg-bg flex flex-col">
      {/* Computation topbar */}
      <div className="h-[62px] bg-bg-card border-b border-border flex items-center px-6 gap-4 flex-shrink-0">
        <button onClick={closeComputation} className="flex items-center gap-2 text-[13px] font-medium text-text-secondary hover:text-text-primary transition-colors">
          <svg width="14" height="14" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="2"><path d="M12 18l-6-6 6-6"/></svg>
          Back
        </button>
        <div className="h-5 w-px bg-border" />
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 bg-navy rounded-lg flex items-center justify-center font-serif text-[14px] text-gold-light font-semibold">
            {savedData?.pan?.slice(0, 2) ?? '??'}
          </div>
          <div>
            <div className="text-[14px] font-semibold">{savedData?.pan ?? 'Loading…'}</div>
            <div className="font-mono text-[11.5px] text-text-muted">{formData.pan}</div>
          </div>
        </div>
        <div className="flex items-center gap-2 ml-4">
          <Badge variant="navy">{formData.itrType}</Badge>
          <Badge variant="muted">AY {formData.assessmentYear}</Badge>
          <Badge variant={formData.taxRegime === 'NEW' ? 'info' : 'gold'}>{formData.taxRegime === 'NEW' ? 'New Regime' : 'Old Regime'}</Badge>
          {formData.originalOrRevised === 'R' && <Badge variant="warning">Revised Return</Badge>}
        </div>
        <div className="ml-auto flex items-center gap-2.5">
          {catAErrors.length > 0 && <Badge variant="danger">⛔ {catAErrors.length} CAT-A Errors</Badge>}
          {catBWarnings.length > 0 && <Badge variant="warning">⚠ {catBWarnings.length} Warnings</Badge>}
          {catAErrors.length === 0 && catBWarnings.length === 0 && validationResult && (
            <Badge variant="success">✓ Validation Passed</Badge>
          )}
          <Button variant="ghost" size="sm" onClick={() => saveDraftMutation.mutate()}>Save Draft</Button>
          <Button
            variant="primary" size="sm"
            disabled={hasBlocker || submitMutation.isPending}
            onClick={() => submitMutation.mutate()}
          >
            {submitMutation.isPending ? 'Filing…' : 'Submit ITR'}
          </Button>
        </div>
      </div>

      {/* 3-column body */}
      <div className="flex flex-1 overflow-hidden">
        {/* Left nav */}
        <nav className="w-[260px] flex-shrink-0 bg-bg-card border-r border-border overflow-y-auto py-4 px-2.5">
          {navItems.map((section) => (
            <button
              key={section.id}
              onClick={() => setActiveSection(section.id as Section)}
              className={`relative w-full text-left flex items-center gap-2 px-3 py-2.5 rounded-lg text-[13.5px] mb-0.5 transition-all ${
                activeSection === section.id
                  ? 'bg-gold/[0.12] text-[#92640A] font-medium'
                  : 'text-text-secondary hover:bg-bg hover:text-text-primary'}`}
            >
              {activeSection === section.id && (
                <span className="absolute left-0 top-1/2 -translate-y-1/2 w-[3px] h-5 bg-gold rounded-r-sm" />
              )}
              <span className="text-[16px]">{section.icon}</span>
              <span>{section.label}</span>
              {section.hasError && <span className="ml-auto w-2 h-2 bg-danger rounded-full" />}
            </button>
          ))}
        </nav>

        {/* Center form */}
        <div className="flex-1 overflow-y-auto p-7">
          {isLoading ? (
            <div className="space-y-4">
              {Array.from({ length: 6 }).map((_, i) => <div key={i} className="h-12 bg-border rounded-lg animate-pulse" />)}
            </div>
          ) : (
            <>
              {activeSection === 'personal'     && <PersonalDetails formData={formData} onChange={updateField} />}
              {activeSection === 'salary'       && <ScheduleSalaryForm formData={formData} onChange={updateField} />}
              {activeSection === 'hp'           && <ScheduleHPForm formData={formData} onChange={updateField} />}
              {activeSection === 'cg'           && <ScheduleCGForm formData={formData} onChange={updateField} />}
              {activeSection === 'vda'          && <ScheduleVDAForm formData={formData} onChange={updateField} />}
              {activeSection === 'os'           && <ScheduleOSForm formData={formData} onChange={updateField} />}
              {activeSection === 'bp'           && <ScheduleBPForm formData={formData} onChange={updateField} />}
              {activeSection === 'presumptive'  && <PresumptiveForm formData={formData} onChange={updateField} />}
              {activeSection === 'deductions'   && <DeductionsForm formData={formData} onChange={updateField} />}
              {activeSection === 'tds'          && <TDSTaxPaidForm formData={formData} onChange={updateField} clientId={computationClientId} />}
              {activeSection === 'loss'         && <LossSetOffForm formData={formData} onChange={updateField} />}
              {activeSection === 'foreign'      && <ForeignAssetsForm formData={formData} onChange={updateField} />}
              {activeSection === 'amt'          && <AMTForm formData={formData} computed={computedResult} />}
            </>
          )}

          {/* Validation errors inline */}
          {(catAErrors.length > 0 || catBWarnings.length > 0) && (
            <div className="mt-6 space-y-2">
              {catAErrors.map((err) => (
                <div key={err.ruleId} className="flex items-start gap-3 p-3 bg-danger-bg border border-[#FECACA] rounded-lg">
                  <span className="text-danger font-bold flex-shrink-0">⛔</span>
                  <div>
                    <div className="text-[12px] font-bold text-danger">[CAT-A] {err.ruleId} — {err.field}</div>
                    <div className="text-[13px] text-danger">{err.message}</div>
                  </div>
                </div>
              ))}
              {catBWarnings.map((w) => (
                <div key={w.ruleId} className="flex items-start gap-3 p-3 bg-warning-bg border border-[#FDE68A] rounded-lg">
                  <span className="text-warning font-bold flex-shrink-0">⚠</span>
                  <div>
                    <div className="text-[12px] font-bold text-warning">[CAT-B] {w.ruleId} — {w.field}</div>
                    <div className="text-[13px] text-warning">{w.message}</div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Right summary */}
        <LiveSummaryPanel
          formData={formData}
          computed={computedResult}
          validation={validationResult}
          onSubmit={() => submitMutation.mutate()}
          onSaveDraft={() => saveDraftMutation.mutate()}
          onExportJSON={handleExportJSON}
          onDownloadITRV={() => filingApi.downloadITRV(computationClientId, activeAY)}
          onEVerify={() => setShowEVCModal(true)}
          isSubmitting={submitMutation.isPending}
          hasBlocker={hasBlocker}
          clientId={computationClientId}
          assessmentYear={activeAY}
        />
      </div>

      {/* EVC Modal */}
      {showEVCModal && filingResult && (
        <EVCModal
          pan={formData.pan}
          ackNo={filingResult.acknowledgementNumber}
          onClose={() => setShowEVCModal(false)}
        />
      )}
    </div>
  );
}

function buildNavItems(itrType: string) {
  const base = [
    { id: 'personal',    icon: '👤', label: 'Personal Details' },
    { id: 'salary',      icon: '💼', label: 'Schedule: Salary' },
    { id: 'hp',          icon: '🏠', label: 'Schedule: House Property' },
    { id: 'cg',          icon: '📈', label: 'Schedule: Capital Gains' },
    { id: 'vda',         icon: '₿',  label: 'Schedule: VDA / Crypto' },
    { id: 'os',          icon: '💰', label: 'Schedule: Other Sources' },
    { id: 'deductions',  icon: '🧾', label: 'Chapter VI-A Deductions' },
    { id: 'tds',         icon: '🏦', label: 'TDS & Tax Paid' },
    { id: 'loss',        icon: '📉', label: 'Loss Set-Off (CYLA/BFLA)' },
    { id: 'foreign',     icon: '🌐', label: 'Schedule FA — Foreign Assets' },
  ];
  if (itrType === 'ITR-3') {
    base.push({ id: 'bp', icon: '🏭', label: 'Schedule BP — Business' });
    base.push({ id: 'amt', icon: '⚖', label: 'Schedule AMT' });
  }
  if (itrType === 'ITR-4') {
    base.push({ id: 'presumptive', icon: '📊', label: 'Presumptive Income' });
  }
  return base;
}
```

## 3.3 `src/components/computation/LiveSummaryPanel.tsx`

```tsx
import { ITRFormData, ValidationResult, RegimeComparison } from '../../api/types';
import Button from '../ui/Button';
import Badge from '../ui/Badge';
import ProgressBar from '../ui/ProgressBar';

interface Props {
  formData: ITRFormData;
  computed: any;
  validation: ValidationResult | undefined;
  onSubmit: () => void;
  onSaveDraft: () => void;
  onExportJSON: () => void;
  onDownloadITRV: () => void;
  onEVerify: () => void;
  isSubmitting: boolean;
  hasBlocker: boolean;
  clientId: number;
  assessmentYear: string;
}

function FmtAmount({ value, negative }: { value: number; negative?: boolean }) {
  if (!value) return <span className="text-text-muted">—</span>;
  return (
    <span className={`font-serif text-[17px] font-semibold ${negative ? 'text-danger' : 'text-text-primary'}`}>
      ₹{value.toLocaleString('en-IN')}
    </span>
  );
}

export default function LiveSummaryPanel({ formData, computed, validation, onSubmit, onSaveDraft, onExportJSON, onEVerify, isSubmitting, hasBlocker }: Props) {
  const tax = computed?.taxComputation;
  const regime = computed?.regimeComparison as RegimeComparison | undefined;

  const summaryRows = [
    { label: 'Gross Salary',          value: formData.scheduleSalary?.grossSalary ?? 0 },
    { label: 'House Property',        value: formData.scheduleHP?.[0]?.incomeFromHP ?? 0 },
    { label: 'Capital Gains',         value: formData.scheduleCG?.totalCG ?? 0 },
    { label: 'VDA / Crypto',          value: formData.scheduleVDA?.vdaProfit ?? 0 },
    { label: 'Other Sources',         value: formData.scheduleOS?.totalOS ?? 0 },
    { label: 'Business / Profession', value: formData.scheduleBP?.businessIncome ?? 0 },
  ].filter(r => r.value !== 0);

  return (
    <aside className="w-[360px] flex-shrink-0 bg-bg-card border-l border-border overflow-y-auto flex flex-col">
      <div className="flex-1 p-5">
        {/* Income summary */}
        <div className="mb-5">
          <div className="text-[11px] font-bold uppercase tracking-[0.8px] text-text-muted mb-3 pb-2 border-b border-border">
            Income Summary
          </div>
          {summaryRows.map(row => (
            <div key={row.label} className="flex justify-between items-center py-1.5 text-[12.5px]">
              <span className="text-text-secondary">{row.label}</span>
              <span className="font-medium">₹{row.value.toLocaleString('en-IN')}</span>
            </div>
          ))}
          <div className="flex justify-between items-center py-2 border-t border-border mt-1 font-semibold">
            <span>Gross Total Income</span>
            <FmtAmount value={tax?.totalIncome ?? 0} />
          </div>
          <div className="flex justify-between items-center py-1 text-success text-[12.5px]">
            <span>Total Deductions (VI-A)</span>
            <span>−₹{(formData.deductions?.totalDeductions ?? 0).toLocaleString('en-IN')}</span>
          </div>
          <div className="flex justify-between items-center py-2 border-t border-border mt-1 font-bold text-[14px]">
            <span>Net Taxable Income</span>
            <span className="font-serif text-[18px]">₹{((tax?.totalIncome ?? 0) - (formData.deductions?.totalDeductions ?? 0)).toLocaleString('en-IN')}</span>
          </div>
        </div>

        {/* Tax computation */}
        {tax && (
          <div className="mb-5">
            <div className="text-[11px] font-bold uppercase tracking-[0.8px] text-text-muted mb-3 pb-2 border-b border-border">
              Tax Computation ({formData.taxRegime === 'NEW' ? 'New' : 'Old'} Regime)
            </div>
            {[
              { label: 'Tax on Total Income', value: tax.taxOnTotalIncome },
              { label: 'Rebate u/s 87A',      value: -tax.rebate87A },
              { label: 'Surcharge',            value: tax.surcharge },
              { label: 'Health & Ed. Cess 4%', value: tax.healthEducationCess },
              { label: 'AMT Liability',        value: tax.amtLiability },
            ].filter(r => r.value !== 0).map(row => (
              <div key={row.label} className="flex justify-between items-center py-1.5 text-[12.5px]">
                <span className="text-text-secondary">{row.label}</span>
                <span className={row.value < 0 ? 'text-success font-medium' : 'font-medium'}>
                  {row.value < 0 ? '−' : ''}₹{Math.abs(row.value).toLocaleString('en-IN')}
                </span>
              </div>
            ))}
            <div className="flex justify-between items-center py-2 border-t border-border mt-1 font-bold">
              <span>Gross Tax Liability</span>
              <FmtAmount value={tax.grossTaxLiability} negative />
            </div>
            <div className="flex justify-between items-center py-1 text-success text-[12.5px]">
              <span>TDS + Advance Tax</span>
              <span>−₹{tax.totalTaxPaid.toLocaleString('en-IN')}</span>
            </div>
            <div className={`flex justify-between items-center py-2 border-t border-border mt-1 font-bold text-[14px] ${tax.refundDue > 0 ? 'text-accent-teal' : 'text-danger'}`}>
              <span>{tax.refundDue > 0 ? 'Refund Due' : 'Tax Payable'}</span>
              <span className="font-serif text-[18px]">
                {tax.refundDue > 0 ? '₹' + tax.refundDue.toLocaleString('en-IN') : '₹' + tax.taxPayable.toLocaleString('en-IN')}
              </span>
            </div>
          </div>
        )}

        {/* Regime comparison */}
        {regime && (
          <div className={`mb-5 rounded-lg p-4 border ${regime.recommendedRegime === 'NEW' ? 'bg-info-bg border-[#BFDBFE]' : 'bg-gold-pale border-[#FDE68A]'}`}>
            <div className="text-[11px] font-bold uppercase tracking-[0.8px] text-text-muted mb-2">
              Regime Recommendation
            </div>
            <div className="flex items-center justify-between mb-2">
              <span className={`text-[13.5px] font-semibold ${regime.recommendedRegime === 'NEW' ? 'text-info' : 'text-[#92640A]'}`}>
                ✅ {regime.recommendedRegime} Regime Recommended
              </span>
              <span className="text-[12px] text-text-muted">{regime.confidenceScore}% confidence</span>
            </div>
            <ProgressBar value={regime.confidenceScore} color={regime.recommendedRegime === 'NEW' ? 'blue' : 'gold'} className="mb-2" />
            <div className="text-[12px] text-text-secondary">
              Saves <strong className="text-text-primary">₹{regime.saving.toLocaleString('en-IN')}</strong> vs {regime.recommendedRegime === 'NEW' ? 'old' : 'new'} regime
            </div>
            <div className="text-[12px] text-text-muted mt-1">{regime.primaryReason}</div>
            {regime.warnings.map((w, i) => (
              <div key={i} className="text-[11.5px] text-warning mt-1.5">{w}</div>
            ))}
          </div>
        )}

        {/* CBDT Validation */}
        {validation && (
          <div className="mb-5">
            <div className="text-[11px] font-bold uppercase tracking-[0.8px] text-text-muted mb-3 pb-2 border-b border-border">
              CBDT Validation Status
            </div>
            <div className={`flex items-center gap-2 p-3 rounded-lg mb-2 ${validation.catAErrors.length > 0 ? 'bg-danger-bg' : 'bg-success-bg'}`}>
              <span className="text-lg">{validation.catAErrors.length > 0 ? '⛔' : '✅'}</span>
              <div>
                <div className={`text-[12px] font-bold ${validation.catAErrors.length > 0 ? 'text-danger' : 'text-success'}`}>
                  CAT-A (Hard Stop): {validation.catAErrors.length} errors
                </div>
                {validation.catAErrors.length > 0 && (
                  <div className="text-[11.5px] text-danger">Filing blocked until resolved</div>
                )}
              </div>
            </div>
            {validation.catBWarnings.length > 0 && (
              <div className="flex items-center gap-2 p-3 rounded-lg bg-warning-bg">
                <span className="text-lg">⚠</span>
                <div>
                  <div className="text-[12px] font-bold text-warning">CAT-B Warnings: {validation.catBWarnings.length}</div>
                  <div className="text-[11.5px] text-warning">May cause defective return notice</div>
                </div>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Action buttons — sticky bottom */}
      <div className="p-5 border-t border-border flex-shrink-0 space-y-2">
        <Button
          variant="primary" className="w-full justify-center"
          disabled={hasBlocker || isSubmitting}
          onClick={onSubmit}
        >
          {isSubmitting ? '⏳ Filing…' : hasBlocker ? '⛔ Fix Errors First' : '✅ Submit ITR'}
        </Button>
        <Button variant="secondary" className="w-full justify-center" onClick={onSaveDraft}>
          💾 Save Draft
        </Button>
        <Button variant="ghost" className="w-full justify-center" onClick={onExportJSON}>
          📥 Export ITD JSON
        </Button>
        <Button variant="ghost" className="w-full justify-center" onClick={onEVerify}>
          🔐 E-Verify Return
        </Button>
      </div>
    </aside>
  );
}
```

## 3.4 All Schedule Form Components — IMPLEMENT EACH IN FULL

Each form component follows this exact pattern:

```tsx
// src/components/computation/ScheduleSalaryForm.tsx — EXAMPLE
import { ITRFormData, ScheduleSalary } from '../../api/types';

interface FormField { label: string; field: string; helpText?: string; readOnly?: boolean; }

const SALARY_FIELDS: FormField[] = [
  { label: 'Basic Salary',                       field: 'scheduleSalary.basicSalary' },
  { label: 'Dearness Allowance',                 field: 'scheduleSalary.dearnessAllowance' },
  { label: 'House Rent Allowance (HRA)',          field: 'scheduleSalary.houseRentAllowance' },
  { label: 'Leave Travel Allowance (LTA)',        field: 'scheduleSalary.leaveTravelAllowance' },
  { label: 'Leave Encashment',                   field: 'scheduleSalary.leaveEncashment' },
  { label: 'Gratuity',                           field: 'scheduleSalary.gratuity' },
  { label: 'Bonus',                              field: 'scheduleSalary.bonus' },
  { label: 'Commission',                         field: 'scheduleSalary.commission' },
  { label: 'Arrears of Salary',                  field: 'scheduleSalary.arrearsOfSalary' },
  { label: 'Other Allowances',                   field: 'scheduleSalary.otherAllowances' },
  { label: 'Total u/s 17(1) [AUTO]',             field: 'scheduleSalary.total17_1', readOnly: true },
  { label: 'Total Perquisites u/s 17(2)',         field: 'scheduleSalary.totalPerquisites17_2' },
  { label: 'Total Profits in Lieu u/s 17(3)',     field: 'scheduleSalary.totalProfitsInLieu17_3' },
  { label: 'Gross Salary [AUTO]',                field: 'scheduleSalary.grossSalary', readOnly: true },
  { label: 'HRA Exemption u/s 10(13A)',           field: 'scheduleSalary.hraExemption10_13A',        helpText: 'Auto-computed from HRA, rent paid, metro/non-metro' },
  { label: 'LTA Exemption u/s 10(5)',             field: 'scheduleSalary.ltaExemption10_5' },
  { label: 'Gratuity Exemption u/s 10(10)',       field: 'scheduleSalary.gratuityExemption10_10' },
  { label: 'Leave Encashment Exemption 10(10AA)', field: 'scheduleSalary.leaveEncashmentExemption10_10AA' },
  { label: 'Total Exemptions u/s 10 [AUTO]',     field: 'scheduleSalary.totalExemptionsUnder10', readOnly: true },
  { label: 'Standard Deduction (₹75,000 / ₹50,000)', field: 'scheduleSalary.standardDeduction', readOnly: true, helpText: '₹75,000 for AY 2026-27, ₹50,000 for AY 2025-26' },
  { label: 'Professional Tax',                   field: 'scheduleSalary.professionalTax' },
  { label: 'Entertainment Allowance',            field: 'scheduleSalary.entertainmentAllowance' },
  { label: 'Income from Salary (Taxable) [AUTO]',field: 'scheduleSalary.taxableIncomeSalary', readOnly: true },
];

export default function ScheduleSalaryForm({ formData, onChange }: { formData: ITRFormData; onChange: (path: string, value: any) => void }) {
  return (
    <div>
      <h2 className="font-serif text-[22px] font-semibold text-text-primary mb-6">Schedule: Salary Income</h2>
      <div className="bg-info-bg border border-[#BFDBFE] rounded-lg p-3 mb-5 text-[12.5px] text-info">
        ℹ All amounts in ₹ (Rupees). Read-only fields are auto-computed.
        Standard deduction: ₹75,000 (AY 2026-27 new regime), ₹50,000 (AY 2025-26).
      </div>
      <div className="grid grid-cols-2 gap-5">
        {SALARY_FIELDS.map(({ label, field, helpText, readOnly }) => {
          const val = field.split('.').reduce((obj, key) => obj?.[key], formData as any) ?? '';
          return (
            <div key={field}>
              <label className="block text-[12.5px] font-semibold text-text-secondary uppercase tracking-[0.4px] mb-1.5">
                {label}
              </label>
              <input
                type="number"
                value={val}
                readOnly={readOnly}
                onChange={e => !readOnly && onChange(field, Number(e.target.value))}
                className={`w-full px-3 py-[9px] bg-bg-card border border-border-strong rounded-lg text-[13.5px] font-mono outline-none transition-colors
                  ${readOnly
                    ? 'bg-bg text-text-secondary cursor-not-allowed border-border'
                    : 'focus:border-gold'}`}
                placeholder="0"
              />
              {helpText && <p className="text-[11px] text-text-muted mt-1">{helpText}</p>}
            </div>
          );
        })}
      </div>

      {/* Employer details */}
      <div className="mt-8 border-t border-border pt-6">
        <h3 className="text-[15px] font-semibold mb-4">Employer TDS Details</h3>
        <div className="grid grid-cols-2 gap-5">
          <div>
            <label className="block text-[12.5px] font-semibold text-text-secondary uppercase tracking-[0.4px] mb-1.5">
              Employer Name
            </label>
            <input type="text" onChange={e => onChange('scheduleSalary.employerName', e.target.value)}
              className="w-full px-3 py-[9px] bg-bg-card border border-border-strong rounded-lg text-[13.5px] outline-none focus:border-gold" />
          </div>
          <div>
            <label className="block text-[12.5px] font-semibold text-text-secondary uppercase tracking-[0.4px] mb-1.5">
              Employer TAN
            </label>
            <input type="text" maxLength={10} onChange={e => onChange('scheduleSalary.employerTan', e.target.value.toUpperCase())}
              placeholder="PUNE12345A"
              className="w-full px-3 py-[9px] bg-bg-card border border-border-strong rounded-lg text-[13.5px] font-mono outline-none focus:border-gold" />
          </div>
          <div>
            <label className="block text-[12.5px] font-semibold text-text-secondary uppercase tracking-[0.4px] mb-1.5">
              Nature of Employment
            </label>
            <select onChange={e => onChange('scheduleSalary.natureOfEmployment', e.target.value)}
              className="w-full px-3 py-[9px] bg-bg-card border border-border-strong rounded-lg text-[13.5px] outline-none focus:border-gold cursor-pointer">
              {['Central Govt', 'State Govt', 'PSU', 'Pensioner', 'Others'].map(o => <option key={o}>{o}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-[12.5px] font-semibold text-text-secondary uppercase tracking-[0.4px] mb-1.5">
              TDS Deducted (₹)
            </label>
            <input type="number" onChange={e => onChange('scheduleSalary.tdsDeducted', Number(e.target.value))}
              className="w-full px-3 py-[9px] bg-bg-card border border-border-strong rounded-lg text-[13.5px] font-mono outline-none focus:border-gold" />
          </div>
        </div>
      </div>

      {/* Form 16 OCR Upload */}
      <div className="mt-6 border border-dashed border-border-strong rounded-lg p-5 text-center">
        <div className="text-[16px] mb-2">📄</div>
        <div className="text-[13px] font-medium mb-1">Upload Form 16 (Auto-fill via OCR)</div>
        <div className="text-[12px] text-text-muted mb-3">PDF — Part A (TRACES) and Part B (Employer)</div>
        <div className="flex justify-center gap-3">
          <label className="cursor-pointer">
            <input type="file" accept=".pdf" className="hidden" onChange={e => { /* call filingApi.uploadForm16 */ }} />
            <span className="px-3 py-1.5 text-[12px] bg-bg border border-border-strong rounded-lg text-text-secondary hover:bg-border cursor-pointer">Upload Part A</span>
          </label>
          <label className="cursor-pointer">
            <input type="file" accept=".pdf" className="hidden" />
            <span className="px-3 py-1.5 text-[12px] bg-bg border border-border-strong rounded-lg text-text-secondary hover:bg-border cursor-pointer">Upload Part B</span>
          </label>
        </div>
      </div>
    </div>
  );
}
```

**Implement ALL remaining schedule forms using this exact pattern:**

- **`ScheduleHPForm.tsx`** — Fields: propertyType (radio), grossRentReceived, municipalTaxPaid, netAnnualValue (auto), standardDeduction30Pct (auto, 30%), interestOnHousingLoan (cap: ₹2L self-occ / ₹30K pre-2000 / unlimited let-out), priorPeriodInterest, incomeFromHP (auto). Add/remove properties via "+ Add Property" button.

- **`ScheduleCGForm.tsx`** — Sections: STCG (111A at 15%, other at slab), LTCG (112A above ₹1.25L at 10%, LTCG other at 20%). Fields for each: saleConsideration, costOfAcquisition, indexedCost, periodOfHolding, exemptionUnder54/54EC. Include grandfathering FMV input for 112A.

- **`ScheduleVDAForm.tsx`** — VDA transactions list (add multiple). Per row: assetName, purchaseDate, purchaseCost, saleDate, saleConsideration, tds194S. Summary: totalVDAProfit, tds194STotal, vdaLossNote (always zero — non-adjustable per §115BBH).

- **`ScheduleOSForm.tsx`** — Fields: savingsInterest, fdInterest, dividendIncome, familyPension (standard deduction 1/3rd or ₹15K), winningsLottery (30% flat), otherIncome. totalOS (auto).

- **`ScheduleBPForm.tsx`** (ITR-3 only) — All Schedule BP fields from `ITR_Complete_Part2.md §6.2`. Add/Subtract items. Section 43B panel with MSME payment tracking. Partner remuneration 40(b) calculator. F&O turnover input.

- **`PresumptiveForm.tsx`** (ITR-4 only) — Radio: 44AD / 44ADA / 44AE. 44AD: grossTurnover, digitalTurnover, cashTurnover, presumptiveIncome (auto: 8%/6% digital). 44ADA: grossReceipts, 50% presumptive. 44AE: per-vehicle entry table.

- **`DeductionsForm.tsx`** — All VI-A sections. 80C cluster (cap ₹1.5L auto-enforced with live counter). 80CCD(1B) NPS additional ₹50K. 80CCD(2) employer NPS — no cap. 80D with premium grid (self/family/parents, senior/non-senior, preventive checkup ₹5K sublimit). 80G: donation table (qualifying %, limit).

- **`TDSTaxPaidForm.tsx`** — Multiple TDS entries (salary/other). Per-entry: deductorName, TAN, amountPaid, tdsAmount, status. Button to **Auto-import from 26AS** (calls `filingApi.reconcileAIS`). Display TDS mismatch report inline. Advance tax / SAT challan table: BSR code, challan no, date, amount.

- **`LossSetOffForm.tsx`** — CYLA/BFLA matrix following IT Act §70–80. HP loss carryforward, business loss, speculative loss (4-year), STCG/LTCG loss. Display remaining losses after set-off. Show 8-year expiry tracking.

- **`ForeignAssetsForm.tsx`** — Parts A–G of Schedule FA. Bank account table (country, bank, SWIFT, peak value, interest). Immovable property table. Foreign income table with DTAA fields.

- **`AMTForm.tsx`** (ITR-3 only) — Read-only AMT computation display from `computedResult.amtResult`. Show: ATI, AMT rate 18.5%, AMT before surcharge, total AMT, regular tax, which is higher, AMT credit available.

## 3.5 `src/components/computation/EVCModal.tsx`

```tsx
import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { filingApi } from '../../api/filing';
import Button from '../ui/Button';
import toast from 'react-hot-toast';

interface Props { pan: string; ackNo: string; onClose: () => void; }

type EVCMode = 'AADHAAR_OTP' | 'NET_BANKING' | 'BANK_ACCOUNT' | 'DSC';

export default function EVCModal({ pan, ackNo, onClose }: Props) {
  const [mode, setMode] = useState<EVCMode>('AADHAAR_OTP');
  const [step, setStep] = useState<'select' | 'otp' | 'done'>('select');
  const [transactionId, setTransactionId] = useState('');
  const [otp, setOtp] = useState('');

  const initiateMutation = useMutation({
    mutationFn: () => filingApi.initiateEVC(pan, mode),
    onSuccess: (data: any) => {
      setTransactionId(data.transactionId);
      setStep('otp');
      toast.success('OTP sent to Aadhaar-linked mobile');
    },
    onError: () => toast.error('OTP initiation failed'),
  });

  const confirmMutation = useMutation({
    mutationFn: () => filingApi.confirmEVC(pan, transactionId, otp, ackNo),
    onSuccess: () => { setStep('done'); toast.success('e-Verification successful!'); },
    onError: () => toast.error('Invalid OTP — please retry'),
  });

  return (
    <div className="fixed inset-0 z-[400] flex items-center justify-center">
      <div className="absolute inset-0 bg-black/40" onClick={onClose} />
      <div className="relative bg-bg-card rounded-[10px] border border-border p-7 w-[480px] shadow-xl">
        <div className="flex justify-between mb-5">
          <h2 className="font-serif text-[20px] font-semibold">e-Verify Return</h2>
          <button onClick={onClose} className="text-text-muted hover:text-text-primary">
            <svg width="16" height="16" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="2"><path d="M15 5 5 15M5 5l10 10"/></svg>
          </button>
        </div>

        <div className="text-[12.5px] text-text-muted mb-5">
          Acknowledgement No: <span className="font-mono font-semibold text-text-primary">{ackNo}</span>
        </div>

        {step === 'select' && (
          <>
            <p className="text-[13px] mb-4">Select verification method:</p>
            <div className="space-y-2 mb-5">
              {[
                { mode: 'AADHAAR_OTP', label: 'Aadhaar OTP (Recommended)', sub: 'OTP to Aadhaar-linked mobile' },
                { mode: 'NET_BANKING', label: 'Net Banking', sub: 'Redirect to bank portal' },
                { mode: 'BANK_ACCOUNT', label: 'Bank Account EVC', sub: 'Pre-validated bank account' },
                { mode: 'DSC', label: 'Digital Signature (DSC)', sub: 'Upload PKCS#7 signature' },
              ].map(({ mode: m, label, sub }) => (
                <label key={m} className={`flex items-start gap-3 p-3.5 rounded-lg border cursor-pointer transition-all ${mode === m ? 'border-gold bg-gold-pale' : 'border-border hover:border-border-strong'}`}>
                  <input type="radio" name="evc-mode" checked={mode === m} onChange={() => setMode(m as EVCMode)} className="mt-0.5" />
                  <div>
                    <div className="text-[13.5px] font-medium">{label}</div>
                    <div className="text-[12px] text-text-muted">{sub}</div>
                  </div>
                </label>
              ))}
            </div>
            <Button variant="primary" className="w-full justify-center" onClick={() => initiateMutation.mutate()} disabled={initiateMutation.isPending}>
              {initiateMutation.isPending ? 'Initiating…' : 'Proceed'}
            </Button>
          </>
        )}

        {step === 'otp' && (
          <>
            <p className="text-[13px] mb-4">Enter the 6-digit OTP sent to Aadhaar-linked mobile:</p>
            <input
              value={otp} onChange={e => setOtp(e.target.value.replace(/\D/g, '').slice(0, 6))}
              maxLength={6} placeholder="_ _ _ _ _ _"
              className="w-full text-center text-[24px] font-mono tracking-[8px] px-4 py-3 bg-bg border border-border-strong rounded-lg outline-none focus:border-gold mb-4"
            />
            <Button variant="primary" className="w-full justify-center" onClick={() => confirmMutation.mutate()} disabled={otp.length !== 6 || confirmMutation.isPending}>
              {confirmMutation.isPending ? 'Verifying…' : 'Verify OTP'}
            </Button>
            <Button variant="ghost" className="w-full justify-center mt-2" onClick={() => initiateMutation.mutate()}>Resend OTP</Button>
          </>
        )}

        {step === 'done' && (
          <div className="text-center py-4">
            <div className="text-[48px] mb-3">✅</div>
            <div className="font-serif text-[20px] font-semibold text-success mb-2">Verified!</div>
            <p className="text-[13px] text-text-muted mb-5">Return successfully e-verified via {mode.replace('_', ' ')}.</p>
            <Button variant="primary" className="w-full justify-center" onClick={onClose}>Done</Button>
          </div>
        )}
      </div>
    </div>
  );
}
```

## 3.6 `src/hooks/useDebounce.ts`

```typescript
import { useState, useEffect } from 'react';
export function useDebounce<T>(value: T, delay: number): T {
  const [debouncedValue, setDebouncedValue] = useState<T>(value);
  useEffect(() => {
    const handler = setTimeout(() => setDebouncedValue(value), delay);
    return () => clearTimeout(handler);
  }, [value, delay]);
  return debouncedValue;
}
```

---

# PHASE 4 — ALL REMAINING VIEWS + COMPLETE API LAYER

**Deliverable:** All 13 views fully functional with live backend data. Complete API service layer. React Query setup with global error handling. Production build passing.
**Gate:** `npm run build` zero errors, zero TypeScript errors. All views load with real data.

---

## 4.1 API Services — Complete All Remaining

### `src/api/reconciliation.ts`
```typescript
import api from './axios';
export const reconciliationApi = {
  list: (params?: any) => api.get('/reconciliation', { params }).then(r => r.data),
  getMismatchDetail: (clientId: number, ay: string) =>
    api.get(`/reconciliation/${clientId}/${ay}`).then(r => r.data),
  resolve: (mismatchId: number, notes: string) =>
    api.post(`/reconciliation/${mismatchId}/resolve`, { notes }).then(r => r.data),
  escalate: (mismatchId: number) =>
    api.post(`/reconciliation/${mismatchId}/escalate`).then(r => r.data),
  downloadReport: (mismatchId: number) =>
    api.get(`/reconciliation/${mismatchId}/report`, { responseType: 'blob' }).then(r => r.data),
};
```

### `src/api/sync.ts`
```typescript
import api from './axios';
export const syncApi = {
  triggerSync: (config: { scope: string; assessmentYear: string; documents: string; batchSize: number }) =>
    api.post('/sync/trigger', config).then(r => r.data),
  schedule: (config: any) => api.post('/sync/schedule', config).then(r => r.data),
  getHistory: (page = 0, size = 20) => api.get('/sync/history', { params: { page, size } }).then(r => r.data),
  stopJob: (jobId: number) => api.post(`/sync/${jobId}/stop`).then(r => r.data),
  getCaptchaStatus: () => api.get('/sync/captcha-status').then(r => r.data),
};
```

### `src/api/jobs.ts`
```typescript
import api from './axios';
import { BatchJob, PageResponse } from './types';
export const jobsApi = {
  list: (params?: any) => api.get<PageResponse<BatchJob>>('/jobs', { params }).then(r => r.data),
  get: (jobId: number) => api.get<BatchJob>(`/jobs/${jobId}`).then(r => r.data),
  stop: (jobId: number) => api.post(`/jobs/${jobId}/stop`).then(r => r.data),
  getAsyncStatus: (jobId: number) => api.get(`/filing/async/status/${jobId}`).then(r => r.data),
};
```

### `src/api/notices.ts`
```typescript
import api from './axios';
import { Notice } from './types';
export const noticesApi = {
  list: (params?: any) => api.get<Notice[]>('/notices', { params }).then(r => r.data),
  get: (id: number) => api.get<Notice>(`/notices/${id}`).then(r => r.data),
  update: (id: number, data: Partial<Notice>) => api.put<Notice>(`/notices/${id}`, data).then(r => r.data),
  addNote: (id: number, note: string) => api.post(`/notices/${id}/notes`, { note }).then(r => r.data),
  close: (id: number) => api.post(`/notices/${id}/close`).then(r => r.data),
  getDeadlines: (month: string) => api.get('/notices/deadlines', { params: { month } }).then(r => r.data),
};
```

### `src/api/billing.ts`
```typescript
import api from './axios';
import { Invoice } from './types';
export const billingApi = {
  list: (params?: any) => api.get('/billing/invoices', { params }).then(r => r.data),
  create: (data: Partial<Invoice>) => api.post<Invoice>('/billing/invoices', data).then(r => r.data),
  recordPayment: (id: string, amount: number) => api.post(`/billing/invoices/${id}/payment`, { amount }).then(r => r.data),
  sendWhatsApp: (id: string) => api.post(`/billing/invoices/${id}/whatsapp`).then(r => r.data),
  download: (id: string) => api.get(`/billing/invoices/${id}/pdf`, { responseType: 'blob' }).then(r => r.data),
  getStats: () => api.get('/billing/stats').then(r => r.data),
};
```

### `src/api/communication.ts`
```typescript
import api from './axios';
export const communicationApi = {
  broadcast: (template: string, recipients: string, schedule?: string) =>
    api.post('/communication/broadcast', { template, recipients, schedule }).then(r => r.data),
  getTemplates: () => api.get('/communication/templates').then(r => r.data),
  getHistory: () => api.get('/communication/history').then(r => r.data),
  getChannelStatus: () => api.get('/communication/channels').then(r => r.data),
};
```

## 4.2 Remaining Pages — Implement with exact visual replica

### `src/pages/Reconciliation.tsx`

3-stat header (218 mismatches, 18 under review, 34 resolved). 2-column grid: left = filterable mismatch table (client, type badge, difference colored ±, status, Resolve button). Right = detail panel with 3 progress bars (26AS/AIS/Computation amounts), mismatch summary box (danger-bg), advocate notes textarea, Mark Resolved / Escalate / Download Report buttons. Wire all to `reconciliationApi`.

### `src/pages/Sync.tsx`

2-column: left = trigger form (4 selects: scope, AY, documents, batch size), orange warning banner, 3 action buttons. Right = CAPTCHA solver status panel (Tesseract active/2Captcha standby/max retries). Below = sync history table (Job ID, started, scope, AY, total, success, failed, duration, status badge, Stop/Details buttons). Wire to `syncApi`.

### `src/pages/Jobs.tsx`

4 stat cards (running, queued, completed today, failed). Job list with per-job: status dot (animate-pulse-dot for running), job name, detail text, progress bar + pct, action buttons (Stop/Pause/Details). Auto-refresh every 5 seconds using `useQuery` with `refetchInterval: 5000`. Wire to `jobsApi`.

### `src/pages/Notices.tsx`

4 stat cards (31 active, 7 due, 4 scrutiny, 12 closed). 2-column: left = filter dropdowns (section, priority) + notice cards list (urgent/high/normal left border color, ⚠/📋/📄 icons, title, PAN, AY, received date, due date colored by urgency). Right = notice detail panel (full details, response history, notes, deadline countdown, attach documents, respond button). Wire to `noticesApi`.

### `src/pages/Calendar.tsx`

Month selector (April 2026). 7-column calendar grid. Days with deadline dots (red = deadline, blue = event). Below calendar = deadline list for selected month from `noticesApi.getDeadlines`. Click day → show that day's deadlines.

### `src/pages/Tasks.tsx`

3-column Kanban board: Pending / In Progress / Done. Each column with task cards (client name, task title, due date, priority dot). Drag-and-drop via `@dnd-kit/core`. Filter bar at top. Add task button opens modal. Wire to tasks API.

### `src/pages/Billing.tsx`

4 stat cards (billed, collected, outstanding, invoices). Filter bar (search, status, service). Invoice list with rows: ID, client (name + PAN), service, amount (Crimson Pro font), status badge, actions (View/Remind/Record Payment/Download). New Invoice modal. Wire to `billingApi`.

### `src/pages/Accounting.tsx`

2-column: left = Monthly P&L card (income rows in success color, expense rows in danger color, net profit in accent-blue, Crimson Pro font for totals). Right = Bank register (HDFC balance, SBI balance) + Transaction log table. Partner drawings section below.

### `src/pages/Reports.tsx`

4 tab-bar (AY Completion, Revenue, Notices, Sync Status). Each tab = appropriate Recharts chart. AY Completion: horizontal bar chart by form type. Revenue: line chart monthly. Notices: pie chart by section. Sync Status: area chart success rate. All data from API.

### `src/pages/Communication.tsx`

2-column. Left = compose panel: template selector (itr_filed_confirmation, ev_reminder, adv_tax_reminder, itr_deadline_reminder, notice_received), recipients dropdown (all/specific segments), message preview (rendered with variable placeholders highlighted), Send/Schedule/Preview buttons. Right = channel status cards (WhatsApp WATI, SendGrid, E-Verify OTP Bot each with status badge) + recent broadcasts activity list. Wire to `communicationApi`.

## 4.3 `src/main.tsx` — Root setup

```tsx
import React from 'react';
import ReactDOM from 'react-dom/client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Toaster } from 'react-hot-toast';
import Layout from './components/shell/Layout';
import Computation from './pages/Computation';
import { useAppStore } from './store/useAppStore';
import './index.css';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      gcTime:    5 * 60_000,
      retry: (failureCount, error: any) => {
        if (error?.response?.status === 404) return false;
        return failureCount < 2;
      },
    },
  },
});

function App() {
  const { computationClientId } = useAppStore();
  return (
    <>
      <Layout />
      {computationClientId !== null && <Computation />}
    </>
  );
}

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <App />
      <Toaster
        position="bottom-right"
        toastOptions={{
          duration: 4000,
          style: { fontFamily: 'DM Sans, sans-serif', fontSize: '13px' },
          success: { iconTheme: { primary: '#15803D', secondary: '#DCFCE7' } },
          error:   { iconTheme: { primary: '#B91C1C', secondary: '#FEE2E2' } },
        }}
      />
    </QueryClientProvider>
  </React.StrictMode>
);
```

## 4.4 Environment Configuration

```bash
# .env.development
VITE_API_BASE_URL=http://localhost:8080/api/v1

# .env.production
VITE_API_BASE_URL=https://your-backend-domain.com/api/v1
```

## 4.5 Backend CORS Configuration — Add to Spring Boot

```java
// Add to your Spring Boot main class or a separate @Configuration class:
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("http://localhost:5173", "https://your-frontend-domain.com"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return source;
}
```

## 4.6 Production Build Verification Checklist

Run these before declaring the build complete:

```bash
# 1. TypeScript — zero errors
npx tsc --noEmit

# 2. Production build
npm run build

# 3. Verify build size
du -sh dist/

# 4. Preview build locally
npm run preview

# 5. Verify all API endpoints are reachable
# (Use browser DevTools Network tab — check all XHR/fetch calls)
```

### Final Gate Criteria

| Check | Requirement |
|-------|-------------|
| TypeScript | Zero compiler errors |
| Build | Zero Vite build errors |
| Sidebar | All 13 nav items switch views correctly |
| Design tokens | All colors match source HTML pixel-for-pixel |
| Fonts | Crimson Pro, DM Sans, DM Mono loaded from Google Fonts |
| Client Master | Table loads from API with pagination and search |
| Client Panel | Slide-in panel shows correct client data |
| ITR Filing | Pipeline visual + filing table loads from API |
| Computation | Opens as overlay, loads saved data, recomputes on change |
| All schedules | ITR-1/2/3/4 fields all present and wired |
| CBDT Validation | CAT-A errors shown, Submit blocked |
| Regime recommender | Confidence score + tipping point displayed |
| EVC Modal | OTP flow works against backend |
| Export JSON | Downloads valid JSON file |
| Reconciliation | Mismatch detail panel + resolve works |
| Jobs | Auto-refreshes every 5s |
| Communication | Broadcast sends via API |
| Responsive | No layout breaks at 1280px minimum width |
| Toast notifications | Success/error toasts on all mutations |

---

**End of Frontend Directive — All 4 Phases**
*Date: April 11, 2026*
*Stack: React 18 + TypeScript + Tailwind CSS + TanStack Query + Zustand + Recharts*
*Backend: Spring Boot 3.x on localhost:8080*
