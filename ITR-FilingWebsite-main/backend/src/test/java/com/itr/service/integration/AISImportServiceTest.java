package com.itr.service.integration;

import com.itr.dto.AISData;
import com.itr.util.ITDPdfDecryptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AISImportServiceTest {

    @Mock
    private ITDPdfDecryptor pdfDecryptor;

    @InjectMocks
    private AISImportService aisImportService;

    @Test
    void testParseAIS_WithSampleData() throws Exception {
        // Sample AIS text from the provided PDF
        String sampleAISText = """
            Part A - General Information
            Permanent Account Number (PAN) Aadhaar Number Name of Assessee
            ACUPG3482G XXXX XXXX 0929 SUNIT RAMASHANKAR GOYANKA
            Date of Birth Mobile Number E-mail Address
            14/06/1974 9422772675 sunitgoyanka@gmail.com
            
            Part B1-Information relating to tax deducted or collected at source
            Interest from deposit
            SR. NO. INFORMATION CODE INFORMATION DESCRIPTION INFORMATION SOURCE COUNT AMOUNT
            1 TDS-194A Interest other than "Interest on Securities" received 
            (Section 194A)
            ANAND PURUSHOTTAM AGRAWAL (NGPA14339D) 2 45,000
            SR. NO. QUARTER DATE OF PAYMENT/CREDIT AMOUNT PAID/CREDITED TDS DEDUCTED TDS DEPOSITED STATUS
            1 Q3(Oct-Dec) 31/12/2025 15,000 1,500 1,500 Active
            2 Q2(Jul-Sep) 30/09/2025 30,000 3,000 3,000 Active
            
            SR. NO. INFORMATION CODE INFORMATION DESCRIPTION INFORMATION SOURCE COUNT AMOUNT
            2 TDS-194A Interest other than "Interest on Securities" received 
            (Section 194A)
            STATE BANK OF INDIA (MUMS89569E) 1 1,410
            SR. NO. QUARTER DATE OF PAYMENT/CREDIT AMOUNT PAID/CREDITED TDS DEDUCTED TDS DEPOSITED STATUS
            1 Q3(Oct-Dec) 08/10/2025 1,410 141 141 Active
            
            Part B2-Information relating to specified financial transaction (SFT)
            Dividend
            SR. NO. INFORMATION CODE INFORMATION DESCRIPTION INFORMATION SOURCE COUNT AMOUNT
            1 SFT-015 Dividend income (SFT-015) LIFE INSURANCE CORPORATION OF INDIA (AAACL0582H.AI497) 1 180
            SR. NO. REPORTED ON DIVIDEND AMOUNT STATUS
            1 08/04/2026 180 Active
            
            Sale of securities and units of mutual fund
            SR. NO. INFORMATION CODE INFORMATION DESCRIPTION INFORMATION SOURCE COUNT AMOUNT
            2 SFT-17-LES(M) Sale of listed equity share (Depository) CENTRAL DEPOSITORY SERVICES(I) LIMITED 
            (AAACC6233AMUMC09975A)
            1 1,523.00
            SR. NO. DATE OF SALE SECURITY NAME (SECURITY CODE) SECURITY CLASS DEBIT TYPE CREDIT TYPE ASSET TYPE QUANTITY SALE PRICE PER UNIT SALES CONSIDERATION COST OF ACQUISITION UNIT FMV FAIR MARKET VALUE INDEXED COST OF ACQUISITION STATUS
            1 07/07/2025 RELIANCE INDUSTRIES LIMITED EQUITY SHARES(INE002A01018) Listed Equity Share Market Market Short term 1.00 1,523.45 1,523 1,517.00 961.15 961.15 0 Active
            
            Purchase of securities and units of mutual funds
            SR. NO. INFORMATION CODE INFORMATION DESCRIPTION INFORMATION SOURCE COUNT AMOUNT
            3 SFT-17(Pur) Purchase of securities (SFT - 017) CENTRAL DEPOSITORY SERVICES(I) LIMITED 
            (AAACC6233AMUMC09975A)
            1 1,517
            SR. NO. QUARTER CLIENT ID HOLDER FLAG MARKET PURCHASE MARKET SALES STATUS
            1-85102941 First 1,517 1,523 Active
            
            SR. NO. INFORMATION CODE INFORMATION DESCRIPTION INFORMATION SOURCE COUNT AMOUNT
            4 SFT-18(Pur) Purchase of mutual funds (SFT - 018) Computer Age Management Services Limited - ICICI Prudential Mutual Fund(P) (AAACC3035G.AZ670) 1 72,996
            SR. NO. QUARTER CLIENT ID AMC NAME (CODE) HOLDER FLAG TOTAL PURCHASE AMOUNT TOTAL SALES VALUE STATUS
            1 Q2(Jul-Sep) 5635362 ICICI Prudential Mutual Fund(P) First 72,996 0 Active
            
            SR. NO. INFORMATION CODE INFORMATION DESCRIPTION INFORMATION SOURCE COUNT AMOUNT
            5 SFT-18(Pur) Purchase of mutual funds (SFT - 018) Computer Age Management Services Limited - HDFC Asset Management Company Limited(H) (AAACC3035G.AZ670) 1 37,998
            SR. NO. QUARTER CLIENT ID AMC NAME (CODE) HOLDER FLAG TOTAL PURCHASE AMOUNT TOTAL SALES VALUE STATUS
            1 Q2(Jul-Sep) 7698933 HDFC Asset Management Company Limited(H) First 37,998 0 Active
            
            Part B3-Information relating to payment of taxes
            SR. NO. FINANCIAL YEAR MAJOR HEAD MINOR HEAD TAX (A) SURCHARGE (B) EDUCATION CESS (C) OTHERS (D) TOTAL (A+B+C+D) BSR CODE DATE OF DEPOSIT CHALLAN SERIAL NUMBER CHALLAN IDENTIFICATION NUMBER
            1 2024-25 Income Tax (Other than Companies) Self Assessment 37,772 0 1,511 0 39,283 0002271 31/08/2025 34945 25083100181019SBIN
            """;

        when(pdfDecryptor.decryptAndExtractText(any(), any(), any())).thenReturn(sampleAISText);

        // Execute
        AISData result = aisImportService.importAIS(new byte[0], "ACUPG3482G", LocalDate.of(1974, 6, 14));

        // Verify Part A
        assertNotNull(result.getGeneralInfo());
        assertEquals("ACUPG3482G", result.getGeneralInfo().getPan());
        assertEquals("9422772675", result.getGeneralInfo().getMobile());
        assertEquals("sunitgoyanka@gmail.com", result.getGeneralInfo().getEmail());

        // Verify Part B1 - TDS entries
        assertNotNull(result.getPartB1());
        assertEquals(2, result.getPartB1().getTdsEntries().size());
        
        AISData.AISTDSEntry entry1 = result.getPartB1().getTdsEntries().get(0);
        assertEquals("194A", entry1.getSection());
        assertEquals("ANAND PURUSHOTTAM AGRAWAL", entry1.getDeductorName());
        assertEquals("NGPA14339D", entry1.getDeductorTAN());
        assertEquals(45000, entry1.getTotalAmountPaid());
        assertEquals(4500, entry1.getTotalTDSDeducted());

        AISData.AISTDSEntry entry2 = result.getPartB1().getTdsEntries().get(1);
        assertEquals("194A", entry2.getSection());
        assertEquals("STATE BANK OF INDIA", entry2.getDeductorName());
        assertEquals("MUMS89569E", entry2.getDeductorTAN());
        assertEquals(1410, entry2.getTotalAmountPaid());
        assertEquals(141, entry2.getTotalTDSDeducted());

        // Verify Part B2 - SFT data
        assertNotNull(result.getPartB2());
        assertEquals(180, result.getPartB2().getDividendIncome());
        assertEquals(1517, result.getPartB2().getSecuritiesPurchaseAmount());
        
        // Securities sale
        assertEquals(1, result.getPartB2().getSecuritiesSale().size());
        AISData.SFTSaleEntry sale = result.getPartB2().getSecuritiesSale().get(0);
        assertEquals("STCG", sale.getAssetType());
        assertEquals(1523, sale.getSalesConsideration().longValue());
        assertEquals(1517, sale.getCostOfAcquisition().longValue());

        // Mutual funds
        assertEquals(2, result.getPartB2().getMutualFundPurchase().size());
        assertTrue(result.getPartB2().getMutualFundPurchase().stream()
            .anyMatch(mf -> mf.getAmcName().contains("ICICI") && mf.getTotalPurchase() == 72996));
        assertTrue(result.getPartB2().getMutualFundPurchase().stream()
            .anyMatch(mf -> mf.getAmcName().contains("HDFC") && mf.getTotalPurchase() == 37998));

        // Verify Part B3 - Tax payments
        assertNotNull(result.getPartB3());
        assertEquals(1, result.getPartB3().size());
        
        AISData.TaxPaymentAIS payment = result.getPartB3().get(0);
        assertEquals("2024-25", payment.getFinancialYear());
        assertEquals("Self Assessment", payment.getMinorHead());
        assertEquals(37772, payment.getTaxAmount());
        assertEquals(1511, payment.getEducationCess());
        assertEquals(39283, payment.getTotalAmount());
        assertEquals("0002271", payment.getBsrCode());
        assertEquals("34945", payment.getChallanSerialNo());
    }
}
