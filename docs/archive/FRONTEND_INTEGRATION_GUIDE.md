# Frontend ↔ Backend Integration Guide
## ITR Filing Website — Agentic Implementation Spec

> **Purpose:** This document is a complete, actionable specification for wiring the frontend to the backend APIs.  
> Follow every section in order. Do not skip steps. Every type, endpoint, and edge-case is documented here.

---

## Table of Contents
1. [Project Setup & Axios Base Instance](#1-project-setup--axios-base-instance)
2. [TypeScript DTO Interfaces](#2-typescript-dto-interfaces)
3. [Authentication API](#3-authentication-api)
4. [Client Management API](#4-client-management-api)
5. [Dashboard API](#5-dashboard-api)
6. [ITR Filing API (Unified)](#6-itr-filing-api-unified)
7. [Filing Management API](#7-filing-management-api)
8. [Document Management API](#8-document-management-api)
9. [PAN Validation API](#9-pan-validation-api)
10. [Prefill & Integration API](#10-prefill--integration-api)
11. [Stub APIs (Backend Not Yet Implemented)](#11-stub-apis-backend-not-yet-implemented)
12. [Global Error Handling](#12-global-error-handling)
13. [Auth Token Management](#13-auth-token-management)
14. [Implementation Checklist](#14-implementation-checklist)

---

## 1. Project Setup & Axios Base Instance

### File: `src/lib/api/axiosInstance.ts`

Create a **single shared Axios instance** used by ALL API modules. Never create ad-hoc `axios.create()` calls elsewhere.

```typescript
import axios, { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse, AxiosError } from 'axios';

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api';

const axiosInstance: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// ── REQUEST INTERCEPTOR: Attach JWT ──────────────────────────────────────────
axiosInstance.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('auth_token');
    if (token && config.headers) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// ── RESPONSE INTERCEPTOR: Handle 401 globally ────────────────────────────────
axiosInstance.interceptors.response.use(
  (response: AxiosResponse) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('auth_token');
      localStorage.removeItem('auth_email');
      window.location.href = '/login'; // adjust route as needed
    }
    return Promise.reject(error);
  }
);

export default axiosInstance;
```

### Environment Variable
Add to `.env` (and `.env.example`):
```
VITE_API_BASE_URL=http://localhost:8080/api
```

---

## 2. TypeScript DTO Interfaces

### File: `src/types/api.types.ts`

Define **every** backend DTO as a TypeScript interface. Import these in all API service files.

```typescript
// ─────────────────────────────────────────────
// AUTH
// ─────────────────────────────────────────────
export interface AuthRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  email: string;
  expiresIn: number; // seconds
}

// ─────────────────────────────────────────────
// CLIENT
// ─────────────────────────────────────────────
export interface ClientRequest {
  pan: string;
  name: string;
  email?: string;
  mobile?: string;
  aadhaar?: string;
  dob?: string; // ISO date string "YYYY-MM-DD" (maps to backend LocalDate)
}

export interface YearStatus {
  year: string;         // e.g. "2024-25"
  status: string;       // e.g. "Filed", "In Progress", "Not Started"
  itrType: string;      // e.g. "ITR-1", "ITR-2"
}

export interface ClientResponse {
  id: number;
  pan: string;
  name: string;
  email: string;
  mobile: string;
  aadhaar: string;
  dob: string;          // ISO date string
  years: YearStatus[];
}

export interface UpdateItrTypeRequest {
  itrType: string;      // e.g. "ITR-1"
}

// ─────────────────────────────────────────────
// ITR CLASSIFICATION
// ─────────────────────────────────────────────
export interface IncomeProfile {
  pan: string;
  residentialStatus: string;
  salaryIncome: number;
  housePropertyIncome: number;
  businessIncome: number;
  capitalGainsIncome: number;
  otherSourcesIncome: number;
  agriculturalIncome: number;
  totalIncome: number;
  hasCapitalGains: boolean;
  hasBusinessIncome: boolean;
  hasProfessionalIncome: boolean;
  hasMultipleProperties: boolean;
  hasForeignIncome: boolean;
  hasForeignAssets: boolean;
  isDirector: boolean;
  hasUnlistedShares: boolean;
  hasLotteryIncome: boolean;
  hasRaceHorseIncome: boolean;
  eligibleFor44AD: boolean;
  eligibleFor44ADA: boolean;
  eligibleFor44AE: boolean;
  businessTurnover: number;
  professionalReceipts: number;
  numberOfGoodsVehicles: number;
  hasCarriedForwardLosses: boolean;
  claimingForeignTaxCredit: boolean;
}

export interface ITRClassificationResult {
  pan: string;
  entityType: string;
  entityDescription: string;
  recommendedForm: string;
  eligibleForms: string[];
  classificationReason: string;
  reasonDetails: string[];
  warnings: string[];
  ineligibilityReasons: string[];
  totalIncome: number;
  hasCapitalGains: boolean;
  hasBusinessIncome: boolean;
  hasMultipleProperties: boolean;
  hasForeignIncome: boolean;
}

// ─────────────────────────────────────────────
// PAN
// ─────────────────────────────────────────────
export interface PANValidateResponse {
  pan: string;
  valid: boolean;
  message?: string;
}

export interface PANEntityTypeResponse {
  pan: string;
  entityType: string;
  entityDescription: string;
  isIndividualOrHUF: boolean;
}

export interface PANEligibleFormsResponse {
  pan: string;
  entityType: string;
  entityDescription: string;
  eligibleITRForms: string[];
}

export interface PANAnalysis {
  pan: string;
  valid: boolean;
  entityType: string;
  entityDescription: string;
  isIndividualOrHUF: boolean;
  eligibleITRForms: string[];
  warnings: string[];
}

// ─────────────────────────────────────────────
// DASHBOARD
// ─────────────────────────────────────────────
export interface DashboardStats {
  total: number;
  filed: number;
  inProgress: number;
  docPending: number;
  watchList: number;
  totalMismatches: number;
  totalNotices: number;
}

// ─────────────────────────────────────────────
// ITR FORM DATA
// ─────────────────────────────────────────────
/** Top-level ITR-1 form. All sub-objects below. */
export interface Itr1FormData {
  personalInfo: PersonalInfo;
  salaryIncome: SalaryIncome;
  housePropertyIncome: HousePropertyIncome;
  otherSourcesIncome: OtherSourcesIncome;
  deductions: Deductions;
  taxPayments: TaxPayments;
  exemptIncome?: ExemptIncome;
  taxComputation?: TaxComputation;
  validationErrors?: string[];
  validationWarnings?: string[];
}

export interface PersonalInfo {
  assesseeName: string;
  pan: string;
  aadhaar?: string;
  dateOfBirth: string;
  age?: number;
  ageCategory?: string;
  email?: string;
  mobile?: string;
  // Address fields
  flatDoorNo?: string;
  premisesName?: string;
  road?: string;
  area?: string;
  city?: string;
  state?: string;
  pinCode?: string;
  country?: string;
  assessmentYear: string;
  financialYear: string;
  filingType?: string;
  residentialStatus: string;   // "ROR" | "RNOR" | "NR"
  regime?: string;             // "OLD" | "NEW"
  employerCategory?: string;
  bankDetails?: BankDetails;
}

export interface BankDetails {
  ifscCode: string;
  bankName: string;
  accountNumber: string;
  accountType: string;
}

export interface SalaryIncome {
  salary17_1?: number;
  perquisites17_2?: number;
  profitsInLieu17_3?: number;
  grossSalary?: number;
  exemptAllowances?: ExemptAllowances;
  standardDeduction?: number;
  professionalTax?: number;
  incomeFromSalary?: number;
  employers?: EmployerDetail[];
}

export interface ExemptAllowances {
  hra?: number;
  lta?: number;
  other?: number;
}

export interface EmployerDetail {
  employerName: string;
  employerTAN: string;
  salary?: number;
  tdsDeducted?: number;
}

export interface HousePropertyIncome {
  propertyType?: string;
  address?: string;
  annualRent?: number;
  municipalTaxesPaid?: number;
  standardDeduction30Pct?: number;
  interestOnLoan?: number;
  incomeFromHP?: number;
  loanDetails?: LoanDetail;
  coOwners?: CoOwner[];
}

export interface LoanDetail {
  lenderName?: string;
  loanAmount?: number;
  disbursementDate?: string;
}

export interface CoOwner {
  name: string;
  pan: string;
  share: number;
}

export interface OtherSourcesIncome {
  savingsAccountInterest?: number;
  fixedDepositInterest?: number;
  dividendIncome?: number;
  familyPension?: number;
  casualIncome?: number;
  totalOtherSourcesIncome?: number;
  bankInterestDetails?: BankInterestDetail[];
  dividendDetails?: DividendDetail[];
}

export interface BankInterestDetail {
  bankName: string;
  accountNumber?: string;
  interestAmount: number;
}

export interface DividendDetail {
  companyName: string;
  dividendAmount: number;
}

export interface Deductions {
  // 80C
  lic?: number;
  ppf?: number;
  elss?: number;
  epf?: number;
  nsc?: number;
  homeLoanPrincipal?: number;
  tuitionFees?: number;
  total80C?: number;
  // NPS
  nps80CCD1?: number;
  nps80CCD1B?: number;
  nps80CCD2?: number;
  // Health Insurance
  healthInsurance80D?: number;
  // Other sections
  deduction80DD?: number;
  deduction80DDB?: number;
  deduction80E?: number;
  deduction80EE?: number;
  deduction80EEA?: number;
  deduction80EEB?: number;
  deduction80G?: number;
  deduction80GG?: number;
  deduction80TTA?: number;
  deduction80TTB?: number;
  deduction80U?: number;
  totalDeductions?: number;
}

export interface TaxPayments {
  tdsOnSalary?: TDSEntry[];
  tdsOnOther?: TDSEntry[];
  tcsEntries?: TCSEntry[];
  advanceTaxEntries?: TaxPaymentEntry[];
  selfAssessmentTaxEntries?: TaxPaymentEntry[];
}

export interface TDSEntry {
  deductorName: string;
  deductorTAN: string;
  amountPaid?: number;
  tdsDeducted: number;
}

export interface TCSEntry {
  collectorName: string;
  collectorTAN: string;
  amountReceived?: number;
  tcsCollected: number;
}

export interface TaxPaymentEntry {
  bsrCode: string;
  challanNo: string;
  date: string;
  amount: number;
}

export interface ExemptIncome {
  agriculturalIncome?: number;
  ppfInterest?: number;
  dividendExempt?: number;
  totalExemptIncome?: number;
}

export interface TaxComputation {
  grossTotalIncome?: number;
  totalDeductions?: number;
  totalIncome?: number;
  taxOnNormalIncome?: number;
  rebate87A?: number;
  surcharge?: number;
  cess?: number;
  totalTaxLiability?: number;
  relief89?: number;
  interest234A?: number;
  interest234B?: number;
  interest234C?: number;
  fee234F?: number;
  taxPayable?: number;
  refund?: number;
}

export interface ValidationResponse {
  valid: boolean;
  errors: string[];
  warnings: string[];
}

// ─────────────────────────────────────────────
// FILING
// ─────────────────────────────────────────────
export interface FilingRequest {
  clientId: number;
  assessmentYear: string;
  itrType: string;
  status: string;
  filingDate?: string;             // "YYYY-MM-DD"
  acknowledgementNumber?: string;
}

export interface FilingResponse {
  id: number;
  clientId: number;
  clientName: string;
  clientPan: string;
  assessmentYear: string;
  itrType: string;
  status: string;
  filingDate?: string;
  acknowledgementNumber?: string;
  totalIncome?: number;
  taxPayable?: number;
  refundAmount?: number;
  createdAt: string;   // OffsetDateTime → ISO string
  updatedAt: string;
}

// ─────────────────────────────────────────────
// DOCUMENTS
// ─────────────────────────────────────────────
export type DocumentType =
  | 'FORM_16'
  | 'AIS'
  | 'FORM_26AS'
  | 'PREFILL_JSON'
  | 'ITR_JSON'
  | 'ITR_PDF'
  | 'OTHER';

export interface DocumentMetadata {
  id: number;
  clientId: number;
  assessmentYear: string;
  documentType: DocumentType;
  fileName: string;
  fileSize: number;
  uploadedAt: string;
  url?: string;
}

// ─────────────────────────────────────────────
// PREFILL
// ─────────────────────────────────────────────
export interface PrefillResponse {
  clientId: number;
  clientName: string;
  pan: string;
  assessmentYear: string;
  status: string;
  recommendedItrType: string;
  incomeSummary: {
    assesseeName: string;
    panNumber: string;
    dob: string;
    grossSalary: number;
    section80C: number;
    section80D: number;
    totalIncome: number;
    otherSources: number;
  };
}

// ─────────────────────────────────────────────
// INTEGRATION
// ─────────────────────────────────────────────
export interface Form16Data {
  partA: {
    employerName: string;
    employerTAN: string;
    employeeName: string;
    employeePAN: string;
    financialYear: string;
    assessmentYear: string;
    totalTaxDeducted: number;
    quarterlyTDS: QuarterlyTDS[];
  };
  partB: {
    grossSalary: number;
    exemptAllowances: number;
    deductions: number;
    incomeFromSalary: number;
    totalIncome: number;
    taxComputation: TaxComputation;
  };
}

export interface QuarterlyTDS {
  quarter: string;
  amountPaid: number;
  tdsDeducted: number;
}

export interface AISData {
  pan: string;
  assessmentYear: string;
  financialYear: string;
  tdsSalary: TDSEntry[];
  tdsOther: TDSEntry[];
  tcs: TCSEntry[];
  taxPayments: TaxPaymentEntry[];
  sft: { entries: SFTEntry[] };
  demandRefund: any[];
  airTransactions: any[];
  capitalGainsTransactions: CapitalGainsTransaction[];
}

export interface SFTEntry {
  transactionType: string;
  amount: number;
  reportingEntity: string;
}

export interface CapitalGainsTransaction {
  assetType: string;
  saleDate: string;
  purchaseDate: string;
  saleValue: number;
  purchaseCost: number;
  gain: number;
}

export interface Form26ASData {
  pan: string;
  assessmentYear: string;
  tdsEntries: TDSEntry[];
  tcsEntries: TCSEntry[];
  taxPayments: TaxPaymentEntry[];
}

export interface ITDPrefillData {
  pan: string;
  assessmentYear: string;
  personalInfo: Partial<PersonalInfo>;
  incomeDetails: Partial<Itr1FormData>;
}
```

---

## 3. Authentication API

### File: `src/lib/api/auth.ts`

```typescript
import axiosInstance from './axiosInstance';
import type { AuthRequest, AuthResponse } from '@/types/api.types';

export const authApi = {
  /**
   * POST /api/auth/register
   * Registers a new CA/user account.
   */
  register: async (payload: AuthRequest): Promise<AuthResponse> => {
    const { data } = await axiosInstance.post<AuthResponse>('/auth/register', payload);
    return data;
  },

  /**
   * POST /api/auth/login
   * Authenticates user and returns a JWT.
   * IMPORTANT: After calling this, store token via `tokenManager.save(response)`.
   */
  login: async (payload: AuthRequest): Promise<AuthResponse> => {
    const { data } = await axiosInstance.post<AuthResponse>('/auth/login', payload);
    return data;
  },
};
```

### Token Manager — `src/lib/api/tokenManager.ts`

```typescript
import type { AuthResponse } from '@/types/api.types';

const TOKEN_KEY = 'auth_token';
const EMAIL_KEY = 'auth_email';
const EXPIRY_KEY = 'auth_expiry';

export const tokenManager = {
  save(response: AuthResponse) {
    localStorage.setItem(TOKEN_KEY, response.token);
    localStorage.setItem(EMAIL_KEY, response.email);
    const expiresAt = Date.now() + response.expiresIn * 1000;
    localStorage.setItem(EXPIRY_KEY, String(expiresAt));
  },
  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  },
  getEmail(): string | null {
    return localStorage.getItem(EMAIL_KEY);
  },
  isExpired(): boolean {
    const expiry = localStorage.getItem(EXPIRY_KEY);
    if (!expiry) return true;
    return Date.now() > parseInt(expiry, 10);
  },
  isAuthenticated(): boolean {
    return !!this.getToken() && !this.isExpired();
  },
  clear() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(EMAIL_KEY);
    localStorage.removeItem(EXPIRY_KEY);
  },
};
```

### Usage in Login Component
```typescript
import { authApi } from '@/lib/api/auth';
import { tokenManager } from '@/lib/api/tokenManager';

const handleLogin = async (email: string, password: string) => {
  const response = await authApi.login({ email, password });
  tokenManager.save(response);
  navigate('/dashboard');
};
```

---

## 4. Client Management API

### File: `src/lib/api/clients.ts`

> **⚠️ ALIGNMENT NOTE:** The existing `clients.ts` uses a `Client` frontend type. Replace all references with `ClientResponse` from the backend. Add adapters if the existing UI requires the old shape.

```typescript
import axiosInstance from './axiosInstance';
import type {
  ClientRequest,
  ClientResponse,
  YearStatus,
  UpdateItrTypeRequest,
  IncomeProfile,
  ITRClassificationResult,
  PANAnalysis,
} from '@/types/api.types';

export interface ClientFilters {
  search?: string;
  status?: string;
  itrType?: string;
  assessmentYear?: string;
}

export const clientsApi = {
  /**
   * GET /api/clients
   * Returns all clients for the authenticated user.
   * Use `filters` for optional client-side query params (confirm with backend if supported).
   */
  list: async (filters?: ClientFilters): Promise<ClientResponse[]> => {
    const { data } = await axiosInstance.get<ClientResponse[]>('/clients', {
      params: filters,
    });
    return data;
  },

  /**
   * GET /api/clients/{clientId}
   */
  get: async (clientId: number): Promise<ClientResponse> => {
    const { data } = await axiosInstance.get<ClientResponse>(`/clients/${clientId}`);
    return data;
  },

  /**
   * POST /api/clients
   * Required: `pan`, `name`. All others optional.
   */
  create: async (payload: ClientRequest): Promise<ClientResponse> => {
    const { data } = await axiosInstance.post<ClientResponse>('/clients', payload);
    return data;
  },

  /**
   * PUT /api/clients/{clientId}
   */
  update: async (clientId: number, payload: Partial<ClientRequest>): Promise<ClientResponse> => {
    const { data } = await axiosInstance.put<ClientResponse>(`/clients/${clientId}`, payload);
    return data;
  },

  /**
   * DELETE /api/clients/{clientId}
   * Returns 204 No Content on success.
   */
  delete: async (clientId: number): Promise<void> => {
    await axiosInstance.delete(`/clients/${clientId}`);
  },

  /**
   * GET /api/clients/{clientId}/years
   * Returns filing year status list for a client.
   */
  getYears: async (clientId: number): Promise<YearStatus[]> => {
    const { data } = await axiosInstance.get<YearStatus[]>(`/clients/${clientId}/years`);
    return data;
  },

  /**
   * PUT /api/clients/{clientId}/years/{year}/itr-type
   * e.g. year = "2024-25", payload = { itrType: "ITR-2" }
   */
  updateItrType: async (
    clientId: number,
    year: string,
    payload: UpdateItrTypeRequest
  ): Promise<void> => {
    await axiosInstance.put(`/clients/${clientId}/years/${year}/itr-type`, payload);
  },

  /**
   * POST /api/clients/{clientId}/itr-classification
   * Pass an IncomeProfile to get recommended ITR form.
   */
  getItrClassification: async (
    clientId: number,
    incomeProfile: IncomeProfile
  ): Promise<ITRClassificationResult> => {
    const { data } = await axiosInstance.post<ITRClassificationResult>(
      `/clients/${clientId}/itr-classification`,
      incomeProfile
    );
    return data;
  },

  /**
   * GET /api/clients/{clientId}/pan-analysis
   */
  getPanAnalysis: async (clientId: number): Promise<PANAnalysis> => {
    const { data } = await axiosInstance.get<PANAnalysis>(`/clients/${clientId}/pan-analysis`);
    return data;
  },
};
```

---

## 5. Dashboard API

### File: `src/lib/api/dashboard.ts`

> **NOTE:** The existing `clientsApi.getDashboardStats()` call must be moved here and updated.

```typescript
import axiosInstance from './axiosInstance';
import type { DashboardStats } from '@/types/api.types';

export const dashboardApi = {
  /**
   * GET /api/dashboard/stats
   * Returns aggregate counts for the dashboard overview cards.
   */
  getStats: async (): Promise<DashboardStats> => {
    const { data } = await axiosInstance.get<DashboardStats>('/dashboard/stats');
    return data;
  },
};
```

### Usage in Dashboard Component
```typescript
import { dashboardApi } from '@/lib/api/dashboard';

const stats = await dashboardApi.getStats();
// stats.total, stats.filed, stats.inProgress, stats.docPending,
// stats.watchList, stats.totalMismatches, stats.totalNotices
```

---

## 6. ITR Filing API (Unified)

### File: `src/lib/api/itr.ts`

> This is the most complex API. All ITR form operations go through `/api/clients/{clientId}/itr/{year}`.

```typescript
import axiosInstance from './axiosInstance';
import type { Itr1FormData, ValidationResponse } from '@/types/api.types';

export const itrApi = {
  /**
   * GET /api/clients/{clientId}/itr/{year}
   * Fetch saved ITR form data for a client + year.
   * year format: "2024-25"
   */
  getFormData: async (clientId: number, year: string): Promise<Itr1FormData> => {
    const { data } = await axiosInstance.get<Itr1FormData>(`/clients/${clientId}/itr/${year}`);
    return data;
  },

  /**
   * PUT /api/clients/{clientId}/itr/{year}
   * Save/update ITR form data (draft save).
   */
  saveFormData: async (
    clientId: number,
    year: string,
    formData: Itr1FormData
  ): Promise<Itr1FormData> => {
    const { data } = await axiosInstance.put<Itr1FormData>(
      `/clients/${clientId}/itr/${year}`,
      formData
    );
    return data;
  },

  /**
   * POST /api/clients/{clientId}/itr/{year}/compute
   * Computes tax based on current form data and returns updated form with TaxComputation populated.
   */
  computeTax: async (
    clientId: number,
    year: string,
    formData: Itr1FormData
  ): Promise<Itr1FormData> => {
    const { data } = await axiosInstance.post<Itr1FormData>(
      `/clients/${clientId}/itr/${year}/compute`,
      formData
    );
    return data;
  },

  /**
   * POST /api/clients/{clientId}/itr/{year}/validate
   * Validate form data. Check `response.valid` and display `response.errors` / `response.warnings`.
   */
  validate: async (
    clientId: number,
    year: string,
    formData: Itr1FormData
  ): Promise<ValidationResponse> => {
    const { data } = await axiosInstance.post<ValidationResponse>(
      `/clients/${clientId}/itr/${year}/validate`,
      formData
    );
    return data;
  },

  /**
   * GET /api/clients/{clientId}/itr/{year}/download
   * Downloads the ITR JSON file.
   * Use `window.URL.createObjectURL` to trigger browser download.
   */
  downloadJson: async (clientId: number, year: string): Promise<void> => {
    const response = await axiosInstance.get(
      `/clients/${clientId}/itr/${year}/download`,
      { responseType: 'blob' }
    );
    const url = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `ITR_${clientId}_${year}.json`);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  },

  /**
   * GET /api/clients/{clientId}/itr/{year}/download-pdf
   * Downloads the ITR PDF report.
   */
  downloadPdf: async (clientId: number, year: string): Promise<void> => {
    const response = await axiosInstance.get(
      `/clients/${clientId}/itr/${year}/download-pdf`,
      { responseType: 'blob' }
    );
    const url = window.URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `ITR_${clientId}_${year}.pdf`);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  },
};
```

---

## 7. Filing Management API

### File: `src/lib/api/filing.ts`

> **⚠️ ALIGNMENT NOTE:** The existing `filing.ts` calls several endpoints (`reconcileAIS`, `initiateEVC`, `confirmEVC`, `uploadForm16`) that do NOT have backend implementations. Replace those with stubs (see Section 11).

```typescript
import axiosInstance from './axiosInstance';
import type { FilingRequest, FilingResponse } from '@/types/api.types';

export interface FilingFilters {
  clientId?: number;
  assessmentYear?: string;
  itrType?: string;
  status?: string;
}

export const filingApi = {
  /**
   * GET /api/filing
   * Optional query params: clientId, assessmentYear, itrType, status
   */
  list: async (filters?: FilingFilters): Promise<FilingResponse[]> => {
    const { data } = await axiosInstance.get<FilingResponse[]>('/filing', {
      params: filters,
    });
    return data;
  },

  /**
   * POST /api/filing
   */
  create: async (payload: FilingRequest): Promise<FilingResponse> => {
    const { data } = await axiosInstance.post<FilingResponse>('/filing', payload);
    return data;
  },

  /**
   * PUT /api/filing/{id}
   */
  update: async (id: number, payload: Partial<FilingRequest>): Promise<FilingResponse> => {
    const { data } = await axiosInstance.put<FilingResponse>(`/filing/${id}`, payload);
    return data;
  },

  /**
   * DELETE /api/filing/{id}
   */
  delete: async (id: number): Promise<void> => {
    await axiosInstance.delete(`/filing/${id}`);
  },
};
```

---

## 8. Document Management API

### File: `src/lib/api/documents.ts`

> **NOTE:** This module is **not yet connected in the frontend**. Create it from scratch.

```typescript
import axiosInstance from './axiosInstance';
import type { DocumentMetadata, DocumentType } from '@/types/api.types';

export interface DocumentListParams {
  clientId: number;
  assessmentYear: string;
  documentType?: DocumentType;
}

export const documentsApi = {
  /**
   * POST /api/documents/upload
   * Multipart upload. Pass `clientId`, `assessmentYear`, `documentType` as form fields alongside the file.
   */
  upload: async (
    file: File,
    clientId: number,
    assessmentYear: string,
    documentType: DocumentType
  ): Promise<DocumentMetadata> => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('clientId', String(clientId));
    formData.append('assessmentYear', assessmentYear);
    formData.append('documentType', documentType);

    const { data } = await axiosInstance.post<DocumentMetadata>('/documents/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return data;
  },

  /**
   * GET /api/documents/list?clientId=&assessmentYear=&documentType=
   */
  list: async (params: DocumentListParams): Promise<DocumentMetadata[]> => {
    const { data } = await axiosInstance.get<DocumentMetadata[]>('/documents/list', {
      params,
    });
    return data;
  },

  /**
   * GET /api/documents/by-type?clientId=&assessmentYear=&documentType=
   * Returns the first/only document of that type for the client+year.
   */
  getByType: async (params: DocumentListParams): Promise<DocumentMetadata> => {
    const { data } = await axiosInstance.get<DocumentMetadata>('/documents/by-type', { params });
    return data;
  },

  /**
   * GET /api/documents/download/{documentId}
   * Triggers a browser download.
   */
  download: async (documentId: number, fileName: string): Promise<void> => {
    const response = await axiosInstance.get(`/documents/download/${documentId}`, {
      responseType: 'blob',
    });
    const url = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', fileName);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  },

  /**
   * DELETE /api/documents/{documentId}
   */
  delete: async (documentId: number): Promise<void> => {
    await axiosInstance.delete(`/documents/${documentId}`);
  },
};
```

---

## 9. PAN Validation API

### File: `src/lib/api/pan.ts`

> **NOTE:** This module is **not yet in the frontend**. Create it from scratch.

```typescript
import axiosInstance from './axiosInstance';
import type {
  PANValidateResponse,
  PANEntityTypeResponse,
  PANEligibleFormsResponse,
  PANAnalysis,
} from '@/types/api.types';

export const panApi = {
  /**
   * GET /api/pan/{pan}/validate
   * Quick format + checksum validation of a PAN string.
   */
  validate: async (pan: string): Promise<PANValidateResponse> => {
    const { data } = await axiosInstance.get<PANValidateResponse>(`/pan/${pan}/validate`);
    return data;
  },

  /**
   * GET /api/pan/{pan}/entity-type
   * Returns entity type (Individual, HUF, Company, etc.)
   */
  getEntityType: async (pan: string): Promise<PANEntityTypeResponse> => {
    const { data } = await axiosInstance.get<PANEntityTypeResponse>(`/pan/${pan}/entity-type`);
    return data;
  },

  /**
   * GET /api/pan/{pan}/eligible-itr-forms
   * Returns list of ITR forms the entity is eligible for.
   */
  getEligibleForms: async (pan: string): Promise<PANEligibleFormsResponse> => {
    const { data } = await axiosInstance.get<PANEligibleFormsResponse>(
      `/pan/${pan}/eligible-itr-forms`
    );
    return data;
  },

  /**
   * GET /api/pan/{pan}/analyze
   * Full analysis combining validation + entity type + eligible forms + warnings.
   * Use this for the "Add Client" flow to prefill entity info automatically.
   */
  analyze: async (pan: string): Promise<PANAnalysis> => {
    const { data } = await axiosInstance.get<PANAnalysis>(`/pan/${pan}/analyze`);
    return data;
  },
};
```

### Usage: Auto-fill on PAN Input
```typescript
// In the "Add Client" form, on PAN field blur:
const handlePanBlur = async (pan: string) => {
  if (pan.length === 10) {
    const analysis = await panApi.analyze(pan);
    if (!analysis.valid) {
      setFieldError('pan', 'Invalid PAN');
      return;
    }
    // Auto-fill entity type, eligible forms
    setFieldValue('entityType', analysis.entityType);
    setEligibleForms(analysis.eligibleITRForms);
    if (analysis.warnings.length) showWarnings(analysis.warnings);
  }
};
```

---

## 10. Prefill & Integration API

### File: `src/lib/api/integration.ts`

```typescript
import axiosInstance from './axiosInstance';
import type {
  PrefillResponse,
  Form16Data,
  AISData,
  Form26ASData,
  ITDPrefillData,
  Itr1FormData,
  Itr2FormData, // extend if ITR-2 is implemented on frontend
} from '@/types/api.types';

export const prefillApi = {
  /**
   * POST /api/clients/{clientId}/prefill/{year}
   * Upload a prefill JSON/file for a client.
   */
  upload: async (
    clientId: number,
    year: string,
    file: File
  ): Promise<PrefillResponse> => {
    const formData = new FormData();
    formData.append('file', file);
    const { data } = await axiosInstance.post<PrefillResponse>(
      `/clients/${clientId}/prefill/${year}`,
      formData,
      { headers: { 'Content-Type': 'multipart/form-data' } }
    );
    return data;
  },

  /**
   * GET /api/clients/{clientId}/prefill/{year}
   */
  get: async (clientId: number, year: string): Promise<PrefillResponse> => {
    const { data } = await axiosInstance.get<PrefillResponse>(
      `/clients/${clientId}/prefill/${year}`
    );
    return data;
  },
};

export const integrationApi = {
  /**
   * POST /api/integration/form16/extract
   * Upload a Form 16 PDF/ZIP and extract structured data.
   */
  extractForm16: async (file: File): Promise<Form16Data> => {
    const formData = new FormData();
    formData.append('file', file);
    const { data } = await axiosInstance.post<Form16Data>(
      '/integration/form16/extract',
      formData,
      { headers: { 'Content-Type': 'multipart/form-data' } }
    );
    return data;
  },

  /**
   * POST /api/integration/ais/import
   */
  importAIS: async (file: File): Promise<AISData> => {
    const formData = new FormData();
    formData.append('file', file);
    const { data } = await axiosInstance.post<AISData>('/integration/ais/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return data;
  },

  /**
   * POST /api/integration/26as/import
   */
  import26AS: async (file: File): Promise<Form26ASData> => {
    const formData = new FormData();
    formData.append('file', file);
    const { data } = await axiosInstance.post<Form26ASData>('/integration/26as/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return data;
  },

  /**
   * POST /api/integration/prefill/import
   */
  importITDPrefill: async (file: File): Promise<ITDPrefillData> => {
    const formData = new FormData();
    formData.append('file', file);
    const { data } = await axiosInstance.post<ITDPrefillData>(
      '/integration/prefill/import',
      formData,
      { headers: { 'Content-Type': 'multipart/form-data' } }
    );
    return data;
  },

  /**
   * POST /api/integration/autopopulate/form16
   * Pass the current ITR-1 form data + extracted Form16Data to auto-populate.
   */
  autoPopulateFromForm16: async (
    itrData: Itr1FormData,
    form16Data: Form16Data
  ): Promise<Itr1FormData> => {
    const { data } = await axiosInstance.post<Itr1FormData>(
      '/integration/autopopulate/form16',
      { itrData, form16Data }
    );
    return data;
  },

  /**
   * POST /api/integration/autopopulate/ais
   */
  autoPopulateFromAIS: async (
    itrData: Itr1FormData,
    aisData: AISData
  ): Promise<Itr1FormData> => {
    const { data } = await axiosInstance.post<Itr1FormData>(
      '/integration/autopopulate/ais',
      { itrData, aisData }
    );
    return data;
  },

  /**
   * POST /api/integration/autopopulate/26as
   */
  autoPopulateFrom26AS: async (
    itrData: Itr1FormData,
    form26asData: Form26ASData
  ): Promise<Itr1FormData> => {
    const { data } = await axiosInstance.post<Itr1FormData>(
      '/integration/autopopulate/26as',
      { itrData, form26asData }
    );
    return data;
  },

  /**
   * POST /api/integration/autopopulate/prefill
   */
  autoPopulateFromPrefill: async (
    itrData: Itr1FormData,
    prefillData: ITDPrefillData
  ): Promise<Itr1FormData> => {
    const { data } = await axiosInstance.post<Itr1FormData>(
      '/integration/autopopulate/prefill',
      { itrData, prefillData }
    );
    return data;
  },
};
```

---

## 11. Stub APIs (Backend Not Yet Implemented)

These frontend API files call endpoints that **do not exist on the backend yet**. Instead of letting them throw 404/network errors, replace all their implementations with stubs that:
1. Log a `console.warn` noting the missing backend.
2. Return a typed mock/empty response so the UI doesn't crash.
3. Throw a specific `NotImplementedError` if the calling UI must know (e.g., for disabling buttons).

### File: `src/lib/api/_stubs.ts`

```typescript
export class NotImplementedError extends Error {
  constructor(endpoint: string) {
    super(`Backend endpoint not implemented: ${endpoint}`);
    this.name = 'NotImplementedError';
  }
}

export function stub<T>(endpoint: string, fallback: T): T {
  console.warn(`[STUB] ${endpoint} — backend not implemented, returning mock data.`);
  return fallback;
}
```

### File: `src/lib/api/notices.ts` — STUB

```typescript
import { stub } from './_stubs';
import type { Notice } from '@/types/types'; // existing frontend type

export const noticesApi = {
  list: async (): Promise<Notice[]> =>
    stub('/api/notices', []),

  get: async (id: number): Promise<Notice | null> =>
    stub(`/api/notices/${id}`, null),

  update: async (id: number, data: Partial<Notice>): Promise<Notice | null> =>
    stub(`/api/notices/${id} PUT`, null),

  addNote: async (id: number, note: string): Promise<void> =>
    stub(`/api/notices/${id}/notes`, undefined),

  close: async (id: number): Promise<void> =>
    stub(`/api/notices/${id}/close`, undefined),

  getDeadlines: async (month: string): Promise<any[]> =>
    stub(`/api/notices/deadlines?month=${month}`, []),
};
```

### File: `src/lib/api/billing.ts` — STUB

```typescript
import { stub } from './_stubs';
import type { Invoice } from '@/types/types';

export const billingApi = {
  list: async (): Promise<Invoice[]> =>
    stub('/api/billing', []),

  create: async (data: Partial<Invoice>): Promise<Invoice | null> =>
    stub('/api/billing POST', null),

  recordPayment: async (id: string, amount: number): Promise<void> =>
    stub(`/api/billing/${id}/payment`, undefined),

  sendWhatsApp: async (id: string): Promise<void> =>
    stub(`/api/billing/${id}/whatsapp`, undefined),

  download: async (id: string): Promise<void> =>
    stub(`/api/billing/${id}/download`, undefined),

  getStats: async (): Promise<any> =>
    stub('/api/billing/stats', { totalBilled: 0, totalCollected: 0, outstanding: 0 }),
};
```

### File: `src/lib/api/sync.ts` — STUB

```typescript
import { stub } from './_stubs';

export const syncApi = {
  trigger: async (params: {
    scope: string;
    assessmentYear: string;
    documents: string[];
    batchSize: number;
  }): Promise<any> => stub('/api/sync/trigger', { jobId: null }),

  getHistory: async (): Promise<any[]> =>
    stub('/api/sync/history', []),

  stop: async (jobId: number): Promise<void> =>
    stub(`/api/sync/stop/${jobId}`, undefined),

  getDetails: async (jobId: number): Promise<any> =>
    stub(`/api/sync/${jobId}`, null),
};
```

### File: `src/lib/api/jobs.ts` — STUB

```typescript
import { stub } from './_stubs';
import type { BatchJob } from '@/types/types';

export const jobsApi = {
  list: async (): Promise<BatchJob[]> =>
    stub('/api/jobs', []),

  get: async (jobId: number): Promise<BatchJob | null> =>
    stub(`/api/jobs/${jobId}`, null),

  getStats: async (): Promise<any> =>
    stub('/api/jobs/stats', { running: 0, completed: 0, failed: 0 }),

  stop: async (jobId: number): Promise<void> =>
    stub(`/api/jobs/${jobId}/stop`, undefined),

  restart: async (jobId: number): Promise<void> =>
    stub(`/api/jobs/${jobId}/restart`, undefined),

  getLogs: async (jobId: number): Promise<string[]> =>
    stub(`/api/jobs/${jobId}/logs`, []),
};
```

### File: `src/lib/api/reconciliation.ts` — STUB

```typescript
import { stub } from './_stubs';

export const reconciliationApi = {
  getStats: async (): Promise<any> =>
    stub('/api/reconciliation/stats', { total: 0, resolved: 0, pending: 0 }),

  listMismatches: async (): Promise<any[]> =>
    stub('/api/reconciliation/mismatches', []),

  resolve: async (id: number): Promise<void> =>
    stub(`/api/reconciliation/${id}/resolve`, undefined),

  escalate: async (id: number): Promise<void> =>
    stub(`/api/reconciliation/${id}/escalate`, undefined),

  downloadReport: async (id: number): Promise<void> =>
    stub(`/api/reconciliation/${id}/download`, undefined),
};
```

### File: `src/lib/api/communication.ts` — STUB

```typescript
import { stub } from './_stubs';

export const communicationApi = {
  broadcast: async (template: string, recipients: string, schedule?: string): Promise<void> =>
    stub('/api/communication/broadcast', undefined),

  getTemplates: async (): Promise<any[]> =>
    stub('/api/communication/templates', []),

  getHistory: async (): Promise<any[]> =>
    stub('/api/communication/history', []),

  getChannelStatus: async (): Promise<any> =>
    stub('/api/communication/channel-status', {
      whatsapp: false,
      email: false,
      sms: false,
    }),
};
```

---

## 12. Global Error Handling

### File: `src/lib/api/apiError.ts`

```typescript
import { AxiosError } from 'axios';

export interface ApiErrorBody {
  message?: string;
  error?: string;
  status?: number;
  path?: string;
}

export class ApiError extends Error {
  public status: number;
  public body: ApiErrorBody;

  constructor(status: number, body: ApiErrorBody) {
    super(body.message ?? body.error ?? 'An unexpected error occurred');
    this.name = 'ApiError';
    this.status = status;
    this.body = body;
  }
}

/**
 * Wrap any API call to standardize error handling.
 * Usage: const data = await handleApiCall(() => clientsApi.list());
 */
export async function handleApiCall<T>(fn: () => Promise<T>): Promise<T> {
  try {
    return await fn();
  } catch (err) {
    if (err instanceof AxiosError && err.response) {
      throw new ApiError(err.response.status, err.response.data as ApiErrorBody);
    }
    throw err;
  }
}
```

### Usage Pattern in Components / Hooks
```typescript
import { handleApiCall, ApiError } from '@/lib/api/apiError';
import { clientsApi } from '@/lib/api/clients';

try {
  const clients = await handleApiCall(() => clientsApi.list());
} catch (err) {
  if (err instanceof ApiError) {
    if (err.status === 404) showToast('Client not found');
    else if (err.status === 400) showToast(err.message); // validation error from backend
    else if (err.status === 500) showToast('Server error. Please try again.');
  }
}
```

---

## 13. Auth Token Management

### Protected Route Guard — `src/components/ProtectedRoute.tsx`

```typescript
import { Navigate, Outlet } from 'react-router-dom';
import { tokenManager } from '@/lib/api/tokenManager';

export const ProtectedRoute = () => {
  if (!tokenManager.isAuthenticated()) {
    return <Navigate to="/login" replace />;
  }
  return <Outlet />;
};
```

### Logout Handler
```typescript
import { tokenManager } from '@/lib/api/tokenManager';
import { useNavigate } from 'react-router-dom';

const handleLogout = () => {
  tokenManager.clear();
  navigate('/login');
};
```

---

## 14. Implementation Checklist

Work through this in order. Check each item off as you complete it.

### Phase 1 — Foundation
- [ ] Create `src/types/api.types.ts` with all interfaces from Section 2
- [ ] Create `src/lib/api/axiosInstance.ts` with interceptors (Section 1)
- [ ] Create `src/lib/api/tokenManager.ts` (Section 13)
- [ ] Create `src/lib/api/apiError.ts` (Section 12)
- [ ] Create `src/lib/api/_stubs.ts` (Section 11)
- [ ] Add `VITE_API_BASE_URL` to `.env` and `.env.example`

### Phase 2 — Auth
- [ ] Create `src/lib/api/auth.ts`
- [ ] Update Login component to call `authApi.login()` and `tokenManager.save()`
- [ ] Update Register component to call `authApi.register()`
- [ ] Add `ProtectedRoute` wrapper around all authenticated routes
- [ ] Add logout button that calls `tokenManager.clear()` and redirects

### Phase 3 — Core Data APIs
- [ ] Create `src/lib/api/clients.ts` (replace existing, align types)
- [ ] Create `src/lib/api/dashboard.ts` (move `getDashboardStats` here from old clients.ts)
- [ ] Create `src/lib/api/itr.ts`
- [ ] Create `src/lib/api/filing.ts` (replace existing, remove unimplemented calls)
- [ ] Create `src/lib/api/pan.ts`

### Phase 4 — Documents & Integration
- [ ] Create `src/lib/api/documents.ts`
- [ ] Create `src/lib/api/integration.ts`
- [ ] Wire Form 16 upload UI → `integrationApi.extractForm16()` → `integrationApi.autoPopulateFromForm16()`
- [ ] Wire AIS upload UI → `integrationApi.importAIS()` → `integrationApi.autoPopulateFromAIS()`
- [ ] Wire Prefill upload UI → `prefillApi.upload()` → `integrationApi.autoPopulateFromPrefill()`

### Phase 5 — Stub Replacements
- [ ] Create `src/lib/api/notices.ts` (stub)
- [ ] Create `src/lib/api/billing.ts` (stub)
- [ ] Create `src/lib/api/sync.ts` (stub)
- [ ] Create `src/lib/api/jobs.ts` (stub)
- [ ] Create `src/lib/api/reconciliation.ts` (stub)
- [ ] Create `src/lib/api/communication.ts` (stub)
- [ ] Remove any direct `axios` calls in components — replace with the appropriate API module

### Phase 6 — ITR Form Wiring
- [ ] On form load → call `itrApi.getFormData(clientId, year)`
- [ ] On auto-save / "Save Draft" → call `itrApi.saveFormData(clientId, year, formData)`
- [ ] "Compute Tax" button → call `itrApi.computeTax()` → update `taxComputation` section in UI
- [ ] "Validate" button → call `itrApi.validate()` → show errors/warnings in a toast or panel
- [ ] "Download JSON" → call `itrApi.downloadJson()`
- [ ] "Download PDF" → call `itrApi.downloadPdf()`

### Phase 7 — Type Alignment
- [ ] Audit all components using the old `Client` type from `types.ts` — replace with `ClientResponse` from `api.types.ts` or write an adapter function
- [ ] Ensure all `date` fields passed to the backend are formatted as `"YYYY-MM-DD"` strings (use `dayjs` or `date-fns`)
- [ ] Ensure all `assessmentYear` strings are in `"2024-25"` format consistently

---

## Appendix: File Structure Summary

```
src/
├── lib/
│   └── api/
│       ├── axiosInstance.ts        ← shared Axios instance + interceptors
│       ├── tokenManager.ts         ← JWT storage helpers
│       ├── apiError.ts             ← standardized error handling
│       ├── _stubs.ts               ← stub helpers for unimplemented backends
│       ├── auth.ts                 ← /api/auth/*
│       ├── clients.ts              ← /api/clients/*
│       ├── dashboard.ts            ← /api/dashboard/*
│       ├── itr.ts                  ← /api/clients/{id}/itr/{year}/*
│       ├── filing.ts               ← /api/filing/*
│       ├── documents.ts            ← /api/documents/*
│       ├── pan.ts                  ← /api/pan/*
│       ├── integration.ts          ← /api/integration/* + /api/clients/{id}/prefill/*
│       ├── notices.ts              ← STUB (/api/notices/*)
│       ├── billing.ts              ← STUB (/api/billing/*)
│       ├── sync.ts                 ← STUB (/api/sync/*)
│       ├── jobs.ts                 ← STUB (/api/jobs/*)
│       ├── reconciliation.ts       ← STUB (/api/reconciliation/*)
│       └── communication.ts        ← STUB (/api/communication/*)
├── types/
│   ├── api.types.ts                ← all backend DTO interfaces (NEW)
│   └── types.ts                    ← existing frontend-only types (keep, don't delete)
└── components/
    └── ProtectedRoute.tsx          ← auth guard
```

---

> **Final Notes for the Agent:**
> - Never delete `src/types/types.ts` — it contains frontend-only types like `Notice`, `BatchJob`, `Invoice` still used by stub modules and UI components.
> - All date fields going to the backend must be plain ISO strings (`"YYYY-MM-DD"`), not `Date` objects.
> - `OffsetDateTime` fields coming from the backend (e.g., `createdAt`, `updatedAt`) will arrive as ISO-8601 strings — parse them with your date library before display.
> - For multipart/form-data requests, do **not** manually set the `Content-Type` header as `multipart/form-data` if you're using `FormData` — let Axios set the boundary automatically by only specifying `'Content-Type': 'multipart/form-data'` as shown.
> - The base path `/api` is already in `BASE_URL`. Do **not** prefix paths in individual API files with `/api` — use `/clients`, `/auth`, etc.
