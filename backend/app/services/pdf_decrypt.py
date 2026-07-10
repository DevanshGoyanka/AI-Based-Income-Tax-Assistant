"""PDF Decryption and Table Extraction for ITD AIS/TIS/26AS documents.

PDF types supported:
  - AIS PDF     — password = PAN(lowercase) + DDMMYYYY
  - TIS PDF     — same password scheme
  - Form 26AS   — same password scheme

All three PDFs have bordered tables. We use camelot-py for precise table
extraction, with pdfplumber as a faster fallback. Plain pypdf text
extraction + regex is the final fallback.

Tables we need to extract from AIS PDF:
  - TDS on Salary
  - TDS on Other than Salary
  - Interest from Banks
  - Interest from Others
  - Dividends
  - Sale of Immovable Property
  - Foreign Remittance / 15CA/15CB
  - Other information

Tables we need to extract from TIS PDF:
  - TDS Details
  - Tax Payments (Advance tax, Self-assessment tax)
  - Interest Income
  - Dividend Income
"""
from __future__ import annotations

import re
import logging
from dataclasses import dataclass, field
from io import BytesIO
from typing import Literal

from pypdf import PdfReader

logger = logging.getLogger(__name__)


class PDFDecryptionError(ValueError):
    """Raised when PDF decryption fails."""


# ── Camelot-based table extractor ────────────────────────────────────────────

_camelot_available: bool | None = None


def _camelot_available() -> bool:
    global _camelot_available
    if _camelot_available is not None:
        return _camelot_available
    try:
        import camelot  # noqa: F401
        _camelot_available = True
    except ImportError:
        _camelot_available = False
    return _camelot_available


# ── pdfplumber-based table extractor ─────────────────────────────────────────

_pdfplumber_available: bool | None = None


def _pdfplumber_available() -> bool:
    global _pdfplumber_available
    if _pdfplumber_available is not None:
        return _pdfplumber_available
    try:
        import pdfplumber  # noqa: F401
        _pdfplumber_available = True
    except ImportError:
        _pdfplumber_available = False
    return _pdfplumber_available


def _safe_int(val) -> int:
    """Convert a value to integer rupees, stripping commas and paise."""
    if val is None or val == "":
        return 0
    if isinstance(val, (int, float)):
        return int(val)
    s = str(val).strip()
    s = re.sub(r"[^\d.\-]", "", s)
    if not s or s == "-":
        return 0
    try:
        return int(float(s))
    except (ValueError, TypeError):
        return 0


# ─────────────────────────────────────────────────────────────────────────────


def _build_pdf_password(pan: str, dob: str) -> str:
    """Build ITD PDF password: PAN(lowercase) + DDMMYYYY."""
    return f"{pan.strip().lower()}{dob.strip()}"


def _try_decrypt(reader: PdfReader, password: str) -> bool:
    try:
        return reader.decrypt(password) > 0
    except Exception:
        return False


def _decrypt_pdf(pdf_bytes: bytes, pan: str, dob: str) -> PdfReader:
    """Decrypt a PDF and return a PdfReader.

    Raises PDFDecryptionError on failure.
    """
    password = _build_pdf_password(pan, dob)
    reader = PdfReader(BytesIO(pdf_bytes))
    if reader.is_encrypted:
        for pwd in [password, pan.lower(), pan.lower() + dob]:
            if _try_decrypt(reader, pwd):
                return reader
        raise PDFDecryptionError(
            f"Failed to decrypt PDF. Check PAN and DOB. "
            f"Password format: PAN(lower) + DDMMYYYY (e.g. {pan.lower()}{dob})"
        )
    return reader


# ── Table extraction strategies ───────────────────────────────────────────────


