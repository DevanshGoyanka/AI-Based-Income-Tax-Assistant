package com.itr.config;

import com.itr.domain.common.AssessmentYear;
import com.itr.domain.common.TaxRegime;
import com.itr.domain.salary.SalaryComputationResult;
import com.itr.domain.salary.SalaryScheduleComputer;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Salary Computation Controller
 * Accepts employerEntries[] from frontend formData and returns
 * a complete Schedule S (ITD-tagged) breakdown computed by SalaryScheduleComputer.
 *
 * Flow: Frontend → POST /api/v1/calculations/salary
 *       → converts Map entries to EmployerEntry records
 *       → SalaryScheduleComputer.compute()
 *       → SalaryComputationResult (all Schedule S fields)
 *
 * This endpoint is called directly by the frontend salary tab for
 * real-time backend-computed results, and the result is also
 * inlined into the full /tax-summary/compute response.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/calculations")
@RequiredArgsConstructor
public class SalaryCalculationController {

    /**
     * POST /api/v1/calculations/salary
     *
     * Request body:
     * {
     *   employers: [               // matches formData.employerEntries[]
     *     { basic, da, hra, rentPaid, isMetroCity, employerName, employerTAN, ... },
     *     ...
     *   ],
     *   taxRegime: "OLD" | "NEW",
     *   assessmentYear: "2026-27"
     * }
     */
    @PostMapping("/salary")
    public ResponseEntity<Map<String, Object>> calculateSalary(
            @RequestBody SalaryCalculationRequest request) {

        String ay = request.getAssessmentYear() != null ? request.getAssessmentYear() : "2026-27";
        TaxRegime regime = "NEW".equalsIgnoreCase(request.getTaxRegime())
                ? TaxRegime.NEW : TaxRegime.OLD;

        log.info("Computing salary for AY: {}, regime: {}, employers: {}",
                ay, regime, request.getEmployers() == null ? 0 : request.getEmployers().size());

        try {
            List<com.itr.domain.salary.EmployerEntry> employers = convertToEmployerEntries(request.getEmployers(), regime, ay);

            long stdDed = regime == TaxRegime.NEW
                    ? AssessmentYear.NEW_REGIME_STANDARD_DEDUCTION
                    : AssessmentYear.OLD_REGIME_STANDARD_DEDUCTION;

            SalaryComputationResult result = SalaryScheduleComputer.compute(
                    employers, stdDed, regime, ay);

            log.info("Salary computed: Gross={}, Exemptions={}, Net={}",
                    result.grossSalaryTotal(), result.totalSection10Exempt(), result.netTaxableSalary());

            return ResponseEntity.ok(toResponseMap(result));

        } catch (Exception e) {
            log.error("Salary calculation failed", e);
            Map<String, Object> err = new HashMap<>();
            err.put("status", "error");
            err.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(err);
        }
    }

    // ── Convert frontend Map entries → backend EmployerEntry records ──────────

    private List<com.itr.domain.salary.EmployerEntry> convertToEmployerEntries(
            List<EmployerInput> inputs,
            TaxRegime regime,
            String ay) {

        if (inputs == null || inputs.isEmpty()) {
            return Collections.emptyList();
        }

        List<com.itr.domain.salary.EmployerEntry> entries = new ArrayList<>();
        for (EmployerInput emp : inputs) {
            entries.add(emp.toEmployerEntry(regime, ay));
        }
        return entries;
    }

    // ── SalaryComputationResult → Map<String, Object> for JSON response ──────

