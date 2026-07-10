"""Comprehensive AIS JSON Parser - extracts ALL parts from v15.0.0+ structure."""
from __future__ import annotations

from dataclasses import dataclass, field
from typing import Optional
from app.schemas.prefill import TDSRecord, AISInfoItem


def _safe_int(val) -> int:
    """Convert value to integer safely."""
    if val is None or val == "":
        return 0
    try:
        # Remove commas and parse
        cleaned = str(val).replace(",", "").strip()
        return int(float(cleaned))
    except (ValueError, TypeError):
        return 0


def _safe_float(val) -> float:
    """Convert value to float safely."""
    if val is None or val == "":
        return 0.0
    try:
        cleaned = str(val).replace(",", "").strip()
        return float(cleaned)
    except (ValueError, TypeError):
        return 0.0


@dataclass
class TaxPaymentRecord:
    """Tax payment entry from Part B3."""
    bsr_code: str = ""
    challan_serial_no: str = ""
    challan_date: str = ""
    tax_amount: int = 0
    interest_amount: int = 0
    total_amount: int = 0
    major_head: str = ""
    minor_head: str = ""
    status: str = ""


@dataclass
class DemandRefundRecord:
    """Demand/Refund entry from Part B4."""
    assessment_year: str = ""
    demand_id: str = ""
    demand_status: str = ""
    demand_amount: int = 0
    refund_status: str = ""
    refund_amount: int = 0
    date: str = ""


@dataclass
class OtherInfoRecord:
    """Other information entry from Part B7."""
    info_type: str = ""
    description: str = ""
    amount: int = 0
    details: dict = field(default_factory=dict)


@dataclass
class AISParsedData:
    """Complete parsed AIS data with all parts."""
    # Personal Info
    personal_info: dict = field(default_factory=dict)
    
    # Part B1 - TDS/TCS
    tds_records: list[TDSRecord] = field(default_factory=list)
    
    # Part B2 - SFT (Specified Financial Transactions)
    sft_records: list[AISInfoItem] = field(default_factory=list)
    
    # Part B3 - Tax Payments
    tax_payments: list[TaxPaymentRecord] = field(default_factory=list)
    
    # Part B4 - Demands and Refunds
    demands_refunds: list[DemandRefundRecord] = field(default_factory=list)
    
    # Part B7 - Other Information
    other_info: list[OtherInfoRecord] = field(default_factory=list)
    
    # Bank Accounts
    bank_accounts: list[dict] = field(default_factory=list)
    
    # Errors
    errors: list[str] = field(default_factory=list)
    
    @property
    def total_tds_tax(self) -> int:
        return sum(r.tax_deducted for r in self.tds_records)
    
    @property
    def total_tds_amount(self) -> int:
        return sum(r.amount_paid for r in self.tds_records)
    
    @property
    def deductor_count(self) -> int:
        return len({r.deductor_tan for r in self.tds_records if r.deductor_tan})
    
    @property
    def total_sft_amount(self) -> int:
        return sum(r.amount for r in self.sft_records)
    
    @property
    def total_tax_paid(self) -> int:
        return sum(p.total_amount for p in self.tax_payments)


def _build_field_map(labels: list) -> dict:
    """Build field name to index mapping from column labels."""
    field_map = {}
    for idx, label_obj in enumerate(labels):
        if isinstance(label_obj, dict):
            field_name = label_obj.get("field", "")
            if field_name:
                field_map[field_name] = idx
    return field_map


def _get_row_value(row: list, field_map: dict, field_name: str, default="") -> str:
    """Get value from row using field map."""
    idx = field_map.get(field_name, -1)
    if idx >= 0 and idx < len(row):
        return str(row[idx]) if row[idx] is not None else ""
    return str(default)


