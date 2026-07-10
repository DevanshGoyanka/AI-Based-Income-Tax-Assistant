"""Test AIS PDF extraction with password."""
import sys
import os

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from app.services.pdf_decrypt import decrypt_and_extract_tables, PDFDecryptionError

def main():
    print("=" * 80)
    print("       AIS PDF EXTRACTION TEST")
    print("=" * 80)
    
    # Get inputs from command line or use defaults
    if len(sys.argv) >= 4:
        pan = sys.argv[1].upper()
        dob = sys.argv[2]  # DDMMYYYY
        file_path = sys.argv[3]
    else:
        pan = "COVPC5929M"
        dob = "08022002"  # DDMMYYYY
        file_path = r"C:\Users\Devansh\Desktop\E-FILE_karo\XXXPC5929X_2025-26_AIS.pdf"
    
    print(f"\nPAN: {pan}")
    print(f"DOB: {dob}")
    print(f"File: {file_path}")
    print(f"Password will be: {pan.lower()}{dob}")
    
    if not os.path.exists(file_path):
        print(f"\n❌ File not found: {file_path}")
        return
    
    # Read PDF
    print("\n📖 Reading PDF...")
    with open(file_path, 'rb') as f:
        pdf_bytes = f.read()
    print(f"   PDF size: {len(pdf_bytes):,} bytes")
    
    # Decrypt and extract
    print("\n🔓 Decrypting and extracting tables...")
    try:
        result = decrypt_and_extract_tables(pdf_bytes, pan, dob, "AIS")
        
        print(f"\n✅ Extraction successful!")
        print(f"   Pages: {result.pages}")
        print(f"   Encrypted: {result.is_encrypted}")
        print(f"   Tables found: {len(result.tables)}")
        
        if result.errors:
            print(f"\n⚠️  Warnings:")
            for err in result.errors:
                print(f"   - {err}")
        
        # Show parsed results
        parsed = result.parsed
        print(f"\n📊 PARSED DATA SUMMARY:")
        print("-" * 80)
        
        # Personal info
        if parsed.get("pan"):
            print(f"\n📋 Personal Info:")
            print(f"   PAN: {parsed.get('pan')}")
            print(f"   Name: {parsed.get('name')}")
            print(f"   DOB: {parsed.get('dob')}")
            print(f"   Aadhaar: {parsed.get('aadhaar')}")
            print(f"   Mobile: {parsed.get('mobile')}")
            print(f"   Email: {parsed.get('email')}")
        
        # TDS Salary
        tds_salary = parsed.get("tds_salary", [])
        if tds_salary:
            print(f"\n💰 TDS on Salary: {len(tds_salary)} records")
            total_amt = sum(r.get('amount_paid', 0) + r.get('amount', 0) for r in tds_salary)
            total_tds = sum(r.get('tds_deducted', 0) for r in tds_salary)
            print(f"   Total Amount: ₹{total_amt:,} | Total TDS: ₹{total_tds:,}")
            for i, r in enumerate(tds_salary[:5], 1):
                print(f"   [{i}] Date: {r.get('date')} | Amount: ₹{r.get('amount_paid', 0) or r.get('amount', 0):,} | TDS: ₹{r.get('tds_deducted', 0):,}")
        else:
            print(f"\n💰 TDS on Salary: No records")
        
        # TDS Others
        tds_others = parsed.get("tds_others", [])
        if tds_others:
            print(f"\n💰 TDS on Others: {len(tds_others)} records")
            total_amt = sum(r.get('amount_paid', 0) + r.get('amount', 0) for r in tds_others)
            total_tds = sum(r.get('tds_deducted', 0) for r in tds_others)
            print(f"   Total Amount: ₹{total_amt:,} | Total TDS: ₹{total_tds:,}")
            for i, r in enumerate(tds_others[:5], 1):
                print(f"   [{i}] Date: {r.get('date')} | Amount: ₹{r.get('amount_paid', 0) or r.get('amount', 0):,} | TDS: ₹{r.get('tds_deducted', 0):,}")
        else:
            print(f"\n💰 TDS on Others: No records")
        
        # SFT Dividend
        sft_div = parsed.get("sft_dividend", [])
        if sft_div:
            print(f"\n📈 SFT Dividend: {len(sft_div)} records")
            for i, r in enumerate(sft_div[:3], 1):
                print(f"   [{i}] Date: {r.get('date')} | Amount: ₹{r.get('amount', 0):,} | Source: {r.get('source', 'N/A')[:50]}")
        
        # SFT Mutual Fund
        sft_mf = parsed.get("sft_mutual_fund", [])
        if sft_mf:
            print(f"\n💹 SFT Mutual Fund: {len(sft_mf)} records")
            total_amt = sum(r.get('purchase_amount', 0) or r.get('amount_paid', 0) for r in sft_mf)
            print(f"   Total Purchase: ₹{total_amt:,}")
            for i, r in enumerate(sft_mf[:3], 1):
                src = r.get('source', 'N/A')
                amt = r.get('purchase_amount', 0) or r.get('amount_paid', 0)
                print(f"   [{i}] Amount: ₹{amt:,} | Source: {src[:50] if src else 'N/A'}")
        
        # Tax Payments
        tax_payments = parsed.get("tax_payments", [])
        if tax_payments:
            print(f"\n🏛️ Tax Payments: {len(tax_payments)} records")
            for i, r in enumerate(tax_payments, 1):
                print(f"   [{i}] FY: {r.get('fy')} | Tax: ₹{r.get('tax', 0):,} | Date: {r.get('date')} | Challan: {r.get('challan_no')}")
        
        # Show first few raw tables
        print(f"\n📑 RAW TABLES (first 5):")
        print("-" * 80)
        for i, table in enumerate(result.tables[:5], 1):
            print(f"\nTable {i} (Page {table.get('page', '?')}):")
            print(f"   Headers: {table.get('headers', [])[:6]}")
            print(f"   Rows: {len(table.get('data', []))}")
            if table.get('data'):
                print(f"   First row: {table['data'][0][:6] if table['data'] else []}")
        
    except PDFDecryptionError as e:
        print(f"\n❌ Decryption failed: {e}")
    except Exception as e:
        print(f"\n❌ Error: {e}")
        import traceback
        traceback.print_exc()


if __name__ == "__main__":
    main()
