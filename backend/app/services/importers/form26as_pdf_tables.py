"""Table-based PDF parser for Form 26AS.

This module extracts data from Form 26AS PDF files using pdfplumber's
table extraction capabilities for transaction details, combined with
text extraction for headers and deductor names.

The Form 26AS PDF has a composite table structure where deductor summaries
and their transactions are interleaved in the same table, alternating
between header rows and data rows.
"""
import io
import re
import pdfplumber
from typing import Optional


def parse_pdf_tables(pdf_bytes: bytes, password: str = "") -> dict:
    """Parse Form 26AS PDF using combined text and table extraction.
    
    Text extraction is used for header info and deductor names (which can
    be truncated in table cells). Table extraction is used for transaction
    details.
    
    Args:
        pdf_bytes: Raw PDF file content
        password: DOB password (DDMMYYYY format) for encrypted PDFs
        
    Returns:
        Dictionary with extracted data for all 10 parts
    """
    # Decrypt if needed (modifies pdf_bytes in place for encrypted PDFs)
    try:
        from app.services.importers.crypto import decrypt_pdf
        decrypt_pdf(pdf_bytes, password)
    except Exception:
        pass  # Continue with original bytes
    
    # Open PDF with pdfplumber using a BytesIO wrapper
    with pdfplumber.open(io.BytesIO(pdf_bytes)) as pdf:
        # Extract header and deductor names from text (full text, not truncated)
        header = _extract_header_from_text(pdf)
        deductor_map = _extract_deductor_names_from_text(pdf)
        
        # Extract transaction details from tables
        data = _extract_all_parts(pdf)
        
        # Merge header info
        data["header"] = header
        
        # Merge full deductor names
        for deductor in data.get("part1_tds_deductors", []):
            tan = deductor.get("tan", "")
            if tan in deductor_map:
                deductor["deductor_name"] = deductor_map[tan]
        
        return data


def _extract_header_from_text(pdf) -> dict:
    """Extract header info from PDF text using regex patterns."""
    header = {}
    
    for page in pdf.pages:
        text = page.extract_text() or ""
        
        # PAN
        match = re.search(r"Permanent\s+Account\s+Number\s*\(PAN\)\s*([A-Z]{5}\d{4}[A-Z])", text, re.IGNORECASE)
        if match:
            header["pan"] = match.group(1).upper()
        
        # Name
        match = re.search(r"Name\s+of\s+Assessee[\s:]*([^\n]+)", text, re.IGNORECASE)
        if match:
            header["name"] = match.group(1).strip()
        
        # Financial Year
        match = re.search(r"Financial\s+Year\s*(\d{4}-\d{2})", text, re.IGNORECASE)
        if match:
            header["financial_year"] = match.group(1)
        
        # Assessment Year
        match = re.search(r"Assessment\s+Year\s*(\d{4}-\d{2})", text, re.IGNORECASE)
        if match:
            header["assessment_year"] = match.group(1)
        
        if header:
            break  # Got header from first page
    
    return header


def _extract_deductor_names_from_text(pdf) -> dict:
    """Extract deductor names and summary amounts from PDF text.
    
    The text extraction gives full names (not truncated by table cells).
    Pattern: SrNo Name (rest of line up to TAN) TAN Amount TDS
    """
    deductor_map = {}
    
    # Pattern: digit at start, then name up to 10-char alphanumeric TAN, then amounts
    # e.g.: "1 WELLS FARGO INTERNATIONAL SOLUTIONS PRIVATE LIMITED HYDW00345C 1964956.60 185112.00 185112.00"
    pattern = re.compile(
        r"^\s*(\d+)\s+"  # Sr No
        r"(.+?)\s+"      # Name (non-greedy, stops before TAN)
        r"([A-Z]{4}\d{4}[A-Z])\s+"  # TAN (4 letters + 4 digits + 1 letter)
        r"([\d,]+\.?\d*)\s+"  # Amount
        r"([\d,]+\.?\d*)\s+"  # TDS
        r"([\d,]+\.?\d*)",   # TDS Deposited
        re.MULTILINE
    )
    
    for page in pdf.pages:
        text = page.extract_text() or ""
        
        for match in pattern.finditer(text):
            sr_no = match.group(1)
            name = match.group(2).strip()
            tan = match.group(3).upper()
            amount = _to_float(match.group(4))
            tds = _to_float(match.group(5))
            tds_deposited = _to_float(match.group(6))
            
            deductor_map[tan] = {
                "name": name,
                "sr_no": sr_no,
                "amount": amount,
                "tds": tds,
                "tds_deposited": tds_deposited,
            }
    
    return deductor_map


