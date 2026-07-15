"""TIS PDF Parser - Extract data from password-protected TIS PDFs.

TIS (Tax Information Statement) consolidated view of AIS + 26AS data.
Password format: PAN(lowercase) + DOB(DDMMYYYY)

TIS Structure:
- Page 1: Summary table (category totals only)
- Page 2+: Detailed nested tables:
  * Row 1: Category header (SR.NO | CATEGORY | ... | TOTAL)
  * Row 2: Sub-headers (SR.NO | PART | DESC | SOURCE | AMT_DESC | REPORTED | PROCESSED | ACCEPTED)
  * Row 3+: Data rows with actual records
"""
import re
import logging
from decimal import Decimal
from typing import List, Dict, Any
from io import BytesIO

try:
    import pdfplumber
    PDFPLUMBER_AVAILABLE = True
except ImportError:
    PDFPLUMBER_AVAILABLE = False

from pypdf import PdfReader
from app.core.domain.canonical_tis import (
    CanonicalTIS,
    CanonicalTISTDS,
    CanonicalTISInterest,
    CanonicalTISDividend,
    CanonicalTISCapitalGains,
    CanonicalTISPersonalInfo
)

logger = logging.getLogger(__name__)


class TISPDFParseError(Exception):
    pass


class TISPDFParser:
    
    def parse(self, pdf_bytes: bytes, pan: str, dob: str) -> CanonicalTIS:
        password = f"{pan.strip().lower()}{dob.strip()}"
        
        reader = self._decrypt_pdf(pdf_bytes, password)
        full_text = self._extract_text(reader)
        
        canonical = CanonicalTIS(
            personal_info=self._extract_personal_info(full_text, pan),
            assessment_year=self._extract_ay(full_text),
            source_format="pdf"
        )
        
        if PDFPLUMBER_AVAILABLE:
            tables = self._extract_tables_pdfplumber(pdf_bytes, password)
            logger.info(f"Extracted {len(tables)} tables total")
            self._parse_tables(tables, canonical)
        else:
            logger.warning("pdfplumber not available")
        
        return canonical
    
    def _decrypt_pdf(self, pdf_bytes: bytes, password: str) -> PdfReader:
        reader = PdfReader(BytesIO(pdf_bytes))
        if reader.is_encrypted:
            if reader.decrypt(password) == 0:
                raise TISPDFParseError("Failed to decrypt TIS PDF")
        return reader
    
    def _extract_text(self, reader: PdfReader) -> str:
        return "\n".join([page.extract_text() or "" for page in reader.pages])
    
    def _extract_tables_pdfplumber(self, pdf_bytes: bytes, password: str) -> List[Dict]:
        tables = []
        with pdfplumber.open(BytesIO(pdf_bytes), password=password) as pdf:
            for page_num, page in enumerate(pdf.pages, start=1):
                try:
                    for table in page.extract_tables():
                        if table and len(table) > 2:
                            tables.append({"page": page_num, "rows": table})
                except Exception as e:
                    logger.warning(f"Page {page_num} extraction failed: {e}")
        return tables
    
    def _extract_personal_info(self, text: str, pan: str) -> CanonicalTISPersonalInfo:
        name_match = re.search(r"Name of Assessee\s*\n\s*([A-Z\s]+)", text, re.IGNORECASE)
        name = name_match.group(1).strip() if name_match else ""
        
        dob_match = re.search(r"Date of Birth[:\s]+(\d{2}[/-]\d{2}[/-]\d{4})", text, re.IGNORECASE)
        dob = dob_match.group(1) if dob_match else ""
        
        return CanonicalTISPersonalInfo(pan=pan.upper(), name=name, dob=dob)
    
    def _extract_ay(self, text: str) -> str:
        ay_match = re.search(r"Assessment Year[:\s]+(\d{4}-\d{2})", text, re.IGNORECASE)
        return ay_match.group(1) if ay_match else ""
    
    def _parse_tables(self, tables: List[Dict], canonical: CanonicalTIS):
        """Parse all tables from TIS PDF."""
        logger.info(f"Parsing {len(tables)} tables")
        
        for idx, table in enumerate(tables):
            rows = table["rows"]
            
            if len(rows) < 3:
                logger.debug(f"Table {idx}: skipped (only {len(rows)} rows)")
                continue
            
            # Check row[1] (first data row) for category - row[0] might be category number row
            category_row = rows[1] if len(rows) > 1 else rows[0]
            category_text = " ".join([str(c or "").lower() for c in category_row])
            
            logger.debug(f"Table {idx}: category='{category_text[:80]}'")
            
            if "salary" in category_text:
                logger.info(f"Table {idx}: parsing as SALARY")
                self._parse_salary_section(rows, canonical)
            elif "interest from savings" in category_text:
                logger.info(f"Table {idx}: parsing as INTEREST SAVINGS")
                self._parse_interest_section(rows, canonical)
            elif "interest from deposit" in category_text or "interest other than" in category_text:
                logger.info(f"Table {idx}: parsing as INTEREST DEPOSIT")
                self._parse_interest_section(rows, canonical)
            elif "section 194a" in category_text:
                logger.info(f"Table {idx}: parsing as TDS INTEREST")
                self._parse_tds_interest_section(rows, canonical)
            elif "dividend" in category_text:
                logger.info(f"Table {idx}: parsing as DIVIDEND")
                self._parse_dividend_section(rows, canonical)
            elif "sale of securities" in category_text or "sale of" in category_text and "mutual fund" in category_text:
                logger.info(f"Table {idx}: parsing as CAPITAL GAINS SALE")
                self._parse_capital_gains_section(rows, canonical)
            elif "purchase of securities" in category_text or "purchase of" in category_text and "mutual fund" in category_text:
                logger.info(f"Table {idx}: parsing as CAPITAL GAINS PURCHASE")
                self._parse_capital_gains_section(rows, canonical)
            else:
                logger.debug(f"Table {idx}: no matching category")
    
    def _parse_salary_section(self, rows: List, canonical: CanonicalTIS):
        """Parse Salary section rows.
        Data starts at row index 3 (after header, category, sub-header).
        Columns: [SR, PART, DESC, SOURCE, AMT_DESC, REPORTED, _, PROCESSED, ACCEPTED, _]
        """
        for i in range(3, len(rows)):
            row = rows[i]
            
            if not row or len(row) < 9:
                continue
            
            row_text = " ".join([str(c or "").lower() for c in row])
            
            if "salary" not in row_text:
                continue
            
            source = str(row[3] or "").strip()
            tan_match = re.search(r'\(([A-Z]{4}\d{5}[A-Z])\)', source)
            
            if not tan_match:
                continue
            
            tan = tan_match.group(1)
            name = re.sub(r'\s*\([A-Z]{4}\d{5}[A-Z]\)', '', source).strip().replace('\n', ' ')
            
            amount = self._to_decimal(str(row[5] or ""))
            
            if amount > 0:
                canonical.tds_records.append(CanonicalTISTDS(
                    deductor_name=name,
                    deductor_tan=tan,
                    section_code="192",
                    amount_paid=amount,
                    tax_deducted=Decimal("0"),
                    tds_deposited=Decimal("0"),
                    source="tis"
                ))
    
    def _parse_interest_section(self, rows: List, canonical: CanonicalTIS):
        """Parse Interest from savings/deposit section.
        Data starts at row index 3.
        """
        for i in range(3, len(rows)):
            row = rows[i]
            
            if not row or len(row) < 9:
                continue
            
            row_text = " ".join([str(c or "").lower() for c in row])
            
            # Skip rows that don't have interest-related content
            if not any(kw in row_text for kw in ["interest", "sft", "tds/tcs", "tds", "section 194a"]):
                continue
            
            source = str(row[3] or "").strip()
            amount = self._to_decimal(str(row[5] or ""))
            
            if amount <= 0:
                continue
            
            # Extract TAN or PAN from source
            tan_match = re.search(r'\(([A-Z]{4}\d{5}[A-Z])\)', source)
            pan_match = re.search(r'\(([A-Z]{5}\d{4}[A-Z])', source)
            
            identifier = tan_match.group(1) if tan_match else (pan_match.group(1) if pan_match else None)
            name = re.sub(r'\s*\([A-Z0-9.]+\)', '', source).strip().replace('\n', ' ')
            
            # If it's TDS/TCS (194A), add to TDS records
            if "tds" in row_text or "194a" in row_text:
                if tan_match:
                    canonical.tds_records.append(CanonicalTISTDS(
                        deductor_name=name,
                        deductor_tan=identifier,
                        section_code="194A",
                        amount_paid=amount,
                        tax_deducted=Decimal("0"),
                        tds_deposited=Decimal("0"),
                        source="tis"
                    ))
            
            # Always add to interest records
            canonical.interest_records.append(CanonicalTISInterest(
                payer_name=name,
                amount=amount,
                payer_pan=identifier if pan_match else None,
                source="tis"
            ))
    
    def _parse_tds_interest_section(self, rows: List, canonical: CanonicalTIS):
        """Parse TDS Interest (194A) section - same as interest section."""
        self._parse_interest_section(rows, canonical)
    
    def _parse_dividend_section(self, rows: List, canonical: CanonicalTIS):
        """Parse Dividend section.
        Data starts at row index 3.
        """
        for i in range(3, len(rows)):
            row = rows[i]
            
            if not row or len(row) < 9:
                continue
            
            row_text = " ".join([str(c or "").lower() for c in row])
            
            if "dividend" not in row_text:
                continue
            
            source = str(row[3] or "").strip()
            amount = self._to_decimal(str(row[5] or ""))
            
            if amount <= 0:
                continue
            
            pan_match = re.search(r'\(([A-Z]{5}\d{4}[A-Z])', source)
            pan = pan_match.group(1) if pan_match else None
            
            name = re.sub(r'\s*\([A-Z0-9.]+\)', '', source).strip().replace('\n', ' ')
            
            canonical.dividend_records.append(CanonicalTISDividend(
                payer_name=name,
                amount=amount,
                payer_pan=pan,
                source="tis"
            ))
    
    def _parse_capital_gains_section(self, rows: List, canonical: CanonicalTIS):
        """Parse Capital Gains (Sale/Purchase of securities) section.
        Data starts at row index 3.
        """
        # Determine transaction type from category row
        category_text = " ".join([str(c or "").lower() for c in rows[1]])
        transaction_type = "sale" if "sale" in category_text else "purchase"
        
        for i in range(3, len(rows)):
            row = rows[i]
            
            if not row or len(row) < 9:
                continue
            
            row_text = " ".join([str(c or "").lower() for c in row])
            
            # Look for SFT or security-related keywords
            if not any(kw in row_text for kw in ["sft", "security", "securities", "equity", "mutual fund", "depository"]):
                continue
            
            source = str(row[3] or "").strip()
            description = str(row[2] or "").strip().replace('\n', ' ')
            amount = self._to_decimal(str(row[5] or ""))
            
            if amount <= 0:
                continue
            
            pan_match = re.search(r'\(([A-Z]{5}\d{4}[A-Z])', source)
            pan = pan_match.group(1) if pan_match else None
            
            name = re.sub(r'\s*\([A-Z0-9.]+\)', '', source).strip().replace('\n', ' ')
            
            canonical.capital_gains_records.append(CanonicalTISCapitalGains(
                transaction_type=transaction_type,
                source_name=name,
                description=description,
                amount=amount,
                source_pan=pan,
                source="tis"
            ))
    
    def _to_decimal(self, value: str) -> Decimal:
        """Convert string to Decimal."""
        if not value:
            return Decimal("0")
        cleaned = re.sub(r'[^\d.-]', '', value)
        if not cleaned or cleaned == '-':
            return Decimal("0")
        try:
            return Decimal(cleaned)
        except:
            return Decimal("0")
