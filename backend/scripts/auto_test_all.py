"""Automated universal test - tests all clients with DOB against ZIP and PDF files.

ZIP behavior: ZIP is encrypted with the client's DOB (DDMMYYYY). Only the client
whose DOB matches the ZIP password can decrypt it. Other clients will fail.

PDF behavior: PDF is NOT encrypted. The PAN/Name comes from the FILE content,
not the client record. Uploading Sunit's PDF for any client will show
Sunit's PAN/Name - this is correct.

For meaningful tests, each client should upload their OWN Form26AS file
(both ZIP and PDF) that was downloaded from the Income Tax portal using
their own credentials.
"""
import asyncio
import httpx
from pathlib import Path
import sys
sys.path.insert(0, ".")
from app.infra.db.base import AsyncSessionLocal
from sqlalchemy import text

BASE = "http://localhost:8001/api/v1"
FILES = {
    "zip": r"C:\Users\Devansh\Desktop\E-FILE_karo\COVPC5929M-2026.zip",
    "pdf": r"C:\Users\Devansh\Desktop\E-FILE_karo\ACUPG3482G-2025.pdf",
}


async def get_clients_with_dob() -> list[dict]:
    """Fetch client info including DOB directly from DB."""
    async with AsyncSessionLocal() as db:
        result = await db.execute(text("""
            SELECT id, pan, name, email, dob 
            FROM clients 
            ORDER BY name
        """))
        return [dict(row._mapping) for row in result.fetchall()]


async def test_upload(client_id: str, client_name: str, client_pan: str, file_path: str, file_type: str):
    r = httpx.post("http://localhost:8001/api/v1/auth/login", json={"email": "test@example.com", "password": "Test123!@#"}, timeout=15)
    token = r.json()["access_token"]
    
    with open(file_path, "rb") as f:
        r = httpx.post(
            f"http://localhost:8001/api/v1/imports/form26as/{client_id}",
            headers={"Authorization": f"Bearer {token}"},
            files={"file": (Path(file_path).name, f, "application/zip" if file_type == "zip" else "application/pdf")},
            timeout=30
        )
    
    result = r.json()
    ext = result.get("extracted", {})
    
    success = ext.get("source") == file_type
    pan = ext.get("header", {}).get("pan") or "-"
    ay = ext.get("assessment_year") or "-"
    deductors = ext.get("totals", {}).get("tds_deductors_count", 0)
    total_tds = ext.get("totals", {}).get("total_tds_deducted", 0)
    errors = result.get("errors", [])
    
    status = "PASS" if success else "FAIL"
    mark = "Y" if success else "N"
    print(f"  [{mark}] {status:<6} | {client_name:<28} | {file_type:<3} | PAN={pan:<12} | AY={ay:<8} | Ded={deductors} | TDS=Rs.{total_tds:>12,.2f} | Err={len(errors)}")
    
    return success


async def main():
    print("=" * 95)
    print("  UNIVERSAL FORM26AS TEST - All clients, both file types")
    print("=" * 95)
    print()
    
    # Get clients from DB (includes DOB)
    clients = await get_clients_with_dob()
    with_dob = [c for c in clients if c.get("dob")]
    
    print(f"Total clients: {len(clients)} | With DOB: {len(with_dob)}")
    if with_dob:
        print("Clients with DOB (ZIP-eligible):")
        for c in with_dob:
            pwd = c["dob"].strftime("%d%m%Y") if c["dob"] else "N/A"
            print(f"  - {c['name']} (PAN: {c['pan']}, DOB: {c['dob']}, Password: {pwd})")
    print()
    
    # Show file info
    for ft, fp in FILES.items():
        exists = "EXISTS" if Path(fp).exists() else "NOT FOUND"
        print(f"  {ft.upper()} file: {exists} - {Path(fp).name}")
    print()
    
    all_passed = True
    
    # Test ZIP with clients that have DOB
    if Path(FILES["zip"]).exists():
        print("-" * 95)
        print("  ZIP TESTS (TRACES Annual Tax Statement)")
        print("  NOTE: ZIP is encrypted. Only the client whose DOB matches the ZIP")
        print("        password will succeed. Other clients fail - this is expected.")
        print("-" * 95)
        if not with_dob:
            print("  SKIPPED - no clients have DOB set")
        else:
            for c in with_dob:
                passed = await test_upload(c["id"], c["name"], c["pan"], FILES["zip"], "zip")
                if not passed:
                    all_passed = False
        print()
    
    # Test PDF with all clients
    if Path(FILES["pdf"]).exists():
        print("-" * 95)
        print("  PDF TESTS (e-Filing Portal)")
        print("  NOTE: PAN/Name comes from the FILE, not the client record.")
        print("        All clients show ACUPG3482G because the same Sunit's PDF")
        print("        was uploaded for every client - this is expected.")
        print("-" * 95)
        for c in clients:
            passed = await test_upload(c["id"], c["name"], c["pan"], FILES["pdf"], "pdf")
            if not passed:
                all_passed = False
        print()
    
    print("=" * 95)
    if all_passed:
        print("  ALL TESTS PASSED!")
    else:
        print("  SOME TESTS FAILED!")
        print("  NOTE: ZIP failures are EXPECTED if client's DOB != ZIP password.")
        print("        PDF PAN mismatches are EXPECTED if the same PDF is used for all clients.")
    print("=" * 95)

asyncio.run(main())
