package com.itr.domain.salary;

import java.util.List;

/**
 * EmployerEntry — immutable per-employer salary data.
 */
public record EmployerEntry(
    String employerName,
    String tan,
    long grossSalary,
    long basicDA,
    long hraReceived,
    long ltaReceived,
    long specialAllowances,
    long bonus,
    long gratuityReceived,
    long leaveEncashmentReceived,
    long employerPFContribution,
    long employerNPSContribution,
    long perksValue,
    long professionalTax,
    long tdsDeducted
) {
    public long getTotalAllowances() {
        return hraReceived + ltaReceived + specialAllowances + bonus;
    }
}
