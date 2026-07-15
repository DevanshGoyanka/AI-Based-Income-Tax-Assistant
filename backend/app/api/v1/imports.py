"""Import endpoints for ITD documents."""
from fastapi import APIRouter, UploadFile, File, Form, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from uuid import UUID
import json

from app.infra.db.base import get_db
from app.infra.db.models.client import Client

from app.core.use_cases.import_prefill import ImportPrefillUseCase
from app.core.use_cases.import_form26as import ImportForm26ASUseCase
from app.core.normalizers.prefill_normalizer import PrefillNormalizer
from app.core.normalizers.canonical_mapper import CanonicalToScheduleMapper
from app.adapters.repositories.filing_repository import SQLAlchemyFilingRepository

router = APIRouter(prefix="/imports", tags=["imports"])


@router.post("/prefill/{client_id}")
async def import_prefill(
    client_id: UUID,
    file: UploadFile = File(...),
    db: AsyncSession = Depends(get_db)
):
    """Import prefill JSON for a client using hexagonal architecture."""
    if not file.filename.endswith('.json'):
        raise HTTPException(400, "Only .json files accepted")

    # Parse JSON
    content = await file.read()
    try:
        raw_data = json.loads(content.decode('utf-8'))
    except (json.JSONDecodeError, UnicodeDecodeError) as e:
        raise HTTPException(400, f"Invalid JSON: {e}")
    
    # Extract AY
    ay = _extract_ay(raw_data, file.filename)
    
    # Wire dependencies (hexagonal architecture)
    filing_repo = SQLAlchemyFilingRepository(db)
    normalizer = PrefillNormalizer()
    mapper = CanonicalToScheduleMapper()
    use_case = ImportPrefillUseCase(filing_repo, normalizer, mapper)
    
    # Execute use case
    try:
        result = await use_case.execute(client_id, raw_data, ay)
        return result
    except Exception as e:
        raise HTTPException(400, {"error": str(e)})


def _extract_ay(data: dict, filename: str) -> str:
    """Extract AY from data or filename."""
    if "filingStatus" in data and "AssessmentYear" in data["filingStatus"]:
        return data["filingStatus"]["AssessmentYear"]
    
    import re
    match = re.search(r'-(\d{4})-', filename)
    if match:
        fy_start = int(match.group(1))
        return f"{fy_start}-{str(fy_start + 1)[-2:]}"
    
    return "2026-27"


@router.post("/form26as/{client_id}")
async def import_form26as(
    client_id: UUID,
    file: UploadFile = File(...),
    password: str | None = Form(None),
    db: AsyncSession = Depends(get_db)
):
    """Import Form 26AS ZIP or PDF for a client using hexagonal architecture.

    ZIP files are encrypted with the client's DOB (DDMMYYYY) as password.
    PDF files may be password-protected with DOB (DDMMYYYY) format.
    """
    filename = file.filename or ""
    if not (filename.lower().endswith('.zip') or filename.lower().endswith('.pdf')):
        raise HTTPException(400, "Only .zip or .pdf files accepted")

    client_row = (await db.execute(select(Client).where(Client.id == client_id))).scalar_one_or_none()
    if not client_row:
        raise HTTPException(404, "Client not found.")

    file_type = "zip" if filename.lower().endswith('.zip') else "pdf"
    
    if not password and client_row.dob:
        password = client_row.dob.strftime('%d%m%Y')

    use_case = ImportForm26ASUseCase()
    
    try:
        content = await file.read()
        import io
        file_stream = io.BytesIO(content)
        
        canonical_income = await use_case.execute(
            file=file_stream,
            file_type=file_type,
            password=password
        )
        
        return {
            "success": True,
            "client_id": str(client_id),
            "tds_count": len(canonical_income.tds_entries),
            "tds_total": sum(tds.tax_deducted for tds in canonical_income.tds_entries),
            "message": f"Imported {len(canonical_income.tds_entries)} TDS entries"
        }
    except Exception as e:
        raise HTTPException(400, f"Import failed: {str(e)}")


@router.post("/ais/{client_id}")
async def import_ais(
    client_id: UUID,
    file: UploadFile = File(...),
    password: str = Form(...),
    pan: str = Form(None),
    db: AsyncSession = Depends(get_db)
):
    """Import AIS JSON or PDF for a client.
    
    JSON: Encrypted with DOB (DDMMYYYY) as password
    PDF: Password-protected with PAN (lowercase) + DOB (DDMMYYYY)
    
    Args:
        client_id: Client UUID
        file: JSON or PDF file
        password: DOB in DDMMYYYY format
        pan: PAN number (required for PDF, optional for JSON)
    """
    filename = file.filename or ""
    if not (filename.lower().endswith('.json') or filename.lower().endswith('.pdf')):
        raise HTTPException(400, "Only .json or .pdf files accepted")

    client_row = (await db.execute(select(Client).where(Client.id == client_id))).scalar_one_or_none()
    if not client_row:
        raise HTTPException(404, "Client not found.")

    file_type = "json" if filename.lower().endswith('.json') else "pdf"
    
    if file_type == "pdf" and not pan:
        pan = client_row.pan

    from app.core.use_cases.import_ais import ImportAISUseCase
    import io
    
    use_case = ImportAISUseCase()
    
    try:
        content = await file.read()
        file_stream = io.BytesIO(content)
        
        canonical_ais = await use_case.execute(
            file=file_stream,
            file_type=file_type,
            password=password,
            pan=pan
        )
        
        return {
            "success": True,
            "client_id": str(client_id),
            "pan": canonical_ais.personal_info.pan,
            "name": canonical_ais.personal_info.name,
            "assessment_year": canonical_ais.assessment_year,
            "tds_count": len(canonical_ais.tds_records),
            "sft_count": len(canonical_ais.sft_records),
            "tax_payments_count": len(canonical_ais.tax_payments),
            "total_tds": float(canonical_ais.total_tds),
            "total_tax_paid": float(canonical_ais.total_tax_paid),
            "message": f"Imported AIS with {len(canonical_ais.tds_records)} TDS records"
        }
    except Exception as e:
        raise HTTPException(400, f"Import failed: {str(e)}")


@router.post("/ais-json/{client_id}")
async def import_ais_json(
    client_id: UUID,
    file: UploadFile = File(...),
    pan: str = Form(...),
    dob: str = Form(...),
    db: AsyncSession = Depends(get_db)
):
    """Import encrypted AIS JSON for a client.
    
    AIS JSON files are encrypted with password format: pan.lower() + 'GQ39%*g' + dob
    
    Args:
        client_id: Client UUID
        file: Encrypted JSON file
        pan: PAN number
        dob: Date of birth in DDMMYYYY format
    """
    filename = file.filename or ""
    if not filename.lower().endswith('.json'):
        raise HTTPException(400, "Only .json files accepted")

    client_row = (await db.execute(select(Client).where(Client.id == client_id))).scalar_one_or_none()
    if not client_row:
        raise HTTPException(404, "Client not found.")

    from app.services.importers.crypto import decrypt_ais_json
    from app.services.ais_organizer import organize_ais_data
    
    try:
        encrypted_content = (await file.read()).decode('utf-8')
        password = pan.lower() + 'GQ39%*g' + dob
        decrypted_data = decrypt_ais_json(encrypted_content, password)
        organized_data = organize_ais_data(decrypted_data)
        
        return {
            "success": True,
            "client_id": str(client_id),
            "data": organized_data,
            "errors": []
        }
    except ValueError as e:
        raise HTTPException(400, f"Decryption failed: {str(e)}")
    except Exception as e:
        raise HTTPException(500, f"Import failed: {str(e)}")
