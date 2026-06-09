package com.itr.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.itr.dto.*;
import com.itr.util.ITDDateFormatter;
import com.itr.util.SHA256DigestUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.Period;

@Slf4j
@Service
public class ITR2JSONExportService {

    private final ObjectMapper mapper = new ObjectMapper();

    public String export(Itr2FormData data) {
        try {
            ObjectNode root = mapper.createObjectNode();
            ObjectNode itr = mapper.createObjectNode();
            ObjectNode itr2 = mapper.createObjectNode();

            itr2.set("CreationInfo", buildCreationInfo(data));
            itr2.set("Form_ITR2", buildFormITR2(data));

            itr.set("ITR2", itr2);
            root.set("ITR", itr);

            String jsonWithoutDigest = mapper.writeValueAsString(root);
            String digest = SHA256DigestUtil.computeDigest(jsonWithoutDigest);
            ((ObjectNode) root.path("ITR").path("ITR2").path("CreationInfo"))
                .put("Digest", digest);

            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (Exception e) {
            log.error("ITR-2 JSON export failed", e);
            throw new RuntimeException("ITR-2 JSON export failed: " + e.getMessage(), e);
        }
    }

    private ObjectNode buildCreationInfo(Itr2FormData data) {
        ObjectNode ci = mapper.createObjectNode();
        ci.put("SWVersionNo", "1.1");
        ci.put("SWCreatedBy", "ITR_ERP_v1");
        ci.put("JSONCreatedBy", "ITR_ERP_v1");
        ci.put("JSONCreatedDate", ITDDateFormatter.format(LocalDate.now()));
        ci.put("InterfaceType", "JSON");
        String city = data.getIntermediaryCity() != null ? data.getIntermediaryCity() : "SYSTEM";
        ci.put("IntermediaryCity", city.length() > 25 ? city.substring(0, 25) : city);
        ci.put("Digest", "");
        return ci;
    }

    private ObjectNode buildPersonalInfo(Itr2FormData data) {
        ObjectNode pi = mapper.createObjectNode();

        ObjectNode name = mapper.createObjectNode();
        name.put("FirstName", nvl(data.getFirstName()));
        name.put("MiddleName", nvl(data.getMiddleName()));
        name.put("SurNameOrOrgName", nvl(data.getSurName()));
        pi.set("AssesseeName", name);

        pi.put("PAN", data.getPAN());
        pi.put("DOB", ITDDateFormatter.format(data.getDateOfBirth()));
        pi.put("AadhaarCardNo", nvl(data.getAadhaarNumber()));
        pi.put("AadhaarEnrolmentID", nvl(data.getAadhaarEnrolmentId()));
        pi.put("PrimaryMobileNo", nvl(data.getMobileNumber()));
        pi.put("EmailID", nvl(data.getEmail()));

        ObjectNode addr = mapper.createObjectNode();
        addr.put("ResidenceName", nvl(data.getFlatDoorBlock()));
        addr.put("ResidenceNo", nvl(data.getPremisesName()));
        addr.put("RoadOrStreet", nvl(data.getRoadStreet()));
        addr.put("LocalityOrArea", nvl(data.getLocality()));
        addr.put("CityOrTownOrDistrict", nvl(data.getCity()));
        addr.put("StateCode", nvl(data.getStateCode()));
        addr.put("PinCode", nvl(data.getPinCode()));
        addr.put("CountryCode", "91");
        pi.set("Address", addr);

        pi.put("EmployerCategory", nvl(data.getEmployerCategory()));
        pi.put("ResidentialStatus", "RES");
        pi.put("Status", "I");
        pi.put("AssesseeType", computeAssesseeType(data));
        pi.put("SeventhProvisotoSec139i", "N");

        return pi;
    }

    private String computeAssesseeType(Itr2FormData data) {
        if (data.getDateOfBirth() == null) return "05";
        LocalDate refDate = LocalDate.of(
            Integer.parseInt(data.getAssessmentYear().split("-")[0]) - 1, 4, 1);
        int age = Period.between(data.getDateOfBirth(), refDate).getYears();
        if (age >= 80) return "07";
        if (age >= 60) return "06";
        return "05";
    }

    private ObjectNode buildFilingStatus(Itr2FormData data) {
        ObjectNode fs = mapper.createObjectNode();
        fs.put("ReturnFileSec", "11");
        fs.put("ReturnType", "O");
        fs.put("IsRevised", "N");
        fs.put("IsDefective", "N");
        fs.put("TaxRegime", "NEW".equals(data.getTaxRegime()) ? "N" : "O");
        fs.put("OptOutNewTaxRegime", "OLD".equals(data.getTaxRegime()) ? "Y" : "N");
        fs.put("ModeOfFiling", "O");
        if (data.getForm10IEAAckNo() != null) {
            fs.put("Form10IEAFiled", "Y");
            fs.put("Form10IEAAckNo", data.getForm10IEAAckNo());
        } else {
            fs.put("Form10IEAFiled", "N");
        }
        return fs;
    }

    private ObjectNode buildScheduleS(Itr2FormData data) {
        ObjectNode s = mapper.createObjectNode();
        s.put("Salary", safeInt(data.getBasicSalary()));
        s.put("PerquisitesValue", safeInt(data.getPerquisites()));
        s.put("ProfitsInLieuSalary", safeInt(data.getProfitsInLieu()));
        s.put("GrossSalary", (int) data.getGrossSalary());

        ObjectNode allwExempt = mapper.createObjectNode();
        allwExempt.put("Sec10_5_LTA", safeInt(data.getLtaExemption()));
        allwExempt.put("Sec10_10_Gratuity", safeInt(data.getGratuityExemption()));
        allwExempt.put("Sec10_10A_CommutedPension", safeInt(data.getCommutedPensionExemption()));
        allwExempt.put("Sec10_10AA_LeaveEncashment", safeInt(data.getLeaveEncashmentExemption()));
        allwExempt.put("Sec10_13A_HRA", safeInt(data.getHraExemption()));
        allwExempt.put("TotalExemptAllowances", safeInt(data.getTotalExemptAllowances()));
        s.set("AllwncExemptUs10", allwExempt);

        s.put("NetSalary", safeInt(data.getNetSalary()));

        int stdDed = "NEW".equals(data.getTaxRegime()) ? 75000 : 50000;
        stdDed = Math.min(stdDed, safeInt(data.getNetSalary()));
        s.put("DeductionUs16ia", stdDed);
        s.put("DeductionUs16ii", safeInt(data.getEntertainmentAllowance()));
        int profTax = "NEW".equals(data.getTaxRegime()) ? 0 : Math.min(2500, safeInt(data.getProfessionalTax()));
        s.put("DeductionUs16iii", profTax);
        s.put("DeductionUs16Total", stdDed + safeInt(data.getEntertainmentAllowance()) + profTax);

        s.put("IncomeFromSalaries", safeInt(data.getIncomeFromSalary()));
        return s;
    }

    private ObjectNode buildScheduleHP(Itr2FormData data) {
        ObjectNode hp = mapper.createObjectNode();
        ArrayNode props = mapper.createArrayNode();

        if (data.getHousePropertiesNew() != null) {
            for (ITRSharedDtos.PropertyDetail p : data.getHousePropertiesNew()) {
                ObjectNode prop = mapper.createObjectNode();
                prop.put("PropertyAddress", nvl(p.getAddress()));
                prop.put("TypeOfHP", mapPropertyType(p.getType()));
                prop.put("GrossRentReceived", safeInt(p.getGrossAnnualValue()));
                prop.put("TaxPaidLocalAuthority", safeInt(p.getMunicipalTax()));
                prop.put("AnnualValue", safeInt(p.getNetAnnualValue()));
                
                int stdDed = (p.getType() == ITRSharedDtos.PropertyType.SELF_OCCUPIED) ? 0 : safeInt(p.getStandardDeduction());
                prop.put("StandardDeduction30Pct", stdDed);
                
                int interest = p.getInterestOnBorrowedCapital();
                if ("OLD".equals(data.getTaxRegime()) && p.getType() == ITRSharedDtos.PropertyType.SELF_OCCUPIED) {
                    interest = Math.min(interest, 200000);
                }
                if ("NEW".equals(data.getTaxRegime()) && p.getType() == ITRSharedDtos.PropertyType.SELF_OCCUPIED) {
                    interest = 0;
                }
                prop.put("InterestPayable", interest);
                prop.put("ArrearUnrealizedRent", safeInt(p.getArrearUnrealisedRent()));
                prop.put("IncomeFromHP",
                    safeInt(p.getNetAnnualValue()) - stdDed - interest + safeInt(p.getArrearUnrealisedRent()));

                if (p.isCoOwned()) {
                    prop.put("CoOwnerPAN", nvl(p.getCoOwnerPAN()));
                    prop.put("OwnerSharePct", p.getOwnershipSharePct());
                }

                props.add(prop);
            }
        }

        hp.set("PropertyDetails", props);
        hp.put("TotalHPIncome", safeInt(data.getTotalHPIncome()));
        return hp;
    }

    private String mapPropertyType(ITRSharedDtos.PropertyType type) {
        if (type == null) return "S";
        return switch (type) {
            case SELF_OCCUPIED -> "S";
            case LET_OUT -> "L";
            case DEEMED_LET_OUT -> "D";
        };
    }

    private ObjectNode buildScheduleCG(Itr2FormData data) {
        ITRSharedDtos.ScheduleCGData cg = data.getScheduleCGNew();
        ObjectNode cgNode = mapper.createObjectNode();
        if (cg == null) return cgNode;

        ObjectNode stcg = mapper.createObjectNode();
        ObjectNode stcgPre = mapper.createObjectNode();
        stcgPre.put("TaxableRateSec111A_15Pct", safeInt(cg.getStcg111A_preJul23()));
        stcg.set("SaleBeforeJuly2024_111A", stcgPre);

        ObjectNode stcgPost = mapper.createObjectNode();
        stcgPost.put("TaxableRateSec111A_20Pct", safeInt(cg.getStcg111A_postJul23()));
        stcg.set("SaleOnOrAfterJuly2024_111A", stcgPost);

        stcg.put("STCGOnOtherAssets_SlabRate", safeInt(cg.getStcgAtSlab()));
        stcg.put("TotalSTCG", safeInt(cg.getTotalSTCG()));
        cgNode.set("ShortTermCapGain", stcg);

        if (cg.getSchedule112ARows() != null && !cg.getSchedule112ARows().isEmpty()) {
            ArrayNode rows112A = mapper.createArrayNode();
            for (ITRSharedDtos.Schedule112ARow row : cg.getSchedule112ARows()) {
                ObjectNode r = mapper.createObjectNode();
                r.put("ISINCode", nvl(row.getIsinCode()));
                r.put("NameOfScrip", nvl(row.getNameOfScrip()));
                r.put("SharesOrUnits", safeInt(row.getUnits()));
                r.put("SalePricePerUnit", safeInt(row.getSalePricePerUnit()));
                r.put("TotalSaleValue", safeInt(row.getTotalSaleValue()));
                r.put("ActualCostOfAcq", safeInt(row.getActualCostOfAcquisition()));
                
                boolean preGrandfathering = row.getDateOfAcquisition() != null &&
                    row.getDateOfAcquisition().isBefore(LocalDate.of(2018, 2, 1));
                r.put("FMVPerShareOn31Jan2018", preGrandfathering ? safeInt(row.getFmvPerShareOn31Jan2018()) : 0);
                r.put("TotalFMV_55_2_ac", preGrandfathering ? safeInt(row.getTotalFMV55_2_ac()) : 0);
                
                int col7 = preGrandfathering
                    ? Math.max(row.getActualCostOfAcquisition(), row.getFmvOn31Jan2018())
                    : row.getActualCostOfAcquisition();
                r.put("CostWithoutIndexation", col7);
                r.put("ExpenditureOnTransfer", safeInt(row.getExpenditureOnTransfer()));
                r.put("TotalDeductions", col7 + safeInt(row.getExpenditureOnTransfer()));
                r.put("BalanceLTCG", safeInt(row.getTotalSaleValue()) - col7 - safeInt(row.getExpenditureOnTransfer()));
                r.put("TransferDate", row.getDateOfTransfer() != null ? ITDDateFormatter.format(row.getDateOfTransfer()) : "");
                r.put("ExemptionUs54EC", safeInt(row.getExemptionUs54EC()));
                r.put("ExemptionUs54F", safeInt(row.getExemptionUs54F()));
                r.put("PreJuly23Sale", row.isPreJuly23Sale() ? "Y" : "N");
                rows112A.add(r);
            }
            cgNode.set("Schedule112A", rows112A);
        }

        ObjectNode ltcg = mapper.createObjectNode();
        int ltcgPre = safeInt(cg.getLtcg112A_preJul23());
        int taxablePre = Math.max(0, ltcgPre - 100000);
        ObjectNode ltcgPreNode = mapper.createObjectNode();
        ltcgPreNode.put("TotalLTCG112A_PreJul23", ltcgPre);
        ltcgPreNode.put("ExemptionThreshold_100000", Math.min(ltcgPre, 100000));
        ltcgPreNode.put("TaxableLTCG_10Pct", taxablePre);
        ltcg.set("LTCGEquity_PreJuly2024_112A", ltcgPreNode);

        int ltcgPost = safeInt(cg.getLtcg112A_postJul23());
        int taxablePost = Math.max(0, ltcgPost - 125000);
        ObjectNode ltcgPostNode = mapper.createObjectNode();
        ltcgPostNode.put("TotalLTCG112A_PostJul23", ltcgPost);
        ltcgPostNode.put("ExemptionThreshold_125000", Math.min(ltcgPost, 125000));
        ltcgPostNode.put("TaxableLTCG_12_5Pct", taxablePost);
        ltcg.set("LTCGEquity_PostJuly2024_112A", ltcgPostNode);

        ltcg.put("TotalLTCG", safeInt(cg.getTotalLTCG()));
        cgNode.set("LongTermCapGain", ltcg);

        cgNode.put("TotalCGIncome", safeInt(cg.getTotalCGIncome()));
        return cgNode;
    }

    private ObjectNode buildScheduleVIA(Itr2FormData data) {
        ObjectNode via = mapper.createObjectNode();
        boolean isNew = "NEW".equals(data.getTaxRegime());
        via.put("DeductionUs80C", isNew ? 0 : safeInt(data.getDeduction80C()));
        via.put("DeductionUs80CCC", isNew ? 0 : safeInt(data.getDeduction80CCC()));
        via.put("DeductionUs80CCD1", isNew ? 0 : safeInt(data.getDeduction80CCD1()));
        via.put("DeductionUs80CCD1B", isNew ? 0 : safeInt(data.getDeduction80CCD1B()));
        via.put("DeductionUs80CCD2", safeInt(data.getDeduction80CCD2()));
        via.put("DeductionUs80CCH", safeInt(data.getDeduction80CCH()));
        via.put("DeductionUs80D", isNew ? 0 : safeInt(data.getDeduction80D()));
        via.put("DeductionUs80DD", isNew ? 0 : safeInt(data.getDeduction80DD()));
        via.put("DeductionUs80DDB", isNew ? 0 : safeInt(data.getDeduction80DDB()));
        via.put("DeductionUs80E", isNew ? 0 : safeInt(data.getDeduction80E()));
        via.put("DeductionUs80EEA", isNew ? 0 : safeInt(data.getDeduction80EEA()));
        via.put("DeductionUs80EEB", isNew ? 0 : safeInt(data.getDeduction80EEB()));
        via.put("DeductionUs80G", isNew ? 0 : safeInt(data.getDeduction80G()));
        via.put("DeductionUs80GG", isNew ? 0 : safeInt(data.getDeduction80GG()));
        via.put("DeductionUs80GGA", isNew ? 0 : safeInt(data.getDeduction80GGA()));
        via.put("DeductionUs80TTA", isNew ? 0 : safeInt(data.getDeduction80TTA()));
        via.put("DeductionUs80TTB", isNew ? 0 : safeInt(data.getDeduction80TTB()));
        via.put("DeductionUs80U", isNew ? 0 : safeInt(data.getDeduction80U()));
        via.put("DeductionUs80JJAA", safeInt(data.getDeduction80JJAA()));
        via.put("TotalChapVIADeductions", safeInt(data.getTotalChapterVIADeductions()));
        return via;
    }

    private ObjectNode buildScheduleOS(Itr2FormData data) {
        ObjectNode os = mapper.createObjectNode();
        os.put("InterestFromSavingsBankAcc", safeInt(data.getInterestSavingsBank()));
        os.put("InterestFromDeposits", safeInt(data.getInterestFD()));
        os.put("InterestFromITRefund", safeInt(data.getInterestITRefund()));
        os.put("DividendGross", safeInt(data.getDividendIncome()));
        os.put("FamilyPension", safeInt(data.getFamilyPension()));
        int familyPensionDed = "OLD".equals(data.getTaxRegime())
            ? Math.min(15000, (int) Math.round(safeInt(data.getFamilyPension()) / 3.0))
            : Math.min(25000, (int) Math.round(safeInt(data.getFamilyPension()) / 3.0));
        os.put("FamilyPensionDedUs57iia", familyPensionDed);
        os.put("OtherIncomeTotal", safeInt(data.getOtherSourcesIncomeTotal()));
        return os;
    }

    private ObjectNode buildTaxComputation(Itr2FormData data) {
        ObjectNode tc = mapper.createObjectNode();
        tc.put("TaxPayableOnTI", safeInt(data.getTaxOnTotalIncome()));
        tc.put("RebateUs87A", safeInt(data.getRebate87A()));
        tc.put("TaxAfterRebate", safeInt(data.getTaxAfterRebate()));
        tc.put("Surcharge", safeInt(data.getSurcharge()));
        tc.put("SurchargeOnSpecialInc", safeInt(data.getSurchargeOnSpecialIncome()));
        tc.put("TotalSurcharge", safeInt(data.getTotalSurcharge()));
        tc.put("HealthEduCess", safeInt(data.getHealthEducationCess()));
        tc.put("TotalTaxAndCess", safeInt(data.getTotalTaxAndCess()));
        tc.put("ReliefUs89", safeInt(data.getReliefUs89()));
        tc.put("NetTaxLiability", safeInt(data.getNetTaxLiability()));
        tc.put("IntrstUs234A", safeInt(data.getInterest234A()));
        tc.put("IntrstUs234B", safeInt(data.getInterest234B()));
        tc.put("IntrstUs234C", safeInt(data.getInterest234C()));
        tc.put("LateFilingFeeUs234F", safeInt(data.getLateFilingFee234F()));
        tc.put("TotalIntrstPnltyFee",
            safeInt(data.getInterest234A()) + safeInt(data.getInterest234B()) +
            safeInt(data.getInterest234C()) + safeInt(data.getLateFilingFee234F()));
        tc.put("GrossTaxLiability", safeInt(data.getNetTaxLiability()) +
            safeInt(data.getInterest234A()) + safeInt(data.getInterest234B()) +
            safeInt(data.getInterest234C()) + safeInt(data.getLateFilingFee234F()));
        return tc;
    }

    private ObjectNode buildTaxPaid(Itr2FormData data) {
        ObjectNode tp = mapper.createObjectNode();
        ObjectNode tds1 = mapper.createObjectNode();
        ArrayNode tds1List = mapper.createArrayNode();
        if (data.getTdsSalaryEntries() != null) {
            for (CommonFormData.TDSEntry e : data.getTdsSalaryEntries()) {
                ObjectNode t = mapper.createObjectNode();
                t.put("EmployerOrDeductorTAN", nvl(e.getTan()));
                t.put("EmployerName", nvl(e.getDeductorName()));
                t.put("TotalTDSSal", safeInt((int) e.getTaxDeducted()));
                t.put("ClaimOutOfTotTDSSal", safeInt((int) e.getTaxDeducted()));
                tds1List.add(t);
            }
        }
        tds1.set("TDSSal", tds1List);
        tds1.put("TotalTDS1TaxDeducted", data.getTdsSalaryEntries() == null ? 0 :
            data.getTdsSalaryEntries().stream().mapToInt(e -> (int) e.getTaxDeducted()).sum());
        tds1.put("TotalTDS1TaxClaimed", data.getTdsSalaryEntries() == null ? 0 :
            data.getTdsSalaryEntries().stream().mapToInt(e -> (int) e.getTaxDeducted()).sum());
        tp.set("TDS1", tds1);
        ObjectNode tds2 = mapper.createObjectNode();
        ArrayNode tds2List = mapper.createArrayNode();
        if (data.getTdsOtherEntries() != null) {
            for (CommonFormData.TDSEntry e : data.getTdsOtherEntries()) {
                ObjectNode t = mapper.createObjectNode();
                t.put("DeductorTAN", nvl(e.getTan()));
                t.put("DeductorName", nvl(e.getDeductorName()));
                t.put("AmtOnWhichTDSDeducted", safeInt((int) e.getGrossAmount()));
                t.put("TaxDeducted", safeInt((int) e.getTaxDeducted()));
                t.put("YrOfTaxDeduction", nvl(data.getAssessmentYear()));
                t.put("ClaimOutOfTotTDSOthThanSals", safeInt((int) e.getTaxDeducted()));
                tds2List.add(t);
            }
        }
        tds2.set("TDSOthThanSals", tds2List);
        tds2.put("TotalTDSOthThanSalClaimed", data.getTdsOtherEntries() == null ? 0 :
            data.getTdsOtherEntries().stream().mapToInt(e -> (int) e.getTaxDeducted()).sum());
        tp.set("TDS2", tds2);
        ObjectNode tcs = mapper.createObjectNode();
        ArrayNode tcsList = mapper.createArrayNode();
        if (data.getTcsEntries() != null) {
            for (CommonFormData.TCSEntry e : data.getTcsEntries()) {
                ObjectNode t = mapper.createObjectNode();
                t.put("CollectorTAN", nvl(e.getTan()));
                t.put("CollectorName", nvl(e.getCollectorName()));
                t.put("AmtOnWhichTCSCollected", safeInt((int) e.getGrossAmount()));
                t.put("TaxCollected", safeInt((int) e.getTaxCollected()));
                t.put("ClaimOutOfTotTCS", safeInt((int) e.getTaxCollected()));
                tcsList.add(t);
            }
        }
        tcs.set("TCS", tcsList);
        tcs.put("TotalTCSClaimedThisYear", data.getTcsEntries() == null ? 0 :
            data.getTcsEntries().stream().mapToInt(e -> (int) e.getTaxCollected()).sum());
        tp.set("TCS", tcs);
        ObjectNode at = mapper.createObjectNode();
        ArrayNode atList = mapper.createArrayNode();
        if (data.getAdvanceTaxEntries() != null) {
            for (CommonFormData.AdvanceTaxEntry e : data.getAdvanceTaxEntries()) {
                ObjectNode t = mapper.createObjectNode();
                t.put("BSRCode", nvl(e.getBsrCode()));
                t.put("DateDep", nvl(e.getDateOfDeposit()));
                t.put("SrlNoOfChaln", nvl(e.getChallanNo()));
                t.put("Amt", safeInt((int) e.getAmount()));
                t.put("Type", "300");
                atList.add(t);
            }
        }
        at.set("AdvTaxDetails", atList);
        at.put("TotalAdvTaxPaid", data.getAdvanceTaxEntries() == null ? 0 :
            data.getAdvanceTaxEntries().stream().mapToInt(e -> (int) e.getAmount()).sum());
        tp.set("AdvanceTax", at);
        return tp;
    }

    private ObjectNode buildVerification(Itr2FormData data) {
        ObjectNode v = mapper.createObjectNode();
        v.put("Capacity", "S");
        v.put("AssesseeVerName", nvl(data.getFirstName() + " " + data.getSurName()));
        v.put("FatherName", nvl(data.getFatherName()));
        v.put("Place", nvl(data.getPlaceOfFiling()));
        v.put("Date", ITDDateFormatter.format(LocalDate.now()));
        v.put("Declaration",
            "I solemnly declare that the information given in this return and the schedules " +
            "thereto is correct and complete and that the amount of total income and other " +
            "particulars shown therein are truly stated.");
        return v;
    }

    private ObjectNode buildBankAccount(Itr2FormData data) {
        ObjectNode bank = mapper.createObjectNode();
        bank.put("IFSCCode", nvl(data.getBankIFSC()));
        bank.put("BankName", nvl(data.getBankName()));
        bank.put("AccountNo", nvl(data.getBankAccountNumber()));
        bank.put("AccountType", nvl(data.getBankAccountType()));
        bank.put("IsPreValidated", "Y");
        return bank;
    }

    private ObjectNode buildFormITR2(Itr2FormData data) {
        ObjectNode form = mapper.createObjectNode();
        form.set("PersonalInfo", buildPersonalInfo(data));
        form.set("FilingStatus", buildFilingStatus(data));
        form.set("ScheduleS", buildScheduleS(data));
        form.set("ScheduleHP", buildScheduleHP(data));
        form.set("ScheduleCG", buildScheduleCG(data));
        form.set("ScheduleOS", buildScheduleOS(data));
        form.set("ScheduleVIA", buildScheduleVIA(data));
        form.set("TaxComputation", buildTaxComputation(data));
        form.set("TaxPaid", buildTaxPaid(data));
        form.set("Verification", buildVerification(data));
        form.set("BankAccountDetail", buildBankAccount(data));
        return form;
    }

    private int safeInt(Integer val) {
        return val == null ? 0 : val;
    }

    private String nvl(String s) {
        return s == null ? "" : s.replaceAll("[~@#$%^&*()_+{}|:<>?]", "");
    }
}
