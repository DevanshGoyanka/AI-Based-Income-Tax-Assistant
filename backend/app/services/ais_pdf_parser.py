"""AIS PDF Parser - handles the specific table structure of ITD AIS PDFs.

Each table in the AIS PDF has this structure:
- Row 1: Summary row (SR. NO, INFO CODE, DESCRIPTION, SOURCE, COUNT, AMOUNT)
- Row 2: Column headers for detail data
- Rows 3+: Detail data rows

The PDF has Part A (Personal Info) and Part B (Income Information).
"""
from __future__ import annotations

import re
from typing import Any


def _clean_text(text: str | None) -> str:
    """Clean extracted text, handling None and newlines."""
    if not text:
        return ""
    return re.sub(r"\s+", " ", str(text).replace("\n", " ")).strip()


def _safe_int(val) -> int:
    """Convert value to integer, handling commas."""
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


def parse_ais_pdf_tables(tables: list[dict], full_text: str) -> dict:
    """Parse AIS PDF tables into structured data.
    
    Args:
        tables: Raw tables extracted by pdfplumber
        full_text: Full text content from all pages
        
    Returns:
        Structured dict with parsed AIS data
    """
    result = {
        "pan": None,
        "name": None,
        "dob": None,
        "aadhaar": None,
        "mobile": None,
        "email": None,
        "address": None,
        "assessment_year": "2025-26",
        "tds_salary": [],
        "tds_others": [],
        "sft_dividend": [],
        "sft_interest": [],
        "sft_mutual_fund": [],
        "sft_other": [],
        "tax_payments": [],
        "demands_refunds": [],
        "other_information": [],
    }
    
    # Extract personal info from text
    _extract_part_a(result, full_text)
    
    # Parse each table
    for table in tables:
        headers = table.get("headers", [])
        data = table.get("data", [])
        
        if not data or len(data) < 2:
            continue
        
        # Skip non-data tables
        first_cell = _clean_text(data[0][0]) if data[0] else ""
        if not first_cell or first_cell.startswith("Download"):
            continue
        if "No Transactions Present" in first_cell:
            continue
        if "Annual Information Statement" in first_cell:
            continue
        if "PAN Name Financial Year" in first_cell:
            continue
            
        # Check if this is a TDS/SFT detail table (has "SR. NO." as second row)
        if len(data) >= 2:
            second_row = data[1]
            second_text = " ".join([_clean_text(c) for c in second_row if c])
            
            if "SR. NO." in second_text and ("QUARTER" in second_text or "DATE" in second_text or "REPORTED" in second_text or "DIVIDEND" in second_text):
                # This is a TDS/SFT detail table
                _parse_tds_sft_table(table, result)
            elif "MAJOR HEAD" in second_text:
                # This is a tax payment table
                _parse_tax_payment_table(table, result)
    
    return result


def _extract_part_a(result: dict, text: str) -> None:
    """Extract Part A personal information from text."""
    # PAN
    pan_match = re.search(r"Permanent Account Number \(PAN\)\s+([A-Z]{5}\d{4}[A-Z])", text)
    if pan_match:
        result["pan"] = pan_match.group(1)
    
    # Also try page 2 format: "COVPC5929M YASH UMESH CHANDAK 2025-26"
    pan_match2 = re.search(r"\b([A-Z]{5}\d{4}[A-Z])\s+([A-Z\s]+?)\s+(\d{4}-\d{2})", text)
    if pan_match2:
        if not result["pan"]:
            result["pan"] = pan_match2.group(1)
        result["name"] = pan_match2.group(2).strip()
        result["assessment_year"] = pan_match2.group(3)
    
    # Aadhaar
    aadhaar_match = re.search(r"Aadhaar Number\s+([\dX\s]{14,})", text)
    if aadhaar_match:
        result["aadhaar"] = aadhaar_match.group(1).strip()
    
    # Name (if not already extracted)
    if not result.get("name"):
        name_match = re.search(r"Name of Assessee\s+([A-Z][A-Z\s]{2,})", text)
        if name_match:
            result["name"] = name_match.group(1).strip()
    
    # DOB
    dob_match = re.search(r"Date of Birth\s+(\d{2}/\d{2}/\d{4})", text)
    if dob_match:
        result["dob"] = dob_match.group(1)
    
    # Mobile
    mobile_match = re.search(r"Mobile Number\s+(\d{10})", text)
    if mobile_match:
        result["mobile"] = mobile_match.group(1)
    
    # Email
    email_match = re.search(r"E-mail Address\s+([\w\.\-]+@[\w\.\-]+)", text)
    if email_match:
        result["email"] = email_match.group(1).strip()
    
    # Address
    address_match = re.search(r"Address\s+(.+?)(?:\n[A-Z][a-z]+|$)", text, re.DOTALL)
    if address_match:
        result["address"] = _clean_text(address_match.group(1))


