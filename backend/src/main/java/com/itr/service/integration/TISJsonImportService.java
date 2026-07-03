package com.itr.service.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itr.dto.TISData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * TIS JSON import service - handles JSON format from ITD portal.
 * Document 3 Phase 4
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TISJsonImportService {
    
    private final ObjectMapper objectMapper;
    
    public TISData importFromJson(String json, String pan) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        
        TISData data = TISData.builder()
                .dividendIncome(root.path("dividendIncome").asLong(0))
                .interestFromDeposit(root.path("interestFromDeposit").asLong(0))
                .securitiesSaleConsideration(root.path("securitiesSaleConsideration").asLong(0))
                .securitiesPurchaseAmount(root.path("securitiesPurchaseAmount").asLong(0))
                .interestOnSecurities(root.path("interestOnSecurities").asLong(0))
                .salaryAmount(root.path("salaryAmount").asLong(0))
                .rentIncome(root.path("rentIncome").asLong(0))
                .build();
        
        log.info("TIS JSON import complete: Dividend={}, Interest={}, SecSale={}, Salary={}",
                data.getDividendIncome(), data.getInterestFromDeposit(),
                data.getSecuritiesSaleConsideration(), data.getSalaryAmount());
        
        return data;
    }
}
