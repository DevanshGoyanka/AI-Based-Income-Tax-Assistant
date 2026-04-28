package com.itr.service;

import com.itr.dto.Itr1FormData;
import com.itr.dto.Form16Data;
import com.itr.entity.Client;
import com.itr.entity.ClientYearData;
import com.itr.repository.ClientRepository;
import com.itr.repository.ClientYearDataRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;

@Slf4j
@Service
@RequiredArgsConstructor
public class Itr1FormService {
    
    private final ITR1CalculatorService calculatorService;
    private final ClientRepository clientRepository;
    private final ClientYearDataRepository clientYearDataRepository;
    private final ObjectMapper objectMapper;
    
    @Transactional(readOnly = true)
    public com.itr.dto.FlatFormData getFlatFormData(Long clientId, String year, Long userId) {
        log.info("Getting flat form data for client {} year {}", clientId, year);
        
        Client client = clientRepository.findByIdAndUserId(clientId, userId)
                .orElseThrow(() -> new RuntimeException("Client not found"));
        
        ClientYearData yearData = clientYearDataRepository
                .findByClientIdAndAssessmentYear(clientId, year)
                .orElseGet(() -> createDefaultYearData(client, year));
        
        // If saved JSON exists, parse as FlatFormData
        if (yearData.getComputedItr1Json() != null && !yearData.getComputedItr1Json().isEmpty()) {
            try {
                com.itr.dto.FlatFormData flatData = objectMapper.readValue(
                    yearData.getComputedItr1Json(), 
                    com.itr.dto.FlatFormData.class
                );
                log.info("Successfully loaded flat form data for client {}", clientId);
                return flatData;
            } catch (Exception e) {
                log.warn("Failed to parse saved JSON as FlatFormData", e);
            }
        }
        
        // Return empty flat form data
        return com.itr.dto.FlatFormData.builder().build();
    }
    
    @Transactional(readOnly = true)
    public Itr1FormData getFormData(Long clientId, String year, Long userId) {
        log.info("Getting ITR-1 form data for client {} year {}", clientId, year);
        
        Client client = clientRepository.findByIdAndUserId(clientId, userId)
                .orElseThrow(() -> new RuntimeException("Client not found"));
        
        ClientYearData yearData = clientYearDataRepository
                .findByClientIdAndAssessmentYear(clientId, year)
                .orElseGet(() -> createDefaultYearData(client, year));
        
        // If saved JSON exists, try to parse it
        if (yearData.getComputedItr1Json() != null && !yearData.getComputedItr1Json().isEmpty()) {
            try {
                // Try parsing as FlatFormData first (most common case)
                com.itr.dto.FlatFormData flatData = objectMapper.readValue(
                    yearData.getComputedItr1Json(), 
                    com.itr.dto.FlatFormData.class
                );
                log.info("Successfully parsed as FlatFormData, converting to Itr1FormData");
                return convertFlatToItr1(flatData, client, year);
            } catch (Exception e1) {
                log.info("Failed to parse as FlatFormData, trying Itr1FormData");
                try {
                    // Fallback: Try parsing as Itr1FormData
                    Itr1FormData itr1 = objectMapper.readValue(yearData.getComputedItr1Json(), Itr1FormData.class);
                    // Verify personalInfo is not null
                    if (itr1.getPersonalInfo() == null) {
                        log.warn("Parsed Itr1FormData has null personalInfo, returning default");
                        return buildDefaultFormData(client, year);
                    }
                    return itr1;
                } catch (Exception e2) {
                    log.warn("Failed to parse saved JSON as both formats, returning default", e2);
                }
            }
        }
        
        // Return default form with client personal info
        return buildDefaultFormData(client, year);
    }
    