def _extract_all_parts(pdf) -> dict:
    """Extract all 10 parts from Form 26AS PDF.
    
    Args:
        pdf: pdfplumber PDF object
        
    Returns:
        Dictionary with extracted data
    """
    data = {
        "header": {},
        "part1_tds_deductors": [],
        "part1_tds": [],  # Alias
        "part2_tds_15g15h": [],
        "part3_transactions_194b_194r_194s_194ba": [],
        "part4_tds_194ia_194ib_194m_194s_seller": [],
        "part5_transactions_26qe_seller": [],
        "part6_tcs_collectors": [],
        "part6_tcs": [],  # Alias
        "part7_paid_refunds": [],
        "part8_tds_194ia_194ib_194m_194s_buyer": [],
        "part9_transactions_26qe_buyer": [],
        "part10_tds_tcs_defaults": [],
        "source": "pdf",
    }
    
    # Track current deductor for PART-I nested transactions
    current_deductor = None
    
    for page_num, page in enumerate(pdf.pages, 1):
        tables = page.extract_tables()
        
        for table in tables:
            if not table or len(table) < 2:
                continue
                
            # Analyze table headers to determine what part this is
            table_type = _detect_table_type(table)
            
            if table_type == "header":
                _parse_header_table(table, data)
            elif table_type == "part1_deductor":
                current_deductor = _parse_part1_deductor_table(table, data, current_deductor)
            elif table_type == "part1_transaction":
                _parse_part1_transaction_table(table, current_deductor)
            elif table_type == "part2":
                _parse_part2_table(table, data)
            elif table_type == "part3":
                _parse_part3_table(table, data)
            elif table_type == "part4":
                _parse_part4_table(table, data)
            elif table_type == "part5":
                _parse_part5_table(table, data)
            elif table_type == "part6_collector":
                _parse_part6_collector_table(table, data)
            elif table_type == "part6_transaction":
                # TCS transactions (currently empty in most cases)
                pass
            elif table_type == "part7":
                _parse_part7_table(table, data)
            elif table_type == "part8":
                _parse_part8_table(table, data)
            elif table_type == "part9":
                _parse_part9_table(table, data)
            elif table_type == "part10":
                _parse_part10_table(table, data)
    
    return data


def _detect_table_type(table: list) -> str:
    """Detect what type of data this table contains based on headers."""
    if not table or len(table) < 1:
        return "unknown"
    
    # Flatten header row to check for keywords
    header_row = [str(cell).strip() if cell else "" for cell in table[0]]
    header_text = " ".join(header_row).upper()
    
    # Check for header info table (PAN, Name, Address)
    if "PERMANENT ACCOUNT NUMBER" in header_text or "PAN" in header_text:
        if "NAME OF ASSESSEE" in header_text or "ASSESSEE" in header_text:
            return "header"
    
    # Check for PART-I TDS Deductor summary
    if "NAME OF DEDUCTOR" in header_text and "TAN OF DEDUCTOR" in header_text:
        if "TOTAL AMOUNT" in header_text and "TOTAL TAX DEDUCTED" in header_text:
            return "part1_deductor"
    
    # Check for PART-I Transaction detail
    if "SECTION" in header_text and "TRANSACTION DATE" in header_text:
        if "STATUS OF BOOKING" in header_text and "AMOUNT PAID" in header_text:
            return "part1_transaction"
    
    # Check for PART-II 15G/15H
    if "SECTION" in header_text and "15G" in header_text or "15H" in header_text:
        return "part2"
    
    # Check for PART-III 194B/194R/194S/194BA
    if "ACKNOWLEDGEMENT" in header_text and "NAME OF DEDUCTOR" in header_text:
        if "194B" in header_text or "194R" in header_text or "194S" in header_text or "194BA" in header_text:
            return "part3"
    
    # Check for PART-IV 194IA/194IB/194M/194S seller
    if "ACKNOWLEDGEMENT NUMBER" in header_text and "NAME OF" in header_text:
        if "TOTAL TRANSACTION AMOUNT" in header_text and "TOTAL TDS DEPOSITED" in header_text:
            return "part4"
    
    # Check for PART-V 26QE
    if "NAME OF BUYER" in header_text or "NAME OF SELLER" in header_text:
        if "26QE" in header_text:
            return "part5"
    
    # Check for PART-VI TCS Collector
    if "NAME OF COLLECTOR" in header_text and "TAN OF COLLECTOR" in header_text:
        if "TOTAL TAX COLLECTED" in header_text:
            return "part6_collector"
    
    # Check for PART-VI TCS Transaction
    if "SECTION" in header_text and "TAX COLLECTED" in header_text:
        return "part6_transaction"
    
    # Check for PART-VII Refunds
    if "REFUND" in header_text and "ASSESSMENT YEAR" in header_text:
        return "part7"
    
    # Check for PART-VIII buyer/tenant
    if "NAME OF DEDUCTEE" in header_text or "DEDUCTEE" in header_text:
        return "part8"
    
    # Check for PART-IX 26QE
    if "NAME OF SELLER" in header_text and "26QE" in header_text:
        return "part9"
    
    # Check for PART-X Defaults
    if "SHORT PAYMENT" in header_text or "DEFAULT" in header_text:
        return "part10"
    
    return "unknown"


