import axiosInstance from '../lib/api/axiosInstance';

export interface EmployerInput {
  employerName: string;
  employerTAN: string;
  basic: number;
  da: number;
  hra: number;
  bonus: number;
  allowances: number;
  lta: number;
  rentPaid?: number;
  isMetroCity?: boolean;
  pension: number;
  commutedPension: number;
  uncommutedPension: number;
  gratuity: number;
  leaveEncashment: number;
  arrearsOfSalary: number;
  perqRentFreeAccommodation: number;
  perqConcessionalRent: number;
  perqMotorCar: number;
  perqSweeper: number;
  perqGasElectricityWater: number;
  perqInterestFreeLoan: number;
  perqHolidayExpenses: number;
  perqFreeEducation: number;
  perqGiftsVouchers: number;
  perqCreditCard: number;
  perqClubExpenses: number;
  perqMovableAssets: number;
  perqOthers: number;
  profitsCompensationTermination: number;
  profitsNonCompete: number;
  ltaExempt: number;
  gratuityExempt: number;
  leaveEncashmentExempt: number;
  otherExemptions: number;
  professionalTax: number;
  entertainmentAllowance: number;
  tdsDeducted: number;
}

export interface EmployerCalculation {
  employerName: string;
  employerTAN: string;
  basic: number;
  da: number;
  hra: number;
  bonus: number;
  allowances: number;
  lta: number;
  pension: number;
  perquisites: number;
  profitsInLieu: number;
  hraExempt: number;
  ltaExempt: number;
  gratuityExempt: number;
  leaveEncashmentExempt: number;
  otherExemptions: number;
  totalExemptions: number;
  grossSalary: number;
  standardDeduction: number;
  professionalTax: number;
  entertainmentAllowance: number;
  totalDeductions16: number;
  netSalary: number;
  tdsDeducted: number;
}

export interface SalaryCalculationResponse {
  employers: EmployerCalculation[];
  totalGrossSalary: number;
  totalNetSalary: number;
  totalTDS: number;
}

export const calculateSalary = async (
  assessmentYear: string,
  employers: EmployerInput[]
): Promise<SalaryCalculationResponse> => {
  const response = await axiosInstance.post<SalaryCalculationResponse>('/salary/calculate', {
    assessmentYear,
    employers,
  });
  return response.data;
};