def _parse_tds_tcs_section(section: dict, result: AISParsedData):
    """Parse Part B1 - TDS/TCS section."""
    elements = section.get("elements", [])
    
    for element in elements:
        title = element.get("title", "")
        info_src_id = element.get("infoSrcId", "") or ""
        
        l1_data = element.get("l1", {})
        l1_labels = l1_data.get("columnLabel", [])
        l1_rows = l1_data.get("columnData", [])
        
        field_map = _build_field_map(l1_labels)
        
        # Get info code from l2 (section code)
        l2_data = element.get("l2", {})
        l2_rows = l2_data.get("columnData", [])
        section_code = ""
        deductor_name = ""
        
        if l2_rows and isinstance(l2_rows[0], list) and len(l2_rows[0]) >= 4:
            source_str = str(l2_rows[0][3])
            # Format: "EMPLOYER NAME (TAN)"
            if "(" in source_str and ")" in source_str:
                deductor_name = source_str.split("(")[0].strip()
            else:
                deductor_name = source_str
            
            if len(l2_rows[0]) >= 2:
                section_code = str(l2_rows[0][1]).replace("TDS-", "").replace("TCS-", "")
        
        # Get TAN from info_src_id or row
        tan = info_src_id.split(".")[0] if info_src_id else ""
        if not tan or len(tan) != 10:
            tan = _get_row_value([], field_map, "tan", "")
        
        # Process each transaction
        for row in l1_rows:
            if not isinstance(row, list) or len(row) < 5:
                continue
            
            tsn = _get_row_value(row, field_map, "tsnId", "")
            quarter = _get_row_value(row, field_map, "quarter", "")
            trans_date = _get_row_value(row, field_map, "transactionDate", "")
            amount_str = _get_row_value(row, field_map, "amtPaid", "0")
            tax_deducted_str = _get_row_value(row, field_map, "amountDeducted", "0")
            tax_deposited_str = _get_row_value(row, field_map, "amountDeposited", "0")
            status = _get_row_value(row, field_map, "status", "")
            
            # TAN from row if not in info_src_id
            row_tan = _get_row_value(row, field_map, "tan", "")
            if row_tan and len(row_tan) == 10:
                tan = row_tan
            elif not tan or len(tan) != 10:
                tan = "UNKNOWN000"
            
            amount_paid = _safe_int(amount_str)
            tax_deducted = _safe_int(tax_deducted_str)
            tax_deposited = _safe_int(tax_deposited_str)
            
            if section_code and amount_paid > 0:
                result.tds_records.append(TDSRecord(
                    deductor_tan=tan,
                    deductor_name=deductor_name or title,
                    section_code=section_code,
                    amount_paid=amount_paid,
                    tax_deducted=tax_deducted,
                    tax_deposited=tax_deposited,
                    quarter=quarter or None,
                    receipt_no=tsn or None,
                    date_of_credit=trans_date or None,
                    date_of_deduction=None,
                    date_of_deposit=None,
                    status=status or None,
                    remarks=title,
                ))


def _parse_sft_section(section: dict, result: AISParsedData):
    """Parse Part B2 - SFT (Specified Financial Transactions)."""
    elements = section.get("elements", [])
    
    for element in elements:
        title = element.get("title", "")
        info_src_id = element.get("infoSrcId", "") or ""
        
        l1_data = element.get("l1", {})
        if not l1_data:
            continue
            
        l1_labels = l1_data.get("columnLabel", [])
        l1_rows = l1_data.get("columnData", [])
        
        field_map = _build_field_map(l1_labels)
        
        # Get SFT code from l2
        l2_data = element.get("l2", {})
        l2_rows = l2_data.get("columnData", [])
        sft_code = ""
        source_name = ""
        
        if l2_rows and isinstance(l2_rows[0], list) and len(l2_rows[0]) >= 4:
            if len(l2_rows[0]) >= 2:
                sft_code = str(l2_rows[0][1])
            source_str = str(l2_rows[0][3])
            if "(" in source_str:
                source_name = source_str.split("(")[0].strip()
            else:
                source_name = source_str
        
        # Determine info type from SFT code
        info_type = _get_sft_info_type(sft_code, title)
        
        # Process each transaction
        for row in l1_rows:
            if not isinstance(row, list) or len(row) < 3:
                continue
            
            tsn = _get_row_value(row, field_map, "tsnId", "")
            trans_date = _get_row_value(row, field_map, "reportedOn", "") or _get_row_value(row, field_map, "transactionDate", "")
            
            # Handle different SFT types with different amount fields
            amount = 0
            sale_amount = 0
            purchase_amount = 0
            
            if "amtcash" in field_map:
                amount = _safe_int(_get_row_value(row, field_map, "amtcash", "0"))
            elif "amtPaid" in field_map:
                amount = _safe_int(_get_row_value(row, field_map, "amtPaid", "0"))
            elif "amount" in field_map:
                amount = _safe_int(_get_row_value(row, field_map, "amount", "0"))
            
            # For mutual fund purchases - use totalPurchaseAmount
            if "totalPurchaseAmount" in field_map:
                purchase_amount = _safe_int(_get_row_value(row, field_map, "totalPurchaseAmount", "0"))
                amount = purchase_amount
            
            # For mutual fund sales - use totalSalesValue
            if "totalSalesValue" in field_map:
                sale_amount = _safe_int(_get_row_value(row, field_map, "totalSalesValue", "0"))
                # For sales, use sales value; for purchases, use purchase amount
                if sale_amount > 0:
                    amount = sale_amount
                elif amount == 0:
                    amount = purchase_amount
            
            pan_of_source = info_src_id.split(".")[0] if "." in info_src_id else info_src_id
            
            result.sft_records.append(AISInfoItem(
                info_type=info_type,
                description=title,
                amount=amount,
                pan_of_deductor=pan_of_source or None,
                name_of_deductor=source_name or None,
                tan_of_deductor=None,
                section_code=sft_code or None,
                transaction_date=trans_date or None,
                remarks=f"TSN: {tsn}" if tsn else None,
            ))