def _clean_cell(value) -> str:
    """Clean a table cell value."""
    if value is None:
        return ""
    return str(value).strip().replace('\n', ' ').replace('\r', ' ')


def _to_float(value: str) -> float:
    """Convert string to float, handling commas and empty values."""
    if not value or value == "":
        return 0.0
    try:
        return float(value.replace(',', '').strip())
    except (ValueError, AttributeError):
        return 0.0


def _is_data_row(row: list) -> bool:
    """Check if a table row contains actual data (not headers or empty)."""
    if not row or len(row) < 2:
        return False
    
    # First cell should be a number (serial number)
    first_cell = _clean_cell(row[0])
    if not first_cell.isdigit():
        return False
    
    # Check if row has some non-empty data cells
    non_empty = sum(1 for cell in row if cell and _clean_cell(cell))
    return non_empty >= 2


def _is_header_row(row: list) -> bool:
    """Check if a row is a header row."""
    if not row:
        return False
    first_cell = _clean_cell(row[0])
    # Header rows typically start with "Sr. No." or "Sr No" or "Sr.\nNo."
    return first_cell.startswith("Sr.") or first_cell.startswith("Sr No") or first_cell == ""


def _is_gross_total_row(row: list) -> bool:
    """Check if row is a gross total summary row."""
    if not row:
        return False
    first_cell = _clean_cell(row[0])
    return "GROSS TOTAL" in first_cell.upper() or first_cell == ""


def _parse_header_table(table: list, data: dict) -> None:
    """Parse header table with PAN, Name, Address."""
    for row in table:
        if not row:
            continue
        row_text = " ".join([_clean_cell(c) for c in row if c])
        
        # PAN
        if "PAN" in row_text.upper():
            for i, cell in enumerate(row):
                cell_text = _clean_cell(cell)
                if len(cell_text) == 10 and cell_text[:5].isalpha() and cell_text[5:9].isdigit() and cell_text[-1].isalpha():
                    data["header"]["pan"] = cell_text
                # Check for Financial Year / Assessment Year
                if "202" in cell_text and "-" in cell_text:
                    if len(cell_text) == 9:  # e.g., "2025-26"
                        if "fy" in row_text.lower() or "financial" in row_text.lower():
                            data["header"]["financial_year"] = cell_text
                        elif "ay" in row_text.lower() or "assessment" in row_text.lower():
                            data["header"]["assessment_year"] = cell_text
                # Status
                if "ACTIVE" in cell_text.upper() or "OPERATIVE" in cell_text.upper():
                    data["header"]["current_status"] = cell_text
        
        # Name
        if "NAME OF ASSESSEE" in row_text.upper() or "ASSESSEE" in row_text.upper():
            for cell in row[1:]:
                cell_text = _clean_cell(cell)
                if cell_text and len(cell_text) > 2 and not cell_text.startswith("20"):
                    data["header"]["name"] = cell_text
                    break
        
        # Address
        if "ADDRESS" in row_text.upper():
            addr_parts = []
            for cell in row[1:]:
                cell_text = _clean_cell(cell)
                if cell_text and len(cell_text) > 5:
                    addr_parts.append(cell_text)
            if addr_parts:
                data["header"]["address"] = ", ".join(addr_parts)


