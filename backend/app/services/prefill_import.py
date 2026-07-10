"""26AS / AIS / TIS JSON parsers and importers.

The ITD portal exports these in well-defined JSON schemas. We accept the
canonical ITD shapes and store them as TDS records.
"""
from __future__ import annotations
import json
from datetime import datetime, timezone
from uuid import UUID
from sqlalchemy import select, delete
from sqlalchemy.ext.asyncio import AsyncSession
from app.infra.db.models.client import Client
from app.infra.db.models.tds_deductor import TDSDeductor
from app.infra.db.models.form26as_data import Form26ASData
from app.infra.db.models.ais_data import AISData
from app.infra.db.models.tis_data import TISData
from app.schemas.prefill import TDSRecord, TDSImportSummary, AISInfoItem, AISImportSummary


def _safe_int(x) -> int:
    """Convert any value to integer rupees, dropping paise/floats."""
    if x is None or x == "":
        return 0
    try:
        return int(float(x))
    except (ValueError, TypeError):
        return 0


def parse_26as_json(raw: dict, pan: str, ay: str) -> list[TDSRecord]:
    """Parse ITD 26AS JSON shape into TDSRecords.

    ITD 26AS JSON has structure:
    {
      "PAN": "...",
      "AssessmentYear": "2026-27",
      "TDSDetail": [
        {
          "TAN": "...",
          "Name": "...",
          "SectionCode": "192",
          "TransactionDate": "...",
          "Status": "F",
          "AmountPaid": 1234.50,
          "TaxDeducted": 123.45,
          "TaxDeposited": 123.45,
          "Quarter": "Q1",
          "ReceiptNo": "...",
          ...
        }
      ]
    }
    """
    out: list[TDSRecord] = []
    details = raw.get("TDSDetail") or raw.get("TdsDetails") or raw.get("tdsDetail") or []
    for d in details:
        rec = TDSRecord(
            deductor_tan=str(d.get("TAN") or d.get("Tan") or d.get("tan") or "").strip(),
            deductor_name=str(d.get("Name") or d.get("DeductorName") or d.get("name") or "Unknown").strip(),
            section_code=str(d.get("SectionCode") or d.get("Section") or d.get("sectionCode") or "").strip(),
            amount_paid=_safe_int(d.get("AmountPaid") or d.get("amountPaid")),
            tax_deducted=_safe_int(d.get("TaxDeducted") or d.get("taxDeducted")),
            tax_deposited=_safe_int(d.get("TaxDeposited") or d.get("taxDeposited")),
            quarter=str(d.get("Quarter") or d.get("quarter") or "") or None,
            receipt_no=str(d.get("ReceiptNo") or d.get("receiptNo") or "") or None,
            date_of_credit=str(d.get("TransactionDate") or d.get("DateOfCredit") or "") or None,
            date_of_deduction=str(d.get("DateOfDeduction") or d.get("DeductionDate") or "") or None,
            date_of_deposit=str(d.get("DateOfDeposit") or d.get("DepositDate") or "") or None,
            status=str(d.get("Status") or d.get("status") or "") or None,
            remarks=str(d.get("Remarks") or d.get("remarks") or "") or None,
        )
        if rec.deductor_tan and len(rec.deductor_tan) == 10:
            out.append(rec)
    return out


def parse_ais_json(raw: dict, pan: str, ay: str) -> tuple[list[TDSRecord], list[AISInfoItem]]:
    """Parse ITD AIS JSON into TDSRecords and other info items.

    AIS JSON has multiple sections: TDS on salary, TDS on other than salary,
    Interest, Dividend, etc.
    """
    tds: list[TDSRecord] = []
    info: list[AISInfoItem] = []

    # TDS on Salary
    for d in raw.get("TDSonSalary") or []:
        tds.append(TDSRecord(
            deductor_tan=str(d.get("TAN") or "").strip(),
            deductor_name=str(d.get("EmployerName") or d.get("Name") or "Employer").strip(),
            section_code="192",
            amount_paid=_safe_int(d.get("AmountPaid") or d.get("TotalAmount")),
            tax_deducted=_safe_int(d.get("TaxDeducted") or d.get("TotalTaxDeducted")),
            tax_deposited=_safe_int(d.get("TaxDeposited")),
        ))

    # TDS on Other than Salary
    for d in raw.get("TDSonOthThanSal") or []:
        tds.append(TDSRecord(
            deductor_tan=str(d.get("TAN") or "").strip(),
            deductor_name=str(d.get("DeductorName") or d.get("Name") or "Unknown").strip(),
            section_code=str(d.get("SectionCode") or "").strip(),
            amount_paid=_safe_int(d.get("AmountPaid")),
            tax_deducted=_safe_int(d.get("TaxDeducted")),
            tax_deposited=_safe_int(d.get("TaxDeposited")),
        ))

    # Interest
    for d in raw.get("Interest") or []:
        info.append(AISInfoItem(
            info_type="interest",
            description=str(d.get("BankName") or d.get("Name") or "Interest"),
            amount=_safe_int(d.get("Amount")),
            pan_of_deductor=str(d.get("PAN") or "") or None,
            name_of_deductor=str(d.get("Name") or "") or None,
        ))

    # Dividend
    for d in raw.get("Dividend") or []:
        info.append(AISInfoItem(
            info_type="dividend",
            description=str(d.get("CompanyName") or d.get("Name") or "Dividend"),
            amount=_safe_int(d.get("Amount")),
            pan_of_deductor=str(d.get("PAN") or "") or None,
        ))

    return tds, info


