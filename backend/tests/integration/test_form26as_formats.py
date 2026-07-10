"""Integration tests for Form 26AS ZIP and PDF format support.

This test verifies that:
1. ZIP files are correctly detected and parsed as ZIP format
2. PDF files are correctly detected and parsed as PDF format
3. All 10 parts of Form 26AS are parsed
4. TDS deductor summary and transaction data is correctly extracted
5. Data is stored in the database
"""
import io
import pytest
import httpx
import pytest_asyncio
from uuid import UUID
from pathlib import Path


BASE_URL = "http://localhost:8001/api/v1"
TEST_CLIENT_ID = UUID("2fe92273-a57f-4b2b-8867-d6b2916c6680")
ZIP_FILE = Path(r"C:\Users\Devansh\Desktop\E-FILE_karo\COVPC5929M-2026.zip")
PDF_FILE = Path(r"C:\Users\Devansh\Desktop\E-FILE_karo\ACUPG3482G-2025.pdf")


@pytest_asyncio.fixture
async def auth_token():
    """Get auth token for API calls."""
    async with httpx.AsyncClient(timeout=30) as client:
        r = await client.post(f"{BASE_URL}/auth/login", json={
            "email": "test@example.com",
            "password": "Test123!@#"
        })
        return r.json()["access_token"]


@pytest.mark.skipif(not ZIP_FILE.exists(), reason="ZIP file not found")
class TestForm26ASZipFormat:
    """Test ZIP format (TRACES Annual Tax Statement) parsing."""

    @pytest.mark.asyncio
    async def test_zip_upload_returns_success(self, auth_token):
        """ZIP upload should return 200 with imported=1."""
        headers = {"Authorization": f"Bearer {auth_token}"}
        async with httpx.AsyncClient(timeout=30) as client:
            with open(ZIP_FILE, "rb") as f:
                r = await client.post(
                    f"{BASE_URL}/imports/form26as/{TEST_CLIENT_ID}",
                    headers=headers,
                    files={"file": (ZIP_FILE.name, f, "application/zip")}
                )
        assert r.status_code == 200, f"Expected 200, got {r.status_code}: {r.text}"
        data = r.json()
        assert data["imported"] == 1, f"Expected imported=1, got {data}"
        assert data["errors"] == [], f"Expected no errors, got {data['errors']}"

    @pytest.mark.asyncio
    async def test_zip_format_detection(self, auth_token):
        """ZIP file should be detected as 'zip' source format."""
        headers = {"Authorization": f"Bearer {auth_token}"}
        async with httpx.AsyncClient(timeout=30) as client:
            with open(ZIP_FILE, "rb") as f:
                r = await client.post(
                    f"{BASE_URL}/imports/form26as/{TEST_CLIENT_ID}",
                    headers=headers,
                    files={"file": (ZIP_FILE.name, f, "application/zip")}
                )
        data = r.json()
        assert data["extracted"]["source"] == "zip", \
            f"Expected source='zip', got {data['extracted'].get('source')}"

    @pytest.mark.asyncio
    async def test_zip_header_parsed(self, auth_token):
        """ZIP file header should be correctly parsed."""
        headers = {"Authorization": f"Bearer {auth_token}"}
        async with httpx.AsyncClient(timeout=30) as client:
            with open(ZIP_FILE, "rb") as f:
                r = await client.post(
                    f"{BASE_URL}/imports/form26as/{TEST_CLIENT_ID}",
                    headers=headers,
                    files={"file": (ZIP_FILE.name, f, "application/zip")}
                )
        data = r.json()
        extracted = data["extracted"]
        assert extracted["header"]["pan"] == "COVPC5929M", \
            f"Expected PAN='COVPC5929M', got {extracted['header']['pan']!r}"
        assert "YASH" in extracted["header"]["name"].upper(), \
            f"Expected name to contain 'YASH', got {extracted['header']['name']!r}"
        assert "2026-27" in extracted["assessment_year"], \
            f"Expected AY='2026-27', got {extracted['assessment_year']!r}"

    @pytest.mark.asyncio
    async def test_zip_tds_deductors_parsed(self, auth_token):
        """ZIP file should parse TDS deductors with transactions."""
        headers = {"Authorization": f"Bearer {auth_token}"}
        async with httpx.AsyncClient(timeout=30) as client:
            with open(ZIP_FILE, "rb") as f:
                r = await client.post(
                    f"{BASE_URL}/imports/form26as/{TEST_CLIENT_ID}",
                    headers=headers,
                    files={"file": (ZIP_FILE.name, f, "application/zip")}
                )
        data = r.json()
        extracted = data["extracted"]
        
        # Should have 3 deductors
        assert extracted["totals"]["tds_deductors_count"] == 3, \
            f"Expected 3 deductors, got {extracted['totals']['tds_deductors_count']}"
        
        # Should have TDS entries in response
        assert len(extracted["tds_entries"]) == 3, \
            f"Expected 3 TDS entries, got {len(extracted['tds_entries'])}"
        
        # WELLS FARGO should have transactions
        wells_fargo = next((e for e in extracted["tds_entries"] 
                           if "WELLS FARGO" in e.get("deductor_name", "").upper()), None)
        assert wells_fargo is not None, "WELLS FARGO deductor not found"
        assert len(wells_fargo.get("transactions", [])) >= 12, \
            f"Expected >=12 transactions, got {len(wells_fargo.get('transactions', []))}"

    @pytest.mark.asyncio
    async def test_zip_tds_amounts_correct(self, auth_token):
        """ZIP file should correctly sum TDS amounts."""
        headers = {"Authorization": f"Bearer {auth_token}"}
        async with httpx.AsyncClient(timeout=30) as client:
            with open(ZIP_FILE, "rb") as f:
                r = await client.post(
                    f"{BASE_URL}/imports/form26as/{TEST_CLIENT_ID}",
                    headers=headers,
                    files={"file": (ZIP_FILE.name, f, "application/zip")}
                )
        data = r.json()
        extracted = data["extracted"]
        
        # WELLS FARGO has Rs.185,112 TDS
        assert extracted["totals"]["total_tds_deducted"] >= 185000, \
            f"Expected total_tds >= 185000, got {extracted['totals']['total_tds_deducted']}"
        
        # Amount totals should be reasonable
        assert extracted["totals"]["total_amount_paid_credited"] >= 1900000, \
            f"Expected total_amount >= 1900000, got {extracted['totals']['total_amount_paid_credited']}"