def _extract_tables_camelot(
    pdf_bytes: bytes,
    document_type: Literal["AIS", "TIS", "26AS"],
) -> list[dict]:
    """Extract tables using camelot-py (best accuracy for bordered tables)."""
    import camelot

    flavor = "lattice"  # bordered tables → lattice mode
    tables_per_page: list[dict] = []

    try:
        # camelot reads from a file path; pass pdf_bytes as a temp file
        import tempfile, os
        with tempfile.NamedTemporaryFile(suffix=".pdf", delete=False) as tmp:
            tmp.write(pdf_bytes)
            tmp_path = tmp.name

        try:
            # Read all pages at once
            camelot_tables = camelot.read_pdf(tmp_path, pages="all", flavor=flavor)
            logger.info(
                "camelot extracted %d tables across %d pages for %s",
                len(camelot_tables), len(camelot_tables.pages), document_type
            )

            for table_idx, table in enumerate(camelot_tables):
                df = table.df
                if df.empty:
                    continue
                tables_per_page.append({
                    "page": table_idx + 1,
                    "data": _normalise_table(df),
                    "headers": list(df.columns),
                    "shape": df.shape,
                })
        finally:
            os.unlink(tmp_path)

    except Exception as e:
        logger.warning("camelot extraction failed: %s", e)
        raise

    return tables_per_page


def _extract_tables_pdfplumber(
    pdf_bytes: bytes,
    document_type: Literal["AIS", "TIS", "26AS"],
    password: str | None = None,
) -> list[dict]:
    """Extract tables using pdfplumber (fast, good for clean PDFs)."""
    import pdfplumber

    tables_per_page: list[dict] = []
    
    # pdfplumber needs decrypted PDF - try with password if encrypted
    with pdfplumber.open(BytesIO(pdf_bytes), password=password) as pdf:
        for page_num, page in enumerate(pdf.pages, start=1):
            try:
                extracted_tables = page.extract_tables()
            except Exception as e:
                logger.warning("pdfplumber page %d failed: %s", page_num, e)
                continue

            for t_idx, table in enumerate(extracted_tables):
                if not table:
                    continue
                rows = [row for row in table if any(c is not None for c in row)]
                if len(rows) < 2:
                    continue
                # First row = headers (may need cleaning)
                headers = [str(c or "").strip() for c in rows[0]]
                data_rows = rows[1:]
                tables_per_page.append({
                    "page": page_num,
                    "table_idx": t_idx,
                    "headers": headers,
                    "data": data_rows,
                    "shape": (len(data_rows), len(headers)),
                })

    logger.info(
        "pdfplumber extracted %d tables for %s",
        len(tables_per_page), document_type
    )
    return tables_per_page


def _normalise_table(df) -> list[list[str]]:
    """Convert a camelot DataFrame to a plain list of rows."""
    return [list(row) for row in df.itertuples(index=False)]


# ── AIS table parsing ─────────────────────────────────────────────────────────


