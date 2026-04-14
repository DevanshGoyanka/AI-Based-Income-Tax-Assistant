# ITR ERP — Complete Frontend Build Directive
## For Agentic AI: Build & Wire the Entire Frontend

> **Mission:** Scaffold a production-grade React + Vite + TypeScript SPA for a CA/Advocate income-tax practice ERP. Wire every page to the Spring Boot backend running at `http://localhost:8080`. Pixel-perfectly replicate the design tokens and layout from the reference UI. All stub pages must render dummy data gracefully. The tax computation engine must run entirely client-side — never replace it with a backend call.

---

## 0. Absolute Rules

| Rule | Detail |
|---|---|
| ❌ Never touch backend | No `.java`, no config files — frontend only |
| ✅ Exact design replication | Navy/gold dark sidebar, white card content area — pixel-perfect match to reference |
| ✅ Every API call has loading + error state | Skeleton loaders for tables, spinners for forms, red toast for errors |
| ✅ Tax engine is client-side only | `computeTax()` function runs in browser — never call a backend computation API for display |
| ✅ Optimistic UI on save | Keep local state as source of truth; sync to backend in background |
| ✅ All monetary values in Indian format | Use `₹X,XX,XXX` with `en-IN` locale; use DM Mono font for all number cells |
| ✅ Zero hardcoded data in real pages | Dashboard, Clients, Filing — all from API. Stub pages may show mock data clearly labeled |

---

## 1. Tech Stack & Project Setup

### 1.1 Initialize Project
```bash
npm create vite@latest itr-erp-frontend -- --template react-ts
cd itr-erp-frontend
npm install axios react-router-dom@6 dayjs react-hot-toast
npm install -D @types/node
```

### 1.2 Environment Variables
Create `.env`:
```
VITE_API_BASE_URL=http://localhost:8080/api
```

### 1.3 Design Tokens (CSS Variables)
Add to `src/index.css`. These are the EXACT tokens from the reference design — do not alter:

```css
@import url('https://fonts.googleapis.com/css2?family=Crimson+Pro:wght@400;500;600&family=DM+Sans:wght@300;400;500;600&family=DM+Mono:wght@400;500&display=swap');

:root {
  --navy: #0B1929;
  --navy-mid: #112236;
  --navy-light: #1A3150;
  --gold: #C9943A;
  --gold-light: #E8B86D;
  --gold-pale: #FDF3E0;
  --accent-blue: #1D6FA4;
  --accent-teal: #0F766E;
  --accent-rose: #BE185D;
  --success: #15803D;
  --success-bg: #DCFCE7;
  --warning: #B45309;
  --warning-bg: #FEF3C7;
  --danger: #B91C1C;
  --danger-bg: #FEE2E2;
  --info: #1E40AF;
  --info-bg: #DBEAFE;
  --bg: #F4F6F9;
  --bg-card: #FFFFFF;
  --border: #E1E7EF;
  --border-strong: #C8D3E0;
  --text-primary: #0F1E2D;
  --text-secondary: #4A5D72;
  --text-muted: #8A9BB0;
  --sidebar-w: 258px;
  --header-h: 62px;
  --radius: 10px;
  --radius-sm: 6px;
}

*, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
body {
  font-family: 'DM Sans', sans-serif;
  background: var(--bg);
  color: var(--text-primary);
  font-size: 14px;
  line-height: 1.5;
}
.crimson { font-family: 'Crimson Pro', serif; }
.mono { font-family: 'DM Mono', monospace; }

/* Scrollbar */
::-webkit-scrollbar { width: 5px; height: 5px; }
::-webkit-scrollbar-thumb { background: var(--border-strong); border-radius: 3px; }

/* Badges */
.badge { display:inline-flex;align-items:center;gap:4px;padding:3px 9px;border-radius:20px;font-size:11.5px;font-weight:600;white-space:nowrap; }
.badge-success { background:var(--success-bg);color:var(--success); }
.badge-warning { background:var(--warning-bg);color:var(--warning); }
.badge-danger  { background:var(--danger-bg);color:var(--danger); }
.badge-info    { background:var(--info-bg);color:var(--info); }
.badge-gold    { background:var(--gold-pale);color:#92640A; }
.badge-muted   { background:var(--bg);color:var(--text-secondary);border:1px solid var(--border); }
.badge-navy    { background:#E0E8F4;color:var(--navy); }
.badge-dot::before { content:'';width:5px;height:5px;border-radius:50%;background:currentColor;display:inline-block; }

/* Tables */
table { width:100%;border-collapse:collapse; }
thead th { text-align:left;padding:10px 14px;font-size:11.5px;font-weight:600;color:var(--text-muted);text-transform:uppercase;letter-spacing:0.5px;background:var(--bg);border-bottom:1px solid var(--border);white-space:nowrap; }
tbody td { padding:12px 14px;font-size:13.5px;border-bottom:1px solid var(--border);color:var(--text-primary);vertical-align:middle; }
tbody tr:last-child td { border-bottom:none; }
tbody tr:hover { background:#FAFBFD; }

/* Progress */
.progress-bar { height:8px;background:var(--bg);border-radius:4px;overflow:hidden; }
.progress-fill { height:100%;border-radius:4px; }

/* Animations */
@keyframes pulse { 0%,100%{opacity:1;transform:scale(1)} 50%{opacity:0.6;transform:scale(1.3)} }
@keyframes slideIn { from{transform:translateX(100%);opacity:0} to{transform:translateX(0);opacity:1} }
@keyframes spin { from{transform:rotate(0deg)} to{transform:rotate(360deg)} }
```

---

## 2. Folder Structure

Create exactly this structure:

```
src/
├── lib/
│   └── api/
│       ├── axiosInstance.ts
│       ├── tokenManager.ts
│       ├── apiError.ts
│       ├── _stubs.ts
│       ├── auth.ts
│       ├── clients.ts
│       ├── dashboard.ts
│       ├── itr.ts
│       ├── filing.ts
│       ├── documents.ts
│       ├── pan.ts
│       ├── integration.ts
│       ├── notices.ts      (stub)
│       ├── billing.ts      (stub)
│       ├── sync.ts         (stub)
│       ├── jobs.ts         (stub)
│       ├── reconciliation.ts (stub)
│       └── communication.ts  (stub)
├── types/
│   └── api.types.ts
├── components/
│   ├── layout/
│   │   ├── Sidebar.tsx
│   │   ├── Topbar.tsx
│   │   └── AppLayout.tsx
│   ├── ui/
│   │   ├── Badge.tsx
│   │   ├── Spinner.tsx
│   │   ├── SkeletonRow.tsx
│   │   ├── Toast.tsx
│   │   ├── Modal.tsx
│   │   └── EmptyState.tsx
│   └── ProtectedRoute.tsx
├── pages/
│   ├── LoginPage.tsx
│   ├── DashboardPage.tsx
│   ├── ClientsPage.tsx
│   ├── FilingPage.tsx
│   ├── ITRComputationPage.tsx
│   ├── ReconciliationPage.tsx
│   ├── SyncPage.tsx
│   ├── JobsPage.tsx
│   ├── NoticesPage.tsx
│   ├── CalendarPage.tsx
│   ├── TasksPage.tsx
│   ├── BillingPage.tsx
│   ├── AccountingPage.tsx
│   ├── ReportsPage.tsx
│   └── CommunicationPage.tsx
├── hooks/
│   ├── useClients.ts
│   ├── useDashboard.ts
│   └── useITRForm.ts
├── utils/
│   └── formatters.ts
├── App.tsx
└── main.tsx
```

