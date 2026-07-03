package com.itr.service.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itr.dto.Form26ASData;
import com.itr.dto.Form26ASData.TDSOtherThanSalary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Form26AS JSON import service - handles JSON format from ITD portal.
 * Document 3 Phase 4
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class Form26ASJsonImportService {
    
    private final ObjectMapper objectMapper;
    
    public Form26ASData importFromJson(String json, String pan) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        
        // Support both simplified JSON and ITD prefill JSON format
        if (root.has("form26as")) {
            return importFromPrefillJson(root, pan);
        } else {
            return importFromSimpleJson(root, pan);
        }
    }
    
    /**
     * Import from ITD prefill JSON structure (personalInfo.form26as.*)
     */
    private Form26ASData importFromPrefillJson(JsonNode root, String pan) {
        JsonNode form26as = root.path("form26as");
        JsonNode personalInfo = root.path("personalInfo");
        
        String assesseeName = personalInfo.path("assesseeName").path("firstName").asText("") + " " +
                personalInfo.path("assesseeName").path("middleName").asText("") + " " +
                personalInfo.path("assesseeName").path("surNameOrOrgName").asText("");
        
        Form26ASData data = Form26ASData.builder()
                .assessePAN(pan)
                .assesseName(assesseeName.trim())
                .tdsOnInterest(new ArrayList<>())
                .tdsOnContractor(new ArrayList<>())
                .tdsOnProfessional(new ArrayList<>())
                .tdsOnCommission(new ArrayList<>())
                .tdsOnRent(new ArrayList<>())
                .tdsOnInsurance(new ArrayList<>())
                .tdsOnProperty(new ArrayList<>())
                .tdsOnOther(new ArrayList<>())
                .build();
        
        // Parse TDS on other than salary
        JsonNode tdsOther = form26as.path("tdsOnOthThanSals").path("tdSonOthThanSal");
        if (tdsOther.isArray()) {
            for (JsonNode entry : tdsOther) {
                TDSOtherThanSalary tds = parsePrefillTdsEntry(entry);
                if (tds != null) {
                    classifyEntry(data, tds);
                }
            }
        }
        
        // Calculate totals
        data.setTotalTDSInterest(calculateTotal(data.getTdsOnInterest()));
        data.setTotalTDSContractor(calculateTotal(data.getTdsOnContractor()));
        data.setTotalTDSProfessional(calculateTotal(data.getTdsOnProfessional()));
        data.setTotalTDSOther(calculateTotal(data.getTdsOnOther()));
        
        log.info("26AS prefill JSON import: Interest={}, Contractor={}, Professional={}, Other={}",
                data.getTdsOnInterest().size(), data.getTdsOnContractor().size(),
                data.getTdsOnProfessional().size(), data.getTdsOnOther().size());
        
        return data;
    }
    
    /**
     * Import from simplified JSON structure (direct tdsEntries array)
     */
    private Form26ASData importFromSimpleJson(JsonNode root, String pan) {
        Form26ASData data = Form26ASData.builder()
                .assessePAN(pan)
                .assesseName(root.path("assesseeName").asText(""))
                .tdsOnInterest(new ArrayList<>())
                .tdsOnContractor(new ArrayList<>())
                .tdsOnProfessional(new ArrayList<>())
                .tdsOnCommission(new ArrayList<>())
                .tdsOnRent(new ArrayList<>())
                .tdsOnInsurance(new ArrayList<>())
                .tdsOnProperty(new ArrayList<>())
                .tdsOnOther(new ArrayList<>())
                .build();
        
        JsonNode tdsEntries = root.path("tdsEntries");
        if (tdsEntries.isArray()) {
            for (JsonNode entry : tdsEntries) {
                TDSOtherThanSalary tds = parseTdsEntry(entry);
                if (tds != null) {
                    classifyEntry(data, tds);
                }
            }
        }
        
        data.setTotalTDSInterest(calculateTotal(data.getTdsOnInterest()));
        data.setTotalTDSContractor(calculateTotal(data.getTdsOnContractor()));
        data.setTotalTDSProfessional(calculateTotal(data.getTdsOnProfessional()));
        data.setTotalTDSOther(calculateTotal(data.getTdsOnOther()));
        
        log.info("26AS simple JSON import: Interest={}, Contractor={}, Professional={}, Other={}",
                data.getTdsOnInterest().size(), data.getTdsOnContractor().size(),
                data.getTdsOnProfessional().size(), data.getTdsOnOther().size());
        
        return data;
    }
    
    /**
     * Parse TDS entry from ITD prefill format
     */
    private TDSOtherThanSalary parsePrefillTdsEntry(JsonNode entry) {
        String section = entry.path("sectionCode").asText("");
        if (section.isEmpty()) return null;
        
        JsonNode deductor = entry.path("employerOrDeductorOrCollectDetl");
        long grossAmount = entry.path("grossAmount").asLong(0);
        long tdsAmount = entry.path("taxDeductCreditDtls").path("taxDeductedOwnHands").asLong(0);
        
        return TDSOtherThanSalary.builder()
                .deductorName(deductor.path("employerOrDeductorOrCollecterName").asText(""))
                .deductorTAN(deductor.path("tan").asText(""))
                .section(section)
                .amountPaid(BigDecimal.valueOf(grossAmount))
                .taxDeducted(BigDecimal.valueOf(tdsAmount))
                .build();
    }
    
    private TDSOtherThanSalary parseTdsEntry(JsonNode entry) {
        String section = entry.path("section").asText("");
        if (section.isEmpty()) return null;
        
        return TDSOtherThanSalary.builder()
                .deductorName(entry.path("deductorName").asText(""))
                .deductorTAN(entry.path("deductorTAN").asText(""))
                .section(section)
                .amountPaid(new BigDecimal(entry.path("amountPaid").asText("0")))
                .taxDeducted(new BigDecimal(entry.path("taxDeducted").asText("0")))
                .build();
    }
    
    private void classifyEntry(Form26ASData data, TDSOtherThanSalary e) {
        String s = e.getSection();
        if (s == null) s = "OTHER";
        
        switch (s) {
            case "194A": data.getTdsOnInterest().add(e); break;
            case "194C": data.getTdsOnContractor().add(e); break;
            case "194J": data.getTdsOnProfessional().add(e); break;
            case "194H": data.getTdsOnCommission().add(e); break;
            case "194I": data.getTdsOnRent().add(e); break;
            case "194D": data.getTdsOnInsurance().add(e); break;
            case "194IA": case "194IB": data.getTdsOnProperty().add(e); break;
            default: data.getTdsOnOther().add(e); break;
        }
    }
    
    private BigDecimal calculateTotal(List<TDSOtherThanSalary> list) {
        return list.stream()
                .map(TDSOtherThanSalary::getTaxDeducted)
                .filter(tax -> tax != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