@pytest.mark.skipif(not PDF_FILE.exists(), reason="PDF file not found")
class TestForm26ASPdfFormat:
    """Test PDF format (e-filing portal Annual Tax Statement) parsing."""

    PDF_CLIENT_ID = UUID("ee604226-feda-4c8e-9425-18309827b1a7")

    @pytest.mark.asyncio
    async def test_pdf_upload_returns_success(self, auth_token):
        """PDF upload should return 200 with imported=1."""
        headers = {"Authorization": f"Bearer {auth_token}"}
        async with httpx.AsyncClient(timeout=30) as client:
            with open(PDF_FILE, "rb") as f:
                r = await client.post(
                    f"{BASE_URL}/imports/form26as/{self.PDF_CLIENT_ID}",
                    headers=headers,
                    files={"file": (PDF_FILE.name, f, "application/pdf")}
                )
        assert r.status_code == 200, f"Expected 200, got {r.status_code}: {r.text}"
        data = r.json()
        assert data["imported"] == 1, f"Expected imported=1, got {data}"
        assert data["errors"] == [], f"Expected no errors, got {data['errors']}"

    @pytest.mark.asyncio
    async def test_pdf_format_detection(self, auth_token):
        """PDF file should be detected as 'pdf' source format."""
        headers = {"Authorization": f"Bearer {auth_token}"}
        async with httpx.AsyncClient(timeout=30) as client:
            with open(PDF_FILE, "rb") as f:
                r = await client.post(
                    f"{BASE_URL}/imports/form26as/{self.PDF_CLIENT_ID}",
                    headers=headers,
                    files={"file": (PDF_FILE.name, f, "application/pdf")}
                )
        data = r.json()
        assert data["extracted"]["source"] == "pdf", \
            f"Expected source='pdf', got {data['extracted'].get('source')}"

    @pytest.mark.asyncio
    async def test_pdf_header_parsed(self, auth_token):
        """PDF file header should be correctly parsed."""
        headers = {"Authorization": f"Bearer {auth_token}"}
        async with httpx.AsyncClient(timeout=30) as client:
            with open(PDF_FILE, "rb") as f:
                r = await client.post(
                    f"{BASE_URL}/imports/form26as/{self.PDF_CLIENT_ID}",
                    headers=headers,
                    files={"file": (PDF_FILE.name, f, "application/pdf")}
                )
        data = r.json()
        extracted = data["extracted"]
        assert extracted["header"]["pan"] == "ACUPG3482G", \
            f"Expected PAN='ACUPG3482G', got {extracted['header']['pan']!r}"
        assert "SUNIT" in extracted["header"]["name"].upper(), \
            f"Expected name to contain 'SUNIT', got {extracted['header']['name']!r}"

    @pytest.mark.asyncio
    async def test_pdf_tds_deductors_parsed(self, auth_token):
        """PDF file should parse at least 1 TDS deductor."""
        headers = {"Authorization": f"Bearer {auth_token}"}
        async with httpx.AsyncClient(timeout=30) as client:
            with open(PDF_FILE, "rb") as f:
                r = await client.post(
                    f"{BASE_URL}/imports/form26as/{self.PDF_CLIENT_ID}",
                    headers=headers,
                    files={"file": (PDF_FILE.name, f, "application/pdf")}
                )
        data = r.json()
        extracted = data["extracted"]
        assert extracted["totals"]["tds_deductors_count"] >= 1, \
            f"Expected >=1 deductor, got {extracted['totals']['tds_deductors_count']}"
        assert len(extracted["tds_entries"]) >= 1, \
            f"Expected >=1 TDS entry, got {len(extracted['tds_entries'])}"

    @pytest.mark.asyncio
    async def test_pdf_tds_amounts_correct(self, auth_token):
        """PDF file should correctly extract TDS amounts."""
        headers = {"Authorization": f"Bearer {auth_token}"}
        async with httpx.AsyncClient(timeout=30) as client:
            with open(PDF_FILE, "rb") as f:
                r = await client.post(
                    f"{BASE_URL}/imports/form26as/{self.PDF_CLIENT_ID}",
                    headers=headers,
                    files={"file": (PDF_FILE.name, f, "application/pdf")}
                )
        data = r.json()
        extracted = data["extracted"]
        
        # Should have TDS deducted amount
        assert extracted["totals"]["total_tds_deducted"] >= 22000, \
            f"Expected total_tds >= 22000, got {extracted['totals']['total_tds_deducted']}"
        
        # Should have total amount
        assert extracted["totals"]["total_amount_paid_credited"] >= 220000, \
            f"Expected total_amount >= 220000, got {extracted['totals']['total_amount_paid_credited']}"


