"""Unit tests for Form 26AS importer."""
import pytest
from io import BytesIO
from uuid import uuid4
from app.services.importers.form26as import Form26ASImporter


@pytest.mark.asyncio
async def test_format_dob_password():
    """Test DOB password formatting."""
    importer = Form26ASImporter()
    
    # YYYY-MM-DD format
    assert importer._format_dob_password("2002-02-08") == b"08022002"
    assert importer._format_dob_password("1974-06-14") == b"14061974"
    
    # DD-MM-YYYY format
    assert importer._format_dob_password("08-02-2002") == b"08022002"
    assert importer._format_dob_password("14-06-1974") == b"14061974"
    
    # Raw DDMMYYYY
    assert importer._format_dob_password("08022002") == b"08022002"


@pytest.mark.asyncio
async def test_parse_26as_header():
    """Test parsing 26AS header line."""
    importer = Form26ASImporter()
    
    sample_txt = """^Annual Tax Statement^

File Creation Date^Permanent Account Number (PAN)^Current Status of PAN^Financial Year^Assessment Year^Name of Assessee^Address Line 1^Address Line 2^Address Line 3^Address Line 4^Address Line 5^Statecode^Pin Code
11-06-2026^COVPC5929M^ACTIVE AND OPERATIVE^2025-2026^2026-2027^YASH UMESH CHANDAK^PAVAN PURV APARTMENT^SANGANI COLONY^DURGA CHOWK^AKOLA^AKOLA^MAHARASHTRA^444005

^PART-I - Details of Tax Deducted at Source^
^^^*********** No Transactions Present **********^
"""
    
    data = importer._parse_26as_text(sample_txt)
    
    assert data["header"]["pan"] == "COVPC5929M"
    assert data["header"]["name"] == "YASH UMESH CHANDAK"
    assert data["header"]["assessment_year"] == "2026-2027"
    assert data["header"]["financial_year"] == "2025-2026"
    assert data["header"]["pin_code"] == "444005"


@pytest.mark.asyncio
async def test_parse_26as_with_tds():
    """Test parsing PART-I TDS entries."""
    importer = Form26ASImporter()
    
    sample_txt = """11-06-2026^COVPC5929M^ACTIVE^2025-2026^2026-2027^YASH^ADDR1^ADDR2^ADDR3^CITY^STATE^MH^444005

^PART-I - Details of Tax Deducted at Source^
1^WELLS FARGO INTERNATIONAL SOLUTIONS PRIVATE LIMITED^HYDW00345C^^^^^1964956.60^185112.00^185112.00^
^1^192^26-Mar-2026^F^28-May-2026^-^148459.00^11664.00^11664.00^
^2^192^26-Feb-2026^F^28-May-2026^-^393259.00^62583.00^62583.00^

2^STATE BANK OF INDIA^MUMS89569E^^^^^224329.00^22443.00^22443.00^
^1^194A^31-Mar-2025^F^23-May-2025^-^42822.00^4283.00^4283.00^
"""
    
    data = importer._parse_26as_text(sample_txt)
    
    assert len(data["part1_tds"]) == 2
    
    # First deductor
    d1 = data["part1_tds"][0]
    assert d1["deductor_name"] == "WELLS FARGO INTERNATIONAL SOLUTIONS PRIVATE LIMITED"
    assert d1["tan"] == "HYDW00345C"
    assert d1["total_tds"] == 185112.00
    assert len(d1["transactions"]) == 2
    assert d1["transactions"][0]["section"] == "192"
    assert d1["transactions"][0]["amount"] == 148459.00
    
    # Second deductor
    d2 = data["part1_tds"][1]
    assert d2["deductor_name"] == "STATE BANK OF INDIA"
    assert d2["tan"] == "MUMS89569E"
    assert len(d2["transactions"]) == 1


@pytest.mark.asyncio
async def test_extract_summary():
    """Test summary extraction."""
    importer = Form26ASImporter()
    
    parsed_data = {
        "header": {"pan": "COVPC5929M", "assessment_year": "2026-2027"},
        "part1_tds": [
            {"total_tds": 185112.00, "total_tds_deposited": 185112.00, "total_amount": 1964956.60},
            {"total_tds": 22443.00, "total_tds_deposited": 22443.00, "total_amount": 224329.00}
        ],
        "part6_tcs": []
    }
    
    summary = importer._extract_summary(parsed_data, "2026-2027")
    
    assert summary["assessment_year"] == "2026-2027"
    assert summary["tds_deductors_count"] == 2
    assert summary["total_tds_deducted"] == 207555.00
    assert summary["total_tds_deposited"] == 207555.00
    assert summary["total_amount"] == 2189285.60
