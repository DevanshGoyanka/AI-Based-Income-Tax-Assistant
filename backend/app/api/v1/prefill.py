"""Prefill import API: 26AS / AIS / TIS — JSON (plain + encrypted) and PDF (password-protected).

Import paths:
  POST /prefill/26as          — plain JSON (ITD 26AS JSON export)
  POST /prefill/ais/json      — encrypted AIS JSON (ITD AIS Utility)
  POST /prefill/ais/pdf       — AIS PDF (password: PAN(lower) + DDMMYYYY)
  POST /prefill/tis/pdf       — TIS PDF (password: PAN(lower) + DDMMYYYY)
  GET  /prefill/tds/{id}      — list TDS records for a client
"""
from __future__ import annotations
import json
import logging
from uuid import UUID

from fastapi import APIRouter, Depends, File, Form, HTTPException, UploadFile, status
from pydantic import BaseModel, Field
from typing import Literal, Any

from app.deps import CurrentUser, DatabaseSession
from app.services.prefill_import import (
    parse_26as_json,
    summarize_tds, store_tds_records, get_or_create_client,
)
from app.services.ais_decrypt import decrypt_ais
from app.services.ais_parser import parse_ais_json as parse_ais_decrypted_json, AISParsedData
from app.services.tis_parser import parse_tis_json as parse_tis_decrypted_json
from app.services.pdf_decrypt import (
    decrypt_and_extract_tables,
    PDFParseResult,
    PDFDecryptionError,
)
from app.schemas.prefill import ImportResponse

logger = logging.getLogger(__name__)
router = APIRouter()


# ── Shared helpers ────────────────────────────────────────────────────────────


async def _read_upload(file: UploadFile) -> bytes:
    raw = await file.read()
    if not raw:
        raise HTTPException(status.HTTP_400_BAD_REQUEST, "Empty file upload")
    return raw


