"""Comprehensive AIS Test Script - PART-wise summary with detailed entries."""
import json
import sys
import os

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from app.services.ais_decrypt import decrypt_ais
from app.services.ais_parser import parse_ais_json, TaxPaymentRecord, DemandRefundRecord, OtherInfoRecord


def format_currency(amount: int) -> str:
    return f"₹{amount:,}"


def print_summary_section(result):
    """Print comprehensive PART-wise summary."""
    print("=" * 80)
    print("                    AIS COMPREHENSIVE SUMMARY")
    print("=" * 80)
    
    # Personal Info
    if result.personal_info:
        print(f"\n📋 PART A - PERSONAL INFORMATION")
        print("-" * 80)
        print(f"   Name: {result.personal_info.get('name', 'N/A')}")
        print(f"   PAN: {result.personal_info.get('pan', 'N/A')}")
        print(f"   DOB: {result.personal_info.get('dob', 'N/A')}")
        print(f"   Aadhaar: {result.personal_info.get('aadhaar_masked', 'N/A')}")
        print(f"   Mobile: {result.personal_info.get('mobile', 'N/A')}")
        print(f"   Email: {result.personal_info.get('email', 'N/A')}")
    
    # Part B1 - TDS/TCS Summary
    print(f"\n💰 PART B1 - TDS/TCS SUMMARY")
    print("-" * 80)
    print(f"   Total Records: {len(result.tds_records)}")
    print(f"   Total Amount: {format_currency(result.total_tds_amount)}")
    print(f"   Total TDS Tax: {format_currency(result.total_tds_tax)}")
    print(f"   Unique Deductors: {result.deductor_count}")
    
    # Group by section
    by_section = {}
    for tds in result.tds_records:
        by_section.setdefault(tds.section_code, []).append(tds)
    
    if by_section:
        print(f"\n   By Section:")
        for sec, records in sorted(by_section.items()):
            sec_amount = sum(r.amount_paid for r in records)
            sec_tax = sum(r.tax_deducted for r in records)
            deduplicators = {r.deductor_tan for r in records}
            print(f"   └─ Section {sec}: {len(records)} entries | Amount: {format_currency(sec_amount)} | TDS: {format_currency(sec_tax)} | Deductors: {len(deduplicators)}")
    
    # Group by deductor
    by_deductor = {}
    for tds in result.tds_records:
        by_deductor.setdefault(tds.deductor_tan, []).append(tds)
    
    if by_deductor:
        print(f"\n   By Deductor:")
        for tan, records in sorted(by_deductor.items(), key=lambda x: -sum(r.amount_paid for r in x[1])):
            name = records[0].deductor_name
            amount = sum(r.amount_paid for r in records)
            tax = sum(r.tax_deducted for r in records)
            sections = list({r.section_code for r in records})
            print(f"   └─ {name} (TAN: {tan})")
            print(f"      Sections: {sections}")
            print(f"      Amount: {format_currency(amount)} | TDS: {format_currency(tax)}")
    
    # Part B2 - SFT Summary
    if result.sft_records:
        print(f"\n🏦 PART B2 - SFT (SPECIFIED FINANCIAL TRANSACTIONS) SUMMARY")
        print("-" * 80)
        print(f"   Total Records: {len(result.sft_records)}")
        print(f"   Total Value: {format_currency(result.total_sft_amount)}")
        
        # Group by info type
        by_type = {}
        for item in result.sft_records:
            by_type.setdefault(item.info_type, []).append(item)
        
        for itype, items in sorted(by_type.items(), key=lambda x: -sum(i.amount for i in x[1])):
            itype_total = sum(i.amount for i in items)
            type_name = itype.replace('_', ' ').title()
            print(f"   └─ {type_name}: {len(items)} entries | Value: {format_currency(itype_total)}")
    
    # Part B3 - Tax Payments Summary
    if result.tax_payments:
        print(f"\n🏛️ PART B3 - PAYMENT OF TAXES SUMMARY")
        print("-" * 80)
        print(f"   Total Challans: {len(result.tax_payments)}")
        print(f"   Total Tax Paid: {format_currency(result.total_tax_paid)}")
        
        # Group by major head
        by_head = {}
        for payment in result.tax_payments:
            by_head.setdefault(payment.major_head, []).append(payment)
        
        for head, payments in sorted(by_head.items()):
            head_total = sum(p.total_amount for p in payments)
            print(f"   └─ {head}: {len(payments)} entries | Total: {format_currency(head_total)}")
    else:
        print(f"\n🏛️ PART B3 - PAYMENT OF TAXES SUMMARY")
        print("-" * 80)
        print("   No tax payment records found")
    
    # Part B4 - Demand and Refund Summary
    if result.demands_refunds:
        print(f"\n📑 PART B4 - DEMAND AND REFUND SUMMARY")
        print("-" * 80)
        total_demands = sum(d.demand_amount for d in result.demands_refunds)
        total_refunds = sum(d.refund_amount for d in result.demands_refunds)
        print(f"   Total Demands/Refunds: {len(result.demands_refunds)}")
        print(f"   Total Demand Amount: {format_currency(total_demands)}")
        print(f"   Total Refund Amount: {format_currency(total_refunds)}")
    else:
        print(f"\n📑 PART B4 - DEMAND AND REFUND SUMMARY")
        print("-" * 80)
        print("   No demand/refund records found")
    
    # Part B7 - Other Information Summary
    if result.other_info:
        print(f"\n📝 PART B7 - OTHER INFORMATION SUMMARY")
        print("-" * 80)
        total_other = sum(o.amount for o in result.other_info)
        print(f"   Total Records: {len(result.other_info)}")
        print(f"   Total Amount: {format_currency(total_other)}")
        
        for oi in result.other_info[:5]:
            print(f"   └─ {oi.info_type}: {format_currency(oi.amount)}")
    else:
        print(f"\n📝 PART B7 - OTHER INFORMATION SUMMARY")
        print("-" * 80)
        print("   No other information records found")
    
    # Grand Total
    print("\n" + "=" * 80)
    print("                     GRAND TOTALS")
    print("=" * 80)
    print(f"   TDS Tax Total:          {format_currency(result.total_tds_tax)}")
    print(f"   TDS Amount Total:       {format_currency(result.total_tds_amount)}")
    print(f"   SFT Value Total:        {format_currency(result.total_sft_amount)}")
    print(f"   Tax Paid Total:         {format_currency(result.total_tax_paid)}")
    print("=" * 80)