def _get_sft_info_type(sft_code: str, title: str) -> str:
    """Map SFT code to info type."""
    code_upper = sft_code.upper()
    title_lower = title.lower()
    
    if "016(SB)" in code_upper or "SAVINGS" in code_upper:
        return "interest_savings_bank"
    elif "016(TD)" in code_upper or "TERM DEPOSIT" in code_upper:
        return "interest_term_deposit"
    elif "016" in code_upper:
        return "interest_bank"
    elif "015" in code_upper or "DIVIDEND" in code_upper:
        return "dividend"
    elif "005" in code_upper or "006" in code_upper:
        return "property_purchase"
    elif "007" in code_upper or "008" in code_upper:
        return "property_sale"
    elif "013" in code_upper:
        return "credit_card"
    elif "010" in code_upper or "MUTUAL FUND" in title_lower:
        return "mutual_fund"
    elif "012" in code_upper or "SECURITIES" in title_lower:
        return "shares_securities"
    elif "014" in code_upper:
        return "vehicle"
    elif "18" in code_upper or "PURCHASE" in title_lower or "SALE" in title_lower:
        return "mutual_fund_purchase_sale"
    elif "007" in code_upper:
        return "cash_deposit"
    elif "008" in code_upper:
        return "cash_withdrawal"
    else:
        return "sft_other"


def _parse_payment_of_taxes_section(section: dict, result: AISParsedData):
    """Parse Part B3 - Payment of Taxes."""
    elements = section.get("elements", [])
    
    for element in elements:
        l1_data = element.get("l1", {})
        if not l1_data:
            continue
            
        l1_labels = l1_data.get("columnLabel", [])
        l1_rows = l1_data.get("columnData", [])
        
        field_map = _build_field_map(l1_labels)
        
        for row in l1_rows:
            if not isinstance(row, list) or len(row) < 5:
                continue
            
            tax_date = _get_row_value(row, field_map, "taxDate", "") or _get_row_value(row, field_map, "challanDate", "")
            bsr_code = _get_row_value(row, field_map, "bsrCode", "") or _get_row_value(row, field_map, "bsr", "")
            challan_serial = _get_row_value(row, field_map, "challanSerialNo", "") or _get_row_value(row, field_map, "serialNo", "")
            major_head = _get_row_value(row, field_map, "majorHead", "") or _get_row_value(row, field_map, "mjrHead", "")
            minor_head = _get_row_value(row, field_map, "minorHead", "") or _get_row_value(row, field_map, "mnrHead", "")
            tax_amount_str = _get_row_value(row, field_map, "taxAmount", "0") or _get_row_value(row, field_map, "tax", "0")
            interest_str = _get_row_value(row, field_map, "interestAmount", "0") or _get_row_value(row, field_map, "interest", "0")
            total_str = _get_row_value(row, field_map, "totalAmount", "0") or _get_row_value(row, field_map, "total", "0")
            status = _get_row_value(row, field_map, "status", "")
            
            tax_amount = _safe_int(tax_amount_str)
            interest_amount = _safe_int(interest_str)
            total_amount = _safe_int(total_str)
            
            if tax_amount > 0 or total_amount > 0:
                result.tax_payments.append(TaxPaymentRecord(
                    bsr_code=bsr_code,
                    challan_serial_no=challan_serial,
                    challan_date=tax_date,
                    tax_amount=tax_amount,
                    interest_amount=interest_amount,
                    total_amount=total_amount or (tax_amount + interest_amount),
                    major_head=major_head,
                    minor_head=minor_head,
                    status=status,
                ))


