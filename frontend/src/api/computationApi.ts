import httpClient from './httpClient';

export interface ComputationInput {
  regime: 'OLD' | 'NEW';
  ageCategory: 'BELOW_60' | 'SENIOR' | 'SUPER_SENIOR';
  employers: Array<{
    employerName: string;
    grossSalary: number;
    basicDA: number;
    hraReceived: number;
    professionalTax: number;
    tdsDeducted: number;
  }>;
  businessIncome?: number;
  capitalGains?: number;
  otherSourcesIncome?: number;
  tdsAmount: number;
  advanceTaxPaid: number;
  selfAssessmentTax: number;
}

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
  totalTaxesPaid: number;
  interest234B: number;
  fee234F: number;
  balanceTaxPayable: number;
  refundAmount: number;
  taxRegime: 'OLD' | 'NEW';
}

export interface RegimeComparisonResult {
  oldRegimeResult: TaxComputationResult;
  newRegimeResult: TaxComputationResult;
  recommendedRegime: 'OLD' | 'NEW';
  savingsInPaise: number;
}

export const computationApi = {
  calculate: (input: ComputationInput) =>
    httpClient.post<TaxComputationResult>('/computation/calculate', input).then(r => r.data),

  regimeCompare: (input: ComputationInput) =>
    httpClient.post<RegimeComparisonResult>('/computation/regime-compare', input).then(r => r.data),

  quickEstimate: (input: ComputationInput) =>
    httpClient.post<TaxComputationResult>('/computation/quick-estimate', input).then(r => r.data),

  getSaved: (clientId: number, ay: string) =>
    httpClient.get<TaxComputationResult>(`/computation/${clientId}/${ay}`).then(r => r.data),
};