def parse_tis_json(raw: dict, pan: str, ay: str) -> list[TDSRecord]:
    """Parse TIS JSON. TIS is the consolidated Tax Information Statement.

    Similar to AIS but with different section names.
    """
    tds: list[TDSRecord] = []
    for d in raw.get("TDSDetails") or raw.get("TdsDetails") or []:
        tds.append(TDSRecord(
            deductor_tan=str(d.get("Tan") or d.get("TAN") or "").strip(),
            deductor_name=str(d.get("DeductorName") or d.get("Name") or "Unknown").strip(),
            section_code=str(d.get("Section") or d.get("SectionCode") or "").strip(),
            amount_paid=_safe_int(d.get("Amount") or d.get("AmountPaid")),
            tax_deducted=_safe_int(d.get("TaxDeducted") or d.get("TDS")),
            tax_deposited=_safe_int(d.get("TaxDeposited")),
        ))
    return [r for r in tds if len(r.deductor_tan) == 10]


def summarize_tds(records: list[TDSRecord]) -> TDSImportSummary:
    deductor_set = {r.deductor_tan for r in records}
    sections = sorted({r.section_code for r in records if r.section_code})
    return TDSImportSummary(
        total_records=len(records),
        total_tax_deducted=sum(r.tax_deducted for r in records),
        total_amount_paid=sum(r.amount_paid for r in records),
        deductor_count=len(deductor_set),
        sections_seen=sections,
    )


def summarize_ais(items: list[AISInfoItem], tds: list[TDSRecord]) -> AISImportSummary:
    by_type: dict[str, int] = {}
    for it in items:
        by_type[it.info_type] = by_type.get(it.info_type, 0) + 1
    return AISImportSummary(
        total_items=len(items),
        by_type=by_type,
        total_tds=sum(r.tax_deducted for r in tds),
        total_amount=sum(it.amount for it in items),
    )


async def store_tds_records(
    db: AsyncSession,
    client_id: UUID,
    ay: str,
    records: list[TDSRecord],
    source: str,
) -> int:
    """Replace existing TDS records for this (client, ay) with new ones."""
    # Delete old records for the same (client, ay)
    await db.execute(
        delete(TDSDeductor).where(
            TDSDeductor.client_id == client_id,
            TDSDeductor.ay == ay,
        )
    )
    stored = 0
    for r in records:
        db.add(TDSDeductor(
            client_id=client_id,
            ay=ay,
            deductor_tan=r.deductor_tan,
            deductor_name=r.deductor_name,
            section_code=r.section_code,
            amount_paid=r.amount_paid,
            tax_deducted=r.tax_deducted,
            tax_deposited=r.tax_deposited,
            quarter=r.quarter,
            receipt_no=r.receipt_no,
            extra_data={
                "source": source,
                "date_of_credit": r.date_of_credit,
                "date_of_deduction": r.date_of_deduction,
                "date_of_deposit": r.date_of_deposit,
                "status": r.status,
                "remarks": r.remarks,
            },
        ))
        stored += 1
    await db.commit()
    return stored


async def get_or_create_client(db: AsyncSession, pan: str) -> Client:
    """Find client by PAN, or create a placeholder. If multiple exist, return the oldest."""
    from sqlalchemy import asc
    pan = pan.upper()
    res = await db.execute(
        select(Client).where(Client.pan == pan).order_by(asc(Client.created_at))
    )
    existing = res.scalars().first()
    if existing:
        return existing
    placeholder = Client(pan=pan, name=f"PAN {pan} (auto)", category="individual")
    db.add(placeholder)
    await db.commit()
    await db.refresh(placeholder)
    return placeholder
