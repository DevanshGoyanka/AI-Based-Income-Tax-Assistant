export interface TaxComputationResult {
  totalIncome: number;
  grossTotalIncome: number;
  totalDeductions: number;
  netTaxableIncome: number;
  taxOnNormalIncome: number;
  taxOnSpecialRateIncome: number;
  rebate87A: number;
  taxAfterRebate: number;
  surcharge: number;
  healthAndEducationCess: number;
  totalTaxLiability: number;
  tdsAmount: number;
  advanceTaxPaid: number;
  selfAssessmentTaxPaid: number;
  totalTaxesPaid: number;
  interest234A: number;
  interest234B: number;
  interest234C: number;
  fee234F: number;
  balanceTaxPayable: number;
  refundAmount: number;
  taxRegime: 'OLD' | 'NEW';
}

export interface Client {
  id: number;
  pan: string;
  panMasked: string;
  name: string;
  entityType: string;
  mobile?: string;
  email?: string;
  assignedUserId?: number;
  status?: string;
}

export interface Notice {
  id: number;
  clientId: number;
  noticeType: string;
  section: string;
  replyDueDate: string;
  status: 'NEW' | 'REVIEWED' | 'DRAFT_READY' | 'REPLIED' | 'RESOLVED';
  riskLevel: 'HIGH' | 'MEDIUM' | 'LOW';
}

export interface FilingRecord {
  clientId: number;
  assessmentYear: string;
  status: string;
  acknowledgementNumber?: string;
  filingDate?: string;
  isRevised: boolean;
}

export type PermissionString =
  | 'client:create' | 'client:edit' | 'client:delete' | 'client:view'
  | 'computation:run' | 'computation:view'
  | 'filing:submit' | 'filing:view' | 'filing:bulk'
  | 'otp:send' | 'otp:collect'
  | 'notice:view' | 'notice:reply' | 'notice:submit'
  | 'tds:manage' | 'tds:form16'
  | 'billing:create' | 'billing:view' | 'billing:collect'
  | 'accounting:manage' | 'accounting:view'
  | 'admin:users' | 'admin:firm' | 'admin:audit'
  | 'report:view' | 'analytics:view'
  | 'document:upload' | 'document:share';
