"""Form 16 Parser — extracts salary income from ITD Form 16 Part A/B.

Form 16 is issued by employers and has two parts:
  - Part A: TDS Certificate (deductor details + salary + TDS)
  - Part B: Annual statement (gross salary breakdown, deductions, etc.)

Format: Password-protected PDF or plain text. The salary fields we care about:
  - Gross salary (Section 17(1))
  - Value of perquisites (17(2))
  - Profits in lieu of salary (17(3))
  - Deductions from salary:
      - Standard deduction u/s 16(ia): ₹75,000 (AY 26-27 new regime)
      - Professional tax u/s 16(iii): actual amount
  - HRA exemption details (if applicable)
  - Leave Travel Concession (LTC) exemption
  - Exemptions under Section 10

This parser handles the plain JSON export from the ITD Form 16 utility,
as well as PDF text extraction.
"""
from __future__ import annotations

import re
from dataclasses import dataclass, field
from typing import Literal

from app.schemas.prefill import TDSRecord


def _safe_int(val) -> int:
    if val is None or val == "":
        return 0
    try:
        return int(float(str(val).replace(",", "")))
    except (ValueError, TypeError):
        return 0


@dataclass
class Form16ParsedData:
    """All salary data extracted from Form 16."""
    # Employer details
    employer_name: str = ""
    employer_tan: str = ""
    employer_pan: str = ""
    employer_address: str = ""

    # Employee details
    employee_name: str = ""
    employee_pan: str = ""
    employee_dob: str = ""

    # Period of employment
    from_date: str = ""
    to_date: str = ""

    # Gross salary components
    gross_salary: int = 0
    value_of_perquisites: int = 0
    profits_in_lieu: int = 0

    # Section 10 exemptions
    exempt_10_13a: int = 0   # HRA
    exempt_10_5: int = 0     # LTC
    exempt_10_10: int = 0    # Gratuity
    exempt_10_10aa: int = 0  # Leave encashment
    exempt_10_10c: int = 0   # VRS/remuneration
    exempt_10_14: int = 0    # Other allowances
    exempt_other: int = 0

    # Deductions from salary (Chapter VI)
    standard_deduction_16ia: int = 0
    professional_tax_16iii: int = 0
    entertainment_tax_16ii: int = 0

    # Net salary (after exemptions + deductions)
    net_salary: int = 0

    # TDS on salary from Form 16
    tds_deducted: int = 0
    tds_deposited: int = 0
    tds_deduction_date: str = ""

    # HRA detail (from Form 10RA / employer calc)
    hra_received: int = 0
    hra_exempt: int = 0
    hra_actual_rent_paid: int = 0
    hra_city_tier: str = ""  # "metro" or "non_metro"

    # Section 80C (from Part B)
    sec_80c_claimed: int = 0
    sec_80ccd1b_claimed: int = 0

    # JSON or PDF raw source
    raw_source: dict = field(default_factory=dict)
    errors: list[str] = field(default_factory=list)

    @property
    def total_exemptions_10(self) -> int:
        return (self.exempt_10_13a + self.exempt_10_5 + self.exempt_10_10 +
                self.exempt_10_10aa + self.exempt_10_10c + self.exempt_10_14 +
                self.exempt_other)

    @property
    def total_deductions(self) -> int:
        return (self.standard_deduction_16ia +
                self.professional_tax_16iii +
                self.entertainment_tax_16ii)

    @property
    def salary_after_exemptions(self) -> int:
        return self.gross_salary - self.total_exemptions_10

    @property
    def salary_after_all(self) -> int:
        return self.salary_after_exemptions - self.total_deductions


def _pick(d: dict, *keys, default="") -> str:
    for k in keys:
        val = d.get(k, d.get(k.lower(), d.get(k.upper())))
        if val is not None and val != "":
            return str(val).strip()
    return str(default)


def _extract_amount(d: dict, *keys) -> int:
    for k in keys:
        val = d.get(k, d.get(k.lower(), d.get(k.upper())))
        if val is not None and val != "":
            return _safe_int(val)
    return 0


