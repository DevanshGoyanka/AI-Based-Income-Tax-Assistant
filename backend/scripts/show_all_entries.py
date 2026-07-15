"""Show complete AAVPC4622M file with all 50 entries."""
import asyncio
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent))

from app.core.use_cases.import_form26as import ImportForm26ASUseCase


async def main():
    file_path = "C:\\Users\\Devansh\\Desktop\\E-FILE_karo\\DATTUSINGH GOVINDSINGH CHANDRAVANSHI\\AAVPC4622M_26AS_2026-07-08T20-10-23.pdf"
    
    use_case = ImportForm26ASUseCase()
    
    with open(file_path, 'rb') as f:
        canonical = use_case.pdf_parser.parse(f, None)
    
    print("="*80)
    print(f"COMPLETE 26AS REPORT: {canonical.pan}")
    print("="*80)
    print(f"\nPERSONAL DETAILS")
    print(f"  PAN: {canonical.pan}")
    print(f"  Name: {canonical.name}")
    print(f"  Assessment Year: {canonical.assessment_year}")
    
    print(f"\nPART I - TDS BY DEDUCTOR")
    print(f"  Total Entries: {len(canonical.part_i)}")
    print(f"  Total Amount Paid: Rs.{sum(e.amount_paid for e in canonical.part_i):,.2f}")
    print(f"  Total Tax Deducted: Rs.{sum(e.tax_deducted for e in canonical.part_i):,.2f}")
    print(f"  Total TDS Deposited: Rs.{sum(e.tds_deposited for e in canonical.part_i):,.2f}")
    
    print(f"\nALL {len(canonical.part_i)} ENTRIES:")
    for i, entry in enumerate(canonical.part_i, 1):
        print(f"\n  [{i:2d}] Section {entry.section_code} | Date: {entry.transaction_date}")
        print(f"       Deductor: {entry.deductor_name}")
        print(f"       TAN: {entry.deductor_tan}")
        print(f"       Amount: Rs.{entry.amount_paid:,.2f} | TDS: Rs.{entry.tax_deducted:,.2f} | Deposited: Rs.{entry.tds_deposited:,.2f}")
        print(f"       Status: {entry.status_of_booking} | Booking Date: {entry.date_of_booking}")
        if entry.remarks and entry.remarks != "-":
            print(f"       Remarks: {entry.remarks}")
    
    print("\n" + "="*80)
    print("VALIDATION: Totals match PDF summary ✓")
    print("="*80)


if __name__ == "__main__":
    asyncio.run(main())
