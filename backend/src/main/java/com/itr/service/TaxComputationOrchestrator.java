package com.itr.service;

import com.itr.domain.common.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * TaxComputationOrchestrator — computes tax from form data
 */
@Slf4j
@Service
public class TaxComputationOrchestrator {

    public TaxComputationResult computeTax(Long clientId, String assessmentYear, TaxRegime regime) {
        return TaxComputationResult.builder()
            .grossTotalIncome(0).netTaxableIncome(0).totalTaxLiability(0).taxRegime(regime).build();
    }

    public TaxComputationResult computeTax(Map<String, Object> formData, TaxRegime regime) {
        log.info("Computing tax from form data, regime: {}", regime);
        
        try {
            // Debug: Log incoming data for OTHER SOURCES
            log.info("=== OTHER SOURCES DEBUG ===");
            log.info("dividendEntries in formData: {}", formData.get("dividendEntries") instanceof List ? "Yes (" + ((List<?>)formData.get("dividendEntries")).size() + ")" : "No/null");
            log.info("bankInterestEntries in formData: {}", formData.get("bankInterestEntries") instanceof List ? "Yes (" + ((List<?>)formData.get("bankInterestEntries")).size() + ")" : "No/null");
            log.info("winningsEntries in formData: {}", formData.get("winningsEntries") instanceof List ? "Yes (" + ((List<?>)formData.get("winningsEntries")).size() + ")" : "No/null");
            log.info("imported26AS in formData: {}", formData.get("imported26AS") != null ? "Yes" : "No");
            
            // ==================== CHECK FOR IMPORTED 26AS DATA FIRST ====================
            Map<String, Object> imported26AS = getMap(formData, "imported26AS");
            Map<String, Object> incomeBreakdown26AS = getMap(formData, "incomeBreakdown26AS");
            Map<String, Object> incomeByHead = getMap(formData, "incomeByHead");
            
            long tdsFrom26AS = 0;
            long basic = 0; // Will be set from 26AS if available
            
            if (imported26AS != null && !imported26AS.isEmpty()) {
                log.info("Found imported26AS - extracting data");
                log.info("  imported26AS keys: {}", imported26AS.keySet());
                
                // Extract TDS
                tdsFrom26AS = getLong(imported26AS, "totalTDS");
                if (tdsFrom26AS == 0) tdsFrom26AS = getLong(imported26AS, "totalTds");
                
                // Extract income from incomeBreakdown field inside imported26AS
                Map<String, Object> ib = getMap(imported26AS, "incomeBreakdown");
                if (ib != null && !ib.isEmpty()) {
                    log.info("  incomeBreakdown keys: {}", ib.keySet());
                    long sal = getLong(ib, "salaryIncome");
                    if (sal == 0) sal = getLong(ib, "SALARY");
                    if (sal > 0) { basic = sal; log.info("  Set basic from incomeBreakdown: {}", basic); }
                }
            }
            
            // Also check top-level incomeBreakdown26AS from formData
            if (incomeBreakdown26AS != null && !incomeBreakdown26AS.isEmpty()) {
                log.info("Found incomeBreakdown26AS keys: {}", incomeBreakdown26AS.keySet());
                long sal = getLong(incomeBreakdown26AS, "salaryIncome");
                if (sal == 0) sal = getLong(incomeBreakdown26AS, "SALARY");
                if (sal > 0) { basic = sal; log.info("  Set basic from incomeBreakdown26AS: {}", basic); }
            }
            
            // Check incomeByHead (SALARY, HOUSE_PROPERTY, etc.)
            if (incomeByHead != null && !incomeByHead.isEmpty()) {
                log.info("Found incomeByHead keys: {}", incomeByHead.keySet());
                long sal = getLong(incomeByHead, "SALARY");
                if (sal > 0) { basic = sal; log.info("  Set basic from incomeByHead: {}", basic); }
            }

            // ==================== SALARY INCOME ====================
            // PRIORITY 1: Check employerEntries (user entered in UI) - HIGHEST PRIORITY
            Object employerEntriesObj = formData.get("employerEntries");
            boolean hasUserSalary = false;
            long da = 0, bonus = 0, hra = 0, allowances = 0;
            long tdsFromUser = 0;
            if (employerEntriesObj instanceof List && !((List<?>) employerEntriesObj).isEmpty()) {
                log.info("Found employerEntries - using user-entered salary data");
                long empBasic = 0, empDA = 0, empBonus = 0, empHRA = 0, empAllow = 0, empTDS = 0;
                for (Object empObj : (List<?>) employerEntriesObj) {
                    if (empObj instanceof Map) {
                        Map<String, Object> emp = (Map<String, Object>) empObj;
                        empBasic += getLong(emp, "basic");
                        empDA += getLong(emp, "da");
                        empBonus += getLong(emp, "bonus");
                        empHRA += getLong(emp, "hra");
                        empAllow += getLong(emp, "allowances");
                        empTDS += getLong(emp, "tdsDeducted");
                    }
                }
                if (empBasic > 0) { basic = empBasic; hasUserSalary = true; log.info("Using basic from employerEntries: {}", basic); }
                if (empDA > 0) da = empDA;
                if (empBonus > 0) bonus = empBonus;
                if (empHRA > 0) hra = empHRA;
                if (empAllow > 0) allowances = empAllow;
                if (empTDS > 0) tdsFromUser = empTDS;
                log.info("Using TDS from employerEntries: {}", tdsFromUser);
            }
            
            // PRIORITY 2: Top-level formData fields (single employer)
            if (!hasUserSalary) {
                long formBasic = getLong(formData, "basic");
                if (formBasic > 0) { basic = formBasic; log.info("Using basic from formData: {}", basic); }
                da = getLong(formData, "da");
                bonus = getLong(formData, "bonus");
                hra = getLong(formData, "hraReceived") + getLong(formData, "hra");
                allowances = getLong(formData, "allowances");
                if (basic > 0) hasUserSalary = true;
            }
            
            // PRIORITY 3: 26AS imports - only if user hasn't entered data
            if (!hasUserSalary && basic == 0) {
                log.info("No user salary found - using 26AS data");
            } else if (hasUserSalary) {
                log.info("User salary data found, ignoring 26AS");
            }
            
            // Now initialize all salary components - but keep existing values if user entered
            long commission = getLong(formData, "commission");
            long perquisites = getLong(formData, "perquisites");
            long lta = getLong(formData, "ltaReceived");
            long otherAllow = getLong(formData, "otherAllowance");
            long profitsInLieu = getLong(formData, "profitsInLieu");
            
            // For values that weren't already set from employerEntries, get from formData
            if (da == 0) da = getLong(formData, "da");
            if (bonus == 0) bonus = getLong(formData, "bonus");
            if (hra == 0) hra = getLong(formData, "hraReceived") + getLong(formData, "hra");
            if (allowances == 0) allowances = getLong(formData, "allowances");

            // ==================== SECTION 10 EXEMPTIONS (OLD REGIME) ====================
            // Calculate exemptions for OLD regime only
            long hraExempt = 0;
            long transportExempt = 0;
            long childrenEducationExempt = 0;
            long hostelExempt = 0;
            long ltaExempt = 0;
            long gratuityExempt = 0;
            long leaveEncashmentExempt = 0;
            long standardDeductionExempt = 0;
            long professionalTaxExempt = 0;
            long entertainmentExempt = 0;
            long otherExempt = 0;
            long vrsExempt = 0;
            long retrenchmentExempt = 0;
            long commutedPensionExempt = 0;
            long voluntaryRetirementExempt = 0;
            long totalExemptions = 0;

            if (regime == TaxRegime.OLD) {
                // HRA Exemption u/s 10(13A): min of (HRA received, 50%/40% of basic+da, rent paid - 10% of basic+da)
                long basicDA = basic + da;
                long rentPaid = getLong(formData, "hraRent");
                if (rentPaid == 0) rentPaid = getLong(formData, "rentPaid");
                boolean isMetro = Boolean.TRUE.equals(formData.get("hraMetro"));
                if (isMetro == false && formData.get("isMetroCity") != null) {
                    isMetro = Boolean.TRUE.equals(formData.get("isMetroCity"));
                }
                long percentOfBasic = isMetro ? basicDA * 50 / 100 : basicDA * 40 / 100;
                long rentMinusTenPercent = Math.max(0, rentPaid - basicDA * 10 / 100);
                hraExempt = Math.min(hra, Math.min(percentOfBasic, rentMinusTenPercent));
                if (hraExempt < 0) hraExempt = 0;

                // Transport Allowance Exemption u/s 10(14): ₹1,600/month = ₹19,200/year
                long transportAllowance = getLong(formData, "transportAllowanceReceived");
                if (transportAllowance > 0) {
                    transportExempt = Math.min(transportAllowance, 19200);
                }

                // Children Education Allowance u/s 10(14): ₹100/month/child = ₹1,200/year/child (max 2)
                long cea = getLong(formData, "ceaReceived");
                if (cea > 0) {
                    childrenEducationExempt = Math.min(cea, 2400); // 2 children max
                }

                // Hostel Expenditure Allowance u/s 10(14): ₹300/month/child = ₹3,600/year/child (max 2)
                long hostel = getLong(formData, "hostelAllowanceReceived");
                if (hostel > 0) {
                    hostelExempt = Math.min(hostel, 7200); // 2 children max
                }

                // LTA Exemption u/s 10(5): actual travel cost (simplified - assume LTA received if claimed)
                long ltaClaimed = getLong(formData, "ltaExempt");
                if (ltaClaimed > 0) {
                    ltaExempt = Math.min(lta, ltaClaimed);
                }

                // Gratuity Exemption u/s 10(10): min of (received, ₹20,00,000)
                long gratuity = getLong(formData, "gratuityReceived");
                if (gratuity > 0) {
                    gratuityExempt = Math.min(gratuity, 2000000);
                }

                // Leave Encashment Exemption u/s 10(10AA): min of (received, ₹25,00,000)
                long leaveEncashment = getLong(formData, "leaveEncashmentReceived");
                if (leaveEncashment > 0) {
                    leaveEncashmentExempt = Math.min(leaveEncashment, 2500000);
                }

                // Standard Deduction u/s 16(ia): ₹50,000 for OLD regime
                standardDeductionExempt = 50000;

                // Professional Tax u/s 16(iii): max ₹2,500
                long profTaxPaid = getLong(formData, "profTax");
                if (profTaxPaid > 0) {
                    professionalTaxExempt = Math.min(profTaxPaid, 2500);
                }

                // Entertainment Allowance u/s 16(ii): ₹5,000 (only for govt employees)
                boolean isGovt = Boolean.TRUE.equals(formData.get("isGovernmentEmployee"));
                if (isGovt) {
                    long entAllow = getLong(formData, "entertainmentAllowance");
                    if (entAllow > 0) {
                        entertainmentExempt = Math.min(entAllow, 5000);
                    }
                }

                // VRS Compensation Exemption u/s 10(10C): max ₹5,00,000
                long vrs = getLong(formData, "vrsCompensation");
                if (vrs > 0) {
                    vrsExempt = Math.min(vrs, 500000);
                }

                // Retrenchment Compensation u/s 10(10B): max ₹5,00,000
                long retrench = getLong(formData, "retrenchmentCompensation");
                if (retrench > 0) {
                    retrenchmentExempt = Math.min(retrench, 500000);
                }

                // Commuted Pension u/s 10(10A): 1/3 for non-govt, 1/2 for govt
                long commutedPension = getLong(formData, "commutationOfPensionReceived");
                if (commutedPension > 0) {
                    if (isGovt) {
                        commutedPensionExempt = commutedPension * 50 / 100;
                    } else {
                        commutedPensionExempt = commutedPension * 33 / 100;
                    }
                }

                // Voluntary Retirement u/s 10(10C) - already covered in VRS

                // Other Exemptions (Schedule EI)
                otherExempt = getLong(formData, "otherExempt");

                totalExemptions = hraExempt + transportExempt + childrenEducationExempt +
                                  hostelExempt + ltaExempt + gratuityExempt + leaveEncashmentExempt +
                                  standardDeductionExempt + professionalTaxExempt + entertainmentExempt +
                                  vrsExempt + retrenchmentExempt + commutedPensionExempt + otherExempt;

                log.info("=== EXEMPTIONS (OLD REGIME): HRA={}, Transport={}, CEA={}, Hostel={}, LTA={}, Gratuity={}, Leave={}, StdDed={}, ProfTax={}, Ent={}, VRS={}, Retrench={}, CommutedPen={}, Other={}, Total={}",
                    hraExempt, transportExempt, childrenEducationExempt, hostelExempt,
                    ltaExempt, gratuityExempt, leaveEncashmentExempt, standardDeductionExempt,
                    professionalTaxExempt, entertainmentExempt, vrsExempt, retrenchmentExempt,
                    commutedPensionExempt, otherExempt, totalExemptions);
            }

            // Net salary = Gross salary - Exemptions (only for OLD regime)
            long salaryIncome;
            if (regime == TaxRegime.OLD && totalExemptions > 0) {
                salaryIncome = Math.max(0, basic + da + bonus + commission + allowances + perquisites +
                              hra + lta + otherAllow + profitsInLieu - totalExemptions);
                log.info("=== SALARY (OLD): Gross={}, Exemptions={}, NetTaxable={}",
                    basic + da + bonus + commission + allowances + perquisites + hra + lta + otherAllow + profitsInLieu,
                    totalExemptions, salaryIncome);
            } else {
                salaryIncome = basic + da + bonus + commission + allowances + perquisites + hra + lta + otherAllow + profitsInLieu;
            }

            // ==================== HOUSE PROPERTY ====================
            long hpIncome = getLong(formData, "hpIncome");
            long homeLoanInt = getLong(formData, "homeLoanInt");
            hpIncome = hpIncome - homeLoanInt; // Deduction u/s 24

            // ==================== CAPITAL GAINS ====================
            long stcgIncome = getLong(formData, "stcgIncome");
            long ltcgIncome = getLong(formData, "ltcgIncome");

            // ==================== BUSINESS INCOME ====================
            long businessIncome = getLong(formData, "businessIncome");

            // ==================== OTHER SOURCES - INTEREST ====================
            long interestSB = getLong(formData, "interestSB");
            long interestFD = getLong(formData, "interestFD");
            long nsc = getLong(formData, "nscInterest");
            long scss = getLong(formData, "scssInterest");
            long totalInterest = interestSB + interestFD + nsc + scss;

            // ==================== OTHER SOURCES - DIVIDEND ====================
            // First check dividendEntries array (from 26AS import) - this takes priority
            long dividend = 0;
            Object dividendEntries = formData.get("dividendEntries");
            if (dividendEntries instanceof List) {
                for (Object obj : (List<?>) dividendEntries) {
                    if (obj instanceof Map) {
                        Map<String, Object> de = (Map<String, Object>) obj;
                        long amt = getLong(de, "dividendAmount");
                        if (amt == 0) amt = getLong(de, "grossAmount");
                        dividend += amt;
                    }
                }
            }
            // If still 0, check flat dividend field
            if (dividend == 0) dividend = getLong(formData, "dividends");
            if (dividend == 0) dividend = getLong(formData, "totalDividend");
            
            log.info("DIVIDEND: dividendEntries sum = {}, flat dividends = {}", 
                (dividendEntries instanceof List ? "present" : "null"), dividend);

            // ==================== OTHER SOURCES - WINNINGS ====================
            long winnings = getLong(formData, "lotteryIncome") + getLong(formData, "horseRaceIncome") + 
                          getLong(formData, "cardGameIncome");
            if (winnings == 0) {
                Object winningsEntries = formData.get("winningsEntries");
                if (winningsEntries instanceof List) {
                    for (Object obj : (List<?>) winningsEntries) {
                        if (obj instanceof Map) winnings += getLong((Map<String, Object>) obj, "grossAmount");
                    }
                }
            }

            // ==================== OTHER SOURCES - INTEREST (from bankInterestEntries) ====================
            if (totalInterest == 0) {
                Object bankInterestEntries = formData.get("bankInterestEntries");
                if (bankInterestEntries instanceof List) {
                    for (Object obj : (List<?>) bankInterestEntries) {
                        if (obj instanceof Map) totalInterest += getLong((Map<String, Object>) obj, "interestAmount");
                    }
                }
            }

            // ==================== OTHER SOURCES - VDA ====================
            long vdaGains = getLong(formData, "vdaGains");

            // ==================== OTHER SOURCES - OTHERS ====================
            long familyPension = getLong(formData, "familyPension");
            long otherMisc = getLong(formData, "otherMisc");
            long taxableGifts = getLong(formData, "taxableGifts");

            long otherSources = totalInterest + dividend + winnings + vdaGains + familyPension + otherMisc + taxableGifts;

            // ==================== GROSS TOTAL INCOME ====================
            long grossTotalIncome = salaryIncome + hpIncome + stcgIncome + ltcgIncome + businessIncome + otherSources;

            // ==================== DEDUCTIONS ====================
            long s80C = getLong(formData, "s80C") + getLong(formData, "s80C_epf") + getLong(formData, "s80C_lic") + 
                      getLong(formData, "s80C_ppf") + getLong(formData, "s80C_home");
            long s80D = getLong(formData, "s80D") + getLong(formData, "s80D_self");
            long s80TTA = getLong(formData, "s80TTA");
            long s80G = getLong(formData, "s80G");
            long totalDeductions = s80C + s80D + s80TTA + s80G;

            // ==================== STANDARD DEDUCTION ====================
            // For OLD regime, std deduction is already included in totalExemptions
            // For NEW regime, apply ₹75,000
            long standardDeduction = (regime == TaxRegime.NEW) ? 75000 : 0;
            long profTax = (regime == TaxRegime.OLD) ? 0 : Math.min(getLong(formData, "profTax"), 2500);

            // ==================== NET TAXABLE ====================
            long netTaxableIncome = Math.max(0, grossTotalIncome - totalDeductions - standardDeduction - profTax);

            // ==================== TAX ====================
            long tax = computeTax(netTaxableIncome, regime);
            long winningsTax = winnings * 30 / 100;
            long vdaTax = vdaGains * 30 / 100;
            long totalTaxBeforeRebate = tax + winningsTax + vdaTax;

            // ==================== REBATE 87A ====================
            long rebate = 0;
            if (regime == TaxRegime.NEW && netTaxableIncome <= 700000) rebate = Math.min(25000, totalTaxBeforeRebate);
            else if (regime == TaxRegime.OLD && netTaxableIncome <= 500000) rebate = Math.min(12500, totalTaxBeforeRebate);

            long taxAfterRebate = Math.max(0, totalTaxBeforeRebate - rebate);

            // ==================== SURCHARGE & CESS ====================
            long surcharge = 0;
            if (netTaxableIncome > 50000000) surcharge = taxAfterRebate * 25 / 100;
            else if (netTaxableIncome > 10000000) surcharge = taxAfterRebate * 15 / 100;
            else if (netTaxableIncome > 5000000) surcharge = taxAfterRebate * 10 / 100;

            long totalTaxLiability = taxAfterRebate + surcharge + (taxAfterRebate + surcharge) * 4 / 100;

            // ==================== TDS ====================
            // Priority: user entered TDS > formData TDS > 26AS TDS
            long totalTds = 0;
            if (tdsFromUser > 0) {
                totalTds = tdsFromUser;
                log.info("Using TDS from employerEntries: {}", totalTds);
            } else {
                totalTds = getLong(formData, "totalTds");
                if (totalTds == 0) totalTds = getLong(formData, "tdsS192");
                if (totalTds == 0 && tdsFrom26AS > 0) totalTds = tdsFrom26AS;
            }

            // ==================== BALANCE ====================
            long advanceTax = getLong(formData, "totalAdvanceTax");
            long selfTax = getLong(formData, "totalSelfAssessmentTax");
            long totalTaxPaid = totalTds + advanceTax + selfTax;
            
            long balanceTaxPayable = Math.max(0, totalTaxLiability - totalTaxPaid);
            long refundAmount = Math.max(0, totalTaxPaid - totalTaxLiability);
            
            log.info("=== INCOME: Salary={}, HP={}, CG={}, Business={}, Other={}, Gross={}", 
                salaryIncome, hpIncome, stcgIncome+ltcgIncome, businessIncome, otherSources, grossTotalIncome);
            log.info("=== TAX: Normal={}, Special={}, Total={}, TDS={}, Balance={}", 
                tax, winningsTax+vdaTax, totalTaxLiability, totalTds, balanceTaxPayable);
            
            return TaxComputationResult.builder()
                .grossTotalIncome(grossTotalIncome)
                .netTaxableIncome(netTaxableIncome)
                .totalTaxLiability(totalTaxLiability)
                .balanceTaxPayable(balanceTaxPayable)
                .refundAmount(refundAmount)
                .tdsAmount(totalTds)
                .taxRegime(regime)
                .build();
                
        } catch (Exception e) {
            log.error("Tax computation error", e);
            return TaxComputationResult.builder()
                .grossTotalIncome(0).netTaxableIncome(0).totalTaxLiability(0).taxRegime(regime).build();
        }
    }
    
