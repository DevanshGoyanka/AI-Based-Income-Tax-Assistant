package com.itr.controller;

import com.itr.service.HRAComputationService;
import com.itr.service.SalaryExemptionService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/salary")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class SalaryCalculationController {

    private final HRAComputationService hraComputationService;
    private final SalaryExemptionService salaryExemptionService;

    @PostMapping("/calculate")
    public ResponseEntity<SalaryCalculationResponse> calculateSalary(@RequestBody SalaryCalculationRequest request) {
        log.info("Calculating salary for {} employers", request.getEmployers().size());
        
        SalaryCalculationResponse response = new SalaryCalculationResponse();
        List<EmployerCalculation> calculations = new ArrayList<>();
        
        for (EmployerInput employer : request.getEmployers()) {
            EmployerCalculation calc = calculateEmployer(employer, request.getAssessmentYear());
            calculations.add(calc);
        }
        
        response.setEmployers(calculations);
        response.setTotalGrossSalary(calculations.stream().mapToDouble(EmployerCalculation::getGrossSalary).sum());
        response.setTotalNetSalary(calculations.stream().mapToDouble(EmployerCalculation::getNetSalary).sum());
        response.setTotalTDS(calculations.stream().mapToDouble(EmployerCalculation::getTdsDeducted).sum());
        
        return ResponseEntity.ok(response);
    }

    private EmployerCalculation calculateEmployer(EmployerInput input, String assessmentYear) {
        EmployerCalculation calc = new EmployerCalculation();
        
        // Section 17(1) - Salary Components
        double basic = input.getBasic();
        double da = input.getDa();
        double hra = input.getHra();
        double bonus = input.getBonus();
        double allowances = input.getAllowances();
        double lta = input.getLta();
        double pension = input.getPension();
        double commutedPension = input.getCommutedPension();
        double uncommutedPension = input.getUncommutedPension();
        double gratuity = input.getGratuity();
        double leaveEncashment = input.getLeaveEncashment();
        double arrearsOfSalary = input.getArrearsOfSalary();
        
        // Section 17(2) - Perquisites (sum of all detailed perquisites)
        double perquisites = input.getPerqRentFreeAccommodation() + input.getPerqConcessionalRent() +
                input.getPerqMotorCar() + input.getPerqSweeper() + input.getPerqGasElectricityWater() +
                input.getPerqInterestFreeLoan() + input.getPerqHolidayExpenses() + input.getPerqFreeEducation() +
                input.getPerqGiftsVouchers() + input.getPerqCreditCard() + input.getPerqClubExpenses() +
                input.getPerqMovableAssets() + input.getPerqOthers();
        
        // Section 17(3) - Profits in Lieu of Salary
        double profitsInLieu = input.getProfitsCompensationTermination() + input.getProfitsNonCompete();
        
        // Calculate HRA Exemption u/s 10(13A)
        double hraExempt = 0;
        if (input.getRentPaid() != null && input.getRentPaid() > 0) {
            HRAComputationService.HRAInput hraInput = new HRAComputationService.HRAInput();
            hraInput.setHraReceived(hra);
            hraInput.setBasicDA(basic + da);
            hraInput.setRentPaid(input.getRentPaid());
            hraInput.setCityType(Boolean.TRUE.equals(input.getIsMetroCity()) ? "METRO" : "NON_METRO");
            
            HRAComputationService.HRAResult hraResult = hraComputationService.computeHRAExemption(hraInput);
            hraExempt = hraResult.getExemption();
        }
        
        // Other exemptions (user can override if needed)
        double ltaExempt = input.getLtaExempt();
        double gratuityExempt = input.getGratuityExempt();
        double leaveEncashmentExempt = input.getLeaveEncashmentExempt();
        double otherExemptions = input.getOtherExemptions();
        
        // Total exemptions u/s 10
        double totalExemptions = hraExempt + ltaExempt + gratuityExempt + leaveEncashmentExempt + otherExemptions;
        
        // Gross Salary = Section 17(1) + 17(2) + 17(3)
        double grossSalary = basic + da + hra + bonus + allowances + lta + pension + 
                commutedPension + uncommutedPension + gratuity + leaveEncashment + 
                arrearsOfSalary + perquisites + profitsInLieu;
        
        // Less: Exemptions u/s 10
        double salaryAfterExemptions = grossSalary - totalExemptions;
        
        // Standard Deduction u/s 16(ia)
        double standardDeduction = "2026-27".equals(assessmentYear) ? 75000 : 50000;
        
        // Professional Tax u/s 16(iii)
        double professionalTax = Math.min(input.getProfessionalTax(), 2500);
        
        // Entertainment Allowance u/s 16(ii) - only for government employees
        double entertainmentAllowance = input.getEntertainmentAllowance();
        
        // Total deductions u/s 16
        double totalDeductions16 = standardDeduction + professionalTax + entertainmentAllowance;
        
        // Net Salary (Income from Salary)
        double netSalary = salaryAfterExemptions - totalDeductions16;
        
        // Set calculation results
        calc.setEmployerName(input.getEmployerName());
        calc.setEmployerTAN(input.getEmployerTAN());
        calc.setBasic(basic);
        calc.setDa(da);
        calc.setHra(hra);
        calc.setBonus(bonus);
        calc.setAllowances(allowances);
        calc.setLta(lta);
        calc.setPension(pension);
        calc.setPerquisites(perquisites);
        calc.setProfitsInLieu(profitsInLieu);
        calc.setHraExempt(hraExempt);
        calc.setLtaExempt(ltaExempt);
        calc.setGratuityExempt(gratuityExempt);
        calc.setLeaveEncashmentExempt(leaveEncashmentExempt);
        calc.setOtherExemptions(otherExemptions);
        calc.setTotalExemptions(totalExemptions);
        calc.setGrossSalary(grossSalary);
        calc.setStandardDeduction(standardDeduction);
        calc.setProfessionalTax(professionalTax);
        calc.setEntertainmentAllowance(entertainmentAllowance);
        calc.setTotalDeductions16(totalDeductions16);
        calc.setNetSalary(netSalary);
        calc.setTdsDeducted(input.getTdsDeducted());
        
        return calc;
    }

    @Data
    public static class SalaryCalculationRequest {
        private String assessmentYear;
        private List<EmployerInput> employers;
    }

    @Data
    public static class EmployerInput {
        private String employerName;
        private String employerTAN;
        private double basic;
        private double da;
        private double hra;
        private double bonus;
        private double allowances;
        private double lta;
        private Double rentPaid;
        private Boolean isMetroCity;
        private double pension;
        private double commutedPension;
        private double uncommutedPension;
        private double gratuity;
        private double leaveEncashment;
        private double arrearsOfSalary;
        private double perqRentFreeAccommodation;
        private double perqConcessionalRent;
        private double perqMotorCar;
        private double perqSweeper;
        private double perqGasElectricityWater;
        private double perqInterestFreeLoan;
        private double perqHolidayExpenses;
        private double perqFreeEducation;
        private double perqGiftsVouchers;
        private double perqCreditCard;
        private double perqClubExpenses;
        private double perqMovableAssets;
        private double perqOthers;
        private double profitsCompensationTermination;
        private double profitsNonCompete;
        private double ltaExempt;
        private double gratuityExempt;
        private double leaveEncashmentExempt;
        private double otherExemptions;
        private double professionalTax;
        private double entertainmentAllowance;
        private double tdsDeducted;
    }

    @Data
    public static class SalaryCalculationResponse {
        private List<EmployerCalculation> employers;
        private double totalGrossSalary;
        private double totalNetSalary;
        private double totalTDS;
    }

    @Data
    public static class EmployerCalculation {
        private String employerName;
        private String employerTAN;
        private double basic;
        private double da;
        private double hra;
        private double bonus;
        private double allowances;
        private double lta;
        private double pension;
        private double perquisites;
        private double profitsInLieu;
        private double hraExempt;
        private double ltaExempt;
        private double gratuityExempt;
        private double leaveEncashmentExempt;
        private double otherExemptions;
        private double totalExemptions;
        private double grossSalary;
        private double standardDeduction;
        private double professionalTax;
        private double entertainmentAllowance;
        private double totalDeductions16;
        private double netSalary;
        private double tdsDeducted;
    }
}