---

## 3. API Layer — Complete Implementation

### 3.1 `src/lib/api/axiosInstance.ts`
```typescript
import axios from 'axios';

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api';

const axiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
});

axiosInstance.interceptors.request.use((config) => {
  const token = localStorage.getItem('auth_token');
  if (token && config.headers) {
    config.headers['Authorization'] = `Bearer ${token}`;
  }
  return config;
});

axiosInstance.interceptors.response.use(
  (res) => res,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.clear();
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default axiosInstance;
```

### 3.2 `src/lib/api/tokenManager.ts`
```typescript
export const tokenManager = {
  save(token: string, email: string, expiresIn: number) {
    localStorage.setItem('auth_token', token);
    localStorage.setItem('auth_email', email);
    localStorage.setItem('auth_expiry', String(Date.now() + expiresIn * 1000));
  },
  getToken: () => localStorage.getItem('auth_token'),
  getEmail: () => localStorage.getItem('auth_email'),
  isExpired() {
    const e = localStorage.getItem('auth_expiry');
    return !e || Date.now() > parseInt(e);
  },
  isAuthenticated() { return !!this.getToken() && !this.isExpired(); },
  clear() { localStorage.clear(); },
};
```

### 3.3 `src/lib/api/apiError.ts`
```typescript
import { AxiosError } from 'axios';

export class ApiError extends Error {
  constructor(public status: number, public body: any) {
    super(body?.message ?? body?.error ?? 'An unexpected error occurred');
    this.name = 'ApiError';
  }
}

export async function handleApiCall<T>(fn: () => Promise<T>): Promise<T> {
  try {
    return await fn();
  } catch (err) {
    if (err instanceof AxiosError && err.response) {
      throw new ApiError(err.response.status, err.response.data);
    }
    throw err;
  }
}
```

### 3.4 `src/lib/api/_stubs.ts`
```typescript
export class NotImplementedError extends Error {
  constructor(endpoint: string) {
    super(`Backend endpoint not implemented: ${endpoint}`);
    this.name = 'NotImplementedError';
  }
}

export function stub<T>(endpoint: string, fallback: T): T {
  console.warn(`[STUB] ${endpoint} — returning mock data.`);
  return fallback;
}
```

### 3.5 `src/lib/api/auth.ts`
```typescript
import axiosInstance from './axiosInstance';

export const authApi = {
  login: async (email: string, password: string) => {
    const { data } = await axiosInstance.post('/auth/login', { email, password });
    return data as { token: string; email: string; expiresIn: number };
  },
  register: async (email: string, password: string) => {
    const { data } = await axiosInstance.post('/auth/register', { email, password });
    return data as { token: string; email: string; expiresIn: number };
  },
};
```

### 3.6 `src/lib/api/clients.ts`
```typescript
import axiosInstance from './axiosInstance';

export const clientsApi = {
  list: async (params?: { search?: string; assessmentYear?: string; status?: string }) => {
    const { data } = await axiosInstance.get('/clients', { params });
    return data as ClientResponse[];
  },
  get: async (id: number) => {
    const { data } = await axiosInstance.get(`/clients/${id}`);
    return data as ClientResponse;
  },
  create: async (payload: ClientRequest) => {
    const { data } = await axiosInstance.post('/clients', payload);
    return data as ClientResponse;
  },
  update: async (id: number, payload: Partial<ClientRequest>) => {
    const { data } = await axiosInstance.put(`/clients/${id}`, payload);
    return data as ClientResponse;
  },
  delete: async (id: number) => {
    await axiosInstance.delete(`/clients/${id}`);
  },
  getYears: async (id: number) => {
    const { data } = await axiosInstance.get(`/clients/${id}/years`);
    return data as YearStatus[];
  },
  analyzepan: async (id: number) => {
    const { data } = await axiosInstance.get(`/clients/${id}/pan-analysis`);
    return data;
  },
};
```

### 3.7 `src/lib/api/dashboard.ts`
```typescript
import axiosInstance from './axiosInstance';

export const dashboardApi = {
  getStats: async (assessmentYear?: string) => {
    const { data } = await axiosInstance.get('/dashboard/stats', {
      params: assessmentYear ? { ay: assessmentYear } : undefined,
    });
    return data as {
      total: number; filed: number; inProgress: number;
      docPending: number; watchList: number; totalMismatches: number; totalNotices: number;
    };
  },
};
```

### 3.8 `src/lib/api/itr.ts`
```typescript
import axiosInstance from './axiosInstance';

export const itrApi = {
  getFormData: async (clientId: number, year: string) => {
    const { data } = await axiosInstance.get(`/clients/${clientId}/itr/${year}`);
    return data;
  },
  saveFormData: async (clientId: number, year: string, formData: any) => {
    const { data } = await axiosInstance.put(`/clients/${clientId}/itr/${year}`, formData);
    return data;
  },
  computeTax: async (clientId: number, year: string, formData: any) => {
    const { data } = await axiosInstance.post(`/clients/${clientId}/itr/${year}/compute`, formData);
    return data;
  },
  validate: async (clientId: number, year: string, formData: any) => {
    const { data } = await axiosInstance.post(`/clients/${clientId}/itr/${year}/validate`, formData);
    return data as { valid: boolean; errors: string[]; warnings: string[] };
  },
  downloadJson: async (clientId: number, year: string) => {
    const res = await axiosInstance.get(`/clients/${clientId}/itr/${year}/download`, { responseType: 'blob' });
    const url = URL.createObjectURL(new Blob([res.data]));
    const a = document.createElement('a');
    a.href = url; a.download = `ITR_${clientId}_${year}.json`;
    document.body.appendChild(a); a.click(); a.remove();
    URL.revokeObjectURL(url);
  },
  downloadPdf: async (clientId: number, year: string) => {
    const res = await axiosInstance.get(`/clients/${clientId}/itr/${year}/download-pdf`, { responseType: 'blob' });
    const url = URL.createObjectURL(new Blob([res.data], { type: 'application/pdf' }));
    const a = document.createElement('a');
    a.href = url; a.download = `ITR_${clientId}_${year}.pdf`;
    document.body.appendChild(a); a.click(); a.remove();
    URL.revokeObjectURL(url);
  },
};
```

### 3.9 `src/lib/api/filing.ts`
```typescript
import axiosInstance from './axiosInstance';

export const filingApi = {
  list: async (filters?: { clientId?: number; assessmentYear?: string; status?: string }) => {
    const { data } = await axiosInstance.get('/filing', { params: filters });
    return data as FilingResponse[];
  },
  create: async (payload: FilingRequest) => {
    const { data } = await axiosInstance.post('/filing', payload);
    return data as FilingResponse;
  },
  update: async (id: number, payload: Partial<FilingRequest>) => {
    const { data } = await axiosInstance.put(`/filing/${id}`, payload);
    return data as FilingResponse;
  },
  delete: async (id: number) => {
    await axiosInstance.delete(`/filing/${id}`);
  },
};
```