    private Map<String, Object> toResponseMap(SalaryComputationResult r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", "success");
        m.put("assessmentYear", r.assessmentYear());
        m.put("regimeUsed", r.regimeUsed().name());
        m.put("employerCount", r.employerCount());

        // Section 17 Gross
        m.put("grossSalarySection17_1", r.grossSalarySection17_1());
        m.put("grossSalarySection17_2", r.grossSalarySection17_2());
        m.put("grossSalarySection17_3", r.grossSalarySection17_3());
        m.put("grossSalaryTotal", r.grossSalaryTotal());

        // Section 10 Exemptions
        m.put("hraExempt", r.hraExempt());
        m.put("ltaExempt", r.ltaExempt());
        m.put("gratuityExempt", r.gratuityExempt());
        m.put("leaveEncashmentExempt", r.leaveEncashmentExempt());
        m.put("pensionCommutationExempt", r.pensionCommutationExempt());
        m.put("transportAllowanceExempt", r.transportAllowanceExempt());
        m.put("childrenEducationExempt", r.childrenEducationExempt());
        m.put("hostelExpenditureExempt", r.hostelExpenditureExempt());
        m.put("uniformAllowanceExempt", r.uniformAllowanceExempt());
        m.put("totalSection10Exempt", r.totalSection10Exempt());

        // Section 16 Deductions
        m.put("standardDeduction", r.standardDeduction());
        m.put("entertainmentAllowanceDed", r.entertainmentAllowanceDed());
        m.put("professionalTaxDed", r.professionalTaxDed());
        m.put("totalSection16Deductions", r.totalSection16Deductions());

        // Net
        m.put("netTaxableSalary", r.netTaxableSalary());
        m.put("totalTDSDeducted", r.totalTDSDeducted());

        // HRA Debug
        m.put("hraCondition1_Actual", r.hraCondition1_Actual());
        m.put("hraCondition2_RentMinus10Pct", r.hraCondition2_RentMinus10Pct());
        m.put("hraCondition3_MetroPct", r.hraCondition3_MetroPct());
        m.put("hraIsMetroCity", r.hraIsMetroCity());
        m.put("hraCityClassified", r.hraCityClassified());

        return m;
    }

    // ── Request DTOs ─────────────────────────────────────────────────────────

    @Data
    public static class EmployerInput {
        // Core employer info
        private String employerName;
        private String tan;

        // Section 17(1) — Gross Salary
        private Long basic;
        private Long da;
        private Long commission;
        private Long hra;
        private Long lta;
        private Long transportAllowance;
        private Long childrenEducationAllowance;
        private Long hostelExpenditureAllowance;
        private Long uniformAllowance;
        private Long otherAllowances;
        private Long bonus;
        private Long arrearSalary;

        // Section 17(2) — Perquisites (single aggregate field from frontend)
        private Long perquisites;

        // Section 17(3) — Profits in Lieu (single aggregate field from frontend)
        private Long profitsInLieu;

        // HRA inputs
        private Long rentPaid;
        private String city;
        private Boolean isMetroCity;
        private Boolean isGovernmentEmployee;
        private Boolean isDisabledEmployee;

        // Retirement benefits
        private Long commutedPension;
        private Long gratuity;
        private Long leaveEncashment;
        private Long averageMonthlySalary;
        private Integer yearsOfService;
        private Integer unavailedLeaveDays;

        // LTA inputs
        private Long actualLtaFare;
        private Boolean isDomesticTravel;
        private Integer journeysInBlock;
        private Long ltaExempt;  // user-claimed exempt portion

        // Children count (for CEA/hostel)
        private Integer numberOfChildren;

        // Gratuity also received (for pension commutation)
        private Boolean gratuityAlsoReceived;

        // Deductions
        private Long professionalTax;
        private Long entertainmentAllowance;

        // TDS
        private Long tdsDeducted;

        // NPS
        private Long employerNPSContribution;

        // ── Convert to backend EmployerEntry record ─────────────────────────

