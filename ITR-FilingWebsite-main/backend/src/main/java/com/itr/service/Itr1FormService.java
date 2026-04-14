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
    public Itr1FormData getFormData(Long clientId, String year, Long userId) {
        log.info("Getting ITR-1 form data for client {} year {}", clientId, year);
        
        Client client = clientRepository.findByIdAndUserId(clientId, userId)
                .orElseThrow(() -> new RuntimeException("Client not found"));
        
        ClientYearData yearData = clientYearDataRepository
                .findByClientIdAndAssessmentYear(clientId, year)
                .orElseGet(() -> createDefaultYearData(client, year));
        
        // If saved JSON exists, parse it
        if (yearData.getComputedItr1Json() != null && !yearData.getComputedItr1Json().isEmpty()) {
            try {
                return objectMapper.readValue(yearData.getComputedItr1Json(), Itr1FormData.class);
            } catch (Exception e) {
                log.warn("Failed to parse saved ITR-1 JSON, returning default", e);
            }
        }
        
        // Return default form with client personal info
        return buildDefaultFormData(client, year);
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