def _parse_part1_deductor_table(table: list, data: dict, current_deductor) -> dict:
    """Parse PART-I deductor summary table.
    
    This handles the composite table structure where deducor summaries
    and transactions are in the same table, alternating every few rows.
    Pattern: Dedcutor Header → Deductor Data → Tx Header → Tx Data → repeat
    """
    current_deductor = None
    in_transaction_section = False
    
    for row_idx, row in enumerate(table):
        if not row or len(row) < 6:
            continue
        
        first_cell = _clean_cell(row[0])
        
        # Check if this is a header row
        if first_cell.startswith("Sr.") or first_cell.startswith("Sr No"):
            # Check if this is a transaction header (has "Section" in row)
            row_text = " ".join([_clean_cell(c) for c in row])
            if "SECTION" in row_text.upper() and "TRANSACTION" in row_text.upper():
                in_transaction_section = True
            else:
                in_transaction_section = False
            continue
        
        # Skip empty rows
        if not first_cell or first_cell == "":
            continue
        
        # Skip gross total rows
        if "GROSS TOTAL" in first_cell.upper():
            continue
        
        # Now we have actual data - check if it's a deductor or transaction
        if not first_cell.isdigit():
            continue
        
        if in_transaction_section and current_deductor:
            # This is a transaction row
            # Columns: Sr No, Section, Date, Status, Booking Date, Remarks, Amount, Tax, TDS
            txn = {
                "sr_no": first_cell,
                "section": _clean_cell(row[1]),
                "transaction_date": _clean_cell(row[2]),
                "status_of_booking": _clean_cell(row[3]),
                "date_of_booking": _clean_cell(row[4]),
                "remarks": _clean_cell(row[5]),
                "amount": _to_float(row[6] if len(row) > 6 else ""),
                "tax_deducted": _to_float(row[7] if len(row) > 7 else ""),
                "tds_deposited": _to_float(row[8] if len(row) > 8 else ""),
            }
            current_deductor["transactions"].append(txn)
        else:
            # This is a deductor summary row
            # Columns: Sr No, Name, -, -, -, TAN, Amount, Tax, TDS
            deductor = {
                "sr_no": first_cell,
                "deductor_name": _clean_cell(row[1]),
                "tan": _clean_cell(row[5]) if len(row) > 5 else "",
                "amount_paid_credited": _to_float(row[6] if len(row) > 6 else ""),
                "tax_deducted": _to_float(row[7] if len(row) > 7 else ""),
                "tds_deposited": _to_float(row[8] if len(row) > 8 else ""),
                # Alias fields
                "total_amount": _to_float(row[6] if len(row) > 6 else ""),
                "total_tds": _to_float(row[7] if len(row) > 7 else ""),
                "total_tds_deposited": _to_float(row[8] if len(row) > 8 else ""),
                "transactions": [],
            }
            
            data["part1_tds_deductors"].append(deductor)
            data["part1_tds"].append(deductor)
            current_deductor = deductor
            in_transaction_section = False
    
    return current_deductor


def _parse_part1_transaction_table(table: list, deductor: dict) -> None:
    """Parse PART-I transaction detail table."""
    if not deductor:
        return
    
    # Skip header rows
    for row in table:
        if not row or len(row) < 7:
            continue
            
        first_cell = _clean_cell(row[0])
        if not first_cell.isdigit():
            continue
        
        # Columns: Sr No, Section, Date, Status, Booking Date, Remarks, Amount, Tax, TDS
        txn = {
            "sr_no": first_cell,
            "section": _clean_cell(row[1]),
            "transaction_date": _clean_cell(row[2]),
            "status_of_booking": _clean_cell(row[3]),
            "date_of_booking": _clean_cell(row[4]),
            "remarks": _clean_cell(row[5]),
            "amount": _to_float(row[6] if len(row) > 6 else ""),
            "tax_deducted": _to_float(row[7] if len(row) > 7 else ""),
            "tds_deposited": _to_float(row[8] if len(row) > 8 else ""),
        }
        
        deductor["transactions"].append(txn)


