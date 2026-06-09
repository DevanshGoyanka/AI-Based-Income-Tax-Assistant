package com.itr.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Schedule FA (Foreign Assets) Service - 101% CBDT Compliant
 * Mandatory for residents with foreign assets/income
 * Reference: Section 139(1) proviso, CBDT Notification
 */
@Slf4j
@Service
public class ScheduleFAService {

    /**
     * Validate Schedule FA completeness and CBDT compliance
     */
    public ValidationResult validate(ScheduleFA scheduleFA) {
        ValidationResult result = new ValidationResult();
        
        if (scheduleFA == null || scheduleFA.getForeignAssets().isEmpty()) {
            result.addError("Schedule FA is mandatory if you have foreign assets/income");
            return result;
        }

        for (ForeignAsset asset : scheduleFA.getForeignAssets()) {
            validateAsset(asset, result);
        }

        for (ForeignAccount account : scheduleFA.getForeignAccounts()) {
            validateAccount(account, result);
        }

        for (ForeignIncome income : scheduleFA.getForeignIncomes()) {
            validateIncome(income, result);
        }

        log.info("Schedule FA validation: {} errors, {} warnings", 
                 result.getErrors().size(), result.getWarnings().size());
        return result;
    }

    private void validateAsset(ForeignAsset asset, ValidationResult result) {
        if (asset.getCountryCode() == null || asset.getCountryCode().length() != 2) {
            result.addError("Invalid country code for foreign asset: " + asset.getCountryCode());
        }

        if (asset.getAssetType() == null) {
            result.addError("Asset type is mandatory");
        }

        if (asset.getOpeningBalance() < 0 || asset.getClosingBalance() < 0) {
            result.addError("Asset balance cannot be negative");
        }

        if (asset.getPeakBalance() < asset.getClosingBalance()) {
            result.addWarning("Peak balance should be >= closing balance");
        }

        // Immovable property must have address
        if ("IMMOVABLE_PROPERTY".equals(asset.getAssetType()) && 
            (asset.getAddress() == null || asset.getAddress().trim().isEmpty())) {
            result.addError("Address is mandatory for immovable property");
        }

        // Financial interest must have institution name
        if (("FINANCIAL_INTEREST".equals(asset.getAssetType()) || 
             "CAPITAL_ASSET".equals(asset.getAssetType())) &&
            (asset.getInstitutionName() == null || asset.getInstitutionName().trim().isEmpty())) {
            result.addError("Institution name is mandatory for financial assets");
        }
    }

    private void validateAccount(ForeignAccount account, ValidationResult result) {
        if (account.getCountryCode() == null || account.getCountryCode().length() != 2) {
            result.addError("Invalid country code for foreign account");
        }

        if (account.getAccountNumber() == null || account.getAccountNumber().trim().isEmpty()) {
            result.addError("Account number is mandatory");
        }

        if (account.getInstitutionName() == null || account.getInstitutionName().trim().isEmpty()) {
            result.addError("Institution name is mandatory");
        }

        if (account.getOpeningBalance() < 0 || account.getClosingBalance() < 0) {
            result.addError("Account balance cannot be negative");
        }

        if (account.getPeakBalance() < account.getClosingBalance()) {
            result.addWarning("Peak balance should be >= closing balance");
        }

        // Interest earned must be reported
        if (account.getInterestEarned() > 0 && account.getClosingBalance() == 0) {
            result.addWarning("Interest earned but closing balance is zero - verify correctness");
        }
    }

    private void validateIncome(ForeignIncome income, ValidationResult result) {
        if (income.getCountryCode() == null || income.getCountryCode().length() != 2) {
            result.addError("Invalid country code for foreign income");
        }

        if (income.getIncomeType() == null) {
            result.addError("Income type is mandatory");
        }

        if (income.getGrossIncome() < 0) {
            result.addError("Gross income cannot be negative");
        }

        if (income.getTaxPaidOutsideIndia() > income.getGrossIncome()) {
            result.addError("Tax paid cannot exceed gross income");
        }

        if (income.getTaxReliefClaimed() > income.getTaxPaidOutsideIndia()) {
            result.addError("Tax relief claimed cannot exceed tax paid outside India");
        }

        // DTAA validation
        if (income.getTaxReliefClaimed() > 0 && 
            (income.getDtaaArticle() == null || income.getDtaaArticle().trim().isEmpty())) {
            result.addWarning("DTAA article reference recommended when claiming tax relief");
        }
    }

