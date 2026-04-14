package com.itr.dto.itd;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * ITD Tax Payment Schedule DTOs: TDS1, TDS2, TDS3, TCS, AdvanceTax
 * Reference: ITR_Import_JSON_Validation.md Section 4 (TaxPaid)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ITDTaxPaidDtos {

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TaxPaid {
        @JsonProperty("TDS1")        private TDS1 tds1;
        @JsonProperty("TDS2")        private TDS2 tds2;
        @JsonProperty("TDS3")        private TDS3 tds3;
        @JsonProperty("TCS")         private TCS tcs;
        @JsonProperty("AdvanceTax")  private AdvanceTax advanceTax;
        @JsonProperty("TotalTaxPaid") private Integer totalTaxPaid;
        @JsonProperty("BalTaxPayable") private Integer balTaxPayable;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TDS1 {
        @JsonProperty("TDSSal")                   private List<TDSSalEntry> tdsSal;
        @JsonProperty("TotalTDS1TaxDeducted")     private Integer totalTDS1TaxDeducted;
        @JsonProperty("TotalTDS1TaxClaimed")      private Integer totalTDS1TaxClaimed;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TDSSalEntry {
        @JsonProperty("EmployerOrDeductorOrCollectTAN")  private String tan;
        @JsonProperty("EmployerOrDeductorOrCollectName") private String name;
        @JsonProperty("TotalTDSSal")                     private Integer totalTDSSal;
        @JsonProperty("ClaimOutOfTotTDSSal")             private Integer claimOutOfTotTDSSal;
        @JsonProperty("RuleOf26As")                      private String ruleOf26As;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TDS2 {
        @JsonProperty("TDSOthThanSals")                      private List<TDSOthThanSalEntry> tdsOthThanSals;
        @JsonProperty("TotalTDSOthThanSalTaxDeducted")       private Integer totalTDSOthThanSalTaxDeducted;
        @JsonProperty("TotalTDSOthThanSalTaxClaimed")        private Integer totalTDSOthThanSalTaxClaimed;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TDSOthThanSalEntry {
        @JsonProperty("DeductorTAN")                 private String deductorTAN;
        @JsonProperty("DeductorName")                private String deductorName;
        @JsonProperty("AmtOnWhichTDSDeducted")       private Integer amtOnWhichTDSDeducted;
        @JsonProperty("TaxDeducted")                 private Integer taxDeducted;
        @JsonProperty("YrOfTaxDeduction")            private String yrOfTaxDeduction;
        @JsonProperty("ClaimOutOfTotTDSOthThanSals") private Integer claimOutOfTotTDSOthThanSals;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TDS3 {
        @JsonProperty("TDSonSal194P") private List<TDS194PEntry> tdsOnSal194P;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TDS194PEntry {
        @JsonProperty("DeductorTAN")  private String deductorTAN;
        @JsonProperty("DeductorName") private String deductorName;
        @JsonProperty("TaxDeducted")  private Integer taxDeducted;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TCS {
        @JsonProperty("TCS")                      private List<TCSEntry> tcsEntries;
        @JsonProperty("TotalTCSClaimedThisYear")  private Integer totalTCSClaimedThisYear;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TCSEntry {
        @JsonProperty("CollectorTAN")             private String collectorTAN;
        @JsonProperty("CollectorName")            private String collectorName;
        @JsonProperty("AmtOnWhichTCSCollected")   private Integer amtOnWhichTCSCollected;
        @JsonProperty("TaxCollected")             private Integer taxCollected;
        @JsonProperty("ClaimOutOfTotTCS")         private Integer claimOutOfTotTCS;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AdvanceTax {
        @JsonProperty("AdvTaxDetails")   private List<AdvTaxEntry> advTaxDetails;
        @JsonProperty("TotalAdvTaxPaid") private Integer totalAdvTaxPaid;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AdvTaxEntry {
        @JsonProperty("BSRCode")      private String bsrCode;
        @JsonProperty("DateDep")      private String dateDep;
        @JsonProperty("SrlNoOfChaln") private String srlNoOfChaln;
        @JsonProperty("Amt")          private Integer amt;
        @JsonProperty("Type")         private String type;
    }
}
