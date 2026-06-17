package com.itr.config;

import com.itr.domain.common.AssessmentYear;
import com.itr.domain.common.TaxRegime;
import com.itr.domain.salary.EmployerEntry;
import com.itr.domain.salary.SalaryComputationResult;
import com.itr.domain.salary.SalaryScheduleComputer;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Salary Computation Controller
 * All salary calculations done HERE in backend.
 * Frontend only displays results.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/calculations")
@RequiredArgsConstructor
public class SalaryCalculationController {

    @PostMapping("/salary")
    public ResponseEntity<SalaryComputationResponse> calculateSalary(
            @RequestBody SalaryCalculationRequest request) {
        
        String ay = request.getAssessmentYear() != null ? request.getAssessmentYear() : "2026-27";
        log.info("Computing salary for AY: {}", ay);
        
        try {
            // Convert request to EmployerEntry list
            List<EmployerEntry> employers = request.getEmployers().stream()
                .map(SalaryCalculationRequest.EmployerInput::toEmployerEntry)
                .toList();
            
            // Determine regime
            TaxRegime regime = "NEW".equalsIgnoreCase(request.getTaxRegime())
                ? TaxRegime.NEW : TaxRegime.OLD;
            
            long stdDed = regime == TaxRegime.NEW
                ? AssessmentYear.NEW_REGIME_STANDARD_DEDUCTION
                : AssessmentYear.OLD_REGIME_STANDARD_DEDUCTION;
            
            // Compute using domain/salary
            SalaryComputationResult result = SalaryScheduleComputer.compute(
                employers, stdDed, regime, ay
            );
            
            log.info("Salary computed: Gross={}, Exemptions={}, Net={}",
                result.grossSalaryTotal(), result.totalSection10Exempt(), result.netTaxableSalary());
            
            return ResponseEntity.ok(SalaryComputationResponse.fromResult(result));
            
        } catch (Exception e) {
            log.error("Salary calculation failed", e);
            return ResponseEntity.internalServerError()
                .body(SalaryComputationResponse.error(e.getMessage()));
        }
    }

    @Data
    public static class SalaryCalculationRequest {
        private String taxRegime;
        private String assessmentYear;
        private List<EmployerInput> employers;

        @Data
        public static class EmployerInput {
            private String employerName;
            private String tan;
            private Long basic;
            private Long da;
            private Long hraReceived;
            private Long ltaReceived;
            private Long bonus;
            private Long allowances;
            private Long perquisitesValue;
            private Long profitsInLieu;
            private Long annualRentPaid;
            private String city;
            private Boolean isMetroCity;
            private Boolean isGovernmentEmployee;
            private Boolean isDisabledEmployee;
            private Long gratuityReceived;
            private Long leaveEncashmentReceived;
            private Long commutedPensionReceived;
            private Long professionalTax;
            private Long tdsDeducted;
            private Integer numberOfChildren;
            private Long childrenEducationAllowance;
            private Long hostelExpenditureAllowance;
            private Long transportAllowance;
            
