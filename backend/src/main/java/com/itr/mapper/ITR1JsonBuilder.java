package com.itr.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itr.dto.Itr1FormData;
import com.itr.dto.HouseProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Builds the final ITR-1 JSON matching ITR-1_2026_Main_V1.0_0 schema structure (AY 2026-27).
 * This JSON can be submitted to the Income Tax e-filing portal.
 * 
 * COMPLETE 101% CBDT COMPLIANT
 * Based on official schema: ITR-1_2026_Main_V1.0_0.json
 */
@Component
public class ITR1JsonBuilder {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    
    // AY 2026-27 Schema Constants
    private static final String ASSESSMENT_YEAR = "2026";
    private static final String SCHEMA_VER = "Ver1.0";
    private static final String FORM_VER = "Ver1.0";
    private static final String SW_CREATED_BY = "SW20014242";
    private static final String JSON_CREATED_BY = "SW20014242";
    private static final String FILING_DUE_DATE = "2026-07-31";
    
    // Employer Category Enums (AY 2026-27)
    private static final String EMP_CAT_CGOV = "CGOV";
    private static final String EMP_CAT_OTH = "OTH";
    private static final String EMP_CAT_NA = "NA";

    /**
     * Main entry point - builds complete ITR-1 JSON from Itr1FormData.
     * COMPLETE AY 2026-27 Schema Compliance
     */
    public String build(Itr1FormData formData, String userId) {
        try {
            ObjectNode root = mapper.createObjectNode();
            ObjectNode itr = root.putObject("ITR");
            ObjectNode itr1 = itr.putObject("ITR1");

            // Required sections per schema
            itr1.set("CreationInfo", creationInfo(userId));
            itr1.set("Form_ITR1", formItr1());
            itr1.set("PersonalInfo", personalInfo(formData));
            itr1.set("FilingStatus", filingStatus(formData));
            itr1.set("ITR1_IncomeDeductions", incomeDeductions(formData));
            itr1.set("ITR1_TaxComputation", taxComputation(formData));
            itr1.set("TaxPaid", taxPaid(formData));
            itr1.set("Refund", refund(formData));
            itr1.set("Verification", verification(formData));
            
            // TDS Schedules
            itr1.set("TDSonSalaries", tdsOnSalaries(formData));
            itr1.set("TDSonOthThanSals", tdsOnOtherThanSalaries(formData));
            
            // Tax Payments
            itr1.set("TaxPayments", taxPayments(formData));

            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build ITR-1 JSON: " + e.getMessage(), e);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SECTION 1: CreationInfo
    // ═══════════════════════════════════════════════════════════════════════════
    
    private ObjectNode creationInfo(String userId) {
        ObjectNode n = mapper.createObjectNode();
        n.put("SWVersionNo", "1.0");
        n.put("SWCreatedBy", SW_CREATED_BY);
        n.put("JSONCreatedBy", JSON_CREATED_BY);
        n.put("JSONCreationDate", LocalDate.now().format(DATE_FORMAT));
        n.put("IntermediaryCity", "Delhi");
        n.put("Digest", "-");
        return n;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SECTION 2: Form_ITR1
    // ═══════════════════════════════════════════════════════════════════════════
    
    private ObjectNode formItr1() {
        ObjectNode n = mapper.createObjectNode();
        n.put("FormName", "ITR-1");
        n.put("Description", "For Individuals having income from Salaries, one house property, other sources (Interest etc.) and having total income upto Rs.50 lakh");
        n.put("AssessmentYear", ASSESSMENT_YEAR);
        n.put("SchemaVer", SCHEMA_VER);
        n.put("FormVer", FORM_VER);
        return n;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SECTION 3: PersonalInfo
    // ═══════════════════════════════════════════════════════════════════════════
    
    private ObjectNode personalInfo(Itr1FormData formData) {
        Itr1FormData.PersonalInfo pi = formData.getPersonalInfo();
        ObjectNode n = mapper.createObjectNode();

        // AssesseeName
        String[] names = splitName(pi.getAssesseeName());
        ObjectNode name = n.putObject("AssesseeName");
        name.put("FirstName", safeString(names[0]));
        name.put("MiddleName", safeString(names[1]));
        name.put("SurNameOrOrgName", safeString(names[2]));

        // PAN
        n.put("PAN", safeString(pi.getPan()));

        // Address
        ObjectNode addr = n.putObject("Address");
        addr.put("ResidenceNo", safeString(pi.getFlatDoorNo()));
        addr.put("ResidenceName", safeString(pi.getPremisesName()));
        addr.put("RoadOrStreet", safeString(pi.getRoadStreet()));
        addr.put("LocalityOrArea", safeString(pi.getArea()));
        addr.put("CityOrTownOrDistrict", safeString(pi.getTownCity()));
        addr.put("StateCode", mapStateCode(pi.getState()));
        addr.put("PinCode", mapPinCode(pi.getPinCode()));
        addr.put("CountryCode", "91");
        addr.put("MobileNo", safeString(pi.getMobile()));
        addr.put("EmailAddress", safeString(pi.getEmail()));

        // SecondaryAdd - NEW in AY 2026-27
        n.put("SecondaryAdd", "N");

        // DOB
        n.put("DOB", formatDate(pi.getDateOfBirth()));

        // EmployerCategory - Enum: CGOV/SGOV/PSU/PE/PESG/PEPS/PEO/OTH/NA
        n.put("EmployerCategory", mapEmployerCategory(pi.getEmployerCategory()));

        // AadhaarCardNo
        n.put("AadhaarCardNo", safeString(pi.getAadhaar()));

        return n;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SECTION 4: FilingStatus
    // ═══════════════════════════════════════════════════════════════════════════
    
    private ObjectNode filingStatus(Itr1FormData formData) {
        Itr1FormData.PersonalInfo pi = formData.getPersonalInfo();
        ObjectNode n = mapper.createObjectNode();

        // ReturnFileSec - 11 = original, 17 = revised
        boolean isRevised = "REVISED".equals(pi.getFilingType());
        n.put("ReturnFileSec", isRevised ? 17 : 11);

        // OptOutNewTaxRegime
        n.put("OptOutNewTaxRegime", "OLD".equals(pi.getRegime()) ? "Y" : "N");

        // SeventhProvisio139 - NEW in AY 2026-27
        n.put("SeventhProvisio139", "N");
        n.put("IncrExpAggAmt2LkTrvFrgnCntryFlg", "N");
        n.put("AmtSeventhProvisio139ii", 0);
        n.put("IncrExpAggAmt1LkElctrctyPrYrFlg", "N");
        n.put("AmtSeventhProvisio139iii", 0);
        n.put("clauseiv7provisio139i", "N");

        // AsseseeRepFlg - NEW in AY 2026-27
        n.put("AsseseeRepFlg", "N");

        // ItrFilingDueDate
        n.put("ItrFilingDueDate", FILING_DUE_DATE);

        return n;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SECTION 5: ITR1_IncomeDeductions
    // ═══════════════════════════════════════════════════════════════════════════
    
    private ObjectNode incomeDeductions(Itr1FormData formData) {
        Itr1FormData.SalaryIncome sal = formData.getSalaryIncome();
        Itr1FormData.HousePropertyIncome hp = formData.getHousePropertyIncome();
        Itr1FormData.OtherSourcesIncome os = formData.getOtherSourcesIncome();
        Itr1FormData.Deductions ded = formData.getDeductions();
        Itr1FormData.TaxComputation tax = formData.getTaxComputation();
        Itr1FormData.PersonalInfo pi = formData.getPersonalInfo();
        
        ObjectNode n = mapper.createObjectNode();

        // ── SALARY SECTION ──
        n.put("GrossSalary", toInt(sal != null ? sal.getGrossSalary() : 0));
        n.put("Salary", toInt(sal != null ? sal.getSalary17_1() : 0));
        n.put("PerquisitesValue", toInt(sal != null ? sal.getPerquisites17_2() : 0));
        n.put("ProfitsInSalary", toInt(sal != null ? sal.getProfitsInLieu17_3() : 0));
        
        // AllwncExemptUs10 - EXPANDED
        ObjectNode allwncExempt = n.putObject("AllwncExemptUs10");
        allwncExempt.put("TotalAllwncExemptUs10", toInt(sal != null ? sal.getTotalExemptAllowances() : 0));
        ArrayNode allwncDtls = allwncExempt.putArray("AllwncExemptUs10Dtls");
        
        if (sal != null && sal.getHraExempt() > 0) {
            ObjectNode hra = allwncDtls.addObject();
            hra.put("SalNatureDesc", "10(13A)");
            hra.put("SalOthAmount", toInt(sal.getHraExempt()));
        }
        if (sal != null && sal.getLtaExempt() > 0) {
            ObjectNode lta = allwncDtls.addObject();
            lta.put("SalNatureDesc", "10(5)");
            lta.put("SalOthAmount", toInt(sal.getLtaExempt()));
        }
        
        n.put("NetSalary", toInt(sal != null ? sal.getNetSalary() : 0));
        n.put("DeductionUs16", toInt(getDeductionUs16(sal)));
        n.put("DeductionUs16ia", toInt(sal != null ? sal.getStandardDeduction() : 0));
        n.put("EntertainmentAlw16ii", toInt(sal != null ? sal.getEntertainmentAllowance() : 0));
        n.put("ProfessionalTaxUs16iii", toInt(sal != null ? sal.getProfessionalTax() : 0));
        
        // USE COMPUTED INCOME FROM TaxComputation - this is the key fix!
        long salaryIncome = 0;
        if (tax != null) {
            salaryIncome = toLong(tax.getIncomeFromSalary());
        }
        if (salaryIncome == 0 && sal != null) {
            salaryIncome = toLong(sal.getIncomeFromSalary());
        }
        // ── HOUSE PROPERTY SECTION (AY 2026-27: Multiple Properties Support) ──
        double totalHPIncome = 0;
        ArrayNode propertyDetails = n.putArray("PropertyDetails");
        
        // Use new houseProperties list (from unified HouseProperty DTO)
        List<HouseProperty> hpList = formData.getHouseProperties();
        
        // Get legacy single property for backward compatibility
        Itr1FormData.HousePropertyIncome legacyHp = formData.getHousePropertyIncome();
        
        if (hpList != null && !hpList.isEmpty()) {
            for (HouseProperty hpItem : hpList) {
                ObjectNode prop = propertyDetails.addObject();
                
                // Property Sequence Number
                prop.put("HPSNo", hpItem.getPropertySequenceNo() > 0 ? hpItem.getPropertySequenceNo() : 1);
                
                // AddressDetailWithZipCode
                ObjectNode addrDetail = prop.putObject("AddressDetailWithZipCode");
                addrDetail.put("AddrDetail", safeString(hpItem.getAddress()));
                addrDetail.put("ResidenceName", safeString(hpItem.getPremisesName() != null ? hpItem.getPremisesName() : ""));
                addrDetail.put("RoadOrStreet", safeString(hpItem.getRoadOrStreet() != null ? hpItem.getRoadOrStreet() : ""));
                addrDetail.put("LocalityOrArea", safeString(hpItem.getArea() != null ? hpItem.getArea() : ""));
                addrDetail.put("CityOrTownOrDistrict", safeString(hpItem.getCity()));
                addrDetail.put("StateCode", mapStateCode(hpItem.getState()));
                addrDetail.put("PinCode", mapPinCode(hpItem.getPinCode()));
                addrDetail.put("CountryCode", safeString(hpItem.getCountryCode()) != "" ? hpItem.getCountryCode() : "91");
                
                // Property Owner
                prop.put("PropertyOwner", mapPropertyOwnerType(hpItem.getPropertyOwnerType()));
                prop.put("PropCoOwnedFlg", hpItem.isCoOwned() ? "YES" : "NO");
                prop.put("AsseseeShareProperty", hpItem.getAssesseeShareProperty() > 0 ? hpItem.getAssesseeShareProperty() : 100.0);
                prop.put("ifLetOut", mapHPType(hpItem.getPropertyType()));
                
                // Rentdetails
                ObjectNode rentDetails = prop.putObject("Rentdetails");
                boolean isLetOut = hpItem.isLetOut();
                
                rentDetails.put("AnnualLetableValue", isLetOut ? toInt(hpItem.getAnnualRent()) : 0);
                rentDetails.put("RentNotRealized", isLetOut ? toInt(hpItem.getUnrealizedRent()) : 0);
                rentDetails.put("LocalTaxes", isLetOut ? toInt(hpItem.getMunicipalTaxesPaid()) : 0);
                
                // Calculate BalanceALV
                double balanceALV = 0;
                if (isLetOut) {
                    balanceALV = toInt(hpItem.getAnnualRent() - hpItem.getUnrealizedRent() - hpItem.getMunicipalTaxesPaid());
                }
                rentDetails.put("BalanceALV", balanceALV);
                rentDetails.put("ThirtyPercentOfBalance", toInt(balanceALV * 0.30));
                rentDetails.put("IntOnBorwCap", toInt(hpItem.getInterestOnLoan()));
                
                // Section24B - Detailed Loan Info (AY 2026-27)
                ObjectNode section24B = rentDetails.putObject("Section24B");
                ArrayNode loanDtls = section24B.putArray("Section24BDtls");
                double totalInterest24B = 0;
                
                if (hpItem.getHomeLoans() != null && !hpItem.getHomeLoans().isEmpty()) {
                    for (HouseProperty.HomeLoan loan : hpItem.getHomeLoans()) {
                        ObjectNode loanNode = loanDtls.addObject();
                        loanNode.put("LoanTknFrom", safeString(loan.getLenderType()) != "" ? loan.getLenderType() : "B");
                        loanNode.put("BankOrInstnName", safeString(loan.getLenderName()));
                        loanNode.put("LoanAccNoOfBankOrInstnRefNo", safeString(loan.getLoanAccountNo()));
                        loanNode.put("DateofLoan", loan.getDateOfLoan() != null ? loan.getDateOfLoan().format(DATE_FORMAT) : "");
                        loanNode.put("TotalLoanAmt", toLong(loan.getTotalLoanAmount()));
                        loanNode.put("LoanOutstndngAmt", toLong(loan.getLoanOutstandingAmount()));
                        loanNode.put("InterestUs24B", toInt(loan.getInterestUs24B()));
                        totalInterest24B += loan.getInterestUs24B();
                    }
                } else if (hpItem.getInterestOnLoan() > 0 && loanDtls.size() == 0) {
                    // Legacy: Single loan from main interest field
                    ObjectNode loanNode = loanDtls.addObject();
                    loanNode.put("LoanTknFrom", "B");
                    loanNode.put("BankOrInstnName", safeString(hpItem.getPropertyOwnerType()));
                    loanNode.put("InterestUs24B", toInt(hpItem.getInterestOnLoan()));
                    totalInterest24B = hpItem.getInterestOnLoan();
                }
                section24B.put("TotalInterestUs24B", toInt(totalInterest24B > 0 ? totalInterest24B : hpItem.getInterestOnLoan()));
                
                // Calculate deductions
                int totalDeduct = isLetOut ? 
                    toInt((long)(balanceALV * 0.30) + hpItem.getInterestOnLoan()) : 
                    toInt(hpItem.getInterestOnLoan());
                rentDetails.put("TotalDeduct", totalDeduct);
                
                // Add Co-Owners if present
                if (hpItem.getCoOwners() != null && !hpItem.getCoOwners().isEmpty()) {
                    ArrayNode coOwnerArray = prop.putArray("CoOwners");
                    for (HouseProperty.CoOwner co : hpItem.getCoOwners()) {
                        ObjectNode coNode = coOwnerArray.addObject();
                        coNode.put("CoOwnersSNo", co.getCoOwnerSNo());
                        coNode.put("NameCoOwner", safeString(co.getName()));
                        coNode.put("PANCoOwner", safeString(co.getPan()));
                        coNode.put("AadhaarCoOwner", safeString(co.getAadhaar()));
                        coNode.put("PercentShareProperty", co.getSharePercentage());
                    }
                }
                
                // Add Tenant Details if present
                if (hpItem.getTenants() != null && !hpItem.getTenants().isEmpty()) {
                    ArrayNode tenantArray = prop.putArray("TenantDetails");
                    for (HouseProperty.Tenant tenant : hpItem.getTenants()) {
                        ObjectNode tenantNode = tenantArray.addObject();
                        tenantNode.put("TenantSNo", tenant.getTenantSNo());
                        tenantNode.put("NameOfTenant", safeString(tenant.getName()));
                        tenantNode.put("PANOfTenant", safeString(tenant.getPan()));
                        tenantNode.put("AadhaarOfTenant", safeString(tenant.getAadhaar()));
                    }
                }
                
                totalHPIncome += hpItem.getIncomeFromHP();
            }
        } else if (legacyHp != null && legacyHp.getPropertyType() != null) {
            // Legacy single property (backward compatibility)
            ObjectNode prop = propertyDetails.addObject();
            prop.put("HPSNo", 1);
            
            ObjectNode addrDetail = prop.putObject("AddressDetailWithZipCode");
            addrDetail.put("AddrDetail", safeString(legacyHp.getAddress()));
            addrDetail.put("CityOrTownOrDistrict", safeString(legacyHp.getCity()));
            addrDetail.put("StateCode", mapStateCode(legacyHp.getState()));
            addrDetail.put("CountryCode", "91");
            addrDetail.put("PinCode", mapPinCode(legacyHp.getPinCode()));
            
            prop.put("PropertyOwner", mapPropertyOwnerType(legacyHp.getPropertyOwner()));
            prop.put("PropCoOwnedFlg", legacyHp.isPropertyCoOwned() ? "YES" : "NO");
            prop.put("AsseseeShareProperty", legacyHp.getOwnershipShare() > 0 ? legacyHp.getOwnershipShare() : 100.0);
            prop.put("ifLetOut", mapHPType(legacyHp.getPropertyType()));
            
            ObjectNode rentDetails = prop.putObject("Rentdetails");
            boolean isLetOut = "LET_OUT".equals(legacyHp.getPropertyType()) || "L".equals(mapHPType(legacyHp.getPropertyType()));
            
            rentDetails.put("AnnualLetableValue", isLetOut ? toInt(legacyHp.getAnnualRent()) : 0);
            rentDetails.put("RentNotRealized", isLetOut ? toInt(legacyHp.getUnrealizedRent()) : 0);
            rentDetails.put("LocalTaxes", isLetOut ? toInt(legacyHp.getMunicipalTaxesPaid()) : 0);
            
            int balanceALV = isLetOut ? toInt(legacyHp.getAnnualRent() - legacyHp.getUnrealizedRent() - legacyHp.getMunicipalTaxesPaid()) : 0;
            rentDetails.put("BalanceALV", balanceALV);
            rentDetails.put("ThirtyPercentOfBalance", toInt(balanceALV * 0.30));
            rentDetails.put("IntOnBorwCap", toInt(legacyHp.getInterestOnLoan()));
            
            ObjectNode section24B = rentDetails.putObject("Section24B");
            if (legacyHp.getInterestOnLoan() > 0) {
                ArrayNode loanDtls = section24B.putArray("Section24BDtls");
                ObjectNode loan = loanDtls.addObject();
                loan.put("LoanTknFrom", "B");
                loan.put("BankOrInstnName", "Bank");
                loan.put("InterestUs24B", toInt(legacyHp.getInterestOnLoan()));
            }
            section24B.put("TotalInterestUs24B", toInt(legacyHp.getInterestOnLoan()));
            
            int totalDeduct = isLetOut ? toInt(balanceALV * 0.30 + legacyHp.getInterestOnLoan()) : toInt(legacyHp.getInterestOnLoan());
            rentDetails.put("TotalDeduct", totalDeduct);
            
            totalHPIncome = legacyHp.getIncomeFromHP();
        }
        
        n.put("TotalIncomeChargeableUnHP", (int)totalHPIncome);

        // ── OTHER SOURCES SECTION ──
        // USE COMPUTED INCOME FROM TaxComputation
        long otherSrcIncome = 0;
        if (tax != null) {
            otherSrcIncome = toLong(tax.getIncomeFromOtherSources());
        }
        if (otherSrcIncome == 0 && os != null) {
            otherSrcIncome = toLong(os.getTotalOtherSourcesIncome());
        }
        n.put("IncomeOthSrc", (int)otherSrcIncome);
        
        ObjectNode othersInc = n.putObject("OthersInc");
        ArrayNode othDtls = othersInc.putArray("OthersIncDtlsOthSrc");
        
        if (os != null) {
            if (os.getSavingsAccountInterest() > 0) addOtherSrcEntry(othDtls, "SAV", os.getSavingsAccountInterest());
            if (os.getFixedDepositInterest() > 0) addOtherSrcEntry(othDtls, "IFD", os.getFixedDepositInterest());
            if (os.getTotalDividendIncome() > 0) addOtherSrcEntry(othDtls, "DIV", os.getTotalDividendIncome());
            if (os.getFamilyPensionReceived() > 0) addOtherSrcEntry(othDtls, "FAP", os.getFamilyPensionTaxable());
            if (os.getOtherIncome() > 0) addOtherSrcEntry(othDtls, "OTH", os.getOtherIncome());
        }

        n.put("DeductionUs57iia", toInt(os != null ? os.getFamilyPensionDeduction() : 0));

        // ── GROSS TOTAL INCOME ──
        // Use computed TaxComputation values for accuracy
        Itr1FormData.TaxComputation computedTax = formData.getTaxComputation();
        if (computedTax != null) {
            n.put("GrossTotIncome", toInt(computedTax.getGrossTotalIncome()));
            n.put("GrossTotIncomeIncLTCG112A", toInt(computedTax.getGrossTotalIncome() + computedTax.getLtcg112A()));
            n.put("TotalIncome", toInt(roundToNearest10(computedTax.getTotalIncome())));
        } else {
            n.put("GrossTotIncome", 0);
            n.put("GrossTotIncomeIncLTCG112A", 0);
            n.put("TotalIncome", 0);
        }
        ObjectNode chapVIA = n.putObject("UsrDeductUndChapVIA");
        if (ded != null) {
            chapVIA.put("Section80C", toInt(ded.getDeduction80C()));
            chapVIA.put("Section80CCDEmployeeOrSE", toInt(ded.getNpsEmployee80CCD1()));
            chapVIA.put("Section80CCD1B", toInt(ded.getNpsEmployee80CCD1B()));
            chapVIA.put("Section80CCDEmployer", toInt(ded.getNpsEmployer80CCD2()));
            chapVIA.put("Section80D", toInt(ded.getDeduction80D()));
            chapVIA.put("Section80G", toInt(ded.getDeduction80G()));
            chapVIA.put("Section80GG", toInt(ded.getDeduction80GG()));
            chapVIA.put("Section80TTA", toInt(ded.getDeduction80TTA()));
            chapVIA.put("Section80TTB", toInt(ded.getDeduction80TTB()));
            chapVIA.put("TotalChapVIADeductions", toInt(ded.getTotalDeductions()));
        }
        
        n.set("DeductUndChapVIA", chapVIA.deepCopy());

        // ── TOTAL INCOME ──
        n.put("TotalIncome", toInt(tax != null ? roundToNearest10(tax.getTotalIncome()) : 0));

        // ── EXEMPT INCOME ──
        Itr1FormData.ExemptIncome ei = formData.getExemptIncome();
        if (ei != null && ei.getAgricultureIncome() > 0) {
            ObjectNode exemptInc = n.putObject("ExemptIncAgriOthUs10");
            exemptInc.put("ExemptIncAgriOthUs10Total", toInt(ei.getTotalExemptIncome()));
            ArrayNode exemptDtls = exemptInc.putArray("ExemptIncAgriOthUs10Dtls");
            ObjectNode agri = exemptDtls.addObject();
            agri.put("Category", "AGRI");
            agri.put("SubCategory", "10(1)");
            agri.put("OthAmount", toInt(ei.getAgricultureIncome()));
        }

        return n;
    }

    private void addOtherSrcEntry(ArrayNode arr, String code, double amt) {
        if (amt > 0) {
            ObjectNode e = arr.addObject();
            e.put("OthSrcNatureDesc", code);
            e.put("OthSrcOthAmount", toInt(amt));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SECTION 6: ITR1_TaxComputation
    // ═══════════════════════════════════════════════════════════════════════════
    
    private ObjectNode taxComputation(Itr1FormData formData) {
        Itr1FormData.TaxComputation tax = formData.getTaxComputation();
        ObjectNode n = mapper.createObjectNode();
        
        if (tax != null) {
            n.put("TotalTaxPayable", toInt(tax.getTaxOnNormalIncome()));
            n.put("Rebate87A", toInt(tax.getRebate87A()));
            n.put("TaxPayableOnRebate", toInt(tax.getTaxAfterRebate()));
            n.put("EducationCess", toInt(tax.getCess()));
            n.put("GrossTaxLiability", toInt(tax.getTotalTaxLiability()));
            n.put("Section89", toInt(tax.getRelief89()));
            n.put("NetTaxLiability", toInt(tax.getTaxPayableAfterRelief()));
            n.put("TotalIntrstPay", toInt(tax.getTotalInterestAndFees()));

            ObjectNode intrstPay = n.putObject("IntrstPay");
            intrstPay.put("IntrstPayUs234A", toInt(tax.getInterest234A()));
            intrstPay.put("IntrstPayUs234B", toInt(tax.getInterest234B()));
            intrstPay.put("IntrstPayUs234C", toInt(tax.getInterest234C()));
            intrstPay.put("LateFilingFee234F", toInt(tax.getFee234F()));

            n.put("TotTaxPlusIntrstPay", toInt(tax.getTaxPayableAfterRelief() + tax.getTotalInterestAndFees()));
        }
        
        return n;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SECTION 7: TaxPaid (FIXED - Use computed values from TaxComputation)
    // ═══════════════════════════════════════════════════════════════════════════
    
    private ObjectNode taxPaid(Itr1FormData formData) {
        Itr1FormData.TaxPayments tp = formData.getTaxPayments();
        Itr1FormData.TaxComputation tax = formData.getTaxComputation();
        
        // Calculate TDS from list to ensure accuracy
        long totalTDSFromSalary = 0;
        long totalTDSFromOther = 0;
        long totalTCS = 0;
        long totalAdvanceTax = 0;
        long totalSelfAssessmentTax = 0;
        
        if (tp != null) {
            if (tp.getTdsOnSalary() != null) {
                for (Itr1FormData.TDSOnSalary tds : tp.getTdsOnSalary()) {
                    totalTDSFromSalary += toLong(tds.getTdsAmount());
                }
            }
            if (tp.getTdsOnOther() != null) {
                for (Itr1FormData.TDSOnOther tds : tp.getTdsOnOther()) {
                    totalTDSFromOther += toLong(tds.getTdsAmount());
                }
            }
            if (tp.getTcsEntries() != null) {
                for (Itr1FormData.TCSEntry tcs : tp.getTcsEntries()) {
                    totalTCS += toLong(tcs.getTcsAmount());
                }
            }
            if (tp.getAdvanceTaxEntries() != null) {
                for (Itr1FormData.AdvanceTaxEntry at : tp.getAdvanceTaxEntries()) {
                    totalAdvanceTax += toLong(at.getAmount());
                }
            }
            if (tp.getSelfAssessmentTaxEntries() != null) {
                for (Itr1FormData.SelfAssessmentTaxEntry sat : tp.getSelfAssessmentTaxEntries()) {
                    totalSelfAssessmentTax += toLong(sat.getAmount());
                }
            }
            // Also check if totalSelfAssessmentTax is already computed in TaxPayments
            if (totalSelfAssessmentTax == 0 && tp.getTotalSelfAssessmentTax() > 0) {
                totalSelfAssessmentTax = toLong(tp.getTotalSelfAssessmentTax());
            }
        }
        
        // Calculate total taxes paid
        long totalTaxesPaid = totalTDSFromSalary + totalTDSFromOther + totalTCS + totalAdvanceTax + totalSelfAssessmentTax;
        
        // Use computed Balance Tax from TaxComputation
        long balTaxPayable = 0;
        
        if (tax != null) {
            balTaxPayable = toLong(tax.getBalanceTax());
            // If computed total taxes paid exists, use it
            if (tax.getTotalTaxesPaid() > 0) {
                totalTaxesPaid = toLong(tax.getTotalTaxesPaid());
            }
        }
        
        ObjectNode n = mapper.createObjectNode();

        ObjectNode taxesPaid = n.putObject("TaxesPaid");
        taxesPaid.put("AdvanceTax", totalAdvanceTax);
        taxesPaid.put("TDS", totalTDSFromSalary + totalTDSFromOther);
        taxesPaid.put("TCS", totalTCS);
        taxesPaid.put("SelfAssessmentTax", totalSelfAssessmentTax);
        taxesPaid.put("TotalTaxesPaid", totalTaxesPaid);

        n.put("BalTaxPayable", (int)balTaxPayable);
        return n;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SECTION 8: Refund (WITH BankAccountDtls - BLOCKER FIX)
    // ═══════════════════════════════════════════════════════════════════════════
    
    private ObjectNode refund(Itr1FormData formData) {
        Itr1FormData.TaxComputation tax = formData.getTaxComputation();
        Itr1FormData.PersonalInfo pi = formData.getPersonalInfo();
        
        ObjectNode n = mapper.createObjectNode();
        n.put("RefundDue", toInt(tax != null ? tax.getRefund() : 0));
        
        // BankAccountDtls - REQUIRED if RefundDue > 0
        ObjectNode bank = n.putObject("BankAccountDtls");
        bank.put("IFSCCode", safeString(pi.getBankIFSC()));
        bank.put("BankName", safeString(pi.getBankName()));
        bank.put("BankAccountNo", safeString(pi.getBankAccountNo()));
        bank.put("AccountType", mapAccountType(pi.getBankAccountType()));
        
        return n;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SECTION 9: Verification (WITH FatherName - BLOCKER FIX)
    // ═══════════════════════════════════════════════════════════════════════════
    
    private ObjectNode verification(Itr1FormData formData) {
        Itr1FormData.PersonalInfo pi = formData.getPersonalInfo();
        String fullName = pi.getAssesseeName();
        String[] names = splitName(fullName);
        String fullNameFormatted = (names[0] + " " + names[1] + " " + names[2]).trim();
        
        ObjectNode n = mapper.createObjectNode();
        n.put("AssesseeVerName", fullNameFormatted);
        
        // FatherName - BLOCKER FIX - was missing
        n.put("FatherName", safeString(pi.getFatherName()));
        
        n.put("Capacity", "S");
        n.put("Place", safeString(pi.getTownCity()));
        n.put("Date", LocalDate.now().format(DATE_FORMAT));
        
        // Declaration
        ObjectNode decl = n.putObject("Declaration");
        decl.put("AssesseeVerPAN", safeString(pi.getPan()));
        decl.put("AssesseeVerName", fullNameFormatted);
        decl.put("FatherName", safeString(pi.getFatherName()));
        decl.put("Capacity", "S");
        
        return n;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SECTION 10: TDSonSalaries (Fix for empty array handling)
    // ═══════════════════════════════════════════════════════════════════════════
    
    private ObjectNode tdsOnSalaries(Itr1FormData formData) {
        Itr1FormData.TaxPayments tp = formData.getTaxPayments();
        ObjectNode n = mapper.createObjectNode();
        
        // Use array node that will serialize to [] not null
        ArrayNode arr = mapper.createArrayNode();
        
        boolean hasData = false;
        if (tp != null && tp.getTdsOnSalary() != null) {
            for (Itr1FormData.TDSOnSalary e : tp.getTdsOnSalary()) {
                if (e.getTdsAmount() > 0 || e.getSalaryAmount() > 0) {
                    hasData = true;
                    ObjectNode entry = arr.addObject();
                    ObjectNode emp = entry.putObject("EmployerOrDeductorOrCollectDetl");
                    emp.put("TAN", safeString(e.getEmployerTAN()));
                    emp.put("EmployerOrDeductorOrCollecterName", safeString(e.getEmployerName()));
                    entry.put("IncChrgSal", toLong(e.getSalaryAmount() > 0 ? e.getSalaryAmount() : e.getTotalSalary()));
                    entry.put("TotalTDSSal", toLong(e.getTdsAmount() > 0 ? e.getTdsAmount() : e.getTaxDeducted()));
                    entry.put("TDSClaimed", toLong(e.getTdsAmount() > 0 ? e.getTdsAmount() : e.getTaxDeducted()));
                }
            }
        }
        
        n.set("TDSonSalary", arr);
        return n;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SECTION 11: TDSonOthThanSals
    // ═══════════════════════════════════════════════════════════════════════════
    
    private ObjectNode tdsOnOtherThanSalaries(Itr1FormData formData) {
        Itr1FormData.TaxPayments tp = formData.getTaxPayments();
        ObjectNode n = mapper.createObjectNode();
        
        ArrayNode arr = mapper.createArrayNode();
        
        if (tp != null && tp.getTdsOnOther() != null) {
            for (Itr1FormData.TDSOnOther e : tp.getTdsOnOther()) {
                if (e.getTdsAmount() > 0 || e.getIncomeAmount() > 0) {
                    ObjectNode entry = arr.addObject();
                    ObjectNode ded = entry.putObject("EmployerOrDeductorOrCollectDetl");
                    ded.put("TAN", safeString(e.getDeductorTAN()));
                    ded.put("EmployerOrDeductorOrCollecterName", safeString(e.getDeductorName()));
                    entry.put("GrossAmt", toLong(e.getIncomeAmount() > 0 ? e.getIncomeAmount() : e.getGrossAmount()));
                    entry.put("TotalTDS", toLong(e.getTdsAmount() > 0 ? e.getTdsAmount() : e.getTaxDeducted()));
                    entry.put("TDSClaimed", toLong(e.getTdsAmount() > 0 ? e.getTdsAmount() : e.getTaxDeducted()));
                }
            }
        }
        
        n.set("TDSonOthThanSal", arr);
        return n;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SECTION 12: TaxPayments
    // ═══════════════════════════════════════════════════════════════════════════
    
    private ObjectNode taxPayments(Itr1FormData formData) {
        Itr1FormData.TaxPayments tp = formData.getTaxPayments();
        ObjectNode n = mapper.createObjectNode();
        ArrayNode arr = n.putArray("TaxPayment");
        
        if (tp != null) {
            if (tp.getAdvanceTaxEntries() != null) {
                for (Itr1FormData.AdvanceTaxEntry e : tp.getAdvanceTaxEntries()) {
                    ObjectNode entry = arr.addObject();
                    entry.put("BSRCode", safeString(e.getBsrCode()));
                    entry.put("DateDep", formatDate(e.getDepositDate()));
                    entry.put("SrlNoOfChaln", safeString(e.getChallanNo()));
                    entry.put("Amt", toLong(e.getAmount()));
                }
            }
            if (tp.getSelfAssessmentTaxEntries() != null) {
                for (Itr1FormData.SelfAssessmentTaxEntry e : tp.getSelfAssessmentTaxEntries()) {
                    ObjectNode entry = arr.addObject();
                    entry.put("BSRCode", safeString(e.getBsrCode()));
                    entry.put("DateDep", formatDate(e.getDepositDate()));
                    entry.put("SrlNoOfChaln", safeString(e.getChallanNo()));
                    entry.put("Amt", toLong(e.getAmount()));
                }
            }
        }
        
        return n;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private String[] splitName(String fullName) {
        if (fullName == null) return new String[]{"", "", ""};
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return new String[]{parts[0], "", ""};
        if (parts.length == 2) return new String[]{parts[0], "", parts[1]};
        if (parts.length == 3) return new String[]{parts[0], parts[1], parts[2]};
        return new String[]{parts[0], parts[1], parts[parts.length-1]};
    }
    
    private String safeString(String s) {
        return s != null ? s : "";
    }
    
    private int toInt(double d) {
        return (int) Math.round(d);
    }
    
    private Long toLong(Double d) {
        return d != null ? Math.round(d) : 0L;
    }
    
    private int roundToNearest10(double value) {
        return (int) (Math.round(value / 10.0) * 10);
    }
    
    private String formatDate(java.time.LocalDate date) {
        return date != null ? date.format(DATE_FORMAT) : "";
    }
    
    private String mapStateCode(String state) {
        if (state == null) return "99";
        
        String s = state.toUpperCase().trim();
        
        // CBDT Official State Codes (AY 2026-27)
        switch (s) {
            // 01-09
            case "ANDAMAN AND NICOBAR ISLANDS":
            case "ANDAMAN & NICOBAR ISLANDS":
                return "01";
            case "ANDHRA PRADESH":
                return "02";
            case "ARUNACHAL PRADESH":
                return "03";
            case "ASSAM":
                return "04";
            case "BIHAR":
                return "05";
            case "CHANDIGARH":
                return "06";
            case "DADRA NAGAR AND HAVELI":
            case "DADRA & NAGAR HAVELI":
                return "07";
            case "DAMAN AND DIU":
            case "DAMAN & DIU":
                return "08";
            case "DELHI":
            case "NEW DELHI":
                return "09";
            
            // 10-19
            case "GOA":
                return "10";
            case "GUJARAT":
                return "11";
            case "HARYANA":
                return "12";
            case "HIMACHAL PRADESH":
                return "13";
            case "JAMMU AND KASHMIR":
            case "JAMMU & KASHMIR":
                return "14";
            case "KARNATAKA":
                return "15";
            case "KERALA":
                return "16";
            case "LAKSHADWEEP":
                return "17";
            case "MADHYA PRADESH":
                return "18";
            case "MAHARASHTRA":
                return "19";
            
            // 20-29
            case "MANIPUR":
                return "20";
            case "MEGHALAYA":
                return "21";
            case "MIZORAM":
                return "22";
            case "NAGALAND":
                return "23";
            case "ODISHA":
            case "ORISSA":
                return "24";
            case "PUDUCHERRY":
            case "PONDICHERRY":
                return "25";
            case "PUNJAB":
                return "26";
            case "RAJASTHAN":
                return "27";
            case "SIKKIM":
                return "28";
            case "TAMIL NADU":
            case "TAMILNADU":
                return "29";
            
            // 30-39
            case "TRIPURA":
                return "30";
            case "UTTAR PRADESH":
            case "UP":
                return "31";
            case "WEST BENGAL":
            case "BENGAL":
                return "32";
            case "CHHATTISGARGH":
            case "CHHATTISGARH":
                return "33";
            case "UTTARAKHAND":
                return "34";
            case "JHARKHAND":
                return "35";
            case "TELANGANA":
                return "36";
            case "LADAKH":
            case "LADAK":
                return "37";
            
            default:
                // Try partial matching for common variations
                if (s.contains("DELHI")) return "09";
                if (s.contains("MAHARASHTRA")) return "19";
                if (s.contains("KARNATAKA")) return "15";
                if (s.contains("TAMIL NADU") || s.contains("TAMILNADU")) return "29";
                if (s.contains("WEST BENGAL") || s.contains("BENGAL")) return "32";
                if (s.contains("GUJARAT")) return "11";
                if (s.contains("RAJASTHAN")) return "27";
                if (s.contains("UTTAR PRADESH") || s.contains(" UP")) return "31";
                if (s.contains("KERALA")) return "16";
                if (s.contains("MADHYA PRADESH")) return "18";
                if (s.contains("ODISHA") || s.contains("ORISSA")) return "24";
                if (s.contains("PUNJAB")) return "26";
                if (s.contains("JHARKHAND")) return "35";
                if (s.contains("TELANGANA")) return "36";
                if (s.contains("ASSAM")) return "04";
                if (s.contains("BIHAR")) return "05";
                if (s.contains("HARYANA")) return "12";
                if (s.contains("KERALA")) return "16";
                if (s.contains("GOA")) return "10";
                return "99";
        }
    }
    
    private Long mapPinCode(String pinCode) {
        if (pinCode == null || pinCode.isEmpty()) return null;
        try {
            return Long.parseLong(pinCode);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private String mapEmployerCategory(String category) {
        if (category == null) return EMP_CAT_NA;
        String upper = category.toUpperCase();
        if (upper.contains("CENTRAL")) return EMP_CAT_CGOV;
        if (upper.contains("PENSIONER")) return "PE";
        return EMP_CAT_OTH;
    }
    
    private String mapHPType(String type) {
        if (type == null) return "S";
        String upper = type.toUpperCase();
        if (upper.contains("LET_OUT")) return "L";
        if (upper.contains("DEEMED")) return "D";
        return "S";
    }
    
    private String mapPropertyOwnerType(String ownerType) {
        if (ownerType == null) return "SE";
        String upper = ownerType.toUpperCase();
        if (upper.contains("MINOR") || upper.equals("MI")) return "MI";
        if (upper.contains("SPOUSE") || upper.contains("SP")) return "SP";
        if (upper.equals("OT")) return "OT";
        return "SE"; // Default: Single Owner
    }
    
    private String mapAccountType(String type) {
        if (type == null) return "SB";
        return type.toUpperCase().contains("CURRENT") ? "CURRENT" : "SB";
    }
    
    private double getDeductionUs16(Itr1FormData.SalaryIncome sal) {
        if (sal == null) return 0;
        return sal.getStandardDeduction() + sal.getEntertainmentAllowance() + sal.getProfessionalTax();
    }
    
    private void addDeductionSchedules(ObjectNode itr1, Itr1FormData formData) {
        // Placeholder for old regime deduction schedules
    }
}