async def _read_json(file: UploadFile) -> dict:
    raw = await _read_upload(file)
    try:
        return json.loads(raw.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as e:
        raise HTTPException(status.HTTP_400_BAD_REQUEST, f"Invalid JSON: {e}")


def _safe_int(val) -> int:
    if val is None or val == "":
        return 0
    try:
        return int(float(val))
    except (ValueError, TypeError):
        return 0


# ── Response models ───────────────────────────────────────────────────────────


class AISPDFImportResponse(BaseModel):
    """Response for AIS PDF import."""
    source: Literal["AIS_PDF"]
    pan: str
    ay: str
    pages_processed: int
    is_encrypted: bool
    tds_salary_count: int
    tds_others_count: int
    interest_count: int
    dividend_count: int
    other_count: int
    total_tax_deducted: int
    stored: int
    message: str
    extractor: str = ""
    warnings: list[str] = Field(default_factory=list)


class TISPDFImportResponse(BaseModel):
    """Response for TIS PDF import."""
    source: Literal["TIS_PDF"]
    pan: str
    ay: str
    pages_processed: int
    is_encrypted: bool
    tds_count: int
    tax_payment_count: int
    total_tax_deducted: int
    stored: int
    message: str
    extractor: str = ""
    warnings: list[str] = Field(default_factory=list)


class AISDecryptedJSONResponse(BaseModel):
    """Response for decrypted AIS JSON import."""
    source: Literal["AIS_DECRYPTED"]
    pan: str
    ay: str
    tds_salary_count: int
    tds_others_count: int
    info_items_count: int
    total_tax_deducted: int
    stored: int
    personal_info: dict = Field(default_factory=dict)
    bank_accounts: list[dict] = Field(default_factory=list)
    message: str


# ── 26AS plain JSON (existing) ────────────────────────────────────────────────


@router.post("/prefill/26as", response_model=ImportResponse)
async def import_26as(
    file: UploadFile = File(...),
    ay: str = Form("2026-27"),
    db: DatabaseSession = None,
    current_user: CurrentUser = None,
):
    """Import plain (unencrypted) 26AS JSON from ITD e-Filing portal."""
    raw = await _read_json(file)
    pan = str(raw.get("PAN") or raw.get("Pan") or raw.get("pan") or "").upper()
    if not pan or len(pan) != 10:
        raise HTTPException(400, "PAN not found or invalid in 26AS JSON")

    client = await get_or_create_client(db, pan)
    records = parse_26as_json(raw, pan, ay)
    stored = await store_tds_records(db, client.id, ay, records, "26AS")

    from app.infra.db.models.form26as_data import Form26ASData
    db.add(Form26ASData(
        client_id=client.id,
        ay=ay,
        pan=pan,
        raw_json=raw,
        parsed_summary=summarize_tds(records).model_dump(),
        imported_by=current_user.id,
    ))
    await db.commit()

    return ImportResponse(
        source="26AS",
        pan=pan,
        ay=ay,
        summary=summarize_tds(records),
        stored=stored,
        message=f"Imported {stored} TDS records for PAN {pan} (AY {ay})",
    )


# ── AIS: encrypted JSON ───────────────────────────────────────────────────────


@router.post("/prefill/ais/json", response_model=AISDecryptedJSONResponse)
async def import_ais_encrypted_json(
    file: UploadFile = File(...),
    pan: str = Form(..., max_length=10, description="10-char PAN"),
    dob: str = Form(..., max_length=8, description="DOB in DDMMYYYY"),
    ay: str = Form("2026-27"),
    db: DatabaseSession = None,
    current_user: CurrentUser = None,
):
    """Decrypt and import an encrypted AIS JSON from the ITD AIS Utility.

    The AIS Utility exports a .json file that is AES-256-CBC encrypted.
    The password is derived from: PAN(lowercase) + "GQ39%*g" + DOB(ddmmyyyy)

    Upload the raw encrypted .json file (the one you download from AIS Utility).
    """
    raw_bytes = await _read_upload(file)

    if len(raw_bytes) < 64:
        raise HTTPException(
            status.HTTP_400_BAD_REQUEST,
            "File too short. Expected encrypted AIS JSON from ITD AIS Utility."
        )

    # Decrypt
    try:
        decrypted: dict = decrypt_ais(raw_bytes, pan, dob)
    except ValueError as e:
        logger.warning("AIS decrypt failed for PAN %s: %s", pan, e)
        raise HTTPException(status.HTTP_400_BAD_REQUEST, f"AIS decryption failed: {e}")

    # Log structure for debugging
    logger.info("Decrypted AIS keys: %s", list(decrypted.keys()))
    
    # Parse the decrypted JSON
    parsed: AISParsedData = parse_ais_decrypted_json(decrypted)

    # Get PAN from parsed data (may differ from form input)
    resolved_pan = parsed.personal_info.get("pan") or pan.upper()
    client = await get_or_create_client(db, resolved_pan)

    # Build TDS records
    from app.schemas.prefill import TDSRecord
    tds_records = []
    for raw_rec in parsed.tds_records:
        tds_records.append(raw_rec)

    if not tds_records and not parsed.info_items:
        raise HTTPException(
            status.HTTP_400_BAD_REQUEST,
            "Decrypted AIS JSON contains no TDS records or info items. "
            "Check if the PAN and DOB are correct."
        )

    stored = await store_tds_records(db, client.id, ay, tds_records, "AIS")

    # Store AIS data
    from app.infra.db.models.ais_data import AISData
    db.add(AISData(
        client_id=client.id,
        ay=ay,
        pan=resolved_pan,
        source="AIS_ENCRYPTED",
        raw_json=decrypted,
        parsed_summary={
            "info_items": [i.model_dump() for i in parsed.info_items],
            "bank_accounts": parsed.bank_accounts,
            "personal_info": parsed.personal_info,
            "tds_salary_count": sum(1 for r in tds_records if r.section_code == "192"),
            "tds_others_count": sum(1 for r in tds_records if r.section_code != "192"),
            "info_items_count": len(parsed.info_items),
            "total_tax_deducted": parsed.total_tds_tax,
        },
        imported_by=current_user.id,
    ))
    await db.commit()

    return AISDecryptedJSONResponse(
        source="AIS_DECRYPTED",
        pan=resolved_pan,
        ay=ay,
        tds_salary_count=sum(1 for r in tds_records if r.section_code == "192"),
        tds_others_count=sum(1 for r in tds_records if r.section_code != "192"),
        info_items_count=len(parsed.info_items),
        total_tax_deducted=parsed.total_tds_tax,
        stored=stored,
        personal_info=parsed.personal_info,
        bank_accounts=parsed.bank_accounts,
        message=(
            f"Decrypted and imported {stored} TDS records and "
            f"{len(parsed.info_items)} info items for PAN {resolved_pan}"
        ),
    )


# ── AIS: password-protected PDF ───────────────────────────────────────────────


@router.post("/prefill/ais/pdf", response_model=AISPDFImportResponse)
async def import_ais_pdf(
    file: UploadFile = File(...),
    pan: str = Form(..., max_length=10),
    dob: str = Form(..., max_length=8, description="DDMMYYYY"),
    ay: str = Form("2026-27"),
    db: DatabaseSession = None,
    current_user: CurrentUser = None,
):
    """Import AIS as a password-protected PDF.

    The PDF is encrypted with: PAN(lowercase) + DDMMYYYY
    e.g. PAN=AAAAA1234A, DOB=21Jan1991 → "aaaaa1234a21011991"

    The PDF contains bordered tables for:
      - TDS on Salary, TDS on Other Income
      - Interest from Banks / Others
      - Dividends, Property Sales, Foreign Remittances, etc.

    We extract tables using camelot-py (best), pdfplumber (fallback), or
    plain text regex (last resort).
    """
    pdf_bytes = await _read_upload(file)

    try:
        result: PDFParseResult = decrypt_and_extract_tables(
            pdf_bytes, pan, dob, document_type="AIS"
        )
    except PDFDecryptionError as e:
        raise HTTPException(status.HTTP_400_BAD_REQUEST, str(e))
    except Exception as e:
        logger.exception("AIS PDF import failed")
        raise HTTPException(status.HTTP_500_INTERNAL_SERVER_ERROR, f"AIS PDF import failed: {e}")

    parsed = result.parsed

    # Resolve PAN (may have been extracted from PDF)
    resolved_pan = parsed.get("pan") or pan.upper()
    client = await get_or_create_client(db, resolved_pan)

    # Convert parsed data → TDSRecord list
    from app.schemas.prefill import TDSRecord
    tds_records: list[TDSRecord] = []

    for entry in parsed.get("tds_salary", []):
        tan = str(entry.get("tan", "")).strip()
        if len(tan) != 10:
            continue
        tds_records.append(TDSRecord(
            deductor_tan=tan,
            deductor_name=str(entry.get("name", "Unknown")).strip(),
            section_code="192",
            amount_paid=_safe_int(entry.get("amount_paid")),
            tax_deducted=_safe_int(entry.get("tax_deducted")),
            tax_deposited=_safe_int(entry.get("tax_deposited", entry.get("tax_deducted"))),
            date_of_credit=str(entry.get("date_of_credit") or ""),
            receipt_no=str(entry.get("receipt_no") or "") or None,
            status=str(entry.get("status") or "") or None,
        ))

    for entry in parsed.get("tds_others", []):
        tan = str(entry.get("tan", "")).strip()
        if len(tan) != 10:
            continue
        section = str(entry.get("section", "")).replace(" ", "") or "OTH"
        tds_records.append(TDSRecord(
            deductor_tan=tan,
            deductor_name=str(entry.get("name", "Unknown")).strip(),
            section_code=section,
            amount_paid=_safe_int(entry.get("amount_paid")),
            tax_deducted=_safe_int(entry.get("tax_deducted")),
            tax_deposited=_safe_int(entry.get("tax_deposited", entry.get("tax_deducted"))),
            date_of_credit=str(entry.get("date_of_credit") or ""),
            receipt_no=str(entry.get("receipt_no") or "") or None,
            status=str(entry.get("status") or "") or None,
        ))

    stored = await store_tds_records(db, client.id, ay, tds_records, "AIS_PDF")

    # Store AIS data
    from app.infra.db.models.ais_data import AISData
    db.add(AISData(
        client_id=client.id,
        ay=ay,
        pan=resolved_pan,
        source="AIS_PDF",
        raw_json=parsed,
        parsed_summary={
            "tds_salary": parsed.get("tds_salary", []),
            "tds_others": parsed.get("tds_others", []),
            "interest_bank": parsed.get("interest_bank", []),
            "interest_others": parsed.get("interest_others", []),
            "dividends": parsed.get("dividends", []),
            "other_info": parsed.get("other", []),
            "assessee_name": parsed.get("assessee_name"),
            "assessment_year": parsed.get("assessment_year"),
        },
        imported_by=current_user.id,
    ))
    await db.commit()

    extractor = "camelot" if any("camelot" in e for e in result.errors) else "pdfplumber" if result.errors else "direct"
    return AISPDFImportResponse(
        source="AIS_PDF",
        pan=resolved_pan,
        ay=ay,
        pages_processed=result.pages,
        is_encrypted=result.is_encrypted,
        tds_salary_count=len(parsed.get("tds_salary", [])),
        tds_others_count=len(parsed.get("tds_others", [])),
        interest_count=len(parsed.get("interest_bank", [])) + len(parsed.get("interest_others", [])),
        dividend_count=len(parsed.get("dividends", [])),
        other_count=len(parsed.get("other", [])),
        total_tax_deducted=sum(_safe_int(e.get("tax_deducted")) for e in parsed.get("tds_salary", []) + parsed.get("tds_others", [])),
        stored=stored,
        message=f"Extracted {stored} TDS records from AIS PDF for PAN {resolved_pan}",
        extractor=extractor,
        warnings=result.errors,
    )


# ── TIS: password-protected PDF ───────────────────────────────────────────────


@router.post("/prefill/tis/pdf", response_model=TISPDFImportResponse)
async def import_tis_pdf(
    file: UploadFile = File(...),
    pan: str = Form(..., max_length=10),
    dob: str = Form(..., max_length=8, description="DDMMYYYY"),
    ay: str = Form("2026-27"),
    db: DatabaseSession = None,
    current_user: CurrentUser = None,
):
    """Import TIS as a password-protected PDF.

    Password: PAN(lowercase) + DDMMYYYY

    TIS contains:
      - TDS Details table
      - Tax Payments table (Advance tax, Self-assessment tax, etc.)
    """
    pdf_bytes = await _read_upload(file)

    try:
        result: PDFParseResult = decrypt_and_extract_tables(
            pdf_bytes, pan, dob, document_type="TIS"
        )
    except PDFDecryptionError as e:
        raise HTTPException(status.HTTP_400_BAD_REQUEST, str(e))
    except Exception as e:
        logger.exception("TIS PDF import failed")
        raise HTTPException(status.HTTP_500_INTERNAL_SERVER_ERROR, f"TIS PDF import failed: {e}")

    parsed = result.parsed
    resolved_pan = parsed.get("pan") or pan.upper()
    client = await get_or_create_client(db, resolved_pan)

    # Convert to TDS records
    from app.schemas.prefill import TDSRecord
    tds_records: list[TDSRecord] = []

    for entry in parsed.get("tds", []):
        tan = str(entry.get("tan", "")).strip()
        if len(tan) != 10:
            continue
        tds_records.append(TDSRecord(
            deductor_tan=tan,
            deductor_name=str(entry.get("name", "Unknown")).strip(),
            section_code=str(entry.get("section", "")).replace(" ", "") or "OTH",
            amount_paid=_safe_int(entry.get("amount")),
            tax_deducted=_safe_int(entry.get("tax_deducted")),
            tax_deposited=_safe_int(entry.get("tax_deposited", entry.get("tax_deducted"))),
            date_of_credit=str(entry.get("date_of_credit") or "") or None,
            date_of_deposit=str(entry.get("date_of_deposit") or "") or None,
            receipt_no=str(entry.get("challan_no") or "") or None,
            quarter=str(entry.get("quarter") or "") or None,
        ))

    stored = await store_tds_records(db, client.id, ay, tds_records, "TIS_PDF")

    # Store TIS data
    from app.infra.db.models.tis_data import TISData
    db.add(TISData(
        client_id=client.id,
        ay=ay,
        pan=resolved_pan,
        raw_json=parsed,
        parsed_summary={
            "tds": parsed.get("tds", []),
            "tax_payments": parsed.get("tax_payments", []),
            "assessee_name": parsed.get("assessee_name"),
            "assessment_year": parsed.get("assessment_year"),
        },
        imported_by=current_user.id,
    ))
    await db.commit()

    return TISPDFImportResponse(
        source="TIS_PDF",
        pan=resolved_pan,
        ay=ay,
        pages_processed=result.pages,
        is_encrypted=result.is_encrypted,
        tds_count=len(parsed.get("tds", [])),
        tax_payment_count=len(parsed.get("tax_payments", [])),
        total_tax_deducted=sum(_safe_int(e.get("tax_deducted")) for e in parsed.get("tds", [])),
        stored=stored,
        message=f"Extracted {stored} TDS records from TIS PDF for PAN {resolved_pan}",
        warnings=result.errors,
    )


# ── TIS: plain JSON (existing, now under /prefill/tis) ────────────────────────


@router.post("/prefill/tis", response_model=ImportResponse)
async def import_tis_json(
    file: UploadFile = File(...),
    ay: str = Form("2026-27"),
    db: DatabaseSession = None,
    current_user: CurrentUser = None,
):
    """Import plain (unencrypted) TIS JSON from e-Filing portal."""
    raw = await _read_json(file)
    pan = str(raw.get("PAN") or raw.get("Pan") or raw.get("pan") or "").upper()
    if not pan or len(pan) != 10:
        raise HTTPException(400, "PAN not found or invalid in TIS JSON")

    client = await get_or_create_client(db, pan)
    parsed: Any = parse_tis_decrypted_json(raw)
    records = parsed.tds_records
    stored = await store_tds_records(db, client.id, ay, records, "TIS")

    from app.infra.db.models.tis_data import TISData
    db.add(TISData(
        client_id=client.id,
        ay=ay,
        pan=pan,
        raw_json=raw,
        parsed_summary={
            "tds_count": len(records),
            "tax_payment_count": len(parsed.tax_payments),
            "total_tax_deducted": parsed.total_tds_tax,
        },
        imported_by=current_user.id,
    ))
    await db.commit()

    return ImportResponse(
        source="TIS",
        pan=pan,
        ay=ay,
        summary=summarize_tds(records),
        stored=stored,
        message=f"Imported {stored} TDS records from TIS JSON for PAN {pan}",
    )


# ── AIS: plain JSON (for testing / already-decrypted AIS) ─────────────────────


@router.post("/prefill/ais", response_model=ImportResponse)
async def import_ais_plain_json(
    file: UploadFile = File(...),
    ay: str = Form("2026-27"),
    db: DatabaseSession = None,
    current_user: CurrentUser = None,
):
    """Import a plain (already-decrypted) AIS JSON.

    Use this for:
      - AIS JSON files that are not encrypted
      - Development / testing with sample data
    For encrypted files, use POST /prefill/ais/json instead.
    """
    raw = await _read_json(file)
    pan = str(raw.get("PAN") or raw.get("pan") or "").upper()
    if not pan or len(pan) != 10:
        raise HTTPException(400, "PAN not found or invalid in AIS JSON")

    client = await get_or_create_client(db, pan)
    parsed: AISParsedData = parse_ais_decrypted_json(raw)
    stored = await store_tds_records(db, client.id, ay, parsed.tds_records, "AIS")

    from app.infra.db.models.ais_data import AISData
    db.add(AISData(
        client_id=client.id,
        ay=ay,
        pan=pan,
        source="AIS_JSON",
        raw_json=raw,
        parsed_summary={
            "info_items": [i.model_dump() for i in parsed.info_items],
            "bank_accounts": parsed.bank_accounts,
            "total_tax_deducted": parsed.total_tds_tax,
        },
        imported_by=current_user.id,
    ))
    await db.commit()

    return ImportResponse(
        source="AIS",
        pan=pan,
        ay=ay,
        summary=summarize_tds(parsed.tds_records),
        stored=stored,
        message=f"Imported {stored} TDS records and {len(parsed.info_items)} info items for PAN {pan}",
    )


# ── TDS list endpoint ─────────────────────────────────────────────────────────


@router.get("/prefill/tds/{client_id}")
async def list_tds(
    client_id: UUID,
    ay: str = "2026-27",
    db: DatabaseSession = None,
    current_user: CurrentUser = None,
):
    """List TDS records for a client + AY."""
    from sqlalchemy import select
    from app.infra.db.models.tds_deductor import TDSDeductor
    res = await db.execute(
        select(TDSDeductor)
        .where(TDSDeductor.client_id == client_id, TDSDeductor.ay == ay)
        .order_by(TDSDeductor.section_code, TDSDeductor.deductor_tan)
    )
    records = res.scalars().all()
    return {
        "client_id": str(client_id),
        "ay": ay,
        "count": len(records),
        "total_tax_deducted": sum(r.tax_deducted for r in records),
        "total_amount_paid": sum(r.amount_paid for r in records),
        "records": [
            {
                "id": str(r.id),
                "tan": r.deductor_tan,
                "name": r.deductor_name,
                "section": r.section_code,
                "amount_paid": r.amount_paid,
                "tax_deducted": r.tax_deducted,
                "tax_deposited": r.tax_deposited,
                "quarter": r.quarter,
                "receipt_no": r.receipt_no,
            }
            for r in records
        ],
    }