def _parse_ais_tables(tables: list[dict]) -> dict:
    """Parse AIS PDF tables into structured dict.

    AIS PDF tables typically look like:
      Page 1: Header + TDS on Salary
      Page 2: TDS on Other than Salary
      Page 3: Interest / Dividends
      ...

    Table headers are like:
      ["Sr No", "TAN of Deductor", "Name of Deductor", "Section", "Amount Paid",
       "Tax Deducted", "TDS Deposited", "Date of Credit/Tax Dedn", "..."]

    We normalise each row to a dict using header positions.
    """
    result: dict = {
        "pan": None,
        "assessment_year": None,
        "assessee_name": None,
        "tds_salary": [],
        "tds_others": [],
        "interest_bank": [],
        "interest_others": [],
        "dividends": [],
        "dividends_mf": [],
        "property_sales": [],
        "foreign_remittances": [],
        "trust_benefits": [],
        "lottery": [],
        "other": [],
    }

    for table in tables:
        page = table.get("page", "?")
        headers = [h.lower().strip() for h in table.get("headers", [])]
        data = table.get("data", [])

        # ── Detect table type from headers ─────────────────────────────────
        headers_j = " ".join(headers)

        if not headers:
            continue

        # Try to detect PAN/AY from header row (sometimes header is on data row 0)
        if any("pan" in h for h in headers) or len(headers) < 5:
            # May be a header/info row — look for PAN on data rows
            for row in data[:3]:
                for cell in row:
                    pm = re.search(r"\b([A-Z]{5}\d{4}[A-Z])\b", str(cell))
                    if pm and not result["pan"]:
                        result["pan"] = pm.group(1)
                    am = re.search(r"AY[:\s]*(\d{4}-\d{2})", str(cell), re.IGNORECASE)
                    if am and not result["assessment_year"]:
                        result["assessment_year"] = am.group(1)

        # ── Identify section from content ──────────────────────────────────
        section: str | None = None
        if re.search(r"tds.*salary|salary.*tds", headers_j, re.IGNORECASE):
            section = "tds_salary"
        elif re.search(r"tds.*other|other.*tds|non.?salary", headers_j, re.IGNORECASE):
            section = "tds_others"
        elif re.search(r"interest.*bank|bank.*interest|saving|fdr|fixed deposit",
                       headers_j, re.IGNORECASE):
            section = "interest_bank"
        elif re.search(r"interest.*(?!bank)|other.*interest", headers_j, re.IGNORECASE):
            section = "interest_others"
        elif re.search(r"dividend.*mutual|mutual.*dividend|mf", headers_j, re.IGNORECASE):
            section = "dividends_mf"
        elif re.search(r"dividend", headers_j, re.IGNORECASE):
            section = "dividends"
        elif re.search(r"property|sale.*immovable|immovable.*sale", headers_j, re.IGNORECASE):
            section = "property_sales"
        elif re.search(r"foreign|remittance|15ca|15cb", headers_j, re.IGNORECASE):
            section = "foreign_remittances"
        elif re.search(r"trust|society", headers_j, re.IGNORECASE):
            section = "trust_benefits"
        elif re.search(r"lottery|crossword|horse.*race|115bb", headers_j, re.IGNORECASE):
            section = "lottery"
        else:
            # Default: treat as TDS others
            section = "tds_others"

        # ── Parse data rows ────────────────────────────────────────────────
        target = result.get(section, [])
        for row_idx, row in enumerate(data):
            if not row or all(str(c).strip() in ("", "nan", "None") for c in row):
                continue

            row_s = [str(c).strip() if c is not None else "" for c in row]

            entry = _parse_row_as_ais(row_s, headers, section, page, row_idx)
            if entry:
                target.append(entry)

    return result


def _parse_row_as_ais(
    row: list[str],
    headers: list[str],
    section: str,
    page: int,
    row_idx: int,
) -> dict | None:
    """Parse a single AIS table row into a dict.

    We identify columns by header keywords rather than fixed positions,
    since column order can vary between years.
    """
    n = len(row)
    # Build header→index map
    hmap: dict[str, int] = {}
    for i, h in enumerate(headers):
        h_clean = re.sub(r"\s+", " ", h).strip().lower()
        if h_clean:
            hmap[h_clean] = i

    def _v(*header_variants) -> str:
        for hv in header_variants:
            h_lower = hv.lower().strip()
            for key, idx in hmap.items():
                if h_lower in key or key in h_lower:
                    return row[idx].strip() if idx < n else ""
        return ""

    def _i(*header_variants) -> int:
        return _safe_int(_v(*header_variants))

    entry: dict = {}

    if section in ("tds_salary", "tds_others"):
        tan = _v("tan", "tax deduction account number", "deductor tan")
        if len(tan) != 10:
            # Try to find TAN by pattern in any cell
            for cell in row:
                cm = re.search(r"\b([A-Z]{5}\d{4}[A-Z])\b", str(cell))
                if cm:
                    tan = cm.group(1)
                    break
        entry = {
            "tan": tan,
            "name": _v("name of deductor", "deductor name", "employer name", "name"),
            "section": _v("section", "section code", "sectioncode", "sectioncode").replace(" ", ""),
            "amount_paid": _i("amount paid", "amount credited", "gross amount", "amount"),
            "tax_deducted": _i("tax deducted", "tds", "tax deduct"),
            "tax_deposited": _i("tds deposited", "tax deposited", "tax dep"),
            "date_of_credit": _v("date of credit", "date of credit/tax dedn", "transaction date"),
            "receipt_no": _v("receipt no", "challan no", "challan serial no", "receiptnumber"),
            "status": _v("status", "remarks"),
            "deductor_pan": _v("deductor pan", "pan of deductor"),
        }
    elif section in ("interest_bank", "interest_others"):
        entry = {
            "bank_name": _v("bank name", "financial institute", "institution", "name"),
            "pan": _v("pan", "pan of deductor", "deductor pan"),
            "amount": _i("amount", "interest amount", "gross amount"),
            "type": _v("type", "nature"),
        }
    elif section in ("dividends", "dividends_mf"):
        entry = {
            "company_name": _v("company name", "mutual fund", "name of mutual fund", "name"),
            "pan": _v("pan", "pan of deductor"),
            "amount": _i("amount", "dividend amount", "gross amount"),
        }
    elif section == "property_sales":
        entry = {
            "property_details": _v("property details", "description", "name"),
            "pan": _v("pan", "buyer pan"),
            "amount": _i("amount", "sale consideration", "value"),
            "tax_deducted": _i("tax deducted", "tds"),
        }
    elif section == "foreign_remittances":
        entry = {
            "name": _v("name", "remittee name"),
            "pan": _v("pan", "remittee pan"),
            "amount": _i("amount", "remittance amount"),
            "tax_deducted": _i("tax deducted", "tds"),
            "section": _v("section", "sectioncode"),
        }
    elif section == "lottery":
        entry = {
            "description": _v("description", "nature", "name"),
            "amount": _i("amount", "winning amount", "gross amount"),
            "tax_deducted": _i("tax deducted", "tds", "115bb"),
        }
    else:
        # Generic catch-all
        entry = {
            f"col_{i}": cell
            for i, cell in enumerate(row)
            if cell and cell not in ("", "nan")
        }

    # Drop all-empty entries
    if not entry or all(v == "" or v == 0 for v in entry.values()):
        return None

    entry["_page"] = page
    entry["_row"] = row_idx
    return entry


