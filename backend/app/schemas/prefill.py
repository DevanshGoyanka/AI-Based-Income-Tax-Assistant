"""Pydantic schemas for 26AS / AIS / TIS JSON import."""
from __future__ import annotations
from pydantic import BaseModel, Field
from typing import Literal


class TDSRecord(BaseModel):
    """Single TDS entry from 26AS / AIS / TIS."""
    deductor_tan: str = Field(..., min_length=10, max_length=10)
    deductor_name: str
    section_code: str  # 192, 194, 194A, 194J, etc.
    amount_paid: int = 0  # in rupees
    tax_deducted: int = 0
    tax_deposited: int = 0
    quarter: str | None = None
    receipt_no: str | None = None
    date_of_credit: str | None = None
    date_of_deduction: str | None = None
    date_of_deposit: str | None = None
    status: str | None = None
    remarks: str | None = None


class TDSImportSummary(BaseModel):
    total_records: int
    total_tax_deducted: int
    total_amount_paid: int
    deductor_count: int
    sections_seen: list[str]


class ImportResponse(BaseModel):
    source: Literal["26AS", "AIS", "TIS"]
    pan: str
    ay: str
    summary: TDSImportSummary
    stored: int
    message: str


class AISInfoItem(BaseModel):
    """Generic AIS info item (interest, dividend, etc.)."""
    info_type: str
    description: str | None = None
    amount: int = 0
    pan_of_deductor: str | None = None
    name_of_deductor: str | None = None
    tan_of_deductor: str | None = None
    section_code: str | None = None
    transaction_date: str | None = None
    remarks: str | None = None


class AISImportSummary(BaseModel):
    total_items: int
    by_type: dict[str, int]
    total_tds: int
    total_amount: int
