"""Import Form 26AS use case."""
from pathlib import Path
from typing import BinaryIO, Optional
from uuid import UUID

from app.core.domain.canonical_26as import Canonical26AS
from app.core.domain.canonical_models import CanonicalIncome
from app.core.parsers.form26as_pdf_parser import Form26ASPDFParser
from app.core.parsers.form26as_zip_parser import Form26ASZipParser
from app.core.normalizers.form26as_normalizer import Form26ASNormalizer


class ImportForm26ASUseCase:
    """Import Form 26AS (PDF or ZIP) following hexagonal architecture."""
    
    def __init__(self):
        self.pdf_parser = Form26ASPDFParser()
        self.zip_parser = Form26ASZipParser()
        self.normalizer = Form26ASNormalizer()
    
    async def execute(
        self,
        file: BinaryIO,
        file_type: str,
        password: Optional[str] = None,
    ) -> CanonicalIncome:
        """Import Form 26AS and return canonical income data.
        
        Args:
            file: File binary stream
            file_type: "pdf" or "zip"
            password: DOB in DDMMYYYY format
            
        Returns:
            CanonicalIncome with TDS and TCS data
        """
        if file_type.lower() == "zip":
            if not password:
                raise ValueError("Password required for ZIP files")
            canonical_26as = self.zip_parser.parse(file, password)
        elif file_type.lower() == "pdf":
            canonical_26as = self.pdf_parser.parse(file, password)
        else:
            raise ValueError(f"Unsupported file type: {file_type}")
        
        tds_entries = self.normalizer.normalize_to_tds(canonical_26as)
        
        return CanonicalIncome(
            personal_info=None,
            bank_accounts=[],
            advance_tax=[],
            salaries=[],
            house_properties=[],
            interest_income=[],
            dividend_income=[],
            tds_entries=tds_entries,
            other_income=0,
        )
