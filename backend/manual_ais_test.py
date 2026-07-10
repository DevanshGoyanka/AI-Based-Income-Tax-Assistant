"""Manual AIS import test script."""
import sys
import os
import getpass
import json
from pathlib import Path

# Add parent directory to path to import app modules
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from app.core.database import SessionLocal
from app.core.security import verify_password, get_password_hash
from app.models.user import User
from app.services.ais_decrypt import decrypt_ais_json
from app.services.ais_parser import parse_ais_json


def main():
    print("=== AIS Manual Test ===\n")
    
    # Step 1: Login
    db = SessionLocal()
    try:
        email = input("Email: ").strip()
        password = getpass.getpass("Password: ")
        
        user = db.query(User).filter(User.email == email).first()
        if not user or not verify_password(password, user.hashed_password):
            print("❌ Invalid credentials")
            return
        
        print(f"✅ Logged in as {user.name}\n")
        
        # Step 2: Get PAN
        pan = input("PAN: ").strip().upper()
        if len(pan) != 10:
            print("❌ PAN must be 10 characters")
            return
        
        # Step 3: Get DOB
        dob = input("DOB (DD/MM/YYYY): ").strip()
        
        # Step 4: Get file path
        file_path = input("Encrypted AIS JSON path: ").strip()
        if not Path(file_path).exists():
            print(f"❌ File not found: {file_path}")
            return
        
        print("\n🔓 Decrypting AIS JSON...")
        with open(file_path, 'rb') as f:
            encrypted_data = f.read()
        
        decrypted_json = decrypt_ais_json(encrypted_data, pan, dob)
        ais_data = json.loads(decrypted_json)
        
        print("✅ Decryption successful\n")
        
        # Step 5: Parse AIS
        print("📊 Parsing AIS data...\n")
        result = parse_ais_json(ais_data)
        
        # Display results
        print("=" * 60)
        print("PERSONAL INFORMATION")
        print("=" * 60)
        if result.personal_info:
            for key, value in result.personal_info.items():
                if value:
                    print(f"  {key}: {value}")
        print()
        
        print("=" * 60)
        print(f"TDS RECORDS ({len(result.tds_records)})")
        print("=" * 60)
        for i, tds in enumerate(result.tds_records, 1):
            print(f"\n[{i}] Section {tds.section_code}")
            print(f"    Deductor: {tds.deductor_name} (TAN: {tds.deductor_tan})")
            print(f"    Amount Paid: ₹{tds.amount_paid:,}")
            print(f"    Tax Deducted: ₹{tds.tax_deducted:,}")
            print(f"    Tax Deposited: ₹{tds.tax_deposited:,}")
            if tds.quarter:
                print(f"    Quarter: {tds.quarter}")
            if tds.date_of_credit:
                print(f"    Date: {tds.date_of_credit}")
            if tds.status:
                print(f"    Status: {tds.status}")
        
        print("\n" + "=" * 60)
        print(f"INFO ITEMS ({len(result.info_items)})")
        print("=" * 60)
        
        # Group by type
        by_type = {}
        for item in result.info_items:
            by_type.setdefault(item.info_type, []).append(item)
        
        for info_type, items in by_type.items():
            print(f"\n{info_type.upper().replace('_', ' ')} ({len(items)})")
            print("-" * 60)
            for i, item in enumerate(items, 1):
                print(f"  [{i}] {item.description}")
                print(f"      Amount: ₹{item.amount:,}")
                if item.section_code:
                    print(f"      Code: {item.section_code}")
                if item.transaction_date:
                    print(f"      Date: {item.transaction_date}")
                if item.name_of_deductor:
                    print(f"      Source: {item.name_of_deductor}")
        
        print("\n" + "=" * 60)
        print("BANK ACCOUNTS")
        print("=" * 60)
        if result.bank_accounts:
            for i, bank in enumerate(result.bank_accounts, 1):
                print(f"\n[{i}] {bank.get('bank_name', 'N/A')}")
                print(f"    IFSC: {bank.get('ifsc', 'N/A')}")
                print(f"    Account: {bank.get('account_no', 'N/A')}")
                print(f"    Type: {bank.get('account_type', 'N/A')}")
        else:
            print("  No bank accounts found")
        
        print("\n" + "=" * 60)
        print("SUMMARY")
        print("=" * 60)
        print(f"  Total TDS Records: {len(result.tds_records)}")
        print(f"  Total TDS Tax: ₹{result.total_tds_tax:,}")
        print(f"  Total TDS Amount: ₹{result.total_tds_amount:,}")
        print(f"  Unique Deductors: {result.deductor_count}")
        print(f"  Total Info Items: {len(result.info_items)}")
        print(f"  Info Types: {', '.join(by_type.keys())}")
        
        if result.errors:
            print("\n⚠️  ERRORS:")
            for err in result.errors:
                print(f"  - {err}")
        
    finally:
        db.close()


if __name__ == "__main__":
    main()