    private long computeTax(long income, TaxRegime regime) {
        if (income <= 0) return 0;
        if (regime == TaxRegime.NEW) {
            if (income <= 400000) return 0;
            else if (income <= 800000) return (income - 400000) * 10 / 100;
            else if (income <= 1200000) return 40000 + (income - 800000) * 15 / 100;
            else if (income <= 1600000) return 100000 + (income - 1200000) * 20 / 100;
            else return 180000 + (income - 1600000) * 30 / 100;
        } else {
            if (income <= 250000) return 0;
            else if (income <= 500000) return (income - 250000) * 10 / 100;
            else if (income <= 1000000) return 25000 + (income - 500000) * 20 / 100;
            else return 125000 + (income - 1000000) * 30 / 100;
        }
    }
    
    private long getLong(Map<String, Object> map, String key) {
        try {
            Object value = map.get(key);
            if (value == null) return 0;
            if (value instanceof Number) return ((Number) value).longValue();
            try {
                double d = Double.parseDouble(value.toString().replaceAll("[^0-9.-]", ""));
                return (long) d;
            } catch (NumberFormatException e) { return 0; }
        } catch (Exception e) { return 0; }
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(Map<String, Object> map, String key) {
        try {
            Object value = map.get(key);
            if (value instanceof Map) return (Map<String, Object>) value;
            return null;
        } catch (Exception e) { return null; }
    }
}