def print_detailed_entries(result):
    """Print detailed entries for all parts."""
    print("\n\n")
    print("=" * 80)
    print("                    DETAILED ENTRIES")
    print("=" * 80)
    
    # Part B1 - TDS Detailed
    print("\n" + "=" * 80)
    print(f"💰 PART B1 - TDS/TCS DETAILED RECORDS ({len(result.tds_records)})")
    print("=" * 80)
    
    if not result.tds_records:
        print("   No TDS records found")
    else:
        # Group by section for better organization
        by_section = {}
        for tds in result.tds_records:
            by_section.setdefault(tds.section_code, []).append(tds)
        
        for section_code, records in sorted(by_section.items()):
            print(f"\n   ── Section {section_code} ({len(records)} records) ──")
            
            for i, tds in enumerate(records, 1):
                print(f"\n   [{i}] Deductor: {tds.deductor_name}")
                print(f"       TAN: {tds.deductor_tan}")
                print(f"       Amount Paid: {format_currency(tds.amount_paid)}")
                print(f"       Tax Deducted: {format_currency(tds.tax_deducted)}")
                print(f"       Tax Deposited: {format_currency(tds.tax_deposited)}")
                if tds.quarter:
                    print(f"       Quarter: {tds.quarter}")
                if tds.date_of_credit:
                    print(f"       Date: {tds.date_of_credit}")
                if tds.status:
                    print(f"       Status: {tds.status}")
                if tds.receipt_no:
                    print(f"       TSN: {tds.receipt_no}")
    
    # Part B2 - SFT Detailed
    if result.sft_records:
        print("\n" + "=" * 80)
        print(f"🏦 PART B2 - SFT DETAILED RECORDS ({len(result.sft_records)})")
        print("=" * 80)
        
        # Group by type
        by_type = {}
        for item in result.sft_records:
            by_type.setdefault(item.info_type, []).append(item)
        
        for itype, items in sorted(by_type.items()):
            type_name = itype.replace('_', ' ').upper()
            print(f"\n   ── {type_name} ({len(items)} records) ──")
            
            for i, item in enumerate(items, 1):
                print(f"\n   [{i}] {item.description}")
                print(f"       Amount: {format_currency(item.amount)}")
                print(f"       SFT Code: {item.section_code}")
                if item.name_of_deductor:
                    print(f"       Source: {item.name_of_deductor}")
                if item.pan_of_deductor:
                    print(f"       PAN: {item.pan_of_deductor}")
                if item.transaction_date:
                    print(f"       Date: {item.transaction_date}")
                if item.remarks:
                    print(f"       Remarks: {item.remarks}")
    
    # Part B3 - Tax Payments Detailed
    if result.tax_payments:
        print("\n" + "=" * 80)
        print(f"🏛️ PART B3 - TAX PAYMENTS DETAILED ({len(result.tax_payments)})")
        print("=" * 80)
        
        for i, payment in enumerate(result.tax_payments, 1):
            print(f"\n   [{i}] Challan Details:")
            print(f"       BSR Code: {payment.bsr_code}")
            print(f"       Challan Serial No: {payment.challan_serial_no}")
            print(f"       Challan Date: {payment.challan_date}")
            print(f"       Major Head: {payment.major_head}")
            print(f"       Minor Head: {payment.minor_head}")
            print(f"       Tax Amount: {format_currency(payment.tax_amount)}")
            print(f"       Interest: {format_currency(payment.interest_amount)}")
            print(f"       Total Amount: {format_currency(payment.total_amount)}")
            if payment.status:
                print(f"       Status: {payment.status}")
    
    # Part B4 - Demands/Refunds Detailed
    if result.demands_refunds:
        print("\n" + "=" * 80)
        print(f"📑 PART B4 - DEMANDS/REFUNDS DETAILED ({len(result.demands_refunds)})")
        print("=" * 80)
        
        for i, dr in enumerate(result.demands_refunds, 1):
            print(f"\n   [{i}] Assessment Year: {dr.assessment_year}")
            print(f"       Demand ID: {dr.demand_id}")
            print(f"       Demand Status: {dr.demand_status}")
            print(f"       Demand Amount: {format_currency(dr.demand_amount)}")
            print(f"       Refund Status: {dr.refund_status}")
            print(f"       Refund Amount: {format_currency(dr.refund_amount)}")
            if dr.date:
                print(f"       Date: {dr.date}")
    
    # Part B7 - Other Info Detailed
    if result.other_info:
        print("\n" + "=" * 80)
        print(f"📝 PART B7 - OTHER INFORMATION DETAILED ({len(result.other_info)})")
        print("=" * 80)
        
        for i, oi in enumerate(result.other_info, 1):
            print(f"\n   [{i}] Type: {oi.info_type}")
            print(f"       Description: {oi.description}")
            print(f"       Amount: {format_currency(oi.amount)}")
            if oi.details:
                print(f"       Details:")
                for key, value in list(oi.details.items())[:5]:
                    if value and str(value) != 'None':
                        print(f"          {key}: {value}")