def parse_form16_json(raw: dict) -> Form16ParsedData:
    """Parse a Form 16 JSON export dict into Form16ParsedData.

    Form 16 JSON from ITD utility has this approximate structure:
    {
      "employerDetails": {
        "name": "...", "tan": "...", "pan": "...", "address": "..."
      },
      "employeeDetails": {
        "name": "...", "pan": "...", "dob": "..."
      },
      "employmentPeriod": {
        "from": "01-Apr-2025", "to": "31-Mar-2026"
      },
      "salary": {
        "grossSalary": 1500000,
        "valueOfPerquisites": 0,
        "profitsInLieuOfSalary": 0,
        "exemptionsUnderSection10": {
          "10_13A": 180000,   // HRA
          "10_5": 0,          // LTC
          "10_10": 0,         // Gratuity
          "10_10AA": 0,       // Leave encashment
          "10_10C": 0,        // VRS
          "10_14": 0,         // Other allowances
        },
        "deductions": {
          "standardDeduction": 75000,
          "professionalTax": 2500,
        },
        "netSalary": 1237500
      },
      "tdsDetails": {
        "taxDeducted": 150000,
        "taxDeposited": 150000,
        "dateOfDeduction": "31-Mar-2026"
      },
      "hraDetails": {
        "received": 360000,
        "exempt": 180000,
        "actualRentPaid": 240000,
        "cityTier": "metro"
      },
      "deductions80C": 150000,
      "deductions80CCD1B": 50000,
    }
    """
    result = Form16ParsedData(raw_source=raw)

    # ── Employer Details ────────────────────────────────────────────────────
    emp = raw.get("employerDetails") or raw.get("employer_details") or raw.get("EmployerDetails") or {}
    result.employer_name = _pick(emp, "name", "Name", "employerName")
    result.employer_tan = _pick(emp, "tan", "TAN", "employerTan").replace(" ", "")
    result.employer_pan = _pick(emp, "pan", "PAN", "employerPan")
    result.employer_address = _pick(emp, "address", "Address")

    # ── Employee Details ────────────────────────────────────────────────────
    emp2 = raw.get("employeeDetails") or raw.get("employee_details") or raw.get("EmployeeDetails") or {}
    result.employee_name = _pick(emp2, "name", "Name", "employeeName")
    result.employee_pan = _pick(emp2, "pan", "PAN", "employeePan")
    result.employee_dob = _pick(emp2, "dob", "DOB", "dateOfBirth")

    # ── Employment Period ───────────────────────────────────────────────────
    period = raw.get("employmentPeriod") or raw.get("employment_period") or {}
    result.from_date = _pick(period, "from", "From", "fromDate")
    result.to_date = _pick(period, "to", "To", "toDate")

    # ── Salary Components ───────────────────────────────────────────────────
    sal = raw.get("salary") or raw.get("Salary") or {}
    result.gross_salary = _extract_amount(sal, "grossSalary", "gross_salary", "GrossSalary", "salary")
    result.value_of_perquisites = _extract_amount(
        sal, "valueOfPerquisites", "value_of_perquisites", "ValueOfPerquisites"
    )
    result.profits_in_lieu = _extract_amount(
        sal, "profitsInLieuOfSalary", "profits_in_lieu", "ProfitsInLieu"
    )

    # ── Section 10 Exemptions ───────────────────────────────────────────────
    ex10 = (sal.get("exemptionsUnderSection10") or
            sal.get("exemptions_10") or
            sal.get("exemptions") or
            {})
    result.exempt_10_13a = _extract_amount(ex10, "10_13A", "10(13A)", "10.13A", "10-13A", "hra")
    result.exempt_10_5 = _extract_amount(ex10, "10_5", "10(5)", "ltc")
    result.exempt_10_10 = _extract_amount(ex10, "10_10", "10(10)", "gratuity")
    result.exempt_10_10aa = _extract_amount(ex10, "10_10AA", "10(10AA)", "10.10AA", "leaveEncashment")
    result.exempt_10_10c = _extract_amount(ex10, "10_10C", "10(10C)", "10.10C", "vrs")
    result.exempt_10_14 = _extract_amount(ex10, "10_14", "10(14)", "10.14", "otherAllowances")
    # Catch-all for any other exemption keys
    for k, v in ex10.items():
        if isinstance(v, (int, float, str)) and v and k not in (
            "10_13A", "10(13A)", "10_5", "10(5)", "10_10", "10(10)",
            "10_10AA", "10(10AA)", "10_10C", "10(10C)", "10_14", "10(14)"
        ):
            result.exempt_other += _safe_int(v)

    # ── Deductions from Salary ──────────────────────────────────────────────
    ded = sal.get("deductions") or sal.get("Deductions") or {}
    result.standard_deduction_16ia = _extract_amount(
        ded, "standardDeduction", "standard_deduction", "16ia", "StdDeduction"
    )
    result.professional_tax_16iii = _extract_amount(
        ded, "professionalTax", "professional_tax", "16iii", "ProfTax"
    )
    result.entertainment_tax_16ii = _extract_amount(
        ded, "entertainmentTax", "entertainment_tax", "16ii"
    )
    result.net_salary = _extract_amount(sal, "netSalary", "net_salary", "NetSalary")

    # ── TDS Details ─────────────────────────────────────────────────────────
    tds = raw.get("tdsDetails") or raw.get("tds_details") or raw.get("TDSDetails") or {}
    result.tds_deducted = _extract_amount(tds, "taxDeducted", "tax_deducted", "TaxDeducted", "tds")
    result.tds_deposited = _extract_amount(tds, "taxDeposited", "tax_deposited", "TaxDeposited")
    result.tds_deduction_date = _pick(tds, "dateOfDeduction", "date", "DeductionDate")

    # ── HRA Details ─────────────────────────────────────────────────────────
    hra = raw.get("hraDetails") or raw.get("hra_details") or raw.get("HRADetails") or {}
    result.hra_received = _extract_amount(hra, "received", "hraReceived", "HraReceived")
    result.hra_exempt = _extract_amount(hra, "exempt", "hraExempt", "HraExempt")
    result.hra_actual_rent_paid = _extract_amount(hra, "actualRentPaid", "actualRent", "ActualRentPaid")
    result.hra_city_tier = _pick(hra, "cityTier", "city_tier", "CityTier", "tier")

    # ── Section 80C / 80CCD(1B) from Part B ────────────────────────────────
    result.sec_80c_claimed = _extract_amount(raw, "deductions80C", "80C", "sec_80c", "section80C")
    result.sec_80ccd1b_claimed = _extract_amount(
        raw, "deductions80CCD1B", "80CCD1B", "sec_80ccd1b", "section80CCD1B"
    )

    return result