def _parse_part2_table(table: list, data: dict) -> None:
    """Parse PART-II 15G/15H table."""
    for row in table:
        if not row or len(row) < 6:
            continue
        
        first_cell = _clean_cell(row[0])
        if not first_cell.isdigit():
            continue
        
        # Similar to PART-I deductor
        entry = {
            "sr_no": first_cell,
            "deductor_name": _clean_cell(row[1]),
            "tan": _clean_cell(row[4]) if len(row) > 4 else "",
            "amount_paid_credited": _to_float(row[5] if len(row) > 5 else ""),
            "tax_deducted": _to_float(row[6] if len(row) > 6 else ""),
            "tds_deposited": _to_float(row[7] if len(row) > 7 else ""),
        }
        data["part2_tds_15g15h"].append(entry)


def _parse_part3_table(table: list, data: dict) -> None:
    """Parse PART-III 194B/194R/194S/194BA table."""
    for row in table:
        if not row or len(row) < 4:
            continue
        
        first_cell = _clean_cell(row[0])
        if not first_cell.isdigit():
            continue
        
        # Columns: Sr No, Ack No, Name, PAN, Total Amount
        entry = {
            "sr_no": first_cell,
            "acknowledgement_number": _clean_cell(row[1]),
            "name_of_deductor": _clean_cell(row[2]),
            "pan_of_deductor": _clean_cell(row[3]),
            "total_amount_paid_credited": _to_float(row[4] if len(row) > 4 else ""),
        }
        data["part3_transactions_194b_194r_194s_194ba"].append(entry)


def _parse_part4_table(table: list, data: dict) -> None:
    """Parse PART-IV 194IA/194IB/194M/194S seller table."""
    for row in table:
        if not row or len(row) < 7:
            continue
        
        first_cell = _clean_cell(row[0])
        if not first_cell.isdigit():
            continue
        
        # Columns: Sr No, Ack No, Name, PAN, Date, Amount, TDS
        entry = {
            "sr_no": first_cell,
            "acknowledgement_number": _clean_cell(row[1]),
            "name_of_deductor": _clean_cell(row[2]),
            "pan_of_deductor": _clean_cell(row[3]),
            "transaction_date": _clean_cell(row[4]),
            "total_transaction_amount": _to_float(row[5] if len(row) > 5 else ""),
            "total_tds_deposited": _to_float(row[6] if len(row) > 6 else ""),
        }
        data["part4_tds_194ia_194ib_194m_194s_seller"].append(entry)


def _parse_part5_table(table: list, data: dict) -> None:
    """Parse PART-V 26QE table."""
    for row in table:
        if not row or len(row) < 6:
            continue
        
        first_cell = _clean_cell(row[0])
        if not first_cell.isdigit():
            continue
        
        # Columns: Sr No, Ack No, Name, PAN, Date, Amount
        entry = {
            "sr_no": first_cell,
            "acknowledgement_number": _clean_cell(row[1]),
            "name_of_buyer": _clean_cell(row[2]),
            "pan_of_buyer": _clean_cell(row[3]),
            "transaction_date": _clean_cell(row[4]),
            "total_transaction_amount": _to_float(row[5] if len(row) > 5 else ""),
        }
        data["part5_transactions_26qe_seller"].append(entry)


def _parse_part6_collector_table(table: list, data: dict) -> None:
    """Parse PART-VI TCS collector table."""
    # Skip header row
    for row in table[1:]:
        if not row or len(row) < 5:
            continue
        
        first_cell = _clean_cell(row[0])
        if not first_cell.isdigit():
            continue
        
        # Columns: Sr No, Name, [merged], TAN, Amount, Tax, TCS
        entry = {
            "sr_no": first_cell,
            "name_of_collector": _clean_cell(row[1]),
            "tan_of_collector": _clean_cell(row[5]) if len(row) > 5 else "",
            "amount_paid_debited": _to_float(row[6] if len(row) > 6 else ""),
            "tax_collected": _to_float(row[7] if len(row) > 7 else ""),
            "tcs_deposited": _to_float(row[8] if len(row) > 8 else ""),
        }
        data["part6_tcs_collectors"].append(entry)
        data["part6_tcs"].append(entry)


