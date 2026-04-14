package com.itr.dto.itd;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ITD CreationInfo + PersonalInfo + FilingStatus DTOs
 * Reference: ITR_Import_JSON_Validation.md Section 3.1 & 3.2
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ITDCommonDtos {

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CreationInfo {
        @JsonProperty("SWVersionNo")   private String swVersionNo;
        @JsonProperty("SWCreatedBy")   private String swCreatedBy;
        @JsonProperty("XMLCreatedBy")  private String xmlCreatedBy;
        @JsonProperty("XMLCreationDate") private String xmlCreationDate;
        @JsonProperty("IntermediaryCity") private String intermediaryCity;
        @JsonProperty("Digest")        private String digest;
        @JsonProperty("CreatedBy")     private String createdBy;
        @JsonProperty("FolioNo")       private String folioNo;
        @JsonProperty("FormName")      private String formName;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AssesseeName {
        @JsonProperty("FirstName")         private String firstName;
        @JsonProperty("MiddleName")        private String middleName;
        @JsonProperty("SurNameOrOrgName")  private String surNameOrOrgName;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Address {
        @JsonProperty("ResidenceName")          private String residenceName;
        @JsonProperty("ResidenceNo")            private String residenceNo;
        @JsonProperty("RoadOrStreet")           private String roadOrStreet;
        @JsonProperty("LocalityOrArea")         private String localityOrArea;
        @JsonProperty("CityOrTownOrDistrict")   private String cityOrTownOrDistrict;
        @JsonProperty("StateCode")              private String stateCode;
        @JsonProperty("PinCode")               private String pinCode;
        @JsonProperty("CountryCode")            private String countryCode;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PersonalInfo {
        @JsonProperty("AssesseeName")       private AssesseeName assesseeName;
        @JsonProperty("PAN")                private String pan;
        @JsonProperty("DOB")                private String dob;
        @JsonProperty("AadhaarCardNo")      private String aadhaarCardNo;
        @JsonProperty("AadhaarEnrolmentID") private String aadhaarEnrolmentId;
        @JsonProperty("PrimaryMobileNo")    private String primaryMobileNo;
        @JsonProperty("EmailID")            private String emailId;
        @JsonProperty("Address")            private Address address;
        @JsonProperty("EmployerCategory")   private String employerCategory;
        @JsonProperty("ResidentialStatus")  private String residentialStatus;
        @JsonProperty("Status")             private String status;
        @JsonProperty("AssesseeType")       private String assesseeType;
        @JsonProperty("SeventhProvisotoSec139i")                          private String seventhProviso;
        @JsonProperty("SeventhProvisotoSec139i_ConditionA_500000")        private String conditionA;
        @JsonProperty("SeventhProvisotoSec139i_ConditionB_1Crore")        private String conditionB;
        @JsonProperty("SeventhProvisotoSec139i_ConditionC_2Lakh_Travel")  private String conditionC;
        @JsonProperty("SeventhProvisotoSec139i_ConditionD_1Lakh_Electricity") private String conditionD;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FilingStatus {
        @JsonProperty("ReturnFileSec")              private String returnFileSec;
        @JsonProperty("ReturnType")                 private String returnType;
        @JsonProperty("IsRevised")                  private String isRevised;
        @JsonProperty("OriginalReturnAckNo")        private String originalReturnAckNo;
        @JsonProperty("OriginalReturnFiledDate")    private String originalReturnFiledDate;
        @JsonProperty("IsDefective")                private String isDefective;
        @JsonProperty("DefectiveReturnNoticeNum")   private String defectiveReturnNoticeNum;
        @JsonProperty("NoticeNum")                  private String noticeNum;
        @JsonProperty("DIN")                        private String din;
        @JsonProperty("NoticeDate")                 private String noticeDate;
        @JsonProperty("TaxRegime")                  private String taxRegime;
        @JsonProperty("OptOutNewTaxRegime")         private String optOutNewTaxRegime;
        @JsonProperty("Form10IEAFiled")             private String form10IEAFiled;
        @JsonProperty("Form10IEAAckNo")             private String form10IEAAckNo;
        @JsonProperty("ModeOfFiling")              private String modeOfFiling;
        @JsonProperty("IsPortugueseCivilCode5A")   private String isPortugueseCivilCode5A;
    }
}