### 3.10 `src/lib/api/documents.ts`
```typescript
import axiosInstance from './axiosInstance';

export const documentsApi = {
  upload: async (file: File, clientId: number, assessmentYear: string, documentType: string) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('clientId', String(clientId));
    formData.append('assessmentYear', assessmentYear);
    formData.append('documentType', documentType);
    const { data } = await axiosInstance.post('/documents/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return data;
  },
  list: async (clientId: number, assessmentYear: string, documentType?: string) => {
    const { data } = await axiosInstance.get('/documents/list', {
      params: { clientId, assessmentYear, documentType },
    });
    return data as DocumentMetadata[];
  },
  download: async (documentId: number, fileName: string) => {
    const res = await axiosInstance.get(`/documents/download/${documentId}`, { responseType: 'blob' });
    const url = URL.createObjectURL(new Blob([res.data]));
    const a = document.createElement('a');
    a.href = url; a.download = fileName;
    document.body.appendChild(a); a.click(); a.remove();
    URL.revokeObjectURL(url);
  },
  delete: async (documentId: number) => {
    await axiosInstance.delete(`/documents/${documentId}`);
  },
};
```

### 3.11 `src/lib/api/pan.ts`
```typescript
import axiosInstance from './axiosInstance';

export const panApi = {
  validate: async (pan: string) => {
    const { data } = await axiosInstance.get(`/pan/${pan}/validate`);
    return data as { pan: string; valid: boolean; message?: string };
  },
  analyze: async (pan: string) => {
    const { data } = await axiosInstance.get(`/pan/${pan}/analyze`);
    return data as {
      pan: string; valid: boolean; entityType: string;
      entityDescription: string; isIndividualOrHUF: boolean;
      eligibleITRForms: string[]; warnings: string[];
    };
  },
};
```

### 3.12 `src/lib/api/integration.ts`
```typescript
import axiosInstance from './axiosInstance';

const multipartPost = async (endpoint: string, file: File) => {
  const fd = new FormData();
  fd.append('file', file);
  const { data } = await axiosInstance.post(endpoint, fd, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return data;
};

export const integrationApi = {
  extractForm16: (file: File) => multipartPost('/integration/form16/extract', file),
  importAIS: (file: File) => multipartPost('/integration/ais/import', file),
  import26AS: (file: File) => multipartPost('/integration/26as/import', file),
  importITDPrefill: (file: File) => multipartPost('/integration/prefill/import', file),
  autoPopulateFromForm16: async (itrData: any, form16Data: any) => {
    const { data } = await axiosInstance.post('/integration/autopopulate/form16', { itrData, form16Data });
    return data;
  },
  autoPopulateFromAIS: async (itrData: any, aisData: any) => {
    const { data } = await axiosInstance.post('/integration/autopopulate/ais', { itrData, aisData });
    return data;
  },
};
```

### 3.13 Stub API Files

Create these files — each returns mock/empty data. This prevents 404 crashes on pages that reference unimplemented backend endpoints.

**`src/lib/api/notices.ts`** — stub, returns `[]`
**`src/lib/api/billing.ts`** — stub, returns `[]`
**`src/lib/api/sync.ts`** — stub, returns `{}`
**`src/lib/api/jobs.ts`** — stub, returns `[]`
**`src/lib/api/reconciliation.ts`** — stub, returns `{}`
**`src/lib/api/communication.ts`** — stub, returns `{}`

Each stub uses: `import { stub } from './_stubs'; export const xApi = { list: async () => stub('/api/x', []) };`

---

## 4. TypeScript Types — `src/types/api.types.ts`

Define these interfaces (complete list):

```typescript
export interface ClientRequest {
  pan: string;
  name: string;
  email?: string;
  mobile?: string;
  aadhaar?: string;
  dob?: string; // "YYYY-MM-DD"
}

export interface YearStatus {
  year: string;     // "2024-25"
  status: string;   // "Filed" | "In Progress" | "Not Started" | "Doc Pending"
  itrType: string;  // "ITR-1" .. "ITR-4"
}

export interface ClientResponse {
  id: number;
  pan: string;
  name: string;
  email: string;
  mobile: string;
  aadhaar: string;
  dob: string;
  years: YearStatus[];
}

export interface FilingRequest {
  clientId: number;
  assessmentYear: string;
  itrType: string;
  status: string;
  filingDate?: string;
  acknowledgementNumber?: string;
}

export interface FilingResponse extends FilingRequest {
  id: number;
  clientName: string;
  clientPan: string;
  totalIncome?: number;
  taxPayable?: number;
  refundAmount?: number;
  createdAt: string;
  updatedAt: string;
}

export interface DocumentMetadata {
  id: number;
  clientId: number;
  assessmentYear: string;
  documentType: string;
  fileName: string;
  fileSize: number;
  uploadedAt: string;
  url?: string;
}
```

---

## 5. Routing — `src/App.tsx`

```tsx
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { ProtectedRoute } from './components/ProtectedRoute';
import { AppLayout } from './components/layout/AppLayout';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import ClientsPage from './pages/ClientsPage';
import FilingPage from './pages/FilingPage';
import ITRComputationPage from './pages/ITRComputationPage';
// ... import all other pages

export default function App() {
  return (
    <BrowserRouter>
      <Toaster position="top-right" />
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route element={<ProtectedRoute />}>
          <Route element={<AppLayout />}>
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/clients" element={<ClientsPage />} />
            <Route path="/filing" element={<FilingPage />} />
            <Route path="/filing/:clientId/:year" element={<ITRComputationPage />} />
            <Route path="/reconciliation" element={<ReconciliationPage />} />
            <Route path="/sync" element={<SyncPage />} />
            <Route path="/jobs" element={<JobsPage />} />
            <Route path="/notices" element={<NoticesPage />} />
            <Route path="/calendar" element={<CalendarPage />} />
            <Route path="/tasks" element={<TasksPage />} />
            <Route path="/billing" element={<BillingPage />} />
            <Route path="/accounting" element={<AccountingPage />} />
            <Route path="/reports" element={<ReportsPage />} />
            <Route path="/communication" element={<CommunicationPage />} />
          </Route>
        </Route>
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
```

---

## 6. Layout Components

### 6.1 `src/components/layout/AppLayout.tsx`
Shell with fixed sidebar (258px wide) + scrollable main area. Uses `<Outlet />` for page content.

```tsx
// Fixed sidebar on left (var(--sidebar-w) = 258px)
// Top header bar (var(--header-h) = 62px)  
// Scrollable content area to the right of sidebar, below header
// Structure:
// <div style={{display:'flex',height:'100vh',overflow:'hidden'}}>
//   <Sidebar />
//   <div style={{flex:1,display:'flex',flexDirection:'column',overflow:'hidden'}}>
//     <Topbar />
//     <main style={{flex:1,overflowY:'auto',padding:'24px'}}>
//       <Outlet />
//     </main>
//   </div>
// </div>
```

### 6.2 `src/components/layout/Sidebar.tsx`

Exact design from reference. Navy background (`var(--navy)`), 258px wide, fixed position.

**Brand area** (top, 16px padding):
- Square gold icon (36×36, rounded 8px, `var(--gold)` background) with "IT" in Crimson Pro
- Brand name: "IncomeTax ERP" in Crimson Pro 16px white
- Sub: "Advocate Practice" in gold-light 10.5px uppercase

**Search bar** below brand:
- Dark input, search icon, placeholder "Search clients, PAN…"
- On input change (300ms debounce): `GET /api/clients?search={q}`

**Navigation sections** (exact order and grouping):

