"""Form 26AS importer - password-protected ZIP or PDF.

Parses ALL 10 parts of Form 26AS as per ITD Annual Tax Statement schema:
  PART-I    : Details of Tax Deducted at Source (TDS by deductor)
  PART-II   : TDS for 15G / 15H declarations
  PART-III  : Transactions u/s 194B / 194R / 194S / 194BA
  PART-IV   : TDS u/s 194IA / 194IB / 194M / 194S (seller/landlord/contractor/professional/VDA)
  PART-V    : Transactions u/s 194S as per Form-26QE (VDA)
  PART-VI   : Tax Collected at Source (TCS)
  PART-VII  : Paid Refunds
  PART-VIII : TDS u/s 194IA / 194IB / 194M / 194S (buyer/tenant)
  PART-IX   : Transactions / Demand Payments u/s 194S (26QE)
  PART-X    : TDS / TCS Defaults (Processing of Statements)

Supports TWO input formats:
  1. ZIP format  : ^-delimited TXT file (from TRACES)
  2. PDF format  : Tabular PDF (annual tax statement from e-filing portal)
"""
import io
import re
from typing import BinaryIO
from uuid import UUID
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, delete
import pyzipper


# ---------------------------------------------------------------------------
# Part header schema definitions (column names for each part)
# ---------------------------------------------------------------------------

PART1_HEADERS = ["sr_no", "deductor_name", "tan", "amount_paid_credited", "tax_deducted", "tds_deposited"]
PART1_TXN_HEADERS = ["sr_no", "section", "transaction_date", "status_of_booking",
                     "date_of_booking", "remarks", "amount", "tax_deducted", "tds_deposited"]

PART2_HEADERS = ["sr_no", "deductor_name", "tan", "amount_paid_credited", "tax_deducted", "tds_deposited"]

PART3_HEADERS = ["sr_no", "acknowledgement_number", "name_of_deductor", "pan_of_deductor",
                 "total_amount_paid_credited"]

PART4_HEADERS = ["sr_no", "acknowledgement_number", "name_of_deductor", "pan_of_deductor",
                 "transaction_date", "total_transaction_amount", "total_tds_deposited"]

PART5_HEADERS = ["sr_no", "acknowledgement_number", "name_of_buyer", "pan_of_buyer",
                 "transaction_date", "total_transaction_amount"]

PART6_HEADERS = ["sr_no", "name_of_collector", "tan_of_collector", "amount_paid_debited",
                 "tax_collected", "tcs_deposited"]

PART7_HEADERS = ["sr_no", "assessment_year", "mode", "refund_issued", "nature_of_refund",
                 "amount_of_refund", "interest", "date_of_payment", "remarks"]

PART8_HEADERS = ["sr_no", "acknowledgement_number", "name_of_deductee", "pan_of_deductee",
                 "transaction_date", "total_transaction_amount", "total_tds_deposited",
                 "total_amount_deposited_other_than_tds"]

PART9_HEADERS = ["sr_no", "acknowledgement_number", "name_of_seller", "pan_of_seller",
                 "transaction_date", "total_transaction_amount", "total_amount_deposited_other_than_tds"]

PART10_HEADERS = ["sr_no", "financial_year", "short_payment", "short_deduction_collection",
                  "interest_on_tds_tcs_payments_default", "interest_on_tds_tcs_deduction_default",
                  "late_filing_fee_234e", "interest_u_s_220_2", "total_default"]


PART_SCHEMAS = {
    "PART-I":    {"deductor": PART1_HEADERS, "transaction": PART1_TXN_HEADERS},
    "PART-II":   {"deductor": PART2_HEADERS},
    "PART-III":  {"records": PART3_HEADERS},
    "PART-IV":   {"records": PART4_HEADERS},
    "PART-V":    {"records": PART5_HEADERS},
    "PART-VI":   {"collector": PART6_HEADERS},
    "PART-VII":  {"refunds": PART7_HEADERS},
    "PART-VIII": {"records": PART8_HEADERS},
    "PART-IX":   {"records": PART9_HEADERS},
    "PART-X":    {"defaults": PART10_HEADERS},
}


# ---------------------------------------------------------------------------
# PDF Table patterns - detected from actual Form 26AS PDF structure
# ---------------------------------------------------------------------------

# PART-I/II/VI Deductors/Collectors summary table pattern
# Example: "1 STATE BANK OF INDIA MUMS89569E 224329.00 22443.00 22443.00"
PART_SUMMARY_PATTERN = re.compile(
    r'^(\d+)\s+'                      # Sr. No.
    r'([A-Z][A-Z0-9\s\.\-\/\(\)&\',]+?)\s+'  # Name of deductor/collector
    r'([A-Z]{4}[0-9]{5}[A-Z])\s+'     # TAN
    r'([\d\,\.]+)\s+'                 # Amount
    r'([\d\,\.]+)\s+'                # Tax deducted/collected
    r'([\d\,\.]+)$'                  # TDS/TCS deposited
)

# PART-I Transaction detail table pattern
# Example: "1 194A 31-Mar-2025 F 23-May-2025 - 42822.00 4283.00 4283.00"
PART_TXN_PATTERN = re.compile(
    r'^(\d+)\s+'                      # Sr. No.
    r'(\d{4}[A-Z]?)\s+'              # Section
    r'(\d{2}-[A-Za-z]{3}-\d{4})\s+'  # Transaction Date
    r'([A-Z])\s+'                    # Status of Booking
    r'(\d{2}-[A-Za-z]{3}-\d{4})\s+'  # Date of Booking
    r'([^\d\s][^\s]*)\s+'            # Remarks (single char or word)
    r'([\d\-\,\.]+)\s+'              # Amount
    r'([\d\-\,\.]+)\s+'              # Tax deducted
    r'([\d\-\,\.]+)$'                # TDS deposited
)

# PART-IV/VIII Acknowledgement pattern
# Example: "1 ABCD1234E XYZ COMPANY ABCD1234E 31-Mar-2025 500000.00 50000.00"
PART_ACK_PATTERN = re.compile(
    r'^(\d+)\s+'                      # Sr. No.
    r'([A-Z0-9]+)\s+'                # Acknowledgement Number
    r'([A-Z][A-Z0-9\s\.\-\/\(\)&\']+?)\s+'  # Name
    r'([A-Z]{5}\d{4}[A-Z])\s+'       # PAN
    r'(\d{2}-[A-Za-z]{3}-\d{4})\s+'  # Transaction Date
    r'([\d\,\.]+)\s+'                # Amount
    r'([\d\,\.]+)$'                  # TDS deposited
)

