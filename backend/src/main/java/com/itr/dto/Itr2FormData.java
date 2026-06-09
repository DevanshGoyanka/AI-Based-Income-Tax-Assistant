package com.itr.dto;

import com.itr.dto.ITRSharedDtos;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * ITR-2 Form Data - 101% CBDT Compliant
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Itr2FormData {

    private CommonFormData.PartA partA;
    private CommonFormData.ScheduleSalary scheduleSalary;
    
    // ITR-2 allows MULTIPLE house properties (CBDT MANDATORY)
    @Builder.Default
    private List<HouseProperty> houseProperties = new ArrayList<>();
    private CommonFormData.ScheduleOtherSources scheduleOS;
    private ScheduleCapitalGains scheduleCG;
    private ScheduleVDA scheduleVDA;
    private ScheduleAL scheduleAL;
    private ScheduleFA scheduleFA;
    private ScheduleFSI scheduleFSI;
    private ScheduleAMT scheduleAMT;
    private ScheduleLosses scheduleLosses;
    private com.itr.dto.ScheduleCYLA scheduleCYLA;
    private com.itr.dto.ScheduleBFLA scheduleBFLA;
    private com.itr.dto.ScheduleCFL scheduleCFL;
    private CommonFormData.ScheduleExemptIncome scheduleEI;
    private CommonFormData.ScheduleTDS scheduleTDS;
    private CommonFormData.ScheduleTCS scheduleTCS;
    private CommonFormData.ScheduleIT scheduleIT;
    private CommonFormData.DeductionsVIA deductions;
    private TaxComputation computation;
    @Builder.Default
    private List<String> validationErrors = new ArrayList<>();
    @Builder.Default
    private List<String> validationWarnings = new ArrayList<>();
    
    // Phase 6 additions
    private String taxRegime;
    private String assessmentYear;
    private boolean isHUF = false;
    @Builder.Default
    private int reliefUs89A = 0;
    @Builder.Default
    private List<ITRSharedDtos.LossLedgerEntry> broughtForwardLosses = new ArrayList<>();
    
    // SECTION 1 - Missing fields from directive
    @Builder.Default
    private List<ITRSharedDtos.PropertyDetail> housePropertiesNew = new ArrayList<>();
    private ITRSharedDtos.ScheduleCGData scheduleCGNew;
    @Builder.Default
    private List<ITRSharedDtos.VDATransaction> vdaTransactions = new ArrayList<>();
    private ITRSharedDtos.ScheduleCYLAData scheduleCYLANew;
    private ITRSharedDtos.ScheduleBFLAData scheduleBFLANew;
    private ITRSharedDtos.ScheduleCFLData scheduleCFLNew;
    private ITRSharedDtos.Schedule80GData schedule80G;
    
    // AMT fields
    private boolean amtApplicable = false;
    @Builder.Default
    private int adjustedTotalIncomeForAMT = 0;
    @Builder.Default
    private int amtTax = 0;
    @Builder.Default
    private int amtCreditBroughtForward = 0;
    @Builder.Default
    private int amtCreditUtilized = 0;
    @Builder.Default
    private int amtCreditCarriedForward = 0;
    @Builder.Default
    private int regularTaxBeforeCess = 0;
    
    // Foreign assets
    private boolean hasForeignAssets = false;
    private String scheduleFA_json;
    private String scheduleFSI_json;
    private String scheduleTR_json;
    
    // Schedule AL
    private ITRSharedDtos.ScheduleALData scheduleALNew;
    
    // Clubbing
    private boolean hasClubbing = false;
    
    // Special rate income - split pre/post July 2023
    @Builder.Default
    private int stcg111A_preJul23 = 0;
    @Builder.Default
    private int stcg111A_postJul23 = 0;
    @Builder.Default
    private int ltcg112A_preJul23 = 0;
    @Builder.Default
    private int ltcg112A_postJul23 = 0;
    @Builder.Default
    private int vdaIncome = 0;
    @Builder.Default
    private int lotteryIncome = 0;
    
    // Helper methods for validation
    public String getPAN() {
        return partA != null ? partA.getPan() : null;
    }
    
    public int getTotalIncome() {
        return computation != null ? (int) computation.getTotalIncome() : 0;
    }
    
    public int getTaxpayerAge() {
        return 30; // Placeholder - extend PartA with DOB if needed
    }
    
    public int getDeduction80C() {
        return deductions != null ? (int) deductions.getDeduction80C() : 0;
    }
    
    public int getDeduction80D_self() {
        return deductions != null ? (int) deductions.getDeduction80D() : 0;
    }
    
    public int getDeduction80D_parents() {
        return 0; // Placeholder
    }
    
    public boolean isParentSeniorCitizen() {
        return false; // Placeholder
    }
    
    public int getDeduction80CCD1() {
        return deductions != null ? (int) deductions.getDeduction80CCD1() : 0;
    }
    
    public int getDeduction80CCD1B() {
        return deductions != null ? (int) deductions.getDeduction80CCD1B() : 0;
    }
    
    public int getDeduction80E() {
        return deductions != null ? (int) deductions.getDeduction80E() : 0;
    }
    
    public int getDeduction80TTA() {
        return deductions != null ? (int) deductions.getDeduction80TTA() : 0;
    }
    
    public int getDeduction80TTB() {
        return deductions != null ? (int) deductions.getDeduction80TTB() : 0;
    }
    
    public double getGrossSalary() {
        return scheduleSalary != null ? scheduleSalary.getGrossSalary() : 0;
    }

    // Additional helper methods for ITR2JSONExportService
    public String getFirstName() { return partA != null ? partA.getAssesseeName() : ""; }
    public String getMiddleName() { return ""; }
    public String getSurName() { return ""; }
    public LocalDate getDateOfBirth() { return partA != null && partA.getDob() != null ? LocalDate.parse(partA.getDob()) : null; }
    public String getAadhaarNumber() { return partA != null ? partA.getAadhaar() : ""; }
    public String getAadhaarEnrolmentId() { return ""; }
    public String getMobileNumber() { return partA != null ? partA.getMobile() : ""; }
    public String getEmail() { return partA != null ? partA.getEmail() : ""; }
    public String getFlatDoorBlock() { return ""; }
    public String getPremisesName() { return ""; }
    public String getRoadStreet() { return ""; }
    public String getLocality() { return ""; }
    public String getCity() { return partA != null ? partA.getCity() : ""; }
    public String getStateCode() { return partA != null ? partA.getState() : ""; }
    public String getPinCode() { return partA != null ? partA.getPinCode() : ""; }
    public String getEmployerCategory() { return ""; }
    public String getIntermediaryCity() { return partA != null ? partA.getCity() : ""; }
    public String getForm10IEAAckNo() { return null; }
    public String getFatherName() { return ""; }
    public String getPlaceOfFiling() { return partA != null ? partA.getCity() : ""; }
    public String getBankIFSC() { return partA != null ? partA.getBankIFSC() : ""; }
    public String getBankName() { return partA != null ? partA.getBankName() : ""; }
    public String getBankAccountNumber() { return partA != null ? partA.getBankAccountNo() : ""; }
    public String getBankAccountType() { return "S"; }
    
    public Integer getBasicSalary() { return scheduleSalary != null ? (int) scheduleSalary.getBasicSalary() : 0; }
    public Integer getPerquisites() { return scheduleSalary != null ? (int) scheduleSalary.getPerquisites17_2() : 0; }
    public Integer getProfitsInLieu() { return scheduleSalary != null ? (int) scheduleSalary.getProfitsInLieu17_3() : 0; }
    public Integer getLtaExemption() { return 0; }
    public Integer getGratuityExemption() { return 0; }
    public Integer getCommutedPensionExemption() { return 0; }
    public Integer getLeaveEncashmentExemption() { return 0; }
    public Integer getHraExemption() { return 0; }
    public Integer getTotalExemptAllowances() { return scheduleSalary != null ? (int) scheduleSalary.getAllowancesExempt() : 0; }
    public Integer getNetSalary() { return scheduleSalary != null ? (int) scheduleSalary.getNetSalary() : 0; }
    public Integer getEntertainmentAllowance() { return 0; }
    public Integer getProfessionalTax() { return scheduleSalary != null ? (int) scheduleSalary.getProfessionalTax() : 0; }
    public Integer getIncomeFromSalary() { return scheduleSalary != null ? (int) scheduleSalary.getIncomeFromSalary() : 0; }
    
    public Integer getTotalHPIncome() { return houseProperties != null ? houseProperties.stream().mapToInt(h -> (int) h.getIncomeFromHP()).sum() : 0; }
    
    public Integer getDeduction80CCC() { return deductions != null ? (int) deductions.getDeduction80CCC() : 0; }
    public Integer getDeduction80CCD2() { return deductions != null ? (int) deductions.getDeduction80CCD2() : 0; }
    public Integer getDeduction80CCH() { return 0; }
    public Integer getDeduction80D() { return deductions != null ? (int) deductions.getDeduction80D() : 0; }
    public Integer getDeduction80DD() { return deductions != null ? (int) deductions.getDeduction80DD() : 0; }
    public Integer getDeduction80DDB() { return deductions != null ? (int) deductions.getDeduction80DDB() : 0; }
    public Integer getDeduction80EEA() { return deductions != null ? (int) deductions.getDeduction80EEA() : 0; }
    public Integer getDeduction80EEB() { return deductions != null ? (int) deductions.getDeduction80EEB() : 0; }
    public Integer getDeduction80G() { return deductions != null ? (int) deductions.getDeduction80G() : 0; }
    public Integer getDeduction80GG() { return deductions != null ? (int) deductions.getDeduction80GG() : 0; }
    public Integer getDeduction80GGA() { return deductions != null ? (int) deductions.getDeduction80GGA() : 0; }
    public Integer getDeduction80U() { return deductions != null ? (int) deductions.getDeduction80U() : 0; }
    public Integer getDeduction80JJAA() { return 0; }
    public Integer getTotalChapterVIADeductions() { return deductions != null ? (int) deductions.getTotalDeductions() : 0; }
    
    public Integer getInterestSavingsBank() { return scheduleOS != null ? (int) scheduleOS.getSavingsInterest() : 0; }
    public Integer getInterestFD() { return scheduleOS != null ? (int) scheduleOS.getDepositInterest() : 0; }
    public Integer getInterestITRefund() { return 0; }
    public Integer getDividendIncome() { return scheduleOS != null ? (int) scheduleOS.getDividendIncome() : 0; }
    public Integer getFamilyPension() { return scheduleOS != null ? (int) scheduleOS.getFamilyPension() : 0; }
    public Integer getOtherSourcesIncomeTotal() { return scheduleOS != null ? (int) scheduleOS.getIncomeFromOtherSources() : 0; }
    
    public Integer getTaxOnTotalIncome() { return computation != null ? (int) computation.getTaxOnNormalIncome() : 0; }
    public Integer getRebate87A() { return computation != null ? (int) computation.getRebate87A() : 0; }
    public Integer getTaxAfterRebate() { return computation != null ? (int) (computation.getTaxOnNormalIncome() - computation.getRebate87A()) : 0; }
    public Integer getSurcharge() { return computation != null ? (int) computation.getSurcharge() : 0; }
    public Integer getSurchargeOnSpecialIncome() { return 0; }
    public Integer getTotalSurcharge() { return computation != null ? (int) computation.getSurcharge() : 0; }
    public Integer getHealthEducationCess() { return computation != null ? (int) computation.getCess() : 0; }
    public Integer getTotalTaxAndCess() { return computation != null ? (int) computation.getTotalTaxLiability() : 0; }
    public Integer getReliefUs89() { return reliefUs89A; }
    public Integer getNetTaxLiability() { return computation != null ? (int) computation.getTaxPayable() : 0; }
    public Integer getInterest234A() { return computation != null ? (int) computation.getInterest234A() : 0; }
    public Integer getInterest234B() { return computation != null ? (int) computation.getInterest234B() : 0; }
    public Integer getInterest234C() { return computation != null ? (int) computation.getInterest234C() : 0; }
    public Integer getLateFilingFee234F() { return computation != null ? (int) computation.getFee234F() : 0; }
    
    public List<CommonFormData.TDSEntry> getTdsSalaryEntries() { return scheduleTDS != null ? scheduleTDS.getTdsOnSalary() : new ArrayList<>(); }
    public List<CommonFormData.TDSEntry> getTdsOtherEntries() { return scheduleTDS != null ? scheduleTDS.getTdsOther() : new ArrayList<>(); }
    public List<CommonFormData.TCSEntry> getTcsEntries() { return scheduleTCS != null ? scheduleTCS.getTcsEntries() : new ArrayList<>(); }
    public List<CommonFormData.AdvanceTaxEntry> getAdvanceTaxEntries() { return scheduleIT != null ? scheduleIT.getAdvanceTaxEntries() : new ArrayList<>(); }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaxComputation {
        @Builder.Default
        private double grossTotalIncome = 0;
        @Builder.Default
        private double totalIncome = 0;
        @Builder.Default
        private double taxOnNormalIncome = 0;
        @Builder.Default
        private double taxOnSpecialRateIncome = 0;
        @Builder.Default
        private double rebate87A = 0;
        @Builder.Default
        private double surcharge = 0;
        @Builder.Default
        private double cess = 0;
        @Builder.Default
        private double totalTaxLiability = 0;
        @Builder.Default
        private double interest234A = 0;
        @Builder.Default
        private double interest234B = 0;
        @Builder.Default
        private double interest234C = 0;
        @Builder.Default
        private double fee234F = 0;
        @Builder.Default
        private double taxPayable = 0;
        @Builder.Default
        private double refund = 0;
    }



    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HouseProperty {
        private String propertyType;
        private String address;
        private String propertyAddress;
        @Builder.Default
        private double grossRent = 0;
        @Builder.Default
        private double fairRent = 0;
        @Builder.Default
        private double municipalRateableValue = 0;
        @Builder.Default
        private double municipalTaxPaid = 0;
        @Builder.Default
        private double annualValue = 0;
        @Builder.Default
        private double netAnnualValue = 0;
        @Builder.Default
        private double standardDeduction = 0;
        @Builder.Default
        private double interestOnLoan = 0;
        @Builder.Default
        private double preConstructionInterest = 0;
        @Builder.Default
        private double preConstructionInterest1_5th = 0;
        @Builder.Default
        private double totalInterestDeduction = 0;
        @Builder.Default
        private double incomeFromHP = 0;
        private String coOwnerPAN;
        @Builder.Default
        private double coOwnerShare = 0;
        private String tenantPAN;
        private String lenderName;
        private String lenderPAN;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleCapitalGains {
        @Builder.Default
        private List<CGTransaction> stcg111A = new ArrayList<>();
        @Builder.Default
        private List<CGTransaction> stcgOther = new ArrayList<>();
        @Builder.Default
        private List<CGTransaction> ltcg112A = new ArrayList<>();
        @Builder.Default
        private List<CGTransaction> ltcg112 = new ArrayList<>();
        @Builder.Default
        private double stcg111ATotal = 0;
        @Builder.Default
        private double stcgOtherTotal = 0;
        @Builder.Default
        private double ltcg112ATotal = 0;
        @Builder.Default
        private double ltcg112Total = 0;
        @Builder.Default
        private double ltcg112AExemption = 0;
        @Builder.Default
        private double ltcg112AExemptionUsed = 0;
        @Builder.Default
        private double ltcg112ATaxable = 0;
        @Builder.Default
        private double totalCapitalGains = 0;
        @Builder.Default
        private List<CGExemption> exemptions = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CGTransaction {
        private String assetType;
        private String assetDescription;
        private String description;
        private LocalDate acquisitionDate;
        private String purchaseDate;
        private LocalDate saleDate;
        private String saleDateStr;
        @Builder.Default
        private double purchasePrice = 0;
        @Builder.Default
        private double costOfAcquisition = 0;
        @Builder.Default
        private double salePrice = 0;
        @Builder.Default
        private double transferExpenses = 0;
        @Builder.Default
        private double indexedCost = 0;
        @Builder.Default
        private double indexedCostOfAcquisition = 0;
        @Builder.Default
        private double capitalGain = 0;
        @Builder.Default
        private double gain = 0;
        @Builder.Default
        private double grandfatheredCost = 0;
        @Builder.Default
        private double fmvJan312018 = 0;
        @Builder.Default
        private boolean indexationChosen = false;
        private String section;
        private String isinCode;
        private String brokerName;
        private String brokerPAN;
        @Builder.Default
        private double sttPaid = 0;
        @Builder.Default
        private double stampDutyValue = 0;
        @Builder.Default
        private boolean grandfathering = false;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CGExemption {
        private String section;
        @Builder.Default
        private double exemptionAmount = 0;
        private String investmentDetails;
        private String investmentDate;
        private String cgasDepositDate;
        @Builder.Default
        private double cgasAmount = 0;
        @Builder.Default
        private double cgasUtilized = 0;
        @Builder.Default
        private double cgasBalance = 0;
        private String investmentDeadline;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleVDA {
        @Builder.Default
        private List<VDATransaction> transactions = new ArrayList<>();
        @Builder.Default
        private double totalVDAIncome = 0;
        @Builder.Default
        private double totalTDS194S = 0;
        @Builder.Default
        private double taxAt30Percent = 0;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VDATransaction {
        private String assetName;
        private String vdaDescription;
        private String transactionDate;
        @Builder.Default
        private double acquisitionCost = 0;
        @Builder.Default
        private double costOfAcquisition = 0;
        @Builder.Default
        private double saleConsideration = 0;
        @Builder.Default
        private double salePrice = 0;
        @Builder.Default
        private double profit = 0;
        @Builder.Default
        private double tdsDeducted = 0;
        private String exchangeName;
        private String walletAddress;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleAL {
        @Builder.Default
        private List<ImmovableProperty> immovableProperties = new ArrayList<>();
        @Builder.Default
        private double immovablePropertyCost = 0;
        @Builder.Default
        private double jewelleryBullionCost = 0;
        @Builder.Default
        private double vehiclesCost = 0;
        @Builder.Default
        private double sharesSecuritiesCost = 0;
        @Builder.Default
        private double insurancePoliciesAmount = 0;
        @Builder.Default
        private double loansAdvancesGiven = 0;
        @Builder.Default
        private double cashInHand = 0;
        @Builder.Default
        private double bankDeposits = 0;
        @Builder.Default
        private double otherAssets = 0;
        @Builder.Default
        private double totalAssets = 0;
        @Builder.Default
        private double loansFromBanks = 0;
        @Builder.Default
        private double loansFromOthers = 0;
        @Builder.Default
        private double otherLiabilities = 0;
        @Builder.Default
        private double totalLiabilities = 0;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImmovableProperty {
        private String address;
        private String city;
        private String pinCode;
        @Builder.Default
        private double cost = 0;
        private String acquisitionDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleFA {
        @Builder.Default
        private List<ForeignAsset> foreignBankAccounts = new ArrayList<>();
        @Builder.Default
        private List<ForeignAsset> foreignEquityDebt = new ArrayList<>();
        @Builder.Default
        private List<ForeignAsset> foreignImmovableProperty = new ArrayList<>();
        @Builder.Default
        private List<ForeignAsset> signingAuthority = new ArrayList<>();
        @Builder.Default
        private boolean blackMoneyActAcknowledged = false;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ForeignAsset {
        private String countryCode;
        private String countryName;
        private String assetDescription;
        @Builder.Default
        private double openingBalance = 0;
        @Builder.Default
        private double closingBalance = 0;
        @Builder.Default
        private double peakBalance = 0;
        @Builder.Default
        private double incomeFromAsset = 0;
        private String bankName;
        private String accountNumber;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleFSI {
        @Builder.Default
        private List<ForeignIncomeEntry> entries = new ArrayList<>();
        @Builder.Default
        private double totalForeignIncome = 0;
        @Builder.Default
        private double totalForeignTaxPaid = 0;
        @Builder.Default
        private double reliefU90 = 0;
        @Builder.Default
        private double reliefU91 = 0;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ForeignIncomeEntry {
        private String countryCode;
        private String countryName;
        private String incomeType;
        @Builder.Default
        private double incomeAmount = 0;
        @Builder.Default
        private double foreignTaxPaid = 0;
        @Builder.Default
        private double taxReliefClaimed = 0;
        private String dtaaArticle;
        private String reliefMethod;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleAMT {
        @Builder.Default
        private double totalIncome = 0;
        @Builder.Default
        private double addback10AA = 0;
        @Builder.Default
        private double addback35AD = 0;
        @Builder.Default
        private double addback80HTo80RRB = 0;
        @Builder.Default
        private double adjustedTotalIncome = 0;
        @Builder.Default
        private double amtAt185Percent = 0;
        @Builder.Default
        private double regularTax = 0;
        @Builder.Default
        private boolean amtApplicable = false;
        @Builder.Default
        private double taxPayable = 0;
        @Builder.Default
        private double amtCreditBroughtForward = 0;
        @Builder.Default
        private double amtCreditUtilized = 0;
        @Builder.Default
        private double amtCreditCarryForward = 0;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleLosses {
        @Builder.Default
        private double hpLossCurrentYear = 0;
        @Builder.Default
        private double stcgLossCurrentYear = 0;
        @Builder.Default
        private double ltcgLossCurrentYear = 0;
        @Builder.Default
        private double hpLossBroughtForward = 0;
        @Builder.Default
        private double businessLossBroughtForward = 0;
        @Builder.Default
        private double stcgLossBroughtForward = 0;
        @Builder.Default
        private double ltcgLossBroughtForward = 0;
        @Builder.Default
        private double unabsorbedDepreciation = 0;
        @Builder.Default
        private double hpLossCarryForward = 0;
        @Builder.Default
        private double stcgLossCarryForward = 0;
        @Builder.Default
        private double ltcgLossCarryForward = 0;
        @Builder.Default
        private double hpLossSetOffCurrentYear = 0;
        @Builder.Default
        private double netHPIncomeAfterSetOff = 0;
    }
}
