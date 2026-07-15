"""Canonical AIS domain model - normalized structure for all AIS sources."""
from dataclasses import dataclass, field
from decimal import Decimal
from typing import List, Optional


@dataclass
class CanonicalAISTDS:
    """TDS record from AIS (Part B1)."""
    deductor_name: str
    deductor_tan: str
    section_code: str
    amount_paid: Decimal
    tax_deducted: Decimal
    tds_deposited: Decimal
    information_category: Optional[str] = None
    information_code: Optional[str] = None
    information_description: Optional[str] = None
    count: Optional[int] = None
    quarter: Optional[str] = None
    date_of_deduction: Optional[str] = None
    date_of_booking: Optional[str] = None
    status: Optional[str] = None
    remarks: Optional[str] = None
    source: str = "ais"


@dataclass
class CanonicalAISSFT:
    """SFT (Specified Financial Transaction) record from AIS (Part B2)."""
    transaction_type: str
    payer_name: str
    amount: Decimal
    payer_pan: Optional[str] = None
    information_category: Optional[str] = None
    information_code: Optional[str] = None
    information_description: Optional[str] = None
    count: Optional[int] = None
    date: Optional[str] = None
    remarks: Optional[str] = None
    source: str = "ais"


@dataclass
class CanonicalAISTaxPayment:
    """Tax payment record from AIS (Part B3)."""
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
    others: Optional[Decimal] = None
    cin: Optional[str] = None
    status: Optional[str] = None
    source: str = "ais"


@dataclass
class CanonicalAISDemandRefund:
    """Demand/Refund record from AIS (Part B4)."""
    assessment_year: str
    demand_amount: Decimal
    refund_amount: Decimal
    date: Optional[str] = None
    status: Optional[str] = None
    source: str = "ais"


@dataclass
class CanonicalAISPersonalInfo:
    """Personal information from AIS."""
    pan: str
    name: str
    dob: Optional[str] = None
    aadhaar: Optional[str] = None
    mobile: Optional[str] = None
    email: Optional[str] = None
    address: Optional[str] = None


@dataclass
class CanonicalAIS:
    """Complete canonical AIS data."""
    personal_info: CanonicalAISPersonalInfo
    assessment_year: str
    tds_records: List[CanonicalAISTDS] = field(default_factory=list)
    sft_records: List[CanonicalAISSFT] = field(default_factory=list)
    tax_payments: List[CanonicalAISTaxPayment] = field(default_factory=list)
    demands_refunds: List[CanonicalAISDemandRefund] = field(default_factory=list)
    source_format: str = "json"  # "json" or "pdf"
    
    @property
    def total_tds(self) -> Decimal:
        return sum(r.tax_deducted for r in self.tds_records)
    
    @property
    def total_tax_paid(self) -> Decimal:
        return sum(r.amount for r in self.tax_payments)
