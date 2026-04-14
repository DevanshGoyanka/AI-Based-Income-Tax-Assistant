package com.itr.dto.itd;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * ITR-1 JSON Output - complete wrapper matching ITD schema
 * Reference: ITR_Import_JSON_Validation.md Section 4
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ITR1Output {

    @JsonProperty("ITR")
    private ITRWrapper itr;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ITRWrapper {
        @JsonProperty("ITR1") private ITR1 itr1;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ITR1 {
        @JsonProperty("CreationInfo") private ITDCommonDtos.CreationInfo creationInfo;
        @JsonProperty("Form_ITR1")    private FormITR1 formItr1;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FormITR1 {
        @JsonProperty("PersonalInfo")      private ITDCommonDtos.PersonalInfo personalInfo;
        @JsonProperty("FilingStatus")      private ITDCommonDtos.FilingStatus filingStatus;
        @JsonProperty("IncomeDeductions")  private IncomeDeductions incomeDeductions;
        @JsonProperty("TaxComputation")    private TaxComputation taxComputation;
        @JsonProperty("TaxPaid")           private ITDTaxPaidDtos.TaxPaid taxPaid;
        @JsonProperty("Refund")            private Refund refund;
        @JsonProperty("Schedule80G")       private Schedule80G schedule80G;
        @JsonProperty("ExemptIncome")      private ExemptIncome exemptIncome;
        @JsonProperty("Verification")      private Verification verification;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class IncomeDeductions {
        @JsonProperty("GrossSalary")           private Integer grossSalary;
        @JsonProperty("Salary17_1")            private Integer salary17_1;
        @JsonProperty("PerquisitesValue17_2")  private Integer perquisitesValue17_2;
        @JsonProperty("ProfitsInLieuSalary17_3") private Integer profitsInLieuSalary17_3;
        @JsonProperty("AllwncExemptUs10")      private Integer allwncExemptUs10;
        @JsonProperty("NetSalary")             private Integer netSalary;
        @JsonProperty("DeductionUs16")         private Integer deductionUs16;
        @JsonProperty("DeductionUs16ia")       private Integer deductionUs16ia;
        @JsonProperty("DeductionUs16ii")       private Integer deductionUs16ii;
        @JsonProperty("DeductionUs16iii")      private Integer deductionUs16iii;
        @JsonProperty("IncomeFromSal")         private Integer incomeFromSal;
        @JsonProperty("TypeOfHP")              private String typeOfHP;
        @JsonProperty("GrossRentReceived")     private Integer grossRentReceived;
        @JsonProperty("TaxPaidLocalAuthority") private Integer taxPaidLocalAuthority;
        @JsonProperty("AnnualValue")           private Integer annualValue;
        @JsonProperty("StandardDeduction30Pct") private Integer standardDeduction30Pct;
        @JsonProperty("InterestPayable")       private Integer interestPayable;
        @JsonProperty("ArrearUnrealizedRentReceived") private Integer arrearUnrealizedRentReceived;
        @JsonProperty("IncomeFromHP")          private Integer incomeFromHP;
        @JsonProperty("IncomeOthSrc")          private Integer incomeOthSrc;
        @JsonProperty("OthersInc")             private List<OthersIncEntry> othersInc;
        @JsonProperty("FamilyPension")         private Integer familyPension;
        @JsonProperty("FamilyPensionDedUs57iia") private Integer familyPensionDedUs57iia;
        @JsonProperty("GrossTotIncome")        private Integer grossTotIncome;
        @JsonProperty("LTCGUs112A")            private Integer ltcgUs112A;
        @JsonProperty("ExemptInc")             private Integer exemptInc;
        @JsonProperty("DeductionUs80C")        private Integer deductionUs80C;
        @JsonProperty("DeductionUs80CCC")      private Integer deductionUs80CCC;
        @JsonProperty("DeductionUs80CCD1")     private Integer deductionUs80CCD1;
        @JsonProperty("DeductionUs80CCD1B")    private Integer deductionUs80CCD1B;
        @JsonProperty("DeductionUs80CCD2")     private Integer deductionUs80CCD2;
        @JsonProperty("DeductionUs80CCH")      private Integer deductionUs80CCH;
        @JsonProperty("DeductionUs80D")        private Integer deductionUs80D;
        @JsonProperty("DeductionUs80DD")       private Integer deductionUs80DD;
        @JsonProperty("DeductionUs80DDB")      private Integer deductionUs80DDB;
        @JsonProperty("DeductionUs80E")        private Integer deductionUs80E;
        @JsonProperty("DeductionUs80EE")       private Integer deductionUs80EE;
        @JsonProperty("DeductionUs80EEA")      private Integer deductionUs80EEA;
        @JsonProperty("DeductionUs80EEB")      private Integer deductionUs80EEB;
        @JsonProperty("DeductionUs80G")        private Integer deductionUs80G;
        @JsonProperty("DeductionUs80GG")       private Integer deductionUs80GG;
        @JsonProperty("DeductionUs80GGA")      private Integer deductionUs80GGA;
        @JsonProperty("DeductionUs80GGC")      private Integer deductionUs80GGC;
        @JsonProperty("DeductionUs80TTA")      private Integer deductionUs80TTA;
        @JsonProperty("DeductionUs80TTB")      private Integer deductionUs80TTB;
        @JsonProperty("DeductionUs80U")        private Integer deductionUs80U;
        @JsonProperty("TotalChapVIADeductions") private Integer totalChapVIADeductions;
        @JsonProperty("TotalIncome")           private Integer totalIncome;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OthersIncEntry {
        @JsonProperty("IncNatureDesc")   private String incNatureDesc;
        @JsonProperty("OthSrcNatureInc") private String othSrcNatureInc;
        @JsonProperty("OthSrcInc")       private Integer othSrcInc;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TaxComputation {
        @JsonProperty("TaxPayableOnTI")         private Integer taxPayableOnTI;
        @JsonProperty("RebateUs87A")            private Integer rebateUs87A;
        @JsonProperty("TaxAfterRebate")         private Integer taxAfterRebate;
        @JsonProperty("Surcharge25")            private Integer surcharge25;
        @JsonProperty("Surcharge37")            private Integer surcharge37;
        @JsonProperty("SurchargeOnSpecialIncome") private Integer surchargeOnSpecialIncome;
        @JsonProperty("TotalSurcharge")         private Integer totalSurcharge;
        @JsonProperty("HealthEduCess")          private Integer healthEduCess;
        @JsonProperty("TaxPayableOnRebate")     private Integer taxPayableOnRebate;
        @JsonProperty("TotalTaxAndCess")        private Integer totalTaxAndCess;
        @JsonProperty("ReliefUs89")             private Integer reliefUs89;
        @JsonProperty("NetTaxLiability")        private Integer netTaxLiability;
        @JsonProperty("IntrstUs234A")           private Integer intrstUs234A;
        @JsonProperty("IntrstUs234B")           private Integer intrstUs234B;
        @JsonProperty("IntrstUs234C")           private Integer intrstUs234C;
        @JsonProperty("LateFilingFeeUs234F")    private Integer lateFilingFeeUs234F;
        @JsonProperty("TotalIntrstPnltyFee")    private Integer totalIntrstPnltyFee;
        @JsonProperty("GrossTaxLiability")      private Integer grossTaxLiability;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Refund {
        @JsonProperty("BankAccountNo")  private String bankAccountNo;
        @JsonProperty("BankIFSCCode")   private String bankIFSCCode;
        @JsonProperty("BankName")       private String bankName;
        @JsonProperty("AccountType")    private String accountType;
        @JsonProperty("IsIfscValid")    private String isIfscValid;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Schedule80G {
        @JsonProperty("DonationUs80GCash")       private List<Donation80GEntry> donationUs80GCash;
        @JsonProperty("DonationUs80GChequeOrDD") private List<Donation80GEntry> donationUs80GChequeOrDD;
        @JsonProperty("TotDonationsUs80G")       private Integer totDonationsUs80G;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Donation80GEntry {
        @JsonProperty("NameOfDonee")   private String nameOfDonee;
        @JsonProperty("AddressOfDonee") private String addressOfDonee;
        @JsonProperty("PINCode")       private String pinCode;
        @JsonProperty("DoneePAN")      private String doneePAN;
        @JsonProperty("DonationAmt")   private Integer donationAmt;
        @JsonProperty("EligibleAmt")   private Integer eligibleAmt;
        @JsonProperty("DeductionUs80G") private Integer deductionUs80G;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ExemptIncome {
        @JsonProperty("ExcAgriInc")                private Integer excAgriInc;
        @JsonProperty("ShareFromHUF")              private Integer shareFromHUF;
        @JsonProperty("ShareFromFirm")             private Integer shareFromFirm;
        @JsonProperty("InterestFromGovtSecurities") private Integer interestFromGovtSecurities;
        @JsonProperty("GratuityExempt")            private Integer gratuityExempt;
        @JsonProperty("LeaveEncashmentExempt")     private Integer leaveEncashmentExempt;
        @JsonProperty("VRSExempt")                 private Integer vrsExempt;
        @JsonProperty("CommutedPensionExempt")     private Integer commutedPensionExempt;
        @JsonProperty("PPFInterest")               private Integer ppfInterest;
        @JsonProperty("SukanyaInterest")           private Integer sukanyaInterest;
        @JsonProperty("TaxFreeBondInterest")       private Integer taxFreeBondInterest;
        @JsonProperty("LICMaturityExempt")         private Integer licMaturityExempt;
        @JsonProperty("OtherExemptIncome")         private Integer otherExemptIncome;
        @JsonProperty("AllExemptIncome")           private Integer allExemptIncome;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Verification {
        @JsonProperty("Capacity")    private String capacity;
        @JsonProperty("FatherName")  private String fatherName;
        @JsonProperty("Place")       private String place;
        @JsonProperty("Date")        private String date;
        @JsonProperty("Declaration") private String declaration;
    }
}
