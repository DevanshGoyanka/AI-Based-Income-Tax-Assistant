package com.itr.service;

import com.itr.dto.Itr1FormData;
import com.itr.dto.FlatFormData;
import com.itr.dto.Form16Data;
import com.itr.dto.HouseProperty;
import com.itr.entity.Client;
import com.itr.entity.ClientYearData;
import com.itr.entity.LossLedger;
import com.itr.repository.ClientRepository;
import com.itr.repository.ClientYearDataRepository;
import com.itr.repository.LossLedgerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class Itr1FormService {
    
    private final ITR1CalculatorService calculatorService;
    private final ClientRepository clientRepository;
    private final ClientYearDataRepository clientYearDataRepository;
    private final LossLedgerRepository lossLedgerRepository;
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
                
                // Auto-populate bfLossHP from LossLedger for the current AY
                String pan = client.getPan();
                List<LossLedger> activeHPLosses = lossLedgerRepository.findActiveByType(pan, "HP");
                if (!activeHPLosses.isEmpty()) {
                    double totalRemainingLoss = activeHPLosses.stream()
                            .mapToDouble(LossLedger::getRemainingAmount)
                            .sum();
                    flatData.setBfLossHP(totalRemainingLoss);
                    log.info("Auto-populated bfLossHP: {} from {} LossLedger entries", totalRemainingLoss, activeHPLosses.size());
                }
                
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
                Itr1FormData formData = convertFlatToItr1(flatData, client, year);
                
                // Auto-populate bfLossHP from LossLedger for the current AY
                String pan = client.getPan();
                List<LossLedger> activeHPLosses = lossLedgerRepository.findActiveByType(pan, "HP");
                if (!activeHPLosses.isEmpty()) {
                    double totalRemainingLoss = activeHPLosses.stream()
                            .mapToDouble(LossLedger::getRemainingAmount)
                            .sum();
                    formData.setBfLossHP(totalRemainingLoss);
                    log.info("Auto-populated bfLossHP: {} from {} LossLedger entries", totalRemainingLoss, activeHPLosses.size());
                }
                
                // ALWAYS compute fresh on every load to ensure calculation is up-to-date
                log.info("Computing ITR-1 for fresh calculation on form load");
                return calculatorService.calculateITR1(formData);
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
                    
                    // Auto-populate bfLossHP from LossLedger for the current AY
                    String pan = client.getPan();
                    List<LossLedger> activeHPLosses = lossLedgerRepository.findActiveByType(pan, "HP");
                    if (!activeHPLosses.isEmpty()) {
                        double totalRemainingLoss = activeHPLosses.stream()
                                .mapToDouble(LossLedger::getRemainingAmount)
                                .sum();
                        itr1.setBfLossHP(totalRemainingLoss);
                        log.info("Auto-populated bfLossHP: {} from {} LossLedger entries", totalRemainingLoss, activeHPLosses.size());
                    }
                    
                    // ALWAYS compute fresh on every load to ensure calculation is up-to-date
                    log.info("Computing ITR-1 for fresh calculation on form load (from Itr1FormData)");
                    return calculatorService.calculateITR1(itr1);
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
                .flatDoorNo(flat.getFlatDoorNo())
                .premisesName(flat.getPremisesName())
                .roadStreet(flat.getRoadStreet())
                .area(flat.getArea())
                .townCity(flat.getTownCity())
                .state(flat.getState())
                .pinCode(flat.getPinCode())
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
        
        // Calculate gross salary properly including all components
        double basic = flat.getBasic();
        double da = flat.getDa();
        double hra = flat.getHra();
        double bonus = flat.getBonus();
        double allowances = flat.getAllowances();
        double perquisites = flat.getPerquisites();
        
        // FIX: If salary components are empty but TDS entries exist, derive salary from TDS
        if (basic == 0 && flat.getTdsEntries() != null && !flat.getTdsEntries().isEmpty()) {
            for (FlatFormData.TDSEntry tdsEntry : flat.getTdsEntries()) {
                // Only use section 192 (salary) TDS entries
                if ("192".equals(tdsEntry.getSection()) || tdsEntry.getSection() == null) {
                    if (tdsEntry.getIncomeAmount() > 0) {
                        basic = tdsEntry.getIncomeAmount();
                        log.info("Derived salary from TDS entry: incomeAmount={}", basic);
                        break;
                    }
                }
            }
        }
        
        double grossTotal = basic + da + hra + bonus + allowances + perquisites;
        double netTaxable = Math.max(0, grossTotal - 75000.0 - flat.getProfTax());
        
        log.info("DEBUG FlatForm conversion - basic={}, allowances={}, hra={}, da={}, grossTotal={}, netTaxable={}", 
                basic, allowances, hra, da, grossTotal, netTaxable);
        
        Itr1FormData.SalaryIncome salaryIncome = Itr1FormData.SalaryIncome.builder()
                .basicSalary(basic)
                .grossSalary(grossTotal)
                .salary17_1(grossTotal)  // FIX: salary 17(1) should include all components
                .daAmount(da)
                .hraReceived(hra)
                .bonusAmount(bonus)
                .otherAllowance(allowances)
                .perquisites17_2(perquisites)
                .standardDeduction(75000.0)
                .professionalTax(flat.getProfTax())
                .employerName(flat.getEmployerName())
                .employerTAN(flat.getEmployerTAN())
                .employers(employers)
                .netSalary(grossTotal - 75000.0 - flat.getProfTax())
                .incomeFromSalary(netTaxable)
                .build();
        
        // Build other sources
        Itr1FormData.OtherSourcesIncome otherSources = Itr1FormData.OtherSourcesIncome.builder()
                .savingsAccountInterest(flat.getInterestSB())
                .fixedDepositInterest(flat.getInterestFD())
                .dividendFromShares(flat.getDividends())
                .totalOtherSourcesIncome(flat.getInterestSB() + flat.getInterestFD() + flat.getDividends())
                .build();
        
        // Build tax payments - use flat form fields directly (they're primitive doubles, not nullable)
        double advanceTax = flat.getAdv15Jun() + flat.getAdv15Sep() + flat.getAdv15Dec() + flat.getAdv15Mar();
        double selfTaxAmt = flat.getSelfTax();
        
        Itr1FormData.TaxPayments taxPayments = Itr1FormData.TaxPayments.builder()
                .tdsOnSalary(new java.util.ArrayList<>())
                .totalTDSOnSalary(flat.getTdsS192())
                .totalAdvanceTax(advanceTax)
                .totalSelfAssessmentTax(selfTaxAmt)
                .advanceTaxEntries(new java.util.ArrayList<>())
                .selfAssessmentTaxEntries(new java.util.ArrayList<>())
                .build();
        
        // Add Self Assessment Tax entries if amount > 0
        if (selfTaxAmt > 0) {
            taxPayments.getSelfAssessmentTaxEntries().add(
                Itr1FormData.SelfAssessmentTaxEntry.builder()
                    .bsrCode("")
                    .challanNo("SELF-" + System.currentTimeMillis())
                    .depositDate(java.time.LocalDate.now())
                    .amount(selfTaxAmt)
                    .build()
            );
            log.info("Added SelfAssessmentTax entry: amount={}", selfTaxAmt);
        }
        
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
        
        // ===== CONVERT HOUSE PROPERTIES (AY 2026-27: Up to 2 properties in ITR-1) =====
        java.util.List<HouseProperty> housePropertiesList = new java.util.ArrayList<>();
        double totalHPIncome = 0;
        
        // Convert from FlatFormData houseProperties (now uses unified HouseProperty DTO)
        if (flat.getHouseProperties() != null && !flat.getHouseProperties().isEmpty()) {
            for (HouseProperty hpEntry : flat.getHouseProperties()) {
                HouseProperty hp = HouseProperty.builder()
                        .propertySequenceNo(hpEntry.getPropertySequenceNo())
                        .propertyType(hpEntry.getPropertyType())
                        .address(hpEntry.getAddress())
                        .city(hpEntry.getCity())
                        .state(hpEntry.getState())
                        .pinCode(hpEntry.getPinCode())
                        .countryCode(hpEntry.getCountryCode() != null ? hpEntry.getCountryCode() : "91")
                        .propertyIdentificationNo(hpEntry.getPropertyIdentificationNo())
                        .propertyOwnerType(hpEntry.getPropertyOwnerType())
                        .isCoOwned(hpEntry.isCoOwned())
                        .assesseeShareProperty(hpEntry.getAssesseeShareProperty())
                        .annualRent(hpEntry.getAnnualRent())
                        .municipalRateableValue(hpEntry.getMunicipalRateableValue())
                        .fairRentValue(hpEntry.getFairRentValue())
                        .standardRent(hpEntry.getStandardRent())
                        .vacancyPeriodMonths(hpEntry.getVacancyPeriodMonths())
                        .unrealizedRent(hpEntry.getUnrealizedRent())
                        .arrearsOfRent(hpEntry.getArrearsOfRent())
                        .municipalTaxesPaid(hpEntry.getMunicipalTaxesPaid())
                        .interestOnLoan(hpEntry.getInterestOnLoan())
                        .grossAnnualValue(hpEntry.getGrossAnnualValue())
                        .netAnnualValue(hpEntry.getNetAnnualValue())
                        .standardDeduction(hpEntry.getStandardDeduction())
                        .incomeFromHP(hpEntry.getIncomeFromHP())
                        .preConstructionInterest(hpEntry.getPreConstructionInterest())
                        .coOwners(hpEntry.getCoOwners() != null ? hpEntry.getCoOwners() : new java.util.ArrayList<>())
                        .homeLoans(hpEntry.getHomeLoans() != null ? hpEntry.getHomeLoans() : new java.util.ArrayList<>())
                        .tenants(hpEntry.getTenants() != null ? hpEntry.getTenants() : new java.util.ArrayList<>())
                        .build();
                
                // Calculate income if not already computed
                if (hp.getIncomeFromHP() == 0) {
                    hp.calculateIncome();
                }
                
                housePropertiesList.add(hp);
                totalHPIncome += hp.getIncomeFromHP();
                
                log.info("Converted HouseProperty {}: type={}, income={}", 
                        hp.getPropertySequenceNo(), hp.getPropertyType(), hp.getIncomeFromHP());
            }
        } else if (flat.getHpType() != null) {
            // Legacy single property - convert to new format
            HouseProperty hp = HouseProperty.builder()
                    .propertySequenceNo(1)
                    .propertyType(flat.getHpType())
                    .address(flat.getHpAddress())
                    .city(flat.getHpCity())
                    .state(flat.getHpState())
                    .pinCode(flat.getHpPinCode())
                    .countryCode("91")
                    .annualRent(flat.getGrossRent())
                    .municipalTaxesPaid(flat.getMunTax())
                    .interestOnLoan(flat.getHomeLoanInt())
                    .build();
            
            // Calculate based on property type
            hp.calculateIncome();
            
            housePropertiesList.add(hp);
            totalHPIncome = hp.getIncomeFromHP();
            
            log.info("Converted legacy HouseProperty: type={}, income={}", hp.getPropertyType(), hp.getIncomeFromHP());
        }
        
        log.info("Total House Properties: {}, Total HP Income: {}", housePropertiesList.size(), totalHPIncome);
        
        return Itr1FormData.builder()
                .personalInfo(personalInfo)
                .salaryIncome(salaryIncome)
                .houseProperties(housePropertiesList)
                .housePropertyIncome(Itr1FormData.HousePropertyIncome.builder().build())
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
        
        // Calculate HP income and handle loss carry-forward
        double hpIncome = calculateHousePropertyIncome(formData);
        
        // If HP income is negative, carry forward the loss to LossLedger
        if (hpIncome < 0) {
            int lossAmount = (int) Math.abs(hpIncome);
            String pan = client.getPan();
            
            // Check for existing HP loss from previous year that can be set off first
            List<LossLedger> existingHPLosses = lossLedgerRepository.findActiveByType(pan, "HP");
            int usableExistingLoss = 0;
            int remainingToSetOff = (int) Math.abs(hpIncome);
            
            // First use existing loss to set off current HP income (which is negative)
            for (LossLedger existingLoss : existingHPLosses) {
                if (remainingToSetOff <= 0) break;
                int amountToUtilize = (int) Math.min(existingLoss.getRemainingAmount(), remainingToSetOff);
                existingLoss.utilize(amountToUtilize);
                lossLedgerRepository.save(existingLoss);
                usableExistingLoss += amountToUtilize;
                remainingToSetOff -= amountToUtilize;
                log.info("Set off {} from existing HP loss, remaining set-off needed: {}", amountToUtilize, remainingToSetOff);
            }
            
            // Calculate net loss to carry forward after set-off
            int netLossToCarryForward = Math.max(0, remainingToSetOff);
            
            // Only create new LossLedger entry if there's still a net loss after set-off
            if (netLossToCarryForward > 0) {
                // Calculate expiry AY (8 years for HP loss per IT Act)
                String[] yearParts = year.split("-");
                int startYear = Integer.parseInt(yearParts[0]);
                int expiryYear = startYear + 8;
                String expiryAY = expiryYear + "-" + String.format("%02d", expiryYear % 100 + 1);
                
                LossLedger newLoss = LossLedger.of(pan, year, "HP", netLossToCarryForward, expiryAY);
                lossLedgerRepository.save(newLoss);
                log.info("Created new HP loss carry-forward entry: {} for AY {}", netLossToCarryForward, year);
            }
            
            log.info("HP income after set-off: {}, net loss carried forward: {}", 
                    (hpIncome + usableExistingLoss), netLossToCarryForward);
        } else if (hpIncome > 0 && formData.getBfLossHP() > 0) {
            // If there's current positive HP income and bfLossHP, apply set-off
            double lossToSetOff = Math.min(hpIncome, formData.getBfLossHP());
            String pan = client.getPan();
            List<LossLedger> existingHPLosses = lossLedgerRepository.findActiveByType(pan, "HP");
            
            int remainingToSetOff = (int) lossToSetOff;
            for (LossLedger existingLoss : existingHPLosses) {
                if (remainingToSetOff <= 0) break;
                int amountToUtilize = (int) Math.min(existingLoss.getRemainingAmount(), remainingToSetOff);
                existingLoss.utilize(amountToUtilize);
                lossLedgerRepository.save(existingLoss);
                remainingToSetOff -= amountToUtilize;
                log.info("Set off {} from bfLossHP against current HP income", amountToUtilize);
            }
            
            // Update bfLossHP in form data after set-off
            formData.setBfLossHP(remainingToSetOff);
            log.info("Updated bfLossHP after set-off: {}", remainingToSetOff);
        }
        
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
    
    /**
     * Calculate House Property income from FlatFormData
     * Following CBDT rules for HP income computation
     */
    private double calculateHousePropertyIncome(com.itr.dto.FlatFormData formData) {
        String hpType = formData.getHpType();
        double grossRent = formData.getGrossRent();
        double munTax = formData.getMunTax();
        double homeLoanInt = formData.getHomeLoanInt();
        
        if ("SELF_OCCUPIED".equalsIgnoreCase(hpType) || "self_occupied".equalsIgnoreCase(hpType)) {
            // Self-occupied: Annual value is 0, interest is loss
            // Standard deduction is 30% of nil (which is 0)
            // Income from HP = 0 - 30% of 0 - interest = -interest (or 0 if interest is 0)
            double income = -homeLoanInt;
            // Cap interest deduction at 200000 for self-occupied
            double maxInterest = 200000;
            if (formData.getHomeLoanInt() > maxInterest) {
                income = -maxInterest;
            }
            return income;
        } else {
            // Let-out or deemed let-out
            // GAV = Gross Rent - Municipal Taxes
            double gav = grossRent - munTax;
            // Standard deduction = 30% of GAV
            double stdDed = 0.30 * Math.max(gav, 0);
            // Net Annual Value = GAV - Std Ded
            double nav = Math.max(gav, 0) - stdDed;
            // Income from HP = NAV - Interest on loan
            return nav - homeLoanInt;
        }
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
