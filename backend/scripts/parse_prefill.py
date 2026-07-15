#!/usr/bin/env python3
"""CLI script to parse and display prefill JSON data."""
import sys
import json
from pathlib import Path
from decimal import Decimal

# Add backend to path
sys.path.insert(0, str(Path(__file__).parent.parent))

from app.core.normalizers.prefill_normalizer import PrefillNormalizer
from app.core.normalizers.canonical_mapper import CanonicalToScheduleMapper
from uuid import uuid4


def main():
    if len(sys.argv) != 2:
        print("Usage: python parse_prefill.py <path_to_prefill.json>")
        sys.exit(1)
    
    file_path = Path(sys.argv[1])
    
    if not file_path.exists():
        print(f"Error: File not found: {file_path}")
        sys.exit(1)
    
    # Load JSON
    with open(file_path, 'r', encoding='utf-8') as f:
        raw_data = json.load(f)
    
    print(f"\n{'='*60}")
    print(f"PREFILL JSON PARSER")
    print(f"{'='*60}\n")
    
    # Normalize
    normalizer = PrefillNormalizer()
    canonical = normalizer.normalize(raw_data)
    
    # Display results
    print(f"📄 File: {file_path.name}\n")
    
    # Personal Info
    if canonical.personal_info:
        pi = canonical.personal_info
        print(f"👤 PERSONAL INFORMATION")
        print("-" * 60)
        print(f"  Name: {pi.name}")
        print(f"  PAN: {pi.pan}")
        print(f"  Father: {pi.father_name}")
        print(f"  DOB: {pi.dob}")
        print(f"  Email: {pi.email}")
        print(f"  Mobile: {pi.mobile}")
        print(f"  Address: {pi.address}")
        print(f"  Pincode: {pi.pincode}\n")
    
    # Bank Accounts
    if canonical.bank_accounts:
        print(f"🏦 BANK ACCOUNTS ({len(canonical.bank_accounts)} accounts)")
        print("-" * 60)
        for i, bank in enumerate(canonical.bank_accounts, 1):
            refund_mark = " ✓ [For Refund]" if bank.use_for_refund else ""
            print(f"  {i}. {bank.bank_name}{refund_mark}")
            print(f"     Account: {bank.account_number}")
            print(f"     IFSC: {bank.ifsc_code}")
            print(f"     Type: {bank.account_type}\n")
    
    # Advance Tax
    if canonical.advance_tax:
        total_advance = sum(tax.amount for tax in canonical.advance_tax)
        print(f"💳 ADVANCE TAX / CHALLANS (Total: ₹{total_advance:,.0f})")
        print("-" * 60)
        for i, tax in enumerate(canonical.advance_tax, 1):
            print(f"  {i}. Date: {tax.date_paid}")
            print(f"     Amount: ₹{tax.amount:,.0f}")
            print(f"     BSR: {tax.bsr_code} | Challan: {tax.challan_serial}")
            print(f"     Bank: {tax.bank_name}")
            print(f"     Branch: {tax.branch_name}\n")
    
    # Salaries
    if canonical.salaries:
        print(f"💼 SALARY INCOME ({len(canonical.salaries)} employers)")
        print("-" * 60)
        for i, sal in enumerate(canonical.salaries, 1):
            print(f"  {i}. {sal.employer_name}")
            print(f"     TAN: {sal.employer_tan}")
            print(f"     Gross: ₹{sal.gross_salary:,.0f}")
            print(f"     Exempt: ₹{sal.allowances_exempt:,.0f}")
            print(f"     Prof Tax: ₹{sal.professional_tax:,.0f}")
            print(f"     TDS: ₹{sal.tds_deducted:,.0f}")
            net = sal.gross_salary - sal.allowances_exempt - sal.professional_tax - sal.standard_deduction
            print(f"     Net: ₹{net:,.0f}\n")
    
    # House Property
    if canonical.house_properties:
        print(f"🏠 HOUSE PROPERTY ({len(canonical.house_properties)} properties)")
        print("-" * 60)
        for i, prop in enumerate(canonical.house_properties, 1):
            print(f"  {i}. {prop.address}")
            print(f"     Rent: ₹{prop.annual_rent:,.0f}")
            print(f"     Municipal Tax: ₹{prop.municipal_taxes:,.0f}")
            print(f"     Interest: ₹{prop.interest_paid:,.0f}")
            print(f"     Ownership: {float(prop.ownership_share)*100:.0f}%\n")
    
    # Interest
    if canonical.interest_income:
        total_interest = sum(i.interest_amount for i in canonical.interest_income)
        print(f"💰 INTEREST INCOME (Total: ₹{total_interest:,.0f})")
        print("-" * 60)
        for i, interest in enumerate(canonical.interest_income, 1):
            print(f"  {i}. {interest.bank_name}: ₹{interest.interest_amount:,.0f}")
        print()
    
    # Dividends
    if canonical.dividend_income:
        total_div = sum(d.dividend_amount for d in canonical.dividend_income)
        print(f"📊 DIVIDEND INCOME (Total: ₹{total_div:,.0f})")
        print("-" * 60)
        for i, div in enumerate(canonical.dividend_income, 1):
            print(f"  {i}. {div.company_name}: ₹{div.dividend_amount:,.0f}")
        print()
    
    # TDS
    if canonical.tds_entries:
        total_tds = sum(t.tax_deposited for t in canonical.tds_entries)
        print(f"🧾 TDS ENTRIES (Total: ₹{total_tds:,.0f})")
        print("-" * 60)
        for i, tds in enumerate(canonical.tds_entries, 1):
            print(f"  {i}. {tds.deductor_name} (TAN: {tds.deductor_tan})")
            print(f"     Section: {tds.section_code}")
            print(f"     Amount Paid: ₹{tds.amount_paid:,.0f}")
            print(f"     TDS: ₹{tds.tax_deposited:,.0f}\n")
    
    # Map to schedules
    print(f"\n{'='*60}")
    print("SCHEDULE MAPPING")
    print(f"{'='*60}\n")
    
    mapper = CanonicalToScheduleMapper()
    schedules = mapper.map_to_schedules(canonical, uuid4(), "2026-27")
    
    for schedule in schedules:
        schedule_name = type(schedule).__name__
        income = schedule.compute_income()
        print(f"✓ {schedule_name}")
        if income != 0:
            print(f"  Computed Income: ₹{income:,.2f}")
        
        errors = schedule.validate()
        if errors:
            print(f"  ⚠ Validation Errors: {len(errors)}")
            for err in errors[:3]:
                print(f"    - {err.field}: {err.message}")
        print()
    
    print(f"{'='*60}")
    print("✓ Parsing complete")
    print(f"{'='*60}\n")


if __name__ == "__main__":
    main()
