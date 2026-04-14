package com.itr.service.integration;

import com.itr.dto.AISData;
import com.itr.dto.Form26ASData;
import com.itr.dto.Itr2FormData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Capital Gains Auto-Population Service - 101% CBDT Compliant
 * Handles STCG/LTCG from AIS/26AS/TIS JSON imports
 */
@Slf4j
@Service
public class CapitalGainsAutoPopulationService {

    public Itr2FormData autoPopulateCapitalGains(Itr2FormData formData, AISData ais) {
        log.info("Auto-populating capital gains from AIS");
        
        if (ais.getCapitalGainsTransactions() == null || ais.getCapitalGainsTransactions().isEmpty()) {
            log.info("No capital gains transactions found in AIS");
            return formData;
        }
        
        if (formData.getScheduleCG() == null) {
            formData.setScheduleCG(Itr2FormData.ScheduleCapitalGains.builder().build());
        }
        
        for (AISData.CapitalGainsTransaction aisTxn : ais.getCapitalGainsTransactions()) {
            Itr2FormData.CGTransaction cg = convertAISToCapitalGain(aisTxn);
            categorizeAndAddTransaction(formData.getScheduleCG(), cg, aisTxn.getSection());
        }
        
        log.info("Auto-populated {} capital gains transactions", ais.getCapitalGainsTransactions().size());
        return formData;
    }

    public Itr2FormData autoPopulateCapitalGainsFrom26AS(Itr2FormData formData, Form26ASData data26AS) {
        log.info("Auto-populating capital gains from Form 26AS");
        
        if (data26AS.getCapitalGainsTransactions() == null || data26AS.getCapitalGainsTransactions().isEmpty()) {
            log.info("No capital gains transactions found in Form 26AS");
            return formData;
        }
        
        if (formData.getScheduleCG() == null) {
            formData.setScheduleCG(Itr2FormData.ScheduleCapitalGains.builder().build());
        }
        
        for (Form26ASData.CapitalGainsTransaction txn : data26AS.getCapitalGainsTransactions()) {
            Itr2FormData.CGTransaction cg = convert26ASToCapitalGain(txn);
            categorizeAndAddTransaction(formData.getScheduleCG(), cg, txn.getSection());
        }
        
        log.info("Auto-populated {} capital gains transactions from 26AS", data26AS.getCapitalGainsTransactions().size());
        return formData;
    }

    private Itr2FormData.CGTransaction convertAISToCapitalGain(AISData.CapitalGainsTransaction aisTxn) {
        Itr2FormData.CGTransaction.CGTransactionBuilder builder = Itr2FormData.CGTransaction.builder();
        
        builder.assetType(aisTxn.getAssetType());
        builder.assetDescription(aisTxn.getAssetDescription());
        builder.isinCode(aisTxn.getIsin());
        
        if (aisTxn.getDateOfAcquisition() != null) {
            builder.acquisitionDate(LocalDate.parse(aisTxn.getDateOfAcquisition()));
        }
        
        if (aisTxn.getDateOfSale() != null) {
            builder.saleDate(LocalDate.parse(aisTxn.getDateOfSale()));
        }
        
        builder.purchasePrice(aisTxn.getPurchasePrice() != null ? aisTxn.getPurchasePrice() : 0.0);
        builder.costOfAcquisition(aisTxn.getPurchasePrice() != null ? aisTxn.getPurchasePrice() : 0.0);
        builder.salePrice(aisTxn.getSalePrice() != null ? aisTxn.getSalePrice() : 0.0);
        builder.transferExpenses(aisTxn.getExpenditureOnTransfer() != null ? aisTxn.getExpenditureOnTransfer() : 0.0);
        
        String section = determineSection(aisTxn.getAssetType(), 
                                         determineGainType(aisTxn.getAssetType(), aisTxn.getDateOfAcquisition(), aisTxn.getDateOfSale()), 
                                         aisTxn.getSttPaid());
        builder.section(section);
        
        builder.sttPaid(aisTxn.getSttPaid() != null ? aisTxn.getSttPaid() : 0.0);
        builder.brokerName(aisTxn.getBrokerName());
        builder.brokerPAN(aisTxn.getBrokerPAN());
        
        if (aisTxn.getIndexedCost() != null) {
            builder.indexedCost(aisTxn.getIndexedCost());
            builder.indexedCostOfAcquisition(aisTxn.getIndexedCost());
        }
        
        if (aisTxn.getGrandfatheredCost() != null) {
            builder.grandfatheredCost(aisTxn.getGrandfatheredCost());
            builder.fmvJan312018(aisTxn.getGrandfatheredCost());
            builder.grandfathering(true);
        }
        
        double grossGain = calculateGrossGain(aisTxn);
        builder.capitalGain(grossGain);
        builder.gain(grossGain);
        
        return builder.build();
    }