    private Itr1FormData convertFlatToItr1(com.itr.dto.FlatFormData flat, Client client, String year) {
        log.info("Converting FlatFormData to Itr1FormData for client {}", client.getId());
        
        // Parse DOB
        LocalDate dob = null;
        if (flat.getDob() != null) {
            try {
                dob = LocalDate.parse(flat.getDob());
            } catch (Exception e) {
                dob = client.getDob();
            }
        } else {
            dob = client.getDob();
        }
        
        // Build personal info with all required fields
        Itr1FormData.PersonalInfo personalInfo = Itr1FormData.PersonalInfo.builder()
                .assesseeName(flat.getName() != null ? flat.getName() : client.getName())
                .pan(flat.getPan() != null ? flat.getPan() : client.getPan())
                .aadhaar(flat.getAadhaar())
                .dateOfBirth(dob)
                .age(flat.getAge() > 0 ? flat.getAge() : (dob != null ? Period.between(dob, LocalDate.now()).getYears() : 30))
                .fatherName(flat.getFatherName())
                .email(flat.getEmail() != null ? flat.getEmail() : client.getEmail())
                .mobile(flat.getMobile() != null ? flat.getMobile() : client.getMobile())
                .assessmentYear(year)
                .financialYear(convertToFinancialYear(year))
                .residentialStatus(flat.getResidentialStatus() != null ? flat.getResidentialStatus() : "ROR")
                .regime("NEW")
                .filingType("ORIGINAL")
                .employerCategory("OTHERS")
                .panAadhaarLinked(true)
                .build();
        
        // Build salary income with employers list
        java.util.List<Itr1FormData.EmployerDetails> employers = new java.util.ArrayList<>();
        if (flat.getEmployerName() != null || flat.getEmployerTAN() != null) {
            employers.add(Itr1FormData.EmployerDetails.builder()
                    .employerName(flat.getEmployerName() != null ? flat.getEmployerName() : "")
                    .employerTAN(flat.getEmployerTAN() != null ? flat.getEmployerTAN() : "")
                    .employerCategory("OTHERS")
                    .employmentType("REGULAR")
                    .employerCountry("INDIA")
                    .salaryReceived(flat.getBasic())
                    .tdsDeducted(flat.getTdsS192())
                    .pensioner(false)
                    .build());
        }
        
        Itr1FormData.SalaryIncome salaryIncome = Itr1FormData.SalaryIncome.builder()
                .grossSalary(flat.getBasic())
                .salary17_1(flat.getBasic())
                .standardDeduction(75000.0)
                .professionalTax(flat.getProfTax())
                .employerName(flat.getEmployerName())
                .employerTAN(flat.getEmployerTAN())
                .employers(employers)
                .netSalary(flat.getBasic() - 75000.0 - flat.getProfTax())
                .incomeFromSalary(flat.getBasic() - 75000.0 - flat.getProfTax())
                .build();
        
        // Build other sources
        Itr1FormData.OtherSourcesIncome otherSources = Itr1FormData.OtherSourcesIncome.builder()
                .savingsAccountInterest(flat.getInterestSB())
                .fixedDepositInterest(flat.getInterestFD())
                .dividendFromShares(flat.getDividends())
                .totalOtherSourcesIncome(flat.getInterestSB() + flat.getInterestFD() + flat.getDividends())
                .build();
        
        // Build tax payments with TDS entries
        Itr1FormData.TaxPayments taxPayments = Itr1FormData.TaxPayments.builder()
                .tdsOnSalary(new java.util.ArrayList<>())
                .totalTDSOnSalary(flat.getTdsS192())
                .build();
        
        // Convert TDS entries if present
        if (flat.getTdsEntries() != null && !flat.getTdsEntries().isEmpty()) {
            for (com.itr.dto.FlatFormData.TDSEntry tdsEntry : flat.getTdsEntries()) {
                Itr1FormData.TDSOnSalary tds = Itr1FormData.TDSOnSalary.builder()
                        .employerName(tdsEntry.getDeductorName())
                        .employerTAN(tdsEntry.getDeductorTAN())
                        .tdsAmount(tdsEntry.getTdsDeducted())
                        .salaryAmount(tdsEntry.getIncomeAmount())
                        .verified26AS(tdsEntry.isVerified26AS())
                        .build();
                taxPayments.getTdsOnSalary().add(tds);
            }
        }
        
        return Itr1FormData.builder()
                .personalInfo(personalInfo)
                .salaryIncome(salaryIncome)
                .otherSourcesIncome(otherSources)
                .taxPayments(taxPayments)
                .deductions(Itr1FormData.Deductions.builder().build())
                .build();
    }
    
    private ClientYearData createDefaultYearData(Client client, String year) {
        return ClientYearData.builder()
                .client(client)
                .assessmentYear(year)
                .status("draft")
                .itrType("ITR1")
                .build();
    }
    
