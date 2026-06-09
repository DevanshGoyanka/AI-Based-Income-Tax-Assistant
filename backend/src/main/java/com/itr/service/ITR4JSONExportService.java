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
public class ITR4JSONExportService {

    private final ObjectMapper mapper = new ObjectMapper();

    public String export(Itr4FormData data) {
        try {
            ObjectNode root = mapper.createObjectNode();
            ObjectNode itr = mapper.createObjectNode();
            ObjectNode itr4 = mapper.createObjectNode();

            itr4.set("CreationInfo", buildCreationInfo(data));
            itr4.set("Form_ITR4", buildFormITR4(data));

            itr.set("ITR4", itr4);
            root.set("ITR", itr);

            String jsonWithoutDigest = mapper.writeValueAsString(root);
            String digest = SHA256DigestUtil.computeDigest(jsonWithoutDigest);
            ((ObjectNode) root.path("ITR").path("ITR4").path("CreationInfo"))
                .put("Digest", digest);

            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (Exception e) {
            log.error("ITR-4 JSON export failed", e);
            throw new RuntimeException("ITR-4 JSON export failed: " + e.getMessage(), e);
        }
    }

    private ObjectNode buildCreationInfo(Itr4FormData data) {
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

    private ObjectNode buildPresumptiveIncome(Itr4FormData data) {
        ObjectNode pi = mapper.createObjectNode();
        
        if (data.getSchedulePresumptive() != null) {
            Itr4FormData.SchedulePresumptive sched = data.getSchedulePresumptive();
            
            if (sched.getBusiness44AD() != null && sched.getBusiness44AD().isApplicable()) {
                Itr4FormData.Business44AD biz = sched.getBusiness44AD();
                ObjectNode ad = mapper.createObjectNode();
                ad.put("GrossTurnoverDigital", (int) biz.getGrossTurnoverDigital());
                ad.put("GrossTurnoverCash", (int) biz.getGrossTurnoverCash());
                ad.put("GrossTurnoverTotal", (int) biz.getGrossTurnoverTotal());
                ad.put("PresumptiveIncomeDigital", (int) biz.getPresumptiveIncomeDigital());
                ad.put("PresumptiveIncomeCash", (int) biz.getPresumptiveIncomeCash());
                ad.put("TotalPresumptiveIncome", (int) biz.getTotalPresumptiveIncome());
                ad.put("DeclaredIncome", (int) biz.getDeclaredIncome());
                pi.set("Business44AD", ad);
            }
            
            if (sched.getProfessional44ADA() != null && sched.getProfessional44ADA().isApplicable()) {
                Itr4FormData.Professional44ADA prof = sched.getProfessional44ADA();
                ObjectNode ada = mapper.createObjectNode();
                ada.put("GrossReceipts", (int) prof.getGrossReceipts());
                ada.put("PresumptiveIncome", (int) prof.getPresumptiveIncome());
                ada.put("DeclaredIncome", (int) prof.getDeclaredIncome());
                pi.set("Professional44ADA", ada);
            }
            
            if (sched.getVehicles44AE() != null && !sched.getVehicles44AE().isEmpty()) {
                ArrayNode vehicles = mapper.createArrayNode();
                for (Itr4FormData.Vehicle44AE v : sched.getVehicles44AE()) {
                    ObjectNode veh = mapper.createObjectNode();
                    veh.put("VehicleRegNo", nvl(v.getVehicleRegNo()));
                    veh.put("VehicleType", nvl(v.getVehicleType()));
                    veh.put("MonthsOwned", v.getMonthsOwned());
                    veh.put("PresumptiveIncome", (int) v.getPresumptiveIncome());
                    veh.put("DeclaredIncome", (int) v.getDeclaredIncome());
                    vehicles.add(veh);
                }
                pi.set("Vehicles44AE", vehicles);
                pi.put("Total44AEIncome", (int) sched.getTotal44AEIncome());
            }
            
            pi.put("TotalPresumptiveIncome", (int) sched.getTotalPresumptiveIncome());
        }
        
        return pi;
    }

    private ObjectNode buildSimplifiedBS(Itr4FormData data) {
        ObjectNode bs = mapper.createObjectNode();
        if (data.getSimplifiedBalanceSheet() == null) return bs;

        ITRBusinessDtos.SimplifiedBalanceSheet sbs = data.getSimplifiedBalanceSheet();
        
        bs.put("GrossReceipts", safeInt(sbs.getGrossReceipts()));
        bs.put("NetProfit", safeInt(sbs.getNetProfit()));
        bs.put("SundryDebtors", safeInt(sbs.getSundryDebtors()));
        bs.put("SundryCreditors", safeInt(sbs.getSundryCreditors()));
        bs.put("StockInTrade", safeInt(sbs.getStockInTrade()));
        bs.put("CashBalance", safeInt(sbs.getCashBalance()));
        bs.put("OpeningCapital", safeInt(sbs.getOpeningCapital()));
        bs.put("Drawings", safeInt(sbs.getDrawings()));
        bs.put("AdditionsToCapital", safeInt(sbs.getAdditionsToCapital()));
        bs.put("ClosingCapital", safeInt(sbs.getClosingCapital()));
        bs.put("SecuredLoans", safeInt(sbs.getSecuredLoans()));
        bs.put("UnsecuredLoans", safeInt(sbs.getUnsecuredLoans()));
        bs.put("TotalFixedAssetsWDV", safeInt(sbs.getTotalFixedAssetsWDV()));
        
        return bs;
    }

    private ObjectNode buildFormITR4(Itr4FormData data) {
        ObjectNode form = mapper.createObjectNode();
        
        form.set("PresumptiveIncome", buildPresumptiveIncome(data));
        form.set("SimplifiedBalanceSheet", buildSimplifiedBS(data));
        
        return form;
    }

    private int safeInt(Integer val) {
        return val == null ? 0 : val;
    }

    private String nvl(String s) {
        return s == null ? "" : s.replaceAll("[~@#$%^&*()_+{}|:<>?]", "");
    }
}
