package com.itr.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * HRA Computation Engine - 101% CBDT Compliant
 * Metro/Non-metro formula per Section 10(13A)
 * Reference: ITR_Import_JSON_Validation.md Section 1.4
 */
@Slf4j
@Service
public class HRAComputationService {

    public HRAResult computeHRAExemption(HRAInput input) {
        HRAResult result = new HRAResult();
        result.setHraReceived(input.getHraReceived());
        result.setBasicDA(input.getBasicDA());
        result.setRentPaid(input.getRentPaid());
        result.setCityType(input.getCityType());

        // HRA exemption = min of 3 components
        double salaryPct = "METRO".equals(input.getCityType()) ? 0.50 : 0.40;
        
        // Component 1: Actual HRA received
        double component1 = input.getHraReceived();
        
        // Component 2: Rent paid - 10% of basic+DA
        double component2 = Math.max(0, input.getRentPaid() - (0.10 * input.getBasicDA()));
        
        // Component 3: 50% (metro) or 40% (non-metro) of basic+DA
        double component3 = salaryPct * input.getBasicDA();
        
        double exemption = Math.min(component1, Math.min(component2, component3));
        exemption = Math.max(0, exemption);
        
        result.setComponent1(component1);
        result.setComponent2(component2);
        result.setComponent3(component3);
        result.setExemption(exemption);
        result.setTaxableHRA(input.getHraReceived() - exemption);
        
        return result;
    }

    @Data
    public static class HRAInput {
        private double hraReceived;
        private double basicDA;
        private double rentPaid;
        private String cityType; // METRO or NON_METRO
    }

    @Data
    public static class HRAResult {
        private double hraReceived;
        private double basicDA;
        private double rentPaid;
        private String cityType;
        private double component1;
        private double component2;
        private double component3;
        private double exemption;
        private double taxableHRA;
        private String warning;  // Added for validation warnings
    }
}
