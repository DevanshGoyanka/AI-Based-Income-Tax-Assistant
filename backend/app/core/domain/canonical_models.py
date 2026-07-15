"""Canonical income models - normalized representation."""
from dataclasses import dataclass
from decimal import Decimal
from typing import List, Optional


@dataclass
class CanonicalPersonalInfo:
    """Normalized personal information."""
    pan: str
    name: str
    father_name: str
    dob: str
    aadhaar: str
    email: str
    mobile: str
    address: str
    pincode: str
    state_code: str


@dataclass
class CanonicalBankAccount:
    """Normalized bank account."""
    bank_name: str
    account_number: str
    ifsc_code: str
    account_type: str
    use_for_refund: bool = False


@dataclass
class CanonicalAdvanceTax:
    """Normalized advance tax payment."""
    bsr_code: str
    challan_serial: str
    date_paid: str
    amount: Decimal
    bank_name: str
    branch_name: str


@dataclass
class CanonicalSalary:
    """Normalized salary income."""
    employer_name: str
    employer_tan: str
    gross_salary: Decimal
    allowances_exempt: Decimal
    professional_tax: Decimal
    tds_deducted: Decimal
    standard_deduction: Decimal = Decimal("0")


@dataclass
class CanonicalHouseProperty:
    """Normalized house property."""
    address: str
    ownership_share: Decimal
    annual_rent: Decimal
    municipal_taxes: Decimal
    interest_paid: Decimal
    co_owners: List[str]


@dataclass
class CanonicalInterest:
    """Normalized interest income."""
    bank_name: str
    account_number: str
    interest_amount: Decimal


@dataclass
class CanonicalDividend:
    """Normalized dividend income."""
    company_name: str
    dividend_amount: Decimal


@dataclass
class CanonicalTDS:
    """Normalized TDS entry."""
    deductor_name: str
    deductor_tan: str
    section_code: str
    amount_paid: Decimal
    tax_deducted: Decimal
    tax_deposited: Decimal
    quarter: str


@dataclass
class CanonicalIncome:
    """Complete normalized income data from any source."""
    personal_info: Optional[CanonicalPersonalInfo]
    bank_accounts: List[CanonicalBankAccount]
    advance_tax: List[CanonicalAdvanceTax]
    salaries: List[CanonicalSalary]
    house_properties: List[CanonicalHouseProperty]
    interest_income: List[CanonicalInterest]
    dividend_income: List[CanonicalDividend]
    tds_entries: List[CanonicalTDS]
    other_income: Decimal = Decimal("0")
