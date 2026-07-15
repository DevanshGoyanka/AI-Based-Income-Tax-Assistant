"""Canonical TIS domain model - normalized structure for TIS sources."""
from dataclasses import dataclass, field
from decimal import Decimal
from typing import List, Optional


@dataclass
class CanonicalTISTDS:
    """TDS record from TIS."""
    deductor_name: str
    deductor_tan: str
    section_code: str
    amount_paid: Decimal
    tax_deducted: Decimal
    tds_deposited: Decimal
    count: Optional[int] = None
    quarter: Optional[str] = None
    transaction_date: Optional[str] = None
    date_of_deduction: Optional[str] = None
    date_of_deposit: Optional[str] = None
    status: Optional[str] = None
    remarks: Optional[str] = None
    source: str = "tis"


@dataclass
class CanonicalTISInterest:
    """Interest income record from TIS."""
    payer_name: str
    amount: Decimal
    payer_pan: Optional[str] = None
    account_number: Optional[str] = None
    date: Optional[str] = None
    remarks: Optional[str] = None
    source: str = "tis"


@dataclass
class CanonicalTISDividend:
    """Dividend income record from TIS."""
    payer_name: str
    amount: Decimal
    payer_pan: Optional[str] = None
    date: Optional[str] = None
    remarks: Optional[str] = None
    source: str = "tis"


@dataclass
class CanonicalTISCapitalGains:
    """Capital gains/securities transaction record from TIS."""
    transaction_type: str  # "sale" or "purchase"
    source_name: str
    description: str
    amount: Decimal
    source_pan: Optional[str] = None
    date: Optional[str] = None
    remarks: Optional[str] = None
    source: str = "tis"


@dataclass
class CanonicalTISTaxPayment:
    """Tax payment record from TIS."""
    bsr_code: str
    challan_serial_no: str
    challan_date: str
    amount: Decimal
    major_head: str
    minor_head: str
    financial_year: Optional[str] = None
    tax_amount: Optional[Decimal] = None
    surcharge: Optional[Decimal] = None
    cess: Optional[Decimal] = None
    interest: Optional[Decimal] = None
    cin: Optional[str] = None
    status: Optional[str] = None
    source: str = "tis"


@dataclass
class CanonicalTISRefund:
    """Refund record from TIS."""
    assessment_year: str
    refund_amount: Decimal
    mode: Optional[str] = None
    date: Optional[str] = None
    status: Optional[str] = None
    source: str = "tis"


@dataclass
class CanonicalTISPersonalInfo:
    """Personal information from TIS."""
    pan: str
    name: str
    dob: Optional[str] = None
    aadhaar: Optional[str] = None
    mobile: Optional[str] = None
    email: Optional[str] = None
    address: Optional[str] = None


@dataclass
class CanonicalTIS:
    """Complete canonical TIS data."""
    personal_info: CanonicalTISPersonalInfo
    assessment_year: str
    tds_records: List[CanonicalTISTDS] = field(default_factory=list)
    interest_records: List[CanonicalTISInterest] = field(default_factory=list)
    dividend_records: List[CanonicalTISDividend] = field(default_factory=list)
    capital_gains_records: List[CanonicalTISCapitalGains] = field(default_factory=list)
    tax_payments: List[CanonicalTISTaxPayment] = field(default_factory=list)
    refunds: List[CanonicalTISRefund] = field(default_factory=list)
    source_format: str = "json"
    
    @property
    def total_tds(self) -> Decimal:
        return sum(r.tax_deducted for r in self.tds_records)
    
    @property
    def total_tax_paid(self) -> Decimal:
        return sum(r.amount for r in self.tax_payments)
    
    @property
    def total_interest(self) -> Decimal:
        return sum(r.amount for r in self.interest_records)
    
    @property
    def total_dividend(self) -> Decimal:
        return sum(r.amount for r in self.dividend_records)
    
    @property
    def total_capital_gains_sale(self) -> Decimal:
        return sum(r.amount for r in self.capital_gains_records if r.transaction_type == "sale")
    
    @property
    def total_capital_gains_purchase(self) -> Decimal:
        return sum(r.amount for r in self.capital_gains_records if r.transaction_type == "purchase")
