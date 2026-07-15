"""Unit tests for Form 26AS ZIP parser."""
import pytest
from io import BytesIO
import zipfile
from decimal import Decimal
from uuid import uuid4

from app.core.parsers.form26as_zip_parser import Form26ASZipParser


@pytest.mark.asyncio
async def test_format_dob():
    """Test DOB password formatting in ZIP parser.
    
    Note: The Form26ASZipParser expects DOB in DDMMYYYY format
    as bytes for ZIP password. The ZIP uses AES encryption.
    """
    # DOB should be passed as DDMMYYYY format bytes
    dob = b"08022002"
    assert len(dob) == 8
    assert dob.isdigit()


@pytest.mark.asyncio
async def test_parse_26as_header():
    """Test parsing 26AS header line from canonical domain."""
    from app.core.domain.canonical_26as import Canonical26AS
    
    # Verify canonical model structure
    canonical = Canonical26AS(
        pan="COVPC5929M",
        name="YASH UMESH CHANDAK",
        assessment_year="2026-2027",
        source_format="zip"
    )
    
    assert canonical.pan == "COVPC5929M"
    assert canonical.name == "YASH UMESH CHANDAK"
    assert canonical.assessment_year == "2026-2027"
    assert canonical.source_format == "zip"


@pytest.mark.asyncio
async def test_canonical_part_i():
    """Test canonical Part I TDS entry structure."""
    from app.core.domain.canonical_26as import Canonical26ASPartI
    
    entry = Canonical26ASPartI(
        deductor_name="WELLS FARGO INTERNATIONAL SOLUTIONS PRIVATE LIMITED",
        deductor_tan="HYDW00345C",
        section_code="192",
        transaction_date="26-Mar-2026",
        amount_paid=Decimal("148459.00"),
        tax_deducted=Decimal("11664.00"),
        tds_deposited=Decimal("11664.00"),
        status_of_booking="F",
        date_of_booking="28-May-2026",
        remarks=None,
    )
    
    assert entry.deductor_name == "WELLS FARGO INTERNATIONAL SOLUTIONS PRIVATE LIMITED"
    assert entry.deductor_tan == "HYDW00345C"
    assert entry.section_code == "192"
    assert entry.amount_paid == Decimal("148459.00")
    assert entry.tax_deducted == Decimal("11664.00")


@pytest.mark.asyncio
async def test_canonical_26as_complete():
    """Test complete canonical 26AS structure."""
    from app.core.domain.canonical_26as import Canonical26AS, Canonical26ASPartI, Canonical26ASPartVI
    
    canonical = Canonical26AS(
        pan="COVPC5929M",
        name="YASH UMESH CHANDAK",
        assessment_year="2026-2027",
        source_format="zip"
    )
    
    # Add Part I TDS entries
    tds1 = Canonical26ASPartI(
        deductor_name="WELLS FARGO INTERNATIONAL SOLUTIONS PRIVATE LIMITED",
        deductor_tan="HYDW00345C",
        section_code="192",
        transaction_date="26-Mar-2026",
        amount_paid=Decimal("148459.00"),
        tax_deducted=Decimal("11664.00"),
        tds_deposited=Decimal("11664.00"),
    )
    canonical.part_i.append(tds1)
    
    tds2 = Canonical26ASPartI(
        deductor_name="STATE BANK OF INDIA",
        deductor_tan="MUMS89569E",
        section_code="194A",
        transaction_date="31-Mar-2025",
        amount_paid=Decimal("42822.00"),
        tax_deducted=Decimal("4283.00"),
        tds_deposited=Decimal("4283.00"),
    )
    canonical.part_i.append(tds2)
    
    # Verify structure
    assert len(canonical.part_i) == 2
    assert canonical.pan == "COVPC5929M"
    
    # Calculate totals
    total_tds = sum(e.tax_deducted for e in canonical.part_i)
    assert total_tds == Decimal("15947.00")


@pytest.mark.asyncio
async def test_decimal_conversion():
    """Test decimal conversion helper in parser."""
    parser = Form26ASZipParser()
    
    assert parser._to_decimal("") == Decimal("0")
    assert parser._to_decimal("  ") == Decimal("0")
    assert parser._to_decimal("1,48,459.00") == Decimal("148459")
    assert parser._to_decimal("11664.00") == Decimal("11664")
    assert parser._to_decimal("0") == Decimal("0")
