"""Import endpoints for ITD documents."""
from fastapi import APIRouter, UploadFile, File, Form, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from uuid import UUID
from app.infra.db.base import get_db
from app.infra.db.models.client import Client
from app.services.importers.prefill import PrefillImporter
from app.services.importers.form26as import Form26ASImporter
from app.services.importers.ais_pdf import AISPDFImporter

router = APIRouter(prefix="/imports", tags=["imports"])


@router.post("/prefill/{client_id}")
async def import_prefill(
    client_id: UUID,
    file: UploadFile = File(...),
    db: AsyncSession = Depends(get_db)
):
    """Import prefill JSON for a client."""
    if not file.filename.endswith('.json'):
        raise HTTPException(400, "Only .json files accepted")

    importer = PrefillImporter()
    result = await importer.import_file(db, client_id, file.file, file.filename)

    if result["errors"]:
        raise HTTPException(400, {"errors": result["errors"]})

    return result


@router.post("/form26as/{client_id}")
async def import_form26as(
    client_id: UUID,
    file: UploadFile = File(...),
    dob: str | None = None,  # Override: pass ?dob=YYYY-MM-DD to set password
    db: AsyncSession = Depends(get_db)
):
    """Import Form 26AS ZIP or PDF for a client.

    ZIP files are encrypted with the client's DOB (DDMMYYYY) as password.
    PDF files are not encrypted and do not require a password.

    The DOB used as ZIP password is resolved in this order:
      1. ?dob=YYYY-MM-DD query param (from test script — overrides everything)
      2. Client's DOB from database
    """
    filename = file.filename or ""
    if not (filename.lower().endswith('.zip') or filename.lower().endswith('.pdf')):
        raise HTTPException(400, "Only .zip or .pdf files accepted")

    is_zip = filename.lower().endswith('.zip')

    # Get client record (only for 404 check — DOB comes from query param or DB)
    client_row = (await db.execute(select(Client).where(Client.id == client_id))).scalar_one_or_none()
    if not client_row:
        raise HTTPException(404, "Client not found.")

    # Priority: explicit dob param > DB dob
    if is_zip:
        dob_str = dob or (client_row.dob.isoformat() if client_row.dob else "")
    else:
        dob_str = ""

    importer = Form26ASImporter()
    result = await importer.import_file(db, client_id, file.file, filename, dob_str)

    if result["errors"]:
        raise HTTPException(400, {"errors": result["errors"]})

    return result


@router.post("/ais-pdf/{client_id}")
async def import_ais_pdf(
    client_id: UUID,
    file: UploadFile = File(...),
    pan: str = Form(...),
    dob: str = Form(...),
    db: AsyncSession = Depends(get_db)
):
    """Import AIS PDF for a client.
    
    AIS PDFs are encrypted with PAN+DOB password (format: panDDMMYYYY).
    
    Args:
        client_id: Client UUID
        file: PDF file
        pan: PAN number (case-insensitive)
        dob: Date of birth in DDMMYYYY format
    """
    filename = file.filename or ""
    if not filename.lower().endswith('.pdf'):
        raise HTTPException(400, "Only .pdf files accepted")

    client_row = (await db.execute(select(Client).where(Client.id == client_id))).scalar_one_or_none()
    if not client_row:
        raise HTTPException(404, "Client not found.")

    importer = AISPDFImporter()
    result = await importer.import_file(db, client_id, file.file, filename, pan, dob)

    if result["errors"]:
        raise HTTPException(400, {"errors": result["errors"]})

    return result
