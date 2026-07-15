"""Show complete parsed output for one specific file."""
import asyncio
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent))

from app.core.use_cases.import_form26as import ImportForm26ASUseCase


async def show_complete_file(file_path: str, password: str = None):
    """Show complete parsed output for a single file."""
    path = Path(file_path)
    
    file_type = "zip" if path.suffix.lower() == ".zip" else "pdf"
    
    print("=" * 80)
    print(f"COMPLETE PARSED OUTPUT: {path.name}")
    print("=" * 80)
    
    use_case = ImportForm26ASUseCase()
    
    with open(path, 'rb') as f:
        if file_type == "zip":
            canonical_26as = use_case.zip_parser.parse(f, password)
        else:
            canonical_26as = use_case.pdf_parser.parse(f, password)
    
    # Personal Details
    print(f"\nPERSONAL DETAILS")
    print(f"  PAN: {canonical_26as.pan}")
    print(f"  Name: {canonical_26as.name}")
    print(f"  Assessment Year: {canonical_26as.assessment_year}")
    print(f"  Source: {canonical_26as.source_format}")
    
    # Part I - TDS
    if canonical_26as.part_i:
        print(f"\nPART I - TDS BY DEDUCTOR ({len(canonical_26as.part_i)} entries)")
        total_amount = sum(e.amount_paid for e in canonical_26as.part_i)
        total_tds = sum(e.tax_deducted for e in canonical_26as.part_i)
        print(f"  Total Amount Paid: Rs.{total_amount:,.2f}")
        print(f"  Total TDS: Rs.{total_tds:,.2f}")
        
        for i, entry in enumerate(canonical_26as.part_i, 1):
            print(f"\n  [{i}] {entry.deductor_name}")
            print(f"      TAN: {entry.deductor_tan}")
            print(f"      Section: {entry.section_code}")
            print(f"      Date: {entry.transaction_date}")
            print(f"      Amount Paid: Rs.{entry.amount_paid:,.2f}")
            print(f"      Tax Deducted: Rs.{entry.tax_deducted:,.2f}")
            print(f"      TDS Deposited: Rs.{entry.tds_deposited:,.2f}")
            if entry.status_of_booking:
                print(f"      Status: {entry.status_of_booking}")
            if entry.date_of_booking:
                print(f"      Booking Date: {entry.date_of_booking}")
            if entry.remarks and entry.remarks != "-":
                print(f"      Remarks: {entry.remarks}")
    
    # Part II
    if canonical_26as.part_ii:
        print(f"\nPART II - 15G/15H ({len(canonical_26as.part_ii)} entries)")
        for i, entry in enumerate(canonical_26as.part_ii, 1):
            print(f"\n  [{i}] {entry.deductor_name}")
            print(f"      TAN: {entry.deductor_tan}")
            print(f"      Amount: Rs.{entry.amount_paid:,.2f}")
            print(f"      TDS: Rs.{entry.tax_deducted:,.2f}")
    
    # Part VI - TCS
    if canonical_26as.part_vi:
        print(f"\nPART VI - TAX COLLECTED AT SOURCE ({len(canonical_26as.part_vi)} entries)")
        total_tcs = sum(e.tax_collected for e in canonical_26as.part_vi)
        print(f"  Total TCS: Rs.{total_tcs:,.2f}")
        
        for i, entry in enumerate(canonical_26as.part_vi, 1):
            print(f"\n  [{i}] {entry.collector_name}")
            print(f"      TAN: {entry.collector_tan}")
            if entry.section_code:
                print(f"      Section: {entry.section_code}")
            print(f"      Amount: Rs.{entry.amount_paid:,.2f}")
            print(f"      Tax Collected: Rs.{entry.tax_collected:,.2f}")
            print(f"      TCS Deposited: Rs.{entry.tcs_deposited:,.2f}")
    
    # Part VII - Refunds
    if canonical_26as.part_vii:
        print(f"\nPART VII - REFUNDS ({len(canonical_26as.part_vii)} entries)")
        for i, entry in enumerate(canonical_26as.part_vii, 1):
            print(f"\n  [{i}] AY {entry.assessment_year}")
            print(f"      Mode: {entry.mode}")
            print(f"      Nature: {entry.nature_of_refund}")
            print(f"      Amount: Rs.{entry.amount_of_refund:,.2f}")
            print(f"      Interest: Rs.{entry.interest:,.2f}")
            print(f"      Date: {entry.date_of_payment}")
    
    # Summary
    print(f"\n" + "=" * 80)
    print(f"SUMMARY")
    print(f"  Part I (TDS): {len(canonical_26as.part_i)} entries")
    print(f"  Part II (15G/15H): {len(canonical_26as.part_ii)} entries")
    print(f"  Part III: {len(canonical_26as.part_iii)} entries")
    print(f"  Part IV: {len(canonical_26as.part_iv)} entries")
    print(f"  Part V: {len(canonical_26as.part_v)} entries")
    print(f"  Part VI (TCS): {len(canonical_26as.part_vi)} entries")
    print(f"  Part VII (Refunds): {len(canonical_26as.part_vii)} entries")
    print(f"  Part VIII: {len(canonical_26as.part_viii)} entries")
    print(f"  Part IX: {len(canonical_26as.part_ix)} entries")
    print(f"  Part X: {len(canonical_26as.part_x)} entries")
    print("=" * 80)


if __name__ == "__main__":
    # Show COVPC5929M ZIP file with all details
    asyncio.run(show_complete_file(
        "C:\\Users\\Devansh\\Desktop\\E-FILE_karo\\COVPC5929M-2026.zip",
        "08022002"
    ))
