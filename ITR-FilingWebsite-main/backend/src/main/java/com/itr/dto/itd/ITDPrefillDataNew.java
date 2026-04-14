package com.itr.dto.itd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * ITD Prefill JSON Data DTO - Matches REAL ITD JSON structure
 * Based on actual sample: ACUPG3482G-Prefill-2025-14_31_2026_18_50.json
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ITDPrefillDataNew {
    
    private PersonalInfo personalInfo;
    private Object Form10BC;
    private Object orgFirmInfo;
    private Object Form10BA;
    private Object form10DA;
    private Form3CD form3CD;
    private Object scheduleAL;
    private Object form3CE;
    private Form26AS form26as;
    private Object form10IBICID;
    private Object otherSourceIncome;
    private Object form10IE;
    private Object form10CCE;
    private Object form10CCF;
    private Object form10CCD;
    private Object itba;
    private Object form64D;
    private Verification verification;
    private List<BankAccountDtls> bankAccountDtls;
    private Object form56F;
    private Insights insights;
    private Object form10CCB;
    private Object form29b;
    private ScheduleCFL scheduleCFL;
    private FilingStatus filingStatus;
    private Form24Q form24q;
    private Object partAGEN2;
    private Object ScheduleEI;
    private Object Form10A;
    private Object Form10IFA;
    private Object ais;
    private Object ScheduleESOP;
    private Object formCCBA;
    private Object auditInfo;
    private List<IncDeductionsOthIncCPC> incDeductionsOthIncCPC;
    private Object formCCBD;
    private Object formCCBC;
    private Object formCCBB;
    private Object form67;
    private Object assesseeRep;
    private Object form66;
    private Object form10E;
    private Object Schedule80RA;
    private Object Form10IA;
    private Object Schedule80G;
    private Object form64A;
    private Object Form10AB;
    private Object Form10IEA;
    private Form10IF form10IF;
    private Object form10AC;
    private Object form10BOR10BB;
    private Object natOfBus;
    private Object filingReturn;
    private LastFiledITR lastFiledITR;
    private Object form3CEA;
    private Object form3CEB;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PersonalInfo {
        private String fatherName;
        private OrgFirmInfo orgFirmInfo;
        private Address address;
        private AssesseeName assesseeName;
        private String portugeseCC5A;
        private String assesseeVerName;
        private String capacity;
        private FilingStatusInner filingStatus;
        private String assesseVerPan;
        private String dob;
        private String aadhaarCardNo; // Base64 encoded
        private String pan;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrgFirmInfo {
        private AssesseeName AssesseeName;
        private String DateOFFormOrIncorp;
        private String StatusOrCompanyType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssesseeName {
        private String firstName;
        private String surNameOrOrgName;
        private String middleName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Address {
        private String zipCode;
        private String localityOrArea;
        private String residenceNo;
        private String cityOrTownOrDistrict;
        private Long mobileNo;
        private String emailAddressSecondary;
        private Long mobileNoSec;
        private String emailAddress;
        private String countryCodeMobile;
        private Phone phone;
        private String countryCode;
        private String roadOrStreet;
        private String pinCode;
        private String stateCode;
        private String residenceName;
    }

    @Data
    public static class Phone {
        // Empty in sample - no fields needed
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FilingStatusInner {
        private String residentialStatus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Form3CD {
        private PartAOI PartAOI;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartAOI {
        private String ScheduleTPSAFlg;
        private Double Amount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Form26AS {
        private TaxPayments taxPayments;
        private TdsOnOthThanSals tdsOnOthThanSals;
        private ScheduleOS scheduleOS;
        private PresumptiveInc44ADA persumptiveInc44ADA;
        private List<IncomeDeductionsOthersInc> incomeDeductionsOthersInc;
        private Double intrstFrmTermDeposit;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaxPayments {
        private List<TaxPayment> taxPayment;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaxPayment {
        private Long srlNoOfChaln;
        private String dateDep;
        private String bsrCode;
        private NameOfBankAndBranch nameOfBankAndBranch;
        private Double amt;
        private String receiptNumber;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NameOfBankAndBranch {
        private String nameOfBank;
        private String nameOfBranch;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TdsOnOthThanSals {
        private List<TdSonOthThanSal> tdSonOthThanSal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TdSonOthThanSal {
        private TaxDeductCreditDtls taxDeductCreditDtls;
        private String TDSCreditName;
        private String sectionCode;
        private Double grossAmount;
        private String headOfIncome;
        private EmployerOrDeductorOrCollectDetl employerOrDeductorOrCollectDetl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaxDeductCreditDtls {
        private Double taxClaimedOwnHands;
        private Double taxDeductedOwnHands;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmployerOrDeductorOrCollectDetl {
        private String tan;
        private String employerOrDeductorOrCollecterName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleOS {
        private IncOthThanOwnRaceHorse incOthThanOwnRaceHorse;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IncOthThanOwnRaceHorse {
        private Double dividendGross;
        private Double DividendOthThan22e;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PresumptiveInc44ADA {
        private Double grsReceipt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IncomeDeductionsOthersInc {
        private Double othSrcOthAmount;
        private String othSrcNatureDesc;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Verification {
        private Declaration declaration;
        private String capacity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Declaration {
        private String fatherName;
        private String assesseeVerPAN;
        private String assesseeVerName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BankAccountDtls {
        private List<AddtnlBankDetails> addtnlBankDetails;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddtnlBankDetails {
        private String useForRefund;
        private String bankAccountNo;
        private String bankName;
        private String ifsccode;
        private String AccountType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Insights {
        private Double intrstFrmSavingBank;
        private ScheduleOS scheduleOS;
        private Double intrstFrmTermDeposit;
        private List<IncomeDeductionsOthersInc> incomeDeductionsOthersInc;
        private UsrDeductUndChapVIAType UsrDeductUndChapVIAType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsrDeductUndChapVIAType {
        private Double Section80TTB;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleCFL {
        private List<CarryFwdLossDetail> CarryFwdLossDetail;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CarryFwdLossDetail {
        private Double OthSrcLossRaceHorseCF;
        private Double StcgLossCF;
        private String AssessmentYear;
        private String DateOfFiling;
        private Double LtcgLossCF;
        private Double HpLossCF;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FilingStatus {
        private String SeventhProvisio139;
        private Integer OptingNewTaxRegimeForm10IF;
        private String receiptNo;
        private Integer returnFileSec;
        private String origRetFiledDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Form24Q {
        private UsrDeductUndChapVIAType usrDeductUndChapVIAType;
        private Double intrstFrmSavingBank;
        private Boolean isActive;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IncDeductionsOthIncCPC {
        private String itrAy;
        private Double othSrcOthAmount;
        private String othSrcNatureDesc;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Form10IF {
        private String newTaxRegime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LastFiledITR {
        private List<Object> foreignBankDetails;
        private UsrDeductUndChapVIAType usrDeductUndChapVIAType;
        private String benefitUs115HFlg;
        private Schedule80D schedule80D;
        private String assetOutIndiaFlag;
        private List<Object> scheduleUD;
        private ScheduleSPI scheduleSPI;
        private ScheduleAMTC scheduleAMTC;
        private Integer totalNumOfMonths;
        private ScheduleHP scheduleHP;
        private List<Object> scheduleTCS;
        private FilingStatusInner filingStatus;
        private String heldUnlistedEqShrPrYrFlg;
        private String compDirectorPrvYrFlg;
        private Object scheduleMATC;
        private Object schedule5A2014;
        private List<String> natOfEmployment;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Schedule80D {
        private Sec80DSelfFamSrCtznHealth Sec80DSelfFamSrCtznHealth;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Sec80DSelfFamSrCtznHealth {
        private Object SeniorCitizenFlag;
        private Object ParentSeniorCitizenFlag;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleSPI {
        private List<Object> specifiedPerson;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleAMTC {
        private List<ScheduleAMTCDtl> scheduleAMTCDtls;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleAMTCDtl {
        private Double gross;
        private String assYr;
        private Double amtCreditSetOfEy;
        private Double amtCreditFwd;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleHP {
        private List<Object> propertyDetails;
    }
}