# ── TIS table parsing ─────────────────────────────────────────────────────────


def _parse_tis_tables(tables: list[dict]) -> dict:
    """Parse TIS PDF tables into structured dict.

    TIS PDF tables:
      - TDS Details (TAN, Name, Section, Amount, Tax Deducted, etc.)
      - Tax Payments (BSR, Challan, Date, Tax Amount, Interest, etc.)
    """
    result: dict = {
        "pan": None,
        "assessment_year": None,
        "assessee_name": None,
        "tds": [],
        "tax_payments": [],
    }

    for table in tables:
        page = table.get("page", "?")
        headers = [h.lower().strip() for h in table.get("headers", [])]
        data = table.get("data", [])
        headers_j = " ".join(headers)

        # Detect type
        section: str | None = None
        if re.search(r"tax payment|advance tax|self.assessment|challan", headers_j, re.IGNORECASE):
            section = "tax_payments"
        else:
            section = "tds"

        for row_idx, row in enumerate(data):
            if not row:
                continue
            row_s = [str(c).strip() if c is not None else "" for c in row]
            if all(v in ("", "nan", "None") for v in row_s):
                continue

            entry = _parse_row_as_tis(row_s, headers, section, page, row_idx)
            if entry:
                result[section].append(entry)

    return result


def _parse_row_as_tis(
    row: list[str],
    headers: list[str],
    section: str,
    page: int,
    row_idx: int,
) -> dict | None:
    """Parse a single TIS table row."""
    n = len(row)
    hmap: dict[str, int] = {}
    for i, h in enumerate(headers):
        h_clean = re.sub(r"\s+", " ", h).strip().lower()
        if h_clean:
            hmap[h_clean] = i

    def _v(*hv_list) -> str:
        for hv in hv_list:
            h_lower = hv.lower().strip()
            for key, idx in hmap.items():
                if h_lower in key or key in h_lower:
                    return row[idx].strip() if idx < n else ""
        return ""

    def _i(*hv_list) -> int:
        return _safe_int(_v(*hv_list))

    entry: dict = {}

    if section == "tds":
        tan = _v("tan", "tax deduction account number")
        if len(tan) != 10:
            for cell in row:
                cm = re.search(r"\b([A-Z]{5}\d{4}[A-Z])\b", str(cell))
                if cm:
                    tan = cm.group(1)
                    break
        entry = {
            "tan": tan,
            "name": _v("name of deductor", "deductor name", "name"),
            "section": _v("section", "sectioncode").replace(" ", ""),
            "amount": _i("amount paid", "amount", "gross amount", "amount credited"),
            "tax_deducted": _i("tax deducted", "tds", "tax deduct"),
            "tax_deposited": _i("tax deposited", "tax deposited", "tax dep"),
            "date_of_credit": _v("date of credit", "date of credit/tax dedn", "transaction date"),
            "date_of_deposit": _v("date of deposit", "date of tax deposit", "deposit date"),
            "challan_no": _v("receipt no", "challan no", "challan serial no"),
            "quarter": _v("quarter", "q"),
            "status": _v("status", "remarks"),
        }
    elif section == "tax_payments":
        entry = {
            "bsr_code": _v("bsr", "bsr code", "bsrcode"),
            "challan_no": _v("challan no", "challan serial no", "serial no", "slno"),
            "date_of_deposit": _v("date of deposit", "date of tax deposit", "date"),
            "major_head": _v("major head", "majorhead", "tax type"),
            "minor_head": _v("minor head", "minorhead"),
            "tax_amount": _i("tax amount", "tax", "amount"),
            "interest_amount": _i("interest", "interest amount"),
            "total_amount": _i("total amount", "total"),
        }

    if not entry or all(v == "" or v == 0 for v in entry.values()):
        return None

    entry["_page"] = page
    entry["_row"] = row_idx
    return entry