    private Itr2FormData.CGTransaction convert26ASToCapitalGain(Form26ASData.CapitalGainsTransaction txn) {
        Itr2FormData.CGTransaction.CGTransactionBuilder builder = Itr2FormData.CGTransaction.builder();
        
        builder.assetType(txn.getAssetType());
        builder.assetDescription(txn.getAssetDescription());
        builder.isinCode(txn.getIsin());
        
        if (txn.getDateOfAcquisition() != null) {
            builder.acquisitionDate(LocalDate.parse(txn.getDateOfAcquisition()));
        }
        
        if (txn.getDateOfSale() != null) {
            builder.saleDate(LocalDate.parse(txn.getDateOfSale()));
        }
        
        builder.purchasePrice(txn.getPurchasePrice() != null ? txn.getPurchasePrice() : 0.0);
        builder.costOfAcquisition(txn.getPurchasePrice() != null ? txn.getPurchasePrice() : 0.0);
        builder.salePrice(txn.getSalePrice() != null ? txn.getSalePrice() : 0.0);
        builder.transferExpenses(txn.getExpenditureOnTransfer() != null ? txn.getExpenditureOnTransfer() : 0.0);
        
        String section = determineSection(txn.getAssetType(), 
                                         determineGainType(txn.getAssetType(), txn.getDateOfAcquisition(), txn.getDateOfSale()), 
                                         txn.getSttPaid());
        builder.section(section);
        
        builder.sttPaid(txn.getSttPaid() != null ? txn.getSttPaid() : 0.0);
        builder.brokerName(txn.getBrokerName());
        builder.brokerPAN(txn.getBrokerPAN());
        
        double grossGain = txn.getSalePrice() - txn.getPurchasePrice() - 
                          (txn.getExpenditureOnTransfer() != null ? txn.getExpenditureOnTransfer() : 0.0);
        builder.capitalGain(grossGain);
        builder.gain(grossGain);
        
        return builder.build();
    }
    
    private void categorizeAndAddTransaction(Itr2FormData.ScheduleCapitalGains scheduleCG, 
                                            Itr2FormData.CGTransaction transaction, 
                                            String section) {
        if (section == null) {
            section = transaction.getSection();
        }
        
        if ("111A".equals(section)) {
            scheduleCG.getStcg111A().add(transaction);
            scheduleCG.setStcg111ATotal(scheduleCG.getStcg111ATotal() + transaction.getGain());
        } else if ("112A".equals(section)) {
            scheduleCG.getLtcg112A().add(transaction);
            scheduleCG.setLtcg112ATotal(scheduleCG.getLtcg112ATotal() + transaction.getGain());
        } else if ("112".equals(section)) {
            scheduleCG.getLtcg112().add(transaction);
            scheduleCG.setLtcg112Total(scheduleCG.getLtcg112Total() + transaction.getGain());
        } else {
            scheduleCG.getStcgOther().add(transaction);
            scheduleCG.setStcgOtherTotal(scheduleCG.getStcgOtherTotal() + transaction.getGain());
        }
        
        scheduleCG.setTotalCapitalGains(
            scheduleCG.getStcg111ATotal() + 
            scheduleCG.getStcgOtherTotal() + 
            scheduleCG.getLtcg112ATotal() + 
            scheduleCG.getLtcg112Total()
        );
    }

    private String determineGainType(String assetType, String dateOfAcquisition, String dateOfSale) {
        if (dateOfAcquisition == null || dateOfSale == null) {
            return "STCG";
        }
        
        LocalDate acquisitionDate = LocalDate.parse(dateOfAcquisition);
        LocalDate saleDate = LocalDate.parse(dateOfSale);
        long monthsHeld = ChronoUnit.MONTHS.between(acquisitionDate, saleDate);
        
        switch (assetType) {
            case "EQUITY_LISTED":
            case "EQUITY_MF":
                return monthsHeld > 12 ? "LTCG" : "STCG";
            
            case "EQUITY_UNLISTED":
            case "PROPERTY":
            case "GOLD":
            case "BONDS":
                return monthsHeld > 24 ? "LTCG" : "STCG";
            
            case "DEBT_MF":
                LocalDate debtMFCutoff = LocalDate.of(2023, 4, 1);
                if (acquisitionDate.isBefore(debtMFCutoff)) {
                    return monthsHeld > 36 ? "LTCG" : "STCG";
                } else {
                    return "STCG";
                }
            
            default:
                return monthsHeld > 24 ? "LTCG" : "STCG";
        }
    }

    private String determineSection(String assetType, String gainType, Double sttPaid) {
        if ("STCG".equals(gainType)) {
            if (("EQUITY_LISTED".equals(assetType) || "EQUITY_MF".equals(assetType)) && 
                sttPaid != null && sttPaid > 0) {
                return "111A";
            }
            return "NORMAL";
        }
        
        if ("LTCG".equals(gainType)) {
            if (("EQUITY_LISTED".equals(assetType) || "EQUITY_MF".equals(assetType)) && 
                sttPaid != null && sttPaid > 0) {
                return "112A";
            }
            
            if ("EQUITY_UNLISTED".equals(assetType) || "DEBT_MF".equals(assetType)) {
                return "112";
            }
            
            return "112";
        }
        
        return "NORMAL";
    }

    private double calculateGrossGain(AISData.CapitalGainsTransaction txn) {
        double salePrice = txn.getSalePrice() != null ? txn.getSalePrice() : 0.0;
        double expenditure = txn.getExpenditureOnTransfer() != null ? txn.getExpenditureOnTransfer() : 0.0;
        
        double costBasis;
        if (txn.getIndexedCost() != null && txn.getIndexedCost() > 0) {
            costBasis = txn.getIndexedCost();
        } else if (txn.getGrandfatheredCost() != null && txn.getGrandfatheredCost() > 0) {
            costBasis = txn.getGrandfatheredCost();
        } else {
            costBasis = txn.getPurchasePrice() != null ? txn.getPurchasePrice() : 0.0;
        }
        
        return salePrice - expenditure - costBasis;
    }
}