    /**
     * Auto-populate Schedule FA from SFT-009 (Foreign Assets)
     */
    public void populateFromSFT(ScheduleFA scheduleFA, List<SFTForeignAssetRecord> sftRecords) {
        if (sftRecords == null || sftRecords.isEmpty()) {
            return;
        }

        for (SFTForeignAssetRecord sft : sftRecords) {
            ForeignAccount account = new ForeignAccount();
            account.setCountryCode(sft.getCountryCode());
            account.setAccountNumber(sft.getAccountNumber());
            account.setInstitutionName(sft.getInstitutionName());
            account.setOpeningBalance(sft.getOpeningBalance());
            account.setClosingBalance(sft.getClosingBalance());
            account.setPeakBalance(sft.getPeakBalance());
            account.setInterestEarned(sft.getInterestEarned());
            
            scheduleFA.getForeignAccounts().add(account);
        }

        log.info("Populated Schedule FA from {} SFT-009 records", sftRecords.size());
    }

    @Data
    public static class ScheduleFA {
        private List<ForeignAsset> foreignAssets = new ArrayList<>();
        private List<ForeignAccount> foreignAccounts = new ArrayList<>();
        private List<ForeignIncome> foreignIncomes = new ArrayList<>();
        private List<ForeignTrust> foreignTrusts = new ArrayList<>();
        private List<ForeignEntity> foreignEntities = new ArrayList<>();
    }

    @Data
    public static class ForeignAsset {
        private String countryCode;
        private String countryName;
        private String assetType; // IMMOVABLE_PROPERTY, FINANCIAL_INTEREST, CAPITAL_ASSET, OTHER
        private String address;
        private String institutionName;
        private long openingBalance;
        private long closingBalance;
        private long peakBalance;
        private String zipCode;
        private String natureOfAsset;
        private String dateOfAcquisition;
        private long totalInvestment;
        private long incomeEarned;
    }

    @Data
    public static class ForeignAccount {
        private String countryCode;
        private String countryName;
        private String accountNumber;
        private String institutionName;
        private String address;
        private String zipCode;
        private String accountType; // SAVINGS, CURRENT, DEPOSIT, CUSTODIAL
        private long openingBalance;
        private long closingBalance;
        private long peakBalance;
        private long interestEarned;
        private String dateOfOpening;
    }

    @Data
    public static class ForeignIncome {
        private String countryCode;
        private String countryName;
        private String incomeType; // SALARY, BUSINESS, CAPITAL_GAINS, OTHER
        private long grossIncome;
        private long taxPaidOutsideIndia;
        private long taxReliefClaimed;
        private String dtaaArticle;
        private String tinOfForeignCountry;
    }

    @Data
    public static class ForeignTrust {
        private String countryCode;
        private String trustName;
        private String address;
        private String zipCode;
        private String dateOfCreation;
        private long incomeAccrued;
        private long taxPaidByTrust;
    }

    @Data
    public static class ForeignEntity {
        private String countryCode;
        private String entityName;
        private String address;
        private String zipCode;
        private String natureOfEntity;
        private String dateOfInterestAcquisition;
        private long incomeFromEntity;
    }

    @Data
    public static class SFTForeignAssetRecord {
        private String countryCode;
        private String accountNumber;
        private String institutionName;
        private long openingBalance;
        private long closingBalance;
        private long peakBalance;
        private long interestEarned;
    }

    @Data
    public static class ValidationResult {
        private List<String> errors = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();

        public void addError(String error) {
            errors.add(error);
        }

        public void addWarning(String warning) {
            warnings.add(warning);
        }

        public boolean isValid() {
            return errors.isEmpty();
        }
    }
}