# ── 26AS table parsing ────────────────────────────────────────────────────────


def _parse_26as_tables(tables: list[dict]) -> dict:
    """Parse Form 26AS PDF tables."""
    result: dict = {
        "pan": None,
        "assessment_year": None,
        "tds_salary": [],
        "tds_others": [],
        "tax_payments": [],
    }

    for table in tables:
        page = table.get("page", "?")
        headers = [h.lower().strip() for h in table.get("headers", [])]
        data = table.get("data", [])
        headers_j = " ".join(headers)

        section = "tds_others"
        if re.search(r"salary", headers_j, re.IGNORECASE):
            section = "tds_salary"
        elif re.search(r"tax payment|challan", headers_j, re.IGNORECASE):
            section = "tax_payments"

        for row_idx, row in enumerate(data):
            if not row:
                continue
            row_s = [str(c).strip() if c is not None else "" for c in row]
            if all(v in ("", "nan") for v in row_s):
                continue

            entry = _parse_row_as_tis(row_s, headers, section, page, row_idx)
            if entry:
                result[section].append(entry)

    return result


# ── Public API ────────────────────────────────────────────────────────────────


@dataclass
class PDFParseResult:
    """Result of parsing an ITD PDF."""
    document_type: str
    pages: int
    is_encrypted: bool
    tables: list[dict]  # raw tables before parsing
    parsed: dict        # parsed into TDS/info dicts
    errors: list[str] = field(default_factory=list)

    @property
    def text(self) -> str:
        return ""  # tables-only, no raw text returned


