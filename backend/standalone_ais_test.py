"""Standalone AIS test - no database required."""
import json
import sys
import os

# Add to path for imports
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from app.services.ais_decrypt import decrypt_ais
from app.services.ais_parser import parse_ais_json


def main():
    print("=== Standalone AIS Test ===\n")
    
    # Get inputs
    pan = input("PAN (10 chars): ").strip().upper()
    if len(pan) != 10:
        print("❌ PAN must be 10 characters")
        return
    
    dob = input("DOB (DD/MM/YYYY): ").strip()
    # Convert DD/MM/YYYY to DDMMYYYY for decryption
    dob = dob.replace("/", "").replace("-", "")
    
    file_path = input("Encrypted AIS JSON path: ").strip()
    if not os.path.exists(file_path):
        print(f"❌ File not found: {file_path}")
        return
    
    # Decrypt
    print("\n🔓 Decrypting...")
    with open(file_path, 'rb') as f:
        encrypted = f.read()
    
    try:
        ais_data = decrypt_ais(encrypted, pan, dob)
        print("✅ Decryption successful\n")
    except Exception as e:
        print(f"❌ Decryption failed: {e}")
        return
    
    # Parse
    print("📊 Parsing...\n")
    result = parse_ais_json(ais_data)
    
    # Group info items by type
    by_type = {}
    for item in result.info_items:
        by_type.setdefault(item.info_type, []).append(item)
    
    # Group TDS by section
    by_section = {}
    for tds in result.tds_records:
        by_section.setdefault(tds.section_code, []).append(tds)
    
    # ============================================================
    # TOP SUMMARY (like AIS)
    # ============================================================
    print("=" * 70)
    print("AIS SUMMARY")
    print("=" * 70)
    
    # Personal info summary
    if result.personal_info:
        print(f"\n📋 Assessee: {result.personal_info.get('name', 'N/A')}")
        print(f"   PAN: {result.personal_info.get('pan', 'N/A')}")
        print(f"   DOB: {result.personal_info.get('dob', 'N/A')}")
    
    # TDS Summary
    print(f"\n💰 TDS/TCS SUMMARY (Part B1)")
    print(f"   Total Records: {len(result.tds_records)}")
    print(f"   Total Amount: ₹{result.total_tds_amount:,}")
    print(f"   Total Tax Deducted: ₹{result.total_tds_tax:,}")
    print(f"   Deductors: {result.deductor_count}")
    if by_section:
        for sec, records in sorted(by_section.items()):
            sec_amount = sum(r.amount_paid for r in records)
            sec_tax = sum(r.tax_deducted for r in records)
            print(f"   └─ Section {sec}: {len(records)} entries | ₹{sec_amount:,} | TDS ₹{sec_tax:,}")
    
    # SFT Summary
    sft_items = [i for i in result.info_items if i.info_type.startswith('interest_') or i.info_type in ['mutual_fund', 'property_sale', 'property_purchase', 'shares_securities']]
    if sft_items:
        sft_total = sum(i.amount for i in sft_items)
        print(f"\n🏦 SFT SUMMARY (Part B2)")
        print(f"   Total Transactions: {len(sft_items)}")
        print(f"   Total Value: ₹{sft_total:,}")
        
        # Group by info type
        sft_by_type = {}
        for item in sft_items:
            sft_by_type.setdefault(item.info_type, []).append(item)
        
        for itype, items in sorted(sft_by_type.items()):
            itype_total = sum(i.amount for i in items)
            type_name = itype.replace('_', ' ').title()
            print(f"   └─ {type_name}: {len(items)} entries | ₹{itype_total:,}")
    
    # Other info summary
    other_items = [i for i in result.info_items if i.info_type not in ['interest_bank', 'interest_other', 'mutual_fund', 'property_sale', 'property_purchase', 'shares_securities', 'sft_other']]
    if other_items:
        print(f"\n📋 OTHER INFORMATION (Part B7)")
        print(f"   Total Items: {len(other_items)}")
        for item in other_items[:5]:  # Show first 5
            print(f"   └─ {item.info_type}: ₹{item.amount:,} - {item.description}")
    
    print("\n" + "=" * 70)
    print("DETAILED ENTRIES")
    print("=" * 70)
    
    # ============================================================
    # DETAILED TDS RECORDS
    # ============================================================
    print("\n" + "=" * 70)
    print(f"💰 TDS RECORDS ({len(result.tds_records)})")
    print("=" * 70)
    
    if not result.tds_records:
        print("  No TDS records found")
    else:
        for i, tds in enumerate(result.tds_records, 1):
            print(f"\n[{i}] Section {tds.section_code}")
            print(f"    Deductor: {tds.deductor_name}")
            print(f"    TAN: {tds.deductor_tan}")
            print(f"    Amount Paid: ₹{tds.amount_paid:,}")
            print(f"    Tax Deducted: ₹{tds.tax_deducted:,}")
            print(f"    Tax Deposited: ₹{tds.tax_deposited:,}")
            if tds.quarter:
                print(f"    Quarter: {tds.quarter}")
            if tds.date_of_credit:
                print(f"    Date: {tds.date_of_credit}")
            if tds.status:
                print(f"    Status: {tds.status}")
    
    # ============================================================
    # DETAILED INFO ITEMS
    # ============================================================
    print("\n" + "=" * 70)
    print(f"📋 INFO ITEMS ({len(result.info_items)})")
    print("=" * 70)
    
    if not result.info_items:
        print("  No info items found")
    else:
        for info_type, items in sorted(by_type.items()):
            type_name = info_type.replace('_', ' ').upper()
            print(f"\n{type_name} ({len(items)})")
            print("-" * 70)
            for i, item in enumerate(items, 1):
                print(f"  [{i}] {item.description}")
                print(f"      Amount: ₹{item.amount:,}")
                if item.section_code:
                    print(f"      Code: {item.section_code}")
                if item.transaction_date:
                    print(f"      Date: {item.transaction_date}")
                if item.name_of_deductor:
                    print(f"      Source: {item.name_of_deductor}")
                if item.remarks:
                    print(f"      Remarks: {item.remarks}")
    
    # ============================================================
    # FINAL SUMMARY
    # ============================================================
    print("\n" + "=" * 70)
    print("FINAL SUMMARY")
    print("=" * 70)
    print(f"  TDS Records: {len(result.tds_records)}")
    print(f"  Total TDS Tax: ₹{result.total_tds_tax:,}")
    print(f"  Total TDS Amount: ₹{result.total_tds_amount:,}")
    print(f"  Unique Deductors: {result.deductor_count}")
    print(f"  Info Items: {len(result.info_items)}")
    print(f"  Info Types: {len(by_type)}")
    
    if result.errors:
        print("\n⚠️  ERRORS:")
        for err in result.errors:
            print(f"  - {err}")


if __name__ == "__main__":
    main()