def _parse_demand_refund_section(section: dict, result: AISParsedData):
    """Parse Part B4 - Demand and Refund."""
    elements = section.get("elements", [])
    
    for element in elements:
        l1_data = element.get("l1", {})
        if not l1_data:
            continue
            
        l1_labels = l1_data.get("columnLabel", [])
        l1_rows = l1_data.get("columnData", [])
        
        field_map = _build_field_map(l1_labels)
        
        for row in l1_rows:
            if not isinstance(row, list) or len(row) < 3:
                continue
            
            ay = _get_row_value(row, field_map, "assessmentYear", "") or _get_row_value(row, field_map, "AY", "")
            demand_id = _get_row_value(row, field_map, "demandId", "") or _get_row_value(row, field_map, "id", "")
            demand_status = _get_row_value(row, field_map, "demandStatus", "") or _get_row_value(row, field_map, "status", "")
            demand_amount_str = _get_row_value(row, field_map, "demandAmount", "0") or _get_row_value(row, field_map, "amount", "0")
            refund_status = _get_row_value(row, field_map, "refundStatus", "") or _get_row_value(row, field_map, "refundStatus", "")
            refund_amount_str = _get_row_value(row, field_map, "refundAmount", "0") or _get_row_value(row, field_map, "refund", "0")
            date = _get_row_value(row, field_map, "date", "") or _get_row_value(row, field_map, "demandDate", "")
            
            demand_amount = _safe_int(demand_amount_str)
            refund_amount = _safe_int(refund_amount_str)
            
            if demand_amount > 0 or refund_amount > 0 or demand_id:
                result.demands_refunds.append(DemandRefundRecord(
                    assessment_year=ay,
                    demand_id=demand_id,
                    demand_status=demand_status,
                    demand_amount=demand_amount,
                    refund_status=refund_status,
                    refund_amount=refund_amount,
                    date=date,
                ))


def _parse_other_info_section(section: dict, result: AISParsedData):
    """Parse Part B7 - Other Information."""
    elements = section.get("elements", [])
    
    for element in elements:
        title = element.get("title", "")
        
        l1_data = element.get("l1", {})
        if not l1_data:
            continue
            
        l1_labels = l1_data.get("columnLabel", [])
        l1_rows = l1_data.get("columnData", [])
        
        field_map = _build_field_map(l1_labels)
        
        for row in l1_rows:
            if not isinstance(row, list):
                continue
            
            # Build details dict from all row values
            details = {}
            for idx, label in enumerate(l1_labels):
                if isinstance(label, dict):
                    field_name = label.get("field", f"field_{idx}")
                    if idx < len(row):
                        details[field_name] = row[idx]
            
            # Extract amount
            amount = 0
            if "grossSalary171" in field_map:
                amount = _safe_int(_get_row_value(row, field_map, "grossSalary171", "0"))
            elif "grossSalary" in field_map:
                amount = _safe_int(_get_row_value(row, field_map, "grossSalary", "0"))
            elif "amount" in field_map:
                amount = _safe_int(_get_row_value(row, field_map, "amount", "0"))
            
            result.other_info.append(OtherInfoRecord(
                info_type=title,
                description=title,
                amount=amount,
                details=details,
            ))


def _parse_personal_info(ais_data: dict, result: AISParsedData):
    """Parse Part A - Personal Information."""
    part_a = ais_data.get("partA", {})
    if not part_a:
        return
    
    labels = part_a.get("columnLabel", [])
    data = part_a.get("columnData", [])
    
    if labels and data and isinstance(data, list):
        pi_dict = dict(zip(labels, data))
        result.personal_info = {
            "pan": pi_dict.get("Permanent Account Number (PAN)", ""),
            "name": pi_dict.get("Name of Assessee", ""),
            "dob": pi_dict.get("Date of Birth", ""),
            "aadhaar_masked": pi_dict.get("Aadhaar Number", ""),
            "email": pi_dict.get("E-mail Address", ""),
            "mobile": pi_dict.get("Mobile Number", ""),
            "address": pi_dict.get("Address", ""),
        }


def parse_ais_json(raw: dict) -> AISParsedData:
    """Parse complete AIS JSON into structured data.
    
    Args:
        raw: Decrypted AIS JSON as dict.
        
    Returns:
        AISParsedData with all sections parsed.
    """
    result = AISParsedData()
    
    # Parse Personal Info
    _parse_personal_info(raw, result)
    
    # Parse Part B sections
    part_b = raw.get("partB", {})
    sections = part_b.get("sections", [])
    
    for section in sections:
        section_key = section.get("sectionKey", "")
        
        if section_key == "tdsTcs":
            _parse_tds_tcs_section(section, result)
        elif section_key == "sft":
            _parse_sft_section(section, result)
        elif section_key == "paymentOfTaxes":
            _parse_payment_of_taxes_section(section, result)
        elif section_key == "demandAndRefund":
            _parse_demand_refund_section(section, result)
        elif section_key == "other-info":
            _parse_other_info_section(section, result)
    
    return result
