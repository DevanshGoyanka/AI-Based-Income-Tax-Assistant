package com.itr.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * Form 16 Part A Data - TDS Certificate from TRACES
 * Reference: ITR_Import_JSON_Validation.md Section 1
 */
@Data
public class Form16PartAData {
    private String tanOfEmployer;
    private String panOfEmployee;
    private String employerName;
    private String employerAddress;
    private String assessmentYear;
    private String financialYear;
    private long aggregateSalaryPaid;
    private long aggregateTdsDeposited;
    private String certificateNumber;
    private String acknowledgementNumberTraces;
    private String periodFrom;
    private String periodTo;
    private List<QuarterlyTDS> quarterlyBreakup = new ArrayList<>();

    @Data
    public static class QuarterlyTDS {
        private String quarter; // Q1, Q2, Q3, Q4
        private String dateOfPaymentCredit;
        private String dateOfTdsDeposit;
        private String bsrCode;
        private String challanSerialNumber;
        private long amountOfTaxDeposited;
    }
}
