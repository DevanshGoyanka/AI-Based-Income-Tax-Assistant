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

@Slf4j
@Service
public class ITR3JSONExportService {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ITR2JSONExportService itr2Service;

    public ITR3JSONExportService(ITR2JSONExportService itr2Service) {
        this.itr2Service = itr2Service;
    }

    public String export(Itr3FormData data) {
        try {
            ObjectNode root = mapper.createObjectNode();
            ObjectNode itr = mapper.createObjectNode();
            ObjectNode itr3 = mapper.createObjectNode();

            itr3.set("CreationInfo", buildCreationInfo(data));
            itr3.set("Form_ITR3", buildFormITR3(data));

            itr.set("ITR3", itr3);
            root.set("ITR", itr);

            String jsonWithoutDigest = mapper.writeValueAsString(root);
            String digest = SHA256DigestUtil.computeDigest(jsonWithoutDigest);
            ((ObjectNode) root.path("ITR").path("ITR3").path("CreationInfo"))
                .put("Digest", digest);

            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (Exception e) {
            log.error("ITR-3 JSON export failed", e);
            throw new RuntimeException("ITR-3 JSON export failed: " + e.getMessage(), e);
        }
    }

    private ObjectNode buildCreationInfo(Itr3FormData data) {
        ObjectNode ci = mapper.createObjectNode();
        ci.put("SWVersionNo", "1.1");
        ci.put("SWCreatedBy", "ITR_ERP_v1");
        ci.put("JSONCreatedBy", "ITR_ERP_v1");
        ci.put("JSONCreatedDate", ITDDateFormatter.format(LocalDate.now()));
        ci.put("InterfaceType", "JSON");
        String city = data.getPartA() != null ? data.getPartA().getCity() : "SYSTEM";
        ci.put("IntermediaryCity", city != null && city.length() > 25 ? city.substring(0, 25) : city);
        ci.put("Digest", "");
        return ci;
    }

    private ObjectNode buildScheduleBP(Itr3FormData data) {
        ObjectNode bp = mapper.createObjectNode();
        if (data.getScheduleBP() == null) return bp;

        Itr3FormData.ScheduleBusinessProfession sched = data.getScheduleBP();
        
        bp.put("NetProfitAsPerBooks", (int) sched.getNetProfitAsPerBooks());
        bp.put("AddPersonalExpenses", (int) sched.getAddPersonalExpenses());
        bp.put("AddCapitalExpenses", (int) sched.getAddCapitalExpenses());
        bp.put("AddDisallowance40A3", (int) sched.getAddDisallowance40A3());
        bp.put("AddDisallowance43B", (int) sched.getAddDisallowance43B());
        bp.put("AddDisallowance43Bh_MSME", safeInt(sched.getMsmeDisallowance()));
        bp.put("AddDisallowance14A", (int) sched.getAddDisallowance14A());
        bp.put("LessITActDepreciation", (int) sched.getLessITActDepreciation());
        bp.put("LessAdditionalDepreciation", (int) sched.getLessAdditionalDepreciation());
        bp.put("LessOtherDeductions", (int) sched.getLessOtherDeductions());
        bp.put("ProfitFromBusiness", (int) sched.getProfitFromBusiness());
        
        if (sched.isFandO()) {
            bp.put("FandOTurnover", (int) sched.getFandOTurnover());
        }
        
        if (sched.isSpeculative()) {
            bp.put("SpeculativeTurnover", (int) sched.getSpeculativeTurnover());
        }
        
        if (sched.getDeemedCapitalGains() > 0) {
            bp.put("DeemedCapitalGainsUs50CA", (int) sched.getDeemedCapitalGains());
        }

        return bp;
    }

    private ObjectNode buildScheduleDPM(Itr3FormData data) {
        ObjectNode dpm = mapper.createObjectNode();
        if (data.getScheduleDPM() == null) return dpm;

        Itr3FormData.ScheduleDepreciation sched = data.getScheduleDPM();
        ArrayNode blocks = mapper.createArrayNode();

        if (sched.getBlocks() != null) {
            for (Itr3FormData.DepreciationBlock block : sched.getBlocks()) {
                ObjectNode b = mapper.createObjectNode();
                b.put("BlockDescription", nvl(block.getDescription()));
                b.put("Rate", block.getDepreciationRate());
                b.put("WDVAtBeginning", (int) block.getOpeningWDV());
                b.put("Additions", (int) (block.getAdditionsFirstHalf() + block.getAdditionsSecondHalf()));
                b.put("Deductions", (int) block.getSaleProceeds());
                b.put("WDVBeforeDepreciation", (int) (block.getOpeningWDV() + block.getAdditionsFirstHalf() + block.getAdditionsSecondHalf() - block.getSaleProceeds()));
                b.put("DepreciationAmount", (int) block.getDepreciationAmount());
                b.put("WDVAtEnd", (int) block.getClosingWDV());
                blocks.add(b);
            }
        }

        dpm.set("DepreciationBlocks", blocks);
        dpm.put("TotalDepreciation", (int) sched.getTotalDepreciation());
        
        if (sched.getGoodwillDepreciation() > 0) {
            dpm.put("GoodwillDepreciation", (int) sched.getGoodwillDepreciation());
        }

        return dpm;
    }

    private ObjectNode buildScheduleGST(Itr3FormData data) {
        ObjectNode gst = mapper.createObjectNode();
        if (data.getGstRegistrations() == null || data.getGstRegistrations().isEmpty()) return gst;

        ArrayNode registrations = mapper.createArrayNode();
        for (ITRBusinessDtos.GSTRegistration reg : data.getGstRegistrations()) {
            ObjectNode r = mapper.createObjectNode();
            r.put("GSTIN", nvl(reg.getGstin()));
            r.put("TradeName", nvl(reg.getBusinessName()));
            r.put("GrossTurnover", safeInt(reg.getTurnoverBooks()));
            registrations.add(r);
        }

        gst.set("GSTRegistrations", registrations);
        gst.put("TotalGSTTurnover", data.getGstRegistrations().stream()
            .mapToInt(r -> r.getTurnoverBooks()).sum());

        return gst;
    }

    private ObjectNode buildSchedulePL(Itr3FormData data) {
        ObjectNode pl = mapper.createObjectNode();
        if (data.getProfitAndLoss() == null) return pl;

        ITRBusinessDtos.ProfitAndLossData pnl = data.getProfitAndLoss();

        pl.put("GrossReceipts", safeInt(pnl.getGrossReceipts()));
        pl.put("OtherIncome", safeInt(pnl.getOtherIncome()));
        pl.put("OpeningStock", safeInt(pnl.getOpeningStock()));
        pl.put("Purchases", safeInt(pnl.getPurchases()));
        pl.put("ClosingStock", safeInt(pnl.getClosingStock()));
        pl.put("DirectExpenses", safeInt(pnl.getDirectExpenses()));
        pl.put("GrossProfit", safeInt(pnl.getGrossProfit()));
        pl.put("EmployeeCost", safeInt(pnl.getSalariesAndWages()));
        pl.put("Depreciation", safeInt(pnl.getDepreciation()));
        pl.put("OtherExpenses", safeInt(pnl.getOtherExpenses()));
        pl.put("NetProfitBeforeTax", safeInt(pnl.getNetProfitAsPerPL()));

        return pl;
    }

    private ObjectNode buildScheduleBS(Itr3FormData data) {
        ObjectNode bs = mapper.createObjectNode();
        if (data.getBalanceSheet() == null) return bs;

        ITRBusinessDtos.BalanceSheetData balance = data.getBalanceSheet();

        ObjectNode assets = mapper.createObjectNode();
        assets.put("FixedAssets", safeInt(balance.getFixedAssetsNet()));
        assets.put("Stock", safeInt(balance.getStockInTrade()));
        assets.put("Debtors", safeInt(balance.getSundryDebtors()));
        assets.put("CashAndBank", safeInt(balance.getCashAndBankBalance()));
        assets.put("LoansAndAdvances", safeInt(balance.getLoansAndAdvances()));
        assets.put("TotalAssets", safeInt(balance.getTotalAssets()));
        bs.set("Assets", assets);

        ObjectNode liabilities = mapper.createObjectNode();
        liabilities.put("Capital", safeInt(balance.getCapitalAccount()));
        liabilities.put("ReservesAndSurplus", safeInt(balance.getReservesAndSurplus()));
        liabilities.put("SecuredLoans", safeInt(balance.getSecuredLoans()));
        liabilities.put("UnsecuredLoans", safeInt(balance.getUnsecuredLoans()));
        liabilities.put("Creditors", safeInt(balance.getSundryCreditors()));
        liabilities.put("TotalLiabilities", safeInt(balance.getTotalLiabilities()));
        bs.set("Liabilities", liabilities);

        return bs;
    }

    private ObjectNode buildFormITR3(Itr3FormData data) {
        ObjectNode form = mapper.createObjectNode();
        
        form.set("ScheduleBP", buildScheduleBP(data));
        form.set("ScheduleDPM", buildScheduleDPM(data));
        form.set("ScheduleGST", buildScheduleGST(data));
        form.set("SchedulePL", buildSchedulePL(data));
        form.set("ScheduleBS", buildScheduleBS(data));

        return form;
    }

    private int safeInt(Integer val) {
        return val == null ? 0 : val;
    }

    private String nvl(String s) {
        return s == null ? "" : s.replaceAll("[~@#$%^&*()_+{}|:<>?]", "");
    }
}