def parse_form16_pdf_text(text: str) -> Form16ParsedData:
    """Parse Form 16 data extracted from a PDF (scanned or text).

    Uses regex patterns to extract key fields from raw PDF text.
    This is a best-effort approach for PDF text; JSON import is preferred.
    """
    result = Form16ParsedData()

    # PAN
    pan_match = re.search(r"\b([A-Z]{5}\d{4}[A-Z])\b", text)
    if pan_match:
        result.employee_pan = pan_match.group(1)

    # TAN
    tan_match = re.search(r"TAN[:\s]*([A-Z]{5}\d{4}[A-Z])", text, re.IGNORECASE)
    if tan_match:
        result.employer_tan = tan_match.group(1)

    # Gross Salary
    salary_match = re.search(
        r"gross\s*salary|gross\s*amount.*?salary|salary.*?(\d[\d,]+\.?\d*)",
        text, re.IGNORECASE
    )
    if salary_match:
        result.gross_salary = _safe_int(salary_match.group(1))

    # TDS
    tds_match = re.search(
        r"tax\s*deducted|tds\s*deducted|tax\s*deducted\s*at\s*source.*?(\d[\d,]+\.?\d*)",
        text, re.IGNORECASE
    )
    if tds_match:
        result.tds_deducted = _safe_int(tds_match.group(1))

    # Standard Deduction
    std_match = re.search(
        r"standard\s*deduction.*?(\d[\d,]+\.?\d*)",
        text, re.IGNORECASE
    )
    if std_match:
        result.standard_deduction_16ia = _safe_int(std_match.group(1))

    # Net Salary
    net_match = re.search(
        r"net\s*salary|taxable\s*salary.*?(\d[\d,]+\.?\d*)",
        text, re.IGNORECASE
    )
    if net_match:
        result.net_salary = _safe_int(net_match.group(1))

    # HRA
    hra_match = re.search(
        r"house\s*rent\s*allowance.*?(\d[\d,]+\.?\d*)",
        text, re.IGNORECASE
    )
    if hra_match:
        result.hra_received = _safe_int(hra_match.group(1))

    result.errors.append(
        "Form 16 PDF text extraction is best-effort. "
        "Please use the JSON export for accurate data."
    )
    return result


def form16_to_tds_record(f16: Form16ParsedData) -> TDSRecord:
    """Convert Form 16 data into a TDSRecord for consistency with 26AS/AIS."""
    return TDSRecord(
        deductor_tan=f16.employer_tan or "XXXXXXXXXX",
        deductor_name=f16.employer_name or "Employer",
        section_code="192",
        amount_paid=f16.gross_salary,
        tax_deducted=f16.tds_deducted,
        tax_deposited=f16.tds_deposited,
        quarter="Q4",
        date_of_credit=f16.tds_deduction_date,
    )