        public com.itr.domain.salary.EmployerEntry toEmployerEntry(TaxRegime regime, String ay) {
            long basicVal = basic != null ? basic : 0L;
            long daVal = da != null ? da : 0L;
            long commissionVal = commission != null ? commission : 0L;

            // Section 17(1)
            long hraReceived = hra != null ? hra : 0L;
            long ltaReceived = lta != null ? lta : 0L;
            long transportRec = transportAllowance != null ? transportAllowance : 0L;
            long ceaRec = childrenEducationAllowance != null ? childrenEducationAllowance : 0L;
            long hostelRec = hostelExpenditureAllowance != null ? hostelExpenditureAllowance : 0L;
            long uniformRec = uniformAllowance != null ? uniformAllowance : 0L;
            long otherAllow = otherAllowances != null ? otherAllowances : 0L;
            long bonusVal = bonus != null ? bonus : 0L;
            long arrearVal = arrearSalary != null ? arrearSalary : 0L;

            // 17(2) perquisites — single aggregate from frontend
            long perqVal = perquisites != null ? perquisites : 0L;

            // 17(3) profits in lieu — single aggregate from frontend
            long pilVal = profitsInLieu != null ? profitsInLieu : 0L;

            // HRA inputs
            long rentPaidVal = rentPaid != null ? rentPaid : 0L;
            String cityVal = city != null ? city : "";
            boolean isMetroVal = Boolean.TRUE.equals(isMetroCity);
            // Population and employer-owned are not captured in frontend — use defaults
            long cityPop = 0L;
            boolean empOwned = false;
            long actualRentByEmp = 0L;

            // Retirement
            long commutedPen = commutedPension != null ? commutedPension : 0L;
            boolean gratuityAlso = Boolean.TRUE.equals(gratuityAlsoReceived);
            long gratuityVal = gratuity != null ? gratuity : 0L;
            long leaveEncVal = leaveEncashment != null ? leaveEncashment : 0L;
            long avgMonthly = averageMonthlySalary != null ? averageMonthlySalary : 0L;
            int yearsSvc = yearsOfService != null ? yearsOfService : 0;
            int unavailedLeave = unavailedLeaveDays != null ? unavailedLeaveDays : 0;

            // LTA
            long actualFare = actualLtaFare != null ? actualLtaFare : 0L;
            boolean domestic = isDomesticTravel == null || Boolean.TRUE.equals(isDomesticTravel);
            int journeys = journeysInBlock != null ? journeysInBlock : 0;

            // Children
            int childCount = numberOfChildren != null ? numberOfChildren : 0;

            // Employer flags
            boolean isGovt = Boolean.TRUE.equals(isGovernmentEmployee);
            boolean isDisabled = Boolean.TRUE.equals(isDisabledEmployee);

            // Deductions
            long profTax = professionalTax != null ? professionalTax : 0L;
            long entAllow = entertainmentAllowance != null ? entertainmentAllowance : 0L;
            long empNPS = employerNPSContribution != null ? employerNPSContribution : 0L;

            // TDS
            long tds = tdsDeducted != null ? tdsDeducted : 0L;

            // Uncommuted pension — default to 0 (not captured in frontend)
            long uncommutedPen = 0L;

            return new com.itr.domain.salary.EmployerEntry(
                    employerName, tan,
                    // Section 17(1)
                    basicVal, daVal, commissionVal,
                    hraReceived, ltaReceived,
                    transportRec, ceaRec, hostelRec, uniformRec,
                    otherAllow, bonusVal, arrearVal,
                    // Section 17(2)
                    perqVal,
                    // Section 17(3)
                    pilVal,
                    // Additional perquisites (all 0 — frontend uses single aggregate)
                    0L, 0L, 0L, 0L,
                    // HRA inputs
                    rentPaidVal, cityVal, cityPop, empOwned, actualRentByEmp,
                    // Taxable benefits
                    commutedPen, gratuityAlso, gratuityVal, leaveEncVal,
                    avgMonthly, unavailedLeave, uncommutedPen,
                    actualFare, childCount, domestic, journeys, yearsSvc,
                    // Employer details
                    isGovt, isDisabled,
                    // Deductions
                    profTax, entAllow, empNPS,
                    // TDS
                    tds
            );
        }
    }

    @Data
    public static class SalaryCalculationRequest {
        private List<EmployerInput> employers;
        private String taxRegime;
        private String assessmentYear;
    }
}