    private Itr1FormData buildDefaultFormData(Client client, String year) {
        Integer age = client.getDob() != null ? 
                Period.between(client.getDob(), LocalDate.now()).getYears() : null;
        
        String ageCategory = "BELOW_60";
        if (age != null) {
            if (age >= 80) ageCategory = "SUPER_SENIOR_80_PLUS";
            else if (age >= 60) ageCategory = "SENIOR_60_TO_80";
        }
        
        Itr1FormData.PersonalInfo personalInfo = Itr1FormData.PersonalInfo.builder()
                .assesseeName(client.getName())
                .pan(client.getPan())
                .aadhaar(client.getAadhaar())
                .dateOfBirth(client.getDob())
                .age(age)
                .ageCategory(ageCategory)
                .email(client.getEmail())
                .mobile(client.getMobile())
                .assessmentYear(year)
                .financialYear(convertToFinancialYear(year))
                .filingType("ORIGINAL")
                .residentialStatus("ROR")
                .regime("NEW")
                .employerCategory("OTHERS")
                .panAadhaarLinked(true)
                .build();
        
        return Itr1FormData.builder()
                .personalInfo(personalInfo)
                .salaryIncome(Itr1FormData.SalaryIncome.builder().build())
                .housePropertyIncome(Itr1FormData.HousePropertyIncome.builder().build())
                .otherSourcesIncome(Itr1FormData.OtherSourcesIncome.builder().build())
                .deductions(Itr1FormData.Deductions.builder().build())
                .taxPayments(Itr1FormData.TaxPayments.builder().build())
                .build();
    }
    
    private String convertToFinancialYear(String assessmentYear) {
        // 2025-26 -> 2024-25
        String[] parts = assessmentYear.split("-");
        int startYear = Integer.parseInt(parts[0]) - 1;
        int endYear = Integer.parseInt(parts[1]) - 1;
        return startYear + "-" + String.format("%02d", endYear);
    }
    
    @Transactional
    public Itr1FormData saveFormData(Long clientId, String year, Itr1FormData formData, Long userId) {
        log.info("Saving ITR-1 form data for client {} year {}", clientId, year);
        
        Client client = clientRepository.findByIdAndUserId(clientId, userId)
                .orElseThrow(() -> new RuntimeException("Client not found"));
        
        ClientYearData yearData = clientYearDataRepository
                .findByClientIdAndAssessmentYear(clientId, year)
                .orElseGet(() -> createDefaultYearData(client, year));
        
        try {
            String json = objectMapper.writeValueAsString(formData);
            yearData.setComputedItr1Json(json);
            yearData.setItrType("ITR1");
            clientYearDataRepository.save(yearData);
            log.info("Successfully saved ITR-1 form data for client {}", clientId);
        } catch (Exception e) {
            log.error("Failed to save ITR-1 form data", e);
            throw new RuntimeException("Failed to save form data", e);
        }
        
        return formData;
    }
    
    @Transactional
    public com.itr.dto.FlatFormData saveFormData(Long clientId, String year, com.itr.dto.FlatFormData formData, Long userId) {
        log.info("Saving flat form data for client {} year {}", clientId, year);
        
        Client client = clientRepository.findByIdAndUserId(clientId, userId)
                .orElseThrow(() -> new RuntimeException("Client not found"));
        
        ClientYearData yearData = clientYearDataRepository
                .findByClientIdAndAssessmentYear(clientId, year)
                .orElseGet(() -> createDefaultYearData(client, year));
        
        try {
            String json = objectMapper.writeValueAsString(formData);
            yearData.setComputedItr1Json(json);
            yearData.setItrType("ITR1");
            clientYearDataRepository.save(yearData);
            log.info("Successfully saved flat form data for client {}", clientId);
        } catch (Exception e) {
            log.error("Failed to save flat form data", e);
            throw new RuntimeException("Failed to save form data", e);
        }
        
        return formData;
    }
    
    public Itr1FormData computeForm(Long clientId, String year, Itr1FormData formData, Long userId) {
        log.info("Computing ITR-1 for client {} year {}", clientId, year);
        return calculatorService.calculateITR1(formData);
    }
    
    public String generateItr1Json(Long clientId, String year, Long userId) {
        log.warn("Itr1FormService.generateItr1Json - stub implementation");
        return "{\"message\":\"ITD JSON export not yet implemented\"}";
    }
    
    public void applyForm16Data(Long clientId, String year, Form16Data form16Data, Long userId) {
        log.warn("Itr1FormService.applyForm16Data - stub implementation");
    }
}