def _parse_tds_sft_table(table: dict, result: dict) -> None:
    """Parse a TDS or SFT table."""
    data = table.get("data", [])
    if len(data) < 3:
        return
    
    # Row 1 is the summary row
    summary_row = data[0]
    # Row 2 is the column headers
    header_row = data[1]
    # Rows 3+ are data rows
    
    # Extract info from summary row
    info_code = ""
    description = ""
    source = ""
    count = ""
    amount_str = ""
    
    # Parse summary row - it has variable structure with None values
    non_none_values = [c for c in summary_row if c is not None and str(c).strip()]
    
    for i, cell in enumerate(non_none_values):
        cell_text = _clean_text(cell)
        if cell == non_none_values[0] and re.match(r'^\d+$', cell_text):
            # First value is serial number, skip
            continue
        if "TDS-" in cell_text or "SFT-" in cell_text or "TCS-" in cell_text:
            info_code = cell_text
        elif "SECTION" in cell_text.upper() or "(" in cell_text:
            description = cell_text
        elif re.match(r'^\d+$', cell_text) and not amount_str:
            count = cell_text
        elif re.match(r'^[\d,]+$', cell_text.replace(",", "")):
            amount_str = cell_text
        elif len(cell_text) > 10:
            source = cell_text
    
    # If source wasn't found, try the original row
    if not source and len(summary_row) > 5:
        source = _clean_text(summary_row[5])
    
    # If description wasn't found, try column index 3
    if not description and len(summary_row) > 3 and summary_row[3]:
        description = _clean_text(summary_row[3])
    
    # Determine section type
    section = _identify_section(info_code, description)
    
    # Build header map from header row
    hmap = _build_header_map(header_row)
    
    # Parse data rows
    for row_idx in range(2, len(data)):
        row = data[row_idx]
        if not row or all(c is None or str(c).strip() == "" for c in row):
            continue
        
        entry = _parse_detail_row(row, hmap, info_code, source, section)
        if entry:
            _add_entry(result, section, entry)


def _build_header_map(header_row: list) -> dict:
    """Build a map of header keywords to column indices."""
    hmap = {}
    for i, cell in enumerate(header_row):
        if cell is None:
            continue
        text = _clean_text(cell).lower()
        # Map common header variations
        if "sr" in text or "sl" in text:
            hmap["sr"] = i
        elif "quarter" in text:
            hmap["quarter"] = i
        elif "date" in text and ("payment" in text or "credit" in text):
            hmap["date"] = i
        elif "reported" in text and "on" in text:
            hmap["date"] = i
        elif "amount" in text and "paid" in text:
            hmap["amount_paid"] = i
        elif "amount" in text and ("credited" in text or "dividend" in text):
            hmap["amount"] = i
        elif "tds" in text and "deducted" in text:
            hmap["tds_deducted"] = i
        elif "tds" in text and "deposited" in text:
            hmap["tds_deposited"] = i
        elif "status" in text:
            hmap["status"] = i
        elif "quarter" in text:
            hmap["quarter"] = i
        elif "client" in text:
            hmap["client_id"] = i
        elif "amc" in text:
            hmap["amc"] = i
        elif "holder" in text:
            hmap["holder_flag"] = i
        elif "purchase" in text:
            hmap["purchase"] = i
        elif "sales" in text or "sale" in text:
            hmap["sales"] = i
    
    return hmap


def _identify_section(info_code: str, description: str) -> str:
    """Identify which section this entry belongs to."""
    info_code_upper = info_code.upper()
    desc_lower = description.lower()
    
    if "TDS-192" in info_code_upper or "SALARY" in info_code_upper:
        return "tds_salary"
    elif "TDS-" in info_code_upper or "TCS-" in info_code_upper:
        return "tds_others"
    elif "SFT-015" in info_code_upper or "SFT-15" in info_code_upper or "DIVIDEND" in info_code_upper:
        return "sft_dividend"
    elif "SFT-016" in info_code_upper or "SFT-16" in info_code_upper or "INTEREST" in info_code_upper:
        return "sft_interest"
    elif "SFT-018" in info_code_upper or "SFT-18" in info_code_upper or "MUTUAL" in info_code_upper:
        return "sft_mutual_fund"
    else:
        return "sft_other"