```
── Overview ──────────────────────
  Dashboard          (route: /dashboard)

── Clients & Filing ──────────────
  Client Master      (route: /clients)     badge: live count from API
  ITR Filing         (route: /filing)      badge: inProgress count (blue)
  Reconciliation     (route: /reconciliation) badge: mismatch count (red)

── Automation ────────────────────
  ITD Portal Sync    (route: /sync)
  Background Jobs    (route: /jobs)        badge: running count (blue)

── Compliance ────────────────────
  Notice Management  (route: /notices)     badge: active notices (red)
  Compliance Calendar (route: /calendar)
  Tasks & Work Queue  (route: /tasks)      badge: pending count

── Finance ───────────────────────
  Billing & Fees     (route: /billing)
  Firm Accounting    (route: /accounting)

── Analytics ─────────────────────
  Reports & Analytics (route: /reports)
  Communication      (route: /communication)
```

**Active state**: left gold border + gold text + subtle gold-tinted background
**Nav icons**: inline SVG, 18×18, stroke-based, `var(--text-muted)` default → white on active/hover

**Footer** (bottom, pinned):
- User avatar circle (gold bg, navy text, user initials)
- User name + role (fetched from `tokenManager.getEmail()`)
- Settings/logout icon button → `tokenManager.clear()` then navigate to `/login`

### 6.3 `src/components/layout/Topbar.tsx`

Height: 62px, white background, bottom border `var(--border)`.

**Left**: breadcrumb showing current page name (derived from route)
**Center**: AY selector `<select>`:
- Options: "AY 2025–26", "AY 2026–27", "Both AYs"
- Store selected AY in React context (AYContext)
- On change: refetch all data-driven views with the new AY param
- Map display → param: `{ "AY 2025–26": "2025-26", "AY 2026–27": "2026-27", "Both AYs": null }`

**Right**: "Run Bulk Sync" button (gold, small) → `POST /api/sync/bulk` stub

---

## 7. Auth Pages

### 7.1 `src/pages/LoginPage.tsx`

Full-screen split layout:
- **Left panel** (40%): navy background, gold "IT" brand logo, tagline "Trusted by 500+ CA Firms across India", subtle wave/grid decoration
- **Right panel** (60%): white, centered form

Login form fields:
- Email input (label: "Email Address")
- Password input with show/hide toggle
- "Sign In" button (full width, gold background)
- "Don't have an account? Register" link

On submit:
1. Call `authApi.login(email, password)`
2. On success: `tokenManager.save(data.token, data.email, data.expiresIn)` then navigate to `/dashboard`
3. On error: show red toast with error message and shake animation on form
4. Show spinner inside button while loading; disable button

### 7.2 Register page: same layout, with confirm password field. Call `authApi.register()`.

---

## 8. Dashboard Page — `src/pages/DashboardPage.tsx`

This is the first page users see after login. All data comes from API calls.

### 8.1 AY Filing Season Progress Bar (top strip)

A thin gold progress bar banner with text:
```
AY 2025-26 Filing Season: [████████░░] 67% complete
Clients filed: {stats.filed} of {stats.total} | {stats.inProgress} in progress | {stats.docPending} doc pending
```
Fetch from `GET /api/dashboard/stats?ay={currentAY}`

### 8.2 Four Stat Cards (2×2 grid, or row of 4)

| Label | Value Source | Icon |
|---|---|---|
| Total Clients | `stats.total` | Users icon |
| Filed ✓ | `stats.filed` | Checkmark |
| In Progress | `stats.inProgress` | Clock |
| Doc Pending | `stats.docPending` | Document |

Card style: white background, `var(--radius)` border-radius, subtle shadow, left-border accent color, large bold number in DM Mono, muted label below. Show skeleton while loading.

### 8.3 Filing Progress Pipeline (horizontal flow)

5 boxes in a row with right-arrow connectors:
```
Total [N] → Filed [N] → In Progress [N] → Doc Pending [N] → Mismatch [N]
```
Use `stats` fields for counts. While loading, show gray placeholder rectangles.

### 8.4 Bottom Row (2 columns)