def decrypt_and_extract_tables(
    pdf_bytes: bytes,
    pan: str,
    dob: str,
    document_type: Literal["AIS", "TIS", "26AS"] = "AIS",
) -> PDFParseResult:
    """Decrypt an ITD PDF and extract all tables.

    Extraction priority:
      1. camelot-py (lattice mode) — best for bordered tables
      2. pdfplumber — fallback
      3. pypdf text + regex — last resort

    Args:
        pdf_bytes: Raw bytes of the PDF.
        pan: 10-char PAN (used to derive password).
        dob: Date of birth in DDMMYYYY format.
        document_type: "AIS", "TIS", or "26AS".

    Returns:
        PDFParseResult with raw tables and parsed data.

    Raises:
        PDFDecryptionError: If the PDF can't be decrypted.
    """
    errors: list[str] = []

    # Build password for encrypted PDF access
    password = _build_pdf_password(pan, dob)

    # ── 1. Decrypt ──────────────────────────────────────────────────────────
    try:
        reader = _decrypt_pdf(pdf_bytes, pan, dob)
    except PDFDecryptionError:
        raise
    except Exception as e:
        raise PDFDecryptionError(f"PDF read error: {e}") from e

    is_encrypted = reader.is_encrypted
    pages = len(reader.pages)

    # ── 2. Extract tables ───────────────────────────────────────────────────
    raw_tables: list[dict] = []
    extractor_used = "none"

    # Try camelot first (best accuracy for bordered ITD tables)
    if _camelot_available():
        try:
            raw_tables = _extract_tables_camelot(pdf_bytes, document_type)
            extractor_used = "camelot"
        except Exception as e:
            errors.append(f"camelot failed: {e}")
            raw_tables = []

    # Fallback to pdfplumber (pass password for encrypted PDFs)
    if not raw_tables and _pdfplumber_available():
        try:
            raw_tables = _extract_tables_pdfplumber(pdf_bytes, document_type, password=password)
            extractor_used = "pdfplumber"
        except Exception as e:
            errors.append(f"pdfplumber failed: {e}")
            raw_tables = []

    # Last resort: pypdf text + regex
    if not raw_tables:
        logger.warning("Table extraction failed for %s, falling back to text regex", document_type)
        try:
            text_parts = []
            for page in reader.pages:
                t = page.extract_text() or ""
                if t:
                    text_parts.append(t)
            text = "\n".join(text_parts)
            raw_tables = _text_to_tables_fallback(text, document_type)
            extractor_used = "pypdf_text"
            errors.append("Used text extraction (camelot/pdfplumber unavailable). Table data may be incomplete.")
        except Exception as e:
            errors.append(f"Text fallback failed: {e}")

    logger.info(
        "Extracted %d tables from %s using %s (%d pages)",
        len(raw_tables), document_type, extractor_used, pages
    )

    # ── 3. Parse tables ─────────────────────────────────────────────────────
    if document_type == "AIS":
        # Use specialized AIS parser
        from app.services.ais_pdf_parser import parse_ais_pdf_tables
        text_parts = []
        for page in reader.pages:
            t = page.extract_text() or ""
            if t:
                text_parts.append(t)
        full_text = "\n".join(text_parts)
        parsed = parse_ais_pdf_tables(raw_tables, full_text)
    elif document_type == "TIS":
        parsed = _parse_tis_tables(raw_tables)
    else:
        parsed = _parse_26as_tables(raw_tables)

    return PDFParseResult(
        document_type=document_type,
        pages=pages,
        is_encrypted=is_encrypted,
        tables=raw_tables,
        parsed=parsed,
        errors=errors,
    )


def _text_to_tables_fallback(text: str, document_type: str) -> list[dict]:
    """Extract structured data from raw PDF text using regex.

    Used when table extractors are unavailable. Parses the text for
    common ITD patterns (TAN, amounts, etc.) and returns pseudo-table rows.
    """
    # PAN
    pan_m = re.search(r"\b([A-Z]{5}\d{4}[A-Z])\b", text)
    # AY
    ay_m = re.search(r"AY[:\s]*(\d{4}-\d{2})", text, re.IGNORECASE)

    # Find all TAN + amount patterns
    tan_pattern = re.compile(
        r"([A-Z]{5}\d{4}[A-Z])\s+(.{5,60}?)\s+(\d[\d,]+)\s+(\d[\d,]+)",
        re.MULTILINE
    )

    rows = []
    for m in tan_pattern.finditer(text):
        tan, name, amount_str, tax_str = m.groups()
        rows.append([tan, name.strip(), "", _safe_int(amount_str), _safe_int(tax_str)])

    if not rows:
        # Try more generic pattern
        rows_pattern = re.compile(r"([A-Z]{5}\d{4}[A-Z]).*?(\d[\d,]+).*?(\d[\d,]+)")
        for m in rows_pattern.finditer(text):
            rows.append([m.group(1), "", "", _safe_int(m.group(2)), _safe_int(m.group(3))])

    return [{
        "page": 1,
        "headers": ["TAN", "Name", "Section", "Amount", "Tax"],
        "data": rows,
        "source": "pypdf_text_fallback",
    }]
