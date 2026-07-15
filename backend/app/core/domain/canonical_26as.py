"""Canonical 26AS models - normalized representation of all 10 parts."""
from dataclasses import dataclass, field
from decimal import Decimal
from typing import List, Optional


@dataclass
class Canonical26ASPartI:
    """Part I: TDS by deductor."""
    deductor_name: str
    deductor_tan: str
    section_code: str
    transaction_date: str
    amount_paid: Decimal
    tax_deducted: Decimal
    tds_deposited: Decimal
    status_of_booking: Optional[str] = None
    date_of_booking: Optional[str] = None
    remarks: Optional[str] = None


@dataclass
class Canonical26ASPartII:
    """Part II: TDS for 15G/15H declarations."""
    deductor_name: str
    deductor_tan: str
    amount_paid: Decimal
    tax_deducted: Decimal
    tds_deposited: Decimal


@dataclass
class Canonical26ASPartIII:
    """Part III: Transactions u/s 194B/194R/194S/194BA."""
    acknowledgement_number: str
    deductor_name: str
    deductor_pan: str
    amount_paid: Decimal


@dataclass
class Canonical26ASPartIV:
    """Part IV: TDS u/s 194IA/194IB/194M/194S (as seller/landlord/contractor)."""
    acknowledgement_number: str
    deductor_name: str
    deductor_pan: str
    transaction_date: str
    transaction_amount: Decimal
    tds_deposited: Decimal


@dataclass
class Canonical26ASPartV:
    """Part V: Transactions u/s 194S as per Form-26QE (VDA)."""
    acknowledgement_number: str
    buyer_name: str
    buyer_pan: str
    transaction_date: str
    transaction_amount: Decimal


@dataclass
class Canonical26ASPartVI:
    """Part VI: Tax Collected at Source (TCS)."""
    collector_name: str
    collector_tan: str
    section_code: Optional[str] = None
    amount_paid: Decimal = Decimal("0")
    tax_collected: Decimal = Decimal("0")
    tcs_deposited: Decimal = Decimal("0")


@dataclass
class Canonical26ASPartVII:
    """Part VII: Paid Refunds."""
    assessment_year: str
    mode: str
    refund_issued: str
    nature_of_refund: str
    amount_of_refund: Decimal
    interest: Decimal
    date_of_payment: str
    remarks: Optional[str] = None


@dataclass
class Canonical26ASPartVIII:
    """Part VIII: TDS u/s 194IA/194IB/194M/194S (as buyer/tenant)."""
    acknowledgement_number: str
    deductee_name: str
    deductee_pan: str
    transaction_date: str
    transaction_amount: Decimal
    tds_deposited: Decimal
    amount_deposited_other_than_tds: Decimal


@dataclass
class Canonical26ASPartIX:
    """Part IX: Transactions/Demand Payments u/s 194S (26QE)."""
    acknowledgement_number: str
    seller_name: str
    seller_pan: str
    transaction_date: str
    transaction_amount: Decimal
    amount_deposited_other_than_tds: Decimal


@dataclass
class Canonical26ASPartX:
    """Part X: TDS/TCS Defaults."""
    financial_year: str
    short_payment: Decimal
    short_deduction_collection: Decimal
    interest_on_tds_tcs_payments_default: Decimal
    interest_on_tds_tcs_deduction_default: Decimal
    late_filing_fee_234e: Decimal
    interest_220_2: Decimal
    total_default: Decimal


@dataclass
class Canonical26AS:
    """Complete normalized Form 26AS data."""
    pan: str
    name: str
    assessment_year: str
    
    part_i: List[Canonical26ASPartI] = field(default_factory=list)
    part_ii: List[Canonical26ASPartII] = field(default_factory=list)
    part_iii: List[Canonical26ASPartIII] = field(default_factory=list)
    part_iv: List[Canonical26ASPartIV] = field(default_factory=list)
    part_v: List[Canonical26ASPartV] = field(default_factory=list)
    part_vi: List[Canonical26ASPartVI] = field(default_factory=list)
    part_vii: List[Canonical26ASPartVII] = field(default_factory=list)
    part_viii: List[Canonical26ASPartVIII] = field(default_factory=list)
    part_ix: List[Canonical26ASPartIX] = field(default_factory=list)
    part_x: List[Canonical26ASPartX] = field(default_factory=list)
    
    source_format: str = "pdf"
