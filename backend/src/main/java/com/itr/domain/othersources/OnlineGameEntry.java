package com.itr.domain.othersources;

import java.math.BigDecimal;

/**
 * OnlineGameEntry — Section 115BBJ winnings entry.
 * Net Winnings = Total Withdrawal − (Total Deposit + Opening Balance)
 */
public class OnlineGameEntry {
    private String platformName;
    private BigDecimal totalWithdrawal;     // Total withdrawals in FY
    private BigDecimal totalDeposit;        // Total deposits in FY
    private BigDecimal openingBalance;      // Balance at start of FY
    private BigDecimal tdsU194BA;           // TDS u/s 194BA (Form 26AS/AIS)

    public OnlineGameEntry() {}

    public OnlineGameEntry(String platformName, BigDecimal totalWithdrawal, BigDecimal totalDeposit,
                           BigDecimal openingBalance, BigDecimal tdsU194BA) {
        this.platformName = platformName;
        this.totalWithdrawal = totalWithdrawal;
        this.totalDeposit = totalDeposit;
        this.openingBalance = openingBalance;
        this.tdsU194BA = tdsU194BA;
    }

    public String getPlatformName() { return platformName; }
    public void setPlatformName(String platformName) { this.platformName = platformName; }
    public BigDecimal getTotalWithdrawal() { return totalWithdrawal; }
    public void setTotalWithdrawal(BigDecimal totalWithdrawal) { this.totalWithdrawal = totalWithdrawal; }
    public BigDecimal getTotalDeposit() { return totalDeposit; }
    public void setTotalDeposit(BigDecimal totalDeposit) { this.totalDeposit = totalDeposit; }
    public BigDecimal getOpeningBalance() { return openingBalance; }
    public void setOpeningBalance(BigDecimal openingBalance) { this.openingBalance = openingBalance; }
    public BigDecimal getTdsU194BA() { return tdsU194BA; }
    public void setTdsU194BA(BigDecimal tdsU194BA) { this.tdsU194BA = tdsU194BA; }
}