            public EmployerEntry toEmployerEntry() {
                // Convert boolean isMetroCity to a proper city name
                String resolvedCity = city;
                if (resolvedCity == null || resolvedCity.isBlank()) {
                    if (Boolean.TRUE.equals(isMetroCity)) {
                        resolvedCity = "MUMBAI";  // 50% for metro
                    } else {
                        resolvedCity = "PUNE";     // 40% for non-metro
                    }
                }
                // Compute proper salary for HRA: basic + da
                long salaryForHRA = nvl(basic) + nvl(da);
                // For gratuity: use 15/26 rule; provide average monthly salary
                long avgMonthlySalary = nvl(basic) / 12;
                int yrs = 5; // default years of service
                // Default numberOfChildren: if CEA or hostel entered, assume 2 children
                int numChildren = (nvl(childrenEducationAllowance) > 0 || nvl(hostelExpenditureAllowance) > 0) ? 2 : 1;
                // Default transport allowance for regular employee: ₹19,200/yr in paise = 1,920,000
                // For disabled employee: ₹38,400/yr in paise
                long resolvedTransport = nvl(transportAllowance);
                boolean disabled = isDisabledEmployee != null && isDisabledEmployee;
                if (resolvedTransport == 0L) {
                    resolvedTransport = disabled ? 38_40000L : 19_20000L;
                }
                
                return new EmployerEntry(
                    employerName, tan,
                    nvl(basic), nvl(da), 0L,
                    nvl(hraReceived), nvl(ltaReceived),
                    resolvedTransport, nvl(childrenEducationAllowance),
                    nvl(hostelExpenditureAllowance), 0L,
                    nvl(allowances), nvl(bonus), 0L,
                    nvl(perquisitesValue),
                    nvl(profitsInLieu),
                    0L, 0L, 0L, 0L,
                    nvl(annualRentPaid), resolvedCity, 0L, false, 0L,
                    nvl(commutedPensionReceived), false,
                    nvl(gratuityReceived), nvl(leaveEncashmentReceived),
                    avgMonthlySalary, 0, 0L, 0L, numChildren, false, 0, yrs,
                    isGovernmentEmployee != null && isGovernmentEmployee,
                    isDisabledEmployee != null && isDisabledEmployee,
                    nvl(professionalTax), 0L, 0L,
                    nvl(tdsDeducted)
                );
            }
            
            private Long nvl(Long v) { return v != null ? v : 0L; }
        }
    }

    @Data
    public static class SalaryComputationResponse {
        private Long grossSalary;
        private Long hraExempt;
        private Long ltaExempt;
        private Long gratuityExempt;
        private Long leaveEncashmentExempt;
        private Long transportExempt;
        private Long childrenEducationExempt;
        private Long hostelExempt;
        private Long totalExemptions;
        private Long standardDeduction;
        private Long professionalTax;
        private Long netTaxableSalary;
        private Long tdsDeducted;
        private List<EmployerResult> employers;
        private String assessmentYear;
        private String taxRegime;
        private String error;

        public static SalaryComputationResponse fromResult(SalaryComputationResult r) {
            SalaryComputationResponse resp = new SalaryComputationResponse();
            resp.setGrossSalary(r.grossSalaryTotal());
            resp.setHraExempt(r.hraExempt());
            resp.setLtaExempt(r.ltaExempt());
            resp.setGratuityExempt(r.gratuityExempt());
            resp.setLeaveEncashmentExempt(r.leaveEncashmentExempt());
            resp.setTransportExempt(r.transportAllowanceExempt());
            resp.setChildrenEducationExempt(r.childrenEducationExempt());
            resp.setHostelExempt(r.hostelExpenditureExempt());
            resp.setTotalExemptions(r.totalSection10Exempt());
            resp.setStandardDeduction(r.standardDeduction());
            resp.setProfessionalTax(r.professionalTaxDed());
            resp.setNetTaxableSalary(r.netTaxableSalary());
            resp.setTdsDeducted(r.totalTDSDeducted());
            resp.setAssessmentYear(r.assessmentYear());
            resp.setTaxRegime(r.regimeUsed() != null ? r.regimeUsed().name() : "OLD");
            resp.setEmployers(List.of(
                new EmployerResult("Employer 1", r.grossSalaryTotal(), 
                    r.totalSection10Exempt(), r.netTaxableSalary())
            ));
            return resp;
        }

        public static SalaryComputationResponse error(String msg) {
            SalaryComputationResponse resp = new SalaryComputationResponse();
            resp.setError(msg);
            resp.setGrossSalary(0L);
            resp.setNetTaxableSalary(0L);
            return resp;
        }
    }

    @Data
    public static class EmployerResult {
        private String name;
        private Long grossSalary;
        private Long exemptions;
        private Long netSalary;
        
        public EmployerResult() {}
        public EmployerResult(String n, Long g, Long e, Long net) {
            this.name = n; this.grossSalary = g; this.exemptions = e; this.netSalary = net;
        }
    }
}