def _parse_part7_table(table: list, data: dict) -> None:
    """Parse PART-VII refund table."""
    for row in table:
        if not row or len(row) < 8:
            continue
        
        first_cell = _clean_cell(row[0])
        if not first_cell.isdigit():
            continue
        
        # Columns: Sr No, AY, Mode, Ref No, Nature, Amount, Interest, Date, Remarks
        entry = {
            "sr_no": first_cell,
            "assessment_year": _clean_cell(row[1]),
            "mode": _clean_cell(row[2]),
            "refund_issued": _clean_cell(row[3]),
            "nature_of_refund": _clean_cell(row[4]),
            "amount_of_refund": _to_float(row[5] if len(row) > 5 else ""),
            "interest": _to_float(row[6] if len(row) > 6 else ""),
            "date_of_payment": _clean_cell(row[7]),
            "remarks": _clean_cell(row[8] if len(row) > 8 else ""),
        }
        data["part7_paid_refunds"].append(entry)


def _parse_part8_table(table: list, data: dict) -> None:
    """Parse PART-VIII buyer/tenant table."""
    for row in table:
        if not row or len(row) < 8:
            continue
        
        first_cell = _clean_cell(row[0])
        if not first_cell.isdigit():
            continue
        
        # Columns: Sr No, Ack No, Name, PAN, Date, Amount, TDS, Other
        entry = {
            "sr_no": first_cell,
            "acknowledgement_number": _clean_cell(row[1]),
            "name_of_deductee": _clean_cell(row[2]),
            "pan_of_deductee": _clean_cell(row[4]) if len(row) > 4 else "",
            "transaction_date": _clean_cell(row[5]) if len(row) > 5 else "",
            "total_transaction_amount": _to_float(row[6] if len(row) > 6 else ""),
            "total_tds_deposited": _to_float(row[7] if len(row) > 7 else ""),
            "total_amount_deposited_other_than_tds": _to_float(row[8] if len(row) > 8 else ""),
        }
        data["part8_tds_194ia_194ib_194m_194s_buyer"].append(entry)


def _parse_part9_table(table: list, data: dict) -> None:
    """Parse PART-IX 26QE buyer table."""
    for row in table:
        if not row or len(row) < 6:
            continue
        
        first_cell = _clean_cell(row[0])
        if not first_cell.isdigit():
            continue
        
        # Columns: Sr No, Ack No, Name, PAN, Date, Amount
        entry = {
            "sr_no": first_cell,
            "acknowledgement_number": _clean_cell(row[1]),
            "name_of_seller": _clean_cell(row[2]),
            "pan_of_seller": _clean_cell(row[4]) if len(row) > 4 else "",
            "transaction_date": _clean_cell(row[5]) if len(row) > 5 else "",
            "total_transaction_amount": _to_float(row[6] if len(row) > 6 else ""),
            "total_amount_deposited_other_than_tds": _to_float(row[7] if len(row) > 7 else ""),
        }
        data["part9_transactions_26qe_buyer"].append(entry)


def _parse_part10_table(table: list, data: dict) -> None:
    """Parse PART-X defaults table."""
    for row in table:
        if not row or len(row) < 9:
            continue
        
        first_cell = _clean_cell(row[0])
        if not first_cell.isdigit():
            continue
        
        # Columns: Sr No, FY, Short Pay, Short Ded, Int Pay, Int Ded, LFF, Int 220, Total
        entry = {
            "sr_no": first_cell,
            "financial_year": _clean_cell(row[1]),
            "short_payment": _to_float(row[2] if len(row) > 2 else ""),
            "short_deduction_collection": _to_float(row[3] if len(row) > 3 else ""),
            "interest_on_tds_tcs_payments_default": _to_float(row[4] if len(row) > 4 else ""),
            "interest_on_tds_tcs_deduction_default": _to_float(row[5] if len(row) > 5 else ""),
            "late_filing_fee_234e": _to_float(row[6] if len(row) > 6 else ""),
            "interest_u_s_220_2": _to_float(row[7] if len(row) > 7 else ""),
            "total_default": _to_float(row[8] if len(row) > 8 else ""),
        }
        data["part10_tds_tcs_defaults"].append(entry)