def _parse_detail_row(row: list, hmap: dict, info_code: str, source: str, section: str) -> dict | None:
    """Parse a detail row based on header map."""
    if not row:
        return None
    
    entry = {
        "info_code": info_code,
        "source": source,
        "sr_no": "",
        "quarter": "",
        "date": "",
        "amount_paid": 0,
        "amount": 0,
        "tds_deducted": 0,
        "tds_deposited": 0,
        "status": "",
    }
    
    # Extract TAN from source
    tan_match = re.search(r"\(([A-Z]{5}\d{4}[A-Z])\)", source)
    if tan_match:
        entry["tan"] = tan_match.group(1)
    
    # Extract based on section type
    for key, idx in hmap.items():
        if idx >= len(row) or row[idx] is None:
            continue
        
        value = _clean_text(row[idx])
        
        if key == "sr":
            entry["sr_no"] = value
        elif key == "quarter":
            entry["quarter"] = value
        elif key == "date":
            entry["date"] = value
        elif key == "amount_paid":
            entry["amount_paid"] = _safe_int(value)
        elif key == "amount":
            entry["amount"] = _safe_int(value)
        elif key == "tds_deducted":
            entry["tds_deducted"] = _safe_int(value)
        elif key == "tds_deposited":
            entry["tds_deposited"] = _safe_int(value)
        elif key == "status":
            entry["status"] = value
    
    # For SFT, use "amount" field
    if section.startswith("sft_"):
        if entry["amount"] > 0:
            entry["amount_paid"] = entry["amount"]
        # Add purchase/sales if present
        if "purchase" in hmap:
            entry["purchase_amount"] = _safe_int(_clean_text(row[hmap["purchase"]]))
        if "sales" in hmap:
            entry["sales_amount"] = _safe_int(_clean_text(row[hmap["sales"]]))
    
    # Skip rows with no amounts
    has_amount = (entry["amount_paid"] > 0 or 
                  entry["amount"] > 0 or 
                  entry["tds_deducted"] > 0 or
                  entry.get("purchase_amount", 0) > 0 or
                  entry.get("sales_amount", 0) > 0)
    
    if not has_amount:
        # Check if this is just a header row
        if entry["sr_no"].upper() in ("SR.", "SR. NO.", "SL NO", ""):
            return None
        # Skip rows without meaningful data
        return None
    
    return entry


def _add_entry(result: dict, section: str, entry: dict) -> None:
    """Add entry to the appropriate section."""
    if section in result and isinstance(result[section], list):
        result[section].append(entry)


def _parse_tax_payment_table(table: dict, result: dict) -> None:
    """Parse a tax payment table."""
    data = table.get("data", [])
    if not data or len(data) < 2:
        return
    
    headers = table.get("headers", [])
    header_row = data[0]  # First data row is the header
    
    # Build header map
    hmap = {}
    for i, cell in enumerate(header_row):
        if cell is None:
            continue
        text = _clean_text(cell).lower()
        if "major" in text:
            hmap["major_head"] = i
        elif "minor" in text:
            hmap["minor_head"] = i
        elif "tax" in text and "(" not in text:
            hmap["tax"] = i
        elif "surcharge" in text:
            hmap["surcharge"] = i
        elif "cess" in text:
            hmap["cess"] = i
        elif "total" in text:
            hmap["total"] = i
        elif "bsr" in text:
            hmap["bsr"] = i
        elif "date" in text and "deposit" in text:
            hmap["date"] = i
        elif "challan" in text and "serial" in text:
            hmap["challan_no"] = i
        elif "challan" in text and "ident" in text:
            hmap["challan_id"] = i
        elif "financial" in text:
            hmap["fy"] = i
    
    # Parse data rows
    for row_idx in range(1, len(data)):
        row = data[row_idx]
        if not row or all(c is None or str(c).strip() == "" for c in row):
            continue
        
        # Skip header-like rows
        first_cell = _clean_text(row[0]) if row else ""
        if first_cell in ("SR. NO.", "Sr.", "1"):
            continue
        if "No Transactions" in first_cell:
            continue
        
        entry = {
            "fy": _clean_text(row[hmap.get("fy", 0)]) if len(row) > 0 else "",
            "major_head": _clean_text(row[hmap.get("major_head", 1)]) if len(row) > 1 else "",
            "minor_head": _clean_text(row[hmap.get("minor_head", 2)]) if len(row) > 2 else "",
            "tax": _safe_int(row[hmap.get("tax", 3)]) if len(row) > 3 else 0,
            "surcharge": _safe_int(row[hmap.get("surcharge", 4)]) if len(row) > 4 else 0,
            "cess": _safe_int(row[hmap.get("cess", 5)]) if len(row) > 5 else 0,
            "total": _safe_int(row[hmap.get("total", 7)]) if len(row) > 7 else 0,
            "bsr": _clean_text(row[hmap.get("bsr", 8)]) if len(row) > 8 else "",
            "date": _clean_text(row[hmap.get("date", 9)]) if len(row) > 9 else "",
            "challan_no": _clean_text(row[hmap.get("challan_no", 10)]) if len(row) > 10 else "",
        }
        
        if entry["tax"] > 0 or entry["total"] > 0:
            result["tax_payments"].append(entry)
