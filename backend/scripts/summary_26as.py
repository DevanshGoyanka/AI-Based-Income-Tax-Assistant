"""Generate summary report of all 26AS files."""
import asyncio
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent))

from app.core.use_cases.import_form26as import ImportForm26ASUseCase


async def summarize_file(file_path: str, password: str = None):
    """Generate summary for one file."""
    path = Path(file_path)
    if not path.exists():
        return None
    
    file_type = "zip" if path.suffix.lower() == ".zip" else "pdf"
    use_case = ImportForm26ASUseCase()
    
    try:
        with open(path, 'rb') as f:
            if file_type == "zip":
                canonical = use_case.zip_parser.parse(f, password)
            else:
                canonical = use_case.pdf_parser.parse(f, password)
        
        return {
            "file": path.name,
            "type": file_type,
            "pan": canonical.pan,
            "name": canonical.name,
            "ay": canonical.assessment_year,
            "part_i": len(canonical.part_i),
            "part_ii": len(canonical.part_ii),
            "part_vi": len(canonical.part_vi),
            "part_vii": len(canonical.part_vii),
            "total_tds": sum(e.tax_deducted for e in canonical.part_i),
            "total_amount": sum(e.amount_paid for e in canonical.part_i),
        }
    except Exception as e:
        return {"file": path.name, "error": str(e)}


async def main():
    files = [
        ("C:\\Users\\Devansh\\Desktop\\E-FILE_karo\\ASHOK NARAYAN GOSATWAR\\AJRPG3879R_26AS_2026-07-08T20-06-22.pdf", None),
        ("C:\\Users\\Devansh\\Desktop\\E-FILE_karo\\AVINASH EKNATH JODH\\ACQPJ4324A_26AS_2026-07-08T20-07-22.pdf", None),
        ("C:\\Users\\Devansh\\Desktop\\E-FILE_karo\\BHUPESH KRISHNAPRATAP SHUKLA\\BLTPS7401L_26AS_2026-07-08T20-08-19.pdf", None),
        ("C:\\Users\\Devansh\\Desktop\\E-FILE_karo\\BHUSHAN NARESH TILAWAT\\BRKPT6059A_26AS_2026-07-08T20-09-15.pdf", None),
        ("C:\\Users\\Devansh\\Desktop\\E-FILE_karo\\DATTUSINGH GOVINDSINGH CHANDRAVANSHI\\AAVPC4622M_26AS_2026-07-08T20-10-23.pdf", None),
        ("C:\\Users\\Devansh\\Desktop\\E-FILE_karo\\GAJANAN SALIGRAM SOLANKE\\AFFPS7054E_26AS_2026-07-08T20-11-19.pdf", None),
        ("C:\\Users\\Devansh\\Desktop\\E-FILE_karo\\MAHENDRA GULABRAO KOKATE\\ADYPK9796Q_26AS_2026-07-08T20-12-14.pdf", None),
        ("C:\\Users\\Devansh\\Desktop\\E-FILE_karo\\NITIN ARUN AMBHORE\\ABHPB8923F_26AS_2026-07-08T20-13-08.pdf", None),
        ("C:\\Users\\Devansh\\Desktop\\E-FILE_karo\\TEENA SUNIT GOYANKA\\ASIPG5631M_26AS_2026-06-13T00-38-29.pdf", None),
        ("C:\\Users\\Devansh\\Desktop\\E-FILE_karo\\ACUPG3482G-2026 (1).zip", "14061974"),
        ("C:\\Users\\Devansh\\Desktop\\E-FILE_karo\\COVPC5929M-2026.zip", "08022002"),
    ]
    
    print("=" * 120)
    print(f"{'FILE':<50} {'TYPE':<6} {'PAN':<12} {'AY':<10} {'P-I':<5} {'P-VI':<5} {'TOTAL TDS':<15} {'TOTAL AMT':<15}")
    print("=" * 120)
    
    for file_path, password in files:
        result = await summarize_file(file_path, password)
        if result:
            if "error" in result:
                print(f"{result['file']:<50} ERROR: {result['error']}")
            else:
                print(f"{result['file']:<50} {result['type'].upper():<6} {result['pan']:<12} {result['ay']:<10} "
                      f"{result['part_i']:<5} {result['part_vi']:<5} Rs.{result['total_tds']:>12,.0f} Rs.{result['total_amount']:>12,.0f}")
    
    print("=" * 120)


if __name__ == "__main__":
    asyncio.run(main())