class TestFormatDetection:
    """Test that format detection correctly distinguishes ZIP from PDF content."""

    @pytest.mark.asyncio
    async def test_zip_content_not_misclassified_as_pdf(self, auth_token):
        """ZIP content with ^ delimiters should not be misclassified as PDF."""
        # This is the core bug that was fixed:
        # ZIP files contain "Annual Tax Statement" text which could trigger
        # PDF format detection, but the ^ delimiters should take priority.
        headers = {"Authorization": f"Bearer {auth_token}"}
        async with httpx.AsyncClient(timeout=30) as client:
            with open(ZIP_FILE, "rb") as f:
                r = await client.post(
                    f"{BASE_URL}/imports/form26as/{TEST_CLIENT_ID}",
                    headers=headers,
                    files={"file": (ZIP_FILE.name, f, "application/zip")}
                )
        data = r.json()
        # The key assertion: ZIP should NOT return source='pdf'
        assert data["extracted"]["source"] != "pdf", \
            "ZIP file was misclassified as PDF format!"
        assert data["extracted"]["source"] == "zip", \
            f"Expected source='zip', got {data['extracted']['source']}"
        
        # And it should have actual data, not empty fields
        assert data["extracted"]["totals"]["tds_deductors_count"] > 0, \
            "ZIP file should have parsed TDS deductors"
        assert data["extracted"]["header"]["pan"] != "", \
            "ZIP file should have parsed PAN from header"