def main():
    print("=" * 80)
    print("       COMPREHENSIVE AIS EXTRACTOR")
    print("=" * 80)
    
    # Check for command line arguments
    if len(sys.argv) >= 4:
        pan = sys.argv[1].strip().upper()
        dob = sys.argv[2].strip().replace("/", "").replace("-", "")
        file_path = sys.argv[3].strip().strip('"')
    else:
        # Interactive mode
        pan = input("\nPAN (10 chars): ").strip().upper()
        if len(pan) != 10:
            print("❌ PAN must be 10 characters")
            return
        
        dob = input("DOB (DD/MM/YYYY): ").strip()
        dob = dob.replace("/", "").replace("-", "")
        
        file_path = input("Encrypted AIS JSON path: ").strip().strip('"')
    
    if len(pan) != 10:
        print("❌ PAN must be 10 characters")
        return
    
    if not os.path.exists(file_path):
        print(f"❌ File not found: {file_path}")
        return
    
    # Decrypt
    print("\n🔓 Decrypting...")
    try:
        with open(file_path, 'rb') as f:
            encrypted = f.read()
        ais_data = decrypt_ais(encrypted, pan, dob)
        print("✅ Decryption successful")
    except Exception as e:
        print(f"❌ Decryption failed: {e}")
        return
    
    # Parse
    print("📊 Parsing AIS data...")
    result = parse_ais_json(ais_data)
    print("✅ Parsing complete")
    
    # Display summary first
    print_summary_section(result)
    
    # Display detailed entries
    print_detailed_entries(result)
    
    # Final summary
    print("\n\n" + "=" * 80)
    print("                    EXTRACTION COMPLETE")
    print("=" * 80)
    print(f"   TDS Records: {len(result.tds_records)}")
    print(f"   SFT Records: {len(result.sft_records)}")
    print(f"   Tax Payments: {len(result.tax_payments)}")
    print(f"   Demands/Refunds: {len(result.demands_refunds)}")
    print(f"   Other Info: {len(result.other_info)}")
    if result.errors:
        print(f"   Errors: {len(result.errors)}")
        for err in result.errors:
            print(f"      - {err}")
    print("=" * 80)


if __name__ == "__main__":
    main()