**Left: Recent Activity Feed** (larger column)
- Call `GET /api/dashboard/stats` (backend returns limited data — use what's available)
- Show a vertical list of activity items, each with a colored dot, bold client name, action text, and timestamp
- If backend returns empty activity, show a friendly "No recent activity" empty state with an icon
- Each item: dot (colored per activity type) + text + relative time ("2 min ago")

**Right Column (stacked)**:

*ITR Type Breakdown card*:
- Horizontal bar chart showing ITR-1/2/3/4 distribution
- Derive from `stats` — if breakdown not available, show placeholder bars with label "Data loading…"

*Today's Deadlines card*:
- 3–5 deadline items with urgency color (red = urgent, amber = warning, blue = info)
- Backend may not return these — fallback to static standard tax deadlines for AY 2025-26:
  - "ITR Filing Deadline" — 31 Jul 2025 — urgent
  - "Advance Tax Q1" — 15 Jun 2025 — info
  - "Form 16 Issuance" — 15 Jun 2025 — warning

### 8.5 Running Jobs Quick View (bottom strip)

Shows a small horizontal strip with background job status badges. Call stub `jobsApi.list()` → shows empty state if no jobs.

---

## 9. Clients Page — `src/pages/ClientsPage.tsx`

### 9.1 Page Header
- Title "Client Master" (Crimson Pro 22px)
- Right side: Search input + "Add Client" button (gold)

### 9.2 Filter Bar
Horizontal pill filters:
- Status: All | Filed | In Progress | Doc Pending | Mismatch | Ready to File
- ITR Form: All | ITR-1 | ITR-2 | ITR-3 | ITR-4
- AY: from AY context

### 9.3 Client Table

On mount: `GET /api/clients?assessmentYear={ay}` (refetch on filter change, search, or AY change)

Show **skeleton rows** (5 animated gray bars) while loading.

| Column | Data | Notes |
|---|---|---|
| Client | Initials avatar (gold circle) + Name + PAN below in mono | Initials = first letter of each word in name |
| PAN | Monospace font, styled | `DEFPV1122K` |
| Type | Badge | Derived from PAN 4th character: P=Individual, H=HUF, F=Firm |
| ITR Form | Badge (navy) | From `years[0].itrType` |
| AY Status | Colored badge | From `years[0].status` |
| Last Sync | Text | Format as "Apr 2" using dayjs |
| Actions | Three icon buttons | Open ITR · Edit · Delete |

**Status badge colors**:
```
"Filed"          → badge-success
"In Progress"    → badge-info
"Doc Pending"    → badge-warning  
"Mismatch"       → badge-danger
"Ready to File"  → badge-gold
"Not Started"    → badge-muted
```

### 9.4 Add Client Modal

Triggered by "Add Client" button. Full-screen overlay modal.

Fields:
- **PAN** — text input, 10 chars, uppercase. On blur (when 10 chars): call `panApi.analyze(pan)` → auto-fill Entity Type badge + show eligible ITR forms. Show green checkmark if valid, red ✕ if invalid.
- **Name** — text
- **Email** — email input
- **Mobile** — tel input, 10 digits
- **Aadhaar** — text, 12 digits (masked: XXXX-XXXX-1234 display)
- **Date of Birth** — date picker
- **Assessment Year** — dropdown pre-filled from AY context

On submit: `POST /api/clients` → on success close modal + refresh table + show green toast "Client {name} added successfully"

### 9.5 Edit Client

Same modal, pre-filled. `PUT /api/clients/{id}` on save.

### 9.6 Delete Client

Confirmation modal: "Are you sure you want to delete {name}? This cannot be undone." → `DELETE /api/clients/{id}` → refresh + toast

### 9.7 Click Row → Open ITR Computation

Click "Open ITR" action icon or row → navigate to `/filing/{clientId}/{year}` (use the client's latest year).

---

## 10. Filing Page — `src/pages/FilingPage.tsx`

### 10.1 Filing Pipeline Header

Horizontal kanban-style stage pipeline:
```
[Mismatch Review] [Reconciliation] [Ready to File] [Doc Pending] [Filed ✓]
```
Each stage is a card showing count of clients in that stage.

### 10.2 Filing Table

`GET /api/filing?assessmentYear={ay}` — if returns empty, also try `GET /api/clients?assessmentYear={ay}` and filter client-side by status.

| Column | Data |
|---|---|
| Client | Name + PAN |
| ITR Form | Badge |
| Filing Stage | Colored badge |
| Recon Status | Badge |
| Tax Payable / Refund | ₹ formatted in DM Mono; green for refund, amber for payable |
| Last Updated | Relative date |
| Actions | "Open ITR" button (primary) · Status dropdown |

### 10.3 Stage Drag Actions

Status update: clicking "Move to Next Stage" calls `PUT /api/filing/{id}` with updated status.

---

## 11. ITR Computation Page — `src/pages/ITRComputationPage.tsx`

This is the most complex page. It is a full-screen form for entering and computing ITR data for one client.

### 11.1 Top Action Bar

Fixed bar at top of the computation view:
```
[← Back to Filing]  [Client Name · PAN · AY Badge · ITR Form Badge]
                    [⬆ AIS/TIS Import] [📄 Form 16 Import] [🔍 Form 16 PDF]
                    [Regime: OLD/NEW toggle] [AY selector]
                    [✓ Validate ITR] [💾 Save] [⬇ JSON] [📊 PDF]
```

**Save button behavior**:
- Shows spinner while saving
- Calls `PUT /api/clients/{clientId}/itr/{year}` with current form data
- On success: shows green toast "Saved ✓"
- Auto-save on tab change (2s debounce)

**Import buttons**:
- AIS/TIS → file picker (JSON) → `POST /api/integration/ais/import` → merge into form state
- Form 16 JSON → `POST /api/integration/form16/extract` → `POST /api/integration/autopopulate/form16`
- Form 16 PDF → multipart → same auto-populate flow
- Show blue import banner after successful import: "✓ Auto-populated from Form 16 — Review and confirm"

**Validate button**: calls `POST /api/clients/{clientId}/itr/{year}/validate` → shows errors in a panel below the tab bar. Valid = green "All checks passed" banner. Errors = red list.

### 11.2 Section Tabs

Dark navy tab bar (matching ITR form sections). Tabs:
1. 📋 Personal Info
2. 💼 Salary Income
3. 🏠 House Property
4. 📈 Capital Gains
5. 🏪 Business / Presumptive
6. 💰 Other Sources
7. ₿ VDA / Crypto
8. ➖ Deductions (80C/D/E...)
9. 📉 Losses B/F
10. 🧾 TDS & Advance Tax
11. 🧮 Tax Computation (read-only)

Each tab shows the section-level computed total in small mono text (e.g., "₹8,40,000") next to the tab label.

Active tab: gold underline + gold text + subtle gold-tinted background.

### 11.3 On Mount — Load Saved Data

```typescript
useEffect(() => {
  if (!clientId) return;
  setLoading(true);
  itrApi.getFormData(Number(clientId), year)
    .then(data => setFormData(prev => ({ ...prev, ...data })))
    .catch(err => toast.error(err.message))
    .finally(() => setLoading(false));
}, [clientId, year]);
```

Show spinner overlay on entire form area while loading.

### 11.4 Field Component

A reusable `<Field>` component:
- Props: `label`, `value`, `onChange`, `computed?`, `prefix?` (defaults to "₹")
- Computed fields: gold-pale background, non-editable, cursor:default — NEVER wired to backend writes
- Normal fields: white background, editable, calls `onChange(newValue)` → updates local `formData` state
- All currency fields have ₹ prefix, formatted with `en-IN` locale on blur

### 11.5 Tax Engine (`computeTax` function)

**This runs entirely client-side in a `useMemo`. Never replace with API call.**

```typescript
const taxResult = useMemo(() => computeTax(formData, ay, regime), [formData, ay, regime]);
```

Implement this engine exactly as specified in the original `ITRDashboard.jsx`:

```typescript
function computeTax(d: any, ay: string, regime: string) {
  // HRA Exempt calculation
  const hraExempt = Math.min(
    d.hra || 0,
    Math.max(0, (d.hraRent || 0) - 0.1 * ((d.basic || 0) + (d.da || 0))),
    (d.hraMetro ? 0.5 : 0.4) * ((d.basic || 0) + (d.da || 0))
  );
  
  // Gross salary
  const grossSalary = (d.basic||0)+(d.da||0)+(d.hra||0)+(d.bonus||0)+(d.allowances||0)+(d.perquisites||0);
  const stdDed = 75000;
  const netSalary = Math.max(0, grossSalary - hraExempt - stdDed - Math.min(d.profTax||0, 2500));

  // House Property
  let hpIncome = d.hpType === 'letout'
    ? Math.max(0,(d.grossRent||0)-(d.munTax||0)) * 0.70 - (d.homeLoanInt||0)
    : -Math.min(d.sopLoanInt||0, 200000);

  // Capital Gains
  const stcg111A = ((d.stcgPre||0)*0.15) + ((d.stcgPost||0)*0.20);
  const ltcgExempt = ay === '2026-27' ? 125000 : 100000;
  const ltcg112A_tax = Math.max(0,(d.ltcgPre||0)+(d.ltcgPost||0)-ltcgExempt) * (ay === '2026-27' ? 0.125 : 0.10);
  const cgTax = stcg111A + ltcg112A_tax + (d.stcgOther||0)*0.20 + (d.ltcgOther||0)*0.125;
  
  // Business
  const bizIncome = d.bizPresumptive === '44AD'
    ? Math.max((d.bizTurnover||0)*0.08, d.bizDeclared||0)
    : d.bizPresumptive === '44ADA'
    ? Math.max((d.bizTurnover||0)*0.50, d.bizDeclared||0)
    : (d.bpNetProfit||0);
  
  // Other sources
  const otherIncome = (d.interestSB||0)+(d.interestFD||0)+(d.dividends||0)+(d.familyPension||0)+(d.otherMisc||0);
  
  // VDA
  const vdaGains = Math.max(0, d.vdaGains||0);
  const vdaTax = vdaGains * 0.30;
  
  // GTI
  const gti = netSalary + Math.max(0, hpIncome) + otherIncome + bizIncome;
  const hpLoss = hpIncome < 0 ? Math.abs(hpIncome) : 0;
  const bfLoss = d.bfLoss || 0;
  const gtiAfterSetOff = Math.max(0, gti - hpLoss - bfLoss);
  
  // Deductions (Old Regime only)
  let totalDeductions = 0;
  if (regime === 'old') {
    const s80C = Math.min(150000, (d.s80C_epf||0)+(d.s80C_ppf||0)+(d.s80C_elss||0)+(d.s80C_lic||0)+(d.s80C_home||0));
    const s80CCD1B = Math.min(50000, d.s80CCD1B||0);
    const s80D = (d.s80D_self||0) + (d.s80D_parent||0);
    totalDeductions = s80C + s80CCD1B + (d.s80CCD2||0) + s80D + (d.s80E||0) + Math.min(10000, d.s80TTA||0) + (d.s80G||0);
  }
  
  const totalIncome = Math.max(0, gtiAfterSetOff - totalDeductions);
  
  // Tax slabs
  let normalTax = 0;
  if (regime === 'new') {
    // New regime slabs AY 2026-27
    if (ay === '2026-27') {
      if (totalIncome > 2400000) normalTax = (totalIncome - 2400000)*0.30 + 540000;
      else if (totalIncome > 2000000) normalTax = (totalIncome - 2000000)*0.25 + 440000;
      else if (totalIncome > 1600000) normalTax = (totalIncome - 1600000)*0.20 + 280000;
      else if (totalIncome > 1200000) normalTax = (totalIncome - 1200000)*0.15 + 120000;
      else if (totalIncome > 800000) normalTax = (totalIncome - 800000)*0.10 + 40000;
      else if (totalIncome > 400000) normalTax = (totalIncome - 400000)*0.05;
    } else {
      // AY 2025-26 new regime
      if (totalIncome > 1500000) normalTax = (totalIncome - 1500000)*0.30 + 150000;
      else if (totalIncome > 1200000) normalTax = (totalIncome - 1200000)*0.20 + 90000;
      else if (totalIncome > 900000) normalTax = (totalIncome - 900000)*0.15 + 45000;
      else if (totalIncome > 600000) normalTax = (totalIncome - 600000)*0.10 + 15000;
      else if (totalIncome > 300000) normalTax = (totalIncome - 300000)*0.05;
    }
  } else {
    // Old regime
    const age = d.age || 30;
    const basic_exempt = age >= 80 ? 500000 : age >= 60 ? 300000 : 250000;
    if (totalIncome > 1000000) normalTax = (totalIncome - 1000000)*0.30 + 112500;
    else if (totalIncome > 500000) normalTax = (totalIncome - 500000)*0.20 + 12500;
    else if (totalIncome > basic_exempt) normalTax = (totalIncome - basic_exempt)*0.05;
  }
  
  // Rebate 87A
  const rebateLimit = (regime === 'new' && ay === '2026-27') ? 1200000 : (regime === 'new' ? 700000 : 500000);
  const rebate87A = totalIncome <= rebateLimit ? Math.min(normalTax, (regime === 'new' ? 60000 : 12500)) : 0;
  
  const taxAfterRebate = Math.max(0, normalTax - rebate87A);
  
  // Surcharge
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
    normalTax, rebate87A, surcharge, cess, totalTaxLiability, totalTaxPaid, taxPayable, refund,
  };
}
```

### 11.6 Section Layouts

#### Personal Info Tab
2-column grid of Field components:
- Assessee Name, PAN, Aadhaar, DOB, Age (auto-computed from DOB), Email, Mobile
- Address: Flat/Door, Premises, Road, Area, City, State, PIN
- Filing Type, Residential Status (dropdown: ROR / RNOR / NR), Employer Category
- Bank: IFSC, Bank Name, Account Number, Account Type
- Assessment Year (read-only, from route), Financial Year (auto: AY minus 1)

#### Salary Income Tab
3-column grid:
- Basic (₹), DA (₹), HRA Received (₹), Bonus/Incentives (₹), Special Allowances (₹), Perquisites (₹)
- Computed: Gross Salary (gold field)
- HRA Rent Paid p.a., Metro City? (checkbox)
- Professional Tax
- Computed: HRA Exempt (gold), Standard Deduction (gold, fixed ₹75,000), Net Taxable Salary (gold)

#### House Property Tab
- Type selector: "Self Occupied" / "Let Out" / "Deemed Let Out" (toggle pills)
- If Let Out: Gross Annual Rent, Municipal Tax Paid, Computed: NAV, Computed: 30% Std Deduction, Home Loan Interest
- If Self Occupied: SOP Loan Interest (capped at ₹2L), Pre-Construction Interest
- Computed: Net HP Income (gold — can be negative/loss)

#### Capital Gains Tab
2 sub-sections: STCG and LTCG
- STCG: Pre 23 Jul 2024 (15% rate), Post 23 Jul 2024 (20% rate), Other Assets (20%)
- LTCG: Pre 23 Jul 2024, Post 23 Jul 2024, Other Assets
- Computed: LTCG Exempt (₹1,00,000 or ₹1,25,000 per AY), Total CG Tax (gold)
- Small info note: "Rate change effective 23 Jul 2024 per Finance Act 2024"

#### Business / Presumptive Tab
- Scheme selector: 44AD / 44ADA / Regular Books
- Gross Turnover/Receipts, Digital Receipts % toggle
- Declared Income (minimum required shown as tooltip)
- For Regular: Net Profit from P&L
- Computed: Taxable Business Income (gold)

#### Other Sources Tab
- SB Interest, FD Interest, IT Refund Interest
- Dividend Income, Family Pension
- Other Misc Income
- Computed: Total Other Sources Income (gold)

#### VDA / Crypto Tab
- Sale Consideration, Cost of Acquisition, Net VDA Gains
- TDS u/s 194S
- Computed: VDA Tax @ 30% (gold), No loss set-off allowed (info note)

#### Deductions Tab
Show this tab only when Regime = "OLD". Show a note "No deductions in New Regime (except 80CCD(2))" when NEW.

Grouped sub-sections:
- **80C** (max ₹1.5L cap): EPF, PPF, ELSS, LIC, Home Loan Principal, Tuition Fees
- **NPS**: 80CCD(1B) max ₹50K, 80CCD(2) — employer contribution
- **Health Insurance 80D**: Self+Family, Parents (enhanced if senior)
- **Others**: 80E (education loan), 80TTA (savings interest, max ₹10K), 80G (donations), 80GG (rent if no HRA)
- Computed: Total Deductions (gold, capped per section limits)

#### Losses B/F Tab
- Brought Forward Business Loss
- BF STCG Loss, BF LTCG Loss
- BF Speculative Loss
- Set-off utilised this year
- Computed: Total B/F Loss Utilized (gold)

#### TDS & Advance Tax Tab
2 sub-sections side by side:

**TDS Deducted**:
- 192 (Salary), 194A (Interest), 194B (Lottery), 194IA (Property), 194S (VDA), Others
- Computed: Total TDS (gold)

**Advance Tax & Self Assessment**:
- 4 instalments: 15-Jun, 15-Sep, 15-Dec, 15-Mar
- Self Assessment Tax
- Computed: Total Tax Paid (gold)

#### Tax Computation Tab (READ-ONLY)

Full summary panel — all values computed by `computeTax()`, displayed in a clean two-column table:

```
Income Summary                    ₹ Amount
──────────────────────────────────────────
Salary (Net Taxable)              X,XX,XXX
House Property Income             X,XX,XXX
Capital Gains (Normal Rate)       X,XX,XXX
Business Income                   X,XX,XXX
Other Sources                     X,XX,XXX
VDA Income                        X,XX,XXX
──────────────────────────────────────────
Gross Total Income (GTI)          X,XX,XXX
Less: HP Loss Set-off            (X,XX,XXX)
Less: B/F Loss                   (X,XX,XXX)
GTI After Set-offs                X,XX,XXX
Less: Total Deductions           (X,XX,XXX)  [OLD regime only]
Total Taxable Income              X,XX,XXX
──────────────────────────────────────────
Tax on Normal Income              X,XX,XXX
Less: Rebate u/s 87A             (X,XX,XXX)
Tax After Rebate                  X,XX,XXX
Surcharge                         X,XX,XXX
Health & Education Cess (4%)      X,XX,XXX
──────────────────────────────────────────
VDA Tax @ 30%                     X,XX,XXX
Capital Gains Tax (Special)       X,XX,XXX
──────────────────────────────────────────
TOTAL TAX LIABILITY               X,XX,XXX   ← Bold, navy background
Less: TDS Deducted               (X,XX,XXX)
Less: Advance Tax Paid           (X,XX,XXX)
──────────────────────────────────────────
TAX PAYABLE / (REFUND)            X,XX,XXX   ← Red if payable, Green if refund
```

Below the summary, show:
- Old vs New regime comparison: if user is on OLD, compute NEW tax and vice versa, show "Save ₹XX,XXX by switching to New Regime" or vice versa in a yellow advisory box
- Key notes: 87A rebate applicability, surcharge applicability

---

## 12. Stub Pages (render gracefully with mock data)

Each stub page must:
1. Show the page heading and basic layout matching the overall design
2. Render a clear "🚧 Backend integration coming soon" banner in yellow
3. Show 2–3 mock data rows/cards so the UI looks populated
4. Wire all action buttons to show `toast.info("This feature is being integrated with the backend")` on click

### Pages to stub:

**ReconciliationPage**: Show a table of AIS vs ITR mismatches with mock entries (salary, dividend, capital gains rows). Toolbar with "Run Reconciliation" button. Detail panel on row click.

**SyncPage**: Show ITD Portal Sync UI with last sync timestamp, sync scope checkboxes (AIS, Form 26AS, Prefill JSON), batch size slider, "Start Sync" button. Show sync history table below.

**JobsPage**: Show a table of background jobs with status badges (Running, Completed, Failed), progress bars, timestamps. Action buttons (Stop, Restart, View Logs).

**NoticesPage**: Show notices table with notice type, date, client name, due date, status. Side panel for notice details and notes. Action buttons (Respond, Mark Resolved, Escalate).

**CalendarPage**: Show a full-month calendar grid with tax deadline events highlighted. Standard Indian tax calendar deadlines (ITR filing 31 Jul, Advance Tax Q1 15 Jun etc.) as colored blocks.

**TasksPage**: Show a kanban-style board (To Do | In Progress | Done) with task cards. Each card shows client name, task description, due date, priority badge.

**BillingPage**: Show invoices table with client name, amount, status (Paid/Pending/Overdue), date. Top stats row: Total Billed / Collected / Outstanding. "Generate Invoice" button.

**AccountingPage**: Show income/expense summary for the firm with simple bar chart placeholder.

**ReportsPage**: Show a grid of report tiles (Filing Status Report, AIS Mismatch Report, Tax Computation Summary, Client-wise Report). Each tile has a "Generate" button → stub toast.

**CommunicationPage**: Show broadcast message composer (template selector, recipient group selector, schedule date). Message history table below.

---

## 13. Utility Files

### `src/utils/formatters.ts`
```typescript
import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
dayjs.extend(relativeTime);

export const fmt = (n: number) => Math.round(n || 0).toLocaleString('en-IN');
export const INR = (n: number) => `₹${fmt(n)}`;
export const INRLakh = (n: number) => {
  if (n >= 10000000) return `₹${(n/10000000).toFixed(2)}Cr`;
  if (n >= 100000) return `₹${(n/100000).toFixed(2)}L`;
  return INR(n);
};
export const relTime = (iso: string) => dayjs(iso).fromNow();
export const fmtDate = (iso: string) => dayjs(iso).format('DD MMM YYYY');
export const toAPIDate = (date: Date | string) => dayjs(date).format('YYYY-MM-DD');
export const panInitials = (name: string) =>
  name.split(' ').map(w => w[0]).join('').toUpperCase().slice(0, 2);
export const deriveEntityFromPAN = (pan: string) => {
  const map: Record<string, string> = {
    P: 'Individual', H: 'HUF', F: 'Firm', A: 'AOP', B: 'BOI',
    C: 'Company', G: 'Govt', J: 'AI', L: 'Local', T: 'Trust'
  };
  return map[pan?.[3]?.toUpperCase()] ?? 'Unknown';
};
```

### `src/components/ui/Spinner.tsx`
```tsx
export const Spinner = ({ size = 20 }: { size?: number }) => (
  <div style={{
    width: size, height: size,
    border: `2px solid var(--border)`,
    borderTopColor: `var(--gold)`,
    borderRadius: '50%',
    animation: 'spin 0.8s linear infinite',
    display: 'inline-block',
  }} />
);
```

### `src/components/ui/SkeletonRow.tsx`
```tsx
export const SkeletonRow = ({ cols = 6 }: { cols?: number }) => (
  <tr>
    {Array.from({ length: cols }).map((_, i) => (
      <td key={i}>
        <div style={{
          height: 14, borderRadius: 4,
          background: 'linear-gradient(90deg, #e8edf3 25%, #f4f6f9 50%, #e8edf3 75%)',
          backgroundSize: '400% 100%',
          animation: 'shimmer 1.5s infinite',
          width: `${60 + Math.random() * 30}%`,
        }} />
      </td>
    ))}
  </tr>
);
```

### `src/components/ProtectedRoute.tsx`
```tsx
import { Navigate, Outlet } from 'react-router-dom';
import { tokenManager } from '../lib/api/tokenManager';

export const ProtectedRoute = () =>
  tokenManager.isAuthenticated() ? <Outlet /> : <Navigate to="/login" replace />;
```

---

## 14. Context — AY Selector

### `src/contexts/AYContext.tsx`
```tsx
import { createContext, useContext, useState } from 'react';

interface AYContextType {
  ay: string;
  ayParam: string | null;
  setAY: (ay: string) => void;
}

const AYContext = createContext<AYContextType>({
  ay: 'AY 2025–26', ayParam: '2025-26', setAY: () => {}
});

export const AYProvider = ({ children }: { children: React.ReactNode }) => {
  const [ay, setAYState] = useState('AY 2025–26');
  const ayParamMap: Record<string, string | null> = {
    'AY 2025–26': '2025-26',
    'AY 2026–27': '2026-27',
    'Both AYs': null,
  };
  const setAY = (v: string) => setAYState(v);
  return (
    <AYContext.Provider value={{ ay, ayParam: ayParamMap[ay], setAY }}>
      {children}
    </AYContext.Provider>
  );
};

export const useAY = () => useContext(AYContext);
```

Wrap the entire app in `<AYProvider>` in `main.tsx`.

---

## 15. Loading & Error State Patterns

### Pattern for Data Fetching
```typescript
const [data, setData] = useState<T[]>([]);
const [loading, setLoading] = useState(true);
const [error, setError] = useState<string | null>(null);

useEffect(() => {
  setLoading(true);
  setError(null);
  someApi.list(params)
    .then(setData)
    .catch(err => {
      setError(err.message);
      toast.error(err.message);
    })
    .finally(() => setLoading(false));
}, [params]);

// In render:
{loading && <SkeletonRow />}
{error && <ErrorBanner message={error} />}
{!loading && !error && data.length === 0 && <EmptyState />}
```

### Error Banner Component
```tsx
export const ErrorBanner = ({ message }: { message: string }) => (
  <div style={{
    background: 'var(--danger-bg)', color: 'var(--danger)',
    border: '1px solid #FCA5A5', borderRadius: 'var(--radius-sm)',
    padding: '10px 16px', marginBottom: 16, fontSize: 13,
  }}>
    ⚠️ {message}
  </div>
);
```

---

## 16. Document Management Integration in ITR Page

In the ITR Computation page, add a **"Documents" section** accessible via a tab or collapsible panel in the sidebar:

- **Upload area**: drag-and-drop zone for Form 16, AIS JSON, Form 26AS, ITR JSON, Other
- **Document list**: table showing uploaded documents with name, type badge, upload date, download/delete actions
- Wire to `documentsApi.upload()` and `documentsApi.list(clientId, year)`
- After Form 16 upload → show "Auto-populate" button → call `integrationApi.extractForm16()` then `integrationApi.autoPopulateFromForm16()`

---

## 17. PAN Validation in Add Client Modal

```typescript
const handlePanBlur = async (pan: string) => {
  if (pan.length !== 10) return;
  setPanChecking(true);
  try {
    const result = await panApi.analyze(pan);
    if (!result.valid) {
      setPanError('Invalid PAN — check format and checksum');
      return;
    }
    setPanStatus('valid');
    setEntityType(result.entityType);
    setEligibleForms(result.eligibleITRForms);
    if (result.warnings.length) {
      toast(result.warnings.join(', '), { icon: '⚠️' });
    }
  } catch {
    setPanError('Could not validate PAN — please verify manually');
  } finally {
    setPanChecking(false);
  }
};
```

---

## 18. Global Search in Sidebar

```typescript
const [searchQuery, setSearchQuery] = useState('');
const [searchResults, setSearchResults] = useState<ClientResponse[]>([]);

const handleSearch = useCallback(
  debounce(async (q: string) => {
    if (q.length < 2) { setSearchResults([]); return; }
    const results = await clientsApi.list({ search: q });
    setSearchResults(results);
  }, 300),
  []
);
```

Show a floating dropdown under the search box with matching clients. Clicking a result navigates to that client's latest ITR year: `/filing/{clientId}/{latestYear}`.

---

## 19. Final Implementation Checklist

Complete items in this exact order:

**Phase 1 — Foundation (Day 1)**
- [ ] Vite + React + TypeScript project created
- [ ] All dependencies installed
- [ ] `index.css` with all design tokens
- [ ] `src/lib/api/` — all API modules created (real + stubs)
- [ ] `src/types/api.types.ts` — all interfaces defined
- [ ] `src/utils/formatters.ts`
- [ ] `AYContext` created and wired in `main.tsx`
- [ ] `.env` with `VITE_API_BASE_URL`

**Phase 2 — Shell (Day 1-2)**
- [ ] `AppLayout` with sidebar + topbar + outlet
- [ ] `Sidebar` — all nav items, active states, search input, user footer
- [ ] `Topbar` — breadcrumb, AY selector, bulk sync button
- [ ] `ProtectedRoute` working
- [ ] React Router configured with all routes
- [ ] Login page fully functional (calls auth API, saves token, redirects)
- [ ] Register page functional

**Phase 3 — Dashboard (Day 2)**
- [ ] Stats cards fetch from `/api/dashboard/stats`
- [ ] Filing pipeline pipeline shows derived counts
- [ ] Activity feed (with empty state fallback)
- [ ] ITR type breakdown (with fallback)
- [ ] Deadlines section (static fallback if API not available)

**Phase 4 — Clients (Day 2-3)**
- [ ] Client table with pagination/search (API-driven)
- [ ] Skeleton rows while loading
- [ ] Add Client modal with PAN validation
- [ ] Edit client modal
- [ ] Delete confirmation
- [ ] Status badge color mapping
- [ ] "Open ITR" navigation

**Phase 5 — ITR Computation (Day 3-5)**
- [ ] Load form data from `/api/clients/{id}/itr/{year}` on mount
- [ ] All 11 section tabs with correct fields
- [ ] `computeTax()` engine running in `useMemo`
- [ ] All computed fields read-only gold
- [ ] Save button wired to `PUT` endpoint
- [ ] Auto-save on tab change (2s debounce)
- [ ] Validate button wired, errors displayed
- [ ] AIS import → auto-populate flow
- [ ] Form 16 import → auto-populate flow
- [ ] Download JSON + PDF
- [ ] Regime toggle (OLD/NEW) updates `computeTax` instantly
- [ ] AY toggle updates tax engine (LTCG rates change)

**Phase 6 — Filing Page (Day 5)**
- [ ] Filing table from `/api/filing`
- [ ] Status pipeline header
- [ ] Status update via PUT
- [ ] "Open ITR" navigation

**Phase 7 — Stub Pages (Day 6)**
- [ ] All 10 stub pages render without crashes
- [ ] Mock data displayed clearly
- [ ] "Coming soon" banners
- [ ] Action buttons show toast

**Phase 8 — Polish (Day 6-7)**
- [ ] All monetary values formatted `en-IN`
- [ ] All number cells in DM Mono font
- [ ] Responsive at 1280px minimum width
- [ ] No console errors in production
- [ ] `react-hot-toast` toasts for all success/error states
- [ ] Loading spinners on all async buttons
- [ ] AY context change triggers refetch on Dashboard + Clients + Filing

---

## 20. Key Notes for the Agent

1. **Tax Engine is Sacred** — `computeTax()` runs only in the browser. Never mock it, never replace it with an API call, never simplify it. It is the core feature.

2. **Computed fields** — Any field where the value is derived by `computeTax()` must have `computed={true}` prop on the `<Field>` component, rendering with gold-pale background and cursor:default. These values are NEVER written back to the backend directly — only the input fields are sent.

3. **Date Format** — All dates to backend: `"YYYY-MM-DD"` strings (use `dayjs`). All AY strings: `"2025-26"` format (hyphen, not dash-dash).

4. **Indian Number Format** — All currency display: `n.toLocaleString('en-IN')` → gives `8,40,000` (not `840,000`). Use `INR(n)` helper from formatters.

5. **PAN 4th character** — determines entity type for display badges. Use `deriveEntityFromPAN()` utility. Never assume individual — always derive.

6. **Backend base path** — Axios base URL already includes `/api`. Individual API calls use `/clients`, `/auth`, etc. — never `/api/clients`.

7. **Auth token** — JWT stored in localStorage as `auth_token`. All API calls auto-attach it via Axios request interceptor. 401 response auto-clears token and redirects to `/login`.

8. **Stub APIs** — never crash the app if a stub endpoint returns empty data. All stub pages gracefully handle empty arrays.

9. **AY context** — the AY selector in Topbar changes `ayParam` in context. All data-fetching hooks that use AY must subscribe to this context and refetch when it changes.

10. **Preserve all design tokens** — no inline color strings. Always use CSS variables (`var(--navy)`, `var(--gold)`, etc.). Never introduce Tailwind, MUI, or any other design system — raw CSS only, matching the reference.