# PART-V Acknowledgement pattern (no TDS deposited)
# Example: "1 ABCD1234E XYZ COMPANY ABCD1234E 31-Mar-2025 500000.00"
PART_V_ACK_PATTERN = re.compile(
    r'^(\d+)\s+'                      # Sr. No.
    r'([A-Z0-9]+)\s+'                # Acknowledgement Number
    r'([A-Z][A-Z0-9\s\.\-\/\(\)&\']+?)\s+'  # Name
    r'([A-Z]{5}\d{4}[A-Z])\s+'       # PAN
    r'(\d{2}-[A-Za-z]{3}-\d{4})\s+' # Transaction Date
    r'([\d\,\.]+)$'                  # Amount
)

# PART-IX pattern (different - no TDS deposited, has amount deposited other than TDS)
PART_IX_PATTERN = re.compile(
    r'^(\d+)\s+'                      # Sr. No.
    r'([A-Z0-9]+)\s+'                # Acknowledgement Number
    r'([A-Z][A-Z0-9\s\.\-\/\(\)&\']+?)\s+'  # Name
    r'([A-Z]{5}\d{4}[A-Z])\s+'       # PAN
    r'(\d{2}-[A-Za-z]{3}-\d{4})\s+' # Transaction Date
    r'([\d\,\.]+)$'                  # Total Amount
)

# PART-VII Refund pattern
# Example: "1 2025-26 R RFD456789 Income Tax Refund 10000.00 0.00 15-May-2025 -"
PART_REFUND_PATTERN = re.compile(
    r'^(\d+)\s+'                      # Sr. No.
    r'(\d{4}-\d{2})\s+'              # Assessment Year
    r'([A-Z])\s+'                    # Mode
    r'([A-Z0-9]+)\s+'                # Refund Issued
    r'([A-Za-z\s\/]+?)\s+'          # Nature of Refund
    r'([\d\,\.]+)\s+'               # Amount of Refund
    r'([\d\,\.]+)\s+'               # Interest
    r'(\d{2}-[A-Za-z]{3}-\d{4})\s+' # Date of Payment
    r'(.+)$'                         # Remarks
)

# PART-X Default pattern
# Example: "1 2024-25 0.00 0.00 0.00 0.00 0.00 0.00 0.00"
PART_DEFAULT_PATTERN = re.compile(
    r'^(\d+)\s+'                      # Sr. No.
    r'(\d{4}-\d{2})\s+'              # Financial Year
    r'([\d\,\.]+)\s+'                # Short Payment
    r'([\d\,\.]+)\s+'                # Short Deduction/Collection
    r'([\d\,\.]+)\s+'                # Interest on TDS/TCS Payments Default
    r'([\d\,\.]+)\s+'                # Interest on TDS/TCS Deduction Default
    r'([\d\,\.]+)\s+'                # Late Filing Fee u/s 234E
    r'([\d\,\.]+)\s+'                # Interest u/s 220(2)
    r'([\d\,\.]+)$'                  # Total Default
)

# Header patterns
HEADER_PATTERN = re.compile(
    r'Permanent\s*Account\s*Number\s*\(PAN\)\s*([A-Z]{5}\d{4}[A-Z])\s+'
    r'Current\s*Status\s*of\s*PAN\s*([^,\n]+?)\s+'
    r'Financial\s*Year\s*(\d{4}-\d{4})\s+'
    r'Assessment\s*Year\s*(\d{4}-\d{4})'
)

HEADER_NAME_PATTERN = re.compile(r'Name\s*of\s*Assessee\s*([A-Z][A-Z0-9\s\.\-\/\(\)&\']+)')
HEADER_ADDRESS_PATTERN = re.compile(
    r'Address\s*of\s*Assessee\s*(.+?)(?=\n[A-Z]|$)',
    re.DOTALL
)


