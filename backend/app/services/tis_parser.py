"""TIS (Taxpayer Information Summary) JSON Parser.

TIS is the consolidated version of AIS shown in the e-Filing portal.
It merges 26AS + AIS data. Field names are similar to AIS but often
capitalised differently.

Known TIS sections:
  - tdsDetails[]         — All TDS entries
  - taxPaymentDetails[]  — Advance tax / self-assessment tax
  - interestIncome[]     — Interest income
  - dividendIncome[]     — Dividend income
  - capitalGainDetails[] — Capital gains from listed securities
  - refundDetails[]      — Refunds from previous years
  - highValueTransactions[] — Cash deposits, property, etc.
"""
from __future__ import annotations

from dataclasses import dataclass, field
from app.schemas.prefill import TDSRecord, AISInfoItem


def _safe_int(val) -> int:
    if val is None or val == "":
        return 0
    try:
        return int(float(val))
    except (ValueError, TypeError):
        return 0


@dataclass
class TISParsedData:
    """All data extracted from one TIS JSON."""
    tds_records: list[TDSRecord] = field(default_factory=list)
    info_items: list[AISInfoItem] = field(default_factory=list)
    tax_payments: list[dict] = field(default_factory=list)
    personal_info: dict = field(default_factory=dict)
    errors: list[str] = field(default_factory=list)

    @property
    def total_tds_tax(self) -> int:
        return sum(r.tax_deducted for r in self.tds_records)

    @property
    def total_tds_amount(self) -> int:
        return sum(r.amount_paid for r in self.tds_records)

    @property
    def deductor_count(self) -> int:
        return len({r.deductor_tan for r in self.tds_records})


def _pick(d: dict, *keys, default="") -> str:
    for k in keys:
        val = d.get(k, d.get(k.lower(), d.get(k.upper())))
        if val is not None and val != "":
            return str(val).strip()
    return str(default)


def _build_tds(d: dict) -> TDSRecord | None:
    """Build a TDSRecord from a TIS dict. Returns None if TAN is invalid."""
    amount = _safe_int(
        d.get("Amount") or d.get("AmountPaid") or d.get("amountCredited")
    )
    tax = _safe_int(
        d.get("TaxDeducted") or d.get("TDS") or d.get("taxDeducted")
    )
    deposited = _safe_int(
        d.get("TaxDeposited") or d.get("TaxDeposited") or d.get("taxDeposited") or tax
    )

    tan = _pick(d, "Tan", "TAN", "DeductorTAN").replace(" ", "")
    if len(tan) != 10:
        return None  # skip invalid TAN

    return TDSRecord(
        deductor_tan=tan,
        deductor_name=_pick(d, "DeductorName", "Name", "DeductedBy", "EmployerName"),
        section_code=_pick(d, "Section", "SectionCode", "sectionCode", "section") or "OTH",
        amount_paid=amount,
        tax_deducted=tax,
        tax_deposited=deposited,
        quarter=_pick(d, "Quarter", "quarter") or None,
        receipt_no=_pick(d, "ReceiptNo", "ChallanNo", "receiptNo") or None,
        date_of_credit=_pick(d, "TransactionDate", "DateOfCredit", "dateOfCredit") or None,
        date_of_deduction=_pick(d, "DateOfDeduction", "DeductionDate") or None,
        date_of_deposit=_pick(d, "DateOfDeposit", "DepositDate") or None,
        status=_pick(d, "Status", "status") or None,
        remarks=_pick(d, "Remarks", "remarks") or None,
    )


def _build_info(d: dict, info_type: str) -> AISInfoItem:
    amount = _safe_int(d.get("Amount") or d.get("GrossAmount") or d.get("amountCredited"))
    return AISInfoItem(
        info_type=info_type,
        description=_pick(d, "BankName", "FinancialInstitute", "Name", "NatureOfIncome"),
        amount=amount,
        pan_of_deductor=_pick(d, "PAN", "pan") or None,
        name_of_deductor=_pick(d, "DeductorName", "Name") or None,
        tan_of_deductor=_pick(d, "Tan", "TAN").replace(" ", "") or None,
        section_code=_pick(d, "Section", "SectionCode") or None,
        transaction_date=_pick(d, "TransactionDate", "DateOfTransaction") or None,
        remarks=_pick(d, "Remarks", "remarks") or None,
    )


def parse_tis_json(raw: dict) -> TISParsedData:
    """Parse a TIS JSON dict into structured data.

    Args:
        raw: The TIS JSON as a Python dict.

    Returns:
        TISParsedData with tds_records, info_items, tax_payments, personal_info.
    """
    result = TISParsedData()

    # ── Personal Info ───────────────────────────────────────────────────────
    pi = raw.get("personalInfo") or raw.get("PersonalInfo") or {}
    if pi:
        name_obj = pi.get("assesseeName") or {}
        last_name = name_obj.get("surName") or name_obj.get("surNameOrOrgName") or ""
        result.personal_info = {
            "pan": _pick(pi, "pan", "PAN"),
            "name": " ".join(filter(None, [
                name_obj.get("firstName", ""),
                name_obj.get("middleName", ""),
                last_name,
            ])).strip(),
            "dob": _pick(pi, "dob", "DOB"),
        }

    # ── TDS Details ─────────────────────────────────────────────────────────
    for d in raw.get("tdsDetails") or raw.get("TdsDetails") or raw.get("TDSDetails") or []:
        rec = _build_tds(d)
        if rec:
            result.tds_records.append(rec)

    # ── Tax Payments (Advance tax, Self-assessment tax) ─────────────────────
    for d in raw.get("taxPaymentDetails") or raw.get("TaxPaymentDetails") or []:
        result.tax_payments.append({
            "bsr_code": _pick(d, "bsrCode", "BSRCode"),
            "serial_no": _pick(d, "serialNo", "SerialNo", "SlNo"),
            "date_of_deposit": _pick(d, "dateOfDeposit", "DateOfDeposit"),
            "major_head": _pick(d, "majorHead", "MajorHead"),
            "minor_head": _pick(d, "minorHead", "MinorHead"),
            "tax_amount": _safe_int(d.get("taxAmount") or d.get("TaxAmount")),
            "interest_amount": _safe_int(d.get("interestAmount") or d.get("InterestAmount")),
            "total_amount": _safe_int(d.get("totalAmount") or d.get("TotalAmount")),
            "challan_no": _pick(d, "challanNo", "ChallanNo"),
            "remarks": _pick(d, "remarks", "Remarks"),
        })

    # ── Interest Income ─────────────────────────────────────────────────────
    for d in raw.get("interestIncome") or raw.get("InterestIncome") or []:
        result.info_items.append(_build_info(d, "interest"))

    # ── Dividend Income ─────────────────────────────────────────────────────
    for d in raw.get("dividendIncome") or raw.get("DividendIncome") or []:
        result.info_items.append(_build_info(d, "dividend"))

    # ── Capital Gains ───────────────────────────────────────────────────────
    for d in raw.get("capitalGainDetails") or raw.get("CapitalGainDetails") or []:
        result.info_items.append(_build_info(d, "capital_gain"))

    # ── High Value Transactions ─────────────────────────────────────────────
    for d in raw.get("highValueTransactions") or raw.get("HighValueTransactions") or []:
        result.info_items.append(_build_info(d, "high_value_txn"))

    # ── Refund Details ──────────────────────────────────────────────────────
    for d in raw.get("refundDetails") or raw.get("RefundDetails") or []:
        result.info_items.append(_build_info(d, "refund"))

    return result