class Form26ASImporter:
    """Import Form 26AS from ZIP (password-protected) or PDF."""

    def __init__(self):
        self._pdf_parser_state = None

    async def import_file(
        self,
        db: AsyncSession,
        client_id: UUID,
        file: BinaryIO,
        filename: str,
        dob_str: str,
    ) -> dict:
        """Import Form 26AS and parse into structured data.

        Returns: {"imported": int, "skipped": int, "errors": list[str], "extracted": dict}
        """
        content = file.read()

        # Convert DOB to DDMMYYYY password format
        password = self._format_dob_password(dob_str)

        # Extract text based on file type
        if filename.lower().endswith('.zip'):
            txt_content = self._extract_from_zip(content, password)
            self._pdf_parser_state = None
        elif filename.lower().endswith('.pdf'):
            # For PDF, use table-based parser for better extraction
            from app.services.importers.form26as_pdf_tables import parse_pdf_tables
            try:
                parsed_data = parse_pdf_tables(content, password.decode('utf-8'))
                # Continue with storing the data
                self._pdf_parser_state = {'current_part': None, 'current_deductor': None}
                
                # Extract AY from parsed data
                ay_full = parsed_data.get("header", {}).get("assessment_year", "")
                if ay_full and "-" in ay_full:
                    start, end = ay_full.split("-")
                    ay = f"{start}-{end[-2:]}"
                else:
                    ay = ay_full or ""
                
                # Store in form26as_data table
                from app.infra.db.models.form26as_data import Form26ASData
                existing = await db.execute(
                    select(Form26ASData).where(
                        Form26ASData.client_id == client_id,
                        Form26ASData.ay == ay,
                    )
                )
                if existing.scalar_one_or_none():
                    await db.execute(
                        delete(Form26ASData).where(
                            Form26ASData.client_id == client_id,
                            Form26ASData.ay == ay,
                        )
                    )

                form26as_record = Form26ASData(
                    client_id=client_id,
                    ay=ay,
                    pan=parsed_data.get("header", {}).get("pan", ""),
                    raw_json=parsed_data,
                    parsed_summary=self._extract_summary(parsed_data, ay),
                )
                db.add(form26as_record)
                await db.commit()

                extracted = self._extract_summary(parsed_data, ay)
                return {"imported": 1, "skipped": 0, "errors": [], "extracted": extracted}
            except Exception as e:
                return {
                    "imported": 0, "skipped": 0,
                    "errors": [f"Failed to parse PDF: {str(e)}"],
                    "extracted": {},
                }
        else:
            return {
                "imported": 0, "skipped": 0,
                "errors": ["Unsupported file type. Only ZIP and PDF are supported."],
                "extracted": {},
            }

        if not txt_content:
            return {
                "imported": 0, "skipped": 0,
                "errors": ["Failed to extract/decrypt file. Check DOB password."],
                "extracted": {},
            }

        # Detect format and parse accordingly
        if self._is_pdf_format(txt_content):
            parsed_data = self._parse_pdf_text(txt_content)
        else:
            parsed_data = self._parse_26as_text(txt_content)

        # Extract AY from parsed data
        ay_full = parsed_data.get("header", {}).get("assessment_year", "")
        if ay_full and "-" in ay_full:
            start, end = ay_full.split("-")
            ay = f"{start}-{end[-2:]}"
        else:
            ay = ay_full or ""

        # Store in form26as_data table (replace existing for client+AY)
        from app.infra.db.models.form26as_data import Form26ASData

        existing = await db.execute(
            select(Form26ASData).where(
                Form26ASData.client_id == client_id,
                Form26ASData.ay == ay,
            )
        )
        if existing.scalar_one_or_none():
            await db.execute(
                delete(Form26ASData).where(
                    Form26ASData.client_id == client_id,
                    Form26ASData.ay == ay,
                )
            )

        form26as_record = Form26ASData(
            client_id=client_id,
            ay=ay,
            pan=parsed_data.get("header", {}).get("pan", ""),
            raw_json=parsed_data,
            parsed_summary=self._extract_summary(parsed_data, ay),
        )
        db.add(form26as_record)
        await db.commit()

        extracted = self._extract_summary(parsed_data, ay)
        return {"imported": 1, "skipped": 0, "errors": [], "extracted": extracted}

    # -----------------------------------------------------------------------
    # Format detection
    # -----------------------------------------------------------------------

    def _is_pdf_format(self, content: str) -> bool:
        """Detect if content is from PDF (tabular) or ZIP (^-delimited) format.
        
        PDF format: Contains 'Permanent Account Number (PAN)', table headers like
        'Sr. No. Name of Deductor TAN', and amounts like '224329.00'
        
        ZIP format: Uses ^ as delimiter and starts with file creation date
        """
        # Check for PDF-specific markers
        pdf_markers = [
            'Permanent Account Number (PAN)',
            'Name of Assessee',
            'Annual Tax Statement',
            'Sr. No. Name of Deductor TAN',
        ]
        
        has_pdf_marker = any(marker in content for marker in pdf_markers)
        
        # Check for ZIP format markers
        zip_markers = ['^Annual Tax Statement^', 'File Creation Date^']
        
        # If we see ^ delimited structure early, it's ZIP format
        lines = content.split('\n')[:20]
        has_delimiter = any('^^^' in line or line.count('^') > 5 for line in lines)
        
        # ZIP format takes priority: if content has ^ delimiters, treat as ZIP even if
        # it also contains PDF-like text (e.g. "Annual Tax Statement" appears in both)
        if has_delimiter:
            return False  # ZIP format
        
        return has_pdf_marker

    # -----------------------------------------------------------------------
    # PDF Parser
    # -----------------------------------------------------------------------

    def _parse_pdf_text(self, txt_content: str) -> dict:
        """Parse tabular PDF format into structured JSON covering ALL 10 parts."""
        data: dict = {
            "header": {},
            "part1_tds_deductors": [],
            "part1_tds": [],
            "part2_tds_15g15h": [],
            "part3_transactions_194b_194r_194s_194ba": [],
            "part4_tds_194ia_194ib_194m_194s_seller": [],
            "part5_transactions_26qe_seller": [],
            "part6_tcs_collectors": [],
            "part6_tcs": [],
            "part7_paid_refunds": [],
            "part8_tds_194ia_194ib_194m_194s_buyer": [],
            "part9_transactions_26qe_buyer": [],
            "part10_tds_tcs_defaults": [],
            "source": "pdf",
        }

        lines = txt_content.split('\n')
        
        # Parse header
        data["header"] = self._parse_pdf_header(txt_content)
        
        # Parse all parts
        current_part = None
        current_deductor = None
        in_transaction_section = False
        
        i = 0
        while i < len(lines):
            line = lines[i].strip()
            
            # Detect part transitions
            part_match = self._detect_pdf_part(line)
            if part_match:
                current_part = part_match
                current_deductor = None
                in_transaction_section = False
                i += 1
                continue
            
            # Skip empty lines and noise
            if not line or self._is_pdf_noise(line):
                i += 1
                continue
            
            # Parse based on current part
            if current_part == "PART-I":
                # First, try to parse a deductor summary row
                parsed_deductor = self._parse_pdf_part1_deductor(line, lines, i)
                if parsed_deductor:
                    data["part1_tds_deductors"].append(parsed_deductor)
                    data["part1_tds"].append(parsed_deductor)
                    current_deductor = parsed_deductor
                    # Skip the transaction rows that were already consumed
                    i += parsed_deductor.get('_txn_rows_skipped', 1)
                    continue
                
                # If no deductor, try parsing transaction row
                txn = self._parse_pdf_part1_transaction(line)
                if txn and current_deductor:
                    current_deductor.setdefault("transactions", []).append(txn)
                i += 1
                
            elif current_part == "PART-II":
                parsed = self._parse_pdf_part2_deductor(line)
                if parsed:
                    data["part2_tds_15g15h"].append(parsed)
                i += 1
                
            elif current_part == "PART-III":
                parsed = self._parse_pdf_part3_record(line)
                if parsed:
                    data["part3_transactions_194b_194r_194s_194ba"].append(parsed)
                i += 1
                
            elif current_part == "PART-IV":
                parsed = self._parse_pdf_part4_record(line)
                if parsed:
                    data["part4_tds_194ia_194ib_194m_194s_seller"].append(parsed)
                i += 1
                
            elif current_part == "PART-V":
                parsed = self._parse_pdf_part5_record(line)
                if parsed:
                    data["part5_transactions_26qe_seller"].append(parsed)
                i += 1
                
            elif current_part == "PART-VI":
                parsed = self._parse_pdf_part6_collector(line)
                if parsed:
                    data["part6_tcs_collectors"].append(parsed)
                    data["part6_tcs"].append(parsed)
                i += 1
                
            elif current_part == "PART-VII":
                parsed = self._parse_pdf_part7_refund(line)
                if parsed:
                    data["part7_paid_refunds"].append(parsed)
                i += 1
                
            elif current_part == "PART-VIII":
                parsed = self._parse_pdf_part8_record(line)
                if parsed:
                    data["part8_tds_194ia_194ib_194m_194s_buyer"].append(parsed)
                i += 1
                
            elif current_part == "PART-IX":
                parsed = self._parse_pdf_part9_record(line)
                if parsed:
                    data["part9_transactions_26qe_buyer"].append(parsed)
                i += 1
                
            elif current_part == "PART-X":
                parsed = self._parse_pdf_part10_default(line)
                if parsed:
                    data["part10_tds_tcs_defaults"].append(parsed)
                i += 1
                
            else:
                i += 1
        
        return data

    def _detect_pdf_part(self, line: str) -> str | None:
        """Detect PDF part from line."""
        line_upper = line.upper()
        if "PART-I" in line_upper and "Details of Tax Deducted at Source" in line_upper:
            return "PART-I"
        if "PART-II" in line_upper and ("15G" in line_upper or "15H" in line_upper):
            return "PART-II"
        if "PART-III" in line_upper and ("194B" in line_upper or "194R" in line_upper or "194S" in line_upper or "194BA" in line_upper):
            return "PART-III"
        if "PART-IV" in line_upper and ("194IA" in line_upper or "194IB" in line_upper or "194M" in line_upper or "194S" in line_upper):
            if "BUYER" not in line_upper and "TENANT" not in line_upper and "DEDUCTEE" not in line_upper:
                return "PART-IV"
        if "PART-V" in line_upper and "26QE" in line_upper:
            return "PART-V"
        if "PART-VI" in line_upper and "Tax Collected at Source" in line_upper:
            return "PART-VI"
        if "PART-VII" in line_upper and "Paid Refund" in line_upper:
            return "PART-VII"
        if "PART-VIII" in line_upper and ("194IA" in line_upper or "194IB" in line_upper or "194M" in line_upper or "194S" in line_upper):
            if "BUYER" in line_upper or "TENANT" in line_upper or "DEDUCTEE" in line_upper:
                return "PART-VIII"
        if "PART-IX" in line_upper and "194S" in line_upper:
            return "PART-IX"
        if "PART X" in line_upper and ("TDS" in line_upper or "TCS" in line_upper) and "Default" in line_upper:
            return "PART-X"
        return None

    def _is_pdf_noise(self, line: str) -> bool:
        """Check if PDF line is noise."""
        noise_patterns = [
            'No Transactions Present',
            'Data updated till',
            'Annual Tax Statement',
            'Sr. No. Name of Deductor TAN',
            'Sr. No. Section',
            'Sr. No. Acknowledgement Number',
            'Sr. No. Name of Collector TAN',
            'Sr. No. Assessment Year',
            'Sr. No. Financial Year',
            'Status of Booking',
            'Legend Description',
            'Assessee PAN:',
            'Glossary',
            'Notes for Annual Tax Statement',
            'Contact Information',
            '*********',
            'Total Across',
            'Gross Total',
        ]
        return any(p in line for p in noise_patterns)

    def _parse_pdf_header(self, text: str) -> dict:
        """Parse PDF header section."""
        header = {
            "file_creation_date": "",
            "pan": "",
            "current_status": "",
            "financial_year": "",
            "assessment_year": "",
            "name": "",
            "address": "",
        }
        
        # Extract PAN, status, FY, AY
        match = HEADER_PATTERN.search(text)
        if match:
            header["pan"] = match.group(1).strip()
            header["current_status"] = match.group(2).strip()
            header["financial_year"] = match.group(3).strip()
            header["assessment_year"] = match.group(4).strip()
        
        # Extract name
        name_match = HEADER_NAME_PATTERN.search(text)
        if name_match:
            header["name"] = name_match.group(1).strip()
        
        # Extract address
        addr_match = re.search(
            r'Address\s*of\s*Assessee\s*(.+?)(?=\n\n|\nAbove|\nRefer)',
            text, re.DOTALL
        )
        if addr_match:
            header["address"] = ' '.join(addr_match.group(1).split())
        
        # Extract data updated date
        date_match = re.search(r'Data updated till (\d{2}-[A-Za-z]{3}-\d{4})', text)
        if date_match:
            header["file_creation_date"] = date_match.group(1)
        
        return header

    def _parse_pdf_part1_deductor(self, line: str, lines: list, idx: int) -> dict | None:
        """Parse PART-I deductor summary row and its transactions."""
        # Pattern: "1 WELLS FARGO INTERNATIONAL SOLUTIONS PRIVATE LIMITED HYDW00345C 1964956.60 185112.00 185112.00"
        # Structure: Sr No | Name (uppercase words) | TAN | Amount | Tax Deducted | TDS Deposited
        
        # First try greedy match for name (matches as much as possible before TAN)
        match = re.match(
            r'^(\d+)\s+([A-Z][A-Z0-9\s\.\-\/\(\)&\']+)\s+([A-Z]{4}\d{5}[A-Z])\s+([\d\,\.]+)\s+([\d\,\.]+)\s+([\d\,\.]+)$',
            line
        )
        
        # If greedy fails, try non-greedy as fallback
        if not match:
            match = re.match(
                r'^(\d+)\s+([A-Z][A-Z0-9\s\.\-\/\(\)&\']+?)\s+([A-Z]{4}\d{5}[A-Z])\s+([\d\,\.]+)\s+([\d\,\.]+)\s+([\d\,\.]+)$',
                line
            )
        
        if not match:
            return None
        
        deductor = {
            "sr_no": match.group(1),
            "deductor_name": match.group(2).strip(),
            "tan": match.group(3),
            "amount_paid_credited": self._to_float(match.group(4)),
            "tax_deducted": self._to_float(match.group(5)),
            "tds_deposited": self._to_float(match.group(6)),
            # Alias fields
            "total_amount": self._to_float(match.group(4)),
            "total_tds": self._to_float(match.group(5)),
            "total_tds_deposited": self._to_float(match.group(6)),
            "transactions": [],
        }
        
        # Look ahead for transaction rows
        txn_rows = 0
        j = idx + 1
        while j < len(lines) and txn_rows < 200:  # Safety limit
            next_line = lines[j].strip()
            
            # Stop at next deductor or part header
            if self._detect_pdf_part(next_line):
                break
            if not next_line or self._is_pdf_noise(next_line):
                j += 1
                continue
            
            # Check if it's a transaction row
            # Format: "1 192 26-Mar-2026 F 28-May-2026 - 148459.00 11664.00 11664.00"
            txn_match = re.match(
                r'^(\d+)\s+(\d{4}[A-Z]?)\s+(\d{2}-[A-Za-z]{3}-\d{4})\s+([A-Z])\s+(\d{2}-[A-Za-z]{3}-\d{4})\s+([^\d][^\s]*)\s+([\d\-\,\.]+)\s+([\d\-\,\.]+)\s+([\d\-\,\.]+)$',
                next_line
            )
            if txn_match:
                txn = {
                    "sr_no": txn_match.group(1),
                    "section": txn_match.group(2),
                    "transaction_date": txn_match.group(3),
                    "status_of_booking": txn_match.group(4),
                    "date_of_booking": txn_match.group(5),
                    "remarks": txn_match.group(6),
                    "amount": self._to_float(txn_match.group(7)),
                    "tax_deducted": self._to_float(txn_match.group(8)),
                    "tds_deposited": self._to_float(txn_match.group(9)),
                }
                deductor["transactions"].append(txn)
                txn_rows += 1
            elif re.match(r'^\d+\s+[A-Z]', next_line):
                # This is likely the next deductor
                break
            j += 1
        
        deductor['_txn_rows_skipped'] = txn_rows + 1
        return deductor

    def _parse_pdf_part1_transaction(self, line: str) -> dict | None:
        """Parse PART-I transaction row."""
        # Format: "1 192 26-Mar-2026 F 28-May-2026 - 148459.00 11664.00 11664.00"
        match = re.match(
            r'^(\d+)\s+(\d{4}[A-Z]?)\s+(\d{2}-[A-Za-z]{3}-\d{4})\s+([A-Z])\s+(\d{2}-[A-Za-z]{3}-\d{4})\s+([^\d][^\s]*)\s+([\d\-\,\.]+)\s+([\d\-\,\.]+)\s+([\d\-\,\.]+)$',
            line
        )
        if not match:
            return None
        
        return {
            "sr_no": match.group(1),
            "section": match.group(2),
            "transaction_date": match.group(3),
            "status_of_booking": match.group(4),
            "date_of_booking": match.group(5),
            "remarks": match.group(6),
            "amount": self._to_float(match.group(7)),
            "tax_deducted": self._to_float(match.group(8)),
            "tds_deposited": self._to_float(match.group(9)),
        }

    def _parse_pdf_part2_deductor(self, line: str) -> dict | None:
        """Parse PART-II (15G/15H) deductor row."""
        match = re.match(
            r'^(\d+)\s+([A-Z][A-Z0-9\s\.\-\/\(\)&\',]+?)\s+([A-Z]{4}\d{5}[A-Z])\s+([\d\,\.]+)\s+([\d\,\.]+)\s+([\d\,\.]+)$',
            line
        )
        if not match:
            return None
        
        return {
            "sr_no": match.group(1),
            "deductor_name": match.group(2).strip(),
            "tan": match.group(3),
            "amount_paid_credited": self._to_float(match.group(4)),
            "tax_deducted": self._to_float(match.group(5)),
            "tds_deposited": self._to_float(match.group(6)),
        }

    def _parse_pdf_part3_record(self, line: str) -> dict | None:
        """Parse PART-III (194B/194R/194S/194BA) record."""
        # Format: Sr No | Name | TAN | Total Amount
        match = re.match(
            r'^(\d+)\s+([A-Z][A-Z0-9\s\.\-\/\(\)&\']+?)\s+([A-Z]{4}\d{5}[A-Z])\s+([\d\,\.]+)$',
            line
        )
        if not match:
            return None
        
        return {
            "sr_no": match.group(1),
            "name_of_deductor": match.group(2).strip(),
            "tan_of_deductor": match.group(3),
            "total_amount_paid_credited": self._to_float(match.group(4)),
        }

    def _parse_pdf_part4_record(self, line: str) -> dict | None:
        """Parse PART-IV (194IA/194IB/194M/194S seller) record."""
        # Format: Sr No | Ack No | Name | PAN | Date | Amount | TDS
        match = re.match(
            r'^(\d+)\s+([A-Z0-9]+)\s+([A-Z][A-Z0-9\s\.\-\/\(\)&\']+?)\s+([A-Z]{5}\d{4}[A-Z])\s+(\d{2}-[A-Za-z]{3}-\d{4})\s+([\d\,\.]+)\s+([\d\,\.]+)$',
            line
        )
        if not match:
            return None
        
        return {
            "sr_no": match.group(1),
            "acknowledgement_number": match.group(2),
            "name_of_deductor": match.group(3).strip(),
            "pan_of_deductor": match.group(4),
            "transaction_date": match.group(5),
            "total_transaction_amount": self._to_float(match.group(6)),
            "total_tds_deposited": self._to_float(match.group(7)),
        }

    def _parse_pdf_part5_record(self, line: str) -> dict | None:
        """Parse PART-V (26QE VDA seller) record."""
        # Format: Sr No | Ack No | Name | PAN | Date | Amount
        match = re.match(
            r'^(\d+)\s+([A-Z0-9]+)\s+([A-Z][A-Z0-9\s\.\-\/\(\)&\']+?)\s+([A-Z]{5}\d{4}[A-Z])\s+(\d{2}-[A-Za-z]{3}-\d{4})\s+([\d\,\.]+)$',
            line
        )
        if not match:
            return None
        
        return {
            "sr_no": match.group(1),
            "acknowledgement_number": match.group(2),
            "name_of_buyer": match.group(3).strip(),
            "pan_of_buyer": match.group(4),
            "transaction_date": match.group(5),
            "total_transaction_amount": self._to_float(match.group(6)),
        }

    def _parse_pdf_part6_collector(self, line: str) -> dict | None:
        """Parse PART-VI (TCS) collector row."""
        match = re.match(
            r'^(\d+)\s+([A-Z][A-Z0-9\s\.\-\/\(\)&\',]+?)\s+([A-Z]{4}\d{5}[A-Z])\s+([\d\,\.]+)\s+([\d\,\.]+)\s+([\d\,\.]+)$',
            line
        )
        if not match:
            return None
        
        return {
            "sr_no": match.group(1),
            "name_of_collector": match.group(2).strip(),
            "tan_of_collector": match.group(3),
            "amount_paid_debited": self._to_float(match.group(4)),
            "tax_collected": self._to_float(match.group(5)),
            "tcs_deposited": self._to_float(match.group(6)),
        }

    def _parse_pdf_part7_refund(self, line: str) -> dict | None:
        """Parse PART-VII refund record."""
        # Format: Sr No | AY | Mode | Ref No | Nature | Amount | Interest | Date | Remarks
        match = re.match(
            r'^(\d+)\s+(\d{4}-\d{2})\s+([A-Z])\s+([A-Z0-9]+)\s+([A-Za-z\s\/]+?)\s+([\d\,\.]+)\s+([\d\,\.]+)\s+(\d{2}-[A-Za-z]{3}-\d{4})\s*(.*)$',
            line
        )
        if not match:
            return None
        
        return {
            "sr_no": match.group(1),
            "assessment_year": match.group(2),
            "mode": match.group(3),
            "refund_issued": match.group(4),
            "nature_of_refund": match.group(5).strip(),
            "amount_of_refund": self._to_float(match.group(6)),
            "interest": self._to_float(match.group(7)),
            "date_of_payment": match.group(8),
            "remarks": match.group(9).strip() if match.group(9) else "",
        }

    def _parse_pdf_part8_record(self, line: str) -> dict | None:
        """Parse PART-VIII (buyer/tenant) record."""
        # Format: Sr No | Ack No | Name | PAN | Date | Amount | TDS | Amount Other
        match = re.match(
            r'^(\d+)\s+([A-Z0-9]+)\s+([A-Z][A-Z0-9\s\.\-\/\(\)&\']+?)\s+([A-Z]{5}\d{4}[A-Z])\s+(\d{2}-[A-Za-z]{3}-\d{4})\s+([\d\,\.]+)\s+([\d\,\.]+)\s+([\d\,\.]+)$',
            line
        )
        if not match:
            return None
        
        return {
            "sr_no": match.group(1),
            "acknowledgement_number": match.group(2),
            "name_of_deductee": match.group(3).strip(),
            "pan_of_deductee": match.group(4),
            "transaction_date": match.group(5),
            "total_transaction_amount": self._to_float(match.group(6)),
            "total_tds_deposited": self._to_float(match.group(7)),
            "total_amount_deposited_other_than_tds": self._to_float(match.group(8)),
        }

    def _parse_pdf_part9_record(self, line: str) -> dict | None:
        """Parse PART-IX (26QE buyer) record."""
        # Format: Sr No | Ack No | Name | PAN | Date | Amount
        match = re.match(
            r'^(\d+)\s+([A-Z0-9]+)\s+([A-Z][A-Z0-9\s\.\-\/\(\)&\']+?)\s+([A-Z]{5}\d{4}[A-Z])\s+(\d{2}-[A-Za-z]{3}-\d{4})\s+([\d\,\.]+)$',
            line
        )
        if not match:
            return None
        
        return {
            "sr_no": match.group(1),
            "acknowledgement_number": match.group(2),
            "name_of_seller": match.group(3).strip(),
            "pan_of_seller": match.group(4),
            "transaction_date": match.group(5),
            "total_transaction_amount": self._to_float(match.group(6)),
        }

    def _parse_pdf_part10_default(self, line: str) -> dict | None:
        """Parse PART-X (TDS/TCS defaults) record."""
        # Format: Sr No | FY | Short Pay | Short Ded | Int Pay | Int Ded | LFF | Int 220 | Total
        match = re.match(
            r'^(\d+)\s+(\d{4}-\d{2})\s+([\d\,\.]+)\s+([\d\,\.]+)\s+([\d\,\.]+)\s+([\d\,\.]+)\s+([\d\,\.]+)\s+([\d\,\.]+)\s+([\d\,\.]+)$',
            line
        )
        if not match:
            return None
        
        return {
            "sr_no": match.group(1),
            "financial_year": match.group(2),
            "short_payment": self._to_float(match.group(3)),
            "short_deduction_collection": self._to_float(match.group(4)),
            "interest_on_tds_tcs_payments_default": self._to_float(match.group(5)),
            "interest_on_tds_tcs_deduction_default": self._to_float(match.group(6)),
            "late_filing_fee_234e": self._to_float(match.group(7)),
            "interest_u_s_220_2": self._to_float(match.group(8)),
            "total_default": self._to_float(match.group(9)),
        }

    # -----------------------------------------------------------------------
    # ZIP Parser (^-delimited format)
    # -----------------------------------------------------------------------

    def _parse_26as_text(self, txt_content: str) -> dict:
        """Parse ^-delimited 26AS text into structured JSON covering ALL 10 parts."""
        lines = [ln.strip() for ln in txt_content.strip().split("\n") if ln.strip()]

        # Build the data structure with slots for every part.
        # ALSO maintain backward-compatible aliases used by tests (part1_tds, part2_tds_15g, etc.)
        data: dict = {
            "header": {},
            "part1_tds_deductors": [],
            "part1_tds": [],       # alias for tests
            "part2_tds_15g15h": [],
            "part2_tds_15g": [],   # alias for tests
            "part3_transactions_194b_194r_194s_194ba": [],
            "part4_tds_194ia_194ib_194m_194s_seller": [],
            "part5_transactions_26qe_seller": [],
            "part6_tcs_collectors": [],
            "part6_tcs": [],       # alias for tests
            "part7_paid_refunds": [],
            "part8_tds_194ia_194ib_194m_194s_buyer": [],
            "part9_transactions_26qe_buyer": [],
            "part10_tds_tcs_defaults": [],
            "source": "zip",
        }

        current_part: str | None = None   # e.g. "PART-I"
        current_deductor: dict | None = None  # For PART-I / PART-II / PART-VI
        header_done: bool = False

        for line in lines:
            # ---- Detect part transitions ----
            detected_part = self._detect_part(line)
            if detected_part:
                current_part = detected_part
                current_deductor = None
                continue

            if current_part is None:
                # Try to read header (occurs before PART-I)
                if not header_done and self._looks_like_header(line):
                    data["header"] = self._parse_header(line)
                    header_done = True
                continue

            # ---- Skip noise ----
            if self._is_noise(line):
                continue

            # ---- Parse by part ----
            if current_part == "PART-I":
                self._parse_part1(line, data, current_deductor)
                # Re-acquire ref to dict that was just appended
                if data["part1_tds_deductors"] and current_deductor is None:
                    current_deductor = data["part1_tds_deductors"][-1]
                elif data["part1_tds_deductors"]:
                    current_deductor = data["part1_tds_deductors"][-1]
            elif current_part == "PART-II":
                self._parse_part2(line, data)
            elif current_part == "PART-III":
                self._parse_generic_records(line, data["part3_transactions_194b_194r_194s_194ba"], PART3_HEADERS)
            elif current_part == "PART-IV":
                self._parse_generic_records(line, data["part4_tds_194ia_194ib_194m_194s_seller"], PART4_HEADERS)
            elif current_part == "PART-V":
                self._parse_generic_records(line, data["part5_transactions_26qe_seller"], PART5_HEADERS)
            elif current_part == "PART-VI":
                self._parse_part6(line, data)
            elif current_part == "PART-VII":
                self._parse_part7(line, data)
            elif current_part == "PART-VIII":
                self._parse_generic_records(line, data["part8_tds_194ia_194ib_194m_194s_buyer"], PART8_HEADERS)
            elif current_part == "PART-IX":
                self._parse_generic_records(line, data["part9_transactions_26qe_buyer"], PART9_HEADERS)
            elif current_part == "PART-X":
                self._parse_part10(line, data)

        return data

    # -----------------------------------------------------------------------
    # ZIP Format Part detection & noise filter
    # -----------------------------------------------------------------------

    def _detect_part(self, line: str) -> str | None:
        """Return canonical part name (e.g. 'PART-I') or None."""
        if not line.startswith("^PART-"):
            return None
        # Roman numeral is the first token after the leading ^
        token = line.lstrip("^").split(" ", 1)[0]  # e.g. "PART-I"
        # Normalize: 'PART-I', 'PART-II', 'PART-III', 'PART-IV', 'PART-V', ...
        if token in PART_SCHEMAS:
            return token
        return None

    def _is_noise(self, line: str) -> bool:
        """True if line is a header row, 'no transactions' marker, or separator."""
        if "No Transactions Present" in line:
            return True
        if "Annual Tax Statement" in line:
            return True
        if line.startswith("Sr. No.") or line.startswith("^Sr. No."):
            return True
        return False

    def _looks_like_header(self, line: str) -> bool:
        """Header row: starts with date, has 10-char PAN, ends with pincode."""
        if "^" not in line:
            return False
        parts = line.split("^")
        if len(parts) < 13:
            return False
        if len(parts[1]) != 10 or not parts[1][:5].isalpha() or not parts[1][5:9].isdigit():
            return False
        return True

    # -----------------------------------------------------------------------
    # ZIP Format Header
    # -----------------------------------------------------------------------

    def _parse_header(self, line: str) -> dict:
        parts = line.split("^")
        return {
            "file_creation_date": parts[0],
            "pan": parts[1],
            "current_status": parts[2],
            "financial_year": parts[3],
            "assessment_year": parts[4],
            "name": parts[5],
            "address_line_1": parts[6] if len(parts) > 6 else "",
            "address_line_2": parts[7] if len(parts) > 7 else "",
            "address_line_3": parts[8] if len(parts) > 8 else "",
            "address_line_4": parts[9] if len(parts) > 9 else "",
            "address_line_5": parts[10] if len(parts) > 10 else "",
            "state_code": parts[11] if len(parts) > 11 else "",
            "pin_code": parts[12] if len(parts) > 12 else "",
        }

    # -----------------------------------------------------------------------
    # ZIP Format PART-I: TDS Deductors (with nested transactions)
    # -----------------------------------------------------------------------

    def _parse_part1(self, line: str, data: dict, current_deductor: dict | None) -> None:
        """Parse one line of PART-I (deductor summary or transaction row)."""
        if line.startswith("^"):
            # Transaction row attached to the most-recent deductor
            if current_deductor is None:
                return
            clean = line.strip("^")
            parts = clean.split("^")
            if len(parts) >= 9:
                txn = {
                    "sr_no": parts[0],
                    "section": parts[1],
                    "transaction_date": parts[2],
                    "status_of_booking": parts[3],
                    "date_of_booking": parts[4],
                    "remarks": parts[5],
                    "amount": self._to_float(parts[6]),
                    "tax_deducted": self._to_float(parts[7]),
                    "tds_deposited": self._to_float(parts[8]),
                }
                current_deductor.setdefault("transactions", []).append(txn)
        else:
            # Deductor summary: 1^WELLS FARGO^HYDW00345C^^^^^1964956.60^185112.00^185112.00^
            parts = [p for p in line.split("^") if p != ""]
            if len(parts) >= 6 and parts[0].isdigit():
                amount = self._to_float(parts[-3])
                tax_deducted = self._to_float(parts[-2])
                tds_deposited = self._to_float(parts[-1])
                deductor = {
                    "sr_no": parts[0],
                    "deductor_name": parts[1],
                    "tan": parts[2],
                    # Primary field names (used by parser and DB)
                    "amount_paid_credited": amount,
                    "tax_deducted": tax_deducted,
                    "tds_deposited": tds_deposited,
                    # Alias field names (used by tests for backward compatibility)
                    "total_amount": amount,
                    "total_tds": tax_deducted,
                    "total_tds_deposited": tds_deposited,
                    "transactions": [],
                }
                data["part1_tds_deductors"].append(deductor)
                # Also add to alias key for tests
                data["part1_tds"].append(deductor)

    # -----------------------------------------------------------------------
    # ZIP Format PART-II: TDS for 15G / 15H (deductor summary)
    # -----------------------------------------------------------------------

    def _parse_part2(self, line: str, data: dict) -> None:
        if line.startswith("^"):
            return  # PART-II in modern files has no nested transactions
        parts = [p for p in line.split("^") if p != ""]
        if len(parts) >= 6 and parts[0].isdigit():
            data["part2_tds_15g15h"].append({
                "sr_no": parts[0],
                "deductor_name": parts[1],
                "tan": parts[2],
                "amount_paid_credited": self._to_float(parts[-3]),
                "tax_deducted": self._to_float(parts[-2]),
                "tds_deposited": self._to_float(parts[-1]),
            })

    # -----------------------------------------------------------------------
    # ZIP Format PART-III/IV/V/VIII/IX: Generic record rows
    # -----------------------------------------------------------------------

    def _parse_generic_records(self, line: str, target_list: list, headers: list) -> None:
        if line.startswith("^"):
            return
        parts = line.split("^")
        # Remove trailing empty fields
        while parts and parts[-1] == "":
            parts.pop()
        if len(parts) < len(headers):
            return
        record = {}
        for i, h in enumerate(headers):
            v = parts[i] if i < len(parts) else ""
            if h in ("total_transaction_amount", "total_tds_deposited", "amount", 
                     "amount_paid_credited", "tax_deducted", "tds_deposited",
                     "total_amount_paid_credited"):
                record[h] = self._to_float(v)
            else:
                record[h] = v
        target_list.append(record)

    # -----------------------------------------------------------------------
    # ZIP Format PART-VI: TCS Collectors
    # -----------------------------------------------------------------------

    def _parse_part6(self, line: str, data: dict) -> None:
        if line.startswith("^"):
            return
        parts = [p for p in line.split("^") if p != ""]
        if len(parts) >= 6 and parts[0].isdigit():
            data["part6_tcs_collectors"].append({
                "sr_no": parts[0],
                "name_of_collector": parts[1],
                "tan_of_collector": parts[2],
                "amount_paid_debited": self._to_float(parts[-3]),
                "tax_collected": self._to_float(parts[-2]),
                "tcs_deposited": self._to_float(parts[-1]),
            })

    # -----------------------------------------------------------------------
    # ZIP Format PART-VII: Paid Refunds
    # -----------------------------------------------------------------------

    def _parse_part7(self, line: str, data: dict) -> None:
        if line.startswith("^"):
            return
        parts = line.split("^")
        while parts and parts[-1] == "":
            parts.pop()
        if len(parts) < len(PART7_HEADERS):
            return
        data["part7_paid_refunds"].append({
            "sr_no": parts[0],
            "assessment_year": parts[1],
            "mode": parts[2],
            "refund_issued": parts[3],
            "nature_of_refund": parts[4],
            "amount_of_refund": self._to_float(parts[5]),
            "interest": self._to_float(parts[6]),
            "date_of_payment": parts[7],
            "remarks": parts[8] if len(parts) > 8 else "",
        })

    # -----------------------------------------------------------------------
    # ZIP Format PART-X: TDS/TCS Defaults
    # -----------------------------------------------------------------------

    def _parse_part10(self, line: str, data: dict) -> None:
        if line.startswith("^"):
            return
        parts = line.split("^")
        while parts and parts[-1] == "":
            parts.pop()
        if len(parts) < len(PART10_HEADERS):
            return
        data["part10_tds_tcs_defaults"].append({
            "sr_no": parts[0],
            "financial_year": parts[1],
            "short_payment": self._to_float(parts[2]),
            "short_deduction_collection": self._to_float(parts[3]),
            "interest_on_tds_tcs_payments_default": self._to_float(parts[4]),
            "interest_on_tds_tcs_deduction_default": self._to_float(parts[5]),
            "late_filing_fee_234e": self._to_float(parts[6]),
            "interest_u_s_220_2": self._to_float(parts[7]) if len(parts) > 7 else 0.0,
            "total_default": self._to_float(parts[8]) if len(parts) > 8 else 0.0,
        })

    # -----------------------------------------------------------------------
    # Helpers
    # -----------------------------------------------------------------------

    @staticmethod
    def _to_float(s: str) -> float:
        """Robust string-to-float. Handles commas and negative values."""
        if not s:
            return 0.0
        try:
            # Remove commas and handle negative values
            s = s.strip().replace(',', '')
            return float(s)
        except (ValueError, TypeError):
            return 0.0

    # -----------------------------------------------------------------------
    # Password + extraction helpers
    # -----------------------------------------------------------------------

    def _format_dob_password(self, dob_str: str) -> bytes:
        """Convert DOB string to DDMMYYYY password format (ITD 26AS ZIP password)."""
        dob_str = dob_str.strip()
        if "-" in dob_str:
            parts = dob_str.split("-")
            if len(parts[0]) == 4:  # YYYY-MM-DD
                yyyy, mm, dd = parts
            else:  # DD-MM-YYYY
                dd, mm, yyyy = parts
            password = f"{dd}{mm}{yyyy}"
        else:
            password = dob_str
        return password.encode("utf-8")

    def _extract_from_zip(self, zip_bytes: bytes, password: bytes) -> str:
        """Extract TXT file from password-protected ZIP."""
        try:
            with pyzipper.AESZipFile(io.BytesIO(zip_bytes), "r") as zf:
                zf.setpassword(password)
                txt_files = [name for name in zf.namelist() if name.lower().endswith(".txt")]
                if not txt_files:
                    return ""
                return zf.read(txt_files[0]).decode("utf-8")
        except Exception:
            return ""

    # -----------------------------------------------------------------------
    # Summary for API response
    # -----------------------------------------------------------------------

    def _extract_summary(self, data: dict, ay: str) -> dict:
        """Build a flat summary covering all 10 parts for API response."""
        # Support both primary keys (part1_tds_deductors) and alias keys (part1_tds) used by tests
        part1 = data.get("part1_tds", []) or data.get("part1_tds_deductors", [])
        part6 = data.get("part6_tcs", []) or data.get("part6_tcs_collectors", [])
        part7 = data.get("part7_paid_refunds", [])

        # Support both field name formats (tests use total_tds, etc.)
        total_tds = sum(d.get("total_tds", 0) or d.get("tax_deducted", 0) for d in part1)
        total_tds_deposited = sum(d.get("total_tds_deposited", 0) or d.get("tds_deposited", 0) for d in part1)
        total_amount = sum(d.get("total_amount", 0) or d.get("amount_paid_credited", 0) for d in part1)
        total_tcs = sum(d.get("tax_collected", 0) for d in part6)
        total_refund = sum(d.get("amount_of_refund", 0) for d in part7)

        return {
            "assessment_year": ay,
            "header": data.get("header", {}),
            "parts": {
                "part1_tds_deductors": len(part1),
                "part2_tds_15g15h": len(data.get("part2_tds_15g15h", [])),
                "part3_transactions_194b_194r_194s_194ba": len(data.get("part3_transactions_194b_194r_194s_194ba", [])),
                "part4_tds_194ia_194ib_194m_194s_seller": len(data.get("part4_tds_194ia_194ib_194m_194s_seller", [])),
                "part5_transactions_26qe_seller": len(data.get("part5_transactions_26qe_seller", [])),
                "part6_tcs_collectors": len(part6),
                "part7_paid_refunds": len(part7),
                "part8_tds_194ia_194ib_194m_194s_buyer": len(data.get("part8_tds_194ia_194ib_194m_194s_buyer", [])),
                "part9_transactions_26qe_buyer": len(data.get("part9_transactions_26qe_buyer", [])),
                "part10_tds_tcs_defaults": len(data.get("part10_tds_tcs_defaults", [])),
            },
            "totals": {
                "tds_deductors_count": len(part1),
                "tcs_collectors_count": len(part6),
                "refunds_count": len(part7),
                "total_tds_deducted": total_tds,
                "total_tds_deposited": total_tds_deposited,
                "total_amount_paid_credited": total_amount,
                "total_tcs_collected": total_tcs,
                "total_refund_amount": total_refund,
            },
            # Top-level fields for backward compatibility with tests
            "tds_deductors_count": len(part1),
            "total_tds_deducted": total_tds,
            "total_tds_deposited": total_tds_deposited,
            "total_amount": total_amount,
            "tcs_collectors_count": len(part6),
            "total_tcs_collected": total_tcs,
            # TDS entries details for UI display
            "tds_entries": [
                {
                    "sr_no": d.get("sr_no", ""),
                    "deductor_name": d.get("deductor_name", ""),
                    "tan": d.get("tan", ""),
                    "total_amount": d.get("total_amount", 0) or d.get("amount_paid_credited", 0),
                    "total_tds": d.get("total_tds", 0) or d.get("tax_deducted", 0),
                    "total_tds_deposited": d.get("total_tds_deposited", 0) or d.get("tds_deposited", 0),
                    "transactions": d.get("transactions", []),
                }
                for d in part1
            ],
            # TCS entries details
            "tcs_entries": [
                {
                    "sr_no": d.get("sr_no", ""),
                    "collector_name": d.get("name_of_collector", ""),
                    "tan": d.get("tan_of_collector", ""),
                    "total_amount": d.get("amount_paid_debited", 0),
                    "total_tcs": d.get("tax_collected", 0),
                }
                for d in part6
            ],
            # Source indicator
            "source": data.get("source", "unknown"),
        }
